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
                       members named name), CLASS#<init> (its constructors); internal names
                       with / or binary names with . (repeatable)
    --tests            also copy jrt's test files into the program
    --main DIR         a main package's forms tree to add to the program (its go/... files)"
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
  "Method keys of a root spec."
  [spec]
  (let [[cls mname] (str/split spec #"#" 2)
        n (internal cls)
        dd (a/decl n)]
    (when-not dd (throw (ex-info (str "c2g: root class not found: " cls) {})))
    (concat
      (for [mm (:methods dd)
            :when (if mname (= mname (:name mm))
                      (m/has? (:flags mm) 1))]
        [n (:name mm) (:desc mm)])
      [[n "<clinit>" "()V"]])))

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

(defn run [opts]
  (let [t0 (now)
        root-dir "."
        jdk (or (:jdk opts) ".tmp/jrt/conv")
        inputs (vec (concat
                      (when (or (:lang opts) (not (:jdk opts))) [{:root "arbace" :dirs ["lang"]}])
                      (when (or (:jdk opts) (not (:lang opts))) (when (.isDirectory (io/file jdk)) [{:root jdk :tree true}]))
                      (for [i (:inputs opts)] {:root i :tree true})))
        variant-files (or (:variant-files opts)
                          (when (some #(= "arbace" (:root %)) inputs)
                            (let [d (io/file "arbace/lang/go")]
                              (when (.isDirectory d)
                                (sort (filter #(str/ends-with? (str %) ".clj") (.listFiles d)))))))
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
        scan (cond-> scan
               formatter? (update :funcs into ["String_Format_String_O1__String" "String_Format_Locale_String_O1__String"]))
        slice (:slice opts)
        in-slice? (fn [n] (or (empty? slice) (some #(re-find % n) slice)))
        cs (:compile-set world)]
    (w/with-world world
      (let [wst {:jrt scan :jrt-classes jc :T #{} :vmethods-cache (atom {}) :trivial-cache (atom {})}
            t1 (now)
            roots (vec (concat (mapcat root-keys (concat (:roots opts) (when-let [f (:root-fn opts)] (f))))
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
                        res2 (binding [m/*w* wst] (r/run {:roots (vec (concat roots sroots)) :slice? slice? :classes standins}))]
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
                  d/*reached* (:reached res)]
          ;; classes, grouped by the file of their top-level class
          (doseq [n (sort T)]
            (let [pkg (m/pkg n)
                  fname (out/go-file-name pkg (top-of n))]
              (binding [c/*pkgstate* (get pkgstates pkg)]
                (try
                  (let [forms (if (m/reflected? n) (d/reflected-forms n) (d/class-forms n))]
                    (swap! files update [pkg fname] (fnil into []) forms))
                  (catch Throwable ex
                    (swap! errors conj [n (.getMessage ex)])
                    (println "c2g: error in" n ":" (.getMessage ex))
                    (when (System/getenv "C2G_TRACE") (.printStackTrace ex)))))))
          ;; adapters of the functional interfaces lambdas target (in the interface's package)
          (loop [done #{}]
            (let [todo (doall (distinct (remove done (for [[pkg ps] pkgstates fi @(:lambdas ps)] fi))))]
              (when (seq todo)
                (doseq [fi todo]
                  (let [pkg (m/pkg fi)]
                    (binding [c/*pkgstate* (get pkgstates pkg)]
                      (swap! files update [pkg "c2g_lambdas.go"] (fnil into []) (out/adapter-forms pkg fi)))))
                (recur (into done todo)))))
          ;; per package: registrations, the literal pool, conversions, frames, support
          (doseq [pkg [:jrt :lang]
                  :let [ps (get pkgstates pkg)
                        classes (sort (filter #(= pkg (m/pkg %)) T))]
                  :when (or (seq classes) (= pkg :jrt))]
            (binding [c/*pkgstate* ps]
              (swap! files assoc [pkg "c2g_classes.go"] (vec (mapcat #(out/class-registration pkg %) classes)))
              (when-let [fr (out/frames-forms pkg classes)] (swap! files assoc [pkg "c2g_frames.go"] (vec fr)))
              (when (= pkg :jrt) (swap! files assoc [pkg "c2g_support.go"] (out/support-forms))))
            (swap! files assoc [pkg "c2g_cut.go"] (vec (out/cut-forms pkg (filter #(= pkg (m/pkg %)) @cuts))))
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
            (when-let [after (:after opts)]
              (after {:prog-dir prog-dir :T T :res res :world world}))
            (doseq [mdir (:mains opts)]
              (doseq [^java.io.File f (file-seq (io/file mdir))
                      :when (.isFile f)
                      :let [rel (str (.relativize (.toPath (.getCanonicalFile (io/file mdir))) (.toPath (.getCanonicalFile f))))]]
                (copy-file! f (str prog-dir "/" rel))))
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
                          :times {:load t-load :reach t-reach :translate t-trans :write (secs t3)}}]
              (write! (str (:out opts) "/report.edn") (with-out-str (arbace.pprint/pprint report)))
              (println (str "c2g: translated in " t-trans " s, written in " (secs t3) " s: "
                            (count written) " files; program root " prog-dir))
              report)))))))

(defn -main [& args]
  (let [opts (parse-args args)
        report (run opts)]
    (shutdown-agents)
    (System/exit (if (seq (:errors report)) 1 0))))
