# Java → class forms converter: implementation notes

The converter of agenda step 3: it turns Java sources into the class forms of [SPEC.md](SPEC.md),
making explicit what §7 lists. These notes say how it is built and used, what it was checked on,
how its output compares with the spec's worked examples (§11), and where converting showed that
the spec should change (amendments, accepted and folded into SPEC.md). The compiler side is in
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
| `arbace/j2c/resolve.clj` | `arbace.j2c.resolve` | asks the class forms compiler's overload resolution where pins are needed, over class infos made from javac's symbols |
| `bin/j2c` | | starts the frozen Clojure with access to javac's internals |
| `bin/j2c-check` | | the checks (below) |
| `test/j2c/java/sample/` | | Java constructs the baseline does not use (records, sealed types, patterns, switch expressions, lambdas, method references, local and anonymous classes, annotation types), including §11.4; `Pins.java`: overloads that need pins and that must not get them, constructor calls of every kind, `o.new Inner(...) {...}` |
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
3. **Overloads.** Pins are decided by the class forms compiler's own resolution (SPEC §5.6,
   §7.3; `arbace.j2c.resolve`): for every call, constructor call and method reference the
   converter asks `arbace.classes.analyze/select-method`, with the compiler's candidates
   (`instance-candidates`, `static-candidates`, `ctor-candidates`), what the compiler will
   choose for the call as written without param-tags, and pins only where that is not javac's
   method in javac's arity mode, or where the compiler would find none or an ambiguity. The
   arguments are stubs with the types the converted forms have for the compiler: an integer
   literal (or a conditional of them) is a `long` literal node with its values, so the
   literal narrowing phase sees them. The class infos come from javac's symbols (erased
   descriptors, access flags, supertypes, nest), so the classes being converted need not be
   loadable and resolution sees what javac saw. Pins go on the head (`(^[int] Math/max a 1)`,
   `(^[int] this. 5)`, `(^[int] .super o 7)`, `(^[int] .new o In 8)`) or on the argument vector
   (`(anon C ^[int] [9] ...)`, `(A ^[int] [1])` for enum constants). Method references to
   `super::m`, on array types and on anonymous classes keep the older rule: pinned when the
   name is overloaded.
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
- **Pins** (2026-10-07, counted by `:form/param-tags`, `:pin/*` in the reports): deciding them
  with the compiler's resolution instead of the conservative prediction takes the baseline from
  534 param-tags to 4 (two `Math/max` with a literal, `List/.remove` with a literal,
  `GeneratorAdapter/.push` with an `int` conditional; plus the signature polymorphic call,
  which always has them), with every class still shape-identical to javac and the test suite
  unchanged; jdk26u (12,730 files) from 14,182 to 1,440 (236 instance calls, 543 static calls,
  658 constructor calls of 664,014 decided). Before, 134 constructor calls (`this.`, `super.`)
  and one qualified `new` that needed pins had none (there was no syntax), and 6 qualified
  anonymous classes lost their outer instance; they are now pinned or written with `:outer`.
  Compiling the converted JDK: 11,753 of 12,444 files shape-identical (11,745 with the earlier
  output), no file regressing.
