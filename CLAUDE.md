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
| `post` | Blog posts, news and reflections; `author` is a free-text byline |
| `event` | Events with recurrence support |
| `page` | DB overrides for static pages (about, worship, etc.) |
| `file` | Uploaded documents (bulletins, slides) — stored in Cloudflare R2 |
| `sermon` | Sermon records with Cloudflare Stream video_id |

## Publish state: one `status` column, five tables

`feature`, `post`, `event`, `page` and `sermon` each carry
`status TEXT` — exactly one of `draft`, `published`, `archived` — plus
`published_at` (when it first went live) and `archived_at`.

**Public read queries filter on `[:= :status "published"]`.** Nothing else.
Before this there were four dialects of "not live yet" (three `published INTEGER`
columns, and on `post` the convention that a NULL `published_at` meant draft);
that is gone.

Go through `com.mtzion.model.content` rather than writing the column by hand:

```clojure
(content/live ctx :event)                 ; published rows — what the site sees
(content/ls   ctx :post {:status "draft"})
(content/publish! ctx :event id)          ; also stamps published_at, once
(content/archive! ctx :post id)           ; the replacement for Delete
(content/restore! ctx :post id)           ; back to draft, never straight to live
```

Two rules the namespace enforces, both deliberate:

- **`save!` never changes publish state.** Status moves only through the
  transition fns, so editing an item can't put it on the site by accident.
  Same rule the importer follows.
- **New content is always a draft.** `content/defaults` is merged into every
  insert.

### The `published` column is vestigial

The old `published INTEGER` still exists on the four tables that had it, and is
still written in lockstep with `status`, purely so a deploy can be rolled back.
**Nothing reads it.** It gets dropped once the console replaces `/admin`.

### Migrating an existing database

`status` is nullable with no default on purpose. `content/use-status-backfill`
is a Biff component ordered directly after `biff.sqlite/use-sqlite` (in both
`com.mtzion/components` and `com.mtzion.system`); it derives `status` for rows
that predate the column, guarded by `status IS NULL`, so it is a no-op on every
boot after the first. Giving the column `DEFAULT 'draft'` instead would have had
sqlite3def fill every existing row with 'draft' on the way up — i.e. blank the
live site.

## The console (`/console`)

The task-shaped replacement for `/admin`, built alongside it — both read and
write the same rows, so neither can drift while the migration is in progress.
`/admin` stays fully working until the console covers everything it does.

| Route | State |
|---|---|
| `/console` | dashboard — three pane cards |
| `/console/writing` | **built** — blog / news / reflections, list + editor |
| `/console/archive` | **built** — everything archived, across all five types |
| `/console/site` | **built** — the site outline, tree + leaf editor |
| `/console/calendar` | placeholder → `/admin/events` |
| `/console/inbox` | placeholder — bulletin review queue |
| `/console/media` | placeholder → `/admin/images` |

Every pane is the same shape: **listing on the left, editor on the right**.

### The status pill is the control

`con/status-pill` renders the current state *and* toggles it — reading it and
changing it are the same gesture, which is why there is no separate Publish
button. It posts to `…/:id/status`, swaps itself, and swaps the matching
listing row's dot via `hx-swap-oob` so the two halves of the screen agree
without a reload.

### Autosave

`resources/public/js/console.js` — **plain JS, deliberately not in the esbuild
bundle**, so editing it does not require `npm run build`. It debounces 1.5s and
posts the whole form to `…/:id/autosave`. ProseMirror is contenteditable, so its
`input` events bubble to the form: one listener covers title, body and details.

Autosave only runs for a post that already exists (the form carries
`data-autosave` only then) — a brand-new post has nowhere to save to yet.

## The site outline (`/console/site`)

Menu item → page → the editable parts of that page. Declared in
`com.mtzion.model.outline`, **not derived** — the page templates ask for named
slots (`landing.clj` for `home-hero`, `worship.clj` for `current-theme`) and
those names are addresses in code, not rows anything can be read off.

Five kinds of leaf, and the `:slot` / `:list` distinction is the one that
answers "can I add another section?":

| Kind | Means |
|---|---|
| `:slot` | ONE row, in a place the design lays out by hand. A second row under that slug is **ignored**. |
| `:list` | MANY rows, one uniform layout, editor's order. **Unlimited** — this is where "add a section" lives. |
| `:body` | The page's own Tiptap body on the `page` row. Only `about` and `outreach` templates read one. |
| `:link` | Rendered on this page but owned by another pane; sends you there instead of opening a second editor onto the same rows. |
| `:static` | The page reads nothing from the database. Declared so the tree doesn't pretend otherwise (Preschool). |

`:fields` is what the template actually **reads**, and the editor renders only
those. That is the fix for one universal twelve-field form serving rows that
mean completely different things — `/admin/features` offered Image and Sort
Order for `current-theme`, which renders neither.

### The generic sections region

`com.mtzion.ui.sections/region` renders every published `feature` filed under a
page's slug, in `sort_order`. Seven templates splice it in just before their
closing CTA. Storage is the `feature` table, which already had every column.

Slugs are deliberately distinct where a page has both a designed list and a
generic one: `/activities` uses `activities` for the seasonal-programme cards
and **`activities-extra`** for console-added sections, so adding a section
doesn't silently add a card to the programme grid.

### The drift guard

`outline_test.clj` seeds a section under every declared `:list` slug, renders
the real page handler, and asserts the text appears. A leaf that offers an
editor whose words never show up is the failure it catches — it caught a real
one on the first run (the tree offered a Page body editor on six pages whose
templates never read `page.body`).

