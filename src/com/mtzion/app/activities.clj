(ns com.mtzion.app.activities
  (:require [com.mtzion.app.home-sections :as home-sections]
            [com.mtzion.ui.base :as base]))

(defn- page-content []
  (list
   [:section {:class "mtz-section"}
    [:p {:class "mtz-kicker"} "Open to All · No Membership Required"]
    [:h1 {:class "mtz-h1" :style "max-width: 760px;"} "Activities at Mt. Zion"]
    [:p {:class "mtz-lede" :style "max-width: 640px;"}
     "Mt. Zion is more than Sunday morning. We host a wide range of weekly "
     "activities that bring the community together throughout the week."]
    [:hr {:class "mtz-rule"}]]

   (home-sections/always-at-mtz-section)

   [:section {:class "mtz-section--cream"}
    [:div {:class "mtz-section-inner"}
     [:h2 {:class "mtz-h2" :style "margin-bottom: 8px;"} "Seasonal & Special Programs"]
     [:p {:class "mtz-mute" :style "font-size: 18px; margin: 0 0 36px; font-family: var(--mtz-serif-body);"}
      "In addition to weekly activities, we offer programs throughout the year."]
     [:div {:class "mtz-grid mtz-grid--2" :style "gap: 32px;"}
      (for [[title when- desc] [["Vacation Bible School"
                                 "Mid-June · Mon–Fri"
                                 "A week-long program for children ages 4–12 with crafts, songs, and stories."]
                                ["Advent & Christmas Programming"
                                 "December"
                                 "Cantata, Christmas Eve services, and holiday fellowship events."]
                                ["Lenten Study Series"
                                 "Feb–March"
                                 "Weekly evening reflections led by Pastor Jim during the season of Lent."]
                                ["Community Movie Night"
                                 "Quarterly"
                                 "Family-friendly films in the Fellowship Hall with popcorn provided."]]]
        [:div {:class "mtz-card" :style "padding: 24px;"}
         [:p {:class "mtz-mono"
              :style "font-size: 12px; letter-spacing: 0.12em; color: var(--mtz-mint-dark); margin: 0 0 8px; font-weight: 600; text-transform: uppercase;"}
          when-]
         [:h3 {:class "mtz-h3" :style "font-size: 20px; margin-bottom: 8px;"} title]
         [:p {:style "color: var(--mtz-ink-soft); margin: 0; font-size: 15px;"} desc]])]]]

   [:section {:class "mtz-section--tint"}
    [:div {:class "mtz-section-inner" :style "text-align: center;"}
     [:h2 {:class "mtz-h2" :style "margin-bottom: 16px;"} "Get Involved"]
     [:p {:class "mtz-lede" :style "max-width: 520px; margin: 0 auto 28px;"}
      "Questions about any of our programs? We'd love to hear from you."]
     [:a {:class "mtz-btn mtz-btn--primary" :href "/contact"} "Contact Us"]]]))

(defn activities [_ctx]
  (base/page "Activities — Mount Zion UCC" (page-content)))

(def module
  {:biff.ring/routes
   [["/activities" {:get activities :name ::activities}]]})
