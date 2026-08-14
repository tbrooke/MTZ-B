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
APP_DIR="${APP_DIR:-/home/tmb/mtz-b}"
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

if [ "${remote_state%%:*}" = "exists" ]; then
  echo "==> Backing up the database being replaced"
  ssh "$REMOTE" "cp '$REMOTE_DB' '$REMOTE_DB.replaced-\$(date +%Y%m%d-%H%M%S)'"
fi

echo "==> Copying"
ssh "$REMOTE" "mkdir -p '$APP_DIR/storage/sqlite' '$APP_DIR/storage/uploads'"
scp -q "$snapshot" "$REMOTE:$REMOTE_DB.incoming"
# Remove stale -wal/-shm belonging to the old database, or SQLite will try to
# replay them against the new file.
ssh "$REMOTE" "
  rm -f '$REMOTE_DB-wal' '$REMOTE_DB-shm'
  mv '$REMOTE_DB.incoming' '$REMOTE_DB'
"

echo "==> Fixing ownership"
# The container runs as uid $CONTAINER_UID; a bind mount keeps host ownership,
# so without this the app cannot write and every save fails at runtime.
ssh -t "$REMOTE" "sudo chown -R $CONTAINER_UID:$CONTAINER_UID '$APP_DIR/storage'"

if [ "$was_running" = "yes" ]; then
  echo "==> Restarting the container"
  ssh "$REMOTE" "cd '$APP_DIR' && docker compose start mtz-b" >/dev/null
fi

echo "==> Verifying"
remote_after=$(ssh "$REMOTE" "sudo -n cat /dev/null 2>/dev/null; sqlite3 '$REMOTE_DB' \"
  SELECT (SELECT COUNT(*) FROM event) || ' events, ' ||
         (SELECT COUNT(*) FROM post)  || ' posts, ' ||
         (SELECT COUNT(*) FROM sermon)|| ' sermons, ' ||
         (SELECT COUNT(*) FROM page)  || ' pages, ' ||
         (SELECT COUNT(*) FROM feature)|| ' features';\" 2>/dev/null" || echo "unreadable")

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
