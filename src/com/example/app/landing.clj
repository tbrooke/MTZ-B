(ns com.example.app.landing)

(defn home
  [{:keys [session]}]
  {:status 303
   :headers {"location" (if (:uid session) "/app" "/signin")}})

(def module
  {:biff.ring/routes
   ["/" {:get home
         :name ::home}]})
