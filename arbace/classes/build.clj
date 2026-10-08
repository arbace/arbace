(ns arbace.classes.build
  "Compiles trees of class forms sources (one file per Java file, `in-ns`, `import` and `defclass`
  forms, SPEC §9.1) into class files, without defining them: how bin/build-arbace makes each
  stage of the bootstrap (SPEC §9.6). Runs at every stage, loaded by arbace.classes.boot.

  The classes of the packages being built are compiled from their sources only (env/*from-source*):
  when a stage recompiles itself, its own loaded classes of the same names are ignored, so every
  stage sees the same class environment, the one the stage before it sees.

  (build! root out pkg-dirs) or, from a command line after loading arbace.classes.boot,
  (arbace.classes.build/-main ROOT OUT PKG-DIR...): compiles the files of the package directories
  PKG-DIR (relative to ROOT, which must be on the class path) that hold class forms, in order of
  their paths, and writes the classes under OUT."
  (:require [arbace.java.io :as io]
            [arbace.string :as str]
            [arbace.classes.env :as env]
            [arbace.classes.analyze :as a]
            [arbace.classes.compiler :as compiler]))

(defn- read-forms [f]
  (with-open [r (arbace.lang.LineNumberingPushbackReader. (io/reader f))]
    (binding [*read-eval* false]
      (doall (take-while #(not= % ::eof) (repeatedly #(read {:eof ::eof :read-cond :allow} r)))))))

(defn- class-form? [f] (and (seq? f) (symbol? (first f)) (= "defclass" (name (first f)))))

(defn compile-file
  "Compiles the class forms of file f; returns {:classes [{:name :bytes}]} or {:error msg}."
  [f]
  (let [forms (read-forms f)]
    (binding [*ns* *ns*]
      (doseq [fm forms
              :when (and (seq? fm) (symbol? (first fm)) (#{"in-ns" "import"} (name (first fm))))]
        (a/eval-ns-form! fm))
      (let [ns *ns*]
        (try
          (compiler/compile-forms ns (map rest (filter class-form? forms)))
          (catch Throwable e
            {:error (str (.getMessage e) (some->> (.getCause e) .getMessage (str " / ")))}))))))

(defn source-files
  "The files of the package directories that hold class forms, sorted by path."
  [root pkg-dirs]
  (sort-by str (for [d pkg-dirs
                     ^java.io.File f (.listFiles (io/file root d))
                     :when (and (.isFile f) (str/ends-with? (.getName f) ".clj")
                                (some class-form? (read-forms f)))]
                 f)))

(defn build!
  "Compiles the class forms files of pkg-dirs (relative to root) and writes the classes under
  out. Returns {:files n :failed [[file msg]...] :classes n}."
  [root out pkg-dirs]
  (let [pkgs (set (map #(str/replace % "\\" "/") pkg-dirs))
        files (source-files root pkg-dirs)
        n (atom 0)
        failed (atom [])]
    (binding [env/*from-source* (fn [n] (contains? pkgs (let [i (.lastIndexOf ^String n "/")]
                                                          (if (neg? i) "" (subs n 0 i)))))]
      (doseq [f files]
        (let [{:keys [classes error]} (compile-file f)]
          (if error
            (swap! failed conj [(str f) error])
            (do (compiler/write-classes! out classes)
                (swap! n + (count classes)))))))
    {:files (count files) :failed @failed :classes @n}))

(defn -main [root out & pkg-dirs]
  (let [{:keys [files failed classes]} (build! root out pkg-dirs)]
    (doseq [[f msg] failed] (println "ERROR" f msg))
    (println (format "%d files compiled, %d failed, %d classes written to %s"
                     (- files (count failed)) (count failed) classes out))
    (shutdown-agents)
    (System/exit (if (seq failed) 1 0))))
