# Java's conformance: a check for the Java forms as goi's is for Go — survey and plan

Status: survey and plan, 2026-10-24, at the user's request (doc/JOURNAL.md, 2026-10-24: "a survey
of Java's conformance (no counterpart of `$GOROOT/test` for interpreting and compiling Java: a
corpus, today's coverage by `arbace.classes.interp` and j2g, a plan)"). Nothing here is adopted:
no code under `arbace/`, `go/` or `bin/` changed, and the decisions are the user's, as proposals
JCS1-JCS15 (§9). The milestones are JC1-JC8 (§8).

Names. **JC1-JC10 are taken**: they are the amendments of `java.util.concurrent` in the Go build
(doc/go/J2G-SPEC.md §16, doc/go/JRT-NOTES.md, "Concurrency", accepted 2026-10-10). The request
named this survey's milestones JC1..JCn, so this document uses them, and in it JCn always means
its own milestones; JCS1 asks whether to rename them (JK, JT and JX are unused in the
repository). JCS is unused.

Base: branch `java-conformance-survey`, from main `317927c0`. The executable that interprets and
the tools' host is the gate's build of exactly these sources (`.tmp/arbace-go-gate/amd64/arbace`
of main's checkout, its `gate_key` `72edcf4a2de6…` equal to the worktree's; copied to the
worktree's `.tmp/jc/bin/arbace`). javac's tool executable is main's `target/arbace-go/amd64/arbace-javac`
of 04:35 the same day (key `bf740cf0…`; the worktree's key is `776dabce…`, so it ran with
`REPR_JAVA_STALE_OK=1`: the tool's own sources differ by the commits of the day, and repr/java's
namespaces load from the checkout where they differ, `bin/repr-java`); the platform javac on
Arbace attributes against was main's `.tmp/repr-java-platform`, linked (its pin equal to
`jdk/javac/platform.sha256`). j2g's input reused main's
compiled namespaces (`target/arbace-go/aot`, its `aot_key` `ec75d466…` equal to the worktree's)
and goi's table (`target/arbace-go/gotable`). The JDK of reference is the system's, OpenJDK
26.0.2.1 built from jdk26u (`26.0.2.1-internal-adhoc.root.jdk26u`), as `bin/javac-vendor classes`
uses it. jdk26u is `/root/jdk26u` at `baf63fbe42b8758448fee570a9d9bb914272d259`. Go was only
TamaGo's go1.27.1 (`/root/tamago-go`), without cgo. The machine is the shared 64-core amd64 host;
the runs used at most 8 jobs, and the Go build went through the host's heavy-command slots. The
scratch is the worktree's `.tmp/jc/` (not tracked); Appendix A lists its scripts and what each
does.

Contents: Executive summary · 1 Today · 2 What goi's check is, and what Java's must be · 3 The
corpora · 4 What "pass" means · 5 Measurements · 6 What the failures say · 7 The tool:
`bin/java-check` · 8 Plan (JC1-JC8) · 9 Proposals (JCS1-JCS15) · 10 Risks · 11 Sources ·
Appendix A: the scripts.

## Executive summary

- **Today (§1).** The Java forms are checked by hand-picked cases, not by a corpus of programs:
  diffcheck's Java pair (`bin/diffcheck`: the oracle's `defclass`, `defclass_corpus` and `native`
  forms and j2g's fixtures, 721 of 723 cases agree between `arbace.classes.interp` and j2g, the
  two disagreements recorded), j2g's ten fixtures (`test/j2g/fixtures`, recorded on the frozen
  JVM), the twelve Java fixtures of Clojure's suite (the test build), the oracle's 213 `defclass`
  cases against the frozen JVM, and on the front end's side javac on Arbace compiling the
  baseline, the samples and java.base's closure byte for byte. Nothing runs whole Java programs
  that Arbace did not write, against the JDK's own run of them. goi has exactly that:
  `$GOROOT/test`, 1,837 programs against gc (GOI-SPEC.md §11).
- **The corpus is jdk26u's own test tree (§3).** `test/` holds 26,271 jtreg test descriptions
  (`@test` blocks in `.java` files; measured, §3.1). jtreg passes a `@run main` test when its
  `main` returns without an exception (or the JVM exits 0). Classified by what each needs: 5,182
  plain runs of `main` (no `@library`, `@modules`, `@requires` or VM options), 393 more with
  `@compile` steps, 738 whose only VM options choose HotSpot's JIT, heap or logging (649 of them
  in `hotspot/jtreg/compiler`: `-Xcomp`, `-XX:...`; dropped, the program's meaning is the same),
  1,211 langtools tests that compare javac's diagnostics with a golden file
  (`@compile/fail/ref=`), 1,105 compile-only tests; and, needing more than Arbace has: 5,762 with
  `@library` (mostly `/test/lib`), 2,797 TestNG or JUnit tests, 3,770 drivers (they start other
  JVMs), 1,616 with `@modules` (internal packages), 1,302 with native libraries. The JCK (the
  Java Compatibility Kit) is not public; Eclipse's JDT core tests (EPL-2.0) are the one other
  large public corpus, of compiler conformance, and need an extraction step (§3.3).
- **Measured (§5), on a sample of 2,428 tests** — every plain or `@compile` run test of
  `jdk/java/lang`, `jdk/java/util` and `langtools/tools/javac`, and every plain or JIT-option run
  test of `hotspot/jtreg/compiler` — each put in a package of its own (`package tNNNNN;` prepended
  to its first line, no line moved), compiled and run by the JDK (10 s each), read by repr/java
  on javac's tool executable, run by the interpreter and translated by j2g into one program:

  | step | java/lang | java/util | hotspot compiler | langtools | all |
  |---|---:|---:|---:|---:|---:|
  | selected | 393 | 422 | 990 | 623 | 2,428 |
  | pass on the JDK (the reference) | 347 | 370 | 862 | 542 | **2,121** |
  | read by repr/java (javac on Arbace) | 347 | 370 | 862 | 542 | **2,121** (2,304 files) |
  | compiled by javac on Arbace, class files equal to the JDK javac's | 347 | 370 | 862 | 542 | **2,121** (5,500 class files) |
  | interpreted, each file loaded as the REPL loads it | 119 | 145 | 659 | 320 | **1,243** (58.6%) |
  | interpreted, a test's files as one top-level `do` | 136 | 146 | 663 | 334 | **1,279** (60.3%) |
  | compiled by j2g into one program of 1,610 tests, run | 151 | 204 | 705 | 367 | **1,427** (67.3%; 88.6% of those in the program) |

  So the front end conforms already: every test the JDK passes, javac on Arbace compiles to the
  JDK javac's bytes and repr/java converts. The back ends do not: the interpreter passes 60% of
  them, j2g's program 67%: 1,195 pass both ways, 232 only compiled (the interpreter's limits:
  extendable superclasses, its bugs, speed), 20 only interpreted (j2g's own findings). And of
  langtools' 1,211 golden-diagnostics tests, javac on Arbace gives the golden output for 1,116,
  the JDK's javac for 1,157; the 41 where only the JDK matches are `--release N` (no `ct.sym` on
  Arbace), doclint, an `-encoding`, a jar on the module path and two messages (§5.5).
- **What the failures say (§6).** Of the interpreter's 842 non-passing tests, 505 are the world:
  JDK classes or members jrt does not have (reflection's annotations and generic types 70, zip and
  jar 52, serialization 46 (out for good, J2G-SPEC §12), `java.lang.invoke` 35, javac's API 34,
  `java.lang.management` 31, logging 25, `java.lang.classfile` 15, other classes 42, other members
  55) or superclasses of the world a Java form cannot extend yet (67: `Thread`, `ArrayList`,
  `AtomicReference`...). 73 are **bugs of the interpreter** found here: a private method called
  on another instance of its class throws `AbstractMethodError` (58; three lines reproduce it), an
  `Object` method called through an interface type (`Map.toString()`, `Collection.getClass()`)
  likewise (13), `clone` of an interpreted `Cloneable` class (2). 79 are **gaps of the Java forms'
  analysis** (`arbace.classes.analyze` refusing forms javac accepted and repr/java wrote: boxing
  and generic conversions, `switch` on `long` and constant labels, qualified `super` fields,
  intersection types, pattern bindings' scope). 23 are the REPL's model (no source lookup on Go:
  a class of a later file). 83 time out (60 s; 79 of them HotSpot's JIT tests, which loop to warm
  the JIT up), 41 are the harness's (several packages, outputs that differ), and 2 are a
  **repr/java bug**: an array of more than nine dimensions is written as `String/50`, which the
  reader refuses. **j2g** refuses 104 of the tests it is given (analysis 77, the same gaps; Go that
  does not compile 23: a constant negative index or division by zero that Java throws at run
  time, unused temporaries, a local class's captured values, a static member through `null`;
  the edge, names, a case-insensitive file name collision 4), and one input class with an
  unknown annotation type stops its whole run; of the 1,610 it compiles, 145 fail (the world's
  members 81, reflection over non-public members 13, jrt's `Calendar` and friends 11 as
  interpreted, an unnamed-package main 13).
- **The tool (§7, JCS2-JCS10).** `bin/java-check` as `bin/goi check` is: corpora of jdk26u's
  tests by category, read where they are, each program put in a package of its own; the JDK's
  references recorded as data (status and output digests, `test/java/ref-jdk26.edn`), so that
  the check runs without a JVM; three levels per test (javac on Arbace's class files, the
  interpreter, j2g's program in shards); results against a reference (`test/java/check-*.edn`),
  failing on a regression; the non-passing tests in recorded classes (`test/java/divergences.edn`,
  as `test/goi/divergences.edn`); an essential subset in the essential gate. The frozen JVM is
  not involved: the reference is the JDK's run of plain Java, not Arbace's.
- **Plan (§8): JC1-JC8, about 18 to 29 working days** to a milestone in goi's manner (every test
  of the corpora passing or in a recorded class, none failing unexplained): JC1 the check and its
  interpreted level (3-4 days), JC2 the compiled level and j2g's findings (3-4), JC3 the front
  end's levels (1), JC4 the interpreter's bugs and the REPL's source lookup (2-4), JC5 the analysis' gaps (3-5), JC6
  the corpus widened to all of jdk26u's plain run tests with the world's classes decided (2-4),
  JC7 `/test/lib` and TestNG/JUnit as Java forms (3-5), JC8 the check in the box and the
  divergences recorded as v1 (1-2).
- **Recommended first (JCS2-JCS6):** jdk26u's run tests as the corpus, pass as status and stdout
  against the JDK's run, the references recorded as data, JC1 then JC4 (the interpreter's bugs:
  58 + 13 tests for what look like two small fixes).

