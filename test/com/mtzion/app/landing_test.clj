(ns com.mtzion.app.landing-test
  (:require [clojure.test :refer [deftest is]]
            [com.mtzion.app.landing :as landing]))

(deftest landing-page-renders
  (let [response (landing/home {})]
    (is (= 200 (:status response)))
    (is (re-find #"Mount Zion" (:body response)))))
