(ns app.state
  (:require
   [gamebase-ecs.virtual-timer :as vt]

   [app.scratch :as scratch]
   ;;[gamebase.ecs :as ecs]
   ;;[schema.core :as s :include-macros true]
   ;;[gamebase.systems.drawing]
   ))

#_(def s-world
  ;; we use ecs' notion of world and we add some keys to it; UGLY
  (merge
   ecs/s-world
   {:tile-context gamebase.systems.drawing/s-tile-context
    :layers [[(s/one s/Keyword "layer key") (s/one s/Any "layer")]]}))

#_(def s-app-state
  {:world s-world
   :timer s/Any
   :tileset-rendering-map s/Any
   :selected-tool s/Any
   :center-on-player s/Bool
   ;; ...
   })

(defonce app-state
  (atom
   {:frame-rate 0
    :timer (vt/mk-timer)
    :selected-tool [:build :ground]
    :center-on-player true
    :control :ludzik ;; :flauto
    :build-brush [:square 1 1]
    :overlay.tileset :main
    }))


(def b2-state (atom (scratch/mk-b2-state)))





