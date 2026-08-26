(ns com.mtzion.app.landing
  (:require [clojure.string :as str]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.app.home-sections :as home-sections]
            [com.mtzion.model.church :as church]
            [com.mtzion.model.event :as event]
            [com.mtzion.model.normalize :as norm]
            [com.mtzion.ui.base :as base]
            [lambdaisland.hiccup :as hiccup]))

(def ^:private normalize norm/snake-keys)

(defn- format-month-year [epoch]
  (when epoch
    (-> (java.time.Instant/ofEpochSecond epoch)
        (java.time.LocalDate/ofInstant java.time.ZoneOffset/UTC)
        (.format (java.time.format.DateTimeFormatter/ofPattern "MMMM yyyy")))))

(def ^:private hero-defaults
  "Used until a `home-hero` feature exists, and as a per-field fallback if one
  exists but leaves a field blank. Keeping real copy here means the home page
  never renders empty, but the CMS row wins whenever it has a value."
  {:kicker    (str "Welcome to Mt Zion UCC · est. " church/founded-year)
   :headline  "A family-oriented church in China Grove — come as you are."
   :subtext   "Sunday Service at 10:30"
   :image-url "/images/DSC01305.jpg"
   :cta-label "Get in Touch"
   :cta-url   "/contact"
   :lede      (str "If you're looking for a place to call home, join us one Sunday morning or "
                   "at one of our community events. Maybe you'll find that Mount Zion is the "
                   "family you've been looking for.")})

