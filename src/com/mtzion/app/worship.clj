(ns com.mtzion.app.worship
  (:require [com.mtzion.app.home-sections :as home-sections]
            [com.mtzion.ui.base :as base]))

(def ^:private placeholder-sermons
  [{:date "May 4, 2026"   :scripture "John 14:1-14"    :title "I Am the Way"         :preacher "Pastor Jim"}
   {:date "April 27, 2026" :scripture "Acts 9:1-20"    :title "Unexpected Encounters" :preacher "Pastor Jim"}
   {:date "April 20, 2026" :scripture "John 20:19-31"  :title "Peace Be With You"     :preacher "Pastor Jim"}])

(defn- page-content []
  (list
   [:section {:class "mtz-section"}
    [:p {:class "mtz-kicker"} "Sunday Worship · 10:30 AM"]
    [:h1 {:class "mtz-h1" :style "max-width: 760px;"} "Sunday Worship"]
    [:p {:class "mtz-lede" :style "max-width: 640px;"}
     "Join us each Sunday for traditional worship with choir, organ, and sermon. "
     "All are welcome at every service."]
    [:hr {:class "mtz-rule"}]]

   (home-sections/this-sunday-section)

   [:section {:class "mtz-section--cream"}
    [:div {:class "mtz-section-inner"}
     [:div {:class "mtz-row"
            :style "justify-content: space-between; align-items: baseline; margin-bottom: 28px;"}
      [:h2 {:class "mtz-h2" :style "margin: 0;"} "Recent Sermons"]
      [:a {:class "mtz-arrow-link" :href "/worship#archive"} "Full archive →"]]
     [:div {:class "mtz-grid mtz-grid--3"}
      (for [s placeholder-sermons]
        [:article {:class "mtz-card"}
         [:div {:class "mtz-img" :style "aspect-ratio: 16/9; border-radius: 0; border-left: 0; border-right: 0; border-top: 0;"}
          [:div {:style "position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;"}
           [:div {:style "width: 48px; height: 48px; border-radius: 50%; background: rgba(0,0,0,0.35); display: flex; align-items: center; justify-content: center; color: #fff; font-size: 18px;"}
            "▶"]]]
         [:div {:class "mtz-card-body"}
          [:p {:class "mtz-card-meta"} (str (:date s) " · " (:scripture s))]
          [:h3 {:class "mtz-h3" :style "font-size: 20px; margin-bottom: 6px;"} (:title s)]
          [:p {:style "color: var(--mtz-ink-soft); font-size: 14px; margin: 0;"} (:preacher s)]]])]]]

   [:section {:class "mtz-section"}
    [:div {:class "mtz-grid mtz-grid--2" :style "gap: 64px; align-items: center;"}
     [:div
      [:p {:class "mtz-kicker"} "Music Ministry"]
      [:h2 {:class "mtz-h2"} "The sound of Mt. Zion."]
      [:p {:class "mtz-prose" :style "color: var(--mtz-ink-soft);"}
       "Our sanctuary choir rehearses every Wednesday at 7:00 PM and sings at the "
       "10:30 AM service. We also feature seasonal ensembles, handbell choir, and "
       "special music throughout the year. New voices are always welcome — no audition required."]
      [:a {:class "mtz-arrow-link" :href "/activities" :style "margin-top: 16px; display: inline-flex;"}
       "Choir rehearsal schedule →"]]
     [:div {:class "mtz-img" :style "aspect-ratio: 4/3; min-height: 0;"}
      [:span {:class "mtz-img-label"} "choir · sanctuary"]]]]

   [:section {:class "mtz-section--tint"}
    [:div {:class "mtz-section-inner" :style "text-align: center;"}
     [:h2 {:class "mtz-h2" :style "margin-bottom: 16px;"} "Plan Your Visit"]
     [:p {:class "mtz-lede" :style "max-width: 520px; margin: 0 auto 32px;"}
      "We meet at 1415 S Main St, China Grove, NC 28023. "
      "Sunday worship begins at 10:30 AM."]
     [:div {:class "mtz-row" :style "justify-content: center; gap: 12px; flex-wrap: wrap;"}
      [:a {:class "mtz-btn mtz-btn--primary" :href "/contact"} "Get Directions"]
      [:a {:class "mtz-btn mtz-btn--ghost"   :href "/about"}   "Learn More About Us"]]]]))

(defn worship [_ctx]
  (base/page "Sunday Worship — Mount Zion UCC" (page-content)))

(def module
  {:biff.ring/routes
   [["/worship" {:get worship :name ::worship}]]})
