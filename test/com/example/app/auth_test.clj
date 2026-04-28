(ns com.example.app.auth-test
  (:require [clojure.test :refer [deftest is]]
             [com.biffweb.sqlite :as biff.sqlite]
             [com.example.app.auth :as auth]
             [com.example.lib.email :as email]
             [com.example.model.schema :as schema]))

(deftest sqlite-user-functions-roundtrip
  (let [db-file (java.io.File/createTempFile "starter-auth" ".db")
        db-path (.getAbsolutePath db-file)]
    (.delete db-file)
    (try
      (let [ctx (biff.sqlite/use-sqlite {:biff.core/stop []
                                         :biff.sqlite/db-path db-path
                                         :biff.sqlite/columns schema/columns})
             user-id (auth/create-user! ctx {:email "test@example.com" :params {:foo "bar"}})
            stop-fn (first (:biff.core/stop ctx))]
        (is (uuid? user-id))
        (is (= user-id (auth/get-user-id ctx "test@example.com")))
        (stop-fn))
      (finally
        (.delete (java.io.File. db-path))
        (.delete (java.io.File. (str db-path "-wal")))
        (.delete (java.io.File. (str db-path "-shm")))))))

(deftest mailersend-enabled-only-when-required-settings-exist
  (let [ctx {:biff/secret (fn [k] ({:mailersend/api-key " mailer-key "} k))
             :mailersend/from "noreply@example.com"}]
    (is (true? (email/mailersend-enabled? ctx)))
    (is (false? (email/mailersend-enabled? {:biff/secret (constantly nil)
                                            :mailersend/from "noreply@example.com"})))
    (is (false? (email/mailersend-enabled? {:biff/secret (fn [k] ({:mailersend/api-key "mailer-key"} k))
                                            :mailersend/from ""})))))

(deftest captcha-is-skipped-when-email-delivery-is-disabled
  (let [ctx {:biff.auth/turnstile-secret "turnstile-secret"
             :biff/secret (constantly nil)
             :mailersend/from nil}]
    (is (false? (auth/captcha-configured? ctx)))
    (is (= {:success true} (auth/verify-captcha ctx)))
    (is (nil? (auth/captcha-head ctx)))
    (is (nil? (auth/captcha-widget (assoc ctx :biff.auth/turnstile-site-key "site-key"))))))

(deftest captcha-renders-when-email-and-turnstile-are-configured
  (let [ctx {:biff/secret (fn [k] ({:mailersend/api-key "mailer-key"} k))
             :biff.auth/turnstile-secret "turnstile-secret"
             :mailersend/from "noreply@example.com"
             :biff.auth/turnstile-site-key "site-key"}]
    (is (true? (auth/captcha-configured? ctx)))
    (is (vector? (auth/captcha-head ctx)))
    (is (vector? (auth/captcha-widget ctx)))))
