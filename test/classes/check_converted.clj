;; Compiles converted Java files (class forms written by the converter, one file per Java file:
;; (in-ns ...) and (import ...) then defclass forms) with the class forms compiler, without
;; defining anything, and compares each class's shape with the class of the same name on the class
;; path (for the frozen clojure/, its javac classes, which bin/j2c-check puts on the class path;
;; for a converted JDK module, the running JDK's own classes). Runs on Arbace (bin/lib/tools.bash).
;;
;; Usage: bin/class-forms-check DIR [SUBDIR]
;;   checks every .clj file under DIR/SUBDIR (SUBDIR defaults to clojure/lang); each file is
;;   compiled in a fresh namespace of its own, as Java imports are per file
;; Environment:
;;   ONLY=Name,Name   only these files (base names without .clj)
;;   ALL=1            print all differences of a file, not the first three
;;   TRACE=1          stack traces of errors
;;   CONCAT=inline    javac's -XDstringConcat=inline;  PARAMETERS=1  javac's -parameters
;;   VERSION=69       the class file version (default: the running JDK's)
;;   OUT=file.edn     also write the results as EDN
;;   WRITE=dir        also write the compiled classes under dir
;;   FILES=list       check only the files listed (one path per line, relative to DIR)
;;   REF=dir[:dir2]   javac's classes from directory dir instead of the class path; with dir2 (a
;;                    second javac run of the same sources), classes whose shapes differ between
;;                    the two runs are not compared (javac's nondeterminism, :nondet). With REF,
;;                    the classes javac made from the Java file (by SourceFile) that the class
;;                    forms did not make are differences too, and defpackage and defmodule forms
;;                    are compiled and compared (package-info, module-info)
;; For a JDK module run the JVM with --add-modules ALL-SYSTEM (bin/class-forms-check passes
;; JAVA_OPTS) so that all of the JDK's classes resolve.
(require '[arbace.classes.compiler :as compiler] '[arbace.classes.analyze :as a]
         '[arbace.classes.emit :as e] '[arbace.classes.shape :as shape]
         '[arbace.java.io :as io] '[arbace.string :as str])

(def src-root (or (first *command-line-args*)
                  (throw (IllegalArgumentException. "usage: bin/class-forms-check DIR [SUBDIR]"))))
(def sub-dir (or (second *command-line-args*) "clojure/lang"))
(def only (set (remove str/blank? (str/split (or (System/getenv "ONLY") "") #","))))

(defn read-forms [f]
  (with-open [r (arbace.lang.LineNumberingPushbackReader. (io/reader f))]
    (binding [*read-eval* false]
      (doall (take-while #(not= % ::eof) (repeatedly #(read {:eof ::eof} r)))))))

(def results (atom []))

(def ref-dirs (some-> (System/getenv "REF") (str/split #":")))

(defn ref-bytes [dir name]
  (let [f (io/file dir (str name ".class"))]
    (when (.isFile f) (java.nio.file.Files/readAllBytes (.toPath f)))))

(defn- class-bytes [name]
  (if ref-dirs
    (ref-bytes (first ref-dirs) name)
    (when-let [res (ClassLoader/getSystemResource (str name ".class"))]
      (with-open [in (io/input-stream res)] (.readAllBytes in)))))

(def ^:private source-index
  ;; package path -> {source file name -> #{class names}} of javac's classes (by SourceFile)
  (memoize
   (fn [pkg-path]
     (let [d (io/file (first ref-dirs) pkg-path)]
       (reduce (fn [m ^java.io.File f]
                 (let [src (atom nil)
                       cr (arbace.asm.ClassReader. (java.nio.file.Files/readAllBytes (.toPath f)))]
                   (.accept cr (proxy [arbace.asm.ClassVisitor] [arbace.asm.Opcodes/ASM9]
                                 (visitSource [s _] (reset! src s)))
                            (bit-or arbace.asm.ClassReader/SKIP_CODE arbace.asm.ClassReader/SKIP_FRAMES))
                   (update m @src (fnil conj #{}) (str (when (seq pkg-path) (str pkg-path "/"))
                                                       (str/replace (.getName f) #"\.class$" "")))))
               {}
               (filter #(and (.isFile ^java.io.File %) (str/ends-with? (.getName ^java.io.File %) ".class"))
                       (.listFiles d)))))))

(defn- javac-classes
  "The classes javac made from the Java file converted into f (by SourceFile)."
  [f]
  (let [rel (str (.relativize (.toPath (.getCanonicalFile (io/file src-root))) (.toPath (.getCanonicalFile (io/file f)))))
        pkg-path (or (some-> (.getParent (io/file rel)) (str/replace java.io.File/separator "/")) "")
        java-name (str (str/replace (.getName (io/file rel)) #"(_class)?\.clj$" "") ".java")]
    (get (source-index pkg-path) java-name #{})))

(defn- extra-classes
  "package-info and module-info classes of defpackage and defmodule forms (only with REF)."
  [ns forms]
  (when ref-dirs
    (concat
     (for [fm forms :when (and (seq? fm) (= 'defpackage (first fm)))]
       (compiler/package-bytes ns (second fm)))
     (for [fm forms :when (and (seq? fm) (= 'defmodule (first fm)))]
       {:name "module-info" :bytes (compiler/module-bytes ns (rest fm))}))))

(defn check-file [f]
  (try
    (let [forms (read-forms f)
          cforms (for [fm forms :when (and (seq? fm) (= 'defclass (first fm)))] (rest fm))]
      (when (or (seq cforms)
                (and ref-dirs (some #(and (seq? %) (#{'defpackage 'defmodule} (first %))) forms)))
        (let [ns-form (first (filter #(and (seq? %) (= 'in-ns (first %))) forms))
              nsname (second (second ns-form))
              ns (do (remove-ns nsname) (create-ns nsname))
              collisions (binding [*ns* ns]
                           (refer 'arbace.core)
                           (vec (mapcat #(a/import-classes! ns %)
                                        (filter #(and (seq? %) (= 'import (first %))) forms))))
              classes (binding [*ns* ns
                                          e/*string-concat* (if (= "inline" (System/getenv "CONCAT")) :inline :indy)
                                          e/*method-parameters* (= "1" (System/getenv "PARAMETERS"))
                                          e/*version* (or (some-> (System/getenv "VERSION") parse-long) e/*version*)]
                        (vec (concat (when (seq cforms) (:classes (compiler/compile-forms ns cforms)))
                                     (extra-classes ns forms))))
              _ (when-let [w (System/getenv "WRITE")] (compiler/write-classes! w classes))
              nondet (when (second ref-dirs)
                       (set (for [{:keys [name]} classes
                                  :let [a (ref-bytes (first ref-dirs) name)
                                        b (ref-bytes (second ref-dirs) name)]
                                  :when (and a b (seq (shape/diff (shape/shape a) (shape/shape b))))]
                              name)))
              missing (when ref-dirs
                        (sort (remove (set (map :name classes)) (javac-classes f))))
              diffs (vec (concat
                          (for [{:keys [name bytes]} classes
                                :when (not (contains? nondet name))
                                :let [ref (class-bytes name)]
                                d (if-not ref
                                    [[[name] :missing-in-javac nil]]
                                    (map (fn [[p x y]] [(into [name] p) x y])
                                         (shape/diff (shape/shape ref) (shape/shape bytes))))]
                            d)
                          (for [name missing] [[name] nil :missing-in-forms])))]
          (swap! results conj {:file (str f) :classes (count classes) :ok (empty? diffs)
                               :diffs (vec (take 20 diffs)) :ndiffs (count diffs)
                               :nondet (vec (sort nondet))
                               :import-collisions collisions}))))
    (catch Throwable e
      (when (System/getenv "TRACE") (.printStackTrace e))
      (swap! results conj {:file (str f)
                           :error (let [m (str (.getMessage e) (when-let [c (.getCause e)] (str " / " (.getMessage c))))]
                                    (subs m 0 (min 400 (count m))))}))))

(doseq [f (if-let [l (System/getenv "FILES")]
            (map #(io/file src-root %) (remove str/blank? (str/split-lines (slurp l))))
            (sort (filter #(str/ends-with? (.getName ^java.io.File %) ".clj") (file-seq (io/file src-root sub-dir)))))
        :when (or (empty? only) (only (str/replace (.getName ^java.io.File f) ".clj" "")))]
  (check-file f))

(when-let [out (System/getenv "OUT")] (spit out (pr-str @results)))

(let [rs @results]
  (println src-root sub-dir "files" (count rs) "ok" (count (filter :ok rs))
           "errors" (count (filter :error rs)) "diffs" (count (filter #(pos? (:ndiffs % 0)) rs)))
  (doseq [r rs :when (:error r)] (println "ERROR" (:file r) (:error r)))
  (doseq [r rs :when (pos? (:ndiffs r 0))]
    (println "DIFF" (:file r) (:ndiffs r))
    (doseq [d (take (if (System/getenv "ALL") 100 3) (:diffs r))] (println "   " (pr-str d))))
  (shutdown-agents)
  (System/exit (if (every? :ok rs) 0 1)))
