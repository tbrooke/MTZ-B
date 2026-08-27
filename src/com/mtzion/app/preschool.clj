(ns com.mtzion.app.preschool
  "The preschool site. Its own design (ps-*), its own outline in the console.

  Every section reads the CMS first and falls back to the copy that ships with
  the design (com.mtzion.content.defaults). Nothing looks different until
  somebody edits something — and the console can copy the shipped text into the
  database, after which it is ordinary content."
  (:require [clojure.string :as str]
            [com.mtzion.content.defaults :as defaults]
            [com.mtzion.ui.base :as base]
            [com.mtzion.ui.sections :as sections]
            [lambdaisland.hiccup :as hiccup]))

(def ^:private artwork-url
  "https://imagedelivery.net/gNdSe_N39XhCrHxk2h53Cw/f37fb815-ddd2-4aea-c674-2dac97b18800/w=800")

;; ---------------------------------------------------------------------------
;; Content lookup
;; ---------------------------------------------------------------------------

(defn- content
  "Published rows for a slug, or the shipped defaults when there are none. The
  moment the first row is written the defaults stop applying — that section has
  been adopted, and what the console shows is what the page shows."
  [ctx slug]
  (let [rows (sections/rows ctx slug)]
    (if (seq rows) rows (defaults/rows slug))))

(defn- one [ctx slug] (first (content ctx slug)))

(defn- html [s] (when (seq s) [::hiccup/unsafe-html s]))

(defn- para
  "Body copy is Tiptap HTML once edited, but the shipped defaults for short
  fields are plain text. Either renders."
  [s]
  (when (seq s)
    (if (str/starts-with? (str/trim s) "<")
      [::hiccup/unsafe-html s]
      [:p s])))

;; ---------------------------------------------------------------------------
;; Sections
;; ---------------------------------------------------------------------------

(defn- hero [ctx]
  (let [h (one ctx "ps-hero")]
    (list
     [:section {:class "ps-hero"}
      [:div {:class "ps-hero-copy"}
       [:span {:class "ps-mono-eyebrow ps-hero-eyebrow"} (:subtitle h)]
       [:p {:class "ps-hero-lede"} (:body h)]
       [:div {:class "ps-cta-row"}
        (when (seq (:cta_label h))
          [:a {:class "ps-btn ps-btn--primary" :href (or (not-empty (:cta_url h)) "#enroll")}
           (:cta_label h)])
        [:a {:class "ps-btn ps-btn--secondary" :href "#programs"} "Our programs"]]]
      [:div {:class "ps-hero-right"}
       [:div {:class "ps-hero-art"}
        [:div {:class "ps-backplate"}]
        [:div {:class "ps-dot ps-dot-tl"}]
        [:div {:class "ps-dot ps-dot-br"}]
        [:img {:class "ps-art-img"
               :src (or (sections/image-url ctx (:image_id h) "w=800") artwork-url)
               :alt "Children at play — artwork by Linda M."}]
        [:span {:class "ps-stamp"} "Original artwork · Linda M."]]
       [:h1 {:class "ps-hero-headline"} (sections/emphasis (:title h))]]]
     [:div {:class "ps-rule-row"} [:hr]])))

(defn- welcome [ctx]
  (let [w (one ctx "ps-welcome")
        [name & rest-of-sig] (defaults/split-meta (:meta w))]
    [:section {:class "ps-welcome"}
     [:div
      [:span {:class "ps-mono-eyebrow"} (:subtitle w)]
      [:h2 (sections/emphasis (:title w))]]
     [:div {:class "ps-welcome-body"}
      (para (:body w))
      (when name
        [:div {:class "ps-signature"}
         (str "— " name)
         (when (seq rest-of-sig) [:small (str/join " · " rest-of-sig)])])]]))

