(ns com.biffweb.core
  (:require [clojure.tools.logging :as log]))

(defn- init-modules
  [system modules-var]
  (let [init-results
        (->> @modules-var
             (keep :biff.core/init)
             (map (fn [init-fn] (init-fn modules-var)))
             (apply merge))]
    (merge init-results system)))

(defn- shim-old-component
  [component]
  (fn [{:biff.core/keys [stop] :as system}]
    (let [system* (component (assoc system :biff/stop stop))
          stop-fns (cond
                     (not= (:biff.core/stop system*) stop) (:biff.core/stop system*)
                     (not= (:biff/stop system*) stop) (:biff/stop system*)
                     (contains? system* :biff.core/stop) (:biff.core/stop system*)
                     :else (:biff/stop system*))]
      (-> system*
          (dissoc :biff/stop)
          (assoc :biff.core/stop (or stop-fns []))))))

(defn start
  [initial-system modules-var components]
  (let [system
        (->> (mapv shim-old-component components)
             (reduce
              (fn [system component]
                 (log/info "starting:" (str component))
                 (component system))
                (-> initial-system
                   (assoc :biff.core/stop [])
                   (init-modules modules-var))))]
    system))

(defn stop
  [system]
  (doseq [stop-fn (reverse (:biff.core/stop system))]
    (stop-fn)))
