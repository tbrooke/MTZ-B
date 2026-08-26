(ns com.mtzion.model.content-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.model.content :as content]
            [com.mtzion.model.normalize :as normalize]
            [com.mtzion.test-util :refer [with-temp-ctx]]))

(defn- insert!
  "Writes a row with the given columns verbatim, bypassing content/save! so a
  test can construct pre-migration shapes (status NULL)."
  [ctx table cols]
  (let [id (str (random-uuid))]
    (biff.sqlite/execute ctx {:insert-into table :values [(assoc cols :id id)]})
    id))

(defn- legacy-event! [ctx title published]
  (insert! ctx :event {:title title :description "" :start_at 1000
                       :published published :created_at 1}))

(defn- legacy-post! [ctx title published-at]
  (insert! ctx :post {:slug (normalize/slugify title) :title title
                      :published_at published-at :created_at 1}))

(defn- status-of [ctx type id]
  (:status (content/get-one ctx type id)))

;; ---------------------------------------------------------------------------
;; Transitions
;; ---------------------------------------------------------------------------

(deftest publish-and-unpublish
  (with-temp-ctx [ctx]
    (let [id (content/save! ctx :event (str (random-uuid))
                            {:title "Rally Day" :description "" :start_at 1000 :created_at 1})]
      (is (= content/draft (status-of ctx :event id))
          "new content is a draft — nothing reaches the site without a decision")

      (content/publish! ctx :event id)
      (let [row (content/get-one ctx :event id)]
        (is (= content/published (:status row)))
        (is (some? (:published_at row)) "publishing records when it went live")
        (is (= 1 (:published row)) "the vestigial column is mirrored for rollback"))

      (let [first-run (:published_at (content/get-one ctx :event id))]
        (content/unpublish! ctx :event id)
        (let [row (content/get-one ctx :event id)]
          (is (= content/draft (:status row)))
          (is (= first-run (:published_at row))
              "published_at is a record of when it ran, not a flag")
          (is (= 0 (:published row))))

        (content/publish! ctx :event id)
        (is (= first-run (:published_at (content/get-one ctx :event id)))
            "re-publishing keeps the original date")))))

(deftest archive-replaces-delete
  (with-temp-ctx [ctx]
    (let [id (content/save! ctx :event (str (random-uuid))
                            {:title "Fall Festival" :description "" :start_at 1000 :created_at 1})]
      (content/publish! ctx :event id)
      (content/archive! ctx :event id)
      (let [row (content/get-one ctx :event id)]
        (is (= content/archived (:status row)))
        (is (some? (:archived_at row)))
        (is (= 0 (:published row)) "an archived item is off the site"))

      (is (empty? (content/live ctx :event)) "archived items are not live")
      (is (= 1 (count (content/ls ctx :event))) "but the row is still there")

      (content/restore! ctx :event id)
      (let [row (content/get-one ctx :event id)]
        (is (= content/draft (:status row))
            "restore lands in draft — never straight back onto the site")
        (is (nil? (:archived_at row)))))))

(deftest purge-only-touches-archived
  (with-temp-ctx [ctx]
    (let [id (content/save! ctx :event (str (random-uuid))
                            {:title "Choir" :description "" :start_at 1000 :created_at 1})]
      (content/publish! ctx :event id)
      (is (nil? (content/purge! ctx :event id)) "a live item cannot be purged")
      (is (some? (content/get-one ctx :event id)))

      (content/archive! ctx :event id)
      (is (= id (content/purge! ctx :event id)))
      (is (nil? (content/get-one ctx :event id))))))

(deftest toggle-is-the-status-pill
  (with-temp-ctx [ctx]
    (let [id (content/save! ctx :post (str (random-uuid))
                            {:slug "on-stillness" :title "On stillness" :created_at 1})]
      (content/toggle! ctx :post id)
      (is (= content/published (status-of ctx :post id)))
      (content/toggle! ctx :post id)
      (is (= content/draft (status-of ctx :post id)))

      (content/archive! ctx :post id)
      (content/toggle! ctx :post id)
      (is (= content/archived (status-of ctx :post id))
          "an archived item is left alone — it comes back through restore!"))))

