(ns com.mtzion.app.news
  (:require [com.mtzion.app.home-sections :as home-sections]
            [com.mtzion.ui.base :as base]))

(defn- page-content []
  (list
   [:section {:class "mtz-section"}
    [:p {:class "mtz-kicker"} "From the Mt. Zion Community"]
    [:h1 {:class "mtz-h1" :style "max-width: 760px;"} "News & Announcements"]
    [:p {:class "mtz-lede" :style "max-width: 640px;"}
     "Updates from our congregation, community, and ministry partners."]
    [:hr {:class "mtz-rule"}]]

   (home-sections/news-section)

   [:section {:class "mtz-section"}
    [:div {:class "mtz-grid mtz-grid--2" :style "gap: 64px; align-items: center;"}
     [:div
      [:p {:class "mtz-kicker"} "Monthly Newsletter"]
      [:h2 {:class "mtz-h2"} "The Mt. Zion Messenger"]
      [:p {:class "mtz-prose" :style "color: var(--mtz-ink-soft);"}
       "Our monthly newsletter includes pastoral reflections, congregation news, "
       "upcoming events, and outreach updates. Delivered by email and available "
       "in print at the church office."]
      [:div {:class "mtz-row" :style "gap: 12px; margin-top: 24px; flex-wrap: wrap;"}
       [:a {:class "mtz-btn mtz-btn--primary" :href "/contact"} "Subscribe by Email"]
       [:a {:class "mtz-btn mtz-btn--ghost" :href "/contact"} "Past Issues"]]]
     [:div {:class "mtz-img" :style "aspect-ratio: 4/3; min-height: 0;"}
      [:span {:class "mtz-img-label"} "newsletter · May 2026"]]]]

   [:section {:class "mtz-section--tint"}
    [:div {:class "mtz-section-inner" :style "text-align: center;"}
     [:h2 {:class "mtz-h2" :style "margin-bottom: 16px;"} "Stay in the Loop"]
     [:p {:class "mtz-lede" :style "max-width: 520px; margin: 0 auto 28px;"}
      "Sign up to receive news and announcements directly to your inbox."]
     [:a {:class "mtz-btn mtz-btn--primary" :href "/contact"} "Subscribe to Newsletter"]]]))

(defn news [_ctx]
  (base/page "News — Mount Zion UCC" (page-content)))

(def module
  {:biff.ring/routes
   [["/news" {:get news :name ::news}]]})
