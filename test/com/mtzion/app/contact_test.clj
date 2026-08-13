(ns com.mtzion.app.contact-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [com.mtzion.app.contact :as contact-page]
            [com.mtzion.app.pages :as pages]
            [com.mtzion.lib.contact :as contact]
            [com.mtzion.modules :as modules]
            [com.mtzion.test-util :refer [with-temp-ctx]]))

(def ^:private good
  {:name "Jane Visitor" :email "jane@example.com"
   :message "Hello, I'd like to know what time the service starts."})

;; ---------------------------------------------------------------------------
;; Validation
;; ---------------------------------------------------------------------------

(deftest validation
  (is (nil? (contact/validate good)))
  (testing "each field is required"
    (is (some? (contact/validate (assoc good :name ""))))
    (is (some? (contact/validate (assoc good :email ""))))
    (is (some? (contact/validate (assoc good :message "   ")))))
  (testing "the email has to look like one"
    (is (some? (contact/validate (assoc good :email "not-an-email"))))
    (is (some? (contact/validate (assoc good :email "missing@domain")))))
  (testing "length limits"
    (is (some? (contact/validate (assoc good :message (apply str (repeat 5000 "x")))))))
  (testing "error messages are addressed to a person, not a developer"
    (is (re-find #"Please" (contact/validate (assoc good :name ""))))))

(deftest honeypot
  (is (not (contact/honeypot-tripped? good)))
  (is (contact/honeypot-tripped? (assoc good :website "http://spam.test"))))

;; ---------------------------------------------------------------------------
;; Emails
;; ---------------------------------------------------------------------------

(deftest office-email-is-replyable
  (let [m (contact/office-email good)]
    (is (str/includes? (:subject m) "Jane Visitor"))
    (is (str/includes? (:text m) "jane@example.com"))
    (is (str/includes? (:text m) "what time the service starts"))
    (testing "HTML escapes user input"
      (let [evil (contact/office-email (assoc good :name "<script>alert(1)</script>"))]
        (is (not (str/includes? (:html evil) "<script>")))
        (is (str/includes? (:html evil) "&lt;script&gt;"))))))

(deftest confirmation-quotes-the-message-back
  (let [m (contact/confirmation-email good)]
    (is (str/includes? (:subject m) "received"))
    (is (str/includes? (:text m) "Jane Visitor"))
    (is (str/includes? (:text m) "what time the service starts")
        "the sender gets a copy of what they wrote")
    (is (str/includes? (:text m) "704"))
    (testing "HTML escapes user input"
      (let [evil (contact/confirmation-email (assoc good :name "<b>x</b>"))]
        (is (not (str/includes? (:html evil) "<b>x</b>")))))))

(deftest contact-to-falls-back
  (is (= "office@mtz.test" (contact/contact-to {:mtz/contact-to "office@mtz.test"})))
  (is (= "reply@mtz.test"  (contact/contact-to {:mailersend/reply-to "reply@mtz.test"}))
      "a half-configured deployment still delivers somewhere real")
  (is (nil? (contact/contact-to {}))))

(deftest turnstile-is-skipped-when-unconfigured
  (is (not (contact/turnstile-configured? {})))
  (is (true? (contact/verify-turnstile {} nil nil))
      "local development must not require Cloudflare keys"))

;; ---------------------------------------------------------------------------
;; The handler
;; ---------------------------------------------------------------------------

(defn- post [ctx params]
  (contact-page/contact-post (merge ctx {:params params :remote-addr "127.0.0.1"})))

(deftest submission-sends-both-emails
  (with-temp-ctx [ctx]
    (let [sent (atom [])
          ctx  (assoc ctx
                      :mtz/contact-to "office@mtz.test"
                      :biff.admin/send-email (fn [_ m] (swap! sent conj m) true))]
      (with-redefs [com.mtzion.lib.email/send-email (fn [_ m] (swap! sent conj m) true)]
        (let [resp (post ctx good)]
          (is (= 303 (:status resp)))
          (is (= "/contact/thanks" (get-in resp [:headers "location"])))
          (is (= 2 (count @sent)) "office copy and sender confirmation")
          (let [[office confirmation] @sent]
            (is (= "office@mtz.test" (:to office)))
            (is (= "jane@example.com" (:reply-to office))
                "replying to the office copy reaches the sender")
            (is (= "jane@example.com" (:to confirmation)))))))))

(deftest a-bad-submission-is-re-rendered-not-sent
  (with-temp-ctx [ctx]
    (let [sent (atom [])
          ctx  (assoc ctx :mtz/contact-to "office@mtz.test")]
      (with-redefs [com.mtzion.lib.email/send-email (fn [_ m] (swap! sent conj m) true)]
        (let [resp (post ctx (assoc good :email "nope"))]
          (is (= 400 (:status resp)))
          (is (zero? (count @sent)) "nothing is sent")
          (testing "the form comes back with the message still in it"
            (is (str/includes? (:body resp) "what time the service starts"))
            (is (str/includes? (:body resp) "doesn't look right"))))))))

(deftest honeypot-submissions-look-successful-but-send-nothing
  (with-temp-ctx [ctx]
    (let [sent (atom [])
          ctx  (assoc ctx :mtz/contact-to "office@mtz.test")]
      (with-redefs [com.mtzion.lib.email/send-email (fn [_ m] (swap! sent conj m) true)]
        (let [resp (post ctx (assoc good :website "http://spam.test"))]
          (is (= 303 (:status resp)) "a bot is told it succeeded")
          (is (zero? (count @sent)) "but nothing is sent"))))))

(deftest a-failed-confirmation-does-not-fail-the-submission
  (with-temp-ctx [ctx]
    (let [calls (atom 0)
          ctx   (assoc ctx :mtz/contact-to "office@mtz.test")]
      (with-redefs [com.mtzion.lib.email/send-email
                    (fn [_ m]
                      (swap! calls inc)
                      (if (= "jane@example.com" (:to m))
                        (throw (ex-info "sender mailbox bounced" {}))
                        true))]
        (let [resp (post ctx good)]
          (is (= 303 (:status resp))
              "the office already has the message, so the submission succeeded")
          (is (= 2 @calls)))))))

(deftest a-failed-office-email-tells-the-sender
  (with-temp-ctx [ctx]
    (let [ctx (assoc ctx :mtz/contact-to "office@mtz.test")]
      (with-redefs [com.mtzion.lib.email/send-email (fn [_ _] false)]
        (let [resp (post ctx good)]
          (is (= 400 (:status resp)))
          (is (str/includes? (:body resp) "couldn't send")))))))

(deftest unconfigured-recipient-is-reported-not-swallowed
  (with-temp-ctx [ctx]
    (with-redefs [com.mtzion.lib.email/send-email (fn [_ _] true)]
      (let [resp (post ctx good)]   ; no :mtz/contact-to anywhere
        (is (= 400 (:status resp)))
        (is (str/includes? (:body resp) "isn't set up correctly"))))))

;; ---------------------------------------------------------------------------
;; Pages
;; ---------------------------------------------------------------------------

(defn- app
  "The ring handler plus the system map its init produced. The init also creates
  the session store, so requests must carry it — without it each request gets a
  fresh session and CSRF can never validate."
  []
  ((:biff.core/init pages/ring-module) #'modules/modules))

(defn- app-handler []
  (:biff.ring/handler (app)))

(deftest contact-page-has-a-csrf-token
  ;; Must go through the real handler: anti-forgery-field renders nothing when
  ;; the token isn't bound, and only the middleware binds it.
  (with-temp-ctx [ctx]
    (let [h    (app-handler)
          body (:body (h (merge ctx {:request-method :get :uri "/contact" :headers {}})))]
      (is (str/includes? body "__anti-forgery-token")
          "without this the POST is rejected before the handler ever runs")
      (is (str/includes? body "name=\"website\"") "honeypot present")
      (is (not (str/includes? body "challenges.cloudflare.com"))
          "no Turnstile script when it isn't configured"))))

(defn- form-post
  "A POST the way a browser sends one — a urlencoded body, not a synthetic
  :params map, which wrap-params would overwrite."
  [h ctx uri params cookie]
  (let [enc  #(java.net.URLEncoder/encode (str %) "UTF-8")
        body (str/join "&" (for [[k v] params] (str (enc (name k)) "=" (enc v))))]
    (h (merge ctx {:request-method :post
                   :uri uri
                   :headers (cond-> {"content-type"   "application/x-www-form-urlencoded"
                                     "content-length" (str (count (.getBytes body "UTF-8")))}
                              cookie (assoc "cookie" cookie))
                   :body (java.io.ByteArrayInputStream. (.getBytes body "UTF-8"))}))))

(defn- session-cookie [resp]
  (let [sc (get-in resp [:headers "Set-Cookie"])]
    (some-> (if (sequential? sc) (first sc) sc) (str/split #";") first)))

(deftest full-round-trip-through-the-middleware
  ;; The form was previously unusable: with no token every POST 403'd before
  ;; reaching the handler. This proves a real browser submission now works.
  (with-temp-ctx [ctx]
    (let [sys    (app)
          h      (:biff.ring/handler sys)
          ctx    (merge ctx sys {:mtz/contact-to "office@mtz.test"})
          get-   (h (merge ctx {:request-method :get :uri "/contact" :headers {}}))
          token  (second (re-find #"name=\"__anti-forgery-token\" value=\"([^\"]+)\"" (:body get-)))
          cookie (session-cookie get-)]
      (is (some? token) "the page hands out a token")
      (testing "posting without the token is still rejected"
        (is (= 403 (:status (form-post h ctx "/contact" good cookie)))))
      (testing "posting with it succeeds and sends both emails"
        (let [sent (atom [])]
          (with-redefs [com.mtzion.lib.email/send-email (fn [_ m] (swap! sent conj m) true)]
            (let [resp (form-post h ctx "/contact"
                                  (assoc good :__anti-forgery-token token)
                                  cookie)]
              (is (= 303 (:status resp)))
              (is (= "/contact/thanks" (get-in resp [:headers "location"])))
              (is (= 2 (count @sent))))))))))

(deftest thanks-page-renders
  (with-temp-ctx [ctx]
    (let [resp (contact-page/contact-thanks ctx)]
      (is (= 200 (:status resp)))
      (is (str/includes? (:body resp) "Thank you")))))
