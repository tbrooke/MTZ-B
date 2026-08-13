REMOTE    := tmb@trust
APP_DIR   := /home/tmb/mtz-b
COMPOSE   := docker compose

# Local development ──────────────────────────────────────────────────────────

.PHONY: css js build test dev
css:   ; clj -M:run css
js:    ; npm run build
build: js css ; clj -T:build uber
test:  ; clj -M:run test
dev:   ; clj -M:run dev

# Content import (runs wherever the database is) ─────────────────────────────

.PHONY: import import-apply contract
import:       ; clj -M:run import
import-apply: ; clj -M:run import --apply
contract:     ; clj -M:run content-doc

# Deployment ─────────────────────────────────────────────────────────────────
#
# The server builds from its own git checkout, so a deploy is: push, pull,
# rebuild, restart. The database lives in $(APP_DIR)/storage on the host and is
# never touched by any of this.

.PHONY: push deploy logs ps restart down
push:
	git push origin master

## deploy: pull + rebuild + restart the container (does NOT change what is public)
deploy:
	ssh $(REMOTE) 'set -e; cd $(APP_DIR) && \
	  git pull --ff-only && \
	  $(COMPOSE) build && \
	  $(COMPOSE) up -d && \
	  echo "--- waiting for health ---" && \
	  for i in $$(seq 1 40); do \
	    if curl -fsS -o /dev/null http://127.0.0.1:3100/_biff/admin/health; then echo "healthy"; exit 0; fi; \
	    sleep 3; \
	  done; \
	  echo "did not become healthy in 120s"; $(COMPOSE) logs --tail=40; exit 1'

logs:    ; ssh $(REMOTE) 'cd $(APP_DIR) && $(COMPOSE) logs -f --tail=100'
ps:      ; ssh $(REMOTE) 'cd $(APP_DIR) && $(COMPOSE) ps'
restart: ; ssh $(REMOTE) 'cd $(APP_DIR) && $(COMPOSE) restart'
down:    ; ssh $(REMOTE) 'cd $(APP_DIR) && $(COMPOSE) down'

# The flip ───────────────────────────────────────────────────────────────────
#
# Deploying and going live are separate on purpose: `deploy` gets the new
# version running and health-checked on :3100 while the public site still
# serves the old one. `flip` is the moment it goes public.

.PHONY: status flip rollback
status:   ; ssh $(REMOTE) '$(APP_DIR)/deploy/flip.sh status'
flip:     ; ssh -t $(REMOTE) 'sudo $(APP_DIR)/deploy/flip.sh new'
rollback: ; ssh -t $(REMOTE) 'sudo $(APP_DIR)/deploy/flip.sh old'

# Backups ────────────────────────────────────────────────────────────────────
#
# Dev and production databases are deliberately independent — there is no sync.
# This is a backup, not a sync: a WAL-safe snapshot pulled to ./backups/.

.PHONY: backup-prod
backup-prod:
	@mkdir -p backups
	@ts=$$(date +%Y%m%d-%H%M%S); \
	ssh $(REMOTE) 'sqlite3 $(APP_DIR)/storage/sqlite/main.db ".backup /tmp/mtz-backup.db"' && \
	scp -q $(REMOTE):/tmp/mtz-backup.db backups/prod-$$ts.db && \
	ssh $(REMOTE) 'rm -f /tmp/mtz-backup.db' && \
	echo "  saved backups/prod-$$ts.db"

# Legacy ─────────────────────────────────────────────────────────────────────
# The pre-Docker systemd deploy, kept until the container path is proven.

.PHONY: deploy-systemd-legacy
deploy-systemd-legacy: build
	scp target/mtzion.jar $(REMOTE):/home/tmb/mtz/mtzion.jar.new
	ssh $(REMOTE) "mv /home/tmb/mtz/mtzion.jar.new /home/tmb/mtz/mtzion.jar && \
	  sudo systemctl restart mtzion && sudo systemctl status mtzion --no-pager"
