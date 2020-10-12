(ns app.overlay.utils)

(defn in-button? [px py {:keys [x y w h]}]
  (and
   (>= px x)
   (< px (+ x w))
   (>= py y)
   (< py (+ y h))))

(defn get-button [buttons x y]
  (some
   (fn [[button-key button]] (when (in-button? x y button) [button-key button]))
   buttons))
