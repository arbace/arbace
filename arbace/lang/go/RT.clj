;; Go-build variant of arbace.lang.RT (C2G-SPEC §4.6): read by c2g only, never by the JVM
;; build. RT's static initializer loads arbace/core through the compiler, which the Go build
;; has not (its evaluator is B1a step 5): the variant initializes RT's vars as the JVM's does
;; and leaves the loading of arbace/core to the program. What needs no arbace.core works
;; without it: vars, namespaces, the reader, RT.print's own printer (used until arbace.core
;; sets *print-initialized*), and *out*, *err*, *in* over System's streams.
(in-ns 'arbace.lang)

(c2g/variant RT
  ;; *out*, *err*, *in* are the JVM's (RT's own fields): an OutputStreamWriter over System.out,
  ;; a PrintWriter over one over System.err, a LineNumberingPushbackReader over an
  ;; InputStreamReader over System.in, all translated (C2G-NOTES.md, phase 2C)

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
