# An Arbace-native Java front end — survey and plan

Status: survey, 2026-10-20, at the user's request ("a survey and plan for an Arbace-native Java
front end, so that repr/java reads Java without javac and without the JVM"; a plan only, nothing
built). Nothing is adopted here; the decisions (§9, JFS1-JFS11) are the user's. Measured on main
at `9a6a36e9`: the committed `jdk/forms` (911 files), the conversion reports of the last
`bin/gate --full` run in main's `.tmp/` (2026-10-20: `.tmp/jrt/conv/repr-java-report.edn`,
`.tmp/repr-java-check/{baseline,java,module}/repr-java-report.edn`), the step times in
`.tmp/gate/*.secs`, jdk26u at `baf63fbe42b8` (`/root/jdk26u`, its build tree included) and three
prior-art clones (§3). The measuring scripts and the clones are in the survey's scratch
(`.tmp/jfs/`, `.tmp/prior/`, not tracked); Appendix A says what each computes. No JVM was run for
this survey and no Go build was made. Decided on 2026-10-20 ("Decided", at the end); JN1, the
spike, as built: "As built: JN1 (2026-10-20)", after it.

Names. The milestones are numbered **JF1-JF9**, as the request named them, and the proposals
**JFS1-JFS11**. JF1-JF5 are also the names of repr/java's printer choices of 2026-10-18
([PRINTER-NOTES.md](PRINTER-NOTES.md), "Choices for the user (JF1-JF5)", decided); in this
document JFn always means its own milestones, and JFS11 asks whether to rename them (JN, unused
in the repository, is free). JFS is unused.

The words of [SPEC.md](SPEC.md) apply: the **converter** is repr/java's `read`, the **Java
forms' front end** is `arbace.classes` (parse, lower, analyze, env, types), **attribution** is
what javac's `Enter`, `TypeEnter`, `MemberEnter`, `Attr`, `Resolve`, `Infer`, `DeferredAttr`,
`Flow` and `ConstFold` compute over the parsed tree (symbols, types, chosen members, constants,
inferred types, reachability).

## Executive summary

- **Today (§1).** `bin/repr-java read` runs on the frozen JVM (the jar of
  `arbace-for-java-26-v4` on JDK 26.0.2.1 built from jdk26u `baf63fb`, `bin/lib/frozen.bash`).
  `arbace.repr.java.javac` calls `JavacTask.parse` and `analyze` (attribution and flow, no
  lowering); `convert.ae` (3,203 lines) walks javac's attributed `JCTree`s; `resolve.ae` asks
  `arbace.classes.analyze`'s overload resolution, over class infos made from javac's symbols,
  where a call needs param-tags. Through repr/java the JVM is needed by `bin/jrt-convert`
  (`convert`; its `javac` step and its generators use a JVM of their own), the Java test fixtures
  (`bin/clojure-tests --java-fixtures`), and repr/java's checks and round trips in
  `bin/gate --full` (`repr-java` 328 s, `repr-java-roundtrip` 735 s, `jrt-convert` 151 s,
  `java-fixtures` 11 s in the last run). `bin/j2g-edge` and the oracle need the JVM for reasons
  of their own (§1.3).
- **The key question, measured (§2): a native front end is not mostly a parser.** Of
  `convert.ae`'s 2,824 lines of code, 2,188 (77%, 133 of its 242 top-level forms) are in
  functions that read javac's attribution. Of the 240,546 lists in `jdk/forms`, 25.0% have a
  head chosen by name and member resolution (`.m` or `C/m`, `.-f`, `C/f`) and 14.4% one chosen
  by expression types (the typed operators, `==`/`identical?`/`=`, `java-str`, `cast`, the
  narrowing and widening conversions); the baseline's 60,902 lists 34.9% and 11.5%. Only
  60.6% (jdk) and 53.6% (baseline) follow from the syntax and reachability alone. Of the typed
  share, what arbace.classes does not compute by design (SPEC §1 principle 4: no generic types in
  code) is small in count and decisive for equality: in java.base's closure 2,929 inserted
  conversions (2,204 casts, 417 hints, 156 unboxings, 143 boxings, 9 float promotions), 642
  lambdas and method references typed by their inferred targets, and the 164 pinned calls of
  56,773 overload decisions (99.7% of the decisions arbace.classes' erased resolution already
  makes as javac does). So the front end needs javac's attribution, generics and inference
  included; arbace.classes' environment, erased resolution, constant folding and derivations
  cover perhaps a third of it.
- **Prior art (§3).** javac (jdk26u's `jdk.compiler`, 348 files, 162,441 lines; what its
  attribution takes, about 47,000 of them), ECJ (241,716 lines in
  `org.eclipse.jdt.core.compiler.batch`, EPL-2.0), JavaParser with its symbol solver (99,905 +
  32,375 lines, Apache-2.0 or LGPL-3.0; parses up to Java 26, a partial attribution), and
  tree-sitter-java (a grammar of 1,338 lines generating a 76,760-line C parser, MIT; syntax only).
  Only javac gives javac's answers, and only javac gives javac's class files, which the round
  trip's classes level compares byte for byte.
- **Options (§4).** (a) an Arbace parser and attribution, about 47,000 lines of javac's rules
  to reproduce exactly: 30 to 50 working days, an open tail (every JDK release moves inference:
  jdk26u's `CAPTURE_MREF_RETURN_TYPE`); (b) **javac itself as Java forms, run by Arbace**:
  converted by repr/java (it did so on 2026-10-07: 342 of jdk.compiler's 347 files with forms,
  331 compiled to javac's shapes), translated by j2g into a tool executable like the test
  build, with `convert.ae` unchanged on top: about 13 to 20 working days, javac's fidelity by
  construction, the classes level kept; jrt lacks 67 of the 238 java.base classes javac names
  (file systems, the module system, jar and zip, annotation reflection), most avoidable by
  options; (c) ECJ the same way: neither javac's attribution nor its classes, and a new license;
  (d) a stock JVM behind a process boundary (O1 (b) of S4), JavaParser, or tree-sitter.
- **Recommendation (§7, §9).** (b) first, as STANDALONE-SURVEY's D12 (b) already recommended
  "as a later stage": the same path as the JDK's forms and as `tamago/cmd/` (S9), javac staying
  the reference. JF1-JF6 take repr/java off the JVM entirely (read, check, round trip at every
  level, `jrt-convert`'s convert and javac steps, the fixtures), with a fixpoint: javac on Arbace
  reads javac's own sources into the committed forms. The native front end (a) comes after, if
  the user wants it, as JF8 (a parser, javac's trees and positions checked tree by tree) and JF9
  (attribution, checked fact by fact against javac on Arbace). After JF6 the frozen JVM remains
  only for what is not repr/java: the oracle's reference, j2g's edge (HotSpot's reflection
  order), `check --suite` (the frozen bytecode back end) and the data generators.

## 1. Where things stand (2026-10-20)

### 1.1 The reader

| file | lines | role | reads javac's attribution |
|---|---:|---|---|
| `arbace/repr/java.ae` | 314 | the command (`read`, `read-jdk`), the runs, the report | no |
| `arbace/repr/java/javac.ae` | 40 | `JavacTool`, `JavacTask.parse` + `analyze` (`-proc:none -nowarn -Xlint:none -XDsuppressNotes`), the `Context`, `Types`, `Symtab` | (runs it) |
| `arbace/repr/java/convert.ae` | 3,203 | attributed trees into Java forms: declarations, code, control flow, patterns, switches, lambdas | 77% of its code lines |
| `arbace/repr/java/jtypes.ae` | 93 | javac types and symbols: erasure, boxing, widening, flags, binary names | 68% |
| `arbace/repr/java/resolve.ae` | 164 | javac symbols as arbace.classes' class infos; `pin-needed?` asks `arbace.classes.analyze/select-method` | 81% |
| `arbace/repr/java/names.ae` | 138 | one namespace per package, simple names, imports | 0% (works on the converted units) |
| `arbace/repr/java/layout.ae`, `forms.ae` | 370, 44 | the `.ae` text | 0% |
| `arbace/repr/java/full.ae` | 130 | mode `:full`'s record and hashes | 0% (positions from `TreeInfo`, in convert) |

`read` attributes all files of a run together (`-sourcepath ROOT`), `read-jdk` copies a JDK
module's files into `--patch-module` directories and attributes chunks of about 150 files in
parallel (`REPR_JAVA_THREADS`, 12) against the running JDK, with `-implicit:none
--enable-preview -source 26`. What the converter takes from javac, by the method calls of
`convert.ae` (Appendix A): `.type` (124 calls), `.sym` (47), `.tsym` (49), `.owner` (14),
`.constValue` (6), `.operator` (4, the resolved `OperatorSymbol`), `.varargsElement` (5), the
lambda's and method reference's `.target` (11), `.polyKind` (4),
`.patternSwitch`, `.hasUnconditionalPattern`, `.completesNormally` (`Flow`'s liveness, 3),
`.declaredUsingVar`, `Types` (`erasure`, `isSameType`, `isSubtype`, `boxedClass`,
`unboxedType`), symbols' flags and members, and the positions (`TreeInfo.getStartPos`,
`getEndPos` with the unit's `EndPosTable`) that mode `:full` records. It also recognizes what
javac's `TypeEnter` added (an implicit `super()` by the source text at its position, default
constructors) and leaves it out.

### 1.2 What the forms state that javac computed

SPEC §7 lists what the converter makes explicit; every item is an attribution result:
names (simple or binary, `(.m this)` against `(C/m)`, inherited fields as `(.-f this)`, javac's
qualifying class), declared types and the tags of locals typed differently from their
initializer, param-tags where Arbace's resolution differs from javac's, conversions (narrowing,
`checkcast`s, erasure casts as `TransTypes` inserts them, unboxing from non-wrapper types,
`float` promotions in comparisons, branch types), constants (folded `switch` labels, `^:const`),
evaluation order (temporaries), the typed operators of §5.4 (chosen by the promoted type),
lambdas and method references with their target type and instantiated signature, the implicit
`MatchException` default. §6, what the Java forms' front end derives (bridges, `this$0`,
accessors, enum and record members, lambda methods, switch maps), is the converter's to leave
out, and arbace.classes' to compute.

### 1.3 What needs the JVM, and why

| use | what it runs on the JVM | a native front end (a) frees it | javac on Arbace (b) frees it |
|---|---|---|---|
| `bin/repr-java read`, `read-jdk` | javac's parser and attribution, the converter (frozen jar) | yes | yes |
| `bin/repr-java check` (steps 1-3: baseline by SHA-256, samples, module) | `read` | yes | yes |
| `bin/repr-java check --suite` | the converted baseline compiled by the frozen jar's own bytecode back end, Clojure's suite on the JVM | no (the back end is the jar's) | no |
| `bin/repr-java roundtrip`, levels `forms`, `ae`, `record`, `text` | `read` of the originals and of the printed Java | yes | yes |
| `bin/repr-java roundtrip`, levels `classes` (and `anew`) | javac's class files of the original and of the printed Java, `-g:none`, byte for byte | **no**: only javac makes javac's class files | yes |
| `bin/jrt-convert` `convert` | `read-jdk` of java.base's closure (838 files) | yes | yes |
| `bin/jrt-convert` `javac` | the tree compiled twice by the system JDK's javac (determinism, the classes `edge` reads) | no | yes |
| `bin/jrt-convert` `generate` | the JDK build's generators (`generatecharacter`, `spp`, `cldrconverter`, `tzdb`, ...) compiled and run on a stock JDK (`GEN_JDK`, the build's boot JDK or `java-25-openjdk`) | no | only with JF7 (the generators as Java forms run by Arbace) |
| `bin/jrt-convert`'s own namespaces (`jrt.jrt-sources`: sources, locales, edge, report) | Arbace code on the frozen jar | could move to the tools' host either way | same |
| `bin/clojure-tests --java-fixtures` (12 files) | `read`, javac's class path the frozen jar (for `arbace.lang.IFn`) | yes, with Arbace's own classes declared another way (JFS6) | yes, the same |
| `bin/j2g-edge update`, `verify`, `trace` | reflection on the classes outside the forms (388 classes in `jdk/edge.edn`), HotSpot's method order (`:method-order`), the frozen runtime's classes, ASM's `ClassReader` | no (only the declarations could come from sources; the order is HotSpot's) | no |
| the oracle (`bin/oracle`, `test/oracle/expected`) | the frozen v4 Arbace, the reference by definition | no | no |
| `bin/jrt testdata`, `manifest`, `standins`, `jrt-coverage` | data generators reading the JVM | no | no |

So repr/java's own JVM uses are `read` and the classes level; the rest of the JVM's work is the
reference (oracle, edge, `--suite`) or data generation. A native front end (a) alone would leave
the classes level, and with it the round trip's strongest check, on the JVM; (b) frees all of
repr/java.

## 2. How much of the forms comes from attribution (measured)

### 2.1 The converter's code

A top-level form of a namespace counts as reading javac's attribution when its text calls one of
javac's attribution accessors (`.type`, `.sym`, `.tsym`, `.constValue`, `.operator`, `.owner`,
`.varargsElement`, `.target`, `.completesNormally`, ...), a `Type$`/`Symbol$` class, `Flags`,
`jtypes` or `resolve` (Appendix A, `attr.py`; comment and blank lines not counted):

| namespace | top-level forms | of them reading attribution | code lines | in those forms |
|---|---:|---:|---:|---:|
| `convert` | 242 | 133 | 2,824 | 2,188 (77%) |
| `resolve` | 18 | 12 | 137 | 111 (81%) |
| `jtypes` | 32 | 18 | 68 | 46 (68%) |
| `names`, `layout`, `full`, `forms`, `javac` | 83 | 0 | 618 | 0 |

The tree's shape (kinds, children) is read everywhere too; a parser alone would give that.

### 2.2 The forms

Every list in the converted files, classified by its head (Appendix A, `heads.py`,
`classify.py`; metadata not counted):

| head | examples | needs | jdk/forms (240,546) | baseline (60,902) | samples (589) |
|---|---|---|---:|---:|---:|
| declarations | `defclass`, `method`, `field`, `constructor`, `in-ns`, `import`, `load`, `anon`, `letclass` | syntax | 13.2% | 12.9% | 25.1% |
| code from syntax and reachability | `let`, `if`, `when`, `cond`, `set!`, `loop`, `recur`, `while`, `aget`, `aset`, `alength`, `new`, `C.`, `throw`, `try`, `<`, `and`, `nil?`, `some?`, `switch`, `lambda` | syntax, `Flow`'s liveness | 47.4% | 40.7% | 46.0% |
| instance calls | `(.m x ...)` (`m(...)` written bare is `(.m this ...)` or `(C/m ...)`) | names, members | 14.4% | 16.3% | 6.8% |
| field access | `(.-f x)`, inherited fields as `(.-f this)` | names, members | 5.1% | 4.7% | 0.2% |
| static members | `(C/m ...)`, `(Objects/requireNonNull ...)` | names, members | 5.5% | 13.9% | 1.7% |
| typed operators and conversions | `unchecked-add-int`, `bit-and-int`, `==`, `identical?`, `=`, `java-str`, `cast`, `unchecked-int`, `long`, `char` | expression types | 14.2% | 10.9% | 19.2% |
| boxing and unboxing calls | `(.intValue ...)`, `(Integer/valueOf ...)` (written or inserted) | expression types | 0.2% | 0.6% | 1.0% |

The leaves depend on attribution as much: java.base's closure has 270,466 identifiers and
69,122 member selections in javac's trees, each resolved to a local, a field (36,264 field reads
become bare symbols, `(.-f this)` or `C/f`), a class (written by simple name only when the
package namespace's naming gives it, `names.ae`) or a package; the tags on bindings are written
where javac's type of the initializer differs from the declared one.

### 2.3 What only javac's generic attribution gives

The converter's counters (the reports' `:stats`), for the four corpora:

