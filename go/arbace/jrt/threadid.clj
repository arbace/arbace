;; jrt: the current thread's number, for monitors and class initialization.
;;
;; Phase 1's stopgap (C2G-SPEC §8.4): the goroutine id parsed from runtime.Stack's header,
;; about 3 µs. Phase 2 replaces currentThreadID with the number of the current Thread found
;; through the goroutine-local slot of the patched runtime (§9.3), 3.7 ns.
(in-ns 'go.arbace.jrt)

(go/file "threadid.go"
  :imports [[runtime "runtime"]])

(go/func currentThreadID
  "currentThreadID is a number identifying the current thread, never 0.\n"
  ^uint64 []
  (let [^{:tag (array 64 byte)} buf (zero (array 64 byte))
        n (runtime/Stack (subslice buf) false)
        b (subslice buf _ n)
        ^uint64 id 0]
    ;; "goroutine 123 [running]:..."
    (let [i (len "goroutine ")]
      (while (and (< i (len b)) (>= (aget b i) \0) (<= (aget b i) \9))
        (set! id (+ (* id 10) (conv uint64 (- (aget b i) \0))))
        (inc! i)))
    (+ id 1)))
