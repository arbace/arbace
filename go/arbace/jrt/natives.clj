;; jrt: the native methods of the translated JDK classes (C2G-SPEC §9.1): c2g translates a
;; ^:native method into a call of C_M..._native. Double's and Float's are numconv.clj's; the
;; fifth is here (added with c2g, doc/go/C2G-NOTES.md). And the natives of arbace.lang's
;; Go-build variants (arbace/lang/go/*.clj; C2G-NOTES.md, phase 2A): Reflector's adaptation of
;; functions, RT's writers and reader over the host's standard streams.
(in-ns 'go.arbace.jrt)

(go/file "natives.go"
  :imports [[bufio "bufio"] [io "io"] [sync "sync"] [utf16 "unicode/utf16"] [utf8 "unicode/utf8"]])

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
;; arbace.lang.RT$HostWriter and RT$HostReader (variant): *out*, *err* and *in*

(go/type hostOut
  "hostOut is the encoder and buffer of a standard output stream: UTF-16 in, UTF-8 out, as
an OutputStreamWriter's StreamEncoder (8 KiB), a high surrogate kept until its pair comes.\n"
  (struct ^{:tag sync/Mutex} mu ^{:tag (* bufio/Writer)} w ^uint16 high ^bool hasHigh))

(go/var ^{:tag (array 3 (* hostOut))} hostOuts
  (lit (array 3 (* hostOut)) nil
       (addr (lit hostOut :w (bufio/NewWriterSize Stdout 8192)))
       (addr (lit hostOut :w (bufio/NewWriterSize Stderr 8192)))))

(go/func hostIOError "hostIOError throws err as an IOException.\n" [^error err]
  (panic (Thrown (IOException_New_String (Str (.Error err))))))

(go/func RT_HostWriter_HostWrite_I_C1_I_I__V_native
  "RT_HostWriter_HostWrite_I_C1_I_I__V_native writes cbuf[off, off+n) to the host's stream
fd (1 or 2), encoded as UTF-8; a lone surrogate is written as ?, as Java's encoder replaces
it.\n"
  [^int32 fd ^{:tag (* CharArray)} cbuf ^int32 off ^int32 n]
  (let [o (aget hostOuts fd)
        units (.-A cbuf)]
    (.Lock (.-mu o))
    (defer (.Unlock (.-mu o)))
    (let [put (fn [^rune r]
                (let [(values _ err) (.WriteRune (.-w o) r)]
                  (when (!= err nil) (hostIOError err))))]
      (for [i off] (< i (+ off n)) (inc! i)
        (let [u (aget units i)]
          (when (.-hasHigh o)
            (set! (.-hasHigh o) false)
            (when (and (>= u 0xDC00) (<= u 0xDFFF))
              (put (utf16/DecodeRune (conv rune (.-high o)) (conv rune u)))
              (continue))
            (put \?))
          (cond
            (and (>= u 0xD800) (<= u 0xDBFF)) (do (set! (.-high o) u) (set! (.-hasHigh o) true))
            (and (>= u 0xDC00) (<= u 0xDFFF)) (put \?)
            :else (put (conv rune u))))))))

(go/func RT_HostWriter_HostFlush_I__V_native
  "RT_HostWriter_HostFlush_I__V_native writes what the stream fd buffers to the host.\n"
  [^int32 fd]
  (let [o (aget hostOuts fd)]
    (.Lock (.-mu o))
    (defer (.Unlock (.-mu o)))
    (let [err (.Flush (.-w o))]
      (when (!= err nil) (hostIOError err)))))

(go/var ^{:tag sync/Mutex} hostInMu)
(go/var ^{:tag (* bufio/Reader)} hostIn (bufio/NewReaderSize Stdin 8192))
(go/var ^uint16 hostInLow)
(go/var ^bool hostInHasLow)

(go/func RT_HostReader_HostRead_C1_I_I__I_native
  "RT_HostReader_HostRead_C1_I_I__I_native reads at most n (> 0) chars of the host's standard
input into cbuf from off: UTF-8 decoded (a malformed byte as U+FFFD), blocking until one char
is there, then taking only what is buffered; -1 at the end. A supplementary character is two
chars; its second waits for the next read when only one fits.\n"
  ^int32 [^{:tag (* CharArray)} cbuf ^int32 off ^int32 n]
  (.Lock hostInMu)
  (defer (.Unlock hostInMu))
  (let [units (.-A cbuf)
        ^int32 k 0]
    (when hostInHasLow
      (aset units off hostInLow)
      (set! hostInHasLow false)
      (inc! k))
    (for [] (< k n) _
      (when (> k 0)
        ;; after the first char only what is buffered, whole characters
        (let [(values b _) (.Peek hostIn (.Buffered hostIn))]
          (when (or (== (len b) 0) (not (utf8/FullRune b)))
            (break))))
      (let [(values r _ err) (.ReadRune hostIn)]
        (when (!= err nil)
          (when (== err io/EOF)
            (break))
          (when (> k 0) (break))
          (hostIOError err))
        (if (> r 0xFFFF)
          (let [(values hi lo) (utf16/EncodeRune r)]
            (aset units (+ off k) (conv uint16 hi))
            (inc! k)
            (if (< k n)
              (do (aset units (+ off k) (conv uint16 lo)) (inc! k))
              (do (set! hostInLow (conv uint16 lo)) (set! hostInHasLow true))))
          (do (aset units (+ off k) (conv uint16 r)) (inc! k)))))
    (when (== k 0) (return -1))
    k))
