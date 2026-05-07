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
- **Rich text**: Tiptap 2.x (framework-agnostic), bundled via esbuild → `resources/public/js/admin.js`
- **Images**: Cloudflare Images (CDN delivery via `imagedelivery.net`)
- **Video**: Cloudflare Stream (upload via API, embed via `iframe.cloudflarestream.com`)
- **Email (future)**: MailerSend for outbound transactional email (contact form). Cloudflare Email is routing-only (incoming), not for sending.

## Project structure

```
src/com/mtzion/
  app/
    admin.clj       — content admin CRUD: features, posts, events, pages, files
    media.clj       — Cloudflare Images upload endpoint + sermon CRUD (Stream video)
    auth.clj        — sign-in/sign-out routes + create-admin! REPL helper
    landing.clj     — public home page
    home_sections.clj — home page section components (static defaults)
    about/worship/events/activities/news/outreach/contact/preschool.clj — public pages
  ui/
    admin.clj       — admin UI components (top-bar, field, text-input, tiptap-field, etc.)
    base.clj        — public page template (header/footer/fonts/JS); returns Ring response map
    nav.clj         — site-header, site-footer, nav components
  lib/
    middleware.clj  — wrap-signed-in (redirects to /admin/signin)
    ui.clj          — css-path, anti-forgery-field helpers
  model/
    schema.clj      — SQLite column defs + extra-sql table definitions

admin-js/
  main.js           — Tiptap editor init, toolbar, image upload via /admin/upload

resources/
  config.edn        — Aero config (all env vars declared here with defaults)
  schema.sql        — sqlite3def schema file (auto-generated, do not edit by hand)
  tailwind.css      — Tailwind v4 input + full mtz + admin design system CSS
  public/
    images/         — static site images (church-exterior.jpg, DSC01305.jpg, etc.)
    js/
      htmx.min.js
      admin.js      — compiled Tiptap bundle (built by npm run build)
```

## SQLite tables

All defined in `schema.clj` (`extra-sql`) and mirrored in `resources/schema.sql`:

| Table | Purpose |
|-------|---------|
| `user` | Admin users (BCrypt password) |
| `feature` | Home page content slots (placement, title, subtitle, body, CTA) |
| `post` | Blog posts — "Pastor Jim Reflects" |
| `event` | Events with recurrence support |
| `page` | DB overrides for static pages (about, worship, etc.) |
| `file` | Uploaded documents (bulletins, slides) — stored on disk |
| `sermon` | Sermon records with Cloudflare Stream video_id |

## Admin panel routes

All under `/admin` with `wrap-signed-in` middleware:

- `/admin` — dashboard
- `/admin/features` — home page content slots CRUD
- `/admin/posts` — blog posts CRUD (Tiptap body)
- `/admin/events` — events CRUD (recurrence, all-day, location)
- `/admin/pages` — upsert page body by slug (about, worship, etc.)
- `/admin/files` — document upload (bulletins, slides → disk → `/uploads/:filename`)
- `/admin/sermons` — sermon CRUD with Cloudflare Stream video upload
- `/admin/upload` — POST multipart → Cloudflare Images → returns `{"url":"..."}` (used by Tiptap image button)

### Reitit route conflict pattern

`/new` (literal) and `/:id` (wildcard) at the same path level conflict in Reitit.
Fix: add `:conflicting true` to **both** the `/new` and `/:id` routes. Applied to
features, posts, events, and sermons.

## Cloudflare credentials

All stored in `config.env` (dev) / `config.prod.env` (prod). Keys in `config.edn`:

| config.edn key | env var | notes |
|---|---|---|
| `:cf/account-id` | `CLOUDFLARE_ACCOUNT_ID` | same for Images + Stream |
| `:cf/api-token` | `CLOUDFLARE_IMAGES_TOKEN` | token with Images + Stream permissions — `#biff/secret` returns a thunk; call it: `((:cf/api-token ctx))` |
| `:cf/images-hash` | `CLOUDFLARE_IMAGES_HASH` | hash from `imagedelivery.net/HASH/...` |

Cloudflare Stream URLs are fixed patterns using the video UID returned from upload — no extra hash needed:
- Embed: `https://iframe.cloudflarestream.com/<video-uid>`
- Thumbnail: `https://videodelivery.net/<video-uid>/thumbnails/thumbnail.jpg`

