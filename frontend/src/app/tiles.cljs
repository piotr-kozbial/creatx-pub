(ns app.tiles)

;; We'll define tiles with double identification: a keyword and the number of tile in tiles.png.
;; So first we define them in a list and then we'll convert them into two maps.
;; We'll also add synonyms, so one tile will be identified by multiple keywords.
;;
;; :number - tile graphics number as in the tiles.png, starting from zero,
;;           left-to-right (20 tiles in a row), then top-to-bottom,
;; :ids - identifiers to be used in a code (all equivalent),
;; :tracks - railway tracks [from to], but ordering is irrelevant.
nil


(defmulti initialize-tile-extra (fn [tile-id tile-x tile-y tile-info] tile-id) :default nil)

(defmethod initialize-tile-extra nil
  [tile-id tile-x tile-y tile-info]
  nil)

;;=== Terminology ===================================
;; tile id - a keyword, such as :ground, :spaceship-plate; unique across all tilesets
;; tile-coords - [tileset number], such as [:kafelki 5]
nil

;;=== Tileset definitions ===========================


(def tileset-rendering-map
  {:kafelki {:img "tiles.png", :width-in-tiles 20}
   :lights  {:img "tilesets/lights.png", :width-in-tiles 20}
   :cactuses {:img "tilesets/cactuses.png", :width-in-tiles 20}})

(def -tiles
  {:kafelki
   {0   {:ids [:ground] :solid? true}
    1   {:ids [:stone] :solid? true}
    2   {:ids [:water] :solid? false}
    3   {:ids [:lava] :solid? false}
    4   {:ids [:spaceship-plate] :solid? true}
    5   {:ids [:stone-brick] :solid? true}
    6   {:ids [:chest] :solid? false}
    7   {:ids [:sand] :solid? true}
    8   {:ids [:non-solid-sand] :solid? false}
    9   {:ids [:snow] :solid? true}
    10  {:ids [:dog-house] :solid? false}
    11  {:ids [:dog-house-with-bg] :solid? false}
    13  {:ids [:dog-house-1] :solid? false}
    14  {:ids [:dog-house-2] :solid? false}
    15  {:ids [:dog-house-with-bg-1] :solid? false}
    16  {:ids [:dog-house-with-bg-2] :solid? false}
    20  {:ids [:ground-with-grass] :solid? true}
    21  {:ids [:hout] :solid? true}
    22  {:ids [:leaves] :solid? true}
    23  {:ids [:non-solid-hout] :solid? false}
    24  {:ids [:gold-block] :solid? true}
    25  {:ids [:bell] :solid? false}
    26  {:ids [:gold-chest] :solid? false, :glow? true}
    27  {:ids [:gold-platform] :solid? false :supports? true :glow? true}
    28  {:ids [:gold-window] :solid? false, :glow? true}
    29  {:ids [:tnt] :solid? true}
    30  {:ids [:small-villager] :solid? false}
    31  {:ids [:wood-platform] :solid? false :supports? true}
    33  {:ids [:dog-house-3] :solid? false}
    34  {:ids [:dog-house-4] :solid? false}
    35  {:ids [:dog-house-with-bg-3] :solid? false}
    36  {:ids [:dog-house-with-bg-4] :solid? false}
    40  {:ids [:grass] :solid? false}
    41  {:ids [:green-flower] :solid? false}
    42  {:ids [:yellow-flower] :solid? false}
    43  {:ids [:red-flower] :solid? false}
    47  {:ids [:gold-door-top] :solid? false, :glow? true}
    48  {:ids [:wood-fence] :solid? false}
    51  {:ids [:ladder] :solid? false :supports? true}
    67  {:ids [:gold-door-middle] :solid? false, :glow? true}
    87  {:ids [:gold-door-bottom] :solid? false, :glow? true}
    60  {:ids [:green-teleport] :solid? false}
    61  {:ids [:match] :solid? false}
    62  {:ids [:flame] :solid? false}
    80  {:ids [:lamp] :solid? false}
    82  {:ids [:cactus] :solid? true}
    100 {:ids [:yellow-teleport] :solid? false}
    101 {:ids [:red-teleport] :solid? false}
    102 {:ids [:blue-teleport] :solid? false}
    103 {:ids [:iron-ore] :solid? true}
    104 {:ids [:diamond-ore] :solid? true}
    105 {:ids [:coal-ore] :solid? true}
    106 {:ids [:gold-ore] :solid? true}
    107 {:ids [:redstone-ore] :solid? true}
    108 {:ids [:emerald-ore] :solid? true}
    109 {:ids [:lapis-lazuli-ore] :solid? true}
    110 {:ids [:button] :solid? false}
    111 {:ids [:lever-right] :solid? false}
    112 {:ids [:lever-left] :solid? false}



    }
   :lights
   {0   {:ids [:lights/street-retro-big] :solid? false :glow? true}
    1   {:ids [:lights/street-retro-triple] :solid? false :glow? true}
    2   {:ids [:lights/street-retro-small] :solid? false :glow? true}
    3   {:ids [:lights/street-retro-left] :solid? false :glow? true}
    4   {:ids [:lights/street-retro-right] :solid? false :glow? true}

    5   {:ids [:lights/street-globe-big] :solid? false :glow? true}
    6   {:ids [:lights/street-globe-triple] :solid? false :glow? true}
    7   {:ids [:lights/street-globe-small] :solid? false :glow? true}
    8   {:ids [:lights/street-globe-left] :solid? false :glow? true}
    9   {:ids [:lights/street-globe-right] :solid? false :glow? true}

    10  {:ids [:lights/street-boring-left] :solid? false :glow? true}
    11  {:ids [:lights/street-boring-right] :solid? false :glow? true}
    12  {:ids [:lights/street-boring-center] :solid? false :glow? true}

    13  {:ids [:lights/torch-center] :solid? false :glow? true}
    14  {:ids [:lights/torch-right] :solid? false :glow? true}
    15  {:ids [:lights/torch-left] :solid? false :glow? true}
    16  {:ids [:lights/torch-double] :solid? false :glow? true}
    17  {:ids [:lights/torch-triple] :solid? false :glow? true}

    20  {:ids [:lights/street-pole-big-ring] :solid? false}
    21  {:ids [:lights/street-pole-ring] :solid? false}
    22  {:ids [:lights/street-pole] :solid? false}
    23  {:ids [:lights/street-pole-right] :solid? false}
    24  {:ids [:lights/street-pole-left-base] :solid? false}
    26  {:ids [:lights/street-pole-big-base] :solid? false}
    27  {:ids [:lights/street-pole-base] :solid? false}
    28  {:ids [:lights/street-pole-thin-right] :solid? false}
    29  {:ids [:lights/street-pole-thin-left-base] :solid? false}
    43  {:ids [:lights/street-pole-left] :solid? false}
    44  {:ids [:lights/street-pole-right-base] :solid? false}
    48  {:ids [:lights/street-pole-thin-left] :solid? false}
    49  {:ids [:lights/street-pole-thin-right-base] :solid? false}}
   :cactuses
   {
     0 {:ids [:cactus/big-00] :solid? true}
     1 {:ids [:cactus/big-01] :solid? true}
     2 {:ids [:cactus/big-02] :solid? true}
    20 {:ids [:cactus/big-10] :solid? true}
    21 {:ids [:cactus/big-11] :solid? true}
    22 {:ids [:cactus/big-12] :solid? true}
    40 {:ids [:cactus/big-20] :solid? true}
    41 {:ids [:cactus/big-21] :solid? true}
    42 {:ids [:cactus/big-22] :solid? true}
    60 {:ids [:cactus/big-30] :solid? true}
    61 {:ids [:cactus/big-31] :solid? true}
    62 {:ids [:cactus/big-32] :solid? true}
    80 {:ids [:cactus/big-40] :solid? true}
    81 {:ids [:cactus/big-41] :solid? true}
    82 {:ids [:cactus/big-42] :solid? true}
    100 {:ids [:cactus/flower-yellow] :solid? true}
    101 {:ids [:cactus/potted-with-yellow-flower] :solid? true}
    102 {:ids [:cactus/potted] :solid? true}}})


