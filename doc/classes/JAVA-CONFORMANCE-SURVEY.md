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
`bin/java-check` · 8 Plan (JC1-JC8) · 9 Proposals (JCS1-JCS15) · Decided · As built: JC1
(JCS16-JCS25) · 10 Risks · 11 Sources · Appendix A: the scripts · As built: JC4 (JCS30-JCS33) ·
As built: JC2 (JCS40-JCS49) · As built: JC3 (JCS50-JCS55).

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

## As built: JC4 (2026-10-24)

Branch `java-interp-jc4`, from main `549c5c49`. JC1's tool was not on main yet, so the measurement
is this section's own (the worktree's `.tmp/jc4/`, not tracked): §5.1's way on two samples of
jdk26u's tests, each put in a package of its own (`package tNNNN; ` prepended), compiled and run by
the JDK twice (10 s; stdout compared only when the two runs agree), read by repr/java on main's
javac tool executable (`REPR_JAVA_STALE_OK=1`), loaded as the REPL loads a package (file by file,
`(require 'tNNNN)`, then `main`; 60 s, 8 jobs), before on main's gate executable and after on this
branch's.

**The causes, traced** (the survey's §6.2 guessed them from the symptoms):

- *A private method on another instance* — and on `this` too: the reduction of §6.2 passes only
  because its first call is not private; `this.run()` of a private `run` fails the same way. The
  analysis gives a private instance method's call the kind `:virtual` (javac's `invokevirtual` of
  a nestmate's private method, JVMS 5.4.6), and the builder (`arbace.classes.interp`, `:invoke`)
  made every `:virtual` call a `CallV`, which looks the receiver's class's vtable up; private
  methods are not in the vtable (`vtable!`, rightly), so it fell to reflection, whose method
  (`real-method`) an interpreted owner does not have: `AbstractMethodError`. Now a private
  instance method of an interpreted owner is called directly (`CallI`, the method itself, the
  receiver in its slot), as CLASSFORMS-REPL §2.2 says; also for an unbound method reference to it
  (`P::run`). Bound references already did so.
- *`Object`'s methods through an interface of the world*: javac qualifies `m.toString()` by the
  receiver's static type, `java/util/Map` (JLS 13.1), and `real-method` looked `toString` up in
  `Map`'s Class, which does not list `Object`'s methods: no method, `AbstractMethodError` when the
  receiver is of the world. Now an interface's lookup falls back to `Object`'s public methods
  (JLS 9.2), and an interpreted owner's to `Object`'s.
- *`clone` of an interpreted `Cloneable` class*: `super.clone()` was a `SuperCall` of `Object`
  (`UnsupportedOperationException: super call of java.lang.Object.clone() is not in the Go
  build`), an unqualified `clone()` a reflective call of `Object.clone` on a `Dyn`
  (`CloneNotSupportedException`). Now both reach `Rt/cloneOf` (`Compiler$CF`): for an object of an
  interpreted class whose superclasses are interpreted up to `Object`, a new object of its class
  with its fields copied (shallow, as `Object.clone`), `CloneNotSupportedException` with the class's
  name when it is not `Cloneable` (`Klass.implementsClass`); any other object as before.
- *The source lookup* (JCS8 (b)): `arbace.classes.analyze/*source-lookup*` was false on Go and
  `enter-from-source!` read `ClassLoader.getResource`'s URL, which jrt does not have. Now it is
  true everywhere (j2g binds it false, as before) and reads `RT/resourceAsStream` (ARBACE_PATH,
  then the embedded sources). Measured, it recovered none of the sample's tests: they are one
  Java file each, and a class of a later *form of the same file* (repr/java writes a file's
  top-level classes as consecutive `defclass`es) is not `p/C.ae`. So the lookup also tries the
  file being loaded (`*file*`): its later forms are entered as declarations, as javac compiles a
  file's classes together (SPEC §9.2 (3), amended; JCS31). That recovered 7.

Changed: `arbace/classes/interp.ae` (the builder: `private-method`, `real-method`, `clone`'s
`ObjectCall`), `arbace/lang/Compiler.ae` (`Compiler$CF`: `Rt/cloneOf`, `Klass.worldSuper`,
`implementsClass`, `ObjectCall`'s and `CallV.reflect`'s `clone`), `arbace/classes/analyze.ae` (the
lookup). They are embedded namespaces and the runtime: the shipped program changes, the re-seed
batched (not done here).

**Numbers.**

| sample | tests (pass on the JDK, read) | before | after | of the bugs' classes before | after |
|---|---:|---:|---:|---:|---:|
| A: every single-file plain `@run main` test of `jdk/java/lang`, `jdk/java/util`, `langtools/tools/javac` and `hotspot/jtreg/compiler` (no `@library`, `@modules`, `@requires`, `@build`, `@compile`; HotSpot's VM options dropped) | 324 of 354 | 205 | **221** | 9 | 8 pass, 1 moved (`Proxy/Boxing`: a `ClassCastException` of `Proxy`'s, the world) |
| B: tests found by grepping for the patterns (private instance methods, `Cloneable` with `clone()`, interface-typed receivers of `toString`/`getClass`) | 24 of 27 | 2 | **13** | 11 | 11 pass |

Sample A by step: the three fixes 205 → 215 (8 of the bugs' tests, 2 HotSpot tests that had timed
out at 60 s and did not this time), the lookup 215 → 221 (7 tests whose class of a later form was
unknown or unresolved: `LambdaInSuperCallCapturingOuterThis` 1-3, `CheckNoClassCastException`, three
HotSpot tests; one timeout the other way). No test passing before fails after. Of sample B's 11
not passing after, every one is in another class of §6.1 (serialization, `PhantomReference`,
`System.runFinalization`, extending `Thread`, an `AbstractMap` constructor, qualified `super`, a
lambda's conversion, a multi-file test).

What remains of §6.1's REPL classes: a superclass (or a static initializer's class) of a later form
of the same file or `do` (sample A: `PvtMbrsNotInherit1`, its `OtherClass` declared by the lookup,
not yet defined when the subclass is laid out): JCS30.

**Regression cases.** The oracle's `test/oracle/forms/defclass.ae` gains 14 cases at its end
(private methods on `this`, another instance and from a nested class; `Object`'s methods through
`Map`, `Collection`, `List`, `CharSequence` and an interpreted interface; `clone` by `super.clone()`
and `this.clone()`, of a subclass, and of a class not `Cloneable`), recorded on the frozen JVM
(`bin/oracle record forms/defclass.ae`, 213 → 227 cases): main's executable fails 6 of them, this
branch's none; diffcheck's Java pair runs them interpreted against j2g's translation. The smoke
test (`bin/arbace-go --smoke`) gains "Java forms: a class of a later file of the package (the
source lookup)": `jl.A` names `jl.B`, loaded after it (main's executable: `Unknown type: B`).

