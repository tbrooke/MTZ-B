(ns com.example.model.schema
  (:require [com.biffweb.sqlite :as biff.sqlite]))

(def columns
  {:user/id {:type :uuid :primary-key true}
   :user/email {:type :text :required true :unique true}
   :user/joined-at {:type :inst :required true}})

(def ^:private immutable-user-fields
  #{:user/id :user/email :user/joined-at})

(defn- session-user-id [ctx]
  (get-in ctx [:session :uid]))

(defn- allowed-user-update?
  [uid {:keys [op before after]}]
  (and (= op :update)
       (= uid (:user/id after))
       (every? (fn [field]
                 (= (get before field) (get after field)))
               immutable-user-fields)))

(defn authorize
  [ctx diff]
  (let [uid (session-user-id ctx)]
    (every?
     (fn [{:keys [table] :as entry}]
       (case table
         :user (allowed-user-update? uid entry)
         false))
     diff)))

(def extra-sql [])

(defn init [_modules-var]
  {:biff.sqlite/columns columns
   :biff.sqlite/extra-sql extra-sql
   :biff.sqlite/authorize #'authorize})

(def module
  {:biff.core/init #'init
   :biff.graph/resolvers (biff.sqlite/make-resolvers
                          {:biff.sqlite/columns columns})})
