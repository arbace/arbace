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
- The box, Arbace built with TamaGo as a guest of its own monitor (vendored from go-whim, a pilot
  Arbace no longer depends on), is two milestones on main
  (not freezes: no branch): on amd64 with /dev/kvm (tag `arbace-box-amd64-v1`, 2026-10-12) and
  on arm64 (tag `arbace-box-arm64-v1`, 2026-10-12): KVM/arm64, checked in an emulated aarch64
  VM, and Hypervisor.framework on an M2 Max (the bundle of `bin/box-mac-bundle`, 42 of 42).
- The standalone Arbace (doc/STANDALONE-SURVEY.md, D1-D13 decided), S1-S9 done, two milestones
  on main (tags, not freezes): the Go-hosted bootstrap (S1-S4): the seed is Arbace's Go program
  as Arbace printed it, the Go executable rebuilds itself, and the JVM stays only as the oracle's
  reference (the frozen v4 jar) and repr/java's front end (`arbace-seed-is-go-v1`, 2026-10-13); the box
  rebuilding Arbace inside itself (S5-S8), its namespaces compiled to Go (S6), byte for byte on
  amd64, in the emulated arm64 VM and on the Mac (`arbace-inside-itself-v1`, 2026-10-15); then S9, Go's
  toolchain as forms (`tamago/cmd/`, 2026-10-15). Now: doc/AGENDA.md.
- Long term: Arbace is a standalone ecosystem, not binary compatible with Java. Java stays
  usable at the source level through repr/java. The language is Clojure extended with an idiomatic
  representation of Java (the Java forms), and later of Go: the Arbace language. The goal is a
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
  fixpoint printed it (j2g's translation printed by repr/go, the module `go build` compiles, with Go's
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
  need of that tree as data (repr/java's corpus, the baseline of Clojure's suite) from the tag,
  extracted into `.tmp/frozen/` by `bin/lib/tools.bash`.
- Stage 0 hosts Arbace's tools (`bin/lib/host.bash`'s `arbace_host_exe`: `ARBACE_HOST_EXE`, else
  stage 0, built when missing or when the seed changed): c2j, j2g, repr/go, repr/clojure, the suite's runner,
  compiled into it (S6, SA133), a tool's namespace whose source in the checkout differs from the
  embedded copy loaded from the checkout instead, evaluated (`bin/lib/tools.bash`'s
  `arbace_tool`, `ARBACE_PATH`). The JVM runs only the frozen jar of `arbace-for-java-26-v4` (built from its tag,
  pinned by `test/oracle/frozen-jar.sha256`; `bin/lib/frozen.bash`, its tools' namespaces from the
  checkout as `.clj` links before the jar): the oracle's reference, repr/java (javac's front end:
  `bin/repr-java`, its `check`, `jrt-convert`, `--java-fixtures`), j2g's edge (`bin/j2g-edge`) and the
  data generators (`bin/jrt testdata|manifest|standins`, `jrt-coverage`).
- `bin/clojure-tests` runs Clojure's upstream test suite against a Clojure, by default the
  frozen baseline on the JVM (`test/baseline-results.edn`), or with `CLOJURE_TESTS_GO` the Go
  build's test executable (`test/arbace-go-results.edn`; repr/clojure's translation and the report on the
  tools' host).
- `LICENSE.md` holds all licenses: their full texts and which files each covers. Licenses are
  stated nowhere else, apart from the notices source files came with, which stay. Extend it when
  vendoring from a new source.
