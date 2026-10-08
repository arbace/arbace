;; jrt: java.lang.System and java.lang.Runtime over the host (C2G-SPEC §9.4; doc/go/
;; JRT-NOTES.md, phase 2a): the clocks, system properties, the environment, exit, the
;; standard error behind printStackTrace. System.arraycopy and identityHashCode are in
;; array.go; System.in, out and err (PrintStream, InputStream) come with those classes, over
;; the host streams Stdin, Stdout, Stderr (host.go).
(in-ns 'go.arbace.jrt)

(go/file "system.go"
  :imports [[runtime "runtime"] [debug "runtime/debug"] [sort "sort"] [strings "strings"]
            [sync "sync"]])

;; ---------------------------------------------------------------------------------------
;; The clocks and the environment

(go/func System_NanoTime__J "System_NanoTime__J is System.nanoTime: the host's monotonic clock.\n"
  ^int64 []
  (.Nanotime (CurrentHost)))

(go/func System_CurrentTimeMillis__J "System_CurrentTimeMillis__J is System.currentTimeMillis.\n"
  ^int64 []
  (.UnixMilli (.Now (CurrentHost))))

(go/func System_Getenv_String__String
  "System_Getenv_String__String is System.getenv(String): null when the variable is not set.\n"
  ^{:tag (* String)} [^{:tag (* String)} name]
  (let [(values v ok) (.Getenv (CurrentHost) (.String (NN name)))]
    (when (not ok)
      (return nil))
    (Str v)))

(go/func System_Exit_I__V
  "System_Exit_I__V is System.exit (and Runtime.exit): the host ends the process; the
shutdown hooks registered with jrt run first.\n"
  [^int32 status]
  (runShutdownHooks)
  (.Exit (CurrentHost) (conv int status)))

(go/func System_Gc__V "System_Gc__V is System.gc: runtime.GC.\n" []
  (runtime/GC))

(go/func System_LineSeparator__String "System_LineSeparator__String is System.lineSeparator: \\n.\n"
  ^{:tag (* String)} []
  lineSeparator)

(go/var lineSeparator (Intern "\n"))

;; ---------------------------------------------------------------------------------------
;; System properties: initialized from the host on first use, as the JVM's (the values a
;; Java program on Linux reads, where they mean something here)

(go/var ^{:tag sync/Mutex} propMu)
(go/var ^{:tag (map string (* String))} props)

(go/func osArch ^string []
  (switch runtime/GOARCH
    (case ["arm64"] (return "aarch64"))
    (default (return runtime/GOARCH))))

(go/func initProps "initProps fills the properties (propMu held).\n" []
  (when (!= props nil)
    (return))
  (set! props (make (map string (* String))))
  (let [h (CurrentHost)
        set (fn [^string k ^string v] (aset props k (Str v)))
        env (fn ^string [^string k ^string dflt]
              (let [(values v ok) (.Getenv h k)]
                (when (and ok (!= v ""))
                  (return v))
                dflt))]
    (set "java.version" "26")
    (set "java.specification.version" "26")
    (set "java.vm.specification.version" "26")
    (set "java.class.version" "70.0")
    (set "java.vendor" "Arbace")
    (set "java.vm.name" "jrt")
    (set "java.vm.vendor" "Arbace")
    (set "java.runtime.name" "Arbace on Go")
    (set "java.home" "")
    (set "java.class.path" "")
    (set "java.library.path" "")
    (set "os.name" (+ (strings/ToUpper (subslice runtime/GOOS 0 1)) (subslice runtime/GOOS 1)))
    (set "os.arch" (osArch))
    (set "os.version" "")
    (set "file.separator" "/")
    (set "path.separator" ":")
    (set "line.separator" "\n")
    (set "file.encoding" "UTF-8")
    (set "native.encoding" "UTF-8")
    (set "sun.jnu.encoding" "UTF-8")
    (set "stdout.encoding" "UTF-8")
    (set "stderr.encoding" "UTF-8")
    (set "java.io.tmpdir" (env "TMPDIR" "/tmp"))
    (set "user.home" (env "HOME" "?"))
    (set "user.name" (env "USER" "?"))
    (let [(values wd err) (.Getwd h)]
      (if (== err nil)
        (set "user.dir" wd)
        (set "user.dir" "/")))
    (set "go.version" (runtime/Version))))

(go/func checkKey ^string [^{:tag (* String)} key]
  (when (== key nil)
    (panic (NullPointerException_New_String (Str "key can't be null"))))
  (when (== (.Length__I key) 0)
    (panic (IllegalArgumentException_New_String (Str "key can't be empty"))))
  (.String key))

(go/func System_GetProperty_String__String "System_GetProperty_String__String is System.getProperty(String).\n"
  ^{:tag (* String)} [^{:tag (* String)} key]
  (let [k (checkKey key)]
    (.Lock propMu)
    (initProps)
    (let [v (aget props k)]
      (.Unlock propMu)
      v)))

(go/func System_GetProperty_String_String__String
  "System_GetProperty_String_String__String is System.getProperty(String, String).\n"
  ^{:tag (* String)} [^{:tag (* String)} key ^{:tag (* String)} def]
  (let [v (System_GetProperty_String__String key)]
    (when (== v nil)
      (return def))
    v))

