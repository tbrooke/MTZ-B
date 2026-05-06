(ns com.mtzion.app.contact
  (:require [com.mtzion.ui.base :as base]))

(defn- page-content []
  (list
   [:section {:class "mtz-section"}
    [:p {:class "mtz-kicker"} "We'd Love to Hear From You"]
    [:h1 {:class "mtz-h1" :style "max-width: 760px;"} "Contact Us"]
    [:p {:class "mtz-lede" :style "max-width: 640px;"}
     "Reach out with questions, prayer requests, or to learn more about Mt. Zion."]
    [:hr {:class "mtz-rule"}]]

   [:section {:class "mtz-section"}
    [:div {:class "mtz-grid mtz-grid--2" :style "gap: 72px; align-items: start;"}
     [:div
      [:h2 {:class "mtz-h2" :style "margin-bottom: 32px;"} "Send Us a Message"]
      [:form {:method "post" :action "/contact"
              :class "mtz-stack" :style "gap: 20px;"}
       [:div
        [:label {:for "contact-name"
                 :style "display: block; font-family: var(--mtz-sans-menu); font-size: 13px; font-weight: 600; letter-spacing: 0.08em; text-transform: uppercase; color: var(--mtz-ink-mute); margin-bottom: 6px;"}
         "Your Name"]
        [:input {:id "contact-name" :name "name" :type "text" :required true
                 :style "width: 100%; padding: 10px 14px; border: 1px solid var(--mtz-rule); border-radius: 4px; font-family: var(--mtz-serif-body); font-size: 16px; background: var(--mtz-bg);"}]]
       [:div
        [:label {:for "contact-email"
                 :style "display: block; font-family: var(--mtz-sans-menu); font-size: 13px; font-weight: 600; letter-spacing: 0.08em; text-transform: uppercase; color: var(--mtz-ink-mute); margin-bottom: 6px;"}
         "Email Address"]
        [:input {:id "contact-email" :name "email" :type "email" :required true
                 :style "width: 100%; padding: 10px 14px; border: 1px solid var(--mtz-rule); border-radius: 4px; font-family: var(--mtz-serif-body); font-size: 16px; background: var(--mtz-bg);"}]]
       [:div
        [:label {:for "contact-message"
                 :style "display: block; font-family: var(--mtz-sans-menu); font-size: 13px; font-weight: 600; letter-spacing: 0.08em; text-transform: uppercase; color: var(--mtz-ink-mute); margin-bottom: 6px;"}
         "Message"]
        [:textarea {:id "contact-message" :name "message" :rows "6" :required true
                    :style "width: 100%; padding: 10px 14px; border: 1px solid var(--mtz-rule); border-radius: 4px; font-family: var(--mtz-serif-body); font-size: 16px; background: var(--mtz-bg); resize: vertical;"}]]
       [:button {:type "submit" :class "mtz-btn mtz-btn--primary" :style "align-self: flex-start;"}
        "Send Message"]]]

     [:div
      [:h2 {:class "mtz-h2" :style "margin-bottom: 24px;"} "Church Office"]
      [:div {:class "mtz-stack" :style "gap: 28px;"}
       [:div
        [:p {:class "mtz-mono" :style "font-size: 12px; letter-spacing: 0.12em; color: var(--mtz-mint-dark); font-weight: 600; text-transform: uppercase; margin: 0 0 6px;"}
         "Address"]
        [:p {:style "color: var(--mtz-ink-soft); margin: 0; line-height: 1.7;"}
         "1415 S Main St" [:br]
         "China Grove, NC 28023"]]
       [:div
        [:p {:class "mtz-mono" :style "font-size: 12px; letter-spacing: 0.12em; color: var(--mtz-mint-dark); font-weight: 600; text-transform: uppercase; margin: 0 0 6px;"}
         "Phone"]
        [:a {:href "tel:+17048571169"
             :style "color: var(--mtz-ink-soft); text-decoration: underline; text-underline-offset: 2px;"}
         "(704) 857-1169"]]
       [:div
        [:p {:class "mtz-mono" :style "font-size: 12px; letter-spacing: 0.12em; color: var(--mtz-mint-dark); font-weight: 600; text-transform: uppercase; margin: 0 0 6px;"}
         "Office Hours"]
        [:p {:style "color: var(--mtz-ink-soft); margin: 0; line-height: 1.7;"}
         "Tuesday – Friday" [:br]
         "9:00 AM – 2:00 PM"]]
       [:div
        [:p {:class "mtz-mono" :style "font-size: 12px; letter-spacing: 0.12em; color: var(--mtz-mint-dark); font-weight: 600; text-transform: uppercase; margin: 0 0 6px;"}
         "Sunday Worship"]
        [:p {:style "color: var(--mtz-ink-soft); margin: 0;"} "10:30 AM"]]
       [:div {:class "mtz-img" :style "aspect-ratio: 4/3; min-height: 0; border-radius: 8px;"}
        [:span {:class "mtz-img-label"} "map · 1415 S Main St"]]]]]]))

(defn contact-get [_ctx]
  (base/page "Contact — Mount Zion UCC" (page-content)))

(defn contact-post [_ctx]
  {:status  303
   :headers {"Location" "/contact?sent=1"}})

(def module
  {:biff.ring/routes
   [["/contact" {:get  contact-get
                 :post contact-post
                 :name ::contact}]]})
