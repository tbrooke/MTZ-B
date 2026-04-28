(ns com.example.app.auth
  (:require [com.biffweb.authenticate :as biff.auth]
             [com.biffweb.authenticate.impl.backend :as biff.auth.backend]
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

(defn captcha-configured? [ctx]
  (boolean
   (and (email/mailersend-enabled? ctx)
        ((:biff.auth/captcha-configured? biff.auth/turnstile-config) ctx))))

(defn verify-captcha [ctx]
  (if (captcha-configured? ctx)
    ((:biff.auth/verify-captcha biff.auth/turnstile-config) ctx)
    {:success true}))

(defn captcha-head [ctx]
  (when (captcha-configured? ctx)
    ((:biff.auth/captcha-head biff.auth/turnstile-config) ctx)))

(defn captcha-widget [ctx]
  (when (captcha-configured? ctx)
    ((:biff.auth/captcha-widget biff.auth/turnstile-config) ctx)))

(def fx-overrides
  {:biff.auth/verify-captcha #'verify-captcha
   :biff.auth/send-email #'email/send-email
   :biff.auth/get-user-id #'get-user-id
   :biff.auth/create-user! #'create-user!
   :biff.auth/new-code biff.auth.backend/new-code
   :biff.auth/new-link-token biff.auth.backend/new-link-token
   :biff.kv/get-value (fn [ctx & args]
                        (apply (:biff.kv/get-value ctx) ctx args))
   :biff.kv/set-value (fn [ctx & args]
                        (apply (:biff.kv/set-value ctx) ctx args))})

(def module
  (biff.auth/module
   {:biff.auth/app-path "/app"
    :biff.auth/primary-color "#2563eb"
    :biff.auth/send-email #'email/send-email
    :biff.auth/get-user-id #'get-user-id
    :biff.auth/create-user! #'create-user!
    :biff.auth/verify-captcha #'verify-captcha
    :biff.auth/captcha-head #'captcha-head
    :biff.auth/captcha-widget #'captcha-widget
    :biff.auth/captcha-param (:biff.auth/captcha-param biff.auth/turnstile-config)
    :biff.auth/captcha-configured? #'captcha-configured?}))
