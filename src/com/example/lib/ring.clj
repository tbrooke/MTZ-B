(ns com.example.lib.ring
  (:require [com.biffweb.admin :as biff.admin]
            [com.example.lib.middleware :as mid]
            [reitit.ring :as ring]))

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
