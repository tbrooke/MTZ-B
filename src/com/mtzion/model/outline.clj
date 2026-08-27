(ns com.mtzion.model.outline
  "The site as the editor sees it: menu item → page → the editable parts of that
  page.

  This has to be declared rather than derived. The public pages are hand-written
  templates that ask for named slots — `landing.clj` asks for `home-hero`,
  `worship.clj` for `current-theme` — and those names are addresses in code, not
  rows anything can be read off. A test asserts every slug here resolves and
  that no template slot is missing, so the two cannot drift apart silently.

  Three kinds of leaf, and the difference is the one that matters when someone
  asks 'can I add another section?':

    :slot   ONE row, in a place the template lays out by hand. Fixed by the
            design. A second row filed under the same slug is ignored.
    :list   MANY rows, all rendered with the same layout, ordered by the editor.
            Unlimited — this is where 'add a section' lives.
    :body   The page's own Tiptap body on the `page` row.
    :link   Rendered here but owned by another pane; the console sends you there
            rather than opening a second editor onto the same rows.

  `:fields` is what the template actually READS. The editor renders only those,
  which is the fix for one universal twelve-field form serving rows that mean
  completely different things."
  (:require [com.mtzion.model.nav :as model.nav]))

(def all-fields [:title :subtitle :body :image :cta])

(def church-tree
  [{:label "Home" :path "/" :page-slug nil
    :sections
    [{:key "hero" :label "Hero" :kind :slot :slug "home-hero"
      :fields [:title :subtitle :body :image :cta]
      :note "The large photo at the top of the home page."}
     {:key "worship" :label "Sanctuary block" :kind :slot :slug "home-worship"
      :fields [:subtitle :body :cta]
      :note (str "The \"Worship this Sunday\" block. The heading is fixed by the "
                 "design — Subtitle is the time line and Body is the paragraph.")}
     {:key "activities" :label "Always at Mt. Zion" :kind :list :slug "home-activities"
      :fields [:title :subtitle :image]
      :note (str "The graphics strip. An image is REQUIRED — one without it is "
                 "skipped rather than shown as an empty box. The heading is used "
                 "as the image's alt text, and the first card's Kicker doubles as "
                 "the paragraph beside the strip.")}
     {:key "featured-events" :label "Featured events" :kind :link
      :goto "/console/calendar"
      :note "Events with \"Featured\" ticked. Edited in the Calendar pane."}
     {:key "latest-news" :label "Latest news" :kind :link
      :goto "/console/writing"
      :note "Posts with \"Show on home page\" ticked. Edited in the Writing pane."}]}

   {:label "About" :path "/about" :page-slug "about"
    :sections
    [{:key "body" :label "Page body" :kind :body :slug "about"
      :note (str "A body here REPLACES the whole designed About page. Leave it "
                 "empty to keep the designed one and add Sections below instead.")}
     {:key "sections" :label "Sections" :kind :list :slug "about"
      :fields all-fields}]}

   {:label "Worship" :path "/worship" :page-slug "worship"
    :sections
    [{:key "theme" :label "Current theme page — header" :kind :slot :slug "current-theme"
      :fields [:title :subtitle :body :cta]
      :note (str "The headline and intro of /worship/theme. The sermon list below "
                 "it is built from the sermons themselves.")}
     {:key "sections" :label "Sections" :kind :list :slug "worship"
      :fields all-fields}]}

   {:label "Events" :path "/events" :page-slug "events"
    :sections
    [{:key "list" :label "The events themselves" :kind :link
      :goto "/console/calendar"
      :note "The listing is built from events. Edited in the Calendar pane."}
     {:key "sections" :label "Sections" :kind :list :slug "events"
      :fields all-fields}]}

   {:label "Activities" :path "/activities" :page-slug "activities"
    :sections
    [{:key "programs" :label "Seasonal & special programs" :kind :list :slug "activities"
      :fields [:title :subtitle :body]
      :note "The card grid. Subtitle is the small uppercase line above the name."}
     {:key "sections" :label "Sections" :kind :list :slug "activities-extra"
      :fields all-fields}]}

   {:label "News" :path "/news" :page-slug "news"
    :sections
    [{:key "posts" :label "The posts themselves" :kind :link
      :goto "/console/writing"
      :note "The listing is built from posts. Edited in the Writing pane."}
     {:key "sections" :label "Sections" :kind :list :slug "news"
      :fields all-fields}]}

   {:label "Outreach" :path "/outreach" :page-slug "outreach"
    :sections
    [{:key "body" :label "Page body" :kind :body :slug "outreach"
      :note (str "A body here REPLACES the whole designed Outreach page. Leave it "
                 "empty to keep the designed one and add Sections below instead.")}
     {:key "sections" :label "Sections" :kind :list :slug "outreach"
      :fields all-fields}]}

   {:label "Contact" :path "/contact" :page-slug "contact"
    :sections
    [{:key "sections" :label "Sections" :kind :list :slug "contact"
      :fields all-fields
      :note (str "Added below the form and office details, above the map. "
                 "The form itself is part of the design.")}]}

   ])

