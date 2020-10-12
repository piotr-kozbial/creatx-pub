(ns ^:figwheel-hooks app.core
  (:require
   [goog.dom :as gdom]
   [rum.core :as rum]
 ;;  [schema.core :as s :include-macros true]


   [app.overlay.overlay :as overlay]
   [app.overlay.main :as overlay-main]
   [app.overlay.utils :as ou]
[cljsjs.p5]


   [gamebase.projection :as proj]
   [app.debug-component :refer [debug-component]]

   [app.config :as config]
   [app.state :refer [app-state]]

   [app.fff :as fff]

   [gamebase.resources :as resources]

   [gamebase.systems.drawing :as sys-drawing]

   [gamebase.ecs :as ecs]
   [app.ecs.world]
   [app.ecs.ludzik :as ludzik]
   [app.ecs.flauto :as flauto]

   [gamebase.events :as events]
   [gamebase.ecs :as ecs]
   [gamebase.canvas-control :as canvas-control]
   [gamebase.layouts.fullscreen :as our-layout]

   [cljs.pprint :refer [pprint]]

   [gamebase.layers :as layers]
   [gamebase.debug :as debug]

   [app.tiles :as tiles]

   [app.state :as st]
   [app.hooks :as hooks]
   [app.tools :as tools]
   [app.world-interop :as wo]

   [app.scratch :as scratch]

   [app.features.teleport :as teleport]
   [app.features.tnt :as tnt]
   [app.features.small-villager :as small-villager]

   [app.identity :as identity]
   )


  )

;; Simulation animation and drawing

(def mouse-pressed-since (atom nil))
(def LONG-PRESS-THRESHOLD 500)
(def DRAG-THRESHOLD 150)



;; Event handlers (but there are more in (main), which shouldn't be there!)

(defn key-handler [{:keys [key x y]}]
     (when-let [[conv-x conv-y] (canvas-control/get-canvas-to-world-converters)]
       (let [world-x (conv-x x)
             world-y (conv-y y)
             tile-x (quot world-x 32)
             tile-y (quot world-y 32)]
         (cond
           (= key "-") (swap! debug/settings update-in [:debug-overlay-on] not)
           (= key "1") (let [scale (canvas-control/get-scale)]
                         (when (> scale 0.12)
                           (canvas-control/set-scale (* 0.5 scale))))
           (= key "2")  (let [scale (canvas-control/get-scale)]
                          (when (< scale 4)
                            (canvas-control/set-scale (* 2 scale))))
           (= key " ") (if (overlay/overlay-on?)
                         (do
                           (wo/run!)
                           (overlay/overlay-off))
                         (do
                           (wo/stop!)
                           (overlay/overlay-on)))
           (= key "z") (.webkitRequestFullScreen (.-documentElement js/document))
           (= key "f") (wo/put-event!
                        (ecs/mk-event (ecs/to-entity (wo/get-ludzik (wo/get-world!)))
                                      :toggle-flight-mode
                                      (wo/get-time!)))
           (= key "c") (wo/put-event!
                        (ecs/mk-event (ecs/to-entity (wo/get-ludzik (wo/get-world!)))
                                      :toggle-speed-mode
                                      (wo/get-time!)))

           :else (hooks/trigger-hook "key-hit" key)))))



(defn handle-click-on-tile [world tile-x tile-y selected-tool]
  (cond
    ;; TODO - to ma byc potem inaczej; tool [:build nil]
    (nil? selected-tool)
    (let [[_ number :as clicked-tile] (wo/get-tile world :foreground tile-x tile-y)
          {:keys [build-brush]} @st/app-state
          [_ brush-w brush-h] build-brush]
      (doseq [dx (range brush-w) dy (range brush-h)]
        (wo/del-tile! :foreground (+ tile-x dx) (+ tile-y dy))))

    (= (first selected-tool) :build)
    (let [{:keys [build-brush]} @st/app-state
          [_ brush-w brush-h] build-brush]
      (doseq [dx (range brush-w) dy (range brush-h)]
        (apply wo/set-tile! :foreground (+ tile-x dx) (+ tile-y dy)
               (tiles/tile-coords-by-id (second selected-tool))))

      #_(let [{:keys [world body]} @st/b2-state]
        (scratch/mk-static-rectangle world tile-x tile-y 1 1))
      )


    :else
    (tools/notify-click (first selected-tool) tile-x tile-y)))
