(ns com.mtzion.app.preschool
  (:require [com.mtzion.ui.base :as base]))

(defn- page-content []
  (list
   [:section {:class "mtz-section"}
    [:p {:class "mtz-kicker"} "A nurturing Christian early childhood program"]
    [:h1 {:class "mtz-h1" :style "max-width: 760px;"} "Mt. Zion Preschool"]
    [:p {:class "mtz-lede" :style "max-width: 640px;"}
     "Providing quality early childhood education in a warm, faith-based environment "
     "for children ages 2–5 in China Grove and Rowan County."]
    [:div {:class "mtz-row" :style "gap: 12px; margin-top: 28px; flex-wrap: wrap;"}
     [:a {:class "mtz-btn mtz-btn--primary" :href "/contact"} "Inquire About Enrollment"]
     [:a {:class "mtz-btn mtz-btn--ghost"   :href "#programs"} "Our Programs"]]
    [:hr {:class "mtz-rule" :style "margin-top: 48px;"}]]

   [:section {:id "programs" :class "mtz-section"}
    [:h2 {:class "mtz-h2" :style "margin-bottom: 32px;"} "Our Programs"]
    [:div {:class "mtz-grid mtz-grid--3"}
     (for [[title age days desc] [["Toddler Class"
                                   "Ages 2–3"
                                   "Tue & Thu · 9 AM–12 PM"
                                   "Gentle introduction to structured learning through play, song, and story in a nurturing classroom."]
                                  ["PreK-3"
                                   "Age 3"
                                   "Mon, Wed & Fri · 9 AM–12 PM"
                                   "Building social skills, early literacy, and creativity through hands-on activities and circle time."]
                                  ["PreK-4"
                                   "Age 4–5"
                                   "Mon–Fri · 9 AM–12 PM"
                                   "School-readiness focus with phonics, math readiness, science exploration, and daily chapel."]]]
       [:div {:class "mtz-card" :style "padding: 28px;"}
        [:p {:class "mtz-mono"
             :style "font-size: 12px; letter-spacing: 0.12em; color: var(--mtz-mint-dark); font-weight: 600; text-transform: uppercase; margin: 0 0 6px;"}
         age]
        [:h3 {:class "mtz-h3" :style "font-size: 22px; margin-bottom: 8px;"} title]
        [:p {:class "mtz-mute" :style "font-size: 13px; margin: 0 0 12px;"} days]
        [:p {:style "color: var(--mtz-ink-soft); margin: 0; font-size: 15px; line-height: 1.6;"} desc]])]]

   [:section {:class "mtz-section--cream"}
    [:div {:class "mtz-section-inner"}
     [:div {:class "mtz-grid mtz-grid--2" :style "gap: 64px; align-items: center;"}
      [:div {:class "mtz-img" :style "aspect-ratio: 4/3; min-height: 0;"}
       [:span {:class "mtz-img-label"} "classroom · preschool"]]
      [:div
       [:p {:class "mtz-kicker"} "Our Approach"]
       [:h2 {:class "mtz-h2"} "Learning through love."]
       [:p {:class "mtz-prose" :style "color: var(--mtz-ink-soft);"}
        "Mt. Zion Preschool believes that the early childhood years are foundational. "
        "Our program blends developmentally appropriate practice with a gentle Christian "
        "environment — daily chapel, seasonal celebrations, and a caring staff that "
        "treats every child as a gift."]
       [:p {:class "mtz-prose" :style "color: var(--mtz-ink-soft);"}
        "We are licensed by the state of North Carolina and maintain small class sizes "
        "to ensure every child gets individual attention."]]]]]

   [:section {:class "mtz-section--tint"}
    [:div {:class "mtz-section-inner"}
     [:div {:class "mtz-grid mtz-grid--3"}
      (for [[label value] [["Licensed by" "NC DHHS Division of Child Development"]
                           ["Class Size"  "Max 12 students per class"]
                           ["Calendar"    "September – May · follows Rowan-Salisbury Schools"]]]
        [:div {:style "text-align: center;"}
         [:p {:class "mtz-mono" :style "font-size: 12px; letter-spacing: 0.12em; color: var(--mtz-mint-dark); font-weight: 600; text-transform: uppercase; margin: 0 0 8px;"}
          label]
         [:p {:style "font-family: var(--mtz-serif-display); font-size: 18px; color: var(--mtz-ink); margin: 0;"}
          value]])]]]

   [:section {:class "mtz-section" :style "text-align: center;"}
    [:h2 {:class "mtz-h2" :style "margin-bottom: 16px;"} "Enrollment & Registration"]
    [:p {:class "mtz-lede" :style "max-width: 520px; margin: 0 auto 28px;"}
     "We accept rolling enrollment throughout the year, subject to availability. "
     "Contact us to schedule a visit and learn about openings."]
    [:div {:class "mtz-row" :style "justify-content: center; gap: 12px; flex-wrap: wrap;"}
     [:a {:class "mtz-btn mtz-btn--primary" :href "/contact"} "Inquire About Enrollment"]
     [:a {:class "mtz-btn mtz-btn--ghost"   :href "tel:+17048571169"} "(704) 857-1169"]]]))

(defn preschool [_ctx]
  (base/page "Mt. Zion Preschool" (page-content) :preschool))

(def module
  {:biff.ring/routes
   [["/preschool" {:get preschool :name ::preschool}]]})
