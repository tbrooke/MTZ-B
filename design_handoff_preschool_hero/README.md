# Handoff: Mt. Zion Preschool — Top Section (Header + Hero)

## Overview
This handoff covers the **top section** of the Mt. Zion Preschool homepage — the header (nav + brand mark + Church-toggle) and the hero (headline, lede, CTAs, and Linda's line-art graphic).

The preschool site is a sibling of the existing Mt. Zion Church site. It uses the same chrome (cream header band, centered brand mark, mirrored nav, toggle pill on the right) but with **preschool-specific nav items** and a **toggle that flips back to the Church site**.

The full HTML reference includes additional sections below the fold (welcome, programs, day-in-the-life, values, enrollment, footer). **Focus is on the top section** — the rest is included only as visual context for type/color/component reuse.

## About the Design Files
The files in this bundle are **design references created in HTML** — a static prototype showing the intended look and behavior, not production code to copy directly.

Your task is to **recreate the top section inside the existing Mt. Zion codebase**, using whatever framework and patterns are already established there (React, Astro, plain templates, etc.). The preschool side should reuse the church site's existing components (header shell, brand mark, button styles, type ramp) wherever possible — only the nav links, page content, and toggle direction change.

If you find that the church site does not yet have the header componentized in a way that supports two sibling sites, take this opportunity to factor it out into a shared component that accepts the nav items and toggle target as props/config.

## Fidelity
**High-fidelity.** Colors, type, spacing, and component proportions in the reference are intentional and should be matched. Treat hex values, font sizes, and the grid below as the source of truth.

The one deliberate exception: **Linda's artwork SVG**. The version included here is the raw artwork; in production it will be served from **Cloudflare** (see "Assets" below). Wire the `<img>` `src` to the Cloudflare URL rather than bundling the SVG.

---

## Screens / Views

### 1. Header (sticky chrome, shared with the rest of the preschool site)

**Layout**
- Full-width band, background `--cream` (`#F2EBDD`), bottom border `1px solid --rule` (`#E4DCC8`).
- Inner container: `max-width: 1320px`, centered, padding `28px 48px 36px`.
- Three-column grid: `1fr auto 1fr`, `align-items: center`, column gap `32px`.
  - **Left**: primary nav (`Home`, `About ▾`, `Programs ▾`, `Enrollment`), `gap: 56px`.
  - **Center**: brand cluster (logo mark + "MT ZION" + "PRESCHOOL" + locale line).
  - **Right**: secondary nav (`Calendar ▾`, `Staff`, `Contact`) + **Church** toggle pill, `gap: 56px`, justified to the end.

**Nav links**
- Font: `Outfit`, weight 400, letter-spacing `0.18em`, uppercase, `13px`, color `--ink` (`#1A1A1A`).
- Hover: color shifts to `--green` (`#1B6B53`).
- Dropdown indicator: small downward triangle, `3px` borders, `6px` left margin, `translateY(-2px)`. Applied to items marked with `.has-caret` in the reference (`About`, `Programs`, `Calendar`).

**Brand cluster (center)**
- Mark: a simple triangle/mountain outline, `56×38px`, stroke `--green`, stroke-width `1.6`, square line caps. Three paths: left ridge, right ridge, baseline. (See `reference/Mt Zion Preschool.html` for the exact SVG path data.)
- Wordmark line 1: "MT ZION" — `Outfit` 500, `18px`, letter-spacing `0.22em`, color `--ink`, `2px` margin-top.
- Wordmark line 2: "PRESCHOOL" — `Outfit` 500, `11px`, letter-spacing `0.22em`, color `--ink`, `2px` margin-top.
- Locale: "CHINA GROVE, / NORTH CAROLINA" — `IBM Plex Mono` 400, `9.5px`, letter-spacing `0.16em`, color `--muted` (`#6B6358`), `6px` margin-top, line-height `1.3`.
- The entire cluster is a single `<a>` to the preschool home.

**Church-toggle pill (right edge)**
- Pill button, `1px solid --ink`, border-radius `999px`, padding `12px 18px`, transparent background, color `--ink`.
- Contents: small 24×24 house icon + the word `Church`, gap `10px`. Icon is a simple house outline (roof line + body rect + door), stroke-width `1.5`, color currentColor.
- Hover: background `--ink`, color `--cream`.
- Label uses the same nav typography (`Outfit` 400, `13px`, `0.18em` tracking, uppercase).
- **Behavior**: clicking navigates to the **church** site root (mirroring the church site's existing "Preschool" button which navigates here). The two buttons are mirror images of each other.

### 2. Hero

**Layout**
- Inner container: `max-width: 1320px`, centered, padding `72px 48px 88px`.
- Two-column grid: `1.05fr 1fr`, column gap `72px`, `align-items: center`.
- Below the hero, a `max-width: 1320px` container holds an `<hr>` at `--rule` color as a section divider.

**Left column — copy**
1. **Eyebrow**: "A Nurturing Christian Early Childhood Program"
   - `IBM Plex Mono` 500, `13px`, letter-spacing `0.18em`, uppercase, color `--green`.
   - `28px` margin-bottom.
2. **Headline (h1)**: "Where little ones *grow, play,* and find their place in the world."
   - `Cormorant Garamond` 500, `clamp(54px, 6vw, 88px)`, line-height `1.02`, letter-spacing `-0.01em`, color `--ink`, `text-wrap: pretty`.
   - The phrase **"grow, play,"** is wrapped in `<em>` — italic, weight 400, color `--green`.
   - `28px` margin-bottom.
3. **Lede**: short paragraph about the program (age 2–5, China Grove, since 1989).
   - `Cormorant Garamond` 400, `24px`, line-height `1.45`, color `--ink-2` (`#2A2A2A`), `max-width: 520px`, `40px` margin-bottom.
4. **CTA row** — `display: flex`, `gap: 14px`, `flex-wrap: wrap`.
   - **Primary**: "Inquire about enrollment" — bg `--green`, text white, `1px` solid `--green`. Hover bg `--green-deep` (`#155743`).
   - **Secondary**: "Our programs" — transparent, `1px solid --ink`, text `--ink`. Hover swaps to `--ink` bg / `--cream` text.
   - Both buttons: `Outfit` 500, `12.5px`, letter-spacing `0.16em`, uppercase, padding `18px 28px`, border-radius `2px`, transition `all 0.15s ease`.

**Right column — artwork (the focus of this handoff)**
- A relatively positioned frame, `aspect-ratio: 558 / 400` (matches the SVG's `viewBox`), `width: 100%`, `max-width: 640px`, `justify-self: end`.
- Four stacked children, in z-order back→front:
  1. **Backplate** (`.backplate`): absolutely positioned, `inset: -28px -32px -40px -16px` (intentionally asymmetric — slightly offset down-right behind the art), background `--cream-soft` (`#F8F3E8`), border-radius `4px`, z-index `0`.
  2. **Artwork** (`<img class="art-img">`): the Linda line-art SVG, `width/height: 100%`, `object-fit: contain`, z-index `1`. Currently rendered with `filter: brightness(0) saturate(100%)` to flatten the multi-class paths to solid `--ink`. *See the "Coloring the artwork" note below.*
  3. **Dots** (`.dot.tl`, `.dot.br`): two `14×14` circles, `border-radius: 999px`, z-index `2`.
     - `.tl` — top: `-10px`, left: `10%`, color `--sun` (`#E8C77A`).
     - `.br` — bottom: `-8px`, right: `18%`, color `--green`.
  4. **Stamp** (`.stamp`): `Original artwork · Linda M.` — absolutely positioned at `bottom: -22px; left: -8px`, `IBM Plex Mono` 400, `10.5px`, letter-spacing `0.16em`, uppercase, color `--muted`, background `--paper`, padding `4px 10px`, `1px solid --rule`, z-index `3`.

**Coloring the artwork**
The source SVG has many `cls-*` classes but **no `<style>` block defines fills** in the file we received — so browsers render every path in the default fill (black). The reference handles this with a `brightness(0)` filter to normalize the look to a single ink color.

For production you have two clean options:
- **Easiest (recommended)**: serve the SVG as-is from Cloudflare and apply `filter: brightness(0) saturate(100%) invert(28%) sepia(15%) saturate(1600%) hue-rotate(115deg)` to tint it `--green` — or just leave it flat black if you prefer the reference look.
- **Cleaner**: ask Linda (or whoever maintains the artwork) to export an SVG with a single `currentColor` fill, then style with `color: var(--green)` and drop the filter. Inlining the SVG via `fetch` + `innerHTML` is also fine if the Cloudflare bucket allows it.

---

## Interactions & Behavior

- **Nav hover**: color transitions from `--ink` to `--green`. No underline.
- **Dropdown carets** (`About`, `Programs`, `Calendar`) — the reference doesn't implement the dropdown panels themselves. Match whatever the church site uses for its dropdown menus (this header should reuse the same component).
- **Church toggle**: on click, navigate to the church site root. Same transition style as the church-side "Preschool" button (mirror it exactly — including any view-transition / cross-fade the church site uses).
- **Buttons**: `0.15s ease` transition on all properties.
- **Responsive**:
  - `≤ 1080px`: nav gaps tighten to `32px`, container padding drops to `28px`.
  - `≤ 920px`: nav links hide (replace with a mobile menu — the church site likely already has one; reuse it), brand cluster centers alone, hero collapses to a single column with `48px` row gap, artwork shrinks to fit.
- **Reduced motion**: no specific motion in the top section, so no special handling required. If you add a parallax or fade-in on the artwork later, gate it behind `@media (prefers-reduced-motion: no-preference)`.

## State Management
The top section is static — no client state. The only dynamic value is the **current site** (church vs. preschool), which is implicit in the route. The toggle is just a link, not a stateful component.

---

## Design Tokens

```css
/* Backgrounds & surfaces */
--cream:        #F2EBDD;   /* header & footer band */
--cream-soft:   #F8F3E8;   /* hero artwork backplate, alt section bg */
--paper:        #FFFFFF;   /* default page bg */

/* Ink */
--ink:          #1A1A1A;   /* primary text, brand mark */
--ink-2:        #2A2A2A;   /* body / lede text */
--muted:        #6B6358;   /* mono labels, locale, stamp */

/* Lines */
--rule:         #E4DCC8;   /* hairline dividers, border under header */

/* Brand greens */
--green:        #1B6B53;   /* primary green — CTAs, italics, brand mark */
--green-deep:   #155743;   /* primary CTA hover */
--green-tint:   #E6EFEB;   /* available for soft green surfaces */

/* Warm accents (preschool-only — do NOT use on the church side) */
--sun:          #E8C77A;   /* hero dot top-left */
--blush:        #E9B69A;   /* reserved for program tags */
```

### Typography
| Role                | Family              | Weight | Size                          | Tracking  | Notes |
| ------------------- | ------------------- | ------ | ----------------------------- | --------- | ----- |
| Display / headlines | Cormorant Garamond  | 500    | `clamp(54px, 6vw, 88px)`      | `-0.01em` | `<em>` switches to italic 400 + `--green` |
| Lede                | Cormorant Garamond  | 400    | `24px / 1.45`                 | normal    | color `--ink-2` |
| Body                | Cormorant Garamond  | 400    | `19px / 1.55` (base)          | normal    | set on `<body>` |
| Nav                 | Outfit              | 400    | `13px`                        | `0.18em`  | uppercase |
| Brand wordmark      | Outfit              | 500    | `18px` / sub `11px`           | `0.22em`  | uppercase |
| Buttons             | Outfit              | 500    | `12.5px`                      | `0.16em`  | uppercase |
| Eyebrows (mono)     | IBM Plex Mono       | 500    | `13px`                        | `0.18em`  | uppercase, color `--green` |
| Locale / stamps     | IBM Plex Mono       | 400    | `9.5–10.5px`                  | `0.16em`  | uppercase, color `--muted` |

Load via Google Fonts:
```
Cormorant+Garamond: 400, 500, 600, 700 (+ italic 400)
Outfit: 300, 400, 500, 600
IBM+Plex+Mono: 400, 500
```

### Spacing
- Container max-width: **1320px**, side padding **48px** (28px under 1080px).
- Hero block: top **72px**, bottom **88px**.
- Header inner: **28px** top, **36px** bottom, **48px** sides.
- Hero column gap: **72px** (collapses to **48px** row gap on stack).
- CTA row gap: **14px**.

### Border radius
- Buttons: `2px` (intentionally square — matches the church site's typographic, editorial feel).
- Toggle pill: `999px`.
- Hero backplate: `4px`.

### Shadows
None in the top section. Keep it flat — the church site's identity is hairline rules and color, not elevation.

---

## Assets

### Linda's hero artwork
- **File in this bundle**: `reference/assets/linda-artwork.svg` (the original upload, 558×400 viewBox).
- **Production source**: **Cloudflare** (R2 or Images). Drop the SVG into the Cloudflare bucket the rest of the Mt. Zion site uses and wire the URL into the `<img src>` of the hero.
  - Suggested filename in the bucket: `preschool/hero/linda-line-art.svg`
  - The final URL will look like `https://<your-cloudflare-domain>/preschool/hero/linda-line-art.svg` — replace the placeholder in your code accordingly.
- **Notes**:
  - The SVG has no defined fills in its `<style>` block — paths render black by default. See "Coloring the artwork" in the Hero section for tint options.
  - The artwork **must keep its aspect ratio** (`558 / 400 ≈ 1.395`). The reference uses CSS `aspect-ratio` + `object-fit: contain` to guarantee this regardless of column width.
  - The artwork is **decorative**; `<img>` `alt=""` is correct. The hero meaning comes from the headline text.

### Brand mark (logo)
Inlined SVG in the reference (`<svg class="brand-mark" viewBox="0 0 56 38">`). Copy the three `<path>` elements as-is — or, if the church site already has a `<BrandMark>` component, reuse it (the preschool side should use the same mark).

### Icons
- Toggle button "Church" icon: small inline house SVG (24×24, currentColor strokes). See the `<button class="toggle-btn">` in the reference. If the church site uses an icon library (Lucide, Phosphor, Heroicons), swap to its equivalent for consistency.

---

## Files in this bundle

```
design_handoff_preschool_hero/
├── README.md                         ← you are here
└── reference/
    ├── Mt Zion Preschool.html        ← full design reference (focus on header + hero)
    └── assets/
        └── linda-artwork.svg         ← raw artwork (production version comes from Cloudflare)
```

Open `reference/Mt Zion Preschool.html` directly in a browser to see the design. The top ~600px is the target of this handoff; the rest is included as supporting context for type, color, and component patterns you may also need when building out the other preschool pages.
