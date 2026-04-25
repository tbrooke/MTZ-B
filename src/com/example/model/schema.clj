(ns com.example.model.schema
  (:require [com.biffweb.sqlite :as biff.sqlite]))

(def columns
  {:user/id {:type :uuid :primary-key true}
   :user/email {:type :text :required true :unique true}
   :user/joined-at {:type :inst :required true}})

(def module
  {:biff.sqlite/columns columns
   :biff.graph/resolvers (biff.sqlite/make-resolvers
                          {:biff.sqlite/columns columns})})
