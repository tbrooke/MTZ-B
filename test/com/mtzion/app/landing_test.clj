(ns com.mtzion.app.landing-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.mtzion.app.landing :as landing]
            [com.mtzion.test-util :refer [with-temp-ctx]]))

(deftest landing-page-renders
  (with-temp-ctx [ctx]
    (let [response (landing/home ctx)]
      (is (= 200 (:status response)))
      (testing "renders site chrome"
        (is (re-find #"Mount Zion" (:body response)))
        (is (re-find #"mtz-header" (:body response)))
        (is (re-find #"mtz-footer" (:body response))))
      (testing "renders with no content rows present"
        (is (re-find #"mtz-main" (:body response)))))))