## Design system

CSS custom properties and component classes all prefixed `--mtz-` / `.mtz-`.
Key classes: `.mtz-header`, `.mtz-header-inner`, `.mtz-wordmark`, `.mtz-logo-*`,
`.mtz-nav`, `.mtz-nav-item`, `.mtz-footer`, `.mtz-btn`, `.mtz-flip-chip`.

Admin classes prefixed `.adm-`: `.adm-bar`, `.adm-content`, `.adm-table`, `.adm-field`,
`.adm-input`, `.adm-select`, `.adm-textarea`, `.adm-badge`, `.adm-card`, `.adm-form`, etc.

Header is a 4-column grid: `1fr auto 1fr auto` (left-nav | wordmark | right-nav | flip-chip).
The flip chip switches between Church and Preschool site contexts.

Logo shrink-on-scroll: JS in `base.clj` adds/removes `is-scrolled` on `#mtz-header`,
which triggers CSS transitions on `.mtz-logo-zion`, `.mtz-logo-mt`, `.mtz-logo-roof`.

Church nav: 8 items split 4+4 around the centred wordmark:
- Left: Home, About, Worship, Events
- Right: Activities, News, Outreach, Contact

## Auth flow

- `/admin/signin` GET — renders email+password form
- `/admin/signin` POST — verifies BCrypt hash, sets `{:uid user-id}` in session, redirects to `/admin`
- `/admin/signout` POST — clears session, redirects to `/`
- Create/reset admin from REPL: `(com.mtzion.app.auth/create-admin! @system "email@example.com" "password")`
- If REPL is unavailable: run the standalone script in `dev-notes/create-admin.clj` (uses raw JDBC + BCrypt, correct 16-byte UUID format)

## nREPL / Calva

nREPL starts on port 7888. It must be started with cider-nrepl middleware — without it,
Calva's CIDER initialization ops crash the ThreadPoolExecutor and kill the nREPL server.
This is already fixed in `src/com/mtzion.clj` (`-main` passes `cider-middleware` to `start-server`).

Wait for "System started" in the server log before connecting Calva.

## sqlite3def

The `sqlite3def` binary must be manually placed at `storage/sqlite3def/sqlite3def`.
Recent releases switched from `.tar.gz` to `.zip` for darwin_arm64 — download from GitHub
(sqldef v3.10.1) and extract manually if the auto-download fails with a corrupt file.

## Dev workflow

```bash
clj -M:run dev      # start dev server on :8080 with hot reload + nREPL on :7888
npm run build       # rebuild admin.js Tiptap bundle (run after editing admin-js/main.js)
clj -M:test         # run tests
```

To trigger a namespace reload without restarting (if the file watcher misses a change):
connect to nREPL on port 7888 and call `(clojure.tools.namespace.repl/refresh)`.

## File uploads

Bulletins, slide decks, and other documents are uploaded via `/admin/files` and stored on disk.

- Upload directory configured via `UPLOAD_DIR` env var (default: `storage/uploads`)
- Files served publicly at `/uploads/:filename` (path-traversal sanitised in handler)
- SQLite `file` table stores label, category, url, size_bytes, uploaded_at

**On the VPS**: set `UPLOAD_DIR=/home/app/storage/uploads` in `config.prod.env`.
Optionally serve directly via nginx to bypass the app:
```nginx
location /uploads/ {
    alias /home/app/storage/uploads/;
}
```

## Deployment

Single Biff uberjar + systemd + nginx on VPS. No Docker needed.
Set `BASE_URL=https://mtzcg.com` in `config.prod.env` (gitignored — contains secrets).

Target deployment workflow: push to GitHub → pull on server → restart systemd service.
The `storage/` directory (SQLite DB + uploads) lives outside the repo and persists across deploys.

Config vars needed in `config.prod.env`:
```
BASE_URL=https://mtzcg.com
SECURE=true
COOKIE_SECRET=<random 32+ char string>
CLOUDFLARE_ACCOUNT_ID=...
CLOUDFLARE_IMAGES_TOKEN=...
CLOUDFLARE_IMAGES_HASH=gNdSe_N39XhCrHxk2h53Cw
UPLOAD_DIR=/home/app/storage/uploads
SQLITE_DB_PATH=/home/app/storage/sqlite/main.db
```
