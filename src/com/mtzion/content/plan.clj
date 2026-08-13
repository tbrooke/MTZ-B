(ns com.mtzion.content.plan
  "Turns validated EDN items into a plan of database writes, and renders that
  plan as a human-readable diff.

  Two rules govern everything here:

  1. **Never delete.** There is no prune. Removing stale content stays a
     deliberate human action in the admin panel.
  2. **Never touch published state on UPDATE.** Only an INSERT sets draft state.
     Otherwise re-dropping a corrected bulletin would silently un-publish
     something the pastor had already approved.

  Matching order per item: `import_key`, then the type's natural key (adopting
  the row by stamping it with the key), then insert."
  (:require [clojure.string :as str]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.content.hiccup :as ch]
            [com.mtzion.model.normalize :as norm]))

(defn- exec [ctx honey]
  (norm/snake-keys-all (biff.sqlite/execute ctx honey)))

(defn- new-id [] (str (random-uuid)))

;; ---------------------------------------------------------------------------
;; Per-type description
;; ---------------------------------------------------------------------------

(def ^:private types
  {:event   {:table :event
             :natural-key [:title :start_at]
             :label "event"}
   :post    {:table :post
             :natural-key [:slug]
             :label "post"}
   :page    {:table :page
             :natural-key [:slug]
             :label "page"}
   :feature {:table :feature
             :natural-key [:page_slug :title]
             :label "feature"}
   :sermon  {:table :sermon
             :natural-key [:sermon_date :title]
             :label "sermon"}})

(defn- hiccup->html [v opts]
  (when (some? v) (ch/->html v opts)))

(defn- kw->str [v] (when (some? v) (name v)))

(defn- ->row
  "EDN item -> the column map the database expects.

  Returns only fields the agent controls. Draft state, ids, timestamps and
  import bookkeeping are added later by `insert-values` / `update-values`, so
  they cannot be influenced by the contract."
  [{:keys [type] :as item} opts]
  (case type
    :event
    {:title       (:title item)
     :description (or (hiccup->html (:description item) opts) "")
     :location    (or (:location item) "")
     :start_at    (norm/local-datetime->epoch (:starts-at item))
     :end_at      (norm/local-datetime->epoch (:ends-at item))
     :all_day     (norm/edn-bool->int (:all-day item))
     :recurrence  (or (kw->str (:recurrence item)) "none")
     :recur_until (norm/local-date->epoch (:recur-until item))
     :featured    (norm/edn-bool->int (:featured item))}

    :post
    {:title        (:title item)
     :slug         (or (:slug item) (norm/slugify (:title item)))
     :category     (or (kw->str (:category item)) "blog")
     :excerpt      (or (:excerpt item) "")
     :body         (or (hiccup->html (:body item) opts) "")
     :show_on_home (norm/edn-bool->int (:show-on-home item))}

    :page
    {:slug        (:slug item)
     :title       (or (:title item) "")
     :nav_label   (or (:nav-label item) "")
     :nav_order   (:nav-order item)
     :parent_slug (kw->str (:parent item))
     :body        (or (hiccup->html (:body item) opts) "")}

    :feature
    {:page_slug    (kw->str (:page-slug item))
     :title        (:title item)
     :subtitle     (or (:subtitle item) "")
     :body         (or (hiccup->html (:body item) opts) "")
     :cta_label    (or (:cta-label item) "")
     :cta_url      (or (:cta-url item) "")
     :sort_order   (or (:sort-order item) 0)
     :show_on_home (norm/edn-bool->int (:show-on-home item))}

    :sermon
    {:title            (:title item)
     :sermon_date      (norm/local-date->epoch (:sermon-date item))
     :scripture_cw     (not-empty (:scripture-cw item))
     :scripture_gospel (not-empty (:scripture-gospel item))
     :series           (not-empty (:series item))
     :description      (or (:description item) "")}))

(defn- import-meta
  "Provenance, plus anything the agent proposed that the importer refuses to
  apply directly. `post` has no published column — draft-ness IS
  published_at IS NULL — so the extracted date is parked here for the admin form
  to prefill rather than thrown away."
  [item source]
  (pr-str (cond-> {:source source :at (norm/now-epoch)}
            (:published-on item) (assoc :proposed-published-on (:published-on item)))))

;; ---------------------------------------------------------------------------
;; Matching
;; ---------------------------------------------------------------------------

(defn- find-by-import-key [ctx table k]
  (first (exec ctx {:select :* :from table :where [:= :import_key k] :limit 1})))

