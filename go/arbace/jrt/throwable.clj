;; jrt: exceptions (C2G-SPEC §7.9): Throwable, the panic and catch machinery, the mapping of
;; Go's run-time errors, stack traces.
(in-ns 'go.arbace.jrt)

(go/file "throwable.go"
  :imports [[os "os"] [reflect "reflect"] [runtime "runtime"] [strconv "strconv"]
            [strings "strings"] [sync "sync"]])

;; ---------------------------------------------------------------------------------------
;; Throwable (hand-written, a non-leaf class: Throwable_I, implementations Impl_..., §5.4)

(go/type Throwable_I
  "Throwable_I is java.lang.Throwable's class interface: every Java exception, the value of
every panic jrt throws or catches.\n"
  (interface Object_I
    (Self_Throwable ^{:tag (* Throwable)} [])
    (Is_Serializable [])
    (GetMessage__String ^{:tag (* String)} [])
    (GetLocalizedMessage__String ^{:tag (* String)} [])
    (GetCause__Throwable ^Throwable_I [])
    (InitCause_Throwable__Throwable ^Throwable_I [^Throwable_I cause])
    (SetCause_Throwable__V [^Throwable_I cause])
    (FillInStackTrace__Throwable ^Throwable_I [])
    (GetStackTrace__StackTraceElement1 ^{:tag (* RefArray)} [])
    (SetStackTrace_StackTraceElement1__V [^{:tag (* RefArray)} trace])
    (PrintStackTrace__V [])
    (AddSuppressed_Throwable__V [^Throwable_I e])
    (GetSuppressed__Throwable1 ^{:tag (* RefArray)} [])))

(go/type Throwable
  "Throwable is java.lang.Throwable's struct. The stack is captured as program counters when
the exception is constructed (fillInStackTrace) and mapped to StackTraceElements on first
use.\n"
  (struct Object
          ^{:tag (* String)} detailMessage
          ^Throwable_I cause
          ^bool causeSet
          ^{:tag (slice uintptr)} pcs
          ^{:tag (* RefArray)} stackTrace
          ^{:tag (slice Throwable_I)} suppressed
          ^bool noSuppression
          ^bool notWritable))

(go/var Throwable_class
  (Define (addr (lit ClassInfo :Name "java.lang.Throwable" :Kind KindClass :Modifiers AccPublic
                     :Super Object_class :Interfaces (lit (slice (* Class)) Serializable_class)
                     :Go "arbace/jrt.Throwable"))))

(go/var ^{:doc "Throwable_jfrTracing is Throwable.jfrTracing: JFR is not part of jrt.\n"
          :tag bool} Throwable_jfrTracing)

(go/func ThrowableTracer_TraceError_Class_String__V
  "ThrowableTracer_TraceError_Class_String__V is jdk.internal.event.ThrowableTracer.traceError:
nothing (no JFR).\n"
  [^{:tag (* Class)} c ^{:tag (* String)} msg])

(go/func Throwable_New ^{:tag (* Throwable)} []
  (let [t (addr (lit Throwable))] (.Ctor t t) t))
(go/func Throwable_New_String ^{:tag (* Throwable)} [^{:tag (* String)} msg]
  (let [t (addr (lit Throwable))] (.Ctor_String t t msg) t))
(go/func Throwable_New_String_Throwable ^{:tag (* Throwable)} [^{:tag (* String)} msg ^Throwable_I cause]
  (let [t (addr (lit Throwable))] (.Ctor_String_Throwable t t msg cause) t))
(go/func Throwable_New_Throwable ^{:tag (* Throwable)} [^Throwable_I cause]
  (let [t (addr (lit Throwable))] (.Ctor_Throwable t t cause) t))
(go/func Throwable_New_String_Throwable_Z_Z ^{:tag (* Throwable)}
  [^{:tag (* String)} msg ^Throwable_I cause ^bool enableSuppression ^bool writableStackTrace]
  (let [t (addr (lit Throwable))] (.Ctor_String_Throwable_Z_Z t t msg cause enableSuppression writableStackTrace) t))

;; the constructors' bodies; this is the whole object (§5.4)
(go/method Ctor [^{:tag (* Throwable)} t ^Throwable_I this]
  (.FillInStackTrace__Throwable this))
(go/method Ctor_String [^{:tag (* Throwable)} t ^Throwable_I this ^{:tag (* String)} msg]
  (.FillInStackTrace__Throwable this)
  (set! (.-detailMessage t) msg))
(go/method Ctor_String_Throwable [^{:tag (* Throwable)} t ^Throwable_I this ^{:tag (* String)} msg ^Throwable_I cause]
  (.FillInStackTrace__Throwable this)
  (set! (.-detailMessage t) msg)
  (set! (.-cause t) cause)
  (set! (.-causeSet t) true))
(go/method Ctor_Throwable [^{:tag (* Throwable)} t ^Throwable_I this ^Throwable_I cause]
  (.FillInStackTrace__Throwable this)
  (when (!= cause nil)
    (set! (.-detailMessage t) (.ToString__String cause)))
  (set! (.-cause t) cause)
  (set! (.-causeSet t) true))
(go/method Ctor_String_Throwable_Z_Z
  [^{:tag (* Throwable)} t ^Throwable_I this ^{:tag (* String)} msg ^Throwable_I cause
   ^bool enableSuppression ^bool writableStackTrace]
  (if writableStackTrace
    (.FillInStackTrace__Throwable this)
    (set! (.-notWritable t) true))
  (set! (.-detailMessage t) msg)
  (set! (.-cause t) cause)
  (set! (.-causeSet t) true)
  (when (not enableSuppression)
    (set! (.-noSuppression t) true)))

;; the implementations
(go/method Impl_GetMessage__String ^{:tag (* String)} [^{:tag (* Throwable)} t ^Throwable_I this]
  (.-detailMessage t))

(go/method Impl_GetLocalizedMessage__String ^{:tag (* String)} [^{:tag (* Throwable)} t ^Throwable_I this]
  (.GetMessage__String this))

(go/method Impl_GetCause__Throwable ^Throwable_I [^{:tag (* Throwable)} t ^Throwable_I this]
  (when (not (.-causeSet t))
    (return nil))
  (.-cause t))

(go/method Impl_InitCause_Throwable__Throwable ^Throwable_I
  [^{:tag (* Throwable)} t ^Throwable_I this ^Throwable_I cause]
  (when (.-causeSet t)
    (let [s (Str "a null")]
      (when (!= cause nil)
        (set! s (.ToString__String cause)))
      (panic (IllegalStateException_New_String_Throwable
               (Concat (Str "Can't overwrite cause with ") s) this))))
  (when (and (!= cause nil) (== (.Self_Throwable cause) t))
    (panic (IllegalArgumentException_New_String_Throwable (Str "Self-causation not permitted") this)))
  (set! (.-cause t) cause)
  (set! (.-causeSet t) true)
  this)

(go/method Impl_SetCause_Throwable__V [^{:tag (* Throwable)} t ^Throwable_I this ^Throwable_I cause]
  (set! (.-cause t) cause)
  (set! (.-causeSet t) true))

(go/method Impl_ToString__String ^{:tag (* String)} [^{:tag (* Throwable)} t ^Throwable_I this]
  (let [s (.GetName__String (.GetClass__Class this))
        m (.GetLocalizedMessage__String this)]
    (when (== m nil)
      (return s))
    (Concat s (Str ": ") m)))

(go/method Impl_FillInStackTrace__Throwable ^Throwable_I [^{:tag (* Throwable)} t ^Throwable_I this]
  (when (not (.-notWritable t))
    (set! (.-pcs t) (callers 2))
    (set! (.-stackTrace t) nil))
  this)

(go/method Impl_GetStackTrace__StackTraceElement1 ^{:tag (* RefArray)} [^{:tag (* Throwable)} t ^Throwable_I this]
  (.Copy (.ourStackTrace t)))

(go/method ourStackTrace ^{:tag (* RefArray)} [^{:tag (* Throwable)} t]
  (when (== (.-stackTrace t) nil)
    (set! (.-stackTrace t) (javaFrames (.-pcs t))))
  (.-stackTrace t))

(go/method Impl_SetStackTrace_StackTraceElement1__V
  [^{:tag (* Throwable)} t ^Throwable_I this ^{:tag (* RefArray)} trace]
  (let [c (.Copy (NN trace))]
    (range [i e (.-A c)]
      (when (== e nil)
        (panic (NullPointerException_New_String (Str (+ "stackTrace[" (strconv/Itoa i) "]"))))))
    (when (not (.-notWritable t))
      (set! (.-stackTrace t) c))))

(go/method Impl_PrintStackTrace__V [^{:tag (* Throwable)} t ^Throwable_I this]
  (StderrPrint (.String (StackTraceString this))))

(go/method Impl_AddSuppressed_Throwable__V [^{:tag (* Throwable)} t ^Throwable_I this ^Throwable_I e]
  (when (and (!= e nil) (== (.Self_Throwable e) t))
    (panic (IllegalArgumentException_New_String_Throwable (Str "Self-suppression not permitted") e)))
  (when (== e nil)
    (panic (NullPointerException_New_String (Str "Cannot suppress a null exception."))))
  (when (not (.-noSuppression t))
    (set! (.-suppressed t) (append (.-suppressed t) e))))

(go/method Impl_GetSuppressed__Throwable1 ^{:tag (* RefArray)} [^{:tag (* Throwable)} t ^Throwable_I this]
  (let [a (NewRefArray Throwable_class (conv int32 (len (.-suppressed t))))]
    (range [i e (.-suppressed t)]
      (aset (.-A a) i e))
    a))

;; Throwable's own dispatch methods, markers and generated members (§2, §5.4)
(go/method Self_Throwable ^{:tag (* Throwable)} [^{:tag (* Throwable)} t] t)
(go/method Is_Serializable [^{:tag (* Throwable)} t])
(go/method Ref ^any [^{:tag (* Throwable)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* Throwable)} t] Throwable_class)
(go/method Clone__O ^any [^{:tag (* Throwable)} t] (panic (CloneNotSupported t)))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* Throwable)} t] (.Impl_ToString__String t t))
(go/method GetMessage__String ^{:tag (* String)} [^{:tag (* Throwable)} t] (.Impl_GetMessage__String t t))
(go/method GetLocalizedMessage__String ^{:tag (* String)} [^{:tag (* Throwable)} t] (.Impl_GetLocalizedMessage__String t t))
(go/method GetCause__Throwable ^Throwable_I [^{:tag (* Throwable)} t] (.Impl_GetCause__Throwable t t))
(go/method InitCause_Throwable__Throwable ^Throwable_I [^{:tag (* Throwable)} t ^Throwable_I c] (.Impl_InitCause_Throwable__Throwable t t c))
(go/method SetCause_Throwable__V [^{:tag (* Throwable)} t ^Throwable_I c] (.Impl_SetCause_Throwable__V t t c))
(go/method FillInStackTrace__Throwable ^Throwable_I [^{:tag (* Throwable)} t] (.Impl_FillInStackTrace__Throwable t t))
(go/method GetStackTrace__StackTraceElement1 ^{:tag (* RefArray)} [^{:tag (* Throwable)} t] (.Impl_GetStackTrace__StackTraceElement1 t t))
(go/method SetStackTrace_StackTraceElement1__V [^{:tag (* Throwable)} t ^{:tag (* RefArray)} a] (.Impl_SetStackTrace_StackTraceElement1__V t t a))
(go/method PrintStackTrace__V [^{:tag (* Throwable)} t] (.Impl_PrintStackTrace__V t t))
(go/method AddSuppressed_Throwable__V [^{:tag (* Throwable)} t ^Throwable_I e] (.Impl_AddSuppressed_Throwable__V t t e))
(go/method GetSuppressed__Throwable1 ^{:tag (* RefArray)} [^{:tag (* Throwable)} t] (.Impl_GetSuppressed__Throwable1 t t))

