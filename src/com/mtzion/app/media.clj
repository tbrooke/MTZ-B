(ns com.mtzion.app.media
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.lib.middleware :refer [wrap-signed-in]]
            [com.mtzion.lib.r2 :as r2]
            [com.mtzion.lib.ui :as ui]
            [com.mtzion.model.content :as content]
            [com.mtzion.model.normalize :as norm]
            [com.mtzion.ui.admin :as adm]
            [hato.client :as http]
            [lambdaisland.hiccup :as hiccup]))

;; ---------------------------------------------------------------------------
;; Cloudflare helpers
;; ---------------------------------------------------------------------------

(defn- cf-url [ctx path]
  (str "https://api.cloudflare.com/client/v4/accounts/"
       (:cf/account-id ctx) path))

(defn- cf-headers [ctx]
  {"Authorization" (str "Bearer " ((:cf/api-token ctx)))})

(defn- image-delivery-url [ctx image-id variant]
  (str "https://imagedelivery.net/" (:cf/images-hash ctx) "/" image-id "/" variant))

;; ---------------------------------------------------------------------------
;; Image upload (used by Tiptap editor)
;; ---------------------------------------------------------------------------

(defn upload-image [{:keys [params] :as ctx}]
  (let [upload (:file params)]
    (if-not (:tempfile upload)
      {:status 400 :body "No file provided"}
      (let [resp (http/post (cf-url ctx "/images/v1")
                            {:headers   (cf-headers ctx)
                             :multipart [{:name    "file"
                                          :content (java.io.FileInputStream. (:tempfile upload))
                                          :filename (:filename upload)
                                          :content-type (:content-type upload)}
                                         {:name    "metadata"
                                          :content (json/generate-string {:category "content"})}]
                             :as        :string})
            body (json/parse-string (:body resp) true)]
        (if (get-in body [:result :id])
          {:status  200
           :headers {"Content-Type" "application/json"}
           :body    (json/generate-string
                     {:url (image-delivery-url ctx (get-in body [:result :id]) "public")})}
          {:status 500
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Cloudflare upload failed"
                                        :detail (get-in body [:errors 0 :message])})})))))

;; ---------------------------------------------------------------------------
;; Sermons (Cloudflare Stream)
;; ---------------------------------------------------------------------------

(def ^:private now-epoch norm/now-epoch)

(defn- exec
  "Wrapper around biff.sqlite/execute that returns rows with unqualified
  snake_case keys, matching the DB column names used throughout this file."
  [ctx honey]
  (norm/snake-keys-all (biff.sqlite/execute ctx honey)))
(defn- new-id [] (str (random-uuid)))

(defn- save-sermon-pdf [ctx date-str param-file suffix]
  (when-let [f (:tempfile param-file)]
    (when (and (seq (:filename param-file)) (pos? (.length f)))
      (let [key (str "sermons/" date-str "/" suffix)]
        (with-open [in (java.io.FileInputStream. f)]
          (r2/put! ctx key "application/pdf" in (.length f)))
        (r2/public-url ctx key)))))

(def ^:private epoch->date      norm/epoch->date-str)
(def ^:private parse-date-epoch norm/local-date->epoch)

(defn- stream-embed-url [video-id]
  (str "https://iframe.cloudflarestream.com/" video-id))

(defn- stream-thumbnail-url [video-id]
  (str "https://videodelivery.net/" video-id "/thumbnails/thumbnail.jpg"))

(defn sermons-list [ctx]
  (let [rows (exec ctx {:select :* :from :sermon
                        :order-by [[:sermon_date :desc]]})]
    (adm/admin-page "Sermons"
                    (adm/top-bar)
                    [:div {:class "adm-content"}
                     (adm/page-header "Sermons" "/admin")
                     [:div {:style "margin-bottom:20px;"}
                      [:a {:href "/admin/sermons/new" :class "mtz-btn mtz-btn--primary"} "+ New Sermon"]]
                     (if (empty? rows)
                       [:p {:class "adm-hint"} "No sermons yet."]
                       [:table {:class "adm-table"}
                        [:thead [:tr [:th "Title"] [:th "Date"] [:th "Call to Worship"] [:th "Gospel"] [:th "Status"] [:th ""]]]
                        [:tbody
                         (for [r rows]
                           [:tr
                            [:td (:title r)]
                            [:td (or (epoch->date (:sermon_date r)) "—")]
                            [:td (or (:scripture_cw r) "—")]
                            [:td (or (:scripture_gospel r) "—")]
                            [:td (adm/badge (= content/published (:status r)))]
                            [:td
                             [:div {:class "adm-actions"}
                              [:a {:href (str "/admin/sermons/" (:id r) "/edit") :class "adm-link"} "Edit"]
                              (adm/delete-form (str "/admin/sermons/" (:id r) "/delete")
                                               (ui/anti-forgery-field))]]])]])])))

