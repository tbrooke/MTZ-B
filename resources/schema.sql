-- Auto-generated; do not edit.

CREATE TABLE biff_sqlite_kv (
  id BLOB PRIMARY KEY NOT NULL,
  key_ TEXT NOT NULL,
  namespace TEXT NOT NULL,
  value_ BLOB NOT NULL,
  UNIQUE(namespace, key_)
) STRICT;

CREATE TABLE user (
  id BLOB PRIMARY KEY NOT NULL,
  email TEXT NOT NULL,
  joined_at INT NOT NULL,
  password_hash TEXT,
  UNIQUE(email)
) STRICT;

CREATE TABLE IF NOT EXISTS feature (
      id TEXT PRIMARY KEY NOT NULL,
      placement TEXT NOT NULL,
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
    ) STRICT;
CREATE TABLE IF NOT EXISTS post (
      id TEXT PRIMARY KEY NOT NULL,
      slug TEXT NOT NULL,
      title TEXT NOT NULL,
      excerpt TEXT,
      body TEXT NOT NULL DEFAULT '',
      image_id TEXT,
      published_at INTEGER,
      created_at INTEGER NOT NULL,
      UNIQUE(slug)
    ) STRICT;
CREATE TABLE IF NOT EXISTS event (
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
      published INTEGER NOT NULL DEFAULT 1,
      created_at INTEGER NOT NULL
    ) STRICT;
CREATE TABLE IF NOT EXISTS page (
      id TEXT PRIMARY KEY NOT NULL,
      slug TEXT NOT NULL,
      title TEXT,
      body TEXT NOT NULL DEFAULT '',
      updated_at INTEGER NOT NULL,
      UNIQUE(slug)
    ) STRICT;