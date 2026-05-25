(ns com.mtzion.app.sermons
  (:require [clojure.string :as str]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.ui.base :as base]
            [lambdaisland.hiccup :as hiccup]))

(defn- normalize [rows]
  (mapv (fn [row]
          (into {} (map (fn [[k v]] [(keyword (str/replace (name k) "-" "_")) v]) row)))
        rows))

(defn- format-date [epoch]
  (when epoch
    (-> (java.time.Instant/ofEpochSecond epoch)
        (java.time.LocalDate/ofInstant java.time.ZoneOffset/UTC)
        (.format (java.time.format.DateTimeFormatter/ofPattern "MMMM d, yyyy")))))

(defn- format-date-short [epoch]
  (when epoch
    (-> (java.time.Instant/ofEpochSecond epoch)
        (java.time.LocalDate/ofInstant java.time.ZoneOffset/UTC)
        (.format (java.time.format.DateTimeFormatter/ofPattern "MMMM d, yyyy")))))

;; ---------------------------------------------------------------------------
;; Listing page
;; ---------------------------------------------------------------------------

(def ^:private per-page 10)

(defn- sermon-row [s]
  [:article {:style "display: flex; gap: 28px; align-items: flex-start; padding: 32px 0; border-bottom: 1px solid var(--mtz-rule);"}
   ;; thumbnail
   [:a {:href  (str "/sermons/" (:id s))
        :style "flex: 0 0 220px; display: block;"}
    [:div {:style (str "position: relative; width: 220px; aspect-ratio: 16/9;"
                       " border-radius: 6px; overflow: hidden;"
                       " background: var(--mtz-stone); border: 1px solid var(--mtz-rule);")}
     (if (:video_id s)
       (list
        [:img {:src   (str "https://videodelivery.net/" (:video_id s) "/thumbnails/thumbnail.jpg")
               :alt   (:title s)
               :style "position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover;"}]
        [:div {:style "position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;"}
         [:div {:style "width: 40px; height: 40px; border-radius: 50%; background: rgba(0,0,0,0.55); display: flex; align-items: center; justify-content: center; color: #fff; font-size: 16px;"}
          "▶"]])
       [:div {:style "position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;"}
        [:div {:style "width: 40px; height: 40px; border-radius: 50%; background: rgba(0,0,0,0.25); display: flex; align-items: center; justify-content: center; color: #fff; font-size: 16px;"}
         "▶"]])]]
   ;; text
   [:div {:style "flex: 1; min-width: 0;"}
    [:p {:class "mtz-card-meta" :style "margin-bottom: 6px;"}
     (str (or (format-date (:sermon_date s)) "")
          (when (seq (:scripture s)) (str " · " (:scripture s))))]
    [:h3 {:class "mtz-h3" :style "font-size: 22px; margin-bottom: 8px;"}
     [:a {:href (str "/sermons/" (:id s)) :style "color: inherit;"} (:title s)]]
    (when (seq (:description s))
      [:p {:style "color: var(--mtz-ink-soft); font-size: 15px; margin: 0 0 12px; max-width: 560px; line-height: 1.55;"}
       (let [d (:description s)]
         (if (> (count d) 160) (str (subs d 0 160) "…") d))])
    [:a {:class "mtz-arrow-link" :href (str "/sermons/" (:id s))} "View this sermon →"]]])

(defn- pagination [page total-pages]
  (when (> total-pages 1)
    [:div {:style "display: flex; gap: 12px; align-items: center; padding-top: 32px; justify-content: center;"}
     (if (> page 1)
       [:a {:class "mtz-btn mtz-btn--ghost" :href (str "/sermons?page=" (dec page))} "← Newer"]
       [:span {:class "mtz-btn" :style "opacity: 0.35; pointer-events: none;"} "← Newer"])
     [:span {:class "mtz-mono" :style "font-size: 13px; color: var(--mtz-ink-mute);"}
      (str "Page " page " of " total-pages)]
     (if (< page total-pages)
       [:a {:class "mtz-btn mtz-btn--ghost" :href (str "/sermons?page=" (inc page))} "Older →"]
       [:span {:class "mtz-btn" :style "opacity: 0.35; pointer-events: none;"} "Older →"])]))

(defn sermon-list [{:keys [query-params] :as ctx}]
  (let [page       (max 1 (try (Integer/parseInt (get query-params "page" "1")) (catch Exception _ 1)))
        offset     (* (dec page) per-page)
        total      (:n (first (biff.sqlite/execute ctx ["SELECT COUNT(*) AS n FROM sermon WHERE published = 1"])))
        total-pages (max 1 (int (Math/ceil (/ (double (or total 0)) per-page))))
        sermons    (normalize (biff.sqlite/execute ctx {:select   :*
                                                        :from     :sermon
                                                        :where    [:= :published 1]
                                                        :order-by [[:sermon_date :desc]]
                                                        :limit    per-page
                                                        :offset   offset}))]
    (base/page "Sermon Archive — Mount Zion UCC"
               (list
                [:section {:class "mtz-section"}
                 [:p {:class "mtz-kicker"} "Sunday Worship · 10:30 AM"]
                 [:h1 {:class "mtz-h1" :style "max-width: 760px;"} "Sermon Archive"]
                 [:p {:class "mtz-lede" :style "max-width: 580px;"}
                  "Messages from Sunday worship at Mount Zion UCC."]
                 [:hr {:class "mtz-rule"}]
                 (if (seq sermons)
                   (list
                    [:div (map sermon-row sermons)]
                    (pagination page total-pages))
                   [:p {:class "mtz-mute" :style "padding: 48px 0;"}
                    "No sermons posted yet."])]))))

