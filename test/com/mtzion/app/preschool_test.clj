(ns com.mtzion.app.preschool-test
  "The preschool page used to be entirely static. It now reads the CMS and falls
  back to the copy that ships with the design, so the two things that matter are:
  an untouched site looks exactly as it did, and an edited one actually changes."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [com.mtzion.app.preschool :as preschool]
            [com.mtzion.app.site :as site]
            [com.mtzion.content.defaults :as defaults]
            [com.mtzion.model.content :as content]
            [com.mtzion.model.outline :as outline]
            [com.mtzion.test-util :refer [with-temp-ctx]]))

(defn- page [ctx] (str (:body (preschool/preschool ctx))))

(defn- adopt! [ctx section]
  (site/adopt (assoc ctx :path-params {:page "preschool" :section section})))

;; ---------------------------------------------------------------------------
;; Nothing changes until somebody changes something
;; ---------------------------------------------------------------------------

(deftest an-untouched-preschool-page-shows-the-shipped-copy
  (with-temp-ctx [ctx]
    (let [html (page ctx)]
      (testing "every section still renders its designed text"
        (doseq [probe ["Where little ones "
                       "A note from our Director"
                       "Little Lambs" "Doves Class" "Shepherds"
                       "Morning circle"  ; & renders escaped, so probe short of it
                       "Play is the work"
                       "NC 5-Star Center"]]
          (is (str/includes? html probe)
              (str "the shipped copy for \"" probe "\" disappeared"))))
      (testing "emphasis inside a heading survives"
        (is (str/includes? html "<em>grow, play,</em>")
            "the designed headings italicise a phrase mid-sentence; *markers* keep that editable"))
      (testing "and the signature is split out of the meta line"
        (is (str/includes? html "Mrs. Karen Whitley"))
        (is (str/includes? html "Director · since 2011"))))))

(deftest the-classroom-cards-number-themselves
  (testing "the designed page numbers these from 02 — preserved deliberately rather
            than quietly corrected, because it is live copy"
    (with-temp-ctx [ctx]
      (let [html (page ctx)]
        (is (str/includes? html "02 — Toddlers"))
        (is (str/includes? html "03 — Preschool"))
        (is (str/includes? html "04 — Pre-Kindergarten"))))))

(deftest a-meta-line-splits-on-pipe-not-middot
  (testing "· appears inside the copy itself, so it cannot be the separator"
    (with-temp-ctx [ctx]
      (let [html (page ctx)]
        (is (str/includes? html "<span>Ages 2 – 3</span><span>T / Th · ½ day</span>")
            "two cells, and the second keeps its own middot")))))

;; ---------------------------------------------------------------------------
;; Adopting
;; ---------------------------------------------------------------------------

