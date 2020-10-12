;;;;; STATUS: DONE for now ;;;;;

(ns gamebase.layers
  (:require-macros [gamebase.helpers :refer [examples]]))

;;; This module defines structures for layers.

;;;# Tiled layers
(do

;;; TODO : weryfikowac, czy tile-width/tile-height sie zgadzaja
;;; TODO : Potem tez weryfikowac pomiedzy wszystkimi layerami

;;; Tiles will be grouped in tilesets.
;;; So a tile id will consist of a keywordized tileset name and tile number, e.g.:

  [:kafelki 3]

;;;## Basic operations
  (do

;;; Creating a layer filled with a given tile:

    (defn clean-layer [width height fill-tile]
      (let [row (apply vector (repeat width fill-tile))
            r (atom 0)
            data (apply vector (repeat height row))]
        {:layer-type :tiled
         :data data}))

    (defn v2-clean-layer [width height fill-tile]
      {:layer-type :v2-tiled
       :width width
       :height height
       :fill-tile fill-tile
       :tile-map {}})

    
    (examples
     (clean-layer 5 3 [:kafelki 3])
     =>
     {:layer-type :tiled
      :data [[[:kafelki 3] [:kafelki 3] [:kafelki 3] [:kafelki 3] [:kafelki 3]]
             [[:kafelki 3] [:kafelki 3] [:kafelki 3] [:kafelki 3] [:kafelki 3]]
             [[:kafelki 3] [:kafelki 3] [:kafelki 3] [:kafelki 3] [:kafelki 3]]]})

    (defn clean-layer-with-frame [width height fill-tile frame-tile]
      (let [row (apply vector
                  (concat
                   [frame-tile frame-tile]
                   (repeat (- width 4) fill-tile)
                   [frame-tile frame-tile]))
            frame-row (apply vector (repeat width frame-tile))
            r (atom 0)
            data (apply vector
                   (concat
                    [frame-row frame-row]
                    (repeat (- height 4) row)
                    [frame-row frame-row]))]
        {:layer-type :tiled
         :data data}))

    ;; TODO
    (defn v2-clean-layer-with-frame [width height fill-tile frame-tile]
      ;; TODO
      (v2-clean-layer width height fill-tile))




;;; WARNING! A layer printed like this "looks" upside-down wrt. how it will be
;;; rendered, because row number logically grows upwards in the coordinate
;;; system.

;;; And of course this data can be used only in context where the tileset-map is
;;; known and rendered only where tileset-rendering-map is given.

;;; Let's also add a convenience function to substitute a tile in a layer:

    (defn set-tile-in-layer [layer x y new-tile]
      (update-in layer [:data y] #(assoc % x new-tile)))

    (defn v2-set-tile-in-layer [{:as layer :keys [width height tile-map]} x y new-tile]
      (if (and (<= 0 x (dec width)) (<= 0 y (dec height)))
        (let [tile-map' (assoc tile-map [x y] new-tile)]
          (assoc layer :tile-map tile-map'))))

    (examples
     (set-tile-in-layer
      {:layer-type :tiled
       :data [[[:kafelki 0] [:kafelki 1] [:inny-set 5]]
              [[:kafelki 1] [:kafelki 1] [:inny-set 5]]
              [[:kafelki 2] [:kafelki 1] [:inny-set 5]]
              [[:kafelki 3] [:kafelki 1] [:inny-set 5]]]}
      1 3
      [:ten-zmieniony 10])
     =>
     {:layer-type :tiled
      :data [[[:kafelki 0] [:kafelki 1] [:inny-set 5]]
             [[:kafelki 1] [:kafelki 1] [:inny-set 5]]
             [[:kafelki 2] [:kafelki 1] [:inny-set 5]]
             [[:kafelki 3] [:ten-zmieniony 10] [:inny-set 5]]]})

;;; ...

    (defn get-layer-height [layer]
      (count (:data layer)))

    (defn v2-get-layer-height [layer]
      (:height layer))

    (defn get-layer-width [layer]
      (count (first (:data layer))))

    (defn v2-get-layer-width [layer]
      (:width layer))


;;; Another function will get a tile from layer (very simple):

    (defn get-tile-from-layer [layer x y]
      ;;(println "get-tile-info")
      (let [height (get-layer-height layer)
            width (get-layer-width layer)]
        (when (and (<= 0 x (dec width))
                   (<= 0 y (dec height)))
          (((:data layer) y) x))))

    (defn v2-get-tile-from-layer [layer x y]
      ;;(println "get-tile-info")
      ((:tile-map layer) [x y]))

    
    (examples
     (get-tile-from-layer
      {:layer-type :tiled
       :data [[[:kafelki 0] [:kafelki 1] [:inny-set 5]]
              [[:kafelki 1] [:kafelki 1] [:inny-set 55]]
              [[:kafelki 2] [:kafelki 1] [:inny-set 5]]
              [[:kafelki 3] [:kafelki 1] [:inny-set 5]]]}
      2 1)
     =>
     [:inny-set 55])



    )

;;;## Loading tiled layers from tmx files
  (do

    (def example-tmx
      '{:width 5
        :height 3
        :tile-width 32
        :tile-height 32
        :tilesets {"background" {:name "background"
                                 :id-offset 401
                                 :tile-width 32
                                 :tile-height 32
                                 :tile-count 40
                                 :tile-columns 10
                                 :image-file-name "background.png"
                                 :image-file-width 320
                                 :image-file-height 128}
                   "kafelki"    {:name "kafelki"
                                 :id-offset 1
                                 :tile-width 32
                                 :tile-height 32
                                 :tile-count 400
                                 :tile-columns 20
                                 :image-file-name "tiles.png"
                                 :image-file-width 640
                                 :image-file-height 640}}
        :layers {"foreground" {:name "foreground"
                               :width 5
                               :height 3
                               :id-list (1 4 4 62 63 24 0 0 0 0 24 0 0 0 0)}
                 "above"      {:name "above"
                               :width 5
                               :height 3
                               :id-list (0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)}
                 "background" {:name "background"
                               :width 5
                               :height 3
                               :id-list
                               (401 401 401 401 401 401 401 401 401 401 401 401 401 401 401)}}})

;;; First, we'll need to preapre a list of tileset names (keywordized) with their id-offset, sorted by those offsets.

    (defn- create-tmx-tileset-offset-list [tmx-data]
      (let [base-list (->> (:tilesets tmx-data)
                           (map (fn [[name {:keys [id-offset]}]] [(keyword name) id-offset]))
                           (sort-by second))]
        (->> (interleave base-list (concat (rest base-list) [nil]))
             (partition 2)
            (map (fn [[[id offset] [_ offset']]]
                    [id offset offset'])))))

    (examples
     (create-tmx-tileset-offset-list example-tmx) => '([:kafelki 1 401] [:background 401 nil]))

;;; Now we can define a function to convert a single id from a tmx id-list into
;;; our kind of id.

    (defn- tmx-tile-id-to-our-id [tmx-tileset-offset-list tmx-id]
      (if (= 0 tmx-id)
        nil
        (let [[tileset-id id-offset _]
              (first (filter
                      (fn [[_ _ id-offset-next]] (or (nil? id-offset-next) (< tmx-id id-offset-next)))
                      tmx-tileset-offset-list))]
          [tileset-id (- tmx-id id-offset)])))

    (examples
     (tmx-tile-id-to-our-id (create-tmx-tileset-offset-list example-tmx) 0) => nil
     (tmx-tile-id-to-our-id (create-tmx-tileset-offset-list example-tmx) 2) => [:kafelki 1]
     (tmx-tile-id-to-our-id (create-tmx-tileset-offset-list example-tmx) 410) => [:background 9])

    (defn- id-list-to-tile-matrix [id-list width tmx-tileset-offset-list]
      (->> id-list
           (map (partial tmx-tile-id-to-our-id tmx-tileset-offset-list))
           (partition width)
           (map #(apply vector %))
           (reverse)
           (apply vector)))

    ;; (examples
    ;;  (id-list-to-tile-matrix
    ;;   '(4 410 20, 302 500 1, 0 27 405)
    ;;   3
    ;;   (create-tmx-tileset-offset-list example-tmx))

    ;;  =>

    ;;  [[[:kafelki 3]   [:background 9]  [:kafelki 19]]
    ;;   [[:kafelki 301] [:background 99] [:kafelki 0]]
    ;;   [nil            [:kafelki 26]    [:background 4]]])

    (defn- id-list-to-tiled-layer [id-list width tmx-tileset-offset-list]
      {:layer-type :tiled
       :data (id-list-to-tile-matrix id-list width tmx-tileset-offset-list)})

    ;; (examples
    ;;  (id-list-to-tiled-layer
    ;;   '(4 410 20, 302 500 1, 0 27 405)
    ;;   3
    ;;   (create-tmx-tileset-offset-list example-tmx))

    ;;  =>

    ;;  {:layer-type :tiled
    ;;   :data [[[:kafelki 3]   [:background 9]  [:kafelki 19]]
    ;;          [[:kafelki 301] [:background 99] [:kafelki 0]]
    ;;          [nil            [:kafelki 26]    [:background 4]]]})

;;; Our final function:

    (defn get-all-layers-from-tmx [tmx-data]
      (let [{:keys [width layers]} tmx-data
            offset-list (create-tmx-tileset-offset-list tmx-data)]
        (->> layers
             (vals)
             (map (fn [{:keys [name id-list]}]
                    [(keyword name) (id-list-to-tiled-layer id-list width offset-list)])))))

    ;; (examples

    ;;  (get-all-layers-from-tmx example-tmx)

    ;;  =>

    ;;  '([:foreground
    ;;     {:layer-type :tiled,
    ;;      :data
    ;;      [[[:kafelki 0] [:kafelki 3] [:kafelki 3] [:kafelki 61] [:kafelki 62]]
    ;;       [[:kafelki 23] nil nil nil nil]
    ;;       [[:kafelki 23] nil nil nil nil]]}]
    ;;    [:above
    ;;     {:layer-type :tiled,
    ;;      :data
    ;;      [[nil nil nil nil nil]
    ;;       [nil nil nil nil nil]
    ;;       [nil nil nil nil nil]]}]
    ;;    [:background
    ;;     {:layer-type :tiled,
    ;;      :data
    ;;      [[[:kafelki 400] [:kafelki 400] [:kafelki 400] [:kafelki 400] [:kafelki 400]]
    ;;       [[:kafelki 400] [:kafelki 400] [:kafelki 400] [:kafelki 400] [:kafelki 400]]
    ;;       [[:kafelki 400] [:kafelki 400] [:kafelki 400] [:kafelki 400] [:kafelki 400]]]}]))

;;; In practice, some layer names may have special meaning to the game logic,
;;; but in general, a good practice would be to assume that they will be
;;; rendered in the order presented in the tmx file.
    )

;;;## Additional information for tiles
  (do

;;; From now we introduce a "layer context", which will hold various global
;;; information about layers and will be passed to several more involved
;;; functions. We will define new required keys in this context as we go along.

;;; A tileset map is supposed to hold information about particular tiles in all
;;; tilesets. 
;;; Such a map must exist in context under key :tileset-map

    (def example-context-with-tileset-map
      {:tileset-map {:background {0 {:example-data "a"}}
                     :kafelki {0 {:example-data "b"}
                               3 {:example-data "c"}
                               23 {:example-data "dd"}
                               61 {:example-data "eee"}
                               62 {:example-data "ffff"}}}})

;;; And for convenience:

    (defn get-tile-info-from-layer [context layer x y]
      (let [{:keys [tileset-map]} context
            [tileset-id tile-id] (get-tile-from-layer layer x y)]
        (when-let [ts-map (tileset-map tileset-id)]
          ;;(.log js/console (str "tile " tile-id " -> " (pr-str (ts-map tile-id))))
          (ts-map tile-id))))

    (defn v2-get-tile-info-from-layer [context layer x y]
      (let [{:keys [tileset-map]} context
            [tileset-id tile-id] (v2-get-tile-from-layer layer x y)]
        (when-let [ts-map (tileset-map tileset-id)]
          ;;(.log js/console (str "tile " tile-id " -> " (pr-str (ts-map tile-id))))
          (ts-map tile-id))))

    
    (examples

     (get-tile-info-from-layer
      example-context-with-tileset-map
      {:layer-type :tiled
       :data [[[:kafelki 0] [:kafelki 1] [:inny-set 5]]
              [[:kafelki 1] [:kafelki 1] [:inny-set 55]]
              [[:kafelki 2] [:kafelki 1] [:inny-set 5]]
              [[:kafelki 3] [:kafelki 1] [:inny-set 5]]]}
      0 3)

     =>

     {:example-data "c"})
    )

;;;## Rendering information for tiles
  (do

;;; In order to provide rendering information, we'll need some extra keys in our context.

    (def example-context-with-rendering-info

      {:tile-width 32
       :tile-height 32
       :world-width-in-tiles 0 ;; TODO
       :word-height-in-tiles 0 ;; TODO

;;; We also want to be able to use different rendering of logically same tiles
;;; by providing multiple image files for the same tileset. And so to render our
;;; layers we'll also need a map with an image file defined for each tileset
;;; name. The idea is that this configuration could even be changed at runtime
;;; causing an immediate change in tile appearence.

       :tileset-rendering-map {:background {:img "background.png"
                                            :width-in-tiles 5}
                               :kafelki {:img "tiles.png"
                                         :width-in-tiles 3}}})

;;; We can also extract a tileset rendering map from a tmx file:

    (defn get-tileset-rendering-map-from-tmx [tmx-data]
      (let [{:keys [tilesets]} tmx-data]
        (->> tilesets
             (vals)
             (mapcat (fn [{:keys [name image-file-name tile-columns]}]
                       [(keyword name) {:img image-file-name
                                        :width-in-tiles tile-columns}]))
             (apply hash-map))))

    (defn v2-get-tileset-rendering-map-from-tmx [tmx-data]
      (let [{:keys [tilesets]} tmx-data]
        (->> tilesets
             (vals)
             (mapcat (fn [{:keys [name image-file-name tile-columns]}]
                       [(keyword name) {:img image-file-name
                                        :width-in-tiles tile-columns}]))
             (apply hash-map))))
    
    (examples

     (get-tileset-rendering-map-from-tmx example-tmx)

     =>

     {:background {:img "background.png" :width-in-tiles 10}
      :kafelki {:img "tiles.png" :width-in-tiles 20}}
     )

;;; Now a function that will fetch all information required to render a tile.

    (defn get-rendering-information-for-tile
      ([context id] (let [{:keys [tileset-rendering-map world-width-in-tiles
                                  tile-width tile-height]} context
                          [tileset-id tile-id] id
                          {:keys [img width-in-tiles]} (tileset-rendering-map tileset-id)
                          row (quot tile-id width-in-tiles)
                          col (mod tile-id width-in-tiles)]
                      {:img img
                       :x (* tile-width col)
                       :y (* tile-height row)
                       :w tile-width
                       :h tile-height}))
      ([context tiled-layer x y] (get-rendering-information-for-tile
                                  context
                                  (get-tile-from-layer tiled-layer x y))))

    (defn v2-get-rendering-information-for-tile
      ([context id] (let [{:keys [tileset-rendering-map world-width-in-tiles
                                  tile-width tile-height]} context
                          [tileset-id tile-id] id
                          {:keys [img width-in-tiles]} (tileset-rendering-map tileset-id)
                          row (quot tile-id width-in-tiles)
                          col (mod tile-id width-in-tiles)]
                      {:img img
                       :x (* tile-width col)
                       :y (* tile-height row)
                       :w tile-width
                       :h tile-height}))
      ([context tiled-layer x y] (get-rendering-information-for-tile
                                  context
                                  (v2-get-tile-from-layer tiled-layer x y))))


    (examples
     (get-rendering-information-for-tile
      example-context-with-rendering-info
      [:kafelki 5])
     =>
     {:img "tiles.png", :x 64, :y 32, :w 32, :h 32})
    ))

;;;# Background layers
(do
  ;; layery tla jako pojedynczej bitmapy, byc moze przesuwanej
  ;; z inna predkoscia (pseudo-3d).

  ;; TODO - pozniej...
  )

;;;# Custom layers
(do

;;; To ostatnie by sluzylo np. do malowania deszczu. Bo to by bylo jakos
;;; proceduralnie, czyli taki layer to bylaby po prostu jakas funkcja draw jego
;;; wlasna, a ogolny mechanizm dbalby tylko o to, zeby wywolac ja w odpowiednim
;;; momencie. Na takim layerze moglyby tez znajdowac sie np. jakies elementy UI,
;;; czy oznaczenia, podpisy itp.

;;; UWAGA!!! To nie moze byc funkcja wklejona, bo chcemy miec czyste dane,
;;; np. zapisane do redisa.
;;; Czyli znowu jakas multimetoda (draw-custom-layer), ktora bedzie dispatchowana
;;; po jakiejs wartosci :custom-layer-type czy cos. I w danych bedzie tylko
;;; ta wartosc, a implementacje w ogolnym kodzie.


  ;; TODO - pozniej...

  )

;;;# Synopsis

;; (examples

;; ;;;## Basic operations

;; ;;; - create a tiled layer filled with one tile id:

;;  (clean-layer 5 3 [:kafelki 3])
;;  =>
;;  {:layer-type :tiled
;;   :data [[[:kafelki 3] [:kafelki 3] [:kafelki 3] [:kafelki 3] [:kafelki 3]]
;;          [[:kafelki 3] [:kafelki 3] [:kafelki 3] [:kafelki 3] [:kafelki 3]]
;;          [[:kafelki 3] [:kafelki 3] [:kafelki 3] [:kafelki 3] [:kafelki 3]]]}

;; ;;; - substitute a single tile in layer

;;  (set-tile-in-layer
;;   {:layer-type :tiled
;;    :data [[[:kafelki 0] [:kafelki 1] [:inny-set 5]]
;;           [[:kafelki 1] [:kafelki 1] [:inny-set 5]]
;;           [[:kafelki 2] [:kafelki 1] [:inny-set 5]]
;;           [[:kafelki 3] [:kafelki 1] [:inny-set 5]]]}
;;   1 3
;;   [:ten-zmieniony 10])
;;  =>
;;  {:layer-type :tiled
;;   :data [[[:kafelki 0] [:kafelki 1] [:inny-set 5]]
;;          [[:kafelki 1] [:kafelki 1] [:inny-set 5]]
;;          [[:kafelki 2] [:kafelki 1] [:inny-set 5]]
;;          [[:kafelki 3] [:ten-zmieniony 10] [:inny-set 5]]]}

;; ;;; - get tile id from laer at given x, y position

;;  (get-tile-from-layer
;;   {:layer-type :tiled
;;    :data [[[:kafelki 0] [:kafelki 1] [:inny-set 5]]
;;           [[:kafelki 1] [:kafelki 1] [:inny-set 55]]
;;           [[:kafelki 2] [:kafelki 1] [:inny-set 5]]
;;           [[:kafelki 3] [:kafelki 1] [:inny-set 5]]]}
;;   2 1)
;;  =>
;;  [:inny-set 55]

;; ;;;## Loading tiled layers from tmx files

;;  (get-all-layers-from-tmx
;;  '{:width 5
;;         :height 3
;;         :tile-width 32
;;         :tile-height 32
;;         :tilesets {"background" {:name "background"
;;                                  :id-offset 401
;;                                  :tile-width 32
;;                                  :tile-height 32
;;                                  :tile-count 40
;;                                  :tile-columns 10
;;                                  :image-file-name "background.png"
;;                                  :image-file-width 320
;;                                  :image-file-height 128}
;;                    "kafelki"    {:name "kafelki"
;;                                  :id-offset 1
;;                                  :tile-width 32
;;                                  :tile-height 32
;;                                  :tile-count 400
;;                                  :tile-columns 20
;;                                  :image-file-name "tiles.png"
;;                                  :image-file-width 640
;;                                  :image-file-height 640}}
;;         :layers {"foreground" {:name "foreground"
;;                                :width 5
;;                                :height 3
;;                                :id-list (1 4 4 62 63 24 0 0 0 0 24 0 0 0 0)}
;;                  "above"      {:name "above"
;;                                :width 5
;;                                :height 3
;;                                :id-list (0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)}
;;                  "background" {:name "background"
;;                                :width 5
;;                                :height 3
;;                                :id-list
;;                                (401 401 401 401 401 401 401 401 401 401 401 401 401 401 401)}}})
;;  =>
;;  '([:foreground
;;     {:layer-type :tiled,
;;      :data
;;      [[[:kafelki 0] [:kafelki 3] [:kafelki 3] [:kafelki 61] [:kafelki 62]]
;;       [[:kafelki 23] nil nil nil nil]
;;       [[:kafelki 23] nil nil nil nil]]}]
;;    [:above
;;     {:layer-type :tiled,
;;      :data
;;      [[nil nil nil nil nil]
;;       [nil nil nil nil nil]
;;       [nil nil nil nil nil]]}]
;;    [:background
;;     {:layer-type :tiled,
;;      :data
;;      [[[:kafelki 400] [:kafelki 400] [:kafelki 400] [:kafelki 400] [:kafelki 400]]
;;       [[:kafelki 400] [:kafelki 400] [:kafelki 400] [:kafelki 400] [:kafelki 400]]
;;       [[:kafelki 400] [:kafelki 400] [:kafelki 400] [:kafelki 400] [:kafelki 400]]]}])

;; ;;;## Additional information for tiles

;;  (get-tile-info-from-layer
;;   #_"context"
;;   {:tileset-map {:background {0 {:example-data "a"}}
;;                  :kafelki {0 {:example-data "b"}
;;                            3 {:example-data "c"}
;;                            23 {:example-data "dd"}
;;                            61 {:example-data "eee"}
;;                            62 {:example-data "ffff"}}}}
;;   #_"tiled layer"
;;   {:layer-type :tiled
;;    :data [[[:kafelki 0] [:kafelki 1] [:inny-set 5]]
;;           [[:kafelki 1] [:kafelki 1] [:inny-set 55]]
;;           [[:kafelki 2] [:kafelki 1] [:inny-set 5]]
;;           [[:kafelki 3] [:kafelki 1] [:inny-set 5]]]}

;;   0 #_"x" 3 #_"y")
;;  =>
;;  {:example-data "c"}

;; ;;;## Rendering information for tiles

;; ;;; - reading a tileset map from a tmx file: can be put into context
;;  (get-tileset-rendering-map-from-tmx
;;   '{:width 5
;;     :height 3
;;     :tile-width 32
;;     :tile-height 32
;;     :tilesets {"background" {:name "background"
;;                              :id-offset 401
;;                              :tile-width 32
;;                              :tile-height 32
;;                              :tile-count 40
;;                              :tile-columns 10
;;                              :image-file-name "background.png"
;;                              :image-file-width 320
;;                              :image-file-height 128}
;;                "kafelki"    {:name "kafelki"
;;                              :id-offset 1
;;                              :tile-width 32
;;                              :tile-height 32
;;                              :tile-count 400
;;                              :tile-columns 20
;;                              :image-file-name "tiles.png"
;;                              :image-file-width 640
;;                              :image-file-height 640}}
;;     :layers {"foreground" {:name "foreground"
;;                            :width 5
;;                            :height 3
;;                            :id-list (1 4 4 62 63 24 0 0 0 0 24 0 0 0 0)}
;;              "above"      {:name "above"
;;                            :width 5
;;                            :height 3
;;                            :id-list (0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)}
;;              "background" {:name "background"
;;                            :width 5
;;                            :height 3
;;                            :id-list
;;                            (401 401 401 401 401 401 401 401 401 401 401 401 401 401 401)}}})
;;  =>
;;  {:background {:img "background.png" :width-in-tiles 10}
;;   :kafelki {:img "tiles.png" :width-in-tiles 20}}

;; ;;; - getting rendering information for given tile

;;  (get-rendering-information-for-tile
;;   #_"context"
;;   {:tile-width 32
;;    :tile-height 32
;;    :world-width-in-tiles 0 ;; TODO
;;    :word-height-in-tiles 0 ;; TODO
;;    :tileset-rendering-map {:background {:img "background.png"
;;                                         :width-in-tiles 5}
;;                            :kafelki {:img "tiles.png"
;;                                      :width-in-tiles 3}}}
;;   #_"tile id"
;;   [:kafelki 5])
;;  =>
;;  {:img "tiles.png", :x 64, :y 32, :w 32, :h 32})