Two template quirks the tests encode, both intentional:
- Home's `home-activities` cards are **image-only**. A row without an image is
  skipped by design, and its heading becomes the `img` alt text.
- `/activities` calls `always-at-mtz-section` with no arguments, so the graphics
  strip renders copy-only there. Not a bug introduced by the console.

### Fragments must not carry a doctype

`hiccup/render` prepends `<!DOCTYPE html>` unless told otherwise. Every HTMX
endpoint here goes through `console/fragment`, which passes
`{:doctype? false}`. (`media.clj`'s image browser predates this and still emits
one; harmless, but don't copy it.)

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

## Reading rows: `biff.sqlite/execute` returns QUALIFIED KEBAB keys

`biff.sqlite/execute` returns `:post/published-at`, `:page/body`, `:feature/subtitle` —
namespace-qualified and kebab-case. Every renderer in this app reads **unqualified
snake_case** (`:published_at`, `:body`). Reading a raw result therefore yields `nil`
for every field, silently, with no error — this caused four separate "section renders
blank" bugs (about, outreach, news, activities).

Always pass results through `com.mtzion.model.normalize/snake-keys` (single row) or
`snake-keys-all` (collection). `(name k)` strips the namespace and the dashes become
underscores in one step. The `exec` helpers in `admin.clj` and `media.clj` already do this.

## Dates: two different timezone conventions

Defined and tested in `com.mtzion.model.normalize`. Do not conflate them:

| Kind | Columns | Stored as |
|---|---|---|
| datetime | `event.start_at`, `event.end_at` | church wall-clock, **America/New_York** |
| date-only | `recur_until`, `published_at`, `file_date`, `sermon_date` | **UTC midnight** |

So `start_at` must be *rendered* in Eastern (rendering it in UTC showed every event 4–5
hours late), while `sermon_date` must be rendered in UTC. Recurrence expansion steps in
Eastern `LocalDateTime` so a 6:30 PM weekly event stays 6:30 PM across DST.

## Content import (Claude Desktop → EDN → site)

The weekly path for the pastor's bulletin/slides. Content is authored by an AI
agent against a published contract, reviewed as a diff, and published by a human.

```bash
clj -M:run content-doc       # regenerate content-inbox/CONTRACT.md from the schemas
clj -M:run import            # dry run — prints a diff, writes NOTHING
clj -M:run import --apply    # commit; archives the input + a receipt
```

Weekly loop:

1. Drop the bulletin PDF + slides PPTX into a Claude Desktop **Project** whose
   knowledge is `content-inbox/CONTRACT.md`. Desktop reads both formats natively —
   there is no parsing code in this repo.
2. Prompt: *"Extract this week's content per CONTRACT.md. Output exactly one EDN
   map, no prose, no markdown fence."*
3. Save the output to `content-inbox/<date>-bulletin.edn`.
4. `clj -M:run import` → read the diff → `clj -M:run import --apply`.
5. Review and **publish** in `/admin`. Attach images/video there too.

### Invariants the importer guarantees

- **Everything imports as a draft.** `:published` is not part of the contract.
- **Re-importing never un-publishes.** Only INSERT sets publish state; UPDATE
  never touches `status` / `published_at`.
- **Never deletes.** Removing an item from the EDN leaves the row alone.
- **Idempotent.** Re-dropping an unchanged file plans zero writes.
- **All-or-nothing per file**, in a transaction. Validation failure = zero writes.
- Matching order: `import_key` → natural key (adopting a hand-made row) → insert.

### Contract drift

`CONTRACT.md` is *generated* from `content/schema.clj` + `content/hiccup.clj` +
`model/nav.clj`. A test asserts the committed file equals the generated output, so
changing a schema without running `content-doc` fails the build. Each file carries
a `:contract-sha`; the importer warns (but still imports) when it is stale.

### Layout

```
src/com/mtzion/content/
  hiccup.clj   tag/attribute allowlist, explain, ->html (throws on invalid)
  schema.clj   Malli item + envelope schemas
  plan.clj     match existing rows, classify create/update/unchanged, render diff
  ingest.clj   read → validate → plan → apply; the CLI task
  doc.clj      generates CONTRACT.md
content-inbox/           gitignored except CONTRACT.md + examples/
  CONTRACT.md            what Claude Desktop is told to follow
  examples/bulletin.edn  worked example (validated by a test)
  applied/               archived inputs + receipts after --apply
```

## Dev workflow

```bash
clj -M:run dev      # start dev server on :8080 with hot reload + nREPL on :7888
npm run build       # rebuild admin.js Tiptap bundle (run after editing admin-js/main.js)
clj -M:run test     # run tests (there is no :test alias — this is Biff's task)
```

To trigger a namespace reload without restarting (if the file watcher misses a change):
connect to nREPL on port 7888 and call `(clojure.tools.namespace.repl/refresh)`.

## File uploads

Bulletins, slide decks, and other documents are uploaded via `/admin/files` and
stored in **Cloudflare R2** (`com.mtzion.lib.r2`), not on disk. `files-upload`
PUTs the object under `files/<uuid>-<name>` and stores R2's public URL in the
`file` row; `files-delete` removes the object as well as the row.

- Config: `R2_BUCKET`, `R2_ACCESS_KEY_ID`, `R2_SECRET_KEY`, `R2_PUBLIC_URL`
- SQLite `file` table stores label, category, url, size_bytes, uploaded_at

`UPLOAD_DIR` (`:mtz/upload-dir`) is still declared in `config.edn` but nothing
reads it any more — it is a leftover of the pre-R2 disk implementation.

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
