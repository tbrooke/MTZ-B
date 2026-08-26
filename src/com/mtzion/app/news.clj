(ns com.mtzion.app.news
  (:require [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.model.normalize :as norm]
            [com.mtzion.ui.base :as base]
            [lambdaisland.hiccup :as hiccup]))

(defn- format-month-year [epoch]
  (when epoch
    (-> (java.time.Instant/ofEpochSecond epoch)
        (java.time.LocalDate/ofInstant java.time.ZoneOffset/UTC)
        (.format (java.time.format.DateTimeFormatter/ofPattern "MMMM yyyy")))))

(defn- cf-img-url
  "The account hash segment is required — without it the URL 404s. This is why
  post images silently never appeared."
  [ctx image-id]
  (when (seq image-id)
    (str "https://imagedelivery.net/" (:cf/images-hash ctx) "/" image-id "/public")))

(defn- post-card [ctx p]
  [:article {:class "mtz-card"}
   ;; No image, no box. A grey "image · 800×500" placeholder reads as an
   ;; unfinished site, which is worse than a card that is simply text.
   (when-let [img (cf-img-url ctx (:image_id p))]
     [:img {:src   img
            :alt   (:title p)
            :style "width: 100%; aspect-ratio: 16/10; object-fit: cover;"}])
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
  ;; snake-keys is required: execute returns :post/published-at, but post-card
  ;; reads :published_at. Without it every card rendered blank.
  (let [posts (norm/snake-keys-all
               (biff.sqlite/execute ctx {:select   :*
                                         :from     :post
                                         :where    [:= :status "published"]
                                         :order-by [[:published_at :desc]]}))]
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
          (map #(post-card ctx %) posts)]
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
  (base/page ctx "News — Mount Zion UCC" (page-content ctx)))

;; ---------------------------------------------------------------------------
;; /news/:slug — a single post
;; ---------------------------------------------------------------------------

(defn- post-detail-content [p]
  (list
   [:section {:class "mtz-section"}
    [:p {:class "mtz-kicker"}
     (str "News · " (or (format-month-year (:published_at p)) ""))]
    [:h1 {:class "mtz-h1" :style "max-width: 760px;"} (:title p)]
    ;; The byline is free text, deliberately not tied to a login: Pastor Jim gets
    ;; the credit whether or not he ever signs in to write it himself.
    (when (seq (:author p))
      [:p {:class "mtz-byline"} (str "by " (:author p))])
    (when (seq (:excerpt p))
      [:p {:class "mtz-lede" :style "max-width: 640px;"} (:excerpt p)])
    [:hr {:class "mtz-rule"}]]
   [:section {:class "mtz-section"}
    [:div {:class "mtz-prose" :style "max-width: 760px;"}
     [::hiccup/unsafe-html (:body p)]]
    [:a {:class "mtz-arrow-link" :href "/news" :style "margin-top: 32px; display: inline-flex;"}
     "← All news"]]))

(defn post-detail
  "A post is readable only once it has a published_at date — that is what draft
  means for this table, so the same filter as the listing applies here."
  [{:keys [path-params] :as ctx}]
  (let [p (norm/snake-keys
           (first (biff.sqlite/execute ctx {:select :* :from :post
                                            :where  [:and
                                                     [:= :slug (:slug path-params)]
                                                     [:= :status "published"]]})))]
    (if p
      (base/page ctx (str (:title p) " — Mount Zion UCC") (post-detail-content p))
      (-> (base/page ctx "Not Found — Mount Zion UCC"
                     [:section {:class "mtz-section"}
                      [:h1 {:class "mtz-h1"} "Post not found"]
                      [:p {:class "mtz-lede" :style "max-width: 560px;"}
                       "That post doesn't exist or hasn't been published yet."]
                      [:a {:class "mtz-btn mtz-btn--primary" :href "/news"} "All news"]])
          (assoc :status 404)))))

(def module
  {:biff.ring/routes
   [["/news" {:get news :name ::news}]
    ;; Must be a real route: /news/<slug> is two segments with "news" in
    ;; model.nav/top-level-slugs, so without this the CMS 404-fallback would
    ;; look the slug up in the page table and always miss.
    ["/news/:slug" {:get post-detail :name ::post-detail}]]})
