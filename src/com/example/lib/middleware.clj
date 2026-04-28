(ns com.example.lib.middleware)

(defn wrap-legacy-biff-now [handler]
  (fn [ctx]
    (handler (update ctx :biff/now #(or % (java.time.Instant/now))))))

(defn wrap-signed-in [handler]
  (fn [{:keys [session] :as ctx}]
    (if (some? (:uid session))
      (handler ctx)
      {:status 303
       :headers {"location" "/signin"}})))
