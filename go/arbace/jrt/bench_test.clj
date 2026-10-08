;; jrt's benchmarks (JRT-NOTES.md, "Measurements"): bin/jrt test --arch amd64 -run X -bench . -cpu 1
(in-ns 'go.arbace.jrt)

(go/file "bench_test.go"
  :imports [[testing "testing"]])

(go/var ^int32 sinkI)
(go/var ^any sinkA)
(go/var ^{:tag (* String)} sinkS)

(go/func BenchmarkConcat [^{:tag (* testing/B)} b]
  (let [a (Intern "count: ")
        c (Intern " items")]
    (for [i 0] (< i (.-N b)) (inc! i)
      (set! sinkS (Concat a (StrOfInt (conv int32 i)) c)))))

(go/func BenchmarkStrOfDouble [^{:tag (* testing/B)} b]
  (for [i 0] (< i (.-N b)) (inc! i)
    (set! sinkS (StrOfDouble (* (conv float64 i) 0.1)))))

(go/func BenchmarkStringHashCode16 "the hash of a new 16-unit string\n" [^{:tag (* testing/B)} b]
  (let [v (.-value (Str "0123456789abcdef"))]
    (for [i 0] (< i (.-N b)) (inc! i)
      (set! sinkI (.HashCode__I (newString v))))))

(go/func BenchmarkStringEquals16 [^{:tag (* testing/B)} b]
  (let [x (Str "0123456789abcdef")
        y (Str "0123456789abcdef")]
    (for [i 0] (< i (.-N b)) (inc! i)
      (when (.Equals_O__Z x y)
        (inc! sinkI)))))

(go/func BenchmarkIntern [^{:tag (* testing/B)} b]
  (let [s (Str "an interned string")]
    (for [i 0] (< i (.-N b)) (inc! i)
      (set! sinkS (.Intern__String s)))))

(go/func BenchmarkToUpperCaseASCII [^{:tag (* testing/B)} b]
  (let [s (Str "hello, world")]
    (for [i 0] (< i (.-N b)) (inc! i)
      (set! sinkS (.ToUpperCase__String s)))))

(go/func BenchmarkStringBuilder "append a string, an int and a char, then toString\n"
  [^{:tag (* testing/B)} b]
  (let [s (Intern "x=")]
    (for [i 0] (< i (.-N b)) (inc! i)
      (let [sb (StringBuilder_New)]
        (.Append_C__StringBuilder (.Append_I__StringBuilder (.Append_String__StringBuilder sb s) (conv int32 i)) \;)
        (set! sinkS (.ToString__String sb))))))

(go/func BenchmarkIdentityHashNew "the identity hash of a new object (assigned)\n" [^{:tag (* testing/B)} b]
  (for [i 0] (< i (.-N b)) (inc! i)
    (set! sinkI (IdentityHash (Object_New)))))

(go/func BenchmarkIdentityHash "the identity hash of an object (read)\n" [^{:tag (* testing/B)} b]
  (let [o (Object_New)]
    (for [i 0] (< i (.-N b)) (inc! i)
      (set! sinkI (IdentityHash o)))))

(go/func BenchmarkCurrentThreadID "phase 1's stopgap (runtime.Stack)\n" [^{:tag (* testing/B)} b]
  (for [i 0] (< i (.-N b)) (inc! i)
    (set! sinkI (conv int32 (currentThreadID)))))

(go/func BenchmarkMonitorUncontended "MonitorEnter and MonitorExit, thin, with the stopgap thread id\n"
  [^{:tag (* testing/B)} b]
  (let [o (Object_New)]
    (for [i 0] (< i (.-N b)) (inc! i)
      (MonitorEnter o)
      (MonitorExit o))))

(go/var ^{:tag ClassInit} benchInit)
(go/func benchClinit [])
(go/func benchC_Init [] (when (not (.Done benchInit)) (.Run benchInit benchClinit)))

(go/func BenchmarkClassInitGuard "C_Init's fast path\n" [^{:tag (* testing/B)} b]
  (benchC_Init)
  (for [i 0] (< i (.-N b)) (inc! i)
    (benchC_Init)))

(go/func BenchmarkRefArrayStore "a checked store (String[])\n" [^{:tag (* testing/B)} b]
  (let [a (NewRefArray String_class 8)
        s (Str "x")]
    (for [i 0] (< i (.-N b)) (inc! i)
      (.Store a (conv int32 (bit-and i 7)) s))))

(go/func benchTry ^int32 [^int32 x] :results [^int32 r]
  (let [exc ((fn [] :results [^Throwable_I exc]
               (defer (Catch (addr exc)))
               (set! r (+ x 1))
               (return)))]
    (when (!= exc nil)
      (set! r -1))
    (return)))

(go/func BenchmarkTryNoThrow "an entered try that does not throw\n" [^{:tag (* testing/B)} b]
  (for [i 0] (< i (.-N b)) (inc! i)
    (set! sinkI (benchTry (conv int32 i)))))

(go/func BenchmarkThrowCatch "new IllegalStateException, throw, catch, one frame deep\n"
  [^{:tag (* testing/B)} b]
  (for [i 0] (< i (.-N b)) (inc! i)
    (let [exc ((fn [] :results [^Throwable_I exc]
                 (defer (Catch (addr exc)))
                 (panic (Thrown (IllegalStateException_New)))))]
      (set! sinkA exc))))

(go/func BenchmarkCatchNilDereference "a nil dereference caught as NullPointerException\n"
  [^{:tag (* testing/B)} b]
  (for [i 0] (< i (.-N b)) (inc! i)
    (let [exc ((fn [] :results [^Throwable_I exc]
                 (defer (Catch (addr exc)))
                 (let [^{:tag (* Throwable)} p nil]
                   (set! (.-causeSet p) true))
                 (return)))]
      (set! sinkA exc))))

(go/func BenchmarkGetStackTrace "mapping a captured trace to StackTraceElements\n"
  [^{:tag (* testing/B)} b]
  (let [e (IllegalStateException_New)]
    (for [i 0] (< i (.-N b)) (inc! i)
      (set! (.-stackTrace (.Self_Throwable e)) nil)
      (set! sinkA (.GetStackTrace__StackTraceElement1 e)))))

(go/func BenchmarkMathPow "FdLibm's pow, ported\n" [^{:tag (* testing/B)} b]
  (let [x 1.0001]
    (for [i 0] (< i (.-N b)) (inc! i)
      (set! sinkI (conv int32 (Math_Pow_D_D__D x (conv float64 (bit-and i 1023))))))))

(go/func BenchmarkMathLog "FdLibm's log, ported\n" [^{:tag (* testing/B)} b]
  (for [i 0] (< i (.-N b)) (inc! i)
    (set! sinkI (conv int32 (Math_Log_D__D (conv float64 (+ i 1)))))))
