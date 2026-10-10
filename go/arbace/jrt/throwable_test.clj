;; jrt's tests: exceptions as C2G-SPEC §7.9 has c2g write them (try bodies as function
;; literals with jrt.Catch, control codes, finally), Go's run-time errors mapped to Java's
;; exceptions, Throwable's API, stack traces, printStackTrace's text.
(in-ns 'go.arbace.jrt)

(go/file "throwable_test.go"
  :imports [[strings "strings"] [testing "testing"]])

;; ---------------------------------------------------------------------------------------
;; try/catch/finally as c2g writes them

(go/func tryCatch
  "tryCatch is (try (body) (catch IllegalStateException e \"ise\") (catch RuntimeException e
\"rte\")) with a value, c2g's §7.9.2 shape; other exceptions propagate.\n"
  ^string [^{:tag (func [])} body]
  (let [exc ((fn [] :results [^Throwable_I exc]
               (defer (Catch (addr exc)))
               (body)
               (return)))]
    (when (!= exc nil)
      (cond
        (IllegalStateException_InstanceOf exc) (let [e (IllegalStateException_Cast exc)] (return (+ "ise:" (.String (.GetMessage__String e)))))
        (RuntimeException_InstanceOf exc) (let [e (RuntimeException_Cast exc)] (return (+ "rte:" (.String (.GetName__String (.GetClass__Class e))))))
        :else (panic exc)))
    "ok"))

(go/func TestTryCatch [^{:tag (* testing/T)} t]
  (let [cases (lit (slice (struct ^{:tag (func [])} body ^string want))
                   (lit _ (fn []) "ok")
                   (lit _ (fn [] (panic (Thrown (IllegalStateException_New_String (Str "a"))))) "ise:a")
                   (lit _ (fn [] (panic (Thrown (UnsupportedOperationException_New)))) "rte:java.lang.UnsupportedOperationException")
                   ;; Go's run-time errors arrive as Java's exceptions
                   (lit _ (fn [] (let [^{:tag (* Throwable)} p nil] (set! _ (.-detailMessage p)))) "rte:java.lang.NullPointerException")
                   (lit _ (fn [] (let [a (NewIntArray 2) i (conv int32 2)] (set! _ (aget (.-A a) i)))) "rte:java.lang.ArrayIndexOutOfBoundsException")
                   (lit _ (fn [] (let [z (conv int32 0)] (set! _ (aget (.-A (NewIntArray 1)) (/ 1 z))))) "rte:java.lang.ArithmeticException")
                   (lit _ (fn [] (let [^Throwable_I e nil] (.GetMessage__String e))) "rte:java.lang.NullPointerException")
                   (lit _ (fn [] (let [^{:tag (* String)} s nil] (.Length__I (NN s)))) "rte:java.lang.NullPointerException")
                   (lit _ (fn [] (let [^any x (Str "s")] (set! _ (assert (* Class) x)))) "rte:java.lang.ClassCastException")
                   (lit _ (fn [] (panic (Thrown nil))) "rte:java.lang.NullPointerException"))]
    (range [_ c cases]
      (let [got (tryCatch (.-body c))]
        (when (!= got (.-want c))
          (.Errorf t "got %q, want %q" got (.-want c))))))
  ;; an Error is not caught by catch RuntimeException: it propagates
  (let [got (res (fn ^string [] (tryCatch (fn [] (panic (Thrown (AssertionError_New)))))))]
    (when (!= got "!java.lang.AssertionError")
      (.Errorf t "Error through catch RuntimeException: %q" got))))

(go/func TestRuntimeErrorMessages
  "The messages of the mapped run-time errors are Java's.\n"
  [^{:tag (* testing/T)} t]
  (let [cases (lit (slice (struct ^{:tag (func [] [string])} f ^string want))
                   (lit _ (fn ^string [] (let [a (NewIntArray 3) i (conv int32 7)] (ires (aget (.-A a) i))))
                        "!java.lang.ArrayIndexOutOfBoundsException: Index 7 out of bounds for length 3")
                   (lit _ (fn ^string [] (let [a (NewIntArray 3) i (conv int32 -1)] (ires (aget (.-A a) i))))
                        "!java.lang.ArrayIndexOutOfBoundsException: Index -1 out of bounds for length 3")
                   ;; the index computed: gc for arm64 (go1.27.1) reports a constant negative index against a
                   ;; length it does not know as 0 (SPEED-NOTES.md)
                   (lit _ (fn ^string [] (let [a (NewRefArray Object_class 0) i (- (conv int64 (len (.-A a))) 5)] (sres (StrOfObj (aget (.-A a) i)))))
                        "!java.lang.ArrayIndexOutOfBoundsException: Index -5 out of bounds for length 0")
                   (lit _ (fn ^string [] (let [a (NewRefArray String_class 2)] (.Store a 3 (Object_New)) "x"))
                        "!java.lang.ArrayIndexOutOfBoundsException: Index 3 out of bounds for length 2")
                   (lit _ (fn ^string [] (let [a (NewRefArray String_class 2)] (.Store a 1 (Object_New)) "x"))
                        "!java.lang.ArrayStoreException: java.lang.Object")
                   (lit _ (fn ^string [] (let [a (conv int64 -9223372036854775808) b (conv int64 0)] (strings/Repeat "x" (conv int (% a b)))))
                        "!java.lang.ArithmeticException: / by zero")
                   (lit _ (fn ^string [] (let [^Throwable_I e nil] (sres (.ToString__String e))))
                        "!java.lang.NullPointerException"))]
    (range [_ c cases]
      (let [got (res (.-f c))]
        (when (!= got (.-want c))
          (.Errorf t "got %q, want %q" got (.-want c))))))
  ;; Java's overflow cases of / and %: no exception
  (let [a (conv int32 -2147483648)
        b (conv int32 -1)]
    (when (or (!= (/ a b) -2147483648) (!= (% a b) 0))
      (.Error t "MIN_VALUE / -1"))))

(go/func finallyOrder
  "finallyOrder is c2g's §7.9.4 shape of
   (try (log \"body\") (when ret (return \"returned\")) (when thr (throw (ISE.)))
     (catch IllegalStateException e (log \"catch\") (when rethrow (throw e)))
     (finally (log \"finally\")))
   followed by (log \"after\") \"normal\".\n"
  ^string [^{:tag (* (slice string))} log ^bool ret ^bool thr ^bool rethrow]
  (let [(values ctl rv exc)
        ((fn [] :results [^int32 ctl ^string rv ^Throwable_I exc]
           (defer (Catch (addr exc)))
           (let [exc2 ((fn [] :results [^Throwable_I exc]
                         (defer (Catch (addr exc)))
                         (set! @log (append @log "body"))
                         (when ret
                           (set! (values ctl rv) (values 1 "returned"))
                           (return))
                         (when thr
                           (panic (Thrown (IllegalStateException_New))))
                         (return)))]
             (when (> ctl 0)
               (return))
             (when (!= exc2 nil)
               (if (IllegalStateException_InstanceOf exc2)
                 (do
                   (set! @log (append @log "catch"))
                   (when rethrow
                     (panic exc2)))
                 (panic exc2))))
           (return)))]
    (set! @log (append @log "finally"))
    (when (!= exc nil)
      (panic exc))
    (when (== ctl 1)
      (return rv))
    (set! @log (append @log "after"))
    "normal"))

(go/func TestFinally [^{:tag (* testing/T)} t]
  (let [cases (lit (slice (struct ^bool ret ^bool thr ^bool rethrow ^string want ^string log))
                   (lit _ false false false "normal" "body finally after")
                   (lit _ true false false "returned" "body finally")
                   (lit _ false true false "normal" "body catch finally after")
                   (lit _ false true true "!java.lang.IllegalStateException" "body catch finally"))]
    (range [_ c cases]
      (let [^{:tag (slice string)} log nil
            got (res (fn ^string [] (finallyOrder (addr log) (.-ret c) (.-thr c) (.-rethrow c))))]
        (when (or (!= got (.-want c)) (!= (strings/Join log " ") (.-log c)))
          (.Errorf t "%v: got %q, log %q" c got log))))))

(go/func breakOutOfTry
  "breakOutOfTry is a loop whose try body breaks and continues (control codes 2 and 3).\n"
  ^int [^int n]
  (let [sum 0]
    (label :L1
      (for [i 0] (< i n) (inc! i)
        (let [(values ctl exc) ((fn [] :results [^int32 ctl ^Throwable_I exc]
                                  (defer (Catch (addr exc)))
                                  (when (== i 5)
                                    (return 2 nil))
                                  (when (== (% i 2) 0)
                                    (return 3 nil))
                                  (set! sum (+ sum i))
                                  (return 0 nil)))]
          (set! sum (+ sum 100))                     ; the finally code
          (when (!= exc nil) (panic exc))
          (switch ctl
            (case [2] (break :L1))
            (case [3] (continue :L1))))))
    sum))

(go/func TestControlCodes [^{:tag (* testing/T)} t]
  ;; i = 0..5: odd 1 and 3 summed, 100 per finally (6 times)
  (when (!= (breakOutOfTry 10) 604)
    (.Errorf t "control codes: %d" (breakOutOfTry 10))))

;; ---------------------------------------------------------------------------------------
;; Throwable's API

(go/func TestThrowableAPI [^{:tag (* testing/T)} t]
  (let [cause (IllegalStateException_New_String (Str "inner"))
        e (RuntimeException_New_String_Throwable (Str "outer") cause)]
    (when (!= (.GetCause__Throwable e) cause)
      (.Error t "getCause"))
    (when (!= (.String (.ToString__String e)) "java.lang.RuntimeException: outer")
      (.Errorf t "toString %q" (.ToString__String e)))
    (.AddSuppressed_Throwable__V e (ArithmeticException_New_String (Str "s1")))
    (when (!= (len (.-A (.GetSuppressed__Throwable1 e))) 1)
      (.Error t "suppressed"))
    ;; the four-argument constructor: no suppression, no stack trace
    (let [q (Throwable_New_String_Throwable_Z_Z (Str "q") nil false false)]
      (.AddSuppressed_Throwable__V q cause)
      (when (or (!= (len (.-A (.GetSuppressed__Throwable1 q))) 0) (!= (len (.-A (.GetStackTrace__StackTraceElement1 q))) 0))
        (.Error t "four-argument constructor"))
      ;; setStackTrace is ignored when not writable
      (.SetStackTrace_StackTraceElement1__V q (RefArrayOf StackTraceElement_class (StackTraceElement_New_String_String_String_I (Str "a.B") (Str "m") nil 1)))
      (when (!= (len (.-A (.GetStackTrace__StackTraceElement1 q))) 0)
        (.Error t "setStackTrace on an unwritable trace")))
    ;; setStackTrace replaces; getStackTrace returns copies
    (.SetStackTrace_StackTraceElement1__V e (RefArrayOf StackTraceElement_class (StackTraceElement_New_String_String_String_I (Str "a.B") (Str "m") (Str "B.java") 7)))
    (let [st (.GetStackTrace__StackTraceElement1 e)]
      (aset (.-A st) 0 nil)
      (when (!= (.String (StrOfObj (aget (.-A (.GetStackTrace__StackTraceElement1 e)) 0))) "a.B.m(B.java:7)")
        (.Error t "getStackTrace is not a copy")))
    (when (!= (res (fn ^string [] (.SetStackTrace_StackTraceElement1__V e (RefArrayOf StackTraceElement_class nil)) "x"))
              "!java.lang.NullPointerException: stackTrace[0]")
      (.Error t "setStackTrace with null element"))
    ;; Thrown, Catch of a Java exception keeps the object
    (let [^Throwable_I got nil]
      (set! got ((fn [] :results [^Throwable_I exc]
                   (defer (Catch (addr exc)))
                   (panic (Thrown e)))))
      (when (!= got e)
        (.Error t "the caught exception is not the thrown object")))
    ;; Throwable_Cast and the ClassCast message
    (when (!= (res (fn ^string [] (Throwable_Cast (Str "x")) "x"))
              "!java.lang.ClassCastException: class java.lang.String cannot be cast to class java.lang.Throwable (java.lang.String and java.lang.Throwable are in module java.base of loader 'bootstrap')")
      (.Error t "Throwable_Cast"))))

;; ---------------------------------------------------------------------------------------
;; Stack traces

(go/func init []
  (RegisterFrames
    (lit (slice FrameInfo)
         (lit _ "arbace/jrt.traceLevel1" "test.Tracer" "level1" 0)
         (lit _ "arbace/jrt.traceLevel2" "test.Tracer" "level2" 0)
         (lit _ "arbace/jrt.traceTry" "test.Tracer" "withTry" 0)
         (lit _ "arbace/jrt.traceNil" "test.Tracer" "nilDeref" 0)
         (lit _ "arbace/jrt.traceHidden" "test.Tracer" "hidden" FrameElide)
         (lit _ "arbace/jrt.TestStackTraces" "test.Tracer" "main" 0)
         (lit _ "arbace/jrt.TestPrintStackTrace" "test.Tracer" "main" 0)
         (lit _ "arbace/jrt.tracePair" "test.Tracer" "pair" 0))))

(go/func tracePair "an exception and its cause, made in one frame\n" ^Throwable_I []
  (let [inner (traceLevel1)
        outer (RuntimeException_New_String_Throwable (Str "outer") inner)]
    (.AddSuppressed_Throwable__V outer (ArithmeticException_New_String (Str "sup")))
    outer))

(go/func traceLevel2 ^Throwable_I [] (IllegalStateException_New_String (Str "deep")))
(go/func traceLevel1 ^Throwable_I [] (traceHidden))
(go/func traceHidden ^Throwable_I [] (traceLevel2))

(go/func traceTry "an exception made in a try body's function literal\n" ^Throwable_I []
  (let [^Throwable_I made nil]
    ((fn [] (set! made (RuntimeException_New))))
    made))

(go/func traceNil "a nil dereference, caught\n" ^Throwable_I []
  ((fn [] :results [^Throwable_I exc]
     (defer (Catch (addr exc)))
     (let [^{:tag (* Throwable)} p nil]
       (set! (.-causeSet p) true))
     (return))))

(go/func frames ^string [^Throwable_I e]
  (let [^{:tag (slice string)} parts nil]
    (range [_ x (.-A (.GetStackTrace__StackTraceElement1 e))]
      (let [s (assert (* StackTraceElement) x)]
        (set! parts (append parts (+ (.String (.GetClassName__String s)) "." (.String (.GetMethodName__String s)))))))
    (strings/Join parts " ")))

(go/func TestStackTraces [^{:tag (* testing/T)} t]
  ;; the constructors and fillInStackTrace are not shown; an elided frame is left out
  (let [f (frames (traceLevel1))]
    (when (!= f "test.Tracer.level2 test.Tracer.level1 test.Tracer.main")
      (.Errorf t "frames: %q" f)))
  ;; a function literal's frame stands for its function
  (let [f (frames (traceTry))]
    (when (!= f "test.Tracer.withTry test.Tracer.main")
      (.Errorf t "try literal: %q" f)))
  ;; an implicit exception's trace starts at the failing frame
  (let [f (frames (traceNil))]
    (when (!= f "test.Tracer.nilDeref test.Tracer.main")
      (.Errorf t "nil dereference: %q" f)))
  ;; jrt's own methods are named as Java's: String.charAt
  (let [e ((fn [] :results [^Throwable_I exc]
             (defer (Catch (addr exc)))
             (.CharAt_I__C (Str "abc") 9)
             (return)))
        f (frames e)]
    (when (not (strings/HasPrefix f "java.lang.String.charAt test.Tracer.main"))
      (.Errorf t "String.charAt: %q" f)))
  ;; file names and lines: Go's (the forms' with --line-file)
  (let [st (.GetStackTrace__StackTraceElement1 (traceLevel1))
        top (assert (* StackTraceElement) (aget (.-A st) 0))]
    (when (or (!= (.String (.GetFileName__String top)) "throwable_test.go") (<= (.GetLineNumber__I top) 0))
      (.Errorf t "file and line: %s" (.String (StrOfObj top))))))

(go/func TestPrintStackTrace [^{:tag (* testing/T)} t]
  (let [outer (tracePair)]
    (let [text (.String (StackTraceString outer))
          lines (strings/Split text "\n")]
    (when (or (!= (aget lines 0) "java.lang.RuntimeException: outer")
              (not (strings/HasPrefix (aget lines 1) "\tat test.Tracer.pair(throwable_test.go:"))
              (not (strings/HasPrefix (aget lines 2) "\tat test.Tracer.main(throwable_test.go:"))
              (not (strings/HasPrefix (aget lines 3) "\tSuppressed: java.lang.ArithmeticException: sup"))
              (not (strings/Contains text "\t\t... 1 more\n"))
              (not (strings/Contains text "Caused by: java.lang.IllegalStateException: deep\n\tat test.Tracer.level2(throwable_test.go:"))
              (not (strings/HasSuffix text "\t... 1 more\n")))
      (.Errorf t "printStackTrace:\n%s" text))))
  ;; a cycle of causes
  (let [a (RuntimeException_New_String (Str "a"))
        b (RuntimeException_New_String_Throwable (Str "b") a)]
    (.SetCause_Throwable__V a b)
    (when (not (strings/Contains (.String (StackTraceString b)) "Caused by: [CIRCULAR REFERENCE: java.lang.RuntimeException: b]"))
      (.Errorf t "circular: %s" (.String (StackTraceString b)))))
  ;; the uncaught handler's text
  (let [saved StderrPrint
        out ""]
    (set! StderrPrint (fn [^string s] (set! out (+ out s))))
    (Uncaught "main" (IllegalStateException_New_String (Str "x")))
    (set! StderrPrint saved)
    (when (not (strings/HasPrefix out "Exception in thread \"main\" java.lang.IllegalStateException: x\n\tat "))
      (.Errorf t "uncaught: %q" out))))
