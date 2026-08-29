(ns com.mtzion.app.calendar
  "The Calendar pane — the month, the day, and the event, on one screen.

  /admin/events split these across three URLs: a list, a second list including
  past events, and a calendar you had to navigate away to. Here the grid picks
  the day, the day picks the event, and the event opens beside them.

  The other thing this pane adds is a way to cancel one occurrence of a series.
  A recurring event is a single row whose occurrences are computed at render
  time, so before this there was no way to say 'no Bible study on the 24th'
  short of ending the series and starting a new one."
  (:require [clojure.string :as str]
            [com.mtzion.lib.middleware :refer [wrap-signed-in]]
            [com.mtzion.lib.ui :as ui]
            [com.mtzion.model.content :as content]
            [com.mtzion.model.event :as event]
            [com.mtzion.model.normalize :as normalize]
            [com.mtzion.ui.console :as con]
            [lambdaisland.hiccup :as hiccup]))

(def ^:private eastern normalize/eastern)
(def ^:private now-epoch normalize/now-epoch)

(defn- new-id [] (str (random-uuid)))

(defn- fragment [form]
  {:status 200 :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (hiccup/render form {:doctype? false})})

;; ---------------------------------------------------------------------------
;; Dates
;; ---------------------------------------------------------------------------

(defn- today [] (java.time.LocalDate/now eastern))

(defn- fmt [^java.time.LocalDate d pattern]
  (.format d (java.time.format.DateTimeFormatter/ofPattern pattern)))

(defn- day-start-epoch [^java.time.LocalDate d]
  (.toEpochSecond (.atStartOfDay d eastern)))

(defn- epoch->date [epoch]
  (-> (java.time.Instant/ofEpochSecond epoch) (java.time.LocalDate/ofInstant eastern)))

(defn- epoch->time-str [epoch]
  (-> (java.time.Instant/ofEpochSecond epoch)
      (java.time.LocalDateTime/ofInstant eastern)
      (.format (java.time.format.DateTimeFormatter/ofPattern "h:mm a"))))

(defn- epoch->long [epoch]
  (-> (epoch->date epoch) (fmt "EEEE, d MMMM yyyy")))

(defn- parse-month [s]
  (or (when (seq s)
        (try (java.time.LocalDate/parse (str s "-01")) (catch Exception _ nil)))
      (let [t (today)] (java.time.LocalDate/of (.getYear t) (.getMonthValue t) 1))))

(defn- parse-day [s]
  (when (seq s)
    (try (java.time.LocalDate/parse s) (catch Exception _ nil))))

;; ---------------------------------------------------------------------------
;; Loading
;; ---------------------------------------------------------------------------