(deftest save-never-changes-publish-state
  (with-temp-ctx [ctx]
    (let [id (content/save! ctx :post (str (random-uuid))
                            {:slug "notes" :title "Council notes" :created_at 1})]
      (content/publish! ctx :post id)
      (let [before (content/get-one ctx :post id)]
        (content/save! ctx :post id {:title "Council notes, August"
                                     :status content/draft
                                     :archived_at 999})
        (let [after (content/get-one ctx :post id)]
          (is (= "Council notes, August" (:title after)) "the edit lands")
          (is (= content/published (:status after))
              "publish state changes only through the transitions, never as a side effect of an edit")
          (is (= (:published_at before) (:published_at after)))
          (is (nil? (:archived_at after))))))))

;; ---------------------------------------------------------------------------
;; Reading
;; ---------------------------------------------------------------------------

(deftest listing-filters-by-status
  (with-temp-ctx [ctx]
    (let [live-id  (content/save! ctx :event (str (random-uuid))
                                  {:title "Bible study" :description "" :start_at 2000 :created_at 1})
          draft-id (content/save! ctx :event (str (random-uuid))
                                  {:title "Pet blessing" :description "" :start_at 3000 :created_at 1})]
      (content/publish! ctx :event live-id)
      (is (= [live-id] (mapv :id (content/live ctx :event))))
      (is (= #{live-id draft-id} (set (map :id (content/ls ctx :event)))))
      (is (= {content/published 1 content/draft 1}
             (content/counts-by-status ctx :event)))
      (is (= [draft-id] (mapv :id (content/ls ctx :event {:status content/draft})))))))

(deftest listing-combines-status-with-an-extra-predicate
  (with-temp-ctx [ctx]
    (let [a (content/save! ctx :event (str (random-uuid))
                           {:title "Featured" :description "" :start_at 1000
                            :featured 1 :created_at 1})
          b (content/save! ctx :event (str (random-uuid))
                           {:title "Ordinary" :description "" :start_at 1000
                            :featured 0 :created_at 1})]
      (content/publish! ctx :event a)
      (content/publish! ctx :event b)
      (is (= [a] (mapv :id (content/live ctx :event {:where [:= :featured 1]})))))))

(deftest unknown-type-is-rejected
  (is (thrown? clojure.lang.ExceptionInfo (content/spec :widget))))

;; ---------------------------------------------------------------------------
;; Migration
;; ---------------------------------------------------------------------------

(deftest backfill-derives-status-from-the-old-columns
  (with-temp-ctx [ctx]
    (let [live-ev  (legacy-event! ctx "Sunday service" 1)
          draft-ev (legacy-event! ctx "Tentative retreat" 0)
          live-po  (legacy-post! ctx "Welcome back" 1750000000)
          draft-po (legacy-post! ctx "Half-written" nil)]

      (testing "before the backfill nothing has a status"
        (is (every? nil? (map :status (content/ls ctx :event)))))

      (is (pos? (content/backfill! ctx)))

      (is (= content/published (status-of ctx :event live-ev)))
      (is (= content/draft     (status-of ctx :event draft-ev)))
      (is (= content/published (status-of ctx :post live-po))
          "a post with a publication date was live")
      (is (= content/draft     (status-of ctx :post draft-po))
          "a NULL published_at was the old way of saying draft")

      (testing "published_at is filled in for tables that never had one"
        (is (= 1 (:published_at (content/get-one ctx :event live-ev)))
            "created_at is the closest true answer")
        (is (nil? (:published_at (content/get-one ctx :event draft-ev)))))

      (testing "the editorial date on a post is never overwritten"
        (is (= 1750000000 (:published_at (content/get-one ctx :post live-po))))))))

(deftest backfill-is-idempotent-and-leaves-new-rows-alone
  (with-temp-ctx [ctx]
    (legacy-event! ctx "Sunday service" 1)
    (content/backfill! ctx)
    (is (zero? (content/backfill! ctx)) "a second run touches nothing")

    (let [fresh (content/save! ctx :event (str (random-uuid))
                               {:title "Newly written" :description ""
                                :start_at 5000 :created_at 1})]
      (content/backfill! ctx)
      (is (= content/draft (status-of ctx :event fresh))
          "a draft written after the migration is not mistaken for un-backfilled")

      (content/publish! ctx :event fresh)
      (content/backfill! ctx)
      (is (= content/published (status-of ctx :event fresh))))))
