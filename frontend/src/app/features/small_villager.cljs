(ns app.features.small-villager
  (:require
   [app.hooks :as hooks]
   [app.world-interop :as wo]
   [app.tiles :refer [SMALL-VILLAGER]]

   [gamebase.ecs :as ecs]
   [gamebase.ecsu]
   [gamebase.events :as events]
   [gamebase.systems.drawing :as sys-drawing]
   [gamebase.event-queue :as eq]
   [app.tiles :as tiles]
   [gamebase.layers :as layers]
   [app.world-interop :as wo]
   [app.state :as st]
   [app.debug-component :as dbgc]
   [com.rpl.specter :refer [transform]]

   [app.ecs.move-logic :as ml]


   [app.ecs.generic-ludzik :refer [handle-event-update conj-if disj-if]]

   [app.overlay.overlay :refer [message!]]
   )

  (:require-macros [gamebase.ecsu :as ecsu]
                   [app.lang :refer [def+ defn+]]
                   )



  )

(declare when-tile-set when-tile-deleted)

(defn initialize []
  (hooks/subscribe-for-hook "tile-set" #'when-tile-set)
  (hooks/subscribe-for-hook "tile-deleted" #'when-tile-deleted)
  )

(def ROOT-KEY :features.small-villager)

(defn get-state [world]
  (ROOT-KEY world))
(defn get-state! []
  (ROOT-KEY (wo/get-world!)))
(defn set-state [world new-state]
  (assoc world ROOT-KEY new-state))
(defn set-state! [new-state]
  (let [world (wo/get-world!)]
    (wo/set-world! (assoc world ROOT-KEY new-state))))

(declare on-villager-created on-villager-deleted)

(defn find-villager-by-xy [world tile-x tile-y]
  (let [state (get-state world)
        villager-map (or (:villager-map state) {})
        coords-map (->> villager-map
                        (keys)
                        (map (fn [id] [id (wo/get-entity world id)]))
                        (mapcat (fn [[id {:keys [tile-x tile-y]}]] [[tile-x tile-y] id]))
                        (apply hash-map))]
    (coords-map [tile-x tile-y])))

(defn when-tile-set [world tile-x tile-y old-tile new-tile]
  (if (= new-tile SMALL-VILLAGER)
    (if (find-villager-by-xy world tile-x tile-y)
      world
      (on-villager-created world tile-x tile-y old-tile))
    (if-let [id (find-villager-by-xy world tile-x tile-y)]
      (on-villager-deleted world id)
      world)))

(defn when-tile-deleted [world tile-x tile-y old-tile]
  (if-let [id (find-villager-by-xy world tile-x tile-y)]
    (on-villager-deleted world id)
    world))


(def MAX-VILLAGER-COUNT 4)

;; Villager map: [tile-x tile-y] => { villager state - TODO }

;; (defn ensure-villager-map [world]
;;   (let [{:as state :keys [villager-map]} (get-state world)]
;;     (if villager-map
;;       world
;;       (set-state world (assoc state :villager-map {})))))
;; (defn ensure-villager-map! []
;;   (let [{:as state :keys [villager-map]} (get-state!)]
;;     (if-not villager-map
;;       (set-state! (assoc state :villager-map {})))))

(declare mk-entity)

(defn find-free-number [state]
  (let [villager-map (or (:villager-map state) {})
        existing-numbers (apply hash-set (map :number (vals villager-map)))]
    (->> (iterate inc 0)
         (remove existing-numbers)
         (first))))

(defn on-villager-created [world tile-x tile-y old-tile]
  (let [state (get-state world)
        villager-map (or (get-in state [:villager-map]) {})
        free-number (find-free-number state)]

    (let [world'
          (if (< (count villager-map) MAX-VILLAGER-COUNT)
            (-> world
                (set-state
                 (assoc state :villager-map
                        (assoc villager-map
                               (str "small-villager-" free-number)
                               {:number free-number
                                :id (str "small-villager-" free-number)})))
                (ecs/insert-object (mk-entity free-number tile-x tile-y))
                (wo/add-controllable [:small-villager free-number] "small-villager.png"))
            (do
              (message! "Too many small villagers!")
              world)
            )

          world'' (if old-tile
                    (apply wo/edit-set-tile world' :foreground tile-x tile-y old-tile)
                    (wo/edit-del-tile world' :foreground tile-x tile-y))]
      world'')))

(defn on-villager-deleted [world entity-id]
  (let [state (get-state world)
        villager-map (or (get-in state [:villager-map]) {})
        {:keys [number]} (villager-map entity-id)]
    (-> world
        (set-state (assoc state :villager-map
                          (dissoc villager-map entity-id)))
        (ecs/remove-entity-by-key entity-id)
        (wo/del-controllable [:small-villager number]))))


(do ;; entity

  (do ;; input handling
    
    (def KEY-LEFT 65)
    (def KEY-RIGHT 68)
    (def KEY-UP 87)
    (def KEY-DOWN 83)
    ;;(def KEY-FLIGHT )


    (def button-down (atom nil))


    (def left-flag (atom nil))
    (def right-flag (atom nil))
    (def up-flag (atom nil))
    (def down-flag (atom nil))
    (events/add-handler ;; setup key handler
     :canvas-key-typed
     (fn [{:keys [key x y]}]
       (cond
         (= key "a") (reset! left-flag (js/millis))
         (= key "d") (reset! right-flag (js/millis))
         (= key "w") (reset! up-flag (js/millis))
         (= key "s") (reset! down-flag (js/millis))
         :else nil)))

    (defn get-keys []
      {:k-left? (or (js/keyIsDown KEY-LEFT)
                    (= @button-down :left)
                    @left-flag)
       :k-right? (or (js/keyIsDown KEY-RIGHT)
                     (= @button-down :right)
                     @right-flag)
       :k-up?  (or (js/keyIsDown KEY-UP)
                   (= @button-down :up)
                   @up-flag)
       :k-down? (or (js/keyIsDown KEY-DOWN)
                    (= @button-down :down)
                    @down-flag)})
    (defn expire-flags []
      (let [current-millis (js/millis)]
        (doseq [flag [left-flag right-flag up-flag down-flag]]
          (reset! flag nil))))

    )

  (defn mk-entity [number tile-x tile-y]
    (let [id (str "small-villager-" number)
          entity
          (ecsu/mk-entity
           id
           :small-villager



           {:img (ecsu/mk-component sys-drawing/mk-static-image-component
                                    {:point-kvs [:position]
                                     :angle-kvs [:angle]
                                     :center [16 32]
                                     :resource-name-kvs [:image]})}
           :tile-x tile-x
           :tile-y tile-y ;; 11

           :number number
           :height 1
           :image-map {
                       [:right false false]   "small-villager.png"
                       [:right true false]  "small-villager.png"
                       [:right false true]  "small-villager.png"
                       [:right true true] "small-villager.png"

                       [:left false false]    "small-villager.png"
                       [:left true false]   "small-villager.png"
                       [:left false true]   "small-villager.png"
                       [:left true true]  "small-villager.png"
                       }


           :direction :stop ;; :stop :right :left
           :facing :right ;; :left :right
           :flight-mode false
           :flying? false
           :v-direction :stop ;; :stop :up :down

           :image "small-villager.png"
           :angle 0)]



      entity
      ))
  
  (defn get-controls [this]
    (if (= [:small-villager (:number this)] (:control @st/app-state))
      (let [{:keys [facing]} this ;; :left :right
            {:keys [k-left? k-right? k-up? k-down?]} (get-keys)]
        (-> #{}
            (conj-if k-up? :up)
            (conj-if k-down? :down)
            (conj-if (or (and (= facing :right) k-right?)
                         (and (= facing :left) k-left?)) :fwd)
            (conj-if (or (and (= facing :right) k-left?)
                         (and (= facing :left) k-right?)) :rev)))
      #{})) 

  (defmethod ecs/handle-event [:to-entity :small-villager ::ecs/init]
    [world event this]
    this)

  (defmethod ecs/handle-event [:to-entity :small-villager :update]
    [world event this]
    (let [controls (get-controls this)]
      (expire-flags)

      (handle-event-update world event this controls))))

