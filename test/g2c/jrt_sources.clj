(ns g2c.jrt-sources
  "jrt's translated input (B1a step 3, doc/go/JRT-SOURCES.md): the Java source files of the
  translation closure measured by bin/java-surface (doc/go/java-surface.edn, :closure :repl
  :sources), converted by j2c into class forms. Run by bin/jrt-convert, which also generates
  the JDK build's generated sources, converts, compiles and compares; this namespace holds the
  parts written in Clojure:

  - `sources`: the closure's source files, as paths relative to jdk26u;
  - `edge`: what the translated classes (whole files, compiled) call outside themselves (the
    edge jrt hand-writes), with the natives and the VM intrinsics they declare, cross-checked
    against the closure's boundary (reached methods only) recorded by bin/java-surface;
  - `report`: the counts of a run, from class-forms-check's results.

  Outputs are deterministic: sorted, without times."
  (:require [arbace.java.io :as io]
            [arbace.string :as str]
            [arbace.pprint :as pp]
            [g2c.java-surface :as js])
  (:import (java.lang.classfile ClassFile ClassModel MethodModel FieldModel Attributes Annotation)
           (java.lang.classfile.attribute RuntimeVisibleAnnotationsAttribute)
           (java.io File)
           (java.nio.file Files)))

(defn- read-data [f] (binding [*read-eval* false] (read-string (slurp f))))

;; ---------------------------------------------------------------------------------------
;; the closure's sources

