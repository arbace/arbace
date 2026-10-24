# REAP: read, eval, AOT and print for Clojure, Java and Go — survey and plan

Status: survey and plan, 2026-10-24, at the user's request (doc/JOURNAL.md, 2026-10-24, "the next
milestone's goal, full support for Clojure, Java and Go"). The milestone's working name is the
user's: **REAP**, Read + Eval + AOT + Print (it echoes REPL). Its tag is decided (the user,
journal 2026-10-24): **`arbace-reaps-3-crops-v1`**, the three crops being Clojure, Java and Go, an
annotated tag (and the journal's entry) naming the crops and their versions: Clojure at its
vendored revision (`clojure/clojure` `98d735fab02f`, 1.13.0-master-SNAPSHOT), Java 26 (jdk26u
`baf63fbe`), Go 1.27.1 (TamaGo's go1.27.1). Nothing else here is adopted: no
code under `arbace/`, `go/`, `bin/` or `test/` changed, and the decisions are the user's, as
proposals RPS1-RPS17 (§8). The milestones are REAP1-REAP9 (§7).

Names. `RP1` is taken (the reproducibility amendments of 2026-10-11, doc/go/J2G-SPEC.md,
"Reproducibility"; BOX-NOTES.md, `test/jrt/tables.ae`), so the milestones are **REAP1..REAPn**; **RPS** is
unused and names the proposals. FS (the files' amendments FS1-FS7, JRT-NOTES.md), FL and FU were
also looked at: FS is taken.

Base: branch `full-support-survey`, from main `fcda820e`. The executable that measured is the
essential gate's build of main (`.tmp/arbace-go-gate/amd64/arbace`, 13:03 the same day, its
sources main's but for the journal's last commits), used read-only as the tools' host
(`ARBACE_HOST_EXE`); the Go forms measured are main's conversions of `$GOROOT/test`
(`.tmp/repr-go/forms/linux-test`, `linux-errtest`, of 04:26 the same day, the native reader's),
read in place; gc is TamaGo's go1.27.1 (`/root/tamago-go`), without cgo; every measurement ran
through the host's heavy-command slots, at 6 jobs (the evaluated build's j2g and suite at 8). The scratch is the worktree's `.tmp/fs/`
(Appendix A), kept.

The binding decisions (journal 2026-10-24): the criterion is goi's v1 rule (each language's
conformance corpus at each level against its reference; none fails; every non-pass in a recorded
class with its reason, by design or deferred and named); Java's read is javac on Arbace (JN9b-JN9d
beside, not gating); Go's AOT is Arbace's build of Go forms (repr/go's print and Go's toolchain,
`arbace.build.go`), checked over `$GOROOT/test` against gc as goi's check is; the host
(linux/amd64) alone, forms only (no box subsets, no language text at the REPL, no GPS11); the
tag `arbace-reaps-3-crops-v1`, annotated with the crops' versions. The Java
conformance survey's JC1-JC8 and JCS1-JCS15 (doc/classes/JAVA-CONFORMANCE-SURVEY.md) are binding
for Java's eval and AOT.

Contents: Executive summary · 1 The REAP matrix · 2 Clojure · 3 Java · 4 Go · 5 Measurements ·
6 The gaps by class · 7 Plan (REAP1-REAP9) · 8 Proposals (RPS1-RPS17) · 9 Risks · 10 Sources ·
Appendix A: the scratch.

## Executive summary

- **Where the matrix stands (§1).** Of the twelve cells, **six are at the v1 rule in substance
  today** — Clojure read and print, Go read, eval and print, Java read — with nothing failing; what
  they lack is the record in goi's form (classes as data, by design or deferred, judged by a tool).
  **Two are close**: Clojure eval (Clojure's suite 20,506 of 20,520 assertions on the test build,
  the 14 others and 230 not run all recorded with comments, the oracle 22,658 of 22,711 with 53
  known) and Go AOT (measured here: 1,032 of 1,051 run programs, 388 of 407 judgeable errorcheck
  programs, every non-pass in a class goi already has or a small new one). **Four have real work**:
  Clojure AOT (the suite has never run compiled; diffcheck's pair 12,020 of 12,041 with j2g's F2.4
  limit, 11 cases, open), and Java eval, AOT and print (the conformance sample interpreted 60.3%,
  by j2g 67.3%, the plan JC1-JC8 running; print checked on three corpora but not on the
  conformance corpus).
- **Go's AOT, measured (§5.1, §5.2).** No check ran `$GOROOT/test` through `arbace.build.go`
  before: goi's reference is the printed forms built by gc (Arbace's AOT itself), and repr/go's
  round trip proves the printed source compiles to gc's code symbol by symbol (1,713 test
  programs, 376 std packages), not that the module build runs as gc's run of the original does.
  Built here by `bin/build-go` from main's forms and run against gc's build of the originals: with
  the lines layout **1,032 of 1,051 run programs pass**, 6 fail (all in classes goi already
  records: the go command's logs 2, a `//line` directive, a selector or call split over lines 2,
  `runtime.GOROOT` under `-trimpath`), 13 need gc's flags; with `bin/build-go`'s default gofmt
  layout 1,022 (10 more: `runtime.Caller`'s lines). The errorcheck programs' printed forms compiled
  by gc and judged against the originals' `// ERROR` comments: **388 of 407** (16 fail: spellings,
  lines and imports the forms of an invalid program do not keep, goi's GC49 class and its kin; 3
  the printer refuses), 134 without forms (the invalid programs repr/go cannot convert), 124 with
  gc's later phases' flags (judgeable at this level, since it is gc: RPS17).
- **Clojure's eval and AOT (§2.4, §2.5, §5.3).** The suite's test namespaces are evaluated today on
  a compiled runtime; the evaluated build (`ARBACE_AOT=0`), measured here, gives the same 20,506 of
  20,520 with the same 14 (so the 14 are the evaluator's or the world's, not c2j's). The AOT level
  needs the suite compiled (REAP3), with F2.4's limit fixed (RPS6).
- **Java (§3).** Read is complete (2,121 of 2,121 tests read, 5,500 class files byte for byte;
  diagnostics 1,116 of 1,211, the rest `ct.sym`, doclint, options); eval and AOT are the conformance
  survey's plan; print adds a level to `bin/java-check` (REAP5) and decides the 39 type-argument
  files (RPS9).
- **Plan (§7): REAP1-REAP9 with Java's JC1-JC8, about 24 to 41 working days** (27-46 with JC7):
  REAP1 the record and `bin/reap` (2-3), REAP2 Go AOT as `bin/goi check --aot` (1-2), REAP3
  Clojure's suite compiled and F2.4 (3-5), REAP4 Clojure's deferred (1-3), REAP5 Java's print level
  (1-2), REAP6 goi's deferred (running), REAP7 a user's AOT entry (1-2), REAP8 the essential
  subsets (0.5-1), REAP9 the milestone run (1-2: `bin/gate --full`, the fixpoint, the re-seed, the
  tag); Java's JC1-JC6 and JC8's record 14.5-23, the long pole.
- **Recommended first:** REAP1 now (data and a tool, beside the five agents), REAP2 after
  `goi-after-v1` merges, REAP3 after JFS65 (b); RPS2 (a), RPS4 (a), RPS6 (a), RPS12 (a), RPS17 (a).

## 1. The REAP matrix

What each cell is in Arbace, its corpus, its reference, today's check and number, and the gap to
the v1 rule. "Record" says whether the non-passing cases are already data with a reason per class
(as `test/goi/divergences.edn`) or only scattered (a reference's comments, a known file's reasons
without by design/deferred).

| cell | in Arbace | corpus | reference | check (gate) | today | gap to the v1 rule | record |
|---|---|---|---|---|---|---|---|
| **Clojure read** | `LispReader`; repr/clojure `read` | the suite's `reader`, `edn`; the oracle's reader forms; the frozen `clojure/`, the suite, its libraries | upstream's suite; the frozen JVM; the original text | `suite-go`, `oracle-go` (essential); `bin/repr-clojure check --suite --roundtrip` (`--full`) | 7,796 of 7,796; 602 of 602; round trips 0 differences | none failing; the classes as data (the one-way rules) | prose (REPR-CLOJURE.md), known file empty |
| **Clojure eval** | the evaluator (`Compiler$Evaluator`) | Clojure's suite (85 ns, 20,750 assertions, 27 specs); the oracle (22,711) | upstream's run; the frozen JVM | `suite-go`, `oracle-go`, diffcheck (essential) | 20,506 of 20,520 run (11 fail, 3 errors), 230 not run; generative 27 of 27; oracle 22,658, 53 known | the 14 and the skipped as classes; 6 deferred named (REAP4) | the reference's comments; the oracle's reasons |
| **Clojure AOT** | c2j → j2g → `arbace.build.go` | the runtime's namespaces (every build); diffcheck's corpora; **the suite: not run** | upstream's run; the evaluated half (diffcheck) | diffcheck (essential), the suite and oracle over compiled core | diffcheck 12,020 of 12,041, 21 known (11 of them F2.4's limit) | the suite compiled (REAP3), F2.4 (RPS6), classes | diffcheck's known with reasons |
| **Clojure print** | `pr`, `print-method`, pprint; repr/clojure `print` | the suite's `printer`, `pprint`; the oracle's printing, format; repr/clojure's corpora | upstream's run; the frozen JVM; the original text | `suite-go`, `oracle-go`; `bin/repr-clojure check --roundtrip` | 548 of 548; 722 of 722; 0 differences | the record only | none needed beyond REAP1 |
| **Java read** | javac on Arbace + repr/java | jdk26u's jtreg tests (JCS2), repr/java's corpora, jdk26u's 68 modules, langtools' golden diagnostics | the JDK's javac (class files, diagnostics); javac on Arbace's trees (the converter's checks) | `javac-vendor classes`, `repr-java check --suite`, `--jdk`, `parse-check`, `front-check`, `attr-check` (`--full`); JC3 | sample 2,121 of 2,121, 5,500 class files equal; diagnostics 1,116 of 1,211 | JC3's levels; >9-dimension arrays (JCS12 (b)); classes (`ct.sym`, doclint, options) | prose (the conformance survey §5.5) |
| **Java eval** | `arbace.classes.interp` | the jtreg corpora (JC1), the JLS corpus | the JDK's run (JCS3, JCS5) | diffcheck's Java pair, oracle `defclass`; JC1's `bin/java-check` (running) | sample 1,279 of 2,121 (60.3%); diffcheck 723 of 725 | JC1, JC4, JC5, JC6, JC8 (842 non-passing: world 505, bugs 73, analysis 79, ...) | JC1's `test/java/divergences.edn` (to come) |
| **Java AOT** | j2g → `arbace.build.go` | as eval | as eval | diffcheck's Java pair, `bin/j2g-check`, the test build; JC2 | sample 1,427 of 2,121 (67.3%) | JC2, JC5, JC6 (refused 104, failing 145, ...) | to come with JC2 |
| **Java print** | repr/java `print` (lines; `:full`) | baseline 183, samples 14, `jdk/forms` 838 Java files; **not the jtreg corpus** | the original Java (text), the forms read back, javac's class files | `bin/repr-java roundtrip --corpus baseline,samples,jdk --text` (`--full`) | `:full` 1,035 of 1,035 byte for byte; anew: forms 1,080 of 1,117, javac rejects 39 jdk files, class files 628 of 1,035 (the rest by design) | the conformance corpus (REAP5); the 39 decided (RPS9) | `*-known.edn` with reasons |
| **Go read** | repr/go's native reader; gc's syntax errors | 22 corpora (std, `$GOROOT/test`, tamago, cmd, ...) | the helper (go/parser, go/types) | `bin/repr-go native-check --fresh` (`--full`) | 12,158 of 12,158 equal; 117 raw only, 73 rejected (by design) | the record only | `native-known.edn` (empty), raw-only in prose |
| **Go eval** | goi | `$GOROOT/test` (1,837) | gc | `bin/goi check` (`--essential`; `--full`), `divergences` | 1,610 pass, 0 fail, 227 in 17 classes | GC157 (6), `go:embed` (2): running | `test/goi/divergences.edn` |
| **Go AOT** | `arbace.build.go` (`bin/build-go`, `go/run-compiled`) | `$GOROOT/test`'s 1,051 run programs, 680 errorcheck, 127 run directory tests | gc on the originals | the round trips' code level (`--full`); **none of runs** | measured: run 1,032 of 1,051 (lines layout), errorcheck 388 of 407 judged | the check (REAP2), the classes, directory tests (RPS11), gc's flags (RPS17) | none (§5's scratch) |
| **Go print** | repr/go's printer | std 376, test programs 1,713, directory tests 254; `:full` over the same | gc's trees, export data, code; the original text | `bin/repr-go roundtrip`, `--text`, `--dirs` (`--full`) | every level 0 known; text 3 known; tests 9 known | the record only | `roundtrip-*-known.edn` with reasons |

