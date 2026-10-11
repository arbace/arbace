(ns arbace.classes.env
  "The class environment (SPEC §9.2): what the compiler knows about classes, from the class
  forms being compiled, from classes it defined earlier and from the class path (by reflection,
  with constant values read from class files). Member lookup with Java's access rules (§5.6).
  Package class loaders with generations for the REPL (§10)."
  (:require [arbace.string :as str]
            [arbace.set]
            [arbace.classes.types :as t])
  (:import (arbace.asm ClassReader ClassVisitor Opcodes)
           (java.lang.reflect Field Method Constructor Modifier)))

;; A class info is a map:
;;   {:name "p/C" :flags int :super "java/lang/Object" :interfaces ["p/I" ...]
;;    :fields  [{:name "f" :desc "I" :flags int :owner "p/C" :const v-or-nil} ...]
;;    :methods [{:name "m" :desc "(I)V" :flags int :owner "p/C"} ...]   ; <init> included
;;    :nest-host "p/Outer" :outer "p/Outer"-or-nil}
;; Class declarations being compiled (see arbace.classes.parse) are class infos too.

(def ^:dynamic *compile-set*
  "An atom: internal name -> class declaration, the classes of the current compilation."
  nil)

(defonce ^{:doc "internal name -> class info of the classes this compiler defined or wrote."}
  defined (atom {}))

;; ---------------------------------------------------------------------------------------------
;; reflection

(defn- internal [^Class c] (str/replace (.getName c) "." "/"))

