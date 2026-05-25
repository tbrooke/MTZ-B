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
      sort_order INTEGER NOT NULL DEFAULT 0,
      published INTEGER NOT NULL DEFAULT 1,
      updated_at INTEGER NOT NULL,
      created_at INTEGER NOT NULL
    ) STRICT;"
   "CREATE TABLE IF NOT EXISTS post (
      id TEXT PRIMARY KEY NOT NULL,
      slug TEXT NOT NULL,
      title TEXT NOT NULL,
      excerpt TEXT,
      body TEXT NOT NULL DEFAULT '',
      image_id TEXT,
      show_on_home INTEGER NOT NULL DEFAULT 0,
      published_at INTEGER,
      created_at INTEGER NOT NULL,
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
      created_at INTEGER NOT NULL
    ) STRICT;"
   "CREATE TABLE IF NOT EXISTS page (
      id TEXT PRIMARY KEY NOT NULL,
      slug TEXT NOT NULL,
      title TEXT,
      nav_label TEXT,
      nav_order INTEGER,
      body TEXT NOT NULL DEFAULT '',
      published INTEGER NOT NULL DEFAULT 1,
      updated_at INTEGER NOT NULL,
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
   "CREATE TABLE IF NOT EXISTS sermon (
      id TEXT PRIMARY KEY NOT NULL,
      title TEXT NOT NULL,
      sermon_date INTEGER,
      scripture TEXT,
      description TEXT NOT NULL DEFAULT '',
      video_id TEXT,
      bulletin_path TEXT,
      presentation_path TEXT,
      published INTEGER NOT NULL DEFAULT 1,
      created_at INTEGER NOT NULL
    ) STRICT;"])

(defn init [_modules-var]
  {:biff.sqlite/columns columns
   :biff.sqlite/extra-sql extra-sql
   :biff.sqlite/authorize #'authorize})

(def module
  {:biff.core/init #'init
   :biff.graph/resolvers (biff.sqlite/make-resolvers
                          {:biff.sqlite/columns columns})})