;; ---------------------------------------------------------------------------
;; Detail page
;; ---------------------------------------------------------------------------

(defn- pdf-card [path label]
  (when (seq path)
    [:a {:href path :target "_blank" :rel "noopener"
         :style "display: flex; flex-direction: column; gap: 0; text-decoration: none; color: inherit; width: 180px;"}
     [:div {:style (str "background: var(--mtz-bg-tint); border: 1px solid var(--mtz-rule);"
                        " border-radius: 6px 6px 0 0; padding: 28px 16px; display: flex;"
                        " align-items: center; justify-content: center;")}
      [:svg {:xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 24 24" :width "48" :height "48"
             :fill "none" :stroke "var(--mtz-ink-soft)" :stroke-width "1.5"
             :stroke-linecap "round" :stroke-linejoin "round"}
       [:path {:d "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"}]
       [:polyline {:points "14 2 14 8 20 8"}]
       [:path {:d "M9 13h6"}]
       [:path {:d "M9 17h6"}]]]
     [:div {:style (str "border: 1px solid var(--mtz-rule); border-top: 0;"
                        " border-radius: 0 0 6px 6px; padding: 14px 16px;")}
      [:p {:class "mtz-card-meta" :style "margin: 0 0 4px;"} label]
      [:p {:class "mtz-arrow-link" :style "font-size: 11px;"} "Open PDF →"]]]))

(defn sermon-detail [{:keys [path-params] :as ctx}]
  (let [s (first (normalize (biff.sqlite/execute ctx {:select :*
                                                      :from   :sermon
                                                      :where  [:and
                                                               [:= :id (:id path-params)]
                                                               [:= :published 1]]})))]
    (if-not s
      {:status 404 :body "Sermon not found"}
      (base/page (str (:title s) " — Mount Zion UCC")
                 (list
                  [:section {:class "mtz-section" :style "padding-bottom: 0;"}
                   [:a {:class "mtz-arrow-link"
                        :href  "/sermons"
                        :style "font-size: 11px; margin-bottom: 24px; display: inline-flex;"}
                    "← All sermons"]
                   [:p {:class "mtz-card-meta" :style "margin: 16px 0 8px;"}
                    (str (or (format-date (:sermon_date s)) "")
                         (when (seq (:scripture s)) (str " · " (:scripture s))))]
                   [:h1 {:class "mtz-h1" :style "font-size: 48px; max-width: 760px; margin-bottom: 0;"}
                    (:title s)]]

                  ;; video
                  (when (:video_id s)
                    [:section {:class "mtz-section" :style "padding-top: 32px; padding-bottom: 32px;"}
                     [::hiccup/unsafe-html
                      (str "<iframe src=\"https://iframe.cloudflarestream.com/" (:video_id s)
                           "\" style=\"display:block;width:100%;max-width:900px;margin:0 auto;"
                           "aspect-ratio:16/9;border:none;border-radius:8px;\""
                           " allow=\"accelerometer; gyroscope; autoplay; encrypted-media; picture-in-picture\""
                           " allowfullscreen></iframe>")]])

                  ;; description
                  (when (seq (:description s))
                    [:section {:class "mtz-section" :style "padding-top: 0; padding-bottom: 32px;"}
                     [:p {:class "mtz-prose" :style "max-width: 680px; color: var(--mtz-ink-soft);"}
                      (:description s)]])

                  ;; PDFs
                  (when (or (seq (:bulletin_path s)) (seq (:presentation_path s)))
                    [:section {:class "mtz-section" :style "padding-top: 0; padding-bottom: 64px;"}
                     [:p {:class "mtz-kicker" :style "margin-bottom: 16px;"} "Downloads"]
                     [:div {:style "display: flex; gap: 16px; flex-wrap: wrap;"}
                      (pdf-card (:bulletin_path s) "Sunday Bulletin")
                      (pdf-card (:presentation_path s) "Presentation Slides")]]))))))

;; ---------------------------------------------------------------------------
;; Module
;; ---------------------------------------------------------------------------

(def module
  {:biff.ring/routes
   [["/sermons"
     ["" {:get sermon-list :name ::sermon-list}]
     ["/:id" {:get sermon-detail :name ::sermon-detail}]]]})
