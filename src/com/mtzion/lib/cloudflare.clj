(ns com.mtzion.lib.cloudflare
  "Cloudflare Images primitives.

  These lived privately in app/media.clj, which meant the console could not use
  them without a second copy of the URL shapes — and the delivery URL in
  particular has already been got wrong once (omitting the account hash makes
  every image 404 silently)."
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [hato.client :as http]))

(defn api-url [ctx path]
  (str "https://api.cloudflare.com/client/v4/accounts/" (:cf/account-id ctx) path))

(defn headers
  "The API token is a #biff/secret, so it arrives as a thunk and has to be
  called."
  [ctx]
  {"Authorization" (str "Bearer " ((:cf/api-token ctx)))})

(defn delivery-url
  "The account hash segment is required — without it the URL 404s."
  [ctx image-id variant]
  (when (seq image-id)
    (str "https://imagedelivery.net/" (:cf/images-hash ctx) "/" image-id "/"
         (or variant "public"))))

(defn configured? [ctx]
  (boolean (and (:cf/account-id ctx) (:cf/images-hash ctx))))

;; ---------------------------------------------------------------------------
;; API
;; ---------------------------------------------------------------------------

(defn list-images
  "One page of the account's images, newest first. 100 per page is Cloudflare's
  maximum."
  [ctx page]
  (let [resp (http/get (api-url ctx "/images/v1")
                       {:headers      (headers ctx)
                        :query-params {"per_page" 100 "page" page "sort_order" "desc"}
                        :as           :string})]
    (get-in (json/parse-string (:body resp) true) [:result :images])))

(defn upload!
  "Uploads one file and returns Cloudflare's result map, or nil. `metadata` is
  stored on the image too, so the account remains readable without this app."
  [ctx {:keys [tempfile filename content-type]} metadata]
  (when tempfile
    (let [resp (http/post (api-url ctx "/images/v1")
                          {:headers   (headers ctx)
                           :multipart [{:name "file"
                                        :content (java.io.FileInputStream. tempfile)
                                        :filename filename
                                        :content-type (or content-type "application/octet-stream")}
                                       {:name "metadata"
                                        :content (json/generate-string metadata)}]
                           :as :string})]
      (get (json/parse-string (:body resp) true) :result))))

(defn delete!
  "Removes the image from Cloudflare. Returns true when it is gone — including
  when it was already gone, which is the outcome the caller wants either way."
  [ctx image-id]
  (try
    (http/delete (api-url ctx (str "/images/v1/" image-id))
                 {:headers (headers ctx) :as :string :throw-exceptions false})
    true
    (catch Exception _ false)))

;; ---------------------------------------------------------------------------
;; Reading what the API returns
;; ---------------------------------------------------------------------------

(defn image-meta
  "Cloudflare returns metadata as a map or a JSON string depending on the call."
  [img]
  (let [m (:meta img)]
    (cond (map? m) m
          (string? m) (try (json/parse-string m true) (catch Exception _ {}))
          :else {})))

(defn variant-url [img suffix fallback]
  (or (some #(when (str/ends-with? % (str "/" suffix)) %) (:variants img))
      fallback))
