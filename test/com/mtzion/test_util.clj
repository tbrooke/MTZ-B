(ns com.mtzion.test-util
  "Shared test fixtures.

  IMPORTANT: any test that calls `biff.sqlite/use-sqlite` MUST pass both
  `:biff.sqlite/columns` and `:biff.sqlite/extra-sql`. `apply-schema!`
  unconditionally rewrites the hardcoded path `resources/schema.sql` from
  whatever it is given, so a partial config silently strips the content tables
  out of that file. Since sqlite3def is declarative in both directions, applying
  a stripped schema.sql to a real database would DROP those tables and their
  data. Use `with-temp-ctx` rather than calling use-sqlite directly."
  (:require [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.model.schema :as schema]))

(defn temp-ctx*
  "Calls (f ctx) with a ctx backed by a fresh temp SQLite database carrying the
  full application schema. Tears the database down afterwards."
  [f]
  (let [db-file (java.io.File/createTempFile "mtz-test" ".db")
        db-path (.getAbsolutePath db-file)]
    (.delete db-file)
    (try
      (let [ctx (biff.sqlite/use-sqlite
                 {:biff.core/stop []
                  :biff.sqlite/db-path db-path
                  :biff.sqlite/columns schema/columns
                  :biff.sqlite/extra-sql schema/extra-sql})]
        (try
          (f ctx)
          (finally
            (when-let [stop-fn (first (:biff.core/stop ctx))]
              (stop-fn)))))
      (finally
        (doseq [suffix ["" "-wal" "-shm"]]
          (.delete (java.io.File. (str db-path suffix))))))))

(defmacro with-temp-ctx
  "(with-temp-ctx [ctx] body...) — runs body with ctx bound to a throwaway
  database containing every application table."
  [[binding] & body]
  `(temp-ctx* (fn [~binding] ~@body)))