(go/func System_SetProperty_String_String__String
  "System_SetProperty_String_String__String is System.setProperty: the previous value.\n"
  ^{:tag (* String)} [^{:tag (* String)} key ^{:tag (* String)} value]
  (let [k (checkKey key)]
    (when (== value nil)
      (panic (NPE)))
    (.Lock propMu)
    (initProps)
    (let [old (aget props k)]
      (aset props k value)
      (.Unlock propMu)
      old)))

(go/func System_ClearProperty_String__String
  "System_ClearProperty_String__String is System.clearProperty: the previous value.\n"
  ^{:tag (* String)} [^{:tag (* String)} key]
  (let [k (checkKey key)]
    (.Lock propMu)
    (initProps)
    (let [old (aget props k)]
      (delete props k)
      (.Unlock propMu)
      old)))

(go/func PropertyNames
  "PropertyNames lists the system properties' names, sorted (System.getProperties waits for
the translated Properties).\n"
  ^{:tag (slice string)} []
  (.Lock propMu)
  (initProps)
  (let [ks (make (slice string) 0 (len props))]
    (range [k props]
      (set! ks (append ks k)))
    (.Unlock propMu)
    (sort/Strings ks)
    ks))

;; ---------------------------------------------------------------------------------------
;; Runtime (leaf; Runtime.getRuntime's one instance)

(go/type Runtime (struct Object))

(go/var Runtime_class
  (Define (addr (lit ClassInfo :Name "java.lang.Runtime" :Kind KindClass :Modifiers AccPublic
                     :Super Object_class :Go "arbace/jrt.Runtime"))))

(go/var theRuntime (addr (lit Runtime)))

(go/func Runtime_GetRuntime__Runtime "Runtime_GetRuntime__Runtime is Runtime.getRuntime().\n"
  ^{:tag (* Runtime)} []
  theRuntime)

(go/method AvailableProcessors__I "AvailableProcessors__I is the host's processor count.\n"
  ^int32 [^{:tag (* Runtime)} r]
  (conv int32 (.NumCPU (CurrentHost))))

(go/method FreeMemory__J "FreeMemory__J: the heap's free bytes (HeapIdle).\n" ^int64 [^{:tag (* Runtime)} r]
  (let [^runtime/MemStats m (zero runtime/MemStats)]
    (runtime/ReadMemStats (addr m))
    (conv int64 (- (.-HeapSys m) (.-HeapInuse m)))))

(go/method TotalMemory__J "TotalMemory__J: the heap's bytes obtained from the system.\n" ^int64 [^{:tag (* Runtime)} r]
  (let [^runtime/MemStats m (zero runtime/MemStats)]
    (runtime/ReadMemStats (addr m))
    (conv int64 (.-HeapSys m))))

(go/method MaxMemory__J "MaxMemory__J: the memory limit (GOMEMLIMIT), Long.MAX_VALUE without one.\n"
  ^int64 [^{:tag (* Runtime)} r]
  (debug/SetMemoryLimit -1))

(go/method Gc__V [^{:tag (* Runtime)} r] (runtime/GC))
(go/method Exit_I__V [^{:tag (* Runtime)} r ^int32 status] (System_Exit_I__V status))
(go/method Halt_I__V "Halt_I__V is Runtime.halt: exit without the shutdown hooks.\n"
  [^{:tag (* Runtime)} r ^int32 status]
  (.Exit (CurrentHost) (conv int status)))

(go/method AddShutdownHook_Thread__V
  "AddShutdownHook_Thread__V is Runtime.addShutdownHook: the hook runs at System.exit (not
when main returns: RunMain's caller exits).\n"
  [^{:tag (* Runtime)} r ^Thread_I hook]
  (let [t (.Self_Thread (nnIface hook))]
    (when (!= (.Load (.-state t)) threadNew)
      (panic (IllegalArgumentException_New_String (Str "Hook already running"))))
    (.Lock hookMu)
    (defer (.Unlock hookMu))
    (range [_ h hooks]
      (when (== h t)
        (panic (IllegalArgumentException_New_String (Str "Hook previously registered")))))
    (set! hooks (append hooks t))))

(go/method Ref ^any [^{:tag (* Runtime)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* Runtime)} t] Runtime_class)
(go/method Clone__O ^any [^{:tag (* Runtime)} t] (panic (CloneNotSupported t)))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* Runtime)} t] (Object_toString t))

(go/func Runtime_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* Runtime) x)] ok))

(go/var ^{:tag sync/Mutex} hookMu)
(go/var ^{:tag (slice (* Thread))} hooks)

(go/func runShutdownHooks "runShutdownHooks starts every hook and joins them (once).\n" []
  (.Lock hookMu)
  (let [hs hooks]
    (set! hooks nil)
    (.Unlock hookMu)
    (range [_ h hs]
      (.Start__V (.-self h)))
    (range [_ h hs]
      (runCatching (fn [] (.join h -1))))))

;; ---------------------------------------------------------------------------------------
;; The standard error of printStackTrace and uncaught exceptions: the host's (Stderr), until
;; PrintStream exists and System.err can be set

(go/func init []
  (set! (.-IsInstance (.Info Runtime_class)) Runtime_InstanceOf)
  (set! StderrPrint (fn [^string s] (.WriteString Stderr s))))
