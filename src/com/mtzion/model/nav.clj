(ns com.mtzion.model.nav
  "CMS-managed navigation pages. Pure data access — the nav *shape* lives in
  com.mtzion.ui.nav, which merges these rows into the static skeleton."
  (:require [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.model.normalize :as norm]))

(def ^:private normalize norm/snake-keys-all)

(def top-level-slugs
  "Slugs a CMS page may be filed under. Mirrors the static nav in ui.nav —
  these are the only valid values for page.parent_slug."
  ["about" "worship" "events" "activities" "news" "outreach" "contact"])

(defn page-path
  "Public URL for a CMS page. Child pages nest under their parent so their URLs
  read hierarchically and don't collide with top-level static routes."
  [{:keys [slug parent_slug]}]
  (if (seq parent_slug)
    (str "/" parent_slug "/" slug)
    (str "/" slug)))

(defn nav-pages
  "Published CMS pages that carry a nav label, ordered by nav_order (blank last).
  These are the pages that should appear in site navigation."
  [ctx]
  (->> (normalize (biff.sqlite/execute
                   ctx {:select   :*
                        :from     :page
                        :where    [:and [:= :status "published"]
                                   [:is-not :nav_label nil]
                                   [:!= :nav_label ""]]
                        :order-by [[:nav_order :asc] [:slug :asc]]}))
       (sort-by (juxt #(or (:nav_order %) 9999) :slug))
       vec))

(defn published-page
  "Look up a single published page by slug, verifying it sits under the expected
  parent (nil for top-level). Returns nil on mismatch so a child page can't also
  be served from the top-level URL."
  [ctx slug parent]
  (let [p (first (normalize (biff.sqlite/execute
                             ctx {:select :* :from :page
                                  :where  [:and [:= :slug slug] [:= :status "published"]]})))]
    (when (and p (= (not-empty (or (:parent_slug p) "")) (not-empty (or parent ""))))
      p)))
