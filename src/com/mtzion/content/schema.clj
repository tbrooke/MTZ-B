(ns com.mtzion.content.schema
  "Malli schemas for the EDN content contract.

  Design rules, all aimed at making an LLM's output reliable and at keeping the
  quirks of HTML form handling out of the import path:

  - Typed values. Booleans are real booleans, never 1/0 or key-presence. Dates
    are ISO strings with one stated timezone meaning.
  - Closed maps. An unrecognised key is almost always a hallucinated field, so
    it is an error rather than something silently dropped.
  - No `:published`. Draft state is the importer's decision, not the agent's.
  - No media fields. image-id / video-id / bulletin-path stay human-controlled so
    a re-import can never clobber an image somebody attached by hand.

  Pure: no I/O, no ctx, no DB."
  (:require [com.mtzion.content.hiccup :as ch]
            [com.mtzion.model.nav :as model.nav]
            [malli.core :as m]
            [malli.error :as me]))

;; ---------------------------------------------------------------------------
;; Scalars
;; ---------------------------------------------------------------------------

(def LocalDate
  [:re {:error/message "must be a date like \"2026-08-09\""}
   #"^\d{4}-\d{2}-\d{2}$"])

(def LocalDateTime
  [:re {:error/message
        "must be a date and time like \"2026-08-09T18:30\" (Mount Zion local time, 24-hour)"}
   #"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$"])

(def ImportKey
  [:re {:error/message
        "must be a stable lowercase identifier, 3-64 chars, e.g. \"vbs-2026\""}
   #"^[a-z0-9][a-z0-9-]{2,63}$"])

(def Slug
  [:re {:error/message "must be lowercase letters, digits and dashes, e.g. \"johns-river\""}
   #"^[a-z0-9][a-z0-9-]{0,63}$"])

(def Recurrence
  [:enum :none :daily :weekly :biweekly :monthly :yearly])

(def PostCategory
  [:enum :news :blog :reflection])

(def ParentSlug
  (into [:enum] (map keyword) model.nav/top-level-slugs))

(def FeaturePageSlug
  "Named content slots. These are addresses in the page templates, not free text —
  `home-worship` drives the sanctuary section, `current-theme` the worship banner."
  [:enum :home :home-worship :current-theme :activities])

;; ---------------------------------------------------------------------------
;; Items
;; ---------------------------------------------------------------------------

(def Event
  [:map {:closed true}
   [:type [:= :event]]
   [:key ImportKey]
   [:title [:string {:min 1 :max 200}]]
   [:starts-at LocalDateTime]
   [:ends-at {:optional true} [:maybe LocalDateTime]]
   [:all-day {:optional true} :boolean]
   [:location {:optional true} [:maybe [:string {:max 200}]]]
   [:description {:optional true} [:maybe ch/Hiccup]]
   [:recurrence {:optional true} Recurrence]
   [:recur-until {:optional true} [:maybe LocalDate]]
   [:featured {:optional true} :boolean]])

(def Post
  [:map {:closed true}
   [:type [:= :post]]
   [:key ImportKey]
   [:title [:string {:min 1 :max 200}]]
   [:slug {:optional true} [:maybe Slug]]
   [:category {:optional true} PostCategory]
   [:excerpt {:optional true} [:maybe [:string {:max 300}]]]
   [:body {:optional true} [:maybe ch/Hiccup]]
   [:published-on {:optional true} [:maybe LocalDate]]
   [:show-on-home {:optional true} :boolean]])

(def Page
  [:map {:closed true}
   [:type [:= :page]]
   [:key ImportKey]
   [:slug Slug]
   [:title {:optional true} [:maybe [:string {:max 200}]]]
   [:parent {:optional true} [:maybe ParentSlug]]
   [:nav-label {:optional true} [:maybe [:string {:max 40}]]]
   [:nav-order {:optional true} [:maybe [:int {:min 1 :max 99}]]]
   [:body {:optional true} [:maybe ch/Hiccup]]])

(def Feature
  [:map {:closed true}
   [:type [:= :feature]]
   [:key ImportKey]
   [:page-slug FeaturePageSlug]
   [:title [:string {:min 1 :max 200}]]
   [:subtitle {:optional true} [:maybe [:string {:max 200}]]]
   [:body {:optional true} [:maybe ch/Hiccup]]
   [:cta-label {:optional true} [:maybe [:string {:max 60}]]]
   [:cta-url {:optional true} [:maybe [:string {:max 300}]]]
   [:sort-order {:optional true} [:maybe [:int {:min 0 :max 999}]]]
   [:show-on-home {:optional true} :boolean]])

(def Sermon
  ;; :description is plain text, not Hiccup — sermons.clj renders it as an
  ;; escaped string rather than through unsafe-html.
  [:map {:closed true}
   [:type [:= :sermon]]
   [:key ImportKey]
   [:sermon-date LocalDate]
   [:title [:string {:min 1 :max 200}]]
   [:scripture-cw {:optional true} [:maybe [:string {:max 120}]]]
   [:scripture-gospel {:optional true} [:maybe [:string {:max 120}]]]
   [:series {:optional true} [:maybe Slug]]
   [:description {:optional true} [:maybe [:string {:max 2000}]]]])

(def Item
  [:multi {:dispatch :type
           :error/message "must have a :type of :event, :post, :page, :feature or :sermon"}
   [:event Event]
   [:post Post]
   [:page Page]
   [:feature Feature]
   [:sermon Sermon]])

;; ---------------------------------------------------------------------------
;; Cross-field rules
;; ---------------------------------------------------------------------------

;; NOTE: there is deliberately no "recurring events must have :recur-until" rule.
;; Most of what a church bulletin lists — Tai Chi, choir practice, Scouts,
;; pickleball — is an ongoing weekly activity with no end date, and the rest of
;; the system already treats that as first-class: recur_until is nullable,
;; model.event/occurrences-in-range handles a nil until, and the admin form's own
;; hint reads "leave blank for no end date". An earlier version of this file
;; required it and rejected 11 of the first real bulletin's events.

(defn- end-after-start [{:keys [type starts-at ends-at]}]
  (when (and (= type :event) starts-at ends-at (neg? (compare ends-at starts-at)))
    ":ends-at is before :starts-at"))

(defn- nav-label-needs-order [{:keys [type nav-label nav-order]}]
  (when (and (= type :page) (seq nav-label) (nil? nav-order))
    (str "a page with a :nav-label should also set :nav-order, "
         "otherwise its position in the menu is arbitrary")))

(def ^:private cross-field-rules
  [end-after-start nav-label-needs-order])

(defn cross-field-errors
  "Rules that span several fields. Run after the shape check so the messages are
  not buried under type errors. Returns a seq of {:index :key :message}."
  [items]
  (for [[i item] (map-indexed vector items)
        rule     cross-field-rules
        :let     [msg (rule item)]
        :when    msg]
    {:index i :key (:key item) :message msg}))

(defn duplicate-key-errors
  "Two items sharing a :key would fight over the same row on every import."
  [items]
  (for [[k group] (group-by :key items)
        :when (and k (> (count group) 1))]
    {:key k :message (str "duplicate :key — used by " (count group) " items")}))

;; ---------------------------------------------------------------------------
;; Envelope
;; ---------------------------------------------------------------------------

(def contract-version 1)

(def Envelope
  [:map {:closed true}
   [:mtz/contract [:= contract-version]]
   [:contract-sha {:optional true} [:maybe :string]]
   [:source {:optional true}
    [:map {:closed true}
     [:kind {:optional true} [:enum :bulletin :slides :newsletter :other]]
     [:files {:optional true} [:sequential :string]]
     [:extracted-on {:optional true} LocalDate]]]
   [:items [:sequential Item]]])

;; ---------------------------------------------------------------------------
;; Entry point
;; ---------------------------------------------------------------------------

(defn validate
  "Returns {:ok? true :envelope env} or {:ok? false :errors {...}}.

  :errors carries :shape (humanized Malli output), :hiccup (per-item field paths
  from com.mtzion.content.hiccup/explain), :cross-field and :duplicates."
  [envelope]
  (if-not (m/validate Envelope envelope)
    {:ok?    false
     :errors {:shape (me/humanize (m/explain Envelope envelope))
              :hiccup (vec
                       (for [[i item] (map-indexed vector (:items envelope))
                             field    [:body :description]
                             :let     [v (get item field)]
                             :when    (and v (not (ch/valid? v)))
                             err      (ch/explain v)]
                         (assoc err :index i :key (:key item) :field field)))}}
    (let [cross (cross-field-errors (:items envelope))
          dupes (duplicate-key-errors (:items envelope))]
      (if (or (seq cross) (seq dupes))
        {:ok? false :errors {:cross-field (vec cross) :duplicates (vec dupes)}}
        {:ok? true :envelope envelope}))))
