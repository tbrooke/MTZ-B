(ns com.mtzion.app.landing
  (:require [com.mtzion.lib.ui :as ui]))

(defn home [_ctx]
  (ui/page
   "Mount Zion UCC"
   [:div.space-y-4
    [:h1.text-3xl.font-bold "Mount Zion UCC"]
    [:p "Welcome. The public site is coming soon."]]))

(def module
  {:biff.ring/routes
   ["/" {:get home
         :name ::home}]})
