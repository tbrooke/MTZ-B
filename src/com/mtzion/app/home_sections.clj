(ns com.mtzion.app.home-sections
  "Homepage section components — all static defaults, no CMS dependency."
  (:require [clojure.string :as str]))

;; ---------------------------------------------------------------------------
;; THIS SUNDAY
;; ---------------------------------------------------------------------------

(def ^:private default-this-sunday
  {:date         "Sunday"
   :sunday-title "Sunday Worship"
   :scripture    "Join us this week"
   :sermon-title nil
   :notes        "All are welcome"
   :bulletin-url nil
   :facebook-url nil})

(def ^:private default-services
  [["8:30 AM"  "Early Worship"       "Sanctuary · contemplative"]
   ["9:30 AM"  "Sunday School"       "All ages"]
   ["10:30 AM" "Traditional Worship" "Sanctuary · choir & organ"]
   ["6:00 PM"  "Youth Group"         "Fellowship Hall"]])

(defn this-sunday-section
  ([] (this-sunday-section nil))
  ([data]
   (let [d (merge default-this-sunday data)]
     [:section {:id "worship" :class "mtz-section"}
      [:div {:class "mtz-grid mtz-grid--2" :style "gap: 64px; align-items: start;"}
       [:div
        [:p {:class "mtz-kicker"} (str "This Sunday · " (:date d))]
        [:h2 {:class "mtz-h2"} (:sunday-title d)]
        [:div {:class "mtz-stack" :style "gap: 10px; margin-top: 4px;"}
         [:p {:style "margin: 0; font-family: var(--mtz-serif-display); font-size: 20px; color: var(--mtz-ink);"}
          [:span {:class "mtz-mono"
                  :style "font-size: 12px; letter-spacing: 0.14em; color: var(--mtz-mint-dark); margin-right: 10px;"}
           "SCRIPTURE"]
          (:scripture d)]
         (when (:sermon-title d)
           [:p {:style "margin: 0; font-family: var(--mtz-serif-display); font-size: 20px; color: var(--mtz-ink);"}
            [:span {:class "mtz-mono"
                    :style "font-size: 12px; letter-spacing: 0.14em; color: var(--mtz-mint-dark); margin-right: 10px;"}
             "SERMON"]
            [:em (:sermon-title d)]])
         [:p {:style "margin: 0; font-family: var(--mtz-serif-display); font-size: 20px; color: var(--mtz-ink);"}
          [:span {:class "mtz-mono"
                  :style "font-size: 12px; letter-spacing: 0.14em; color: var(--mtz-mint-dark); margin-right: 10px;"}
           "NOTE"]
          (:notes d)]]
        [:div {:class "mtz-row" :style "gap: 28px; margin-top: 24px; flex-wrap: wrap;"}
         (when (:bulletin-url d)
           [:a {:class "mtz-arrow-link" :href (:bulletin-url d)} "View this week's bulletin →"])
         (when (:facebook-url d)
           [:a {:class "mtz-arrow-link" :href (:facebook-url d)} "Watch live on Facebook →"])]]
       [:div {:class "mtz-stack" :style "gap: 0; border-top: 1px solid var(--mtz-ink);"}
        (for [[time title sub] default-services]
          [:div {:class "mtz-row"
                 :style "padding: 18px 0; border-bottom: 1px solid var(--mtz-rule); gap: 24px; align-items: baseline;"}
           [:span {:class "mtz-mono"
                   :style "width: 96px; color: var(--mtz-mint-dark); font-size: 14px;"}
            time]
           [:div {:style "flex: 1;"}
            [:div {:style "font-family: var(--mtz-serif-display); font-size: 19px; font-weight: 600;"} title]
            [:div {:class "mtz-mute" :style "font-size: 14px;"} sub]]])]]])))

;; ---------------------------------------------------------------------------
;; LAST SUNDAY
;; ---------------------------------------------------------------------------

(def ^:private default-last-sunday
  {:date        "Last Sunday"
   :title       "Last Sunday's Sermon"
   :scripture   nil
   :excerpt     "Join us each week for inspiring worship and community."
   :archive-url "/worship"})

