#!/usr/bin/env bash
#
# Switch mtzcg.com between the new app and the old Alfresco site.
#
#   sudo deploy/flip.sh new      -> 127.0.0.1:3100  (mtz-b)
#   sudo deploy/flip.sh old      -> 127.0.0.1:3000  (mtz-cms, Alfresco)
#   deploy/flip.sh status        -> what is live right now (no sudo needed)
#
# Safe by construction: it refuses to point nginx at a port nothing is
# listening on, and validates the config before reloading. A bad flip should
# be impossible; a slow one is fine.

set -euo pipefail

UPSTREAM_FILE=/etc/nginx/conf.d/mtz-upstream.conf
NEW_PORT=3100
OLD_PORT=3000

current_port() {
  grep -oE 'server 127\.0\.0\.1:[0-9]+' "$UPSTREAM_FILE" 2>/dev/null \
    | grep -oE '[0-9]+$' || echo "?"
}

label_for() {
  case "$1" in
    "$NEW_PORT") echo "mtz-b (new Biff app)" ;;
    "$OLD_PORT") echo "mtz-cms (old Alfresco site)" ;;
    *)           echo "unknown" ;;
  esac
}

port_is_live() {
  # Anything actually answering on loopback counts; the app may be starting.
  curl -fsS -o /dev/null --max-time 4 "http://127.0.0.1:$1/" 2>/dev/null
}

status() {
  local p; p="$(current_port)"
  echo "  nginx upstream : 127.0.0.1:$p  — $(label_for "$p")"
  for port in "$NEW_PORT" "$OLD_PORT"; do
    if port_is_live "$port"; then
      echo "  port $port      : responding    ($(label_for "$port"))"
    else
      echo "  port $port      : NOT responding ($(label_for "$port"))"
    fi
  done
}

case "${1:-status}" in
  status)
    status
    ;;

  new|old)
    target_port=$NEW_PORT
    [ "$1" = "old" ] && target_port=$OLD_PORT

    if [ "$(id -u)" -ne 0 ]; then
      echo "This needs root to write nginx config and reload. Re-run with sudo." >&2
      exit 1
    fi

    echo "Flipping to $(label_for "$target_port") on port $target_port"

    # 1. Refuse to send live traffic somewhere dead.
    if ! port_is_live "$target_port"; then
      echo "REFUSING: nothing is answering on 127.0.0.1:$target_port." >&2
      echo "Start it first, then flip. Nothing has been changed." >&2
      exit 2
    fi

    # 2. Keep a copy so a manual undo is always possible.
    backup="${UPSTREAM_FILE}.bak-$(date +%Y%m%d-%H%M%S)"
    cp "$UPSTREAM_FILE" "$backup"

    # 3. Rewrite, validate, reload — restoring the backup if nginx objects.
    sed -i -E "s|server 127\.0\.0\.1:[0-9]+;|server 127.0.0.1:${target_port};|" "$UPSTREAM_FILE"

    if ! nginx -t 2>/dev/null; then
      echo "nginx rejected the config — restoring and aborting." >&2
      cp "$backup" "$UPSTREAM_FILE"
      nginx -t
      exit 3
    fi

    systemctl reload nginx
    echo "Done. Previous config saved at $backup"
    echo
    status
    ;;

  *)
    echo "usage: $0 {new|old|status}" >&2
    exit 64
    ;;
esac
