(ns com.example.app.hello
  (:require [com.biffweb.fx :as fx]
             [com.example.lib.middleware :as mid]
             [com.example.lib.ui :as ui]))

(fx/defroute app-page "/app"
  [:biff.fx/graph [{:session/user [:user/email]}]]
  :get
  (fn [_ctx {:keys [session/user]}]
    (ui/page
      "Hello world"
      [:div.space-y-6
       [:div
        [:p.text-sm.text-slate-500.uppercase.tracking-wide "Starter app"]
        (ui/page-title "hello world")]
       [:p "You're signed in as " [:strong (:user/email user)] "."]
       [:div.flex.items-center.gap-4
        (ui/link {:href "/"} "Home")
        [:form {:method "post"
                :action "/_biff/auth/signout"}
         (ui/anti-forgery-field)
         (ui/button {:type "submit"} "Log out")]]])))

(def module
  {:routes
   [["" {:middleware [mid/wrap-signed-in]}
     app-page]]})
