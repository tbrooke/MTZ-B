(ns com.mtzion.model.outreach
  "The outreach partners, as data.

  Static on purpose. These are five long-standing relationships with real
  organisations, not weekly content — they change about as often as the church's
  address does, and putting them in the CMS would mean a console pane nobody
  opens guarding rows nobody edits.

  One vector, three readers: the home page shows the featured ones, /outreach
  lists them all, and ui.nav builds the Outreach submenu from the same slugs. A
  partner added here appears in all three without touching any of them.

  Facts and links were checked against each organisation's own site in September
  2026. Anything not verifiable there is simply absent rather than guessed —
  these are other people's charities and getting their details wrong in print is
  worse than saying less.")

(def partners
  [{:slug     "rowan-helping-ministries"
    :name     "Rowan Helping Ministries"
    :note     "Food, shelter & crisis help"
    :where    "Salisbury"
    :url      "https://rowanhelpingministries.org/"
    :summary  (str "Food, shelter and crisis assistance for neighbours across "
                   "Rowan County.")
    :body     [(str "Rowan Helping Ministries serves people in Rowan County who "
                    "are in crisis, providing essential needs and helping them "
                    "break the cycle. It continues a ministry begun in 1967 by "
                    "several of Salisbury's downtown churches.")
               (str "Their work covers a homeless shelter, a food pantry, a "
                    "clothing centre, transitional and permanent supportive "
                    "housing, and a Crisis Assistance Network that helps with "
                    "utility disconnections, eviction notices, heating fuel and "
                    "medication.")
               (str "Jeannie's Kitchen, their community kitchen, serves three "
                    "meals a day to shelter guests and a midday meal open to "
                    "anyone in need — offered without qualification. The "
                    "organisation relies on 50 to 60 volunteers every day.")]
    :involve  "Mt. Zion volunteers help with the monthly food sort."}

   {:slug     "main-street-marketplace"
    :name     "Main Street Marketplace"
    :note     "Affordable groceries · China Grove"
    :where    "306 S. Main Street, China Grove"
    :url      "https://www.marketandmeeting.org/"
    :summary  (str "A non-profit market on Main Street selling fresh food on a "
                   "sliding scale.")
    :body     [(str "Main Street Marketplace and Meeting Place is a non-profit "
                    "market in downtown China Grove — the closest of our "
                    "partners, a few minutes from the church.")
               (str "It sells fresh produce, local meat and staple groceries on "
                    "a tier-based pricing scale, so neighbours can buy good food "
                    "at a price they can afford. Shoppers can save up to 40% "
                    "while supporting more than 45 local farmers and small "
                    "businesses, and a 400 square foot hydroponic garden grows "
                    "fresh greens year round.")
               (str "It grew out of Main Street Mission, started in 2003 to meet "
                    "immediate needs in southern Rowan County after the textile "
                    "mills closed. Since 2023 it has hosted the China Grove "
                    "Farmers Market, May through August, and its Meeting Place "
                    "runs classes and community resources.")]
    :involve  "Donations and volunteer shifts; the Farmers Market runs each summer."}

   {:slug     "meals-on-wheels"
    :name     "Meals on Wheels Rowan"
    :note     "Weekday meals for homebound seniors"
    :where    "Rowan County"
    :url      "https://www.mowrowan.org/"
    :summary  (str "Hot meals delivered each weekday to homebound seniors across "
                   "the county.")
    :body     [(str "Meals on Wheels Rowan delivers nourishing meals every "
                    "weekday to more than 300 homebound seniors across Rowan "
                    "County, supported by over 1,000 volunteers.")
               (str "A delivery is more than a meal. Volunteers offer a friendly "
                    "greeting and check for any sign of a safety concern or a "
                    "change in wellbeing — for some recipients it is the only "
                    "visit of the day. A grocery programme shops for and "
                    "delivers groceries as well.")
               (str "The programme serves Rowan County residents aged 60 and "
                    "over who cannot shop or prepare meals for themselves; "
                    "homebound disabled adults under 60 may also qualify.")]
    :involve  "Congregation members drive a delivery route. Referrals: 704-633-0352."}

   {:slug     "habitat-for-humanity"
    :name     "Habitat for Humanity"
    :note     "Building homes in Rowan County"
    :where    "1707 S. Main Street, Salisbury"
    :url      "https://www.habitatrowan.org/"
    :summary  "Building and improving homes alongside families who need them."
    :body     [(str "Habitat for Humanity of Rowan County builds and improves "
                    "homes in partnership with families who need a decent, "
                    "affordable place to live — four to six houses a year.")
               (str "Homes are built with the family, not merely for them. "
                    "Applicants must have lived in Rowan County for at least "
                    "twelve consecutive months.")
               (str "The Habitat ReStore on South Main Street in Salisbury sells "
                    "donated furniture, appliances and building materials to the "
                    "public at a fraction of retail, funding the building "
                    "programme and keeping usable material out of landfill.")]
    :involve  "Mt. Zion joins two builds a year. ReStore: (704) 642-1222."}

   {:slug     "south-side-church-of-god"
    :name     "South Side Church of God"
    :note     "Neighbourhood food pantry"
    :where    "China Grove"
    :url      nil
    :summary  "A neighbouring congregation running a food pantry in China Grove."
    :body     [(str "South Side Church of God is a neighbouring congregation here "
                    "in China Grove that runs a food pantry for the community.")
               (str "Mt. Zion supports their food drive — one of the ways two "
                    "congregations a few streets apart can do more together than "
                    "either would alone.")
               (str "Pantry hours vary. Contact the church directly, or the "
                    "Mt. Zion office, before making a donation run.")]
    :involve  "Periodic food drives — watch the bulletin for what is needed."}])

(defn path
  "Public URL for a partner page. Nested under /outreach so the URL says where
  it belongs and cannot collide with a top-level route."
  [{:keys [slug]}]
  (str "/outreach/" slug))

(defn by-slug [slug]
  (first (filter #(= slug (:slug %)) partners)))
