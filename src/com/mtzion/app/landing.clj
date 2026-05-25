(ns com.mtzion.app.landing
  (:require [clojure.string :as str]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.app.home-sections :as home-sections]
            [com.mtzion.ui.base :as base]))

(defn- normalize [row]
  (when row
    (into {} (map (fn [[k v]] [(keyword (str/replace (name k) "-" "_")) v]) row))))

(defn- format-month-year [epoch]
  (when epoch
    (-> (java.time.Instant/ofEpochSecond epoch)
        (java.time.LocalDate/ofInstant java.time.ZoneOffset/UTC)
        (.format (java.time.format.DateTimeFormatter/ofPattern "MMMM yyyy")))))

(defn- photo-hero []
  [:section {:id "home"}
   [:div {:style "position: relative; max-width: none; padding: 0;"}
    [:div {:style (str "height: 680px;"
                       "border-bottom: 1px solid var(--mtz-ink);"
                       "background-image: url('/images/DSC01305.jpg');"
                       "background-size: cover;"
                       "background-position: center top;")}]
    [:div {:style "position: absolute; inset: 0; background: linear-gradient(180deg, rgba(0,0,0,0.10) 0%, rgba(0,0,0,0.10) 35%, rgba(0,0,0,0.65) 100%); pointer-events: none;"}]
    [:div {:style "position: absolute; left: 0; right: 0; bottom: 0; padding: 0 32px 56px; max-width: var(--mtz-page-w); margin: 0 auto;"}
     [:div {:style "max-width: 760px; color: #fff;"}
      [:p {:class "mtz-kicker"
           :style "color: rgba(255,255,255,0.85); margin-bottom: 18px;"}
       "Welcome to Mt Zion UCC · est. 1755"]
      [:h1 {:class "mtz-h1"
            :style "color: #fff; margin: 0; font-size: 72px; line-height: 1.02;"}
       "A family-oriented church in China Grove — "
       [:em {:style "font-style: italic; color: var(--mtz-mint-accent);"} "come as you are."]]
      [:p {:style "margin: 24px 0 0; font-family: var(--mtz-serif-body); font-size: 20px; color: rgba(255,255,255,0.92); max-width: 620px; line-height: 1.45;"}
       "Sunday Service at 10:30"]
      [:div {:style "display: flex; gap: 12px; margin-top: 28px; flex-wrap: wrap;"}
       [:a {:class "mtz-btn mtz-btn--primary" :href "/contact"} "Get in Touch"]
       [:a {:class "mtz-btn"
            :href  "/about"
            :style "background: transparent; color: #fff; border: 1px solid #fff;"}
        "Meet Our Community"]]]]]
   [:div {:class "mtz-section" :style "padding-top: 56px; padding-bottom: 48px;"}
    [:p {:class "mtz-lede" :style "max-width: 760px; font-size: 26px;"}
     "If you're looking for a place to call home, join us one Sunday morning or "
     "at one of our community events. Maybe you'll find that Mount Zion is the "
     "family you've been looking for."]]
   [:hr {:class "mtz-rule"}]])

(defn- exec [ctx honey]
  (mapv normalize (biff.sqlite/execute ctx honey)))

(defn- cf-img-url [ctx image-id variant]
  (when (seq image-id)
    (str "https://imagedelivery.net/" (:cf/images-hash ctx) "/" image-id "/" variant)))

(defn- format-event-date [epoch]
  (when epoch
    (-> (java.time.Instant/ofEpochSecond epoch)
        (java.time.LocalDate/ofInstant java.time.ZoneOffset/UTC)
        (.format (java.time.format.DateTimeFormatter/ofPattern "EEEE, MMMM d")))))

(defn home [ctx]
  (let [featured-events (exec ctx {:select   :*
                                   :from     :event
                                   :where    [:and [:= :featured 1] [:= :published 1]]
                                   :order-by [[:start_at :asc]]
                                   :limit    2})
        latest-posts    (exec ctx {:select   :*
                                   :from     :post
                                   :where    [:is-not :published_at nil]
                                   :order-by [[:published_at :desc]]
                                   :limit    3})]
    (base/page "Mount Zion UCC — Welcome"
               (list
                (photo-hero)
                (home-sections/home-page
                 {:features (map (fn [ev]
                                   {:body      (:description ev)
                                    :image-url (cf-img-url ctx (:image_id ev) "public")
                                    :title     (:title ev)
                                    :subtitle  (format-event-date (:start_at ev))
                                    :cta-url   "/events"
                                    :cta-label "See all events"})
                                 featured-events)
                  :news     (when (seq latest-posts)
                              (map (fn [p]
                                     {:tag     "News"
                                      :title   (:title p)
                                      :date    (or (format-month-year (:published_at p)) "")
                                      :excerpt (or (:excerpt p) "")})
                                   latest-posts))})))))

(def module
  {:biff.ring/routes
   ["/" {:get home
         :name ::home}]})
