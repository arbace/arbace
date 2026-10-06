# Agenda

The current state of the work. This file is rewritten as things change. For the history, see
[JOURNAL.md](JOURNAL.md).

## State

- The repository is bootstrapped with docs only: README.md, CLAUDE.md, doc/AGENDA.md and
  doc/JOURNAL.md.
- `clojure/` holds the frozen reference baseline: Clojure's Java and clj sources, with spec stubbed
  out, plus ASM repackaged as `clojure.asm`. It compiles with `javac -g` and the REPL works. The
  seed script is kept only in git history (commit `d21dc91`).
- `LICENSE.md` holds the vendored licenses: EPL-1.0 for Clojure and BSD-3-Clause for ASM.

## Next

- Decide the bootstrap strategy: which host runs Arbace before it can host itself.
- Decide the build tooling and project layout.
- Survey the prior art: clojure/clojure, openjdk/jdk26u and golang/go. Pin the revisions we study.

## Open questions

- Which native runtime will Arbace target first, and how much of a JVM-like or Go-like runtime will
  it reimplement?
