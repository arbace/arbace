(ns arbace.j2c.coverage
  "Coverage of the converter: javac's tree kinds met in converted sources and the forms written
  for them, from the reports (j2c-report.edn) of converter runs.

  bin/j2c -m arbace.j2c.coverage REPORT.edn... prints a Markdown summary."
  (:require [clojure.edn :as edn]
            [clojure.string :as str])
  (:import [com.sun.source.tree Tree$Kind]))

(def kind-forms
  "What the converter writes for each tree kind (spec §8.1)."
  {"COMPILATION_UNIT" "a file: `in-ns`, `import`, class forms; a package file with `ns` and `load`"
   "PACKAGE" "the namespace; `defpackage` for package annotations"
   "IMPORT" "`import` of the classes used (no wildcards, no static imports)"
   "MODULE" "`defmodule`" "REQUIRES" "`(requires ...)`" "EXPORTS" "`(exports ...)`"
   "OPENS" "`(opens ...)`" "USES" "`(uses ...)`" "PROVIDES" "`(provides ... :with [...])`"
   "CLASS" "`defclass`, `anon`, `letclass`" "INTERFACE" "`defclass ^:interface`"
   "ENUM" "`defclass ^:enum` with `constants`" "RECORD" "`defclass ^:record Name [components]`"
   "ANNOTATION_TYPE" "`defclass ^:annotation`, `:default`"
   "METHOD" "`method`, `constructor`" "VARIABLE" "`field`, parameters, `let`/`loop` bindings, `catch`, resources, patterns"
   "BLOCK" "bodies, `do`, `initializer`, `static-initializer`"
   "MODIFIERS" "metadata keywords" "ANNOTATION" "`^{A v}` metadata (SOURCE retention dropped)"
   "TYPE_ANNOTATION" "not written (todo)" "TYPE_PARAMETER" "`:type-params`"
   "PRIMITIVE_TYPE" "`int` ..." "ARRAY_TYPE" "`T/n`, `(array (G T))`" "PARAMETERIZED_TYPE" "`(List String)` in `:tag`"
   "UNION_TYPE" "`(catch [A B] e ...)`" "INTERSECTION_TYPE" "`(& A B)`"
   "UNBOUNDED_WILDCARD" "`?`" "EXTENDS_WILDCARD" "`(? extends T)`" "SUPER_WILDCARD" "`(? super T)`"
   "ANNOTATED_TYPE" "the type without its annotations (todo)"
   "EMPTY_STATEMENT" "nothing" "EXPRESSION_STATEMENT" "the form"
   "IF" "`if`, `when`, `when-not`, `cond`, folded into the rest of the block" "CONDITIONAL_EXPRESSION" "`if`"
   "WHILE_LOOP" "`while`, `loop`/`recur`" "DO_WHILE_LOOP" "`(loop [] ... (when c (recur)))`"
   "FOR_LOOP" "`loop`/`recur`, or `let` with `^:mutable` locals and `while`" "ENHANCED_FOR_LOOP" "`for-each`"
   "LABELED_STATEMENT" "`label` where a jump needs it" "BREAK" "elided, `(break)`, `(break :L)`"
   "CONTINUE" "elided, `recur`, `(continue ...)`" "RETURN" "the value, or `(return v)`" "YIELD" "the arm's value, or `(break :L v)`"
   "SWITCH" "`switch`, repeating code for fall-through" "SWITCH_EXPRESSION" "`switch`"
   "CASE" "`switch` arms" "CONSTANT_CASE_LABEL" "constants, enum names, lists" "PATTERN_CASE_LABEL" "`[pattern]`, `[pattern :when g]`"
   "DEFAULT_CASE_LABEL" "the trailing default, `(nil :default)`"
   "ANY_PATTERN" "`_`" "BINDING_PATTERN" "`^T x`" "DECONSTRUCTION_PATTERN" "`(R p...)`"
   "THROW" "`throw`" "TRY" "`try`, `with-resources`" "CATCH" "`catch`" "SYNCHRONIZED" "`locking`" "ASSERT" "`java-assert`"
   "IDENTIFIER" "locals, own fields by name, `C/f`, `(.-f this)`, `Outer/this`"
   "MEMBER_SELECT" "`(.-f x)`, `C/f`, `C/this`, `(alength a)`, class literals"
   "METHOD_INVOCATION" "`(.m x)`, `(C/m)`, param-tags pins, `(this. ...)`, `(super. ...)`"
   "NEW_CLASS" "`(C. ...)`, `(C/new ...)` pinned, `(.new o C ...)`, `anon`" "NEW_ARRAY" "`(new T/n dims)`, `(new T/n [..])`"
   "ARRAY_ACCESS" "`aget`" "ASSIGNMENT" "`set!`, `aset`"
   "PREFIX_INCREMENT" "`set!` with `unchecked-inc*`" "POSTFIX_INCREMENT" "`set!`, old value bound or update moved"
   "PREFIX_DECREMENT" "`set!` with `unchecked-dec*`" "POSTFIX_DECREMENT" "`set!`, old value bound or update moved"
   "UNARY_PLUS" "nothing" "UNARY_MINUS" "`unchecked-negate*`" "BITWISE_COMPLEMENT" "`bit-not*`" "LOGICAL_COMPLEMENT" "`not`"
   "INSTANCE_OF" "`instance?`, `if-instance`, `when-instance`" "TYPE_CAST" "`cast`, hints, `unchecked-*`, boxing calls"
   "LAMBDA_EXPRESSION" "`lambda`" "MEMBER_REFERENCE" "`method-ref`, array constructors as `lambda`"
   "PARENTHESIZED" "nothing" "INT_LITERAL" "numbers, radix kept, `(unchecked-int 0x...)`"
   "LONG_LITERAL" "numbers" "FLOAT_LITERAL" "`(float x)`" "DOUBLE_LITERAL" "numbers" "BOOLEAN_LITERAL" "`true`, `false`"
   "CHAR_LITERAL" "`\\c`, `(unchecked-char 0x...)`" "STRING_LITERAL" "strings" "NULL_LITERAL" "`nil`"
   "ERRONEOUS" "none (javac errors stop the conversion of a file)" "OTHER" "none"})

