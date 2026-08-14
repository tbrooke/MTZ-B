#!/usr/bin/env bash
#
# One-time nginx wiring so deploy/flip.sh has something to switch.
#
#   sudo deploy/wire-nginx.sh            show what would change
#   sudo deploy/wire-nginx.sh --apply    make the change
#
# Installs the shared upstream and repoints ONLY the mtzcg.com server block at
# it. mtzionchinagrove.com is left pinned to :3000 on purpose, so the previous
# build stays reachable while content is still being pulled out of it.
#
# This does not change which app is live: the upstream ships pointing at 3100,
# so applying this IS the go-live. Use deploy/flip.sh old to go back.

set -euo pipefail

SITE=/etc/nginx/sites-available/mtzcg.com
UPSTREAM_FILE=/etc/nginx/conf.d/mtz-upstream.conf
HERE="$(cd "$(dirname "$0")" && pwd)"
DOMAIN="mtzcg.com"
APPLY=0
[ "${1:-}" = "--apply" ] && APPLY=1

die() { echo "ERROR: $*" >&2; exit 1; }

[ -f "$SITE" ] || die "$SITE not found"

# Find the proxy_pass inside the server block whose server_name is mtzcg.com.
# Matching on the block rather than a line number, so an unrelated edit to the
# file cannot silently retarget the wrong domain.
target_line=$(awk -v d="$DOMAIN" '
  /server_name/           { inblock = ($0 ~ d) }
  inblock && /location \/ / { inloc = 1 }
  inloc && /proxy_pass http:\/\/127\.0\.0\.1:3000;/ { print NR; exit }
' "$SITE")

[ -n "$target_line" ] || {
  if grep -q "proxy_pass http://mtz_site;" "$SITE"; then
    echo "Already wired — $DOMAIN proxies to mtz_site."
    echo "Use deploy/flip.sh to switch which port that upstream points at."
    exit 0
  fi
  die "could not find $DOMAIN's proxy_pass to 127.0.0.1:3000 in $SITE"
}

echo "Wiring $DOMAIN to the mtz_site upstream"
echo "  $SITE line $target_line:"
sed -n "${target_line}p" "$SITE" | sed 's/^/    /'
echo "  becomes:"
sed -n "${target_line}s|http://127.0.0.1:3000|http://mtz_site|p" "$SITE" | sed 's/^/    /'
echo
echo "  other proxy_pass lines left untouched:"
grep -n "proxy_pass" "$SITE" | grep -v "^${target_line}:" | sed 's/^/    /'

if [ "$APPLY" -ne 1 ]; then
  echo
  echo "Dry run. Re-run with --apply to make the change."
  exit 0
fi

[ "$(id -u)" -eq 0 ] || die "needs root to write nginx config. Re-run with sudo."

backup="${SITE}.bak-$(date +%Y%m%d-%H%M%S)"
cp "$SITE" "$backup"
echo
echo "==> Backed up to $backup"

cp "$HERE/nginx/mtz-upstream.conf" "$UPSTREAM_FILE"
echo "==> Installed $UPSTREAM_FILE"

sed -i "${target_line}s|http://127.0.0.1:3000|http://mtz_site|" "$SITE"
echo "==> Repointed $DOMAIN"

if ! nginx -t 2>&1 | sed 's/^/    /'; then
  echo "nginx rejected the config — restoring and aborting." >&2
  cp "$backup" "$SITE"
  rm -f "$UPSTREAM_FILE"
  exit 3
fi

systemctl reload nginx
echo "==> Reloaded"
echo
"$HERE/flip.sh" status
