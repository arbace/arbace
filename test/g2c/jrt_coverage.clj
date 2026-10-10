(ns g2c.jrt-coverage
  "How much of java.base's exported API the Go build implements (doc/go/JAVA-BASE.md). Run by
  bin/jrt-coverage.

  The API is read by reflection on the running JDK (26, built from jdk26u): the packages
  java.base exports to everyone, their classes (the module image's class files, jrt:/), the
  public ones and the public or protected nested ones of accessible classes, and of each its
  declared public and protected methods, constructors and fields (protected ones only in
  classes that are not final), not synthetic, not bridges. Members are named as the JVM names
  them: name and descriptor.

  What the Go build provides is read from c2g's output for the program (bin/arbace-go
  --build: target/arbace-go/c2g): its report (the translated classes, the methods with
  operation-level stubs), the Go forms it wrote (the stubs of methods not translated, the cut
  classes, the Go names defined), and from jrt's hand-written forms (go/arbace/jrt: the classes
  they define; manifest.edn: the members). A class is
    :translated    c2g translated it from jdk26u's source (or jrt's overlay Java)
    :declared      c2g declared it from reflection (a JDK interface without source: its
                   abstract methods, defaults and statics as stubs)
    :hand-written  jrt defines it in hand-written Go
    :cut           a name only: registered for the evaluator, its members throw
                   UnsupportedOperationException (D6, C2G-SPEC §4.1, amendment M3)
    :absent        not in the Go build
  and a member
    :translated    translated with its body (or abstract: declared in the Go interface)
    :partial       translated, but an operation in it names something outside the build and
                   throws when reached (c2g's report, :unavailable)
    :stub          exists, throws (c2g: not translated: not reached from the program's roots,
                   or its translation failed)
    :hand-written  jrt implements it (the manifest, or the Go name in the program)
    :outside-type  not in Go: its descriptor names a class outside the build
    :cut           a member of a cut class (named, throws)
    :absent        not in the build (a class's member jrt does not implement, or of an absent
                   class)

  Arbace's use of the API is read from doc/go/java-surface.edn (bin/java-surface): the members
  the runtime and the namespaces loaded at a REPL's start reference (groups :runtime and :repl).

  Deterministic: the output depends on the JDK, c2g's output and jrt's sources."
  (:require [arbace.java.io :as io]
            [arbace.string :as str]
            [arbace.set :as set]
            [arbace.pprint :as pp]
            [arbace.c2g.names :as nm]
            [arbace.c2g.jrt :as jrt]
            [arbace.g2c.print :as gp])
  (:import (java.lang.reflect Modifier Method Constructor Field)
           (java.net URI)
           (java.nio.file FileSystems Files Path)))

;; ---------------------------------------------------------------------------------------
;; the API, by reflection

(defn exported-packages
  "The packages java.base exports to every module, sorted."
  []
  (let [m (.get (.findModule (ModuleLayer/boot) "java.base"))]
    (sort (for [e (.exports (.getDescriptor m)) :when (not (.isQualified e))] (.source e)))))

(defn- package-class-names
  "The binary names of the classes of package p in java.base's image."
  [p]
  (let [fs (FileSystems/getFileSystem (URI. "jrt:/"))
        dir (.getPath fs (str "/modules/java.base/" (str/replace p "." "/")) (make-array String 0))]
    (with-open [s (Files/list dir)]
      (vec (sort (for [^Path f (iterator-seq (.iterator s))
                       :let [n (str (.getFileName f))]
                       :when (and (str/ends-with? n ".class") (not= n "package-info.class")
                                  (not= n "module-info.class"))]
                   (str p "." (subs n 0 (- (count n) 6)))))))))

(defn- accessible-class?
  "Is class c part of the API: public, or a public or protected member class, all its
  enclosing classes so too; not synthetic, anonymous or local."
  [^Class c]
  (and (not (.isSynthetic c)) (not (.isAnonymousClass c)) (not (.isLocalClass c))
       (let [m (.getModifiers c)]
         (if-let [d (.getDeclaringClass c)]
           (and (or (Modifier/isPublic m) (and (Modifier/isProtected m) (not (Modifier/isFinal (.getModifiers d)))))
                (accessible-class? d))
           (Modifier/isPublic m)))))

