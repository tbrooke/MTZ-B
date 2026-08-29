(ns com.mtzion.app.events
  (:require [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.model.event :as event]
            [com.mtzion.model.normalize :as norm]
            [com.mtzion.ui.base :as base]
            [com.mtzion.ui.sections :as sections]
            [lambdaisland.hiccup :as hiccup]))

(def ^:private normalize norm/snake-keys-all)
(def ^:private now-epoch norm/now-epoch)

;; start_at/end_at are church wall-clock instants (see model.normalize), so they
;; must be rendered in Eastern. Rendering them in UTC showed every event 4-5
;; hours late.
(defn- format-date [epoch]
  (when epoch
    (-> (java.time.Instant/ofEpochSecond epoch)
        (java.time.LocalDate/ofInstant norm/eastern)
        (.format (java.time.format.DateTimeFormatter/ofPattern "EEEE, MMMM d, yyyy")))))

(defn- format-time [epoch]
  (when epoch
    (-> (java.time.Instant/ofEpochSecond epoch)
        (java.time.LocalTime/ofInstant norm/eastern)
        (.format (java.time.format.DateTimeFormatter/ofPattern "h:mm a")))))

(defn- cf-img-url [ctx image-id]
  (when (seq image-id)
    (str "https://imagedelivery.net/" (:cf/images-hash ctx) "/" image-id "/public")))

(defn- event-row [ev ctx]
  [:div {:class "mtz-row"
         :style "padding: 24px 0; border-bottom: 1px solid var(--mtz-rule); gap: 32px; align-items: start;"}
   (when-let [img (cf-img-url ctx (:image_id ev))]
     [:div {:style "flex: 0 0 140px;"}
      [:img {:src img :alt (:title ev)
             :style "width:140px; height:90px; object-fit:cover; border-radius:4px; display:block;"}]])
   [:div {:style "flex: 1; min-width: 0;"}
    [:div {:class "mtz-mono"
           :style "font-size: 13px; color: var(--mtz-mint-dark); margin-bottom: 6px; font-weight: 600;"}
     (str (format-date (:start_at ev))
          (when (and (not= 1 (:all_day ev)) (:start_at ev))
            (str " · " (format-time (:start_at ev))))
          (when (seq (:location ev)) (str " · " (:location ev)))
          (when (not= "none" (:recurrence ev "none"))
            (case (:recurrence ev)
              "weekly"   " · Weekly"
              "biweekly" " · Every 2 weeks"
              "monthly"  " · Monthly"
              "daily"    " · Daily"
              "yearly"   " · Yearly"
              "")))]
    [:h3 {:class "mtz-h3" :style "font-size: 20px; margin-bottom: 8px;"} (:title ev)]
    (when (seq (:description ev))
      [:div {:class "mtz-prose"
             :style "color: var(--mtz-ink-soft); font-size: 15px; margin: 0;"}
       [::hiccup/unsafe-html (:description ev)]])]])

(defn- featured-card [ev ctx]
  (let [img (cf-img-url ctx (:image_id ev))]
    [:article {:class "mtz-card"}
     (when img
       [:div {:class "mtz-flyer-img"}
        [:img {:src img :alt (:title ev)
               :style "width:100%; height:100%; object-fit:cover; display:block;"}]])
     [:div {:class "mtz-card-body"}
      [:p {:class "mtz-card-meta"}
       (str (format-date (:start_at ev))
            (when (and (not= 1 (:all_day ev)) (:start_at ev))
              (str " · " (format-time (:start_at ev))))
            (when (seq (:location ev)) (str " · " (:location ev))))]
      [:h3 {:class "mtz-h3" :style "font-size: 22px; margin-bottom: 8px;"} (:title ev)]
      (when (seq (:description ev))
        [:div {:style "color: var(--mtz-ink-soft); font-size: 15px; margin: 0 0 12px;"}
         [::hiccup/unsafe-html (:description ev)]])
      [:a {:class "mtz-arrow-link" :href "/contact"} "Get in Touch →"]]]))

(defn- page-content [ctx events featured-events]
  (list
   [:section {:class "mtz-section"}
    [:p {:class "mtz-kicker"} "What's Coming Up"]
    [:h1 {:class "mtz-h1" :style "max-width: 760px;"} "Events & Calendar"]
    [:p {:class "mtz-lede" :style "max-width: 640px;"}
     "From Sunday worship to community gatherings, there's always something happening at Mt. Zion."]
    [:hr {:class "mtz-rule"}]]

   (when (seq featured-events)
     [:section {:class "mtz-section--tint"}
      [:div {:class "mtz-section-inner"}
       [:div {:class "mtz-row"
              :style "justify-content: space-between; align-items: baseline; margin-bottom: 28px;"}
        [:div
         [:p {:class "mtz-kicker" :style "margin: 0;"} "Featured"]
         [:h2 {:class "mtz-h2" :style "margin: 4px 0 0;"} "Mark your calendar."]]
        [:a {:class "mtz-arrow-link" :href "/contact"} "Questions? Contact us →"]]
       [:div {:class (if (> (count featured-events) 1) "mtz-grid mtz-grid--2" "mtz-grid")
              :style "gap: 36px; align-items: stretch;"}
        (map #(featured-card % ctx) featured-events)]]])

   [:section {:class "mtz-section"}
    [:div {:class "mtz-row"
           :style "justify-content: space-between; align-items: baseline; margin-bottom: 32px;"}
     [:h2 {:class "mtz-h2" :style "margin: 0;"} "Upcoming Events"]
     [:a {:class "mtz-arrow-link" :href "/calendar.ics"} "Subscribe to calendar →"]]
    (if (seq events)
      [:div {:style "border-top: 1px solid var(--mtz-ink);"}
       (map #(event-row % ctx) events)]
      [:p {:class "mtz-mute" :style "padding: 32px 0;"}
       "No upcoming events at the moment. Check back soon."])]

   ;; Sections added in the console — unlimited, in the editor's order.
   (sections/region ctx "events")

   [:section {:class "mtz-section--cream"}
    [:div {:class "mtz-section-inner" :style "text-align: center;"}
     [:h2 {:class "mtz-h2" :style "margin-bottom: 16px;"} "Stay Connected"]
     [:p {:class "mtz-lede" :style "max-width: 520px; margin: 0 auto 28px;"}
      "Subscribe to our calendar to get Mt. Zion events right in your calendar app."]
     [:a {:class "mtz-btn mtz-btn--ghost" :href "/calendar.ics"} "Subscribe (.ics)"]]]))

(defn events [ctx]
  (let [n-ep            (now-epoch)
        ;; upcoming-where keeps recurring events (whose start_at is in the past
        ;; by design) in the list; next-occurrences then dates each one correctly.
        upcoming-src    (normalize (biff.sqlite/execute ctx {:select :*
                                                             :from   :event
                                                             :where  [:and [:= :status "published"]
                                                                      (event/upcoming-where n-ep)]}))
        rows            (event/next-occurrences (event/with-skips ctx upcoming-src) n-ep)
        featured-events (filter #(= 1 (:featured %)) rows)]
    (base/page ctx "Events — Mount Zion UCC" (page-content ctx rows featured-events))))

(def module
  {:biff.ring/routes
   [["/events" {:get events :name ::events}]]})
