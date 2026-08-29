(ns com.mtzion.model.event
  "Shared event query + recurrence logic used by both the admin panel and the
  public site. Keeping this in one place prevents the admin list and the public
  pages from disagreeing about which events are 'upcoming'.

  The expansion itself is pure — it reads `:skips` off the event rather than
  querying — so `with-skips` is the one function that touches the database, and
  every caller that expands occurrences should go through it. A cancelled week
  that still showed on the public calendar would be worse than no cancelling."
  (:require [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.model.normalize :as norm]))

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
  Non-recurring events are returned as-is when start_at falls in the range.

  `:skips` on ev, when present, is a set of occurrence epochs to leave out —
  the church cancelled that one week. Attaching it to the event keeps this
  namespace pure: the caller loads the exceptions, this decides what renders."
  [ev from-epoch to-epoch]
  (let [recurrence (:recurrence ev "none")
        base       (or (:start_at ev) 0)
        until      (:recur_until ev)]
    (if (= recurrence "none")
      (when (and (>= base from-epoch) (< base to-epoch)
                 (not (contains? (:skips ev) base)))
        [ev])
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
                  (let [nxt  (advance-recurrence ldt recurrence)
                        acc' (if (contains? (:skips ev) t)
                               acc
                               (conj acc (assoc ev :start_at t)))]
                    (if nxt (recur nxt acc' (inc n)) acc'))
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

;; ---------------------------------------------------------------------------
;; Describing a recurrence
;; ---------------------------------------------------------------------------

(def ^:private recurrence-words
  {"none"     "Does not repeat"
   "daily"    "Every day"
   "weekly"   "Every week"
   "biweekly" "Every two weeks"
   "monthly"  "Every month"
   "yearly"   "Every year"})

(defn- weekday-of [epoch]
  (-> (java.time.Instant/ofEpochSecond epoch)
      (java.time.LocalDate/ofInstant norm/eastern)
      .getDayOfWeek
      (.getDisplayName java.time.format.TextStyle/FULL java.util.Locale/US)))

(defn describe
  "A recurrence in words: \"Every week on Tuesday, until 21 December\".

  `weekly` in a table column tells an editor nothing about when the thing
  actually happens, which is the whole question they came to answer."
  [{:keys [recurrence start_at recur_until] :as _ev}]
  (let [r (or recurrence "none")]
    (if (= r "none")
      (recurrence-words r)
      (str (get recurrence-words r r)
           (when (and start_at (#{"weekly" "biweekly"} r))
             (str " on " (weekday-of start_at)))
           (when recur_until
             (str ", until "
                  (-> (java.time.Instant/ofEpochSecond recur_until)
                      (java.time.LocalDate/ofInstant java.time.ZoneOffset/UTC)
                      (.format (java.time.format.DateTimeFormatter/ofPattern "d MMMM yyyy")))))))))

;; ---------------------------------------------------------------------------
;; Cancelled occurrences
;; ---------------------------------------------------------------------------

(defn- exec [ctx honey]
  (norm/snake-keys-all (biff.sqlite/execute ctx honey)))

(defn skips-by-event
  "{event-id #{occurrence-epoch ...}} for the given events (all of them when
  `ids` is omitted)."
  ([ctx] (skips-by-event ctx nil))
  ([ctx ids]
   (->> (exec ctx (cond-> {:select :* :from :event_exception}
                    (seq ids) (assoc :where [:in :event_id (vec ids)])))
        (group-by :event_id)
        (reduce-kv (fn [m k v] (assoc m k (into #{} (map :occurrence_at) v))) {}))))

(defn with-skips
  "Attaches each event's cancelled occurrences before expansion. Call this on
  any collection of event rows you are about to expand."
  [ctx events]
  (let [by-id (skips-by-event ctx (keep :id events))]
    (mapv (fn [ev] (cond-> ev
                     (seq (get by-id (:id ev))) (assoc :skips (get by-id (:id ev)))))
          events)))

(defn skip!
  "Cancels one occurrence. Idempotent — the unique index means asking twice is
  the same as asking once."
  [ctx event-id occurrence-at]
  (exec ctx {:insert-into :event_exception
             :values [{:id (str (random-uuid))
                       :event_id event-id
                       :occurrence_at occurrence-at
                       :created_at (norm/now-epoch)}]
             :on-conflict [:event_id :occurrence_at]
             :do-nothing true})
  nil)

(defn unskip! [ctx event-id occurrence-at]
  (exec ctx {:delete-from :event_exception
             :where [:and [:= :event_id event-id]
                     [:= :occurrence_at occurrence-at]]})
  nil)

(defn skips-for [ctx event-id]
  (get (skips-by-event ctx [event-id]) event-id #{}))
