(ns classes.helpers
  "Helpers for the class forms tests: compile class forms and equivalent Java with javac, and
  compare the class shapes (SPEC §3). Everything is written under .tmp/class-forms-tests/."
  (:require [arbace.test :refer [is]]
            [arbace.java.io :as io]
            [arbace.string :as str]
            [arbace.classes.analyze :as a]
            [arbace.classes.compiler :as compiler]
            [arbace.classes.shape :as shape]))

(def tmp-root (io/file (or (System/getProperty "arbace.tmp") ".tmp") "class-forms-tests"))

(defn package-of-ns [ns] (a/ns-package (the-ns ns)))

(defn javac-classes
  "Compiles Java sources {\"Name\" \"source without package line\"} in the package of ns.
  Returns {internal-name bytes}."
  [ns sources & {:keys [options]}]
  (let [pkg (package-of-ns ns)
        dir (io/file tmp-root "javac" (str/replace pkg "/" "_") (str (System/nanoTime)))
        src (io/file dir "src") out (io/file dir "out")]
    (.mkdirs out)
    (let [files (doall (for [[n s] sources]
                         (let [f (io/file src (if (str/starts-with? s "package ")
                                                (str/replace (second (re-find #"package ([\w.]+);" s)) "." "/")
                                                pkg)
                                          (str n ".java"))]
                           (io/make-parents f)
                           (spit f (if (str/starts-with? s "package ")
                                     s
                                     (str "package " (str/replace pkg "/" ".") ";\n" s)))
                           (.getPath f))))
          javac (javax.tools.ToolProvider/getSystemJavaCompiler)
          err (java.io.ByteArrayOutputStream.)
          rc (.run javac nil nil err (into-array String (concat ["-d" (.getPath out) "-proc:none"] options files)))]
      (when-not (zero? rc) (throw (ex-info (str "javac failed: " err) {})))
      (into {} (for [f (file-seq out) :when (str/ends-with? (.getName f) ".class")]
                 [(-> (.getPath f) (subs (inc (count (.getPath out)))) (str/replace #"\.class$" ""))
                  (java.nio.file.Files/readAllBytes (.toPath f))])))))

(defn forms-classes
  "Compiles class forms (each the rest of a defclass form) in ns. Returns {internal-name bytes}."
  [ns forms]
  (into {} (for [{:keys [name bytes]} (:classes (compiler/compile-forms (the-ns ns) forms))]
             [name bytes])))

(defn ignored? [ignore [path]]
  (some (fn [p] (= p (take (count p) path))) ignore))

(defn shape-diffs
  "Differences between the shapes of two sets of classes, minus ignored paths
  (paths start with the class name: [\"p/C\" :methods [\"m\" \"()V\"] :code])."
  [java forms & {:keys [ignore]}]
  (let [names (sort (distinct (concat (keys java) (keys forms))))]
    (vec (for [n names
               :let [j (get java n) f (get forms n)]
               d (cond
                   (nil? j) [[[n] :missing-in-javac :present]]
                   (nil? f) [[[n] :present :missing-in-forms]]
                   :else (map (fn [[p x y]] [(into [n] p) x y])
                              (shape/diff (shape/shape j) (shape/shape f))))
               :when (not (ignored? ignore d))]
           d))))

(defmacro same-shapes?
  "Asserts that class forms compile to classes equivalent to javac's for the Java sources."
  [ns java-sources forms & opts]
  `(let [ds# (shape-diffs (javac-classes ~ns ~java-sources) (forms-classes ~ns ~forms) ~@opts)]
     (is (empty? ds#) (with-out-str (doseq [d# ds#] (prn d#))))))

(defn load-forms
  "Compiles and defines class forms in ns (as defclasses does). Returns the classes."
  [ns forms]
  (mapv second (compiler/compile-and-load! (the-ns ns) forms)))
