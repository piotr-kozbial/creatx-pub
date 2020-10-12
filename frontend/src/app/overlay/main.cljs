(ns app.overlay.main
  (:require
   [app.state :as st]
   [app.fff :as fff]
   [app.config :as config]
   [app.world-interop :as wo]
   [app.utils.jscript :as j]
   [gamebase.layers :as layers]
   [gamebase.resources :as resources]
   [app.tiles :as tiles]
   [app.server-communication :as sc]

   [app.ecs.flauto :as flauto]
   
   [app.scratch :as scratch]

   [gamebase.ecs :as ecs]
   
   [app.overlay.utils :as ou]

   [app.overlay.ui :as ui]
   ))

(declare overlay-draw-content)

(def WIDTH 500)
(def HEIGHT 500)

(declare save-game load-game)

(defn save-game [-state _]
  (swap! -state assoc :action :save-game)
  ;; (.log js/console "Saving game...")
  ;; timeout to let the browser render the "Saving game" text first
  (js/setTimeout
   #(sc/save-game (fn [_]
                    ;; (.log js/console "GAME SAVED.")
                    (swap! -state assoc
                           :action nil
                           :saved-time (js/millis))))
   100))

;; (defn switch-center-on-player []
;;   (swap! st/app-state update :center-on-player not))

;; (defn switch-night-mode []
;;   (swap! st/app-state update :night-mode not))

(defn go-fullscreen []
  (.webkitRequestFullScreen (.-documentElement js/document))

  )

(defn mobile-mode []
  (swap! st/app-state update :mobile-mode not))

(defn load-game [-state _]
  (swap! -state assoc :action :load-game)
  ;; (.log js/console "Loading game...")
  ;; timeout to let the browser render the "Loading game" text first
  (js/setTimeout

   #(sc/load-game
     (fn [ret]
       (let [world (:world (:state ret))
             ctx
             {:tile-width 32
              :tile-height 32
              :world-width-in-tiles config/WORLD-WIDTH-IN-TILES
              :world-height-in-tiles config/WORLD-HEIGHT-IN-TILES
              :tileset-rendering-map tiles/tileset-rendering-map
              :tileset-map tiles/tileset-map}

             world'
             (assoc world :tile-context ctx)]
         (wo/set-world!
          world'
          ;;(ecs/insert-object world' (flauto/mk-entity "flauto-1" 20 20))
          )
         (wo/stop!)
         ;; (.log js/console "GAME LOADED.")

         ;; Create box2d objects corresponding to tiles

         #_(let [layer (wo/get-layer world :foreground)
               width (layers/v2-get-layer-width layer)
               height (layers/v2-get-layer-height layer)]
           (print "TILE COUNT: "
                  (->>
                   (for [tile-x (range width) tile-y (range height)]
                     (when-let [[_ tile-number] (layers/v2-get-tile-from-layer layer tile-x tile-y)]
                       (let [{:as tile :keys [solid?]} (tiles/ti----les-by-number tile-number)]
                         ;;(println (pr-str [tile-x tile-y tile]))
                         (when solid?
                           1
                           )
                         ))

                     )
                   (remove nil?)
                   (count)))
           #_(doseq [tile-x (range width)
                   tile-y (range height)]
             (when-let [[_ tile-number] (layers/v2-get-tile-from-layer layer tile-x tile-y)]
               (let [{:as tile :keys [solid?]} (tiles/til----es-by-number tile-number)]
                 ;;(println (pr-str [tile-x tile-y tile]))
                 (when solid?
                   (let [{:keys [world body]} @st/b2-state]
                     (scratch/mk-static-rectangle world tile-x tile-y 1 1))
                   )
                 ))

             ))





         (swap! -state assoc :action nil)))
     (fn []
       (println "CANNOT LOAD GAME")
       (swap! -state assoc :action nil)
       (wo/set-world! (fff/mk-new-world))
       (wo/stop!)))
   100)
  )