(defn last-sunday-section
  ([] (last-sunday-section nil))
  ([data]
   (let [d (merge default-last-sunday data)]
     [:section {:id "last-sunday" :class "mtz-section"
                :style "padding-top: 24px; padding-bottom: 72px;"}
      [:div {:class "mtz-grid mtz-grid--2" :style "gap: 56px; align-items: center;"}
       [:div {:class "mtz-img"
              :style "aspect-ratio: 16/9; min-height: 0; position: relative; background: linear-gradient(135deg, var(--mtz-cream), var(--mtz-stone));"}
        (if (:video-url d)
          [:a {:href (:video-url d)
               :style "position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;"}
           [:div {:style "width: 64px; height: 64px; border-radius: 50%; background: rgba(0,0,0,0.55); display: flex; align-items: center; justify-content: center; color: #fff; font-size: 22px;"}
            "▶"]]
          [:div {:style "position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;"}
           [:div {:style "width: 64px; height: 64px; border-radius: 50%; background: rgba(0,0,0,0.35); display: flex; align-items: center; justify-content: center; color: #fff; font-size: 22px;"}
            "▶"]])
        [:span {:class "mtz-img-label"
                :style "position: absolute; bottom: 12px; left: 12px;"}
         "Sunday service · recording"]]
       [:div
        [:p {:class "mtz-kicker" :style "color: var(--mtz-ink-mute);"} (:date d)]
        [:h3 {:class "mtz-h3" :style "font-size: 28px; margin-bottom: 10px;"} (:title d)]
        (when (:scripture d)
          [:p {:class "mtz-mono"
               :style "font-size: 13px; letter-spacing: 0.10em; color: var(--mtz-ink-soft); margin: 0 0 12px;"}
           (str/upper-case (:scripture d))])
        [:p {:style "color: var(--mtz-ink-soft); margin: 0; max-width: 460px;"} (:excerpt d)]
        [:a {:class "mtz-arrow-link"
             :href  (:archive-url d)
             :style "margin-top: 20px; display: inline-flex;"}
         "View sermon archive →"]]]])))

;; ---------------------------------------------------------------------------
;; FEATURED EVENT
;; ---------------------------------------------------------------------------

(def ^:private default-featured-event
  {:tags      ["Fellowship" "All ages"]
   :title     "Upcoming Event"
   :date-line "See calendar for details"
   :excerpt   "Join us for fellowship and community."
   :rsvp-url  nil
   :cal-url   nil})

(defn featured-event-section
  ([] (featured-event-section nil))
  ([data]
   (let [d (merge default-featured-event data)]
     [:section {:id "events" :class "mtz-section--tint"}
      [:div {:class "mtz-section-inner"}
       [:div {:class "mtz-row"
              :style "justify-content: space-between; align-items: baseline; margin-bottom: 28px;"}
        [:p {:class "mtz-kicker" :style "margin: 0;"} "Featured Event"]
        [:a {:class "mtz-arrow-link" :href "/events"} "All events →"]]
       [:div {:class "mtz-grid mtz-grid--2" :style "gap: 48px; align-items: stretch;"}
        [:div {:class "mtz-img" :style "min-height: 360px; border-radius: 8px;"}
         [:span {:class "mtz-img-label"} "event poster"]]
        [:div {:class "mtz-stack" :style "justify-content: center; gap: 16px;"}
         [:div {:class "mtz-row" :style "gap: 8px; flex-wrap: wrap;"}
          (for [tag (:tags d)]
            [:span {:class "mtz-tag"}
             [:span {:class "mtz-dot"}]
             tag])]
         [:h3 {:class "mtz-h2" :style "font-size: 38px; margin: 0;"} (:title d)]
         [:p {:class "mtz-mute mtz-mono" :style "font-size: 13px; letter-spacing: 0.10em;"}
          (:date-line d)]
         [:p {:style "color: var(--mtz-ink-soft); max-width: 480px; margin: 0;"} (:excerpt d)]
         [:div {:class "mtz-row" :style "gap: 12px; margin-top: 8px; flex-wrap: wrap;"}
          (cond
            (:rsvp-url d) [:a {:class "mtz-btn mtz-btn--primary" :href (:rsvp-url d)} "RSVP"]
            (:cal-url d)  [:a {:class "mtz-btn mtz-btn--ghost"   :href (:cal-url d)}  "Add to Calendar"]
            :else         [:a {:class "mtz-btn mtz-btn--primary" :href "/events"}      "View All Events"])]]]]])))

