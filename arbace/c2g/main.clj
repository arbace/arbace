(ns arbace.c2g.main
  "bin/c2g: class forms to Go forms (doc/go/C2G-SPEC.md, doc/go/C2G-NOTES.md).

  bin/c2g [OPTIONS]
    --out DIR          output directory (default .tmp/c2g/out): DIR/gen/go/... the generated
                       forms (C2G-SPEC §4.3: the files c2g writes), DIR/prog/ a program root
                       merging them with jrt's hand-written forms (stand-ins of translated
                       classes removed), for bin/g2c build; DIR/report.edn
    --lang             input: Arbace's arbace/lang/*.clj (default: both)
    --jdk DIR          input: the converted JDK closure (default .tmp/jrt/conv, from
                       bin/jrt-convert)
    --input DIR        input: an extra tree of class forms files (test fixtures)
    --slice REGEX      only classes whose internal names match one of the given regexes
                       (repeatable) are translated; everything else is outside the closed
                       world and the operations needing it throw (phase 1's slices)
    --root SPEC        a reachability root: CLASS (all its public members), CLASS#name (its
                       members named name), CLASS#<init> (its constructors), CLASS$* (the
                       class and its named nested classes, all their public members);
                       internal names with / or binary names with . (repeatable)
    --tests            also copy jrt's test files into the program
    --main DIR         a main package's forms tree to add to the program (its go/... files)
    --program          the whole program (C2G-SPEC §10.6): arbace.lang.Main is a root, and
                       c2g writes the main package arbace/cmd/arbace, whose main makes the
                       calling goroutine the thread main (jrt.RunMain) and calls
                       arbace.lang.Main.main(args) (RT's initialization first); build it with
                       bin/g2c build --line-file DIR/prog"
  (:require [arbace.string :as str]
            [arbace.java.io :as io]
            [arbace.classes.types :as t]
            [arbace.classes.env :as env]
            [arbace.classes.analyze :as a]
            [arbace.classes.emit :as e]
            [arbace.c2g.world :as w]
            [arbace.c2g.jrt :as jrt]
            [arbace.c2g.model :as m]
            [arbace.c2g.names :as nm]
            [arbace.c2g.reach :as r]
            [arbace.c2g.access :as acc]
            [arbace.c2g.code :as c]
            [arbace.c2g.decls :as d]
            [arbace.c2g.out :as out]
            [arbace.c2g.dyn :as dyn]
            [arbace.c2g.fromfn :as fromfn]
            [arbace.c2g.checks :as chk]
            [arbace.c2g.embed :as embed]
            [arbace.g2c.print :as gp]
            [arbace.pprint]))

(defn- now [] (System/nanoTime))
(defn- secs [t0] (/ (Math/round (/ (- (System/nanoTime) t0) 1e7)) 100.0))

(defn parse-args [args]
  (loop [[a & more] args opts {:out ".tmp/c2g/out" :slice [] :roots [] :inputs []}]
    (case a
      nil opts
      "--out" (recur (rest more) (assoc opts :out (first more)))
      "--lang" (recur more (assoc opts :lang true))
      "--jdk" (recur (rest more) (assoc opts :jdk (first more)))
      "--input" (recur (rest more) (update opts :inputs conj (first more)))
      "--slice" (recur (rest more) (update opts :slice conj (re-pattern (first more))))
      "--root" (recur (rest more) (update opts :roots conj (first more)))
      "--tests" (recur more (assoc opts :tests true))
      "--main" (recur (rest more) (update opts :mains (fnil conj []) (first more)))
      "--program" (recur more (-> opts (assoc :program true) (update :roots conj "arbace.lang.Main#main")))
      (throw (ex-info (str "c2g: unknown option " a) {})))))

(defn- internal [s] (str/replace s "." "/"))

