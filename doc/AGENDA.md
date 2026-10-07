# Agenda

The current state of the work. This file is rewritten as things change. For the history, see
[JOURNAL.md](JOURNAL.md).

## State

- `clojure/` holds the frozen reference baseline: Clojure's Java and clj sources, with spec stubbed
  out, plus ASM repackaged as `clojure.asm`. It compiles with `javac -g` and the REPL works. The
  seed script is kept only in git history (commit `d21dc91`).
- `LICENSE.md` holds the vendored licenses: EPL-1.0 for Clojure and BSD-3-Clause for ASM.
- javalisp (`arbace/javalisp/`, spec in `doc/javalisp/SPEC.md`, files `.jls`) is the pathfinder: an
  exact, line-preserving Java ↔ s-expression transcription, verified on `clojure/` and all of
  jdk26u. It stays as a tool.

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

## Next

- Make the class forms native (SPEC §9.5): real `arbace.core` vars for the macros, and special
  forms in `arbace.lang.Compiler`, instead of `arbace.classes.boot` interning them at run time.
- Open decisions from step 4: whether to keep `arbace.clj` (the package file of the class
  `arbace.main`), and whether to rename the leftover `clojure` identifiers (thread names,
  `__clojureFnMap`, the temp-file prefix, `clojure-version`).
- Later: decide which native runtime Arbace targets, and how much of a JVM-like or Go-like
  runtime it reimplements.
