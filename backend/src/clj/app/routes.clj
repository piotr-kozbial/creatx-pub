(ns app.routes
  (:require [clojure.java.io :as io]
            [compojure.core :refer [ANY GET PUT POST
                                    DELETE routes]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [response]]
            [clojure.java.io :as io]
            [gamebase.local-redis-saveload-server]
            [clojure.string :as str]))

(defn home-routes [_]
  (let [cfg (if (.exists (io/file "config.edn"))
              (read-string (slurp "config.edn"))
              {})
        root-page #(-> (slurp "public/index.html")
                      (str/replace #"<<URL-PATH>>" (or (:url-path cfg) "creatx")))
        config {:host "127.0.0.1"
                :http-port (or (:port cfg) 11555)
                :root-page root-page
                :files-root "public"}]
    (println "redis prefix: " (:redis-prefix cfg))
    (gamebase.local-redis-saveload-server/build-handler config (or (:redis-prefix cfg) "creatx/"))))



