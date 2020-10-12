(ns app.tools)

(def -tool-map (atom {}))

;; tool-info
{
 ;; callback to call when tile clicked with the tool selected
 ;; (<callback> key tile-x tile-y)
 :on-tile-click nil
 
 }

(defn notify-click [key tile-x tile-y]
  (if-let [tool-info (@-tool-map key)]
    (when-let [callback (:on-tile-click tool-info)]
      (callback key tile-x tile-y))
    (println "UNKNOWN TOOL:" (pr-str key))))

(defn register-tool [key tool-info]
  (swap! -tool-map assoc key tool-info))





