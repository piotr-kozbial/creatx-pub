(ns app.ecs.box2d-system)

;; TODO

;; Tutaj to co w scratch.js odnosnie box2d.
;; Zrobic komponent "box2d-body".
;; Niech to bedzie opis typu strukturka (jak w komen tarzu w scratch.cjls).
;; I niech dopiero :init ja konstruuje.

;; Ale jeszcze. Niech bedzie to indirect. No bo box2d jest stateful i to sa okropne obiekty.
;; Wiec niech sam niniejszy modul trzyma obiekt modulu box2d (co i tak trzeba: (def b2 (js/Box2D.)))
;; ale i katalog obiektow po kluczach (numerek auto-increment?).

;; I wtedy w komponencie bedzie tylko opis (strukturka) i numerek.

;; I wtedy przy save game zapisze sie strukturka i numerek.
;; Ale to malo. Trzeba jeszcze wydostac stan - polozenie, oraz dla wszystkich fixtures
;; trzeba miec polozenie predkosc i rotacje chyba.
;; Moze cos gotowego jest w box2d, ale to pewnie i tak ohydne.

;; Czyli moze tak:
;;  - w komponencie *tylko* numerek (moze i fixtures beda mialy wlasne numerki?,
;;                                        gdyby komponent chcial na nie jakos dzialac),
;;  - zapis i odczyt stanu komponentow przy save/load game bedzie normalnie, bez niczego.
;;  - natomiast specjalnie bedzie zapisywany i odczytywany system box2d - wszystkie bodies,
;; ich strukturki, polozenia, predkosci, rotacje itd.
;;  - i przy odczycie (load game) beda odtwarzane wszystkie obiekty box2d w ramach
;;   systemu box2d, pod tymi samymi numerami.
