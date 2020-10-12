(ns app.hooks)

(declare hook-map mk-hook-struct)

(defn create-regular-hook [name & [options]]
  (let [hook-struct (mk-hook-struct name :regular options)]
    (swap! hook-map #(assoc % name hook-struct))))

(defn create-threaded-hook [name & [options]]
  (let [hook-struct (mk-hook-struct name :threaded options)]
    (swap! hook-map #(assoc % name hook-struct))))

(defn trigger-regular-hook [hook-struct & args]
  (let [{:keys [handlers]} hook-struct]
    (doseq [handler handlers]
      (apply handler args))
    nil))

(defn trigger-threaded-hook [hook-struct data & args]
  (let [{:keys [handlers]} hook-struct]
    (reduce
     (fn [acc handler] (apply handler acc args))
     data
     handlers)))


(defn trigger-hook [name & args]
  (when-let [hook-struct (@hook-map name)]
    (let [{:keys [type handlers]} hook-struct]
      (case type
        :regular (apply trigger-regular-hook hook-struct args)
        :threaded (apply trigger-threaded-hook hook-struct args)))))

(defn subscribe-for-hook [hook-name handler-fn]
 (if-let [hook-struct (@hook-map hook-name)]
   (let [{:keys [handlers]} hook-struct
         handlers' (conj handlers handler-fn)
         hook-struct' (assoc hook-struct :handlers handlers')]
     (swap! hook-map #(assoc % hook-name hook-struct')))
   (println (str "UNKNOWN HOOK: " hook-name))))

(def hook-map (atom {}))

(defn mk-hook-struct [name type options]
  {:name name
   :type type
   :handlers []})


