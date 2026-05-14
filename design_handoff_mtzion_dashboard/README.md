# Handoff: Mt Zion CMS — Admin Dashboard Redesign

## Overview

Redesign of the Mt Zion Church CMS admin dashboard landing page. The original
(`uploads/admin.clj` + screenshot) was a uniform 12-button grid (six content
types × two actions) rendered as identical bordered tiles — utilitarian but
flat. This handoff replaces it with an **asymmetric "bento" layout** that
gives each content type a distinct shape, scale, and graphic, and consolidates
file uploads into a single split-bar at the bottom.

The dashboard preserves the existing CMS data model — Features, Blog Posts,
Events, Pages, Photos (new), Files, Sermons — and the existing actions
(`+ New X`, `All X`). It only changes how they're presented.

## About the Design Files

The files in this bundle are **design references created in HTML/React** —
prototypes showing intended look and behavior, **not production code to copy
directly**. The Mt Zion CMS is a Clojure / Hiccup / vanilla CSS application
(`com.mtzion.ui.admin`); the task is to **recreate these designs in that
existing environment** using the established patterns:

- Hiccup data structures (see `uploads/admin.clj`)
- Server-rendered HTML
- The existing `mtz-btn` / `adm-*` CSS class conventions
- The `ui/css-path`, `ui/anti-forgery-field` helpers

Treat the HTML as a faithful visual spec. Translate the structure and tokens
into Hiccup + a stylesheet that fits the existing codebase.

## Fidelity

**High-fidelity.** Final colors, typography, spacing, and graphics are all
specified below. The developer should recreate the layout pixel-accurately,
adapting the markup to Hiccup and folding the CSS into the existing
stylesheet rather than inlining styles.

## Files in this bundle

| File | What it is |
|---|---|
| `Dashboard.html` | Entry point. Mounts the design canvas with three variations. |
| `dashboard-variations.jsx` | All three React variants (`BentoVariant`, `EditorialVariant`, `ColorCardsVariant`) plus shared primitives (`TYPES`, `ACCENT`, `Mark`, `TopBar`). |
| `design-canvas.jsx` | Canvas shell — pan/zoom + focus mode for comparing variants. **Not part of the production design — discard.** |
| `uploads/admin.clj` | Existing Hiccup helpers. The new dashboard view should live alongside this and reuse its primitives. |
| `uploads/Screenshot 2026-05-07 at 8.50.14 PM.png` | The dashboard as it currently looks. |

## The chosen direction: Variant A — Bento (asymmetric)

Three variants are in the canvas. **Implement Variant A (`BentoVariant`).**
Variants B (Editorial list) and C (Color-coded cards) are alternatives that
were explored but not chosen — keep them in the prototype for reference but
do not ship them.

## Screen: Admin Dashboard (`/admin/`)

### Purpose

Entry point after sign-in. Lets a CMS user (Pastor Jim, staff) jump into any
content type to create or browse, plus upload files and sermon videos directly.

### Overall layout

Single page, ~1280 wide, ~1000 tall. Five horizontal regions stacked:

1. **Top bar** — full-width, 60 px tall. Brand on the left, account + nav on the right.
2. **Page header** — 32px top padding, 40px horizontal padding. Title + date.
3. **Primary grid (row 1)** — 4 columns, `1fr 1fr 1fr 0.66fr`, 14 px gap, 40 px horizontal padding. Three large square tiles (Blog Posts, Pages, Features) + a tall Events column on the right.
4. **Secondary grid (row 2)** — 3 columns, `1.5fr 1.5fr 0.66fr`, 14 px gap. Photos tile + Calendar tile + an empty cell that aligns with the Events column above.
5. **Upload split bar** — 2 columns, equal width, 14 px gap. Files uploads on the left, Sermon video upload on the right.

The 0.66fr Events column is the same height as the entire 2-row stack on its left, so it visually spans both rows.

### Region 1 — Top bar

- Background: transparent (page bg shows through)
- Border-bottom: `1px solid #E5DFD2`
- Padding: `20px 48px`
- Layout: flex row, space-between, align-center

**Left:**
- "Mt Zion" — Fraunces 600, 20px, letter-spacing -0.01em
- "CMS" — JetBrains Mono, 11px, letter-spacing 0.16em, uppercase, color `#1C1A17` at 55% opacity, 12px gap from the brand

