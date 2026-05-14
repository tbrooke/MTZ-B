(ns com.mtzion.app.admin
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.lib.middleware :refer [wrap-signed-in]]
            [com.mtzion.lib.ui :as ui]
            [com.mtzion.ui.admin :as adm]))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(defn- now-epoch [] (.getEpochSecond (java.time.Instant/now)))

(defn- exec
  "Wrapper around biff.sqlite/execute that returns rows with unqualified
  snake_case keys, matching the DB column names used throughout this file."
  [ctx honey]
  (mapv (fn [row]
          (into {} (map (fn [[k v]]
                          [(keyword (str/replace (name k) "-" "_")) v])
                        row)))
        (biff.sqlite/execute ctx honey)))
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
;; Dashboard — bento layout
;; ---------------------------------------------------------------------------

(def ^:private accent
  {:terra {:ink "#C24A1F" :soft "#F4D9CC" :tint "#FBE9DF"}
   :sage  {:ink "#5A7257" :soft "#DCE2D6" :tint "#EAEEE4"}
   :gold  {:ink "#A87A2A" :soft "#F1E2BD" :tint "#F8EED1"}
   :slate {:ink "#3D4A60" :soft "#D5DCE6" :tint "#E6EBF2"}})

(defn- ac [k prop] (get-in accent [k prop]))

(defn- mk [kind size stroke]
  (let [sw (format "%.2f" (max 1.25 (/ (double size) 22.0)))]
    (case kind
      :features [:svg {:width size :height size :viewBox "0 0 40 40" :fill "none"}
                 [:rect {:x "4" :y "8" :width "32" :height "24" :rx "1" :stroke stroke :stroke-width sw}]
                 [:path {:d "M4 26 L14 18 L22 24 L30 16 L36 22" :stroke stroke :stroke-width sw :stroke-linecap "round" :stroke-linejoin "round"}]
                 [:circle {:cx "28" :cy "14" :r "2" :fill stroke}]]
      :blog     [:svg {:width size :height size :viewBox "0 0 40 40" :fill "none"}
                 [:path {:d "M8 10 H30" :stroke stroke :stroke-width sw :stroke-linecap "round"}]
                 [:path {:d "M8 17 H32" :stroke stroke :stroke-width sw :stroke-linecap "round"}]
                 [:path {:d "M8 24 H26" :stroke stroke :stroke-width sw :stroke-linecap "round"}]
                 [:path {:d "M8 31 H20" :stroke stroke :stroke-width sw :stroke-linecap "round"}]]
      :events   [:svg {:width size :height size :viewBox "0 0 40 40" :fill "none"}
                 [:rect {:x "6" :y "9" :width "28" :height "25" :rx "1.5" :stroke stroke :stroke-width sw}]
                 [:path {:d "M6 16 H34" :stroke stroke :stroke-width sw}]
                 [:path {:d "M13 6 V12" :stroke stroke :stroke-width sw :stroke-linecap "round"}]
                 [:path {:d "M27 6 V12" :stroke stroke :stroke-width sw :stroke-linecap "round"}]
                 [:circle {:cx "20" :cy "25" :r "2.5" :fill stroke}]]
      :pages    [:svg {:width size :height size :viewBox "0 0 40 40" :fill "none"}
                 [:rect {:x "10" :y "6" :width "22" :height "28" :rx "1" :stroke stroke :stroke-width sw}]
                 [:rect {:x "6" :y "10" :width "22" :height "28" :rx "1" :stroke stroke :stroke-width sw}]]
      :files    [:svg {:width size :height size :viewBox "0 0 40 40" :fill "none"}
                 [:path {:d "M8 12 H18 L21 16 H32 V31 H8 Z" :stroke stroke :stroke-width sw :stroke-linejoin "round"}]
                 [:path {:d "M14 22 H26" :stroke stroke :stroke-width sw :stroke-linecap "round"}]]
      :photos   [:svg {:width size :height size :viewBox "0 0 40 40" :fill "none"}
                 [:rect {:x "6" :y "10" :width "28" :height "22" :rx "1.5" :stroke stroke :stroke-width sw}]
                 [:path {:d "M6 26 L14 19 L20 24 L28 16 L34 21" :stroke stroke :stroke-width sw :stroke-linejoin "round" :stroke-linecap "round"}]
                 [:circle {:cx "14" :cy "16" :r "2" :fill stroke}]]
      :sermons  [:svg {:width size :height size :viewBox "0 0 40 40" :fill "none"}
                 [:circle {:cx "20" :cy "20" :r "14" :stroke stroke :stroke-width sw}]
                 [:path {:d "M17 14 L27 20 L17 26 Z" :fill stroke}]]
      nil)))

