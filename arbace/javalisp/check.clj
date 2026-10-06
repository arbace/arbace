(ns arbace.javalisp.check
  "Round-trip checks: Java -> javalisp -> Java must give back the same javac tree, with the
  same line for every position javac records, and the javalisp text must be readable by
  both clojure.edn and clojure.core."
  (:require [arbace.javalisp.javac :as javac]
            [arbace.javalisp.tree :as tree]
            [arbace.javalisp.j2l :as j2l]
            [arbace.javalisp.l2j :as l2j]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.io PushbackReader StringReader]))

(defn read-all
  "Read every form of `text` with `read-fn` (edn/read or clojure.core/read)."
  [read-fn text]
  (let [r (PushbackReader. (StringReader. text))
        eof (Object.)]
    (loop [n 0]
      (let [f (read-fn r eof)]
        (if (identical? f eof) n (recur (inc n)))))))

(defn readable?
  "nil when both Clojure readers accept `text`, else the error."
  [text]
  (try
    (read-all #(edn/read {:eof %2} %1) text)
    (binding [*read-eval* false]
      (read-all #(read %1 false %2) text))
    nil
    (catch Throwable e (str (.getMessage e) (some->> (.getCause e) .getMessage (str ": "))))))

(defn roundtrip
  "Check one Java file. Returns {:ok true} or {:stage .. :detail ..}."
  [path text]
  (try
    (let [{:keys [cu errors]} (javac/parse path text)]
      (if (seq errors)
        {:stage :parse :detail (first errors)}
        (let [{clj :text v1 :violations :as t} (j2l/transcribe path text)
              rerr (readable? clj)
              {java :text v2 :violations} (l2j/translate clj)
              {cu2 :cu errors2 :errors} (javac/parse path java)]
          (cond
            (seq (:errors t)) {:stage :transcribe :detail (first (:errors t))}
            rerr {:stage :read :detail rerr :clj clj}
            (seq errors2) {:stage :reparse :detail (first errors2) :clj clj :java java}
            :else (let [d (tree/diff (tree/view cu cu) (tree/view cu2 cu2))
                        clj2 (when-not (or d (seq v1) (seq v2)) (:text (j2l/transcribe path java)))
                        idem (when (and clj2 (not= clj clj2))
                               (let [a (str/split-lines clj) b (str/split-lines clj2)
                                     i (count (take-while true? (map = a b)))]
                                 {:line (inc i) :clj (get a i) :again (get b i)}))]
                    (cond-> {:clj clj :java java}
                      d (assoc :stage :diff :detail d)
                      (seq v1) (assoc :layout v1)
                      (seq v2) (assoc :layout2 v2)
                      idem (assoc :stage :idempotence :detail idem)
                      (not (or d idem (seq v1) (seq v2))) (assoc :ok true)
                      (and (not d) (or (seq v1) (seq v2))) (assoc :stage :layout)))))))
    (catch Throwable e
      {:stage :exception :detail (str (.getName (class e)) ": " (.getMessage e))
       :trace (take 8 (filter #(re-find #"arbace" %) (map str (.getStackTrace e))))})))

(defn java-files [paths]
  (sort (for [p paths
              ^java.io.File f (file-seq (io/file p))
              :when (and (.isFile f) (str/ends-with? (.getName f) ".java"))]
          (.getPath f))))

(defn check
  "Round-trip all .java files under `paths`; print a summary and the failures."
  [paths & {:keys [verbose out-dir]}]
  (let [files (java-files paths)
        results (doall (pmap (fn [f] (assoc (roundtrip f (slurp f)) :file f)) files))
        bad (remove :ok results)]
    (when out-dir
      (doseq [{:keys [file clj java]} results]
        (when clj
          (let [o (io/file out-dir (str (str/replace file #"^/+" "") ".clj"))]
            (io/make-parents o)
            (spit o clj)))
        (when java
          (spit (io/file out-dir (str (str/replace file #"^/+" "") ".rt.java")) java))))
    (doseq [{:keys [file stage detail layout layout2 trace clj java]} bad]
      (println (str file ": " (name stage) " " (pr-str detail)))
      (when verbose
        (doseq [v (concat layout layout2)] (println "   " (pr-str v)))
        (doseq [t trace] (println "   at" t))
        (when (and (= stage :diff) (:near detail))
          (let [src (str/split-lines (slurp file))
                lines (fn [text] (str/split-lines (or text "")))
                [lo hi] (if (and (number? (:a detail)) (number? (:b detail)) (= :line (last (:path detail))))
                          [(min (:a detail) (:b detail)) (max (:a detail) (:b detail))]
                          [(:near detail) (:near detail)])
                show (fn [label ls]
                       (doseq [l (range (max 1 (dec lo)) (inc hi))]
                         (println (format "   %-5s %5d| %s" label l (get ls (dec l) "")))))]
            (show "java" src) (show "clj" (lines clj)) (show "rt" (lines java))))))
    (println (format "%d files, %d ok, %d failed %s" (count files) (- (count files) (count bad))
                     (count bad) (pr-str (frequencies (map :stage bad)))))
    bad))

(defn compare-classes
  "Compile the .java files under `paths` twice, as they are and after a round trip through
  javalisp, with javac options `opts`; report every class whose bytes differ."
  [paths opts]
  (let [files (java-files paths)
        originals (into (sorted-map) (for [f files] [f (slurp f)]))
        roundtripped (into (sorted-map)
                           (pmap (fn [[f text]]
                                   [f (:text (l2j/translate (:text (j2l/transcribe f text))))])
                                 originals))
        a (javac/compile-sources originals opts)
        b (javac/compile-sources roundtripped opts)
        differ (for [[k bytes] (:classes a)
                     :when (not (java.util.Arrays/equals ^bytes bytes ^bytes (get (:classes b) k)))]
                 k)]
    (println (format "%d sources, %d classes / %d classes, errors %d / %d, %d differ"
                     (count files) (count (:classes a)) (count (:classes b))
                     (count (:errors a)) (count (:errors b)) (count differ)))
    (run! println (take 5 (:errors b)))
    (run! #(println "differs:" %) (take 20 differ))
    (empty? differ)))

(defn compare-each
  "Compile each .java file under `paths` on its own, as it is and after a round trip through
  javalisp, with javac options `opts`. Files that do not compile on their own are skipped.
  Report every file whose class bytes differ."
  [paths opts]
  (let [files (java-files paths)
        results (doall
                 (pmap (fn [f]
                         (try
                           (let [text (slurp f)
                                 a (javac/compile-sources {f text} opts)
                                 same? (fn [x y] (java.util.Arrays/equals ^bytes x ^bytes y))]
                             (if (or (seq (:errors a)) (empty? (:classes a)))
                               {:file f :skip true}
                               (let [a2 (javac/compile-sources {f text} opts)
                                     stable (into {} (filter (fn [[k v]] (same? v (get (:classes a2) k)))) (:classes a))
                                     rt (:text (l2j/translate (:text (j2l/transcribe f text))))
                                     b (javac/compile-sources {f rt} opts)
                                     differ (for [[k bytes] stable :when (not (same? bytes (get (:classes b) k)))] k)]
                                 {:file f :classes (count stable) :unstable (- (count (:classes a)) (count stable))
                                  :differ differ :errors (:errors b)})))
                           (catch Throwable e {:file f :error (str e)})))
                       files))
        compiled (remove :skip results)
        bad (filter #(or (seq (:differ %)) (seq (:errors %)) (:error %)) compiled)]
    (doseq [r bad] (println (:file r) (pr-str (select-keys r [:differ :errors :error]))))
    (println (format "%d files, %d compiled alone (%d classes compared, %d skipped as javac is not deterministic on them), %d differ"
                     (count files) (count compiled) (reduce + (keep :classes compiled))
                     (reduce + (keep :unstable compiled)) (count bad)))
    (empty? bad)))