**Right (flex, 28px gap, 13px text):**
- "Signed in as jim@mtzion.org" (label at 55% opacity, email at 90%)
- "View site →" — underlined
- "Sign out" — at 70% opacity

### Region 2 — Page header

- Padding: `32px 40px 0`
- Layout: flex row, baseline align, space-between

**Left:** `<h1>` "Mt Zion Dashboard" — Fraunces 500, 36px, letter-spacing -0.02em
**Right:** "Thursday · May 7" (or current day in same format) — JetBrains Mono 11px, letter-spacing 0.18em, uppercase, color `#8A8478`

### Region 3 — Primary grid (3 big squares + Events)

Padding: `24px 40px 14px`. Grid: `1fr 1fr 1fr 0.66fr`, 14px gap.

#### 3a. Three big square tiles (Blog Posts, Pages, Features)

Shared structure for all three:

- Container: `aspect-ratio: 1/1`, background `#FBF9F4`, border `1px solid #E5DFD2`, border-radius 6px, padding 20px, flex column with space-between.
- **Header row:** flex space-between
  - Left: content type name in JetBrains Mono 10px, letter-spacing 0.18em, uppercase, color = the type's accent ink (see Design Tokens)
  - Right: count in JetBrains Mono 10px, color `#8A8478`
- **Graphic body:** flex 1, centered, padding `12px 0`. The graphic itself differs per type — see below.
- **Title block:**
  - Type name in Fraunces 500, 26px, letter-spacing -0.01em
  - Recent activity in 11.5px, color `#8A8478`, ellipsis on overflow
- **Buttons row:** flex, 6px gap, 12px top margin
  - "+ New" — primary button with `background: ${accent.ink}`, `color: #FFF`, flex: 1, 7px/12px padding, 3px radius, 12px font, font-weight 500
  - "All" — ghost button with `border: 1px solid #C9C2B2`, `color: #1C1A17`, 6px/12px padding, 3px radius, 12px font

##### Blog Posts tile graphic

A miniature blog post card, ~78% wide of the parent, aspect ~0.78:

- Background: `#FBE9DF` (terra tint)
- Box-shadow: `4px 4px 0 #F4D9CC` (terra soft, hard offset shadow — no blur)
- Padding: 14px, border-radius: 3px, flex column, 6px gap
- Heading: Fraunces 600, 13px, color `#C24A1F`, two lines: "On stillness, and the / shape of an evening."
- Five text-line bars: 2px tall, color `#C24A1F` at 35% opacity for the first 3, 20% for the next 2. Widths 85%, 95%, 70%, 90%, 80%.

##### Pages tile graphic

A fanned stack of three page silhouettes, ~70% wide of the parent, aspect ~0.85:

- Three absolutely-positioned divs at `inset: 0`, each transformed `translate(i*8px, i*8px) rotate((i-1)*-2deg)` for i in 0..2.
- Bottom and middle pages: `background: #FBF9F4`, `border: 1px solid #D5DCE6` (slate soft).
- Top page (i=2): `background: #E6EBF2` (slate tint), padding 12px, contains:
  - A solid rectangle 8px tall, 60% wide — `background: #3D4A60` at 70% opacity (page title)
  - Four 2px text bars at 25% opacity, widths 90%, 80%, 85%, 70%

##### Features tile graphic

A miniature website hero preview, 85% wide, aspect ~1.4:

- Background: `#F8EED1` (gold tint), padding 10px, border-radius 3px, flex column 6px gap
- Top: 3 dots (5×5px circles, `background: #A87A2A` at 40% opacity), 3px gap — fake browser bar
- Body: flex row, 4px gap
  - 2/3 width: `background: #A87A2A` at 85% opacity, padding 6px, border-radius 2px, flex column justify-end. Contains "Sunday / Welcome" — Fraunces 600, 9px, color `#F8EED1`, line-height 1.1
  - 1/3 width: 2 stacked bars (4px gap), each `background: #FBF9F4`, border-radius 2px

#### 3b. Events column (right)

- Aspect ratio: `0.66 / 1` (so it's the same height as the 1fr squares — same height as a single big-square tile, **not** the full 2-row span; the second-row grid has an empty cell beneath that the Events column visually doesn't fill — see "Important geometry note" below).
- Background: `#FBE9DF` (terra tint), border-radius 6px, padding 20px, flex column.
- **Header:** flex row, space-between
  - "Events" in JetBrains Mono 10px / 0.18em / uppercase, color `#C24A1F`
  - "12 upcoming" in JetBrains Mono 10px, color `#C24A1F` at 70% opacity