(defn- blog-graphic []
  [:div {:style "width:78%;aspect-ratio:0.78/1;background:#FBE9DF;border-radius:3px;padding:14px;display:flex;flex-direction:column;gap:6px;box-shadow:4px 4px 0 #F4D9CC;"}
   [:div {:style "font-family:var(--adm-serif);font-size:13px;font-weight:600;color:#C24A1F;line-height:1.15;margin-bottom:4px;"}
    "On stillness, and the" [:br] "shape of an evening."]
   (for [[w o] [["85%" ".35"] ["95%" ".35"] ["70%" ".35"] ["90%" ".2"] ["80%" ".2"]]]
     [:div {:style (str "height:2px;background:#C24A1F;opacity:" o ";border-radius:1px;width:" w ";")}])])

(defn- pages-graphic []
  [:div {:style "position:relative;width:70%;aspect-ratio:0.85/1;"}
   [:div {:style "position:absolute;inset:0;background:#FBF9F4;border:1px solid #D5DCE6;border-radius:3px;transform:translate(0,0) rotate(2deg);"}]
   [:div {:style "position:absolute;inset:0;background:#FBF9F4;border:1px solid #D5DCE6;border-radius:3px;transform:translate(8px,8px);"}]
   [:div {:style "position:absolute;inset:0;background:#E6EBF2;border:1px solid #D5DCE6;border-radius:3px;transform:translate(16px,16px) rotate(-2deg);padding:12px;display:flex;flex-direction:column;gap:5px;"}
    [:div {:style "height:8px;width:60%;background:#3D4A60;opacity:0.7;border-radius:1px;"}]
    [:div {:style "height:2px;background:#3D4A60;opacity:0.25;border-radius:1px;width:90%;margin-top:4px;"}]
    [:div {:style "height:2px;background:#3D4A60;opacity:0.25;border-radius:1px;width:80%;"}]
    [:div {:style "height:2px;background:#3D4A60;opacity:0.25;border-radius:1px;width:85%;"}]
    [:div {:style "height:2px;background:#3D4A60;opacity:0.25;border-radius:1px;width:70%;"}]]])

(defn- features-graphic []
  [:div {:style "width:85%;aspect-ratio:1.4/1;background:#F8EED1;border-radius:3px;padding:10px;display:flex;flex-direction:column;gap:6px;overflow:hidden;"}
   [:div {:style "display:flex;gap:3px;margin-bottom:4px;"}
    [:div {:style "width:5px;height:5px;border-radius:5px;background:#A87A2A;opacity:0.4;"}]
    [:div {:style "width:5px;height:5px;border-radius:5px;background:#A87A2A;opacity:0.4;"}]
    [:div {:style "width:5px;height:5px;border-radius:5px;background:#A87A2A;opacity:0.4;"}]]
   [:div {:style "flex:1;display:flex;gap:4px;"}
    [:div {:style "flex:2;background:#A87A2A;opacity:0.85;border-radius:2px;padding:6px;display:flex;flex-direction:column;justify-content:flex-end;"}
     [:div {:style "font-family:var(--adm-serif);font-size:9px;color:#F8EED1;font-weight:600;line-height:1.1;"} "Sunday" [:br] "Welcome"]]
    [:div {:style "flex:1;display:flex;flex-direction:column;gap:4px;"}
     [:div {:style "flex:1;background:#FBF9F4;border-radius:2px;"}]
     [:div {:style "flex:1;background:#FBF9F4;border-radius:2px;"}]]]])

