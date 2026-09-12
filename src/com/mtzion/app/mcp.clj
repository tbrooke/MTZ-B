(ns com.mtzion.app.mcp
  "An MCP endpoint over the church's own content.

  The weekly bulletin is not an extraction job, it is a diff: seven activities
  that have not changed since March, plus two new things and one cancellation.
  Treating it as an extraction is what produced twenty-two cards to review for
  six real changes, and what let an agent invent `boyscouts` for a row the
  database already held as `boy-scouts-monday` — a duplicate nobody would have
  caught without reading the dry run.

  The fix is to let the agent look first. `current_calendar` hands over the
  existing keys, so adoption is the default rather than a lucky guess, and
  `plan_changes` answers \"what would this do\" against the live rows before
  anything is written.

  Everything here is a thin wrapper over code the CLI importer already uses —
  `content.plan` classifies, `content.inbox` stages, `content.schema` validates.
  A second implementation of any of those would be a second set of rules to
  keep in step, and the console and the importer already share these.

  Registered under :biff.ring/api-routes, not :biff.ring/routes: site defaults
  wrap every POST in anti-forgery, which an external client cannot satisfy and
  should not have to. api-defaults carries no session and no CSRF."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.content.inbox :as inbox]
            [com.mtzion.content.read :as cread]
            [com.mtzion.content.plan :as plan]
            [com.mtzion.content.schema :as cs]
            [com.mtzion.model.content :as content]
            [com.mtzion.model.event :as event]
            [com.mtzion.model.normalize :as norm]))

(def protocol-version "2025-06-18")

;; ---------------------------------------------------------------------------
;; Auth
;; ---------------------------------------------------------------------------

(defn configured-token
  "The shared secret, or nil when none is set. #biff/secret yields a thunk."
  [ctx]
  (let [t (:mtz/mcp-token ctx)]
    (not-empty (str/trim (str (if (fn? t) (t) t))))))

