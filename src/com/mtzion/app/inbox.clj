(ns com.mtzion.app.inbox
  "The Inbox pane — what arrived from the bulletin, before it is anything.

  A batch is one drop: the Sunday bulletin goes through Claude Desktop, comes
  out as EDN, and `clj -M:run import --apply` stages it here. Nothing is on the
  site until somebody accepts an item, and accepting creates a draft — so there
  are still two decisions between an extraction and a published page.

  Each card shows what accepting would do, planned against the database as it
  stands right now rather than as it stood when the file was dropped."
  (:require [clojure.string :as str]
            [com.mtzion.content.inbox :as inbox]
            [com.mtzion.content.hiccup :as ch]
            [com.mtzion.content.schema :as cs]
            [com.mtzion.lib.middleware :refer [wrap-signed-in]]
            [com.mtzion.lib.ui :as ui]
            [com.mtzion.model.normalize :as normalize]
            [com.mtzion.ui.console :as con]))

(def ^:private editor-for
  "Where an accepted item goes to be finished off."
  {"post"    "/console/writing/"
   "event"   "/console/calendar/"
   "sermon"  "/admin/sermons/"
   "feature" nil
   "page"    nil})

(defn- plan-opts [ctx]
  (when-let [r2 (:r2/public-url ctx)]
    {:image-hosts (conj ch/default-image-hosts (str r2 "/"))}))

(defn- when-str [epoch]
  (when epoch
    (-> (java.time.Instant/ofEpochSecond epoch)
        (java.time.LocalDate/ofInstant normalize/eastern)
        (.format (java.time.format.DateTimeFormatter/ofPattern "d MMM yyyy")))))

;; ---------------------------------------------------------------------------
;; What one item would do
;; ---------------------------------------------------------------------------

(defn- action-word [op]
  (case (:action op)
    :create    "New"
    :update    (if (:adopting op) "Fills in an existing one" "Updates an existing one")
    :unchanged "Already there, unchanged"
    "—"))

