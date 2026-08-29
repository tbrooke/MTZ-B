(ns com.mtzion.content.ingest
  "The `import` CLI task: read EDN from content-inbox/, validate it, show a diff,
  and — only with --apply — stage it for review.

  --apply no longer writes content rows. It puts the items in the console's
  inbox, where each one is looked at and accepted individually. Everything the
  importer guaranteed still holds, because the same planning and applying code
  runs — just later, from a button, one item at a time.

  Dry run is still the default. Staging is all-or-nothing per file: any
  validation error means zero writes, and any SQL error rolls the whole file
  back."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [com.mtzion.content.doc :as doc]
            [com.mtzion.content.inbox :as inbox]
            [com.mtzion.content.plan :as plan]
            [com.mtzion.content.schema :as cs]
            [com.mtzion.model.normalize :as norm]
            [com.mtzion.system :as system]))

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

(defn read-envelope
  "Reads one EDN file. Tagged literals are refused outright — the contract is
  plain data, and a reader tag is either a mistake or an attempt to construct
  something the schema was not written to reason about."
  [file]
  (try
    {:ok? true :envelope (edn/read-string edn-opts (slurp file))}
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

;; ---------------------------------------------------------------------------
;; Applying
;; ---------------------------------------------------------------------------

(defn- archive! [file receipt]
  (.mkdirs (io/file applied-dir))
  (let [name    (.getName (io/file file))
        dest    (io/file applied-dir name)
        receipt-file (io/file applied-dir (str/replace name #"\.edn$" ".receipt.edn"))]
    (spit receipt-file (with-out-str (pp/pprint receipt)))
    (io/copy (io/file file) dest)
    (.delete (io/file file))
    (.getPath dest)))

;; ---------------------------------------------------------------------------
;; Per-file processing
;; ---------------------------------------------------------------------------

(defn process-file
  "Returns {:status 0|1|2 :output string}."
  [ctx file {:keys [apply?]}]
  (let [{:keys [ok? envelope error]} (read-envelope file)]
    (if-not ok?
      {:status 1 :output (format "UNREADABLE EDN — %s\n  %s" file error)}
      (let [result (cs/validate envelope)]
        (if-not (:ok? result)
          {:status 1 :output (render-errors file (:errors result))}
          (let [items  (:items envelope)
                source (or (some-> envelope :source :files first) (.getName (io/file file)))
                drift  (let [claimed (:contract-sha envelope)
                             current (try (doc/current-sha) (catch Exception _ nil))]
                         ;; A warning, not an error: an older-but-valid file should
                         ;; still import. It only means the doc Desktop was working
                         ;; from is out of date.
                         (when (and claimed current
                                    (not= claimed "example")
                                    (not= claimed current))
                           (format (str "  ⚠ contract drift: this file was produced against %s, "
                                        "current is %s\n    Re-attach CONTRACT.md in the Claude "
                                        "Desktop project.\n")
                                   claimed current)))
                opts   (when-let [r2 (:r2/public-url ctx)]
                         {:image-hosts (conj com.mtzion.content.hiccup/default-image-hosts
                                             (str r2 "/"))})
                ops    (plan/build ctx items source opts)
                header (str (format "%s   contract %d ✓   %d item%s\n"
                                    file (:mtz/contract envelope)
                                    (count items) (if (= 1 (count items)) "" "s"))
                            drift)
                diff   (plan/render-diff ops)]
            (if-not apply?
              {:status 0
               :output (str header "\n" diff
                            "\n\n  DRY RUN — nothing was written."
                            "\n  Re-run with --apply to put these in the inbox.")}
              (try
                (let [batch   (inbox/stage! ctx items {:source "bulletin"
                                                       :source-ref source})
                      receipt {:staged-at (norm/now-epoch)
                               :batch     batch
                               :items     (vec (for [i items]
                                                 {:type (:type i) :key (:key i)}))}
                      dest    (archive! file receipt)]
                  {:status 0
                   :output (str header "\n" diff
                                (format "\n\n  STAGED. %d item%s waiting in the console inbox."
                                        (count items) (if (= 1 (count items)) "" "s"))
                                (format "\n  Input archived to %s" dest)
                                "\n  Nothing has been written to the site yet — review them at"
                                "\n  /console/inbox, where accepting an item creates it as a draft.")})
                (catch Exception e
                  {:status 2
                   :output (str header "\n" diff
                                "\n\n  FAILED — the whole file was rolled back, nothing was staged."
                                "\n  " (.getMessage e))})))))))))

;; ---------------------------------------------------------------------------
;; Task entry point
;; ---------------------------------------------------------------------------

(defn- edn-files [args]
  (let [explicit (remove #(str/starts-with? % "-") args)]
    (if (seq explicit)
      (map io/file explicit)
      (->> (io/file inbox-dir) .listFiles
           (filter #(and (.isFile %) (str/ends-with? (.getName %) ".edn")))
           (sort-by #(.getName %))))))

(defn run
  "Testable core: returns {:status n :output s} for the given files."
  [ctx files opts]
  (if (empty? files)
    {:status 0 :output (format "No .edn files in %s/ — nothing to import." inbox-dir)}
    (let [results (mapv #(process-file ctx % opts) files)]
      {:status (apply max (map :status results))
       :output (str/join "\n\n" (map :output results))})))

(defn import-task
  "clj -M:run import [file...] [--apply]

  Dry run by default: prints the diff and writes nothing. Pass --apply to put
  the items in the console's inbox for review — accepting them there is what
  creates content. Exit 0 = clean, 1 = validation failure, 2 = database error."
  [& args]
  (let [apply? (boolean (some #{"--apply"} args))
        files  (edn-files args)
        {:keys [status output]} (system/with-system #(run % files {:apply? apply?}))]
    (println output)
    (System/exit status)))
