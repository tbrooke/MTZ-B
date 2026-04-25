(ns com.example.app.hello
  (:require [com.example.lib.middleware :as mid]
            [com.example.lib.ui :as ui]))

(defn app-page
  [{:keys [session]}]
  (ui/page
   "Hello world"
   [:div.space-y-6
    [:div
     [:p.text-sm.text-slate-500.uppercase.tracking-wide "Starter app"]
     [:h1 "hello world"]]
    [:p "You're signed in as " [:strong (:email session "unknown user")] "."]
    [:form {:method "post"
            :action "/_biff/auth/signout"}
     (ui/anti-forgery-field)
     (ui/button {:type "submit"} "Log out")]]))

(def module
  {:routes
   ["/app" {:middleware [mid/wrap-signed-in]}
    ["" {:get app-page
         :name ::page}]]})
