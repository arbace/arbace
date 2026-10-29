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
spike, as built: "As built: JN1 (2026-10-20)", after it; JN2, javac's forms vendored: "As built:
JN2 (2026-10-20)", with JFS18-JFS22; JN3, javac runs on Arbace: "As built: JN3 (2026-10-21)", with
JFS23-JFS26; JN4, repr/java's `read` on Arbace: "As built: JN4 (2026-10-21)", with
JFS27-JFS30; JN5, the fixpoint and the checks moved: "As built: JN5 (2026-10-21)", with
JFS31-JFS36; JN6, the JVM leaves repr/java: "As built: JN6 (2026-10-21)", with
JFS37-JFS42; JN7, the generators on Arbace: "As built: JN7 (2026-10-22)", with
JFS43-JFS50; JFS48 (b), javac's generated sources on the tool: "As built: JFS48 (b) (2026-10-22)",
with JFS51-JFS54; JFS52 (b), `FlagsEnum` on the tool: "As built: JFS52 (b) (2026-10-22)", with
JFS55-JFS58; JN8, the native parser: "As built: JN8 (2026-10-23)", with JFS59-JFS66; JFS66 (b), the
converter on an interface over both front ends and the split measured: "As built: JFS66 (b)
(2026-10-23)", with JFS67-JFS71; JFS70 (b), the pins independent of the javac run's history: "As
built: JFS70 (2026-10-23)", with JFS72; JFS65 (b), j2g's checked array casts as a type switch: "As
built: JFS65 (b) (2026-10-24)", at the end, with JFS80-JFS82; JN9a-JN9e, the native attribution
provider and the read without javac: "As built: JN9a (2026-10-24)" to "As built: JN9f (2026-10-27,
2026-10-28)", with their proposals to JFS134 (the provider's reference: [NATIVE-ATTR.md](NATIVE-ATTR.md)).

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

## As built: JN2 (2026-10-20)

JN2, javac's forms vendored, on the branch `java-frontend-jn2` from main `52d8c504`, with main
merged in at `58806d78` (GP2) before the final checks. javac is committed as Java forms, jdk26u's
text converted with no patch in it (JFS12 (c)); what JN1 patched into javac is now jrt's, j2g's or
the analysis's. javac's world translates with no class failing its analysis, and JN1's driver,
run on the tool executable built from the committed forms, gives javac on the frozen JVM's
output, as in JN1. The JVM ran only the frozen v4 jar (through `bin/repr-java`,
`bin/lib/frozen.bash`); Go only TamaGo's go1.27.1, without cgo; the tools' host was the worktree's
stage 0, and, for j2g's analysis of javac's world, the program built from the checkout (below).

### JN2.1 javac's forms: `jdk/javac/` and `bin/javac-vendor`

`bin/javac-vendor` (modelled on `bin/toolchain-vendor` and `bin/jrt-convert`):

- `convert`: the sources copied from jdk26u (`JDK`, pinned: its commit must be `baf63fb`), read by
  `bin/repr-java read-jdk --check --origins` on the frozen JVM module by module, merged into
  `jdk/javac/forms`, with `sources.txt` (`MODULE KIND PATH`: `share`, `gensrc`, `overlay`),
  `provenance.edn` (jdk26u's commit and cleanliness, the build tree's gensrc, the JDK, repr/java's
  reader hash and the frozen host, jdk/edge.edn's hash, the counts), then the edge;
- `edge`: javac's edge alone (JFS14 (b)): `bin/j2g --program --record-edge` on the frozen JVM over
  the program and javac's forms with the tool's variants, less what `jdk/edge.edn` answers alike
  (`arbace.j2g.edge`'s new `subtract`), into `jdk/javac/edge.edn`;
- `verify`: `convert` into `.tmp/javac-vendor/fresh`, `diff -r` with `jdk/javac`;
- `check`: j2g translates javac's world (`--program`, javac's forms, the variants, both edges):
  status 0 and no class failing its analysis;
- `build`: `bin/arbace-go --build-java` (below).

Makefile: `javac-verify` (`verify`, `check`, in `make verify`) and `build-java`.

| | files | bytes |
|---|---:|---:|
| sources (`sources.txt`) | 524: jdk.compiler 368 (347 of `share/classes`, 21 of the build's `support/gensrc/jdk.compiler`), java.compiler 135, jdk.internal.opt 1 (`CommandLine`), java.base 20 (19 of jdk26u, 1 of `overlay/jdk/javac`) | |
| `jdk/javac/forms` | 561 | 6,684,582 |
| `jdk/javac/edge.edn` | 35 classes beyond `jdk/edge.edn`'s | 77,522 |

The conversion takes 77-84 s (repr/java 16 s for jdk.compiler, 2 s java.compiler, the edge's
recording 33-41 s). `module-info.java` and java.compiler's javadoc `snippet-files` are left out, as
`read-jdk` leaves them. The generated sources are copied from the build tree (JFS20).

### JN2.2 What javac's world needs of the JDK, and where it lives (JFS18, JFS19)

JN1 patched javac in 11 files. JN2 patches none; each patch became:

| JN1's patch | JN2 |
|---|---|
| `JN1Bundles` for `ResourceBundle.getBundle` (JavacMessages, JavaCompiler.version, JRTIndex) | jrt (shipped): `overlay/jdk/java.base/java/util/ResourceBundle.java`'s `getBundle(String)` and `getBundle(String, Locale)`, the JDK's search with the default Control (candidate and fallback locales, the parents chained, a base bundle alone last), each bundle made by `Class.forName` and the public no-argument constructor; j2g roots every concrete `ResourceBundle` subclass's constructor (`main.ae`, `bundle-roots`); javac's messages are found as on the JVM |
| `SourceVersion`: `Runtime.version()` | jrt (shipped): `java.lang.Runtime$Version`, hand-written (`go/arbace/jrt/system.ae`: feature 26, `interim`, `update`, `patch`, `compareTo`, `equals`, `hashCode` and `toString` as the JDK's), `Runtime.version()`; the manifest, its test and the member tables regenerated (`bin/jrt manifest`); a jrt test |
| `JavacTaskImpl`'s crash report: `printStackTrace(PrintWriter)` | j2g (shipped): calls of `Throwable.printStackTrace(PrintWriter)` and `(PrintStream)` are j2g's support functions `J2g_PrintStackTraceWriter`, `Stream` (which existed for the REPL's reflection; `model.ae`, `throwable-print-helper`) |
| `DocLint.newDocLint`, `BasicJavacTask.initPlugins`: no `ServiceLoader` | javac's world: `overlay/jdk/javac/java.base/java/util/ServiceLoader.java`, a ServiceLoader that finds no provider, converted into `jdk/javac/forms` |
| `PathFileObject.openReader`: `InputStreamReader(InputStream, CharsetDecoder)` | javac's world: a j2g variant, `overlay/jdk/javac/variants/InputStreamReader.ae` |
| the copies of jrt's `Files` and `Path` with the members javac's file package names | javac's world: variants `Files.ae` (`list`, `walk` x2, `find`, `walkFileTree` x2, `readAttributes`, `isSymbolicLink`, `readSymbolicLink`, `getLastModifiedTime`: they throw) and `Path.ae` (`getFileSystem`) |
| the 19 java.base classes of JN1.2 (`add-tree`) | javac's world: converted from jdk26u by `bin/javac-vendor` (module java.base in `sources.txt`) |
| the four workarounds of the analysis's and j2g's bugs (Attr, Types, Resolve x2) | fixes (JN2.3) |

The split is JFS18's: what javac alone needs, and whose types are outside the shipped world
(`FileSystem`, `CharsetDecoder`, `FileVisitor`, `BasicFileAttributes`, `ServiceLoader$Provider`),
is in javac's world, because in the shipped closure it would change `jdk/edge.edn` (the classes the
shipped world's j2g asks the JVM about) and root unused classes in every shipped build. The tool
build reads it: `bin/j2g`'s new `--variants DIR` (more Go-build variant files) and repeatable
`--edge` (the union of the edges). jrt's shipped additions still change `jdk/edge.edn` in two
entries (JFS19): `Runtime$Version` is no longer a cut class (its `:cut` entry left), and the
three-argument `getBundle` that `sun.util.resources.LocaleData` calls, which jrt lacks, is now
looked up among `getBundle` overloads in the forms (HotSpot's method order recorded:
`:method-order`). `bin/j2g-edge update` recorded them; `jdk/edge.edn` otherwise holds no answer of
javac's world.

The runtime key (`bin/lib/version.bash`) leaves out `jdk/javac` and `overlay/jdk/javac`, and
`test/jrt/jrt_sources.ae`'s overlay sources leave out `overlay/jdk/javac`: they are the tool's.

### JN2.3 j2g's and the analysis's fixes, with their regression forms

Each has a case in j2g's fixtures, `test/j2g/fixtures/FxJavac.ae` (recorded on the frozen JVM into
`test/j2g/fixtures.edn`) and `FxJavacShapes.ae` (the two shapes the frozen jar's own analysis does
not take: their results are kept by hand in `test/j2g/record_fixtures.ae`, JFS21), which
`bin/diffcheck` runs interpreted against translated:

| fix | where | regression case |
|---|---|---|
| binary names `Jv_...` for the tool's modules (JFS13 (a)): the packages of jdk.compiler, java.compiler and jdk.internal.opt; `--qualify PREFIX` adds a driver's | `names.ae` (`tool-module-prefixes`, `qualified?`); the rename `java/nio/file/FileSystem` → `Nio_FileSystem` | the tool build (0 name collisions); the shipped names are unchanged (JN2.5) |
| a cast to a class outside the closed world (an `instanceof` pattern's binding): an operation-level stub | `code.ae` `cast-val` | none in the fixtures: the interpreted half cannot name a class outside the program (its import fails); javac's world is its test (`bin/javac-vendor check`) |
| `Object.wait(long)` on an Object-typed receiver: jrt's `WaitTimeout` takes the nanoseconds too; `wait(long, int)` in the helpers | `code.ae` | `tWaitLong`; the shipped `TimeUnit.timedWait` |
| a bound method reference to a private method reached directly, not as a virtual call; the interpreter calls it so too (it threw `AbstractMethodError`) | `reach.ae`; `interp.ae` | `tPrivateMethodRef` |
| `Throwable.printStackTrace(PrintWriter\|PrintStream)` | `code.ae`, `reach.ae`, `model.ae` | `tPrintStackTrace` |
| a lambda's null-typed body value (j2g's temporary of type `any`, which no other interface takes): a statement, then nil of the target type | `code.ae` `expr-as`, `convert-ref-or-prim` | `tLambdaTemp` |
| an anonymous class's superclass outer instance reached through a non-leaf class's interface (`Self_`) | `decls.ae` (`super-outer-path` through `c/outer-field`) | `tOuterThroughInterface` |
| analysis: a local or anonymous class in a constructor's prologue reads an outer instance through the constructor's outer instance parameter, captured as a local (as for lambdas); j2g names that capture the constructor's `this_0`, the interpreter reads it as the receiver's `this$0` | `analyze.ae` (`prologue-class?`, `outer-this-node`), `decls.ae`, `interp.ae` | `FxJShapes/tPrologueAnon` |
| analysis: a lambda's return tag naming the anonymous class its body creates (not declared yet) is the functional interface method's return type | `analyze.ae` (`lambda-ret-desc`) | `FxJShapes/tLambdaAnonType` |
| `Runtime.version()` | jrt | `tRuntimeVersion`; jrt's `TestRuntimeVersion` |

No javac patch remains.

### JN2.4 javac's world translated, the tool, and JN1's driver

`bin/arbace-go --build-java [DIR...]` is the tool build (JFS4 (b)), the test build's recipe with
javac's forms, the variants, both edges and the DIRs' Java forms (a driver), into
`target/arbace-go/<arch>/arbace-javac`. j2g's analysis is its host's own (`bin/j2g`), so the analysis
fixes take effect on a host built from the checkout: `bin/javac-vendor check` and the tool build
run j2g on `target/arbace-go/amd64/arbace` (JFS22).

| | JN1 (patched javac) | JN2 |
|---|---:|---:|
| classes analyzed, failing (with the driver) | 16,241, 2 | 16,668, **0** |
| classes translated, methods reached, with missing parts | 16,128, 82,648, 363 | 16,565, 84,078, 349 |
| tool executable (stripped) | 183,373,984 bytes | 186,527,904 bytes |
| tool build (j2g 79-151 s, build-go 184 s; 135 s with Go's cache warm) | about 5 minutes | 343 s |

JN1's driver (`jn1.Main`, `jn1.Files`, JN1's converted forms, named with `--qualify jn1/`) on the
tool, against JN1's outputs of the same driver on the frozen JVM's javac (`.tmp/jn1/jvm-*.txt`):

| run | javac on Arbace (wall) | equal to the JVM's |
|---|---:|---|
| parse javac's own sources (482 files, 160,225 lines printed) | 1.3 s | **yes** |
| parse java.base's closure (838 files, 380,024 lines) | 2.9 s | **yes** |
| analyze `Reduced.java` with the baseline as source path | 3.8 s | **yes** |
| analyze the samples (2,173 lines of facts) | 0.3 s | yes, but capture numbers |
| analyze the baseline (183 files, 217,098 lines of facts) | 4.9 s (javac's own: 3.7 s) | yes, but capture numbers |

As in JN1, "capture numbers" are javac's names of captured types, from identity hashes; normalized,
every output is equal. javac's messages now come from its bundles through `getBundle`.

### JN2.5 The shipped program's change

j2g's program tree (`target/arbace-go/j2g`) of a build of main `58806d78` against the branch's,
function by function (string literals' numbers and gensym suffixes normalized,
`.tmp/scripts/fndiff.py`):

- the translated JDK and runtime, package `jrt`: the same 3,117 classes translated. Changed:
  `java.util.concurrent.TimeUnit.timedWait` (translated instead of a "not translated" stub: j2g's
  `wait(long)`), `java.util.ResourceBundle` (`getBundle` x2, `newBundle` added; the class's other
  functions differ in their temporaries' numbering only), `j2g_classes` (ResourceBundle's member
  table and its reflective invoker), `j2g_cut` (`Runtime$Version` no longer a cut class: jrt's own),
  `java.lang.ProcessImpl`'s `onExit` lambda (its null value returned as `nil` instead of through a
  temporary of type `any`: the fix of `expr-as`, the same code); jrt's hand-written
  `Runtime_Version` and `Runtime.version()`. Nothing else; the bundle classes' constructors were all
  reached before.
- the compiled namespaces: those whose sources changed (`arbace.j2g.*`, `arbace.classes.analyze`),
  and three unchanged ones (`arbace.box.plan`, `arbace.box.rebuild`, `arbace.repr.go.load`) whose
  compiled code differs by the build's mode: main's was compiled by the host, the branch's (its
  runtime changed) by a first build of the program, which links a namespace's own fns directly
  (Futamura F5); the fixpoint's G2 = G3 (JN2.6) holds for the branch.

### JN2.6 The checks

- `bin/javac-vendor verify`: `jdk/javac` is the conversion and the recording, byte for byte (564 files, 86 s)
- `bin/javac-vendor check`: j2g translates javac's world, no failures (45 s)
- `bin/gate` (essential, on the merged tree, `ef1b91d6`): passes (go-build, go-smoke, suite-go 777 tests, oracle-go, diffcheck, goi-check 740 of 740, generated)
- `bin/diffcheck` (in the gate), with FxJavac's 7 and FxJavacShapes' 2 cases: 12,741 of 12,764 cases agree, the 23 known disagreements as recorded (`test/diffcheck/known.edn`: FxVarHandles' known case renumbered from `fixtures:7` to `fixtures:9`, the two new fixture files coming before it)
- `bin/arbace-go --fixpoint`: holds, G1 = G2 = G3 byte for byte (`a3534827...`, 146,354,336 bytes; j2g's trees equal); the host (stage 0 of the unchanged seed) differs from G1, as for any change to the program
- `bin/j2g-edge verify` after `update`: the edge is the JVM's answer
- `bin/jrt-convert update`: one form file changed (`jdk/forms/java/util/ResourceBundle.ae`); `bin/jrt test`: passes

No re-seed (batched).

### JN2.7 Proposals

- **JFS18 Where the JDK API that only javac's world needs lives.**
  - Options: (a) the shipped closure (jrt-convert's closure, `overlay/jdk/java.base`), and
    `jdk/edge.edn` updated for the classes outside the shipped world their types name; (b)
    javac's world: jdk26u's classes converted into `jdk/javac` (module java.base), jrt's own Java
    for it in `overlay/jdk/javac/java.base`, members of shipped classes as j2g variants in
    `overlay/jdk/javac/variants`; (c) a second jrt-convert closure for the tool.
  - *Recommended: (b), built*: the shipped program and its edge hold nothing only javac uses, and
    the tool's input stays beside javac's forms (JFS14's reason). jrt's general API (getBundle,
    Runtime.version, printStackTrace) is shipped.
- **JFS19 jdk/edge.edn after jrt's shipped additions.**
  - Options: (a) the two entries recorded (`bin/j2g-edge update`); (b) getBundle and
    Runtime.version moved into javac's world too, `jdk/edge.edn` byte for byte main's.
  - *Recommended: (a), built*: the edge stays the JVM's answer for the shipped world, as
    `bin/j2g-edge verify` requires; (b) would keep a JDK API javac merely happens to use out of the
    program.
- **JFS20 jdk.compiler's generated sources.**
  - Options: (a) copied from the JDK build tree's `support/gensrc/jdk.compiler` (built; the forms
    are committed, the provenance names the tree); (b) generated by `bin/javac-vendor` with the
    JDK build's tools (`compileproperties`, `propertiesparser`, `flagsgenerator`) and compared with
    the build's, as `bin/jrt-convert generate` does.
  - *Recommended: (a) now, (b) with JN7* (the generators on Arbace).
- **JFS21 Regression cases the frozen jar cannot record.**
  - Options: (a) their results kept by hand in `test/j2g/record_fixtures.ae` (built); (b) record
    them on the program built from the checkout.
  - *Recommended: (a)* until the reference moves off the frozen jar; the cases are few and their
    results Java's.
- **JFS22 The Java forms' analysis of the tool's j2g.**
  - Options: (a) j2g on the program built from the checkout (`target/arbace-go/amd64/arbace`, built);
    (b) `bin/j2g` loads `arbace.classes.*` from the checkout when it differs from the host's, as
    it loads j2g.
  - *Recommended: (a)*; (b) if analysis changes become frequent before a re-seed.

### JN2.8 Sources and scratch

- jdk26u at `baf63fbe42b8758448fee570a9d9bb914272d259` (`/root/jdk26u`): `src/jdk.compiler`,
  `src/java.compiler`, `src/jdk.internal.opt/.../CommandLine.java`, 19 files of `src/java.base`, and
  the build tree's `support/gensrc/jdk.compiler`.
- The worktree's `.tmp/`: `javac-vendor/` (the conversion's work and logs), `driver/` (JN1's driver,
  its commands and outputs), `base/` (main's export and build for JN2.5), `shipped-fndiff.txt`,
  `scripts/fndiff.py`.

## Decided (2026-10-20): JFS18-JFS22

The user decided JFS18-JFS22 as built:
- JFS18: the JDK API only javac needs is in javac's world, not the shipped closure.
- JFS19: the shipped additions (`ResourceBundle.getBundle`, `Runtime.version`,
  `printStackTrace(PrintWriter|PrintStream)`) and their two lines of `jdk/edge.edn` are kept.
- JFS20: the generated sources are copied from the build tree until JN7.
- JFS21: two fixture results are kept by hand for shapes the frozen jar cannot compile.
- JFS22: j2g for the tool runs on a program built from the checkout.

## As built: JN3 (2026-10-21)

JN3, javac runs on Arbace, on the branch `java-frontend-jn3` from main `982ae875`, in about an hour
and a half of work (against the 3-4 days estimated): javac's forms, translated by j2g into the tool
executable `arbace-javac`, compile class files, and they are the JVM's javac's byte for byte
over every corpus, and java.base compiled as the JDK build compiles it is the build's own
classes. No javac patch (JFS12 (c)). The JVM ran only the frozen v4 jar (`bin/repr-java`,
`bin/lib/frozen.bash`: the conversion and the edges) and the frozen JDK's `javac` (the
reference, `images/jdk` of jdk26u's build tree at `baf63fb`, 26.0.2.1-internal); Go only
TamaGo's go1.27.1, without cgo. The tools' host was the worktree's stage 0 (the seed of
`665fa0b6`, which holds JN2); j2g for the tool ran on the program built from the checkout
(JFS22 (a)).

### JN3.1 javac's command line on Arbace (JFS5 (b), JFS25)

javac's own launcher, `com.sun.tools.javac.Main`, runs over `JavacFileManager`, which reads through
java.nio.file's file systems, `jrt:/` and zip. JN3's launcher is jrt's own Java in javac's
module, `overlay/jdk/javac/jdk.compiler/jdk/internal/jrt/javac/` (package
`jdk.internal.jrt.javac`, named by binary names as javac's packages are: `names.ae`'s
`tool-module-prefixes`), converted by `bin/javac-vendor` into `jdk/javac/forms` with javac:

- `Main` (170 lines): javac's command line. The file manager's options are parsed here (`-d`,
  `-sourcepath`, `-cp` with javac's default `.`, `--patch-module`, `--module-source-path`, and
  `--system none`, an empty `--module-path` and `-encoding utf-8` accepted as what the file
  manager does), `@argfiles` expand as javac's (`jdk.internal.opt.CommandLine.loadCmdFile`), and
  the rest goes to `JavacTool.getTask` with the diagnostics printed by javac's own `Log`, so the
  output is javac's command line's. `--platform DIR` names the platform (below). The exit status
  is javac's (0, 1 errors, 2 a bad command line).
- `FileManager` (451 lines), a `JavaFileManager` over `java.io.File`: the source path, the class
  path, the class output, the platform's system modules (each a module directory with a
  `module-info.class`), `--patch-module`'s patches and the module source path's modules (javac's
  module-pattern form, `P/*/S`), with the class output per module in multi-module mode, as
  `Modules`, `ModuleFinder`, `ClassFinder` and `ClassWriter` ask (`getLocationForModule` by name
  and by file, `listLocationsForModules`, `inferModuleName`, `contains`, `getJavaFileForInput` of
  `module-info`). It decides only where files come from and go to. JN1's file manager answered no
  `getJavaFileForInput`, so javac took the platform's java.base for an automatic module; this one
  reads its descriptor.

The tool runs it as `arbace-javac SCRIPT ARGS...`, the script
`(System/exit (jdk.internal.jrt.javac.Main/run (into-array String *command-line-args*)))`.

### JN3.2 What javac's world needed beyond JN2's

| gap | where it showed | the fix |
|---|---|---|
| `DataOutputStream` (javac's `ByteBuffer.appendLong`, `appendFloat`, `appendDouble`: the constant pool's `long`, `float`, `double`), a declared-only class in jrt | ClassWriter | jdk26u's `java/io/DataOutput.java`, `DataOutputStream.java`, `UTFDataFormatException.java` and `jdk/internal/util/ModifiedUtf.java` converted into javac's world (`bin/javac-vendor`'s `base_files`, JFS18 (b)); unpatched |
| `JavaLangAccess.countNonZeroAscii` (`DataOutputStream.writeUTF`) | DataOutputStream's analysis | jrt (shipped): the member in jrt's hand-written `JavaLangAccess` (`go/arbace/jrt/access.ae`, 10 lines), as the JDK's `String.countNonZeroAscii`; jrt test `TestCountNonZeroAscii` |
| `java.nio.file.FileSystems` (`PathFileObject.<clinit>` holds `FileSystems.getDefault()`; reached by `ClassWriter` for the `SourceFile` attribute, so by every compilation with `-g` or javac's default `-g:source,lines`, not by `-g:none`) | java.base with `-g`: "class java.nio.file.FileSystems is not in the closed world" | javac's world: `overlay/jdk/javac/java.base/java/nio/file/FileSystems.java`, jrt's own (126 lines): the default file system of jrt's paths (`getPath` as `Path.of`, the separator, the root; the rest throws), no other (`ProviderNotFoundException`) |
| analysis: a class with forms that the declared edge also declares (`java.io.DataOutput`, in `jdk/edge.edn` for the shipped world, in javac's world's forms now) was asked of the edge, which declares only its `:info` ("Undeclared functional of class java.io.DataOutput", j2g's `fromfn/functional?`) | j2g over javac's world | `arbace.classes.env/known?`: with `*declared*` bound, a class compiled from source (`*from-source*`) is not known, as `load-class` already said without it. No case in `bin/diffcheck`'s corpora: the test build's world cannot hold forms of a class the shipped edge declares without changing the shipped edge; javac's world is its test (`bin/javac-vendor check`, the tool build), as for JN2's cast fix |

`jdk/javac/`: 531 sources (jdk.compiler 370: 347 of `share/classes`, 21 generated, the 2
launcher files; java.compiler 135; jdk.internal.opt 1; java.base 25: 23 of jdk26u, 2 of
`overlay/jdk/javac`), 570 form files (6,721,894 bytes, against 561 and 6,684,582), the edge 38
classes beyond `jdk/edge.edn`'s (77,998 bytes, against 35 and 77,522), `platform.sha256` new.
The tool:

| | JN2 | JN3 |
|---|---:|---:|
| classes analyzed, failing | 16,668, 0 | 16,777, 0 |
| classes translated, methods reached, with missing parts | 16,565, 84,078, 349 | 16,674, 84,518, 339 |
| tool executable (stripped) | 186,527,904 bytes | 187,433,120 bytes |
| tool build (j2g 75 s, build-go 177 s) | 343 s | 261-280 s |

The processors' paths (`java.lang.annotation.Annotation`'s members, `sun.reflect.annotation`)
stayed stubs: no corpus reached them (`-proc:none`).

### JN3.3 The platform (JFS15 (b), JFS23)

The build tree's exploded modules (`build/linux-x86_64-server-release/jdk/modules`), the six the
corpora name and require: java.base, java.xml (the baseline's `org.xml.sax`), java.logging and
java.desktop (the module sample's `requires`), java.datatransfer and java.prefs (java.desktop's).
The exploded modules' class files are the image's (`jimage extract` of `lib/modules`,
compared once) but for three kinds: the module descriptors, which jlink and jmod give the
module's version (`26.0.2.1-internal`) and `ModulePackages`, jlink's generated
`java.lang.invoke` holders and species, and `SystemModules`. javac writes a required module's
version into `requires` (the samples' `module-info.class`: 20 bytes and three constant pool
entries apart), so the platform takes each module's descriptor from the build's jmod
(`images/jmods/MODULE.jmod`'s `classes/module-info.class`, byte for byte the image's), JFS23 (b).
`bin/javac-vendor` assembles it in `.tmp/javac-vendor/platform` (a tree of links, the
descriptors copied, by `unzip`) and pins it in `jdk/javac/platform.sha256` (per module the
SHA-256 of its files' `sha256sum` listing), written by `convert` (so `verify` compares it) and
`platform-pin`, checked by every `classes` run.

### JN3.4 The check: `bin/javac-vendor classes` (JFS26)

`bin/javac-vendor classes [baseline samples jdk java.base]` compiles each corpus by the JVM's
javac and by javac on Arbace (`$JAVAC_EXE`, default `target/arbace-go/amd64/arbace-javac`),
with the round trip's options (`bin/lib/repr-java/roundtrip`: `-g:none -nowarn
-XDsuppressNotes -proc:none -implicit:none`, the jdk corpus with `--patch-module java.base=...
-XDstringConcat=inline --enable-preview -source 26`), the files sorted as the round trip lists
them, and compares the class files (`diff -r`) and the diagnostics; `java.base` is JFS2 (a)'s
check (JN3.5). Makefile: `javac-classes` (in `make verify`). Results, all byte for byte:

| corpus | files | classes | equal | JVM's javac: wall, CPU, peak RSS | javac on Arbace: wall, CPU, peak RSS |
|---|---:|---:|---|---|---|
| baseline (the frozen `clojure/`) | 183 | 812 | **yes** | 4.0 s, 33.2 s, 512 MB | 5.5 s, 6.8 s, 542 MB |
| samples `java` | 10 | 28 | **yes** | 0.9 s | 0.3 s |
| samples `module` | 4 | 4 | **yes** | 0.5 s | 0.2 s |
| java.base's closure (the round trip's jdk originals) | 838 | 2,224 | **yes** | 8.7 s, 61.5 s, 1,033 MB | 18.0 s, 26.1 s, 1,188 MB |
| java.base as the JDK build (JN3.5) | 3,404 | 7,434 | **yes**, and the build's | 21.1 s, 100.7 s, 2,291 MB | 62.9 s, 101.4 s, 3,253 MB |

(`/usr/bin/time` of each compilation alone; the JVM's times include HotSpot's start and warm-up,
its CPU its compiler and collector threads.) javac on Arbace is 1.4 times the JVM's wall time on
the baseline, 2.1 times on the closure and 3.0 times on java.base, faster on the small samples
(no warm-up), and uses less CPU but on java.base (the same); memory is 6-42% more. R1 (Arbace
runs Java not as the JVM) showed nothing: no class file, constant pool order, attribute or
diagnostic differs. The whole run of the four corpora: 178 s.

### JN3.5 java.base compiled by javac on Arbace (JFS2 (a))

The JDK build's own compilation of java.base, read from the build tree
(`jdk/modules/java.base/_the.java.base_batch.cmdline` and `.filelist`: 3,404 files of
`src/java.base/{share,unix,linux}/classes` and `support/gensrc/java.base`, the build's interim
javac of jdk26u on the boot JDK 25), with its options: `-g -source 26 -target 26 -implicit:none
-Xprefer:source -XDignore.symbol.file=true -XDstringConcat=inline --module-source-path
"gensrc/*:src/*/linux/classes:src/*/unix/classes:src/*/share/classes" --module-path "" --system
none -encoding utf-8 -d OUT`; left out: `-h` (the JNI headers), `-Xplugin:depend` and the
javacserver's `-XD` options, `-Xlint:all`, `-Xdoclint`, `-Werror` (diagnostics only). javac on
Arbace and the JVM's javac give the same 7,434 class files, and all but one are the build's own
class files in `jdk/modules/java.base`, byte for byte. The one is `module-info.class`, which the
build rewrites after javac (`make/ExplodedImageOptimize.gmk`: `build.tools.jigsaw.AddPackagesAttribute`
adds `ModulePackages`); javac's own is compared with the JVM's. So "the platform's class files
equal to the frozen JDK's" holds for java.base, the larger check of JF3 in §7: the whole module,
not only what the corpora read, with debug attributes (`-g`). The first run failed in
`PathFileObject.<clinit>` (JN3.2's `FileSystems`), after attributing all of java.base in about
10 s: that run was the only failure of JN3.

### JN3.6 The shipped program's change

j2g's program tree of main `982ae875` built in the worktree's `.tmp/base`, against the branch's
(`diff -r`): 18 files differ, of three changes:
- jrt's `access.ae` (`JavaLangAccess` with `countNonZeroAscii`), and so j2g's `Dyn` table, whose
  slots count one method more (1,412 against 1,411; the slots after it renumbered);
- the compiled namespaces `arbace.classes.env` (`known?`) and `arbace.j2g.names`
  (`tool-module-prefixes`), with their embedded sources;
- j2g's report.

Nothing else: the translated JDK and runtime are the same. `bin/j2g-edge verify` passes
(`jdk/edge.edn` unchanged), `bin/jrt test` passes. No re-seed (batched).

### JN3.7 The checks

- `bin/javac-vendor classes baseline samples jdk java.base`: all equal, byte for byte (178 s)
- `bin/javac-vendor verify`: `jdk/javac` is the conversion, the recording and the pin, byte for
  byte (574 files, 79 s)
- `bin/javac-vendor check`: j2g translates javac's world, no failures (40 s)
- `bin/j2g-edge verify`: passes (59 s); `bin/jrt test --arch amd64`: passes, with
  `TestCountNonZeroAscii`
- `bin/gate` (essential, on the branch at `ed56f50f`): passes in 15m34s (go-build, go-smoke,
  suite-go 777 tests with no regression, oracle-go 53 known mismatches as recorded, diffcheck
  12,741 of 12,764 cases agree with the 23 known, goi-check 740 of 740, generated)

### JN3.8 What it means for JN4

javac is the front end on Arbace for class files already; JN4 runs repr/java's reader on it.
Two things carry over: the file manager serves `read-jdk`'s `--patch-module` and the platform
(JN3.1), and the tool roots only javac's exported API and its launcher's package, so JN4 needs
JFS17 (a)'s rooting of javac's internals (`com.sun.tools.javac.tree`, `code`, `comp`) for the
converter. The platform's other modules (java.xml and the rest) could be compiled the same way
as java.base (JFS24).

### JN3.9 Proposals

- **JFS23 The platform's module descriptors.**
  - Options: (a) the exploded modules alone: javac on Arbace writes a named module's `requires`
    without the required modules' versions, so the module sample's `module-info.class` differs
    from the JVM's javac's; (b) the exploded modules with each module's descriptor from the
    build's jmod (`images/jmods`, the image's descriptor byte for byte), pinned with them; (c) the
    image's classes entirely (`jimage extract` of `lib/modules`, a JDK tool, with jlink's
    generated classes).
  - *Recommended: (b), built*: the declarations javac reads are the JVM javac's (the image's
    descriptors), from the build tree, with no JVM tool; (c) would add jlink's own classes, which
    no source declares.
- **JFS24 The platform after java.base compiled equal (JFS15's replacement).**
  - Options: (a) the build tree's exploded modules stay the platform (pinned) until JN6, the
    equality shown by `classes java.base`; (b) the platform compiled by javac on Arbace (java.base
    as JN3.5, then java.xml, java.logging, java.desktop, java.datatransfer, java.prefs the same
    way), cached by key, the descriptors still the jmods'.
  - *Recommended: (a) now* (nothing to build): the bytes are the same, as JN3.5 shows, and (b)
    costs a minute of java.base and more of java.desktop for every new key; (b) when the build tree
    is to go (JN6, with JN7's generators for the generated sources).
- **JFS25 Where javac's command line on Arbace lives.**
  - Options: (a) jrt's own Java in javac's module (`overlay/jdk/javac/jdk.compiler`,
    `jdk.internal.jrt.javac`), converted with javac into `jdk/javac/forms` and verified with it;
    (b) an Arbace namespace driving javac's API by interop (needs javac's internals rooted, JFS17,
    and a `JavaFileManager` reified in Arbace); (c) javac's own launcher over `JavacFileManager`
    (JFS5 (a): the file systems, `jrt:/` and zip in jrt).
  - *Recommended: (a), built*: one tree, one conversion, one `verify`; JN4's `javac.ae` can hand
    the same `FileManager` to `getTask`.
- **JFS26 The check in the gate.**
  - Options: (a) `bin/gate --full`'s Go chain gets a step `javac` after `go-build-full`:
    `bin/javac-vendor verify`, `check`, `build`, `classes baseline samples jdk java.base` (about 10
    minutes; JN2's `verify` and `check` had no gate step); (b) the same plus the samples in the
    essential gate (needs the tool build cached as `--gate-build` caches its builds); (c) the
    Makefile only (`javac-verify`, `javac-classes`).
  - *Recommended: (a), built* while the JVM's javac exists (it is the reference); JFS9 moves it
    on at JN5.

### JN3.10 Sources and scratch

- jdk26u at `baf63fbe42b8758448fee570a9d9bb914272d259` (`/root/jdk26u`): `src/java.base`'s
  `java/io/DataOutput.java`, `DataOutputStream.java`, `UTFDataFormatException.java`,
  `jdk/internal/util/ModifiedUtf.java` (converted), `java/nio/file/FileSystems.java`,
  `FileSystem.java` (their API read), `src/jdk.compiler`'s `api/JavacTool.java`,
  `main/Arguments.java`, `main/Option.java`, `comp/Modules.java`, `code/ModuleFinder.java`,
  `jvm/ClassWriter.java`, `util/ByteBuffer.java`, `file/PathFileObject.java` (read),
  `make/ExplodedImageOptimize.gmk`, `make/ModuleTools.gmk`; the build tree's `jdk/modules`,
  `images/jmods`, `images/jdk` and `jdk/modules/java.base/_the.java.base_batch.{cmdline,filelist}`.
- The worktree's `.tmp/`: `javac-vendor/` (the conversions, the platform, `classes/CORPUS/` with
  each side's classes, logs and times, `jdk-originals/`), `jn3/` (the build logs, `rss.sh` and
  its measures, `jimg/` and `jmod/`, the image's and jmods' classes compared), `base/` (main's
  export and build for JN3.6).

## Decided (2026-10-21): JFS23-JFS26

The user decided JFS23-JFS26 as built:
- JFS23 (b): the exploded modules with each module's descriptor from its jmod, pinned with them.
- JFS24 (a): the pinned build tree stays the platform until JN6; the platform compiled by javac on
  Arbace (b) when the build tree goes.
- JFS25 (a): javac's command line on Arbace as jrt's own Java in jdk.compiler's overlay.
- JFS26 (a): the `javac` step in `bin/gate --full`'s Go chain.

## As built: JN4 (2026-10-21)

JN4, repr/java's `read` on Arbace, on the branch `java-frontend-jn4` from main `864a5de2`, in about
three hours of work (against the 2-4 days estimated): `arbace.repr.java`'s reader runs on javac's
tool executable, its namespaces compiled into the tool by c2j, javac on Arbace attributing against
the pinned platform, and it reads every corpus of §6 byte for byte as the frozen JVM's reader does:
the baseline's hashes, the samples, the module sample, `jdk/forms`, the suite's fixtures and mode
`:full` of the round trip's three corpora. The reader's code is unchanged but for javac's file
manager (`javac.ae`). The JVM stays the default host (JN5 and JN6 move the checks and remove it).
The JVM ran only the frozen v4 jar (`bin/repr-java`, `bin/lib/frozen.bash`: the reference reads,
the re-recorded provenance and edge) and the system JDK's generators (`bin/jrt-convert generate`,
JN7's); Go only TamaGo's go1.27.1, without cgo. The tools' host was the worktree's stage 0 (the seed
of `665fa0b6`); j2g for the tool ran on the program built from the checkout (JFS22 (a)).

### JN4.1 The reader on javac's tool executable

- **`javac.ae`** (the reader's only change): when the class `jdk.internal.jrt.javac.FileManager`
  exists (javac's tool executable; the frozen jar has none), `analyze` hands JN3's file manager to
  `JavacTool.getTask`, as javac's command line on Arbace does (JFS25 (a)): the file manager's
  options are taken out of javac's (`-sourcepath`, `-cp`, `--patch-module`), the platform is the
  directory `REPR_JAVA_PLATFORM` names, the class path `-cp`, else `REPR_JAVA_CP` (on the JVM the
  class path's tail), else none; the sources are `FileManager.source`'s. Everything else (`parse`,
  `analyze`, the diagnostics, `Types`, `Symtab`) is as on the JVM, and on the JVM nothing changed.
  `convert`, `jtypes`, `resolve`, `names`, `layout`, `full`, `forms` and `java.ae` run unchanged.
- **`read-jdk`'s `--patch-module`** is the file manager's `addPatch`, one per chunk, against the
  platform's system modules (JN3.3: the build tree's exploded modules with the jmods' module
  descriptors, `jdk/javac/platform.sha256`), the chunks and threads as on the JVM
  (`REPR_JAVA_THREADS`, `REPR_JAVA_PATCH_ALL`).
- **`bin/repr-java`**: `REPR_JAVA_HOST=jvm` (the default) or `arbace`: `read`, `read-jdk`,
  `coverage` and `main` run on `REPR_JAVA_EXE` (default `target/arbace-go/<arch>/arbace-javac`)
  with `ARBACE_PATH` the checkout, the platform made once into `.tmp/repr-java-platform` (under a
  lock) by `bin/javac-vendor platform DIR`, a new command that makes it and checks it against the
  pin. An environment variable rather than an option, so that every script that calls
  `bin/repr-java` (`check`, `bin/jrt-convert`, `bin/clojure-tests --java-fixtures`,
  `bin/javac-vendor`) runs on the selected host unchanged. `bin/lib/repr-java/check` takes
  `REPR_JAVA_CHECK_WORK`.
- **The provenance names the host**: `bin/lib/frozen.bash`'s new `repr_java_host` (the frozen jar's
  name and SHA-256 on the JVM, as before; `arbace-javac SHA-256` of the tool on Arbace) replaces
  `frozen_host` in the three provenance writers (`bin/jrt-convert`, `bin/clojure-tests`,
  `bin/javac-vendor`): JFS30.

### JN4.2 What the tool roots (JFS17 (a)) and j2g's fix

j2g's `repl-visible?` (`arbace/j2g/main.ae`) treats a class of the tool's modules (`nm/qualified?`:
javac's packages, JFS13) as visible whatever the edge's `:unexported` says, so `--program` roots
every public member of `com.sun.tools.javac.*` as of `javax.*`; the shipped world holds none of
these packages. The tool, before and after (the program built from the checkout as j2g's host; the
reader not yet compiled in):

| | JN3 | JN4, javac's internals rooted |
|---|---:|---:|
| classes analyzed, failing | 16,777, 0 | 16,780, 0 |
| classes translated, methods reached, with missing parts | 16,674, 84,518, 339 | 16,725, 85,990, 368 |
| tool executable (stripped) | 187,433,120 bytes | 188,485,792 bytes (+1,052,672, +0.6%); 188,498,080 with javac's edge re-recorded (below) |

So JFS17 (a) stands: the tool grows by 0.6%, not a third, because javac's API already reaches most
of its internals; (b) is not needed. The rooting made one class compile in Go that had not been
reached: javac's source launcher's record `MemoryModuleFinder`, whose component `descriptor` is a
`java.lang.module.ModuleDescriptor`, outside the closed world. Its struct has no field for it
(j2g's `class-fields`), but j2g's derived `toString`, `hashCode` and `equals` read the field: a Go
compile error. **The fix** (`arbace/j2g/decls.ae`, `record-object-body`): such a component's value
is an operation-level stub (J2G-SPEC §4.1, `J2g_Missing`) as an Object; the record's canonical
constructor does not exist either, so the stub is never reached. No regression case in
`bin/diffcheck`'s corpora: the interpreted half cannot name a class outside the program, as for
JN2's cast fix; javac's world is its test (the tool build).

`jdk/javac/edge.edn` was recorded again (`bin/javac-vendor edge`, then `convert`): the rooted record
adds `ModuleDescriptor`'s members to `:cut` (39 classes beyond `jdk/edge.edn`'s, 79,753 bytes,
against 38 and 77,998), and `:unexported` shrinks to `#{"sun.reflect.annotation"}`, since
`repl-visible?` no longer asks about javac's packages. `jdk/edge.edn` is unchanged (`bin/j2g-edge
verify`).

### JN4.3 The reader compiled into the tool (JFS28)

Evaluated from the checkout, the reader converted the baseline in 33.1 s (javac's `analyze` 4.7 s);
the JVM's took 5.4 s (javac 3.6 s). The converter is evaluated Clojure code, and the evaluator
was the cost, so the tool now carries the reader compiled:

- j2g's new `--embed PREFIX` (`main.ae`, `embed.ae`'s `sources`): sources the shipped program leaves
  out are embedded too; the tool embeds `arbace/repr/java` (the reader, the printer, the round
  trip's comparator: 12 namespaces).
- `bin/arbace-go --build-java` builds the tool in two passes when the compiled reader's tree is
  missing or of another key (`reader_key`: the compiled namespaces' key, `aot_key`, and javac's
  world, `jdk/javac`, `overlay/jdk/javac`, `jdk/edge.edn`): a first tool build as before, then
  `reader_compile` on it (the reader's namespaces required from the checkout as embedded sources,
  `ARBACE_PATH_FIRST`, with `ARBACE_COMPILE`: only their units are recorded, the program's own
  namespaces staying compiled; `arbace.c2j.main/run` on them, each source in its own id space) into
  `target/arbace-go/aot-reader` (12 sources, 1,540 classes, 52 files, 8.4 MB of Java forms, 10 s),
  then the tool again with `--input aot-reader --embed arbace/repr/java`. With the tree's key
  unchanged, one pass. `ARBACE_AOT=0` builds the reader evaluated.
- At run time the reader's namespaces load compiled; a namespace whose source in the checkout
  differs from the tool's embedded copy is loaded from the checkout, evaluated (`RT.toolSource`:
  `arbace/repr/`), so a change to the reader runs before the tool is rebuilt.

| | evaluated | compiled | the JVM |
|---|---:|---:|---:|
| the baseline's conversion (after javac, 183 units) | 33.1 s | 15.4 s | 5.4 s |
| `read --check` of the baseline (the run's own time) | 47.3 s | 21.5 s | 8.9 s |
| `read-jdk --full` of the jdk corpus (4 threads, the run's own time) | 205.9 s | 120.0 s | 32.5-46.1 s |
| tool executable | 188,498,080 bytes | 197,910,688 bytes (+5.0%) | |
| j2g: classes analyzed, translated, methods reached | 16,780, 16,725, 85,990 | 18,381, 18,326, 91,849 | |

The tool build: the first pass as JN3's (261-280 s with the host of the checkout's runtime), the
reader's compile 10 s, the second pass about as long again; 708 s here, where both passes compiled
the namespaces after a first build of their own (the host's runtime was not the checkout's after
`jdk/provenance.edn` changed).

**c2j's fix found.** The first two-pass build failed: `arbace/repr/java.ae` was "not compiled:
Unable to resolve classname: Context". A linked call (`emit-link-site`) reads the callee's fn
method's nodes for its unboxed signature and spec mask (`site-psig`, `site-spec`); a parameter's
class there is its tag resolved lazily (`LocalBinding.getJavaClass`, `HostExpr.tagToClass`) in
`*ns*`, which was the caller's namespace. `arbace.repr.java` calls `arbace.repr.java.javac/types`,
`[^Context ctx]`, and does not import `Context`; when `javac.ae` had been compiled first, the class
was already cached in the binding, so the failure depended on the order of the sources (an earlier
order compiled all twelve). **The fix** (`arbace/c2j/emit.ae`): `site-spec` and `site-psig` bind
`*ns*` to the callee var's namespace. No regression case in `bin/diffcheck`'s corpora: they are
the oracle's forms, one namespace per file, and the case needs a namespace calling another's fn
whose parameter tag only the callee imports, compiled before it; the tool build is its test
(`reader_compile` compiles `java.ae` before `javac.ae`). The shipped namespaces' compiled tree is
unchanged by it (JN4.7).

### JN4.4 The fixtures' runtime (JFS6, JFS27)

On the JVM javac reads `arbace.lang.IFn` and `arbace.java.api.Clojure` (the suite's `SwissArmy`)
from the frozen jar on its class path. JFS6 (a) decided the checkout's `arbace/lang` printed by
`repr/java print` and put on javac's source path. As decided it does not work: `print` fails on
`RT.ae` (a stack overflow), `Compiler.ae` and `GoInterp.ae` ("unsupported loop") and the Go
variants of `arbace/lang/go` (`j2g/variant`), and the runtime is not Java a javac takes whole (the
Go build's forms, goi's classes, natives). What javac needs is the runtime's declarations, so
(JFS27 (b)):

- `bin/repr-java print --layout declarations` (`print.ae`'s `*declarations*`): every method and
  constructor body `throw null;` (a constructor's explicit `this(...)`/`super(...)` call kept), no
  initializer blocks, a field's initializer kept only when it is a literal of a final field or the
  field is an interface's (a final field without one is printed non-final), one top-level class per
  Java file (`GoInterp.ae` holds eight public ones), and a parameter named by a Java keyword
  (`Compiler`'s `super`, `extends`; `ReflectorCallSite`'s `_`) given a trailing `_`.
- One printer fix it needed holds for any print: an interface's instance method with a body is
  printed `default` when the forms leave `^:default` out (the runtime's hand-written
  `Compiler$Expr.evalIn`; converted forms always write it). The printer's output over `jdk/forms`
  (838 files), the samples and the baseline's mode `:full` forms is byte for byte the same before
  and after the change.
- `bin/repr-java declarations OUT`: `arbace/lang/*.ae` and `arbace/java/api` printed so into
  `OUT/java` (141 forms files, 0 failed; 10 s on the tools' host), compiled by javac on Arbace
  (`-g:none`, the platform) into `OUT/classes` (836 class files, about 10 s), whose path it prints.
- `bin/clojure-tests --java-fixtures` with `REPR_JAVA_HOST=arbace` and no `CLOJURE_SRC` uses it
  as javac's class path. The 15 forms files are `test/java-fixtures/forms` byte for byte: the
  runtime's declarations give the fixtures' forms the frozen jar's gave (R5 did not occur).

### JN4.5 The check: `bin/repr-java host-check` (JFS29)

`bin/repr-java host-check [check jdk fixtures full]` (`bin/lib/repr-java/host-check`; Makefile
`repr-java-host-check`, in `make verify`), under `.tmp/repr-java-host-check`:

| corpus | on javac's tool executable | against | result |
|---|---|---|---|
| `check` | `bin/repr-java check`: the baseline (183 files, 189 converted), `test/repr/java/java` (10, 11), `module` (4, 6) | `test/repr/java/expected/baseline.sha256`, `expected/java`, `expected/module` | **equal** |
| `jdk` | `bin/jrt-convert`'s `sources`, `generate` and `convert` (838 files, 911 converted) | `jdk/forms` (`diff -r`), `jdk/provenance.edn` but the host | **equal** |
| `fixtures` | `bin/clojure-tests --java-fixtures` (12 files, 15 converted), the runtime's declarations | `test/java-fixtures/forms` | **equal** |
| `full` | `read --full` of the baseline, the samples and the jdk corpus's originals (`read-jdk --origins`, the round trip's) | the same on the frozen JVM, `diff -r` | **equal** (189, 11, 6, 911 files) |

The whole check: 393 s with the compiled reader (609 s evaluated). The converters' counters and
heads in the reports (`repr-java-report.edn`) are the JVM's too (compared for the samples). R1
(Arbace runs Java not as the JVM) showed nothing in the reader either.

### JN4.6 Times

Each read alone (`/usr/bin/time`: wall, user + system CPU, peak RSS; the JVM's include its start,
about 4-5 s, and HotSpot's warm-up):

| read | files | the tool: wall, CPU, RSS | the JVM: wall, CPU, RSS | the tool against the JVM |
|---|---:|---|---|---:|
| baseline (`read --check`) | 183 | 24.1 s, 35.1 s, 1,161 MB | 13.8 s, 86.0 s, 2,285 MB | 1.75 times |
| baseline in mode `:full` | 183 | 26.5 s, 40.4 s, 1,256 MB | 13.2 s, 88.2 s, 1,755 MB | 2.0 times |
| samples `java` | 10 | 0.75 s, 1.0 s, 442 MB | 6.1 s, 25.0 s, 724 MB | 0.12 |
| samples `module` | 4 | 0.33 s, 0.4 s, 265 MB | 5.1 s, 20.4 s, 670 MB | 0.07 |
| the suite's fixtures | 12 | 0.57 s, 0.7 s, 275 MB | 5.7 s, 23.7 s, 736 MB | 0.10 |
| jdk (`read-jdk --check`, 4 threads, as `bin/jrt-convert`) | 838 | 98.2 s, 392.3 s, 26,639 MB | 35.7 s, 249.0 s, 13,946 MB | 2.7 times |
| jdk, 12 threads | 838 | 80.3 s, 405.1 s, 27,362 MB | 25.3 s, 255.5 s, 14,070 MB | 3.2 times |

So the reader on Arbace takes 1.75 to 3.2 times the JVM's wall time on the large corpora and a
tenth of it on the small ones (no JVM start), using less CPU on the baseline and 1.6 times more on
the jdk corpus. Memory: half the JVM's on the baseline, twice on the jdk corpus (each chunk's javac
context and converted units live together; with `GOMEMLIMIT=8GiB` the read took 238 s and 3,099 s
of CPU at 14,216 MB, the collector running all along, so Go's default stays). The whole of `bin/jrt-convert`'s steps sources,
generate and convert: 141 s on the tool.

### JN4.7 The shipped program's change

j2g's program tree of main `864a5de2` built in the worktree's `.tmp/base` against the branch's (the
essential gate's build at `da058fcb`, `diff -r`): 33 files differ, of the four namespaces whose
sources changed, all of them tools compiled into the program:
- `arbace.j2g.main` (`repl-visible?`, `--embed`), `arbace.j2g.embed` (`sources`' `also`),
  `arbace.j2g.decls` (`record-object-body`) and `arbace.c2j.emit` (`site-spec`, `site-psig`): their
  compiled packages (`aot_arbace_j2g_main`, `_embed`, `_decls`, `aot_arbace_c2j_emit`, each in
  `gen/` and `prog/`) and their embedded sources (`res/arbace/{j2g/main,j2g/embed,j2g/decls,c2j/emit}.ae.z`);
- j2g's report.

Nothing else: the translated JDK and runtime (`jrt`, `lang`) and every other compiled namespace
are the same, so c2j's fix compiles the shipped namespaces as before (their compiled tree
`target/arbace-go/aot` differs in the same four sources and `aot.edn` alone). The runtime key
(`bin/lib/version.bash`) changes with `jdk/provenance.edn` (`2e3537a4...` against main's
`a8919879...`), and with it the version the executable carries; the executable is 147,206,304
bytes (main's 147,177,632). Not in the program: `arbace/repr/java` (the reader, the printer),
`bin/`, the Makefile, `jdk/javac` (the tool's). `jdk/edge.edn` and `jdk/forms` are unchanged. No
re-seed (batched with GP4's and JN3's changes).

### JN4.8 The checks

- `bin/repr-java host-check` (`check jdk fixtures full`): every corpus equal to the JVM's reader
  (JN4.5), 393 s with the reader compiled (609 s evaluated, before JN4.3)
- the JVM's read with the new `javac.ae`: the samples as `test/repr/java/expected/java`
- `bin/jrt-convert update`, `bin/clojure-tests --java-fixtures update`, `bin/javac-vendor convert`
  (on the frozen JVM): the forms unchanged, the provenance files' `:repr-java` hash only
  (`b53fba6e...` to `229d9082...`: `javac.ae` and `bin/repr-java` changed), and javac's edge as
  JN4.2 says
- `bin/javac-vendor verify`: `jdk/javac` is the conversion, the recording and the pin, byte for byte
  (574 files, 82 s); `check`: j2g translates javac's world, no failures (49 s)
- `bin/clojure-tests --java-fixtures verify`: byte for byte (15 files, 10 s)
- `bin/j2g-edge verify`: `jdk/edge.edn` is the JVM's answer (57 s)
- `bin/javac-vendor classes baseline samples jdk java.base` on the new tool: every corpus byte for byte
  the JVM's javac's (baseline 812 classes, samples 28 and 4, jdk 2,224, java.base 7,434 and the
  build's), 234 s
- the printer before and after JN4.4's changes over `jdk/forms` (838 files), the samples (14) and
  the baseline's mode `:full` forms (183): the same Java, byte for byte
- `bin/gate` (essential, on the branch at `da058fcb`): passes in 13m51s (go-build, go-smoke,
  suite-go 777 tests with no regression, oracle-go 53 known mismatches as recorded, diffcheck
  12,741 of 12,764 cases agree with the 23 known, goi-check 740 of 740, generated)
- not run: `bin/gate --full` (by the rule for branches), so `--full`'s `javac` step with
  `host-check` (JFS29) has not run as a step; each of its commands ran above

### JN4.9 What it means for JN5

JN5 moves `bin/repr-java check`, `roundtrip`, `bin/jrt-convert`'s `convert` and `javac` steps and
`bin/clojure-tests --java-fixtures` onto the tool: each already runs there by `REPR_JAVA_HOST=arbace`
(the reads) and `bin/javac-vendor classes`' way of running javac (the class files), and
`host-check` shows the reads equal. What remains is the round trip's classes level on javac on
Arbace and the fixpoint (javac's own sources read by the tool into `jdk/javac/forms`). Speed: the
round trip reads each corpus twice and in mode `:full`; at the measured 1.75-3.2 times, its 735 s
on the JVM would be about 25-35 minutes on the tool unless the reads run in parallel. Which host
the provenance files then name is JFS30's question.

### JN4.10 Proposals

- **JFS27 The fixtures' runtime declarations (JFS6 as built).**
  - Options: (a) as JFS6 (a) decided, the checkout's `arbace/lang` printed whole onto javac's
    source path: it does not print (`RT`, `Compiler`, `GoInterp`, the Go variants) and is not Java
    javac takes; (b) its declarations printed (`print --layout declarations`), compiled by javac on
    Arbace, javac's class path (`bin/repr-java declarations`); (c) class-file stubs written from
    arbace.classes' class infos (JFS6 (b)), a new writer.
  - *Recommended: (b), built*: JFS6 (a)'s intent (the checkout's runtime, printed by repr/java, no
    JVM artefact) with javac reading only what it needs, through code that exists; the fixtures'
    forms are the committed ones.
- **JFS28 How the reader runs on the tool.**
  - Options: (a) evaluated from the checkout (no build change; 2.1 times slower in the converter);
    (b) compiled into the tool by c2j on a first tool build, its tree kept by key, embedded with
    j2g's `--embed` (built); (c) compiled with the program's namespaces in the shipped build's
    compile (its host lacks javac's classes, so only a tool can compile the reader: (b) by
    another name).
  - *Recommended: (b), built*: the converter twice as fast for one more pass of the tool build when
    Arbace's sources or javac's world change; evaluated still runs whatever the checkout holds.
- **JFS29 The check in the gate.**
  - Options: (a) `bin/gate --full`'s `javac` step runs `bin/repr-java host-check` after `classes`
    (about 10 minutes more with the tool's second pass, JFS28, while the JVM's reader is the reference, until JN5 moves the checks
    themselves); (b) the same, and the samples on the tool in the essential gate (needs the tool
    build cached as `--gate-build` caches its builds: about 10 minutes when not); (c) the Makefile
    only (`repr-java-host-check`).
  - *Recommended: (a), built* (the step's command; not run here, by the rule of no `--full` gate on
    a branch): the tool is built in that step anyway, and JN5 replaces the step's host-check with the
    checks on the tool.
- **JFS30 The host the provenance files name.**
  - Options: (a) the host that read (`repr_java_host`): the frozen jar's name and hash on the JVM,
    `arbace-javac` and the tool executable's SHA-256 on Arbace (built; the committed files are the
    JVM's until JN5, and `host-check` compares them but the host); (b) on Arbace the key the tool is
    built from (the sources, as `--gate-build`'s), so that two builds of one commit name it alike;
    (c) the host left out of the provenance.
  - *Recommended: (a) now, (b) at JN5* when the tool writes the committed files: the executable's
    bytes change with every change to the program, its sources' key only with what the tool is
    made of.

### JN4.11 Sources and scratch

- jdk26u at `baf63fbe42b8758448fee570a9d9bb914272d259` (`/root/jdk26u`): `src/jdk.compiler`'s
  `launcher/MemoryModuleFinder.java`, `api/JavacTaskImpl.java` (`analyze`), `main/JavaCompiler.java`
  (`-implicit` and `todo`), `comp/TypeEnter.java` (read); the build tree's `jdk/modules` and
  `images/jmods` (the platform, JN3.3).
- The worktree's `.tmp/`: `jn4/` (the build and check logs, `prof.ae` and the profiles,
  `times.sh` and `times/`, `decl/`, the evaluated tool `arbace-javac-evaluated`, `aotr/` the first
  compiled tree, `memtest/`), `repr-java-host-check/` (the check's work: each corpus on both hosts),
  `base/` (main's export and build for JN4.7).

## Decided (2026-10-21): JFS27-JFS30

The user decided JFS27-JFS30 as built:
- JFS27 (b): the fixtures' runtime declarations printed and compiled by javac on Arbace.
- JFS28 (b): the reader compiled into the tool by c2j, its tree kept by key.
- JFS29 (a): `bin/repr-java host-check` at the end of `bin/gate --full`'s javac step, until JN5.
- JFS30 (a) now, (b) at JN5.

## As built: JN5 (2026-10-21)

JN5, the fixpoint and the checks moved, on the branch `java-frontend-jn5` from main `ad80344b`
(whose seed holds JN3 and JN4), in about three hours of work (against the 2-3 days
estimated). javac on Arbace reads javac's own sources into the committed forms, byte for byte (the
fixpoint), and every repr/java check of `bin/gate --full` runs on javac's tool executable: `bin/repr-java
check` and `roundtrip` (every level, the classes level compiled by javac on Arbace, mode `:full`'s
text level included), `bin/jrt-convert`'s `convert` and `javac` steps, `bin/clojure-tests
--java-fixtures` and `bin/javac-vendor verify`. The references of `test/repr/java/roundtrip/` are
unchanged, and so are every corpus's forms; the committed provenance files now name the tool by
the key of its sources (JFS30 (b)). javac on Arbace is the default host (JFS33). No change to j2g,
arbace.classes, c2j or jrt was needed: R1 (Arbace runs Java not as the JVM) showed nothing. The
JVM ran only the frozen v4 jar (the edges' recordings, `check --suite`'s back end and suite,
`bin/jrt-convert`'s own namespaces, the JVM-hosted reads measured for comparison), the frozen JDK's
`javac` (the reference of `bin/javac-vendor classes`, and once for the comparison below) and the
system JDK's generators (`bin/jrt-convert generate`, JN7's); Go only TamaGo's go1.27.1, without cgo.
The tools' host was the worktree's stage 0 (the seed of `66f8aaba`); j2g for the tool ran on the
program built from the checkout (JFS22 (a)).

### JN5.1 The fixpoint: javac on Arbace reads javac

`bin/javac-vendor convert` (and so `verify`) runs `bin/repr-java read-jdk` module by module:
jdk.compiler (370 files, `--patch-module jdk.compiler=...`), java.compiler (135), jdk.internal.opt (1)
and java.base (25). On the JVM the running JDK holds every module; javac on Arbace attributes against
the platform (JN3.3), which held only the six modules the corpora name, so the first run stopped at
the platform's pin. The platform now holds javac's own modules too, jdk.compiler, java.compiler,
jdk.internal.opt and jdk.zipfs (jdk.compiler's `requires`), from the same build tree with their jmods'
descriptors (JFS31): `jdk/javac/platform.sha256` gains four lines, the six others unchanged. Then:

- the 570 forms files are `jdk/javac/forms` byte for byte, javac's edge (recorded on the frozen JVM, as
  before) and the platform's pin too: the whole tree but the provenance's `:repr-java` hash
  (`bin/repr-java` changed) and its host;
- the read of jdk.compiler takes 41.5-42.9 s on the tool against 14.2 s on the JVM (three chunks of
  about 125 files, `REPR_JAVA_THREADS=8`), the others 1-2 s; the whole `verify` 94 s against 87 s on
  the JVM, of either 35-36 s the edge's recording on the JVM;
- javac on Arbace's class files with the grown platform: `bin/javac-vendor classes baseline samples
  jdk java.base` byte for byte the JVM's javac's as before (baseline 812 classes, samples 28 and 4, jdk
  2,224, java.base 7,434 and the build's), 228 s. The unnamed module's default roots now include
  jdk.compiler and java.compiler (they export API), as on the JVM's full image.

So a javac upgrade needs no JVM for its forms: the tool of the previous javac reads the next one's
sources (§7's "even javac's next upgrade needs no JVM"), the edge's recording aside (JVM data, JFS14).

### JN5.2 The checks on the tool, and the default host

- **`bin/repr-java javac`** (new): javac's command line on repr/java's host, javac on Arbace
  (`jdk.internal.jrt.javac.Main`, the script `.tmp/repr-java-platform.javac.ae`, against the platform)
  or on the JVM the frozen JDK's `javac -J-Xmx4g`. The round trip's `compile` (the classes level, its
  rejection passes, the text level's `classes` and `anew`) and `bin/jrt-convert`'s `javac` step
  compile with it, so they follow the host; `bin/repr-java declarations` uses the same script.
- **The default host is javac's tool executable** (JFS33 (b)): `REPR_JAVA_HOST` defaults to
  `arbace` in `bin/repr-java`, `bin/lib/frozen.bash`'s `repr_java_host` and `bin/clojure-tests`;
  `REPR_JAVA_HOST=jvm` is the frozen JVM, the reference until JN6 (`host-check` uses it). A missing
  tool stops `bin/repr-java` with the command that builds it (`bin/javac-vendor build`).
- **`bin/jrt-convert`**: `convert` and `javac` on the host; `sources`, `generate`, `edge` and `report`
  as before (the closure's own namespaces `jrt.jrt-sources` on the frozen jar, the generators on
  `GEN_JDK`). The `javac` step's classes (the default `-g`, so `SourceFile` and `LineNumberTable`)
  on the tool are the JVM's javac's byte for byte (2,224 classes, compared once by hand; ref1 = ref2),
  so the edge read from them is the same.
- **`bin/clojure-tests --java-fixtures`**: on the tool its class path is the runtime's declarations
  (JN4.4), the default now.
- **`bin/repr-java check`**: steps 1-3 on the tool, `--suite`'s step 4 (the converted baseline
  compiled by the frozen jar's back end, Clojure's suite on the JVM) unchanged.

### JN5.3 The provenance names the tool by its sources' key (JFS30 (b), JFS32)

`bin/lib/javac-key.bash` (new): `arbace_javac_key` is the SHA-256 of what the tool is built from,
`bin/arbace-go`'s `gate_key` inputs (Arbace's sources, `go/`, `overlay/`, `jdk/` with javac's forms,
edge and the platform's pin, `x509roots/`, the build's scripts, the seed's and Go's pins, Go's
version) without the files repr/java writes (`jdk/provenance.edn`, `jdk/javac/provenance.edn`; the
fixtures are not an input) and without the check scripts (`bin/lib/repr-*/`, `bin/lib/frozen.bash`).
`bin/arbace-go --build-java` records it beside the executable (`arbace-javac.key`); `repr_java_host`
prints `arbace-javac KEY` and refuses a tool whose recorded key is not the checkout's
(`arbace_javac_fresh`; `REPR_JAVA_STALE_OK=1` warns instead, as `ARBACE_BOX_STALE_OK` does for the
box). Two builds of one commit write the same provenance. The executable's hash would not serve: the
tool rebuilt after `bin/arbace-go` and the platform's pin changed (so of another key) was byte for
byte the first build (199,901,344 bytes), and its version map carries the commit, so a commit that
changes nothing of the tool still changes its bytes.

Since any change to the tool's sources changes the key, a byte-for-byte comparison of the provenance
would make three verifies fail after every such change until their updates ran again. So the
verifies (`bin/jrt-convert verify`, `bin/javac-vendor verify`, `bin/clojure-tests --java-fixtures
verify`) compare the provenance but its `:host` line (`repr_java_provenance_same`) and print both
hosts, the recorded one and the run's (JFS32 (b)): the host says which tool wrote the files, not what
they are. With it the JVM-hosted verifies pass too (`jdk/`, `jdk/javac`, the fixtures: the run's host
`arbace-for-java-26-v4 4b6f0319...`, the recorded `arbace-javac ee04e344...`).

The three provenance files were written on the tool (`bin/jrt-convert update`, `bin/clojure-tests
--java-fixtures update`, `bin/javac-vendor convert`): in each only `:repr-java` (`229d9082...` to
`2edfddeb...`: `bin/repr-java` changed) and `:host` (`arbace-for-java-26-v4 4b6f0319...` to
`arbace-javac ee04e344...`) changed. `:jdk` stays `java -version`'s line, which names the JDK
build tree the platform is made of (JFS35).

### JN5.4 A bug found in `bin/jrt-convert verify`

`verify` compared `jdk/` with the fresh tree by `diff -r` leaving out `README.md`, `.gitattributes`
and `edge.edn`, but not `jdk/javac/` (since JN2, `bin/javac-vendor`'s): "Only in jdk: javac", so the
verify has failed since JN2 on main too (no `--full` gate ran since; JN2-JN4 ran `update` and
`host-check`, which compare `jdk/forms` alone). It now leaves out `javac` as well.

### JN5.5 The round trip in parallel (JFS34), and times

The round trip's corpora run concurrently, each logged into `.tmp/repr-java-roundtrip/CORPUS.log` and
shown in order; in the jdk corpus the originals compile while F2 is read, and the text level's
`anew` print runs while the full layout's print compiles. Results (every level of the three corpora
as the references, `test/repr/java/roundtrip/` unchanged): baseline forms 189, ae 182 of 189, classes
143 of 183; samples 17, 17, 12 of 14; jdk forms 874 of 911 (7 failing, 30 missing), ae 816, classes
473 of 838; the text level record, text and classes all, `anew` 183, 14, 799 of 838.

Each command alone (`/usr/bin/time`: wall, the largest process's peak RSS; the JVM's include its
starts and HotSpot's warm-up):

| command | javac's tool executable | the frozen JVM | the tool against the JVM |
|---|---:|---:|---:|
| `bin/repr-java roundtrip --corpus baseline,samples,jdk --text`, sequential | 837 s (50 s of it making the jdk originals), 30.0 GB | 548 s, 9.3 GB | 1.4 times |
| the same, corpora concurrently (as built) | **543 s**, 29.6 GB, 1,696 s of CPU | (548 s sequential, 2,046 s of CPU) | 0.99 |
| of it, the jdk corpus: F2's `read-jdk` (4 threads), the text level's `read-jdk --full` | 94 s, 124 s | 30 s, 34 s | 3.1, 3.6 |
| of it, javac: the jdk originals, the full layout's print | 19 s, 18 s | 8 s, 9 s | 2.4, 2.0 |
| `bin/repr-java check` (the baseline, the samples, the module) | 24 s, 1.2 GB | 23 s, 1.8 GB | 1.0 |
| `bin/repr-java check --suite` | 178 s, 13.3 GB (the suite on the JVM) | (328 s in the gate of 2026-10-20) | |
| `bin/jrt-convert verify` (its `convert`, its `javac`) | 193 s (96 s, 40 s), 27.8 GB | 114 s (34 s, 20 s), 9.4 GB | 1.7 |
| `bin/clojure-tests --java-fixtures verify` | 20 s, 1.0 GB | 11 s, 0.5 GB | 1.8 |
| `bin/javac-vendor verify` | 94 s, 8.2 GB | 87 s, 8.1 GB | 1.1 |
| `bin/javac-vendor classes baseline samples jdk java.base` | 228 s | (the comparison itself) | |
| the tool's build (`bin/javac-vendor build`, both passes, Go's cache warm) | 231 s (1,006 s from nothing, the shipped build first: 584 s) | | |

The JN4 estimate, 25-35 minutes for the round trip unless its reads ran in parallel, did not hold
even sequentially (14 minutes): the reads are 3-3.6 times the JVM's on the jdk corpus, but prints and
javac dominate the rest, and the printer runs on the tools' host either way. With the corpora
concurrent the round trip takes the JVM's time. Running the jdk corpus's two reads concurrently was
not done: each peaks at about 27 GB.

`bin/gate --full`'s repr/java steps, before and after: the JVM chain's `repr-java`,
`repr-java-roundtrip`, `jrt-convert` and `java-fixtures` (328 + 735 + 151 + 11 s in the gate of
2026-10-20, beside the go chain) and the javac step's `verify` and `host-check` (79 + 393 s, JN3,
JN4) become the java chain on the tool (178 + 543 + 193 + 20 s, about 15.5 minutes, beside the box's
steps) and `verify` on the tool (94 s).

### JN5.6 `bin/gate --full` (JFS9 (a), JFS29)

- the `jvm` chain keeps `j2g-edge` alone (HotSpot's answers);
- the go chain's `javac` step: `bin/javac-vendor check`, `build`, the tool copied into
  `.tmp/gate-javac` (with its key; the box's and the fixpoint's builds run on), `verify` on it (the
  fixpoint), `classes baseline samples jdk java.base` (the JVM's javac the reference while it exists);
  `host-check` is no longer in the gate (JFS29: replaced by the checks themselves);
- when it passes, the new `java` chain runs beside the rest of the go chain, with `REPR_JAVA_HOST=arbace`
  and `REPR_JAVA_EXE` the copy: `repr-java` (`check --suite`), `repr-java-roundtrip`
  (`roundtrip --corpus baseline,samples,jdk --text`), `jrt-convert` (`verify`), `java-fixtures`
  (`--java-fixtures verify`), one after the other (the jdk reads' memory); the step names stay.

Makefile: `repr-java-check` (new, in `make verify`); the `##` lines of `jdk-verify`, `javac-verify`,
`fixtures-verify` and `repr-java-host-check` name the hosts; `make check-makefile` passes.

### JN5.7 The shipped program's change

None in code: no change to `arbace/`, `go/`, `overlay/` or `jdk/forms`. `jdk/provenance.edn`'s two
lines change the runtime key (`bin/lib/version.bash`), and with it the version map the executable
carries (`:runtime`); `jdk/javac/platform.sha256` and `jdk/javac/provenance.edn` are the tool's
(outside the runtime key). j2g's program tree of the build at the branch's start (`ad80344b`, the
provenance as main's) against the essential gate's build at `160e96c3` (`diff -r`): the same but
j2g's `report.edn` (its times), and the compiled namespaces' tree `target/arbace-go/aot` the same;
the executables, 149,196,960 bytes each, differ in the version map (`:commit`, `:describe`,
`:dirty`, `:runtime` `2e3537a4...` to `ddd668ad...`) and Go's build id (2,657 bytes). No re-seed
(batched).

### JN5.8 The checks

Each moved `--full` step's command, as `bin/gate --full` runs it (on the tool's copy, `REPR_JAVA_HOST=arbace`,
`REPR_JAVA_EXE=.tmp/gate-javac/arbace-javac`), alone:

- `bin/javac-vendor check`: j2g translates javac's world, no failures (49 s)
- `bin/javac-vendor verify`: `jdk/javac` read by javac on Arbace is the committed tree, byte for byte
  (574 files; 94 s)
- `bin/javac-vendor classes baseline samples jdk java.base`: byte for byte (228 s)
- `bin/repr-java check --suite`: the baseline's 189 files as `baseline.sha256`, the samples (11) and the
  module (6) as expected; the suite on the converted baseline 809 tests, 20,750 assertions, no
  regressions against `test/baseline-results.edn` (178 s)
- `bin/repr-java roundtrip --corpus baseline,samples,jdk --text`: as the references (543 s)
- `bin/jrt-convert verify`: `jdk/` is the conversion, byte for byte (911 forms files, 10 data files;
  193 s; the first run failed by JN5.4)
- `bin/clojure-tests --java-fixtures verify`: byte for byte (15 files; 20 s)
- on the frozen JVM, for comparison, `bin/jrt-convert verify`, `bin/javac-vendor verify`,
  `bin/clojure-tests --java-fixtures verify`, `bin/repr-java check`, `roundtrip ... --text`: all pass
- `bin/gate` (essential, on the branch at `160e96c3`): passes in 16m02s (go-build, go-smoke,
  suite-go 777 tests with no regression, oracle-go 53 known mismatches as recorded, diffcheck 12,741
  of 12,764 cases agree with the 23 known, goi-check 740 of 740, generated)
- the gate's `--full` chains' order, mocked (`step` replaced by a printer): the java chain starts
  after the javac step and runs beside box-forms, box-smoke, box-oracle, box-suite and the fixpoint;
  when the javac step fails it does not run (its steps then report "not run")
- not run: `bin/gate --full` as a whole (the user's rule: at the milestone)

### JN5.9 What it means for JN6

Nothing of repr/java needs the JVM now but as the reference: `REPR_JAVA_HOST=jvm` (the JVM's read,
`host-check`), `check --suite`'s back end (the frozen jar's compiler and the suite on the JVM, which
§1.3 keeps there), the edges' recordings (`bin/j2g-edge`, `bin/javac-vendor edge`: JVM data by
design, JFS14) and `bin/javac-vendor classes`' reference javac. What JN6 removes: the JVM branch of
`bin/repr-java` (and with it `host-check`, JFS36), `frozen_view tools`' repr/java links, the
provenance's `java -version` (JFS35). What stays on the frozen jar after JN6 and is not repr/java's:
`bin/jrt-convert`'s own namespaces (`jrt.jrt-sources`: the source list, the locales, the edge, the
report; they could run on the tools' host, but the edge reads class files through the JVM-only
`arbace.classes.jvm`) and the generators (JN7).

### JN5.10 Proposals

- **JFS31 The platform for javac's own sources.**
  - Options: (a) the one platform grown by javac's modules, jdk.compiler, java.compiler,
    jdk.internal.opt and jdk.zipfs (jdk.compiler's `requires`), pinned with the others (built); (b) a
    second platform for `bin/javac-vendor` alone, the corpora's unchanged; (c) javac's modules read
    with `--module-source-path` instead of `--patch-module`, no platform module for them (another
    shape of attribution than the JVM's read, so other forms possible).
  - *Recommended: (a), built*: the JVM's javac sees every module of the image, so the grown platform
    is nearer to it, and every corpus's class files and forms are unchanged
    (`bin/javac-vendor classes`, the round trip, `check`, `verify`s).
- **JFS32 How the verifies treat the provenance's host.**
  - Options: (a) byte for byte, the host included: every change to the tool's sources (nearly any
    change to `arbace/`, `go/`, `overlay/`, `jdk/`) fails `bin/jrt-convert verify`, `bin/javac-vendor
    verify` and `--java-fixtures verify` until their updates are run again, and changes the runtime key
    with `jdk/provenance.edn`; (b) the provenance compared but its `:host` line, both hosts printed
    (built); (c) a narrower key (only the reader, javac's world and the platform): fewer changes, but
    it would not name what the tool is (j2g, jrt and the runtime decide its output as much).
  - *Recommended: (b), built*: the host records which tool wrote the files, as a version, and the
    verify proves that today's tool (or the JVM) writes the same forms; `update` refreshes the host.
- **JFS33 The default host after JN5.**
  - Options: (a) the JVM until JN6, the tool by `REPR_JAVA_HOST=arbace` (as JN4): the committed
    provenance would name a host the default does not run, and an `update` run without the variable
    would write the JVM back into it; (b) javac's tool executable the default, built by hand
    (`bin/javac-vendor build`, a missing tool named with the command), provenance writers refusing a
    tool not built from the checkout (`REPR_JAVA_STALE_OK=1` to go on), the JVM by
    `REPR_JAVA_HOST=jvm` until JN6 (built); (c) as (b), the tool built on demand when missing or of
    another key, under a lock, as stage 0 is.
  - *Recommended: (b), built*: the checks and the committed files already use the tool, and a source
    change must not start a 4-17 minute build inside a read (with (c), every edit to `arbace/` would);
    the reader's own changes run before a rebuild anyway (JN4.3, `RT.toolSource`). (c) if building by
    hand proves a nuisance.
- **JFS34 The round trip's parallelism.**
  - Options: (a) sequential, as on the JVM (837 s on the tool); (b) the corpora concurrently, the jdk
    corpus's originals compiled while F2 is read and its `anew` print while the full print compiles
    (543 s, built); (c) also the jdk corpus's two reads concurrently (about 27 GB each: near the
    machine's 62 GB, beside the gate's other chains).
  - *Recommended: (b), built*: the JVM's time, the jdk read's memory once.
- **JFS35 The provenance's `:jdk` on the tool.**
  - Options: (a) unchanged, `java -version`'s first line (the JDK build tree the platform is made
    of names the same release; built: no change); (b) the platform by its pin
    (`jdk/javac/platform.sha256`'s hash), no JVM asked.
  - *Recommended: (a) now, (b) at JN6*, when the JVM leaves repr/java and `java` is no longer asked.
- **JFS36 `bin/repr-java host-check` after JN5.**
  - Options: (a) kept as a command and in `make verify` (not in the gate), until JN6 removes the
    JVM's read it compares with (built); (b) removed now.
  - *Recommended: (a)*: it is the one check of the tool's reads against the JVM's while both exist,
    and costs nothing in the gate.

### JN5.11 Sources and scratch

- jdk26u at `baf63fbe42b8758448fee570a9d9bb914272d259` (`/root/jdk26u`): the build tree's
  `jdk/modules/{jdk.compiler,java.compiler,jdk.internal.opt,jdk.zipfs}` and their `images/jmods`
  (the platform), `src/jdk.compiler/share/classes/module-info.java` (its `requires`).
- The worktree's `.tmp/`: `jn5/` (`run.sh`, the runs' logs and `/usr/bin/time` outputs, `runs.txt`,
  `moved.sh` and `cmp.sh`, the commands as the gate runs them and the JVM's, `gatemock.sh`, the
  gate's chain order mocked, `ref-jvm/`, the javac step's classes by the JVM's javac, `base/`, the
  program tree before the provenance changed), `javac-vendor/`, `jrt/`, `repr-java-roundtrip/`,
  `gate-javac/` (the tool's copy).

## Decided (2026-10-21): JFS31-JFS36

The user decided JFS31-JFS36 as built:
- JFS31 (a): the one platform grown by jdk.compiler, java.compiler, jdk.internal.opt and jdk.zipfs.
- JFS32 (b): the verifies compare the provenance but its `:host` line, both hosts printed.
- JFS33 (b): javac's tool executable the default host, built by hand; the JVM by
  `REPR_JAVA_HOST=jvm` until JN6.
- JFS34 (b): the round trip's corpora concurrently.
- JFS35 (a) now, (b) at JN6.
- JFS36 (a): `bin/repr-java host-check` kept, out of the gate, until JN6.

## As built: JN6 (2026-10-21)

JN6, the JVM leaves repr/java, on the branch `java-frontend-jn6` from main `02d2c34b` (whose seed
holds JN3 and JN4; JN5 is not in it), in about four hours of work (against the day estimated).
`bin/repr-java` starts no JVM: its read runs on javac's tool executable alone, the frozen JVM's read
(`REPR_JAVA_HOST=jvm`) and `host-check` are gone, `frozen_view tools` holds no repr/java, and the
provenance files' `:jdk` names the platform by its pin, not `java -version` (JFS35 (b)). The two
JVM-only remainders of repr/java's own work moved too: `check --jdk` (jdk26u's 68 modules) runs on the
tool, the platform grown to the JDK image's 68 modules (JFS37), and `bin/jrt-convert`'s source list,
locales and report run on the tools' host (JFS40). Every corpus's forms, the round trip's references
and the class files are unchanged. A trace of every exec (strace) over the affected `--full` steps'
commands shows no JVM under `bin/repr-java` (JN6.4). The JVM ran only the frozen v4 jar (the edges'
recordings, `check --suite`'s back end and suite, `bin/jrt-convert`'s edge step, the trace's control
run of main's JVM read), the frozen JDK's `javac` (the reference of `bin/javac-vendor classes`) and the
system JDK's generators (`bin/jrt-convert generate`, JN7's); Go only TamaGo's go1.27.1, without cgo.
The tools' host was the worktree's stage 0; j2g for the tool ran on the program built from the
checkout (JFS22 (a)).

### JN6.1 `bin/repr-java` without the JVM

- **`bin/repr-java`**: the JVM branch (`bin/lib/frozen.bash`, `frozen_java tools` with javac's
  `--add-exports`, `REPR_JAVA_JVM_OPTS`) and the command `host-check` (`bin/lib/repr-java/host-check`)
  removed; `read`, `read-jdk`, `coverage`, `main`, `javac` and `declarations` run on javac's tool
  executable (`REPR_JAVA_EXE`), as they did by default since JN5. `REPR_JAVA_HOST` unset or `arbace`
  is accepted; `jvm` stops with a message naming this section (JFS38), any other value as before.
- **`arbace/repr/java/javac.ae`**: the reader's JVM branch (javac's standard file manager against the
  running JDK, taken when `jdk.internal.jrt.javac.FileManager` was absent) removed; elsewhere than on
  the tool `analyze` now refuses with a message. With `java.ae`'s fix (JN6.7) and `full.ae`'s
  docstring the reader's only changes; they move the provenance's `:repr-java` hash (`2edfddeb...` to
  `8028aa94...`).
- **`bin/lib/repr-java/host.bash`** (new): `repr_java_host`, `repr_java_provenance_same`,
  `repr_java_reader_hash`, moved out of `bin/lib/frozen.bash` (the JVM's half of `repr_java_host` and
  `frozen_host` dropped), and `repr_java_platform` (JFS35 (b)). Under `bin/lib/repr-java/`, the check
  scripts' directory, so that the tool's key (`bin/lib/javac-key.bash`) does not hold them, as it did
  not hold `frozen.bash`. The provenance writers (`bin/jrt-convert`, `bin/clojure-tests
  --java-fixtures`, `bin/javac-vendor`) source it, not `frozen.bash`.
- **`frozen_view tools`** links no `arbace/repr/java` (the awk's explicit exclusion: the jar of v4
  holds no `arbace/repr/java`, so the rule "every namespace the jar lacks" would have linked it
  anyway) and removes the links a view made before JN6 holds. Its users, `bin/j2g-edge`,
  `bin/javac-vendor edge`, `bin/jrt` and `bin/jrt-coverage`, need none of it (`bin/j2g-edge verify`
  passes, JN6.6).
- **`bin/lib/repr-java/check`** sources `frozen.bash` only for `--suite`'s step 4; **`roundtrip`**,
  **`bin/jrt-convert`**, **`bin/javac-vendor`**, **`bin/clojure-tests --java-fixtures`** and
  **`test/repr/java/jdk.bash`** lost `REPR_JAVA_JVM_OPTS` (the JVM read's heap); `--java-fixtures`
  always takes the runtime's declarations as javac's class path (the frozen jar was the JVM's).
- **`bin/gate`**: the java chain and the javac step no longer set `REPR_JAVA_HOST`.

### JN6.2 The provenance's `:jdk` (JFS35 (b))

`repr_java_platform` prints `platform SHA-256`, the SHA-256 of `jdk/javac/platform.sha256`: the
platform javac on Arbace attributes against (jdk26u's build tree's exploded modules with the jmods'
descriptors), so no `java -version` is asked. The three provenance files were written on the tool
(`bin/jrt-convert update`, `bin/clojure-tests --java-fixtures update`, `bin/javac-vendor convert`):
in each only `:jdk` (`"openjdk version \"26.0.2.1-internal\" 2026-08-18"` to `"platform
7abd8629..."`), `:repr-java` (JN6.1) and `:host` (`arbace-javac ee04e344...` to `arbace-javac
811e563f...`, the final tool's) changed. `jdk/provenance.edn`'s `:gen-jdk` stays `java -version`'s line of `GEN_JDK`,
the generators' JDK (JN7).

### JN6.3 The remainders decided

| remainder (JN5.9) | decided | as built |
|---|---|---|
| `check --jdk`: the platform held 10 modules | moved (JFS37) | the platform holds every module of the build tree's `jdk/modules`, the 68 of the JDK image (`bin/javac-vendor`'s `platform_modules`, a function of the build tree now; `jdk/javac/platform.sha256` 58 lines more, the 10 of JN5 unchanged); `check --jdk` reads them on the tool (JN6.5). The unnamed module's default roots are the full image's again, as on the JVM: every corpus's forms and class files are unchanged (JN6.6) |
| `bin/jrt-convert`'s own namespaces | moved but the edge (JFS40) | `jrt.jrt-sources` (the source list, the locales and `LocaleDataIndex`, the report) on the tools' host (`arbace_tool -p test`: 0.7 s for `print-sources` against a JVM's start); the edge split into `jrt.jrt-edge` (`test/jrt/jrt_edge.ae`), on the frozen jar: it reads javac's classes with `java.lang.classfile` and `jrt.java-surface`'s class reading, which jrt has not. `sources.txt`, `LocaleDataIndex.java` (so its forms), the report: the same |
| the edges' recordings (`bin/j2g-edge`, `bin/javac-vendor edge`) | kept on the JVM | JVM data by design (JFS14): HotSpot's reflection and method order |
| `check --suite`'s back end | kept on the JVM | the frozen jar's Java forms compiler and the suite on the JVM (§1.3, O2 (a)); the Go test build runs the suite on the converted forms already |
| the reference javac of `bin/javac-vendor classes` | kept (JFS41) | the JDK's javac (not the frozen jar) as the reference of javac on Arbace's class files, while a JDK is at hand |
| `REPR_JAVA_HOST=jvm` | removed (JFS38) | stops with a message |
| `bin/repr-java host-check` | removed (JFS39) | with the JVM's read it compared against |
| the generators (`bin/jrt-convert generate`) | JN7 | on `GEN_JDK` |

### JN6.4 The trace: nothing of repr/java starts a JVM

Each affected `--full` step's command ran alone, as `bin/gate --full` runs it (the tool
`target/arbace-go/amd64/arbace-javac`, key `21adc92f...`, before JN6.7's fix of the reader; the
updates, the three verifies, `check` and `check --jdk` again on the final tool, key `811e563f...`, the
counts the same but `check --jdk`'s), under `strace -f --seccomp-bpf -e
trace=execve,fork,vfork,clone,clone3` (`.tmp/jn6/trace.sh`): every exec and the process tree, so a
JVM started by an absolute path (`$GEN_JDK/bin/java`) or through a script counts too. `.tmp/jn6/javas.py`
finds every exec of a `java` or `javac` binary, rebuilds the chain of processes above it (a pid's
history reset at each fork, since pids are reused) and counts those with a `bin/repr-java` process
above them other than `check` and `roundtrip` (which `exec` the check scripts: `check --suite`'s step
4 is the back end, and the round trip's own reads and compiles are `bin/repr-java read`, `read-jdk`
and `javac` processes below it). A control run of main's `bin/repr-java` (`02d2c34b`, exported into
`.tmp/jn6/control`) with `REPR_JAVA_HOST=jvm` over the samples is found: 2 execs under `bin/repr-java
read` (the `/usr/bin/java` launcher re-execs the JDK's, so each JVM is two execs).

| command | JVM execs | under `bin/repr-java` | what they are |
|---|---:|---:|---|
| control: main's `REPR_JAVA_HOST=jvm bin/repr-java read --check` of the samples | 2 | **2** | the frozen jar, view `tools`, `-m arbace.repr.java` |
| `bin/javac-vendor check` | 0 | 0 | |
| `bin/javac-vendor verify` | 4 | 0 | the frozen jar, view `tools`: `arbace.j2g.main --record-edge`, `arbace.j2g.edge subtract` (the edge) |
| `bin/javac-vendor classes baseline samples jdk java.base` | 110 | 0 | the reference javac (12), the jdk corpus's originals' generators on `GEN_JDK` (82, with their 16 javac) |
| `bin/repr-java check --suite` | 123 | 0 | step 4: the frozen jar, view `test`, `repr.java.build` (2); Clojure's suite on the JVM (117, its javac 2, the AOT example 2) |
| `bin/repr-java roundtrip --corpus baseline,samples,jdk --text` | 97 | 0 | the jdk corpus's originals' generators (82, with their 15 javac) |
| `bin/jrt-convert verify` | 100 | 0 | the generators (82, 16 javac); the frozen jar, view `test`, `jrt.jrt-edge` (2) |
| `bin/clojure-tests --java-fixtures verify` | **0** | 0 | |
| `bin/j2g-edge verify` | 6 | 0 | the frozen jar, view `tools`: `arbace.j2g.main`, `arbace.j2g.edge` |
| `bin/repr-java check --jdk` (JN6.5) | 0 | 0 | |
| the updates (`bin/jrt-convert update`, `--java-fixtures update`, `bin/javac-vendor convert`) | 135, 0, 4 | 0 | as their verifies, and the frozen jar built once from its tag inside the edge step (jrt-convert's first run in the worktree) |

So the frozen jar is used only by the oracle (not in these steps), the edges (`arbace.j2g.main`,
`arbace.j2g.edge`), `check --suite` (`repr.java.build`, then the suite) and `bin/jrt-convert`'s edge step
(`jrt.jrt-edge`, a data generator); no JVM runs `arbace.repr.java`. The traces: `.tmp/jn6/trace/`,
the summary `summary.txt`. The steps' times under the trace are longer than the gate's (ptrace stops
at every thread's creation: `check --suite` 608 s against JN5's 178 s, the round trip 1,012 s against
543 s); they are not the measure of this milestone.

### JN6.5 `check --jdk` on the tool

`bin/repr-java check --jdk` (`test/repr/java/jdk.bash`) reads every module of jdk26u's
`src/*/share/classes`, largest first, with `read-jdk --check` on the tool, each module attributed
with `--patch-module` against the platform of 68 modules (JFS37). Two bugs stopped it first (JN6.7);
then, on the final tool (key `811e563f...`):

| | |
|---|---:|
| modules | 68 (63 with Java files; 5 of `module-info.java` alone) |
| Java files read, converted | 12,730, 12,730 |
| failures, javac errors, files unreadable by `arbace.core/read` | 0, 0, 0 |
| files written | 13,566 |
| wall time (one module at a time, `JDK_THREADS` 4 chunks; traced) | 14 m 16 s |
| peak RSS of a reader process | 8.5 GB |
| the largest: java.base, java.desktop, java.xml | 3,088 files 180 s, 2,287 files 144 s, 1,854 files 92 s |

The defaults changed for the tool: `JDK_JOBS` 1 (was 2 JVMs of 10 GB), `JDK_THREADS` 4 chunks per
module (was 8 threads in the JVM). The JVM's last full run (2026-10-07, CONVERTER-NOTES' "The converted
JDK") had the same 68 modules without failure; the forms are not compared (`check --jdk` checks only
that every file converts and reads back), as since S4.

### JN6.6 The checks

Each affected `--full` step's command alone, as `bin/gate --full` runs it, on the tool (all traced,
JN6.4):

- `bin/javac-vendor check`: j2g translates javac's world, no failures (65 s)
- `bin/javac-vendor verify`: `jdk/javac` read by javac on Arbace is the committed tree, byte for byte
  (574 files, the grown pin included; 111 s)
- `bin/javac-vendor classes baseline samples jdk java.base`: byte for byte the JVM's javac's
  (baseline 812 classes, samples 28 and 4, jdk 2,224, java.base 7,434 and the build's but
  `module-info.class`), with the platform of 68 modules (364 s)
- `bin/repr-java check --suite`: the baseline's 189 files as `baseline.sha256`, the samples (11) and
  the module (6) as expected; the suite on the converted baseline 809 tests, 20,750 assertions, 20,718
  passing, no regressions against `test/baseline-results.edn` (608 s traced)
- `bin/repr-java roundtrip --corpus baseline,samples,jdk --text`: as the references (baseline forms
  189, ae 182, classes 143 of 183; samples 17, 17, 12 of 14; jdk forms 874 of 911 (7 failing, 30
  missing), ae 816, classes 473 of 838; the text level record, text and classes all, `anew` 183, 14,
  799 of 838; 1,012 s traced)
- `bin/jrt-convert verify`: `jdk/` is the conversion, byte for byte (911 forms files, 10 data files;
  `jrt.jrt-sources` on the tools' host; 280 s)
- `bin/clojure-tests --java-fixtures verify`: byte for byte (15 files; 22 s)
- `bin/j2g-edge verify`: `jdk/edge.edn` is the JVM's answer, with `frozen_view tools` without
  repr/java (76 s)
- `bin/repr-java check`: as expected (30 s; 50 s traced on the final tool), and `make check-makefile`
- on the final tool (`811e563f...`, after JN6.7's fix), the updates, `bin/jrt-convert verify` (335 s),
  `--java-fixtures verify` (26 s), `bin/javac-vendor verify` (172 s), `check` and `check --jdk` traced
  again (no JVM under `bin/repr-java`), and untraced the round trip (as the references, 592 s), `check
  --suite` (no regressions, 194 s) and `bin/javac-vendor classes baseline samples jdk java.base` (byte
  for byte, 300 s)
- `bin/gate` (essential): on the branch at `d173a4b6`, passes in 26m07s (go-build 830 s, both
  builds afresh; go-smoke, suite-go 777 tests with no regression, oracle-go 53 known mismatches as
  recorded, diffcheck 12,741 of 12,764 cases agree with the 23 known, goi-check 740 of 740, generated)
- not run: `bin/gate --full` as a whole (the user's rule: at the milestone)

### JN6.7 Bugs found

- **A module of `module-info.java` alone stopped `read --check`** (`java.se`, `jdk.crypto.ec`,
  `jdk.jdwp.agent`, `jdk.graal.compiler`, `jdk.graal.compiler.management` in jdk26u: their Java is
  generated or elsewhere): `read-jdk` skips the descriptor, writes no file, and the report's `spit`
  found no `OUT` ("FileNotFoundException ... repr-java-report.edn"). The frozen JVM's read failed the
  same (main's `REPR_JAVA_HOST=jvm` on `java.se`), so `check --jdk` could not have passed on the JVM
  either since the report was written by `read` itself. Fixed in `arbace/repr/java.ae` (`OUT` made
  before the report). No regression form (the reader is not in `bin/diffcheck`'s corpora); `check
  --jdk` is its test.
- **`bin/repr-java check --jdk` had not run since 2026-10-17.** `test/repr/java/jdk.bash` took the
  repository root as `$(dirname "$0")/../..`, two levels up, from when it was `test/j2a/jdk.bash`;
  the move to `test/repr/java/` (`789b8534`, R1's renames) made it `test/`, so every module "failed"
  with `bin/repr-java: No such file or directory` at once. Fixed (`../../..`). No regression form:
  a script, not Arbace code; the check itself is its test.
- **The trace's own first version** missed `fork()` lines (strace pads them before `=`) and merged
  reused pids' histories; the control run caught both before the results above were read.

### JN6.8 The shipped program's change

None in code: `arbace/repr/java` is not in the shipped program (j2g's `embed.ae` leaves it out; the
tool embeds it), and `go/`, `overlay/`, `jdk/forms`, `jdk/data` and `jdk/edge.edn` are unchanged.
`jdk/provenance.edn` (JN6.2) and `jdk/README.md` (its rows' text) change the runtime key
(`bin/lib/version.bash`'s `arbace_runtime_key` holds `jdk/` but j2g's edge), and with it the version map
the executable carries (`:runtime`), as at JN5. `jdk/javac/` is the tool's (outside the runtime key).
No re-seed (batched).

### JN6.9 What stays on a JVM, and why

| use | on | why |
|---|---|---|
| the oracle's reference (`bin/oracle`, `test/oracle/expected`) | the frozen jar | the frozen v4 Arbace is the reference by definition |
| j2g's edge (`bin/j2g-edge update`, `verify`, `trace`) and javac's (`bin/javac-vendor edge`, in `convert` and `verify`) | the frozen jar | the JVM's reflection over the classes outside the forms and HotSpot's method order (JFS14) |
| `bin/repr-java check --suite`'s step 4 | the frozen jar, then the suite's JVMs | the converted baseline compiled by the jar's own Java forms back end (main has none, O2 (a)) and Clojure's suite run on its classes |
| `bin/jrt-convert`'s edge step (`jrt.jrt-edge`) | the frozen jar | javac's classes read with the JDK's class file API and `jrt.java-surface` (JFS40); a report (`edge.edn`, the report's tables), compared with nothing |
| the data generators (`bin/jrt testdata`, `manifest`, `standins`, `bin/jrt-coverage`, `bin/net-check`'s servers) | the frozen jar | they read the JVM |
| `bin/jrt-convert generate` | a JDK (`GEN_JDK`) | the JDK build's generators, Java run on a JDK (JN7) |
| the reference javac of `bin/javac-vendor classes` | a JDK (`javac` on `PATH`) | the reference of javac on Arbace's class files (JFS41) |

Nothing of it is repr/java's read or print, and nothing of it runs `arbace.repr.java`.

### JN6.10 Proposals

- **JFS37 The platform for `check --jdk`.**
  - Options: (a) the one platform grown to every module of the build tree, the 68 of the JDK image,
    pinned with the others (built); (b) a second platform of 68 modules for `check --jdk` alone, the
    corpora's of 10 unchanged; (c) `check --jdk` kept on the JVM (the frozen jar's view keeping
    repr/java for it alone).
  - *Recommended: (a), built*: the JVM's javac sees the whole image, so the full platform is what the
    committed forms were read against until JN5; every corpus's forms and class files are unchanged,
    the platform is links (made in seconds), and (c) would keep the JVM's read alive for one check.
- **JFS38 `REPR_JAVA_HOST=jvm` after JN6.**
  - Options: (a) kept as an opt-in reference (it needs `frozen_view tools` to keep repr/java, the
    reader's JVM branch in `javac.ae` and `bin/repr-java`'s JVM branch: JN6's exit criteria not
    met); (b) removed, the variable's `jvm` stopping with a message (built); (c) removed silently.
  - *Recommended: (b), built*: the references are the committed outputs, which every verify compares
    byte for byte, and javac's class files, compared with the JDK's javac (`bin/javac-vendor
    classes`); the JVM's read is on the frozen branch's history and at main's `02d2c34b` if ever
    wanted. A stale setting fails loudly rather than reading on another host.
- **JFS39 `bin/repr-java host-check` (JFS36 at JN6).**
  - Options: (a) kept (needs JFS38 (a)); (b) removed with the JVM's read (built).
  - *Recommended: (b), built*: its reference is gone, and what it compared (every corpus) is what the
    verifies and `check` compare with the committed files.
- **JFS40 `bin/jrt-convert`'s own namespaces.**
  - Options: (a) `jrt.jrt-sources` on the tools' host, the edge split into `jrt.jrt-edge` on the
    frozen jar (built); (b) all of it on the frozen jar, as JN5; (c) the edge too on the tools' host,
    with a class-file reader of Arbace's own (javac on Arbace's `ClassReader`, or a small reader in
    Arbace) in place of `java.lang.classfile`, and `jrt.java-surface`'s reading ported.
  - *Recommended: (a) now, built; (c) later* (with JN7, when the generators leave the JDK too): the
    edge is a report compared with nothing, and porting `jrt.java-surface` (1,051 lines over the
    class file API) is a task of its own.
- **JFS41 The reference javac of `bin/javac-vendor classes`.**
  - Options: (a) the JDK's javac kept as the reference in `bin/gate --full`'s javac step while a JDK
    is at hand (built: unchanged); (b) the class files of javac on Arbace recorded as references
    (hashes, as `baseline.sha256`), the JDK no longer needed; (c) dropped.
  - *Recommended: (a)*: javac on Arbace against the JDK's javac is the check that R1 holds, and the
    JDK stays on the host for the generators (JN7) anyway; (b) when the JDK leaves the host.
- **JFS42 `check --jdk` in the gate.**
  - Options: (a) out of every gate, a command and a Makefile target (`repr-java-jdk`, built); (b) in
    `bin/gate --full`'s java chain.
  - *Recommended: (a)*: 14 minutes, 8.5 GB, and the round trip's jdk corpus already reads java.base's
    closure; run it when the reader or javac's world changes.

### JN6.11 Sources and scratch

- jdk26u at `baf63fbe42b8758448fee570a9d9bb914272d259` (`/root/jdk26u`): the build tree's
  `jdk/modules` (the 68 modules) and `images/jmods` (the platform), `src/*/share/classes` (`check --jdk`).
- Arbace at main `02d2c34b` (the control run's `bin/repr-java`, `.tmp/jn6/control`).
- The worktree's `.tmp/`: `jn6/` (`build.sh` and the build logs, `trace.sh`, `javas.py`,
  `updates.sh`, `checks.sh`, `trace/` with every traced run's strace log, output and `status.txt`,
  `summary.txt`, `control/`, `jdk.out`), `javac-vendor/`, `jrt/`, `repr-java-roundtrip/`,
  `repr-java-jdk/`, `clojure-tests/`.

## Decided (2026-10-22): JFS37-JFS42

The user decided JFS37-JFS42 as built:
- JFS37 (a): one platform, grown to every module of the build tree, the JDK image's 68.
- JFS38 (b): `REPR_JAVA_HOST=jvm` removed; set, it stops with a message.
- JFS39 (b): `bin/repr-java host-check` removed with the JVM's read.
- JFS40 (a) now: `jrt.jrt-sources` on the tools' host, `jrt.jrt-edge` on the frozen jar; (c) later,
  with JN7: a class-file reader of Arbace's own for the edge.
- JFS41 (a): the JDK's javac kept as the reference of `bin/javac-vendor classes` while a JDK is at hand.
- JFS42 (a): `check --jdk` out of every gate, a command and the Makefile's `repr-java-jdk`.

## As built: JN7 (2026-10-22)

JN7, the generators on Arbace, on the branch `java-frontend-jn7` from main `f76981ca` (whose seed
holds JN6 and GP5), in about seven hours of work (against the 3-5 days estimated). The JDK build's
tools that `bin/jrt-convert`'s generate step runs (generatecharacter, generateextraproperties,
spp, cldrconverter with java.xml's validating SAX parser, generatelsrequivmaps, tzdb,
generatecurrencydata, generatebreakiteratordata, makejavasecurity) are read by repr/java on Arbace
into `jdk/javac/forms` and run as Java forms translated into javac's tool executable: every output
is the JDK build's byte for byte (`generated.edn`) but `CaseFolding`, which follows the running
JDK's Unicode version and is the stock JDK 26's byte for byte (JN7.3, JFS43). With JFS40 (c) the
edge step's `jrt.jrt-edge` runs on the tools' host, reading class files with a reader of Arbace's
own: `bin/jrt-convert` starts no JVM (JN7.6). `jdk/forms` and `jdk/data` are unchanged. The JVM ran
the frozen v4 jar (the edges' recordings, the fixtures' recording, the edge's control run) and the
JDK's `javac` (the reference of `bin/javac-vendor classes`), and, for three one-time measures in
scratch named below (JN7.2, JN7.3, JN7.4), the generators' JDK and the JDK 26; Go only TamaGo's
go1.27.1, without cgo. The tools' host was the worktree's stage 0; j2g for the tool ran on the
program built from the checkout (JFS22 (a)).

### JN7.1 The tools as Java forms (JFS45)

- **`bin/javac-vendor`** reads two more modules into `jdk/javac/forms` (`sources.txt`'s new kinds):
  `buildtools`, the class path: the 50 files of `make/jdk/src/classes/build/tools/{cldrconverter,
  generatebreakiteratordata, generatecharacter, generatecurrencydata, generateextraproperties,
  generatelsrequivmaps, makejavasecurity, spp, tzdb}` (KIND `make`) and the 4 interim copies of
  `java/time/zone/{ZoneOffsetTransition, ZoneOffsetTransitionRule, ZoneRules, Ser}` in package
  `build.tools.tzdb` as `make/CopyInterimTZDB.gmk` makes them (KIND `interim`, the `sed` of the
  build), read by `bin/repr-java read` with the build's `--add-exports java.base/sun.text=ALL-UNNAMED`;
  and `java.xml`, 221 files of `src/java.xml/share/classes` (JN7.2). java.base gains 6 files of
  jdk26u (`Readable`, `Scanner`, `PropertyResourceBundle`, `ServiceConfigurationError`,
  `DeflaterOutputStream`, `sun.text.resources.BreakIteratorRules`: the list
  `overlay/jdk/javac/closure.txt`) and one of jrt's own, `java.util.zip.Deflater` (JN7.4).
  `arbace/repr/java.ae`'s `read` takes `--origins` as `read-jdk` does, so the tools' forms name
  `jdk26u make/jdk/src/classes/...` in their headers, not the work tree.
- **Tool-world variants** (`overlay/jdk/javac/variants/`, JFS18 (b)): `ResourceBundle` (newBundle
  reads `NAME.properties` from the resources when no class that is a bundle has the name, as the
  default Control's java.properties format: cldrconverter's `OtherCommonLocales.properties`,
  java.xml's messages), `Files` (the two `walk`s, depth first in jrt's listing order, for
  cldrconverter's time-zone directory), `Scanner` (its `File` constructors without
  `FileInputStream.getChannel`), `jdk.xml.internal.SecuritySupport` (the context class loader is the
  system loader: jrt's threads have none) and `JdkXmlConfig$CatalogHolder` (the JDK's XML catalog,
  java.xml's resource `jdk/xml/internal/jdkcatalog/JDKCatalog.xml`, by a `file:` URI from
  `ARBACE_PATH`: jrt has no `jrt:/` URL handler).
- **j2g** (shipped code, JN7.7): the tool's new packages are named by binary names
  (`tool-module-prefixes`: `build/tools/`, `com/sun/org/apache/`, `com/sun/xml/internal/`,
  `javax/xml/`, `jdk/xml/internal/`, `org/w3c/dom/`, `org/xml/sax/`; `build.tools.tzdb.ZoneRules`
  beside `java.time.zone.ZoneRules`) in package jrt (`pkg-of`: `org/xml/`, `org/w3c/`,
  `build/tools/`, which fell into package lang and made an import cycle); and three fixes found by
  xerces, below.
- **`bin/jrt-convert generate`** calls each tool's `main` by class name with a script the tool runs
  (`gen-tools/gen.ae`), its `ARBACE_PATH` the tools' and java.xml's resource directories
  (`make/jdk/src/classes`, `src/java.xml/share/classes`); the JAVA_SMALL flags' locale is jrt's
  default (en_US). No javac compiles the tools, no `--patch-module` (the patch of java.base is the
  tool's `BreakIteratorRules`; the Thai rules, read only with `-language`, are not converted).
  `provenance.edn`'s `:gen-jdk` (the JDK's `java -version`) became `:generators` (the converter's
  host runs them); `GEN_JDK` is not read (JFS49).

| | JN6 | JN7 |
|---|---:|---:|
| `jdk/javac/sources.txt` | 531 | 813: jdk.compiler 370, java.compiler 135, jdk.internal.opt 1, java.base 32, java.xml 221, buildtools 54 |
| `jdk/javac/forms` | 570 files | 898 files, 9,290,750 bytes |
| `jdk/javac/edge.edn` | 39 classes beyond `jdk/edge.edn`'s, 79,753 bytes | 223, 419,123 bytes |
| the tool's j2g: files, classes analyzed, failing; methods reached, with missing parts | | 2,296, 19,090, 0; 97,219, 617 |
| the tool (amd64, `arbace-javac`) | 200,052,896 bytes | 209,285,280 bytes |
| a tool build (amd64, the reader compiled first) | about 9 minutes | about 12 minutes |

### JN7.2 java.xml's closure, measured (JFS46)

cldrconverter parses CLDR's XML with `SAXParserFactory.newInstance()`, validating, with the DTDs
(`accessExternalDTD` `file`). A rapid type analysis over the class files (the tools' and the
platform's, a scratch namespace on `jrt.classfile`) reached 654 of java.xml's top-level classes
(schemas, DOM, the serializer: whatever the parser's configuration names), and stopping at the
world's classes it still dragged java.base's security and invoke internals; the JDK itself loads
162 of java.xml's when it runs the tools (`-Xlog:class+load` over `bin/jrt-convert`'s generate
step on the generators' JDK, once, in scratch). The committed list is that set, plus the classes the
descriptors of the members their code calls name (j2g translates a call only when its descriptor's
classes are in the world: `XMLGrammarPool` in `SAXParser`'s constructor) and their supertypes, one
level (14, then 37 once a scratch bug was fixed), and the exceptions the parser throws and the tools
catch (9): 221 files, 70 of them never loaded. Their unexecuted paths name 600-odd classes outside
the world, translated as j2g's missing parts. The list is data (`overlay/jdk/javac/closure.txt`),
regenerated by hand when the tools change.

What xerces needed of j2g, each found by the build or by a run, each fixed in j2g:
- **A superinterface's static field read by an implementing class was read uninitialized.**
  `guarded-static?` left out the `_Init()` call for a field of a supertype of the reading class,
  as a class's initialization initializes its superclass; but not its superinterfaces (JLS
  12.4.1). xerces' `NamespaceSupport.reset` stored `NamespaceContext.XML_URI` (`"...".intern()`)
  as null, so every namespace prefix was unbound ("The prefix "xmlns" for attribute "xmlns" ... is
  not bound"). Now the guard is left out for the class itself and its superclasses only. Regression
  case: `test/j2g/fixtures/FxJn7.ae`'s `tIfaceStaticInit` (recorded on the frozen jar).
- **A value of a class outside the closed world passed where an interface of the world is
  expected** (`XSMessageFormatter` as a `MessageFormatter`, `XMLParseException` thrown as a
  `Throwable`): Go's `any` implements no interface of the world; `coerce` converts it (`As`), the
  value being `J2g_Missing`'s, which throws first.
- **A call on such a receiver** (`fId.hashCode()`, `fId` of a class outside the world): the call is
  missing too (`missing-reason` of its target), not a method of `any`.
The last two have no regression form: the interpreted half of `bin/diffcheck` cannot name a class
outside the world (javac's world, the tool build, is their test, as for JN2's cast).

### JN7.3 The outputs (the exit criterion)

`bin/jrt-convert`'s generate step on the tool (`JRT_STEPS=sources,generate`, and within `update` and
`verify`), against the JDK build's `support/gensrc` and module image (`generated.edn`) and against the
same step of main's script on the generators' JDK (`diff -r` of the trees):

| | outputs | equal to the build's | equal to main's run on the JDK |
|---|---:|---:|---:|
| generated sources, default locales | 31 | 30, and `CaseFolding` the build's file (`:boot-jdk`) | 31 (the tree) |
| generated sources, `JRT_LOCALES=all` (jdk.localedata's CLDR bundles) | 1,562 | 1,561, and `CaseFolding` | not run |
| resource data (`uniName.dat` deflated by zlib, `currency.data`, the break iterators' 4, `tzdb.dat`, `java.security`, ICU's 3 copied) | 10 | 10 | 10 |
| cldrconverter's own output directory (base module) | 13 | | 13 |

`CaseFolding.java` (GenerateCaseFolding filters its regex entries with the running JDK's
`Character.toUpperCase`/`toLowerCase`: JRT-SOURCES.md, "The generator JDK matters"): on Arbace, whose
`Character` is jdk26u's (Unicode 17), it is the stock JDK 26's output byte for byte (the same tool
on the JDK 26 build of jdk26u, once, in scratch), 28 entries fewer than the build's (its boot JDK
25's, Unicode 16). The tree takes the build's file when the two differ by entries the build's alone
has (JFS43 (a)), so `jdk/forms` is unchanged and the shipped program's regex stays the JDK 26 it is
checked against. The step is deterministic (two runs, the same trees) and takes 44-62 s on the tool
(cldrconverter 16 s per run, 784 MB peak), against 36-42 s on the JDK.

### JN7.4 zlib's compressor in Java (JFS47)

`build.tools.generatecharacter.CharacterName` writes `uniName.dat` through `DeflaterOutputStream`;
the JDK's `Deflater` is zlib's C code, Go's `compress/flate` writes other bytes, and j2g's own
compressor (`arbace/j2g/zlib.ae`) is not zlib's. `overlay/jdk/javac/java.base/java/util/zip/Deflater.java`
ports zlib 1.3.2's deflate (as jdk26u bundles it, and the host's system zlib, `zlib-1.3.2-r1`, which
the generators' JDK links) to Java: `deflate`, `fill_window`, `longest_match`, `deflate_fast`,
`deflate_slow`, and `trees.c` whole, for levels 1-9, the zlib and raw formats and the flush modes
(not level 0, Huffman-only, dictionaries or a change of parameters mid-stream: they throw). Checked
once in scratch against the JDK's zlib on 170 cases (8 inputs up to 149 MB, 5 levels, input chunks
of 7, 512 and 100,000 bytes, with and without SYNC_FLUSH): equal byte for byte; on the tool,
`uniName.dat` is the build's. Under zlib's License (LICENSE.md).

### JN7.5 A class-file reader of Arbace's own (JFS40 (c))

- **`test/jrt/classfile.ae`** (`jrt.classfile`, 268 lines): a class file as data: the constant pool
  resolved (every tag of Java 26), the class's name, superclass, interfaces and flags, its fields and
  methods with their `RuntimeVisibleAnnotations`' types, and a method's code: its length, the
  instructions that name the constant pool (member, class, `ldc` and `invokedynamic` with their
  bootstrap methods and arguments), the exception handlers' catch types, in `java.lang.classfile`'s
  element order. It reads all 7,434 classes of the build's java.base in 35 s on the tools' host.
- **`jrt.java-surface`** reads with it and takes the JDK classes' facts from the platform's class
  files (`*platform*`, `bin/javac-vendor platform`) as reflection gave them (an interface's
  superclass nil, no constructors among the methods, the fields `Reflection` filters left out), where
  it used `java.lang.classfile` and reflection on the running JVM; **`jrt.jrt-edge`** likewise; the
  edge step runs on the tools' host (`arbace_tool -p test`, 25 s).
- **The check**: the edge of the same `ref1` computed by main's namespaces on the frozen jar and by
  the port: equal but 3 members (1,220 against 1,223): `commit()V` and `isEnabled()Z` of
  `jdk.internal.event.ProcessStartEvent` and `commit()V` of `X509CertificateEvent`, which HotSpot's
  JFR adds to the event classes when it loads them; the class files declare them in
  `jdk.internal.event.Event`, where the port resolves them (JFS44). The rest of `jrt.java-surface`
  (the retired `bin/java-surface`'s closure, units and tables) is ported on the same reader but not
  run (JFS50).

### JN7.6 The trace: `bin/jrt-convert` starts no JVM

Each command alone under `strace -f --seccomp-bpf -e trace=execve,fork,vfork,clone,clone3`
(`.tmp/jn7/trace.sh`), the execs of a `java` or `javac` binary counted with the chain of processes
above them (`.tmp/jn7/scripts/javas.py`, as JN6.4's). The control: main's `bin/jrt-convert`
(`f76981ca`, exported into `.tmp/jn7/control`) with `JRT_STEPS=sources,generate,edge`.

| command | JVM execs | JN6 | what they are |
|---|---:|---:|---|
| control: main's `bin/jrt-convert` (sources, generate, edge) | 134 | | the generators on the generators' JDK (81 `java`, 16 `javac`), the edge on the frozen jar (2), the frozen jar built from its tag in the control's tree (35) |
| `bin/jrt-convert verify` | **0** | 100 | |
| `bin/javac-vendor verify` | 4 | 4 | the frozen jar: `arbace.j2g.main --record-edge`, `arbace.j2g.edge subtract` (the edge) |
| `bin/javac-vendor check` | 0 | 0 | |
| `bin/javac-vendor classes baseline samples jdk java.base` | 12 | 110 | the reference javac (JFS41); the jdk corpus's originals now generated on the tool |
| `bin/repr-java roundtrip --corpus baseline,samples,jdk --text` | **0** | 97 | (the jdk corpus's originals likewise) |
| `bin/clojure-tests --java-fixtures verify` | 0 | 0 | |
| `bin/j2g-edge verify` | 6 | 6 | the frozen jar: `arbace.j2g.main`, `arbace.j2g.edge` |
| `bin/repr-java check` | 0 | 0 | |

So no generator runs on a JDK and nothing of `bin/jrt-convert` starts a JVM; the JVM remains for the
edges' recordings, `check --suite`'s back end, the oracle, the data generators, and the JDK's javac
as the reference (JN7.9). Traces: `.tmp/jn7/trace/`.

### JN7.7 The shipped program's change

j2g's code changed (`arbace/j2g/names.ae`, `code.ae`: JN7.1, JN7.2), so its compiled namespaces in
the program differ; `jdk/provenance.edn` (`:generators`, `:repr-java`, `:host`) and `jdk/README.md`
change the runtime key and with it the version map the executable carries. The translated JDK and
runtime: j2g's program tree (`bin/j2g --program`, without the compiled namespaces) by main's j2g
(`f76981ca`, `.tmp/jn7/control`) and by the branch's, string literals' numbers normalized
(`.tmp/jn7/scripts/normdiff.py`): one function differs, `java.security.MessageDigest`'s
`isEqual`-path call of `equals` on a value of a class outside the closed world, now one missing
call (JN7.2's third fix), and with it one string literal fewer (`j2g_strings.ae`, the others'
numbers shifted): the same behaviour, `J2g_Missing` throwing first. The superinterface's
initialization (JN7.2's first fix) changes nothing in the shipped program. `jdk/forms`, `jdk/data`, `jdk/edge.edn`, `go/`
and `overlay/jdk/java.base` are unchanged; `jdk/javac/` and `overlay/jdk/javac/` are the tool's.
No re-seed (batched).

### JN7.8 The checks

- `bin/jrt-convert update`, then `verify`: `jdk/` is the conversion, byte for byte (911 forms files,
  10 data files; generate, convert, javac, edge on Arbace; 256 s traced)
- `bin/javac-vendor convert`, then `verify`: `jdk/javac` is the conversion and the recording, byte
  for byte (902 files; 146 s)
- `bin/javac-vendor check`: j2g translates javac's world, no failures (62 s)
- `bin/javac-vendor classes baseline samples jdk java.base`: javac on Arbace, in the grown tool, byte
  for byte the JDK's javac's and java.base the build's (271 s)
- `bin/clojure-tests --java-fixtures update`, then `verify`: byte for byte (only the provenance's
  `:repr-java` and `:host` changed; 22 s)
- `bin/j2g-edge verify`: `jdk/edge.edn` is the JVM's answer (65 s)
- `bin/repr-java check`: as expected (26 s); `bin/repr-java roundtrip --corpus baseline,samples,jdk
  --text`: as the references (jdk: record 911, text 838, classes 838, anew 799 of 838; 682 s traced)
- `bin/j2g-check --record-fixtures` (the frozen jar): `test/j2g/fixtures.edn` gains FxJn7's case
- `make check-makefile`
- `bin/gate` (essential): first at `efc39ada`, failed only in diffcheck: FxJn7's file came before
  FxVarHandles, whose known case was renumbered (`test/j2g/fixtures:9` to `:10`, as at JN2;
  `test/diffcheck/known.edn`); then at `36dc57ba` passes (go-build 572 s, both builds afresh, then
  cached; go-smoke, suite-go 777 tests with no regression, oracle-go 53 known mismatches as recorded,
  diffcheck 12,743 of 12,766 cases agree with the 23 known, FxJn7's case among the agreeing,
  goi-check 740 of 740, generated), and again at this section's commit
- not run: `bin/gate --full` as a whole (the user's rule: at the milestone)

### JN7.9 What stays on a JVM, and why

| use | on | why |
|---|---|---|
| the oracle's reference | the frozen jar | the frozen v4 Arbace is the reference by definition |
| j2g's edge and javac's (`bin/j2g-edge`, `bin/javac-vendor edge`) | the frozen jar | the JVM's reflection over the classes outside the forms and HotSpot's method order (JFS14) |
| `bin/repr-java check --suite`'s step 4 | the frozen jar, then the suite's JVMs | the frozen jar's Java forms back end (O2 (a)) |
| j2g's fixtures' results (`bin/j2g-check --record-fixtures`) | the frozen jar | the reference of the fixtures' translations |
| the data generators (`bin/jrt testdata`, `manifest`, `standins`, `bin/jrt-coverage`) | the frozen jar | they read the JVM |
| the reference javac of `bin/javac-vendor classes` | a JDK | JFS41 |

`bin/jrt-convert` is no longer in it: its generators and its edge left with JN7.

### JN7.10 Proposals

- **JFS43 CaseFolding and the generators' Unicode version.**
  - Options: (a) the build tree's file when the tool's output differs from it by entries the
    build's alone has (`:boot-jdk`), else the tool's (built); (b) the tool's output (a JDK 26's)
    committed: `jdk/forms`' `CaseFolding` loses 28 entries and the shipped regex's case-insensitive
    matching differs from the JDK 26 it is checked against for those code points; (c) the boot
    JDK's Unicode version emulated (JDK 25's `CharacterData` from its `src.zip` as the tool's
    `Character` for this tool), or the file committed as an input.
  - *Recommended: (a), built*: the program stays the JDK 26 it is checked against, and the
    difference is the JDK build's own (its output changes with its boot JDK); (c) when the build tree
    goes (JFS24 (b)).
- **JFS44 The edge's three members of the JFR event classes.**
  - Options: (a) the class files' answer, those members resolved to `jdk.internal.event.Event`
    (built); (b) HotSpot's instrumentation emulated (the event classes' `commit`, `isEnabled`,
    `shouldCommit` declared in each subclass).
  - *Recommended: (a)*: the edge is a report of what the translated classes call, and the class
    files are what javac and j2g read.
- **JFS45 Where the tools' forms live.**
  - Options: (a) `jdk/javac/`, one tool world, one conversion, one edge and one `verify` (built);
    (b) a tree of their own (`jdk/buildtools/`, a script, a separate tool executable); (c) not
    committed: read and built on demand by `bin/jrt-convert`.
  - *Recommended: (a)*: the tool that runs repr/java runs them, the tool's key covers them, and a
    second tool would cost a build of its own (12 minutes) and a second edge.
- **JFS46 java.xml's closure.**
  - Options: (a) the classes the JDK loads running the tools, with the descriptors' and the
    exceptions' classes, as a committed list (built; the list regenerated by hand); (b) an analysis
    of Arbace's own over the class files (`jrt.java-surface`'s closure on `jrt.classfile`) without a
    JDK, bounded at the world; (c) all of java.xml (1,854 files).
  - *Recommended: (a) now, (b) when the JDK leaves the host*: (b) as tried over-approximates
    (654 classes of java.xml) and needs the world's semantics (jrt's `ServiceLoader`, `Files`) to
    stop where the JVM does.
- **JFS47 zlib's compressor.**
  - Options: (a) jrt's own `Deflater` in the tool's world (built); (b) in the shipped jrt, with
    `DeflaterOutputStream` and `GZIPOutputStream` in the closure; (c) j2g's `zlib.ae` made zlib's.
  - *Recommended: (a)*: only the tool needs it (JFS18); (b) when the program needs compression.
- **JFS48 javac's generated sources (JFS20 (b)).**
  - Options: (a) still copied from the build tree; (b) `compileproperties` and `propertiesparser`
    (`make/langtools/tools`) run on the tool by `bin/javac-vendor`, `FlagsEnum` still copied; (c) all
    three, `flagsgenerator` with javac's standard file manager on Arbace (JFS5 (a)).
  - *Recommended: (b) next*, as JN7's tools; (c) needs `JavacFileManager` over jrt's file systems.
- **JFS49 The generators' JDK.**
  - Options: (a) removed: the generators run on the tool only (built); (b) kept behind a variable
    (`JRT_GENERATORS=jdk`) as a reference.
  - *Recommended: (a)*: `generated.edn` compares with the JDK build's own outputs, the reference.
- **JFS50 `jrt.java-surface`'s retired parts.**
  - Options: (a) ported on `jrt.classfile`, run only through the edge (built); (b) cut to what the
    edge uses; (c) `bin/java-surface` revived on the tools' host, `doc/go/java-surface.edn` recomputed.
  - *Recommended: (a)*: no reader of the JDK's API remains, and (c) is a task of its own.

### JN7.11 Sources and scratch

- jdk26u at `baf63fbe42b8758448fee570a9d9bb914272d259` (`/root/jdk26u`): `make/jdk/src/classes/build/tools/`
  (the nine tools), `make/CopyInterimTZDB.gmk`, `make/modules/java.base/{Gensrc,Gendata}.gmk`,
  `src/java.xml/share/classes` (221 files and its resources), `src/java.base/share/classes` (the 6
  files), `src/java.base/share/native/libzip/zlib/` (`deflate.c`, `deflate.h`, `trees.c`, `zlib.h`,
  zlib 1.3.2, ported), `src/java.base/share/native/libzip/Deflater.c` (read), the build tree's
  `support/gensrc` and `jdk/modules`.
- Arbace at main `f76981ca` (the control's `bin/jrt-convert`, `.tmp/jn7/control`).
- The worktree's `.tmp/jn7/`: `scripts/` (`closure.ae`, the rapid type analysis; `sigclosure.ae`, the
  descriptors' closure; `notworld.py`; `javas.py`; `port_surface.py`), `cl/` (the JDK's class
  loading logs), `deftest/` (the zlib port against the JDK's), `jrt-ref/` (main's generate on the
  JDK), `jrt-arb/`, `jrt-arb-all/`, `edge-jvm.edn`, `edge-arb.edn`, `trace/`, the build and
  conversion logs.

## Decided (2026-10-22): JFS43-JFS50

The user decided JFS43-JFS50 as built:
- JFS43 (a): the build tree's `CaseFolding` kept where only the boot JDK's entries differ; (c) when
  the build tree goes.
- JFS44 (a): the edge's JFR members as the class files declare them.
- JFS45 (a): the tools' forms in `jdk/javac/`, one tool world.
- JFS46 (a) now, a committed list; (b), an analysis of Arbace's own, when the JDK leaves the host.
- JFS47 (a): jrt's `Deflater` in the tool's world only.
- JFS48 (b) next: `compileproperties` and `propertiesparser` on the tool, `FlagsEnum` copied.
- JFS49 (a): the generators' JDK removed.
- JFS50 (a): `jrt.java-surface`'s retired parts ported, run only through the edge.

## As built: JFS48 (b) (2026-10-22)

JFS48 (b), javac's generated sources on the tool, on the branch `java-frontend-gensrc` from main
`cee9199d`. The sources the JDK build generates for jdk.compiler with its langtools tools
(`make/modules/jdk.compiler/Gensrc.gmk`: `compileproperties`, the resource bundles' classes;
`propertiesparser`, `CompilerProperties` and `LauncherProperties`) are generated by those tools,
read into `jdk/javac/forms` and run on javac's tool executable by `bin/javac-vendor convert`, as
JN7's generators are by `bin/jrt-convert`: all 20, and the intermediate `version.properties`, are the
build tree's byte for byte, with no difference to explain. `FlagsEnum` (`flagsgenerator`) is still
copied (JFS48 (b), JFS52). Nothing of it starts a JVM (JFS48 (b).4). The JVM ran the frozen v4 jar
(javac's edge, as before) and the JDK's `javac` (the reference of `bin/javac-vendor classes`); Go
only TamaGo's go1.27.1, without cgo. The tools' host was the worktree's stage 0; j2g for the tool
ran on the program built from the checkout (JFS22 (a)).

### JFS48 (b).1 What was built

- **The tools as Java forms.** `bin/javac-vendor` reads the 8 files of
  `make/langtools/tools/{compileproperties,propertiesparser}` into module `buildtools` (KIND
  `langtools` in `sources.txt`; the class path, read with the module's `bin/repr-java read`; their
  forms name `jdk26u make/langtools/tools/...`). propertiesparser's templates
  (`propertiesparser/resources/templates.properties`, read by `Class.getResourceAsStream`) are found
  on `ARBACE_PATH` (`make/langtools/tools`), as JN7's resources are; nothing else of the tool world
  was needed: no variant, no new JDK class, and javac's edge (`jdk/javac/edge.edn`) is unchanged,
  byte for byte (223 classes, 419,123 bytes): the tools use only what javac's world already holds
  (`Properties`, `MessageFormat`, `Files`, streams, regex).
- **j2g** (shipped code, JFS48 (b).5): the tools' packages are named by binary names and placed in
  package jrt (`arbace/j2g/names.ae`: `tool-module-prefixes` and `pkg-of` gain `compileproperties/`
  and `propertiesparser/`), as JN7's `build/tools/` (JFS54).
- **The generate step** (`convert`'s first, before the sources are copied): into
  `WORK/gensrc/jdk.compiler`, as the build makes them:
  - `version.properties`, as `GensrcCommon.gmk`'s `SetupVersionProperties` writes it: `jdk=`,
    `full=`, `release=` with the build's `spec.gmk`'s `VERSION_NUMBER`, `VERSION_STRING`,
    `VERSION_SHORT` (JFS51);
  - `compileproperties.CompileProperties -quiet @FILE` (`GensrcProperties.gmk`'s
    `SetupCompileProperties`, TYPE langtools): the 17 `.properties` of
    `src/jdk.compiler/share/classes` and `version.properties`, each `-compile PROPERTIES JAVA
    java.util.ListResourceBundle` (no translation excluded: the build's `EXCLUDE_TRANSLATIONS` is
    empty); the package is inferred from the paths, so the outputs keep the build's layout;
  - `propertiesparser.PropertiesParser -compile compiler.properties DIR -compile
    launcher.properties DIR` (`Gensrc.gmk`'s `PARSEPROPERTIES`);

  each tool's `main` called by its class name on the tool (`WORK/gen-tools/gen.ae`, as
  `bin/jrt-convert`'s), then every output compared with the build tree's
  `support/gensrc/jdk.compiler` (`WORK/generated.edn`: `:same`, `:different`, `:not-generated`,
  `:no-reference`; a difference, or a file the build has and this step does not, fails `convert`).
- **`sources.txt`**: KIND `gensrc` is now a file this step generated (its form's first line says
  "generated by bin/javac-vendor"); KIND `gensrc-build` a file copied from the build tree
  (`FlagsEnum`, "generated by the JDK build"). **`provenance.edn`** gains `:generated` (by what, the
  count, how many equal the build's, what is copied).
- **The bootstrap** (JFS53): a tool without the generators cannot generate; `JAVAC_VENDOR_BOOTSTRAP=1`
  takes the build tree's files instead, and says so in the provenance (`:generated {:by "the JDK
  build tree (bootstrap)"}`), which `verify` (always generating) then finds different. Built so:
  the tool at `cee9199d` (key `56036ed4`) converted the tree with the bootstrap; the tool built from
  that tree (key `da2869a3`) generated the 20 files equal to the build tree's, so its conversion
  wrote the same forms (the provenance aside, which is not in the tool's key): the tree is a
  fixpoint of the tool, as JN5's javac reading javac is.

| | JN7 | JFS48 (b) |
|---|---:|---:|
| `jdk/javac/sources.txt` | 813 | 821: jdk.compiler 370 (20 generated here, 1 copied), java.compiler 135, jdk.internal.opt 1, java.base 32, java.xml 221, buildtools 62 |
| `jdk/javac/forms` | 898 files, 9,290,750 bytes | 910 files, 9,349,032 bytes (the tools' 12, 58,222 bytes) |
| `jdk/javac/edge.edn` | 223 classes, 419,123 bytes | the same, byte for byte |
| the tool's j2g: files, classes analyzed; methods reached, with missing parts | 2,296, 19,090; 97,219, 617 | 2,306, 19,175; 97,575, 617 |
| the tool (amd64, `arbace-javac`) | 209,698,976 bytes (`cee9199d`) | 210,055,328 bytes |
| a tool build (amd64, the reader compiled first) | about 12 minutes | 680 s |
| the generate step | | 1 s (both tools) |
| `bin/javac-vendor convert` | 146 s | 224-251 s (the edge's recording 51-129 s, the machine shared) |

### JFS48 (b).2 The outputs (the exit criterion)

| output | by | equal to the build tree's |
|---|---|---:|
| `compiler.java`, `compiler_{de,ja,zh_CN}.java`, `ct.java`, `javac.java`, `javac_{de,ja,zh_CN}.java`, `launcher.java`, `launcher_{de,ja,zh_CN}.java`, `serialver.java`, `serialver_{de,ja,zh_CN}.java` | compileproperties | 17 of 17 |
| `version.properties` (the intermediate), `version.java` | the script, compileproperties | 2 of 2 |
| `CompilerProperties.java` (415,441 bytes), `LauncherProperties.java` | propertiesparser | 2 of 2 |
| `FlagsEnum.java` | copied from the build tree | (the build's) |

So 20 of 20 generated sources and the intermediate are the build's byte for byte: unlike JN7's
`CaseFolding`, nothing here depends on the running JDK's data (the tools escape non-ASCII
themselves, sort keys with `String`'s order and `TreeMap`, and write US-ASCII or UTF-8 text).
`version.java` depends on the build's version strings, which are the build's configuration
(`spec.gmk`), not the tool's (JFS51).

### JFS48 (b).3 The checks

- `bin/javac-vendor convert` (bootstrap, then generating), then `verify`: `jdk/javac` is the
  conversion and the recording, byte for byte (914 files; generated on the tool, 251 s, traced)
- `bin/javac-vendor check`: j2g translates javac's world, no failures (1,800 files, 5,310 classes
  analyzed; 77 s)
- `bin/javac-vendor classes baseline samples jdk java.base`: javac on Arbace, on the tool whose
  bundles were generated on it, byte for byte the JDK's javac's (baseline 812 classes, samples 28
  and 4, jdk 2,224) and java.base the build's (7,433 classes but `module-info.class`), the
  diagnostics' text equal too (441 s)
- `bin/repr-java check`: as expected
- `bin/jrt-convert verify`: `jdk/` is the conversion, byte for byte (911 forms files, 10 data
  files; the tool world grew, the generators of JN7 on it as before; 272 s)
- `make check-makefile`
- `bin/gate` (essential) at this section's commit: the result is the branch's last commit's
  message and the report's (JFS48 (b).5)
- not run: `bin/gate --full` (the user's rule: at the milestone)

### JFS48 (b).4 The trace: no JVM generates

`bin/javac-vendor verify` under `strace -f --seccomp-bpf -e trace=execve`
(`.tmp/jfs48/trace/verify.strace`), the successful execs counted:

| what | execs |
|---|---:|
| javac's tool executable, `compileproperties.CompileProperties` | 1 |
| javac's tool executable, `propertiesparser.PropertiesParser` | 1 |
| javac's tool executable, repr/java's `read-jdk` (5 modules) and `read` (buildtools) | 6 |
| a JVM (`java`): the frozen jar's `arbace.j2g.main --record-edge` and `arbace.j2g.edge subtract`, each a launcher and the JDK it runs | 4 |
| `javac` | 0 |

The 4 are JN6's and JN7's (javac's edge, JFS14 (b)); the generate step starts no JVM, and no
generated source of javac's comes from a JDK's run any more but `FlagsEnum`'s, which the build
tree's boot JDK made.

### JFS48 (b).5 The shipped program's change

j2g's `names.ae` changed (JFS48 (b).1), so j2g's compiled namespace in the program differs. The
translated runtime cannot: the two prefixes name no class of the shipped world (`jdk/sources.txt`,
`jdk/forms`, `arbace/lang`, `go`, `overlay/jdk` hold none). `jdk/`, `go/`, `overlay/jdk/java.base`
and the edges are unchanged; `jdk/javac/` is the tool's (outside the runtime key). The committed
provenance files that name the tool (`jdk/provenance.edn`, `test/java-fixtures/provenance.edn`)
keep `:host` of the tool they were written on (`7a352b7a`): the verifies compare all but the host
(JFS32), and refreshing it would change the runtime key for no change of content. No re-seed here
(batched); a re-seed would carry the j2g change.

### JFS48 (b).6 What is still copied from the build tree, and why

| what | from | why |
|---|---|---|
| `FlagsEnum.java` | `support/gensrc/jdk.compiler` | `flagsgenerator` runs javac itself on `Flags.java` through `ToolProvider.getSystemJavaCompiler()`, `getStandardFileManager` and `JavacTask.analyze`: javac's standard file manager over the platform (JFS52) |
| the version strings of `version.properties` | `spec.gmk` | the build's configuration (configure's `--with-version-*`, the user and the directory in `VERSION_OPT`), not a source (JFS51) |
| the platform, the java.base batch, the jdk corpus's references | the build tree | as before (JFS15 (b), JFS23 (b), JFS2 (a)) |

### JFS48 (b).7 Proposals

- **JFS51 version.properties' values.**
  - Options: (a) the build's `spec.gmk` (`VERSION_NUMBER`, `VERSION_STRING`, `VERSION_SHORT`),
    `printf` as the build (built); (b) computed from jdk26u's `make/conf/version-numbers.conf` with
    configure's rules (`VERSION_OPT` is `adhoc.USER.DIR` for a build without `--with-version-opt`:
    the build's user and directory); (c) the three values committed beside `jdk/javac/` as an input.
  - *Recommended: (a)* while the build tree is the reference (the platform is pinned from it
    anyway); (c) when the build tree goes (JFS24 (b)), as JFS43 (c) for `CaseFolding`.
- **JFS52 FlagsEnum (JFS48 (c)).**
  - Options: (a) still copied (built); (b) `flagsgenerator` on the tool with a tool-world variant of
    `javax.tools.ToolProvider` returning javac's `JavacTool` and its standard file manager as
    javac on Arbace's (`jdk.internal.jrt.javac`'s, JN3), over `Flags.java` and the platform;
    (c) javac's own `JavacFileManager` over jrt's file systems (JFS5 (a)).
  - *Recommended: (b) next*: one tool of 161 lines, javac already runs on the tool with that file
    manager; (c) is a task of its own.
- **JFS53 The bootstrap.**
  - Options: (a) `JAVAC_VENDOR_BOOTSTRAP=1`, the build tree's files for a tool without the
    generators, the provenance saying so (built); (b) none: the generators' forms committed by hand
    first, a tool built, then the conversion; (c) the generators evaluated by repr/java's tool
    from their Java forms without a build (`arbace.classes.interp`).
  - *Recommended: (a)*: needed once per checkout that lacks the generators, the provenance and
    `verify` keep it honest; (c) when the interpreter runs that much of the JDK.
- **JFS54 How j2g names the tools' packages.**
  - Options: (a) a rule: each tool package's prefix in `tool-module-prefixes` and `pkg-of` (built,
    as JN7's `build/tools/`); (b) `--qualify` per build (`J2G_JAVA_ARGS`), with `pkg-of` placing
    qualified prefixes in package jrt: no j2g change per tool; (c) package jrt for every class of
    javac's world not of `arbace/` (the tool's inputs).
  - *Recommended: (a)* for now: two prefixes, the shipped program untouched; (c) when a third
    group of tools comes, so that adding a tool no longer changes the shipped j2g.

### JFS48 (b).8 Sources and scratch

- jdk26u at `baf63fbe42b8758448fee570a9d9bb914272d259` (`/root/jdk26u`):
  `make/langtools/tools/compileproperties/CompileProperties.java`,
  `make/langtools/tools/propertiesparser/` (7 files and `resources/templates.properties`),
  `make/modules/jdk.compiler/Gensrc.gmk`, `make/common/modules/GensrcProperties.gmk`,
  `make/common/modules/GensrcCommon.gmk`, `make/common/Utils.gmk` (`FilterExcludedTranslations`),
  `make/langtools/tools/flagsgenerator/FlagsGenerator.java` (read), `src/jdk.compiler/share/classes`'
  17 `.properties`, the build tree's `spec.gmk` and `support/gensrc/jdk.compiler` (its
  `_the.COMPILE_PROPERTIES.cmdline` and `_PARSEPROPERTIES_exec.cmdline`: the build's command lines).
- The worktree's `.tmp/jfs48/`: the build and conversion logs (`convert-1.log`, the bootstrap;
  `convert-2.log`), `verify.log`, `check.log`, `classes.log`, `repr-check.log`, `jrt-verify.log`,
  `trace/verify.strace`, `try/` (the first run of propertiesparser on the tool);
  `.tmp/javac-vendor/generated.edn`.

## Decided (2026-10-22): JFS51-JFS54

The user decided JFS51-JFS54 as built:
- JFS51 (a): `version.properties` from the build's `spec.gmk` while the build tree is the reference;
  (c), committed as an input, when it goes.
- JFS52 (a): `FlagsEnum` still copied; (b), flagsgenerator on the tool, a possible next step.
- JFS53 (a): `JAVAC_VENDOR_BOOTSTRAP=1`, `verify` always generating.
- JFS54 (a): a prefix per tool in j2g's names; (c), package jrt for every non-`arbace/` class of
  javac's world, when a third group of tools comes.

## As built: JFS52 (b) (2026-10-22)

JFS52 (b), `FlagsEnum` on the tool, on the branch `java-frontend-flags` from main `818ccb0c` (whose
seed `d7303cf8` holds JFS48 (b)). The last source the JDK build generates for jdk.compiler,
`FlagsEnum` (`make/modules/jdk.compiler/Gensrc.gmk`'s `FLAGSGENERATOR`), is made by the build's
own `flagsgenerator` read into `jdk/javac/forms` and run on javac's tool executable by `bin/javac-vendor
convert`'s generate step, beside JFS48 (b)'s compileproperties and propertiesparser: it is the build
tree's byte for byte, with no difference to explain. No generated source of javac's is copied from
the build tree any more. flagsgenerator compiles `Flags.java` through javax.tools' API, so the tool
world gained what the API asks: `ToolProvider.getSystemJavaCompiler` answers javac on Arbace's
`JavaCompiler`. Nothing of it starts a JVM (JFS52 (b).4). The JVM ran the frozen v4 jar (javac's
edge, as before) and the JDK's `javac` (the reference of `bin/javac-vendor classes`); Go only
TamaGo's go1.27.1, without cgo. The tools' host was the worktree's stage 0 (the seed's, `0fdfb00d`);
j2g for the tool ran on the program built from the checkout (JFS22 (a)).

### JFS52 (b).1 What was built

- **The tool as Java forms.** `bin/javac-vendor` reads `make/langtools/tools/flagsgenerator/FlagsGenerator.java`
  (161 lines) into module `buildtools` (KIND `langtools`, `langtools_dirs`), as JFS48 (b)'s tools.
- **javax.tools on Arbace** (jrt's own Java in `overlay/jdk/javac/jdk.compiler`, JFS5 (b)):
  - `jdk.internal.jrt.javac.SystemCompiler`, a `JavaCompiler`: its tasks are javac's `JavacTool`'s
    (`getTask`, with its standard file manager when given none), its standard file manager the next
    class, its `run` javac on Arbace's command line (`Main`). The platform its file manager
    attributes against is the system property `jdk.internal.jrt.javac.platform` (directories, each
    served as `Main`'s `--platform DIR`): the JDK's standard file manager takes its own image
    (`java.home`), which the tool has not (JFS55).
  - `jdk.internal.jrt.javac.StandardFileManager`, javac on Arbace's `FileManager` as a
    `StandardJavaFileManager`: file objects of files named by `File` or by path (their kind by their
    extension), `setLocation`/`getLocation` for the source path, the class path (default the current
    directory) and the class output. `FileManager` is no longer final; its source path, class path and
    class output are settable by it. javac's own `JavacFileManager` stays unused (JFS52 (c)).
- **A tool-world variant** (`overlay/jdk/javac/variants/ToolProvider.ae`, JFS18 (b)):
  `getSystemJavaCompiler` returns a `SystemCompiler`. The JDK's looks javac up in the boot layer's
  jdk.compiler module by reflection; jrt has no module layer. Nothing else of the tool world was
  needed: `Trees.instance`'s reflective lookup of `JavacTrees.instance` works on jrt as is (javac's
  packages are rooted whole, JN4.2), and `Files.newBufferedWriter`, `EnumMap`, the text blocks and the
  unnamed lambda parameters (`_ ->`) translate as they are.
- **j2g** (shipped code, JFS52 (b).5): `flagsgenerator/` joins `tool-module-prefixes` and `pkg-of`'s
  package jrt (`arbace/j2g/names.ae`), by JFS54 (a)'s rule. flagsgenerator is the third tool of
  make/langtools/tools (`make/ToolsLangtools.gmk` builds the three as one, `INCLUDES :=
  compileproperties flagsgenerator propertiesparser`), so it completes JFS48 (b)'s group rather than
  starting a third; JFS54 (c) waits for a third group (JFS56).
- **The generate step** runs, after propertiesparser, `flagsgenerator.FlagsGenerator Flags.java
  FlagsEnum.java` (Gensrc.gmk's command line) on the tool, the platform made in `WORK/platform`
  and checked against `jdk/javac/platform.sha256` first and named by the script (`gen.ae` sets the
  property): the JDK build ran it on its boot JDK 25 against that JDK's own image; the output depends
  only on `Flags.java`'s annotations and constants. `gensrc_build` is empty, so KIND `gensrc-build`
  no longer occurs in `sources.txt`, and the provenance's `:generated` names three tools and copies
  none (`:copied []`). `JAVAC_VENDOR_BOOTSTRAP=1` copies `FlagsEnum` too.
- **The provenance's host** (JFS57 (b)): `convert` names the tool by its key before it writes, not
  after. The bootstrap conversion below wrote `:host ""`: `repr_java_host` compares the tool's key
  with the checkout's, which holds `jdk/javac`, already rewritten. A stale tool now stops `convert`
  at its start (`REPR_JAVA_STALE_OK=1` still warns instead).
- **The bootstrap** (JFS53 (a)): the tool built from main's tree (key `589683c9`; the variant's
  `SystemCompiler` not yet in its world, so `ToolProvider` failed its analysis there, the only
  failure, harmless to the conversion) converted the tree with `JAVAC_VENDOR_BOOTSTRAP=1`; the tool
  built from that tree (key `7c805cbb`, no analysis failure) generated the 21 files equal to the build
  tree's, and its conversion wrote the same forms and edge: the checkout's key after it was the
  tool's own (`repr_java_host` accepted it), so the tree is a fixpoint of the tool, as JFS48 (b)'s.

| | JFS48 (b) | JFS52 (b) |
|---|---:|---:|
| `jdk/javac/sources.txt` | 821 | 824: jdk.compiler 372 (21 generated here, none copied; the 2 overlay classes), java.compiler 135, jdk.internal.opt 1, java.base 32, java.xml 221, buildtools 63 |
| `jdk/javac/forms` | 910 files, 9,349,032 bytes | 914 files, 9,364,978 bytes (flagsgenerator's 2 and the overlay's 2, 15,910 bytes) |
| `jdk/javac/edge.edn` | 223 classes, 419,123 bytes | 223 classes, 419,285 bytes: `:exists` gained 4 classes, not by this change (JFS58) |
| the tool's j2g: files, classes analyzed, failing; methods reached, with missing parts | 2,306, 19,175, 0; 97,575, 617 | 2,310, 19,209, 0; 97,708, 617 |
| the tool (amd64, `arbace-javac`) | 210,055,328 bytes | 211,210,400 bytes (main's tree in this worktree, after GP7: 211,108,000) |
| a tool build (amd64, the reader compiled first) | 680 s | 431-449 s |
| the generate step | 1 s | 6 s (the platform made and checked 5 s; flagsgenerator 0.3 s) |
| `bin/javac-vendor convert` | 224-251 s | 125 s (the edge's recording 34 s); 226 s with the frozen jar built first |

### JFS52 (b).2 The output (the exit criterion)

| output | by | equal to the build tree's |
|---|---|---:|
| `FlagsEnum.java` (82 lines, 3,328 bytes) | flagsgenerator on javac on Arbace | yes, byte for byte |
| JFS48 (b)'s 20 and `version.properties` | compileproperties, propertiesparser, the script | 21 of 21, as before |

`WORK/generated.edn`: 22 of 22 `:same`. As JFS48 (b)'s, nothing here depends on the running JDK's
data: the flags' names and bits are `Flags.java`'s constants and annotations, attributed by javac on
Arbace (jdk26u's javac) where the build attributed them with its boot JDK 25's javac, and the two
agree. Unlike the build's run, which compiled `Flags.java` in the unnamed module against an image
whose jdk.compiler holds the same package, javac on Arbace sees the build tree's platform; the errors
either reports (the package in another module, `Assert` not visible) go to flagsgenerator's diagnostic
listener, which drops them, and attribution of the annotations and constants does not need them.

### JFS52 (b).3 The checks

- `bin/javac-vendor convert` (bootstrap, then generating), then `verify`: `jdk/javac` is the
  conversion and the recording, byte for byte (918 files; generated on the tool, 175 s, traced),
  and again after JFS57 (b)'s change
- `bin/javac-vendor check`: j2g translates javac's world, no failures (1,803 files, 5,314 classes
  analyzed; 50 s)
- `bin/javac-vendor classes baseline samples jdk java.base`: javac on Arbace byte for byte the JDK's
  javac's (baseline 812 classes, samples 28 and 4, jdk 2,224) and java.base the build's (7,433
  classes but `module-info.class`), the diagnostics' text equal too (223 s)
- `bin/repr-java check`: as expected
- `bin/jrt-convert verify`: `jdk/` is the conversion, byte for byte (911 forms files, 10 data
  files; the tool world grew, JN7's generators on it as before; 232 s)
- `make check-makefile`
- `bin/gate` (essential) at this section's commit: the result is the branch's last commit's
  message and the report's
- not run: `bin/gate --full` (the user's rule: at the milestone)

### JFS52 (b).4 The trace: no JVM generates

`bin/javac-vendor verify` under `strace -f --seccomp-bpf -e trace=execve`
(`.tmp/jfs52/trace/verify.strace`), the successful execs counted:

| what | execs |
|---|---:|
| javac's tool executable, `compileproperties.CompileProperties` | 1 |
| javac's tool executable, `propertiesparser.PropertiesParser` | 1 |
| javac's tool executable, `flagsgenerator.FlagsGenerator` | 1 |
| javac's tool executable, repr/java's `read-jdk` (5 modules) and `read` (buildtools) | 6 |
| a JVM (`java`): the frozen jar's `arbace.j2g.main --record-edge` and `arbace.j2g.edge subtract`, each a launcher and the JDK it runs | 4 |
| `javac` | 0 |

The 4 are javac's edge (JFS14 (b)), as in JN6, JN7 and JFS48 (b). No generated source of javac's
comes from a JDK's run any more.

### JFS52 (b).5 The shipped program's change

j2g's `names.ae` changed (JFS52 (b).1), so j2g's compiled namespace in the program differs; the
translated runtime cannot: `flagsgenerator/` names no class of the shipped world. `jdk/`, `go/`,
`overlay/jdk/java.base` and `jdk/edge.edn` are unchanged; `jdk/javac/` and `overlay/jdk/javac/` are
the tool's (outside the runtime key). As in JFS48 (b), the committed provenance files that name the
tool (`jdk/provenance.edn`, `test/java-fixtures/provenance.edn`) keep their host. No re-seed here;
a re-seed would carry the j2g change.

### JFS52 (b).6 What is still taken from the build tree, and why

| what | from | why |
|---|---|---|
| no generated source | | (all 21 and `version.properties` generated on the tool) |
| the build tree's files as the reference the generated ones are compared with | `support/gensrc/jdk.compiler` | the check, not an input; and the bootstrap's input (JFS53 (a)) |
| the version strings of `version.properties` | `spec.gmk` | the build's configuration (JFS51 (a)) |
| the platform (now also flagsgenerator's), the java.base batch, the jdk corpus's references | the build tree | as before (JFS15 (b), JFS23 (b), JFS2 (a)) |

### JFS52 (b).7 Proposals

- **JFS55 Where javax.tools' system compiler finds its platform.**
  - Options: (a) the system property `jdk.internal.jrt.javac.platform`, set by the caller (built:
    the generate step's script); (b) a default when it is unset: an environment variable, or a
    directory beside the tool (as the JDK's `java.home`); (c) the platform's module descriptors and
    classes embedded in the tool's resources, its image.
  - *Recommended: (a)* while the one caller is `bin/javac-vendor`, which already makes and pins the
    platform; (c) with JFS24 (b), when the build tree goes and the platform must travel with the tool.
- **JFS56 How j2g names the tools' packages (JFS54 again).**
  - Options: (a) a prefix per tool package (built: `flagsgenerator/`, the third of
    make/langtools/tools, JFS48 (b)'s group); (b) `--qualify` per build with `pkg-of` placing
    qualified prefixes in package jrt; (c) package jrt and qualified names for every class j2g reads
    from the tool's inputs not of `arbace/` (a j2g option marking `jdk/javac/forms` as the tool
    world), so that a new tool never changes the shipped j2g.
  - *Recommended: (a)* now (the decided rule; flagsgenerator is no new group); (c) at the next
    group of tools (e.g. the JDK build's `make/jdk/src/classes` tools beyond JN7's, or the
    jlink/jmod tools), when JFS54 (c) applies.
- **JFS57 The provenance's host on a conversion that changes the tree.**
  - Options: (a) as before: the host asked after writing, empty when the tree changed (the next
    conversion, on a tool built from the new tree, writes it); (b) the host asked before writing, a
    stale tool refused at the start (built); (c) the host read from the tool's key file without the
    freshness check.
  - *Recommended: (b)*: the provenance names the tool that converted; (a) left a committed-looking
    `:host ""` after every bootstrap. `bin/jrt-convert` is not affected: it writes its provenance
    into its work directory before `update` replaces `jdk/forms`.
- **JFS58 javac's edge and the shipped edge after GP7.** javac's edge gained an `:exists` set of 4
  classes (`java.util.concurrent.ExecutionException`, `FutureTask`, `Semaphore`,
  `java.util.concurrent.atomic.LongAdder`): GP7's `arbace/repr/go/annotate.ae` names them, the
  recording asks the JVM whether the embedded sources' names exist, and `jdk/edge.edn`'s `:exists`,
  recorded before GP7, lacks them, so the subtraction leaves them in javac's edge. On main,
  `bin/javac-vendor verify` would have failed on this alone, and `bin/j2g-edge verify` (`--full`)
  likely fails on `jdk/edge.edn`.
  - Options: (a) `bin/j2g-edge update` on main (the shipped edge with the 4), then `bin/javac-vendor
    convert` (javac's edge without them again); (b) leave `jdk/edge.edn`, javac's edge carrying the
    4 (as committed here); (c) the essential gate checks the edges' `:exists` against the embedded
    sources' names on the tools' host, cheaply, so the drift shows when it happens.
  - *Recommended: (a)* before the next re-seed (it changes j2g's data for the shipped program, not
    this branch's business), with (c) if it recurs.

### JFS52 (b).8 Sources and scratch

- jdk26u at `baf63fbe42b8758448fee570a9d9bb914272d259` (`/root/jdk26u`):
  `make/langtools/tools/flagsgenerator/FlagsGenerator.java`, `make/modules/jdk.compiler/Gensrc.gmk`
  (`FLAGSGENERATOR`), `make/ToolsLangtools.gmk`, `src/jdk.compiler/share/classes/com/sun/tools/javac/code/Flags.java`,
  `src/java.compiler/share/classes/javax/tools/{ToolProvider,JavaCompiler,StandardJavaFileManager}.java`,
  `src/jdk.compiler/share/classes/com/sun/tools/javac/api/{JavacTool,ClientCodeWrapper}.java`,
  `com/sun/tools/javac/main/Arguments.java`, `com/sun/tools/javac/code/ClassFinder.java` (where javac
  treats a `StandardJavaFileManager` apart), the build tree's
  `support/gensrc/jdk.compiler/com/sun/tools/javac/code/{FlagsEnum.java,_FLAGSGENERATOR_exec.cmdline}`.
- The worktree's `.tmp/jfs52/`: `build-program.log`, `build-tool-1.log` (main's tree),
  `build-tool-2.log` (the bootstrap's tree), `convert-1.log` (bootstrap), `convert-2.log`,
  `verify.log`, `verify-2.log`, `check.log`, `classes.log`, `repr-check.log`, `jrt-verify.log`,
  `trace/verify.strace`, `try/` (flagsgenerator's first run on the tool, 0.3 s, equal);
  `.tmp/javac-vendor/generated.edn`.

## Decided (2026-10-22): JFS55-JFS58

The user decided JFS55-JFS57 as built and JFS58 (a) to be done:
- JFS55 (a): the system compiler's platform from the property its caller sets; (c), embedded in
  the tool, when the build tree goes.
- JFS56 (a): a prefix per tool package in j2g's names; (c) at the next group of tools.
- JFS57 (b): `convert` reads the tool's key before it writes.
- JFS58 (a), done on main after the merge: `bin/j2g-edge verify` failed at `5b3fa825` (`jdk/edge.edn`'s
  `:exists` lacked `ExecutionException`, `FutureTask`, `Semaphore`, `LongAdder`, asked since GP7);
  `bin/j2g-edge update`, then `bin/javac-vendor build` and `convert` (javac's edge loses its
  `:exists`), then `build`, `verify` (918 files byte for byte) and `bin/j2g-edge verify`: pass.

## As built: JN8 (2026-10-23)

JN8, the native parser (JF8 of §7: option (a)'s first half, as JFS10 put it, parser first), on the
branch `java-frontend-jn8` from main `a138539a` (whose seed `3ab3b603` holds JFS52 (b)), in about
four and a half hours of work (against the 4-6 days estimated). `arbace.repr.java.parse`, with its lexer
`arbace.repr.java.lex`, is Java 26's lexer and parser in Arbace, a port of jdk26u's javac parser
method by method, and gives javac's trees as data: the tags, javac's `pos`, `TreeInfo.getStartPos`
and `getEndPos`, the end positions the parser stores, the doc comments it attaches and every field
of every tree. `bin/repr-java parse-check`, the tree level, parses every file of its corpora with
javac on Arbace (javac's tool executable, `JavacTask.parse`) and natively and compares the two
trees: **every file of every corpus has javac's tree**, positions included: the samples, the
baseline, the jdk corpus, javac's own sources and jdk26u's 68 modules (14,668 files), 8,000 mutants
(javac's error recovery) and jdk26u's 37,797 test files; the known differences
(`test/repr/java/parse-check-known.edn`) are none. It joins `bin/gate --full`'s java chain (JFS60
(a)). The JVM ran only the frozen v4 jar, for javac's edge in `bin/javac-vendor convert`
(JN8.7); Go only TamaGo's go1.27.1, without cgo. The tools' host was the worktree's stage 0 (the
seed's); j2g for the tool ran on it (the worktree held no program built from the checkout, JFS22).

### JN8.1 The parser: `arbace.repr.java.lex` and `arbace.repr.java.parse`

| file | lines | ports (jdk26u `src/jdk.compiler/share/classes/com/sun/tools/javac/`) |
|---|---:|---|
| `arbace/repr/java/lex.ae` | 533 | `parser/UnicodeReader.java` (the escapes, decoded once into the characters and a map to the text's offsets), `JavaTokenizer.java` (`readToken` and its scanners, by code point), `JavadocTokenizer.java` and `BasicComment` (the comments, `isDeprecated`'s scan of a doc comment), `Tokens.java` (the kinds, `docComment`, `deprecatedFlag`, `split`) |
| `arbace/repr/java/parse.ae` | 2,881 | `parser/JavacParser.java` (every method `parseCompilationUnit` reaches, error recovery and `VirtualParser`'s speculation included), its `SimpleEndPosTable` and `MinimalEndPosTable`, `tree/TreeMaker.java` (its factories and their `pos`), `tree/TreeInfo.java` (`getStartPos`, `getEndPos` and the helpers the parser calls) |
| `arbace/repr/java/jtree.ae` | 354 | (javac's side) javac's trees as the same data; the check; the mutants |

§4 (a) estimated the parser at 3,000 to 5,000 lines of Arbace: the lexer and the parser are 3,414.
Both carry the notice of the files they port (LICENSE.md's jdk26u list, doc/ARBACE.md).

The parser runs as javac's `JavacTask.parse` runs it: doc comments and end positions kept, source
26 with preview enabled (`read-jdk`'s options, JFS64). Its state is javac's: the tokens (lexed whole
first: javac's lexing depends on nothing the parser does but `split`, which replaces the current
token in place, undone after a speculation), the modes, `errorEndPos`, TreeMaker's `pos` (a
factory called without `at` takes the last one), `prevToken`'s end, `allowThisIdent`, the
pushed-back type annotations and the receiver parameter. Trees are mutable maps while parsing,
since javac changes its trees after making them (`mods.flags |=`, `mods.pos`, a method
reference's `expr` and `pos`, a new array's annotations, a lambda parameter's `vartype`, `bracePos`,
the type into which `insertAnnotationsToMostInner` moves annotations), and become persistent data at
the end (`freeze`): a map per node with `:tag` (javac's `JCTree.Tag`), `:pos`, `:endpos` (stored, -1
when none), `:start`, `:end`, `:doc` ([style pos end] of its doc comment) and its fields under
javac's names (names as strings, flags as longs, enums as keywords, a literal's value as
`literal-value` gives it: floats by their bits). `parse/dump` prints a tree one node per line: the
form the check compares.

javac's behaviour the port reproduces where it shapes trees, the first five found by the check (JN8.4):
- the lexer's `tk` survives from one token to the next, and a character literal's error recovery
  reads it: after `'''` the next character literal swallows the text to the next quote;
- a non-ASCII digit is replaced by its ASCII digit in a number's text, a surrogate pair's code point
  is kept (`UnicodeReader.digit`, `put`); the reader advances by code point;
- an unclosed text block resets to its first line end; a character literal of a surrogate pair is
  an error token;
- `syntaxError(pos, null, ...)`'s erroneous tree has no list (`errs` nil), the others an empty one;
- a speculation (`VirtualParser`) has a `MinimalEndPosTable` of its own: it stores no end position
  but declarations', not even into the trees it shares with the real parse (the modifiers, whose
  `pos` it does set, so that the real parse stores none either);
- `F.at(x).Foo(args)`: TreeMaker's `pos` is set before the arguments are evaluated and read after,
  so a factory call among the arguments moves it (`AnnotatedType(annos, basicType())` takes the
  primitive's position).

### JN8.2 javac's trees as data: `arbace.repr.java.jtree`

On javac's tool executable, `arbace.repr.java.javac/parse-only` runs `JavacTask.parse` with javac on
Arbace's file manager, as `analyze` does but without attribution, and `jtree/unit-data` walks each
`JCCompilationUnit`: per tag javac's fields (JCTree's public fields; a module's kind by
`getModuleType`), `pos`, the unit's `EndPosTable`, `TreeInfo.getStartPos` and `getEndPos`, and the
unit's `DocCommentTable` (a comment's style, `getPos()`'s start and end). The native parser parses
the text javac read (`getCharContent`), so both see the same characters.

### JN8.3 The check: `bin/repr-java parse-check`

`bin/repr-java parse-check [--corpus C,...]` (`bin/lib/repr-java/parse-check`; Makefile
`repr-java-parse-check`, in `make verify`) runs `jtree check` per corpus on javac's tool executable:
the files in chunks of 50, each chunk parsed by javac on Arbace in one task, then natively file by
file, on `REPR_JAVA_THREADS` threads (12); a file is equal when both dumps are, and the differing
files must be `test/repr/java/parse-check-known.edn`'s (`{}`). Each corpus writes `report.edn`
(the counts, the nodes, the characters, both parse times), `files.tsv` (a line per file, the first
differing line), `javac-errors.txt` (javac's syntax errors: a file with errors is compared like any
other) and both dumps of each differing file under `diff/`. Also `bin/repr-java parse [--javac]
FILE...` (a file's tree: native, on the tools' host; or javac's, on the tool) and `parse [--javac]
--time PATH...` (the speed, JN8.5).

| corpus | what | files | equal | javac's syntax errors | wall |
|---|---|---:|---:|---:|---:|
| `samples` | `test/repr/java/java`, `module` | 14 | 14 | 0 | 0.1 s |
| `baseline` | the frozen `clojure/` | 183 | 183 | 0 | 2.8 s |
| `jdk` | java.base's closure, `jdk/forms`' originals (`bin/jrt-convert`'s sources and generate steps, made again when `jdk/sources.txt` changes) | 838 | 838 | 0 | 5.5 s |
| `javac` | javac's own sources, `jdk/javac/sources.txt` (the generated ones from the build tree's `support/gensrc`, which `bin/javac-vendor` generates byte for byte) | 824 | 824 | 0 | 7.7 s |
| `jdk26u` | every Java file of jdk26u's 68 modules (`src/*/share/classes`: the 12,730 that `check --jdk` reads, the 68 `module-info.java`, 11 snippet files) | 12,809 | 12,809 | 0 | 37 s |
| `mutants` | javac's error recovery: 4,000 files of the 68 modules (a shuffle, seed 26), each made into 2 mutants of 4 edits at random tokens (deleted, duplicated, swapped with the next, a token inserted before; `jtree mutate`) | 8,000 | 8,000 | 66,598 (7,979 files) | 24 s |
| `tests` | every Java file of jdk26u's `test/`, langtools' error tests among them | 37,797 | 37,797 | 553 (213 files) | 80 s |

The nodes compared: 4,460 (samples), 425,618 (baseline), 1,733,429 (jdk), 1,540,934 (javac),
16,408,129 (jdk26u), 10,254,529 (mutants), 31,538,285 (tests). The wall times are the check's alone, on the final tool,
12 threads, the host shared: the whole command took 199 s (117 s for the first six corpora, the
mutants' generation included, and 82 s for the tests), the jdk corpus's originals about a minute more
when the closure changes. Beyond the corpora,
scratch mutants of the worktree's `.tmp/jn8/` (Python, `scripts/mut.py`): 6,000 of one edit, 10,000
of 4 and 20,417 of 2 to 8 edits, over the modules and the tests: all equal (two of the 10,000 found
the speculation's end positions, JN8.4).

### JN8.4 Bugs found

The first run over the samples, the baseline, java.base and then jdk26u's 68 modules had every file
equal; what differed was error recovery, found by langtools' tests (5 of 7,227 files: the lexer's
`tk`, the non-ASCII and surrogate digits, the text block's reset, `errs` nil), the rest of `test/`
(1 of 37,797: the surrogate pair in a character literal) and the scratch mutants (2 of 10,000: the
speculation's end positions): the six behaviours of JN8.1, each fixed in the port. No regression
form: `parse-check` is the parser's test, its `tests` corpus and the mutants the error recovery's.
One more cost found: `pp/pprint` of the report took about 6.5 minutes when it held the mutants'
66,598 diagnostics; they go to `javac-errors.txt` now, the report keeps their count.

### JN8.5 Speed

One thread, the parse alone (the texts read before; `bin/repr-java parse --time`, `parse --javac
--time`), on the shared host (other work running):

| | baseline (183 files, 2.20 M chars) | `java/util` (370 files, 9.62 M chars) | java.base (3,097 files, 46.0 M chars) |
|---|---:|---:|---:|
| javac on Arbace (`JavacTask.parse`, javac's tool executable) | 0.41 s (5.4 M chars/s) | | 5.6 s (8.2 M chars/s) |
| the native parser compiled into the tool (c2j) | 4.0 s (0.55 M chars/s) | 10.9 s (0.88 M chars/s) | 50.5 s (0.91 M chars/s) |
| the native parser evaluated (the tools' host, stage 0) | 21.9 s (0.10 M chars/s) | 62.0 s (0.16 M chars/s) | |

So the native parser compiled takes about 9 times javac on Arbace's time, and evaluated about 5 times
its compiled time. Its compiled time over `java/util` splits into the lexer 3.9 s (2.5 M chars/s),
the parse 5.6 s and `freeze` 3.6 s (before `freeze` iterated its maps' entries directly: now about
1.4 s less); a CPU profile (`ARBACE_CPUPROFILE`) puts 17% of it in j2g's checked casts of arrays
(`J2g_CastArray` through `Class.IsInstance`: the parser's state and the lexer's characters are
arrays, each hinted use a checked cast) and much of the rest in allocation (a `HashMap` per node,
boxed positions) (JFS65).

### JN8.6 Where the parser runs (JFS59)

Built: in both places the reader's namespaces already run. On the tools' host it is evaluated from
the checkout (`bin/repr-java parse`, like repr/haskell's and repr/rust's namespaces: not in the
shipped program, no re-seed); in javac's tool it is compiled with the reader (`--build-java` embeds
`arbace/repr/java` whole: 15 sources, 1,925 classes, the tool 215,048,352 bytes against the first
pass's 201,785,504 and JFS52 (b)'s 211,210,400), where the check runs it beside javac. The parser
needs nothing of javac's world, so it could run compiled anywhere the program does (JFS59 (b)).

### JN8.7 The shipped program's change

None in code: `arbace/repr/java` is not in the shipped program (j2g's `embed.ae` leaves it out; the
tool embeds it), and `go/`, `overlay/`, `jdk/forms`, `jdk/data` and `jdk/edge.edn` are unchanged.
The provenance files' `:repr-java` (`bin/lib/repr-java/host.bash`'s `repr_java_reader_hash`: the
reader's sources and `bin/repr-java`, which gained `parse`, `parse-check` and `javac.ae`'s
`parse-only`) moved from `295a3d0a...` to `2abe20bf...`, written by the updates on the final tool
(key `64d7081d...`): `bin/jrt-convert update`, `bin/clojure-tests --java-fixtures update` and
`bin/javac-vendor convert` (its edge on the frozen v4 jar), every form file byte for byte as
committed, only `:repr-java` and `:host` changed in `jdk/provenance.edn`,
`test/java-fixtures/provenance.edn` and `jdk/javac/provenance.edn`. `jdk/provenance.edn` is in the
runtime key (`bin/lib/version.bash`), so the version map the executable carries changes, as at JN6.
No re-seed (batched).

### JN8.8 The checks

- `bin/repr-java parse-check` (the default corpora) and `--corpus tests`, on the final tool (key
  `64d7081d...`): every file equal, as JN8.3's table
- `bin/repr-java check`: as expected (the baseline's 189 files as `baseline.sha256`, the samples' 11
  and the module's 6 as `test/repr/java/expected`; 25 s)
- the updates of JN8.7: `bin/clojure-tests --java-fixtures update` (15 forms files, 20 s), `bin/jrt-convert
  update` (911 forms files, 10 data files, 216 s), `bin/javac-vendor convert` (914 forms files, the
  edge on the frozen jar: 223 classes beyond `jdk/edge.edn`'s, 129 s): only the three provenance
  files' `:repr-java` and `:host` changed (the converter's outputs unchanged)
- `make check-makefile`: passes
- `bin/gate` (essential): on the branch at `fdfb0fd5` (this section's commit but this list), passes
  in 16m41s (go-build 406 s, afresh; go-smoke, suite-go 777 tests with no regression, oracle-go 53
  known mismatches as recorded, diffcheck 12,743 of 12,766 cases agree with the 23 known, goi-check
  797 of 797, generated)
- not run: `bin/gate --full` (the user's rule: at the milestone), `bin/repr-java roundtrip`, `check
  --suite`, `bin/javac-vendor verify` and `classes` (the converter and javac's world are unchanged:
  `javac.ae` only gained `parse-only`, which `read` does not call, and `bin/repr-java` only its two
  verbs; the updates of JN8.7 show the forms unchanged)

### JN8.9 What it means for JN9

The parser gives what javac's parser gives, so the syntax JN9's attribution would start from is
javac's, positions included, without javac. What it does not give is what §2 measured: 77% of
`convert.ae`'s code reads javac's attribution, not its trees. The trees as data have javac's shape
and names (the converter's tree walking, `(.-defs t)` against `(:defs t)`, is a mechanical port), so
JFS7 (b), the converter over an interface, can be tried on these trees for the syntax-only facts
first (JFS66).

### JN8.10 Proposals

- **JFS59 Where the parser runs.**
  - Options: (a) evaluated from the checkout on the tools' host and compiled into javac's tool
    (built: the tool embeds `arbace/repr/java` whole, JFS28); (b) also embedded in the shipped
    program and compiled (j2g's `embed.ae`, as goi's namespaces), so that the tools' host and the
    box parse Java at the compiled speed without javac's world: the program grows and is re-seeded;
    (c) javac's tool only.
  - *Recommended: (a)*; (b) when a consumer outside the tool needs the speed (JN9's attribution on
    the tools' host, or Java read in the box), batched with that re-seed.
- **JFS60 The tree level in the gates.**
  - Options: (a) `bin/repr-java parse-check` (the default corpora) in `bin/gate --full`'s java chain,
    after `java-fixtures`, on the tool the javac step built (built: `repr-java-parse`, about 3.5
    minutes); (b) out of every gate, a command and the Makefile's target, as `check --jdk` (JFS42);
    (c) in `--full` the four small corpora only, jdk26u, the mutants and the tests a command.
  - *Recommended: (a)*: a few minutes beside the chain's 10 to 20, and the parser's only check.
- **JFS61 jdk26u's tests as a corpus.**
  - Options: (a) opt-in (`--corpus tests`); (b) in the default, so in `--full` (built: 37,797 files,
    80 s; jdk26u's test tree is read where it is, nothing copied).
  - *Recommended: (b)*: langtools' tests are javac's own error cases (213 files with 553 syntax
    errors javac reports), cheap beside the mutants, and found one of the six behaviours alone.
- **JFS62 The mutants.**
  - Options: (a) one fixed set, 8,000 by seed 26 (built: reproducible from the commit); (b) a new
    seed per gate run (its date), more of javac's recovery over time but a failure not reproducible
    from the commit alone (the seed in the log would reproduce it); (c) more of them (every file of
    the 68 modules, 25,618 mutants, about three times the time).
  - *Recommended: (a)*, the seed changed by hand when the parser changes (a commit's known file and
    seed together).
- **JFS63 Doc comments' text.**
  - Options: (a) a doc comment's style and positions, `@deprecated` in the modifiers' flags as javac
    folds it (built); (b) also the text `JavadocTokenizer` extracts (`getText`, its offset map), for
    a consumer of Javadoc.
  - *Recommended: (a)*: the converter keeps comments only in mode `:full`, by the text's positions.
- **JFS64 Source levels.**
  - Options: (a) Java 26 with preview, fixed (built: javac's source-dependent branches as at 26,
    `-source 26 --enable-preview`, the options `read-jdk` reads every corpus with); (b) a source
    level argument following javac's `Feature.allowedInSource` (`yield`, `record`, `sealed`, `_`,
    `var` and the warnings), checked against javac at each level.
  - *Recommended: (a)* until a corpus must be read at another level.
- **JFS65 The parser's speed.**
  - Options: (a) as built, about 9 times javac on Arbace compiled (JN8.5); (b) j2g's checked array
    casts as a type switch, not `Class.IsInstance` (17% of the parser's time, and of all compiled
    code's that uses arrays: a j2g change to the shipped program, measured on `bin/j2g-perf`,
    re-seeded); (c) the parser's own: nodes as records rather than `HashMap`s, the arrays bound once
    per function.
  - *Recommended: (a)* for JN8; (b) as a j2g task of its own, since it speeds every compiled
    program; (c) with JN9 if the parser's cost then matters next to attribution's.
- **JFS66 After JN8.**
  - Options: (a) JN9 as §7 plans it (the attribution, 25-45 days, an open tail: §8 R6); (b) a
    bounded step first: JFS7 (b)'s interface tried on the native trees for the converter's syntax-only
    facts (§2.1's forms that read no attribution, and the trees' shape everywhere), javac on Arbace
    still answering the attribution by position (§4 (d4) as a check), measuring how the converter
    splits before JN9 is decided; (c) stop after JN8: the native parser serves tools that need Java's
    syntax (an editor's view, Java in the box without javac's world), javac on Arbace stays the
    front end.
  - *Recommended: (b)*: a few days, and the numbers JN9's decision needs; (c) if no consumer of a
    JVM-free, javac-free attribution is in sight.

### JN8.11 Sources and scratch

- jdk26u at `baf63fbe42b8758448fee570a9d9bb914272d259` (`/root/jdk26u`):
  `src/jdk.compiler/share/classes/com/sun/tools/javac/parser/{JavacParser,JavaTokenizer,JavadocTokenizer,UnicodeReader,Tokens,Scanner,VirtualParser,TextBlockSupport,LazyDocCommentTable}.java`,
  `tree/{JCTree,TreeInfo,TreeMaker,EndPosTable}.java`, `code/Flags.java`, `util/Convert.java`,
  `main/JavaCompiler.java` and `api/JavacTaskImpl.java` (what `parse` keeps); `src/*/share/classes`
  (the jdk26u corpus), `test/` (the tests corpus).
- The worktree's `.tmp/`: `jn8/` (`ref/JavacParser.nc`, the parser without comments, read for the
  port; `exp/` the evaluator's micro-benchmarks; `scripts/mut.py`, `prof.ae`, `prof2.ae`,
  `final-checks.sh`; the mutants `mut*/` and their checks `pc/`; `parse.pprof`, `mut6.pprof`; the
  tool builds' logs `tool-build-{1,2,3}.log`; `devtool/`, a copy of main's tool for the first,
  evaluated, runs), `repr-java-parse-check/` (the corpora's reports, the jdk originals, javac's
  sources, the mutants), `repr-java-check/`, `jrt/`, `javac-vendor/`, `clojure-tests/`.

## Decided (2026-10-23): JFS59-JFS66

The user decided JFS59-JFS66 as built:
- JFS59 (a): the parser evaluated on the tools' host and compiled in javac's tool; (b), embedded
  in the shipped program, when a consumer outside the tool needs its speed.
- JFS60 (a): `bin/repr-java parse-check` in `bin/gate --full`'s java chain.
- JFS61 (b): jdk26u's tests in the default corpora.
- JFS62 (a): one fixed set of mutants (seed 26), the seed changed by hand with the parser.
- JFS63 (a): doc comments' style and positions, not their text.
- JFS64 (a): Java 26 with preview, fixed.
- JFS65 (a) for JN8; (b), j2g's checked array casts as a type switch, a j2g task of its own.
- JFS66 (b): next, the converter's interface (JFS7 (b)) tried on the native trees for its
  syntax-only facts, javac on Arbace answering the attribution by position, measured before JN9.

## As built: JFS66 (b) (2026-10-23)

JFS66 (b), the converter on an interface over both front ends (JFS7 (b)) tried on the native trees,
javac on Arbace answering the attribution by position (§4 (d4) as a check), and the split measured,
on the branch `java-frontend-jfs66` from main `348db2fa` (whose seed `3ab3b603` holds JFS52 (b)), in
about a working day. `convert.ae` reads the tree only through `arbace.repr.java.tree` and asks
attribution only through `arbace.repr.java.attr`; `bin/repr-java read --front native` converts the
native parser's trees (JN8) with javac's attribution answered by position, and **every form of every
corpus is the one javac's trees give, byte for byte**: the samples, the baseline, the jdk corpus,
javac's sources and jdk26u's 68 modules (12,730 files), but three files of jdk26u whose one pinned
call each differs for a reason that is not the trees' (the converter's resolution depends on the
history of the javac run, found here: JFS66 (b).5, JFS70). The current converter's outputs are
unchanged (the three updates byte for byte). The measurement (JFS66 (b).4) gives JN9's numbers:
78.6% of `convert.ae`'s code consumes attribution (§2.1 measured 77% by a text scan), only literals
and parentheses among expressions ask nothing of it, and the output's lists, by the most demanding
fact their writing asked: 2.3% need no more than syntax and local scopes, 15.0% no more than the
declarations (javac's Enter, TypeEnter, MemberEnter), 68.7% no more than expression types, 98.3% no
more than overload resolution; inference (JLS 18) is needed by 1.7% (jdk26u's 2,258,047 lists). The
JVM ran only the frozen v4 jar, for javac's edge in `bin/javac-vendor convert`; Go only TamaGo's
go1.27.1, without cgo; the tools' host was the worktree's stage 0 (the seed's).

### JFS66 (b).1 The interface: `arbace.repr.java.tree` and `arbace.repr.java.attr`

| file | lines | what |
|---|---:|---|
| `arbace/repr/java/tree.ae` | 316 | the syntax over either representation: `tag` (javac's `JCTree.Tag` as a keyword), `is?`, the tag classes of javac's hierarchy (`statement?`, `binary?`, `unary?`, `assignop?`, `pattern?`), `f` (a field by javac's name: a macro generates, from the tag table and `arbace.repr.java.parse`'s field lists, one case per field over javac's classes that have it, a map lookup for the native trees), the methods javac's tree classes give (`var-decl?`, `rule-case?`, `open-module?`, `class-kind`, `literal-value`), positions (`start`, `end`, `pos`), TreeInfo's helpers the converter used (`skip-parens`, `tree-name`, `full-name`, `diamond?`), `children` (javac's by reflection, as the converter always walked them; the native trees' in the same field order, taken from javac's classes by reflection) |
| `arbace/repr/java/attr.ae` | 349 | every fact taken from attribution, asked of a tree of either representation: `sym`, `symbol-of` (TreeInfo.symbol), `type-of`, `method-type`, `constructor`, `constructor-type`, `varargs-element`, `fn-target`, `operator`, `ref-kind`, `jump-target` (a tree of the asker's representation), `pattern-switch?`, `unconditional-pattern?`, `exhaustive?`, `completes-normally?`, `attribute`, `mods-flags`, `packge`, `anon-super-ctor`: for javac's trees the tree's own answer, for the native trees the answer of its twin; `index`, the pairing; the measurement (`note!`, `framed`, `split-of`) |
| `arbace/repr/java/convert.ae` | 3,184 (was 3,203) | the same converter: 575 calls of the tree interface and 159 of attribution's replace its 600-odd javac accessors and `instance?` tests; `ex`, `stmt`, `stmts`' statements, the class, method, field, enum-constant and initializer forms are frames (below); javac's type of an expression (`:jt`) asked when a conversion needs it, not for every expression |
| `arbace/repr/java/split.ae` | 308 | the catalogue of the facts and their tiers, the summaries of the runs, the static split |
| `arbace/repr/java.ae` | 403 (was 328) | `read`/`read-jdk --front javac\|native\|both`, `--native-out`, `--split` (`repr-java-split.edn`) |
| `bin/lib/repr-java/front-check` | 161 | the check, `bin/repr-java front-check [--corpus C,...]` |

The answers stay javac's objects (`Symbol`, `Type`, `Attribute`): the interface is at the boundary
where a tree is asked something, which is what a native attribution (JN9) would have to answer; what
the converter then asks of the answers (`jtypes`, `resolve`, about 70 methods of javac's `Symbol`,
`Type`, `Types`, `Symtab` and `Attribute`, 25 of `Flags`' bits) is the model JN9's symbols and types
would have to offer.

`read --front native` parses each unit's text (`JavacTask`'s own, `getCharContent`) with
`arbace.repr.java.parse`, pairs the native tree with javac's attributed tree (`attr/index`) and
converts the native tree; `--front both` converts each unit twice in one javac run, javac's trees into
OUT and the native ones into `--native-out` (default `OUT.native`). javac's path is unchanged in what
it reads; its cost grew little (`bin/repr-java check` 29 s against JN8's 25 s, `bin/jrt-convert
update` 230 s against 216 s, the host shared).

### JFS66 (b).2 Attribution by position: what javac adds to the trees

`attr/index` walks the native tree and javac's attributed one together, field by field in
`arbace.repr.java.parse`'s order: a native node and javac's of the same tag and `pos` are twins; in a
list, javac's nodes before the next twin are javac's additions, skipped; a native field empty where
javac's holds a tree is javac's addition too. What attribution adds, found by the corpora (all of them
handled; no native node is left without a twin, `:misses` 0 in every corpus):

| javac's addition | phase | the converter |
|---|---|---|
| a default constructor, prepended to `defs`; a record's canonical constructor and accessors | TypeEnter | left out by its flags (`GENERATEDCONSTR`, `GENERATED_MEMBER`): absent from the native trees |
| `super()` as a constructor's first statement | Attr | left out by the text at its position (`implicit-super-call?`): absent |
| an anonymous class's constructor; its `extending`/`implementing` | Attr | the superclass constructor it calls is a fact (`anon-super-ctor`, by the native class body's twin) |
| a `var`'s and an implicit lambda parameter's `vartype`; a compact constructor's parameters | Attr, TypeEnter | read from the symbols, not the trees |
| an annotation's single argument `x` made `value = x` | Annotate | the twin of `x` is the assignment's right side |
| a record component's annotations that do not apply to its field, taken off the field's modifiers (4 in java.base's `Gatherers`) | TypeEnter | no value (`attribute` nil), as javac's tree has none |
| the modifiers' flags: `PUBLIC \| STATIC` of an interface's member class (Enter), `VARARGS` off a record field (TypeEnter), an internal bit (Attr) | Enter, TypeEnter, Attr | the modifiers as Enter left them are a fact (`mods-flags`) |
| break, continue and yield targets, symbols, types, operators, chosen members, instantiated signatures, flow's bits | Attr, Flow | the facts of `attr` |

| corpus | native nodes paired | javac's additions skipped | modifiers whose flags javac changed | annotations taken off |
|---|---:|---:|---:|---:|
| samples | 2,379 | 42 | 10 | 0 |
| baseline | 232,601 | 412 | 1,132 | 0 |
| jdk | 947,293 | 3,092 | 4,594 | 4 |
| javac | 836,769 | 4,173 | 5,314 | 0 |
| jdk26u | 8,872,827 | 33,066 | 59,061 | 4 |

### JFS66 (b).3 The check: `bin/repr-java front-check`

`bin/repr-java front-check [--corpus C,...]` (`bin/lib/repr-java/front-check`; Makefile
`repr-java-front-check`, in `make verify`) reads each corpus as its vendoring script or check reads
it, with `--front both --split`, and compares the two outputs file by file (the reports aside); the
files that differ must be `test/repr/java/front-check-known.edn`'s. Then `arbace.repr.java.split`
sums the split by corpus (`.tmp/repr-java-front-check/split.md`, `split.edn`) and the static split
(`static.edn`). On the final tool (key `e0096891...`), the host shared:

| corpus | read as | Java files | forms files | equal | wall |
|---|---|---:|---:|---:|---:|
| `samples` | `bin/repr-java check` | 14 | 17 | 17 | 2 s |
| `baseline` | `bin/repr-java check` | 183 | 189 | 189 | 52 s |
| `jdk` | `bin/jrt-convert`'s convert step (read-jdk, 4 chunks, `REPR_JAVA_PATCH_ALL`), on parse-check's originals | 838 | 911 | 911 | 165 s |
| `javac` | `bin/javac-vendor convert` (read-jdk per module, 8 chunks; `buildtools` by read) | 824 | 1,828 | 1,828 | 153 s |
| `jdk26u` (`--corpus all`) | `bin/repr-java check --jdk` (read-jdk per module, 4 chunks) | 12,730 | 13,788 | 13,785 to 13,788 (JFS66 (b).5) | about 19 minutes |

The default corpora take about 6.5 minutes; `jdk26u`, opt-in, about 19 more (JFS67).

### JFS66 (b).4 The split, measured

**The code** (`arbace.repr.java.split`'s static split of `convert.ae`: its top-level forms read, the
symbols each names; code lines without blanks and comments):

| | top-level forms | code lines |
|---|---:|---:|
| `convert.ae` | 243 | 2,803 |
| asking attribution (`attr/`) | 79 | 1,661 (59.3%) |
| asking it or working on its answers (javac's `Symbol`, `Type`, `Flags`, `jtypes`, `resolve`) | 135 | 2,203 (78.6%) |
| the same or calling such a form | 164 | 2,488 (88.8%) |
| neither (syntax alone: the tree's shape, the forms' layout, `simplify`, label and loop bookkeeping, literals' text) | 79 | 315 (11.2%) |

So §2.1's measurement holds (77%, by its text scan), and the converter's tree walking (`tree.ae`'s
interface, 575 calls) is the small part, as JN8.9 expected.

**The facts** (`read --split`: each attribution call counted by its site; `split.ae`'s catalogue
gives each fact's javac phase and its tier, cheapest first: *syntax* what the tree gives by a local
rule (a literal's type), *scope* local variables and jump targets (a syntactic pass), *declarations*
Enter, TypeEnter and MemberEnter's (classes, members, flags, declared types, type names, annotation
values), *flow*, *members* (a name in code resolved to a field or class), *types* (expression types,
constants, operators, member types at the site's type arguments), *overload* (the method or
constructor chosen, arity, the declared signature), *inference* (a generic method's or a diamond's
instantiated signature, lambda and method reference targets)). An instantiated signature is classed
by javac's answer: the symbol's own type when it is not generic (*overload*), the member type at the
site's type arguments (*types*), inferred when the method has type parameters of its own or the
creation is a diamond (*inference*). Facts asked, by tier:

| tier | jdk26u | jdk | baseline |
|---|---:|---:|---:|
| syntax | 536,151 (3.5%) | 78,820 (5.0%) | 13,647 (3.0%) |
| scope | 3,306,809 (21.8%) | 374,782 (23.6%) | 108,301 (24.0%) |
| declarations | 2,083,326 (13.8%) | 192,383 (12.1%) | 55,740 (12.4%) |
| flow | 33,946 (0.2%) | 3,661 (0.2%) | 1,849 (0.4%) |
| members | 1,286,625 (8.5%) | 119,604 (7.5%) | 34,623 (7.7%) |
| types | 5,369,276 (35.4%) | 607,758 (38.2%) | 158,476 (35.2%) |
| overload | 2,494,035 (16.5%) | 209,077 (13.1%) | 77,643 (17.2%) |
| inference | 38,589 (0.3%) | 5,101 (0.3%) | 44 (0.0%) |
| total | 15,148,757 | 1,591,186 | 450,323 |

**The output.** Each converted node is a frame (`attr/framed`: an expression, a statement, a class,
method, field, enum constant or initializer); a frame records the facts it asks itself (not its
sub-frames'), and owns the lists of its result that no sub-frame wrote. Every list of the output
(before `simplify`, which only merges `if`s into `cond`s and splices `do`s) is so classed by what its
writing asked:

| lists, by the most demanding tier asked (cumulative) | jdk26u | jdk | javac | baseline |
|---|---:|---:|---:|---:|
| syntax | 0.6% | 0.7% | 0.5% | 0.3% |
| + scope | 2.3% | 2.7% | 1.6% | 0.9% |
| + declarations | 15.0% | 14.9% | 13.1% | 14.0% |
| + flow | 15.0% | 14.9% | 13.1% | 14.0% |
| + members | 16.0% | 16.1% | 14.2% | 14.5% |
| + types | 68.7% | 75.0% | 67.0% | 65.0% |
| + overload | 98.3% | 97.8% | 96.8% | 99.9% |
| + inference | 100% | 100% | 100% | 100% |
| lists | 2,258,047 | 241,459 | 215,029 | 61,474 |

| lists asking a fact of the tier (not exclusive) | jdk26u | jdk | baseline |
|---|---:|---:|---:|
| scope | 28.1% | 27.3% | 32.2% |
| declarations | 32.1% | 27.9% | 38.2% |
| flow | 0.4% | 0.2% | 0.5% |
| members | 11.5% | 10.7% | 9.3% |
| types | 81.2% | 81.9% | 83.2% |
| overload | 31.1% | 24.9% | 35.0% |
| inference | 1.7% | 2.2% | 0.1% |

By node: of jdk26u's 3,438,563 converted expressions, 729,333 (21.2%) ask nothing, and they are the
literals (527,961, whose type is their token's) and the parentheses (201,372); every other expression
asks something. Most of *types* is the converter's conversions (`:types/result`, 2,631,738: an
operand's type decides an unboxing, a cast, a widening, the typed operator) and the qualifiers' types
(1,120,268); most of *overload* the calls' methods (772,792) and their declared signatures (480,709
calls of a non-generic method, 76,827 of a generic class's member at the site's type arguments, 21,049
inferred).

### JFS66 (b).5 Found: the pins depend on the history of the javac run

In jdk26u three files differ between the two conversions, one pinned call each: `EnumSet.of(Kind)`
in jdk.javadoc's `ElementsTable` and jdk.jdeps' `jdeprscan/Main`, `Stream.of(DocTree.Kind.values())`
in jdk.javadoc's `doclint/Checker`. They are not the native trees': converting javac's own trees twice
in one run gives the same difference (a temporary `--front` of two javac conversions), and two fresh
runs of javac's trees alone differ from each other on `Checker` (one pins, one does not). The cause is
the converter's pin decision (`arbace.repr.java.resolve`): `arbace.classes.analyze`'s resolution over
class infos made from javac's symbols as they are met, kept per javac run; an info made while a
class's completion is in a particular state (javac on Arbace completes `JavaFileObject.Kind` once
with a failure, then without its superclass) is kept, so a later call of `EnumSet.of` with that
argument finds no method and is pinned. The committed corpora are not affected (their updates are
byte for byte, and `verify` reproduces them at every gate); `check --jdk` checks only that every file
converts. `test/repr/java/front-check-known.edn` lists the three (JFS70).

### JFS66 (b).6 What it means for JN9

The native parser plus javac's attribution by position reproduces the converter exactly: the
interface is complete, and what JN9 must provide is exactly `attr`'s facts. The measurement says
where the work is:

- **The facts JN9 must answer** (the catalogue, `arbace.repr.java.split/catalogue`, 70 sites):
  *scope*: the local variable or parameter a name denotes, a local declaration's symbol (identity,
  declared type, finality), pattern variables, lambda parameters (their inferred types when
  implicit), jump targets. *declarations*: each class's, method's and field's symbol with javac's
  flags (implicit modifiers, varargs, generated members, compact constructors, enum and record
  members), declared types with type names resolved (imports, nesting, inherited member types, type
  arguments, type annotations), whether a qualifier names a type or package (JLS 6.5), the package's
  classes, annotation values with retention, the platform's declarations behind all of them (class
  files: jdk26u's 68 modules). *flow*: whether a case's statements complete normally, exhaustiveness,
  unconditional patterns. *members*: a simple or qualified name's field or class (inheritance, outer
  classes, static imports). *types*: every expression's type with generics (member types at the
  site's type arguments, capture, `lub` of conditionals), constant values (JLS 15.29), the operator by
  the operand types, pattern switches. *overload*: javac's method and constructor resolution (the
  three phases, most specific, the declared signature), the arity, the anonymous class's superclass
  constructor, method references' methods and kinds. *inference*: generic methods' and diamonds'
  instantiated signatures and variable arity elements, lambda and method reference targets,
  signature polymorphic results.
- **How much each tier carries**: a front end of declarations alone (no expression typing) would
  already give 15% of the lists exactly, and every declaration form; expression types bring that to
  69%, javac's overload resolution to 98%; inference, the open tail of §8 R6, is 1.7% of the lists
  (0.1% of the baseline's, 2.2% of java.base's closure) and 0.3% of the facts, but no corpus is
  equal without it.
- **The estimate**, re-made from the tiers and from JN8's record (javac's parser, 9,300 lines, ported
  in about four and a half hours against 4-6 days estimated): javac's attribution is about 47,000
  lines (§3.1) and stateful (symbols completed lazily from class files, scopes, deferred attribution),
  so a port method by method, as JN8's, by tiers each checked fact by fact against javac through
  `attr` (the check JFS69 builds on): declarations and scope with the platform's class files (Enter,
  TypeEnter, MemberEnter, Annotate, ClassReader's role: `jrt.classfile` and `arbace.classes.env`
  exist) 4-7 days; members and expression types with generics (Attr's expressions, Types, ConstFold,
  Operators) 6-10; overload resolution (Resolve) 3-5; inference (Infer, DeferredAttr, ArgumentAttr)
  6-12 with the tail; about 19-34 working days against §7's 25-45, the first stage alone exact for 15%
  of the lists.

### JFS66 (b).7 The shipped program's change

None in code: `arbace/repr/java` is not in the shipped program (j2g's `embed.ae` leaves it out; javac's
tool embeds it, now with `tree`, `attr` and `split`: 216,862,880 bytes against JN8's 215,048,352), and
`go/`, `overlay/`, `jdk/forms`, `jdk/data` and `jdk/edge.edn` are unchanged. The provenance files'
`:repr-java` (`bin/lib/repr-java/host.bash`'s `repr_java_reader_hash`, which now hashes `tree` and
`attr` too) moved from `2abe20bf...` to `9f87559b...`, written by the updates on the final tool (key
`e0096891...`): `bin/clojure-tests --java-fixtures update` (15 forms files), `bin/jrt-convert update`
(911 forms files, 10 data files, 911 of 911 equal to the committed forms), `bin/javac-vendor convert`
(914 forms files; its edge on the frozen v4 jar: 223 classes beyond `jdk/edge.edn`'s): every form
file byte for byte as committed, only `:repr-java` and `:host` changed in `jdk/provenance.edn`,
`test/java-fixtures/provenance.edn` and `jdk/javac/provenance.edn`. `jdk/provenance.edn` is in the
runtime key (`bin/lib/version.bash`), so the version map the executable carries changes, as at JN8.
No re-seed (batched).

### JFS66 (b).8 The checks

- `bin/repr-java front-check --corpus all` on the final tool: JFS66 (b).3's table; the default
  corpora every file equal, jdk26u's 13,788 but the known three
- `bin/repr-java check`: as expected (the baseline's 189 files as `baseline.sha256`, the samples' 11
  and the module's 6 as `test/repr/java/expected`; 29 s)
- `bin/repr-java parse-check` (the default corpora, the parser unchanged): every file of every corpus
  equal (200 s)
- the updates of JFS66 (b).7: only the three provenance files' `:repr-java` and `:host` changed
- `make check-makefile`: passes
- `bin/gate` (essential): on the branch at `432d05a4` (this section's commit but this line), passes in
  17m37s (go-build 478 s, afresh; go-smoke, suite-go 777 tests with no regression, oracle-go 53 known
  mismatches as recorded, diffcheck 12,743 of 12,766 cases agree with the 23 known, goi-check 797 of
  797, generated)
- not run: `bin/gate --full` (the user's rule: at the milestone), `bin/repr-java roundtrip`, `check
  --suite`, `bin/javac-vendor verify` and `classes` (the converter's outputs and javac's world are
  unchanged: the updates show the forms byte for byte)

### JFS66 (b).9 Proposals

- **JFS67 The check in the gates.**
  - Options: (a) `bin/repr-java front-check` (its default corpora: samples, baseline, jdk, javac) in
    `bin/gate --full`'s java chain after `repr-java-parse`, on the tool the javac step built (built:
    `repr-java-front`, about 6.5 minutes), jdk26u opt-in (`--corpus all`, about 19 minutes more); (b)
    out of every gate, a command and the Makefile's target; (c) jdk26u in `--full` too.
  - *Recommended: (a)*: it is the interface's and the index's check, and JN9's harness; (c) once JFS70
    makes jdk26u deterministic.
- **JFS68 The converter's interface.**
  - Options: (a) as built: `convert.ae` over `tree` and `attr`, javac's trees the default, the native
    trees by `--front native`; (b) back to javac's accessors in `convert.ae` (the interface kept on a
    branch); (c) the native trees the default of `read` (javac still attributing, the parser's cost,
    about 9 times javac's, JN8.5, added to every read).
  - *Recommended: (a)*: the outputs are unchanged and its cost small; it is where JN9 plugs in, and
    (c) brings nothing until javac's attribution goes.
- **JFS69 JN9.**
  - Options: (a) as §7 plans it: the whole attribution, then the switch (19-34 days by JFS66 (b).6,
    the tail in inference); (b) by the measured tiers, each a provider behind `attr` checked fact by
    fact against javac's answers over the corpora (an `attr` check beside `front-check`: both
    providers asked, the answers compared as data): JN9a scope and declarations with the platform's
    class files (4-7 days; exact for 15% of the lists and every declaration form, and what tools
    without javac's world need: names, members, types of declarations), then JN9b types, JN9c
    overloads, JN9d inference, decided one after the other on the previous one's numbers; (c) stop:
    javac on Arbace stays the front end, the native parser and the interface serve tools.
  - *Recommended: (b), starting with JN9a* if a consumer of a javac-free front end is in sight (Java in
    the box without javac's world, an editor's names); (c) otherwise: javac on Arbace already makes
    repr/java JVM-free, and the facts beyond the declarations are 85% of the lists.
- **JFS70 The pins' dependence on the run's history** (JFS66 (b).5).
  - Options: (a) recorded (built: `test/repr/java/front-check-known.edn`, three files of jdk26u); (b)
    fixed in `arbace.repr.java.resolve`: class infos made from completed symbols only (complete before
    reading, never keep an info made during a failure), or a registry per unit, then the updates and
    `check --jdk` show whether any committed form changes; (c) the pin decided by javac's answer alone
    (javac's method against the forms compiler's choice computed from the declarations, not from
    symbols met in passing).
  - *Recommended: (b)* as a task of its own (the committed forms are not affected today, but read-jdk's
    chunking and thread timing could make them so), the known list then emptied.
- **JFS71 The converter's demand.**
  - Options: (a) as built: the converter asks what it always asked (the symbol of every name in an
    expression with postfix increments, `:names/hoist` and `:overload/hoist` 950,509 in jdk26u; the
    qualifier's type before deciding a field is static); (b) narrowed before JN9: identity questions
    answered by scope (a name is a local or not) where the converter only compares locals, so that
    JN9a's stage covers more of the lists.
  - *Recommended: (b), within JN9a* if JFS69 (b) is decided; (a) otherwise.

### JFS66 (b).10 Sources and scratch

- jdk26u at `baf63fbe42b8758448fee570a9d9bb914272d259` (`/root/jdk26u`):
  `src/jdk.compiler/share/classes/com/sun/tools/javac/comp/{Attr,TypeEnter,Enter,MemberEnter,Annotate,Flow}.java`
  (what attribution adds to the trees: JFS66 (b).2), `tree/{JCTree,TreeInfo}.java` (`getTypeDecls`,
  `getKind`, `symbol`, `name`, `fullName`, `isDiamond`), `src/*/share/classes` (the jdk26u corpus).
- The worktree's `.tmp/`: `jfs66/` (the tool builds' logs, the checks' logs, `rw/` the converter's
  rewrite as edit scripts, `dbg/` the investigation of JFS66 (b).5, `split-run2.edn`),
  `repr-java-front-check/` (the corpora's outputs, logs, `split.md`, `split.edn`, `static.edn`),
  `repr-java-check/`, `repr-java-parse-check/`, `jrt/`, `javac-vendor/`, `clojure-tests/`.

## Decided (2026-10-23): JFS67-JFS71

The user decided:
- JFS67 (a): `bin/repr-java front-check`'s default corpora in `bin/gate --full`'s java chain,
  jdk26u by `--corpus all`.
- JFS68 (a): javac's trees the converter's default, the native trees by `--front native`.
- JFS69 (b): JN9 by tiers, each a provider behind `attr` checked fact by fact against javac;
  first JN9a, scope and declarations from the platform's class files.
- JFS70 (b): `resolve.ae`'s history-dependent pins fixed as a task of its own, the known list
  emptied; with it, the notice and LICENSE.md/doc/ARBACE.md entry for `tree.ae`'s TreeInfo helpers.
- JFS71: the converter's eager queries narrowed within JN9a.

## As built: JFS70 (2026-10-23)

JFS70 (b), the converter's pins made independent of the history of the javac run, on the branch
`java-frontend-jfs70` from main `ba707b05`, in about half a working day. The three files of jdk26u
JFS66 (b).5 listed are equal now, `test/repr/java/front-check-known.edn` is `{}`, and every form
file the updates write is byte for byte as committed. The JVM ran only the frozen v4 jar, for
javac's edge in `bin/javac-vendor convert`; Go only TamaGo's go1.27.1, without cgo; the tools'
host was the worktree's stage 0 (the seed's).

### JFS70.1 What was wrong

`arbace.repr.java.resolve` decides a pin by asking the Java forms compiler's resolution
(`arbace.classes.analyze`) over class infos it makes from javac's symbols. It found a class by
name as `(first (Symtab/getClassesForName name))`, remembered every symbol it met by internal name
(`register!`, the last one met winning) and kept the info made of a name per javac run.

javac's `Symtab` keeps class symbols by flat name and module (`classes: Map<Name, Map<ModuleSymbol,
ClassSymbol>>`). Besides a nested class's own symbol, under the module of its package, `ClassReader`
enters one more under the key of each other module whose class file names the nested class in its
`InnerClasses` attribute (`readInnerClasses`, `enterClass(currentModule, name, outer)`); that one
has no class file, and completing it fails ("class file for com.sun.source.doctree.DocTree$Kind not
found"); once failed it stays javac's erroneous class (`kind` ERR, no members, no supertypes), and
completing it again no longer throws. `getClassesForName` returns them all in the order of a
`HashMap` keyed by the modules' identity hashes. Instrumented on jdk.javadoc: 2 to 3 symbols for
`DocTree$Kind`, `JavaFileObject$Kind`, `Diagnostic$Kind`, `Symbol$ClassSymbol`, `Context$Key`, one of
them with a class file; with one chunk (`REPR_JAVA_THREADS=1`) the same 6 completion failures in
every run, with 4 chunks a different number in each run. An info made from a stray symbol has no
superclass and no methods, so an enum constant is not an `Enum` and an enum has no `values` or
`toString`: the compiler finds no method and the converter pins javac's choice. Which symbol came
first depended on the run (the identity hashes, the files converted before in the chunk), hence
the three files of JFS66 (b).5 and more that front-check could not see, being wrong the same way in
both of its conversions of a run: on the base tool, every run of jdk.javadoc pinned
`DocTree$Kind`'s inherited `toString` and `equals` in six files (`HtmlDocletWriter`, `SeeTaglet`,
`TagletManager`, `IndexItem`, `Utils`, `JavadocLog`: `(^[] DocTree$Kind/.toString ...)`), and
`JavadocClassFinder`'s `EnumSet.of(JavaFileObject.Kind...)` was pinned in one run of two.

### JFS70.2 The fix

`resolve.ae` (164 lines, now 207) works on javac's own symbols only:

- `canonical`: for a symbol met in a type, a tree or an info, javac's own: the symbol itself when it
  is a class of the run's sources (`Check.getCompiled` of its module and flat name gives it) or a
  local or anonymous class, else the one `Symtab.getClass` keeps under the module of its package.
- `find-class`: a name is resolved to the class of the run's sources `Check` compiles under it in
  the module of the call's class (`*module*`, bound by `choice`), else among `getClassesForName`'s
  symbols those that are their module's own, the module of the call's class first, else the first
  by module name. A first version took Symtab's symbol before the run's sources: a local class of a
  patched module (`CharSequence.chars()`'s `CharIterator`, java.base) has a namesake in the
  platform's class files, whose constructor takes the outer instance, and `bin/jrt-convert update`
  pinned calls in 7 files of `jdk/forms` (constructors of local classes, and
  `HostFiles.setDescriptorAccess` given an anonymous class); with the run's sources first the
  committed forms came back byte for byte.
- `class-info`: made only from a symbol that completes (`completed?`: no `CompletionFailure`, not
  erroneous) and kept per symbol (nil kept for one that does not complete, which is then
  deterministic: a canonical symbol fails only when its class file is absent).
- The registry of symbols met by name (`register!`) is gone; `jtypes/*check*` (javac's `Check` of
  the run) is bound by `convert-units` beside `*syms*`.

Each answer is now a function of the name, the module and the platform, whatever was converted
before. JFS70 (b)'s two candidate approaches: "infos only from completed symbols" is part of the
fix but not enough alone (an info made from a stray symbol is never a completed one, but the right
symbol must still be found by name); "a registry per unit" would make a file independent of its
predecessors but not of `getClassesForName`'s order, which changes between runs.

### JFS70.3 The reproduction

`.tmp/jfs70/repro.sh` (worktree scratch): jdk.javadoc and jdk.jdeps each read twice by
`bin/repr-java read-jdk --check` (4 chunks, fresh runs), and `ElementsTable.java`, `Checker.java`
and jdeprscan's `Main.java` each read alone:

| | base tool (`e0096891...`) | fixed |
|---|---|---|
| jdk.javadoc, run 1 against run 2 | `Checker.ae`, `JavadocClassFinder.ae` differ | equal |
| jdk.jdeps, run 1 against run 2 | equal | equal |
| the three files alone against in their module | (not taken) | equal |
| jdk.javadoc with 1, 4 and 12 chunks, and its `doclint`, `toolkit/util` and `tool` packages alone | the 1-chunk runs pin `Stream.of(DocTree.Kind.values())` | every file equal to the 4-chunk run's |

And over all of jdk26u, two fresh runs on the final tool: `bin/repr-java check --jdk`'s outputs
(`.tmp/repr-java-jdk/conv`) and front-check's conversion of javac's trees
(`.tmp/repr-java-front-check/jdk26u/*/javac`), 68 modules, every forms file equal.

The pins the fix removes are the right ones to remove: javac resolves `EnumSet.of(E)` (JLS 15.12.2's
first phase, `JavaFileObject.Kind extends Enum<Kind>`), `DocTree.Kind.values()` (the enum's
implicit method) and `Enum.toString()`, `Enum.equals(Object)` (inherited), and so does the forms
compiler once `Kind`'s info has its superclass and members: the call needs no param-tags.

### JFS70.4 The effect on the corpora

- The committed forms: none changed. `bin/clojure-tests --java-fixtures update` (15 forms files),
  `bin/jrt-convert update` (911 forms files, 10 data files), `bin/javac-vendor convert` (914 forms
  files; its edge on the frozen v4 jar, 223 classes beyond `jdk/edge.edn`'s, unchanged): only the
  provenance files' `:repr-java` (`9f87559b...` to `c40d75a6...`) and `:host` (the tool's key,
  `e0096891...` to `bbd9d979...`) changed in `jdk/provenance.edn`, `jdk/javac/provenance.edn`,
  `test/java-fixtures/provenance.edn`. The default corpora never met a stray symbol first.
- jdk26u (not committed, checked by `check --jdk` and front-check): the files above (jdk.javadoc's
  six `DocTree$Kind` files, `Checker`, `ElementsTable`, `JavadocClassFinder`, jdeprscan's `Main`)
  lose their spurious pins; the rest of jdk26u was not converted on the base tool for a full count.
- `tree.ae` (JFS70 (b)'s notice): the non-javac branches of `skip-parens`, `tree-name`, `full-name`,
  `diamond?` and `class-kind` reimplement TreeInfo's `skipParens`, `name`, `fullName`, `isDiamond` and
  `JCClassDecl.getKind`'s flag test (jdk26u `baf63fbe42b8758448fee570a9d9bb914272d259`,
  `src/jdk.compiler/share/classes/com/sun/tools/javac/tree/TreeInfo.java`, `JCTree.java`); the file
  carries their notice as `parse.ae`'s port does, and LICENSE.md and doc/ARBACE.md name them.

### JFS70.5 The shipped program's change

None in code: `arbace/repr/java` is not in the shipped program (javac's tool embeds it: 216,879,264
bytes against 216,862,880). `jdk/provenance.edn` is in the runtime key (`bin/lib/version.bash`), so
the version map the executable carries changes, as at JFS66 (b). No re-seed.

### JFS70.6 The checks

- the reproduction (JFS70.3): every run equal
- the updates (JFS70.4): every forms file byte for byte; only the three provenance files changed
- `bin/repr-java check --jdk`: 68 modules converted, 0 with failures (9 m 19 s)
- `bin/repr-java front-check --corpus all`: every corpus equal, the known list empty: samples 17,
  baseline 189, jdk 911, javac 1,828, jdk26u 13,788 forms files (25 m 48 s, the host shared)
- `bin/repr-java check`: the baseline's 189 files as `baseline.sha256`, the samples' 11 and the
  module's 6 as `test/repr/java/expected` (27 s)
- `bin/repr-java parse-check`: every corpus's trees as javac's (the parser unchanged; 3 m 18 s)
- `bin/gate` (essential): on the branch at `ff7a68ba` (this section's commit but this line), passes in
  17m59s (go-build 453 s, afresh; go-smoke, suite-go 777 tests with no regression, oracle-go 53 known
  mismatches as recorded, diffcheck 12,743 of 12,766 cases agree with the 23 known, goi-check 797 of
  797, generated)
- not run: `bin/gate --full` (the user's rule), `bin/repr-java roundtrip`, `check --suite`,
  `bin/javac-vendor verify` and `classes` (the converter's outputs and javac's world are unchanged:
  the updates show the forms byte for byte)

### JFS70.7 Proposals

- **JFS72 jdk26u's forms recorded.**
  - Options: (a) as built: `check --jdk` checks only that every file converts, and front-check that
    both trees give the same forms, so a pin that is wrong the same way in both (the six
    `DocTree$Kind` files) shows nowhere; (b) the hashes of jdk26u's 13,788 forms files recorded
    (`test/repr/java/expected/jdk26u.sha256`, as the baseline's), `check --jdk` comparing against
    them and `check --update --jdk` rewriting them, so that any change to jdk26u's forms is seen and
    reviewed; (c) (b) and two runs compared in `check --jdk` (the history's check, about 9 minutes
    more).
  - *Recommended: (b)*: it costs a file of about 1.4 MB and no time, and it would have shown this
    defect's six files; (c) adds little once the answers no longer depend on the run.

### JFS70.8 Sources and scratch

- jdk26u at `baf63fbe42b8758448fee570a9d9bb914272d259` (`/root/jdk26u`):
  `src/jdk.compiler/share/classes/com/sun/tools/javac/code/Symtab.java` (`classes`,
  `getClassesForName`, `getClass`, `enterClass`), `jvm/ClassReader.java` (`readInnerClasses`,
  `enterClass`), `code/ClassFinder.java` (`complete`, `completeEnclosing`, `fillIn`),
  `code/DeferredCompletionFailureHandler.java`, `comp/Enter.java` (local classes, `unenter`),
  `comp/Check.java` (`localClassName`, `putCompiled`, `getCompiled`), `tree/TreeInfo.java`,
  `tree/JCTree.java`.
- The worktree's `.tmp/`: `jfs70/` (the tool builds' logs, `repro.sh` and `repro/` its runs on the
  base and the fixed tool, `repro/dbg/` the instrumented runs, `dbg2/`, the updates' and checks'
  logs, `arbace-javac.base`, the base tool), `repr-java-front-check/`, `repr-java-jdk/`,
  `repr-java-check/`, `repr-java-parse-check/`, `jrt/`, `javac-vendor/`, `clojure-tests/`.

## Decided (2026-10-23): JFS72

The user decided JFS72 (b): jdk26u's forms recorded as hashes (`test/repr/java/expected/jdk26u.sha256`),
`bin/repr-java check --jdk` comparing against them; built with JN9a, which starts after GP8, `bin/gate
--full` and the re-seed have settled (the user).

## Fixed (2026-10-23): mode :full's members

- **What was seen.** `bin/repr-java roundtrip --corpus jdk --text` failed at the record level on
  `java/util/ResourceBundle.ae`: the forms read in mode `:full` from the corpus's
  `java/util/ResourceBundle.java`, their record stripped, lacked 3 of the class's 22 members (both
  `getBundle` overloads and `newBundle`) that `jdk/forms/java/util/ResourceBundle.ae` has. The text
  level passed.
- **The cause: not the reader.** The jdk corpus's originals, `bin/jrt-convert`'s sources and generate
  steps into `.tmp/repr-java-roundtrip/jdk-originals` (`JRT_WORK_RT`), were made once, when the
  directory was missing, and never again (`jdk_originals` in `bin/lib/repr-java/roundtrip`, since
  `f8d03c78`, the text level). Main's tree was made on 2026-10-20 at 08:06, before JN2's `f1f7889d`
  (2026-10-20, 19:25) added `getBundle` and `newBundle` to jrt's own
  `overlay/jdk/java.base/java/util/ResourceBundle.java`: the text level read the old file in mode
  `:full`, printed it back byte for byte (text: pass, against the same stale original) and compared
  its members with the committed forms of the new file (record: fail). Of the corpus's 806 copied
  originals (not generated) only this one was stale. The reader's recent changes (JN8, JFS66 (b),
  JFS70 (b)) are not involved: with fresh originals the record level passes, 911 of 911, on main's
  reader. The same stale original also fed the `:lines` level's class level (`c1`, compiled from
  the originals) for this file; its result and `jdk-known.edn`'s reason did not change with fresh
  originals.
- **The same flaw elsewhere.** `bin/repr-java parse-check` made its own tree of the originals
  again only when `jdk/sources.txt` changed (an edit to jrt's own Java left it stale), `front-check`
  read parse-check's tree the same way, and `bin/javac-vendor classes jdk` made its tree once, as the
  round trip did. And parse-check and front-check run at once both made the shared tree, one
  removing it under the other (seen here: parse-check's jdk corpus failed so).
- **The fix.** One helper for the four, `bin/lib/repr-java/originals.bash` (outside javac's tool's
  key, as `bin/lib/repr-java/`): the originals keyed (`arbace_jdk_originals_key`, written to
  `originals.key` beside them: the SHA-256 of `bin/jrt-convert`, `test/jrt`,
  `doc/go/java-surface.edn`, `overlay/jdk/java.base`, jdk26u's checkout, its commit and status, and
  `JRT_LOCALES`), the tree made again when the key differs, under a lock (`ORIG.lock`, flock). A
  tree named by `JRT_WORK_RT` is the caller's: made when missing, never removed nor keyed.
- **The check added.** `arbace_jdk_originals_check`, run whenever the originals are used (the
  round trip's jdk corpus, parse-check's and front-check's jdk corpus, `javac-vendor classes jdk`):
  each original that `jdk/sources.txt` lists as copied (share, share:MODULE, unix, overlay; not the
  generated ones) compared byte for byte with its source, a stale file failing the run by name. On
  main's stale tree it names `java/util/ResourceBundle.java` and fails.

- **JFS73 the round trip's cached inputs checked.**
  - Options: (a) as built: the originals keyed and their copied files compared, the generated ones
    trusted to the key (bin/jrt-convert's generate step compares them with the build tree's
    `support/gensrc` when it runs); (b) (a) and the generated originals compared with
    `bin/jrt-convert`'s last work tree (`.tmp/jrt/src`) when one exists; (c) no cache: the
    originals made on every jdk run (about a minute of the generators on the tool).
  - *Recommended: (a)*: the copied files are the ones edited by hand (jrt's own Java); the
    generated ones change only with the inputs the key names.

## Decided (2026-10-24): JFS73

JFS73 (a), as built: the round trip's cached originals keyed and the copied files checked byte for byte,
the generated ones trusted to the key (taken as recommended with the fix's merge).

## As built: JN9a (2026-10-24)

JN9a (JFS69 (b)'s first tier), with JFS72 (b) and JFS71 within it, on the branch `java-frontend-jn9a`
from main `bd3ba0bb`, in about a working day. A native attribution provider answers, behind
`arbace.repr.java.attr`, every fact of the scope and declarations tiers (and the syntax tier's
literal types) of the native parser's trees **without javac**: locals, parameters, pattern variables
and jump targets; class, method and field symbols with javac's flags, declared types with names
resolved, whether a qualifier names a type or a package, annotation values, the modifiers as Enter
leaves them, the unit's package; the platform's classes read from their class files by a reader of
Arbace's own. Each native answer is checked against javac on Arbace's answer for the same tree,
fact by fact: `bin/repr-java attr-check` (front-check `--attr native`) over every corpus, the
samples, the baseline, the jdk corpus, javac's sources and jdk26u's 68 modules, **6,660,390 facts equal,
none different** (the known differences, `test/repr/java/attr-check-known.edn`, are none), and the
converter given the native answers for those tiers and javac's for the rest writes **every corpus's
forms byte for byte as from javac's trees**. The provider alone wrote 344,337 of jdk26u's 2,258,047 lists
(15.2%: the whole of the lists the first three tiers write, JFS66 (b).4's 15.0% grown by JFS71). jdk26u's forms are recorded as hashes
(`test/repr/java/expected/jdk26u.sha256`, JFS72 (b)), `check --jdk` comparing them. The JVM ran only
the frozen v4 jar, for javac's edge in `bin/javac-vendor convert`; Go only TamaGo's go1.27.1, without
cgo; the tools' host was the worktree's stage 0 (the seed's).

### JN9a.1 What was built

| file | lines | what |
|---|---:|---|
| `arbace/repr/java/classfile.ae` | 299 | `arbace.repr.java.classfile`: a class file's declarations as data (flags, superclass, interfaces, Signature, InnerClasses, fields with their ConstantValue and Signature, methods with their Signature, annotations with their values, Record, PermittedSubclasses), and JVMS 4.7.9.1's signatures parsed; after `jrt.classfile` (JN7), which reads code |
| `arbace/repr/java/decls.ae` | 1,635 | `arbace.repr.java.decls`, the declarations tier: a run's registry (its units' classes, the sources javac would find on the source path or in a patched module's directories, the platform's classes from their class files, each made on first use); class, field, method and type variable symbols with memoized parts (flags as javac's: Check.checkFlags, Enter's, TypeEnter's, MemberEnter's, Annotate's DEPRECATED, an enum's ABSTRACT; headers: type parameters, superclass, interfaces as TypeEnter makes them; members; declared types); types as data (class types with their arguments and enclosing type, arrays, type variables, wildcards, intersections, type annotations), erasure, substitution, asSuper, asOuterSuper, asEnclosingSuper; environments as data and Resolve's lookups (findType, findVar, findField, member types, imports: single, on demand, static, the unit's package and java.lang); a type tree's type (Attr.attribType, checkId's for a class name); constants (ConstFold) and annotation values (Annotate) |
| `arbace/repr/java/scope.ae` | 593 | `arbace.repr.java.scope`, the scope tier: a unit's native tree walked with javac's environments (Attr's for blocks, loops, switches with addVars, lambdas, try resources and catch parameters, the match bindings of patterns with their flow scoping, Flow's liveness as far as the bindings need it, findJumpTarget), its answers recorded by node (an IdentityHashMap), the expensive ones (types, annotation values) as delays |
| `arbace/repr/java/nattr.ae` | 377 | `arbace.repr.java.nattr`, the provider: a unit's answers, javac's and the native ones rendered alike as data (symbols by identity, flags under a mask per kind, declared types, annotation values), the outcomes counted, and the native answers realized as javac's objects for the converter (a symbol by its declaration's twin or by module and flat name in javac's Symtab, a type built from the native one, an annotation's value constructed) |
| `arbace/repr/java/attr.ae` | 528 (was 349) | each fact of the native tiers asked of the provider when there is one (`*provider*`): the native answer checked against javac's and given to the converter; `local-of` and `type-expr?` (JFS71); `note!` marks a frame whose facts javac answered (`:provider/javac`), for the split's share |
| `arbace/repr/java/convert.ae` | 3,189 (was 3,184) | JFS71's narrowed questions (JN9a.4) |
| `arbace/repr/java.ae` | 475 (was 404) | `read`/`read-jdk --attr native`: the run's native units parsed and entered in a registry before any is converted (the module a source module-info names, a patched module's directories, the source path); `OUT/repr-java-attr.edn` |
| `arbace/repr/java/split.ae` | 382 (was 308) | the share of lists the provider wrote alone; `split attr OUT KNOWN DIR...`, the outcomes by corpus, tier and fact (`attr.md`, `attr.edn`), the differences beyond the known |
| `bin/lib/repr-java/front-check` | 192 (was 161) | `--attr native` (`bin/repr-java attr-check`): the reads with the provider, its outcomes summed, the known differences in `test/repr/java/attr-check-known.edn` |
| `test/repr/java/jdk.bash`, `bin/lib/repr-java/check` | | JFS72 (b) (JN9a.5) |

The platform is what javac on Arbace attributes against (the exploded modules of
`jdk/javac/platform.sha256`, REPR_JAVA_PLATFORM); a package's module is the one whose directory holds
it, the run's own module first. A run's sources are entered before any unit is walked, so that a
unit's names resolve to the other units' classes; a class the run does not convert but javac reads
from source (the source path of `read`, the other chunks of a patched module with
`REPR_JAVA_PATCH_ALL`) is parsed and entered on first use, as javac's ClassFinder prefers the
sources it is given (they are newer than the platform's class files).

### JN9a.2 The facts, the answers, the check

The provider answers the facts `arbace.repr.java.split`'s catalogue puts in the syntax, scope and
declarations tiers, asked through `attr`:

| fact | the native answer | data compared with javac's |
|---|---|---|
| `scope/*` of a name (`sym`, `local-of`: ident, assigned, capture, hoist, refers, local-names) | the local variable entry the name denotes (Resolve.findVar over the environment's levels: locals, then the class's fields, inherited ones too, an inaccessible field not hiding), or not a local | the local's declaration (its position), or `[:nonlocal]` |
| `scope/local-decl`, `param`, `lambda-param`, `binding`, `enter/field-decl`, `method-decl`, `class-decl` | the declaration's symbol | identity, name, flags under a mask (the modifiers and javac's bits the converter reads: VARARGS, ENUM, RECORD, COMPACT_RECORD_CONSTRUCTOR, GENERATEDCONSTR, DEPRECATED, DEPRECATED_ANNOTATION, HASINIT, DEFAULT, SEALED, ...), declared type; a method's type parameters with their bounds, parameter types, return and thrown types, parameter names and default value; a class's type parameters, superclass and interfaces |
| `scope/jump-target` | the statement left | its position and tag |
| `names/type-tree`, `names/record-pattern`, `types/this`, `types/class-literal`, `types/literal` | the type | the type as data, its annotations included |
| `names/type-expr` (JFS71) | whether the qualifier names a type or a package (JLS 6.5, as Attr's KindSelector VAL \| TYP) | the boolean |
| `annotations/attribute` | the annotation's value (Annotate.attributeAnnotationValue: constants folded and coerced to the element's type, classes, enum constants, arrays, nested annotations) | the value as data |
| `enter/flags` | the modifiers' flags as Enter and Attr leave them on the tree | the long |
| `names/package` | the unit's package | its name |

A fact the provider cannot answer within its tiers it **declines** (javac answers, the outcome
counted): a `var` local's and an implicit lambda parameter's type (inferred), a diamond's type tree and
an anonymous class of a diamond (its supertype inferred), a qualified creation's class through an
expression (`o.new C() {}`, `o` not a variable), a name whose lookup crosses such a class, an
annotation javac takes off a record component. The answers' further properties that the converter
reads from the realized javac objects (a class's members, a local's constant value, a symbol's
erasure) are javac's: the converter's model is still javac's `Symbol`, `Type` and `Types` (JFS75).

What javac does that the corpora showed and the provider follows (each found by a fact that differed,
then fixed): the ABSTRACT an enum gets when a constant body implements its abstract method
(`markAbstractIfNeeded`); RECORD on a record's canonical constructor; FIELD_INIT_TYPE_ANNOTATIONS_QUEUED
(bit 53) on a field's modifiers when it has an initializer, shared by the declarators of one declaration
(`int a = 1, b;`); a pattern variable's flags (its modifiers', HASINIT, no checkFlags); a TYPE_USE
declaration annotation on the declared type (an array's element type), on a catch union's first
alternative's tree but not on the union; `import T.*` imports T's own member classes, a static import
also inherited ones (Scope's SymbolImporter), and only accessible ones (typeImportFilter); a private
member class found through its site class; a rule case's statements and body are one tree that the
native parser freezes as two copies, both asked; a compact constructor's parameters are the parser's
(not TypeEnter's); `o.super(...)`'s qualifier is an expression; Object's supertype is javac's NONE.

### JN9a.3 The check: `bin/repr-java attr-check`

`bin/repr-java attr-check [--corpus C,...]` is front-check (JFS66 (b).3) with `--attr native`: each
corpus read with `--front both --split --attr native`, javac attributing once, the converter writing
the forms of javac's trees and of the native trees with the native provider; the forms must be equal
(but `test/repr/java/attr-check-known.edn`'s files) and no fact differ beyond that file's counts; the
outcomes summed by corpus, tier and fact (`.tmp/repr-java-attr-check/attr.md`), the split with the
provider's share (`split.md`). Makefile `repr-java-attr-check`, in `make verify`, and in `bin/gate
--full`'s java chain after `repr-java-front` (`repr-java-attr`, JFS74 (a)). On the final tool (key
`9aef633f...`), the host shared:

| corpus | read as | forms files | equal | facts equal / different / declined | wall (front-check's) |
|---|---|---:|---:|---:|---:|
| `samples` | `bin/repr-java check` | 17 | 17 | 1,618 / 0 / 12 | 3 s (2 s) |
| `baseline` | `bin/repr-java check` | 189 | 189 | 156,291 / 0 / 14 | 59 s (50 s) |
| `jdk` | `bin/jrt-convert`'s convert step (read-jdk, 4 chunks, `REPR_JAVA_PATCH_ALL`) | 911 | 911 | 585,764 / 0 / 2,469 | 174 s (161 s) |
| `javac` | `bin/javac-vendor convert` (read-jdk per module, 8 chunks; `buildtools` by read) | 1,828 | 1,828 | 490,792 / 0 / 2,828 | 170 s (149 s) |
| `jdk26u` (`--corpus all`) | `bin/repr-java check --jdk` (read-jdk per module, 4 chunks) | 13,788 | 13,788 | 5,425,925 / 0 / 28,585 | 1,336 s (about 19 minutes, JFS66 (b).3) |

By tier (equal / different / declined):

| tier | samples | baseline | jdk | javac | jdk26u |
|---|---:|---:|---:|---:|---:|
| syntax | 128 / 0 / 0 | 13,647 / 0 / 0 | 78,820 / 0 / 0 | 52,729 / 0 / 0 | 536,151 / 0 / 0 |
| scope | 871 / 0 / 8 | 84,353 / 0 / 10 | 308,991 / 0 / 1,302 | 242,494 / 0 / 1,657 | 2,715,734 / 0 / 20,628 |
| declarations | 619 / 0 / 4 | 58,291 / 0 / 4 | 197,953 / 0 / 1,167 | 195,569 / 0 / 1,171 | 2,174,040 / 0 / 7,957 |

By fact, in jdk26u (equal / declined; none different): `types/literal` 536,151; `scope/ident`
969,565; `scope/local-decl` 647,980 / 12,896 (`var`); `scope/local-names` 507,277; `scope/param`
220,689; `scope/assigned` 183,733; `scope/capture` 69,026; `scope/refers` 68,093; `scope/hoist`
22,619; `scope/jump-target` 22,433; `scope/binding` 4,245 / 15; `scope/lambda-param` 74 / 7,717
(implicit); `names/type-expr` 601,702; `enter/method-decl` 561,805; `enter/flags` 295,856;
`enter/field-decl` 285,307; `names/type-tree` 188,905 / 7,375 (diamonds); `annotations/attribute`
86,779 / 8 (record components); `enter/class-decl` 81,264 / 574 (anonymous classes of diamonds and
of qualified creations); `types/this` 49,161; `names/package` 12,730; `types/class-literal` 10,508;
`names/record-pattern` 23. The whole run of the five corpora took 29 minutes on the final tool (the
host shared). The provider's cost is in the native conversion, the second of each corpus's two: the
corpora's walls grew 8-18% against front-check's (javac's attribution answering both).

### JN9a.4 JFS71: the converter's eager queries narrowed

The converter asked a symbol where it only compared locals; those questions are now asked as such,
`attr/local-of` (the local a name or declaration denotes, or nil: the scope tier) and
`attr/type-expr?` (whether a qualifier names a type or package: a boolean), and their answers are the
same as before for every tree (the updates and `check --jdk` byte for byte, JN9a.6):

| site | before | now |
|---|---|---|
| hoisting (`hoistable-increments`) | the symbol of every name in an expression with postfix increments (950,509 in jdk26u, `:names/hoist`, `:overload/hoist`) | a candidate's local by scope and the uses of that local counted by scope; the names' symbols asked only for a field candidate in an expression without effects |
| `captured-mutables`, `assigned-syms`, `step-update`, `assigns-only?`, `refs-sym?`, `*local-names*` | the symbol, then `local?` | `local-of` |
| `type-expr?` | TreeInfo.symbol's kind | `type-expr?` (`:names/type-expr`) |
| a case label naming a constant | the symbol's kind | none: a constant label's name is a variable's |
| a try resource's name | its symbol's name | the tree's name |
| `ident`, `select` | the symbol first, also for `this`, `super`, `.class` | after those cases |

In jdk26u the facts asked went from 15,148,757 (JFS66 (b).4) to 13,544,011, the members tier's from
1,286,625 to 556,811 (the hoisting's and the comparisons' symbols), the overload tier's from 2,494,035
to 2,090,879; the first three tiers' share of the facts from 39.1% to 40.3%, and the lists they write
alone from 15.0% to 15.2% (the first two tiers' from 2.3% to 2.5%).

### JN9a.5 JFS72 (b): jdk26u's forms recorded

`test/repr/java/expected/jdk26u.sha256` holds the SHA-256 of each of jdk26u's 13,566 forms files
(68 modules; their first line without `JDK_SRC`), 1.6 MB; `bin/repr-java check --jdk` compares the
converted modules' lines with it, `check --jdk --update` (or `check --update --jdk`) rewrites them. It
was recorded on the base tool (key `bf740cf0...`, main's converter) before any converter change, so
that `check --jdk` on the final tool shows JFS71's narrowing kept jdk26u's forms: 13,566 files of 68
modules, as expected (559 s).

### JN9a.6 The shipped program's change

None in code: `arbace/repr/java` is not in the shipped program (j2g's `embed.ae` leaves it out;
javac's tool embeds it, now with `classfile`, `decls`, `scope` and `nattr`: 220,930,208 bytes against
JFS70's 216,879,264), and `go/`, `overlay/`, `jdk/forms`, `jdk/data` and `jdk/edge.edn` are unchanged.
The provenance files' `:repr-java` (`bin/lib/repr-java/host.bash`'s `repr_java_reader_hash`, which
hashes `attr` and `convert`) moved from `c40d75a6...` to `16970302...`, written by the updates on the
final tool (key `9aef633f...`): `bin/clojure-tests --java-fixtures update` (15 forms files),
`bin/jrt-convert update` (911 forms files, 10 data files), `bin/javac-vendor convert` (914 forms
files; its edge on the frozen v4 jar: 223 classes beyond `jdk/edge.edn`'s): every forms file byte for
byte as committed, only `:repr-java` and `:host` changed in `jdk/provenance.edn`,
`jdk/javac/provenance.edn` and `test/java-fixtures/provenance.edn`. The reader hash does not name the
provider's namespaces (`classfile`, `decls`, `scope`, `nattr`): they change no forms of a read with
javac's attribution, as `parse` and `split` do not. `jdk/provenance.edn` is in the runtime key
(`bin/lib/version.bash`), so the version map the executable carries changes, as at JFS70. No re-seed.

### JN9a.7 The checks

All on the final tool (key `9aef633f...`), one after another, the host shared:

- `bin/repr-java attr-check --corpus all`: JN9a.3's table; every corpus's forms equal and every fact
  equal, the known differences none (1,746 s)
- `bin/repr-java front-check`: every default corpus equal (364 s); `bin/repr-java parse-check`: every
  corpus's trees as javac's (the parser unchanged; 205 s)
- `bin/repr-java check`: the baseline's 189 files as `baseline.sha256`, the samples' 11 and the
  module's 6 as `test/repr/java/expected` (27 s)
- `bin/repr-java check --jdk`: 68 modules converted, 0 with failures, the 13,566 forms files as
  `test/repr/java/expected/jdk26u.sha256` (recorded on the base tool; 559 s)
- `bin/repr-java roundtrip --corpus baseline,samples,jdk --text`: as the references (606 s; the
  converter's JFS71 change)
- the updates of JN9a.6: only the three provenance files' `:repr-java` and `:host` changed
- `make check-makefile`: passes
- `bin/gate` (essential): on the branch at `61d38abd` (this section's commit but this line), passes in
  18m30s (go-build 478 s, afresh; go-smoke, suite-go 777 tests with no regression, oracle-go 53 known
  mismatches as recorded, diffcheck 12,743 of 12,766 cases agree with the 23 known, goi-check 797 of
  797, generated); and `bin/j2g-edge verify` (main's essential gate holds it since this branch left
  main): `jdk/edge.edn` is the JVM's answer (5m32s)
- not run: `bin/gate --full` (the user's rule), `bin/repr-java check --suite`, `bin/javac-vendor
  verify` and `classes` (the converted forms and javac's world are unchanged: the updates show the
  forms byte for byte)

### JN9a.8 What it means for the next tiers

JN9a took about a working day against JFS66 (b).6's 4-7 days (JN8 four and a half hours against 4-6),
the port again following javac's code (Check, Resolve, TypeEnter, MemberEnter, Annotate, ConstFold,
the scoping of Attr) and the check finding what a reading missed (JN9a.2's list: a dozen javac
behaviours, each fixed in minutes once a fact named it). The re-estimate of the rest, by the same
ratio and from what JN9a showed:

- **The model behind the answers** (JFS75): the converter reads about 70 methods of javac's `Symbol`,
  `Type`, `Types` and `Attribute` on the answers (JFS66 (b).1); JN9a's answers are realized as
  javac's objects so that it can. Before the next tier, those reads move behind an interface over
  either model (as `tree` did for the trees), the native model being `decls`' symbols and types: 1-2
  days, checked as JN9a is (the forms byte for byte with the native model for the scope and
  declarations tiers).
- **JN9b, members and expression types** (Attr's expressions, Types' subtyping, capture, lub and
  memberType, ConstFold over expressions, Operators): 6-10 days estimated, now 2-4: the declarations,
  the lookups (findVar, findField, member types, imports) and the walk with its environments exist;
  what is new is the expression typing and Types' relations. With it 69% of the lists (JFS66 (b).4).
- **JN9c, overload resolution** (Resolve's three phases, most specific): 3-5 estimated, now 1-2.
- **JN9d, inference** (Infer, DeferredAttr, ArgumentAttr; the declined `var` types, implicit lambda
  parameters and diamonds join it): 6-12 estimated, now 3-6, the open tail of §8 R6 unchanged.

So about 7-14 working days for the rest (JFS66 (b).6: 15-27 after JN9a), each tier checked fact by
fact by `attr-check` as JN9a's, its known differences empty.

### JN9a.9 Proposals

- **JFS74 The fact check in the gates.**
  - Options: (a) `bin/repr-java attr-check` (its default corpora: samples, baseline, jdk, javac) in
    `bin/gate --full`'s java chain after `repr-java-front`, on the tool the javac step built (built:
    `repr-java-attr`, about 7 minutes), jdk26u opt-in (`--corpus all`, about 22 minutes more); (b) out
    of every gate, a command and the Makefile's target; (c) jdk26u in `--full` too.
  - *Recommended: (a)*: it is the provider's check and every later tier's harness, at the cost of one
    more front-check; front-check then checks nothing attr-check does not (the same reads but the
    provider), and could leave the chain, but it is the check of the index and of javac's answers by
    position, which the provider's realization relies on.
- **JFS75 The model behind the answers.**
  - Options: (a) as built: the native answers realized as javac's objects, the converter reading
    javac's model on them (a class's members, a local's constant value, erasure, subtyping); (b) the
    converter's reads of the answers behind an interface over both models (`jtypes`, `resolve` and
    the converter's interop on `Symbol`, `Type`, `Types`, `Attribute`), `decls`' symbols and types
    the native model, the realization then gone for the native tiers; (c) (b) only with JN9b, the
    model's reads of types then asked by the new tier anyway.
  - *Recommended: (b), first*: the converter's 70-odd reads of the model are what a javac-free
    converter must answer whatever the tiers, and moving them is mechanical, checked by the forms;
    JN9b then plugs into the native model directly.
- **JFS76 The declined facts.**
  - Options: (a) as built: a `var` local's type, an implicit lambda parameter's, a diamond's type tree
    and an anonymous class of a diamond or of a qualified creation through an expression left to
    javac (28,585 facts in jdk26u, 0.2% of its facts); (b) the cases whose answer needs no inference
    answered now from the declarations (a `var` initialized by a literal, `new C(...)` of a non-generic
    class, a local; `o.new C()` with `o` a method's result whose declared return type is known): a
    part of the types tier taken early.
  - *Recommended: (a)*: the declined facts are the types and inference tiers' by nature, and an early
    part of them would be redone by JN9b and JN9d.
- **JFS77 Next.**
  - Options: (a) JFS75 (b), then JN9b (members and expression types), decided after it on its
    numbers; (b) stop at JN9a: the native declarations serve tools (an outline of a Java tree without
    javac, Java in the box without javac's world) and javac on Arbace stays the front end; (c) JN9b
    on javac's model (the realization extended to expression types), JFS75 later.
  - *Recommended: (a)*, if a javac-free converter is still the aim (JFS69 (b)); (b) otherwise: JN9a
    is complete on its own.
- **JFS78 The native declarations outside the check.**
  - Options: (a) as built: the provider runs only beside javac (`--attr native`, its answers checked);
    (b) also without javac, on the tools' host (`decls` and `scope` need no javac: `classfile`'s reader
    and the native parser): a command giving a Java tree's declarations (classes, members, flags,
    declared types, resolved names) as data, for tools that need Java's names without javac's world
    (an editor's, the box's); (c) (b) in the shipped program.
  - *Recommended: (a)* until such a consumer is named; (b) is a small command then (the walk and the
    registry run on the tools' host today, as the scratch scripts of JN9a.10 show).

### JN9a.10 Sources and scratch

- jdk26u at `baf63fbe42b8758448fee570a9d9bb914272d259` (`/root/jdk26u`):
  `src/jdk.compiler/share/classes/com/sun/tools/javac/comp/{Check,Resolve,TypeEnter,MemberEnter,Enter,Annotate,ConstFold,Attr,MatchBindingsComputer,Flow}.java`,
  `code/{Flags,Types,Symbol,Scope,Symtab,Attribute,Type,TypeAnnotationPosition}.java`
  (`Scope.FilterImportScope`, `SymbolImporter`: what an import brings), `jvm/ClassReader.java`
  (`adjustClassFlags`, `adjustMethodFlags`), `tree/TreeInfo.java` (`symbol`).
- The worktree's `.tmp/`: `jn9a/` (the tool builds' logs `build1.log`, `build2.log`, the base tool
  `arbace-javac.base`, the scratch scripts `diffs.ae` (a run's differences by fact), `walk.ae` and
  `anon.ae` (a unit's answers on the tools' host), `run-s1.sh`, the first runs `s1`, `mod`, `base`,
  `ac-*.log`, and `final/` (the final tool's checks: their logs, `summary.txt`, `attr.md`, `attr.edn`,
  `split.md`)), `repr-java-attr-check/`, `repr-java-front-check/`, `repr-java-jdk/`,
  `repr-java-check/`, `repr-java-parse-check/`, `repr-java-roundtrip/`, `jrt/`, `javac-vendor/`,
  `clojure-tests/`.

## Decided (2026-10-24): JFS74-JFS78

- JFS74 (a), as built: `bin/repr-java attr-check`'s default corpora in `bin/gate --full`'s java chain
  (`repr-java-attr`), jdk26u opt-in (`--corpus all`).
- JFS75 (b), first: the converter's reads of javac's model (`Symbol`, `Type`, `Types`, `Attribute`)
  behind an interface over both models, `decls`' symbols and types the native one.
- JFS76 (a), as built: the declined facts left to javac.
- JFS77 (a): JFS75 (b), then JN9b (members and expression types), decided after it on its numbers.
- JFS78 (a), as built: the provider only beside javac, inside the check, until a consumer is named.

## As built: JFS75 (b) (2026-10-24)

JFS75 (b), decided first by JFS77 (a): the converter's reads of the model behind attribution's answers
moved behind an interface over both models, on the branch `java-model` from main `d36a03c9`, in about
half a working day. `arbace.repr.java.model` is to the answers what `arbace.repr.java.tree` is to the
trees: every read `convert.ae` and `resolve.ae` made of javac's `Symbol`, `Type`, `Types` and
`Attribute` (JFS66 (b).1's 70-odd methods, 25 `Flags` bits) is now one of its functions, which
dispatches on its argument's model: javac's objects as before, or `decls`' symbols (maps by identity)
and types (data). With the native provider the scope and declarations tiers' answers reach the
converter **as native objects, not realized**: JN9a.6's realization (`nattr`'s `rl-type`,
`rl-compound`, `rl-value`, the constructed javac types, annotations and values) is gone, and the
converter reads the native model. **Every form of every corpus is unchanged**: the native trees with
the native provider give javac's trees' forms byte for byte in the samples, the baseline, jdk, javac
and jdk26u's 68 modules, every fact of the provider equal as at JN9a (6,660,391, none different), and
the javac front's forms are the committed ones (the three updates byte for byte, `check --jdk` as
`jdk26u.sha256`). Of the model's reads in a native conversion of jdk26u, **62.7% go to the native
model** (40.5M of 64.5M), the rest are reads of the higher tiers' answers (types, overloads,
inference), which stay javac's; of the reads of the native tiers' answers, **99.4% are answered
natively**, 0.6% (257,451) ask javac's symbol of the same declaration (a bridge, JFS75 (b).3). The JVM
ran only the frozen v4 jar, for javac's edge in `bin/javac-vendor convert`; Go only TamaGo's go1.27.1,
without cgo; the tools' host was the worktree's stage 0 (the seed's).

### JFS75 (b).1 What was built

| file | lines | what |
|---|---:|---|
| `arbace/repr/java/model.ae` | 836 | `arbace.repr.java.model`: 90 reads over either model: of symbols (kind, name, flags, `isStatic`, owner, a variable's type and erasure, a final local's constant, a method's declared and erased signature, its parameters, an annotation element's default, `isSignaturePolymorphic`, a class's member classes, fields, methods by name, type parameters, superclass and interfaces, a record's variable arity, `isMemberOf`, binary and simple names, the package, `outermostClass`), of types (tag, primitive, array and element, class type, type variable, wildcard and bound, intersection, `tsym`, type arguments, enclosing type, `allparams`, type annotations, being the class's own type, erasure with javac's exceptions (a primitive and String keep their type annotations, an array its own), the same erasure, subtyping of erasures, boxing, unboxing, Object, String, the closure's classes, javac's `toString` of an erased type) and of annotation values (a compound's class and values, retention, constants, classes, enum constants, arrays, nested annotations); the higher tiers' readings (instantiated method types, qualifiers' `skipTypeVars`, functional descriptors, expression constants) as javac-only functions, counted; each read counted by name and model with the native provider |
| `arbace/repr/java/convert.ae` | 3,159 (was 3,189) | every read of the model through `model` (no javac interop on an answer is left but `jt/object-type` and the int and boolean types it writes into results); identity of symbols by `identical?` on canonical symbols |
| `arbace/repr/java/resolve.ae` | 226 (was 212) | the pin decision's names and descriptors of either model (`internal`, `desc`, a native class's module by name); the class infos stay javac's (JFS75 (b).3) |
| `arbace/repr/java/attr.ae` | 533 (was 528) | the native answers given unrealized; javac's answer of a symbol of the unit's declarations (a higher tier's, or a declined fact's) given as its native twin's (`nattr/canon`); the unit's package a native symbol |
| `arbace/repr/java/nattr.ae` | 424 (was 377) | `canon`, the model's read counts (`read!`), a local's flags, `isMemberOf` of a native field (Symbol.isMemberOf, isSubClass, isAccessibleIn, hiddenIn), an annotation class's retention; the realization of types, annotations and values removed |
| `arbace/repr/java/decls.ae` | 1,651 (was 1,637) | a final local's constant value (was none: `local-constant` read an absent `:const-fn`), ConstFold's coercion for a constant variable (a boolean field's constant was none), and no folding of a long shifted by a long (ConstFold folds no `lushrl`: found in jdk26u, JFS75 (b).2) |
| `arbace/repr/java.ae`, `split.ae` | | the model's reads merged into the provider's outcomes (`repr-java-attr.edn`'s `:model`), their table in `attr.md` |

The identity of symbols is one model's: with the native provider a symbol of the unit's declarations
(a class, a method, a field, a local) reaches the converter as the native one whichever tier answered
it (`nattr/canon`: javac's symbol, by its declaration's twin, to the native answer of that tree), and
the model's reads that return symbols (owner, `tsym`, `outermostClass`, members) do the same, so the
converter's `identical?` and its maps keyed by symbols (locals, assigned variables, enclosing classes,
anonymous receivers) need no change. Javac's symbols of other classes (the platform's, the run's other
units') stay javac's; a relation of a native type with a javac type (the same erasure, a subtype) is
answered in the native model, the javac operand's classes named in the run's registry (`nativize`:
1.67M in jdk26u).

### JFS75 (b).2 The check

`bin/repr-java attr-check` is the check, as JN9a's: the forms of the native trees converted with the
provider, now read through the native model, against javac's trees'. Its first run on jdk26u found one
file different (`java.base`'s `MontgomeryIntegerPolynomialP256`: `final long HALF_LIMB_MASK = -1L >>> (64
- HALF_BITS_LIMB)` made a constant natively; javac's ConstFold has no case for `lushrl`, a long shifted
by a long, so it is none): a reading of a final local's constant that JN9a took from javac, now native.
Fixed in `decls` (`fold-binary`), every corpus equal again. On the final tool (key `db72d5ad...`), the host
shared:

| corpus | forms files | equal | facts equal / different / declined | wall (JN9a's) |
|---|---:|---:|---:|---:|
| `samples` | 17 | 17 | 1,618 / 0 / 12 | 6 s (3 s) |
| `baseline` | 189 | 189 | 156,291 / 0 / 14 | 64 s (59 s) |
| `jdk` | 911 | 911 | 585,764 / 0 / 2,469 | 181 s (174 s) |
| `javac` | 1,828 | 1,828 | 490,792 / 0 / 2,828 | 193 s (170 s) |
| `jdk26u` (`--corpus all`) | 13,788 | 13,788 | 5,425,926 / 0 / 28,585 | 1,378 s (1,336 s) |

`--corpus all` took 1,827 s against JN9a's 1,746 (5% more: the reads' counting, a `HashMap` update per
read, and the native model's readings, the host shared); the default corpora 444 s (JN9a about 7
minutes). The facts are JN9a's to the count but one `scope/refers` more in jdk26u (6,660,391 equal,
none different), and the lists the provider writes alone too (344,337 of jdk26u's 2,258,047, 15.2%):
what the converter asks did not change, only what reads the answers.

### JFS75 (b).3 The model's reads, measured

Each read of `model` is counted with the native provider by its name and how it was answered:
*native* (of a native object, in the native model), *javac* (of javac's object: an answer of a higher
tier, or of a class the unit does not declare met through one), *bridge* (of a native object, but
answered by javac's symbol of the same declaration). In the native conversions (`attr.md`'s last
table):

| corpus | reads | native | javac | bridge | native share | of the native objects' reads, native |
|---|---:|---:|---:|---:|---:|---:|
| samples | 14,889 | 10,543 | 4,306 | 40 | 70.8% | 99.6% |
| baseline | 1,913,256 | 1,151,215 | 757,483 | 4,558 | 60.2% | 99.6% |
| jdk | 6,572,756 | 4,321,740 | 2,227,670 | 23,346 | 65.8% | 99.5% |
| javac | 6,589,830 | 4,048,240 | 2,511,997 | 29,593 | 61.4% | 99.3% |
| jdk26u | 64,537,482 | 40,465,679 | 23,814,352 | 257,451 | 62.7% | 99.4% |

The javac third is the higher tiers' (in jdk26u): the instantiated method types' parameters and
results (1.69M), the qualifiers' types through their type variables (1.09M, `skipTypeVars`), the
expressions' types read for erasure, tags and boxing, the chosen methods' names, flags and erasures
when they are the platform's (a method of the unit is canonical, native), functional descriptors
(11,904), expression constants (40,278): JN9b's and JN9c's to answer.

The bridges, the reads of a native tiers' answer that still need javac, and why (jdk26u):

| read | bridged | why |
|---|---:|---|
| `member-of` (Symbol.isMemberOf of a method) | 197,077 | a method's hiding is by subsignature (Types.isSubSignature) and its inheritance into a class by overriding: the members tier's (a field's is native: 328,926) |
| `type` of a local | 32,102 | a `var`'s and an implicit lambda parameter's type is inferred, a multi-catch parameter's is the least upper bound of its alternatives: the inference and types tiers' |
| `binary-name` of a local or anonymous class | 14,800 | javac numbers local classes (`Outer$1Local`, `Outer$1`) as Attr meets them (Check.localClassName), speculative attribution included |
| `package-members` | 12,730 | `:pkg-classes`, once per unit: the classes of the unit's package as javac's ClassFinder lists them on every location (the class files' `Outer$Inner` names included, before their completion) |
| `supertypes` of an anonymous class of a diamond | 430 | its supertype is inferred (JFS76 (a): declined) |
| `const-value` of a `var` local | 312 | its type is inferred |

No read of a native answer constructs a javac object: a bridge finds javac's symbol of the same
declaration, by its twin or by name, as `attr`'s answers by position do. Statically, of `model`'s 90
reads, 83 have a native reading, 7 are javac-only (the higher tiers' types above) and 10 of the 83
bridge in the cases of the table (and a local class's owner, never met).

### JFS75 (b).4 What it means for JN9b

The converter is javac-free in its reading of the scope and declarations tiers' answers; what keeps it
on javac is now exactly the facts of the higher tiers (attr's) and these reads of their answers. JN9b
(members and expression types) plugs into the native model directly: its answers are native types and
symbols that `model` already reads, and the relations it needs (erasure, subtyping of erasures,
boxing, `isMemberOf` of fields, the closure, members by name) exist natively, checked by the forms.
Two gaps found on the way: `decls`' members lack TypeEnter's generated ones (a default or canonical
constructor, a record's accessors, `toString`, `hashCode`, `equals`, an enum's `values` and
`valueOf`), which the converter's counts of methods by name (an overloaded method reference, a
variable arity call's only method) read natively now; no corpus's forms depend on them (every forms
file equal), but a unit that names its own enum's `valueOf` as a method reference would (JFS79). And
`isMemberOf` of a method bridges (above). The re-estimate (JFS93).

### JFS75 (b).5 The shipped program's change

None in code: `arbace/repr/java` is not in the shipped program (j2g's `embed.ae` leaves it out; javac's
tool embeds it, now with `model`: 221,864,096 bytes against JN9a's tool's 221,032,608 here), and
`go/`, `overlay/`, `jdk/forms`, `jdk/data` and `jdk/edge.edn` are unchanged. `model` is in the reader
hash (`bin/lib/repr-java/host.bash`'s `repr_java_reader_hash`: it reads the answers of javac's front
too), which moved from `16970302...` to `6f5f2988...`, written by the updates on the final tool:
`bin/clojure-tests --java-fixtures update` (15 forms files), `bin/jrt-convert update` (911 forms files,
10 data files, 911 of 911 equal to the committed forms), `bin/javac-vendor convert` (914 forms files;
its edge on the frozen v4 jar: 223 classes beyond `jdk/edge.edn`'s): every forms file byte for byte
as committed, only `:repr-java` and `:host` changed in `jdk/provenance.edn`, `jdk/javac/provenance.edn`
and `test/java-fixtures/provenance.edn`. `jdk/provenance.edn` is in the runtime key
(`bin/lib/version.bash`), so the version map the executable carries changes, as at JN9a. No re-seed.

### JFS75 (b).6 The checks

On the final tool (key `db72d5ad...`), one after another, the host shared:

- `bin/repr-java attr-check --corpus all`: JFS75 (b).2's table; every corpus's forms equal, every fact
  equal, the known differences none (1,827 s)
- `bin/repr-java check`: the baseline's 189 files as `baseline.sha256`, the samples' 11 and the
  module's 6 as `test/repr/java/expected` (28 s); `bin/repr-java parse-check`: every corpus's trees as
  javac's (the parser unchanged; 227 s)
- on the tool before the last commit's one-line fix in `decls` (key `4c1d4fbe...`; `decls` is read only
  with `--attr native`, so the javac front's forms these check are the same): `bin/repr-java
  front-check` every default corpus equal (418 s); `bin/repr-java check --jdk`: 68 modules, the 13,566
  forms files as `test/repr/java/expected/jdk26u.sha256` (627 s); `bin/repr-java roundtrip --corpus
  baseline,samples,jdk --text`: as the references (663 s)
- the updates of JFS75 (b).5: only the three provenance files' `:repr-java` and `:host` changed
- `make check-makefile`: passes
- `bin/gate` (essential): on the branch at `11c3733d` (this section's commit but this line), passes in
  19m56s (go-build 602 s, afresh; go-smoke, suite-go with no regression, oracle-go 53 known
  mismatches as recorded, diffcheck 23 known as recorded, goi-check 801 of 801, generated, j2g-edge
  verify: `jdk/edge.edn` the JVM's answer)
- not run: `bin/gate --full` (the user's rule), `bin/repr-java check --suite`, `bin/javac-vendor
  verify` and `classes` (the converted forms and javac's world are unchanged: the updates show the
  forms byte for byte)

### JFS75 (b).7 Proposals

(JFS80 to JFS89 are a concurrent branch's, j2g's array casts.)

- **JFS79 TypeEnter's generated members in the native declarations.**
  - Options: (a) as built: `decls`' members are the declared ones; the converter's counts of methods
    by name read them natively, and a unit naming a generated member (its enum's `valueOf` as a method
    reference, a record accessor's overloads) would differ from javac's (no corpus does); (b) the
    generated members added to `decls` as TypeEnter makes them (the default constructor,
    GENERATEDCONSTR; a record's canonical constructor, accessors, `toString`, `hashCode`, `equals`,
    GENERATED_MEMBER; an enum's `values` and `valueOf`; an anonymous class's constructor left to JN9c,
    its signature being the chosen superclass constructor's), checked by a new fact, a class's
    members by name and erased signature against javac's `members()`, in attr-check; (c) the counts
    bridged for a class that has generated members.
  - *Recommended: (b)*, a few hours at JN9b's start: the members are the declarations tier's, and
    JN9b's member lookup needs them.
- **JFS90 `isMemberOf` of a method.**
  - Options: (a) as built: bridged (197,077 reads in jdk26u, the most common bridge); (b) native in
    JN9b (Types.isSubSignature for a static method's hiding; the members tier's inheritance), the
    bridge then gone.
  - *Recommended: (b)*, within JN9b.
- **JFS91 javac's names of local classes.**
  - Options: (a) as built: a local or anonymous class's binary name is javac's (14,800 reads), as
    Check.localClassName numbers them in the order Attr meets them; (b) the numbering ported (Attr's
    order, which speculative attribution of lambdas' bodies also takes part in); (c) the converter
    no longer writing local classes' binary names (they appear only in its fallbacks and in the pin
    decision's class infos, javac's).
  - *Recommended: (a)* until JN9d (the order is the inference tier's world), then (b).
- **JFS92 A package's classes.**
  - Options: (a) as built: `:pkg-classes` from javac's package symbol (12,730 reads, one per unit);
    (b) listed natively from the registry (the platform module's package directory, the source
    directories, the run's units), as ClassFinder.fillIn lists them, checked as a fact.
  - *Recommended: (b)* when a javac-free read is the aim (JFS77's end), a small task; (a) until then.
- **JFS93 JN9b, re-estimated.**
  - JFS75 (b) took about half a day against JN9a.8's 1-2. JN9b (members and expression types) is now
    the facts alone: its answers are native types and symbols `model` reads, and the relations it
    needs exist natively (erasure, subtyping of erasures, boxing, the closure, members by name,
    fields' `isMemberOf`, checked by the forms); what is new is Attr's expression typing, Types'
    relations on generic types (capture, lub, memberType, asSuper with arguments), ConstFold over
    expressions and Operators. Re-estimate 2-3 days (JN9a.8: 2-4), JN9c 1-2, JN9d 3-6: about 6-11
    working days for the rest.
  - Options: (a) JN9b next, with JFS79 (b) and JFS90 (b) inside it, decided after it on its numbers;
    (b) stop at JFS75 (b) (JFS77 (b)'s reasons: the native declarations serve tools; javac on Arbace
    stays the front end).
  - *Recommended: (a)*, if a javac-free converter is still the aim (JFS69 (b)).
- **JFS94 The model's reads in the check.**
  - Options: (a) as built: counted whenever the native provider answers (5% of attr-check's time),
    tabled in `attr.md`, not judged; (b) judged: attr-check fails when a read bridges that is not in
    a known list (`test/repr/java/attr-check-known.edn`'s `:bridges`), so that a later tier sees its
    reads leave javac; (c) counted only with `--split`.
  - *Recommended: (b)* with JN9b, whose aim is to move the javac column; (a) until then.

### JFS75 (b).8 Sources and scratch

- jdk26u at `baf63fbe42b8758448fee570a9d9bb914272d259` (`/root/jdk26u`):
  `src/jdk.compiler/share/classes/com/sun/tools/javac/code/{Symbol,Types}.java` (isStatic,
  outermostClass, isMemberOf, isSubClass, hiddenIn, isAccessibleIn, getSuperclass, getInterfaces;
  erasure and eraseNotNeeded, isSignaturePolymorphic, getRetention, closure),
  `comp/{ConstFold,TypeEnter,Attr}.java` (no `lushrl`; addEnumMembers, addRecordMembersIfNeeded,
  the default constructor helpers; the lazy constant values of final locals).
- The worktree's `.tmp/`: `jfs75/` (`draft/`, the first edits; the tool builds' logs `build0.log`, the
  base tool, `build1.log`, `build2.log`; `t0`, `t1`, the samples read on the base and the evaluated
  checkout; `ac1`, `ac2`, attr-check on the base tool with the checkout's namespaces evaluated;
  `final1/`, the checks on the tool `4c1d4fbe...`; `final/`, on the final tool: logs, `summary.txt`),
  `repr-java-attr-check/` (`attr.md`, `split.md`), `repr-java-attr-check-jdk26u/` (the run that found
  JFS75 (b).2's file), `repr-java-front-check/`, `repr-java-jdk/`, `repr-java-check/`,
  `repr-java-parse-check/`, `repr-java-roundtrip/`, `jrt/`, `javac-vendor/`, `clojure-tests/`.

## As built: JFS65 (b) (2026-10-24)

JFS65 (b), j2g's checked casts to array types as a type switch on the array's Go type rather than
`Class.isInstance` (JN8.5: 17% of the native parser's time compiled), on the branch
`j2g-array-casts` from main `317927c0`, in about a working day, most of it builds and checks
queued on the shared host. A change to j2g and jrt, so to every compiled program that casts to an
array type: the shipped program changes (a re-seed, not done here).

### JFS65 (b).1 What it was

Every checkcast to an array type (javac's `(char[]) o`, the analyzer's erasure casts, c2j's casts of
a hinted local or argument, `^chars`, `^objects`, the dynamic classes' results, `dyn.ae`, and the
functional interfaces' adapters, `fromfn.ae`) was j2g's generic helper of jrt's package:

```clojure
((inst J2g_CastArray (* CharArray)) x (.ArrayClass Prim_char))
```

that is, at each call the array class (`ArrayClass`, an atomic load behind a call), then in
`J2g_CastArray` the nil test, `Class.isInstance` (the class's `IsInstance` hook, then
`isAssignableFrom` of `GetClass(x)`: a type switch, the assertion to `Object_I`, the interface call
`GetClass__Class`, which itself calls `ArrayClass`), then Go's assertion through the generic
dictionary. `instanceof` of an array type was `(.IsInstance_O__Z (.ArrayClass ...) x)` alike. A CPU
profile of `bin/repr-java parse --javac --time` over `java/util` on javac's tool (the base tool, built
from `317927c0`): `J2g_CastArray[go.shape.*uint8]` 15.3% cumulative, `GetClass` 9.2%, `IsInstance_O__Z`
13.9% (most of it under the casts), `ArrayClass` 3.2% flat at the call sites.

### JFS65 (b).2 What it is

jrt's own functions (`go/arbace/jrt/array.ae`, hand-written Go forms), one per array kind, named as
the classes' `C_Cast` and `C_InstanceOf` (J2G-SPEC §5.7, §5.9):

| Java | j2g's Go form | fast path | out of line |
|---|---|---|---|
| `(int[]) x` (and the 7 other primitive arrays) | `(jrt/IntArray_Cast x)` | Go's comma-ok assertion to `*IntArray`; `nil` for `nil` | `ClassCastException` (`arrayCastFailed`, `jrt.ClassCast`'s message) |
| `(Object[]) x` | `(jrt/ObjectArray_Cast x)` | the assertion to `*RefArray`: every reference array is an `Object[]` | the exception |
| `(T[]) x`, `T` another reference type (`String[]`, `int[][]`: `T` is `int[]`) | `(jrt/RefArray_Cast x T_class)` | the assertion and `Comp == T_class` | `refArrayCast`: `Class.isInstance` of `T[]`'s class (covariance: a `String[]` as `CharSequence[]`, a subclass's array as its superclass's), the exception |
| `x instanceof int[]`, `Object[]`, `T[]` | `IntArray_InstanceOf`, `ObjectArray_InstanceOf`, `RefArray_InstanceOf` | the same | `refArrayIsInstance`: `Class.isInstance` |

The fast paths are within gc's inlining budget (`-gcflags=-m=2` on the printed jrt: cost 80 of 80 for
`RefArray_Cast`, `RefArray_InstanceOf`, `ObjectArray_Cast`; 9 for the `_InstanceOf`s of a Go type;
`RefArray_Cast` was 84, not inlined, while it also tested `comp == Object_class`, which is why
`Object[]` has its own function, j2g deciding it statically). j2g's `arbace.j2g.model/array-test`
writes all of them (`code.ae`'s `cast-val` and `instance-val`, `dyn.ae`, `fromfn.ae`);
`J2g_CastArray` is gone from j2g's support file. The semantics are Java's as before: `null` casts to
any array type; a failed cast's message is HotSpot's, `class [I cannot be cast to class [C ([I and
[C are in module java.base of loader 'bootstrap')`, the same function making it; the JVM's
covariance by `Class.isInstance`, which the fast path never contradicts (an array whose component
class is `T`'s is a `T[]`; a `*RefArray` is an `Object[]`; a primitive array's Go type is its class).
The one Go type per array kind (§5.9) is what makes the switch decide: only a reference array of
another component class needs the class hierarchy.

The regression fixture `test/j2g/fixtures/FxArrays.ae` (`j2g.fixtures.FxArrays`, recorded on the
frozen jar by `bin/j2g-check --record-fixtures`): every cast and `instanceof` of 21 array types (the
8 primitive, `Object[]`, `Object[][]`, `String[]`, `CharSequence[]`, `Comparable[]`, `Number[]`,
`Integer[]`, `int[][]`, `long[][]`, `String[][]`, `CharSequence[][]`, a fixture class's array and its
subclass's) over 18 values (an array of each kind, a string, an `Integer`), the exceptions' messages
(but for a fixture class's loader, which the JVM recording names as its `DynamicClassLoader @hash`
and the Go build `'app'`), `null` cast to each, and a cast's result used (an element read, an
`Object[]`-typed `String[]` refusing an `Integer` with `ArrayStoreException`). It passes on the base
(`317927c0`'s j2g) and on the change: the semantics did not move.

### JFS65 (b).3 Speed

The host shared (load 11-37 during the runs), each pair alternated, the minimum of the runs:

| | base (`317927c0`) | JFS65 (b) | |
|---|---:|---:|---:|
| the native parser compiled in javac's tool, java.base (3,097 files, 46.0 M chars, one thread; `bin/repr-java parse --javac --time`, 3 runs each) | 49.0 s (0.94 M chars/s) | 37.3 s (1.23 M chars/s) | -24% |
| the same over `java/util` (370 files, 9.62 M chars, 2 runs each) | 11.1 s | 7.8 s | -30% |
| javac on Arbace's parse in the same runs (`JavacTask.parse`, java.base) | 5.3 s | 5.5 s | noise |
| javac on Arbace compiling java.base (`bin/javac-vendor classes jdk java.base`, one run each, base first; the JVM's javac in the same runs 28.6 s and 25.0 s) | 89.3 s (3.1 times the JVM's) | 76.9 s (3.1 times) | noise |
| the shipped program, hinted arrays through the evaluator (`aget`/`aset` on `^longs`, `^objects`, `^chars`, `areduce`, 100,000 elements; 3 runs each) | 7.6-65.5 ms | 6.0-52.0 ms | -19 to -22% |
| the same, `into-array` of 10,000 (arbace.core's, compiled) | 32.1 ms | 18.6 ms | -42% |
| the same, general workloads without array casts (`sort`, `reduce`, `assoc` into a map, `str`): user instructions over the script (`perf stat`) | 20.72 G | 20.72-20.79 G | none |

With the change the casts leave the profile: over `java/util` `LongArray_Cast` 0.6% and
`RefArray_Cast` 0.7% where not inlined, `GetClass` 1.6% (from 9.2%), `IsInstance_O__Z` below the
listing. The native parser compiled is now about 7 times javac on Arbace's parse (from 9, JN8.5).
`bin/j2g-perf` does not apply: it lives on the branch `arbace-for-golang` only (J2G-NOTES.md, 5), its
workloads timed against the JVM's.

### JFS65 (b).4 The shipped program's change

j2g's output changes wherever a program casts to an array type or tests one, and jrt gains the 19
functions: the shipped executable 151,773,344 bytes (the gate's build) against 151,711,904
(+61,440), javac's tool 217,956,512 against 217,764,000 (+192,512: the fast paths inlined at their
sites). `bin/lib/version.bash`'s runtime key changes (`go/`, `arbace/j2g` in the build's keys), so
the seed is stale for this change: a re-seed carries it (not done here).

### JFS65 (b).5 The checks

- `bin/j2g-check` (amd64): 9,036 steps, 8,972 pass, 17 fail, 47 need core, the 17 the same steps as
  the base's (`317927c0`, run in `basetree/`): its slice's environment (`Properties.setProperty` not
  translated, `lib/tzdb.dat` missing: FxDates, FxStreams' print streams, FxCode's `tVarargs`, six
  oracle class cases initializing through them); FxArrays' 5 cases pass on both. `test/j2g/check.ae`
  did not load on stage 0 (`arbace.pprint/pprint` unresolved: the report's pprint without its
  require); it requires `arbace.pprint` now.
- `bin/diffcheck`: 12,749 of 12,772 cases agree, the 23 known (the Java pair 729 of 731): FxArrays
  interpreted and translated agree; FxArrays sorting first renumbered the two known fixture cases
  (`test/j2g/fixtures:2`, `:10` now `:3`, `:11`, the same `load-file`s), `test/diffcheck/known.edn`
  updated.
- `bin/javac-vendor check`: j2g translates javac's world, no failures (54 s).
- `bin/javac-vendor classes baseline samples jdk java.base` on the new tool: every corpus byte for
  byte as the JVM's javac, java.base also as the build's classes (248 s).
- `bin/repr-java parse-check` on the new tool: every corpus's trees equal (samples 14, baseline 183,
  jdk 838, javac 824, jdk26u 12,809, mutants 8,000, tests 37,797).
- `bin/gate` (essential): on the branch at `f2289818` (this section's commit but this list), passes
  in 9m57s (go-build cached from the diffcheck run's build of the same key; go-smoke, suite-go 777
  tests with no regression, oracle-go 53 known mismatches as recorded, diffcheck as above, goi-check
  801 of 801, generated, j2g-edge verify: `jdk/edge.edn` unchanged, j2g asks no new JDK class).
- not run: `bin/gate --full` (the user's rule), `bin/arbace-go --fixpoint`, the box.

### JFS65 (b).6 Proposals

- **JFS80 The other checked casts.**
  - Options: (a) as built: casts to classes and interfaces stay `C_Cast` (a Go assertion, already a
    type switch, J2G-SPEC §5.7); (b) the dynamic check of an interface's cast (`jrt.IsDynamic`, a
    header load) skipped where the static world shows no dynamic class can implement the interface;
    (c) c2j's repeated casts of one hinted local hoisted (one cast per binding, not per use: the
    parser's state and the lexer's arrays are cast at every `aget`).
  - *Recommended: (a)*: after this change no cast shows in the parser's profile above 1%; (c) when a
    profile shows the casts again (it is c2j's, and a cast is now a few instructions).
- **JFS81 The re-seed.**
  - Options: (a) the change carried by the next re-seed with whatever else is on main; (b) a re-seed
    of its own after the merge, so that the tools' host (stage 0) runs j2g, repr/go and goi at the new
    speed (their code casts arrays: repr/go's printer and goi's frames are `Object[]`-heavy).
  - *Recommended: (a)*: the tools' host gains as the program does, but nothing waits on it.
- **JFS82 A regression test of the speed.**
  - Options: (a) none, as built (the numbers here, measured by hand); (b) the parser's
    `parse --time` over java.base recorded in `parse-check`'s report and compared with a stored
    ceiling (a slowdown of more than, say, 30% failing it), noise permitting on a shared host.
  - *Recommended: (a)*: wall times on the shared host vary by ±20%, a ceiling would fail on load,
    not on a regression.

### JFS65 (b).7 Sources and scratch

- Go 1.27.1 (TamaGo's, `/root/tamago-go`): `cmd/compile`'s inlining budget (80) as `-m=2` reports it.
- The worktree's `.tmp/`: `jfs65/` (the tool builds' logs `base-build-java.log`, `new-build-java.log`,
  `new2-build-java.log`; the profiles `base-parse.pprof`, `new-parse.pprof`, `new2-parse.pprof`;
  `w.clj`, `w2.clj`, `w3.clj`, the workloads, `w.out`, `w2.out`, `perf*`; the checks' logs). The base
  (`basetree/`, `317927c0` as `git archive` gives it, with its own tool build; `base/arbace`, main's
  shipped executable of the same `aot.key`) was removed after the measurements, for the host's disk;
  `git archive 317927c0` and `bin/arbace-go --build-java` in it make it again.

## Decided (2026-10-24): JFS79-JFS82, JFS90-JFS94

The user decided as recommended:
- JFS79 (b): javac's generated members (default and canonical constructors, a record's members, an
  enum's `values`/`valueOf`) in `decls`, checked by a members fact, inside JN9b.
- JFS80 (a): the other casts as they are. JFS81 (a): the next batched re-seed carries JFS65 (b).
  JFS82 (a): no speed regression test.
- JFS90 (b): a method's `isMemberOf` native inside JN9b. JFS91 (a): local classes' names javac's until
  JN9d. JFS92: a package's classes native when a javac-free read is the aim ((a) until then).
- JFS93 (a): JN9b next, with JFS79 and JFS90 inside it. JFS94 (b): from JN9b, attr-check fails on a
  bridge outside a known list.

## As built: JN9b (2026-10-24)

JN9b (JFS69 (b)'s second tier, decided by JFS93 (a)), with JFS79 (b), JFS90 (b) and JFS94 (b) inside it,
on the branch `java-jn9b` from main `407ed654`, in about a working day. The native attribution provider
now answers, behind `arbace.repr.java.attr`, the **members tier** (the field, class or package a name in
code denotes, a field of an expression's type) and the **types tier** (every expression's type as javac's
Attr sets it: member types at the site's type arguments, capture conversion, the constants of constant
expressions, the operators Operators resolves, a call's and a constructor's instantiated signature and
variable arity element, a `var`'s type from its initializer), each answer checked against javac on
Arbace's for the same tree, fact by fact: `bin/repr-java attr-check --corpus all`, over the samples, the baseline, the jdk corpus, javac's sources and jdk26u's 68 modules **11,440,693 facts equal** (jdk26u alone 9,302,929; JN9a and JFS75 (b): 6,660,391), **none
different**, and the converter given the native answers writes **every corpus's forms byte for byte as
from javac's trees**. A call's type needs the method overload resolution chose (JN9c's tier): until it
is native the provider takes javac's choice, finds that method in the native model and computes the
rest itself; such an answer is counted as *leaned on javac* (1,090,446 of them, 903,905 in jdk26u) and its frame as javac's. The
provider alone now writes 63.4% of jdk26u's lists (JN9a: 15.2%), and of the model's reads in a
native conversion of jdk26u 97.4% are answered natively (JFS75 (b): 62.7%). javac's generated
members are in the native declarations, checked by a new fact (JFS79 (b)); a method's `isMemberOf` is
native (JFS90 (b), its 197,077 bridges gone); attr-check fails on a bridge outside
`test/repr/java/attr-check-known.edn`'s `:bridges` (JFS94 (b)). The JVM ran only the frozen v4 jar, for
javac's edge in `bin/javac-vendor convert`; Go only TamaGo's go1.27.1, without cgo; the tools' host was
the worktree's stage 0 (the seed's).

### JN9b.1 What was built

| file | lines | what |
|---|---:|---|
| `arbace/repr/java/expr.ae` | 761 | `arbace.repr.java.expr`, the members and types tiers: the symbol a name in code denotes (Resolve's findVar, findType and findIdentInType through the walk's environments; a field of an expression's type; an enum switch's case label, Attr.enumConstant; an array's `length`, Symtab's lengthVar), every expression's type as Attr's visit methods set it (`etype`, memoized per node with its declines: names with checkIdInternal's memberType at visitIdent's and visitSelect's sites and its capture in a value context, calls with adjustMethodReturnType's `getClass()` and array `clone()` and checkMethod's unchecked erasure, indexing, parentheses, assignments, compound assignments, unary and binary operators, casts, `instanceof`, conditionals with isBooleanOrNumeric and condType or their target, switch expressions with their yields', creations, arrays with their initializer's target, literals), the constants (`econst`, ConstFold's), the operators (Operators' helpers: unary and binary promotion, string promotion, the operator symbols as native method symbols), a call's and a constructor's instantiated type, a `var`'s type from its initializer or for-each element, javac's target (`pt`) from the walk's record; the overload tier's choice taken from javac (`method-of`, `nmethod`: javac's method found in the native model by owner, name and erased parameters, a member's clone with another owner, Symtab's array `clone()`), marked in `*leaned*` |
| `arbace/repr/java/types.ae` | 378 | `arbace.repr.java.types`, Types' relations on the native types: boxing, unboxing, the numeric promotions, the same type, subtyping with generics (containment, type variables' bounds, captured variables' bounds, intersections, arrays), memberType (at asOuterSuper's type arguments, erased for a raw site), capture conversion (fresh captured type variables, their upper bound the glb of the wildcard's and the formal's), glb, lub with one minimal candidate, Attr.condType |
| `arbace/repr/java/scope.ae` | 631 (was 593) | the walk records each expression's environment, role (javac's expected kind: a value, an assignment's target, a qualifier: what checkIdInternal captures), target (pt: a declaration's type, an assignment's left side, the method's return type, an array's element, a boolean or int context, a parent's, unknown for a lambda's body or a constructor's argument), a case label's switch, a for-each's loop, a call's tree for its method |
| `arbace/repr/java/attr.ae` | 602 (was 533) | the members tier's `sym`; `type-of` for `:types/expr`, `qualifier`, `constant`, `new`, `new-array`, `ident`; `operator`; `method-type`, `constructor-type` and `varargs-element` for a method without type parameters of its own; a `var` local's declared type; `symbol-of` and `constructor` give javac's chosen method in the native model (`native-method`); a fact's native answer that took javac's higher-tier answer recorded as `:leaned` and its frame marked javac's; a declined fact's reason kept with `REPR_JAVA_ATTR_DECLINED`; `note-result!` (below) |
| `arbace/repr/java/nattr.ae` | 606 (was 424) | the new facts' data (a name's symbol, a constant type, an operator, an instantiated method type), captured type variables as javac's `[:captured]`, the members fact (`check-members!`), a method's `isMemberOf` (JFS90: MethodSymbol.isInheritedIn, hiddenInInternal by static methods of the same erased parameters) |
| `arbace/repr/java/decls.ae` | 1,744 (was 1,651) | TypeEnter's generated members (JFS79, below), `member-clone` (Symbol.clone as Resolve's findVar and findFun use it for a static import's inherited member), a class file's thrown types from its Exceptions attribute, float constants beyond float's range folded unchecked |
| `arbace/repr/java/model.ae` | 872 (was 836) | the native readings of the types tier's answers: a constant type's tag and value, a captured variable, an instantiated method type's parameters and result; `member-of?` of a method native, also of javac's chosen method with a native class; a `var` local's type |
| `arbace/repr/java/classfile.ae` | 304 (was 299) | a method's Exceptions attribute |
| `arbace/repr/java/convert.ae` | 3,159 | `:types/result`'s classification (`attr/note-result!`, below) |
| `arbace/repr/java/split.ae` | 405 (was 396) | `:enter/members` in the catalogue; the leaned answers in `attr.md`; the bridges checked against the known list |
| `test/repr/java/attr-check-known.edn` | | `:bridges`, the reads allowed to bridge, each with its reason (JFS94 (b)) |
| `bin/lib/repr-java/front-check` | | its header |

### JN9b.2 The facts, the answers, the check

| fact | the native answer | data compared with javac's |
|---|---|---|
| `names/*` (ident, select, assigned, capture, hoist, refers, label, resource, local-names) of a name that is no local | the field (a static import's inherited one: its clone with the imported class as owner, as Resolve's), class or package the name denotes; a field of a qualifier expression's type (captured, through type variables), an array's `length` | a field by owner and name, a class as a declaration's data, a package by name |
| `types/expr`, `types/qualifier`, `types/new`, `types/new-array`, `types/ident` | the expression's type as Attr sets it (`.type`) | the type as data, type arguments, enclosing types, type annotations and captured variables (`[:captured]`) included |
| `types/constant` | the type and its constant (ConstFold over the constant types: names of constant variables only through a type, as isStaticReference) | the type and the constant as data |
| `types/operator` | the operator Operators resolves for the operand types | its name, parameter and result types |
| `types/member-method-type`, `overload/declared-method-type`, `types/member-constructor-type`, `overload/declared-constructor-type` | a call's or creation's instantiated type: memberType of the chosen method at the site (an unqualified call's: the innermost enclosing class from which it is accessible; a qualifier's: captured, through type variables), erased by an unchecked call; not for a generic method or constructor (`infer/*`), a diamond, a qualified creation or an anonymous class's | parameter, result and thrown types |
| `types/member-varargs-element`, `overload/declared-varargs-element` | the element of the last parameter at the site, of a call javac resolved as of variable arity | the type |
| `scope/local-decl` of a `var` | its type: its initializer's (javac's checkLocalVarType), a for-each's element, when it has no captured variable to project | the declaration's data with its type |
| `enter/members` (new, JFS79) | the class's members, its generated ones included | each method by name, flags under MMASK and erased signature, each field by name and flags, each member class by name, as javac's `members()`; an anonymous class's declined (its constructor is the overload tier's) |

A call's type, a method type, a variable arity element and, through them, any expression containing a
call need the method overload resolution chose. Until JN9c that choice is javac's: the provider takes
the call's symbol from javac's twin (`TreeInfo.symbol`), finds it in the native model (by owner, name
and erased parameters; a member a static import or an interface reaches through a supertype as javac's
clone of it with that owner; an array's `clone()`) and computes the type itself (memberType at the site,
capture, unchecked erasure, `getClass()`'s `Class<? extends |T|>`). Such an answer is *leaned*: counted
in `attr.md` beside the equal ones, and its frame marked as javac's (`:provider/javac`), so the lists
it writes do not count as the provider's alone. A poly expression in an argument (a conditional, a
switch expression, an array initializer whose target is the formal) is declined: its target is the
formal as deferred attribution checks it (the overload and inference tiers'). What needs inference is
declined as JFS76 (a) decided: a generic method's or constructor's call, a diamond, a lambda or method
reference, a member reached through an anonymous class of a diamond, a `var` whose initializer's type
has captured variables (its upward projection); also a least upper bound with several minimal
candidates or merged type arguments (lci), and a multi-catch parameter's type.

The converter's model now reads the native answers of these tiers directly (constant types, captured
variables, instantiated method types); and `symbol-of` and `constructor`, the overload tier's facts,
give javac's chosen method *in the native model* (`attr/native-method`), so that the model's reads of
it (its name, flags, owner, erasure, `isSignaturePolymorphic`, `isMemberOf`) are native too; the fact
itself stays javac's.

What javac does that the corpora showed and the provider follows (each found by a fact that differed,
then fixed): a local's uses carry its declared type without its declaration's TYPE_USE annotations
(TypeAnnotations puts them on the symbol after the body's attribution); an enum switch's case label is
the enum's constant (Attr.enumConstant), not a name in scope; a qualifier (a selection's, a call's, a
method reference's) is attributed as KindSelector VAL_TYP and so not captured, the selection's site is;
an indexed array's element is never captured (visitIndexed captures where no value is expected, and
KindSelector VAR contains VAL); a class file's thrown types are its Exceptions attribute's when its
Signature has none; a static import's inherited field and method, and an interface's Object method, are
javac's clones with the importing class or the interface as owner; a member inherited through an
anonymous class of a diamond is at the inferred supertype's arguments (declined); a poly argument's
type is the formal's captured (declined); a float constant may be infinite (`Float.POSITIVE_INFINITY`).

### JN9b.3 JFS79 (b): javac's generated members

`decls`' members of a source class now hold what TypeEnter generates, as native method symbols without a
tree (their flags checked as MemberEnter checks a method's): the default constructor of a class without
one (`GENERATEDCONSTR`, the class's access, an enum's `PRIVATE`); an enum's `values()` and
`valueOf(String)` (`addEnumMembers`); a record's canonical constructor when none is declared
(`RecordConstructorHelper`: `GENERATEDCONSTR | RECORD`, `VARARGS` after a variable arity component), its
`toString()`, `hashCode()`, `equals(Object)` (`PUBLIC | RECORD | FINAL | GENERATED_MEMBER`) and the
accessors of components that declare none (`PUBLIC | GENERATED_MEMBER`). An anonymous class's
constructor is the chosen superclass constructor's (the overload tier's), left out. The new fact
`enter/members` compares every class declaration's members with javac's `members()`.

### JN9b.4 JFS94 (b): the bridges judged

`test/repr/java/attr-check-known.edn`'s `:bridges` lists the model's reads that may answer a native
object by javac's symbol of the same declaration, each with its reason: `binary-name` (a local class's
numbering, JFS91 (a)), `package-members` (JFS92 (a)), `type` (an implicit lambda parameter's, a
multi-catch parameter's, a `var`'s of a poly or captured initializer), `const-value` (such a `var`'s),
`supertypes` (an anonymous class of a diamond, JFS76 (a)). attr-check prints a read bridged beyond them
as `beyond the known bridges` and fails. `member-of`, JFS75 (b)'s most common bridge, is not among them:
a method's `isMemberOf` is native (JFS90 (b)), and so is javac's chosen method's with a native class.

### JN9b.5 `:types/result`

The converter's decisions by a converted operand's type (an unboxing, a cast, a widening: `atype`,
`coerce`, `test-form`) were counted as javac's facts whenever the native provider was on, whatever the
type's source (JN9a's measure). They now count by the type they read (`attr/note-result!`): javac's when
it is javac's answer (a javac type, but a primitive one, which the converter also makes itself), else
the native tiers'. The lists' share below counts so; JN9a's 15.2% counted every such decision as javac's.

### JN9b.6 The check: `bin/repr-java attr-check`

`bin/repr-java attr-check [--corpus C,...]` is JN9a's check (JN9a.3) with the new facts; it fails, beside a
fact that differs or a forms file, on a bridge outside the known list (JN9b.4). On the final tool (key
`6a667178...`), the host shared:

| corpus | forms files | equal | facts equal / different / declined (leaned on javac) | wall (JFS75 (b)'s) |
|---|---:|---:|---:|---:|
| `samples` | 17 | 17 | 2,304 / 0 / 56 (104) | 3 s (6 s) |
| `baseline` | 189 | 189 | 271,813 / 0 / 242 (25,741) | 80 s (64 s) |
| `jdk` | 911 | 911 | 976,158 / 0 / 8,497 (74,022) | 205 s (181 s) |
| `javac` | 1,828 | 1,828 | 887,489 / 0 / 12,164 (86,674) | 214 s (193 s) |
| `jdk26u` (`--corpus all`) | 13,788 | 13,788 | 9,302,929 / 0 / 84,239 (903,905) | 1,586 s (1,378 s) |

`--corpus all` took 2,092 s against JFS75 (b)'s 1,827 (14% more: the new facts computed and compared,
the host shared); the default corpora 502 s (JFS75 (b): 444 s). By tier in jdk26u (equal / different /
declined, leaned):

| tier | jdk26u | JN9a, JFS75 (b) |
|---|---:|---:|
| syntax | 536,151 / 0 / 0 | 536,151 / 0 / 0 |
| scope | 2,726,388 / 0 / 9,974 (6,933) | 2,715,734 / 0 / 20,628 |
| declarations | 2,251,160 / 0 / 12,675 | 2,174,040 / 0 / 7,957 |
| members | 556,242 / 0 / 569 (2,796) | (javac's) |
| types | 2,680,096 / 0 / 53,987 (341,284) | (javac's) |
| overload | 552,892 / 0 / 7,034 (552,892): the declared signatures and elements, after javac's choice | (javac's) |

By fact, in jdk26u (equal / declined; none different): the members tier's `names/ident` 361,894 / 0,
`names/select` 191,601 / 568, `names/hoist` 2,747 / 1; the types tier's `types/qualifier` 1,099,396 /
20,872, `types/expr` 911,910 / 6,523, `types/operator` 341,118 / 2,300, `types/new` 143,721 / 15,169,
`types/constant` 70,805 / 343, `types/new-array` 39,716 / 0, `types/member-method-type` 69,263 / 8,136,
`types/member-constructor-type` 3,511 / 629, `types/member-varargs-element` 656 / 15; the overload tier's
signatures after javac's choice `overload/declared-method-type` 472,137 / 6,870,
`overload/declared-constructor-type` 73,518 / 41, `overload/declared-varargs-element` 7,237 / 123; the
new `enter/members` 77,120 / 4,718 (the anonymous classes); `scope/local-decl` 658,634 / 2,242 (JN9a:
647,980 / 12,896: the `var`s whose initializer's type is the types tier's); the rest as JN9a's.

### JN9b.7 The numbers: facts, lists, model reads, bridges

| | samples | baseline | jdk | javac | jdk26u |
|---|---:|---:|---:|---:|---:|
| lists the provider wrote alone | 73.2% | 61.5% | 70.4% | 60.7% | 63.4% (1,432,044 of 2,258,047; JN9a 15.2%) |
| model reads native / javac / bridge | 13,654 / 1,144 / 33 | 1,973,975 / 6,110 / 378 | 6,398,590 / 186,309 / 4,339 | 6,539,360 / 240,809 / 5,451 | 64,274,174 / 1,660,053 / 48,519 |
| native share of the model's reads (JFS75 (b)) | 92.1% (70.8%) | 99.7% (60.2%) | 97.1% (65.8%) | 96.4% (61.4%) | 97.4% (62.7%) |

The lists' share counts a list as the provider's when no fact its writing asked was javac's answer,
nor a leaned one (JN9b.2), and the converter's operand-type decisions by the type they read (JN9b.5).
What is left of javac in jdk26u is the overload tier's choice (the methods of calls and creations,
arity, method references' kinds, the anonymous classes' superclass constructors: every list that calls a
method), the inference tier's facts and what JN9b declines (below).

The model's javac reads in jdk26u (1.66M, 2.6%) are of the higher tiers' answers the provider declines
(an inferred call's or lambda's types, a diamond's, functional descriptors: `descriptor` reads are all
javac's) and of javac's symbols those answers name. The bridges went from 257,451 to 48,519, all of the
known list: `type` 19,327 (implicit lambda parameters, multi-catch parameters, `var`s of poly or
captured initializers), `binary-name` 15,720 (JFS91), `package-members` 12,730 (JFS92), `supertypes` 430,
`const-value` 312; `member-of` (197,077) is gone (JFS90).

What the provider declines in jdk26u, 84,239 facts (0.9% of those it is asked), is the next tiers': a
sample of 6,137 declines recorded with their reasons (`REPR_JAVA_ATTR_DECLINED`, the jdk and javac
corpora; a decline's reason is the first one met, so an expression over a `var` local declines for the
local), by reason:

| reason | declines | the tier it waits for |
|---|---:|---|
| a `var` local's or an implicit lambda parameter's type, inferred | 1,678 | JN9d |
| a diamond | 1,524 | JN9d |
| JN9a's (an anonymous class's members, a diamond's type tree, an implicit lambda parameter) | 997 | JN9c, JN9d |
| a generic method's call or type | 890 | JN9d |
| a lambda's or method reference's type | 351 | JN9d |
| a member through an anonymous class of a diamond | 296 | JN9d |
| a poly argument's target (deferred attribution) | 168 | JN9c, JN9d |
| a name the members tier does not resolve (a package-qualified name in an expression) | 90 | JN9b's tail |
| a qualified creation, its created class | 78 | JN9b's tail |
| a `var`'s upward projection, an unknown target, a multi-catch union's lub, other | 65 | JN9b's tail, JN9d |

### JN9b.8 The shipped program's change

None in code: `arbace/repr/java` is not in the shipped program (j2g's `embed.ae` leaves it out; javac's
tool embeds it, now with `expr` and `types`: 224,006,304 bytes against JFS75 (b)'s tool's 221,864,096),
and `go/`, `overlay/`, `jdk/forms`, `jdk/data` and `jdk/edge.edn` are unchanged. The reader hash
(`bin/lib/repr-java/host.bash`'s `repr_java_reader_hash`: `attr`, `convert`, `model` changed) moved from
`6f5f2988...` to `079c57b8...`, written by the updates on the final tool (key `6a667178...`): `bin/clojure-tests
--java-fixtures update` (15 forms files), `bin/jrt-convert update` (911 forms files, 10 data files),
`bin/javac-vendor convert` (914 forms files; its edge on the frozen v4 jar: 223 classes beyond
`jdk/edge.edn`'s): every forms file byte for byte as committed, only `:repr-java` and `:host` changed in
`jdk/provenance.edn`, `jdk/javac/provenance.edn` and `test/java-fixtures/provenance.edn`. The new
namespaces `expr` and `types` are not in the reader hash, as JN9a's `decls`, `scope` and `nattr` are not:
they change no forms of a read with javac's attribution. `jdk/provenance.edn` is in the runtime key, so
the version map the executable carries changes, as at JFS75 (b). No re-seed.

### JN9b.9 The checks

All on the final tool (key `6a667178...`), one after another, the host shared:

- `bin/repr-java attr-check --corpus all`: JN9b.6's table; every corpus's forms equal and every fact
  equal, no bridge beyond the known (2,092 s)
- `bin/repr-java front-check`: every default corpus equal (395 s); `bin/repr-java parse-check`: every
  corpus's trees as javac's (the parser unchanged; 179 s)
- `bin/repr-java check`: the baseline's 189 files as `baseline.sha256`, the samples' 11 and the
  module's 6 as `test/repr/java/expected` (27 s)
- `bin/repr-java check --jdk`: 68 modules converted, 0 with failures, the 13,566 forms files as
  `test/repr/java/expected/jdk26u.sha256` (572 s)
- `bin/repr-java roundtrip --corpus baseline,samples,jdk --text`: as the references (591 s)
- the updates of JN9b.8: only the three provenance files' `:repr-java` and `:host` changed
- `bin/gate` (essential): on the branch at `2b69672d` (the code and the provenance as final; this
  section's commits change only `doc/`), passes in 18m21s (go-build 505 s, afresh; go-smoke, suite-go
  with no regression, oracle-go 53 known mismatches as recorded, diffcheck 12,763 of 12,786 cases agree
  with the 23 known, goi-check 801 of 801, generated, j2g-edge verify: `jdk/edge.edn` the JVM's answer)
- not run: `bin/gate --full` (the user's rule), `bin/repr-java check --suite`, `bin/javac-vendor
  verify` and `classes` (the converted forms and javac's world are unchanged: the updates show the
  forms byte for byte)

### JN9b.10 What it means for the next tiers

JN9b took about a working day of work (the elapsed time was longer: the host's two heavy slots were
shared with four other branches) against JFS93's 2-3 days, the port again following javac's code (Attr's
visit methods, checkIdInternal, Operators, Types' memberType and capture) and the check finding what a
reading missed (JN9b.2's list: ten javac behaviours, each a fact that differed). What it leaves:

- **JN9c, overload resolution** (Resolve's three phases, applicability by strict, loose and variable
  arity invocation, most specific, `findFun`'s enclosing classes and static imports; the anonymous class's
  constructor; method references' methods and kinds): the provider already finds javac's choice in the
  native model and computes everything after it, so JN9c is the choice itself, checked as the `:overload/*`
  facts plus the leaned answers turning native. The candidates' applicability needs the arguments' types
  (JN9b's) and, for poly arguments (lambdas, generic calls, conditionals in an argument), deferred
  attribution: JN9c can choose natively wherever no argument is a poly expression and decline the rest to
  JN9d. Re-estimate 1-1.5 days (JFS93: 1-2).
- **JN9d, inference** (Infer, DeferredAttr, ArgumentAttr: generic methods' and diamonds' instantiation,
  lambda and method reference targets, poly arguments, `var` projections, local classes' numbering by
  Attr's order, JFS91): the declined facts of every tier are its, and with them the lists the provider
  cannot write alone. Re-estimate 3-5 days (JFS93: 3-6), the tail of §8 R6 unchanged.

### JN9b.11 Proposals

- **JFS95 The lean on javac's overload choice.**
  - Options: (a) as built: a call's type, signature and element computed natively after javac's chosen
    method, the answer counted as leaned and its frame as javac's; (b) the same, but leaned answers counted
    as the provider's in the lists' share (the measure then says what the types tier alone would write);
    (c) declined until JN9c.
  - *Recommended: (a)*: the answers are checked fact by fact (none differs), and the share stays honest;
    JN9c then turns the leaned answers native without touching them.
- **JFS96 The converter's operand-type decisions in the measure** (JN9b.5).
  - Options: (a) as built: `:types/result` counted by the type it reads; (b) JN9a's: always javac's when
    the provider is on.
  - *Recommended: (a)*: (b) made every list deciding a coercion javac's whatever the type's source, so
    the lists' share could not move with the types tier.
- **JFS97 javac's chosen methods in the native model.**
  - Options: (a) as built: `symbol-of` and `constructor` give the method javac chose as the native model's
    (the fact still javac's), so the model's reads of it are native (in the baseline 98.9% of the model's
    reads native against 90.1% without); (b) javac's symbols, as before.
  - *Recommended: (a)*: the forms are unchanged and JN9c replaces only the choice.
- **JFS98 What JN9b declines.**
  - Options: (a) as built: left to the next tiers (poly arguments, generic calls and constructors,
    diamonds, lambdas and method references, `var` projections and members through anonymous diamonds:
    JN9d; nothing to JN9c but the choice itself) and a lub with several minimal candidates or merged
    arguments, a multi-catch parameter's union (the types tier's, rare); (b) the full lub (lci, the
    intersection of candidates) and the union types now, a few hours.
  - *Recommended: (a)*: the lub's remaining cases are a few hundred facts; JN9d's lub of inference
    bounds will need the full one, and it is built there once.
- **JFS99 The members fact.**
  - Options: (a) as built: `enter/members` only checked (at each class declaration), the converter's
    counts of methods by name reading the native members; (b) asked by the converter where it counts
    methods by name, a fact of its own sites.
  - *Recommended: (a)*: the converter's reads are checked by the forms, the members by the fact.
- **JFS100 The declines' reasons in the check.**
  - Options: (a) as built: kept in the diffs with `REPR_JAVA_ATTR_DECLINED` (a debugging aid); (b) always
    summed by fact and reason in `attr.md` (a counter per reason, no positions).
  - *Recommended: (b)*: the declines are the next tiers' work list, and a table of them costs little.
- **JFS101 JN9c next.**
  - Options: (a) JN9c (overload resolution, re-estimated 1-1.5 days, JN9b.10), then JN9d (3-5 days):
    about 4-6.5 working days for the rest (JFS93: 4-8 after JN9b); (b) stop at JN9b: the provider gives a
    javac-free front end everything but the method choice and inference, 63.4% of jdk26u's lists alone.
  - *Recommended: (a)*, if a javac-free converter is still the aim (JFS69 (b)); JN9b's lean is exactly
    what JN9c removes.

### JN9b.12 Sources and scratch

- jdk26u at `baf63fbe42b8758448fee570a9d9bb914272d259` (`/root/jdk26u`):
  `src/jdk.compiler/share/classes/com/sun/tools/javac/comp/{Attr,Operators,TypeEnter,Resolve}.java`,
  `code/{Types,Symbol,Symtab,Kinds}.java` (KindSelector: VAR contains VAL), `jvm/ClassReader.java` (the
  Exceptions attribute's thrown types).
- The worktree's `.tmp/`: `jn9b/` (`draft/`, the patches and this section's drafts; the tool builds' logs
  `build0.log` (the base tool), `build1.log`, `build2.log` (the final tool); `diffs.ae`, `differ.ae`,
  `why.ae` (a run's declines, differences, decline reasons; `snap/`, a detached worktree of the branch
  for the checks run while the checkout changed, removed); `why.txt` (JN9b.7's decline reasons); `final/` (the final tool's checks: `run.sh`, their logs,
  `summary.txt`, `attr.md`, `attr.edn`, `split.md`, `gate.log`)), `repr-java-attr-check/`,
  `repr-java-front-check/`, `repr-java-jdk/`, `repr-java-check/`, `repr-java-parse-check/`,
  `repr-java-roundtrip/`, `jrt/`, `javac-vendor/`, `clojure-tests/`.

## Decided (2026-10-25): JFS95-JFS101

The user decided JN9b's proposals as recommended: JFS95 (a) (the lean on javac's overload choice kept
until JN9c, counted as leaned), JFS96 (a) (`:types/result` counted by the type it reads), JFS97 (a)
(javac's chosen method handed over as the native model's), JFS98 (a) (the declined facts left to JN9c
and JN9d), JFS99 (a) (`enter/members` a checked fact only), JFS100 (b) (the declines' reasons always
summed in `attr.md`), JFS101 (a) (JN9c next, then JN9d; started beside the REAP work).

## As built: JN9c (2026-10-25)

JN9c (JFS69 (b)'s third tier, decided by JFS101 (a)), with JFS100 (b) inside it, on the branch `java-jn9c`
from main `840e514b`, in about a working day (most of it the shared heavy slots' queue and three tool
builds). The native attribution provider now **chooses the method and the constructor of every call that
needs no inference**, as javac's Resolve chooses them: the candidates of the site's classes and interfaces
(`findMethod`, with its interface phases and the "concrete" rule), of the enclosing classes and the static
imports for an unqualified call (`findFun`), each checked for applicability by strict, loose and variable
arity invocation (the three phases of `lookupMethod`), the most specific of the applicable and accessible
ones (`mostSpecific`, `signatureMoreSpecific`, `mergeAbstracts`), errors as javac's kinds (absent,
inapplicable, inaccessible, ambiguous). The lean on javac's overload choice (JFS95 (a)) is gone wherever no
inference is needed: in jdk26u the answers leaned on javac went from **903,905 to 53,316**, and the facts the
overload tier answers natively from 552,892 (all leaned) to **2,001,666** (49,591 leaned), checked by
`bin/repr-java attr-check --corpus all`: over the samples, the baseline, the jdk corpus, javac's sources
and jdk26u's 68 modules **13,193,153 facts equal, none different**, no bridge beyond the known list, and the
converter given the native answers writes **every corpus's forms byte for byte as from javac's trees**. The
provider alone now writes **95.9% of jdk26u's lists** (JN9b: 63.4%). What needs inference is declined as
JFS76 (a) and JFS98 (a) decided (a poly argument, a generic candidate that passes the arity check, explicit
type arguments, a diamond, a signature polymorphic call), and then the provider leans on javac's choice as
before. The JVM ran only the frozen v4 jar, for javac's edge in `bin/javac-vendor convert`; Go only
TamaGo's go1.27.1, without cgo; the tools' host was the worktree's stage 0 (the seed's).

### JN9c.1 What was built

| file | lines | what |
|---|---:|---|
| `arbace/repr/java/overload.ae` | 794 (new) | `arbace.repr.java.overload`, the overload tier: Resolve's `isAccessible` of classes and members (`isProtectedAccessible`, `isInnerSubClass`, `notOverriddenIn` with Types' `implementation`, `isDerivedRaw`), `rawInstantiate` without inference (`resolveMethodCheck`'s `argumentsAcceptable` and `checkArg`, `MethodResultInfo.check`'s capture of the upper bound, `MethodCheckContext.compatible`: `isSubtypeUnchecked` strictly, `isConvertible` loosely), `selectBest`, `mostSpecific` (`signatureMoreSpecific`, `adjustArgs`, `MostSpecificCheck`, the same signature's rules: bridges, the base symbol, overriding, abstract), `ambiguityError` and `mergeAbstracts` (Types' with `MostSpecificReturnCheck` and Check's `intersect` of the thrown types), `findMethodInScope` and its `LookupFilter`, `findMethod` (`superclasses`, `InterfaceLookupPhase`, the interfaces' `closure` ordered by `rank` and `precedes`, the concrete rule, an interface's Object methods as clones), `findFun` (the enclosing classes, then the single and on-demand static imports in `SymbolImporter`'s order, `staticImportFilter`, `importAccessible`), `lookupMethod`'s phases (`shouldStop`, `mergeResults`), `resolveMethod`, `resolveQualifiedMethod`, `resolveConstructor`; Types' `hasSameArgs`, `isSubSignature`, `overrideEquivalent`, `returnTypeSubstitutable`, `resultSubtype`, `covariantReturnType`; Symbol's `overrides`, `isOverridableIn`; TypeEnter's `AnonClassConstructorHelper` (an anonymous class's constructor) |
| `arbace/repr/java/expr.ae` | 919 (was 761) | `choice`: a call's resolution (APPLY: unqualified, qualified at the qualifier's captured type through its type variables, `this(...)` and `super(...)` with select-super; NEWCLASS at the created type), over the arguments' standalone types (ArgumentAttr's: a poly argument has none and the call is declined), memoized with its lean as `etype`; `method-of`, `ctor-of`, `call-varargs?`: the choice, else javac's, leaned; `anon-super-choice` (Attr.visitNewClass: the superclass constructor at the created class type, select-super; Object's for an interface) and `anon-ctor-choice`; `ctor-method-type` over the native constructor |
| `arbace/repr/java/attr.ae` | 649 (was 602) | `choice-fact`: the overload tier's facts native, checked against javac's (`symbol-of` of a call's method, `constructor`, an anonymous class's own and `anon-super-ctor`), an error javac would report as data (`[:error kind]`, a difference); `varargs-element`: whether a call is of variable arity and its element native for calls and creations (`:overload/arity` included) |
| `arbace/repr/java/decls.ae` | 1,764 (was 1,744) | ClassReader's `readMethod`: a class file interface's method with a body, not static or private, is DEFAULT \| ABSTRACT and its interface DEFAULT; a member inner class's constructor read from its descriptor without the enclosing instance's parameter (`adjustMethodParams`); `import-scope` public |
| `arbace/repr/java/types.ae` | 383 (was 378) | `same-type?`: type arguments compared as `containsTypeEquivalent` for `?` and `? extends Object` |
| `arbace/repr/java/nattr.ae` | 632 (was 606) | `nmethod-id`, `jmethod-id` (a chosen method as data: owner, name, erased parameters); `record!` counts each decline by its reason (JFS100 (b)); the provider's `:choices` |
| `arbace/repr/java.ae`, `arbace/repr/java/split.ae` | 484, 417 | the declines' reasons summed (`:why` in `repr-java-attr.edn`), `attr.md`'s table of declines by fact and reason (JFS100 (b)) |

### JN9c.2 The facts, the answers, the check

| fact | the native answer | data compared with javac's |
|---|---|---|
| `overload/method` (`symbol-of` of an APPLY's meth: a call's method, `this(...)`/`super(...)`'s constructor) | the method lookupMethod chooses, or an error | `[:method owner name erased-parameters]` (the owner a clone's: a static import's, an interface's Object method's), an error `[:error kind]` |
| `overload/constructor` (NEWCLASS) | the constructor resolveConstructor chooses at the created type; an anonymous class's own, TypeEnter's over the superclass constructor chosen | the same |
| `overload/anon-super-ctor` | the superclass constructor an anonymous class's constructor calls | the same |
| `overload/arity`, `overload/declared-varargs-element`, `types/member-varargs-element` (calls and, new, creations) | whether the choice was made in the third phase, and the element of the last formal at the site | the element type, nil for a call of fixed arity |

The facts after the choice (`overload/declared-method-type`, `types/member-method-type`, `types/expr` of a
call, and every fact over an expression with a call in it) are unchanged in their computation (JN9b's);
they are now native where the choice is, *leaned* only where it is javac's. A choice javac's model needs
(an array's `clone()`, of Symtab's `arrayClass`) is handed to the converter as javac's symbol, the fact
still checked. What javac does that the corpora showed and the provider follows (each found by a fact that
differed, then fixed):

- `findFun` stops at the innermost enclosing class that has a method of the name even when it is not
  applicable: `InapplicableSymbolError.exists()` is true (only `SymbolNotFoundError` and `AccessError` are
  not), so an inner class's inapplicable inherited method hides an applicable one of the outer class in
  that phase (`JTextComponent`'s accessible context's `firePropertyChange(String, int, int)`: javac takes
  `AccessibleContext`'s in the loose phase, not `JComponent`'s in the strict one).
- `isSameType` compares a class type's arguments by `containsTypeEquivalent`: `Map<?,?>` and
  `Map<? extends Object, ? extends Object>` are the same type, so a method declared with the one overrides
  one declared with the other (`Provider.putAll`, `Properties.replaceAll`): without it `notOverriddenIn`
  kept the overridden method and the call was ambiguous.
- ClassReader gives a class file interface's default method DEFAULT | ABSTRACT and its interface DEFAULT
  (the interface phases' DEFAULT_OK, the concrete rule and `mostSpecific`'s abstract rule read them), and
  reads a member inner class's constructor without the enclosing instance's parameter of its descriptor.

### JN9c.3 The check and the numbers

On the final tool (key `18d12685...`), the host shared, `bin/repr-java attr-check --corpus all` against
the base tool's run (key `abe3444d...`, main's sources, which reproduced JN9b's numbers to the fact):

| corpus | forms files equal | facts equal / different / declined (leaned on javac) | wall (base) |
|---|---:|---:|---:|
| `samples` | 17 of 17 | 2,448 / 0 / 76 (11; base 104) | 6 s (7 s) |
| `baseline` | 189 of 189 | 315,089 / 0 / 360 (221; base 25,741) | 77 s (76 s) |
| `jdk` | 911 of 911 | 1,096,768 / 0 / 16,598 (6,305; base 74,022) | 218 s (247 s) |
| `javac` | 1,828 of 1,828 | 1,027,107 / 0 / 23,923 (8,470; base 86,674) | 232 s (216 s) |
| `jdk26u` | 13,788 of 13,788 | 10,751,741 / 0 / 145,401 (53,316; base 903,905) | 1,839 s (1,501 s) |

The whole `--corpus all` took 2,378 s against the base's 2,056 s (16% more; jdk26u 22% more: the
resolution of every call, the host shared). The facts counted grew because the overload tier's are now
answered (before, `overload/method`, `overload/constructor`, `overload/arity` and `overload/anon-super-ctor`
were javac's, counted only in the split). By tier in jdk26u (equal / different / declined, leaned):

| tier | JN9c | JN9b (the base) |
|---|---:|---:|
| scope | 2,726,388 / 0 / 9,974 (360) | 2,726,389 / 0 / 9,974 (6,933) |
| members | 556,242 / 0 / 569 (18) | 556,242 / 0 / 569 (2,796) |
| types | 2,680,134 / 0 / 53,982 (3,347) | 2,680,096 / 0 / 53,987 (341,284) |
| overload | 2,001,666 / 0 / 68,201 (49,591) | 552,892 / 0 / 7,034 (552,892) |
| total | 10,751,741 / 0 / 145,401 (53,316) | 9,302,930 / 0 / 84,239 (903,905) |

By fact, jdk26u's overload tier: `overload/method` 721,217 / 51,575 declined (989 leaned: a call whose
choice is native but an argument's type needed javac's choice of a call inside it), `overload/arity`
646,496 (41,875 leaned), `overload/constructor` 78,241 / 9,274, `overload/anon-super-ctor` 2,052 / 307,
`overload/declared-method-type` 472,142 / 6,865 (5,336 leaned), `overload/declared-constructor-type`
73,544 / 15 (915), `overload/declared-varargs-element` 7,974 / 165 (410).

| | samples | baseline | jdk | javac | jdk26u |
|---|---:|---:|---:|---:|---:|
| lists the provider wrote alone | 87.5% (JN9b 73.2%) | 99.1% (61.5%) | 95.9% (70.4%) | 93.0% (60.7%) | 95.9% (2,165,829 of 2,258,047; JN9b 63.4%) |
| model reads native / javac / bridge | 13,657 / 1,141 / 33 | 1,974,065 / 6,020 / 378 | 6,409,655 / 175,291 / 4,339 | 6,540,650 / 239,650 / 5,451 | 64,300,046 / 1,634,167 / 48,519 |
| native share of the model's reads | 92.1% (92.1%) | 99.7% (99.7%) | 97.3% (97.1%) | 96.4% (96.4%) | 97.4% (97.4%) |

The model's reads moved little: JFS97 (a) had already handed javac's choice over as the native model's
method, so the reads of the chosen method were native before; what JN9c changes is whose choice it is. The
bridges are the known list's, unchanged (48,519 in jdk26u).

What the overload tier declines in jdk26u, by reason (JFS100 (b), `attr.md`'s new table; a decline's first
reason met): `overload/method` a generic candidate that passes the arity check (its applicability is
Infer's) 21,525, a `var` local's or an implicit lambda parameter's type in an argument 8,360, a lambda or
method reference argument 7,648, a generic method's call as an argument 6,995, a poly argument
(conditional, switch expression) 1,847, a call inside an anonymous class of a diamond 1,509, a signature
polymorphic call 1,317, a name the members tier does not resolve 693, a diamond argument 604, a
multi-catch parameter 429, a `var`'s upward projection 296, explicit type arguments 165, the rest under
200; `overload/constructor` a diamond 7,619 and its arguments' as above; `overload/anon-super-ctor` a
diamond 239. Every one is the inference tier's (JN9d) but the members tier's unresolved names and the
multi-catch parameters (JFS98 (a)).

### JN9c.4 JFS100 (b): the declines' reasons

`nattr/record!` counts each decline under `:why/<fact>` by its reason, the decline's message without the
names it gives (`decline-reason`); `repr-java-attr.edn` carries them as `:why`, `split attr` sums them and
`attr.md` ends with a table by fact and reason, per corpus. `REPR_JAVA_ATTR_DECLINED` still keeps the
declines' positions in the diffs (a debugging aid).

### JN9c.5 The shipped program's change

None in code: `arbace/repr/java` is not in the shipped program (j2g's `embed.ae` leaves it out; javac's
tool embeds it, now with `overload`: 226,652,320 bytes against the base tool's 225,738,912), and `go/`,
`overlay/`, `jdk/forms`, `jdk/data` and `jdk/edge.edn` are unchanged. The reader hash
(`repr_java_reader_hash`: `arbace/repr/java.ae` and `attr` changed) moved from `079c57b8...` to
`a2daae6b...`, written by the updates on the final tool: `bin/clojure-tests --java-fixtures update` (15
forms files), `bin/jrt-convert update` (911 forms files, 10 data files), `bin/javac-vendor convert` (914
forms files; its edge on the frozen v4 jar: 223 classes beyond `jdk/edge.edn`'s): every forms file byte for
byte as committed, only `:repr-java` and `:host` changed in `jdk/provenance.edn`, `jdk/javac/provenance.edn`
and `test/java-fixtures/provenance.edn`. `overload` is not in the reader hash, as `decls`, `scope`, `nattr`,
`expr` and `types` are not. `jdk/provenance.edn` is in the runtime key, so the version map the executable
carries changes, as at JN9b. No re-seed. The tools were built for amd64 only (`ARBACE_GO_ARCHES=amd64`).

### JN9c.6 The checks

All on the final tool, one after another, the host shared:

- `bin/repr-java attr-check --corpus all`: JN9c.3's table; every corpus's forms equal and every fact
  equal, no bridge beyond the known (2,378 s)
- `bin/repr-java front-check`: every default corpus equal (403 s); `bin/repr-java parse-check`: every
  corpus's trees as javac's (195 s)
- `bin/repr-java check`: the baseline, the samples and the module as expected (27 s)
- `bin/repr-java check --jdk`: 68 modules converted, 0 with failures, the 13,566 forms files as
  `test/repr/java/expected/jdk26u.sha256` (580 s)
- `bin/repr-java roundtrip --corpus baseline,samples,jdk --text`: as the references (777 s)
- the updates of JN9c.5: only the three provenance files' `:repr-java` and `:host` changed
- `bin/gate` (essential): on the branch at `1243821e` (the code and the provenance as final; this section's commit changes only `doc/`), passes in 27m17s (go-build 17m57s afresh, go-smoke, suite-go with no regression, oracle-go 52 known mismatches as recorded, diffcheck 12,789 of 12,801 cases agree with the 12 known, goi-check 801 of 801, java-check 428 of 428, generated, j2g-edge verify: `jdk/edge.edn` the JVM's answer, reap)
- not run: `bin/gate --full` (the user's rule), `bin/repr-java check --suite`, `bin/javac-vendor verify`
  and `classes` (the converted forms and javac's world are unchanged: the updates show the forms byte
  for byte)

### JN9c.7 What it means for the next tier

JN9c took about a working day against JN9b.10's 1-1.5 days; the port followed Resolve's code closely and
the check found three javac behaviours a reading had missed (JN9c.2), each a fact that differed. One cost
of the iteration is worth naming: the base tool loads a changed namespace from the checkout, but `decls`
defines the native symbols' type (`deftype Sym`), and a reloaded `decls` makes the compiled model's
`instance?` checks fail, so a change to `decls` needs a tool build (about 25 minutes) to be tried (JFS107).

What is left of javac in jdk26u's conversion is the inference tier's (JN9d: 53,316 leaned answers, the
declines of JN9c.3 and JN9b.7), the flow tier's few facts (`flow/case-completes`, `exhaustive`,
`unconditional-pattern`: 33,946 asked), whether a switch is a pattern switch (`types/pattern-switch`, 4,813), the anonymous creation's constructor type and the method
references' kinds and methods. JN9d (Infer's applicability and instantiation, DeferredAttr and ArgumentAttr
for the poly arguments, diamonds, lambdas and method references, `var` projections, local classes'
numbering): re-estimated 3-5 days (JN9b.10's), its first step the generic candidates' applicability, the
largest decline (21,525).

### JN9c.8 Proposals

- **JFS102 The generic candidates.**
  - Options: (a) as built: a call is declined when a candidate with type parameters of its own passes the
    phase's arity check (its applicability is Infer's), 21,525 calls in jdk26u, most of whose chosen method
    is not generic; (b) JN9d's first step: Infer's applicability for a candidate whose inference variables
    are bounded by the standalone argument types alone (no target, no poly argument), which decides most of
    them without the rest of inference; (c) an approximation now (a generic candidate's formals' erasure).
  - *Recommended: (b), first in JN9d*: it is Infer's own work and the inference tier needs it anyway; (c)
    would choose differently from javac where the bounds matter, and the check would only hide it.
- **JFS103 The anonymous class's constructor in its members.**
  - Options: (a) as built: the anonymous class's constructor made on demand for the `constructor` fact
    (`overload/anon-constructor`), not among the class's members, so `enter/members` still declines every
    anonymous class (4,718 in jdk26u); (b) decls' members of an anonymous class with that constructor,
    through a hook to the overload tier's choice of the superclass constructor, `enter/members` checked for
    them.
  - *Recommended: (b)*: a few hours, and the members fact then covers every class.
- **JFS104 An anonymous creation's constructor type.**
  - Options: (a) as built: `constructor-type` of a creation with a body stays javac's (`types/member-*`,
    `overload/declared-constructor-type` not asked natively there); (b) native: the anonymous constructor's
    type at the created type (TypeEnter's, its enclosing instance's type first for a qualified creation).
  - *Recommended: (b)*, with JFS103: the same constructor, its type follows.
- **JFS105 The errors.**
  - Options: (a) as built: javac's error kinds as data (absent, inapplicable, inaccessible, ambiguous,
    static), an error answer counted as a difference: the corpora compile, so none occurs, and any would
    be the port's bug; (b) also check the errors against javac's on invalid code (JC3's diagnostics corpus,
    the parser's mutants), the kinds and the candidates.
  - *Recommended: (a)* until a javac-free front end must report errors; (b) is then the check of that
    front end's diagnostics, a step of its own.
- **JFS106 The class files' members.**
  - Options: (a) as built: ClassReader's two behaviours the overload tier needed (default methods'
    flags, inner constructors' parameters) in `decls`, found by resolution; (b) a fact checking the
    platform classes' members (flags, erased signatures) against javac's ClassSymbols, as `enter/members`
    does the units' classes, for the classes the run reads.
  - *Recommended: (b)*: `decls`' class-file side was checked only through the facts that used it, and two
    divergences surfaced late; a members fact on the platform's classes is cheap and finds the rest.
- **JFS107 Iterating on `decls`.**
  - Options: (a) as built: a change to `decls` needs a tool build to be tried (its `deftype Sym` reloaded
    from the checkout is a new class the compiled model does not know); (b) the symbol type in a namespace
    of its own (`arbace.repr.java.sym`) that changes rarely, so `decls` reloads like the others.
  - *Recommended: (b)*: a mechanical move, and it saves a build per `decls` change in JN9d.
- **JFS108 Signature polymorphic calls.**
  - Options: (a) as built: declined (1,317 calls in jdk26u: `MethodHandle.invoke*`, `VarHandle`'s); (b)
    native: Infer's `instantiatePolymorphicSignatureInstance` (the parameters the arguments' types, the
    result the cast's or Object), a small part taken from JN9d.
  - *Recommended: (b), in JN9d*: it is Infer's, small, and with JN9d's poly checks at hand.
- **JFS109 Next.**
  - Options: (a) JN9d (inference: JFS102 (b) first, then poly arguments, diamonds, lambdas and method
    references, `var` projections, local classes' numbering), re-estimated 3-5 days, with JFS103, JFS104,
    JFS107 (b) and JFS108 (b) inside it; (b) stop at JN9c: the provider writes 95.9% of jdk26u's lists alone,
    javac answers inference.
  - *Recommended: (a)*, if a javac-free converter is still the aim (JFS69 (b)); JFS107 (b) first, as it
    makes JN9d's iteration cheaper.

### JN9c.9 Sources and scratch

- jdk26u at `baf63fbe42b8758448fee570a9d9bb914272d259` (`/root/jdk26u`):
  `src/jdk.compiler/share/classes/com/sun/tools/javac/comp/{Resolve,Attr,ArgumentAttr,Check,TypeEnter}.java`,
  `code/{Types,Symbol,Scope}.java`, `jvm/ClassReader.java` (`readMethod`, `adjustMethodParams`).
- The worktree's `.tmp/`: `jn9c/` (`build0.log`, `build1.log`, `build2.log`: the base, the intermediate and
  the final tool's builds; `base/`: the base tool's `attr-check --corpus all`, `base-all.log`, `attr.md`,
  `attr.edn`, `split.md`; `b1/`: the intermediate tool's run, with the two jdk26u differences of JN9c.2's
  first item; `s1.log`-`s3.log`: the runs on the base tool with the changed namespaces evaluated; `t1/`,
  `t2/`: the small reproductions; `diffs.ae`: a run's differences; `decls.jn9c.ae`: decls while it could
  not be reloaded; `final/`: the final tool's checks, `run.sh`, their logs, `summary.txt`, `attr.md`,
  `attr.edn`, `split.md`, `gate.log`), `repr-java-attr-check/`, `repr-java-front-check/`, `repr-java-jdk/`,
  `repr-java-check/`, `repr-java-parse-check/`, `repr-java-roundtrip/`, `jrt/`, `javac-vendor/`,
  `clojure-tests/`.

## Decided (2026-10-25): JFS102-JFS109

The user decided JN9c's proposals as recommended: JFS102 (b) (generic candidates: Infer's applicability
from standalone argument types, JN9d's first step), JFS103 (b) (anonymous classes' constructors in decls'
members), JFS104 (b) (the anonymous creation's constructor type native, with JFS103), JFS105 (a) (error
kinds as data), JFS106 (b) (a fact checking the platform classes' members against javac's ClassSymbols),
JFS107 (b) (`deftype Sym` in a namespace of its own, so decls reloads from the checkout), JFS108 (b)
(signature-polymorphic calls native, in JN9d), JFS109 (a) (JN9d next, JFS107 first; started now, beside
the REAP work).

## As built: JN9d (2026-10-25)

JN9d (JFS69 (b)'s fourth tier, decided by JFS109 (a)), with JFS102 (b), JFS103 (b), JFS104 (b), JFS106
(b), JFS107 (b) and JFS108 (b) inside it, on the branch `java-jn9d` from main `9c47edcd`, in about a
working day and a half (much of it the shared heavy slots' queue and four tool builds, one killed by the
host's memory). The native attribution provider now answers the **inference tier**: the applicability and
instantiation of generic methods and constructors as javac's Infer computes them (inference variables and
their bounds, incorporation, the graph solver, the return type against the target, propagation into an
enclosing call's inference, thrown variables), the poly arguments as ArgumentAttr and DeferredAttr treat
them (generic calls, diamonds, conditionals, switch expressions, lambdas, method references; stuck or not,
speculatively in the overload phase and in CHECK mode after it), explicit type arguments, diamonds
(`findDiamond`, an anonymous class's), lambdas' and method references' targets and descriptors, method
references' resolution and kinds, signature polymorphic calls, `var`'s upward projection, local classes'
numbering, record patterns' inferred types, the anonymous creation's constructor type and members, and,
beyond the inference tier, the switch facts Attr and Flow give (a pattern switch, an unconditional
pattern, exhaustiveness, a case that completes normally). The lean on javac is **gone** where the provider
answers: in jdk26u the answers that took javac's choice went from **53,316 to 35**, and in the other
corpora from 15,007 to none. `bin/repr-java attr-check --corpus all`: over the samples, the baseline, the
jdk corpus, javac's sources and jdk26u's 68 modules **13,528,529** facts equal (JN9c: 13,193,153), **none
different**, no bridge beyond the known list, and every corpus's forms byte for byte as from javac's
trees. What the provider still declines is **169 facts** (JN9c: 186,358), 161 of them in jdk26u, each
counted with its reason: a cycle among the inference tier's memoized computations (139), a multi-catch
parameter whose lub is an intersection (14), an annotation javac took off a record component's modifiers
(16, 8 of them in the jdk corpus). The provider alone now writes **99.5% of jdk26u's lists** (JN9c:
95.9%). The JVM ran only the frozen v4 jar, for javac's edge in `bin/javac-vendor convert`; Go only
TamaGo's go1.27.1, without cgo; the tools' host was the worktree's stage 0 (the seed's).

### JN9d.1 JFS107 (b): the symbols' type, first

`deftype Sym` (the native symbols: a map with a mutable memo) moved from `decls` into a namespace of its
own, `arbace.repr.java.sym` (`sym`, `sym?`, `sym-map`; Arbace's own, 21 lines); `decls`' `sym` delegates
to it, and `model` and `overload` name `arbace.repr.java.sym.Sym`. Proved before anything else, on the
base tool (main's sources with the move, key `e6c4416f...`): a change to `decls` (JFS103 (b)'s
`members-all` and `*anon-ctor*`) evaluated from the checkout on that tool, `bin/repr-java attr-check
--corpus samples` all equal (2,448 / 0 / 76, as JN9c), where before the move a reloaded `decls` made the
compiled model's `instance?` checks fail. Every later change to `decls` in JN9d was tried on a built tool
without rebuilding it.

### JN9d.2 What was built

| file | lines | what |
|---|---:|---|
| `arbace/repr/java/infer.ae` | 1,835 (new) | `arbace.repr.java.infer`, the inference tier's engine: Infer's inference variables (UndetVar: bounds by kind, newest first, the incorporation actions queue), the incorporation engine (CheckBounds, EqCheckLegacy, CheckInst, SubstBounds, CheckUpperBounds, PropagateBounds, the incorporation cache) and the relations it needs over types with inference variables (Types' `isSubtype`, `isSuperType`, `isSameType`, `containsType`, `isSubtypeUnchecked` with its warner, `isConvertible`, `asSuper`, `supertype`, `interfaces`, `closure`, `union`, `intersect`, `merge`, `closureMin`, `lub`, `glb`, `upward`, `captures`), the graph solver (Tarjan's components, the leaf and best-leaf strategies, the inference steps EQ, LOWER, THROWS, UPPER, CAPTURED in InferenceStep's order), `instantiateAsUninferredVars`, InferenceContext (`asUndetVar`, `asInstType`, `save`, `rollback`, `dupTo`, `min` with its ReachabilityVisitor, free-type listeners), `instantiateMethod` (`newInstances`, `argumentsAcceptable` through the deferred types' checks, PartiallyInferredMethodType's `check`, `generateReturnConstraints`, `needsEagerInstantiation`, `generateReferenceToTargetConstraint`, propagation), the method checks' compatibility (strict, loose, assignment, a void expression lambda's, most specific's, a method reference's), `instantiatePatternType`, the stuck graph of DeferredAttr's `pickDeferredNode`, Attr's `checkExConstraints`; its limits (more than 64 bounds on a variable, 2,000 incorporation rounds) decline the inference as beyond the port |
| `arbace/repr/java/expr.ae` | 2,147 (was 919) | the inference tier's attribution: ArgumentAttr's argument types (deferred: a call without explicit type arguments, a diamond, a conditional, a switch expression, a lambda, a method reference), the standalone and poly kinds, `checkMethod`'s instantiation against the call's target (Attr's result infos: an argument's by its call's inference, recorded when its context's variables are instantiated; an explicitly typed call standalone, its poly arguments checked after the choice), the unchecked call's erasure; lambdas (explicit or implicit, `getTargetInfo`, `instantiateFunctionalInterface`, `removeWildcards`, the descriptor, the returns against its return type, a void descriptor's expression statement, the thrown types' constraints with the body's thrown types where they need no definite assignment), DeferredAttr's stuck policies and structural checks, method references (the qualifier's type, unbound and bound lookups, the overload kind, the referenced method instantiated against the descriptor, the unbound receiver's constraint), diamonds' sites (visitTypeApply's enclosing type) and anonymous diamonds' supertypes, signature polymorphic instances (JFS108 (b)), record patterns' component types and inferred deconstructors, `var`'s upward projection and for-each element, multi-catch parameters' lub, local classes' flat names (Check.localClassName in Attr's order of entering them, poly arguments' anonymous classes after the call), the anonymous creation's constructor type (JFS104 (b)), the switch facts (`pattern-switch?`, `unconditional-pattern?`, `switch-exhaustive?`, `case-completes?`); a cycle among its memoized computations declined |
| `arbace/repr/java/overload.ae` | 1,221 (was 794) | generic candidates' applicability through Infer (JFS102 (b)), explicit type arguments with their bounds, the deferred arguments' checks, most specific with generic and deferred candidates; `resolveDiamond` and `findDiamond`'s constructors (per site); method references (`resolveMemberReference`'s lookups, the static kinds, the basic and structural choosers, the reference kinds, the overload kind's arity check); functional interfaces (`findDescriptorSymbol`, `findDescriptorType` with an intersection's marker interfaces, `interfaceCandidates`, `prune`, `membersClosure`); an intersection site's lookup (a type variable with several bounds); an enum's anonymous constant's constructor PRIVATE, an interface's anonymous class's without ANONCONSTR |
| `arbace/repr/java/attr.ae` | 704 (was 649) | the inference facts native (`method-type`, `constructor-type`, `varargs-element` of generic calls, diamonds and anonymous creations; `fn-target`, `ref-kind`, a reference's symbol; the poly result), the names of a diamond's and a record pattern's type trees, the switch facts (`pattern-switch?`, `unconditional-pattern?`, `exhaustive?`, `completes-normally?`), a dropped annotation's decline with its reason |
| `arbace/repr/java/nattr.ae` | 694 (was 632) | the provider's new caches (its weak reference in the unit's memo), the anonymous class's constructor in its members (JFS103 (b)), `check-platform-members!` (JFS106 (b): `:enter/platform-members`), a type variable of another file's method named as javac names it, a union as its alternatives |
| `arbace/repr/java/decls.ae` | 1,814 (was 1,764) | JFS107 (b)'s move; `members-all` with the anonymous class's constructor (JFS103 (b)); `anon-creation-type`'s hook to the inference tier; `type-apply-outer` (visitTypeApply's enclosing type); ClassReader's `enterMember` (synthetic members skipped) and its @Deprecated flags; a class file's local or anonymous class entered by its flat name as ClassReader enters one a signature names |
| `arbace/repr/java/scope.ae` | 660 (was 631) | the targets the inference tier reads: a creation's arguments, a lambda's body and returns, a cast of a lambda or method reference, a pattern's (`:pattern-of`), a binding pattern's var; a `var`'s initializer without a target; a name or qualifier left to a later lookup (through an anonymous class of a diamond) |
| `arbace/repr/java/model.ae` | 909 (was 872) | the native readings: a functional descriptor, a signature polymorphic instance, a local class's flat and binary name (checked once per class, `:names/local-class`), a multi-catch parameter's type, the inner creation's method type without its outer instance, an anonymous class of a diamond's supertypes |
| `arbace/repr/java/sym.ae` | 21 (new) | JFS107 (b) |
| `arbace/repr/java/types.ae`, `classfile.ae`, `split.ae`, `java.ae` | 387, 304, 419, 487 | a memberType's substitution kept for its type parameters' bounds, capture with the inference tier's glb; a class file's members' annotations; the new facts in the catalogue; the platform members' check after a unit's conversion |

### JN9d.3 The facts

| fact | tier | the native answer | data compared with javac's |
|---|---|---|---|
| `infer/method-type` (a generic method's call's meth) | inference | the instantiated method type (Attr.checkMethod over Infer.instantiateMethod against the call's target, erased by an unchecked call) | the method type as data |
| `infer/constructor-type` (a diamond, a generic constructor, an anonymous creation) | inference | the constructor's instantiated type (an anonymous creation's: its constructor's at the anonymous class's type, JFS104 (b)) | the same |
| `infer/varargs-element` | inference | a generic call's variable arity element | the element type |
| `infer/target` (LAMBDA, REFERENCE) | inference | the functional interface type javac sets on the lambda or reference | the type |
| `infer/poly-result` | inference | a poly expression's type in its argument position | the type |
| `names/local-class` | inference | a local or anonymous class's flat name, numbered as Attr enters them | the name |
| `overload/method-ref`, `overload/ref-kind` | overload | a method reference's method and kind | the method as data, the kind's name |
| `enter/platform-members` (once per platform class the run reads) | declarations | the class file's members by name, flags and erased signature (JFS106 (b)) | javac's ClassSymbol's members |
| `types/pattern-switch` | types | Attr.handleSwitch's patternSwitch | the boolean |
| `flow/unconditional-pattern`, `flow/exhaustive`, `flow/case-completes` | flow | Attr's hasUnconditionalPattern; Flow's isExhaustive (for compiling code: an unconditional pattern or a switch expected to be exhaustive); the case's liveness (the walk's AliveAnalyzer) | the booleans |

The facts of JN9b and JN9c whose answer needed a choice javac made by inference (`overload/method`,
`overload/arity`, `types/expr` of a generic call, `scope/lambda-param`, and every fact over an expression
with such a call in it) are now native where they leaned. What javac does that the corpora showed and the
provider follows, each found by a fact that differed (or failed), then fixed:

- ArgumentAttr's kinds are syntactic: every method call without explicit type arguments in an argument
  position is a deferred `ResolvedMethodType` (generic or not), a lambda without parameters is explicit,
  and an explicitly typed generic call is standalone (its poly arguments checked after the choice, at the
  explicit instantiation, their unchecked conversions erasing the call's result).
- An argument the target cannot type (a lambda or method reference against a non-functional formal, a
  method reference whose resolution fails) makes the candidate inapplicable: an error of the check, not a
  decline.
- A void descriptor accepts an expression lambda only when its body is an expression statement
  (TreeInfo.isExpressionStatement).
- A `var`'s initializer has no target (Type.noType), so a generic call there is inferred standalone.
- Unchecked conversions arise inside incorporation (the warner passed through the bounds' checks), and an
  unchecked call's return and thrown types are erased.
- A method's type parameters' bounds are substituted at the site (memberType substitutes a ForAll's bounds
  too); a capture of a type mentioning inference variables takes glb through Types' subtyping, which adds
  bounds (`Class<? extends U>` captured against `Class<? extends ClassLoader>`).
- The inference steps of the last graph phase are an EnumSet, so THROWS comes before UPPER: a thrown
  variable without other bounds is RuntimeException, once checkExConstraints has marked it (a lambda's
  body's checked exceptions, a referenced method's thrown types).
- A method reference's resolution is rolled back (`save`/`rollback`) and the unbound receiver's constraint
  regenerated after it (Attr.visitReference), which is where `comparing(P::name)` gets `T <: P`.
- A diamond's class tree keeps the inference variables when its constructor type is propagated into the
  enclosing call (Attr.visitNewClass sets it before they are instantiated); a diamond's site has the
  enclosing type visitTypeApply recovers (`Hashtable<K,V>.Enumerator<T>`), and findDiamond's synthesized
  constructors are per site.
- A poly conditional or switch argument's type is its target as MethodResultInfo.check gives it, captured;
  a rule case's expression is the case's body (its synthetic yield's value is the same tree in javac, a
  copy in the native tree).
- A qualified creation's enclosing type is the qualifier's type's asOuterSuper; an anonymous class of a
  diamond takes its supertype from the inference of its creation, so its members and the names through it
  wait for it.
- An enum constant's body's constructor is PRIVATE (Check.checkFlags), and an anonymous class implementing
  an interface has BasicConstructorHelper's constructor, without ANONCONSTR.
- ClassReader skips synthetic members when it enters them and sets the deprecation flags from
  `@Deprecated`; a local or anonymous class a class file's signature names is entered by its flat name,
  its owner the package.
- A type variable bounded by several types is looked up as its intersection's synthetic class (no members,
  its interfaces the bounds after the class).
- The qualifiers of a type qualifier name types or packages too (`java.lang.reflect.Array.newInstance`).
- A type variable of a method of another file of the unit is named as a class file's method's (`[:m owner
  name]`), as javac's side names it.
- A method's type variable in its throws clause is THROWS from its declaration (MemberEnter, ClassReader's
  signature: Infer.fromTypeVarFun), so `orElseThrow(() -> new X())` and `<E extends Exception> ... throws
  E` called without constraints infer RuntimeException.
- A block lambda is void-compatible only without `return e;` and value-compatible only when it cannot
  complete normally (Attr.visitReturn, checkLambdaCompatible), which decides `submit(Runnable)` against
  `submit(Callable)`; and unrelated functional interfaces are compared by their descriptors in most
  specific (MostSpecificCheckContext.functionalInterfaceMostSpecific and its return checker), not by
  subtyping.
- A wildcard's upper bound is its formal's declared bound when it is `?` or `? super` (WildcardType.bound),
  so `Attribute<? extends Attribute<?>>` and `Attribute<?>` are the same type argument
  (containsTypeEquivalent), in both tiers.
- A class file's superclass and interfaces without a signature are erased with their enclosing type (an
  inner class's supertype keeps its outer).
- A signature polymorphic call's argument that is a call takes MethodResultInfo.check's upper bound of a
  captured variable; a lambda or method reference argument is not converted, so it does not make a
  non-generic call unchecked (its returns' conversions aside).
- A poly conditional's captured target reaches it through parentheses and enclosing arms; a lambda's
  recorded target is its instantiated functional interface, not its argument's formal.
- A method reference lookup's error is ignorable when every candidate failed by arity
  (ReferenceLookupResult.canIgnore).

### JN9d.4 The check and the numbers

On the final tool (key `bd463814dc88...`), the host shared with other work, `bin/repr-java attr-check
--corpus all` (with `GOGC=100`: the first final tool's run of the jdk corpus, at Arbace's default of 200, where
a read of java.base peaks near 40 GB, was killed by the host's memory with the other heavy slot busy; JFS116),
against JN9c.3's run:

| corpus | forms files equal | facts equal / different / declined (leaned on javac) | JN9c | wall (JN9c) |
|---|---:|---:|---:|---:|
| `samples` | 17 of 17 | 2,634 / 0 / 0 (0) | 2,448 / 0 / 76 (11) | 12 s (6 s) |
| `baseline` | 189 of 189 | 317,731 / 0 / 0 (0) | 315,089 / 0 / 360 (221) | 104 s (77 s) |
| `jdk` | 911 of 911 | 1,123,925 / 0 / 8 (0) | 1,096,768 / 0 / 16,598 (6,305) | 336 s (218 s) |
| `javac` | 1,828 of 1,828 | 1,065,850 / 0 / 0 (0) | 1,027,107 / 0 / 23,923 (8,470) | 267 s (232 s) |
| `jdk26u` | 13,788 of 13,788 | 11,018,389 / 0 / 161 (35) | 10,751,741 / 0 / 145,401 (53,316) | 2,009 s (1,839 s) |

The whole `--corpus all` took 2,732 s against JN9c's 2,378 s (15% more; jdk26u 9% more, the jdk corpus
54%: its generic code is the inference tier's), its peak resident memory 29.1 GB. The facts counted grew
because the inference tier's are answered (before, `infer/*`, `names/local-class`, `overload/method-ref`,
`overload/ref-kind`, `enter/platform-members` and the switch facts were javac's, counted only in the
split). By tier in jdk26u (equal / different / declined, leaned):

| tier | JN9d | JN9c |
|---|---:|---:|
| syntax | 536,151 / 0 / 0 | (not in the table) |
| scope | 2,736,358 / 0 / 5 | 2,726,388 / 0 / 9,974 (360) |
| declarations | 2,295,649 / 0 / 8 | (not in the table) |
| flow | 33,946 / 0 / 0 | (javac's) |
| members | 556,807 / 0 / 4 | 556,242 / 0 / 569 (18) |
| types | 2,738,889 / 0 / 67 (3) | 2,680,134 / 0 / 53,982 (3,347) |
| overload | 2,078,502 / 0 / 34 (32) | 2,001,666 / 0 / 68,201 (49,591) |
| inference | 42,087 / 0 / 43 | (javac's) |
| total | 11,018,389 / 0 / 161 (35) | 10,751,741 / 0 / 145,401 (53,316) |

By fact, jdk26u's inference tier: `infer/method-type` 22,150 / 29 declined, `infer/target` 8,878 / 14,
`infer/constructor-type` 7,679, `names/local-class` 2,416, `infer/poly-result` 786,
`infer/varargs-element` 178; `overload/method-ref` and `overload/ref-kind` 3,012 each;
`enter/platform-members` 31,822 (the classes the runs read, JFS106 (b)); `types/pattern-switch` 4,813;
`flow/case-completes` 32,674, `flow/unconditional-pattern` 1,163, `flow/exhaustive` 109.

| | samples | baseline | jdk | javac | jdk26u |
|---|---:|---:|---:|---:|---:|
| lists the provider wrote alone | 96.5% (JN9c 87.5%) | 99.9% (99.1%) | 99.5% (95.9%) | 99.2% (93.0%) | 99.5% (2,247,464 of 2,258,047; JN9c 95.9%) |
| model reads native / javac / bridge | 14,728 / 82 / 15 | 1,976,227 / 3,835 / 183 | 6,558,815 / 23,868 / 880 | 6,762,918 / 12,099 / 828 | 65,771,160 / 183,165 / 13,053 |
| native share of the model's reads | 99.3% (92.1%) | 99.8% (99.7%) | 99.6% (97.3%) | 99.8% (96.4%) | 99.7% (97.4%) |

The bridges are within the known list (attr-check fails otherwise): in jdk26u 13,053 (JN9c 48,519), of
them 12,730 the package members (`:model/package-members`, one per file), 312 a constant's value, 11 a
type; the local classes' names (JFS91's bridge) and an anonymous class of a diamond's supertypes are
native now. What the lists the provider does not write alone still ask is javac's answer of a declined
fact (above) or a javac-only read of the model (its javac column).

The declines in all the corpora, by reason (`attr.md`'s table): a cycle among the inference tier's
memoized computations, `(:checked)` or `(:args-checked)` (139 facts in jdk26u: a computation re-entered
more than three times, where javac's order of attribution reaches the same answer without the re-entry;
JFS113), a multi-catch parameter whose lub is an intersection (14), an annotation javac moved off a record
component's modifiers to its field or accessor (8 in the jdk corpus, 8 in jdk26u: javac's tree no longer
has it).

### JN9d.5 The shipped program's change

None in code: `arbace/repr/java` is not in the shipped program (j2g's `embed.ae` leaves it out; javac's
tool embeds it, now with `infer` and `sym`: 230,387,872 bytes against JN9c's 226,652,320), and `go/`,
`overlay/`, `jdk/forms`, `jdk/data` and `jdk/edge.edn` are unchanged. The reader hash
(`repr_java_reader_hash`: `arbace/repr/java.ae`, `attr` and `model` changed) moved from `a2daae6b...` to
`4b477c57...`, written by the updates on the final tool: `bin/clojure-tests --java-fixtures update`,
`bin/jrt-convert update`, `bin/javac-vendor convert` (its edge on the frozen v4 jar): every forms and data
file byte for byte as committed, only `:repr-java` and `:host` changed in `jdk/provenance.edn`,
`jdk/javac/provenance.edn` and `test/java-fixtures/provenance.edn`. One more file came out different and
is not committed: `bin/javac-vendor convert` writes `jdk/javac/edge.edn` without `java/lang/Module`, which
REAP4 (`6453bfbc`) recorded into `jdk/edge.edn` without converting javac's edge again (javac's edge is
what its world asks beyond `jdk/edge.edn`); that is main's, and committing it here would change the tool's
key (`jdk/javac/edge.edn` is among its inputs) after the provenance named it (JFS119's (a) takes it with
the next tool build; `.tmp/jn9d/final/javac-edge.edn` has the converted file). No re-seed. The tools were
built for amd64 only (`ARBACE_GO_ARCHES=amd64`).

### JN9d.6 The checks

All on the final tool, one after another (`.tmp/jn9d/final/final.sh`), the host shared:

- `bin/repr-java attr-check --corpus all`: JN9d.4's table; every corpus's forms equal and every fact
  equal, no bridge beyond the known (2,732 s; on the first final tool, key `26bb2629...`, it found
  jdk26u's 53 differing facts and 2 differing forms files that the commit `f9c68130` fixed, and its jdk
  corpus was killed by the host's memory)
- `bin/repr-java front-check`: every default corpus equal (403 s); `bin/repr-java parse-check`: every
  corpus's trees as javac's (274 s)
- `bin/repr-java check`: the baseline, the samples and the module as expected (30 s)
- `bin/repr-java check --jdk`: 68 modules converted, 0 with failures, the 13,566 forms files as
  `test/repr/java/expected/jdk26u.sha256` (658 s)
- `bin/repr-java roundtrip --corpus baseline,samples,jdk --text`: as the references (the jdk corpus's 39
  known `:anew` rejections included; 691 s)
- the updates of JN9d.5: only the three provenance files' `:repr-java` and `:host` changed (and javac's
  edge, main's, not committed)
- `bin/gate` (essential): on the branch at `4b317ac2` (the code and the provenance as final; this section's
  commit changes only `doc/`), passes in 31m35s (go-build 17m31s afresh, go-smoke, suite-go with no
  regression, oracle-go 52 known mismatches as recorded, diffcheck 12,789 of 12,801 cases agree with the 12
  known, goi-check 801 of 801, goi-aot-essential 248 of 248, java-check 428 of 428, java-compiled 420 of 420,
  suite-aot-essential, generated, j2g-edge verify: `jdk/edge.edn` the JVM's answer, reap 12 of 12 cells)
- not run: `bin/gate --full` (the user's rule), `bin/repr-java check --suite`, `bin/javac-vendor verify`
  and `classes` (the converted forms and javac's world are unchanged: the updates show the forms byte for
  byte)

### JN9d.7 What it means

JN9d took about a working day and a half against JN9c.7's 3-5 days. The port followed Infer's code closely
(the incorporation and the solver are its structure); most of the time went to what javac does around it,
in Attr, ArgumentAttr, DeferredAttr and Resolve, that a reading had missed: JN9d.3's list, each item a
fact that differed. Two things helped: JFS107 (b), so that every change to `decls` was tried on a built
tool, and the trace (`REPR_JAVA_INFER_TRACE`), which shows the bounds, the errors and the cycles. Two
cost: the evaluated namespaces run about twice as slow as compiled ones, so a corpus run on a tool built
before the change takes longer than one on the final tool; and a forward reference that the tool's
compiled copy hid only showed in the next tool build (a fresh load, `.tmp/jn9d/loadfresh.sh`, finds them
first).

The provider now answers every fact the corpora ask but 169, javac's choices are taken nowhere but 35
calls of jdk26u (their arguments' facts declined), and it writes 99.5% of jdk26u's lists alone. What javac
still does in a native read is parse and attribute every file beside the provider, answer the declines,
and give the model the reads of JN9d.4's javac and bridge columns. A read without javac (JFS111) is the
step left for the provider to be the converter's front end; its prerequisites are JFS110's declines and
JFS112's reads, both small.

### JN9d.8 Proposals

- **JFS110 The remaining declines.**
  - Options: (a) as built: each counted with its reason in `attr.md` (JN9d.4's table: 139 cycles among the
    inference tier's computations, 14 multi-catch parameters whose lub is an intersection, 16 annotations
    javac took off a record component's modifiers), javac's answer taken for it; (b) port what each needs:
    the cycles first (javac's order of attribution, DeferredAttr's nodes, in place of the memoized
    computations' re-entry), an intersection lub for a multi-catch parameter, TypeEnter's propagation of a
    record component's annotations to its field, accessor and parameter by their targets (the dropped
    annotations); then what no corpus asks yet but a read without javac would meet: a non-public member at
    an intersection site, a lambda's thrown types through a try with resources, a rethrown catch parameter
    or a finally block that cannot complete (Flow's analysis); (c) leave them: they are a few hundred in
    jdk26u's millions.
  - *Recommended: (b), but only with JFS111*: none of them matters while javac runs beside the provider; a
    javac-free read needs each one, and they are small ports one by one.
- **JFS111 A read without javac.**
  - Options: (a) as built: the native provider runs beside javac on Arbace, every fact it answers checked
    against javac's (`--front native --attr native` still parses and attributes every file with javac, and
    takes its answer where the provider declines); (b) a read mode that does not run javac at all (`read
    --front native --attr native --no-javac`): the native parser's trees, the provider's answers alone, a
    file whose conversion meets a decline converted again with javac (counted), so the forms stay javac's
    where the provider cannot answer; (c) the same without the fallback, a decline an error.
  - *Recommended: (b)*: it is what JFS69 (b) aimed at, the corpora show the provider answers nearly
    everything, and the fallback keeps every corpus's forms byte for byte while the declines of JFS110 go.
    Its check: the forms of `--no-javac` against javac's, file for file, and the time without javac.
- **JFS112 The model's last javac reads.**
  - Options: (a) as built: 99.7% (65,771,160) of jdk26u's model reads are native; the rest (183,165) read
    javac's objects that reach the model through facts javac still answers (the declines) and javac-only
    paths, and 13,053 bridge (the package members, `:model/package-members`, 12,730; a constant's value,
    312); (b) trace each read kind's javac objects to their source and nativize them, the package members
    first.
  - *Recommended: (b), with JFS111*: a read without javac cannot read javac's objects; until then they
    cost nothing.
- **JFS113 The inference port's limits.**
  - Options: (a) as built: an inference with a variable of more than 64 bounds or more than 2,000
    incorporation rounds, and a computation re-entered more than three times (a cycle among the memoized
    computations), is declined (the cycles of JN9d.4, 139 facts in jdk26u; the size limits fired in none);
    (b) remove the limits (javac has none: a runaway is the port's bug, and a decline hides it); (c) keep
    them and make attr-check fail when one fires.
  - *Recommended: (c)*: the size limits stop a bug from exhausting memory (the first jdk run without them
    did: the deferred nodes' loop of a bug), and a check that fails on them still finds the bug; the cycle
    limit's declines are JFS110's to remove.
- **JFS114 Flow's exhaustiveness.**
  - Options: (a) as built: `flow/exhaustive` native for compiling code (an unconditional pattern, or a
    switch expected to be exhaustive, which javac then requires to be); (b) Flow's exhaustiveness proper
    (sealed hierarchies, record patterns' components, enum constants), so that invalid code gets javac's
    answer too.
  - *Recommended: (a)* until the converter must read code javac rejects (JFS105 (a)'s stance); (b) is the
    diagnostics' work.
- **JFS115 The time.**
  - Options: (a) as built: `attr-check --corpus all` 2,732 s, 15% more than JN9c's (the jdk corpus 54%
    more, jdk26u 9%), the inference tier's cost while javac's attribution still runs beside it; (b)
    profile the inference tier on jdk26u (the evaluator-free tool: `ARBACE_CPUPROFILE`) and cut what it
    repeats (the deferred arguments' checks in each phase, the incorporation of large contexts).
  - *Recommended: (b), after JFS111*: without javac, the provider's time is the read's time, and the
    profile is then worth reading.
- **JFS116 The memory.**
  - Options: (a) as built: a read of java.base with 4 threads peaks at about 38-40 GB, with or without the
    inference tier (the base tool's run: 38.6 GB, JN9d's code evaluated on it 40.3 GB; `--corpus all` with
    `GOGC=100`: 29.1 GB): javac's attribution of the module and the native trees of 838 files held at
    once; (b) measure where it goes (`ARBACE_MEMPROFILE`) and release each unit's answers after its
    conversion.
  - *Recommended: (b)*: the host has 62 GB and two heavy slots; a read that takes most of it blocks the
    other slot.
- **JFS117 The inference trace.**
  - Options: (a) as built: `REPR_JAVA_INFER_TRACE` prints the inference's steps, the cycles' stacks and a
    failed fact's stack to stderr (a debugging aid, off by default, no cost when off); (b) remove it.
  - *Recommended: (a)*: it found most of JN9d.3's behaviours, and it costs one test of a constant.
- **JFS118 Documenting the provider.**
  - Options: (a) as built: the provider's tiers are documented in this survey's As built sections and the
    namespaces' docstrings; (b) a reference, `doc/classes/NATIVE-ATTR.md`: the tiers, each fact with its
    javac source, the declines' reasons, how attr-check judges them.
  - *Recommended: (b)*: four tiers on, the survey's sections are history, and a reference is what the next
    tier (JFS111) needs to start from.
- **JFS119 Next.**
  - Options: (a) JFS111 (b), the read without javac, with JFS110 (b) and JFS112 (b) inside it (and javac's
    edge converted again with its tool build, JN9d.5), estimated 1-2 days; (b) stop at JN9d: javac stays
    as the provider's check and its fallback.
  - *Recommended: (a)*, if a javac-free converter is still the aim (JFS69 (b)): the provider now answers
    every fact the corpora ask but a few hundred, and the read without javac is the step that makes it the
    converter's front end.

### JN9d.9 Sources and scratch

- jdk26u at `baf63fbe42b8758448fee570a9d9bb914272d259` (`/root/jdk26u`):
  `src/jdk.compiler/share/classes/com/sun/tools/javac/comp/{Infer,InferenceContext,DeferredAttr,ArgumentAttr,Attr,Resolve,Check,TypeEnter,Flow}.java`,
  `code/{Type,Types,Symbol}.java`, `jvm/ClassReader.java`, `tree/TreeInfo.java`, `util/GraphUtils.java`.
- The worktree's `.tmp/jn9d/` (scratch, not tracked): `build1.log.keep`, `build2.log`, `build3.log`,
  `build4.log` (the base tool's build, an intermediate tool's, the first final tool's and the final
  tool's); `p107-samples.log`, `p107-samples-attr.md`, `decls107.patch` (JFS107 (b)'s proof);
  `r9base-attr/` (the base tool's jdk corpus: `attr.md`, `split.md`, `jdk.time` with its peak memory);
  `r8-attr/` (the first jdk corpus run that converted, JN9d's code evaluated on the base tool, its
  differences); `r10-attr/` (the intermediate tool's default corpora); `final-b3/` (the first final tool's
  checks, with jdk26u's differences); `final/` (the final tool's checks: `final.sh`, their logs,
  `summary.txt`, `attr.md`, `attr.edn`, `split.md`, `javac-edge.edn`); `q*/`, `j*/`, `m*/` (the small
  reproductions, the java.base batches and the single files of jdk26u's modules: `q1.sh`, `qjdk.sh`,
  `qmod.sh`); `facts.ae`, `why.ae` (a run's differences and declines by reason); `snaprun.sh`, `two.sh`,
  `jdk.sh`, `build.sh`, `load.sh`, `loadfresh.sh` (the snapshot runs, the load checks); `gate.log` (the
  essential gate).

### JN9d.10 After merging main (JC5, REAP7b, JC6)

main was merged twice (`0234d262`, `3aacf8d0`); the tool rebuilt from the merged checkout (key `0cb23c6e...`,
231,776,416 bytes). The merge showed one more native fact differing in the samples (PrintSyntax.java,
REAP5b's: anonymous classes in switch rule arms numbered twice, the rule's body and its yield's value being
copies in the native tree), fixed by `1afd9b5f`. On the merged tool `bin/repr-java attr-check --corpus all`
(GOGC=100, 2,283 s, 29.4 GB peak): every corpus's forms byte for byte and every fact equal, **14,416,929
facts** (JC5's converter asks more), 171 declined (140 cycles, 14 multi-catch intersection lubs, 16 dropped
record-component annotations, 1 a package's type in the samples), 35 leaned (jdk26u); lists written alone
99.5% of jdk26u's; model reads 99.7% native in jdk26u. `front-check`, `parse-check`, `check`, `check --jdk`
(the hashes as main's), `roundtrip --corpus baseline,samples,jdk --text` pass; the updates (fixtures,
`jrt-convert`, `javac-vendor convert`, `java-check --forms update`) changed only the four provenance files'
`:repr-java` (`3d54bdd5...`) and `:host`, javac's edge now main's (its `java/lang/Module` gone with JC5); the
essential gate passed at `0ce59afd` (25m44s).

## Decided (2026-10-26): JFS110-JFS119

The user decided JN9d's proposals as recommended, the next step after the milestone `arbace-reaps-3-crops-v1`:
JFS110 (b) (the remaining declines ported, cycles first), JFS111 (b) (a read with no javac beside it, a
counted per-file fallback to javac where a decline is met), JFS112 (b) (the model's last javac reads made
native), JFS113 (c), JFS114 (a), JFS115 (b) after JFS111, JFS116 (b) (each unit's answers released after its
conversion), JFS117 (a), JFS118 (b) (`doc/classes/NATIVE-ATTR.md`), JFS119 (a): JN9e, after the tag.

## As built: JN9e (2026-10-27)

JN9e (JFS111 (b), decided with JFS110-JFS119 as recommended), with JFS110 (b), JFS112 (b), JFS113 (c),
JFS116 (b), JFS117 (a) and JFS118 (b) inside it and JFS115 (b) after it, on the branch `java-jn9e` from main
`2e8629c7`, in about a working day (one tool build, about half an hour, and the corpora's runs in the shared heavy slots).
repr/java now **reads Java without javac**: `bin/repr-java read --no-javac` (and `read-jdk --no-javac`)
parses each file with the native parser, enters it in the run's registry, and converts it with the
native provider's answers alone; whatever would ask javac (a node's javac twin, a declined fact, a lean on
javac's choice, a javac or bridged read of the model, a parse error, a failure) stops the unit, and its
file is read again by javac (the fallback, counted by reason). Over every corpus (the samples, the
baseline, the jdk corpus, javac's sources and jdk26u's 68 modules, **14,650 files**) the read without
javac reads **every file natively, no fallback**, and its forms are javac's **byte for byte, file for
file** (`bin/repr-java nojavac-check --corpus all`). Beside javac, `bin/repr-java attr-check --corpus all`:
**14,503,922 facts equal** (JN9d.10: 14,416,929), **none different, none declined** (JN9d.10: 171), **none
leaned** (35), no model read bridged (jdk26u JN9d: 13,053). A read of jdk26u without javac takes 1,123 s
on 4 threads and at most **8.4 GB** (a read of java.base beside javac: 38-40 GB, JFS116). The JVM ran
only the frozen v4 jar, for javac's edge in `bin/javac-vendor convert`; Go only TamaGo's go1.27.1,
without cgo; the tools' host was the worktree's stage 0 (the seed's).

### JN9e.1 What was built

| file | lines (main) | what |
|---|---:|---|
| `arbace/repr/java.ae` | 585 (487) | `--no-javac`: `convert-units-nojavac` (the files parsed and entered, each unit converted with `decls/*no-javac*`, the fallback's files read by javac in one `convert-units` run), `registry-units` (the registry of `native-units`, now shared), `OUT/repr-java-nojavac.edn` (files, native, fallbacks with reasons, times, the process's peak resident memory), read-jdk's chunks summed |
| `arbace/repr/java/decls.ae` | 1,875 (1,823) | `*no-javac*`, `javac-needed!`, `javac-needed-reason`; `package-class-names` (JFS112 (b): ClassFinder's top-level names of a package: the source directories' `.java` files, the run's module's class files, the units' classes); a record accessor's `@Deprecated` (TypeEnter's original annotations, MemberEnter's deprecation flags); `const-as` public |
| `arbace/repr/java/attr.ae` | 796 (704) | javac's answers lazy (each a delay, never forced without javac); `native-fact` without javac: the native answer, else `javac-needed!` with the fact and the reason; a fact's class from the native answer (`nsym-class`); the anonymous creation's variable arity element; a record component's annotations Check takes off its field (`record-dropped?`, the fact `:annotations/dropped`, JFS110 (b)); a native method's instantiation class; an array's clone and constructor native |
| `arbace/repr/java/model.ae` | 962 (909) | `rd` stops a javac or bridged read without javac; `pkg-class-names` native, checked against javac's once per unit (`:names/package-classes`); a `var` local's constant at its inferred type (`:types/var-constant`); a local class's flat name native without javac, and recorded for the class infos |
| `arbace/repr/java/resolve.ae` | 281 (226) | the Java forms compiler's class infos from the native model (`nclass-info`, `nfind-class`: flags, supertypes, nest host, outer class, members' erased descriptors, a local class by its provider-given flat name) whenever the native provider answers |
| `arbace/repr/java/expr.ae` | 2,366 (2,268) | a lambda's thrown types (Flow.FlowAnalyzer.visitTry): a try's resources' initializers and close() (resolved at each AutoCloseable supertype), precise rethrow of a final or effectively final catch parameter (Check.intersect, incl, subset), a finally block that cannot complete; a call the lambda returns while its outer call is being checked throws its method's declared types (`call-thrown`: the cycles, JFS110 (b)); a multi-catch parameter's intersection lub; a type variable named as a method reference's qualifier; `lean!` stops a read without javac |
| `arbace/repr/java/overload.ae` | 1,241 (1,233) | an intersection site's non-public members (Types.makeIntersectionType's synthetic class: not an interface, the unnamed package; a private member never inherited) |
| `arbace/repr/java/convert.ae`, `nattr.ae` | 3,388, 696 | the converter's int, boolean, Object and Iterable types native with the provider, `convert-unit` without javac's unit; the provider without javac's index, its maps for record annotations and local classes' flat names; `twin-of` stops a read without javac |
| `arbace/repr/java/split.ae` | 482 (419) | `split nojavac` (`nojavac.md`, `nojavac.edn`), JFS113 (c)'s failures in `attr` and `nojavac`, the new facts in the catalogue |
| `bin/lib/repr-java/front-check`, `bin/repr-java`, `Makefile` | | `front-check --no-javac` (`bin/repr-java nojavac-check`, `make repr-java-nojavac-check`); `test/repr/java/nojavac-check-known.edn` (none) |
| `doc/classes/NATIVE-ATTR.md` | 219 (new) | JFS118 (b): the provider's reference |

### JN9e.2 JFS110 (b): the declines ported

Each found by a small reproduction (`.tmp/jn9e/q1`-`q4`, `cy`) and checked fact by fact against javac:

- **The cycles** (JN9d: 139 facts in jdk26u). A lambda's thrown types (Flow.analyzeLambdaThrownTypes)
  walk its body, and a call the lambda returns is a poly argument of the call the lambda is an argument
  of (`arg-context` through `:lambda-return`): its instantiation is that outer call's check, the one
  under way, so the memoized computations re-entered each other until the cycle limit declined. javac
  attributes the lambda's body inside the outer call's inference, the returned call's type partially
  inferred, and Flow reads its thrown types from it; the port takes the call's method's declared thrown
  types while the outer check is active (`checking?` over the active computations), and declines only
  when they name the method's own type variables (none in the corpora). The read without javac met
  them in 8 files of jdk.javadoc before the port; none after; attr-check counts no cycle.
- **The multi-catch intersection lub** (14): javac's UnionClassType over the IntersectionClassType;
  the native type keeps its alternatives (`:union-alts`) on the intersection, as data `[:union ...]`.
- **The record components' annotations** (16): Check.validateAnnotation takes off a record component's
  field the annotations whose targets are neither FIELD nor TYPE_USE (TypeEnter put them on every
  record member); `record-dropped?` decides it natively, checked against javac's tree (`:annotations/dropped`:
  8 in the jdk corpus, 10 in jdk26u, equal);
  a component's `@Deprecated` now also deprecates its generated accessor (`:enter/members` differed on a
  sample, none in the corpora).
- **A type variable as a qualifier** (1, the samples' `E::get`): its type is the variable's.
- **What a read without javac meets** (no corpus asked them beside javac): an intersection site's
  non-public members (the members of `E1 | E2`'s lub `IOException & Tag`), a lambda's thrown types
  through a try with resources, a rethrown catch parameter and a finally block that cannot complete
  (Flow.FlowAnalyzer.visitTry, visitThrow), each on a reproduction against javac.

### JN9e.3 JFS112 (b): the model's last javac reads

The bridges of JN9d.4 (jdk26u: 12,730 package members, 312 constants, 11 types) are native: a unit's
package's class names as ClassFinder lists them (checked against javac's in every unit: 14,650 equal), a
`var` local's constant (`:types/var-constant`: 359 equal, 312 of them in jdk26u), the rest with the inference tier's answers. With
the provider the converter's own types (int, boolean, Object, Iterable), the Java forms compiler's class
infos for the param-tags decision (`arbace.repr.java.resolve`, which read javac's symbols outside the
model and so were not counted before), an array's clone and constructor, and the facts' classes of a
native method are native too. Left beside javac: 998 javac reads in the jdk corpus and 1,374 in jdk26u
(of 8.3 and 81.7 million), all on paths a read without javac does not take (javac's answer kept for a
name whose native answer is compared, a javac type the converter's coercions mix in); without javac
there are none (a read without javac fails over on any).

### JN9e.4 The numbers

attr-check on the final tool (key `570a7bb8...`), `--corpus all`, GOGC=100, against JN9d (JN9d.4's run,
and JN9d.10's totals after merging main):

| corpus | forms files equal | facts equal / different / declined (leaned) | JN9d | wall (JN9d) |
|---|---:|---:|---:|---:|
| `samples` | 18 of 18 | 3,748 / 0 / 0 (0) | 2,634 / 0 / 0 (0) | 11 s (12 s) |
| `baseline` | 189 of 189 | 339,088 / 0 / 0 (0) | 317,731 / 0 / 0 (0) | 131 s (104 s) |
| `jdk` | 982 of 982 | 1,279,085 / 0 / 0 (0) | 1,123,925 / 0 / 8 (0) | 412 s (336 s) |
| `javac` | 1,806 of 1,806 | 1,125,815 / 0 / 0 (0) | 1,065,850 / 0 / 0 (0) | 367 s (267 s) |
| `jdk26u` | 13,788 of 13,788 | 11,756,186 / 0 / 0 (0) | 11,018,389 / 0 / 161 (35) | 2,592 s (2,009 s) |
| total | | 14,503,922 / 0 / 0 (0) | 14,416,929 / 0 / 171 (35) (JN9d.10) | 3,518 s (2,283 s, JN9d.10) |

The corpora grew with main (JC5's converter asks more facts; the jdk corpus is 906 files, javac's 816);
the facts per corpus grew with the new facts (`:names/package-classes`, `:types/var-constant`,
`:annotations/dropped`) and the facts that were declined. The run's peak resident memory 30.1 GB (JN9d.10:
29.4 GB). By tier in jdk26u: syntax 536,151; scope 3,274,935; declarations 2,297,938; flow 33,946;
members 569,541; types 2,916,507; overload 2,086,172; inference 40,996; all equal.

| | samples | baseline | jdk | javac | jdk26u |
|---|---:|---:|---:|---:|---:|
| lists the provider wrote alone | 98.1% (JN9d 96.5%) | 100.0% (99.9%) | 99.5% (99.5%) | 99.4% (99.2%) | 99.6% (2,250,002 of 2,258,049; JN9d 99.5%) |
| model reads native / javac / bridge | 38,618 / 0 / 0 | 2,346,364 / 0 / 0 | 8,319,602 / 998 / 0 | 8,147,749 / 0 / 0 | 81,688,421 / 1,374 / 0 |

The read without javac, `bin/repr-java nojavac-check --corpus all` on the final tool (2,315 s; the reads
without javac on 4 threads but the baseline's and the samples', the reference javac's reads beside them):

| corpus | files | read natively | fallbacks | wall without javac | javac's read (the reference) | peak without javac |
|---|---:|---:|---:|---:|---:|---:|
| `samples` | 15 | 15 | 0 | 2 s | 2 s | 0.3 GB |
| `baseline` | 183 | 183 | 0 | 56 s | 29 s | 1.1 GB |
| `jdk` | 906 | 906 | 0 | 140 s | 109 s | 7.6 GB |
| `javac` | 816 | 816 | 0 | 155 s | 89 s | 3.6 GB |
| `jdk26u` | 12,730 | 12,730 | 0 | 1,123 s | 609 s | 8.4 GB |

Every file's forms equal javac's. A read without javac costs about 1.3 to 1.9 times javac's own read
(javac's parser and attribution, compiled Java forms translated to Go, against the native parser and
provider, compiled Arbace), and a fifth of its memory: javac's attribution of a module's files holds
every tree and symbol at once, the provider holds the registry (the trees and the symbols' memos) and
one unit's answers (released after its conversion: the provider is dropped, its weak reference in the
unit's memo, JFS116 (b)).

JFS115 (b)'s profile (`ARBACE_CPUPROFILE`, `ARBACE_MEMPROFILE`; `.tmp/jn9e/prof/`): the jdk corpus read
without javac on the final tool, 4 threads, GOGC=100: 214 s wall, 1,395 s of CPU. **Go's garbage
collector takes about 60% of it** (`runtime.gcDrain` 842 s cumulative); of the rest, the converter's frames
(`attr/framed*`) 195 s, the native parser 111 s, lazy sequences' realization about a quarter throughout.
The run allocated 182 GB: string builders' buffers 37 GB (`sbuf.ensure`), function objects 19 GB, lazy
sequences 20 GB, strings 15 GB. The heap at the end (10.7 GB in use at the last collection) is mostly the
written forms' text (7.3 GB of strings under `arbace.repr.java.layout/form-text`), not the provider's:
each unit's answers are gone with its provider. So the time without javac is allocation's, and GOGC or
fewer intermediate sequences and strings are what would cut it (JFS124).

### JN9e.5 JFS113 (c), JFS117 (a)

The inference's size limits stay (64 bounds on a variable, 2,000 incorporation rounds, 1,000 deferred
nodes); `bin/repr-java attr-check` and `nojavac-check` fail when one fires (`split attr` and `split
nojavac` print it as beyond the known). None fired. `REPR_JAVA_INFER_TRACE` stays, and
`REPR_JAVA_NOJAVAC_TRACE` prints each fallback's reason and stack.

### JN9e.6 The shipped program's change

None: `arbace/repr/java` is javac's tool's (231,776,416 bytes at JN9d.10, 233,865,376 now), not the
shipped program's; `go/`, `overlay/`, `jdk/forms`, `jdk/data` and both edges are unchanged. The reader hash
(`repr_java_reader_hash`) moved from `3d54bdd5...` to `ea669c97...`, written by the updates on the final
tool: `bin/clojure-tests --java-fixtures update`, `bin/jrt-convert update`, `bin/javac-vendor convert` (its
edge on the frozen v4 jar, as committed), `bin/java-check --forms update`: every forms and data file byte
for byte, only `:repr-java` and `:host` changed in the provenance files. No re-seed; amd64 only.

### JN9e.7 The checks

On the final tool, one after another (`.tmp/jn9e/final.sh`), the host shared:

- `bin/repr-java attr-check --corpus all`: JN9e.4's first table, every corpus's forms and facts equal (3,518 s)
- `bin/repr-java nojavac-check --corpus all`: JN9e.4's second table (2,315 s)
- `bin/repr-java front-check`: every default corpus equal (433 s); `parse-check`: every corpus's trees as
  javac's (245 s)
- `bin/repr-java check`: as expected (29 s); `check --jdk`: 68 modules, 0 with failures, the 13,566 forms
  files as `test/repr/java/expected/jdk26u.sha256` (664 s)
- `bin/repr-java roundtrip --corpus baseline,samples,jdk --text`: as the references (the jdk corpus's 39
  known `:anew` rejections included; 672 s)
- the updates of JN9e.6
- `bin/gate` (essential): passes at `1ab8bf79` (the code and the provenance as final; the later commits
  change only `test/java/forms/provenance.edn` and `doc/`) in 17m04s: go-build 7m31s afresh, go-smoke,
  suite-go with no regression, oracle-go 52 known mismatches as recorded, diffcheck 12,789 of 12,801 cases
  agree with the 12 known, goi-check 801 of 801, goi-aot-essential 248 of 248, java-check 428 of 428,
  java-compiled, suite-aot-essential, programs 2 of 2, generated, j2g-edge verify, reap 12 of 12 cells
- not run: `bin/gate --full` (the user's rule), `bin/repr-java check --suite`, `bin/javac-vendor verify` and
  `classes` (the converted forms and javac's world unchanged: the updates show them byte for byte)

### JN9e.8 What it means

JFS69 (b)'s aim is reached for the corpora: the converter's front end is native, the parser and the
attribution, javac only the reference that checks it (attr-check, nojavac-check) and the fallback for a
file the port does not cover. What is left to javac is what no corpus asks: code that does not compile
(the provider follows javac for compiling code, JFS105 (a), JFS114 (a)), a generic thrown type of a call
its outer call checks, and javac's diagnostics. The corpora are jdk26u's own sources, the JDK's style;
the jtreg tests of `bin/java-check` (the language's corners, 10,000 and more files) are the next corpus
for the read without javac (JFS123).

### JN9e.9 Proposals

- **JFS120 The vendoring tools' read.**
  - Options: (a) as built: `bin/jrt-convert`, `bin/javac-vendor convert`, `bin/clojure-tests --java-fixtures`
    and `bin/repr-java check --jdk` read with javac's trees; the read without javac is checked beside them;
    (b) make `--no-javac` their read (the fallback counted and printed; javac still runs for a file that
    falls back), the provenance naming the read (`:read :native`); (c) the same with the fallback an error.
  - *Recommended: (b)*: every file of their corpora reads natively and byte for byte, the read takes a
    fifth of the memory (java.base: 7.6 GB against 38-40 GB), and the fallback keeps them correct where
    a later Java source meets a gap; (c) when a release's corpora have stayed at no fallback.
- **JFS121 nojavac-check in the gate.**
  - Options: (a) as built: by hand (`make repr-java-nojavac-check`); (b) in `bin/gate --full` beside
    attr-check (the default corpora, about 10 minutes on the final tool); (c) in place of attr-check.
  - *Recommended: (b)*: it is the check of what JFS120 (b) would ship; attr-check stays, as it is the one
    that judges the facts.
- **JFS122 The fallback.**
  - Options: (a) as built: per file, by reason, the file read again by javac in one run; (b) per unit's
    fact, javac attributing only the files that fall back but the native answers kept elsewhere in the
    unit (a mixed unit); (c) none: a decline an error (JFS111 (c)).
  - *Recommended: (a)*: a file is the unit javac's forms are judged by, a mixed unit would need
    javac's twin index again, and with no fallback in the corpora the fallback's cost is nil.
- **JFS123 The jtreg tests read without javac.**
  - Options: (a) as built: `bin/java-check --levels read` reads with javac's trees; (b) a level `read`
    with `--no-javac` (or a level `nread`): each test read without javac, its fallbacks counted by reason,
    the forms against javac's read; (c) leave it.
  - *Recommended: (b)*: the jtreg corpora (jdk's and langtools' tests, Arbace's JLS programs) are the
    language's corners the JDK's own sources do not exercise; their fallbacks are the next ports' list.
- **JFS124 The time without javac (JFS115 continued).**
  - Options: (a) as built: a read without javac is 1.3-1.9 times javac's read (jdk26u: 1,123 s against
    609 s on 4 threads); (b) cut what the profile shows (JN9e.4: the collector takes 60% of the CPU; string builders, function objects and lazy sequences most of the 182 GB allocated); (c) more threads (the
    memory now allows 8 or more chunks at once).
  - *Recommended: (c) then (b)*: (c) is a setting (by the 4-thread peak, a read at 8 threads should stay near twice 8.4 GB, still under half of javac's; to be measured), (b) is
    work on code that is now the read's own path.
- **JFS125 The memory.**
  - Options: (a) as built: the provider drops each unit's answers after its conversion; a read without
    javac peaks at 8.4 GB (jdk26u's modules, 4 threads), 7.6 GB for java.base's closure; (b) release the
    registry's parsed trees of the units already converted (only their declarations needed after).
  - *Recommended: (a)*: the peak is a fifth of javac's; (b) only if JFS124 (c)'s threads need it.
- **JFS126 A reader without javac's world.**
  - Options: (a) as built: repr/java runs on javac's tool executable (233 MB, javac's world translated),
    even without javac; (b) embed `arbace/repr/java` in the shipped program too, `read --no-javac` there,
    its fallback an error ("javac's tool needed") (the tool stays for the fallback and the checks); (c)
    leave the reader in the tool.
  - *Recommended: (b)*: it gives the REPL Java's reader (Java source loaded as Java forms) without
    javac's world, the step towards a self-sustaining Arbace; its cost is the shipped program's size.
- **JFS127 Code that does not compile.**
  - Options: (a) as built: the provider answers as javac does for compiling code; on code javac rejects
    it may answer where javac reports an error (no fallback triggers); (b) a read without javac checks
    the errors the converter's facts depend on (Attr's and Check's errors in the tiers ported) and falls
    back on them; (c) the diagnostics ported (JC3's golden files natively).
  - *Recommended: (a)*, with a note in NATIVE-ATTR.md: the vendoring reads compile, and the REPL's
    Java (JFS126 (b)) would be judged by its run; (c) is a project of its own.
- **JFS128 The known file's bridges.**
  - Options: (a) as built: `test/repr/java/attr-check-known.edn` still lists the five bridges of JN9b-JN9d,
    none of which bridged; (b) empty the list, so that any bridge fails attr-check.
  - *Recommended: (b)*: no corpus bridges any more, and a bridge is now a gap of a read without javac.
- **JFS129 Next.**
  - Options: (a) JFS120 (b), JFS121 (b), JFS123 (b), JFS128 (b), about a day; (b) JFS126 (b), a reader in
    the shipped program, after (a); (c) stop at JN9e.
  - *Recommended: (a) then (b)*: (a) makes the native read the vendoring tools' own and widens its
    corpus; (b) is what the native front end is for.

### JN9e.10 Sources and scratch

- jdk26u at `baf63fbe42b8758448fee570a9d9bb914272d259` (`/root/jdk26u`):
  `src/jdk.compiler/share/classes/com/sun/tools/javac/comp/{Flow,Check,Attr,TypeEnter,MemberEnter}.java`,
  `code/{ClassFinder,Types,Symbol}.java` (LICENSE.md, doc/ARBACE.md).
- The worktree's `.tmp/jn9e/` (scratch, not tracked): `q1`-`q4` (the reproductions: a type variable
  qualifier, record annotations, a multi-catch intersection, lambdas' thrown types; `q.sh`), `cy/` (the
  cycles' 8 files of jdk.javadoc, `run.sh`, `trace.log`), `n26a/`, `n26a.log` (the survey of jdk26u read
  without javac, before the cycles' port: the 8 fallbacks), `nj1.log`, `nj2.log` (the first nojavac-check
  runs), `ac1.log`-`ac4.log` (attr-checks of the samples and the baseline), `build1.log` (the final tool's
  build), `final/` (the final tool's checks: `summary.txt`, their logs, `attr.md`, `attr.edn`, `split.md`,
  `nojavac.md`, `nojavac.edn`), `prof/` (JFS115's profile: `cpu-top.txt`, `cpu-cum.txt`, `mem-inuse.txt`,
  `mem-alloc.txt`), `gate.log`, `snap.sh`, `run-nj.sh`, `nj26.sh`, `final.sh`, `prof.sh`.

## Decided (2026-10-27): JFS120-JFS129

The user decided JN9e's proposals as recommended, inside the milestone `arbace-reaps-3-crops-v2`: JFS120 (b)
(`--no-javac` the vendoring tools' read, the fallback counted, the provenance naming the read; (c) once a
release's corpora have stayed at no fallback), JFS121 (b) (nojavac-check in `bin/gate --full` beside
attr-check), JFS122 (a) (the fallback per file), JFS123 (b) (a read level of `bin/java-check` without javac,
fallbacks counted by reason, forms against javac's), JFS124 (c) then (b) (more threads, then less
allocation), JFS125 (a) (the 8.4 GB peak kept), JFS126 (b) (repr/java embedded in the shipped program,
`read --no-javac` there, a fallback the error "javac's tool needed"; in v2, with its re-seed), JFS127 (a)
(the provider assumes compiling code, as NATIVE-ATTR.md notes), JFS128 (b) (the known file's bridges
emptied), JFS129 (a) then (b): JN9f, JFS120 (b), JFS121 (b), JFS123 (b), JFS128 (b), then JFS126 (b).

## As built: JN9f (2026-10-27, 2026-10-28)

JN9f (JFS129 (a) then (b), decided with JFS120-JFS129 as recommended): JFS120 (b), JFS128 (b), JFS123 (b),
JCS114 (a) and JCS115 (a) (doc/classes/JAVA-CONFORMANCE-SURVEY.md, "Decided (2026-10-27): JCS110-JCS119"),
JFS121 (b), and JFS126 (b) as a design (below: more than a few days' work, so stopped after the design and its
proposals, as the decision's scope allowed), on the branch `java-jn9f` from main `b60d3075`, main merged at
`ee8ee304` (GT1-GT3, v2's small leftovers B); after the merge also RPS151 (a) (doc/FULL-SUPPORT-SURVEY.md,
"Decided (2026-10-28)": the converter keeps a constant's widening, the five deferred `:print-forms` tests) and
JCS114's binding cast (JN9f.5). One tool build
(2,537 s: the reader's key changed, so two builds, the first to compile the reader), after main's merge three
more (1,829 s, 1,747 s, 1,139 s: RPS151's updates moved `jdk/forms`, then `jdk/javac/forms`, from which the tool is
built, so each was read again by the tool built from them until nothing moved), the corpora's runs in the
shared heavy slots under the host's big-job lock. The JVM ran only the frozen v4 jar (javac's edge in
`bin/javac-vendor convert`, `bin/j2g-edge verify` in the gate); Go only TamaGo's go1.27.1, without cgo; the
tools' host the worktree's stage 0 (main's, the seed's).

**JCS114 and JCS115 are fixed at both run levels** (BoxedForeach, GenericRecordDeconstructionPattern and
TypedDeconstructionPatternExc pass interpreted and compiled: interp 1,790 to 1,793, compiled 1,881 to 1,884), **the
five deferred `:print-forms` tests pass the forms step** (RPS151 (a); the class is gone; Java print 2,597 / 683 / 41 to
2,598 / 687 / 36), and **nojavac-check is a step of `bin/gate --full`**. **The vendoring tools now read without javac**: every file of their corpora reads natively, no fallback,
and every forms file they write is byte for byte javac's read's (the committed ones, until RPS151 (a) changed the
converter for both reads, JN9f.7); the provenance names
the read (`:read :native`). **The bridges are gone from the known file**: attr-check passes with none.
**The jtreg tests read without javac**: of Java's conformance check's 3,321 run tests, 3,276 read natively
with javac's forms byte for byte (98.6%), 38 fall back to javac on a decline or a failure, 6 differ, 1 is not
read by javac either; their 44 are the next ports' list, by class.

### JN9f.1 What was built

| file | what |
|---|---|
| `bin/lib/repr-java/host.bash` | `repr_java_vendor_read` (native, the default; `REPR_JAVA_VENDOR_READ=javac` the read with javac's trees), `repr_java_vendor_read_opts` (`--no-javac`), `repr_java_nojavac_take` (the read's report moved out of the forms' directory); `repr_java_reader_hash` covers the native front end too (classfile, decls, expr, infer, lex, nattr, overload, parse, scope, sym, types): the vendoring tools' forms are now theirs |
| `bin/jrt-convert`, `bin/javac-vendor`, `bin/clojure-tests`, `test/repr/java/jdk.bash` | the read `--no-javac` (JFS120 (b)); each prints the read's "no javac:" line (files, native, fallbacks by reason; `check --jdk` the 68 modules' sum); the provenance's `:converter` gains `:read` |
| `arbace/repr/java.ae`, `decls.ae` | the native registry takes javac's `-cp` directories (`:class-dirs`: a class path's classes, the unnamed module's, after the platform's; their packages and ClassFinder's names): the Java fixtures name the runtime's declarations on the class path, and read natively |
| `test/repr/java/attr-check-known.edn` | `:bridges #{}` (JFS128 (b)) |
| `bin/java-check`, `test/java/check-nread.edn`, `test/java/divergences.edn`, `Makefile` | the level `nread` (JFS123 (b)): `nread_one`, the read without javac of each run test's sources (cached in `WORK/forms-nojavac` by the read's key), against the read level's forms byte for byte (the files' first lines, which name the test whose read filled the cache, aside): pass, fallback (the reasons, the "no javac:" map), differ (the first file), fail, unread; five classes `:nread-*`; `make java-check-nread`. The script bug JN9e found: `--forms update` makes its work directory, and the levels without interp or compiled need no executable (`rexe` none) |
| `arbace/repr/java/attr.ae`, `expr.ae`, `split.ae`, `doc/classes/NATIVE-ATTR.md` | the fact `:types/record-components` (JCS114 (a)): a record pattern's components' erased member types at its type (TransTypes' fullComponentTypes) and their declared erasures; javac's from the JCRecordPattern (`memberType` of each component), native from `component-types` (`record-component-erasures`) |
| `arbace/repr/java/convert.ae`, `model.ae`, `layout.ae` | JCS114 (a): `unconditional-by-args?`, a nested type pattern TransPatterns lets match null (its component's full type a subtype of its type, boxed) where the declared erasure would not, written `^:unconditional`; JCS115 (a): `m/iterable-arg` (Lower.visitIterableForeachLoop's iteratorTarget), a primitive for-each binding over another primitive's box written `^{:box C}` (the layout's item `[:box C]`) |
| `arbace/classes/analyze.ae` (shipped) | `analyze-pattern`: a component's type pattern `^:unconditional` is not tested; `analyze-for-each`: `^{:box C}` casts the element to C before the unboxing and widening |
| `doc/classes/SPEC.md` | §5.7's for-each and §5.8's patterns: the two marks |

### JN9f.2 JFS120 (b): the vendoring tools' read

The four tools pass `--no-javac` to `bin/repr-java read` and `read-jdk`; a file that meets what only javac
answers is read again by javac in the same run (JFS122 (a)), counted in the log's "no javac:" line, which each
tool prints. The updates on the final tool (key `da018f22...`):

| tool | files | native | fallbacks | forms against the committed |
|---|---:|---:|---:|---|
| `bin/jrt-convert update` (java.base's closure) | 906 | 906 | 0 | 982 of 982 equal |
| `bin/javac-vendor convert` (javac's world, 6 modules) | 816 | 816 | 0 | `jdk/javac/forms` unchanged (its edge too) |
| `bin/clojure-tests --java-fixtures update` | 12 | 12 | 0 | 15 of 15 equal |
| `bin/repr-java check --jdk --update` (68 modules) | 12,730 | 12,730 | 0 | `jdk26u.sha256` unchanged (13,566 files) |

(After main's merge and RPS151 (a) the four updates and `--forms update` ran again on the final tool, key
`74dfa6c26ac6`: every file still native, no fallback; what moved is JN9f.7's, and the provenance files name that
tool.) Before RPS151 only the provenance files changed: `:repr-java` (the reader's hash, now of the native front end too), `:read
:native`, `:host`. `bin/java-check --forms update` (the essential tests' forms) still reads with javac's trees
(it is the read level's, which nread is judged against): its 880 files unchanged, its provenance's hash and host
new. The verifies are the updates' comparisons (each tool's update compares with the committed tree before
writing it; the forms are the same bytes), and `bin/repr-java nojavac-check` (JN9f.4) reads the same corpora
both ways.

### JN9f.3 JFS128 (b): the bridges

`attr-check-known.edn`'s `:bridges` is empty: a model read answered by javac's symbol of a native declaration
fails attr-check. `bin/repr-java attr-check` (the default corpora) passes: no bridge, every fact equal (samples
3,759, baseline 339,088, jdk 1,279,085, javac 1,125,805), the forms equal; the model's reads 998 javac (the jdk
corpus's, as JN9e.3), none bridged.

### JN9f.4 JFS123 (b): the jtreg tests read without javac

`bin/java-check --levels read,nread --jobs 8 --record` over every corpus (533 s; the read level as recorded,
3,320 of 3,321):

| nread | tests |
|---|---:|
| pass (read natively, javac's forms byte for byte) | 3,276 (98.6%) |
| fallback (javac's forms, a file read by javac) | 38 |
| differ (the forms read without javac are not javac's) | 6 |
| unread (javac's read fails: `:read-*`, as recorded) | 1 |

By class (`test/java/divergences.edn`, the next ports):

| class | tests | what |
|---|---:|---|
| `:nread-differ` | 6 | Numbers (a concatenation's folded constants), TypeAnnotationReflection and T8008769 (a nested type's and a creation's type annotations), Pos03 (an anonymous diamond's type), LambdaCannotCapturePatternVariables (a lambda's return boxed), ExpressionSwitchBugsInGen (a switch expression's intersection cast) |
| `:nread-inference` | 20 | most specific of generic functional descriptors (MostSpecific15-27: 7), merged descriptors (LambdaConv16, LambdaTest6, BridgeMethod), an argument context (EarlyLocalTest8, T8058199, MethodReferenceIntersection1), an intersection target, a cycle (MostSpecific10), a choice absent or ambiguous (T6531075, T5073060a, MostSpecific29), an NPE in the overload tier (T6531090b) and in Switches' unconditional pattern, `no descriptor` (LostInterrupt) |
| `:nread-constant` | 7 | HotSpot's c1/c2 tests: a double constant beyond long's range cast to long (ConstFold saturates, the native fold throws) |
| `:nread-names` | 7 | a member class through a qualifier (`.Inner5.Inner6`, `X.Inner`), a qualified creation (`outer.new Inner()`: AnonymousInSuperCallTest, MissingCast2, T6569404a), `BiFunction` (AccessControlTest) |
| `:nread-scope` | 4 | `:scope/assigned` (two annotation tests), `:scope/capture` in an early construction context (SuperInitGood), a local class's number |

The corpora of nojavac-check read every file natively; the jtreg tests are the language's corners, and the
first ones beyond the port. The 6 differences are the more important: there the read without javac answers
where javac answers otherwise (nothing in a read without javac checks it), so a vendoring tool meeting such code
would write other forms; the fallbacks are safe (javac reads the file).

`bin/repr-java nojavac-check` (default corpora, 576 s): every corpus's forms equal, no fallback (samples 15,
baseline 183, jdk 906, javac 816 files); the jdk corpus's peak 10.9 GB with GOMEMLIMIT=24GiB set by the run
(JN9e: 7.6 GB at GOGC=100 alone: the limit lets the heap grow).

### JN9f.5 JCS114 (a), JCS115 (a)

- JCS114 (a): javac's TransPatterns tests a record pattern's nested type pattern against the component's full
  type (TransTypes' `memberType` at the pattern's type, erased) and lets it match null when that type is a
  subtype of the pattern's (boxed). The analysis knows the component's declared erasure (`Object` for a `T`),
  so `Box<String>(String s)` was an `instanceof String` test that a null component failed. The converter asks
  the new fact and writes `^:unconditional` where the full type says unconditional and the declared one does
  not; the analysis then does not test it.
- The binding's cast (after main's merge). Untested, the binding took any value: TypedDeconstructionPatternExc
  (t06376) passes a `Pair<Integer, Integer>` as a `Pair<String, Integer>` and expects javac's
  `ClassCastException` from the checkcast of the erased component (`String s = (String) $c`), and the
  interpreter bound the `Integer`, failing later at `s.length()` (`IllegalArgumentException: object of type
  java.lang.Integer is not an instance of java.lang.String`, the message the coordinator saw). The analysis
  marks such a pattern `:cast` (a binding, unconditional by the mark, not by the static type); the
  interpreter's `Pat` (`arbace/lang/Compiler.ae`, the field `checked`) casts the value to its class before the
  binding, outside the accessor's `MatchException` wrapping, with the JVM's message (`Rt/HOST`'s `checkCast`);
  j2g's binding was already a cast (`cast-val`). A null still matches.
- JCS115 (a): javac's Lower casts a for-each's element to the erased type argument of the Iterable before a
  primitive binding's unboxing (`for (float f : Iterable<Integer>)`: `(Integer) it.next()`, unboxed, widened);
  the analysis cast to the binding's own box (`Float`). The converter writes `^{:box Integer}` where the type
  argument's box is not the binding's, and the analysis casts to it. `for (int c : List<Character>)` had the
  same bug (a `ClassCastException` from `Character` to `Integer`).

Re-recorded on the gate's executable after main's merge (`bin/java-check --levels read,interp,compiled,print
--record --only`, the 21 tests whose forms RPS151 moved and these four, 490 s): interp and compiled t06032, t06354,
t06375 and t06376 pass (t06376 interpreted only with the binding's cast; t06375 passed already); none of the 25
regressed at any level; `levels-known.edn` as recorded. `test/java/divergences.edn`: `:boxed-foreach` and
`:record-pattern-findings` removed (their tests pass at every run level); t06376 at the print level is now in
`:print-erasure` (the printer writes `switch ((Pair<String, Integer>) (Object) x)` raw, `(Pair) (Object) x`, so javac
infers no type argument for `case Pair(String s, Integer i)` and the read gives no `^:unconditional`: Java's type
arguments are not in the forms, RPS9).

### JN9f.6 JFS121 (b): nojavac-check in `bin/gate --full`

`test/gate/steps.edn` declares the step `repr-java-nojavac` beside `repr-java-attr` (phase `:full`, chain
`java`, after `javac-tool`; hosts `:javac` and `:stage0`, the platform's pin, inputs `:lib :tools
:repr-java`, `:heavy`, `:mem-gib 11`: the jdk corpus's 10.8 GB peak in JN9f.9's run, at the `GOMEMLIMIT=24GiB` the run sets, where JN9e measured 8.4 GB at `GOGC=100` alone, JFS125 (a); its summary the corpora's lines, the
`nojavac:` lines and the verdict); `bin/gate`'s list of steps names it, GATE-SURVEY.md §"The table" lists it
in the java chain. `bin/gate --full --plan` lists it (JN9f.9).

### JN9f.7 RPS151 (a): a constant's widening kept

The five deferred `:print-forms` tests (doc/FULL-SUPPORT-SURVEY.md, "As built: v2 small leftovers B") were
three shapes where the forms leave a conversion to the analysis that the printed Java's literal types
otherwise: the printer writes an integer form as an `int` literal when it fits, so a `long` constant read back
as an `int` one.

| test | Java | forms before | after |
|---|---|---|---|
| t03822 TestStoreSunkToOuterLoop | `f3 += -2061721519L` | `(unchecked-add-float f3 -2061721519)` | `... (float -2.061721519E9)` |
| t03458 Tester (cr6714694) | `double var_72 = (double) 3858573493713776640L` printed `= 3858573493713776640L` | read back `^double var_72 3858573493713776640` | `(double 3858573493713776640)`, no tag |
| t06350 DeconstructionDesugaring | `new R6(0L, 0)`, `R6(Object, Object)` | `(R6. 0 (Integer/valueOf 0))`, printed `new R6(0, ...)`: an `Integer` | `(R6. (Long/valueOf 0) ...)` |
| t06270 TargetType05, t08323 BackEnds | `return c ? 1 : x;` of an `Integer` result | `(if c 1 x)`, printed as an `if` whose `return 1;` boxes alone | `(Integer/valueOf (if c 1 x))` |

`coerce` (`arbace/repr/java/convert.ae`): a `long` constant widened to `float` is `(float D)` (D its double,
when the double holds it exactly; else `(float N)`), to `double` the double in `int`'s range (an `int`
literal's widening) and `(double N)` beyond it (the printer's literal then reads back to it); a `long`
constant into a reference other than `Long` is boxed; a primitive conditional with a literal branch
(`literal-branch?`) into a reference is boxed whole, as javac's Lower boxes it. SPEC.md §5.5 states them.

What moved (the vendoring updates on the fresh tool, every file read natively): `jdk/forms` 3 files of 982
(CompactNumberFormat, Parsed, Collectors: a `(Long/valueOf 1)` into a `List<Long>`, `^Long (Long/valueOf (if
...))`, a lambda's `(Double/valueOf (if ...))`), `jdk/javac/forms` 3 of 903 (ExhaustivenessComputer,
AbstractDiagnosticFormatter, XMLSecurityManager: `(Integer/valueOf (if ...))`), `test/repr/java/expected/
jdk26u.sha256` 30 of 13,566, the essential tests' forms 1 (t06270), the Java fixtures none; of the jtreg run
tests' forms 21 of 3,321 (the five, HotSpot's generated c2 fuzz tests t03453-t03457 and t03567, the c1/c2
tests t04571, t04771-t04774, t05412, t05696-t05714, and t06402). Each is the same value with its conversion
written; `jdk/forms` is the shipped world's (a re-seed carries it).

### JN9f.8 JFS126 (b): the reader in the shipped program, a design

JFS126 (b) decided embedding `arbace/repr/java` in the shipped program, `read --no-javac` there, a fallback
the error "javac's tool needed". What the read needs at run time without javac's world:

1. **The code.** The read's namespaces (`arbace.repr.java` and its parts) name javac's classes in 11 of them:
   type hints, `instance?` tests that dispatch a fact between the two models (`(instance? Type t)`, `JCTree`),
   static fields (`Flags/...`, `TypeTag/...`), imports. In a read without javac none of these objects exist,
   but c2j must resolve every class symbol, static field and hinted member when it compiles the namespaces,
   and the shipped program holds none of javac's classes. Counted: tree.ae 72 distinct javac names, jtree.ae
   68 (javac's tree as data: not needed), convert.ae 68, attr.ae 28, nattr.ae 7, model.ae 7, javac.ae 6
   (javac's driver: not needed), resolve.ae 4, jtypes.ae 3, expr.ae 3, java.ae 3; the native front end (lex,
   parse, decls, scope, sym, types, overload, infer, classfile) none.
2. **The platform.** The native provider reads the platform's declarations from class files
   (`arbace.repr.java.classfile` over the exploded modules `REPR_JAVA_PLATFORM` names: jdk26u's build tree,
   27,598 class files, java.base's 34 MB). The shipped program has none of them.
3. **The size.** The compiled reader is 24 MB of Java forms (`target/arbace-go/aot-reader`, 132 files; the
   shipped program's compiled namespaces 78 MB): by JN5's measure (8.4 MB of forms, +9.4 MB of executable)
   about +27 MB, +16% of the 174.5 MB shipped program, the printer included.

The design has three parts, each its proposal below: the read's javac half made separable (JFS130), the
platform the shipped reader reads (JFS131), and the command and the REPL's entry (JFS132). Together about
four to six days; none of it is built.

### JN9f.9 The checks

On the final tree (commit `2a2925fc` plus this record), in the worktree, under the host's big-job lock:

- `bin/gate` (the essential gate, keyed: 17 steps run, none skipped, the runtime having changed): passed in
  12m36s; Java's essential interp 428 of 428, essential-compiled 423 of 423, `bin/reap` 12 of 12 cells, 0
  problems (Java eval 1,793 / 329 / 1,199, AOT 1,884 / 181 / 1,256, print 2,598 / 687 / 36).
- `bin/repr-java nojavac-check` on the final tool: every corpus's forms equal, no fallback (samples 15, baseline
  183, jdk 906, javac 816 files; 540 s); the jdk corpus's peak 10.8 GB.
- `bin/gate --full --plan`: `repr-java-nojavac` planned to run beside `repr-java-attr` ("never passed").
- The vendoring updates on the final tool (`bin/jrt-convert update`, `bin/javac-vendor convert`, `bin/clojure-tests
  --java-fixtures update`, `bin/java-check --forms update`; `bin/repr-java check --jdk --update` on the tool before
  the last two rebuilds, its reader the same) and `bin/jrt-convert verify` on the second: as JN9f.7 lists.
- `bin/java-check --levels read,nread --record` over every test (591 s, on the second tool): read 3,320 of 3,321,
  nread 3,276 / 38 / 6 / 1, both as recorded; the levels read, interp, compiled and print of the 25 tests of JN9f.5
  and JN9f.7 (490 s).
- Not run: the rest of `bin/gate --full` (javac on Arbace's checks, the roundtrips, attr-check, the whole
  conformance check at interp, compiled, classes, diagnostics and print), which the coordinator runs serially at
  the merge; attr-check and front-check last ran before main's merge (JN9f.3), the converter's change since
  touches no fact.

### JN9f.10 The shipped program's change

`arbace/classes/analyze.ae` (JCS114 (a), JCS115 (a): the two marks honoured, `:cast`), `arbace/classes/
interp.ae` (the `Pat`'s `checked`), `arbace/lang/Compiler.ae` (the runtime: `Pat`'s field and cast; with
main's merge the runtime key changes), `jdk/forms` (3 files, RPS151 (a)); `arbace/repr/java` and
`jdk/javac/forms` are javac's tool's. The shipped program 174,506,144 bytes; javac's tool 265,015,456 (build1)
and 265,011,360 (the final build, key `74dfa6c26ac6`). A re-seed carries the runtime's and `jdk/`'s change; amd64 only.

### JN9f.11 Proposals

- **JFS130 The read's javac half (JFS126 (b), part 1).**
  - Options: (a) split each namespace that names javac into its native part and a javac part loaded only on
    the tool (`arbace.repr.java.javac-model`, `javac-tree`: the `instance?` branches, the hinted calls, the
    imports), the native part asking the javac part through a protocol whose default (no javac part loaded)
    is `javac-needed!`; jtree.ae and javac.ae stay the tool's; (b) stub declarations of the ~110 javac
    classes the read names, printed from `jdk/javac/forms` by the declarations layout (bodies `throw null`),
    in the shipped world only, so that c2j resolves them; (c) leave the reader in the tool.
  - *Recommended: (a)*: the facts already dispatch on the model (`attr`'s `native?`, `model`'s `nsym?`), so
    the split follows a line the code has; (b) is cheaper (a day against three) but ships dead javac names
    and two worlds' classes under one name, which the closed world's checks would have to tell apart.
- **JFS131 The shipped reader's platform (part 2).**
  - Options: (a) a directory given at run time (`REPR_JAVA_PLATFORM`, as on the tool): not standalone;
    (b) a declarations-only platform embedded (class files without code, as ct.sym's: java.base's 34 MB to
    some 6-8 MB, the 68 modules several times that), read by `classfile` as now; (c) the provider reading the
    runtime's own Java forms (`jdk/forms`, what jrt has) for the declarations, a reader beside `classfile`:
    javac's answers then of jrt's world, not jdk26u's.
  - *Recommended: (b) for java.base alone, (a) for the rest*: the REPL's Java is java.base's in the shipped
    world (jrt is java.base's closure), the answers stay jdk26u's (attr-check's reference), and the size is
    measured before the other modules are considered; (c) gives answers no check compares.
- **JFS132 The command and the REPL (part 3).**
  - Options: (a) `arbace -m arbace.repr.java read --no-javac OUT ROOT` on the shipped program and
    `(load-java "Foo.java")` at the REPL (the forms loaded as Java forms are, `arbace.classes`), a fallback
    the error "javac's tool needed: REASON"; (b) the command alone; (c) the REPL alone.
  - *Recommended: (a)*: the REPL's entry is what JFS126 is for; the command is the check's (nread's forms
    against the tool's, on the shipped program).
- **JFS133 The jtreg tests' 44 (JFS123 (b)'s nread).**
  - Options: (a) the next ports by class, the 6 that differ first (there the native read answers where javac
    answers otherwise, unchecked), then `:nread-inference` (20), `:nread-names` (7), `:nread-constant` (7),
    `:nread-scope` (4); (b) leave them falling back (safe: javac reads the file).
  - *Recommended: (a), the 6 first*: a difference is a wrong answer a shipped reader (JFS130-JFS132) would
    give without a fallback; the fallbacks can wait.
- **JFS134 Next.**
  - Options: (a) JFS133 (a)'s 6 differences, then JFS130-JFS132 as JN10, about a week; (b) JFS130-JFS132
    first; (c) stop the native front end here.
  - *Recommended: (a)*: the shipped reader should start from a read without known wrong answers.

### JN9f.12 Sources and scratch

- jdk26u at `baf63fbe42b8758448fee570a9d9bb914272d259` (`/root/jdk26u`):
  `src/jdk.compiler/share/classes/com/sun/tools/javac/comp/{TransPatterns,TransTypes,Lower,Attr}.java`
  (visitRecordPattern, unrollRecordPattern, visitIterableForeachLoop, visitConditional's boxing), `tree/JCTree.java`
  (LICENSE.md, doc/ARBACE.md).
- The worktree's `.tmp/jn9f/` (scratch, not tracked): `q1/` (the reproduction of JCS114/JCS115: `src/Q.java`,
  its forms read with javac, without and both), `fx/` (the fixtures read without javac before the update),
  `build.sh`, `build1.log` (the tool), `phase2.sh`, `p2/` (repr-java check, java-check read,nread, nojavac-check,
  attr-check), `phase3.sh`, `p3/` (nread again, the updates), `phase4.sh`, `p4/` (the gate, the four tests at
  interp and compiled; the compiled run that died on the full disk), `pr/` (the five `:print-forms` tests'
  forms printed by hand, RPS151's diagnosis), `nav.ae` (a form at a path), `oldforms.txt`, `moved-tests.txt`
  (the 21 tests whose forms RPS151 moved), `phase5.sh`-`phase7.sh`, `p5/`-`p7/` (the tool builds, the updates,
  the read levels, the levels of the moved tests), `jfs126.md` (the design's draft);
  `.tmp/java-check/forms-nojavac/` (the nread level's reads), `.tmp/repr-java-nojavac-check/nojavac.md`,
  `.tmp/repr-java-attr-check/attr.md`.

## Decided (2026-10-28): JFS130-JFS134

The user decided JN9f's proposals as recommended: JFS130 (a) (each reader namespace that names javac split into its
native part and a javac part loaded only on the tool, through a protocol whose default is `javac-needed!`), JFS131
(b) for java.base and (a) for the other modules (a declarations-only java.base embedded, the rest from a directory at
run time), JFS132 (a) (`read --no-javac` on the shipped program and `load-java` at the REPL), JFS133 (a) (the 44
fallbacks ported by class, the 6 that differ first), JFS134 (a) (the 6 differences first, started 2026-10-28 beside
v2 on the branch `java-jfs133`; then JFS130-JFS132 as JN10, after `arbace-reaps-3-crops-v2`).

## As built: JFS133 (a), the six differences (2026-10-28)

JFS133 (a), decided by the user 2026-10-28: the six jtreg tests that the read without javac (`--attr native`,
the level `nread` of `bin/java-check`) read to forms other than javac's, each a wrong native answer, unchecked
until these tests. Each one's fact where the native provider answered otherwise than javac, and its fix (the
provider's files only: `arbace/repr/java/{decls,expr,infer,overload}.ae`; no converter, no tool change).

| Test | The fact | The fix |
|---|---|---|
| t03215 `jls/Numbers.java` (a concatenation `-7.5 % 2 + " " + ...`) | `:types/constant`: the double remainder was not folded (`fold-binary` had no `:MOD` for doubles, so the concatenation of constants had none, and `flatten-concat` flattened what javac keeps as one constant operand); the float case used `Math/IEEEremainder`, which is not Java's `%` | `decls.ae`: `java-rem`, Java's `%` on doubles (C's fmod: exact, the dividend's sign, NaN for a zero divisor or an infinite dividend, the dividend for an infinite divisor) over BigDecimal; used for the double and float cases |
| t04723 `TypeAnnotationReflection` (`@TypeAnno("O") Outer<..>.@TypeAnno("I") Inner<..> foo2()`) | `:names/type-tree` of a method's result and of a field's type: a declaration annotation that is a type annotation went on the innermost written type, javac's `TypeAnnotations.typeWithAnnotations` puts it on the outermost enclosing type written in the source (the walk over the type tree: a selection steps to the enclosing type, a parameterized or annotated type to its class, and a selection beyond the nesting leaves the type) | `decls.ae`: `with-annotations` takes the type tree (`vartype-of`, the method's result), `annotate-outer` rebuilds the chain; the three-argument form is new, the old callers (a union's first alternative, arrays) keep their two |
| t06002 `T8008769` (`Test<@A @B String> t0 = new Test<>()`) | `:names/type-tree` of a diamond's class tree: the inferred type kept the target's type annotations, javac's `UndetVar.addBound` stores `bound.map(toTypeVarMap).baseType()`, so an inferred type carries none of its own (it made the converter write `(new (Test ..))` for javac's `(Test.)`) | `infer.ae`: `add-bound!` takes `base-type` (the type without its own annotations, those of its type arguments stay) |
| t06116 `Pos03` (`p.new Foo<>(1){}`, a qualified anonymous diamond) | the anonymous class's supertype (`anon-creation-type`): the qualified branch answered the member class raw (`Foo`), javac's is the diamond's inferred type (`Foo<Integer>`) | `decls.ae`: a diamond with a qualifier takes `*anon-diamond-type*` like an unqualified one |
| t06357 `LambdaCannotCapturePatternVariables` (`return (y instanceof Integer z) ? z : 1;` of a lambda returning Integer) | `:types/expr` of the conditional: `Attr.isBooleanOrNumeric` attributes an arm speculatively in the conditional's own environment, where the conditional's pattern bindings are not in scope (visitConditional enters them for the real attribution only): `z` is erroneous there, so the conditional is a poly expression (typed by its target, each arm boxed), not a numeric standalone one (`int`, `z` unboxed) | `expr.ae`: `*cond-top*`, `binding-ident?`, `inner-binding?` and `spec-erroneous?` (a name of a binding, a selection, call, operator or index on one: an error type, as `Operators.resolveBinary` answers noOpSymbol for an erroneous operand): such an arm is not numeric; any other arm that names a binding of the conditional (an argument of a call) is declined (the fallback), the bodies of lambdas and classes not entered (attributed after the choice) |
| t06405 `ExpressionSwitchBugsInGen` (`(switch ..).toString()` on `String`/`StringBuilder`) | `:overload/method`: an intersection (the lub `Object & Serializable & Comparable & CharSequence`) was looked up as an interface; javac's `Types.makeIntersectionType` gives its symbol ABSTRACT, PUBLIC, SYNTHETIC, COMPOUND and not INTERFACE, so `findMethod` runs the class chain first, finds `Object.toString` concrete, and keeps it over `CharSequence.toString` (the CR 6178365 branch); native found `CharSequence.toString` and cast the qualifier to `CharSequence` too | `overload.ae`: `find-method*`'s `iface?` is false for an intersection; its decline "an intersection's Object method" is gone (those calls are now answered) |

The first full run found three regressions of the fifth fix (t03211 `jls/Generics`, t06349 `BindingsTest1`,
t06354 `GenericRecordDeconstructionPattern` fell back where they had passed: `s.length() > 0`, `s == null ? 1 :
s.length()`, a call on a binding's selection): the first version declined every arm naming a binding. The
rule above (`spec-erroneous?`) answers them, as javac does. Not found by the 45 tests of the `:nread-*` classes:
only by the whole level.

### JFS133 (a): the numbers

| Level `nread` (3,321 tests, 3,322 after t08324 joined the corpus) | pass | fallback | differ | unread |
|---|---|---|---|---|
| before (JN9f) | 3,276 | 38 | 6 | 1 |
| after | 3,282 | 39 | 0 | 1 |

The 39th fallback is t08324 `jls/BackEnds2`, a test the committed `check-nread.edn` did not hold yet ("declined
:infer/target: a functional interface's descriptors merged", the class `:nread-inference`); the 38 of the
`:nread-*` classes are as before (20 inference, now 21 with it; 7 constant, 7 names, 4 scope). `:nread-differ` has no test now and stays as
a guard (`test/java/divergences.edn`: its title and why say so). The read level: 3,322 as recorded, none
changed.

### JFS133 (a): the checks

- `bin/java-check --levels read,nread` on the six: 6 differ before, 6 pass after (each fix on its own test
  first, `.tmp/jfs133/run0.log` to `run3.log`).
- The 45 tests not passing in `test/java/check-nread.edn`, then the whole level (`--jobs 8`, under the big-job
  lock; `.tmp/jfs133/full.log` with the three regressions, `full2.log` after their fix): 0 regressions, 3,282
  pass; `--record` rewrote `test/java/check-nread.edn` (the six now `:pass`, t08324 new).
- `bin/reap`: 12 of 12 cells checked, 0 with no check yet, 0 problems (`.tmp/jfs133/reap.log`).
- The essential gate in the worktree under the lock (`.tmp/jfs133/gate.log`): passed (43 min, 8 steps run, 9 skipped).
- Not run: `bin/repr-java attr-check` and `nojavac-check` over jdk26u's 68 modules (hours; the lock was busy
  for most of a day). The whole `nread` level (3,321 tests of jdk26u's tests, the jls programs and javac's
  sources) is the check that ran. The fixes touch four facts the corpora's code uses (a double remainder, a
  declaration annotation on a nested type, an inferred type's annotations, an intersection's methods): the
  next `bin/gate --full` runs `nojavac-check` and will say.

Where the provider's code is shipped: `arbace/repr/java` is embedded by `bin/arbace-go --build-java` in javac's
tool (not shipped) and evaluated from the checkout on the tools' host; it is inside `arbace_javac_key` (so the
tool built before this change is stale for the provenance writers: `REPR_JAVA_STALE_OK=1` was used for these
runs, the tool's reader being replaced by the checkout's via `ARBACE_PATH`) and outside the gate key of
`bin/arbace-go` (`gate_key` excludes repr/java). The shipped program does not carry the native provider yet
(JFS130-JFS132, JN10).

Scratch: the worktree's `.tmp/jfs133/` (not tracked): `run0.log` (before: 6 differ), `run1.log`-`run5.log`
(each step), `q1/` (the intersection reproduction `src/Q.java`, read with and without javac), `names.txt`,
`full.log`, `full2.log`, `chain.sh`, `reap.log`, `gate.log`; `.tmp/java-check/forms-nojavac/` (the diffs).

### JFS133 (a): proposals

None open. JFS133 (b)'s rest (`:nread-inference` 21, `:nread-names` 7, `:nread-constant` 7, `:nread-scope` 4)
stays as JFS133 lists it.

## JN10: the plan (JFS130-JFS132) (2026-10-29)

A plan only: written after reading the code, with no build, no check and no lock taken. JN10 itself starts after the
tag `arbace-reaps-3-crops-v2`, on a branch of its own from main. JFS126 (b) (decided 2026-10-27: the reader in the
shipped program, `read --no-javac` there, a fallback the error "javac's tool needed") was designed in "As built:
JN9f" (JN9f.8) as three parts, and the user decided them as recommended ("Decided (2026-10-28): JFS130-JFS134"):
JFS130 (a), the read's javac half split off; JFS131 (b) for java.base and (a) for the rest, the platform; JFS132 (a),
the command and the REPL's entry. JFS133 (a)'s six differences are built (the section above), so the reader starts
from a read with no known wrong answer: `bin/java-check --levels read,nread` 3,282 pass, 39 fallbacks, 0 differ.

Read for this plan: `arbace/repr/java.ae` (`convert-one`, `convert-units`, `convert-units-nojavac`,
`registry-units`, the command), the 11 namespaces that name javac, `decls.ae`'s platform (`platform-index`,
`registry`, `package-exists?`, `package-class-names`, `platform-class`), `classfile.ae` (`read-file`),
`bin/arbace-go` (`reader_key`, `reader_compile`, `build_java`, `gate_key`), `bin/lib/version.bash`,
`bin/lib/javac-key.bash`, `bin/lib/repr-java/host.bash` (`repr_java_reader_hash`), `bin/provenance-check`,
`arbace/j2g/embed.ae` (`excluded`), `test/gate/steps.edn`, `bin/repr-java`, `bin/lib/repr-java/front-check`.

### JN10.1 What the reading found

- **javac's names, counted by site** (type hints, `instance?` tests, static fields, imports): `model.ae` about 140
  (81 hints, 59 `instance?`, 61 uses of `jt/`: the javac branch of every read), `attr.ae` about 80, `nattr.ae` about 75,
  `tree.ae` about 34 (its 60-odd imported tree classes), `convert.ae` about 46 (40 of them statics, `Flags/...`
  constants), `resolve.ae` 18, `jtypes.ae` 29 (it only holds javac's `Types`, `Symtab`, `Check`, `Names` as dynamic
  vars), `expr.ae` 9, `java.ae` 7 (`convert-one`'s `JCCompilationUnit`, `convert-units`' `Context`); `jtree.ae` and
  `javac.ae` are javac's alone. The native front end (`lex`, `parse`, `decls`, `scope`, `sym`, `types`, `overload`,
  `infer`, `classfile`) names none, `names`, `layout`, `full`, `forms` none, `print` only a comment. About 440 sites.
- **The dispatch is already there.** The converter reads the tree through `tr/` (either tree), attribution through
  `attr/` (`native?`) and symbols and types through `model/` (`nsym?`, `ntype?`, `javac-of`); in a read without javac
  `convert-unit` is already called with a nil javac unit (`convert-units-nojavac`). `decls/javac-needed!` and
  `javac-needed-reason` exist. So the split moves javac's branches; it does not add a model.
- **The fallback is `convert-units`.** `convert-units-nojavac` ends in `(convert-units ... :javac)`, which needs
  `javac/analyze`: that call is the one place the shipped program must replace by the error.
- **The platform is plain files.** `decls.ae` reaches the platform through `java.io.File` in four places
  (`platform-index`, `package-exists?`, `package-class-names`, `platform-class`) and `classfile/read-file`; the
  native provider reads no `module-info` (it is only skipped in a package's listing). So an archive behind the same
  four calls is enough. java.base's exploded directory is 34 MB, 27,598 class files in all 68 modules.
- **The tool compiles the reader on itself** (`reader_compile`: c2j on a first tool build, because c2j must resolve
  javac's classes); once the native part names none, the shipped build's own c2j run compiles it, and the tool only
  needs the javac half compiled on it.
- **Every key that lists repr/java as "not embedded"** has to change: `arbace/j2g/embed.ae` `excluded`,
  `arbace_embedded_sources` and `arbace_reader_sources` (`bin/lib/version.bash`), `gate_key`'s and `reader_key`'s
  readings, steps.edn's `:tools` group, `repr_java_reader_hash`'s file list (new files).

### JN10.2 The design in one paragraph

`arbace.repr.java` is cut in two. The **read closure** (the namespaces `java.ae` needs to read, minus javac's: `java`,
`convert`, `forms`, `full`, `layout`, `names`, `tree`, `attr`, `model`, `nattr`, `resolve`, `expr`, `decls`, `infer`,
`lex`, `overload`, `parse`, `scope`, `sym`, `types`, `classfile`, a new `platform`, and `javac-hook`) names no
javac class and is embedded in the shipped program like any other namespace. The **javac half** (`javac`, `jtree`,
`jtypes`, the new `javac-tree`, `javac-model`, `javac-read`, and `attr`'s index of javac nodes) stays on javac's tool,
loaded there, and installs itself into `arbace.repr.java.javac-hook`, an atom of functions whose default for every
entry is `(decls/javac-needed! "NAME")`. A branch of the closure tests the native model first (`map?`, `nsym?`,
`ntype?`: the common case, as now) and otherwise calls the hook; on the tool the call is the javac branch that was
there, on the shipped program it is `javac-needed!`, which `convert-units-nojavac` already turns into a fallback and
which the shipped command turns into the error "javac's tool needed: REASON". The platform behind `registry` becomes
a small interface (`index`, `list`, `exists?`, `bytes`) with two implementations: a directory (as now, the other
modules, `REPR_JAVA_PLATFORM`) and a packed declarations-only archive (java.base, embedded); `platform-index` merges
them.

### JN10.3 Part 1: JFS130 (a), the read's javac half

**Files that change** (all under `arbace/repr/`; a count of sites in JN10.1):

| file | change |
|---|---|
| `java/javac_hook.ae` (new `arbace.repr.java.javac-hook`) | the atom `hooks` (`{:tree-of ... :model-of ... :index ...}`), `(call! name & args)` defaulting to `javac-needed!`, `(install! m)`; no javac name |
| `java/tree.ae` | the 60-odd `JCTree$...` imports and the `instance? JCTree` branches of `tag`, `f`, `start`, `end`, `children`, `skip-parens`, `tree-name`, `full-name`, `diamond?` move to `java/javac_tree.ae` (new), installed as the hook's tree entries; `tree.ae` keeps the native (map) branch |
| `java/attr.ae` | `attr/index` (the pairing of native nodes with javac's by tag and position), the `Symbol`/`Type` answers and `*measure*` stay the provider's; the javac-answered branch (`provider` nil) and the 55 hints go to `javac_model.ae`/`javac_tree.ae`; with `*provider*` bound the closure never reaches them |
| `java/model.ae` | the 59 `instance?` branches and `javac-of`, `jt/` reads move to `java/javac_model.ae` (new, installed); `nsym?`, `ntype?`, `native?` and the native branch stay; the 10 `javac-of` bridges (reads of a javac symbol of the same declaration) are the hook's, so on the shipped program they fall back |
| `java/nattr.ae` | the 52 hints and 24 `instance?` tests are for the javac-side facts it checks against (`check`, `diffs`): the checking half (`attr-check`, `check-platform-members!`'s javac comparison) moves to the javac half; `provider` and the native answers stay |
| `java/convert.ae` | 40 static `Flags/...`, `TypeTag/...` uses become numeric constants in a new `java/jflags.ae` (javac's `Flags` values the converter reads, with a load-time assertion on the tool that each equals javac's), the 4 hints and 2 `instance?` go to the hook |
| `java/resolve.ae`, `expr.ae`, `jtypes.ae` | `resolve.ae`'s javac reads (`Check.getCompiled`, `CompletionFailure`) to the hook; `expr.ae`'s `JCNewClass` twin lookup (line 406) to the hook; `jtypes.ae` is javac's alone (its dynamic vars are bound by `convert-units`) and leaves the closure: the closure's `jt/` references (model, nattr, resolve, expr: 77 sites) go with their javac branches |
| `java.ae` | `convert-one`, `native-units`, `convert-units` (javac's `analyze`, the `Context` bindings) move to `java/javac_read.ae` (new; the tool's `read` default and `--front`, `--attr`, `--split`, `--javac`, `read-jdk`'s javac path); `convert-units-nojavac`, `registry-units`, `write-output`, the option parser stay; `convert-units-nojavac`'s fallback calls the hook (`:convert-units`), whose default is the error; the command loads `javac_read` when it exists (try `require`, tool only) |
| `bin/lib/repr-java/host.bash` | `repr_java_reader_hash`'s file list gains `javac_hook`, `javac_tree`, `javac_model`, `javac_read`, `jflags`, `platform`; the header text names them |
| `bin/arbace-go` | `reader_compile` compiles the javac half only (the closure is the base's: `--aot-base`); `reader_key` still hashes all of `arbace/repr/java`; `build_java`'s `--embed arbace/repr/java` names the javac half's files |
| `arbace/j2g/embed.ae`, `bin/lib/version.bash` (`arbace_embedded_sources`, `arbace_reader_sources`), `gate_key` (reads `arbace_embedded_sources`), `test/gate/steps.edn` (`:tools`) | repr/java's read closure is no longer excluded; the javac half, `print`, `roundtrip`, `coverage`, `split` (unless the closure needs it) still are |

**Steps, in order.**

1. The inventory: `grep` the 11 namespaces for `com.sun.tools.javac` and `javax.tools` and write `javac-sites.txt` (scratch);
   fix the closure's file list (the require graph of `java.ae` less the javac half). Add the light guard
   `bin/repr-java closure-check` (the closure's files name no javac class; seconds; Makefile target `repr-java-closure-check`).
2. `javac_hook.ae` and `jflags.ae`; the load-time assertion of `jflags` on the tool.
3. `tree.ae`, then `attr.ae`: the two interfaces everything reads through. Tool check after each (`bin/repr-java
   parse-check --corpus samples,baseline`, `front-check --corpus samples,baseline`).
4. `model.ae`, `nattr.ae`, `resolve.ae`, `expr.ae`, `convert.ae`: one file at a time, `attr-check --corpus samples,baseline`
   after each.
5. `java.ae` and `javac_read.ae`; `bin/arbace-go --build-java` (two builds: the key changes; the first compiles the
   javac half on a tool built with the closure from the base); `bin/repr-java check --suite`.
6. The shipped side: `embed.ae`, `version.bash`, `gate_key` readings, steps.edn; `bin/arbace-go --gate-build` compiles
   the closure with the shipped namespaces (a javac name left in it is a c2j error naming the file: the guard of the
   build itself); `bin/arbace-go --smoke` plus a `(require 'arbace.repr.java)` line in the smoke script.

**Checks that prove it** (the tool's results unchanged, byte for byte, is the whole claim of the part):
`bin/repr-java parse-check`, `front-check`, `attr-check`, `nojavac-check` (every corpus, their known files unchanged:
`test/repr/java/*-check-known.edn`), `bin/repr-java check --suite` and `check --jdk` (against
`test/repr/java/expected/jdk26u.sha256`: not one forms file moves), `bin/java-check --levels read,nread` (3,282 / 39 / 0
as recorded, `test/java/check-nread.edn`), `bin/javac-vendor check --from` and `verify`, `bin/provenance-check`, the
essential `bin/gate`.

### JN10.4 Part 2: JFS131, the shipped reader's platform

**Files.**

| file | change |
|---|---|
| `arbace/repr/java/platform.ae` (new) | the interface (`index`, `package-dir?`, `list-names`, `exists?`, `bytes`) and its two implementations: `dir-platform` (the code `decls.ae` has now) and `pack-platform` (an archive resource); `(composite & ps)` for java.base's pack and `REPR_JAVA_PLATFORM`'s directory (JFS131: (b) for java.base, (a) for the rest); the pack's writer `pack!` and reader; a pack read once, class entries inflated on demand (`arbace/j2g/zlib.ae`'s DEFLATE is j2g's; the runtime's `java.util.zip.Inflater` is jrt's port) |
| `arbace/repr/java/decls.ae` | `platform-index`, `package-exists?`, `package-class-names`, `platform-class` call `platform.ae` instead of `File`; `registry`'s `:platform` is a platform object; `read-class-file` reads `(platform/bytes p path)`; `classfile/read-file` stays for the class path's directories |
| `arbace/repr/java/classfile.ae` | `parse` unchanged; a `strip` function (drop `Code`, `LineNumberTable`, `StackMapTable`, `LocalVariable*`, debug attributes; keep what `parse` reads: flags, `Signature`, `InnerClasses`, `ConstantValue`, `Exceptions`, the annotations, `Record`, `PermittedSubclasses`, `Deprecated`), so that `(parse (strip b))` equals `(parse b)` |
| `arbace/repr/java.ae`, `registry-units` | the registry's platform is the composite: java.base's embedded pack, then `REPR_JAVA_PLATFORM` if set (its java.base ignored); a read that needs another module with no directory fails with "javac's tool needed: platform module M" |
| `jdk/platform/java.base.decls` (new, committed, deterministic), `jdk/platform/provenance.edn` (new: pin, writer, hash of the pack) | the pack: per class the stripped file, deflated, an index of `(package, class) -> offset`; written by `bin/repr-java platform-pack OUT` on the tools' host from the platform at `jdk/javac/platform.sha256`'s pin (`bin/javac-vendor platform-pin` says which directory) |
| `arbace/j2g/embed.ae` / `bin/arbace-go --build-program`'s resources | `jdk/platform/` embedded as a resource (the `--resources` path j2g has; jdk/data's resources are the model), read by `Class.getResourceAsStream` in jrt |
| `bin/repr-java` | the verb `platform-pack`, `platform-check`; `test/gate/steps.edn`, `Makefile` |

**Steps.** (1) Measure: strip java.base's 34 MB and report the size raw and deflated (the survey's 6-8 MB estimate is
unmeasured; JFS131 (b) is conditional on it, JN10.9's risk 6). (2) `platform.ae` with the directory implementation
only, `decls.ae` through it; `attr-check` shows no change. (3) `strip` and `(parse (strip b)) = (parse b)` over every
java.base class. (4) The pack writer and reader; `decls` over the pack equals `decls` over the directory. (5) The
resource embedded in the shipped program; the composite.

**Checks.** New `bin/repr-java platform-check`: (a) `(parse (strip b))` equal to `(parse b)` for every class of java.base
(and, as a one-off, of all 68 modules) and the pack's entries equal to a fresh pack of the directory; (b) `attr-check`
and `nojavac-check` over the pack for java.base and the directory for the rest equal to the directory alone (known files
unchanged); the pack's hash against `jdk/platform/provenance.edn`. Then `bin/repr-java nojavac-check` on the final tool.

### JN10.5 Part 3: JFS132 (a), the command and the REPL

**Files.**

| file | change |
|---|---|
| `arbace/repr/java.ae` | `-main` on the shipped program: `arbace -m arbace.repr.java read --no-javac OUT ROOT [PATH...]`; without `--no-javac` on the shipped program the error "repr/java: read needs javac's tool (use --no-javac, or javac's tool executable)"; a fallback file is reported (the file and the reason) and the exit status is non-zero, the files read natively are still written |
| `arbace/repr/java/load.ae` (new `arbace.repr.java.load`), `arbace.core`-level entry `load-java` | `(load-java "Foo.java")`/`(load-java dir)`: read to a temporary directory with the native read, the files' source path the file's own directory, then loaded as Java forms are (SPEC §9.2: the package's ns file, `require`); errors (parse, fallback) as REPL exceptions with the reason; the REPL's `*source-path*` for sibling sources |
| `bin/repr-java` | `shipped-check` (below) and `read` with `REPR_JAVA_HOST=shipped` (the executable `ARBACE_GO_EXE`/the gate's `$GATE_EXE`) |
| `bin/lib/repr-java/front-check` | `--shipped` beside `--no-javac`: the same loop with the shipped program as the native side |
| `test/repr/java/shipped-check-known.edn` (new) | by corpus: files that may fall back on the shipped program (expected: none that the tool's `nojavac` does not already list as fallbacks, `test/repr/java/nojavac-check-known.edn` being `{}` and the fallbacks counted by reason, not listed) |
| `test/programs/` | a sample program whose `-main` calls `(load-java ...)` on a small Java file and runs its class (`bin/arbace-go --build-program --check`) |
| `Makefile`, `CLAUDE.md`, `doc/go/BUILD.md`, `doc/classes/JAVA-FRONTEND-SURVEY.md` (the "As built: JN10") | `repr-java-shipped-check` target and `##` line; layout paragraphs (at JN10's close, not now) |

**Steps.** (1) `-main` and the error path on the closure with the hook's defaults (this runs on stage 0 evaluated from
the checkout before any shipped build: a fast loop). (2) `load.ae` and `load-java` over `arbace.classes`' loader.
(3) `shipped-check`. (4) The sample program.

**Checks.** `bin/repr-java shipped-check --corpus samples,baseline,jdk,javac` (each corpus read by javac's tool with
`--no-javac` and by the shipped program, the forms equal file for file; a file that falls back on the tool errors on
the shipped program with the same reason; the time and peak memory summed as nojavac-check does); the essential cut
(`samples` read by `$GATE_EXE` against the committed `test/repr/java/expected` forms, no tool); `bin/arbace-go
--build-program --check` (the new sample); `bin/java-check --levels read,interp` on a handful of tests loaded by
`load-java` (the 45 essential tests read by the shipped program equal their committed forms in `test/java/forms/`).

### JN10.6 What changes in shipped code, and the re-seed

Changed in the shipped program: the 22-odd read-closure namespaces of `arbace/repr/java` (new in `arbace_embedded_sources`,
so the compiled namespaces, `aot.key` and `gate_key` change), `jdk/platform/` (a resource), `arbace/j2g/embed.ae` and
`bin/lib/version.bash` (the embedded list). The runtime (`arbace/lang`, `jdk/forms`, `overlay/jdk`, `go/`) does not
change: the part needs no new runtime feature (the platform is read through `getResourceAsStream` and `Inflater`, both
in jrt), so the runtime key is unchanged. **Size:** about +27 MB by JN5's measure (+16% of 174.5 MB) for the closure
and a smaller figure without the printer; the seed grows with it (19 MB tar.xz now, about +3 MB).

**Re-seed.** Not needed to build or to check: stage 0 builds the shipped program from the checkout, and the closure is
compiled by that build (or loaded from the checkout, evaluated, on stage 0 until then, as the tools are). It is needed
for `target/seed/aot` to be a base again (RPS95 (b): the aot key moves) and so that `bin/seed --verify` closes on a seed
that carries the compiled reader. So: **one re-seed, batched with C23's** (doc/C23-SURVEY.md: C3's compiled reader
is the first, C9's the last): if JN10 merges before C3's re-seed it goes in that one, else in C9's. The re-seed is amd64
only (as JN9f's). Until it, every gate run rebuilds the executables (8 minutes) when the reader changes.

### JN10.7 Provenance files and the gate

**Provenance.** `repr_java_reader_hash` changes with the first edit of any listed file and with the new files, so
the four files that name it are rewritten once, at the end, on the final tool, in this order, by their writers:
`jdk/provenance.edn` by `bin/jrt-convert update`; `jdk/javac/provenance.edn` by `bin/javac-vendor convert` (also
`jdk/javac/edge.edn` if javac's world's names changed, which they do not); `test/java-fixtures/provenance.edn` by
`bin/clojure-tests --java-fixtures update`; `test/java/forms/provenance.edn` by `bin/java-check --forms update`.
Each rewrites its forms too, and they must not move (the claim of part 1): a forms diff is a bug, not an update.
During the work use `REPR_JAVA_STALE_OK=1` (as JN9f and JFS133 did) rather than rewrite per commit; `bin/provenance-check`
last, in the same commit as the last reader change (GTS36 (b)). A new `jdk/platform/provenance.edn` records the pack
(`bin/repr-java platform-pack` writes it; `platform-check` verifies it); `bin/provenance-check` learns it.

**Gate steps** (test/gate/steps.edn):

- new, essential, light: `repr-java-closure` (`bin/repr-java closure-check`, seconds, no executable).
- new, essential, `:hosts [:prog :stage0]`, needs `go-build`: `repr-java-shipped` (`bin/repr-java shipped-check --essential`:
  `samples` read by `$GATE_EXE` against the committed expected forms; no tool, no lock).
- new, `--full`, chain `java`, after `javac-tool`, `:heavy :inner`, memory like `repr-java-nojavac` (11 GiB):
  `repr-java-shipped-full` (`shipped-check --corpus {part}`), beside `repr-java-nojavac`.
- changed: `:tools` group (repr/java's closure is no longer excluded), `go-build` (the key moves), `javac-tool`
  (`arbace_javac_key` already hashes `arbace/`: rebuilt once), every step hosted on `:gate` or `:prog` (reruns once),
  `repr-java-*` (inputs `:repr-java` gain `jdk/platform/` and the new script), `provenance` (inputs gain
  `jdk/platform/`), `bin/gate`'s step list, GATE-SURVEY.md's table. Run `bin/gate --audit` on `smoke` (inside
  `go-build`), `repr-java-shipped`, `repr-java-closure`, `provenance` (undeclared reads show here).
- A consequence the decision did not weigh: from the merge on, an edit to the read closure changes `gate_key`, so
  every repr/java edit costs the gate-build (about 8 minutes) and the checks on `:gate`: JFS137 below.

### JN10.8 Estimate, concurrency, who does what

| part | working days | who |
|---|---|---|
| 1 (JFS130) | 3.5 (the inventory 0.3, hook and flags 0.3, tree and attr 0.8, model, nattr, resolve, expr, convert 1.5, java.ae and the tool builds 0.6) | the hook protocol and `tree`/`attr`/`model` split: Opus (the dispatch order and what the tool must still do); `nattr` and `convert` hint and static removal, `jflags`, `closure-check`, the keys: Sonnet |
| 2 (JFS131) | 2 (measure 0.2, `platform.ae` and `decls` 0.8, `strip` and the pack 0.6, embedding and composite 0.4) | `strip` (every attribute `parse` reads kept) and the equality check: Opus; the interface, the pack format, the resource, the writer verb: Sonnet |
| 3 (JFS132) | 1.5 (`-main` 0.2, `load.ae` 0.5, `shipped-check` and the sample 0.8) | Sonnet; the REPL's `load-java` semantics over `arbace.classes` loading: Opus reviews |
| integration | 1 (keys, steps.edn, the gate, the provenance rewrite, docs, the audit) | Sonnet; Opus for anything the tool's rebuilds show |
| **total** | **8 sequential, about 5.5 elapsed** | |

Part 2's `platform.ae` and the measurement run beside part 1 (they share only `decls.ae`'s four platform calls and
`java.ae`'s `registry-units`: two small hunks, merged by hand). Part 3 needs part 1's hook (for the error path) and
part 2's pack (for the composite) but its `-main`, `load.ae` and the checks' script can be written on stubs from the
start; it is the last to finish. The heavy waits are the host's: two tool builds per change of the closure's key
(about 30-40 minutes each; JN9f's 2,537 s), the corpora's checks under the big lock (jdk corpus 540 s, 10.8 GB), the
`bin/gate --full`'s chain at the end, serial: the elapsed days above assume the lock is free. One branch, not three
worktrees, for part 1; parts 2 and 3 in worktrees merged into it (the rules: one 20 GiB job at a time).

### JN10.9 Risks

1. **A javac name left in the closure** shows only when c2j compiles the shipped build (or the guard fails): the guard
   `closure-check` makes it a seconds-long failure; a name reached through reflection (`Class/forName` on a string) the
   grep misses, but the shipped build's smoke read (the essential step) meets it.
2. **A silent change of the tool's answers** from the reordering of branches (native test first): the five checks
   (parse, front, attr, nojavac, `check --suite`) on every corpus are byte-for-byte; any diff stops the part.
3. **Speed.** A hook call on a hot accessor (`tr/f`) costs: measure `bin/repr-java parse --time` and nojavac's
   total before and after on the tool (JN9e: 540 s for the corpora, the jdk corpus 10.8 GB); budget 5%, and keep
   the native test inline so that only the javac model pays an indirection.
4. **Reflection in the shipped build.** Dropping a type hint where the value is a map is harmless; dropping one on a
   host class (`File`, `ConcurrentHashMap`) is not: `*warn-on-reflection*` is on in the namespaces, and the
   guard counts its warnings in the closure (zero).
5. **The shipped program's growth and the gate-build's time and memory** (+27 MB; the j2g run 2.5-3 minutes plus about
   half a minute; `go-build`'s `:mem-gib 19` may need raising: re-measure, `.tmp/gate/go-build.mem`).
6. **The pack's size is an estimate.** If java.base's declarations come to more than 12 MB deflated, JFS131 (b) is
   reconsidered (the proposal recommended it on a measure that step 1 of part 2 makes): the fallback is the directory
   (a), at no loss for the tool.
7. **Answers of a stripped class differing** (an attribute `parse` reads that `strip` dropped, for instance
   `RuntimeInvisibleAnnotations`): `platform-check`'s equality over all 27,598 classes is the proof.
8. **Two worlds, one name**: the tool and the shipped program both hold `arbace.repr.java.*`; the tool's base now
   includes the closure, and `reader_compile` must compile only the javac half, or a namespace is compiled twice.
   `javac-tool` building on the gate's base is the test.
9. **`load-java`'s scope.** Sibling sources and the source path (javac's `-sourcepath` is the file's directory), and
   a class that needs a module not in the pack: the error, not a wrong answer.
10. **Re-seed deferral**: until then, `bin/seed --verify` and `target/seed/aot` as a base are stale for the
    reader's namespaces (known, cost only time).

### JN10.10 Proposals

- **JFS135 The platform pack's home and form.**
  - Options: (a) committed in the repository as `jdk/platform/java.base.decls`, one deterministic file (stripped class
    files deflated, with an index), written by `bin/repr-java platform-pack` from the platform at its pin, its hash in
    `jdk/platform/provenance.edn` and checked by `bin/provenance-check`; (b) not committed, packed by the build from
    the platform's directory (`REPR_JAVA_PLATFORM`) when it is set, else the shipped reader has none (a); (c) the
    original class files embedded unstripped (34 MB for java.base).
  - *Recommended: (a)*: the shipped build must not need jdk26u (CLAUDE.md: the build needs only TamaGo's Go); the repository
    already commits `jdk/forms` (12 MB) and jdk/data; (b) makes the standalone build depend on the host; (c) is five times
    the data for answers a pack gives equally (the equality check proves it).
- **JFS136 What of repr/java the shipped program embeds.**
  - Options: (a) the read closure only (the reader without the printer, round trip, coverage, split): about the 24 MB
    of forms JN9f.8 counted less `print` (3,993 lines, the biggest namespace); (b) all of repr/java but javac's half,
    the printer included (JN9f.8's +27 MB), so that Java forms print back to Java at the REPL; (c) the closure now, the
    printer as its own later part.
  - *Recommended: (a)*: JFS126 is for reading Java into the REPL; the printer's check is the tool's (roundtrip at the
    classes level needs javac anyway); (c) is (a) with the door open, and costs nothing now.
- **JFS137 The reader and the gate's key.**
  - Options: (a) accept that an edit of the read closure changes `gate_key` (the gate-build, about 8 minutes, and the
    steps hosted on `:gate` or `:prog` rerun once per edit); (b) hash the closure into the build's key only for releases
    and the shipped checks, and keep the gate's executables keyed without it (the gate's shipped read check then runs on
    an executable with the reader evaluated from the checkout, as the tools are, `bin/lib/tools.bash`); (c) leave the reader
    out of the gate's executable and compile it only into releases.
  - *Recommended: (b) for the gate, (a) for releases and the tag*: the reader is edited far more often than the runtime
    (JN9e, JN9f, JFS133: each a week of edits to repr/java), and (a) in the gate would add eight minutes and a full rerun of
    the :gate-hosted steps to each; (b) keeps the shipped check meaningful (the closure is evaluated on the gate's
    executable, the compiled one is built and smoke-tested at the release and in `--full`). Whether the `:tools` group's
    audit accepts this is part 3's first step to try; if not, (a).
- **JFS138 Next.**
  - Options: (a) JN10 as planned, three parts, one branch, after `arbace-reaps-3-crops-v2`, about 5.5 days elapsed; (b) part 2
    first as its own small step (the platform behind an interface, the pack measured, no embedding), since it stands
    alone and settles JFS131's size question before the split is begun; (c) later, after C23.
  - *Recommended: (b), then (a)'s parts 1 and 3*: the pack's size is the one number the decision rests on and it takes
    half a day to get; nothing in parts 1 and 3 depends on it.
