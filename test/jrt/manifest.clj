(ns jrt.manifest
  "jrt's manifest (C2G-SPEC §9.1, doc/go/JRT-NOTES.md, \"The manifest\"): for each class jrt
  hand-writes, its Java declaration as c2g needs it (flags, superclass, the interfaces jrt
  implements, the members jrt implements with their descriptors and modifiers, the promotable
  methods). Derived, not written: every member of the JDK class (the running JDK 26, by
  reflection) whose C2G-SPEC §4.4 Go name jrt's forms define is in the manifest; the others are
  the coverage report's missing members. A generated test file refers to every manifest member
  by its Go name, so that the build fails when the forms and the manifest drift apart.

  bin/jrt manifest   writes go/arbace/jrt/manifest.edn and manifest_test.clj, and prints the
                     coverage of the edge (.tmp/jrt/edge.edn, from bin/jrt-convert, when present)."
  (:require [arbace.g2c.print :as p]
            [arbace.string :as str]
            [arbace.pprint :as pp])
  (:import [java.io File]
           [java.lang.reflect Constructor Field Method Modifier]))

;; ---------------------------------------------------------------------------------------
;; C2G-SPEC §4.4's names

(defn go-type-name
  "The Go name of a class: its binary name after the package, $ as _, X before a first
  character that is not an upper-case letter."
  [^String binary]
  (let [simple (subs binary (inc (.lastIndexOf binary ".")))
        n (str/replace simple "$" "_")]
    (if (Character/isUpperCase (.charAt n 0)) n (str "X" n))))

(defn type-code [^Class c]
  (cond
    (.isArray c) (loop [c c d 0] (if (.isArray c) (recur (.getComponentType c) (inc d)) (str (type-code c) d)))
    (= c Boolean/TYPE) "Z" (= c Byte/TYPE) "B" (= c Character/TYPE) "C" (= c Short/TYPE) "S"
    (= c Integer/TYPE) "I" (= c Long/TYPE) "J" (= c Float/TYPE) "F" (= c Double/TYPE) "D"
    (= c Void/TYPE) "V"
    (= c Object) "O"
    :else (go-type-name (.getName c))))

