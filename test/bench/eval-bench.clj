;; The evaluator's benchmarks (B1a step 7b, doc/go/SPEED-NOTES.md, "The evaluator"): small
;; workloads run unchanged by the JVM's bin/arbace and the Go executable, to compare the Go
;; build's evaluator before and after a change, and against the JVM's compiled code.
;;
;; Usage: bin/arbace test/bench/eval-bench.clj [NAME...]
;;        target/arbace-go/amd64/arbace test/bench/eval-bench.clj [NAME...]
;; Each benchmark's thunk runs once (warm-up), then repeatedly until at least MIN-MS (default
;; 1000, the environment's EVAL_BENCH_MS) have passed and 3 runs are done; it prints one EDN map
;; per benchmark: {:name :fib :ms 12.3 :runs 81}, :ms the best run's time. With no NAME, all.

(defn fib [n] (if (< n 2) n (+ (fib (- n 1)) (fib (- n 2)))))

(defn fib-prim ^long [^long n] (if (< n 2) n (+ (fib-prim (- n 1)) (fib-prim (- n 2)))))

(defprotocol Shape (area [s]))
(defrecord Circle [r] Shape (area [_] (* 3.0 r r)))
(defrecord Square [a] Shape (area [_] (* a a)))
(deftype Rect [w h] Shape (area [_] (* w h)))

(defmulti marea :kind)
(defmethod marea :circle [m] (* 3.0 (:r m) (:r m)))
(defmethod marea :square [m] (* (:a m) (:a m)))
(defmethod marea :rect [m] (* (:w m) (:h m)))

(def sink (volatile! nil))

(def benchmarks
  [[:fib "(fib 22), boxed arithmetic, recursive calls"
    (fn [] (fib 22))]
   [:fib-prim "(fib-prim 22), ^long fn"
    (fn [] (fib-prim 22))]
   [:loop-long "loop/recur 1,000,000: inc, xor, multiply, add on longs"
    (fn [] (loop [i 0 acc 0] (if (< i 1000000) (recur (inc i) (+ acc (bit-xor i (* i 3)))) acc)))]
   [:loop-double "loop/recur 200,000: sqrt, multiply, add on doubles"
    (fn [] (loop [i 0 acc 0.0] (if (< i 200000) (recur (inc i) (+ acc (* 1.5 (Math/sqrt (double i))))) acc)))]
   [:dotimes-array "dotimes 200,000 aset/aget on a long-array"
    (let [^longs a (long-array 200000)]
      (fn [] (dotimes [i 200000] (aset a i (+ i (aget a i)))) (aget a 999)))]
   [:reduce-range "reduce + over (range 1,000,000)"
    (fn [] (reduce + (range 1000000)))]
   [:reduce-fn "reduce with an evaluated fn over (range 200,000)"
    (fn [] (reduce (fn [acc x] (+ acc (* x x))) 0 (range 200000)))]
   [:vector-conj "conj 100,000 longs onto []"
    (fn [] (loop [v [] i 0] (if (< i 100000) (recur (conj v i) (inc i)) (count v))))]
   [:map-assoc-get "assoc 20,000 keys into {}, then get each"
    (fn [] (let [m (loop [m {} i 0] (if (< i 20000) (recur (assoc m i i) (inc i)) m))]
             (loop [i 0 acc 0] (if (< i 20000) (recur (inc i) (+ acc (get m i))) acc))))]
   [:keyword-map "(:x m) on a 3-entry map, 200,000 times"
    (let [m {:x 1 :y 2 :z 3}]
      (fn [] (loop [i 0 acc 0] (if (< i 200000) (recur (inc i) (+ acc (:x m))) acc))))]
   [:str-build "str of 20,000 small pieces, and a StringBuilder"
    (fn [] (let [sb (StringBuilder.)]
             (dotimes [i 20000] (.append sb (str "item-" i ":" (* i 2))))
             (.length sb)))]
   [:seq-ops "map, filter, partition, interleave, distinct, sort over 20,000"
    (let [xs (vec (range 20000))]
      (fn [] [(count (filter odd? (map inc xs))) (count (partition 3 xs))
              (count (interleave xs xs)) (count (distinct (map #(mod % 97) xs)))
              (first (sort-by - xs))]))]
   [:lazy-walk "first/next over (map inc (filter odd? (range 100,000)))"
    (fn [] (loop [s (seq (map inc (filter odd? (range 100000)))) acc 0]
             (if s (recur (next s) (+ acc (first s))) acc)))]
   [:transduce "transduce (map inc) (filter even?) (map square) + over 100,000"
    (let [v (vec (range 100000)) xf (comp (map inc) (filter even?) (map #(* % %)))]
      (fn [] (transduce xf + v)))]
   [:into-xform "into [] with (map inc) (filter even?) over 100,000"
    (let [v (vec (range 100000)) xf (comp (map inc) (filter even?))]
      (fn [] (count (into [] xf v))))]
   [:protocol "a protocol fn on 2 records and a deftype in turn, 100,000 calls"
    (let [shapes (object-array [(->Circle 2) (->Square 3) (Rect. 2 5)])]
      (fn [] (loop [i 0 acc 0.0]
               (if (< i 100000) (recur (inc i) (+ acc (area (aget shapes (rem i 3))))) acc))))]
   [:multimethod "a multimethod on :kind over 3 methods, 50,000 calls"
    (let [ms (object-array [{:kind :circle :r 2} {:kind :square :a 3} {:kind :rect :w 2 :h 5}])]
      (fn [] (loop [i 0 acc 0.0]
               (if (< i 50000) (recur (inc i) (+ acc (marea (aget ms (rem i 3))))) acc))))]
   [:destructure "fn with map and vector destructuring, 50,000 calls"
    (let [f (fn [{:keys [a b] :or {b 2}} [x y & more]] (+ a b x y (count more)))]
      (fn [] (loop [i 0 acc 0] (if (< i 50000) (recur (inc i) (+ acc (f {:a i} [1 2 3 4]))) acc))))]
   [:closures "make and call closures (comp, partial, fn), 100,000 times"
    (fn [] (loop [i 0 acc 0]
             (if (< i 100000)
               (let [g (fn [x] (+ x i))
                     h (partial + 1)]
                 (recur (inc i) (+ acc (g (h 1)))))
               acc)))]])

(defn now-ms ^double [] (/ (double (System/nanoTime)) 1.0e6))

(defn run [[name _ thunk]]
  (let [limit (double (Long/parseLong (or (System/getenv "EVAL_BENCH_MS") "1000")))]
    (vreset! sink (thunk))
    (let [t0 (now-ms)]
      (loop [runs 0 best Double/MAX_VALUE]
        (let [t (now-ms)
              _ (vreset! sink (thunk))
              d (- (now-ms) t)
              best (min best d)
              runs (inc runs)]
          (if (and (>= runs 3) (> (- (now-ms) t0) limit))
            (println (pr-str {:name name :ms (/ (Math/round (* best 100.0)) 100.0) :runs runs}))
            (recur runs best)))))))

(let [names (set (map keyword *command-line-args*))]
  (doseq [b benchmarks :when (or (empty? names) (names (first b)))]
    (run b)
    (flush)))