(def added-sources
  "Files of src/java.base/share/classes added to the measured closure (B1a step 4, phase 2C,
  doc/go/JRT-NOTES.md \"Phase 2C\"): plain Java that jrt had as stand-ins (C2G-SPEC §4.3) or
  that the standard streams need, translated by c2g like the rest of the closure."
  (vec (sort
         (concat
           ;; the 26 stand-ins of jrt whose classes the measured closure did not hold: exceptions
           ;; and interfaces with nothing but constructors, fields and declarations
           (map #(str "java/" % ".java")
                ["io/Serializable" "io/UnsupportedEncodingException"
                 "lang/AbstractMethodError" "lang/ArrayStoreException" "lang/CloneNotSupportedException"
                 "lang/Cloneable" "lang/IllegalAccessException" "lang/IllegalMonitorStateException"
                 "lang/IllegalThreadStateException" "lang/InstantiationException" "lang/InterruptedException"
                 "lang/NegativeArraySizeException" "lang/NoClassDefFoundError" "lang/NoSuchFieldException"
                 "lang/NoSuchMethodException" "lang/Runnable" "lang/StackOverflowError"
                 "lang/reflect/InvocationTargetException" "lang/reflect/Type"
                 "nio/charset/IllegalCharsetNameException" "nio/charset/UnsupportedCharsetException"
                 "util/concurrent/CancellationException" "util/concurrent/ExecutionException"
                 "util/concurrent/RejectedExecutionException" "util/concurrent/TimeoutException"
                 "util/function/Supplier"])
           ;; the standard streams (System.out, err, in): PrintStream and the interfaces of the
           ;; closure's streams and writers
           (map #(str "java/" % ".java")
                ["io/PrintStream" "io/Closeable" "io/Flushable" "lang/AutoCloseable"])
           ;; BigInteger's modInverse and modPow with a negative exponent (the oracle's
           ;; BigNumbers script)
           ["java/math/SignedMutableBigInteger.java"]
           ;; B1a step 5, phase 2B (EVAL-NOTES.md): JDK 21's sequenced collections, which the
           ;; closure's List, Deque, SortedSet, SortedMap, LinkedHashSet and LinkedHashMap
           ;; implement (LinkedHashMap's views are SequencedSets), their reverse-order views and
           ;; SequencedMap's entry holder
           (map #(str "java/util/" % ".java")
                ["SequencedCollection" "SequencedSet" "SequencedMap" "ReverseOrderDequeView"
                 "ReverseOrderSortedSetView" "ReverseOrderSortedMapView"])
           ["jdk/internal/util/NullableKeyValueHolder.java"]
           ;; the constant API's two interfaces, which String and the wrapper classes implement
           ;; (supers, bases and ancestors list them); their methods name classes outside the
           ;; world (Optional, MethodHandles.Lookup) and do not exist in Go
           (map #(str "java/lang/constant/" % ".java") ["Constable" "ConstantDesc"])
           ;; streams (String's lines, chars, codePoints; Collectors; core's stream-reduce!
           ;; and the like): java.util.stream whole, the functional interfaces, Optional and
           ;; its primitive kin, the summary statistics, StringJoiner, Spliterator,
           ;; PrimitiveIterator, and CountedCompleter, the parallel tasks' superclass (whose
           ;; ForkJoinTask internals are jrt's; parallel streams run on jrt's pool)
           (map #(str "java/util/stream/" % ".java")
                ["AbstractPipeline" "AbstractShortCircuitTask" "AbstractSpinedBuffer" "AbstractTask"
                 "BaseStream" "Collector" "Collectors" "DistinctOps" "DoublePipeline" "DoubleStream"
                 "FindOps" "ForEachOps" "Gatherer" "GathererOp" "Gatherers" "IntPipeline" "IntStream"
                 "LongPipeline" "LongStream" "MatchOps" "Node" "Nodes" "PipelineHelper" "ReduceOps"
                 "ReferencePipeline" "Sink" "SliceOps" "SortedOps" "SpinedBuffer" "Stream"
                 "StreamOpFlag" "StreamShape" "StreamSpliterators" "StreamSupport" "Streams"
                 "TerminalOp" "TerminalSink" "Tripwire" "WhileOps"])
           (map #(str "java/util/function/" % ".java")
                ["BiConsumer" "BiFunction" "BiPredicate" "BinaryOperator" "BooleanSupplier" "Consumer"
                 "DoubleBinaryOperator" "DoubleConsumer" "DoubleFunction" "DoublePredicate"
                 "DoubleSupplier" "DoubleToIntFunction" "DoubleToLongFunction" "DoubleUnaryOperator"
                 "Function" "IntBinaryOperator" "IntConsumer" "IntFunction" "IntPredicate" "IntSupplier"
                 "IntToDoubleFunction" "IntToLongFunction" "IntUnaryOperator" "LongBinaryOperator"
                 "LongConsumer" "LongFunction" "LongPredicate" "LongSupplier" "LongToDoubleFunction"
                 "LongToIntFunction" "LongUnaryOperator" "ObjDoubleConsumer" "ObjIntConsumer"
                 "ObjLongConsumer" "Predicate" "Supplier" "ToDoubleBiFunction" "ToDoubleFunction"
                 "ToIntBiFunction" "ToIntFunction" "ToLongBiFunction" "ToLongFunction" "UnaryOperator"])
           (map #(str "java/util/" % ".java")
                ["Optional" "OptionalInt" "OptionalLong" "OptionalDouble" "IntSummaryStatistics"
                 "LongSummaryStatistics" "DoubleSummaryStatistics" "StringJoiner" "Spliterator"
                 "PrimitiveIterator"])
           ["java/util/concurrent/CountedCompleter.java"
            ;; Collectors' concurrent collectors cast to it
            "java/util/concurrent/ConcurrentMap.java"
            ;; Collectors' characteristics sets
            "java/util/EnumSet.java" "java/util/RegularEnumSet.java" "java/util/JumboEnumSet.java"
            ;; StreamOpFlag's flag maps; the spliterators' debugging trip wire
            "java/util/EnumMap.java" "java/util/Tripwire.java"
            ;; the interface Random implements (Collections.shuffle takes one)
            "java/util/random/RandomGenerator.java"]
           ;; Clojure's test suite on the Go build (step 5 phase 2): uri? (java.net.URI's
           ;; parser), the delays' tests (CyclicBarrier, over jrt's ReentrantLock)
           ["java/net/URI.java" "java/net/URISyntaxException.java"
            "java/util/concurrent/CyclicBarrier.java" "java/util/concurrent/BrokenBarrierException.java"]
           ;; java.time.Instant for the REPL (#inst, Date.toInstant; JRT-NOTES.md, phase 2B
           ;; \"Dates\"): Instant and the exception it throws; the rest of java.time stays
           ;; outside the world (its uses in Instant are operation-level stubs, toString a
           ;; variant)
           ["java/time/Instant.java" "java/time/DateTimeException.java"]
           ;; sockets and the socket REPL (arbace.core.server; JRT-NOTES.md, "Sockets"): the plain
           ;; Java of java.net's sockets and addresses, over jrt's own HostSocketImpl
           ;; (overlay/jdk) in place of sun.nio.ch.NioSocketImpl; the name service through
           ;; Inet6AddressImpl's natives (variants over the host's). NetworkInterface, Proxy, the
           ;; SOCKS and HTTP-tunnel impls and the resolver SPI's providers stay outside the world
           (map #(str "java/net/" % ".java")
                ["ServerSocket" "Socket" "SocketImpl" "SocketImplFactory" "SocketAddress"
                 "InetSocketAddress" "InetAddress" "Inet4Address" "Inet6Address" "InetAddressImpl"
                 "Inet4AddressImpl" "Inet6AddressImpl" "SocketOptions" "SocketOption"
                 "StandardSocketOptions" "SocketException" "BindException" "ConnectException"
                 "NoRouteToHostException" "UnknownHostException" "SocketTimeoutException"
                 "spi/InetAddressResolver"])
           ;; (IPAddressUtil, Exceptions and Hashtable, which System.getProperties()'s Properties
           ;; extends, are file-sources')
           ["java/io/InterruptedIOException.java" "sun/net/PlatformSocketImpl.java"]
           ;; the regex flag CANON_EQ (JRT-NOTES.md, "The JDK's resource data"): java.text's
           ;; Normalizer over jdk.internal.icu's normalizer, whose loader reads ICU's nfc.nrm and
           ;; nfkc.nrm through java.nio's heap buffers (the buffers' generated files are
           ;; added-gensrc's); ScopedMemoryAccess's heap paths and the two foreign memory types
           ;; the buffers' constructors and accessors name
           (map #(str "java/text/" % ".java") ["Normalizer" "CharacterIterator"])
           (map #(str "jdk/internal/icu/" % ".java")
                ["text/NormalizerBase" "text/UTF16" "text/UCharacterIterator" "text/Replaceable"
                 "text/ReplaceableString" "impl/UCharacterProperty" "impl/CharacterIteratorWrapper"
                 "impl/ReplaceableUCharacterIterator" "impl/Trie2" "impl/Trie2_16"
                 "util/OutputInt"])
           (map #(str "java/nio/" % ".java")
                ["Buffer" "ByteOrder" "StringCharBuffer" "BufferUnderflowException" "BufferOverflowException"
                 "ReadOnlyBufferException" "InvalidMarkException"])
           ["java/lang/foreign/MemorySegment.java" "jdk/internal/foreign/MemorySessionImpl.java"]
           ;; \N{name} (CharacterName's uniName.dat is zlib-compressed: the overlay's
           ;; InflaterInputStream throws ZipException)
           ["java/util/zip/ZipException.java"]))))