**Checks** (the essential `bin/gate` at the code's head `2572f76b`, passed in 9m21s): the smoke
test with the new check; Clojure's suite on the test build, no regressions; the oracle 22,672 of
22,725, its 53 known mismatches as recorded (the 14 new `defclass` cases among the matches);
diffcheck's Java pair 737 of 739 cases agree (723 before, the 14 new ones agreeing), its two known
disagreements, Clojure's pair 12,020 of 12,041 as recorded; goi's essential check 801 of 801;
the generated variants; j2g's edge. `test/diffcheck/known.edn` unchanged.

## Proposals of JC4 (JCS30-JCS33)

- **JCS30 A superclass of a later form.** A file's (or a `do`'s) classes are defined form by
  form; a subclass whose superclass is a later form fails, though the lookup now declares it.
  - Options: (a) a file of Java forms is loaded as one compilation: `load` gathers a file's
    consecutive top-level `defclass`es into one `define!` (supertypes first, as `define!` already
    orders one compilation's classes), the other forms in place; (b) repr/java writes a Java
    file's top-level classes as one `defclasses` form, so that the file is one compilation by
    construction, and a top-level `do` of Java forms is one `define!` (SPEC §9.2 (2)'s `do`
    taken further: defined, not only declared, together); (c) leave (5 to 9 tests of the survey's
    sample, a class of their own).
  - *Recommended: (b)*: it changes nothing of how a file loads, javac's unit (the compilation
    unit) becomes the forms' unit, and a hand-written `do` of mutually dependent classes works the
    same way; repr/java's change is JFS's, so it is the converter's agent's (JC5 with JCS11).
- **JCS31 The lookup in the file being loaded.** Built here beyond SPEC §9.2 (3) as decided
  (JCS8 (b) said `p/C.ae`).
  - Options: (a) keep it, SPEC §9.2 (3) amended (done on the branch); (b) `p/C.ae` only, the
    file's later forms left to JCS30.
  - *Recommended: (a)*: it is what javac does for a file's classes, it recovered 7 of sample A's
    324 where `p/C.ae` recovered none, and JCS30 (b) would make it redundant only for converted
    files, not for hand-written ones.
- **JCS32 `clone` of an interpreted class extending the world.** `Rt/cloneOf` copies interpreted
  objects whose superclasses are interpreted up to `Object`; a `Cloneable` subclass of a world
  class (a `DynSub`) still throws `UnsupportedOperationException`.
  - Options: (a) a native of `Compiler$CFGo` (`arbace.j2g.dyncf`) copying a `DynSub`'s world part
    with its Go value and the fields; (b) leave until a test needs it (none of either sample did).
  - *Recommended: (b)*.
- **JCS33 The check's load.** JCS8 decided the one-`do` load in the check until the lookup works.
  - Options: (a) JC1's `interp` level loads file by file now (as a user's `require`), the
    one-`do` load kept as a mode for comparison; (b) keep the one-`do` load as the check's.
  - *Recommended: (a)*, when JC1's tool and this branch are both on main: the check then
    measures what a user gets, and the difference between the two loads is JCS30's measure.

## As built: JC1 (2026-10-24)

