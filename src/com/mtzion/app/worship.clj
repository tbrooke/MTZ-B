(ns com.mtzion.app.worship
  (:require [clojure.string :as str]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.model.normalize :as norm]
            [com.mtzion.ui.base :as base]
            [lambdaisland.hiccup :as hiccup]))

(def ^:private normalize norm/snake-keys-all)

(defn- format-date [epoch]
  (when epoch
    (-> (java.time.Instant/ofEpochSecond epoch)
        (java.time.LocalDate/ofInstant java.time.ZoneOffset/UTC)
        (.format (java.time.format.DateTimeFormatter/ofPattern "MMMM d, yyyy")))))

(defn- scripture-line [s]
  (let [cw     (not-empty (:scripture_cw s))
        gospel (not-empty (:scripture_gospel s))]
    (cond
      (and cw gospel) (str cw " · " gospel)
      cw              cw
      gospel          gospel
      :else           nil)))

;; ---------------------------------------------------------------------------
;; /worship — main worship page
;; ---------------------------------------------------------------------------

(defn- page-content [ctx]
  (let [sermons (normalize (biff.sqlite/execute ctx {:select   :*
                                                     :from     :sermon
                                                     :where    [:= :status "published"]
                                                     :order-by [[:sermon_date :desc]]}))
        latest  (first sermons)
        ;; current series = series with the most recent future (or latest) sermon
        current-series (some :series sermons)]
    (list
     [:section {:class "mtz-section"}
      [:p {:class "mtz-kicker"} "Sunday Worship · 10:30 AM"]
      [:h1 {:class "mtz-h1" :style "max-width:760px;"} "Sunday Worship"]
      [:p {:class "mtz-lede" :style "max-width:640px;"}
       "Join us each Sunday for traditional worship with choir, organ, and sermon. "
       "All are welcome at every service."]
      [:div {:class "mtz-row" :style "gap:16px; margin-top:28px;"}
       [:a {:class "mtz-btn mtz-btn--primary" :href "/worship/sundays"} "Sunday Archive"]
       (when current-series
         [:a {:class "mtz-btn mtz-btn--ghost" :href "/worship/theme"} "Current Theme →"])]]

     (when latest
       [:section {:class "mtz-section--cream"}
        [:div {:class "mtz-section-inner"}
         [:p {:class "mtz-kicker" :style "margin-bottom:12px;"}
          (str "Most Recent · " (or (format-date (:sermon_date latest)) ""))]
         (when (:video_id latest)
           [:div {:style "margin-bottom:28px;"}
            [::hiccup/unsafe-html
             (str "<iframe src=\"https://iframe.cloudflarestream.com/" (:video_id latest)
                  "\" style=\"width:100%;max-width:760px;aspect-ratio:16/9;border:none;border-radius:6px;\""
                  " allow=\"accelerometer; gyroscope; autoplay; encrypted-media; picture-in-picture\""
                  " allowfullscreen></iframe>")]])
         [:h2 {:class "mtz-h2" :style "margin-bottom:8px;"} (:title latest)]
         (when-let [sc (scripture-line latest)]
           [:p {:class "mtz-mono" :style "font-size:13px; letter-spacing:0.08em; color:var(--mtz-ink-soft); margin:0 0 16px;"}
            (str/upper-case sc)])
         (when (seq (:description latest))
           [:p {:class "mtz-prose" :style "color:var(--mtz-ink-soft); max-width:640px;"}
            (:description latest)])
         [:a {:class "mtz-arrow-link" :href (str "/worship/sundays/" (:id latest))
              :style "margin-top:12px; display:inline-flex;"}
          "Full service details →"]]])

     [:section {:class "mtz-section"}
      [:div {:class "mtz-grid mtz-grid--2" :style "gap:64px; align-items:center;"}
       [:div
        [:p {:class "mtz-kicker"} "Music Ministry"]
        [:h2 {:class "mtz-h2"} "The sound of Mt. Zion."]
        [:p {:class "mtz-prose" :style "color:var(--mtz-ink-soft);"}
         "Our sanctuary choir rehearses every Wednesday at 7:00 PM and sings at the "
         "10:30 AM service. We also feature seasonal ensembles, handbell choir, and "
         "special music throughout the year. New voices are always welcome — no audition required."]
        [:a {:class "mtz-arrow-link" :href "/activities" :style "margin-top:16px; display:inline-flex;"}
         "Choir rehearsal schedule →"]]
       [:div {:class "mtz-img" :style "aspect-ratio:4/3; min-height:0;"}
        [:span {:class "mtz-img-label"} "choir · sanctuary"]]]]

     [:section {:class "mtz-section--tint"}
      [:div {:class "mtz-section-inner" :style "text-align:center;"}
       [:h2 {:class "mtz-h2" :style "margin-bottom:16px;"} "Plan Your Visit"]
       [:p {:class "mtz-lede" :style "max-width:520px; margin:0 auto 32px;"}
        "We meet at 1415 S Main St, China Grove, NC 28023. "
        "Sunday worship begins at 10:30 AM."]
       [:div {:class "mtz-row" :style "justify-content:center; gap:12px; flex-wrap:wrap;"}
        [:a {:class "mtz-btn mtz-btn--primary" :href "/contact"} "Get Directions"]
        [:a {:class "mtz-btn mtz-btn--ghost"   :href "/about"}   "Learn More About Us"]]]])))

