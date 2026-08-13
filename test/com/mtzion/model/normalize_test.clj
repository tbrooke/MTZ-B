(ns com.mtzion.model.normalize-test
  "Pins the two timezone conventions. These are not style preferences: changing
  either one silently reinterprets every row already in the database, and the
  import contract promises Claude Desktop what a time string means."
  (:require [clojure.test :refer [deftest is testing]]
            [com.mtzion.model.normalize :as norm]))

(deftest datetime-fields-are-church-local
  (testing "a datetime-local input is read as America/New_York, not UTC"
    ;; 2026-05-15 10:30 EDT (UTC-4) = 14:30 UTC = 1778855400
    (is (= 1778855400 (norm/local-datetime->epoch "2026-05-15T10:30"))))
  (testing "round-trips back to the same wall-clock string"
    (is (= "2026-05-15T10:30"
           (norm/epoch->local-datetime-str
            (norm/local-datetime->epoch "2026-05-15T10:30")))))
  (testing "standard time uses the -05:00 offset"
    ;; 2026-01-15 10:30 EST (UTC-5) = 15:30 UTC
    (let [e (norm/local-datetime->epoch "2026-01-15T10:30")]
      (is (= "2026-01-15T15:30"
             (subs (str (java.time.LocalDateTime/ofInstant
                         (java.time.Instant/ofEpochSecond e)
                         java.time.ZoneOffset/UTC))
                   0 16)))
      (is (= "2026-01-15T10:30" (norm/epoch->local-datetime-str e)))))
  (testing "blank and unparseable input yield nil rather than throwing"
    (is (nil? (norm/local-datetime->epoch "")))
    (is (nil? (norm/local-datetime->epoch nil)))
    (is (nil? (norm/local-datetime->epoch "August 13 at 6:30pm")))))

(deftest date-only-fields-are-utc-midnight
  (testing "a date input is stored as UTC midnight, NOT church-local midnight"
    (is (= 1778803200 (norm/local-date->epoch "2026-05-15")))
    (is (zero? (mod (norm/local-date->epoch "2026-05-15") 86400))
        "a UTC-midnight epoch is an exact multiple of a day"))
  (testing "round-trips"
    (is (= "2026-05-15" (norm/epoch->date-str (norm/local-date->epoch "2026-05-15")))))
  (testing "blank and unparseable input yield nil"
    (is (nil? (norm/local-date->epoch "")))
    (is (nil? (norm/local-date->epoch nil)))
    (is (nil? (norm/local-date->epoch "not-a-date")))))

(deftest the-two-conventions-are-genuinely-different
  ;; If someone "simplifies" these into one function, this fails.
  (is (not= (norm/local-date->epoch "2026-05-15")
            (norm/local-datetime->epoch "2026-05-15T00:00"))
      "date-only midnight is UTC; datetime midnight is Eastern — 4h apart in May"))

(deftest booleans
  (testing "form checkboxes are truthiness-based (absent means unchecked)"
    (is (= 1 (norm/form-bool->int "1")))
    (is (= 1 (norm/form-bool->int "on")))
    (is (= 0 (norm/form-bool->int nil))))
  (testing "typed EDN booleans distinguish false from absent"
    (is (= 1 (norm/edn-bool->int true)))
    (is (= 0 (norm/edn-bool->int false)))
    (is (= 0 (norm/edn-bool->int nil)))
    (is (= 0 (norm/edn-bool->int "true"))
        "a string is not a boolean — the import schema must reject it upstream")))

(deftest slugify
  (is (= "back-to-school-blessing" (norm/slugify "  Back-to-School Blessing!  ")))
  (is (= "vbs-2026" (norm/slugify "VBS 2026")))
  (testing "apostrophes become separators rather than being dropped"
    ;; Existing behaviour, pinned deliberately: every non-alphanumeric run
    ;; collapses to a dash, so "John's" yields "john-s" and not "johns".
    ;; Changing this would alter slugs generated for new content, so it is a
    ;; product decision rather than a cleanup.
    (is (= "john-s-river-valley-camp" (norm/slugify "John's River Valley Camp")))))

(deftest snake-keys
  (is (= {:page_slug "home" :show_on_home 1}
         (norm/snake-keys {:page-slug "home" :show-on-home 1})))
  (is (nil? (norm/snake-keys nil)) "nil-safe for single-row lookups")
  (is (= [{:start_at 1} {:start_at 2}]
         (norm/snake-keys-all [{:start-at 1} {:start-at 2}]))))