(defn- presented-token
  "Bearer header first, `?key=` second. The query parameter is there because
  some clients take a URL and nothing else; it is the weaker of the two (URLs
  reach logs and history) so prefer the header where the client allows it."
  [{:keys [headers query-params]}]
  (or (some-> (get headers "authorization")
              (->> (re-find #"(?i)^\s*bearer\s+(.+)$"))
              second str/trim not-empty)
      (not-empty (str/trim (str (get query-params "key"))))))

(defn- token-ok?
  "Constant-time compare. A byte-by-byte early return leaks the prefix length
  to anyone willing to time enough requests."
  [expected presented]
  (boolean
   (and expected presented
        (java.security.MessageDigest/isEqual
         (.getBytes ^String expected "UTF-8")
         (.getBytes ^String presented "UTF-8")))))

;; ---------------------------------------------------------------------------
;; Reading the database
;; ---------------------------------------------------------------------------

(defn- exec [ctx honey]
  (norm/snake-keys-all (biff.sqlite/execute ctx honey)))

(defn- when-words
  "How an editor would say it: a recurrence in words, or a date and time."
  [{:keys [start_at recurrence] :as ev}]
  (if (and recurrence (not= recurrence "none"))
    (str (event/describe ev)
         (when start_at
           (str ", " (-> (norm/epoch->local-datetime-str start_at)
                         (str/split #"T") second))))
    (some-> start_at norm/epoch->local-datetime-str (str/replace "T" " "))))

(defn- event-line [{:keys [import_key title status] :as ev}]
  (format "  %-34s %-10s %-38s %s"
          (or import_key "(no key — created by hand)")
          (or status "?")
          (or (when-words ev) "")
          (or title "")))

(defn current-calendar
  "Standing activities and the one-off events ahead, with the keys an agent has
  to reuse. Drafts and archived rows are included deliberately: an agent that
  cannot see a draft will propose creating it again."
  [ctx {:keys [days] :or {days 60}}]
  (let [now  (norm/now-epoch)
        acts (exec ctx {:select [:event/import-key :event/title :event/status
                                 :event/start-at :event/recurrence :event/recur-until]
                        :from :event
                        :where [:= :event/kind "activity"]
                        :order-by [[:event/title :asc]]})
        evs  (exec ctx {:select [:event/import-key :event/title :event/status
                                 :event/start-at :event/recurrence :event/recur-until]
                        :from :event
                        :where [:and [:= :event/kind "event"]
                                [:>= :event/start-at now]
                                [:<= :event/start-at (+ now (* days 86400))]]
                        :order-by [[:event/start-at :asc]]})]
    (str "STANDING ACTIVITIES — kind :activity. These recur; they are already "
         "on the site and almost never change.\n"
         "Reuse these keys. A new key for one of these creates a SECOND row.\n\n"
         (format "  %-34s %-10s %-38s %s" "key" "status" "when" "title") "\n"
         (if (seq acts)
           (str/join "\n" (map event-line acts))
           "  (none)")
         "\n\nONE-OFF EVENTS in the next " days " days — kind :event.\n\n"
         (if (seq evs)
           (str/join "\n" (map event-line evs))
           "  (none)")
         "\n\nAnything in the bulletin that is NOT listed above is new. Anything "
         "listed above\nthat the bulletin no longer mentions is a candidate for "
         "archive_item — but say so\nrather than assuming; a bulletin omits "
         "plenty that is still happening.")))

(defn current-posts
  "Recent posts, newest first, with keys."
  [ctx {:keys [limit] :or {limit 25}}]
  (let [rows (exec ctx {:select [:post/import-key :post/title :post/slug
                                 :post/category :post/status :post/published-at]
                        :from :post
                        :order-by [[:post/created-at :desc]]
                        :limit limit})]
    (str "POSTS — most recent " limit ", newest first.\n\n"
         (format "  %-34s %-10s %-12s %s" "key" "status" "category" "title") "\n"
         (if (seq rows)
           (str/join "\n"
                     (for [{:keys [import_key title status category]} rows]
                       (format "  %-34s %-10s %-12s %s"
                               (or import_key "(no key — created by hand)")
                               (or status "?") (or category "") (or title ""))))
           "  (none)"))))

(defn contract-text
  "The contract itself, so a client never depends on an uploaded copy. A stale
  copy in a project's knowledge is what sent a whole bulletin through the old
  schema, describing every weekly class as a one-off."
  []
  (let [f (io/file cread/inbox-dir "CONTRACT.md")]
    (if (.exists f)
      (slurp f)
      (or (some-> (io/resource "CONTRACT.md") slurp)
          (str "CONTRACT.md is not present on the server. It is generated by "
               "`clj -M:run content-doc` and committed at content-inbox/CONTRACT.md.")))))

;; ---------------------------------------------------------------------------
;; Writing
;; ---------------------------------------------------------------------------

(defn- validated
  "Parse + validate an envelope given as a string. Returns {:ok? :items} or
  {:ok? false :report ...} carrying the same report the CLI prints."
  [edn-string]
  (let [{:keys [ok? envelope error]} (cread/read-envelope-string edn-string)]
    (if-not ok?
      {:ok? false :report (str "COULD NOT PARSE THE EDN\n\n  " error)}
      (let [{:keys [ok? errors]} (cs/validate envelope)]
        (if ok?
          {:ok? true :items (:items envelope) :envelope envelope}
          {:ok? false :report (cread/render-errors "(supplied over MCP)" errors)})))))

(defn plan-changes
  "Dry run. Classifies every item against the live rows and writes nothing."
  [ctx {:keys [edn]}]
  (let [{:keys [ok? items report]} (validated edn)]
    (if-not ok?
      report
      (let [ops (plan/build ctx items "mcp" nil)]
        (str (plan/render-diff ops)
             "\n\n  DRY RUN — nothing was written."
             "\n  UPDATE never changes publish state; CREATE always lands as a draft."
             "\n  Call stage_changes with the same EDN to put these in the console inbox.")))))

(defn stage-changes
  "Puts the items in the console inbox, where a human accepts or dismisses each
  one. Staging still creates no content row — accepting does, as a draft."
  [ctx {:keys [edn source-ref]}]
  (let [{:keys [ok? items report]} (validated edn)]
    (if-not ok?
      report
      (let [ops   (plan/build ctx items "mcp" nil)
            batch (inbox/stage! ctx items {:source "bulletin"
                                           :source-ref (or source-ref "via MCP")})]
        (str (plan/render-diff ops)
             "\n\n  STAGED — " (count items) " items are waiting in the console inbox"
             "\n  (batch " batch ")."
             "\n  Nothing is on the site yet. Review at /console/inbox; accepting"
             "\n  an item creates it as a draft, and publishing is a separate step.")))))

(def ^:private archivable
  {"event" :event "post" :post "page" :page "feature" :feature "sermon" :sermon})

(defn archive-item
  "Takes one row off the site. Archive, never delete — it stays in
  /console/archive and can be restored, which is the only reason it is safe to
  expose this at all. Addressed by exact key so it cannot sweep."
  [ctx {:keys [type key]}]
  (let [t (get archivable (some-> type name str/lower-case))]
    (cond
      (nil? t)
      (str "Unknown type " (pr-str type) ". One of: "
           (str/join ", " (sort (keys archivable))) ".")

      (str/blank? (str key))
      "Give the exact import key of the row to archive — see current_calendar."

      :else
      (let [row (first (exec ctx {:select :* :from t
                                  :where [:= (keyword (name t) "import-key") key]}))]
        (cond
          (nil? row)
          (str "No " (name t) " has key " (pr-str key)
               ". Call current_calendar or current_posts and use a key from there.")

          (= "archived" (:status row))
          (str (name t) " " (pr-str key) " is already archived. Nothing to do.")

          :else
          (do (content/archive! ctx t (:id row))
              (str "Archived " (name t) " " (pr-str key) " — " (pr-str (:title row)) "."
                   "\nIt is off the site and listed in /console/archive, where it can"
                   "\nbe restored. Nothing was deleted.")))))))

;; ---------------------------------------------------------------------------
;; Tool registry
;; ---------------------------------------------------------------------------

(def tools
  [{:name "contract"
    :description
    (str "The content contract: the exact EDN shape this site accepts, the "
         "allowed hiccup tags, and the mistakes that actually happen. Call this "
         "FIRST, before writing any EDN. It is generated from the code, so it "
         "is never out of date — do not rely on an uploaded copy.")
    :inputSchema {:type "object" :properties {} :additionalProperties false}
    :handler (fn [_ctx _args] (contract-text))}

   {:name "current_calendar"
    :description
    (str "What is already on the calendar: the standing weekly activities and "
         "the one-off events coming up, each with the import key that "
         "identifies it. Call this BEFORE proposing any change. Reuse the keys "
         "shown here for anything that already exists — a new key for an "
         "existing thing creates a duplicate row rather than updating it.")
    :inputSchema {:type "object"
                  :properties {:days {:type "integer"
                                      :description "How far ahead to list one-off events. Default 60."}}
                  :additionalProperties false}
    :handler (fn [ctx args] (current-calendar ctx args))}

   {:name "current_posts"
    :description
    (str "Recent news posts and reflections with their import keys. Same rule "
         "as current_calendar: reuse a key to revise an existing post.")
    :inputSchema {:type "object"
                  :properties {:limit {:type "integer"
                                       :description "How many posts. Default 25."}}
                  :additionalProperties false}
    :handler (fn [ctx args] (current-posts ctx args))}

   {:name "plan_changes"
    :description
    (str "Dry run. Give it one EDN envelope and it reports exactly what would "
         "happen to each item against the live database — create, update (with "
         "the fields that would change) or unchanged — and writes nothing. Use "
         "this to check your work before staging. An item reported as CREATE "
         "that you expected to be an UPDATE means the key is wrong.")
    :inputSchema {:type "object"
                  :properties {:edn {:type "string"
                                     :description "One EDN envelope, exactly as the contract describes."}}
                  :required ["edn"]
                  :additionalProperties false}
    :handler (fn [ctx args] (plan-changes ctx args))}

   {:name "stage_changes"
    :description
    (str "Puts the items in the console inbox for a person to review. This "
         "still puts nothing on the site: accepting a card creates a draft, "
         "and publishing is a further step a human takes. Run plan_changes "
         "first and make sure the diff is what you meant.")
    :inputSchema {:type "object"
                  :properties {:edn {:type "string"
                                     :description "One EDN envelope, exactly as the contract describes."}
                               :source-ref {:type "string"
                                            :description "Where this came from, e.g. the bulletin filename."}}
                  :required ["edn"]
                  :additionalProperties false}
    :handler (fn [ctx args] (stage-changes ctx args))}

   {:name "archive_item"
    :description
    (str "Takes one row off the site, addressed by its exact import key. This "
         "archives rather than deletes — the row stays in /console/archive and "
         "can be restored. Use it when something is cancelled or finished. If "
         "the bulletin simply does not mention something this week, that is "
         "not a reason to archive it; ask first.")
    :inputSchema {:type "object"
                  :properties {:type {:type "string"
                                      :enum ["event" "post" "page" "feature" "sermon"]}
                               :key {:type "string"
                                     :description "The exact import key, from current_calendar or current_posts."}}
                  :required ["type" "key"]
                  :additionalProperties false}
    :handler (fn [ctx args] (archive-item ctx args))}])

(def ^:private by-name (into {} (map (juxt :name identity)) tools))

(defn- tool-descriptor [t] (select-keys t [:name :description :inputSchema]))

;; ---------------------------------------------------------------------------
;; JSON-RPC
;; ---------------------------------------------------------------------------

(defn- result [id v] {:jsonrpc "2.0" :id id :result v})

(defn- rpc-error [id code message]
  {:jsonrpc "2.0" :id id :error {:code code :message message}})

(defn- text-result [s] {:content [{:type "text" :text (str s)}]})

(defn- call-tool [ctx id {:keys [name arguments]}]
  (if-let [t (get by-name name)]
    (try
      (result id (text-result ((:handler t) ctx (or arguments {}))))
      (catch Exception e
        ;; Reported as a tool result, not a protocol error: the model can read
        ;; it and correct itself, which is the whole point of a dry run.
        (result id (assoc (text-result (str "The tool threw: " (.getMessage e)))
                          :isError true))))
    (rpc-error id -32602 (str "Unknown tool " (pr-str name)))))

(defn handle-rpc [ctx {:keys [id method params]}]
  (case method
    "initialize"
    (result id {:protocolVersion protocol-version
                :capabilities {:tools {}}
                :serverInfo {:name "mtzion-content" :version "1"}})

    "ping" (result id {})

    "tools/list" (result id {:tools (mapv tool-descriptor tools)})

    "tools/call" (call-tool ctx id params)

    (if (str/starts-with? (str method) "notifications/")
      ::notification
      (rpc-error id -32601 (str "Unknown method " (pr-str method))))))

;; ---------------------------------------------------------------------------
;; HTTP
;; ---------------------------------------------------------------------------

(defn- json-response [status body]
  {:status status
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string body)})

(defn handler [{:keys [body] :as ctx}]
  (let [expected (configured-token ctx)]
    (cond
      ;; No token configured means the endpoint is off, not open. An MCP server
      ;; that answers without one would expose the whole calendar and a writer.
      (nil? expected)
      (json-response 503 {:error "MCP is not configured on this server (MCP_TOKEN is unset)."})

      (not (token-ok? expected (presented-token ctx)))
      {:status 401
       :headers {"Content-Type" "application/json"
                 "WWW-Authenticate" "Bearer"}
       :body (json/generate-string {:error "Unauthorized."})}

      :else
      (let [payload (try (json/parse-string (slurp body) true)
                         (catch Exception _ ::bad-json))]
        (cond
          (= ::bad-json payload)
          (json-response 400 (rpc-error nil -32700 "Parse error"))

          (sequential? payload)
          (let [rs (remove #(= ::notification %) (map #(handle-rpc ctx %) payload))]
            (if (seq rs) (json-response 200 (vec rs)) {:status 202 :body ""}))

          :else
          (let [r (handle-rpc ctx payload)]
            (if (= ::notification r)
              {:status 202 :body ""}
              (json-response 200 r))))))))

(defn info
  "A GET tells a person poking at the URL what this is, without revealing
  anything. MCP itself is POST-only here — no SSE stream is offered."
  [_ctx]
  (json-response 200 {:name "mtzion-content"
                      :protocol "mcp"
                      :protocolVersion protocol-version
                      :transport "streamable-http (POST only)"}))

(def module
  {:biff.ring/api-routes
   [["/mcp" {:post handler :get info :name ::mcp}]]})
