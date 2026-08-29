(ns com.mtzion.app.calendar-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [com.mtzion.app.calendar :as cal]
            [com.mtzion.model.content :as content]
            [com.mtzion.model.event :as event]
            [com.mtzion.model.normalize :as norm]
            [com.mtzion.test-util :refer [with-temp-ctx]]))

(defn- dt [s] (norm/local-datetime->epoch s))
(defn- d  [s] (norm/local-date->epoch s))

(defn- make! [ctx m]
  (let [id (str (random-uuid))]
    (content/save! ctx :event id
                   (merge {:title "Bible study" :description "" :location ""
                           :start_at (dt "2026-09-01T18:30") :all_day 0
                           :recurrence "none" :featured 0 :created_at 1}
                          m))
    (content/publish! ctx :event id)
    id))

(defn- occ-times [ctx id from to]
  (->> (event/expand-in-range (event/with-skips ctx (content/ls ctx :event {:where [:= :id id]}))
                              from to)
       (mapv #(norm/epoch->local-datetime-str (:start_at %)))))

;; ---------------------------------------------------------------------------
;; Cancelling one occurrence of a series
;; ---------------------------------------------------------------------------

(deftest one-occurrence-can-be-cancelled
  (with-temp-ctx [ctx]
    (let [id (make! ctx {:recurrence "weekly" :recur_until (d "2026-09-29")})
          from (dt "2026-08-01T00:00") to (dt "2026-10-31T00:00")]
      (is (= ["2026-09-01T18:30" "2026-09-08T18:30" "2026-09-15T18:30"
              "2026-09-22T18:30" "2026-09-29T18:30"]
             (occ-times ctx id from to)))

      (event/skip! ctx id (dt "2026-09-15T18:30"))
      (is (= ["2026-09-01T18:30" "2026-09-08T18:30" "2026-09-22T18:30" "2026-09-29T18:30"]
             (occ-times ctx id from to))
          "the cancelled week is gone; the rest of the series is untouched")

      (event/unskip! ctx id (dt "2026-09-15T18:30"))
      (is (= 5 (count (occ-times ctx id from to))) "and it comes back"))))

(deftest cancelling-twice-is-harmless
  (with-temp-ctx [ctx]
    (let [id (make! ctx {:recurrence "weekly" :recur_until (d "2026-09-29")})]
      (event/skip! ctx id (dt "2026-09-15T18:30"))
      (event/skip! ctx id (dt "2026-09-15T18:30"))
      (is (= 1 (count (event/skips-for ctx id)))))))

(deftest a-one-off-event-can-be-cancelled-too
  (with-temp-ctx [ctx]
    (let [id (make! ctx {})]
      (event/skip! ctx id (dt "2026-09-01T18:30"))
      (is (empty? (occ-times ctx id (dt "2026-08-01T00:00") (dt "2026-10-01T00:00")))))))

(deftest skips-belong-to-their-own-event
  (with-temp-ctx [ctx]
    (let [a (make! ctx {:title "Bible study" :recurrence "weekly" :recur_until (d "2026-09-29")})
          b (make! ctx {:title "Choir" :recurrence "weekly" :recur_until (d "2026-09-29")})]
      (event/skip! ctx a (dt "2026-09-15T18:30"))
      (is (= 4 (count (occ-times ctx a (dt "2026-08-01T00:00") (dt "2026-10-31T00:00")))))
      (is (= 5 (count (occ-times ctx b (dt "2026-08-01T00:00") (dt "2026-10-31T00:00"))))
          "cancelling one series must not touch another at the same time"))))

(deftest the-handler-cancels-and-restores
  (with-temp-ctx [ctx]
    (let [id  (make! ctx {:recurrence "weekly" :recur_until (d "2026-09-29")})
          occ (str (dt "2026-09-15T18:30"))]
      (cal/calendar-skip (assoc ctx :path-params {:id id} :params {:occ occ}))
      (is (= 1 (count (event/skips-for ctx id))))
      (cal/calendar-unskip (assoc ctx :path-params {:id id} :params {:occ occ}))
      (is (empty? (event/skips-for ctx id))))))

(deftest a-junk-occurrence-parameter-is-ignored
  (with-temp-ctx [ctx]
    (let [id (make! ctx {:recurrence "weekly"})]
      (is (= 303 (:status (cal/calendar-skip
                           (assoc ctx :path-params {:id id} :params {:occ "not-a-number"})))))
      (is (empty? (event/skips-for ctx id))))))

;; ---------------------------------------------------------------------------
;; Describing a repeat
;; ---------------------------------------------------------------------------

(deftest a-repeat-reads-as-a-sentence
  (is (= "Does not repeat" (event/describe {:recurrence "none"})))
  (is (= "Every week on Tuesday" (event/describe {:recurrence "weekly"
                                                  :start_at (dt "2026-09-01T18:30")})))
  (is (= "Every week on Tuesday, until 29 September 2026"
         (event/describe {:recurrence "weekly" :start_at (dt "2026-09-01T18:30")
                          :recur_until (d "2026-09-29")})))
  (testing "a weekday only helps for weekly repeats"
    (is (= "Every month" (event/describe {:recurrence "monthly"
                                          :start_at (dt "2026-09-01T18:30")})))))

;; ---------------------------------------------------------------------------
;; The pane
;; ---------------------------------------------------------------------------

(deftest the-grid-marks-the-days-that-have-something-on
  (with-temp-ctx [ctx]
    (make! ctx {:title "Rally Day" :start_at (dt "2026-09-13T10:30")})
    (let [html (str (:body (cal/calendar (assoc ctx :query-params {"month" "2026-09"}))))]
      (is (str/includes? html "September 2026"))
      (is (str/includes? html "con-cal-pip") "the day carries a marker")
      (is (str/includes? html "month=2026-08") "and the arrows go somewhere")
      (is (str/includes? html "month=2026-10")))))

(deftest picking-a-day-lists-what-is-on-it
  (with-temp-ctx [ctx]
    (make! ctx {:title "Rally Day"    :start_at (dt "2026-09-13T10:30")})
    (make! ctx {:title "Pet blessing" :start_at (dt "2026-09-20T14:00")})
    (let [html (str (:body (cal/calendar (assoc ctx :query-params
                                                {"month" "2026-09" "day" "2026-09-13"}))))]
      (is (str/includes? html "Rally Day"))
      (is (not (str/includes? html "Pet blessing"))
          "only the chosen day's events are listed"))))

(deftest a-recurring-event-shows-every-week-in-the-grid
  (with-temp-ctx [ctx]
    (make! ctx {:recurrence "weekly" :start_at (dt "2026-09-01T18:30")})
    (let [html (str (:body (cal/calendar (assoc ctx :query-params {"month" "2026-09"}))))]
      (is (= 5 (count (re-seq #"con-cal-pips" html)))
          "five Tuesdays in September 2026"))))

(deftest drafts-appear-in-the-pane-but-not-on-the-site
  (with-temp-ctx [ctx]
    (let [id (make! ctx {:title "Tentative retreat" :start_at (dt "2026-09-13T09:00")})]
      (content/unpublish! ctx :event id)
      (let [html (str (:body (cal/calendar (assoc ctx :query-params
                                                  {"month" "2026-09" "day" "2026-09-13"}))))]
        (is (str/includes? html "Tentative retreat")
            "you can see what is lined up before it goes live"))
      (is (empty? (content/live ctx :event))))))

(deftest an-archived-event-leaves-the-calendar
  (with-temp-ctx [ctx]
    (let [id (make! ctx {:title "Cancelled thing" :start_at (dt "2026-09-13T09:00")})]
      (content/archive! ctx :event id)
      (let [html (str (:body (cal/calendar (assoc ctx :query-params
                                                  {"month" "2026-09" "day" "2026-09-13"}))))]
        (is (not (str/includes? html "Cancelled thing")))))))

(deftest the-editor-previews-what-a-repeat-produces
  (with-temp-ctx [ctx]
    (let [id (make! ctx {:recurrence "weekly" :recur_until (d "2026-09-29")})]
      (let [html (str (:body (cal/calendar (assoc ctx :path-params {:id id} :query-params {}))))]
        (is (str/includes? html "Next occurrences"))
        (is (str/includes? html "Every week on Tuesday"))
        (is (str/includes? html "Cancel this one"))))))

(deftest a-one-off-event-gets-no-repeat-preview
  (with-temp-ctx [ctx]
    (let [id (make! ctx {})]
      (is (not (str/includes? (str (:body (cal/calendar (assoc ctx :path-params {:id id}
                                                               :query-params {}))))
                              "Next occurrences"))))))

(deftest a-cancelled-occurrence-is-shown-as-cancelled
  (with-temp-ctx [ctx]
    (let [id (make! ctx {:recurrence "weekly" :recur_until (d "2026-09-29")})]
      (event/skip! ctx id (dt "2026-09-15T18:30"))
      (let [html (str (:body (cal/calendar (assoc ctx :path-params {:id id} :query-params {}))))]
        (is (str/includes? html "is-skipped") "struck through rather than hidden")
        (is (str/includes? html "Restore") "with a way back")))))

;; ---------------------------------------------------------------------------
;; Writes
;; ---------------------------------------------------------------------------

(deftest a-new-event-is-a-draft
  (with-temp-ctx [ctx]
    (let [resp (cal/calendar-create
                (assoc ctx :params {:title "Fall festival" :start_at "2026-10-03T17:00"}))
          id   (last (str/split (get-in resp [:headers "location"]) #"/"))]
      (is (= 303 (:status resp)))
      (is (= content/draft (:status (content/get-one ctx :event id)))))))

(deftest creating-the-same-event-twice-reuses-the-row
  (testing "(title, start_at) is UNIQUE — the importer relies on it, and a
            constraint error is not a useful thing to show an editor"
    (with-temp-ctx [ctx]
      (let [p {:title "Homecoming" :start_at "2026-10-11T10:30"}
            a (last (str/split (get-in (cal/calendar-create (assoc ctx :params p))
                                       [:headers "location"]) #"/"))
            b (last (str/split (get-in (cal/calendar-create (assoc ctx :params p))
                                       [:headers "location"]) #"/"))]
        (is (= a b))
        (is (= 1 (count (content/ls ctx :event))))))))

(deftest saving-an-event-never-changes-its-publish-state
  (with-temp-ctx [ctx]
    (let [id (make! ctx {:title "Choir returns"})]
      (is (= content/published (:status (content/get-one ctx :event id))))
      (cal/calendar-save (assoc ctx :path-params {:id id}
                                :params {:title "Choir returns (7pm)"
                                         :start_at "2026-09-01T19:00"}))
      (let [row (content/get-one ctx :event id)]
        (is (= "Choir returns (7pm)" (:title row)))
        (is (= content/published (:status row)))))))

(deftest featured-is-a-detail-not-a-publish-state
  (with-temp-ctx [ctx]
    (let [id (make! ctx {})]
      (cal/calendar-save (assoc ctx :path-params {:id id}
                                :params {:title "Bible study" :start_at "2026-09-01T18:30"
                                         :featured "1"}))
      (is (= 1 (:featured (content/get-one ctx :event id))))
      (is (= content/published (:status (content/get-one ctx :event id)))))))

(deftest the-pill-toggles-an-event
  (with-temp-ctx [ctx]
    (let [id (make! ctx {})]
      (cal/calendar-status (assoc ctx :path-params {:id id}))
      (is (= content/draft (:status (content/get-one ctx :event id))))
      (let [body (:body (cal/calendar-status (assoc ctx :path-params {:id id})))]
        (is (str/includes? body "con-pill--published"))
        (is (not (str/includes? body "<!DOCTYPE")))))))
