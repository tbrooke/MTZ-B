(ns com.mtzion.app.console
  "The console — /admin reorganised around the work rather than the tables.

  Phase 1 builds the Writing pane and the Archive. The Site and Calendar panes
  are honest placeholders that point at the /admin screens still doing the job.
  /admin keeps working untouched throughout; both read and write the same rows."
  (:require [clojure.string :as str]
            [com.mtzion.lib.middleware :refer [wrap-signed-in]]
            [com.mtzion.lib.ui :as ui]
            [com.mtzion.model.content :as content]
            [com.mtzion.model.event :as event]
            [com.mtzion.model.normalize :as normalize]
            [com.mtzion.ui.console :as con]
            [lambdaisland.hiccup :as hiccup]))

(def ^:private now-epoch    normalize/now-epoch)
(def ^:private ->date-epoch normalize/local-date->epoch)
(def ^:private epoch->date  normalize/epoch->date-str)

(defn- event-day
  "An event's start_at is church wall-clock, so it renders in Eastern.
  epoch->date-str is the UTC-midnight convention and would be a day out."
  [epoch]
  (when epoch
    (-> (java.time.Instant/ofEpochSecond epoch)
        (java.time.LocalDate/ofInstant normalize/eastern)
        (.format (java.time.format.DateTimeFormatter/ofPattern "MMM d")))))

(defn- new-id [] (str (random-uuid)))

(defn- fragment
  "An HTML response that is a piece of a page rather than a page — what every
  HTMX endpoint here returns. `:doctype? false` matters: hiccup/render prepends
  <!DOCTYPE html> by default, which has no business in a swapped fragment."
  [hiccup-form]
  {:status  200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    (hiccup/render hiccup-form {:doctype? false})})

;; ---------------------------------------------------------------------------
;; Writing — blog, news, reflections
;; ---------------------------------------------------------------------------

(def ^:private categories
  [["reflection" "Reflection"]
   ["news"       "News"]
   ["blog"       "Blog"]])

(defn- category-label [c]
  (or (some (fn [[v l]] (when (= v c) l)) categories) c))

(defn- unique-slug
  "Slugs are UNIQUE on `post`, so a second 'Welcome' would fail the insert with
  a constraint error the editor has no way to explain. Suffix instead."
  [ctx desired self-id]
  (let [taken (into #{} (comp (remove #(= self-id (:id %))) (map :slug))
                    (content/ls ctx :post))
        base  (if (seq desired) desired "post")]
    (if-not (taken base)
      base
      (first (for [n (iterate inc 2)
                   :let [candidate (str base "-" n)]
                   :when (not (taken candidate))]
               candidate)))))

(defn- post-cols
  "Form params -> column map. Status columns are absent by design: publish state
  moves through the pill, never through saving an edit. `published_at` is the
  exception the model declares — it is the date printed on the article."
  [{:keys [params] :as ctx} id]
  (let [title (str/trim (or (:title params) ""))
        slug  (unique-slug ctx
                           (normalize/slugify (let [s (str/trim (or (:slug params) ""))]
                                                (if (seq s) s title)))
                           id)]
    {:title        title
     :slug         slug
     :category     (or (not-empty (:category params)) "blog")
     :author       (not-empty (str/trim (or (:author params) "")))
     :excerpt      (or (:excerpt params) "")
     :image_id     (not-empty (:image_id params))
     :body         (or (:body params) "")
     :show_on_home (if (:show_on_home params) 1 0)
     :published_at (->date-epoch (:published_at params))}))

