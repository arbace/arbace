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
   and Clojure's test suite passes on it as on the baseline. **Pending:** the user's decision on
   the 28 proposed spec amendments, which then go into SPEC.md.
4. Vendor `clojure/` as `arbace/`, renamed to `arbace.*` with `version.properties` folded or
   dropped, and with all of it in `.clj`. Self-host: stage 1 and stage 2, with stage 2
   reproducing itself.
- Later: decide which native runtime Arbace targets, and how much of a JVM-like or Go-like
  runtime it reimplements.