- `arbace/` holds Arbace's own Clojure code, namespaces `arbace.*`, all `.ae` (Arbace's source
  extension: Clojure plus forms Clojure cannot evaluate; the loaders take `.ae`, then `.clj`):
  - the vendored Clojure, renamed from `clojure.*` (`arbace.core`, `arbace.lang` and the rest;
    the Java parts are Java forms). It is hand-maintained source; its derivation is recorded in
    `doc/VENDOR-NOTES.md` (replayable on `arbace-for-java-26`). Since S4 it has no bytecode back
    end, no ASM and no class loader (on `arbace-for-java-26`, with the JVM's three-stage
    bootstrap, the jar, its AOT cache, the jlink image and `bin/arbace-j`): the Go build
    translates it. The Go build's variants of `RT`, `Main` and `Compiler` are merged into their
    bases (SA150); `arbace/lang/go/` keeps the generated ones (`CompilerOps.ae`,
    `CompilerAOTFn.ae`, regenerated by `test/j2g/eval_ops.ae`, `test/c2j/gen_fns.ae`). The divergences this makes
    (gen-class a no-op, no class files, ...) are recorded in doc/VENDOR-NOTES.md and
    doc/STANDALONE-NOTES.md (D13).
  - `arbace/classes/`: the Java forms' front end (spec `doc/classes/SPEC.md`): parse, lower,
    analyze, env, types, native, the access flags (`opcodes`), `go`, the interpreter (`interp`);
    `jvm`, env's class-file reading, only for the frozen jar.
  - `arbace/c2j/`: c2j, the back end compiling namespaces to Java forms (`arbace.c2j.*`;
    standalone S6, doc/STANDALONE-NOTES.md, "S6: Arbace compiled to Go"): the analyzed units of
    a program's prepare run, `Compiler`'s `Expr` trees, become classes j2g translates like the
    runtime's (the compiled pipeline, AOT: c2j, j2g, repr/go's print, Go's compiler).
  - `arbace/repr/`: the bridges between a language's source text and its forms, each one
    namespace with its parts below it and one command with the verbs `read` and `print`:
    `arbace.repr.java` (`arbace/repr/java{.ae,/}`), Java → Java forms (`bin/repr-java read`,
    `check`) on the frozen JVM, and Java forms → Java (`print`, `arbace.repr.java.print`; the
    `.ae` writer is `arbace.repr.java.layout`) on the tools' host, with its round trip Java →
    forms → Java → forms at the levels forms, ae (the converted text) and classes (javac's, byte
    for byte; `bin/repr-java roundtrip`, corpora baseline, samples and jdk/forms, references in
    `test/repr/java/roundtrip/`; doc/classes/PRINTER-NOTES.md); `arbace.repr.go`
    (`arbace/repr/go{.ae,/}`), Go as Go forms and back (spec `doc/go/SPEC.md`; mode `:full` keeps
    the text: comments, spellings, layout, printed back byte for byte; helpers `tools/godump`, `tools/gocmp`;
    `bin/repr-go read`, `print`, `roundtrip`, `check`, `test`), on the tools' host
    (`REPR_GO_EXE` names another executable); `arbace.repr.haskell` (`arbace/repr/haskell{.ae,/}`),
    Haskell as Haskell forms and back (spec `doc/haskell/SPEC.md`, notes
    `doc/haskell/HASKELL-NOTES.md`; helper `tools/hsdump` on the host's GHC, pinned by
    `tools/hsdump/GHC_VERSION`; `bin/repr-haskell read`, `print`, `roundtrip` (`--iface`: GHC's
    interfaces too), `test`; corpus GHC
    9.14.1's boot libraries, `test/haskell/corpus-ghc-9.14.1.edn`), evaluated from the checkout,
    not embedded.
  - `arbace/repr/clojure{.ae,/}`: `arbace.repr.clojure`, Clojure's bridge (`doc/REPR-CLOJURE.md`;
    `bin/repr-clojure read`, `print` to come, `check`): Clojure source into Arbace's, the names
    of the vendored Clojure, `.clj`/`.cljc` into `.ae`, a table of rules for the dialects'
    divergences; `bin/clojure-tests` translates Clojure's suite with it.
  - `arbace/build/`: `arbace.build.go`, the build of a program held as Go forms: printed by
    repr/go into a module with its overlay, then `go build` (`bin/build-go`, `doc/go/BUILD.md`).
  - `arbace/j2g/`: the Java forms → Go forms translator (spec `doc/go/J2G-SPEC.md`; `bin/j2g`),
    on the tools' host (`J2G_GO_EXE`), its classes translated in parallel (`J2G_JOBS`, default at
    most 16) with the sequential output byte for byte; the resources compressed by
    `arbace/j2g/zlib.ae` (SA21).
