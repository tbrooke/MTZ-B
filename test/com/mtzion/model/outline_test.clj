(ns com.mtzion.model.outline-test
  "The drift guard.

  The outline is a declaration about code that lives somewhere else: it claims
  `/worship` renders sections filed under `worship`, that Home's hero is
  `home-hero`. Nothing stops those two drifting apart except a test that
  actually renders the page and looks.

  So that is what this does. For every list the tree offers, it puts a section
  into the database and asserts the page shows it. A leaf that promises an
  editor whose text never appears is the failure this catches — and it caught a
  real one: the tree originally offered a Page body editor on six pages whose
  templates never read `page.body` at all."
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [com.mtzion.app.about :as about]
            [com.mtzion.app.activities :as activities]
            [com.mtzion.app.contact :as contact]
            [com.mtzion.app.events :as events]
            [com.mtzion.app.landing :as landing]
            [com.mtzion.app.news :as news]
            [com.mtzion.app.outreach :as outreach]
            [com.mtzion.app.preschool :as preschool]
            [com.mtzion.app.worship :as worship]
            [com.mtzion.model.content :as content]
            [com.mtzion.model.normalize :as normalize]
            [com.mtzion.model.outline :as outline]
            [com.mtzion.test-util :refer [with-temp-ctx]]))

(def ^:private page-handlers
  "Which handler actually renders each top-level entry of the tree."
  {"home"       #'landing/home
   "about"      #'about/about
   "worship"    #'worship/worship
   "events"     #'events/events
   "activities" #'activities/activities
   "news"       #'news/news
   "outreach"   #'outreach/outreach
   "contact"    #'contact/contact-get
   "preschool"  #'preschool/preschool})

(defn- render [ctx pk]
  (when-let [h (get page-handlers pk)]
    (str (:body (h ctx)))))

(defn- add-section!
  "Every probe carries an image id. Home's \"Always at Mt. Zion\" cards are
  image-only by design — a row without one is deliberately skipped, and its
  title becomes the img alt — so a probe with no image would look like drift
  when it is really the template working as intended."
  [ctx slug title body]
  (let [id (str (random-uuid))]
    (content/save! ctx :feature id
                   {:page_slug slug :title title :subtitle "" :body body
                    :image_id "probe-image-id"
                    :sort_order 0
                    :created_at (normalize/now-epoch)
                    :updated_at (normalize/now-epoch)})
    (content/publish! ctx :feature id)
    id))

;; ---------------------------------------------------------------------------
;; Shape
;; ---------------------------------------------------------------------------

(deftest every-entry-has-a-known-handler
  (doseq [entry outline/tree
          :let [pk (outline/page-key entry)]]
    (is (contains? page-handlers pk)
        (str "the tree offers " pk " but this test does not know how to render it"))))

(deftest leaf-keys-are-unique-within-a-page
  (doseq [entry outline/tree
          :let [ks (map :key (:sections entry))]]
    (is (= (count ks) (count (distinct ks)))
        (str (:label entry) " has two leaves with the same key — one would be unreachable"))))

(deftest every-leaf-is-addressable
  (doseq [entry outline/tree
          :let [pk (outline/page-key entry)]
          s (:sections entry)]
    (is (= s (outline/find-section pk (:key s)))
        (str pk "/" (:key s) " does not round-trip through find-section"))
    (is (contains? #{:slot :list :body :link :static} (:kind s))
        (str pk "/" (:key s) " has an unknown kind"))
    (when (#{:slot :list :body} (:kind s))
      (is (seq (:slug s)) (str pk "/" (:key s) " needs a slug")))
    (when (= :link (:kind s))
      (is (seq (:goto s)) (str pk "/" (:key s) " needs somewhere to go")))))

(deftest lists-do-not-share-a-slug-with-a-slot
  (testing "a list and a slot on the same slug would fight over the same rows"
    (let [by-kind (group-by :kind (mapcat :sections outline/tree))]
      (is (empty? (set/intersection
                   (set (map :slug (:list by-kind)))
                   (set (map :slug (:slot by-kind)))))))))

;; ---------------------------------------------------------------------------
;; The guard that matters
;; ---------------------------------------------------------------------------

(deftest every-declared-list-actually-renders
  (doseq [entry outline/tree
          :let [pk (outline/page-key entry)]
          s     (:sections entry)
          :when (= :list (:kind s))]
    (with-temp-ctx [ctx]
      (let [marker (str "OutlineProbe" (str/replace (:slug s) #"-" ""))]
        (add-section! ctx (:slug s) marker "<p>probe body</p>")
        (let [html (render ctx pk)]
          (is (some? html) (str pk " has no handler"))
          (is (str/includes? (str html) marker)
              (str "\"" (:label s) "\" on " (:label entry)
                   " offers an editor for page_slug \"" (:slug s)
                   "\", but nothing filed there appears on " (:path entry)
                   " — the tree is promising something the template does not do")))))))

(deftest a-draft-section-does-not-render
  (doseq [entry outline/tree
          :let [pk (outline/page-key entry)]
          s     (:sections entry)
          :when (= :list (:kind s))]
    (with-temp-ctx [ctx]
      (let [marker (str "DraftProbe" (str/replace (:slug s) #"-" ""))
            id     (add-section! ctx (:slug s) marker "<p>probe</p>")]
        (content/unpublish! ctx :feature id)
        (is (not (str/includes? (str (render ctx pk)) marker))
            (str (:label entry) " / " (:label s) " leaks a draft onto the public page"))))))

(deftest sections-render-in-the-editors-order
  (with-temp-ctx [ctx]
    (let [slug "worship"]
      (doseq [[i t] (map-indexed vector ["ProbeAlpha" "ProbeBeta" "ProbeGamma"])]
        (let [id (str (random-uuid))]
          (content/save! ctx :feature id
                         {:page_slug slug :title t :subtitle "" :body ""
                          :sort_order (- 10 i)   ; deliberately reversed
                          :created_at (normalize/now-epoch)
                          :updated_at (normalize/now-epoch)})
          (content/publish! ctx :feature id)))
      (let [html (render ctx "worship")
            seen (map second (re-seq #"(ProbeAlpha|ProbeBeta|ProbeGamma)" html))]
        (is (= ["ProbeGamma" "ProbeBeta" "ProbeAlpha"] (distinct seen))
            "sort_order, not insertion order, decides the sequence on the page")))))

(deftest a-page-with-no-sections-is-unchanged
  (with-temp-ctx [ctx]
    (doseq [entry outline/tree
            :let [pk (outline/page-key entry)]
            :when (get page-handlers pk)]
      (is (some? (render ctx pk))
          (str (:label entry) " must still render with nothing added to it")))))
