(ns native.deftype-test
  "The class forms in the methods of deftype, defrecord and reify (doc/classes/SPEC.md §9.5):
  arbace.lang.Compiler hands a deftype one of whose methods uses them over to the class forms
  compiler, which gives the class the shape the compiler gives deftypes; methods of handed-over
  deftypes and reifys implement the interface method arbace.lang.Compiler would choose."
  (:require [arbace.test :refer :all]))

(definterface Counter
  (^long add [^long x])
  (^String add [^String x])
  (total [xs])
  (^String describe [x]))

(defprotocol Shape (area [s]) (scaled [s k]))

;; the same deftype twice: its methods using the class forms (handed over) and not (compiled by
;; arbace.lang.Compiler), to compare their classes
(deftype Native [^long base ^:unsynchronized-mutable ^long n ^:volatile-mutable flag ^String title
                 plain-field]
  Counter
  (^long add [this ^long x] (set! n (+ n x)) (label :done (when (neg? x) (break :done -1)) (+ base n)))
  (^String add [this ^String x] (str title x))
  (total [this xs]
    (let [^:mutable t 0]
      (for-each [x ^Iterable xs] (set! t (+ t (long x))))
      t))
  (describe [this x] (switch (int x) 0 "zero" 1 (.toUpperCase title) "many"))
  Shape
  (area [this] (label :a (break :a (* base base))))
  (scaled [this k] (Native. (* base (long k)) 0 nil title plain-field))
  Object
  (toString [this] (label :s (when (zero? n) (break :s (str title ":none"))) (str title ":" n))))

(deftype Plain [^long base ^:unsynchronized-mutable ^long n ^:volatile-mutable flag ^String title
                plain-field]
  Counter
  (^long add [this ^long x] (set! n (+ n x)) (+ base n))
  (^String add [this ^String x] (str title x))
  (total [this xs] (reduce + 0 xs))
  (describe [this x] (.toUpperCase title))
  Shape
  (area [this] (* base base))
  (scaled [this k] (Plain. (* base (long k)) 0 nil title plain-field))
  Object
  (toString [this] (str title ":" n)))