(defn- sermon-form [action s csrf]
  [:form {:method "post" :action action :class "adm-form" :enctype "multipart/form-data"}
   csrf
   (adm/field {:label "Title"}
              (adm/text-input {:name "title" :value (or (:title s) "") :required "true"}))
   (adm/field {:label "Date"}
              [:input {:type "date" :name "sermon_date" :class "adm-input"
                       :value (or (epoch->date (:sermon_date s)) "")}])
   (adm/field {:label "Series" :hint "Slug for a multi-week theme, e.g. apostles-creed-2026 — leave blank for standalone sermons"}
              (adm/text-input {:name "series" :value (or (:series s) "")}))
   (adm/field {:label "Call to Worship" :hint "e.g. Acts 7:54–60"}
              (adm/text-input {:name "scripture_cw" :value (or (:scripture_cw s) "")}))
   (adm/field {:label "Gospel Reading" :hint "e.g. John 14:8–14"}
              (adm/text-input {:name "scripture_gospel" :value (or (:scripture_gospel s) "")}))
   (adm/field {:label "Description / Pastor's Note"}
              [:textarea {:name "description" :class "adm-textarea" :rows "4"}
               (or (:description s) "")])
   (adm/field {:label "Video" :hint "Uploads directly to Cloudflare — no file size limit"}
              [:div {:id "sermon-video-widget" :style "display:flex; flex-direction:column; gap:8px;"}
               [:input {:type "file" :accept "video/*" :id "sermon-video-input"
                        :style "position:absolute; opacity:0; pointer-events:none; width:1px; height:1px; overflow:hidden;"}]
               [:div {:style "display:flex; align-items:center; gap:10px; flex-wrap:wrap;"}
                [:button {:type "button" :id "sermon-video-btn" :class "mtz-btn mtz-btn--ghost"}
                 (if (:video_id s) "Replace Video" "Choose Video File")]
                [:span {:id "sermon-video-filename"
                        :style "font-size:13px; color:var(--mtz-ink-soft);"}]]
               [:div {:id "sermon-video-progress" :style "display:none; margin-top:4px;"}
                [:div {:style "height:5px; background:var(--mtz-stone); border-radius:3px; overflow:hidden; margin-bottom:4px;"}
                 [:div {:id "sermon-video-bar"
                        :style "height:100%; width:0; background:#5A7257; border-radius:3px; transition:width 0.15s;"}]]
                [:span {:id "sermon-video-pct" :style "font-size:12px; color:var(--mtz-ink-soft);"} "0%"]]
               [:p {:id "sermon-video-status"
                    :style "font-size:13px; color:var(--mtz-ink-soft); margin:0;"} ""]
               [:details {:style "margin-top:4px;"}
                [:summary {:class "adm-hint" :style "cursor:pointer; user-select:none;"}
                 "Paste Cloudflare video ID manually"]
                [:div {:style "margin-top:8px; display:flex; flex-direction:column; gap:4px;"}
                 [:input {:type "text" :name "video_id" :id "sermon-video-id"
                          :class "adm-input"
                          :placeholder "e.g. 0fcc55133ed33f986a1a2a185f3c45e5"
                          :value (or (:video_id s) "")}]
                 [:p {:class "adm-hint" :style "margin:0;"}
                  "The video ID is set automatically after upload. "
                  "Only paste here if the automatic upload completed but the ID was lost."]]]])
   (adm/field {:label "Bulletin PDF"
               :hint  (if (:bulletin_path s)
                        (str "Current: " (:bulletin_path s) " — upload to replace")
                        "Upload the Sunday bulletin (PDF)")}
              [:input {:type "file" :name "bulletin" :class "adm-input" :accept ".pdf,application/pdf"}])
   (adm/field {:label "Presentation PDF"
               :hint  (if (:presentation_path s)
                        (str "Current: " (:presentation_path s) " — upload to replace")
                        "Upload the presentation slides (PDF)")}
              [:input {:type "file" :name "presentation" :class "adm-input" :accept ".pdf,application/pdf"}])
   (adm/field {:label "Status"}
              [:label {:class "adm-check-row"}
               [:input {:type "checkbox" :name "published" :value "1"
                        :checked (not= content/draft (:status s content/published))}]
               "Published"])
   (adm/submit-row {:cancel-href "/admin/sermons"})])

