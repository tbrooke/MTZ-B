(ns com.mtzion.app.admin
  (:require [clojure.string :as str]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.lib.middleware :refer [wrap-signed-in]]
            [com.mtzion.lib.ui :as ui]
            [com.mtzion.ui.admin :as adm]))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(defn- now-epoch [] (.getEpochSecond (java.time.Instant/now)))
(defn- new-id [] (str (random-uuid)))

(defn- parse-epoch
  "datetime-local input gives '2025-05-15T10:30' — append :00Z to parse as UTC."
  [s]
  (when (seq s)
    (try (.getEpochSecond (java.time.Instant/parse (str s ":00Z")))
         (catch Exception _ nil))))

(defn- parse-date-epoch
  "date input gives '2025-05-15' — parse as start-of-day UTC."
  [s]
  (when (seq s)
    (try (.getEpochSecond (java.time.Instant/parse (str s "T00:00:00Z")))
         (catch Exception _ nil))))

(defn- epoch->dt [epoch]
  (when epoch
    (-> (java.time.Instant/ofEpochSecond epoch) .toString (subs 0 16))))

(defn- epoch->date [epoch]
  (when epoch
    (-> (java.time.Instant/ofEpochSecond epoch) .toString (subs 0 10))))

(defn- slugify [s]
  (-> s str/lower-case (str/replace #"[^a-z0-9]+" "-") (str/replace #"^-|-$" "")))

(defn- checked? [params k]
  (contains? params k))

;; ---------------------------------------------------------------------------
;; Dashboard
;; ---------------------------------------------------------------------------

(defn- dashboard-card [title icon sub new-href list-href]
  [:div {:class "adm-card"}
   [:a {:href new-href :class "adm-card-action"}
    [:span {:class "adm-card-icon"} icon]
    [:span {:class "adm-card-label"} (str "+ New " title)]
    [:span {:class "adm-card-sub"} sub]]
   [:a {:href list-href :class "adm-card-action"}
    [:span {:class "adm-card-icon"} "☰"]
    [:span {:class "adm-card-label"} (str "All " title)]
    [:span {:class "adm-card-sub"} "View & edit"]]])

(defn dashboard [_ctx]
  (adm/admin-page "Dashboard"
                  (adm/top-bar)
                  [:div {:class "adm-content"}
                   [:h1 {:class "adm-page-title"} "Content"]
                   [:div {:class "adm-card-grid"}
                    (dashboard-card "Feature"    "🖼"  "Home page slots"     "/admin/features/new" "/admin/features")
                    (dashboard-card "Blog Post"  "✍"  "Pastor Jim Reflects" "/admin/posts/new"    "/admin/posts")
                    (dashboard-card "Event"      "📅" "Events & calendar"   "/admin/events/new"   "/admin/events")
                    (dashboard-card "Page"       "📄" "Site pages"          "/admin/pages"        "/admin/pages")]]))

;; ---------------------------------------------------------------------------
;; Features
;; ---------------------------------------------------------------------------

(def ^:private placement-options
  [["home_main"      "Home — Main Feature"]
   ["home_secondary" "Home — Secondary Feature"]
   ["home_news_1"    "Home — News #1"]
   ["home_news_2"    "Home — News #2"]
   ["home_news_3"    "Home — News #3"]])

(defn- placement-label [v]
  (or (some (fn [[k l]] (when (= k v) l)) placement-options) v))

(defn- feature-form [action f csrf]
  [:form {:method "post" :action action :class "adm-form"}
   csrf
   (adm/field {:label "Placement"}
              (adm/select-input {:name "placement" :required "true"} placement-options (:placement f)))
   (adm/field {:label "Title"}
              (adm/text-input {:name "title" :value (or (:title f) "") :required "true"}))
   (adm/field {:label "Subtitle" :hint "Optional short line below title"}
              (adm/text-input {:name "subtitle" :value (or (:subtitle f) "")}))
   (adm/field {:label "Body"}
              (adm/tiptap-field "body" (:body f)))
   (adm/field {:label "CTA Button Label" :hint "e.g. Learn More"}
              (adm/text-input {:name "cta_label" :value (or (:cta_label f) "")}))
   (adm/field {:label "CTA URL"}
              (adm/text-input {:name "cta_url" :value (or (:cta_url f) "")}))
   (adm/field {:label "Status"}
              [:label {:class "adm-check-row"}
               [:input {:type "checkbox" :name "published" :value "1"
                        :checked (not= "0" (str (:published f "1")))}]
               "Published"])
   (adm/submit-row {:cancel-href "/admin/features"})])

(defn features-list [ctx]
  (let [rows (biff.sqlite/execute ctx {:select :* :from :feature
                                       :order-by [[:sort_order :asc] [:created_at :desc]]})]
    (adm/admin-page "Features"
                    (adm/top-bar)
                    [:div {:class "adm-content"}
                     (adm/page-header "Features" "/admin")
                     [:div {:style "margin-bottom:20px;"}
                      [:a {:href "/admin/features/new" :class "mtz-btn mtz-btn--primary"} "+ New Feature"]]
                     (if (empty? rows)
                       [:p {:class "adm-empty"} "No features yet."]
                       [:table {:class "adm-table"}
                        [:thead
                         [:tr
                          [:th "Placement"] [:th "Title"] [:th "Status"] [:th ""]]]
                        [:tbody
                         (for [r rows]
                           [:tr
                            [:td (placement-label (:placement r))]
                            [:td (:title r)]
                            [:td (adm/badge (= 1 (:published r)))]
                            [:td
                             [:div {:class "adm-actions"}
                              [:a {:href (str "/admin/features/" (:id r) "/edit") :class "adm-link"} "Edit"]
                              (adm/delete-form (str "/admin/features/" (:id r) "/delete")
                                               (ui/anti-forgery-field))]]])]])])))

(defn features-new [_ctx]
  (adm/admin-page "New Feature"
                  (adm/top-bar)
                  [:div {:class "adm-content"}
                   (adm/page-header "New Feature" "/admin/features")
                   (feature-form "/admin/features" nil (ui/anti-forgery-field))]))

(defn features-create [{:keys [params] :as ctx}]
  (biff.sqlite/execute ctx
                       {:insert-into :feature
                        :values [{:id (new-id)
                                  :placement (or (:placement params) "home_main")
                                  :title (or (:title params) "")
                                  :subtitle (or (:subtitle params) "")
                                  :body (or (:body params) "")
                                  :cta_label (or (:cta_label params) "")
                                  :cta_url (or (:cta_url params) "")
                                  :published (if (:published params) 1 0)
                                  :sort_order 0
                                  :updated_at (now-epoch)
                                  :created_at (now-epoch)}]})
  {:status 303 :headers {"location" "/admin/features"}})

(defn features-edit [{:keys [path-params] :as ctx}]
  (let [f (first (biff.sqlite/execute ctx {:select :* :from :feature
                                           :where [:= :id (:id path-params)]}))]
    (if f
      (adm/admin-page "Edit Feature"
                      (adm/top-bar)
                      [:div {:class "adm-content"}
                       (adm/page-header "Edit Feature" "/admin/features")
                       (feature-form (str "/admin/features/" (:id f)) f (ui/anti-forgery-field))])
      {:status 404 :body "Not found"})))

(defn features-update [{:keys [params path-params] :as ctx}]
  (biff.sqlite/execute ctx
                       {:update :feature
                        :set {:placement (or (:placement params) "home_main")
                              :title (or (:title params) "")
                              :subtitle (or (:subtitle params) "")
                              :body (or (:body params) "")
                              :cta_label (or (:cta_label params) "")
                              :cta_url (or (:cta_url params) "")
                              :published (if (:published params) 1 0)
                              :updated_at (now-epoch)}
                        :where [:= :id (:id path-params)]})
  {:status 303 :headers {"location" "/admin/features"}})

(defn features-delete [{:keys [path-params] :as ctx}]
  (biff.sqlite/execute ctx {:delete-from :feature :where [:= :id (:id path-params)]})
  {:status 303 :headers {"location" "/admin/features"}})

;; ---------------------------------------------------------------------------
;; Posts (Pastor Jim Reflects)
;; ---------------------------------------------------------------------------

(defn- post-form [action p csrf]
  [:form {:method "post" :action action :class "adm-form"}
   csrf
   (adm/field {:label "Title"}
              (adm/text-input {:name "title" :value (or (:title p) "") :required "true"}))
   (adm/field {:label "Slug" :hint "URL path — auto-generated from title if left blank"}
              (adm/text-input {:name "slug" :value (or (:slug p) "")}))
   (adm/field {:label "Excerpt" :hint "Short summary for listing pages"}
              [:textarea {:name "excerpt" :class "adm-textarea"} (or (:excerpt p) "")])
   (adm/field {:label "Body"}
              (adm/tiptap-field "body" (:body p)))
   (adm/field {:label "Published Date" :hint "Leave blank to save as draft"}
              [:input {:type "date" :name "published_at" :class "adm-input"
                       :value (or (epoch->date (:published_at p)) "")}])
   (adm/submit-row {:cancel-href "/admin/posts"})])

(defn posts-list [ctx]
  (let [rows (biff.sqlite/execute ctx {:select :* :from :post :order-by [[:created_at :desc]]})]
    (adm/admin-page "Blog Posts"
                    (adm/top-bar)
                    [:div {:class "adm-content"}
                     (adm/page-header "Pastor Jim Reflects" "/admin")
                     [:div {:style "margin-bottom:20px;"}
                      [:a {:href "/admin/posts/new" :class "mtz-btn mtz-btn--primary"} "+ New Post"]]
                     (if (empty? rows)
                       [:p {:class "adm-empty"} "No posts yet."]
                       [:table {:class "adm-table"}
                        [:thead [:tr [:th "Title"] [:th "Published"] [:th ""]]]
                        [:tbody
                         (for [r rows]
                           [:tr
                            [:td (:title r)]
                            [:td (if (:published_at r)
                                   (epoch->date (:published_at r))
                                   (adm/badge false))]
                            [:td
                             [:div {:class "adm-actions"}
                              [:a {:href (str "/admin/posts/" (:id r) "/edit") :class "adm-link"} "Edit"]
                              (adm/delete-form (str "/admin/posts/" (:id r) "/delete")
                                               (ui/anti-forgery-field))]]])]])])))

(defn posts-new [_ctx]
  (adm/admin-page "New Post"
                  (adm/top-bar)
                  [:div {:class "adm-content"}
                   (adm/page-header "New Post" "/admin/posts")
                   (post-form "/admin/posts" nil (ui/anti-forgery-field))]))

(defn posts-create [{:keys [params] :as ctx}]
  (let [title (or (:title params) "")
        slug  (let [s (str/trim (or (:slug params) ""))]
                (if (seq s) s (slugify title)))]
    (biff.sqlite/execute ctx
                         {:insert-into :post
                          :values [{:id (new-id)
                                    :slug slug
                                    :title title
                                    :excerpt (or (:excerpt params) "")
                                    :body (or (:body params) "")
                                    :published_at (parse-date-epoch (:published_at params))
                                    :created_at (now-epoch)}]}))
  {:status 303 :headers {"location" "/admin/posts"}})

(defn posts-edit [{:keys [path-params] :as ctx}]
  (let [p (first (biff.sqlite/execute ctx {:select :* :from :post
                                           :where [:= :id (:id path-params)]}))]
    (if p
      (adm/admin-page "Edit Post"
                      (adm/top-bar)
                      [:div {:class "adm-content"}
                       (adm/page-header "Edit Post" "/admin/posts")
                       (post-form (str "/admin/posts/" (:id p)) p (ui/anti-forgery-field))])
      {:status 404 :body "Not found"})))

(defn posts-update [{:keys [params path-params] :as ctx}]
  (let [title (or (:title params) "")
        slug  (let [s (str/trim (or (:slug params) ""))]
                (if (seq s) s (slugify title)))]
    (biff.sqlite/execute ctx
                         {:update :post
                          :set {:slug slug
                                :title title
                                :excerpt (or (:excerpt params) "")
                                :body (or (:body params) "")
                                :published_at (parse-date-epoch (:published_at params))}
                          :where [:= :id (:id path-params)]}))
  {:status 303 :headers {"location" "/admin/posts"}})

(defn posts-delete [{:keys [path-params] :as ctx}]
  (biff.sqlite/execute ctx {:delete-from :post :where [:= :id (:id path-params)]})
  {:status 303 :headers {"location" "/admin/posts"}})

;; ---------------------------------------------------------------------------
;; Events
;; ---------------------------------------------------------------------------

(def ^:private recurrence-options
  [["none"      "Does not repeat"]
   ["daily"     "Daily"]
   ["weekly"    "Weekly"]
   ["biweekly"  "Every two weeks"]
   ["monthly"   "Monthly"]
   ["yearly"    "Yearly"]])

(defn- event-form [action e csrf]
  [:form {:method "post" :action action :class "adm-form"}
   csrf
   (adm/field {:label "Title"}
              (adm/text-input {:name "title" :value (or (:title e) "") :required "true"}))
   (adm/field {:label "Description"}
              (adm/tiptap-field "description" (:description e)))
   (adm/field {:label "Location"}
              (adm/text-input {:name "location" :value (or (:location e) "")}))
   (adm/field {:label "Start"}
              [:input {:type "datetime-local" :name "start_at" :class "adm-input" :required "true"
                       :value (or (epoch->dt (:start_at e)) "")}])
   (adm/field {:label "End" :hint "Optional"}
              [:input {:type "datetime-local" :name "end_at" :class "adm-input"
                       :value (or (epoch->dt (:end_at e)) "")}])
   (adm/field {:label "Options"}
              [:label {:class "adm-check-row"}
               [:input {:type "checkbox" :name "all_day" :value "1"
                        :checked (= 1 (:all_day e))}]
               "All day"])
   (adm/field {:label "Repeats"}
              (adm/select-input {:name "recurrence"} recurrence-options (:recurrence e "none")))
   (adm/field {:label "Repeat Until" :hint "For recurring events — leave blank for no end date"}
              [:input {:type "date" :name "recur_until" :class "adm-input"
                       :value (or (epoch->date (:recur_until e)) "")}])
   (adm/field {:label "Status"}
              [:label {:class "adm-check-row"}
               [:input {:type "checkbox" :name "published" :value "1"
                        :checked (not= 0 (:published e 1))}]
               "Published"])
   (adm/submit-row {:cancel-href "/admin/events"})])

(defn events-list [ctx]
  (let [rows (biff.sqlite/execute ctx {:select :* :from :event :order-by [[:start_at :asc]]})]
    (adm/admin-page "Events"
                    (adm/top-bar)
                    [:div {:class "adm-content"}
                     (adm/page-header "Events" "/admin")
                     [:div {:style "margin-bottom:20px;"}
                      [:a {:href "/admin/events/new" :class "mtz-btn mtz-btn--primary"} "+ New Event"]]
                     (if (empty? rows)
                       [:p {:class "adm-empty"} "No events yet."]
                       [:table {:class "adm-table"}
                        [:thead [:tr [:th "Title"] [:th "Start"] [:th "Location"] [:th "Repeats"] [:th "Status"] [:th ""]]]
                        [:tbody
                         (for [r rows]
                           [:tr
                            [:td (:title r)]
                            [:td (or (epoch->dt (:start_at r)) "—")]
                            [:td (or (:location r) "—")]
                            [:td (:recurrence r "none")]
                            [:td (adm/badge (= 1 (:published r)))]
                            [:td
                             [:div {:class "adm-actions"}
                              [:a {:href (str "/admin/events/" (:id r) "/edit") :class "adm-link"} "Edit"]
                              (adm/delete-form (str "/admin/events/" (:id r) "/delete")
                                               (ui/anti-forgery-field))]]])]])])))

(defn events-new [_ctx]
  (adm/admin-page "New Event"
                  (adm/top-bar)
                  [:div {:class "adm-content"}
                   (adm/page-header "New Event" "/admin/events")
                   (event-form "/admin/events" nil (ui/anti-forgery-field))]))

(defn events-create [{:keys [params] :as ctx}]
  (biff.sqlite/execute ctx
                       {:insert-into :event
                        :values [{:id (new-id)
                                  :title (or (:title params) "")
                                  :description (or (:description params) "")
                                  :location (or (:location params) "")
                                  :start_at (or (parse-epoch (:start_at params)) (now-epoch))
                                  :end_at (parse-epoch (:end_at params))
                                  :all_day (if (:all_day params) 1 0)
                                  :recurrence (or (:recurrence params) "none")
                                  :recur_until (parse-date-epoch (:recur_until params))
                                  :published (if (:published params) 1 0)
                                  :created_at (now-epoch)}]})
  {:status 303 :headers {"location" "/admin/events"}})

(defn events-edit [{:keys [path-params] :as ctx}]
  (let [e (first (biff.sqlite/execute ctx {:select :* :from :event
                                           :where [:= :id (:id path-params)]}))]
    (if e
      (adm/admin-page "Edit Event"
                      (adm/top-bar)
                      [:div {:class "adm-content"}
                       (adm/page-header "Edit Event" "/admin/events")
                       (event-form (str "/admin/events/" (:id e)) e (ui/anti-forgery-field))])
      {:status 404 :body "Not found"})))

(defn events-update [{:keys [params path-params] :as ctx}]
  (biff.sqlite/execute ctx
                       {:update :event
                        :set {:title (or (:title params) "")
                              :description (or (:description params) "")
                              :location (or (:location params) "")
                              :start_at (or (parse-epoch (:start_at params)) (now-epoch))
                              :end_at (parse-epoch (:end_at params))
                              :all_day (if (:all_day params) 1 0)
                              :recurrence (or (:recurrence params) "none")
                              :recur_until (parse-date-epoch (:recur_until params))
                              :published (if (:published params) 1 0)}
                        :where [:= :id (:id path-params)]})
  {:status 303 :headers {"location" "/admin/events"}})

(defn events-delete [{:keys [path-params] :as ctx}]
  (biff.sqlite/execute ctx {:delete-from :event :where [:= :id (:id path-params)]})
  {:status 303 :headers {"location" "/admin/events"}})

;; ---------------------------------------------------------------------------
;; Pages
;; ---------------------------------------------------------------------------

(def ^:private page-slugs
  [["about"      "About"]
   ["worship"    "Worship"]
   ["events"     "Events"]
   ["activities" "Activities"]
   ["news"       "News"]
   ["outreach"   "Outreach"]
   ["contact"    "Contact"]
   ["preschool"  "Preschool"]
   ["privacy"    "Privacy"]])

(defn- page-slug-label [slug]
  (or (some (fn [[s l]] (when (= s slug) l)) page-slugs) slug))

(defn- page-form [slug p csrf]
  [:form {:method "post" :action (str "/admin/pages/" slug) :class "adm-form"}
   csrf
   (adm/field {:label "Page Title Override" :hint "Optional — leave blank to use the default page title"}
              (adm/text-input {:name "title" :value (or (:title p) "")}))
   (adm/field {:label "Page Body"}
              (adm/tiptap-field "body" (:body p)))
   (adm/submit-row {:cancel-href "/admin/pages"})])

(defn pages-list [ctx]
  (let [rows (biff.sqlite/execute ctx {:select :* :from :page :order-by [[:slug :asc]]})]
    (adm/admin-page "Pages"
                    (adm/top-bar)
                    [:div {:class "adm-content"}
                     (adm/page-header "Pages" "/admin")
                     [:p {:class "adm-hint" :style "margin-bottom:20px;"}
                      "Select a page to edit its content. If no DB record exists yet the page shows its default static content."]
                     [:table {:class "adm-table"}
                      [:thead [:tr [:th "Page"] [:th "Last Updated"] [:th ""]]]
                      [:tbody
                       (for [[slug label] page-slugs]
                         (let [r (first (filter #(= (:slug %) slug) rows))]
                           [:tr
                            [:td label]
                            [:td (if r (epoch->date (:updated_at r)) [:em {:class "adm-hint"} "default"])]
                            [:td [:a {:href (str "/admin/pages/" slug "/edit") :class "adm-link"} "Edit"]]]))]]])))

(defn pages-edit [{:keys [path-params] :as ctx}]
  (let [slug (:slug path-params)
        p    (first (biff.sqlite/execute ctx {:select :* :from :page
                                              :where [:= :slug slug]}))]
    (adm/admin-page (str "Edit — " (page-slug-label slug))
                    (adm/top-bar)
                    [:div {:class "adm-content"}
                     (adm/page-header (str "Edit: " (page-slug-label slug)) "/admin/pages")
                     (page-form slug p (ui/anti-forgery-field))])))

(defn pages-update [{:keys [params path-params] :as ctx}]
  (let [slug (str/trim (:slug path-params))]
    (biff.sqlite/execute ctx
                         {:insert-into :page
                          :values [{:id (new-id)
                                    :slug slug
                                    :title (or (:title params) "")
                                    :body (or (:body params) "")
                                    :updated_at (now-epoch)}]
                          :on-conflict {:on [:slug]
                                        :do-update-set [:title :body :updated_at]}}))
  {:status 303 :headers {"location" "/admin/pages"}})

;; ---------------------------------------------------------------------------
;; Module
;; ---------------------------------------------------------------------------

(def module
  {:biff.ring/routes
   [["/admin" {:middleware [[wrap-signed-in]]}
     ["" {:get dashboard :name ::dashboard}]
     ["/features"
      ["" {:get features-list :post features-create :name ::features}]
      ["/new" {:get features-new :name ::features-new}]
      ["/:id" {:post features-update :name ::features-update}]
      ["/:id/edit" {:get features-edit :name ::features-edit}]
      ["/:id/delete" {:post features-delete :name ::features-delete}]]
     ["/posts"
      ["" {:get posts-list :post posts-create :name ::posts}]
      ["/new" {:get posts-new :name ::posts-new}]
      ["/:id" {:post posts-update :name ::posts-update}]
      ["/:id/edit" {:get posts-edit :name ::posts-edit}]
      ["/:id/delete" {:post posts-delete :name ::posts-delete}]]
     ["/events"
      ["" {:get events-list :post events-create :name ::events}]
      ["/new" {:get events-new :name ::events-new}]
      ["/:id" {:post events-update :name ::events-update}]
      ["/:id/edit" {:get events-edit :name ::events-edit}]
      ["/:id/delete" {:post events-delete :name ::events-delete}]]
     ["/pages"
      ["" {:get pages-list :name ::pages}]
      ["/:slug/edit" {:get pages-edit :name ::pages-edit}]
      ["/:slug" {:post pages-update :name ::pages-update}]]]]})
