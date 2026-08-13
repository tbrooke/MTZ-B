# Mount Zion content contract

*Generated from the code — do not edit by hand. Run `clj -M:run content-doc`.*

You are extracting content from a church bulletin or slide deck into EDN that a
CLI tool will import. Output **one EDN map and nothing else** — no prose, no
markdown fence, no commentary.

## Ground rules

- Every item needs a `:type` and a `:key`.
- `:key` is a stable lowercase identifier you choose. Reuse it if you re-extract
  the same item later — that is how corrections replace rather than duplicate.
- **All dates are Mount Zion local time.** Dates are `"YYYY-MM-DD"`; dates with a
  time are `"YYYY-MM-DDTHH:MM"` on a 24-hour clock. No timezone suffix.
- Booleans are `true` / `false`, never `1` / `0` / `"on"`.
- Omit a field entirely to mean "not provided".
- Unrecognised keys are an error, not something quietly ignored.
- Nothing you write is published. Everything imports as a draft for a human to
  review, so `:published` is not part of this contract.

## Envelope

```clojure
{:mtz/contract 1
 :contract-sha "<the sha at the bottom of this document>"
 :source {:kind :bulletin
          :files ["Bulletin 8-9-26.pdf"]
          :extracted-on "2026-08-07"}
 :items [...]}
```

## Item types

### `:type :event`

Anything with a date and time.

