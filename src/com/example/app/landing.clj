(ns com.example.app.landing)

(defn home
  [{:keys [session]}]
  {:status 303
   :headers {"location" (if (:uid session) "/app" "/signin")}})

(def module
  {:routes
   ["/" {:get home
         :name ::home}]})
