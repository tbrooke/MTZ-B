(ns com.example.app.landing-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.example.app.landing :as landing]))

(deftest landing-page-redirects
  (testing "signed-out visitors go to sign in"
    (is (= "/signin"
           (get-in (landing/home {}) [:headers "location"]))))
  (testing "signed-in visitors go to the app"
    (is (= "/app"
           (get-in (landing/home {:session {:uid (random-uuid)}})
                   [:headers "location"])))))
