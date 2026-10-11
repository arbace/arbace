(ns g2c.java-surface
  "The Java surface of Arbace (B1a step 1, doc/go/JAVA-SURFACE.md): every JDK class, method,
  constructor and field the compiled classes of a stage reference, read from their bytecode
  with the JDK's class file API (java.lang.classfile). Run by bin/java-surface.

  Every class file under the stage directory is parsed; from each, the references to classes
  outside Arbace (java.*, javax.*, jdk.*, sun.*, org.*) are collected:
  - the instructions: invoke* (owner, name, descriptor), get/put field/static, new,
    checkcast, instanceof, anewarray, multianewarray, ldc of a class, method handle or method
    type, invokedynamic (its bootstrap method and the method handles among its arguments), the
    catch types of exception handlers;
  - the class itself: its superclass and interfaces, and the methods it declares that override
    or implement a method of a JDK supertype;
  - the types in descriptors (fields, methods, referenced members) as type uses.
  A reference whose owner is an Arbace class but whose member is inherited from a JDK class
  (Ratio's byteValue, from Number) is attributed to the declaring JDK class. A JDK reference is
  also resolved to the class that declares the member (StringBuilder.length to
  AbstractStringBuilder), kept as :decl.

  Each class belongs to a unit (the top-level class of a class form, or the namespace of an
  AOT-compiled class) and each unit to a group: :runtime (arbace.lang), :asm (arbace.asm),
  :repl (arbace.core and the namespaces loaded at a REPL's start, measured by starting one),
  :shipped (the other namespaces Clojure ships, spec, arbace.java.api), :class-forms (the class
  forms compiler arbace.classes, AOT-compiled into the stage).

  Besides: the translation closure (what the JDK classes proposed for translation reach in the
  JDK's own bytecode, and where it stops), the references to the runtime's class loading,
  reflection and call site classes, every invokedynamic bootstrap and reflective call site,
  the arbace.lang members the namespaces name, and a javap cross-check of the runtime group.
  `tables` renders the data as the tables of doc/go/JAVA-SURFACE.md.

  The output (doc/go/java-surface.edn) is deterministic: sorted, without times."
  (:require [arbace.java.io :as io]
            [arbace.string :as str])
  (:import (java.lang.classfile ClassFile ClassModel MethodModel FieldModel CodeModel)
           (java.lang.classfile.instruction InvokeInstruction FieldInstruction
                                            NewObjectInstruction TypeCheckInstruction
                                            ConstantInstruction$LoadConstantInstruction
                                            InvokeDynamicInstruction
                                            NewReferenceArrayInstruction
                                            NewMultiArrayInstruction ExceptionCatch)
           (java.lang.classfile.constantpool ClassEntry MethodHandleEntry MethodTypeEntry
                                             MemberRefEntry ConstantDynamicEntry)
           (java.lang.invoke MethodType)
           (java.io File)
           (java.nio.file Files)))

;; ---------------------------------------------------------------------------------------
;; names

(defn- dotted [^String internal] (.replace internal \/ \.))

(defn jdk-class?
  "Whether a class (dotted name) is outside Arbace: the JDK's or a platform library's."
  [^String n]
  (boolean (re-find #"^(java|javax|jdk|sun|com\.sun|org\.(xml|w3c|ietf))\." n)))

(defn- desc-classes
  "The class names (dotted) in a field or method descriptor."
  [^String d]
  (map (comp dotted second) (re-seq #"L([^;]+);" d)))

(defn- elem-class
  "The element class of an internal name or array descriptor ([[Ljava/lang/String; to
  java.lang.String), nil for a primitive array."
  [^String n]
  (if (str/starts-with? n "[")
    (first (desc-classes n))
    (dotted n)))

;; ---------------------------------------------------------------------------------------
;; the class index: Arbace's classes from their class files, JDK classes by reflection

(def ^:private cf (ClassFile/of))

(defn- parse [^File f] (.parse ^ClassFile cf (Files/readAllBytes (.toPath f))))

(defn- method-desc [^java.lang.reflect.Method m]
  (.toMethodDescriptorString (MethodType/methodType (.getReturnType m) (.getParameterTypes m))))

(def ^:private jdk-info
  (memoize
    (fn [n]
      (when-let [^Class c (try (Class/forName n false (ClassLoader/getPlatformClassLoader))
                               (catch Throwable _ nil))]
        {:super (some-> (.getSuperclass c) .getName)
         :ifaces (mapv #(.getName ^Class %) (.getInterfaces c))
         :interface? (.isInterface c)
         :methods (into {} (for [^java.lang.reflect.Method m (.getDeclaredMethods c)]
                             [(str (.getName m) (method-desc m))
                              {:static? (java.lang.reflect.Modifier/isStatic (.getModifiers m))
                               :private? (java.lang.reflect.Modifier/isPrivate (.getModifiers m))}]))
         :fields (set (map #(.getName ^java.lang.reflect.Field %) (.getDeclaredFields c)))}))))

(defn- model-info [^ClassModel m]
  {:super (some-> (.orElse (.superclass m) nil) .asInternalName dotted)
   :ifaces (mapv #(dotted (.asInternalName ^ClassEntry %)) (.interfaces m))
   :interface? (.has (.flags m) java.lang.reflect.AccessFlag/INTERFACE)
   :methods (into {} (for [^MethodModel mm (.methods m)]
                       [(str (.stringValue (.methodName mm)) (.stringValue (.methodType mm)))
                        {:static? (.has (.flags mm) java.lang.reflect.AccessFlag/STATIC)
                         :private? (.has (.flags mm) java.lang.reflect.AccessFlag/PRIVATE)}]))
   :fields (set (map #(.stringValue (.fieldName ^FieldModel %)) (.fields m)))})

(def ^:dynamic *index* "Arbace's classes: dotted name to model-info." {})

(defn- info [n] (or (*index* n) (jdk-info n)))

(defn- find-method
  "The class declaring method key (name + descriptor) as the JVM resolves it from class n:
  the superclass chain, then the superinterfaces (first found, breadth first)."
  [n k]
  (or (loop [c n]
        (when-let [i (and c (info c))]
          (if (contains? (:methods i) k) c (recur (:super i)))))
      (loop [queue (vec (mapcat #(:ifaces (info %))
                                (take-while some? (iterate #(:super (info %)) n))))
             seen #{}]
        (when-let [[c & more] (seq queue)]
          (if (seen c)
            (recur (vec more) seen)
            (let [i (info c)]
              (if (and i (contains? (:methods i) k) (not (:static? ((:methods i) k))))
                c
                (recur (into (vec more) (:ifaces i)) (conj seen c)))))))
      (when (str/starts-with? k "<") n)))

(defn- find-field
  "The class declaring field f as resolved from class n (superinterfaces, then superclass)."
  [n f]
  (loop [c n]
    (when-let [i (and c (info c))]
      (if (contains? (:fields i) f)
        c
        (or (some #(find-field % f) (:ifaces i)) (recur (:super i)))))))

(defn- jdk-supertypes
  "The JDK supertypes of class n, transitively, as a sorted set."
  [n]
  (loop [todo [n] seen #{} out (sorted-set)]
    (if-let [[c & more] (seq todo)]
      (if (seen c)
        (recur more seen out)
        (let [i (info c)
              sups (remove nil? (cons (:super i) (:ifaces i)))]
          (recur (into (vec more) sups) (conj seen c)
                 (into out (filter jdk-class? sups)))))
      out)))

;; ---------------------------------------------------------------------------------------
;; units and groups

(def class-form-groups
  "Packages of class forms (compiled by the class forms compiler) and their group."
  {"arbace.lang" :runtime "arbace.asm" :asm "arbace.asm.commons" :asm
   "arbace.asm.signature" :asm "arbace.java.api" :shipped})

(defn- package-of [^String n] (let [i (.lastIndexOf n ".")] (if (neg? i) "" (subs n 0 i))))

(defn source-namespaces
  "{munged file base (arbace.core_print) -> namespace} for every .clj file under root/arbace
  that is not a class form: the namespace its first ns or in-ns form names."
  [root]
  (into (sorted-map)
        (for [^File f (file-seq (io/file root "arbace"))
              :let [p (.getPath f)]
              :when (str/ends-with? p ".clj")
              :let [rel (subs p (inc (count (str root))))
                    base (-> rel (subs 0 (- (count rel) 4)) (str/replace "/" "."))]
              :when (not (class-form-groups (package-of base)))
              :let [form (with-open [r (java.io.PushbackReader. (io/reader f))]
                           (binding [*read-eval* false] (read r false nil)))
                    ns (when (seq? form)
                         (case (first form)
                           ns (str (second form))
                           in-ns (str (second (second form)))
                           nil))]
              :when ns]
          [base ns])))

(defn- unit-of
  "The unit [group-key unit-name] of class n: the top-level class of a class form, else the
  namespace whose AOT compilation made it."
  [file-ns ns-munged n]
  (let [pkg (package-of n)]
    (if-let [g (class-form-groups pkg)]
      [g (first (str/split n #"\$"))]
      (let [base (-> (first (str/split n #"\$")) (str/replace #"__init$" ""))]
        [nil (or (file-ns base)
                 (ns-munged base)
                 (some (fn [[m ns]] (when (str/starts-with? base (str m ".")) ns))
                       (sort-by (comp - count key) ns-munged))
                 (throw (ex-info (str "no namespace for " n) {})))]))))

(defn ns-group
  "The group of a namespace."
  [repl-nss ns]
  (cond (str/starts-with? ns "arbace.classes") :class-forms
        (repl-nss ns) :repl
        :else :shipped))

;; ---------------------------------------------------------------------------------------
;; collection

(defn- member-ref [kind ^String owner name desc]
  {:kind kind :owner owner :name name :desc desc})

(declare class-refs-of-code)

(defn class-refs
  "The references of a parsed class: a seq of maps with :kind and :owner (internal or array
  name), and :name/:desc for members."
  [^ClassModel m]
  (let [out (transient [])
        add! (fn [r] (conj! out r))]
    (when-let [s (.orElse (.superclass m) nil)] (add! {:kind :super :owner (.asInternalName s)}))
    (doseq [^ClassEntry i (.interfaces m)] (add! {:kind :interface :owner (.asInternalName i)}))
    (doseq [^FieldModel f (.fields m)]
      (doseq [c (desc-classes (.stringValue (.fieldType f)))] (add! {:kind :sig :owner c})))
    (doseq [^MethodModel mm (.methods m)]
      (doseq [c (desc-classes (.stringValue (.methodType mm)))] (add! {:kind :sig :owner c}))
      (when-let [^CodeModel code (.orElse (.code mm) nil)]
        (doseq [r (class-refs-of-code code)] (add! r))))
    (persistent! out)))

(defn class-refs-of-code
  "The references of the instructions and exception handlers of a method's code."
  [^CodeModel code]
  (let [out (transient [])
        add! (fn [r] (conj! out r))
        mh! (fn [kind ^MethodHandleEntry e]
              (let [^MemberRefEntry r (.reference e)]
                (add! (member-ref kind (.asInternalName (.owner r)) (.stringValue (.name r))
                                  (.stringValue (.type r))))))]
        (doseq [e (.elementList code)]
          (condp instance? e
            InvokeInstruction
            (let [^InvokeInstruction i e]
              (add! (member-ref (keyword (str/lower-case (str (.opcode i))))
                                (.asInternalName (.owner i)) (.stringValue (.name i))
                                (.stringValue (.type i)))))
            FieldInstruction
            (let [^FieldInstruction i e]
              (add! (member-ref (keyword (str/lower-case (str (.opcode i))))
                                (.asInternalName (.owner i)) (.stringValue (.name i))
                                (.stringValue (.type i)))))
            NewObjectInstruction
            (add! {:kind :new :owner (.asInternalName (.className ^NewObjectInstruction e))})
            TypeCheckInstruction
            (let [^TypeCheckInstruction i e]
              (add! {:kind (keyword (str/lower-case (str (.opcode i))))
                     :owner (.asInternalName (.type i))}))
            NewReferenceArrayInstruction
            (add! {:kind :anewarray
                   :owner (.asInternalName (.componentType ^NewReferenceArrayInstruction e))})
            NewMultiArrayInstruction
            (add! {:kind :multianewarray
                   :owner (.asInternalName (.arrayType ^NewMultiArrayInstruction e))})
            ConstantInstruction$LoadConstantInstruction
            (let [c (.constantEntry ^ConstantInstruction$LoadConstantInstruction e)]
              (condp instance? c
                ClassEntry (add! {:kind :ldc-class :owner (.asInternalName ^ClassEntry c)})
                MethodHandleEntry (mh! :ldc-mh c)
                MethodTypeEntry (doseq [x (desc-classes (.stringValue (.descriptor ^MethodTypeEntry c)))]
                                  (add! {:kind :sig :owner x}))
                ConstantDynamicEntry
                (let [^ConstantDynamicEntry d c]
                  (mh! :condy-bootstrap (.bootstrapMethod (.bootstrap d))))
                nil))
            InvokeDynamicInstruction
            (let [^InvokeDynamicInstruction i e
                  bsm (.bootstrap (.invokedynamic i))]
              (mh! :indy-bootstrap (.bootstrapMethod bsm))
              (when (= "arbace/lang/ReflectorCallSite"
                       (.asInternalName (.owner (.reference (.bootstrapMethod bsm)))))
                (add! {:kind :reflective-site :owner "arbace/lang/ReflectorCallSite"
                       :args (mapv (fn [a]
                                     (condp instance? a
                                       java.lang.classfile.constantpool.StringEntry
                                       (.stringValue ^java.lang.classfile.constantpool.StringEntry a)
                                       java.lang.classfile.constantpool.IntegerEntry
                                       (.intValue ^java.lang.classfile.constantpool.IntegerEntry a)
                                       (str a)))
                                   (.arguments bsm))}))
              (doseq [a (.arguments bsm)]
                (when (instance? MethodHandleEntry a) (mh! :indy-mh-arg a)))
              (doseq [x (desc-classes (.stringValue (.type i)))] (add! {:kind :sig :owner x})))
            ExceptionCatch
            (when-let [t (.orElse (.catchType ^ExceptionCatch e) nil)]
              (add! {:kind :catch :owner (.asInternalName ^ClassEntry t)}))
            nil))
    (persistent! out)))

(def member-kinds
  #{:invokevirtual :invokeinterface :invokestatic :invokespecial :getfield :putfield
    :getstatic :putstatic :ldc-mh :indy-bootstrap :indy-mh-arg :condy-bootstrap})

(defn- resolve-ref
  "A reference as a use of the JDK, or nil: {:kind :class (dotted) :member (name+desc, for
  members) :decl (declaring class) :via (the Arbace class named in the bytecode, when the
  member is inherited from the JDK)}."
  [{:keys [kind owner name desc]}]
  (let [arr? (str/starts-with? owner "[")
        c (elem-class owner)]
    (cond
      (nil? c) nil
      (and arr? (member-kinds kind)) nil         ; clone of an array: c2g's arrays
      (member-kinds kind)
      (let [field? (#{:getfield :putfield :getstatic :putstatic} kind)
            k (if field? name (str name desc))
            decl (or (if field? (find-field c name) (find-method c k)) c)]
        (cond (jdk-class? decl)
              (cond-> {:kind kind :class decl :member k}
                (not (jdk-class? c)) (assoc :via c)
                (and (jdk-class? c) (not= c decl)) (assoc :class c :decl decl))
              :else nil))
      (jdk-class? c) {:kind kind :class c}
      :else nil)))

(defn- overrides
  "The JDK methods that the methods class model m declares override or implement:
  [{:kind :override :class jdk-class :member k}]."
  [n ^ClassModel m]
  (let [sups (jdk-supertypes n)]
    (when (seq sups)
      (for [^MethodModel mm (.methods m)
            :let [k (str (.stringValue (.methodName mm)) (.stringValue (.methodType mm)))
                  fl (.flags mm)]
            :when (not (or (str/starts-with? k "<")
                           (.has fl java.lang.reflect.AccessFlag/STATIC)
                           (.has fl java.lang.reflect.AccessFlag/PRIVATE)))
            c sups
            :let [mi (get (:methods (jdk-info c)) k)]
            :when (and mi (not (:static? mi)) (not (:private? mi)))]
        {:kind :override :class c :member k}))))

(defn- class-files [dir]
  (->> (file-seq (io/file dir))
       (filter #(str/ends-with? (.getName ^File %) ".class"))
       (sort-by #(.getPath ^File %))))

(defn collect
  "All JDK uses of the classes under stage-dir: a seq of {:group :unit :from :kind :class
  :member? :decl? :via?}."
  [stage-dir file-ns repl-nss]
  (let [root (.toPath (io/file stage-dir))
        models (for [^File f (class-files stage-dir)
                     :let [m (parse f)]]
                 [(dotted (.asInternalName (.thisClass ^ClassModel m))) m])
        idx (into {} (map (fn [[n m]] [n (model-info m)])) models)
        ns-munged (into {} (map (fn [ns] [(munge ns) ns])) (set (vals file-ns)))]
    (binding [*index* idx]
      (doall
        (for [[n m] models
              :let [[g unit] (unit-of file-ns ns-munged n)
                    group (or g (ns-group repl-nss unit))]
              u (concat (keep resolve-ref (class-refs m)) (overrides n m))]
          (assoc u :group group :unit unit :from n))))))

;; ---------------------------------------------------------------------------------------
;; classification and proposed treatment

(def categories
  "Category of a JDK class, by the first matching rule: [category regex]."
  [[:lang-core #"^java\.lang\.(Object|String|StringBuilder|StringBuffer|AbstractStringBuilder|CharSequence|Number|Long|Integer|Short|Byte|Double|Float|Boolean|Character|CharacterData.*|CharacterName|Math|StrictMath|Comparable|Iterable|Enum|Void|Record|Cloneable|AutoCloseable|Appendable|Readable|Runnable|Override|Deprecated|FunctionalInterface|SafeVarargs)$"]
   [:lang-exceptions #"^java\.lang\.[A-Za-z]*(Exception|Error|Throwable)$"]
   [:lang-system #"^java\.lang\.(System|Runtime|Runtime\$.*|Thread|Thread\$.*|ThreadLocal|InheritableThreadLocal|StackTraceElement|StackWalker.*|Process|ProcessBuilder.*|ProcessHandle.*|ScopedValue.*)$"]
   [:class #"^java\.lang\.(Class|ClassLoader|Package|Module|ModuleLayer|ClassValue)$"]
   [:reflect #"^java\.lang\.reflect\."]
   [:invoke #"^java\.lang\.(invoke|constant|runtime)\."]
   [:classfile #"^java\.lang\.classfile\."]
   [:ref #"^java\.lang\.ref\."]
   [:lang-misc #"^java\.lang\.[A-Z]"]
   [:regex #"^java\.util\.regex\."]
   [:functional #"^java\.util\.function\."]
   [:stream #"^java\.util\.stream\."]
   [:atomics-locks #"^java\.util\.concurrent\.(atomic|locks)\."]
   [:concurrency #"^java\.util\.concurrent\."]
   [:collections #"^java\.util\.(ArrayList|LinkedList|ArrayDeque|HashMap|LinkedHashMap|TreeMap|HashSet|LinkedHashSet|TreeSet|IdentityHashMap|WeakHashMap|AbstractList|AbstractMap|AbstractMap\$.*|AbstractCollection|AbstractSet|AbstractSequentialList|Collection|List|Map|Map\$Entry|Set|SortedMap|SortedSet|NavigableMap|NavigableSet|SequencedCollection|SequencedMap|SequencedSet|Iterator|ListIterator|Enumeration|RandomAccess|Collections|Arrays|Comparator|Spliterator|Spliterators|Deque|Queue|Vector|Hashtable|Dictionary|Stack|BitSet|Objects|Optional.*|ConcurrentModificationException|NoSuchElementException)$"]
   [:util-misc #"^java\.util\.[A-Z]"]
   [:math #"^java\.math\."]
   [:io #"^java\.io\."]
   [:nio #"^java\.nio\."]
   [:net #"^java\.net\."]
   [:text #"^java\.text\."]
   [:time #"^java\.(time|sql)\."]
   [:security #"^java(x)?\.security\."]
   [:desktop #"^(java\.awt|javax\.swing)\."]
   [:xml #"^(javax\.xml|org\.xml|org\.w3c)\."]
   [:jdk-internal #"^(jdk|sun|com\.sun)\."]
   [:other #"."]])

(defn top-level "The top-level class of a nested class's name." [n] (first (str/split n #"\$")))

(defn category
  "The category of a JDK class (a nested class's is its top-level class's)."
  [n]
  (let [t (top-level n)] (some (fn [[c re]] (when (re-find re t) c)) categories)))

(def treatments
  "The proposed treatment of a JDK class: by class (or its top-level class), else by category.
  :translate  c2g from jdk26u's Java (through j2c): exact semantics, the JDK's own code
  :shim       hand-written Go forms over Go's std or jrt's host interface
  :c2g        a language feature c2g compiles directly (no jrt class): Java's string `+`
              (StringConcatFactory) and lambdas (LambdaMetafactory) in the class forms
  :rework     Arbace's runtime changed to do without it (method handles and call sites, class
              loading: the evaluator's concerns)
  :cut        left out of the first REPL (D6); what breaks is listed in JAVA-SURFACE.md"
  {:by-class
   {;; java.lang: the object model, strings and the VM's services are jrt's own
    "java.lang.Object" :shim "java.lang.String" :shim "java.lang.StringBuilder" :shim
    "java.lang.AbstractStringBuilder" :shim "java.lang.StringBuffer" :shim
    "java.lang.StringLatin1" :shim "java.lang.StringUTF16" :shim "java.lang.StringConcatHelper" :shim
    "java.lang.Math" :shim "java.lang.StrictMath" :shim "java.lang.Enum" :shim
    "java.lang.Record" :shim "java.lang.Throwable" :shim "java.lang.StackTraceElement" :shim
    "java.lang.StackWalker" :cut "java.lang.Class" :shim "java.lang.ClassLoader" :rework
    "java.lang.Package" :cut "java.lang.Module" :cut "java.lang.ModuleLayer" :cut
    "java.lang.ClassValue" :shim "java.lang.ThreadGroup" :cut "java.lang.ThreadDeath" :cut
    "java.lang.Process" :cut "java.lang.ProcessBuilder" :cut "java.lang.ProcessHandle" :cut
    "java.lang.RuntimePermission" :cut "java.lang.Override" :cut "java.lang.Deprecated" :cut
    "java.lang.FunctionalInterface" :cut "java.lang.SuppressWarnings" :cut
    "java.lang.SafeVarargs" :cut
    "java.lang.reflect.Array" :shim "java.lang.reflect.Modifier" :translate
    "java.lang.reflect.ParameterizedType" :cut "java.lang.reflect.TypeVariable" :cut
    "java.lang.reflect.WildcardType" :cut "java.lang.reflect.GenericArrayType" :cut
    "java.lang.reflect.RecordComponent" :cut "java.lang.reflect.Type" :cut
    "java.lang.invoke.StringConcatFactory" :c2g "java.lang.invoke.LambdaMetafactory" :c2g
    ;; java.util
    "java.util.Calendar" :cut "java.util.GregorianCalendar" :cut "java.util.TimeZone" :cut
    "java.util.SimpleTimeZone" :cut "java.util.Scanner" :cut "java.util.ServiceLoader" :cut
    "java.util.EventListener" :cut "java.util.ResourceBundle" :cut
    "java.util.Locale" :shim "java.util.Date" :shim
    "java.util.concurrent.Executors" :shim "java.util.concurrent.ExecutorService" :shim
    "java.util.concurrent.Executor" :shim "java.util.concurrent.ThreadFactory" :shim
    "java.util.concurrent.Future" :shim "java.util.concurrent.FutureTask" :shim
    "java.util.concurrent.CountDownLatch" :shim "java.util.concurrent.ThreadLocalRandom" :shim
    "java.util.concurrent.ForkJoinPool" :cut "java.util.concurrent.ForkJoinTask" :cut
    ;; java.io: the host's files and the platform charsets behind a shim
    "java.io.File" :shim "java.io.FileInputStream" :shim "java.io.FileOutputStream" :shim
    "java.io.FileReader" :shim "java.io.FileWriter" :shim "java.io.FileDescriptor" :shim
    "java.io.PrintStream" :shim "java.io.InputStreamReader" :shim
    "java.io.OutputStreamWriter" :shim "java.io.Console" :shim
    "java.io.ObjectInputStream" :cut "java.io.ObjectOutputStream" :cut
    "java.io.ObjectStreamField" :cut "java.io.RandomAccessFile" :cut
    "java.nio.charset.Charset" :shim "java.security.SecureRandom" :shim
    "java.text.DecimalFormatSymbols" :shim}
   :by-category
   {:lang-core :translate :lang-exceptions :translate :lang-system :shim :class :shim
    :reflect :shim :invoke :rework :classfile :cut :ref :shim :lang-misc :cut
    :regex :translate :functional :translate :stream :cut :concurrency :translate
    :atomics-locks :shim :collections :translate :util-misc :translate :math :translate
    :io :translate :nio :cut :net :cut :text :cut :time :cut :security :cut :desktop :cut
    :xml :cut :jdk-internal :translate :other :cut}})

(def internal-boundary
  "JDK-internal classes that are the VM's interfaces (Unsafe, VM, SharedSecrets, the
  charsets' and the security providers' machinery): shimmed with the java.* classes they serve.
  The other internal classes (jdk.internal.math, jdk.internal.util, ...) are plain Java."
  #"^(jdk\.internal\.(misc|access|reflect|loader|vm|ref|event|module|foreign|invoke|platform)\.|sun\.(nio|security|reflect|invoke|misc)\.|sun\.util\.(locale|resources|calendar|cldr)\.|jdk\.internal\.util\.SystemProps)")

(defn treatment
  "The proposed treatment of a JDK class."
  [n]
  (let [t (top-level n)]
    (or (get-in treatments [:by-class n])
        (get-in treatments [:by-class t])
        (when (re-find internal-boundary t) :shim)
        (get-in treatments [:by-category (category n)]))))

(defn jdk-source
  "The jdk26u source file of a JDK class (its top-level class), or nil."
  [jdk n]
  (let [top (first (str/split n #"\$"))
        rel (str (str/replace top "." "/") ".java")]
    (or (some (fn [^File m]
                (let [f (io/file m "share/classes" rel)]
                  (when (.isFile f) f)))
              (sort (.listFiles (io/file jdk "src"))))
        ;; classes the JDK's build generates (CharacterData00, ...)
        (some (fn [^File b]
                (some (fn [^File m]
                        (let [f (io/file m rel)]
                          (when (.isFile f) f)))
                      (sort (.listFiles (io/file b "support/gensrc")))))
              (sort (.listFiles (io/file jdk "build")))))))

(defn code-lines
  "The lines of a Java file that hold code: neither blank nor only comment."
  [^File f]
  (loop [[l & more :as ls] (str/split-lines (slurp f)) in-block false n 0]
    (if (empty? ls)
      n
      (let [s (str/trim l)
            [code? in-block]
            (loop [s s in-block in-block code? false]
              (cond
                (str/blank? s) [code? in-block]
                in-block (let [i (str/index-of s "*/")]
                           (if i (recur (str/trim (subs s (+ i 2))) false code?) [code? true]))
                (str/starts-with? s "//") [code? false]
                (str/starts-with? s "/*") (recur (subs s 2) true code?)
                :else [true (let [i (str/index-of s "/*")
                                  j (str/index-of s "*/" (or i 0))]
                              (and i (not (str/includes? (subs s 0 i) "//")) (nil? j)))]))]
        (recur more in-block (if code? (inc n) n))))))

;; ---------------------------------------------------------------------------------------
;; summary

;; ---------------------------------------------------------------------------------------
;; the translation closure: what the translated JDK classes need of the JDK in turn

(def jrt-fs (delay (java.nio.file.FileSystems/getFileSystem (java.net.URI/create "jrt:/"))))

(def jdk-model
  "The class model of a JDK class (dotted name), from the running JDK's jrt image, or nil."
  (memoize
    (fn [n]
      (when-let [^Class c (try (Class/forName n false (ClassLoader/getPlatformClassLoader))
                               (catch Throwable _ nil))]
        (let [p (.getPath ^java.nio.file.FileSystem @jrt-fs
                          (str "/modules/" (.getName (.getModule c)) "/"
                               (str/replace n "." "/") ".class")
                          (make-array String 0))]
          (when (Files/exists p (make-array java.nio.file.LinkOption 0))
            (.parse ^ClassFile cf (Files/readAllBytes p))))))))

(defn- translated? [n] (= :translate (treatment n)))

(defn- method-model ^MethodModel [^ClassModel m k]
  (some (fn [^MethodModel mm]
          (when (= k (str (.stringValue (.methodName mm)) (.stringValue (.methodType mm)))) mm))
        (.methods m)))

(defn closure
  "The JDK methods reached from the entry members [class key] by rapid type analysis over the
  JDK's bytecode, translating only the classes whose treatment is :translate (and the JDK's
  internal classes but the VM's interfaces); calls into other classes are the boundary.
  Returns {:methods {class #{key}} :code-bytes {class n} :boundary {class #{key}}
  :instantiated #{class} :native {class #{key}}}."
  [entries instantiated0]
  (let [reached (atom #{}) boundary (atom (sorted-map)) natives (atom (sorted-map))
        inst (atom #{}) vcalls (atom {}) code-bytes (atom (sorted-map)) touched (atom #{})
        work (java.util.ArrayDeque.)
        subtype? (fn [x c] (or (= x c) (contains? (jdk-supertypes x) c)))]
    (letfn [(reach! [c k]
              (cond
                (not (jdk-class? c)) nil
                (not (translated? c)) (swap! boundary update c (fnil conj (sorted-set)) k)
                (contains? @reached [c k]) nil
                :else (do (swap! reached conj [c k]) (.add work [c k]) (touch! c))))
            (touch! [c]
              (when (and (translated? c) (not (@touched c)))
                (swap! touched conj c)
                (when-let [m (jdk-model c)]
                  (when (method-model m "<clinit>()V") (reach! c "<clinit>()V")))
                (when-let [s (:super (info c))] (touch! s))))
            (instantiate! [x]
              (when (and (jdk-class? x) (not (@inst x)))
                (swap! inst conj x)
                (touch! x)
                (doseq [[k cs] @vcalls
                        :when (some #(subtype? x %) cs)]
                  (when-let [d (find-method x k)] (reach! d k)))))
            (vcall! [c k]
              (when-not (contains? (get @vcalls k) c)
                (swap! vcalls update k (fnil conj #{}) c)
                (when-let [d (find-method c k)] (reach! d k))
                (doseq [x @inst :when (and (not= x c) (subtype? x c))]
                  (when-let [d (find-method x k)] (reach! d k)))))
            (scan! [c k]
              (when-let [m (jdk-model c)]
                (when-let [mm (method-model m k)]
                  (if-let [^CodeModel code (.orElse (.code mm) nil)]
                    (do
                      (swap! code-bytes update c (fnil + 0)
                             (.codeLength ^java.lang.classfile.attribute.CodeAttribute code))
                      (doseq [r (class-refs-of-code code)]
                        (let [o (elem-class (:owner r))]
                          (when (and o (not (str/starts-with? (:owner r) "[")))
                            (case (:kind r)
                              (:invokevirtual :invokeinterface)
                              (vcall! o (str (:name r) (:desc r)))
                              (:invokestatic :invokespecial :ldc-mh :indy-mh-arg)
                              (reach! (or (find-method o (str (:name r) (:desc r))) o)
                                      (str (:name r) (:desc r)))
                              (:getfield :putfield :getstatic :putstatic)
                              (let [d (or (find-field o (:name r)) o)]
                                (if (translated? d) (touch! d)
                                    (when (jdk-class? d)
                                      (swap! boundary update d (fnil conj (sorted-set)) (:name r)))))
                              :new (instantiate! o)
                              :indy-bootstrap (reach! o (str (:name r) (:desc r)))
                              (when (jdk-class? o) (touch! o)))))))
                    (when (.has (.flags mm) java.lang.reflect.AccessFlag/NATIVE)
                      (swap! natives update c (fnil conj (sorted-set)) k))))))]
      (doseq [x instantiated0] (instantiate! x))
      (doseq [[c k] entries] (if (= k :new) (instantiate! c) (reach! c k)))
      (loop []
        (when-let [[c k] (.poll work)]
          (scan! c k)
          (recur)))
      {:methods (reduce (fn [acc [c k]] (update acc c (fnil conj (sorted-set)) k))
                        (sorted-map) @reached)
       :code-bytes @code-bytes
       :boundary @boundary
       :native @natives
       :instantiated (into (sorted-set) @inst)})))

(defn closure-of
  "The closure from the JDK uses of groups gs: the members of translated classes they use,
  their JDK supertypes' methods they override (reached by JDK code calling them), and the
  classes they instantiate or extend."
  [uses gs & [only]]
  (let [us (filter #(and (gs (:group %)) (translated? (:class %))
                         (or (nil? only) (only (:class %))))
                   uses)]
    (closure (for [u us :when (:member u) :when (not= :override (:kind u))]
               [(or (:decl u) (:class u)) (:member u)])
             (for [u us :when (#{:new :super} (:kind u))] (:class u)))))

(def groups [:runtime :repl :shipped :asm :class-forms])

(def plumbing
  "JDK members the Clojure compiler emits into every namespace's classes whatever the source
  says (boxing, unboxing, constants, the loading of __init classes): [class member]. In the
  namespace groups they are counted apart from the source's own interop; the evaluator does not
  emit them, but jrt has them anyway (the runtime uses each)."
  #{["java.lang.Integer" "valueOf(I)Ljava/lang/Integer;"] ["java.lang.Long" "valueOf(J)Ljava/lang/Long;"]
    ["java.lang.Double" "valueOf(D)Ljava/lang/Double;"] ["java.lang.Character" "valueOf(C)Ljava/lang/Character;"]
    ["java.lang.Short" "valueOf(S)Ljava/lang/Short;"] ["java.lang.Byte" "valueOf(B)Ljava/lang/Byte;"]
    ["java.lang.Float" "valueOf(F)Ljava/lang/Float;"]
    ["java.lang.Boolean" "TRUE"] ["java.lang.Boolean" "FALSE"] ["java.lang.Boolean" "booleanValue()Z"]
    ["java.lang.Character" "charValue()C"] ["java.lang.Number" "intValue()I"]
    ["java.lang.Number" "longValue()J"] ["java.lang.Number" "doubleValue()D"]
    ["java.lang.Number" "floatValue()F"] ["java.lang.Number" "shortValue()S"]
    ["java.lang.Number" "byteValue()B"] ["java.lang.Object" "<init>()V"]
    ["java.lang.Object" "toString()Ljava/lang/String;"] ["java.lang.Object" "getClass()Ljava/lang/Class;"]
    ["java.lang.Class" "getClassLoader()Ljava/lang/ClassLoader;"]
    ["java.util.Arrays" "asList([Ljava/lang/Object;)Ljava/util/List;"]
    ["java.util.regex.Pattern" "compile(Ljava/lang/String;)Ljava/util/regex/Pattern;"]})

(defn summarize
  "The surface as data: per JDK class, its category, treatment, source, and per group its
  members (with kinds, sites and users) and type-only uses."
  [uses jdk]
  (let [by-class (group-by :class uses)]
    (into (sorted-map)
          (for [[c us] by-class
                :let [src (jdk-source jdk c)]]
            [c (cond->
                 {:category (category c)
                  :treatment (treatment c)
                  :groups
                  (into (sorted-map)
                        (for [[g gus] (group-by :group us)]
                          [g {:units (into (sorted-set) (map :unit gus))
                              :sites (count gus)
                              :kinds (into (sorted-set) (map :kind gus))
                              :members
                              (into (sorted-map)
                                    (for [[k mus] (group-by :member (filter :member gus))]
                                      [k (cond-> {:kinds (into (sorted-set) (map :kind mus))
                                                  :sites (count mus)
                                                  :units (into (sorted-set) (map :unit mus))}
                                           (contains? plumbing [c k]) (assoc :plumbing true)
                                           (some :decl mus) (assoc :decl (some :decl mus))
                                           (some :via mus)
                                           (assoc :via (into (sorted-set) (keep :via mus))))]))}]))}
                 src (assoc :source (subs (str src) (inc (count jdk)))
                            :source-lines (count (str/split-lines (slurp src)))
                            :source-code-lines (code-lines src)))]))))

(def watched
  "Arbace classes whose callers are the reflection, call site and class loading surface:
  references to their members are counted per group and unit."
  #{"arbace.lang.Reflector" "arbace.lang.ReflectorCallSite" "arbace.lang.KeywordInvokeSite"
    "arbace.lang.DynamicClassLoader" "arbace.lang.Compiler" "arbace.lang.RT"
    "arbace.lang.Intrinsics" "arbace.lang.Compile" "arbace.lang.Proxy" "arbace.lang.ProxyHandler"})

(def watched-members
  "Of the watched classes' members, those counted: all of Reflector's, ReflectorCallSite's,
  KeywordInvokeSite's, DynamicClassLoader's, Intrinsics', Compile's, ProxyHandler's; of
  Compiler and RT those about loading, evaluating and classes."
  #"^(load|loadResourceScript|loadLibrary|classForName|classForNameNonLoading|baseLoader|makeClassLoader|eval|compile|analyze|macroexpand|macroexpand1|maybeResolveIn|resolve|isSystemClass|emitReflectorSite|getCompilerOption|LOADER|CLASS_LOADER|USE_CONTEXT_CLASSLOADER)$")

(def lang-plumbing-owners
  "arbace.lang classes whose members the Clojure compiler emits on its own (fn classes, call
  sites, constants of literals): their references from namespaces are plumbing."
  #{"arbace.lang.IFn" "arbace.lang.AFn" "arbace.lang.AFunction" "arbace.lang.RestFn"
    "arbace.lang.Fn" "arbace.lang.Compiler" "arbace.lang.Reflector" "arbace.lang.KeywordInvokeSite"
    "arbace.lang.ReflectorCallSite" "arbace.lang.ILookupThunk" "arbace.lang.KeywordLookupSite"
    "arbace.lang.IObj" "arbace.lang.Tuple" "arbace.lang.MethodImplCache"})

(def lang-plumbing-names
  "Other arbace.lang members the compiler emits: [class name-regex]."
  {"arbace.lang.Var" #"^(getRawRoot|get|set|setMeta|bindRoot|setDynamic|intern|pushThreadBindings|popThreadBindings|isBound|hasRoot)$"
   "arbace.lang.RT" #"^(var|map|mapUniqueKeys|vector|set|classForName|classForNameNonLoading|arrayToList|readString|keyword|.*Cast|seq|list|nth|get|count|booleanCast|box|aget|aset|alength)$"
   "arbace.lang.Keyword" #"^(intern)$" "arbace.lang.Symbol" #"^(intern|create)$"
   "arbace.lang.Util" #"^(equiv|hash|classOf|equals|identical|sneakyThrow)$"
   "arbace.lang.Numbers" #"^(num)$"
   "arbace.lang.PersistentList" #"^(EMPTY|create)$" "arbace.lang.PersistentVector" #"^(EMPTY|create|adopt)$"
   "arbace.lang.PersistentArrayMap" #"^(EMPTY|createAsIfByAssoc|create)$"
   "arbace.lang.PersistentHashSet" #"^(EMPTY|create|createWithCheck)$"
   "arbace.lang.ArraySeq" #"^(create)$" "arbace.lang.Namespace" #"^(importClass)$"})

(defn lang-plumbing? [owner name]
  (boolean (or (lang-plumbing-owners owner)
               (some-> (lang-plumbing-names owner) (re-find name)))))

(defn arbace-refs
  "The references of the classes under stage-dir to the watched Arbace classes, and every
  invokedynamic or condy bootstrap (JDK or Arbace): {:watched {class {member {group {:sites
  :units}}}} :bootstraps {bootstrap {group {:sites :units}}}}."
  [stage-dir file-ns repl-nss]
  (let [ns-munged (into {} (map (fn [ns] [(munge ns) ns])) (set (vals file-ns)))
        rows (for [^File f (class-files stage-dir)
                   :let [m (parse f)
                         n (dotted (.asInternalName (.thisClass ^ClassModel m)))
                         [g u] (unit-of file-ns ns-munged n)
                         g (or g (ns-group repl-nss u))]
                   r (class-refs m)
                   :let [o (dotted (:owner r))]
                   :when (or (#{:indy-bootstrap :condy-bootstrap :reflective-site} (:kind r))
                             (and (#{:repl :shipped :class-forms} g) (:name r)
                                  (str/starts-with? o "arbace.lang.")
                                  (member-kinds (:kind r)))
                             (and (watched o) (:name r) (not= o (first (str/split n #"\$")))
                                  (or (not (#{"arbace.lang.Compiler" "arbace.lang.RT"} o))
                                      (re-find watched-members (:name r)))))]
               {:what (case (:kind r)
                        (:indy-bootstrap :condy-bootstrap) [:bootstraps (str o "." (:name r))]
                        :reflective-site
                        (let [[nm cl k] (:args r)]
                          [:reflective-sites (str (get ["METHOD" "MEMBER" "FIELD" "STATIC" "NEW"] k k)
                                                  " " (if (= cl "") "?" cl) " " nm)])
                        (if (and (#{:repl :shipped :class-forms} g) (str/starts-with? o "arbace.lang.")
                                 (not (and (watched o) (not= o (first (str/split n #"\$")))
                                           (or (not (#{"arbace.lang.Compiler" "arbace.lang.RT"} o))
                                               (re-find watched-members (:name r))))))
                          [:lang-interop o (str (:name r) (:desc r))]
                          [:watched o (:name r)]))
                :group g :unit u})
        tally (fn [rs] (into (sorted-map)
                             (for [[g grs] (group-by :group rs)]
                               [g {:sites (count grs) :units (into (sorted-set) (map :unit grs))}])))]
    (reduce (fn [acc [what rs]] (assoc-in acc what (tally rs)))
            {:watched (sorted-map) :bootstraps (sorted-map) :reflective-sites (sorted-map)
             :lang-interop (sorted-map)}
            (sort-by (comp str key) (group-by :what rows)))))

(defn javap-check
  "The cross-check of the runtime group by javap: the JDK members (owner.name:descriptor, or
  owner.name for a field) in the constant pools of the class files under dir, as `javap -v -p`
  prints them, against those this tool collected for the group from the same classes (member
  references whose owner in the bytecode is a JDK class). Returns {:javap n :tool n
  :only-javap [...] :only-tool [...]}."
  [dir uses group]
  (let [files (map str (class-files dir))
        pb (ProcessBuilder. ^java.util.List (into ["javap" "-v" "-p"] files))
        _ (.redirectErrorStream pb true)
        proc (.start pb)
        text (slurp (.getInputStream proc))
        _ (.waitFor proc)
        jv (into (sorted-set)
                 (for [[_ kind owner name desc]
                       (re-seq #"= (Methodref|InterfaceMethodref|Fieldref)\s+#\d+\.#\d+\s+// \"?([\w/$\[;]+)\"?\.\"?([^\":]+)\"?:(\S+)" text)
                       :let [o (dotted owner)]
                       :when (jdk-class? o)]
                   (if (= kind "Fieldref") (str o "." name) (str o "." name desc))))
        ours (into (sorted-set)
                   (for [u uses
                         :when (and (= group (:group u)) (:member u) (not (:via u))
                                    (not= :override (:kind u)))]
                     (str (:class u) "." (:member u))))]
    {:javap (count jv) :tool (count ours)
     :only-javap (vec (remove ours jv)) :only-tool (vec (remove jv ours))}))

(defn j2c-statuses
  "The result of bin/j2c-check --jdk per converted JDK source file, from its work directory
  (res/*.edn): {\"src/MODULE/share/classes/PATH.java\" :identical|:differing|:error}, or {}."
  [work]
  (into (sorted-map)
        (for [^File f (sort (or (.listFiles (io/file work "res")) []))
              :when (str/ends-with? (.getName f) ".edn")
              r (binding [*read-eval* false] (read-string (slurp f)))
              :let [[_ m path] (re-find #"/conv/([^/]+)/(.*)\.clj$" (:file r))]
              :when m]
          [(str "src/" m "/share/classes/" path ".java")
           (cond (:error r) :error (:ok r) :identical :else :differing)])))

(defn run
  "Measures stage-dir (with repository root and jdk26u checkout jdk) and writes the data to
  out. repl-nss: the namespaces loaded at a REPL's start."
  [root stage-dir jdk out repl-nss j2c-work]
  (let [file-ns (source-namespaces root)
        j2c (j2c-statuses j2c-work)
        repl-nss (conj (set repl-nss) "arbace.core")
        uses (collect stage-dir file-ns repl-nss)
        units (->> (class-files stage-dir)
                   (map (fn [^File f]
                          (let [n (-> (.getPath f) (subs (inc (count (str stage-dir))))
                                      (str/replace #"\.class$" "") dotted)
                                ns-munged (into {} (map (fn [ns] [(munge ns) ns])) (set (vals file-ns)))
                                [g u] (unit-of file-ns ns-munged n)]
                            [(or g (ns-group repl-nss u)) u])))
                   (group-by first)
                   (map (fn [[g us]] [g {:classes (count us)
                                         :units (into (sorted-set) (map second us))}]))
                   (into (sorted-map)))
        ;; arbace.lang classes that use arbace.asm (by their bytecode references)
        asm-users (binding [*index* {}]
                    (into (sorted-set)
                          (for [^File f (class-files (io/file stage-dir "arbace/lang"))
                                :let [m (parse f)
                                      n (dotted (.asInternalName (.thisClass ^ClassModel m)))]
                                r (class-refs m)
                                :when (some-> (elem-class (:owner r)) (str/starts-with? "arbace.asm."))]
                            (first (str/split n #"\$")))))
        closure-data
        (fn [gs & [only]]
          (let [c (closure-of uses gs only)]
            {:scope (vec (sort gs))
             :classes (into (sorted-map)
                            (for [[cl ks] (:methods c)
                                  :let [src (jdk-source jdk cl)]]
                              [cl (cond-> {:methods (count ks)
                                           :code-bytes (get (:code-bytes c) cl 0)
                                           :category (category cl)}
                                    src (assoc :source (subs (str src) (inc (count jdk)))))]))
             :sources (into (sorted-map)
                            (for [src (distinct (keep #(jdk-source jdk %) (keys (:methods c))))]
                              (let [path (subs (str src) (inc (count jdk)))]
                                [path [(count (str/split-lines (slurp src))) (code-lines src)
                                       (cond (empty? j2c) :unchecked
                                             (str/starts-with? path "build/") :generated
                                             :else (get j2c path :unchecked))]])))
             :boundary (:boundary c)
             :native (:native c)}))
        data {:about "bin/java-surface: the JDK classes and members Arbace's compiled classes reference (doc/go/JAVA-SURFACE.md)"
              :stage (let [s (str stage-dir) r (str root "/")]
                       (if (str/starts-with? s r) (subs s (count r)) s))
              :jdk (System/getProperty "java.version")
              :repl-namespaces (into (sorted-set) repl-nss)
              :groups units
              :runtime-asm-users asm-users
              :arbace (let [a (arbace-refs stage-dir file-ns repl-nss)]
                        (update a :watched #(into (sorted-map)
                                                  (map (fn [[k v]] [k (into (sorted-map) v)]) %))))
              :javap-check (javap-check (io/file stage-dir "arbace/lang") uses :runtime)
              :closure {:repl (closure-data #{:runtime :repl})
                        :all (closure-data #{:runtime :repl :shipped})
                        :regex (closure-data #{:runtime :repl :shipped}
                                             #{"java.util.regex.Pattern" "java.util.regex.Matcher"})}
              :classes (summarize uses jdk)}]
    (with-open [w (io/writer out)]
      (binding [*out* w *print-length* nil *print-level* nil]
        (println ";; Generated by bin/java-surface; do not edit. Read with arbace.core/read.")
        (println "{")
        (doseq [[k v] (dissoc data :classes :closure :arbace)]
          (print " ") (pr k) (print " ") (pr v) (println))
        ;; the larger parts one entry per line, for readable diffs
        (doseq [k [:arbace :closure]]
          (print " ") (pr k) (println " {")
          (doseq [[k2 v2] (get data k)]
            (print "  ") (pr k2) (println " {")
            (doseq [[k3 v3] v2]
              (print "   ") (pr k3)
              (if (map? v3)
                (do (println " {")
                    (doseq [[k4 v4] v3] (print "    ") (pr k4) (print " ") (pr v4) (println))
                    (println "   }"))
                (do (print " ") (pr v3) (println))))
            (println "  }"))
          (println " }"))
        (println " :classes")
        (println " {")
        (doseq [[c v] (:classes data)]
          (print "  ") (pr c) (print " ") (pr v) (println))
        (println " }")
        (println "}")))
    data))

;; ---------------------------------------------------------------------------------------
;; tables (markdown, for doc/go/JAVA-SURFACE.md)

(def group-names {:runtime "runtime" :repl "REPL" :shipped "shipped" :asm "asm"
                  :class-forms "class forms"})

(defn- members-of
  "The member keys class entry v uses in groups gs; with source? only those not plumbing."
  [v gs source?]
  (into #{} (for [g gs [k mv] (get-in v [:groups g :members])
                  :when (or (not source?) (not (:plumbing mv)))]
              k)))

(defn- used-in? [v gs] (some #(get-in v [:groups %]) gs))

(defn- fmt [n] (let [s (str n)] (if (> (count s) 3) (str (subs s 0 (- (count s) 3)) "," (subs s (- (count s) 3))) s)))

(defn- row [& cells] (str "| " (str/join " | " cells) " |"))

(defn tables
  "Markdown tables of the measured data d."
  [d]
  (let [cls (:classes d)
        out (java.io.StringWriter.)
        p (fn [& xs] (.write out (str (apply str xs) "\n")))
        cats (map first categories)
        trs [:translate :shim :c2g :rework :cut]]
    ;; 1 groups
    (p "### Groups\n")
    (p (row "group" "classes" "units" "JDK classes" "JDK members" "of which plumbing" "reference sites"))
    (p "|---|---:|---:|---:|---:|---:|---:|")
    (doseq [g groups
            :let [gi (get-in d [:groups g])
                  vs (filter #(used-in? (val %) [g]) cls)
                  all (reduce + (map #(count (members-of (val %) [g] false)) vs))
                  src (reduce + (map #(count (members-of (val %) [g] true)) vs))]]
      (p (row (group-names g) (fmt (:classes gi)) (count (:units gi)) (count vs) (fmt all)
              (- all src) (fmt (reduce + (map #(get-in (val %) [:groups g :sites]) vs))))))
    (let [gs [:runtime :repl] vs (filter #(used-in? (val %) gs) cls)]
      (p (row "**runtime + REPL**" "" "" (count vs)
              (fmt (reduce + (map #(count (members-of (val %) gs false)) vs))) "" "")))
    (let [gs [:runtime :repl :shipped] vs (filter #(used-in? (val %) gs) cls)]
      (p (row "**runtime + REPL + shipped**" "" "" (count vs)
              (fmt (reduce + (map #(count (members-of (val %) gs false)) vs))) "" "")))
    ;; 2 categories
    (p "\n### Categories\n")
    (p "Classes / distinct members per group; the treatment is the category's default, with the")
    (p "classes treated otherwise counted under \"other treatments\".\n")
    (p (row "category" "treatment" "runtime" "REPL" "shipped" "asm" "class forms" "other treatments"))
    (p "|---|---|---:|---:|---:|---:|---:|---|")
    (doseq [c cats
            :let [vs (filter #(= c (:category (val %))) cls)]
            :when (seq vs)]
      (p (row (name c) (name (get-in treatments [:by-category c]))
              (str/join " | "
                        (for [g groups
                              :let [gv (filter #(used-in? (val %) [g]) vs)]]
                          (if (seq gv)
                            (str (count gv) " / " (reduce + (map #(count (members-of (val %) [g] false)) gv)))
                            "")))
              (str/join ", " (for [[t n] (frequencies (keep #(let [t (:treatment (val %))]
                                                                 (when (not= t (get-in treatments [:by-category c])) t))
                                                              vs))]
                               (str (name t) " " n))))))
    ;; 3 treatments
    (p "\n### Treatments, runtime + REPL\n")
    (p (row "treatment" "JDK classes" "members" "sites" "classes (runtime + REPL + shipped)" "members"))
    (p "|---|---:|---:|---:|---:|---:|")
    (doseq [t trs
            :let [vs (filter #(= t (:treatment (val %))) cls)
                  a (filter #(used-in? (val %) [:runtime :repl]) vs)
                  b (filter #(used-in? (val %) [:runtime :repl :shipped]) vs)]]
      (p (row (name t) (count a) (fmt (reduce + (map #(count (members-of (val %) [:runtime :repl] false)) a)))
              (fmt (reduce + (for [[_ v] a g [:runtime :repl]] (get-in v [:groups g :sites] 0))))
              (count b) (fmt (reduce + (map #(count (members-of (val %) [:runtime :repl :shipped] false)) b))))))
    ;; 4 closure
    (doseq [[k label] [[:repl "runtime + REPL"] [:all "runtime + REPL + shipped"]
                       [:regex "java.util.regex alone (Pattern and Matcher as used)"]]
            :let [c (get-in d [:closure k])]]
      (p "\n### The translation closure, " label "\n")
      (p (row "category" "classes" "methods" "bytecode bytes"))
      (p "|---|---:|---:|---:|")
      (doseq [[cat vs] (sort-by (comp - #(reduce + (map :code-bytes %)) val)
                                (group-by :category (vals (:classes c))))]
        (p (row (name cat) (count vs) (fmt (reduce + (map :methods vs)))
                (fmt (reduce + (map :code-bytes vs))))))
      (p (row "**all**" (count (:classes c)) (fmt (reduce + (map :methods (vals (:classes c)))))
              (fmt (reduce + (map :code-bytes (vals (:classes c)))))))
      (p (str "\nSource files touched: " (count (:sources c)) ", " (fmt (reduce + (map first (vals (:sources c)))))
              " lines, " (fmt (reduce + (map second (vals (:sources c))))) " code lines (whole files)."))
      (p (str "By j2c (`bin/j2c-check --jdk java.base`): "
              (str/join ", " (for [[st fs] (sort-by key (group-by #(nth % 2) (vals (:sources c))))]
                               (str (count fs) " files " (name st) " (" (fmt (reduce + (map second fs))) " code lines)")))
              "."))
      (let [bad (for [[path [_ _ st]] (:sources c) :when (#{:differing :error} st)] (str "`" path "`"))]
        (when (seq bad) (p (str "Not shape-identical: " (str/join ", " bad) "."))))
      (p (str "Boundary (calls from translated code into shimmed, reworked or cut classes): "
              (count (:boundary c)) " classes, " (reduce + (map count (vals (:boundary c)))) " members."))
      (when (= k :repl)
        (p "\n" (row "boundary class" "treatment" "members" "examples"))
        (p "|---|---|---:|---|")
        (doseq [[bc ks] (sort-by (comp - count val) (:boundary c))]
          (p (row (str "`" bc "`") (name (or (treatment bc) :none)) (count ks)
                  (str/join ", " (map #(str "`" (first (str/split % #"\(")) "`") (take 5 ks))))))))
    ;; 5 runtime units
    (p "\n### Runtime classes by treatment of what they use\n")
    (p "JDK members used by each `arbace.lang` class (with its nested classes), by treatment.\n")
    (p (row "class" "translate" "shim" "c2g" "rework" "cut" "uses arbace.asm"))
    (p "|---|---:|---:|---:|---:|---:|---|")
    (let [per (reduce (fn [acc [c v]]
                        (reduce (fn [acc [k mv]]
                                  (reduce (fn [acc u] (update-in acc [u (:treatment v)] (fnil conj #{}) [c k]))
                                          acc (:units mv)))
                                acc (get-in v [:groups :runtime :members])))
                      {} cls)]
      (doseq [[u m] (sort-by key per)]
        (p (row (str/replace u "arbace.lang." "")
                (str/join " | " (for [t trs] (let [n (count (get m t))] (if (pos? n) n ""))))
                (if (contains? (:runtime-asm-users d) u) "yes" "")))))
    ;; 6 reflective sites and bootstraps
    (p "\n### invokedynamic bootstraps\n")
    (p (row "bootstrap" "runtime" "REPL" "shipped" "asm" "class forms"))
    (p "|---|---:|---:|---:|---:|---:|")
    (doseq [[b gm] (get-in d [:arbace :bootstraps])]
      (p (row (str "`" b "`") (str/join " | " (for [g groups] (if-let [x (gm g)] (fmt (:sites x)) ""))))))
    (p "\n### Reflective call sites (`ReflectorCallSite`), by group\n")
    (let [rs (get-in d [:arbace :reflective-sites])]
      (p (row "group" "sites" "distinct (kind, class, member)" "units"))
      (p "|---|---:|---:|---|")
      (doseq [g groups
              :let [xs (filter #(get (val %) g) rs)]
              :when (seq xs)]
        (p (row (group-names g) (reduce + (map #(get-in (val %) [g :sites]) xs)) (count xs)
                (str/join ", " (sort (into #{} (mapcat #(get-in (val %) [g :units]) xs))))))))
    (p "\n### arbace.lang members the namespaces reference\n")
    (p "What the evaluator must reach by name when it evaluates the namespaces' interop into the")
    (p "runtime (`(. RT (count x))`, `(Numbers/add x y)` in inlines, `(.meta x)`); plumbing as for the")
    (p "JDK (members the compiler emits on its own: fn classes, `Var` roots, literals) apart.\n")
    (p (row "group" "classes" "members" "of which plumbing" "sites"))
    (p "|---|---:|---:|---:|---:|")
    (let [li (get-in d [:arbace :lang-interop])]
      (doseq [gs [[:repl] [:shipped] [:class-forms] [:repl :shipped]]
              :let [xs (for [[o ms] li [m gm] ms :when (some gm gs)] [o m gm])]]
        (p (row (str/join " + " (map group-names gs)) (count (distinct (map first xs))) (count xs)
                (count (filter (fn [[o m]] (lang-plumbing? o (first (str/split m #"\(")))) xs))
                (fmt (reduce + (for [[_ _ gm] xs g gs] (get-in gm [g :sites] 0))))))))
    ;; 7 per class
    (p "\n### Per JDK class\n")
    (p "Distinct members used per group (R runtime, P REPL, S shipped, A asm, K class forms; `t`")
    (p "for a type-only use: a cast, an `instanceof`, a catch, a descriptor, a supertype, a class")
    (p "constant); the runtime's users; the code lines of the class's jdk26u source file.\n")
    (p (row "class" "category" "treatment" "R" "P" "S" "A" "K" "runtime users" "source code lines"))
    (p "|---|---|---|---:|---:|---:|---:|---:|---|---:|")
    (doseq [[c v] (sort-by (fn [[c v]] [(.indexOf ^java.util.List (vec cats) (:category v)) c]) cls)]
      (p (row (str "`" c "`") (name (:category v)) (name (:treatment v))
              (str/join " | " (for [g [:runtime :repl :shipped :asm :class-forms]
                                    :let [gv (get-in v [:groups g])]]
                                (cond (nil? gv) ""
                                      (empty? (:members gv)) "t"
                                      :else (count (:members gv)))))
              (let [us (map #(str/replace % "arbace.lang." "") (get-in v [:groups :runtime :units]))]
                (str (str/join ", " (take 4 us)) (when (> (count us) 4) (str " +" (- (count us) 4)))))
              (if-let [n (:source-code-lines v)] (fmt n) ""))))
    (str out)))