(def preschool-tree
  "The preschool site is a separate design with a separate audience, so it gets
  its own outline behind the flip rather than a branch of the church one.

  Every leaf here falls back to the copy that ships with the design, so an
  untouched section still renders exactly as it always has. `:defaults` names
  the slug in com.mtzion.content.defaults that the console can copy in to start
  editing."
  [{:label "Preschool" :path "/preschool" :page-slug "preschool" :site :preschool
    :sections
    [{:key "hero" :label "Hero" :kind :slot :slug "ps-hero" :defaults? true
      :fields [:title :subtitle :body :image :cta]
      :note (str "Heading is the big line beside the artwork; Kicker is the small "
                 "line above the paragraph. Leave the image blank for the "
                 "original artwork.")}
     {:key "welcome" :label "Director's note" :kind :slot :slug "ps-welcome" :defaults? true
      :fields [:title :subtitle :body :meta]
      :note "Meta is the signature — name · role · since, separated by ·"}
     {:key "programs" :label "Classrooms" :kind :list :slug "ps-programs" :defaults? true
      :fields [:title :subtitle :body :meta]
      :note (str "One card per classroom. Kicker is the age band; Meta is the two "
                 "lines at the bottom, separated by ·. They number themselves in "
                 "this order.")}
     {:key "day" :label "A day in the life — heading" :kind :slot :slug "ps-day" :defaults? true
      :fields [:title :subtitle :meta]
      :note "Meta is the pull quote on the coloured panel."}
     {:key "schedule" :label "The daily schedule" :kind :list :slug "ps-schedule" :defaults? true
      :fields [:title :subtitle :meta]
      :note "Kicker is the time, Heading is what happens, Meta is where."}
     {:key "values" :label "What we believe" :kind :list :slug "ps-values" :defaults? true
      :fields [:title :body]
      :note "Numbered by position — reorder them and they renumber themselves."}
     {:key "enroll" :label "Enrollment" :kind :slot :slug "ps-enroll" :defaults? true
      :fields [:title :subtitle :body :cta]}
     {:key "enroll-facts" :label "Enrollment facts" :kind :list :slug "ps-enroll-facts"
      :defaults? true
      :fields [:title :meta]
      :note "The Hours / Calendar / License row. Heading is the label, Meta the value."}]}])

(def trees
  {:church    church-tree
   :preschool preschool-tree})

(def sites
  [{:key :church    :label "Church"    :path "/"}
   {:key :preschool :label "Preschool" :path "/preschool"}])

(def tree
  "Both trees, for anything that needs to walk every leaf."
  (into church-tree preschool-tree))

;; ---------------------------------------------------------------------------
;; Lookup
;; ---------------------------------------------------------------------------

(defn page-key
  "The tree's addressable name for a top-level entry. Home has no page slug, so
  it gets a literal."
  [entry]
  (or (:page-slug entry) "home"))

(defn site-of
  "Which tree an entry belongs to. The church tree is the default."
  [entry]
  (or (:site entry) :church))

(defn pages [site]
  (get trees site church-tree))

(defn find-page [pk]
  (first (filter #(= pk (page-key %)) tree)))

(defn find-section [pk sk]
  (when-let [entry (find-page pk)]
    (first (filter #(= sk (:key %)) (:sections entry)))))

(defn feature-slugs
  "Every page_slug the tree claims to manage. The drift test compares this with
  what the templates actually query."
  []
  (into #{} (comp (mapcat :sections)
                  (filter (comp #{:slot :list} :kind))
                  (map :slug))
        tree))

(defn body-slugs []
  (into #{} (comp (mapcat :sections)
                  (filter (comp #{:body} :kind))
                  (map :slug))
        tree))

(defn cms-children
  "CMS-created pages filed under a top-level slug — they append themselves to
  the tree so a page added in the console shows up without a code change."
  [nav-pages parent-slug]
  (when parent-slug
    (filter #(= parent-slug (:parent_slug %)) nav-pages)))

(def top-level-slugs model.nav/top-level-slugs)