| counter | what | jdk (838 files) | baseline (183) | samples (10 + 4) |
|---|---|---:|---:|---:|
| `:conv/cast` | `checkcast`s javac inserts (erasure casts, `TransTypes`) | 2,204 | 1,406 | 5 |
| `:conv/hint` | upcast hints | 417 | 89 | 1 |
| `:conv/unbox`, `:conv/box` | conversions from or to generic or non-wrapper types | 156, 143 | 2, 67 | 0, 4 |
| `:conv/float-promotion` | Java's `int`/`long` against `float` comparisons | 9 | 10 | 1 |
| `:form/lambda`, `:form/method-ref` | typed by their inferred target and instantiated signature | 396, 246 | 8, 1 | 4, 8 |
| `:form/anon-diamond` | `^:diamond` (javac's `Signature` rule) | 43 | 0 | 0 |
| `:pin/decided` | overload decisions asked of arbace.classes | 56,773 | 20,168 | 78 |
| `:form/param-tags` | of them where its erased resolution differs from javac's | 164 | 4 | 13 |
| `:switch/match-exception` | javac's implicit default (exhaustiveness) | 6 | 0 | 1 |

These are 3,735 facts against jdk/forms' 240,546 lists (1.6%), and 1,587 against the
baseline's 60,902 (2.6%): few, and
each one needs the generic type of an expression, an inferred type argument or a lambda's
target, which is javac's `Attr`, `Infer` and `DeferredAttr` (and `Types`' capture, `lub` and
substitution). A front end that got any of them wrong would change the forms (a cast missing,
a different operator after unboxing, an unpinned call that Arbace resolves to another method).

### 2.4 What arbace.classes computes, and what it does not

arbace.classes analyzes the Java forms, where all of §1.2 is already explicit:

| javac's work | arbace.classes today | for a native front end |
|---|---|---|
| declarations entered: classes, members, flags, nesting (`Enter`, `TypeEnter`, `MemberEnter`) | yes, over forms (`parse`, `analyze`'s entering) and class infos (`env`: forms, defined classes, the edge's, a JVM's reflection) | over parsed Java and over the platform's declarations (§4.6) |
| generic declarations, `Signature`, bridges with substitution | yes (declarations only; the world's signatures in `arbace/classes/generics.edn` on Go) | the same, plus generic types in code |
| member lookup with Java's access rules, qualifying type (JLS 13.1) | yes | reusable |
| overload resolution, JLS 15.12.2 on erased types, four phases | yes (`select-method`; agrees with javac in 99.7% of jdk's decisions) | needs javac's own, generic and inference-aware |
| constant folding (JLS 15.29), `ConstantValue` | yes | reusable |
| expression types | erased, under the forms' rules (an integer literal is a `long`, `if` boxes mixed branches) | Java's rules: numeric promotion, conditional typing, `lub` |
| generic types in code, capture, wildcards, inference (JLS 18), poly expressions, lambda targets, `var`, diamond | no (SPEC §1 principle 4) | new |
| reachability, definite assignment, exhaustiveness (`Flow`, `ExhaustivenessComputer`) | partly (forms that do not complete, §5.7) | new for Java's statements |
| names in Java's scopes: packages, imports, static imports, shadowing, obscuring (JLS 6.4, 6.5) | no (the forms have Clojure's resolution, SPEC §5.2) | new |
| `TransTypes`' erasure casts | no (the converter writes them) | new |

### 2.5 The answer

A native Java front end is not "a parser, with resolution left to arbace.classes": arbace.classes
works on forms whose names, types, overloads and conversions are explicit, and its resolution
is Arbace's (erased, Clojure's literals), not javac's. What it provides (the class environment,
access, erased resolution as a first answer, constant folding, the derivations, generic
declarations) is roughly a third of what the converter consumes; the rest is javac's attribution
proper: Java's name resolution, expression typing, generics and inference, flow. The parser is
the smaller part: javac's parser is 9,300 lines without its doc comment parsing, what its
attribution takes about 47,000 (§3.1).

## 3. Prior art

### 3.1 javac (jdk26u)

https://github.com/openjdk/jdk26u at `baf63fbe42b8758448fee570a9d9bb914272d259` (2026-08-18),
the commit `jdk/provenance.edn` names and the frozen JDK 26.0.2.1 was built from;
`src/jdk.compiler/share/classes`: 348 files, 162,441 lines (`com/sun/tools/javac` 205 files,
146,965; `com/sun/source` 140 files, 14,787), every file under the GPLv2 with the Classpath
Exception (all 348 headers designate it). `java.compiler` (javax.lang.model, javax.tools,
javax.annotation.processing): 137 files, 20,080 lines. `jdk.internal.opt`: javac uses only
`jdk/internal/opt/CommandLine.java` (317 lines); `jdk.zipfs` is required for jars and `ct.sym`.
The build generates 21 files, 15,069 lines (11,361 without the translated bundles:
`CompilerProperties.java` 9,125 from `compiler.properties`, the bundles `compiler`, `javac`,
`launcher`, `ct`, `version`, `FlagsEnum`), in `build/*/support/gensrc/jdk.compiler`.

