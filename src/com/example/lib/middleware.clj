(ns com.example.lib.middleware
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [lambdaisland.hiccup :as hiccup]
            [muuntaja.middleware :as muuntaja]
            [ring.middleware.anti-forgery :as csrf]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.defaults :as rd]
            [ring.middleware.resource :as res]
            [ring.middleware.session :as session]
            [ring.middleware.session.memory :as memory]
            [ring.middleware.session.cookie :as cookie]
            [ring.middleware.ssl :as ssl]))

(defn wrap-signed-in [handler]
  (fn [{:keys [session] :as ctx}]
    (if (some? (:uid session))
      (handler ctx)
      {:status 303
       :headers {"location" "/signin"}})))

(defn wrap-render-hiccup [handler]
  (fn [ctx]
    (let [response (handler ctx)]
      (if (vector? response)
        {:status 200
         :headers {"content-type" "text/html; charset=utf-8"}
         :body (hiccup/render response)}
         response))))

(defn wrap-resource [handler]
  (fn [{:biff.middleware/keys [root index-files]
        :or {root "public"
             index-files ["index.html"]}
        :as ctx}]
    (or (->> index-files
             (map #(update ctx :uri str/replace-first #"/?$" (str "/" %)))
             (into [ctx])
             (some (wrap-content-type #(res/resource-request % root))))
        (handler ctx))))

(def ^:private http-status->msg
  {400 "Bad Request"
   401 "Unauthorized"
   403 "Forbidden"
   404 "Not Found"
   500 "Internal Server Error"})

(defn- default-on-error [{:keys [status]}]
  {:status status
   :headers {"content-type" "text/html; charset=utf-8"}
   :body (str "<h1>" (http-status->msg status "Error") "</h1>")})

(defn wrap-internal-error [handler]
  (fn [{:biff.middleware/keys [on-error]
        :or {on-error default-on-error}
        :as ctx}]
    (try
      (handler ctx)
      (catch Throwable t
        (log/error t "Exception while handling request")
        (on-error (assoc ctx :status 500 :ex t))))))

(defn wrap-log-requests [handler]
  (fn [ctx]
    (let [start (System/nanoTime)
          resp (handler ctx)
          stop (System/nanoTime)
          duration (quot (- stop start) 1000000)]
      (log/infof "%3sms %s %-4s %s"
                 (str duration)
                 (:status resp "nil")
                 (name (:request-method ctx))
                 (str (:uri ctx)
                      (when-some [qs (:query-string ctx)]
                        (str "?" qs))))
      resp)))

(defn wrap-https-scheme [handler]
  (fn [{:keys [biff.middleware/secure]
        :or {secure true}
        :as ctx}]
    (handler (if (and secure (= :http (:scheme ctx)))
               (assoc ctx :scheme :https)
               ctx))))

(defn- session-store [{:keys [biff/secret]}]
  (if-some [cookie-secret (when secret
                            (secret :biff.middleware/cookie-secret))]
    (cookie/cookie-store
     {:key (.decode (java.util.Base64/getDecoder) cookie-secret)})
    (do
      (log/warn "No cookie secret configured; using in-memory Ring sessions.")
      (memory/memory-store))))

(defn wrap-session [handler]
  (fn [ctx]
    ((session/wrap-session
      handler
      {:cookie-attrs {:max-age (* 60 60 24 60)
                      :same-site :lax
                      :http-only true}
       :store (session-store ctx)})
     ctx)))

(defn wrap-ssl [handler]
  (fn [{:keys [biff.middleware/secure
               biff.middleware/hsts
               biff.middleware/ssl-redirect]
        :or {secure true
             hsts true
             ssl-redirect false}
        :as ctx}]
    (let [handler
          (if secure
            (cond-> handler
              hsts ssl/wrap-hsts
              ssl-redirect ssl/wrap-ssl-redirect)
            handler)]
      (handler ctx))))

(defn wrap-site-defaults [handler]
  (-> handler
      wrap-render-hiccup
      csrf/wrap-anti-forgery
      wrap-session
      muuntaja/wrap-params
      muuntaja/wrap-format
      (rd/wrap-defaults
       (-> rd/site-defaults
           (assoc-in [:security :anti-forgery] false)
           (assoc-in [:responses :absolute-redirects] false)
           (assoc :session false)
           (assoc :static false)))))

(defn wrap-base-defaults [handler]
  (-> handler
      wrap-https-scheme
      wrap-resource
      wrap-internal-error
      wrap-ssl
      wrap-log-requests))
