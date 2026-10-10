;; jrt: the native methods of the translated JDK classes (C2G-SPEC §9.1): c2g translates a
;; ^:native method into a call of C_M..._native. Double's and Float's are numconv.clj's; the
;; fifth is here (added with c2g, doc/go/C2G-NOTES.md). And the natives of arbace.lang's
;; Go-build variants (arbace/lang/go/*.clj; C2G-NOTES.md, phase 2A): Reflector's adaptation of
;; functions.
(in-ns 'go.arbace.jrt)

(go/file "natives.go"
  :imports [[md5 "crypto/md5"]])

(go/func NullPointerException_GetExtendedNPEMessage__String_native
  "NullPointerException_GetExtendedNPEMessage__String_native is the native
NullPointerException.getExtendedNPEMessage: null, so an implicit NullPointerException has no
message (V11, amendment J6).\n"
  ^{:tag (* String)} [^{:tag (* NullPointerException)} t]
  nil)

;; ---------------------------------------------------------------------------------------
;; arbace.lang.Reflector (variant)

(go/func Reflector_AdaptFn_Class_O__O_native
  "Reflector_AdaptFn_Class_O__O_native is the Go build's Reflector.adaptFn: f, a Clojure
function, as an instance of the functional interface c (AdaptFn, through c's ClassInfo.FromFn),
where the JVM's Reflector.boxArg makes a Proxy; UnsupportedOperationException when c has no
adapter.\n"
  ^any [^{:tag (* Class)} c ^any f]
  (let [a (AdaptFn c f)]
    (when (== a nil)
      (panic (Thrown (UnsupportedOperationException_New_String
                       (Str (+ "no functional interface adapter for " (.-Name (.Info c))))))))
    a))

;; ---------------------------------------------------------------------------------------
;; arbace.lang.Compiler$Evaluator (variant; doc/go/EVAL-NOTES.md)

(go/func Compiler_Evaluator_SetTraceHook_Supplier__V_native
  "Compiler_Evaluator_SetTraceHook_Supplier__V_native sets EvalTrace, the evaluated frames of
stack traces (C2G-SPEC §10.7).\n"
  [^Supplier s]
  (set! EvalTrace s))

(go/func Compiler_Evaluator_MonitorEnter_O__V_native
  "Compiler_Evaluator_MonitorEnter_O__V_native is the evaluator's monitor-enter.\n"
  [^any o]
  (MonitorEnter o))

(go/func Compiler_Evaluator_MonitorExit_O__V_native
  "Compiler_Evaluator_MonitorExit_O__V_native is the evaluator's monitor-exit.\n"
  [^any o]
  (MonitorExit o))

;; ---------------------------------------------------------------------------------------
;; arbace.lang.Compiler$CodeRun (variant, CompilerCode.clj; doc/go/SPEED-NOTES.md): the closure
;; compiler's calls of resolved constructors, through their member table's invoker as compiled
;; code calls them: the arguments already of their parameters' types (boxed), no Reflector, no
;; InvocationTargetException (resolved methods: Evaluator.invokeDirect, below)

(go/func directArgs
  "directArgs: the boxed arguments args for the parameters ps in the tables' convention
(Unbox: a primitive's wrapper to its Go value).\n"
  ^{:tag (slice any)} [^{:tag (slice (* Class))} ps ^{:tag (* RefArray)} args]
  (let [out (make (slice any) (len ps))]
    (range [i p ps]
      (let [x (aget (.-A args) i)]
        (if (.IsPrimitive__Z p)
          (let [(values v ok) (Unbox p x)]
            (when (not ok)
              (panic (NPE)))
            (aset out i v))
          (aset out i x))))
    out))

(go/func Compiler_CodeRun_Construct_Constructor_O1__O_native
  "Compiler_CodeRun_Construct_Constructor_O1__O_native makes an object with constructor k and
args, the class initialized first; InstantiationException for an abstract class or an
interface.\n"
  ^any [^{:tag (* Constructor)} k ^{:tag (* RefArray)} args]
  (let [c (.-clazz k)]
    (when (or (!= (bit-and (.GetModifiers__I c) (bit-or AccAbstract AccInterface)) 0) (== (.-New (.-info k)) nil))
      (panic (InstantiationException_New)))
    (when (!= (.-Init (.-info c)) nil)
      ((.-Init (.-info c))))
    ((.-New (.-info k)) (directArgs (.-params k) args))))

;; ---------------------------------------------------------------------------------------
;; arbace.lang.RT (variant; doc/go/EVAL-NOTES.md): the program's embedded sources

(go/func RT_HostResource_String__B1_native
  "RT_HostResource_String__B1_native is the Go build's RT.hostResource: the bytes of the
embedded resource name (the host's Resource: RT.load's sources), or null.\n"
  ^{:tag (* ByteArray)} [^{:tag (* String)} name]
  (let [(values b ok) (.Resource (CurrentHost) (.String (NN name)))]
    (when (not ok)
      (return nil))
    (let [a (NewByteArray (conv int32 (len b)))]
      (range [i x b]
        (aset (.-A a) i (conv int8 x)))
      a)))

;; ---------------------------------------------------------------------------------------
;; java.util.UUID (variant, overlay/jdk/variants/UUID.clj)

(go/func UUID_HostRandomBytes_B1__V_native
  "UUID_HostRandomBytes_B1__V_native fills b from the host's random source (randomUUID).\n"
  [^{:tag (* ByteArray)} b]
  (let [bs (make (slice byte) (len (.-A b)))]
    (.RandomBytes (CurrentHost) bs)
    (range [i x bs]
      (aset (.-A b) i (conv int8 x)))))

(go/func UUID_Md5_B1__B1_native
  "UUID_Md5_B1__B1_native is MessageDigest.getInstance(\"MD5\").digest(b) (nameUUIDFromBytes),
over Go's crypto/md5.\n"
  ^{:tag (* ByteArray)} [^{:tag (* ByteArray)} b]
  (let [bs (make (slice byte) (len (.-A (NN b))))]
    (range [i x (.-A b)]
      (aset bs i (conv byte x)))
    (let [sum (md5/Sum bs)
          a (NewByteArray (conv int32 (len sum)))]
      (range [i x sum]
        (aset (.-A a) i (conv int8 x)))
      a)))

(go/func Compiler_Evaluator_CheckCast_Class_O__O_native
  "Compiler_Evaluator_CheckCast_Class_O__O_native is the evaluator's checkcast of o to c (null
passes), with the JVM's ClassCastException message.\n"
  ^any [^{:tag (* Class)} c ^any o]
  (when (and (!= o nil) (not (.IsInstance_O__Z c o)))
    (panic (ClassCast o c)))
  o)

(go/func Compiler_Evaluator_InvokeDirect_Method_O_O1__O_native
  "Compiler_Evaluator_InvokeDirect_Method_O_O1__O_native is the evaluator's call of a resolved
method (a hinted interop call; EVAL-NOTES.md, \"The suite's last failures\"), as compiled code calls it: the
arguments converted (convertArgs), the method called through its invoker, an exception it throws
propagating as it is (Method.invoke wraps it in InvocationTargetException, which Reflector then
unwraps), a primitive result boxed. A null receiver of an instance method is
NullPointerException.\n"
  ^any [^{:tag (* Method)} m ^any obj ^{:tag (* RefArray)} args]
  (let [info (.-info m)]
    (when (and (== (bit-and (.-mods m) AccStatic) 0) (== obj nil))
      (panic (NPE)))
    (let [cargs (convertArgs (.-params m) args)]
      (when (== (.-Invoke info) nil)
        (panic (AbstractMethodError_New_String (Str (+ (.GoName (.-clazz m)) "." (.-Name info))))))
      (Box ((.-Invoke info) obj cargs)))))
