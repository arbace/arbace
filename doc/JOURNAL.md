# Journal

An append-only log of important decisions and actions, oldest first. Never edit past entries; add
a new entry to correct or supersede one. The goal is that the progress could be recreated from this
log.

This journal starts over at the second freeze, by the user's decision (2026-10-10), as it did at
the first. The history before it is on the frozen branches and in git history:

- 2026-10-06 to 2026-10-08 (the class forms, j2c, the vendored and self-hosting Clojure, the work
  for Java 26, the g2c survey): on
  [`arbace-for-java-26`](https://github.com/arbace/arbace/blob/arbace-for-java-26/doc/JOURNAL.md),
  tag `arbace-for-java-26-v1`;
- 2026-10-08 to 2026-10-11 (milestone B1a: g2c, c2g, jrt, the evaluator, the Go executable and
  its feature completion, the freeze of the Go build): on
  [`arbace-for-golang`](https://github.com/arbace/arbace/blob/arbace-for-golang/doc/JOURNAL.md),
  tag `arbace-for-go1.27.1-v1`.

## 2026-10-11: the arbace-for-golang freeze

- On the user's confirmation: the branch `arbace-for-golang` and the annotated tag
  `arbace-for-go1.27.1-v1` on main's `e358a78`, pushed. The tag's message names the commit, the
  full gate's result (19m34s on `e09139d`; documents and `bin/gate`'s default test JVMs since),
  the toolchains (JDK 26.0.2.1 built from jdk26u `baf63fb`, TamaGo's go1.27.1) and the release
  executables (PGO; amd64 66,371,744 bytes, arm64 64,225,440 bytes; not published). The branch's
  `doc/go/FREEZE.md` describes it.
- After the tag, on main (the user's decisions): this journal starts over; `doc/go/FREEZE.md` is
  removed from main and linked on the branch, as the JVM's FREEZE.md; the README's "Arbace for
  Golang" links it there; CLAUDE.md's direction names both frozen branches and what comes next,
  and its Records paragraph both journal restarts. The Go build stays on main and is built and
  checked as before (`bin/gate`, `bin/gate --full`).
- Next (the agenda): the `.ae` rename; c2a and the tools renamed j2a, g2a, a2g; Arbace's own
  version; then B1b, the box (doc/go/BOX-SURVEY.md).

## 2026-10-11: after the freeze: the .ae rename, c2a, Arbace's version

- The user's decisions: do the agenda's items 1-3 (the `.ae` rename; c2a and the tools renamed
  j2a, g2a, a2g; Arbace's own version); no `bin/gate --full` until the next milestone (targeted
  checks and the essential `bin/gate` instead, as before the freeze).
- The `.ae` rename first, alone (branch `ae`, an agent): it touches the loaders, the bootstrap
  (the seed reads `.clj` only), every tool, the oracle's case ids and the documents. c2a with
  the tool renames and the version follow on the renamed tree, in parallel.
- The `.ae` rename done (agent, branch `ae`: `8913648` the `git mv` of 688 files, `c76b468`
  loaders, bootstrap, tools, documents, `d36a19d`; fast-forwarded into main). Renamed: all of
  `arbace/`, `go/`, `overlay/`, and Arbace's test sources and data under `test/` (class forms,
  native, oracle forms and tools, g2c cases and expected forms, c2g, jrt, j2c, net, the PGO and
  AOT training scripts); kept `.clj`: what upstream Clojure also runs (Clojure's suite and its
  libraries, copied and renamed by `bin/clojure-tests`; `test/run_clojure_tests.clj`; the
  benchmarks' `workload.clj` and `repl-session.clj`), `data_readers.clj`/`.cljc`, the reader's
  `:clj` feature. `RT.load` (JVM and the Go variant) looks for `.ae`, then `.clj`, then `.cljc`;
  `user.ae` before `user.clj`; stack traces demunge all three; the tools read and write `.ae`
  (six places that cut a 4-character extension fixed). The bootstrap: the seed reads `.clj`
  only, so `bin/build-arbace` gives stage 1 `target/seed-view`, `.clj` symlinks to the sources;
  every stage's AOT names `core.ae`; stage 2 = stage 3 (5,772 classes). The oracle's case ids are
  `forms/x.ae:LINE`. `arbace.j2c.rename` maps a vendored namespace's source path to `.ae` (the
  suite's java.io `test-resource` needed it). VENDOR-NOTES hand change 19 and a dialect
  divergence (a library with both `x.ae` and `x.clj` loads the `.ae`); `.gitattributes`
  highlights `.ae` as Clojure.
- Checks (the agent's; targeted, no `--full`): `bin/gate` (the suite on stage 2 20,750 of
  20,750 after the java.io fix); class forms tests; `bin/j2c-check` 183 of 183; g2c print and
  convert tests and `bin/g2c roundtrip amd64` without regressions; the Go build, smoke and test
  build; the Go oracle 22,232 of 22,278 as recorded; Clojure's suite on Go 20,506 of 20,520;
  `bin/oracle check jvm` (the known case; a `concurrency` barrier-order flake under load);
  `bin/jrt test` amd64 and arm64; by hand: `.ae` and `.clj` scripts on both launchers, `source`,
  `doc`, stack traces naming `core.ae`.
- The user's decisions on the rename's open points: Arbace also reads `data_readers.ae`, before
  `.clj` and `.cljc` (with the version work); the rest as the agent left it (the upstream-run
  test tools stay `.clj`; the suite's renaming maps source paths to `.ae`, c2a's to take over;
  upstream file-name comments untouched; `.ae` wins over `.clj`, a dialect divergence).
- Started in parallel: c2a and the tool renames j2a, g2a, a2g (branch `c2a`); Arbace's own
  version (branch `version`).

## 2026-10-11: c2a; the tools renamed j2a, g2a, a2g

- Agent, branch `c2a` (7 commits), fast-forwarded into main. c2a, the Clojure → Arbace
  translator (`arbace/c2a/`, `bin/c2a`, doc/C2A.md): text-level, guided by a lexical reader
  (`text.ae`, as LispReader reads), so layout and comments are kept; `arbace.j2c.rename` moved
  in as `arbace.c2a.rename`, its rules unchanged; a rule table (`rules.ae`): reader conditionals
  resolved for `:clj` when `.cljc` becomes `.ae`, the renames, the proxy method mapping names
  (the suite's former `sed`), `:core-names` (a new dialect divergence: arbace.core's
  `{:added "arbace"}` vars such as `return` are excluded where a file defines them; test.check's
  generators did), `SecurityManager` qualified; a table naming, for each VENDOR-NOTES divergence,
  its rule or why none applies. `bin/clojure-tests` translates the suite and its libraries with
  c2a: the suite's translation is byte-identical to before (`test/c2a/suite.sha256`); six
  libraries translated to `.ae` load and pass 14 checks on both builds (`bin/c2a-check`).
- The tools renamed after their target (`git mv`, then every caller): `arbace/j2c` → `j2a`,
  `g2c` → `g2a`, `c2g` → `a2g`, with `bin/`, `test/`, environment variables (`J2A_*`, `G2A_*`,
  `A2G_*`), properties, the overlay directives (`a2g/variant`...), the generated Go program's
  names (`a2g_*.go`, `A2g_*`), and the documents (`C2G-SPEC.md` → `A2G-SPEC.md`, `C2G-NOTES.md`
  → `A2G-NOTES.md`; `G2C-SURVEY.md` keeps its name as a historical survey). No shims for the old
  command names. Living documents use the new names, dated passages included, each with a note
  of the old names; the journal and the vendored files' provenance lines keep them.
- Checks (the agent's; targeted): `bin/gate` passed (7m04s); `bin/j2a-check` 183 of 183; g2a's
  print and convert tests and `bin/g2a roundtrip amd64` without regressions; jrt-convert; the
  Go build, smoke, test build; the Go oracle as recorded (46); Clojure's suite on Go 20,506 of
  20,520; `bin/oracle check jvm` (the two known cases); `bin/jrt test`.

## 2026-10-11: Arbace's own version; data_readers.ae

- Agent, branch `version` (to `c853641`, with main merged), fast-forwarded into main.
  `*arbace-version*` `{:describe :commit :dirty}` and `(arbace-version)` (the describe), both
  `{:added "arbace"}`; the REPL's banner `Arbace <describe> (<7 hex>)` on both builds;
  `*clojure-version*` unchanged. The value: `bin/lib/version.bash` asks git when ROOT is a
  checkout's top level (so a tarball unpacked in another repository, as abuild's `src/`, does
  not take that repository's version), else reads `/VERSION`, filled by `git archive` through
  `export-subst` (`$Format:%H$`, `$Format:%(describe:tags)$`), else "unknown". `arbace.core`
  reads it at load (the system property `arbace.version`, then the resource
  `arbace/version.edn`), so no compiled class, image or AOT cache holds it. The JVM build writes
  `arbace/version.edn` into every stage and the jar (stage 2 = stage 3; reproducible per commit
  and dirty flag); the Go build sets `main.arbaceVersion` with `-ldflags -X` at its final links
  (a2g's output stays a function of the sources), and the main package sets the property. The
  gate's cached executable keeps the commit it was built at (its key hashes inputs, not the
  commit); its smoke test checks the banner against the executable's own `*arbace-version*`.
  Describe uses every tag: main reads `arbace-for-go1.27.1-v1-N-gHASH` until it has a tag of its
  own. VENDOR-NOTES hand changes 20 (the version) and 21 (`data_readers.ae` read before `.clj`
  and `.cljc`), both dialect divergences.
- Checks (the agent's, on the merged code): `bin/gate` passed (native tests 54, the suite on
  stage 2 20,750 of 20,750); `bin/c2a-check`; `bin/oracle check jvm` (the two known cases); the
  Go build, smoke, test build and suite, the Go oracle as recorded; `git archive`'s `VERSION`
  read correctly inside another repository.
- The user's decisions on c2a's open points: living documents keep the new tool names, dated
  passages included; c2a translates a library's `data_readers.clj`/`.cljc` into
  `data_readers.ae` (now that Arbace reads it).
- The user's decisions on the version's open points: describe keeps every tag (main tags come at
  the next milestone); the `arbace.version` property overrides on both builds; a tarball's
  `:dirty false` is acceptable (patches are the packager's record); the gate's cache keyed on
  inputs, not the commit; the old banner in dated transcripts stays.
- c2a translates a library's `data_readers.clj`/`.cljc` into `data_readers.ae` (agent, branch
  `c2a-readers` `bb34f8f`, fast-forwarded): the usual rules, a `.cljc`'s conditionals resolved
  for `:clj`; a file that would collide with an existing data_readers file keeps its name (Arbace
  reads and merges all three); `--keep-ext` and `project.clj` unchanged. The suite has no
  data_readers files (its digest unchanged); `bin/c2a-check --libs` adds a small library with a
  `data_readers.cljc` and a tag, 15 of 15 on both builds.

## 2026-10-11: B1b, the box: the user's decisions

- On BOX-SURVEY.md's decisions, all as recommended: D3 go-whim's box (`hv/`, `vmm/`,
  `guest/abi`, the board, `internal/libsys`) vendored as Go forms converted by g2a, its origin
  recorded, first unchanged (a byte-identical build), then the editor-specific dispatch made
  generic as an Arbace change; D8 the vendored parts are EPL-1.0, stated in LICENSE.md (the
  user's own code, not vim-derived, under Vim's licence in go-whim by that repository's
  default); D9 the first target S1, a boxed REPL on amd64 (REPL, `-e`, `-m` over the console
  doorbell; files in memory; no network, no processes; four vCPUs by default, one in the
  tests); D10 the host's wall-clock time at entry in a register, the board's clock offset by it
  (no new call); D11 files in memory, then the content-addressed store (S3), no host file
  system; D12 the network decided at S5; D13 processes cut for good in the box; D14 the monitor
  stays a Go program on the host.
- The user's go-ahead for B1b: S0 (vendoring go-whim's box as Go forms, an agent, branch
  `box-s0`), then S1 (the boxed REPL on amd64).

## 2026-10-11: B1b S0: go-whim's box vendored

- Agent, branch `box-s0` (`3fa588c`, `0012914`), fast-forwarded into main. From
  /root/go-whim at `80c49bf` (read only): `hv/`, `vmm/` (without `cmd/`), `guest/abi`,
  `guest/tamago/board`, `internal/libsys`, the exact closure (38 Go files, 2 assembly; 4,571 +
  599 + 432 lines), converted by g2a into Go forms under a source root of its own, `box/`
  (`box/go/arbace/box/{abi,board,hv,libsys,vmm}`; import paths `arbace/box/*`, only the import
  specs rewritten; libsys out of `internal/`): one tree for every target (linux/amd64,
  linux/arm64, darwin/arm64, tamago/amd64, tamago/arm64 gave identical forms for shared files).
  `bin/box-vendor` (`convert`, `check`) reproduces it from scratch. Proofs: gocmp's round trip
  on each package in each target (14 of 14); go-whim's monitor and guests built from the
  printed sources byte-identical to go-whim's own (7 of 7, with a fixed build id); go-whim's
  `hv`, `vmm` and guest tests on /dev/kvm on the printed copy. No x/sys or seccomp library (raw
  ioctls, hand-written BPF); purego (Apache-2.0) for darwin only and TamaGo's `goos` module
  (BSD-3) for linking a guest, neither vendored. `tools/godump` takes a module's `go 1.27.1` as
  language `go1.27`. LICENSE.md: the vendored parts under EPL-1.0 (D8): the user's own code
  (one author in their history, `rhz`), not derived from vim, whose licence go-whim's LICENSE
  is. CLAUDE.md's layout names `box/`.
- What S1 needs: `vmm.Config.Host` is go-whim's editor host and `vmm.Run` maps the editor's exit
  panic: a host interface and exit type of `vmm`'s own; Arbace's own launcher (cooked console,
  the image, its environment names, rlwrap, the watchdog off or long, four vCPUs); D10's clock
  register; the guest's build (`GOOS=tamago`, `GOOSPKG`, a `require` of TamaGo's module) in
  `bin/g2a build`.
- The user's decisions: keep `box/`; pin `goos` at S1 and decide purego at S4 (the Mac); the
  round trip and go-whim's KVM tests join `bin/gate --full`, the byte-identity proof stays a
  one-time S0 proof (S1 changes `vmm`).

## 2026-10-11: Tidy-up: files no longer useful at this stage

- A survey of tracked files (`.tmp/obsolete-files.md`) found nothing clearly dead; the user took
  all four of its proposals. Agent, branch `tidy` (`860e654`..`da5a2f6`), merged into main;
  the essential `bin/gate` passed on its tree (7m10s; Clojure's suite 20,750 of 20,750, class
  forms tests 136 of 136, the Go smoke test).
- `.gitignore`: `.cpcache/` and `.nrepl-port` dropped (GitHub's Clojure template; no deps.edn, no
  nREPL).
- The JVM-era benchmark (`bin/arbace-bench`, `test/bench/{driver.ae,workload.clj,
  repl-session.clj}`, `doc/BENCHMARKS.md`) removed from main: `arbace-for-java-26` has its own
  copies (its driver as `test/bench/driver.clj`); the README links the branch's BENCHMARKS.md.
  `test/bench/eval-bench.ae` stays.
- jrt's tools, misfiled under `test/g2a` as `g2a.*`, moved to `test/jrt/{java_surface,
  jrt_coverage,jrt_sources}.ae` as `jrt.*`. `bin/jrt-convert` again 835 files (830 identical, 5
  known differences); `bin/jrt-coverage` byte-identical to `doc/go/java-base-coverage.edn`
  (55.9%). `arbace/inspector.ae` and `xml.ae` begin with a module guard before their `ns`, which
  had broken `bin/java-surface`; it now reads forms up to the `ns`.
- The one-off checks run once more: kept `bin/a2g-check -- --program` (9,022 of 9,022 steps on
  amd64 and arm64, after a fix: it embedded only `.ae` resources, while a2g's main package embeds
  `*.ae.z` since SZ2, so all 50 builds failed), `bin/a2g-evalproof` (as expected, both
  architectures), `bin/a2g-race` (the 4 known reports on the `int32` hash caches; it matters for
  the box's four vCPUs), `bin/g2a-build-tests` (30 of 30) and `bin/net-check` (5 of 5). Retired
  to `arbace-for-golang` (there as `bin/c2g-regex`, `bin/c2g-perf`): `a2g-regex` (1,161 of 1,233:
  its program, rooted at `RegexCheck#run`, no longer reaches `Properties.setProperty` that a class
  initializer now calls; the oracle's regex part checks the same corpus on the executable, 1,233
  of 1,233, in `--full`) and `a2g-perf` (its ratios unchanged from SPEED-NOTES' "after" column).
- AGENDA's Done sections trimmed to records pointing to the frozen branches; B1-PLAN marks step 8
  done and B1b started.
- Open: `doc/go/java-surface.edn` lags stage 2 (for instance class-forms 1,449 → 1,454 classes);
  it is a build input, so regenerating it is a step of its own.
