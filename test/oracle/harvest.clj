(ns oracle.harvest
  "Harvests forms for the oracle's forms corpus from Clojure's test suite (doc/go/ORACLE.md),
  as bin/clojure-tests renames it to arbace.*: the expressions under test in its `is` and `are`
  assertions, kept when they are self-contained (they evaluate in a fresh `user` namespace
  without the test file's definitions) and deterministic. Writes
  test/oracle/forms/harvest/NAME.clj, one per test namespace. Run by bin/oracle harvest."
  (:require [arbace.string :as str]
            [arbace.java.io :as io]
            [arbace.walk :as walk]
            [arbace.template :as template]
            [oracle.runner :as runner])
  (:import (java.io File PushbackReader)))

;; Test namespaces left out whole: concurrency and timing (agents, atoms' threads, parallel,
;; refs, server, futures), loading and compiling files or classes (compilation, genclass,
;; ns_libs, main, repl, annotations, reflect, clearing, method_thunks, generated_*,
;; param_tags, java_interop), the test framework itself (test, test_fixtures, run_single_test),
;; I/O (streams, serialization, tap) and errors/rt (stack traces, warnings).
(def excluded
  #{"agents" "annotations" "clearing" "compilation" "errors" "genclass" "generators"
    "generated_all_fi_adapters_in_let" "generated_functional_adapters_in_def"
    "generated_functional_adapters_in_def_requiring_reflection" "java_interop" "main"
    "method_thunks" "ns_libs" "ns_libs_load_later" "parallel" "param_tags" "reflect"
    "refs" "repl" "rt" "run_single_test" "serialization" "server" "streams" "tap" "test"
    "test_fixtures"})

;; Forms mentioning any of these symbols are left out: time, randomness, threads, the host,
;; identity, exiting, the class forms (D6). Proxies are in (the Go build has them over Dyn:
;; C2G-SPEC §10.4, amendment X4).
(def unsafe
  '#{System/exit System/currentTimeMillis System/nanoTime System/getProperty System/getenv
     System/identityHashCode System/gc Runtime/getRuntime Thread/sleep Thread. Thread
     shutdown-agents future future-call pmap pcalls pvalues agent send send-off send-via await
     await-for rand rand-int rand-nth shuffle random-sample random-uuid gensym time
     slurp spit delete-file io/file io/delete-file file-seq load load-file load-string require
     use compile gen-class gen-interface defclass definterface Object. java.util.Date.
     Date. java.util.Random. Math/random add-tap remove-tap tap> System/setProperty
     with-redefs-fn alter-var-root set-validator! *compile-files* *compile-path*
     repeatedly locking promise deliver})

(def proxy-forms
  "Of the namespaces left out whole, the one whose forms naming proxies are harvested all the
  same: java_interop holds the suite's proxy tests (the suite's proxy namespace,
  proxy/examples.clj, only defines the classes they use)."
  {"java_interop" '#{proxy proxy-super update-proxy get-proxy-class construct-proxy init-proxy
                     proxy-mappings proxy-call-with-super}})

(defn- read-all
  "The forms of a test file, read with reader conditionals allowed and any ::alias/name
  resolved to the alias as namespace."
  [^File f]
  (let [resolver (reify arbace.lang.LispReader$Resolver
                   (currentNS [_] 'oracle.harvest.scratch)
                   (resolveClass [_ s] s)
                   (resolveAlias [_ s] s)
                   (resolveVar [_ s] s))]
    (with-open [r (PushbackReader. (io/reader f))]
      (binding [*reader-resolver* resolver *read-eval* false]
        (loop [acc []]
          (let [x (try (read {:read-cond :allow :eof ::eof} r)
                       (catch Throwable _ ::eof))]
            (if (= x ::eof) acc (recur (conj acc x)))))))))

(defn- literal? [x]
  (or (nil? x) (boolean? x) (number? x) (string? x) (keyword? x) (char? x)
      (and (coll? x) (not (seq? x)) (every? literal? (if (map? x) (mapcat identity x) x)))))

(defn- assertions
  "The assertion forms of a test file's forms: (is X ...) gives X; (are argv expr & rows) gives
  expr with each row substituted."
  [forms]
  (let [acc (transient [])]
    (walk/prewalk
     (fn [x]
       (when (seq? x)
         (cond
           (= 'is (first x)) (when (next x) (conj! acc (second x)))
           (and (= 'are (first x)) (vector? (second x)) (pos? (count (second x))))
           (let [[_ argv expr & rows] x]
             (when (zero? (rem (count rows) (count argv)))
               (doseq [row (partition (count argv) rows)]
                 (conj! acc (template/apply-template argv expr row)))))))
       x)
     forms)
    (persistent! acc)))

(defn- exprs
  "The expressions worth evaluating in an assertion: the non-literal arguments of =, ==, not=,
  the body of thrown?/thrown-with-msg?, otherwise the assertion itself when it is a call."
  [a]
  (cond
    (not (seq? a)) []
    (contains? '#{= == not= identical?} (first a)) (filter #(and (seq? %) (not= 'quote (first %))) (rest a))
    (= 'thrown? (first a)) (let [body (nnext a)] (if (next body) [(cons 'do body)] body))
    (= 'thrown-with-msg? (first a)) (let [body (drop 3 a)] (if (next body) [(cons 'do body)] body))
    :else [a]))

