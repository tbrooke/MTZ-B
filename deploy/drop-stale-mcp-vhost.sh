#!/usr/bin/env bash
# Remove the two dead `location /mcp` blocks from the mtzcg.com vhost.
#
# They proxy to 127.0.0.1:8085 — the mesh-MCP server retired with Alfresco.
# Nothing listens there, so every request to /mcp returns 502 before it can
# reach the app. Deleting them lets /mcp fall through to `location /`, which
# already points at the flip-controlled upstream, so MCP follows a flip or a
# rollback without a second place to keep in step.
set -euo pipefail

CONF=/etc/nginx/sites-enabled/mtzcg.com
BACKUP="${CONF}.bak-$(date +%Y%m%d-%H%M%S)"

cp -a "$CONF" "$BACKUP"
echo "  backup: $BACKUP"

python3 - "$CONF" <<'PY'
import re, sys
p = sys.argv[1]
s = open(p).read()
block = re.compile(r'[ \t]*# MCP endpoint\n[ \t]*location /mcp \{.*?\n[ \t]*\}\n\n', re.S)
new, n = block.subn('', s)
if n == 0:
    print("  no /mcp blocks found - nothing to do"); sys.exit(0)
if 'location /mcp' in new:
    print("  ERROR: a /mcp block survived the edit; aborting"); sys.exit(1)
open(p, 'w').write(new)
print(f"  removed {n} /mcp block(s)")
PY

echo "  --- nginx -t ---"
if nginx -t; then
  systemctl reload nginx
  echo "  reloaded"
else
  echo "  CONFIG INVALID - restoring $BACKUP and leaving nginx untouched"
  cp -a "$BACKUP" "$CONF"
  exit 1
fi
