;; The training workload of the Go executable's profile-guided build (bin/arbace-go --build
;; --pgo, doc/go/SPEED-NOTES.md): the executable runs it with a CPU profile (its start, loading
;; arbace.core and the namespaces below through the evaluator, included), and gc builds the
;; executable again with that profile, devirtualizing and inlining the hot calls it shows. What
;; it runs: the common libraries, collections, seqs and transducers, numbers, strings and
;; regexes, printing, protocols, deftype and defrecord, dynamic vars, exceptions, atoms,
;; futures and agents, as Arbace's tests and programs use them.
(require 'arbace.repl '[arbace.string :as str] '[arbace.set :as set] '[arbace.walk :as walk]
         '[arbace.pprint :as pp] '[arbace.edn :as edn] '[arbace.test :as t]
         '[arbace.data :as data] '[arbace.zip :as zip])
(def m (into {} (map (fn [i] [(keyword (str "k" i)) (vec (range i))]) (range 50))))
(with-out-str (pp/pprint (select-keys m [:k1 :k2 :k3])))
(set/union #{1 2} #{3})
(walk/postwalk identity {:a [1 2 {:b '(3 4)}]})
(data/diff {:a 1 :b [1 2]} {:a 2 :b [1 3]})
(zip/root (zip/vector-zip [1 [2]]))
(defrecord TrainingR [a b])
(deftype TrainingT [x])
(defprotocol TrainingP (tp [x]))
(extend-protocol TrainingP Object (tp [x] x) TrainingR (tp [r] (:a r)))
(t/deftest training-test (t/is (= 1 1)) (t/is (thrown? ArithmeticException (/ 1 0))))
(binding [t/*test-out* (java.io.StringWriter.)] (t/run-tests))
(edn/read-string "{:a [1 2 #{3} \"s\" 1.5 \\c nil true]}")
(with-out-str (arbace.repl/doc map))
@(future (+ 1 1))
(let [a (agent 0)] (send a inc) (await a) @a)
(def ^:dynamic *training* 0)
(defn training-loop [n]
  (loop [i 0 acc 0]
    (if (< i n) (recur (inc i) (+ acc (* i *training*))) acc)))
(dotimes [_ 2]
  (binding [*training* 2] (training-loop 100000))
  (reduce + (range 300000))
  (reduce + (map inc (filter even? (range 100000))))
  (into [] (comp (map inc) (filter odd?) (partition-all 3)) (range 100000))
  (transduce (map #(* % %)) + (range 100000))
  (count (reduce #(assoc %1 %2 (str %2)) {} (range 20000)))
  (count (reduce conj #{} (range 20000)))
  (count (reduce conj [] (range 100000)))
  (let [v (vec (range 10000))] (reduce + (map #(nth v %) (range 10000))))
  (= (vec (range 10000)) (seq (range 10000)))
  (count (sort (map #(mod (* % 7919) 10007) (range 20000))))
  (count (sort-by :a (map (fn [i] {:a (- i) :b i}) (range 5000))))
  (frequencies (map #(mod % 17) (range 20000)))
  (group-by odd? (range 10000))
  (count (apply str (map str (range 20000))))
  (count (str/join "," (range 20000)))
  (str/split (str/join " " (range 2000)) #" ")
  (count (re-seq #"\d+" (str/join " " (range 2000))))
  (str/upper-case (apply str (repeat 1000 "abc")))
  (count (with-out-str (prn (range 2000) {:a "s" :b 1.5 :c \c})))
  (count (pr-str (into {} (map (fn [i] [i (keyword (str "k" i))]) (range 2000)))))
  (reduce + (map tp (map #(->TrainingR % %) (range 10000))))
  (reduce + (map #(.-x ^TrainingT %) (map #(TrainingT. %) (range 10000))))
  (let [a (atom 0)] (dotimes [i 20000] (swap! a + i)) @a)
  (count (for [i (range 100) j (range 100) :when (< i j)] [i j]))
  (reduce + (map (fn [x] (try (if (zero? (mod x 100)) (throw (ex-info "t" {:x x})) x)
                              (catch Exception e 0)))
                 (range 10000)))
  (letfn [(fib [n] (if (< n 2) n (+ (fib (- n 1)) (fib (- n 2)))))] (fib 18))
  (reduce + (map double (range 10000)))
  (reduce *' (range 1 200))
  (count (str (reduce + (map bigdec (range 1000))))))
(shutdown-agents)