| package (`com/sun/tools/javac/`) | files | lines | in a parse + analyze run |
|---|---:|---:|---|
| `parser` | 15 | 12,618 | yes, without `DocCommentParser` (2,477), `JavadocTokenizer` (513), `ReferenceParser` (304): about 9,300 |
| `tree` | 12 | 12,795 | yes (`JCTree`, `TreeInfo`, `TreeMaker`, `TreeScanner`; `Pretty` and `DocTree*` not needed) |
| `code` | 26 | 21,525 | yes: `Types` 5,455, `Symbol` 2,701, `Type` 2,441, `Symtab` 923, flags, scopes, `Source`, `Preview` |
| `comp` | 34 | 46,312 | attribution: `Attr` 6,156, `Check` 5,796, `Resolve` 5,247, `Flow` 3,028, `Infer` 1,950, `TypeEnter` 1,593, `Annotate` 1,438, `DeferredAttr` 1,376, `Operators` 868, `ArgumentAttr` 757, `Enter` 688, `InferenceContext` 603, `ExhaustivenessComputer` 677, `MemberEnter` 455, `ConstFold` 339, `Modules` 1,841; lowering (not run by repr/java's `analyze`): `Lower` 4,426, `TransPatterns` 1,611, `LambdaToMethod` 1,353, `TransTypes` 1,233 |
| `jvm` | 18 | 15,481 | `ClassReader` 3,546 (the platform's class files); `Gen` 2,608, `ClassWriter` 1,765 for class files (the round trip's classes level) |
| `util`, `file`, `main`, `api`, `model`, `processing`, `platform`, `launcher` | 101 | 38,142 | the context, names, diagnostics, the file manager, options, the API; processing and the launcher not needed |

The attribution a front end must reproduce (`Attr`, `Check`'s part of it, `Resolve`, `Infer`,
`InferenceContext`, `DeferredAttr`, `ArgumentAttr`, `Types`, `Symbol`, `Type`, `Symtab`, the
`Enter`s, `ConstFold`, `Operators`, `Annotate`, `Flow`'s liveness, `ExhaustivenessComputer`,
`TransTypes`' casts, `ClassReader`) is 47,270 lines of javac (all of `Check` and `Flow`
counted); Go's `go/types` (24,879 lines at TamaGo's go1.27.1), which goi's checker
`arbace.go.check` follows (8,071 lines today; GR2's checker was built on 2026-10-17), is about
half of it, without overloading or Java's inference.

javac moves with every release: jdk26u's `Source.Feature` adds `CAPTURE_MREF_RETURN_TYPE`
(JDK 26, a change of inference for method references), and `PRIMITIVE_PATTERNS` is its one
preview feature (`Preview.isPreview`). A reproduction follows that drift by hand; javac itself
follows it by being upgraded.

### 3.2 ECJ, Eclipse's compiler

https://github.com/eclipse-jdt/eclipse.jdt.core at `5e1198e83e6d8af17da1c570d1759675cf4f9036`
(2026-10-20), `org.eclipse.jdt.core.compiler.batch` (Bundle-Version 3.47.0; its
`ClassFileConstants` knows JDK 25 to 28): 553 files, 241,716 lines; `internal/compiler/ast`
54,680, `lookup` 51,503 (bindings, inference), `parser` 31,700 (LALR tables generated from a
grammar by jikespg), `codegen` 12,375, `flow` 6,747, `problem` 13,554. EPL-2.0
(`SPDX-License-Identifier: EPL-2.0`). A complete compiler with its own attribution and class
file writer: its answers agree with javac's on correct programs by the JLS, but not as data
(other synthetic names such as `lambda$0`, other anonymous numbering, other erasure casts and
constant pool order, its own inference corner cases), so neither the forms nor the class files
would be javac's.

### 3.3 JavaParser and its symbol solver

https://github.com/javaparser/javaparser at `6dc10fcd35fc40630239f7f84c8980f7ad8268d5`
(2026-10-19; the release `javaparser-parent-3.28.2`, 2026-05-31): `javaparser-core` 577 files,
99,905 lines (much of it generated: visitors, metamodel), its JavaCC grammar `java.jj` 6,153
lines, language levels up to `JAVA_26` (`ParserConfiguration.LanguageLevel.BLEEDING_EDGE`);
`javaparser-symbol-solver-core` 173 files, 32,375 lines, of which `typeinference` 5,771 (a JLS
18 implementation that the solver uses only in part). Apache-2.0 or LGPL-3.0, at the user's
choice. A good parser with comments and positions (a concrete model for a native parser's tree,
its positions not javac's), and an attribution that is incomplete exactly where §2.3's facts
come from (lambdas, generic methods, method references).

### 3.4 tree-sitter-java

https://github.com/tree-sitter/tree-sitter-java at `e10607b45ff745f5f876bfa3e94fbcc6b44bdc11`
(2025-09-14, version 0.23.5): `grammar.js` 1,338 lines, the generated `src/parser.c` 76,760
lines; records, record patterns, guards, switch expressions, `yield`, modules, `permits`. MIT.
An error-tolerant concrete syntax tree for editors, in C (Arbace builds without cgo): syntax
only, a grammar to consult.

### 3.5 Precedents in Arbace

- `jdk/forms` (S1): 838 Java files of java.base's closure (460,626 lines, 18.4 MB) converted by
  repr/java into 911 files of forms (10.4 MB), translated by j2g into every Go build: Java code
  of the JDK run by Arbace, at 2.3 to 32 times the JVM's time on `bin/j2g-perf`'s workloads
  ([../go/SPEED-NOTES.md](../go/SPEED-NOTES.md)).
- `tamago/cmd/` (S9, 2026-10-15): Go's toolchain (105 packages of `GOROOT/src/cmd`) held as Go
  forms, `bin/toolchain-vendor convert|verify|check`, the nine tools built from the forms
  byte-identical to the originals'. "Arbace runs javac" is the same move for Java.
- The converted JDK of 2026-10-07 ([CONVERTER-NOTES.md](CONVERTER-NOTES.md), "The converted
  JDK"): all 68 modules of jdk26u converted without failure, `jdk.compiler` 347 files of which
  342 with forms, 331 compiled to javac's class shapes (10 differing: switch fall-through
  repeating a lambda in `JavacParser` and `DeferredAttr`, the attribution order of anonymous
  classes; 1 error, an anonymous class in `Attr`'s constructor prologue); `java.compiler` 135,
  129, 129.

### 3.6 Licenses

| source | license | in LICENSE.md today | vendoring it would mean |
|---|---|---|---|
| javac, java.compiler, jdk.internal.opt (jdk26u) | GPLv2 with the Classpath Exception | yes ("OpenJDK (jdk26u)", covering `jdk/`) | the covered files extended (the new forms' tree); no new text. The program already links jdk26u's GPL+CE code (`jdk/forms` translated); a tool executable with javac is the same arrangement |
| ECJ | EPL-2.0 | no (Arbace's own code is EPL-1.0) | EPL-2.0's text added, its files listed |
| JavaParser | Apache-2.0 or LGPL-3.0 | Apache-2.0's text is there (Murmur3, purego) | its files listed under Apache-2.0 (choosing it) |
| tree-sitter-java | MIT | no | MIT's text added (only if code is taken; consulting the grammar is not vendoring) |

## 4. Options

### (a) An Arbace parser of Java text, attribution by Arbace

`arbace.repr.java.parse` (a lexer and a recursive descent parser for Java 26, after
`JavacParser`'s structure, so that the trees and positions are javac's), then an attribution in
Arbace: Java's scopes and names, the platform's and the sources' declarations entered into
arbace.classes' environment, expression typing with Java's rules, javac's overload resolution
(arbace.classes' as the first phase), generics in code with capture and `lub`, JLS 18 inference
with javac's deferred attribution of lambdas and method references, `Flow`'s liveness and
exhaustiveness, `TransTypes`' casts. The converter then reads the native trees (its 133
attribution-reading forms rewritten against an interface, JFS7).

- Size: the parser 3,000 to 5,000 lines of Arbace; the attribution 15,000 to 25,000 (goi's
  checker is a third of go/types' size).
- Fidelity: by differential checking only, fact by fact against javac, over corpora; exact
  equality over the JDK's 12,730 files is an open tail (javac's inference is defined by its
  implementation where the JLS leaves latitude, and javac's diagnostics are not needed but its
  choices are). Each JDK release must be followed by hand.
- The classes level: not given (no class files); the round trip would keep javac on the JVM, or
  lose the level.
- Direction: the most "Arbace all the way down": Java's front end written in Arbace.

### (b) javac itself, as Java forms, run by Arbace

repr/java converts jdk26u's `jdk.compiler`, `java.compiler`, `jdk.internal.opt`'s
`CommandLine` and javac's generated sources into Java forms (once on the frozen JVM, then by
itself, JF5); j2g translates them with jrt into a tool executable, as `--build-tests` adds the
fixtures to the program; `arbace.repr.java`'s reader namespaces, unchanged, run on it (compiled
by c2j into the tool, or evaluated), calling javac's classes as today they call them on the JVM.

- Size: 485 Java files, 6.9 MB of text (plus 15,069 generated lines), about 4 MB of forms by
  `jdk/forms`' ratio: about 200,000 lines of Java, some 40% of what java.base's closure
  (460,626 lines) puts into the program. Javac's own source uses
  what java.base's closure uses, more of some (by a text scan: about 1,000 lambdas, 220 method
  references, 270 `instanceof` patterns, 265 arrow cases, 240 anonymous classes, 21 records).
- What jrt lacks (Appendix A, `deps.py`, a name scan of javac's imports and qualified names
  against jdk26u's packages): javac names 305 classes outside `jdk.compiler`: 238 of java.base,
  66 of java.compiler, 1 of jdk.internal.opt. Of the 238, 171 are in jrt's world today (in
  `jdk/sources.txt` or jrt's hand-written `manifest.edn`), 67 are not:

  | area | classes (of the 67) | needed by `read`? |
  |---|---|---|
  | file systems: `java.nio.file.FileSystem(s)`, `spi.FileSystemProvider`, `SimpleFileVisitor`, `FileVisitOption`, `FileVisitResult`, `attribute.BasicFileAttributes`, their exceptions | 12 | yes, `JavacFileManager`, `Locations`, `FSInfo` (unless an Arbace `JavaFileManager` replaces the standard one, JFS5) |
  | the module system: `java.lang.Module`, `ModuleLayer`, `java.lang.module.*`, `jdk.internal.module.Resources` | 9 | partly (`Locations`' module path, `ModuleFinder` for jars), avoidable without `--module-path` jars |
  | jar, zip, jmod: `JarFile`, `Manifest`, `Attributes`, `ZipFile`, `ZipEntry`, `JmodFile` | 6 | only for jars on the class path and `ct.sym` (`--release`): avoidable |
  | annotations: `java.lang.annotation.*` (9), `Deprecated`, `Override`, `SafeVarargs`, `SuppressWarnings`, `FunctionalInterface`, `sun.reflect.annotation.*` (5) | 19 | the annotation types yes (small); `sun.reflect.annotation` only for processors' `getAnnotation` |
  | serialization, security: `ObjectInputStream`, `ObjectStreamClass`, `Serial`, `ProtectionDomain`, `CodeSource`, `CodeSigner` | 6 | no (plugins, processors' class loaders) |
  | services: `ServiceLoader`, `spi.ToolProvider` | 2 | no (`JavacTool.create` is called directly) |
  | the rest: `DataOutputStream`, `FilterWriter`, `IOError`, `ClassFormatError`, `UnsupportedClassVersionError`, `CoderResult`, `CodingErrorAction`, `LambdaMetafactory`, `ExactConversionsSupport`, `PreviewFeature`, `MethodFinder`, `VM`, `Object` (jrt's own, hand-written in Go) | 13 | some; small |

  Each class added to the closure brings its own closure (`bin/java-surface` measures it, JF1);
  where javac reaches what Arbace does not want (the `jrt:/` file system of `lib/modules`,
  `ServiceLoader`), an `overlay/jdk` variant cuts it, as for java.base.
- The platform's declarations: javac reads the JDK's classes from the image (`lib/modules`
  through `jrt:/`) or `ct.sym`; Arbace has neither. §4.6.
- Speed: javac on the frozen JVM compiles the baseline (812 classes) in 8 s and java.base's
  closure (2,224 classes) in 33 s (the round trip's logs); `read` takes 18 s for the baseline and
  31-34 s for the closure (12 threads). At the j2g-translated code's 2.3 to 32 times the JVM's time
  (and without HotSpot's warm-up, which dominates short runs) the baseline's `read` would take
  about 1 to 5 minutes; JF1 measures it.
- Fidelity: javac's, by construction, as far as Arbace runs Java as the JVM does; checked by the
  class files (javac on Arbace against javac on the JVM, byte for byte, JF3) and by the forms
  (every corpus equal, JF4).
- The classes level: kept, without a JVM.
- Direction: Java's front end is javac, held as Arbace forms and run by Arbace, as Go's toolchain
  in `tamago/cmd/`; it also gives the executable javac's whole surface (Java text at the REPL,
  javac's diagnostics, the JDK's own build).

### (c) ECJ, in the same way

ECJ's 241,716 lines converted and translated as (b), the converter rewritten against ECJ's AST
and bindings (`convert.ae`, `jtypes.ae`, `resolve.ae`, about 3,400 lines). Gives a JVM-free
attribution, but not javac's: the forms would differ from today's wherever ECJ's choices do
(synthetic names, numbering, casts, inference corners), so the switch is not checkable by
equality, and the classes level is not javac's. A new license (EPL-2.0). Dominated by (b).

### (d) Other

- **(d1) javac on a stock JVM, behind a process boundary** (S4's O1 (b)): repr/java's Arbace
  side on the Go host, javac alone in a JDK serving attributed trees as data. Frees the frozen
  jar, not the JVM; a protocol for the attributed trees to design (§2.3's facts as data).
- **(d2) JavaParser's parser plus its symbol solver, converted and run as (b)**: smaller than
  javac (132,280 lines) but its attribution misses §2.3's facts; Apache-2.0.
- **(d3) tree-sitter-java** for the syntax: C, error-tolerant, not javac's trees or positions;
  no attribution.
- **(d4) a native parser with javac's attribution**: (a)'s parser feeding javac on Arbace
  (javac's `Attr` over trees made outside its parser): javac's trees are its own classes, so
  this is (b) with a second parser; useful only as (a)'s first step and check (JF8).

### 4.5 Side by side

| | (a) native | (b) javac as forms | (c) ECJ as forms | (d1) stock JVM process |
|---|---|---|---|---|
| no JVM | yes | yes | yes | no |
| no javac | yes | no (javac as Arbace forms) | yes | no |
| forms equal to today's, checkable | by differential work, open tail | by construction | no | yes |
| classes level (javac's class files) | no | yes | no | yes |
| Arbace code to write | 18,000-30,000 lines | the tool build, jrt's gaps, a file manager, the platform: a few thousand | 3,400 rewritten, jrt's gaps | a protocol |
| speed | Arbace's (compiled by c2j) | j2g's, 2.3-32 times the JVM's | j2g's | the JVM's |
| follows JDK releases | by hand | by upgrading the forms | by upgrading the forms | by upgrading the JDK |
| licenses | none new | none new | EPL-2.0 | none |
| effort to the switch | 30-50 days | 13-20 days | 15-25 days | 4-6 days |

### 4.6 The platform's declarations

What javac attributes the corpora against, without a JDK image:

- **Exploded modules compiled by javac on Arbace**: java.base (3,097 + 84 Unix and Linux files
  and 236 generated, 1,159,182 + 142,016 lines) and java.xml (the baseline's `XMLHandler.java`
  imports `org.xml.sax`) compiled from jdk26u's sources with the JDK build's own options
  (`--system none --module-source-path`, `make/CompileJavaModules.gmk`), into a directory
  cached by key (jdk26u's commit, javac's forms, the generated sources); every later run uses
  them as the JDK build's tools do (`--system none --upgrade-module-path`,
  `make/common/JavaCompilation.gmk`). The class files can be compared once with the frozen
  JDK's (the declarations javac sees are the same, so the forms are).
- **jdk26u's sources on the source path**, completed lazily (only the classes the corpus names
  are parsed): no cache, but each run parses hundreds of the JDK's files, and javac's module
  rules for an unnamed module without a system `java.base` must be tried (JF1).
- **A recorded stub set**: the platform's declarations recorded on the frozen JVM as `ct.sym`-like
  data and committed (as `jdk/edge.edn`): JVM data again.

The generated sources java.base needs (`CharacterData*`, the NIO buffers, `sun.nio.cs`'s charsets,
CLDR's locale data) come from the JDK build's `support/gensrc` or from `bin/jrt-convert`'s
`generate` step, which runs the build's generators on a stock JDK (§1.3; JF7 moves them).

### 4.7 The classes level: only javac

The round trip's `classes` level compiles the original and the printed Java with javac
(`-g:none`) and compares the class files byte for byte, per Java file: 143 of 183 (baseline),
12 of 14 (samples), 473 of 838 (jdk) in mode `:lines`, and 1,035 of 1,035 in mode `:full`
([PRINTER-NOTES.md](PRINTER-NOTES.md)). The check needs a compiler whose output is a function of
the source; byte identity with the JVM's javac is needed only to keep the references
(`test/repr/java/roundtrip/*.edn`) unchanged across the switch. ECJ or Arbace's own Java forms
back end (on `arbace-for-java-26`) would give other bytes, a weaker and different level. javac on
Arbace gives the same bytes if Arbace runs it faithfully, which JF3 checks once against the
frozen JVM's javac (the baseline's 812 classes, java.base's closure's 2,224).

## 5. Java 26's language level, and what the corpora use

javac's tree kinds met by the converter (the reports' `:kind/` counters), and a text scan of the
originals for what has no tree kind of its own:

| feature (release) | jdk (838 files) | baseline (183) | samples (14) | fixtures (12) |
|---|---:|---:|---:|---:|
| generics: type parameters, parameterized types, wildcards | 1,817, 12,156, 3,390 | 6, 211, 24 | 8, 50, 14 | 1 generic type, 9 uses (text) |
| explicit type arguments of calls (`.<T>m(...)`, text) | 28 | 1 | 0 | 0 |
| diamond `<>` (7; text) | 1,030 | 4 | 3 | 7 |
| lambdas, method references (8) | 401, 264 | 8, 1 | 4, 8 | 0, 0 (text) |
| local classes (`letclass`), anonymous classes | 62, 234 | 0, 50 | 1, 3 | 0 anonymous (text) |
| `var` (10; text) | 213 | 0 | 1 | 0 |
| switch expressions, `yield`, arrow cases (14) | 129, 795 | 0 | 4, 13 | 0 |
| text blocks (15; text) | about 7 | 0 | 1 | 0 |
| records (16) | 6 | 0 | 3 | 0 |
| `instanceof` patterns (16), their flow scoping | 374 bindings, 294 `if-instance` | 0 | 14, 4 | 0 |
| sealed classes, `permits` (17) | 32 sealed, 11 non-sealed | 0 | 1 | 0 |
| pattern switches, `case null`, guards (21) | 80 pattern labels, 14 guards | 0 | 7 | 0 |
| record patterns (21) | 0 | 0 | 4 | 0 |
| unnamed variables `_` (22) | 1 | 0 | 1 | 0 |
| flexible constructor bodies (25) | (not counted; one in `Attr` of jdk.compiler) | 0 | 0 | 0 |
| module import declarations, compact source files (25) | 0 | 0 | 0 | 0 |
| modules (`module-info.java`) | skipped by `read-jdk` | 0 | 1 (`test/repr/java/module`) | 0 |
| annotations, type annotations | 6,112, 1 | 203, 0 | 21, 25 | 101 annotations (text) |
| Unicode escapes (`\uXXXX`, text) | 1,666 lines | 3 | 0 | 0 |
| primitive patterns (26, preview) | 0 | 0 | 0 | 0 |

So the corpora use the language to Java 22, java.base's closure carrying the generics, lambdas,
switch expressions and patterns, the samples the rest (record patterns, type annotations,
modules). A native parser needs all of Java 26's syntax (the JDK's sources may use any of it,
`read-jdk` runs with `--enable-preview -source 26`); its attribution needs, for these corpora,
everything but primitive patterns. javac's own source (option (b)'s input) is written in the
boot JDK's language (25).

## 6. The output, and how the switch is checked

Whatever front end reads Java, the forms must be today's, byte for byte, over every corpus, so
that the switch is a comparison and every downstream check stays as it is:

| corpus | reference today | check after the switch |
|---|---|---|
| baseline (183 Java files, 189 converted) | `test/repr/java/expected/baseline.sha256` | the same hashes |
| samples (10 + 4 files) | `test/repr/java/expected/java`, `module` | the same files |
| java.base's closure (838 files) | `jdk/forms` (`bin/jrt-convert verify`) | the same files, and `jdk/provenance.edn` naming the new host |
| the suite's fixtures (12 files) | `test/java-fixtures/forms` | the same files (JFS6) |
| mode `:full` of the three round-trip corpora | `test/repr/java/roundtrip/*-text.edn` (record, text, classes: no known difference) | the same results |
| the round trip's levels `forms`, `ae`, `classes` | `test/repr/java/roundtrip/*.edn`, `*-known.edn` | the same results |
| javac's own sources (option (b), JF5) | the committed javac forms | the same files, read by javac on Arbace |

Mode `:full` adds javac's positions (UTF-16 offsets, from a declaration's first modifier or
annotation to its closing brace or semicolon) and the file's text as `getCharContent` gives it;
with (b) they are javac's by construction, with (a) the native parser must reproduce them (JF8's
tree level compares them).

## 7. Plan

The recommended path is (b) to the switch (JF1-JF6), then optionally (a) on (b)'s checks (JF8,
JF9). The estimates are working days, by the record of comparable work here: S9's toolchain as
forms in about a day (on repr/go and the vendoring scripts), the JDK's forms and jrt (B1a) in
three to four days, goi's checker (GR2) within a day (2026-10-17), j2g's first program in about
three.

| milestone | content | exit criteria | effort | checks, gate |
|---|---|---|---:|---|
| **JF1** spike | javac's sources (jdk.compiler, java.compiler, `CommandLine`, the generated sources) read by `bin/repr-java read-jdk` on the frozen JVM into `.tmp/`; their closure measured (`bin/java-surface`); a minimal program (parse one file, then attribute `Reduced.java`) translated by j2g with jrt; the platform's options tried (§4.6) | the parse of the samples on Go printed by javac's `Pretty` equal to the JVM's; the list of jrt's gaps and cuts; the tool's size, build time and a first `analyze` time | 2-3 | none (scratch) |
| **JF2** javac's forms vendored | the forms committed with provenance and a source list (JFS3), a script `convert`, `verify` (repr/java on the frozen JVM until JF5); LICENSE.md's GPL+CE list extended; jrt-convert's closure grown by the java.base classes javac needs, `overlay/jdk` cuts (the `jrt:/` file system, `ServiceLoader`) | `verify` byte for byte; j2g translates the javac world without errors; `bin/gate` (essential) unchanged | 2-3 | `verify` in `bin/gate --full` |
| **JF3** javac runs on Arbace | jrt's gaps filled (the file system API javac's file manager uses, or an Arbace `JavaFileManager`, JFS5); the tool executable (JFS4); the platform (JFS2): java.base and java.xml exploded, compiled by javac on Arbace, cached by key | javac on Arbace compiles the baseline (812 classes), the samples and java.base's closure (2,224 classes), `-g:none`, byte for byte equal to the frozen JVM's javac; the platform's class files equal to the frozen JDK's | 4-6 | a `check` step of the script in `bin/gate --full` (while the JVM exists) |
| **JF4** repr/java's `read` on Arbace | `arbace.repr.java`'s reader (`java.ae`, `javac`, `convert`, `jtypes`, `resolve`, `names`, `layout`, `full`, `forms`) on the tool executable, compiled by c2j or evaluated; `read-jdk`'s `--patch-module` against the exploded platform; the fixtures' declarations of Arbace's runtime (JFS6) | every corpus of §6 equal: baseline hashes, samples, module, `jdk/forms`, the fixtures, mode `:full`; times measured against the JVM's | 2-4 | `bin/repr-java check` on the new host |
| **JF5** the fixpoint, the checks moved | javac on Arbace reads javac's own sources into the committed forms (the script's `verify` without the JVM); `bin/repr-java check`, `roundtrip` (every level, classes and text included), `bin/jrt-convert`'s `convert` and `javac` steps, `bin/clojure-tests --java-fixtures` on the tool executable | the references of `test/repr/java/roundtrip/` unchanged; `bin/jrt-convert verify`, `--java-fixtures verify`, the script's `verify` and `check` pass on the new host; `bin/gate --full` passes | 2-3 | the `--full` steps `repr-java`, `repr-java-roundtrip`, `jrt-convert`, `java-fixtures` on the new host (JFS9) |
| **JF6** the JVM leaves repr/java | `bin/repr-java` no longer sources `bin/lib/frozen.bash` for `read`; `frozen_view tools` loses repr/java; CLAUDE.md, CONVERTER-NOTES, STANDALONE-NOTES (O1 closed), README; what remains on the JVM listed | nothing of repr/java starts a JVM (a trace of the gate's run); the frozen jar used only by the oracle, the edge, `check --suite` and the data generators | 1 | — |
| JF7 (later) the generators | `bin/jrt-convert generate`'s tools (`generatecharacter` 4,463 lines, `spp` 207, `generateextraproperties` 168, `tzdb` 1,232, `cldrconverter` 6,221 with java.xml's SAX parser, `generatebreakiteratordata` 4,206, ...) read by repr/java on Arbace and run as Java forms (interpreted or built) | their outputs byte for byte the stock JDK's (`generated.edn`) | 3-5 | in `jrt-convert` |
| JF8 (later, (a)) a native parser | `arbace.repr.java.parse`: Java 26's lexer and parser in Arbace, javac's trees as data (kinds, names, modifiers, positions with javac's start and end) | a tree level: every file of jdk26u's 68 modules (12,730), the corpora and javac's sources parsed to the trees javac on Arbace gives, positions included | 4-6 | a `--full` step |
| JF9 (later, (a)) a native attribution | the `Enter`s over arbace.classes' environment and the platform's declarations; Java's names; expression types; javac's resolution; generics, capture, inference, lambdas; liveness and exhaustiveness; `TransTypes`' casts; the converter on an interface over both front ends (JFS7) | per fact (each tree's symbol, type, constant, operator, varargs, target), equal to javac on Arbace over the corpora (a differential check, as `bin/diffcheck`); then every corpus's forms equal with the native front end | 25-45 | the differential check in `--full`; javac kept for the classes level and as the reference |

To the switch (JF1-JF6): 13-20 working days. With the generators (JF7) nothing of `bin/jrt-convert`
needs a JVM. The native front end (JF8, JF9): 30-50 more.

What lets the frozen JVM be dropped from repr/java entirely: JF3 (javac on Arbace, byte for byte
the JVM's), JF4 (the forms equal) and JF5 (the fixpoint: repr/java on Arbace reproduces javac's
own forms, so even javac's next upgrade needs no JVM: the JDK always builds with the previous
release's javac). The JVM's remaining roles afterwards are not repr/java's: the oracle's
reference, j2g's edge (HotSpot's reflection answers and method order), `check --suite` (the frozen
bytecode back end; the Go test build already runs the suite on the converted forms) and the data
generators (`bin/jrt`, `jrt-coverage`; `jrt-convert generate` until JF7).

Re-seeds: none needed by JF1-JF6 if javac lives in a tool executable (JFS4 (b)); jrt's additions
(JF2, JF3) change the program and are batched with the next re-seed. JF8 and JF9 are Arbace
namespaces run by the tools' host, evaluated from the checkout like repr/haskell's.

## 8. Risks

- **R1 Arbace runs Java not exactly as the JVM.** javac depends on `String.hashCode`, `HashMap`
  and `LinkedHashMap` orders, identity hashes (symbols do not override `hashCode`; javac is
  deterministic in its output but not necessarily in its iteration over identity-keyed maps),
  `SoftReference` caches, deep recursion (long `+` chains in the JDK), exceptions as control
  flow (`CompletionFailure`). JF3's byte-for-byte class files are the test; a divergence found
  there is a jrt or j2g bug worth fixing anyway.
- **R2 The closure grows.** javac's 67 missing java.base classes bring their own; the module
  system and the file systems are large. Cuts in `overlay/jdk` and an Arbace `JavaFileManager`
  (JFS5) keep it bounded; JF1 measures before anything is committed.
- **R3 Speed.** j2g's code is 2.3 to 32 times slower than the JVM's; the platform's exploded
  modules cost a long first compile (java.base's 3,400 files), cached by key. The round trip's
  three corpora (735 s in the last gate on the JVM) could take several times longer.
- **R4 The tool executable's size and build time.** About 40% of java.base's closure added to
  the program's world;
  a separate build (JFS4 (b)) keeps the shipped executable and every gate build as they are.
- **R5 The fixtures' declarations of Arbace's runtime.** Today javac reads `arbace.lang.IFn` from
  the frozen jar (v4's runtime); a JVM-free declaration of the checkout's runtime may change the
  fixtures' forms where the two runtimes differ (JFS6).
- **R6 For (a): the tail.** Exact equality with javac's attribution over 12,730 JDK files may not
  converge in the estimate; javac's inference changes with releases (`CAPTURE_MREF_RETURN_TYPE`
  in 26).
- **R7 javac's internals in the converter.** `convert.ae` uses javac's internal API
  (`com.sun.tools.javac.tree`, `code`), which changes between JDK releases; with (b) javac's
  upgrade and the converter's adaptation are one change, as today.

## 9. Proposals

- **JFS1 The approach.**
  - Options: (a) an Arbace parser and attribution (§4 (a)); (b) javac as Java forms, run by Arbace
    (§4 (b)); (c) ECJ as forms; (d) javac on a stock JVM behind a process (d1).
  - *Recommended: (b) to the switch (JF1-JF6), (a) later on (b)'s checks* (JFS10). (b) is D12 (b)
    of STANDALONE-SURVEY, keeps the forms and the classes level exactly, and costs a third of (a).
- **JFS2 The platform's declarations** (§4.6).
  - Options: (a) java.base and java.xml exploded, compiled by javac on Arbace from jdk26u with the
    JDK build's options, cached by key; (b) jdk26u's sources on the source path, completed
    lazily; (c) a stub set recorded on the frozen JVM and committed.
  - *Recommended: (a)*, the JDK build's own way, checked once against the frozen JDK's classes;
    (b) if JF1 shows the exploded compile too slow and the source path workable.
- **JFS3 Where javac's forms live.**
  - Options: (a) in `jdk/forms` with java.base's closure, `bin/jrt-convert`'s modules extended;
    (b) a tree of their own beside it (`jdk/javac/`: forms, `provenance.edn`, `sources.txt`) with
    a script `bin/javac-vendor convert|verify|check`, as `tamago/cmd/` and
    `bin/toolchain-vendor`; (c) a top-level `javac/`.
  - *Recommended: (b)*: a tool's input, not the runtime's (jrt-convert's closure and checks stay
    as they are), under `jdk/` with the JDK's other forms and license.
- **JFS4 Where javac runs.**
  - Options: (a) in the shipped executable (Java text at the REPL; every build larger and
    slower); (b) a tool executable built on demand from the program and javac's forms, as the
    test build adds the fixtures, cached by key (`bin/arbace-go --build-java` or the script's
    `build`); (c) javac's forms interpreted by the class forms interpreter (no build; too slow
    for 838 files).
  - *Recommended: (b)*; (a) when Java at the REPL is wanted.
- **JFS5 javac's file manager.**
  - Options: (a) javac's own `JavacFileManager` over jrt's `java.nio.file` (the file system API
    filled in jrt); (b) an Arbace `JavaFileManager` (sources and class files from directories,
    no `jrt:/`, no zip) handed to `JavacTool.getTask`, `javac.ae` changed only there.
  - *Recommended: (b)* for the switch (it bounds R2 and changes no output: the file manager
    decides only where files come from); (a) if javac's command line is wanted.
- **JFS6 Arbace's runtime for the fixtures** (`arbace.lang.IFn` and the rest).
  - Options: (a) the checkout's `arbace/lang` printed to Java by `repr/java print` and put on
    javac's source path; (b) class-file stubs written from arbace.classes' class infos (a small
    writer: declarations, `Signature`, `ConstantValue`); (c) the frozen jar's classes read from
    the jar by javac on Arbace (needs `ZipFile`; the jar stays an input).
  - *Recommended: (a)*: no JVM artefact, and the declarations are the runtime the fixtures run
    against; if the fixtures' forms change at JF4, the change is reviewed and committed with it
    (R5).
- **JFS7 The converter's interface to its front end.**
  - Options: (a) `convert.ae` as it is, over javac's trees (with (b)); (b) an interface (the
    attributed tree's facts as a protocol) introduced so a native front end plugs in.
  - *Recommended: (a) now, (b) at JF9's start*: no change before there is a second front end.
- **JFS8 javac's version.**
  - Options: (a) jdk26u at `baf63fb`, the frozen JDK's and `jdk/forms`'; (b) javac moved with each
    jdk26u update, its forms regenerated by itself (after JF5).
  - *Recommended: (a)* until the JDK is upgraded for java.base too; then (b), in one change.
- **JFS9 The checks in the gate.**
  - Options: (a) at JF5, `bin/gate --full`'s steps `repr-java`, `repr-java-roundtrip`,
    `jrt-convert`, `java-fixtures` move to the tool executable, and the javac script's `verify`
    and `check` join them (`check` comparing with the frozen JVM's javac while it exists); (b) the
    same, plus a fast smoke in the essential gate (javac on Arbace compiles the samples).
  - *Recommended: (a)*; (b) only if the tool build is cached as cheaply as `--gate-build`.
- **JFS10 The native front end.**
  - Options: (a) not now (javac on Arbace is the Java front end); (b) the parser only (JF8),
    checked tree by tree; (c) the parser and the attribution (JF8, JF9), javac kept as the
    reference and the classes level's compiler.
  - *Recommended: (a) now, decided again after JF6*; then (b) first: a tree level over 12,730
    files is cheap, and the parser alone serves tools that need Java's syntax without
    attribution (an editor's view, a Java reader in the box without javac's world).
- **JFS11 The milestones' names.**
  - Options: (a) JF1-JF9 as here (PRINTER-NOTES' JF1-JF5 are decided choices of another
    document); (b) renamed JN1-JN9 before adoption.
  - *Recommended: (b)*, so that "JF3" names one thing in the repository.

## 10. Sources

- jdk26u: https://github.com/openjdk/jdk26u at `baf63fbe42b8758448fee570a9d9bb914272d259`
  (`/root/jdk26u`): `src/jdk.compiler/share/classes` (all of it measured;
  `com/sun/tools/javac/code/Source.java`, `Preview.java`, `comp/*.java`, `parser/*.java`, `jvm/*.java`),
  `src/java.compiler/share/classes`,
  `src/jdk.internal.opt/share/classes/jdk/internal/opt/CommandLine.java`,
  `src/java.base/{share,unix,linux}/classes`, `make/CompileJavaModules.gmk`,
  `make/common/JavaCompilation.gmk`, `make/jdk/src/classes/build/tools/`, the build tree's
  `build/linux-x86_64-server-release/support/gensrc/{jdk.compiler,java.base}`.
- Eclipse JDT core: https://github.com/eclipse-jdt/eclipse.jdt.core at
  `5e1198e83e6d8af17da1c570d1759675cf4f9036` (2026-10-20), `org.eclipse.jdt.core.compiler.batch`
  (`META-INF/MANIFEST.MF`, `about.html`, `src/org/eclipse/jdt/internal/compiler/`).
- JavaParser: https://github.com/javaparser/javaparser at
  `6dc10fcd35fc40630239f7f84c8980f7ad8268d5` (2026-10-19), release 3.28.2: `LICENSE*`,
  `FEATURES.md`, `javaparser-core/src/main/javacc/java.jj`,
  `javaparser-core/src/main/java/com/github/javaparser/ParserConfiguration.java`,
  `javaparser-symbol-solver-core/src/main/java/.../resolution/typeinference/`.
- tree-sitter-java: https://github.com/tree-sitter/tree-sitter-java at
  `e10607b45ff745f5f876bfa3e94fbcc6b44bdc11` (2025-09-14): `grammar.js`, `src/parser.c`,
  `LICENSE`.
- Go: TamaGo's go1.27.1 (`/root/tamago-go/src/go/types`, `cmd/compile/internal/types2`), for
  the size comparison.
- Arbace at `9a6a36e9`: `arbace/repr/java.ae`, `arbace/repr/java/*.ae`, `arbace/classes/*.ae`,
  `arbace/go/check.ae` and `check/`, `bin/repr-java`, `bin/lib/repr-java/roundtrip`,
  `bin/lib/frozen.bash`, `bin/jrt-convert`, `bin/j2g-edge`, `test/jrt/jrt_sources.ae`,
  `jdk/forms`, `jdk/sources.txt`, `jdk/provenance.edn`, `jdk/edge.edn`,
  `go/arbace/jrt/manifest.edn`, `test/java-fixtures/README.md`, `test/repr/java/`, LICENSE.md;
  [SPEC.md](SPEC.md) (§1, §5, §6, §7), [CONVERTER-NOTES.md](CONVERTER-NOTES.md),
  [PRINTER-NOTES.md](PRINTER-NOTES.md), [COMPILER-NOTES.md](COMPILER-NOTES.md),
  [../STANDALONE-NOTES.md](../STANDALONE-NOTES.md) (S4, O1, O2, "What stays on a JVM"),
  [../STANDALONE-SURVEY.md](../STANDALONE-SURVEY.md) (D12),
  [../go/SPEED-NOTES.md](../go/SPEED-NOTES.md),
  [../GO-FORMS-REPL-SURVEY.md](../GO-FORMS-REPL-SURVEY.md) (§6.3, effort by record).
- The last `bin/gate --full` run's outputs in main's `.tmp/` (2026-10-20): the conversion reports
  named above, `.tmp/gate/*.secs`, the round trip's logs (`.tmp/repr-java-roundtrip/`), the jdk
  corpus's originals (`.tmp/repr-java-roundtrip/jdk-originals/src`), the fixtures' Java
  (`.tmp/clojure-tests/java-fixtures/clojure/test/java`), the frozen baseline
  (`.tmp/frozen/clojure`, from the tag `arbace-for-java-26-v1`).

## Appendix A: the measurements

The scripts are in the survey's scratch, `.tmp/jfs/` (Python, run with `python3 -I`, reading the
files named; nothing in them is needed to read this survey):

- `attr.py FILE`: splits a namespace into its top-level forms (lines starting with `(`) and counts
  the non-blank, non-comment lines of the forms whose text matches javac's attribution accessors
  (`.type`, `.sym`, `.tsym`, `.constValue`, `.owner`, `.erasure`, `.getParameterTypes`,
  `.getReturnType`, `.varargsElement`, `.isSignaturePolymorphic`, `.baseSymbol`, `.operator`,
  `.completesNormally`, `.target`, `.flags`, `.kind`, `.patternSwitch`, `.hasUnconditionalPattern`,
  `.declaredUsingVar`, ...), `Type$`, `Symbol$`, `Flags/`, `TreeInfo/symbol`, or `jt/`, `res/`
  and the jtypes helpers (§2.1). The method call counts of §1.1 are a `grep -o '(\.[a-zA-Z]*'`
  over `convert.ae`.
- `heads.py DIR...`: a reader of Clojure's syntax sufficient for the converted files (strings,
  characters, comments, metadata, `#{}`, `#""`), counting every list by its head symbol, lists
  inside metadata excluded; `classify.py` groups the heads as in §2.2 (`unchecked-*`, `bit-*`,
  `unsigned-bit-*`, `==`, `identical?`, `=`, `java-str`, `cast` and the primitive conversion
  functions as typed; `.-f` field access; `.m` calls; `C/m` static members; the declaration
  heads; the rest syntax). Run over `jdk/forms`, the baseline's conversion
  (`.tmp/repr-java-check/baseline`) and `test/repr/java/expected/{java,module}`.
