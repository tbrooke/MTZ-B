(ns com.mtzion.app.library
  "The Media pane — upload before you know where a picture is going, then find
  it again later.

  Two things the old /admin/images screen could not do. Uploading was only
  reachable from inside something else (the Tiptap image button, or a one-file
  form), so a set of photos from an event had nowhere to land until somebody had
  already decided what to write about them. And there was no local record of an
  image at all — the screen called Cloudflare on every render — so nothing could
  be searched, grouped, or made into a gallery."
  (:require [clojure.string :as str]
            [com.mtzion.lib.cloudflare :as cf]
            [com.mtzion.lib.middleware :refer [wrap-signed-in]]
            [com.mtzion.lib.ui :as ui]
            [com.mtzion.model.media :as media]
            [com.mtzion.model.normalize :as normalize]
            [com.mtzion.ui.console :as con]))

(defn- date-str [epoch] (normalize/epoch->date-str epoch))

(defn- qs [{:keys [album category q]}]
  (let [parts (cond-> []
                (= :none album)  (conj "album=none")
                (string? album)  (conj (str "album=" album))
                (seq category)   (conj (str "category=" category))
                (seq q)          (conj (str "q=" q)))]
    (if (seq parts) (str "?" (str/join "&" parts)) "")))

(defn- filters-of [{:keys [query-params]}]
  {:album    (case (get query-params "album")
               nil    nil
               "none" :none
               (get query-params "album"))
   :category (get query-params "category")
   :q        (get query-params "q")})

;; ---------------------------------------------------------------------------
;; Left rail — albums
;; ---------------------------------------------------------------------------

(defn- rail [ctx {:keys [album category q] :as f}]
  (let [albums   (media/albums ctx)
        unfiled  (media/unfiled-count ctx)
        link     (fn [a] (str "/console/media" (qs (assoc f :album a))))]
    [:aside {:class "con-list"}
     [:div {:class "con-list-head"}
      [:h1 {:class "con-list-title"} "Media"]
      [:a {:href "/console/media/upload" :class "con-btn con-btn--primary"} "+ Upload"]]

     [:div {:class "con-chips"}
      [:a {:href (str "/console/media" (qs (assoc f :category nil)))
           :class (str "con-chip" (when-not (seq category) " is-active"))} "All"]
      (for [[v label] media/categories]
        [:a {:href (str "/console/media" (qs (assoc f :category v)))
             :class (str "con-chip" (when (= v category) " is-active"))} label])]

     [:form {:method "get" :action "/console/media" :class "con-search"}
      (when (= :none album)  [:input {:type "hidden" :name "album" :value "none"}])
      (when (string? album)  [:input {:type "hidden" :name "album" :value album}])
      (when (seq category)   [:input {:type "hidden" :name "category" :value category}])
      [:input {:type "search" :name "q" :value (or q "")
               :class "con-input con-input--search" :placeholder "Search labels…"}]]

     [:div {:class "con-rows"}
      [:a {:href (str "/console/media" (qs (assoc f :album nil)))
           :class (str "con-row" (when (nil? album) " is-selected"))}
       [:span {:class "con-row-main"}
        [:span {:class "con-row-title"} "Everything"]
        [:span {:class "con-row-meta"} (str (media/total ctx) " images")]]]

      (when (pos? unfiled)
        [:a {:href (link :none) :class (str "con-row" (when (= :none album) " is-selected"))}
         [:span {:class "con-row-main"}
          [:span {:class "con-row-title"} "Not in an album"]
          [:span {:class "con-row-meta"} (str unfiled " images")]]])

      (when (seq albums)
        [:p {:class "con-rail-heading"} "Albums"])
      (for [{a :album n :n} albums]
        [:a {:href (link a) :class (str "con-row" (when (= a album) " is-selected"))}
         [:span {:class "con-row-main"}
          [:span {:class "con-row-title"} a]
          [:span {:class "con-row-meta"} (str n (if (= 1 n) " image" " images"))]]])]]))

;; ---------------------------------------------------------------------------
;; Grid and detail
;; ---------------------------------------------------------------------------