(defn- find-by-natural-key [ctx table cols row]
  (when (every? #(some? (get row %)) cols)
    (first (exec ctx {:select :* :from table
                      :where  (into [:and] (map #(vector := % (get row %))) cols)
                      :limit  1}))))

(defn- match [ctx {:keys [table natural-key]} import-key row]
  (if-let [hit (find-by-import-key ctx table import-key)]
    [:import-key hit]
    (if-let [hit (find-by-natural-key ctx table natural-key row)]
      [:natural-key hit]
      [nil nil])))

;; ---------------------------------------------------------------------------
;; Change detection
;; ---------------------------------------------------------------------------

(defn- changed-fields
  "Which of the agent-controlled columns actually differ. Compares loosely so a
  nil and an empty string are not reported as a change."
  [row existing]
  (into (sorted-map)
        (keep (fn [[k v]]
                (let [old (get existing k)
                      norm* #(if (or (nil? %) (and (string? %) (str/blank? %))) nil %)]
                  (when (not= (norm* v) (norm* old))
                    [k {:from old :to v}])))
              row)))

;; ---------------------------------------------------------------------------
;; Planning
;; ---------------------------------------------------------------------------

(defn plan-item [ctx item source opts]
  (let [{:keys [table] :as spec} (get types (:type item))
        k        (:key item)
        row      (->row item opts)
        [via existing] (match ctx spec k row)
        changes  (when existing (changed-fields row existing))
        adopting (= via :natural-key)]
    (cond-> {:type       (:type item)
             :key        k
             :table      table
             :label      (:label spec)
             :row        row
             :meta       (import-meta item source)
             :title      (or (:title row) (:slug row))}
      (nil? existing)
      (assoc :action :create :id (new-id))

      (and existing (seq changes))
      (assoc :action :update :id (:id existing) :changes changes :adopting adopting)

      (and existing (empty? changes) adopting)
      (assoc :action :update :id (:id existing) :changes {} :adopting true)

      (and existing (empty? changes) (not adopting))
      (assoc :action :unchanged :id (:id existing)))))

(defn build
  "Returns a seq of planned operations, in item order."
  [ctx items source opts]
  (mapv #(plan-item ctx % source opts) items))

;; ---------------------------------------------------------------------------
;; Applying
;; ---------------------------------------------------------------------------

(def ^:private draft-on-insert
  "How each table expresses 'not published yet'."
  {:event   {:published 0}
   :page    {:published 0}
   :feature {:published 0}
   :sermon  {:published 0}
   ;; post has no published column — a NULL published_at IS the draft state
   :post    {:published_at nil}})

(defn- insert-values [{:keys [id table row meta]}]
  (let [now (norm/now-epoch)]
    (merge row
           (get draft-on-insert table)
           {:id id :import_key nil :import_meta meta}
           (when (contains? #{:event :post :feature :sermon} table) {:created_at now})
           (when (contains? #{:page :feature} table) {:updated_at now}))))

(defn- update-values [{:keys [row meta table]}]
  ;; Deliberately omits published / published_at — see the namespace docstring.
  (merge row
         {:import_meta meta}
         (when (contains? #{:page :feature} table) {:updated_at (norm/now-epoch)})))

(defn apply-op!
  "Executes one planned operation. `exec-fn` runs a HoneySQL map (so the caller
  can supply a transaction-bound executor)."
  [exec-fn {:keys [action table key id] :as op}]
  (case action
    :create   (exec-fn {:insert-into table
                        :values [(assoc (insert-values op) :import_key key)]})
    :update   (exec-fn {:update table
                        :set    (assoc (update-values op) :import_key key)
                        :where  [:= :id id]})
    :unchanged nil))

;; ---------------------------------------------------------------------------
;; Diff rendering
;; ---------------------------------------------------------------------------

(def ^:private datetime-cols #{:start_at :end_at})
(def ^:private date-cols     #{:recur_until :sermon_date :published_at :file_date})
(def ^:private html-cols     #{:body :description})

(defn- fmt-epoch-datetime [e]
  (when e
    (let [z (-> (java.time.Instant/ofEpochSecond e) (java.time.ZonedDateTime/ofInstant norm/eastern))]
      (str (.format z (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm"))
           " " (.format z (java.time.format.DateTimeFormatter/ofPattern "zzz"))
           "  (" e ")"))))

(defn- block-tag-counts [html]
  (frequencies (map second (re-seq #"<(p|h2|h3|h4|ul|ol|li|blockquote|pre|table|tr|img|a)\b" (or html "")))))

(defn- fmt-html-delta [from to]
  (let [a (block-tag-counts from) b (block-tag-counts to)
        tags (sort (distinct (concat (keys a) (keys b))))
        delta (for [t tags
                    :let [d (- (get b t 0) (get a t 0))]
                    :when (not (zero? d))]
                (str (if (pos? d) "+" "") d " <" t ">"))]
    (str (count (or from "")) " B -> " (count (or to "")) " B"
         (when (seq delta) (str "   (" (str/join ", " delta) ")")))))

(defn- fmt-value [col v]
  (cond
    (nil? v)                    "—"
    (datetime-cols col)         (fmt-epoch-datetime v)
    (date-cols col)             (str (norm/epoch->date-str v) "  (" v ", UTC midnight)")
    (and (string? v) (> (count v) 60)) (str (pr-str (subs v 0 57)) "…")
    :else                       (pr-str v)))

(defn- fmt-change [col {:keys [from to]}]
  (if (html-cols col)
    (format "    %-16s %s" (name col) (fmt-html-delta from to))
    (format "    %-16s %s -> %s" (name col) (fmt-value col from) (fmt-value col to))))

(defn render-diff [ops]
  (let [lines
        (for [{:keys [action label key title changes adopting table]} ops]
          (case action
            :create
            (str (format "  %-8s %-34s CREATE\n" label key)
                 (format "    %-16s %s\n" "title" (pr-str title))
                 (format "    %-16s %s"
                         (if (= table :post) "published_at" "published")
                         (if (= table :post) "NULL   <- draft" "0      <- draft")))

            :update
            (str (format "  %-8s %-34s UPDATE%s\n" label key
                         (if adopting "   (matched by natural key — adopting)" ""))
                 (if (seq changes)
                   (str/join "\n" (map (fn [[c ch]] (fmt-change c ch)) changes))
                   "    (no field changes; stamping import_key)")
                 (format "\n    %-16s %s" "published" "unchanged — human-controlled"))

            :unchanged
            (format "  %-8s %-34s UNCHANGED" label key)))
        counts (frequencies (map :action ops))]
    (str (str/join "\n\n" lines)
         "\n\n  " (str/join " · " [(str (get counts :create 0) " create")
                                   (str (get counts :update 0) " update")
                                   (str (get counts :unchanged 0) " unchanged")]))))
