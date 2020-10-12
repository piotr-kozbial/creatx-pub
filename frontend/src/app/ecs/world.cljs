(ns app.ecs.world
  (:require
   [gamebase.ecs :as ecs]
   [app.hooks :as hooks]))


(hooks/create-threaded-hook "post-update")

(defmethod ecs/handle-event [:to-world :update]
  [world event _]
  (-> world
      (#(reduce
         (fn [wrl sys] (ecs/do-handle-event wrl (ecs/retarget event sys)))
         %
         (ecs/all-systems %)))
      (#(reduce
         (fn [wrl e] (ecs/do-handle-event wrl (ecs/retarget event e)))
         %
         (ecs/all-entities %)))
      (#(hooks/trigger-hook "post-update" %))))


