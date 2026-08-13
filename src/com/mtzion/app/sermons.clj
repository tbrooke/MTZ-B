(ns com.mtzion.app.sermons
  (:require [clojure.string :as str]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.model.normalize :as norm]
            [com.mtzion.ui.base :as base]
            [lambdaisland.hiccup :as hiccup]))

(def ^:private normalize norm/snake-keys-all)

(defn- format-date [epoch]
  (when epoch
    (-> (java.time.Instant/ofEpochSecond epoch)
        (java.time.LocalDate/ofInstant java.time.ZoneOffset/UTC)
        (.format (java.time.format.DateTimeFormatter/ofPattern "MMMM d, yyyy")))))

(defn- scripture-line [s]
  (let [cw     (not-empty (:scripture_cw s))
        gospel (not-empty (:scripture_gospel s))]
    (cond
      (and cw gospel) (str cw " · " gospel)
      cw              cw
      gospel          gospel
      :else           nil)))

;; ---------------------------------------------------------------------------
;; Sunday Worship listing  /worship/sundays
;; ---------------------------------------------------------------------------

(def ^:private per-page 12)

(defn- sermon-row [s]
  [:article {:style "display:flex; gap:28px; align-items:flex-start; padding:32px 0; border-bottom:1px solid var(--mtz-rule);"}
   [:a {:href  (str "/worship/sundays/" (:id s))
        :style "flex:0 0 200px; display:block;"}
    [:div {:style (str "position:relative; width:200px; aspect-ratio:16/9;"
                       " border-radius:6px; overflow:hidden;"
                       " background:var(--mtz-stone); border:1px solid var(--mtz-rule);")}
     (if (:video_id s)
       (list
        [:img {:src   (str "https://videodelivery.net/" (:video_id s) "/thumbnails/thumbnail.jpg")
               :alt   (:title s)
               :style "position:absolute; inset:0; width:100%; height:100%; object-fit:cover;"}]
        [:div {:style "position:absolute; inset:0; display:flex; align-items:center; justify-content:center;"}
         [:div {:style "width:36px; height:36px; border-radius:50%; background:rgba(0,0,0,0.55); display:flex; align-items:center; justify-content:center; color:#fff; font-size:14px;"}
          "▶"]])
       [:div {:style "position:absolute; inset:0; display:flex; align-items:center; justify-content:center;"}
        [:div {:style "width:36px; height:36px; border-radius:50%; background:rgba(0,0,0,0.2); display:flex; align-items:center; justify-content:center; color:#fff; font-size:14px;"}
         "▶"]])]]
   [:div {:style "flex:1; min-width:0;"}
    [:p {:class "mtz-card-meta" :style "margin-bottom:4px;"}
     (str (or (format-date (:sermon_date s)) "")
          (when-let [sc (scripture-line s)] (str " · " sc)))]
    [:h3 {:class "mtz-h3" :style "font-size:20px; margin-bottom:8px; line-height:1.3;"}
     [:a {:href (str "/worship/sundays/" (:id s)) :style "color:inherit;"} (:title s)]]
    (when (seq (:description s))
      [:p {:style "color:var(--mtz-ink-soft); font-size:14px; margin:0 0 10px; max-width:520px; line-height:1.55;"}
       (let [d (:description s)]
         (if (> (count d) 140) (str (subs d 0 140) "…") d))])
    (when (or (seq (:bulletin_path s)) (seq (:presentation_path s)) (:video_id s))
      [:div {:style "display:flex; gap:16px; flex-wrap:wrap; font-size:12px;"}
       (when (:video_id s)
         [:a {:class "mtz-arrow-link" :href (str "/worship/sundays/" (:id s))
              :style "font-size:12px;"} "Watch"])
       (when (seq (:bulletin_path s))
         [:a {:class "mtz-arrow-link" :href (:bulletin_path s) :target "_blank"
              :rel "noopener" :style "font-size:12px;"} "Bulletin PDF"])
       (when (seq (:presentation_path s))
         [:a {:class "mtz-arrow-link" :href (:presentation_path s) :target "_blank"
              :rel "noopener" :style "font-size:12px;"} "Slides PDF"])])]])

(defn- pagination [page total-pages base-path]
  (when (> total-pages 1)
    [:div {:style "display:flex; gap:12px; align-items:center; padding-top:32px; justify-content:center;"}
     (if (> page 1)
       [:a {:class "mtz-btn mtz-btn--ghost" :href (str base-path "?page=" (dec page))} "← Newer"]
       [:span {:class "mtz-btn" :style "opacity:0.35; pointer-events:none;"} "← Newer"])
     [:span {:class "mtz-mono" :style "font-size:13px; color:var(--mtz-ink-mute);"}
      (str "Page " page " of " total-pages)]
     (if (< page total-pages)
       [:a {:class "mtz-btn mtz-btn--ghost" :href (str base-path "?page=" (inc page))} "Older →"]
       [:span {:class "mtz-btn" :style "opacity:0.35; pointer-events:none;"} "Older →"])]))

