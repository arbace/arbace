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
- The box, Arbace built with TamaGo as a guest of go-whim's monitor, is two milestones on main
  (not freezes: no branch): on amd64 with /dev/kvm (tag `arbace-box-amd64-v1`, 2026-10-12) and
  on arm64 (tag `arbace-box-arm64-v1`, 2026-10-12): KVM/arm64, checked in an emulated aarch64
  VM, and Hypervisor.framework on an M2 Max (the bundle of `bin/box-mac-bundle`, 42 of 42).
- The standalone Arbace (doc/STANDALONE-SURVEY.md, D1-D13 decided), S1-S8 done, two milestones
  on main (tags, not freezes): the Go-hosted bootstrap (S1-S4): the seed is Arbace's Go program
  as Arbace printed it, the Go executable rebuilds itself, and the JVM stays only as the oracle's
  reference (the frozen v4 jar) and j2a's front end (`arbace-seed-is-go-v1`, 2026-10-13); the box
  rebuilding Arbace inside itself (S5-S8), its namespaces compiled to Go (S6), byte for byte on
  amd64, in the emulated arm64 VM and on the Mac (`arbace-inside-itself-v1`, 2026-10-15). Now:
  doc/AGENDA.md (the build's speed; S9, Go's toolchain as forms, later).
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

