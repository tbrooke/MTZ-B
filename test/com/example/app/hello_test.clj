(ns com.example.app.hello-test
  (:require [clojure.test :refer [deftest is]]
            [com.example.app.hello :as hello]))

(deftest hello-page-renders
  (let [response (hello/app-page {:session {:email "alice@example.com"}})]
    (is (= 200 (:status response)))
    (is (re-find #"hello world" (:body response)))
    (is (re-find #"alice@example.com" (:body response)))))
