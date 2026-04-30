(ns com.example.app.auth-test
  (:require [clojure.test :refer [deftest is]]
             [com.biffweb.sqlite :as biff.sqlite]
             [com.example.app.auth :as auth]
             [com.example.lib.email :as email]
             [hato.client :as hato]
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
  (let [ctx {:mailersend/api-key (fn [] " mailer-key ")
             :mailersend/from "noreply@example.com"}]
    (is (true? (email/mailersend-enabled? ctx)))
    (is (false? (email/mailersend-enabled? {:mailersend/api-key (fn [] nil)
                                            :mailersend/from "noreply@example.com"})))
    (is (false? (email/mailersend-enabled? {:mailersend/api-key (fn [] "mailer-key")
                                            :mailersend/from ""})))))

(deftest send-email-sends-when-mailersend-is-configured
  (let [called? (atom false)]
    (with-redefs [hato/post (fn [& _]
                              (reset! called? true)
                              {:status 202})]
      (is (true? (email/send-email
                  {:mailersend/api-key (fn [] "mailer-key")
                   :mailersend/from "noreply@example.com"
                   :biff.auth/turnstile-secret nil}
                   {:to "test@example.com"
                    :subject "Subject"
                    :text "Hello"})))
      (is (true? @called?)))))
