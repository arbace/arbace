# Agenda

The current state of the work. This file is rewritten as things change. For the history, see
[JOURNAL.md](JOURNAL.md).

## State

- `clojure/` holds the frozen reference baseline: Clojure's Java and clj sources, with spec stubbed
  out, plus ASM repackaged as `clojure.asm`. It compiles with `javac -g` and the REPL works. The
  seed script is kept only in git history (commit `d21dc91`).
- `LICENSE.md` holds all licenses: Arbace is EPL-1.0, like Clojure; EPL-1.0 for Clojure, BSD-3-Clause for ASM, Apache-2.0 for
  Guava's Murmur3, and which files each covers.
- javalisp, the pathfinder (an exact Java ↔ s-expression transcription), was dropped once the
  class forms superseded it; it is in git history (last at `959d114`).

## Next: Java's classfile constructs as idiomatic Clojure

1. Spec: map every construct javac can emit to Clojure forms. **Drafted** as
   [classes/SPEC.md](classes/SPEC.md) ("class forms": `defclass` and its members, the new code
   forms, what the compiler derives as javac does, what the converter makes explicit, coverage
   tables, compilation model, REPL, worked examples). Reviewed: the user accepted all 17
   recommendations of §12.
2. Class forms compiler: **done** at stage 0 (`arbace/classes/`, `bin/class-forms-tests`,
   `doc/classes/COMPILER-NOTES.md`).
3. Converter: **done** (`arbace/j2c/`, `bin/j2c`, `bin/j2c-check`,
   `doc/classes/CONVERTER-NOTES.md`). The converted baseline compiles to javac's class shapes,
   and Clojure's test suite passes on it as on the baseline. The 28 spec amendments found while
   implementing (13 compiler, 15 converter) were accepted and are folded into SPEC.md. Follow-ups
   where the implementation lags the amended spec: constructor-call param-tags and
   `(anon Inner [args] :outer o ...)` in the compiler, `int` typing of all-literal conditionals
   under `long`/`float`/`double` operators, and the converter deciding pins with the compiler's
   resolution.
4. Vendor `clojure/` as `arbace/` and self-host: **done** (`af29cc6`, `doc/VENDOR-NOTES.md`).
   `arbace/` is all `.clj`. Stages 1, 2 and 3 are byte-identical, and Clojure's test suite
   (renamed) gives the baseline's result on stages 1 and 2.
5. Native class forms (SPEC §9.5): **done** (`0290f14`, `5081e57`). `defclass` and the code
   forms are `arbace.core` vars and `arbace.lang.Compiler` special forms, compiled by the one
   implementation `arbace.classes`; stage 1 needs no boot step. With it, the step 4 cleanups
   (`08e2167`): the main class is `arbace.lang.Main`, and runtime-visible `clojure` names are
   renamed.

6. Step 5's open ends: **done** (`367c83d`, `2dd9cba`, `2f2a5f2`). The §5.4 operators inline
   to instructions. `arbace.classes` is AOT-compiled into each stage, so the first class form
   costs 0.26 s, not about 1 s. Class forms work in `deftype`/`defrecord` methods, and
   `reify`/`deftype` methods are chosen by hints as in Clojure. The stages hold 2,150 classes,
   byte-identical.

## Next

- Decide on the survey of modern JVM features, [MODERN-COMPILER.md](MODERN-COMPILER.md). Its
  first recommendation is to AOT-compile all namespaces into the stages and use the JDK AOT
  cache: launch time measured 3.3 s → 0.45 s. Then add `ClassFile.verify` to the checks and
  emit classfile version 70.
- Short term: make Arbace excellent on the JVM.
  - Done: compiled namespaces in every stage, a reproducible jar, the JDK AOT cache and
    `bin/arbace`, which launches in about 0.2 s against about 2 s before (`8c9f2d3`).
  - Done: the JDK verifier checks every class of stages 1 and 2, and the Clojure compiler emits
    classfile version 70 (`e8a5e30`). All "do first" items of the survey are done.
  - In progress (user's decisions, each measured): condy constants, `invokedynamic` keyword
    sites and `StringConcatFactory` for `str`; `invokedynamic` reflective calls; VarHandles in
    `Atom`, an opt-in virtual-thread executor and a `jlink` image. ASM stays, and Var calls stay
    indirect.
- g2c (Go as Arbace forms): survey and plan in [G2C-SURVEY.md](G2C-SURVEY.md). The open
  questions are decided (journal, 2026-10-07): front end, spec and round trip (G0-G2), then the
  box via gc + TamaGo (B1). No Go libraries on the JVM. Target `GOOS=tamago` amd64/arm64,
  go1.27.1, with the helper under `tools/`. Not started: the JVM work comes first.

## Later

- Before breaking away from the JVM and the `.class` format: freeze the JVM state of the art on
  the branch `arbace-for-java-26` and advertise it in the docs.
- The standalone Arbace: no Java binary compatibility, with Java at the source level through j2c.
  The language is Clojure plus class forms, and later forms for Go. The goal is a
  self-sustaining REPL in a virtual sandbox, in Arbace down to the bare metal ISA, in the style
  of Go plus TamaGo. Targets: /dev/kvm on amd64 (Alpine edge Linux host) and
  Hypervisor.framework on arm64 (macOS host). Open: the runtime's design, Go-inspired (scheduler,
  GC), with proper tail calls possible.