## 1. Today

### 1.1 The two back ends of the Java forms

The Java forms (doc/classes/SPEC.md) have one front end, `arbace.classes` (parse, lower, analyze,
env, types: declarations entered, members resolved, code analyzed into typed nodes), and two back
ends since the JVM's back end left main (standalone S4):

- **the interpreter**, `arbace.classes.interp` with `Compiler$CF` and `Compiler$CFGo`
  (doc/go/CLASSFORMS-REPL.md): a `defclass` at the REPL is analyzed, its methods built into
  trees of interpreter nodes, its class made at run time (`Dyn`, J2G-SPEC §5.12). Interpreted
  classes may extend `Object`, another interpreted class, or one of the world's classes on
  `arbace.j2g.dyn/class-supers` (`Throwable`, `Exception`, `RuntimeException`, `Error`,
  `IllegalArgumentException`, `IllegalStateException`, `Enum`, `Record`, `AFn` and the proxies'
  `Writer`, `Reader`, `InputStream`, `OutputStream`, ...); they call the world by reflection over
  jrt's member tables (public members only);
- **j2g** (doc/go/J2G-SPEC.md): Java forms translated to Go forms, a closed world of Arbace's
  runtime, the JDK closure (`jdk/forms`, 838 files of jdk26u converted, with jrt's own Java,
  `overlay/jdk/`), jrt's hand-written classes (`go/arbace/jrt`, its manifest) and the edge
  (`jdk/edge.edn`, what j2g knows of the classes outside the forms). A class outside the world is
  an error naming it; a cut member throws `UnsupportedOperationException`. j2g translates a set of
  Java forms either into a slice (`--slice`, `--root`; j2g's fixtures) or into the whole program
  (`--program`, with `--input DIR`: the test build, javac's tool, diffcheck's program).

What both run is the JDK's library as jrt has it: a partial Java runtime (J2G-SPEC §12: no class
loading from jars, no serialization, no XML, no Swing/AWT, for good; no `java.lang.invoke` beyond
what j2g compiles directly, no annotations at run time, generic reflection as plain classes,
three plus three charsets, the root and English locales).

### 1.2 The checks of the Java forms