- `seed/arbace-seed-go.tar.xz` is the seed of the bootstrap: Arbace's Go program as the Go-hosted
  fixpoint printed it (a2g's translation printed by g2a, the module `go build` compiles, with Go's
  runtime as `overlay/go` patches it; since S6 the program with its namespaces compiled to Go,
  doc/STANDALONE-NOTES.md, "S6: the compiled build as the default"), one deterministic tar.xz pinned by
  `seed/arbace-seed-go.tar.xz.sha256`; the Go toolchain that builds it (TamaGo's go1.27.1) pinned
  by `seed/goroot.sha256` (doc/STANDALONE-NOTES.md, "S4: the Go-source seed", "S4: the JVM
  removed"). `bin/seed --check` checks the hash; `--build` builds stage 0 from it with Go's
  toolchain alone (`target/seed/<arch>/arbace`); `--make` packs a build (the fixpoint's G3, for a
  seed to commit, rarely: a re-seed); `--verify` prints it again from its commit (the fixpoint from
  the seed's own stage 0) and requires the same tar and stage 0 = G1 = G2 = G3. Until S4 the seed
  was a jar (`arbace-for-java-26-v1`'s), and before that the frozen reference tree `clojure/`
  (upstream Clojure and ASM): both on the branch `arbace-for-java-26`. The tools take what they
  need of that tree as data (j2a's corpus, the baseline of Clojure's suite) from the tag,
  extracted into `.tmp/frozen/` by `bin/lib/tools.bash`.
- Stage 0 hosts Arbace's tools (`bin/lib/host.bash`'s `arbace_host_exe`: `ARBACE_HOST_EXE`, else
  stage 0, built when missing or when the seed changed): a2g, g2a, c2a, the suite's runner,
  compiled into it (S6, SA133), a tool's namespace whose source in the checkout differs from the
  embedded copy loaded from the checkout instead, evaluated (`bin/lib/tools.bash`'s
  `arbace_tool`, `ARBACE_PATH`). The JVM runs only the frozen jar of `arbace-for-java-26-v4` (built from its tag,
  pinned by `test/oracle/frozen-jar.sha256`; `bin/lib/frozen.bash`, its tools' namespaces from the
  checkout as `.clj` links before the jar): the oracle's reference, j2a (javac's front end:
  `bin/j2a`, `j2a-check`, `jrt-convert`, `--java-fixtures`), a2g's edge (`bin/a2g-edge`) and the
  data generators (`bin/jrt testdata|manifest|standins`, `jrt-coverage`).
- `bin/clojure-tests` runs Clojure's upstream test suite against a Clojure, by default the
  frozen baseline on the JVM (`test/baseline-results.edn`), or with `CLOJURE_TESTS_GO` the Go
  build's test executable (`test/arbace-go-results.edn`; c2a's translation and the report on the
  tools' host).
- `LICENSE.md` holds all licenses: their full texts and which files each covers. Licenses are
  stated nowhere else, apart from the notices source files came with, which stay. Extend it when
  vendoring from a new source.
- `arbace/` holds Arbace's own Clojure code, namespaces `arbace.*`, all `.ae` (Arbace's source
  extension: Clojure plus forms Clojure cannot evaluate; the loaders take `.ae`, then `.clj`):
  - the vendored Clojure, renamed from `clojure.*` (`arbace.core`, `arbace.lang` and the rest;
    the Java parts are class forms). It is hand-maintained source; its derivation is recorded in
    `doc/VENDOR-NOTES.md` (replayable on `arbace-for-java-26`). Since S4 it has no bytecode back
    end, no ASM and no class loader (on `arbace-for-java-26`, with the JVM's three-stage
    bootstrap, the jar, its AOT cache, the jlink image and `bin/arbace-j`): the Go build
    translates it. The Go build's variants of `RT`, `Main` and `Compiler` are merged into their
    bases (SA150); `arbace/lang/go/` keeps the generated ones (`CompilerOps.ae`,
    `CompilerAOTFn.ae`, regenerated by `test/a2g/eval_ops.ae`, `test/aot/gen_fns.ae`). The divergences this makes
    (gen-class a no-op, no class files, ...) are recorded in doc/VENDOR-NOTES.md and
    doc/STANDALONE-NOTES.md (D13).
  - `arbace/classes/`: the class forms' front end (spec `doc/classes/SPEC.md`): parse, lower,
    analyze, env, types, native, the access flags (`opcodes`), `go`, the interpreter (`interp`);
    `jvm`, env's class-file reading, only for the frozen jar.
  - `arbace/aot/`: the back end compiling namespaces to class forms (standalone S6,
    doc/STANDALONE-NOTES.md, "S6: Arbace compiled to Go"): the analyzed units of a program's
    prepare run, `Compiler`'s `Expr` trees, become classes a2g translates like the runtime's.
  - `arbace/c2a/`: the Clojure → Arbace translator (`doc/C2A.md`; `bin/c2a`, `bin/c2a-check`):
    the names of the vendored Clojure, `.clj`/`.cljc` into `.ae`, a table of rules for the
    dialects' divergences; `bin/clojure-tests` translates Clojure's suite with it.
  - `arbace/j2a/`: the Java → class forms converter.
  - `arbace/g2a/`: Go as Go forms and back (spec `doc/go/SPEC.md`; helpers `tools/godump`,
    `tools/gocmp`; `bin/g2a`), on the tools' host (`G2A_GO_EXE` names another executable).
  - `arbace/a2g/`: the class forms → Go forms translator (spec `doc/go/A2G-SPEC.md`; `bin/a2g`),
    on the tools' host (`A2G_GO_EXE`), its classes translated in parallel (`A2G_JOBS`, default at
    most 16) with the sequential output byte for byte; the resources compressed by
    `arbace/a2g/zlib.ae` (SA21).
