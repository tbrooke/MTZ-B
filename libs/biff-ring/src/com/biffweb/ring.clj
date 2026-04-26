(ns com.biffweb.ring
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [muuntaja.middleware :as muuntaja]
            [reitit.ring :as reitit-ring]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.anti-forgery :as anti-forgery]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.defaults :as rd]
            [ring.middleware.resource :as res]
            [ring.middleware.session :as session]
            [ring.middleware.session.cookie :as cookie]
            [ring.middleware.session.memory :as memory]
            [ring.middleware.ssl :as ssl]))

(def ^:private http-status->msg
  {400 "Bad Request"
   401 "Unauthorized"
   403 "Forbidden"
   404 "Not Found"
   405 "Method Not Allowed"
   406 "Not Acceptable"
   500 "Internal Server Error"})

(defn- option
  ([ctx new-key default]
   (if (contains? ctx new-key)
     (get ctx new-key)
     default))
  ([ctx new-key old-key default]
   (cond
     (contains? ctx new-key) (get ctx new-key)
     (contains? ctx old-key) (get ctx old-key)
     :else default)))

(defn- default-on-error [{:keys [status]}]
  {:status status
   :headers {"content-type" "text/html; charset=utf-8"}
   :body (str "<h1>" (http-status->msg status "Error") "</h1>")})

(defn- on-error-handler [ctx]
  (or (:biff.ring/on-error ctx)
      (:biff.middleware/on-error ctx)
      default-on-error))

(defn- websocket-request? [{:keys [headers]}]
  (and (str/includes? (str/lower-case (get headers "upgrade" "")) "websocket")
       (str/includes? (str/lower-case (get headers "connection" "")) "upgrade")))

(defn wrap-anti-forgery-websockets [handler]
  (fn [{:keys [biff/base-url headers] :as ctx}]
    (cond
      (not (websocket-request? ctx))
      (handler ctx)

      (nil? base-url)
      (do
        (log/warn "Rejecting websocket request because :biff/base-url is not set.")
        {:status 403
         :headers {"content-type" "text/plain; charset=utf-8"}
         :body "Forbidden"})

      (not= base-url (get headers "origin"))
      (do
        (log/warn "Rejecting websocket request due to origin mismatch." {:origin (get headers "origin")})
        {:status 403
         :headers {"content-type" "text/plain; charset=utf-8"}
         :body "Forbidden"})

      :else
      (handler ctx))))

