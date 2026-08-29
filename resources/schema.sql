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
    ) STRICT;
CREATE TABLE IF NOT EXISTS post (
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
      featured INTEGER NOT NULL DEFAULT 0,
      published INTEGER NOT NULL DEFAULT 1,
      status TEXT,
      published_at INTEGER,
      archived_at INTEGER,
      created_at INTEGER NOT NULL,
      import_key TEXT,
      import_meta TEXT
    ) STRICT;
CREATE TABLE IF NOT EXISTS page (
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
    ) STRICT;
CREATE TABLE IF NOT EXISTS file (
      id TEXT PRIMARY KEY NOT NULL,
      filename TEXT NOT NULL,
      label TEXT NOT NULL DEFAULT '',
      category TEXT NOT NULL DEFAULT 'other',
      url TEXT NOT NULL,
      size_bytes INTEGER,
      file_date INTEGER,
      uploaded_at INTEGER NOT NULL
    ) STRICT;
CREATE UNIQUE INDEX IF NOT EXISTS event_title_start ON event (title, start_at);
CREATE TABLE IF NOT EXISTS event_exception (
      id TEXT PRIMARY KEY NOT NULL,
      event_id TEXT NOT NULL,
      occurrence_at INTEGER NOT NULL,
      created_at INTEGER NOT NULL,
      UNIQUE(event_id, occurrence_at)
    ) STRICT;
CREATE INDEX IF NOT EXISTS event_exception_event ON event_exception (event_id);
CREATE TABLE IF NOT EXISTS sermon (
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
    ) STRICT;
CREATE UNIQUE INDEX IF NOT EXISTS feature_import_key ON feature (import_key);
CREATE UNIQUE INDEX IF NOT EXISTS post_import_key ON post (import_key);
CREATE UNIQUE INDEX IF NOT EXISTS event_import_key ON event (import_key);
CREATE UNIQUE INDEX IF NOT EXISTS page_import_key ON page (import_key);
CREATE UNIQUE INDEX IF NOT EXISTS sermon_import_key ON sermon (import_key);