- The Go build (B1a, `doc/go/B1-PLAN.md`): Arbace as a static Go executable. j2g translates
  the runtime (with the generated Go-build variants in `arbace/lang/go/`), and jrt, the Java runtime on Go (`go/arbace/jrt`, hand-written Go forms;
  `overlay/jdk/`, its own Java and variants of JDK classes; `jdk/`, the JDK's Java forms and
  resource data committed (repr/java's output from jdk26u and `overlay/jdk/java.base`, provenance in
  `jdk/provenance.edn`; `bin/jrt-convert update` regenerates them, `verify` checks them; standalone
  S1); `jdk/edge.edn`, the JVM's answers j2g needs, so j2g uses no JVM reflection or ASM
  (`bin/j2g-edge update`, `verify`, `trace`; S2, doc/STANDALONE-NOTES.md); `bin/jrt`); `overlay/go/` patches Go's runtime. `bin/arbace-go` builds
  (`--build`, `--build-tests`, `--pgo`, by the tools' host; the test build's Java
  fixtures committed as forms in `test/java-fixtures/`, `bin/clojure-tests --java-fixtures update`,
  `verify`), smoke-tests and runs it. Its namespaces are compiled to Go (`ARBACE_AOT=1`, the
  default since S6; `ARBACE_AOT=0`: evaluated from the image of prepared namespaces, as before):
  by the host when its runtime (`bin/lib/version.bash`'s `arbace_runtime_key`: `arbace/lang`,
  `go`, `overlay/jdk`, `jdk` but j2g's edge) is the checkout's, one j2g run, started as the host
  compiles them (SA160), about 2.5 to 3 minutes; else by a first build of the program, two runs, about
  5.5 minutes (after a change to the runtime). The test build reuses the shipped build's compiled namespaces when made from the same sources and
  runtime (`target/arbace-go/aot.key`); `bin/build-go` keeps a cache of printed packages
  (`--print-cache`, `.tmp/repr-go-print-cache`); `--fixpoint` builds G1 by the host, G2 by G1, G3 by G2 into
  `target/arbace-go/fixpoint/` and requires G2 = G3. The oracle (`bin/oracle`, `test/oracle/`,
  `doc/go/ORACLE.md`) checks it against the frozen JVM Arbace (the v4 jar's expected results,
  `test/oracle/expected`), against its known mismatches `test/oracle/known-go-amd64-frozen.edn`.
  Notes in `doc/go/`.
- The box (B1b, `doc/go/BOX-SURVEY.md`, `doc/go/BOX-NOTES.md`): the same program built with
  TamaGo (`GOOS=tamago`) as a guest of a monitor on /dev/kvm (amd64) and Hypervisor.framework
  (arm64; KVM/arm64 too). `box/` holds the hypervisor API, the monitor, the doorbell ABI and the
  TamaGo board, vendored as Go forms from go-whim (at `80c49bf`) and Arbace's own since, with
  their KVM tests on the box's own test guests (`arbace/box/guest`: a runtime-less guest in Go's
  assembly, a demo and a faulty Go guest; 2026-10-16, nothing of go-whim needed), and purego for
  the launcher on darwin/arm64 (`box/README.md` records the origins; `bin/box-vendor` checks
  them: `roundtrip`, `kvm-tests`, `purego-check`); import paths `arbace/box/*`. `tamago/` holds the guest's own Go as Go
  forms (S6): TamaGo's runtime and standard library (`tamago/goroot/`, GOROOT/src's layout) and
  its `goos` module (`tamago/goos/`), for tamago/amd64 and tamago/arm64 as one tree, converted by `bin/tamago-vendor`
  (`convert`, `verify`; `check` proves the guest image built from them byte-identical to one
  built from the originals); the box's guest is built from them printed (`ARBACE_BOX_GO=orig`:
  from the toolchain's tree). `tamago/cmd/` holds Go's toolchain as Go forms (S9): the 105
  packages of GOROOT/src/cmd that `compile`, `link` and `asm` build from, for linux/amd64,
  tamago/amd64 and tamago/arm64 as one tree, converted by `bin/toolchain-vendor` (`convert`,
  `verify`; `check` proves the nine tools built from them byte-identical to the originals'); the
  box's tool guests (`bin/box-tools`) are built from them printed (`BOX_TOOLS_GO=orig`: from the
  toolchain's tree). `bin/arbace-go --build --box` builds the
  guest image and Arbace's launcher `arbace-box` (`--smoke --box`, which runs `bin/box-smoke`;
  `bin/box-suite`; arm64 with `ARBACE_BOX_ARCHES`). `bin/box-arm64-vm` runs an emulated aarch64
  VM with KVM/arm64 for the arm64 box's checks (`check`: its smoke test, `box/`'s KVM tests);
  `bin/box-mac-bundle` packs the Mac's launcher, guest and checklist (`box/mac/`). The store (S3):
  blobs and refs in a host directory (`ARBACE_BOX_STORE`, `arbace-box --store DIR`, which is also
  its tool), the namespace `arbace.box` over it in the box and natively.
