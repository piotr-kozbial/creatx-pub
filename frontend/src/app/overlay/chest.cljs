(ns app.overlay.chest
  (:require
   [app.overlay.utils :as ou]
   [gamebase.resources :as resources]))



(defn to-main-options [_ _]
  [:to-page :main])

(def buttons
  {:to-main {:resource nil
             :caption "To main options ->"
             :x 350 :y 20
             :w 130 :h 16
             :callback #'to-main-options}})

(defn draw [-state canvas-width canvas-height]


  (js/image
   (resources/get-resource "chest.png")
   5 5 32 32
   )
  (js/stroke 62 30 0)
  (js/strokeWeight 1)
  (js/fill 109 73 36)
  (js/textSize 20)
  (js/text "Chest" 50 28)

  (js/stroke 0 0 0)
  (js/strokeWeight 1)
  (js/fill 50 50 50)
  (js/textSize 16)
  (js/text "The chect is empty." 100 100)



  (doseq [{:keys [resource caption
                  x y w h
                  callback]} (vals buttons)]
    (when resource
      ;; TODO
      nil)
    (when caption
      (js/text caption x (+ y 12))
      )

    )
  )

(defn on-click [-state x y]
  (when-let [[button-key button] (ou/get-button buttons x y)]
    ((:callback button) -state button)))

(def page-def
  {:width 500
   :height 350
   :root-widget {:type :container, :children []}
   })
