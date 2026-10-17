# Java → Java forms converter: implementation notes

> Names: the converter is repr/java (`arbace/repr/java.ae`, `arbace/repr/java/`, `bin/repr-java`,
> `bin/repr-java check`); from 2026-10-11 to 2026-10-17 it was j2a (`arbace/j2a/`, `bin/j2a`,
> `bin/j2a-check`), and before that j2c, as the frozen branches and the records before that date name it, and its renaming
> rules `arbace.j2c.rename`, since moved into c2a ([../C2A.md](../C2A.md)). This document uses
> the current names, but where it records the derivation, replayed with the old ones.

The converter of agenda step 3 (repr/java's `read`; its `print`, Java forms back to Java, is
[PRINTER-NOTES.md](PRINTER-NOTES.md)): it turns Java sources into the Java forms of [SPEC.md](SPEC.md),
making explicit what §7 lists. These notes say how it is built and used, what it was checked on,
how its output compares with the spec's worked examples (§11), and where converting showed that
the spec should change (amendments, accepted and folded into SPEC.md). The compiler side is in
[COMPILER-NOTES.md](COMPILER-NOTES.md).

## Layout

| file | namespace | role |
|---|---|---|
| `arbace/repr/java/javac.ae` | `arbace.repr.java.javac` | runs the JDK's javac (`JavacTask.parse` + `analyze`: attribution and flow, no desugaring) |
| `arbace/repr/java/jtypes.ae` | `arbace.repr.java.jtypes` | javac types and symbols: erasure, boxing, widening, flags, binary names, renaming |
| `arbace/repr/java/forms.ae` | `arbace.repr.java.forms` | output data: forms, class references named later (`CRef`), raw tokens, ordered metadata |
| `arbace/repr/java/convert.ae` | `arbace.repr.java.convert` | the conversion of attributed trees: declarations, code, control flow, patterns, switches, lambdas |
| `arbace/repr/java/names.ae` | `arbace.repr.java.names` | one namespace per package: which class gets which simple name, imports, file layout |
| `arbace/repr/java/layout.ae` | `arbace.repr.java.layout` | writing the forms as `.ae` text, with metadata and a Clojure-style layout (until 2026-10-17 `print.ae`, `arbace.repr.java.print`: renamed by R1, doc/NAMING-SURVEY.md, for the printer) |
| `arbace/repr/java.ae` | `arbace.repr.java` | command line: `convert`, `jdk` |
| `arbace/repr/java/coverage.ae` | `arbace.repr.java.coverage` | coverage summary from run reports |
| `arbace/repr/java/resolve.ae` | `arbace.repr.java.resolve` | asks the Java forms compiler's overload resolution where pins are needed, over class infos made from javac's symbols |
| `bin/repr-java` | | starts the frozen JVM Arbace (`bin/lib/frozen.bash`) with access to javac's internals, repr/java and the Java forms' front end loaded from the checkout's sources |
| `bin/repr-java check` | | the checks (below) |
| `test/repr/java/expected/` | | the expected output of the checks: `java/` and `module/` (the samples' forms), `baseline.sha256` (the baseline's, by hash) |
| `test/repr/java/build.ae` | `repr.java.build` | compiles converted files with the frozen jar's own Java forms compiler into class files (`bin/repr-java check --suite`; until S4 `arbace.repr.java.build`) |
| `test/repr/java/java/sample/` | | Java constructs the baseline does not use (records, sealed types, patterns, switch expressions, lambdas, method references, local and anonymous classes, annotation types), including §11.4; `Pins.java`: overloads that need pins and that must not get them, constructor calls of every kind, `o.new Inner(...) {...}` |
| `test/repr/java/module/` | | a module declaration and package annotations |

