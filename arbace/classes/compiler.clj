(ns arbace.classes.compiler
  "Compiles class forms (SPEC §9): enters the declarations of one top-level form, analyzes and
  emits all classes, then defines them in package class loaders or, under *compile-files*,
  writes them to *compile-path* as well."
  (:require [arbace.string :as str]
            [arbace.java.io :as io]
            [arbace.classes.types :as t]
            [arbace.classes.env :as env]
            [arbace.classes.parse :as p]
            [arbace.classes.analyze :as a]
            [arbace.classes.emit :as e])
  (:import (arbace.asm Opcodes)))

(defn- infer-permits!
  "PermittedSubclasses: :permits, or for a sealed class without it, the subclasses in the
  compilation (§4.13)."
  []
  (doseq [n @(:order a/*unit*)]
    (let [d (a/decl n)]
      ;; an enum with constant bodies is sealed, permitting them (javac)
      (when (and (= :enum (:kind d)) (some :has-body (:constants d)))
        (a/update-decl! n assoc :permits-final
                        (vec (for [c @(:order a/*unit*) :when (:enum-body (a/decl c))
                                   :when (= n (:super (a/decl c)))] c))))
      (when (:sealed (:meta d))
        (a/update-decl! n assoc :permits-final
                        (if (seq (:permits d))
                          (:permits d)
                          (vec (for [c @(:order a/*unit*)
                                     :let [cd (a/decl c)]
                                     :when (or (= n (:super cd)) (some #{n} (:interfaces cd)))]
                                 c))))))))

(defn compile-forms
  "Compiles class forms, each the rest of a (defclass ...) form, in namespace ns, as one
  compilation. Returns {:names [top-level names] :classes [{:name :bytes :info}]}."
  [ns forms]
  (binding [env/*compile-set* (atom {})
            a/*unit* {:order (atom []) :counters (atom {}) :switch-maps (atom {})
                      :switch-holders (atom {}) :source-tried (atom #{})
                      :assert-holders (atom {}) :holder-first (atom {})}]
    (let [names (vec (for [f forms]
                       (a/declare-class! {:nesting :top} (p/parse-class {:ns ns :nesting :top} f))))]
      (a/process-classes! 0)
      (infer-permits!)
      ;; javac's synthetic holder classes of the outermost class, named after all anonymous
      ;; classes: the $assertionsDisabled of interfaces gets a new one, the $SwitchMap$ arrays go
      ;; into an existing one (Lower.assertionsDisabledClass, outerCacheClass), in the order
      ;; javac meets them
      (doseq [top (distinct (concat (keys @(:switch-maps a/*unit*)) (keys @(:assert-holders a/*unit*))))]
        (let [enums (get @(:switch-maps a/*unit*) top)
              asserts? (contains? @(:assert-holders a/*unit*) top)
              shared? (or (not enums) (not asserts?) (= :assert (get @(:holder-first a/*unit*) top)))
              add! (fn [d] (swap! env/*compile-set* assoc (:name d) d) (swap! (:order a/*unit*) conj (:name d)))]
          (if shared?
            (let [h (a/local-class-name top nil)
                  d (cond-> (e/switch-holder-decl top h (or enums {}))
                      asserts? (update :state #(atom (assoc @% :uses-assert true))))]
              (add! d)
              (when enums (swap! (:switch-holders a/*unit*) assoc top h))
              (when asserts? (swap! (:assert-holders a/*unit*) assoc top h)))
            (let [h1 (a/local-class-name top nil)
                  _ (add! (e/switch-holder-decl top h1 enums))
                  h2 (a/local-class-name top nil)]
              (add! (e/assert-holder-decl top h2))
              (swap! (:switch-holders a/*unit*) assoc top h1)
              (swap! (:assert-holders a/*unit*) assoc top h2)))))
      (let [classes (vec (for [c @(:order a/*unit*)
                               :when (not (:declared-only (a/decl c)))]
                           {:name c :bytes (e/emit-class c) :info (a/decl c)}))]
        {:names names :classes classes}))))

(defn write-classes!
  "Writes class files under dir."
  [dir classes]
  (doseq [{:keys [name bytes]} classes]
    (let [f (io/file dir (str name ".class"))]
      (io/make-parents f)
      (with-open [o (io/output-stream f)] (.write o ^bytes bytes)))))

(defn compile-and-load!
  "Compiles class forms and defines the classes (also writing them under *compile-files*).
  Returns [[dotted-name Class] ...] of the top-level classes."
  [ns forms]
  (let [{:keys [names classes]} (compile-forms ns forms)]
    (when *compile-files*
      (write-classes! *compile-path* classes))
    (let [defined (env/define-classes! classes)]
      (mapv (fn [n] [(str/replace n "/" ".") (get defined n)]) names))))

;; ---------------------------------------------------------------------------------------------
;; modules and packages (§4.14): written only when compiling files

(defn- dotted->internal [s] (str/replace (str s) "." "/"))

(defn- module-version [name]
  (let [m (.findModule (java.lang.ModuleLayer/boot) (str name))]
    (when (.isPresent m)
      (let [v (.rawVersion (.getDescriptor ^java.lang.Module (.get m)))]
        (when (.isPresent v) (.get v))))))

(defn module-bytes
  "module-info.class of a defmodule form (name directive*)."
  [ns [nm & directives]]
  (let [m (meta nm)
        scope {:class nil :ns ns :local-classes {} :bounds {}}
        cw (arbace.asm.ClassWriter. 0)
        _ (.visit cw e/*version* Opcodes/ACC_MODULE "module-info" nil nil nil)
        mv (.visitModule cw (str nm) (bit-or (if (:open m) Opcodes/ACC_OPEN 0)
                                             (if (:synthetic m) Opcodes/ACC_SYNTHETIC 0))
                         nil)
        requires (filter #(= 'requires (first %)) directives)]
    (when-not (some #(= 'java.base (second %)) requires)
      (.visitRequire mv "java.base" Opcodes/ACC_MANDATED (module-version "java.base")))
    (doseq [[head x & opts] directives
            :let [xm (meta x) opts (apply hash-map opts)]]
      (case head
        requires (.visitRequire mv (str x)
                                (bit-or (if (:transitive xm) Opcodes/ACC_TRANSITIVE 0)
                                        (if (:static xm) Opcodes/ACC_STATIC_PHASE 0)
                                        (if (= 'java.base x) Opcodes/ACC_MANDATED 0))
                                (module-version x))
        exports (.visitExport mv (dotted->internal x) 0
                              (when-let [to (:to opts)] (into-array String (map str to))))
        opens (.visitOpen mv (dotted->internal x) 0
                          (when-let [to (:to opts)] (into-array String (map str to))))
        uses (.visitUse mv (dotted->internal x))
        provides (.visitProvide mv (dotted->internal x) (into-array String (map dotted->internal (:with opts))))
        (throw (IllegalArgumentException. (str "Bad module directive: " head)))))
    (.visitEnd mv)
    (e/write-annotations #(.visitAnnotation cw %1 %2) (a/annotations-now scope m))
    (.visitEnd cw)
    (.toByteArray cw)))

(defn package-bytes
  "package-info.class of a defpackage form: javac's synthetic interface with the annotations."
  [ns nm]
  (let [n (str (dotted->internal nm) "/package-info")
        cw (arbace.asm.ClassWriter. 0)]
    (.visit cw e/*version* (bit-or Opcodes/ACC_INTERFACE Opcodes/ACC_ABSTRACT Opcodes/ACC_SYNTHETIC)
            n nil "java/lang/Object" nil)
    (e/write-annotations #(.visitAnnotation cw %1 %2)
                         (a/annotations-now {:class nil :ns ns :local-classes {} :bounds {}} (meta nm)))
    (.visitEnd cw)
    {:name n :bytes (.toByteArray cw)}))

(defn write-module! [ns form]
  (when *compile-files*
    (write-classes! *compile-path* [{:name "module-info" :bytes (module-bytes ns form)}])))

(defn write-package! [ns nm]
  (when *compile-files*
    (write-classes! *compile-path* [(package-bytes ns nm)])))