- `deps.py`: javac's 348 files with comments and strings removed; every explicit import, every
  on-demand import resolved against the simple names its package declares in jdk26u's
  `src/*/{share,unix,linux}/classes` (counted when the name occurs in the file), `java.lang`'s
  names occurring, and fully qualified names; the classes outside `jdk.compiler` grouped by
  module, then java.base's compared with `jdk/sources.txt` and `go/arbace/jrt/manifest.edn`
  (§4 (b)). An over-approximation for on-demand imports (a name occurring is not a use).
- The feature counts of §5: the reports' `:kind/`, `:form/`, `:mod/` counters, and `grep` over
  the originals for text blocks (`"""`, halved), `var` declarations, `<>`, `.<T>m(`, `\uXXXX`,
  `sealed`, `permits`, `yield`, guards (`case ... when`), `import module` and unnamed variables;
  the counts for javac's own sources likewise.
- Prior art: shallow clones in `.tmp/prior/` (`jp`, `tsj`, `ecj` with a sparse checkout of
  `org.eclipse.jdt.core.compiler.batch`), measured with `find ... -name '*.java' | wc -l` and
  `wc -l`.

## Decided (2026-10-20)

The user decided JFS1-JFS11 as recommended:
- JFS1 (b): javac as Java forms, run by Arbace, to the switch; a native front end later on its
  checks.