(defn- all-events
  "Everything not archived — the pane shows drafts too, so you can see what is
  lined up before it goes live. Skips are attached here, once, for every path
  that expands occurrences below."
  [ctx]
  (event/with-skips ctx (content/ls ctx :event {:status #{content/draft content/published}})))

(defn- occurrences-in-month [ctx first-day]
  (let [from (day-start-epoch first-day)
        to   (day-start-epoch (.plusMonths first-day 1))]
    (event/expand-in-range (all-events ctx) from to)))

(defn- occurrences-on-day [ctx ^java.time.LocalDate d]
  (event/expand-in-range (all-events ctx)
                         (day-start-epoch d)
                         (day-start-epoch (.plusDays d 1))))

;; ---------------------------------------------------------------------------
;; The month grid
;; ---------------------------------------------------------------------------

(defn- month-grid [first-day occs selected-day sel-id]
  (let [ym         (fmt first-day "yyyy-MM")
        days-in    (.lengthOfMonth first-day)
        offset     (mod (.getValue (.getDayOfWeek first-day)) 7)
        by-day     (group-by #(.getDayOfMonth (epoch->date (:start_at %))) occs)
        t          (today)
        this-month (and (= (.getYear t) (.getYear first-day))
                        (= (.getMonthValue t) (.getMonthValue first-day)))
        link       (fn [d] (str "/console/calendar?month=" ym
                                "&day=" (fmt (.withDayOfMonth first-day d) "yyyy-MM-dd")
                                (when sel-id (str "&sel=" sel-id))))]
    [:div {:class "con-cal"}
     [:div {:class "con-cal-nav"}
      [:a {:class "con-cal-arrow" :aria-label "Previous month"
           :href (str "/console/calendar?month=" (fmt (.minusMonths first-day 1) "yyyy-MM"))} "‹"]
      [:span {:class "con-cal-month"} (fmt first-day "MMMM yyyy")]
      [:a {:class "con-cal-arrow" :aria-label "Next month"
           :href (str "/console/calendar?month=" (fmt (.plusMonths first-day 1) "yyyy-MM"))} "›"]]
     [:div {:class "con-cal-grid"}
      (for [d ["S" "M" "T" "W" "T" "F" "S"]] [:div {:class "con-cal-dow"} d])
      (for [_ (range offset)] [:div {:class "con-cal-pad"}])
      (for [d (range 1 (inc days-in))
            :let [evs      (get by-day d)
                  today?   (and this-month (= d (.getDayOfMonth t)))
                  chosen?  (and selected-day (= d (.getDayOfMonth selected-day))
                                (= (.getMonth selected-day) (.getMonth first-day)))]]
        [:a {:href  (link d)
             :class (str "con-cal-day"
                         (when today?  " is-today")
                         (when chosen? " is-selected")
                         (when (seq evs) " has-events"))}
         [:span {:class "con-cal-num"} (str d)]
         (when (seq evs)
           [:span {:class "con-cal-pips"}
            (for [_ (range (min 3 (count evs)))] [:span {:class "con-cal-pip"}])])])]]))

;; ---------------------------------------------------------------------------
;; The day list
;; ---------------------------------------------------------------------------

(defn- day-list [^java.time.LocalDate d occs ym sel-id]
  [:div {:class "con-day"}
   [:div {:class "con-day-head"}
    [:span {:class "con-day-title"} (fmt d "EEEE d MMMM")]
    [:a {:class "con-btn con-btn--primary"
         :href (str "/console/calendar/new?on=" (fmt d "yyyy-MM-dd"))} "+ New"]]
   (if (empty? occs)
     [:p {:class "con-rows-empty"} "Nothing on this day."]
     (for [o occs]
       [:a {:href  (str "/console/calendar/" (:id o) "?month=" ym
                        "&day=" (fmt d "yyyy-MM-dd") "&occ=" (:start_at o))
            :class (str "con-row" (when (= (:id o) sel-id) " is-selected"))}
        (con/status-dot (:status o))
        [:span {:class "con-row-main"}
         [:span {:class "con-row-title"} (:title o)]
         [:span {:class "con-row-meta"}
          (if (= 1 (:all_day o)) "All day" (epoch->time-str (:start_at o)))
          (when (seq (:location o)) (str " · " (:location o)))]]]))])

;; ---------------------------------------------------------------------------
;; Occurrence preview
;; ---------------------------------------------------------------------------

(defn- occurrence-preview
  "The next few occurrences, each cancellable. Setting up a repeat without being
  shown what it produced is how a 'weekly' event quietly lands on the wrong day."
  [ctx ev]
  (when (not= "none" (:recurrence ev "none"))
    (let [from  (min (or (:start_at ev) 0) (now-epoch))
          plain (dissoc ev :skips)
          occs  (take 8 (event/occurrences-in-range plain from (+ (now-epoch) (* 400 86400))))
          skips (event/skips-for ctx (:id ev))]
      [:div {:class "con-occ"}
       [:div {:class "con-occ-head"}
        [:span {:class "con-label"} "Next occurrences"]
        [:span {:class "con-hint"} (event/describe ev)]]
       (if (empty? occs)
         [:p {:class "con-rows-empty"} "This repeat produces no upcoming dates."]
         (for [o occs
               :let [t       (:start_at o)
                     skipped (contains? skips t)]]
           [:div {:class (str "con-occ-row" (when skipped " is-skipped"))}
            [:span {:class "con-occ-date"} (epoch->long t)]
            [:span {:class "con-occ-time"}
             (if (= 1 (:all_day ev)) "All day" (epoch->time-str t))]
            [:form {:method "post"
                    :action (str "/console/calendar/" (:id ev)
                                 (if skipped "/unskip" "/skip"))}
             (ui/anti-forgery-field)
             [:input {:type "hidden" :name "occ" :value (str t)}]
             [:button {:type "submit"
                       :class (str "con-btn " (if skipped "con-btn--ghost" "con-btn--quiet"))}
              (if skipped "Restore" "Cancel this one")]]]))])))

;; ---------------------------------------------------------------------------
;; Editor
;; ---------------------------------------------------------------------------

(def ^:private recurrences
  [["none" "Does not repeat"] ["daily" "Every day"] ["weekly" "Every week"]
   ["biweekly" "Every two weeks"] ["monthly" "Every month"] ["yearly" "Every year"]])

(defn- editor [ctx ev qs]
  (let [new?   (nil? (:id ev))
        action (if new? "/console/calendar" (str "/console/calendar/" (:id ev)))]
    [:section {:class "con-editor"}
     [:form {:method "post" :action (str action qs) :class "con-form" :id "con-post-form"}
      (ui/anti-forgery-field)
      [:div {:class "con-editor-bar"}
       (if new?
         [:span {:class "con-pill con-pill--draft"} [:span {:class "con-pill-dot"}] "New event"]
         (con/status-pill (str action "/status") (:status ev)))
       [:div {:class "con-editor-bar-right"}
        [:button {:type "submit" :class "con-btn con-btn--primary"} "Save"]
        (when-not new?
          [:button {:type "submit" :class "con-btn con-btn--quiet"
                    :formaction (str action "/archive") :formnovalidate "true"
                    :onclick "return confirm('Archive this event? It leaves the calendar but is kept under Archive.')"}
           "Archive"])]]

      [:input {:type "text" :name "title" :class "con-title-input" :required "true"
               :value (or (:title ev) "") :placeholder "Event name" :autocomplete "off"}]

      [:div {:class "con-details-grid con-details-grid--open"}
       (con/field {:label "Starts"}
                  [:input {:type "datetime-local" :name "start_at" :class "con-input" :required "true"
                           :value (or (normalize/epoch->local-datetime-str (:start_at ev)) "")}])
       (con/field {:label "Ends" :hint "Optional"}
                  [:input {:type "datetime-local" :name "end_at" :class "con-input"
                           :value (or (normalize/epoch->local-datetime-str (:end_at ev)) "")}])
       (con/field {:label "Where"}
                  (con/text-input {:name "location" :value (or (:location ev) "")
                                   :placeholder "Fellowship Hall"}))
       (con/field {:label "Repeats"}
                  (con/select-input {:name "recurrence"} recurrences (:recurrence ev "none")))
       (con/field {:label "Until" :hint "Blank means it keeps repeating"}
                  [:input {:type "date" :name "recur_until" :class "con-input"
                           :value (or (normalize/epoch->date-str (:recur_until ev)) "")}])
       (con/field {:label "Options"}
                  [:label {:class "con-check"}
                   [:input {:type "checkbox" :name "all_day" :value "1"
                            :checked (= 1 (:all_day ev))}] "All day"]
                  [:label {:class "con-check"}
                   [:input {:type "checkbox" :name "featured" :value "1"
                            :checked (= 1 (:featured ev))}] "Feature on the home page"])]

      [:details {:class "con-details"}
       [:summary {:class "con-details-summary"} "Description & image"]
       [:div {:class "con-details-grid"}
        (con/field {:label "Image" :hint "Cloudflare image ID" :wide? true}
                   (con/text-input {:name "image_id" :value (or (:image_id ev) "")}))
        [:div {:class "con-field con-field--wide"}
         [:label {:class "con-label"} "Description"]
         [:div {:class "con-body-editor"}
          [:div {:data-tiptap "description" :class "tiptap-wrapper"}]
          [:input {:type "hidden" :name "description" :value (or (:description ev) "")}]]]]]]

     (when-not new? (occurrence-preview ctx ev))]))

;; ---------------------------------------------------------------------------
;; Page
;; ---------------------------------------------------------------------------

(defn- render [ctx first-day sel-day sel-id right]
  (let [ym   (fmt first-day "yyyy-MM")
        occs (occurrences-in-month ctx first-day)]
    (con/page "Calendar" (con/nav ctx :calendar)
              [:div {:class "con-pane"}
               [:aside {:class "con-list"}
                [:div {:class "con-list-head"}
                 [:h1 {:class "con-list-title"} "Calendar"]]
                (month-grid first-day occs sel-day sel-id)
                (when sel-day
                  (day-list sel-day (occurrences-on-day ctx sel-day) ym sel-id))]
               right]
              [:script {:src "/js/console.js" :defer "true"}])))

(defn- qs-of [{:keys [query-params]}]
  (let [m (get query-params "month") d (get query-params "day")]
    (if (or m d)
      (str "?" (str/join "&" (cond-> [] m (conj (str "month=" m)) d (conj (str "day=" d)))))
      "")))

(defn calendar [{:keys [query-params path-params] :as ctx}]
  (let [first-day (parse-month (get query-params "month"))
        sel-day   (or (parse-day (get query-params "day"))
                      (let [t (today)]
                        (when (and (= (.getYear t) (.getYear first-day))
                                   (= (.getMonthValue t) (.getMonthValue first-day)))
                          t)))
        id        (or (:id path-params) (get query-params "sel"))
        ev        (when id (content/get-one ctx :event id))]
    (if (and id (nil? ev))
      {:status 404 :body "No such event"}
      (render ctx first-day sel-day (:id ev)
              (if ev
                (editor ctx ev (qs-of ctx))
                (con/empty-state
                 "Pick a day, or an event"
                 [:p "The grid shows every event that is not archived — drafts included, so you can see what is lined up before it goes live."]
                 [:a {:href (str "/console/calendar/new"
                                 (when sel-day (str "?on=" (fmt sel-day "yyyy-MM-dd"))))
                      :class "con-btn con-btn--primary"} "+ New event"]))))))

(defn calendar-new [{:keys [query-params] :as ctx}]
  (let [on        (or (parse-day (get query-params "on")) (today))
        first-day (java.time.LocalDate/of (.getYear on) (.getMonthValue on) 1)]
    (render ctx first-day on nil
            ;; A new event defaults to 6pm on the day you clicked — church
            ;; things are mostly evenings, and it beats an empty required field.
            (editor ctx {:start_at (.toEpochSecond (.atZone (.atTime on 18 0) eastern))}
                    ""))))

;; ---------------------------------------------------------------------------
;; Writes
;; ---------------------------------------------------------------------------

(defn- event-cols [{:keys [params]}]
  {:title       (str/trim (or (:title params) ""))
   :description (or (:description params) "")
   :location    (or (:location params) "")
   :start_at    (or (normalize/local-datetime->epoch (:start_at params)) (now-epoch))
   :end_at      (normalize/local-datetime->epoch (:end_at params))
   :all_day     (if (:all_day params) 1 0)
   :recurrence  (or (not-empty (:recurrence params)) "none")
   :recur_until (normalize/local-date->epoch (:recur_until params))
   :image_id    (not-empty (:image_id params))
   :featured    (if (:featured params) 1 0)})

(defn calendar-create [ctx]
  (let [cols (event-cols ctx)
        ;; (title, start_at) is UNIQUE — the importer relies on it to adopt a
        ;; hand-made row. Reuse rather than 500 on the constraint.
        dupe (first (content/ls ctx :event {:where [:and [:= :title (:title cols)]
                                                    [:= :start_at (:start_at cols)]]
                                            :limit 1}))
        id   (or (:id dupe) (new-id))]
    (content/save! ctx :event id (cond-> cols (nil? dupe) (assoc :created_at (now-epoch))))
    {:status 303 :headers {"location" (str "/console/calendar/" id)}}))

(defn calendar-save [{:keys [path-params] :as ctx}]
  (let [id (:id path-params)]
    (content/save! ctx :event id (event-cols ctx))
    {:status 303 :headers {"location" (str "/console/calendar/" id (qs-of ctx))}}))

(defn calendar-status [{:keys [path-params] :as ctx}]
  (let [id (:id path-params)]
    (content/toggle! ctx :event id)
    (fragment (con/status-pill (str "/console/calendar/" id "/status")
                               (:status (content/get-one ctx :event id))))))

(defn calendar-archive [{:keys [path-params] :as ctx}]
  (content/archive! ctx :event (:id path-params))
  {:status 303 :headers {"location" "/console/calendar"}})

(defn- occ-param [{:keys [params]}]
  (try (Long/parseLong (str/trim (str (:occ params)))) (catch Exception _ nil)))

(defn calendar-skip [{:keys [path-params] :as ctx}]
  (let [id (:id path-params)]
    (when-let [occ (occ-param ctx)] (event/skip! ctx id occ))
    {:status 303 :headers {"location" (str "/console/calendar/" id (qs-of ctx))}}))

(defn calendar-unskip [{:keys [path-params] :as ctx}]
  (let [id (:id path-params)]
    (when-let [occ (occ-param ctx)] (event/unskip! ctx id occ))
    {:status 303 :headers {"location" (str "/console/calendar/" id (qs-of ctx))}}))

;; ---------------------------------------------------------------------------

(def module
  {:biff.ring/routes
   [["/console/calendar" {:middleware [[wrap-signed-in]]}
     ["" {:get calendar :post calendar-create :name ::calendar}]
     ["/new" {:get calendar-new :name ::calendar-new :conflicting true}]
     ["/:id" {:get calendar :post calendar-save :name ::calendar-one :conflicting true}]
     ["/:id/status"  {:post calendar-status  :name ::calendar-status}]
     ["/:id/archive" {:post calendar-archive :name ::calendar-archive}]
     ["/:id/skip"    {:post calendar-skip    :name ::calendar-skip}]
     ["/:id/unskip"  {:post calendar-unskip  :name ::calendar-unskip}]]]})
