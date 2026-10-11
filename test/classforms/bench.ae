;; Timings of the class forms at the REPL (doc/go/CLASSFORMS-REPL.md, "Results"): the same work as
;; a class form's methods and as Clojure fns, on whichever Arbace runs it (bin/arbace-j, or the Go
;; build: bin/arbace-go test/classforms/bench.clj).
(defclass ^:public CFBench
  (method ^:public ^:static sumSq ^long [^int n]
    (let [^:mutable ^long s 0]
      (loop [^int i 0]
        (when (< i n)
          (set! s (unchecked-add s (unchecked-multiply (unchecked-long i) i)))
          (recur (unchecked-inc-int i))))
      s))
  (method ^:public ^:static fib ^int [^int n]
    (if (< n 2) n (unchecked-add-int (CFBench/fib (unchecked-dec-int n)) (CFBench/fib (unchecked-subtract-int n 2)))))
  (method ^:public ^:static sieve ^int [^int n]
    (let [a (new boolean/1 (unchecked-inc-int n))
          ^:mutable ^int c 0]
      (loop [^int i 2]
        (when (<= i n)
          (when (not (aget a i))
            (set! c (unchecked-inc-int c))
            (loop [^int j (unchecked-multiply-int i 2)]
              (when (<= j n)
                (aset a j true)
                (recur (unchecked-add-int j i)))))
          (recur (unchecked-inc-int i))))
      c))
  (field ^:private ^int v)
  (constructor ^:public [this ^int v] (set! (.-v this) v))
  (method ^:public get ^int [this] v)
  (method ^:public ^:static objs ^long [^int n]
    (let [^:mutable ^long s 0]
      (dotimes [i n] (set! s (unchecked-add s (.get (CFBench. (unchecked-int i))))))
      s))
  (method ^:public ^:static strs ^int [^int n]
    (let [sb (StringBuilder.)]
      (dotimes [i n] (.append sb (java-str i ",")))
      (.length sb))))

(defn sum-sq [^long n] (loop [i 0 s 0] (if (< i n) (recur (inc i) (+ s (* i i))) s)))
(defn fib [^long n] (if (< n 2) n (+ (fib (dec n)) (fib (- n 2)))))

(defmacro timed [label expr]
  `(let [t0# (System/nanoTime) v# ~expr]
     (println (format "%-28s %10.1f ms  %s" ~label (/ (- (System/nanoTime) t0# ) 1e6) (pr-str v#)))))

(dotimes [_ 2]
  (timed "class form: sumSq 1e6" (CFBench/sumSq 1000000))
  (timed "Clojure fn: sum-sq 1e6" (sum-sq 1000000))
  (timed "class form: fib 22" (CFBench/fib 22))
  (timed "Clojure fn: fib 22" (fib 22))
  (timed "class form: sieve 1e6" (CFBench/sieve 1000000))
  (timed "class form: objs 1e5" (CFBench/objs 100000))
  (timed "class form: strs 1e5" (CFBench/strs 100000)))
