(ns com.mtzion.content.hiccup-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [com.mtzion.content.hiccup :as ch]))

(defn- errs [nodes] (map :error (ch/explain nodes)))

;; ---------------------------------------------------------------------------
;; Acceptance
;; ---------------------------------------------------------------------------

(deftest accepts-what-tiptap-produces
  (testing "the editor's whole vocabulary validates"
    (is (ch/valid? [[:p "Plain text."]
                    [:h2 "A heading"] [:h3 "Sub"] [:h4 "Sub-sub"]
                    [:p [:strong "bold"] " " [:em "italic"] " " [:s "struck"] " " [:code "x"]]
                    [:ul [:li "one"] [:li "two"]]
                    [:ol {:start 3} [:li "three"]]
                    [:blockquote [:p "quoted"]]
                    [:pre [:code "(inc 1)"]]
                    [:hr] [:p "after" [:br] "break"]
                    [:a {:href "/contact" :title "t"} "link"]
                    [:img {:src "https://imagedelivery.net/abc/def/public" :alt "photo"}]
                    [:table [:thead [:tr [:th {:scope "col"} "H"]]]
                     [:tbody [:tr [:td {:colspan 2} "cell"]]]]])))
  (testing "an empty fragment is valid"
    (is (ch/valid? [])))
  (testing "safe link targets"
    (is (ch/valid? [[:a {:href "https://example.com"} "x"]]))
    (is (ch/valid? [[:a {:href "mailto:a@b.com"} "x"]]))
    (is (ch/valid? [[:a {:href "tel:+17048571169"} "x"]]))
    (is (ch/valid? [[:a {:href "#section"} "x"]]))
    (is (ch/valid? [[:img {:src "/images/local.jpg" :alt "a"}]]))))

;; ---------------------------------------------------------------------------
;; The attacks that matter
;; ---------------------------------------------------------------------------

(deftest rejects-raw-html-escape-hatch
  (testing "::hiccup/unsafe-html would render arbitrary HTML verbatim"
    (is (= [:namespaced-tag]
           (errs [[:lambdaisland.hiccup/unsafe-html "<script>alert(1)</script>"]])))))

(deftest rejects-script-and-unknown-tags
  (is (= [:disallowed-tag] (errs [[:script "alert(1)"]])))
  (is (= [:disallowed-tag] (errs [[:iframe {:src "https://evil.test"}]])))
  (is (= [:disallowed-tag] (errs [[:div "generic container"]])))
  (is (= [:disallowed-tag] (errs [[:h1 "competes with the page title"]])))
  (is (= [:disallowed-tag] (errs [[:style "body{display:none}"]]))))

(deftest rejects-event-handlers
  ;; The on* tripwire is checked before the allowlist, so these report the more
  ;; specific :event-handler-attr rather than a generic :disallowed-attr.
  (is (= [:event-handler-attr] (errs [[:a {:href "/x" :onclick "steal()"} "x"]])))
  (is (= [:event-handler-attr] (errs [[:img {:src "/images/a.jpg" :onerror "steal()"}]])))
  (testing "fires even for a tag with no allowlisted attrs"
    (is (= [:event-handler-attr] (errs [[:p {:onmouseover "x"} "y"]])))))

(deftest rejects-other-unknown-attrs
  (is (= [:disallowed-attr] (errs [[:a {:href "/x" :download "f"} "x"]])))
  (is (= [:disallowed-attr] (errs [[:p {:href "/x"} "y"]]))))

(deftest rejects-dangerous-urls
  (is (= [:dangerous-url] (errs [[:a {:href "javascript:alert(1)"} "x"]])))
  (is (= [:dangerous-url] (errs [[:img {:src "data:text/html;base64,PHNjcmlwdD4="}]])))
  (is (= [:dangerous-url] (errs [[:a {:href "vbscript:msgbox"} "x"]])))
  (testing "obfuscation with embedded whitespace/control chars is caught"
    (is (= [:dangerous-url] (errs [[:a {:href "java\tscript:alert(1)"} "x"]])))
    (is (= [:dangerous-url] (errs [[:a {:href "  JaVaScRiPt:alert(1)"} "x"]]))))
  (testing "an unknown scheme is rejected rather than passed through"
    (is (= [:bad-url-scheme] (errs [[:a {:href "ftp://host/x"} "x"]])))))

(deftest rejects-offsite-images
  (is (= [:offsite-image] (errs [[:img {:src "https://tracker.test/pixel.gif"}]])))
  (testing "the configured host is allowed"
    (is (ch/valid? [[:img {:src "https://pub-abc.r2.dev/a.jpg"}]]
                   {:image-hosts ["https://pub-abc.r2.dev/"]}))))

