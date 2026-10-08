;; Harvested by bin/oracle harvest (test/oracle/harvest.clj) from Clojure's test suite,
;; clojure/clojure 98d735fab02f337cee654cb0629bddc09883a75a test/clojure/test_clojure/clojure_walk.clj,
;; renamed to arbace.* by bin/clojure-tests: the expressions of its assertions that are
;; self-contained and deterministic. Eclipse Public License 1.0 (LICENSE.md). Do not edit.
(require (quote [arbace.walk :as w]))
(w/prewalk-replace {:a :b} [:a {:a :a} (list 3 :c :a)])
(w/postwalk-replace {:a :b} [:a {:a :a} (list 3 :c :a)])
(w/stringify-keys {:a 1, nil {:b 2, :c 3}, :d 4})
(w/transform-keys {:a 1, nil {:b 2, :c 3}, :d 4, :e 5} (fn [k] (if (keyword? k) (str (name k) "-" (name k)) k)))
(let [a (atom [])] (w/prewalk (fn [form] (swap! a conj form) form) [1 2 {:a 3} (list 4 [5])]) (arbace.core/deref a))
(let [a (atom [])] (w/postwalk (fn [form] (swap! a conj form) form) [1 2 {:a 3} (list 4 [5])]) (arbace.core/deref a))
(list :html {:a (list "b" 1)} "")
