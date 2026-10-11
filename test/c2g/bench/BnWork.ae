;; c2g's micro workloads (doc/go/C2G-NOTES.md, "Phase 2D: performance"): the same class forms
;; run by the JVM Arbace (compiled by the class forms compiler) and by the Go program c2g
;; translates (test/c2g/bench.clj). Each workload takes a size and returns a checksum, so that
;; both runs can be compared and nothing is optimized away. Only arbace.lang's runtime classes
;; are used, not arbace.core.

(in-ns 'c2g.bench)

(import '(arbace.lang PersistentVector PersistentHashMap PersistentArrayMap PersistentList
                      IPersistentMap IPersistentVector ISeq Numbers Murmur3 Util RT Keyword Symbol))

(do

(defclass ^:public BnWork
  (field ^:private ^:static ^:final ^String/1 WORDS
    (new String/1 ["alpha" "beta" "gamma" "delta" "epsilon" "zeta" "eta" "theta"]))

  ;; PersistentVector: conj n boxed longs, then nth over all of them
  (method ^:public ^:static vecConj ^long [^int n]
    (let [^:mutable ^IPersistentVector v PersistentVector/EMPTY
          ^:mutable ^long sum 0]
      (loop [^int i 0] (when (< i n) (set! v (.cons v (Long/valueOf (long i)))) (recur (unchecked-inc-int i))))
      (loop [^int i 0] (when (< i n) (set! sum (unchecked-add sum (.longValue ^Long (.nth v i)))) (recur (unchecked-inc-int i))))
      (unchecked-add sum (long (.count v)))))

  ;; PersistentHashMap: assoc n boxed long keys, then look each up
  (method ^:public ^:static hashMapAssoc ^long [^int n]
    (let [^:mutable ^IPersistentMap m PersistentHashMap/EMPTY
          ^:mutable ^long sum 0]
      (loop [^int i 0] (when (< i n) (set! m (.assoc m (Long/valueOf (long i)) (Long/valueOf (long (unchecked-multiply-int i 3))))) (recur (unchecked-inc-int i))))
      (loop [^int i 0] (when (< i n) (set! sum (unchecked-add sum (.longValue ^Long (.valAt m (Long/valueOf (long i)))))) (recur (unchecked-inc-int i))))
      (unchecked-add sum (long (.count m)))))

  ;; PersistentArrayMap: small maps of keywords, built and read
  (method ^:public ^:static arrayMapSmall ^long [^int n]
    (let [ks (new Keyword/1 8)
          ^:mutable ^long sum 0]
      (loop [^int j 0] (when (< j 8) (aset ks j (Keyword/intern (Symbol/intern (aget WORDS j)))) (recur (unchecked-inc-int j))))
      (loop [^int i 0] (when (< i n) (let [^:mutable ^IPersistentMap m PersistentArrayMap/EMPTY]
          (loop [^int j 0] (when (< j 8) (set! m (.assoc m (aget ks j) (Long/valueOf (long j)))) (recur (unchecked-inc-int j))))
          (set! sum (unchecked-add sum (.longValue ^Long (.valAt m (aget ks (unchecked-remainder-int i 8))))))) (recur (unchecked-inc-int i))))
      sum))

  ;; hashing: Murmur3 over longs and strings, Util.hasheq over boxed numbers
  (method ^:public ^:static hashing ^long [^int n]
    (let [^:mutable ^long h 0]
      (loop [^int i 0] (when (< i n) (set! h (unchecked-add h (long (Murmur3/hashLong (long i)))))
        (set! h (unchecked-add h (long (Murmur3/hashUnencodedChars (aget WORDS (unchecked-remainder-int i 8))))))
        (set! h (unchecked-add h (long (Util/hasheq (Long/valueOf (long i)))))) (recur (unchecked-inc-int i))))
      h))

  ;; boxed arithmetic through Numbers (long and double paths)
  (method ^:public ^:static numbers ^long [^int n]
    (let [^:mutable ^Object acc (Long/valueOf 0)
          ^:mutable ^Object d (Double/valueOf 0.5)]
      (loop [^int i 0] (when (< i n) (set! acc (Numbers/add acc (Long/valueOf (long i))))
        (set! d (Numbers/multiply d (Double/valueOf 1.0000001)))
        (when (Numbers/gt acc (Long/valueOf 1000000000)) (set! acc (Long/valueOf 0))) (recur (unchecked-inc-int i))))
      (unchecked-add (.longValue ^Number acc) (long (.doubleValue ^Number d)))))

  ;; seqs: a PersistentList consed and walked with first/next
  (method ^:public ^:static seqWalk ^long [^int n]
    (let [^:mutable ^ISeq s PersistentList/EMPTY
          ^:mutable ^long sum 0]
      (loop [^int i 0] (when (< i n) (set! s (.cons s (Long/valueOf (long i)))) (recur (unchecked-inc-int i))))
      (loop [^ISeq x (.seq s)]
        (when (some? x)
          (set! sum (unchecked-add sum (.longValue ^Long (.first x))))
          (recur (.next x))))
      sum))

  ;; equality of vectors (Util.equiv over element-wise equiv)
  (method ^:public ^:static vecEquiv ^long [^int n]
    (let [^:mutable ^IPersistentVector a PersistentVector/EMPTY
          ^:mutable ^IPersistentVector b PersistentVector/EMPTY
          ^:mutable ^long k 0]
      (loop [^int i 0] (when (< i 1000) (set! a (.cons a (Long/valueOf (long i))))
        (set! b (.cons b (Long/valueOf (long i)))) (recur (unchecked-inc-int i))))
      (loop [^int i 0] (when (< i n) (when (Util/equiv a b) (set! k (unchecked-inc k))) (recur (unchecked-inc-int i))))
      k))

  ;; strings: StringBuilder appends and String methods
  (method ^:public ^:static strings ^long [^int n]
    (let [^:mutable ^long sum 0]
      (loop [^int i 0] (when (< i n) (let [sb (StringBuilder.)]
          (.append sb (aget WORDS (unchecked-remainder-int i 8)))
          (.append sb i)
          (.append sb \-)
          (set! sum (unchecked-add sum (long (.hashCode (.toString sb)))))) (recur (unchecked-inc-int i))))
      sum))))