(defn sundays-list [{:keys [query-params] :as ctx}]
  (let [page        (max 1 (try (Integer/parseInt (get query-params "page" "1")) (catch Exception _ 1)))
        offset      (* (dec page) per-page)
        total       (:n (first (biff.sqlite/execute ctx ["SELECT COUNT(*) AS n FROM sermon WHERE published = 1"])))
        total-pages (max 1 (int (Math/ceil (/ (double (or total 0)) per-page))))
        sermons     (normalize (biff.sqlite/execute ctx {:select   :*
                                                         :from     :sermon
                                                         :where    [:= :published 1]
                                                         :order-by [[:sermon_date :desc]]
                                                         :limit    per-page
                                                         :offset   offset}))]
    (base/page ctx "Sunday Worship — Mount Zion UCC"
               (list
                [:section {:class "mtz-section"}
                 [:p {:class "mtz-kicker"} "Worship · 10:30 AM"]
                 [:h1 {:class "mtz-h1"} "Sunday Worship"]
                 [:p {:class "mtz-lede" :style "max-width:580px;"}
                  "Messages from Sunday worship at Mount Zion UCC."]
                 [:hr {:class "mtz-rule"}]
                 (if (seq sermons)
                   (list
                    [:div (map sermon-row sermons)]
                    (pagination page total-pages "/worship/sundays"))
                   [:p {:class "mtz-mute" :style "padding:48px 0;"}
                    "No sermons posted yet."])]))))

;; ---------------------------------------------------------------------------
;; Individual Sunday detail  /worship/sundays/:id
;; ---------------------------------------------------------------------------

(defn- pdf-card [path label]
  (when (seq path)
    [:a {:href path :target "_blank" :rel "noopener"
         :style "display:flex; flex-direction:column; gap:0; text-decoration:none; color:inherit; width:180px;"}
     [:div {:style (str "background:var(--mtz-bg-tint); border:1px solid var(--mtz-rule);"
                        " border-radius:6px 6px 0 0; padding:28px 16px; display:flex;"
                        " align-items:center; justify-content:center;")}
      [:svg {:xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 24 24" :width "48" :height "48"
             :fill "none" :stroke "var(--mtz-ink-soft)" :stroke-width "1.5"
             :stroke-linecap "round" :stroke-linejoin "round"}
       [:path {:d "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"}]
       [:polyline {:points "14 2 14 8 20 8"}]
       [:path {:d "M9 13h6"}]
       [:path {:d "M9 17h6"}]]]
     [:div {:style (str "border:1px solid var(--mtz-rule); border-top:0;"
                        " border-radius:0 0 6px 6px; padding:14px 16px;")}
      [:p {:class "mtz-card-meta" :style "margin:0 0 4px;"} label]
      [:p {:class "mtz-arrow-link" :style "font-size:11px;"} "Open PDF →"]]]))

(defn sunday-detail [{:keys [path-params] :as ctx}]
  (let [s (first (normalize (biff.sqlite/execute ctx {:select :*
                                                      :from   :sermon
                                                      :where  [:and
                                                               [:= :id (:id path-params)]
                                                               [:= :published 1]]})))]
    (if-not s
      {:status 404 :body "Sermon not found"}
      (base/page ctx (str (:title s) " — Mount Zion UCC")
                 (list
                  [:section {:class "mtz-section" :style "padding-bottom:0;"}
                   [:a {:class "mtz-arrow-link"
                        :href  "/worship/sundays"
                        :style "font-size:11px; margin-bottom:24px; display:inline-flex;"}
                    "← Sunday Worship"]
                   [:p {:class "mtz-card-meta" :style "margin:16px 0 4px;"}
                    (format-date (:sermon_date s))]
                   (when (scripture-line s)
                     [:p {:class "mtz-mono"
                          :style "font-size:12px; letter-spacing:0.08em; color:var(--mtz-ink-soft); margin:0 0 16px;"}
                      (str/upper-case (scripture-line s))])
                   [:h1 {:class "mtz-h1" :style "font-size:44px; max-width:760px; margin-bottom:0; line-height:1.15;"}
                    (:title s)]]

                  (when (:video_id s)
                    [:section {:class "mtz-section" :style "padding-top:32px; padding-bottom:32px;"}
                     [::hiccup/unsafe-html
                      (str "<iframe src=\"https://iframe.cloudflarestream.com/" (:video_id s)
                           "\" style=\"display:block;width:100%;max-width:900px;margin:0 auto;"
                           "aspect-ratio:16/9;border:none;border-radius:8px;\""
                           " allow=\"accelerometer; gyroscope; autoplay; encrypted-media; picture-in-picture\""
                           " allowfullscreen></iframe>")]])

                  (when (seq (:description s))
                    [:section {:class "mtz-section" :style "padding-top:0; padding-bottom:32px;"}
                     [:p {:class "mtz-prose" :style "max-width:680px; color:var(--mtz-ink-soft);"}
                      (:description s)]])

                  (when (or (seq (:bulletin_path s)) (seq (:presentation_path s)))
                    [:section {:class "mtz-section" :style "padding-top:0; padding-bottom:64px;"}
                     [:p {:class "mtz-kicker" :style "margin-bottom:16px;"} "Downloads"]
                     [:div {:style "display:flex; gap:16px; flex-wrap:wrap;"}
                      (pdf-card (:bulletin_path s) "Sunday Bulletin")
                      (pdf-card (:presentation_path s) "Presentation Slides")]]))))))

;; ---------------------------------------------------------------------------
;; Module
;; ---------------------------------------------------------------------------

(def module
  {:biff.ring/routes
   [["/worship/sundays"
     ["" {:get sundays-list :name ::sundays-list}]
     ["/:id" {:get sunday-detail :name ::sunday-detail}]]
    ;; Legacy redirect
    ["/sermons"
     ["" {:get (fn [_] {:status 301 :headers {"location" "/worship/sundays"}}) :name ::sermons-redirect}]
     ["/:id" {:get (fn [{:keys [path-params]}]
                     {:status 301 :headers {"location" (str "/worship/sundays/" (:id path-params))}})
              :name ::sermon-redirect}]]]})
