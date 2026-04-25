(ns com.example.model.user)

(defn session-user
  {:output [:session/user]}
  [{:keys [session]} _]
  (when-let [uid (:uid session)]
    {:session/user {:user/id uid}}))

(def module
  {:biff.graph/resolvers [#'session-user]})
