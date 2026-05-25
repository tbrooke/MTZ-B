(ns com.mtzion.app.activities
  (:require [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.app.home-sections :as home-sections]
            [com.mtzion.ui.base :as base]
            [lambdaisland.hiccup :as hiccup]))

(defn- activity-card [f]
  [:div {:class "mtz-card" :style "padding: 24px;"}
   (when (seq (:subtitle f))
     [:p {:class "mtz-mono"
          :style "font-size: 12px; letter-spacing: 0.12em; color: var(--mtz-mint-dark); margin: 0 0 8px; font-weight: 600; text-transform: uppercase;"}
      (:subtitle f)])
   [:h3 {:class "mtz-h3" :style "font-size: 20px; margin-bottom: 8px;"} (:title f)]
   (when (seq (:body f))
     [:div {:style "color: var(--mtz-ink-soft); margin: 0; font-size: 15px;"}
      [::hiccup/unsafe-html (:body f)]])])

(defn- page-content [cards]
  (list
   [:section {:class "mtz-section"}
    [:p {:class "mtz-kicker"} "Open to All · No Membership Required"]
    [:h1 {:class "mtz-h1" :style "max-width: 760px;"} "Activities at Mt. Zion"]
    [:p {:class "mtz-lede" :style "max-width: 640px;"}
     "Mt. Zion is more than Sunday morning. We host a wide range of weekly "
     "activities that bring the community together throughout the week."]
    [:hr {:class "mtz-rule"}]]

   (home-sections/always-at-mtz-section)

   (when (seq cards)
     [:section {:class "mtz-section--cream"}
      [:div {:class "mtz-section-inner"}
       [:h2 {:class "mtz-h2" :style "margin-bottom: 8px;"} "Seasonal & Special Programs"]
       [:p {:class "mtz-mute" :style "font-size: 18px; margin: 0 0 36px; font-family: var(--mtz-serif-body);"}
        "In addition to weekly activities, we offer programs throughout the year."]
       [:div {:class "mtz-grid mtz-grid--2" :style "gap: 32px;"}
        (map activity-card cards)]]])

   [:section {:class "mtz-section--tint"}
    [:div {:class "mtz-section-inner" :style "text-align: center;"}
     [:h2 {:class "mtz-h2" :style "margin-bottom: 16px;"} "Get Involved"]
     [:p {:class "mtz-lede" :style "max-width: 520px; margin: 0 auto 28px;"}
      "Questions about any of our programs? We'd love to hear from you."]
     [:a {:class "mtz-btn mtz-btn--primary" :href "/contact"} "Contact Us"]]]))

(defn activities [ctx]
  (let [cards (biff.sqlite/execute ctx {:select   :*
                                        :from     :feature
                                        :where    [:and [:= :placement "activities"] [:= :published 1]]
                                        :order-by [[:sort_order :asc]]})]
    (base/page "Activities — Mount Zion UCC" (page-content cards))))

(def module
  {:biff.ring/routes
   [["/activities" {:get activities :name ::activities}]]})
