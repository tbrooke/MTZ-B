(ns com.mtzion
  (:require [clojure.tools.logging :as log]
            [clojure.tools.namespace.repl :as tn-repl]
            [com.biffweb.admin :as biff.admin]
            [com.biffweb.background :as biff.background]
            [com.biffweb.core :as biff.core]
            [com.biffweb.config :as config]
            [com.biffweb.ring :as biff.ring]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.lib.email :as email]
            [com.mtzion.modules :as modules]
            [nrepl.server :as nrepl]
            [cider.nrepl :refer [cider-middleware]])
  (:gen-class))

(defonce system (atom {}))

(defn initial-system []
  {:biff.admin/send-email #'email/send-email})

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
    (nrepl/start-server :port port
                        :handler (apply nrepl/default-handler cider-middleware))
    (spit ".nrepl-port" port)
    (log/info "nREPL server started on port" port))
  (start))
