(ns com.example.fx
  (:require [com.biffweb.sqlite :as biff.sqlite]))

(def handlers
  {:biff.fx/sqlite biff.sqlite/execute
   :biff.fx.sqlite/authorized-write biff.sqlite/authorized-write
   :biff.fx/secure-random-int
   (fn [_ n]
     (.nextInt (java.security.SecureRandom.) n))})
