(ns arbace.classes.compiler
  "Compiles class forms (SPEC §9): enters the declarations of one top-level form, analyzes and
  emits all classes, then defines them in package class loaders or, under *compile-files*,
  writes them to *compile-path* as well."
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
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
                      :switch-holders (atom {}) :source-tried (atom #{})}]
    (let [names (vec (for [f forms]
                       (a/declare-class! {:nesting :top} (p/parse-class {:ns ns :nesting :top} f))))]
      (a/process-classes! 0)
      (infer-permits!)
      ;; javac's $SwitchMap$ holder classes, named after all anonymous classes
      (doseq [[top enums] @(:switch-maps a/*unit*)]
        (let [h (a/local-class-name top nil)]
          (swap! env/*compile-set* assoc h (e/switch-holder-decl top h enums))
          (swap! (:order a/*unit*) conj h)
          (swap! (:switch-holders a/*unit*) assoc top h)))
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