(defn- change-list
  "The fields that would move. Long values and HTML bodies are summarised —
  what an editor needs is whether the body changed, not the diff of it."
  [op]
  (when (seq (:changes op))
    [:dl {:class "con-diff"}
     (for [[col {:keys [from to]}] (:changes op)]
       (list
        [:dt (str/replace (name col) "_" " ")]
        [:dd (cond
               (#{:body :description} col)
               (str (count (str from)) " → " (count (str to)) " characters")

               (> (count (str to)) 70)
               (str (subs (str to) 0 67) "…")

               :else (str (or from "—") " → " (or to "—")))]))]))

(defn- item-card [row op]
  (let [type   (:type row)
        base   (str "/console/inbox/" (:id row))
        target (when (and (= :update (:action op)) (editor-for type))
                 (str (editor-for type) (:id op)))]
    [:div {:class "con-card con-inbox-card"}
     [:div {:class "con-inbox-head"}
      [:span {:class "con-kind"} (:label op type)]
      [:span {:class (str "con-inbox-action con-inbox-action--" (name (:action op)))}
       (action-word op)]]

     [:h3 {:class "con-inbox-title"} (or (not-empty (:title row)) "Untitled")]
     [:p {:class "con-inbox-key"} (:import_key row)]

     (change-list op)

     (when (= :unchanged (:action op))
       [:p {:class "con-hint"}
        "Nothing here differs from what is already on the site. Accepting stamps
         the import key so future drops match this row; dismissing is fine too."])

     [:div {:class "con-inbox-actions"}
      [:form {:method "post" :action (str base "/accept")}
       (ui/anti-forgery-field)
       [:button {:type "submit" :class "con-btn con-btn--primary"} "Accept"]]
      (when target
        [:a {:href target :class "con-btn con-btn--ghost"} "Open the existing one"])
      [:form {:method "post" :action (str base "/dismiss")}
       (ui/anti-forgery-field)
       [:button {:type "submit" :class "con-btn con-btn--quiet"} "Dismiss"]]]]))

;; ---------------------------------------------------------------------------
;; The pane
;; ---------------------------------------------------------------------------

(defn- batch-block [ctx [batch-id rows] opts]
  (let [first-row (first rows)]
    [:section {:class "con-batch"}
     [:div {:class "con-batch-head"}
      [:div
       [:h2 {:class "con-batch-title"}
        (if (= "manual" (:source first-row)) "Added by hand" "From the bulletin")]
       [:p {:class "con-batch-meta"}
        (str (count rows) (if (= 1 (count rows)) " item" " items")
             " · " (when-str (:received_at first-row))
             (when (seq (:source_ref first-row))
               (str " · " (:source_ref first-row))))]]
      [:div {:class "con-inbox-actions"}
       [:form {:method "post" :action (str "/console/inbox/batch/" batch-id "/accept")}
        (ui/anti-forgery-field)
        [:button {:type "submit" :class "con-btn con-btn--primary"}
         (str "Accept all " (count rows))]]
       [:form {:method "post" :action (str "/console/inbox/batch/" batch-id "/dismiss")}
        (ui/anti-forgery-field)
        [:button {:type "submit" :class "con-btn con-btn--quiet"
                  :onclick "return confirm('Dismiss every item in this batch?')"}
         "Dismiss all"]]]]
     [:div {:class "con-inbox-grid"}
      (for [row rows]
        (item-card row (inbox/plan-for ctx row opts)))]]))

(defn- history [ctx]
  (let [rows (inbox/decided ctx 12)]
    (when (seq rows)
      [:details {:class "con-details" :style "margin-top:34px;"}
       [:summary {:class "con-details-summary"}
        (str "Recently decided (" (count rows) ")")]
       [:table {:class "con-table" :style "padding:0 16px 16px;"}
        [:thead [:tr [:th "Title"] [:th "Kind"] [:th "Outcome"] [:th "When"]]]
        [:tbody
         (for [r rows]
           [:tr
            [:td (or (not-empty (:title r)) (:import_key r))]
            [:td [:span {:class "con-kind"} (:type r)]]
            [:td (if (= inbox/accepted-state (:state r)) "Accepted" "Dismissed")]
            [:td {:class "con-num"} (when-str (:decided_at r))]])]]])))

(defn inbox [ctx]
  (let [opts    (plan-opts ctx)
        batches (inbox/batches ctx)]
    (con/page "Inbox" (con/nav ctx :inbox)
              [:div {:class "con-single con-single--wide"}
               [:div {:class "con-list-head con-list-head--page"}
                [:h1 {:class "con-list-title"} "Inbox"]
                [:a {:href "/console/inbox/new" :class "con-btn con-btn--ghost"}
                 "+ Add by hand"]]

               (if (empty? batches)
                 (con/empty-state
                  "Nothing waiting"
                  [:p "This is where the Sunday bulletin lands. Drop the PDF and slides into
                       the Claude Desktop project, save the EDN into content-inbox/, then:"]
                  [:pre {:class "con-cmd"} "clj -M:run import          # see the diff\nclj -M:run import --apply  # put it here"]
                  [:p "Nothing reaches the site from that command. Accepting an item here
                       creates it as a draft, and publishing it is a third decision."])
                 (list
                  [:p {:class "con-hint con-hint--block"}
                   "Accepting creates a draft — it does not put anything on the site.
                    Each card shows what would happen if you accepted it now, checked
                    against the database as it stands rather than when the file arrived."]
                  (for [b batches] (batch-block ctx b opts))))

               (history ctx)])))

;; ---------------------------------------------------------------------------
;; Adding by hand
;; ---------------------------------------------------------------------------

(def ^:private manual-types
  [["post" "News item or reflection"] ["event" "Event"]])

(defn inbox-new [ctx]
  (con/page "Add to inbox" (con/nav ctx :inbox)
            [:div {:class "con-single"}
             [:div {:class "con-list-head con-list-head--page"}
              [:h1 {:class "con-list-title"} "Add to the inbox"]]
             [:p {:class "con-hint con-hint--block"}
              "For something that arrived some other way — an email from the graphics
               designer, a note from a committee. It goes through the same review as a
               bulletin extraction rather than straight onto the site."]
             [:form {:method "post" :action "/console/inbox" :class "con-form"}
              (ui/anti-forgery-field)
              [:div {:class "con-details-grid con-details-grid--open"}
               (con/field {:label "Kind"}
                          (con/select-input {:name "type"} manual-types "post"))
               (con/field {:label "Title"}
                          (con/text-input {:name "title" :required "true"}))
               (con/field {:label "Who it came from" :hint "Recorded on the card"}
                          (con/text-input {:name "source_ref" :placeholder "Kathy, by email"}))
               (con/field {:label "When" :hint "Events only — ignored for a news item"}
                          [:input {:type "datetime-local" :name "starts_at" :class "con-input"}])
               (con/field {:label "Where" :hint "Events only" :wide? true}
                          (con/text-input {:name "location"}))
               (con/field {:label "Text" :wide? true}
                          [:textarea {:name "body" :class "con-input con-textarea"
                                      :style "min-height:160px;"}])]
              [:div {:class "con-editor-bar-right" :style "margin-top:18px;"}
               [:button {:type "submit" :class "con-btn con-btn--primary"} "Add to inbox"]
               [:a {:href "/console/inbox" :class "con-btn con-btn--quiet"} "Cancel"]]]]))

(defn- manual-item
  "Form fields -> a contract item. Built here and validated by the same schema
  the importer uses, so a hand-added item cannot be shaped differently from an
  extracted one."
  [{:keys [params]}]
  (let [title (str/trim (or (:title params) ""))
        body  (str/trim (or (:body params) ""))
        key   (str (normalize/slugify title) "-"
                   (subs (str (random-uuid)) 0 4))]
    (cond-> {:type (keyword (or (not-empty (:type params)) "post"))
             :key  key
             :title title}
      (seq body) (assoc :body [[:p body]])
      (= "event" (:type params))
      ;; starts-at is deliberately passed through as-is, blank included: the
      ;; contract schema says what an event needs, and letting it say so beats
      ;; inventing a date here.
      (merge (when (seq (:starts_at params)) {:starts-at (:starts_at params)})
             (when (seq (:location params)) {:location (:location params)})))))

(defn inbox-create [ctx]
  (let [item   (manual-item ctx)
        result (cs/validate {:mtz/contract 1 :items [item]})]
    (if-not (:ok? result)
      (con/page "Add to inbox" (con/nav ctx :inbox)
                [:div {:class "con-single"}
                 (con/empty-state
                  "That didn't validate"
                  [:p "The same rules apply to a hand-added item as to an extracted one."]
                  [:pre {:class "con-cmd"} (pr-str (:errors result))]
                  [:a {:href "/console/inbox/new" :class "con-btn con-btn--primary"} "Back"])])
      (do (inbox/stage-one! ctx item (not-empty (str/trim (or (:source_ref (:params ctx)) ""))))
          {:status 303 :headers {"location" "/console/inbox"}}))))

;; ---------------------------------------------------------------------------
;; Decisions
;; ---------------------------------------------------------------------------

(defn inbox-accept [{:keys [path-params] :as ctx}]
  (when-let [row (inbox/get-one ctx (:id path-params))]
    (inbox/accept! ctx row (plan-opts ctx)))
  {:status 303 :headers {"location" "/console/inbox"}})

(defn inbox-dismiss [{:keys [path-params] :as ctx}]
  (when-let [row (inbox/get-one ctx (:id path-params))]
    (inbox/dismiss! ctx row))
  {:status 303 :headers {"location" "/console/inbox"}})

(defn inbox-accept-batch [{:keys [path-params] :as ctx}]
  (inbox/accept-batch! ctx (:batch path-params) (plan-opts ctx))
  {:status 303 :headers {"location" "/console/inbox"}})

(defn inbox-dismiss-batch [{:keys [path-params] :as ctx}]
  (inbox/dismiss-batch! ctx (:batch path-params))
  {:status 303 :headers {"location" "/console/inbox"}})

;; ---------------------------------------------------------------------------

(def module
  {:biff.ring/routes
   [["/console/inbox" {:middleware [[wrap-signed-in]]}
     ["" {:get inbox :post inbox-create :name ::inbox}]
     ["/new" {:get inbox-new :name ::inbox-new :conflicting true}]
     ["/batch/:batch/accept"  {:post inbox-accept-batch  :name ::accept-batch :conflicting true}]
     ["/batch/:batch/dismiss" {:post inbox-dismiss-batch :name ::dismiss-batch :conflicting true}]
     ["/:id/accept"  {:post inbox-accept  :name ::accept}]
     ["/:id/dismiss" {:post inbox-dismiss :name ::dismiss}]]]})