(defn- api-member? [^Class c mods synthetic]
  (and (not synthetic)
       (or (Modifier/isPublic mods)
           (and (Modifier/isProtected mods) (not (Modifier/isFinal (.getModifiers c)))))))

(defn- mdesc [ps ^Class r]
  (str "(" (apply str (map #(.descriptorString ^Class %) ps)) ")" (.descriptorString r)))

(defn- class-kind [^Class c]
  (cond (.isAnnotation c) :annotation (.isInterface c) :interface (.isEnum c) :enum
        (.isRecord c) :record :else :class))

(defn api-members
  "The API members of class c: [kind name desc abstract?], sorted (fields' desc is their type)."
  [^Class c]
  (vec (sort
         (concat
           (for [^Method m (.getDeclaredMethods c)
                 :when (and (not (.isBridge m)) (api-member? c (.getModifiers m) (.isSynthetic m)))]
             [:method (.getName m) (mdesc (.getParameterTypes m) (.getReturnType m))
              (Modifier/isAbstract (.getModifiers m))])
           (for [^Constructor k (.getDeclaredConstructors c)
                 :when (api-member? c (.getModifiers k) (.isSynthetic k))]
             [:ctor "<init>" (mdesc (.getParameterTypes k) Void/TYPE) false])
           (for [^Field f (.getDeclaredFields c)
                 :when (api-member? c (.getModifiers f) (.isSynthetic f))]
             [:field (.getName f) (.descriptorString (.getType f)) false])))))

(defn api
  "{package [{:name :internal :kind :members}]} of java.base's exported API."
  []
  (into (sorted-map)
        (for [p (exported-packages)]
          [p (vec (for [n (package-class-names p)
                        :let [c (Class/forName n false nil)]
                        :when (accessible-class? c)]
                    {:name n :internal (str/replace n "." "/") :kind (class-kind c)
                     :members (api-members c)}))])))

;; ---------------------------------------------------------------------------------------
;; the member tables of jrt's hand-written classes (reflection, C2G-SPEC §5.11): jrt's own
;; (reflect_tables) and those c2g adds where jrt's class needs translated types
;; (reflect_tables_c2g, c2g_support)

(def ^:private prim-descs
  {"Prim_boolean" "Z" "Prim_byte" "B" "Prim_char" "C" "Prim_short" "S" "Prim_int" "I"
   "Prim_long" "J" "Prim_float" "F" "Prim_double" "D" "Prim_void" "V"})

(defn- class-expr-desc
  "The descriptor of a Class expression of a member table (Prim_int, String_class,
  (.ArrayClass e)), or nil."
  [var->class x]
  (cond
    (symbol? x) (let [s (name x)]
                  (or (prim-descs s)
                      (when (= s "Object_class") "Ljava/lang/Object;")
                      (when-let [c (var->class s)] (str "L" c ";"))))
    (and (seq? x) (= '.ArrayClass (first x))) (when-let [d (class-expr-desc var->class (second x))] (str "[" d))
    :else nil))

(defn- table-entries
  "[kind kvs] of the (lit MethodInfo|CtorInfo|FieldInfo ...) in form x."
  [x]
  (cond
    (and (seq? x) (= 'lit (first x)) ('#{MethodInfo CtorInfo FieldInfo} (second x)))
    [[({'MethodInfo :method 'CtorInfo :ctor 'FieldInfo :field} (second x)) (apply hash-map (drop 2 x))]]
    (seq? x) (mapcat table-entries x)
    (vector? x) (mapcat table-entries x)
    :else nil))

(defn table-members
  "#{[internal kind name desc]} of the member tables jrt's hand-written files and c2g's support
  file of the program's jrt directory set (desc nil for fields)."
  [dir var->class]
  (set (for [f ["reflect_tables.clj" "reflect_tables_c2g.clj" "c2g_support.clj"]
             :let [file (io/file dir f)] :when (.exists file)
             form (gp/read-forms file)
             :when (and (seq? form) (= "func" (name (first form))) (= 'init (second form)))
             st (drop 3 form)
             :when (and (seq? st) (= 'set! (first st)))
             :let [target (second st)
                   info (when (seq? target) (second target))
                   cls (when (and (seq? info) (= '.Info (first info))) (second info))
                   v (nth st 2 nil)
                   c (when (symbol? cls) (var->class (name cls)))]
             :when c
             [kind kvs] (table-entries v)
             :let [ps (let [p (:Params kvs)] (if (and (seq? p) (= 'lit (first p))) (drop 2 p) []))
                   pd (map #(class-expr-desc var->class %) ps)
                   r (case kind :method (class-expr-desc var->class (:Return kvs)) :ctor "V" nil)]
             :when (and (every? some? pd) (or (= kind :field) r))]
         [c kind (if (= kind :ctor) "<init>" (:Name kvs))
          (when-not (= kind :field) (str "(" (apply str pd) ")" r))])))

;; ---------------------------------------------------------------------------------------
;; the Go build

(defn- slurp-dir
  "The texts of the .clj files of a directory, by file name."
  [dir]
  (into (sorted-map)
        (for [^java.io.File f (sort-by #(.getName ^java.io.File %) (.listFiles (io/file dir)))
              :when (str/ends-with? (.getName f) ".clj")]
          [(.getName f) (slurp f)])))

(defn- parse-stub
  "\"java.util.TreeMap$SubMap.readResolve()Ljava/lang/Object;[: why]\" -> [internal name desc]."
  [^String s]
  (let [s (first (str/split s #": " 2))
        i (.indexOf s "(")
        j (.lastIndexOf s "." i)]
    (when (and (pos? i) (pos? j))
      [(str/replace (subs s 0 j) "." "/") (subs s (inc j) i) (subs s i)])))

(defn- parse-unavailable
  "\"java/util/X name desc\" -> [internal name desc]."
  [^String s]
  (let [[c n d] (str/split s #" " 3)] [c n (or d "")]))

(defn- overlay-tops
  "The top-level classes (internal names) bin/jrt-convert takes from jrt's own Java (KIND overlay
  in its sources.txt), when that file exists."
  [sources-txt]
  (let [f (io/file sources-txt)]
    (if (.isFile f)
      (set (for [l (str/split-lines (slurp f))
                 :let [[_ kind rel] (str/split l #" ")]
                 :when (= kind "overlay")]
             (str/replace rel #"\.java$" "")))
      #{})))

(defn go-build
  "What the Go build provides, from c2g's output directory c2g-out, jrt's forms jrt-dir and
  bin/jrt-convert's sources.txt."
  [c2g-out jrt-dir sources-txt]
  (let [report (read-string (slurp (str c2g-out "/report.edn")))
        prog (str c2g-out "/prog/go/arbace")
        texts (merge (update-keys (slurp-dir (str prog "/jrt")) #(str "jrt/" %))
                     (update-keys (slurp-dir (str prog "/lang")) #(str "lang/" %)))
        stubs (set (for [[_ t] texts
                         [_ s] (re-seq #"C2g_NotTranslated \"((?:[^\"\\]|\\.)*)\"" t)
                         :let [k (parse-stub s)] :when k]
                     k))
        cuts (set (for [[f t] texts :when (str/ends-with? f "c2g_cut.clj")
                        [_ n] (re-seq #":Name \"([^\"]+)\"[^\n]*\(cut\)\"" t)]
                    (str/replace n "." "/")))
        declared (set (for [[_ t] texts
                            [_ n] (re-seq #"\(c2g/comment \"---- ([^ ]+) \(declared from reflection\)\"\)" t)]
                        (str/replace n "." "/")))
        hw-scan (jrt/scan jrt-dir)
        manifest (read-string (slurp (str jrt-dir "/manifest.edn")))
        prog-scan (jrt/scan (str prog "/jrt"))
        var->class (into {} (for [[jn v] (:classes prog-scan)] [(:var v) (str/replace jn "." "/")]))]
    {:translated (set (:translated report))
     :overlay (overlay-tops sources-txt)
     :declared declared
     ;; as c2g's main/jrt-classes: those jrt's forms register (Define) and those its manifest
     ;; declares
     :jrt-classes (merge (into {} (for [c (keys manifest)
                                        :let [n (str/replace (str c) "." "/")]]
                                    [n {:go (nm/go-class-name n)}]))
                         (into {} (for [[jn v] (:classes hw-scan)] [(str/replace jn "." "/") v])))
     :manifest (into {} (for [[c v] manifest]
                          [(str/replace (str c) "." "/")
                           (set (for [[k n d] (:members v)] [k (if (= k :ctor) "<init>" n) d]))]))
     :cuts cuts
     :stubs stubs
     :partial (set (map parse-unavailable (keys (:unavailable report))))
     :prog-methods (:methods prog-scan)
     :prog-funcs (:funcs prog-scan)
     :prog-vars (set/union (:vars prog-scan) (:consts prog-scan))
     :tables (table-members (str prog "/jrt") var->class)
     :report-counts (select-keys report [:classes :reached :world :methods-with-missing-parts])}))

;; ---------------------------------------------------------------------------------------
;; classification

(defn- desc-classes
  "The internal names of the classes in a descriptor."
  [^String d]
  (map second (re-seq #"L([^;]+);" d)))

(defn class-status [b n]
  (cond
    (contains? (:declared b) n) :declared
    (contains? (:translated b) n) :translated
    (or (= n "java/lang/Object") (contains? (:jrt-classes b) n)) :hand-written
    (contains? (:cuts b) n) :cut
    :else :absent))

(defn- in-world? [b n]
  (or (= n "java/lang/Object") (contains? (:translated b) n) (contains? (:jrt-classes b) n)))

(defn- go-name [b n]
  (if (and (not (contains? (:translated b) n)) (get (:jrt-classes b) n))
    (:go (get (:jrt-classes b) n))
    (nm/go-class-name n)))

(defn- go-defined?
  "Does the program define member [kind name desc] of class n under its Go name (C2G-SPEC
  §4.4)? nil when the check does not apply (instance fields, abstract methods)."
  [b n [kind name desc abstract] static?]
  (let [g (go-name b n)
        ms (get (:prog-methods b) g #{})
        fs (:prog-funcs b)]
    (case kind
      :method (when-not abstract
                (let [base (nm/method-base name desc)]
                  (if static?
                    (contains? fs (str g "_" base))
                    (or (contains? ms base) (contains? ms (str "Impl_" base))
                        (contains? fs (str g "_" base))))))
      :ctor (or (contains? fs (nm/new-name g desc)) (contains? ms (nm/ctor-base desc)))
      :field (when static? (contains? (:prog-vars b) (nm/static-field-name g name))))))

(def ^:private object-members
  "Object's members jrt implements (C2G-SPEC §5.8; finalize is not used, V8)."
  #{"<init>" "getClass" "hashCode" "equals" "clone" "toString" "notify" "notifyAll" "wait"})

(defn member-status [b n cstatus [kind name desc :as m] static?]
  (let [k [kind name desc]
        mk [n name desc]]
    (case cstatus
      (:translated :declared)
      (cond
        (not (every? #(in-world? b %) (desc-classes desc))) :outside-type
        ;; not in the translated source (jrt's own Java in overlay/ has a subset of the JDK
        ;; class's members; a variant cut it)
        (false? (go-defined? b n m static?)) :absent
        (contains? (:stubs b) mk) :stub
        (contains? (:partial b) mk) :partial
        :else :translated)
      :hand-written
      (cond
        (= n "java/lang/Object") (if (object-members name) :hand-written :absent)
        (contains? (get (:manifest b) n #{}) k) :hand-written
        (contains? (:tables b) [n kind name (when-not (= kind :field) desc)]) :hand-written
        (and (every? #(in-world? b %) (desc-classes desc)) (go-defined? b n m static?)) :hand-written
        :else :absent)
      :cut :cut
      :absent :absent)))

(defn measure
  "The coverage: per package, per class, per member."
  [api b]
  (into (sorted-map)
        (for [[p classes] api]
          [p (vec (for [{:keys [name internal kind members]} classes
                        :let [c (Class/forName name false nil)
                              cs (class-status b internal)
                              statics (set (concat
                                             (for [^Method m (.getDeclaredMethods c) :when (Modifier/isStatic (.getModifiers m))]
                                               [:method (.getName m) (mdesc (.getParameterTypes m) (.getReturnType m))])
                                             (for [^Field f (.getDeclaredFields c) :when (Modifier/isStatic (.getModifiers f))]
                                               [:field (.getName f) (.descriptorString (.getType f))])))]]
                    {:name name :kind kind :status cs
                     :overlay (and (= cs :translated)
                                   (contains? (:overlay b) (first (str/split internal #"\$"))))
                     :members (vec (for [[kind mname desc abstract :as m] members
                                         :let [static? (contains? statics [kind mname desc])
                                               st (member-status b internal cs m static?)]]
                                     (cond-> {:kind kind :name mname :desc desc :status st}
                                       abstract (assoc :abstract true)
                                       static? (assoc :static true)
                                       ;; the cross-check: c2g's Go name exists exactly when the
                                       ;; member is in Go
                                       (#{:translated :declared} cs)
                                       (assoc :go-defined (go-defined? b internal m static?)))))}))])))

;; ---------------------------------------------------------------------------------------
;; Arbace's use (java-surface.edn)

(defn- surface-key
  "A java-surface member string (\"name(desc)R\" or a field name) of class c (dotted) as
  [internal name desc-or-nil]."
  [c ^String s]
  (let [i (.indexOf s "(")]
    (if (neg? i)
      [(str/replace c "." "/") s nil]
      [(str/replace c "." "/") (subs s 0 i) (subs s i)])))

(defn used-members
  "The JDK members Arbace's runtime and REPL namespaces reference: #{[internal name desc]}
  (desc nil for fields), attributed to their declaring class."
  [surface]
  (set (for [[c ci] (:classes surface)
             g [:runtime :repl]
             [s mi] (get-in ci [:groups g :members])]
         (surface-key (or (:decl mi) c) s))))

;; ---------------------------------------------------------------------------------------
;; counting

(def member-statuses [:translated :partial :hand-written :stub :outside-type :cut :absent])
(def class-statuses [:translated :declared :hand-written :cut :absent])
(def provided #{:translated :partial :hand-written})

(defn- count-by [k xs] (merge (zipmap (if (= k :class) class-statuses member-statuses) (repeat 0))
                              (frequencies (map :status xs))))

(defn- summarize [classes]
  (let [ms (mapcat :members classes)]
    {:classes (count classes)
     :classes-by-status (count-by :class classes)
     :members (count ms)
     :members-by-status (count-by :member ms)
     :members-by-kind (into (sorted-map)
                            (for [[k xs] (group-by :kind ms)]
                              [k {:all (count xs) :provided (count (filter #(provided (:status %)) xs))}]))
     :provided (count (filter #(provided (:status %)) ms))
     ;; the classes the Go build has: how complete they are
     :in-go (let [cs (filter #(#{:translated :declared :hand-written} (:status %)) classes)
                  ims (mapcat :members cs)]
              {:classes (count cs) :members (count ims)
               :provided (count (filter #(provided (:status %)) ims))})}))

(defn- top-package
  "The family a package is counted in for the headline table."
  [p]
  (let [fams ["java.util.concurrent" "java.util.regex" "java.util.stream" "java.util.function"
              "java.lang.invoke" "java.lang.reflect" "java.lang.constant" "java.lang.classfile"
              "java.lang.foreign" "java.lang.ref" "java.lang.annotation" "java.lang.runtime"
              "java.lang.module" "java.nio.file" "java.nio.channels" "java.nio.charset"
              "java.time" "java.text" "java.math" "java.net" "java.security" "java.io"
              "java.nio" "java.util" "java.lang" "javax.crypto" "javax.net" "javax.security"]]
    (or (some #(when (or (= p %) (str/starts-with? p (str % "."))) %) fams) p)))

(defn result
  "The data written: totals, per package, per family, per class, and Arbace's use."
  [cov used]
  (let [all (mapcat val cov)
        member-rows (for [c all m (:members c)] [c m])
        used-rows (filter (fn [[c m]]
                            (contains? used [(str/replace (:name c) "." "/") (:name m)
                                             (when-not (= :field (:kind m)) (:desc m))]))
                          member-rows)
        mismatch (for [[c m] member-rows
                       :when (contains? m :go-defined)
                       :let [in-go (#{:translated :partial :stub} (:status m))
                             gd (:go-defined m)]
                       :when (and (some? gd) (not= (boolean in-go) gd))]
                   [(:name c) (:name m) (:desc m) (:status m)])]
    {:about "bin/jrt-coverage: java.base's exported API in the Go build (doc/go/JAVA-BASE.md)"
     :jdk (System/getProperty "java.runtime.version")
     :total (summarize all)
     :used {:members (count used-rows)
            :members-by-status (count-by :member (map second used-rows))
            :not-provided (vec (sort (for [[c m] used-rows :when (not (provided (:status m)))]
                                       [(:name c) (str (:name m) (when-not (= :field (:kind m)) (:desc m))) (:status m)])))}
     :families (into (sorted-map) (for [[f ps] (group-by top-package (keys cov))]
                                    [f (summarize (mapcat cov ps))]))
     :packages (into (sorted-map) (for [[p cs] cov] [p (summarize cs)]))
     :classes (into (sorted-map)
                    (for [c all]
                      [(:name c) (let [s (count-by :member (:members c))]
                                   (into [(:status c) (count (:members c))]
                                         (for [k member-statuses] (get s k))))]))
     :overlay-classes (vec (for [c all :when (:overlay c)]
                             [(:name c) (count (:members c))
                              (count (filter #(provided (:status %)) (:members c)))]))
     :cross-check {:mismatches (count mismatch) :examples (vec (take 40 (sort mismatch)))}}))

;; ---------------------------------------------------------------------------------------
;; the human summary

(defn- pct [a b] (if (zero? b) "-" (format "%.1f%%" (* 100.0 (/ (double a) b)))))
(defn- fmt [n] (format "%,d" n))
(defn- row [& xs] (str "| " (str/join " | " xs) " |"))

(defn- summary-row [label s]
  (let [cs (:classes-by-status s) ms (:members-by-status s)]
    (row label (fmt (:classes s))
         (fmt (+ (:translated cs) (:declared cs) (:hand-written cs)))
         (fmt (:cut cs))
         (fmt (:members s))
         (fmt (:provided s)) (pct (:provided s) (:members s))
         (fmt (:translated ms)) (fmt (:partial ms)) (fmt (:hand-written ms))
         (fmt (:stub ms)) (fmt (:outside-type ms)) (fmt (+ (:cut ms) (:absent ms))))))

(def ^:private summary-head
  (str (row "" "classes" "in Go" "cut" "members" "provided" "%" "translated" "partial"
            "hand-written" "stub" "outside type" "cut or absent") "\n"
       "|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|"))

(defn summary
  "Markdown tables of the result r."
  [r]
  (let [out (java.io.StringWriter.)
        p (fn [& xs] (.write out (str (apply str xs) "\n")))]
    (p "# java.base in the Go build (bin/jrt-coverage)\n")
    (p "JDK " (:jdk r) ". Classes: the API's public classes and accessible member classes of the "
       "exported packages; \"in Go\": translated, declared or hand-written. Members: their public "
       "and protected methods, constructors and fields; \"provided\": translated (with a body), "
       "partial or hand-written.\n")
    (p "## Overall\n")
    (p summary-head)
    (p (summary-row "**java.base**" (:total r)))
    (let [g (:in-go (:total r))]
      (p "\nThe " (fmt (:classes g)) " classes in Go have " (fmt (:members g)) " API members, of which "
         (fmt (:provided g)) " (" (pct (:provided g) (:members g)) ") are provided."))
    (p "\nOf the translated classes, " (count (:overlay-classes r)) " are jrt's own Java (overlay/, "
       "bin/jrt-convert's KIND overlay), not jdk26u's: "
       (str/join ", " (for [[c m pv] (:overlay-classes r)] (str "`" c "` " pv "/" m))) ".")
    (p "\n## By member kind\n")
    (p (row "kind" "members" "provided" "%"))
    (p "|---|---:|---:|---:|")
    (doseq [[k {:keys [all provided]}] (:members-by-kind (:total r))]
      (p (row (name k) (fmt all) (fmt provided) (pct provided all))))
    (p "\n## By package family\n")
    (p summary-head)
    (doseq [[f s] (sort-by (comp - :members val) (:families r))]
      (p (summary-row (str "`" f "`") s)))
    (p "\n## By package\n")
    (p summary-head)
    (doseq [[pk s] (:packages r)]
      (p (summary-row (str "`" pk "`") s)))
    (let [u (:used r) ms (:members-by-status u)]
      (p "\n## Arbace's use (java-surface.edn: the runtime and the REPL's namespaces)\n")
      (p (row "members used" "translated" "partial" "hand-written" "stub" "outside type" "cut" "absent"))
      (p "|---:|---:|---:|---:|---:|---:|---:|---:|")
      (p (row (fmt (:members u)) (fmt (:translated ms)) (fmt (:partial ms)) (fmt (:hand-written ms))
              (fmt (:stub ms)) (fmt (:outside-type ms)) (fmt (:cut ms)) (fmt (:absent ms))))
      (p "\nUsed and not provided:\n")
      (doseq [[c m st] (:not-provided u)]
        (p "- `" c "." m "` " (name st))))
    (p "\n## Cross-check\n")
    (p "Members whose status and c2g's Go name in the program disagree: "
       (:mismatches (:cross-check r)) ".")
    (doseq [x (:examples (:cross-check r))] (p "- " (pr-str x)))
    (str out)))

;; ---------------------------------------------------------------------------------------
;; the run

(defn- write-edn [f header x]
  (io/make-parents f)
  (spit f (str header (with-out-str (binding [*print-length* nil *print-level* nil] (pp/pprint x))))))

(defn run
  "Measures and writes out-edn (the result), details-edn (every class and member) and
  summary-md. Returns the result."
  [root c2g-out sources-txt surface-file out-edn details-edn summary-md]
  (let [a (api)
        b (go-build c2g-out (str root "/go/arbace/jrt") sources-txt)
        cov (measure a b)
        used (used-members (read-string (slurp surface-file)))
        r (result cov used)
        header ";; Generated by bin/jrt-coverage; do not edit. Read with arbace.core/read.\n"]
    ;; one map; its tables (:families, :packages, :classes) last, an entry per line
    (io/make-parents out-edn)
    (let [tables [:families :packages :classes]
          head (str/trimr (with-out-str
                            (binding [*print-length* nil *print-level* nil]
                              (pp/pprint (assoc (apply dissoc r tables)
                                                :class-columns [:status :members :translated :partial
                                                                :hand-written :stub :outside-type :cut :absent])))))]
      (spit out-edn
            (str header
                 (subs head 0 (dec (count head)))
                 (apply str (for [k tables]
                              (str "\n " k "\n {\n"
                                   (apply str (for [[c v] (get r k)] (str "  " (pr-str c) " " (pr-str v) "\n")))
                                   " }")))
                 "}\n")))
    (write-edn details-edn header
               (into (sorted-map)
                     (for [[p cs] cov]
                       [p (vec (for [c cs]
                                 [(:name c) (:status c)
                                  (vec (for [m (:members c)]
                                         [(:kind m) (:name m) (:desc m) (:status m)]))]))])))
    (spit summary-md (summary r))
    r))
