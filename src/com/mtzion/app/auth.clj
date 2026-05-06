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

(defn signin-get [{:keys [query-params]}]
  (let [error (get query-params "error")]
    (ui/page
     "Admin Sign In — Mount Zion UCC"
     [:div.max-w-sm.mx-auto.mt-16
      [:h1.text-2xl.font-bold.mb-6 "Admin Sign In"]
      (when error
        [:p.text-red-600.mb-4 "Invalid email or password."])
      [:form {:method "post" :action "/admin/signin" :class "space-y-4"}
       (ui/anti-forgery-field)
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
        user (get-user ctx email)]
    (if (and user
             (:user/password-hash user)
             (check-password password (:user/password-hash user)))
      {:status 303
       :headers {"location" "/admin"}
       :session (assoc session :uid (:user/id user))}
      {:status 303
       :headers {"location" "/admin/signin?error=1"}
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
