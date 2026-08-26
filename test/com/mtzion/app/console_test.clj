(ns com.mtzion.app.console-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [com.mtzion.app.console :as console]
            [com.mtzion.model.content :as content]
            [com.mtzion.test-util :refer [with-temp-ctx]]))

(def ^:private post-cols     #'console/post-cols)
(def ^:private unique-slug   #'console/unique-slug)
(def ^:private visible-posts #'console/visible-posts)

(defn- create!
  "Runs the create handler and returns the new post's id, the way the browser
  learns it — out of the redirect."
  [ctx params]
  (let [resp (console/writing-create (assoc ctx :params params))]
    (is (= 303 (:status resp)))
    (last (str/split (get-in resp [:headers "location"]) #"/"))))

(defn- status-of [ctx id] (:status (content/get-one ctx :post id)))

;; ---------------------------------------------------------------------------
;; Slugs
;; ---------------------------------------------------------------------------

(deftest slugs-stay-unique
  (with-temp-ctx [ctx]
    (let [a (create! ctx {:title "Welcome Back"})
          b (create! ctx {:title "Welcome Back"})
          c (create! ctx {:title "Welcome Back"})]
      (is (= "welcome-back"   (:slug (content/get-one ctx :post a))))
      (is (= "welcome-back-2" (:slug (content/get-one ctx :post b)))
          "a duplicate title is suffixed rather than failing the UNIQUE constraint")
      (is (= "welcome-back-3" (:slug (content/get-one ctx :post c)))))))

(deftest editing-a-post-keeps-its-own-slug
  (with-temp-ctx [ctx]
    (let [id (create! ctx {:title "Welcome Back"})]
      (console/writing-save (assoc ctx :params {:title "Welcome Back" :slug "welcome-back"}
                                   :path-params {:id id}))
      (is (= "welcome-back" (:slug (content/get-one ctx :post id)))
          "a post does not collide with itself and get suffixed on every save"))))

(deftest a-blank-title-still-produces-a-usable-slug
  (with-temp-ctx [ctx]
    (let [id (create! ctx {:title ""})]
      (is (seq (:slug (content/get-one ctx :post id)))))))

(deftest post-cols-omits-publish-status
  (with-temp-ctx [ctx]
    (let [cols (post-cols (assoc ctx :params {:title "T" :published_at "2026-08-09"}) nil)]
      (is (not (contains? cols :status)))
      (is (not (contains? cols :published)))
      (is (contains? cols :published_at)
          "the display date is editorial and does travel with the form"))))

;; ---------------------------------------------------------------------------
;; Publish state
;; ---------------------------------------------------------------------------

(deftest new-posts-are-drafts
  (with-temp-ctx [ctx]
    (let [id (create! ctx {:title "Half an idea"})]
      (is (= content/draft (status-of ctx id)))
      (is (empty? (content/live ctx :post))))))

(deftest the-pill-toggles-and-reports-both-halves
  (with-temp-ctx [ctx]
    (let [id   (create! ctx {:title "Rally Day"})
          resp (console/writing-status (assoc ctx :path-params {:id id}))
          body (:body resp)]
      (is (= content/published (status-of ctx id)))
      (is (not (str/includes? body "<!DOCTYPE"))
          "a swapped fragment is not a document")
      (is (str/includes? body "con-pill--published"))
      (testing "the listing row's dot is swapped out of band so both agree"
        (is (str/includes? body "hx-swap-oob"))
        (is (str/includes? body (str "con-row-dot-" id))))

      (console/writing-status (assoc ctx :path-params {:id id}))
      (is (= content/draft (status-of ctx id))))))

(deftest saving-an-edit-never-changes-publish-state
  (with-temp-ctx [ctx]
    (let [id (create! ctx {:title "Council notes"})]
      (console/writing-status (assoc ctx :path-params {:id id}))
      (is (= content/published (status-of ctx id)))

      (console/writing-save (assoc ctx :params {:title "Council notes, August"}
                                   :path-params {:id id}))
      (is (= content/published (status-of ctx id)) "a full save leaves it live")
      (is (= "Council notes, August" (:title (content/get-one ctx :post id))))

      (console/writing-autosave (assoc ctx :params {:title "Council notes, revised"}
                                       :path-params {:id id}))
      (is (= content/published (status-of ctx id))
          "and neither does autosave — typing must never change what the site shows")
      (is (= "Council notes, revised" (:title (content/get-one ctx :post id)))))))

(deftest the-display-date-is-editable-while-published
  (with-temp-ctx [ctx]
    (let [id (create! ctx {:title "Dated" :published_at "2026-08-09"})]
      (console/writing-status (assoc ctx :path-params {:id id}))
      (console/writing-save (assoc ctx :params {:title "Dated" :slug "dated"
                                                :published_at "2026-09-01"}
                                   :path-params {:id id}))
      (is (= content/published (status-of ctx id)))
      (is (= (com.mtzion.model.normalize/local-date->epoch "2026-09-01")
             (:published_at (content/get-one ctx :post id)))
          "the date printed on the article is content, not a publish flag"))))

;; ---------------------------------------------------------------------------
;; Archive
;; ---------------------------------------------------------------------------

(deftest archiving-removes-it-from-the-pane-but-not-the-database
  (with-temp-ctx [ctx]
    (let [id (create! ctx {:title "Old news"})]
      (console/writing-status (assoc ctx :path-params {:id id}))
      (console/writing-archive (assoc ctx :path-params {:id id}))

      (is (= content/archived (status-of ctx id)))
      (is (empty? (content/live ctx :post)) "off the site")
      (is (empty? (visible-posts ctx nil))  "out of the Writing pane")
      (is (some? (content/get-one ctx :post id)) "still on record"))))

(deftest restore-lands-in-draft
  (with-temp-ctx [ctx]
    (let [id (create! ctx {:title "Second thoughts"})]
      (console/writing-status (assoc ctx :path-params {:id id}))
      (console/writing-archive (assoc ctx :path-params {:id id}))
      (console/archive-restore (assoc ctx :path-params {:type "post" :id id}))
      (is (= content/draft (status-of ctx id))
          "restoring must not put something back on the site by surprise"))))

(deftest purge-needs-the-archive-first
  (with-temp-ctx [ctx]
    (let [id (create! ctx {:title "Mistake"})]
      (console/archive-purge (assoc ctx :path-params {:type "post" :id id}))
      (is (some? (content/get-one ctx :post id))
          "a live or draft row cannot be deleted, even by URL")

      (console/writing-archive (assoc ctx :path-params {:id id}))
      (console/archive-purge (assoc ctx :path-params {:type "post" :id id}))
      (is (nil? (content/get-one ctx :post id))))))

(deftest an-unknown-type-in-the-archive-url-does-nothing
  (with-temp-ctx [ctx]
    (let [id (create! ctx {:title "Safe"})]
      (is (= 303 (:status (console/archive-purge
                           (assoc ctx :path-params {:type "user" :id id})))))
      (is (= 303 (:status (console/archive-restore
                           (assoc ctx :path-params {:type "widget" :id id})))))
      (is (some? (content/get-one ctx :post id))))))

;; ---------------------------------------------------------------------------
;; Listing
;; ---------------------------------------------------------------------------

(deftest the-pane-filters-by-category-and-search
  (with-temp-ctx [ctx]
    (create! ctx {:title "On stillness"    :category "reflection"})
    (create! ctx {:title "Fall festival"   :category "news"})
    (create! ctx {:title "Council notes"   :category "news" :excerpt "consistory minutes"})

    (is (= 3 (count (visible-posts ctx nil))))
    (is (= ["On stillness"] (mapv :title (visible-posts ctx {:cat "reflection"}))))
    (is (= 2 (count (visible-posts ctx {:cat "news"}))))
    (is (= ["Fall festival"] (mapv :title (visible-posts ctx {:q "festival"})))
        "search is case-insensitive on the title")
    (is (= ["Council notes"] (mapv :title (visible-posts ctx {:q "CONSISTORY"})))
        "and reaches the summary too")
    (is (empty? (visible-posts ctx {:cat "news" :q "stillness"}))
        "category and search compose")))

(deftest unique-slug-ignores-the-post-being-edited
  (with-temp-ctx [ctx]
    (let [id (create! ctx {:title "Only one"})]
      (is (= "only-one" (unique-slug ctx "only-one" id)))
      (is (= "only-one-2" (unique-slug ctx "only-one" nil))))))