(defn sermons-new [_ctx]
  (adm/admin-page "New Sermon"
                  (adm/top-bar)
                  [:div {:class "adm-content"}
                   [:div {:class "adm-section-header"}
                    [:h1 {:class "adm-page-title" :style "margin:0;"} "New Sermon"]
                    [:a {:href "/admin/sermons" :class "adm-link"} "View all sermons →"]]
                   (sermon-form "/admin/sermons" nil (ui/anti-forgery-field))]))

(defn sermons-edit [{:keys [path-params] :as ctx}]
  (let [s (first (exec ctx {:select :* :from :sermon
                            :where [:= :id (:id path-params)]}))]
    (if s
      (adm/admin-page "Edit Sermon"
                      (adm/top-bar)
                      [:div {:class "adm-content"}
                       (adm/page-header "Edit Sermon" "/admin/sermons")
                       (when (:video_id s)
                         [:div {:style "margin-bottom:20px;"}
                          [:iframe {:src (stream-embed-url (:video_id s))
                                    :width "560" :height "315"
                                    :allow "accelerometer; gyroscope; autoplay; encrypted-media; picture-in-picture;"
                                    :allowfullscreen "true"
                                    :style "border:none; border-radius:4px;"}]])
                       (sermon-form (str "/admin/sermons/" (:id s)) s (ui/anti-forgery-field))])
      {:status 404 :body "Not found"})))

(defn stream-upload-slot [{:keys [params] :as ctx}]
  (let [title  (or (:title params) "Sermon")
        expiry (-> (java.time.Instant/now) (.plusSeconds 7200) .toString)
        resp   (http/post (cf-url ctx "/stream/direct_upload")
                          {:headers (assoc (cf-headers ctx) "Content-Type" "application/json")
                           :body    (json/generate-string {:maxDurationSeconds 21600
                                                           :expiry             expiry
                                                           :meta               {:name title}})
                           :as      :string})
        body   (json/parse-string (:body resp) true)]
    (if (get-in body [:result :uid])
      {:status  200
       :headers {"Content-Type" "application/json"}
       :body    (json/generate-string {:uid       (get-in body [:result :uid])
                                       :uploadUrl (get-in body [:result :uploadURL])})}
      {:status  500
       :headers {"Content-Type" "application/json"}
       :body    (json/generate-string {:error (get-in body [:errors 0 :message] "Upload slot request failed")})})))

(defn sermons-create [{:keys [params] :as ctx}]
  (let [title         (or (:title params) "")
        date-str      (or (:sermon_date params) "")
        video-id      (not-empty (:video_id params))
        bulletin-path (save-sermon-pdf ctx date-str (:bulletin params) "bulletin.pdf")
        pres-path     (save-sermon-pdf ctx date-str (:presentation params) "presentation.pdf")]
    (exec ctx
          {:insert-into :sermon
           :values [{:id                (new-id)
                     :title             title
                     :sermon_date       (parse-date-epoch date-str)
                     :scripture_cw      (not-empty (str/trim (or (:scripture_cw params) "")))
                     :scripture_gospel  (not-empty (str/trim (or (:scripture_gospel params) "")))
                     :series            (not-empty (str/trim (or (:series params) "")))
                     :description       (or (:description params) "")
                     :video_id          video-id
                     :bulletin_path     bulletin-path
                     :presentation_path pres-path
                     :published         (if (:published params) 1 0)
                     :status            (if (:published params) content/published content/draft)
                     :created_at        (now-epoch)}]}))
  {:status 303 :headers {"location" "/admin/sermons"}})

(defn sermons-update [{:keys [params path-params] :as ctx}]
  (let [title         (or (:title params) "")
        date-str      (or (:sermon_date params) "")
        new-vid       (not-empty (:video_id params))
        existing      (first (exec ctx {:select [:video_id :bulletin_path :presentation_path]
                                        :from :sermon
                                        :where [:= :id (:id path-params)]}))
        bulletin-path (or (save-sermon-pdf ctx date-str (:bulletin params) "bulletin.pdf")
                          (:bulletin_path existing))
        pres-path     (or (save-sermon-pdf ctx date-str (:presentation params) "presentation.pdf")
                          (:presentation_path existing))]
    (exec ctx
          {:update :sermon
           :set    {:title             title
                    :sermon_date       (parse-date-epoch date-str)
                    :scripture_cw      (not-empty (str/trim (or (:scripture_cw params) "")))
                    :scripture_gospel  (not-empty (str/trim (or (:scripture_gospel params) "")))
                    :series            (not-empty (str/trim (or (:series params) "")))
                    :description       (or (:description params) "")
                    :video_id          (or new-vid (:video_id existing))
                    :bulletin_path     bulletin-path
                    :presentation_path pres-path
                    :published         (if (:published params) 1 0)
                    :status            (if (:published params) content/published content/draft)}
           :where  [:= :id (:id path-params)]}))
  {:status 303 :headers {"location" "/admin/sermons"}})

