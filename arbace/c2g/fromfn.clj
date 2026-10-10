(ns arbace.c2g.fromfn
  "C2G-SPEC §7.11, §5.11 (amendment R13): ClassInfo.FromFn of every functional interface of the
  closed world (the @FunctionalInterface ones), which wraps a Clojure fn into the interface's
  adapter F_Fn. jrt.AdaptFn calls it where the JVM's Reflector makes a java.lang.reflect.Proxy
  (Reflector.boxArg), and jrt's isAnnotationPresent(FunctionalInterface) answers FromFn != nil.
  The adapter behaves as that proxy's handler: the arguments boxed, the fn applied, the result
  converted as Reflector.coerceAdapterReturn converts it. Written in arbace/lang
  (c2g_fromfn.go), as it calls IFn and RT; it sets the jrt classes' FromFn at Go package
  initialization."
  (:require [arbace.string :as str]
            [arbace.classes.types :as t]
            [arbace.classes.env :as env]
            [arbace.classes.analyze :as a]
            [arbace.c2g.model :as m]
            [arbace.c2g.names :as nm])
  (:import (arbace.asm Opcodes)))

(defn- tag [sym type] (with-meta sym {:tag type}))

(def ^:private casts
  "Reflector.coerceAdapterReturn: a primitive result by RT's casts (boolean by truth)."
  {"Z" "booleanCast" "J" "longCast" "D" "doubleCast" "I" "intCast" "S" "shortCast" "B" "byteCast"
   "F" "floatCast"})

(defn roots
  "What the FromFn adapters call: reachability roots."
  []
  (concat
    (for [[r nme] casts] ["arbace/lang/RT" nme (str "(Ljava/lang/Object;)" r)])
    [["arbace/lang/RT" "seq" "(Ljava/lang/Object;)Larbace/lang/ISeq;"]]))

(defn- functional? [n]
  (if-let [^Class c (env/load-class n)]
    (.isAnnotationPresent c java.lang.FunctionalInterface)
    (when-let [d (a/decl n)]
      (some #(= "Ljava/lang/FunctionalInterface;" (:type %)) (force (:annotations d))))))

(defn fis
  "The functional interfaces among the classes T that get FromFn: interfaces with
  @FunctionalInterface and one abstract method (none when the program has no Clojure
  runtime)."
  [T]
  (when (and (contains? T "arbace/lang/RT") (contains? T "arbace/lang/IFn"))
    (sort (filter #(and (env/interface? %) (not (m/reflected? %)) (functional? %)
                        (try (a/find-sam %) (catch Exception _ nil)))
                  T))))

(defn- box [p d]
  (cond (t/prim? d) (list 'jrt/Box p)
        (m/pointer-desc? d) (list 'fromFnRef p)
        :else p))

(defn- result [r d]
  (cond
    (casts d) (list (m/class-sym :lang "arbace/lang/RT" (str "_" (nm/method-base (casts d) (str "(Ljava/lang/Object;)" d)))) r)
    (= "C" d) (list 'fromFnChar r)
    (= "Ljava/lang/Object;" d) r
    ;; checked against the array class of the return type, as dyn's results are (it was the
    ;; result's own class, through the generic NN, which Go cannot instantiate on an any, and
    ;; with the call written twice; amendment JB3)
    (t/array? d) (list (list 'inst 'jrt/C2g_CastArray (m/go-type :lang d)) r
                       (loop [d d n 0] (if (t/array? d) (recur (t/elem-type d) (inc n))
                                         (nth (iterate #(list '.ArrayClass %) (if (t/prim? d)
                                                                                 (symbol (str "jrt/Prim_" (t/prim-desc->name d)))
                                                                                 (m/class-sym :lang (t/desc->internal d) "_class")))
                                              n))))
    :else (list (m/class-sym :lang (t/desc->internal d) "_Cast") r)))

(defn set-form
  "The statement setting FromFn of functional interface fi (in the init function)."
  [fi]
  (let [sam (a/find-sam fi)
        [ps r] (t/parse-method-desc (:desc sam))
        pn (vec (for [i (range (count ps))] (symbol (str "p" i))))
        call (list '.ApplyTo_ISeq__O 'f (list 'RT_Seq_O__ISeq (apply list 'jrt/RefArrayOf 'jrt/Object_class (map box pn ps))))
        fnf (list 'fn (cond-> (vec (map #(tag %1 (m/go-type :lang %2)) pn ps))
                        (not= "V" r) (vary-meta assoc :tag (m/go-type :lang r)))
                  (if (= "V" r) call (list 'return (result call r))))]
    (list 'set! (list '.-FromFn (list '.Info (m/class-sym :lang fi "_class")))
          (list 'fn (with-meta [(tag 'x 'any)] {:tag 'any})
                (list 'let ['f (list 'IFn_Cast 'x)]
                      (list 'addr (list 'lit (m/class-sym :lang fi "_Fn") :Fn fnf)))))))

(defn forms
  "The forms of c2g_fromfn.go (package arbace/lang) for the functional interfaces fis."
  [fis]
  (when (seq fis)
    [(list 'c2g/comment (str "---- FromFn of " (count fis) " functional interfaces (C2G-SPEC §7.11)"))
     '(go/func fromFnRef :type-params [T] ^any [^{:tag (* T)} p]
        (when (== p nil) (return nil))
        p)
     '(go/func fromFnChar ^uint16 [^any r]
        (let [(values v ok) (jrt/Unbox jrt/Prim_char r)]
          (when (not ok)
            (when (== r nil) (panic (jrt/Thrown (jrt/NPE))))
            (panic (jrt/ClassCast r jrt/Prim_char)))
          (assert uint16 v)))
     (apply list 'go/func 'init [] (map set-form fis))]))
