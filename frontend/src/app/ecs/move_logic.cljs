(ns app.ecs.move-logic)

;; Tutaj od nowa logika ruchu ludzika
;; i tak uogolniona, zeby mini villager tez zadzialal.
;;
;; W sumie mozna obsluzyc ludzika dowolnej wysokosci, a szerokosci 1.
nil

;; 7. Teraz wyznaczamy D = A and B and C.
;; 8. Moze nam pozostac wiecej niz jeden ruch (np. w wypadku schodow). Trzeba
;;    wprowadzic jakas priorytetyzacje, zeby wybrac jeden. TODO.
;;
;; OPTYMALIZACJA
;;
;; Zastosowac symetrie lewo-prawo, zeby uproscic sprawe.
nil

;; Definition of tile map:
;; {key tile}, where key:
;; -1..n : tile in front at the given relative Y
;; :above : tile right above head
;; :under : tile right under feet
nil

;; Definition of properties:
;; {:height n}

(defn conj-if [set condition key]
  (if condition
    (conj set key)
    set))

(defn disj-if [set condition key]
  (if condition
    (disj set key)
    set))


;; Plan wyznaczania ruchu:
;;
;; 1. Wziac zbior wszystkich ruchow, czyli w 8 kierunkach.
(def all-moves-in-preference-order
  [:stay :fwd :fwd-up :fwd-down :up :turn :down])

