(ns com.mtzion.ui.base-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [com.mtzion.app.admin :as admin]
            [com.mtzion.app.preschool :as preschool]
            [com.mtzion.ui.base :as base]
            [com.mtzion.test-util :refer [with-temp-ctx]]))

(def ^:private token "abc123def456")

(defn- beaconed? [body]
  (str/includes? (str body) "static.cloudflareinsights.com"))

(deftest analytics-is-off-unless-configured
  (testing "no token, no beacon — local dev and tests never report traffic"
    (is (nil? (base/analytics-beacon {})))
    (is (nil? (base/analytics-beacon {:mtz/analytics-token ""})))
    (is (nil? (base/analytics-beacon {:mtz/analytics-token "   "}))))
  (testing "a token produces the beacon"
    (let [tag (base/analytics-beacon {:mtz/analytics-token token})]
      (is (some? tag))
      (is (str/includes? (pr-str tag) token)))))

(deftest public-pages-carry-the-beacon-when-configured
  (with-temp-ctx [ctx]
    (let [on (assoc ctx :mtz/analytics-token token)]
      (testing "the main template"
        (is (not (beaconed? (:body (base/page ctx "T" [:p "x"])))))
        (is (beaconed? (:body (base/page on "T" [:p "x"])))))
      (testing "the preschool template, which has its own chrome"
        (is (not (beaconed? (:body (preschool/preschool ctx)))))
        (is (beaconed? (:body (preschool/preschool on))))))))

(deftest admin-pages-never-carry-the-beacon
  ;; Admin traffic is one person maintaining the site; counting it would only
  ;; pollute the numbers this exists to measure.
  (with-temp-ctx [ctx]
    (let [on (assoc ctx :mtz/analytics-token token)]
      (is (not (beaconed? (:body (admin/dashboard on)))))
      (is (not (beaconed? (:body (admin/pages-list on))))))))

(deftest beacon-token-is-embedded-as-valid-json
  (let [[_ attrs] (base/analytics-beacon {:mtz/analytics-token token})]
    (is (= "defer" (:defer attrs)))
    (is (= (str "{\"token\": \"" token "\"}") (:data-cf-beacon attrs))
        "Cloudflare parses this attribute as JSON")))
