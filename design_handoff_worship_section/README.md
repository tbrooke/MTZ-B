# Handoff: Worship Section — Gothic Arch

## Overview
A redesign of the **Worship** section on the Mt Zion UCC homepage (`mtzionucc.org`-style). The current page shows two services (9:30 Sunday School + 10:30 Traditional Worship) alongside a photo of the altar. The new section reflects current reality — **one service at 10:30 AM** — and replaces the altar photo with a photograph of the **Good Shepherd stained-glass window** from the church's own sanctuary, clipped into a Gothic arch so it reads as a window inside the page.

The section lives on the homepage immediately below the upcoming-events block and immediately above the footer/preschool band.

## About the Design Files
The files in this bundle are **design references created in HTML/JSX** — a static prototype showing the intended look. They are not production code to copy directly. The task is to **recreate this design inside the existing Mt Zion website codebase** (whatever CMS / framework it uses — WordPress block, Squarespace section, custom React/Astro component, etc.), using that codebase's established patterns, type system, color tokens, and image-handling conventions.

If the site is on a block-based CMS, this should become a single custom block / section. If it is a custom React/Astro/Next site, it should become one component (e.g. `<WorshipSection />`).

## Fidelity
**High-fidelity (hifi).** Colors, type, spacing, and the arched-window shape are intentional and final. Recreate pixel-close, using the existing site's font files and color tokens where they match.

## Screen / View

### Name
`WorshipSection` — a single full-width homepage section.

### Purpose
Tell a first-time visitor when worship happens, where, and what to expect — and invite them to plan a visit. One service, no schedule grid.

### Layout

The section is a full-width `<section>` on a warm off-white background (`#f7f4ee`). At the top, a thin **mint band** (`#e6efe6`, ~90 px tall) bleeds in from the section above so this section feels continuous with the events block.

Content sits inside a centered container with `96 px` horizontal padding (desktop). Vertical layout:

