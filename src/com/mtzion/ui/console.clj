(ns com.mtzion.ui.console
  "Shell and shared components for the console — the task-shaped replacement for
  /admin. Three panes (Writing, Site, Calendar) plus Inbox, Media and Archive.

  Every pane is the same shape: a listing on the left, an editor on the right.
  One interaction to learn, three places to use it."
  (:require [com.mtzion.lib.ui :as ui]
            [com.mtzion.model.content :as content]
            [lambdaisland.hiccup :as hiccup]))

(defn- fonts []
  (list
   [:link {:rel "preconnect" :href "https://fonts.googleapis.com"}]
   [:link {:rel "preconnect" :href "https://fonts.gstatic.com" :crossorigin "anonymous"}]
   [:link {:rel "stylesheet"
           :href (str "https://fonts.googleapis.com/css2?"
                      "family=Fraunces:opsz,wght@9..144,300..900"
                      "&family=JetBrains+Mono:wght@400;500"
                      "&family=IBM+Plex+Sans:wght@400;500;600&display=swap")}]))

(defn top-bar
  "The console's one persistent surface. Inbox and Media live here rather than
  inside a pane because you reach for them from wherever you happen to be."
  [{:keys [inbox-count active]}]
  (let [item (fn [href label id]
               [:a {:href  href
                    :class (str "con-bar-item" (when (= id active) " is-active"))}
                label
                (when (and (= id :inbox) (pos? (or inbox-count 0)))
                  [:span {:class "con-bar-count"} (str inbox-count)])])]
    [:div {:class "con-bar"}
     [:a {:href "/console" :class "con-brand"}
      [:span {:class "con-brand-name"} "Mt Zion"]
      [:span {:class "con-brand-tag"} "Console"]]
     [:nav {:class "con-bar-nav"}
      (item "/console/writing" "Writing"  :writing)
      (item "/console/site"    "Site"     :site)
      (item "/console/calendar" "Calendar" :calendar)]
     [:div {:class "con-bar-right"}
      (item "/console/inbox"   "Inbox"   :inbox)
      (item "/console/media"   "Media"   :media)
      (item "/console/archive" "Archive" :archive)
      [:a {:href "/" :class "con-bar-item con-bar-item--quiet" :target "_blank"} "View site ↗"]
      [:form {:method "post" :action "/admin/signout" :style "margin:0;"}
       (ui/anti-forgery-field)
       [:button {:type "submit" :class "con-bar-item con-bar-item--quiet con-bar-signout"}
        "Sign out"]]]]))

(defn page
  "Full console page. `opts` are passed to top-bar."
  [title opts & body]
  {:status  200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body
   (hiccup/render
    [:html {:lang "en"}
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:title (str title " — Mt Zion Console")]
      [:link {:rel "icon" :href "data:,"}]
      (fonts)
      [:link {:rel "stylesheet" :href (ui/css-path)}]
      [:script {:src "/js/admin.js" :defer "true"}]
      [:script {:src "/js/htmx.min.js" :defer "true"}]]
     [:body {:class "con-body"}
      (top-bar opts)
      body]])})

;; ---------------------------------------------------------------------------
;; Status
;; ---------------------------------------------------------------------------

(defn status-dot
  "The compact status marker used in listings. `id` makes it addressable so a
  toggle in the editor can swap it out of band."
  ([status] (status-dot status nil))
  ([status id]
   [:span (cond-> {:class (str "con-dot con-dot--" (or status "draft"))
                   :title (content/status-label status)}
            id (assoc :id id))]))

(defn status-pill
  "The pill IS the control: it names the current state, and clicking it changes
  that state. Reading it and changing it are the same gesture — which is the
  whole point, and why there is no separate Publish button.

  Posts back and swaps itself, plus the row's dot in the listing (out of band)."
  [action-url status]
  (let [published? (= content/published status)]
    [:button {:type       "button"
              :id         "con-status-pill"
              :class      (str "con-pill con-pill--" status)
              :hx-post    action-url
              :hx-target  "#con-status-pill"
              :hx-swap    "outerHTML"
              :hx-include "[name='__anti-forgery-token']"
              :title      (if published?
                            "Live on the site — click to pull it back to a draft"
                            "Not on the site — click to publish it")}
     [:span {:class "con-pill-dot"}]
     (if published? "Published" "Draft")]))

(defn field [{:keys [label hint wide?]} & children]
  [:div {:class (str "con-field" (when wide? " con-field--wide"))}
   [:label {:class "con-label"} label]
   children
   (when hint [:span {:class "con-hint"} hint])])

(defn text-input [attrs]
  [:input (merge {:class "con-input" :type "text"} attrs)])

(defn select-input [attrs options current]
  [:select (merge {:class "con-input con-select"} attrs)
   (for [[v label] options]
     [:option {:value v :selected (= v (str current))} label])])

(defn empty-state [heading & body]
  [:div {:class "con-empty"}
   [:p {:class "con-empty-head"} heading]
   body])

(defn not-built-yet
  "Honest placeholder for a pane that has not been built. Says which phase it is
  and points at the /admin screen that still does the job today."
  [title blurb admin-href admin-label]
  [:div {:class "con-empty"}
   [:p {:class "con-empty-head"} title]
   [:p blurb]
   [:a {:href admin-href :class "con-btn con-btn--ghost"} admin-label]])
