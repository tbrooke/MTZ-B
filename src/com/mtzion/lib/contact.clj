(ns com.mtzion.lib.contact
  "Contact-form validation, spam checks, and the two emails a submission sends.

  Kept separate from the page so it can be tested without rendering HTML."
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [hato.client :as http]))

(def max-name 120)
(def max-message 4000)

;; ---------------------------------------------------------------------------
;; Validation
;; ---------------------------------------------------------------------------

(def ^:private email-re
  ;; Deliberately loose. Real validation is "did the confirmation arrive"; a
  ;; strict regex only ever rejects addresses that turn out to be valid.
  #"^[^@\s]+@[^@\s.]+\.[^@\s]+$")

(defn valid-email? [s]
  (boolean (and s (re-matches email-re (str/trim s)))))

(defn validate
  "Returns nil when the submission is fine, or a message to show the sender."
  [{:keys [name email message]}]
  (let [name    (str/trim (or name ""))
        email   (str/trim (or email ""))
        message (str/trim (or message ""))]
    (cond
      (str/blank? name)            "Please tell us your name."
      (> (count name) max-name)    "That name is longer than we can accept."
      (str/blank? email)           "Please give us an email address so we can reply."
      (not (valid-email? email))   "That email address doesn't look right — please check it."
      (str/blank? message)         "Please include a message."
      (> (count message) max-message) "That message is longer than we can accept."
      :else nil)))

(defn honeypot-tripped?
  "The `website` field is hidden from people by CSS. Anything filling it is a bot."
  [params]
  (not (str/blank? (str (:website params)))))

;; ---------------------------------------------------------------------------
;; Turnstile
;; ---------------------------------------------------------------------------

(defn- secret-value [s]
  (some-> (if (ifn? s) (s) s) str str/trim not-empty))

(defn turnstile-configured? [ctx]
  (boolean (and (some-> (:biff.auth/turnstile-site-key ctx) str/trim not-empty)
                (secret-value (:biff.auth/turnstile-secret ctx)))))

(defn verify-turnstile
  "True when the challenge passed — or when Turnstile isn't configured, so local
  development doesn't require Cloudflare keys."
  [ctx token remote-ip]
  (if-not (turnstile-configured? ctx)
    true
    (try
      (let [resp (http/post "https://challenges.cloudflare.com/turnstile/v0/siteverify"
                            {:form-params (cond-> {:secret   (secret-value (:biff.auth/turnstile-secret ctx))
                                                   :response (str token)}
                                            remote-ip (assoc :remoteip remote-ip))
                             :throw-exceptions false
                             :timeout 8000})]
        (boolean (:success (json/parse-string (:body resp) true))))
      (catch Exception e
        ;; Fail closed: an unverifiable submission is not accepted.
        (log/error e "Turnstile verification failed")
        false))))

;; ---------------------------------------------------------------------------
;; The two emails
;; ---------------------------------------------------------------------------

(defn contact-to
  "The church office address. Falls back to the MailerSend reply-to so a
  half-configured deployment still delivers somewhere real."
  [ctx]
  (or (some-> (:mtz/contact-to ctx) str/trim not-empty)
      (some-> (:mailersend/reply-to ctx) str/trim not-empty)))

(defn- esc [s]
  (-> (str s)
      (str/replace "&" "&amp;") (str/replace "<" "&lt;") (str/replace ">" "&gt;")))

(defn office-email
  "Sent to the church office. reply-to is the sender, so hitting Reply works."
  [{:keys [name email message]}]
  {:subject (str "Website contact form — " name)
   :text    (str name " <" email "> wrote via the website contact form:\n\n"
                 message "\n")
   :html    (str "<p><strong>" (esc name) "</strong> &lt;" (esc email) "&gt; "
                 "wrote via the website contact form:</p>"
                 "<blockquote style=\"border-left:3px solid #ccc;margin:0;padding-left:12px;\">"
                 (str/join "" (for [p (str/split (str message) #"\n{2,}")]
                                (str "<p>" (esc p) "</p>")))
                 "</blockquote>"
                 "<p style=\"color:#666;font-size:13px;\">Reply directly to this message to "
                 "answer " (esc name) ".</p>")})

(defn confirmation-email
  "Sent to whoever filled in the form, so they have a record and know it arrived."
  [{:keys [name message]}]
  {:subject "We received your message — Mount Zion UCC"
   :text    (str "Hi " name ",\n\n"
                 "Thank you for reaching out to Mount Zion UCC. We've received your "
                 "message and someone from the church office will be in touch soon.\n\n"
                 "For reference, this is what you sent:\n\n"
                 message "\n\n"
                 "If you need us sooner, call the church office at (704) 857-1169 "
                 "during office hours, Tuesday to Friday, 9:00 AM to 2:00 PM.\n\n"
                 "Grace and peace,\nMount Zion United Church of Christ\n"
                 "1415 S. Main St, China Grove, NC 28023\n")
   :html    (str "<p>Hi " (esc name) ",</p>"
                 "<p>Thank you for reaching out to Mount Zion UCC. We've received your "
                 "message and someone from the church office will be in touch soon.</p>"
                 "<p>For reference, this is what you sent:</p>"
                 "<blockquote style=\"border-left:3px solid #ccc;margin:0;padding-left:12px;color:#555;\">"
                 (str/join "" (for [p (str/split (str message) #"\n{2,}")]
                                (str "<p>" (esc p) "</p>")))
                 "</blockquote>"
                 "<p>If you need us sooner, call the church office at "
                 "<a href=\"tel:+17048571169\">(704) 857-1169</a> during office hours, "
                 "Tuesday to Friday, 9:00&nbsp;AM to 2:00&nbsp;PM.</p>"
                 "<p>Grace and peace,<br>Mount Zion United Church of Christ<br>"
                 "1415 S. Main St, China Grove, NC 28023</p>")})
