(ns user
  (:require [app.application]
            [com.stuartsierra.component :as component]
            [reloaded.repl :refer [system init]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.file :refer [wrap-file]]
            [system.components.middleware :refer [new-middleware]]
            [app.config :refer [config]]


            [gamebase.helpers]
            ))

(defn dev-system []
  (let [config (config)]
    (assoc (app.application/app-system config)
           :middleware (new-middleware
                        {:middleware (into [[wrap-file "dev-target/public"]]
                                           (:middleware config))}))))

(reloaded.repl/set-init! #(dev-system))



;; Set up aliases so they don't accidentally
;; get scrubbed from the namespace declaration
(def start reloaded.repl/start)
(def stop reloaded.repl/stop)
(def go reloaded.repl/go)
(def reset reloaded.repl/reset)
(def reset-all reloaded.repl/reset-all)

;; deprecated, to be removed in Chestnut 1.0
(defn run []
  (println "(run) is deprecated, use (go)")
  (go))


