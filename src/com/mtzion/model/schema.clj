(ns com.mtzion.model.schema
  (:require [com.biffweb.sqlite :as biff.sqlite]))

(def columns
  {:user/id {:type :uuid :primary-key true}
   :user/email {:type :text :required true :unique true}
   :user/password-hash {:type :text}
   :user/joined-at {:type :inst :required true}})

(def ^:private immutable-user-fields
  #{:user/id :user/email :user/joined-at})

(defn- session-user-id [ctx]
  (get-in ctx [:session :uid]))

(defn- allowed-user-update?
  [uid {:keys [op before after]}]
  (and (= op :update)
       (= uid (:user/id after))
       (every? (fn [field]
                 (= (get before field) (get after field)))
               immutable-user-fields)))

(defn authorize
  [ctx diff]
  (let [uid (session-user-id ctx)]
    (every?
     (fn [{:keys [table] :as entry}]
       (case table
         :user (allowed-user-update? uid entry)
         false))
     diff)))

(def extra-sql
  ["CREATE TABLE IF NOT EXISTS feature (
      id TEXT PRIMARY KEY NOT NULL,
      page_slug TEXT NOT NULL DEFAULT 'home',
      show_on_home INTEGER NOT NULL DEFAULT 0,
      title TEXT NOT NULL,
      subtitle TEXT,
      body TEXT NOT NULL DEFAULT '',
      image_id TEXT,
      cta_label TEXT,
      cta_url TEXT,
      meta TEXT,
      sort_order INTEGER NOT NULL DEFAULT 0,
      published INTEGER NOT NULL DEFAULT 1,
      status TEXT,
      published_at INTEGER,
      archived_at INTEGER,
      updated_at INTEGER NOT NULL,
      created_at INTEGER NOT NULL,
      import_key TEXT,
      import_meta TEXT
    ) STRICT;"
   "CREATE TABLE IF NOT EXISTS post (
      id TEXT PRIMARY KEY NOT NULL,
      slug TEXT NOT NULL,
      title TEXT NOT NULL,
      category TEXT NOT NULL DEFAULT 'blog',
      excerpt TEXT,
      body TEXT NOT NULL DEFAULT '',
      image_id TEXT,
      show_on_home INTEGER NOT NULL DEFAULT 0,
      author TEXT,
      published_at INTEGER,
      status TEXT,
      archived_at INTEGER,
      created_at INTEGER NOT NULL,
      import_key TEXT,
      import_meta TEXT,
      UNIQUE(slug)
    ) STRICT;"
   "CREATE TABLE IF NOT EXISTS event (
      id TEXT PRIMARY KEY NOT NULL,
      title TEXT NOT NULL,
      description TEXT NOT NULL DEFAULT '',
      location TEXT,
      start_at INTEGER NOT NULL,
      end_at INTEGER,
      all_day INTEGER NOT NULL DEFAULT 0,
      recurrence TEXT NOT NULL DEFAULT 'none',
      recur_until INTEGER,
      image_id TEXT,
      featured INTEGER NOT NULL DEFAULT 0,
      published INTEGER NOT NULL DEFAULT 1,
      status TEXT,
      published_at INTEGER,
      archived_at INTEGER,
      created_at INTEGER NOT NULL,
      import_key TEXT,
      import_meta TEXT
    ) STRICT;"
   "CREATE TABLE IF NOT EXISTS page (
      id TEXT PRIMARY KEY NOT NULL,
      slug TEXT NOT NULL,
      title TEXT,
      nav_label TEXT,
      nav_order INTEGER,
      parent_slug TEXT,
      body TEXT NOT NULL DEFAULT '',
      published INTEGER NOT NULL DEFAULT 1,
      status TEXT,
      published_at INTEGER,
      archived_at INTEGER,
      updated_at INTEGER NOT NULL,
      import_key TEXT,
      import_meta TEXT,
      UNIQUE(slug)
    ) STRICT;"
   "CREATE TABLE IF NOT EXISTS file (
      id TEXT PRIMARY KEY NOT NULL,
      filename TEXT NOT NULL,
      label TEXT NOT NULL DEFAULT '',
      category TEXT NOT NULL DEFAULT 'other',
      url TEXT NOT NULL,
      size_bytes INTEGER,
      file_date INTEGER,
      uploaded_at INTEGER NOT NULL
    ) STRICT;"
   "CREATE UNIQUE INDEX IF NOT EXISTS event_title_start ON event (title, start_at);"
   ;; One cancelled occurrence of a recurring event. A recurring event is a
   ;; single row whose occurrences are computed at render time, so "no Bible
   ;; study on the 24th" has nowhere else to live.
   ;;
   ;; occurrence_at is the epoch the occurrence WOULD have had — the value
   ;; expansion produces — which is what makes the lookup a plain map access.
   ;; Moving an occurrence rather than cancelling it is the natural extension:
   ;; add a moved_to column and emit that instead of skipping.
   "CREATE TABLE IF NOT EXISTS event_exception (
      id TEXT PRIMARY KEY NOT NULL,
      event_id TEXT NOT NULL,
      occurrence_at INTEGER NOT NULL,
      created_at INTEGER NOT NULL,
      UNIQUE(event_id, occurrence_at)
    ) STRICT;"
   "CREATE INDEX IF NOT EXISTS event_exception_event ON event_exception (event_id);"
   "CREATE TABLE IF NOT EXISTS sermon (
      id TEXT PRIMARY KEY NOT NULL,
      title TEXT NOT NULL,
      sermon_date INTEGER,
      scripture_cw TEXT,
      scripture_gospel TEXT,
      series TEXT,
      description TEXT NOT NULL DEFAULT '',
      video_id TEXT,
      bulletin_path TEXT,
      presentation_path TEXT,
      published INTEGER NOT NULL DEFAULT 1,
      status TEXT,
      published_at INTEGER,
      archived_at INTEGER,
      created_at INTEGER NOT NULL,
      import_key TEXT,
      import_meta TEXT
    ) STRICT;"
   ;; One extracted item waiting to be looked at. The importer stages here now
   ;; rather than writing content rows directly, so a bulletin drop is something
   ;; you review as a batch instead of six drafts scattered across three screens.
   ;;
   ;; `payload` is the validated EDN item, verbatim. The plan is deliberately NOT
   ;; frozen alongside it: it is rebuilt from the payload when the item is shown
   ;; and again when it is accepted, so matching still sees the database as it is
   ;; now — somebody may have created the row by hand in between.
   "CREATE TABLE IF NOT EXISTS inbox_item (
      id TEXT PRIMARY KEY NOT NULL,
      batch_id TEXT NOT NULL,
      source TEXT NOT NULL,
      source_ref TEXT,
      received_at INTEGER NOT NULL,
      type TEXT NOT NULL,
      import_key TEXT,
      title TEXT NOT NULL DEFAULT '',
      payload TEXT NOT NULL,
      state TEXT NOT NULL DEFAULT 'new',
      target_id TEXT,
      decided_at INTEGER
    ) STRICT;"
   "CREATE INDEX IF NOT EXISTS inbox_item_state ON inbox_item (state, received_at);"
   "CREATE UNIQUE INDEX IF NOT EXISTS feature_import_key ON feature (import_key);"
   "CREATE UNIQUE INDEX IF NOT EXISTS post_import_key ON post (import_key);"
   "CREATE UNIQUE INDEX IF NOT EXISTS event_import_key ON event (import_key);"
   "CREATE UNIQUE INDEX IF NOT EXISTS page_import_key ON page (import_key);"
   "CREATE UNIQUE INDEX IF NOT EXISTS sermon_import_key ON sermon (import_key);"])

(defn init [_modules-var]
  {:biff.sqlite/columns columns
   :biff.sqlite/extra-sql extra-sql
   :biff.sqlite/authorize #'authorize})

(def module
  {:biff.core/init #'init
   :biff.graph/resolvers (biff.sqlite/make-resolvers
                          {:biff.sqlite/columns columns})})