(deftest rejects-style-and-non-scalar-attrs
  (testing "map-valued :style reaches Garden, so it must never validate"
    (is (= [:disallowed-attr] (errs [[:p {:style {:color "red"}} "x"]]))))
  (testing "even on a tag where the attr is otherwise allowed, values stay scalar"
    (is (= [:non-scalar-attr] (errs [[:p {:class ["a" "b"]} "x"]])))
    (is (= [:non-scalar-attr] (errs [[:td {:colspan [1 2]} "x"]])))
    (is (= [:non-scalar-attr] (errs [[:td {:colspan -1} "x"]])))))

(deftest rejects-shorthand-tags
  (testing "shorthand hides classes from the attribute check"
    (is (= [:shorthand-tag] (errs [[:p.danger "x"]])))
    (is (= [:shorthand-tag] (errs [[:p#anchor "x"]])))))

(deftest enforces-structural-limits
  (testing "depth"
    (let [deep (reduce (fn [acc _] [:ul [:li acc]]) [:p "bottom"] (range 25))]
      (is (some #{:too-deep} (errs [deep])))))
  (testing "node count"
    (is (some #{:too-large} (errs (vec (repeat 3000 [:p "x"])))))))

(deftest rejects-malformed-input
  (is (= [:not-a-fragment] (errs "just a string")))
  (is (= [:not-a-fragment] (errs {:type :para})))
  (is (= [:tag-not-keyword] (errs [["p" "x"]])))
  (is (= [:nil-node] (errs [[:p nil]]))))

;; ---------------------------------------------------------------------------
;; Error reporting quality
;; ---------------------------------------------------------------------------

(deftest error-paths-point-at-the-problem
  (let [[e] (ch/explain [[:p "fine"] [:p "fine"] [:p [:a {:href "/x" :onclick "x"} "bad"]]])]
    (is (= :event-handler-attr (:error e)))
    (is (= [2 1 1 :onclick] (:path e))
        "node 2 -> child at position 1 -> its attr map at position 1 -> :onclick"))
  (testing "the report tells the author what IS allowed"
    (let [[e] (ch/explain [[:a {:href "/x" :download "f"} "bad"]])]
      (is (= :disallowed-attr (:error e)))
      (is (contains? (set (:allowed e)) :href))))
  (testing "several problems are all reported, not just the first"
    (is (= 3 (count (ch/explain [[:script "a"] [:p {:onclick "b"} "c"]
                                 [:a {:href "javascript:d"} "e"]]))))))

;; ---------------------------------------------------------------------------
;; Rendering
;; ---------------------------------------------------------------------------

(deftest renders-valid-fragments
  (is (= "<p>Hello</p>" (ch/->html [[:p "Hello"]])))
  (is (= "<p>a</p><p>b</p>" (ch/->html [[:p "a"] [:p "b"]]))
      "a fragment is a list to hiccup, not a vector")
  (is (= "" (ch/->html [])))
  (testing "text and attribute values are escaped"
    (is (= "<p>a &lt; b &amp; c</p>" (ch/->html [[:p "a < b & c"]])))
    (is (str/includes? (ch/->html [[:a {:href "/x?a=1&b=2"} "y"]]) "&amp;")))
  (testing "void elements are emitted self-closing"
    (is (= "<hr />" (ch/->html [[:hr]])))
    (is (str/starts-with? (ch/->html [[:img {:src "/images/a.jpg" :alt "a"}]]) "<img"))))

(deftest render-refuses-invalid-input
  (testing "->html must not be a way to bypass validation"
    (is (thrown? clojure.lang.ExceptionInfo (ch/->html [[:script "alert(1)"]])))
    (is (thrown? clojure.lang.ExceptionInfo
                 (ch/->html [[:lambdaisland.hiccup/unsafe-html "<b>x</b>"]])))
    (let [e (try (ch/->html [[:script "x"]]) (catch clojure.lang.ExceptionInfo ex ex))]
      (is (seq (:errors (ex-data e))) "the thrown data carries the explain report"))))

(deftest output-round-trips-through-the-editor-vocabulary
  ;; What we store must be what Tiptap would have stored for the same document,
  ;; so imported content opens and re-saves in the admin without surprise.
  (is (= (str "<h2>Heading</h2><p>Some <strong>bold</strong> text.</p>"
              "<ul><li>one</li><li>two</li></ul>")
         (ch/->html [[:h2 "Heading"]
                     [:p "Some " [:strong "bold"] " text."]
                     [:ul [:li "one"] [:li "two"]]]))))
