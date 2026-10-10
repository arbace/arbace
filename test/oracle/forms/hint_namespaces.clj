;; Type hints are resolved in the namespace the code is analyzed in, with that namespace's
;; imports, whatever *ns* is when the code later runs. The Go build's evaluator asked the
;; analyzer for some types (a case's tested expression, a primitive return's body) only when a
;; fn was first called, and resolved their short class names against the caller's namespace
;; ("Unable to resolve classname"). Each fn below is defined in oracle.hints, which imports the
;; classes, and first called from user, which does not.

(ns oracle.hints
  (:import (java.util ArrayList LinkedList)
           (java.util.concurrent.atomic AtomicLong AtomicInteger)))

;; a case on a hinted expression (its primitive type asked by the case)
(defn kase [x] (case ^ArrayList (identity x) 1 :one 2 :two :other))
(defn kase-int [^ArrayList l] (case (.size l) 0 :empty 1 :one :many))
;; a primitive return whose body is hinted with an imported Number class
(defn as-long ^long [x] ^AtomicLong (identity x))
(defn as-double ^double [x] ^AtomicInteger (identity x))
;; both inside a loop and a let
(defn count-big ^long [xs]
  (loop [s (seq xs) n 0]
    (if s
      (recur (next s) (+ n (case ^LinkedList (identity (first s)) 1 1 0)))
      n)))
(defn first-size ^long [^ArrayList l] (let [^ArrayList m l] (.size m)))
;; a case on keywords or strings (hash tests: the tested expression's type asked when it runs)
(defn kase-kw [x] (case ^ArrayList (identity x) :a 1 :b 2 3))
(defn kase-str [x] (case ^LinkedList (identity x) "a" 1 "b" 2 3))
;; an int-returning method (hashCode) whose body is hinted with an imported Number class
(deftype H [v] Object (hashCode [_] ^AtomicInteger (identity v)))
;; a deftype's method and a reify's
(deftype Box [v] Object (toString [_] (str (case ^ArrayList (identity v) 1 "one" "other"))))
(defn reified [] (reify Object (toString [_] (str (as-long (AtomicLong. 7))))))

(in-ns 'user)

(oracle.hints/kase 1)
(oracle.hints/kase 2)
(oracle.hints/kase (java.util.ArrayList.))
(oracle.hints/kase-int (java.util.ArrayList. [1 2]))
(oracle.hints/as-long (java.util.concurrent.atomic.AtomicLong. 42))
(oracle.hints/as-double (java.util.concurrent.atomic.AtomicInteger. 3))
(oracle.hints/count-big [1 2 1 3])
(oracle.hints/first-size (java.util.ArrayList. [1 2 3]))
(str (oracle.hints/->Box 1))
(str (oracle.hints/->Box 5))
(str (oracle.hints/reified))
(oracle.hints/kase-kw :b)
(oracle.hints/kase-kw "b")
(oracle.hints/kase-str "a")
(oracle.hints/kase-str :a)
(.hashCode (oracle.hints/->H (java.util.concurrent.atomic.AtomicInteger. 9)))
(hash (oracle.hints/->H (java.util.concurrent.atomic.AtomicInteger. 11)))
;; the same fns from a third namespace
(ns oracle.hints.other)
(oracle.hints/kase 2)
(oracle.hints/as-long (java.util.concurrent.atomic.AtomicLong. -1))
(oracle.hints/kase-kw :a)
(in-ns 'user)
