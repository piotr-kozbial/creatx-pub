(ns app.debug-component
  (:require
   [app.utils.jscript :as u]
   [rum.core :as rum]
   [gamebase.debug :as dbg]
   [app.state :as st]))

;; Use by resetting `content0` to whatever you want to display.
;; Modify `(body)` to adjust *how* it is displayed.


(def content0 (atom {}))

(defn body [app-state content0]

  

  [:div
   [:p {:style {:white-space "pre"}} (u/pp (dissoc content0 :tile-map))]
   [:p (pr-str (:tile-map' content0))]
   [:p "*tile-map*"]
   [:table
    [:tr
     [:td] [:td -1] [:td 0] [:td 1]]
    (for [y [3 2 1 0 -1 -2]]
      [:tr
       [:td [:b y]]
       (for [x [-1 0 1]]
         [:td (when-let [tile-map (:tile-map content0)]
                (pr-str (tile-map [x y])))])])]])


(rum/defc debug-component < rum/reactive []
  [:div
   (let [{:keys [debug-overlay-on]} (rum/react dbg/settings)
         app-state (rum/react st/app-state)
         content0 (rum/react content0)]
     (when debug-overlay-on
       [:div {:style {:background "cyan"}}
        (body app-state content0)]))])

