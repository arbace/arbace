;; Harvested by bin/oracle harvest (test/oracle/harvest.clj) from Clojure's test suite,
;; clojure/clojure 98d735fab02f337cee654cb0629bddc09883a75a test/clojure/test_clojure/def.clj,
;; renamed to arbace.* by bin/clojure-tests: the expressions of its assertions that are
;; self-contained and deterministic. Eclipse Public License 1.0 (LICENSE.md). Do not edit.
(eval (quote (do (defmacro my-macro [] :a) (defn do-macro [] (my-macro)) (defmacro my-macro [] :b) (defn do-macro [] (my-macro)) (do-macro))))
(eval (quote (do (list (declare ^{:dynamic true} p) (defn q [] (arbace.core/deref p))) (binding [p (atom 10)] (q)))))
