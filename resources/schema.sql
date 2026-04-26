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
  UNIQUE(email)
) STRICT;