-- Auto-generated; do not edit.

CREATE TABLE biff_auth_signin (
  id BLOB PRIMARY KEY NOT NULL,
  code TEXT NOT NULL,
  created_at INT NOT NULL,
  email TEXT NOT NULL,
  failed_attempts INT NOT NULL,
  params TEXT,
  UNIQUE(email)
) STRICT;

CREATE TABLE user (
  id BLOB PRIMARY KEY NOT NULL,
  email TEXT NOT NULL,
  joined_at INT NOT NULL
) STRICT;

CREATE UNIQUE INDEX IF NOT EXISTS idx_user_email ON user(email);