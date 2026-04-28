(ns com.example.background-test
  (:require [clojure.test :refer [deftest is]]
              [com.biffweb.background :as biff.background]
              [com.example.lib.email :as email]
              [com.example :as example]
              [com.example.modules :as modules]))

(deftest app-wires-background-components
  (is (some #(= biff.background/use-scheduled-tasks %) example/components))
  (is (some #(= biff.background/use-queues %) example/components)))

(deftest background-module-initializes-empty-config
  (let [init (some (fn [module]
                     (when-let [init-fn (:biff.core/init module)]
                       (let [result (init-fn #'modules/modules)]
                         (when (contains? result :biff.background/tasks)
                           result))))
                   modules/modules)]
    (is init)
    (is (= [] (:biff.background/tasks init)))
    (is (= {} (:biff.background/queues init)))))

(deftest initial-system-uses-namespaced-email-handlers
  (let [send-email #'email/send-email
        system (example/initial-system)]
    (is (= send-email (:biff.auth/send-email system)))
    (is (= send-email (:biff.admin/send-email system)))))
