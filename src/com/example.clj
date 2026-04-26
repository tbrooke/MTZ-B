(ns com.example
  (:require [clojure.tools.logging :as log]
            [com.biffweb.admin :as biff.admin]
            [com.biffweb.config :as config]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.example.authorization :as authz]
            [com.example.fx :as fx]
            [com.biffweb.ring :as ring]
            [com.example.lib.email :as email]
            [com.example.modules :as modules]
            [nrepl.server :as nrepl])
  (:gen-class))

(defonce system (atom {}))

(defn init
  [modules-var initial-system]
  (let [init-results
        (->> @modules-var
             (keep :biff/init)
              (map (fn [init-fn] (init-fn modules-var)))
              (apply merge))]
     (merge init-results initial-system)))

(defn initial-system []
  (init
   #'modules/modules
    {:biff/stop []
     :biff/send-email #'email/send-email
     :biff.sqlite/columns (apply merge (keep :biff.sqlite/columns modules/modules))
     :biff.sqlite/extra-sql (into [] (mapcat :biff.sqlite/extra-sql) modules/modules)
     :biff.sqlite/authorize #'authz/authorize
     :biff.fx/get-handlers (fn [] fx/handlers)}))

(def components
  [config/use-aero-config
   biff.admin/use-alerts
   biff.sqlite/use-sqlite
   ring/use-jetty])

(defn start []
  (let [new-system
         (reduce (fn [ctx component]
                   (log/info "starting:" (str component))
                   (component ctx))
                 (initial-system)
                 components)]
    (reset! system new-system)
    (log/info "System started.")
    (log/info "Go to" (:biff/base-url new-system))
    new-system))

(defn stop []
  (doseq [stop-fn (reverse (:biff/stop @system))]
    (stop-fn))
  (reset! system {})
  :stopped)

(defn refresh []
  (stop)
  (start)
  :done)

(defn -main [& _args]
  (let [{:biff.nrepl/keys [port]
         :or {port 7888}}
        (config/use-aero-config {:biff.config/skip-validation true})]
    (nrepl/start-server :port port)
    (spit ".nrepl-port" port)
    (log/info "nREPL server started on port" port))
  (start))
