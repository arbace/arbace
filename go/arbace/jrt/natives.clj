;; jrt: the native methods of the translated JDK classes (C2G-SPEC §9.1): c2g translates a
;; ^:native method into a call of C_M..._native. Double's and Float's are numconv.clj's; the
;; fifth is here (added with c2g, doc/go/C2G-NOTES.md). And the natives of arbace.lang's
;; Go-build variants (arbace/lang/go/*.clj; C2G-NOTES.md, phase 2A): Reflector's adaptation of
;; functions.
(in-ns 'go.arbace.jrt)

(go/file "natives.go"
  :imports [[bytes "bytes"] [zlib "compress/zlib"] [md5 "crypto/md5"] [errors "errors"] [io "io"]])

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

;; ---------------------------------------------------------------------------------------
;; java.util.zip.InflaterInputStream (jrt's own Java, overlay/jdk; JRT-NOTES.md, "The JDK's
;; resource data")

(go/func InflaterInputStream_Inflate0_B1_String1__B1_native
  "InflaterInputStream_Inflate0_B1_String1__B1_native is the data of the zlib stream input
(RFC 1950, as the JDK's default Inflater reads it), over Go's compress/zlib; or null with the
reason in error[0]: zlib's \"Unexpected end of ZLIB input stream\" (the JDK's message for a
truncated stream), else Go's description of the malformed data.\n"
  ^{:tag (* ByteArray)} [^{:tag (* ByteArray)} input ^{:tag (* RefArray)} error]
  (let [in (make (slice byte) (len (.-A (NN input))))]
    (range [i x (.-A input)]
      (aset in i (conv byte x)))
    (let [(values r err) (zlib/NewReader (bytes/NewReader in))
          ^{:tag (slice byte)} out nil]
      (when (== err nil)
        (set! (values out err) (io/ReadAll r)))
      (when (!= err nil)
        (if (or (errors/Is err io/ErrUnexpectedEOF) (errors/Is err io/EOF))
          (aset (.-A error) 0 (Str "Unexpected end of ZLIB input stream"))
          (aset (.-A error) 0 (Str (.Error err))))
        (return nil))
      (let [a (NewByteArray (conv int32 (len out)))]
        (range [i x out]
          (aset (.-A a) i (conv int8 x)))
        a))))
