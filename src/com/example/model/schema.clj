(ns com.example.model.schema)

(def columns
  {:user/id {:type :uuid :primary-key true}
   :user/email {:type :text :required true}
   :user/joined-at {:type :inst :required true}})

(def extra-sql
  ["CREATE UNIQUE INDEX IF NOT EXISTS idx_user_email ON user(email);"])

(def module
  {:biff.sqlite/columns columns
   :biff.sqlite/extra-sql extra-sql})
