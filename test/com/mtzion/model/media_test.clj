(ns com.mtzion.model.media-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [com.mtzion.app.library :as library]
            [com.mtzion.app.site :as site]
            [com.mtzion.lib.ui :as ui]
            [com.mtzion.model.content :as content]
            [com.mtzion.model.media :as media]
            [com.mtzion.model.normalize :as norm]
            [com.mtzion.ui.sections :as sections]
            [com.mtzion.test-util :refer [with-temp-ctx]]))

(defn- add! [ctx id m]
  (media/record! ctx (merge {:id id :label id :category "photo"
                             :uploaded_at (norm/now-epoch)} m))
  id)

;; ---------------------------------------------------------------------------
;; The index
;; ---------------------------------------------------------------------------

(deftest recording-an-image-is-idempotent
  (with-temp-ctx [ctx]
    (add! ctx "abc" {:label "First"})
    (add! ctx "abc" {:label "Second attempt"})
    (is (= 1 (media/total ctx)))
    (is (= "First" (:label (media/get-one ctx "abc")))
        "re-syncing must never clobber a label somebody edited")))

(deftest images-filter-by-album-kind-and-label
  (with-temp-ctx [ctx]
    (add! ctx "a" {:label "VBS opening"  :album "vbs-2026"})
    (add! ctx "b" {:label "VBS crafts"   :album "vbs-2026"})
    (add! ctx "c" {:label "Fall banner"  :album "fall-2026" :category "graphic"})
    (add! ctx "d" {:label "Loose photo"})

    (is (= 4 (media/total ctx)))
    (is (= 2 (count (media/ls ctx {:album "vbs-2026"}))))
    (is (= ["d"] (mapv :id (media/ls ctx {:album :none})))
        "images with no album are findable as a group")
    (is (= ["c"] (mapv :id (media/ls ctx {:category "graphic"}))))
    (is (= 2 (count (media/ls ctx {:q "vbs"}))) "label search is case-insensitive")
    (is (= ["a"] (mapv :id (media/ls ctx {:album "vbs-2026" :q "opening"}))))))