(defn- naming? [syms form]
  (let [found (volatile! false)]
    (walk/prewalk (fn [x] (when (and (symbol? x) (contains? syms x)) (vreset! found true)) x) form)
    @found))

(defn- unsafe? [form] (naming? unsafe form))

(defn- form-text
  "The form printed back as source (type hints and other metadata kept), or nil when it does
  not read back."
  [form]
  (let [t (binding [*print-meta* true *print-length* nil *print-level* nil
                    *print-namespace-maps* false]
            (pr-str form))]
    (when (try (read-string {:read-cond :allow} t) true (catch Throwable _ false))
      (when-not (re-find #"#object\[|#=|#<" t) t))))

(def ^:private bad-error
  #"Unable to resolve symbol|No such namespace|No such var|Unable to resolve classname|ClassNotFoundException|Could not locate|Unable to find static field|Can't refer|is not public|Expecting var|Can't resolve|Could not resolve|unknown class|No namespace:")

(defn- bad-case?
  "A trial result that makes the case unfit: missing (the run died there), a failure to
  resolve the test file's own names, an identity-dependent or very long result."
  [c]
  (let [s (runner/emit (select-keys c [:value :out :err :ex :print-ex]))]
    (or (:missing c)
        (re-find bad-error s)
        (re-find #"#object\[|0xN|@N\b|\$evalN|__N" s)
        (> (count s) 3000))))

(defn- ns-preamble
  "The require forms for the aliases of arbace.* namespaces (not arbace.test) and the import
  forms for java.* classes in the test file's ns form."
  [forms]
  (let [nsf (first (filter #(and (seq? %) (= 'ns (first %))) forms))
        clauses (filter seq? nsf)
        lib-ok? (fn [l] (and (symbol? l) (str/starts-with? (str l) "arbace.")
                             (not (str/starts-with? (str l) "arbace.test"))))]
    (concat
     (for [c clauses :when (= :require (first c))
           spec (rest c)
           :when (and (vector? spec) (lib-ok? (first spec)))
           :let [as (second (drop-while #(not= :as %) spec))]
           :when as]
       (pr-str (list 'require (list 'quote [(first spec) :as as]))))
     (for [c clauses :when (= :import (first c))
           spec (rest c)
           :when (and (or (seq? spec) (vector? spec)) (str/starts-with? (str (first spec)) "java."))]
       (pr-str (list 'import (list 'quote (apply list spec))))))))

(defn- write-forms [^File f header texts]
  (.mkdirs (.getParentFile f))
  (spit f (str header (str/join "\n" texts) "\n") :encoding "UTF-8"))

(defn- trial
  "Runs the texts as a forms file twice; the texts whose cases are fit and identical in both."
  [^File tmp header texts]
  (loop [texts texts tries 0]
    (write-forms tmp header texts)
    (let [cases (#'runner/source-cases "forms" tmp)
          r1 (:cases (#'runner/run-file runner/default-impl "forms" tmp cases 120))
          r2 (:cases (#'runner/run-file runner/default-impl "forms" tmp cases 120))
          ks [:value :out :err :ex :print-ex :missing]
          first-missing (first (keep-indexed (fn [i c] (when (:missing c) i)) r1))]
      (if (and first-missing (< tries 20))
        ;; the run died at a case: drop it and try again
        (recur (vec (concat (take first-missing texts) (drop (inc first-missing) texts))) (inc tries))
        (vec (keep (fn [[t a b]] (when (and (not (bad-case? a)) (= (select-keys a ks) (select-keys b ks))) t))
                   (map vector texts r1 r2)))))))

(defn harvest [suite]
  (let [dir (io/file suite "test/clojure/test_clojure")
        files (->> (.listFiles dir)
                   (filter #(re-find #"\.cljc?$" (.getName ^File %)))
                   (sort-by #(.getName ^File %)))
        out-dir (io/file runner/oracle-dir "forms/harvest")
        tmp-dir (io/file runner/root ".tmp/oracle/harvest")]
    (doseq [^File f files
            :let [nm (str/replace (.getName f) #"\.cljc?$" "")]
            :when (or (not (excluded nm)) (proxy-forms nm))]
      (let [forms (read-all f)
            pre (vec (ns-preamble forms))
            only (if (excluded nm) #(naming? (proxy-forms nm) %) (constantly true))
            texts (->> forms assertions (mapcat exprs)
                       (filter only) (remove unsafe?) (keep form-text) distinct vec)
            header (str ";; Harvested by bin/oracle harvest (test/oracle/harvest.clj) from Clojure's test suite,\n"
                        ";; clojure/clojure 98d735fab02f337cee654cb0629bddc09883a75a test/clojure/test_clojure/"
                        (.getName f) ",\n;; renamed to arbace.* by bin/clojure-tests: the expressions of its assertions that are\n"
                        ";; self-contained and deterministic. Eclipse Public License 1.0 (LICENSE.md). Do not edit.\n")
            kept (if (empty? texts) [] (trial (io/file tmp-dir (str nm ".clj")) header (into pre texts)))
            kept (if (= kept pre) [] kept)
            out (io/file out-dir (str nm ".clj"))]
        (println (format "%-40s %5d candidates %5d kept" nm (count texts) (count kept)))
        (if (seq kept)
          (write-forms out header kept)
          (.delete out))))))

(defn -main [suite]
  (harvest suite)
  (shutdown-agents)
  (System/exit 0))