(defn- big-tile [accent-k label count recent new-href list-href graphic]
  (let [ink (ac accent-k :ink)]
    [:div {:class "bento-tile bento-tile--sq"}
     [:a {:href list-href :class "bento-tile-link" :aria-label (str "View " label)}]
     [:div {:class "bento-tile-top"}
      [:span {:class "bento-caption" :style (str "color:" ink ";")} label]
      [:span {:class "bento-count"} (str count)]]
     [:div {:class "bento-tile-graphic"} graphic]
     [:div
      [:div {:class "bento-tile-title"} label]
      [:div {:class "bento-tile-recent"} (or recent "No items yet")]]
     [:div {:class "bento-btn-row"}
      [:a {:href new-href :class "bento-btn bento-btn--primary" :style (str "background:" ink ";")} "+ New"]
      [:a {:href list-href :class "bento-btn bento-btn--ghost"} "All"]]]))

(defn- events-tile [count upcoming]
  (let [ink  (ac :terra :ink)
        soft (ac :terra :soft)]
    [:div {:class "bento-tile bento-tile--events"}
     [:div {:class "bento-tile-top"}
      [:span {:class "bento-caption" :style (str "color:" ink ";")} "Events"]
      [:span {:style (str "font-family:var(--adm-mono);font-size:10px;color:" ink ";opacity:0.7;")}
       (str count " upcoming")]]
     [:div {:class "bento-tile-title" :style "margin-top:14px;"} "What's next"]
     [:div {:class "bento-event-list"}
      (if (empty? upcoming)
        [:div {:style "font-size:12px;color:#8A8478;margin-top:4px;"} "No upcoming events"]
        (for [ev upcoming]
          (let [ld (some-> (:start_at ev)
                           java.time.Instant/ofEpochSecond
                           (java.time.LocalDate/ofInstant java.time.ZoneOffset/UTC))]
            [:div {:class "bento-event-row"}
             [:div {:class "bento-date-chip" :style (str "border:1px solid " soft ";")}
              [:span {:class "bento-date-chip-m" :style (str "color:" ink ";")}
               (when ld (.format ld (java.time.format.DateTimeFormatter/ofPattern "MMM")))]
              [:span {:class "bento-date-chip-d" :style (str "color:" ink ";")}
               (when ld (str (.getDayOfMonth ld)))]]
             [:span {:class "bento-event-name"} (:title ev)]])))]
     [:div {:class "bento-btn-row" :style "margin-top:14px;"}
      [:a {:href "/admin/events/new" :class "bento-btn bento-btn--primary" :style (str "background:" ink ";flex:1;")} "+ New event"]
      [:a {:href "/admin/events" :class "bento-btn bento-btn--ghost" :style (str "border-color:" ink ";color:" ink ";")} "All"]]]))

