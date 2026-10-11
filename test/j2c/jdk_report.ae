;; The report of bin/j2c-check --jdk: per module, the Java files converted, the converted files
;; compiled by the class forms compiler, those whose classes are all shape-identical to javac's,
;; and the differences and compile errors by kind.
;;
;; Usage: run by test/j2c/jdk.bash on Arbace: arbace.lang.Main test/j2c/jdk_report.clj WORK MODULE...
;; Reads WORK/conv/M/j2c-report.edn and WORK/res/M.*.edn; writes WORK/report.md, WORK/report.edn.
(require '[arbace.java.io :as io] '[arbace.string :as str] '[arbace.pprint :as pp])

(def work (first *command-line-args*))
(def modules (rest *command-line-args*))

(defn- read-edn [f] (binding [*read-eval* false] (read-string (slurp f))))

(defn diff-kind
  "A difference [path javac forms] as a kind: the attribute, or for members the member's
  attribute, missing members and classes."
  [[[_ k & more] x y]]
  (cond
    (= x :missing-in-javac) "class not made by javac"
    (= y :missing-in-forms) "class not made by the forms"
    (#{:methods :fields} k) (let [[member attr] more
                                  kind (if (= k :methods) "method" "field")]
                              (cond (nil? attr) (if (nil? x) (str "extra " kind) (str "missing " kind))
                                    :else (str kind " " (name attr))))
    :else (name k)))

(defn error-kind
  "A compile error message with names and numbers taken out."
  [msg]
  (as-> msg msg
      (str/replace msg #"(?s) / .*" "")
      (str/replace msg #"[\w$]+(?:[./][\w$<>]+)+" "_")
      (str/replace msg #"\b\d+\b" "N")
      (str/replace msg #"\b[a-z]\w*[A-Z]\w*\b" "_")
      (subs msg 0 (min 90 (count msg)))))

(defn module-result [m]
  (let [conv (let [f (io/file work "conv" m "j2c-report.edn")] (when (.exists f) (read-edn f)))
        rs (vec (mapcat read-edn (sort (filter #(re-matches (re-pattern (str "\\Q" m "\\E\\.\\d+\\.edn")) (.getName ^java.io.File %))
                                               (.listFiles (io/file work "res"))))))
        errors (filter :error rs)
        differing (filter #(pos? (:ndiffs % 0)) rs)]
    {:module m
     :java-files (:files conv 0)
     :converted (:converted conv 0)
     :convert-failures (count (:failures conv))
     :checked (count rs)
     :compiled (- (count rs) (count errors))
     :identical (count (filter :ok rs))
     :differing (count differing)
     :errors (count errors)
     :classes (reduce + (keep :classes rs))
     :nondet (reduce + (map (comp count :nondet) rs))
     :diff-kinds (frequencies (mapcat (fn [r] (distinct (map diff-kind (:diffs r)))) differing))
     :error-kinds (frequencies (map (comp error-kind :error) errors))
     :examples (into {} (for [r differing d (:diffs r)] [(diff-kind d) (str (:file r) " " (pr-str d))]))
     :error-examples (into {} (for [r errors] [(error-kind (:error r)) (str (:file r) " " (:error r))]))}))

(let [results (mapv module-result modules)
      total (reduce (fn [a r] (merge-with + a (select-keys r [:java-files :converted :convert-failures :checked :compiled :identical :differing :errors :classes :nondet])))
                    {} results)
      kinds (apply merge-with + (map :diff-kinds results))
      ekinds (apply merge-with + (map :error-kinds results))
      examples (apply merge (map :examples results))
      eexamples (apply merge (map :error-examples results))
      row (fn [name r] (format "| %s | %,d | %,d | %,d | %,d | %,d | %,d | %,d | %,d |"
                               name (:java-files r 0) (:converted r 0) (:compiled r 0) (:identical r 0)
                               (:differing r 0) (:errors r 0) (:classes r 0) (:nondet r 0)))
      md (str/join
          "\n"
          (concat
           ["# The converted JDK compiled with the class forms compiler"
            ""
            "Files: Java files converted; converted files with class forms compiled without error;"
            "files whose classes are all shape-identical to javac's; files with differences; compile"
            "errors. Classes: classes compiled from the forms; javac-nondeterministic ones not compared."
            ""
            "| module | Java files | converted | compiled | identical | differing | errors | classes | nondet. |"
            "|---|---:|---:|---:|---:|---:|---:|---:|---:|"]
           (map #(row (:module %) %) results)
           [(row "**all**" total)
            ""
            "## Differences by kind (files)"
            ""
            "| kind | files | example |"
            "|---|---:|---|"]
           (for [[k n] (sort-by (comp - val) kinds)]
             (format "| %s | %,d | `%s` |" k n (let [e (examples k)] (subs e 0 (min 300 (count e))))))
           [""
            "## Compile errors by kind (files)"
            ""
            "| kind | files | example |"
            "|---|---:|---|"]
           (for [[k n] (sort-by (comp - val) ekinds)]
             (format "| %s | %,d | `%s` |" (str/replace k "|" "\\|") n
                     (let [e (str/replace (eexamples k) "|" "\\|")] (subs e 0 (min 300 (count e))))))))]
  (spit (io/file work "report.md") (str md "\n"))
  (spit (io/file work "report.edn") (with-out-str (pp/pprint {:total total :modules (mapv #(dissoc % :examples :error-examples) results)
                                                             :diff-kinds kinds :error-kinds ekinds})))
  (println (row "all" total))
  (println "report:" (str (io/file work "report.md"))))
