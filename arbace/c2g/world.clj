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

;; ---------------------------------------------------------------------------------------
;; Go-build variants (C2G-SPEC §4.6): (c2g/variant Class member... (c2g/cut head) (c2g/add
;; member)) in files read by c2g only (arbace/lang/go/*.clj)

(defn- head-name [f] (when (and (seq? f) (symbol? (first f))) (name (first f))))

(defn- param-key
  "A method's or constructor's identity in a variant: kind, name, static, parameter tags."
  [f]
  (let [h (head-name f)]
    (case h
      ("method" "c2g-cut-method")
      (let [[_ nm & more] f
            params (first (filter vector? more))
            static (boolean (:static (meta nm)))
            ps (if static params (rest params))]
        [:method (name nm) static (mapv #(some-> (:tag (meta %)) str) ps)])
      "constructor"
      (let [params (first (filter vector? (rest f)))]
        [:ctor (mapv #(some-> (:tag (meta %)) str) (rest params))])
      "field" (let [nm (first (filter symbol? (rest f)))] [:field (name nm)])
      "static-initializer" [:static-initializer (or (:c2g/nth (meta f)) 0)]
      "defclass" [:class (name (second f))]
      nil)))

(defn- cut-key [x]
  (cond
    (and (seq? x) (= "field" (head-name x))) (param-key x)
    ;; (c2g/cut (static-initializer n)): the class's n-th static initializer
    (and (seq? x) (= "static-initializer" (head-name x))) [:static-initializer (or (second x) 0)]
    (symbol? (first x)) (param-key (cons 'c2g-cut-method x))
    :else (throw (ex-info (str "c2g: bad c2g/cut " (pr-str x)) {}))))

(defn- class-parts
  "[head members] of a defclass form: head is defclass, name, components, options."
  [cf]
  (let [[dc nm & more] cf
        [comps more] (if (vector? (first more)) [[(first more)] (rest more)] [[] more])
        [opts members] (p/split-options more)]
    [(concat [dc nm] comps (mapcat identity opts)) (vec members)]))

(defn apply-variant
  "Class form cf with the variant's members applied: replaced, cut, added. Returns [cf'
  counts]. With variants-of (a function of a nested class's simple name, giving its variant
  members or nil) and applied (an atom of {name counts}), the variants of nested classes
  (c2g/variant Outer$Inner ...) are applied to the nested class forms too, recursively, and
  recorded in applied under their names (proposed amendment B1)."
  ([cf vforms] (apply-variant cf vforms nil nil nil))
  ([cf vforms prefix variants-of applied]
  (let [[head members] (class-parts cf)
        ;; static initializers are told apart by their position in the class form
        members (let [n (atom -1)]
                  (mapv (fn [m] (if (= "static-initializer" (head-name m))
                                  (vary-meta m assoc :c2g/nth (swap! n inc))
                                  m))
                        members))
        counts (atom {:replaced 0 :cut 0 :added 0})
        members (reduce
                  (fn [ms vf]
                    (case (head-name vf)
                      ;; (c2g/cut (field ...)), (c2g/cut (static-initializer n)), or a
                      ;; method's head spliced: (c2g/cut ^:static name ^Ret [params])
                      "cut" (let [k (cut-key (if (symbol? (second vf)) (rest vf) (second vf)))
                                  hit (some #(= k (param-key %)) ms)]
                              (when-not hit (throw (ex-info (str "c2g: variant cuts no member: " (pr-str (second vf))) {})))
                              (swap! counts update :cut inc)
                              (vec (remove #(= k (param-key %)) ms)))
                      "add" (do (swap! counts update :added inc) (conj ms (second vf)))
                      (let [k (param-key vf)
                            i (first (keep-indexed (fn [i m] (when (= k (param-key m)) i)) ms))]
                        (when-not i (throw (ex-info (str "c2g: variant replaces no member: " (pr-str k)) {})))
                        (swap! counts update :replaced inc)
                        (assoc ms i vf))))
                  members vforms)
        ;; nested classes with variants of their own
        members (if variants-of
                  (mapv (fn [m]
                          (if (= "defclass" (head-name m))
                            (let [nn (str prefix "$" (name (second m)))
                                  vfs (variants-of nn)
                                  [m2 c] (apply-variant m (or vfs []) nn variants-of applied)]
                              (when vfs (swap! applied assoc nn c))
                              m2)
                            m))
                        members)
                  members)]
    [(with-meta (apply list (concat head members)) (meta cf)) @counts])))

(defn read-variants
  "{internal-name [member-form ...]} of the variant files; a nested class's variant is named
  by its binary name, (c2g/variant Compiler$ObjExpr ...). The prefixes of the packages a
  variant file erases, (c2g/erase \"arbace/asm/\"), are under the key ::erase (proposed
  amendment B2)."
  [files]
  (reduce (fn [acc f]
            (let [forms (read-forms f)
                  nsname (ns-of-forms forms)]
              (reduce (fn [acc fm]
                        (cond
                          (and (seq? fm) (= "variant" (head-name fm)))
                          (let [[_ cls & members] fm
                                n (str (str/replace (munge (name nsname)) "." "/") "/" (name cls))]
                            (update acc n (fnil into []) members))
                          (and (seq? fm) (= "erase" (head-name fm)))
                          (update acc ::erase (fnil into #{}) (map str (rest fm)))
                          :else acc))
                      acc forms)))
          {} files))

(defn- new-unit []
  {:order (atom []) :counters (atom {}) :switch-maps (atom {}) :switch-holders (atom {})
   :source-tried (atom #{}) :assert-holders (atom {}) :holder-first (atom {})})

(defn load-world
  "Reads, enters and analyzes the class forms of the inputs, each {:root :dirs|:tree|:files},
  with the Go-build variants of the variant files applied. Returns the world:
  {:compile-set atom :unit unit :from-source pred :files [...] :tops #{} :failed [[class
  msg]...] :variants {class counts}}. Run translation inside (with-world w ...)."
  [inputs & {:keys [variant-files]}]
  (let [variants (read-variants variant-files)
        erased (get variants ::erase #{})
        variants (dissoc variants ::erase)
        applied (atom {})
        files (vec (mapcat source-files inputs))
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
                (let [n (first (top-names ns [cf]))
                      vfs (get variants n)
                      [cf counts] (apply-variant cf (or vfs []) n #(get variants %) applied)
                      _ (when vfs (swap! applied assoc n counts))
                      pc (p/parse-class {:ns nsobj :nesting :top} (rest cf))]
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
    (doseq [n (keys variants) :when (not (contains? @applied n))]
      (swap! failed conj [n "a variant for a class not in the inputs"]))
    {:compile-set cs :unit unit :from-source from-source :files files :tops tops
     :failed @failed :variants @applied :erased erased}))

(defmacro with-world
  "Runs body with the world's class environment bound (analyzer and env functions see it)."
  [w & body]
  `(let [w# ~w]
     (binding [env/*compile-set* (:compile-set w#)
               a/*unit* (:unit w#)
               env/*from-source* (:from-source w#)]
       ~@body)))
