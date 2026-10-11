# CLAUDE.md

Guidance for Claude Code when working in this repository.

## What this is

Arbace is a Clojure derivative written entirely in Clojure. Studying prior art and vendoring
parts of it is always allowed. The work is a derivation done once by hand, so it must stay
reproducible from the record we keep.

Direction:
- Two states of the art are frozen, each on a well-known branch advertised in the README, which
  takes fixes only (tags `-v2`, `-v3`...):
  - the JVM: `arbace-for-java-26` (tag `arbace-for-java-26-v1`, 2026-10-08), Arbace on Java 26,
    binary compatible with it;
  - Go: `arbace-for-golang` (tag `arbace-for-go1.27.1-v1`, 2026-10-11), Arbace as one static
    executable for Linux, its runtime and the part of jdk26u it needs translated into Go.
- Now: the `.ae` rename, the Arbace dialect's tools (c2a, j2a, g2a), Arbace's own version, then
  the box: Arbace built with TamaGo, as a guest of go-whim's monitor (doc/AGENDA.md).
- Long term: Arbace is a standalone ecosystem, not binary compatible with Java. Java stays
  usable at the source level through j2a. The language is Clojure extended with an idiomatic
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
  tools take what they need of that tree as data (j2a's corpus, the baseline of Clojure's suite)
  from the tag, extracted into `.tmp/frozen/` by `bin/lib/tools.bash`.
- `bin/clojure-tests` runs Clojure's upstream test suite against a Clojure, by default the
  frozen baseline, and checks the result against `test/baseline-results.edn` (Arbace's:
  `test/arbace-results.edn`; the Go build's, `CLOJURE_TESTS_GO`: `test/arbace-go-results.edn`).
- `LICENSE.md` holds all licenses: their full texts and which files each covers. Licenses are
  stated nowhere else, apart from the notices source files came with, which stay. Extend it when
  vendoring from a new source.
- `arbace/` holds Arbace's own Clojure code, namespaces `arbace.*`, all `.ae` (Arbace's source
  extension: Clojure plus forms Clojure cannot evaluate; the loaders take `.ae`, then `.clj`):
  - the vendored Clojure, renamed from `clojure.*` (`arbace.core`, `arbace.lang`, `arbace.asm`
    and the rest; the Java parts are class forms). It is hand-maintained source; its
    derivation is recorded in `doc/VENDOR-NOTES.md` (replayable on `arbace-for-java-26`).
    `bin/build-arbace [--suite]` bootstraps it: stage 0 (the seed, which reads `.clj` only,
    through `target/seed-view`, `.clj` links to the `.ae` sources) builds `target/stage1`,
    which builds `target/stage2`, which must rebuild itself byte for byte (`target/stage3`). Each stage holds its namespaces AOT-compiled by
    its own runtime. Every class of stages 1 and 2 must pass the JDK's class file verifier
    (`arbace.classes.verify`). The build then makes `target/arbace.jar` (stage 2,
    reproducible) and the JDK AOT cache `target/arbace.aot` (training workload
    `test/aot-training.ae`). With `--suite` it also runs Clojure's test suite on stages 1
    and 2. Run Arbace with `bin/arbace-j` (the jar, with the cache when it applies), or a stage
    with `java -cp target/stageN:. arbace.lang.Main`. `bin/arbace-image` (or `bin/build-arbace
    --image`) builds `target/arbace-image`, a self-contained jlink image with its own AOT cache.
  - `arbace/classes/`: the class forms compiler (spec `doc/classes/SPEC.md`).
  - `arbace/c2a/`: the Clojure → Arbace translator (`doc/C2A.md`; `bin/c2a`, `bin/c2a-check`):
    the names of the vendored Clojure, `.clj`/`.cljc` into `.ae`, a table of rules for the
    dialects' divergences; `bin/clojure-tests` translates Clojure's suite with it.
  - `arbace/j2a/`: the Java → class forms converter.
  - `arbace/g2a/`: Go as Go forms and back (spec `doc/go/SPEC.md`; helpers `tools/godump`,
    `tools/gocmp`; `bin/g2a`).
  - `arbace/a2g/`: the class forms → Go forms translator (spec `doc/go/A2G-SPEC.md`; `bin/a2g`).
- The Go build (B1a, `doc/go/B1-PLAN.md`): Arbace as a static Go executable. a2g translates
  the runtime, with the Go-build variants of Clojure's classes and namespaces in
  `arbace/lang/go/`, and jrt, the Java runtime on Go (`go/arbace/jrt`, hand-written Go forms;
  `overlay/jdk/`, its own Java and variants of JDK classes; the jdk26u sources converted by
  `bin/jrt-convert`; `bin/jrt`); `overlay/go/` patches Go's runtime. `bin/arbace-go` builds
  (`--build`, `--build-tests`, `--pgo`), smoke-tests and runs it. The oracle (`bin/oracle`,
  `test/oracle/`, `doc/go/ORACLE.md`) checks it against the JVM Arbace. Notes in `doc/go/`.