- `doc/ALPINE.md`: the host's Alpine packages as one minimal `apk add` line (`bin/alpine-env
  world`, `check`, `simulate`), the subset Arbace uses, and what apk does not provide (jdk26u,
  TamaGo's Go, Go's module cache: only `bin/gate --full`'s vendoring checks need it). No build
  uses cgo (`CGO_ENABLED=0` everywhere, 2026-10-16).
- `Makefile`: a familiar interface for newcomers, nothing more (`make help`): each target runs a
  script of `bin/`, which holds the logic. Keep it in step with `bin/`: when a script's name or
  options change, or a script becomes an objective of the repo, update its target and `##` line;
  `make check-makefile` (the essential gate's first step) checks the scripts it runs exist.
- `bin/gate` is the check every change to main must pass: the Makefile's scripts (`make
  check-makefile`), the seed's hash, stage 0 built from
  it (`bin/seed --build`), the executable and the test build by stage 0 (`bin/arbace-go
  --gate-build`, cached in `.tmp/arbace-go-gate` and keyed by a hash of what they are built from:
  `arbace/`, `go/`, `overlay/`, `jdk/`, `x509roots/`, the fixtures' forms, the build's scripts, the
  seed's and Go's pins; seconds when unchanged, else two Go-hosted builds, about 8 minutes: the
  test build reuses the executable's compiled namespaces), then
  concurrently the smoke test, the per-form differential check (`bin/diffcheck`, Futamura F2, since
  2026-10-17 in the essential gate: the oracle's forms evaluated against compiled by c2j, the Java
  forms interpreted against translated by j2g, against `test/diffcheck/known.edn`; about 5
  minutes, 40 s cached), Clojure's suite on the test build (`test/arbace-go-results.edn`) and
  the oracle against the frozen reference. `bin/gate --full` adds, concurrently: on the frozen
  JVM `bin/repr-java check --suite`, `bin/repr-java roundtrip --corpus baseline,samples,jdk`,
  `bin/jrt-convert verify` (`jdk/` converted afresh, byte for byte),
  `bin/j2g-edge verify`, `bin/clojure-tests --java-fixtures verify`; the Go build afresh with its
  smoke test, the box's guests from `tamago/`'s forms byte-identical to TamaGo's originals'
  (`bin/arbace-go --build --box`, `bin/tamago-vendor check`), the box's smoke test, its oracle's
  subset and `bin/box-suite`, then
  `bin/arbace-go --fixpoint`; repr/go's round trips (`bin/repr-go roundtrip amd64`, `linux/amd64`, and
  `--text amd64`: mode `:full` printed back to the original text, byte for byte), repr/haskell's
  (`bin/repr-haskell roundtrip --iface`: GHC's trees and lines over GHC 9.14.1's boot
  libraries, then their interfaces compiled at `-O1`, and `test`; on the host's GHC), the box's vendoring checks (`bin/box-vendor roundtrip`, `kvm-tests`, `purego-check`),
  `bin/tamago-vendor verify`, `bin/x509roots-vendor verify`, `check`; Go's toolchain as forms
  (`bin/toolchain-vendor verify`, `check`). Run it when the Java forms'
  front end, repr/java, repr/go, j2g or jrt change (after a change to `overlay/jdk/java.base`, jrt's closure
  or repr/java, `bin/jrt-convert update` first; after a change to what j2g asks of the JVM,
  `bin/j2g-edge update`). Logs in `.tmp/gate/`. Every gate run and every `bin/arbace-go --build`
  first trims Go's build cache of entries unused for three hours (`bin/lib/gocache.bash`,
  `ARBACE_GOCACHE_KEEP_MIN`). No build may use the host's `go`: only TamaGo's (`ARBACE_GOROOT`).

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
- The tools' names: the bridges between a language's text and its forms are the `repr` family
  (read/print, representation), repr/java, repr/go and repr/clojure (c2a before), and the
  translators are c2j (the back end, Arbace's namespaces → Java forms; `arbace.aot` before) and
  j2g (Java forms → Go forms), since 2026-10-17 (doc/NAMING-SURVEY.md; g2a's build is
  `arbace.build.go` since then, and class forms are called Java forms; "AOT" names the whole
  compiled pipeline: `ARBACE_AOT`, the packages `aot_arbace_*`); j2a, g2a and a2g before, as dated
  records and the tags name them; j2c, g2c and c2g before 2026-10-11, as the frozen branches and
  dated records name them.
- The tools (c2j, j2g, repr/go, repr/clojure, the suite runner) run on the tools' Go host, stage 0 by default
  (`bin/lib/host.bash`, `bin/lib/tools.bash`); repr/java and the JVM's records on the frozen jar
  (`bin/lib/frozen.bash`). Nothing builds or runs a JVM Arbace of the checkout.
- Put temporary and scratch files in `.tmp/`, which is gitignored.
- Never create or modify anything under `.claude/`.
