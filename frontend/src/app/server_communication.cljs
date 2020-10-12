(ns app.server-communication
  (:refer-clojure :exclude [get])
  (:require [cljs.reader :refer [read-string]]
            [ajax.core :as ajax]
            [app.state :as st]
            [app.utils.jscript :refer [pr-json]]
            [app.utils.jscript :as j]
            [app.identity :as identity]
            [cljs.pprint :refer [pprint]]))

(defn generic-error-handler [x]
  (.log js/console (str "ERROR in ajax to server: " x)))

(defn post [path body handler & [error-handler]]

  (ajax/POST
   path
   {:body (pr-str body) ;;(j/pr-json body)
    ;; (ajax/text-request-format)
    ;;:response-format :json
    :headers {"Content-Type" "text/plain; charset=UTF-8"}
    :keywords? true
    ;; :headers {"Access-Control-Allow-Origin" "*"}
    :handler (fn [ret]
               (handler ret))
    :error-handler (or error-handler generic-error-handler)}))


(defn compress-state [state]
  state
  #_(let [compress-layer-data
        (fn [layer-data]
          (->>
            (for [y (range (count layer-data))
                  x (range (count (first layer-data)))]
              [x y ((layer-data y) x)])
            (remove #(nil? (% 2)))
            (apply vector)))

        compress-layer-body
        (fn [layer-body]
          (assoc layer-body :data (compress-layer-data (:data layer-body))))

        compress-layer
        (fn [[layer-key layer-body]]
          [layer-key (compress-layer-body layer-body)])

        compress-layers
        (fn [layers]
          (map compress-layer layers))

        compress-world
        (fn [world] world

          (assoc world :layers (compress-layers (:layers world))))]

    (assoc state :world (compress-world (:world state)))))


(defn uncompress-state [compressed-state]
  compressed-state
  #_(let [uncompress-row
        (fn [row]
          ;; (.log js/console (str "UNCOMPRESS-ROW: " ;(pr-str row)))
          (let [width (inc (first (last row)))
                row-as-map (->> row
                                (mapcat (fn [[x _ v]] [x v]))
                                (apply hash-map))]
            (->> (range width)
                 (map row-as-map)
                 (apply vector))))

        uncompress-layer-data
        (fn [layer-data]
          (->> layer-data
               (sort-by (fn [[x y _]] (vector y x)))
               (partition-by (fn [[_ y _]] y))
               (map uncompress-row)
               (apply vector)))

        uncompress-layer-body
        (fn [layer-body]
          (assoc layer-body :data (uncompress-layer-data (:data layer-body))))

        uncompress-layer
        (fn [[layer-key layer-body]]
          [layer-key (uncompress-layer-body layer-body)])

        uncompress-layers
        (fn [layers]
          (apply vector (map uncompress-layer layers)))

        uncompress-world
        (fn [compressed-world]
          (assoc compressed-world :layers (uncompress-layers (:layers compressed-world))))]

    (assoc compressed-state :world (uncompress-world (:world compressed-state)))))


(defn save-game [handler & [error-handler]]
  (post ;;"/creatx/request"
        js/server_communications_url_path
        {:request :save-game
         :id (str (identity/get-user) "/1")
         :name "the only game"
         :state (compress-state @st/app-state)}
        handler
        error-handler))


#_(defn set-world [new-world]
  (swap! st/app-state assoc :world new-world))

(defn load-game [handler & [error-handler]]
  (post ;;"/creatx/request"
        js/server_communications_url_path
        {:request :load-game, :id (str (identity/get-user) "/1")}
        (fn [ret]
          (let [rret (read-string ret)]
            (if (:state rret)
              (let [
                    compressed-state (:state rret)
                    state (uncompress-state compressed-state)]
                (handler (assoc rret :state state)))
              (error-handler))))
        error-handler))

(comment ;; try it out







  (post "/request" {:request :list-games} #(.log js/console %))


;;   (post "/request" {:request :save-game
;;                     :name "the only game"
;;                     :state @app.state/app-state

;;                     }
;; )


  (save-game #(.log js/console %))

  )