- JFS2 (a): java.base and java.xml compiled once by javac on Arbace, cached.
- JFS3 (b): javac's forms in `jdk/javac/`, with `bin/javac-vendor convert|verify|check`.
- JFS4 (b): a tool executable built on demand, cached.
- JFS5 (b): an Arbace `JavaFileManager`.
- JFS6 (a): the fixtures' runtime printed from the checkout's `arbace/lang`, any change to their
  forms reviewed.
- JFS7 (a): the converter's interface unchanged until a native front end starts.
- JFS8 (a): javac pinned to jdk26u `baf63fb`.
- JFS9 (a): the repr/java `--full` steps move to the tool executable.
- JFS10 (a): no native front end now; decided again after JN6, parser first.
- JFS11 (b): the milestones are named JN1-JN9; read this document's JF1-JF9 as JN1-JN9.

## As built: JN1 (2026-10-20)

JN1, the spike of JFS1 (b), on the branch `java-frontend-jn1` at main `26816f48`, in about two
hours of work: javac runs on Arbace. javac's sources read by repr/java, translated by j2g with
jrt into a tool executable, parse every corpus and attribute the baseline with output equal to
javac on the frozen JVM. Everything below is scratch in the worktree's `.tmp/jn1/` (sources,
forms, scripts, logs, the executable); nothing of it is committed but this section. The shipped
program, jrt, `overlay/jdk`, j2g and `jdk/` are unchanged on the branch. The changes the spike
needed were made to copies, or made to the checkout and then reverted after their diff was
saved; each one is listed below (§JN1.5). jdk26u is the checkout at `baf63fbe42b8` (JFS8 (a)).
The JVM ran only the frozen v4 jar (copied into the worktree's `.tmp/oracle-frozen/`, hash
checked against `test/oracle/frozen-jar.sha256`), through `bin/repr-java` and
`bin/lib/frozen.bash`. Go ran only TamaGo's go1.27.1, with no cgo. The tools' host was the
worktree's own stage 0 (`bin/seed --build`).

