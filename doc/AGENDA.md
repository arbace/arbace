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
- Decide which native runtime Arbace targets, and how much of a JVM-like or Go-like runtime it
  reimplements. One input: Java 26 has no tail calls, while an own runtime could have proper
  ones.
