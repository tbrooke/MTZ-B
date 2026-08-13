(ns com.mtzion.model.normalize
  "Shared conversions between input (form params / imported EDN), epoch storage,
  and display.

  Two different timezone conventions are in play and must not be conflated:

  - **Datetime fields** (`event.start_at`, `event.end_at`) are wall-clock times
    at the church, parsed and rendered in America/New_York.
  - **Date-only fields** (`recur_until`, `published_at`, `file_date`,
    `sermon_date`) are stored as UTC midnight, so the epoch is a stable day
    marker rather than an instant carrying a local offset.

  Changing either convention silently reinterprets every existing row, so both
  are pinned by tests in test/com/mtzion/model/normalize_test.clj."
  (:require [clojure.string :as str]))

(def eastern (java.time.ZoneId/of "America/New_York"))

(defn now-epoch []
  (.getEpochSecond (java.time.Instant/now)))

;; ---------------------------------------------------------------------------
;; Parsing input -> epoch seconds
;; ---------------------------------------------------------------------------

(defn local-datetime->epoch
  "'2026-05-15T10:30' (datetime-local input, church wall-clock) -> epoch seconds.
  Returns nil on blank or unparseable input."
  [s]
  (when (seq s)
    (try (-> (java.time.LocalDateTime/parse s)
             (.atZone eastern)
             .toInstant
             .getEpochSecond)
         (catch Exception _ nil))))

(defn local-date->epoch
  "'2026-05-15' (date input) -> epoch seconds at UTC midnight.
  Returns nil on blank or unparseable input."
  [s]
  (when (seq s)
    (try (.getEpochSecond (java.time.Instant/parse (str s "T00:00:00Z")))
         (catch Exception _ nil))))

;; ---------------------------------------------------------------------------
;; epoch seconds -> input/display strings
;; ---------------------------------------------------------------------------

(defn epoch->local-datetime-str
  "Epoch seconds -> '2026-05-15T10:30' for a datetime-local input, in church time."
  [epoch]
  (when epoch
    (-> (java.time.Instant/ofEpochSecond epoch)
        (.atZone eastern)
        .toLocalDateTime
        .toString
        (subs 0 16))))

(defn epoch->date-str
  "Epoch seconds -> '2026-05-15' for a date input, read as UTC."
  [epoch]
  (when epoch
    (-> (java.time.Instant/ofEpochSecond epoch) .toString (subs 0 10))))

;; ---------------------------------------------------------------------------
;; Misc
;; ---------------------------------------------------------------------------

(defn slugify [s]
  (-> s str/lower-case (str/replace #"[^a-z0-9]+" "-") (str/replace #"^-|-$" "")))

(defn form-bool->int
  "Checkbox param value -> 1/0. An unchecked box submits nothing, so nil is false.

  Truthiness-based on purpose: this mirrors how HTML forms behave. Do NOT use it
  for typed input (EDN/JSON) where `false` is a real value that must not be
  confused with absence — use `edn-bool->int` there."
  [v]
  (if v 1 0))

(defn edn-bool->int
  "Typed boolean -> 1/0. Only literal true is true; nil, absent and false are 0."
  [v]
  (if (true? v) 1 0))

;; ---------------------------------------------------------------------------
;; Result-set keys
;; ---------------------------------------------------------------------------

(defn snake-keys
  "Rewrite one row's keys from kebab-case to snake_case, matching the DB column
  names used throughout the app. nil-safe."
  [row]
  (when row
    (into {} (map (fn [[k v]] [(keyword (str/replace (name k) "-" "_")) v])) row)))

(defn snake-keys-all
  "snake-keys across a collection of rows."
  [rows]
  (mapv snake-keys rows))
