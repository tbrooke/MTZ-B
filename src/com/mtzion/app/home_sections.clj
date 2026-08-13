(ns com.mtzion.app.home-sections
  "Homepage section components. Sections take CMS data where it exists and fall
  back to the static defaults below, so the page never renders empty."
  (:require [clojure.string :as str]
            [com.mtzion.model.church :as church]
            [lambdaisland.hiccup :as hiccup]))

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
  [["9:30 AM"  "Sunday School"       "All ages · Education Wing"]
   ["10:30 AM" "Traditional Worship" "Sanctuary · choir & organ"]])

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
;; WHAT'S COMING UP  (secretary event flyers, replaces old featured-event)
;; ---------------------------------------------------------------------------

(defn- flyer-card [f]
  (let [cta-href  (or (when (seq (:cta-url f)) (:cta-url f))
                      (when (seq (:cta_url f)) (:cta_url f))
                      "/events")
        img-url   (:image-url f)
        body-html (when (seq (:body f)) (:body f))
        has-text? (or (seq (:title f)) (seq (:subtitle f)) (seq (:cta-label f)) (seq (:cta_label f)))]
    [:article {:class "mtz-card"}
     [:a {:href cta-href :style "display: block;"}
      [:div {:class "mtz-flyer-img"}
       (cond
         img-url   [:img {:src img-url :alt (or (:title f) "Event")
                          :style "width:100%; height:100%; object-fit:cover; display:block;"}]
         body-html [::hiccup/unsafe-html body-html]
         :else     nil)]]
     (when has-text?
       [:div {:class "mtz-card-body"}
        (when (seq (:subtitle f))
          [:p {:class "mtz-card-meta"} (:subtitle f)])
        (when (seq (:title f))
          [:h3 {:class "mtz-h3" :style "font-size: 22px; margin-bottom: 8px;"} (:title f)])
        (let [label (or (:cta-label f) (:cta_label f))]
          (when (seq label)
            [:a {:class "mtz-arrow-link" :href cta-href} (str label " →")]))])]))

(defn whats-coming-up-section
  ([] nil)
  ([features]
   (when (seq features)
     [:section {:id "events" :class "mtz-section--tint"}
      [:div {:class "mtz-section-inner"}
       [:div {:class "mtz-row"
              :style "justify-content: space-between; align-items: baseline; margin-bottom: 28px;"}
        [:div
         [:p {:class "mtz-kicker" :style "margin: 0;"} "What's Coming Up"]
         [:h2 {:class "mtz-h2" :style "margin: 4px 0 0;"} "Mark your calendar."]]
        [:a {:class "mtz-arrow-link" :href "/events"} "All events →"]]
       [:div {:class (if (> (count features) 1) "mtz-grid mtz-grid--2" "mtz-grid")
              :style "gap: 36px; align-items: stretch;"}
        (map flyer-card features)]]])))

;; ---------------------------------------------------------------------------
;; WORSHIP SANCTUARY — Gothic arch section (home page)
;; ---------------------------------------------------------------------------

(def ^:private worship-green "#2f5a3f")

(defn worship-sanctuary-section
  ([] (worship-sanctuary-section nil))
  ([data]
   (let [img-url   (:image-url data)
         body-text (or (:body data)
                       "One service. Scripture, prayer, and song beneath the windows that have watched over this congregation since 1910 — with our choir and pipe organ.")
         time-txt  (or (:subtitle data) "10:30 AM · Every Sunday")
         cta-label (or (:cta-label data) "Plan a visit")
         cta-url   (or (:cta-url data) "/contact")]
     [:section {:id "worship" :class "mtz-section--cream"}
      [:div {:class "mtz-section-inner"}
       ;; header row
       [:p {:style (str "font-family: ui-monospace,'SF Mono',Menlo,Consolas,monospace;"
                        " font-size: 11px; letter-spacing: 0.18em; text-transform: uppercase;"
                        " color: " worship-green "; font-weight: 500; margin: 0 0 14px;")}
        "This Sunday"]
       [:h2 {:style (str "font-family: var(--mtz-serif-display);"
                         " font-size: 52px; font-weight: 400; line-height: 1.05;"
                         " letter-spacing: -0.01em; color: var(--mtz-ink); margin: 0 0 32px;")}
        "Worship "
        [:em {:style (str "font-style: italic; color: " worship-green ";")} "this Sunday"]]
       [:hr {:style "height: 1px; background: var(--mtz-ink); border: 0; opacity: 0.85; margin: 0 0 40px;"}]
       ;; two-column body
       [:div {:class "mtz-arch-grid"
              :style "display: grid; grid-template-columns: 0.85fr 1fr; gap: 80px; align-items: center;"}
        ;; left: arch image
        [:div {:style "display: flex; justify-content: flex-start;"}
         [:div {:class "mtz-arch-frame"}
          [:div {:class "mtz-arch-inner"}
           (if (seq img-url)
             [:img {:src   img-url
                    :alt   "Stained-glass window in the Mt Zion sanctuary depicting Christ as the Good Shepherd with a lamb and sheep"
                    :style "width: 100%; height: 100%; object-fit: cover; object-position: center 30%;"}]
             [:div {:style "width: 100%; height: 100%; background: var(--mtz-stone);"}])]
          [:div {:class "mtz-arch-mullion"}]
          [:div {:class "mtz-arch-plaque"} "The Good Shepherd · c. 1910"]]]
        ;; right: service details
        [:div {:style "padding-bottom: 28px;"}
         [:p {:style (str "font-family: ui-monospace,'SF Mono',Menlo,Consolas,monospace;"
                          " font-size: 13px; letter-spacing: 0.14em; text-transform: uppercase;"
                          " color: " worship-green "; font-weight: 500; margin: 0 0 14px;")}
          time-txt]
         [:h3 {:style (str "font-family: var(--mtz-serif-display);"
                           " font-size: 52px; font-weight: 400; line-height: 1.05;"
                           " letter-spacing: -0.01em; color: var(--mtz-ink); margin: 0 0 22px;")}
          "Gather in the" [:br]
          [:em {:style (str "font-style: italic; color: " worship-green ";")} "Sanctuary."]]
         [:p {:style (str "font-family: var(--mtz-serif-body); font-size: 18px; line-height: 1.55;"
                          " color: var(--mtz-ink-soft); max-width: 480px; margin: 0 0 22px;")}
          body-text]
         [:p {:style (str "font-family: var(--mtz-serif-body); font-size: 14px;"
                          " color: var(--mtz-ink-mute); margin: 0 0 36px;")}
          "Sanctuary · ≈60 min · Nursery provided"]
         [:a {:href  cta-url
              :style (str "display: inline-flex; align-items: center;"
                          " background: " worship-green "; color: var(--mtz-cream);"
                          " padding: 14px 22px; border-radius: 0; border: 0;"
                          " font-family: ui-monospace,'SF Mono',Menlo,Consolas,monospace;"
                          " font-size: 12px; font-weight: 500; letter-spacing: 0.14em;"
                          " text-transform: uppercase; text-decoration: none;"
                          " transition: background 150ms;")}
          cta-label]]]]])))

