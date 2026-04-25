(ns com.example.lib.ring
  (:require [clojure.tools.logging :as log]
            [com.biffweb.admin :as biff.admin]
            [com.example.lib.middleware :as mid]
            [reitit.ring :as ring]
            [ring.adapter.jetty :as jetty]))

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

(defn ring-module []
  {:biff/init
   (fn [modules-var]
     {:biff/handler
      (fn [request]
        ((handler-for-modules @modules-var) request))})})
