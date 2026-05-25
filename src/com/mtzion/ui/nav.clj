(ns com.mtzion.ui.nav
  "Navigation and footer components for the public site.")

;; --- SVGs ---

(defn- lollipop-svg
  ([] (lollipop-svg 18 22))
  ([w h]
   [:svg {:viewBox "0 0 22 26" :width w :height h :aria-hidden "true"}
    [:defs
     [:radialGradient {:id "mtz-lolli-g" :cx "0.38" :cy "0.36" :r "0.7"}
      [:stop {:offset "0%"   :stop-color "#ffd6a8"}]
      [:stop {:offset "38%"  :stop-color "#ff9bbf"}]
      [:stop {:offset "72%"  :stop-color "#b88dff"}]
      [:stop {:offset "100%" :stop-color "#6fb6e8"}]]]
    [:circle {:cx "11" :cy "9" :r "7" :fill "url(#mtz-lolli-g)"}]
    [:path {:d "M11 4.6 a4.4 4.4 0 0 1 0 8.8 a3 3 0 0 1 0 -6 a1.6 1.6 0 0 1 0 3.2"
            :fill "none" :stroke "#ffffff" :stroke-width "0.9"
            :stroke-linecap "round" :opacity "0.85"}]
    [:ellipse {:cx "8.4" :cy "6.6" :rx "1.6" :ry "1.1" :fill "#ffffff" :opacity "0.55"}]
    [:line {:x1 "11" :y1 "16" :x2 "11" :y2 "24"
            :stroke "#9a8b73" :stroke-width "1.3" :stroke-linecap "round"}]]))

(defn- steeple-svg []
  [:svg {:viewBox "0 0 22 26" :width "18" :height "22" :aria-hidden "true"}
   [:path {:d "M11 3 L11 8" :stroke "currentColor" :stroke-width "1.3" :stroke-linecap "round"}]
   [:path {:d "M9 5.4 L13 5.4 M9 7 L13 7" :stroke "currentColor" :stroke-width "1" :stroke-linecap "round"}]
   [:path {:d "M11 8 L4 15 L4 22 L18 22 L18 15 Z" :fill "currentColor" :opacity "0.92"}]])

;; --- NAV ITEM ---

(defn- nav-item [item current-page]
  (let [active?    (= current-page (some-> item :label clojure.string/lower-case))
        preschool? (= (:key item) :preschool)]
    [:div {:class "mtz-nav-item"}
     [:a {:href  (:path item "/")
          :class (str "mtz-nav-link"
                      (when active? " is-active")
                      (when preschool? " mtz-nav-link--preschool"))
          :data-mtz-nav-link "true"
          :data-mtz-anchor  (when (:scroll? item)
                              (some-> item :label clojure.string/lower-case))}
      (when preschool? (lollipop-svg 13 16))
      (:label item)
      (when (:has-children? item)
        [:span {:class "mtz-caret" :aria-hidden "true"} "▾"])]
     (when (:has-children? item)
       [:div {:class "mtz-submenu"}
        (for [sub (:submenu item)]
          [:a {:key   (:label sub)
               :href  (:path sub "#")
               :class "mtz-submenu-item"}
           (:label sub)])])]))

;; --- FLIP CHIP ---

(defn- flip-chip [site-context]
  (let [church? (= site-context :church)]
    [:a {:href  (if church? "/preschool" "/")
         :class "mtz-flip-chip"
         :aria-label (if church? "Switch to Preschool site" "Switch to Church site")}
     [:span {:class "mtz-flip-mark"}
      (if church? (lollipop-svg) (steeple-svg))]
     [:span {:class "mtz-flip-name"}
      (if church? "Preschool" "Church")]]))

;; --- FALLBACK NAV DATA ---

