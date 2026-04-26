(ns com.example.app.auth
  (:require [com.biffweb.authenticate :as biff.auth]
             [com.biffweb.sqlite :as biff.sqlite]
             [com.example.lib.email :as email]))

(defn get-user-id [ctx email]
  (:user/id
   (first
    (biff.sqlite/execute ctx {:select [:user/id]
                              :from :user
                              :where [:= :user/email email]}))))

(defn create-user! [ctx {:keys [email]}]
  (let [id (random-uuid)]
    (biff.sqlite/execute ctx {:insert-into :user
                              :values [{:user/id id
                                        :user/email email
                                        :user/joined-at (java.time.Instant/now)}]})
    id))

(def module
  (biff.auth/module
   (merge
    {:biff.auth/app-path "/app"
     :biff.auth/primary-color "#2563eb"
     :biff/send-email #'email/send-email
     :biff.auth/get-user-id #'get-user-id
     :biff.auth/create-user! #'create-user!}
     biff.auth/turnstile-config)))
