-- Auto-generated; do not edit.

CREATE TABLE biff_sqlite_kv (
  key_ TEXT NOT NULL,
  namespace TEXT NOT NULL,
  value_ BLOB NOT NULL,
  PRIMARY KEY(namespace, key_)
) STRICT;

CREATE TABLE user (
  id BLOB PRIMARY KEY NOT NULL,
  email TEXT NOT NULL,
  joined_at INT NOT NULL,
  UNIQUE(email)
) STRICT;