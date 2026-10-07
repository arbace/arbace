(ns native.overload-test
  "Overloads chosen in fns handed over to the class forms compiler as arbace.lang.Compiler
  chooses them (doc/classes/SPEC.md §9.5): the only method of that arity, else
  Compiler.getMatchingParams with Reflector.paramArgTypeMatch, with the compiler's errors and
  reflection where it chooses none; compared with the same calls compiled by the compiler."
  (:require [arbace.test :refer :all]))

(defclass ^:public Over
  (method ^:public ^:static f ^String [^int x] "int")
  (method ^:public ^:static f ^String [^long x] "long")
  (method ^:public ^:static f ^String [^double x] "double")
  (method ^:public ^:static f ^String [^Object x] "Object")
  (method ^:public ^:static f ^String [^String x] "String")
  (method ^:public ^:static f ^String [^CharSequence x] "CharSequence")
  (method ^:public ^:static g ^String [^Number x ^Object y] "Number,Object")
  (method ^:public ^:static g ^String [^Object x ^Number y] "Object,Number")
  (method ^:public ^:static g ^String [^Long x ^Long y] "Long,Long")
  (method ^:public ^:static h ^String [^float x] "float")
  (method ^:public ^:static h ^String [^Integer x] "Integer")
  (method ^:public ^:static one ^String [^int x] (java-str "one" x))
  (method ^:public ^:static fi ^Object [^java.util.function.Function f ^Object x] (.apply f x))
  (method ^:public ^:static fi ^Object [^String s ^Object x] "String")
  (constructor ^:public [this ^int x])
  (constructor ^:public [this ^String x])
  (method ^:public tag ^String [this ^long x] "long")
  (method ^:public tag ^String [this ^Object x] "Object"))

(defmacro ^:private both
  "[(fn compiled by arbace.lang.Compiler) (the same, handed over)] of a body using the args."
  [args body]
  `[(fn ~args ~body) (fn ~args (label :l# ~body))])

(defn- results [[plain native] & args]
  [(apply plain args) (apply native args)])

(deftest same-overloads-as-clojure
  (doseq [[fs args]
          [[(both [^long x] (Over/f x)) [1]]
           [(both [^double x] (Over/f x)) [1.5]]
           [(both [x] (Over/f (int x))) [1]]
           [(both [^String x] (Over/f x)) ["s"]]
           [(both [^StringBuilder x] (Over/f x)) [(StringBuilder.)]]
           [(both [] (Over/f nil)) []]
           [(both [] (Over/f 1)) []]
           [(both [] (Over/f "lit")) []]
           [(both [^Long x] (Over/g x x)) [1]]
           [(both [^Integer x] (Over/h x)) [(int 1)]]
           [(both [^double x] (Over/h x)) [1.5]]
           [(both [x] (Over/one x)) [7]]
           [(both [^long x] (Over/one x)) [7]]
           [(both [^double x] (Over/one x)) [7.0]]
           [(both [^Over o ^long x] (.tag o x)) [(Over. 1) 1]]
           [(both [^Over o ^String x] (.tag o x)) [(Over. 1) "s"]]
           [(both [^Over o x] (.tag o x)) [(Over. 1) :k]]
           [(both [x] (Over/fi (fn [y] [y]) x)) [1]]
           [(both [x] (Over/fi "s" x)) [1]]
           [(both [^long x] (.getName (class (Over. x)))) [1]]]]
    (let [[p n] (apply results fs args)]
      (is (= p n) (pr-str args)))))

(defn- root-message [e]
  (ex-message (last (take-while some? (iterate ex-cause e)))))

(defn- compile-error [form]
  (try (binding [*ns* (the-ns 'native.overload-test)] (eval form)) nil
       (catch Throwable e (root-message e))))

(deftest same-errors-as-clojure
  (doseq [body '[(Over/g 1 2)
                 (Over/g (Long/valueOf 1) (Integer/valueOf 2))
                 (Over/f 1 2)
                 (Over/h (Long/valueOf 1))
                 (Over. 1 2)]]
    (is (= (compile-error (list 'fn [] body))
           (compile-error (list 'fn [] (list 'label :l body))))
        (pr-str body)))
  (is (= "More than one matching method found: g"
         (compile-error '(fn [] (label :l (Over/g (Long/valueOf 1) (Integer/valueOf 2)))))))
  (is (= "No matching method f found taking 2 args for class native.overload_test.Over"
         (compile-error '(fn [] (label :l (Over/f 1 2))))))
  (is (= "No matching ctor found for class native.overload_test.Over"
         (compile-error '(fn [] (label :l (Over. 1 2)))))))

(deftest reflection-where-clojure-reflects
  ;; untyped: no method chosen, the call goes through Reflector at run time
  (let [[p n] (both [x] (Over/f x))]
    (is (= "Object" (p 1) (n 1)))
    (is (= (p "s") (n "s"))))
  (let [[p n] (both [] (Over/g 1 2))]
    (is (= "Long,Long" (p) (n)))))
