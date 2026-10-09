;; Go-build variant of arbace.lang.RT (C2G-SPEC §4.6): read by c2g only, never by the JVM
;; build. RT's static initializer loads arbace/core through the compiler, which the Go build
;; has not (its evaluator is B1a step 5): the variant initializes RT's vars as the JVM's does
;; and leaves the loading of arbace/core to the program.
(in-ns 'arbace.lang)

(c2g/variant RT
  ;; *out*, *in*, *err*: jrt has no OutputStreamWriter or InputStreamReader yet (phase 2: Writers
  ;; and a Reader over jrt.Stdout, Stderr, Stdin); the vars start unbound to a stream
  (field ^:public ^:static ^:final ^Var OUT
    (.setDynamic (Var/intern CLOJURE_NS (Symbol/intern "*out*") nil)))

  (field ^:public ^:static ^:final ^Var IN
    (.setDynamic (Var/intern CLOJURE_NS (Symbol/intern "*in*") nil)))

  (field ^:public ^:static ^:final ^Var ERR
    (.setDynamic (Var/intern CLOJURE_NS (Symbol/intern "*err*") nil)))

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
  (method ^:public ^:static makeClassLoader ^ClassLoader [] nil)

  (method ^:public ^:static baseLoader ^ClassLoader [] nil)

  ;; classes by name: jrt's registry, the closed world and the classes the evaluator defines
  ;; (C2G-SPEC §10.3); no DynamicClassLoader holds classes in memory
  (method ^:public ^:static classForName ^Class [^String name ^boolean load ^ClassLoader loader]
    (Class/forName name load loader)))
