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

(deftest deftype-in-class-bodies
  ;; deftype* sees only its fields, so Clojure's compiler defines it while the body is analyzed;
  ;; import* imports at run time. The deftype macro names the class after *ns*.
  (let [[C] (binding [*ns* (the-ns 'classes.clojure-test)]
              (load-forms 'classes.clojure-test
                        '[(^:public WithTypes
                           (method ^:public ^:static make [n]
                             (deftype InBody [^long v w] Object (toString [this] (str "v" v w)))
                             (defrecord RecInBody [a])
                             [(str (classes.clojure_test.InBody. (long n) :w))
                              (classes.clojure_test.RecInBody/getBasis)
                              (import java.util.UUID)]))]))]
    (is (= ["v3:w" '[a] java.util.UUID] (call C "make" 3)))
    (is (= {:a 1} (into {} ((resolve 'classes.clojure-test/->RecInBody) 1))))))

(definterface CovBase (^Object item []) (^CharSequence label [^long i]))
(definterface CovNarrow (^String item []) (^String label [^long i]))

(defn- bridges-of [^Class c]
  (sort (for [^java.lang.reflect.Method m (.getDeclaredMethods c) :when (.isBridge m)]
          [(.getName m) (.getName (.getReturnType m)) (.getModifiers m) (.isSynthetic m)])))

(deftest reify-covariant-bridges
  ;; Clojure's compiler's bridges: one per overridden return type, implemented or not,
  ;; ACC_PUBLIC | ACC_BRIDGE only
  (let [[C] (load-forms 'classes.clojure-test
                        '[(^:public Cov
                           (method ^:public ^:static make [s]
                             (reify CovBase CovNarrow (item [this] (str s)))))])
        r (call C "make" 5)]
    (is (= "5" (.item ^CovBase r)))
    (is (= [["item" "java.lang.Object" 65 false] ["label" "java.lang.CharSequence" 65 false]]
           (bridges-of (class r))
           (bridges-of (class (reify CovBase CovNarrow (item [this] "x"))))))))

(deftest clojure-overloads-in-class-bodies
  ;; a fn in a class body chooses overloads as Clojure's compiler does
  ;; (Compiler.getMatchingParams), with its errors
  (let [[C] (load-forms 'classes.clojure-test
                        '[(^:public Overs
                           (method ^:public ^:static abs [x]
                             ((fn [^long y] [(Math/abs y) (Math/abs (int y)) (Math/abs (double y))]) x))
                           (method ^:public ^:static one [x] ((fn [y] (Integer/toBinaryString y)) x)))])]
    (is (= [3 3 3.0] (call C "abs" -3)))
    (is (= "101" (call C "one" 5))))
  (is (re-find #"More than one matching method found: abs"
               (try (load-forms 'classes.clojure-test
                                '[(^:public OversTied
                                   (method ^:public ^:static f [^Long x] ((fn [^Long y] (Math/abs y)) x)))])
                    ""
                    (catch Exception e (str (ex-message e) " " (some-> e ex-cause ex-message)))))))
