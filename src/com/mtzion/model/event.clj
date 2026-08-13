(ns com.mtzion.model.event
  "Shared event query + recurrence logic used by both the admin panel and the
  public site. Keeping this in one place prevents the admin list and the public
  pages from disagreeing about which events are 'upcoming'."
  (:require [com.mtzion.model.normalize :as norm]))

(def now-epoch norm/now-epoch)

(defn upcoming-where
  "HoneySQL predicate for 'this event still has occurrences at or after now'.

  A non-recurring event qualifies while its start is in the future. A recurring
  event qualifies until its repeat-until date passes (or forever, if blank) —
  its original start_at is usually in the past and must not disqualify it."
  [now-ep]
  [:or
   [:and [:= :recurrence "none"] [:>= :start_at now-ep]]
   [:and [:not= :recurrence "none"]
    [:or [:is :recur_until nil] [:>= :recur_until now-ep]]]])

;; ---------------------------------------------------------------------------
;; Recurrence expansion
;; ---------------------------------------------------------------------------

(defn- ldt->epoch
  "Church wall-clock LocalDateTime -> epoch seconds.

  Stepping in LocalDateTime and resolving the offset per occurrence is what
  keeps a 6:30 PM weekly event at 6:30 PM across a DST change. Resolving in UTC
  instead (as this used to) shifted every occurrence by an hour in November."
  [^java.time.LocalDateTime ldt]
  (.toEpochSecond (.atZone ldt norm/eastern)))

(defn- epoch->local-date
  "UTC-midnight date-only epoch (recur_until) -> LocalDate."
  [epoch]
  (-> (java.time.Instant/ofEpochSecond epoch)
      (java.time.LocalDate/ofInstant java.time.ZoneOffset/UTC)))

(defn- advance-recurrence [^java.time.LocalDateTime ldt recurrence]
  (case recurrence
    "daily"    (.plusDays ldt 1)
    "weekly"   (.plusWeeks ldt 1)
    "biweekly" (.plusWeeks ldt 2)
    "monthly"  (.plusMonths ldt 1)
    "yearly"   (.plusYears ldt 1)
    nil))

(defn occurrences-in-range
  "Returns coll of ev maps (with :start_at set to the occurrence epoch) for all
  occurrences of ev within [from-epoch, to-epoch).
  Non-recurring events are returned as-is when start_at falls in the range."
  [ev from-epoch to-epoch]
  (let [recurrence (:recurrence ev "none")
        base       (or (:start_at ev) 0)
        until      (:recur_until ev)]
    (if (= recurrence "none")
      (when (and (>= base from-epoch) (< base to-epoch)) [ev])
      ;; +86400 because until is UTC midnight of its day: an occurrence later on
      ;; that same local day is still within range.
      (when (and (pos? base) (or (nil? until) (>= (+ until 86400) from-epoch)))
        (let [base-ldt   (java.time.LocalDateTime/ofInstant
                          (java.time.Instant/ofEpochSecond base)
                          norm/eastern)
              until-date (some-> until epoch->local-date)]
          (loop [ldt base-ldt acc [] n 0]
            (if (> n 3650)
              acc
              (let [t (ldt->epoch ldt)]
                (cond
                  ;; Compare by local date, not instant: "repeats until Dec 17"
                  ;; must include Dec 17's occurrence. Comparing instants excluded
                  ;; it, since an evening occurrence is past that day's UTC midnight.
                  (or (>= t to-epoch)
                      (and until-date (.isAfter (.toLocalDate ldt) until-date))) acc
                  (>= t from-epoch)
                  (let [nxt (advance-recurrence ldt recurrence)]
                    (if nxt
                      (recur nxt (conj acc (assoc ev :start_at t)) (inc n))
                      (conj acc (assoc ev :start_at t))))
                  :else
                  (let [nxt (advance-recurrence ldt recurrence)]
                    (if nxt (recur nxt acc (inc n)) acc)))))))))))

(defn expand-in-range [events from-epoch to-epoch]
  (sort-by :start_at
           (mapcat #(occurrences-in-range % from-epoch to-epoch) events)))

(defn next-occurrence
  "Returns ev with :start_at set to the next occurrence >= after-epoch,
  or nil if the event has no future occurrences."
  [ev after-epoch]
  (first (occurrences-in-range ev after-epoch (+ after-epoch (* 3650 86400)))))

(defn next-occurrences
  "Returns events (one entry per event) with :start_at set to their next
  occurrence >= after-epoch, sorted by that date."
  [events after-epoch]
  (->> events
       (keep #(next-occurrence % after-epoch))
       (sort-by :start_at)))