## 2. Clojure

### 2.1 What the four levels are

Clojure's text is Arbace's text but for the dialect's renames (doc/VENDOR-NOTES.md), and Arbace
reads, evaluates and prints Clojure itself. So each level has a language half and a bridge half:

| level | the language's own | the bridge (repr/clojure) |
|---|---|---|
| read | Arbace's reader, `arbace.lang.LispReader` (and `arbace.edn`, `arbace.core/read`): Clojure's text into data | `bin/repr-clojure read`: a Clojure library's `.clj`/`.cljc` text into Arbace's `.ae` text, the rules of `arbace.repr.clojure.rules` |
| eval | the evaluator (`Compiler$Evaluator`, `arbace/lang/Compiler.ae`: each node's `evalIn`, F7's generated `CompilerNodes.ae`): code loaded from source at run time | — |
| AOT | c2j (`arbace.c2j.*`): a namespace's analyzed units compiled to Java forms, j2g to Go forms, repr/go's print, gc (`ARBACE_AOT=1`, the default since S6): the runtime's own namespaces in every build; any other namespace only in diffcheck's program | — |
| print | the printer (`pr`, `print-method`, `print-dup`, `arbace.pprint`, `cl-format`) | `bin/repr-clojure print`: `.ae` back to Clojure's text, each rule reversed |

Recommended (RPS2): both halves at read and print, the language's half being what Clojure's suite
and the oracle exercise and the bridge's what its round trip proves; neither alone is "Clojure
read" for a user, who reads Clojure files (the bridge) into forms (the reader).

### 2.2 The corpora and the references

