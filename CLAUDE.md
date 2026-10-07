# CLAUDE.md

Guidance for Claude Code when working in this repository.

## What this is

Arbace is a from-scratch reimplementation of Clojure in Clojure. Studying prior art and vendoring
parts of it is always allowed. The work is a derivation done once by hand, so it must stay
reproducible from the record we keep.

Prior art:
- https://github.com/clojure/clojure covers the language.
- https://github.com/openjdk/jdk26u covers the native runtime.
- https://github.com/golang/go is a model for a slimmer runtime and anything else useful from it.
  The Go language itself is out of scope.

## Layout

- `clojure/` is the frozen reference baseline: upstream Clojure and ASM sources as seeded by
  `SEED.bash` (now only in git history, commit `d21dc91`). Its compiled classes are tracked too,
  built in place with `javac -g $(find clojure -name '*.java')`; rebuild them after a fix. Don't modify it, except for bug
  fixes the user approves, each recorded in the journal. The rewrite is derived from it but
  lives elsewhere.
- `bin/clojure-tests` runs Clojure's upstream test suite against a Clojure, by default the
  baseline, and checks the result against `test/baseline-results.edn`.
- `LICENSE.md` holds all licenses: their full texts and which files each covers. Licenses are
  stated nowhere else, apart from the notices source files came with, which stay. Extend it when
  vendoring from a new source.
- `arbace/` holds Arbace's own Clojure code, namespaces `arbace.*`, all `.clj`:
  - the vendored Clojure, renamed from `clojure.*` (`arbace.core`, `arbace.lang`, `arbace.asm`
    and the rest; the Java parts are class forms). It is now hand-maintained source; its
    derivation is recorded in `doc/VENDOR-NOTES.md` and replayable with `bin/vendor-arbace`.
    `bin/build-arbace [--suite]` bootstraps it: stage 0 (the frozen `clojure/` plus
    `arbace.classes`) builds `target/stage1`, which builds `target/stage2`, which must rebuild
    itself byte for byte (`target/stage3`). With `--suite` it also runs Clojure's test suite
    on stages 1 and 2. Run a stage with `java -cp target/stageN:. arbace.lang.Main`.
  - `arbace/classes/`: the class forms compiler (spec `doc/classes/SPEC.md`).
  - `arbace/j2c/`: the Java → class forms converter.

## Records

- `doc/AGENDA.md` holds the current state of the work. Keep it up to date: rewrite it freely so it
  always reflects where things stand and what comes next.
- `doc/JOURNAL.md` is append-only. Add a dated entry for every important decision or action: what
  was done, why, and what alternatives were considered. Name sources precisely, including
  repo, path and commit/tag for anything studied or vendored. Never edit or delete past entries.
  Correct them with a new entry. Write it so the progress could be recreated from it.
- When vendoring code from prior art, record its origin (repo, path, revision) in the journal and
  next to the vendored code (for `arbace/`, which holds only `.clj` files: `doc/ARBACE.md`), and
  its license in `LICENSE.md`.

## Working conventions

- Put temporary and scratch files in `.tmp/`, which is gitignored.
- Never create or modify anything under `.claude/`.