1. **Header row** (top of section, ~78 px from top edge)
   - Eyebrow label: `THIS SUNDAY`
   - H2 headline: `Worship` followed by an italic accent phrase `this Sunday` (the italic phrase is the brand's mint-green color)
   - 1 px dark horizontal rule spanning the full container, 36 px below the headline
2. **Two-column body** (40 px below the rule), grid `0.85fr 1fr` with `80 px` column gap, vertically centered:
   - **Left column**: the arched stained-glass image (see Components below)
   - **Right column**: time, service title, body copy, meta line, primary CTA button

Approximate desktop measurements at a 1440 px design width:
- Section padding: `78px 96px 0`
- Image column inner width: ~360 px (the arch frame)
- Image arch height: 520 px
- Headline → rule: 36 px
- Rule → body: 40 px

### Components

#### Eyebrow label `THIS SUNDAY`
- Font: **JetBrains Mono**, weight 500
- Size: 13 px
- Letter-spacing: 0.18em
- Text-transform: uppercase
- Color: `#2f5a3f` (mint/forest green)

#### Section headline `Worship this Sunday`
- Font: **Newsreader** (or the site's existing display serif), weight 400 (regular)
- Size: 56 px desktop / 40 px tablet / 32 px mobile
- Line-height: 1.05
- Letter-spacing: -0.01em
- Color: `#1b1b18`
- The word **`this Sunday`** is in **italic** and colored `#2f5a3f`
- Margin-top from eyebrow: 18 px

#### Horizontal rule
- 1 px solid `#1b1b18` at 0.85 opacity, full container width
- 36 px below the headline

#### Arched stained-glass window (left column)
This is the centerpiece. It is a photograph (`shepherd.jpg`) clipped into a pointed-arch shape with a dark frame, sized like a real sanctuary window.

Structure:
```
.arch-outer  ← dark frame (acts as window lead)
  └─ .arch-inner ← image, slightly smaller
       └─ <img src="shepherd.jpg" />
  └─ .arch-mullion ← vertical center line, optional decorative detail
.arch-plaque ← caption pill that sits over the bottom edge of the frame
```

Styling:
- `.arch-outer`:
  - Width: 360 px, height: 520 px
  - Background: `#1b1b18` (acts as the "lead" frame)
  - Padding: 10 px (creates the dark border around the image)
  - `border-radius: 180px 180px 6px 6px` — this is the trick that creates the pointed-arch top with square bottom corners
  - Box-shadow: `0 30px 60px -30px rgba(27,27,24,0.35)`
- `.arch-inner`:
  - Fills the padded area
  - `border-radius: 170px 170px 2px 2px`
  - `overflow: hidden` so the image is clipped to the arch
  - Background: `#000` (so any transparent edge of the image goes black, not cream)
- `<img>`:
  - `width: 100%; height: 100%; object-fit: cover; object-position: center 30%`
  - The 30% Y-offset keeps the figure of Christ centered in the arch
- `.arch-mullion` (optional decorative center line):
  - Absolutely positioned, 1 px wide, `rgba(27,27,24,0.45)`
  - Spans from ~200 px down from the top to 20 px above the bottom, horizontally centered
- `.arch-plaque`:
  - Absolutely positioned, bottom: -28 px (sits over the bottom edge), `left: 50%; transform: translateX(-50%)`
  - Background `#f7f4ee` (matches section bg), 1 px border `rgba(27,27,24,0.18)`
  - Padding: 8 px 18 px
  - Text: `THE GOOD SHEPHERD · C. 1910` *(verify date — this is a placeholder)*
  - Font: JetBrains Mono, 10 px, letter-spacing 0.2em, uppercase, color `#6a6a64`

#### Right column

##### Service time label
- Text: `10:30 AM  ·  EVERY SUNDAY`
- Font: JetBrains Mono, weight 500
- Size: 16 px
- Letter-spacing: 0.14em, uppercase
- Color: `#2f5a3f`

##### Service title `Gather in the Sanctuary.`
- Font: Newsreader, weight 400
- Size: 56 px
- Line-height: 1.05
- Two lines:
  - Line 1: `Gather in the` (regular)
  - Line 2: `Sanctuary.` (italic, color `#2f5a3f`)
- Margin: 18 px 0 22 px

##### Body paragraph
- Font: Newsreader, 18 px, line-height 1.55, color `#4a4a44`
- Max-width: 480 px
- Copy (use exactly): _"One service. Scripture, prayer, and song beneath the windows that have watched over this congregation since 1910 — with our choir and pipe organ."_

##### Meta line
- Font: Newsreader, 14 px, color `#6a6a64`
- Margin-top: 22 px
- Text: `Sanctuary  ·  ≈ 60 min  ·  Nursery provided`
- Use `·` (middle dot, U+00B7) as separators with `&nbsp;` padding on each side

##### Primary CTA
- Button: `Plan a visit →`
- Background: `#2f5a3f`, color: `#f7f4ee`
- Padding: 14 px 22 px
- Font: JetBrains Mono, 12 px, weight 500, letter-spacing 0.14em, uppercase
- No border, no border-radius (sharp corners — matches the editorial feel)
- Margin-top: 36 px from the meta line

## Interactions & Behavior

- **CTA button hover**: invert — background `#1b1b18`, text stays `#f7f4ee`. 150 ms transition on `background-color`.
- **CTA link target**: route to `/visit` (or whatever the existing "Plan a visit" page is on the current site).
- **Arched image**: not interactive. No hover, no link. It's content, not navigation.
- **No carousel, no multiple services, no tabs.** Static, single-purpose section.

### Responsive behavior

| Breakpoint | Behavior |
|---|---|
| `≥ 1024 px` (desktop) | Two columns as described. |
| `768–1023 px` (tablet) | Two columns, container padding reduced to `48 px`, column gap reduced to `48 px`. |
| `< 768 px` (mobile) | Single column. Arched image centered, max-width 280 px, height 400 px. All text re-aligns left. Headline drops to 36 px. Service title drops to 36 px. |

## State Management
None. This is a static content section. If the existing site is on a CMS, the following fields should be authorable:

- Eyebrow text (default `THIS SUNDAY`)
- Headline + italic phrase (default `Worship` / `this Sunday`)
- Service time (default `10:30 AM`)
- Service frequency (default `EVERY SUNDAY`)
- Service title line 1 + line 2-italic (default `Gather in the` / `Sanctuary.`)
- Body paragraph
- Meta line items (array of strings, joined with `·`)
- CTA label + href
- Stained-glass image (with alt text)
- Image plaque text (default `THE GOOD SHEPHERD · C. 1910`)

## Design Tokens

### Colors
| Token | Value | Use |
|---|---|---|
| `--bg-cream` | `#f7f4ee` | Section background, button text |
| `--bg-mint` | `#e6efe6` | Top band that bleeds in from the section above |
| `--ink-primary` | `#1b1b18` | Headlines, body, arch frame, rule |
| `--ink-secondary` | `#4a4a44` | Body paragraph |
| `--ink-tertiary` | `#6a6a64` | Meta line, plaque text |
| `--accent-green` | `#2f5a3f` | Eyebrow, italic accents, CTA background |
| `--accent-green-soft` | `#cfe2d6` | (Reserved — used in dark-hero variant, not here) |
| `--frame-black` | `#1b1b18` | Arch frame (same as `--ink-primary`) |

### Typography
| Token | Family | Use |
|---|---|---|
| `--font-display` | Newsreader, Georgia, serif | Headlines, service title, body |
| `--font-mono` | JetBrains Mono, ui-monospace, monospace | Eyebrow, time, meta-as-uppercase, CTA |

Load weights: Newsreader 400 (with italic), JetBrains Mono 400/500.

### Spacing scale (used in this section)
`10, 14, 18, 22, 28, 36, 40, 48, 56, 64, 78, 80, 96` (px).

### Border radius
- Arch outer: `180px 180px 6px 6px`
- Arch inner: `170px 170px 2px 2px`
- Plaque: `0` (sharp)
- Button: `0` (sharp)

### Shadow
- Arch frame: `0 30px 60px -30px rgba(27,27,24,0.35)`

## Assets

| File | Source | Notes |
|---|---|---|
| `assets/shepherd.jpg` | Photograph provided by the church (original: `DSC00858.jpg`) | The Good Shepherd stained-glass window in the sanctuary. Currently 1200×1809, ~645 KB. For production, generate responsive sizes (e.g. 600w / 900w / 1200w) and serve via `<picture>` or the framework's image component. Use intrinsic `width`/`height` attributes to avoid CLS. |

Alt text: `Stained-glass window in the Mt Zion sanctuary depicting Christ as the Good Shepherd with a lamb and sheep`.

The original full-resolution photos (~7 MB each) live in the project's `uploads/` folder and should be kept as the source of truth; only the resized `shepherd.jpg` is needed for the web build.

## Files in this bundle

- `README.md` — this document
- `Worship Section.html` — the full prototype with all 5 explorations (V2 is the one to implement)
- `worship-variations.jsx` — the source for all 5 variations; the **`V2GothicArch`** component is the canonical reference
- `design-canvas.jsx` — the canvas wrapper used to present the explorations side-by-side (not needed in production)
- `assets/shepherd.jpg` — the stained-glass image to ship
