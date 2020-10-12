(ns app.fff
  (:require
   [gamebase-ecs.virtual-timer :as vt]

   [gamebase.systems.drawing :as sys-drawing]
   [app.ecs.ludzik :as ludzik]
   [app.tiles :as tiles]
   [app.config :as config]
   [gamebase.ecs :as ecs]
   [gamebase.layers :as layers]
   [app.scratch :as scratch]
   ;;[gamebase.ecs :as ecs]
   ;;[schema.core :as s :include-macros true]
   ;;[gamebase.systems.drawing]
   )

  )

(defn mk-new-world []
  (let [l (layers/v2-clean-layer-with-frame config/WORLD-WIDTH-IN-TILES config/WORLD-HEIGHT-IN-TILES
                                         nil
                                         [:kafelki 0])
        ls [[:foreground l]]
        ctx {:tile-width 32
             :tile-height 32
             :world-width-in-tiles config/WORLD-WIDTH-IN-TILES
             :world-height-in-tiles config/WORLD-HEIGHT-IN-TILES
             :tileset-rendering-map tiles/tileset-rendering-map
             :tileset-map tiles/tileset-map}]
    (-> (ecs/mk-world)
        (ecs/insert-object (sys-drawing/mk-system))
        (ecs/insert-object (ludzik/mk-entity "ludzik-1" 11 20))
        ;;(ecs/insert-object (flauto/mk-entity "flauto-1" 20 5))
        (assoc :layers ls :tile-context ctx)
        ((fn [wrl] (ecs/put-all-events wrl
                   (map #(ecs/mk-event % ::ecs/init 0) (ecs/all-components wrl)))))
        ((fn [wrl] (ecs/put-all-events wrl
                                       (map #(ecs/mk-event % ::ecs/init 1) (ecs/all-entities wrl))))))))
