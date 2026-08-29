(ns com.mtzion.content.inbox
  "The review queue between the bulletin scan and the site.

  The importer used to write content rows directly. Everything it produced
  arrived as a draft, which was safe, but it scattered across three screens and
  looked identical to things somebody had written by hand — the only record that
  a batch had arrived at all was a receipt file on disk.

  Now `--apply` stages here instead, and accepting an item is what writes the
  content row. Every guarantee the importer made survives, because the same
  code runs; it just runs later, one item at a time, from a button.

  ## The plan is rebuilt, never frozen

  Only the validated EDN item is stored. `plan/plan-item` runs again when the
  item is displayed and again when it is accepted, so matching sees the database
  as it is at that moment. Freeze the plan at drop time and a row somebody
  created by hand on Saturday would be duplicated rather than adopted on Sunday."
  (:require [clojure.edn :as edn]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.biffweb.sqlite.impl.execute :as biff.execute]
            [com.mtzion.content.plan :as plan]
            [com.mtzion.model.normalize :as norm]
            [honey.sql :as hsql]
            [next.jdbc :as jdbc]))

(def new-state       "new")
(def accepted-state  "accepted")
(def dismissed-state "dismissed")

(defn- exec [ctx honey]
  (norm/snake-keys-all (biff.sqlite/execute ctx honey)))

(defn- new-id [] (str (random-uuid)))

;; ---------------------------------------------------------------------------
;; Staging
;; ---------------------------------------------------------------------------

(defn- row-for [batch-id source source-ref item]
  {:id          (new-id)
   :batch_id    batch-id
   :source      source
   :source_ref  source-ref
   :received_at (norm/now-epoch)
   :type        (name (:type item))
   :import_key  (:key item)
   :title       (or (:title item) (:slug item) "")
   :payload     (pr-str item)
   :state       new-state})

(defn stage!
  "Writes validated items into the queue, all or nothing. Returns the batch id.

  The transaction and the write lock are here for the same reason the importer
  took them: a half-staged bulletin is worse than a rejected one."
  [ctx items {:keys [source source-ref]}]
  (let [batch-id (new-id)
        rows     (mapv #(row-for batch-id (or source "bulletin") source-ref %) items)]
    (when (seq rows)
      (locking biff.execute/write-lock
        (jdbc/with-transaction [tx (:biff.sqlite/write-conn ctx)]
          (doseq [r rows]
            (jdbc/execute! tx (hsql/format {:insert-into :inbox_item :values [r]}))))))
    batch-id))

;; ---------------------------------------------------------------------------
;; Reading
;; ---------------------------------------------------------------------------

(defn item-of
  "The EDN item back out of a queue row. This is data this application wrote
  itself, but read it with the same refusal of tagged literals the importer uses
  — nothing downstream is prepared for a Date or a UUID object."
  [row]
  (edn/read-string {:default (fn [tag _]
                               (throw (ex-info (str "unsupported tag #" tag) {:tag tag})))}
                   (:payload row)))

(defn pending
  "Items still waiting, oldest batch first so the queue reads in arrival order."
  [ctx]
  (exec ctx {:select :* :from :inbox_item
             :where [:= :state new-state]
             :order-by [[:received_at :asc] [:title :asc]]}))

(defn decided
  "Recently accepted or dismissed, newest first — the record of what happened."
  [ctx limit]
  (exec ctx {:select :* :from :inbox_item
             :where [:!= :state new-state]
             :order-by [[:decided_at :desc]]
             :limit limit}))

(defn get-one [ctx id]
  (first (exec ctx {:select :* :from :inbox_item :where [:= :id id] :limit 1})))

(defn pending-count [ctx]
  (or (:n (first (exec ctx {:select [[:%count.id :n]] :from :inbox_item
                            :where [:= :state new-state]})))
      0))

(defn batches
  "Pending items grouped by the drop they came from, in arrival order."
  [ctx]
  (->> (pending ctx)
       (group-by :batch_id)
       (sort-by (fn [[_ rows]] (apply min (map :received_at rows))))))

;; ---------------------------------------------------------------------------
;; Planning and deciding
;; ---------------------------------------------------------------------------

(defn plan-for
  "What accepting this item would do, against the database as it stands now."
  [ctx row opts]
  (plan/plan-item ctx (item-of row) (or (:source_ref row) (:source row)) opts))

(defn accept!
  "Writes the content row. Returns the op that was applied, or nil if the item
  was already decided — accepting twice must not create a second row."
  [ctx row opts]
  (when (= new-state (:state row))
    (let [op (plan-for ctx row opts)]
      (plan/apply-op! #(biff.sqlite/execute ctx %) op)
      (exec ctx {:update :inbox_item
                 :set    {:state accepted-state
                          :target_id (:id op)
                          :decided_at (norm/now-epoch)}
                 :where  [:= :id (:id row)]})
      op)))

(defn dismiss!
  "Leaves the content tables alone and takes the item out of the queue. The row
  stays, so 'we looked at that and said no' is still on the record."
  [ctx row]
  (when (= new-state (:state row))
    (exec ctx {:update :inbox_item
               :set    {:state dismissed-state :decided_at (norm/now-epoch)}
               :where  [:= :id (:id row)]})
    row))

(defn accept-batch!
  "Accepts every still-pending item in one batch. Returns how many were applied."
  [ctx batch-id opts]
  (let [rows (exec ctx {:select :* :from :inbox_item
                        :where [:and [:= :batch_id batch-id] [:= :state new-state]]
                        :order-by [[:received_at :asc]]})]
    (count (keep #(accept! ctx % opts) rows))))

(defn dismiss-batch! [ctx batch-id]
  (let [rows (exec ctx {:select :* :from :inbox_item
                        :where [:and [:= :batch_id batch-id] [:= :state new-state]]})]
    (count (keep #(dismiss! ctx %) rows))))

;; ---------------------------------------------------------------------------
;; Adding by hand
;; ---------------------------------------------------------------------------

(defn stage-one!
  "A single item typed into the console rather than extracted from a bulletin —
  the graphics designer sends something over and it goes through the same
  review as everything else. Validation is the caller's job."
  [ctx item source-ref]
  (stage! ctx [item] {:source "manual" :source-ref source-ref}))
