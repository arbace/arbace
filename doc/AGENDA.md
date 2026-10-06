# Agenda

The current state of the work. This file is rewritten as things change. For the history, see
[JOURNAL.md](JOURNAL.md).

## State

- `clojure/` holds the frozen reference baseline: Clojure's Java and clj sources, with spec stubbed
  out, plus ASM repackaged as `clojure.asm`. It compiles with `javac -g` and the REPL works. The
  seed script is kept only in git history (commit `d21dc91`).
- `LICENSE.md` holds the vendored licenses: EPL-1.0 for Clojure and BSD-3-Clause for ASM.
- javalisp (`arbace/javalisp/`, spec in `doc/javalisp/SPEC.md`) transcribes Java 26 to
  Clojure-readable s-expressions and back.
  - Java → javalisp → Java gives the identical javac tree, lines included, for all 183 files
    of `clojure/`, all 14,202 files of jdk26u `src/`, and every javac-accepted file of jdk26u
    `test/` except two known limits.
  - Classes compiled from round-tripped sources are byte-identical. That covers all 812 classes
    of `clojure/`, and 27,987 classes from 12,411 test files that compile on their own.

## Next

- Vendor javac and give it a second parser that reads javalisp straight into its tree, so
  `.clj` sources compile to the same class files as the `.java` they came from.
- Decide the bootstrap strategy: which host runs Arbace before it can host itself.
- Decide the build tooling and project layout.
- Survey the prior art: clojure/clojure, openjdk/jdk26u and golang/go. Pin the revisions we study.

## Open questions

- Which native runtime will Arbace target first, and how much of a JVM-like or Go-like runtime will
  it reimplement?
- javalisp's known limits (comment delimiters written as unicode escapes, stray `;` around doc
  comments): fix them or accept them?