(def added-gensrc
  "Files the JDK build generates (make/modules/java.base/gensrc/GensrcBuffer.gmk,
  GensrcScopedMemoryAccess.gmk) added to the closure, as paths below gensrc/java.base: the heap
  byte, char and int buffers and their views of a byte buffer in either byte order (ICU's data
  loader: JRT-NOTES.md, \"The JDK's resource data\"), and ScopedMemoryAccess, through which the
  buffers reach Unsafe. bin/jrt-convert generates them as the JDK build does."
  (vec (concat (map #(str "java/nio/" % ".java")
                    ["ByteBuffer" "CharBuffer" "IntBuffer" "HeapByteBuffer" "HeapCharBuffer"
                     "HeapIntBuffer" "ByteBufferAsCharBufferB" "ByteBufferAsCharBufferL"
                     "ByteBufferAsIntBufferB" "ByteBufferAsIntBufferL"])
               ["jdk/internal/misc/ScopedMemoryAccess.java"])))

(def util-sources
  "Files of src/java.base/share/classes added for java.util's plain Java (doc/go/JAVA-BASE.md,
  amendment JB1; the user's decision of 2026-10-10): first the files whose absence made
  translated members throw (operation-level stubs), then the plain Java of java.util that
  Clojure users reach for. java.time and java.util.concurrent are other branches'."
  (vec (sort
         (concat
           ;; Arrays.sort of the primitive arrays (and its parallel sorts on jrt's pool),
           ;; parallelSort of objects, parallelPrefix
           (map #(str "java/util/" % ".java")
                ["DualPivotQuicksort" "ArraysParallelSortHelpers" "ArrayPrefixHelpers"
                 ;; Comparator.naturalOrder, nullsFirst, nullsLast; Collections.reverseOrder
                 "Comparators"
                 ;; TreeSet's and NavigableSet's SortedSet members, Collections' SortedSet
                 ;; methods, instance? SortedSet
                 "SortedSet"])
           ;; RandomGenerator's bounded defaults and streams, Random's streams, SplittableRandom
           ["jdk/internal/util/random/RandomSupport.java"]
           ;; BigInteger.nextProbablePrime of large numbers
           ["java/math/BitSieve.java"]
           ;; plain Java of java.util
           (map #(str "java/util/" % ".java")
                ["BitSet" "PriorityQueue" "WeakHashMap" "StringTokenizer" "Base64"
                 "SplittableRandom" "EventObject" "EventListener" "EventListenerProxy"
                 "Observable" "Observer" "TooManyListenersException" "InputMismatchException"
                 "MissingResourceException" "IllformedLocaleException"
                 "InvalidPropertiesFormatException" "FormattableFlags"
                 "ServiceConfigurationError"])))))

(def added-module-sources
  "Files of other modules' src/MODULE/share/classes added to the closure (paths relative to
  jdk26u), translated as if they were java.base's (javac's --patch-module java.base takes them):
  java.sql.Timestamp, whose class core's #inst support names (JRT-NOTES.md, phase 2B
  \"Dates\"), and java.sql.Date, which Timestamp's toString uses (its Go name is Sql_Date: c2g's
  rename table)."
  ["src/java.sql/share/classes/java/sql/Date.java" "src/java.sql/share/classes/java/sql/Timestamp.java"])

