(ns com.mtzion.app.events
  (:require [com.mtzion.ui.base :as base]))

(def ^:private upcoming-events
  [{:date "May 18, 2026"  :tags ["Fellowship"] :title "Spring Picnic"
    :desc "Annual church picnic on the lawn. Bring a dish to share. All ages welcome."}
   {:date "May 25, 2026"  :tags ["Worship"]    :title "Memorial Day Sunday"
    :desc "Special service honoring those who have served. No Sunday School."}
   {:date "June 1, 2026"  :tags ["Community"]  :title "Food Drive Drop-Off"
    :desc "Monthly food sort at Rowan Helping Ministries. Meet at the fellowship hall at 9 AM."}
   {:date "June 8, 2026"  :tags ["Youth"]      :title "Youth Group Pool Party"
    :desc "Middle and high school students are invited for an end-of-year celebration."}
   {:date "June 15, 2026" :tags ["All ages"]   :title "Vacation Bible School Kick-Off"
    :desc "VBS runs June 15–19. Registration open now — see the church office for details."}])

(defn- page-content []
  (list
   [:section {:class "mtz-section"}
    [:p {:class "mtz-kicker"} "What's Coming Up"]
    [:h1 {:class "mtz-h1" :style "max-width: 760px;"} "Events & Calendar"]
    [:p {:class "mtz-lede" :style "max-width: 640px;"}
     "From Sunday worship to community gatherings, there's always something happening at Mt. Zion."]
    [:hr {:class "mtz-rule"}]]

   [:section {:class "mtz-section--tint"}
    [:div {:class "mtz-section-inner"}
     [:p {:class "mtz-kicker" :style "margin: 0 0 12px;"} "Featured Event"]
     [:div {:class "mtz-grid mtz-grid--2" :style "gap: 48px; align-items: center;"}
      [:div {:class "mtz-img" :style "min-height: 320px; border-radius: 8px;"}
       [:span {:class "mtz-img-label"} "event poster"]]
      [:div
       [:div {:class "mtz-row" :style "gap: 8px; margin-bottom: 16px; flex-wrap: wrap;"}
        [:span {:class "mtz-tag"} [:span {:class "mtz-dot"}] "Fellowship"]
        [:span {:class "mtz-tag"} [:span {:class "mtz-dot"}] "All ages"]]
       [:h2 {:class "mtz-h2"} "Spring Picnic"]
       [:p {:class "mtz-mono mtz-mute" :style "font-size: 13px; letter-spacing: 0.10em; margin-bottom: 16px;"}
        "Sunday, May 18 · After the 10:30 AM Service"]
       [:p {:style "color: var(--mtz-ink-soft); max-width: 480px; margin: 0 0 24px;"}
        "Join us on the church lawn for our annual spring picnic. Bring a dish to share — "
        "the church will provide lemonade and dessert. All are welcome."]
       [:a {:class "mtz-btn mtz-btn--primary" :href "/contact"} "RSVP / Questions"]]]]]

   [:section {:class "mtz-section"}
    [:div {:class "mtz-row"
           :style "justify-content: space-between; align-items: baseline; margin-bottom: 32px;"}
     [:h2 {:class "mtz-h2" :style "margin: 0;"} "Upcoming Events"]
     [:a {:class "mtz-arrow-link" :href "/calendar.ics"} "Subscribe to calendar →"]]
    [:div {:class "mtz-stack" :style "gap: 0; border-top: 1px solid var(--mtz-ink);"}
     (for [e upcoming-events]
       [:div {:class "mtz-row"
              :style "padding: 24px 0; border-bottom: 1px solid var(--mtz-rule); gap: 32px; align-items: start;"}
        [:div {:class "mtz-mono" :style "min-width: 140px; color: var(--mtz-mint-dark); font-size: 13px; padding-top: 4px;"}
         (:date e)]
        [:div {:style "flex: 1;"}
         [:div {:class "mtz-row" :style "gap: 6px; margin-bottom: 8px; flex-wrap: wrap;"}
          (for [tag (:tags e)]
            [:span {:class "mtz-tag"} [:span {:class "mtz-dot"}] tag])]
         [:h3 {:class "mtz-h3" :style "font-size: 20px; margin-bottom: 6px;"} (:title e)]
         [:p {:style "color: var(--mtz-ink-soft); margin: 0; font-size: 15px;"} (:desc e)]]])]]

   [:section {:class "mtz-section--cream"}
    [:div {:class "mtz-section-inner" :style "text-align: center;"}
     [:h2 {:class "mtz-h2" :style "margin-bottom: 16px;"} "Stay Connected"]
     [:p {:class "mtz-lede" :style "max-width: 520px; margin: 0 auto 28px;"}
      "Subscribe to our calendar to get Mt. Zion events right in your calendar app."]
     [:a {:class "mtz-btn mtz-btn--ghost" :href "/calendar.ics"} "Subscribe (.ics)"]]]))

(defn events [_ctx]
  (base/page "Events — Mount Zion UCC" (page-content)))

(def module
  {:biff.ring/routes
   [["/events" {:get events :name ::events}]]})
