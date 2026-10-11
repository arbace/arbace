;; Harvested by bin/oracle harvest (test/oracle/harvest.clj) from Clojure's test suite,
;; clojure/clojure 98d735fab02f337cee654cb0629bddc09883a75a test/clojure/test_clojure/reducers.clj,
;; renamed to arbace.* by bin/clojure-tests: the expressions of its assertions that are
;; self-contained and deterministic. Eclipse Public License 1.0 (LICENSE.md). Do not edit.
(require (quote [arbace.core.reducers :as r]))
(->> (concat (range 100) (lazy-seq (throw (Exception. "Too eager")))) (r/mapcat (juxt inc str)) (r/take 5) (into []))
(reduce-kv assoc {:k :v} nil)
(r/fold + nil)
(number? (reduce + 0 (r/map identity (range 1.0E8))))
