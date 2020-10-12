(ns app.features.teleport
  (:require [app.world-interop :as wo]
            [gamebase.layers :as layers]
            [gamebase.ecs :as ecs]
            [app.hooks :as hooks]))

(declare when-tile-set when-tile-deleted when-key-hit)
(defn initialize []
  (hooks/subscribe-for-hook "tile-set" #'when-tile-set)
  (hooks/subscribe-for-hook "tile-deleted" #'when-tile-deleted)
  (hooks/subscribe-for-hook "key-hit" #'when-key-hit))

(def TELEPORTS {[:kafelki 60] :green
                [:kafelki 100] :yellow
                [:kafelki 101] :red
                [:kafelki 102] :blue})

(def ROOT-KEY :features.teleport)

(defn get-state [world]
  (ROOT-KEY world))

(defn get-state! []
  (ROOT-KEY (wo/get-world!)))

(defn set-state [world new-state]
  (assoc world ROOT-KEY new-state))

(defn set-state! [new-state]
  (let [world (wo/get-world!)]
    (wo/set-world! (assoc world ROOT-KEY new-state))))

(defn teleport-compare [[x1 y1] [x2 y2]]
  (let [cy (compare y1 y2)]
    (if (= 0 cy)
      (compare x1 x2)
      cy)))

(defn find-teleport [world start-x start-y color]
  (let [state (get-state!)
        teleports (or (get-in state [:teleports color]) [])]
    (or
     (->> teleports
          (drop-while #(<= (teleport-compare % [start-x start-y]) 0))
          (first))
     (first teleports))))

(defn teleport-ludzik-to! [x y]
  (wo/put-event!
   (assoc (ecs/mk-event (ecs/to-entity (wo/get-ludzik (wo/get-world!)))
                        :app.ecs.ludzik/teleport-to (wo/get-time!))
          :x x
          :y y)))

(defn build-teleport [world tile-x tile-y color]
  (let [state (get-state world)
        teleports (or (get-in state [:teleports color]) [])
        teleport-set (apply hash-set teleports)]
    (if (contains? teleport-set [tile-x tile-y])
      world
      (let [teleports' (->> (conj teleport-set [tile-x tile-y])
                            (sort-by identity teleport-compare)
                            (apply vector))
            state' (assoc-in state [:teleports color] teleports')]
        (set-state world state')))))
(defn demolish-teleport [world tile-x tile-y color]
  (let [state (get-state world)
        teleports (or (get-in state [:teleports color]) [])
        teleport-set (apply hash-set teleports)]
    (if (contains? teleport-set [tile-x tile-y])
      (let [teleports' (->> teleports
                            (remove #(= % [tile-x tile-y]))
                            (apply vector))
            state' (assoc-in state [:teleports color] teleports')]
        (set-state world state'))
      world)))

(defn demolish-teleport! [tile-x tile-y color]
  (wo/set-world! (demolish-teleport (wo/get-world!) tile-x tile-y color)))

(defn when-tile-set [world tile-x tile-y old-tile new-tile]
  (let [world'(if-let [color (TELEPORTS old-tile)]
                (demolish-teleport world tile-x tile-y color)
                world)
        world'' (if-let [color (TELEPORTS new-tile)]
                  (build-teleport world' tile-x tile-y color)
                  world')]
    world''))

(defn when-tile-deleted [world tile-x tile-y old-tile]
  (if-let [color (TELEPORTS old-tile)]
    (demolish-teleport world tile-x tile-y color)
    world))

(defn when-key-hit [key]
  (when (= key "x")
    (let [world (wo/get-world!)
          {:keys [tile-x tile-y]} (wo/get-ludzik world)
          tile (wo/get-tile world :foreground tile-x tile-y)]
      (when-let [color (TELEPORTS tile)]
        (let [[t-x t-y] (find-teleport world tile-x tile-y color)]
          (teleport-ludzik-to! t-x t-y)
          true)))))