;; ---------------------------------------------------------------------------
;; ALWAYS AT MT. ZION
;; ---------------------------------------------------------------------------

(def ^:private default-activity-blurb
  (str "Tai Chi, pickleball, Scouts, choir practice, the lectionary group, JOY Club — "
       "there's something happening at Mt. Zion nearly every day of the week."))

(defn always-at-mtz-section
  "Copy on the left third, activity graphics on the right two thirds.

  Cards come from CMS `feature` rows with page_slug \"home-activities\", so
  graphics are added and reordered in the admin rather than in this file. Items
  without an image are skipped — a grey placeholder box reads as an unfinished
  site, which is the impression this redesign exists to fix."
  ([] (always-at-mtz-section nil))
  ([{:keys [cards blurb]}]
   (let [cards (filter :image-url cards)
         copy  [:div
                [:p {:class "mtz-kicker"} "Every Week"]
                [:h2 {:class "mtz-h2" :style "margin-bottom: 16px;"} "Always at Mt. Zion"]
                [:p {:class "mtz-mute"
                     :style "font-size: 18px; margin: 0 0 20px; font-family: var(--mtz-serif-body); line-height: 1.55;"}
                 (or (not-empty blurb) default-activity-blurb)]
                [:a {:class "mtz-arrow-link" :href "/activities"} "See all activities →"]]]
     [:section {:id "always" :class "mtz-section" :style "padding-top: 72px; padding-bottom: 72px;"}
      (if (seq cards)
        [:div {:class "mtz-always-grid"}
         copy
         [:div {:class "mtz-always-cards"}
          (for [c cards]
            [:img {:src (:image-url c) :alt (or (not-empty (:name c)) "Mt. Zion activity")}])]]
        ;; No graphics yet — show the copy full width rather than an empty column.
        [:div {:style "max-width: 640px;"} copy])])))

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
           ;; No image, no box — see the same decision in news.clj/post-card.
           (when (:image-url n)
             [:img {:src (:image-url n) :alt (:title n)
                    :style "width: 100%; aspect-ratio: 16/10; object-fit: cover;"}])
           [:div {:class "mtz-card-body"}
            [:p {:class "mtz-card-meta"} (str (:tag n) " · " (:date n))]
            [:h3 {:class "mtz-h3" :style "font-size: 22px; margin-bottom: 10px;"}
             (if (:url n)
               [:a {:href (:url n) :style "color: inherit;"} (:title n)]
               (:title n))]
            [:p {:style "color: var(--mtz-ink-soft); font-size: 15px; margin: 0;"} (:excerpt n)]
            (when (:url n)
              [:a {:class "mtz-arrow-link" :href (:url n)
                   :style "margin-top: 12px; display: inline-flex;"}
               "Read more →"])]])]]])))

;; ---------------------------------------------------------------------------
;; ABOUT TEASER — PARKED, not currently on the home page.
;;
;; Removed 2026-08 pending the history work. As written it promised a "growing
;; digital archive" that does not exist, linked to /about#archive (an anchor that
;; was never added), and carried the home page's last grey placeholder. Kept here
;; so it can be reinstated with real content and a real archive behind it — add
;; `(about-teaser-section)` back to `home-page` below.
;; ---------------------------------------------------------------------------

(defn about-teaser-section []
  [:section {:id "about" :class "mtz-section"}
   [:div {:class "mtz-grid mtz-grid--2" :style "gap: 64px; align-items: center;"}
    [:div {:class "mtz-img" :style "aspect-ratio: 4/5; min-height: 0;"}
     [:span {:class "mtz-img-label"} "archival photo · ca. 1910"]]
    [:div
     [:p {:class "mtz-kicker"} (str "Our Story · " (church/years-since-founding) " Years")]
     [:h2 {:class "mtz-h2"} (str "A congregation that has gathered on this hill since " church/founded-year ".")]
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
    (whats-coming-up-section   (:features data))
    (worship-sanctuary-section (:worship data))
    (always-at-mtz-section     (:activities data))
    (news-section              (:news data))
    ;; (about-teaser-section)  — parked until the history work; see above.
    (outreach-section          (:outreach data)))))
