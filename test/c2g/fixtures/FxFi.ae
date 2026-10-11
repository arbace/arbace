;; c2g's fixtures (see FxCode.clj): Clojure inside class bodies passing a fn where Java expects
;; a functional interface: the analyzer's :fi-adapter (Compiler's FISupport adapter, C2G-SPEC
;; §7.11, C2G-NOTES.md phase 2B), a fn adapted to Function, BiFunction and BiConsumer through
;; FnInvokers, and an argument that already is an instance of the interface (a lambda), which
;; stays itself. The calls are in Clojure fns (the analyzer's Clojure context).

(in-ns 'c2g.fixtures)

(import '(java.util HashMap)
        '(java.util.function Function))

(do

(defclass ^:public FxFi
  (method ^:public ^:static tFiFunction ^String []
    (let [m (HashMap.)
          run (fn [^HashMap hm]
                (.computeIfAbsent hm "a" (fn [k] (.concat ^String k "!")))
                (.computeIfAbsent hm "b" (fn [k] (.concat ^String k "!"))))]
      (.invoke run m)
      (java-str (.get m "a") (.get m "b") (.size m))))

  (method ^:public ^:static tFiBiFunction ^String []
    (let [m (HashMap.)
          run (fn [^HashMap hm]
                (.merge hm "x" "2" (fn [a b] (.concat ^String a ^String b)))
                (.merge hm "y" "3" (fn [a b] (.concat ^String a ^String b))))]
      (.put m "x" "1")
      (.invoke run m)
      (java-str (.get m "x") "," (.get m "y"))))

  (method ^:public ^:static tFiBiConsumer ^String []
    (let [m (HashMap.)
          sb (StringBuilder.)
          run (fn [^HashMap hm ^StringBuilder b]
                (.forEach hm (fn [k v] (.append b (.concat ^String k ^String v)))))]
      (.put m "a" "1")
      (.put m "b" "2")
      (.invoke run m sb)
      (.toString sb)))

  (method ^:public ^:static tFiLambdaStays ^String []
    (let [m (HashMap.)
          ^Function g (lambda Function [k] (.concat ^String k "?"))
          run (fn [^HashMap hm ^Function f] (.computeIfAbsent hm "z" f))]
      (.invoke run m g)
      (cast String (.get m "z")))))

)
