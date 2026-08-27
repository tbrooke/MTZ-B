(ns com.mtzion.content.defaults
  "The copy that ships with the design, as data.

  Until now this text lived inside the page templates, which meant changing a
  word was a code change. Moving it here does two things at once: the template
  falls back to it when nothing has been written in the console, so the page
  looks exactly as it always did; and the console can offer to copy it into the
  database, after which it is ordinary editable content.

  That is the incremental path off static text — a page is not rewritten, it is
  adopted one section at a time.

  Keyed by the same `page_slug` the outline declares. Pure data: no ctx, no I/O."
  (:require [clojure.string :as str]))

(def preschool
  {"ps-hero"
   [{:title    "Where little ones *grow, play,* and find their place in the world."
     :subtitle "A Nurturing Christian Early Childhood Program"
     :body     (str "Quality, faith-based early childhood education in a warm, welcoming "
                    "environment — serving children ages 2–5 in China Grove and Rowan "
                    "County since 1989.")
     :cta_label "Inquire about enrollment"
     :cta_url   "#enroll"}]

   "ps-welcome"
   [{:title    "Every child is a *gift* — we are honored to share in their first steps."
     :subtitle "A note from our Director"
     :body     (str "<p>For more than three decades, Mt. Zion Preschool has been a place "
                    "where children are known by name, met with kindness, and gently guided "
                    "into a lifelong love of learning. Our classrooms are small, our teachers "
                    "are patient, and our days are filled with the kind of unhurried play that "
                    "lets a two-year-old become a confident four-year-old without anyone "
                    "rushing the work.</p>"
                    "<p>We are a ministry of Mt. Zion United Church of Christ, but our doors "
                    "are open to every family. What we share with every child — regardless of "
                    "background — is a commitment to safety, dignity, and joy.</p>")
     :meta     "Mrs. Karen Whitley | Director · since 2011"}]

   "ps-programs"
   [{:subtitle "Toddlers"  :title "Little Lambs"
     :body     (str "A gentle first step away from home. Songs, simple stories, sensory play, "
                    "and lots of laps. Potty-training supported at the family's pace.")
     :meta     "Ages 2 – 3 | T / Th · ½ day"}
    {:subtitle "Preschool" :title "Doves Class"
     :body     (str "The world opens up. Friendships, dramatic play, early letters and numbers, "
                    "gardening, chapel time, and outdoor adventures every day weather allows.")
     :meta     "Ages 3 – 4 | M/W/F · ½ day"}
    {:subtitle "Pre-Kindergarten" :title "Shepherds"
     :body     (str "Kindergarten-ready confidence. Phonics, journaling, problem-solving, "
                    "project work, and the social-emotional skills that matter most in big school.")
     :meta     "Ages 4 – 5 | M – F · Full day option"}]

   "ps-day"
   [{:title    "What a day at *Mt. Zion* looks like."
     :subtitle "Our Rhythm"
     :meta     (str "Children learn most when they are loved, listened to, and allowed to "
                    "wonder out loud.")}]

   "ps-schedule"
   [{:subtitle "8:30 AM"  :title "Arrival & free play"       :meta "Classroom"}
    {:subtitle "9:15 AM"  :title "Morning circle & chapel"    :meta "Sanctuary"}
    {:subtitle "9:45 AM"  :title "Centers & small group work" :meta "Classroom"}
    {:subtitle "10:45 AM" :title "Outdoor play"               :meta "Playground"}
    {:subtitle "11:30 AM" :title "Lunch & story time"         :meta "Fellowship"}
    {:subtitle "12:30 PM" :title "Rest, art, dismissal"       :meta "Classroom"}]

   "ps-values"
   [{:title "Faith, gently shared"
     :body  (str "Bible stories, simple songs, and a weekly chapel time — taught with warmth, "
                 "never pressure. Families of every background are welcome.")}
    {:title "Play is the work"
     :body  (str "Children do their most important learning through play. Our classrooms "
                 "protect time for it — uninterrupted, imaginative, and a little messy.")}
    {:title "Small & known"
     :body  (str "Low ratios mean every child is seen, every name is known, and every parent "
                 "knows the teacher who knows their kid.")}
    {:title "Outside, daily"
     :body  (str "Wide playground, vegetable garden, a wooded path. Children spend at least an "
                 "hour outside every day school is in session.")}]

   "ps-enroll"
   [{:title     "We’d love to *show you around.*"
     :subtitle  "Enrollment · 2026 – 2027"
     :body      (str "Tours are offered every Wednesday morning and by appointment. Families "
                     "typically enroll one to three months before their child’s start date — "
                     "and yes, sibling spots are held.")
     :cta_label "Schedule a tour"
     :cta_url   "/contact"}]

   "ps-enroll-facts"
   [{:title "Hours"    :meta "8:30 AM – 12:30 PM"}
    {:title "Calendar" :meta "September – May"}
    {:title "License"  :meta "NC 5-Star Center"}]})

(def by-slug preschool)

(defn rows
  "The shipped copy for one slug, or nil. Used as the template's fallback and as
  the source for the console's \"copy these in\" action."
  [slug]
  (get by-slug slug))

(defn one [slug] (first (rows slug)))

(defn split-meta
  "A meta line is one input in the editor but can render as separate cells.
  `|` is the separator — deliberately not `·`, which appears inside the copy
  itself (\"T / Th · ½ day\" is one cell, not two)."
  [s]
  (when (seq s)
    (mapv str/trim (str/split s #"\|"))))
