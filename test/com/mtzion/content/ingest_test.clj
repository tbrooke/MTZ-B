(ns com.mtzion.content.ingest-test
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.content.ingest :as ingest]
            [com.mtzion.content.plan :as plan]
            [com.mtzion.content.schema :as cs]
            [com.mtzion.model.normalize :as norm]
            [com.mtzion.test-util :refer [with-temp-ctx]]))

(def example-file "content-inbox/examples/bulletin.edn")

(defn- q [ctx honey]
  (norm/snake-keys-all (biff.sqlite/execute ctx honey)))

(defn- row [ctx table k]
  (first (q ctx {:select :* :from table :where [:= :import_key k]})))

(defn- apply-file! [ctx file]
  (let [{:keys [envelope]} (ingest/read-envelope file)
        ops (plan/build ctx (:items envelope) "test" nil)]
    (ingest/apply-ops! ctx ops)
    ops))

;; ---------------------------------------------------------------------------
;; The committed example must always be valid
;; ---------------------------------------------------------------------------

(deftest example-file-validates
  (testing "the example cannot illustrate something the schema rejects"
    (let [{:keys [ok? envelope]} (ingest/read-envelope example-file)]
      (is ok?)
      (is (:ok? (cs/validate envelope)) (pr-str (:errors (cs/validate envelope)))))))

;; ---------------------------------------------------------------------------
;; Reading
;; ---------------------------------------------------------------------------