(defn- calendar-tile [ctx]
  (let [today      (java.time.LocalDate/now java.time.ZoneOffset/UTC)
        first-day  (java.time.LocalDate/of (.getYear today) (.getMonthValue today) 1)
        dow        (.getValue (.getDayOfWeek first-day))
        offset     (mod dow 7)
        days-in    (.lengthOfMonth first-day)
        month-name (.format first-day (java.time.format.DateTimeFormatter/ofPattern "MMMM"))
        ms         (.toEpochSecond (.atStartOfDay first-day java.time.ZoneOffset/UTC))
        me         (.toEpochSecond (.atStartOfDay (.plusMonths first-day 1) java.time.ZoneOffset/UTC))
        evs        (exec ctx
                         {:select [:start_at] :from :event
                          :where  [:and [:>= :start_at ms] [:< :start_at me] [:= :published 1]]})
        event-days (into #{} (map #(-> (java.time.Instant/ofEpochSecond (:start_at %))
                                       (java.time.LocalDate/ofInstant java.time.ZoneOffset/UTC)
                                       .getDayOfMonth) evs))
        today-day  (.getDayOfMonth today)
        raw-cells  (concat (repeat offset nil) (range 1 (inc days-in)))
        rem        (mod (count raw-cells) 7)
        cells      (if (zero? rem) raw-cells (concat raw-cells (repeat (- 7 rem) nil)))]
    [:div {:class "bento-tile" :style "padding:18px;"}
     [:div {:class "bento-cal-header"}
      [:div
       [:span {:class "bento-caption" :style "color:#C24A1F;"} "Calendar"]
       [:div {:class "bento-cal-month"}
        month-name " " [:span {:class "bento-cal-year"} (str (.getYear today))]]]
      [:div {:class "bento-cal-chevrons"}
       [:button {:class "bento-chev" :disabled "true"} "&#8249;"]
       [:button {:class "bento-chev" :disabled "true"} "&#8250;"]]]
     [:div {:class "bento-cal-grid"}
      (for [d ["S" "M" "T" "W" "T" "F" "S"]]
        [:div {:class "bento-cal-dow"} d])
      (for [d cells]
        (if (nil? d)
          [:div]
          (let [is-today  (= d today-day)
                has-event (contains? event-days d)]
            [:div {:class (str "bento-cal-day"
                               (cond is-today  " bento-cal-day--today"
                                     has-event " bento-cal-day--event"
                                     :else ""))}
             (str d)
             (when (and has-event (not is-today))
               [:div {:class "bento-cal-dot"}])])))]
     [:div {:class "bento-cal-footer"}
      [:span
       [:span {:class "bento-cal-events-count"} (str (count evs))]
       " event" (when (not= 1 (count evs)) "s") " in " month-name]
      [:a {:href "/admin/events" :class "bento-cal-link"} "Open calendar →"]]]))

(defn- qa-tile [color href icon-kw word hint]
  [:a {:href href :class (str "bento-qa-tile bento-qa-tile--" (name color))}
   (mk icon-kw 22 (ac color :ink))
   [:div {:class "bento-qa-text"}
    [:span {:class (str "bento-qa-word bento-qa-word--" (name color))} word]
    [:span {:class "bento-qa-hint"} hint]]])

(defn- quick-actions-row []
  [:div {:class "bento-qa-row"}
   (qa-tile :slate "/admin/files/new"                    :files   "File"    "documents")
   (qa-tile :sage  "/admin/images/new?category=photo"    :photos  "Photo"   "images")
   (qa-tile :sage  "/admin/images/new?category=graphic"  :pages   "Graphic" "design assets")
   (qa-tile :terra "/admin/sermons/new"                  :sermons "Video"   "sermons")])

(defn- count-table [ctx table]
  (or (:n (first (exec ctx {:select [[:%count.id :n]] :from table}))) 0))

(defn- latest-title [ctx table order-col]
  (:title (first (exec ctx {:select [:title] :from table
                            :order-by [[order-col :desc]] :limit 1}))))

(defn- format-day-header []
  (let [d (java.time.LocalDate/now java.time.ZoneOffset/UTC)]
    (str (.format d (java.time.format.DateTimeFormatter/ofPattern "EEEE"))
         " · "
         (.format d (java.time.format.DateTimeFormatter/ofPattern "MMM d")))))

