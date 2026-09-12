(ns com.mtzion.app.mcp-test
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.app.mcp :as mcp]
            [com.mtzion.content.plan :as plan]
            [com.mtzion.model.content :as content]
            [com.mtzion.model.normalize :as norm]
            [com.mtzion.test-util :refer [with-temp-ctx]]))

(def ^:private token "test-token-not-a-real-secret")

(defn- ctx+token [ctx] (assoc ctx :mtz/mcp-token token))

(defn- post
  "One JSON-RPC call over the real handler, with the body as a client sends it."
  ([ctx payload] (post ctx payload {"authorization" (str "Bearer " token)}))
  ([ctx payload headers]
   (let [resp (mcp/handler (assoc (ctx+token ctx)
                                  :headers headers
                                  :query-params {}
                                  :body (java.io.ByteArrayInputStream.
                                         (.getBytes (json/generate-string payload) "UTF-8"))))]
     (assoc resp :parsed (when (string? (:body resp))
                           (try (json/parse-string (:body resp) true)
                                (catch Exception _ nil)))))))

(defn- call-tool [ctx tool args]
  (-> (post ctx {:jsonrpc "2.0" :id 1 :method "tools/call"
                 :params {:name tool :arguments args}})
      :parsed :result))

(defn- tool-text [ctx tool args]
  (-> (call-tool ctx tool args) :content first :text))

;; ---------------------------------------------------------------------------
;; Auth — the endpoint guards a writer, so this is the part that must not slip
;; ---------------------------------------------------------------------------

(deftest no-token-configured-means-off-not-open
  (with-temp-ctx [ctx]
    (testing "an unset MCP_TOKEN refuses rather than serving everything"
      (let [resp (mcp/handler (assoc ctx :headers {} :query-params {}
                                     :body (java.io.ByteArrayInputStream. (.getBytes "{}" "UTF-8"))))]
        (is (= 503 (:status resp)))
        (is (str/includes? (:body resp) "not configured"))))
    (testing "and a blank one counts as unset"
      (is (nil? (mcp/configured-token (assoc ctx :mtz/mcp-token "   ")))))))

(deftest a-wrong-or-missing-token-is-refused
  (with-temp-ctx [ctx]
    (doseq [[label headers] [["no header"    {}]
                             ["wrong token"  {"authorization" "Bearer nope"}]
                             ["empty bearer" {"authorization" "Bearer "}]
                             ["not bearer"   {"authorization" token}]
                             ["prefix only"  {"authorization" (str "Bearer " (subs token 0 8))}]]]
      (testing label
        (is (= 401 (:status (post ctx {:jsonrpc "2.0" :id 1 :method "tools/list"} headers))))))))

(deftest the-right-token-is-accepted-by-either-route
  (with-temp-ctx [ctx]
    (testing "bearer header"
      (is (= 200 (:status (post ctx {:jsonrpc "2.0" :id 1 :method "ping"})))))
    (testing "?key= query parameter, for clients that only accept a URL"
      (let [resp (mcp/handler (assoc (ctx+token ctx)
                                     :headers {} :query-params {"key" token}
                                     :body (java.io.ByteArrayInputStream.
                                            (.getBytes (json/generate-string
                                                        {:jsonrpc "2.0" :id 1 :method "ping"}) "UTF-8"))))]
        (is (= 200 (:status resp)))))))

;; ---------------------------------------------------------------------------
;; Protocol
;; ---------------------------------------------------------------------------

(deftest reads-the-body-muuntaja-already-parsed
  ;; api-defaults parses JSON into :body-params and consumes the stream. Reading
  ;; :body there gave every request a nil method — which is what the live
  ;; endpoint did on its first deploy.
  (with-temp-ctx [ctx]
    (let [resp (mcp/handler (assoc (ctx+token ctx)
                                   :headers {"authorization" (str "Bearer " token)}
                                   :query-params {}
                                   :body-params {:jsonrpc "2.0" :id 7 :method "ping"}
                                   :body (java.io.ByteArrayInputStream. (.getBytes "" "UTF-8"))))]
      (is (= 200 (:status resp)))
      (is (= 7 (:id (json/parse-string (:body resp) true)))))))