(defn- visible-posts
  "Everything that has not been archived, newest first, narrowed by the filter
  chips and the search box. The table is small enough that filtering in Clojure
  beats three variants of the query."
  [ctx {:keys [cat q]}]
  (cond->> (content/ls ctx :post {:status #{content/draft content/published}})
    (seq cat) (filter #(= cat (:category %)))
    (seq q)   (filter #(str/includes? (str/lower-case (str (:title %) " " (:excerpt %)))
                                      (str/lower-case q)))))

(defn- writing-list [ctx posts selected-id {:keys [cat q]}]
  (let [qs (fn [c] (str "/console/writing"
                        (when (or (seq c) (seq q))
                          (str "?" (str/join "&" (cond-> []
                                                   (seq c) (conj (str "cat=" c))
                                                   (seq q) (conj (str "q=" q))))))))]
    [:aside {:class "con-list"}
     [:div {:class "con-list-head"}
      [:h1 {:class "con-list-title"} "Writing"]
      [:a {:href "/console/writing/new" :class "con-btn con-btn--primary"} "+ New"]]

     [:div {:class "con-chips"}
      [:a {:href (qs nil) :class (str "con-chip" (when-not (seq cat) " is-active"))} "All"]
      (for [[v label] categories]
        [:a {:href (qs v) :class (str "con-chip" (when (= v cat) " is-active"))} label])]

     [:form {:method "get" :action "/console/writing" :class "con-search"}
      (when (seq cat) [:input {:type "hidden" :name "cat" :value cat}])
      [:input {:type "search" :name "q" :value (or q "")
               :class "con-input con-input--search" :placeholder "Search titles…"}]]

     [:div {:class "con-rows"}
      (if (empty? posts)
        [:p {:class "con-rows-empty"}
         (if (or (seq cat) (seq q)) "Nothing matches that." "Nothing written yet.")]
        (for [p posts]
          [:a {:href  (str "/console/writing/" (:id p))
               :class (str "con-row" (when (= (:id p) selected-id) " is-selected"))}
           (con/status-dot (:status p) (str "con-row-dot-" (:id p)))
           [:span {:class "con-row-main"}
            [:span {:class "con-row-title"} (or (not-empty (:title p)) "Untitled")]
            [:span {:class "con-row-meta"}
             (category-label (:category p))
             (when-let [d (epoch->date (:published_at p))] (str " · " d))]]]))]]))

(defn- details-strip [p]
  [:details {:class "con-details"}
   [:summary {:class "con-details-summary"} "Details"]
   [:div {:class "con-details-grid"}
    (con/field {:label "Category"}
               (con/select-input {:name "category"} categories (or (:category p) "blog")))
    (con/field {:label "Byline" :hint "Who wrote it — free text, not a login"}
               (con/text-input {:name "author" :value (or (:author p) "")
                                :placeholder "Pastor Jim"}))
    (con/field {:label "Display date" :hint "The date printed on the article"}
               [:input {:type "date" :name "published_at" :class "con-input"
                        :value (or (epoch->date (:published_at p)) "")}])
    (con/field {:label "URL slug" :hint "Leave blank to build it from the title"}
               (con/text-input {:name "slug" :value (or (:slug p) "")}))
    (con/field {:label "Image" :hint "Cloudflare image ID — shown on the news card"}
               (con/text-input {:name "image_id" :value (or (:image_id p) "")}))
    (con/field {:label "Home page"}
               [:label {:class "con-check"}
                [:input {:type "checkbox" :name "show_on_home" :value "1"
                         :checked (= 1 (:show_on_home p))}]
                "Show as a teaser on the home page"])
    (con/field {:label "Summary" :hint "Shown on the news listing, above the link" :wide? true}
               [:textarea {:name "excerpt" :class "con-input con-textarea"}
                (or (:excerpt p) "")])]])

(defn- editor [p]
  (let [new?   (nil? (:id p))
        action (if new? "/console/writing" (str "/console/writing/" (:id p)))]
    [:section {:class "con-editor"}
     [:form {:method "post" :action action :class "con-form"
             :id "con-post-form"
             :data-autosave (when-not new? (str action "/autosave"))}
      (ui/anti-forgery-field)

      [:div {:class "con-editor-bar"}
       (if new?
         [:span {:class "con-pill con-pill--draft"} [:span {:class "con-pill-dot"}] "New draft"]
         (con/status-pill (str action "/status") (:status p)))
       [:div {:class "con-editor-bar-right"}
        [:span {:id "con-saved" :class "con-saved"}]
        [:button {:type "submit" :class "con-btn con-btn--primary"} "Save"]
        (when-not new?
          [:button {:type "submit" :class "con-btn con-btn--quiet"
                    :formaction (str action "/archive")
                    :formnovalidate "true"
                    :onclick "return confirm('Archive this post? It leaves the site but is kept under Archive.')"}
           "Archive"])]]

      [:input {:type "text" :name "title" :class "con-title-input"
               :value (or (:title p) "") :placeholder "Title" :required "true"
               :autocomplete "off"}]

      [:div {:class "con-body-editor"}
       [:div {:data-tiptap "body" :class "tiptap-wrapper"}]
       [:input {:type "hidden" :name "body" :value (or (:body p) "")}]]

      (details-strip p)]

     (when-not new?
       [:p {:class "con-editor-foot"}
        (if (= content/published (:status p))
          (list "Live at " [:a {:href (str "/news/" (:slug p)) :target "_blank"}
                            (str "/news/" (:slug p))])
          "Not on the site yet — click Draft above to publish it.")])]))

(defn- writing-page [ctx p posts filters]
  (con/page (if p (or (not-empty (:title p)) "Untitled") "Writing")
            {:active :writing}
            [:div {:class "con-pane"}
             (writing-list ctx posts (:id p) filters)
             (if p
               (editor p)
               (con/empty-state "Pick something to edit"
                                [:p "Or start a new one — a reflection, a news item, an announcement."]
                                [:a {:href "/console/writing/new" :class "con-btn con-btn--primary"}
                                 "+ New post"]))]
            [:script {:src "/js/console.js" :defer "true"}]))

(defn writing
  "The pane. With an :id in the path that post is open in the editor; without
  one the editor shows an empty state."
  [{:keys [path-params query-params] :as ctx}]
  (let [filters {:cat (get query-params "cat") :q (get query-params "q")}
        posts   (visible-posts ctx filters)
        p       (when-let [id (:id path-params)] (content/get-one ctx :post id))]
    (if (and (:id path-params) (nil? p))
      {:status 404 :body "No such post"}
      (writing-page ctx p posts filters))))

(defn writing-new [ctx]
  (writing-page ctx {} (visible-posts ctx nil) nil))

(defn writing-create [ctx]
  (let [id (new-id)]
    (content/save! ctx :post id (assoc (post-cols ctx nil) :created_at (now-epoch)))
    {:status 303 :headers {"location" (str "/console/writing/" id)}}))

(defn writing-save [{:keys [path-params] :as ctx}]
  (let [id (:id path-params)]
    (content/save! ctx :post id (post-cols ctx id))
    {:status 303 :headers {"location" (str "/console/writing/" id)}}))

(defn writing-autosave
  "Saves silently while you type and returns the little timestamp. Never touches
  publish state, so autosaving a live post cannot change what the site shows
  beyond the edit itself."
  [{:keys [path-params] :as ctx}]
  (let [id (:id path-params)]
    (content/save! ctx :post id (post-cols ctx id))
    (fragment [:span {:id "con-saved" :class "con-saved"}
               (str "Saved " (.format (java.time.LocalTime/now normalize/eastern)
                                      (java.time.format.DateTimeFormatter/ofPattern "h:mm a")))])))

(defn writing-status
  "Toggles the pill, and swaps the matching row's dot out of band so the listing
  agrees with the editor without a reload."
  [{:keys [path-params] :as ctx}]
  (let [id (:id path-params)
        _  (content/toggle! ctx :post id)
        p  (content/get-one ctx :post id)]
    (fragment
     (list (con/status-pill (str "/console/writing/" id "/status") (:status p))
           [:span {:class (str "con-dot con-dot--" (:status p))
                   :id (str "con-row-dot-" id)
                   :hx-swap-oob "true"
                   :title (content/status-label (:status p))}]))))

(defn writing-archive [{:keys [path-params] :as ctx}]
  (content/archive! ctx :post (:id path-params))
  {:status 303 :headers {"location" "/console/writing"}})

;; ---------------------------------------------------------------------------
;; Archive — where Delete used to be
;; ---------------------------------------------------------------------------

(def ^:private archive-types [:post :event :feature :page :sermon])

(defn- archived-rows [ctx]
  (->> archive-types
       (mapcat (fn [t]
                 (map #(assoc % ::type t) (content/ls ctx t {:status content/archived}))))
       (sort-by #(or (:archived_at %) 0) >)))

(defn archive-list [ctx]
  (let [rows (archived-rows ctx)]
    (con/page "Archive" {:active :archive}
              [:div {:class "con-single"}
               [:div {:class "con-list-head con-list-head--page"}
                [:h1 {:class "con-list-title"} "Archive"]]
               [:p {:class "con-hint con-hint--block"}
                "Nothing here is on the site. Restore brings an item back as a draft — "
                "never straight back to live. Deleting is permanent and only possible from here."]
               (if (empty? rows)
                 [:p {:class "con-rows-empty"} "Nothing archived."]
                 [:table {:class "con-table"}
                  [:thead [:tr [:th "Title"] [:th "Kind"] [:th "Archived"] [:th ""]]]
                  [:tbody
                   (for [r rows
                         :let [t (::type r)
                               base (str "/console/archive/" (name t) "/" (:id r))]]
                     [:tr
                      [:td (or (not-empty (:title r)) (:slug r) "Untitled")]
                      [:td [:span {:class "con-kind"} (:label (content/spec t))]]
                      [:td {:class "con-num"} (or (epoch->date (:archived_at r)) "—")]
                      [:td
                       [:div {:class "con-row-actions"}
                        [:form {:method "post" :action (str base "/restore")}
                         (ui/anti-forgery-field)
                         [:button {:type "submit" :class "con-btn con-btn--ghost"} "Restore"]]
                        [:form {:method "post" :action (str base "/delete")}
                         (ui/anti-forgery-field)
                         [:button {:type "submit" :class "con-btn con-btn--danger"
                                   :onclick "return confirm('Delete permanently? This cannot be undone.')"}
                          "Delete"]]]]])]])])))

(defn- archive-type [path-params]
  (let [t (keyword (:type path-params))]
    (when (some #{t} archive-types) t)))

(defn archive-restore [{:keys [path-params] :as ctx}]
  (when-let [t (archive-type path-params)]
    (content/restore! ctx t (:id path-params)))
  {:status 303 :headers {"location" "/console/archive"}})

(defn archive-purge [{:keys [path-params] :as ctx}]
  (when-let [t (archive-type path-params)]
    (content/purge! ctx t (:id path-params)))
  {:status 303 :headers {"location" "/console/archive"}})

;; ---------------------------------------------------------------------------
;; Dashboard
;; ---------------------------------------------------------------------------

(defn- pane-card [{:keys [title sub href rows footer]}]
  [:div {:class "con-card"}
   [:div {:class "con-card-head"}
    [:a {:href href :class "con-card-title"} title]
    [:span {:class "con-card-sub"} sub]]
   [:div {:class "con-card-rows"} rows]
   [:div {:class "con-card-foot"} footer]])

(defn dashboard [ctx]
  (let [counts   (content/counts-by-status ctx :post)
        recent   (take 5 (content/ls ctx :post {:status #{content/draft content/published}}))
        n-ep     (now-epoch)
        upcoming (take 5 (event/next-occurrences
                          (event/with-skips
                           ctx (content/live ctx :event {:where (event/upcoming-where n-ep)
                                                         :order [[:start_at :asc]]}))
                          n-ep))]
    (con/page "Console" {:active nil}
              [:div {:class "con-dash"}
               [:div {:class "con-dash-head"}
                [:h1 {:class "con-dash-title"} "Mt Zion Console"]
                [:span {:class "con-dash-date"}
                 (.format (java.time.LocalDate/now normalize/eastern)
                          (java.time.format.DateTimeFormatter/ofPattern "EEEE · MMM d"))]]
               [:div {:class "con-dash-grid"}
                (pane-card
                 {:title "Writing" :href "/console/writing"
                  :sub   (str (get counts content/published 0) " live · "
                              (get counts content/draft 0) " draft")
                  :rows  (if (empty? recent)
                           [:p {:class "con-rows-empty"} "Nothing written yet."]
                           (for [p recent]
                             [:a {:href (str "/console/writing/" (:id p)) :class "con-card-row"}
                              (con/status-dot (:status p))
                              [:span {:class "con-row-title"} (or (not-empty (:title p)) "Untitled")]]))
                  :footer [:a {:href "/console/writing/new" :class "con-btn con-btn--primary"}
                           "+ New post"]})

                (pane-card
                 {:title "Site" :href "/console/site" :sub "Pages & sections"
                  :rows  [:p {:class "con-rows-empty"}
                          "The page outline is the next thing being built."]
                  :footer [:a {:href "/admin/pages" :class "con-btn con-btn--ghost"}
                           "Edit pages in /admin"]})

                (pane-card
                 {:title "Calendar" :href "/console/calendar" :sub "Events"
                  :rows  (if (empty? upcoming)
                           [:p {:class "con-rows-empty"} "No upcoming events."]
                           (for [e upcoming]
                             [:span {:class "con-card-row"}
                              [:span {:class "con-card-date"}
                               (or (event-day (:start_at e)) "")]
                              [:span {:class "con-row-title"} (:title e)]]))
                  :footer [:a {:href "/admin/events" :class "con-btn con-btn--ghost"}
                           "Edit events in /admin"]})]])))

;; ---------------------------------------------------------------------------
;; Panes not built yet
;; ---------------------------------------------------------------------------

(defn- placeholder [title active blurb href label]
  (fn [_ctx]
    (con/page title {:active active}
              [:div {:class "con-single"}
               (con/not-built-yet title blurb href label)])))

(def inbox-pane
  (placeholder "Inbox" :inbox
               (str "This becomes the review queue for content extracted from the Sunday "
                    "bulletin. Until then the importer writes drafts straight into the "
                    "content tables — run clj -M:run import to see the diff.")
               "/console/writing" "Back to Writing"))

(def media-pane
  (placeholder "Media" :media
               (str "This becomes one upload dialog and a searchable library with albums. "
                    "Until then, images are uploaded and browsed in /admin.")
               "/admin/images" "Image library in /admin →"))

;; ---------------------------------------------------------------------------
;; Module
;; ---------------------------------------------------------------------------

(def module
  {:biff.ring/routes
   [["/console" {:middleware [[wrap-signed-in]]}
     ["" {:get dashboard :name ::dashboard}]
     ["/writing"
      ["" {:get writing :post writing-create :name ::writing}]
      ["/new" {:get writing-new :name ::writing-new :conflicting true}]
      ["/:id" {:get writing :post writing-save :name ::writing-one :conflicting true}]
      ["/:id/autosave" {:post writing-autosave :name ::writing-autosave}]
      ["/:id/status"   {:post writing-status   :name ::writing-status}]
      ["/:id/archive"  {:post writing-archive  :name ::writing-archive}]]
     ["/inbox"    {:get inbox-pane    :name ::inbox}]
     ["/media"    {:get media-pane    :name ::media}]
     ["/archive"
      ["" {:get archive-list :name ::archive}]
      ["/:type/:id/restore" {:post archive-restore :name ::archive-restore}]
      ["/:type/:id/delete"  {:post archive-purge   :name ::archive-purge}]]]]})