(defn- tile [ctx img selected-id f]
  [:a {:href  (str "/console/media/" (:id img) (qs f))
       :class (str "con-tile" (when (= (:id img) selected-id) " is-selected"))
       :title (or (not-empty (:label img)) (:id img))}
   [:img {:src (media/url ctx (:id img) "public") :alt (or (:alt_text img) "")
          :loading "lazy" :class "con-tile-img"}]
   [:span {:class "con-tile-label"} (or (not-empty (:label img)) "Untitled")]])

(defn- detail [ctx img f]
  [:section {:class "con-editor"}
   [:form {:method "post" :action (str "/console/media/" (:id img)) :class "con-form"}
    (ui/anti-forgery-field)
    [:div {:class "con-editor-bar"}
     [:span {:class "con-kind"} (or (:album img) "Not in an album")]
     [:div {:class "con-editor-bar-right"}
      [:button {:type "submit" :class "con-btn con-btn--primary"} "Save"]
      [:button {:type "submit" :class "con-btn con-btn--danger"
                :formaction (str "/console/media/" (:id img) "/delete")
                :formnovalidate "true"
                :onclick "return confirm('Delete this image from Cloudflare? Anything using it will break. This cannot be undone.')"}
       "Delete"]]]

    [:img {:src (media/url ctx (:id img) "public") :alt (or (:alt_text img) "")
           :class "con-detail-img"}]

    [:div {:class "con-details-grid con-details-grid--open"}
     (con/field {:label "Label" :hint "What this is — how you will find it again"}
                (con/text-input {:name "label" :value (or (:label img) "")}))
     (con/field {:label "Album" :hint "Group a set together — a section can then show the whole album"}
                (con/text-input {:name "album" :value (or (:album img) "")
                                 :list "con-album-list" :placeholder "vbs-2026"}))
     (con/field {:label "Kind"}
                (con/select-input {:name "category"} media/categories (:category img "photo")))
     (con/field {:label "Date taken"}
                [:input {:type "date" :name "taken_on" :class "con-input"
                         :value (or (date-str (:taken_on img)) "")}])
     (con/field {:label "Alt text" :hint "Described for someone who cannot see it" :wide? true}
                (con/text-input {:name "alt_text" :value (or (:alt_text img) "")}))]

    [:datalist {:id "con-album-list"}
     (for [{a :album} (media/albums ctx)] [:option {:value a}])]]

   [:div {:class "con-idbox"}
    [:span {:class "con-label"} "Cloudflare image ID"]
    [:input {:class "con-input con-idbox-input" :readonly "true" :value (:id img)
             :onclick "this.select()"}]
    [:span {:class "con-hint"}
     "Paste this into a section's Image field. Clicking selects it."]]])

;; ---------------------------------------------------------------------------
;; Pages
;; ---------------------------------------------------------------------------

(defn- render [ctx f selected right]
  (let [imgs (media/ls ctx f)]
    (con/page "Media" (con/nav ctx :media)
              [:div {:class "con-pane"}
               (rail ctx f)
               [:div {:class "con-media"}
                (if (empty? imgs)
                  (con/empty-state
                   (if (pos? (media/total ctx)) "Nothing matches that" "No images yet")
                   [:p (if (pos? (media/total ctx))
                         "Try a different album, kind, or search."
                         "Upload a set now and decide where they go later — that is what albums are for.")]
                   [:a {:href "/console/media/upload" :class "con-btn con-btn--primary"} "+ Upload"]
                   [:form {:method "post" :action "/console/media/sync" :style "margin-top:14px;"}
                    (ui/anti-forgery-field)
                    [:button {:type "submit" :class "con-btn con-btn--ghost"}
                     "Index what is already in Cloudflare"]])
                  [:div {:class "con-grid"}
                   (for [i imgs] (tile ctx i selected f))])]
               right])))

(defn media [{:keys [path-params] :as ctx}]
  (let [f   (filters-of ctx)
        id  (:id path-params)
        img (when id (media/get-one ctx id))]
    (if (and id (nil? img))
      {:status 404 :body "No such image"}
      (render ctx f (:id img)
              (if img
                (detail ctx img f)
                (con/empty-state
                 "Pick an image"
                 [:p "Or upload a batch — one album, one date, as many files as you like."]
                 [:a {:href "/console/media/upload" :class "con-btn con-btn--primary"} "+ Upload"]))))))

;; ---------------------------------------------------------------------------
;; Upload
;; ---------------------------------------------------------------------------

