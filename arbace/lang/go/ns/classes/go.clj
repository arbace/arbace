(ns arbace.classes.go
  "What the class forms' analysis (arbace.classes.analyze) takes from the JVM, in the Go build
  (doc/go/CLASSFORMS-REPL.md): ASM's TypeReference (arbace.asm is outside the Go build's world),
  whose type reference values (sort << 24 | ...) it computes for type annotations, as ASM does,
  each answering getValue; and the generic signatures of the classes of the world, which jrt's
  reflection does not have (arbace/classes/generics.edn, written by c2g --program), for javac's
  bridges. arbace.classes.analyze refers to it as TypeReference."
  (:require [arbace.classes.types :as t]
            [arbace.string :as str]))

(defn- tref [^long v] (reify java.util.Map$Entry (getKey [_] nil) (getValue [_] (int v))))

(def CLASS_TYPE_PARAMETER 0x00)
(def METHOD_TYPE_PARAMETER 0x01)
(def CLASS_EXTENDS 0x10)
(def CLASS_TYPE_PARAMETER_BOUND 0x11)
(def METHOD_TYPE_PARAMETER_BOUND 0x12)
(def FIELD 0x13)
(def METHOD_RETURN 0x14)
(def METHOD_RECEIVER 0x15)
(def METHOD_FORMAL_PARAMETER 0x16)
(def THROWS 0x17)
(def LOCAL_VARIABLE 0x40)
(def RESOURCE_VARIABLE 0x41)
(def EXCEPTION_PARAMETER 0x42)
(def INSTANCEOF 0x43)
(def NEW 0x44)
(def CONSTRUCTOR_REFERENCE 0x45)
(def METHOD_REFERENCE 0x46)
(def CAST 0x47)
(def CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT 0x48)
(def METHOD_INVOCATION_TYPE_ARGUMENT 0x49)
(def CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT 0x4A)
(def METHOD_REFERENCE_TYPE_ARGUMENT 0x4B)

(defn newTypeReference [sort] (tref (bit-shift-left (long sort) 24)))
(defn newTypeParameterReference [sort i]
  (tref (bit-or (bit-shift-left (long sort) 24) (bit-shift-left (long i) 16))))
(defn newTypeParameterBoundReference [sort p b]
  (tref (bit-or (bit-shift-left (long sort) 24) (bit-shift-left (long p) 16) (bit-shift-left (long b) 8))))
(defn newSuperTypeReference [i]
  (tref (bit-or (bit-shift-left CLASS_EXTENDS 24) (bit-shift-left (bit-and (long i) 0xFFFF) 8))))
(defn newFormalParameterReference [i]
  (tref (bit-or (bit-shift-left METHOD_FORMAL_PARAMETER 24) (bit-shift-left (long i) 16))))
(defn newExceptionReference [i] (tref (bit-or (bit-shift-left THROWS 24) (bit-shift-left (long i) 8))))
(defn newTryCatchReference [i]
  (tref (bit-or (bit-shift-left EXCEPTION_PARAMETER 24) (bit-shift-left (long i) 8))))
(defn newTypeArgumentReference [sort i]
  (tref (bit-or (bit-shift-left (long sort) 24) (long i))))

;; ---------------------------------------------------------------------------------------------
;; generic signatures

(def ^:private world-generics
  (delay
    (if-let [in (arbace.lang.RT/resourceAsStream nil "arbace/classes/generics.edn")]
      (binding [*read-eval* false]
        (read-string (slurp in)))
      {})))

(defn generics
  "arbace.classes.analyze/class-generics of class n (Class c), which is not being compiled: from
  the world's generic signatures when n is generic or has generic supertypes, else its erased
  view by reflection."
  [n ^Class c]
  (let [g (get @world-generics n)
        raw (fn []
              (for [^java.lang.reflect.Method m (.getDeclaredMethods c)]
                {:name (.getName m)
                 :desc (t/method-desc (map t/class->desc (.getParameterTypes m)) (t/class->desc (.getReturnType m)))
                 :flags (.getModifiers m)
                 :throws (mapv #(str/replace (.getName ^Class %) "." "/") (.getExceptionTypes m))
                 :params (mapv t/reflect->tnode (.getParameterTypes m))
                 :bounds {}}))]
    (cond
      (and g (:methods g)) g
      g (assoc g :methods (raw))
      :else {:tparams []
             :supers (remove nil? (cons (some-> (.getSuperclass c) t/reflect->tnode)
                                        (map t/reflect->tnode (.getInterfaces c))))
             :methods (raw)})))
