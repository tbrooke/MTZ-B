(ns com.example-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.example.app.hello :as hello]
            [com.example.app.landing :as landing]
            [com.example.authorization :as authz]))

(deftest landing-page-redirects
  (testing "signed-out visitors go to sign in"
    (is (= "/signin"
           (get-in (landing/home {}) [:headers "location"]))))
  (testing "signed-in visitors go to the app"
    (is (= "/app"
           (get-in (landing/home {:session {:uid (random-uuid)}})
                   [:headers "location"])))))

(deftest hello-page-renders
  (let [response (hello/app-page {:session {:email "alice@example.com"}})]
    (is (= 200 (:status response)))
    (is (re-find #"hello world" (:body response)))
    (is (re-find #"alice@example.com" (:body response)))))

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
