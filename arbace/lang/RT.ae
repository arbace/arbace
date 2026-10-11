;; /**
;;  *   Copyright (c) Rich Hickey. All rights reserved.
;;  *   The use and distribution terms for this software are covered by the
;;  *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  *   which can be found in the file epl-v10.html at the root of this distribution.
;;  *   By using this software in any fashion, you are agreeing to be bound by
;;  *     the terms of this license.
;;  *   You must not remove this notice, or any other, from this software.
;;  **/
;;
;; /* rich Mar 25, 2006 4:28:27 PM */
;;
;; Converted from clojure/lang/RT.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.io File
                  FileNotFoundException
                  IOException
                  InputStream
                  InputStreamReader
                  ObjectStreamException
                  OutputStreamWriter
                  PrintWriter
                  PushbackReader
                  Reader
                  Serializable
                  StringReader
                  StringWriter
                  Writer)
        '(java.lang.reflect Array)
        '(java.net JarURLConnection MalformedURLException URI URISyntaxException URL)
        '(java.nio.charset Charset)
        '(java.util ArrayList
                    Arrays
                    Collection
                    Comparator
                    Iterator
                    List
                    Map
                    Map$Entry
                    NoSuchElementException
                    RandomAccess
                    Set)
        '(java.util.concurrent.atomic AtomicInteger)
        '(java.util.regex Matcher Pattern))

