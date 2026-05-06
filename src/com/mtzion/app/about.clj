(ns com.mtzion.app.about
  (:require [com.mtzion.ui.base :as base]))

(defn- page-content []
  (list
   [:section {:class "mtz-section"}
    [:p {:class "mtz-kicker"} "Est. 1858 · China Grove, NC"]
    [:h1 {:class "mtz-h1" :style "max-width: 760px;"} "About Mount Zion UCC"]
    [:p {:class "mtz-lede" :style "max-width: 680px;"}
     "A United Church of Christ congregation that has welcomed the community of "
     "China Grove for more than 165 years."]
    [:hr {:class "mtz-rule"}]]

   [:section {:class "mtz-section"}
    [:div {:class "mtz-grid mtz-grid--2" :style "gap: 64px; align-items: center;"}
     [:div {:class "mtz-img" :style "aspect-ratio: 4/5; min-height: 0;"}
      [:span {:class "mtz-img-label"} "archival photo · ca. 1910"]]
     [:div
      [:p {:class "mtz-kicker"} "Our Story"]
      [:h2 {:class "mtz-h2"} "A congregation on this hill since 1858."]
      [:p {:class "mtz-prose" :style "color: var(--mtz-ink-soft);"}
       "Mount Zion was founded in 1858 by a small group of German Reformed settlers "
       "in what was then a rural crossroads community. The original log meetinghouse "
       "gave way to a series of larger sanctuaries as the congregation grew — the "
       "current building dates to 1954."]
      [:p {:class "mtz-prose" :style "color: var(--mtz-ink-soft);"}
       "In 1957 the congregation joined the United Church of Christ at its formation, "
       "embracing an open and affirming theology that continues to define us today. "
       "We are a community shaped by history and oriented toward welcome."]
      [:a {:class "mtz-arrow-link" :href "/about#archive" :style "margin-top: 16px; display: inline-flex;"}
       "Search the archive →"]]]]

   [:section {:class "mtz-section--cream"}
    [:div {:class "mtz-section-inner"}
     [:h2 {:class "mtz-h2" :style "margin-bottom: 32px;"} "What We Believe"]
     [:div {:class "mtz-grid mtz-grid--3"}
      (for [[title body] [["Open & Affirming"
                           "All people are welcome at Mt Zion — regardless of background, identity, or where you are on your spiritual journey."]
                          ["Progressive Faith"
                           "We take scripture seriously and our tradition thoughtfully, while remaining open to continuing revelation."]
                          ["Community Rooted"
                           "Faith expressed in service: to China Grove, Rowan County, and beyond through active outreach ministries."]]]
        [:div {:class "mtz-card" :style "padding: 28px;"}
         [:h3 {:class "mtz-h3" :style "font-size: 20px; margin-bottom: 12px;"} title]
         [:p {:style "color: var(--mtz-ink-soft); margin: 0; font-size: 15px; line-height: 1.6;"} body]])]]]

   [:section {:class "mtz-section"}
    [:h2 {:class "mtz-h2" :style "margin-bottom: 32px;"} "Our Staff"]
    [:div {:class "mtz-grid mtz-grid--3"}
     [:div {:class "mtz-card" :style "padding: 28px;"}
      [:div {:class "mtz-img" :style "aspect-ratio: 1/1; border-radius: 50%; width: 96px; height: 96px; margin-bottom: 20px; min-height: 0;"}
       [:span {:class "mtz-img-label"} "photo"]]
      [:h3 {:class "mtz-h3" :style "font-size: 20px; margin-bottom: 4px;"} "Pastor Jim"]
      [:p {:class "mtz-mono" :style "font-size: 12px; letter-spacing: 0.12em; color: var(--mtz-mint-dark); margin: 0 0 12px; text-transform: uppercase;"} "Senior Pastor"]
      [:p {:style "color: var(--mtz-ink-soft); margin: 0; font-size: 15px;"} "Pastor Jim has served Mt Zion since 2008 and leads our weekly worship, Bible study, and pastoral care."]]]]

   [:section {:class "mtz-section--tint"}
    [:div {:class "mtz-section-inner" :style "text-align: center;"}
     [:h2 {:class "mtz-h2" :style "margin-bottom: 16px;"} "Come as you are."]
     [:p {:class "mtz-lede" :style "max-width: 560px; margin: 0 auto 32px;"}
      "We'd love to meet you. Reach out or join us any Sunday at 10:30 AM."]
     [:div {:class "mtz-row" :style "justify-content: center; gap: 12px; flex-wrap: wrap;"}
      [:a {:class "mtz-btn mtz-btn--primary" :href "/contact"} "Get in Touch"]
      [:a {:class "mtz-btn mtz-btn--ghost" :href "/worship"} "Plan Your Visit"]]]]))

(defn about [_ctx]
  (base/page "About — Mount Zion UCC" (page-content)))

(def module
  {:biff.ring/routes
   [["/about" {:get about :name ::about}]]})