The converted files are not kept in the repository: they are regenerated (the user's decision);
what the checks expect of them is (`test/repr/java/expected/`, since S4), and so are the converted
JDK closure (`jdk/forms`, S1) and the suite's Java fixtures (`test/java-fixtures/`).

Where the converter runs. Since standalone S4 (doc/STANDALONE-NOTES.md, "S4: the JVM removed",
open point O1 (a)): on the frozen JVM Arbace, the jar of `arbace-for-java-26-v4` built from its
tag and pinned by hash (`bin/lib/frozen.bash`, `test/oracle/frozen-jar.sha256`), since main has
no JVM build any more and javac, repr/java's front end, needs a JVM. The jar's loader reads `.clj`
only and loads a namespace from its source when the source is newer than its `__init` class (the
jar's entries are dated 2000-01-01): `frozen_view tools` puts `.clj` links to the checkout's
`arbace.repr.java`, the Java forms' front end (`arbace.classes.types`, `env`, `parse`, `lower`,
`analyze`: `arbace.repr.java.resolve` asks its overload resolution) and every namespace the jar lacks
before the jar on the class path, so those load from the checkout's sources and the rest
(`arbace.core`, the runtime, the jar's own back end) is the jar's. The converter's output is the
same as on main's stage 2 (`jdk/forms`, `test/java-fixtures` and the samples, byte for byte).
From the freeze to S4 it ran on `target/stage2`, before the freeze on the frozen Clojure. Its
output names the running core: a head shadowed by a local or field is qualified `arbace.core/`
(before, `clojure.core/` for Clojure's own names). The baseline `clojure/` is no longer in
main's tree: `bin/repr-java check` extracts it from the tag `arbace-for-java-26-v1` into
`.tmp/frozen/` once (`frozen_clojure` in `bin/lib/tools.bash`).

## Use

```
bin/repr-java read [--rename FROM=TO] [--javac OPT]... [--check] OUT ROOT [PATH...]
bin/repr-java read-jdk [--check] [--origins FILE] OUT MODULE MODULE-SRC [PATH...]
bin/repr-java coverage REPORT.edn...
bin/repr-java check [--suite | --update]
bin/repr-java check --jdk [MODULE...]
```

- `convert` converts the `.java` files under `ROOT` (or under the `PATH`s) into `OUT`, with
  `ROOT` as javac's source path. `bin/repr-java read .tmp/repr-java/clojure-out
  .tmp/frozen clojure` converts the baseline (extracted by `bin/repr-java check`); put the output
  under `.tmp/`.
- `--rename clojure=arbace` renames the package prefix in every class name, namespace and path
  (agenda step 4). String literals are left alone (`"clojure.core"` in `RT` stays); step 4
  renamed them afterwards with `arbace.j2c.rename` (now `arbace.c2a.rename`) (`doc/VENDOR-NOTES.md`; `bin/vendor-arbace`
  on the branch `arbace-for-java-26`).
- `--check` reads every output file back with the core's `read` and the EDN reader (on Arbace
  `arbace.core/read`, `arbace.edn/read`), and
  writes `OUT/repr-java-report.edn`: failures, unreadable files, counters of tree kinds met and of the
  forms and conversions written.
- `jdk` converts a JDK module's sources against the running JDK's own classes: the files are
  copied into `--patch-module` directories under `.tmp/repr-java-patch/`, in chunks of packages
  converted in parallel (`REPR_JAVA_THREADS`, default 12). A `module-info.java` cannot be patched in
  and is skipped (modules are converted from ordinary source trees, `test/repr/java/module`). Each
  output file's header (`;; Converted from ...`) names the original file under `MODULE-SRC`,
  not its copy, whose numbered directory depends on the chunking; `--origins FILE` (a map
  `{"path/below/MODULE-SRC.java" "name"}`, read with `read`) names them instead, so that the
  output does not depend on where the tree was made (`bin/jrt-convert` names each by its
  jdk26u path, 2026-10-11; before, the headers named the copy, `.tmp/repr-java-patch/java.base-N`).
- `bin/jrt-convert` (doc/go/JRT-SOURCES.md) runs `jdk --check` on java.base's translation
  closure for the Go build's runtime jrt, then compares the output with the committed forms
  (`jdk/forms`; until S4 it compiled them with `bin/class-forms-check` and compared the classes
  with javac's).
- `test/repr/java/build.ae` (`repr.java.build`, run by `bin/repr-java check --suite` on the frozen jar with the
  jar's own `arbace.classes`; until S4 `bin/repr-java -m arbace.repr.java.build`, the checkout's compiler)
  compiles converted files with the Java forms compiler and writes the class files. Classes it
  does not compile are looked up on the class path (`FROZEN_CP`: `bin/repr-java check` passes the
  frozen tree's javac classes).

Output layout (§9.1): for package `p.q`, `p/q.ae` holds `(ns p.q)` and a `(load "q/File")` per
Java file, supertypes in the same package first; `p/q/File.ae` holds `(in-ns 'p.q)`, an
`(import ...)` of the classes it uses, and the file's Java forms. A comment line names the Java
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
3. **Overloads.** Pins are decided by the Java forms compiler's own resolution (SPEC §5.6,
   §7.3; `arbace.repr.java.resolve`): for every call, constructor call and method reference the
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

On 2026-10-06 (the table) and 2026-10-07 (as dated below), jdk26u at the system JDK (26.0.2.1).

| corpus | files | converted | failures | unreadable (the core's `read`) |
|---|---:|---:|---:|---:|
| baseline `clojure/` | 183 | 183 | 0 | 0 |
| jdk26u `src/*/share/classes`, all 68 modules (module-infos skipped) | 12,730 | 12,730 | 0 | 0 |
| `test/repr/java/java`, `test/repr/java/module` (14 files since: `Pins`, `TypeAnns`) | 9 | 9 | 0 | 0 |

- **Compiled with the Java forms compiler** (`bin/class-forms-check`, compiler at `origin/main`
  of the same day): every one of the 183 converted baseline files with Java forms compiles, and
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
- `test/repr/java/java/sample`: all 6 files shape-identical to javac, §11.4 included (2026-10-07;
  before, `Features` differed in the constants of `d + (z ? 1 : 2)`, a serializable lambda's
  name and record pattern merging, all fixed in the compiler since; `Pins` was added). Later
  `TypeAnns.java` and its annotation types `TypeUseA` to `TypeUseC` were added (10 files, all
  shape-identical; `bin/repr-java check`).
- A sample of the JDK conversion compiled the same way (`javax/security/auth/x500`,
  `sun/util/calendar`, `jdk/internal/util`): the differences are javac's string concatenation
  with `StringBuilder` (java.base is built with `-XDstringConcat=inline`), one array upcast
  (fixed since: javac keeps `checkcast` for casts to array types), and a compiler error on a type
  variable of a method used inside an anonymous class (`Preconditions`, for the compiler).
- `clojure.edn/read` reads none of the files that use Clojure 1.12 syntax: array class
  symbols (`int/1`) and param-tags (`^[int]`) are not EDN (amendment 2).

Until standalone S4, `bin/repr-java check` reran the baseline, compiler and sample checks above (one
minute), the compiler's against javac's classes; with `--suite` it also built the converted
baseline and ran Clojure's test suite on it.

### The checks since S4: forms-level

Main has no Java forms back end since S4 (it compiled to JVM bytecode, which only the JVM
build used), so repr/java's checks no longer compile its output and compare the classes with javac's
(doc/STANDALONE-NOTES.md, "S4: the JVM removed", open point O2: (b) forms-level, with (a), the
frozen jar's own compiler, for the suite). `bin/repr-java check`:

1. converts the baseline (183 files) with `--check` (no failures, every file read back) and
   compares the converted files with `test/repr/java/expected/baseline.sha256`, their SHA-256 by path
   (the forms are 2.2 MB; the suite of step 4 is their semantic check, and a change shows as the
   files whose hash moved, to diff against a conversion made before it);
2. converts `test/repr/java/java` and compares the output with `test/repr/java/expected/java/`, byte for
   byte (the samples' forms are small and are what a change to repr/java should be reviewed by);
3. the same for `test/repr/java/module` and `test/repr/java/expected/module/`;
4. with `--suite`: compiles the converted baseline with the frozen jar's own Java forms
   compiler (`test/repr/java/build.ae`, on the jar alone: its `arbace.classes` at the freeze, which
   compiled these forms to javac's shapes) and runs Clojure's suite on the classes against
   `test/baseline-results.edn` (no regressions).

The first line of each converted file names its source; the checkout's path is left out of it
(`clojure/lang/RT.java`, `test/repr/java/java/sample/Shapes.java`), so that the expected files do not
depend on where the check runs. `bin/repr-java check --update` rewrites `test/repr/java/expected/` after a
change to repr/java or the samples (commit it with the change). What the converted forms mean is
checked downstream as well: j2g compiles `jdk/forms` (the JDK closure) and the suite's fixtures
through the Java forms' front end into the Go build, and the oracle and Clojure's suite run it.

### The converted JDK

Since S4 `bin/repr-java check --jdk [MODULE...]` only converts the modules (`--check`: no failures,
every file read back; `test/repr/java/jdk.bash`, two conversions at a time, `JDK_JOBS`); the
compilation and comparison with javac below went with the back end.

Until S4, `bin/repr-java check --jdk [MODULE...]` (about 10 minutes on 64 cores; not part of the default
checks) converted every module of jdk26u's `src/*/share/classes` with `bin/repr-java ... jdk`,
compiles the same Java files twice with javac (`--patch-module` against the running JDK, which
is built from the same sources, with the options of the JDK build for the module:
`-XDstringConcat=inline` for `java.base`, `jdk.compiler`, `jdk.jfr`, `jdk.jartool`,
`jdk.internal.vm.ci`, and `-parameters` for the latter), compiles the converted forms with the
Java forms compiler in parallel chunks (`bin/class-forms-check` with `REF`, `FILES`, and the
converted module on the class path for classes found nowhere else) and compares every class's
shape with javac's (SPEC §3). Classes whose two javac runs differ are not compared (2 do);
classes javac made from a Java file (by `SourceFile`) that the forms did not make count as
differences; `defpackage` forms are compiled and compared too. The report,
`.tmp/repr-java-jdk/report.md`, counts per module the Java files converted, the converted files with
Java forms compiled without error, those whose classes are all shape-identical, those with
differences, and the compile errors, and lists differences and errors by kind with an example.

Results (2026-10-07; "before" is the first run, with the compiler and converter of
`origin/main` at `f425bc8`, where one chunk of 60 `java.base` files did not finish because
`java.lang.Object` sent the compiler into a loop; "after" is `bin/repr-java check --jdk` at the end of
the work). Files are converted Java files with Java forms; 286 have none (no annotations in a
`package-info.java`):

| module | Java files | with Java forms | identical before | identical after | differing before | differing after | errors before | errors after |
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
met was converted without failure. Generated by `arbace.repr.java.coverage` (rounded counts).

| tree kinds | met | written as |
|---|---|---|
| `COMPILATION_UNIT`, `PACKAGE`, `IMPORT` | all files | file with `in-ns`, `import`; package file with `ns`, `load`; `defpackage` |
| `MODULE`, `REQUIRES`, `EXPORTS`, `OPENS`, `USES`, `PROVIDES` | test module | `defmodule` |
| `CLASS`, `INTERFACE`, `ENUM`, `RECORD`, `ANNOTATION_TYPE` | ✓ | `defclass` with kind metadata, `anon`, `letclass`, `constants` |
| `METHOD`, `VARIABLE`, `BLOCK`, `MODIFIERS`, `ANNOTATION`, `TYPE_PARAMETER` | ✓ | `method`, `constructor`, `field`, bindings, metadata, `:type-params` |
| `TYPE_ANNOTATION`, `ANNOTATED_TYPE` | ✓ | metadata on the type forms' nodes; in code on the operand of `cast`, `instance?`, `new`, `catch`; `^{:type-args [...]}`, `^{:qualifier T}` on method symbols (`test/repr/java/java/sample/TypeAnns.java`) |
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
`:throws`, `:default`, generic types for `Signature`. The Java forms compiler derives the rest,
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
- **§11.4** (`test/repr/java/java/sample`): identical apart from `rowOf`'s last test written as
  `(if (== ...) (return i) (recur ...))`, the `with-resources` binding untagged (`var` in Java),
  and `(when (< side 0.0) ...)`. All four files compile to javac's shapes.

## Spec amendments (accepted)

All accepted by the user and folded into SPEC.md; the original texts are in git history
(commit `7229dd9`).

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

## Spec amendments 16-19 (accepted, 2026-10-07)

Found while compiling the converted JDK. The user accepted all four on 2026-10-07; in SPEC.md
they are marked "amendment, accepted 2026-10-07".

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
  image), so `bin/repr-java check --jdk` could convert and compare them with `defmodule`; not done.
- The JDK differences listed in "The converted JDK" above.