- **Clojure's test suite** (`bin/clojure-tests`) on those 812 classes, compiled from the
  converted forms, with the baseline's `.clj` sources: 83 namespaces, 809 tests, 20,718 of
  20,750 assertions pass and 27 of 27 test.generative specs pass, the same as the baseline (the
  32 failures expect spec's messages). No regressions.
- `test/j2c/java/sample`: all 6 files shape-identical to javac, §11.4 included (2026-10-07;
  before, `Features` differed in the constants of `d + (z ? 1 : 2)`, a serializable lambda's
  name and record pattern merging, all fixed in the compiler since; `Pins` was added).
- A sample of the JDK conversion compiled the same way (`javax/security/auth/x500`,
  `sun/util/calendar`, `jdk/internal/util`): the differences are javac's string concatenation
  with `StringBuilder` (java.base is built with `-XDstringConcat=inline`), one array upcast
  (fixed since: javac keeps `checkcast` for casts to array types), and a compiler error on a type
  variable of a method used inside an anonymous class (`Preconditions`, for the compiler).
- `clojure.edn/read` reads none of the files that use Clojure 1.12 syntax: array class
  symbols (`int/1`) and param-tags (`^[int]`) are not EDN (amendment 2).

`bin/j2c-check` reruns the baseline, compiler and sample checks (one minute); with `--suite` it
also builds the converted baseline and runs Clojure's test suite on it.

### The converted JDK

`bin/j2c-check --jdk [MODULE...]` (about 10 minutes on 64 cores; not part of the default
checks) converts every module of jdk26u's `src/*/share/classes` with `bin/j2c ... jdk`,
compiles the same Java files twice with javac (`--patch-module` against the running JDK, which
is built from the same sources, with the options of the JDK build for the module:
`-XDstringConcat=inline` for `java.base`, `jdk.compiler`, `jdk.jfr`, `jdk.jartool`,
`jdk.internal.vm.ci`, and `-parameters` for the latter), compiles the converted forms with the
class forms compiler in parallel chunks (`bin/class-forms-check` with `REF`, `FILES`, and the
converted module on the class path for classes found nowhere else) and compares every class's
shape with javac's (SPEC §3). Classes whose two javac runs differ are not compared (2 do);
classes javac made from a Java file (by `SourceFile`) that the forms did not make count as
differences; `defpackage` forms are compiled and compared too. The report,
`.tmp/j2c-jdk/report.md`, counts per module the Java files converted, the converted files with
class forms compiled without error, those whose classes are all shape-identical, those with
differences, and the compile errors, and lists differences and errors by kind with an example.

Results (2026-10-07; "before" is the first run, with the compiler and converter of
`origin/main` at `f425bc8`, where one chunk of 60 `java.base` files did not finish because
`java.lang.Object` sent the compiler into a loop; "after" is `bin/j2c-check --jdk` at the end of
the work). Files are converted Java files with class forms; 286 have none (no annotations in a
`package-info.java`):

| module | Java files | with class forms | identical before | identical after | differing before | differing after | errors before | errors after |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| java.base | 3,088 | 3,026 | 2,755 | 3,002 | 144 | 23 | 67 | 1 |
| java.desktop | 2,287 | 2,235 | 2,168 | 2,223 | 53 | 12 | 14 | 0 |
| java.xml | 1,854 | 1,825 | 1,792 | 1,823 | 27 | 2 | 6 | 0 |
| jdk.hotspot.agent | 837 | 837 | 828 | 836 | 7 | 1 | 2 | 0 |
| jdk.compiler | 347 | 342 | 289 | 331 | 28 | 10 | 25 | 1 |
| java.management | 327 | 318 | 308 | 317 | 7 | 1 | 3 | 0 |
| jdk.jfr | 321 | 318 | 301 | 318 | 17 | 0 | 0 | 0 |
| java.net.http | 312 | 308 | 259 | 308 | 31 | 0 | 18 | 0 |
| java.xml.crypto | 271 | 265 | 263 | 265 | 2 | 0 | 0 | 0 |
| jdk.jdi | 248 | 243 | 200 | 243 | 41 | 0 | 2 | 0 |
| jdk.internal.vm.ci | 213 | 199 | 193 | 199 | 6 | 0 | 0 | 0 |
| java.security.jgss | 210 | 208 | 206 | 208 | 2 | 0 | 0 | 0 |
| java.naming | 203 | 197 | 194 | 197 | 1 | 0 | 2 | 0 |
| jdk.javadoc | 195 | 184 | 166 | 180 | 16 | 4 | 2 | 0 |
| jdk.jpackage | 145 | 144 | 113 | 141 | 25 | 2 | 6 | 1 |
| jdk.internal.md | 144 | 135 | 128 | 135 | 7 | 0 | 0 | 0 |
| java.compiler | 135 | 129 | 119 | 129 | 10 | 0 | 0 | 0 |
| jdk.internal.le | 117 | 112 | 100 | 112 | 9 | 0 | 3 | 0 |
| java.rmi | 104 | 99 | 95 | 99 | 2 | 0 | 2 | 0 |
| jdk.jshell | 92 | 88 | 75 | 84 | 8 | 4 | 5 | 0 |
| jdk.jlink | 81 | 81 | 73 | 80 | 7 | 1 | 1 | 0 |
| jdk.crypto.cryptoki | 81 | 81 | 76 | 80 | 3 | 1 | 2 | 0 |
| jdk.dynalink | 66 | 61 | 60 | 60 | 1 | 1 | 0 | 0 |
| jdk.jdeps | 64 | 63 | 54 | 62 | 6 | 1 | 3 | 0 |
| jdk.jconsole | 63 | 62 | 58 | 60 | 3 | 2 | 1 | 0 |
| 43 other modules | 925 | 884 | 826 | 884 | 49 | 0 | 9 | 0 |
| **all 68** | 12,730 | 12,444 | 11,699 | 12,376 | 512 | 65 | 173 | 3 |

All 12,730 files convert; 24,127 classes compile from the forms. What differs or fails at the
end (68 files), by cause:
- **Code duplicated by the forms.** `switch` fall-through repeats the following arms' code
  (SPEC §12 question 9), so a lambda or anonymous class there is made twice (`JavacParser`,
  `GraphUtils`, `PrintingProcessor`, `DeferredAttr`: missing and extra lambda methods and
  classes).
- **javac's attribution order.** javac numbers anonymous classes in an argument of a chained
  call before those of its qualifier (arguments are attributed first); the forms number them
  in textual order (`StringConcatFactory`).
- **Stack map frames.** javac's frames name the common superclass of merged types, ASM's
  sometimes another class; classes named only there give `InnerClasses` differences (15 files).
- **Constants of instance fields read as `this.k` in Java.** javac null-checks `this`; the
  forms write an inherited constant read by simple name the same way, `(.-k this)`, so the
  compiler cannot tell them apart and checks neither.
- **Smaller ones**, one or two files each: a lambda's `throws` (`ThrowsTaglet`), widening of
  some constants (`Math.nextAfter`, metal look and feel `double` constants), a `switch` on an
  interface type with qualified enum constants as labels (`PackageBuilder`; javac's
  `typeSwitch` with `EnumDesc` labels, not supported), an anonymous class in a constructor
  prologue whose methods use the enclosing instance of the class being constructed (`Attr`),
  and a call passing `T[]` with `T extends Object & Comparable` where javac relies on the
  verifier's leniency for interface arrays (`ModuleDescriptor`).

## Coverage

Tree kinds met in the whole corpus (baseline, jdk26u, samples) and what they become. Every kind
met was converted without failure. Generated by `arbace.j2c.coverage` (rounded counts).

| tree kinds | met | written as |
|---|---|---|
| `COMPILATION_UNIT`, `PACKAGE`, `IMPORT` | all files | file with `in-ns`, `import`; package file with `ns`, `load`; `defpackage` |
| `MODULE`, `REQUIRES`, `EXPORTS`, `OPENS`, `USES`, `PROVIDES` | test module | `defmodule` |
| `CLASS`, `INTERFACE`, `ENUM`, `RECORD`, `ANNOTATION_TYPE` | ✓ | `defclass` with kind metadata, `anon`, `letclass`, `constants` |
| `METHOD`, `VARIABLE`, `BLOCK`, `MODIFIERS`, `ANNOTATION`, `TYPE_PARAMETER` | ✓ | `method`, `constructor`, `field`, bindings, metadata, `:type-params` |
| `TYPE_ANNOTATION`, `ANNOTATED_TYPE` | ✓ | metadata on the type forms' nodes; in code on the operand of `cast`, `instance?`, `new`, `catch`; `^{:type-args [...]}`, `^{:qualifier T}` on method symbols (`test/j2c/java/sample/TypeAnns.java`) |
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

## Spec amendments (accepted)

All accepted by the user and folded into SPEC.md; the original texts are in git history
(commit 7229dd9).

1. **§5.5, erasure casts as `TransTypes` inserts them.** Accepted (2026-10-07), folded into SPEC §5.5, §7.4.
2. **§1.5, readers: `clojure.core/read`, not EDN.** Accepted (2026-10-07), folded into SPEC §1.5.
3. **§4.5, §4.7, the receiver in initializers.** Accepted (2026-10-07), folded into SPEC §4.5, §4.7, §5.2.
4. **§4.8, enclosing anonymous classes.** Accepted (2026-10-07), folded into SPEC §4.8, §7.1.
5. **§5.6, pins on constructor calls.** Accepted (2026-10-07), folded into SPEC §4.7, §4.8, §4.10, §5.6.
6. **§5.8, list labels.** Accepted (2026-10-07), folded into SPEC §5.8, §7.5.
7. **§4.6, names that are not symbols (`C/name`).** Accepted (2026-10-07), folded into SPEC §4.6.
8. **§9.1, file names (`_class`).** Accepted (2026-10-07), folded into SPEC §9.1.
9. **§5.7, `for-each` over an `Iterable` with a primitive binding.** Accepted (2026-10-07), folded into SPEC §5.7.
10. **§5.6, array `clone()`.** Accepted (2026-10-07), folded into SPEC §5.6.
11. **§5.5, casts of `null`, to array types, intersection casts.** Accepted (2026-10-07), folded into SPEC §5.5, §7.4.
12. **§5.12, signature polymorphic calls.** Accepted (2026-10-07), folded into SPEC §5.6.
13. **§5.4, all-literal conditionals in other numeric contexts.** Accepted (2026-10-07), folded into SPEC §5.4.
14. **§5.3, cross-case locals.** Accepted (2026-10-07), folded into SPEC §5.3, §7.10.
15. **§7.3, resolution.** Accepted (2026-10-07), folded into SPEC §5.6, §7.3.

## Spec amendments (proposed, 2026-10-07)

Found while compiling the converted JDK; already in SPEC.md, marked "amendment, 2026-10-07",
for the user's approval:

16. **§4.8, `^:diamond`.** javac gives the constructor of an anonymous class of an interface
    that captures locals a `Signature`, except when Java wrote the diamond; the forms say so
    with `^:diamond` on the supertype.
17. **§5.12, `^:method-ref` on an array constructor reference's lambda.** javac names the
    lambda it makes of `T[]::new` as a reference (after the field in a field initializer),
    not as an explicit lambda.
18. **§4.11, `&` in a record's components.** A variable arity record's canonical constructor
    is `ACC_VARARGS`.
19. **§4.4, type annotations in code.** Generic type operands (`(new (C ^{A true} T))`,
    `(new (array ^{A true} T) n)`, `(cast (C ^{A true} T) x)`), annotated `catch`
    alternatives, `^{:type-args [...]}` also on method references, and `^{:qualifier T}` for
    the qualifying type of a method reference.

## What remains

- Method references to `super::m`, on array types and on anonymous classes are still pinned
  whenever the name is overloaded (the compiler turns `super::m` into a lambda whose call
  drops the pin, so it must not rely on it either way).
- Variable arity arguments are packed into an explicit array unless the method is the only
  one of its name and the array's element type is the erased parameter's; the compiler's
  resolution could decide this too.
- Comments are not carried over (§7 says "where easy").
- Restructurings the spec's examples show and the converter does not do: folding a local
  assigned in a loop into the loop's bindings, computing a later-assigned local with an `if`
  initializer when the branches do more than assign it.
- Local and anonymous classes declared in field initializers of anonymous classes cannot name
  that anonymous instance (no receiver parameter there); none in the corpus.
- Module declarations of the JDK are not converted (`--patch-module` cannot take them). An
  experiment showed the way: javac attributes a module's `module-info.java` with
  `--module-source-path` naming only that module's sources (the others come from the system
  image), so `bin/j2c-check --jdk` could convert and compare them with `defmodule`; not done.
- The JDK differences listed in "The converted JDK" above.