(def file-sources
  "java.io.File and the file system in the Go build (the user's decision of 2026-10-09,
  reversing D6 for files; doc/go/JRT-NOTES.md, \"Files\"): paths relative to jdk26u.
  UnixFileSystem, DefaultFileSystem and the file: URL handler are src/java.base/unix/classes
  files (KIND unix)."
  (vec (sort
         (concat
           ;; File and the abstract FileSystem with its Unix implementation, whose natives are
           ;; jrt's over the host (variants overlay/jdk/variants/UnixFileSystem.clj, File.clj),
           ;; the interfaces and the delete-on-exit hook File uses; FileReader and FileWriter
           (map #(str "src/java.base/share/classes/java/io/" % ".java")
                ["File" "FileSystem" "FileFilter" "FilenameFilter" "DeleteOnExitHook" "FileReader"
                 "FileWriter"])
           (map #(str "src/java.base/unix/classes/java/io/" % ".java")
                ["UnixFileSystem" "DefaultFileSystem"])
           ;; java.nio.file as far as Clojure's java.io namespace, arbace.main and the suite use
           ;; it: jrt's own Path, Files and HostPath (overlay/jdk), and jdk26u's Paths, options
           ;; and exceptions
           (map #(str "src/java.base/share/classes/java/nio/file/" % ".java")
                ["Paths" "OpenOption" "StandardOpenOption" "LinkOption" "CopyOption"
                 "StandardCopyOption" "FileSystemException" "NoSuchFileException"
                 "FileAlreadyExistsException" "DirectoryNotEmptyException" "AccessDeniedException"
                 "InvalidPathException" "ProviderMismatchException" "attribute/FileAttribute"])
           ;; URLs (io/as-url, readers on file: URLs, slurp of a path string, RT.toUrl): URL and
           ;; its handlers (file:, and http:, https: and jar: for their syntax; their
           ;; connections are outside the world), the file: connection, URLDecoder, and what
           ;; they use (Hashtable, HexFormat, the address checks of IPAddressUtil)
           (map #(str "src/java.base/share/classes/java/net/" % ".java")
                ["URL" "URLStreamHandler" "URLStreamHandlerFactory" "MalformedURLException"
                 "URLConnection" "UnknownServiceException" "URLDecoder" "ContentHandler"
                 "FileNameMap"])
           (map #(str "src/java.base/share/classes/" % ".java")
                ["java/util/Hashtable" "java/util/Dictionary" "java/util/HexFormat"
                 "jdk/internal/util/HexDigits" "jdk/internal/util/Exceptions"
                 "sun/net/util/IPAddressUtil" "sun/net/www/ParseUtil" "sun/net/www/URLConnection"
                 "sun/net/www/MessageHeader" "sun/net/www/protocol/file/FileURLConnection"
                 "sun/net/www/protocol/http/Handler" "sun/net/www/protocol/https/Handler"
                 "sun/net/www/protocol/jar/Handler"])
           ["src/java.base/unix/classes/sun/net/www/protocol/file/Handler.java"]))))