(defclass ^:public RT
  (field ^:public ^:static ^:final ^Boolean T Boolean/TRUE)

  (field ^:public ^:static ^:final ^Boolean F Boolean/FALSE)

  (field ^:public ^:static ^:final ^String LOADER_SUFFIX "__init")

  (field ^:public ^:static ^:final ^IPersistentMap DEFAULT_IMPORTS
    (^[Object/1] RT/map (Symbol/intern "AbstractMethodError")
                        AbstractMethodError
                        (Symbol/intern "Appendable")
                        Appendable
                        (Symbol/intern "ArithmeticException")
                        ArithmeticException
                        (Symbol/intern "ArrayIndexOutOfBoundsException")
                        ArrayIndexOutOfBoundsException
                        (Symbol/intern "ArrayStoreException")
                        ArrayStoreException
                        (Symbol/intern "AssertionError")
                        AssertionError
                        (Symbol/intern "AutoCloseable")
                        AutoCloseable
                        (Symbol/intern "BigDecimal")
                        BigDecimal
                        (Symbol/intern "BigInteger")
                        BigInteger
                        (Symbol/intern "Boolean")
                        Boolean
                        (Symbol/intern "BootstrapMethodError")
                        BootstrapMethodError
                        (Symbol/intern "Byte")
                        Byte
                        (Symbol/intern "Callable")
                        Callable
                        (Symbol/intern "CharSequence")
                        CharSequence
                        (Symbol/intern "Character")
                        Character
                        (Symbol/intern "Class")
                        Class
                        (Symbol/intern "ClassCastException")
                        ClassCastException
                        (Symbol/intern "ClassCircularityError")
                        ClassCircularityError
                        (Symbol/intern "ClassFormatError")
                        ClassFormatError
                        (Symbol/intern "ClassLoader")
                        ClassLoader
                        (Symbol/intern "ClassNotFoundException")
                        ClassNotFoundException
                        (Symbol/intern "ClassValue")
                        ClassValue
                        (Symbol/intern "CloneNotSupportedException")
                        CloneNotSupportedException
                        (Symbol/intern "Cloneable")
                        Cloneable
                        (Symbol/intern "Comparable")
                        Comparable
                        (Symbol/intern "Compiler")
                        arbace.lang.Compiler
                        (Symbol/intern "Deprecated")
                        Deprecated
                        (Symbol/intern "Double")
                        Double
                        (Symbol/intern "Enum")
                        Enum
                        (Symbol/intern "EnumConstantNotPresentException")
                        EnumConstantNotPresentException
                        (Symbol/intern "Error")
                        Error
                        (Symbol/intern "Exception")
                        Exception
                        (Symbol/intern "ExceptionInInitializerError")
                        ExceptionInInitializerError
                        (Symbol/intern "Float")
                        Float
                        (Symbol/intern "IllegalAccessError")
                        IllegalAccessError
                        (Symbol/intern "IllegalAccessException")
                        IllegalAccessException
                        (Symbol/intern "IllegalArgumentException")
                        IllegalArgumentException
                        (Symbol/intern "IllegalCallerException")
                        IllegalCallerException
                        (Symbol/intern "IllegalMonitorStateException")
                        IllegalMonitorStateException
                        (Symbol/intern "IllegalStateException")
                        IllegalStateException
                        (Symbol/intern "IllegalThreadStateException")
                        IllegalThreadStateException
                        (Symbol/intern "IncompatibleClassChangeError")
                        IncompatibleClassChangeError
                        (Symbol/intern "IndexOutOfBoundsException")
                        IndexOutOfBoundsException
                        (Symbol/intern "InheritableThreadLocal")
                        InheritableThreadLocal
                        (Symbol/intern "InstantiationError")
                        InstantiationError
                        (Symbol/intern "InstantiationException")
                        InstantiationException
                        (Symbol/intern "Integer")
                        Integer
                        (Symbol/intern "InternalError")
                        InternalError
                        (Symbol/intern "InterruptedException")
                        InterruptedException
                        (Symbol/intern "Iterable")
                        Iterable
                        (Symbol/intern "LayerInstantiationException")
                        LayerInstantiationException
                        (Symbol/intern "LinkageError")
                        LinkageError
                        (Symbol/intern "Long")
                        Long
                        (Symbol/intern "Math")
                        Math
                        (Symbol/intern "ModuleLayer")
                        ModuleLayer
                        (Symbol/intern "NegativeArraySizeException")
                        NegativeArraySizeException
                        (Symbol/intern "NoClassDefFoundError")
                        NoClassDefFoundError
                        (Symbol/intern "NoSuchFieldError")
                        NoSuchFieldError
                        (Symbol/intern "NoSuchFieldException")
                        NoSuchFieldException
                        (Symbol/intern "NoSuchMethodError")
                        NoSuchMethodError
                        (Symbol/intern "NoSuchMethodException")
                        NoSuchMethodException
                        (Symbol/intern "NullPointerException")
                        NullPointerException
                        (Symbol/intern "Number")
                        Number
                        (Symbol/intern "NumberFormatException")
                        NumberFormatException
                        (Symbol/intern "Object")
                        Object
                        (Symbol/intern "OutOfMemoryError")
                        OutOfMemoryError
                        (Symbol/intern "Override")
                        Override
                        (Symbol/intern "Package")
                        Package
                        (Symbol/intern "Process")
                        Process
                        (Symbol/intern "ProcessBuilder")
                        ProcessBuilder
                        (Symbol/intern "ProcessHandle")
                        ProcessHandle
                        (Symbol/intern "Readable")
                        Readable
                        (Symbol/intern "ReflectiveOperationException")
                        ReflectiveOperationException
                        (Symbol/intern "Runnable")
                        Runnable
                        (Symbol/intern "Runtime")
                        Runtime
                        (Symbol/intern "RuntimeException")
                        RuntimeException
                        (Symbol/intern "RuntimePermission")
                        RuntimePermission
                        (Symbol/intern "SecurityException")
                        SecurityException
                        (Symbol/intern "Short")
                        Short
                        (Symbol/intern "StackOverflowError")
                        StackOverflowError
                        (Symbol/intern "StackTraceElement")
                        StackTraceElement
                        (Symbol/intern "StackWalker")
                        StackWalker
                        (Symbol/intern "StrictMath")
                        StrictMath
                        (Symbol/intern "String")
                        String
                        (Symbol/intern "StringBuffer")
                        StringBuffer
                        (Symbol/intern "StringBuilder")
                        StringBuilder
                        (Symbol/intern "StringIndexOutOfBoundsException")
                        StringIndexOutOfBoundsException
                        (Symbol/intern "SuppressWarnings")
                        SuppressWarnings
                        (Symbol/intern "System")
                        System
                        (Symbol/intern "Thread")
                        Thread
                        (Symbol/intern "Thread$State")
                        Thread$State
                        (Symbol/intern "Thread$UncaughtExceptionHandler")
                        Thread$UncaughtExceptionHandler
                        (Symbol/intern "ThreadDeath")
                        ThreadDeath
                        (Symbol/intern "ThreadGroup")
                        ThreadGroup
                        (Symbol/intern "ThreadLocal")
                        ThreadLocal
                        (Symbol/intern "Throwable")
                        Throwable
                        (Symbol/intern "TypeNotPresentException")
                        TypeNotPresentException
                        (Symbol/intern "UnknownError")
                        UnknownError
                        (Symbol/intern "UnsatisfiedLinkError")
                        UnsatisfiedLinkError
                        (Symbol/intern "UnsupportedClassVersionError")
                        UnsupportedClassVersionError
                        (Symbol/intern "UnsupportedOperationException")
                        UnsupportedOperationException
                        (Symbol/intern "VerifyError")
                        VerifyError
                        (Symbol/intern "VirtualMachineError")
                        VirtualMachineError
                        (Symbol/intern "Void")
                        Void
                        (Symbol/intern "ExceptionInfo")
                        arbace.lang.ExceptionInfo))

  (field ^:public ^:static ^Charset UTF8 (Charset/forName "UTF-8"))

  (method ^:static readTrueFalseUnknown [^String s]
    (cond
      (.equals s "true") Boolean/TRUE
      (.equals s "false") Boolean/FALSE
      :else (Keyword/intern nil "unknown")))

  (field ^:public ^:static ^:final REQUIRE_LOCK (Object.))

  (field ^:public ^:static ^:final ^Namespace CLOJURE_NS
    (Namespace/findOrCreate (Symbol/intern "arbace.core")))

  (field ^:public ^:static ^:final ^Var OUT
    (.setDynamic (Var/intern CLOJURE_NS (Symbol/intern "*out*") (OutputStreamWriter. System/out))))

  (field ^:public ^:static ^:final ^Var IN
    (.setDynamic (Var/intern CLOJURE_NS
                             (Symbol/intern "*in*")
                             (LineNumberingPushbackReader. (InputStreamReader. System/in)))))

  (field ^:public ^:static ^:final ^Var ERR
    (.setDynamic (Var/intern CLOJURE_NS
                             (Symbol/intern "*err*")
                             (^[Writer boolean] PrintWriter/new
                               (OutputStreamWriter. System/err)
                               true))))

  (field ^:static ^:final ^Keyword TAG_KEY (Keyword/intern nil "tag"))

  (field ^:static ^:final ^Keyword PARAM_TAGS_KEY (Keyword/intern nil "param-tags"))

  (field ^:static ^:final ^Keyword CONST_KEY (Keyword/intern nil "const"))

  (field ^:public ^:static ^:final ^Var AGENT
    (.setDynamic (Var/intern CLOJURE_NS (Symbol/intern "*agent*") nil)))

  (field ^:static readeval
    (RT/readTrueFalseUnknown (System/getProperty "arbace.read.eval" "true")))

  (field ^:public ^:static ^:final ^Var READEVAL
    (.setDynamic (Var/intern CLOJURE_NS (Symbol/intern "*read-eval*") readeval)))

  (field ^:public ^:static ^:final ^Var DATA_READERS
    (.setDynamic (Var/intern CLOJURE_NS (Symbol/intern "*data-readers*") (^[Object/1] RT/map))))

  (field ^:public ^:static ^:final ^Var DEFAULT_DATA_READER_FN
    (.setDynamic (Var/intern CLOJURE_NS
                             (Symbol/intern "*default-data-reader-fn*")
                             (^[Object/1] RT/map))))

  (field ^:public ^:static ^:final ^Var DEFAULT_DATA_READERS
    (Var/intern CLOJURE_NS (Symbol/intern "default-data-readers") (^[Object/1] RT/map)))

  (field ^:public ^:static ^:final ^Var SUPPRESS_READ
    (.setDynamic (Var/intern CLOJURE_NS (Symbol/intern "*suppress-read*") nil)))

  (field ^:public ^:static ^:final ^Var ASSERT
    (.setDynamic (Var/intern CLOJURE_NS (Symbol/intern "*assert*") T)))

  (field ^:public ^:static ^:final ^Var MATH_CONTEXT
    (.setDynamic (Var/intern CLOJURE_NS (Symbol/intern "*math-context*") nil)))

  (field ^:static ^Keyword EVAL_FILE_KEY (Keyword/intern "arbace.core" "eval-file"))

  (field ^:static ^Keyword LINE_KEY (Keyword/intern nil "line"))

  (field ^:static ^Keyword COLUMN_KEY (Keyword/intern nil "column"))

  (field ^:static ^Keyword FILE_KEY (Keyword/intern nil "file"))

  (field ^:static ^Keyword DECLARED_KEY (Keyword/intern nil "declared"))

  (field ^:static ^Keyword DOC_KEY (Keyword/intern nil "doc"))

  (field ^:public ^:static ^:final ^Var USE_CONTEXT_CLASSLOADER
    (.setDynamic (Var/intern CLOJURE_NS (Symbol/intern "*use-context-classloader*") T)))

  (field ^:public ^:static ^:final ^Var UNCHECKED_MATH
    (.setDynamic (Var/intern (Namespace/findOrCreate (Symbol/intern "arbace.core"))
                             (Symbol/intern "*unchecked-math*")
                             Boolean/FALSE)))

  (field ^:static ^:final ^Symbol LOAD_FILE (Symbol/intern "load-file"))

  (field ^:static ^:final ^Symbol IN_NAMESPACE (Symbol/intern "in-ns"))

  (field ^:static ^:final ^Symbol NAMESPACE (Symbol/intern "ns"))

  (field ^:static ^:final ^Symbol IDENTICAL (Symbol/intern "identical?"))

  (field ^:static ^:final ^Var CMD_LINE_ARGS
    (.setDynamic (Var/intern CLOJURE_NS (Symbol/intern "*command-line-args*") nil)))

  (field ^:public ^:static ^:final ^Var CURRENT_NS
    (.setDynamic (Var/intern CLOJURE_NS (Symbol/intern "*ns*") CLOJURE_NS)))

  (field ^:static ^:final ^Var FLUSH_ON_NEWLINE
    (.setDynamic (Var/intern CLOJURE_NS (Symbol/intern "*flush-on-newline*") T)))

  (field ^:static ^:final ^Var PRINT_META
    (.setDynamic (Var/intern CLOJURE_NS (Symbol/intern "*print-meta*") F)))

  (field ^:static ^:final ^Var PRINT_READABLY
    (.setDynamic (Var/intern CLOJURE_NS (Symbol/intern "*print-readably*") T)))

  (field ^:static ^:final ^Var PRINT_DUP
    (.setDynamic (Var/intern CLOJURE_NS (Symbol/intern "*print-dup*") F)))

  (field ^:static ^:final ^Var WARN_ON_REFLECTION
    (.setDynamic (Var/intern CLOJURE_NS (Symbol/intern "*warn-on-reflection*") F)))

  (field ^:static ^:final ^Var ALLOW_UNRESOLVED_VARS
    (.setDynamic (Var/intern CLOJURE_NS (Symbol/intern "*allow-unresolved-vars*") F)))

  (field ^:static ^:final ^Var READER_RESOLVER
    (.setDynamic (Var/intern CLOJURE_NS (Symbol/intern "*reader-resolver*") nil)))

  (field ^:static ^:final ^Var IN_NS_VAR (Var/intern CLOJURE_NS (Symbol/intern "in-ns") F))

  (field ^:static ^:final ^Var NS_VAR (Var/intern CLOJURE_NS (Symbol/intern "ns") F))

  (field ^:static ^:final ^Var FN_LOADER_VAR
    (.setDynamic (Var/intern CLOJURE_NS (Symbol/intern "*fn-loader*") nil)))

  (field ^:static ^:final ^Var PRINT_INITIALIZED
    (Var/intern CLOJURE_NS (Symbol/intern "print-initialized")))

  (field ^:static ^:final ^Var PR_ON (Var/intern CLOJURE_NS (Symbol/intern "pr-on")))

  (field ^:static ^:final ^IFn inNamespace
    (anon AFn []
      (method ^:public invoke [this arg1]
        (let [nsname (cast Symbol arg1) ns (Namespace/findOrCreate nsname)] (.set CURRENT_NS ns) ns))))

  (field ^:static ^:final ^IFn bootNamespace
    (anon AFn []
      (method ^:public invoke [this __form __env arg1]
        (let [nsname (cast Symbol arg1) ns (Namespace/findOrCreate nsname)] (.set CURRENT_NS ns) ns))))

  (method ^:public ^:static processCommandLine ^{:tag (List String)} [^String/1 args]
    (let [^{:tag (List String)} arglist (Arrays/asList args)
          split (.indexOf arglist "--")]
      (if (>= split 0)
          (do
            (.bindRoot CMD_LINE_ARGS
                       (RT/seq (.subList arglist (unchecked-add-int split 1) (alength args))))
            (.subList arglist 0 split))
          arglist)))

  (method ^:public ^:static errPrintWriter ^PrintWriter []
    (let [w (cast Writer (.deref ERR))]
      (if (instance? PrintWriter w) (cast PrintWriter w) (PrintWriter. w))))

  (field ^:public ^:static ^:final ^Object/1 EMPTY_ARRAY (new Object/1 []))

  (field ^:public ^:static ^:final ^Comparator DEFAULT_COMPARATOR (DefaultComparator.))

  (defclass ^:private ^:static ^:final DefaultComparator
    :implements [Comparator Serializable]

    (method ^:public compare ^int [this o1 o2] (Util/compare o1 o2))

    (method ^:private readResolve :throws [ObjectStreamException] [this]
      DEFAULT_COMPARATOR))

  (field ^:static ^AtomicInteger id (AtomicInteger. 1))

  (method ^:public ^:static toUrl :throws [MalformedURLException] ^URL [^String url]
    (try
      (.toURL (URI. url))
      (catch [URISyntaxException IllegalArgumentException] e
        (let [ex (MalformedURLException.)] (.initCause ex e) (throw ex)))))

  (method ^:public ^:static toUrl :throws [MalformedURLException] ^URL [^File file]
    (.toURL (.toURI file)))

  (method ^:public ^:static addURL :throws [MalformedURLException] ^void [url]
    (let [u (if (instance? String url) (RT/toUrl (cast String url)) (cast URL url))
          ccl (.getContextClassLoader (Thread/currentThread))]
      (if (instance? DynamicClassLoader ccl)
          (.addURL (cast DynamicClassLoader ccl) u)
          (throw (IllegalAccessError. "Context classloader is not a DynamicClassLoader")))))

  (field ^:public ^:static ^boolean checkSpecAsserts
    (Boolean/getBoolean "arbace.spec.check-asserts"))

  (field ^:public ^:static ^boolean instrumentMacros
    (not (Boolean/getBoolean "arbace.spec.skip-macros")))

  (field ^:static ^:volatile ^boolean CHECK_SPECS false)

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
          (try (RT/load "arbace/core") (catch Exception e (throw (Util/sneakyThrow e))))
          (set! CHECK_SPECS RT/instrumentMacros)))))

  (method ^:public ^:static keyword ^Keyword [^String ns ^String name]
    (Keyword/intern (Symbol/intern ns name)))

  (method ^:public ^:static var ^Var [^String ns ^String name]
    (Var/intern (Namespace/findOrCreate (Symbol/intern nil ns)) (Symbol/intern nil name)))

  (method ^:public ^:static var ^Var [^String ns ^String name init]
    (Var/intern (Namespace/findOrCreate (Symbol/intern nil ns)) (Symbol/intern nil name) init))

  (method ^:public ^:static loadResourceScript :throws [IOException] ^void [^String name]
    (RT/loadResourceScript name true))

  (method ^:public ^:static maybeLoadResourceScript :throws [IOException] ^void [^String name]
    (RT/loadResourceScript name false))

  (method ^:public ^:static loadResourceScript :throws [IOException] ^void [^String name
                                                                            ^boolean failIfNotFound]
    (RT/loadResourceScript RT name failIfNotFound))

  (method ^:public ^:static loadResourceScript :throws [IOException] ^void [^Class c ^String name]
    (RT/loadResourceScript c name true))

  (method ^:public ^:static loadResourceScript :throws [IOException] ^void [^Class c ^String name
                                                                            ^boolean failIfNotFound]
    (let [slash (.lastIndexOf name \/)
          file (if (>= slash 0) (.substring name (unchecked-add-int slash 1)) name)
          ins (RT/resourceAsStream (RT/baseLoader) name)]
      (cond
        (some? ins)
          (try
            (arbace.lang.Compiler/load (InputStreamReader. ins UTF8) name file)
            (finally (.close ins)))
        failIfNotFound
          (throw (FileNotFoundException.
                   (java-str "Could not locate Clojure resource on classpath: " name))))))

  (method ^:public ^:static lastModified :throws [IOException] ^long [^URL url ^String libfile]
    (let [connection (.openConnection url)]
      (try
        (if (.equals (.getProtocol url) "jar")
            (.getTime (.getEntry (.getJarFile (cast JarURLConnection connection)) libfile))
            (.getLastModified connection))
        (finally (let [ins (.getInputStream connection)] (when (some? ins) (.close ins)))))))

  (method ^:static compile :throws [IOException] ^void [^String cljfile]
    (let [ins (RT/resourceAsStream (RT/baseLoader) cljfile)]
      (if (some? ins)
          (try
            (arbace.lang.Compiler/compile (InputStreamReader. ins UTF8)
                                          cljfile
                                          (.substring
                                            cljfile
                                            (unchecked-add-int 1 (.lastIndexOf cljfile "/"))))
            (finally (.close ins)))
          (throw (FileNotFoundException.
                   (java-str "Could not locate Clojure resource on classpath: " cljfile))))))

  (method ^:public ^:static load :throws [IOException ClassNotFoundException] ^void [^String scriptbase]
    (RT/load scriptbase true))

  (method ^:public ^:static load :throws [IOException ClassNotFoundException] ^void [^String scriptbase
                                                                                     ^boolean failIfNotFound]
    (let [classfile (java-str scriptbase LOADER_SUFFIX ".class")
          cljfile (java-str scriptbase ".clj")
          cljcfile (java-str scriptbase ".cljc")
          ^:mutable scriptfile cljfile
          classURL (RT/getResource (RT/baseLoader) classfile)
          ^:mutable cljURL (RT/getResource (RT/baseLoader) scriptfile)]
      (when (nil? cljURL)
        (set! scriptfile cljcfile)
        (set! cljURL (RT/getResource (RT/baseLoader) scriptfile)))
      (let [^:mutable loaded false]
        (when (or (and (some? classURL)
                       (or (nil? cljURL)
                           (> (RT/lastModified classURL classfile)
                              (RT/lastModified cljURL scriptfile))))
                  (nil? classURL))
          (try
            (Var/pushThreadBindings
              (^[Object/1] RT/mapUniqueKeys CURRENT_NS
                                            (.deref CURRENT_NS)
                                            WARN_ON_REFLECTION
                                            (.deref WARN_ON_REFLECTION)
                                            RT/UNCHECKED_MATH
                                            (.deref RT/UNCHECKED_MATH)))
            (set!
              loaded
              (some? (RT/loadClassForName (java-str (.replace scriptbase \/ \.) LOADER_SUFFIX))))
            (finally (Var/popThreadBindings))))
        (cond
          (and (not loaded) (some? cljURL))
            (if (RT/booleanCast (.deref arbace.lang.Compiler/COMPILE_FILES))
                (RT/compile scriptfile)
                (RT/loadResourceScript RT scriptfile))
          (and (not loaded) failIfNotFound)
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
                         "")]))))))))

  (method ^:public ^:static init ^void [] (RT/doInit))

  (field ^:private ^:static ^boolean INIT false)

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
          (RT/maybeLoadResourceScript "user.clj")
          (let [require (RT/var "arbace.core" "require")
                SERVER (Symbol/intern "arbace.core.server")]
            (.invoke require SERVER)
            (let [start_servers (RT/var "arbace.core.server" "start-servers")]
              (.invoke start_servers (System/getProperties)))))
        (catch Exception e (throw (Util/sneakyThrow e)))
        (finally (Var/popThreadBindings)))))

  (method ^:public ^:static nextID ^int [] (.getAndIncrement id))

  (method ^:public ^:static loadLibrary ^void [^String libname]
    (System/loadLibrary libname))

  (field ^:private ^:static ^:final ^int CHUNK_SIZE 32)

  (method ^:public ^:static chunkIteratorSeq ^ISeq [^:final ^Iterator iter]
    (when (.hasNext iter)
      (LazySeq. (anon AFn []
                  (method ^:public invoke [this]
                    (let [arr (new Object/1 CHUNK_SIZE)
                          ^:mutable ^int n 0]
                      (while (and (.hasNext iter) (< n CHUNK_SIZE))
                        (aset arr n (.next iter))
                        (set! n (unchecked-inc-int n)))
                      (ChunkedCons. (ArrayChunk. arr 0 n) (RT/chunkIteratorSeq iter))))))))

  (method ^:public ^:static seq ^ISeq [coll]
    (cond
      (instance? ASeq coll) (cast ASeq coll)
      (instance? LazySeq coll) (.seq (cast LazySeq coll))
      :else (RT/seqFrom coll)))

  (method ^:static seqFrom ^ISeq [coll]
    (cond
      (instance? Seqable coll) (.seq (cast Seqable coll))
      (some? coll)
        (cond
          (instance? Iterable coll) (RT/chunkIteratorSeq (.iterator (cast Iterable coll)))
          (.isArray (.getClass coll)) (ArraySeq/createFromObject coll)
          (instance? CharSequence coll) (StringSeq/create (cast CharSequence coll))
          (instance? Map coll) (RT/seq (.entrySet (cast Map coll)))
          :else
            (let [c (.getClass coll)
                  sc (.getSuperclass c)]
              (throw (IllegalArgumentException.
                       (java-str "Don't know how to create ISeq from: " (.getName c))))))))

  (method ^:public ^:static canSeq ^boolean [coll]
    (or (or (or (or (or (or (instance? ISeq coll) (instance? Seqable coll)) (nil? coll))
                    (instance? Iterable coll))
                (.isArray (.getClass coll)))
            (instance? CharSequence coll))
        (instance? Map coll)))

  (method ^:public ^:static iter ^Iterator [coll]
    (cond
      (instance? Iterable coll) (.iterator (cast Iterable coll))
      (nil? coll)
        (anon Iterator []
          (method ^:public hasNext ^boolean [this] false)

          (method ^:public next [this] (throw (NoSuchElementException.)))

          (method ^:public remove ^void [this]
            (throw (UnsupportedOperationException.))))
      (instance? Map coll) (.iterator (.entrySet (cast Map coll)))
      (instance? String coll)
        (let [s (cast String coll)]
          (anon Iterator []
            (field ^int i 0)

            (method ^:public hasNext ^boolean [this] (< i (.length s)))

            (method ^:public next [this]
              (.charAt s (let [old-1 i] (set! i (unchecked-inc-int i)) old-1)))

            (method ^:public remove ^void [this]
              (throw (UnsupportedOperationException.)))))
      (.isArray (.getClass coll)) (ArrayIter/createFromObject coll)
      :else (RT/iter (RT/seq coll))))

  (method ^:public ^:static seqOrElse [o] (when (some? (RT/seq o)) o))

  (method ^:public ^:static keys ^ISeq [coll]
    (if (instance? IPersistentMap coll)
        (APersistentMap$KeySeq/createFromMap (cast IPersistentMap coll))
        (APersistentMap$KeySeq/create (RT/seq coll))))

  (method ^:public ^:static vals ^ISeq [coll]
    (if (instance? IPersistentMap coll)
        (APersistentMap$ValSeq/createFromMap (cast IPersistentMap coll))
        (APersistentMap$ValSeq/create (RT/seq coll))))

  (method ^:public ^:static meta ^IPersistentMap [x]
    (when (instance? IMeta x) (.meta (cast IMeta x))))

  (method ^:public ^:static count ^int [^:mutable o]
    (if (instance? Counted o) (.count (cast Counted o)) (RT/countFrom (Util/ret1 o (set! o nil)))))

  (method ^:static countFrom ^int [^:mutable o]
    (cond
      (nil? o) 0
      (instance? IPersistentCollection o)
        (let [^:mutable s (RT/seq o)]
          (set! o nil)
          (let [^:mutable ^int i 0]
            (while (some? s)
              (when (instance? Counted s) (return (unchecked-add-int i (.count s))))
              (set! i (unchecked-inc-int i))
              (set! s (.next s)))
            i))
      (instance? CharSequence o) (.length (cast CharSequence o))
      (instance? Collection o) (.size (cast Collection o))
      (instance? Map o) (.size (cast Map o))
      (instance? Map$Entry o) 2
      (.isArray (.getClass o)) (Array/getLength o)
      :else
        (throw (UnsupportedOperationException.
                 (java-str "count not supported on this type: " (.getSimpleName (.getClass o)))))))

  (method ^:public ^:static conj ^IPersistentCollection [^IPersistentCollection coll x]
    (if (nil? coll) (PersistentList. x) (.cons coll x)))

  (method ^:public ^:static cons ^ISeq [x coll]
    (cond
      (nil? coll) (PersistentList. x)
      (instance? ISeq coll) (Cons. x (cast ISeq coll))
      :else (Cons. x (RT/seq coll))))

  (method ^:public ^:static first [x]
    (if (instance? ISeq x)
        (.first (cast ISeq x))
        (let [seq (RT/seq x)] (when (some? seq) (.first seq)))))

  (method ^:public ^:static second [x] (RT/first (RT/next x)))

  (method ^:public ^:static third [x] (RT/first (RT/next (RT/next x))))

  (method ^:public ^:static fourth [x]
    (RT/first (RT/next (RT/next (RT/next x)))))

  (method ^:public ^:static next ^ISeq [x]
    (if (instance? ISeq x)
        (.next (cast ISeq x))
        (let [seq (RT/seq x)] (when (some? seq) (.next seq)))))

  (method ^:public ^:static more ^ISeq [x]
    (if (instance? ISeq x)
        (.more (cast ISeq x))
        (let [seq (RT/seq x)] (if (nil? seq) PersistentList/EMPTY (.more seq)))))

  (method ^:public ^:static peek [x]
    (when (some? x) (.peek (cast IPersistentStack x))))

  (method ^:public ^:static pop [x]
    (when (some? x) (.pop (cast IPersistentStack x))))

  (field ^:private ^:static ^:final REQ_NOT_FOUND (Object.))

  (method ^:public ^:static reqmsg ^String [key]
    (let [msg "Missing required key: "]
      (if (instance? String key) (java-str msg "\"" key "\"") (java-str msg key))))

  (method ^:public ^:static req [coll key]
    (let [v (RT/get coll key REQ_NOT_FOUND)]
      (if (identical? v REQ_NOT_FOUND) (throw (IllegalArgumentException. (RT/reqmsg key))) v)))

  (method ^:public ^:static get [coll key]
    (if (instance? ILookup coll) (.valAt (cast ILookup coll) key) (RT/getFrom coll key)))

  (method ^:static getFrom [coll key]
    (when (some? coll)
      (cond
        (instance? Map coll) (let [m (cast Map coll)] (.get m key))
        (and (instance? Number key) (or (instance? String coll) (.isArray (.getClass coll))))
          (let [n (.intValue (cast Number key))]
            (when (and (>= n 0) (< n (RT/count coll))) (RT/nth coll n))))))

  (method ^:public ^:static get [coll key notFound]
    (if (instance? ILookup coll)
        (.valAt (cast ILookup coll) key notFound)
        (RT/getFrom coll key notFound)))

  (method ^:static getFrom [coll key notFound]
    (cond
      (nil? coll) notFound
      (instance? Map coll) (let [m (cast Map coll)] (if (.containsKey m key) (.get m key) notFound))
      (and (instance? Number key) (or (instance? String coll) (.isArray (.getClass coll))))
        (let [n (.intValue (cast Number key))]
          (if (and (>= n 0) (< n (RT/count coll))) (RT/nth coll n) notFound))
      :else notFound))

  (method ^:public ^:static assoc ^Associative [coll key val]
    (if (nil? coll)
        (PersistentArrayMap. (new Object/1 [key val]))
        (.assoc (cast Associative coll) key val)))

  (method ^:public ^:static contains [coll key]
    (cond
      (nil? coll) F
      (instance? Associative coll) (if (.containsKey (cast Associative coll) key) T F)
      (instance? IPersistentSet coll) (if (.contains (cast IPersistentSet coll) key) T F)
      (instance? Map coll) (let [m (cast Map coll)] (if (.containsKey m key) T F))
      (instance? Set coll) (let [s (cast Set coll)] (if (.contains s key) T F))
      (and (instance? Number key) (or (instance? String coll) (.isArray (.getClass coll))))
        (let [n (.intValue (cast Number key))] (and (>= n 0) (< n (RT/count coll))))
      (instance? ITransientSet coll) (if (.contains (cast ITransientSet coll) key) T F)
      (instance? ITransientAssociative2 coll)
        (if (.containsKey (cast ITransientAssociative2 coll) key) T F)
      :else
        (throw (IllegalArgumentException.
                 (java-str "contains? not supported on type: " (.getName (.getClass coll)))))))

  (method ^:public ^:static find [coll key]
    (when (some? coll)
      (cond
        (instance? Associative coll) (.entryAt (cast Associative coll) key)
        (instance? Map coll)
          (let [m (cast Map coll)] (when (.containsKey m key) (MapEntry/create key (.get m key))))
        (instance? ITransientAssociative2 coll) (.entryAt (cast ITransientAssociative2 coll) key)
        :else
          (throw (IllegalArgumentException.
                   (java-str "find not supported on type: " (.getName (.getClass coll))))))))

  (method ^:public ^:static findKey ^ISeq [^Keyword key ^:mutable ^ISeq keyvals]
    (while (some? keyvals)
      (let [r (.next keyvals)]
        (when (nil? r) (throw (Util/runtimeException "Malformed keyword argslist")))
        (when (identical? (.first keyvals) key) (return r))
        (set! keyvals (.next r))))
    nil)

  (method ^:public ^:static dissoc [coll key]
    (when (some? coll) (.without (cast IPersistentMap coll) key)))

  (method ^:public ^:static nth [^:mutable coll ^int n]
    (if (instance? Indexed coll)
        (.nth (cast Indexed coll) n)
        (RT/nthFrom (Util/ret1 coll (set! coll nil)) n)))

  (method ^:static nthFrom [^:mutable coll ^int n]
    (when (some? coll)
      (cond
        (instance? CharSequence coll) (Character/valueOf (.charAt (cast CharSequence coll) n))
        (.isArray (.getClass coll))
          (Reflector/prepRet (.getComponentType (.getClass coll)) (Array/get coll n))
        (instance? RandomAccess coll) (.get (cast List coll) n)
        (instance? Matcher coll) (.group (cast Matcher coll) n)
        (instance? Map$Entry coll)
          (let [e (cast Map$Entry coll)]
            (cond
              (== n 0) (.getKey e)
              (== n 1) (.getValue e)
              :else (throw (IndexOutOfBoundsException.))))
        (instance? Sequential coll)
          (let [^:mutable seq (RT/seq coll)]
            (set! coll nil)
            (let [^:mutable ^int i 0]
              (while (and (<= i n) (some? seq))
                (when (== i n) (return (.first seq)))
                (set! i (unchecked-inc-int i))
                (set! seq (.next seq))))
            (throw (IndexOutOfBoundsException.)))
        :else
          (throw (UnsupportedOperationException.
                   (java-str "nth not supported on this type: " (.getSimpleName (.getClass coll))))))))

  (method ^:public ^:static nth [coll ^int n notFound]
    (if (instance? Indexed coll)
        (let [v (cast Indexed coll)] (.nth v n notFound))
        (RT/nthFrom coll n notFound)))

  (method ^:static nthFrom [^:mutable coll ^int n notFound]
    (cond
      (nil? coll) notFound
      (< n 0) notFound
      (instance? CharSequence coll)
        (let [s (cast CharSequence coll)]
          (if (< n (.length s)) (Character/valueOf (.charAt s n)) notFound))
      (.isArray (.getClass coll))
        (if (< n (Array/getLength coll))
            (Reflector/prepRet (.getComponentType (.getClass coll)) (Array/get coll n))
            notFound)
      (instance? RandomAccess coll)
        (let [list (cast List coll)] (if (< n (.size list)) (.get list n) notFound))
      (instance? Matcher coll)
        (let [m (cast Matcher coll)
              groups (.groupCount m)]
          (if (and (> groups 0) (<= n (.groupCount m))) (.group m n) notFound))
      (instance? Map$Entry coll)
        (let [e (cast Map$Entry coll)]
          (cond (== n 0) (.getKey e) (== n 1) (.getValue e) :else notFound))
      (instance? Sequential coll)
        (let [^:mutable seq (RT/seq coll)]
          (set! coll nil)
          (let [^:mutable ^int i 0]
            (while (and (<= i n) (some? seq))
              (when (== i n) (return (.first seq)))
              (set! i (unchecked-inc-int i))
              (set! seq (.next seq))))
          notFound)
      :else
        (throw (UnsupportedOperationException.
                 (java-str "nth not supported on this type: " (.getSimpleName (.getClass coll)))))))

  (method ^:public ^:static assocN [^int n val coll]
    (when (some? coll)
      (cond
        (instance? IPersistentVector coll) (.assocN (cast IPersistentVector coll) n val)
        (instance? Object/1 coll) (let [array (cast Object/1 coll)] (aset array n val) array))))

  (method ^:static hasTag ^boolean [o tag]
    (Util/equals tag (RT/get (RT/meta o) TAG_KEY)))

  (method ^:public ^:static box [x] x)

  (method ^:public ^:static box ^Character [^char x]
    (Character/valueOf x))

  (method ^:public ^:static box [^boolean x] (if x T F))

  (method ^:public ^:static box [^Boolean x] x)

  (method ^:public ^:static box ^Number [^byte x] x)

  (method ^:public ^:static box ^Number [^short x] x)

  (method ^:public ^:static box ^Number [^int x] x)

  (method ^:public ^:static box ^Number [^long x] x)

  (method ^:public ^:static box ^Number [^float x] x)

  (method ^:public ^:static box ^Number [^double x] x)

  (method ^:public ^:static charCast ^char [x]
    (if (instance? Character x)
        (.charValue (cast Character x))
        (let [n (.longValue (cast Number x))]
          (when (or (< n Character/MIN_VALUE) (> n Character/MAX_VALUE))
            (throw (IllegalArgumentException. (java-str "Value out of range for char: " x))))
          (unchecked-char n))))

  (method ^:public ^:static charCast ^char [^byte x]
    (let [i (unchecked-char x)]
      (when-not (== i x)
        (throw (IllegalArgumentException. (java-str "Value out of range for char: " x))))
      i))

  (method ^:public ^:static charCast ^char [^short x]
    (let [i (unchecked-char x)]
      (when-not (== i x)
        (throw (IllegalArgumentException. (java-str "Value out of range for char: " x))))
      i))

  (method ^:public ^:static charCast ^char [^char x] x)

  (method ^:public ^:static charCast ^char [^int x]
    (let [i (unchecked-char x)]
      (when-not (== i x)
        (throw (IllegalArgumentException. (java-str "Value out of range for char: " x))))
      i))

  (method ^:public ^:static charCast ^char [^long x]
    (let [i (unchecked-char x)]
      (when-not (== i x)
        (throw (IllegalArgumentException. (java-str "Value out of range for char: " x))))
      i))

  (method ^:public ^:static charCast ^char [^float x]
    (if (and (>= x (unchecked-float Character/MIN_VALUE))
             (<= x (unchecked-float Character/MAX_VALUE)))
        (unchecked-char x)
        (throw (IllegalArgumentException. (java-str "Value out of range for char: " x)))))

  (method ^:public ^:static charCast ^char [^double x]
    (if (and (>= x Character/MIN_VALUE) (<= x Character/MAX_VALUE))
        (unchecked-char x)
        (throw (IllegalArgumentException. (java-str "Value out of range for char: " x)))))

  (method ^:public ^:static booleanCast ^boolean [x]
    (if (instance? Boolean x) (.booleanValue (cast Boolean x)) (some? x)))

  (method ^:public ^:static booleanCast ^boolean [^boolean x] x)

  (method ^:public ^:static byteCast ^byte [x]
    (if (instance? Byte x)
        (.byteValue (cast Byte x))
        (let [n (RT/longCast x)]
          (when (or (< n Byte/MIN_VALUE) (> n Byte/MAX_VALUE))
            (throw (IllegalArgumentException. (java-str "Value out of range for byte: " x))))
          (unchecked-byte n))))

  (method ^:public ^:static byteCast ^byte [^byte x] x)

  (method ^:public ^:static byteCast ^byte [^short x]
    (let [i (unchecked-byte x)]
      (when-not (== i x)
        (throw (IllegalArgumentException. (java-str "Value out of range for byte: " x))))
      i))

  (method ^:public ^:static byteCast ^byte [^int x]
    (let [i (unchecked-byte x)]
      (when-not (== i x)
        (throw (IllegalArgumentException. (java-str "Value out of range for byte: " x))))
      i))

  (method ^:public ^:static byteCast ^byte [^long x]
    (let [i (unchecked-byte x)]
      (when-not (== i x)
        (throw (IllegalArgumentException. (java-str "Value out of range for byte: " x))))
      i))

  (method ^:public ^:static byteCast ^byte [^float x]
    (if (and (>= x (unchecked-float Byte/MIN_VALUE)) (<= x (unchecked-float Byte/MAX_VALUE)))
        (unchecked-byte x)
        (throw (IllegalArgumentException. (java-str "Value out of range for byte: " x)))))

  (method ^:public ^:static byteCast ^byte [^double x]
    (if (and (>= x Byte/MIN_VALUE) (<= x Byte/MAX_VALUE))
        (unchecked-byte x)
        (throw (IllegalArgumentException. (java-str "Value out of range for byte: " x)))))

  (method ^:public ^:static shortCast ^short [x]
    (if (instance? Short x)
        (.shortValue (cast Short x))
        (let [n (RT/longCast x)]
          (when (or (< n Short/MIN_VALUE) (> n Short/MAX_VALUE))
            (throw (IllegalArgumentException. (java-str "Value out of range for short: " x))))
          (unchecked-short n))))

  (method ^:public ^:static shortCast ^short [^byte x] x)

  (method ^:public ^:static shortCast ^short [^short x] x)

  (method ^:public ^:static shortCast ^short [^int x]
    (let [i (unchecked-short x)]
      (when-not (== i x)
        (throw (IllegalArgumentException. (java-str "Value out of range for short: " x))))
      i))

  (method ^:public ^:static shortCast ^short [^long x]
    (let [i (unchecked-short x)]
      (when-not (== i x)
        (throw (IllegalArgumentException. (java-str "Value out of range for short: " x))))
      i))

  (method ^:public ^:static shortCast ^short [^float x]
    (if (and (>= x (unchecked-float Short/MIN_VALUE)) (<= x (unchecked-float Short/MAX_VALUE)))
        (unchecked-short x)
        (throw (IllegalArgumentException. (java-str "Value out of range for short: " x)))))

  (method ^:public ^:static shortCast ^short [^double x]
    (if (and (>= x Short/MIN_VALUE) (<= x Short/MAX_VALUE))
        (unchecked-short x)
        (throw (IllegalArgumentException. (java-str "Value out of range for short: " x)))))

  (method ^:public ^:static intCast ^int [x]
    (cond
      (instance? Integer x) (.intValue (cast Integer x))
      (instance? Number x) (let [n (RT/longCast x)] (^[long] RT/intCast n))
      :else (.charValue (cast Character x))))

  (method ^:public ^:static intCast ^int [^char x] x)

  (method ^:public ^:static intCast ^int [^byte x] x)

  (method ^:public ^:static intCast ^int [^short x] x)

  (method ^:public ^:static intCast ^int [^int x] x)

  (method ^:public ^:static intCast ^int [^float x]
    (when (or (< x (unchecked-float Integer/MIN_VALUE)) (> x (unchecked-float Integer/MAX_VALUE)))
      (throw (IllegalArgumentException. (java-str "Value out of range for int: " x))))
    (unchecked-int x))

  (method ^:public ^:static intCast ^int [^long x] (Math/toIntExact x))

  (method ^:public ^:static intCast ^int [^double x]
    (when (or (< x Integer/MIN_VALUE) (> x Integer/MAX_VALUE))
      (throw (IllegalArgumentException. (java-str "Value out of range for int: " x))))
    (unchecked-int x))

  (method ^:public ^:static longCast ^long [x]
    (cond
      (or (instance? Integer x) (instance? Long x)) (.longValue (cast Number x))
      (instance? BigInt x)
        (let [bi (cast BigInt x)]
          (if (nil? (.-bipart bi))
              (.-lpart bi)
              (throw (IllegalArgumentException. (java-str "Value out of range for long: " x)))))
      (instance? BigInteger x)
        (let [bi (cast BigInteger x)]
          (if (< (.bitLength bi) 64)
              (.longValue bi)
              (throw (IllegalArgumentException. (java-str "Value out of range for long: " x)))))
      (or (instance? Byte x) (instance? Short x)) (.longValue (cast Number x))
      (instance? Ratio x) (RT/longCast (.bigIntegerValue (cast Ratio x)))
      (instance? Character x) (^[int] RT/longCast (.charValue (cast Character x)))
      :else (^[double] RT/longCast (.doubleValue (cast Number x)))))

  (method ^:public ^:static longCast ^long [^byte x] x)

  (method ^:public ^:static longCast ^long [^short x] x)

  (method ^:public ^:static longCast ^long [^int x] x)

  (method ^:public ^:static longCast ^long [^float x]
    (when (or (< x (unchecked-float Long/MIN_VALUE)) (> x (unchecked-float Long/MAX_VALUE)))
      (throw (IllegalArgumentException. (java-str "Value out of range for long: " x))))
    (unchecked-long x))

  (method ^:public ^:static longCast ^long [^long x] x)

  (method ^:public ^:static longCast ^long [^double x]
    (when (or (< x Long/MIN_VALUE) (> x Long/MAX_VALUE))
      (throw (IllegalArgumentException. (java-str "Value out of range for long: " x))))
    (unchecked-long x))

  (method ^:public ^:static floatCast ^float [x]
    (if (instance? Float x)
        (.floatValue (cast Float x))
        (let [n (.doubleValue (cast Number x))]
          (when (or (< n (unchecked-negate-float Float/MAX_VALUE)) (> n Float/MAX_VALUE))
            (throw (IllegalArgumentException. (java-str "Value out of range for float: " x))))
          (unchecked-float n))))

  (method ^:public ^:static floatCast ^float [^byte x] x)

  (method ^:public ^:static floatCast ^float [^short x] x)

  (method ^:public ^:static floatCast ^float [^int x] x)

  (method ^:public ^:static floatCast ^float [^float x] x)

  (method ^:public ^:static floatCast ^float [^long x] x)

  (method ^:public ^:static floatCast ^float [^double x]
    (when (or (< x (unchecked-negate-float Float/MAX_VALUE)) (> x Float/MAX_VALUE))
      (throw (IllegalArgumentException. (java-str "Value out of range for float: " x))))
    (unchecked-float x))

  (method ^:public ^:static doubleCast ^double [x]
    (.doubleValue (cast Number x)))

  (method ^:public ^:static doubleCast ^double [^byte x] x)

  (method ^:public ^:static doubleCast ^double [^short x] x)

  (method ^:public ^:static doubleCast ^double [^int x] x)

  (method ^:public ^:static doubleCast ^double [^float x] x)

  (method ^:public ^:static doubleCast ^double [^long x] x)

  (method ^:public ^:static doubleCast ^double [^double x] x)

  (method ^:public ^:static uncheckedByteCast ^byte [x]
    (.byteValue (cast Number x)))

  (method ^:public ^:static uncheckedByteCast ^byte [^byte x] x)

  (method ^:public ^:static uncheckedByteCast ^byte [^short x]
    (unchecked-byte x))

  (method ^:public ^:static uncheckedByteCast ^byte [^int x]
    (unchecked-byte x))

  (method ^:public ^:static uncheckedByteCast ^byte [^long x]
    (unchecked-byte x))

  (method ^:public ^:static uncheckedByteCast ^byte [^float x]
    (unchecked-byte x))

  (method ^:public ^:static uncheckedByteCast ^byte [^double x]
    (unchecked-byte x))

  (method ^:public ^:static uncheckedShortCast ^short [x]
    (.shortValue (cast Number x)))

  (method ^:public ^:static uncheckedShortCast ^short [^byte x] x)

  (method ^:public ^:static uncheckedShortCast ^short [^short x] x)

  (method ^:public ^:static uncheckedShortCast ^short [^int x]
    (unchecked-short x))

  (method ^:public ^:static uncheckedShortCast ^short [^long x]
    (unchecked-short x))

  (method ^:public ^:static uncheckedShortCast ^short [^float x]
    (unchecked-short x))

  (method ^:public ^:static uncheckedShortCast ^short [^double x]
    (unchecked-short x))

  (method ^:public ^:static uncheckedCharCast ^char [x]
    (if (instance? Character x)
        (.charValue (cast Character x))
        (unchecked-char (.longValue (cast Number x)))))

  (method ^:public ^:static uncheckedCharCast ^char [^byte x]
    (unchecked-char x))

  (method ^:public ^:static uncheckedCharCast ^char [^short x]
    (unchecked-char x))

  (method ^:public ^:static uncheckedCharCast ^char [^char x] x)

  (method ^:public ^:static uncheckedCharCast ^char [^int x]
    (unchecked-char x))

  (method ^:public ^:static uncheckedCharCast ^char [^long x]
    (unchecked-char x))

  (method ^:public ^:static uncheckedCharCast ^char [^float x]
    (unchecked-char x))

  (method ^:public ^:static uncheckedCharCast ^char [^double x]
    (unchecked-char x))

  (method ^:public ^:static uncheckedIntCast ^int [x]
    (if (instance? Number x) (.intValue (cast Number x)) (.charValue (cast Character x))))

  (method ^:public ^:static uncheckedIntCast ^int [^byte x] x)

  (method ^:public ^:static uncheckedIntCast ^int [^short x] x)

  (method ^:public ^:static uncheckedIntCast ^int [^char x] x)

  (method ^:public ^:static uncheckedIntCast ^int [^int x] x)

  (method ^:public ^:static uncheckedIntCast ^int [^long x]
    (unchecked-int x))

  (method ^:public ^:static uncheckedIntCast ^int [^float x]
    (unchecked-int x))

  (method ^:public ^:static uncheckedIntCast ^int [^double x]
    (unchecked-int x))

  (method ^:public ^:static uncheckedLongCast ^long [x]
    (.longValue (cast Number x)))

  (method ^:public ^:static uncheckedLongCast ^long [^byte x] x)

  (method ^:public ^:static uncheckedLongCast ^long [^short x] x)

  (method ^:public ^:static uncheckedLongCast ^long [^int x] x)

  (method ^:public ^:static uncheckedLongCast ^long [^long x] x)

  (method ^:public ^:static uncheckedLongCast ^long [^float x]
    (unchecked-long x))

  (method ^:public ^:static uncheckedLongCast ^long [^double x]
    (unchecked-long x))

  (method ^:public ^:static uncheckedFloatCast ^float [x]
    (.floatValue (cast Number x)))

  (method ^:public ^:static uncheckedFloatCast ^float [^byte x] x)

  (method ^:public ^:static uncheckedFloatCast ^float [^short x] x)

  (method ^:public ^:static uncheckedFloatCast ^float [^int x] x)

  (method ^:public ^:static uncheckedFloatCast ^float [^long x] x)

  (method ^:public ^:static uncheckedFloatCast ^float [^float x] x)

  (method ^:public ^:static uncheckedFloatCast ^float [^double x]
    (unchecked-float x))

  (method ^:public ^:static uncheckedDoubleCast ^double [x]
    (.doubleValue (cast Number x)))

  (method ^:public ^:static uncheckedDoubleCast ^double [^byte x] x)

  (method ^:public ^:static uncheckedDoubleCast ^double [^short x] x)

  (method ^:public ^:static uncheckedDoubleCast ^double [^int x] x)

  (method ^:public ^:static uncheckedDoubleCast ^double [^long x] x)

  (method ^:public ^:static uncheckedDoubleCast ^double [^float x] x)

  (method ^:public ^:static uncheckedDoubleCast ^double [^double x] x)

  (method ^:public ^:static map ^IPersistentMap [& ^Object/1 init]
    (cond
      (or (nil? init) (== (alength init) 0)) PersistentArrayMap/EMPTY
      (PersistentArrayMap/canBePAM init) (PersistentArrayMap/createWithCheck init)
      :else (PersistentHashMap/createWithCheck init)))

  (method ^:public ^:static mapUniqueKeys ^IPersistentMap [& ^Object/1 init]
    (cond
      (nil? init) PersistentArrayMap/EMPTY
      (PersistentArrayMap/canBePAM init) (PersistentArrayMap. init)
      :else (PersistentHashMap/create init)))

  (method ^:public ^:static set ^IPersistentSet [& ^Object/1 init]
    (PersistentHashSet/createWithCheck init))

  (method ^:public ^:static vector ^IPersistentVector [& ^Object/1 init]
    (LazilyPersistentVector/createOwning init))

  (method ^:public ^:static subvec ^IPersistentVector [^IPersistentVector v ^int start ^int end]
    (when (or (or (< end start) (< start 0)) (> end (.count v)))
      (throw (IndexOutOfBoundsException.)))
    (if (== start end) PersistentVector/EMPTY (APersistentVector$SubVector. nil v start end)))

  (method ^:public ^:static list ^ISeq [] nil)

  (method ^:public ^:static list ^ISeq [arg1] (PersistentList. arg1))

  (method ^:public ^:static list ^ISeq [arg1 arg2]
    (RT/listStar arg1 arg2 nil))

  (method ^:public ^:static list ^ISeq [arg1 arg2 arg3]
    (RT/listStar arg1 arg2 arg3 nil))

  (method ^:public ^:static list ^ISeq [arg1 arg2 arg3 arg4]
    (RT/listStar arg1 arg2 arg3 arg4 nil))

  (method ^:public ^:static list ^ISeq [arg1 arg2 arg3 arg4 arg5]
    (RT/listStar arg1 arg2 arg3 arg4 arg5 nil))

  (method ^:public ^:static listStar ^ISeq [arg1 ^ISeq rest]
    (RT/cons arg1 rest))

  (method ^:public ^:static listStar ^ISeq [arg1 arg2 ^ISeq rest]
    (RT/cons arg1 (RT/cons arg2 rest)))

  (method ^:public ^:static listStar ^ISeq [arg1 arg2 arg3 ^ISeq rest]
    (RT/cons arg1 (RT/cons arg2 (RT/cons arg3 rest))))

  (method ^:public ^:static listStar ^ISeq [arg1 arg2 arg3 arg4 ^ISeq rest]
    (RT/cons arg1 (RT/cons arg2 (RT/cons arg3 (RT/cons arg4 rest)))))

  (method ^:public ^:static listStar ^ISeq [arg1 arg2 arg3 arg4 arg5 ^ISeq rest]
    (RT/cons arg1 (RT/cons arg2 (RT/cons arg3 (RT/cons arg4 (RT/cons arg5 rest))))))

  (method ^:public ^:static arrayToList ^ISeq [^Object/1 a]
    (let [^:mutable ^ISeq ret nil]
      (loop [^int i (unchecked-subtract-int (alength a) 1)]
        (when (>= i 0) (set! ret (RT/cons (aget a i) ret)) (recur (unchecked-dec-int i))))
      ret))

  (method ^:public ^:static object_array ^Object/1 [sizeOrSeq]
    (if (instance? Number sizeOrSeq)
        (new Object/1 (.intValue (cast Number sizeOrSeq)))
        (let [^:mutable s (RT/seq sizeOrSeq)
              size (RT/count s)
              ret (new Object/1 size)]
          (let [^:mutable ^int i 0]
            (while (and (< i size) (some? s))
              (aset ret i (.first s))
              (set! i (unchecked-inc-int i))
              (set! s (.next s))))
          ret)))

  (method ^:public ^:static toArray ^Object/1 [coll]
    (cond
      (nil? coll) EMPTY_ARRAY
      (instance? Object/1 coll) (cast Object/1 coll)
      (instance? Collection coll) (.toArray (cast Collection coll))
      (instance? Iterable coll)
        (let [ret (ArrayList.)] (for-each [o (cast Iterable coll)] (.add ret o)) (.toArray ret))
      (instance? Map coll) (.toArray (.entrySet (cast Map coll)))
      (instance? String coll)
        (let [chars (.toCharArray (cast String coll))
              ret (new Object/1 (alength chars))]
          (loop [^int i 0]
            (when (< i (alength chars)) (aset ret i (aget chars i)) (recur (unchecked-inc-int i))))
          ret)
      (.isArray (.getClass coll))
        (let [^:mutable s (RT/seq coll)
              ret (new Object/1 (RT/count s))]
          (let [^:mutable ^int i 0]
            (while (< i (alength ret))
              (aset ret i (.first s))
              (set! i (unchecked-inc-int i))
              (set! s (.next s))))
          ret)
      :else
        (throw (Util/runtimeException
                 (java-str "Unable to convert: " (.getClass coll) " to Object[]")))))

  (method ^:public ^:static seqToArray ^Object/1 [^:mutable ^ISeq seq]
    (let [len (RT/length seq)
          ret (new Object/1 len)]
      (let [^:mutable ^int i 0]
        (while (some? seq)
          (aset ret i (.first seq))
          (set! i (unchecked-inc-int i))
          (set! seq (.next seq))))
      ret))

  (method ^:public ^:static seqToPassedArray ^Object/1 [^:mutable ^ISeq seq ^Object/1 passed]
    (let [^:mutable dest passed
          len (RT/count seq)]
      (when (> len (alength dest))
        (set! dest (cast Object/1 (Array/newInstance (.getComponentType (.getClass passed)) len))))
      (let [^:mutable ^int i 0]
        (while (some? seq)
          (aset dest i (.first seq))
          (set! i (unchecked-inc-int i))
          (set! seq (.next seq))))
      (when (< len (alength passed)) (aset dest len nil))
      dest))

  (method ^:public ^:static seqToTypedArray [^ISeq seq]
    (let [type (if (and (some? seq) (some? (.first seq))) (.getClass (.first seq)) Object)]
      (RT/seqToTypedArray type seq)))

  (method ^:public ^:static seqToTypedArray [^Class type ^:mutable ^ISeq seq]
    (let [ret (Array/newInstance type (RT/length seq))]
      (cond
        (identical? type Integer/TYPE)
          (let [^:mutable ^int i 0]
            (while (some? seq)
              (Array/set ret i (RT/intCast (.first seq)))
              (set! i (unchecked-inc-int i))
              (set! seq (.next seq))))
        (identical? type Byte/TYPE)
          (let [^:mutable ^int i 0]
            (while (some? seq)
              (Array/set ret i (RT/byteCast (.first seq)))
              (set! i (unchecked-inc-int i))
              (set! seq (.next seq))))
        (identical? type Float/TYPE)
          (let [^:mutable ^int i 0]
            (while (some? seq)
              (Array/set ret i (RT/floatCast (.first seq)))
              (set! i (unchecked-inc-int i))
              (set! seq (.next seq))))
        (identical? type Short/TYPE)
          (let [^:mutable ^int i 0]
            (while (some? seq)
              (Array/set ret i (RT/shortCast (.first seq)))
              (set! i (unchecked-inc-int i))
              (set! seq (.next seq))))
        (identical? type Character/TYPE)
          (let [^:mutable ^int i 0]
            (while (some? seq)
              (Array/set ret i (RT/charCast (.first seq)))
              (set! i (unchecked-inc-int i))
              (set! seq (.next seq))))
        :else
          (let [^:mutable ^int i 0]
            (while (some? seq)
              (Array/set ret i (.first seq))
              (set! i (unchecked-inc-int i))
              (set! seq (.next seq)))))
      ret))

  (method ^:public ^:static length ^int [^ISeq list]
    (let [^:mutable ^int i 0]
      (loop [c list] (when (some? c) (set! i (unchecked-inc-int i)) (recur (.next c))))
      i))

  (method ^:public ^:static boundedLength ^int [^ISeq list ^int limit]
    (let [^:mutable ^int i 0]
      (loop [c list]
        (when (and (some? c) (<= i limit)) (set! i (unchecked-inc-int i)) (recur (.next c))))
      i))

  (method ^:static readRet ^Character [^int ret]
    (when-not (== ret -1) (^[char] RT/box (unchecked-char ret))))

  (method ^:public ^:static readChar :throws [IOException] ^Character [^Reader r]
    (let [ret (.read r)] (RT/readRet ret)))

  (method ^:public ^:static peekChar :throws [IOException] ^Character [^Reader r]
    (let [^:mutable ^int ret 0]
      (if (instance? PushbackReader r)
          (do (set! ret (.read r)) (.unread (cast PushbackReader r) ret))
          (do (.mark r 1) (set! ret (.read r)) (.reset r)))
      (RT/readRet ret)))

  (method ^:public ^:static getLineNumber ^int [^Reader r]
    (if (instance? LineNumberingPushbackReader r)
        (.getLineNumber (cast LineNumberingPushbackReader r))
        0))

  (method ^:public ^:static getColumnNumber ^int [^Reader r]
    (if (instance? LineNumberingPushbackReader r)
        (.getColumnNumber (cast LineNumberingPushbackReader r))
        0))

  (method ^:public ^:static getLineNumberingReader ^LineNumberingPushbackReader [^Reader r]
    (if (RT/isLineNumberingReader r)
        (cast LineNumberingPushbackReader r)
        (LineNumberingPushbackReader. r)))

  (method ^:public ^:static isLineNumberingReader ^boolean [^Reader r]
    (instance? LineNumberingPushbackReader r))

  (method ^:public ^:static isReduced ^boolean [r] (instance? Reduced r))

  (method ^:public ^:static resolveClassNameInContext ^String [^String className]
    className)

  (method ^:public ^:static suppressRead ^boolean []
    (RT/booleanCast (.deref SUPPRESS_READ)))

  (method ^:public ^:static printString ^String [x]
    (try
      (let [sw (StringWriter.)] (RT/print x sw) (.toString sw))
      (catch Exception e (throw (Util/sneakyThrow e)))))

  (method ^:public ^:static readString [^String s] (RT/readString s nil))

  (method ^:public ^:static readString [^String s opts]
    (let [r (PushbackReader. (StringReader. s))] (LispReader/read r opts)))

  (method ^:public ^:static print :throws [IOException] ^void [x ^Writer w]
    (if (and (.isBound PRINT_INITIALIZED) (RT/booleanCast (.deref PRINT_INITIALIZED)))
        (.invoke PR_ON x w)
        (let [readably (RT/booleanCast (.deref PRINT_READABLY))]
          (when (instance? Obj x)
            (let [o (cast Obj x)]
              (when (and (> (RT/count (.meta o)) 0)
                         (or (and readably (RT/booleanCast (.deref PRINT_META)))
                             (RT/booleanCast (.deref PRINT_DUP))))
                (let [meta (.meta o)]
                  (.write w "#^")
                  (if (and (== (.count meta) 1) (.containsKey meta TAG_KEY))
                      (RT/print (.valAt meta TAG_KEY) w)
                      (RT/print meta w))
                  (.write w \space)))))
          (cond
            (nil? x) (.write w "nil")
            (or (instance? ISeq x) (instance? IPersistentList x))
              (do (.write w \() (RT/printInnerSeq (RT/seq x) w) (.write w \)))
            (instance? String x)
              (let [s (cast String x)]
                (if (not readably)
                    (.write w s)
                    (do
                      (.write w \")
                      (loop [^int i 0]
                        (when (< i (.length s))
                          (let [c (.charAt s i)]
                            (switch c
                              \newline (do (.write w "\\n") (recur (unchecked-inc-int i)))
                              \tab (do (.write w "\\t") (recur (unchecked-inc-int i)))
                              \return (do (.write w "\\r") (recur (unchecked-inc-int i)))
                              \" (do (.write w "\\\"") (recur (unchecked-inc-int i)))
                              \\ (do (.write w "\\\\") (recur (unchecked-inc-int i)))
                              \formfeed (do (.write w "\\f") (recur (unchecked-inc-int i)))
                              \backspace (do (.write w "\\b") (recur (unchecked-inc-int i)))
                              (do (.write w c) (recur (unchecked-inc-int i)))))))
                      (.write w \"))))
            (instance? IPersistentMap x)
              (do
                (.write w \{)
                (loop [s (RT/seq x)]
                  (when (some? s)
                    (let [e (cast IMapEntry (.first s))]
                      (RT/print (.key e) w)
                      (.write w \space)
                      (RT/print (.val e) w)
                      (if (some? (.next s))
                          (do (.write w ", ") (recur (.next s)))
                          (recur (.next s))))))
                (.write w \}))
            (instance? IPersistentVector x)
              (let [a (cast IPersistentVector x)]
                (.write w \[)
                (loop [^int i 0]
                  (when (< i (.count a))
                    (RT/print (.nth a i) w)
                    (if (< i (unchecked-subtract-int (.count a) 1))
                        (do (.write w \space) (recur (unchecked-inc-int i)))
                        (recur (unchecked-inc-int i)))))
                (.write w \]))
            (instance? IPersistentSet x)
              (do
                (.write w "#{")
                (loop [s (RT/seq x)]
                  (when (some? s)
                    (RT/print (.first s) w)
                    (if (some? (.next s)) (do (.write w " ") (recur (.next s))) (recur (.next s)))))
                (.write w \}))
            (instance? Character x)
              (let [c (.charValue (cast Character x))]
                (if (not readably)
                    (.write w c)
                    (do
                      (.write w \\)
                      (switch c
                        \newline (.write w "newline")
                        \tab (.write w "tab")
                        \space (.write w "space")
                        \backspace (.write w "backspace")
                        \formfeed (.write w "formfeed")
                        \return (.write w "return")
                        (.write w c)))))
            (instance? Class x) (do (.write w "#=") (.write w (.getName (cast Class x))))
            (and (instance? BigDecimal x) readably) (do (.write w (.toString x)) (.write w \M))
            (and (instance? BigInt x) readably) (do (.write w (.toString x)) (.write w \N))
            (and (instance? BigInteger x) readably)
              (do (.write w (.toString x)) (.write w "BIGINT"))
            (instance? Var x)
              (let [v (cast Var x)]
                (.write w (java-str "#=(var " (.-name (.-ns v)) "/" (.-sym v) ")")))
            (instance? Pattern x)
              (let [p (cast Pattern x)] (.write w (java-str "#\"" (.pattern p) "\"")))
            :else (.write w (.toString x))))))

  (method ^:private ^:static printInnerSeq :throws [IOException] ^void [^ISeq x ^Writer w]
    (loop [s x]
      (when (some? s)
        (RT/print (.first s) w)
        (if (some? (.next s)) (do (.write w \space) (recur (.next s))) (recur (.next s))))))

  (method ^:public ^:static formatAesthetic :throws [IOException] ^void [^Writer w obj]
    (if (nil? obj) (.write w "null") (.write w (.toString obj))))

  (method ^:public ^:static formatStandard :throws [IOException] ^void [^Writer w obj]
    (cond
      (nil? obj) (.write w "null")
      (instance? String obj) (do (.write w \") (.write w (cast String obj)) (.write w \"))
      (instance? Character obj)
        (do
          (.write w \\)
          (let [c (.charValue (cast Character obj))]
            (switch c
              \newline (.write w "newline")
              \tab (.write w "tab")
              \space (.write w "space")
              \backspace (.write w "backspace")
              \formfeed (.write w "formfeed")
              (.write w c))))
      :else (.write w (.toString obj))))

  (method ^:public ^:static format :throws [IOException] [o ^String s & ^Object/1 args]
    (let [^Writer w (cond
                      (nil? o) (StringWriter.)
                      (Util/equals o T) (cast Writer (.deref OUT))
                      :else (cast Writer o))]
      (RT/doFormat w s (ArraySeq/create args))
      (when (nil? o) (.toString w))))

  (method ^:public ^:static doFormat :throws [IOException] ^ISeq [^Writer w ^String s
                                                                  ^:mutable ^ISeq args]
    (let [^:mutable ^int i 0]
      (while (< i (.length s))
        (let [c (.charAt s (let [old-2 i] (set! i (unchecked-inc-int i)) old-2))]
          (switch (Character/toLowerCase c)
            \~
              (let [d (.charAt s (let [old-3 i] (set! i (unchecked-inc-int i)) old-3))]
                (switch (Character/toLowerCase d)
                  \% (.write w \newline)
                  \t (.write w \tab)
                  \a
                    (do
                      (when (nil? args) (throw (IllegalArgumentException. "Missing argument")))
                      (RT/formatAesthetic w (RT/first args))
                      (set! args (RT/next args)))
                  \s
                    (do
                      (when (nil? args) (throw (IllegalArgumentException. "Missing argument")))
                      (RT/formatStandard w (RT/first args))
                      (set! args (RT/next args)))
                  \{
                    (let [j (.indexOf s "~}" i)]
                      (when (== j -1) (throw (IllegalArgumentException. "Missing ~}")))
                      (let [subs (.substring s i j)]
                        (let [^:mutable sargs (RT/seq (RT/first args))]
                          (while (some? sargs) (set! sargs (RT/doFormat w subs sargs))))
                        (set! args (RT/next args))
                        (set! i (unchecked-add-int j 2))))
                  \^ (when (nil? args) (return nil))
                  \~ (.write w \~)
                  (throw (IllegalArgumentException. (java-str "Unsupported ~ directive: " d)))))
            (.write w c)))))
    args)

  (method ^:public ^:static setValues ^Object/1 [& ^Object/1 vals]
    (when (> (alength vals) 0) vals))

  (method ^:public ^:static makeClassLoader ^ClassLoader []
    (try
      (Var/pushThreadBindings (^[Object/1] RT/map USE_CONTEXT_CLASSLOADER RT/T))
      (DynamicClassLoader. (RT/baseLoader))
      (finally (Var/popThreadBindings))))

  (method ^:public ^:static baseLoader ^ClassLoader []
    (cond
      (.isBound arbace.lang.Compiler/LOADER) (cast ClassLoader (.deref arbace.lang.Compiler/LOADER))
      (RT/booleanCast (.deref USE_CONTEXT_CLASSLOADER))
        (.getContextClassLoader (Thread/currentThread))
      :else (.getClassLoader arbace.lang.Compiler)))

  (method ^:public ^:static resourceAsStream ^InputStream [^ClassLoader loader ^String name]
    (if (nil? loader)
        (ClassLoader/getSystemResourceAsStream name)
        (.getResourceAsStream loader name)))

  (method ^:public ^:static getResource ^URL [^ClassLoader loader ^String name]
    (if (nil? loader) (ClassLoader/getSystemResource name) (.getResource loader name)))

  (method ^:public ^:static classForName ^Class [^String name ^boolean load ^ClassLoader loader]
    (try
      (let [^:mutable ^Class c nil]
        (when-not (instance? DynamicClassLoader loader)
          (set! c (DynamicClassLoader/findInMemoryClass name)))
        (if (some? c) c (Class/forName name load loader)))
      (catch ClassNotFoundException e (throw (Util/sneakyThrow e)))))

  (method ^:public ^:static classForName ^Class [^String name]
    (RT/classForName name true (RT/baseLoader)))

  (method ^:public ^:static classForNameNonLoading ^Class [^String name]
    (RT/classForName name false (RT/baseLoader)))

  (method ^:public ^:static loadClassForName ^Class [^String name]
    (try
      (RT/classForNameNonLoading name)
      (catch Exception e
        (if (instance? ClassNotFoundException e) (return nil) (throw (Util/sneakyThrow e)))))
    (RT/classForName name))

  (method ^:public ^:static aget ^float [^float/1 xs ^int i] (aget xs i))

  (method ^:public ^:static aset ^float [^float/1 xs ^int i ^float v]
    (aset xs i v)
    v)

  (method ^:public ^:static alength ^int [^float/1 xs] (alength xs))

  (method ^:public ^:static aclone ^float/1 [^float/1 xs] (.clone xs))

  (method ^:public ^:static aget ^double [^double/1 xs ^int i]
    (aget xs i))

  (method ^:public ^:static aset ^double [^double/1 xs ^int i ^double v]
    (aset xs i v)
    v)

  (method ^:public ^:static alength ^int [^double/1 xs] (alength xs))

  (method ^:public ^:static aclone ^double/1 [^double/1 xs] (.clone xs))

  (method ^:public ^:static aget ^int [^int/1 xs ^int i] (aget xs i))

  (method ^:public ^:static aset ^int [^int/1 xs ^int i ^int v]
    (aset xs i v)
    v)

  (method ^:public ^:static alength ^int [^int/1 xs] (alength xs))

  (method ^:public ^:static aclone ^int/1 [^int/1 xs] (.clone xs))

  (method ^:public ^:static aget ^long [^long/1 xs ^int i] (aget xs i))

  (method ^:public ^:static aset ^long [^long/1 xs ^int i ^long v]
    (aset xs i v)
    v)

  (method ^:public ^:static alength ^int [^long/1 xs] (alength xs))

  (method ^:public ^:static aclone ^long/1 [^long/1 xs] (.clone xs))

  (method ^:public ^:static aget ^char [^char/1 xs ^int i] (aget xs i))

  (method ^:public ^:static aset ^char [^char/1 xs ^int i ^char v]
    (aset xs i v)
    v)

  (method ^:public ^:static alength ^int [^char/1 xs] (alength xs))

  (method ^:public ^:static aclone ^char/1 [^char/1 xs] (.clone xs))

  (method ^:public ^:static aget ^byte [^byte/1 xs ^int i] (aget xs i))

  (method ^:public ^:static aset ^byte [^byte/1 xs ^int i ^byte v]
    (aset xs i v)
    v)

  (method ^:public ^:static alength ^int [^byte/1 xs] (alength xs))

  (method ^:public ^:static aclone ^byte/1 [^byte/1 xs] (.clone xs))

  (method ^:public ^:static aget ^short [^short/1 xs ^int i] (aget xs i))

  (method ^:public ^:static aset ^short [^short/1 xs ^int i ^short v]
    (aset xs i v)
    v)

  (method ^:public ^:static alength ^int [^short/1 xs] (alength xs))

  (method ^:public ^:static aclone ^short/1 [^short/1 xs] (.clone xs))

  (method ^:public ^:static aget ^boolean [^boolean/1 xs ^int i]
    (aget xs i))

  (method ^:public ^:static aset ^boolean [^boolean/1 xs ^int i ^boolean v]
    (aset xs i v)
    v)

  (method ^:public ^:static alength ^int [^boolean/1 xs] (alength xs))

  (method ^:public ^:static aclone ^boolean/1 [^boolean/1 xs] (.clone xs))

  (method ^:public ^:static aget [^Object/1 xs ^int i] (aget xs i))

  (method ^:public ^:static aset [^Object/1 xs ^int i v] (aset xs i v) v)

  (method ^:public ^:static alength ^int [^Object/1 xs] (alength xs))

  (method ^:public ^:static aclone ^Object/1 [^Object/1 xs] (.clone xs)))