- The box (B1b, `doc/go/BOX-SURVEY.md`, `doc/go/BOX-NOTES.md`): the same program built with
  TamaGo (`GOOS=tamago`) as a guest of a monitor on /dev/kvm (amd64) and Hypervisor.framework
  (arm64). `box/` holds the hypervisor API, the monitor, the doorbell ABI and the TamaGo board,
  vendored as Go forms from go-whim (`box/README.md` records the origin; `bin/box-vendor`
  reproduces and proves it); import paths `arbace/box/*`. `tamago/` holds the guest's own Go as Go
  forms (S6): TamaGo's runtime and standard library (`tamago/goroot/`, GOROOT/src's layout) and
  its `goos` module (`tamago/goos/`), for tamago/amd64, converted by `bin/tamago-vendor`
  (`convert`, `verify`; `check` proves the guest image built from them byte-identical to one
  built from the originals); the box's guest is built from them printed (`ARBACE_BOX_GO=orig`:
  from the toolchain's tree). `bin/arbace-go --build --box` builds the
  guest image and Arbace's launcher `arbace-box` (`--smoke --box`; `bin/box-suite`). The store (S3):
  blobs and refs in a host directory (`ARBACE_BOX_STORE`, `arbace-box --store DIR`, which is also
  its tool), the Go build's namespace `arbace.box` over it in the box and natively.
- `bin/gate` is the check every change to main must pass: the seed's hash, the bootstrap, then
  concurrently Clojure's suite on stage 2, `bin/class-forms-tests` and the Go executable's
  smoke test on amd64 (`bin/arbace-go --gate`; about 4 minutes). The smoke test runs on an
  executable cached in `.tmp/arbace-go-gate`, keyed by a hash of what it is built from
  (`arbace/`, `go/`, `overlay/`, the build's scripts, the seed, the toolchains): seconds when
  they are unchanged, else it is built again beside the suite (`bin/jrt-convert`'s conversion,
  a2g, the Go build, the image: the gate then takes about 7 minutes).
  `bin/gate --full` adds, concurrently, the suite on stage 1, `bin/j2a-check --suite`, the
  g2a round trip on amd64 for tamago and linux (`bin/g2a roundtrip amd64`, `linux/amd64`), the
  box's round trip and go-whim's KVM tests on it (`bin/box-vendor roundtrip`, `kvm-tests`),
  `tamago/` converted afresh and compared (`bin/tamago-vendor verify`) and
  the Go build on amd64: `bin/jrt-convert`, `bin/arbace-go --build`, then its smoke test,
  Clojure's suite on its test build (`bin/arbace-go --build-tests`: the program with the
  suite's Java fixtures; `test/arbace-go-results.edn`) and the oracle on it, against its known
  mismatches (`bin/oracle check ... --expected test/oracle/known-go-amd64.edn`) and the box's
  guest built from `tamago/`'s forms, byte-identical to one from TamaGo's originals
  (`bin/arbace-go --build --box`, `bin/tamago-vendor check`); g2a and the
  Go chain start when the three suites are done, for memory (about 21 minutes, Clojure's suite
  on Go the longest): run it when
  the class forms compiler, j2a, g2a, a2g or jrt change. Logs in `.tmp/gate/`. Every gate run and every `bin/arbace-go --build` first trims Go's build
  cache of entries unused for six hours (`bin/lib/gocache.bash`, `ARBACE_GOCACHE_KEEP_MIN`).

## Records

- `doc/AGENDA.md` holds the current state of the work. Keep it up to date: rewrite it freely so it
  always reflects where things stand and what comes next.
- `doc/JOURNAL.md` is append-only. Add a dated entry for every important decision or action: what
  was done, why, and what alternatives were considered. Name sources precisely, including
  repo, path and commit/tag for anything studied or vendored. Never edit or delete past entries.
  Correct them with a new entry. Write it so the progress could be recreated from it. At each
  freeze main's journal started over, by the user's decision: the history up to 2026-10-08 is
  on `arbace-for-java-26`, from then to 2026-10-11 on `arbace-for-golang`, and all of it in git.
- When vendoring code from prior art, record its origin (repo, path, revision) in the journal and
  next to the vendored code (for `arbace/`, which holds only `.ae` files: `doc/ARBACE.md`), and
  its license in `LICENSE.md`.

## Working conventions

- Data and source formats target the Clojure reader (`read`), not strict EDN; the two differ
  slightly, and where in doubt the Clojure reader wins.
- The translators name Arbace (`a`) as their target or source: c2a, j2a, g2a, a2g (until
  2026-10-11 j2c, g2c, c2g, as the frozen branches and dated records name them).
- The tools (c2a, j2a, the class forms checks, the suite runner) run on Arbace's
  `target/stage2` plus the checkout (`bin/lib/tools.bash`), so build first.
- Put temporary and scratch files in `.tmp/`, which is gitignored.
- Never create or modify anything under `.claude/`.
