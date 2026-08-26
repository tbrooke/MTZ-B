(ns com.mtzion.app.about
  (:require [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.model.church :as church]
            [com.mtzion.model.normalize :as norm]
            [com.mtzion.ui.base :as base]
            [lambdaisland.hiccup :as hiccup]))

(defn- page-content []
  (list
   [:section {:class "mtz-section"}
    [:p {:class "mtz-kicker"} (str "Est. " church/founded-year " · China Grove, NC")]
    [:h1 {:class "mtz-h1" :style "max-width: 760px;"} "About Mount Zion UCC"]
    [:p {:class "mtz-lede" :style "max-width: 680px;"}
     (str "A United Church of Christ congregation that has welcomed the community of "
          "China Grove for more than " (church/approx-years-since-founding) " years.")]
    [:hr {:class "mtz-rule"}]]

   [:section {:class "mtz-section"}
    [:div {:class "mtz-grid mtz-grid--2" :style "gap: 64px; align-items: center;"}
     [:div {:class "mtz-img" :style "aspect-ratio: 4/5; min-height: 0;"}
      [:span {:class "mtz-img-label"} "archival photo · ca. 1910"]]
     [:div
      [:p {:class "mtz-kicker"} "Our Story"]
      [:h2 {:class "mtz-h2"} (str "A congregation on this hill since " church/founded-year ".")]
      [:p {:class "mtz-prose" :style "color: var(--mtz-ink-soft);"}
       (str "Mount Zion was founded in " church/founded-year " by a small group of German Reformed settlers ")
       "in what was then a rural crossroads community. The original log meetinghouse "
       "gave way to a series of larger sanctuaries as the congregation grew — the "
       "current building dates to 1954."]
      [:p {:class "mtz-prose" :style "color: var(--mtz-ink-soft);"}
       "In 1957 the congregation joined the United Church of Christ at its formation, "
       "embracing an open and affirming theology that continues to define us today. "
       "We are a community shaped by history and oriented toward welcome."]
      ;; A "Search the archive →" link lived here, pointing at /about#archive —
      ;; an anchor that never existed, promising an archive that does not exist
      ;; yet. Reinstate it with the history work.
      ]]]

   [:section {:class "mtz-section--cream"}
    [:div {:class "mtz-section-inner"}
     [:h2 {:class "mtz-h2" :style "margin-bottom: 32px;"} "What We Believe"]
     [:div {:class "mtz-grid mtz-grid--3"}
      (for [[title body] [["Open & Affirming"
                           "All people are welcome at Mt Zion — regardless of background, identity, or where you are on your spiritual journey."]
                          ["Progressive Faith"
                           "We take scripture seriously and our tradition thoughtfully, while remaining open to continuing revelation."]
                          ["Community Rooted"
                           "Faith expressed in service: to China Grove, Rowan County, and beyond through active outreach ministries."]]]
        [:div {:class "mtz-card" :style "padding: 28px;"}
         [:h3 {:class "mtz-h3" :style "font-size: 20px; margin-bottom: 12px;"} title]
         [:p {:style "color: var(--mtz-ink-soft); margin: 0; font-size: 15px; line-height: 1.6;"} body]])]]]

   [:section {:class "mtz-section"}
    [:h2 {:class "mtz-h2" :style "margin-bottom: 32px;"} "Our Staff"]
    [:div {:class "mtz-grid mtz-grid--3"}
     [:div {:class "mtz-card" :style "padding: 28px;"}
      [:div {:class "mtz-img" :style "aspect-ratio: 1/1; border-radius: 50%; width: 96px; height: 96px; margin-bottom: 20px; min-height: 0;"}
       [:span {:class "mtz-img-label"} "photo"]]
      [:h3 {:class "mtz-h3" :style "font-size: 20px; margin-bottom: 4px;"} "Pastor Jim"]
      [:p {:class "mtz-mono" :style "font-size: 12px; letter-spacing: 0.12em; color: var(--mtz-mint-dark); margin: 0 0 12px; text-transform: uppercase;"} "Senior Pastor"]
      [:p {:style "color: var(--mtz-ink-soft); margin: 0; font-size: 15px;"} "Pastor Jim has served Mt Zion since 2008 and leads our weekly worship, Bible study, and pastoral care."]]]]

   [:section {:class "mtz-section--tint"}
    [:div {:class "mtz-section-inner" :style "text-align: center;"}
     [:h2 {:class "mtz-h2" :style "margin-bottom: 16px;"} "Come as you are."]
     [:p {:class "mtz-lede" :style "max-width: 560px; margin: 0 auto 32px;"}
      "We'd love to meet you. Reach out or join us any Sunday at 10:30 AM."]
     [:div {:class "mtz-row" :style "justify-content: center; gap: 12px; flex-wrap: wrap;"}
      [:a {:class "mtz-btn mtz-btn--primary" :href "/contact"} "Get in Touch"]
      [:a {:class "mtz-btn mtz-btn--ghost" :href "/worship"} "Plan Your Visit"]]]]))

(defn about [ctx]
  ;; Two things are load-bearing here:
  ;; 1. snake-keys — biff.sqlite/execute returns NAMESPACE-QUALIFIED keys
  ;;    (:page/body), so reading (:body row) off the raw result was always nil
  ;;    and this override silently never worked.
  ;; 2. status = "published" — a DB body REPLACES the whole designed page, so a draft
  ;;    must never be read here.
  (let [db-page (norm/snake-keys
                 (first (biff.sqlite/execute ctx {:select :* :from :page
                                                  :where  [:and [:= :slug "about"]
                                                           [:= :status "published"]]})))]
    (base/page ctx "About — Mount Zion UCC"
               (if (seq (:body db-page))
                 [::hiccup/unsafe-html (:body db-page)]
                 (page-content)))))

(def module
  {:biff.ring/routes
   [["/about" {:get about :name ::about}]]})