(defn dashboard [ctx]
  (let [n-ep     (now-epoch)
        n-posts  (count-table ctx :post)
        n-events (or (:n (first (exec ctx
                                      {:select [[:%count.id :n]] :from :event
                                       :where  [:and [:= :published 1] [:>= :start_at n-ep]]}))) 0)
        n-pages  (count-table ctx :page)
        n-feats  (count-table ctx :feature)
        upcoming (exec ctx
                       {:select   [:title :start_at] :from :event
                        :where    [:and [:= :published 1] [:>= :start_at n-ep]]
                        :order-by [[:start_at :asc]] :limit 3})
        r-post   (latest-title ctx :post :created_at)
        r-feat   (latest-title ctx :feature :updated_at)]
    (adm/bento-page "Dashboard"
                    [:div
                     (adm/bento-top-bar (ui/anti-forgery-field))
                     [:div {:class "bento-inner"}
                      [:div {:class "bento-header"}
                       [:h1 {:class "bento-h1"} "Mt Zion Dashboard"]
                       [:div {:class "bento-date"} (format-day-header)]]
                      [:div {:class "bento-grid-primary"}
                       (big-tile :terra "Blog Posts" n-posts r-post  "/admin/posts/new"    "/admin/posts"    (blog-graphic))
                       (big-tile :slate "Pages"      n-pages nil     "/admin/pages"        "/admin/pages"    (pages-graphic))
                       (big-tile :gold  "Features"   n-feats r-feat  "/admin/features/new" "/admin/features" (features-graphic))
                       (events-tile n-events upcoming)]
                      (quick-actions-row)]])))

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
  (let [rows (exec ctx {:select :* :from :feature
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
  (exec ctx
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
  (let [f (first (exec ctx {:select :* :from :feature
                            :where [:= :id (:id path-params)]}))]
    (if f
      (adm/admin-page "Edit Feature"
                      (adm/top-bar)
                      [:div {:class "adm-content"}
                       (adm/page-header "Edit Feature" "/admin/features")
                       (feature-form (str "/admin/features/" (:id f)) f (ui/anti-forgery-field))])
      {:status 404 :body "Not found"})))

(defn features-update [{:keys [params path-params] :as ctx}]
  (exec ctx
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
  (exec ctx {:delete-from :feature :where [:= :id (:id path-params)]})
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
  (let [rows (exec ctx {:select :* :from :post :order-by [[:created_at :desc]]})]
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
    (exec ctx
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
  (let [p (first (exec ctx {:select :* :from :post
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
    (exec ctx
          {:update :post
           :set {:slug slug
                 :title title
                 :excerpt (or (:excerpt params) "")
                 :body (or (:body params) "")
                 :published_at (parse-date-epoch (:published_at params))}
           :where [:= :id (:id path-params)]}))
  {:status 303 :headers {"location" "/admin/posts"}})

(defn posts-delete [{:keys [path-params] :as ctx}]
  (exec ctx {:delete-from :post :where [:= :id (:id path-params)]})
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

(defn- events-calendar-view [ctx query-params]
  (let [today     (java.time.LocalDate/now java.time.ZoneOffset/UTC)
        month-str (get query-params "month" "")
        first-day (if (seq month-str)
                    (try (java.time.LocalDate/parse (str month-str "-01"))
                         (catch Exception _ (java.time.LocalDate/of (.getYear today) (.getMonthValue today) 1)))
                    (java.time.LocalDate/of (.getYear today) (.getMonthValue today) 1))
        dow       (.getValue (.getDayOfWeek first-day))
        offset    (mod dow 7)
        days-in   (.lengthOfMonth first-day)
        month-fmt (java.time.format.DateTimeFormatter/ofPattern "MMMM yyyy")
        ym-fmt    (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM")
        ms        (.toEpochSecond (.atStartOfDay first-day java.time.ZoneOffset/UTC))
        me        (.toEpochSecond (.atStartOfDay (.plusMonths first-day 1) java.time.ZoneOffset/UTC))
        evs       (exec ctx {:select [:start_at :title :id] :from :event
                             :where  [:and [:>= :start_at ms] [:< :start_at me]]
                             :order-by [[:start_at :asc]]})
        ev-map    (group-by #(-> (java.time.Instant/ofEpochSecond (:start_at %))
                                 (java.time.LocalDate/ofInstant java.time.ZoneOffset/UTC)
                                 .getDayOfMonth) evs)
        today-day (when (and (= (.getYear today) (.getYear first-day))
                             (= (.getMonthValue today) (.getMonthValue first-day)))
                    (.getDayOfMonth today))
        raw-cells (concat (repeat offset nil) (range 1 (inc days-in)))
        rem       (mod (count raw-cells) 7)
        cells     (if (zero? rem) raw-cells (concat raw-cells (repeat (- 7 rem) nil)))
        prev-m    (.format (.minusMonths first-day 1) ym-fmt)
        next-m    (.format (.plusMonths first-day 1) ym-fmt)]
    (list
     [:div {:class "adm-cal-nav"}
      [:div {:class "adm-cal-nav-arrows"}
       [:a {:href (str "/admin/events?view=calendar&month=" prev-m) :class "adm-link"} "‹"]
       [:span {:class "adm-cal-nav-month"} (.format first-day month-fmt)]
       [:a {:href (str "/admin/events?view=calendar&month=" next-m) :class "adm-link"} "›"]]
      [:a {:href "/admin/events" :class "adm-link"} "← List"]]
     [:div {:class "adm-cal-wrap"}
      [:div {:class "adm-cal-grid"}
       (for [d ["Sun" "Mon" "Tue" "Wed" "Thu" "Fri" "Sat"]]
         [:div {:class "adm-cal-dow"} d])
       (for [d cells]
         (if (nil? d)
           [:div {:class "adm-cal-cell adm-cal-cell--empty"}]
           (let [day-evs (get ev-map d [])]
             [:div {:class (str "adm-cal-cell" (when (= d today-day) " adm-cal-cell--today"))}
              [:span {:class (str "adm-cal-day-num" (when (= d today-day) " adm-cal-day-num--today"))} (str d)]
              (for [ev day-evs]
                [:a {:href (str "/admin/events/" (:id ev) "/edit") :class "adm-cal-ev"} (:title ev)])])))]])))

(defn events-list [{:keys [query-params] :as ctx}]
  (let [view     (get query-params "view" "list")
        n-ep     (now-epoch)
        calendar? (= view "calendar")]
    (adm/admin-page "Events"
                    (adm/top-bar)
                    [:div {:class "adm-content"}
                     (adm/page-header "Events" "/admin")
                     [:div {:style "display:flex; gap:12px; margin-bottom:20px; align-items:center;"}
                      [:a {:href "/admin/events/new" :class "mtz-btn mtz-btn--primary"} "+ New Event"]
                      (if calendar?
                        [:a {:href "/admin/events" :class "adm-link"} "List view"]
                        [:a {:href "/admin/events?view=calendar" :class "adm-link"} "Calendar view"])]
                     (if calendar?
                       (events-calendar-view ctx query-params)
                       (let [rows (exec ctx {:select :* :from :event
                                             :where  [:>= :start_at n-ep]
                                             :order-by [[:start_at :asc]]})]
                         (if (empty? rows)
                           [:p {:class "adm-empty"} "No upcoming events. "
                            [:a {:href "/admin/events?view=all" :class "adm-link"} "View all past events"]]
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
                                                   (ui/anti-forgery-field))]]])]])))])))

(defn events-new [_ctx]
  (adm/admin-page "New Event"
                  (adm/top-bar)
                  [:div {:class "adm-content"}
                   (adm/page-header "New Event" "/admin/events")
                   (event-form "/admin/events" nil (ui/anti-forgery-field))]))

(defn events-create [{:keys [params] :as ctx}]
  (exec ctx
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
  (let [e (first (exec ctx {:select :* :from :event
                            :where [:= :id (:id path-params)]}))]
    (if e
      (adm/admin-page "Edit Event"
                      (adm/top-bar)
                      [:div {:class "adm-content"}
                       (adm/page-header "Edit Event" "/admin/events")
                       (event-form (str "/admin/events/" (:id e)) e (ui/anti-forgery-field))])
      {:status 404 :body "Not found"})))

(defn events-update [{:keys [params path-params] :as ctx}]
  (exec ctx
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
  (exec ctx {:delete-from :event :where [:= :id (:id path-params)]})
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
  (let [rows (exec ctx {:select :* :from :page :order-by [[:slug :asc]]})]
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
        p    (first (exec ctx {:select :* :from :page
                               :where [:= :slug slug]}))]
    (adm/admin-page (str "Edit — " (page-slug-label slug))
                    (adm/top-bar)
                    [:div {:class "adm-content"}
                     (adm/page-header (str "Edit: " (page-slug-label slug)) "/admin/pages")
                     (page-form slug p (ui/anti-forgery-field))])))

(defn pages-update [{:keys [params path-params] :as ctx}]
  (let [slug (str/trim (:slug path-params))]
    (exec ctx
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
;; Files (bulletins, slides, documents)
;; ---------------------------------------------------------------------------

(defn- upload-dir [ctx]
  (get ctx :mtz/upload-dir "storage/uploads"))

(defn- ensure-upload-dir! [ctx]
  (.mkdirs (java.io.File. (upload-dir ctx))))

(defn- sanitize-filename [s]
  (-> s str/trim (str/replace #"[^a-zA-Z0-9._\-]" "_")))

(defn- format-bytes [n]
  (cond
    (>= n 1048576) (format "%.1f MB" (/ (double n) 1048576))
    (>= n 1024)    (format "%.1f KB" (/ (double n) 1024))
    :else          (str n " B")))

(def ^:private file-category-options
  [["" "— Select type —"]
   ["bulletin"   "Sunday Bulletin"]
   ["slides"     "Presentation Slides"]
   ["newsletter" "Newsletter"]
   ["other"      "Other"]])

(defn- file-category-label [v]
  (or (some (fn [[k l]] (when (= k v) l)) file-category-options) v))

(defn files-list [ctx]
  (let [rows (exec ctx {:select :* :from :file
                        :order-by [[:uploaded_at :desc]]})]
    (adm/admin-page "Files"
                    (adm/top-bar)
                    [:div {:class "adm-content"}
                     (adm/page-header "Files" "/admin")
                     [:div {:style "margin-bottom:20px;"}
                      [:a {:href "/admin/files/new" :class "mtz-btn mtz-btn--primary"} "↑ Upload File"]]
                     (if (empty? rows)
                       [:p {:class "adm-hint"} "No files uploaded yet."]
                       [:table {:class "adm-table"}
                        [:thead
                         [:tr [:th "Label"] [:th "Category"] [:th "Size"] [:th "Uploaded"] [:th ""]]]
                        [:tbody
                         (for [r rows]
                           [:tr
                            [:td [:a {:href (:url r) :class "adm-link" :target "_blank"} (:label r)]]
                            [:td (file-category-label (:category r))]
                            [:td (when (:size_bytes r) (format-bytes (:size_bytes r)))]
                            [:td (epoch->date (:uploaded_at r))]
                            [:td
                             [:div {:class "adm-actions"}
                              [:a {:href (:url r) :class "adm-link" :target "_blank"} "View"]
                              (adm/delete-form (str "/admin/files/" (:id r) "/delete")
                                               (ui/anti-forgery-field))]]])]])])))

(defn files-new [{:keys [query-params]}]
  (let [pre-cat (get query-params "category" "")]
    (adm/admin-page "Upload File"
                    (adm/top-bar)
                    [:div {:class "adm-content"}
                     [:div {:class "adm-section-header"}
                      [:h1 {:class "adm-page-title" :style "margin:0;"} "Upload File"]
                      [:a {:href "/admin/files" :class "adm-link"} "View all files →"]]
                     [:form {:method "post" :action "/admin/files"
                             :class "adm-form" :enctype "multipart/form-data"}
                      (ui/anti-forgery-field)
                      (adm/field {:label "File" :hint "PDF, PPTX, or other document"}
                                 [:input {:type "file" :name "file" :class "adm-input"
                                          :required "true"
                                          :accept ".pdf,.pptx,.ppt,.doc,.docx,.xls,.xlsx"}])
                      (adm/field {:label "Label" :hint "e.g. Bulletin · May 4, 2026"}
                                 (adm/text-input {:name "label" :placeholder "Bulletin · May 4, 2026"}))
                      (adm/field {:label "Category"}
                                 (adm/select-input {:name "category"} file-category-options pre-cat))
                      (adm/submit-row {:label "Upload" :cancel-href "/admin/files"})]])))

(defn files-upload [{:keys [params] :as ctx}]
  (ensure-upload-dir! ctx)
  (let [dir      (upload-dir ctx)
        upload   (:file params)
        original (sanitize-filename (or (:filename upload) "file"))
        stored   (str (new-id) "-" original)
        dest     (java.io.File. (str dir "/" stored))
        label    (let [l (str/trim (or (:label params) ""))]
                   (if (seq l) l original))
        url      (str "/uploads/" stored)]
    (when (:tempfile upload)
      (io/copy (:tempfile upload) dest))
    (exec ctx
          {:insert-into :file
           :values [{:id (new-id)
                     :filename original
                     :label label
                     :category (or (:category params) "other")
                     :url url
                     :size_bytes (:size upload)
                     :uploaded_at (now-epoch)}]}))
  {:status 303 :headers {"location" "/admin/files"}})

(defn files-delete [{:keys [path-params] :as ctx}]
  (let [row (first (exec ctx {:select :* :from :file
                              :where [:= :id (:id path-params)]}))]
    (when row
      (let [filename (last (str/split (:url row) #"/"))
            f        (java.io.File. (str (upload-dir ctx) "/" filename))]
        (.delete f))
      (exec ctx {:delete-from :file :where [:= :id (:id path-params)]})))
  {:status 303 :headers {"location" "/admin/files"}})

(defn serve-upload [{:keys [path-params] :as ctx}]
  (let [filename (str/replace (:filename path-params) #"\.\.|/" "")
        f        (java.io.File. (str (upload-dir ctx) "/" filename))]
    (if (.exists f)
      {:status  200
       :headers {"Content-Type"        (or (java.nio.file.Files/probeContentType (.toPath f))
                                           "application/octet-stream")
                 "Content-Disposition" (str "inline; filename=\"" filename "\"")}
       :body    f}
      {:status 404 :body "Not found"})))

;; ---------------------------------------------------------------------------
;; Module
;; ---------------------------------------------------------------------------

(def module
  {:biff.ring/routes
   [["/uploads/:filename" {:get serve-upload :name ::serve-upload}]
    ["/admin" {:middleware [[wrap-signed-in]]}
     ["" {:get dashboard :name ::dashboard}]
     ["/features"
      ["" {:get features-list :post features-create :name ::features}]
      ["/new" {:get features-new :name ::features-new :conflicting true}]
      ["/:id" {:post features-update :name ::features-update :conflicting true}]
      ["/:id/edit" {:get features-edit :name ::features-edit}]
      ["/:id/delete" {:post features-delete :name ::features-delete}]]
     ["/posts"
      ["" {:get posts-list :post posts-create :name ::posts}]
      ["/new" {:get posts-new :name ::posts-new :conflicting true}]
      ["/:id" {:post posts-update :name ::posts-update :conflicting true}]
      ["/:id/edit" {:get posts-edit :name ::posts-edit}]
      ["/:id/delete" {:post posts-delete :name ::posts-delete}]]
     ["/events"
      ["" {:get events-list :post events-create :name ::events}]
      ["/new" {:get events-new :name ::events-new :conflicting true}]
      ["/:id" {:post events-update :name ::events-update :conflicting true}]
      ["/:id/edit" {:get events-edit :name ::events-edit}]
      ["/:id/delete" {:post events-delete :name ::events-delete}]]
     ["/pages"
      ["" {:get pages-list :name ::pages}]
      ["/:slug/edit" {:get pages-edit :name ::pages-edit}]
      ["/:slug" {:post pages-update :name ::pages-update}]]
     ["/files"
      ["" {:get files-list :post files-upload :name ::files}]
      ["/new" {:get files-new :name ::files-new}]
      ["/:id/delete" {:post files-delete :name ::files-delete}]]]]})
