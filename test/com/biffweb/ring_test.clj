(ns com.biffweb.ring-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.biffweb.ring :as biff.ring]))

(defn- handler-for [modules]
  (let [system ((:biff/init (biff.ring/module)) (atom modules))]
    (:biff.ring/handler system)))

(defn- response-header [header value-fn]
  (fn [handler]
    (fn [ctx]
      (assoc-in (handler ctx) [:headers header] (value-fn ctx)))))

(deftest module-uses-route-groups-and-custom-middleware
  (let [handler
        (handler-for
         [{:biff.ring/routes
           ["/hello" {:get (fn [_]
                             {:status 200
                              :headers {}
                              :body "site"})
                      :name ::hello}]
           :biff.ring/base-middleware
           [(response-header
             "x-route-name"
             #(str (get-in % [:reitit.core/match :data :name])))]
           :biff.ring/site-middleware
           [(response-header "x-middleware-layer" (constantly "site"))]}
          {:biff.ring/api-routes
           ["/api/ping" {:get (fn [_]
                                {:status 200
                                 :headers {}
                                 :body "api"})}]
           :biff.ring/api-middleware
           [(response-header "x-middleware-layer" (constantly "api"))]}])]
    (testing "site routes get site middleware and base middleware sees route data"
      (let [response (handler {:request-method :get
                               :uri "/hello"
                               :headers {}})]
        (is (= 200 (:status response)))
        (is (= "site" (get-in response [:headers "x-middleware-layer"])))
        (is (= ":com.biffweb.ring-test/hello"
               (get-in response [:headers "x-route-name"])))))
    (testing "api routes get api middleware"
      (let [response (handler {:request-method :get
                               :uri "/api/ping"
                               :headers {}})]
        (is (= 200 (:status response)))
        (is (= "api" (get-in response [:headers "x-middleware-layer"])))))))

(deftest module-uses-biff-ring-on-error-for-default-handler
  (let [handler
        (handler-for
         [{:biff.ring/routes
           ["/hello" {:get (fn [_] {:status 200 :body "site"})}]}])
        response
        (handler {:request-method :get
                  :uri "/missing"
                  :headers {}
                  :biff.ring/on-error
                  (fn [{:keys [status]}]
                    {:status status
                     :body (str "custom " status)})})]
    (is (= 404 (:status response)))
    (is (= "custom 404" (:body response)))))

(deftest site-routes-accept-secret-reader-values
  (let [handler
        (handler-for
         [{:biff.ring/routes
           ["/hello" {:get (fn [_] {:status 200 :body "site"})}]}])
        response
        (handler {:request-method :get
                  :uri "/hello"
                  :headers {}
                  :biff.ring/cookie-secret (constantly "ldlF/I/l7DYn6ahOHjGEhg==")})]
    (is (= 200 (:status response)))
    (is (= "site" (:body response)))))

(deftest wrap-anti-forgery-websockets-rejects-invalid-websocket-requests
  (let [handler (biff.ring/wrap-anti-forgery-websockets (constantly {:status 200}))]
    (testing "missing base URL rejects websocket requests"
      (is (= 403
             (:status
              (handler {:headers {"upgrade" "websocket"
                                  "connection" "Upgrade"}})))))
    (testing "matching origin is allowed"
      (is (= 200
             (:status
              (handler {:biff/base-url "https://example.com"
                        :headers {"upgrade" "websocket"
                                  "connection" "Upgrade"
                                  "origin" "https://example.com"}})))))
    (testing "origin mismatch is rejected"
      (is (= 403
             (:status
              (handler {:biff/base-url "https://example.com"
                        :headers {"upgrade" "websocket"
                                  "connection" "Upgrade"
                                  "origin" "https://evil.example.com"}})))))))