- The Go build (B1a, `doc/go/B1-PLAN.md`): Arbace as a static Go executable. a2g translates
  the runtime (with the generated Go-build variants in `arbace/lang/go/`), and jrt, the Java runtime on Go (`go/arbace/jrt`, hand-written Go forms;
  `overlay/jdk/`, its own Java and variants of JDK classes; `jdk/`, the JDK's class forms and
  resource data committed (j2a's output from jdk26u and `overlay/jdk/java.base`, provenance in
  `jdk/provenance.edn`; `bin/jrt-convert update` regenerates them, `verify` checks them; standalone
  S1); `jdk/edge.edn`, the JVM's answers a2g needs, so a2g uses no JVM reflection or ASM
  (`bin/a2g-edge update`, `verify`, `trace`; S2, doc/STANDALONE-NOTES.md); `bin/jrt`); `overlay/go/` patches Go's runtime. `bin/arbace-go` builds
  (`--build`, `--build-tests`, `--pgo`, by the tools' host; the test build's Java
  fixtures committed as forms in `test/java-fixtures/`, `bin/clojure-tests --java-fixtures update`,
  `verify`), smoke-tests and runs it. Its namespaces are compiled to Go (`ARBACE_AOT=1`, the
  default since S6; `ARBACE_AOT=0`: evaluated from the image of prepared namespaces, as before):
  by the host when its runtime (`bin/lib/version.bash`'s `arbace_runtime_key`: `arbace/lang`,
  `go`, `overlay/jdk`, `jdk`) is the checkout's, one a2g run, about 5 minutes; else by a first
  build of the program, two runs, about 10 minutes (after a change to the runtime). The test
  build reuses the shipped build's compiled namespaces when made from the same sources and
  runtime (`target/arbace-go/aot.key`); g2a keeps a cache of printed packages
  (`--print-cache`, `.tmp/g2a-print-cache`); `--fixpoint` builds G1 by the host, G2 by G1, G3 by G2 into
  `target/arbace-go/fixpoint/` and requires G2 = G3. The oracle (`bin/oracle`, `test/oracle/`,
  `doc/go/ORACLE.md`) checks it against the frozen JVM Arbace (the v4 jar's expected results,
  `test/oracle/expected`), against its known mismatches `test/oracle/known-go-amd64-frozen.edn`.
  Notes in `doc/go/`.
- The box (B1b, `doc/go/BOX-SURVEY.md`, `doc/go/BOX-NOTES.md`): the same program built with
  TamaGo (`GOOS=tamago`) as a guest of a monitor on /dev/kvm (amd64) and Hypervisor.framework
  (arm64; KVM/arm64 too). `box/` holds the hypervisor API, the monitor, the doorbell ABI and the
  TamaGo board, vendored as Go forms from go-whim, and purego for the launcher on darwin/arm64
  (`box/README.md` records the origins; `bin/box-vendor` reproduces and proves them); import
  paths `arbace/box/*`. `tamago/` holds the guest's own Go as Go
  forms (S6): TamaGo's runtime and standard library (`tamago/goroot/`, GOROOT/src's layout) and
  its `goos` module (`tamago/goos/`), for tamago/amd64 and tamago/arm64 as one tree, converted by `bin/tamago-vendor`
  (`convert`, `verify`; `check` proves the guest image built from them byte-identical to one
  built from the originals); the box's guest is built from them printed (`ARBACE_BOX_GO=orig`:
  from the toolchain's tree). `bin/arbace-go --build --box` builds the
  guest image and Arbace's launcher `arbace-box` (`--smoke --box`, which runs `bin/box-smoke`;
  `bin/box-suite`; arm64 with `ARBACE_BOX_ARCHES`). `bin/box-arm64-vm` runs an emulated aarch64
  VM with KVM/arm64 for the arm64 box's checks (`check`: its smoke test, go-whim's tests);
  `bin/box-mac-bundle` packs the Mac's launcher, guest and checklist (`box/mac/`). The store (S3):
  blobs and refs in a host directory (`ARBACE_BOX_STORE`, `arbace-box --store DIR`, which is also
  its tool), the namespace `arbace.box` over it in the box and natively.
