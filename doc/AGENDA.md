# Agenda

The current state of the work. This file is rewritten as things change. For the history, see
[JOURNAL.md](JOURNAL.md).

## State

- Stage 0 of the bootstrap is the binary seed `seed/arbace-seed.jar` (below). The frozen
  reference baseline `clojure/` (upstream Clojure `98d735f` plus ASM as `clojure.asm`) is on the
  branch `arbace-for-java-26`.
- `LICENSE.md` holds all licenses and which files each covers: Arbace is EPL-1.0, like Clojure;
  ASM is BSD-3-Clause, Guava's Murmur3 Apache-2.0.
- javalisp, the pathfinder (an exact Java ↔ s-expression transcription), was dropped once the
  class forms superseded it; it is in git history (last at `959d114`).

## Done: the class forms and self-hosting (steps 1-6)

Other documents refer to these step numbers.

1. Spec: every construct javac can emit as idiomatic Clojure, the class forms
   ([classes/SPEC.md](classes/SPEC.md)). Reviewed: the user accepted all 17 recommendations of
   §12. The 33 amendments found while implementing (14 compiler, 19 converter) were accepted
   and are folded in.
2. The class forms compiler `arbace/classes/` (`bin/class-forms-tests`,
   [classes/COMPILER-NOTES.md](classes/COMPILER-NOTES.md)).
3. The converter j2c `arbace/j2c/` (`bin/j2c`, `bin/j2c-check`,
   [classes/CONVERTER-NOTES.md](classes/CONVERTER-NOTES.md)). The converted baseline compiles
   to javac's class shapes, and Clojure's suite passes on it as on the baseline. The places
   where the compiler and the converter lagged the amended spec are closed (`f240a04`,
   `90ba9ae`, `13ef825`).
4. `clojure/` vendored as `arbace/`, all `.clj`, and self-hosted (`af29cc6`,
   [VENDOR-NOTES.md](VENDOR-NOTES.md)). Stages 1, 2 and 3 are byte-identical.
5. The class forms native (SPEC §9.5; `0290f14`, `5081e57`): `defclass` and the code forms are
   `arbace.core` vars and `arbace.lang.Compiler` special forms, compiled by the one
   implementation `arbace.classes`. With it, step 4's cleanups (`08e2167`): the main class is
   `arbace.lang.Main`, and runtime-visible `clojure` names are renamed.
6. Step 5's open ends (`367c83d`, `2dd9cba`, `2f2a5f2`): the §5.4 operators inline to
   instructions, `arbace.classes` is AOT-compiled into each stage, and class forms work in
   `deftype`/`defrecord` methods.

## Done: Arbace excellent on the JVM

- Compiled namespaces in every stage, a reproducible jar, the JDK AOT cache and `bin/arbace`
  (launch in about 0.2 s; `8c9f2d3`). Since 2026-10-08 the cache is trained without method
  profiles.
- The JDK verifier checks every class of stages 1 and 2, at class file version 70 (`e8a5e30`).
- By the user's decisions, each measured: `invokedynamic` reflective call sites, keyword sites
  and `str` (`StringConcatFactory`), an opt-in virtual-thread executor, and a `jlink` image.
  Tried and not kept: `Atom` on a `VarHandle`, condy constants. ASM stays, and Var calls stay
  indirect.
