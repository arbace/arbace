(ns arbace.classes.core
  "The user-facing names of the class forms (SPEC §9.5): what arbace.core will hold. At stage 0
  arbace.classes.boot interns them into clojure.core.

  `defclass` and `defclasses` run the class forms compiler at macroexpansion time. The other
  macros expand to the new special forms (class*, label*, break*, ...), which only the class forms
  compiler knows: they work inside class bodies. The new operators are functions that work
  anywhere; in class bodies they compile to single instructions."
  (:refer-clojure :exclude [])
  (:require [arbace.classes.compiler :as compiler]))

;; ---------------------------------------------------------------------------------------------
;; class forms

(defmacro defclass
  "(defclass name record-components? option* member*) defines a Java class (SPEC §4.1).
  Imports it and evaluates to the class."
  [& form]
  (let [[[n]] (compiler/compile-and-load! *ns* [form])]
    `(do (import '~(symbol n)) ~(symbol n))))

(defmacro defclasses
  "Several class forms compiled together, so they may refer to each other (the stage-0 stand-in
  for a top-level do of class forms, SPEC §9.2). Evaluates to a vector of the classes."
  [& forms]
  (doseq [f forms]
    (when-not (and (seq? f) (= "defclass" (name (first f))))
      (throw (IllegalArgumentException. (str "defclasses takes defclass forms: " (pr-str f))))))
  (let [ns (map first (compiler/compile-and-load! *ns* (map rest forms)))]
    `(do ~@(for [n ns] `(import '~(symbol n))) ~(mapv symbol ns))))

(defmacro anon
  "(anon Super [ctor-args*] member*): an anonymous class instance (SPEC §4.8)."
  [& form]
  `(~'class* :anon ~@form))

(defmacro letclass
  "(letclass [(name ...) ...] body*): local classes (SPEC §4.8)."
  [specs & body]
  `(~'class* :local ~specs ~@body))

;; ---------------------------------------------------------------------------------------------
;; code forms

(defmacro label "(label :L form*)" [kw & body] `(~'label* ~kw ~@body))
(defmacro break "(break), (break :L), (break :L value)" [& args] `(~'break* ~@args))
(defmacro continue "(continue arg*), (continue :L arg*)" [& args] `(~'continue* ~@args))
(defmacro return "(return), (return value)" [& args] `(~'return* ~@args))
(defmacro switch "(switch expr clause* default?)" [& args] `(~'switch* ~@args))
(defmacro lambda "(lambda FI params body*)" [& args] `(~'lambda* ~@args))
(defmacro method-ref "(method-ref FI sig? receiver? method)" [& args] `(~'method-ref* ~@args))
(defmacro java-str "(java-str x*): Java's string concatenation" [& args] `(~'java-str* ~@args))
(defmacro java-assert "(java-assert c message?)" [& args] `(~'java-assert* ~@args))
(defmacro for-each "(for-each [^T x coll] body*)" [binding & body] `(~'for-each* ~binding ~@body))

(defmacro with-resources
  "(with-resources [^R r init ...] body*): try-with-resources (SPEC §5.9)."
  [bindings & body]
  `(~'with-resources* ~bindings ~@body))

(defmacro if-instance
  "(if-instance [pattern expr] then else?)"
  [& args]
  `(~'if-instance* ~@args))

(defmacro when-instance
  "(when-instance [pattern expr] body*)"
  [binding & body]
  `(~'if-instance* ~binding (do ~@body)))

;; ---------------------------------------------------------------------------------------------
;; operators (SPEC §5.4); outside class bodies plain functions with Java's semantics

(defn bit-and-int [x y] (int (bit-and (int x) (int y))))
(defn bit-or-int [x y] (int (bit-or (int x) (int y))))
(defn bit-xor-int [x y] (int (bit-xor (int x) (int y))))
(defn bit-not-int [x] (int (bit-not (int x))))
(defn bit-shift-left-int [x n] (unchecked-int (bit-shift-left (int x) (bit-and (long n) 31))))
(defn bit-shift-right-int [x n] (int (bit-shift-right (int x) (bit-and (long n) 31))))
(defn unsigned-bit-shift-right-int [x n]
  (unchecked-int (unsigned-bit-shift-right (bit-and (long (int x)) 0xffffffff) (bit-and (long n) 31))))

(defn unchecked-divide
  "Java's / on long (ldiv) or double (ddiv)."
  [x y]
  (if (or (instance? Double x) (instance? Double y) (instance? Float x) (instance? Float y))
    (/ (double x) (double y))
    (quot (long x) (long y))))

(defn unchecked-remainder
  "Java's % on long (lrem) or double (drem)."
  [x y]
  (if (or (instance? Double x) (instance? Double y) (instance? Float x) (instance? Float y))
    (let [x (double x) y (double y)] (- x (* y (double (long (/ x y))))))
    (rem (long x) (long y))))

(defn unchecked-add-float [x y] (float (+ (float x) (float y))))
(defn unchecked-subtract-float [x y] (float (- (float x) (float y))))
(defn unchecked-multiply-float [x y] (float (* (float x) (float y))))
(defn unchecked-divide-float [x y] (float (/ (double (float x)) (double (float y)))))
(defn unchecked-remainder-float [x y] (float (unchecked-remainder (double (float x)) (double (float y)))))
(defn unchecked-negate-float [x] (float (- (float x))))
