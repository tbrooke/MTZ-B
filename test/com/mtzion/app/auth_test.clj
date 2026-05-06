(ns com.mtzion.app.auth-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.app.auth :as auth]
            [com.mtzion.model.schema :as schema]))

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
  (let [db-file (java.io.File/createTempFile "auth-test" ".db")
        db-path (.getAbsolutePath db-file)]
    (.delete db-file)
    (try
      (let [ctx       (biff.sqlite/use-sqlite {:biff.core/stop []
                                               :biff.sqlite/db-path db-path
                                               :biff.sqlite/columns schema/columns})
            stop-fn   (first (:biff.core/stop ctx))
            req       #(merge ctx {:params % :session {}})]
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
            (is (= "/admin/signin?error=1" (get-in resp [:headers "location"])))))
        (stop-fn))
      (finally
        (.delete (java.io.File. db-path))
        (.delete (java.io.File. (str db-path "-wal")))
        (.delete (java.io.File. (str db-path "-shm")))))))