### JN1.1 javac's sources read (step 1)

`bin/repr-java read-jdk --check` on the frozen JVM, run once per module, each module's sources
copied into `.tmp/jn1/src/<module>`: jdk.compiler's `share/classes` plus the 17 generated
sources of `build/linux-x86_64-server-release/support/gensrc/jdk.compiler/com` (CompilerProperties,
LauncherProperties, FlagsEnum, and the bundles `compiler`, `javac`, `launcher` with their `_de`,
`_ja` and `_zh_CN` translations, plus `ct` and `version`; serialver's 4 bundles left out),
java.compiler's `share/classes`, and jdk.internal.opt's `CommandLine.java`:

| module | Java files | converted | failed | with Java forms | forms | time (12 threads) |
|---|---:|---:|---:|---:|---|---:|
| jdk.compiler (348 + 17 generated; `module-info` skipped) | 364 | 364 | 0 | 359 | 6,507,455 bytes, 127,224 lines, 384 files | 18 s |
| java.compiler (136; `module-info` skipped) | 135 | 135 | 0 | 129 | 164,924 bytes, 141 files | 6 s |
| jdk.internal.opt (`CommandLine`) | 1 | 1 | 0 | 1 | 7,672 bytes | 8 s |

That is 500 files with no failures, 489 of them with Java forms, 6.7 MB of forms. §4 (b)
estimated "about 4 MB". The 2026-10-07 counts hold: jdk.compiler had 342 files with forms, and
there are 359 now, the extra 17 being the generated ones. java.compiler has 129, as before. The
5 + 6 files without forms are `package-info`s. Every output reads back with `arbace.core/read`.
After the spike's patches (§JN1.5) jdk.compiler's 365 files still convert, but only with
`REPR_JAVA_PATCH_ALL=1`, as `bin/jrt-convert` runs it: a new class (`JN1Bundles`) is named
across `read-jdk`'s chunks.

### JN1.2 The closure, measured (step 2)

Two measures. The first is static: `scripts/closure.py` scans the forms' import lists, qualified
symbols and bare `java.lang` names. javac's forms name 236 top-level java.base classes. 158 are
in `jdk/forms`, 21 are jrt's hand-written classes, 26 are declared only in `jdk/edge.edn` (no
code), and 36 are absent. This is the survey's "171 in the world, 67 missing", recounted over
the forms instead of the Java text.

The second is j2g's own answer, which decides. j2g ran with `--program` (the test build's
roots: every public member of every REPL-visible class) over the program plus javac's world.
The shipped program translates 14,463 classes. The tool translates 16,128: the same 14,463,
1,646 of javac's world, and **19 java.base classes added to the closure**. That is all javac
needs beyond what jrt already translates, because the shipped program already roots all of
java.base's closure. Reached methods grow from 68,355 to 82,648. j2g's report then lists **69
methods of javac's world with missing parts**, which become operation-level stubs (J2G-SPEC
§4.1). The gap list, divided as step 2 asked (`.tmp/jn1/gaps.edn`, `closure-translated.txt`):