;; ---------------------------------------------------------------------------
;; ALWAYS AT MT. ZION
;; ---------------------------------------------------------------------------

(def ^:private default-activities
  [{:name "Pickleball"           :when "Wed · 6:30 PM"       :desc "Open play in Fellowship Hall. Paddles provided; all skill levels welcome."}
   {:name "Tai Chi"              :when "Tue · 9:00 AM"       :desc "Gentle movement and breath, led in the Education Wing."}
   {:name "Youth Group"          :when "Sun · 6:00 PM"       :desc "Middle and high school students gather for games, dinner, and discussion."}
   {:name "Choir Rehearsal"      :when "Wed · 7:00 PM"       :desc "Sanctuary choir prepares for Sunday worship. New voices always welcome."}
   {:name "Bible Study"          :when "Thu · 10:00 AM"      :desc "Pastor Jim leads a small-group study in the church library. Coffee provided."}
   {:name "Community Food Drive" :when "First Sat · 9–11 AM" :desc "Sort and distribute donations for Rowan Helping Ministries."}])

(defn always-at-mtz-section
  ([] (always-at-mtz-section nil))
  ([activities]
   (let [items (or (seq activities) default-activities)]
     [:section {:id "always" :class "mtz-section" :style "padding-top: 72px; padding-bottom: 72px;"}
      [:div {:style "margin-bottom: 40px;"}
       [:h2 {:class "mtz-h2" :style "margin-bottom: 8px;"} "Always at Mt. Zion"]
       [:p {:class "mtz-mute" :style "font-size: 18px; margin: 0; font-family: var(--mtz-serif-body);"}
        "Regular activities throughout the week."]]
      [:div {:style "display: grid; grid-template-columns: repeat(3, 1fr); gap: 40px;"}
       (for [a items]
         [:article {:class "mtz-card" :style "border: 0; background: transparent;"}
          [:div {:class "mtz-img" :style "aspect-ratio: 5/4; border-radius: 6px; margin-bottom: 18px;"}
           (if (:image-url a)
             [:img {:src (:image-url a) :alt (:name a)
                    :style "position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; border-radius: 6px;"}]
             [:span {:class "mtz-img-label"} (str (str/lower-case (:name a)) " · photo")])]
          [:h3 {:class "mtz-h3" :style "font-size: 24px; margin-bottom: 6px;"} (:name a)]
          [:p {:class "mtz-mono"
               :style "font-size: 12.5px; letter-spacing: 0.12em; color: var(--mtz-mint-dark); margin: 0 0 10px; font-weight: 600; text-transform: uppercase;"}
           (:when a)]
          [:p {:style "color: var(--mtz-ink-soft); margin: 0; font-size: 15.5px; line-height: 1.55;"} (:desc a)]])]])))

;; ---------------------------------------------------------------------------
;; NEWS & ANNOUNCEMENTS
;; ---------------------------------------------------------------------------

(def ^:private default-news
  [{:tag "News"        :title "Welcome to Mt Zion"
    :date "May 2026"   :excerpt "We're glad you're here. Join us for worship each Sunday at 10:30 AM."}
   {:tag "Community"   :title "Spring Food Drive"
    :date "April 2026" :excerpt "Thank you to everyone who contributed to our spring food drive for Rowan Helping Ministries."}
   {:tag "Worship"     :title "Music Ministry Update"
    :date "March 2026" :excerpt "Our choir is looking for new voices. No experience necessary — just a willingness to sing."}])

