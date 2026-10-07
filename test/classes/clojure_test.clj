(ns classes.clojure-test
  "Clojure code in class bodies (SPEC §5.13), beyond the Java subset, also at stage 0: fn,
  letfn, case, def and var, calls of locals and other values, Clojure's operations on any
  value, constants of any kind."
  (:require [clojure.test :refer :all]
            [classes.helpers :refer :all]))

(defn- call [c m & args]
  (clojure.lang.Reflector/invokeStaticMethod ^Class c ^String m (object-array args)))

(def ^:dynamic *dyn* 1)

(deftest clojure-in-class-bodies
  (let [[C] (load-forms 'classes.clojure-test
                        '[(^:public Clj
                           (method ^:public ^:static twice [xs] (vec (map (fn [x] (* 2 x)) xs)))
                           (method ^:public ^:static adder [n] (fn [x] (+ x n)))
                           (method ^:public ^:static named [n] ((fn f [i] (if (zero? i) :done (f (dec i)))) n))
                           (method ^:public ^:static variadic [] ((fn [a & more] [a more]) 1 2 3))
                           (method ^:public ^:static pick [x] (case x :a 1 (:b :c) 2 "s" 3 0))
                           (method ^:public ^:static pick-int [x] (case (long x) 1 :one 2 :two :other))
                           (method ^:public ^:static evens [n]
                             (letfn [(e? [n] (if (zero? n) true (o? (dec n))))
                                     (o? [n] (if (zero? n) false (e? (dec n))))]
                               (e? n)))
                           (method ^:public ^:static objs [a b] [(= a b) (+ a b) (not a) (str a b)])
                           (method ^:public ^:static callers [m f] [(:k m) (m :k) (f 3)])
                           (method ^:public ^:static consts [] [#"a+b" 1/3 2.5M 'sym '(1 2)])
                           (method ^:public ^:static the-var [] (var *dyn*))
                           (method ^:public ^:static prim ^long [^long x]
                             (long ((fn ^long [^long y] (unchecked-multiply y y)) x)))
                           ;; reify's methods implement the interface method Clojure's
                           ;; compiler chooses by the hints
                           (method ^:public ^:static overloads []
                             (let [sb (StringBuilder.)
                                   ^Appendable a (reify Appendable
                                                   (^Appendable append [this ^char c] (.append sb (str "c" c)) this)
                                                   (^Appendable append [this ^CharSequence s] (.append sb (str "s" s)) this)
                                                   (append [this s start end] (.append sb "range") this))]
                               (.append a \x)
                               (.append a "yz")
                               (.append a "abc" 0 1)
                               (str sb))))])]
    (is (= [2 4 6] (call C "twice" [1 2 3])))
    (is (= 15 ((call C "adder" 5) 10)))
    (is (= :done (call C "named" 3)))
    (is (= [1 '(2 3)] (call C "variadic")))
    (is (= [1 2 2 3 0] (map #(call C "pick" %) [:a :b :c "s" 'x])))
    (is (= [:one :two :other] (map #(call C "pick-int" %) [1 2 3])))
    (is (= [true false] [(call C "evens" 10) (call C "evens" 7)]))
    (is (= [false 3 false "12"] (call C "objs" 1 2)))
    (is (= [1 1 4] (call C "callers" {:k 1} inc)))
    (let [[re r d s l] (call C "consts")]
      (is (= "aab" (re-matches re "aab")))
      (is (= [1/3 2.5M 'sym '(1 2)] [r d s l])))
    (is (= #'*dyn* (call C "the-var")))
    (is (= 49 (call C "prim" 7)))
    (is (= "cxsyzrange" (call C "overloads")))))
