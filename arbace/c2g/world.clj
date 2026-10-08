(ns arbace.c2g.world
  "c2g's input (C2G-SPEC §4.1): class forms files (Arbace's arbace/lang/*.clj, j2c's conversion
  of the JDK closure under .tmp/jrt/conv) entered into one class environment and analyzed with
  the class forms compiler's front end exactly as arbace.classes.compiler does before emitting
  (resolve-header!, resolve-members!, add-bridges!, analyze-class!). Classes taken from source
  are never resolved by reflection on the running JDK (env/*from-source*)."
  (:require [arbace.string :as str]
            [arbace.java.io :as io]
            [arbace.classes.types :as t]
            [arbace.classes.env :as env]
            [arbace.classes.parse :as p]
            [arbace.classes.analyze :as a])
  (:import (arbace.lang LineNumberingPushbackReader)))

(defn read-forms [f]
  (with-open [r (LineNumberingPushbackReader. (io/reader f))]
    (binding [*read-eval* false]
      (doall (take-while #(not= % ::eof) (repeatedly #(read {:eof ::eof :read-cond :allow} r)))))))

(defn- class-form? [f] (and (seq? f) (symbol? (first f)) (= "defclass" (name (first f)))))

(defn- class-forms [forms]
  (for [f forms
        f (if (and (seq? f) (= 'do (first f))) (rest f) [f])
        :when (class-form? f)]
    f))

(defn- ns-of-forms [forms]
  (let [f (first (filter #(and (seq? %) (symbol? (first %)) (= "in-ns" (name (first %)))) forms))]
    (when f (let [n (second f)] (if (seq? n) (second n) n)))))

(defn- top-names
  "The internal names of the top-level classes of a file's class forms."
  [nsname forms]
  (for [cf (class-forms forms)
        :let [[nm & more] (rest cf)
              more (if (vector? (first more)) (rest more) more)
              pkg (:package (first (p/split-options more)))
              s (str nm)]]
    (cond (some? pkg) (str (when (seq (str pkg)) (str (str/replace (str pkg) "." "/") "/")) s)
          (str/includes? s ".") (str/replace s "." "/")
          :else (str (str/replace (munge (name nsname)) "." "/") "/" s))))

(defn source-files
  "The class forms files of an input: {:root dir :dirs [subdir...]} or {:root dir :tree true}
  (every .clj under root but package files holding ns forms)."
  [{:keys [root dirs tree files]}]
  (cond
    files (map io/file files)
    tree (->> (file-seq (io/file root))
              (filter #(and (.isFile ^java.io.File %) (str/ends-with? (.getName ^java.io.File %) ".clj")))
              (remove #(some (fn [f] (and (seq? f) (= 'ns (first f)))) (read-forms %)))
              (sort-by str))
    :else (->> (for [d dirs ^java.io.File f (.listFiles (io/file root d))
                     :when (and (.isFile f) (str/ends-with? (.getName f) ".clj"))]
                 f)
               (sort-by str))))

(defn- new-unit []
  {:order (atom []) :counters (atom {}) :switch-maps (atom {}) :switch-holders (atom {})
   :source-tried (atom #{}) :assert-holders (atom {}) :holder-first (atom {})})

(defn load-world
  "Reads, enters and analyzes the class forms of the inputs, each {:root :dirs|:tree|:files}.
  Returns the world: {:compile-set atom :unit unit :from-source pred :files [...] :tops #{}
  :failed [[class msg]...]}. Run translation inside (with-world w ...)."
  [inputs]
  (let [files (vec (mapcat source-files inputs))
        parsed (vec (for [f files :let [forms (read-forms f)]]
                      {:file f :forms forms :ns (ns-of-forms forms)}))
        tops (set (mapcat (fn [{:keys [ns forms]}] (when ns (top-names ns forms))) parsed))
        top-of (fn [^String n] (let [i (.indexOf n "$")] (if (neg? i) n (subs n 0 i))))
        from-source (fn [n] (contains? tops (top-of n)))
        cs (atom {})
        unit (new-unit)
        failed (atom [])]
    (binding [env/*compile-set* cs
              a/*unit* unit
              env/*from-source* from-source]
      (doseq [{:keys [file forms ns]} parsed :when ns]
        (binding [*ns* *ns*]
          (doseq [fm forms
                  :when (and (seq? fm) (symbol? (first fm)) (#{"in-ns" "import"} (name (first fm))))]
            (a/eval-ns-form! fm))
          (let [nsobj *ns*]
            (doseq [cf (class-forms forms)]
              (try
                (let [pc (p/parse-class {:ns nsobj :nesting :top} (rest cf))]
                  (when-not (a/decl (a/top-name nsobj pc))
                    (a/declare-class! {:nesting :top} pc)))
                (catch Throwable e
                  (swap! failed conj [(str file) (str "parse: " (.getMessage e))])))))))
      ;; process-classes! 0, with failures per class recorded rather than fatal
      (let [names @(:order unit)]
        (doseq [n names :when (not (:headers-done (a/decl n)))]
          (try (a/resolve-header! n) (catch Throwable e (swap! failed conj [n (str "header: " (.getMessage e))]))))
        (doseq [n names]
          (try (a/resolve-members! n) (catch Throwable e (swap! failed conj [n (str "members: " (.getMessage e))]))))
        (doseq [n (#'a/supertypes-first names)]
          (try (a/add-bridges! n) (catch Throwable e (swap! failed conj [n (str "bridges: " (.getMessage e))]))))
        (doseq [n names]
          (try (#'a/mark-enum-abstract! n) (catch Throwable e (swap! failed conj [n (str "enum: " (.getMessage e))]))))
        (doseq [n names]
          (try (a/analyze-class! n)
               (catch Throwable e
                 (swap! (:state (a/decl n)) assoc :analysis-failed (.getMessage e))
                 (swap! failed conj [n (str "analysis: " (.getMessage e)
                                            (some->> (.getCause e) .getMessage (str " / ")))]))))))
    {:compile-set cs :unit unit :from-source from-source :files files :tops tops
     :failed @failed}))

(defmacro with-world
  "Runs body with the world's class environment bound (analyzer and env functions see it)."
  [w & body]
  `(let [w# ~w]
     (binding [env/*compile-set* (:compile-set w#)
               a/*unit* (:unit w#)
               env/*from-source* (:from-source w#)]
       ~@body)))
