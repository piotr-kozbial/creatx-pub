(ns app.ecs.flauto
  (:require
   [gamebase.ecs :as ecs]
   [gamebase.ecsu]
   [gamebase.systems.drawing :as sys-drawing]
   [gamebase.event-queue :as eq]
   [app.tiles :as tiles]
   [gamebase.layers :as layers]
   [app.world-interop :as wo]
   [app.state :as st]
   [app.debug-component :as dbgc]
   [com.rpl.specter :refer [transform]]
   [app.scratch :as scratch]) 
  (:require-macros [gamebase.ecsu :as ecsu]))

(def KEY-LEFT 65)
(def KEY-RIGHT 68)
(def KEY-UP 87)
(def KEY-DOWN 83)
;;(def KEY-FLIGHT )


(def WHEEL-DISTANCE (* 3 32))
(def WHEEL-OFFSET 16)
(def WHEEL-RADIUS 16)

(defn mk-entity [id tile-x tile-y]
  (let [entity
        (ecsu/mk-entity
         id
         ::flauto
         {:img (ecsu/mk-component sys-drawing/mk-static-image-component
                                  {:point-kvs [:position]
                                   :angle-kvs [:angle]
                                   :center [16 96]
                                   :resource-name-kvs [:image]})}
         :image "flauto-r.png"
         :angle 0

         :tile-x tile-x
         :tile-y tile-y

         :left-wheel-x WHEEL-OFFSET ;; relative to tile bottom left corner
         :left-wheel-y WHEEL-RADIUS
         :right-wheel-x (+ WHEEL-OFFSET WHEEL-DISTANCE)
         :right-wheel-y WHEEL-RADIUS


         )]
    entity))


(defn update-position [{:as entity :keys [tile-x tile-y
                                          left-wheel-x left-wheel-y
                                          right-wheel-x right-wheel-y]}]
  (let [x (-> tile-x (* 32) (+ left-wheel-x) (- WHEEL-OFFSET))
        y (-> tile-y (* 32) (+ left-wheel-y) (- WHEEL-RADIUS))]
    (assoc entity :position [x y])))




(def ACTIVE-RADIUS 5)


;; tu chyba chodzi o to, czy kratka jest w zasiegu, tzn. w poblizu ludzika,
;; zeby mogl tam budowac
(defn is-tile-active? [this tile-x tile-y]
  (let [our-tile-x (:tile-x this)
        our-tile-y (:tile-y this)]

    ;; (or
    ;;  (and (= tile-x (dec our-tile-x))
    ;;       (<= (- our-tile-y 1) tile-y (+ our-tile-y 3)))
    ;;  (and (= tile-x (inc our-tile-x))
    ;;       (<= (- our-tile-y 1) tile-y (+ our-tile-y 3)))
    ;;  (and (= tile-x our-tile-x)
    ;;       (or (= tile-y (dec our-tile-y))
    ;;           (= tile-y (+ our-tile-y 3)))))

    (and
     (<= (- our-tile-x 5) tile-x (+ our-tile-x 5))
     (<= (- our-tile-y 5) tile-y (+ our-tile-y 7))
     (not (and
           (= our-tile-x tile-x)
           (<= our-tile-y tile-y (+ our-tile-y 2))

           ))

     )



    ))

;;;;;

(def SPEED-AS-TILE-TIME 300)

(defmethod ecs/handle-event [:to-entity ::flauto ::ecs/init]
  [world event this]
  (let [next-tile-time (+ (::eq/time event) SPEED-AS-TILE-TIME)]
    [(assoc
      this
      :start (::eq/time event)
      :next-tile-time next-tile-time
      :target-tile-x (+ 10 (:tile-x this))
      :target-tile-y (:tile-y this))
     (ecs/mk-event this :update next-tile-time)]))


;;      -1   0   1
;;     +---+---+---+
;;   3 |   |   |   |
;;     +---+---+---+
;;   2 |   | o |   |
;;     +---+---+---+
;;   1 |   | # |   |
;;     +---+---+---+
;;   0 |   | # |   |
;;     +---+---+---+
;;  -1 |   |   |   |
;;     +---+---+---+
;;  -2 |   |   |   |
;;     +---+---+---+
nil

(defn -prepare-tile-map [world tile-x tile-y]
  (let [layer (wo/get-layer world :foreground)
        tile-context (:tile-context world)]
    (->>
     (for [dx (range -1 2)
           dy (range -2 4)]
       [[dx dy] (layers/v2-get-tile-info-from-layer
                 tile-context layer
                 (+ tile-x dx) (+ tile-y dy))])
     (apply concat)
     (apply hash-map))))


(defn dooo [world body control?]

  (.ClearForces world)
  (let [velocity (.GetLinearVelocity body)
        vx (.get_x velocity)
        vy (.get_y velocity)
        v (Math/sqrt (+ (* vx vx) (* vy vy)))]

    (when (<= v 20.0)
      (cond



        (and control? (js/keyIsDown KEY-RIGHT)) (do
                                      (.ApplyLinearImpulse body (scratch/b2.b2Vec2. 30 0) (.GetWorldCenter body))
                                      :right)
        (and control? (js/keyIsDown KEY-LEFT)) (do
                                     (.ApplyLinearImpulse body (scratch/b2.b2Vec2. -30 0) (.GetWorldCenter body))
                                     :left)
        (and control? (js/keyIsDown KEY-UP)) (do
                                   (.ApplyLinearImpulse body (scratch/b2.b2Vec2. 0 10) (.GetWorldCenter body))
                                   :left)

        :else nil))))


(defmethod ecs/handle-event [:to-entity ::flauto :update]
  [world event {:keys [tile-x tile-y] :as this}]


  ;;(update-position this)

  #_(let [
        {:keys [world body]} @st/b2-state
        timeStep (/ 1.0 30.0)
        velocityIterations 6
        positionIterations 2
        d (dooo world body (= :flauto (:control @st/app-state)))
        _ (.Step world timeStep velocityIterations positionIterations)
        velocity (.GetLinearVelocity body)
        vx (.get_x velocity)
        position (.GetPosition body)
        angle (.GetAngle body)
        ]
   (assoc this
           :position [(int (* 32 (.get_x position)))
                      (int (* 32 (.get_y position)))]
           :angle angle
           :image (cond
                    (and (= d :left) (< vx 0)) "flauto-l.png"
                    (and (= d :right) (> vx 0)) "flauto-r.png"
                    :default (:image this)))
))

#_(defmethod ecs/handle-event [:to-entity ::flauto :toggle-flight-mode]
  [_ _ this]
  (if (:flight-mode this)
       (assoc this
              :flight-mode false
              :flying? false)
       (assoc this
              :flight-mode true)))