(defn- headline-parts
  "Splits on an em dash so the tail renders emphasised, matching the design.
  No dash simply means no emphasis."
  [s]
  (let [[a b] (str/split (str s) #"\s+—\s+" 2)]
    [(if b (str a " — ") a) b]))

(defn- photo-hero [hero]
  (let [h                   (merge hero-defaults (into {} (remove (comp nil? val)) hero))
        [head-main head-em] (headline-parts (:headline h))]
    [:section {:id "home"}
     [:div {:style "position: relative; max-width: none; padding: 0;"}
      [:div {:style (str "height: 680px;"
                         "border-bottom: 1px solid var(--mtz-ink);"
                         "background-image: url('" (:image-url h) "');"
                         "background-size: cover;"
                         "background-position: center top;")}]
      [:div {:style "position: absolute; inset: 0; background: linear-gradient(180deg, rgba(0,0,0,0.10) 0%, rgba(0,0,0,0.10) 35%, rgba(0,0,0,0.65) 100%); pointer-events: none;"}]
      [:div {:style "position: absolute; left: 0; right: 0; bottom: 0; padding: 0 32px 56px; max-width: var(--mtz-page-w); margin: 0 auto;"}
       [:div {:style "max-width: 760px; color: #fff;"}
        [:p {:class "mtz-kicker"
             :style "color: rgba(255,255,255,0.85); margin-bottom: 18px;"}
         (:kicker h)]
        [:h1 {:class "mtz-h1"
              :style "color: #fff; margin: 0; font-size: 72px; line-height: 1.02;"}
         head-main
         (when head-em
           [:em {:style "font-style: italic; color: var(--mtz-mint-accent);"} head-em])]
        (when (seq (:subtext h))
          [:p {:style "margin: 24px 0 0; font-family: var(--mtz-serif-body); font-size: 20px; color: rgba(255,255,255,0.92); max-width: 620px; line-height: 1.45;"}
           (:subtext h)])
        [:div {:style "display: flex; gap: 12px; margin-top: 28px; flex-wrap: wrap;"}
         [:a {:class "mtz-btn mtz-btn--primary" :href (:cta-url h)} (:cta-label h)]
         [:a {:class "mtz-btn"
              :href  "/about"
              :style "background: transparent; color: #fff; border: 1px solid #fff;"}
          "Meet Our Community"]]]]]
     [:div {:class "mtz-section" :style "padding-top: 56px; padding-bottom: 48px;"}
      [:div {:class "mtz-lede" :style "max-width: 760px; font-size: 26px;"}
       (if (seq (:lede-html h))
         [::hiccup/unsafe-html (:lede-html h)]
         (:lede h))]]
     [:hr {:class "mtz-rule"}]]))

(defn- exec [ctx honey]
  (mapv normalize (biff.sqlite/execute ctx honey)))

(defn- cf-img-url [ctx image-id variant]
  (when (seq image-id)
    (str "https://imagedelivery.net/" (:cf/images-hash ctx) "/" image-id "/" variant)))

;; start_at is a church wall-clock instant — render it in Eastern. (The
;; format-month-year above reads published_at, which is a UTC-midnight date-only
;; column, so UTC is correct there.)
(defn- format-event-date [epoch]
  (when epoch
    (-> (java.time.Instant/ofEpochSecond epoch)
        (java.time.LocalDate/ofInstant norm/eastern)
        (.format (java.time.format.DateTimeFormatter/ofPattern "EEEE, MMMM d")))))

(defn home [ctx]
  (let [n-ep             (event/now-epoch)
        ;; Only events that still have an occurrence ahead of us. Without this
        ;; filter the two oldest featured events pin themselves to the home page
        ;; forever — and are invisible in the admin list, which hides past events.
        featured-src     (exec ctx {:select :*
                                    :from   :event
                                    :where  [:and [:= :featured 1] [:= :status "published"]
                                             (event/upcoming-where n-ep)]})
        featured-events  (take 2 (event/next-occurrences featured-src n-ep))
        latest-posts     (exec ctx {:select   :*
                                    :from     :post
                                    :where    [:= :status "published"]
                                    :order-by [[:published_at :desc]]
                                    :limit    3})
        worship-feature  (first (exec ctx {:select   :*
                                           :from     :feature
                                           :where    [:and
                                                      [:= :page_slug "home-worship"]
                                                      [:= :status "published"]]
                                           :limit    1}))
        activity-cards   (exec ctx {:select   :*
                                    :from     :feature
                                    :where    [:and
                                               [:= :page_slug "home-activities"]
                                               [:= :status "published"]]
                                    :order-by [[:sort_order :asc] [:title :asc]]})
        hero-feature     (first (exec ctx {:select :*
                                           :from   :feature
                                           :where  [:and
                                                    [:= :page_slug "home-hero"]
                                                    [:= :status "published"]]
                                           :limit  1}))
        ;; feature column -> hero slot. Blank values fall through to
        ;; hero-defaults, so a half-filled row still renders a complete hero.
        hero             (when hero-feature
                           {:headline  (not-empty (:title hero-feature))
                            :subtext   (not-empty (:subtitle hero-feature))
                            :lede-html (not-empty (:body hero-feature))
                            :image-url (cf-img-url ctx (:image_id hero-feature) "public")
                            :cta-label (not-empty (:cta_label hero-feature))
                            :cta-url   (not-empty (:cta_url hero-feature))})]
    (base/page ctx "Mount Zion UCC — Welcome"
               (list
                (photo-hero hero)
                (home-sections/home-page
                 {:features (map (fn [ev]
                                   {:body      (:description ev)
                                    :image-url (cf-img-url ctx (:image_id ev) "public")
                                    :title     (:title ev)
                                    :subtitle  (format-event-date (:start_at ev))
                                    :cta-url   "/events"
                                    :cta-label "See all events"})
                                 featured-events)
                  :worship  (when worship-feature
                              {:image-url (cf-img-url ctx (:image_id worship-feature) "public")
                               :body      (:body worship-feature)
                               :subtitle  (:subtitle worship-feature)
                               :cta-label (:cta_label worship-feature)
                               :cta-url   (:cta_url worship-feature)})
                  :activities {:cards (map (fn [f]
                                             {:name      (:title f)
                                              :image-url (cf-img-url ctx (:image_id f) "public")})
                                           activity-cards)
                               ;; the first card's subtitle doubles as the section blurb,
                               ;; so the copy is editable in the admin too
                               :blurb (some (comp not-empty :subtitle) activity-cards)}
                  :news     (when (seq latest-posts)
                              (map (fn [p]
                                     {:tag       "News"
                                      :title     (:title p)
                                      :date      (or (format-month-year (:published_at p)) "")
                                      :excerpt   (or (:excerpt p) "")
                                      :image-url (cf-img-url ctx (:image_id p) "public")
                                      :url       (when (seq (:slug p)) (str "/news/" (:slug p)))})
                                   latest-posts))})))))

(def module
  {:biff.ring/routes
   ["/" {:get home
         :name ::home}]})
