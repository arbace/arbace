;; Harvested by bin/oracle harvest (test/oracle/harvest.clj) from Clojure's test suite,
;; clojure/clojure 98d735fab02f337cee654cb0629bddc09883a75a test/clojure/test_clojure/predicates.clj,
;; renamed to arbace.* by bin/clojure-tests: the expressions of its assertions that are
;; self-contained and deterministic. Eclipse Public License 1.0 (LICENSE.md). Do not edit.
(not (string? (new java.lang.StringBuilder "abc")))
(not (string? (new java.lang.StringBuffer "xyz")))
(NaN? ##NaN)
(NaN? (Double/parseDouble "NaN"))
(NaN? (Float/parseFloat "NaN"))
(NaN? Float/NaN)
(not (NaN? 5))
(NaN? nil)
(NaN? :xyz)
(infinite? ##Inf)
(infinite? ##-Inf)
(infinite? Double/POSITIVE_INFINITY)
(infinite? Double/NEGATIVE_INFINITY)
(infinite? Float/POSITIVE_INFINITY)
(infinite? Float/NEGATIVE_INFINITY)
(infinite? nil)
(infinite? :xyz)
