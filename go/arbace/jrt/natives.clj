;; jrt: the native methods of the translated JDK classes (C2G-SPEC §9.1): c2g translates a
;; ^:native method into a call of C_M..._native. Double's and Float's are numconv.clj's; the
;; fifth is here (added with c2g, doc/go/C2G-NOTES.md). And the natives of arbace.lang's
;; Go-build variants (arbace/lang/go/*.clj; C2G-NOTES.md, phase 2A): Reflector's adaptation of
;; functions.
(in-ns 'go.arbace.jrt)

(go/file "natives.go")

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
