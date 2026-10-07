(ns arbace.j2c.rename
  "The clojure -> arbace renaming of agenda step 4 (doc/VENDOR-NOTES.md, \"What is renamed\"),
  as a textual rewrite of source text. Used once to derive arbace/ from the frozen clojure/ (the
  class names in converted Java are renamed by `convert --rename clojure=arbace`, this renames
  what is left in its string literals, and the .clj sources), and by bin/clojure-tests to make a
  renamed copy of Clojure's test suite and its test libraries.

  Renamed: names that belong to the vendored Clojure, written dotted or as paths:
  - the Java packages clojure.lang, clojure.asm (with commons, signature), clojure.java.api,
    whatever follows them (class names, members, descriptors \"Lclojure/lang/Var;\", array
    names \"[Lclojure.lang.Var;\");
  - the vendored namespaces (exactly: clojure.core, clojure.core.protocols, ..., see
    `namespaces`), as symbols, qualified symbols and keywords, strings and resource paths
    (clojure/core, clojure/pprint/cl_format, clojure/core__init);
  - the keyword namespace clojure.error;
  - the system properties Clojure reads: clojure.compile.*, clojure.compiler.*,
    clojure.server.*, clojure.read.eval, clojure.main.report, clojure.basis;
  - clojure.jar in usage texts.
  Left alone: other libraries' names (clojure.spec.alpha, clojure.core.specs.alpha,
  clojure.test.check, clojure.tools.namespace, clojure.tools.deps, clojure.data.generators,
  clojure.java.classpath, ...; and the suite's own clojure.test-clojure.*), identifiers that
  merely contain the word (clojure-version, *clojure-version*, refer-clojure, CLOJURE_NS,
  __clojureFnMap, thread names clojure-agent-*), prose and URLs (clojure.org)."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(def namespaces
  "The namespaces of the vendored Clojure (clojure/**/*.clj), plus the files loaded into them,
  without the clojure. prefix."
  ["core" "core.protocols" "core.reducers" "core.server" "core_deftype" "core_print"
   "core_proxy" "genclass" "gvec" "data" "datafy" "edn" "inspector" "instant" "java.basis"
   "java.basis.impl" "java.browse" "java.browse-ui" "java.browse_ui" "java.io" "java.javadoc"
   "java.process" "java.shell" "main" "math" "pprint" "pprint.cl_format" "pprint.column_writer"
   "pprint.dispatch" "pprint.pprint_base" "pprint.pretty_writer" "pprint.print_table"
   "pprint.utilities" "reflect" "reflect.java" "repl" "repl.deps" "set" "stacktrace" "string"
   "template" "test" "test.junit" "test.tap" "tools.deps.interop" "uuid" "walk" "xml" "zip"
   ;; system properties and the jar name
   "read.eval" "main.report" "basis" "jar"])

(def prefixes
  "Renamed together with whatever follows them."
  ["lang" "asm" "java.api" "compile" "compiler" "server" "error"])

(defn- alt
  "A regex alternation of the names (longest first), their dots matched by regex sep."
  [names sep]
  (->> names
       (sort-by (comp - count))
       (map (fn [n] (str/join sep (map #(java.util.regex.Pattern/quote %) (str/split n #"\.")))))
       (str/join "|")))

(def ^:private before "(?:(?<=-D)|(?<![\\w.$-]))")

;; separators: a dot, also escaped in regex literals (#"clojure\.lang\.Agent"); a slash; either
(def ^:private dot "\\\\?\\.")
(def ^:private any (str "(?:" dot "|/)"))

(def patterns
  [;; packages, dotted or as paths, with anything after them
   (re-pattern (str before "clojure(?=" any "(?:" (alt prefixes any) ")(?![\\w-]))"))
   ;; descriptors and array class names: Lclojure/lang/..., [Lclojure.lang...
   (re-pattern (str "(?<=(?:^|[^\\w])L)clojure(?=" any "(?:lang|asm|java" any "api)" any ")"))
   ;; namespaces, dotted: clojure.core, clojure.core/x, :clojure.core/x, clojure.core$fn, classes
   ;; of their packages (clojure.core.VecNode), not clojure.core.specs.alpha,
   ;; clojure.test-clojure, clojure.test.check
   (re-pattern (str before "clojure(?=" dot "(?:" (alt namespaces dot) ")(?!(?!__)[\\w*+!?-]|" dot "[a-z]))"))
   ;; namespaces, as paths: clojure/core, clojure/core.clj, clojure/core__init, not
   ;; clojure/core/specs/alpha
   (re-pattern (str before "clojure(?=/(?:" (alt namespaces "/") ")(?!(?!__)[\\w-]|/\\w))"))])

(defn rename
  "Renames the vendored Clojure's names in text s."
  [s]
  (reduce (fn [s p] (str/replace s p "arbace")) s patterns))

;; ---------------------------------------------------------------------------------------------
;; deriving arbace/ from clojure/ (bin/vendor-arbace)

(def ^:private edits
  "Hand edits after renaming: [file [old new]...], each old string must occur exactly once."
  {"arbace/core.clj"
   ;; clojure/version.properties is not vendored: its value is folded in (doc/VENDOR-NOTES.md)
   [["(let [^java.util.Properties
      properties (with-open [version-stream (.getResourceAsStream
                                             (arbace.lang.RT/baseLoader)
                                             \"clojure/version.properties\")]
                   (doto (new java.util.Properties)
                     (.load version-stream)))
      version-string (.getProperty properties \"version\")"
     "(let [version-string \"1.13.0-master-SNAPSHOT\""]]
   "arbace/core/server.clj"
   ;; the arbace.server.* system properties
   [["(= k1 \"clojure\")" "(= k1 \"arbace\")"]]})

(defn- edit [path s]
  (reduce (fn [s [old new]]
            (let [n (count (re-seq (re-pattern (java.util.regex.Pattern/quote old)) s))]
              (when (not= 1 n)
                (throw (ex-info (str "edit of " path ": " n " matches of " (pr-str old)) {})))
              (str/replace s old new)))
          s (get edits path)))

(defn- java-header
  "The comments before the package declaration of a Java file, as ;; lines."
  [^java.io.File f]
  (let [lines (take-while #(not (str/starts-with? % "package ")) (str/split-lines (slurp f)))
        lines (reverse (drop-while str/blank? (reverse (drop-while str/blank? lines))))]
    (apply str (for [l lines] (str (str/trimr (str ";; " (str/replace l "\t" "    "))) "\n")))))

(defn- rel [root ^java.io.File f]
  (str (.relativize (.toPath (.getCanonicalFile (io/file root))) (.toPath (.getCanonicalFile f)))))

(defn vendor!
  "Writes the vendored tree into out: the converter's output conv (converted with
  --rename clojure=arbace from the Java files under src/clojure) with its string literals
  renamed and each file headed by its Java file's notice, and the .clj files of src/clojure,
  renamed, with the hand edits above."
  [conv src out]
  (let [files (fn [dir ext] (sort-by str (for [^java.io.File f (file-seq (io/file dir))
                                               :when (and (.isFile f) (str/ends-with? (.getName f) ext))]
                                           f)))
        rev "98d735fab02f"]
    (doseq [f (files conv ".clj")
            :let [path (rel conv f)
                  [first-line & more] (str/split-lines (slurp f))
                  java (second (re-find #"^;; Converted from \S*?/(clojure/\S+\.java) by arbace\.j2c" first-line))
                  pkg (second (re-find #"the namespace of Java package (\S+)\.$" first-line))
                  head (cond
                         java (str (java-header (io/file src java))
                                   (when-not (str/blank? (java-header (io/file src java))) ";;\n")
                                   ";; Converted from " java " of Clojure " rev " by arbace.j2c\n"
                                   ";; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.\n")
                         pkg (str ";; The namespace of Java package " pkg ", written by arbace.j2c from Clojure " rev
                                  ";\n;; see doc/VENDOR-NOTES.md.\n")
                         :else (throw (ex-info (str "unexpected header: " first-line) {})))
                  o (io/file out path)]]
      (io/make-parents o)
      (spit o (str head (rename (str/join "\n" more)) "\n")))
    (doseq [f (files (io/file src "clojure") ".clj")
            :let [path (rename (rel src f))
                  o (io/file out path)]]
      (io/make-parents o)
      (spit o (edit path (rename (slurp f)))))))

(defn -main
  "files FILE...       renames in place, printing the files changed;
   stdin               renames standard input to standard output;
   tree DIR EXT...     renames in place the files under DIR whose names end in one of EXT;
   vendor CONV SRC OUT see vendor!."
  [cmd & args]
  (case cmd
    "files" (doseq [f args]
              (let [s (slurp f) r (rename s)]
                (when (not= s r)
                  (spit f r)
                  (println f))))
    "stdin" (do (print (rename (slurp *in*))) (flush))
    "tree" (let [[dir & exts] args]
             (doseq [^java.io.File f (file-seq (io/file dir))
                     :when (and (.isFile f) (some #(str/ends-with? (.getName f) %) exts))
                     :let [s (slurp f) r (rename s)]
                     :when (not= s r)]
               (spit f r)))
    "vendor" (apply vendor! args)))
