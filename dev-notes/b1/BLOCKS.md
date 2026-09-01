# B1 blocks, and how the Mt Zion design attaches to them

Notes from a working experiment on `mtzstage.mtzcg.com`, 2026-08-31. Parked
deliberately — pick this up when the hero needs to change (Christmas, Easter) or
when new pages start getting built.

## Why blocks

A `sectionBlock` is **saved content**, not a stylesheet: real sections and
elements with real copy and images. A page section points at one through a
single `targetBlockId` field, so swapping a seasonal hero is a one-field edit —
in B1Admin, or via the Api's MCP `api_call` from Claude Desktop.

Blocks are a **shared instance, not a copy**. Five pages referencing one block
show identical content. That suits invariant furniture (footer, a CTA band) and
suits a hero that is swapped in and out. It does *not* suit "the same shape with
different words per page" — that is a recipe, not a block.

## What the experiment established

**A referencing section is replaced, not decorated.** Gave the block's section a
teal background and the referencing page section orange: only teal rendered, and
only one section appeared. The block's own sections render, carrying their own
`background` and `textColor`.

  → **a seasonal hero block carries its own photo.** This was the open question.

**Block-owned elements need `blockId` as well as `sectionId`.** An element with
`sectionId` set and `blockId` NULL is silently not loaded — the section renders
empty. Every element in the working `Site Footer` block carries both. Anything
creating blocks, by hand or via MCP, must set it.

**Section `stylesJSON` is ignored everywhere.** Page sections render
`id="section-undefined"`, and apphelper's own source acknowledges this
("...or sections lose ids"). Block sections *do* get a real id
(`section-<id>`) — but setting `stylesJSON` on one still emits no rule at all.
So the id is not the cause, and **all section-level styling must come from
`globalStyles.customCss`**. Do not plan around per-section styles.

**Block sections never receive `.sectionFirst`.** That class is computed from
position in the *page's* section list. A hero block therefore got photo-band
styling but not the hero box: 100px tall instead of 640, top-aligned, 57.6px
headline. It also looked noticeably darker — the scrim sweeps 10%→65% black over
whatever height the section has, so a short band is mostly the dark end.

## The convention that closes it

Sections cannot carry a custom class, and `stylesJSON` is dead, so the hook has
to come from content the block owns. Wrap the copy in the text element:

```html
<div class="hero-copy">
  <p class="eyebrow">Welcome to Mt. Zion UCC &middot; est. 1755</p>
  <h1>A family-oriented church in China Grove &mdash; <em>come as you are.</em></h1>
  <p class="lede">Sunday Service at 10:30</p>
</div>
```

`customCss` keys the hero box off `.sectionBG:has(.hero-copy)`, so the block
declares its own role and is styled wherever it is dropped. Verified: with
`sectionFirst` false, the block still rendered 640px, bottom-aligned, 78px white
headline — identical to the home page.

The same shape generalises: `.feature-band`, `.cta-band`, etc.

### House classes

These are the words the design responds to. A block or a page section is
on-brand because its HTML uses them, not because of where it sits.

| class | what it does |
|---|---|
| `.hero-copy` | wraps hero copy; gives the section the hero box |
| `.eyebrow` | uppercase letterspaced kicker above a heading |
| `.lede` | the standfirst line inside a hero |
| `.lede-intro` | the 26px intro paragraph band under a hero |
| `.arrow-link` | uppercase underlined "SEE ALL EVENTS →" link |
| `<em>` in `h1`/`h2` | mid-sentence italic in mint |

Headings cannot be coloured directly — apphelper ships
`.elText h1..h6 { color: inherit !important }`, so set the colour on the element
wrapper the heading inherits from.

## To convert the home hero to a block

Not done. The home hero is still an ordinary page section (`s0SXdkEwZ29` on `/`).
The conversion is:

1. Create a block: `INSERT INTO content.blocks (id, churchId, blockType, name, siteId)`
   with `blockType='sectionBlock'`, e.g. name `Hero — Default`.
2. Create a section owned by it — `blockId` set, `pageId` NULL — copying the
   hero's `background`, `textColor: light`, `sort: 0`, `zone: main`.
3. Copy the hero's elements across with **both** `sectionId` and `blockId` set,
   wrapping the text element's copy in `<div class="hero-copy">`.
4. Point the home page's first section at it: set `targetBlockId` to the block
   id and clear its own `background`.
5. Duplicate the block for `Hero — Christmas` / `Hero — Easter`; swap by
   changing `targetBlockId` on that one section.

Then clear the cache and restart, as always:

```bash
docker exec b1church-b1app-1 sh -lc 'rm -rf .next/cache/fetch-cache/*'
docker restart b1church-b1app-1
```

## The test artifacts

The experiment left block `zzTestBlk01` ("ZZ Test Hero") with section
`zzTestBlkSc` and element `zzTestBlkEl` — a working worked-example of a hero
block. The page that exposed it at `/zz-block-test` has been removed so nothing
renders publicly. To look at it again, recreate one referencing section:

```sql
INSERT INTO content.pages (id, churchId, url, title, layout)
VALUES ('zzTestPag01','eBRdOVmGxt1','/zz-block-test','ZZ Block Test','headerFooter');
INSERT INTO content.sections (id, churchId, pageId, zone, sort, targetBlockId)
VALUES ('zzTestPagSc','eBRdOVmGxt1','zzTestPag01','main',0,'zzTestBlk01');
```

Delete the block entirely when it has served its purpose.

## Related

- `mtz-home.css` / `mtz-header.js` here are copies of
  `content.globalStyles.customCss` / `customJS`; the database is what B1App reads.
- The Api ships an MCP server at `POST /mcp` (`b1api.mtzcg.com`) whose
  `describe_page_builder` tool documents the element vocabulary. What it cannot
  know is the house classes above — that is the doc to hand Claude Desktop
  alongside it, the analogue of `content-inbox/CONTRACT.md`.
