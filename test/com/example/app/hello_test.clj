(ns com.example.app.hello-test
  (:require [clojure.test :refer [deftest is]]
             [com.example.app.hello :as hello]))

(deftest hello-page-renders
  (let [[uri handler-map] hello/app-page
        response ((:get handler-map)
                  {:request-method :get
                   :biff.fx/handlers
                   {:biff.fx/graph (fn [_ctx _query]
                                     {:session/user {:user/email "alice@example.com"}})}})]
    (is (= "/app" uri))
    (is (= 200 (:status response)))
    (is (re-find #"hello world" (:body response)))
    (is (re-find #"alice@example.com" (:body response)))))