(defn upload-form [ctx]
  (con/page "Upload" (con/nav ctx :media)
            [:div {:class "con-single"}
             [:div {:class "con-list-head con-list-head--page"}
              [:h1 {:class "con-list-title"} "Upload images"]
              [:a {:href "/console/media" :class "con-btn con-btn--quiet"} "Back to Media"]]
             [:p {:class "con-hint con-hint--block"}
              "Everything in one go gets the same album, kind and date — so a set of
               photos from one event stays together. You can change any of it afterwards."]
             [:form {:method "post" :action "/console/media/upload"
                     :class "con-form" :enctype "multipart/form-data"}
              (ui/anti-forgery-field)
              [:div {:class "con-details-grid con-details-grid--open"}
               [:div {:class "con-field con-field--wide"}
                [:label {:class "con-label"} "Files"]
                [:input {:type "file" :name "files" :multiple "true" :required "true"
                         :accept "image/*" :class "con-input con-file"}]
                [:span {:class "con-hint"} "Pick as many as you like."]]
               (con/field {:label "Album" :hint "Leave blank to file them later"}
                          (con/text-input {:name "album" :list "con-album-list"
                                           :placeholder "vbs-2026"}))
               (con/field {:label "Kind"}
                          (con/select-input {:name "category"} media/categories "photo"))
               (con/field {:label "Date taken"}
                          [:input {:type "date" :name "taken_on" :class "con-input"}])]
              [:datalist {:id "con-album-list"}
               (for [{a :album} (media/albums ctx)] [:option {:value a}])]
              [:div {:class "con-editor-bar-right" :style "margin-top:18px;"}
               [:button {:type "submit" :class "con-btn con-btn--primary"} "Upload"]
               [:a {:href "/console/media" :class "con-btn con-btn--quiet"} "Cancel"]]]]))

(defn- uploaded-files
  "Ring gives one map for a single file and a vector for several."
  [params]
  (let [f (:files params)]
    (cond (map? f) [f] (sequential? f) (vec f) :else [])))

(defn upload [{:keys [params] :as ctx}]
  (let [album    (not-empty (str/trim (or (:album params) "")))
        category (or (not-empty (:category params)) "photo")
        taken    (normalize/local-date->epoch (:taken_on params))
        files    (remove #(nil? (:tempfile %)) (uploaded-files params))]
    (doseq [file files]
      (let [label  (or (not-empty (:filename file)) "")
            meta   (cond-> {:category category}
                     (seq label) (assoc :label label)
                     album       (assoc :album album)
                     taken       (assoc :date (date-str taken)))
            result (cf/upload! ctx file meta)]
        (when-let [id (:id result)]
          (media/record! ctx {:id id :label label :album album :category category
                              :taken_on taken}))))
    {:status 303 :headers {"location" (str "/console/media"
                                           (when album (str "?album=" album)))}}))

;; ---------------------------------------------------------------------------
;; Writes
;; ---------------------------------------------------------------------------

(defn save [{:keys [params path-params] :as ctx}]
  (media/save! ctx (:id path-params)
               {:label    (str/trim (or (:label params) ""))
                :album    (str/trim (or (:album params) ""))
                :category (:category params)
                :alt_text (str/trim (or (:alt_text params) ""))
                :taken_on (normalize/local-date->epoch (:taken_on params))})
  {:status 303 :headers {"location" (str "/console/media/" (:id path-params)
                                         (qs (filters-of ctx)))}})

(defn delete [{:keys [path-params] :as ctx}]
  (media/delete! ctx (:id path-params))
  {:status 303 :headers {"location" "/console/media"}})

(defn sync-now [ctx]
  (media/sync! ctx)
  {:status 303 :headers {"location" "/console/media"}})

;; ---------------------------------------------------------------------------

(def module
  {:biff.ring/routes
   [["/console/media" {:middleware [[wrap-signed-in]]}
     ["" {:get media :name ::media}]
     ["/upload" {:get upload-form :post upload :name ::upload :conflicting true}]
     ["/sync" {:post sync-now :name ::sync :conflicting true}]
     ["/:id" {:get media :post save :name ::one :conflicting true}]
     ["/:id/delete" {:post delete :name ::delete}]]]})