(defn control-ludzik [_ _]
  ;; (println "CONTROL LUDZIK")
  (wo/set-control :ludzik)
 :overlay-off)

(defn control-flauto [_ _]
  ;; (println "CONTROL FLAUTO")
  (wo/set-control :flauto)
  :overlay-off)

(defn get-brush []
  (:build-brush @st/app-state))

(defn set-brush [w h]
  (swap! st/app-state assoc :build-brush [:square w h]))

;;;;;; External connection







(do ;; Custom widget: Tile Selector Button
  (defn tile-selector-button [tile-id]
    {:type :tile-selector-button
     :tile-id tile-id})

  (defmethod ui/ui-draw :tile-selector-button
    [state {:keys [tile-id]} x0 y0 w0 h0]
    (let [{:keys [world selected-tool]} @st/app-state
          ctx (:tile-context world)]
      (when tile-id
        (let [tile-coords (tiles/tile-coords-by-id tile-id)
              {:keys [img x y w h] :as inf}
              ,   (layers/v2-get-rendering-information-for-tile ctx tile-coords)]
          (js/image
           (resources/get-resource img)
           x0 y0 w0 h0
           x y w h)
          (when (= [:build tile-id] selected-tool)
            (js/strokeWeight 3)
            (js/noFill)
            (js/stroke 250 250 250)
            (js/rect (+ x0 3) (+ y0 3) (- w0 4) (- h0 4))
            (js/strokeWeight 2)
            (js/stroke 40 40 40)
            (js/rect (+ x0 2) (+ y0 2) (- w0 4) (- h0 4)))))))

  (defmethod ui/ui-click :tile-selector-button
    [state {:keys [tile-id]} x0 y0 w0 h0]
    ;;(println "TSB clicked" tile-id x0 y0 w0 h0)
    (wo/select-tool! (when tile-id [:build tile-id]))
    ;; (js/console.log "tile " tile-id " selected")
    :overlay-off
    ))

(do ;; Custom widget: Tool Selector Button
  (defn tool-selector-button [tool-id resource-name]
    {:type :tool-selector-button
     :resource-name resource-name
     :tool-id tool-id})

  (defmethod ui/ui-draw :tool-selector-button
    [state {:keys [resource-name tool-id]} x0 y0 w0 h0]
    (let [{:keys [world selected-tool]} @st/app-state
          ctx (:tile-context world)]
      (js/image
       (resources/get-resource resource-name)
       x0 y0 w0 h0
       0 0 32 32)
      (when (= tool-id selected-tool)
        (js/strokeWeight 3)
        (js/noFill)
        (js/stroke 250 250 250)
        (js/rect (+ x0 3) (+ y0 3) (- w0 4) (- h0 4))
        (js/strokeWeight 2)
        (js/stroke 40 40 40)
        (js/rect (+ x0 2) (+ y0 2) (- w0 4) (- h0 4)))))
  
  (defmethod ui/ui-click :tool-selector-button
    [state {:keys [tool-id]} x0 y0 w0 h0]
    (wo/select-tool! tool-id)
    :overlay-off))

(def save-game-button
  {:type :image-button
   :resource-name "save-game.png"
   :on-click (fn [pstate]
               ;; (println "SAVE GAME")
               (swap! pstate assoc :action :save-game)
               ;; timeout to let the browser render the "Saving game" text first

               (js/setTimeout
                (fn [] (sc/save-game (fn [_]
                                        (.log js/console "GAME SAVED.")
                                        (swap! pstate assoc
                                               :action nil
                                               :saved-time (js/millis)))))
                100))})

