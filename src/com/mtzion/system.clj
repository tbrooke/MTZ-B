(ns com.mtzion.system
  "Headless system for CLI tasks.

  Boots config + SQLite only — no Jetty, no queues, no nREPL, no scheduled
  tasks. Module `:biff.core/init` fns still run (that is where the schema's
  columns and extra-sql come from), so `use-sqlite` migrates on the way up
  exactly as the real app does."
  (:require [com.biffweb.config :as config]
            [com.biffweb.core :as biff.core]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.modules :as modules]))

(def ^:private components
  ;; Order matters: config supplies :biff.sqlite/db-path.
  [config/use-aero-config
   biff.sqlite/use-sqlite])

(defn start []
  (biff.core/start {} #'modules/modules components))

(defn stop [system]
  (biff.core/stop system))

(defn with-system
  "Runs (f ctx) against a booted headless system, stopping it afterwards."
  [f]
  (let [system (start)]
    (try (f system)
         (finally (stop system)))))
