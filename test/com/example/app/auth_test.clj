(ns com.example.app.auth-test
  (:require [clojure.test :refer [deftest is]]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.example.app.auth :as auth]
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
