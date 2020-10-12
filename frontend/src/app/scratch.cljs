(ns app.scratch)

;; TODO - UWAGAszystko jest opisane w app.ecs.box2d-system, co ma byc.


;; Przykladowe strukturki definiujace ciala:

;; static body

;; {:type :static
;;  :position [x y]
;;  :fixtures [{:shape  {:type :box
;;                       :center [(/ w 2) (/ h 2)]
;;                       :half-size [(/ w 2) (/ h 2)]}}]}



;; ;; dynamic body

;; {:type :dynamic
;;  :position [x y]
;;  :fixtures [{:shape  {:type :box
;;                       :center [(/ w 2) (/ h 2)]
;;                       :half-size [(/ w 2) (/ h 2)]}
;;              :density d
;;              :friction f}]
;;  }


(def b2 (js/Box2D.))

(defn mk-shape [{:as shape-spec :keys [type]}]
  (case type
    :box (let [shape (b2.b2PolygonShape.)
               [cx cy] (:center shape-spec)
               [hw hh] (:half-size shape-spec)]
           (.SetAsBox shape hw hh (b2.b2Vec2. cx cy) 0.0)
           shape)
    :poly (let [;;shape (b2.b2PolygonShape.)
                points (:points shape-spec)
                xyzs (partition 2 points)
                ;;xyzs [[4 6] [4 10] [1 10]]
                vertices (->> xyzs
                              (map (fn [[x y]]
                                     (js/console.log (str "vertex " x " " y))
                                     (let [v (b2.b2Vec2. x y)]
                                       (js/console.log v)
                                       v)))
                              (apply array))]
            (js/console.log vertices)
            (js/createPolygonShape b2 vertices)
)))

(defn mk-fixture [body {:keys [shape density friction]}]
  (let [fixture-def (b2.b2FixtureDef.)]
    (.set_shape fixture-def (mk-shape shape))
    (when density
      (.set_density fixture-def density))
    (when friction
      (.set_friction fixture-def friction))
    (.CreateFixture body fixture-def)))

(defn mk-body [world {:keys [type position fixtures]}]
  (assert (#{:static :dynamic} type))
  (let [body-def (b2.b2BodyDef.)]
    ;; set type
    (when (= type :dynamic)
      (.set_type body-def b2.b2_dynamicBody))
    ;; set position
    (when position
      (assert (= (count position) 2))
      (let [[x y] position]
        (.Set (.get_position body-def) x y)))
    (let [body (.CreateBody world body-def)]
      (doseq [fixture-spec fixtures]
        (mk-fixture body fixture-spec))
      body)))

(defn mk-static-rectangle [world x y w h]
  (mk-body world {:type :static
                  :position [x y]
                  :fixtures [{:shape
                              {:type :box,
                               :center [(/ w 2) (/ h 2)]
                               :half-size [(/ w 2) (/ h 2)]}}]}))

(defn mk-dynamic-rectangle [world x y w h density friction]
  (mk-body world {:type :dynamic
                  :position [x y]
                  :fixtures [{:shape {:type :box
                                      :center [(/ w 2) (/ h 2)]
                                      :half-size [(/ w 2) (/ h 2)]}
                              :density density
                              :friction friction}]}))

(defn mk-b2-state []
  (let [gravity (b2.b2Vec2. 0.0 -30.0)
        world (b2.b2World. gravity)
        groundBody (mk-static-rectangle world 0 0 500 2)
        leftWallBody (mk-static-rectangle world 0 0 2 500)
        ;;blockBody (mk-static-rectangle world 2 2 5 1)
        x 6
        y 15
        w 3.6
        h 1
        ;; dx 3.6
        ;; dy -3
        body ;;(mk-dynamic-rectangle world 6 10 3.6 2, 1.0 1)
        ,   (mk-body world {:type :dynamic
                            :position [x y]
                            :fixtures [{:shape {:type :box
                                                :center [(/ w 2) (/ h 2)]
                                                :half-size [(/ w 2) (/ h 2)]}
                                        :density 2.0
                                        :friction 1}
                                       {:shape {:type :box
                                                :center [(/ w 2) h]
                                                :half-size [(/ w 2) (/ h 2)]}
                                        :density 0.1 
                                        :friction 1}

                                       {:shape {:type :poly
                                                :points [
                                                         w 0,
                                                         (* w 1.2) h
                                                         w h
                                                         ]}
                                        :density 0.1
                                        :friction 0}

                                       {:shape {:type :poly
                                                :points [
                                                         0 0,
                                                         (* w -0.2) h
                                                         0 h
                                                         ]}
                                        :density 0.1
                                        :friction 0}

                                       ]})

        ]
    {:world world
     :body body}))

;; ;; simulation

;; (def timeStep (/ 1.0 60.0))
;; (def velocityIterations 6)
;; (def positionIterations 2)

;; (doseq [i (range 100)]
;;   (.Step world timeStep velocityIterations positionIterations)
;;   (let [position (.GetPosition body)
;;         angle (.GetAngle body)]
;;     (println (.get_x position) (.get_y position) angle)))

;; (let [timeStep (/ 1.0 60.0)
;;       velocityIterations 6
;;       positionIterations 2]
;;   (.Step world timeStep velocityIterations positionIterations))

(defn push [st]

  ;; (let [{:keys [world body]} @st
  ;;       position (.GetPosition body)

  ;;       ]

  ;;   (.log js/console "PUSH3" (.get_x (.GetWorldCenter body)) (.get_y (.GetWorldCenter body)))
  ;;   (;;.ApplyLinearImpulse
  ;;    .ApplyForce
  ;;    body
  ;;    (b2.b2Vec2. 1000 0) ;; force
  ;;    (.GetWorldCenter body)
  ;;    )
  ;;   )
  )

#_(defn dooo [world body]

  (.ClearForces world)
  (let [velocity (.GetLinearVelocity body)
        vx (.get_x velocity)
        vy (.get_y velocity)
        v (Math/sqrt (+ (* vx vx) (* vy vy)))]

    (when (<= v 20.0)
      (cond
        (js/keyIsDown 68) (do
                            (.ApplyLinearImpulse body (b2.b2Vec2. 30 0) (.GetWorldCenter body))
                            :right)
        (js/keyIsDown 65) (do
                            (.ApplyLinearImpulse body (b2.b2Vec2. -30 0) (.GetWorldCenter body))
                            :left)
        (js/keyIsDown 87) (do
                            (.ApplyLinearImpulse body (b2.b2Vec2. 0 10) (.GetWorldCenter body))
                            :left)

        :else nil))))