(defn overlay-sources
  "jrt's own Java sources (overlay/jdk/MODULE/...), which replace or add to jdk26u's: classes
  whose JDK sources need what is cut (java.nio's encoders and channels), written for jrt.
  Paths relative to overlay/jdk, sorted."
  []
  (let [root (io/file "overlay/jdk")]
    (if (.isDirectory root)
      (->> (file-seq root)
           (filter #(str/ends-with? (.getName ^File %) ".java"))
           (map #(str (.relativize (.toPath root) (.toPath ^File %))))
           sort vec)
      [])))

(defn sources
  "The source files of the translation closure (runtime + REPL) recorded in surface file
  `f` (doc/go/java-surface.edn), with `added-sources`: sorted paths relative to jdk26u, either
  src/java.base/share/classes/... or build/<conf>/support/gensrc/java.base/... (generated)."
  [f]
  (vec (sort (distinct (concat (keys (get-in (read-data f) [:closure :repl :sources]))
                               (map #(str "src/java.base/share/classes/" %) added-sources)
                               (map #(str "src/java.base/share/classes/" %) util-sources)
                               added-module-sources
                               file-sources)))))

(defn print-sources
  "Prints the closure's sources of surface file f, one per line, as \"MODULE KIND REL\": KIND
  is share (a file of src/MODULE/share/classes), unix (of src/java.base/unix/classes), share:OTHER (a file of another module's
  src/OTHER/share/classes, compiled into MODULE java.base), gensrc (generated by the JDK build)
  or overlay (jrt's own, overlay/jdk/MODULE/REL), REL the path below the module's source root."
  [f]
  (doseq [s (sources f)]
    (if-let [[_ rel] (re-find #"^src/java.base/unix/classes/(.*)$" s)]
      (println "java.base" "unix" rel)
    (if-let [[_ m rel] (re-find #"^src/([^/]+)/share/classes/(.*)$" s)]
      (if (= m "java.base")
        (println m "share" rel)
        ;; another module's file, into java.base's tree (KIND share:MODULE)
        (println "java.base" (str "share:" m) rel))
      (if-let [[_ m rel] (re-find #"^build/[^/]+/support/gensrc/([^/]+)/(.*)$" s)]
        (println m "gensrc" rel)
        (throw (ex-info (str "unexpected source path " s) {:source s}))))))
  ;; generated files added to the closure (added-gensrc), unless the measured closure has them
  (let [measured (set (for [s (sources f)
                            :let [[_ rel] (re-find #"^build/[^/]+/support/gensrc/java\.base/(.*)$" s)]
                            :when rel]
                        rel))]
    (doseq [rel added-gensrc :when (not (measured rel))]
      (println "java.base gensrc" rel)))
  ;; KIND overlay: overlay/jdk/MODULE/REL, jrt's own (replacing jdk26u's file of that path)
  (doseq [s (overlay-sources)
          :let [[_ m rel] (re-find #"^([^/]+)/(.*)$" s)]]
    (println m "overlay" rel)))

;; ---------------------------------------------------------------------------------------
;; the edge

(def ^:private cf (ClassFile/of))

(defn- class-files [dir]
  (->> (file-seq (io/file dir))
       (filter #(str/ends-with? (.getName ^File %) ".class"))
       (sort-by #(.getPath ^File %))))

(defn- dotted [^String s] (.replace s \/ \.))

(defn- elem-class [^String n]
  (if (str/starts-with? n "[")
    (some-> (second (re-find #"L([^;]+);" n)) dotted)
    (dotted n)))

(defn- model-info [^ClassModel m]
  {:super (some-> (.orElse (.superclass m) nil) .asInternalName dotted)
   :ifaces (mapv #(dotted (.asInternalName ^java.lang.classfile.constantpool.ClassEntry %)) (.interfaces m))
   :interface? (.has (.flags m) java.lang.reflect.AccessFlag/INTERFACE)
   :methods (into {} (for [^MethodModel mm (.methods m)]
                       [(str (.stringValue (.methodName mm)) (.stringValue (.methodType mm)))
                        {:static? (.has (.flags mm) java.lang.reflect.AccessFlag/STATIC)
                         :private? (.has (.flags mm) java.lang.reflect.AccessFlag/PRIVATE)}]))
   :fields (set (map #(.stringValue (.fieldName ^FieldModel %)) (.fields m)))})

(defn- intrinsic? [^MethodModel mm]
  (boolean
   (some (fn [^RuntimeVisibleAnnotationsAttribute a]
           (some #(= "Ljdk/internal/vm/annotation/IntrinsicCandidate;"
                     (.stringValue (.className ^Annotation %)))
                 (.annotations a)))
         (.findAttributes mm (Attributes/runtimeVisibleAnnotations)))))

(def ^:private class-kinds
  "Reference kinds that use a class without a member."
  #{:super :interface :new :checkcast :instanceof :anewarray :multianewarray :ldc-class :catch})

(defn- area
  "What a class at the edge is, for c2g and jrt: one of the VM's services."
  [n]
  (let [t (js/top-level n)]
    (cond
      (= t "jdk.internal.misc.Unsafe") :unsafe
      (re-find #"^java\.lang\.(reflect|invoke)\.|^java\.lang\.(Class|ClassLoader|ClassValue)$" t) :reflection
      (re-find #"^java\.lang\.(Object|String|StringBuilder|StringBuffer|AbstractStringBuilder|StringLatin1|StringUTF16|StringConcatHelper|Math|StrictMath|Enum|Record|Throwable|StackTraceElement|System|Runtime|Thread|ThreadLocal)$" t) :lang
      (re-find #"^java\.lang\.ref\." t) :references
      (re-find #"^java\.util\.concurrent\.(atomic|locks)\.|^java\.util\.concurrent\.(Executor|Executors|ExecutorService|Future|FutureTask|CountDownLatch|ThreadLocalRandom|ThreadFactory|ForkJoin)" t) :concurrency
      (re-find js/internal-boundary t) :vm-internal
      :else (or (js/category n) :other))))

(defn edge
  "The edge of the translated classes compiled into `dir` (dotted names = the translated set):
  every class and member outside the set they reference, resolved as the JVM resolves it (a
  call on a translated class inherited from java.lang.Object is Object's), with the
  translated classes using it; the native methods and the @IntrinsicCandidate methods the set
  declares. `closure` is java-surface.edn's [:closure :repl] (its :boundary and :native), for
  the cross-check: the closure's boundary is computed over reached methods only, the edge over
  whole files, so the boundary must be contained in the edge. `arbace-uses`: the member keys
  Arbace (runtime + REPL) itself calls on translated classes (the closure's entries; a member
  they inherit from a shim is boundary without any translated caller)."
  [dir closure arbace-uses]
  (let [models (into (sorted-map)
                     (for [f (class-files dir)
                           :let [m (.parse ^ClassFile cf (Files/readAllBytes (.toPath ^File f)))]]
                       [(dotted (.asInternalName (.thisClass ^ClassModel m))) m]))
        in-set? (fn [n] (contains? models n))
        index (into {} (for [[n m] models] [n (model-info m)]))
        members (atom (sorted-map))           ; [class member] -> #{user top-level}
        classes (atom (sorted-map))           ; class -> #{user} (class-only uses)
        types (atom (sorted-map))             ; class -> #{user} (in descriptors only)
        array-calls (atom (sorted-map))       ; member of an array type -> #{user}
        vcalls (atom {})                      ; virtual call: member -> #{owner named}
        add! (fn [a k u] (swap! a update k (fnil conj (sorted-set)) u))]
    (binding [js/*index* index]
      (doseq [[n ^ClassModel m] models
              :let [user (js/top-level n)]
              r (js/class-refs m)
              :let [owner (:owner r)
                    o (elem-class owner)]
              :when (and o (js/jdk-class? o))]
        (cond
          (contains? js/member-kinds (:kind r))
          (if (str/starts-with? owner "[")
            (add! array-calls (str (:name r) (:desc r)) user)
            (let [field? (#{:getfield :putfield :getstatic :putstatic} (:kind r))
                  k (if field? (:name r) (str (:name r) (:desc r)))
                  ;; a constructor is its class's own, never inherited
                  decl (or (cond field? (@#'js/find-field o (:name r))
                                 (str/starts-with? k "<") o
                                 :else (@#'js/find-method o k))
                           o)]
              (when (#{:invokevirtual :invokeinterface} (:kind r))
                (swap! vcalls update k (fnil conj #{}) o))
              (when-not (in-set? decl)
                (add! members [decl k] user))))
          (class-kinds (:kind r)) (when-not (in-set? o) (add! classes o user))
          :else (when-not (in-set? o) (add! types o user)))))
    (let [natives (into (sorted-map)
                        (for [[n ^ClassModel m] models
                              :let [ks (sort (for [^MethodModel mm (.methods m)
                                                   :when (.has (.flags mm) java.lang.reflect.AccessFlag/NATIVE)]
                                               (str (.stringValue (.methodName mm)) (.stringValue (.methodType mm)))))]
                              :when (seq ks)]
                          [n (vec ks)]))
          intrinsics (into (sorted-map)
                           (for [[n ^ClassModel m] models
                                 :let [ks (sort (for [^MethodModel mm (.methods m) :when (intrinsic? mm)]
                                                  (str (.stringValue (.methodName mm)) (.stringValue (.methodType mm)))))]
                                 :when (seq ks)]
                             [n (vec ks)]))
          by-class (reduce (fn [acc [[c k] us]]
                             (update acc c (fnil assoc (sorted-map)) k us))
                           (sorted-map) @members)
          edge-classes (into (sorted-set) (concat (keys by-class) (keys @classes)))
          ;; the closure's boundary: members by name+descriptor, fields by name
          boundary (for [[c ks] (:boundary closure) k ks] [c k])
          missing (for [[c k] boundary :when (not (contains? @members [c k]))] [c k])
          ;; reached in the closure by dispatch: a virtual call on a supertype of c (an
          ;; interface or class of the set, or Object), with c instantiated
          dispatch? (fn [[c k]]
                      (binding [js/*index* index]
                        (let [sups (conj (@#'js/jdk-supertypes c) "java.lang.Object")]
                          (some sups (get @vcalls k)))))
          by-dispatch (vec (filter dispatch? missing))
          missing (remove dispatch? missing)
          from-arbace (vec (filter (fn [[_ k]] (contains? arbace-uses k)) missing))
          missing (vec (remove (fn [[_ k]] (contains? arbace-uses k)) missing))
          native-missing (vec (for [[c ks] (:native closure) k ks
                                    :when (not (some #{k} (get natives c)))]
                                [c k]))]
      {:translated-classes (count models)
       :translated-top-level (count (into #{} (map js/top-level (keys models))))
       :edge-classes (count edge-classes)
       :edge-members (count @members)
       :by-treatment (into (sorted-map)
                           (for [[t cs] (group-by js/treatment edge-classes)]
                             [t {:classes (count cs)
                                 :members (count (filter #((set cs) (first %)) (keys @members)))}]))
       :by-area (into (sorted-map)
                      (for [[a cs] (group-by area edge-classes)]
                        [a {:classes (count cs)
                            :members (count (filter #((set cs) (first %)) (keys @members)))}]))
       :members (into (sorted-map)
                      (for [[c ks] by-class]
                        [c {:treatment (js/treatment c) :area (area c)
                            :members (into (sorted-map) (for [[k us] ks] [k (vec us)]))}]))
       :class-uses (into (sorted-map) (for [[c us] @classes]
                                        [c {:treatment (js/treatment c) :area (area c) :users (vec us)}]))
       :type-uses (into (sorted-map) (for [[c us] @types :when (not (contains? by-class c))
                                           :when (not (contains? @classes c))]
                                       [c {:treatment (js/treatment c) :users (count us)}]))
       :array-members (into (sorted-map) (for [[k us] @array-calls] [k (count us)]))
       :natives natives
       :intrinsics intrinsics
       :closure-check {:boundary-members (count boundary)
                       :boundary-classes (count (:boundary closure))
                       :in-edge (- (count boundary) (count missing) (count by-dispatch) (count from-arbace))
                       :by-dispatch by-dispatch
                       :from-arbace from-arbace
                       :missing missing
                       :closure-natives (reduce + (map count (vals (:native closure))))
                       :natives-missing native-missing}})))

;; ---------------------------------------------------------------------------------------
;; the report

(def known-differences
  "Converted files whose classes differ from javac's in known, harmless ways (the class forms
  compiler's, doc/classes/CONVERTER-NOTES.md, \"The converted JDK\"), by file: the kind and the
  number of differences. Counted as converted to javac's shapes; any other difference fails."
  {;; a lambda that constructs a local class passes the class's captured variables in their
   ;; order of capture, javac in the reverse order (the synthetic lambda method's parameters)
   "java/util/stream/Collectors.clj" [:local-class-capture-order 3]
   "java/util/stream/Gatherers.clj" [:local-class-capture-order 9]
   "java/util/stream/MatchOps.clj" [:local-class-capture-order 12]
   ;; InnerClasses: a nested class javac's stack map frames name (Spliterator$OfPrimitive)
   "java/util/stream/Nodes.clj" [:inner-classes-of-frames 2]})

(defn known-difference? [r]
  (when-let [[_ n] (known-differences (:file r))]
    (= n (:ndiffs r))))

(defn- fmt [n] (let [s (str n)] (if (> (count s) 3) (str (subs s 0 (- (count s) 3)) "," (subs s (- (count s) 3))) s)))

(defn- row [& cells] (str "| " (str/join " | " cells) " |"))

(defn check-results
  "class-forms-check's results under work/res: per converted file."
  [work]
  (vec (sort-by :file
                (for [^File f (sort (or (.listFiles (io/file work "res")) []))
                      :when (str/ends-with? (.getName f) ".edn")
                      r (read-data f)]
                  (update r :file #(str/replace % #"^.*/conv/" ""))))))

(defn summary
  "The counts of a run in work: sources, generated, converted, compiled, identical."
  [work]
  (let [rs (check-results work)
        conv (read-data (io/file work "conv/j2c-report.edn"))
        gen (some-> (io/file work "generated.edn") (#(when (.isFile ^File %) (read-data %))))
        n-ref (count (class-files (io/file work "ref1")))
        n-forms (count (class-files (io/file work "forms-classes")))]
    {:java-files (:files conv)
     :converted (:converted conv)
     :convert-failures (count (:failures conv))
     :javac-errors (:javac-errors conv)
     :unreadable (count (:unreadable conv))
     :generated gen
     :checked-files (count rs)
     :identical-files (count (filter :ok rs))
     :known-differing-files (count (filter known-difference? rs))
     :differing-files (count (filter #(pos? (:ndiffs % 0)) rs))
     :error-files (count (filter :error rs))
     :classes-compiled (reduce + (keep :classes rs))
     :classes-written n-forms
     :javac-classes n-ref
     :nondet (vec (sort (mapcat :nondet rs)))
     :problems (vec (for [r rs :when (not (:ok r))] (select-keys r [:file :error :ndiffs :diffs])))}))

(defn edge-tables
  "Markdown tables of an edge."
  [e]
  (str/join
   "\n"
   (concat
    [(str "Translated classes (compiled from the forms): " (fmt (:translated-classes e))
          " (" (:translated-top-level e) " top-level).")
     (str "Edge: " (:edge-classes e) " classes, " (fmt (:edge-members e)) " members (whole files).")
     ""
     (row "treatment" "classes" "members")
     (row "---" "---:" "---:")]
    (for [[t {:keys [classes members]}] (:by-treatment e)] (row (name (or t :none)) classes members))
    [""
     (row "area" "classes" "members")
     (row "---" "---:" "---:")]
    (for [[a {:keys [classes members]}] (sort-by (comp - :members val) (:by-area e))]
      (row (name a) classes members))
    [""
     (row "edge class" "treatment" "area" "members" "used by (top-level translated classes)")
     (row "---" "---" "---" "---:" "---")]
    (for [[c {:keys [treatment area members]}] (sort-by (fn [[c v]] [(- (count (:members v))) c]) (:members e))]
      (row (str "`" c "`") (name (or treatment :none)) (name area) (count members)
           (let [us (sort (into #{} (mapcat val members)))]
             (str (str/join ", " (map #(str "`" (last (str/split % #"\.")) "`") (take 6 us)))
                  (when (> (count us) 6) (str ", ... (" (count us) ")"))))))
    [""
     (str "Class-only uses (new, checkcast, instanceof, catch, super, ... without a member): "
          (str/join ", " (map #(str "`" % "`") (keys (:class-uses e)))) ".")
     ""
     (str "Natives declared by translated classes: "
          (str/join ", " (for [[c ks] (:natives e) k ks] (str "`" (last (str/split c #"\.")) "." k "`"))) ".")
     ""
     (str "`@IntrinsicCandidate` methods of translated classes: "
          (reduce + (map count (vals (:intrinsics e)))) " in " (count (:intrinsics e)) " classes: "
          (str/join ", " (for [[c ks] (:intrinsics e)] (str "`" (last (str/split c #"\.")) "` " (count ks)))) ".")
     ""
     (let [k (:closure-check e)]
       (str "Cross-check with the closure's boundary (java-surface.edn, reached methods only): "
            (:in-edge k) " of " (:boundary-members k) " members of " (:boundary-classes k)
            " classes are referenced directly by the translated classes, "
            (count (:by-dispatch k)) " are overrides reached by dispatch (a call on a supertype: "
            "`Object.toString`, `CharSequence`, `Appendable`, `Comparable`, `Random.next`, ...)"
            ", " (count (:from-arbace k)) " called by Arbace itself on a translated class that inherits it "
            (pr-str (:from-arbace k))
            (if (seq (:missing k)) (str "; missing: " (pr-str (:missing k))) ", none missing")
            "; natives of the closure " (:closure-natives k) ", all declared by translated classes"
            (when (seq (:natives-missing k)) (str " but " (pr-str (:natives-missing k))))
            "."))])))

(defn report
  "Writes work/report.edn and work/report.md from a run's results (and work/edge.edn)."
  [work]
  (let [s (summary work)
        e (let [f (io/file work "edge.edn")] (when (.isFile f) (read-data f)))
        e2 (let [f (io/file work "edge-javac.edn")] (when (.isFile f) (read-data f)))
        md (str/join
            "\n"
            (concat
             ["# jrt's translated input: bin/jrt-convert's report" ""
              (row "" "count") (row "---" "---:")
              (row "Java files (closure sources)" (:java-files s))
              (row "of which generated by the JDK build's tools" (count (:files (:generated s))))
              (row "converted by j2c" (:converted s))
              (row "conversion failures / javac errors / unreadable" (str (:convert-failures s) " / " (:javac-errors s) " / " (:unreadable s)))
              (row "files compiled by the class forms compiler" (:checked-files s))
              (row "of which all classes shape-identical to javac's" (:identical-files s))
              (row "differing (of them known, `known-differences`) / errors"
                   (str (:differing-files s) " (" (:known-differing-files s) ") / " (:error-files s)))
              (row "classes compiled from the forms" (fmt (:classes-compiled s)))
              (row "javac's classes of the same files" (fmt (:javac-classes s)))
              (row "classes not compared (javac's two runs differ)" (count (:nondet s)))
              ""]
             (when-let [g (:generated s)]
               [(str "Generated sources equal to the JDK build's (" (:jdk-gensrc g) "): "
                     (count (filter #(= :same (val %)) (:files g))) " of " (count (:files g))
                     (let [d (for [[f v] (:files g) :when (not= :same v)] (str f " " (name v)))]
                       (when (seq d) (str "; " (str/join ", " d))))
                     ".")
                ""])
             (for [p (:problems s)] (str "- " (pr-str p)))
             (when e ["" "## The edge" "" (edge-tables e)])
             (when (and e e2)
               ["" (str "Edge from javac's classes of the same files: "
                        (if (= (dissoc e :translated-classes) (dissoc e2 :translated-classes))
                          "identical to the edge from the forms' classes."
                          (str "DIFFERENT from the edge from the forms' classes ("
                               (:edge-members e2) " members, " (:translated-classes e2) " classes).")))])))]
    (spit (io/file work "report.edn") (with-out-str (pp/pprint (assoc s :edge (some-> e (dissoc :members :class-uses :type-uses :array-members :natives :intrinsics))))))
    (spit (io/file work "report.md") (str md "\n"))
    (println (format "%d Java files (%d generated), %d converted, %d compiled, %d identical, %d differing (%d known), %d errors; %d classes from the forms, %d javac classes%s"
                     (:java-files s) (count (:files (:generated s))) (:converted s) (:checked-files s)
                     (:identical-files s) (:differing-files s) (:known-differing-files s) (:error-files s)
                     (:classes-compiled s) (:javac-classes s)
                     (if e (format "; edge %d classes, %d members" (:edge-classes e) (:edge-members e)) "")))
    s))

(defn write-edge
  "Computes the edge of the classes in dir and writes it to out."
  [dir surface out]
  (let [d (read-data surface)
        uses (into #{} (for [[_ v] (:classes d)
                             :when (= :translate (:treatment v))
                             g [:runtime :repl]
                             k (keys (get-in v [:groups g :members]))]
                         k))
        e (edge dir (get-in d [:closure :repl]) uses)]
    (spit out (with-out-str (binding [*print-length* nil] (pp/pprint e))))
    (println "edge of" dir ":" (:edge-classes e) "classes," (:edge-members e) "members;"
             "closure boundary in edge:" (get-in e [:closure-check :in-edge]) "of"
             (get-in e [:closure-check :boundary-members]))))
