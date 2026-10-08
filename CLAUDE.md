# CLAUDE.md

Guidance for Claude Code when working in this repository.

## What this is

Arbace is a Clojure derivative written entirely in Clojure. Studying prior art and vendoring
parts of it is always allowed. The work is a derivation done once by hand, so it must stay
reproducible from the record we keep.

Direction:
- Short term: Arbace is excellent on the modern JVM (Java 26), binary compatible with it.
- Before breaking away from the JVM and the `.class` format, the JVM state of the art is frozen
  on a well-known branch, `arbace-for-java-26`, advertised in the docs.
- Long term: Arbace is a standalone ecosystem, not binary compatible with Java. Java stays
  usable at the source level through j2c. The language is Clojure extended with an idiomatic
  representation of Java (the class forms), and later of Go: the Arbace language. The goal is a
  self-sustaining REPL inside a virtual sandbox, written in Arbace all the way down to the bare
  metal ISA, in the way of Go plus TamaGo. Targets: /dev/kvm on amd64 (Alpine edge Linux host),
  and Hypervisor.framework on arm64 (macOS host).

Prior art:
- https://github.com/clojure/clojure covers the language.
- https://github.com/openjdk/jdk26u covers the JVM runtime, the near-term target.
- https://github.com/golang/go is the model for a slimmer runtime of Arbace's own, and later a
  language to project into Arbace, as Java was.
- https://github.com/usbarmory/tamago: Go on bare metal, the model for running in a sandbox
  without an OS.
- https://github.com/candid82/joker: a Clojure dialect implemented in Go.

## Layout

- `clojure/` is the frozen reference baseline: upstream Clojure and ASM sources as seeded by
  `SEED.bash` (now only in git history, commit `d21dc91`). Its compiled classes are tracked too,
  built in place with `javac -g $(find clojure -name '*.java')`; rebuild them after a fix.
  Don't modify it, except for bug fixes the user approves, each recorded in the journal. The
  rewrite is derived from it but lives elsewhere. After the freeze, main replaces it with a
  binary seed (`doc/AGENDA.md`, "Later").
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
    itself byte for byte (`target/stage3`). Each stage holds its namespaces AOT-compiled by
    its own runtime. Every class of stages 1 and 2 must pass the JDK's class file verifier
    (`arbace.classes.verify`). The build then makes `target/arbace.jar` (stage 2,
    reproducible) and the JDK AOT cache `target/arbace.aot` (training workload
    `test/aot-training.clj`). With `--suite` it also runs Clojure's test suite on stages 1
    and 2. Run Arbace with `bin/arbace` (the jar, with the cache when it applies), or a stage
    with `java -cp target/stageN:. arbace.lang.Main`. `bin/arbace-image` (or `bin/build-arbace
    --image`) builds `target/arbace-image`, a self-contained jlink image with its own AOT cache.
  - `arbace/classes/`: the class forms compiler (spec `doc/classes/SPEC.md`).
  - `arbace/j2c/`: the Java → class forms converter.

## Records

- `doc/AGENDA.md` holds the current state of the work. Keep it up to date: rewrite it freely so it
  always reflects where things stand and what comes next.
- `doc/JOURNAL.md` is append-only. Add a dated entry for every important decision or action: what
  was done, why, and what alternatives were considered. Name sources precisely, including
  repo, path and commit/tag for anything studied or vendored. Never edit or delete past entries.
  Correct them with a new entry. Write it so the progress could be recreated from it. The one
  exception, decided by the user: after the freeze, main's journal starts over, the history
  up to it staying in git and on `arbace-for-java-26`.
- When vendoring code from prior art, record its origin (repo, path, revision) in the journal and
  next to the vendored code (for `arbace/`, which holds only `.clj` files: `doc/ARBACE.md`), and
  its license in `LICENSE.md`.

## Working conventions

- Data and source formats target the Clojure reader (`read`), not strict EDN; the two differ
  slightly, and where in doubt the Clojure reader wins.
- In a fresh git worktree, run `find clojure -name '*.class' -exec touch {} +` first: checkout
  leaves the `.java` files newer than their tracked classes, and the checks then recompile them.
- Put temporary and scratch files in `.tmp/`, which is gitignored.
- Never create or modify anything under `.claude/`.
