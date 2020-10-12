(ns app.world-interop
  (:refer-clojure :exclude [run!])
  (:require
   [gamebase.ecs :as ecs]
   [gamebase-ecs.virtual-timer :as vt]
   [gamebase.event-queue :as eq]
   [gamebase.layers :as layers]
   [app.state :refer [app-state]]
   [app.hooks :as hooks]
   [app.tiles :as tiles]))

;; Functions working on world structure, either pure or working directly
;; on the :world in the current state (app.state/app-state).
;;
;; In scope are generic operations (such as get-tile) or operations of
;; basic game logic (such as get-ludzik).
;; More specific game logic, such as teleportation or tnt, should be
;; in their separate modules.
nil

;;;;; Pure functions

(defn initialize []
  (hooks/create-threaded-hook "tile-set")
  (hooks/create-threaded-hook "tile-deleted"))

(defn get-layer [world layer-key]
  (->> (:layers world)
       (filter #(= (first %) layer-key))
       (first)
       (second)))
(defn set-layer [world layer-key new-layer]
  (let [layers'
        (->> (:layers world)
             (map (fn [[layer-k layer]]
                    [layer-k (if (= layer-k layer-key) new-layer layer)]))
             (apply vector))]
    (assoc world :layers layers')))

(defn get-tile [world layer-key tile-x tile-y]
  (let [layer (get-layer world layer-key)
        width (layers/v2-get-layer-width layer)
        height (layers/v2-get-layer-height layer)]
    (layers/v2-get-tile-from-layer layer tile-x tile-y)))

;; no hooks called (to be used from *within* hooks)
(defn edit-set-tile [world layer-key tile-x tile-y tileset-id tile-number]
  (let [old-tile (get-tile world layer-key tile-x tile-y)
        new-tile [tileset-id tile-number]
        world'  (let [layer (get-layer world layer-key)
                      width (layers/v2-get-layer-width layer)
                      height (layers/v2-get-layer-height layer)
                      layer'
                      ,   (if (and (<= 1 tile-x (- width 2))
                                   (<= 1 tile-y (- height 2)))
                            (layers/v2-set-tile-in-layer layer tile-x tile-y new-tile)
                            layer)]
                  (set-layer world layer-key layer'))]
    world'))

(defn set-tile [world layer-key tile-x tile-y tileset-id tile-number]
  (let [old-tile (get-tile world layer-key tile-x tile-y)
        new-tile [tileset-id tile-number]
        world'  (let [layer (get-layer world layer-key)
                      width (layers/v2-get-layer-width layer)
                      height (layers/v2-get-layer-height layer)
                      layer'
                      ,   (if (and (<= 1 tile-x (- width 2))
                                   (<= 1 tile-y (- height 2)))
                            (layers/v2-set-tile-in-layer layer tile-x tile-y new-tile)
                            layer)]
                  (set-layer world layer-key layer'))]
    (hooks/trigger-hook "tile-set" world' tile-x tile-y old-tile new-tile)))

(defn del-tile [world layer-key tile-x tile-y]
  (let [layer (get-layer world layer-key)
        width (layers/v2-get-layer-width layer)
        height (layers/v2-get-layer-height layer)
        layer'
        ,   (if (and (<= 1 tile-x (- width 2))
                     (<= 1 tile-y (- height 2)))
              (layers/v2-set-tile-in-layer layer tile-x tile-y nil)
              layer)
        old-tile (get-tile world layer-key tile-x tile-y)
        world' (set-layer world layer-key layer')]
    (hooks/trigger-hook "tile-deleted" world' tile-x tile-y old-tile)))

;; no hooks called (to be used from *within* hooks)
(defn edit-del-tile [world layer-key tile-x tile-y]
  (let [layer (get-layer world layer-key)
        width (layers/v2-get-layer-width layer)
        height (layers/v2-get-layer-height layer)
        layer'
        ,   (if (and (<= 1 tile-x (- width 2))
                     (<= 1 tile-y (- height 2)))
              (layers/v2-set-tile-in-layer layer tile-x tile-y nil)
              layer)
        old-tile (get-tile world layer-key tile-x tile-y)
        world' (set-layer world layer-key layer')]
    world'))


(defn get-ludzik [world]
  (first
   (filter
    #(= (::ecs/type %) :app.ecs.ludzik/ludzik)
    (vals (::ecs/entities world)))))

;;;;; Functions working on @app-state

(defn get-world! []
  (:world @app-state))
(defn set-world! [world]
  (swap! app-state assoc :world world))
(defn destroy-world! []
  (swap! app-state assoc :world nil))

(defn run! []
  (let [{:keys [world timer]} @app-state]
    (assert world)
    (swap! app-state update-in [:timer] #(vt/run % (::ecs/time world)))))
(defn stop! []
 (let [{:keys [world timer]} @app-state]
    (assert world)
    (swap! app-state update-in [:timer] vt/stop)))
(defn running?! []
  (let [{:keys [world timer]} @app-state]
    (assert world)
    (vt/running? timer)))

(defn get-time! []
  "Returns virtual time if the world is running, world time otherwise."
  (let [{:keys [world timer]} @app-state]
    (assert world)
    (if (vt/running? timer)
      (vt/get-time timer)
      (::ecs/time world))))


(defn get-tile! [layer-key tile-x tile-y]
  (get-tile (get-world!) layer-key tile-x tile-y))

(defn get-entity [world entity-id]
  (ecs/get-entity-by-key world entity-id))
(defn get-entity! [world entity-id]
  (get-entity (get-world!) entity-id))



(defn set-tile! [layer-key tile-x tile-y tileset-id tile-number]
  (set-world! (set-tile (get-world!) layer-key tile-x tile-y tileset-id tile-number))

)
(defn del-tile! [layer-key tile-x tile-y]
  (let [old-tile (get-tile! layer-key tile-x tile-y)]
    (swap! app-state update-in [:world] #(del-tile % layer-key tile-x tile-y))
    ;;(hooks/trigger-hook "tile-deleted" tile-x tile-y old-tile)
    ))

(defn put-event! [e]
  (swap! app-state
         (fn [{:keys [world] :as state}]
           (assoc state
                  :world
                  (ecs/put-all-events world [e])))))

(defn inject-entity! [e]
  (swap! app-state
         (fn [{:keys [world] :as state}]
           (assoc
            state
            :world
            (ecs/insert-object world e))))
  nil)
(defn kill-entity! [entity-key]
  (swap! app-state
         (fn [{:keys [world] :as state}]
           (assoc
            state
            :world
            (ecs/remove-entity-by-key world entity-key))))
  nil)

(defn select-tool! [tool]
  (swap! app-state assoc :selected-tool tool))


;;;; Control

(defn add-controllable [world target img-resource]
  (let [controllable (or (:controllables world) {:ludzik "ludzik-r.png"})]
    (assoc world :controllables (assoc controllable target img-resource))))

(defn del-controllable [world target]
  (let [controllable (or (:controllables world) #{})]
    (assoc world :controllables (dissoc controllable target))))

(defn get-controllables [world]
  (or (:controllables world) {:ludzik "ludzik-r.png"}))

(defn set-control [target] ;; :ludzik or :flauto
  (swap! app-state assoc :control target))

