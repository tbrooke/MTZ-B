(ns com.example.lib.ui
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [lambdaisland.hiccup :as hiccup]
            [ring.middleware.anti-forgery :as csrf]
            [ring.util.response :as ring-response]))

(defn anti-forgery-field []
  (when (bound? #'csrf/*anti-forgery-token*)
    [:input {:type "hidden"
             :name "__anti-forgery-token"
             :value csrf/*anti-forgery-token*}]))

(defn css-path []
  (if-some [last-modified
            (some-> (io/resource "public/css/main.css")
                    ring-response/resource-data
                    :last-modified
                    (.getTime))]
    (str "/css/main.css?t=" last-modified)
    "/css/main.css"))

(defn page
  [title & body]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body
   (str
    "<!DOCTYPE html>\n"
    (str/replace
     (hiccup/render
      [:html {:lang "en"}
       [:head
        [:meta {:charset "utf-8"}]
        [:meta {:name "viewport"
                :content "width=device-width, initial-scale=1"}]
        [:title title]
        [:link {:rel "icon" :href "data:,"}]
        [:link {:rel "stylesheet"
                :href (css-path)}]]
       [:body.mx-auto.my-12.max-w-3xl.px-4.font-sans.leading-relaxed
         body]])
      #"^<!DOCTYPE html>\n?"
      ""))})

(defn page-title [& children]
  (into [:h1 {:class "text-3xl font-bold text-slate-950"}] children))

(defn link
  [{:as opts} & children]
  (into
   [:a (update opts :class #(str "text-blue-600 hover:text-blue-800"
                                 (when % (str " " %))))]
   children))

(defn button
  [{:as opts} & children]
  (into
   [:button (update opts :class #(str "rounded bg-blue-600 px-4 py-2 font-medium text-white hover:bg-blue-700"
                                      (when % (str " " %))))]
   children))
