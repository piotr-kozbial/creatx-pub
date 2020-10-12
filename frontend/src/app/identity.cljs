(ns app.identity)

(def -user (atom nil))

(defn set-user [user]
  (reset! -user user))

(defn get-user []
  @-user)
