# Java → class forms converter: implementation notes

The converter of agenda step 3: it turns Java sources into the class forms of [SPEC.md](SPEC.md),
making explicit what §7 lists. These notes say how it is built and used, what it was checked on,
how its output compares with the spec's worked examples (§11), and where converting showed that
the spec should change (proposed amendments, last sections). The compiler side is in
[COMPILER-NOTES.md](COMPILER-NOTES.md).

## Layout

| file | namespace | role |
|---|---|---|
| `arbace/j2c/javac.clj` | `arbace.j2c.javac` | runs the JDK's javac (`JavacTask.parse` + `analyze`: attribution and flow, no desugaring) |
| `arbace/j2c/jtypes.clj` | `arbace.j2c.jtypes` | javac types and symbols: erasure, boxing, widening, flags, binary names, renaming |
| `arbace/j2c/forms.clj` | `arbace.j2c.forms` | output data: forms, class references named later (`CRef`), raw tokens, ordered metadata |
| `arbace/j2c/convert.clj` | `arbace.j2c.convert` | the conversion of attributed trees: declarations, code, control flow, patterns, switches, lambdas |
| `arbace/j2c/names.clj` | `arbace.j2c.names` | one namespace per package: which class gets which simple name, imports, file layout |
| `arbace/j2c/print.clj` | `arbace.j2c.print` | printing with metadata and a Clojure-style layout |
| `arbace/j2c/main.clj` | `arbace.j2c.main` | command line: `convert`, `jdk` |
| `arbace/j2c/build.clj` | `arbace.j2c.build` | compiles converted files with the class forms compiler into class files |
| `arbace/j2c/coverage.clj` | `arbace.j2c.coverage` | coverage summary from run reports |
| `bin/j2c` | | starts the frozen Clojure with access to javac's internals |
| `bin/j2c-check` | | the checks (below) |
| `test/j2c/java/sample/` | | Java constructs the baseline does not use (records, sealed types, patterns, switch expressions, lambdas, method references, local and anonymous classes, annotation types), including §11.4 |
| `test/j2c/module/` | | a module declaration and package annotations |

The converted files are not kept in the repository: they are regenerated (the user's decision).

## Use

```
bin/j2c -m arbace.j2c.main convert [--rename FROM=TO] [--javac OPT]... [--check] OUT ROOT [PATH...]
bin/j2c -m arbace.j2c.main jdk [--check] OUT MODULE MODULE-SRC [PATH...]
bin/j2c -m arbace.j2c.build CONVERTED-DIR CLASSES-DIR [PACKAGE-DIR...]
bin/j2c -m arbace.j2c.coverage REPORT.edn...
bin/j2c-check [--suite]
```

- `convert` converts the `.java` files under `ROOT` (or under the `PATH`s) into `OUT`, with
  `ROOT` as javac's source path. `bin/j2c -m arbace.j2c.main convert .tmp/j2c clojure-out . clojure`
  would be the baseline; use a directory under `.tmp/`.
- `--rename clojure=arbace` renames the package prefix in every class name, namespace and path
  (agenda step 4). String literals are left alone (`"clojure.core"` in `RT` stays; step 4
  decides).
- `--check` reads every output file back with `clojure.core/read` and `clojure.edn/read`, and
  writes `OUT/j2c-report.edn`: failures, unreadable files, counters of tree kinds met and of the
  forms and conversions written.
- `jdk` converts a JDK module's sources against the running JDK's own classes: the files are
  copied into `--patch-module` directories under `.tmp/j2c-patch/`, in chunks of packages
  converted in parallel (`J2C_THREADS`, default 12). A `module-info.java` cannot be patched in
  and is skipped (modules are converted from ordinary source trees, `test/j2c/module`).
- `build` compiles converted files with the class forms compiler (`arbace.classes`, the other
  half of the work, loaded through `arbace.classes.boot`) and writes the class files.

Output layout (§9.1): for package `p.q`, `p/q.clj` holds `(ns p.q)` and a `(load "q/File")` per
Java file, supertypes in the same package first; `p/q/File.clj` holds `(in-ns 'p.q)`, an
`(import ...)` of the classes it uses, and the file's class forms. A comment line names the Java
file; Java's comments are not carried over.

## How it works

1. **javac.** All files of a run are attributed together, so every name, overload, implicit
   conversion, constant value and lambda target is known. The trees are javac's after `Attr` and
   `Flow`: they contain the implicit `super()` calls and default constructors that `Attr` and
   `TypeEnter` add; the converter leaves out what javac generated (an implicit `super()` is
   recognized by the source text at its position).
2. **Expressions** convert to a form and the static type Arbace will give it (an erased javac
   type; `:lit-int` for an integer literal, which is a `long` to Clojure but narrows in an `int`
   context; `:null`). Conversions are then made explicit where the context's type, computed as
   javac's `TransTypes` does, differs: erasure casts (arguments to the erasure of the
   *instantiated* parameter types, qualifiers to the erasure of their type, results of generic
   members), unboxing from non-wrapper types, boxing of literals, narrowing, constant
   promotions (`(unchecked-multiply-float c (float 2.0))`, `(< side 0.0)`).
