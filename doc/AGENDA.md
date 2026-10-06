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

1. Spec: map every construct javac can emit to Clojure forms. That covers class, interface, enum,
   record, annotation and sealed types; nested, inner, local and anonymous classes; fields,
   constructors, initializers and every method kind; generic signatures and annotations; mutable
   locals and control flow (with `return`, `break` and `continue`); exact primitive arithmetic;
   switch and patterns; exceptions, `synchronized`, lambdas and method references. Review it with
   the user before any code.
2. Bootstrap compiler for the new forms, running on the frozen `clojure/` (stage 0) and emitting
   bytecode with `clojure.asm`, with tests per construct, including defining classes at the REPL.
3. Converter (Java → Clojure) on javac's attributed trees. Convert the 183 baseline files and
   check them for equivalence: Clojure's upstream test suite, and class shapes against javac's.
4. Vendor `clojure/` as `arbace/`, renamed to `arbace.*` with `version.properties` folded or
   dropped, and with all of it in `.clj`. Self-host: stage 1 and stage 2, with stage 2
   reproducing itself.
- Later: decide which native runtime Arbace targets, and how much of a JVM-like or Go-like
  runtime it reimplements.