(deftest tagged-literals-are-refused
  (let [f (java.io.File/createTempFile "bad" ".edn")]
    (spit f "{:mtz/contract 1 :items [{:type :event :key \"k\" :starts-at #inst \"2026-08-16\"}]}")
    (let [r (ingest/read-envelope (.getPath f))]
      (is (not (:ok? r)))
      (is (re-find #"tagged literal" (:error r))))
    (.delete f)))

;; ---------------------------------------------------------------------------
;; Apply, then the properties that matter
;; ---------------------------------------------------------------------------

(deftest applies-as-drafts
  (with-temp-ctx [ctx]
    (apply-file! ctx example-file)
    (testing "everything lands unpublished"
      (is (= 0 (:published (row ctx :event "back-to-school-blessing-2026"))))
      (is (= 0 (:published (row ctx :feature "home-fall-kickoff-2026"))))
      (is (= 0 (:published (row ctx :sermon "sermon-2026-08-09")))))
    (testing "a post's draft state is a NULL published_at, not a column"
      (let [p (row ctx :post "council-notes-2026-08")]
        (is (nil? (:published_at p)))
        (testing "and the extracted date is preserved for the admin form"
          (is (re-find #"2026-08-09" (:import_meta p))))))))

(deftest converts-values-correctly
  (with-temp-ctx [ctx]
    (apply-file! ctx example-file)
    (let [ev (row ctx :event "back-to-school-blessing-2026")]
      (testing "a datetime is stored as church wall-clock"
        (is (= "2026-08-16T10:30" (norm/epoch->local-datetime-str (:start_at ev)))))
      (testing "booleans become 1/0"
        (is (= 1 (:featured ev)))
        (is (= 0 (:all_day ev)))))
    (let [hb (row ctx :event "handbell-rehearsal-fall-2026")]
      (testing "a date-only column is UTC midnight"
        (is (= "2026-12-17" (norm/epoch->date-str (:recur_until hb))))
        (is (zero? (mod (:recur_until hb) 86400))))
      (testing "recurrence enum is stored as a string"
        (is (= "weekly" (:recurrence hb)))))
    (testing "hiccup bodies are rendered to the HTML the editor would store"
      (is (= "<p>New ringers welcome — no experience necessary.</p>"
             (:description (row ctx :event "handbell-rehearsal-fall-2026"))))
      (is (str/includes? (:body (row ctx :post "council-notes-2026-08"))
                         "<li><strong>Roof project.</strong>")))))

(deftest re-import-is-idempotent
  (with-temp-ctx [ctx]
    (apply-file! ctx example-file)
    (let [before (q ctx {:select :* :from :event :order-by [[:import_key :asc]]})
          {:keys [envelope]} (ingest/read-envelope example-file)
          ops (plan/build ctx (:items envelope) "test" nil)]
      (testing "a second run plans no writes at all"
        (is (every? #(= :unchanged (:action %)) ops)
            (pr-str (map (juxt :key :action :changes) ops))))
      (ingest/apply-ops! ctx ops)
      (testing "and changes nothing"
        (is (= before (q ctx {:select :* :from :event :order-by [[:import_key :asc]]})))))))

(deftest publishing-survives-re-import
  ;; The single most important rule: re-dropping a corrected file must never
  ;; un-publish something a human already approved.
  (with-temp-ctx [ctx]
    (apply-file! ctx example-file)
    (biff.sqlite/execute ctx {:update :event :set {:published 1}
                              :where [:= :import_key "back-to-school-blessing-2026"]})
    (biff.sqlite/execute ctx {:update :post :set {:published_at 1786579200}
                              :where [:= :import_key "council-notes-2026-08"]})
    (testing "re-import with an edit still leaves published state alone"
      (let [{:keys [envelope]} (ingest/read-envelope example-file)
            edited (update envelope :items
                           (fn [items]
                             (mapv #(if (= "back-to-school-blessing-2026" (:key %))
                                      (assoc % :location "Fellowship Hall")
                                      %)
                                   items)))
            ops (plan/build ctx (:items edited) "test" nil)]
        (ingest/apply-ops! ctx ops)
        (is (= 1 (:published (row ctx :event "back-to-school-blessing-2026")))
            "still published")
        (is (= "Fellowship Hall" (:location (row ctx :event "back-to-school-blessing-2026")))
            "but the edit did land")
        (is (= 1786579200 (:published_at (row ctx :post "council-notes-2026-08")))
            "post publish date untouched")))))

(deftest updates-are-detected-and-applied
  (with-temp-ctx [ctx]
    (apply-file! ctx example-file)
    (let [{:keys [envelope]} (ingest/read-envelope example-file)
          edited (update envelope :items
                         (fn [items]
                           (mapv #(if (= "back-to-school-blessing-2026" (:key %))
                                    (assoc % :starts-at "2026-08-16T11:00")
                                    %)
                                 items)))
          ops    (plan/build ctx (:items edited) "test" nil)
          op     (first (filter #(= "back-to-school-blessing-2026" (:key %)) ops))]
      (is (= :update (:action op)))
      (is (contains? (:changes op) :start_at))
      (ingest/apply-ops! ctx ops)
      (is (= "2026-08-16T11:00"
             (norm/epoch->local-datetime-str
              (:start_at (row ctx :event "back-to-school-blessing-2026"))))))))

(deftest adopts-pre-existing-rows-by-natural-key
  ;; A row created by hand in the admin should be updated, not duplicated.
  (with-temp-ctx [ctx]
    (biff.sqlite/execute
     ctx {:insert-into :event
          :values [{:id "hand-made" :title "Back-to-School Blessing"
                    :description "" :location ""
                    :start_at (norm/local-datetime->epoch "2026-08-16T10:30")
                    :all_day 0 :recurrence "none" :featured 0 :published 1
                    :created_at 1}]})
    (let [{:keys [envelope]} (ingest/read-envelope example-file)
          ops (plan/build ctx (:items envelope) "test" nil)
          op  (first (filter #(= "back-to-school-blessing-2026" (:key %)) ops))]
      (is (= :update (:action op)))
      (is (:adopting op) "matched on (title, start_at)")
      (is (= "hand-made" (:id op)) "the existing row is reused")
      (ingest/apply-ops! ctx ops)
      (is (= 1 (count (q ctx {:select :* :from :event
                              :where [:= :title "Back-to-School Blessing"]})))
          "the hand-made row was adopted, not duplicated")
      (is (= "hand-made" (:id (row ctx :event "back-to-school-blessing-2026")))
          "and it is the same row, now stamped with the import key")
      (is (= 1 (:published (row ctx :event "back-to-school-blessing-2026")))
          "adoption does not un-publish"))))

(deftest never-deletes
  (with-temp-ctx [ctx]
    (apply-file! ctx example-file)
    (let [{:keys [envelope]} (ingest/read-envelope example-file)
          ;; drop an item from the file entirely
          fewer (update envelope :items #(vec (remove (fn [i] (= :sermon (:type i))) %)))
          ops   (plan/build ctx (:items fewer) "test" nil)]
      (ingest/apply-ops! ctx ops)
      (is (some? (row ctx :sermon "sermon-2026-08-09"))
          "an item removed from the file is left alone in the database"))))

;; ---------------------------------------------------------------------------
;; CLI behaviour
;; ---------------------------------------------------------------------------

(deftest dry-run-writes-nothing
  (with-temp-ctx [ctx]
    (let [{:keys [status output]} (ingest/run ctx [(io/file example-file)] {:apply? false})]
      (is (= 0 status))
      (is (str/includes? output "DRY RUN"))
      (is (str/includes? output "CREATE"))
      (is (str/includes? output "<- draft"))
      (is (zero? (count (q ctx {:select :* :from :event})))
          "the database was not touched"))))

(deftest validation-failure-reports-and-writes-nothing
  (with-temp-ctx [ctx]
    (let [f (java.io.File/createTempFile "bad" ".edn")]
      (spit f (pr-str {:mtz/contract 1
                       :items [{:type :event :key "bad-event" :title "T"
                                :starts-at "August 13 at 6:30pm"
                                :description [[:script "alert(1)"]]}]}))
      (let [{:keys [status output]} (ingest/run ctx [f] {:apply? true})]
        (is (= 1 status))
        (is (str/includes? output "VALIDATION FAILED"))
        (is (str/includes? output "Nothing was written"))
        (is (str/includes? output "2026-08-09T18:30") "the message shows the expected format")
        (is (zero? (count (q ctx {:select :* :from :event})))))
      (.delete f))))

(deftest empty-inbox-is-not-an-error
  (with-temp-ctx [ctx]
    (is (= 0 (:status (ingest/run ctx [] {:apply? false}))))))
