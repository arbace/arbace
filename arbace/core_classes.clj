;; The class forms' names in arbace.core (doc/classes/SPEC.md §9.5): the macros over the special
;; forms class*, label*, break*, continue*, return*, switch*, lambda*, method-ref*, java-str*,
;; java-assert*, for-each*, with-resources* and if-instance*, and the operators of §5.4.
;;
;; Loaded by arbace/core.clj. The special forms are known to arbace.lang.Compiler, which hands
;; them to the class forms compiler (arbace.classes, loaded on first use; see
;; arbace.classes.native). Until the freeze, stage 0 was the frozen Clojure, whose compiler knows
;; none of them: there arbace.classes.boot loaded this file into clojure.core (reading arbace.X
;; as clojure.X), and `defclass` and `defclasses` ran the class forms compiler at
;; macroexpansion time instead; the non-native branches below (class-forms-native? false) are
;; what is left of that, unused since.

(in-ns 'arbace.core)

(defn- class-forms-native?
  "Does the running compiler know the class forms' special forms (stage 1 on)?"
  []
  (contains? (. arbace.lang.Compiler specials) 'class*))

(defn- class-forms-fn
  "The function named by qualified symbol sym of the class forms compiler, loading it first."
  [sym]
  (require (symbol (namespace sym)))
  (resolve sym))

;; ---------------------------------------------------------------------------------------------
;; class forms

(defmacro defclass
  "(defclass name record-components? option* member*) defines a Java class (SPEC §4.1).
  Imports it and evaluates to the class. Class forms of one top-level do may refer to each
  other (SPEC §9.2)."
  {:added "arbace"}
  [& form]
  (if (class-forms-native?)
    (list 'class* :top form)
    (let [[[n]] ((class-forms-fn 'arbace.classes.compiler/compile-and-load!) *ns* [form])]
      `(do (import '~(symbol n)) ~(symbol n)))))

(defmacro defclasses
  "(defclasses (defclass ...) ...): class forms compiled together, so they may refer to each
  other (SPEC §9.2). Evaluates to a vector of the classes."
  {:added "arbace"}
  [& forms]
  (doseq [f forms]
    (when-not (and (seq? f) (symbol? (first f)) (= "defclass" (name (first f))))
      (throw (IllegalArgumentException. (str "defclasses takes defclass forms: " (pr-str f))))))
  (if (class-forms-native?)
    (list 'class* :tops (map rest forms))
    (let [ns (map first ((class-forms-fn 'arbace.classes.compiler/compile-and-load!) *ns* (map rest forms)))]
      `(do ~@(for [n ns] `(import '~(symbol n))) ~(mapv symbol ns)))))

(defmacro anon
  "(anon Super [ctor-args*] member*), (anon Inner [ctor-args*] :outer o member*): an instance of
  an anonymous class (SPEC §4.8)."
  {:added "arbace"}
  [& form]
  `(~'class* :anon ~@form))

(defmacro letclass
  "(letclass [(name ...) ...] body*): local classes (SPEC §4.8)."
  {:added "arbace"}
  [specs & body]
  `(~'class* :local ~specs ~@body))

(defmacro defmodule
  "(defmodule name directive*) writes module-info.class when compiling files (SPEC §4.14)."
  {:added "arbace"}
  [& form]
  ((class-forms-fn 'arbace.classes.compiler/write-module!) *ns* form)
  nil)

(defmacro defpackage
  "(defpackage name) writes the package's package-info.class (its annotations) when compiling
  files (SPEC §4.14)."
  {:added "arbace"}
  [nm]
  ((class-forms-fn 'arbace.classes.compiler/write-package!) *ns* nm)
  nil)

;; ---------------------------------------------------------------------------------------------
;; code forms (SPEC §5.7 to §5.12)

(defmacro label "(label :L form*): a named statement (SPEC §5.7)." {:added "arbace"}
  [kw & body] `(~'label* ~kw ~@body))
(defmacro break "(break), (break :L), (break :L value) (SPEC §5.7)." {:added "arbace"}
  [& args] `(~'break* ~@args))
(defmacro continue "(continue arg*), (continue :L arg*) (SPEC §5.7)." {:added "arbace"}
  [& args] `(~'continue* ~@args))
(defmacro return "(return), (return value) (SPEC §5.7)." {:added "arbace"}
  [& args] `(~'return* ~@args))
(defmacro switch "(switch expr clause* default?) (SPEC §5.8)." {:added "arbace"}
  [& args] `(~'switch* ~@args))
(defmacro lambda "(lambda FI params body*) (SPEC §5.12)." {:added "arbace"}
  [& args] `(~'lambda* ~@args))
(defmacro method-ref "(method-ref FI sig? receiver? method) (SPEC §5.12)." {:added "arbace"}
  [& args] `(~'method-ref* ~@args))
(defmacro java-str "(java-str x*): Java's string concatenation (SPEC §5.10)." {:added "arbace"}
  [& args] `(~'java-str* ~@args))
(defmacro java-assert "(java-assert c message?): Java's assert (SPEC §5.9)." {:added "arbace"}
  [& args] `(~'java-assert* ~@args))
(defmacro for-each "(for-each [^T x coll] body*): Java's enhanced for (SPEC §5.7)." {:added "arbace"}
  [binding & body] `(~'for-each* ~binding ~@body))

(defmacro with-resources
  "(with-resources [^R r init ...] body*): try-with-resources (SPEC §5.9)."
  {:added "arbace"}
  [bindings & body]
  `(~'with-resources* ~bindings ~@body))

(defmacro if-instance
  "(if-instance [pattern expr] then else?): Java's instanceof with a pattern (SPEC §5.8)."
  {:added "arbace"}
  [& args]
  `(~'if-instance* ~@args))

(defmacro when-instance
  "(when-instance [pattern expr] body*) (SPEC §5.8)."
  {:added "arbace"}
  [binding & body]
  `(~'if-instance* ~binding (do ~@body)))

;; ---------------------------------------------------------------------------------------------
;; operators (SPEC §5.4): in class bodies single instructions; elsewhere functions with Java's
;; semantics whose :inline expansions call arbace.lang.Numbers, which the compiler emits as
;; instructions where the operands are primitive, as Clojure's own operators (SPEC §9.5)

(defmacro ^:private defop
  "Defines operator `name` (SPEC §5.4) calling arbace.lang.Numbers/`method`, inlined. At stage
  0 the frozen clojure.lang.Numbers lacks the methods: there it is a plain function of `body`,
  the fallback of class bodies (arbace.classes) for operands that fit no instruction."
  [name doc method params body]
  (if (class-forms-native?)
    `(defn ~name ~doc
       {:added "arbace"
        :inline (fn ~params (list '. 'arbace.lang.Numbers (list '~method ~@params)))
        :inline-arities #{~(count params)}}
       ~params (. arbace.lang.Numbers (~method ~@params)))
    `(defn ~name ~doc {:added "arbace"} ~params ~body)))

(defop bit-and-int "Java's & on int: its operands converted as by `int`." andInt [x y]
  (int (bit-and (int x) (int y))))
(defop bit-or-int "Java's | on int: its operands converted as by `int`." orInt [x y]
  (int (bit-or (int x) (int y))))
(defop bit-xor-int "Java's ^ on int: its operands converted as by `int`." xorInt [x y]
  (int (bit-xor (int x) (int y))))
(defop bit-not-int "Java's ~ on int: its operand converted as by `int`." notInt [x]
  (int (bit-not (int x))))
(defop bit-shift-left-int "Java's << on int: x shifted by the low 5 bits of n." shiftLeftInt [x n]
  (unchecked-int (bit-shift-left (int x) (bit-and (long n) 31))))
(defop bit-shift-right-int "Java's >> on int: x shifted by the low 5 bits of n." shiftRightInt [x n]
  (int (bit-shift-right (int x) (bit-and (long n) 31))))
(defop unsigned-bit-shift-right-int "Java's >>> on int: x shifted by the low 5 bits of n."
  unsignedShiftRightInt [x n]
  (unchecked-int (unsigned-bit-shift-right (bit-and (long (int x)) 0xffffffff) (bit-and (long n) 31))))

(defop unchecked-divide "Java's / on long (ldiv) or, when an operand is a double or float, double (ddiv)."
  unchecked_divide [x y]
  (if (or (instance? Double x) (instance? Double y) (instance? Float x) (instance? Float y))
    (/ (double x) (double y))
    (quot (long x) (long y))))

(defop unchecked-remainder "Java's % on long (lrem) or, when an operand is a double or float, double (drem)."
  unchecked_remainder [x y]
  (if (or (instance? Double x) (instance? Double y) (instance? Float x) (instance? Float y))
    (let [x (double x) y (double y)] (- x (* y (double (long (/ x y))))))
    (rem (long x) (long y))))

(defop unchecked-add-float "Java's + on float: its operands converted as by `float`." unchecked_float_add [x y]
  (float (+ (float x) (float y))))
(defop unchecked-subtract-float "Java's - on float." unchecked_float_subtract [x y]
  (float (- (float x) (float y))))
(defop unchecked-multiply-float "Java's * on float." unchecked_float_multiply [x y]
  (float (* (float x) (float y))))
(defop unchecked-divide-float "Java's / on float." unchecked_float_divide [x y]
  (float (/ (double (float x)) (double (float y)))))
(defop unchecked-remainder-float "Java's % on float." unchecked_float_remainder [x y]
  (float (unchecked-remainder (double (float x)) (double (float y)))))
(defop unchecked-negate-float "Java's unary - on float." unchecked_float_negate [x]
  (float (- (float x))))