- **Big title:** "What's next" — Fraunces 500, 26px, letter-spacing -0.01em, 14px top margin
- **Event list:** 14px top margin, flex column 10px gap, flex: 1
  - Each event row: flex, align-center, 12px gap
    - Date chip: 38×38px, `background: #FBF9F4`, `border: 1px solid #F4D9CC`, border-radius 4px, flex column center
      - Month abbreviation (3 letters): JetBrains Mono 8px, 0.1em, uppercase, color `#C24A1F`
      - Day number: Fraunces 600, 15px, color `#C24A1F`, line-height 1
    - Event name: 12.5px, color `#1C1A17`
  - Sample data: `[{m:'May', d:'18', name:'Spring Picnic'}, {m:'May', d:'21', name:'Bible Study'}, {m:'Jun', d:'02', name:"Children's Choir"}]`
- **Buttons row:** 14px top margin, flex 6px gap
  - "+ New event" — primary, flex: 1, `background: #C24A1F`, color white
  - "All" — ghost with `border: 1px solid #C24A1F`, color `#C24A1F`

### Region 4 — Secondary grid (Photos + Calendar)

Padding: `0 40px 14px`. Grid: `1.5fr 1.5fr 0.66fr`, 14px gap. The third 0.66fr cell is empty — it lines up under the Events column and is intentionally blank (do not put anything there).

#### 4a. Photos tile

- Background: `#FBF9F4`, border `1px solid #E5DFD2`, border-radius 6px, padding 20px, flex column
- **Header:** flex row, space-between, align-start
  - Left: "Photos" mono caption + "Gallery" Fraunces 500 / 24px (6px top margin)
  - Right: count "184" Fraunces 500 / 26px, color `#5A7257` (sage ink); "images" mono 9px / 0.14em / uppercase, color `#8A8478`
- **Mini gallery grid:** 14px top margin, flex 1, 4 columns, 6px gap, 8 cells
  - Each cell: aspect-ratio 1, border-radius 2px
  - Cell coloring rotates by `i % 3`:
    - `i%3==0`: `background: #5A7257` (sage ink), 85% opacity
    - `i%3==1`: `background: #EAEEE4` (sage tint)
    - `i%3==2`: 135deg striped repeating linear gradient, `#EAEEE4 0–8px, #DCE2D6 8–16px`
- **Recent line:** 14px top margin, 11.5px text, color `#8A8478`. Sample: "Easter Service — Apr 5"
- **Buttons:** 12px top margin, flex 6px gap. Primary "+ Upload photos" (flex 1, sage ink bg), ghost "All".

#### 4b. Calendar tile

- Background: `#FBF9F4`, border `1px solid #E5DFD2`, border-radius 6px, padding 18px, flex column
- **Header:** flex row, space-between, align-center
  - Left: "Calendar" mono caption + "May *2026*" Fraunces 500 / 22px (the year in `#8A8478`)
  - Right: prev/next chevrons — 22×22 buttons, border `1px solid #E5DFD2`, background `#FBF9F4`, color `#4A463F`, 13px font, 3px radius
- **Day grid:** 14px top margin, 7-column grid, 2px gap, flex 1
  - Header row: S M T W T F S — JetBrains Mono 9px / 0.1em / uppercase, color `#8A8478`, centered, 4px vertical padding
  - 35 day cells (5 rows). May 2026 starts Friday — first 5 cells are empty divs.
  - Each day cell: `aspect-ratio: 1.1/1`, centered, 12px tabular-nums
    - Today (May 7): `background: #C24A1F`, color `#FBF9F4`, font-weight 600
    - Event days (May 18, May 21): color `#C24A1F`, font-weight 600, with a 4×4px dot at bottom-center (3px from bottom)
    - Other days: color `#1C1A17`, font-weight 400
- **Footer:** 10px top margin and padding, dashed top border `#E5DFD2`, flex space-between, 11.5px
  - Left: "**2 events** in May" (number in `#C24A1F`, weight 600)
  - Right: "Open calendar →" (color `#8A8478`)

### Region 5 — Upload split bar

Margin: `0 40px 28px`. Grid: 2 equal columns, 14px gap.

