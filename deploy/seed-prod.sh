#!/usr/bin/env bash
#
# Copy THIS MACHINE'S database up to the server as production's starting point.
#
#   deploy/seed-prod.sh            seed an empty production database
#   deploy/seed-prod.sh --force    overwrite one that already has content
#
# Run from the repo root on the laptop. This is a one-time seeding step, not a
# sync: after this, dev and production are independent and content is authored
# in production.
#
# It refuses to clobber a production database that already holds rows unless
# --force is given, and always takes a timestamped backup of whatever it is
# about to replace.

set -euo pipefail

REMOTE="${REMOTE:-tmb@trust}"
APP_DIR="${APP_DIR:-/home/tmb/MTZ-B}"
LOCAL_DB="${LOCAL_DB:-storage/sqlite/main.db}"
REMOTE_DB="$APP_DIR/storage/sqlite/main.db"
CONTAINER_UID=10001          # must match the `mtz` user in the Dockerfile
FORCE=0
[ "${1:-}" = "--force" ] && FORCE=1

die() { echo "ERROR: $*" >&2; exit 1; }

[ -f "$LOCAL_DB" ] || die "no local database at $LOCAL_DB"

echo "==> Local database"
local_rows=$(sqlite3 "$LOCAL_DB" "
  SELECT (SELECT COUNT(*) FROM event) || ' events, ' ||
         (SELECT COUNT(*) FROM post)  || ' posts, ' ||
         (SELECT COUNT(*) FROM sermon)|| ' sermons, ' ||
         (SELECT COUNT(*) FROM page)  || ' pages, ' ||
         (SELECT COUNT(*) FROM feature)|| ' features';")
echo "    $LOCAL_DB — $local_rows"

# A plain cp of a WAL database can capture a torn snapshot. .backup is the
# only safe way to copy one that may be in use.
snapshot=$(mktemp -t mtz-seed).db
trap 'rm -f "$snapshot"' EXIT
sqlite3 "$LOCAL_DB" ".backup '$snapshot'"
echo "    snapshot taken (WAL-safe)"

echo "==> Checking production"
remote_state=$(ssh "$REMOTE" "
  if [ -f '$REMOTE_DB' ]; then
    n=\$(sqlite3 '$REMOTE_DB' 'SELECT COUNT(*) FROM event;' 2>/dev/null || echo 0)
    echo \"exists:\$n\"
  else
    echo 'absent:0'
  fi")
remote_rows="${remote_state##*:}"

case "$remote_state" in
  absent:*)
    echo "    no production database yet — safe to seed"
    ;;
  exists:0)
    echo "    production database exists but is empty — safe to seed"
    ;;
  *)
    echo "    production database already holds $remote_rows events"
    if [ "$FORCE" -ne 1 ]; then
      die "refusing to overwrite live content. Re-run with --force if that is really what you want."
    fi
    echo "    --force given, continuing"
    ;;
esac

# Stop the app before swapping the file underneath it.
was_running=$(ssh "$REMOTE" "cd '$APP_DIR' 2>/dev/null && docker compose ps -q mtz-b 2>/dev/null | grep -q . && echo yes || echo no")
if [ "$was_running" = "yes" ]; then
  echo "==> Stopping the container"
  ssh "$REMOTE" "cd '$APP_DIR' && docker compose stop mtz-b" >/dev/null
fi

echo "==> Copying"
# storage/ belongs to uid $CONTAINER_UID, so the ssh user cannot write into it —
# not the database, not a backup, not even a temp file. Stage the upload in the
# repo root (which the ssh user does own) and let a throwaway root container do
# the placement, the same trick used for the chown.
scp -q "$snapshot" "$REMOTE:$APP_DIR/.seed-incoming.db"

echo "==> Placing"
stamp=$(date +%Y%m%d-%H%M%S)
ssh "$REMOTE" "docker run --rm \
  -v '$APP_DIR:/app' \
  -e ROWS='$remote_rows' -e STAMP='$stamp' -e OWNER='$CONTAINER_UID' \
  alpine:3.20 sh -c '
    set -e
    mkdir -p /app/storage/sqlite /app/storage/uploads
    # Only worth backing up if it actually holds content; an empty file is noise.
    if [ -f /app/storage/sqlite/main.db ] && [ \"\$ROWS\" -gt 0 ]; then
      cp /app/storage/sqlite/main.db \
         /app/storage/sqlite/main.db.replaced-\$STAMP
    fi
    # Stale -wal/-shm belong to the OLD database; left in place SQLite would try
    # to replay them against the new file.
    rm -f /app/storage/sqlite/main.db-wal /app/storage/sqlite/main.db-shm
    mv /app/.seed-incoming.db /app/storage/sqlite/main.db
    chown -R \$OWNER:\$OWNER /app/storage
  '" >/dev/null

if [ "$was_running" = "yes" ]; then
  echo "==> Restarting the container"
  ssh "$REMOTE" "cd '$APP_DIR' && docker compose start mtz-b" >/dev/null
fi

echo "==> Verifying"
# Read it the way the app does — through the app image, which runs as uid
# $CONTAINER_UID and ships sqlite3. The ssh user cannot open the file at all now.
remote_after=$(ssh "$REMOTE" "docker run --rm \
  -v '$APP_DIR/storage:/app/storage' --entrypoint sqlite3 mtz-b:latest \
  /app/storage/sqlite/main.db \"
    SELECT (SELECT COUNT(*) FROM event) || ' events, ' ||
           (SELECT COUNT(*) FROM post)  || ' posts, ' ||
           (SELECT COUNT(*) FROM sermon)|| ' sermons, ' ||
           (SELECT COUNT(*) FROM page)  || ' pages, ' ||
           (SELECT COUNT(*) FROM feature)|| ' features';\" 2>/dev/null" \
  || echo "unreadable")

echo "    local      : $local_rows"
echo "    production : $remote_after"
if [ "$local_rows" = "$remote_after" ]; then
  echo
  echo "Seeded. Dev and production are now independent — content is authored in"
  echo "production from here, and this script should not need running again."
else
  echo
  echo "WARNING: counts differ. Inspect before relying on this." >&2
  exit 1
fi
