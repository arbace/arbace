;; Harvested by bin/oracle harvest (test/oracle/harvest.clj) from Clojure's test suite,
;; clojure/clojure 98d735fab02f337cee654cb0629bddc09883a75a test/clojure/test_clojure/for.clj,
;; renamed to arbace.* by bin/clojure-tests: the expressions of its assertions that are
;; self-contained and deterministic. Eclipse Public License 1.0 (LICENSE.md). Do not edit.
(take 100 (for [x (range 100000000) y (range 1000000) :while (< y x)] [x y]))
(for [x (range 10) :when (odd? x)] x)
(for [x (range 4) y (range 4) :when (odd? y)] [x y])
(for [x (range 4) y (range 4) :when (odd? x)] [x y])
(for [x (range 4) :when (odd? x) y (range 4)] [x y])
(for [x (range 5) y (range 5) :when (< x y)] [x y])
(for [x (range 4) y (range 4) :while (< x 3)] [x y])
(for [x (range 4) y (range 4) :while (even? x)] [x y])
(for [a (range -2 5) :when (not= a 0) :while (> (Math/abs (/ 1.0 a)) 1/3)] a)
(for [x (quote (a b)) y (interpose x (quote (1 2))) z (list x y)] [x y z])
(for [x [(quote a) nil] y [x (quote b)]] [x y])
(for [{:syms [a b c]} (map (fn* [p1__964#] (zipmap (quote (a b c)) (range p1__964# 5))) (range 3)) x [a b c]] (Integer. (str a b c x)))
(for [x (range 3) y (range 3) :let [z (+ x y)] :when (odd? z)] [x y z])
(for [x (range 6) :let [y (rem x 2)] :when (even? y) z [8 9]] [x z])
(for [x (range 100) :while (even? x)] x)