#### 5a. Files · upload (left, light)

- Background: `#FBF9F4`, border `1px solid #E5DFD2`, border-radius 6px, padding `18px 20px`
- **Header row** (12px bottom margin): flex space-between
  - Left: paperclip mark (Mark kind="files", 20px, stroke `#1C1A17`) + "Files · upload" mono caption color `#1C1A17`
  - Right: "23 in library →" 11px link color `#8A8478`
- **Upload card grid:** 3 columns, 8px gap. Each card:
  - Background `#F7F4EE`, border `1px solid #E5DFD2`, border-radius 4px, padding `14px 12px`, flex column align-start
  - Document mark: 28×36 rectangle, `border: 1.5px solid #1C1A17`, border-radius 2px. Inner 8×8 corner-fold detail at top-right (background cream + bottom-and-left borders only). 8px bottom margin.
  - Title: Fraunces 500, 14px, "+ Bulletin" / "+ Newsletter" / "+ Presentation"
  - Hint: JetBrains Mono 9.5px, 0.06em, uppercase, color `#8A8478`, 3px top margin. Hints: "PDF · weekly", "PDF · monthly", "PPT · slides"

#### 5b. Sermon · upload (right, dark)

- Background `#1C1A17`, color `#F7F4EE`, border-radius 6px, padding `18px 20px`, position relative, overflow hidden
- **Header row** (12px bottom margin): flex space-between
  - Left: play-circle mark (Mark kind="sermons", 20px, stroke `#F7F4EE`) + "Sermon · upload" mono caption at 70% opacity
  - Right: "156 archived →" 11px link, `#F7F4EE` at 55% opacity
- **Drop area + button** in a flex row, 16px gap, align-center:
  - **Drop area (flex 1):** `border: 1.5px dashed rgba(247,244,238,0.35)`, border-radius 4px, padding `18px 16px`, flex row 14px gap align-center
    - Play disc: 44×44 circle, `background: rgba(247,244,238,0.95)`, centered. Inside: a 14×14 right-pointing triangle SVG, fill `#1C1A17`
    - Text block:
      - "+ Upload sermon video" Fraunces 500, 17px
      - "Drop a .mp4 here, or paste a YouTube/Vimeo link" 11.5px, 60% opacity
  - **Choose file button:** primary white-on-cream — `background: #F7F4EE`, `color: #1C1A17`, border none, padding `10px 16px`, border-radius 4px, 13px / 500. White-space nowrap.

## Geometric marks (icons)

Six abstract SVG marks, all 40×40 viewBox, currentColor stroke. Replace the
emoji icons in the original. Each is a few primitive shapes — keep them as
inline SVG. Stroke widths scale to `max(1.25, size/22)`.

| Key | Shape |
|---|---|
| `features` | Picture-frame rect (32×24, rounded 1) + a polyline mountain ridge inside + a 2px-radius circle (sun) |
| `blog` | Four horizontal text bars, widths 22 / 24 / 18 / 12, 7 px apart |
| `events` | Calendar rect (28×25) with two top tabs (vertical lines at x=13, x=27), a horizontal divider at y=16, a centered 2.5-radius dot at (20, 25) |
| `pages` | Two overlapping page rectangles (22×28), the back one offset (10,6), the front one offset (6,10) |
| `files` | Folder silhouette: M8 12 H18 L21 16 H32 V31 H8 Z (the L21,16 is the tab notch) + a horizontal line across the body |
| `photos` | Picture-frame rect (28×22) + a polyline ridge inside + a 2-radius circle (sun) — same family as `features` but flatter aspect |
| `sermons` | Circle (r=14) + filled play triangle (17,14 → 27,20 → 17,26) |

The exact SVG paths are in `dashboard-variations.jsx`'s `Mark` component.

## Design Tokens

### Colors

```
/* Surfaces */
--cream:      #F7F4EE;  /* page background */
--paper:      #FBF9F4;  /* tile background */
--rule:       #E5DFD2;  /* hairlines, borders */

/* Ink */
--ink:        #1C1A17;  /* primary text, dark surfaces */
--ink-2:      #4A463F;  /* secondary text */
--ink-3:      #8A8478;  /* muted, captions */

/* Accent — terra (Blog Posts, Events, Sermons accent ink) */
--terra-ink:  #C24A1F;
--terra-soft: #F4D9CC;
--terra-tint: #FBE9DF;

/* Accent — sage (Photos) */
--sage-ink:   #5A7257;
--sage-soft:  #DCE2D6;
--sage-tint:  #EAEEE4;

/* Accent — gold (Features) */
--gold-ink:   #A87A2A;
--gold-soft:  #F1E2BD;
--gold-tint:  #F8EED1;

/* Accent — slate (Pages) */
--slate-ink:  #3D4A60;
--slate-soft: #D5DCE6;
--slate-tint: #E6EBF2;
```

