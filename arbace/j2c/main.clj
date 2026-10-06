(ns arbace.j2c.main
  "Command line of the Java -> class forms converter. See doc/classes/CONVERTER-NOTES.md.

  bin/j2c -m arbace.j2c.main convert [options] OUT ROOT [PATH...]
    Converts the .java files under ROOT (or only those under the PATHs, relative to ROOT or
    absolute) into class-form files under OUT, one namespace per package. ROOT is also
    javac's source path.
    Options:
      --rename FROM=TO   rename the package prefix FROM to TO (e.g. clojure=arbace)
      --javac OPT        pass an option to javac (repeatable)
      --check            also check that every output reads with clojure.core/read and
                         clojure.edn/read, and write a coverage report to OUT/j2c-report.edn"
  (:require [arbace.j2c.javac :as javac]
            [arbace.j2c.convert :as cv]
            [arbace.j2c.jtypes :as jt]
            [arbace.j2c.names :as names]
            [arbace.j2c.print :as pr]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str])
  (:import [java.io File PushbackReader StringReader]))

(defn java-files [paths]
  (sort (for [p paths
              ^File f (file-seq (io/file p))
              :when (and (.isFile f) (str/ends-with? (.getName f) ".java"))]
          (.getPath f))))

(defn rename-fn
  "A function renaming binary/package names by the prefix map {from to}."
  [m]
  (if (empty? m)
    identity
    (fn [^String s]
      (or (some (fn [[from to]]
                  (cond
                    (= s from) to
                    (str/starts-with? s (str from ".")) (str to (subs s (count from)))))
                m)
          s))))

(defn read-all [read-fn text]
  (let [r (PushbackReader. (StringReader. text))
        eof (Object.)]
    (loop [n 0]
      (let [f (read-fn r eof)]
        (if (identical? f eof) n (recur (inc n)))))))

(defn- read-error [f text]
  (try (f text) nil
       (catch Throwable e (str (.getMessage e) (some->> (.getCause e) .getMessage (str ": "))))))

