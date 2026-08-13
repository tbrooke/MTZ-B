(ns com.mtzion.app.contact
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [com.mtzion.lib.contact :as contact]
            [com.mtzion.lib.email :as email]
            [com.mtzion.lib.ui :as ui]
            [com.mtzion.ui.base :as base]))

(def ^:private label-style
  (str "display: block; font-family: var(--mtz-sans-menu); font-size: 13px; font-weight: 600; "
       "letter-spacing: 0.08em; text-transform: uppercase; color: var(--mtz-ink-mute); margin-bottom: 6px;"))

(def ^:private input-style
  (str "width: 100%; padding: 10px 14px; border: 1px solid var(--mtz-rule); border-radius: 4px; "
       "font-family: var(--mtz-serif-body); font-size: 16px; background: var(--mtz-bg);"))

(defn- field [id fname label type values]
  [:div
   [:label {:for id :style label-style} label]
   (if (= type "textarea")
     [:textarea {:id id :name fname :rows "6" :required true
                 :style (str input-style " resize: vertical;")}
      (get values (keyword fname) "")]
     [:input {:id id :name fname :type type :required true
              :value (get values (keyword fname) "")
              :style input-style}])])

(defn- contact-form [ctx {:keys [error values]}]
  [:form {:method "post" :action "/contact" :class "mtz-stack" :style "gap: 20px;"}
   (ui/anti-forgery-field)
   ;; Honeypot — positioned off-screen so people never see it, but naive bots
   ;; fill every field they find.
   [:div {:style "position:absolute; left:-9999px;" :aria-hidden "true"}
    [:label {:for "contact-website"} "Website"]
    [:input {:id "contact-website" :name "website" :type "text"
             :tabindex "-1" :autocomplete "off"}]]

   (when error
     [:div {:role "alert"
            :style (str "padding: 12px 16px; border-radius: 4px; border: 1px solid #C24A1F; "
                        "background: #FBE9DF; color: #7a2d11; font-size: 15px;")}
      error])

   (field "contact-name"    "name"    "Your Name"     "text"     values)
   (field "contact-email"   "email"   "Email Address" "email"    values)
   (field "contact-message" "message" "Message"       "textarea" values)

   (when (contact/turnstile-configured? ctx)
     [:div {:class "cf-turnstile"
            :data-sitekey (:biff.auth/turnstile-site-key ctx)}])

   [:button {:type "submit" :class "mtz-btn mtz-btn--primary" :style "align-self: flex-start;"}
    "Send Message"]])

(defn- office-details []
  [:div
   [:h2 {:class "mtz-h2" :style "margin-bottom: 24px;"} "Church Office"]
   [:div {:class "mtz-stack" :style "gap: 28px;"}
    (for [[label body]
          [["Address"
            [:p {:style "color: var(--mtz-ink-soft); margin: 0; line-height: 1.7;"}
             "1415 S. Main St" [:br] "China Grove, NC 28023"]]
           ["Phone"
            [:a {:href "tel:+17048571169"
                 :style "color: var(--mtz-ink-soft); text-decoration: underline; text-underline-offset: 2px;"}
             "(704) 857-1169"]]
           ["Office Hours"
            [:p {:style "color: var(--mtz-ink-soft); margin: 0; line-height: 1.7;"}
             "Tuesday – Friday" [:br] "9:00 AM – 2:00 PM"]]
           ["Sunday Worship"
            [:p {:style "color: var(--mtz-ink-soft); margin: 0;"} "10:30 AM"]]]]
      [:div
       [:p {:class "mtz-mono"
            :style "font-size: 12px; letter-spacing: 0.12em; color: var(--mtz-mint-dark); font-weight: 600; text-transform: uppercase; margin: 0 0 6px;"}
        label]
       body])]])

(defn- page-content [ctx opts]
  (list
   [:section {:class "mtz-section"}
    [:p {:class "mtz-kicker"} "We'd Love to Hear From You"]
    [:h1 {:class "mtz-h1" :style "max-width: 760px;"} "Contact Us"]
    [:p {:class "mtz-lede" :style "max-width: 640px;"}
     "Reach out with questions, prayer requests, or to learn more about Mt. Zion."]
    [:hr {:class "mtz-rule"}]]

   [:section {:class "mtz-section"}
    [:div {:class "mtz-grid mtz-grid--2" :style "gap: 72px; align-items: start;"}
     [:div
      [:h2 {:class "mtz-h2" :style "margin-bottom: 32px;"} "Send Us a Message"]
      (contact-form ctx opts)]
     (office-details)]]

   ;; Only loaded when Turnstile is configured, so local development pulls
   ;; nothing from Cloudflare.
   (when (contact/turnstile-configured? ctx)
     [:script {:src "https://challenges.cloudflare.com/turnstile/v0/api.js"
               :async "async" :defer "defer"}])))

