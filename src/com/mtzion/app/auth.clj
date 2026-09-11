(ns com.mtzion.app.auth
  (:require [clojure.string :as str]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzion.lib.ui :as ui])
  (:import org.mindrot.jbcrypt.BCrypt))

(defn hash-password [password]
  (BCrypt/hashpw password (BCrypt/gensalt 12)))

(defn check-password [password hash]
  (try
    (BCrypt/checkpw password hash)
    (catch Exception _ false)))

(defn- get-user [ctx email]
  (first
   (biff.sqlite/execute ctx {:select [:user/id :user/password-hash]
                             :from :user
                             :where [:= :user/email (str/lower-case (str/trim email))]})))

(def ^:private landing
  "Where signing in puts you when nothing else asked for somewhere. The console
  is the admin surface now; /admin is still there, but it is not the front door."
  "/console")

(defn- safe-next
  "A sign-in redirect target arrives in a query string, so it is attacker-supplied
  — anyone can put a link in an email. Accept only a path on this site: one
  leading slash, and not `//elsewhere.example`, which a browser reads as
  protocol-relative and follows off-site. Backslash too, since browsers
  normalise it to a slash and `/\\evil.example` would otherwise slip through."
  [s]
  (when (and (string? s)
             (str/starts-with? s "/")
             (not (str/starts-with? s "//"))
             (not (str/includes? s "\\")))
    s))

(defn signin-get [{:keys [query-params]}]
  (let [error (get query-params "error")
        nxt   (safe-next (get query-params "next"))]
    (ui/page
     "Admin Sign In — Mount Zion UCC"
     [:div.max-w-sm.mx-auto.mt-16
      [:h1.text-2xl.font-bold.mb-6 "Admin Sign In"]
      (when error
        [:p.text-red-600.mb-4 "Invalid email or password."])
      [:form {:method "post" :action "/admin/signin" :class "space-y-4"}
       (ui/anti-forgery-field)
       (when nxt [:input {:type "hidden" :name "next" :value nxt}])
       [:div
        [:label.block.text-sm.font-medium.mb-1 {:for "email"} "Email"]
        [:input.w-full.border.rounded.px-3.py-2
         {:type "email" :name "email" :id "email" :required true :autofocus true}]]
       [:div
        [:label.block.text-sm.font-medium.mb-1 {:for "password"} "Password"]
        [:input.w-full.border.rounded.px-3.py-2
         {:type "password" :name "password" :id "password" :required true}]]
       (ui/button {:type "submit" :class "w-full mt-2"} "Sign In")]])))

(defn signin-post [{:keys [params session] :as ctx}]
  (let [email (-> (get params :email "") str/trim str/lower-case)
        password (get params :password "")
        nxt (safe-next (get params :next))
        user (get-user ctx email)]
    (if (and user
             (:user/password-hash user)
             (check-password password (:user/password-hash user)))
      {:status 303
       :headers {"location" (or nxt landing)}
       :session (assoc session :uid (:user/id user))}
      {:status 303
       ;; Keep the destination across a failed attempt — losing it on a typo
       ;; would send a corrected sign-in to the dashboard instead.
       :headers {"location" (str "/admin/signin?error=1"
                                 (when nxt
                                   (str "&next="
                                        (java.net.URLEncoder/encode nxt "UTF-8"))))}
       :session (dissoc session :uid)})))

(defn signout [{:keys [session]}]
  {:status 303
   :headers {"location" "/"}
   :session (dissoc session :uid)})

(defn create-admin!
  "REPL utility — create or update the admin user's password.
   Usage: (create-admin! @system \"you@example.com\" \"secret\")"
  [ctx email password]
  (let [email (str/lower-case (str/trim email))
        hash  (hash-password password)
        existing (first (biff.sqlite/execute ctx {:select [:user/id]
                                                  :from :user
                                                  :where [:= :user/email email]}))]
    (if existing
      (biff.sqlite/execute ctx {:update :user
                                :set {:user/password-hash hash}
                                :where [:= :user/email email]})
      (biff.sqlite/execute ctx {:insert-into :user
                                :values [{:user/id (random-uuid)
                                          :user/email email
                                          :user/password-hash hash
                                          :user/joined-at (java.time.Instant/now)}]}))
    :done))

(def module
  {:biff.ring/routes
   [["/admin/signin" {:get  signin-get
                      :post signin-post
                      :name ::signin}]
    ["/admin/signout" {:post signout
                       :name ::signout}]]})
