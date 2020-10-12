(ns app.lang)

(defmacro zzz []
  20)


(declare def+-expand defn+-expand)





(defn +-expand-statement [stmt]
 (if (list? stmt)
    (cond
      (= (first stmt) 'def+) (def+-expand (rest stmt))
      (= (first stmt) 'defn+) (defn+-expand (rest stmt))
      :else stmt)
    stmt))

(defn +-process-statement [stmt]
  (if (list? stmt)
    (cond
      (= (first stmt) 'def) (let [[_ left-side right-side] stmt] [:def left-side right-side])
      (= (first stmt) 'defn) (let [[_ left-side args & body] stmt] [:defn left-side args body])
      :else [:void stmt])
    [:void stmt]))

(defn +-translate-let-statement [[type & args]]
  (case type
    :def (let [[destructor expr] args] [destructor expr])
    :defn (let [[sym args body] args] [sym (apply list 'fn args body)])
    :void (let [[expr] args] ['_ expr])))

(defn +-body-expand [body]
  (let [statements-expanded (map +-expand-statement body)
        statements-processed (map +-process-statement statements-expanded)
        [statements-for-let return-expr]
        (if (= (first (last statements-processed)) :void)
          [(butlast statements-processed) (second (last statements-processed))]
          [statements-processed nil])
        args-for-let (mapcat +-translate-let-statement statements-for-let)]

    [args-for-let return-expr]))

(defn def+-expand [[destructor & body]]
  (let [[args-for-let return-expr] (+-body-expand body)]
    (list 'def destructor
          `(let [~@args-for-let]
             ~return-expr))))

(defmacro def+ [& body]
  (def+-expand body))

(defn defn+-expand [[function-symbol function-args & body]]
  (let [[args-for-let return-expr] (+-body-expand body)]
    (list 'defn function-symbol function-args
          `(let [~@args-for-let]
             ~return-expr))))

(defmacro defn+ [& body]
  (defn+-expand body))



;; (defn defn++-expand [args]
;;   (let [[function-symbol function-args & body] args]
;;     (let [statements-expanded (map defn+-expand-statement body)
;;           statements-processed (map defn+-process-statement statements-expanded)
;;           ;; [statements-for-let return-expr]
;;           ;; (if (= (first (last statements-processed)) :void)
;;           ;;   [(butlast statements-processed) (second (last statements-processed))]
;;           ;;   [statements-processed nil])
;;           ;; args-for-let (mapcat defn+-translate-let-statement statements-for-let)
;;           ]
;;       (println
;;        (pr-str
;;         [statements-expanded
;;          (map list? statements-expanded)
;;          statements-processed]))
;;       nil
;;       )))

;; (defmacro defn++ [& body]
;;   (defn++-expand body))


(comment

  (defn+ foo [x y]
    (let [sum (+ x y)]
      (* sum sum)))

  (defn+ foo [x y]
    (defn+ sumowator+ [a b]
      (defn sumowator [a b] (+ a b))
      (sumowator a b))
    (def sum (sumowator+ x y))
    (println "hej")
    #_(def+ factor
      (def dwa 2)
      (def trzy 3)
      (+ dwa trzy))
    (def+ factor
      (def+ [dwa trzy] [2 3])
      (+ dwa trzy))

    (* sum sum factor))

  
  (foo 2 3)

  )