(defn sermons-delete [{:keys [path-params] :as ctx}]
  (exec ctx {:delete-from :sermon :where [:= :id (:id path-params)]})
  {:status 303 :headers {"location" "/admin/sermons"}})

;; ---------------------------------------------------------------------------
;; Image library (Cloudflare Images browser)
;; ---------------------------------------------------------------------------

(defn- cf-images-list [ctx page]
  (let [resp (http/get (cf-url ctx "/images/v1")
                       {:headers      (cf-headers ctx)
                        :query-params {"per_page" 100 "page" page "sort_order" "desc"}
                        :as           :string})
        body (json/parse-string (:body resp) true)]
    (get-in body [:result :images])))

(defn- variant-url [img suffix fallback]
  (or (some #(when (str/ends-with? % (str "/" suffix)) %) (:variants img))
      fallback))

(defn- img-meta [img]
  (let [m (:meta img)]
    (cond
      (map? m)    m
      (string? m) (try (json/parse-string m true) (catch Exception _ {}))
      :else       {})))

(defn- image-card [ctx img insert-mode?]
  (let [meta         (img-meta img)
        any-url      (first (:variants img))
        fallback-url (or any-url (image-delivery-url ctx (:id img) "public"))
        thumb-url    (variant-url img "thumbnail" fallback-url)
        public-url   (variant-url img "public" fallback-url)
        uploaded     (some-> (:uploaded img) (subs 0 10))
        display-name (or (:label meta)
                         (let [f (:filename img)]
                           (when-not (str/starts-with? (or f "") "ring-multipart") f))
                         "Untitled")
        display-date (or (:date meta) uploaded)]
    (if insert-mode?
      [:button {:type          "button"
                :class         "img-browser-item"
                :data-img-url  public-url
                :title         (str display-name " · " display-date)}
       [:img {:src thumb-url :alt display-name :class "img-browser-thumb" :loading "lazy"}]
       [:span {:class "img-browser-name"} display-name]]
      [:div {:class "img-browser-item"}
       [:img {:src thumb-url :alt display-name :class "img-browser-thumb" :loading "lazy"}]
       [:span {:class "img-browser-name"} display-name]
       [:span {:class "img-browser-date"} display-date]])))

(defn- browse-url [page insert? category partial?]
  (str "/admin/images/browse?page=" page
       (when insert?  "&insert=1")
       (when partial? "&partial=1")
       (when (seq category) (str "&category=" category))))

(defn- browse-grid [ctx page insert? category]
  (let [all-imgs (cf-images-list ctx page)
        images   (if (seq category)
                   (filter #(= category (get-in % [:meta :category])) all-imgs)
                   all-imgs)]
    (list
     [:div {:class "img-browser-grid"}
      (if (empty? images)
        [:p {:style "padding: 24px; color: var(--mtz-ink-soft);"} "No images found."]
        (map #(image-card ctx % insert?) images))]
     (when (= (count all-imgs) 100)
       [:div {:style "padding: 16px; text-align: center;"}
        [:button {:type      "button"
                  :class     "adm-link"
                  :hx-get    (browse-url (inc page) insert? category true)
                  :hx-target ".img-browser-grid"
                  :hx-swap   "beforeend"}
         "Load more"]]))))

(defn images-browse [{:keys [query-params] :as ctx}]
  (let [page     (Integer/parseInt (get query-params "page" "1"))
        insert?  (= "1" (get query-params "insert"))
        partial? (= "1" (get query-params "partial"))
        category (get query-params "category" "")]
    {:status  200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body
     (hiccup/render
      (if partial?
        (browse-grid ctx page insert? category)
        [:div {:class "img-browser-wrap"}
         (when insert?
           [:div {:class "img-browser-header"}
            [:span {:class "img-browser-title"} "Image Library"]
            [:select {:class     "img-browser-filter adm-select"
                      :name      "category"
                      :hx-get    (browse-url 1 insert? nil true)
                      :hx-target ".img-browser-grid"
                      :hx-swap   "outerHTML"
                      :hx-include "this"}
             [:option {:value ""} "All images"]
             [:option {:value "content"} "Content"]
             [:option {:value "gallery"} "Gallery"]]
            [:button {:type "button" :class "img-browser-close"
                      :onclick "document.getElementById('mtz-img-browser').close()"} "✕"]])
         (browse-grid ctx page insert? category)]))}))

(defn images-page [ctx]
  (adm/admin-page "Image Library"
                  (adm/top-bar)
                  [:div {:class "adm-content"}
                   (adm/page-header "Image Library" "/admin")
                   [:p {:class "adm-hint" :style "margin-bottom:20px;"}
                    "Images hosted on Cloudflare. Upload new images via the Tiptap editor or the Files section."]
                   [:div {:style "overflow:auto;"}
                    (let [images (cf-images-list ctx 1)]
                      (if (empty? images)
                        [:p {:class "adm-hint"} "No images yet."]
                        [:div {:class "img-browser-grid img-browser-grid--admin"}
                         (map #(image-card ctx % false) images)]))]]))

;; ---------------------------------------------------------------------------
;; Module
;; ---------------------------------------------------------------------------

;; ---------------------------------------------------------------------------
;; Image upload (standalone, from dashboard)
;; ---------------------------------------------------------------------------

(defn images-new [{:keys [query-params]}]
  (let [category  (get query-params "category" "photo")
        cat-label (if (= category "graphic") "Graphic" "Photo")
        uploaded? (= "1" (get query-params "uploaded"))]
    (adm/admin-page (str "Upload " cat-label)
                    (adm/top-bar)
                    [:div {:class "adm-content"}
                     [:div {:class "adm-section-header"}
                      [:h1 {:class "adm-page-title" :style "margin:0;"}
                       (str "Upload " cat-label)]
                      [:a {:href "/admin/images" :class "adm-link"} "View library →"]]
                     (when uploaded?
                       [:p {:class "adm-hint" :style "color:#5A7257;margin-bottom:16px;"}
                        "✓ Image uploaded successfully."])
                     [:form {:method "post" :action "/admin/images/upload"
                             :class "adm-form" :enctype "multipart/form-data"}
                      (ui/anti-forgery-field)
                      [:input {:type "hidden" :name "category" :value category}]
                      (adm/field {:label "Image File"}
                                 [:input {:type "file" :name "file" :class "adm-input"
                                          :required "true" :accept "image/*"}])
                      (adm/field {:label "Label" :hint "Short description, e.g. Easter Sunday 2026"}
                                 (adm/text-input {:name "label"
                                                  :placeholder "Easter Sunday 2026"}))
                      (adm/field {:label "Date" :hint "When the photo was taken"}
                                 [:input {:type "date" :name "photo_date" :class "adm-input"}])
                      (adm/submit-row {:label "Upload" :cancel-href "/admin/images"})]])))

(defn images-upload [{:keys [params] :as ctx}]
  (let [upload   (:file params)
        category (or (:category params) "photo")
        label    (str/trim (or (:label params) ""))
        date     (or (:photo_date params) "")]
    (if-not (:tempfile upload)
      {:status 303 :headers {"location" (str "/admin/images/new?category=" category)}}
      (let [metadata (cond-> {:category category}
                       (seq label) (assoc :label label)
                       (seq date)  (assoc :date date))
            resp     (http/post (cf-url ctx "/images/v1")
                                {:headers   (cf-headers ctx)
                                 :multipart [{:name         "file"
                                              :content      (java.io.FileInputStream. (:tempfile upload))
                                              :filename     (:filename upload)
                                              :content-type (:content-type upload)}
                                             {:name    "metadata"
                                              :content (json/generate-string metadata)}]
                                 :as        :string})
            body     (json/parse-string (:body resp) true)]
        (if (get-in body [:result :id])
          {:status 303 :headers {"location" (str "/admin/images/new?category=" category "&uploaded=1")}}
          {:status 303 :headers {"location" (str "/admin/images/new?category=" category)}})))))

(def module
  {:biff.ring/routes
   [["/admin" {:middleware [[wrap-signed-in]]}
     ["/upload" {:post upload-image :name ::upload-image}]
     ["/images"
      ["" {:get images-page :name ::images}]
      ["/new" {:get images-new :name ::images-new}]
      ["/upload" {:post images-upload :name ::images-upload}]
      ["/browse" {:get images-browse :name ::images-browse}]]
     ["/sermons"
      ["" {:get sermons-list :post sermons-create :name ::sermons}]
      ["/new" {:get sermons-new :name ::sermons-new :conflicting true}]
      ["/upload-slot" {:post stream-upload-slot :name ::stream-upload-slot :conflicting true}]
      ["/:id/edit" {:get sermons-edit :name ::sermons-edit}]
      ["/:id" {:post sermons-update :name ::sermons-update :conflicting true}]
      ["/:id/delete" {:post sermons-delete :name ::sermons-delete}]]]]})