(def load-game-button
  {:type :image-button
   :resource-name "load-game.png"
   :on-click (fn [pstate] ;;(println "LOAD GAME")
               (swap! pstate assoc :action :load-game)
               ;; (.log js/console "Loading game...")
               ;; timeout to let the browser render the "Loading game" text first

               (js/setTimeout

                #(sc/load-game
                  (fn [ret]
                    (let [world (:world (:state ret))
                          ctx
                          {:tile-width 32
                           :tile-height 32
                           :world-width-in-tiles config/WORLD-WIDTH-IN-TILES
                           :world-height-in-tiles config/WORLD-HEIGHT-IN-TILES
                           :tileset-rendering-map tiles/tileset-rendering-map
                           :tileset-map tiles/tileset-map}
                          world'
                          (assoc world :tile-context ctx)]
                      (wo/set-world!
                       (ecs/insert-object world' (flauto/mk-entity "flauto-1" 20 20)))
                      (wo/stop!)
                      ;; (.log js/console "GAME LOADED.")
                      (swap! pstate assoc :action nil)))) 100))}
   )

(def tilesets
  [
   [:main "main"
    [[:ground :sand :stone :lava :water :snow :hout :wood-platform :spaceship-plate :stone-brick]
     [nil :non-solid-sand nil nil nil nil :non-solid-hout]

     [:gold-block :gold-chest :gold-platform :gold-window :gold-door-top :gold-door-middle
      :gold-door-bottom :bell]
     [:grass :red-flower :yellow-flower :green-flower :leaves :cactus :wood-fence :ladder]
     [:chest :green-teleport :yellow-teleport :red-teleport :blue-teleport
      :tnt :button :lever-right :small-villager]
     [:iron-ore :diamond-ore :coal-ore :gold-ore :redstone-ore :emerald-ore :lapis-lazuli-ore]]

    ]

   [:lights "lights"
    [[:lights/street-retro-big
      :lights/street-retro-small
      :lights/street-retro-left
      :lights/street-retro-right
      :lights/street-retro-triple
      nil
      :lights/street-pole
      :lights/street-pole-ring
      :lights/street-pole-big-ring
      :lights/street-pole-base
      :lights/street-pole-big-base
      ]
     [:lights/street-globe-big
      :lights/street-globe-small
      :lights/street-globe-left
      :lights/street-globe-right
      :lights/street-globe-triple
      nil
      :lights/street-pole-left
      :lights/street-pole-left-base
      :lights/street-pole-right
      :lights/street-pole-right-base]
     [:lights/street-boring-center
      :lights/street-boring-left
      :lights/street-boring-right
      nil nil nil
      :lights/street-pole-thin-left
      :lights/street-pole-thin-left-base
      :lights/street-pole-thin-right
      :lights/street-pole-thin-right-base

      ]
     [:lights/torch-center
      :lights/torch-left
      :lights/torch-right
      :lights/torch-double
      :lights/torch-triple]

     [:lamp]

     ]]

   [:dogs "dogs"
    [[:dog-house-1 :dog-house-2 nil         :dog-house-with-bg-1 :dog-house-with-bg-2]
     [:dog-house-3 :dog-house-4 :dog-house  :dog-house-with-bg-3 :dog-house-with-bg-4 :dog-house-with-bg]]]

   [:cactuses "cactus"
    [[:cactus/big-00 :cactus/big-01 :cactus/big-02 :cactus/flower-yellow]
     [:cactus/big-10 :cactus/big-11 :cactus/big-12 :cactus]
     [:cactus/big-20 :cactus/big-21 :cactus/big-22 :cactus/potted-with-yellow-flower]
     [:cactus/big-30 :cactus/big-31 :cactus/big-32 :cactus/potted]
     [:cactus/big-40 :cactus/big-41 :cactus/big-42]]]

   ])

(def tilesets-map
  (->> tilesets
       (mapcat (fn [[key & _ :as tileset]] [key tileset]))
       (apply hash-map)))




(def tile-selector-grid
  {:type :computed
   :produce
   (fn [state & _]
     (let [[_ _ tile-selector-layout] (tilesets-map (:overlay.tileset @st/app-state))]
       ;(println tilesets-map)
       {:type :grid
        :fixed-cell-width 40
        :fixed-cell-height 40
        :children (map
                   (fn [row]
                     (map (fn [tile-id]
                            (when tile-id (tile-selector-button tile-id))) row))
                   tile-selector-layout)}))})

(def tool-selector-grid
  {:type :grid
   :children [[(tool-selector-button nil "delete.png")
               nil
               (tool-selector-button [:match] "match.png")]]})


(def brush-selector
  {:type :computed
   :produce
   (fn [state & _]
     (let [{:keys [build-brush]} @st/app-state
           [_ w h] build-brush]
       {:type :grid
        :children (for [y (range 5)]
                    (for [x (range 5)]
                      {:type :plain-button
                       :color (if (and (< x w) (< y h)) [74 249 244] nil)
                       :on-click #(do
                                    (set-brush (inc x) (inc y))
                                    :overlay-off)}))}))})



