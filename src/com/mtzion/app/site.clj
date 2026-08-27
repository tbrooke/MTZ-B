(ns com.mtzion.app.site
  "The Site pane — the outline of the site, and the editor for one part of it.

  The tree is menu item → page → the editable parts of that page, declared in
  com.mtzion.model.outline. Clicking a leaf opens an editor for exactly that
  leaf, showing only the fields its template actually reads.

  The distinction the tree is built around: a `:slot` is one row in a place the
  design lays out by hand, a `:list` is as many rows as you like all rendered
  the same way. 'Add a section to the Worship page' is a list, and needs no
  code."
  (:require [clojure.string :as str]
            [com.mtzion.lib.middleware :refer [wrap-signed-in]]
            [com.mtzion.lib.ui :as ui]
            [com.mtzion.model.content :as content]
            [com.mtzion.model.nav :as model.nav]
            [com.mtzion.model.normalize :as normalize]
            [com.mtzion.model.outline :as outline]
            [com.mtzion.ui.console :as con]
            [com.mtzion.ui.sections :as sections]
            [lambdaisland.hiccup :as hiccup]))

(defn- new-id [] (str (random-uuid)))

(defn- fragment [form]
  {:status  200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    (hiccup/render form {:doctype? false})})

(defn- base-url [pk sk] (str "/console/site/" pk "/" sk))

;; ---------------------------------------------------------------------------
;; Reading the rows behind a leaf
;; ---------------------------------------------------------------------------

(defn- slot-row [ctx section]
  (first (content/ls ctx :feature {:where [:= :page_slug (:slug section)]
                                   :order [[:sort_order :asc] [:created_at :asc]]
                                   :limit 1})))

(defn- list-rows [ctx section]
  (content/ls ctx :feature {:status #{content/draft content/published}
                            :where  [:= :page_slug (:slug section)]
                            :order  [[:sort_order :asc] [:created_at :asc]]}))

(defn- body-row [ctx section]
  (first (content/ls ctx :page {:where [:= :slug (:slug section)] :limit 1})))

;; ---------------------------------------------------------------------------
;; The tree
;; ---------------------------------------------------------------------------