| what | classes or members | needed for | in JN1 |
|---|---|---|---|
| **added to jrt's closure** (translated) | `java.lang.annotation.{Documented, Inherited, Repeatable, AnnotationTypeMismatchException}`, `SafeVarargs`, `java.io.{IOError, FilterWriter}`, `java.lang.module.InvalidModuleDescriptorException`, `java.lang.runtime.ExactConversionsSupport` (TypeTag's constant range checks, reached by every attribution), `java.nio.file.{FileSystem, FileVisitOption, FileVisitResult, ClosedFileSystemException, DirectoryIteratorException, FileSystemNotFoundException, ProviderNotFoundException, ReadOnlyFileSystemException}`, `java.util.spi.ToolProvider`, `sun.reflect.annotation.ExceptionProxy` (19 classes, all small leaves) | declarations of javac's classes: an annotation type or a supertype missing fails a whole class's analysis | converted by `read-jdk java.base` into an extra input tree (`.tmp/jn1/add-tree`) |
| **members jrt lacks**, declared | `java.nio.file.Files.{list, walk, find, walkFileTree, readAttributes, isSymbolicLink, readSymbolicLink, getLastModifiedTime}`, `Path.getFileSystem` | javac's `file` package (`JavacFileManager`, `PathFileObject`, `Locations`, `JRTIndex`, `CacheFSInfo`, `RelativePath`): a class whose code names a member that does not exist fails its analysis, so it is not translated, and its users then fail (`ClassFinder` holds a `JRTIndex` field) | stubs that throw, in a copy of jrt's `Files.java` and `Path.java` converted into a copy of `jdk/forms` (`.tmp/jn1/jdk/forms`; the unmodified copies convert byte for byte as `jdk/forms` has them) |
| **cut in javac** (or added to jrt, JFS12) | `ResourceBundle.getBundle` (JavacMessages, JavaCompiler's version, JRTIndex), `ServiceLoader` (DocLint, plugins, processors, `--release`'s platform provider, doc comment transformers), `Runtime.version()` (`SourceVersion.<clinit>`), `Throwable.printStackTrace(PrintWriter)` (javac's crash report), `InputStreamReader(InputStream, CharsetDecoder)` (`PathFileObject`) | the task's setup: reached by the first parse | patched in javac's sources (§JN1.5) |
| **avoided by the Arbace file manager** (JFS5 (b)) | `FileSystems`, `FileSystemProvider`, `FileStore`, `PathMatcher`, `WatchService`, `BasicFileAttributes`, `FileTime`, `FileVisitor`, `SimpleFileVisitor`, `JarFile`, `ZipFile`, `Manifest`, `Attributes`, `JmodFile`, the module system (`Module`, `ModuleLayer`, `ModuleFinder`, `ModuleReference`, `ModuleReader`, `ModuleDescriptor`, `Configuration`), `CharsetDecoder`, `CoderResult`, `CodeSource`, `ProtectionDomain` | `JavacFileManager`, `Locations`, `JRTIndex`, the source launcher | declared only, from the edge recorded on the JVM (below); never reached at run time |
| left as stubs, not reached by `parse`/`analyze` | `java.lang.annotation.Annotation` (17 methods: `getAnnotation` for processors), `DataOutputStream` (`ByteBuffer.appendFloat`, `appendDouble`, `appendLong`: **ClassWriter's**, needed by JN3), `Runtime$Version`, `VM.getRuntimeArguments`, `sun.reflect.annotation.*` | processors, class files, `--release` | stubs |

One more gap belongs to j2g's design. **The edge must cover javac's world.** With
`jdk/edge.edn` alone, 85 classes failed their analysis, because a class neither in the forms nor
in the edge "does not exist" (`FileSystem`, `CharsetDecoder` and `ServiceLoader$Provider` in
signatures). `.tmp/jn1/edge.sh` records the edge of javac's world on the frozen JVM, which is
`bin/j2g-edge`'s `record` for a third world, 30 s. Merged with `jdk/edge.edn` it adds 29 classes
(416 against 387), and the failures drop to 13. All 13 are in `com.sun.tools.javac.launcher`
(the source launcher), `sun.tools.serialver`, and the parts of the `file` package the stubs
above leave failing. None is reached.

### JN1.3 The minimal program (step 3)

**The driver.** `jn1.Main` (126 lines of Java) uses javac's public API (`JavacTool.create`,
`JavacTask.parse`, `analyze`; `com.sun.source.util.TreePathScanner`; `JavacTrees`). It runs
over `jn1.Files` (264 lines), a first Arbace `JavaFileManager` after JFS5 (b): sources and class
files from directories, no `jrt:/`, no zip, an exploded module directory served as the system
module `java.base` (`listLocationsForModules`, `getLocationForModule`, `inferModuleName`).
`parse` prints every unit as javac's `Pretty` prints it (`JCTree.toString`). `analyze` also
prints every expression's kind, type and element (`Trees.getTypeMirror`, `getElement`) and the
diagnostics. The same Java forms run on both sides:
- on the JVM, compiled by the frozen jar's own Java forms compiler (`test/repr/java/build.ae`),
  run by `frozen_java` against the JDK's javac (`.tmp/jn1/jvm.sh`);
- on Go, translated by j2g into the tool executable with javac's forms (`.tmp/jn1/go.sh`).

**The tool executable** (JFS4 (b)): the test build's recipe (`bin/arbace-go --build-tests DIR`:
j2g `--program --input`, `bin/build-go`, the image), run by `.tmp/jn1/{j2g,build}.sh` with
javac's world, the 19 added classes, the patched copy of `jdk/forms` and the merged edge, into
`.tmp/jn1/bin/arbace-javac`:

| | shipped executable (amd64) | tool executable |
|---|---:|---:|
| size (stripped, `-s -w`) | 143,749,280 bytes | **183,373,984 bytes** (+39.6 MB, +28%) |
| classes translated, methods reached | 14,463, 68,355 | 16,128, 82,648 |
| j2g's program tree | | 181 MB of Go forms, javac's 478 files 24 MB |
| j2g on stage 0 (the compiled namespaces already made, 76 s once) | | **76-88 s** |
| `bin/build-go` (Go's build cache warm for the runtime) | | **173-195 s** |
| the image and the link | | 8-10 s |

About ten builds were made in the spike, about 5 minutes each, each one after a fix.

**Results.** Every comparison below is byte for byte, on javac's output as the driver prints it:

| run | javac on the frozen JVM | javac on Arbace (Go) | equal |
|---|---:|---:|---|
| parse the samples (`test/repr/java/java/sample`, 10 files, 580 lines printed) | 204 ms | 8 ms | **yes** |
| parse the baseline (the frozen `clojure/`, 183 files, 51,755 lines) | 628 ms | 308 ms | **yes** |
| parse javac's own sources (jdk.compiler and java.compiler, 482 files, 160,225 lines) | 947 ms | 923 ms (1.5 s wall, 519 MB) | **yes** |
| parse java.base's closure (the round trip's 838 originals, 380,024 lines) | 1,585 ms | 2,458 ms (3.6 s wall, 837 MB) | **yes** |
| analyze `Reduced.java` (with a one-method `IDeref`, against the exploded java.base) | 191 ms | **88 ms** | **yes** |
| analyze `Reduced.java` with the baseline as source path (`IDeref` names `RT`: clojure.lang attributed implicitly) | 2,684 ms | 3,928 ms | yes, but capture numbers |
| analyze the samples (`--enable-preview -source 26`, 2,173 lines) | 432 ms | 166 ms | yes, but capture numbers |
| analyze the baseline (183 files, 812 classes, 217,098 lines of facts) | 2,313 ms (5.1 s wall, 650 MB) | **3,933 ms** (5.2 s wall, 612 MB) | yes, but capture numbers |

The JVM's times are javac's own, measured in the process after the frozen runtime's start
(about 1.5 s), and cold: HotSpot's warm-up is part of them. Go's are the whole run's (the
executable starts in about 0.1 s). In "capture numbers" the only differences are javac's names
for captured types (`capture#780 of ?`). javac computes them from identity hash codes, and they
differ between two runs on the JVM too (10 lines between two JVM runs of the samples). Once they
are normalized, every output is equal. So javac attributes on Arbace about 1.7 times slower than
the cold JVM, which is the bottom of §4 (b)'s range (2.3 to 32 times for j2g's code), and it
parses at the JVM's speed. R1 (Arbace runs Java not as the JVM) showed nothing over these
corpora: no difference in trees, types, symbols, diagnostics or order.

### JN1.4 The platform's declarations, tried (step 4, JFS2)

On the JVM, with the same driver and file manager, attributing `Reduced.java`:

