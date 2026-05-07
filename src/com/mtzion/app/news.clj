(ns com.mtzion.app.news
  (:require [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.ui.base :as base]
            [lambdaisland.hiccup :as hiccup]))

(defn- format-month-year [epoch]
  (when epoch
    (-> (java.time.Instant/ofEpochSecond epoch)
        (java.time.LocalDate/ofInstant java.time.ZoneOffset/UTC)
        (.format (java.time.format.DateTimeFormatter/ofPattern "MMMM yyyy")))))

(defn- post-card [p]
  [:article {:class "mtz-card"}
   (if (:image_id p)
     [:img {:src   (str "https://imagedelivery.net/" (:image_id p) "/public")
            :alt   (:title p)
            :style "width: 100%; aspect-ratio: 16/10; object-fit: cover;"}]
     [:div {:class "mtz-img"
            :style "aspect-ratio: 16/10; border-radius: 0; border-left: 0; border-right: 0; border-top: 0;"}
      [:span {:class "mtz-img-label"} "image · 800×500"]])
   [:div {:class "mtz-card-body"}
    [:p {:class "mtz-card-meta"} (str "News · " (or (format-month-year (:published_at p)) ""))]
    [:h3 {:class "mtz-h3" :style "font-size: 22px; margin-bottom: 10px;"} (:title p)]
    (when (seq (:excerpt p))
      [:p {:style "color: var(--mtz-ink-soft); font-size: 15px; margin: 0;"} (:excerpt p)])
    (when (seq (:body p))
      [:a {:href (str "/news/" (:slug p)) :class "mtz-arrow-link" :style "margin-top: 12px; display: inline-flex;"}
       "Read more →"])]])

(defn- default-news-grid []
  [:div {:class "mtz-grid mtz-grid--3"}
   (for [[tag date title excerpt]
         [["News"      "May 2026"   "Welcome to Mt Zion"      "We're glad you're here. Join us for worship each Sunday at 10:30 AM."]
          ["Community" "April 2026" "Spring Food Drive"        "Thank you to everyone who contributed to our spring food drive for Rowan Helping Ministries."]
          ["Worship"   "March 2026" "Music Ministry Update"    "Our choir is looking for new voices. No experience necessary — just a willingness to sing."]]]
     [:article {:class "mtz-card"}
      [:div {:class "mtz-img"
             :style "aspect-ratio: 16/10; border-radius: 0; border-left: 0; border-right: 0; border-top: 0;"}
       [:span {:class "mtz-img-label"} "image · 800×500"]]
      [:div {:class "mtz-card-body"}
       [:p {:class "mtz-card-meta"} (str tag " · " date)]
       [:h3 {:class "mtz-h3" :style "font-size: 22px; margin-bottom: 10px;"} title]
       [:p {:style "color: var(--mtz-ink-soft); font-size: 15px; margin: 0;"} excerpt]]])])

(defn- page-content [ctx]
  (let [posts (biff.sqlite/execute ctx {:select   :*
                                        :from     :post
                                        :where    [:is-not :published_at nil]
                                        :order-by [[:published_at :desc]]})]
    (list
     [:section {:class "mtz-section"}
      [:p {:class "mtz-kicker"} "From the Mt. Zion Community"]
      [:h1 {:class "mtz-h1" :style "max-width: 760px;"} "News & Announcements"]
      [:p {:class "mtz-lede" :style "max-width: 640px;"}
       "Updates from our congregation, community, and ministry partners."]
      [:hr {:class "mtz-rule"}]]

     [:section {:class "mtz-section--cream"}
      [:div {:class "mtz-section-inner"}
       [:h2 {:class "mtz-h2" :style "margin-bottom: 28px;"} "Latest News"]
       (if (seq posts)
         [:div {:class "mtz-grid mtz-grid--3"}
          (map post-card posts)]
         (default-news-grid))]]

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
         [:a {:class "mtz-btn mtz-btn--ghost"   :href "/contact"} "Past Issues"]]]
       [:div {:class "mtz-img" :style "aspect-ratio: 4/3; min-height: 0;"}
        [:span {:class "mtz-img-label"} "newsletter · May 2026"]]]]

     [:section {:class "mtz-section--tint"}
      [:div {:class "mtz-section-inner" :style "text-align: center;"}
       [:h2 {:class "mtz-h2" :style "margin-bottom: 16px;"} "Stay in the Loop"]
       [:p {:class "mtz-lede" :style "max-width: 520px; margin: 0 auto 28px;"}
        "Sign up to receive news and announcements directly to your inbox."]
       [:a {:class "mtz-btn mtz-btn--primary" :href "/contact"} "Subscribe to Newsletter"]]])))

(defn news [ctx]
  (base/page "News — Mount Zion UCC" (page-content ctx)))

(def module
  {:biff.ring/routes
   [["/news" {:get news :name ::news}]]})