- **Clojure's suite** at the vendored revision (`clojure/clojure` `test/` at
  `98d735fab02f337cee654cb0629bddc09883a75a`, Clojure 1.13.0-master-SNAPSHOT), 85 test namespaces,
  809 tests, 20,750 assertions and 27 test.generative specs, all passing upstream on the JDK
  (`test/baseline-results.edn`'s control run): the language's own conformance corpus, as
  `$GOROOT/test` is Go's. Its reference is upstream's run (every assertion passes), so a non-pass is
  any assertion that does not pass or does not run.
- **The oracle** (`bin/oracle`, doc/go/ORACLE.md): 22,711 cases (hand-written forms by topic, the
  suite's assertions harvested as self-contained forms, class scripts, regex), the frozen JVM
  Arbace's results (`arbace-for-java-26-v4`) recorded in `test/oracle/expected`.
- **diffcheck's Clojure pair** (`bin/diffcheck`, F2): the oracle's forms corpora evaluated against
  the same namespaces compiled by c2j (12,041 cases); no external reference, the two halves against
  each other.
- **repr/clojure's corpora**: the frozen `clojure/` (the vendored tree, from the tag), the suite
  (103 files) and its six test libraries (33 files); reference: the original text, byte for byte.

### 2.3 Read

- Arbace's reader: the suite's `reader` namespace 39 tests, 7,796 assertions, all pass; `edn` and
  `generators`' specs pass; the oracle's `forms/reader.ae` 466 of 466 and `harvest/reader.ae` 136
  of 136 (no known mismatch is the reader's); test.generative's reader specs (read of `pr-str`'s
  output) pass.
- repr/clojure (`bin/repr-clojure check --suite`, `--roundtrip`, `bin/gate --full`): the suite's
  translation against its digest (`test/repr/clojure/suite.sha256`), and Clojure → Arbace →
  Clojure byte for byte over the frozen tree, the suite and the libraries in each mode
  (`--keep-ext`, `.ae`, `--full`): **0 differences** (`test/repr/clojure/roundtrip-known.edn`, every
  corpus `{}`). One-way by design, recorded in REPR-CLOJURE.md ("The one-way rules"): a `.cljc`
  read without `--full` comes back as its `:clj` reading; `SecurityManager`'s spelling.
- Gap: none failing. Missing for the v1 rule: the classes as data (the one-way rules are prose and
  the check's tolerance; RPS3), and the corpus statement (which trees are the read level's corpus).

### 2.4 Eval

- **Clojure's suite on the test build** (`bin/clojure-tests` with `CLOJURE_TESTS_GO`, the essential
  gate's `suite-go`): every test namespace loaded from source and **evaluated**, on a runtime whose
  namespaces are compiled (`ARBACE_AOT=1`). 76 namespaces run (all load), 777 tests, 20,520
  assertions: **20,506 pass, 11 fail, 3 errors**; test.generative 27 of 27; 9 namespaces and 6
  tests skipped (`test/arbace-go-results.edn`'s `:skipped`, `:skipped-tests`), 230 of upstream's
  assertions not run (namespaces 190: `clojure-xml` 1, `genclass` 54, `java.javadoc` 5,
  `metadata` 53, `serialization` 77; tests 40: `compilation`'s `clj-1208`, `java-interop`'s four,
  `reflect`'s `invoke-checks-accessibility`, `transducers`' `seq-and-transducer`).
- The 14 not passing (all in the reference, each with a comment):
  - `compilation`: `primitive-return-decl` 2, `incorrect-primitive-type-hint-throws` 1 — the
    evaluator evaluates primitive fns boxed (EVAL-PLAN.md Q1: no recur-mismatch warning, no "Cannot
    coerce long to int"): **by design of the evaluator**; `test-compiler-line-numbers` 7 — the
    evaluator's stack traces give a field access, a field assignment and an invoke's `IFn` cast the
    enclosing call's line: **deferred** (a fix in the evaluator's frames, or by design);
  - `java-interop`: `test-invoke-fiprim-rets` 1 fail 1 error, `test-match-prim-args-only-to-2` 1
    error — a fn adapted to an interface `definterface` makes at run time (run-time interfaces
    carry no annotation; jrt adapts fns to the world's interfaces only): **deferred**;
    `test-proxy-super` 1 error — a proxy of `java.util.BitSet`, not one of the Go build's proxy
    superclasses: **deferred** (the proxy superclasses' list, J2G-SPEC).
- **The skipped**, by design (decided): D6's namespaces (`arbace.xml`/SAX, `inspector`/Swing,
  `javadoc`, `repl.deps`), gen-class, Java serialization (the user, 2026-10-10), the AOT class file
  test (`clj-1208`: no class files), `java.awt` (D6), ASM reading a proxy's class file; deferred:
  `javax.xml.stream` (`reflect`), and `seq-and-transducer`, 200,000 test.check trials beyond the
  900 s a namespace may take (574 s alone after step 7, EC7 not taken: speed).
- **The oracle** (`bin/oracle check`, essential gate): **22,658 of 22,711** (55 by V11), **53
  known** in 9 groups (`test/oracle/known-go-amd64-frozen.edn`): locales (root and English only,
  the user's decision) 42, the security providers 1, serialization 1, the evaluator's fns without
  primitive interfaces (Q1) 3, `SecurityManager` 1, a message's wording 1, main's divergences from
  v4 2, the StringBuilder's identity hash 1 (not reproducible), **the boxed NaN's identity** 1 (the
  file says the JVM's `false` is right: a bug of Arbace's, kept known: deferred, RPS5).
- **The evaluated build** (`ARBACE_AOT=0`, the runtime's namespaces evaluated too): not run since
  S6 made the compiled build the default (SA151's condition then: the suite and the oracle as
  recorded). Measured here (§5.3): the same 20,506 of 20,520, the same 14 not passing, test.generative 27 of 27.
- Gap: every non-pass is recorded with a comment, but not as classes (id, by design or deferred,
  where decided) and not judged by a tool that fails on an unclassed one (RPS3, REAP1); the
  deferred ones named: line numbers (7), FIPrims (2), BitSet's proxy (1), NaN's identity (1),
  `javax.xml.stream` (1 test), `seq-and-transducer` (1 test).

### 2.5 AOT

- **What runs compiled today.** Every namespace of the runtime (`arbace.core` and the 100-odd
  embedded) in every build since S6: Clojure's suite and the oracle run on compiled `arbace.core`,
  so both judge c2j over the runtime. A user's namespace is never compiled: `compile` writes no
  class files (D13), and nothing builds a program from a user's namespaces but diffcheck's own
  script (`bin/diffcheck` steps 3-4: prepare under `ARBACE_COMPILE`, `arbace.c2j.main/run`, j2g
  `--program`, `bin/build-go`; the compiled namespaces loaded by `Compiler$AOT/load`).
- **diffcheck's Clojure pair** (essential gate): **12,020 of 12,041** agree, 21 known
  (`test/diffcheck/known.edn`): compiled code interns the vars it names at load (4, as the JVM's
  AOT: by design), an identity hash (1, not reproducible), `Compiler/LINE` in a compiled loader (1,
  as the JVM's `__init`: by design), symbols shared in a compiled constant pool (1, F3.3 (a)
  decided), compiled fns implementing `IFn$LL`/`IFn$LOL` where the evaluator's do not (3: the
  compiled side is the JVM's answer; the evaluator's Q1), and **j2g's limit left by F2.4** (11: a
  compiled deftype or fn whose Clojure code names the namespace's vars, or holds a `deftype*` in a
  fn, is left out of j2g's analysis and the code naming it fails where it runs: deferred, a real
  gap of the AOT level).
- **Clojure's suite compiled**: never run. It is the AOT level's corpus as it is the eval level's
  (RPS4). What it takes (REAP3): the suite's namespaces and test libraries prepared and compiled
  with the runtime's (diffcheck's steps), the program built with the suite's Java fixtures (the
  test build's), and the runner loading each test namespace through its compiled loader (an
  embedded source's path in `RT.load`, or `Compiler$AOT/load` as diffcheck's driver does). The
  suite's own harvest in diffcheck (`forms/harvest/*`, 3,189 forms of the suite's assertions,
  compiled) agrees but for one case (`harvest/vars.ae:5`, the position above), so the expected
  differences are the namespaces' load-time semantics and F2.4's limit, not the assertions.
  Expected to pass where the evaluator does not: Q1's three `compilation` assertions (compiled fns
  are primitive, diffcheck's three).
- Gap: the corpus run (REAP3), F2.4's limit (fix or defer, RPS6), the classes (REAP1).

### 2.6 Print

- The printer: the suite's `printer` 74 of 74, `pprint` 474 of 474 (proxies of `BufferedWriter`),
  the oracle's `printing.ae` 396 of 396, `format.ae` 326 of 326 (no known mismatch is the
  printer's: the StringBuilder's hash is the hash's).
- repr/clojure print: §2.3's round trips, 0 differences; `print --in-place` of the translated suite
  equal to Clojure's tree (`check --suite`).
- Gap: the record only (REAP1).

## 3. Java

### 3.1 What the four levels are

| level | in Arbace | check today |
|---|---|---|
| read | javac on Arbace (javac's Java forms run by Arbace, `target/arbace-go/amd64/arbace-javac`) with repr/java's converter on it: Java text into Java forms (`bin/repr-java read`); the class files javac on Arbace writes as the front end's own output | `bin/repr-java check --suite`, `check --jdk` (68 modules, forms as hashes), `bin/javac-vendor classes`, `parse-check`, `front-check`, `attr-check` (all `--full`) |
| eval | the Java forms' interpreter, `arbace.classes.interp` with `Compiler$CF`/`CFGo` (a `defclass` at the REPL) | diffcheck's Java pair; the oracle's `defclass`; JC1's `bin/java-check --levels interp` (being built) |
| AOT | j2g: Java forms to Go forms in a closed world, `bin/j2g --program`, `bin/build-go` | diffcheck's Java pair; `bin/j2g-check`; the suite's Java fixtures (the test build); JC2 |
| print | `bin/repr-java print` (`arbace.repr.java.print`): Java forms back to Java, the lines layout or mode `:full`'s record | `bin/repr-java roundtrip --corpus baseline,samples,jdk --text` (`--full`) |

### 3.2 The corpus and the reference

jdk26u's jtreg tests at the platform's pin (JCS2 (b)), first the run tests of `jdk/java/*` (but
AWT and Swing), `langtools/tools/javac` and `hotspot/jtreg/compiler` (about 4,400 with the
`@compile` and JIT-option ones; the survey's sample 2,428, 2,121 passing on the JDK), and a small
hand-curated JLS corpus; the reference the JDK's run of the same renamed sources, recorded as data
(JCS3-JCS5), stdout and status. For read also the corpora repr/java already has (the baseline,
the samples, `jdk/forms`, javac's sources, jdk26u's 68 modules) and langtools' 1,211
golden-diagnostics tests.

### 3.3 Read

- The conformance survey's sample (§5.2, §5.5 there): **2,121 of 2,121** tests read (2,304 files),
  their 5,500 class files equal to the JDK javac's byte for byte; golden diagnostics 1,116 of
  1,211 (the JDK's own javac 1,157).
- Non-passing, by class: `--release N` (no `ct.sym` on Arbace: 31 of the 36 where only the JDK
  matches, and most of the 28 where both differ differently), doclint (4: jdk.javadoc's doclint is
  not in javac's world), `-encoding ascii` and a jar on `--module-path` (unsupported on Arbace), an
  annotation processor's message, a bad header's path, a lint option; the 23 + 3 where the golden
  files are not what either javac gives (the reference's, not Arbace's: out of the corpus by JCS3's
  rule); arrays of more than nine dimensions (2 tests: the forms do not read back, JCS12 (b), a
  fix).
- Gap: the levels `classes`, `read`, `diagnostics` in `bin/java-check` (JC3), the classes as data;
  JCS12 (b)'s fix (part of JC5).

### 3.4 Eval

The conformance survey's §5.3, §6.1-§6.2: **1,279 of 2,121** (60.3%, one `do` per test); 842 not
passing: the world 505 (reflection's annotations and generic types 70, extendable superclasses 67,
missing members 55, zip and jar 52, serialization 46, other classes 42, `java.lang.invoke` 35,
javac's API 34, management 31, logging 25, references 15, classfile 15, `StackWalker` 9, protected
members 5, locales 4), the interpreter's bugs 73 (JC4, running), the analysis 79 (JC5), the REPL's
model 23 (JC4's source lookup, JCS8 (b)), timeouts 83 (HotSpot's JIT warm-up loops: speed), the
harness 41, repr/java's arrays 2, others 33. Gap: JC1 (the check, running), JC4 (running), JC5,
JC6 (the world decided, the corpus widened), then the classes as v1 (JC8's record).

### 3.5 AOT

The conformance survey's §5.4: **1,427 of 2,121** (67.3%; 1,610 tests in one program, 88.6% of
them pass); refused by j2g 104 (the analysis 77, Go that does not compile 23, the edge and names
4), 308 refused for a JDK class outside the world, 99 left out by the harness, 145 failing (the
world's members 83, reflection over non-public members 13, jrt's `Calendar` and friends 11, the
unnamed-package roots 13, ...), 19 differing, 19 timeouts; 20 pass only interpreted, 232 only
compiled. Gap: JC2 (the compiled level and j2g's findings: a class's failure its own, JCS15's
names, the eight kinds of Go that does not compile, the unnamed-package roots), then the world (JC6).

### 3.6 Print

- `bin/repr-java roundtrip` (PRINTER-NOTES.md), three corpora: mode `:full`'s text level **1,035
  of 1,035** Java files byte for byte, their class files the originals', no known difference;
  the lines layout (forms printed anew): forms equal baseline 189 of 189, samples 17 of 17, jdk 874
  of 911; class files equal 143 of 183, 12 of 14, 473 of 838 (the rest by design: the forms do not
  record `?:` against `if`, a switch expression against a statement, a local's default
  initializer, fall-through, an increment's place, code order; 40 + 2 + 365 recorded); javac
  rejects 39 of jdk's printed files (Java's type arguments in code are not in the forms, SPEC §1
  principle 4, and the printer's casts do not recover them: 30 forms missing, 7 package files);
  `java/util/Random.ae`'s literal overload (1); radixes at the `ae` level (7 + 95). All in
  `test/repr/java/roundtrip/*-known.edn` with reasons.
- Not covered: the conformance corpus. Nothing prints jtreg's tests' forms back; JC's levels do
  not include print.
- Gap: the level `print` in `bin/java-check` (forms → Java → forms equal, javac accepts the
  printed Java, and its class files against the original's, by JCS3's classes; REAP5); the 39
  type-argument files decided (RPS9: deferred, the printer inferring type arguments, or the forms
  carrying them through JFS75 (b)'s model).

## 4. Go

### 4.1 What the four levels are

| level | in Arbace | check today |
|---|---|---|
| read | repr/go's native reader (`bin/repr-go read`, GP5: go/parser and go/ast run as compiled Go forms in goi's table, the loader, goi's checker in its resolve mode for the annotations), gc's syntax errors (`syntax-errors`, GP6) | `bin/repr-go native-check --fresh` (22 corpora against the helper, the oracle), the round trips' tree level |
| eval | goi (`arbace.go.*`, `go/arbace/goi`): Go forms interpreted, checked by goi's checker | `bin/goi check` (`--essential` in the essential gate), `bin/goi divergences` |
| AOT | `arbace.build.go` (`bin/build-go`, `go/run-compiled` at the REPL): repr/go's print into a module, gc builds it | `bin/repr-go roundtrip` (tree, export, code), `--dirs` (and run), goi's references; no run of `$GOROOT/test` through `arbace.build.go` against gc on the originals before this survey (§5.1) |
| print | repr/go's printer (`arbace.repr.go.print`), the lines and gofmt layouts and mode `:full` | `bin/repr-go roundtrip amd64`, `linux/amd64`, `--text amd64`, `--dirs` (`--full`) |

### 4.2 The corpus and the reference

`$GOROOT/test` of TamaGo's go1.27.1 for linux/amd64 (as goi's check: 1,051 single-file run programs
and 127 directory tests with a run action, 1,178 in all, and 659 errorcheck programs in goi's corpus
`linux-errtest`), and
for read and print also std (`linux-amd64`, 376 packages) and the round trips' 1,713 test
programs; the reference gc: the originals compiled and run as Go's test driver does, their
`// ERROR` comments judged by its judge (`gocmp errorcheck`), and for read the helper
(`tools/godump`: go/parser and go/types on the host's toolchain).

### 4.3 Read

- `native-check` (GP5, GPS49 (c); `--full`): over 22 corpora, 12,158 dumps resolved, E-tree,
  E-ann and E-forms (`.ae` text of both readers byte for byte, `:lines` and `:full`) all equal,
  `test/repr/go/native-known.edn` empty; 117 raw only (GC47's 64 invalid programs the converter
  cannot convert, 53 packages of TamaGo's goos module for other boards that do not type-check: by
  design, they have no final forms), 73 rejected by go/parser (judged on gc's syntax errors, GP6).
- Gap: none failing; the classes are a known file (empty) and the raw-only list in prose: one
  record with the others (REAP1).

### 4.4 Eval

`bin/goi check` (`test/goi/check-linux-amd64.edn`): **1,610 of 1,837 pass, 0 fail, 0 error**, 69
known, 140 unsupported, 18 skipped, each in one of the 17 classes of `test/goi/divergences.edn`
(`bin/goi divergences`); essential 801 of 801. Deferred and being built now (the agent
`goi-after-v1`): GC157 (`byte`, `rune` and aliases in messages, 6) and `go:embed` (GS14 (a),
GC161 (b): 2). Gap: those two, merged; nothing else.

### 4.5 AOT

- **The round trips prove the compiled code, not its runs.** `bin/repr-go roundtrip linux/amd64`
  compares gc's output for the original and the printed source symbol by symbol (tree, export
  data, code: instructions, relocations, data, type descriptors, DWARF): **376 of 376 std
  packages and 1,713 of 1,713 test programs** (run, build, compile, runoutput actions) pass every
  level, none known; `--dirs` 254 directory tests at tree, export and code, 127 of them at `run`
  (their output against the golden `.out`), the rest compile-only; the same for tamago and arm64.
  Identical code is a stronger judge than a run, but it is gocmp's layout (`go tool compile` per
  package, positions remapped), not `arbace.build.go`'s module build.
- **goi's references are an AOT run.** `bin/goi check`'s reference for a run program is the forms
  printed by repr/go and built by gc (GOPATH mode, not `arbace.build.go`'s module and
  `-trimpath`), so goi is judged against Arbace's AOT, and the AOT itself against gc on the
  originals only through the round trip's code level.
- **Measured here (§5.1, §5.2)**: every run program of `$GOROOT/test` built from its forms by
  `bin/build-go` (`arbace.build.go`) and run against gc's build and run of the original:
  **1,032 of 1,051 pass** with the lines layout (1,022 with `bin/build-go`'s default gofmt layout), 6 fail, all in classes goi already has, 13 need gc's flags; and every errorcheck program printed from its forms and compiled by gc, its errors
  judged against the original's `// ERROR` comments: **388 of 407** judgeable programs pass (16 fail: spellings, lines, imports and directives the `:lines` forms of invalid programs do not keep; 3 the printer refuses), 134 have no forms, 124 use gc's later phases' flags.
- `go/run-compiled` (G15) is the REPL's AOT: the package and the interpreted packages it imports
  written as a forms root and built by `arbace.build.go` (`test/goi/repl.ae` checks it).
- Gap: the check as a tool with its reference and classes (`bin/goi check --aot`, REAP2), the
  directory tests through `arbace.build.go` (their `./a` imports are not module paths: REAP2
  rewrites them under the module, or the level is the round trip's `run`, RPS11), and the classes
  of §5.1's non-passing programs.

### 4.6 Print

- `bin/repr-go roundtrip` (lines layout): §4.5's numbers, 0 known; `--text amd64` (mode `:full`,
  the original text byte for byte): all but **3 known** (`test/repr/go/roundtrip-text-known.edn`:
  an empty `import ( )` or `var ( )` group has no form to hold it, by design); `--tests` 9 known at
  the code level (TamaGo's tests embed their package's own sources: the embedded text differs, by
  design).
- Gap: the record only (REAP1).

## 5. Measurements

### 5.1 Go AOT: `$GOROOT/test`'s run programs through `arbace.build.go`

Method (`.tmp/fs/scripts/aot_one.sh`, `aot_all.sh`). For each package file of main's `linux-test`
conversion whose action is `run` (the 1,291 main packages of the corpus, 1,051 of them run programs:
goi's single-file ones): the action read as `bin/goi`'s `action_of` reads it (a `-t`
taken, an action with gc's flags skipped); **gc's reference**: the original's files (the package's
`:files`) built by `go build` in GOPATH mode in the test's directory (goi's `gc_env`), run in
`$GOROOT/test` with the action's arguments (a 30 s limit), stdin an empty file, stdout, stderr up
to the first `goroutine ` line (signal addresses normalized) and the status recorded, and the
output against the golden `.out` where there is one; **Arbace's**: the package file and its
directory copied into a forms root `src/go/test/...` and built by `bin/build-go -o EXE --work W
src` (`arbace.build.go` on the gate's executable: the module printed, `go build -trimpath`), run
the same way; pass when status, stdout and stderr's head are equal.

| result | gofmt layout (`bin/build-go`'s default) | lines layout (`--layout lines`) |
|---|---:|---:|
| **pass** | 1,022 | **1,032** |
| fail | 16 | 6 |
| skip: the action needs gc's flags (`-gcflags=-d=checkptr` 6, `-l=4` 2, `-d=softfloat`, `-d=ssa/check/on`, `-d=converthash`, `-d=maymorestack`, `all=-N -l`) | 13 | 13 |
| **run programs** | 1,051 | 1,051 |
| not run (other actions of the corpus: compile 192, build 24, runoutput 22, buildrun 1, one file gone) | 240 | 240 |
| wall time (6 jobs; a program's print and build 1.56 s on average) | 302 s | 328 s |

Of the 85 programs with a golden `.out`, gc's run of the original matches it in 84 (the other,
`fixedbugs/issue21808.go`, differs on this host and passes against gc's run). Every build
succeeded. The 6 failing with the lines layout, each in a class `test/goi/divergences.edn` already
has for goi:

| program | why | class |
|---|---|---|
| `devirtualization_nil_panics.go`, `fixedbugs/issue29504.go` | a selector or call split over lines (`v.` then `A()` on the next): mode `:lines` keeps a statement's line, not its expressions' line breaks, so a panic's line is the statement's | positions (goi's "runtime.Caller ... traces") |
| `fixedbugs/issue22662.go` | `//line` directives: the printed program has none | line directives |
| `winbatch.go` | `runtime.GOROOT()` is empty under `-trimpath` (`arbace.build.go`'s) | the toolchain's root |
| `fixedbugs/issue21317.go`, `linkobj.go` | they run the go command and log its times: gc's own run fails the same way (status 1 both), the timestamps differ | runs of the go command (nondeterministic) |

The gofmt layout fails 10 more, all positions (`runtime.Caller`'s lines: `bug347`, `bug348`,
`issue14646`, `issue22083`, `issue27201`, `issue34123`, `issue4562`, `issue5856`, `issue79762`,
`inline_literal`): `bin/build-go` prints gofmt's layout by default, readable Go whose lines are not
the forms'. The check should build with the lines layout (or `--line-file`), as the round trip's
`run` level does (RPS17).

### 5.2 Go AOT: the errorcheck programs, gc's errors on the printed forms

Method (`.tmp/fs/scripts/aot_err.sh`, `aot_err_all.sh`). The 680 programs of `$GOROOT/test` whose
action is `errorcheck` or `errorcheckwithauto` (goi's corpus `linux-errtest` holds 659 of them,
those go/build matches for linux/amd64): the flags as `bin/goi`'s `check_err` reads them (gc's later
phases' flags and `-goexperiment` set apart, as goi's unsupported; line directives apart);
**gc's reference** `go tool compile -p=p -d=panic -C -e -importcfg CFG -d=ssa/check/on` of the
original, judged by `gocmp errorcheck` (a program gc's own errors do not pass is skipped, as
goi's); **Arbace's**: the program's forms (`linux-errtest`, mode `:lines`) printed by `bin/repr-go
print` into a directory, the printed files compiled the same way there, judged against the
original's `// ERROR` comments (the printed lines are the forms' lines, the original's).

| result | programs |
|---|---:|
| **pass** (gc's errors on the printed forms match the original's `// ERROR` comments) | **388** |
| fail | 16 |
| the printer refuses the forms (`ddd1`, `fixedbugs/bug217`, `makenew`: forms of an invalid program) | 3 |
| no forms (the invalid programs repr/go cannot convert: go/parser rejects them, or the converter, GC47) | 134 |
| gc's later phases' flags (`-m` 63, `-d=` 26, `-l` 10, `-+` 6, `-live` 5, `-race` 5, `-N` 3, `-std` 3, `-goexperiment` 2, `-complete` 1), set apart as goi does | 124 |
| line directives | 7 |
| skip: gc's own errors do not pass the judge | 8 |
| **all** | 680 (122 s, 6 jobs) |

The 16 failing: an unused import the forms of an invalid program drop (`import1`,
`fixedbugs/issue11361`, `issue13539`, `issue20298`, `issue6428`: 5); misplaced directives, which
the forms do not hold (`directive`, `directive2`); a non-canonical import path printed canonical
(`issue11362`); `import init` (`issue4517d`); spellings mode `:lines` does not keep (literals:
`issue19947`, `issue56220`, `issue31747` (the language version's literal syntax); parentheses:
`issue11614`); lines (`funcdup2`, `goto`); and `return` (a final expression statement written as
its return). goi's class "message spellings, columns and lines the forms do not keep" (GC49) holds
the last seven for goi too; the imports, directives and paths are the printer's or the converter's
of invalid programs. Mode `:full`'s forms keep comments, spellings and lines (SPEC §3.1): printed
from them most of these would pass (not measured; RPS17).

### 5.3 Clojure's suite on the evaluated build (`ARBACE_AOT=0`)

Method (`.tmp/fs/scripts/eval_suite.sh`): `ARBACE_AOT=0 bin/arbace-go --build-tests` in the
worktree (amd64, the gate's executable as the tools' host), then `bin/clojure-tests -j 8` with
`CLOJURE_TESTS_GO` that executable, against `test/arbace-go-results.edn`.

| | the test build (`ARBACE_AOT=1`, the reference) | the evaluated test build (`ARBACE_AOT=0`) |
|---|---:|---:|
| namespaces run / loading | 76 / 76 | 76 / 76 |
| tests | 777 | 777 |
| assertions | 20,520 | 20,520 |
| pass | 20,506 | **20,506** |
| fail, error | 11, 3 | 11, 3 (the same tests) |
| test.generative | 27 of 27 | 27 of 27 |
| the executable | 153.2 MB (with the fixtures) | 93.9 MB |

The build took 508 s (j2g 421 s at 8 jobs), the suite 290 s at 8 jobs: "no regressions against
`test/arbace-go-results.edn`". So the evaluator alone (`arbace.core` evaluated from the image)
passes what the compiled runtime passes, and the 14 are not c2j's. (`bin/clojure-tests` fetched
test.generative 1.1.1 into the worktree's `.tmp/`, as it does on a fresh tree.)

### 5.4 What was not measured

- Clojure's suite compiled (§2.5): it needs REAP3's harness; its expected shape is diffcheck's.
- Java: the conformance survey's numbers stand (its scratch is gone, the journal's correction of
  2026-10-24; JC1 rebuilds them as `bin/java-check`); the print level over the jtreg sample
  (REAP5's first measurement).

## 6. The gaps by class

Per cell, the non-passing cases by class, their size, and what closes each: **fix** (code), **design**
(recorded by design, already decided or to decide), **defer** (recorded deferred and named), or
**tool** (a check or a record to build). Sizes are today's (or §5's).

| cell | class | size | closes it |
|---|---|---:|---|
| all | the classes as data, judged by a tool (`bin/reap`, per-check verbs) | 9 cells without | tool: REAP1 (RPS3, RPS13) |
| Clojure read | `.cljc` read without `--full` comes back as its `:clj` reading; `SecurityManager`'s spelling | rules, 0 files today | design (REPR-CLOJURE.md, decided) |
| Clojure eval | D6's namespaces, gen-class, serialization, `java.awt`, class files (AOT test), ASM | 9 namespaces, 5 tests, 228 assertions | design (decided 2026-10-10, D6) |
| Clojure eval | primitive fns boxed in the evaluator (Q1): `compilation` 3 assertions, oracle 3 | 6 | design (EVAL-PLAN Q1) |
| Clojure eval | stack trace lines of field accesses, assignments, `IFn` casts | 7 assertions | defer, or fix (RPS5) |
| Clojure eval | FIPrims through run-time interfaces | 2 tests (1 fail, 2 errors) | defer (RPS5) |
| Clojure eval | `BitSet` not a proxy superclass | 1 error | fix (REAP4) |
| Clojure eval | the boxed NaN's identity (`Util.equiv` on Go's shared box) | 1 oracle case | fix (REAP4) |
| Clojure eval | `javax.xml.stream` (D6) | 1 test | design (D6) or defer |
| Clojure eval | `seq-and-transducer`'s 200,000 trials (574 s) | 1 test | defer (speed) or a timeout (EC7) |
| Clojure eval | the oracle's world: locales 42, providers 1, `SecurityManager` 1, a message 1, main's divergences 2, an identity hash 1 | 48 | design (each decided, the known file's reasons) |
| Clojure AOT | the suite compiled | the whole suite | tool: REAP3 |
| Clojure AOT | F2.4: compiled classes naming the namespace's vars or holding `deftype*` in fns | 11 diffcheck cases (+ the suite's) | fix: REAP3 (RPS6) |
| Clojure AOT | compiled load-time semantics (vars interned at load, `Compiler/LINE`, shared symbols) | 6 diffcheck cases | design (as the JVM's AOT; F3.3 (a)) |
| Clojure print | — | 0 | — |
| Java read | `--release N` without `ct.sym`; doclint; `-encoding`, a jar on the module path; a message, a path | 41 of 1,211 diagnostics tests | design (javac's world) or defer `ct.sym` (RPS7) |
| Java read | arrays of more than nine dimensions | 2 tests | fix: JCS12 (b) (JC5) |
| Java eval | the world (jrt's classes and members, extendable superclasses) | 505 of 2,121 | JC6: each class design or defer (RPS8) |
| Java eval | the interpreter's bugs, the REPL's lookup | 73 + 23 | fix: JC4 (running) |
| Java eval and AOT | the analysis' gaps | 79 (j2g 77) | fix: JC5 (JCS11 (c)) |
| Java eval | timeouts (HotSpot's warm-up loops) | 83 | design (speed, the check's limit) |
| Java AOT | j2g's own: a class's failure stopping the run, names colliding, Go that does not compile, unnamed-package roots | 23 + 4 + 13, and the run-stopper | fix: JC2 (JCS15) |
| Java AOT | the world at run time (cut members, reflection over non-public members) | 83 + 13 | JC6 (design or defer) |
| Java print | the conformance corpus not printed | 2,121 tests | tool: REAP5 (RPS9) |
| Java print | Java's type arguments in code not in the forms | 39 of 838 jdk files (anew) | defer to JFS75 (b)'s model (RPS9) |
| Java print | the class files of printed Java: `?:` against `if`, switch expressions, initializers, fall-through, increments, order | 407 of 1,035 Java files | design (PRINTER-NOTES, the forms do not record them; `:full` passes all) |
| Go read | raw only (invalid programs, goos packages for other boards), go/parser's rejections | 117 + 73 | design (no final forms; judged on gc's syntax errors) |
| Go eval | GC157, `go:embed` | 6 + 2 | fix: running (`goi-after-v1`) |
| Go eval | the 15 other classes of v1 | 219 | design (GOI-SPEC "v1's divergences") |
| Go AOT | the check itself | — | tool: REAP2 (RPS10, RPS12) |
| Go AOT | `bin/build-go`'s gofmt layout moves lines | 10 run programs | fix: the check builds with `--layout lines` (RPS17) |
| Go AOT | expressions' line breaks, `//line` directives, `-trimpath`'s `GOROOT`, the go command's logs | 6 run programs | design (goi's classes; `:full` forms would keep the first two) |
| Go AOT | gc's flags of an action | 13 run, 124 errorcheck | fix: the build passes the action's `-gcflags` (RPS17), or design as goi's |
| Go AOT | invalid programs: no forms; the printer refusing; spellings, lines, imports and directives `:lines` drops | 134 + 3 + 16 errorcheck | design (GC47, GC49's kin) or fix by printing mode `:full`'s forms (RPS17) |
| Go AOT | directory tests through `arbace.build.go` | 127 run directory tests | fix: REAP2 (RPS11) |
| Go AOT | `runoutput` programs (a program that writes the program to run) | 22 | tool: REAP2 (the generated program read by repr/go and built) or design |
| Go print | an empty declaration group; TamaGo's tests embedding their sources | 3 + 9 | design (recorded) |

## 7. Plan (REAP1-REAP9)

Each milestone ends with its check green and its records (journal, agenda, this survey's "As
built"). Estimates in working days, as the repository's surveys give them (the recent ones have
run well under). Java's milestones are the conformance survey's JC1-JC8 (binding, JCS14 (a)'s
order), named here as they are; the others are this survey's.

| milestone | content | exit criteria | days | depends on |
|---|---|---|---:|---|
| **JC1** (running, `java-check-jc1`) | `bin/java-check`, its interpreted level and `read`, the JDK's references as data, the classes | the conformance survey's | 3-4 | — |
| **JC4** (running, `java-interp-jc4`) | the interpreter's bugs, the REPL's source lookup | the conformance survey's | 2-4 | JC1 to measure |
| **REAP1** the record and the matrix's tool | one classes file per language and level in `test/goi/divergences.edn`'s format (`test/clojure/divergences.edn`: the suite's skipped and non-passing assertions, the oracle's 53, diffcheck's 23, repr/clojure's one-way rules; Go's read and print classes beside goi's: `native-known.edn`'s, the raw-only list, the text level's 3, the tests' 9), each with by design or deferred and where decided; `bin/reap` (RPS13): reads every cell's reference and classes and prints the matrix, failing on a non-pass no class takes; the suite's results judged per assertion's test against the classes (`bin/clojure-tests --divergences`) | the matrix printed from the references, every non-pass of Clojure's and Go's cells in a class, `make reap` | 2-3 | — |
| **REAP2** Go AOT as a check | `bin/goi check --aot` (or `bin/build-go check`, RPS10): §5.1's and §5.2's method as a tool: the run programs built by `arbace.build.go` against gc's runs of the originals (cached by the original's and the toolchain's hashes, as goi's references), the errorcheck programs' printed forms compiled by gc and judged, the directory tests built (their local imports rewritten under the module, RPS11), results in `test/goi/check-aot-linux-amd64.edn`, the classes in `test/goi/divergences-aot.edn`; goi's own reference switched to gc on the originals (RPS12) | every program of goi's corpora with an AOT result, none failing outside a class; in `--full` beside goi's check | 1-2 | REAP1's format |
| **REAP3** Clojure's suite compiled | the suite's namespaces and test libraries (as `bin/clojure-tests` translates them) prepared and compiled by c2j with the runtime's (diffcheck's steps 3-4), the program built with the suite's Java fixtures (the test build's), the runner loading each through its compiled loader; `bin/clojure-tests --aot` with its reference `test/arbace-go-aot-results.edn`; F2.4's limit (j2g's analysis of compiled classes naming the namespace's vars, 11 diffcheck cases) fixed or deferred (RPS6) | the suite's counts at the AOT level recorded, every non-pass in a class; diffcheck unchanged or better | 3-5 | REAP1 |
| **REAP4** Clojure's eval classes closed | the deferred ones of §2.4 decided or fixed: the evaluator's line numbers in traces (7 assertions), FIPrims through run-time interfaces (2), `BitSet` among the proxy superclasses (1), the boxed NaN's identity (1 oracle case), `javax.xml.stream` (1 test); `seq-and-transducer` by its time (RPS5) | each fixed or recorded deferred by the user's decision; the suite's reference updated | 1-3 | REAP1 |
| **JC2** the compiled level | the conformance survey's | the conformance survey's | 3-4 | JC1 |
| **JC3** the front end's levels | `classes`, `read`, `diagnostics` in `bin/java-check` (Java's read cell) | the conformance survey's | 1 | JC1 |
| **REAP5** Java's print level | `print` in `bin/java-check` (RPS9): every test's forms printed by `bin/repr-java print` (lines layout) and read again (forms equal), javac on Arbace compiling the printed Java (accepted; class files against the original's, the differences classed as `roundtrip`'s: control flow, initializers, fall-through), and mode `:full` (text byte for byte); the 39 type-argument files decided (RPS9) | every test of JC1's corpora with a `print` result, every non-pass in a class | 1-2 | JC1, JC3 |
| **JC5** the analysis' gaps | the conformance survey's (with JCS12 (b), arrays of more than nine dimensions) | the conformance survey's | 3-5 | JC1, JC2 |
| **JC6** the world decided, the corpus widened | the conformance survey's; RPS8 decides the corpus the milestone takes | the conformance survey's | 2-4 | JC2, JC5 |
| **JC7** `/test/lib` and the frameworks | the conformance survey's; in the milestone or after it (RPS8) | the conformance survey's | 3-5 | JC6 |
| **JC8** (its host half) | Java's divergences recorded as v1 (no box, by the milestone's scope) | no test failing outside a class | 0.5-1 | JC2-JC6 |
| **REAP6** Go's deferred | GC157 and `go:embed` (running, `goi-after-v1`), merged; goi's divergences without deferred classes, or with those the user leaves | `bin/goi divergences` | 0 (running) | — |
| **REAP7** the user's AOT entry (optional, RPS14) | `bin/arbace-go --build-program DIR`: a program from a user's Clojure namespaces, Java forms and Go forms (c2j, j2g, `arbace.build.go`), the tool REAP3 and JC2 build for their corpora made general | a sample program of each language built and run | 1-2 | REAP3, JC2 |
| **REAP8** the essential subsets | each new check's essential subset in the essential gate (RPS15): Go AOT's (a few hundred fast programs), Clojure's compiled suite's (some namespaces), Java's (JCS13 (a)) | the essential gate under its present time plus a few minutes | 0.5-1 | REAP2, REAP3, JC1-JC2 |
| **REAP9** the milestone run | `bin/gate --full` with the new checks (`bin/reap`), `bin/arbace-go --fixpoint`, a re-seed (`bin/seed --make`, `--verify`) carrying every runtime change since `54f77c90`, the classes reviewed with the user, the record (a section "REAP v1's divergences" here and in each language's notes), the annotated tag `arbace-reaps-3-crops-v1` | the matrix all green, no cell with a non-pass outside a class | 1-2 | all |

**Total.** Java JC1-JC6 and JC8's half 14.5-23 days (JC1 and JC4 running), JC7 3-5 more if in;
REAP1-REAP5 and REAP8-REAP9 9.5-18 days; REAP7 1-2 if taken. **About 24 to 41 working days** with
JC7 out and REAP7 in (27 to 46 with JC7). Java is the long pole; the Clojure and Go milestones are
independent of it and of each other once REAP1's format is fixed.

**With the five agents running** (JFS65 (b) j2g's array casts, JFS75 (b) repr/java's model, goi's
GC157 and `go:embed`, JC1, JC4): REAP1 can start now (it touches only `test/` data, `bin/reap`
and the Makefile); REAP2 beside it (`bin/goi`, `test/goi/`: after `goi-after-v1` merges, since both
touch `bin/goi`); REAP3 after JFS65 (b) merges (both touch j2g; F2.4's fix is j2g's analysis) and
outside `arbace/lang/` while JC4 runs; REAP4 after JC4 (both change `arbace/lang/Compiler.ae`); JC2
after JC1 and JFS65 (b); REAP5 after JC1 and JFS75 (b) (the printer reads the model). At most four
agents at once keeps to the host's soft guide.

**Re-seeds.** The runtime changes batch: JC4's, JFS65 (b)'s, GC157/`go:embed`'s and GPS49 (c)'s
(already batched) into one re-seed after JC4 and JFS65 (b) merge; REAP3 (j2g's analysis), REAP4
(the evaluator) and JC5 (the analysis) into a second; REAP9's the last, from the milestone's
fixpoint. REAP1, REAP2 and REAP5 change no shipped code (tools and data under `test/`, `bin/`), so
no re-seed.

## 8. Proposals (RPS1-RPS17)

Decided already, not proposals (journal 2026-10-24): the criterion (goi's v1 rule), Java's read
(javac on Arbace; JN9b-JN9d beside), Go's AOT (`arbace.build.go` over `$GOROOT/test` against gc),
the scope (linux/amd64, forms only), the tag `arbace-reaps-3-crops-v1`, annotated with the crops'
versions; JC1-JC8 and JCS1-JCS15 for Java's eval and AOT.

- **RPS1 Names.**
  - Options: (a) the milestones REAP1-REAPn, the proposals RPS1-RPSn, Java's keeping their JC
    names (JC1-JC8) inside the plan; (b) everything renamed REAPn, JC1-JC8 included; (c) FL1-FLn
    and FLS1-FLSn (unused), "full support" in the names.
  - *Recommended: (a)*: the user's name for the milestone, and the JC milestones already
    running under their names (two branches).
- **RPS2 What Clojure's read and print are.**
  - Options: (a) both halves: the language's own (the reader and the printer, judged by the
    suite and the oracle) and the bridge's (repr/clojure's read and print, judged by its round
    trip); (b) the reader and the printer only (repr/clojure a tool); (c) repr/clojure only, as
    repr/java and repr/go are Java's and Go's read and print.
  - *Recommended: (a)*: a user reads a Clojure library through both, and both are checked
    today; for Java and Go the language's reader is the bridge itself.
- **RPS3 The record.**
  - Options: (a) per language and level, classes as data in `test/goi/divergences.edn`'s format
    (`:id`, `:title`, `:kind` `:design` or `:deferred`, `:why`, `:decided`, `:match` regexps over
    `RESULT REASON`), each check's references classified by a verb that fails on a non-pass no
    class takes; the existing known files (the oracle's, diffcheck's, the round trips') kept and
    their entries given a class; (b) one file for the whole matrix; (c) the known files as they
    are, the classes in this survey's prose.
  - *Recommended: (a)*: goi's v1 did it so and the rule is the same; one file per check keeps
    each check's owner the file's owner.
- **RPS4 Clojure's eval and AOT corpora.**
  - Options: (a) Clojure's suite at both levels: eval as today (the test namespaces evaluated on
    the compiled runtime), AOT the suite's namespaces compiled by c2j (REAP3), both against
    upstream's results (every assertion passing), with the oracle (eval) and diffcheck's pair (both
    levels against each other) beside; (b) the suite at eval, diffcheck's harvest (3,189 forms of
    the suite's assertions compiled) standing for the AOT level; (c) (a) and the evaluated build
    (`ARBACE_AOT=0`) as a third configuration.
  - *Recommended: (a)*: the suite is Clojure's own corpus, as `$GOROOT/test` is Go's; (b) leaves
    the namespaces' load-time semantics and the compiled deftypes untested (F2.4's 11 cases are
    exactly those); (c) checks a build nobody ships (§5.3 measures it once, for the record).
- **RPS5 Clojure's deferred non-passes.**
  - Options: (a) fix the local ones in REAP4 (the boxed NaN's identity, `Util.equiv`'s short-cut
    on Go's shared NaN box; `BitSet` among the proxy superclasses), and record deferred the evaluator's
    line numbers in traces (7), FIPrims through run-time interfaces (2), `javax.xml.stream` (1
    test), `seq-and-transducer` (speed: the evaluator's 574 s); (b) fix all, the line numbers and
    FIPrims included; (c) record all deferred.
  - *Recommended: (a)*, (b)'s two after the milestone: each needs a design change (frames per
    expression, annotated run-time interfaces), and the AOT level may pass them (REAP3 shows it).
- **RPS6 j2g's F2.4 limit (compiled classes naming the namespace's vars).**
  - Options: (a) fixed in REAP3: j2g's analysis of c2j's classes given what it lacks (the
    namespace's vars and the compiler's classes as the edge of the compiled tree), so that a
    compiled deftype or fn holding a `deftype*` or naming the namespace's vars is translated;
    (b) deferred, a class of the AOT level (its 11 diffcheck cases and whatever the compiled suite
    adds).
  - *Recommended: (a)*: a `defrecord` inside a fn is ordinary Clojure; an AOT level that drops it
    is not full support.
- **RPS7 Java's corpus for the milestone.**
  - Options: (a) JC1's corpora (`jdk/java/*` but AWT and Swing, `langtools/tools/javac`,
    `hotspot/jtreg/compiler`: run-plain, run-compile, run-jitopts), JC6's widening to all of
    `jdk/`'s plain run tests but AWT/Swing and `hotspot/jtreg/runtime`'s, langtools' golden
    diagnostics, the hand-curated JLS corpus (JCS2 (b)); JC7 (`/test/lib`, TestNG, JUnit) after
    the milestone; (b) (a) with JC7 in; (c) JC1's corpora alone, JC6's widening after.
  - *Recommended: (a)*: JC6 is where the world's classes are decided, which the v1 rule needs
    anyway; JC7 vendors two frameworks and is a corpus extension, not a level.
- **RPS8 The world's classes at the milestone.**
  - Options: (a) as JCS9 (b): JC6 ranks them by tests gained per jrt change, the user decides each
    as by design or deferred (named), and the milestone takes deferred world classes as the v1
    rule allows; (b) a floor: the milestone waits for the cheapest-ranked world classes (say the
    first ten) to be built; (c) every class not in J2G-SPEC §12 built first.
  - *Recommended: (a)*: the rule asks for the classes to be named, not for the world to be
    complete; (b) is the user's call after JC6's ranking.
- **RPS9 Java's print level.**
  - Options: (a) `print` in `bin/java-check` (REAP5: forms → Java → forms equal, javac on Arbace
    accepting the printed Java, its class files against the original's under `roundtrip`'s
    classes, and mode `:full`'s text), the 39 type-argument files (and the conformance corpus's
    like them) a deferred class until JFS75 (b)'s model can carry type arguments; (b) (a) and the
    printer inferring type arguments now (javac's attribution recorded in mode `:full`); (c)
    print judged on the three round-trip corpora only.
  - *Recommended: (a)*: the conformance corpus is the milestone's for every level; the type
    arguments belong to the forms' model, which JFS75 (b) is reworking.
- **RPS10 Go AOT's tool.**
  - Options: (a) `bin/goi check --aot`: goi's corpora and their reading, the references and the
    classes beside goi's, a level of the same check; (b) `bin/build-go check`, a tool of its own;
    (c) §5's scripts kept as they are.
  - *Recommended: (a)*: one corpus, two levels, as `bin/java-check`'s levels; the box parts of
    `bin/goi` untouched.
- **RPS11 Directory tests at the AOT level.**
  - Options: (a) built by `arbace.build.go`: the packages of `F.dir` placed under the module and
    their local imports (`"./a"`) printed as module paths, as Go's test driver's `-D` and importcfg
    resolve them; (b) the round trip's `run` level stands for them (gocmp's build of the printed
    files as the driver's, 127 of 127 against the golden `.out`); (c) out of the AOT corpus.
  - *Recommended: (a)*, with (b) as their reference meanwhile: the decision names `arbace.build.go`,
    and a user's multi-package program is a module.
- **RPS12 Go AOT's reference and limits.**
  - Options: (a) gc's build and run of the originals as Go's test driver runs them (GOPATH mode, the
    action's arguments, stdout and stderr to the first `goroutine ` line, the status, the golden
    `.out` where there is one), the time limit goi's (10 s, the action's `-t`, three times for the
    reference), and goi's own check moved to the same reference (today its reference is the
    printed forms' build, Arbace's AOT); (b) as (a) but goi's reference left as it is; (c) the
    golden `.out` alone.
  - *Recommended: (a)*: one reference per language at every level (RPS16); §5.1 shows gc on the originals and goi's present reference (the printed forms) differ only where a class already says why (positions, `//line`, `-trimpath`), so goi's results do not move but for those, and its known file already holds them*.
- **RPS13 The matrix's tool.**
  - Options: (a) `bin/reap` (and `make reap`): reads every cell's reference and classes (no runs),
    prints the matrix (pass, by design, deferred, per cell), fails on a non-pass no class takes,
    in the essential gate (seconds); (b) each check's own verb, the matrix in this survey only.
  - *Recommended: (a)*: the milestone's criterion becomes one command, and the agenda can print
    its state.
- **RPS14 An AOT entry for a user's program.**
  - Options: (a) REAP7 in the milestone: `bin/arbace-go --build-program DIR` from a user's
    Clojure namespaces, Java forms and Go forms (Go has `go/run-compiled` already); (b) after the
    milestone, the levels checked by the checks' own harnesses.
  - *Recommended: (a)*: "AOT for Clojure and Java" means little if only the checks can do it; the
    harnesses REAP3 and JC2 build are most of it.
- **RPS15 The gates.**
  - Options: (a) each new level's essential subset in the essential gate (Go AOT's fast programs,
    some of the compiled suite's namespaces, Java's JCS13 (a)), the wholes in `--full`; (b) the
    wholes in `--full` only.
  - *Recommended: (a)*: as goi's and diffcheck's; the essential gate grows by a few minutes.
- **RPS16 Per-cell criteria.**
  - Options: (a) each level against the language's reference, not against the other level
    (Clojure's eval and AOT both against upstream's suite, Java's both against the JDK's runs, Go's
    both against gc), the two levels' disagreements listed as diffcheck's are (a test passing at
    one level and not the other is a finding, not a class); print judged at the forms level (read
    back equal) and the text level (mode `:full`, byte for byte), the class files where the
    language has them; `bin/gate --full` as it is for the milestone run, its box steps as
    regression checks only (the milestone's scope is the host); (b) as (a), the box steps left out
    of the milestone's run; (c) the AOT level judged against the eval level (diffcheck's rule).
  - *Recommended: (a)*: one reference per language keeps the classes about the language; the box
    steps cost nothing new and guard what the earlier milestones built.
- **RPS17 How the AOT check builds a program.**
  - Options: (a) with the lines layout (`bin/build-go --layout lines`; or `--line-file`, which names
    the forms' files in positions), the action's gc flags passed to the build (`-gcflags` from the
    action line, and for errorcheck programs `go tool compile`'s flags), and the errorcheck
    programs printed from mode `:full`'s forms where the converter has them; (b) `bin/build-go`'s
    defaults (gofmt layout, no flags), the positions and flags as classes; (c) (a) without mode
    `:full` for errorcheck.
  - *Recommended: (a)*: positions are part of a program's behaviour (10 programs, §5.1); gc's
    flags are gc's to honour at the AOT level, which is gc (13 run and 124 errorcheck programs
    become judgeable); mode `:full` keeps what the GC49 kin needs. (c) if `:full`'s printing of
    invalid programs turns out costly.

## Decided (2026-10-24): RPS1-RPS17

The user decided every proposal as recommended, (a): RPS1 (REAP1-REAP9, RPS, Java's JC names kept),
RPS2 (Clojure's read and print both the reader and printer and repr/clojure), RPS3 (classes as data
per check, `test/goi/divergences.edn`'s format), RPS4 (Clojure's suite at eval and AOT, against
upstream's), RPS5 (NaN identity and `BitSet` fixed in REAP4; trace lines, FIPrims, `javax.xml.stream`,
`seq-and-transducer` deferred), RPS6 (F2.4 fixed in REAP3), RPS7 (JC1's corpora and JC6's widening,
the JLS corpus; JC7 after the milestone), RPS8 (the world's classes by JC6's ranking, deferred ones
allowed), RPS9 (Java's print level in `bin/java-check`, the 39 type-argument files deferred to JFS75
(b)'s model), RPS10 (`bin/goi check --aot`), RPS11 (directory tests built by `arbace.build.go`),
RPS12 (gc on the originals the reference of goi's check and of AOT), RPS13 (`bin/reap`), RPS14
(REAP7 in the milestone), RPS15 (essential subsets in the essential gate), RPS16 (each level against
the language's reference; `--full` as it is, its box steps regression checks), RPS17 (the lines
layout, the action's gc flags, errorcheck programs from mode `:full`). REAP1 and REAP2 started.

## As built: REAP4 (2026-10-24)

Branch `reap4-clojure-eval`, from main `407ed654`: RPS5 (a) as decided (journal 2026-10-24), and
SL5 (EVAL-NOTES.md's proposal of 2026-10-10, "not taken" then), added to REAP4 by the user's RPS18
(b). The runtime changes (`arbace/lang/Compiler.ae`, the proxy superclasses, the embedded
namespaces) reach stage 0 with the second batched re-seed, not this branch.

**The boxed NaN's identity: fixed.** Traced, the cause is not Go's boxing: jrt's `Double.valueOf`
boxes anew as the JVM's does (`(identical? (Double/valueOf ##NaN) (Double/valueOf ##NaN))` is false
on both). The reader's `##NaN` (`LispReader$SymbolicValueReader`'s static `specials` map) is one
Double, on the JVM too, so every `##NaN` read is the same object. The JVM's compiled code makes a
constant collection in the class's static initializer by `emitValue`, each Double element boxed
anew by `Double.valueOf`; the evaluator's `Evaluator.constant` (`ConstantExpr.evalIn`, made once
per constant) rebuilt the collections but kept their numbers themselves. So `[##NaN ##NaN]` held
one Double twice: the oracle's class script `^{:sig [Object Object]} (Util/equiv ##NaN ##NaN)`
(its arguments come in a constant map, `(oracle.driver/step {... :args [##NaN ##NaN]})`) took
`Util.equiv`'s identity short-cut, and in user code `(= [##NaN] [##NaN])`, `(= #{##NaN} #{##NaN})`,
`(contains? #{##NaN} ##NaN)` were true where the JVM answers false. The fix: `Evaluator.constant`
boxes an Integer, Long, Double or Character anew by its `valueOf`, as `emitValue` does (a
`NumberExpr` itself stays one constant per site on both: `(identical? ##NaN ##NaN)` is true on
the JVM too). c2j's compiled constants already did (`arbace.c2j.emit/emit-value`). Regression
cases: 15 forms appended to `test/oracle/forms/numbers.ae` (identity and equality of NaN, Inf,
characters and longs in constant vectors, lists, maps and sets, in fns and at the top level),
recorded on the frozen jar (`bin/oracle record forms/numbers`: 822 cases to 837); the Go build
passes all 837 and the class script's 614. The case leaves `test/oracle/known-go-amd64-frozen.edn`
(53 known to 52) and `known-go-box-amd64.edn` (not run here: the box's oracle subset is `--full`'s).

**`java.util.BitSet` among the proxy superclasses: fixed.** `arbace.j2g.model/proxy-supers` lists
it (J2G-SPEC §5.12 amended: the list, and a listed class is not a leaf): j2g writes
`DynSub_BitSet`, BitSet's uses become its class interface, `proxy-super flip` reaches BitSet's
implementation. `java-interop`'s `test-proxy-super` passes (3 assertions; the namespace 299 of
302). No hand-written jrt code names BitSet; the executable's size: see below.

**SL5: D6's namespaces embedded with their classes cut.** `arbace/inspector.ae`, `arbace/xml.ae`,
`arbace/java/browse.ae`, `arbace/java/javadoc.ae` restored from `ac66b276^` (S4 removed them; the
JVM's are on `arbace-for-java-26`), embedded (`arbace.j2g.embed/excluded` no longer lists them),
hand change 30 (VENDOR-NOTES.md): hand change 17's module checks gone, `arbace.xml`'s
`content-handler` nil when `XMLHandler` or its proxy throws. The classes they name and the world
lacks are cut (J2G-SPEC §4.1, M3), with one change to make it so: the cut candidates' packages
take `org.xml.sax` and `org.w3c.dom` (java.xml's) beside `java`, `javax`, `jdk`, `sun`
(`arbace.j2g.embed/cut-candidates`, `with-member-types`). `jdk/edge.edn` recorded afresh (`bin/j2g-edge
update`: the cut classes' member tables and their member types, 1.29 MB to 1.93 MB; j2g's input,
not shipped). `arbace.java.browse-ui` (a Swing window) is not restored: `browse-url` reaches it only
after the browser commands and `java.awt.Desktop` fail. Results: `metadata` **53 of 53** (6 tests);
`java.javadoc` **2 of 5**, its 3 errors: `Class.getModule` (jrt has no `java.lang.Module`; 2:
`java.lang.String`'s and `Map$Entry`'s URLs) and `Class/forName "java.sql.Connection"`, a class
the Go build lacks (D6: java.sql; 1). `clojure-xml` stays skipped: `arbace.xml` loads but `parse`
throws at `SAXParserFactory` (cut), its one test an error. SL5's estimate (half a day; "javadoc
(5) would pass") held for the embedding; the javadoc test needs a jrt class besides (RPS55).

**Numbers.** Clojure's suite on the test build, before (main `407ed654`) and after:

| | before | after |
|---|---:|---:|
| namespaces run (all load) | 76 | 78 |
| tests | 777 | 784 |
| assertions | 20,520 | 20,580 |
| pass | 20,506 | **20,564** |
| fail, error | 11, 3 | 11, 5 |
| namespaces skipped | 9 | 7 |
| test.generative | 27 of 27 | 27 of 27 |

The 16 not passing: `compilation` 10 (Q1's boxed primitive fns 3, by design; trace lines 7,
deferred), `java-interop` 3 (FIPrims, deferred), `java.javadoc` 3 (RPS55). The oracle (essential gate): **22,688 of
22,740** (the 15 new cases with it; 55 by V11), 52 known, 0 new. diffcheck: 12,778 of 12,801
agree, its 23 known, 0 new (the new forms compiled agree: c2j's constants were the JVM's). The
executable 153.5 MB (153,469,088 bytes; main's build of the same evening 152.4 MB, of a later
main: the embedded namespaces, their cut classes and `DynSub_BitSet` together about 1 MB). The
essential `bin/gate` passed at the branch's head (11m26s; the builds cached from the runs above).

**Deferred by RPS5 (a)**, their references' comments citing it (`test/arbace-go-results.edn`):
the evaluator's line numbers in stack traces (`compilation`'s `test-compiler-line-numbers`, 7
assertions), FIPrims through interfaces made at run time (`java-interop`'s
`test-invoke-fiprim-rets`, `test-match-prim-args-only-to-2`: 1 fail, 2 errors), `javax.xml.stream`
(`reflect`'s `invoke-checks-accessibility`, skipped), `seq-and-transducer` (`transducers`, skipped:
speed). For REAP1's `test/clojure/divergences.edn` (not written here), the classes: **by design**
`evaluator-boxed-primitive-fns` (Q1: `primitive-return-decl` 2, `incorrect-primitive-type-hint-throws`
1); **deferred (RPS5 (a), journal 2026-10-24)** `evaluator-trace-lines` (7), `fiprims-runtime-interfaces`
(2 tests), `javax-xml-stream` (1 skipped test), `seq-and-transducer-speed` (1 skipped test); **by
design (D6)** `sax-cut` (`clojure-xml`, skipped, now loading), `java-sql-cut` (javadoc's
`java.sql.Connection`, 1); **deferred or fixed (RPS55)** `class-get-module` (javadoc, 2). The
BitSet and NaN entries are gone (fixed); `metadata` and `java.javadoc` are no longer skipped.

### Proposals RPS50-RPS59

- **RPS50 The constants' boxes.** (a) every Integer, Long, Double and Character of a constant
  collection boxed anew, as `emitValue` (built); (b) Doubles only (the NaN case); (c) the reader's
  `##NaN` a new Double per read (diverges from the JVM's reader, which shares). *Recommended: (a)*:
  it is the JVM's rule, and c2j's already.
- **RPS51 Where the evaluator's constants are checked.** (a) the oracle's forms (built: 15 cases)
  and diffcheck's pair, which runs the same forms compiled; (b) also a unit test of
  `Evaluator.constant`. *Recommended: (a)*: the oracle is the JVM's answer, diffcheck c2j's.
- **RPS52 The proxy superclasses.** (a) a fixed list grown by need (built: BitSet); (b) every
  non-final translated class proxyable (a `DynSub_C` each, every class non-leaf: size and speed);
  (c) BitSet by design out. *Recommended: (a)*: a proxy of a JDK class is rare outside the suite.
- **RPS53 The evaluator's lines in stack traces (7).** (a) deferred past the milestone (RPS5
  (a)), REAP3's compiled suite showing whether the AOT level passes them; (b) frames per field
  access, assignment and `IFn` cast in the evaluator (a `line` per Code node, F7's generated
  nodes regenerated); (c) by design. *Recommended: (a)*, then (b) if REAP3's level passes.
- **RPS54 FIPrims through run-time interfaces (2 tests).** (a) deferred (RPS5 (a)); (b) the
  interfaces `definterface` makes carry `@FunctionalInterface`-equivalent data so `FISupport` and
  jrt's adapters see them. *Recommended: (a)*.
- **RPS55 `java.javadoc`'s three.** (a) a hand-written `java.lang.Module` in jrt (`getName`,
  `isNamed`, `toString`, `getDescriptor` throwing) and `Class.getModule` (java.base for the
  world's JDK and jrt classes, the unnamed module for Arbace's and the run time's; javac's tool
  world's modules by its sources' list), the manifest and member tables regenerated
  (`bin/jrt manifest`), the edge again: 2 of the 3 pass, about half a day and two build cycles;
  the third (`java.sql.Connection`) by design (D6); (b) all three recorded: `class-get-module`
  deferred, `java-sql-cut` by design. *Recommended: (a)* for the milestone (Clojure's own test
  passing beats a class), the java.sql assertion by design.
- **RPS56 `arbace.xml` and `clojure-xml`.** (a) by design, D6's SAX cut (the namespace loads,
  `parse` throws; the test skipped); (b) java.xml's SAX parser in the shipped world (javac's tool
  world holds it since JN7: the classes, `overlay/jdk/javac/closure.txt`), `clojure-xml`'s test
  passing, the executable larger. *Recommended: (a)*, (b) if the user wants `arbace.xml` working
  at the REPL.
- **RPS57 `arbace.java.browse-ui`.** (a) not restored (built): Swing is cut and its one fn
  needs a window; (b) restored and embedded, its proxy of `HyperlinkListener` expanding to a
  throw. *Recommended: (a)*.
- **RPS58 The cut candidates' packages.** (a) `org.xml.sax` and `org.w3c.dom` beside `java`,
  `javax`, `jdk`, `sun` (built); (b) any class the JVM has. *Recommended: (a)*: (b) would cut a
  library's own classes that a source names before the library is on `ARBACE_PATH`.
- **RPS59 The survey's account of the NaN case.** (a) corrected here (the cause is the
  evaluator's constants, not Go's box; EVAL-NOTES.md's and J2G-NOTES.md's lines left as dated
  records, the journal's entry at the merge naming the correction); (b) those notes edited too.
  *Recommended: (a)*.

## 9. Risks

- **Java is the long pole.** JC1-JC6 are 14-22 days of the 24-41, and the world's long tail
  (the conformance survey §10) makes the classes large; the rule asks for them to be named, so the
  risk is the user's decisions' pace (RPS8), not the code's.
- **The compiled suite may find more of F2.4's kind.** diffcheck's corpora are forms written to be
  compiled; the suite's namespaces define deftypes, protocols and records inside fns and macros,
  and redefine vars at load. REAP3's estimate assumes the compiled suite's new non-passes are a few
  classes; a j2g analysis change is a runtime change (a re-seed) and j2g's own fixpoint.
- **Memory and the shared host.** j2g's whole-program runs (the compiled suite, Java's shards)
  take up to 20 GiB and a heavy slot each; the measurements here queued behind other agents' jobs
  for a long time (Appendix A's wall times are of the runs alone).
- **Nondeterminism.** The Clojure suite's concurrency tests and the oracle's are made
  deterministic (ORACLE.md's rule); Go's AOT runs of `$GOROOT/test` have their own (timing,
  `runtime.NumGoroutine`); a program whose two gc runs differ needs the status-only rule Java's
  JCS3 has (REAP2).
- **References that move.** Clojure's is upstream's suite at the vendored revision (fixed); Java's
  the JDK's runs recorded (JCS5 (b)); Go's gc (cached by hash). A toolchain or jdk26u re-pin records
  them again.
- **Speed classes.** The evaluator's and goi's speed classes (`seq-and-transducer`, goi's 6, Java's
  83 timeouts) are by the check's time limits; they shrink only with speed work (F10), which is
  not in this plan.

## 10. Sources

- Arbace main at `fcda820e`: CLAUDE.md; doc/AGENDA.md; doc/JOURNAL.md (2026-10-24, "the next
  milestone's goal"); doc/REPR-CLOJURE.md ("Print", "The round trip", "Checks"); doc/go/EVAL-NOTES.md
  ("Phase 2C", "The suite's last failures", "The suite's Java fixtures"); doc/go/SPEED-NOTES.md (EC7);
  doc/STANDALONE-NOTES.md (S6, SA141-SA152); doc/FUTAMURA-SURVEY.md (F2, F3.3, F2.4); doc/go/ORACLE.md;
  doc/classes/JAVA-CONFORMANCE-SURVEY.md (§5, §6, §8, §9, "Decided"); doc/classes/JAVA-FRONTEND-SURVEY.md
  ("As built: JN9a"); doc/classes/PRINTER-NOTES.md ("The round trip", "Mode :full as built");
  doc/go/GOI-SPEC.md (§11, "As built: GR10", "v1's divergences"); doc/go/GO-PARSER-SURVEY.md (GP3-GP5);
  doc/go/BUILD.md; doc/go/ROUNDTRIP.md ("Directory tests").
- Data: `test/arbace-go-results.edn`, `test/baseline-results.edn`, `test/diffcheck/known.edn`,
  `test/oracle/known-go-amd64-frozen.edn`, `test/goi/check-linux-amd64.edn`,
  `test/goi/divergences.edn`, `test/repr/go/*.edn`, `test/repr/java/roundtrip/*.edn`,
  `test/repr/clojure/roundtrip-known.edn`; main's last gate logs (`.tmp/gate/`, `.tmp/diffcheck/check.txt`,
  `.tmp/oracle/check.txt`, 2026-10-24 13:03-13:11).
- Code read: `bin/goi` (`check_one`, `check_err`, `gc_env`, `action_of`), `bin/diffcheck`,
  `bin/build-go`, `arbace/build/go.ae` (`build`), `arbace/go/repl.ae` (`run-compiled!`),
  `arbace/lang/RT.ae` (`load`'s compiled loaders), `bin/arbace-go` (`build_tests`), `bin/clojure-tests`.
- Clojure's suite: `clojure/clojure` `test/` at `98d735fab02f337cee654cb0629bddc09883a75a`. Go:
  TamaGo's go1.27.1 at `/root/tamago-go` (`$GOROOT/test`). Java: jdk26u at
  `baf63fbe42b8758448fee570a9d9bb914272d259`.

## Appendix A: the scratch

In the worktree's `.tmp/fs/` (not tracked; kept, to be copied to main's `.tmp/` before the
worktree goes, as the journal's correction of 2026-10-24 asks):

| file | what |
|---|---|
| `scripts/aot_one.sh P` | §5.1 for one run program: gc's build and run of the original, `bin/build-go`'s of its forms, compared; prints `P RESULT REASON GOLDEN` |
| `scripts/aot_all.sh` | every program of `programs.txt`, 6 at a time: `results.tsv`, `wall.txt` |
| `scripts/aot_err.sh P` | §5.2 for one errorcheck program: gc's errors of the original and of the printed forms, judged by `gocmp errorcheck` |
| `scripts/aot_err_all.sh` | every program of `errprograms.txt`, 6 at a time: `eresults.tsv`, `ewall.txt` |
| `scripts/eval_suite.sh` | §5.3: the evaluated test build and Clojure's suite on it: `eval-build.log`, `eval-suite.log`, `eval-suite.txt` |
| `scripts/aot_one_lines.sh`, `scripts/aot_all_lines.sh` | §5.1 again with `bin/build-go --layout lines`: `results-lines.tsv`, `wall-lines.txt`, `work-lines/` (`failing.txt`, `results-lines-failing.tsv`: the gofmt run's 16 tried first) |
| `programs.txt`, `errprograms.txt` | the programs measured |
| `importcfg` | std's export data for linux/amd64 (`go list -export std`), the errorcheck compiles' |
| `summary.md`, `matrix.md`, `plan.md`, ... | this survey's drafts |
| the worktree's `target/arbace-go/amd64/arbace-tests` | §5.3's evaluated test build (93.9 MB), and `.tmp/clojure-tests/evaluated/` its suite run |
| `work/`, `ework/` | per program: the outputs of both sides (`gc/out`, `gc/err`, `a.out`, `a.err`, the build logs; `ref.out`, `aot.out`, the judges) |
