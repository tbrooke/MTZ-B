# Handoff: Homepage — "What's Coming Up" + compact "This Sunday"

## Overview

Two homepage sections that replace the old "Featured Event" + tall "This Sunday" block. The goal is to give the church secretary's hand-designed event flyers a prominent, full-fidelity home, and shrink the worship-times block so it doesn't dominate the page.

These sit between the hero and the "Always at Mt. Zion" section. Section order: **Hero → What's Coming Up → This Sunday → Always at Mt. Zion → News → About → Outreach**.

## About These Files
The HTML/JSX/CSS here is a **design reference** — recreate it in the existing `mtz-cms/` Clojure codebase using Hiccup components + Tailwind utilities, not by pasting JSX. Reuse existing design tokens (mint OKLCH palette, EB Garamond / Source Serif 4 / IBM Plex Sans, `--mtz-*` CSS vars).

## Fidelity
**High-fidelity.** Layout, type, spacing, and copy are final. The two flyer images are real assets supplied by the church secretary; render them at their natural aspect ratio without cropping. The four worship-time entries are the canonical Sunday schedule (church will edit copy in the CMS).

---

## Section 1 — "What's Coming Up"

### Purpose
The two big flyers the secretary produces (currently this week's "Friends and Family Sunday" and the upcoming "Vacation Bible School"). The flyers carry their own typography and color, so the surrounding chrome stays quiet.

### Layout

- Full-width tinted band (background `--mtz-bg-tint` / `mint-light`).
- Inner container max-width `--mtz-page-w` (1180px), centered, 64px vertical / 32px horizontal padding.
- Header row, baseline-aligned, 28px below to grid:
  - **Left**: kicker `WHAT'S COMING UP` (`.mtz-kicker` — IBM Plex 11.5px uppercase, 0.22em tracking, mint-dark), followed by h2 *"Mark your calendar."* (`.mtz-h2` — EB Garamond 500, 44px, 4px top margin).
  - **Right**: arrow link "All events →" pointing to `/events`.
- Two-column grid below the header. `grid-template-columns: 1fr 1fr`, gap **36px**, `align-items: stretch`.

### Each card

- White background, 1px `--mtz-rule` border, 8px radius, `overflow: hidden`. Same as `.mtz-card`.
- The whole image is wrapped in a single `<a href="/events">` so clicking anywhere on the flyer navigates.
- **Image**:
  - `display: block`, `width: 100%`
  - `aspect-ratio: 1230 / 780` (matches the secretary's source PSDs)
  - `object-fit: cover`
  - 1px bottom border (`--mtz-rule`) so it sits cleanly on the card body
- **Body** (`padding: 24px`):
  - **Meta line** (`.mtz-card-meta` — IBM Plex 11px uppercase, 0.16em tracking, `--mtz-ink-mute`, 8px bottom margin). E.g. `This Sunday · May 4 · 10:30 AM`.
  - **Title** (`.mtz-h3` at 22px, EB Garamond 600, 8px bottom margin).
  - **Blurb** — Source Serif 15.5px, `--mtz-ink-soft`, 0 margins except 16px below.
  - **Arrow CTA** at the bottom — `.mtz-arrow-link` ending with `→`.

### Content (initial — editable via CMS)

| Field | Card 1 | Card 2 |
|---|---|---|
| Image | `images/friends-family-sunday.jpg` | `images/vbs-rome-2026.jpg` |
| Alt text | "Friends and Family Sunday — May 4 at 10:30 AM" | "Vacation Bible School: Paul and the Underground Church — June 14–16" |
| Meta | This Sunday · May 4 · 10:30 AM | Save the Date · June 14–16 |
| Title | Friends and Family Sunday | Vacation Bible School |
| Blurb | Worship in our SonCourt, a mini-concert from our preschoolers, and a hot-dog dinner after. Come home. | Paul and the Underground Church. Dinner 5:30, program 6:00–8:00 — for kids of all ages. |
| CTA | Plan to be there → | Register your child → |

### Behavior
- Static section (no JS).
- All links → `/events` until per-event detail pages exist.
- When the CMS has fewer than 2 events, render a single column. When 3+, render the first 2 here and link the rest to the Events page.
- Anchor `id="events"` lives on the outer `<section>` so the header nav's smooth-scroll targets it.

---

## Section 2 — "This Sunday" (compact strip)

### Purpose
Replaces the previous tall 2-column "This Sunday" block. Conveys the same information (worship times + bulletin / Facebook links) in roughly a third of the vertical space.

### Layout

- Standard `.mtz-section` (white bg, 32px horizontal padding, `1180px` max-width, centered). Vertical padding **56px top + 56px bottom**.
- **Top row** (header):
  - `display: flex`, `justify-content: space-between`, `align-items: baseline`, `flex-wrap: wrap`, 16px gap.
  - Bottom border: **1px solid `--mtz-ink`** (thick rule), 16px padding-bottom.
  - **Left**: kicker `THIS SUNDAY` + h3 *"Worship times"* (EB Garamond 600, 24px, 4px top margin).
  - **Right**: two arrow links side by side, 24px gap — *"This week's bulletin →"* and *"Watch on Facebook →"*.
- **Times row**: 4-column CSS grid (`grid-template-columns: repeat(4, 1fr)`, gap 0). Each cell:
  - `padding: 20px 16px`
  - `border-right: 1px solid var(--mtz-rule)` (except the last cell)
  - `border-bottom: 1px solid var(--mtz-rule)`
  - Content stack (no flex/gap needed — block flow):
    - **Time** — `.mtz-mono` 12.5px, 0.12em tracking, mint-dark, weight 600, 6px bottom margin
    - **Title** — EB Garamond 500, 19px, 2px bottom margin
    - **Sub** — Source Serif 13px, `--mtz-ink-mute`

### Times content

| Time | Title | Sub |
|---|---|---|
| 8:30 AM | Early Worship | Contemplative |
| 9:30 AM | Sunday School | All ages |
| 10:30 AM | Traditional Worship | Choir & organ |
| 6:00 PM | Youth Group | Fellowship Hall |

### Behavior
- Static section (no JS).
- Anchor `id="worship"` on the outer section.
- At <768px, collapse the 4 columns to 2 (`repeat(2, 1fr)`), then to 1 below 480px. Right-borders disappear in 2-column mode; bottom borders separate rows.
- The "Watch on Facebook" link is just an external `<a target="_blank" rel="noopener">` to the church's Facebook live page. No embed.

---

## Removed from the homepage
- The previous "Last Sunday" video block. The sermon archive lives on `/worship`; we link there from the header nav.
- The "Featured Event" trivia-night card layout — superseded by "What's Coming Up".

## Assets

| Asset | Source | Where |
|---|---|---|
| `images/friends-family-sunday.jpg` | Provided by church secretary | Flyer card 1 |
| `images/vbs-rome-2026.jpg` | Provided by church secretary | Flyer card 2 |

Both are ~1230×780 px. Serve them as-is. Don't re-crop. Consider serving WebP versions alongside for bandwidth, but always preserve the original 1230×780 aspect on display.

## Design tokens used
- `--mtz-bg-tint` (mint-light) — flyer section band
- `--mtz-rule` — hairlines
- `--mtz-ink`, `--mtz-ink-soft`, `--mtz-ink-mute` — text
- `--mtz-mint-dark` — kickers, time labels, mono accents
- `--mtz-serif-display` (EB Garamond) — h2/h3/title
- `--mtz-serif-body` (Source Serif 4) — body, blurbs
- `--mtz-sans-menu` (IBM Plex Sans) — kicker, meta, mono

All already defined in `mtz-cms/src/mtz_cms/ui/design_system.clj` and the existing Tailwind config — reuse.

## Files in this bundle
- `README.md` — this document
- `page-home.jsx` — JSX reference; the relevant sections are `HomeChurchBody` → "What's Coming Up" and "This Sunday" blocks
- `site-styles.css` — kickers, arrow-link, card, mono, h2/h3, color/spacing tokens
- `images/friends-family-sunday.jpg` — flyer 1
- `images/vbs-rome-2026.jpg` — flyer 2

## Implementation notes for `mtz-cms/`

1. Create a new Hiccup component file `src/mtz_cms/components/upcoming.clj`:
   - `(defn whats-coming-up [events])` — takes a vector of 1–2 event maps `{:image :alt :meta :title :blurb :cta-text :cta-href}` and renders the flyer grid.
   - `(defn this-sunday [services])` — takes the worship-times vector and renders the strip.
2. Wire both into the homepage render in `src/mtz_cms/ui/pages.clj` (or wherever the home view assembles sections).
3. Add `[:section {:id "events"} …]` and `[:section {:id "worship"} …]` wrappers so the existing header smooth-scroll keeps working.
4. The two flyers should be sourced from the Alfresco-backed events CMS where possible — but until then, hard-code with the content table above and let the secretary swap images by replacing the files at the same path.
5. Remove the old "Last Sunday" component from the homepage render (keep the component code; `/worship` may still want a "Recent sermons" block).