(deftest adopting-copies-the-shipped-rows-in
  (with-temp-ctx [ctx]
    (adopt! ctx "programs")
    (let [rows (content/ls ctx :feature {:where [:= :page_slug "ps-programs"]})]
      (is (= 3 (count rows)))
      (is (= ["Little Lambs" "Doves Class" "Shepherds"] (mapv :title (sort-by :sort_order rows))))
      (is (= [0 1 2] (mapv :sort_order (sort-by :sort_order rows))))
      (is (every? #(= content/draft (:status %)) rows)
          "adopted rows start as drafts, so nothing changes on the site yet"))

    (testing "the page still shows the shipped text while they are drafts"
      (is (str/includes? (page ctx) "Little Lambs")))))

(deftest adopting-twice-does-not-duplicate
  (with-temp-ctx [ctx]
    (adopt! ctx "values")
    (adopt! ctx "values")
    (is (= 4 (count (content/ls ctx :feature {:where [:= :page_slug "ps-values"]}))))))

(deftest once-published-the-database-wins
  (with-temp-ctx [ctx]
    (adopt! ctx "values")
    (let [rows (sort-by :sort_order (content/ls ctx :feature {:where [:= :page_slug "ps-values"]}))]
      (doseq [r rows] (content/publish! ctx :feature (:id r)))
      (content/save! ctx :feature (:id (first rows)) {:title "Faith, shared gently"})
      (let [html (page ctx)]
        (is (str/includes? html "Faith, shared gently") "the edit lands on the page")
        (is (not (str/includes? html "Faith, gently shared"))
            "and the shipped wording is gone — the section has been taken over")))))

(deftest values-renumber-when-reordered
  (with-temp-ctx [ctx]
    (adopt! ctx "values")
    (let [rows (sort-by :sort_order (content/ls ctx :feature {:where [:= :page_slug "ps-values"]}))]
      (doseq [r rows] (content/publish! ctx :feature (:id r)))
      (site/move (assoc ctx :params {:dir "up"}
                        :path-params {:page "preschool" :section "values"
                                      :id (:id (second rows))}))
      (let [html (page ctx)]
        (is (str/includes? html "01</div><h3>Play is the work")
            "the number follows the position, so a reorder renumbers rather than leaving gaps")))))

;; ---------------------------------------------------------------------------
;; Slots
;; ---------------------------------------------------------------------------

(deftest a-slot-editor-prefills-from-the-shipped-copy
  (with-temp-ctx [ctx]
    (let [html (str (:body (site/leaf (assoc ctx :path-params
                                             {:page "preschool" :section "hero"}))))]
      (is (str/includes? html "A Nurturing Christian Early Childhood Program")
          "the form opens with the design's own words, so saving adopts rather than blanks it")
      (is (str/includes? html "save it to take it over")))))

(deftest saving-a-prefilled-slot-adopts-it
  (with-temp-ctx [ctx]
    (site/save (assoc ctx :params {:title "Where little ones grow and play."
                                   :subtitle "A Christian Preschool"
                                   :body "Serving China Grove since 1989."}
                      :path-params {:page "preschool" :section "hero"}))
    (let [row (first (content/ls ctx :feature {:where [:= :page_slug "ps-hero"]}))]
      (is (= "A Christian Preschool" (:subtitle row)))
      (is (= content/draft (:status row)))
      (content/publish! ctx :feature (:id row))
      (is (str/includes? (page ctx) "A Christian Preschool")))))

(deftest the-hero-keeps-its-artwork-when-no-image-is-set
  (with-temp-ctx [ctx]
    (is (str/includes? (page ctx) "f37fb815-ddd2-4aea-c674-2dac97b18800")
        "an unset image must not leave the hero with a broken img")))

;; ---------------------------------------------------------------------------
;; The tree
;; ---------------------------------------------------------------------------

(deftest preschool-has-its-own-tree-behind-the-flip
  (is (= 1 (count outline/preschool-tree)))
  (is (= :preschool (outline/site-of (first outline/preschool-tree))))
  (is (every? #(= :church (outline/site-of %)) outline/church-tree))
  (testing "the flip offers exactly the two sites"
    (is (= [:church :preschool] (mapv :key outline/sites))))
  (testing "page keys stay unique across both trees, so a leaf URL is unambiguous"
    (let [ks (map outline/page-key outline/tree)]
      (is (= (count ks) (count (distinct ks)))))))

(deftest every-preschool-leaf-declares-shipped-copy
  (doseq [s (:sections (first outline/preschool-tree))
          :when (:defaults? s)]
    (is (seq (defaults/rows (:slug s)))
        (str (:label s) " claims shipped copy for \"" (:slug s) "\" but there is none"))))

(deftest the-flip-renders-the-right-tree
  (with-temp-ctx [ctx]
    (let [church (str (:body (site/site (assoc ctx :query-params {}))))
          presch (str (:body (site/site (assoc ctx :query-params {"site" "preschool"}))))]
      (is (str/includes? church "Worship"))
      (is (not (str/includes? church "Director's note")))
      (is (str/includes? presch "Director's note"))
      (is (not (str/includes? presch "Outreach"))))))
