(ns com.mtzion.ui.admin
  (:require [lambdaisland.hiccup :as hiccup]
            [com.mtzion.lib.ui :as ui]))

(defn admin-page [title & body]
  {:status  200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body
   (hiccup/render
    [:html {:lang "en"}
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:title (str title " — Mt Zion CMS")]
      [:link {:rel "icon" :href "data:,"}]
      [:link {:rel "stylesheet" :href (ui/css-path)}]
      [:script {:src "/js/admin.js" :defer "true"}]]
     [:body {:style "margin:0; background:#fafaf8;"}
      body]])})

(defn top-bar []
  [:div {:class "adm-bar"}
   [:span {:class "adm-bar-brand"} "Mt Zion CMS"]
   [:div {:class "adm-bar-actions"}
    [:a {:href "/" :class "adm-bar-link" :target "_blank"} "View Site →"]
    [:form {:method "post" :action "/admin/signout" :style "margin:0;"}
     (ui/anti-forgery-field)
     [:button {:type "submit"
               :class "adm-bar-link"
               :style "background:none;border:none;cursor:pointer;padding:0;font:inherit;"}
      "Sign Out"]]]])

(defn page-header
  ([title] (page-header title nil))
  ([title back-href]
   [:div {:class "adm-section-header"}
    [:h1 {:class "adm-page-title" :style "margin:0;"} title]
    (when back-href
      [:a {:href back-href :class "adm-back"} "← Back"])]))

(defn field [{:keys [label hint]} & children]
  [:div {:class "adm-field"}
   [:label {:class "adm-label"} label]
   children
   (when hint [:span {:class "adm-hint"} hint])])

(defn text-input [attrs]
  [:input (merge {:class "adm-input" :type "text"} attrs)])

(defn select-input [attrs options current-val]
  [:select (merge {:class "adm-select"} attrs)
   (for [[v label] options]
     [:option {:value v :selected (= v (str current-val))} label])])

(defn tiptap-field [field-name current-html]
  [:div
   [:div {:data-tiptap field-name :class "tiptap-wrapper"}]
   [:input {:type "hidden" :name field-name :value (or current-html "")}]])

(defn submit-row [{:keys [label cancel-href] :or {label "Save"}}]
  [:div {:class "adm-form-actions"}
   [:button {:type "submit" :class "mtz-btn mtz-btn--primary"} label]
   (when cancel-href
     [:a {:href cancel-href :class "adm-link"} "Cancel"])])

(defn badge [published?]
  [:span {:class (str "adm-badge " (if published? "adm-badge--pub" "adm-badge--draft"))}
   (if published? "Published" "Draft")])

(defn delete-form [action-url anti-forgery]
  [:form {:method "post" :action action-url :style "display:inline;"}
   anti-forgery
   [:button {:type "submit"
             :class "adm-link adm-link--danger"
             :onclick "return confirm('Delete this item?')"}
    "Delete"]])