(defn- programs [ctx]
  (let [cards (content ctx "ps-programs")]
    [:section {:class "ps-programs" :id "programs"}
     [:div {:class "ps-programs-inner"}
      [:div {:class "ps-section-head"}
       [:div
        [:span {:class "ps-mono-eyebrow"} "Our Classrooms"]
        [:h2 "Programs for " [:em "every age"] " & stage"]]
       [:p {:class "ps-section-lede"}
        "Three small, mixed-age classrooms — each designed around how children actually learn at their age."]]
      [:div {:class "ps-prog-grid"}
       (map-indexed
        (fn [i c]
          [:article {:class (str "ps-prog-card t" (inc (mod i 3)))}
           [:span {:class "ps-prog-tag"}]
           [:span {:class "ps-prog-age"}
            ;; The designed page numbers these from 02, not 01. Preserved
            ;; rather than quietly renumbered — it is live copy.
            (format "%02d — %s" (+ i 2) (or (:subtitle c) ""))]
           [:h3 (:title c)]
           (para (:body c))
           [:div {:class "ps-prog-meta"}
            (for [bit (defaults/split-meta (:meta c))] [:span bit])]])
        cards)]]]))

(defn- day-in-life [ctx]
  (let [d (one ctx "ps-day")]
    [:section {:class "ps-day" :id "schedule"}
     [:div {:class "ps-day-art"}
      [:div {:class "ps-day-stripe"}]
      [:span {:class "ps-day-label"} "A day in the life"]
      [:p {:class "ps-day-quote"} (:meta d)]]
     [:div
      [:span {:class "ps-mono-eyebrow"} (:subtitle d)]
      [:h2 (sections/emphasis (:title d))]
      [:ul {:class "ps-schedule"}
       (for [row (content ctx "ps-schedule")]
         [:li
          [:span {:class "ps-sched-time"} (:subtitle row)]
          [:span {:class "ps-sched-what"} (:title row)]
          [:span {:class "ps-sched-note"} (:meta row)]])]]]))

(defn- values [ctx]
  [:section {:class "ps-values"}
   [:div {:class "ps-values-inner"}
    [:span {:class "ps-values-eyebrow"} "What we believe"]
    [:h2 "A few things we " [:em "hold close"] "."]
    [:div {:class "ps-values-grid"}
     ;; The number is positional, not stored — reorder in the console and they
     ;; renumber themselves rather than going 01, 03, 02.
     (map-indexed
      (fn [i v]
        [:div
         [:div {:class "ps-value-num"} (format "%02d" (inc i))]
         [:h3 (:title v)]
         (para (:body v))])
      (content ctx "ps-values"))]]])

(defn- enrollment [ctx]
  (let [e (one ctx "ps-enroll")]
    [:section {:class "ps-enroll" :id "enroll"}
     [:span {:class "ps-mono-eyebrow ps-enroll-eyebrow"} (:subtitle e)]
     [:h2 (sections/emphasis (:title e))]
     (para (:body e))
     [:div {:class "ps-cta-row" :style "justify-content:center;"}
      (when (seq (:cta_label e))
        [:a {:class "ps-btn ps-btn--primary" :href (or (not-empty (:cta_url e)) "/contact")}
         (:cta_label e)])
      [:a {:class "ps-btn ps-btn--secondary" :href "/contact"} "Ask a question"]]
     [:div {:class "ps-enroll-meta"}
      (for [f (content ctx "ps-enroll-facts")]
        [:div
         [:span {:class "ps-enroll-k"} (:title f)]
         [:span {:class "ps-enroll-v"} (:meta f)]])]]))

;; ---------------------------------------------------------------------------

(defn preschool [ctx]
  (base/preschool-page
   ctx
   "Mt. Zion Preschool — China Grove, NC"
   (list
    (hero ctx)
    (welcome ctx)
    (programs ctx)
    (day-in-life ctx)
    (values ctx)
    (enrollment ctx))))

(def module
  {:biff.ring/routes
   [["/preschool" {:get preschool :name ::preschool}]]})
