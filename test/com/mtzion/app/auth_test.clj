(ns com.mtzion.app.auth-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.mtzion.app.auth :as auth]
            [com.mtzion.test-util :refer [with-temp-ctx]]))

(deftest password-hashing
  (testing "correct password verifies"
    (let [hash (auth/hash-password "secret")]
      (is (true? (auth/check-password "secret" hash)))))
  (testing "wrong password fails"
    (let [hash (auth/hash-password "secret")]
      (is (false? (auth/check-password "wrong" hash)))))
  (testing "nil hash fails safely"
    (is (false? (auth/check-password "anything" nil))))
  (testing "each hash is unique (salt works)"
    (is (not= (auth/hash-password "secret") (auth/hash-password "secret")))))

(deftest create-admin-and-signin
  (with-temp-ctx [ctx]
    (let [req #(merge ctx {:params % :session {}})]
      (testing "create-admin! creates a new user"
        (is (= :done (auth/create-admin! ctx "Admin@Example.com" "pass1"))))
      (testing "create-admin! upserts on repeat call (normalises email)"
        (is (= :done (auth/create-admin! ctx "admin@example.com" "pass2"))))
      (testing "correct password redirects to /admin and sets :uid"
        (let [resp (auth/signin-post (req {:email "admin@example.com" :password "pass2"}))]
          (is (= 303 (:status resp)))
          (is (= "/admin" (get-in resp [:headers "location"])))
          (is (uuid? (get-in resp [:session :uid])))))
      (testing "wrong password redirects to error page, no :uid"
        (let [resp (auth/signin-post (req {:email "admin@example.com" :password "wrong"}))]
          (is (= 303 (:status resp)))
          (is (= "/admin/signin?error=1" (get-in resp [:headers "location"])))
          (is (nil? (get-in resp [:session :uid])))))
      (testing "email matching is case-insensitive"
        (let [resp (auth/signin-post (req {:email "ADMIN@EXAMPLE.COM" :password "pass2"}))]
          (is (= "/admin" (get-in resp [:headers "location"])))))
      (testing "unknown email redirects to error page"
        (let [resp (auth/signin-post (req {:email "nobody@example.com" :password "pass2"}))]
          (is (= "/admin/signin?error=1" (get-in resp [:headers "location"]))))))))
