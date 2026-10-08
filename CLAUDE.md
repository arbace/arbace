# CLAUDE.md

Guidance for Claude Code when working in this repository.

## What this is

Arbace is a Clojure derivative written entirely in Clojure. Studying prior art and vendoring
parts of it is always allowed. The work is a derivation done once by hand, so it must stay
reproducible from the record we keep.

Direction:
- Short term: Arbace is excellent on the modern JVM (Java 26), binary compatible with it.
- Before breaking away from the JVM and the `.class` format, the JVM state of the art was frozen
  on the branch `arbace-for-java-26` (tag `arbace-for-java-26-v1`, 2026-10-08), advertised in
  the README.
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

- `seed/arbace-seed.jar` is the binary seed of the bootstrap: Arbace's jar as built at the tag
  `arbace-for-java-26-v1`, pinned by `seed/arbace-seed.jar.sha256`. `bin/seed --check` checks
  the hash; `bin/seed --verify` rebuilds it from the tag. It replaced the frozen reference tree
  `clojure/` (upstream Clojure and ASM), which remains on the branch `arbace-for-java-26`. The
  tools take what they need of that tree as data (j2c's corpus, the baseline of Clojure's suite)
  from the tag, extracted into `.tmp/frozen/` by `bin/lib/tools.bash`.
- `bin/clojure-tests` runs Clojure's upstream test suite against a Clojure, by default the
  frozen baseline, and checks the result against `test/baseline-results.edn` (Arbace's:
  `test/arbace-results.edn`).
- `LICENSE.md` holds all licenses: their full texts and which files each covers. Licenses are
  stated nowhere else, apart from the notices source files came with, which stay. Extend it when
  vendoring from a new source.
- `arbace/` holds Arbace's own Clojure code, namespaces `arbace.*`, all `.clj`:
  - the vendored Clojure, renamed from `clojure.*` (`arbace.core`, `arbace.lang`, `arbace.asm`
    and the rest; the Java parts are class forms). It is hand-maintained source; its
    derivation is recorded in `doc/VENDOR-NOTES.md` (replayable on `arbace-for-java-26`).
    `bin/build-arbace [--suite]` bootstraps it: stage 0 (the seed) builds `target/stage1`,
    which builds `target/stage2`, which must rebuild itself byte for byte (`target/stage3`). Each stage holds its namespaces AOT-compiled by
    its own runtime. Every class of stages 1 and 2 must pass the JDK's class file verifier
    (`arbace.classes.verify`). The build then makes `target/arbace.jar` (stage 2,
    reproducible) and the JDK AOT cache `target/arbace.aot` (training workload
    `test/aot-training.clj`). With `--suite` it also runs Clojure's test suite on stages 1
    and 2. Run Arbace with `bin/arbace` (the jar, with the cache when it applies), or a stage
    with `java -cp target/stageN:. arbace.lang.Main`. `bin/arbace-image` (or `bin/build-arbace
    --image`) builds `target/arbace-image`, a self-contained jlink image with its own AOT cache.
  - `arbace/classes/`: the class forms compiler (spec `doc/classes/SPEC.md`).
- `bin/gate` is the check every change to main must pass: the seed's hash, the bootstrap, then
  concurrently Clojure's suite on stage 2 and `bin/class-forms-tests` (about 4 minutes).
  `bin/gate --full` adds, concurrently, the suite on stage 1, `bin/j2c-check --suite` and the
  g2c round trip on amd64 (`bin/g2c roundtrip amd64`) (about 7 minutes): run it when the
  class forms compiler, j2c or g2c change. Logs in
  `.tmp/gate/`.
  - `arbace/j2c/`: the Java → class forms converter.

## Records

- `doc/AGENDA.md` holds the current state of the work. Keep it up to date: rewrite it freely so it
  always reflects where things stand and what comes next.
- `doc/JOURNAL.md` is append-only. Add a dated entry for every important decision or action: what
  was done, why, and what alternatives were considered. Name sources precisely, including
  repo, path and commit/tag for anything studied or vendored. Never edit or delete past entries.
  Correct them with a new entry. Write it so the progress could be recreated from it. At the
  freeze (2026-10-08) main's journal started over, by the user's decision; the history up to
  it is on `arbace-for-java-26` and in git.
- When vendoring code from prior art, record its origin (repo, path, revision) in the journal and
  next to the vendored code (for `arbace/`, which holds only `.clj` files: `doc/ARBACE.md`), and
  its license in `LICENSE.md`.

## Working conventions

- Data and source formats target the Clojure reader (`read`), not strict EDN; the two differ
  slightly, and where in doubt the Clojure reader wins.
- The tools (j2c, the class forms checks, the suite runner, the bench driver) run on Arbace's
  `target/stage2` plus the checkout (`bin/lib/tools.bash`), so build first.
- Put temporary and scratch files in `.tmp/`, which is gitignored.
- Never create or modify anything under `.claude/`.
