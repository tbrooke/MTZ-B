#!/usr/bin/env bash
#
# One-time server setup. RUN THIS ON trust, not on the laptop.
#
#   ssh tmb@trust
#   git clone https://github.com/tbrooke/MTZ-B.git ~/MTZ-B
#   cd ~/MTZ-B && ./deploy/bootstrap.sh
#
# The directory name does not matter — this script locates itself.
#
# Safe to re-run: every step checks before acting, and nothing here touches the
# running Alfresco site or the live nginx configuration. Making the new site
# public is a separate, deliberate step (deploy/flip.sh).

set -euo pipefail

APP_DIR="$(cd "$(dirname "$0")/.." && pwd)"
CONTAINER_UID=10001
NEW_PORT=3100

echo "MTZ-B server bootstrap"
echo "  directory: $APP_DIR"
echo

# ── 1. storage ───────────────────────────────────────────────────────────────
echo "==> storage/"
mkdir -p "$APP_DIR/storage/sqlite" "$APP_DIR/storage/uploads"
# A bind mount keeps host ownership, and the container runs unprivileged as
# uid $CONTAINER_UID. Without this the app cannot write and saves fail at runtime.
if [ "$(stat -c %u "$APP_DIR/storage")" != "$CONTAINER_UID" ]; then
  echo "    chowning to $CONTAINER_UID"
  # The docker daemon already runs as root, so a throwaway container can do this
  # without sudo. That matters when the script runs over a non-interactive ssh,
  # where a sudo password prompt would simply hang.
  if sudo -n true 2>/dev/null; then
    sudo chown -R "$CONTAINER_UID:$CONTAINER_UID" "$APP_DIR/storage"
  elif docker info >/dev/null 2>&1; then
    docker run --rm -v "$APP_DIR/storage:/s" alpine:3.20 \
      chown -R "$CONTAINER_UID:$CONTAINER_UID" /s
  else
    echo "    need either passwordless sudo or docker. Run by hand:" >&2
    echo "      sudo chown -R $CONTAINER_UID:$CONTAINER_UID $APP_DIR/storage" >&2
    exit 1
  fi
fi
echo "    ok — owned by uid $(stat -c %u "$APP_DIR/storage")"

# ── 2. secrets ───────────────────────────────────────────────────────────────
echo "==> config.env"
if [ -f "$APP_DIR/config.env" ]; then
  chmod 600 "$APP_DIR/config.env"
  echo "    present ($(grep -cE '^[A-Z_0-9]+=' "$APP_DIR/config.env") keys), mode 600"
else
  cat <<EOF
    MISSING.

    1Password's CLI cannot authenticate on Linux (it integrates with the
    desktop app), so render it on the Mac and copy it here:

        op inject -i config.prod.env.tpl -o config.prod.env -f
        scp config.prod.env $(whoami)@$(hostname):$APP_DIR/config.env

    Then re-run this script.
EOF
  exit 1
fi

# ── 3. build ─────────────────────────────────────────────────────────────────
echo "==> Building the image (first run takes a few minutes)"
cd "$APP_DIR"
docker compose build

# ── 4. start ─────────────────────────────────────────────────────────────────
echo "==> Starting on 127.0.0.1:$NEW_PORT"
docker compose up -d

printf "    waiting for health"
for _ in $(seq 1 40); do
  if curl -fsS -o /dev/null "http://127.0.0.1:$NEW_PORT/_biff/admin/health" 2>/dev/null; then
    echo " — healthy"
    break
  fi
  printf "."
  sleep 3
done

if ! curl -fsS -o /dev/null "http://127.0.0.1:$NEW_PORT/_biff/admin/health" 2>/dev/null; then
  echo
  echo "    did not become healthy. Recent logs:" >&2
  docker compose logs --tail=40
  exit 1
fi

# ── 5. what is left ──────────────────────────────────────────────────────────
db_events=$(sqlite3 "$APP_DIR/storage/sqlite/main.db" "SELECT COUNT(*) FROM event;" 2>/dev/null || echo 0)

cat <<EOF

Running on 127.0.0.1:$NEW_PORT — the public site is UNCHANGED and still served
by the old Alfresco container on :3000.

  database   : $db_events events
EOF

if [ "$db_events" -eq 0 ]; then
  cat <<'EOF'

  The database is empty. Seed it once from the laptop:

      deploy/seed-prod.sh
EOF
fi

cat <<EOF

  Check it privately over the SSH tunnel, without exposing it:

      ssh -L 8099:127.0.0.1:$NEW_PORT $(whoami)@$(hostname)
      # then open http://localhost:8099

  Wire nginx to the switchable upstream (one time, needs sudo):

      sudo cp $APP_DIR/deploy/nginx/mtz-upstream.conf /etc/nginx/conf.d/
      sudo sed -i 's|proxy_pass http://127.0.0.1:3000|proxy_pass http://mtz_site|g' \\
        /etc/nginx/sites-available/mtzcg.com
      sudo nginx -t && sudo systemctl reload nginx

  Then, when you are ready for the public to see it:

      sudo $APP_DIR/deploy/flip.sh new      # go live
      sudo $APP_DIR/deploy/flip.sh old      # back to Alfresco, seconds
EOF
