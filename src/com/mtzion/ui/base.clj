(ns com.mtzion.ui.base
  "Base HTML page template for the public site."
  (:require [com.mtzion.lib.ui :as ui]
            [com.mtzion.ui.nav :as nav]
            [lambdaisland.hiccup :as hiccup]))

(defn preschool-page
  "Standalone page template for the preschool site with its own fonts and design tokens."
  [title content]
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
      (nav/preschool-footer)]])})

(defn page
  "Public page with mtz header/footer. Returns a Ring response map.
   site-context — :church (default) or :preschool"
  ([title content]
   (page title content :church))
  ([title content site-context]
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
       (nav/site-header nil site-context)
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
         });</script>"]]])}))