(defn contact-get [ctx]
  (base/page ctx "Contact — Mount Zion UCC" (page-content ctx {})))

;; ---------------------------------------------------------------------------
;; /contact/thanks
;; ---------------------------------------------------------------------------

(defn- thanks-content []
  [:section {:class "mtz-section"}
   [:p {:class "mtz-kicker"} "Message Sent"]
   [:h1 {:class "mtz-h1" :style "max-width: 760px;"} "Thank you — we'll be in touch."]
   [:p {:class "mtz-lede" :style "max-width: 620px;"}
    "Your message is on its way to the church office, and we've sent you a copy "
    "for your records. Someone will reply as soon as they can."]
   [:p {:style "color: var(--mtz-ink-soft); max-width: 620px;"}
    "If you need us sooner, call the office at "
    [:a {:href "tel:+17048571169" :style "text-decoration: underline; text-underline-offset: 2px;"}
     "(704) 857-1169"]
    " — Tuesday to Friday, 9:00 AM to 2:00 PM."]
   [:div {:class "mtz-row" :style "gap: 12px; margin-top: 28px; flex-wrap: wrap;"}
    [:a {:class "mtz-btn mtz-btn--primary" :href "/"} "Back to Home"]
    [:a {:class "mtz-btn mtz-btn--ghost" :href "/worship"} "Plan Your Visit"]]])

(defn contact-thanks [ctx]
  (base/page ctx "Thank You — Mount Zion UCC" (thanks-content)))

;; ---------------------------------------------------------------------------
;; Submission
;; ---------------------------------------------------------------------------

(defn- re-render [ctx params error]
  (-> (base/page ctx "Contact — Mount Zion UCC"
                 (page-content ctx {:error  error
                                    :values (select-keys params [:name :email :message])}))
      (assoc :status 400)))

(def ^:private thanks-redirect
  {:status 303 :headers {"location" "/contact/thanks"}})

(defn contact-post [{:keys [params remote-addr] :as ctx}]
  (if (contact/honeypot-tripped? params)
    ;; Bots get the success page and no email. Telling them they failed only
    ;; teaches them to try again.
    (do (log/info "contact form: honeypot tripped, discarding submission")
        thanks-redirect)

    (if-let [error (contact/validate params)]
      (re-render ctx params error)

      (if-not (contact/verify-turnstile ctx (:cf-turnstile-response params) remote-addr)
        (re-render ctx params "We couldn't verify that you're human. Please try again.")

        (let [to     (contact/contact-to ctx)
              sender (str/trim (:email params))]
          (if-not to
            (do (log/error "contact form: no :mtz/contact-to configured — message NOT delivered:"
                           (pr-str (select-keys params [:name :email :message])))
                (re-render ctx params
                           "Sorry — our contact form isn't set up correctly right now. Please call the office."))

            ;; The office copy is the one that must not be lost, so it goes first
            ;; and its failure is what the sender is told about.
            (let [sent? (try
                          (email/send-email ctx (assoc (contact/office-email params)
                                                       :to to :reply-to sender))
                          (catch Exception e
                            (log/error e "contact form: office email failed")
                            false))]
              (if-not sent?
                (do (log/error "contact form: delivery failed. Message was:"
                               (pr-str (select-keys params [:name :email :message])))
                    (re-render ctx params
                               "Sorry — we couldn't send that just now. Please try again, or call the office."))
                (do
                  ;; A failed confirmation must not fail the submission — the
                  ;; office already has the message.
                  (try
                    (email/send-email ctx (assoc (contact/confirmation-email params) :to sender))
                    (catch Exception e
                      (log/warn e "contact form: confirmation to sender failed")))
                  thanks-redirect)))))))))

(def module
  {:biff.ring/routes
   [["/contact" {:get  contact-get
                 :post contact-post
                 :name ::contact}]
    ;; Must be a real route: /contact/thanks is two segments with "contact" in
    ;; model.nav/top-level-slugs, so the CMS 404-fallback would otherwise look
    ;; "thanks" up in the page table and miss.
    ["/contact/thanks" {:get contact-thanks :name ::contact-thanks}]]})