3. **Overloads.** A call is pinned with param-tags unless only one method of that name and arity
   is accessible, or the most specific match is javac's choice under both Java's and Clojure's
   argument rules (literals tried as `int` and as `long`). This is conservative: the compiler's
   JLS-like resolution (COMPILER-NOTES, amendment 2) would need fewer pins.
4. **Statements** convert in a context `{:fall :jumps :vpos}`: the forms that follow when the
   statement completes normally (for example `(recur ...)` at the end of a loop body), the jump
   targets equivalent to completing normally here with what replaces such a jump, and whose value
   this position delivers. A `return e` in value position is just `e`; `break`, `continue` and
   `yield` to an equivalent target disappear or become the `recur`; the rest stay as `return`,
   `break`, `continue` (labels added only where used). An `if` whose branch ends in such a jump
   takes the rest of the block as its other branch (`if (c) return a; return b;` is
   `(if c a b)`), and chains become `cond`, `when`, `when-not`.
5. **Loops.** A `for` whose variables are declared in it and changed only by its update becomes
   `loop`/`recur` (`int` and `float` bindings always tagged, since `loop` widens untagged ones);
   other `for` loops become `let` with `^:mutable` locals and `while` (a `continue` repeats the
   update), `do`-`while` becomes `(loop [] body (when c (recur)))`, enhanced `for` `for-each`.
6. **Locals.** Each declaration is a `let` over the rest of its block. `^:mutable` marks locals
   and parameters assigned anywhere; a local declared without initializer and assigned in every
   branch of the next `if` gets the `if` as its initializer; others get Java's default value.
   Tags are written where the declared type differs from the initializer's (or is generic, or
   the initializer is a literal), `^:const` on constant variables. Postfix increments inside a
   statement whose variable is not otherwise read there move after the statement
   (`(aset arr i x) (set! i (unchecked-inc-int i))`) or bind the old value before it.
7. **Patterns.** `instanceof` patterns become `if-instance`/`when-instance` with Java's flow
   scoping: through `&&`, `||` and `!`, into the rest of the block after an early exit, in
   conditionals, `while` and `for` conditions and `if`-assigned locals. A case with several
   patterns (`case A _, B _ ->`) becomes one arm per pattern. A branch may be repeated where
   `&&`/`||` need it (the rest of a block is never repeated).
8. **switch.** Old-style groups become arms with the labels as a list; fall-through repeats the
   following arms' code (§12 question 9); `default` goes last; `yield` is the arm's value or
   `(break :switch v)`; locals declared in one case and used in another are bound before the
   switch; javac's implicit `MatchException` default is written where javac adds it.
