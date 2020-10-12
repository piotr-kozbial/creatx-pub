(ns app.utils.jscript
  (:require [clojure.pprint :refer [pprint]]))

(defn timestamp-str->local-date-str [s]
  (when (and s (not= s ""))
    (.toLocaleDateString (js/Date. s))))

(defn pr-json [v]
  (.stringify js/JSON (clj->js v)))

(defn pp [v]
  (with-out-str (pprint v)))

(defn do-after-ms [ms f]
  (js/setTimeout
   (fn [_] (f))
   ms))
