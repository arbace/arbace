(ns native.reduce-test
  "reduce without an init on an IReduceInit that is not an IReduce (doc/VENDOR-NOTES.md, hand
  change on arbace/core/protocols.clj, 2026-10-10). CollReduce's implementation for IReduceInit
  cast to IReduce, so (reduce + (eduction (map inc) (range 10))) threw a ClassCastException
  whenever find-protocol-impl picked it over Iterable's (the order of (supers Eduction), by
  identity hashes: e.g. without the AOT cache), and reduce on iteration or a reify of
  IReduceInit threw every time."
  (:require [arbace.test :refer :all]))

(defn- reduce-init-only
  "A reify of IReduceInit alone over the items of xs."
  [xs]
  (reify arbace.lang.IReduceInit
    (reduce [_ f init]
      (loop [acc init xs (seq xs)]
        (if xs
          (let [acc (f acc (first xs))]
            (if (reduced? acc) @acc (recur acc (next xs))))
          acc)))))

(def ^:private init-only-impl
  ;; CollReduce's implementation for IReduceInit itself, whichever one dispatch picks
  (get-in arbace.core.protocols/CollReduce [:impls arbace.lang.IReduceInit :coll-reduce]))

(deftest eduction-reduce
  (is (= 55 (reduce + (eduction (map inc) (range 10)))))
  (is (= 55 (init-only-impl (eduction (map inc) (range 10)) +)))
  (is (= 0 (reduce + (eduction (map inc) []))))
  (is (= 5 (reduce + (eduction (map inc) [4]))))
  (is (= [0 1 2 3 4 5 6] (reduce into (eduction (partition-all 3) (range 7)))))
  (is (= 20 (reduce + (eduction (filter even?) (range 10))))))

(deftest init-only-reduce
  (is (= 45 (reduce + (iteration (fn [k] k) :somef #(< % 10) :kf inc :initk 0))))
  (is (= 6 (reduce + (reduce-init-only [1 2 3]))))
  (is (= [] (reduce conj (reduce-init-only []))))
  (is (= :x (reduce (fn [_ _] (throw (Exception. "f called"))) (reduce-init-only [:x]))))
  (is (= [[1 2] 3] (reduce vector (reduce-init-only [1 2 3]))))
  (is (= :stop (reduce (fn [a x] (if (> a 5) (reduced :stop) (+ a x)))
                       (reduce-init-only (range 10)))))
  (is (= 10 (reduce + 4 (reduce-init-only [1 2 3])))))
