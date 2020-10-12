(ns app.overlay.overlay
  (:require
   [cljs.reader :refer [read-string]]
   [app.tiles :as tiles]
   [app.utils.jscript :as j]
   [app.world-interop :as wo]
   [app.overlay.main :as overlay-main]
   [app.overlay.chest :as overlay-chest]
   [app.overlay.ui :as ui]))

;; TODO - Zrobic w formie API dla feature'ow: dodawanie nowych stron, ich aktywacja itp.
;;

(def pages
  {:main overlay-main/page-def
   :chest overlay-chest/page-def})

(defonce -state (atom {:on? false
                       :page :main
                       :action nil
                       :saved-time nil

                       :message nil
                       :message-end nil}))

(defn overlay-on? []
  (:on? @-state))

;; TODO - tu jest reczni uwzgledniony chest, trzeba to rozbic
;; PO PIERWSZE, ogolny mechanizm wyboru strony
;; PO DRUGIE, jakie strony sa w ogole aktywne, zaleznie od kontekstu
;; PO TRZECIE, to wszystko co zwiazane z chest, w tym jego UI, ma byc w features/chest!
;;      (choc byc moze powiazane jakos w jedno z Inventory, czyli plecakiem, ktory ma przy sobie)
;;      (choc czesc z tego moze byc uznane za bazowa funkcjonalnosc/logike)
(defn overlay-on []
  (let [world (wo/get-world!)
        {:keys [tile-x tile-y]} (wo/get-ludzik world)
        tile (wo/get-tile world :foreground tile-x tile-y)]
    (if (or (= tile tiles/CHEST) (= tile tiles/GOLD-CHEST))
      (swap! -state assoc :page :chest)
      (swap! -state assoc :page :main)))
  (swap! -state assoc :on? true))

(defn overlay-off []
  (swap! -state assoc :on? false))

(def MESSAGE-DURATION 1500)

(defn message! [text]
  (swap! -state assoc :message text :message-end (+ (js/millis) MESSAGE-DURATION))
  )

(defn overlay-draw [canvas-width canvas-height]
  (let [{:keys [page message message-end]} @-state
        {:keys [width height root-widget]} (pages page)]

    (when (overlay-on?)
      (let [mx (/ canvas-width 2)
            my (/ canvas-height 2)
            left (int (- mx (/ width 2)))
            right (int (+ mx (/ width 2)))
            top (int (- my (/ height 2)))
            bottom (int (+ my (/ height 2)))]

        
        (js/noStroke)
        (js/fill (js/color 255 255 255 150))
        (js/rect left top width height)
        (js/translate left top)
        #_(draw -state)
        (ui/ui-draw @-state root-widget 0 0 width height)))

    (when message
      (when (>= (js/millis) message-end)
        (swap! -state assoc :message nil :message-end nil))
      (let [mx (/ canvas-width 2)
            my (/ canvas-height 2)
            left (int (- mx 200))
            right (int (+ mx 200))
            top (int (- my 50))
            bottom (int (+ my 50))]

        (js/resetMatrix)
        (js/noStroke)
        (js/fill (js/color 255 255 255 255))
        (js/rect left top 400 100)


        

        (js/stroke 0 0 0)
        (js/noFill)
        (js/textSize 24)
        (js/textAlign js/CENTER js/CENTER)

        (js/text message mx my)


        ))))

;; This is our callback for mouse click. It will be called for us externally,
;; only when we're active, i.e. our own (overlay-on?) returns true.
;; TODO - this could be handled as a hook. We would add a mechanism for a hook
;; to say "stop processing", so that if we handle a click (when (overlay-on?)),
;; the regular handling (in world) will not happen.
(defn on-click-main [canvas-width canvas-height cx cy]
  (when (overlay-on?) ;; we check just in case
    (let [{:keys [root-widget width height]} (pages (:page @-state))
          mx (/ canvas-width 2)
          my (/ canvas-height 2)
          left (int (- mx (/ width 2)))
          top (int (- my (/ height 2)))
          x (- cx left)
          y (- cy top)]
      (let [return-value
            (ui/ui-click -state root-widget x y width height)]
        (cond
          (= return-value :overlay-off)
          ,   (j/do-after-ms 250 #(do (overlay-off) (wo/run!)))
          (and (vector? return-value)
               (= (first return-value) :to-page))
          ,   (swap! -state assoc :page (second return-value)))))))