- `doc/ALPINE.md`: the host's Alpine packages as one minimal `apk add` line (`bin/alpine-env
  world`, `check`, `simulate`), the subset Arbace uses, and what apk does not provide (jdk26u,
  TamaGo's Go, go-whim, Go's module cache).
- `bin/gate` is the check every change to main must pass: the seed's hash, stage 0 built from
  it (`bin/seed --build`), the executable and the test build by stage 0 (`bin/arbace-go
  --gate-build`, cached in `.tmp/arbace-go-gate` and keyed by a hash of what they are built from:
  `arbace/`, `go/`, `overlay/`, `jdk/`, `x509roots/`, the fixtures' forms, the build's scripts, the
  seed's and Go's pins; seconds when unchanged, else two Go-hosted builds, about 8 minutes: the
  test build reuses the executable's compiled namespaces), then
  concurrently the smoke test, Clojure's suite on the test build (`test/arbace-go-results.edn`) and
  the oracle against the frozen reference. `bin/gate --full` adds, concurrently: on the frozen
  JVM `bin/j2a-check --suite`, `bin/jrt-convert verify` (`jdk/` converted afresh, byte for byte),
  `bin/a2g-edge verify`, `bin/clojure-tests --java-fixtures verify`; the Go build afresh with its
  smoke test, the box's guests from `tamago/`'s forms byte-identical to TamaGo's originals'
  (`bin/arbace-go --build --box`, `bin/tamago-vendor check`) and the box's smoke test, then
  `bin/arbace-go --fixpoint`; g2a's round trips (`bin/g2a roundtrip amd64`, `linux/amd64`), the
  box's vendoring checks (`bin/box-vendor roundtrip`, `kvm-tests`, `purego-check`),
  `bin/tamago-vendor verify`, `bin/x509roots-vendor verify`, `check`. Run it when the class forms'
  front end, j2a, g2a, a2g or jrt change (after a change to `overlay/jdk/java.base`, jrt's closure
  or j2a, `bin/jrt-convert update` first; after a change to what a2g asks of the JVM,
  `bin/a2g-edge update`). Logs in `.tmp/gate/`. Every gate run and every `bin/arbace-go --build`
  first trims Go's build cache of entries unused for six hours (`bin/lib/gocache.bash`,
  `ARBACE_GOCACHE_KEEP_MIN`). No build may use the host's `go`: only TamaGo's (`G2A_GOROOT`).

## Records

- `doc/AGENDA.md` holds the current state of the work. Keep it up to date: rewrite it freely so it
  always reflects where things stand and what comes next.
- `doc/JOURNAL.md` is append-only. Add a dated entry for every important decision or action: what
  was done, why, and what alternatives were considered. Name sources precisely, including
  repo, path and commit/tag for anything studied or vendored. Never edit or delete past entries.
  Correct them with a new entry. Write it so the progress could be recreated from it. At each
  freeze, and after the milestone `arbace-inside-itself-v1`, main's journal started over, by the
  user's decision: the history up to 2026-10-08 is on `arbace-for-java-26`, from then to
  2026-10-11 on `arbace-for-golang`, from then to 2026-10-15 at the tag
  `arbace-inside-itself-v1`, and all of it in git.
- When vendoring code from prior art, record its origin (repo, path, revision) in the journal and
  next to the vendored code (for `arbace/`, which holds only `.ae` files: `doc/ARBACE.md`), and
  its license in `LICENSE.md`.

## Working conventions

- Data and source formats target the Clojure reader (`read`), not strict EDN; the two differ
  slightly, and where in doubt the Clojure reader wins.
- The translators name Arbace (`a`) as their target or source: c2a, j2a, g2a, a2g (until
  2026-10-11 j2c, g2c, c2g, as the frozen branches and dated records name them).
- The tools (c2a, a2g, g2a, the suite runner) run on the tools' Go host, stage 0 by default
  (`bin/lib/host.bash`, `bin/lib/tools.bash`); j2a and the JVM's records on the frozen jar
  (`bin/lib/frozen.bash`). Nothing builds or runs a JVM Arbace of the checkout.
- Put temporary and scratch files in `.tmp/`, which is gitignored.
- Never create or modify anything under `.claude/`.
