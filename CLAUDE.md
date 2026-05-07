# MTZ-B — Context for Claude

## What this is

Public website and admin dashboard for **Mount Zion UCC**, a church in China Grove, NC.
Production URL: **https://mtzcg.com**

Migrated from `mtz-cms` (Alfresco + Pathom + magic-link auth) to a simpler Biff + SQLite stack.
Biff uses Reitit internally (via biff-ring); Pathom was dropped as unnecessary.

## Stack

- **Framework**: [Biff](https://biffweb.com) (biff-core, biff-ring, biff.sqlite, biff.graph, biff.admin)
- **Database**: SQLite via `biff.sqlite` + HoneySQL; schema migrations via `sqlite3def` binary
- **HTML**: `lambdaisland/hiccup` — use `[::hiccup/unsafe-html "..."]` at body level for raw JS/HTML (nesting it inside `[:script]` doesn't work — enlive won't invoke the custom emitter for child nodes)
- **CSS**: Tailwind v4 (`@import "tailwindcss"`) + hand-written mtz design system in `resources/tailwind.css`, compiled to `target/resources/public/css/main.css`
- **Auth**: BCrypt password auth (no magic links). Admin only — all public routes are unauthenticated.
- **HTMX**: served locally from `resources/public/js/htmx.min.js`

## Project structure

```
src/com/mtzion/
  app/
    auth.clj        — sign-in/sign-out routes + create-admin! REPL helper
    landing.clj     — public home page
  ui/
    base.clj        — page template (header/footer/fonts/JS); returns Ring response map
    nav.clj         — site-header, site-footer, breadcrumbs components
  lib/
    middleware.clj  — wrap-signed-in (redirects to /admin/signin)
    ui.clj          — css-path helper
  model/
    schema.clj      — SQLite column definitions

resources/
  schema.sql        — sqlite3def schema (users table with password_hash)
  tailwind.css      — Tailwind v4 input + full mtz design system CSS
  public/
    images/         — DSC01305.jpg, church-exterior.jpg, etc.
    js/htmx.min.js
```

## Design system

CSS custom properties and component classes all prefixed `--mtz-` / `.mtz-`.
Key classes: `.mtz-header`, `.mtz-header-inner`, `.mtz-wordmark`, `.mtz-logo-*`,
`.mtz-nav`, `.mtz-nav-item`, `.mtz-footer`, `.mtz-btn`, `.mtz-flip-chip`.

Header is a 4-column grid: `1fr auto 1fr auto` (left-nav | wordmark | right-nav | flip-chip).
The flip chip switches between Church and Preschool site contexts.

Logo shrink-on-scroll: JS in `base.clj` adds/removes `is-scrolled` on `#mtz-header`,
which triggers CSS transitions on `.mtz-logo-zion`, `.mtz-logo-mt`, `.mtz-logo-roof`.

Church nav (fallback): 8 items split 4+4 around the centred wordmark:
- Left: Home, About, Worship, Events
- Right: Activities, News, Outreach, Contact

## Auth flow

- `/admin/signin` GET — renders email+password form
- `/admin/signin` POST — verifies BCrypt hash, sets `{:uid user-id}` in session, redirects to `/admin`
- `/admin/signout` POST — clears session, redirects to `/`
- Create first admin from REPL: `(com.mtzion.app.auth/create-admin! @system "email@example.com" "password")`

## sqlite3def

The `sqlite3def` binary must be manually placed at `storage/sqlite3def/sqlite3def`.
Recent releases switched from `.tar.gz` to `.zip` for darwin_arm64 — download from GitHub
(sqldef v3.10.1) and extract manually if the auto-download fails with a corrupt file.

## Dev workflow

```bash
clj -M:run dev      # start dev server on :8080 with hot reload + nREPL on :7888
clj -M:test         # run tests
```

To trigger a namespace reload without restarting (if the file watcher misses a change):
connect to nREPL on port 7888 and call `(clojure.tools.namespace.repl/refresh)`.

## File uploads

Bulletins, slide decks, and other documents are uploaded via `/admin/files` and stored on disk.

- Upload directory is configured via `UPLOAD_DIR` env var (default: `storage/uploads`)
- Files are served publicly at `/uploads/:filename`
- SQLite `file` table stores label, category, url, size, uploaded_at

**On the VPS**: set `UPLOAD_DIR=/home/app/storage/uploads` (or wherever persistent storage lives)
in `config.prod.env`. Make sure nginx also has a location block if you want to serve uploads
directly without proxying through the app:
```nginx
location /uploads/ {
    alias /home/app/storage/uploads/;
}
```

## Deployment

Single Biff uberjar + systemd + nginx on VPS. No Docker needed (Docker was only
required in mtz-cms for Alfresco). Set `BASE_URL=https://mtzcg.com` in `config.prod.env`
(gitignored — contains secrets).

Target deployment workflow: push to GitHub → pull on server → restart systemd service.
The `storage/` directory (SQLite DB + uploads) lives outside the repo and persists across deploys.
