(ns com.example
  (:require [clojure.tools.logging :as log]
	        [clojure.tools.namespace.repl :as tn-repl]
            [com.biffweb.admin :as biff.admin]
            [com.biffweb.background :as biff.background]
            [com.biffweb.core :as biff.core]
            [com.biffweb.config :as config]
            [com.biffweb.ring :as biff.ring]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.example.lib.email :as email]
            [com.example.modules :as modules]
            [nrepl.server :as nrepl])
  (:gen-class))

(defonce system (atom {}))

(defn initial-system []
  (let [send-email #'email/send-email]
    {:biff.auth/send-email send-email
     :biff.admin/send-email send-email}))

(def components
  [config/use-aero-config
   biff.admin/use-alerts
   biff.sqlite/use-sqlite
   biff.background/use-scheduled-tasks
   biff.background/use-queues
   biff.ring/use-jetty])

(defn start []
  (let [new-system (biff.core/start (initial-system) #'modules/modules components)]
    (reset! system new-system)
    (log/info "System started.")
    new-system))

(defn stop []
  (biff.core/stop @system)
  (reset! system {})
  :stopped)

(defn refresh []
  (stop)
  (tn-repl/refresh :after `start)
  :done)

(defn -main [& _args]
  (let [{:biff.nrepl/keys [port]
         :or {port 7888}}
        (config/use-aero-config {})]
    (nrepl/start-server :port port)
    (spit ".nrepl-port" port)
    (log/info "nREPL server started on port" port))
  (start))
