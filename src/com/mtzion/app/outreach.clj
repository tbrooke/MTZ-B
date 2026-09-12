(ns com.mtzion.app.outreach
  (:require [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.app.home-sections :as home-sections]
            [com.mtzion.model.normalize :as norm]
            [com.mtzion.model.outreach :as outreach]
            [com.mtzion.ui.base :as base]
            [com.mtzion.ui.sections :as sections]
            [lambdaisland.hiccup :as hiccup]))

;; ---------------------------------------------------------------------------
;; Overview
;; ---------------------------------------------------------------------------

(defn- partner-row
  "One partner on the overview: what they do, where, and a way through to more."
  [p]
  [:a {:class "mtz-outreach-row" :href (outreach/path p)}
   [:div {:class "mtz-outreach-row-meta"} (:note p)]
   [:div {:class "mtz-outreach-row-main"}
    [:h3 {:class "mtz-h3" :style "font-size: 21px; margin: 0 0 6px;"} (:name p)]
    [:p {:style "color: var(--mtz-ink-soft); margin: 0; font-size: 15px;"}
     (sections/plain (:summary p))]]
   [:span {:class "mtz-outreach-row-go" :aria-hidden "true"} "→"]])

(defn- partners
  "Adopted rows if the console has any, the shipped list otherwise. Same rule as
  the preschool page: the moment a row exists the defaults stop applying."
  [ctx]
  (outreach/merge-cms (sections/rows ctx "outreach-partners")))

(defn- page-content [ctx]
  (list
   [:section {:class "mtz-section"}
    [:p {:class "mtz-kicker"} "Serving Rowan County & Beyond"]
    [:h1 {:class "mtz-h1" :style "max-width: 760px;"} "Outreach Ministries"]
    [:p {:class "mtz-lede" :style "max-width: 640px;"}
     "Mt. Zion believes faith is expressed in action. We work alongside five "
     "organisations here in China Grove and across Rowan County — feeding "
     "neighbours, building homes, and checking in on those who would otherwise "
     "go a day without a visitor."]
    [:hr {:class "mtz-rule"}]]

   [:section {:class "mtz-section"}
    [:div {:class "mtz-section-inner"}
     [:div {:class "mtz-outreach-list"}
      (map partner-row (partners ctx))]]]

   [:section {:class "mtz-section--cream"}
    [:div {:class "mtz-section-inner"}
     [:div {:class "mtz-grid mtz-grid--2" :style "gap: 64px; align-items: center;"}
      [:div
       [:p {:class "mtz-kicker"} "Get Involved"]
       [:h2 {:class "mtz-h2"} "Volunteer with us."]
       [:p {:class "mtz-prose" :style "color: var(--mtz-ink-soft);"}
        "Our outreach is powered by congregation volunteers. Whether you have an "
        "hour a month or a day a week to give, there's a place for you. No "
        "experience required — just a willingness to help."]
       [:div {:class "mtz-row" :style "gap: 12px; margin-top: 24px; flex-wrap: wrap;"}
        [:a {:class "mtz-btn mtz-btn--primary" :href "/contact"} "Sign Up to Volunteer"]
        [:a {:class "mtz-btn mtz-btn--ghost"   :href "/contact"} "Ask a Question"]]]
      [:div {:class "mtz-img" :style "aspect-ratio: 4/3; min-height: 0;"}
       [:span {:class "mtz-img-label"} "volunteers · food drive"]]]]]

   ;; Sections added in the console — unlimited, in the editor's order.
   (sections/region ctx "outreach")))

;; ---------------------------------------------------------------------------
;; One partner
;; ---------------------------------------------------------------------------

(defn- partner-page [p]
  (list
   [:section {:class "mtz-section"}
    [:p {:class "mtz-kicker"}
     [:a {:href "/outreach" :style "color: inherit; text-decoration: none;"} "Outreach"]
     " · " (:where p)]
    [:h1 {:class "mtz-h1" :style "max-width: 760px;"} (:name p)]
    [:p {:class "mtz-lede" :style "max-width: 640px;"} (sections/plain (:summary p))]
    [:hr {:class "mtz-rule"}]]

   [:section {:class "mtz-section"}
    [:div {:class "mtz-section-inner"}
     [:div {:class "mtz-grid mtz-grid--2" :style "gap: 64px; align-items: start;"}
      [:div {:class "mtz-prose" :style "color: var(--mtz-ink-soft);"}
       ;; Shipped partners carry a vector of plain paragraphs; a partner reworded
       ;; in the console carries one Tiptap string. prose renders either.
       (for [para (:body p)]
         (sections/prose {:style "margin: 0 0 18px;"} para))]
      [:aside {:class "mtz-card" :style "padding: 28px;"}
       [:p {:class "mtz-card-meta"} "How Mt. Zion helps"]
       [:p {:style "color: var(--mtz-ink-soft); margin: 0 0 20px; font-size: 15px;"}
        (:involve p)]
       (when (:url p)
         [:a {:class "mtz-arrow-link" :href (:url p)
              :target "_blank" :rel "noopener noreferrer"}
          "Visit their site →"])]]]]

   [:section {:class "mtz-section--tint"}
    [:div {:class "mtz-section-inner" :style "text-align: center;"}
     [:h2 {:class "mtz-h2" :style "margin-bottom: 16px;"} "Want to help?"]
     [:p {:class "mtz-lede" :style "max-width: 520px; margin: 0 auto 28px;"}
      "Tell us which ministry interests you and we'll put you in touch."]
     [:a {:class "mtz-btn mtz-btn--primary" :href "/contact"} "Get in Touch"]
     [:p {:style "margin: 24px 0 0;"}
      [:a {:class "mtz-arrow-link" :href "/outreach"} "← All outreach"]]]]))

;; ---------------------------------------------------------------------------
;; Handlers
;; ---------------------------------------------------------------------------

(defn outreach [ctx]
  ;; See about.clj: snake-keys strips the :page/ namespace biff.sqlite/execute
  ;; adds (without it the body reads as nil and the override does nothing), and
  ;; status = "published" keeps drafts from replacing the designed page.
  (let [db-page (norm/snake-keys
                 (first (biff.sqlite/execute ctx {:select :* :from :page
                                                  :where  [:and [:= :slug "outreach"]
                                                           [:= :status "published"]]})))]
    (base/page ctx "Outreach — Mount Zion UCC"
               (if (seq (:body db-page))
                 (list [::hiccup/unsafe-html (:body db-page)]
                       (sections/region ctx "outreach"))
                 (page-content ctx)))))

(defn partner [{:keys [path-params] :as ctx}]
  (if-let [p (first (filter #(= (:slug path-params) (:slug %)) (partners ctx)))]
    (base/page ctx (str (:name p) " — Mount Zion UCC") (partner-page p))
    {:status 404 :headers {"content-type" "text/html"}
     :body "Not found"}))

(def module
  {:biff.ring/routes
   [["/outreach"       {:get outreach :name ::outreach}]
    ["/outreach/:slug" {:get partner  :name ::partner}]]})
