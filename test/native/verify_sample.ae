(ns native.verify-sample
  "Compiled ahead of time by native.verify-test, whose classes the JDK's verifier must accept:
  a sample of what the Clojure compiler emits, with class forms among it."
  (:gen-class :methods [[twice [long] long]]))

(defn -twice [_ ^long n] (* 2 n))

(defn prim ^long [^long a ^double b] (loop [i 0 acc 0] (if (< i a) (recur (inc i) (+ acc (long b))) acc)))

(defn guarded [f x]
  (try (f x)
       (catch ArithmeticException e :arith)
       (catch Exception e (.getMessage e))
       (finally (locking f (identity x)))))

(defn kinds [x]
  (case x 1 :one (2 3) :few "s" :string [1 2] :vec nil :nil (if (keyword? x) :kw :other)))

(defn closures [n]
  (letfn [(ev? [k] (if (zero? k) true (od? (dec k))))
          (od? [k] (if (zero? k) false (ev? (dec k))))]
    (mapv (fn [k] [(ev? k) (fn [] (+ n k))]) (range n))))

(def ^:dynamic *d* 1)
(defn bound [] (binding [*d* 2] (lazy-seq (cons *d* nil))))

(defprotocol Shape (area [s]) (scale [s k]))
(defrecord Rect [^double w ^double h] Shape (area [_] (* w h)) (scale [_ k] (->Rect (* k w) (* k h))))
(deftype Cell [^:unsynchronized-mutable v]
  Shape (area [_] 0) (scale [this k] (set! v (* k v)) this)
  Object (toString [_] (str "cell" v)))
(extend-protocol Shape String (area [s] (count s)) (scale [s k] (apply str (repeat k s))))

(definterface Named (^String nm []))
(gen-interface :name native.verify_sample.Sized :methods [[size [] int]])

(defmulti describe class)
(defmethod describe Long [n] (str "long " n))
(defmethod describe :default [x] (str "other " x))

(defn objects []
  [(reify Named (nm [_] "reified"))
   (proxy [java.util.AbstractList native.verify_sample.Sized] []
     (get [i] i) (size [] 3))
   (let [^Runnable r (fn [] nil)] r)])

(defclass ^:public Acc
  (field ^:private ^long total)
  (method ^:public add ^Acc [this ^long n] (set! (.-total this) (+ (.-total this) n)) this)
  (method ^:public sum ^long [this] (.-total this)))

(defn result []
  [(prim 3 1.5) (guarded #(/ 1 %) 0) (kinds 2) (count (closures 3)) (first (bound))
   (area (->Rect 2.0 3.0)) (str (scale (Cell. 2) 3)) (area "abc") (describe 1)
   (.nm ^Named (first (objects))) (count (second (objects))) (.sum (.add (Acc.) 5))])