(defn manifest-classes
  "The classes the jrt manifest declares (internal names)."
  [dir]
  (let [f (io/file dir "manifest.edn")]
    (if (.isFile f)
      (set (map #(internal (str %)) (keys (first (w/read-forms f)))))
      #{})))

(defn jrt-classes
  "jrt's classes: those its forms register (Define) and those its manifest declares."
  [scan manifest]
  (merge
    (into {} (for [n manifest]
               [n {:go (nm/go-class-name n) :var (str (nm/go-class-name n) "_class") :standin false :file nil}]))
    (into {} (for [[jn v] (:classes scan)] [(internal jn) v]))))

(defn- root-keys
  "Method keys of a root spec. CLASS$* is the class and every named class nested in it (the
  whole of Compiler, say)."
  [spec]
  (if (str/ends-with? spec "$*")
    (let [n (internal (subs spec 0 (- (count spec) 2)))]
      (mapcat root-keys (cons n (filter #(and (str/starts-with? % (str n "$"))
                                              (not (re-find #"\$\d" %)))
                                        @(:order a/*unit*)))))
  (let [[cls mname] (str/split spec #"#" 2)
        n (internal cls)
        dd (a/decl n)]
    (when-not dd (throw (ex-info (str "c2g: root class not found: " cls) {})))
    (concat
      (for [mm (:methods dd)
            :when (if mname (= mname (:name mm))
                      (m/has? (:flags mm) 1))]
        [n (:name mm) (:desc mm)])
      [[n "<clinit>" "()V"]]))))

(defn standin-roots
  "Roots for the translated classes that replace jrt's stand-ins: their static initializer
  and the members the stand-in defined (jrt's code may use them)."
  [scan T]
  (concat
    (for [n T :let [jc (get (:jrt-classes m/*w*) n)] :when (and jc (:standin jc) (a/decl n))]
      [n "<clinit>" "()V"])
  (for [n T
        :let [jc (get (:jrt-classes m/*w*) n)]
        :when (and jc (:standin jc))
        :let [g (m/go-name n)
              fs (get-in scan [:files (:file jc)])
              funcs (:funcs fs)
              meths (get-in fs [:methods g] #{})]
        mm (:methods (a/decl n))
        :let [gn (cond (= "<init>" (:name mm)) (nm/new-name g (e/ctor-real-desc n mm))
                       (m/static? mm) (str g "_" (nm/method-base (:name mm) (:desc mm)))
                       :else (nm/method-base (:name mm) (:desc mm)))]
        :when (or (contains? funcs gn) (contains? meths gn))]
    [n (:name mm) (:desc mm)])))

;; ---------------------------------------------------------------------------------------
;; filtering jrt's stand-ins

(defn- form-names
  "Go identifiers a top-level form declares."
  [f]
  (when (seq? f)
    (let [h (when (symbol? (first f)) (name (first f)))]
      (case h
        "type" (let [x (second f)] (if (symbol? x) [(name x)] []))
        "var" (let [x (second f)] (if (symbol? x) [(name x)] []))
        "func" [(name (second f))]
        nil))))

(defn- method-recv-type [f]
  (let [params (first (filter vector? (drop 2 f)))
        r (first params)
        tg (when (symbol? r) (:tag (meta r)))]
    (cond (symbol? tg) (name tg)
          (and (seq? tg) (= '* (first tg)) (symbol? (second tg))) (name (second tg))
          :else nil)))

(defn- mentions? [form names-re]
  (let [hit (volatile! false)]
    (letfn [(w [x] (cond @hit nil
                         (symbol? x) (when (re-matches names-re (name x)) (vreset! hit true))
                         (coll? x) (run! w x)
                         :else nil))]
      (w form))
    @hit))

(defn- filter-init-body [f own-re]
  (let [[h nm params & body] f]
    (apply list h nm params (remove #(mentions? % own-re) body))))

(defn filter-standins* [forms gs]
  (if (empty? gs)
    forms
    (let [alt (str/join "|" (map #(java.util.regex.Pattern/quote %) gs))
          own-re (re-pattern (str "(" alt ")(_[A-Za-z0-9_]*)?"))
          drop? (fn [f]
                  (and (seq? f) (symbol? (first f))
                       (let [h (name (first f))]
                         (cond
                           (= h "method") (some? (some->> (method-recv-type f) (re-matches own-re)))
                           (#{"type" "var" "func" "const"} h)
                           (let [nm (first (form-names f))]
                             (and nm (not= nm "init") (re-matches own-re nm)))
                           :else false))))
          ;; groups (go/var [a e] [b e]): the specs of translated classes go
          group (fn [f]
                  (if (and (seq? f) (symbol? (first f)) (#{"var" "const" "type"} (name (first f)))
                           (some vector? (rest f)))
                    (let [[h & more] f
                          [doc specs] (if (string? (first more)) [[(first more)] (rest more)] [[] more])
                          keep (remove (fn [sp] (let [x (first sp)] (and (symbol? x) (re-matches own-re (name x))))) specs)]
                      (when (seq keep) (apply list h (concat doc keep))))
                    f))]
      (vec (for [f forms :when (not (drop? f))
                 :let [f (group f)] :when f]
             (if (and (seq? f) (symbol? (first f)) (= "func" (name (first f))) (= 'init (second f)))
               (filter-init-body f own-re)
               f))))))

;; ---------------------------------------------------------------------------------------

(defn- write! [f text]
  (io/make-parents (io/file f))
  (spit (io/file f) text))

(defn- copy-file! [from to]
  (io/make-parents (io/file to))
  (io/copy (io/file from) (io/file to)))

(defn- top-of [^String n] (let [i (.indexOf n "$")] (if (neg? i) n (subs n 0 i))))

(defn- program-package-forms
  "The main package: main.go and the embedded sources (res/..., EVAL-NOTES.md, \"Loading\")."
  [embedded]
  ['(ns go.arbace.cmd.arbace (:require [arbace.go :as go]))
   (list 'go/package 'main :path "arbace/cmd/arbace" :files ["image.go" "image_types.go" "main.go"]
         :embed-files (conj (vec (for [p embedded] [(str "res/" p) nil])) ["image.bin" nil]))
   '(load "arbace/image")
   '(load "arbace/image_types")
   '(load "arbace/main")])

(defn- program-main-forms
  "main: the command line as a String[], then arbace.lang.Main.main inside jrt.RunMain (the
  thread main, uncaught exceptions printed as the JVM prints them, status 1), its status the
  process's."
  []
  ['(go/file "main.go" :imports [[embed "embed"] [fs "io/fs"] [os "os"] [jrt "arbace/jrt"] [lang "arbace/lang"]])
   '(go/var ^{:go/embed ["res"] :tag embed/FS
              :doc "resources are the namespaces' sources RT.load reads (EVAL-NOTES.md, \"Loading\").\n"}
      resources)
   '(go/var ^{:go/embed ["image.bin"] :tag string
              :doc "imageData is the image of prepared namespaces (EXEC-NOTES.md; image.go): empty until bin/arbace-go --build writes it.\n"}
      imageData)
   '(go/func main []
      (let [(values sub err) (fs/Sub resources "res")]
        (when (== err nil)
          (jrt/SetHost (lit jrt/OSHost :Resources sub))))
      (setupImage imageData)
      (let [args (jrt/NewRefArray jrt/String_class (conv int32 (- (len os/Args) 1)))]
        (range [i a (subslice os/Args 1)]
          (aset (.-A args) i (jrt/Str a)))
        (let [status (jrt/RunMain (fn [] (lang/Main_Main_String1__V args)))]
          (finishImage)
          (os/Exit status))))])

(defn- source-top
  "The name a Go file is named after (§4.3): in arbace/lang the class forms file of top-level
  class top (FxClasses for the classes of FxClasses.clj, so that --line-file positions name
  it), in jrt the class (its Java source path)."
  [world pkg top]
  (if-let [f (and (= pkg :lang) (get (:source-of world) top))]
    (let [b (.getName (io/file f))]
      (str (subs top 0 (inc (.lastIndexOf ^String top "/"))) (subs b 0 (- (count b) 4))))
    top))

(defn run [opts]
  (let [t0 (now)
        root-dir "."
        opts (if (:program opts)
               (let [srcs (embed/sources root-dir)]
                 (assoc opts :embedded srcs :cut-candidates (sort (embed/cut-candidates srcs))))
               opts)
        jdk (or (:jdk opts) ".tmp/jrt/conv")
        inputs (vec (concat
                      (when (or (:lang opts) (not (:jdk opts))) [{:root "arbace" :dirs ["lang"]}])
                      (when (or (:jdk opts) (not (:lang opts))) (when (.isDirectory (io/file jdk)) [{:root jdk :tree true}]))
                      (for [i (:inputs opts)] {:root i :tree true})))
        clj-files (fn [dir] (let [d (io/file dir)]
                              (when (.isDirectory d)
                                (sort (filter #(str/ends-with? (str %) ".clj") (.listFiles d))))))
        variant-files (or (:variant-files opts)
                          (concat
                            (when (some #(= "arbace" (:root %)) inputs) (clj-files "arbace/lang/go"))
                            ;; the JDK closure's variants (jrt's, overlay/jdk/variants)
                            (when (some #(= jdk (:root %)) inputs) (clj-files "overlay/jdk/variants"))))
        world (w/load-world inputs :variant-files variant-files)
        _ (doseq [[n c] (sort (:variants world))]
            (println (str "c2g: variant of " n ": " (:replaced c) " replaced, " (:cut c) " cut, " (:added c) " added")))
        t-load (secs t0)
        _ (println (str "c2g: " (count (:files world)) " files, " (count @(:compile-set world)) " classes analyzed in " t-load " s"
                        (when (seq (:failed world)) (str ", " (count (:failed world)) " failures"))))
        jrt-dir "go/arbace/jrt"
        scan (jrt/scan jrt-dir)
        manifest (manifest-classes jrt-dir)
        jc (jrt-classes scan manifest)
        ;; String.format over the translated Formatter (JRT-NOTES.md: "waiting for c2g"):
        ;; c2g writes it into jrt's c2g_support when Formatter is translated
        formatter? (and (contains? @(:compile-set world) "java/util/Formatter")
                        (or (empty? (:slice opts)) (some #(re-find % "java/util/Formatter") (:slice opts))))
        slice (:slice opts)
        in-slice? (fn [n] (or (empty? slice) (some #(re-find % n) slice)))
        ;; jrt's statics whose values are translated objects (overlay/jdk's jrt classes): the
        ;; standard streams of System and String.CASE_INSENSITIVE_ORDER, written by c2g into
        ;; jrt's c2g_support (out/jrt-statics-forms) when their classes are translated
        jrt-java? (fn [n] (and (contains? @(:compile-set world) n) (in-slice? n)))
        streams? (jrt-java? "jdk/internal/jrt/StandardStreams")
        ci-order? (jrt-java? "jdk/internal/jrt/CaseInsensitiveComparator")
        ;; String's regex methods over the translated java.util.regex (out/support-forms)
        regex? (jrt-java? "java/util/regex/Pattern")
        scan (cond-> scan
               formatter? (update :funcs into ["String_Format_String_O1__String" "String_Format_Locale_String_O1__String"])
               formatter? (update-in [:methods "String"] (fnil conj #{}) "Formatted_O1__String")
               (contains? @(:compile-set world) "java/lang/Iterable")
               (update :funcs conj "String_Join_CharSequence_Iterable__String")
               (contains? @(:compile-set world) "java/util/Optional")
               (update-in [:methods "String"] (fnil conj #{}) "DescribeConstable__Optional")
               (contains? @(:compile-set world) "java/util/stream/Stream")
               (update-in [:methods "String"] (fnil conj #{}) "Lines__Stream")
               streams? (update :vars into ["System_in" "System_out" "System_err"])
               streams? (update :funcs into ["System_SetIn_InputStream__V" "System_SetOut_PrintStream__V" "System_SetErr_PrintStream__V"])
               ci-order? (update :vars conj "String_CASE_INSENSITIVE_ORDER")
               regex? (update-in [:methods "String"] (fnil into #{}) (map first out/string-regex-methods)))
        cs (:compile-set world)]
    (w/with-world world
      (let [wst {:jrt scan :jrt-classes jc :T #{} :vmethods-cache (atom {}) :trivial-cache (atom {})
                 :erased (:erased world)}
            t1 (now)
            roots (vec (concat (mapcat root-keys (concat (:roots opts) (when-let [f (:root-fn opts)] (f))
                                                         ;; --program: the REPL's world, every public
                                                         ;; member of every class with class forms
                                                         ;; (C2G-SPEC §10.6, EVAL-PLAN.md Q2)
                                                         (when (:program opts) (filter a/decl (sort @(:order a/*unit*))))))
                               ;; what the evaluator's Dyn (c2g_dyn.go) calls
                               (when (a/decl dyn/api-class) (concat (dyn/roots) (dyn/proxy-roots)))
                               (when (a/decl "arbace/lang/RT") (fromfn/roots))
                               (when streams?
                                 [["jdk/internal/jrt/StandardStreams" "in" "()Ljava/io/InputStream;"]
                                  ["jdk/internal/jrt/StandardStreams" "out" "()Ljava/io/PrintStream;"]
                                  ["jdk/internal/jrt/StandardStreams" "err" "()Ljava/io/PrintStream;"]])
                               (when regex?
                                 [["java/util/regex/Pattern" "compile" "(Ljava/lang/String;)Ljava/util/regex/Pattern;"]
                                  ["java/util/regex/Pattern" "split" "(Ljava/lang/CharSequence;I)[Ljava/lang/String;"]
                                  ["java/util/regex/Pattern" "matcher" "(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher;"]
                                  ["java/util/regex/Pattern" "matches" "(Ljava/lang/String;Ljava/lang/CharSequence;)Z"]
                                  ["java/util/regex/Matcher" "replaceAll" "(Ljava/lang/String;)Ljava/lang/String;"]
                                  ["java/util/regex/Matcher" "replaceFirst" "(Ljava/lang/String;)Ljava/lang/String;"]])
                               (when ci-order?
                                 [["jdk/internal/jrt/CaseInsensitiveComparator" "<clinit>" "()V"]])
                               ;; c2g's support for the REPL's reflection (out/support-forms)
                               (when (contains? @(:compile-set world) "java/util/Optional")
                                 [["java/util/Optional" "of" "(Ljava/lang/Object;)Ljava/util/Optional;"]])
                               (when (contains? @(:compile-set world) "java/util/stream/Stream")
                                 [["java/util/ArrayList" "<init>" "()V"]
                                  ["java/util/ArrayList" "add" "(Ljava/lang/Object;)Z"]
                                  ["java/util/Collection" "stream" "()Ljava/util/stream/Stream;"]])
                               (when (contains? @(:compile-set world) "java/io/PrintWriter")
                                 [["java/io/PrintWriter" "print" "(Ljava/lang/String;)V"]])
                               (when (contains? @(:compile-set world) "java/io/PrintStream")
                                 [["java/io/PrintStream" "print" "(Ljava/lang/String;)V"]])
                               (when (and (contains? @(:compile-set world) "java/util/HashMap")
                                          (contains? @(:compile-set world) "java/util/Collections"))
                                 [["java/util/HashMap" "<init>" "()V"]
                                  ["java/util/HashMap" "put" "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"]
                                  ["java/util/Collections" "unmodifiableMap" "(Ljava/util/Map;)Ljava/util/Map;"]])
                               (when formatter?
                                 [["java/util/Formatter" "<init>" "()V"]
                                  ["java/util/Formatter" "<init>" "(Ljava/util/Locale;)V"]
                                  ["java/util/Formatter" "format" "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/util/Formatter;"]
                                  ["java/util/Formatter" "toString" "()Ljava/lang/String;"]])))
            slice? (fn [n] (boolean (in-slice? (top-of n))))
            ;; the stand-ins the translated classes replace: the members they define are roots
            ;; too (jrt's own code uses them), to a fixpoint
            ;; every stand-in c2g can translate (in the slice) is replaced (C2G-SPEC §4.3)
            standins (vec (for [[n j] jc :when (and (:standin j) (m/translatable? n) (slice? n))] n))
            res (loop [res (binding [m/*w* wst] (r/run {:roots roots :slice? slice? :classes standins})) n 0]
                  (let [sroots (binding [m/*w* (assoc wst :T (:T res))] (vec (standin-roots scan (:T res))))
                        res2 (binding [m/*w* wst] (r/run {:roots (vec (concat roots sroots)) :slice? slice? :classes standins
                                                          :fis (binding [m/*w* (assoc wst :T (:T res))] (fromfn/fis (:T res)))}))]
                    (if (or (= (:T res2) (:T res)) (> n 8)) res2 (recur res2 (inc n)))))
            T (:T res)
            wst (assoc wst :T T :vmethods-cache (atom {}) :trivial-cache (atom {}))
            wst (assoc wst :subclass-index (m/make-subclass-index wst))
            t-reach (secs t1)
            _ (println (str "c2g: reachability: " (count T) " classes, " (count (:reached res)) " methods reached, "
                            (count (:unavailable res)) " with missing parts (" t-reach " s)"))
            t2 (now)
            gen-dir (str (:out opts) "/gen")
            prog-dir (str (:out opts) "/prog")
            cuts (atom #{})
            pkgstates {:jrt {:lits (atom {}) :ups (atom #{}) :lambdas (atom #{}) :cuts cuts}
                       :lang {:lits (atom {}) :ups (atom #{}) :lambdas (atom #{}) :cuts cuts}}
            files (atom {})   ; [pkg go-file] -> forms
            errors (atom [])]
        (binding [m/*w* wst
                  out/*absent-members* (boolean (:program opts))
                  d/*reached* (:reached res)
                  c/*races* (atom #{})]
          ;; classes, grouped by the file of their top-level class
          (doseq [n (sort T)]
            (let [pkg (m/pkg n)
                  fname (out/go-file-name pkg (source-top world pkg (top-of n)))]
              (binding [c/*pkgstate* (get pkgstates pkg)]
                (try
                  (let [forms (if (m/reflected? n) (d/reflected-forms n) (d/class-forms n))]
                    (swap! files update [pkg fname] (fnil into []) forms))
                  (catch Throwable ex
                    (swap! errors conj [n (.getMessage ex)])
                    (println "c2g: error in" n ":" (.getMessage ex))
                    (when (System/getenv "C2G_TRACE") (.printStackTrace ex)))))))
          ;; the type of the objects of classes made at run time (C2G-SPEC §5.12)
          (when (dyn/enabled?)
            (binding [c/*pkgstate* (get pkgstates :lang)]
              (swap! files assoc [:lang "c2g_dyn.go"] (vec (dyn/forms T)))))
          ;; FromFn of the functional interfaces (C2G-SPEC §7.11), their adapters with the lambdas'
          (let [fis (filter #(contains? T %) (fromfn/fis T))]
            (when (seq fis)
              (swap! (:lambdas (get pkgstates :lang)) into fis)
              (swap! files assoc [:lang "c2g_fromfn.go"] (vec (fromfn/forms fis)))))
          ;; adapters of the functional interfaces lambdas target (in the interface's package)
          (loop [done #{}]
            (let [todo (doall (distinct (remove done (for [[pkg ps] pkgstates fi @(:lambdas ps)] fi))))]
              (when (seq todo)
                (doseq [fi todo]
                  (let [pkg (m/pkg fi)]
                    (binding [c/*pkgstate* (get pkgstates pkg)]
                      (swap! files update [pkg "c2g_lambdas.go"] (fnil into []) (out/adapter-forms pkg fi)))))
                (recur (into done todo)))))
          ;; per package: registrations first (their member tables may name cut classes of
          ;; either package), then the literal pool, conversions, frames, support, cut classes
          (doseq [pkg [:jrt :lang]
                  :let [ps (get pkgstates pkg)
                        classes (sort (filter #(= pkg (m/pkg %)) T))]
                  :when (or (seq classes) (= pkg :jrt))]
            (binding [c/*pkgstate* ps]
              (swap! files assoc [pkg "c2g_classes.go"] (vec (mapcat #(out/class-registration pkg %) classes)))))
          (doseq [pkg [:jrt :lang]
                  :let [ps (get pkgstates pkg)
                        classes (sort (filter #(= pkg (m/pkg %)) T))]
                  :when (or (seq classes) (= pkg :jrt))]
            (binding [c/*pkgstate* ps]
              (when-let [fr (out/frames-forms pkg classes)] (swap! files assoc [pkg "c2g_frames.go"] (vec fr)))
              (when (= pkg :jrt) (swap! files assoc [pkg "c2g_support.go"] (out/support-forms)))
              (when (= pkg :jrt)
                (when-let [tf (out/support-table-forms)]
                  (swap! files assoc [pkg "reflect_tables_c2g.go"] (vec tf)))))
            ;; --program: the classes the embedded namespaces name outside the world are cut
            ;; classes too, so that their imports and hints resolve (EVAL-NOTES.md)
            (let [taken (into (set (map m/go-name T)) (concat (:vars (:jrt m/*w*)) (keys (:types (:jrt m/*w*)))))
                  taken (into taken (map #(str (m/go-name %) "_class") T))
                  cands (set (:cut-candidates opts))]
              (doseq [n (embed/with-member-types (:cut-candidates opts) m/in-world?)
                      :when (or (not (m/in-world? n))
                                (and (m/hand-written? n)
                                     (not (contains? (:vars (:jrt m/*w*)) (str (m/go-name n) "_class")))))
                      ;; a member type whose Go name another class has (java.sql.Date) is left out
                      :when (or (contains? cands n)
                                (not (or (taken (m/go-name n)) (taken (str (m/go-name n) "_class"))
                                         (some #(and (not= % n) (= (m/go-name %) (m/go-name n))) @cuts))))]
                (swap! cuts conj n)))
            (swap! files assoc [pkg "c2g_cut.go"]
                   (vec (concat (out/cut-forms pkg (filter #(= pkg (m/pkg %)) @cuts))
                                ;; --program: the cut classes' members, throwing (EVAL-NOTES.md)
                                (when (:program opts)
                                  (out/cut-table-forms pkg @cuts (filter #(= pkg (m/pkg %)) @cuts))))))
            ;; the classes that replace jrt's stand-ins initialize at Go package initialization:
            ;; jrt's hand-written code reads their statics as the stand-ins' package variables
            ;; (C2G-NOTES.md, proposed amendment A3)
            (when (= pkg :jrt)
              (let [eager (sort (for [n T :let [j (get jc n)]
                                      :when (and j (:standin j) (a/decl n) (not (m/trivial-init? n))
                                                 (contains? (:reached res) [n "<clinit>" "()V"]))]
                                  n))]
                (when (seq eager)
                  (swap! files assoc [pkg "c2g_init.go"]
                         [(apply list 'go/func 'init []
                                 (for [n eager] (list (symbol (str (m/go-name n) "_Init")))))]))))
            (swap! files assoc [pkg "c2g_strings.go"] (vec (out/strings-forms pkg @(:lits ps))))
            (swap! files assoc [pkg "c2g_up.go"] (vec (out/ups-forms pkg @(:ups ps)))))
          (let [t-trans (secs t2)
                ;; §4.4: duplicate Go names, package-private methods Go would override; §8.3
                collisions (chk/collisions @files scan)
                _ (doseq [c collisions]
                    (swap! errors conj [(str "Go name " (:name c)) (str "declared more than once in " (name (:pkg c)) ": " (str/join ", " (:sources c)))])
                    (println (str "c2g: error: Go name " (:name c) " declared more than once in package " (name (:pkg c)) ": " (str/join ", " (:sources c)))))
                pp-overrides (chk/package-private-overrides T)
                _ (doseq [o pp-overrides]
                    (swap! errors conj [(:method o) (str "package-private, redeclared by " (:hidden-by o) " in another package (C2G-SPEC §4.4: rename table)")])
                    (println (str "c2g: error: package-private " (:method o) " is redeclared by " (:hidden-by o) " in another Java package; Go would override it")))
                races (chk/race-report @c/*races*)
                t3 (now)
                ;; write the generated tree
                _ (when (.exists (io/file gen-dir)) (doseq [f (reverse (file-seq (io/file gen-dir)))] (io/delete-file f true)))
                written (doall
                          (for [[[pkg fname] forms] (sort @files)
                                :when (seq forms)]
                            (let [stem (subs fname 0 (- (count fname) 3))
                                  path (str gen-dir "/go/arbace/" (nm/pkg-name pkg) "/" stem ".clj")]
                              (write! path (out/file-text pkg fname forms))
                              [pkg fname stem])))
                ;; the program root: jrt's hand-written files (stand-ins filtered) and the generated ones
                _ (when (.exists (io/file prog-dir)) (doseq [f (reverse (file-seq (io/file prog-dir)))] (io/delete-file f true)))
                replaced (for [n T :let [j (get jc n)] :when (and j (:standin j))] n)
                replaced-by-file (group-by #(:file (get jc %)) replaced)
                ;; a stand-in file all of whose classes are translated goes as a whole (its
                ;; helpers refer to the stand-in's own shapes)
                whole-files (set (for [[fname defined] (group-by :file (vals jc))
                                       :when (and fname (str/starts-with? fname "standin_"))
                                       :when (every? (fn [j] (some #(= (:go j) (m/go-name %)) (get replaced-by-file fname))) defined)]
                                   fname))
                hw-files (sort (for [^java.io.File f (.listFiles (io/file jrt-dir))
                                     :let [nme (.getName f)]
                                     :when (and (str/ends-with? nme ".clj")
                                                (not (contains? whole-files nme))
                                                (or (:tests opts) (not (str/ends-with? nme "_test.clj"))))]
                                 nme))
                _ (doseq [fname hw-files]
                    (let [src (str jrt-dir "/" fname)
                          dst (str prog-dir "/go/arbace/jrt/" fname)
                          gs (map m/go-name (get replaced-by-file fname))]
                      (if (seq gs)
                        (let [forms (filter-standins* (gp/read-forms src) gs)
                              text (str ";; c2g: " fname " without the classes c2g translates: " (str/join ", " (sort gs)) "\n"
                                        (str/join "\n\n" (map out/form-text forms)) "\n")]
                          (write! dst text))
                        (copy-file! src dst))))
                ;; data files jrt embeds or copies (none expected besides forms)
                gen-jrt (sort (for [[pkg _ stem] written :when (= pkg :jrt)] stem))
                gen-lang (sort (for [[pkg _ stem] written :when (= pkg :lang)] stem))
                _ (doseq [[pkg _ stem] written]
                    (copy-file! (str gen-dir "/go/arbace/" (nm/pkg-name pkg) "/" stem ".clj")
                                (str prog-dir "/go/arbace/" (nm/pkg-name pkg) "/" stem ".clj")))
                hw-stems (map #(subs % 0 (- (count %) 4)) hw-files)
                go-names (fn [stems] (vec (sort (map #(str % ".go") stems))))
                jrt-stems (concat (remove #(str/ends-with? % "_test") hw-stems) gen-jrt)
                jrt-tests (filter #(str/ends-with? % "_test") hw-stems)]
            (write! (str prog-dir "/go/arbace/jrt.clj")
                    (str ";; Generated by c2g: jrt's hand-written files and c2g's translation (C2G-SPEC §4.3).\n"
                         (out/form-text '(ns go.arbace.jrt (:require [arbace.go :as go]))) "\n\n"
                         (out/form-text (apply list 'go/package 'jrt :path "arbace/jrt" :files (go-names jrt-stems)
                                               (when (seq jrt-tests) [:test-files (go-names jrt-tests)]))) "\n\n"
                         (str/join "\n" (for [s (sort (concat jrt-stems jrt-tests))] (out/form-text (list 'load (str "jrt/" s)))))
                         "\n"))
            (when (seq gen-lang)
              (write! (str prog-dir "/go/arbace/lang.clj")
                      (str ";; Generated by c2g (C2G-SPEC §4.2): the package arbace/lang.\n"
                           (out/form-text '(ns go.arbace.lang (:require [arbace.go :as go]))) "\n\n"
                           (out/form-text (list 'go/package 'lang :path "arbace/lang" :files (go-names gen-lang))) "\n\n"
                           (str/join "\n" (for [s gen-lang] (out/form-text (list 'load (str "lang/" s)))))
                           "\n")))
            (write! (str prog-dir "/program.edn") "{:module \"arbace\" :go \"1.27\"}\n")
            (when (:program opts)
              (doseq [[p text] (:embedded opts)]
                (write! (str prog-dir "/go/arbace/cmd/arbace/res/" p) text))
              ;; the sources are data to g2c, not forms
              (write! (str prog-dir "/go/arbace/cmd/arbace/res/.g2c-data")
                      "The main package's embedded sources (c2g --program): data, not Go forms.\n")
              (write! (str prog-dir "/go/arbace/cmd/arbace.clj")
                      (str ";; Generated by c2g --program (C2G-SPEC §10.6): the program's main package.\n"
                           (str/join "\n" (map out/form-text (program-package-forms (keys (:embedded opts))))) "\n"))
              (write! (str prog-dir "/go/arbace/cmd/arbace/main.clj")
                      (str ";; Generated by c2g --program (C2G-SPEC §10.6).\n(in-ns 'go.arbace.cmd.arbace)\n\n"
                           (str/join "\n\n" (map out/form-text (program-main-forms))) "\n"))
              ;; the image of prepared namespaces (EXEC-NOTES.md): its encoding (hand-written
              ;; forms of the main package), the program's types, and an empty image, which
              ;; bin/arbace-go --build replaces
              (copy-file! (io/file root-dir embed/image-forms) (str prog-dir "/go/arbace/cmd/arbace/image.clj"))
              (write! (str prog-dir "/go/arbace/cmd/arbace/image_types.clj")
                      (str ";; Generated by c2g --program: the program's struct types, by name (EXEC-NOTES.md).\n"
                           "(in-ns 'go.arbace.cmd.arbace)\n\n"
                           (str/join "\n\n" (map out/form-text (embed/image-type-forms prog-dir))) "\n"))
              (write! (str prog-dir "/go/arbace/cmd/arbace/image.bin") ""))
            (doseq [mdir (:mains opts)]
              (doseq [^java.io.File f (file-seq (io/file mdir))
                      :when (.isFile f)
                      :let [rel (str (.relativize (.toPath (.getCanonicalFile (io/file mdir))) (.toPath (.getCanonicalFile f))))]]
                (copy-file! f (str prog-dir "/" rel))))
            ;; the caller's additions to the program root, once it is complete
            (when-let [after (:after opts)]
              (after {:prog-dir prog-dir :T T :res res :world world}))
            (let [report {:classes (count T) :reached (count (:reached res))
                          ;; the classes in the inputs, by Go package
                          :world (frequencies (map #(name (m/pkg %)) @(:order (:unit world))))
                          :methods-with-missing-parts (count (:unavailable res))
                          :translated (vec (sort T))
                          :replaced-standins (vec (sort replaced))
                          :unavailable (into (sorted-map) (for [[k v] (:unavailable res)] [(str/join " " k) (vec (sort v))]))
                          :missing (into (sorted-map) (for [[k v] (:missing res)] [k (count v)]))
                          :analysis-failures (:failed world)
                          :variants (:variants world)
                          :errors @errors
                          ;; §8.3, Q10: non-volatile fields of interface Go type written after
                          ;; construction without a lock
                          :race-candidates races
                          :package-private-overrides pp-overrides
                          :times {:load t-load :reach t-reach :translate t-trans :write (secs t3)}}]
              (write! (str (:out opts) "/report.edn") (with-out-str (arbace.pprint/pprint report)))
              (println (str "c2g: translated in " t-trans " s, written in " (secs t3) " s: "
                            (count written) " files; program root " prog-dir))
              (println (str "c2g: " (reduce + (map count (vals races))) " candidate two-word race fields in "
                            (count races) " classes (report.edn :race-candidates); "
                            (count collisions) " name collisions, " (count pp-overrides) " package-private overrides"))
              report)))))))

(defn -main [& args]
  (let [opts (parse-args args)
        report (run opts)]
    (shutdown-agents)
    (System/exit (if (seq (:errors report)) 1 0))))