| field | required | accepts |
|---|---|---|
| `:type` | **yes** | always `:event` |
| `:key` | **yes** | must be a stable lowercase identifier, 3-64 chars, e.g. "vbs-2026" |
| `:title` | **yes** | text, 1–200 chars |
| `:starts-at` | **yes** | must be a date and time like "2026-08-09T18:30" (Mount Zion local time, 24-hour) |
| `:ends-at` |  | must be a date and time like "2026-08-09T18:30" (Mount Zion local time, 24-hour) |
| `:all-day` |  | `true` or `false` |
| `:location` |  | text, up to 200 chars |
| `:description` |  | hiccup — see [Body content](#body-content) |
| `:recurrence` |  | one of `:none`, `:daily`, `:weekly`, `:biweekly`, `:monthly`, `:yearly` |
| `:recur-until` |  | must be a date like "2026-08-09" |
| `:featured` |  | `true` or `false` |

### `:type :post`

News items and reflections. Appears under /news.

| field | required | accepts |
|---|---|---|
| `:type` | **yes** | always `:post` |
| `:key` | **yes** | must be a stable lowercase identifier, 3-64 chars, e.g. "vbs-2026" |
| `:title` | **yes** | text, 1–200 chars |
| `:slug` |  | must be lowercase letters, digits and dashes, e.g. "johns-river" |
| `:category` |  | one of `:news`, `:blog`, `:reflection` |
| `:excerpt` |  | text, up to 300 chars |
| `:body` |  | hiccup — see [Body content](#body-content) |
| `:published-on` |  | must be a date like "2026-08-09" |
| `:show-on-home` |  | `true` or `false` |

### `:type :page`

A standalone page, optionally filed under a menu section.

| field | required | accepts |
|---|---|---|
| `:type` | **yes** | always `:page` |
| `:key` | **yes** | must be a stable lowercase identifier, 3-64 chars, e.g. "vbs-2026" |
| `:slug` | **yes** | must be lowercase letters, digits and dashes, e.g. "johns-river" |
| `:title` |  | text, up to 200 chars |
| `:parent` |  | one of `:about`, `:worship`, `:events`, `:activities`, `:news`, `:outreach`, `:contact` |
| `:nav-label` |  | text, up to 40 chars |
| `:nav-order` |  | whole number 1–99 |
| `:body` |  | hiccup — see [Body content](#body-content) |

### `:type :feature`

Editorial content in a named slot on an existing page.

| field | required | accepts |
|---|---|---|
| `:type` | **yes** | always `:feature` |
| `:key` | **yes** | must be a stable lowercase identifier, 3-64 chars, e.g. "vbs-2026" |
| `:page-slug` | **yes** | one of `:home`, `:home-worship`, `:current-theme`, `:activities` |
| `:title` | **yes** | text, 1–200 chars |
| `:subtitle` |  | text, up to 200 chars |
| `:body` |  | hiccup — see [Body content](#body-content) |
| `:cta-label` |  | text, up to 60 chars |
| `:cta-url` |  | text, up to 300 chars |
| `:sort-order` |  | whole number 0–999 |
| `:show-on-home` |  | `true` or `false` |

### `:type :sermon`

One Sunday's service record.

| field | required | accepts |
|---|---|---|
| `:type` | **yes** | always `:sermon` |
| `:key` | **yes** | must be a stable lowercase identifier, 3-64 chars, e.g. "vbs-2026" |
| `:sermon-date` | **yes** | must be a date like "2026-08-09" |
| `:title` | **yes** | text, 1–200 chars |
| `:scripture-cw` |  | text, up to 120 chars |
| `:scripture-gospel` |  | text, up to 120 chars |
| `:series` |  | must be lowercase letters, digits and dashes, e.g. "johns-river" |
| `:description` |  | text, up to 2000 chars |

Valid `:parent` values for a page: `:about`, `:activities`, `:contact`, `:events`, `:news`, `:outreach`, `:worship`.

## Body content

Body fields (`:body`, `:description` on events) are **hiccup** — Clojure data,
not an HTML string:

```clojure
:body [[:p "A paragraph with " [:strong "bold"] " in it."]
       [:ul [:li "first"] [:li "second"]]]
```

It is always a **vector of nodes**, even for a single paragraph.

Only these tags are accepted. Anything else is rejected — this list matches
what the site's editor can produce, so imported content stays editable.

| tag | attributes allowed |
|---|---|
| `:a` | `:href` `:rel` `:target` `:title` |
| `:blockquote` | — |
| `:br` | — |
| `:code` | — |
| `:em` | — |
| `:h2` | — |
| `:h3` | — |
| `:h4` | — |
| `:hr` | — |
| `:img` | `:alt` `:height` `:src` `:title` `:width` |
| `:li` | — |
| `:ol` | `:start` |
| `:p` | — |
| `:pre` | — |
| `:s` | — |
| `:strong` | — |
| `:table` | — |
| `:tbody` | — |
| `:td` | `:colspan` `:rowspan` |
| `:th` | `:colspan` `:rowspan` `:scope` |
| `:thead` | — |
| `:tr` | — |
| `:ul` | — |

`:class` is allowed on every tag. Links may use `https://`, `http://`, `/`,
`#`, `mailto:` or `tel:`. Images must come from Cloudflare Images or a `/images/`
path — a bare external URL is rejected.

Not allowed, and worth knowing why:

- `:h1` — the page already renders its own heading.
- `:div`, `:span` — layout belongs to the site, not to content.
- `:style` attributes, `:script`, `:iframe`, any `on*` handler.
- Raw HTML of any kind, including `[:lambdaisland.hiccup/unsafe-html "…"]`.

## Common mistakes

These are the failures that actually happen. Read them before writing anything.

| don't | do |
|---|---|
| `:starts-at "6:30 PM"` | `:starts-at "2026-08-13T18:30"` |
| `:starts-at #inst "2026-08-13"` | plain strings only — no `#inst`, no `#uuid` |
| `:featured 1` | `:featured true` |
| `:body "<p>text</p>"` | `:body [[:p "text"]]` |
| `:published true` | omit it — everything imports as a draft |
| `:image-id "abc"` | omit it — images are attached by hand afterwards |
| twelve items for a weekly event | **one** item with `:recurrence :weekly` |

**If you are unsure about a value, leave the field out.** A missing field is easy
to add in the admin panel; a confidently wrong date is not.

**Recurring events are one item.** A bulletin lists choir practice every week;
that is a single `:event` with `:recurrence :weekly`, not one item per week.

**Leave `:recur-until` out unless the bulletin gives an end date.** Ongoing
activities — Tai Chi, choir, Scouts, pickleball — simply run, and an omitted
`:recur-until` means exactly that. Only set it for something with a stated last
session, like a summer series.

**Reuse `:key` when you re-extract the same thing.** The key is how the importer
recognises an item it has seen before. `"handbell-rehearsal-fall-2026"` is a good
key because it stays the same if the time changes; `"handbell-2026-08-13"` is a
bad one because a corrected date makes it look like a different event.

## Worked example

```clojure
;; Example content drop — the shape Claude Desktop should produce.
;; Validated by test/com/mtzion/content/ingest_test.clj, so this file can never
;; illustrate something the schema would reject.
{:mtz/contract 1
 :contract-sha "example"
 :source {:kind :bulletin
          :files ["Bulletin 8-9-26.pdf" "Aug 9 Slides.pptx"]
          :extracted-on "2026-08-07"}
 :items
 [{:type :sermon
   :key "sermon-2026-08-09"
   :sermon-date "2026-08-09"
   :title "The Bread That Endures"
   :scripture-cw "Exodus 16:2-4, 9-15"
   :scripture-gospel "John 6:24-35"
   :series "bread-of-life-2026"
   :description "Pastor Jim continues the Bread of Life series."}

  {:type :event
   :key "handbell-rehearsal-fall-2026"
   :title "Handbell Choir Rehearsal"
   :starts-at "2026-08-13T18:30"
   :ends-at "2026-08-13T19:30"
   :all-day false
   :location "Sanctuary"
   :recurrence :weekly
   :recur-until "2026-12-17"
   :featured false
   :description [[:p "New ringers welcome — no experience necessary."]]}

  {:type :event
   :key "back-to-school-blessing-2026"
   :title "Back-to-School Blessing"
   :starts-at "2026-08-16T10:30"
   :all-day false
   :location "Sanctuary"
   :featured true
   :description [[:p "Students, teachers, and staff are invited forward during "
                  "worship for a blessing as the new school year begins. "
                  "Bring your backpack!"]]}

  {:type :post
   :key "council-notes-2026-08"
   :slug "council-notes-august-2026"
   :category :news
   :title "Notes from Consistory"
   :excerpt "Highlights from the August meeting: roof project, budget, and fall programming."
   :published-on "2026-08-09"
   :show-on-home false
   :body [[:p "Consistory met on August 4. Three items are worth sharing:"]
          [:ul
           [:li [:strong "Roof project."] " Bids are in; work begins in September."]
           [:li [:strong "Budget."] " Giving is tracking 4% ahead of last year."]
           [:li [:strong "Fall programming."] " Sunday School resumes September 7."]]
          [:p "The next meeting is " [:strong "September 1 at 7:00 PM"] "."]]}

  {:type :feature
   :key "home-fall-kickoff-2026"
   :page-slug :home
   :title "Fall Kickoff Sunday"
   :subtitle "September 7 · 10:30 AM"
   :body [[:p "One service, one table, one family. Lunch on the lawn follows worship."]]
   :cta-label "See all events"
   :cta-url "/events"
   :sort-order 10
   :show-on-home true}]}
```

---

contract-sha: `7c96e4de`
