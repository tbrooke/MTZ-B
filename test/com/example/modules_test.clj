(ns com.example.modules-test
  (:require [clojure.test :refer [deftest is]]
            [com.example :as example]))

(deftest wrap-static-resources-serves-static-assets
  (let [handler (:biff.ring/handler
                 (example/wrap-static-resources
                  {:biff.ring/handler (constantly {:status 404})}))
        response (handler {:request-method :get
                           :uri "/css/main.css"})]
    (is (= 200 (:status response)))
    (is (= "text/css"
           (get-in response [:headers "Content-Type"])))))
