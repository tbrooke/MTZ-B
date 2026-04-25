(ns com.example.modules
  (:require [com.biffweb.admin :as biff.admin]
            [com.biffweb.graph :as biff.graph]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.example.app.auth :as auth]
            [com.example.app.hello :as hello]
            [com.example.app.landing :as landing]
            [com.example.lib.ring :as ring]
            [com.example.model.schema :as schema]
            [com.example.model.user :as model.user]))

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

(def graph-middleware
  [biff.admin/wrap-resolver-profiling])

(def modules
  [(ring/ring-module)
   (biff.graph/module {:middleware-var #'graph-middleware})
   model.user/module
   schema/module
   admin-module
   landing/module
   auth/module
   hello/module])