| option | how | result |
|---|---|---|
| exploded classes as the system module (JFS2 (a)'s output) | `--platform` the JDK build tree's `build/linux-x86_64-server-release/jdk/modules/java.base`, served as `SYSTEM_MODULES` / `java.base` | **works** on the JVM and on Arbace: all of §JN1.3's analyses |
| java.base's sources on the source path, modules (JFS2 (b)) | jdk26u's `src/java.base/share/classes` as `SOURCE_PATH`, no system module | **fails**: javac aborts in `TypeEnter$ImportsPhase.implicitImports`, since without a system `java.base` there is no `java.lang`. A source path cannot stand for the system module, and `--patch-module` needs one to patch |
| the same with `-source 8` (no modules) | sources on the source path, platform class path empty | fails: java.base's sources use Java 16-25 (100 errors: patterns, switch expressions, sealed classes), and attributing what is reached took 9.9 s |
| exploded classes on the platform class path, `-source 8` | `PLATFORM_CLASS_PATH` | works (67 ms), but at language level 8: not the corpora's |

So JFS2 (a) holds, and the source-path fallback of JFS2's recommendation does not work for
module-aware javac. The class files javac needs (it reads them lazily, through its own
`ClassReader`, translated) must come from an exploded java.base. For JN2-JN3 the build tree's
exploded classes can stand in until javac on Arbace compiles java.base itself (JFS15).

### JN1.5 What the spike changed, exactly

None of these is committed. The diffs are in `.tmp/jn1/patches/`.

**javac's sources** (copies in `.tmp/jn1/src/`; `jdk.compiler.diff`, 94 lines added and 32
removed in 11 files; `java.compiler.diff`, 1 line):
1. `util/JN1Bundles.java` (new) maps javac's five bundle names to `new compiler()` and the rest.
   It is used by `JavacMessages` (2 calls), `JavaCompiler.version`'s bundle and `JRTIndex`, in
   place of `ResourceBundle.getBundle` (jrt's `ResourceBundle` loads nothing).
2. `DocLint.newDocLint` returns `NoDocLint`, and `BasicJavacTask.initPlugins` returns when there
   are no plugin options. On the JVM `ServiceLoader` finds nothing either; on Arbace it is outside
   the world.
3. `JavacTaskImpl`: the crash report prints the stack trace line by line
   (`Throwable.printStackTrace(PrintWriter)` is not in jrt).
4. `PathFileObject.openReader`: `new InputStreamReader(in, decoder.charset())`.
5. `javax.lang.model.SourceVersion.getLatestSupported`: `intVersion = 26` in place of
   `Runtime.version().feature()`.
6. Workarounds for the analysis and j2g bugs below: `Attr.RecoveryInfo`'s anonymous check
   context moved out of the constructor's prologue into an `Attr` method (arbace.classes: "No
   enclosing instance of type Attr in scope"); `Types.DescriptorCache.mergeDescriptors`' lambda
   given a cast to `FunctionDescriptor` ("Unknown type: Types$DescriptorCache$1", a lambda whose
   inferred return type is an anonymous class); `Resolve.onDemandImportScopeRecovery`'s lambda
   body moved into a method (j2g typed the lambda's last temporary `any`: a Go compile error);
   `Resolve.MethodReferenceLookupHelper.unboundLookup`'s anonymous helper moved into a `Resolve`
   method (j2g read `Resolve.this` through the outer parameter's interface type: a Go compile
   error).

**jrt** (a copy of `overlay/jdk/java.base/java/nio/file/{Files,Path}.java`; `overlay-nio-file.diff`,
43 lines): the members listed in §JN1.2, which throw `UnsupportedOperationException("jn1: ...")`.

**j2g** (`j2g.diff`: `arbace/j2g/names.ae`, `code.ae`, `reach.ae`, 33 lines added, 5 removed;
made in the checkout, used by the tool's j2g runs, then reverted):
1. `names.ae`: classes of javac's packages (`com/sun/source/`, `com/sun/tools/`, `javax/lang/`,
   `javax/tools/`, `javax/annotation/processing/`, `sun/tools/`, `jdk/internal/opt/`, the driver)
   are named by their binary name, `Jv_com_sun_tools_javac_util_List`. j2g names a class by its
   simple name in one Go package, `jrt`, with a hand-written rename table for collisions (§4.4).
   javac's world made 462 collisions: with java.base (`List`, `Type`, `Name`, `Flow`, `Modifier`,
   `ByteBuffer`, `Formattable`), within itself (`com.sun.source.tree.ReturnTree` and
   `doctree.ReturnTree`, the two `Scope`s, the two `TreeScanner`s, the two `Main`s), and in
   method names through the descriptor codes (`GetAnnotationMirrors__List` for
   `com.sun.tools.javac.util.List` and `java.util.List`). Also the rename
   `java/nio/file/FileSystem` → `Nio_FileSystem` (against `java.io.FileSystem`).
2. `code.ae` `cast-val`: a cast to a class outside the world (a class that failed its analysis,
   as the binding of an `instanceof` pattern) is an operation-level stub. Before, it emitted a
   `C_Cast` that does not exist.
3. `code.ae`: `Object.wait(long)` on an Object-typed receiver called `jrt.WaitTimeout` with two
   arguments, and it takes three (millis, nanos). The nanos 0 are now added, and `wait(long, int)`
   is in the helpers' table.
4. `reach.ae`: a bound method reference to a private method (`this::initOperators` in
   `Operators.OperatorHelper`, an abstract class) is reached directly. As a virtual call no
   instantiated class overrode it, so it stayed a "not translated" stub.

The shipped program was translated by j2g with these changes and without them (the shipped
world, `.tmp/jn1/ship-{old,new}`). The trees differ in one method and in j2g's own embedded
sources: `java.util.concurrent.TimeUnit.timedWait`, a "not translated" stub before (fix 3), is
translated now. Fixes 1, 2 and 4 do not change the shipped program.

### JN1.6 What it means for JN2-JN3

- **JN2 (javac's forms vendored) is smaller than estimated.** The conversion is clean (500 files,
  no failures, 6.7 MB). jrt's additions are 19 small classes plus about ten declared members, not
  the 67 classes §4 (b) counted, because the Arbace file manager keeps the file systems, jars and
  the module system out. The work: the script (JFS3 (b)); javac's world in the edge (JFS14); j2g's
  naming for a second world (JFS13); the fixes of JN1.5 made properly (JFS12, JFS16). About
  **1-2 working days**, against 2-3.
- **JN3 (javac runs on Arbace) has its hardest part done.** The tool builds, javac parses and
  attributes, and the outputs are the JVM's. What remains for JN3 is new ground: class files
  (`Gen`, `ClassWriter`: `DataOutputStream` is still a stub, and the file manager has no output),
  `--patch-module` for `read-jdk` (a module-oriented location in the file manager), java.base
  compiled by javac on Arbace (the JFS2 (a) cache), byte-for-byte checks against the JVM's javac
  for the baseline's 812 classes and the closure's 2,224, and the processors' paths
  (`Annotation`, `sun.reflect.annotation`) if a corpus needs them. Speed is not the risk §8 R3
  feared: 1.7 times the cold JVM for attribution, and equal for parsing. The estimate is **3-4
  working days**, against 4-6.
- **JN4 needs javac's internals rooted.** With the recorded edge, `com.sun.tools.javac.*` is a
  package jdk.compiler does not export, so `--program` roots only javac's public API
  (`com.sun.source`, `javax.*`). The driver reached `JavacTrees` directly, because `Trees.instance`
  finds it by reflection and hit an unrooted method. repr/java's converter calls
  `com.sun.tools.javac.tree`, `code` and `comp` from Arbace code, so the tool must root them too
  (JFS17). That makes the tool larger than the 183 MB measured here.
- **The tail is j2g's and arbace.classes'.** Eight problems showed in one spike: six in j2g
  (four fixed in j2g, two worked around in javac's sources) and two in the analysis (worked
  around in javac's sources). All are on Java shapes java.base's closure does not use. javac is a good corpus for
  them, as `bin/diffcheck` is for the forms.

### JN1.7 Proposals

- **JFS12 Where javac's adaptations live.**
  - Options: (a) javac's sources patched, as in JN1: an `overlay/jdk/jdk.compiler/` and
    `java.compiler/` tree copied over jdk26u's files before the conversion, the way
    `overlay/jdk/java.base` is; (b) j2g variants of javac's classes (forms, as
    `overlay/jdk/variants`); (c) none in javac: jrt grows what is the JDK's API
    (`ResourceBundle.getBundle` over the world's bundle classes, a `ServiceLoader` that finds no
    provider, `Runtime.version`, `Throwable.printStackTrace(PrintWriter)`,
    `InputStreamReader(InputStream, CharsetDecoder)`), and the JN1.5-6 workarounds are replaced
    by fixes in arbace.classes and j2g.
  - *Recommended: (c)*, with (a) only for what can be fixed nowhere else. javac's forms then
    stay jdk26u's text converted, so JN5's fixpoint compares javac's own sources unpatched, and
    an upgrade of javac is a conversion. jrt's additions change the program and are batched
    with a re-seed.
- **JFS13 j2g's names for the tool's world.**
  - Options: (a) as JN1: the classes of the tool's modules (jdk.compiler, java.compiler,
    jdk.internal.opt) named by their binary name, `Jv_...`, with java.base's names unchanged;
    (b) a binary name for every class whose simple name collides, computed from the world, so
    that adding a class can rename the shipped program's; (c) one Go package per Java package
    for the tool's world.
  - *Recommended: (a)*: the shipped program keeps its names byte for byte, it is a rule rather
    than a table, and the tool's names never depend on what else is in the world. (c) is a
    larger j2g change for no gain in the tool.
- **JFS14 The edge of javac's world.**
  - Options: (a) `bin/j2g-edge update` records a third world (the program with javac's forms)
    into `jdk/edge.edn`; (b) a `jdk/javac/edge.edn` of its own, recorded by the javac script
    (JFS3 (b)) and merged into the tool build's edge.
  - *Recommended: (b)*: the tool's input stays beside javac's forms, and `jdk/edge.edn` and the
    shipped build stay unchanged. After JN5 it is still JVM data, as `jdk/edge.edn` is (§1.3).
- **JFS15 The platform until javac on Arbace compiles java.base.**
  - Options: (a) JN3 starts with java.base compiled by javac on Arbace (JFS2 (a)); (b) the
    JDK build tree's exploded `jdk/modules/java.base` (and `java.xml`) as the platform for JN2
    and the start of JN3, pinned by a hash of its class files, replaced by JFS2 (a)'s output
    once that is shown byte-identical to it.
  - *Recommended: (b)*. It is what JN1 ran on, and it separates "javac runs" from "javac
    compiles java.base equal to the JDK build", which is the larger check.
- **JFS16 The j2g fixes found.**
  - Options: (a) land JN1.5's j2g fixes 2-4 now, as a j2g change through the essential gate:
    the shipped program changes in one method, `TimeUnit.timedWait`, which is translated
    instead of a stub; (b) land them with JN2, with fix 1 (JFS13).
  - *Recommended: (b)*: no user of the shipped program reached `timedWait`, and JN2 brings the
    corpus that tests the fixes. Then each lands with a regression form in `bin/diffcheck`'s
    corpora.
- **JFS17 What the tool roots.**
  - Options: (a) javac's internal packages rooted too (`com.sun.tools.javac.*`), for
    repr/java's converter at JN4: the edge's `:unexported` not applied to the tool's own world;
    (b) only the classes and members `arbace.repr.java`'s namespaces name (a root list made from
    their imports, after `--root`), as the shipped program roots what the REPL needs; (c) only
    javac's API, with the converter rewritten against `com.sun.source` (JFS7 (b)).
  - *Recommended: (a) for JN4*, measured then. If the tool grows by more than about a third,
    move to (b). (c) is JFS7's question, left for a native front end.

### JN1.8 Sources and scratch

- jdk26u at `baf63fbe42b8758448fee570a9d9bb914272d259` (`/root/jdk26u`): `src/jdk.compiler`,
  `src/java.compiler`, `src/jdk.internal.opt/.../CommandLine.java`, `src/java.base` (the 21
  classes of JN1.2, read), and the build tree's `support/gensrc/jdk.compiler` and
  `jdk/modules/java.base`.
- Arbace at `26816f48` on the branch `java-frontend-jn1`: `bin/repr-java`, `bin/arbace-go`
  (`--build-tests`, whose steps `.tmp/jn1/{j2g,build}.sh` repeat), `bin/build-go`, `bin/j2g`,
  `bin/j2g-edge` (the recording, in `.tmp/jn1/edge.sh`), `test/repr/java/build.ae`, the frozen
  v4 jar.
- The worktree's `.tmp/jn1/`: `src/` (the modules' sources, patched), `conv/` (the forms),
  `world/` and `add-tree/` (j2g's inputs), `jdk/forms` (the patched copy), `edge.edn`,
  `driver/` (`jn1.Main`, `jn1.Files`), `scripts/closure.py` and `gaps.py`, `closure.edn`,
  `gaps.edn`, `closure-translated.txt`, `patches/`, `bin/arbace-javac`, and the outputs
  compared (`jvm-*.txt` and `go-*.txt`).

## Decided (2026-10-20): JFS12-JFS17

The user decided JFS12-JFS17 as recommended:
- JFS12 (c): no patches in javac. jrt grows the JDK's API javac uses, and JN1's workarounds become
  fixes in j2g and arbace.classes (batched with a re-seed). javac's own sources may be patched only
  for what can be fixed nowhere else.
- JFS13 (a): binary names (`Jv_...`) for the tool's modules; java.base's and the shipped names
  unchanged.
- JFS14 (b): javac's edge in `jdk/javac/edge.edn`, recorded by the javac script.
- JFS15 (b): the JDK build tree's exploded java.base and java.xml as the platform for JN2 and the
  start of JN3, pinned by hash, until javac on Arbace compiles them identically.
- JFS16 (b): JN1's j2g fixes land with JN2, each with a regression form in `bin/diffcheck`'s
  corpora.
- JFS17 (a): javac's internal packages rooted in the tool for JN4, measured; (b) if the tool grows
  by more than about a third.
