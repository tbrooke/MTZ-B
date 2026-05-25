(ns com.mtzion.app.outreach
  (:require [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.app.home-sections :as home-sections]
            [com.mtzion.ui.base :as base]
            [lambdaisland.hiccup :as hiccup]))

(defn- page-content []
  (list
   [:section {:class "mtz-section"}
    [:p {:class "mtz-kicker"} "Serving Rowan County & Beyond"]
    [:h1 {:class "mtz-h1" :style "max-width: 760px;"} "Outreach Ministries"]
    [:p {:class "mtz-lede" :style "max-width: 640px;"}
     "Mt. Zion believes faith is expressed in action. Our outreach partners "
     "and programs connect our congregation with neighbors in need."]
    [:hr {:class "mtz-rule"}]]

   (home-sections/outreach-section)

   [:section {:class "mtz-section"}
    [:div {:class "mtz-grid mtz-grid--2" :style "gap: 64px; align-items: center;"}
     [:div {:class "mtz-img" :style "aspect-ratio: 4/3; min-height: 0;"}
      [:span {:class "mtz-img-label"} "volunteers · food drive"]]
     [:div
      [:p {:class "mtz-kicker"} "Get Involved"]
      [:h2 {:class "mtz-h2"} "Volunteer with us."]
      [:p {:class "mtz-prose" :style "color: var(--mtz-ink-soft);"}
       "Our outreach ministries are powered by congregation volunteers. "
       "Whether you have an hour a month or a day a week to give, there's a "
       "place for you. No experience required — just a willingness to help."]
      [:div {:class "mtz-row" :style "gap: 12px; margin-top: 24px; flex-wrap: wrap;"}
       [:a {:class "mtz-btn mtz-btn--primary" :href "/contact"} "Sign Up to Volunteer"]
       [:a {:class "mtz-btn mtz-btn--ghost"   :href "/contact"} "Ask a Question"]]]]]

   [:section {:class "mtz-section--cream"}
    [:div {:class "mtz-section-inner"}
     [:h2 {:class "mtz-h2" :style "margin-bottom: 28px;"} "Additional Partners"]
     [:div {:class "mtz-stack" :style "gap: 0; border-top: 1px solid var(--mtz-ink);"}
      (for [[name note desc] [["Rowan County Meals on Wheels"
                               "Monthly delivery"
                               "Congregation members deliver hot meals to homebound seniors in Rowan County."]
                              ["Carolina Cross Connection"
                               "Annual youth mission trip"
                               "Our youth group participates each summer in home repair and community service."]
                              ["Christmas Gifts for Kids"
                               "December"
                               "Annual toy drive collecting gifts for children in need throughout Cabarrus and Rowan counties."]]]
        [:div {:class "mtz-row"
               :style "padding: 24px 0; border-bottom: 1px solid var(--mtz-rule); gap: 32px; align-items: start;"}
         [:div {:class "mtz-mono" :style "min-width: 160px; color: var(--mtz-mint-dark); font-size: 13px; padding-top: 4px;"}
          note]
         [:div {:style "flex: 1;"}
          [:h3 {:class "mtz-h3" :style "font-size: 20px; margin-bottom: 6px;"} name]
          [:p {:style "color: var(--mtz-ink-soft); margin: 0; font-size: 15px;"} desc]]])]]]))

(defn outreach [ctx]
  (let [db-page (first (biff.sqlite/execute ctx {:select :* :from :page :where [:= :slug "outreach"]}))]
    (base/page "Outreach — Mount Zion UCC"
               (if (seq (:body db-page))
                 [::hiccup/unsafe-html (:body db-page)]
                 (page-content)))))

(def module
  {:biff.ring/routes
   [["/outreach" {:get outreach :name ::outreach}]]})
