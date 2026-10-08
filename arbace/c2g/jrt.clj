(ns arbace.c2g.jrt
  "What c2g knows of jrt's hand-written Go forms (go/arbace/jrt, doc/go/JRT-NOTES.md): the Go
  types, methods, functions and variables they declare, the Java classes they define (by their
  Define registrations) and which of those are stand-ins (C2G-SPEC §4.3, amendment J9), read
  from the forms themselves so that c2g's output and jrt meet at the names jrt really has."
  (:require [arbace.string :as str]
            [arbace.set]
            [arbace.java.io :as io]
            [arbace.g2c.print :as gp]))

(defn- head [f] (when (and (seq? f) (symbol? (first f))) (name (first f))))

(defn- go-head [f]
  (when (and (seq? f) (symbol? (first f)) (#{"go" "arbace.go"} (namespace (first f))))
    (name (first f))))

(defn- tag-of [x] (:tag (meta x)))

(defn- recv-type
  "The receiver type name of a go/method's receiver parameter."
  [r]
  (let [t (if (symbol? r) (tag-of r) r)]
    (letfn [(nm [x] (cond (symbol? x) (name x)
                          (and (seq? x) (= '* (first x))) (nm (second x))
                          (and (seq? x) (symbol? (first x))) (name (first x))
                          :else nil))]
      (nm t))))

(defn- strip-doc [xs] (if (string? (first xs)) (rest xs) xs))

(defn- define-name
  "The Java name a (Define (addr (lit ClassInfo :Name \"...\" ...))) registers, or nil."
  [init]
  (let [found (atom nil)]
    (letfn [(walk [x]
              (cond
                (and (seq? x) (= "lit" (head x)) (= 'ClassInfo (second x)))
                (let [kvs (apply hash-map (drop 2 x))]
                  (when (string? (:Name kvs)) (reset! found {:name (:Name kvs) :info kvs})))
                (seq? x) (run! walk x)
                :else nil))]
      (walk init))
    @found))

(defn scan-file
  "Declarations of one jrt forms file: {:types {name {:kind :struct|:interface|:other :form f}}
  :methods {type #{names}} :funcs #{} :vars #{} :classes {java-name var-name}}."
  [f]
  (let [forms (gp/read-forms f)
        acc (volatile! {:types {} :methods {} :funcs #{} :vars #{} :classes {} :consts #{}})]
    (doseq [fm forms
            :let [h (go-head fm)]
            :when h]
      (case h
        "type" (let [[_ & more] fm
                     more (strip-doc more)]
                 (if (vector? (first more))
                   (doseq [[nm tf] more]
                     (vswap! acc assoc-in [:types (name nm)]
                             {:kind (cond (and (seq? tf) (= 'struct (first tf))) :struct
                                          (and (seq? tf) (= 'interface (first tf))) :interface
                                          :else :other)
                              :form tf}))
                   (let [nm (first more)
                         more (strip-doc (rest more))
                         more (if (= :type-params (first more)) (drop 2 more) more)
                         tf (first more)]
                     (vswap! acc assoc-in [:types (name nm)]
                             {:kind (cond (and (seq? tf) (= 'struct (first tf))) :struct
                                          (and (seq? tf) (= 'interface (first tf))) :interface
                                          :else :other)
                              :form tf}))))
        "method" (let [[_ nm & more] fm
                       more (strip-doc more)
                       more (if (= :type-params (first more)) (drop 2 more) more)
                       params (first more)
                       rt (recv-type (first params))]
                   (when rt (vswap! acc update-in [:methods rt] (fnil conj #{}) (name nm))))
        "func" (let [[_ nm] fm] (vswap! acc update :funcs conj (name nm)))
        "var" (let [[_ & more] fm
                    more (strip-doc more)]
                (if (vector? (first more))
                  (doseq [[tg init] more]
                    (if (and (seq? tg) (= 'values (first tg)))
                      (doseq [s (rest tg)] (vswap! acc update :vars conj (name s)))
                      (do (vswap! acc update :vars conj (name tg))
                          (when-let [{n :name} (define-name init)]
                            (vswap! acc assoc-in [:classes n] (name tg))))))
                  (let [[tg init] more]
                    (if (and (seq? tg) (= 'values (first tg)))
                      (doseq [s (rest tg)] (vswap! acc update :vars conj (name s)))
                      (do (vswap! acc update :vars conj (name tg))
                          (when-let [{n :name} (define-name init)]
                            (vswap! acc assoc-in [:classes n] (name tg))))))))
        "const" (let [[_ & more] fm
                      more (strip-doc more)]
                  (if (vector? (first more))
                    (doseq [[tg] more] (when (symbol? tg) (vswap! acc update :consts conj (name tg))))
                    (when (symbol? (first more)) (vswap! acc update :consts conj (name (first more))))))
        nil))
    @acc))

(defn- interface-elems
  "Method names and embedded interface names of an interface type form."
  [tf]
  (let [elems (rest tf)]
    {:methods (set (for [e elems :when (and (seq? e) (symbol? (first e)) (vector? (second (remove string? e))))]
                     (name (first e))))
     :embeds (vec (for [e elems :when (symbol? e)] (name e)))}))

(defn- struct-embed
  "The embedded type name of a struct's first field, when it embeds one."
  [tf]
  (let [f (second tf)]
    (when (and f (symbol? f) (nil? (tag-of f))) (name f))))

(defn scan
  "Scans jrt's hand-written forms directory (go/arbace/jrt, test files excluded). Returns
  {:files {file-name scan} :types :methods :funcs :vars :classes {java-name {:var :go :file
  :standin}} :interfaces {name {:methods :embeds}} :embeds {struct first-embedded}}."
  [dir]
  (let [files (->> (.listFiles (io/file dir))
                   (filter #(and (str/ends-with? (.getName ^java.io.File %) ".clj")
                                 (not (str/ends-with? (.getName ^java.io.File %) "_test.clj"))))
                   (sort-by #(.getName ^java.io.File %)))
        scans (into (sorted-map) (for [f files] [(.getName ^java.io.File f) (scan-file f)]))
        types (apply merge (map :types (vals scans)))
        methods (apply merge-with into (map :methods (vals scans)))]
    {:files scans
     :types types
     :methods methods
     :funcs (apply arbace.set/union (map :funcs (vals scans)))
     :vars (apply arbace.set/union (map :vars (vals scans)))
     :consts (apply arbace.set/union (map :consts (vals scans)))
     :classes (into {} (for [[fname s] scans
                             [jn vn] (:classes s)]
                         [jn {:var vn :go (str/replace vn #"_class$" "") :file fname
                              :standin (str/starts-with? fname "standin_")}]))
     :interfaces (into {} (for [[n {:keys [kind form]}] types :when (= kind :interface)]
                            [n (interface-elems form)]))
     :embeds (into {} (for [[n {:keys [kind form]}] types :when (= kind :struct)
                            :let [e (struct-embed form)] :when e]
                        [n e]))}))

(defn struct-methods
  "The method names a struct type has, its embedded ancestors' included (Go's promotion)."
  [scan go-type]
  (loop [t go-type acc #{}]
    (if (nil? t)
      acc
      (recur (get (:embeds scan) t) (into acc (get (:methods scan) t))))))

(defn iface-methods
  "The method names of an interface type, embedded interfaces included."
  [scan go-type]
  (let [i (get (:interfaces scan) go-type)]
    (into (set (:methods i)) (mapcat #(iface-methods scan %) (:embeds i)))))
