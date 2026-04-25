(ns com.example.model.schema)

(def columns
  {:user/id {:type :uuid :primary-key true}
   :user/email {:type :text :required true :unique true}
   :user/joined-at {:type :inst :required true}})

(def module
  {:biff.sqlite/columns columns})