### Typography

- **Display / serif:** Fraunces (Google Fonts), variable weight, optical sizing on. Weights used: 400, 500, 600.
- **UI / sans:** Inter (Google Fonts). Weights used: 400, 500, 600, 700.
- **Mono / caption:** JetBrains Mono. Weights used: 400, 500.

| Use | Font / size / weight / tracking |
|---|---|
| Page H1 | Fraunces 500, 36px, -0.02em |
| Tile title | Fraunces 500, 26px, -0.01em |
| Calendar month | Fraunces 500, 22px |
| Big number | Fraunces 500, 26px, tabular-nums |
| Mono caption | JetBrains Mono, 10–11px, 0.16–0.18em, uppercase |
| Tile recent line | Inter 400, 11.5px, color `--ink-3` |
| Body | Inter 400, 13px, color `--ink` |
| Button | Inter 500, 12–13px |

### Spacing

Page padding: 40px horizontal. Section gaps: 14px between grid cells, 24–28px between regions. Tile padding: 18–20px. Card padding (upload card): `14px 12px`.

### Radii

- Tiles: 6px
- Buttons: 3–4px (small / large)
- Inner graphics (page rects, photo cells): 2–3px

### Shadows

- Blog Posts inner card: `4px 4px 0 var(--terra-soft)` — hard offset, no blur, no opacity
- Otherwise: no shadows. Use borders.

## Interactions & behavior

- **Buttons** — All `+ New X` buttons POST to `/admin/<type>/new` (or open a create form). All `All` buttons GET `/admin/<type>`. Match the existing routes in `admin.clj`.
- **Upload cards** (Bulletin / Newsletter / Presentation) — open a file picker constrained by the hint MIME (PDF for Bulletin/Newsletter, PPT/PPTX for Presentation), then POST to the existing files endpoint with a `kind` discriminator.
- **Drop area** (Sermon · upload) — accepts `.mp4`/`.mov` drag-drop **and** a pasted YouTube/Vimeo URL. Show a thin progress bar in the dashed border on upload.
- **Calendar chevrons** — paginate the visible month. On page load, show the current month with today highlighted.
- **Hover** — all interactive surfaces should darken 4–6% on hover. Buttons get a subtle `translateY(-1px)` or no movement at all (designer's call). No "pop" animations — the tone is calm.
- **Focus rings** — 2px outline, color `--terra-ink`, 2px offset. Use `:focus-visible`.

## State / data

| Field | Source |
|---|---|
| Counts (47, 12, 8, 184, 23, 156, 4) | `count` from each content type's table |
| Recent activity strings | Derived from each table's `updated_at desc limit 1` |
| Today's date | Server-side, formatted "Day · MMM D" |
| Calendar events | Events table, `start_date >= today`, group by month |
| Signed-in email | Existing session helper |

## Responsive notes

The design is laid out for ~1280px. For narrower viewports:

- Below 1024px, drop the Events column to a horizontal strip above the upload bar; the primary grid becomes 3-up at full width.
- Below 768px, all grids collapse to single-column. Big squares become rectangles (~ 4:3) so the cropping doesn't look weird at narrow widths.
- The upload split bar stacks vertically on mobile.

## Out of scope

- Sub-pages of the CMS (the Blog Posts list, the Event editor, etc.) — only the dashboard landing page is in this handoff.
- Authentication / sign-in screens.
- Dark mode (the Sermons upload panel is the only "dark" surface, and that's intentional).

## Variants B and C (not chosen)

Kept in `dashboard-variations.jsx` for reference:

- **B — Editorial list (`EditorialVariant`):** A numbered table-of-contents
  with ruled rows, big serif type names, and pill CTAs. Calmest option.
- **C — Color-coded cards (`ColorCardsVariant`):** Six equal cards, each with a
  top color stripe and its own hue. Symmetric grid.

Do not implement these.