| check | what runs | against | size |
|---|---|---|---|
| `bin/diffcheck`, the Java pair (essential gate) | the oracle's `defclass.ae` (213), `defclass_corpus.ae` (131), `native.ae` (283) and j2g's fixtures, interpreted and translated by j2g into one program | each other | 721 of 723 cases agree; the two (`FxCode` extends `StringReader`, `FxVarHandles` reaches `MethodHandles`: the interpreter's limits) in `test/diffcheck/known.edn` |
| the oracle (essential gate) | `test/oracle/forms/defclass.ae`, `defclass_corpus.ae` on the Go build | the frozen JVM's record | 212 of 213 and 131 of 131 since CF (CLASSFORMS-REPL.md §6) |
| `bin/j2g-check` | the oracle's class scripts and `test/j2g/fixtures` (10 files, 1,327 lines) translated into a slice, for amd64 and arm64 | the JVM's recorded results (`test/j2g/fixtures.edn`) | steps |
| Clojure's suite on the test build | the suite's 12 Java fixtures (`test/java-fixtures/forms`), translated | the suite's assertions | the suite's reference |
| javac on Arbace (`bin/javac-vendor classes`, `--full`) | the baseline, the samples, java.base's closure compiled by javac on Arbace | the JDK javac's class files, byte for byte | 3 corpora |
| `bin/repr-java parse-check`, `front-check` (`--full`) | the native parser and the converter over 37,797 test files of jdk26u among others | javac on Arbace's trees and forms | trees and forms only: nothing runs |

All of these are cases Arbace's authors wrote or chose, about Arbace's own runtime. None runs a
Java program written by someone else and compares what it does with what the JDK does. The
nearest is JN8's corpus `tests` (doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN8"), which
parses jdk26u's 37,797 test files but runs none.

## 2. What goi's check is, and what Java's must be

goi's check (doc/go/GOI-SPEC.md §11; "v1's divergences"):

1. **A corpus that is the language's own**: `$GOROOT/test`, the programs Go's own developers wrote
   to check gc, with their actions (`run`, `rundir`, `errorcheck`...) read from each file's
   header; 1,178 run programs and directory tests and 659 errorcheck programs (`linux-errtest`).
2. **A reference from the language's own implementation**: gc's build and run of the same forms
   (printed by repr/go), stdout, stderr and status, cached by the forms' and the toolchain's
   hashes (`.tmp/goi-check/ref/`), checked against the golden `.out` where there is one.
3. **One comparison**: the status, stdout byte for byte, stderr up to the first `goroutine ` line.
4. **Results as data**: `pass`, `fail`, `error`, `unsupported` (with the reason), `known`
   (`test/goi/check-known.edn`, by design); a reference of every program's result
   (`test/goi/check-linux-amd64.edn`) that fails the run on a regression, `--record` writing a new
   one.
5. **Classes**: every program not passing falls in a class of `test/goi/divergences.edn` (an id,
   by design or deferred, why, where decided, regexps over `RESULT REASON`); `bin/goi divergences`
   fails on a program no class takes, and on one of the essential subset not passing.
6. **An essential subset** (`test/goi/essential.txt`, 740 programs) in the essential gate, the
   whole in `--full`; the same in the box (amd64, arm64).

A Java check needs each of these, and differs in four ways:

- **Two back ends, and a front end that already conforms.** Go has one interpreter, goi; Java's
  forms have the interpreter and j2g. And the front end has its own levels: javac on Arbace's class
  files (equal to the JDK javac's or not) and its diagnostics, repr/java's conversion, and the
  Java forms' analysis (`arbace.classes.analyze`), which must accept what repr/java writes.
- **The reference is the JDK, not Arbace.** gc is in TamaGo's toolchain, always present. The JDK is
  jdk26u's build on the host: present today, but the direction is a standalone Arbace (CLAUDE.md),
  and the check should not need it at every run (§7.3, JCS5).
- **The corpus is not self-contained.** `$GOROOT/test`'s run programs are single files or
  directories with nothing outside Go's std. jtreg's tests build helper files from their
  directory, share libraries (`/test/lib`), need test frameworks, other JVMs and VM options
  (§3.1).
- **The world is partial by design.** goi's table lists all of std; jrt has the closure Arbace
  needs, and J2G-SPEC §12 puts parts of the JDK out of the Go build for good. So a share of any
  Java corpus will never pass, and the classes must say which share and why.

## 3. The corpora

### 3.1 jdk26u's jtreg tests, measured

jtreg (https://github.com/openjdk/jtreg, GPL-2.0) is OpenJDK's test harness; it is not on the host.
A test is a file with a comment block holding `@test`, and tags: `@run ACTION`, `@build`,
`@compile`, `@clean`, `@library`, `@modules`, `@requires`, `@key`, `@ignore`... Without an action
the default is `@run main ClassName`. A `main` action passes when `main` returns normally (in
`othervm` mode, when the JVM exits 0); `main/fail` inverts it; `compile/fail/ref=F` passes when javac
fails and its output (with `-XDrawDiagnostics`) equals `F`, line by line. `scan.py` and
`classify.py` (Appendix A) read every `@test` block of every `.java` file of jdk26u's `test/`
(jtreg also takes `.sh` and `.html` tests: not counted) and put each in the first category that
fits:

| category | what it needs | count |
|---|---|---:|
| run-plain | `@run main` (or `main/othervm`, `main/timeout=N`) only, the files of its directory (`@build` of them) | **5,182** |
| run-compile | the same with `@compile` steps (their options) | 393 |
| run-jitopts | the same, its only VM options HotSpot's (`-XX:`, `-Xcomp`, `-Xint`, `-Xbatch`, `-Xmixed`, `-Xlog`, `-Xmx`, `-Xms`), dropped | 738 |
| run-vmopts | other VM options (`-Djava.security.manager`, `--add-opens`, `-cp`...) | 1,082 |
| run-modules | `@modules` (exports of internal packages) | 1,616 |
| run-requires | `@requires` (an OS, a VM feature, a property) | 239 |
| run-lib | `@library` (mostly `/test/lib`: `jdk.test.lib.Asserts`, `Utils`, `ProcessTools`, WhiteBox) | 5,762 |
| compile-fail-ref | javac must fail with a golden diagnostics file (langtools) | 1,211 |
| compile-fail | javac must fail, no golden file | 36 |
| compile-only | javac must accept | 1,105 |
| framework | TestNG or JUnit actions | 2,797 |
| driver | `@run driver` (a `main` that starts other JVMs) | 3,770 |
| native | `/native` actions (JNI libraries) | 1,302 |
| shell | `@run shell` | 45 |
| manual or ignored | `/manual`, `@ignore` | 980 |
| other | | 13 |
| **all** | | **26,271** |

By area (the larger ones; the first five columns are what a first check can run):

| area | run-plain | run-compile | run-jitopts | compile-fail-ref | compile-only | run-modules | run-lib | framework | driver | all |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| `langtools/tools/javac` | 356 | 267 | 0 | 1,207 | 1,092 | 405 | 508 | 54 | 0 | 3,940 |
| `hotspot/jtreg/compiler` | 83 | 0 | 649 | 0 | 0 | 5 | 279 | 68 | 807 | 2,189 |
| `hotspot/jtreg/runtime` | 46 | 36 | 18 | 0 | 0 | 7 | 184 | 23 | 727 | 1,208 |
| `jdk/java/lang` | 359 | 34 | 23 | 0 | 2 | 26 | 142 | 399 | 40 | 1,209 |
| `jdk/java/util` | 422 | 0 | 6 | 0 | 2 | 52 | 88 | 303 | 2 | 921 |
| `jdk/java/io` | 272 | 25 | 0 | 0 | 0 | 4 | 29 | 82 | 1 | 438 |
| `jdk/java/nio` | 151 | 0 | 3 | 0 | 0 | 11 | 98 | 98 | 0 | 401 |
| `jdk/java/net` | 153 | 2 | 1 | 0 | 4 | 15 | 177 | 426 | 0 | 810 |
| `jdk/java/text` | 51 | 0 | 0 | 0 | 0 | 18 | 7 | 114 | 0 | 192 |
| `jdk/java/security` | 117 | 3 | 0 | 0 | 0 | 11 | 72 | 4 | 0 | 215 |
| `jdk/java/awt`, `jdk/javax` (Swing, sound, image I/O; also management, net, crypto) | 2,344 | 12 | 8 | 0 | 0 | 459 | 900 | 19 | 2 | 4,998 |
| `hotspot/jtreg/vmTestbase` | 23 | 0 | 0 | 0 | 0 | 0 | 750 | 0 | 1,490 | 3,063 |
| `jaxp` | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 505 | 11 | 517 |

Of the plain runs, about 1,900 are AWT, Swing, sound and image I/O (`java/awt` 857, `javax/swing`
731, `javax/imageio` 185, `javax/sound` 152), out of the Go build for good (J2G-SPEC §12); the rest
(about 3,250 plain runs, with the `@compile` and JIT-option ones about 4,400) is the first
corpus. langtools' run tests are javac's regression tests of the language: a
program javac once miscompiled, run to show it now behaves (`Closure1`..`Closure6`, `boxing/`,
`generics/bridges/`, `lambda/`, `patterns/`, `StringsInSwitch/`, `TryWithResources/`...): the
JLS-oriented corpus the request asks for. HotSpot's compiler tests are the JIT's regression tests:
programs whose arithmetic, loops, casts, array copies and allocation a JIT once got wrong, run with
the JIT forced; without the flags they are plain programs with a known right answer (JCS10).

The test files are GPL-2.0 without the Classpath exception (none of `jdk/java/lang`'s 1,741 files
names it), as jdk26u's `test/` is. The check
reads them where they are, from the pinned jdk26u (as JN8's corpus `tests` and goi's
`$GOROOT/test` from the toolchain's tree); what Arbace commits is its own (lists of paths, results,
classes), so LICENSE.md needs no entry until a test's text is committed (a converted form, say).

### 3.2 What a jtreg test needs, and what the measurement did instead

jtreg compiles a test with `-sourcepath` its directory, so a test's helper classes come from the
directory (`LogTests` uses `Tests.java`, `DoubleConsts.java`, `FloatConsts.java` beside it); runs
each action in a fresh or a shared JVM; sets `test.src`, `test.classes` and friends; substitutes
`${test.main.class}`; and kills a test past its timeout (120 s by default, times a factor). The
measurement's runner (`prep.py`) does the part a first check needs and records what it did not:

- **the source set**: what javac compiles from `-sourcepath DIR` for the test's file and its
  `@build` and `@compile` files (`javac -verbose`'s parsed files under the directory);
- **one package per test**: jtreg's tests are mostly in the unnamed package and reuse names
  (`Basic`, `Test`, `T`), which one j2g program cannot hold; each file gets `package tNNNNN; `
  prepended to its first line (no line moves, so line numbers in traces stay). A test whose files
  declare packages (663 of the 2,121, 627 of them HotSpot's `package compiler.c2;`) keeps them;
  97 of those collide with another test's class names and are left out of the j2g program
  (`merge.py`). The JDK's reference runs **the same renamed sources**, so a test that prints its
  class names prints the same names in both runs;
- **the runs**: each `main` action as `java -cp CLASSES tNNNNN.Main ARGS`, in an empty run
  directory, with `-Dtest.src` (the test's directory) and `-Dtest.classes`, 10 s each; HotSpot's
  JIT, heap and logging options dropped; any other VM option: not run (13 of HotSpot's).

The renaming and the minimal runner cost 307 of the 2,428 selected tests on the JDK itself (§5.1):
a test that expects its own unqualified name, `${test.main.class}`, a timeout over 10 s, a file
looked up in `test.classes` by name. A real tool reads jtreg's tags fully (§7.1) and gets most of
them back.

### 3.3 Other public suites

| suite | what | license, availability | for Arbace |
|---|---|---|---|
| the JCK (Java Compatibility Kit, the TCK of Java SE) | the conformance suite of the JLS, the JVM specification and the APIs, tens of thousands of tests | not public: licensed to OpenJDK contributors under the OCTLA, its results confidential | out |
| Eclipse JDT core's compiler tests (`eclipse-jdt/eclipse.jdt.core`, `org.eclipse.jdt.core.tests.compiler`, `regression/*Test.java`) | ECJ's regression tests: Java programs held as strings in JUnit tests, `runConformTest(files, expectedOutput)` and `runNegativeTest(files, expectedProblems)`, many thousands of them, through every Java release | EPL-2.0, public on GitHub; not on the host, not measured here | a large second language corpus after jtreg's, once a tool extracts the programs and their expected outputs from the JUnit sources (the expected outputs are ECJ's, so the reference stays the JDK's run); its negative tests are ECJ's messages, not javac's |
| Mauve (GNU Classpath's test suite) | library tests, Java 1.4-5 era | GPL-2.0-or-later, sourceware | stale; jtreg's library tests cover the same and more |
| Apache Harmony's class library tests | JUnit tests of the class library, Java 5-6 | Apache-2.0, retired (Apache Attic) | stale, JUnit |
| the Computer Language Benchmarks Game | about ten Java programs with expected outputs | BSD-3-Clause | a small speed corpus, not conformance |
| Rosetta Code | many small programs | GFDL-1.2 | the license does not fit |
| a hand-curated corpus | Arbace's own: per chapter of the JLS (conversions, overloads, initialization order, inner classes, lambdas, switch and patterns, exceptions and `finally`, string concatenation, floating point), each a `main` printing what it checks, its expected output the JDK's | Arbace's own | small and aimed: the gaps jtreg's tests leave (§6.4), and the essential subset's backbone |

## 4. What "pass" means

Per test and per level, as goi's §11.1 does for Go:

- **the reference**: the JDK's run of the test's (renamed) sources, each `main` action: its exit
  status, stdout, stderr. A test that **fails on the JDK** (status not 0 under the runner) is not in
  the corpus (it is the runner's or the test's, not Arbace's). A test whose stdout or status
  **differs between two JDK runs** (`ref2.py`: 87 of the 2,121, hash codes, times, thread
  interleavings) is compared on its status alone;
- **the comparison**: the status (0, 1 for an uncaught exception, a `System.exit` code); stdout
  byte for byte; stderr not compared, but its first line (`Exception in thread "main" C: message`)
  kept as the reason, which is what classes match. jtreg itself looks only at the status; stdout
  is the stricter judge, and the one goi uses;
- **the results**: `pass`; `differ` (status equal, stdout not); `fail` (status differs);
  `load-failed` (the interpreter could not define the classes: an analysis error, a class outside
  the world; for j2g, `refused`: j2g's error for the test's classes); `timeout`; `known` and
  `unsupported` as goi's, from the classes;
- **the levels**: `classes` (javac on Arbace's class files equal to the JDK javac's, byte for
  byte, `bin/javac-vendor classes`' options), `read` (repr/java converts every file), `interp`,
  `j2g`; and for the golden-diagnostics tests, `diagnostics` (javac on Arbace's output equal to
  the golden file, as jtreg compares it).

## 5. Measurements

### 5.1 The sample and the reference

Selected (`prep.py`, `classify.py`'s categories): every run-plain and run-compile test of
`jdk/java/lang`, `jdk/java/util` and `langtools/tools/javac`, and every run-plain and run-jitopts
test of `hotspot/jtreg/compiler`.

| corpus | selected | javac failed (the source set or the renaming) | failed on the JDK | not run (VM options) | **pass on the JDK** | of them nondeterministic | in packages | source files | `main` actions |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| `jdk/java/lang` | 393 | 5 | 41 | 0 | **347** | 7 | 13 | 452 | 353 |
| `jdk/java/util` | 422 | 19 | 33 | 0 | **370** | 39 | 2 | 382 | 386 |
| `hotspot/jtreg/compiler` | 990 | 85 | 30 | 13 | **862** | 39 | 627 | 863 | 1,158 |
| `langtools/tools/javac` | 623 | 17 | 64 | 0 | **542** | 2 | 21 | 607 | 549 |
| **all** | **2,428** | 126 | 168 | 13 | **2,121** | 87 | 663 | 2,304 | 2,446 |

The reference took 1.5 minutes per 400 tests at 8 jobs (javac is most of it).

### 5.2 The front end: read and compiled

- **repr/java** (`read.py`: `bin/repr-java read` per test on javac's tool executable, 6 jobs):
  **2,121 of 2,121 tests, 2,304 of 2,304 files converted**, no javac error, no conversion failure;
  358.7 s of the tool's own time summed over the tests (0.1 to 2 s each), the 1,259 tests of the
  first three corpora in 79 s of wall time at 6 jobs.
- **javac on Arbace** (`classes_cmp.py`: `bin/repr-java javac` and the JDK's `javac`, `-g:none
  -nowarn -XDsuppressNotes -proc:none -implicit:none`, 4 jobs): **2,121 of 2,121 tests' class
  files byte for byte equal, 5,500 class files**; 7.7 minutes.

So for these tests the front end is javac's to the byte, and the converter's output covers them.
One converter issue shows only later: two tests (`hotspot/jtreg/compiler/types/
TestArrayManyDimensions.java`, `langtools/tools/javac/4917091/Test255.java`) declare arrays of 50
and 255 dimensions; repr/java writes their types as `String/50`, `Object/255`, and the reader
takes array-class symbols of 1 to 9 dimensions only (`Invalid token: Object/255`), so their forms
cannot be read back (§6.3; JCS12).

### 5.3 The interpreter

`interp.py`, on the gate's executable: per `main` action a fresh process loads the test's forms
and calls `main` (the driver: `(require 'tNNNNN)`, then `(tNNNNN.Main/main (into-array String
[ARGS]))`, an uncaught exception printed as the JVM prints it and exit 1), `test.src` and
`test.classes` as system properties (`JAVA_TOOL_OPTIONS`), 60 s each (the definition included),
8 jobs. Two ways of loading:

- **as the REPL loads a package** (`interp`): the namespace file loads the class files one by one,
  in order (SPEC §9.1), each `defclass` analyzed and defined when its file is loaded;
- **as one top-level `do`** (`interp-do`): the test's files read, their imports evaluated, then
  every `defclass` of the test in one `do`, entered together before any body is analyzed (SPEC
  §9.2, 2), as javac compiles a test's files together.

| result | `interp` | `interp-do` |
|---|---:|---:|
| pass | 1,243 | **1,279** |
| load-failed (the definition failed) | 649 | 602 |
| fail (status differs) | 128 | 138 |
| differ (stdout differs, deterministic on the JDK) | 19 | 19 |
| timeout (60 s) | 82 | 83 |

By corpus (`interp-do`): `java/lang` 136 of 347 (39%), `java/util` 146 of 370 (39%), HotSpot's
compiler tests 663 of 862 (77%), langtools 334 of 542 (62%). The one-`do` load passes 36 more,
loses 1 (`interp` passed it). A small test is defined and run in 0.3 s (the first Java form loads
the analysis, compiled since S6: no longer CLASSFORMS-REPL §6's 3.2 s); the 1,259 tests of the
three first corpora took 92 s, HotSpot's 862 about 10 minutes (its 83 timeouts).

### 5.4 j2g's closed world

`merge.py` puts every converted test's forms in one tree (2,121 tests; 97 packaged tests whose
class paths collide with another's left out, and the two whose arrays the reader refuses);
`j2gbuild.sh` runs j2g as the test build does (`--program`, the tests' forms and the compiled
namespaces as `--input`, goi's table), through a driver (`scripts/ae/jc_j2g.ae`, j2g's
`:root-fn` as `test/j2g/check.ae` uses it) that roots each test's `main` whose class the load
declared and writes j2g's refusals (`report.edn`'s `:analysis-failures` and `:errors`) one a
line; the tests owning a refused class are taken out and j2g runs again; then `bin/build-go`, and
on a failed build every Go error of the package `lang` (`go build -gcflags=-e`, `goerrs.sh`) takes
its tests out in turn (`j2gloop.sh`). Four things had to be learned on the way, each a finding:

- **Classes of one simple name in two packages collide in the Go package `lang`** (J2G-SPEC §4.2,
  §4.4: a class of no JDK module is named by its simple name): 2,365 errors "Go name ... declared
  more than once" in the first pass. `--qualify PREFIX` (javac's tool's option for its driver's
  package) names them by their binary names; with every test's package qualified, none.
- **One input class can stop j2g's whole run**: an annotation type whose meta-annotation is outside
  the world (`@Repeatable`'s container) throws in `arbace.j2g.fromfn/fis` (the functional
  interfaces' scan, over every class) instead of being that class's failure. The measurement left
  out the 308 tests whose classes the interpreter had found naming a JDK class outside the world
  (`ClassNotFoundException`, `Unknown type`: `prefilter.py`), which j2g refuses by construction
  (J2G-SPEC §4.1: a class neither in the forms nor in the edge does not exist), so that number is
  the interpreter's, not j2g's own.
- **Go file names collide case-insensitively**: `pack/SuperClass` and `mypackage/Superclass` both
  become `lang/SuperClass.go`, `lang/Superclass.go`, which `go build` refuses ("case-insensitive
  file name collision"; on macOS's file systems they would overwrite each other).
- **j2g writes Go that does not compile** for 23 tests (eight kinds, `goerrs.sh`): a constant
  negative array index (`a[-1]`, 8 tests, 7 of them HotSpot's: Java throws
  `ArrayIndexOutOfBoundsException` at run time, Go rejects the constant), a constant division by
  zero (1), temporaries declared and not used
  (pattern matching's, 5), a local class's captured values missing from its constructor call when
  it is made inside an anonymous or another local class (3), a static member reached through a
  `null`-valued expression (`((T) null).A`, 2), a captured variable undefined in a lambda (2), a
  value of type `any` returned as `int32` (1), `finalize` called on a class that does not declare
  it (1).

In the end 1,610 tests are in the program, with no refusal and no Go error: five j2g passes and
four builds (about 17 minutes of a heavy slot in all; the slots' queue on the shared host added
an hour), the program 277,482,863 bytes. The runs (`interp.py` in mode `j2g`: the same driver
without the definition, 10 s each, 8 jobs) took 109 s; a test starts in 0.13 s.

| j2g's result | java/lang | java/util | hotspot compiler | langtools | all |
|---|---:|---:|---:|---:|---:|
| **pass** | **151** | **204** | **705** | **367** | **1,427** |
| fail (status differs) | 65 | 34 | 11 | 35 | 145 |
| differ (stdout, deterministic on the JDK) | 2 | 4 | 7 | 6 | 19 |
| timeout (10 s) | 4 | 3 | 12 | 0 | 19 |
| refused: names a JDK class outside the world (by the interpreter's finding, above) | 103 | 120 | 27 | 58 | 308 |
| refused by j2g: the analysis | 10 | 5 | 8 | 54 | 77 |
| refused by j2g: Go that does not compile | 1 | 0 | 8 | 14 | 23 |
| refused by j2g: the edge, names, a file name collision | 2 | 0 | 1 | 1 | 4 |
| not in the program: the harness (colliding packaged class paths; arrays of more than 9 dimensions) | 9 | 0 | 83 | 7 | 99 |
| **all** | 347 | 370 | 862 | 542 | 2,121 |

The analysis' refusals are the interpreter's analysis gaps again (the same front end:
`Cannot convert`, `switch without default`, `No static field super`, intersection types), and the
edge's are classes whose generic signatures or enum constants `jdk/edge.edn` does not record
(`AtomicReference`, `Semaphore`, `Thread`'s: "not in the declared edge (bin/j2g-edge update)"). Of
the 145 failing: the world, 83 (a JDK member jrt lacks or a class outside the closed world reached
at run time, 64: `UnsupportedOperationException: j2g: class java.lang.annotation.Annotation is not
in the closed world`, `TypeVariable`, `AnnotatedType`, `Runtime.exec`'s overloads,
`Class.getDeclaredClasses`, `ForkJoinPool.submit(Callable)`, `Date.toInstant`, "an outer class's
super call"; zip and jar 9, `StackWalker` 4, annotations 3, locales 3); reflection over non-public
members and class metadata (`getDeclaredField` of a private static field, `getEnclosingMethod`,
`newInstance` of a nested class, `Modifier.isStatic` of a nested enum) 13; the 13 tests whose
`main` is in the unnamed package of a packaged test (j2g declared no such class: the harness's or
j2g's, open); jrt's own `Calendar`, `Date`, `Formatter`, `CRC32`, `StringBuffer` behaviour 11, the
same failures as interpreted; references and collection timing 3; javac's synthetic members by
reflection 2; one goroutine stack overflow (`LambdaTest5`); a test of several packages 1; 18
others.

**The two back ends against each other**, over the 1,610 tests of the program: 1,195 pass both
ways, 163 neither, **232 only compiled** (the interpreter's definition failed on 100: extendable
superclasses, its private-call bug; 77 failed, 54 timed out at 60 s) and **20 only interpreted**:
`Runtime.exec`'s overloads (5) and `ForkJoinPool.submit(Callable)`, `Date.toInstant` (the
interpreter reaches them by reflection; the translated call finds them cut), reflection over
non-public members (5), `CopyMethods`' class initialization, an `AssertionError` of
`TestSyntheticNullChecks`, `SamConversion`'s "unexpected primitive
coercion Ljava/lang/Object; -> I" (a translation error at run time), `LambdaTest5`'s stack
overflow, two timeouts at 10 s, two outputs. These 20 are diffcheck's kind of disagreement, found
on programs diffcheck's corpus does not hold.

### 5.5 javac's diagnostics

`errtest.py`: every compile-fail-ref test (1,211), its first `@compile/fail/ref=F` action run in
the test's directory by the JDK's javac and by javac on Arbace (`bin/repr-java javac`) with the
action's options and files, each output compared with `F` as jtreg compares it (lines, trailing
white space trimmed); 3.7 minutes at 4 jobs.

| JDK's javac | javac on Arbace | count | |
|---|---|---:|---|
| matches | matches | **1,116** | the outputs equal |
| matches | differs | 36 | `--release 8..24` (31: javac on Arbace has no `ct.sym`, "release version 15 not supported"); `-encoding ascii`, a jar on `--module-path` ("unsupported on Arbace"); an annotation processor not found (another message); a source file's bad header (another path) |
| matches | javac does not fail | 5 | doclint (`-Xdoclint`, 4: jdk.javadoc's doclint is not in javac's world), a lint option |
| differs | differs (the two equal) | 23 | the golden files expect options jtreg adds or an older message; both javacs agree |
| differs | differs (the two differ) | 28 | most are `--release` again |
| does not fail | does not fail | 3 | |

So javac on Arbace gives the JDK javac's diagnostics wherever its world has what the test asks;
what differs is the platform (`ct.sym`'s older releases), doclint and two options, all known since
JN3-JN6.

### 5.6 What the runs cost

| step | wall |
|---|---:|
| scan and classify jdk26u's test tree (26,271 descriptions) | 20 s |
| the reference of `jdk/java/lang`'s 393 tests (the source sets, javac twice, the JDK's runs; 8 jobs) | 93 s |
| the second JDK run of 2,121 tests (6 jobs) | a few minutes |
| repr/java, the first 1,259 tests (6 jobs) | 79 s |
| javac on Arbace against the JDK's javac, 2,121 tests (4 jobs) | 7.7 min |
| the interpreter, the first 1,259 tests (8 jobs), each load | 92 s |
| the interpreter, HotSpot's 862 tests (8 jobs), each load | about 10 min (83 timeouts of 60 s) |
| j2g (`--program`, 1,610 to 2,022 tests' forms with the runtime) | 100-114 s a pass |
| `bin/build-go` of that program (the print cache cold, then warm) | 73-118 s |
| the j2g program, 1,610 tests (8 jobs) | 109 s |
| the golden diagnostics, 1,211 tests (4 jobs) | 3.7 min |

## 6. What the failures say

### 6.1 The interpreter's results by class

`classes.py` puts every non-passing test in the first class whose pattern matches its result and
reason (the last cause of a definition's failure, else the first line of stderr), as
`test/goi/divergences.edn`'s regexps do. `interp-do`, 842 tests not passing:

| class | all | java/lang | java/util | hotspot compiler | langtools |
|---|---:|---:|---:|---:|---:|
| **pass** | **1,279** | 136 | 146 | 663 | 334 |
| *the world (jrt's classes, members, extendable superclasses): 505* | | | | | |
| annotations and reflection types (`getAnnotation`, `getTypeParameters`, `getGenericReturnType`, `getParameters`, `getDeclaredClasses`, `Proxy.getProxyClass`, `java.lang.reflect.AccessFlag`; `isLocalClass`, canonical names, InnerClasses flags) | 70 | 61 | 0 | 1 | 8 |
| a superclass the Java forms cannot extend yet (`Thread` 22, `ArrayList`, `AtomicReference`, `ScheduledThreadPoolExecutor`, `LinkedHashMap`, `LinkedHashSet`, `ByteArrayInputStream`...) | 67 | 14 | 28 | 13 | 12 |
| a JDK member jrt lacks or cuts (`System.runFinalization`, `Thread.getThreadGroup`, `GregorianCalendar.computeTime`, `JarFile`'s constructor, `RandomGeneratorFactory`...) | 55 | 14 | 25 | 10 | 6 |
| zip and jar (`ZipFile`, `Deflater`, `Adler32`, `CRC32C`, `GZIPInputStream`, `JarInputStream`) | 52 | 0 | 49 | 3 | 0 |
| serialization (`ObjectInputStream`, `ObjectOutputStream`; out for good, J2G-SPEC §12) | 46 | 10 | 30 | 0 | 6 |
| other JDK classes not in jrt (`Timer`, `ServiceLoader`, `Calendar$Builder`, `DataOutputStream`, `ToolProvider`...) | 42 | 10 | 25 | 5 | 2 |
| `java.lang.invoke` (`MethodHandle`, `MethodHandles`, `CallSite`, `LambdaConversionException`) | 35 | 17 | 0 | 17 | 1 |
| javac's API (`com.sun.tools.javac.Main`, `javax.tools`, `com.sun.source`: in javac's tool, not in the shipped program) | 34 | 0 | 0 | 0 | 34 |
| `java.lang.management`, JMX | 31 | 31 | 0 | 0 | 0 |
| `java.util.logging` | 25 | 0 | 25 | 0 | 0 |
| references, finalization, GC (`PhantomReference`, `System$Logger`) | 15 | 9 | 4 | 2 | 0 |
| `java.lang.classfile` | 15 | 3 | 0 | 0 | 12 |
| `StackWalker`, stack traces and line numbers | 9 | 6 | 1 | 0 | 2 |
| a protected or inherited member of a world class (`Observable.setChanged`, AQS' `getState`, `ClassLoader.findLoadedClass`; CLASSFORMS-REPL §7) | 5 | 1 | 3 | 0 | 1 |
| locale rules (Lithuanian), charsets (`US-ASCII`'s alias) | 4 | 4 | 0 | 0 | 0 |
| *the interpreter's bugs: 73* | | | | | |
| a private method called on another instance of its class (`new P().run()`, `p.run()`): `AbstractMethodError` (57 of the 58 a private method by the sources; one `getClass()` of an anonymous class) | 58 | 9 | 3 | 18 | 28 |
| an `Object` method called through an interface type (`Map.toString()`, `Collection.getClass()`): `AbstractMethodError` | 13 | 2 | 10 | 0 | 1 |
| `clone()` of an interpreted `Cloneable` class: `CloneNotSupportedException` | 2 | 0 | 0 | 2 | 0 |
| *the analysis (`arbace.classes.analyze` refuses forms javac accepted): 79* | | | | | |
| boxing and generic conversions (`Cannot convert ... of type "C" to Ljava/lang/Byte;`, a lambda's `Object` value to its target, erased overloads `Integer.toString(Object)`, two `m`s of a generic functional interface) | 20 | 2 | 1 | 4 | 13 |
| `switch` (`switch on a value of type "J"`, a switch expression without default over an enum, labels `Integer.MIN_VALUE` and qualified enum constants) | 9 | 0 | 0 | 0 | 9 |
| qualified `this`, `super` and enclosing instances (`C.super.f`, a local captured in a nested local class's constructor) | 6 | 0 | 0 | 0 | 6 |
| other: intersection types (`for` over an `Iterable & Serializable`, method references on them), pattern bindings' scope, `^:const` locals, `int[]::clone`, a class named `Enum` in the test's package, `Module`, a later member's field | 44 | 4 | 6 | 16 | 18 |
| *the REPL's model: 23* | | | | | |
| a class of a later file or form (no source lookup on Go, SPEC §9.2, 3; the packaged tests are loaded file by file) | 14 | 0 | 0 | 14 | 0 |
| a sibling of the `do` used before it is defined (a static initializer or a superclass needing a class defined later in the `do`) | 9 | 1 | 0 | 0 | 8 |
| *the rest: 162* | | | | | |
| timeout (60 s; HotSpot's tests loop up to 10⁵-10⁶ times to warm the JIT) | 83 | 1 | 3 | 79 | 0 |
| the harness: a test of several packages (only the main's namespace is loaded) | 22 | 5 | 1 | 0 | 16 |
| the harness: stdout differs, deterministic on the JDK (a stack trace printed to stdout, `Phaser` counts, a temporary path) | 19 | 0 | 4 | 7 | 8 |
| javac's synthetic members seen by reflection (`lambda$r$0`, `access$001`) | 3 | 0 | 0 | 0 | 3 |
| repr/java: an array of more than 9 dimensions | 2 | 0 | 0 | 1 | 1 |
| other, failing (each its own: Calendar's field computations, `Date`'s UTC, `CRC32`'s initializer, a `ThreadLocal`'s initial value on another thread, `BoxedForeach`'s `Float`, bridges' order, `GenericAnonCtor`...) | 30 | 7 | 6 | 5 | 12 |
| other, not defined | 3 | 0 | 0 | 2 | 1 |

### 6.2 The interpreter's bugs, reduced

Each class of §6.1's "bugs" reduces to a few lines; the first:

```java
package m;
public class P {
    private void run() { System.out.println("private run"); }
    void pkg() { System.out.println("pkg"); }
    public static void main(String[] a) {
        new P().pkg();        // prints pkg
        P p = new P(); p.run(); // AbstractMethodError: m.P.run()V
    }
}
```

repr/java writes `(.run p)` and j2g's program runs it; the interpreter, which should call a
private method directly (CLASSFORMS-REPL §2.2: "a static, private or super call of an interpreted
method calls it directly"), by the symptom dispatches it virtually when the receiver is not
`this`, and finds no private method in the vtable (not traced in the code for this survey). The
second: a call of `toString`, `getClass`, `hashCode` or `equals` whose static type is an
interface of the world (`Map m; m.toString()`), by the symptom looked up among the interface's own
methods rather than `Object`'s, which every interface has (JLS 9.2). The fixes belong in
`Compiler$CF`'s call nodes or in the builder (`arbace/lang/Compiler.ae`, `arbace/classes/interp.ae`):
a change to the runtime, so a re-seed (JC4). diffcheck's Java pair did not see them because its cases call private methods on
`this` and world interfaces' own methods.

### 6.3 The front end's findings

- **The analysis lags javac** (79 tests): the Java forms' analysis is erased by design (SPEC §1,
  principle 4) and repr/java's forms carry javac's decisions as casts, hints and pinned calls
  (JAVA-FRONTEND-SURVEY §2); where a form still leaves a decision to the analysis (a boxing
  conversion to `Byte` from a `char` constant, an intersection type's member, a `switch` on
  `long`), the analysis refuses what javac accepted. Each is either the converter making the
  decision explicit (a cast or a hint more) or the analysis learning the rule: one of the two
  sides, case by case (JC5, JCS11).
- **Arrays of more than nine dimensions** (2 tests): the converter's spelling `C/N` for N ≥ 10 is
  not readable. Options in JCS12.
- **Beyond those arrays, no bug of javac on Arbace or of repr/java's reading** was found in 2,121
  tests and 1,211 golden diagnostics; the differences are the platform's known limits (§5.5).

### 6.4 What the corpus does not cover

jtreg's run tests exercise the library more than the language: of the interpreter's 842
non-passing tests, 505 are library classes. The language's corners are langtools' run tests (542
here) and HotSpot's (862, arithmetic and loops). What neither has, and a hand-curated corpus should
(§3.3): the JLS's evaluation order corner cases printed rather than asserted, floating point's
`strictfp` and conversions printed digit for digit, class initialization order across interpreted
and world classes, string concatenation of every type, and the REPL's own model (redefinition,
SPEC §10).

## 7. The tool: `bin/java-check`

### 7.1 Shape

`bin/java-check`, Arbace's own counterpart of `bin/goi check`, on the tools' host, with
`arbace.java.check` (a namespace under `test/`, as `diffcheck.*` and `j2g.check` are, or under
`arbace/` if embedded: JCS7) holding:

- **the test descriptions**: jtreg's tags read from each `@test` block (the actions in order,
  `@build`, `@compile`, `@library`, `@modules`, `@requires`, `@key`, `@run`'s modes and options),
  the categories of §3.1, jtreg's default action; reading the comment text by the native parser
  (`arbace.repr.java.parse` keeps doc comments, not block comments' text: the tags are read from
  the file's text, as `scan.py` does);
- **corpora** (`--corpus`): named selections by area and category (`jdk-run` for `jdk/java/*`'s
  run-plain and run-compile, `langtools-run`, `hotspot-compiler`, `langtools-diagnostics`), from
  jdk26u's `test/` at the platform's pin (`jdk/javac/platform.sha256` names the build tree; the
  test tree is the same commit);
- **the source set and the renaming** as `prep.py` (§3.2), javac on Arbace computing the source
  set (no JDK needed);
- **the levels** of §4, each a verb or a flag: `classes`, `read`, `interp`, `j2g`, `diagnostics`;
- **results** as `bin/goi check`'s: `test/java/check-<corpus>.edn`, every test's result per level,
  failing the run on a regression, `--record` writing it anew;
- **classes**: `test/java/divergences.edn` (id, title, by design or deferred, why, where decided,
  regexps over `RESULT REASON`), `bin/java-check divergences` failing on a test no class takes;
- **an essential subset** `test/java/essential.txt`: tests passing at every level and fast (the
  measured passing tests run in well under a second each: a few hundred of them in a minute at 8
  jobs);
- **jobs** (`--jobs`, default 8), the timeout (`--timeout`, default 10 s, the definition apart).

### 7.2 The compiled level

j2g translates one program per shard of tests (`--program` with the tests' forms as `--input` and
each `main` a `--root`), as diffcheck does with its Java forms (§5.4 measured one shard of 2,022
tests). A test j2g refuses (a class outside the world: j2g's error names it) is its own result,
`refused`, with j2g's message as the reason; the shard is translated again without it, so one
test's refusal never hides the others (the measurement needed five, §5.4). Shards of a few
hundred tests keep a failed build cheap to bisect; the namespaces' compiled tree is the shipped
build's when the keys match (`aot_key`, as the test build's SA157).

### 7.3 The references, and the JVM

goi's references are gc's runs, cached in `.tmp/goi-check/ref/`, because gc is always there. For
Java the choice is JCS5:

- (a) the JDK on the host at each run, cached by the sources' hash and the JDK's build (as goi);
- (b) **recorded as data**: `bin/java-check --record-ref` runs the JDK (as `bin/javac-vendor
  classes` does) and writes `test/java/ref-jdk26.edn`: per test and action, the status, the
  SHA-256 and length of stdout, the first line of stderr, deterministic or not; the check compares
  digests and needs no JVM, as the oracle's expected results let the oracle run without the
  frozen jar (doc/go/ORACLE.md).

The **frozen JVM** (the v4 jar, `bin/lib/frozen.bash`) is not involved: it is Arbace on the JVM,
and the reference here is the JDK running plain Java. j2g's edge (`jdk/edge.edn`) stays what it is:
a test whose classes need the edge to grow is refused by j2g until `bin/j2g-edge update` records
them, which is a world decision (§6.1), not the check's.

### 7.4 In the gates

| where | what | cost (measured, 8 jobs) |
|---|---|---|
| essential gate | `bin/java-check --essential`: the essential subset, interpreted (`classes` and `read` too: javac on Arbace is the `--full` gate's, so the essential gate takes those levels from the committed reference) | about a minute |
| `bin/gate --full`, the java chain (after `javac-vendor build`) | every corpus at the levels `classes`, `read`, `diagnostics` on the tool just built, `interp` on the executable, `j2g` in shards | the front end 15 minutes, the interpreter 15, j2g about 4 minutes a shard (a pass and a build) and 2 minutes of runs |
| the box | `interp` of the essential subset in the box (amd64; arm64's half, as goi's `box.txt`) | minutes |

## 8. Plan (JC1-JC8)

Each milestone ends with its check green and its records (journal, agenda, this survey's "As
built"). Estimates in working days, as the repository's surveys give them (they have run well
under: JN8's 4-6 days took half a day).

| milestone | content | exit criteria | days |
|---|---|---|---:|
| **JC1** the check and its interpreted level | `bin/java-check` (§7.1): jtreg's tags, the corpora `jdk-run`, `langtools-run`, `hotspot-compiler` (the run-plain, run-compile and run-jitopts tests of `jdk/java/*` but AWT, `langtools/tools/javac`, `hotspot/jtreg/compiler`), the source sets by javac on Arbace, the renaming, the JDK's references recorded (JCS5), the levels `read` and `interp` (the one-`do` load, JCS8), results and `--record`, the classes of §6.1 as data, `divergences`; Makefile target | the reference over about 3,000 tests recorded, every non-passing test in a class, the essential subset chosen and green, `make check-makefile` | 3-4 |
| **JC2** the compiled level | j2g in shards (§7.2), `refused` with j2g's reason, re-translation without the refused, runs and comparison as JC1's; j2g's findings of §5.4 fixed: a class's failure in `arbace.j2g.fromfn/fis` its own, user classes' Go names and file names unique (JCS15), the eight kinds of Go that does not compile, the unnamed-package roots | every test of JC1's corpora with a `j2g` result, every non-passing one in a class, none refused for Go that does not compile; the interpreter's and j2g's results compared (a test passing in one and failing in the other is diffcheck's kind of finding, listed); a re-seed if j2g's output for the runtime changes | 3-4 |
| **JC3** the front end's levels | `classes` (byte for byte against the JDK javac, recorded as digests), `diagnostics` (the 1,211 golden tests), `read`; in `--full`'s java chain | §5.2's and §5.5's numbers as the reference, the known differences classed (`ct.sym`, doclint, options) | 1 |
| **JC4** the interpreter's bugs and the REPL's lookup | private calls on any receiver, `Object`'s methods through interfaces, `clone` of interpreted classes (§6.2); the source lookup of SPEC §9.2 (3) on Go, so that a file's class finds a later file's (JCS8); diffcheck's corpus gains the reduced cases | the 73 tests of the bugs' classes pass or move to another class; diffcheck's Java pair still agrees; one re-seed (runtime change), batched | 2-4 |
| **JC5** the analysis' gaps | §6.3's 79 tests: each fixed in the converter or in the analysis (JCS11), with a regression form in `test/repr/java/` or the analysis' tests | the analysis' classes empty or each remaining test recorded with its reason; repr/java's checks (`check`, `roundtrip`, `front-check`) unchanged or updated byte for byte | 3-5 |
| **JC6** the world decided, the corpus widened | every world class of §6.1 marked by design (serialization, AWT/Swing, class loading, XML: J2G-SPEC §12) or deferred, with the user's decisions (JCS9); the corpus widened to the plain run tests of all of `jdk/` but AWT/Swing (`java/io`, `java/nio`, `java/text`, `java/time`, `java/math`, `java/security`, `java/net`, `javax/crypto`...) and `hotspot/jtreg/runtime`'s | every test of the widened corpus in a class; the deferred classes ranked by tests gained per jrt change | 2-4 |
| **JC7** `/test/lib` and the frameworks | `/test/lib`'s plain parts (`jdk.test.lib.Asserts`, `Utils`, `RandomFactory`; not `ProcessTools` or WhiteBox) and TestNG or JUnit 5 as Java forms in the test world (licenses: Apache-2.0, EPL-2.0, LICENSE.md entries), their runners as `main`s | the run-lib and framework tests of JC1's areas that need only those, measured and classed (how many of the 8,559 run-lib and framework tests need no more is JC7's first measurement) | 3-5 |
| **JC8** the box and v1 | `interp` of the essential subset in the box (amd64, arm64's half), as goi's check-box; the divergences of the corpora recorded as v1 (a section in this survey's manner of GOI-SPEC's "v1's divergences") | the box's references recorded; no test failing outside a class; a tag if the user wants one | 1-2 |

**Total: 18 to 29 working days**; JC1, JC3 and JC4 (6-9 days) give the check and its first
fixes. JC2 can run beside JC4 and JC5; JC6 and JC7 are as large as the world decisions make them.

## 9. Proposals (JCS1-JCS15)

- **JCS1 Names.**
  - Options: (a) JC1-JC8 for the milestones, as the request named them, noting JC1-JC10's other
    meaning (the concurrency amendments of 2026-10-10) as JFS11 did for JF1-JF5; (b) rename the
    milestones (JK1-JK8: unused) before any is built.
  - *Recommended: (a)*: the amendments are closed records, the milestones new; the survey says
    which is which.
- **JCS2 The corpus.**
  - Options: (a) jdk26u's jtreg tests, read in place at the platform's pin, by category (§3.1),
    first the run tests of `jdk/java/*` (but AWT), `langtools/tools/javac` and
    `hotspot/jtreg/compiler`; (b) (a) and a hand-curated JLS corpus of Arbace's own (§3.3), small,
    for what jtreg's tests do not print; (c) (b) and ECJ's tests extracted from JDT core (EPL-2.0)
    later; (d) a hand-curated corpus only.
  - *Recommended: (b)*, with (c) once JC1-JC5 are done and a second opinion on the language is
    wanted: jtreg's tests are the JDK's own, as `$GOROOT/test` is Go's, and need no vendoring.
- **JCS3 What passes.**
  - Options: (a) the status and stdout byte for byte against the JDK's run, status only for a test
    nondeterministic on the JDK (two runs), stderr's first line as the reason (§4); (b) jtreg's
    rule, the status alone; (c) (a) and stderr up to the first stack frame, as goi's `goroutine `
    rule.
  - *Recommended: (a)*: stdout found 19 differences the status alone would have passed (§6.1); stderr's
    traces differ by design (frames, CLASSFORMS-REPL §7), their first line is kept as the reason.
- **JCS4 One package per test.**
  - Options: (a) `package tNNNNN; ` prepended to the first line of every file of an unnamed-package
    test, the JDK's reference run on the renamed sources (§3.2; measured); (b) j2g one program per
    test, no renaming (a Go build per test: minutes each); (c) the renaming for the compiled level
    only.
  - *Recommended: (a)*: one form of each test for every level, and the numbers of §5 are with it;
    the tests that need their own names (12 of the reference's failures) are few.
- **JCS5 The references.**
  - Options: (a) the host's JDK at each run, cached in `.tmp/java-check/ref/` by the sources' and
    the JDK build's hashes (as goi's gc references); (b) recorded as data, `test/java/ref-jdk26.edn`
    (status, stdout's digest and length, stderr's first line, deterministic or not), written by
    `--record-ref` with the JDK, the check needing no JVM (§7.3); (c) the frozen JVM Arbace.
  - *Recommended: (b)*: the check of a standalone Arbace should not need a JDK at each run, and the
    data are small (a few hundred kilobytes for 3,000 to 4,400 tests); (c) is the wrong reference (Arbace,
    not the JDK).
- **JCS6 The levels.**
  - Options: (a) `classes`, `read`, `interp`, `j2g`, `diagnostics`, each with its result (§4); (b)
    `interp` and `j2g` only, the front end left to `bin/javac-vendor classes` and repr/java's
    checks.
  - *Recommended: (a)*: the front end's levels are cheap (§5.6) and they are where a test first
    fails when repr/java or javac's world changes.
- **JCS7 Where the tool lives.**
  - Options: (a) `bin/java-check` with its namespace under `test/java/` (as `test/diffcheck/`,
    `test/j2g/check.ae`), evaluated on the tools' host; (b) `arbace.java.check` under `arbace/`,
    embedded and compiled, as goi's `arbace.go.run`; (c) Python or shell scripts as the
    measurement's.
  - *Recommended: (a)*: the check is a tool of the checkout, like diffcheck; (b) when the box runs
    it (JC8) and needs it compiled.
- **JCS8 How a test's classes are defined at the REPL.**
  - Options: (a) as one top-level `do` (measured: 36 more pass); (b) file by file, as a namespace
    loads them, with the source lookup of SPEC §9.2 (3) made to work on Go (a class name resolving
    to nothing looked up as `p/C.ae` on the path), which every user's multi-file Java at the REPL
    needs too; (c) file by file without lookup (today).
  - *Recommended: (b)* as JC4's work, (a) in the check until then; the check then loads as a user
    would.
- **JCS9 The world's classes.**
  - Options: (a) by design: serialization, Swing/AWT, class loading, XML (J2G-SPEC §12, the user's
    decision of 2026-10-10), and `java.lang.management`/JMX, `java.lang.classfile`, the compiler
    API in the shipped program; deferred: reflection's annotations and generic types, extendable
    superclasses (`Thread`, collections, atomics), zip and jar, logging, `java.lang.invoke`,
    `Timer`, `ServiceLoader`; (b) all deferred, to be decided class by class as JC6 ranks them; (c)
    all by design but the interpreter's own limits (extendable superclasses, protected members).
  - *Recommended: (b)*: JC6's ranking (tests gained per jrt change) is what the decisions need;
    the by-design list of J2G-SPEC §12 stands meanwhile.
- **JCS10 HotSpot's JIT tests.**
  - Options: (a) in the corpus with the JIT, heap and logging options dropped (649 of
    `hotspot/jtreg/compiler`'s, 663 of its 862 passing tests pass interpreted); (b) left out (the
    tests aim at the JIT).
  - *Recommended: (a)*: they are plain programs with known answers about arithmetic, loops, casts
    and array copies, exactly what j2g's Go and the interpreter's nodes must get right; their
    timeouts are a class of their own (speed).
- **JCS11 An analysis gap: the converter's or the analysis'.**
  - Options: (a) the converter writes javac's decision explicitly (a cast, a hint, a pinned call),
    the analysis unchanged (repr/java's direction since JAVA-FRONTEND-SURVEY §2); (b) the analysis
    learns the JLS rule (boxing to `Byte` from a constant, intersection members, `switch` on
    `long`); (c) case by case, the converter for javac's decisions, the analysis for what
    hand-written Java forms need too.
  - *Recommended: (c)*: Arbace's own Java forms are written by hand and meet the same rules.
- **JCS12 Arrays of more than nine dimensions.**
  - Options: (a) the reader takes `C/N` for any N (Clojure's reader stops at 9; Arbace's would
    diverge, VENDOR-NOTES records it); (b) the converter writes such a type another way (a string
    tag `^"[[[...Ljava.lang.Object;"`, which the analysis reads); (c) left: two tests in the
    corpus, a class of their own.
  - *Recommended: (b)*: no divergence from Clojure's reader, and the forms stay readable by any
    Clojure reader; (c) until a Java source needs it.
- **JCS13 The check in the gates.**
  - Options: (a) the essential subset in the essential gate, every corpus at every level in
    `--full`'s java chain (§7.4); (b) a command only, as `check --jdk` (JFS42); (c) `--full` only.
  - *Recommended: (a)*: about a minute in the essential gate, where diffcheck's Java pair already
    is; the interpreter's bugs of §6.2 would have been caught there.
- **JCS14 The first step.**
  - Options: (a) JC1, then JC4 (the interpreter's bugs: 71 tests for what look like two small
    fixes), then JC2; (b) JC4 first, from §6.2's reductions, before any tool; (c) JC1 and JC2
    together.
  - *Recommended: (a)*: the check makes JC4's effect measurable; JC4's fixes are a runtime change
    and want a re-seed, so they batch with JC5's analysis changes if those are ready.

- **JCS15 Go names of classes outside the runtime and the JDK.**
  - Options: (a) j2g names every class of no JDK module and not Arbace's runtime by its binary name
    (as `--qualify` does), its Go file too, so that two packages' `Basic`s or `SuperClass` and
    `Superclass` never meet in `lang`; (b) a Go package per Java package for such classes (as
    diffcheck's `jforms/packages.edn` does by hand); (c) as today, the driver's `--qualify`.
  - *Recommended: (a)*: user Java compiled into a program and the check both need it, and it is
    j2g's naming alone (no package boundaries to cross, unlike (b)).

## Decided (2026-10-24): JCS1-JCS15

The user decided every proposal as recommended:
- JCS1 (a): the milestones JC1-JC8, the concurrency amendments' JC1-JC10 of 2026-10-10 a separate, closed record.
- JCS2 (b): jdk26u's jtreg tests read in place, and a small hand-curated JLS corpus; ECJ's tests (c) after JC1-JC5.
- JCS3 (a): the status and stdout byte for byte against the JDK's run, status only for a test nondeterministic on the JDK.
- JCS4 (a): `package tNNNNN; ` prepended, the JDK's reference run on the renamed sources.
- JCS5 (b): the references recorded as data (`test/java/ref-jdk26.edn`, `--record-ref`), the check needing no JVM.
- JCS6 (a): the levels `classes`, `read`, `interp`, `j2g`, `diagnostics`.
- JCS7 (a): `bin/java-check`, its namespace under `test/java/`, on the tools' host.
- JCS8 (b) as JC4's work, (a) (a test's files as one `do`) in the check until then.
- JCS9 (b): every missing class deferred, decided class by class by JC6's ranking; J2G-SPEC §12 stands.
- JCS10 (a): HotSpot's JIT tests in, their JIT, heap and logging options dropped.
- JCS11 (c): case by case, the converter for javac's decisions, the analysis for what hand-written forms need.
- JCS12 (b): the converter writes a type of more than nine dimensions as a string tag.
- JCS13 (a): the essential subset in the essential gate, every corpus at every level in `--full`.
- JCS14 (a): JC1, then JC4, then JC2; JC1 started 2026-10-24.
- JCS15 (a): j2g names classes outside the runtime and the JDK, and their Go files, by binary name.

## 10. Risks

- **The world is the long tail.** 60% of the non-passing interpreted tests are library classes;
  each class is a decision, and some (serialization) are no for good. The check's value is the
  classes being explicit, not reaching 100%.
- **Nondeterminism.** 87 of 2,121 tests print hash codes, times or interleavings; two JDK runs catch
  most, not all. A test flaky on Arbace alone (threads) needs a retry rule or a class.
- **Speed.** The interpreter is 5-200 times slower than HotSpot (CLASSFORMS-REPL §6); 83 tests
  time out at 60 s. The check's timeout decides a class's size, not a bug.
- **j2g's shards.** A whole-program translation per shard costs minutes and memory (j2g's 20 GiB
  limit); the full gate runs it beside the rest of the java chain.
- **jtreg's semantics.** The runner reimplements part of jtreg (tags, defaults, `@build`,
  `othervm`); a test's meaning can hang on a jtreg detail (agentvm's shared JVM, `test.src`'s
  files). The JDK's reference runs under the same runner, so a difference is the runner's on both
  sides, not Arbace's; but a test jtreg passes may fail under the runner (307 of 2,428 here).
- **The JDK of the reference.** Recorded references (JCS5 (b)) are of one JDK build; a re-pin of
  jdk26u records them again.

## 11. Sources

- jdk26u at `baf63fbe42b8758448fee570a9d9bb914272d259` (`/root/jdk26u`): `test/` (jtreg's tests:
  `jdk/java/lang`, `jdk/java/util`, `langtools/tools/javac`, `hotspot/jtreg/compiler`, and the
  whole tree for §3.1), `test/lib`.
- jtreg's documentation of tags and actions (openjdk/jtreg, `src/share/doc/javatest/regtest/tag-spec.html`,
  GPL-2.0; not on the host, its rules as §3.1 states them).
- The system JDK: OpenJDK 26.0.2.1 (`26.0.2.1-internal-adhoc.root.jdk26u`), `java` and `javac`.
- Arbace main at `317927c0`: doc/go/GOI-SPEC.md §11 and "v1's divergences", doc/classes/SPEC.md
  §9, doc/go/CLASSFORMS-REPL.md, doc/go/J2G-SPEC.md §4.1, §12, doc/classes/JAVA-FRONTEND-SURVEY.md
  (JN3, JN6, JN8, JFS66 (b)), doc/FUTAMURA-SURVEY.md ("F2 as built"), `bin/diffcheck`,
  `bin/j2g-check`, `test/j2g/check.ae`, `test/diffcheck/known.edn`, `bin/arbace-go` (`build_tests`,
  `aot_reusable`), `bin/repr-java`, `bin/javac-vendor` (`classes`' options).
- Eclipse JDT core (https://github.com/eclipse-jdt/eclipse.jdt.core,
  `org.eclipse.jdt.core.tests.compiler`, EPL-2.0), Mauve (sourceware.org, GPL), Apache Harmony
  (attic.apache.org, Apache-2.0), the Benchmarks Game (BSD-3-Clause), Rosetta Code (GFDL-1.2): named
  from their public descriptions, not fetched.

## Appendix A: the scripts

In the worktree's `.tmp/jc/scripts/` (not tracked; Python 3 run with `-I`, the paths absolute):

| script | what it does |
|---|---|
| `scan.py TEST OUT.tsv` | every `@test` block of every `.java` under jdk26u's `test/`: its tags, its actions in order (`@run`, `@compile`, `@build`, `@clean`; jtreg's default `main` when there is none) |
| `classify.py scan.tsv OUT.tsv` | §3.1's categories, by area |
| `prep.py cls.tsv WORK JOBS SELECT...` | §3.2: the source set (`javac -verbose -sourcepath`), the renaming, the JDK's compile and runs (10 s), `WORK/tNNNNN/test.json` |
| `ref2.py WORK JOBS` | the JDK's runs again: deterministic or not |
| `read.py WORK JOBS` | `bin/repr-java read` per test on javac's tool executable |
| `classes_cmp.py WORK JOBS` | javac on Arbace against the JDK's javac, class files byte for byte |
| `interp.py WORK JOBS [EXE] [MODE]` | the interpreter (`interp`, `interp-do`) or j2g's program (`j2g`): per action a driver, the run, the comparison |
| `merge.py WORK OUT MAINS` | the j2g input: every test's forms in one tree, colliding packaged tests left out, the namespaces' loads merged, the roots |
| `j2gbuild.sh FORMS OUT` | `bin/j2g --program` with the forms, the compiled namespaces and goi's table, every `main` a root, then `bin/build-go` (through the host's heavy-command slots) |
| `errtest.py cls.tsv OUT JOBS` | the golden-diagnostics tests by both javacs (§5.5) |
| `classes.py WORK MODE [--list CLASS]` | §6.1's classes, by regexps over the result and its reason |
| `msgs.py WORK MODE` | the first message of each non-passing test |

The data: `.tmp/jc/scan.tsv`, `cls.tsv` (26,271 rows), `work/tNNNNN/` (2,428 tests: `src/`,
`jdk-classes/`, `ref/`, `ref2/`, `forms/`, `cmp/`, `interp/`, `interp-do/`, `j2g/`, `test.json`),
`err/results.tsv`, `classes-interp.txt`, `classes-interp-do.txt`; j2g's: `j2g-forms/` (the merged
input, `j2g-forms.excluded.json`), `j2g-forms2/` and `j2g2.mains` (as finally translated),
`j2g2.prefiltered.tsv`, `j2g2/refused-all.tsv`, `j2g2/goerrs-*.log`, `j2g2/j2g/` (j2g's output and
`report.edn`), `j2g2/arbace-j2g` (the program, 277,482,863 bytes).