(defn -mouse-in-active-area? [{:keys [mouse-x mouse-y]} world]

  ;; mouse-x, mouse-y are WORLD coordinates of mouse

  (let [tile-x (quot mouse-x 32)
        tile-y (quot mouse-y 32)]

    (when-let [l (wo/get-ludzik world)]
      (ludzik/is-tile-active? l tile-x tile-y))))
(defn -draw-tile-boxes-around-mouse [{:keys [mouse-x mouse-y]}]
  (js/noFill)
  (js/strokeWeight 1)
  (let [{:keys [build-brush]} @st/app-state
        [_ brush-w brush-h] build-brush 
        tile-x (quot mouse-x 32)
        tile-y (quot mouse-y 32)]
    (doseq [dx (range brush-w) dy (range brush-h)]
      (js/stroke 20 20 20)
      (js/rect (* 32 (+ tile-x dx)) (* 32 (+ tile-y dy)) 31 31)
      (js/stroke 210 210 210)
      (js/rect (inc (* 32 (+ tile-x dx))) (dec (* 32 (+ tile-y dy))) 31 31))))


;; Simulation

(defn advance-simulation []

  (when (and @mouse-pressed-since
             (> (- (js/millis) @mouse-pressed-since) LONG-PRESS-THRESHOLD))

    (reset! mouse-pressed-since nil)
    (or ;; TODO: (teleport)
        (if (overlay/overlay-on?)
          (do
            (wo/run!)
            (overlay/overlay-off))
          (do
            (wo/stop!)
            (overlay/overlay-on))))    )

  (let [{:keys [world timer center-on-player]} @app-state]
    (when world

  ;;; Handle pending events...
      (let [world' (if (wo/running?!)
                     (let [time (wo/get-time!)]
                       (-> world
                           (ecs/advance-until-time time)
                           (ecs/do-handle-event (ecs/mk-event (ecs/to-world) :update time))))
                     world)]
  ;;; and put new world in state.
        (swap! app-state assoc :world world')

  ;;; Pan viewport if that's switched on
        (when center-on-player
          (when-let [ludzik (wo/get-ludzik world')]
            (let [ludzik-x (* 32 (:tile-x ludzik))
                  ludzik-y (* 32 (:tile-y ludzik))]
              (when (and (number? ludzik-x) (number? ludzik-y))
                (canvas-control/center-on (proj/world-point [ludzik-x ludzik-y]))))))))))

;; Main drawing
(defn draw-margin []

  (js/noStroke)
  (js/fill 73 36 0)
  (js/rect -10000 -10000 10000 84000)
  (js/rect -10000 -10000 84000 10000)
  (js/rect 64000 -10000 10000 84000)
  (js/rect -10000 64000 84000 10000)
  )
(defn draw-simulation [{:keys [min-x max-x min-y max-y] :as context}]


  (let [{:keys [world timer night-mode]} @app-state]
    (when world
  ;;; Draw the world.
      (sys-drawing/draw-all world (assoc context
                                         :night-mode night-mode))
  ;;; Draw other things.
      (if (and (not (overlay/overlay-on?))
           (-mouse-in-active-area? context world))
        (-draw-tile-boxes-around-mouse context))
      (draw-margin)

      (hooks/trigger-hook "post-draw")

  ;;; Draw debug stuff.
      (when (-> @debug/settings
                :canvas-control
                :coordinate-system-marker))


      ;; ;;dark overlay
      
      ;; (js/fill "rgba(0,0,0, 0.90 )")
      ;; (js/noStroke)
      ;; (js/rect min-x min-y (- max-x min-x) (- max-y min-y))
      


      )))

;; Overlay drawing
(defn buttons [canvas-width canvas-height]
  (let [ymid (int (/ canvas-height 2))
        d (* canvas-width (/ 100 1360))
        w (int (* 0.8 d))
        h (int (* 0.8 d))]
    {:left {:x (- canvas-width (* 3.3 d)) :y (- ymid (int (/ h 2))) :w w :h h}
     :right {:x (- canvas-width (* 1.4 d)) :y (- ymid (int (/ h 2))) :w w :h h}
     :up {:x (- canvas-width (* 2.35 d)) :y (- ymid h (* 0.6 d)) :w w :h h}
     :down {:x (- canvas-width (* 2.35 d)) :y (+ ymid h (* -0.2 d)) :w w :h h}
     :flight {:x (- canvas-width (* 3.3 d)) :y (+ ymid h h) :w (+ w w w (* 0.3 d)) :h h}
     :zoom-out {:x (- canvas-width (* 3.1 d)) :y (- ymid h h h) :w w :h h}
     :zoom-in {:x (- canvas-width (* 1.6 d)) :y (- ymid h h h) :w w :h h}}))
(defn which-button [bs x y]
  (some
   (fn [[button-key button]] (when (ou/in-button? x y button) button-key))
   bs))
(defn overlay-draw [canvas-width canvas-height]
  (when (:mobile-mode @st/app-state)
    (js/noStroke)
    (js/fill (js/color 255 255 255 150))
    (doseq [{:keys [x y w h]} (vals (buttons canvas-width canvas-height))]
      (js/rect x y w h)))
  (overlay/overlay-draw canvas-width canvas-height))

;; TODO - this shouldn't be here


;; Component for rum

(declare main')

(rum/defc main-component < rum/reactive []
  ;; (rum/react ui-refresh-tick)
  (let [user (rum/react identity/-user)]
    [:div

     (when-not user
       [:div {:style {:position "absolute"  :z-index 100
                      :top "50%" :left "50%";
                      :margin-right "-50%"
                      :transform "translate(-50%, -50%)"
                      }}
        [:h1 {:style {:text-align "center"}} "CREATX"]
        [:br] [:br] [:br]
        [:span {:style {:font-size "40pt"}}
         [:a {:href "#"
              :on-click (fn [_] (identity/set-user "x") (main'))
              } "X"]
         [:span {:style {:white-space "pre"}} "      "]
         [:a {:href "#"
              :on-click (fn [_] (identity/set-user "o") (main'))
              } "O"]]])


     [:div {:style {:position "absolute" :top 0 :left 0 :z-index 100}}
      (debug-component)]

     (our-layout/mk-html)]))

;; Initialization sequence

(declare mount-app-element
         initialize-frame-rate initialize-layout initialize-canvas-control initialize-resources
         do-when-resources-loaded)


(defn main []

  (mount-app-element)

  )

(defn main' [& [skip-init-layout?]]

  ;; (identity/set-user "x")
  
  (hooks/create-regular-hook "post-draw")
  (hooks/create-regular-hook "key-hit")


  (initialize-frame-rate)
  (when-not skip-init-layout?
    (initialize-layout))
  (initialize-canvas-control)

  (events/add-handler ;; setup key handler
   :canvas-key-typed
   #'key-handler)
  (events/add-handler
   :canvas-mouse-released
   (fn [_]
     (reset! ludzik/button-down nil)
     (reset! mouse-pressed-since nil)))
  (events/add-handler
   :canvas-mouse-dragged
   (fn [{:keys [x y prev-x prev-y]}]
     (when (> (+ (* (- x prev-x) (- x prev-x))
                 (* (- y prev-y) (- y prev-y)))
              DRAG-THRESHOLD)
       (reset! mouse-pressed-since nil))))
  (events/add-handler ;; setup mouse handler
   :canvas-mouse-pressed ;; clicked
   (fn [{:keys [x y button]}]
     (let [[cw ch] (our-layout/get-canvas-size)
           bs (buttons cw ch)]

       (if-let [but (which-button bs x y)]
         (case but

           :left (reset! ludzik/button-down :left)
           :right (reset! ludzik/button-down :right)
           :up (reset! ludzik/button-down :up)
           :down (reset! ludzik/button-down :down)

           :flight
           (wo/put-event!
            (ecs/mk-event (ecs/to-entity (wo/get-ludzik (wo/get-world!)))
                          :toggle-flight-mode
                          (wo/get-time!)))

           :zoom-in
           (let [scale (canvas-control/get-scale)]
             (when (< scale 4)
               (canvas-control/set-scale (* 2 scale))))

           :zoom-out
           (let [scale (canvas-control/get-scale)]
             (when (> scale 0.12)
               (canvas-control/set-scale (* 0.5 scale))))
           )

         (do
           (reset! mouse-pressed-since (js/millis))
           (when-let [[conv-x conv-y] (canvas-control/get-canvas-to-world-converters)]
             (let [world-x (conv-x x)
                   world-y (conv-y y)
                   tile-x (quot world-x 32)
                   tile-y (quot world-y 32)]

               (if (overlay/overlay-on?)
                 ;; TODO - click on overlay should always work, not only
                 ;; when canvas is clicked within world bounds
                 ;; (This is probably the problem why you can't use the overlay
                 ;; when there's no sky behind it.)
                 (let [[wc hc] (canvas-control/get-canvas-size)]
                   (overlay/on-click-main wc hc x y))
                 (let [{:keys [world selected-tool]} @app-state]
                   (when-let [l (wo/get-ludzik world)]
                     (when (ludzik/is-tile-active? l tile-x tile-y)
                       (handle-click-on-tile world tile-x tile-y selected-tool))))))))))))

  (initialize-resources do-when-resources-loaded))
(defn initialize-frame-rate []
  (when config/FRAME-RATE (js/frameRate config/FRAME-RATE))

  (js/setInterval ;; set up periodic frame rate measurement update
   (fn []
     (let [rate (js/frameRate)
           rate-s (/ (int (* rate 10)) 10)]
       (swap! app-state assoc :frame-rate rate-s)))
   1000))
(defn initialize-layout []
  (our-layout/initialize
   app-state [:layout]
   {:bottom-bar-height 150
    :side-bar-width 200
    :after-canvas-resize
    #(;;.log js/console "ACR callback"
      )}))
(defn initialize-canvas-control []
  (canvas-control/initialize
   {:state-atom app-state
    :state-kvs [:canvas-control]
    :advance #'advance-simulation
    :draw #'draw-simulation
    :overlay-draw #'overlay-draw
    :get-canvas-size our-layout/get-canvas-size
    :get-world-size #(vector (* 32 config/WORLD-WIDTH-IN-TILES)
                             (* 32 config/WORLD-HEIGHT-IN-TILES))}))
(defn initialize-resources [follow-up]
  (resources/set-prefix (config/RESOURCES-PREFIX))
  (doseq [fname config/RESOURCE-FNAMES] (resources/add-resource fname))

  (resources/set-on-all-loaded
   #(when (every? resources/get-resource config/RESOURCE-FNAMES) ;; TODO: we double check, why?
      (follow-up))))
(defn do-when-resources-loaded []

  ;; initialize features
  (wo/initialize)
  (tnt/initialize)
  (teleport/initialize)
  (small-villager/initialize)
  
  (wo/set-world! (fff/mk-new-world))
  (wo/stop!)
  ;; (wo/run!)

  (overlay/overlay-on)
  (overlay-main/load-game overlay/-state nil)
  
  )

(defn mount-app-element []
  (when-let [el (. js/document (getElementById "app"))]
    (rum/mount (main-component) el)))

(defn ^:after-load on-reload []
  ;(mount-app-element)
  ;(main' true)
  )
