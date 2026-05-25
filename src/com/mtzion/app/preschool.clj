(ns com.mtzion.app.preschool
  (:require [com.mtzion.ui.base :as base]))

(def ^:private artwork-url
  "https://imagedelivery.net/gNdSe_N39XhCrHxk2h53Cw/f37fb815-ddd2-4aea-c674-2dac97b18800/w=800")

(defn- hero []
  (list
   [:section {:class "ps-hero"}
    [:div {:class "ps-hero-copy"}
     [:span {:class "ps-mono-eyebrow ps-hero-eyebrow"}
      "A Nurturing Christian Early Childhood Program"]
     [:p {:class "ps-hero-lede"}
      "Quality, faith-based early childhood education in a warm, welcoming "
      "environment — serving children ages 2–5 in China Grove "
      "and Rowan County since 1989."]
     [:div {:class "ps-cta-row"}
      [:a {:class "ps-btn ps-btn--primary" :href "#enroll"} "Inquire about enrollment"]
      [:a {:class "ps-btn ps-btn--secondary" :href "#programs"} "Our programs"]]]
    [:div {:class "ps-hero-right"}
     [:div {:class "ps-hero-art"}
      [:div {:class "ps-backplate"}]
      [:div {:class "ps-dot ps-dot-tl"}]
      [:div {:class "ps-dot ps-dot-br"}]
      [:img {:class "ps-art-img" :src artwork-url :alt "Children at play — artwork by Linda M."}]
      [:span {:class "ps-stamp"} "Original artwork · Linda M."]]
     [:h1 {:class "ps-hero-headline"}
      "Where little ones " [:em "grow, play,"]
      " and find their place in the world."]]]
   [:div {:class "ps-rule-row"} [:hr]]))

(defn- welcome []
  [:section {:class "ps-welcome"}
   [:div
    [:span {:class "ps-mono-eyebrow"} "A note from our Director"]
    [:h2 "Every child is a " [:em "gift"]
     " — we are honored to share in their first steps."]]
   [:div {:class "ps-welcome-body"}
    [:p "For more than three decades, Mt. Zion Preschool has been a place where children "
     "are known by name, met with kindness, and gently guided into a lifelong love of learning. "
     "Our classrooms are small, our teachers are patient, and our days are filled with the kind "
     "of unhurried play that lets a two-year-old become a confident four-year-old without anyone "
     "rushing the work."]
    [:p "We are a ministry of Mt. Zion United Church of Christ, but our doors are open to every "
     "family. What we share with every child — regardless of background — is a "
     "commitment to safety, dignity, and joy."]
    [:div {:class "ps-signature"}
     "— Mrs. Karen Whitley"
     [:small "Director · since 2011"]]]])

(defn- programs []
  [:section {:class "ps-programs" :id "programs"}
   [:div {:class "ps-programs-inner"}
    [:div {:class "ps-section-head"}
     [:div
      [:span {:class "ps-mono-eyebrow"} "Our Classrooms"]
      [:h2 "Programs for " [:em "every age"] " & stage"]]
     [:p {:class "ps-section-lede"}
      "Three small, mixed-age classrooms — each designed around how children actually learn at their age."]]
    [:div {:class "ps-prog-grid"}
     [:article {:class "ps-prog-card t1"}
      [:span {:class "ps-prog-tag"}]
      [:span {:class "ps-prog-age"} "02 — Toddlers"]
      [:h3 "Little Lambs"]
      [:p "A gentle first step away from home. Songs, simple stories, sensory play, and lots of laps. Potty-training supported at the family's pace."]
      [:div {:class "ps-prog-meta"}
       [:span "Ages 2 – 3"]
       [:span "T / Th · ½ day"]]]
     [:article {:class "ps-prog-card t2"}
      [:span {:class "ps-prog-tag"}]
      [:span {:class "ps-prog-age"} "03 — Preschool"]
      [:h3 "Doves Class"]
      [:p "The world opens up. Friendships, dramatic play, early letters and numbers, gardening, chapel time, and outdoor adventures every day weather allows."]
      [:div {:class "ps-prog-meta"}
       [:span "Ages 3 – 4"]
       [:span "M/W/F · ½ day"]]]
     [:article {:class "ps-prog-card t3"}
      [:span {:class "ps-prog-tag"}]
      [:span {:class "ps-prog-age"} "04 — Pre-Kindergarten"]
      [:h3 "Shepherds"]
      [:p "Kindergarten-ready confidence. Phonics, journaling, problem-solving, project work, and the social-emotional skills that matter most in big school."]
      [:div {:class "ps-prog-meta"}
       [:span "Ages 4 – 5"]
       [:span "M – F · Full day option"]]]]]])

