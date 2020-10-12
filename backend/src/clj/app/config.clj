(ns app.config
  (:require [environ.core :refer [env]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [clojure.java.io :as io]))

(defn config []
  (let [cfg (if (.exists (io/file "config.edn"))
              (slurp "config.edn")
              {})]
    {:http-port  (Integer. (or (env :port) (or (:port cfg) 11555)))
     :middleware [[wrap-defaults api-defaults]
                  wrap-with-logger
                  wrap-gzip]}))
