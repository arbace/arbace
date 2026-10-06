(ns arbace.j2c.jtypes
  "Helpers over javac's types and symbols for the converter: erasure, boxing, subtyping, flags,
  and the forms of types (§4.3 of doc/classes/SPEC.md)."
  (:require [arbace.j2c.forms :as f])
  (:import [com.sun.tools.javac.code Type Type$ArrayType Type$ClassType Type$WildcardType
            Type$TypeVar Type$IntersectionClassType Type$MethodType Types Symtab TypeTag Symbol
            Symbol$ClassSymbol Symbol$MethodSymbol Symbol$VarSymbol Flags Kinds$Kind BoundKind
            Type$CapturedType]))

(def ^:dynamic ^Types *types* nil)
(def ^:dynamic ^Symtab *syms* nil)
(def ^:dynamic ^com.sun.tools.javac.util.Names *names* nil)
(defn jname [^String s] (.fromString *names* s))
(def ^:dynamic *rename*
  "Function from a binary class name to its name in the output."
  identity)

(defn tag-name [^Type t] (str (.getTag t)))

(defn erasure ^Type [^Type t] (.erasure *types* t))

(defn prim? [t] (and (instance? Type t) (.isPrimitive ^Type t)))

(defn void? [t] (and (instance? Type t) (= "VOID" (tag-name t))))

(defn ref-type? [t] (and (instance? Type t) (not (.isPrimitive ^Type t)) (not (void? t))))

(defn same? [a b]
  (and (instance? Type a) (instance? Type b) (.isSameType *types* (erasure a) (erasure b))))

(defn subtype? [a b]
  (and (instance? Type a) (instance? Type b)
       (.isSubtype *types* (erasure a) (erasure b))))

(defn boxed ^Type [^Type t] (.type (.boxedClass *types* t)))

(defn unboxed
  "The primitive type a wrapper type unboxes to, or nil."
  [t]
  (when (instance? Type t)
    (let [u (.unboxedType *types* ^Type t)]
      (when (.isPrimitive u) u))))

(defn object-type ^Type [] (.objectType *syms*))
(defn object? [t] (same? t (object-type)))
(defn string-type? [t] (same? t (.stringType *syms*)))

(defn has-flag? [^Symbol s flag] (not (zero? (bit-and (.flags s) (long flag)))))

(defn flags-of [^Symbol s] (.flags s))

(defn static? [^Symbol s] (.isStatic s))

(defn kind [^Symbol s] (str (.kind s)))

(defn class-sym? [s] (instance? Symbol$ClassSymbol s))

(defn binary-name
  "The binary name of class symbol `c`, renamed for output."
  [^Symbol$ClassSymbol c]
  (*rename* (str (.flatName c))))

(defn package-name [^Symbol c]
  (*rename* (str (.getQualifiedName (.packge c)))))

(defn local-class? [^Symbol$ClassSymbol c] (.isDirectlyOrIndirectlyLocal c))

(defn anonymous? [^Symbol$ClassSymbol c] (.isAnonymous c))

(def prim-names #{"boolean" "byte" "short" "int" "long" "char" "float" "double" "void"})

(defn prim-sym [^Type t]
  (symbol (case (tag-name t)
            "BOOLEAN" "boolean" "BYTE" "byte" "SHORT" "short" "INT" "int" "LONG" "long"
            "CHAR" "char" "FLOAT" "float" "DOUBLE" "double" "VOID" "void")))

(def prim-rank {"BYTE" 1 "SHORT" 2 "CHAR" 2 "INT" 3 "LONG" 4 "FLOAT" 5 "DOUBLE" 6})

(defn widens?
  "Java's widening primitive conversion from `a` to `b` (or identity)."
  [a b]
  (let [ta (tag-name a) tb (tag-name b)]
    (or (= ta tb)
        (and (prim-rank ta) (prim-rank tb)
             (< (prim-rank ta) (prim-rank tb))
             (not (and (= ta "CHAR") (#{"SHORT"} tb)))
             (not (and (#{"BYTE" "SHORT"} ta) (= tb "CHAR")))))))

(defn components
  "The components of an intersection type, else nil."
  [^Type t]
  (when (instance? Type$IntersectionClassType t)
    (seq (.getExplicitComponents ^Type$IntersectionClassType t))))
