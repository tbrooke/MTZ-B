(ns com.mtzion.app.worship
  (:require [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.app.home-sections :as home-sections]
            [com.mtzion.ui.base :as base]
            [lambdaisland.hiccup :as hiccup]))

(defn- format-date [epoch]
  (when epoch
    (-> (java.time.Instant/ofEpochSecond epoch)
        (java.time.LocalDate/ofInstant java.time.ZoneOffset/UTC)
        (.format (java.time.format.DateTimeFormatter/ofPattern "MMMM d, yyyy")))))

(defn- sermon-card [s]
  [:article {:class "mtz-card"}
   [:div {:class "mtz-img"
          :style "aspect-ratio: 16/9; border-radius: 0; border-left: 0; border-right: 0; border-top: 0; background: var(--mtz-stone);"}
    (if (:video_id s)
      [:a {:href   (str "/sermons/" (:id s))
           :style  "position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;"}
       [:img {:src   (str "https://videodelivery.net/" (:video_id s) "/thumbnails/thumbnail.jpg")
              :alt   (:title s)
              :style "position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover;"}]
       [:div {:style "position: relative; width: 48px; height: 48px; border-radius: 50%; background: rgba(0,0,0,0.55); display: flex; align-items: center; justify-content: center; color: #fff; font-size: 18px;"}
        "▶"]]
      [:div {:style "position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;"}
       [:div {:style "width: 48px; height: 48px; border-radius: 50%; background: rgba(0,0,0,0.25); display: flex; align-items: center; justify-content: center; color: #fff; font-size: 18px;"}
        "▶"]])]
   [:div {:class "mtz-card-body"}
    [:p {:class "mtz-card-meta"}
     (str (or (format-date (:sermon_date s)) "") (when (:scripture s) (str " · " (:scripture s))))]
    [:h3 {:class "mtz-h3" :style "font-size: 20px; margin-bottom: 6px;"} (:title s)]
    (when (seq (:description s))
      [:p {:style "color: var(--mtz-ink-soft); font-size: 14px; margin: 0;"}
       (:description s)])]])

(defn- page-content [ctx]
  (let [sermons (biff.sqlite/execute ctx {:select   :*
                                          :from     :sermon
                                          :where    [:= :published 1]
                                          :order-by [[:sermon_date :desc]]})
        latest  (first sermons)
        recent  (take 5 (rest sermons))]
    (list
     [:section {:class "mtz-section"}
      [:p {:class "mtz-kicker"} "Sunday Worship · 10:30 AM"]
      [:h1 {:class "mtz-h1" :style "max-width: 760px;"} "Sunday Worship"]
      [:p {:class "mtz-lede" :style "max-width: 640px;"}
       "Join us each Sunday for traditional worship with choir, organ, and sermon. "
       "All are welcome at every service."]
      [:hr {:class "mtz-rule"}]]

     (home-sections/this-sunday-section
      (when latest
        {:scripture  (:scripture latest)
         :sermon-title (:title latest)}))

     (when latest
       [:section {:id (str "sermon-" (:id latest)) :class "mtz-section--cream"}
        [:div {:class "mtz-section-inner"}
         [:p {:class "mtz-kicker" :style "margin-bottom: 12px;"}
          (str "Last Sunday · " (or (format-date (:sermon_date latest)) ""))]
         (when (:video_id latest)
           [:div {:style "margin-bottom: 28px;"}
            [::hiccup/unsafe-html
             (str "<iframe src=\"https://iframe.cloudflarestream.com/" (:video_id latest)
                  "\" style=\"width:100%;max-width:760px;aspect-ratio:16/9;border:none;border-radius:6px;\""
                  " allow=\"accelerometer; gyroscope; autoplay; encrypted-media; picture-in-picture\""
                  " allowfullscreen></iframe>")]])
         [:h2 {:class "mtz-h2" :style "margin-bottom: 8px;"} (:title latest)]
         (when (:scripture latest)
           [:p {:class "mtz-mono" :style "font-size: 13px; letter-spacing: 0.10em; color: var(--mtz-ink-soft); margin: 0 0 16px;"}
            (clojure.string/upper-case (:scripture latest))])
         (when (seq (:description latest))
           [:p {:class "mtz-prose" :style "color: var(--mtz-ink-soft); max-width: 640px;"}
            (:description latest)])]])

     (when (seq recent)
       [:section {:id "archive" :class "mtz-section"}
        [:div {:class "mtz-section-inner"}
         [:div {:class "mtz-row"
                :style "justify-content: space-between; align-items: baseline; margin-bottom: 28px;"}
          [:h2 {:class "mtz-h2" :style "margin: 0;"} "Recent Sermons"]
          [:a {:class "mtz-arrow-link" :href "/sermons"} "Full archive →"]]
         [:div {:class "mtz-grid mtz-grid--3"}
          (map sermon-card recent)]]])

     (when (seq sermons)
       [:section {:class "mtz-section--tint"}
        [:div {:class "mtz-section-inner"}
         [:h2 {:class "mtz-h2" :style "margin-bottom: 20px;"} "Browse Services"]
         [:div {:style "max-width: 480px;"}
          [:select {:style "width:100%; padding:12px 16px; font-size:15px; border:1px solid var(--mtz-rule); border-radius:6px; background:var(--mtz-bg); cursor:pointer; color:var(--mtz-ink);"
                    :onchange "if(this.value) window.location.href=this.value"}
           [:option {:value ""} "Select a Sunday…"]
           (map (fn [s]
                  [:option {:value (str "/sermons/" (:id s))}
                   (str (or (format-date (:sermon_date s)) "Undated")
                        (when (seq (:title s)) (str " — " (:title s))))])
                sermons)]
          [:a {:class "mtz-arrow-link" :href "/sermons" :style "margin-top: 12px; display: inline-flex;"}
           "Full archive →"]]]])

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
        [:a {:class "mtz-btn mtz-btn--ghost"   :href "/about"}   "Learn More About Us"]]]])))

(defn worship [ctx]
  (base/page "Sunday Worship — Mount Zion UCC" (page-content ctx)))

(def module
  {:biff.ring/routes
   [["/worship" {:get worship :name ::worship}]]})
