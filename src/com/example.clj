(ns com.example
  (:require [clojure.tools.logging :as log]
            [com.biffweb.admin :as biff.admin]
            [com.biffweb.config :as config]
            [com.biffweb.graph :as biff.graph]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.example.app.auth :as auth]
            [com.example.app.hello :as hello]
            [com.example.app.landing :as landing]
            [com.example.authorization :as authz]
            [com.example.fx :as fx]
            [com.example.lib.email :as email]
            [com.example.lib.middleware :as mid]
            [com.example.model.schema :as schema]
            [com.example.model.user :as model.user]
            [nrepl.server :as nrepl]
            [reitit.ring :as ring]
            [ring.adapter.jetty :as jetty])
  (:gen-class))

(defonce system (atom {}))
(defonce nrepl-server (atom nil))

(defn- routes [modules]
  [["" {:middleware [mid/wrap-site-defaults
                     biff.admin/wrap-profiling]}
    (keep :routes modules)]])

(def ^:private handler-for-modules
  (memoize
   (fn [modules]
     (-> (ring/ring-handler
          (ring/router (routes modules))
          (ring/create-default-handler))
         mid/wrap-base-defaults))))

(defn ring-module []
  {:biff/init
   (fn [modules-var]
     {:biff/handler
      (fn [request]
        ((handler-for-modules @modules-var) request))})})

(defn use-jetty
  [{:biff/keys [host port handler]
    :or {host "localhost" port 8080}
    :as ctx}]
  (let [server (jetty/run-jetty
                (fn [req] (handler (merge ctx req)))
                {:host host
                 :port port
                 :join? false})]
    (log/info "Jetty running on" (str "http://" host ":" port))
    (update ctx :biff/stop conj #(.stop server))))

(defn ensure-nrepl!
  [{:biff.nrepl/keys [port]
    :or {port 7888}}]
  (when-not @nrepl-server
    (reset! nrepl-server (nrepl/start-server :port port))
    (spit ".nrepl-port" port)
    (log/info "nREPL server started on port" port)))

(defn stop-nrepl! []
  (when-let [server @nrepl-server]
    (nrepl/stop-server server)
    (reset! nrepl-server nil)))

(defn- get-users [ctx]
  (->> (biff.sqlite/execute ctx {:select [[:user/id :user-id]
                                          [:user/email :email]
                                          [:user/joined-at :joined-at]]
                                 :from :user
                                 :order-by [[:user/joined-at :desc]]})
       vec))

(def admin-module
  (biff.admin/module
   {:biff.admin/get-user-events (constantly [])
    :biff.admin/get-users get-users}))

(def modules
  [(ring-module)
   model.user/module
   schema/module
   admin-module
   landing/module
   auth/module
   hello/module])

(defn init
  [modules-var initial-system]
  (let [init-results
        (->> @modules-var
             (keep :biff/init)
             (map (fn [init-fn] (init-fn modules-var)))
             (apply merge))]
    (merge init-results initial-system)))

(def initial-system
  (init
   #'modules
   {:biff/stop []
    :biff/send-email #'email/send-email
    :biff.sqlite/columns (apply merge (keep :biff.sqlite/columns modules))
    :biff.sqlite/extra-sql (into [] (mapcat :biff.sqlite/extra-sql) modules)
    :biff.sqlite/authorize #'authz/authorize
    :biff.fx/handlers fx/handlers
    :biff.config/skip-validation true
    :biff.graph/index
    (biff.graph/build-index
     (mapcat :biff.graph/resolvers modules)
     :middleware [biff.admin/wrap-resolver-profiling])}))

(def components
  [config/use-aero-config
   biff.admin/use-alerts
   biff.sqlite/use-sqlite
   use-jetty])

(defn start []
  (let [new-system
        (reduce (fn [ctx component]
                  (log/info "starting:" (str component))
                  (component ctx))
                initial-system
                components)]
    (ensure-nrepl! new-system)
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
  (start))

(defn -main [& _args]
  (start))
