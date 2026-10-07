(ns native.bridge-test
  "Covariant bridges of reify and deftype classes compiled by the class forms compiler
  (doc/classes/SPEC.md §9.5): the ones arbace.lang.Compiler makes (NewInstanceExpr's covariants),
  a public ACC_BRIDGE method for every overridden return type, implemented or not, compared
  with the same types compiled by arbace.lang.Compiler."
  (:require [arbace.test :refer :all]))

(definterface Base (^Object item []) (^CharSequence label [^long i]) (^Object other []))
(definterface Narrow (^String item []) (^String label [^long i]))
(definterface Narrower (^String other []))

;; the same deftype twice: handed over (class forms in a method) and compiled by
;; arbace.lang.Compiler
(deftype NativeT [s]
  Base
  (other [this] (label :l (break :l "o")))
  Narrow
  (item [this] (label :l (when (nil? s) (break :l "none")) (str s)))
  Narrower)

(deftype PlainT [s]
  Base
  (other [this] "o")
  Narrow
  (item [this] (if (nil? s) "none" (str s)))
  Narrower)

(defn- native-reify [s] (reify Base Narrow (item [this] (label :l (break :l (str s))))))
(defn- plain-reify [s] (reify Base Narrow (item [this] (str s))))

(defn- methods-of
  "Name, parameter types, return type, access flags (with bridge and synthetic) and exceptions
  of the declared methods of class c."
  [^Class c]
  (sort (for [^java.lang.reflect.Method m (.getDeclaredMethods c)
              :when (not (.startsWith (.getName m) "__"))]
          [(.getName m) (mapv #(.getName ^Class %) (.getParameterTypes m)) (.getName (.getReturnType m))
           (.getModifiers m) (.isBridge m) (.isSynthetic m) (mapv #(.getName ^Class %) (.getExceptionTypes m))])))

(deftest deftype-bridges
  (is (= (methods-of PlainT) (methods-of NativeT)))
  (is (= #{["item" "java.lang.Object"] ["label" "java.lang.CharSequence"] ["other" "java.lang.Object"]}
         (set (for [[n _ r _ bridge? synthetic?] (methods-of NativeT) :when bridge?]
                (do (is (not synthetic?)) [n r])))))
  (let [x (->NativeT 5)]
    (is (= ["5" "5" "o" "o"] [(.item ^Base x) (.item ^Narrow x) (.other ^Base x) (.other ^Narrower x)]))
    (is (= "none" (.item ^Base (->NativeT nil))))
    ;; not implemented: the bridge calls the interface method
    (is (thrown? AbstractMethodError (.label ^Base x 1)))))

(deftest reify-bridges
  (is (= (methods-of (class (plain-reify 1))) (methods-of (class (native-reify 1)))))
  (is (= "7" (.item ^Base (native-reify 7)))))
