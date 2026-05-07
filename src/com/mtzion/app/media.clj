(ns com.mtzion.app.media
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.lib.middleware :refer [wrap-signed-in]]
            [com.mtzion.lib.ui :as ui]
            [com.mtzion.ui.admin :as adm]
            [hato.client :as http]))

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
                                          :content (:tempfile upload)
                                          :filename (:filename upload)
                                          :content-type (:content-type upload)}]
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

(defn- now-epoch [] (.getEpochSecond (java.time.Instant/now)))
(defn- new-id [] (str (random-uuid)))

(defn- epoch->date [epoch]
  (when epoch
    (-> (java.time.Instant/ofEpochSecond epoch) .toString (subs 0 10))))

(defn- parse-date-epoch [s]
  (when (seq s)
    (try (.getEpochSecond (java.time.Instant/parse (str s "T00:00:00Z")))
         (catch Exception _ nil))))

(defn- stream-embed-url [video-id]
  (str "https://iframe.cloudflarestream.com/" video-id))

(defn- stream-thumbnail-url [video-id]
  (str "https://videodelivery.net/" video-id "/thumbnails/thumbnail.jpg"))

(defn sermons-list [ctx]
  (let [rows (biff.sqlite/execute ctx {:select :* :from :sermon
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
                        [:thead [:tr [:th "Title"] [:th "Date"] [:th "Scripture"] [:th "Status"] [:th ""]]]
                        [:tbody
                         (for [r rows]
                           [:tr
                            [:td (:title r)]
                            [:td (or (epoch->date (:sermon_date r)) "—")]
                            [:td (or (:scripture r) "—")]
                            [:td (adm/badge (= 1 (:published r)))]
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
   (adm/field {:label "Scripture" :hint "e.g. John 3:16"}
              (adm/text-input {:name "scripture" :value (or (:scripture s) "")}))
   (adm/field {:label "Description / Pastor's Note"}
              [:textarea {:name "description" :class "adm-textarea" :rows "4"}
               (or (:description s) "")])
   (if (:video_id s)
     (adm/field {:label "Video"}
                [:div
                 [:p {:class "adm-hint" :style "margin-bottom:8px;"}
                  "Current video ID: " (:video_id s)]
                 [:label {:class "adm-label" :style "margin-top:12px;"} "Replace video (optional)"]
                 [:input {:type "file" :name "video" :class "adm-input"
                          :accept "video/*"}]])
     (adm/field {:label "Video File" :hint "Upload to Cloudflare Stream"}
                [:input {:type "file" :name "video" :class "adm-input"
                         :accept "video/*"}]))
   (adm/field {:label "Status"}
              [:label {:class "adm-check-row"}
               [:input {:type "checkbox" :name "published" :value "1"
                        :checked (not= 0 (:published s 1))}]
               "Published"])
   (adm/submit-row {:cancel-href "/admin/sermons"})])

(defn sermons-new [_ctx]
  (adm/admin-page "New Sermon"
                  (adm/top-bar)
                  [:div {:class "adm-content"}
                   (adm/page-header "New Sermon" "/admin/sermons")
                   (sermon-form "/admin/sermons" nil (ui/anti-forgery-field))]))

(defn sermons-edit [{:keys [path-params] :as ctx}]
  (let [s (first (biff.sqlite/execute ctx {:select :* :from :sermon
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

(defn- upload-to-stream [ctx video-upload title]
  (when (:tempfile video-upload)
    (let [resp (http/post (cf-url ctx "/stream")
                          {:headers   (cf-headers ctx)
                           :multipart [{:name         "file"
                                        :content      (:tempfile video-upload)
                                        :filename     (:filename video-upload)
                                        :content-type (:content-type video-upload)}
                                       {:name    "meta"
                                        :content (json/generate-string {:name title})
                                        :content-type "application/json"}]
                           :as        :string})
          body (json/parse-string (:body resp) true)]
      (get-in body [:result :uid]))))

(defn sermons-create [{:keys [params] :as ctx}]
  (let [title    (or (:title params) "")
        video-id (upload-to-stream ctx (:video params) title)]
    (biff.sqlite/execute ctx
                         {:insert-into :sermon
                          :values [{:id          (new-id)
                                    :title       title
                                    :sermon_date (parse-date-epoch (:sermon_date params))
                                    :scripture   (or (:scripture params) "")
                                    :description (or (:description params) "")
                                    :video_id    video-id
                                    :published   (if (:published params) 1 0)
                                    :created_at  (now-epoch)}]}))
  {:status 303 :headers {"location" "/admin/sermons"}})

(defn sermons-update [{:keys [params path-params] :as ctx}]
  (let [title    (or (:title params) "")
        new-vid  (upload-to-stream ctx (:video params) title)
        existing (first (biff.sqlite/execute ctx {:select [:video_id] :from :sermon
                                                  :where [:= :id (:id path-params)]}))]
    (biff.sqlite/execute ctx
                         {:update :sermon
                          :set    {:title       title
                                   :sermon_date (parse-date-epoch (:sermon_date params))
                                   :scripture   (or (:scripture params) "")
                                   :description (or (:description params) "")
                                   :video_id    (or new-vid (:video_id existing))
                                   :published   (if (:published params) 1 0)}
                          :where  [:= :id (:id path-params)]}))
  {:status 303 :headers {"location" "/admin/sermons"}})

(defn sermons-delete [{:keys [path-params] :as ctx}]
  (biff.sqlite/execute ctx {:delete-from :sermon :where [:= :id (:id path-params)]})
  {:status 303 :headers {"location" "/admin/sermons"}})

;; ---------------------------------------------------------------------------
;; Module
;; ---------------------------------------------------------------------------

(def module
  {:biff.ring/routes
   [["/admin" {:middleware [[wrap-signed-in]]}
     ["/upload" {:post upload-image :name ::upload-image}]
     ["/sermons"
      ["" {:get sermons-list :post sermons-create :name ::sermons}]
      ["/new" {:get sermons-new :name ::sermons-new :conflicting true}]
      ["/:id/edit" {:get sermons-edit :name ::sermons-edit}]
      ["/:id" {:post sermons-update :name ::sermons-update :conflicting true}]
      ["/:id/delete" {:post sermons-delete :name ::sermons-delete}]]]]})
