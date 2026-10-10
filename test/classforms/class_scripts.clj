;; The oracle's class scripts (test/oracle/classes/, doc/go/ORACLE.md) run on classes defined at
;; the REPL by defclass rather than precompiled (doc/go/CLASSFORMS-REPL.md): the class forms of
;; the runtime's classes a script exercises (arbace/lang/C.clj) are evaluated in the namespace
;; cf.lang, so that they define the classes cf.lang.C (interpreted in the Go build, compiled on
;; the JVM), and the script's steps call them in place of arbace.lang.C; the results, with
;; cf.lang. read as arbace.lang., are compared with the script's expected file.
;;
;; Usage: bin/arbace test/classforms/class_scripts.clj IMPL [SCRIPT...]
;;   IMPL    a command reading forms on its standard input, as for bin/oracle check (jvm:
;;           bin/arbace -)
;;   SCRIPT  scripts by name (Murmur3); default: all of `scripts`
(ns classforms.class-scripts
  (:require [arbace.string :as str]
            [arbace.java.io :as io]
            [arbace.walk :as walk]))

(load-file "test/oracle/runner.clj")
(in-ns 'classforms.class-scripts)

(def scripts
  "A script and the runtime's classes (their files in arbace/lang) it is run on, defined at the
  REPL: classes whose class forms use only public members of the rest of the runtime."
  {"Murmur3" ["Murmur3"]})

;; Not feasible so: scripts whose classes the driver or the runtime recognizes by identity (Seqs:
;; the driver does not print lazy seqs, by class, so cf.lang.Repeat is printed forever; Names: the
;; runtime's printer and keyword functions know arbace.lang.Keyword, not cf.lang.Keyword), and
;; those whose classes extend classes of the world without a DynSub type (RatioBigInt: Number).

(defn- lang-classes
  "The simple names of the classes of arbace.lang (top-level), from arbace/lang.clj."
  []
  (for [[_ n] (re-seq #"\(load \"lang/([A-Za-z0-9_]+)\"\)" (slurp "arbace/lang.clj"))] n))

(defn- source-forms [file]
  (let [r (arbace.lang.LineNumberingPushbackReader. (java.io.FileReader. ^String file))]
    (binding [*read-eval* false]
      (doall (take-while #(not= % ::eof) (repeatedly #(read {:eof ::eof :read-cond :allow} r)))))))

(defn preamble
  "The forms defining classes cs (simple names) in cf.lang, as text: the namespace with the
  runtime's other classes and the sources' imports, then the sources' class forms in one do (they
  refer to each other, SPEC §9.2)."
  [cs]
  (let [own (set cs)
        imports (for [n (lang-classes) :when (not (own n))] n)
        forms (mapcat #(oracle.runner/read-source (io/file (str "arbace/lang/" % ".clj"))) cs)
        head (fn [{:keys [form]}] (when (seq? form) (first form)))]
    (str "(ns cf.lang)\n"
         ;; each class the program has (the Go build's world lacks some of the JVM's)
         "(doseq [c '" (pr-str (vec (map symbol imports))) "]\n"
         "  (try (.importClass *ns* (Class/forName (str \"arbace.lang.\" c))) (catch Throwable _ nil)))\n"
         (str/join "\n" (for [f forms :when (= 'import (head f))] (:text f)))
         "\n(do\n"
         (str/join "\n" (for [f forms :when (not (#{'import 'in-ns} (head f)))] (:text f)))
         ")\n(in-ns 'user)\n")))

(defn- renamed [cs ^String s]
  (reduce (fn [s c] (str/replace s (str "arbace.lang." c) (str "cf.lang." c))) s cs))

(defn- unrenamed [x]
  (walk/postwalk #(if (string? %) (str/replace % "cf.lang." "arbace.lang.") %) x))

(defn run-script [impl script cs]
  (let [src (io/file "test/oracle/classes" (str script ".clj"))
        e (oracle.runner/read-expected (io/file "test/oracle/expected/classes" (str script ".edn")))
        ks [:class :sig :ret :result :throws :error]
        cases (mapv #(apply dissoc % :missing (remove #{:class :sig} ks)) (:cases e))
        step-keys [:i :op :class :name :target :args :sig :bind]
        input (str @oracle.runner/driver-text "\n"
                   (preamble cs)
                   (apply str (for [c cases]
                                (str "(oracle.driver/step "
                                     (renamed cs (oracle.driver/emit (select-keys c step-keys))) ")\n")))
                   "(oracle.driver/done " (count cases) ")\n")
        cmd (if (= impl "jvm") "bin/arbace -" impl)
        t0 (System/nanoTime)
        {:keys [records noise err exit]} (oracle.runner/run-impl cmd input (Long/parseLong (or (System/getenv "CF_TIMEOUT") "900")))
        done (some :done records)
        got (oracle.runner/merge-results "classes" cases (unrenamed (remove :done records)))
        canon (fn [x] (oracle.driver/emit (walk/postwalk #(if (map? %) (into (sorted-map) %) %) x)))
        ks (conj ks :missing)
        mism (keep (fn [[x a]]
                     (let [[a _] (oracle.runner/accept-helpful-npes x a)
                           a (if (:class x) a (dissoc a :class))]
                       (when (not= (canon (select-keys x ks)) (canon (select-keys a ks))) [x a])))
                   (map vector (:cases e) got))]
    (println (format "%-12s %5d/%5d ok %6d ms%s" script (- (count cases) (count mism)) (count cases)
                     (quot (- (System/nanoTime) t0) 1000000)
                     (if done "" (str ", incomplete (exit " exit ")"))))
    (when-not done (println (subs err 0 (min 3000 (count err)))))
    (doseq [[x a] (take 10 mism)]
      (println "  " (:i x) (:src x))
      (doseq [k ks :when (not= (get x k) (get a k))]
        (println "    " k "expected" (pr-str (get x k)))
        (println "    " k "actual  " (pr-str (get a k)))))
    {:script script :ok (- (count cases) (count mism)) :total (count cases) :done (boolean done)}))

(let [[impl & sel] *command-line-args*]
  (when-not impl
    (println "Usage: bin/arbace test/classforms/class_scripts.clj IMPL [SCRIPT...]")
    (System/exit 2))
  (let [rs (doall (for [s (or (seq sel) (sort (keys scripts)))]
                    (run-script impl s (get scripts s))))]
    (shutdown-agents)
    (System/exit (if (every? #(and (:done %) (= (:ok %) (:total %))) rs) 0 1))))