(go/func Throwable_InstanceOf ^bool [^any x]
  (let [(values _ ok) (assert Throwable_I x)] ok))

(go/func Throwable_Cast ^Throwable_I [^any x]
  (when (== x nil)
    (return nil))
  (let [(values t ok) (assert Throwable_I x)]
    (when (not ok)
      (panic (ClassCast x Throwable_class)))
    t))

;; ---------------------------------------------------------------------------------------
;; Printing

(go/var ^{:doc "StderrPrint writes text to the standard error: printStackTrace() and uncaught
exceptions. Phase 2's System routes it through System.err.\n"}
  StderrPrint
  (fn [^string s] (.WriteString os/Stderr s)))

(go/func StackTraceString
  "StackTraceString is the text printStackTrace prints for t: its toString, its frames, its
suppressed exceptions and causes, as Java's (\"\\tat \", \"Caused by: \", \"... n more\",
\"[CIRCULAR REFERENCE: ...]\"), each line ended by \\n.\n"
  ^{:tag (* String)} [^Throwable_I t]
  (let [^{:tag (slice uint16)} b nil
        seen (make (map (* Throwable) bool))]
    (aset seen (.Self_Throwable t) true)
    (set! b (appendLine b (StrOfObj t)))
    (let [trace (.ourStackTrace (.Self_Throwable t))]
      (range [_ e (.-A trace)]
        (set! b (appendLine b (Concat (Str "\tat ") (StrOfObj e)))))
      (range [_ s (.-A (.GetSuppressed__Throwable1 t))]
        (set! b (printEnclosed b (assert Throwable_I s) trace "Suppressed: " "\t" seen)))
      (let [c (.GetCause__Throwable t)]
        (when (!= c nil)
          (set! b (printEnclosed b c trace "Caused by: " "" seen)))))
    (newString b)))

(go/func appendLine ^{:tag (slice uint16)} [^{:tag (slice uint16)} b ^{:tag (* String)} s]
  (append (append b (spread (.-value s))) \newline))

(go/func printEnclosed ^{:tag (slice uint16)}
  [^{:tag (slice uint16)} b ^Throwable_I t ^{:tag (* RefArray)} enclosing ^string caption
   ^string prefix ^{:tag (map (* Throwable) bool)} seen]
  (when (aget seen (.Self_Throwable t))
    (return (appendLine b (Concat (Str (+ prefix caption "[CIRCULAR REFERENCE: ")) (StrOfObj t) (Str "]")))))
  (aset seen (.Self_Throwable t) true)
  (let [trace (.ourStackTrace (.Self_Throwable t))
        m (- (len (.-A trace)) 1)
        n (- (len (.-A enclosing)) 1)]
    (while (and (>= m 0) (>= n 0) (Equals (aget (.-A trace) m) (aget (.-A enclosing) n)))
      (dec! m)
      (dec! n))
    (let [common (- (len (.-A trace)) 1 m)]
      (set! b (appendLine b (Concat (Str (+ prefix caption)) (StrOfObj t))))
      (for [i 0] (<= i m) (inc! i)
        (set! b (appendLine b (Concat (Str (+ prefix "\tat ")) (StrOfObj (aget (.-A trace) i))))))
      (when (!= common 0)
        (set! b (appendLine b (Str (+ prefix "\t... " (strconv/Itoa common) " more")))))
      (range [_ s (.-A (.GetSuppressed__Throwable1 t))]
        (set! b (printEnclosed b (assert Throwable_I s) trace "Suppressed: " (+ prefix "\t") seen)))
      (let [c (.GetCause__Throwable t)]
        (when (!= c nil)
          (set! b (printEnclosed b c trace "Caused by: " prefix seen))))
      b)))

(go/func Uncaught
  "Uncaught prints an exception that left a thread's run as the JVM's default handler does
(Exception in thread \"NAME\" ...) to the standard error.\n"
  [^string thread ^Throwable_I t]
  (StderrPrint (+ "Exception in thread \"" thread "\" " (.String (StackTraceString t)))))

;; ---------------------------------------------------------------------------------------
;; Throwing and catching (§7.9.1, §7.9.2)

(go/func Thrown
  "Thrown is the value of (throw e): e, or a new NullPointerException when e is null. c2g
writes (panic (jrt/Thrown e)).\n"
  ^Throwable_I [^Throwable_I e]
  (when (== e nil)
    (return (NPE)))
  e)

(go/func Catch
  "Catch is the deferred function of a try body, (defer (jrt/Catch (addr exc))): it recovers
a Java exception into *exc, converts a Go run-time error into its Java exception first
(§7.9.5), and panics again with anything else (a bug).\n"
  [^{:tag (* Throwable_I)} exc]
  (let [p (recover)]
    (when (== p nil)
      (return))
    (type-switch [v p]
      (case [Throwable_I] (set! @exc v))
      (case [runtime/Error]
        (let [e (FromRuntimeError v)]
          (when (== e nil)
            (panic p))
          (set! @exc e)))
      (default (panic p)))))

(go/func FromRuntimeError
  "FromRuntimeError is the Java exception of a Go run-time error, as Java's implicit checks
throw it (§7.9.5): a nil dereference is NullPointerException, an index out of range
ArrayIndexOutOfBoundsException (with Java's message; the length is read from the run-time's
error value, which Go's message omits for negative indexes), a division by zero
ArithmeticException(\"/ by zero\"), a failed type assertion ClassCastException, a slice
range IndexOutOfBoundsException, an impossible allocation OutOfMemoryError; nil for any other.\n"
  ^Throwable_I [^{:tag runtime/Error} e]
  (let [msg (.Error e)]
    (when (strings/HasPrefix msg "runtime error: ")
      (set! msg (subslice msg (len "runtime error: "))))
    (when (or (strings/Contains msg "nil pointer dereference")
              (strings/Contains msg "invalid memory address"))
      (return (NPE)))
    (let [(values _ isNil) (assert (* runtime/PanicNilError) e)]
      (when isNil
        (return (NPE))))
    (when (== msg "integer divide by zero")
      (return (ArithmeticException_New_String (Str "/ by zero"))))
    (let [(values _ isTA) (assert (* runtime/TypeAssertionError) e)]
      (when isTA
        (return (ClassCastException_New_String (Str msg)))))
    (let [v (reflect/ValueOf e)]
      (when (and (== (.Kind v) reflect/Struct) (== (.Name (.Type v)) "boundsError"))
        (let [x (.Int (.FieldByName v "x"))
              y (.Int (.FieldByName v "y"))
              code (.Uint (.FieldByName v "code"))]
          (if (== code 0)
            (return (IndexOutOfBounds (conv int x) (conv int y)))
            (return (IndexOutOfBoundsException_New_String (Str msg)))))))
    (when (strings/Contains msg "out of range")
      (return (IndexOutOfBoundsException_New_String (Str msg))))
    (when (strings/HasPrefix msg "makeslice")
      (return (OutOfMemoryError_New_String (Str "Java heap space"))))
    nil))

;; ---------------------------------------------------------------------------------------
;; Stack traces (§7.9.6)

(go/type FrameInfo
  "FrameInfo maps one Go function to its Java frame: c2g's c2g_frames.go registers one per
generated function (RegisterFrames). Go is the function's name as runtime.Frame.Function
gives it (\"arbace/lang.(*PersistentVector).ArrayFor_I__O1\", \"arbace/lang.Util_clinit\",
\"arbace/lang.(*Delay).Realize__V.func1\"); Class the binary name; Method Java's name
(\"<init>\", \"<clinit>\", \"lambda$m$0\"); Flags FrameElide for frames Java does not show
(forwarders, allocation functions, adapters).\n"
  (struct ^string Go ^string Class ^string Method ^uint8 Flags))

(go/const
  [^{:tag uint8 :val 1 :doc "FrameElide: the frame is not shown.\n"} FrameElide 1])

(go/var ^{:tag sync/RWMutex} framesMu)
(go/var ^{:tag (map string FrameInfo)} frameTable (make (map string FrameInfo)))
(go/var ^{:tag (map string (* Class))} classByGo)

(go/func RegisterFrames
  "RegisterFrames adds Go functions' Java frames to the table stack traces read.\n"
  [^{:tag (slice FrameInfo)} fs]
  (.Lock framesMu)
  (range [_ f fs]
    (aset frameTable (.-Go f) f))
  (.Unlock framesMu))

(go/func lookupFrame [^string goName] :results [^FrameInfo f ^bool ok]
  (.RLock framesMu)
  (set! (values f ok) (aget frameTable goName))
  (.RUnlock framesMu)
  (return))

(go/func classOfGo
  "classOfGo: the class whose ClassInfo.Go is name (\"arbace/jrt.String\"), or nil.\n"
  ^{:tag (* Class)} [^string name]
  (.Lock framesMu)
  (when (== classByGo nil)
    (set! classByGo (make (map string (* Class))))
    (.RLock registryMu)
    (range [_ c registry]
      (when (!= (.-Go (.-info c)) "")
        (aset classByGo (.-Go (.-info c)) c)))
    (.RUnlock registryMu))
  (let [c (aget classByGo name)]
    (.Unlock framesMu)
    c))

(go/func callers
  "callers: the program counters of the calling goroutine's stack, skip frames left out
(1024 at most, MaxJavaStackTraceDepth's default).\n"
  ^{:tag (slice uintptr)} [^int skip]
  (let [pcs (make (slice uintptr) 64)
        n (runtime/Callers (+ skip 1) pcs)]
    (when (== n (len pcs))
      (set! pcs (make (slice uintptr) 1024))
      (set! n (runtime/Callers (+ skip 1) pcs)))
    (subslice pcs _ n)))

(go/type jframe (struct ^string goName ^string cls ^string method ^string file ^int line ^bool elide))

(go/func demangle
  "demangle: the Java frame of a Go function that c2g's table does not list. jrt's own
methods and package functions follow §4.4's names (Type, Type_Method..., (*Type).Method_..._R,
Impl_, Ctor..., _New..., _clinit); other Go functions show as their package and name;
runtime and testing frames and jrt's helpers are elided.\n"
  ^jframe [^string fn]
  (let [f (lit jframe :goName fn)
        slash (strings/LastIndexByte fn \/)
        dot (strings/IndexByte (subslice fn (+ slash 1)) \.)]
    (when (< dot 0)
      (set! (.-elide f) true)
      (return f))
    (let [pkg (subslice fn _ (+ slash 1 dot))
          rest (subslice fn (+ slash 2 dot))
          typ ""]
      (when (or (== pkg "runtime") (strings/HasPrefix pkg "runtime/") (== pkg "testing")
                (strings/HasPrefix pkg "internal/") (== pkg "reflect"))
        (set! (.-elide f) true)
        (return f))
      ;; a function literal (try body): its enclosing function's frame
      (let [lit (strings/Index rest ".func")]
        (when (>= lit 0)
          (set! rest (subslice rest _ lit))))
      (cond
        (strings/HasPrefix rest "(*")
        (let [e (strings/Index rest ").")]
          (set! typ (subslice rest 2 e))
          (set! rest (subslice rest (+ e 2))))
        (strings/HasPrefix rest "(")
        (let [e (strings/Index rest ").")]
          (set! typ (subslice rest 1 e))
          (set! rest (subslice rest (+ e 2)))))
      (let [^{:tag (* Class)} c nil
            member rest]
        (if (!= typ "")
          (set! c (classOfGo (+ pkg "." typ)))
          ;; a package function T_m...: the longest prefix naming a class
          (for [i (len rest)] (> i 0) (dec! i)
            (when (== (aget rest (- i 1)) \_)
              (let [k (classOfGo (+ pkg "." (subslice rest _ (- i 1))))]
                (when (!= k nil)
                  (set! c k)
                  (set! member (subslice rest i))
                  (break))))))
        (when (== c nil)
          (if (== pkg "arbace/jrt")
            (set! (.-elide f) true)
            (do
              (set! (.-cls f) (strings/ReplaceAll pkg "/" "."))
              (when (!= typ "")
                (set! (.-cls f) (+ (.-cls f) "." typ)))
              (set! (.-method f) rest)))
          (return f))
        (set! (.-cls f) (.GoName c))
        (cond
          (strings/HasPrefix member "Impl_") (set! member (subslice member 5))
          (strings/HasPrefix member "New") (do (set! (.-method f) "<init>") (set! (.-elide f) true) (return f))
          (strings/HasPrefix member "Ctor") (do (set! (.-method f) "<init>") (return f))
          (== member "clinit") (do (set! (.-method f) "<clinit>") (return f)))
        (let [u (strings/Index member "__")]
          (when (< u 0)
            (set! (.-elide f) true)
            (return f))
          (let [m (subslice member _ u)
                e (strings/IndexByte m \_)]
            (when (>= e 0)
              (set! m (subslice m _ e)))
            (set! (.-method f) (+ (strings/ToLower (subslice m _ 1)) (subslice m 1)))
            f))))))

(go/func frameOf ^jframe [^{:tag runtime/Frame} fr]
  (let [(values info ok) (lookupFrame (.-Function fr))
        f (lit jframe)]
    ;; a function literal c2g did not register (a try body): its function's frame
    (when (not ok)
      (let [i (strings/Index (.-Function fr) ".func")]
        (when (>= i 0)
          (set! (values info ok) (lookupFrame (subslice (.-Function fr) _ i))))))
    (if ok
      (set! f (lit jframe :goName (.-Function fr) :cls (.-Class info) :method (.-Method info)
                   :elide (!= (bit-and (.-Flags info) FrameElide) 0)))
      (set! f (demangle (.-Function fr))))
    (set! (.-file f) (.-File fr))
    (when (>= (strings/LastIndexByte (.-file f) \/) 0)
      (set! (.-file f) (subslice (.-file f) (+ (strings/LastIndexByte (.-file f) \/) 1))))
    (set! (.-line f) (.-Line fr))
    f))

(go/func isForwarder
  "isForwarder: whether fn (pkg.(*T).M) called callee as its implementation (pkg.(*U).Impl_M),
a dispatch method that forwards (§5.4).\n"
  ^bool [^string fn ^string callee]
  (let [i (strings/LastIndexByte fn \.)
        j (strings/LastIndexByte callee \.)]
    (and (>= i 0) (>= j 0) (== (subslice callee (+ j 1)) (+ "Impl_" (subslice fn (+ i 1)))))))

(go/func isThrowableClass ^bool [^string name]
  (let [c (ForName name)]
    (and (!= c nil) (.assignableFrom Throwable_class c))))

(go/func javaFrames
  "javaFrames maps captured program counters to Java's StackTraceElement[]: the frames up to
the last runtime.gopanic dropped (an exception made while a run-time error panics), the
frames of fillInStackTrace and of the exception's constructors dropped at the top, as
HotSpot drops them, elided frames left out, a function literal's frame merged with the frame
of the function that called it.\n"
  ^{:tag (* RefArray)} [^{:tag (slice uintptr)} pcs]
  (let [^{:tag (slice runtime/Frame)} raw nil
        frames (runtime/CallersFrames pcs)]
    (while true
      (let [(values fr more) (.Next frames)]
        (set! raw (append raw fr))
        (when (not more)
          (break))))
    (range [i fr raw]
      (when (== (.-Function fr) "runtime.gopanic")
        (set! raw (subslice raw (+ i 1)))))
    (let [^{:tag (slice jframe)} fs nil
          top true]
      (range [i fr raw]
        (let [f (frameOf fr)]
          (cond
            (.-elide f) (do)
            ;; a forwarder: its callee is the implementation Impl_M of its method M
            (and (> i 0) (isForwarder (.-Function fr) (.-Function (aget raw (- i 1)))))
            (do)
            ;; the caller of a try body's function literal: the literal's frame stands for it
            (and (> i 0) (strings/HasPrefix (.-Function (aget raw (- i 1))) (+ (.-Function fr) ".func")))
            (do)
            (and top (or (== (.-method f) "fillInStackTrace")
                         (and (== (.-method f) "<init>") (isThrowableClass (.-cls f)))))
            (do)
            :else
            (do
              (set! top false)
              (set! fs (append fs f))))))
      (let [a (NewRefArray StackTraceElement_class (conv int32 (len fs)))]
        (range [i f fs]
          (aset (.-A a) i (StackTraceElement_New_String_String_String_I
                            (Str (.-cls f)) (Str (.-method f)) (Str (.-file f)) (conv int32 (.-line f)))))
        a))))

;; ---------------------------------------------------------------------------------------
;; StackTraceElement (shim, final)

(go/type StackTraceElement "StackTraceElement is java.lang.StackTraceElement.\n"
  (struct Object
          ^{:tag (* String)} declaringClass
          ^{:tag (* String)} methodName
          ^{:tag (* String)} fileName
          ^int32 lineNumber))

(go/var StackTraceElement_class
  (Define (addr (lit ClassInfo :Name "java.lang.StackTraceElement" :Kind KindClass
                     :Modifiers (bit-or AccPublic AccFinal) :Super Object_class
                     :Interfaces (lit (slice (* Class)) Serializable_class)
                     :Go "arbace/jrt.StackTraceElement"))))

(go/func StackTraceElement_New_String_String_String_I
  "StackTraceElement_New_String_String_String_I is new StackTraceElement(declaringClass,
methodName, fileName, lineNumber).\n"
  ^{:tag (* StackTraceElement)}
  [^{:tag (* String)} cls ^{:tag (* String)} method ^{:tag (* String)} file ^int32 line]
  (when (== cls nil)
    (panic (NullPointerException_New_String (Str "Declaring class is null"))))
  (when (== method nil)
    (panic (NullPointerException_New_String (Str "Method name is null"))))
  (addr (lit StackTraceElement :declaringClass cls :methodName method :fileName file :lineNumber line)))

(go/method Ref ^any [^{:tag (* StackTraceElement)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* StackTraceElement)} t] StackTraceElement_class)
(go/method Clone__O ^any [^{:tag (* StackTraceElement)} t] (panic (CloneNotSupported t)))
(go/method Is_Serializable [^{:tag (* StackTraceElement)} t])
(go/method GetClassName__String ^{:tag (* String)} [^{:tag (* StackTraceElement)} t] (.-declaringClass t))
(go/method GetMethodName__String ^{:tag (* String)} [^{:tag (* StackTraceElement)} t] (.-methodName t))
(go/method GetFileName__String ^{:tag (* String)} [^{:tag (* StackTraceElement)} t] (.-fileName t))
(go/method GetLineNumber__I ^int32 [^{:tag (* StackTraceElement)} t] (.-lineNumber t))
(go/method IsNativeMethod__Z ^bool [^{:tag (* StackTraceElement)} t] (== (.-lineNumber t) -2))

(go/method ToString__String
  "ToString__String is Java's format: [java.base/]class.method(file:line), with
(Native Method) and (Unknown Source); JDK classes (java., javax., jdk., sun.) show the module
java.base as the JVM's do.\n"
  ^{:tag (* String)} [^{:tag (* StackTraceElement)} t]
  (let [cls (.String (.-declaringClass t))
        s ""]
    (when (or (strings/HasPrefix cls "java.") (strings/HasPrefix cls "javax.")
              (strings/HasPrefix cls "jdk.") (strings/HasPrefix cls "sun."))
      (set! s "java.base/"))
    (set! s (+ s cls "." (.String (.-methodName t)) "("))
    (cond
      (== (.-lineNumber t) -2) (set! s (+ s "Native Method"))
      (== (.-fileName t) nil) (set! s (+ s "Unknown Source"))
      (>= (.-lineNumber t) 0) (set! s (+ s (.String (.-fileName t)) ":" (strconv/Itoa (conv int (.-lineNumber t)))))
      :else (set! s (+ s (.String (.-fileName t)))))
    (Str (+ s ")"))))

(go/method Equals_O__Z ^bool [^{:tag (* StackTraceElement)} t ^any o]
  (let [(values e ok) (assert (* StackTraceElement) o)]
    (when (or (not ok) (== e nil))
      (return false))
    (and (.Equals_O__Z (.-declaringClass t) (.-declaringClass e))
         (== (.-lineNumber t) (.-lineNumber e))
         (stringsEqual (.-methodName t) (.-methodName e))
         (stringsEqual (.-fileName t) (.-fileName e)))))

(go/func stringsEqual ^bool [^{:tag (* String)} a ^{:tag (* String)} b]
  (when (== a nil)
    (return (== b nil)))
  (.Equals_O__Z a (.Ref b)))

(go/method HashCode__I ^int32 [^{:tag (* StackTraceElement)} t]
  ;; Java's, with no class loader, module or version names
  (let [r (+ (* 31 (.HashCode__I (.-declaringClass t))) (.HashCode__I (.-methodName t)))
        ^int32 fh 0]
    (when (!= (.-fileName t) nil)
      (set! fh (.HashCode__I (.-fileName t))))
    (set! r (+ (* 31 31 31 31 r) fh))
    (+ (* 31 r) (.-lineNumber t))))
