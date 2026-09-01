# Mt Zion house style — for Claude authoring B1 content

Hand this to Claude Desktop **alongside** the Api's own `describe_page_builder`
tool. That tool teaches the *mechanics* — the Page → Section → Element model and
every `elementType`'s `answersJSON` shape. It cannot know any of what follows:
our conventions, our safety rules, and several things it gets silently wrong.

This is the B1 successor to `content-inbox/CONTRACT.md`.

## Workflow rules

**1. Call `describe_page_builder` first, once.** ~2,600 tokens, and it carries
the whole data model. Do this before any `/content/*` work.

**2. Never call `list_endpoints` unfiltered.** Filtered to just `/content/` it
returns over 2,000 lines. Use `describe_page_builder`; reach for
`describe_endpoint` only for a specific route you cannot infer.

**3. Create content unpublished, always.** `describe_page_builder` does not
mention publishing at all — it has zero occurrences of "publish" — so left to
itself Claude will create content and never consider the draft state. But the
endpoints exist:

| route | what it does |
|---|---|
| `POST /content/pages/:id/publish` | makes a page live |
| `POST /content/pages/:id/discard` | throws away unpublished edits |
| `GET  /content/pages/:id/published` | the live version |

A new page is unpublished by default — **leave it that way**. Publishing is a
human decision, exactly as it was in the old importer: extract, review, publish.
Never call `/publish` unless the person explicitly asks in that turn.

**4. Verify by reading back.** `GET /content/pages/:churchId/tree?url=/thing`
returns the whole tree with `answers` / `styles` parsed into objects. Use it
after writing.

**5. Check before you create.** Nothing in this API is idempotent. Re-running a
create makes a second copy. Before adding an event, post or page, `GET` and look
for an existing match on its natural key (title + date, or url). The old
importer did this automatically; here it is on you.

## House classes

The design responds to these words. Content is on-brand because its HTML uses
them — **not** because of where it sits on the page. Use them and a new page
looks like Mt Zion with no styling work.

| class | use it for |
|---|---|
| `.hero-copy` | wrapper around hero copy; gives the section the hero box (640px, bottom-aligned, display headline) |
| `.eyebrow` | small uppercase letterspaced kicker above a heading |
| `.lede` | the standfirst line inside a hero |
| `.lede-intro` | a 26px intro paragraph in its own band under a hero |
| `.arrow-link` | uppercase underlined call-to-action: `SEE ALL EVENTS →` |
| `<em>` inside `h1`/`h2` | mid-sentence italic in mint — the signature of this design |

Use `&mdash;`, `&middot;` and `&rsquo;` rather than the bare characters; the
existing copy does, and straight apostrophes look wrong in this serif.

## Section conventions

A `text` element's `text` field is HTML. A section's look comes from its own
columns plus the classes inside it.

- **Hero** — section `background` = an image URL, `textColor: "light"`, and the
  text element wraps its copy in `<div class="hero-copy">`. That is the whole
  recipe; the photo scrim, white heading, mint emphasis and 78px display size
  follow automatically.
- **Bands** — alternate `#eef6f4` (mint tint) and `#faf6ef` (cream) with
  `textColor: "dark"`. Plain white is fine for a lede band.
- Body copy sits at a 640px measure automatically. Do not fight it.

### A minimal page, start to finish

```jsonc
// 1. the page  (churchId is set from your token — never pass it)
POST /content/pages    [{ "url": "/example", "title": "Example" }]
// 2. sections
POST /content/sections [{ "pageId": "<id>", "zone": "main", "sort": 0,
                          "background": "https://imagedelivery.net/…/public",
                          "textColor": "light" },
                        { "pageId": "<id>", "zone": "main", "sort": 1,
                          "background": "#faf6ef", "textColor": "dark" }]
// 3. elements — answersJSON is a JSON *string*, not an object
POST /content/elements [{ "sectionId": "<id>", "elementType": "text", "sort": 0,
                          "answersJSON": "{\"textAlignment\":\"left\",\"text\":\"<div class=\\\"hero-copy\\\">…</div>\"}" }]
// 4. read it back, and STOP. Do not publish.
GET  /content/pages/eBRdOVmGxt1/tree?url=/example
```

Worked example live at `/mcp-test`, built entirely through `api_call`.

## Things that will silently not work

**Section `stylesJSON` is ignored — everywhere.** Page sections render
`id="section-undefined"`; block sections get a real id and it still emits
nothing. Never try to style a section through `stylesJSON`; it fails without an
error. Section-level design lives in `globalStyles.customCss` and is not
Claude's to edit.

**Heading colours cannot be set.** apphelper ships
`.elText h1..h6 { color: inherit !important }`. Do not put `color` on a heading;
it is already handled by the section's `textColor`.

**Block-owned elements need `blockId` as well as `sectionId`,** or they are
silently not loaded and the section renders empty. See `BLOCKS.md`.

**Do not invent classes.** A class not in the table above has no styling behind
it. If a design needs something new, that is a `customCss` change — say so
rather than inventing a class name that will render unstyled.

## After writing

B1App keeps a persistent Next.js fetch cache that survives a restart. Content
changes may not appear until someone runs, on `trust`:

```bash
docker exec b1church-b1app-1 sh -lc 'rm -rf .next/cache/fetch-cache/*'
docker restart b1church-b1app-1
```

Claude cannot do this over MCP. Mention it when content does not show up.

## Connection

`POST https://b1api.mtzcg.com/mcp`, bearer token. The current key is scoped
`content:read content:write` only — `/membership/*` correctly returns 401, and
`churchId` is forced from the token, so writes cannot escape this church.

The key lives at `~/.config/mtz/b1-mcp-key` on the Mac (mode 600, **not** in this
repo). Key id `cUBcrOzZa57`, prefix `d7074a8d`. Revoke with
`DELETE FROM membership.apiKeys WHERE id='cUBcrOzZa57';` and mint a fresh one
via `POST /membership/apiKeys` — the plaintext is shown exactly once, so store it
before navigating away.

One transport quirk: a default `Python-urllib` User-Agent gets a **403** from the
edge before reaching the Api. Send a normal UA. `mcp.py` here does.
