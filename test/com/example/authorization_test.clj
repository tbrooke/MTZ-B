(ns com.example.authorization-test
  (:require [clojure.test :refer [deftest is]]
            [com.example.authorization :as authz]))

(deftest user-authorization
  (let [uid (random-uuid)
        base {:user/id uid
              :user/email "alice@example.com"
              :user/joined-at #inst "2026-01-01T00:00:00.000-00:00"
              :user/name "Alice"}]
    (is (true?
         (authz/authorize
          {:session {:uid uid}}
          [{:table :user
            :op :update
            :before base
            :after (assoc base :user/name "Alicia")}]))
        "users can edit their own mutable columns")
    (is (false?
         (authz/authorize
          {:session {:uid uid}}
          [{:table :user
            :op :update
            :before base
            :after (assoc base :user/email "other@example.com")}]))
        "users cannot edit immutable columns")
    (is (false?
         (authz/authorize
          {:session {:uid uid}}
          [{:table :user
            :op :create
            :after base}]))
        "users cannot create user rows")))