;; 2. Odjac od tego wszystko co jest zablokowanie przez kafelki :solid.
;; 3. To co zostaje to zbior A.
(defn remove-blocked [{:as properties :keys [height]} tile-map moves]
  (-> moves
      (disj-if (:solid? (tile-map :above)) :up)
      (disj-if (:solid? (tile-map :under)) :down)
      (disj-if (some #(:solid? (tile-map %)) (range height)) :fwd)
      (disj-if (some #(:solid? (tile-map %)) (map inc (range height))) :fwd-up)
      (disj-if (some #(:solid? (tile-map %)) (map dec (range height))) :fwd-down)))

(comment
  (def test-properties
    {:height 3})

  (def test-tile-map
    {:above {:solid? true}                3 {:solid? true}
                                          2 nil
     ,                                    1 {:solid? true}
     ,                                    0 nil
     :under {:solid? true}               -1 {:solid? true}
    })

  (remove-blocked test-properties test-tile-map all-theoretical-moves)
  )


;; 4. Zaczynajac teraz od pustego zbioru dodac wszystko co moze potencjalnie
;;    *umozliwic* ruch: np. jesli jestesmy "flying", to 4 ruchy dodamy
;;    (lewo, prawo, gora, dol). Albo bez sprawdzania "flying", jezeli
;;    jest podtrzymujacy kafelek pod nami i przed nami, to mozemy pojsc
;;    w tamta strone. Jesli pod nami i przed nami oczko wyzej, to ruch po skosie.
;; 5. To co otrzymamy to zbior B.
(defn produce-supported [{:as properties :keys [height flight-mode]} tile-map]
  (let [normal
        (-> #{}
            (conj-if (not (:solid? (tile-map :under))) :down) ;; fall or fly downwards

            ;; TODO: fly upwards

            (conj-if (:solid? (tile-map :under)) :stay)
            (conj-if (:supports? (tile-map :under)) :stay)
            ;; TODO: fly stay
            
            (conj-if (:solid? (tile-map :under)) :turn)
            (conj-if (:supports? (tile-map :under)) :turn)
            ;; TODO: fly turn

            (conj-if (and (or (:solid? (tile-map :under))
                              (:supports? (tile-map :under)))
                          (or (:solid? (tile-map -1))
                              (:supports? (tile-map -1))))
                     :fwd)
            ;; TODO: fly fwd

            (conj-if (and (or (:solid? (tile-map :under))
                              (:supports? (tile-map :under)))
                          (or (:solid? (tile-map 0))
                              (:supports? (tile-map 0))))
                     :fwd-up)

            (conj-if (or (:solid? (tile-map :under))
                         (:supports? (tile-map :under)))
                     :fwd-down))]
    (if flight-mode
      (conj normal :fwd :up :down :turn :stay)
      normal)))

(comment
  (def test-properties
    {:height 3})

  (def test-tile-map
    {:above {:solid? true}                3 {:solid? true}
                                          2 nil
     ,                                    1 {:solid? true}
     ,                                    0 {:solid? true}
     :under {:solid? true}               -1 {:solid? true}
    })

  (produce-supported test-properties test-tile-map)
  )

;; 6. Wreszcie, znow zaczynajac od pustej listy, badamy input od usera
;;    i wyznaczamy liste L - ruchow, ktore chcialby zrobic user,
;;    w kolejnosci preferencji.
nil

;; user-input
;; #{:fwd :rev :up :down}

(defn produce-intended [{:as input :keys [fwd rev up down]}]
  (let [intended-horizontal (case [(boolean fwd) (boolean rev)]
                              [true false] :fwd
                              [false true] :rev
                              nil)
        intended-vertical (case [(boolean up) (boolean down)]
                            [true false] :up
                            [false true] :down
                            nil)]
    (case [intended-horizontal intended-vertical]
      [nil nil]    [:stay]
      [nil :up]    [:up]
      [nil :down]  [:down]
      [:fwd nil]   [:fwd :fwd-up :fwd-down]
      [:fwd :up]   [:fwd-up :fwd :fwd-down]
      [:fwd :down] [:fwd-down :fwd :fwd-up]
      [:rev nil]   [:turn]
      [:rev :up]   [:turn]
      [:rev :down] [:turn])))

(comment
  (do
    (def test-properties
      {:height 3})

    (def test-tile-map
      {:above {:solid? true}                3 nil ;{:solid? true}
       ,                                    2 nil
       ,                                    1 nil
       ,                                    0 {:supports? true}
       :under {:solid? true}               -1 {:solid? true}
       })

    (def test-input {:fwd true
                     :rev false
                     :up  true
                     :down false})
    (def supported (produce-supported test-properties test-tile-map))

    (def possible (remove-blocked test-properties test-tile-map supported))

    (def intended (produce-intended test-input))

    (def intended-possible (filter possible intended))

    (def best-intended-possible (first intended-possible))

    (def actual-move
      (if best-intended-possible
        best-intended-possible
        (first (filter possible all-moves-in-preference-order))))

    actual-move
    )


  )


;; example tile: {:ids [:stone] :solid? true}






;;     +-------+-------+
;;     |       |       |
;;  n  |       |       |
;;     |       |       |                 Czyli mamy 6 mozliwych ruchow,
;;     +-------+-------+                 liczac z pozostaniem w miejscu.
;;     | ###   |       |                 Jak doliczyc symetryczne ruchy w lewo,
;; n-1 |#####  |       |                 no to 9.
;;     | ###   |       |
;;     +-------+-------+
;;       ...      ...
;;     +-------+-------+
;;     | ###   |       |                 Parametry kafelkow:
;;  1  | ###   |       |
;;     | ###   |       |                 :solid - wiadomo, nie mozna tam byc
;;     +-------+-------+                 :supports - mozna na nim stac
;;     | ###   |       |                 (btw. :solid => :supports)
;;  0  | ###   |       |                 btw. moze by go nazwac :top-solid,
;;     | ##### |       |                 co oddaje fakt, ze mozna na nim stac
;;     +-------+-------+                 ale czy tez to, ze nie mozna w niego
;;     |       |       |                 wejsc z gory?
;; -1  |       |       |
;;     |       |       |                 A moze... rozbic to wszystko na mniejsze
;;     +-------+-------+                 parametry i tylko robic ich zestawy.
;;                                       Np.
;;                                         :allows-in-from <set of direction>
;;                                       I wtedy :solid jest rownowazne:
;;                                         :allows-in-from #{:left :right :top :down}
;;                                       Natomiast :supports jest w sumie szczegolne,
;;                                       bo nie mialoby sensu dla innych kierunkow niz top.
;;                                       Ale plytka schodow moze byc teraz dwojakiego rodzaju:
;;                                       1) normalna
;;                                         {:supports true
;;                                          :allows-in-from #{:left :right :down}}
;;                                       2) taka, z ktorej mozna spasc w dol, jesli gracz
;;                                          wyrazi taki zamiar (ale nie sama grawitacja):
;;                                         {:supports true
;;                                          :allows-in-from #{:top :left :right :down}}
;;                                          Dlaczego to zadziala? No bo supports
;;                                          da nam mozliwosc pozostania w miejscu,
;;                                          zas :allows-in-from :top da mozliwosc spadniecia
;;                                          w dol, ale jesli user np. nic nie naciska,
;;                                          to preferenca bedzie zostac, natomiast jesli
;;                                          naciska w dol, to preferencja bedzie spasc.
;;
;;              Z TEGO WYNIKA, ZE PRIORYTETYZACJA RUCHOW (ostatni punkt algorytmu)
;;              MUSI ZALEZEC OD INTENCJI GRACZA (CZYLI JEGO INPUTU)

;; W przyszlosci mozna bedzie dodac drabiny (chodzenie pionowe).
;; To latwe: to bedzie do zbioru B (umozliwianie ruchu).

;; W przyszlosci jeszcze Feliks bedzie chcial zeby byly skoki. To trudne,
;; bo dojdzie jeszcze jeden input: w jakim jestesmy stanie skoku. Potem pytanie,
;; czy w trakcie skoku mozna cos zmienic, ale na pewno stan skoku komplikuje sprawe.

;; A do tego jeszcze polowkowe pozycje z animacja chodzenia. Ale to deterministycznie,
;; decyduje sie caly krok, a potem tylko posrednia faza bez zadnej analizy wyswietla
;; sie po ustalonym czasie.