(defn readable
  "{:core err :edn err}: nil errors when clojure.core/read and clojure.edn/read read `text`."
  [text]
  {:core (read-error (fn [t] (binding [*read-eval* false] (read-all #(read %1 false %2) t))) text)
   :edn (read-error (fn [t] (read-all #(edn/read {:eof %2 :default (fn [t v] v)} %1) t)) text)})

(defn convert-units
  "Analyze the Java `files` with javac options `opts` and convert every compilation unit.
  Returns {:units [...] :failures [...] :errors [...] :stats {...}}."
  [files opts rename]
  (let [{:keys [units context errors]} (javac/analyze files opts)
        stats (atom {})]
    (binding [jt/*types* (javac/types context)
              jt/*syms* (javac/symtab context)
              jt/*names* (com.sun.tools.javac.util.Names/instance context)
              jt/*rename* (rename-fn rename)
              cv/*stats* stats]
      (let [results (doall
                     (for [cu units
                           :let [src (.getName (.getSourceFile ^com.sun.tools.javac.tree.JCTree$JCCompilationUnit cu))]]
                       (try
                         (assoc (cv/convert-unit cu {}) :source src)
                         (catch Throwable e
                           {:failure true :source src
                            :error (str (.getName (class e)) ": " (.getMessage e))
                            :trace (vec (take 12 (filter #(re-find #"arbace" %) (map str (.getStackTrace e)))))}))))]
        {:units (vec (remove :failure results))
         :failures (vec (filter :failure results))
         :errors errors
         :stats @stats}))))

(defn- out-file-name
  "The output file name for unit `u`: the Java file's base name, with _class appended when the
  source directory has a .clj file of that name (clojure/main.java and clojure/main.clj)."
  [{:keys [source file]}]
  (let [base (str/replace file #"\.java$" "")
        sib (io/file (.getParentFile (io/file source)) (str base ".clj"))]
    (if (.exists sib) (str base "_class") base)))

(defn write-output
  "Write the converted `units` under directory `out`. Returns [{:path p :text t}]."
  [out units]
  (vec
   (for [[pkg us] (group-by :package units)
         :let [table (names/naming pkg us)
               ordered (names/topo-order us)
               pkg-path (str/replace pkg "." "/")
               last-seg (last (str/split pkg #"\."))
               files (for [u ordered]
                       (let [nm (out-file-name u)
                             text (names/unit-text table pkg u)
                             path (str (if (seq pkg) (str pkg-path "/") "") nm ".clj")]
                         {:path path :text text :load (str last-seg "/" nm)}))]
         f (concat files
                   (when (seq pkg)
                     [{:path (str pkg-path ".clj")
                       :text (names/package-text pkg (map :load files))}]))]
     (let [o (io/file out (:path f))]
       (io/make-parents o)
       (spit o (:text f))
       f))))

(defn head-symbols
  "Counts of the heads of lists and the metadata keywords used in the forms of `units`."
  [units]
  (let [counts (volatile! {})]
    (letfn [(bump [k] (vswap! counts update k (fnil inc 0)))
            (walk [x]
              (when (instance? clojure.lang.IObj x)
                (doseq [it (arbace.j2c.forms/items x)]
                  (if (keyword? it) (bump (str "^:" (name it))) (bump (str "^" (name (first it)))))))
              (cond
                (seq? x) (do (when-let [hd (first x)]
                               (bump (cond
                                       (instance? arbace.j2c.forms.CRef hd)
                                       (case (:member hd) "." "(Class. ...)" "new" "(Class/new ...)"
                                             (if (str/starts-with? (str (:member hd)) ".") "(Class/.method ...)" "(Class/static ...)"))
                                       (not (symbol? hd)) "(non-symbol head)"
                                       (str/starts-with? (name hd) ".-") "(.-field ...)"
                                       (and (str/starts-with? (name hd) ".") (not (#{"." ".." ".new" ".super"} (name hd)))) "(.method ...)"
                                       (and (namespace hd) (not (#{"clojure.core" "arbace.core"} (namespace hd)))) "(Class/static ...)"
                                       (str/ends-with? (name hd) ".") (if (#{"this." "super."} (name hd)) (name hd) "(Class. ...)")
                                       :else (name hd))))
                             (run! walk x))
                (map? x) (doseq [[k v] x] (walk k) (walk v))
                (coll? x) (run! walk x)))]
      (run! #(run! walk (:forms %)) units))
    @counts))

(defn convert
  [out root paths {:keys [rename javac-opts check]}]
  (let [root (.getCanonicalPath (io/file root))
        paths (if (seq paths)
                (map #(if (.isAbsolute (io/file %)) % (str root "/" %)) paths)
                [root])
        files (java-files paths)
        t0 (System/nanoTime)
        {:keys [units failures errors stats]} (convert-units files (into ["-sourcepath" root] javac-opts) rename)
        written (write-output out units)
        reads (when check (vec (pmap (fn [{:keys [path text]}] (assoc (readable text) :path path)) written)))
        unreadable (vec (filter :core reads))
        edn-errors (frequencies (keep :edn reads))
        secs (/ (- (System/nanoTime) t0) 1e9)]
    (doseq [e (take 20 errors)] (println "javac:" e))
    (doseq [f failures] (println "FAILED" (:source f) (:error f)) (run! #(println "   " %) (:trace f)))
    (doseq [u unreadable] (println "UNREADABLE" (:path u) (:core u)))
    (when (seq edn-errors) (println "clojure.edn/read errors (count by message):" edn-errors))
    (println (format "%d Java files, %d converted, %d failed, %d javac errors, %d files written to %s%s in %.1fs"
                     (count files) (count units) (count failures) (count errors) (count written) out
                     (if check (format ", %d unreadable" (count unreadable)) "")
                     secs))
    (when check
      (let [report {:files (count files) :converted (count units) :failures (mapv #(select-keys % [:source :error]) failures)
                    :javac-errors (count errors) :unreadable unreadable
                    :edn-errors edn-errors
                    :stats (into (sorted-map) stats)
                    :heads (into (sorted-map) (head-symbols units))}]
        (spit (io/file out "j2c-report.edn") (with-out-str (pp/pprint report)))))
    {:failures failures :unreadable unreadable :errors errors}))

(defn- parse-opts [args]
  (loop [args args opts {:rename {} :javac-opts []} pos []]
    (if (empty? args)
      [opts pos]
      (let [[a b & more] args]
        (case a
          "--rename" (let [[from to] (str/split b #"=")] (recur more (assoc-in opts [:rename from] to) pos))
          "--javac" (recur more (update opts :javac-opts conj b) pos)
          "--check" (recur (rest args) (assoc opts :check true) pos)
          (recur (rest args) opts (conj pos a)))))))

(defn -main [cmd & args]
  (case cmd
    "convert" (let [[opts [out root & paths]] (parse-opts args)]
                (convert out root paths opts)))
  (shutdown-agents))
