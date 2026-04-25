(ns com.example.app.auth
  (:require [com.biffweb.authenticate :as biff.auth]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.example.lib.email :as email]))

(def module
  (biff.sqlite/auth-module
   (merge
    {:biff.auth/app-path "/app"
     :biff.auth/app-name "Biff Starter App"
     :biff.auth/primary-color "#2563eb"
     :biff.auth/send-email email/send-email}
    biff.auth/turnstile-config)))