(defn- leaf-counts
  "What each leaf currently holds, so the tree can say so without opening it."
  [ctx section]
  (case (:kind section)
    :list (let [rs (list-rows ctx section)]
            {:n (count rs)
             :live (count (filter #(= content/published (:status %)) rs))})
    :slot (when-let [r (slot-row ctx section)] {:status (:status r)})
    :body (when-let [r (body-row ctx section)]
            (when (seq (:body r)) {:status (:status r)}))
    nil))

(defn- tree-leaf [ctx pk section sel]
  (let [href     (base-url pk (:key section))
        selected (= sel [pk (:key section)])
        info     (leaf-counts ctx section)]
    [:a {:href href :class (str "con-tree-leaf" (when selected " is-selected"))}
     [:span {:class "con-tree-bullet"} (case (:kind section) :list "▾" :link "↗" "·")]
     [:span {:class "con-tree-label"} (:label section)]
     (cond
       (= :list (:kind section))
       [:span {:class "con-tree-count"} (str (:n info 0))]

       (:status info)
       (con/status-dot (:status info))

       (#{:link :static} (:kind section)) nil

       :else [:span {:class "con-tree-empty"} "empty"])]))

(defn- tree [ctx sel]
  (let [nav-pages (model.nav/nav-pages ctx)]
    [:aside {:class "con-list"}
     [:div {:class "con-list-head"}
      [:h1 {:class "con-list-title"} "Site"]]
     [:p {:class "con-tree-hint"}
      "The site as it is laid out. Pick a part of a page to edit it."]
     [:div {:class "con-rows con-tree"}
      (for [entry outline/tree
            :let [pk (outline/page-key entry)]]
        (list
         [:div {:class "con-tree-page"}
          [:span {:class "con-tree-page-label"} (:label entry)]
          [:a {:class "con-tree-page-path" :href (:path entry) :target "_blank"}
           (:path entry)]]
         (for [s (:sections entry)] (tree-leaf ctx pk s sel))
         ;; Pages created in the console file themselves under their parent, so
         ;; they appear here without anyone editing this namespace.
         (for [child (outline/cms-children nav-pages (:page-slug entry))]
           [:a {:href (str "/admin/pages/" (:slug child) "/edit")
                :class "con-tree-leaf con-tree-leaf--child"}
            [:span {:class "con-tree-bullet"} "·"]
            [:span {:class "con-tree-label"} (:nav_label child)]
            (con/status-dot (:status child))])))]]))

;; ---------------------------------------------------------------------------
;; Field editors
;; ---------------------------------------------------------------------------

(defn- field-inputs
  "Only the fields this leaf's template actually reads. A universal form is what
  made /admin/features confusing — it offered Image and Sort Order for a slot
  that renders neither."
  [fields row]
  (let [has? (set fields)]
    (list
     (when (has? :title)
       (con/field {:label "Heading"}
                  (con/text-input {:name "title" :value (or (:title row) "")})))
     (when (has? :subtitle)
       (con/field {:label "Kicker" :hint "The small line above the heading"}
                  (con/text-input {:name "subtitle" :value (or (:subtitle row) "")})))
     (when (has? :image)
       (con/field {:label "Image" :hint "Cloudflare image ID — copy it from Media"}
                  (con/text-input {:name "image_id" :value (or (:image_id row) "")
                                   :placeholder "a4df1d13-4c92-…"})))
     (when (has? :cta)
       (list
        (con/field {:label "Button label" :hint "Leave both blank for no button"}
                   (con/text-input {:name "cta_label" :value (or (:cta_label row) "")}))
        (con/field {:label "Button link"}
                   (con/text-input {:name "cta_url" :value (or (:cta_url row) "")})))))))

(defn- body-editor [row]
  [:div {:class "con-body-editor"}
   [:div {:data-tiptap "body" :class "tiptap-wrapper"}]
   [:input {:type "hidden" :name "body" :value (or (:body row) "")}]])

(defn- editor-shell
  "Every leaf editor is the same frame: what it is, where it shows up, the
  status pill, Save."
  [{:keys [title note action row status-url preview extra-actions fields body?]}]
  [:section {:class "con-editor"}
   [:form {:method "post" :action action :class "con-form" :id "con-post-form"}
    (ui/anti-forgery-field)
    [:div {:class "con-editor-bar"}
     (if status-url
       (con/status-pill status-url (or (:status row) content/draft))
       [:span {:class "con-pill con-pill--draft"} [:span {:class "con-pill-dot"}] "Not created yet"])
     [:div {:class "con-editor-bar-right"}
      [:button {:type "submit" :class "con-btn con-btn--primary"} "Save"]
      extra-actions]]

    [:h1 {:class "con-leaf-title"} title]
    (when note [:p {:class "con-leaf-note"} note])

    [:div {:class "con-details-grid con-details-grid--open"} fields]
    (when body? (body-editor row))]

   (when preview
     [:p {:class "con-editor-foot"}
      "Shows up on " [:a {:href preview :target "_blank"} preview]])])

;; ---------------------------------------------------------------------------
;; Column builders
;; ---------------------------------------------------------------------------

(defn- feature-cols [{:keys [params]} page-slug]
  {:page_slug  page-slug
   :title      (str/trim (or (:title params) ""))
   :subtitle   (or (:subtitle params) "")
   :body       (or (:body params) "")
   :image_id   (not-empty (:image_id params))
   :cta_label  (or (:cta_label params) "")
   :cta_url    (or (:cta_url params) "")
   :updated_at (normalize/now-epoch)})

(defn- page-cols [{:keys [params]} slug existing]
  {:slug        slug
   :title       (or (:title existing) "")
   :nav_label   (or (:nav_label existing) "")
   :nav_order   (:nav_order existing)
   :parent_slug (:parent_slug existing)
   :body        (or (:body params) "")
   :updated_at  (normalize/now-epoch)})

;; ---------------------------------------------------------------------------
;; Leaf views
;; ---------------------------------------------------------------------------

(defn- list-overview
  "Clicking the list itself: every row, in order, with the controls that make it
  a list — add, reorder, publish, archive."
  [ctx entry section pk]
  (let [rows (list-rows ctx section)
        base (base-url pk (:key section))]
    [:section {:class "con-editor"}
     [:div {:class "con-editor-bar"}
      [:span {:class "con-tree-count con-tree-count--lg"}
       (str (count rows) (if (= 1 (count rows)) " section" " sections"))]
      [:form {:method "post" :action (str base "/new") :style "margin:0;"}
       (ui/anti-forgery-field)
       [:button {:type "submit" :class "con-btn con-btn--primary"} "+ Add section"]]]

     [:h1 {:class "con-leaf-title"} (:label section)]
     [:p {:class "con-leaf-note"}
      (or (:note section)
          (str "As many as you like, in this order, on " (:path entry) ". "
               "Each one is a heading, some text, and optionally an image and a button."))]

     (if (empty? rows)
       [:p {:class "con-rows-empty"} "Nothing here yet. Add a section to start."]
       [:div {:class "con-stack"}
        (map-indexed
         (fn [i r]
           [:div {:class "con-stack-row"}
            (con/status-dot (:status r))
            [:a {:href (str base "/" (:id r)) :class "con-stack-main"}
             [:span {:class "con-row-title"} (or (not-empty (:title r)) "Untitled section")]
             [:span {:class "con-row-meta"}
              (if (seq (:body r))
                (str (count (re-seq #"<(p|h2|h3|ul|ol)\b" (:body r))) " blocks")
                "no text yet")]]
            [:div {:class "con-row-actions"}
             [:form {:method "post" :action (str base "/" (:id r) "/move")}
              (ui/anti-forgery-field)
              [:input {:type "hidden" :name "dir" :value "up"}]
              [:button {:type "submit" :class "con-move" :disabled (when (zero? i) "true")
                        :title "Move up"} "↑"]]
             [:form {:method "post" :action (str base "/" (:id r) "/move")}
              (ui/anti-forgery-field)
              [:input {:type "hidden" :name "dir" :value "down"}]
              [:button {:type "submit" :class "con-move"
                        :disabled (when (= i (dec (count rows))) "true")
                        :title "Move down"} "↓"]]]])
         rows)])]))

(defn- static-view [entry section]
  [:section {:class "con-editor"}
   [:h1 {:class "con-leaf-title"} (:label section)]
   [:p {:class "con-leaf-note"} (:note section)]
   [:p {:class "con-editor-foot"}
    "The page itself: " [:a {:href (:path entry) :target "_blank"} (:path entry)]]])

(defn- link-view [entry section]
  [:section {:class "con-editor"}
   [:h1 {:class "con-leaf-title"} (:label section)]
   [:p {:class "con-leaf-note"} (:note section)]
   [:p [:a {:href (:goto section) :class "con-btn con-btn--primary"}
        (str "Open " (last (str/split (:goto section) #"/")) " →")]]
   [:p {:class "con-editor-foot"}
    "Shows up on " [:a {:href (:path entry) :target "_blank"} (:path entry)]]])

(defn- slot-view [ctx entry section pk]
  (let [row  (slot-row ctx section)
        base (base-url pk (:key section))]
    (editor-shell
     {:title      (:label section)
      :note       (:note section)
      :action     base
      :row        row
      :status-url (when row (str base "/status"))
      :preview    (:path entry)
      :fields     (field-inputs (:fields section) row)
      :body?      (some #{:body} (:fields section))})))

(defn- body-view [ctx entry section pk]
  (let [row  (body-row ctx section)
        base (base-url pk (:key section))]
    (editor-shell
     {:title      (:label section)
      :note       (or (:note section)
                      (str "Extra text for " (:path entry)
                           ", above the sections below it."))
      :action     base
      :row        row
      :status-url (when row (str base "/status"))
      :preview    (:path entry)
      :fields     nil
      :body?      true})))

(defn- row-view [ctx entry section pk id]
  (let [row  (content/get-one ctx :feature id)
        base (base-url pk (:key section))]
    (when row
      (editor-shell
       {:title      (or (not-empty (:title row)) "Untitled section")
        :note       (str "One section on " (:path entry) ".")
        :action     (str base "/" id)
        :row        row
        :status-url (str base "/" id "/status")
        :preview    (:path entry)
        :fields     (field-inputs (:fields section) row)
        :body?      true
        :extra-actions
        [:button {:type "submit" :class "con-btn con-btn--quiet"
                  :formaction (str base "/" id "/archive")
                  :formnovalidate "true"
                  :onclick "return confirm('Archive this section? It leaves the page but is kept under Archive.')"}
         "Archive"]}))))

;; ---------------------------------------------------------------------------
;; Handlers
;; ---------------------------------------------------------------------------

(defn- render [ctx sel content]
  (con/page "Site" {:active :site}
            [:div {:class "con-pane"}
             (tree ctx sel)
             content]
            [:script {:src "/js/console.js" :defer "true"}]))

(defn site [ctx]
  (render ctx nil
          (con/empty-state
           "The site, laid out"
           [:p "Every menu item, the pages under it, and the parts of each page you can edit."]
           [:p (str "A · is one fixed place in the design. A ▾ is a list you can add to — "
                    "as many sections as you want, no code.")])))

(defn leaf [{:keys [path-params] :as ctx}]
  (let [{:keys [page section id]} path-params
        entry   (outline/find-page page)
        s       (outline/find-section page section)]
    (if-not (and entry s)
      {:status 404 :body "No such part of the site"}
      (render ctx [page section]
              (cond
                id                    (or (row-view ctx entry s page id)
                                          (con/empty-state "That section is gone"
                                                           [:p "It may have been archived."]))
                (= :static (:kind s)) (static-view entry s)
                (= :link (:kind s))   (link-view entry s)
                (= :list (:kind s))   (list-overview ctx entry s page)
                (= :body (:kind s))   (body-view ctx entry s page)
                :else                 (slot-view ctx entry s page))))))

(defn- save-feature! [ctx section id]
  (let [existing (when id (content/get-one ctx :feature id))
        cols     (feature-cols ctx (:slug section))]
    (if existing
      (content/save! ctx :feature id cols)
      (content/save! ctx :feature (or id (new-id))
                     (assoc cols :created_at (normalize/now-epoch)
                            :sort_order 0)))))

(defn save [{:keys [path-params] :as ctx}]
  (let [{:keys [page section id]} path-params
        s (outline/find-section page section)]
    (when s
      (case (:kind s)
        :body (let [existing (body-row ctx s)]
                (content/save! ctx :page (or (:id existing) (new-id))
                               (page-cols ctx (:slug s) existing)))
        :list (when id (save-feature! ctx s id))
        :slot (save-feature! ctx s (:id (slot-row ctx s)))
        nil))
    {:status 303 :headers {"location" (str (base-url page section)
                                           (when id (str "/" id)))}}))

(defn add-row [{:keys [path-params] :as ctx}]
  (let [{:keys [page section]} path-params
        s  (outline/find-section page section)]
    (if-not (= :list (:kind s))
      {:status 404 :body "Not a list"}
      (let [id   (new-id)
            next (inc (reduce max -1 (map #(or (:sort_order %) 0) (list-rows ctx s))))]
        (content/save! ctx :feature id
                       {:page_slug  (:slug s)
                        :title      ""
                        :subtitle   ""
                        :body       ""
                        :sort_order next
                        :created_at (normalize/now-epoch)
                        :updated_at (normalize/now-epoch)})
        {:status 303 :headers {"location" (str (base-url page section) "/" id)}}))))

(defn move
  "Up/down rather than drag-and-drop: it works without JavaScript, and a list of
  three or four sections does not need a drag affordance."
  [{:keys [path-params params] :as ctx}]
  (let [{:keys [page section id]} path-params
        s     (outline/find-section page section)
        rows  (vec (list-rows ctx s))
        i     (first (keep-indexed #(when (= id (:id %2)) %1) rows))
        j     (if (= "up" (:dir params)) (dec (or i 0)) (inc (or i 0)))]
    (when (and i (<= 0 j (dec (count rows))))
      ;; Rewrite the whole run: sort_order may be duplicated or absent on rows
      ;; that predate the console, so swapping two values is not enough.
      (let [reordered (assoc rows i (rows j) j (rows i))]
        (doseq [[n r] (map-indexed vector reordered)]
          (content/save! ctx :feature (:id r) {:sort_order n
                                               :updated_at (normalize/now-epoch)}))))
    {:status 303 :headers {"location" (base-url page section)}}))

(defn- toggle-and-render [ctx type id status-url]
  (content/toggle! ctx type id)
  (let [row (content/get-one ctx type id)]
    (fragment (con/status-pill status-url (:status row)))))

(defn status [{:keys [path-params] :as ctx}]
  (let [{:keys [page section id]} path-params
        s (outline/find-section page section)]
    (cond
      id (toggle-and-render ctx :feature id (str (base-url page section) "/" id "/status"))

      (= :body (:kind s))
      (if-let [row (body-row ctx s)]
        (toggle-and-render ctx :page (:id row) (str (base-url page section) "/status"))
        {:status 404 :body "Nothing to publish yet"})

      :else
      (if-let [row (slot-row ctx s)]
        (toggle-and-render ctx :feature (:id row) (str (base-url page section) "/status"))
        {:status 404 :body "Nothing to publish yet"}))))

(defn archive [{:keys [path-params] :as ctx}]
  (let [{:keys [page section id]} path-params]
    (content/archive! ctx :feature id)
    {:status 303 :headers {"location" (base-url page section)}}))

;; ---------------------------------------------------------------------------
;; Module
;; ---------------------------------------------------------------------------

(def module
  {:biff.ring/routes
   [["/console/site" {:middleware [[wrap-signed-in]]}
     ["" {:get site :name ::site}]
     ["/:page/:section"
      ["" {:get leaf :post save :name ::leaf}]
      ["/new"    {:post add-row :name ::add-row :conflicting true}]
      ["/status" {:post status  :name ::status  :conflicting true}]
      ["/:id" {:get leaf :post save :name ::row :conflicting true}]
      ["/:id/status"  {:post status  :name ::row-status}]
      ["/:id/archive" {:post archive :name ::row-archive}]
      ["/:id/move"    {:post move    :name ::row-move}]]]]})
