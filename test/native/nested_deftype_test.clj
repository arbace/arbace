(ns native.nested-deftype-test
  "deftype and defrecord inside code the class forms compiler compiles (handed-over fns, class
  bodies; doc/classes/SPEC.md §9.5): a deftype* sees only its fields, never the enclosing locals,
  so arbace.lang.Compiler compiles and defines it while the enclosing code is analyzed, as it does
  for deftype* anywhere; import* imports at run time, as Clojure's ImportExpr."
  (:require [arbace.test :refer :all]))

(defn- in-fn [n]
  (deftype InFn [^long a b] Object (toString [this] (str "InFn" a b)))
  (switch (int n) 0 (str (->InFn 1 2)) (str (native.nested_deftype_test.InFn. n :x))))

(defn- class-forms-in-methods [n]
  (deftype WithForms [^long a]
    Object
    (toString [this] (label :l (when (neg? a) (break :l "neg")) (java-str "a=" a))))
  (label :done (break :done [(str (->WithForms n)) (str (->WithForms -1))])))

(defn- record-in-fn []
  (label :l (defrecord RecInFn [x ^long y]
              Comparable (compareTo [this o] (switch (int y) 1 -1 0))))
  (->RecInFn :a 1))

(defn- import-in-fn []
  (label :l (break :l (import 'java.util.concurrent.atomic.LongAdder))))

(defclass ^:public Host
  (method ^:public ^:static make [n]
    (deftype InHost [q] Object (toString [_] (str "q=" q)))
    (str (native.nested_deftype_test.InHost. n))))

(deftype TopLevel [^long a b] Object (toString [this] (str "InFn" a b)))

(defn- members [^Class c]
  (sort (concat (for [^java.lang.reflect.Field f (.getDeclaredFields c)
                      :when (not (java.lang.reflect.Modifier/isStatic (.getModifiers f)))]
                  [(.getName f) (.getName (.getType f)) (.getModifiers f)])
                (for [^java.lang.reflect.Method m (.getDeclaredMethods c)]
                  [(.getName m) (mapv #(.getName ^Class %) (.getParameterTypes m)) (.getModifiers m)]))))

(deftest deftype-in-handed-over-fns
  (is (= ["InFn12" "InFn5:x"] [(in-fn 0) (in-fn 5)]))
  (is (= '[a b] (native.nested_deftype_test.InFn/getBasis)))
  (is (= (members TopLevel) (members (Class/forName "native.nested_deftype_test.InFn"))))
  (is (= ["a=3" "neg"] (class-forms-in-methods 3)))
  (let [r (record-in-fn)]
    (is (= {:x :a :y 1} (into {} r)))
    (is (= -1 (.compareTo ^Comparable r nil)))
    (is (= "native.nested_deftype_test.RecInFn" (.getName (class r)))))
  ;; into the namespace current at run time, as Clojure's ImportExpr
  (let [ns (create-ns 'native.nested-deftype-test.imports)]
    (is (= java.util.concurrent.atomic.LongAdder (binding [*ns* ns] (import-in-fn))))
    (is (= java.util.concurrent.atomic.LongAdder (ns-resolve ns 'LongAdder)))))

(deftest deftype-in-class-bodies
  (is (= "q=3" (Host/make 3)))
  (is (= "q=4" (str ((resolve 'native.nested-deftype-test/->InHost) 4)))))

(deftest monitor-forms-still-errors
  (is (re-find #"monitor-enter is not supported"
               (try (eval '(fn [x] (label :l (monitor-enter x)))) nil
                    (catch Throwable e (ex-message (last (take-while some? (iterate ex-cause e)))))))))