(defn news-section
  ([] (news-section nil))
  ([articles]
   (let [items (or (seq articles) default-news)]
     [:section {:id "news" :class "mtz-section--cream"}
      [:div {:class "mtz-section-inner"}
       [:div {:class "mtz-row"
              :style "justify-content: space-between; align-items: baseline; margin-bottom: 28px;"}
        [:h2 {:class "mtz-h2" :style "margin: 0;"} "News & Announcements"]
        [:a {:class "mtz-arrow-link" :href "/news"} "All news →"]]
       [:div {:class "mtz-grid mtz-grid--3"}
        (for [n items]
          [:article {:class "mtz-card"}
           (if (:image-url n)
             [:img {:src (:image-url n) :alt (:title n)
                    :style "width: 100%; aspect-ratio: 16/10; object-fit: cover;"}]
             [:div {:class "mtz-img"
                    :style "aspect-ratio: 16/10; border-radius: 0; border-left: 0; border-right: 0; border-top: 0;"}
              [:span {:class "mtz-img-label"} "image · 800×500"]])
           [:div {:class "mtz-card-body"}
            [:p {:class "mtz-card-meta"} (str (:tag n) " · " (:date n))]
            [:h3 {:class "mtz-h3" :style "font-size: 22px; margin-bottom: 10px;"} (:title n)]
            [:p {:style "color: var(--mtz-ink-soft); font-size: 15px; margin: 0;"} (:excerpt n)]]])]]])))

;; ---------------------------------------------------------------------------
;; ABOUT TEASER
;; ---------------------------------------------------------------------------

(defn about-teaser-section []
  [:section {:id "about" :class "mtz-section"}
   [:div {:class "mtz-grid mtz-grid--2" :style "gap: 64px; align-items: center;"}
    [:div {:class "mtz-img" :style "aspect-ratio: 4/5; min-height: 0;"}
     [:span {:class "mtz-img-label"} "archival photo · ca. 1910"]]
    [:div
     [:p {:class "mtz-kicker"} "Our Story · 168 Years"]
     [:h2 {:class "mtz-h2"} "A congregation that has gathered on this hill since 1858."]
     [:p {:class "mtz-prose" :style "color: var(--mtz-ink-soft);"}
      "From a one-room log meetinghouse to the sanctuary that stands today, "
      "Mount Zion's story is told in the people who have shown up — week after "
      "week, generation after generation. Our growing digital archive opens "
      "that story to anyone who'd like to look."]
     [:div {:class "mtz-row" :style "gap: 12px; margin-top: 24px; flex-wrap: wrap;"}
      [:a {:class "mtz-btn mtz-btn--ghost" :href "/about"} "Read our history"]
      [:a {:class "mtz-arrow-link" :href "/about#archive"} "Search the archive →"]]]]])

;; ---------------------------------------------------------------------------
;; OUTREACH PREVIEW
;; ---------------------------------------------------------------------------

(def ^:private default-outreach
  [{:name "Rowan Helping Ministries"
    :note "Monthly food sort · first Saturday"
    :body "Sorting and distributing donations for our neighbors in need across Rowan County."}
   {:name "Habitat for Humanity"
    :note "Spring & fall builds"
    :body "Mt Zion volunteers join two builds each year, with lunch provided on site."}
   {:name "China Grove Backpack"
    :note "Weekly · school year"
    :body "Packing weekend meals for elementary students who need food at home."}])

(defn outreach-section
  ([] (outreach-section nil))
  ([ministries]
   (let [items (or (seq ministries) default-outreach)]
     [:section {:id "outreach" :class "mtz-section--tint"}
      [:div {:class "mtz-section-inner"}
       [:div {:class "mtz-row"
              :style "justify-content: space-between; align-items: baseline; margin-bottom: 28px;"}
        [:div
         [:p {:class "mtz-kicker"} "Outreach"]
         [:h2 {:class "mtz-h2" :style "margin: 0;"} "Showing up, beyond Sunday."]]
        [:a {:class "mtz-arrow-link" :href "/outreach"} "All ministries →"]]
       [:div {:class "mtz-grid mtz-grid--3"}
        (for [o items]
          [:article {:class "mtz-card" :style "padding: 28px;"}
           [:p {:class "mtz-card-meta"} (:note o)]
           [:h3 {:class "mtz-h3" :style "font-size: 22px;"} (:name o)]
           [:p {:style "color: var(--mtz-ink-soft); margin: 0; font-size: 15px;"} (:body o)]])]]])))

;; ---------------------------------------------------------------------------
;; COMPOSED HOME PAGE
;; ---------------------------------------------------------------------------

(defn home-page
  ([] (home-page nil))
  ([data]
   (list
    (this-sunday-section    (:this-sunday data))
    (last-sunday-section    (:last-sunday data))
    (featured-event-section (:featured-event data))
    (always-at-mtz-section  (:activities data))
    (news-section           (:news data))
    (about-teaser-section)
    (outreach-section       (:outreach data)))))