(defn worship [ctx]
  (base/page ctx "Sunday Worship — Mount Zion UCC" (page-content ctx)))

;; ---------------------------------------------------------------------------
;; /worship/theme — current sermon series
;; ---------------------------------------------------------------------------

(defn- series-sermon-row [s today-epoch]
  (let [future? (> (or (:sermon_date s) 0) today-epoch)]
    [:div {:style (str "display:flex; gap:20px; align-items:flex-start; padding:20px 0;"
                       " border-bottom:1px solid var(--mtz-rule);"
                       (when future? " opacity:0.7;"))}
     [:div {:style "flex:0 0 110px; text-align:right;"}
      [:p {:class "mtz-card-meta" :style "margin:0; font-variant-numeric:tabular-nums;"}
       (or (format-date (:sermon_date s)) "—")]
      (when future?
        [:span {:style "font-size:11px; color:var(--mtz-ink-soft); letter-spacing:0.06em; text-transform:uppercase;"}
         "Upcoming"])]
     [:div {:style "flex:1; min-width:0;"}
      [:p {:class "mtz-h3" :style "font-size:17px; margin:0 0 4px; line-height:1.35;"}
       (if (and (:id s) (not future?))
         [:a {:href (str "/worship/sundays/" (:id s)) :style "color:inherit;"} (:title s)]
         (:title s))]
      (when-let [sc (scripture-line s)]
        [:p {:class "mtz-card-meta" :style "margin:0; font-size:12px;"}
         sc])]
     (when (and (:video_id s) (not future?))
       [:a {:class "mtz-arrow-link" :href (str "/worship/sundays/" (:id s))
            :style "font-size:12px; flex-shrink:0; align-self:center;"}
        "Watch"])]))

(defn worship-theme [ctx]
  (let [sermons      (normalize (biff.sqlite/execute ctx {:select   :*
                                                          :from     :sermon
                                                          :where    [:= :status "published"]
                                                          :order-by [[:sermon_date :desc]]}))
        ;; find the current (most recently active) series
        series-slug  (some :series sermons)
        series-rows  (when series-slug
                       (normalize (biff.sqlite/execute ctx {:select   :*
                                                            :from     :sermon
                                                            :where    [:and
                                                                       [:= :status "published"]
                                                                       [:= :series series-slug]]
                                                            :order-by [[:sermon_date :asc]]})))
        ;; overview feature for this page
        features     (normalize (biff.sqlite/execute ctx {:select :*
                                                          :from   :feature
                                                          :where  [:and
                                                                   [:= :page_slug "current-theme"]
                                                                   [:= :status "published"]]
                                                          :order-by [[:sort_order :asc]]}))
        overview     (first features)
        today-epoch  (.getEpochSecond (java.time.Instant/now))]
    (base/page ctx "Current Theme — Mount Zion UCC"
               (list
                [:section {:class "mtz-section"}
                 [:a {:class "mtz-arrow-link"
                      :href  "/worship"
                      :style "font-size:11px; margin-bottom:24px; display:inline-flex;"}
                  "← Worship"]
                 [:p {:class "mtz-kicker"} "Current Theme"]
                 [:h1 {:class "mtz-h1" :style "max-width:760px;"}
                  (or (:title overview) "Current Sermon Series")]
                 (when (seq (:subtitle overview))
                   [:p {:class "mtz-lede" :style "max-width:640px; margin-bottom:0;"}
                    (:subtitle overview)])]

                (when (seq (:body overview))
                  [:section {:class "mtz-section--cream"}
                   [:div {:class "mtz-section-inner"}
                    [:div {:class "mtz-prose" :style "max-width:720px;"}
                     [::hiccup/unsafe-html (:body overview)]]
                    (when (and (seq (:cta_label overview)) (seq (:cta_url overview)))
                      [:a {:class "mtz-arrow-link" :href (:cta_url overview)
                           :style "margin-top:20px; display:inline-flex;"}
                       (:cta_label overview)])]])

                (when (seq series-rows)
                  [:section {:class "mtz-section"}
                   [:div {:class "mtz-section-inner"}
                    [:h2 {:class "mtz-h2" :style "margin-bottom:4px;"}
                     (str (count series-rows) " Sermons")]
                    [:p {:class "mtz-card-meta" :style "margin-bottom:24px;"}
                     (str (format-date (:sermon_date (first series-rows)))
                          " – "
                          (format-date (:sermon_date (last series-rows))))]
                    [:div
                     (map #(series-sermon-row % today-epoch) series-rows)]]])

                (when-not series-slug
                  [:section {:class "mtz-section"}
                   [:p {:class "mtz-mute"} "No current series. Check back soon."]])))))

;; ---------------------------------------------------------------------------
;; Module
;; ---------------------------------------------------------------------------

(def module
  {:biff.ring/routes
   [["/worship"
     ["" {:get worship :name ::worship}]
     ["/theme" {:get worship-theme :name ::worship-theme}]]]})
