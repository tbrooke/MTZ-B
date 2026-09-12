(ns com.mtzion.content.read
  "Reading an envelope, and reporting why one was refused.

  Split out of `content.ingest` because the CLI task reaches for
  `com.mtzion.system` to stand a headless app up, and `system` loads the module
  list — so anything a module requires cannot also require `ingest` without a
  cycle. The MCP endpoint is a module and needs exactly these two things.

  Pure: no ctx, no database, no system."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]))

(def inbox-dir "content-inbox")
(def applied-dir "content-inbox/applied")

;; ---------------------------------------------------------------------------
;; Reading
;; ---------------------------------------------------------------------------

(defn- reject-tag [tag]
  (fn [_]
    (throw (ex-info (str "unsupported tagged literal #" tag
                         " — the contract is plain EDN. Write dates as plain "
                         "strings, e.g. \"2026-08-16T10:30\".")
                    {:tag tag}))))

(def ^:private edn-opts
  ;; :default only fires for UNKNOWN tags — #inst and #uuid are built into
  ;; clojure.edn and would otherwise parse into Date/UUID objects that then fail
  ;; validation with a confusing type error. Override them so the reader reports
  ;; the real problem.
  {:readers {'inst (reject-tag "inst") 'uuid (reject-tag "uuid")}
   :default (fn [tag _] ((reject-tag tag) nil))})

(defn read-envelope-string
  "Parses one envelope out of a string. Tagged literals are refused outright —
  the contract is plain data, and a reader tag is either a mistake or an attempt
  to construct something the schema was not written to reason about."
  [s]
  (try
    {:ok? true :envelope (edn/read-string edn-opts s)}
    (catch Exception e
      {:ok? false :error (.getMessage e)})))

(defn read-envelope
  "Reads one EDN file. See read-envelope-string."
  [file]
  (try
    (read-envelope-string (slurp file))
    (catch Exception e
      {:ok? false :error (.getMessage e)})))

;; ---------------------------------------------------------------------------
;; Error reporting — formatted to be pasted back into Claude Desktop
;; ---------------------------------------------------------------------------

(defn- render-shape-errors [shape]
  (when (seq shape)
    (str/join "\n"
              (for [[i item-errs] (map-indexed vector (:items shape))
                    :when (seq item-errs)
                    [field msgs] item-errs]
                (format "  item %d  [%s]\n      %s"
                        i (name field) (str/join "\n      " (map str msgs)))))))

(defn- render-hiccup-errors [hiccup-errs]
  (when (seq hiccup-errs)
    (str/join "\n"
              (for [h hiccup-errs]
                (str (format "  item %d (%s)  [%s %s]\n      %s%s"
                             (:index h) (:key h) (name (:field h)) (pr-str (:path h))
                             (name (:error h))
                             (if (contains? h :got) (str "   " (pr-str (:got h))) ""))
                     (when (:allowed h)
                       (str "\n      allowed: " (str/join " " (map pr-str (:allowed h))))))))))

(defn- render-simple-errors [label errs]
  (when (seq errs)
    (str/join "\n" (for [e errs]
                     (format "  %s (%s)\n      %s" label (:key e) (:message e))))))

(defn render-errors [file {:keys [shape hiccup cross-field duplicates]}]
  (->> [(format "VALIDATION FAILED — %s" file)
        "Nothing was written. Fix these and re-drop the file.\n"
        (render-shape-errors shape)
        (render-hiccup-errors hiccup)
        (render-simple-errors "item" cross-field)
        (render-simple-errors "duplicate key" duplicates)]
       (remove str/blank?)
       (str/join "\n")))
