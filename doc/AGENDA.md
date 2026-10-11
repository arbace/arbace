# Agenda

The current state of the work. This file is rewritten as things change. For the history, see
[JOURNAL.md](JOURNAL.md).

## State

- Stage 0 of the bootstrap is the binary seed `seed/arbace-seed.jar` (below). The frozen
  reference baseline `clojure/` (upstream Clojure `98d735f` plus ASM as `clojure.asm`) is on the
  branch `arbace-for-java-26`.
- `LICENSE.md` holds all licenses and which files each covers: Arbace is EPL-1.0, like Clojure;
  ASM is BSD-3-Clause, Guava's Murmur3 Apache-2.0; the Go build's parts are under Go's
  BSD-3-Clause, jdk26u's GPLv2 with the Classpath Exception and the Unicode License V3.
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
  (`bin/arbace-j` since 2026-10-10; launch in about 0.2 s; `8c9f2d3`). Since 2026-10-08 the cache is trained without method
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
fallbacks of `arbace/core_classes.clj` are gone. (Before the Go checks were added; today's
gate: CLAUDE.md.)

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
  a port of `java.util.regex`, a first REPL without the class forms, `gen-class`, `proxy`
  (class forms and `proxy` since added; `gen-class` out for good, D6). B1a ends with a freeze
  (a branch and tag, as for Java 26; decided: branch `arbace-for-golang`, tag
  `arbace-for-go1.27.1-v1`) and includes `linux/arm64`, tested under `qemu-aarch64`.
  - Done, steps 0-5 (2026-10-08 to 2026-10-09): the round trip for linux/amd64 and arm64 and
    static executables (`bin/g2c build`); the Java surface measured
    ([go/JAVA-SURFACE.md](go/JAVA-SURFACE.md)), its seven decisions taken; the c2g spec
    ([go/C2G-SPEC.md](go/C2G-SPEC.md)), accepted with its 23 recommendations; jrt's JDK sources
    through j2c (`bin/jrt-convert`) and the JVM oracle (`bin/oracle`,
    [go/ORACLE.md](go/ORACLE.md)); jrt hand-written (core, reflection, threads and the host);
    c2g translating the runtime and the JDK closure; the evaluator
    ([go/EVAL-PLAN.md](go/EVAL-PLAN.md), [go/EVAL-NOTES.md](go/EVAL-NOTES.md)), `proxy` over
    `Dyn`, jrt's surface for the REPL, `bin/arbace-go`. The Go checks in `bin/gate --full`
    (jrt-convert, the Go build, its smoke test, Clojure's suite on it, the oracle against its
    known mismatches, `test/oracle/known-go-amd64.edn`). A multimethod's ambiguity message in
    name order (VENDOR-NOTES.md, hand change 14), on the JVM as on Go.
  - Done, step 6 (2026-10-09): the executable with an image of the prepared core namespaces
    ([go/EXEC-NOTES.md](go/EXEC-NOTES.md)); the essential `bin/gate` smoke-tests it, on an
    executable cached by a hash of its inputs (seconds on a hit; on a miss the Go build, about 5
    minutes, runs beside the suite and the whole gate takes about 7).
  - Done, step 7 (2026-10-10): 7a runtime speed (O1, closing D7, O2, O4, O6, O7; O3 dropped; O5:
    PGO executables at the freeze), 7b the evaluator's closure compilation
    ([go/SPEED-NOTES.md](go/SPEED-NOTES.md)); the evaluator resolves hints in the defining
    namespace.
  - Done, feature completion (2026-10-09 to 2026-10-10; D6 reversed where it had left a part
    out): `proxy`, fork-join, streams, dates, `Semaphore`; files (FS1-FS7); the suite's last failures
    (SL1-SL7; SL4 superseded by FX1, SL5 not taken); sockets and the socket REPL (NT1-NT5);
    regex resources, `\N{name}` and `CANON_EQ` (RD1-RD5); class forms at the REPL, `defclass`
    (CF1-CF6); java.util completed (JB1-JB5); the monitor race; java.time (TM1-TM9);
    java.util.concurrent (JC1-JC10; JC9 ends the agents-without-`shutdown-agents` deadlock);
    the executable trimmed 107.1 → 61.9 MB (SZ1-SZ4); its REPL under rlwrap (RL1-RL3,
    `ARBACE_RLWRAP`); processes (PR1-PR7); the suite's Java fixtures in a test build,
    `arbace-tests` (`bin/arbace-go --build-tests`), and resources by URL (FX1-FX7); java.security
    basics (SC1-SC7). Amendment sets M, X, S, Y, U, Z and EC (EC7 not taken) too. All accepted;
    folded into C2G-SPEC §16 and the notes, or to fold.
  - Done (2026-10-10), the JVM side: `bin/arbace` renamed `bin/arbace-j` (rlwrap on a terminal);
    images of `java.base` and `jdk.unsupported` (VENDOR-NOTES.md hand changes 15-17;
    `arbace-for-java-26-v3`, and main's `bin/arbace-image`); `reduce` without an init over an
    `IReduceInit`-only reducible, a dialect divergence (hand change 18; `-v4` with `bean`'s key
    order); the README brought up to date. Go's build cache trimmed by every `bin/gate` and
    `bin/arbace-go --build` (`bin/lib/gocache.bash`). `java.base` assessed
    ([go/JAVA-BASE.md](go/JAVA-BASE.md)); the box surveyed ([go/BOX-SURVEY.md](go/BOX-SURVEY.md)).
  - The user's choice for narrowing the gaps before the freeze (2026-10-10): the suite's Java
    fixtures, class loader resources, small oracle differences, processes, java.security
    basics, java.text with more CLDR locales: all done but java.text. After it: AOT of
    namespaces to Go (not scheduled); XML (SAX), `gen-class` and Java serialization out for
    good (D6), as Swing/AWT and loading classes from jars.
  - Numbers (2026-10-10, after java.security): the executable 65.1 MB on amd64, start about
    0.2 s; Clojure's suite on the test build 76 namespaces, 777 tests, 20,506 of 20,520
    assertions (11 failures and 3 errors recorded), 9 skipped, test.generative 27 of 27 (the
    JVM: 20,750 of 20,750); the Go oracle 22,111 of 22,118, the 7 recorded mismatches;
    java.base 52.0% of members (`bin/jrt-coverage`).
  - In progress: java.text (branch `gotext`; TL1-TL8 accepted, the default locale set none),
    merging. Then the documents' review fixed (`.tmp/docreview/`).
  - Then: step 8, the freeze (`bin/gate --full`, PGO executables (O5), the coverage
    regenerated, the branch `arbace-for-golang`, the tag `arbace-for-go1.27.1-v1`, the READMEs'
    "Arbace for Golang" sections: main's pointing to the branch, the branch's own; main's
    journal starts over, as at the JVM freeze).

- After the freeze (the user, 2026-10-10), in order:
  1. The `.ae` file extension (the user's decision, 2026-10-09; first after the freeze by the
     user's timing, 2026-10-10): Arbace's sources hold forms Clojure cannot evaluate (class
     forms, Go forms), so `.clj` misleads; they move to `.ae` (ASCII; `.æ` considered and not
     taken: hard to type and script). One self-contained change: loaders (`RT.load`, the
     compiler: `.ae` first, then `.clj`), the tools and their outputs (class forms build, j2c,
     g2c, c2g), `bin/`, `go/`, tests, the oracle, the docs; `bin/build-arbace` gives the frozen
     seed a `.clj` view of the sources for stage 1 (the seed stays as it is).
  2. c2a, a Clojure → Arbace translator: first the mechanical renames `clojure.*` → `arbace.*`
     that `arbace.j2c.rename` does today, later whatever the dialects' divergence needs
     (VENDOR-NOTES.md, "Dialect divergences"); the tools renamed to name Arbace as their
     target: j2c → j2a, g2c → g2a, and c2g → a2g if it is still needed.
  3. Arbace's own version: the REPL's banner says `Arbace <git describe> (<short hash>)`
     instead of `Clojure 1.13.0-master-SNAPSHOT`, with `*arbace-version*`/`(arbace-version)`
     holding the describe, the full commit and whether the tree was dirty (from `git describe
     --tags --always --dirty` in a checkout; from a file `git archive` fills through
     `export-subst` in a tarball); written as a constant into the Go program and a resource
     into the jar (stage 2 = stage 3 still holds); `*clojure-version*` stays, for libraries.
  4. TamaGo, B1b (the box): [go/BOX-SURVEY.md](go/BOX-SURVEY.md), stages S0-S6, its decisions
     D3, D8-D14 for the user.

## Later

- The standalone Arbace: no Java binary compatibility, with Java at the source level through j2c.
  The language is Clojure plus class forms, and later forms for Go. The goal is a
  self-sustaining REPL in a virtual sandbox, in Arbace down to the bare metal ISA, in the style
  of Go plus TamaGo. Targets: /dev/kvm on amd64 (Alpine edge Linux host) and
  Hypervisor.framework on arm64 (macOS host). Open: the runtime's design, Go-inspired (scheduler,
  GC), with proper tail calls possible.
- g2c (Go as Arbace forms): survey and plan in [G2C-SURVEY.md](G2C-SURVEY.md). The open
  questions are decided (journal on `arbace-for-java-26`, 2026-10-07): front end, spec and round trip (G0-G2), then the
  box via gc + TamaGo (B1). No Go libraries on the JVM. Target `GOOS=tamago` amd64/arm64,
  go1.27.1, with the helper under `tools/`. G0-G2 are done (Next, above); the box is B1b
  ([go/BOX-SURVEY.md](go/BOX-SURVEY.md)), after the freeze.
