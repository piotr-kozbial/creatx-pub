(ns app.overlay.ui
  (:require
   [gamebase.resources :as resources]))

(defmulti ui-draw (fn [state widget x y w h] (:type widget)))
(defmulti ui-click (fn [pstate widget x y w h] (:type widget)))

(defmethod ui-draw :image-button
  [state {:keys [resource-name border-color]} x y w h]
   (js/image
           (resources/get-resource resource-name)
           x y w h)
  (when border-color
    (js/noFill)
    (apply js/stroke border-color)
    (js/strokeWeight 2)
    (js/rect (dec x) (dec y) (+ w 2) (+ h 2))
    )
  )
(defmethod ui-click :image-button
  [pstate {:keys [children on-click]} x0 y0 w0 h0]
  (when on-click (on-click pstate)))

(defmethod ui-draw :plain-button
  [state {:keys [color]} x y w h]

  (js/stroke 0 0 0)
  (if color
    (apply js/fill color)
    (js/noFill))
  (js/rect x y w h)


  )
(defmethod ui-click :plain-button
  [pstate {:keys [on-click]} x0 y0 w0 h0]
  (when on-click (on-click pstate)))

(defmethod ui-draw :text-button
  [state {:as widget :keys [color text-color text-size]} x y w h]
  (js/stroke 0 0 0)
  (if color
    (apply js/fill color)
    (js/noFill))
  (js/rect x y w h)

  (js/textSize (or text-size 24))
  (js/textAlign js/CENTER js/CENTER)
  (apply js/fill (or text-color [0 0 0]))
  (js/noStroke)
  (js/text (:text widget) (+ x (/ w 2)) (+ y (/ h 2))))

(defmethod ui-click :text-button
  [pstate {:keys [on-click]} x0 y0 w0 h0]
  (when on-click (on-click pstate)))


(defmethod ui-draw :container
  [state {:keys [children]} x0 y0 w0 h0]
  ;; children must be a sequence of {:keys [widget x y w h]}
  (doseq [{:keys [widget x y w h]} children]
    (ui-draw state widget (+ x0 x) (+ y0 y) w h)))
(defmethod ui-click :container
  [pstate {:keys [children]} x0 y0 w0 h0]

  (->>
   (for [{:keys [widget x y w h]} children]
     (when (and (<= x x0 (+ x w -1)) (<= y y0 (+ y h -1)))
       (ui-click pstate widget (- x0 x) (- y0 y) w h)))
   (remove nil?)
   (last)))

(defmethod ui-draw :grid
  [state {:keys [children
                 fixed-cell-width
                 fixed-cell-height
                 ]} x y w h]
  (let [row-count (count children)
        column-count (apply max (map count children))
        cell-width (or fixed-cell-width (/ w column-count))
        cell-height (or fixed-cell-height (/ h row-count))]
    (doseq [row-number (range row-count)
            column-number (range column-count)]
      (let [row (nth children row-number)]
        (when (< column-number (count row))
          (when-let [child (nth row column-number)]
            (ui-draw state
                     child
                     (+ x (* column-number cell-width))
                     (+ y (* row-number cell-height))
                     cell-width cell-height)))))))
(defmethod ui-click :grid
  [pstate {:keys [children fixed-cell-width fixed-cell-height]} x y w h]
  (let [state @pstate
        row-count (count children)
        column-count (apply max (map count children))
        cell-width (or fixed-cell-width (/ w column-count))
        cell-height (or fixed-cell-height (/ h row-count))
        clicked-column (int (/ x cell-width)) 
        clicked-row (int (/ y cell-height))]

    (when (< clicked-row row-count)
      (let [row (nth children clicked-row)]
        (when (< clicked-column (count row))
          (when-let [child (nth row clicked-column)]
            (ui-click state child
                      (- x (* w (/ clicked-column column-count)))
                      (- y (* h (/ clicked-row row-count)))
                      cell-width cell-height)))))))

(defmethod ui-draw :computed
 [state {:keys [produce]} x y w h]
  (ui-draw state (produce state x y w h) x y w h))
(defmethod ui-click :computed
 [pstate {:keys [produce]} x y w h]
  (ui-click pstate (produce @pstate x y w h) x y w h))

(defmethod ui-draw :text
  [state widget x y w h]
  (js/stroke 0 0 0)
  (js/noFill)
  (js/textSize 32)
  (js/textAlign js/LEFT js/BOTTOM)
  (js/text (:text widget) x y))
(defmethod ui-click :text [& _])

(defmethod ui-draw :checkbox
  [state {:as widget :keys [text-size get-state set-state]} x y w h]
  (js/stroke 0 0 0)
  (js/fill 0 0 0)
  (js/textSize text-size)
  (apply js/stroke (if (get-state) [50 236 239] [0 0 0]))
  (js/textAlign js/LEFT js/BOTTOM)
  (js/text (:text widget) x (+ y text-size)))
(defmethod ui-click :checkbox
  [state {:keys [get-state set-state]} x y w h]
  (set-state (not (get-state))))

(defmethod ui-draw :selectbox
  [state {:as widget :keys [text-size selected? select]} x y w h]
  (js/stroke 0 0 0)
  (js/fill 0 0 0)
  (js/textSize text-size)
  (apply js/stroke (if (selected?) [50 236 239] [0 0 0]))
  (js/textAlign js/LEFT js/BOTTOM)
  (js/text (:text widget) x (+ y text-size)))
(defmethod ui-click :selectbox
  [state {:keys [selected? select]} x y w h]
  (select)
)