(def root-widget
  {:type :computed
   :produce
   (fn [state & _]
     (let [world (wo/get-world!)
           app-state @st/app-state]
       (case (:action state)
         :save-game {:type :text :text "Saving..."}
         :load-game {:type :text :text "Loading..."}
         {:type :container
          :children [;; tileset selection
                     {:x 10 :y 20 :w 200 :h 30
                      :widget {:type :grid
                               :children
                               [
                                (for [[key caption data] tilesets]
                                  {:type :selectbox
                                   :text caption
                                   :text-size 20
                                   :selected? (fn [] (= (:overlay.tileset @st/app-state) key))
                                   :select (fn []
                                             (println "clicked")
                                             (swap! st/app-state assoc :overlay.tileset key))}
                                  )
                                ]
                               }}

                     ;; tile selection
                     {:x 10 :y 50 :w 480 :h 240
                      :widget tile-selector-grid}
                     ;; checkboxes
                     {:x 10 :y 320 :w 220 :h 25
                      :widget {:type :checkbox
                               :get-state #(:center-on-player @st/app-state)
                               :set-state #(swap! st/app-state assoc :center-on-player %)
                               :text-size 20
                               :text "Center on the player"}}
                     {:x 10 :y 345 :w 220 :h 25
                      :widget {:type :checkbox
                               :get-state #(:night-mode @st/app-state)
                               :set-state #(swap! st/app-state assoc :night-mode %)
                               :text-size 20
                               :text "Night mode"}}
                     {:x 10 :y 370 :w 220 :h 25
                      :widget {:type :checkbox
                               :get-state #(:mobile-mode @st/app-state)
                               :set-state #(swap! st/app-state assoc :mobile-mode %)
                               :text-size 20
                               :text "Mobile mode"}}

                     {:x 360 :y 320 :w 120 :h 40
                      :widget tool-selector-grid}
                     {:x 360 :y 420 :w 120 :h 60
                      :widget save-game-button}
                     #_{:x 360 :y 420 :w 120 :h 60
                        :widget load-game-button}

                     {:x 220 :y 320 :w 100 :h 100
                      :widget brush-selector}

                     {:x 10 :y 440 :w 200 :h 40
                      :widget {:type :grid
                               :fixed-cell-width 40
                               :fixed-cell-height 40
                               :children
                               [(vec (for [[target img-resource] (wo/get-controllables world)]
                                       {:type :image-button
                                        :border-color (when (= (:control app-state) target)
                                                        [0 160 160])
                                        :resource-name img-resource
                                        :on-click (fn [& _]
                                                    (js/console.log "set control")
                                                    (wo/set-control target))}))]}}]})))})



#_(defn on-click-main [-state x y]
  (let [button (which-button x y)
        {:keys [action]} @-state]
    (ui/ui-click @-state root-widget x y)
    #_(when-not action
      (when-let [callback (:callback (buttons button))]
        (callback -state button)))))

(def page-def
  {:width WIDTH
   :height HEIGHT
   :root-widget root-widget
   ;; :draw #'overlay-draw-content
   ;; :on-click #'on-click-main
   })
