(ns com.mtzion.app.site-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [com.mtzion.app.site :as site]
            [com.mtzion.model.content :as content]
            [com.mtzion.model.outline :as outline]
            [com.mtzion.test-util :refer [with-temp-ctx]]))

(def ^:private list-rows #'site/list-rows)
(def ^:private slot-row  #'site/slot-row)
(def ^:private body-row  #'site/body-row)

(def ^:private worship-sections {:kind :list :slug "worship" :key "sections"})

(defn- add! [ctx page section]
  (let [resp (site/add-row (assoc ctx :path-params {:page page :section section}))]
    (is (= 303 (:status resp)))
    (last (str/split (get-in resp [:headers "location"]) #"/"))))

(defn- save! [ctx page section id params]
  (site/save (assoc ctx :params params
                    :path-params (cond-> {:page page :section section} id (assoc :id id)))))

(defn- titles [ctx] (mapv :title (list-rows ctx worship-sections)))

;; ---------------------------------------------------------------------------
;; Adding — the thing the whole pane exists for
;; ---------------------------------------------------------------------------

(deftest a-list-takes-as-many-sections-as-you-like
  (with-temp-ctx [ctx]
    (let [ids (doall (for [_ (range 5)] (add! ctx "worship" "sections")))]
      (is (= 5 (count (list-rows ctx worship-sections)))
          "nothing decides in advance how many sections a page has")
      (is (= (range 5) (map :sort_order (list-rows ctx worship-sections)))
          "each new one lands at the end")
      (is (= 5 (count (distinct ids)))))))

(deftest a-new-section-starts-as-a-draft
  (with-temp-ctx [ctx]
    (let [id (add! ctx "worship" "sections")]
      (is (= content/draft (:status (content/get-one ctx :feature id)))
          "adding a section must not put it on the site"))))

(deftest adding-to-a-non-list-is-refused
  (with-temp-ctx [ctx]
    (is (= 404 (:status (site/add-row (assoc ctx :path-params
                                             {:page "home" :section "hero"})))))))

;; ---------------------------------------------------------------------------
;; Ordering
;; ---------------------------------------------------------------------------

(deftest sections-move-up-and-down
  (with-temp-ctx [ctx]
    (let [a (add! ctx "worship" "sections")
          b (add! ctx "worship" "sections")
          c (add! ctx "worship" "sections")]
      (doseq [[id t] [[a "Alpha"] [b "Beta"] [c "Gamma"]]]
        (save! ctx "worship" "sections" id {:title t}))
      (is (= ["Alpha" "Beta" "Gamma"] (titles ctx)))

      (site/move (assoc ctx :params {:dir "up"}
                        :path-params {:page "worship" :section "sections" :id c}))
      (is (= ["Alpha" "Gamma" "Beta"] (titles ctx)))

      (site/move (assoc ctx :params {:dir "down"}
                        :path-params {:page "worship" :section "sections" :id a}))
      (is (= ["Gamma" "Alpha" "Beta"] (titles ctx))))))

(deftest moving-past-either-end-does-nothing
  (with-temp-ctx [ctx]
    (let [a (add! ctx "worship" "sections")
          b (add! ctx "worship" "sections")]
      (save! ctx "worship" "sections" a {:title "First"})
      (save! ctx "worship" "sections" b {:title "Second"})
      (site/move (assoc ctx :params {:dir "up"}
                        :path-params {:page "worship" :section "sections" :id a}))
      (is (= ["First" "Second"] (titles ctx)))
      (site/move (assoc ctx :params {:dir "down"}
                        :path-params {:page "worship" :section "sections" :id b}))
      (is (= ["First" "Second"] (titles ctx))))))

(deftest moving-renumbers-the-whole-run
  (testing "rows made before the console can share or lack a sort_order, so a
            straight swap of two values is not enough"
    (with-temp-ctx [ctx]
      (let [ids (doall (for [t ["A" "B" "C"]]
                         (let [id (add! ctx "worship" "sections")]
                           (save! ctx "worship" "sections" id {:title t})
                           id)))]
        (doseq [id ids] (content/save! ctx :feature id {:sort_order 0}))
        (site/move (assoc ctx :params {:dir "down"}
                          :path-params {:page "worship" :section "sections"
                                        :id (first ids)}))
        (is (= [0 1 2] (mapv :sort_order (list-rows ctx worship-sections)))
            "the run comes out densely and uniquely numbered")))))

;; ---------------------------------------------------------------------------
;; Slots and bodies
;; ---------------------------------------------------------------------------

(deftest saving-a-slot-creates-its-row-the-first-time
  (with-temp-ctx [ctx]
    (let [section {:kind :slot :slug "current-theme" :key "theme"}]
      (is (nil? (slot-row ctx section)))
      (save! ctx "worship" "theme" nil {:title "The Apostles' Creed"})
      (let [row (slot-row ctx section)]
        (is (= "The Apostles' Creed" (:title row)))
        (is (= content/draft (:status row)) "and it starts as a draft"))

      (save! ctx "worship" "theme" nil {:title "Advent 2026"})
      (is (= 1 (count (content/ls ctx :feature {:where [:= :page_slug "current-theme"]})))
          "saving again edits that row rather than making a second one the page would ignore"))))

(deftest saving-a-page-body-upserts-the-page-row
  (with-temp-ctx [ctx]
    (save! ctx "about" "body" nil {:body "<p>Hello</p>"})
    (let [row (body-row ctx {:slug "about"})]
      (is (= "<p>Hello</p>" (:body row)))
      (is (= "about" (:slug row))))
    (save! ctx "about" "body" nil {:body "<p>Changed</p>"})
    (is (= 1 (count (content/ls ctx :page {:where [:= :slug "about"]}))))
    (is (= "<p>Changed</p>" (:body (body-row ctx {:slug "about"}))))))

(deftest saving-a-page-body-keeps-its-nav-settings
  (with-temp-ctx [ctx]
    (content/save! ctx :page (str (random-uuid))
                   {:slug "about" :title "About Us" :nav_label "About"
                    :nav_order 2 :parent_slug nil :body "" :updated_at 1})
    (save! ctx "about" "body" nil {:body "<p>New words</p>"})
    (let [row (body-row ctx {:slug "about"})]
      (is (= "New words" (str/replace (:body row) #"</?p>" "")))
      (is (= "About" (:nav_label row)) "editing the text must not knock the page out of the menu")
      (is (= 2 (:nav_order row))))))

;; ---------------------------------------------------------------------------
;; Status and archive
;; ---------------------------------------------------------------------------

(deftest the-pill-publishes-one-section
  (with-temp-ctx [ctx]
    (let [a (add! ctx "worship" "sections")
          b (add! ctx "worship" "sections")]
      (site/status (assoc ctx :path-params {:page "worship" :section "sections" :id a}))
      (is (= content/published (:status (content/get-one ctx :feature a))))
      (is (= content/draft (:status (content/get-one ctx :feature b)))
          "publishing one section leaves its neighbours alone"))))

(deftest publishing-a-slot-with-no-row-yet-is-refused
  (with-temp-ctx [ctx]
    (is (= 404 (:status (site/status (assoc ctx :path-params
                                            {:page "worship" :section "theme"})))))))

(deftest archiving-a-section-takes-it-out-of-the-list
  (with-temp-ctx [ctx]
    (let [a (add! ctx "worship" "sections")]
      (save! ctx "worship" "sections" a {:title "Temporary"})
      (site/status (assoc ctx :path-params {:page "worship" :section "sections" :id a}))
      (site/archive (assoc ctx :path-params {:page "worship" :section "sections" :id a}))
      (is (empty? (list-rows ctx worship-sections)))
      (is (= content/archived (:status (content/get-one ctx :feature a)))
          "and it is on the Archive screen, not gone"))))

;; ---------------------------------------------------------------------------
;; Addressing
;; ---------------------------------------------------------------------------

(deftest an-unknown-leaf-is-a-404-not-a-crash
  (with-temp-ctx [ctx]
    (is (= 404 (:status (site/leaf (assoc ctx :path-params
                                          {:page "worship" :section "nonsense"})))))
    (is (= 404 (:status (site/leaf (assoc ctx :path-params
                                          {:page "nonsense" :section "sections"})))))))

(deftest every-leaf-in-the-tree-renders
  (with-temp-ctx [ctx]
    (doseq [entry outline/tree
            :let [pk (outline/page-key entry)]
            s (:sections entry)]
      (let [resp (site/leaf (assoc ctx :path-params {:page pk :section (:key s)}))]
        (is (= 200 (:status resp))
            (str pk "/" (:key s) " did not render"))))))
