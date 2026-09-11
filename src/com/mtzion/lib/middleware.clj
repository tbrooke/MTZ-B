(ns com.mtzion.lib.middleware)

(defn- return-to
  "Where the visitor was actually headed. The query string is part of it —
  /console/writing?id=… is a different destination from /console/writing."
  [{:keys [uri query-string]}]
  (str uri (when (seq query-string) (str "?" query-string))))

(defn wrap-signed-in [handler]
  (fn [{:keys [session] :as ctx}]
    (if (some? (:uid session))
      (handler ctx)
      {:status 303
       :headers {"location"
                 ;; Carry the destination through the sign-in so a bookmarked
                 ;; or linked console page survives the detour, instead of
                 ;; landing everyone on the same dashboard.
                 (str "/admin/signin?next="
                      (java.net.URLEncoder/encode (return-to ctx) "UTF-8"))}})))
