;; Driver of the class forms compiler (doc/classes/SPEC.md §9.6), at every stage.
;;
;; The compiler's sources name the runtime they run on as Arbace's: `arbace.core`,
;; `arbace.string`, `arbace.lang.RT`, `arbace.asm.ClassWriter`. From stage 1 on that is the
;; running runtime, and this driver just requires the compiler's namespaces; `arbace.core`
;; itself holds the class forms' names (arbace/core_classes.clj) and `arbace.lang.Compiler` knows
;; their special forms. At stage 0 only the frozen baseline exists, `clojure.core`,
;; `clojure.lang` and `clojure.asm`: the driver then loads the compiler's namespaces itself,
;; rewriting every symbol that names a vendored namespace or package, `arbace.X` (also inside
;; metadata, such as type hints), to `clojure.X` while it reads them. The tools' own namespaces
;; (`arbace.classes`, `arbace.j2c`, `arbace.javalisp`) are not rewritten. Then it loads
;; arbace/core_classes.clj the same way, so the class forms' names (`defclass`, `switch`, ...)
;; are interned into `clojure.core` (at run time; the frozen sources are not touched), and
;; refers them into `user`, so class forms can be written without qualifying the new names.
;;
;; This file itself is read unmapped at every stage, so it names no class or namespace of either
;; runtime: only core functions, Java classes and names computed from the running core.
;;
;; Use: (require 'arbace.classes.boot), or (load-file "arbace/classes/boot.clj").
(ns arbace.classes.boot)

(def core-ns
  "The name of the running core namespace: clojure.core at stage 0, arbace.core after."
  (symbol (namespace `ns)))

(def stage0?
  "Running on the frozen clojure/ (stage 0)?"
  (= 'clojure.core core-ns))

(def ^:private namespaces
  '[arbace.classes.types
    arbace.classes.env
    arbace.classes.parse
    arbace.classes.lower
    arbace.classes.analyze
    arbace.classes.emit
    arbace.classes.compiler
    arbace.classes.native
    arbace.classes.shape
    arbace.classes.build])

(def ^:private own
  "Prefixes of the tools' namespaces, which keep their names at stage 0."
  ["arbace.classes" "arbace.j2c" "arbace.javalisp"])

(defn- map-name [^String s]
  (if (and (.startsWith s "arbace.")
           (not (some #(or (= s %) (.startsWith s (str % "."))) own)))
    (str "clojure." (subs s 7))
    s))

(defn- map-sym [s]
  (let [n (name s) ns (namespace s)]
    (cond
      ns (let [ns2 (map-name ns)] (if (= ns ns2) s (with-meta (symbol ns2 n) (meta s))))
      :else (let [n2 (map-name n)] (if (= n n2) s (with-meta (symbol n2) (meta s)))))))

(defn- runtime-class
  "Class `simple` of the running runtime's lang package."
  [simple]
  (Class/forName (str (if stage0? "clojure" "arbace") ".lang." simple)))

(def ^:private iobj (runtime-class "IObj"))

(defn map-form
  "Form f with arbace.X names read as clojure.X (stage 0)."
  [f]
  (let [m (meta f)
        f2 (cond
             (symbol? f) (map-sym f)
             (seq? f) (apply list (map map-form f))
             (map-entry? f) (vec (map map-form f))
             (vector? f) (vec (map map-form f))
             (map? f) (into (empty f) (map (fn [[k v]] [(map-form k) (map-form v)]) f))
             (set? f) (into (empty f) (map map-form f))
             :else f)]
    (if (and m (instance? iobj f2))
      (with-meta f2 (map-form m))
      f2)))

(defn- resource [^String path]
  (or (some-> (Thread/currentThread) .getContextClassLoader (.getResource path))
      (ClassLoader/getSystemResource path)
      (throw (java.io.FileNotFoundException. path))))

(defn load-mapped
  "Loads the Clojure source resource `path` (\"arbace/classes/env.clj\"), reading arbace.X
  names as clojure.X (stage 0)."
  [path]
  (let [rdr (.newInstance (.getConstructor (runtime-class "LineNumberingPushbackReader")
                                           (into-array Class [java.io.Reader]))
                          (object-array [(java.io.InputStreamReader. (.openStream ^java.net.URL (resource path)) "UTF-8")]))
        eof (Object.)]
    (binding [*ns* *ns* *file* path]
      (loop []
        (let [form (read {:eof eof :read-cond :allow} rdr)]
          (when-not (identical? form eof)
            (eval (map-form form))
            (recur)))))))

(defn load-compiler!
  "Loads (or reloads) the compiler's namespaces in order."
  []
  (if stage0?
    (doseq [ns namespaces]
      (load-mapped (str (.replace (.replace (name ns) "." "/") "-" "_") ".clj"))
      (dosync (commute @(resolve (symbol (str core-ns) "*loaded-libs*")) conj ns)))
    (apply require namespaces)))

(def ^:private core-names-file "arbace/core_classes.clj")

(defn- core-names
  "The public names that arbace/core_classes.clj defines."
  []
  (with-open [rdr (java.io.PushbackReader. (java.io.InputStreamReader. (.openStream ^java.net.URL (resource core-names-file)) "UTF-8"))]
    (let [eof (Object.)]
      (loop [out []]
        (let [f (read {:eof eof} rdr)]
          (if (identical? f eof)
            out
            (recur (if (and (seq? f) (#{'defmacro 'defn} (first f))) (conj out (second f)) out))))))))

(defn install!
  "At stage 0, loads the class forms' names (arbace/core_classes.clj) into clojure.core and
  refers them into namespace `ns` (default: the current one). From stage 1 on arbace.core has
  them already: nothing to do."
  ([] (install! *ns*))
  ([ns]
   (when stage0?
     (load-mapped core-names-file)
     (let [core (the-ns core-ns)]
       (binding [*ns* (the-ns ns)]
         (doseq [sym (core-names)]
           (let [cur (ns-resolve *ns* sym)]
             (when-not (and (var? cur) (not= (:ns (meta cur)) core))
               (.refer *ns* sym (ns-resolve core sym))))))))))

(load-compiler!)
(install! 'user)
