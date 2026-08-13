(ns com.mtzion.model.church
  "Facts about the congregation that appear in more than one place.

  These lived as literals scattered across landing.clj, about.clj and
  home_sections.clj, and had drifted: the site simultaneously claimed the church
  was founded in 1755 and in 1858. One definition means that cannot happen again.

  This namespace is the stepping stone to a `setting` table — when that lands,
  these read from the database and the call sites do not change."
  (:require [com.mtzion.model.normalize :as norm]))

(def founded-year
  "Confirmed 2026-08 — the congregation dates to 1755. Do not change without
  asking; several pages quote it."
  1755)

(defn years-since-founding
  "Computed rather than written down, so it does not silently go stale each
  January the way the hardcoded \"168 Years\" did."
  []
  (- (.getYear (java.time.LocalDate/now norm/eastern)) founded-year))

(defn approx-years-since-founding
  "Floored to the nearest decade, for prose like \"more than 270 years\" where an
  exact figure reads oddly. Flooring keeps the claim true."
  []
  (* 10 (quot (years-since-founding) 10)))
