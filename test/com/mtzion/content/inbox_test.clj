(ns com.mtzion.content.inbox-test
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.app.inbox :as pane]
            [com.mtzion.content.inbox :as inbox]
            [com.mtzion.content.ingest :as ingest]
            [com.mtzion.model.content :as content]
            [com.mtzion.model.normalize :as norm]
            [com.mtzion.test-util :refer [with-temp-ctx]]))

(def example-file "content-inbox/examples/bulletin.edn")

(defn- scratch-copy
  "A throwaway copy of the example.

  `--apply` archives its input: it copies the file into content-inbox/applied/
  and DELETES the original. Pointing a test at the committed example therefore
  deletes it from the working tree — which is exactly what happened the first
  time these tests ran."
  []
  (let [f (java.io.File/createTempFile "bulletin" ".edn")]
    (io/copy (io/file example-file) f)
    f))

(defn- stage-example!
  "Stages the example, then cleans up the archive the CLI leaves behind."
  [ctx]
  (let [f   (scratch-copy)
        res (ingest/run ctx [f] {:apply? true})]
    (doseq [leftover (.listFiles (io/file ingest/applied-dir))
            :when (str/starts-with? (.getName leftover) "bulletin")]
      (.delete leftover))
    (.delete f)
    res))

(defn- q [ctx honey] (norm/snake-keys-all (biff.sqlite/execute ctx honey)))

