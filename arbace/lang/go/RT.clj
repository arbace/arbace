;; Go-build variant of arbace.lang.RT (C2G-SPEC §4.6): read by c2g only, never by the JVM
;; build. RT's static initializer loads arbace/core through the compiler, which the Go build
;; has not (its evaluator is B1a step 5): the variant initializes RT's vars as the JVM's does
;; and leaves the loading of arbace/core to the program. What needs no arbace.core works
;; without it: vars, namespaces, the reader, RT.print's own printer (used until arbace.core
;; sets *print-initialized*), and *out*, *err*, *in* over the host's standard streams.
(in-ns 'arbace.lang)

(c2g/variant RT
  ;; *out*, *err*, *in*: the JVM's are an OutputStreamWriter over System.out, a PrintWriter
  ;; (autoflush) over an OutputStreamWriter over System.err, and a LineNumberingPushbackReader
  ;; over an InputStreamReader over System.in. jrt has neither those readers and writers nor
  ;; System's streams yet (phase 2: C2G-NOTES.md, phase 2A): until it has, HostWriter and
  ;; HostReader below stand in, UTF-8 over jrt.Stdout, Stderr and Stdin, buffered as the
  ;; JVM's (output reaches the stream at flush, or when the 8 KiB buffer fills).
  (field ^:public ^:static ^:final ^Var OUT
    (.setDynamic (Var/intern CLOJURE_NS (Symbol/intern "*out*") (HostWriter. 1))))

  (field ^:public ^:static ^:final ^Var IN
    (.setDynamic (Var/intern CLOJURE_NS
                             (Symbol/intern "*in*")
                             (LineNumberingPushbackReader. (HostReader.)))))

  (field ^:public ^:static ^:final ^Var ERR
    (.setDynamic (Var/intern CLOJURE_NS
                             (Symbol/intern "*err*")
                             (^[Writer boolean] PrintWriter/new (HostWriter. 2) true))))

  ;; A Writer over the host's standard output (fd 1) or error (fd 2): UTF-8, a lone surrogate
  ;; as ?, as the JVM's encoder writes them. The buffer and the encoding are jrt's (natives).
  (c2g/add
    (defclass ^:static ^:final HostWriter
      :extends Writer

      (field ^:private ^:final ^int fd)

      (constructor [this ^int fd]
        (set! (.-fd this) fd))

      (method ^:public write ^void [this ^char/1 cbuf ^int off ^int len]
        (java.util.Objects/checkFromIndexSize off len (alength cbuf))
        (locking (.-lock this) (HostWriter/hostWrite fd cbuf off len)))

      (method ^:public flush ^void [this]
        (locking (.-lock this) (HostWriter/hostFlush fd)))

      (method ^:public close ^void [this] (.flush this))

      (method ^:private ^:static ^:native hostWrite ^void [^int fd ^char/1 cbuf ^int off ^int len])

      (method ^:private ^:static ^:native hostFlush ^void [^int fd])))

  ;; A Reader over the host's standard input: UTF-8 decoded (malformed input as U+FFFD), read
  ;; blocks until at least one char is there, as InputStreamReader's does
  (c2g/add
    (defclass ^:static ^:final HostReader
      :extends Reader

      (method ^:public read ^int [this ^char/1 cbuf ^int off ^int len]
        (java.util.Objects/checkFromIndexSize off len (alength cbuf))
        (if (== len 0)
            0
            (locking (.-lock this) (HostReader/hostRead cbuf off len))))

      (method ^:public close ^void [this])

      (method ^:private ^:static ^:native hostRead ^int [^char/1 cbuf ^int off ^int len])))

  ;; Classes by name (C2G-SPEC §10.3): the closed world in jrt's registry, through the one
  ;; class loader the Go build has; no DynamicClassLoader (not translated: no classes are
  ;; defined at run time) and no thread context loader to consult (*use-context-classloader*
  ;; is kept, but every loader is the same)
  (method ^:public ^:static baseLoader ^ClassLoader []
    (if (.isBound arbace.lang.Compiler/LOADER)
        (cast ClassLoader (.deref arbace.lang.Compiler/LOADER))
        (.getClassLoader arbace.lang.Compiler)))

  (method ^:public ^:static classForName ^Class [^String name ^boolean load ^ClassLoader loader]
    (Class/forName name load loader))

  (static-initializer
    (let [arglistskw (Keyword/intern nil "arglists")
          namesym (Symbol/intern "name")]
      (.setTag OUT (Symbol/intern "java.io.Writer"))
      (.setTag CURRENT_NS (Symbol/intern "arbace.lang.Namespace"))
      (.setMeta AGENT
                (^[Object/1] RT/map
                  DOC_KEY
                  "The agent currently running an action on this thread, else nil"))
      (.setTag AGENT (Symbol/intern "arbace.lang.Agent"))
      (.setTag MATH_CONTEXT (Symbol/intern "java.math.MathContext"))
      (let [nv (Var/intern CLOJURE_NS NAMESPACE bootNamespace)]
        (.setMacro nv)
        (let [^:mutable ^Var v nil]
          (set! v (Var/intern CLOJURE_NS IN_NAMESPACE inNamespace))
          (.setMeta v
                    (^[Object/1] RT/map
                      DOC_KEY
                      "Sets *ns* to the namespace named by the symbol, creating it if needed."
                      arglistskw
                      (RT/list (RT/vector namesym))))
          (set! v
                (Var/intern CLOJURE_NS
                            LOAD_FILE
                            (anon AFn []
                              (method ^:public invoke [this arg1]
                                (try
                                  (arbace.lang.Compiler/loadFile (cast String arg1))
                                  (catch IOException e (throw (Util/sneakyThrow e))))))))
          (.setMeta v
                    (^[Object/1] RT/map
                      DOC_KEY
                      "Sequentially read and evaluate the set of forms contained in the file."
                      arglistskw
                      (RT/list (RT/vector namesym)))))))))

;; B1a step 5 (doc/go/EVAL-PLAN.md): there are no class loaders in the Go build (C2G-SPEC §10.3);
;; Compiler.eval and Compiler.load bind *loader* to this, as on the JVM, and nothing reads it
(c2g/variant RT
  ;; (baseLoader and classForName: above, from phase 2A)
  (method ^:public ^:static makeClassLoader ^ClassLoader [] nil))