;;=== Derived data ==================================

;; tileset -> list of expanded tiles
(def -tiles-expanded
  (->> -tiles
       (mapcat (fn [[tileset tl-map]]
                 [tileset
                  (->> tl-map
                       (map (fn [[number tile]]
                              (assoc tile
                                     :tileset tileset
                                     :nuumber number))))]))
       (apply hash-map)))

(def -tiles-by-id
  (->> (apply concat (vals -tiles-expanded))
       (mapcat #(interleave (:ids %) (take (count (:ids %)) (repeat %))))
       (apply hash-map)))
;; tile-id -> [tileset number]
(defn tile-coords-by-id [tile-id]
  (let [{:keys [tileset nuumber]} (-tiles-by-id tile-id)]
    [tileset nuumber]))

(defn -tiles-by-numberr [tiles]
  (->> tiles
       (mapcat #(vector (:nuumber %) %))
       (apply hash-map)))
;; tileset -> number -> expanded tile
(def tileset-map
  (->> -tiles-expanded
       (mapcat (fn [[tileset tiles]]
                 [tileset (-tiles-by-numberr tiles)]))
       (apply hash-map)))

;;=== Constants for specific tile coords ============

(def CHEST [:kafelki 6])
(def GOLD-CHEST [:kafelki 26])
(def TNT [:kafelki 29])

(def GOLD [:kafelki 24])
;; This is not a real, permanent tile. Just for displaying the icon. TODO - zrobic porzadek
(def MATCH [:kafelki 61])
(def FLAME [:kafelki 62])
(def EXPLOSION [:kafelki 81])

(def LAVA [:kafelki 3])

(def LAMP [:kafelki 80])

(def SMALL-VILLAGER [:kafelki 30])