(def ^:private church-fallback-nav
  [{:label "Home"       :path "/"           :has-children? false :scroll? true}
   {:label "About"      :path "/about"      :has-children? true  :scroll? true
    :submenu [{:label "Our Story"       :path "/about"}
              {:label "History"         :path "/about#archive"}
              {:label "Beliefs"         :path "/about"}
              {:label "Staff & Council" :path "/about"}]}
   {:label "Worship"    :path "/worship"    :has-children? true  :scroll? true
    :submenu [{:label "This Sunday" :path "/worship"}
              {:label "Sunday Worship" :path "/sermons"}
              {:label "Music"       :path "/worship"}]}
   {:label "Events"     :path "/events"     :has-children? false :scroll? false}
   {:label "Activities" :path "/activities" :has-children? false :scroll? false}
   {:label "News"       :path "/news"       :has-children? true  :scroll? true
    :submenu [{:label "Newsletter"    :path "/news"}
              {:label "Announcements" :path "/news"}]}
   {:label "Outreach"   :path "/outreach"   :has-children? false :scroll? true}
   {:label "Contact"    :path "/contact"    :has-children? false :scroll? true}])

;; --- SITE HEADER ---

(defn site-header
  "Sticky 5-column header with centered wordmark and split nav.
   nav-data — vector of nav items (nil falls back to church-fallback-nav)
   site-context — :church (default) or :preschool"
  ([]
   (site-header nil :church))
  ([nav-data]
   (site-header nav-data :church))
  ([nav-data site-context]
   (let [items       (if (seq nav-data)
                       (remove #(= (:key %) :contact) nav-data)
                       church-fallback-nav)
         mid         (int (Math/ceil (/ (count items) 2)))
         left-items  (take mid items)
         right-items (drop mid items)
         wordmark    (if (= site-context :preschool) "MT ZION PRESCHOOL" "MT ZION UCC")
         home-href   (if (= site-context :preschool) "/preschool" "/")]
     [:header {:id "mtz-header"
               :class (str "mtz-header is-top"
                           (when (= site-context :preschool) " mtz-mode--preschool"))}
      [:div {:class "mtz-header-inner"}
       [:nav {:class "mtz-nav mtz-nav--left" :aria-label "Primary navigation left"}
        (for [item left-items]
          (nav-item item nil))]
       [:a {:href home-href :class "mtz-wordmark" :aria-label (str wordmark " home")}
        [:span {:class "mtz-logo" :aria-hidden "true"}
         [:svg {:viewBox "0 0 80 22" :class "mtz-logo-roof" :preserveAspectRatio "none"}
          [:path {:d "M2 20 L40 3 L78 20" :fill "none" :stroke "currentColor"
                  :stroke-width "1.4" :stroke-linecap "round" :stroke-linejoin "round"}]]
         [:span {:class "mtz-logo-mt"} "MT"]
         [:span {:class "mtz-logo-zion"} "ZION"]
         [:span {:class "mtz-logo-ucc"} (if (= site-context :preschool) "Preschool" "UCC")]]
        [:span {:class "mtz-wordmark-sub-static"} "China Grove, NC"]]
       [:nav {:class "mtz-nav mtz-nav--right" :aria-label "Primary navigation right"}
        (for [item right-items]
          (nav-item item nil))]
       (flip-chip site-context)]])))

;; --- SITE FOOTER ---

(defn site-footer
  ([] (site-footer :church))
  ([_site-context]
   [:footer {:class "mtz-footer"}
    [:div {:class "mtz-footer-inner"}
     [:div {:class "mtz-footer-col"}
      [:h4 {:class "mtz-footer-h"} "Join Our Community"]
      [:p {:class "mtz-footer-p"} "All are welcome."]
      [:div {:class "mtz-footer-cta"}
       [:a {:href "/worship" :class "mtz-btn mtz-btn--primary"} "Plan Your Visit"]
       [:a {:href "/contact" :class "mtz-btn mtz-btn--ghost"}   "Contact Us"]]]
     [:div {:class "mtz-footer-col mtz-footer-col--center"}
      [:h4 {:class "mtz-footer-h"} "Mount Zion UCC"]
      [:p {:class "mtz-footer-p"}
       "1415 S Main St" [:br]
       "China Grove, NC 28023" [:br]
       [:a {:href "tel:+17048571169"
            :style "color: inherit; text-decoration: underline; text-underline-offset: 2px;"}
        "(704) 857-1169"]]]
     [:div {:class "mtz-footer-col mtz-footer-col--right"}
      [:p {:class "mtz-footer-meta"} "© 2026 Mount Zion UCC"]
      [:p {:class "mtz-footer-meta"}
       [:a {:href "/privacy"} "Privacy Policy"]]
      [:p {:class "mtz-footer-meta"} "Powered by Mount Zion CMS"]]]]))

;; --- PRESCHOOL HEADER ---

(def ^:private preschool-nav
  [{:label "Home"       :path "/preschool"          :has-children? false :scroll? false}
   {:label "About"      :path "/preschool#about"    :has-children? false :scroll? false}
   {:label "Programs"   :path "/preschool#programs" :has-children? false :scroll? false}
   {:label "Enrollment" :path "/preschool#enroll"   :has-children? false :scroll? false}
   {:label "Calendar"   :path "/preschool#schedule" :has-children? false :scroll? false}
   {:label "Staff"      :path "/preschool#about"    :has-children? false :scroll? false}
   {:label "Contact"    :path "/contact"            :has-children? false :scroll? false}])

(defn preschool-header []
  (site-header preschool-nav :preschool))

;; --- PRESCHOOL FOOTER ---

(defn preschool-footer []
  [:footer {:class "mtz-footer"}
   [:div {:class "mtz-footer-inner"}
    [:div {:class "mtz-footer-col"}
     [:h4 {:class "mtz-footer-h"} "Mt. Zion Preschool"]
     [:p {:class "mtz-footer-p"} "A ministry of Mt. Zion UCC. Open to all families."]
     [:div {:class "mtz-footer-cta"}
      [:a {:href "/preschool#enroll" :class "mtz-btn mtz-btn--primary"} "Inquire About Enrollment"]
      [:a {:href "/contact" :class "mtz-btn mtz-btn--ghost"} "Contact Us"]]]
    [:div {:class "mtz-footer-col mtz-footer-col--center"}
     [:h4 {:class "mtz-footer-h"} "Mt. Zion Preschool"]
     [:p {:class "mtz-footer-p"}
      "1415 S Main St" [:br]
      "China Grove, NC 28023" [:br]
      [:a {:href "tel:+17048571169"
           :style "color: inherit; text-decoration: underline; text-underline-offset: 2px;"}
       "(704) 857-1169"]]]
    [:div {:class "mtz-footer-col mtz-footer-col--right"}
     [:p {:class "mtz-footer-meta"} "© 2026 Mount Zion UCC"]
     [:p {:class "mtz-footer-meta"}
      [:a {:href "/"} "Church Site"]]
     [:p {:class "mtz-footer-meta"} "Powered by Mount Zion CMS"]]]])

;; --- BREADCRUMBS ---

(defn breadcrumbs
  "Renders breadcrumb navigation.
   crumbs — [{:label \"Home\" :path \"/\"} ...]"
  [crumbs]
  (when (seq crumbs)
    [:nav {:class "flex items-center space-x-2 text-sm text-gray-500 mb-4"}
     (for [[idx crumb] (map-indexed vector crumbs)]
       (let [last? (= idx (dec (count crumbs)))]
         [:span {:key idx :class "flex items-center"}
          (if last?
            [:span {:class "font-medium text-gray-900"} (:label crumb)]
            [:a {:href  (:path crumb)
                 :class "text-teal-600 hover:text-teal-800 hover:underline"}
             (:label crumb)])
          (when-not last?
            [:svg {:class "w-4 h-4 mx-2" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
             [:path {:stroke-linecap "round" :stroke-linejoin "round"
                     :stroke-width "2" :d "M9 5l7 7-7 7"}]])]))]))
