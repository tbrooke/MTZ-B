(ns com.mtzion.ui.sections-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [com.mtzion.app.home-sections :as home]
            [com.mtzion.ui.sections :as sections]
            [lambdaisland.hiccup :as hiccup]))

(defn- render [form] (hiccup/render form {:doctype? false}))

;; The body an editor actually produced in the Site pane, verbatim from the
;; production row — a Tiptap document with a blank line in the middle.
(def ^:private edited-body
  (str "<p>One service. Scripture, prayer, and song beneath the windows that "
       "have watched over this congregation since 1910 — </p><p></p>"
       "<p>This week “Mercy Me, I need the Lord”</p>"))

(deftest prose-renders-html-as-html
  (testing "an edited body is markup, not text"
    (let [out (render (sections/prose edited-body))]
      (is (str/includes? out "<p>One service."))
      (testing "and the closing tag never reaches the reader as characters"
        (is (not (str/includes? out "&lt;/p&gt;")))
        (is (not (str/includes? out "&lt;p&gt;")))))))

(deftest prose-still-renders-a-plain-default
  (testing "a shipped default is a sentence, and must not be treated as markup"
    (let [out (render (sections/prose "One service. Scripture, prayer, and song."))]
      (is (str/includes? out "<p>One service. Scripture, prayer, and song.</p>"))))
  (testing "text containing an angle bracket is escaped, not executed"
    (is (str/includes? (render (sections/prose "5 < 6 & rising"))
                       "5 &lt; 6 &amp; rising")))
  (testing "nothing at all renders nothing"
    (is (nil? (sections/prose nil)))
    (is (nil? (sections/prose "")))))

(deftest prose-keeps-the-slot-styling
  (testing "the designed typography is carried on the wrapper, both ways"
    (doseq [body [edited-body "a plain default"]]
      (is (str/includes? (render (sections/prose {:style "font-size: 18px;"} body))
                         "font-size: 18px;")
          (pr-str body))))
  (testing "an html body gets the class the stylesheet targets"
    (is (str/includes? (render (sections/prose {:style "x"} edited-body)) "mtz-rich"))))

(deftest plain-flattens-to-one-line
  (testing "markup becomes words, and the paragraph break becomes a space"
    (is (= (str "One service. Scripture, prayer, and song beneath the windows "
                "that have watched over this congregation since 1910 — "
                "This week “Mercy Me, I need the Lord”")
           (sections/plain edited-body))))
  (testing "entities are decoded rather than shown"
    (is (= "Bread & Wine" (sections/plain "<p>Bread &amp; Wine</p>"))))
  (testing "a plain string is left alone"
    (is (= "Already plain." (sections/plain "Already plain."))))
  (testing "an empty document is nothing, not an empty line"
    (is (nil? (sections/plain "<p></p>")))
    (is (nil? (sections/plain nil)))))

;; ---------------------------------------------------------------------------
;; The bug as it was reported: the Sanctuary block on the home page
;; ---------------------------------------------------------------------------

(deftest sanctuary-block-does-not-show-its-own-markup
  (let [out (render (home/worship-sanctuary-section
                     {:body edited-body :subtitle "10:30 AM · Every Sunday"}))]
    (testing "the words are there"
      (is (str/includes? out "One service."))
      (is (str/includes? out "Mercy Me, I need the Lord")))
    (testing "and the tags are not"
      (is (not (str/includes? out "&lt;/p&gt;"))))))

(deftest sanctuary-block-still-renders-with-no-cms-row
  (testing "the shipped copy survives — this is what an unedited site shows"
    (let [out (render (home/worship-sanctuary-section nil))]
      (is (str/includes? out "One service."))
      (is (not (str/includes? out "&lt;"))))))

;; ---------------------------------------------------------------------------
;; The same defect, second site: the outreach cards
;; ---------------------------------------------------------------------------

(deftest outreach-tiles-do-not-show-markup
  (let [out (render (home/outreach-section
                     [{:title "Meals on Wheels Rowan"
                       :subtitle "Weekday meals"
                       :body "<p>Hot meals delivered each weekday.</p>"}]))]
    (is (str/includes? out "Hot meals delivered each weekday."))
    (is (not (str/includes? out "&lt;p&gt;")))))
