(ns com.mtzion.content.doc
  "Generates content-inbox/CONTRACT.md from the live schemas.

  The contract is what Claude Desktop is told to follow, so it must not be able
  to drift from what the importer actually accepts. Everything factual here —
  field names, requiredness, value formats, the tag allowlist, the valid parent
  sections — is derived from `com.mtzion.content.schema`,
  `com.mtzion.content.hiccup` and `com.mtzion.model.nav`. Only the prose is
  hand-written.

  A test asserts the committed file equals this output, so editing a schema
  without regenerating fails the build."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [com.mtzion.content.hiccup :as ch]
            [com.mtzion.content.schema :as cs]
            [malli.core :as m]))

(def doc-path "content-inbox/CONTRACT.md")
(def example-path "content-inbox/examples/bulletin.edn")

;; ---------------------------------------------------------------------------
;; Describing schema forms in English
;; ---------------------------------------------------------------------------

(defn- size-note [{:keys [min max]}]
  (cond
    (and min max) (format ", %d–%d chars" min max)
    max           (format ", up to %d chars" max)
    min           (format ", at least %d chars" min)
    :else         ""))

(defn- describe
  "Malli form -> a phrase an author can act on."
  [form]
  (cond
    (= form :boolean)     "`true` or `false`"
    (= form :mtz/hiccup)  "hiccup — see [Body content](#body-content)"
    (= form :string)      "text"
    (keyword? form)       (str "`" (name form) "`")

    (vector? form)
    (let [[op & args] form
          props       (when (map? (first args)) (first args))
          args        (if props (rest args) args)]
      (case op
        :=          (str "always `" (pr-str (first args)) "`")
        :re         (or (:error/message props) "a specific format")
        :enum       (str "one of " (str/join ", " (map #(str "`" (pr-str %) "`") args)))
        :string     (str "text" (size-note props))
        :int        (let [{:keys [min max]} props]
                      (if (and min max)
                        (format "whole number %d–%d" min max)
                        "whole number"))
        :maybe      (describe (first args))
        :sequential (str "list of " (describe (first args)))
        :map        "map"
        (pr-str form)))

    :else (pr-str form)))

(defn- field-rows [schema]
  (for [[k props sch] (m/children (m/schema schema))]
    {:field    (str "`" k "`")
     :required (if (:optional props) "" "**yes**")
     :accepts  (describe (m/form sch))}))

(defn- md-table [rows]
  (str "| field | required | accepts |\n|---|---|---|\n"
       (str/join "\n" (for [{:keys [field required accepts]} rows]
                        (format "| %s | %s | %s |" field required accepts)))))

;; ---------------------------------------------------------------------------
;; Sections
;; ---------------------------------------------------------------------------

(def ^:private item-schemas
  [["event"   cs/Event   "Anything with a date and time."]
   ["post"    cs/Post    "News items and reflections. Appears under /news."]
   ["page"    cs/Page    "A standalone page, optionally filed under a menu section."]
   ["feature" cs/Feature "Editorial content in a named slot on an existing page."]
   ["sermon"  cs/Sermon  "One Sunday's service record."]])

(defn- items-section []
  (str/join "\n\n"
            (for [[name schema blurb] item-schemas]
              (str "### `:type :" name "`\n\n" blurb "\n\n" (md-table (field-rows schema))))))

(defn- hiccup-section []
  (str "Body fields (`:body`, `:description` on events) are **hiccup** — Clojure data,\n"
       "not an HTML string:\n\n"
       "```clojure\n"
       ":body [[:p \"A paragraph with \" [:strong \"bold\"] \" in it.\"]\n"
       "       [:ul [:li \"first\"] [:li \"second\"]]]\n"
       "```\n\n"
       "It is always a **vector of nodes**, even for a single paragraph.\n\n"
       "Only these tags are accepted. Anything else is rejected — this list matches\n"
       "what the site's editor can produce, so imported content stays editable.\n\n"
       "| tag | attributes allowed |\n|---|---|\n"
       (str/join "\n"
                 (for [[tag attrs] (sort-by key ch/allowed-tags)]
                   (format "| `%s` | %s |" tag
                           (if (seq attrs)
                             (str/join " " (map #(str "`" % "`") (sort attrs)))
                             "—"))))
       "\n\n`:class` is allowed on every tag. Links may use `https://`, `http://`, `/`,\n"
       "`#`, `mailto:` or `tel:`. Images must come from Cloudflare Images or a `/images/`\n"
       "path — a bare external URL is rejected.\n\n"
       "Not allowed, and worth knowing why:\n\n"
       "- `:h1` — the page already renders its own heading.\n"
       "- `:div`, `:span` — layout belongs to the site, not to content.\n"
       "- `:style` attributes, `:script`, `:iframe`, any `on*` handler.\n"
       "- Raw HTML of any kind, including `[:lambdaisland.hiccup/unsafe-html \"…\"]`."))

(defn- common-mistakes []
  (str "These are the failures that actually happen. Read them before writing anything.\n\n"
       "| don't | do |\n|---|---|\n"
       "| `:starts-at \"6:30 PM\"` | `:starts-at \"2026-08-13T18:30\"` |\n"
       "| `:starts-at #inst \"2026-08-13\"` | plain strings only — no `#inst`, no `#uuid` |\n"
       "| `:featured 1` | `:featured true` |\n"
       "| `:body \"<p>text</p>\"` | `:body [[:p \"text\"]]` |\n"
       "| `:published true` | omit it — everything imports as a draft |\n"
       "| `:image-id \"abc\"` | omit it — images are attached by hand afterwards |\n"
       "| twelve items for a weekly event | **one** item with `:recurrence :weekly` |\n\n"
       "**If you are unsure about a value, leave the field out.** A missing field is easy\n"
       "to add in the admin panel; a confidently wrong date is not.\n\n"
       "**Recurring events are one item.** A bulletin lists choir practice every week;\n"
       "that is a single `:event` with `:recurrence :weekly`, not one item per week.\n\n"
       "**Leave `:recur-until` out unless the bulletin gives an end date.** Ongoing\n"
       "activities — Tai Chi, choir, Scouts, pickleball — simply run, and an omitted\n"
       "`:recur-until` means exactly that. Only set it for something with a stated last\n"
       "session, like a summer series.\n\n"
       "**Reuse `:key` when you re-extract the same thing.** The key is how the importer\n"
       "recognises an item it has seen before. `\"handbell-rehearsal-fall-2026\"` is a good\n"
       "key because it stays the same if the time changes; `\"handbell-2026-08-13\"` is a\n"
       "bad one because a corrected date makes it look like a different event."))

;; ---------------------------------------------------------------------------
;; Assembly
;; ---------------------------------------------------------------------------

(defn- sha256-prefix [s]
  (->> (.digest (java.security.MessageDigest/getInstance "SHA-256")
                (.getBytes s "UTF-8"))
       (take 4)
       (map #(format "%02x" %))
       str/join))

(defn- body []
  (str
   "# Mount Zion content contract\n\n"
   "*Generated from the code — do not edit by hand. Run `clj -M:run content-doc`.*\n\n"
   "You are extracting content from a church bulletin or slide deck into EDN that a\n"
   "CLI tool will import. Output **one EDN map and nothing else** — no prose, no\n"
   "markdown fence, no commentary.\n\n"
   "## Ground rules\n\n"
   "- Every item needs a `:type` and a `:key`.\n"
   "- `:key` is a stable lowercase identifier you choose. Reuse it if you re-extract\n"
   "  the same item later — that is how corrections replace rather than duplicate.\n"
   "- **All dates are Mount Zion local time.** Dates are `\"YYYY-MM-DD\"`; dates with a\n"
   "  time are `\"YYYY-MM-DDTHH:MM\"` on a 24-hour clock. No timezone suffix.\n"
   "- Booleans are `true` / `false`, never `1` / `0` / `\"on\"`.\n"
   "- Omit a field entirely to mean \"not provided\".\n"
   "- Unrecognised keys are an error, not something quietly ignored.\n"
   "- Nothing you write is published. Everything imports as a draft for a human to\n"
   "  review, so `:published` is not part of this contract.\n\n"
   "## Envelope\n\n"
   "```clojure\n"
   "{:mtz/contract " cs/contract-version "\n"
   " :contract-sha \"<the sha at the bottom of this document>\"\n"
   " :source {:kind :bulletin\n"
   "          :files [\"Bulletin 8-9-26.pdf\"]\n"
   "          :extracted-on \"2026-08-07\"}\n"
   " :items [...]}\n"
   "```\n\n"
   "## Item types\n\n"
   (items-section) "\n\n"
   "Valid `:parent` values for a page: "
   (str/join ", " (map #(str "`:" % "`") (sort (map name (m/children (m/schema cs/ParentSlug))))))
   ".\n\n"
   "## Body content\n\n"
   (hiccup-section) "\n\n"
   "## Common mistakes\n\n"
   (common-mistakes) "\n\n"
   "## Worked example\n\n"
   "```clojure\n"
   (str/trim (slurp (io/file example-path))) "\n"
   "```\n"))

(defn render
  "The full CONTRACT.md text, ending with its own content hash."
  []
  (let [b (body)]
    (str b "\n---\n\ncontract-sha: `" (sha256-prefix b) "`\n")))

(defn current-sha
  "The sha the importer compares an incoming file's :contract-sha against."
  []
  (sha256-prefix (body)))

(defn write-doc
  "clj -M:run content-doc

  Regenerates content-inbox/CONTRACT.md from the schemas."
  [& _args]
  (io/make-parents doc-path)
  (spit doc-path (render))
  (println (str "Wrote " doc-path " (contract-sha " (current-sha) ")")))