Branch `java-check-jc1` from main `549c5c49`. `bin/java-check` is the check of §7.1 with the levels
`read`, `interp` and `classes` (JCS6 (a); `j2g` is JC2's, `diagnostics` JC3's: the levels are a
comma list, `check_one`'s cases, and the results are per level, so each lands as one more case),
its data side the namespace `java.check` (`test/java/check.ae`, on the tools' host, JCS7 (a)), the
interpreted runs `test/java/run.ae` (a script on the executable under test). Nothing under
`arbace/`, `go/` or `overlay/` changed: the shipped program is the same, and so are the gate's
keys (`test/java/` is in neither `gate_key` nor the javac tool's key).

**What it is.**

- *The corpora* (`test/java/corpora.edn`, JCS2 (b)): `jdk-run` (`jdk/java/*` but `awt`),
  `langtools-run` (`langtools/tools/javac`), `hotspot-compiler` (`hotspot/jtreg/compiler`), each of
  the categories run-plain, run-compile and run-jitopts, read in place from jdk26u's `test/` at the
  platform's pin (checked against `jdk/javac/platform.sha256`); and `jls`, Arbace's own hand-curated
  programs in `test/java/jls/` (11: evaluation order, string concatenation and the floating
  digits, initialization order, numeric conversions, nested classes, lambdas and method references,
  switches and patterns, exceptions and `finally`, overload resolution, generics and bridges, enums
  and records; each a `main` printing what it checks, its expected output the JDK's run, as for
  jdk26u's). `java.check/scan` reads jtreg's tags from each `.java` file's block comments (a tag is
  an `@` after white space and one of jtreg's names; the actions in order, jtreg's default `@run main`
  when there is none) and puts each description in the first category of §3.1 that fits
  (`java.check/categorize`; its counts by area are within a few of §3.1's: `hotspot/jtreg/compiler`
  83 run-plain and 649 run-jitopts as there, `java/lang` 355 run-plain for 359, `langtools` 357 for
  356 and 222 run-compile for 267: `@run compile` and `@clean` actions count as other). Kept within a
  category: `@compile` options that change javac's output but not the run's meaning (`-g`, `-XD...`,
  `-Xlint`, `-Werror`, `-parameters`, `-source`, `--release`, `-encoding`); the JIT options dropped
  (JCS10 (a)), and two main actions equal once they are dropped run once.
- *Ids and renaming* (JCS4 (a)): each test of the corpora gets an id `tNNNNN`, kept by the reference
  (`test/java/ref-jdk26.edn` is the registry: a new test takes the next free number); its source set
  is what the JDK's `javac -verbose -implicit:class -sourcepath DIR` parses for its file, its `@compile`
  files, `@build` classes and main classes; a file of the unnamed package gets `package ID; ` before
  its first line and goes to `ID/`, one with a package keeps it; a main class without its package
  takes the main file's (jtreg's `${test.main.class}`) or `ID.`.
- *The reference* (`--record-ref`, JCS5 (b)): with the system's JDK (`javac`, `java` on the path,
  OpenJDK 26 built from jdk26u), the renamed sources compiled as the test's `@compile` options say and
  each main action run twice (an empty directory, `-Dtest.src=DIR -Dtest.classes=CLASSES`, `-Xmx2g`,
  60 s), and compiled again with `-g:none -nowarn -XDsuppressNotes -proc:none -implicit:none` for the
  classes level. Per test, one line of `test/java/ref-jdk26.edn` (1.5 MB): id, jtreg name, corpus,
  category, the JDK's result (`:pass`, `:fail`, `:skip` with why), the directory, javac's options,
  the source set and each file's place, the class files' digest, and per action the class, arguments,
  jtreg's timeout, the status, stdout's SHA-256 (16 hex digits) and length, stderr's first line and
  `:det` (the two runs agree). The check reads only this: no JVM.
- *The levels*: `read`, `bin/repr-java read` of the renamed sources on javac's tool executable (the
  test's `-source` and `-encoding` passed, `--release` dropped), cached in `.tmp/java-check/forms` by
  the sources, those options, repr/java's reader hash and the tool's key; `interp`, per main action a
  fresh process of the executable (`JAVA_CHECK_EXE`, the gate's by default) running `test/java/run.ae`:
  the test's packages in the order of their imports of each other, each defined as one top-level `do`
  of its `defclass`es after its namespace form and its files' `in-ns`/`import` forms (JCS8 (a)), then
  `main` with the arguments and the `-D` properties; an uncaught exception printed as the JVM's
  launcher prints it, status 1; a definition that fails `load-failed` with its last cause; pass when
  the status and stdout's digest and length are the JDK's (the status alone for a test whose JDK
  runs differ, JCS3 (a)), else `fail` (the status), `differ` (stdout), `timeout` (20 s an action, the
  definition included), `unread`; `classes`, `bin/repr-java javac` with the reference's options, the
  class files' digest against the JDK javac's.
- *The load* (JCS8 (a), and the user's JCS33 (a) of JC4): `--load do|files` (`JAVA_CHECK_LOAD`), `do`
  the default as built; `files` requires each package's namespace as the REPL loads it, file by
  file, the forms' root on `ARBACE_PATH`. On the essential subset without JC4's source lookup,
  `files` passes 412 of 428 (16 a class of a later file: SPEC §9.2, 3). Once JC1 and JC4 are both on
  main, `files` becomes the default and the references are recorded again; `do` stays the
  comparison mode.
- *Results and classes*: `.tmp/java-check/results.tsv`, against `test/java/check-LEVEL.edn` (as
  recorded, new or changed, now passing; a test passing there and not now is a regression; `--record`
  writes them, a partial run updating its tests' lines); every test not passing in a class of
  `test/java/divergences.edn` (36 classes: id, title, kind, why, where decided, regexps over
  `RESULT REASON` or test ids, an optional level), none in no class; the numbers by area.
- *The essential subset* (JCS13 (a)): `test/java/essential.txt`, 428 tests passing interpreted in
  under a second and deterministic on the JDK, every k-th of each area (120 of HotSpot's, 140 of
  langtools', 160 of `jdk/java`'s) and the 8 passing programs of `jls`; their forms committed in
  `test/java/forms/ID` (1.0 MB, 880 files, `provenance.edn`; GPL-2.0 as jdk26u's `test/`, LICENSE.md),
  so that the essential gate needs no javac (`bin/java-check --essential`: the level `interp` from them,
  every one must pass); `--forms verify` (in `--full`) reads them afresh and compares byte for byte,
  `--forms update` writes them. In `bin/gate`: `java-check` beside `goi-check` in the essential
  step, and `java-check-full` (`--levels read,interp,classes`, then `--forms verify`) at the end of
  the java chain. Makefile: `java-check`, `java-check-essential`, `java-check-ref`.

**Measured** (the gate's executable of main `549c5c49`, `gate_key` `881effe6…`; javac's tool of
04:35 the same day, key `bf740cf0…` for the checkout's `22385f97…`, run with `REPR_JAVA_STALE_OK=1`:
javac's world has not changed since, repr/java's namespaces load from the checkout where they differ;
8 jobs):

| step | tests | wall |
|---|---:|---:|
| scan (10,978 descriptions under the areas) | 3,218 in the corpora | 7 s |
| `--record-ref` (source sets, the JDK's javac twice, two runs) | 3,218 | 21 min |
| the whole check, `read,interp,classes`, cold | 2,967 | 22 min |
| the same, the read cached | 2,967 | 10.4 min |
| `--essential` | 428 | 28 s |

The reference: 3,218 tests, **2,967 pass on the JDK** (126 of them nondeterministic: status only),
179 fail there under the runner (an unqualified name expected, a file under `test.classes`, a timeout
over 60 s, `@run` options the corpus kept), 72 skipped (`@compile` of `.jasm` files, a substitution in
arguments, a source set javac rejects alone). By corpus: `jdk-run` 1,744 of 1,896, `langtools-run`
515 of 579, `hotspot-compiler` 697 of 732, `jls` 11 of 11.

| level | hotspot compiler | java/lang | java/util | java/io | java/net | java/nio | java/beans | other java/* | langtools | jls | **all** |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| tests (pass on the JDK) | 697 | 354 | 375 | 256 | 154 | 151 | 246 | 208 | 515 | 11 | **2,967** |
| read | 696 | 354 | 375 | 256 | 154 | 151 | 246 | 208 | 514 | 11 | **2,965** |
| classes (byte for byte) | 697 | 353 | 375 | 256 | 154 | 151 | 246 | 208 | 514 | 11 | **2,965** |
| interp | 521 | 145 | 147 | 80 | 49 | 6 | 0 | 76 | 332 | 8 | **1,364 (46.0%)** |

("Other java/*": math 35 of 38, text 38 of 50, security 3 of 101, rmi 0 of 18, sql 0 of 1.) On the
survey's four areas the interpreted numbers are §5.3's: `java/lang` 145 of 354 (41%; §5.3 39%),
`java/util` 147 of 375 (39%), HotSpot's 521 of 697 (75%; 77% of §5.3's 862, which held its packaged
tests the runner here takes as theirs), langtools 332 of 515 (64%; 62%). The whole is lower than
§5's 60% because the corpus now holds `java/beans` (246, none passing), `java/nio` (6 of 151),
`java/net` and `java/security`.

The interpreted results by class (`interp`, 1,603 not passing; `bin/java-check` prints them):

| class | kind | tests |
|---|---|---:|
| speed (20 s) | speed | 102 |
| the interpreter: a private method on another instance (`AbstractMethodError`) | interp | 51 |
| the interpreter: an `Object` method through a world type | interp | 13 |
| the interpreter: `clone()` of an interpreted class | interp | 5 |
| the interpreter: the JLS corpus's findings (below) | interp | 3 |
| a superclass the Java forms cannot extend yet | interp | 82 |
| a protected or inherited member of a world class | interp | 11 |
| the REPL's model (a class of the `do` used before it is defined; another package's class) | repl | 11 |
| the analysis (forms javac accepted, `arbace.classes.analyze` refuses) | analysis | 70 |
| repr/java: arrays of more than nine dimensions | repr-java | 2 |
| not read (the read level's failure) | harness | 2 |
| serialization; AWT and Swing; class loading | design | 146; 17; 13 |
| `java.beans` | world | 230 |
| networking; security; `java.io`'s missing classes; NIO | world | 192; 91; 98; 44 |
| reflection's annotations and types; zip and jar; javac's API; `java.lang.invoke`; management | world | 69; 59; 39; 34; 30 |
| logging; threads' members; references; classfile; charsets and locales; stack walking | world | 27; 23; 15; 11; 10; 9 |
| other JDK classes and members | world | 39 |
| identity hash codes or traces in stdout; the run's environment | harness | 11; 2 |
| other failures, each its own (to triage) | other | 41 |

The read level's two failures: repr/java's converter overflows its stack on
`compiler/uncommontrap/UncommonTrapStackBang.java` (a new repr/java finding, JCS24), and javac on Arbace
refuses `-encoding utf8` (`Arguments.reportDiag`: the alias, not the charset); the classes level's
two are `--release 8` and `--release 17` (no `ct.sym`, §5.5).

**What the JLS corpus found at once** (three interpreter bugs no jtreg test of the sample isolates;
`test/java/jls`, the class `:interp-jls`, JC4's):

- `try { throw new RuntimeException("lost"); } finally { return "swallowed"; }` rethrows the exception
  instead of returning (JLS 14.20.2: the `finally`'s abrupt completion replaces the `try`'s);
- `v((Object) "s")` with `v(Object)` and `v(String)` calls `v(String)`: the overload chosen by the
  argument's run-time class, not the cast's static type (JLS 15.12.2);
- `static final String K = "k" + 1 + 2;` then `K == "k12"` is `false`: a constant expression's string
  not interned (JLS 15.29, 3.10.5).

The private-call bug of §6.2 is 51 tests here (58 in the survey's sample); the classes' sizes
otherwise follow §6.1 with the wider corpus's world classes added.

**Proposals (JCS16-JCS25).** JCS30 on are JC4's.

- **JCS16 `java.beans` in the corpus.**
  - Options: (a) as built: `jdk/java/beans` in `jdk-run`, its 246 tests in the world class
    `:beans`; (b) `jdk/java/beans` excluded as `jdk/java/awt` is (`corpora.edn`'s `:exclude`): it is
    `java.desktop`'s, the module J2G-SPEC §12 leaves out with AWT and Swing; (c) kept, the class
    `:beans` made `:design`.
  - *Recommended: (b)*: no test of it can pass without `java.desktop`; the number then says what the
    Go build can run.
- **JCS17 Identity hash codes in stdout.**
  - Options: (a) as built: such a test `differ`s, the class `:identity` (11); (b) `--record-ref`
    records also the digest of stdout with `@` and 6-8 hex digits masked, compared when the plain one
    differs; (c) both sides run with a fixed identity hash (not available on the JDK).
  - *Recommended: (b)*: the tests' meaning does not hang on the hash's digits.
- **JCS18 The JDK's stdout for the differences.**
  - Options: (a) as built: digests only, the reason our stdout's start; (b) the JDK's stdout of the
    tests under 4 KB kept in a data file beside the reference, so a `differ` names the first
    differing line; (c) the JDK at hand when triaging only (`--record-ref --only`).
  - *Recommended: (c)* now, (b) if JC4's triage wants it often: the reference stays small.
- **JCS19 The run's environment.**
  - Options: (a) as built: the JDK's runs in `.tmp/java-check/record/ID/w`, `java.io.tmpdir` the
    JVM's `/tmp`; the interpreter's in `.tmp/java-check/run/ID/w`, `TMPDIR`'s `.tmp/tmp`; (b) both in
    a directory of the same name relative to a common root and `java.io.tmpdir` set alike
    (`-Djava.io.tmpdir`, `TMPDIR`) at the next `--record-ref`.
  - *Recommended: (b)*: two tests (`:environment`) and any later one printing a path.
- **JCS20 Assertions.**
  - Options: (a) as built: both sides without `-ea` (the JVM's default; jtreg's own runs commonly
    enable them); (b) `-ea -esa` on the JDK and the interpreter's `java-assert*` enabled likewise.
  - *Recommended: (a)* until JC4 says how the interpreter's assertions are switched: a test whose
    assertions fail on one side only would read as Arbace's.
- **JCS21 The essential subset's forms committed.**
  - Options: (a) as built: `test/java/forms/` (1.0 MB, GPL-2.0, LICENSE.md), `--forms verify` in
    `--full`; (b) none committed, the essential gate building javac's tool (minutes and a heavy
    slot) or skipping without it; (c) the essential subset only `test/java/jls` (no GPL text, 8 tests).
  - *Recommended: (a)*: as `test/java-fixtures/` is for the suite's fixtures; the essential gate
    stays javac-free and takes 28 s.
- **JCS22 The interpreted timeout.**
  - Options: (a) as built: 20 s an action, the definition included, for every test; (b) jtreg's
    timeout (120 s, `/timeout=N`) times a factor; (c) (a) and a list of tests with their own limits
    (`test/java/timeouts.txt`, as goi's).
  - *Recommended: (a)*, (c) once JC4 has the interpreter faster: 93 of the 102 timeouts are
    HotSpot's warm-up loops, which no sensible limit lets through today.
- **JCS23 Test ids.**
  - Options: (a) as built: numbers in the scan's order, the reference the registry, a new test the
    next number; (b) a digest of the jtreg name (stable without the registry, unreadable).
  - *Recommended: (a)*.
- **JCS24 repr/java's stack overflow on a deep expression.**
  - Options: (a) the converter iterative over long binary chains (JC5); (b) the tool's goroutine
    stack raised; (c) left in its class.
  - *Recommended: (c)* now, (a) with JC5: one test, and the native parser's depth is the same question.
- **JCS25 The JLS corpus.**
  - Options: (a) grown by JLS chapter as JC4 and JC5 fix what it finds, each program Arbace's own,
    its expected output the JDK's run; (b) frozen at JC1's eleven, ECJ's extracted tests later
    (JCS2 (c)).
  - *Recommended: (a)*: eleven programs found three bugs the 2,956 jtreg tests do not isolate.

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

## Decided (2026-10-24): JCS30-JCS33

The user decided JC4's proposals as recommended: JCS30 (b) (repr/java writes a Java file's top-level
classes as one `defclasses`, a top-level `do` one definition; JC5's, with JCS11), JCS31 (a) (the lookup
also in the file being loaded, SPEC §9.2 (3) amended), JCS32 (b) (`clone` of an interpreted class
extending a world class left until a test needs it), JCS33 (a) (JC1's interp level file by file once
JC1 and JC4 are both on main, the one-`do` load kept as a comparison mode).

## Decided (2026-10-24): JCS16-JCS25

The user decided JC1's proposals as recommended: JCS16 (b) (`jdk/java/beans` excluded as `jdk/java/awt`
is), JCS17 (b) (stdout's digest with identity hashes masked recorded too), JCS18 (c) (the JDK at hand
when triaging, the reference small), JCS19 (b) (both sides run in like-named directories with the same
tmpdir, at the next `--record-ref`), JCS20 (a), JCS21 (a), JCS22 (a), JCS23 (a), JCS24 (c) (repr/java's
stack overflow in its class; the converter iterative with JC5), JCS25 (a) (the JLS corpus grown by
chapter). JCS16, JCS17, JCS19 and JCS33 (a) (the interp level file by file, JC4 now on main) are built
with JC2.

## As built: JC3 (2026-10-24)

Branch `java-check-jc3`, from main `0050e92a`. JC3 adds the level `diagnostics` to `bin/java-check`;
the levels `classes` and `read` were JC1's and stay as they are: `classes` was already judged
against the JDK javac's class files recorded as digests (the reference's `:classes`, the first 16
hex digits of the SHA-256 of their `sha256sum` listing), with §5.2's numbers as its reference
(`test/java/check-classes.edn`: 2,965 of 2,967, the two `--release 8` and `--release 17`), and
`read` likewise (2,965 of 2,967). Nothing under `arbace/`, `go/`, `overlay/` or `jdk/` changed: the
shipped program, javac's tool and the gate's keys are the same.

**What it is.**

- *The corpus* `langtools-diagnostics` (`test/java/corpora.edn`): every `compile-fail-ref` test
  (§3.1's category: a `@compile/fail/ref=F` action) under jdk26u's `test/langtools/`: 1,212 tests,
  1,203 of `tools/javac` and 9 of `tools/doclint` (§3.1's measurement counted 1,211 by its
  own scan). Ids `t06427`-`t07638` (renumbered by one at the merge with JC2, whose JLS program took `t06426`),
  after JC1's.
- *Run as jtreg runs them, in place*: a golden file names the test's files and classes
  (`T.java:10:5: compiler.err...`), so these tests are not renamed (JCS4 (a) is the run levels').
  The scan keeps each test's `@build` and `@compile` actions in order with their tokens as written,
  its `@library`, `@modules` and `@enablePreview` (`java.check/diag-plan`); `diag_actions` runs them
  with one classes directory: `-d CLASSES -sourcepath DIR:LIBRARIES -classpath CLASSES` (jtreg's;
  `@library /x` under the test's jtreg root, the directory of `TEST.ROOT`), `@modules m/p` as
  `--add-exports m/p=ALL-UNNAMED` (a module alone as `--add-modules`), `@enablePreview` as
  `--enable-preview --source 26`, jtreg's substitutions `${test.src}`, `${test.classes}` and
  `${jdk.version}`, `/process` as `-proc:only`; `@build C` compiles `C.java` found on the source
  path. javac's output (stdout and stderr together, stdin empty) is compared with `F` as jtreg
  compares a golden file: line by line, each line's trailing white space and CR dropped.
  Skipped, with why: `@clean` between the actions (5), `@compile/module=` (3).
- *The reference* (`--record-ref`, JCS5 (b)): the same actions by the JDK's javac; per action the
  status, the normalized output's SHA-256 (16 hex digits) and length, and `:golden` (equal to `F`).
  The JDK passes a test when each action's status is what jtreg expects (0, or 1 for `/fail`) and
  each `ref=` output is the golden file. `--record-ref --only kind.diagnostics` recorded them alone
  (gather keeps the other tests' lines byte for byte; `java.check/gather-diag` writes the golden
  tests' lines and the reference's header note about them).
- *The check* (`--levels diagnostics`, `check_diag`): the same actions by javac on Arbace
  (`bin/repr-java javac`); `pass` when each status is the JDK javac's and each `ref=` output the
  golden file; `fail` (a status; the reason the output's first line), `differ` (an output; the
  reason its first differing line, ours and the golden file's), a crash of the compiler named by
  its exception. `java.check/plan-diag` gives the golden tests to the level `diagnostics` alone
  and the run tests to the others, so `--levels read,interp,classes,diagnostics` runs each test at
  its levels. Results in `test/java/check-diagnostics.edn` (`--record`), every non-pass in a class
  of `test/java/divergences.edn` with `:level :diagnostics`.
- *In the gates*: `bin/gate --full`'s `java-check-full` now runs `--levels
  read,interp,classes,diagnostics` (§7.4; the essential gate is unchanged: the level needs javac's
  tool); the REAP matrix's Java read cell takes `check-diagnostics.edn` as a third source
  (`test/reap/cells.edn`, the new kind `:javac` mapped to deferred); Makefile
  `java-check-diagnostics`.

**Measured** (javac's tool of main's `target/arbace-go/amd64/arbace-javac`, key `bf740cf0…`, built
04:35 the same day, run with `REPR_JAVA_STALE_OK=1`: javac's world, `jdk/javac` and
`overlay/jdk/javac`, has not changed since; the worktree's key is `3a1913ee…`. The JDK the system's
26.0.2.1. 8 jobs):

| step | tests | wall |
|---|---:|---:|
| `--record-ref --only kind.diagnostics` (the scan, the JDK's javac in place) | 1,212 | 177 s (the actions about 2 minutes) |
| `--levels diagnostics` (javac on Arbace) | 1,194 | 60-65 s |

The reference: **1,194 of 1,212 pass on the JDK** (§5.5: 1,157 of 1,211 matched; the source path,
`@build`, `@library` and `@modules` give back most of §5.5's "JDK differs"), 8 skipped (above), 10
fail there: 8 build a class from a `.jcod` file (`@build Foo` of `Foo.jcod`, `@compile X.jcod`:
jtreg's own class-file assembler, which the runner does not have), `TypeVariableMissingTA` (its
`-cp .`, jtreg's working directory, replaces the classes directory, so the processor is not found)
and a doclint module test whose `@modules` names `jdk.javadoc` for `--add-exports` (a warning,
status 1 under `-Werror`).

| javac on Arbace, the diagnostics level | tests |
|---|---:|
| **pass** (each status the JDK javac's, each output the golden file) | **1,101 (92.2%)** |
| `--release N` (no `ct.sym`: "release version N not supported", 8-26) | 44 |
| annotation processors from class files ("Could not instantiate an instance of processor") | 25 |
| `-Xdoclint` (doclint not in javac's world: `compiler.warn.doclint.not.available`) | 8 |
| javac on Arbace's file manager: file names in full in raw diagnostics (2), no `source.no.system.modules.path` warning for `-source N` (4), `-source 8`'s platform classes (1), a `module-info.java`'s error reported twice (1) | 8 |
| javac on Arbace's command line: `-XDexpectKeys` ignored (1), no error count after a plug-in error (1), `-encoding ascii` and `--module-path missing.jar` refused (2) | 4 |
| a class outside javac's closed world (`java.lang.Module` in the processing environment's processor loader, 3; `java.lang.annotation.Annotation` in `Check`'s leak check under `--patch-module`, 1): the compiler's crash | 4 |
| **all** | **1,194** |

So §5.5's finding holds and is sharpened: wherever javac on Arbace's world has what a test asks,
its diagnostics are the JDK javac's byte for byte (no test differs in a diagnostic javac itself
computes); what differs is the platform (`ct.sym`), what the world lacks (doclint, a class loader for
processors, two closed-world classes) and javac on Arbace's own launcher and file manager
(jrt's own Java in `overlay/jdk/javac`, As built: JN3), which take javac's API, not its command
line: the last two classes (12 tests) are the only ones whose fix is Arbace's own small Java.

**Proposals (JCS50-JCS55).**

- **JCS50 `--release N`.**
  - Options: (a) the platform gains the JDK build's `lib/ct.sym` (pinned with the platform,
    `jdk/javac/platform.sha256`) and javac on Arbace's file manager serves `--release N` from it as
    javac's `JDKPlatformProvider` does (a zip read through jrt's zip classes, not a zip file
    system); (b) `--release 26` (the current release) accepted as `-source 26 -target 26` with the
    system modules, older releases left in the class; (c) by design: javac on Arbace compiles for
    its own platform only.
  - *Recommended: (a)*, with (b) as its first step: `--release` is what build tools pass (Maven's
    `maven.compiler.release`, Gradle's `options.release`), 44 of the 93 tests, and the read and
    classes levels' two; (c) would leave every such build to `-source`/`-target`.
- **JCS51 Annotation processors.**
  - Options: (a) deferred, then processors given as sources defined by the interpreter
    (`arbace.classes.interp`, the REPL's way of making a class) and handed to javac's processing
    environment as instances (javac's `-processor` naming a class of the processor path's sources);
    (b) by design: a processor is a class file loaded by a class loader, out of the Go build
    (J2G-SPEC §12); (c) a closed list of processors compiled into javac's tool, as the JDK build's
    tools are (JN7).
  - *Recommended: (a)*: the processing tests (25) and much of the ecosystem (Lombok, record and
    mapper generators) are processors; the interpreter already defines classes from Java forms, so
    no class loader is needed; (c) fits the build's own tools, not a user's.
- **JCS52 doclint.**
  - Options: (a) jdk.javadoc's doclint (`jdk.javadoc.internal.doclint`, which javac finds through
    `DocLint.newDocLint()`'s service lookup) added to javac's world (`jdk/javac`, `bin/javac-vendor
    convert`), the service bound as javac's world binds its others; (b) by design: doclint is
    javadoc's, not the compiler's.
  - *Recommended: (a)*: `-Xdoclint` is a javac option the JDK's javac honours, the closure is a
    few dozen classes of plain Java, and 8 tests check it.
- **JCS53 javac on Arbace's command line and file manager.**
  - Options: (a) javac on Arbace's `Main` calls javac's own `com.sun.tools.javac.main.Main.compile`
    with Arbace's file manager put in the context (javac's `Main` uses a context's file manager),
    so the command line's behaviour (`-XDexpectKeys`, the counts, the option checks and warnings)
    is javac's by construction, and the file manager's file objects give javac what a
    `PathFileObject` does (short names, `isSameFile`, the system modules location's default) ;
    (b) each difference patched in `jdk.internal.jrt.javac.Main` and `FileManager` as found; (c) left
    in their classes.
  - *Recommended: (a)*: 12 tests now, and every later difference of javac's command line is then
    javac's own code, as the rest of javac on Arbace is; `-encoding` other than UTF-8 and a jar on
    `--module-path` stay refused (jrt's charsets, no jar file system) and in the class.
- **JCS54 The golden warnings tests.**
  - Options: (a) the corpus widened to every test with a `ref=` action, also `@compile/ref=F`
    without `/fail` (233 actions: lint warnings and notes golden files, today categorized
    `compile-only` or other), the level unchanged; (b) the 1,212 `compile-fail-ref` tests only,
    as built.
  - *Recommended: (a)*: the same runner and comparison, and lint's warnings are diagnostics the
    level does not see yet.
- **JCS55 The tests the runner cannot run.**
  - Options: (a) as built: `@clean` and `@compile/module=` skipped, `.jcod` tests failing on the
    JDK (18 in all, not checked); (b) the runner learns `@clean` (delete the named classes' files),
    `/module=M` (`--patch-module M=DIR`, which javac on Arbace takes) and jtreg's working directory
    (`-cp .`), `.jcod` left out.
  - *Recommended: (b)*: small cases of `diag_actions` and up to 9 more tests; `.jcod` needs jtreg's
    assembler (asmtools' `jcod`), a tool of its own.

The scratch this section cites (the worktree's `.tmp/`, not tracked): `jc3-record.log` (the
reference's run), `jc3-diag.log` (the check's run), `jc3-diagnostics-results.tsv` (its results).

## Decided (2026-10-25): JCS50-JCS55

The user decided JC3's proposals as recommended: JCS50 (a) with (b) as its first step (`--release 26`
accepted as `-source`/`-target 26`, then the JDK build's pinned `lib/ct.sym` in the platform, served
as `JDKPlatformProvider` does), JCS51 (a) (processors deferred, then given as sources, defined by the
interpreter and handed to javac's processing environment as instances), JCS52 (a) (jdk.javadoc's
doclint added to javac's world through `bin/javac-vendor convert`), JCS53 (a) (Arbace's `Main` calling
javac's own `Main.compile` with Arbace's file manager, file objects behaving as `PathFileObject`;
non-UTF-8 `-encoding` and a jar on `--module-path` stay refused), JCS54 (a) (the diagnostics corpus
widened to every `ref=` action), JCS55 (b) (the runner learns `@clean`, `/module=M` and jtreg's working
directory, `.jcod` left out). The five `:pending` classes of the diagnostics level cite this.

## As built: JC2 (2026-10-24)

Branch `java-check-jc2`, from main `45c41a46` (JC1, JC4, REAP1, JFS65 (b), JFS75 (b) on it). The
compiled level of `bin/java-check` (§7.2, §8's JC2), j2g's findings of §5.4, and JC1's decided
follow-ups (JCS16 (b), JCS17 (b), JCS19 (b), JCS33 (a)). The executables: the gate's build of main
`45c41a46` (its `gate_key` equal to the branch's before the j2g changes) interprets and hosts j2g
(`J2G_GO_EXE`; j2g's changed namespaces load from the checkout, evaluated); javac's tool executable of
main (key `bf740cf0…`, `REPR_JAVA_STALE_OK=1`, as JC1 ran); the compiled namespaces are main's shipped
build's (`target/arbace-go/aot`, its `aot_key` `6437f7c9…` the branch's before the j2g changes); the
JDK the system's OpenJDK 26.0.2.1. 8 jobs; every j2g run and build in one of the host's heavy slots (a
whole compiled run holds one).

**JC1's follow-ups.**

- *JCS16 (b)*: `jdk/java/beans` is excluded as `jdk/java/awt` is (`test/java/corpora.edn`); the
  reference drops the tests the corpora no longer take (`java.check/gather`: a test of the old
  reference that the scan no longer selects is not kept): 2,951 tests, **2,721 passing on the JDK**
  (2,967 less beans' 246), and since JC2's own JLS program 2,952 and 2,722.
- *JCS17 (b)*: `--record-ref` records, beside stdout's digest, its digest with identity hash codes
  (`@` and 6 to 8 hex digits) masked, `:out-masked`, where masking changes it (39 actions), and
  `:det-masked` where only the masked runs agreed (33); the check compares the masked digest when the
  plain one differs, or alone when only the masked runs agreed (`judge`, shared by the levels).
- *JCS19 (b)*: both sides run in `.tmp/java-check/w/ID`, `java.io.tmpdir` and `TMPDIR`
  `.tmp/java-check/tmp/ID`, `test.classes` `.tmp/java-check/classes/ID` (`run_dirs`); the reference
  recorded again (27 min): no test's status changed, 15 changed their determinism.
- *JCS33 (a)*: the interp level loads file by file by default (`--load files`: each package
  required as the REPL loads it, JC4's source lookup finding a later file's or form's class); `--load
  do` stays the comparison mode. `check-interp.edn` recorded again from the file-by-file run.

**The compiled level** (`--levels compiled`; `make java-check-compiled`; `bin/gate --full`'s
`java-check-full` runs `read,interp,classes,compiled`):

1. the tests whose read passed are put into shards (`java.check/shards`, `--shard-size`, default 700;
   500 for the numbers below): a test goes into the first shard with room where none of its forms
   files' places holds another text (packaged tests share packages: `compiler.c2`'s classes are many
   tests'), and `owners.tsv` records each test's files, top-level classes (read with the reader) and
   their Go names;
2. per shard, `bin/j2g --program` with the shard's forms tree and the shipped build's compiled
   namespaces (`JAVA_CHECK_AOT`, default `target/arbace-go/aot`; goi's table only with
   `JAVA_CHECK_GOTABLE`, as diffcheck's program has none): every public member of every test class is
   a root (`--program`'s REPL world), every `main` among them. j2g's `:analysis-failures` and `:errors`
   (`report.edn`) are attributed to the tests owning the class, file or Go name they name
   (`java.check/refusals`), those tests are `refused` with j2g's reason and the shard is translated
   again without them, until j2g refuses none;
3. `bin/build-go`; when it fails, `go build -gcflags=arbace/lang=-e` in its work module lists every Go
   error of `lang`, each attributed by its file (`J_<path>.go`) or the Go names in it
   (`java.check/goerrors`; an error in jrt's files naming `C_I`, a JDK class a test extends, blames the
   tests extending it), those tests `refused` ("Go does not compile") and the shard translated again;
4. each test's main actions run on the program (`test/java/run.ae --load none`: the classes are the
   program's) in the same directories, judged as interp's actions; the program and its trees removed.
   A shard that fails without a test to blame gives its tests `error` (no class takes it: the report
   fails); `--shard K,...` runs those shards again, the others' results kept.

Measured: the whole level in 41 min of one heavy slot (6 shards, 15 j2g passes of 80-140 s, 9 builds of
70-110 s, the runs 1-2.5 min a shard); a program is about 280 MB.

**j2g's findings of §5.4, fixed** (`arbace/j2g/`; the shipped program's j2g changes, so the
second batched re-seed; the runtime's own translation does not change but where noted):

- *one input class stopped the whole run*: `fromfn/functional?` forced an annotation type's
  annotations, which throw when a meta-annotation names a class outside the world; a class whose
  annotations do not resolve is now not functional (`fromfn.ae`). And a forms file the reader refuses
  (an array type of more than nine dimensions, `String/50`) stopped j2g's read: it is now that file's
  failure, `[file "read: ..."]` among the analysis failures (`world.ae`), its test refused;
- *names* (JCS15 (a)): a class outside the runtime and the JDK in `lang` (`names/user-class?`: of
  package `lang` and not under `arbace/`: a user's Java forms, the check's tests, diffcheck's corpora,
  the fixtures) is named by its binary name between `J_` and `_J` (`t06216.Overloads` is
  `J_t06216_Overloads_J`; `names.ae`), its Go file `J_<path>.go` (`out.ae`), J2G-SPEC §4.4 amended.
  The closing `_J` is needed: with `Jv_` alone (as `--qualify` names, JCS15's words) the first run
  found `p.C`'s static `b(short)` and `p.C$B`'s static `s()` both `Jv_p_C_B_S__String`; the same
  collision existed in the simple names, `C_B_S__String`. No collision of simple names, of
  case-insensitive file names or of a test's file with the runtime's remains in the corpus;
- *Go that does not compile*: a constant negative array index (`a[-1]`, `a[-2] = 5`) and a constant
  zero divisor (`x / 0`, `x % 0`, `y / 0L`, floating point's) go through a function literal's call,
  which is not a constant, so that Java's run-time exception (or infinity) happens at run time
  (`code.ae`: `index-x`, `divisor-x`); a record pattern's bindings and the components no pattern reads
  are read once (`_ = c`), a record without components' `equals` reads no `other` (`decls.ae`),
  `x instanceof T t` of the null literal reads its temporary (gc's "declared and not used").

What is left of §5.4's Go kinds (11 tests, the class `:j2g-go`, JCS42): a local class's captured
values missing from its constructor call when made inside an anonymous or another local class (3), a
captured variable undefined in a lambda or a nested local class (2), an `any` returned as `int32` (1),
`finalize` called on an outer class that does not declare it (1), a member through a null constant
(`OneCaseSwitches`, 1), and a test's subclass of `PrintStream`/`PrintWriter`, which j2g's support code
for `System.out` and the reflection tables take as leaves (3). §5.4's "unnamed-package main" (13) was
the measurement's runner's: JC1's renaming puts every main in a package. j2g's other refusals of the
corpus: a Go name declared twice (a static field `init` and the class's `C_init`, 1; JCS43), a
package-private method redeclared in another package (J2G-SPEC §4.4's rule, 1), the edge's facts (23).

**Numbers** (each level against the JDK's recorded runs; the compiled numbers of shards 0-4 are of
the j2g of `892feb07`, shard 5's (langtools' patterns and the JLS programs) of `38657bff`, which adds
the record and null fixes; interp is the gate executable of main, file by file):

| level | hotspot compiler | java/lang | java/util | java/io | java/net | java/nio | java/text | java/math | java/security | other java/* | langtools | jls | **all** |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| tests (pass on the JDK) | 697 | 354 | 375 | 256 | 154 | 151 | 50 | 38 | 101 | 19 | 515 | 12 | **2,722** |
| read, classes | 696, 697 | 354, 353 | 375 | 256 | 154 | 151 | 50 | 38 | 101 | 19 | 514 | 12 | **2,720, 2,720** |
| interp, file by file (the default) | 531 | 153 | 160 | 83 | 49 | 6 | 38 | 35 | 3 | 0 | 359 | 9 | **1,426 (52.4%)** |
| interp, one `do` (JC1's) | 533 | 153 | 161 | 83 | 49 | 6 | 38 | 35 | 3 | 0 | 360 | 8 of 11 | **1,429 of 2,721** |
| **compiled (j2g)** | **640** | **162** | **207** | **93** | **57** | **7** | **42** | **36** | **8** | **0** | **369** | **11** | **1,632 (60.0%)** |
| compiled: refused | 19 | 102 | 89 | 103 | 66 | 133 | 0 | 0 | 83 | 18 | 113 | 0 | 726 |
| compiled: fail | 21 | 84 | 68 | 59 | 30 | 11 | 7 | 2 | 8 | 1 | 31 | 0 | 322 |
| compiled: differ | 6 | 1 | 4 | 1 | 1 | 0 | 1 | 0 | 1 | 0 | 1 | 1 | 17 |
| compiled: timeout (20 s) | 10 | 5 | 7 | 0 | 0 | 0 | 0 | 0 | 1 | 0 | 0 | 0 | 23 |
| compiled: unread | 1 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 1 | 0 | 2 |

("Other java/*": rmi 18, sql 1.) On the survey's four areas (§5.4: java/lang 151, java/util 204,
HotSpot 705 of its 862, langtools 367) the compiled numbers are 162, 207, 640 of 697 and 369. JC1's
interp was 1,364 of 2,967 with beans; without them and with JC4's fixes it is 1,426.

*File by file against one `do`* (2,721 tests): 1,425 and 1,429; the difference is three tests that
pass only as one `do` (`:interp-files`, JCS40: `MissingBridge`'s bridge, which an interface call through
`TestInterface` needs, is missing when its superclass is an earlier form; `cr6714694`'s and `cr6711117`'s
`Object[][]` field reads as the class `java.lang.Object;` when the file is loaded) and timeouts on the
loaded host (13 HotSpot tests and two others near the 20 s limit flipped between the runs and against
JC1's record: JCS47).

*The compiled level's non-passes by class* (`bin/java-check`'s report, 1,090): the world, by j2g's
refusal of a JDK class the forms, jrt and the edge do not have (`:j2g-world`, 420), and as run-time
throws in the interp level's world classes (serialization 74, other JDK classes and members 40, NIO
35, reflection 34, networking 21, zip and jar 19, `java.lang.invoke` 16, `java.io` 10, threads 9,
charsets 9, class loading 8, security 7, stack walking 5, references 3), the edge's facts 23; the
analysis 230 (j2g's analysis is the interpreter's: `Cannot convert`, `No matching method`, switches,
intersection types); j2g's own 13 (`:j2g-go` 11, names 1, package-private override 1); speed 23; the
JLS corpus's `StringConcat` (an interned constant, 1); a protected member of a world class (2); repr/java's
arrays of more than nine dimensions 2, unread 2; identity hash codes in traces 3; other failures to triage
81 (Calendar's and Date's fields, `ExceptionInInitializerError`s, `Runtime.exec`'s cut overloads,
`NullPointerException`s of reflection over non-public members).

*The two levels against each other* (2,722 tests): **1,400 pass both, 232 only compiled, 26 only
interpreted**, 1,064 neither. Only compiled: the interpreter's limits (extendable superclasses, protected
members, its speed: 112 of them HotSpot's, which j2g runs well under the limit), its analysis' refusals
are the same on both sides. Only interpreted (diffcheck's kind of finding, RPS16 (a), listed in JCS45):
`Runtime.exec`'s overloads (5), `ForkJoinPool.submit` (2) and `Date.toInstant` (1: reached by the
interpreter's reflection, cut in the translation), reflection over non-public members and nested classes
(`Class.cast`'s `newInstance`, `getDeclaredField`'s and `getDeclaredMethod`'s exceptions, `getMethods`'
order, `DefaultAccessibility`, `T8034044`'s static flag, `TwrMultiCatch`'s `newInstance`; 7), `strictfp`'s
methods by reflection (2), `CopyMethods`' class initialization, `TestSyntheticNullChecks`' assertion,
`SamConversion`'s untranslated `test3`, `LambdaTest5`'s goroutine stack overflow, two escape-analysis
tests (an output, a `ClassCastException`), and j2g's refusals of three (Go that does not compile 2, the
name collision 1). These are the survey's §5.4 twenty again, on the widened corpus.

**REAP.** `bin/reap` now judges Java AOT from `test/java/check-compiled.edn` (level `:compiled`, RPS29
(a): the plug-in's names kept), with diffcheck's exclusions (`test/diffcheck/jforms-excluded.edn`) a
source beside it (RPS27 (a), the reader `:jforms-excluded`; its classes in
`test/java/divergences-diffcheck.edn`: F2.4's kin 4, deferred to REAP3; the corpus's own classes 12, by
design); the kind `:j2g` maps to deferred. The matrix's Java row: read 5,440 / 0 / 4, eval 1,426 / 276 /
1,020 (3 pending), **AOT 1,632 / 105 / 985 (12 pending)**; 9 of 12 cells checked, **0 problems**. Pending
(the user's): `:interp-files` (3, JCS40), `:j2g-go` (11, JCS42), `:j2g-names` (1, JCS43).

**The JLS corpus** grows by `test/java/jls/RuntimeConstants.java` (JLS 15.10.4, 15.17.2, 14.30, 8.10.3:
what a compiler must leave to run time, each of j2g's Go fixes above as a case), recorded on the JDK,
passing at the levels read, classes, interp and compiled.

**Not done** (proposed): the compiled level in the essential gate (JCS41, REAP8's); the two levels'
disagreements as data (JCS45).

**Checks**: the essential `bin/gate` at the branch's code head `48b0bfb3` passed in 28m05s (the
executables built afresh, 18 min: j2g changed): the smoke test, Clojure's suite on the test build with
no regressions, the oracle 22,672 of 22,725 as recorded, diffcheck (Clojure's pair 12,020 of 12,041,
the Java forms' pair 743 of 745, as recorded: the fixtures' and corpora's classes now named `J_..._J`),
goi's essential check 801 of 801, Java's essential check 428 of 428 (file by file), the generated
variants, j2g's edge, `bin/reap` 0 problems.

### Proposals (JCS40-JCS49)

- **JCS40 The file-by-file load's findings** (`:interp-files`: `MissingBridge`, `cr6714694`,
  `cr6711117`).
  - Options: (a) with JCS30 (b) in JC5: a file's classes one `defclasses`, one compilation, as the
    one-`do` load that passes them; (b) the interpreter fixed form by form now: the bridge a subclass
    needs added when the subclass is defined after its superclass, and the source lookup's array
    descriptor traced; (c) left in their class.
  - *Recommended: (a)*, (b)'s lookup trace with it: JCS30 (b) is decided and makes the file one unit; a
    hand-written REPL session defining the superclass first then needs (b)'s bridge, so JC5 checks it
    there too.
- **JCS41 The compiled level in the essential gate** (JCS13 (a), RPS15).
  - Options: (a) REAP8: the essential subset's tests that pass compiled (from their committed forms)
    in one program cached by a key of j2g's sources, the runtime and the forms, as diffcheck caches its
    program (seconds when unchanged; a j2g pass and a build, about 4 minutes and a heavy slot, when they
    change), every one must pass; (b) `--full` only (as built); (c) diffcheck's program carries them, its
    Java forms' pair gaining the essential tests as cases.
  - *Recommended: (a)*: the only compiled check of whole Java programs then guards every change to j2g,
    which diffcheck's Java pair (723 cases of Arbace's own) does not cover.
- **JCS42 The Go j2g still writes that gc refuses** (`:j2g-go`, 11 tests).
  - Options: (a) each fixed in j2g in JC5, beside the analysis (the captured values and variables are
    the analysis' facts of local classes and lambdas), and `PrintStream`/`PrintWriter` through their
    interface types in j2g's support forms; (b) the leaf assumption made a refusal (j2g refuses a
    subclass of a JDK class its support code needs as a leaf, naming it), the rest fixed in JC5; (c)
    deferred, decided with JC6.
  - *Recommended: (b)*: a refusal is honest at once and cheap; the capture bugs are real
    miscompilations of ordinary Java (a local class made in an anonymous class) and belong with JC5.
- **JCS43 A class's member names meeting j2g's derived names** (`:j2g-names`: a static field `init`
  and `C_init`; an anonymous class's `GetClass__Class` twice).
  - Options: (a) j2g reserves its suffixes: a static field or method whose Go name would be one j2g
    derives (`_init`, `_Init`, `_class`, `_New`...) gets a mark (`_f`), as locals get `_`; (b) a rename
    table entry per case; (c) left in the class.
  - *Recommended: (a)*: a rule, as J2G-SPEC §4.4's locals have, not a table that grows by test.
- **JCS44 The world as j2g names it.** j2g's refusals name a JDK class by its simple name ("Unable
  to resolve class: DatagramSocket", 420 tests in one class), where the interp level's run-time throws
  name the package (its world classes by area).
  - Options: (a) the analysis' message names the import that would have declared the class
    (`java.net.DatagramSocket`, from the namespace's import list), so that one set of world classes
    serves both levels; (b) the compiled class split by a table of simple names; (c) as built.
  - *Recommended: (a)*: JC6 ranks the world's classes by tests gained, which needs both levels' tests in
    the same classes.
- **JCS45 The two levels' disagreements** (232 only compiled, 26 only interpreted).
  - Options: (a) `bin/java-check` prints them when a run has both levels and records them in
    `test/java/levels-known.edn` (test, both results), failing on a new one, as diffcheck's known file
    does (RPS16 (a): a finding, not a class); (b) printed only; (c) in this survey's prose (as built).
  - *Recommended: (a)*: the 26 are the cases diffcheck's corpus misses, and a new one is a regression
    of one back end against the other.
- **JCS46 The compiled namespaces the level takes.** It uses `target/arbace-go/aot` without looking at
  its key.
  - Options: (a) the runtime's part of `aot.key` (`arbace_runtime_key`) checked against the checkout's,
    a stale tree refused with the build to run (j2g's own sources changing the key is expected: the
    compiled j2g in the tree does not matter to the tests' programs); (b) as built; (c) the check
    compiles the namespaces itself (c2j, as diffcheck does: minutes more a run).
  - *Recommended: (a)*.
- **JCS47 Timeouts on a loaded host.** Fifteen tests near the 20 s limit flipped between runs (the
  interp level, the host's other heavy work).
  - Options: (a) a timeout run again once, with twice the limit, before it counts; (b) the limit per
    level from a quiet run's times plus a margin; (c) as built (JCS22 (a)).
  - *Recommended: (a)*: the record stops moving with the host's load; the speed class keeps the
    tests that are slow.
- **JCS48 Shard size.** One shard of 700 tests (`java/*`, 17,555 classes) was killed once in j2g
  (the host's memory, beside two other agents' heavy jobs); the numbers above are of shards of 500.
  - Options: (a) 500 the default; (b) a shard whose j2g dies split in two and run again; (c) 700.
  - *Recommended: (a) and (b)*.
- **JCS49 The user classes' Go names.** Built as `J_<binary name>_J`, where JCS15 (a) said "as
  `--qualify` does" (`Jv_<binary name>`).
  - Options: (a) as built: `J_..._J`, the closing mark making a class's derived names unable to meet
    another class's (the `Overloads` collision above); (b) `Jv_` as `--qualify`, the collision a
    rename; (c) `--qualify`'s prefixes too closed by `_J`.
  - *Recommended: (a)*, (c) when javac's tool is next rebuilt (its names are the tool's, not the
    shipped program's).
