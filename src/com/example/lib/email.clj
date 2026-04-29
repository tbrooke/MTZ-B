(ns com.example.lib.email
  (:require [clojure.string :as str]
             [clojure.tools.logging :as log]
             [hato.client :as hato]))

(defn- configured-secret [secret]
  (some-> (cond
            (instance? clojure.lang.IFn secret) (.invoke ^clojure.lang.IFn secret)
            :else secret)
          str/trim
          not-empty))

(defn mailersend-enabled?
  [{:keys [mailersend/api-key
           mailersend/from
           biff.auth/turnstile-secret]}]
  (boolean
   (and (configured-secret api-key)
        (some-> from str/trim not-empty)
        (configured-secret turnstile-secret))))

(defn- send-mailersend
  [{:keys [mailersend/from mailersend/from-name mailersend/reply-to]}
   api-key
   {:keys [to subject html text]}]
  (let [response
        (hato/post
         "https://api.mailersend.com/v1/email"
          {:headers {"Authorization" (str "Bearer " api-key)}
           :content-type :json
           :throw-exceptions false
           :as :json
           :form-params {:from (cond-> {:email from}
                                 from-name (assoc :name from-name))
                         :reply_to (cond-> {:email reply-to}
                                     from-name (assoc :name from-name))
                         :to [{:email to}]
                         :subject subject
                         :html html
                        :text text}})]
    (when (<= 400 (:status response))
      (log/error "MailerSend error:" (:body response)))
    (< (:status response) 400)))

(defn send-email
  [ctx
   {:keys [to subject text html]}]
  (let [api-key (when (mailersend-enabled? ctx)
                  (configured-secret (:mailersend/api-key ctx)))]
    (if api-key
      (send-mailersend ctx api-key {:to to
                                    :subject subject
                                    :html html
                                    :text text})
      (do
        (log/info "MailerSend not configured, printing email to console")
        (println (str "Email to " to ": " subject "\n" text))
        true))))
