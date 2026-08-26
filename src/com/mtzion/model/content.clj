(ns com.mtzion.model.content
  "One vocabulary for every kind of editable content.

  Before this namespace, `post`, `event`, `feature`, `page` and `sermon` each
  spelled 'not live yet' differently — four `published INTEGER` columns and, on
  `post`, the convention that a NULL `published_at` meant draft. Every list
  screen rendered its badge from a different expression and the importer carried
  a lookup table just to say 'make this a draft'.

  Now there is one `status` column on all five, holding exactly one of:

    draft      — written, not on the public site
    published  — live
    archived   — taken off the site, kept for the record

  Nothing is ever deleted by an editor. `archive!` is the exit; `purge!` exists
  but refuses to touch anything that isn't already archived.

  ## The `published` column is vestigial

  It is still written, in lockstep with `status`, so a deploy can be rolled back
  to the previous release without the site going blank. Nothing reads it any
  more. It gets dropped once the console replaces /admin — see `backfill!`."
  (:require [clojure.tools.logging :as log]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.model.normalize :as normalize]))

;; ---------------------------------------------------------------------------
;; Statuses
;; ---------------------------------------------------------------------------

(def draft     "draft")
(def published "published")
(def archived  "archived")

(def statuses #{draft published archived})

(defn status-label [s]
  (case s
    "published" "Published"
    "archived"  "Archived"
    "Draft"))

;; ---------------------------------------------------------------------------
;; Type registry
;; ---------------------------------------------------------------------------

(def types
  "The five editable content types.

  :legacy-published? marks the tables that still carry the vestigial
  `published` column — `post` never had one."
  {:post    {:table :post    :label "Post"    :title-col :title
             :order [[:created_at :desc]]  :legacy-published? false}
   :event   {:table :event   :label "Event"   :title-col :title
             :order [[:start_at :desc]]    :legacy-published? true}
   :feature {:table :feature :label "Section" :title-col :title
             :order [[:sort_order :asc] [:created_at :desc]] :legacy-published? true}
   :page    {:table :page    :label "Page"    :title-col :title
             :order [[:slug :asc]]         :legacy-published? true}
   :sermon  {:table :sermon  :label "Sermon"  :title-col :title
             :order [[:sermon_date :desc]] :legacy-published? true}})

(defn spec [type]
  (or (get types type)
      (throw (ex-info "Unknown content type" {:type type :known (keys types)}))))

(defn- exec [ctx honey]
  (normalize/snake-keys-all (biff.sqlite/execute ctx honey)))

;; ---------------------------------------------------------------------------
;; Reading
;; ---------------------------------------------------------------------------

(defn ls
  "Rows of one content type, with unqualified snake_case keys.

  opts:
    :status  a status string, or a set of them; omit for everything
    :where   an extra HoneySQL predicate, ANDed in
    :order   overrides the type's default ordering
    :limit   row cap"
  ([ctx type] (ls ctx type nil))
  ([ctx type {:keys [status where order limit]}]
   (let [{:keys [table] :as s} (spec type)
         preds (cond-> []
                 (string? status) (conj [:= :status status])
                 (set? status)    (conj [:in :status (vec status)])
                 where            (conj where))]
     (exec ctx (cond-> {:select :* :from table :order-by (or order (:order s))}
                 (seq preds) (assoc :where (if (= 1 (count preds))
                                             (first preds)
                                             (into [:and] preds)))
                 limit       (assoc :limit limit))))))

(defn live
  "Published rows only — what the public site should ever see."
  ([ctx type] (live ctx type nil))
  ([ctx type opts] (ls ctx type (assoc opts :status published))))

(defn get-one [ctx type id]
  (first (exec ctx {:select :* :from (:table (spec type))
                    :where [:= :id id] :limit 1})))

(defn counts-by-status
  "{\"draft\" 3 \"published\" 12} for one type — for pane headers and badges."
  [ctx type]
  (into {} (map (juxt :status :n))
        (exec ctx {:select [:status [:%count.id :n]]
                   :from   (:table (spec type))
                   :group-by [:status]})))

;; ---------------------------------------------------------------------------
;; State transitions
;; ---------------------------------------------------------------------------

(defn- set-status!
  "Writes `status` and, where the column still exists, the mirrored `published`
  int. `extra` supplies the timestamp columns for the specific transition."
  [ctx type id status extra]
  (let [{:keys [table legacy-published?]} (spec type)]
    (exec ctx {:update table
               :set    (cond-> (assoc extra :status status)
                         legacy-published? (assoc :published (if (= status published) 1 0)))
               :where  [:= :id id]})
    id))

(defn publish!
  "Makes an item live. `published_at` records when it FIRST went live, so
  re-publishing something that was pulled back keeps the original date — and on
  `post`, where that date is editorial and chosen by hand, an existing value is
  never overwritten."
  [ctx type id]
  (let [row (get-one ctx type id)]
    (set-status! ctx type id published
                 (cond-> {:archived_at nil}
                   (nil? (:published_at row)) (assoc :published_at (normalize/now-epoch))))))

(defn unpublish!
  "Back to draft. Keeps `published_at` — it is a record of when the item ran,
  not a flag, and the editor may well publish it again."
  [ctx type id]
  (set-status! ctx type id draft {:archived_at nil}))

(defn archive!
  "The replacement for Delete. The row stays; it just leaves the site and the
  panes, and shows up under Archive with a Restore button."
  [ctx type id]
  (set-status! ctx type id archived {:archived_at (normalize/now-epoch)}))

(defn restore!
  "Out of the archive, back to draft — never straight to published, so that
  restoring something can't put it on the site by surprise."
  [ctx type id]
  (set-status! ctx type id draft {:archived_at nil}))

(defn toggle!
  "What the status pill does when clicked: published <-> draft. Archived items
  are left alone — they go back through `restore!`."
  [ctx type id]
  (let [row (get-one ctx type id)]
    (case (:status row)
      "published" (unpublish! ctx type id)
      "archived"  id
      (publish! ctx type id))))

(defn purge!
  "A real DELETE, reachable only from the archive screen and only for rows that
  are already archived. Returns nil without deleting anything otherwise."
  [ctx type id]
  (let [{:keys [table]} (spec type)
        row (get-one ctx type id)]
    (when (= archived (:status row))
      (exec ctx {:delete-from table :where [:= :id id]})
      id)))

;; ---------------------------------------------------------------------------
;; Writing
;; ---------------------------------------------------------------------------

(defn defaults
  "Column defaults every newly created row needs. New content is always a draft:
  nothing reaches the public site without somebody choosing to publish it."
  [type]
  (cond-> {:status draft}
    (:legacy-published? (spec type)) (assoc :published 0)))

(defn save!
  "Inserts or updates one row from a map of column values.

  On insert, `defaults` are merged UNDER the caller's map, so a caller that
  knows what it wants can still say so. On update, status columns are dropped
  outright — publish state changes only through the transitions above, never as
  a side effect of saving an edit. That is the same rule the importer follows."
  [ctx type id cols]
  (let [{:keys [table]} (spec type)]
    (if (get-one ctx type id)
      (exec ctx {:update table
                 :set    (apply dissoc cols [:id :status :published :published_at :archived_at])
                 :where  [:= :id id]})
      (exec ctx {:insert-into table
                 :values [(merge (defaults type) cols {:id id})]}))
    id))

;; ---------------------------------------------------------------------------
;; Migration
;; ---------------------------------------------------------------------------

(def ^:private backfill-sql
  "Derives `status` for rows written before the column existed.

  Every statement is guarded by `status IS NULL`, which is what makes this safe
  to run on every boot: a row is backfilled exactly once, and a row created
  afterwards — which always has a status — is never touched.

  That guard is also why the column is nullable with no default. Give it
  `DEFAULT 'draft'` and sqlite3def would fill every existing row with 'draft' on
  the way up, which on the live server means the whole site goes blank."
  ["UPDATE feature SET status = CASE published WHEN 1 THEN 'published' ELSE 'draft' END WHERE status IS NULL;"
   "UPDATE event   SET status = CASE published WHEN 1 THEN 'published' ELSE 'draft' END WHERE status IS NULL;"
   "UPDATE page    SET status = CASE published WHEN 1 THEN 'published' ELSE 'draft' END WHERE status IS NULL;"
   "UPDATE sermon  SET status = CASE published WHEN 1 THEN 'published' ELSE 'draft' END WHERE status IS NULL;"
   ;; post never had a published column — a NULL published_at WAS the draft state
   "UPDATE post    SET status = CASE WHEN published_at IS NOT NULL THEN 'published' ELSE 'draft' END WHERE status IS NULL;"
   ;; published_at now means 'when this first went live'. For the four tables
   ;; that never had one, the creation date is the closest true answer.
   "UPDATE feature SET published_at = created_at  WHERE published_at IS NULL AND status = 'published';"
   "UPDATE event   SET published_at = created_at  WHERE published_at IS NULL AND status = 'published';"
   "UPDATE page    SET published_at = updated_at  WHERE published_at IS NULL AND status = 'published';"
   "UPDATE sermon  SET published_at = created_at  WHERE published_at IS NULL AND status = 'published';"])

(defn backfill!
  "Runs the status backfill. Idempotent — safe on every boot, and a no-op once
  every row has a status. Returns the number of rows it touched."
  [ctx]
  (reduce (fn [n sql]
            (+ n (or (:next.jdbc/update-count (first (biff.sqlite/execute ctx [sql]))) 0)))
          0
          backfill-sql))

(defn use-status-backfill
  "Biff component. MUST be ordered after `biff.sqlite/use-sqlite`, which is what
  creates the `status` column in the first place."
  [ctx]
  (let [n (backfill! ctx)]
    (when (pos? n)
      (log/info "Backfilled status on" n "content rows."))
    ctx))
