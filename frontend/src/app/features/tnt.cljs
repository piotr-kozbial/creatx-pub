(ns app.features.tnt
  (:refer-clojure :exclude [run!])
  (:require [app.state :as st]
            [app.tiles :as tiles]
            [app.world-interop :as wo]
            [gamebase.layers :as layers]
            [gamebase.ecs :as ecs]
            [gamebase.resources :as resources]
            [app.world-interop :as wo]
            [app.hooks :as hooks]
            [app.tools :as tools]))

;; TNT states are kept in world,
;; under ks [ROOT-KEY [tile-x tile-y]]
;;
;; The value is:
;; {:explosion-time
;;  :current-explosion-radius}
;;
;; `:explosion-time` is when we start the explosion
;; The value should be set to current time + EXPLOSION DELAY if we ignite normally.
;; If we ignit because of chain reaction, we set `:explosion-time` to current time.
;;
;; Explosion scenario:
;; 1. We start with explosion radius nil, which means not yet exploded,
;;    and we draw this as a small flame on the tile.
;; 2. After EXPLOSION-DELAY, we go to explosion radius 0,
;;    and we draw this as a big explosion on the tile.
;; 3. After PROPAGATION-DELAY, we delete the tile and go to explosion radius 1,
;;    when we draw big explosions on a square (8 tiles in 3x3) around the tile.
;; 4. Again after PROPAGATION-DELAY, we delete the tiles on the 3x3 square
;;    and go to explosion radius 2, where we draw big explosions on a 5x5 square.
;; *. ... and so on, until we have already been to the MAXIMUM-RADIUS and again
;;    PROPAGATION-DELAY has passed; we then delete the tiles on 2n+1 x 2n+1 square
;;    and that ends the explosion.
;;
;; CORRECTION: every time we go to the next radius and on the new square we find
;; a TNT tile, we *explode* it immediately, meaning it goes directly into explosion
;; radius 0, without explosion delay.

(declare do-update draw when-tile-deleted on-tile-click-with-match)

