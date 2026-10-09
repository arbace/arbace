;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

;; The Go build's arbace/genclass.clj (doc/go/EVAL-NOTES.md, "Namespace variants"): it replaces
;; arbace/genclass.clj in the executable, whose gen-class and gen-interface write class files
;; with ASM, which the Go build has not (B1-PLAN.md D6). gen-interface makes the interface at
;; run time (Compiler$Dyn, C2G-SPEC §10.4; EVAL-PLAN.md Q5), so definterface and defprotocol
;; work; gen-class is left out (D6). prim->class, the-array-class and the-class are
;; arbace/genclass.clj's.

(in-ns 'arbace.core)

;; arbace/genclass.clj's imports, less ASM's (core_deftype.clj uses them)
(import '(java.lang.reflect Modifier Constructor)
        '(arbace.lang IPersistentMap))

(def ^{:private true} prim->class
     {'int Integer/TYPE
      'ints (Class/forName "[I")
      'long Long/TYPE
      'longs (Class/forName "[J")
      'float Float/TYPE
      'floats (Class/forName "[F")
      'double Double/TYPE
      'doubles (Class/forName "[D")
      'void Void/TYPE
      'short Short/TYPE
      'shorts (Class/forName "[S")
      'boolean Boolean/TYPE
      'booleans (Class/forName "[Z")
      'byte Byte/TYPE
      'bytes (Class/forName "[B")
      'char Character/TYPE
      'chars (Class/forName "[C")})

(defn- the-array-class [sym]
  (arbace.lang.RT/classForName
   (let [cn (namespace sym)]
     (arbace.lang.Compiler$HostExpr/buildArrayClassDescriptor
      (if (or (arbace.lang.Compiler/primClass (symbol cn)) (some #{\.} cn))
        sym
        (symbol (str "java.lang." cn) (name sym)))))))

(defn- ^Class the-class [x]
  (cond
    (class? x) x
    (symbol? x) (cond (contains? prim->class x) (prim->class x)
                      (arbace.lang.Compiler$HostExpr/looksLikeArrayClass x)
                        (the-array-class x)
                      :else (let [strx (str x)]
                              (arbace.lang.RT/classForName
                               (if (some #{\. \[} strx)
                                 strx
                                 (str "java.lang." strx)))))
    :else (arbace.lang.RT/classForName x)))

(defmacro gen-class
  "Not in the Go build (B1-PLAN.md D6): gen-class writes a class file."
  {:added "1.0"}
  [& options]
  (throw (UnsupportedOperationException. "gen-class is not available in the Go build")))

(defmacro gen-interface
  "Makes an interface with the given package-qualified :name, extending the :extends
  interfaces, with the :methods [[name [param-types] return-type] ...]; in the Go build it is
  made at run time (no class file is written), and evaluates to the interface."
  {:added "1.0"}
  [& options]
  (let [{:keys [name extends methods]} (apply hash-map options)]
    (when (some #(-> % first arbace.core/name (.contains "-")) methods)
      (throw
        (IllegalArgumentException. "Interface methods must not contain '-'")))
    (let [c (arbace.lang.Compiler$Dyn/defineInterface
              (str name) (into-array Class (map the-class extends)))]
      (doseq [[mname pclasses rclass] methods]
        (arbace.lang.Compiler$Dyn/addInterfaceMethod
          c (str mname) (into-array Class (map the-class pclasses)) (the-class rclass)))
      c)))
