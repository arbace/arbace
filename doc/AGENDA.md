# Agenda

The current state of the work. This file is rewritten as things change. For the history, see
[JOURNAL.md](JOURNAL.md).

## State

- `clojure/` holds the frozen reference baseline: upstream Clojure (`98d735f`) with spec stubbed
  out, plus ASM repackaged as `clojure.asm`. It compiles with `javac -g` and is stage 0 of the
  bootstrap. The seed script is kept only in git history (commit `d21dc91`).
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
     `apply str` 1.16x. The GitHub Actions gate (`.github/workflows/gate.yml`, only on pushes
     to `arbace-for-java-26` and by hand; main is gated by running the checks locally). The
     freeze kit ([FREEZE.md](FREEZE.md), `bin/freeze`).

## Done: the `arbace-for-java-26` freeze (2026-10-08)

The branch `arbace-for-java-26` and the tag `arbace-for-java-26-v1` hold Arbace on the JVM, at
main `38a652d`, after the gate passed on it ([FREEZE.md](FREEZE.md)). The repository is public.
The journal up to the freeze is on that branch; main's [JOURNAL.md](JOURNAL.md) starts over.

## Later

- Next: `clojure/` remains on `arbace-for-java-26` for good, so that branch builds and
  verifies from its own checkout. On main it is replaced by a
  Go-style binary seed (the user's decision, 2026-10-08): stage 0 becomes the jar built from the
  freeze tag, pinned by hash. Its other roles (j2c's regression corpus, the suite's reference
  results, the replay of the derivation) move to the frozen branch or are re-anchored on the
  seed.
- The standalone Arbace: no Java binary compatibility, with Java at the source level through j2c.
  The language is Clojure plus class forms, and later forms for Go. The goal is a
  self-sustaining REPL in a virtual sandbox, in Arbace down to the bare metal ISA, in the style
  of Go plus TamaGo. Targets: /dev/kvm on amd64 (Alpine edge Linux host) and
  Hypervisor.framework on arm64 (macOS host). Open: the runtime's design, Go-inspired (scheduler,
  GC), with proper tail calls possible.
- g2c (Go as Arbace forms): survey and plan in [G2C-SURVEY.md](G2C-SURVEY.md). The open
  questions are decided (journal, 2026-10-07): front end, spec and round trip (G0-G2), then the
  box via gc + TamaGo (B1). No Go libraries on the JVM. Target `GOOS=tamago` amd64/arm64,
  go1.27.1, with the helper under `tools/`. Not started: the JVM work comes first.
