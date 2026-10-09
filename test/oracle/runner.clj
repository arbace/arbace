(ns oracle.runner
  "The oracle's runner (doc/go/ORACLE.md), run by bin/oracle on the JVM Arbace. Each corpus
  file becomes one run of the implementation under test: a command reading forms on its
  standard input (the JVM's: bin/arbace -), given the driver test/oracle/driver.clj and then
  one driver call per case. The records it prints are normalized (normalize) and either written
  as the expected file (record) or compared with it (check)."
  (:require [arbace.string :as str]
            [arbace.java.io :as io]
            [arbace.walk :as walk])
  (:import (java.io File StringReader)
           (java.util.concurrent TimeUnit Executors Callable)
           (arbace.lang LineNumberingPushbackReader)))

(set! *warn-on-reflection* true)

(def root (System/getProperty "user.dir"))
(def oracle-dir (str root "/test/oracle"))
(def parts ["forms" "classes" "regex"])

(load-file (str oracle-dir "/driver.clj"))
(in-ns 'oracle.runner)

(def emit oracle.driver/emit)

;; ----- normalization: what varies between runs or implementations without meaning anything

(def ^:private normalizers
  [[#"(#object\[[^ \]]+ )0x[0-9a-f]+" "$10xN"]
   [#"([A-Za-z_$][\w$.]*;?)@[0-9a-f]{4,}\b" "$1@N"]
   [#"__\d+__auto__" "__N__auto__"]
   [#"\bG__\d+" "G__N"]
   [#"\$eval\d+" "\\$evalN"]
   [#"\beval\d+([/$])" "evalN$1"]
   [#"(\w)--\d+\b" "$1--N"]
   [#"(DynamicClassLoader )@[0-9a-f]+" "$1@N"]
   [#"\b(fn|reify|let|loop|p\d+|rest|x|y|seq|map|vec|deftype|cond|nth|first|ex|and|or)__\d+" "$1__N"]])

(defn normalize-str [^String s]
  (reduce (fn [s [re rep]] (str/replace s re rep)) s normalizers))

(defn normalize [x]
  (walk/postwalk (fn [y] (if (string? y) (normalize-str y) y)) x))

;; ----- helpful NullPointerException messages (V11, the user's decision of 2026-10-09)
;;
;; HotSpot describes the null of an implicit NullPointerException (JEP 358: "Cannot invoke
;; \"String.length()\" because \"s\" is null"); the Go build's implicit ones have a null
;; message. A chain entry of a NullPointerException whose message is such a description is
;; recorded with a third element, :helpful-npe, and check compares it by class only.

(def ^:private helpful-npe-re
  ;; the failed actions of HotSpot's NullPointerExceptions (bytecodeUtils.cpp,
  ;; print_NPE_failed_action), then the optional description of the null (print_NPE_cause)
  #"Cannot (invoke \"[^\"]+\"|read field \"[^\"]+\"|assign field \"[^\"]+\"|load from \w+ array|store to \w+ array|read the array length|throw exception|enter synchronized block|exit synchronized block)( because .+ is null)?")

(defn helpful-npe?
  "Whether the chain entry [class message] is a NullPointerException with a helpful message."
  [entry]
  (and (vector? entry) (= 2 (count entry))
       (= "java.lang.NullPointerException" (first entry))
       (string? (second entry))
       (boolean (re-matches helpful-npe-re (second entry)))))

(def ^:private chain-keys [:ex :print-ex :throws :error])

(defn mark-helpful-npes
  "The record with each helpful NullPointerException entry of its exception chains marked
  [class message :helpful-npe]."
  [r]
  (reduce (fn [r k]
            (if (vector? (get r k))
              (update r k (fn [chain] (mapv #(if (helpful-npe? %) (conj % :helpful-npe) %) chain)))
              r))
          r chain-keys))

(defn accept-helpful-npes
  "[a' n]: the actual record a with each chain entry replaced by the expected record e's where
  e's entry is a marked helpful NullPointerException and a's has the same class (compared by
  class only, whatever a's message), and n the number of entries so replaced. Chains of
  different lengths are left as they are."
  [e a]
  (reduce (fn [[a n] k]
            (let [ec (get e k) ac (get a k)]
              (if (and (vector? ec) (vector? ac) (= (count ec) (count ac)))
                (let [pairs (map (fn [x y]
                                   (if (and (= :helpful-npe (get x 2)) (vector? y) (= (first x) (first y)))
                                     [x (if (= x y) 0 1)] [y 0]))
                                 ec ac)
                      m (reduce + (map second pairs))]
                  (if (pos? m) [(assoc a k (mapv first pairs)) (+ n m)] [a n]))
                [a n])))
          [a 0] chain-keys))

;; ----- reading the sources

(defn- skip-blank
  "The text of a form as read+string captured it, without the blanks and comments before it,
  and the number of lines these took."
  [^String s]
  (loop [s s lines 0]
    (cond
      (str/blank? s) [s lines]
      (Character/isWhitespace (.charAt s 0))
      (recur (subs s 1) (if (= \newline (.charAt s 0)) (inc lines) lines))
      (= \, (.charAt s 0)) (recur (subs s 1) lines)
      (= \; (.charAt s 0)) (let [i (.indexOf s "\n")]
                             (if (neg? i) ["" lines] (recur (subs s (inc i)) (inc lines))))
      :else [s lines])))

(defn- rel [^File f] (subs (.getPath f) (inc (count root))))

(defn- skip-to-form
  "Consumes the blanks, commas and comments before the next form; its line."
  [^LineNumberingPushbackReader r]
  (loop []
    (let [c (.read r)]
      (cond
        (= c -1) (.getLineNumber r)
        (or (Character/isWhitespace (char c)) (= c (int \,))) (recur)
        (= c (int \;)) (do (loop [] (let [d (.read r)] (when-not (or (= d -1) (= d (int \newline))) (recur))))
                           (recur))
        :else (do (.unread r c) (.getLineNumber r))))))

(defn read-source
  "The top-level forms of a file: [{:line :text :form}], read with reader conditionals allowed.
  Forms are read in a scratch namespace so that ::kw does not fail; :form is used only by the
  class scripts and regex data."
  [^File f]
  (with-open [r (LineNumberingPushbackReader. (io/reader f))]
    (binding [*ns* (create-ns 'oracle.scratch)
              ;; ::alias/name with an alias made by an earlier case: any alias resolves
              *reader-resolver* (reify arbace.lang.LispReader$Resolver
                                  (currentNS [_] 'user)
                                  (resolveClass [_ s] s)
                                  (resolveAlias [_ s] s)
                                  (resolveVar [_ s] s))]
      (loop [acc []]
        (let [line (skip-to-form r)
              [form text] (try (read+string {:read-cond :allow :eof ::eof} r)
                               (catch Throwable t
                                 (throw (ex-info (str (rel f) ": unreadable form at line " line ": "
                                                      (.getMessage t)
                                                      " (invalid literals belong inside read-string)")
                                                 {} t))))]
          (if (= form ::eof)
            acc
            (recur (conj acc {:line line :text (str/trim (first (skip-blank text))) :form form}))))))))


(defn- source-files [part]
  (let [d (io/file oracle-dir part)]
    (->> (file-seq d)
         (filter #(and (.isFile ^File %) (.endsWith (.getName ^File %) ".clj")))
         (sort-by #(.getPath ^File %)))))

(defn- expected-file ^File [part ^File src]
  (let [base (io/file oracle-dir part)
        r (subs (.getPath src) (inc (count (.getPath base))))]
    (io/file oracle-dir "expected" part (str/replace r #"\.clj$" ".edn"))))

;; ----- class scripts: source forms to steps

(def ^:private class-packages ["arbace.lang." "java.lang." "java.util." "java.math." "java.util.regex." ""])

(def ^:private prim-names #{"int" "long" "double" "float" "boolean" "char" "short" "byte"})

(defn class-name
  "The full name of a class named in a script: as written when it loads, else in arbace.lang,
  java.lang, java.util, java.math, java.util.regex. Arrays as T[]."
  [n]
  (let [n (str n)]
    (cond
      (prim-names n) n
      (.endsWith n "[]") (str (class-name (subs n 0 (- (count n) 2))) "[]")
      :else (or (first (for [p (reverse class-packages)
                             :let [c (str p n)]
                             :when (try (Class/forName c false (.getContextClassLoader (Thread/currentThread)))
                                        (catch Throwable _ nil))]
                         c))
                (throw (IllegalArgumentException. (str "unknown class " n)))))))

(defn- arg-data [a]
  (cond
    (and (symbol? a) (namespace a)) {:static (class-name (namespace a)) :name (name a)}
    (symbol? a) {:ref (str a)}
    (and (seq? a) (prim-names (str (first a)))) {:box (str (first a)) :value (second a)}
    (and (seq? a) (= 'biginteger (first a))) {:biginteger (str (second a))}
    (and (seq? a) (= 'bigdecimal (first a))) {:bigdecimal (str (second a))}
    (and (seq? a) (= 'array (first a))) {:array (class-name (second a)) :items (mapv arg-data (nnext a))}
    (or (nil? a) (boolean? a) (string? a) (char? a) (instance? Long a) (instance? Double a)) a
    :else (throw (IllegalArgumentException. (str "bad argument " (pr-str a))))))

(defn- sig-of [form]
  (when-let [s (:sig (meta form))] (mapv class-name s)))

(defn step-of
  "The step (data, see oracle.driver/step) of a script form."
  [form]
  (let [[bind expr] (if (and (seq? form) (= 'def (first form))) [(str (second form)) (nth form 2)] [nil form])
        s (cond
            (symbol? expr)
            (do (when-not (namespace expr) (throw (IllegalArgumentException. (str "bad step " (pr-str form)))))
                {:op :get-static :class (class-name (namespace expr)) :name (name expr)})
            (not (seq? expr)) (throw (IllegalArgumentException. (str "bad step " (pr-str form))))
            (= 'new (first expr))
            {:op :new :class (class-name (second expr)) :args (mapv arg-data (nnext expr))}
            (.endsWith (str (first expr)) ".")
            {:op :new :class (class-name (let [n (str (first expr))] (subs n 0 (dec (count n)))))
             :args (mapv arg-data (next expr))}
            (.startsWith (str (first expr)) ".-")
            (cond-> {:op :get :name (subs (str (first expr)) 2) :target (str (second expr))}
              (:tag (meta (second expr))) (assoc :class (class-name (:tag (meta (second expr))))))
            (.startsWith (str (first expr)) ".")
            (cond-> {:op :invoke :name (subs (str (first expr)) 1) :target (str (second expr))
                     :args (mapv arg-data (nnext expr))}
              (:tag (meta (second expr))) (assoc :class (class-name (:tag (meta (second expr))))))
            (and (symbol? (first expr)) (namespace (first expr)))
            {:op :static :class (class-name (namespace (first expr))) :name (name (first expr))
             :args (mapv arg-data (next expr))}
            :else (throw (IllegalArgumentException. (str "bad step " (pr-str form)))))
        s (if-let [sig (sig-of expr)] (assoc s :sig sig) s)]
    (if bind (assoc s :bind bind) s)))

(defn- expand-each
  "(each [a [x y] b [z]] form...): the forms for every combination, a and b replaced."
  [form]
  (if (and (seq? form) (= 'each (first form)))
    (let [pairs (partition 2 (second form))
          combos (reduce (fn [acc [sym vals]] (for [m acc v vals] (assoc m sym v))) [{}] pairs)]
      (for [m combos f (nnext form)] (walk/postwalk-replace m f)))
    [form]))

;; ----- regex data to cases

(defn regex-cases
  "The regex cases of a source's data forms: maps with :pattern or :patterns, :inputs, and
  optionally :flags (one flag set) or :flag-sets (several), :replace."
  [forms]
  (for [{:keys [form]} forms
        :when (map? form)
        p (or (:patterns form) [(:pattern form)])
        fs (or (:flag-sets form) [(or (:flags form) [])])]
    (cond-> {:pattern p :flags (vec fs) :inputs (vec (:inputs form))}
      (seq (:replace form)) (assoc :replace (vec (:replace form))))))

;; ----- running an implementation

(def driver-text (delay (slurp (str oracle-dir "/driver.clj"))))

(defn- call-text [f & args]
  (str "(oracle.driver/" f (apply str (map #(str " " (emit %)) args)) ")\n"))

(defn run-impl
  "Runs the command (bash -c) with input on its standard input; the records it printed (read,
  normalized), the other lines of its standard output, its standard error and exit status."
  [cmd ^String input timeout-s]
  (let [pb (doto (ProcessBuilder. ^"[Ljava.lang.String;" (into-array String ["bash" "-c" cmd]))
             (.directory (io/file root)))
        p (.start pb)
        errf (future (slurp (.getErrorStream p) :encoding "UTF-8"))
        _ (future (try (with-open [w (io/writer (.getOutputStream p) :encoding "UTF-8")]
                         (.write w input))
                       (catch java.io.IOException _ nil)))
        outf (future (slurp (.getInputStream p) :encoding "UTF-8"))
        finished (.waitFor p (long timeout-s) TimeUnit/SECONDS)]
    (when-not finished (.destroyForcibly p) (.waitFor p))
    (let [lines (str/split-lines @outf)
          recs (keep #(when (.startsWith ^String % "@@oracle ")
                        (try (read-string (subs % 9))
                             (catch Throwable t {:unreadable % :why (.getMessage t)})))
                     lines)]
      {:records (vec recs)
       :noise (vec (remove #(.startsWith ^String % "@@oracle ") lines))
       :err @errf
       :exit (if finished (.exitValue p) :timeout)})))

;; ----- the parts: their cases from a source file or an expected file, and the input to send

(defn- source-cases [part ^File f]
  (let [forms (read-source f)]
    (case part
      "forms" (mapv (fn [i {:keys [line text]}] {:id (inc i) :line line :form text})
                    (range) forms)
      "classes" (vec (map-indexed (fn [i [text form]] (assoc (step-of form) :i (inc i) :src text))
                                  (mapcat (fn [{:keys [text form]}]
                                            (let [fs (expand-each form)]
                                              (if (= 1 (count fs)) [[text (first fs)]]
                                                  (map (fn [g] [(pr-str g) g]) fs))))
                                          forms)))
      "regex" (vec (map-indexed (fn [i c] (assoc c :i (inc i))) (regex-cases forms))))))

(def ^:private step-input-keys [:i :op :class :name :target :args :sig :bind])

(defn- input-text [part ^File src cases]
  (str @driver-text "\n"
       (apply str
              (for [c cases]
                (case part
                  "forms" (call-text "form" (:id c) (rel src) (:line c) (:form c))
                  "classes" (call-text "step" (select-keys c step-input-keys))
                  "regex" (call-text "regex" (select-keys c [:i :pattern :flags :inputs :replace])))))
       (call-text "done" (count cases))))

(def ^:private result-keys
  {"forms" [:value :class :out :err :ex :print-ex]
   "classes" [:class :sig :ret :result :throws :error]
   "regex" [:groups :named :results :error]})

(defn- case-key [part] (case part "forms" :id "classes" :i "regex" :i))

(defn merge-results
  "The cases with the results of the run, by case key; [:missing] for a case without one."
  [part cases recs]
  (let [k (case-key part)
        by (into {} (map (fn [r] [(get r k) r]) recs))]
    (mapv (fn [c]
            (if-let [r (by (get c k))]
              (merge c (mark-helpful-npes (select-keys (normalize r) (result-keys part))))
              (assoc c :missing true)))
          cases)))

(def ^:private key-order
  [:id :i :line :form :src :op :class :name :target :args :sig :bind :ret
   :pattern :flags :inputs :replace
   :value :out :err :ex :print-ex :result :throws :error :groups :named :results :missing])

(defn- emit-ordered [m]
  (let [ks (concat (filter #(contains? m %) key-order) (sort (remove (set key-order) (keys m))))
        sb (StringBuilder. "{")]
    (doseq [[i k] (map-indexed vector ks)]
      (when (pos? i) (.append sb " "))
      (oracle.driver/emit-to k sb) (.append sb " ") (oracle.driver/emit-to (get m k) sb))
    (str (.append sb "}"))))

(defn expected-text [part ^File src cases]
  (binding [oracle.driver/*ascii* false]
    (str ";; Written by bin/oracle record from " (rel src) " (doc/go/ORACLE.md); do not edit.\n"
         "{:part :" part " :source " (emit (rel src)) " :count " (count cases) "\n :cases\n ["
         (str/join "\n  " (map emit-ordered cases))
         "]}\n")))

(defn read-expected [^File f]
  (with-open [r (java.io.PushbackReader. (io/reader f :encoding "UTF-8"))]
    (binding [*read-eval* false] (read r))))

;; ----- record and check

(defn- pool-map [jobs f xs]
  (let [ex (Executors/newFixedThreadPool (int jobs))]
    (try
      (let [fs (mapv (fn [x] (.submit ex ^Callable (fn [] (f x)))) xs)]
        (mapv #(.get ^java.util.concurrent.Future %) fs))
      (finally (.shutdown ex)))))

(defn- run-file [cmd part ^File src cases timeout]
  (let [{:keys [records noise err exit]} (run-impl cmd (input-text part src cases) timeout)
        done (some :done records)
        bad (filter :unreadable records)]
    {:src src :part part
     :cases (merge-results part cases (remove :done records))
     :done done :noise noise :err err :exit exit :unreadable bad}))

(defn- selected [sel part ^File f]
  (or (empty? sel)
      (some (fn [^String s] (if (some #{s} parts) (= s part) (.contains ^String (rel f) s))) sel)))

;; With -XX:-OmitStackTraceInFastThrow: once C2 compiles a hot path, the JVM otherwise throws
;; preallocated exceptions without their messages (seen under load: NPE messages turned nil)
(def default-impl "env ARBACE_JAVA_OPTS=-XX:-OmitStackTraceInFastThrow bin/arbace -")

(defn record
  "Regenerates the expected files of the selected parts (or files) from the JVM Arbace."
  [jobs timeout sel]
  (let [work (for [part parts f (source-files part) :when (selected sel part f)] [part f])
        rs (pool-map jobs (fn [[part f]]
                            (let [t0 (System/nanoTime)
                                  r (run-file default-impl part f (source-cases part f) timeout)]
                              (assoc r :ms (quot (- (System/nanoTime) t0) 1000000))))
                     work)
        ok (atom true)]
    (doseq [{:keys [src part cases done noise err exit unreadable ms]} rs]
      (let [missing (count (filter :missing cases))
            errors (if (= part "classes") (count (filter :error cases)) 0)
            out (expected-file part src)]
        (when (or (not done) (pos? missing) (seq unreadable) (pos? errors))
          (reset! ok false)
          (println "FAILED" (rel src) ":" missing "missing," errors "script errors, exit" exit)
          (doseq [c (take 5 (filter :error cases))] (println "  " (:src c) (pr-str (:error c))))
          (when (seq noise) (println "  stdout:" (str/join "\n  " (take 10 noise))))
          (when-not (str/blank? err) (println "  stderr:" (subs err 0 (min 2000 (count err))))))
        (when (and done (zero? missing))
          (.mkdirs (.getParentFile out))
          (spit out (expected-text part src cases) :encoding "UTF-8")
          (println (format "%-46s %5d cases %6d ms" (rel out) (count cases) ms)))))
    @ok))

(defn- short-str [x n]
  (let [s (if (string? x) x (pr-str x))]
    (if (> (count s) n) (str (subs s 0 n) "...") s)))

(defn check
  "Runs the expected files of the selected parts (or files) against the implementation cmd;
  prints the mismatches (all of them to .tmp/oracle/check.txt) and a summary. True when every
  case matches."
  [cmd jobs timeout sel]
  (let [work (for [part parts
                   ^File f (->> (file-seq (io/file oracle-dir "expected" part))
                                (filter #(.endsWith (.getName ^File %) ".edn"))
                                (sort-by #(.getPath ^File %)))
                   :let [e (read-expected f)
                         src (io/file root (:source e))]
                   :when (selected sel part src)]
               [part src e])
        rs (pool-map jobs (fn [[part src e]]
                            (let [ks (result-keys part)
                                  cases (mapv #(apply dissoc % :missing (remove #{:class :sig} ks)) (:cases e))
                                  cases (if (= part "forms") (mapv #(dissoc % :class) cases) cases)
                                  t0 (System/nanoTime)
                                  r (run-file cmd part src cases timeout)]
                              (assoc r :expected (:cases e) :ms (quot (- (System/nanoTime) t0) 1000000))))
                     work)
        report (StringBuilder.)
        total (atom 0) bad (atom 0) incomplete (atom 0) v11 (atom 0)]
    (doseq [{:keys [src part cases expected done noise err exit ms]} rs]
      (let [ks (conj (result-keys part) :missing)
            ;; compared as printed, so that -0.0 differs from 0.0 and ##NaN equals itself
            canon (fn [x] (emit (walk/postwalk #(if (map? %) (into (sorted-map) %) %) x)))
            accepted (atom 0)
            mism (doall
                   (keep (fn [[e a]]
                           (let [[a n] (accept-helpful-npes e a)]
                             (if (not= (canon (select-keys e ks)) (canon (select-keys a ks)))
                               [e a]
                               (do (when (pos? n) (swap! accepted inc)) nil))))
                         (map vector expected cases)))
            line (format "%-40s %5d/%5d ok %6d ms%s" (rel src) (- (count expected) (count mism)) (count expected) ms
                         (str (when (pos? @accepted) (str ", " @accepted " by V11"))
                              (when-not done (str ", incomplete (exit " exit ")"))
                              (when (seq noise) (str ", " (count noise) " other stdout lines"))))]
        (swap! total + (count expected)) (swap! bad + (count mism)) (swap! v11 + @accepted)
        (when-not done (swap! incomplete inc))
        (println line)
        (.append report (str "== " line "\n"))
        (when (and (not done) (not (str/blank? err)))
          (.append report (str "stderr: " (short-str err 4000) "\n")))
        (doseq [[[e a] i] (map vector mism (range))]
          (let [what (str "  " (rel src) ":" (or (:line e) (:i e)) " " (short-str (or (:form e) (:src e) (:pattern e)) 200) "\n"
                          (str/join (for [k ks :when (not= (get e k) (get a k))]
                                      (str "    " k "\n      expected " (short-str (get e k) 600)
                                           "\n      actual   " (short-str (get a k) 600) "\n"))))]
            (.append report what)
            (when (< i 5) (print what))))))
    (.mkdirs (io/file root ".tmp/oracle"))
    (spit (io/file root ".tmp/oracle/check.txt") (str report))
    (println (format "== %d of %d cases match (%d by V11: helpful NullPointerException messages compared by class only), %d mismatches (all in .tmp/oracle/check.txt)%s"
                     (- @total @bad) @total @v11 @bad
                     (if (pos? @incomplete) (str "; " @incomplete " runs incomplete") "")))
    (and (zero? @bad) (zero? @incomplete))))

(defn -main [& args]
  (let [[cmd & args] args
        opts (loop [args args o {:jobs 16 :timeout 300 :sel []}]
               (cond
                 (empty? args) o
                 (= "-j" (first args)) (recur (nnext args) (assoc o :jobs (Long/parseLong (second args))))
                 (= "--timeout" (first args)) (recur (nnext args) (assoc o :timeout (Long/parseLong (second args))))
                 :else (recur (next args) (update o :sel conj (first args)))))
        ok (case cmd
             "record" (record (:jobs opts) (:timeout opts) (:sel opts))
             "check" (let [[impl & sel] (:sel opts)
                           impl (if (= impl "jvm") default-impl impl)]
                       (check impl (:jobs opts) (:timeout opts) (vec sel))))]
    (shutdown-agents)
    (System/exit (if ok 0 1))))