9. **Names.** A class is written by its simple name when it is a member class in scope; else by
   the name the package namespace gives it: one class per simple name per package (same-package
   classes and Clojure's default imports first, then the most used), imported per file; all
   others by binary name. Fields of enclosing classes are written bare when nothing shadows them;
   inherited ones as `(.-f this)` or `C/f` with javac's qualifying class.
10. **Printing.** Metadata is kept in order (`^:public ^:static ^{A v} ^T name`); radix of integer
    literals is kept; layout follows the usual Clojure indentation, 100 columns.

## Results

All on 2026-10-06, jdk26u at the system JDK (26.0.2.1).

| corpus | files | converted | failures | unreadable (`clojure.core/read`) |
|---|---:|---:|---:|---:|
| baseline `clojure/` | 183 | 183 | 0 | 0 |
| jdk26u `src/*/share/classes`, all 68 modules (module-infos skipped) | 12,730 | 12,730 | 0 | 0 |
| `test/j2c/java`, `test/j2c/module` | 9 | 9 | 0 | 0 |

- **Compiled with the class forms compiler** (`bin/class-forms-check`, compiler at `origin/main`
  of the same day): every one of the 187 converted baseline files with class forms compiles, and
  every class (812, the same names as javac's) has the same shape as javac's: flags, supertypes,
  signatures, nest and inner class attributes, fields, methods, and the symbolic content of code
  (member and class references, call sites, constants).
- **Clojure's test suite** (`bin/clojure-tests`) on those 812 classes, compiled from the
  converted forms, with the baseline's `.clj` sources: 83 namespaces, 809 tests, 20,718 of
  20,750 assertions pass and 27 of 27 test.generative specs pass, the same as the baseline (the
  32 failures expect spec's messages). No regressions.
- `test/j2c/java/sample`: 4 of 5 files shape-identical to javac, §11.4 included. `Features`
  compiles with three differences, none from the converter's output being wrong: the constants of
  `d + (z ? 1 : 2)` (amendment 13); the name of a serializable lambda, which javac derives from a
  hash (`lambda$lambdas$35817b2b$1`, for the compiler and §3.6's exemptions); and javac's merging
  of record patterns with a common record type into one nested `typeSwitch` (for the compiler).
- A sample of the JDK conversion compiled the same way (`javax/security/auth/x500`,
  `sun/util/calendar`, `jdk/internal/util`): the differences are javac's string concatenation
  with `StringBuilder` (java.base is built with `-XDstringConcat=inline`), one array upcast
  (fixed since: javac keeps `checkcast` for casts to array types), and a compiler error on a type
  variable of a method used inside an anonymous class (`Preconditions`, for the compiler).
- `clojure.edn/read` reads none of the files that use Clojure 1.12 syntax: array class
  symbols (`int/1`) and param-tags (`^[int]`) are not EDN (amendment 2).

`bin/j2c-check` reruns the baseline, compiler and sample checks (one minute); with `--suite` it
also builds the converted baseline and runs Clojure's test suite on it.

## Coverage

Tree kinds met in the whole corpus (baseline, jdk26u, samples) and what they become. Every kind
met was converted without failure. Generated by `arbace.j2c.coverage` (rounded counts).

| tree kinds | met | written as |
|---|---|---|
| `COMPILATION_UNIT`, `PACKAGE`, `IMPORT` | all files | file with `in-ns`, `import`; package file with `ns`, `load`; `defpackage` |
| `MODULE`, `REQUIRES`, `EXPORTS`, `OPENS`, `USES`, `PROVIDES` | test module | `defmodule` |
| `CLASS`, `INTERFACE`, `ENUM`, `RECORD`, `ANNOTATION_TYPE` | ✓ | `defclass` with kind metadata, `anon`, `letclass`, `constants` |
| `METHOD`, `VARIABLE`, `BLOCK`, `MODIFIERS`, `ANNOTATION`, `TYPE_PARAMETER` | ✓ | `method`, `constructor`, `field`, bindings, metadata, `:type-params` |
| `TYPE_ANNOTATION`, `ANNOTATED_TYPE` | 6, 0 | **dropped** (todo) |
| type kinds, wildcards, `UNION_TYPE`, `INTERSECTION_TYPE` | ✓ | type forms, `(catch [A B] e)`, `(& A B)` |
| `IF`, `CONDITIONAL_EXPRESSION` | ✓ | `if`, `when`, `when-not`, `cond`, folding |
| loops | ✓ | `loop`/`recur`, `while`, `for-each` |
| `LABELED_STATEMENT`, `BREAK`, `CONTINUE`, `RETURN`, `YIELD` | ✓ | elided, `label`, `break`, `continue`, `return` |
| `SWITCH`, `SWITCH_EXPRESSION`, `CASE`, case labels | ✓ | `switch` |
| `ANY_PATTERN`, `BINDING_PATTERN`, `DECONSTRUCTION_PATTERN` | ✓ | `_`, `^T x`, `(R p...)` |
| `THROW`, `TRY`, `CATCH`, `SYNCHRONIZED`, `ASSERT` | ✓ | `throw`, `try`, `with-resources`, `locking`, `java-assert` |
| names, calls, `new`, arrays, assignments, operators, casts, `instanceof` | ✓ | §5 forms and operators |
| `LAMBDA_EXPRESSION`, `MEMBER_REFERENCE` | ✓ | `lambda`, `method-ref` |
| literals, `PARENTHESIZED` | ✓ | Clojure literals; nothing |
| `ERRONEOUS`, `OTHER` | 0 | none |

Access flags and attributes (§8.2, §8.3) follow from the metadata written: modifiers as Java
wrote them, `^:deprecated` for the javadoc tag, annotations other than `SOURCE` ones,
`:throws`, `:default`, generic types for `Signature`. The class forms compiler derives the rest,
and the shape comparison above covers it for the baseline.

## Comparison with the worked examples (§11)

- **§11.1 `Keyword`**: identical apart from comments, and `find`'s
  `(if (some? ref) (cast Keyword (.get ref)) nil)` written `(when (some? ref) ...)`.
- **§11.2 `PersistentVector`**: `create(ISeq)` (with the `aset` before the increment),
  `tailoff`, `create(Iterable)`, `create(Object...)`, the constructors, `TRANSIENT_VECTOR_CONJ`,
  `Node`, `TransientVector`'s fields and constructors are identical. Differences:
  - modifiers in a fixed order (`^:public ^:final ^:transient`): javac's trees do not keep the
    order Java wrote them in;
  - `Node(edit)` writes `(set! (.-array this) ...)`: the Java says `this.array = ...` (the spec
    shortened it to `(set! array ...)`);
  - `arrayFor`: the spec folds the local `node` into the loop's bindings and the loop's exit into
    the `if`; the converter keeps `node` as a `^:mutable` local before a `loop` over `level`;
  - `rangedIterator`: `(when (< start ...) ...)` for `(if ... nil)`; `next` binds the old `i` as
    `i-1` where the spec says `j`;
  - `TransientVector.conj`: the spec computes `newroot` with an `if` as `let` initializer; the
    converter keeps Java's order, a `^:mutable` `newroot` assigned in both branches.
  None of these change the classes (the shapes compare equal).
- **§11.3** `Numbers.Category`: identical.
- **§11.4** (`test/j2c/java/sample`): identical apart from `rowOf`'s last test written as
  `(if (== ...) (return i) (recur ...))`, the `with-resources` binding untagged (`var` in Java),
  and `(when (< side 0.0) ...)`. All four files compile to javac's shapes.

## Proposed spec amendments

1. **§5.5, erasure casts.** "Where Java uses the value without a cast
   (`list.get(0).hashCode()` on `Object`'s method), javac inserts none" is not what javac does:
   `TransTypes` casts a qualifier to the erasure of its static type whatever member is selected
   (`checkcast String` then `String.hashCode()`, checked with javap), and an argument to the
   erasure of the *instantiated* parameter type (`m.add(l.get(0))` with `List<String>`s casts to
   `String`). Proposed: "The converter writes the casts javac's `TransTypes` inserts: qualifiers
   to the erasure of their static type, arguments to the erasure of the instantiated parameter
   types, values to the erasure of the variable or return type. A value used where its erased
   type suffices (`Object o = list.get(0)`) gets none."
2. **§1.5, readers.** Class-form files are read by `clojure.core/read` (Clojure 1.12 and later),
   not by `clojure.edn`: array class symbols (`int/1`) and param-tags (`^[int]`) are not EDN.
   Proposed: say so in principle 5.
3. **§4.5, §4.7, the receiver in initializers.** Field initializers and `initializer` bodies
   have no parameter vector, yet Java code there uses `this` (`this.x`, `this` as an argument,
   own methods). The converter writes `this`. Proposed: "In instance field initializers and
   `initializer` bodies `this` names the instance being initialized."
4. **§4.8, enclosing anonymous classes.** An anonymous class has no name for `Outer/this`. The
   converter names the receiver of an anonymous class's method that has nested classes `this1`
   (`this2` one level deeper) and nested classes use it: `(.pack this1)` for javac's
   `this$0.pack()`. This relies on the compiler treating an outer method's receiver as the
   enclosing instance (`this$0`), as COMPILER-NOTES says it does, not as a captured `val$`.
   Proposed: "A nested class reaches the instance of an enclosing anonymous class through the
   receiver parameter of the enclosing method, compiled as the enclosing instance."
   Related, for the same reason: a method reference to a method of an anonymous class is
   written with an unqualified `.m` (`(method-ref Predicate this .isMemoryManager)`), a pinned
   call on a receiver of anonymous type names the method's declaring class, and a static member
   inherited by an anonymous class is qualified with its declaring class (javac qualifies with the
   anonymous class). Where nothing else can name the class, the converter writes javac's binary
   name (`Type$4/dropMetadata` for a static method declared in an anonymous class, a lambda
   returning an anonymous class's type). In the JDK: 7 receivers named, 7 method references, 8
   statics, 6 pins, 5 binary names; in the baseline none.
5. **§5.6, pins on constructor calls.** There is no place for param-tags in `(super. ...)`,
   `(this. ...)`, `(.new o C ...)`, `anon` arguments and enum constant arguments. Proposed:
   param-tags on the head symbol, `(^[int] super. x)`, `(^[int] this. x)`, and on the argument
   vector of `anon` and enum constants, `(anon C ^[int] [x] ...)`, `(NAME ^[int] [x])`. The
   converter's conservative check flags 2 calls in the baseline (`this(421)` in
   `TransactionalHashMap`, `this(b, 0, b.length)` in `ClassReader`) and 134 in the JDK; the
   compiler resolves the baseline's ones as javac does.
6. **§5.8, list labels.** A label that is a list means several constants, so a single constant
   expression written as a list (`(unchecked-int 0x80000000)`) would be misread. The converter
   writes such labels as their values; proposed: say that labels are literals, constant symbols
   or lists of them, and that other constant expressions are written folded.
7. **§4.6, names that are not symbols.** javac's `List.nil()` is a method named `nil`, which
   Clojure reads as the literal. The converter writes the declared name qualified by its class,
   `(method ^:public ^:static List/nil ...)`, and calls as `(List/nil)`, which reads as a symbol;
   locals named `nil` become `nil_`. Proposed: allow `C/name` as the declared name of a member of
   `C`.
8. **§9.1, file names.** `clojure/main.java` (class `clojure.main`) and `clojure/main.clj` (the
   namespace `clojure.main`) would both be `clojure/main.clj`; `(load "clojure/main")` would
   also pick up `clojure/main__init.class`. The converter appends `_class` to a class file's name
   when the source directory has a `.clj` file of that name: `clojure/main_class.clj` (and
   `arbace/main_class.clj` after renaming). The package `clojure` gets the single-segment
   namespace `clojure`. Proposed: state the rule.
9. **§5.7, `for-each` over an `Iterable` with a primitive binding** (`for (int x : ints)`): the
   compiler casts each element to the wrapper (`Integer`) and unboxes, as javac does. Worth a
   sentence next to the cast rule.
10. **§5.6, array `clone()`.** javac follows `[I.clone()` with `checkcast [I`; with
    `(.clone a)` typed as the array, the compiler should emit that cast (it does).
11. **§5.5, casts.** Two javac details the converter follows: `(String) null` is a `checkcast`
    (`(cast String nil)`), and a cast to an array type keeps its `checkcast` even when it is an
    upcast (`(Object[]) strings`), while other upcasts are hints (`^Object x`) or nothing (on
    literals). An intersection cast checks only the bounds the operand is not known to have.
12. **§5.12, signature polymorphic calls**, as COMPILER-NOTES note 2 proposes: param-tags from
    the call site's argument types, the result type as a tag on the call (`^boolean` from a cast,
    `^void` as a statement). The converter writes them so.
13. **§5.4, all-literal conditionals in other numeric contexts.** `d + (z ? 1 : 2)` is an `int`
    conditional widened to `double` in Java (`iconst`, `i2d`); `(unchecked-add d (if z 1 2))`
    loads `long` constants. Behaviour is the same; the constants differ. Proposed for the
    compiler (extending its amendment 6): an `if` whose values are all integer literals that fit
    an `int` has type `int` when it is an operand of a primitive operator.
14. **§5.3, cross-case locals.** A local declared in one `switch` case and used in a later case
    is bound (`^:mutable`, default value) in a `let` around the `switch`, and assigned where Java
    declares it.
15. **§7.3, resolution.** The converter does not yet call the compiler's resolution, as §7.3
    says it should; it predicts it conservatively (534 pins in the baseline). With the compiler
    now resolving by JLS 15.12.2, the converter should use `arbace.classes` to decide pins.

## What remains

- Type annotations (`TYPE_ANNOTATION`, `ANNOTATED_TYPE`) are dropped (6 in the JDK).
- Pins: call the compiler's resolution (amendment 15); syntax for constructor-call pins
  (amendment 5).
- Comments are not carried over (§7 says "where easy").
- Restructurings the spec's examples show and the converter does not do: folding a local
  assigned in a loop into the loop's bindings, computing a later-assigned local with an `if`
  initializer when the branches do more than assign it.
- Local and anonymous classes declared in field initializers of anonymous classes cannot name
  that anonymous instance (no receiver parameter there); none in the corpus.
- Module declarations of the JDK cannot be attributed with `--patch-module`; they would need the
  module's own sources (`--module-source-path`).
- Equivalence of the JDK conversion has been checked only on a few packages (they compile to
  javac's shapes apart from java.base's inline string concatenation).
