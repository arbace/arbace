;; Compiles converted Java files (class forms written by the converter, one file per Java file:
;; (in-ns ...) and (import ...) then defclass forms) with the class forms compiler, without
;; defining anything, and compares each class's shape with the class of the same name on the class
;; path (the baseline's javac classes for clojure/, the running JDK's own classes for a converted
;; JDK module).
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
;; For a JDK module run the JVM with --add-modules ALL-SYSTEM (bin/class-forms-check passes
;; JAVA_OPTS) so that all of the JDK's classes resolve.
(require 'arbace.classes.boot)
(require '[arbace.classes.compiler :as compiler] '[arbace.classes.analyze :as a]
         '[arbace.classes.emit :as e] '[arbace.classes.shape :as shape]
         '[clojure.java.io :as io] '[clojure.string :as str])

(def src-root (or (first *command-line-args*)
                  (throw (IllegalArgumentException. "usage: bin/class-forms-check DIR [SUBDIR]"))))
(def sub-dir (or (second *command-line-args*) "clojure/lang"))
(def only (set (remove str/blank? (str/split (or (System/getenv "ONLY") "") #","))))

(defn read-forms [f]
  (with-open [r (clojure.lang.LineNumberingPushbackReader. (io/reader f))]
    (binding [*read-eval* false]
      (doall (take-while #(not= % ::eof) (repeatedly #(read {:eof ::eof} r)))))))

(def results (atom []))

(defn check-file [f]
  (try
    (let [forms (read-forms f)
          cforms (for [fm forms :when (and (seq? fm) (= 'defclass (first fm)))] (rest fm))]
      (when (seq cforms)
        (let [ns-form (first (filter #(and (seq? %) (= 'in-ns (first %))) forms))
              nsname (second (second ns-form))
              ns (do (remove-ns nsname) (create-ns nsname))
              collisions (binding [*ns* ns]
                           (refer 'clojure.core)
                           (vec (mapcat #(a/import-classes! ns %)
                                        (filter #(and (seq? %) (= 'import (first %))) forms))))
              {:keys [classes]} (binding [*ns* ns
                                          e/*string-concat* (if (= "inline" (System/getenv "CONCAT")) :inline :indy)
                                          e/*method-parameters* (= "1" (System/getenv "PARAMETERS"))
                                          e/*version* (or (some-> (System/getenv "VERSION") parse-long) e/*version*)]
                                  (compiler/compile-forms ns cforms))
              diffs (vec (for [{:keys [name bytes]} classes
                               :let [res (ClassLoader/getSystemResource (str name ".class"))]
                               d (if-not res
                                   [[[name] :missing-in-javac nil]]
                                   (map (fn [[p x y]] [(into [name] p) x y])
                                        (shape/diff (with-open [in (io/input-stream res)] (shape/shape (.readAllBytes in)))
                                                    (shape/shape bytes))))]
                           d))]
          (swap! results conj {:file (str f) :classes (count classes) :ok (empty? diffs)
                               :diffs (vec (take 20 diffs)) :ndiffs (count diffs)
                               :import-collisions collisions}))))
    (catch Throwable e
      (when (System/getenv "TRACE") (.printStackTrace e))
      (swap! results conj {:file (str f)
                           :error (let [m (str (.getMessage e) (when-let [c (.getCause e)] (str " / " (.getMessage c))))]
                                    (subs m 0 (min 400 (count m))))}))))

(doseq [f (sort (filter #(str/ends-with? (.getName ^java.io.File %) ".clj") (file-seq (io/file src-root sub-dir))))
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
