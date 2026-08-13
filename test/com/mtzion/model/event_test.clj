(ns com.mtzion.model.event-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.mtzion.model.event :as event]
            [com.mtzion.model.normalize :as norm]))

(defn- at
  "Church wall-clock string -> epoch, matching how the admin form stores times."
  [s]
  (norm/local-datetime->epoch s))

(defn- wall-clock
  "epoch -> 'YYYY-MM-DDTHH:MM' in church time, i.e. what a visitor sees."
  [epoch]
  (norm/epoch->local-datetime-str epoch))

(deftest weekly-recurrence-holds-its-wall-clock-across-dst
  ;; US DST ended 2026-11-01. A 6:30 PM Thursday rehearsal must stay 6:30 PM on
  ;; both sides of that boundary. Expanding in UTC (as this used to) slid every
  ;; occurrence after the change to 5:30 PM.
  (let [ev    {:title "Handbell Rehearsal"
               :start_at (at "2026-10-22T18:30")
               :recurrence "weekly"
               :recur_until (norm/local-date->epoch "2026-11-19")}
        times (->> (event/occurrences-in-range ev (at "2026-10-01T00:00") (at "2026-12-01T00:00"))
                   (map (comp wall-clock :start_at)))]
    (is (= ["2026-10-22T18:30" "2026-10-29T18:30"   ; EDT
            "2026-11-05T18:30" "2026-11-12T18:30"   ; EST — same wall clock
            "2026-11-19T18:30"]
           times))
    (testing "every occurrence reads 18:30 regardless of offset"
      (is (every? #(clojure.string/ends-with? % "T18:30") times)))))

(deftest recur-until-includes-its-own-day
  ;; "repeats until Nov 19" must include Nov 19. Comparing instants excluded it,
  ;; because an evening occurrence is past that day's UTC midnight.
  (let [ev {:title "Bible Study"
            :start_at (at "2026-11-05T19:00")
            :recurrence "weekly"
            :recur_until (norm/local-date->epoch "2026-11-19")}
        days (->> (event/occurrences-in-range ev (at "2026-11-01T00:00") (at "2026-12-01T00:00"))
                  (map (comp #(subs % 0 10) wall-clock :start_at)))]
    (is (= ["2026-11-05" "2026-11-12" "2026-11-19"] days)
        "the until date itself is included")))

(deftest non-recurring-events-pass-through
  (let [ev {:title "Back-to-School Blessing" :start_at (at "2026-08-16T10:30")}]
    (is (= 1 (count (event/occurrences-in-range ev (at "2026-08-01T00:00") (at "2026-09-01T00:00")))))
    (is (empty? (event/occurrences-in-range ev (at "2026-09-01T00:00") (at "2026-10-01T00:00"))))))

(deftest next-occurrences-dates-recurring-events-forward
  ;; A weekly event's stored start_at is in the past by design; the admin list
  ;; and the public page must both show its NEXT date, not its first.
  (let [ev   {:title "Pickleball"
              :start_at (at "2026-06-01T17:30")
              :recurrence "weekly"}
        now  (at "2026-08-05T12:00")
        [nx] (event/next-occurrences [ev] now)]
    (is (= "2026-08-10T17:30" (wall-clock (:start_at nx)))
        "next Monday at the same wall-clock time")))

(deftest upcoming-where-keeps-recurring-events
  (let [now (at "2026-08-05T12:00")
        w   (event/upcoming-where now)]
    (is (= :or (first w)))
    (is (some #(= [:= :recurrence "none"] (second %)) (rest w))
        "non-recurring events are filtered by start_at")))
