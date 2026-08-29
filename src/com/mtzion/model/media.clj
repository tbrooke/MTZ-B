(ns com.mtzion.model.media
  "The image library: a local index over Cloudflare Images.

  Cloudflare is still the store. What it is not is queryable — the old library
  screen called the API on every render, 100 images at a time, which meant no
  search, no sorting, no grouping and nothing to join against. So every image
  gets a row here, keyed by the Cloudflare id itself so the two cannot drift.

  The row is what makes an **album** possible, and an album is what makes a
  gallery possible: a section on a page can name one and render everything in
  it, without anybody choosing in advance how many photos that will be."
  (:require [clojure.string :as str]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.lib.cloudflare :as cf]
            [com.mtzion.model.normalize :as norm]))

(def categories
  [["photo"   "Photo"]
   ["graphic" "Graphic"]
   ["content" "In an article"]])

(defn- exec [ctx honey]
  (norm/snake-keys-all (biff.sqlite/execute ctx honey)))

(defn- tidy
  "Blank means no value. Without the trim, an album typed as a couple of spaces
  is stored as \"  \" and turns up in the rail as an album of its own."
  [s]
  (some-> s str str/trim not-empty))

(defn url [ctx image-id variant] (cf/delivery-url ctx image-id variant))

;; ---------------------------------------------------------------------------
;; Reading
;; ---------------------------------------------------------------------------

(defn ls
  "Images, newest first.

  opts:
    :album     an album name, or :none for images not filed in one
    :category  photo / graphic / content
    :q         matches the label
    :limit"
  ([ctx] (ls ctx nil))
  ([ctx {:keys [album category q limit]}]
   (let [preds (cond-> []
                 (= :none album)      (conj [:or [:is :album nil] [:= [:trim :album] ""]])
                 (string? album)      (conj [:= :album album])
                 (seq category)       (conj [:= :category category])
                 (seq q)              (conj [:like [:lower :label] (str "%" (str/lower-case q) "%")]))]
     (exec ctx (cond-> {:select :* :from :image
                        :order-by [[:uploaded_at :desc]]}
                 (seq preds) (assoc :where (if (= 1 (count preds)) (first preds) (into [:and] preds)))
                 limit       (assoc :limit limit))))))

(defn get-one [ctx id]
  (first (exec ctx {:select :* :from :image :where [:= :id id] :limit 1})))

(defn albums
  "Every album with a count and a cover, plus how many images are unfiled."
  [ctx]
  (let [rows (exec ctx {:select [:album [:%count.id :n]] :from :image
                        :where [:and [:is-not :album nil] [:!= [:trim :album] ""]]
                        :group-by [:album] :order-by [[:album :asc]]})]
    (mapv (fn [{:keys [album n]}]
            {:album album :n n
             :cover (:id (first (ls ctx {:album album :limit 1})))})
          rows)))

(defn unfiled-count [ctx]
  (or (:n (first (exec ctx {:select [[:%count.id :n]] :from :image
                            :where [:or [:is :album nil] [:= [:trim :album] ""]]})))
      0))

(defn total [ctx]
  (or (:n (first (exec ctx {:select [[:%count.id :n]] :from :image}))) 0))

;; ---------------------------------------------------------------------------
;; Writing
;; ---------------------------------------------------------------------------

(defn record!
  "Notes an image that already exists in Cloudflare. Idempotent on the id, so
  re-syncing never duplicates and never clobbers a label somebody has edited."
  [ctx {:keys [id label album category alt_text taken_on width height uploaded_at]}]
  (when (seq id)
    (if (get-one ctx id)
      id
      (do (exec ctx {:insert-into :image
                     :values [{:id id
                               :label (or label "")
                               :album (tidy album)
                               :category (or category "photo")
                               :alt_text (tidy alt_text)
                               :taken_on taken_on
                               :width width
                               :height height
                               :uploaded_at (or uploaded_at (norm/now-epoch))}]})
          id))))

(defn save!
  "The editable half of an image. The id, dimensions and upload time are facts
  about the file and are never touched here."
  [ctx id {:keys [label album category alt_text taken_on]}]
  (exec ctx {:update :image
             :set    {:label    (str/trim (or label ""))
                      :album    (tidy album)
                      :category (or category "photo")
                      :alt_text (tidy alt_text)
                      :taken_on taken_on}
             :where  [:= :id id]})
  id)

(defn forget!
  "Removes the row only. Used when Cloudflare no longer has the image."
  [ctx id]
  (exec ctx {:delete-from :image :where [:= :id id]}))

(defn delete!
  "Deletes from Cloudflare and from the index. Unlike content, an image really
  is deleted — it costs storage, and an unreferenced file in a CDN is not
  history worth keeping."
  [ctx id]
  (cf/delete! ctx id)
  (forget! ctx id)
  id)

;; ---------------------------------------------------------------------------
;; Sync
;; ---------------------------------------------------------------------------

(defn- from-cf
  "A Cloudflare image record -> the columns this index keeps. Metadata written
  by the old /admin upload form used the same three keys, so images uploaded
  before this table existed still arrive with their label and date."
  [img]
  (let [m (cf/image-meta img)]
    {:id       (:id img)
     :label    (or (:label m)
                   (let [f (:filename img)]
                     (when-not (str/starts-with? (or f "") "ring-multipart") f))
                   "")
     :album    (:album m)
     :category (or (:category m) "photo")
     :taken_on (some-> (:date m) norm/local-date->epoch)
     :uploaded_at (or (some-> (:uploaded img) (subs 0 10) norm/local-date->epoch)
                      (norm/now-epoch))}))

(defn sync!
  "Walks Cloudflare and indexes anything not already known. Returns how many
  rows were added.

  Additive on purpose: it never deletes rows and never overwrites an existing
  one, so running it can only ever teach the index about images it had missed."
  [ctx]
  (loop [page 1 added 0]
    (let [imgs (try (cf/list-images ctx page) (catch Exception _ nil))]
      (if (empty? imgs)
        added
        (let [n (count (keep #(record! ctx (from-cf %)) imgs))]
          (if (< (count imgs) 100)
            (+ added n)
            (recur (inc page) (+ added n))))))))
