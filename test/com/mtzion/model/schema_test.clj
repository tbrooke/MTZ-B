(ns com.mtzion.model.schema-test
  (:require [clojure.test :refer [deftest is]]
            [com.mtzion.model.schema :as schema]))

(deftest schema-module-initializes-sqlite-config
  (is (= {:biff.sqlite/columns schema/columns
          :biff.sqlite/extra-sql schema/extra-sql
          :biff.sqlite/authorize #'schema/authorize}
         ((:biff.core/init schema/module) nil))))

(deftest schema-module-includes-sqlite-resolvers
  (let [user-resolver (some #(when (= :com.biffweb.sqlite/user-resolver (:id %)) %)
                            (:biff.graph/resolvers schema/module))]
    (is user-resolver)
    (is (contains? (set (:output user-resolver)) :user/email))
    (is (contains? (set (:output user-resolver)) :user/joined-at))))

(deftest user-authorization
  (let [uid (random-uuid)
        base {:user/id uid
              :user/email "alice@example.com"
              :user/joined-at #inst "2026-01-01T00:00:00.000-00:00"
              :user/name "Alice"}]
    (is (true?
         (schema/authorize
          {:session {:uid uid}}
          [{:table :user
            :op :update
            :before base
            :after (assoc base :user/name "Alicia")}]))
        "users can edit their own mutable columns")
    (is (false?
         (schema/authorize
          {:session {:uid uid}}
          [{:table :user
            :op :update
            :before base
            :after (assoc base :user/email "other@example.com")}]))
        "users cannot edit immutable columns")
    (is (false?
         (schema/authorize
          {:session {:uid uid}}
          [{:table :user
            :op :create
            :after base}]))
        "users cannot create user rows")))