(defn- member-shape
  "The declared members of class c that both compilers make alike: instance fields, constructors,
  methods other than the class's constants and their initialization."
  [^Class c]
  (let [cname (.getName c)
        t (fn [^Class x] (if (= x c) :self (.getName x)))
        own (fn [s] (.replace (str s) (str cname ".") ""))]
    {:flags (java.lang.reflect.Modifier/toString (.getModifiers c))
     :interfaces (mapv t (.getInterfaces c))
     :fields (sort (for [^java.lang.reflect.Field f (.getDeclaredFields c)
                         :when (not (java.lang.reflect.Modifier/isStatic (.getModifiers f)))]
                     (own f)))
     :ctors (sort (map #(.replace (str %) cname "C") (.getDeclaredConstructors c)))
     :methods (sort (for [^java.lang.reflect.Method m (.getDeclaredMethods c)
                          :when (not (.startsWith (.getName m) "__"))]
                      [(.getName m) (mapv t (.getParameterTypes m)) (t (.getReturnType m))
                       (java.lang.reflect.Modifier/toString (.getModifiers m))
                       (mapv t (.getExceptionTypes m))]))}))

(deftest deftype-with-class-forms
  (let [a (->Native 10 0 nil "lbl" :p)]
    (is (= "lbl:none" (str a)))
    (is (= 15 (.add a 5)))
    (is (= -1 (.add a -1)))
    (is (= "lbl:4" (str a)))
    (is (= "lblx" (.add a "x")))
    (is (= 6 (.total a [1 2 3])))
    (is (= ["zero" "LBL" "many"] (mapv #(.describe a %) [0 1 2])))
    (is (= 100 (area a)))
    (is (= 400 (area (scaled a 2))))
    (is (instance? arbace.lang.IType a))
    (is (= :p (.-plain-field a))))
  (is (= '[base n flag title plain-field] (Native/getBasis)))
  (is (= (map meta (Plain/getBasis)) (map meta (Native/getBasis))))
  (is (= (update (member-shape Plain) :ctors (partial map #(.replace % "Plain" "C")))
         (update (member-shape Native) :ctors (partial map #(.replace % "Native" "C"))))))

(defrecord NativeRec [a ^long b]
  Comparable
  (compareTo [this o] (label :c (switch (int b) 1 (break :c -1) 2 (break :c 0)) 1)))

(defrecord PlainRec [a ^long b]
  Comparable
  (compareTo [this o] (if (= 1 b) -1 (if (= 2 b) 0 1))))

(deftest defrecord-with-class-forms
  (let [r (->NativeRec 1 2)]
    (is (= 0 (.compareTo r nil)))
    (is (= -1 (.compareTo (assoc r :b 1) nil)))
    (is (= {:a 1 :b 2} (into {} r)))
    (is (= r (map->NativeRec {:a 1 :b 2})))
    (is (= (assoc r :c 3) (NativeRec/create {:a 1 :b 2 :c 3})))
    (is (= [1 2 nil] [(:a r) (:b r) (:c r)]))
    (is (= 2 (get r :b)))
    (is (= {:m 1} (meta (with-meta r {:m 1}))))
    (is (= (hash (into {} r)) (hash (into {} (->PlainRec 1 2)))))
    (is (= "#native.deftype_test.NativeRec{:a 1, :b 2}" (pr-str r)))
    (is (= {:a 1} (into {} (dissoc r :b))))
    (is (= 3 (count (assoc r :z 0)))))
  (is (= '[a b] (NativeRec/getBasis)))
  (is (= (map meta (PlainRec/getBasis)) (map meta (NativeRec/getBasis))))
  (is (= (update (member-shape PlainRec) :ctors (partial map #(.replace % "PlainRec" "C")))
         (update (member-shape NativeRec) :ctors (partial map #(.replace % "NativeRec" "C"))))))

(definterface Overloaded
  (^long f [^long x])
  (^Object f [^Object x])
  (^String g [^String x])
  (h [x y]))

(defn- reify-with-class-forms []
  (reify Overloaded
    (^long f [this ^long x] (label :l (break :l (inc x))))
    (f [this ^Object x] (label :l (break :l [x])))
    (g [this x] (switch (.length x) 0 "empty" (str "len" (.length x))))
    (h [this x y] (label :l (break :l (+ x y))))))

(deftest reify-overloads-chosen-by-hints
  (let [^Overloaded r (reify-with-class-forms)]
    (is (= 2 (.f r (long 1))))
    (is (= [:k] (.f r :k)))
    (is (= ["empty" "len3"] [(.g r "") (.g r "abc")]))
    (is (= 3 (.h r 1 2)))
    (is (= [Long/TYPE String]
           (for [m (.getDeclaredMethods (class r)) :when (#{"f" "g"} (.getName m))
                 :when (= 1 (count (.getParameterTypes m)))
                 :when (not (.isBridge m))
                 :when (#{Long/TYPE String} (first (.getParameterTypes m)))]
             (first (.getParameterTypes m)))))))

(defn- root-message [e]
  (ex-message (last (take-while some? (iterate ex-cause e)))))

(defn- compile-error [form]
  (try (binding [*ns* (the-ns 'native.deftype-test)] (eval form)) nil
       (catch Throwable e (root-message e))))

(deftest clojure-errors-for-overloads
  (is (= "Must hint overloaded method: f"
         (compile-error '(fn [] (reify Overloaded (f [this x] (label :l x)))))))
  (is (= "Can't find matching overloaded method: f"
         (compile-error '(fn [] (reify Overloaded (f [this ^String x] (label :l x)))))))
  (is (= "Mismatched return type: f, expected: long, had: java.lang.Object"
         (compile-error '(fn [] (reify Overloaded (f [this ^long x] (label :l x)))))))
  (is (= "Can't find matching method: g, leave off hints for auto match."
         (compile-error '(fn [] (reify Overloaded (g [this ^Object x] (label :l x)))))))
  (is (= "Can't define method not in interfaces: nope"
         (compile-error '(deftype Bad [] Overloaded (nope [this] (label :l 1))))))
  ;; the same errors from arbace.lang.Compiler, without class forms
  (is (= "Must hint overloaded method: f" (compile-error '(fn [] (reify Overloaded (f [this x] x))))))
  (is (= "Mismatched return type: f, expected: long, had: java.lang.Object"
         (compile-error '(fn [] (reify Overloaded (f [this ^long x] x)))))))

(deftype Ext [x])

(extend-protocol Shape
  Ext
  (area [e] (label :l (break :l (* (.-x e) 2))))
  (scaled [e k] (Ext. (* (.-x e) k))))

(deftype Mutual [^:unsynchronized-mutable cnt]
  Shape
  (area [this] (set! cnt (inc cnt)) (label :l (for-each [s ^Iterable [(->Ext 1) (->Ext 2)]] (set! cnt (+ cnt (area s)))) cnt))
  (scaled [this k] (if (pos? k) (recur (dec k)) cnt)))

(deftest protocols-with-class-forms
  (is (= 6 (area (->Ext 3))))
  (is (= 12 (area (scaled (->Ext 3) 2))))
  (is (= 7 (area (->Mutual 0))))
  (is (= 5 (scaled (->Mutual 5) 3)))
  (is (satisfies? Shape (->Native 1 0 nil "" nil)))
  (is (= [100 4] (map area [(->Native 10 0 nil "" nil) (->Ext 2)]))))
