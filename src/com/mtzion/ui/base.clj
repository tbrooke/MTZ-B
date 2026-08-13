(ns com.mtzion.ui.base
  "Base HTML page template for the public site."
  (:require [clojure.string :as str]
            [com.mtzion.lib.ui :as ui]
            [com.mtzion.model.nav :as model.nav]
            [com.mtzion.ui.nav :as nav]
            [lambdaisland.hiccup :as hiccup]))

(defn analytics-beacon
  "Cloudflare Web Analytics, emitted only on PUBLIC pages and only when a token
  is configured — so local development, tests, and the admin panel never report
  traffic. The token is public by design; it identifies the site, not the account.

  Cookieless, so it needs no consent banner."
  [ctx]
  (when-let [token (some-> (:mtz/analytics-token ctx) str str/trim not-empty)]
    [:script {:defer "defer"
              :src "https://static.cloudflareinsights.com/beacon.min.js"
              :data-cf-beacon (str "{\"token\": \"" token "\"}")}]))

(defn preschool-page
  "Standalone page template for the preschool site with its own fonts and design
  tokens. Accepts an optional leading ctx so it can carry the analytics beacon."
  {:arglists '([title content] [ctx title content])}
  [& args]
  (let [[ctx title content] (if (map? (first args)) args (cons nil args))]
    {:status  200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body
     (hiccup/render
      [:html {:lang "en"}
       [:head
        [:meta {:charset "utf-8"}]
        [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
        [:title title]
        [:link {:rel "icon" :href "data:,"}]
        [:link {:rel "preconnect" :href "https://fonts.googleapis.com"}]
        [:link {:rel "preconnect" :href "https://fonts.gstatic.com" :crossorigin "anonymous"}]
        [:link {:rel "stylesheet"
                :href "https://fonts.googleapis.com/css2?family=Cormorant+Garamond:ital,wght@0,400;0,500;0,600;0,700;1,400&family=IBM+Plex+Mono:wght@400;500&family=IBM+Plex+Sans:wght@400;500;600&family=Outfit:wght@300;400;500;600&display=swap"}]
        [:link {:rel "stylesheet" :href (ui/css-path)}]]
       [:body {:class "ps-page"}
        (nav/preschool-header)
        [:main content]
        (nav/preschool-footer)
        (analytics-beacon ctx)]])}))

(defn page
  "Public page with mtz header/footer. Returns a Ring response map.

   Accepts an optional leading ctx map: (page ctx title content [site-context]).
   When ctx is supplied the header nav is built from CMS pages in the DB;
   without it the static fallback nav is used.

   site-context — :church (default) or :preschool"
  {:arglists '([title content] [title content site-context]
                               [ctx title content] [ctx title content site-context])}
  [& args]
  (let [[ctx args]                   (if (map? (first args))
                                       [(first args) (rest args)]
                                       [nil args])
        [title content site-context] args
        site-context                 (or site-context :church)
        nav-data                     (when ctx
                                       (try
                                         (nav/build-nav (model.nav/nav-pages ctx))
                                         ;; nav must never take the page down
                                         (catch Exception _ nil)))]
    {:status  200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body
     (hiccup/render
      [:html {:lang "en" :class "h-full bg-white"}
       [:head
        [:meta {:charset "utf-8"}]
        [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
        [:title title]
        [:link {:rel "icon" :href "data:,"}]
        [:link {:rel "preconnect" :href "https://fonts.googleapis.com"}]
        [:link {:rel "preconnect" :href "https://fonts.gstatic.com" :crossorigin "anonymous"}]
        [:link {:rel "stylesheet"
                :href "https://fonts.googleapis.com/css2?family=EB+Garamond:wght@400;500;600;700;800&family=IBM+Plex+Sans:wght@400;500;600&family=Source+Serif+4:ital,opsz,wght@0,8..60,200..900;1,8..60,200..900&display=swap"}]
        [:link {:rel "stylesheet" :href (ui/css-path)}]
        [:script {:src "/js/htmx.min.js"}]]
       [:body {:class "mtz-page"}
        (nav/site-header nav-data site-context)
        [:main {:class "mtz-main"} content]
        (nav/site-footer site-context)
        [::hiccup/unsafe-html
         "<script>(function(){
           var h=document.getElementById('mtz-header');
           if(!h)return;
           function upd(){
             if(window.scrollY>12){
               h.classList.remove('is-top');
               h.classList.add('is-scrolled');
             }else{
               h.classList.remove('is-scrolled');
               h.classList.add('is-top');
             }
           }
           window.addEventListener('scroll',upd,{passive:true});
           upd();
         })();
         document.querySelectorAll('[data-mtz-nav-link]').forEach(function(a){
           a.addEventListener('click',function(e){
             var id=a.getAttribute('data-mtz-anchor');
             if(!id)return;
             var t=document.getElementById(id);
             if(t){e.preventDefault();t.scrollIntoView({behavior:'smooth',block:'start'});}
           });
         });</script>"]
        (analytics-beacon ctx)]])}))

