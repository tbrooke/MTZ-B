(ns com.mtzion.lib.email
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
           mailersend/from]}]
  (boolean
   (and (configured-secret api-key)
        (some-> from str/trim not-empty))))

(defn- send-mailersend
  [{:keys [mailersend/from mailersend/from-name] :as ctx}
   api-key
   {:keys [to subject html text reply-to]}]
  ;; A per-message :reply-to wins over the configured default, so the contact
  ;; form's office copy replies to whoever filled in the form.
  (let [reply    (or (some-> reply-to str str/trim not-empty)
                     (some-> (:mailersend/reply-to ctx) str/trim not-empty))
        response
        (hato/post
         "https://api.mailersend.com/v1/email"
         {:headers {"Authorization" (str "Bearer " api-key)}
          :content-type :json
          :throw-exceptions false
          :as :json
          :form-params (cond-> {:from (cond-> {:email from}
                                        from-name (assoc :name from-name))
                                :to [{:email to}]
                                :subject subject
                                :html html
                                :text text}
                         ;; omit entirely when unset — an empty address is rejected
                         reply (assoc :reply_to {:email reply}))})]
    (when (<= 400 (:status response))
      (log/error "MailerSend error:" (:body response)))
    (< (:status response) 400)))

(defn send-email
  "opts: {:to :subject :text :html} plus an optional :reply-to, which overrides
  the configured default for this message only."
  [ctx {:keys [to subject text reply-to] :as opts}]
  (let [api-key (when (mailersend-enabled? ctx)
                  (configured-secret (:mailersend/api-key ctx)))]
    (if api-key
      ;; pass opts through whole, so :reply-to survives
      (send-mailersend ctx api-key opts)
      (do
        (log/info "MailerSend not configured, printing email to console")
        (println (str "\n--- EMAIL (not sent — MailerSend unconfigured) ---\n"
                      "To:       " to "\n"
                      (when reply-to (str "Reply-To: " reply-to "\n"))
                      "Subject:  " subject "\n\n"
                      text
                      "\n--- end email ---\n"))
        true))))
