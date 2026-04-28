(ns com.example
  (:require [clojure.tools.logging :as log]
             [clojure.tools.namespace.repl :as tn-repl]
             [com.biffweb.admin :as biff.admin]
             [com.biffweb.background :as biff.background]
             [com.biffweb.core :as biff.core]
             [com.biffweb.config :as config]
             [com.biffweb.sqlite :as biff.sqlite]
            [com.biffweb.ring :as biff.ring]
            [com.example.lib.email :as email]
            [com.example.modules :as modules]
            [nrepl.server :as nrepl])
  (:gen-class))

(defonce system (atom {}))

(def secret-keys
  [:biff.ring/cookie-secret
   :mailersend/api-key
   :biff.auth/turnstile-secret])

(defn use-secret-values [ctx]
  (reduce (fn [ctx k]
            (update ctx k
                    (fn [value]
                      (cond
                        (nil? value) nil
                        (fn? value) value
                        :else (fn [] value)))))
          ctx
          secret-keys))

(defn initial-system []
  (let [send-email #'email/send-email]
    {:biff/send-email send-email
     :biff.auth/send-email send-email
     :biff.admin/send-email send-email}))

(def components
  [config/use-aero-config
   use-secret-values
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
