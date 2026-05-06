(ns com.mtzion.background-test
  (:require [clojure.test :refer [deftest is]]
            [com.biffweb.background :as biff.background]
            [com.mtzion.lib.email :as email]
            [com.mtzion :as example]
            [com.mtzion.modules :as modules]))

(deftest app-wires-background-components
  (is (some #(= biff.background/use-scheduled-tasks %) example/components))
  (is (some #(= biff.background/use-queues %) example/components)))

(deftest background-module-initializes-admin-pstats-task
  (let [init (some (fn [module]
                     (when-let [init-fn (:biff.core/init module)]
                       (let [result (init-fn #'modules/modules)]
                         (when (contains? result :biff.background/tasks)
                           result))))
                   modules/modules)]
    (is init)
    (is (= 1 (count (:biff.background/tasks init))))
    (is (fn? (get-in init [:biff.background/tasks 0 :schedule])))
    (is (fn? (get-in init [:biff.background/tasks 0 :task])))
    (is (= {} (:biff.background/queues init)))))

(deftest initial-system-wires-admin-email
  (let [system (example/initial-system)]
    (is (= #'email/send-email (:biff.admin/send-email system)))))
