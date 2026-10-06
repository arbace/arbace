;; Stage-0 driver of the class forms compiler (doc/classes/SPEC.md §9.6).
;;
;; The compiler's sources name ASM as `arbace.asm.*`, the package Arbace will vendor it under.
;; At stage 0 only the frozen baseline's `clojure.asm` exists, so this driver loads the
;; compiler's namespaces itself, rewriting every symbol `arbace.asm.X` (also inside metadata,
;; such as type hints) to `clojure.asm.X` while it reads them. Then it interns the user-facing
;; macros and functions of `arbace.classes.core` into `clojure.core` (at run time; the frozen
;; sources are not touched) and refers them into the current namespace, so class forms can be
;; written without qualifying the new names.
;;
;; Use: (require 'arbace.classes.boot), or (load-file "arbace/classes/boot.clj").
(ns arbace.classes.boot
  (:require [clojure.string :as str]))

(def ^:private namespaces
  '[arbace.classes.types
    arbace.classes.env
    arbace.classes.parse
    arbace.classes.analyze
    arbace.classes.emit
    arbace.classes.compiler
    arbace.classes.core
    arbace.classes.shape])

(defn- map-sym [s]
  (let [n (name s) ns (namespace s)]
    (cond
      (and ns (str/starts-with? ns "arbace.asm"))
      (with-meta (symbol (str "clojure.asm" (subs ns 10)) n) (meta s))
      (and (nil? ns) (str/starts-with? n "arbace.asm"))
      (with-meta (symbol (str "clojure.asm" (subs n 10))) (meta s))
      :else s)))

(defn- map-form [f]
  (let [m (meta f)
        f2 (cond
             (symbol? f) (map-sym f)
             (seq? f) (apply list (map map-form f))
             (map-entry? f) (vec (map map-form f))
             (vector? f) (vec (map map-form f))
             (map? f) (into (empty f) (map (fn [[k v]] [(map-form k) (map-form v)]) f))
             (set? f) (into (empty f) (map map-form f))
             :else f)]
    (if (and m (instance? clojure.lang.IObj f2))
      (with-meta f2 (map-form m))
      f2)))

(defn load-mapped
  "Loads the Clojure source resource `path` (\"arbace/classes/env.clj\"), mapping arbace.asm
  to clojure.asm."
  [path]
  (let [url (or (.getResource (clojure.lang.RT/baseLoader) path)
                (throw (java.io.FileNotFoundException. path)))
        rdr (clojure.lang.LineNumberingPushbackReader.
              (java.io.InputStreamReader. (.openStream url) "UTF-8"))
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
  (doseq [ns namespaces]
    (load-mapped (str (str/replace (munge (name ns)) "." "/") ".clj"))
    (dosync (commute @#'clojure.core/*loaded-libs* conj ns))))

(defn install!
  "Interns the public vars of arbace.classes.core into clojure.core (as arbace.core will hold
  them) and refers them into namespace `ns` (default: the current one)."
  ([] (install! *ns*))
  ([ns]
   (let [core (the-ns 'clojure.core)]
     (doseq [[sym v] (ns-publics 'arbace.classes.core)]
       (let [cv (intern core sym @v)]
         (alter-meta! cv merge (select-keys (meta v) [:macro :arglists :doc :inline :inline-arities]))))
     (binding [*ns* (the-ns ns)]
       (doseq [[sym _] (ns-publics 'arbace.classes.core)]
         (let [cur (ns-resolve *ns* sym)]
           (when-not (and (var? cur) (not= (.ns ^clojure.lang.Var cur) core))
             (.refer ^clojure.lang.Namespace *ns* sym (ns-resolve core sym)))))))))

(load-compiler!)
(install! 'user)
