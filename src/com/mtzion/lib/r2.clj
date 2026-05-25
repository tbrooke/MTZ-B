(ns com.mtzion.lib.r2
  (:require [clojure.string :as str])
  (:import [java.net URI]
           [software.amazon.awssdk.auth.credentials
            AwsBasicCredentials StaticCredentialsProvider]
           [software.amazon.awssdk.core.sync RequestBody]
           [software.amazon.awssdk.http.urlconnection UrlConnectionHttpClient]
           [software.amazon.awssdk.regions Region]
           [software.amazon.awssdk.services.s3 S3Client]
           [software.amazon.awssdk.services.s3.model
            DeleteObjectRequest PutObjectRequest]))

(defn- make-client [ctx]
  (-> (S3Client/builder)
      (.credentialsProvider
       (StaticCredentialsProvider/create
        (AwsBasicCredentials/create
         ((:r2/access-key-id ctx))
         ((:r2/secret-key ctx)))))
      (.endpointOverride
       (URI/create (str "https://" (:cf/account-id ctx) ".r2.cloudflarestorage.com")))
      (.region (Region/of "auto"))
      (.httpClientBuilder (UrlConnectionHttpClient/builder))
      .build))

(defn put!
  "Upload input-stream to R2 at key. Throws on failure."
  [ctx key content-type input-stream content-length]
  (let [client (make-client ctx)]
    (try
      (.putObject client
                  (-> (PutObjectRequest/builder)
                      (.bucket (:r2/bucket ctx))
                      (.key key)
                      (.contentType content-type)
                      .build)
                  (RequestBody/fromInputStream input-stream (long content-length)))
      (finally
        (.close client)))))

(defn delete!
  "Delete object at key from R2. Best-effort — does not throw."
  [ctx key]
  (try
    (let [client (make-client ctx)]
      (try
        (.deleteObject client
                       (-> (DeleteObjectRequest/builder)
                           (.bucket (:r2/bucket ctx))
                           (.key key)
                           .build))
        (finally
          (.close client))))
    (catch Exception _)))

(defn public-url [ctx key]
  (str (:r2/public-url ctx) "/" key))

(defn key-from-url
  "Extract the R2 object key from a public URL, or nil if URL doesn't match."
  [ctx url]
  (let [base (:r2/public-url ctx)]
    (when (and (seq url) (seq base) (str/starts-with? url (str base "/")))
      (subs url (inc (count base))))))