(defn initialize []
  (hooks/subscribe-for-hook "post-update" #'do-update)
  (hooks/subscribe-for-hook "post-draw" #'draw)
  (hooks/subscribe-for-hook "tile-deleted" #'when-tile-deleted)
  (tools/register-tool :match
                       {:on-tile-click #'on-tile-click-with-match})
  )


(def ROOT-KEY :features.tnt)

(def EXPLOSION-DELAY 3000) ;; 3 sec??
(def PROPAGATION-DELAY 300) ;; 0.3 sec??
(def MAXIMUM-RADIUS 10)

(defn get-tnt-state [world tile-x tile-y]
  (get-in world [ROOT-KEY [tile-x tile-y]]))

(defn get-all-tnt-states [world]
  (ROOT-KEY world))

(defn set-tnt-state [world tile-x tile-y tnt-state]
  (doall (assoc-in world [ROOT-KEY [tile-x tile-y]] tnt-state)))

(defn del-tnt-state [world tile-x tile-y]
  (let [tnt (ROOT-KEY world)
        tnt' (dissoc tnt [tile-x tile-y])]
    (assoc world ROOT-KEY tnt')))

(defn set-tnt-state! [tile-x tile-y tnt-state]
  (swap! st/app-state assoc-in [:world ROOT-KEY [tile-x tile-y]] (doall tnt-state)))

(defn ignite [tile-x tile-y]
  (let [world (wo/get-world!)
        tnt-state (get-tnt-state world tile-x tile-y)]
    (when-not tnt-state
      (set-tnt-state! tile-x tile-y {:explosion-time (+ (wo/get-time!) EXPLOSION-DELAY)
                                    :current-explosion-radius nil}))))



(defn mk-draw-flame-fn [world]
  (let [ctx (:tile-context world)
        {:keys [img x y w h] :as inf}
        ,   (layers/v2-get-rendering-information-for-tile ctx tiles/FLAME)]
    (fn [tile-x tile-y]
      (js/image
       (resources/get-resource img)
       (* 32 tile-x) (* 32 tile-y) w h ;; dst
       x y w h ;; src
       ))))

(defn mk-draw-explosion-fn [world]
  (let [ctx (:tile-context world)
        {:keys [img x y w h] :as inf}
        ,   (layers/v2-get-rendering-information-for-tile ctx tiles/EXPLOSION)]
    (fn [tile-x tile-y]
      (js/image
       (resources/get-resource img)
       (* 32 tile-x) (* 32 tile-y) w h ;; dst
       x y w h ;; src
       ))))

(defn square-border [tile-x tile-y radius]
  (if (<= radius 0)
    [[tile-x tile-y]]
    (concat
     (apply concat
            (for [x (range (- tile-x radius) (+ 1 tile-x radius))]
              [[x (- tile-y radius)] [x (+ tile-y radius)]]))
     (apply concat
            (for [y (range (inc (- tile-y radius))
                           (+ tile-y radius))]
              [[(- tile-x radius) y] [(+ tile-x radius) y]])))))

(defn draw-one-tile [world tile-x tile-y draw-flame draw-explosion]
  (let [{:keys [explosion-time current-explosion-radius] :as state}
        ,   (get-tnt-state world tile-x tile-y)]
    (if (or (nil? current-explosion-radius) (= :frozen current-explosion-radius))
      (draw-flame tile-x tile-y)
      (doseq [[x y] (square-border tile-x tile-y current-explosion-radius)]
        (draw-explosion x y)))))

(defn draw []
  (let [world (wo/get-world!)
        draw-flame (mk-draw-flame-fn world)
        draw-explosion (mk-draw-explosion-fn world)]
    (doseq [[[tile-x tile-y] {:keys [explosion-time current-explosion-radius]}]
            (get-all-tnt-states world)]
      (draw-one-tile world tile-x tile-y draw-flame draw-explosion))))

;; returns nil if no more steps are to be executed
(defn update-one-tile [world tile-x tile-y]
  (let [current-time (::ecs/time world)]
    (if-let [{:keys [explosion-time current-explosion-radius] :as state}
             (get-tnt-state world tile-x tile-y)]
      (cond
        (= :frozen current-explosion-radius)
        ,   world
        (nil? current-explosion-radius)
        ,   (if (>= current-time explosion-time)
              (set-tnt-state world tile-x tile-y
                             (assoc state :current-explosion-radius 0))
              world)
        (>= current-time (+ explosion-time
                            (* PROPAGATION-DELAY (inc current-explosion-radius))))
        ,   (if (= current-explosion-radius MAXIMUM-RADIUS)
              (del-tnt-state world tile-x tile-y)
              (let [world' (set-tnt-state
                            world tile-x tile-y
                            (assoc state :current-explosion-radius
                                   (inc current-explosion-radius)))]
                (if (= current-explosion-radius 0)
                  (wo/del-tile world' :foreground tile-x tile-y)
                  (reduce
                   (fn [wrl [x y]]
                     (let [tl (wo/get-tile wrl :foreground x y)]
                       (if (= tl tiles/TNT)
                         (if (get-tnt-state wrl x y)
                           wrl
                           (set-tnt-state wrl x y
                                          {:explosion-time current-time 
                                           :current-explosion-radius 0}))
                         (wo/del-tile wrl :foreground x y)
                         )))
                   world'
                   (square-border tile-x tile-y current-explosion-radius)))))
        :else world))))




;; TODO !!!!!!!!!!!!! !!!!!!!!!!! !!!!!!!!! !!!!!! !!! !! !! ! ! !
;;
;; Tutaj usuwamy kafelki, nie przez (wo/del-tile!), ale przez (wo/del-tile)
;; w ramach do-update. To sie powinno jakos ogarnac, bo przeciez ludzie sa podpieci na hookach!
;;
;; To by trzeba jakos tak zrobic, ze wolamy (wo/del-tile) z jakims dodatkowym parametrem,
;; mowiacym "jest to w ramach zmiany prawdziwego world!" i wtedy automatycznie wolalyby
;; sie hooki, ale w trybie :threaded. A tak naprawde moglyby byc definiowane tylko
;; w trybie :threaded, a w (wo/del-tile!) byloby to opakowane.
;;
;; A moze daloby sie to robic zwyklym (wo/del-tile!)? Bo jesli tak, to by mozna (wo/del-tile)
;; w ogole zlikwidowac, bo chyba nikt inny tego nie uzywa. Tylko jak z wydajnoscia
;; wielokrotnego wolania (wo/del-tile!)?
;;
(defn do-update [world]
  (reduce
   (fn [wrl [tx ty]]
     (update-one-tile wrl tx ty))
   world
   (keys (get-all-tnt-states world))))

(defn when-tile-deleted [world tile-x tile-y old-tile]
  (if (= old-tile tiles/TNT)
    (let [tnt (or (ROOT-KEY world) {})
          {:keys [current-explosion-radius] :as t} (or (tnt [tile-x tile-y]) {})]
      (if current-explosion-radius
        world
        (assoc world ROOT-KEY (dissoc tnt [tile-x tile-y]))))
    world))
  
(defn on-tile-click-with-match [_ tile-x tile-y]

  (println "TOOL CALLBACK!" tile-x tile-y)


  (let [[_ number :as clicked-tile] (wo/get-tile! :foreground tile-x tile-y)]
    (when (= clicked-tile tiles/TNT)
      (ignite tile-x tile-y)))

  )
