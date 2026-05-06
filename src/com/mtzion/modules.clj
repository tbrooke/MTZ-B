(ns com.mtzion.modules
  (:require [com.biffweb.admin :as biff.admin]
            [com.biffweb.background :as biff.background]
            [com.biffweb.fx :as biff.fx]
            [com.biffweb.ring :as biff.ring]
            [com.biffweb.graph :as biff.graph]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.app.auth :as auth]
            [com.mtzion.app.hello :as hello]
            [com.mtzion.app.landing :as landing]
            [com.mtzion.model.schema :as schema]
            [com.mtzion.model.user :as model.user]))

(defn- get-users [ctx]
  (->> (biff.sqlite/execute ctx {:select [[:user/id :user-id]
                                          [:user/email :email]
                                          [:user/joined-at :joined-at]]
                                 :from :user
                                 :order-by [[:user/joined-at :desc]]})
       vec))

(def admin-module
  (biff.admin/module
   {:biff.admin/get-user-events (constantly [])
    :biff.admin/get-users get-users}))

(def modules
  [(biff.ring/module)
   (biff.background/module)
   (biff.fx/module)
   (biff.graph/module)
   model.user/module
   schema/module
   admin-module
   landing/module
   auth/module
   hello/module])