(def compound-kinds
  #{"MULTIPLY" "DIVIDE" "REMAINDER" "PLUS" "MINUS" "LEFT_SHIFT" "RIGHT_SHIFT" "UNSIGNED_RIGHT_SHIFT"
    "LESS_THAN" "GREATER_THAN" "LESS_THAN_EQUAL" "GREATER_THAN_EQUAL" "EQUAL_TO" "NOT_EQUAL_TO"
    "AND" "XOR" "OR" "CONDITIONAL_AND" "CONDITIONAL_OR"})

(defn- kind-text [k]
  (cond
    (kind-forms k) (kind-forms k)
    (str/ends-with? k "_ASSIGNMENT") "`set!` of the operator (narrowing written out)"
    (compound-kinds k) "operators of §5.4, `java-str`, `identical?`, `nil?`, `some?`, `and`, `or`"
    :else "?"))

(defn -main [& reports]
  (let [rs (map (comp edn/read-string slurp) reports)
        stats (apply merge-with + (map :stats rs))
        heads (apply merge-with + (map :heads rs))
        files (reduce + (map :files rs))
        converted (reduce + (map :converted rs))]
    (println (format "Corpus: %d Java files, %d converted.\n" files converted))
    (println "| tree kind | met | written as |")
    (println "|---|---:|---|")
    (doseq [k (map str (Tree$Kind/values))]
      (println (format "| `%s` | %s | %s |" k (get stats (keyword "kind" k) 0) (kind-text k))))
    (println "\nForms and metadata written (counts):\n")
    (println (str/join ", " (for [[k v] (sort-by key heads)] (str "`" k "` " v))))
    (println "\nOther counters:\n")
    (println (str/join ", " (for [[k v] (sort-by key stats)
                                  :when (not (#{"kind" "op" "lit" "dbg"} (namespace k)))]
                              (str "`" (subs (str k) 1) "` " v))))))
