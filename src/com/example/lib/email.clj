(ns com.example.lib.email
  (:require [clojure.tools.logging :as log]
            [hato.client :as hato]))

(defn- send-mailersend
  [{:keys [mailersend/from mailersend/reply-to]}
   api-key
   {:keys [to subject html text]}]
  (let [response
        (hato/post
         "https://api.mailersend.com/v1/email"
         {:headers {"Authorization" (str "Bearer " api-key)}
          :content-type :json
          :throw-exceptions false
          :as :json
          :form-params {:from {:email from
                               :name "Biff Starter"}
                        :reply_to {:email reply-to
                                   :name "Biff Starter"}
                        :to [{:email to}]
                        :subject subject
                        :html html
                        :text text}})]
    (when (<= 400 (:status response))
      (log/error "MailerSend error:" (:body response)))
    (< (:status response) 400)))

(defn send-email
  [{:keys [biff/secret] :as ctx}
   {:keys [to subject text html]}]
  (let [api-key (when secret
                  (secret :mailersend/api-key))]
    (if api-key
      (send-mailersend ctx api-key {:to to
                                    :subject subject
                                    :html html
                                    :text text})
      (do
        (log/info "MailerSend not configured, printing email to console")
        (println (str "Email to " to ": " subject "\n" text))
        true))))
