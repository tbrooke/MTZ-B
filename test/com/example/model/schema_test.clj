(ns com.example.model.schema-test
  (:require [clojure.test :refer [deftest is]]
            [com.example.model.schema :as schema]))

(deftest schema-module-includes-sqlite-resolvers
  (let [user-resolver (some #(when (= :com.biffweb.sqlite/user-resolver (:id %)) %)
                            (:biff.graph/resolvers schema/module))]
    (is user-resolver)
    (is (contains? (set (:output user-resolver)) :user/email))
    (is (contains? (set (:output user-resolver)) :user/joined-at))))