- The pre-freeze items chosen by the user (2026-10-07):
  1. The converted jdk26u compiled with the class forms compiler matches javac's class shapes
     for 12,376 of 12,444 files (99.5%, `bin/j2c-check --jdk`); the remainder is listed in
     [classes/CONVERTER-NOTES.md](classes/CONVERTER-NOTES.md), "The converted JDK".
  2. clojure.spec restored (`d9ecda7`): Clojure's suite passes in full on stages 1 and 2
     (20,750 of 20,750 assertions).
  3. The remaining todo rows: `deftype` inside class bodies, covariant bridges, Clojure's full
     overload matching, and the `SecurityManager` import.
  4. Benchmarks against Clojure 1.12.6 ([BENCHMARKS.md](BENCHMARKS.md)): startup 0.2 s against
     0.56 s, un-hinted interop about 580x and multi-argument `str` about 10x faster, the rest
     mostly even; open: `into` through a transducer 1.31x (javac 26's baseline too) and
     `apply str` 1.16x. The GitHub Actions gate (on the branch `arbace-for-java-26` only;
     main is gated by running the checks locally). The freeze kit (`doc/FREEZE.md`,
     `bin/freeze`; on the branch only since the freeze).

## Done: the `arbace-for-java-26` freeze (2026-10-08)

The branch `arbace-for-java-26` and the tag `arbace-for-java-26-v1` hold Arbace on the JVM, at
main `38a652d`, after the gate passed on it ([FREEZE.md](https://github.com/arbace/arbace/blob/arbace-for-java-26/doc/FREEZE.md) there). The repository is public.
The journal up to the freeze is on that branch; main's [JOURNAL.md](JOURNAL.md) starts over.

## Done: the binary seed (2026-10-08)

`clojure/` is gone from main (it remains on `arbace-for-java-26`). Stage 0 of the bootstrap is
`seed/arbace-seed.jar`, the freeze tag's jar, pinned by SHA-256 and reproducible from the tag
(`bin/seed --check`, `--verify`). The tools run on Arbace's `target/stage2`
(`bin/lib/tools.bash`), and take the frozen Clojure sources they compare against from the tag.
Removed from main with it: `bin/vendor-arbace`, `bin/vendor-spec`, `bin/freeze`, the CI
workflow, `doc/FREEZE.md` and `doc/MODERN-COMPILER.md` (all on the branch).

## Done: a faster gate (2026-10-08)

`bin/gate` (essential: the bootstrap, then Clojure's suite on stage 2 and the class forms tests,
concurrently; 3m40s) and `bin/gate --full` (adding the suite on stage 1 and `bin/j2c-check
--suite`; 6m25s, against about 13 minutes run one after the other). The unused non-native
fallbacks of `arbace/core_classes.clj` are gone.

## Next

- g2c, milestones G0-G2 (the user's go, 2026-10-08):
  - Done: the Go helper (`tools/godump`, `bin/g2c`; all of std for tamago/amd64 and arm64 and
    the `$GOROOT/test` programs, 0 type errors, read on Arbace), and the round-trip oracle
    (`tools/gocmp`, `bin/g2c-check`; tree, export data, object code; proven on identity,
    reprinted and respelled candidates, mutations caught).
  - Done: the Go forms spec `doc/go/SPEC.md`; the user accepted all 20 recommendations of its
    §15 and the 28 amendments of the helper, printer and converter (2026-10-08, folded in).
  - Done: the helper's format 2 (§11.3), the printer (`arbace.g2c.print`,
    [go/PRINTER-NOTES.md](go/PRINTER-NOTES.md)) and the converter (`arbace.g2c.convert`,
    `bin/g2c convert`, [go/CONVERTER-NOTES.md](go/CONVERTER-NOTES.md)): all of std (amd64,
    arm64, with tests) and `$GOROOT/test` convert, read back exactly, every node kind covered.
  - Done (G2): the round trip `bin/g2c roundtrip [amd64|arm64] [--tests]`: std (373 packages
    on amd64, 372 on arm64) and `$GOROOT/test` (1,707 and 1,702 programs) pass the tree,
    export and code levels; std with its tests too, but for 9 known differences (tests that
    embed their own sources). It exits non-zero on a regression against the references
    `test/g2c/roundtrip-*.edn`; about 40 s per architecture. One `$GOROOT/test` selection
    for the helper and the oracle (`gocmp tests`). In `bin/gate --full` for amd64 (tamago and
    linux).
  - Open: mode `:full` in the printer (columns; positions exact); `$GOROOT/test`'s directory
    tests.

- B1, Arbace on Go: plan in [go/B1-PLAN.md](go/B1-PLAN.md). First B1a, a static `linux/amd64`
  Go executable of Arbace tested as a user process (the user's proposal, 2026-10-08); the box
  (TamaGo, arm64) later as B1b. Decided: c2g + jrt, an evaluator first, Java's UTF-16 strings,
  a port of `java.util.regex`, a first REPL without the class forms, `gen-class`, `proxy`.
  B1a ends with a freeze (a branch and tag, as for Java 26; proposed branch
  `arbace-for-golang`, tag `arbace-for-go1.27.1`) and includes `linux/arm64`, tested
  under `qemu-aarch64`. Step 0 done (the round trip for linux/amd64 and arm64; `bin/g2c build`,
  static executables for both). Step 1 done: the Java surface measured
  ([go/JAVA-SURFACE.md](go/JAVA-SURFACE.md)), its seven decisions taken. Step 2 done: the c2g spec
  ([go/C2G-SPEC.md](go/C2G-SPEC.md)), accepted with its 23 recommendations. Done alongside:
  jrt's JDK sources through j2c (`bin/jrt-convert`, 184 files shape-identical; unreached
  methods to be stubbed by c2g) and the JVM oracle (`bin/oracle`, 15,854 cases). Step 3 done: jrt
  hand-written (core, reflection, threads and the host). Step 4 done: c2g translates the runtime
  and the JDK closure (8,955 of 9,002 oracle steps; 47 need `arbace.core`). Step 5 done
  (2026-10-09): the evaluator ([go/EVAL-PLAN.md](go/EVAL-PLAN.md),
  [go/EVAL-NOTES.md](go/EVAL-NOTES.md)), `proxy` over `Dyn`, jrt's surface for the REPL;
  `bin/arbace-go` builds static executables (58 MB amd64, 56 MB arm64). On amd64 the oracle
  passes 20,221 of 20,253 (the forms 10,075 of 10,077), Clojure's suite 18,781 of 18,806
  assertions (61 of 64 namespaces load).
  - Done (2026-10-09, the user's decision): the step 5 amendments accepted and folded, renamed
    M1-M8, X1-X4, S1-S8 (C2G-SPEC §16); B7's transcribed jdk26u parts kept, as LICENSE.md records.
  - Done (2026-10-09, the user's decision): the Go checks in `bin/gate --full` (amd64): jrt-convert,
    the Go build, its smoke test, Clojure's suite on it and the oracle against its known
    mismatches (`bin/oracle check --expected`, `test/oracle/known-go-amd64.edn`, 32 cases);
    `--full` now takes about 21 minutes.
  - Done (2026-10-09, the user's decision): the determinism issue at `polymorphism.clj:176`: a
    multimethod's ambiguity message names two classes in the order of their names
    (VENDOR-NOTES.md, hand change 14), on the JVM as on Go.
  - Step 6 done (2026-10-09): the executable with an image of the prepared core namespaces (start
    4.2 s → 0.35 s, [go/EXEC-NOTES.md](go/EXEC-NOTES.md)); amendments U1-U5 accepted and folded;
    the essential `bin/gate` smoke-tests the Go executable, cached by a hash of its inputs
    (seconds on a hit, about 5 minutes beside the suite on a miss).
  - Done (2026-10-10): `java.io.File`, the file system, `java.net.URL` for `file:` and a small
    `java.nio.file` (JRT-NOTES.md "Files", amendments FS1-FS7 accepted and folded); Clojure's suite on Go
    19,379 of 19,398 assertions, 0 errors.
  - Done (2026-10-10): the suite's last failures (EVAL-NOTES.md, SL1-SL7): Clojure's suite on Go
    19,506 of 19,506 assertions (65 namespaces, `java.io` waits for sockets; 20 skipped, by D6,
    the Java fixtures and `seq-and-transducer`'s time); test.generative runs on Go.
  - In progress (agents): step 7a (runtime speed), step 7b (the evaluator's closure
    compilation), and feature completion: class forms in the REPL, sockets and
    the socket REPL, the suite's last failures, regex `\N{name}` and `CANON_EQ`. Amendment Z1
    (pprint's BufferedWriter proxy) accepted and folded (2026-10-10).
  - Before the freeze: an assessment of how much of `java.base` jrt implements (the user's
    request, an agent; `doc/go/JAVA-BASE.md`); SL3's per-namespace timeouts once step 7 lands;
    the agents-without-`shutdown-agents` deadlock found by step 7a.
  - Then: step 8 (the `.ae` rename), step 9 (the freeze, with `bin/gate --full`).

- The `.ae` file extension (the user's decision, 2026-10-09): Arbace's sources hold forms Clojure
  cannot evaluate (class forms, Go forms), so `.clj` misleads; they move to `.ae` (ASCII; `.æ`
  considered and not taken: hard to type and script). One self-contained change, B1a's step 8:
  the last step before the `arbace-for-golang` freeze (the user's timing, 2026-10-09): loaders (`RT.load`, the compiler:
  `.ae` first, then `.clj`), the tools and their outputs (class forms build, j2c, g2c, c2g), `bin/`,
  `go/`, tests, the oracle, the docs; `bin/build-arbace` gives the frozen seed a `.clj` view of
  the sources for stage 1 (the seed stays as it is).

## Later

- The standalone Arbace: no Java binary compatibility, with Java at the source level through j2c.
  The language is Clojure plus class forms, and later forms for Go. The goal is a
  self-sustaining REPL in a virtual sandbox, in Arbace down to the bare metal ISA, in the style
  of Go plus TamaGo. Targets: /dev/kvm on amd64 (Alpine edge Linux host) and
  Hypervisor.framework on arm64 (macOS host). Open: the runtime's design, Go-inspired (scheduler,
  GC), with proper tail calls possible.
- g2c (Go as Arbace forms): survey and plan in [G2C-SURVEY.md](G2C-SURVEY.md). The open
  questions are decided (journal, 2026-10-07): front end, spec and round trip (G0-G2), then the
  box via gc + TamaGo (B1). No Go libraries on the JVM. Target `GOOS=tamago` amd64/arm64,
  go1.27.1, with the helper under `tools/`. G0-G2 are done (Next, above); the box is B1b.