(def ^:dynamic *from-source*
  "nil, or a predicate on internal names: classes that are compiled from their sources and never
  taken from the class path, even when a class of that name is loaded (a stage recompiling
  itself: the runtime's own classes are the ones being compiled, SPEC §9.6)."
  nil)

(defn from-source? [n] (boolean (and *from-source* (*from-source* n))))

(def source-imports
  "An atom: namespace name -> {simple name symbol -> internal name}, the imports of classes that
  are not on the class path (yet) or are compiled from source, which namespaces cannot map."
  (atom {}))

(defn load-class
  "The Class of an internal name or array descriptor, without initializing it, or nil. nil for
  classes compiled from source (*from-source*)."
  [n]
  (when-not (from-source? n)
   (try
    (let [cn (if (str/starts-with? n "[") (str/replace n "/" ".") (str/replace n "/" "."))]
      (arbace.lang.RT/classForNameNonLoading cn))
    (catch ClassNotFoundException _ nil)
    (catch NoClassDefFoundError _ nil))))

(defn- read-fields
  "The fields of the class file of c, read with ClassReader: [{:name :desc :flags :value}], or
  nil without a class file. (Reflection hides some fields of the JDK's own classes,
  jdk.internal.reflect.Reflection's filters.)"
  [^Class c]
  (try
    (let [ld (or (.getClassLoader c) (ClassLoader/getSystemClassLoader))
          in (.getResourceAsStream ld (str (internal c) ".class"))]
      (when in
        (with-open [in in]
          (let [fields (atom [])
                cv (proxy [ClassVisitor] [Opcodes/ASM9]
                     (visitField [acc name desc sig value]
                       (swap! fields conj {:name name :desc desc :flags acc :value value})
                       nil))]
            (.accept (ClassReader. in) cv (bit-or ClassReader/SKIP_CODE ClassReader/SKIP_DEBUG))
            @fields))))
    (catch Exception _ nil)))

(defn- ann-reader
  "An AnnotationVisitor collecting values in the compiler's annotation data format into
  (put name value)."
  [put]
  (let [const (fn [v] (cond
                        (instance? arbace.asm.Type v) {:class (.getDescriptor ^arbace.asm.Type v)}
                        :else {:const {:val v :type (condp instance? v
                                                       Integer "I" Long "J" Short "S" Byte "B"
                                                       Character "C" Float "F" Double "D"
                                                       Boolean "Z" "Ljava/lang/String;")}}))]
    (proxy [arbace.asm.AnnotationVisitor] [Opcodes/ASM9]
      (visit [n v]
        (put n (if (and v (.isArray (class v)))
                 {:array (mapv const (seq v))}
                 (const v))))
      (visitEnum [n d v] (put n {:enum d :name v}))
      (visitAnnotation [n d]
        (let [vals (atom {})]
          (put n {:annotation {:type d :nested true :values vals}})
          (ann-reader #(swap! vals assoc %1 %2))))
      (visitArray [n]
        (let [xs (atom [])]
          (put n {:array xs})
          (ann-reader (fn [_ v] (swap! xs conj v))))))))

(defn- realize-ann [v]
  (cond
    (instance? arbace.lang.Atom v) (realize-ann @v)
    (map? v) (into {} (map (fn [[k x]] [k (realize-ann x)]) v))
    (vector? v) (mapv realize-ann v)
    :else v))

(defn read-method-annotations
  "[name desc] -> {:annotations [...] :param-annotations [[...] ...]} of the methods of the
  class file of c (the declaration annotations javac copies to bridges)."
  [^Class c]
  (try
    (let [ld (or (.getClassLoader c) (ClassLoader/getSystemClassLoader))
          in (.getResourceAsStream ld (str (internal c) ".class"))]
      (if-not in
        {}
        (with-open [in in]
          (let [ms (atom {})
                cv (proxy [ClassVisitor] [Opcodes/ASM9]
                     (visitMethod [acc name desc sig exs]
                       (let [k [name desc]
                             n (count (first (t/parse-method-desc desc)))
                             add (fn [path d vis]
                                   (let [vals (atom {})]
                                     (swap! ms update-in path (fnil conj []) {:type d :visible vis :values vals})
                                     (ann-reader #(swap! vals assoc %1 %2))))]
                         (swap! ms assoc k {:annotations [] :param-annotations (vec (repeat n []))})
                         (proxy [arbace.asm.MethodVisitor] [Opcodes/ASM9]
                           (visitAnnotation [d vis] (add [k :annotations] d vis))
                           (visitParameterAnnotation [i d vis] (add [k :param-annotations i] d vis))))))]
            (.accept (ClassReader. in) cv (bit-or ClassReader/SKIP_CODE ClassReader/SKIP_DEBUG))
            (realize-ann @ms)))))
    (catch Exception _ {})))

(defn- coerce-const [desc v]
  (case desc
    "Z" (not (zero? (long v)))
    "C" (char (int v))
    "S" (short v)
    "B" (byte v)
    v))

(def ^:private reflect-cache (java.util.WeakHashMap.))

(defn- inner-member? [^Class c]
  (boolean (and (.getDeclaringClass c) (not (Modifier/isStatic (.getModifiers c)))
                (not (.isInterface c)) (not (.isEnum c)) (not (.isRecord c)))))

(defn reflect-info [^Class c]
  (locking reflect-cache
    (or (.get reflect-cache c)
        (let [n (internal c)
              manns (delay (read-method-annotations c))
              info {:name n
                    :flags (.getModifiers c)
                    :interface? (.isInterface c)
                    :super (some-> (.getSuperclass c) internal)
                    :interfaces (mapv internal (.getInterfaces c))
                    :nest-host (internal (.getNestHost c))
                    :outer (some-> (.getDeclaringClass c) internal)
                    :fields (if-let [fs (read-fields c)]
                              (vec (for [{:keys [name desc flags value]} fs]
                                     {:name name :desc desc :flags flags :owner n
                                      :const (when (and (Modifier/isFinal flags) (some? value)
                                                        (or (t/prim? desc) (= desc t/string-desc)))
                                               (coerce-const desc value))}))
                              (vec (for [^Field f (.getDeclaredFields c)]
                                     {:name (.getName f) :desc (t/class->desc (.getType f))
                                      :flags (.getModifiers f) :owner n :const nil})))
                    ;; an inner member class: its constructors take the outer instance first
                    :outer-instance? (inner-member? c)
                    :methods (vec (concat
                                    (for [^Method m (.getDeclaredMethods c)
                                          :let [desc (t/method-desc (map t/class->desc (.getParameterTypes m))
                                                                    (t/class->desc (.getReturnType m)))]]
                                      {:name (.getName m)
                                       :desc desc
                                       :flags (.getModifiers m) :owner n
                                       :throws (mapv internal (.getExceptionTypes m))
                                       :annotations (delay (get-in @manns [[(.getName m) desc] :annotations]))
                                       :param-annotations (map-indexed
                                                           (fn [i _] (delay (get-in @manns [[(.getName m) desc] :param-annotations i])))
                                                           (.getParameterTypes m))})
                                    (for [^Constructor m (.getDeclaredConstructors c)]
                                      {:name "<init>"
                                       :desc (t/method-desc (map t/class->desc (cond->> (seq (.getParameterTypes m))
                                                                                  (inner-member? c) rest))
                                                            "V")
                                       :flags (.getModifiers m) :owner n
                                       :throws (mapv internal (.getExceptionTypes m))})))
                    :class c}]
          (.put reflect-cache c info)
          info))))

;; ---------------------------------------------------------------------------------------------
;; lookup

(defn info
  "The class info of an internal name, or nil."
  [n]
  (when (and n (not (str/starts-with? n "[")))
    (or (some-> *compile-set* deref (get n))
        (when-let [c (load-class n)]
          (let [d (get @defined n)]
            (if (and d (identical? (:class d) c)) d (reflect-info c)))))))

(defn info! [n]
  (or (info n) (throw (ex-info (str "Unknown class: " (str/replace (str n) "/" ".")) {:class n}))))

(defn interface? [n]
  (let [i (info n)] (boolean (or (:interface? i) (some-> (:flags i) (bit-and Opcodes/ACC_INTERFACE) pos?)))))

(defn const-value [field]
  (let [c (:const field)] (if (delay? c) @c c)))

(defn supertypes
  "Direct supertypes of class n (superclass first)."
  [n]
  (let [i (info! n)] (cond-> (vec (:interfaces i)) (:super i) (->> (cons (:super i)) vec))))

(defn subclass?
  "Is class a (internal name) a subtype of class b?"
  [a b]
  (or (= a b) (= b "java/lang/Object")
      (boolean (some #(subclass? % b) (when-let [i (info a)]
                                         (cond-> (vec (:interfaces i)) (:super i) (conj (:super i))))))))

(defn assignable?
  "Is a value of reference type `from` (a descriptor or :null) assignable to `to`?"
  [from to]
  (cond
    (= from to) true
    (= from :null) (t/ref? to)
    (not (and (t/ref? from) (t/ref? to))) false
    (= to t/object-desc) true
    (t/array? from)
    (cond (t/array? to) (let [fe (t/elem-type from) te (t/elem-type to)]
                          (and (t/ref? fe) (t/ref? te) (assignable? fe te)))
          :else (contains? #{"Ljava/lang/Cloneable;" "Ljava/io/Serializable;"} to))
    (t/array? to) false
    :else (subclass? (t/desc->internal from) (t/desc->internal to))))

(defn superclass-chain [n]
  (take-while some? (iterate #(some-> % info :super) n)))

(defn common-super
  "The common superclass of two internal names, for stack map frames."
  [a b]
  (cond
    (= a b) a
    (or (str/starts-with? a "[") (str/starts-with? b "[")) "java/lang/Object"
    (subclass? a b) (if (interface? b) "java/lang/Object" b)
    (subclass? b a) (if (interface? a) "java/lang/Object" a)
    (or (interface? a) (interface? b)) "java/lang/Object"
    :else (let [bs (set (superclass-chain b))]
            (or (first (filter bs (superclass-chain a))) "java/lang/Object"))))

(defn lub
  "A least upper bound of two reference descriptors (only the class chain; interfaces give
  Object unless one type is assignable to the other)."
  [a b]
  (cond (= a b) a
        (= a :null) b
        (= b :null) a
        (assignable? a b) b
        (assignable? b a) a
        (and (t/class-desc? a) (t/class-desc? b))
        (let [c (common-super (t/desc->internal a) (t/desc->internal b))]
          (if (= c "java/lang/Object")
            ;; javac's lub: the common interface, when there is one most specific one
            (let [sups (fn [n] (set (tree-seq some? (fn [x] (when-let [i (info x)]
                                                             (cond-> (vec (:interfaces i)) (:super i) (conj (:super i)))))
                                              n)))
                  common (filter interface? (arbace.set/intersection (sups (t/desc->internal a))
                                                                     (sups (t/desc->internal b))))
                  minimal (remove (fn [i] (some #(and (not= % i) (subclass? % i)) common)) common)]
              (if (= 1 (count minimal)) (t/internal->desc (first minimal)) t/object-desc))
            (t/internal->desc c)))
        :else t/object-desc))

;; access

(defn nest-host [n]
  (let [i (info n)]
    (or (:nest-host i)
        (if-let [o (:outer i)] (nest-host o) n))))

(defn- flag? [flags f] (not (zero? (bit-and flags f))))

(defn accessible?
  "Can code in class `from` access member m (with :owner and :flags)?"
  [from m]
  (let [fl (:flags m) owner (:owner m)]
    (cond
      (flag? fl Opcodes/ACC_PUBLIC) true
      (flag? fl Opcodes/ACC_PRIVATE) (= (nest-host from) (nest-host owner))
      (= (t/package-of from) (t/package-of owner)) true
      (flag? fl Opcodes/ACC_PROTECTED)
      (boolean (some #(subclass? % owner)
                     (take-while some? (iterate #(:outer (info %)) from))))
      :else false)))

(defn find-field
  "The field `name` of class n as JLS 8.3 finds it (declared, then superinterfaces, then
  superclass), or nil."
  [n name]
  (when-let [i (info n)]
    (or (first (filter #(= name (:name %)) (:fields i)))
        (some #(find-field % name) (:interfaces i))
        (some-> (:super i) (find-field name)))))

(defn all-supertypes
  "n and all its supertypes, classes first (the superclass chain), then interfaces breadth first."
  [n]
  (let [chain (vec (superclass-chain n))]
    (loop [out chain seen (set chain) queue (vec (mapcat #(:interfaces (info %)) chain))]
      (if-let [[q & more] (seq queue)]
        (if (seen q)
          (recur out seen (vec more))
          (recur (conj out q) (conj seen q) (into (vec more) (:interfaces (info q)))))
        (cond-> out (and (interface? n) (not (seen "java/lang/Object"))) (conj "java/lang/Object"))))))

(defn- params-key [^String desc] (subs desc 0 (inc (.indexOf desc ")"))))

(defn member-methods
  "The methods named `name` that are members of class n (inherited ones included, overridden
  and synthetic ones excluded), in order: most specific declaring class first."
  [n name]
  (loop [[c & more] (all-supertypes n) seen #{} out []]
    (if-not c
      out
      (let [ms (for [m (:methods (info! c))
                     :when (= name (:name m))
                     :when (not (flag? (:flags m) Opcodes/ACC_SYNTHETIC))
                     :when (or (= c n) (not (flag? (:flags m) Opcodes/ACC_PRIVATE)))
                     :when (or (= c n) (not (interface? c)) (not (flag? (:flags m) Opcodes/ACC_STATIC)))
                     :when (not (seen (params-key (:desc m))))]
                 m)]
        (recur more (into seen (map (comp params-key :desc) ms)) (into out ms))))))

(defn constructors [n]
  (filter #(= "<init>" (:name %)) (:methods (info! n))))

;; ---------------------------------------------------------------------------------------------
;; package class loaders (§10)

(defonce ^{:doc "package -> the current DynamicClassLoader of that package"}
  package-loaders (atom {}))

(defn- new-loader []
  (arbace.lang.DynamicClassLoader. (.getClassLoader arbace.lang.RT)))

(defn define-classes!
  "Defines classes [{:name internal :bytes byte[] :info info}] in their package loaders,
  starting a new generation of a package when one of its classes was defined already.
  Supertypes in the set are defined first. Records the infos. Returns name -> Class."
  [classes]
  (let [by-name (into {} (map (juxt :name identity) classes))
        pkgs (set (map (comp t/package-of :name) classes))]
    (doseq [p pkgs]
      (let [ld (get @package-loaders p)
            redefining (some (fn [{n :name}]
                               (and (= p (t/package-of n))
                                    (when-let [d (get @defined n)] (= (:loader d) ld))))
                             classes)]
        (when (or (nil? ld) redefining)
          (swap! package-loaders assoc p (new-loader)))))
    (let [done (atom {})]
      (letfn [(define [n]
                (when-not (contains? @done n)
                  (swap! done assoc n nil)
                  (let [{:keys [bytes info]} (by-name n)]
                    (doseq [s (cons (:super info) (:interfaces info))
                            :when (by-name s)]
                      (define s))
                    (let [ld ^arbace.lang.DynamicClassLoader (get @package-loaders (t/package-of n))
                          c (.defineClass ld (str/replace n "/" ".") ^bytes bytes nil)]
                      (swap! defined assoc n (assoc info :class c :loader ld))
                      (swap! done assoc n c)))))]
        (doseq [{n :name} classes] (define n))
        @done))))

(defn record-written!
  "Records the infos of classes written to class files (AOT)."
  [classes]
  (doseq [{n :name i :info} classes]
    (swap! defined assoc n i)))
