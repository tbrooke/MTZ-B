(ns com.mtzion.content.schema-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.mtzion.content.schema :as cs]))

(defn- env [& items]
  {:mtz/contract 1 :items (vec items)})

(def ^:private an-event
  {:type :event :key "back-to-school-2026"
   :title "Back-to-School Blessing"
   :starts-at "2026-08-16T10:30"})

(deftest accepts-a-realistic-bulletin-drop
  (let [r (cs/validate
           {:mtz/contract 1
            :contract-sha "a91c4f2e"
            :source {:kind :bulletin :files ["Bulletin 8-9-26.pdf"] :extracted-on "2026-08-07"}
            :items
            [{:type :sermon :key "2026-08-09" :sermon-date "2026-08-09"
              :title "The Bread That Endures"
              :scripture-cw "Exodus 16:2-4" :scripture-gospel "John 6:24-35"
              :series "bread-of-life-2026"
              :description "Pastor Jim continues the series."}
             {:type :event :key "handbell-fall-2026"
              :title "Handbell Rehearsal"
              :starts-at "2026-08-13T18:30" :ends-at "2026-08-13T19:30"
              :all-day false :location "Sanctuary"
              :recurrence :weekly :recur-until "2026-12-17"
              :featured false
              :description [[:p "New ringers welcome."]]}
             {:type :post :key "council-notes-2026-08"
              :slug "council-notes-august-2026" :category :news
              :title "Notes from Consistory"
              :excerpt "Highlights from the August meeting."
              :published-on "2026-08-09" :show-on-home false
              :body [[:p "Three items:"]
                     [:ul [:li [:strong "Roof."] " Work begins in September."]]]}
             {:type :page :key "johns-river" :slug "johns-river"
              :parent :activities :title "John's River Valley Camp"
              :nav-label "John's River" :nav-order 2
              :body [[:p "Our camp in the Blue Ridge."]]}
             {:type :feature :key "home-fall-kickoff-2026"
              :page-slug :home :title "Fall Kickoff Sunday"
              :subtitle "September 7 · 10:30 AM"
              :cta-label "See all events" :cta-url "/events"
              :sort-order 10 :show-on-home true}]})]
    (is (:ok? r) (pr-str (:errors r)))))

;; ---------------------------------------------------------------------------
;; Typed values, not form-param quirks
;; ---------------------------------------------------------------------------

(deftest booleans-must-be-real-booleans
  (is (:ok? (cs/validate (env (assoc an-event :featured false)))))
  (testing "1/0 and \"on\" are the HTML form idiom and must not leak in here"
    (is (not (:ok? (cs/validate (env (assoc an-event :featured 1))))))
    (is (not (:ok? (cs/validate (env (assoc an-event :featured "on"))))))))

(deftest dates-must-be-iso-strings
  (testing "prose dates are the most likely LLM mistake"
    (is (not (:ok? (cs/validate (env (assoc an-event :starts-at "August 13 at 6:30pm")))))))
  (testing "a date is not a datetime"
    (is (not (:ok? (cs/validate (env (assoc an-event :starts-at "2026-08-16")))))))
  (testing "no offset suffix — the contract says local church time"
    (is (not (:ok? (cs/validate (env (assoc an-event :starts-at "2026-08-16T10:30:00Z"))))))))

(deftest unknown-keys-are-rejected
  (testing "an unrecognised key is a hallucination, not something to drop silently"
    (is (not (:ok? (cs/validate (env (assoc an-event :venue "Sanctuary"))))))))

(deftest published-state-is-not-the-agents-decision
  (is (not (:ok? (cs/validate (env (assoc an-event :published true))))))
  (is (not (:ok? (cs/validate (env (assoc an-event :featured true :published 1)))))))

(deftest media-fields-are-not-in-the-contract
  (testing "media stays human-attached so re-import cannot clobber it"
    (is (not (:ok? (cs/validate (env (assoc an-event :image-id "abc123"))))))
    (is (not (:ok? (cs/validate (env {:type :sermon :key "sermon-one" :sermon-date "2026-08-09"
                                      :title "T" :video-id "xyz"})))))))

;; ---------------------------------------------------------------------------
;; Cross-field rules
;; ---------------------------------------------------------------------------

(deftest open-ended-recurrence-is-valid
  ;; Most of a church bulletin is ongoing weekly activity with no end date.
  ;; Requiring :recur-until rejected 11 of the first real bulletin's events.
  (testing "a weekly event with no end date validates"
    (is (:ok? (cs/validate (env (assoc an-event :recurrence :weekly))))))
  (testing "an end date is still accepted when the bulletin gives one"
    (is (:ok? (cs/validate (env (assoc an-event :recurrence :weekly
                                       :recur-until "2026-12-17"))))))
  (testing "non-recurring events need nothing extra"
    (is (:ok? (cs/validate (env (assoc an-event :recurrence :none)))))))

(deftest end-must-follow-start
  (is (not (:ok? (cs/validate (env (assoc an-event :ends-at "2026-08-16T09:00"))))))
  (is (:ok? (cs/validate (env (assoc an-event :ends-at "2026-08-16T11:30"))))))

(deftest duplicate-keys-are-rejected
  (let [r (cs/validate (env an-event (assoc an-event :title "Different title")))]
    (is (not (:ok? r)))
    (is (re-find #"duplicate" (-> r :errors :duplicates first :message)))))

;; ---------------------------------------------------------------------------
;; Hiccup bodies
;; ---------------------------------------------------------------------------

(deftest unsafe-bodies-are-rejected-with-field-paths
  (let [r (cs/validate (env (assoc an-event :description
                                   [[:p "ok"] [:script "alert(1)"]])))]
    (is (not (:ok? r)))
    (let [h (-> r :errors :hiccup first)]
      (is (= :disallowed-tag (:error h)))
      (is (= :description (:field h)) "the report names the offending field")
      (is (= "back-to-school-2026" (:key h)) "and the item it belongs to")))
  (testing "the raw-HTML escape hatch is refused in a post body too"
    (is (not (:ok? (cs/validate
                    (env {:type :post :key "post-one" :title "T"
                          :body [[:lambdaisland.hiccup/unsafe-html "<script>x</script>"]]})))))))

;; ---------------------------------------------------------------------------
;; Enums are constrained to real addresses
;; ---------------------------------------------------------------------------

(deftest page-parent-must-be-a-real-section
  (is (:ok? (cs/validate (env {:type :page :key "johns-river" :slug "johns-river" :parent :activities}))))
  (is (not (:ok? (cs/validate (env {:type :page :key "johns-river" :slug "johns-river" :parent :nonsense}))))))

(deftest feature-slot-must-be-a-known-slot
  (is (:ok? (cs/validate (env {:type :feature :key "feature-one" :page-slug :home-worship :title "T"}))))
  (is (not (:ok? (cs/validate (env {:type :feature :key "feature-one" :page-slug :made-up :title "T"}))))))

(deftest envelope-requires-a-contract-version
  (is (not (:ok? (cs/validate {:items [an-event]}))))
  (is (not (:ok? (cs/validate {:mtz/contract 99 :items [an-event]})))))
