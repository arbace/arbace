;; The workloads of Arbace's benchmark suite (bin/arbace-bench, doc/BENCHMARKS.md). One file,
;; run unchanged as a script by Arbace (arbace.lang.Main), upstream Clojure (clojure.main) and
;; the frozen clojure/: it uses only the core names referred into `user`, and reaches other
;; namespaces through `lib` (clojure.string on Clojure, arbace.string on Arbace).
;;
;; Usage (the driver, test/bench/driver.clj, runs it in a fresh JVM per benchmark and fork):
;;   ... workload.clj list                          the steady-state benchmarks, one EDN map each
;;   ... workload.clj steady NAME WARMUP-MS MEASURE-MS
;;   ... workload.clj load LIB                      the time of (require LIB) in this fresh JVM
;; Each run prints one EDN map on a line starting with "#bench ".
;;
;; Steady state: the benchmark's thunk runs for WARMUP-MS, while the number of calls per sample
;; is doubled until a sample takes at least 20 ms; then samples are taken for MEASURE-MS (at
;; least 10). A sample's value is its time divided by its calls: the time of one call. Every
;; result goes into a volatile, so the JIT cannot drop the work.

(def arbace? (some? (find-ns 'arbace.core)))

(defn lib
  "The namespace symbol of Clojure's library NAME (\"string\") in this implementation."
  [name]
  (symbol (str (if arbace? "arbace." "clojure.") name)))

(defn lib-fn [ns-name f]
  (let [ns (lib ns-name)]
    (require ns)
    @(ns-resolve (the-ns ns) f)))

(def sink (volatile! nil))

;; ---------------------------------------------------------------------------------------------
;; The fixtures

(defrecord Point [x y z])
(defprotocol Shape (area [s]))
(defrecord Circle [r] Shape (area [_] (* 3.0 r r)))
(defrecord Square [a] Shape (area [_] (* a a)))
(deftype Rect [w h] Shape (area [_] (* w h)))
(defmulti marea :kind)
(defmethod marea :circle [m] (* 3.0 (:r m) (:r m)))
(defmethod marea :square [m] (* (:a m) (:a m)))
(defmethod marea :rect [m] (* (:w m) (:h m)))

(defn rlength [s] (.length s))
(defn rget [l i] (.get l i))
(defn rabs [x] (Math/abs x))
(defn hlength ^long [^String s] (.length s))
(defn hget [^java.util.ArrayList l ^long i] (.get l (int i)))
(defn habs ^long [^long x] (Math/abs x))

(def text
  (apply str (for [i (range 2000)]
               (str "user" i "@host" (mod i 17) ".com, word" i " Some Text; "))))

(def eval-forms
  ;; macro-heavy forms: destructuring, for, ->>, cond->, case, letfn, when-let, doseq, condp
  '[(fn [{:keys [a b c] :or {c 3} :as m} & [x y :as more]]
      (cond-> {:sum (+ a b c)} x (assoc :x x) y (update :sum + y) (:z m) (dissoc :sum)))
    (fn [coll]
      (->> coll (map inc) (filter odd?) (partition-all 3) (map (fn [[a b c]] (str a b c)))
           (into [])))
    (fn [n] (for [i (range n) j (range i) :when (odd? (+ i j)) :let [k (* i j)]] [i j k]))
    (fn [x] (case x (1 2 3) :small (4 5 6) :medium :x :keyword "s" :string :other))
    (fn [n] (letfn [(ev? [n] (if (zero? n) true (od? (dec n))))
                    (od? [n] (if (zero? n) false (ev? (dec n))))]
              (ev? n)))
    (fn [m] (when-let [{:keys [a]} (get m :inner)] (if-let [b (:b m)] (+ a b) a)))
    (fn [xs] (let [acc (volatile! 0)] (doseq [x xs :when (pos? x)] (vswap! acc + x)) @acc))
    (fn [v] (condp = v 1 :one 2 :two (condp < v 10 :big 5 :mid :small)))
    (fn [s] (try (Long/parseLong s) (catch NumberFormatException e nil) (finally nil)))
    (fn [x] (loop [i 0 acc []] (if (< i x) (recur (inc i) (conj acc (if (even? i) i (- i)))) acc)))])

;; ---------------------------------------------------------------------------------------------
;; The benchmarks: [name description setup], setup returns the thunk to time.

(def benchmarks
  [[:vector-conj "conj 100,000 longs onto []"
    (fn [] (fn [] (loop [v [] i 0] (if (< i 100000) (recur (conj v i) (inc i)) v))))]
   [:vector-transient "conj! 100,000 longs into (transient [])"
    (fn [] (fn [] (loop [v (transient []) i 0]
                    (if (< i 100000) (recur (conj! v i) (inc i)) (persistent! v)))))]
   [:vector-nth "nth 100,000 times on a 100,000-element vector"
    (fn [] (let [v (vec (range 100000))]
             (fn [] (loop [i 0 acc 0] (if (< i 100000) (recur (inc i) (+ acc (nth v i))) acc)))))]
   [:vector-assoc "assoc 10,000 indices of a 100,000-element vector"
    (fn [] (let [v (vec (range 100000))]
             (fn [] (loop [v v i 0] (if (< i 10000) (recur (assoc v (* i 7) i) (inc i)) v)))))]
   [:map-assoc "assoc 10,000 long keys into {}"
    (fn [] (fn [] (loop [m {} i 0] (if (< i 10000) (recur (assoc m i i) (inc i)) m))))]
   [:map-transient "assoc! 10,000 long keys into (transient {})"
    (fn [] (fn [] (loop [m (transient {}) i 0]
                    (if (< i 10000) (recur (assoc! m i i) (inc i)) (persistent! m)))))]
   [:map-get "get 10,000 present keys (keywords) of a 10,000-entry hash map"
    (fn [] (let [ks (mapv #(keyword (str "k" %)) (range 10000))
                 m (zipmap ks (range))]
             (fn [] (reduce (fn [acc k] (+ acc (get m k))) 0 ks))))]
   [:map-update "update-in on a nested map, 10,000 times"
    (fn [] (fn [] (loop [m {:a {:b {:c 0}}} i 0]
                    (if (< i 10000) (recur (update-in m [:a :b :c] inc) (inc i)) m))))]
   [:set-into-contains "into #{} 10,000 longs, then contains? 10,000 times"
    (fn [] (let [xs (vec (range 10000))]
             (fn [] (let [s (into #{} xs)] (reduce (fn [n x] (if (contains? s x) (inc n) n)) 0 xs)))))]
   [:lazy-seq-walk "first/next over (map inc (filter odd? (range 100,000)))"
    (fn [] (fn [] (loop [s (seq (map inc (filter odd? (range 100000)))) acc 0]
                    (if s (recur (next s) (+ acc (first s))) acc))))]
   [:lazy-for "count of a nested for, 4,500 elements of 9,000 candidates"
    (fn [] (fn [] (count (for [x (range 300) y (range 30) :when (odd? y)] [x y]))))]
   [:seq-ops "partition, interleave, take-while, distinct, sort-by on 10,000 elements"
    (fn [] (let [xs (vec (shuffle (range 10000)))]
             (fn [] [(count (partition 3 xs)) (count (interleave xs xs))
                     (count (take-while #(< % 9990) (sort xs))) (count (distinct (map #(mod % 97) xs)))
                     (first (sort-by - xs))])))]
   [:reduce-vector "reduce + over a 100,000-element vector"
    (fn [] (let [v (vec (range 100000))] (fn [] (reduce + v))))]
   [:transduce "transduce (map inc) (filter even?) (map square) + over 100,000"
    (fn [] (let [v (vec (range 100000)) xf (comp (map inc) (filter even?) (map #(* % %)))]
             (fn [] (transduce xf + v))))]
   [:into-xform "into [] with (map inc) (filter even?) over 100,000"
    (fn [] (let [v (vec (range 100000)) xf (comp (map inc) (filter even?))]
             (fn [] (into [] xf v))))]
   [:keyword-map "(:x m) on a 3-entry map, 1,000,000 times"
    (fn [] (let [m {:x 1 :y 2 :z 3}]
             (fn [] (loop [i 0 acc 0] (if (< i 1000000) (recur (inc i) (+ acc (:x m))) acc)))))]
   [:keyword-record "(:x r) on a 3-field record, 1,000,000 times"
    (fn [] (let [r (->Point 1 2 3)]
             (fn [] (loop [i 0 acc 0] (if (< i 1000000) (recur (inc i) (+ acc (:x r))) acc)))))]
   [:protocol "a protocol fn on 3 types in turn (2 records, 1 deftype), 1,000,000 calls"
    (fn [] (let [shapes (object-array [(->Circle 2) (->Square 3) (Rect. 2 5)])]
             (fn [] (loop [i 0 acc 0.0]
                      (if (< i 1000000) (recur (inc i) (+ acc (area (aget shapes (rem i 3))))) acc)))))]
   [:multimethod "a multimethod on :kind over 3 methods, 100,000 calls"
    (fn [] (let [ms (object-array [{:kind :circle :r 2} {:kind :square :a 3} {:kind :rect :w 2 :h 5}])]
             (fn [] (loop [i 0 acc 0.0]
                      (if (< i 100000) (recur (inc i) (+ acc (marea (aget ms (rem i 3))))) acc)))))]
   [:str-concat "(str \"item-\" i \":\" (* i 2) \"/\" kw), 100,000 calls"
    (fn [] (fn [] (loop [i 0 n 0]
                    (if (< i 100000) (recur (inc i) (+ n (.length ^String (str "item-" i ":" (* i 2) "/" :kw)))) n))))]
   [:str-apply "(apply str (range 1,000)), 100 times"
    (fn [] (fn [] (dotimes [_ 100] (vreset! sink (apply str (range 1000))))))]
   [:interop-reflective "un-hinted .length, .get and Math/abs, 100,000 calls each"
    (fn [] (let [l (java.util.ArrayList. ^java.util.Collection (range 100))]
             (fn [] (loop [i 0 acc 0]
                      (if (< i 100000)
                        (recur (inc i) (+ acc (rlength "abcdef") (rget l (rem i 100)) (rabs (- i))))
                        acc)))))]
   [:interop-hinted "hinted .length, .get and Math/abs, 100,000 calls each"
    (fn [] (let [l (java.util.ArrayList. ^java.util.Collection (range 100))]
             (fn [] (loop [i 0 acc 0]
                      (if (< i 100000)
                        (recur (inc i) (+ acc (hlength "abcdef") (long (hget l (rem i 100))) (habs (- i))))
                        acc)))))]
   [:prim-long-loop "primitive long loop: 10,000,000 xor/multiply/add"
    (fn [] (fn [] (loop [i 0 acc 0]
                    (if (< i 10000000) (recur (inc i) (+ acc (bit-xor i (* i 3)))) acc))))]
   [:prim-double-loop "primitive double loop: 1,000,000 sqrt/multiply/add"
    (fn [] (fn [] (loop [i 0 acc 0.0]
                    (if (< i 1000000) (recur (inc i) (+ acc (* 1.5 (Math/sqrt (double i))))) acc))))]
   [:prim-array "aset/aget on a long-array of 1,000,000"
    (fn [] (let [^longs a (long-array 1000000)]
             (fn [] (dotimes [i 1000000] (aset a i (+ i (aget a i)))) (aget a 999))))]
   [:atom-swap "swap! inc on one atom, 1,000,000 times, one thread"
    (fn [] (let [a (atom 0)] (fn [] (dotimes [_ 1000000] (swap! a inc)) @a)))]
   [:atom-contended "swap! inc on one atom, 8 threads x 100,000"
    (fn [] (fn [] (let [a (atom 0)
                        ts (mapv (fn [_] (doto (Thread. ^Runnable (fn [] (dotimes [_ 100000] (swap! a inc))))
                                           (.start)))
                                 (range 8))]
                    (doseq [^Thread t ts] (.join t))
                    @a)))]
   [:future "1,000 futures, each (inc i), then deref all"
    (fn [] (fn [] (reduce + (map deref (doall (map (fn [i] (future (inc i))) (range 1000)))))))]
   [:pmap "pmap over 64 tasks of (reduce + (range 20,000))"
    (fn [] (fn [] (reduce + (pmap (fn [n] (reduce + (range n))) (repeat 64 20000)))))]
   [:regex "re-seq of an e-mail pattern, re-find and re-matches over a 90 KB text"
    (fn [] (let [words (vec (.split ^String text " "))]
             (fn [] [(count (re-seq #"\w+@\w+\.com" text))
                     (count (keep #(re-find #"^word(\d+)" %) words))
                     (count (filter #(re-matches #"[a-z]+\d+@host\d+\.com," %) words))])))]
   [:string-lib "string library: split, join, replace (regex), upper-case, trim, includes? on a 90 KB text"
    (fn [] (let [split (lib-fn "string" 'split) join (lib-fn "string" 'join)
                 replace (lib-fn "string" 'replace) upper-case (lib-fn "string" 'upper-case)
                 trim (lib-fn "string" 'trim) includes? (lib-fn "string" 'includes?)]
             (fn [] (let [parts (split text #";")]
                      [(count (join "|" (map trim parts)))
                       (count (replace text #"\d+" "#"))
                       (count (upper-case text))
                       (count (filter #(includes? % "word1") parts))]))))]
   [:eval-macros "eval 10 macro-heavy fn forms (destructuring, for, ->>, cond->, case, letfn ...)"
    (fn [] (fn [] (mapv eval eval-forms)))]])

;; ---------------------------------------------------------------------------------------------
;; The measurement

(defn time-calls
  "The time in ns of n calls of thunk."
  ^long [thunk ^long n]
  (let [t0 (System/nanoTime)]
    (loop [i 0] (when (< i n) (vreset! sink (thunk)) (recur (inc i))))
    (- (System/nanoTime) t0)))

(defn steady [name warmup-ms measure-ms]
  (let [[_ desc setup] (first (filter #(= name (first %)) benchmarks))
        _ (when-not setup (throw (ex-info (str "no benchmark " name) {})))
        thunk (setup)
        warm-end (+ (System/nanoTime) (* 1000000 (long warmup-ms)))
        calls (loop [n 1]
                (let [t (time-calls thunk n)
                      n (if (< t 20000000) (* 2 n) n)]
                  (if (< (System/nanoTime) warm-end) (recur n) n)))
        end (+ (System/nanoTime) (* 1000000 (long measure-ms)))
        samples (loop [acc []]
                  (if (or (< (count acc) 10) (< (System/nanoTime) end))
                    (recur (conj acc (/ (double (time-calls thunk calls)) calls)))
                    acc))]
    {:kind :steady :name name :desc desc :calls-per-sample calls :samples samples}))

(defn load-lib [name]
  (let [ns (lib name)
        t0 (System/nanoTime)]
    (require ns)
    {:kind :load :name name :ns ns :ms (/ (- (System/nanoTime) t0) 1e6)}))

(let [[cmd & args] *command-line-args*
      out (case cmd
            "list" (mapv (fn [[n d]] {:name n :desc d}) benchmarks)
            "steady" (let [[n w m] args] (steady (keyword n) (Long/parseLong w) (Long/parseLong m)))
            "load" (load-lib (first args)))]
  (println "#bench" (pr-str out))
  (flush)
  (shutdown-agents))