(defn- day-in-life []
  [:section {:class "ps-day" :id "schedule"}
   [:div {:class "ps-day-art"}
    [:div {:class "ps-day-stripe"}]
    [:span {:class "ps-day-label"} "A day in the life"]
    [:p {:class "ps-day-quote"}
     "Children learn most when they are loved, listened to, and allowed to wonder out loud."]]
   [:div
    [:span {:class "ps-mono-eyebrow"} "Our Rhythm"]
    [:h2 "What a day at " [:em "Mt. Zion"] " looks like."]
    [:ul {:class "ps-schedule"}
     (for [[time what note] [["8:30 AM"  "Arrival & free play"        "Classroom"]
                             ["9:15 AM"  "Morning circle & chapel"     "Sanctuary"]
                             ["9:45 AM"  "Centers & small group work"  "Classroom"]
                             ["10:45 AM" "Outdoor play"                "Playground"]
                             ["11:30 AM" "Lunch & story time"          "Fellowship"]
                             ["12:30 PM" "Rest, art, dismissal"        "Classroom"]]]
       [:li
        [:span {:class "ps-sched-time"} time]
        [:span {:class "ps-sched-what"} what]
        [:span {:class "ps-sched-note"} note]])]]])

(defn- values []
  [:section {:class "ps-values"}
   [:div {:class "ps-values-inner"}
    [:span {:class "ps-values-eyebrow"} "What we believe"]
    [:h2 "A few things we " [:em "hold close"] "."]
    [:div {:class "ps-values-grid"}
     (for [[num title body] [["01" "Faith, gently shared"
                              "Bible stories, simple songs, and a weekly chapel time — taught with warmth, never pressure. Families of every background are welcome."]
                             ["02" "Play is the work"
                              "Children do their most important learning through play. Our classrooms protect time for it — uninterrupted, imaginative, and a little messy."]
                             ["03" "Small & known"
                              "Low ratios mean every child is seen, every name is known, and every parent knows the teacher who knows their kid."]
                             ["04" "Outside, daily"
                              "Wide playground, vegetable garden, a wooded path. Children spend at least an hour outside every day school is in session."]]]
       [:div
        [:div {:class "ps-value-num"} num]
        [:h3 title]
        [:p body]])]]])

(defn- enrollment []
  [:section {:class "ps-enroll" :id "enroll"}
   [:span {:class "ps-mono-eyebrow ps-enroll-eyebrow"} "Enrollment · 2026 – 2027"]
   [:h2 "We’d love to " [:em "show you around."]]
   [:p "Tours are offered every Wednesday morning and by appointment. Families typically "
    "enroll one to three months before their child’s start date — and yes, "
    "sibling spots are held."]
   [:div {:class "ps-cta-row" :style "justify-content:center;"}
    [:a {:class "ps-btn ps-btn--primary" :href "/contact"} "Schedule a tour"]
    [:a {:class "ps-btn ps-btn--secondary" :href "/contact"} "Ask a question"]]
   [:div {:class "ps-enroll-meta"}
    [:div [:span {:class "ps-enroll-k"} "Hours"] [:span {:class "ps-enroll-v"} "8:30 AM – 12:30 PM"]]
    [:div [:span {:class "ps-enroll-k"} "Calendar"] [:span {:class "ps-enroll-v"} "September – May"]]
    [:div [:span {:class "ps-enroll-k"} "License"] [:span {:class "ps-enroll-v"} "NC 5-Star Center"]]]])

(defn preschool [_ctx]
  (base/preschool-page
   "Mt. Zion Preschool — China Grove, NC"
   (list
    (hero)
    (welcome)
    (programs)
    (day-in-life)
    (values)
    (enrollment))))

(def module
  {:biff.ring/routes
   [["/preschool" {:get preschool :name ::preschool}]]})