(deftest speaks-json-rpc
  (with-temp-ctx [ctx]
    (testing "initialize advertises tools"
      (let [r (:parsed (post ctx {:jsonrpc "2.0" :id 1 :method "initialize"}))]
        (is (= mcp/protocol-version (get-in r [:result :protocolVersion])))
        (is (contains? (get-in r [:result :capabilities]) :tools))))

    (testing "tools/list names every tool with a schema"
      (let [ts (get-in (:parsed (post ctx {:jsonrpc "2.0" :id 1 :method "tools/list"}))
                       [:result :tools])]
        (is (= #{"contract" "current_calendar" "current_posts"
                 "plan_changes" "stage_changes" "archive_item"}
               (set (map :name ts))))
        (is (every? #(and (seq (:description %)) (:inputSchema %)) ts))))

    (testing "a notification gets no response body"
      (is (= 202 (:status (post ctx {:jsonrpc "2.0" :method "notifications/initialized"})))))

    (testing "an unknown method is a protocol error, not a crash"
      (is (= -32601 (get-in (:parsed (post ctx {:jsonrpc "2.0" :id 1 :method "nope"}))
                            [:error :code]))))

    (testing "an unknown tool is refused"
      (is (= -32602 (get-in (:parsed (post ctx {:jsonrpc "2.0" :id 1 :method "tools/call"
                                                :params {:name "rm_rf" :arguments {}}}))
                            [:error :code]))))

    (testing "malformed JSON does not 500"
      (let [resp (mcp/handler (assoc (ctx+token ctx)
                                     :headers {"authorization" (str "Bearer " token)}
                                     :query-params {}
                                     :body (java.io.ByteArrayInputStream. (.getBytes "{not json" "UTF-8"))))]
        (is (= 400 (:status resp)))))))

;; ---------------------------------------------------------------------------
;; The reason the server exists: the agent can see the keys
;; ---------------------------------------------------------------------------

(defn- seed-activity! [ctx k title start]
  (biff.sqlite/execute
   ctx {:insert-into :event
        :values [(merge (content/defaults :event)
                        {:id (str (random-uuid)) :import_key k :title title
                         :kind "activity" :recurrence "weekly"
                         :start_at (norm/local-datetime->epoch start)
                         :created_at (norm/now-epoch)})]}))

(deftest current-calendar-hands-over-the-keys
  (with-temp-ctx [ctx]
    (seed-activity! ctx "boy-scouts-monday" "Boy Scouts" "2026-09-14T18:30")
    (let [text (tool-text ctx "current_calendar" {})]
      (testing "the existing key is shown, which is the whole point"
        (is (str/includes? text "boy-scouts-monday")))
      (testing "with a recurrence a person can read"
        (is (str/includes? text "Monday")))
      (testing "and the instruction that stops a duplicate being created"
        (is (str/includes? text "creates a SECOND row"))))))

(deftest a-draft-is-still-visible-to-the-agent
  ;; An agent that cannot see a draft proposes creating it again.
  (with-temp-ctx [ctx]
    (seed-activity! ctx "tai-chi-monday" "Tai Chi" "2026-09-14T15:00")
    (is (str/includes? (tool-text ctx "current_calendar" {}) "tai-chi-monday"))))

;; ---------------------------------------------------------------------------
;; Planning and staging
;; ---------------------------------------------------------------------------

(def ^:private one-event
  (pr-str {:mtz/contract 1
           :items [{:type :event :kind :activity :key "boy-scouts-monday"
                    :title "Boy Scouts" :starts-at "2026-09-14T18:30"
                    :recurrence :weekly}]}))

(deftest plan-changes-writes-nothing
  (with-temp-ctx [ctx]
    (seed-activity! ctx "boy-scouts-monday" "Boy Scouts" "2026-08-10T18:30")
    (let [before (biff.sqlite/execute ctx {:select [[[:count :*] :n]] :from :event})
          text   (tool-text ctx "plan_changes" {:edn one-event})]
      (testing "it reports an update against the existing row"
        (is (str/includes? text "UPDATE"))
        (is (str/includes? text "boy-scouts-monday")))
      (testing "and says so plainly"
        (is (str/includes? text "DRY RUN")))
      (testing "and really did not write"
        (is (= before (biff.sqlite/execute ctx {:select [[[:count :*] :n]] :from :event})))))))

(deftest a-wrong-key-shows-up-as-a-create
  ;; The failure this server exists to prevent, made visible before it lands.
  (with-temp-ctx [ctx]
    (seed-activity! ctx "boy-scouts-monday" "Boy Scouts" "2026-08-10T18:30")
    (let [text (tool-text ctx "plan_changes"
                          {:edn (pr-str {:mtz/contract 1
                                         :items [{:type :event :key "boyscouts"
                                                  :title "Boy Scouts"
                                                  :starts-at "2026-09-14T18:30"}]})})]
      (is (str/includes? text "CREATE"))
      (is (str/includes? text "boyscouts")))))

(deftest stage-changes-stages-but-does-not-publish
  (with-temp-ctx [ctx]
    (let [text (tool-text ctx "stage_changes" {:edn one-event :source-ref "9-13-26.pdf"})]
      (is (str/includes? text "STAGED"))
      (testing "one row is waiting for a human"
        (is (= 1 (:n (first (norm/snake-keys-all
                             (biff.sqlite/execute ctx {:select [[[:count :*] :n]]
                                                       :from :inbox_item})))))))
      (testing "and nothing reached the event table"
        (is (zero? (:n (first (norm/snake-keys-all
                               (biff.sqlite/execute ctx {:select [[[:count :*] :n]]
                                                         :from :event}))))))))))

(deftest invalid-edn-is-reported-not-applied
  (with-temp-ctx [ctx]
    (testing "a tagged literal is refused with an explanation"
      (let [text (tool-text ctx "stage_changes"
                            {:edn "{:mtz/contract 1 :items [{:type :event :key \"x\" :title \"X\" :starts-at #inst \"2026-09-14\"}]}"})]
        (is (str/includes? text "COULD NOT PARSE"))))
    (testing "a schema violation reports the field, and writes nothing"
      (let [text (tool-text ctx "stage_changes"
                            {:edn (pr-str {:mtz/contract 1
                                           :items [{:type :event :key "x" :title "X"
                                                    :starts-at "6:30 PM"}]})})]
        (is (str/includes? text "VALIDATION FAILED"))
        (is (zero? (:n (first (norm/snake-keys-all
                               (biff.sqlite/execute ctx {:select [[[:count :*] :n]]
                                                         :from :inbox_item}))))))))))

;; ---------------------------------------------------------------------------
;; Archiving — the one tool that changes the live site
;; ---------------------------------------------------------------------------

(deftest archive-item-archives-and-is-reversible
  (with-temp-ctx [ctx]
    (seed-activity! ctx "fall-hayride-2026" "Let's Go on a Hayride" "2026-10-04T17:00")
    (let [id (:id (first (norm/snake-keys-all
                          (biff.sqlite/execute ctx {:select :* :from :event}))))]
      (content/publish! ctx :event id)
      (let [text (tool-text ctx "archive_item" {:type "event" :key "fall-hayride-2026"})]
        (is (str/includes? text "Archived"))
        (is (str/includes? text "restored"))
        (is (= "archived" (:status (first (norm/snake-keys-all
                                           (biff.sqlite/execute ctx {:select :* :from :event}))))))
        (testing "the row is still there — archive, not delete"
          (is (= 1 (:n (first (norm/snake-keys-all
                               (biff.sqlite/execute ctx {:select [[[:count :*] :n]] :from :event})))))))))))

(deftest archive-item-refuses-what-it-cannot-address
  (with-temp-ctx [ctx]
    (testing "an unknown key changes nothing and says which tool to call"
      (let [text (tool-text ctx "archive_item" {:type "event" :key "no-such-thing"})]
        (is (str/includes? text "No event"))
        (is (str/includes? text "current_calendar"))))
    (testing "an unknown type is refused"
      (is (str/includes? (tool-text ctx "archive_item" {:type "user" :key "x"})
                         "Unknown type")))
    (testing "a blank key cannot sweep"
      (is (str/includes? (tool-text ctx "archive_item" {:type "event" :key ""})
                         "exact import key")))))

;; ---------------------------------------------------------------------------
;; The contract travels with the server, so it cannot go stale
;; ---------------------------------------------------------------------------

(deftest contract-tool-returns-the-committed-contract
  (with-temp-ctx [ctx]
    (let [text (tool-text ctx "contract" {})]
      (is (str/includes? text "Mount Zion content contract"))
      (testing "including the field that was missing when a copy went stale"
        (is (str/includes? text ":kind"))))))
