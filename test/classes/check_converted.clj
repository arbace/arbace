;; Compiles converted Java files (class forms written by the converter, one file per Java
;; file, (in-ns ...) and (import ...) then defclass forms) with the class forms compiler, without
;; defining anything, and compares each class's shape with the class of the same name on the
;; class path (for clojure/lang: the baseline's javac classes).
;; Usage: bin/class-forms-check DIR [PACKAGE-DIR]   (PACKAGE-DIR defaults to clojure/lang)
;; Environment: ONLY=Name,Name (files), ALL=1 (all differences), TRACE=1 (stack traces).
(require 'arbace.classes.boot)
(require '[arbace.classes.compiler :as compiler] '[arbace.classes.shape :as shape] '[clojure.java.io :as io] '[clojure.string :as str])
(def src-root (or (first *command-line-args*) (System/getenv "CONV") (throw (IllegalArgumentException. "usage: bin/class-forms-check DIR"))))
(def pkg-dir (or (second *command-line-args*) "clojure/lang"))
(def only (set (remove str/blank? (str/split (or (System/getenv "ONLY") "") #","))))
(defn read-forms [f]
  (with-open [r (clojure.lang.LineNumberingPushbackReader. (io/reader f))]
    (binding [*read-eval* false]
      (doall (take-while #(not= % ::eof) (repeatedly #(read {:eof ::eof} r)))))))
(def results (atom []))
(defn check-file [f]
  (let [forms (read-forms f)
        ns-form (first (filter #(and (seq? %) (#{'in-ns 'ns} (first %))) forms))
        nsname (if (= 'ns (first ns-form)) (second ns-form) (second (second ns-form)))
        ns (create-ns nsname)]
    (binding [*ns* ns]
      (refer 'clojure.core)
      (doseq [fm forms :when (and (seq? fm) (#{'import 'clojure.core/import} (first fm)))] (eval fm)))
    (let [cforms (for [fm forms :when (and (seq? fm) (= 'defclass (first fm)))] (rest fm))]
      (try
        (let [{:keys [classes]} (compiler/compile-forms ns cforms)
              diffs (vec (for [{:keys [name bytes]} classes
                               :let [res (io/resource (str name ".class"))]
                               d (if-not res
                                   [[[name] :missing-in-javac nil]]
                                   (map (fn [[p a b]] [(into [name] p) a b])
                                        (shape/diff (with-open [in (io/input-stream res)] (shape/shape (.readAllBytes in)))
                                                    (shape/shape bytes))))]
                           d))]
          (swap! results conj {:file (.getName f) :ok (empty? diffs) :diffs diffs}))
        (catch Throwable e
          (when (System/getenv "TRACE") (.printStackTrace e))
          (swap! results conj {:file (.getName f) :error (str (.getMessage e) (when-let [c (.getCause e)] (str " / " (.getMessage c))))}))))))
(doseq [f (sort-by #(.getName %) (.listFiles (io/file src-root pkg-dir)))
        :when (str/ends-with? (.getName f) ".clj")
        :when (or (empty? only) (only (str/replace (.getName f) ".clj" "")))]
  (check-file f))
(let [rs @results]
  (println "files" (count rs) "ok" (count (filter :ok rs)) "errors" (count (filter :error rs)) "diffs" (count (filter #(seq (:diffs %)) rs)))
  (doseq [r rs :when (:error r)] (println "ERROR" (:file r) (:error r)))
  (doseq [r rs :when (seq (:diffs r))]
    (println "DIFF" (:file r) (count (:diffs r)))
    (doseq [d (take (if (System/getenv "ALL") 100 3) (:diffs r))] (println "   " (pr-str d)))))
(System/exit (if (every? :ok @results) 0 1))