(deftest albums-report-their-size
  (with-temp-ctx [ctx]
    (add! ctx "a" {:album "vbs-2026"})
    (add! ctx "b" {:album "vbs-2026"})
    (add! ctx "c" {:album "fall-2026"})
    (add! ctx "d" {})
    (is (= [{:album "fall-2026" :n 1} {:album "vbs-2026" :n 2}]
           (mapv #(select-keys % [:album :n]) (media/albums ctx))))
    (is (= 1 (media/unfiled-count ctx)))))

(deftest saving-keeps-the-facts-and-changes-the-rest
  (with-temp-ctx [ctx]
    (add! ctx "a" {:label "IMG_0042.jpg" :width 1600 :height 1200})
    (media/save! ctx "a" {:label "Rally Day" :album "rally-2026"
                          :category "photo" :alt_text "Children on the lawn"
                          :taken_on (norm/local-date->epoch "2026-09-13")})
    (let [i (media/get-one ctx "a")]
      (is (= "Rally Day" (:label i)))
      (is (= "rally-2026" (:album i)))
      (is (= "Children on the lawn" (:alt_text i)))
      (is (= 1600 (:width i)) "dimensions are facts about the file, not editable"))))

(deftest an-emptied-album-becomes-nil-not-empty-string
  (testing "otherwise \"\" would show up as an album of its own in the rail"
    (with-temp-ctx [ctx]
      (add! ctx "a" {:album "vbs-2026"})
      (media/save! ctx "a" {:label "x" :album "  " :category "photo"})
      (is (nil? (:album (media/get-one ctx "a"))))
      (is (empty? (media/albums ctx)))
      (is (= 1 (media/unfiled-count ctx))))))

;; ---------------------------------------------------------------------------
;; Galleries
;; ---------------------------------------------------------------------------

(deftest a-section-that-names-an-album-renders-every-photo-in-it
  (with-temp-ctx [ctx]
    (add! ctx "a" {:album "vbs-2026" :alt_text "Opening day"})
    (add! ctx "b" {:album "vbs-2026"})
    (add! ctx "c" {:album "other"})
    (let [html (str (:body (ui/page
                            "t" (sections/section ctx {:title "Vacation Bible School"
                                                       :album "vbs-2026"} 0))))]
      (is (str/includes? html "mtz-gallery"))
      (is (= 2 (count (re-seq #"imagedelivery\.net" html)))
          "both photos in the album, and only those")
      (is (str/includes? html "Opening day") "alt text comes from the image"))))

(deftest a-section-with-no-album-is-unchanged
  (with-temp-ctx [ctx]
    (let [html (str (:body (ui/page
                            "t" (sections/section ctx {:title "Ordinary"
                                                       :body "<p>Words</p>"} 0))))]
      (is (not (str/includes? html "mtz-gallery")))
      (is (str/includes? html "Ordinary")))))

(deftest an-empty-album-renders-nothing-rather-than-an-empty-grid
  (with-temp-ctx [ctx]
    (is (nil? (sections/gallery ctx "nothing-here")))))

(deftest the-album-survives-a-save-through-the-site-pane
  (with-temp-ctx [ctx]
    (add! ctx "a" {:album "vbs-2026"})
    (let [id (last (str/split (get-in (site/add-row (assoc ctx :path-params
                                                           {:page "worship" :section "sections"}))
                                      [:headers "location"]) #"/"))]
      (site/save (assoc ctx :params {:title "VBS photos" :album "vbs-2026"}
                        :path-params {:page "worship" :section "sections" :id id}))
      (is (= "vbs-2026" (:album (content/get-one ctx :feature id)))))))

;; ---------------------------------------------------------------------------
;; The pane
;; ---------------------------------------------------------------------------

(deftest the-library-lists-albums-and-images
  (with-temp-ctx [ctx]
    (add! ctx "a" {:label "VBS opening" :album "vbs-2026"})
    (add! ctx "b" {:label "Loose photo"})
    (let [html (str (:body (library/media (assoc ctx :query-params {}))))]
      (is (str/includes? html "vbs-2026"))
      (is (str/includes? html "Not in an album"))
      (is (str/includes? html "VBS opening"))
      (is (str/includes? html "con-tile")))))

(deftest an-empty-library-offers-to-index-cloudflare
  (with-temp-ctx [ctx]
    (let [html (str (:body (library/media (assoc ctx :query-params {}))))]
      (is (str/includes? html "No images yet"))
      (is (str/includes? html "Index what is already in Cloudflare")))))

(deftest opening-an-image-shows-its-id-to-copy
  (with-temp-ctx [ctx]
    (add! ctx "abc123" {:label "Rally Day"})
    (let [html (str (:body (library/media (assoc ctx :path-params {:id "abc123"}
                                                 :query-params {}))))]
      (is (str/includes? html "Cloudflare image ID"))
      (is (str/includes? html "abc123")))))

(deftest an-unknown-image-is-a-404
  (with-temp-ctx [ctx]
    (is (= 404 (:status (library/media (assoc ctx :path-params {:id "nope"}
                                              :query-params {})))))))

(deftest saving-from-the-pane-files-the-image
  (with-temp-ctx [ctx]
    (add! ctx "a" {:label "IMG_1.jpg"})
    (library/save (assoc ctx :path-params {:id "a"}
                         :query-params {}
                         :params {:label "Rally Day" :album "rally-2026"
                                  :category "photo" :alt_text ""
                                  :taken_on "2026-09-13"}))
    (let [i (media/get-one ctx "a")]
      (is (= "Rally Day" (:label i)))
      (is (= "rally-2026" (:album i)))
      (is (= (norm/local-date->epoch "2026-09-13") (:taken_on i))))))
