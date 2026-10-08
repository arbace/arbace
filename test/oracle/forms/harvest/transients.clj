;; Harvested by bin/oracle harvest (test/oracle/harvest.clj) from Clojure's test suite,
;; clojure/clojure 98d735fab02f337cee654cb0629bddc09883a75a test/clojure/test_clojure/transients.clj,
;; renamed to arbace.* by bin/clojure-tests: the expressions of its assertions that are
;; self-contained and deterministic. Eclipse Public License 1.0 (LICENSE.md). Do not edit.
(let [v (-> (range 33) vec)] (= (subvec v 0 (- 33 2)) (-> v transient pop! pop! persistent!)))
(let [v (-> (range (+ 32 (inc (* 32 32)))) vec)] (= (subvec v 0 (- (+ 32 (inc (* 32 32))) 2)) (-> v transient pop! pop! persistent!)))
(let [v (-> (range (+ 32 (inc (* 32 32 32)))) vec)] (= (subvec v 0 (- (+ 32 (inc (* 32 32 32))) 2)) (-> v transient pop! pop! persistent!)))
(-> [] transient pop!)
(let [pv (vec (range 34))] (-> pv transient pop! pop! pop! (conj! 42)) (nth pv 31))
(-> #{20 15 5 10} transient (disj! 10 15) persistent!)
(.contains (transient #{}) :bogus-key)
