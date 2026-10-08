(ns arbace.j2c.rename
  "The clojure -> arbace renaming of agenda step 4 (doc/VENDOR-NOTES.md, \"What is renamed\"),
  as a textual rewrite of source text. Used by bin/clojure-tests to make a renamed copy of
  Clojure's test suite and its test libraries. It also derived arbace/ from the frozen clojure/
  once (bin/vendor-arbace and bin/vendor-spec, on the branch arbace-for-java-26).

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
  - clojure.jar in usage texts;
  - spec, vendored later (doc/VENDOR-NOTES.md, \"Spec\"): clojure.spec with whatever follows
    (clojure.spec.alpha, .gen.alpha, .test.alpha, :clojure.spec.alpha/problems, the
    clojure.spec.* system properties, clojure/spec/alpha) and clojure.core.specs.alpha.
  Left alone: other libraries' names (clojure.test.check, clojure.tools.namespace, clojure.tools.deps, clojure.data.generators,
  clojure.java.classpath, ...; and the suite's own clojure.test-clojure.*), identifiers that
  merely contain the word (clojure-version, *clojure-version*, refer-clojure, CLOJURE_NS,
  __clojureFnMap, thread names clojure-agent-*; some were renamed by hand after vendoring, see
  doc/VENDOR-NOTES.md), prose and URLs (clojure.org)."
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
   ;; core.specs.alpha (spec.alpha's namespaces are in `prefixes`)
   "core.specs.alpha"
   ;; system properties and the jar name
   "read.eval" "main.report" "basis" "jar"])

(def prefixes
  "Renamed together with whatever follows them."
  ["lang" "asm" "java.api" "compile" "compiler" "server" "error" "spec"])

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
   ;; of their packages (clojure.core.VecNode), not clojure.core.foo (a namespace not listed),
   ;; clojure.test-clojure, clojure.test.check
   (re-pattern (str before "clojure(?=" dot "(?:" (alt namespaces dot) ")(?!(?!__)[\\w*+!?-]|" dot "[a-z]))"))
   ;; namespaces, as paths: clojure/core, clojure/core.clj, clojure/core__init,
   ;; clojure/core/specs/alpha, not clojure/core/foo
   (re-pattern (str before "clojure(?=/(?:" (alt namespaces "/") ")(?!(?!__)[\\w-]|/\\w))"))])

(defn rename
  "Renames the vendored Clojure's names in text s."
  [s]
  (reduce (fn [s p] (str/replace s p "arbace")) s patterns))

(defn -main
  "files FILE...       renames in place, printing the files changed;
   stdin               renames standard input to standard output;
   tree DIR EXT...     renames in place the files under DIR whose names end in one of EXT."
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
               (spit f r)))))