(defn method-go-name [^String name params ^Class ret]
  (str (str/upper-case (subs name 0 1)) (subs name 1)
       (apply str (map #(str "_" (type-code %)) params))
       "__" (type-code ret)))

(defn ctor-suffix [params]
  (apply str (map #(str "_" (type-code %)) params)))

(defn descriptor [params ^Class ret]
  (letfn [(d [^Class c]
            (cond
              (.isArray c) (str "[" (d (.getComponentType c)))
              (.isPrimitive c) (type-code c)
              :else (str "L" (str/replace (.getName c) "." "/") ";")))]
    (str "(" (apply str (map d params)) ")" (if ret (d ret) "V"))))

(defn flags [mods]
  (into (sorted-set)
        (keep (fn [[bit k]] (when (pos? (bit-and mods bit)) k))
              [[Modifier/PUBLIC :public] [Modifier/PROTECTED :protected] [Modifier/PRIVATE :private]
               [Modifier/STATIC :static] [Modifier/FINAL :final] [Modifier/ABSTRACT :abstract]
               [Modifier/SYNCHRONIZED :synchronized] [Modifier/NATIVE :native]
               [Modifier/VOLATILE :volatile] [Modifier/INTERFACE :interface]])))

;; ---------------------------------------------------------------------------------------
;; What jrt's forms define

(defn defined
  "The Go names jrt's forms define (test files excluded): {:types #{T}, :methods #{[T M]},
  :values #{f}} for go/type, go/method (by receiver type) and go/func, go/var, go/const; the
  methods of an interface type count as its methods."
  [package-file]
  (let [pkg (p/collect package-file)
        tests (set (map str (:test-files (:package pkg))))
        decls (for [fl (:files pkg) :when (not (tests (str (:name fl)))) d (:decls fl)] d)
        base-type (fn [t] (if (and (seq? t) (= '* (first t))) (second t) t))
        names-of (fn [d] (let [xs (rest d)]
                           (if (vector? (first xs))
                             (map first xs)
                             (let [x (first xs)] (if (and (seq? x) (= 'values (first x))) (rest x) [x])))))]
    (reduce
      (fn [acc d]
        (case (name (first d))
          "type" (let [body (last d)
                       t (str (second d))]
                   (cond-> (update acc :types into (names-of d))
                     ;; an interface's methods, as [T M] (interfaces jrt hand-writes)
                     (and (seq? body) (= 'interface (first body)))
                     (update :methods into (for [x (rest body) :when (seq? x)] [t (str (first x))]))))
          "method" (let [[_ m & xs] d
                         params (first (filter vector? xs))
                         recv (first params)]
                     (update acc :methods conj [(str (base-type (:tag (meta recv)))) (str m)]))
          ("func" "var" "const") (update acc :values into (map str (names-of d)))
          acc))
      {:types #{} :methods #{} :values #{}}
      decls)))

;; ---------------------------------------------------------------------------------------
;; The hand-written classes

(def hand-written
  "jrt's hand-written classes (the shim edge that phase 1 implements): the Java class, the
  interfaces jrt's implementation has, and whether the class is non-leaf (Impl_ methods,
  §5.4). Object is the object model's own (§5.8): its members are not listed by Go name."
  [["java.lang.String" ["java.io.Serializable" "java.lang.Comparable" "java.lang.CharSequence"
                        "java.lang.constant.Constable" "java.lang.constant.ConstantDesc"] false]
   ["java.lang.StringBuilder" ["java.io.Serializable" "java.lang.Comparable" "java.lang.CharSequence" "java.lang.Appendable"] false]
   ["java.lang.StringBuffer" ["java.io.Serializable" "java.lang.Comparable" "java.lang.CharSequence" "java.lang.Appendable"] false]
   ["java.lang.Class" ["java.io.Serializable" "java.lang.reflect.Type"] false]
   ["java.lang.Throwable" ["java.io.Serializable"] true]
   ["java.lang.StackTraceElement" ["java.io.Serializable"] false]
   ["java.lang.Enum" ["java.lang.Comparable" "java.io.Serializable"] true]
   ["java.lang.Record" [] true]
   ["java.lang.Math" [] false]
   ["java.lang.StrictMath" [] false]
   ["java.lang.System" [] false]
   ["java.lang.reflect.Array" [] false]
   ["java.lang.StringLatin1" [] false]
   ["java.lang.StringUTF16" [] false]
   ["jdk.internal.event.ThrowableTracer" [] false]
   ;; phase 2b: reflection, class loaders, charsets, locales, dates
   ["java.lang.reflect.AccessibleObject" [] true]
   ["java.lang.reflect.Executable" ["java.lang.reflect.Member"] true]
   ["java.lang.reflect.Method" [] false]
   ["java.lang.reflect.Constructor" [] false]
   ["java.lang.reflect.Field" ["java.lang.reflect.Member"] false]
   ["java.lang.reflect.Member" [] false]
   ["java.lang.reflect.InvocationHandler" [] false]
   ["java.lang.reflect.Proxy" [] false]
   ["java.lang.ClassLoader" [] true]
   ["java.nio.charset.Charset" ["java.lang.Comparable"] true]
   ["sun.nio.cs.Unicode" [] true]
   ["sun.nio.cs.UTF_8" [] false]
   ["sun.nio.cs.ISO_8859_1" [] false]
   ["sun.nio.cs.US_ASCII" [] false]
   ;; step 5 phase 2B
   ["sun.nio.cs.UTF_16" [] false]
   ["sun.nio.cs.UTF_16BE" [] false]
   ["sun.nio.cs.UTF_16LE" [] false]
   ["java.nio.charset.StandardCharsets" [] false]
   ["java.util.Locale" ["java.lang.Cloneable" "java.io.Serializable"] false]
   ["java.util.Locale$Category" [] false]
   ["java.text.DecimalFormatSymbols" ["java.lang.Cloneable" "java.io.Serializable"] false]
   ["sun.util.locale.provider.LocaleProviderAdapter" [] false]
   ["sun.util.locale.provider.LocaleResources" [] false]
   ["sun.util.locale.provider.ResourceBundleBasedAdapter" [] false]
   ["java.util.Date" ["java.io.Serializable" "java.lang.Cloneable" "java.lang.Comparable"] true]
   ["jdk.internal.foreign.Utils" [] false]
   ;; phase 2a: threads, concurrency, Unsafe, the VM's services
   ["java.lang.Thread" ["java.lang.Runnable"] true]
   ["java.lang.Thread$UncaughtExceptionHandler" [] false]
   ["java.lang.Thread$Builder$OfVirtual" [] false]
   ["java.lang.ThreadLocal" [] true]
   ["java.lang.InheritableThreadLocal" [] false]
   ["java.lang.Runtime" [] false]
   ["java.lang.ref.Reference" [] true]
   ["java.lang.ref.WeakReference" [] false]
   ["java.lang.ref.SoftReference" [] false]
   ["java.lang.ref.ReferenceQueue" [] false]
   ["jdk.internal.misc.Unsafe" [] false]
   ["java.util.concurrent.atomic.AtomicInteger" ["java.io.Serializable"] false]
   ["java.util.concurrent.atomic.AtomicLong" ["java.io.Serializable"] false]
   ["java.util.concurrent.atomic.AtomicBoolean" ["java.io.Serializable"] false]
   ["java.util.concurrent.atomic.AtomicReference" ["java.io.Serializable"] false]
   ["java.util.concurrent.locks.Lock" [] false]
   ["java.util.concurrent.locks.Condition" [] false]
   ["java.util.concurrent.locks.ReentrantLock" ["java.util.concurrent.locks.Lock" "java.io.Serializable"] false]
   ["java.util.concurrent.locks.ReentrantReadWriteLock" ["java.io.Serializable"] false]
   ["java.util.concurrent.locks.ReentrantReadWriteLock$ReadLock" ["java.util.concurrent.locks.Lock" "java.io.Serializable"] false]
   ["java.util.concurrent.locks.ReentrantReadWriteLock$WriteLock" ["java.util.concurrent.locks.Lock" "java.io.Serializable"] false]
   ["java.util.concurrent.locks.LockSupport" [] false]
   ["java.util.concurrent.ThreadFactory" [] false]
   ["java.util.concurrent.Executor" [] false]
   ["java.util.concurrent.ExecutorService" ["java.util.concurrent.Executor"] false]
   ["java.util.concurrent.Executors" [] false]
   ["java.util.concurrent.Future" [] false]
   ["java.util.concurrent.FutureTask" ["java.lang.Runnable" "java.util.concurrent.Future"] false]
   ;; step 5 phase 2B: the fork-join pool (reducers' fold)
   ["java.util.concurrent.ForkJoinTask" ["java.util.concurrent.Future" "java.io.Serializable"] true]
   ["java.util.concurrent.ForkJoinPool" [] false]
   ["java.util.concurrent.CountDownLatch" [] false]
   ;; Clojure's pprint tests (a future blocked in acquire)
   ["java.util.concurrent.Semaphore" ["java.io.Serializable"] false]
   ["java.util.concurrent.ThreadLocalRandom" [] false]])

(def promotable
  {"java.lang.Enum" #{"name()Ljava/lang/String;" "ordinal()I" "hashCode()I" "equals(Ljava/lang/Object;)Z"}
   "java.lang.Object" #{"hashCode()I" "equals(Ljava/lang/Object;)Z"}})

(defn- class-for [n] (Class/forName n false (ClassLoader/getPlatformClassLoader)))

(defn members
  "Every member of the JDK class c with its manifest entry, its Go reference form and whether
  jrt defines it: [{:entry [:method name desc flags] :sig \"name(desc)\" :ref form :ok bool}]."
  [^String cname non-leaf? {:keys [methods values]}]
  (let [c (class-for cname)
        t (go-type-name cname)
        abstract? (Modifier/isAbstract (.getModifiers c))
        prom (promotable cname #{})
        ;; the public methods inherited from non-public superclasses below Object
        ;; (AbstractStringBuilder's), which jrt defines on the class itself
        iface? (.isInterface c)
        ;; jrt's structs embed their hand-written superclass's, so its methods are promoted
        supers (map #(go-type-name (.getName ^Class %)) (take-while some? (iterate #(.getSuperclass ^Class %) c)))
        has-method (fn [g] (some #(contains? methods [% g]) supers))
        own (set (map #(str (.getName ^Method %) (descriptor (.getParameterTypes ^Method %) (.getReturnType ^Method %)))
                      (remove #(.isSynthetic ^Method %) (.getDeclaredMethods c))))
        inherited (for [^Class s (take-while #(and % (not= % Object)) (iterate #(.getSuperclass ^Class %) (.getSuperclass c)))
                        :when (not (Modifier/isPublic (.getModifiers s)))
                        ^Method m (.getDeclaredMethods s)
                        :when (and (Modifier/isPublic (.getModifiers m)) (not (Modifier/isStatic (.getModifiers m)))
                                   (not (own (str (.getName m) (descriptor (.getParameterTypes m) (.getReturnType m))))))]
                    m)]
    (concat
      (for [^Method m (concat (.getDeclaredMethods c) inherited)
            :when (not (.isSynthetic m))
            :let [mods (.getModifiers m)
                  params (.getParameterTypes m)
                  g (method-go-name (.getName m) params (.getReturnType m))
                  desc (descriptor params (.getReturnType m))
                  static? (Modifier/isStatic mods)
                  fname (str t "_" g)
                  ok (cond
                       static? (or (contains? values fname) (contains? values (str fname "_native")))
                       (prom (str (.getName m) desc)) true
                       :else (or (has-method g) (contains? methods [t (str "Impl_" g)])))]]
        {:entry [:method (.getName m) desc (disj (flags mods) :native)]
         :sig (str (.getName m) desc)
         :ok ok
         :member m
         :ref (cond
                static? (list 'go/var '_ (symbol (if (contains? values fname) fname (str fname "_native"))))
                iface? (list 'go/var '_ (list 'method-expr (symbol t) (symbol g)))
                (or (has-method g) (prom (str (.getName m) desc)))
                (list 'go/var '_ (list 'method-expr (list '* (symbol t)) (symbol g)))
                :else (list 'go/var '_ (list 'method-expr (list '* (symbol t)) (symbol (str "Impl_" g)))))})
      (for [^Constructor k (.getDeclaredConstructors c)
            :when (not (.isSynthetic k))
            :let [params (.getParameterTypes k)
                  sfx (ctor-suffix params)
                  new-name (str t "_New" sfx)
                  ok (if (or abstract? non-leaf?)
                       (contains? methods [t (str "Ctor" sfx)])
                       (contains? values new-name))]]
        {:entry [:ctor (descriptor params nil) (disj (flags (.getModifiers k)) :native)]
         :sig (str "<init>" (descriptor params nil))
         :ok ok
         :member k
         :ref (if (or abstract? non-leaf?)
                (list 'go/var '_ (list 'method-expr (list '* (symbol t)) (symbol (str "Ctor" sfx))))
                (list 'go/var '_ (symbol new-name)))})
      (for [^Field f (.getDeclaredFields c)
            :when (and (Modifier/isStatic (.getModifiers f)) (not (.isSynthetic f)))
            :let [n (str t "_" (.getName f))]]
        {:entry [:field (.getName f) (subs (descriptor [(.getType f)] nil) 1 (- (count (descriptor [(.getType f)] nil)) 2)) (flags (.getModifiers f))]
         :sig (.getName f)
         :ok (contains? values n)
         :member f
         :ref (list 'go/var '_ (symbol n))}))))

(defn manifest [defs]
  (into (sorted-map)
        (for [[cname ifaces non-leaf?] hand-written
              :let [c (class-for cname)
                    ms (members cname non-leaf? defs)]]
          [(symbol cname)
           (cond-> {:flags (flags (.getModifiers c))
                    :super (some-> (.getSuperclass c) .getName symbol)
                    :interfaces (mapv symbol ifaces)
                    :members (vec (sort-by pr-str (map :entry (filter :ok ms))))}
             (promotable cname) (assoc :promotable (promotable cname)))])))

(defn check-forms [defs]
  (str ";; Generated by test/jrt/manifest.clj (bin/jrt manifest); do not edit.\n"
       ";; Refers to every member of manifest.edn by its Go name (C2G-SPEC §4.4), so that the build\n"
       ";; fails when jrt's forms and the manifest drift apart.\n"
       "(in-ns 'go.arbace.jrt)\n\n(go/file \"manifest_test.go\")\n\n"
       (str/join "\n"
                 (for [[cname _ non-leaf?] hand-written
                       m (sort-by :sig (members cname non-leaf? defs))
                       :when (:ok m)]
                   (pr-str (:ref m))))
       "\n"))

(defn coverage
  "The edge's members of the hand-written classes ([class sig] pairs from edge.edn) that jrt
  does not define."
  [defs edge-file]
  (when (.isFile (File. ^String edge-file))
    (let [edge (binding [*read-eval* false] (read-string (slurp edge-file)))
          classes (into {} (map (juxt first identity) hand-written))]
      (for [[cname info] (sort (:members edge))
            :when (classes cname)
            :let [[_ _ non-leaf?] (classes cname)
                  ok (set (map :sig (filter :ok (members cname non-leaf? defs))))]
            sig (sort (keys (:members info)))
            :when (not (ok (str/replace sig #"\)V$" ")V")))]
        [cname sig]))))

(defn -main [& [edge]]
  (let [defs (defined "go/arbace/jrt.clj")
        m (manifest defs)]
    (spit "go/arbace/jrt/manifest.edn"
          (str ";; Generated by test/jrt/manifest.clj (bin/jrt manifest); do not edit. Read with the Clojure reader.\n"
               ";; jrt's hand-written classes as c2g declares them (C2G-SPEC §9.1).\n"
               (with-out-str (pp/pprint m))))
    (spit "go/arbace/jrt/manifest_test.clj" (check-forms defs))
    (doseq [[cname ifaces non-leaf?] hand-written
            :let [ms (members cname non-leaf? defs)]]
      (println (format "%-36s %3d of %3d JDK members" cname (count (filter :ok ms)) (count ms))))
    (when edge
      (let [missing (coverage defs edge)]
        (println (str "edge members of these classes jrt does not define: " (count missing)))
        (doseq [[c s] missing] (println "  " c s))))
    (shutdown-agents)))
