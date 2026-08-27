(ns com.mtzion.ui.sections
  "The generic sections region every public page carries.

  A page's *designed* parts — the hero, the sanctuary block, the sermon-series
  header — are named slots the template lays out by hand, and adding a new kind
  of those is a code change because someone has to decide what it looks like.

  This is the other half: an unlimited, ordered list of ordinary sections filed
  under a page's own slug, all rendered with one layout. Two sections or nine,
  no code change. It is what makes 'add a section to the Worship page' a thing
  you do in the console rather than a thing you ask a developer for.

  Storage is the `feature` table, which already had every column this needs."
  (:require [clojure.string :as str]
            [com.mtzion.model.content :as content]
            [lambdaisland.hiccup :as hiccup]))

(defn image-url
  "Cloudflare delivery URL. The account hash segment is required — without it
  the URL 404s, which is why images silently never appeared."
  [ctx image-id variant]
  (when (seq image-id)
    (str "https://imagedelivery.net/" (:cf/images-hash ctx) "/" image-id "/"
         (or variant "public"))))

(defn emphasis
  "A short heading that may italicise part of itself: *like this*.

  The designed headings carry emphasis mid-sentence (\"Where little ones *grow,
  play,* and find their place\"), and losing it flattens the type. A plain text
  input plus one convention keeps that editable without handing an editor raw
  HTML — everything outside the asterisks is escaped."
  [s]
  (when (seq s)
    (let [esc (fn [t] (-> t (str/replace "&" "&amp;")
                          (str/replace "<" "&lt;")
                          (str/replace ">" "&gt;")))
          parts (str/split s #"\*" -1)]
      [::hiccup/unsafe-html
       (str/join (map-indexed (fn [i part]
                                (if (odd? i)
                                  (str "<em>" (esc part) "</em>")
                                  (esc part)))
                              parts))])))

(defn rows
  "Published sections for one page slug, in the order the editor set."
  [ctx page-slug]
  (content/live ctx :feature {:where [:= :page_slug page-slug]
                              :order [[:sort_order :asc] [:created_at :asc]]}))

(defn- body-block [f]
  (when (seq (:body f))
    [:div {:class "mtz-prose" :style "color: var(--mtz-ink-soft);"}
     [::hiccup/unsafe-html (:body f)]]))

(defn- cta-block [f]
  (when (and (seq (:cta_label f)) (seq (:cta_url f)))
    [:a {:class "mtz-btn mtz-btn--primary" :href (:cta_url f)
         :style "margin-top: 24px;"}
     (:cta_label f)]))

(defn- heading-block [f]
  (list
   (when (seq (:subtitle f))
     [:p {:class "mtz-kicker"} (:subtitle f)])
   (when (seq (:title f))
     [:h2 {:class "mtz-h2" :style "margin-bottom: 20px;"} (:title f)])))

(defn section
  "One section. With an image it becomes two columns, alternating side so a run
  of them does not read as a stack of identical blocks; without one it is a
  single measured column."
  [ctx f index]
  (let [img  (image-url ctx (:image_id f) "public")
        alt? (odd? index)]
    [:section {:class (if alt? "mtz-section--cream" "mtz-section")}
     [:div {:class "mtz-section-inner"}
      (if img
        [:div {:class "mtz-grid mtz-grid--2"
               :style "gap: 56px; align-items: center;"}
         [:div {:style (str "order: " (if alt? 2 1) ";")}
          (heading-block f)
          (body-block f)
          (cta-block f)]
         [:div {:style (str "order: " (if alt? 1 2) ";")}
          [:img {:src img :alt (or (:title f) "")
                 :style "width: 100%; height: auto; border-radius: 6px; display: block;"}]]]
        [:div {:style "max-width: 720px;"}
         (heading-block f)
         (body-block f)
         (cta-block f)])]]))

(defn region
  "Every section filed under `page-slug`, or nil when there are none — so a page
  with nothing added looks exactly as it did before."
  [ctx page-slug]
  (let [fs (rows ctx page-slug)]
    (when (seq fs)
      (map-indexed (fn [i f] (section ctx f i)) fs))))
