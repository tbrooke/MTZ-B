(ns com.mtzion.app.pages
  "Public serving of CMS-managed pages.

  These pages can't be registered as reitit routes: a catch-all `/:slug` conflicts
  with every literal top-level route, and reitit only tolerates a conflict when
  *both* sides are marked `:conflicting true` — which is impossible for routes
  owned by Biff itself (`/_biff/admin`).

  So instead of routing, this module wraps Biff's ring handler and only looks for
  a CMS page when normal routing produced a 404. Every real route therefore always
  wins, and CMS slugs can be arbitrary without any chance of a startup conflict.

  Unlike about.clj / outreach.clj — which swap their whole designed layout for the
  DB body — the CMS body renders *inside* the standard page chrome, so editing a
  page can't detonate the site design."
  (:require [clojure.string :as str]
            [com.biffweb.ring :as biff.ring]
            [com.mtzion.model.nav :as model.nav]
            [com.mtzion.ui.base :as base]
            [lambdaisland.hiccup :as hiccup]))

(defn- page-content [p parent-label]
  (list
   [:section {:class "mtz-section"}
    (when parent-label
      [:p {:class "mtz-kicker"} parent-label])
    [:h1 {:class "mtz-h1" :style "max-width: 760px;"}
     (or (not-empty (:title p)) (:nav_label p))]
    [:hr {:class "mtz-rule"}]]
   [:section {:class "mtz-section"}
    [:div {:class "mtz-prose" :style "max-width: 760px;"}
     [::hiccup/unsafe-html (:body p)]]]))

(defn- render [ctx p parent-label]
  (base/page ctx
             (str (or (not-empty (:title p)) (:nav_label p) "Page") " — Mount Zion UCC")
             (page-content p parent-label)))

(defn- label-for [slug]
  (when (seq slug) (str/capitalize slug)))

(defn lookup
  "Returns a rendered response for the request's path, or nil if no published CMS
  page matches. Handles /slug and /parent/slug."
  [ctx]
  (let [segs (->> (str/split (or (:uri ctx) "") #"/")
                  (remove str/blank?)
                  vec)]
    (case (count segs)
      1 (when-let [p (model.nav/published-page ctx (first segs) nil)]
          (render ctx p nil))
      2 (let [[parent slug] segs]
          ;; only the known top-level sections can parent a page — this keeps
          ;; /admin/... and other 2-segment misses from hitting the DB
          (when (some #{parent} model.nav/top-level-slugs)
            (when-let [p (model.nav/published-page ctx slug parent)]
              (render ctx p (label-for parent)))))
      nil)))

(defn wrap-cms-pages
  "Falls back to a CMS page when routing produced a 404."
  [handler]
  (fn [req]
    (let [resp (handler req)]
      (if (and (= 404 (:status resp))
               (#{:get :head} (:request-method req)))
        (or (try (lookup req)
                 ;; a broken page must never turn a clean 404 into a 500
                 (catch Exception _ nil))
            resp)
        resp))))

(def ring-module
  "Drop-in replacement for (biff.ring/module) — same handler, plus CMS page
  fallback. Registered in place of it in com.mtzion.modules."
  {:biff.core/init
   (fn [modules-var]
     (let [base ((:biff.core/init (biff.ring/module)) modules-var)]
       (update base :biff.ring/handler wrap-cms-pages)))})
