(ns app.ecs.ludzik
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

   [app.ecs.generic-ludzik :refer [handle-event-update conj-if disj-if] ]
   
   [app.ecs.move-logic :as ml]

   [app.identity :refer [get-user]]
   ) 
  (:require-macros [gamebase.ecsu :as ecsu]
                   [app.lang :refer [def+ defn+]]
                   ))


(def ACTIVE-RADIUS 5)

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

(defn mk-entity [id tile-x tile-y]
  (let [user (get-user)
        entity
        (ecsu/mk-entity
         id
         ::ludzik
         {:img (ecsu/mk-component sys-drawing/mk-static-image-component
                                  {:point-kvs [:position]
                                   :angle-kvs [:angle]
                                   :center [16 (case user "x" 96 "y" 64)]
                                   :resource-name-kvs [:image]})}
         :tile-x tile-x
         :tile-y tile-y ;; 11

         :height (case user "x" 3 "y" 2)


         :direction :stop ;; :stop :right :left
         :facing :right ;; :left :right
         :flight-mode false
         :flying? false
         :v-direction :stop ;; :stop :up :down

         :image (if (= (get-user) "x") "y/ludzik-r.png" "o/ludzik-r.png")

         :image-map {
                     [:right false false]  (str user "/ludzik-r.png")
                     [:right true false]   (str user "/ludzik-r-flying.png")
                     [:right false true]   (str user "/ludzik-r-speed.png")
                     [:right true true]    (str user "/ludzik-r-flying-speed.png")

                     [:left false false]   (str user "/ludzik-l.png")
                     [:left true false]    (str user "/ludzik-l-flying.png")
                     [:left false true]    (str user "/ludzik-l-speed.png")
                     [:left true true]     (str user "/ludzik-l-flying-speed.png")
                     }

         :angle 0)]



    entity
    ))

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

#_(defn speed-as-tile-time [this]
  (if (-> this :speed-mode) 100 300))

(defmethod ecs/handle-event [:to-entity ::ludzik ::ecs/init]
  [world event this]
  this)

(defn get-controls [this]
  (if (= :ludzik (:control @st/app-state))
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

(defmethod ecs/handle-event [:to-entity ::ludzik :update]
  [world event this]
  (let [controls (get-controls this)]
    (expire-flags)
    (handle-event-update world event this controls)))

(defmethod ecs/handle-event [:to-entity ::ludzik :toggle-flight-mode]
  [_ _ this]
  (if (:flight-mode this)
    (assoc this
           :flight-mode false
           :flying? false)
    (assoc this
           :flight-mode true)))

(defmethod ecs/handle-event [:to-entity ::ludzik :toggle-speed-mode]
  [_ _ this]
  (if (:speed-mode this)
    (assoc this
           :speed-mode false)
    (assoc this
           :speed-mode true)))

(defmethod ecs/handle-event [:to-entity ::ludzik ::teleport-to]
  [world {:keys [x y]} this]
  (assoc this :tile-x x :tile-y y))