(defn wrap-resource [handler]
  (fn [{:as ctx}]
    (let [root (option ctx :biff.ring/root :biff.middleware/root "public")
          index-files (option ctx :biff.ring/index-files :biff.middleware/index-files ["index.html"])]
      (or (->> index-files
               (map #(update ctx :uri str/replace-first #"/?$" (str "/" %)))
               (into [ctx])
               (some (wrap-content-type #(res/resource-request % root))))
          (handler ctx)))))

(defn wrap-internal-error [handler]
  (fn [ctx]
    (try
      (handler ctx)
      (catch Throwable t
        (log/error t "Exception while handling request")
        ((on-error-handler ctx) (assoc ctx :status 500 :ex t))))))

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
  (fn [ctx]
    (let [secure? (option ctx :biff.ring/secure :biff.middleware/secure true)]
      (handler (if (and secure? (= :http (:scheme ctx)))
                 (assoc ctx :scheme :https)
                 ctx)))))

(defn- session-store [{:keys [biff/secret] :as ctx}]
  (let [resolve-secret (fn [k]
                         (when-some [value (get ctx k)]
                           (if (ifn? value)
                             (value)
                             value)))]
    (if-some [cookie-secret (or (resolve-secret :biff.ring/cookie-secret)
                                (resolve-secret :biff.middleware/cookie-secret)
                                (when secret
                                  (or (secret :biff.ring/cookie-secret)
                                      (secret :biff.middleware/cookie-secret))))]
    (let [decoder (java.util.Base64/getDecoder)]
      (cookie/cookie-store
       {:key (.decode ^java.util.Base64$Decoder decoder ^String cookie-secret)}))
    (do
      (log/warn "No cookie secret configured; using in-memory Ring sessions.")
      (memory/memory-store)))))

(defn wrap-session [handler]
  (fn [ctx]
    (let [session-max-age (option ctx :biff.ring/session-max-age (* 60 60 24 60))
          session-same-site (option ctx :biff.ring/session-same-site :lax)]
      ((session/wrap-session
        handler
        {:cookie-attrs {:max-age session-max-age
                        :same-site session-same-site
                        :http-only true}
         :store (session-store ctx)})
       ctx))))

(defn wrap-ssl [handler]
  (fn [ctx]
    (let [secure? (option ctx :biff.ring/secure :biff.middleware/secure true)
          hsts? (option ctx :biff.ring/hsts :biff.middleware/hsts true)
          ssl-redirect? (option ctx :biff.ring/ssl-redirect :biff.middleware/ssl-redirect false)
          handler (if secure?
                    (cond-> handler
                      hsts? ssl/wrap-hsts
                      ssl-redirect? ssl/wrap-ssl-redirect)
                    handler)]
      (handler ctx))))

(defn wrap-site-defaults [handler]
  (-> handler
      wrap-anti-forgery-websockets
      anti-forgery/wrap-anti-forgery
      wrap-session
      muuntaja/wrap-params
      muuntaja/wrap-format
      (rd/wrap-defaults
       (-> rd/site-defaults
           (assoc-in [:security :anti-forgery] false)
           (assoc-in [:responses :absolute-redirects] false)
           (assoc :session false)
           (assoc :static false)))))

(defn wrap-api-defaults [handler]
  (-> handler
      muuntaja/wrap-params
      muuntaja/wrap-format
      (rd/wrap-defaults rd/api-defaults)))

(defn wrap-base-defaults [handler]
  (-> handler
      wrap-https-scheme
      wrap-resource
      wrap-internal-error
      wrap-ssl
      wrap-log-requests))

(defn- make-default-handler [status]
  (fn [ctx]
    ((on-error-handler ctx) (assoc ctx :status status))))

(defn- module-routes [modules new-key old-key]
  (->> modules
       (keep #(or (get % new-key) (get % old-key)))
       vec))

(defn- module-middleware [modules key]
  (->> modules
       (mapcat #(get % key []))
       vec))

(defn- route-group [middleware routes]
  (when (seq routes)
    ["" {:middleware middleware}
     routes]))

(defn- routes [modules]
  (let [base-middleware (module-middleware modules :biff.ring/base-middleware)
        site-middleware (module-middleware modules :biff.ring/site-middleware)
        api-middleware (module-middleware modules :biff.ring/api-middleware)
        site-routes (module-routes modules :biff.ring/routes :routes)
        api-routes (module-routes modules :biff.ring/api-routes :api-routes)
        children (cond-> []
                   (seq site-routes)
                   (conj (route-group (into [wrap-site-defaults] site-middleware) site-routes))

                   (seq api-routes)
                   (conj (route-group (into [wrap-api-defaults] api-middleware) api-routes)))]
    [["" {:middleware (into base-middleware [wrap-base-defaults])}
      children]]))

(def ^:private handler-for-modules
  (memoize
   (fn [modules]
     (reitit-ring/ring-handler
      (reitit-ring/router (routes modules))
      (reitit-ring/create-default-handler
       {:not-found (make-default-handler 404)
        :method-not-allowed (make-default-handler 405)
        :not-acceptable (make-default-handler 406)})))))

(defn use-jetty
  [{:as ctx}]
  (let [host (option ctx :biff.ring/host :biff/host "localhost")
        port (option ctx :biff.ring/port :biff/port 8080)
        handler (or (:biff.ring/handler ctx)
                    (:biff/handler ctx))]
    (when-not handler
      (throw (ex-info "Missing Ring handler" {:required :biff.ring/handler})))
    (let [server (jetty/run-jetty
                  (fn [req]
                    (try
                      (handler (merge ctx req))
                      (catch Throwable t
                        (log/error t "Unhandled exception in Jetty handler")
                        {:status 500
                         :headers {"content-type" "text/plain; charset=utf-8"}
                         :body "Internal Server Error"})))
                  {:host host
                   :port port
                   :join? false})]
      (log/info "Jetty running on" (str "http://" host ":" port))
      (update ctx :biff/stop conj #(.stop server)))))

(defn module []
  {:biff/init
   (fn [modules-var]
     {:biff.ring/handler
      (fn [request]
        ((handler-for-modules @modules-var) request))})})