(defn- row-by-key [ctx k]
  (first (filter #(= k (:import_key %)) (inbox/pending ctx))))

;; ---------------------------------------------------------------------------
;; --apply stages instead of publishing
;; ---------------------------------------------------------------------------

(deftest apply-puts-items-in-the-queue-not-on-the-site
  (with-temp-ctx [ctx]
    (let [{:keys [status output]} (stage-example! ctx)]
      (is (= 0 status))
      (is (str/includes? output "STAGED"))
      (is (str/includes? output "/console/inbox"))

      (testing "five items are waiting"
        (is (= 5 (inbox/pending-count ctx))))

      (testing "and not one content row was written"
        (doseq [table [:event :post :feature :sermon :page]]
          (is (zero? (count (q ctx {:select :* :from table})))
              (str table " should be untouched until somebody accepts")))))))

(deftest a-dry-run-still-stages-nothing
  (with-temp-ctx [ctx]
    (let [{:keys [output]} (ingest/run ctx [(io/file example-file)] {:apply? false})]
      (is (str/includes? output "DRY RUN"))
      (is (zero? (inbox/pending-count ctx))))))

(deftest staging-is-all-or-nothing
  (with-temp-ctx [ctx]
    (let [f (java.io.File/createTempFile "bad" ".edn")]
      (spit f (pr-str {:mtz/contract 1
                       :items [{:type :event :key "good-event" :title "Fine"
                                :starts-at "2026-08-16T10:30"}
                               {:type :event :key "bad-event" :title "Broken"
                                :starts-at "not a date"}]}))
      (let [{:keys [status]} (ingest/run ctx [f] {:apply? true})]
        (is (= 1 status))
        (is (zero? (inbox/pending-count ctx))
            "one invalid item means none of the file is staged"))
      (.delete f))))

;; ---------------------------------------------------------------------------
;; Accepting
;; ---------------------------------------------------------------------------

(deftest accepting-creates-a-draft
  (with-temp-ctx [ctx]
    (stage-example! ctx)
    (let [row (row-by-key ctx "back-to-school-blessing-2026")
          op  (inbox/accept! ctx row nil)]
      (is (= :create (:action op)))
      (let [ev (content/get-one ctx :event (:id op))]
        (is (some? ev))
        (is (= "Back-to-School Blessing" (:title ev)))
        (is (= content/draft (:status ev))
            "accepting is not publishing — that stays a separate decision"))

      (testing "and the item leaves the queue with a record of what it became"
        (is (= 4 (inbox/pending-count ctx)))
        (let [after (inbox/get-one ctx (:id row))]
          (is (= inbox/accepted-state (:state after)))
          (is (= (:id op) (:target_id after)))
          (is (some? (:decided_at after))))))))

(deftest accepting-twice-does-not-create-a-second-row
  (with-temp-ctx [ctx]
    (stage-example! ctx)
    (let [row (row-by-key ctx "back-to-school-blessing-2026")]
      (inbox/accept! ctx row nil)
      (is (nil? (inbox/accept! ctx (inbox/get-one ctx (:id row)) nil))
          "a decided item is inert")
      (is (= 1 (count (q ctx {:select :* :from :event})))))))

(deftest dismissing-writes-nothing-but-keeps-the-record
  (with-temp-ctx [ctx]
    (stage-example! ctx)
    (let [row (row-by-key ctx "back-to-school-blessing-2026")]
      (inbox/dismiss! ctx row)
      (is (zero? (count (q ctx {:select :* :from :event}))))
      (is (= 4 (inbox/pending-count ctx)))
      (is (= inbox/dismissed-state (:state (inbox/get-one ctx (:id row))))
          "'we looked at that and said no' stays on the record"))))

(deftest accepting-a-batch-takes-the-whole-drop
  (with-temp-ctx [ctx]
    (stage-example! ctx)
    (let [batch (:batch_id (first (inbox/pending ctx)))]
      (is (= 5 (inbox/accept-batch! ctx batch nil)))
      (is (zero? (inbox/pending-count ctx)))
      (is (= 1 (count (q ctx {:select :* :from :post}))))
      (is (= 2 (count (q ctx {:select :* :from :event}))))
      (testing "everything it created is a draft"
        (is (every? #(= content/draft (:status %)) (content/ls ctx :event)))
        (is (every? #(= content/draft (:status %)) (content/ls ctx :post)))))))

(deftest dismissing-a-batch-leaves-the-tables-alone
  (with-temp-ctx [ctx]
    (stage-example! ctx)
    (let [batch (:batch_id (first (inbox/pending ctx)))]
      (is (= 5 (inbox/dismiss-batch! ctx batch)))
      (is (zero? (inbox/pending-count ctx)))
      (is (zero? (count (q ctx {:select :* :from :event})))))))

;; ---------------------------------------------------------------------------
;; The plan is rebuilt, not frozen
;; ---------------------------------------------------------------------------

(deftest a-row-created-between-drop-and-accept-is-adopted
  (testing "the whole reason the plan is not frozen at staging time"
    (with-temp-ctx [ctx]
      (stage-example! ctx)
      ;; somebody makes the same event by hand before anyone opens the inbox
      (biff.sqlite/execute
       ctx {:insert-into :event
            :values [{:id "hand-made" :title "Back-to-School Blessing"
                      :description "" :location ""
                      :start_at (norm/local-datetime->epoch "2026-08-16T10:30")
                      :all_day 0 :recurrence "none" :featured 0
                      :published 1 :status "published" :created_at 1}]})

      (let [row (row-by-key ctx "back-to-school-blessing-2026")
            op  (inbox/plan-for ctx row nil)]
        (is (= :update (:action op)))
        (is (:adopting op))
        (is (= "hand-made" (:id op)))

        (inbox/accept! ctx row nil)
        (is (= 1 (count (q ctx {:select :* :from :event})))
            "adopted, not duplicated")
        (is (= content/published (:status (content/get-one ctx :event "hand-made")))
            "and accepting an update never touches publish state")))))

(deftest an-item-already-on-the-site-reads-as-unchanged
  (with-temp-ctx [ctx]
    (stage-example! ctx)
    (let [batch (:batch_id (first (inbox/pending ctx)))]
      (inbox/accept-batch! ctx batch nil)
      (stage-example! ctx)
      (let [ops (map #(inbox/plan-for ctx % nil) (inbox/pending ctx))]
        (is (every? #(= :unchanged (:action %)) ops)
            "re-dropping the same bulletin proposes nothing")))))

;; ---------------------------------------------------------------------------
;; Adding by hand
;; ---------------------------------------------------------------------------

(deftest a-hand-added-item-goes-through-the-same-review
  (with-temp-ctx [ctx]
    (let [resp (pane/inbox-create
                (assoc ctx :params {:type "post" :title "Thanks from the food drive"
                                    :body "Sixty bags this year."
                                    :source_ref "Kathy, by email"}))]
      (is (= 303 (:status resp)))
      (is (= 1 (inbox/pending-count ctx)))
      (let [row (first (inbox/pending ctx))]
        (is (= "manual" (:source row)))
        (is (= "Kathy, by email" (:source_ref row)))
        (is (zero? (count (q ctx {:select :* :from :post})))
            "still nothing on the site")
        (inbox/accept! ctx row nil)
        (let [p (first (q ctx {:select :* :from :post}))]
          (is (= "Thanks from the food drive" (:title p)))
          (is (= content/draft (:status p))))))))

(deftest a-hand-added-item-must-still-validate
  (with-temp-ctx [ctx]
    (let [resp (pane/inbox-create
                (assoc ctx :params {:type "event" :title "No date given" :body ""}))]
      (is (= 200 (:status resp)) "the form comes back rather than staging junk")
      (is (str/includes? (str (:body resp)) "didn't validate"))
      (is (zero? (inbox/pending-count ctx))))))

;; ---------------------------------------------------------------------------
;; The pane
;; ---------------------------------------------------------------------------

(deftest the-pane-shows-what-each-item-would-do
  (with-temp-ctx [ctx]
    (stage-example! ctx)
    (let [html (str (:body (pane/inbox ctx)))]
      (is (str/includes? html "From the bulletin"))
      (is (str/includes? html "Back-to-School Blessing"))
      (is (str/includes? html "Accept all 5"))
      (is (str/includes? html "con-inbox-action--create")))))

(deftest an-empty-queue-explains-the-workflow
  (with-temp-ctx [ctx]
    (let [html (str (:body (pane/inbox ctx)))]
      (is (str/includes? html "Nothing waiting"))
      (is (str/includes? html "clj -M:run import")))))

(deftest the-badge-counts-what-is-waiting
  (with-temp-ctx [ctx]
    (is (zero? (inbox/pending-count ctx)))
    (stage-example! ctx)
    (is (= 5 (inbox/pending-count ctx)))
    (is (str/includes? (str (:body (pane/inbox ctx))) "con-bar-count"))))

(deftest payloads-round-trip
  (with-temp-ctx [ctx]
    (stage-example! ctx)
    (doseq [row (inbox/pending ctx)]
      (let [item (inbox/item-of row)]
        (is (keyword? (:type item)))
        (is (= (:import_key row) (:key item)))))))
