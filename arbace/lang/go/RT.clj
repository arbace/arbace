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
  ;; Compiler/LOADER is bound to nil in the Go build (no DynamicClassLoader: makeClassLoader),
  ;; so the base loader is the application loader then (repl/source-fn's resources)
  (method ^:public ^:static baseLoader ^ClassLoader []
    (let [l (when (.isBound arbace.lang.Compiler/LOADER) (.deref arbace.lang.Compiler/LOADER))]
      (if (some? l)
          (cast ClassLoader l)
          (.getClassLoader arbace.lang.Compiler))))

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
                      (RT/list (RT/vector namesym))))
          ;; arbace.core, as the JVM's RT loads it: from the program's embedded sources (B1a step
          ;; 5; doc/go/EVAL-NOTES.md). A program without them (c2g's check programs, the
          ;; evaluator's proof) runs without arbace.core, as before step 5
          (when (some? (RT/resourceAsStream nil "arbace/core.clj"))
            (try (RT/load "arbace/core") (catch Exception e (throw (Util/sneakyThrow e))))
            (set! CHECK_SPECS RT/instrumentMacros)))))))

;; B1a step 5 (doc/go/EVAL-NOTES.md): sources by name. The Go build has no class files and no
;; URLs: RT.load reads a namespace's .clj (or .cljc) from the program's embedded sources (jrt's
;; host resources, C2G-SPEC §10.3), else from the directories of ARBACE_PATH (separated by :),
;; and evaluates it; RT's initialization loads arbace.core so, and RT.doInit has no socket
;; server (arbace.core.server is not in the Go build: JAVA-SURFACE.md decision 6)
(c2g/variant RT
  (method ^:public ^:static resourceAsStream ^InputStream [^ClassLoader loader ^String name]
    (let [b (RT/hostResource name)]
      (if (some? b)
          (java.io.ByteArrayInputStream. b)
          (RT/pathResource name))))

  (c2g/add
    (method ^:static ^:native hostResource ^byte/1 [^String name]))

  (c2g/add
    (method ^:static pathResource ^InputStream [^String name]
      (let [path (System/getenv "ARBACE_PATH")]
        (when (some? path)
          (loop [^int start 0]
            (when (<= start (.length path))
              (let [colon (.indexOf path ":" start)
                    end (if (< colon 0) (.length path) colon)
                    dir (.substring path start end)]
                (when (> (.length dir) 0)
                  (let [ins (RT/openFile (java-str dir "/" name))]
                    (when (some? ins) (return ins))))
                (recur (unchecked-inc-int end))))))
        nil)))

  (c2g/add
    (method ^:static openFile ^InputStream [^String file]
      (try
        (java.io.FileInputStream. file)
        (catch FileNotFoundException e nil))))

  (method ^:public ^:static load :throws [IOException ClassNotFoundException] ^void [^String scriptbase
                                                                                     ^boolean failIfNotFound]
    (let [classfile (java-str scriptbase LOADER_SUFFIX ".class")
          cljfile (java-str scriptbase ".clj")
          cljcfile (java-str scriptbase ".cljc")
          ^:mutable scriptfile cljfile
          ^:mutable ins (RT/resourceAsStream nil cljfile)]
      (when (nil? ins)
        (set! scriptfile cljcfile)
        (set! ins (RT/resourceAsStream nil cljcfile)))
      (cond
        (some? ins)
          (let [slash (.lastIndexOf scriptfile \/)
                file (if (>= slash 0) (.substring scriptfile (unchecked-add-int slash 1)) scriptfile)]
            (let [t0 (System/nanoTime)
                  st (arbace.lang.Compiler$Evaluator/state)
                  aot (.-aot st)]
              ;; an embedded namespace is one the JVM build loads AOT-compiled (DefExpr.eval)
              (set! (.-aot st) (instance? java.io.ByteArrayInputStream ins))
              (try
                (arbace.lang.Compiler/load (InputStreamReader. ins UTF8) scriptfile file)
                (finally
                  (set! (.-aot st) aot)
                  (.close ins)
                  ;; ARBACE_LOAD_TIMES: each source's load time on stderr (EVAL-NOTES.md)
                  (when (some? (System/getenv "ARBACE_LOAD_TIMES"))
                    (.println (RT/errPrintWriter)
                              (java-str "load " scriptfile " "
                                        (quot (- (System/nanoTime) t0) 1000000) " ms")))))))
        failIfNotFound
          (throw
            (FileNotFoundException.
              (String/format
                "Could not locate %s, %s or %s on classpath.%s"
                (new
                  Object/1
                  [classfile
                   cljfile
                   cljcfile
                   (if (.contains scriptbase "_")
                       " Please check that namespaces with dashes use underscores in the Clojure file name."
                       "")])))))))

  (method ^:private ^:static ^:synchronized doInit ^void []
    (when-not INIT
      (set! INIT true)
      (Var/pushThreadBindings
        (^[Object/1] RT/mapUniqueKeys CURRENT_NS
                                      (.deref CURRENT_NS)
                                      WARN_ON_REFLECTION
                                      (.deref WARN_ON_REFLECTION)
                                      RT/UNCHECKED_MATH
                                      (.deref RT/UNCHECKED_MATH)))
      (try
        (let [USER (Symbol/intern "user")
              CLOJURE (Symbol/intern "arbace.core")
              in_ns (RT/var "arbace.core" "in-ns")
              refer (RT/var "arbace.core" "refer")]
          (.invoke in_ns USER)
          (.invoke refer CLOJURE)
          (RT/maybeLoadResourceScript "user.clj"))
        (catch Exception e (throw (Util/sneakyThrow e)))
        (finally (Var/popThreadBindings))))))

;; B1a step 5 (doc/go/EVAL-PLAN.md): there are no class loaders in the Go build (C2G-SPEC §10.3);
;; Compiler.eval and Compiler.load bind *loader* to this, as on the JVM, and nothing reads it
(c2g/variant RT
  ;; (baseLoader and classForName: above, from phase 2A)
  (method ^:public ^:static makeClassLoader ^ClassLoader [] nil))
