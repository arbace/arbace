(ns arbace.j2c.build
  "Compile converted files with the class forms compiler (arbace.classes, stage 0) into class
  files, to run the converted baseline: Clojure's test suite against classes compiled from the
  converter's output.

  bin/j2c -m arbace.j2c.build CONVERTED-DIR CLASSES-DIR [PACKAGE-DIR...]
    Compiles every converted file of the package directories (default: all directories with
    .clj files) and writes the classes under CLASSES-DIR. Prints the files that do not compile."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- read-forms [f]
  (with-open [r (clojure.lang.LineNumberingPushbackReader. (io/reader f))]
    (binding [*read-eval* false]
      (doall (take-while #(not= % ::eof) (repeatedly #(read {:eof ::eof} r)))))))

(defn compile-file
  "Compile the class forms of converted file `f`; returns {:classes [{:name :bytes}]} or
  {:error msg}."
  [f]
  (let [compile-forms (requiring-resolve 'arbace.classes.compiler/compile-forms)
        forms (read-forms f)
        ns-form (first (filter #(and (seq? %) (#{'in-ns 'ns} (first %))) forms))
        nsname (if (= 'ns (first ns-form)) (second ns-form) (second (second ns-form)))
        ns (create-ns nsname)]
    (binding [*ns* ns]
      (refer 'clojure.core)
      (doseq [fm forms :when (and (seq? fm) (#{'import 'clojure.core/import} (first fm)))] (eval fm)))
    (let [cforms (for [fm forms :when (and (seq? fm) (= 'defclass (first fm)))] (rest fm))]
      (try
        (compile-forms ns cforms)
        (catch Throwable e
          {:error (str (.getMessage e) (some->> (.getCause e) .getMessage (str " / ")))})))))

(defn -main [converted out & pkg-dirs]
  (require 'arbace.classes.boot)
  (let [dirs (if (seq pkg-dirs)
               (map #(io/file converted %) pkg-dirs)
               (distinct (for [^java.io.File f (file-seq (io/file converted))
                               :when (str/ends-with? (.getName f) ".clj")]
                           (.getParentFile f))))
        files (sort-by str (for [^java.io.File d dirs
                                 ^java.io.File f (.listFiles d)
                                 :when (and (.isFile f) (str/ends-with? (.getName f) ".clj"))]
                             f))
        results (doall (for [f files] [f (compile-file f)]))
        bad (filter (comp :error second) results)
        n (atom 0)]
    (doseq [[_ {:keys [classes]}] results
            {:keys [name bytes]} classes]
      (let [o (io/file out (str name ".class"))]
        (io/make-parents o)
        (with-open [s (io/output-stream o)] (.write s ^bytes bytes))
        (swap! n inc)))
    (doseq [[f {:keys [error]}] bad] (println "ERROR" (str f) error))
    (println (format "%d files compiled, %d failed, %d classes written to %s"
                     (- (count files) (count bad)) (count bad) @n out))
    (shutdown-agents)
    (System/exit (if (seq bad) 1 0))))
