(ns com.mtzion.content.doc-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [com.mtzion.content.doc :as doc]
            [com.mtzion.content.hiccup :as ch]
            [com.mtzion.model.nav :as model.nav]))

(defn- first-difference
  "Where two documents diverge, as a short report. Comparing 250-line strings
  with `=` produces an unreadable failure, and this test's whole job is to tell
  someone what to do about it."
  [committed generated]
  (let [a (str/split-lines committed)
        b (str/split-lines generated)]
    (some (fn [i]
            (let [x (get a i) y (get b i)]
              (when (not= x y)
                (format "first difference at line %d:\n  committed: %s\n  generated: %s"
                        (inc i) (pr-str x) (pr-str y)))))
          (range (max (count a) (count b))))))

(deftest contract-doc-is-current
  ;; This is the anti-drift mechanism. If it fails, a schema changed without the
  ;; contract being regenerated, which means Claude Desktop is being told rules
  ;; the importer no longer enforces.
  (let [committed (slurp doc/doc-path)
        generated (doc/render)
        diff      (first-difference committed generated)]
    (is (nil? diff)
        (str "CONTRACT.md is stale — run: clj -M:run content-doc\n" diff))))

(deftest doc-reflects-the-live-schemas
  (let [text (doc/render)]
    (testing "every allowed tag is documented"
      (doseq [tag (keys ch/allowed-tags)]
        (is (str/includes? text (str "`" tag "`"))
            (str tag " missing from the tag table"))))
    (testing "every valid parent section is documented"
      (doseq [slug model.nav/top-level-slugs]
        (is (str/includes? text (str "`:" slug "`"))
            (str slug " missing from the :parent list"))))
    (testing "all five item types are documented"
      (doseq [t ["event" "post" "page" "feature" "sermon"]]
        (is (str/includes? text (str "### `:type :" t "`")))))))

(deftest doc-warns-against-the-real-mistakes
  (let [text (doc/render)]
    (testing "the failures most likely to come back from an LLM"
      (is (str/includes? text "6:30 PM") "prose times")
      (is (str/includes? text "#inst") "tagged literals")
      (is (str/includes? text ":featured 1") "integer booleans")
      (is (str/includes? text "<p>text</p>") "HTML strings for bodies")
      (is (str/includes? text ":recurrence :weekly") "one item per week"))
    (testing "and the guidance that prevents them"
      (is (str/includes? text "leave the field out"))
      (is (str/includes? text "Reuse `:key`")))))

(deftest sha-changes-when-the-contract-changes
  (let [sha (doc/current-sha)]
    (is (re-matches #"[0-9a-f]{8}" sha))
    (is (str/includes? (doc/render) sha) "the doc carries its own sha")))
