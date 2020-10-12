(ns app.ecs.generic-ludzik
  (:require
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

   [app.ecs.move-logic :as ml]) 
  (:require-macros [gamebase.ecsu :as ecsu]
                   [app.lang :refer [def+ defn+]]
                   ))


  (defn conj-if [set condition key]
    (if condition
      (conj set key)
      set))

  (defn disj-if [set condition key]
    (if condition
      (disj set key)
      set))

(defn -prepare-tile-map [world this tile-x tile-y]
  (let [HEIGHT (or (:height this) 3)
        {:keys [facing]} this ;; :left :right
        dx (case facing :left -1 :right 1)
        layer (wo/get-layer world :foreground)
        tile-context (:tile-context world)
        get-tile (fn [x y]
                   (layers/v2-get-tile-info-from-layer
                    tile-context layer x y))]
    (assoc
     (->>
      (for [dy (range -1 (inc HEIGHT))]
        [dy (get-tile (+ tile-x dx) (+ tile-y dy))])
      (mapcat identity)
      (apply hash-map))
     :above (get-tile tile-x (+ tile-y HEIGHT))
     :under (get-tile tile-x (+ tile-y -1)))))

(defn speed-as-tile-time [this]
  (if (-> this :speed-mode) 100 300))

(defn get-next-facing [{:as this :keys [facing]} move]
  (if (= move :turn)
    (case facing :right :left, :left :right)
    facing))

(defn get-next-tile [{:as this :keys [facing tile-x tile-y]} move]
  (case move
    :stay [tile-x tile-y]
    :turn [tile-x tile-y]
    :up [tile-x (inc tile-y)]
    :down [tile-x (dec tile-y)]
    :fwd (case facing :left [(dec tile-x) tile-y] :right [(inc tile-x) tile-y])
    :fwd-up (case facing :left [(dec tile-x) (inc tile-y)]
                  :right [(inc tile-x) (inc tile-y)])
    :fwd-down (case facing :left [(dec tile-x) (dec tile-y)]
                    :right [(inc tile-x) (dec tile-y)])))

(defn image-position [{:as this :keys [tile-x tile-y
                                       next-tile-x next-tile-y
                                       move-start-time
                                       next-tile-time]}
                      time]


  (let [pos

        (let [[start-x start-y :as start-position] [(+ (* 32 tile-x) 16) (* 32 tile-y)]]
          (if next-tile-time
            (let [[end-x end-y :as end-position] [(+ (* 32 next-tile-x) 16) (* 32 next-tile-y)]]
              (if (>= (- time move-start-time) (- next-tile-time time)) ;; we're half there
                [(int (* 0.5 (+ start-x end-x))) (int (* 0.5 (+ start-y end-y)))]
                start-position))
            start-position))]
  ;; (when (= (::ecs/entity-id this) "villager-0")
  ;;   (js/console.log (str "image position for villager 0: " (pr-str pos))))

  pos

    ))

(defn select-image [this]
  (if-let [image-map (:image-map this)]
    (image-map [(:facing this) (boolean (:flight-mode this)) (boolean (:speed-mode this))])
    (case (-> this :facing)
      :left (if (-> this :flight-mode)
              (if (-> this :speed-mode)
                "ludzik-l-flying-speed.png"
                "ludzik-l-flying.png")
              (if (-> this :speed-mode)
                "ludzik-l-speed.png"
                "ludzik-l.png"))
      :right (if (-> this :flight-mode)
               (if (-> this :speed-mode)
                 "ludzik-r-flying-speed.png"
                 "ludzik-r-flying.png")
               (if (-> this :speed-mode)
                 "ludzik-r-speed.png"
                 "ludzik-r.png")))))


(defn handle-event-update [world event this controls]

  ;; (when (= (::ecs/entity-id this) "villager-0")
  ;;   (js/console.log "generic updating villager 0"))

  (let [[this' time-for-control]
        (if (:next-tile-time this)
          (if (>= (::eq/time event) (:next-tile-time this))
            [(assoc this
                    :move-start-time nil
                    :next-tile-time nil
                    :next-tile-x nil
                    :next-tile-y nil
                    :tile-x (:next-tile-x this)
                    :tile-y (:next-tile-y this))
             true]
            [this
             false])
          [this true])

        this''
        (if time-for-control
          (let [tile-map' (-prepare-tile-map world this' (:tile-x this') (:tile-y this'))
                properties {:height (or (:height this') 3)
                            :flight-mode (:flight-mode this')}
                supported (ml/produce-supported properties tile-map')
                possible (ml/remove-blocked properties tile-map' supported)
                intended (ml/produce-intended controls)
                intended-possible (filter possible intended)
                best-intended-possible (first intended-possible)
                actual-move (if best-intended-possible
                              best-intended-possible
                              (first (filter possible ml/all-moves-in-preference-order)))
                [next-tile-x next-tile-y] (get-next-tile this' actual-move)
                next-tile-time (+ (::eq/time event) (speed-as-tile-time this'))
                next-facing (get-next-facing this' actual-move)]

            (assoc this'
                   :next-tile-x next-tile-x
                   :next-tile-y next-tile-y
                   :move-start-time (::eq/time event)
                   :next-tile-time (when (not= actual-move :stay) next-tile-time)
                   :facing next-facing))
          this')]

    (assoc this''
           :position (image-position this' (::eq/time event))
           :image (select-image this'))))

