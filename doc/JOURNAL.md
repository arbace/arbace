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

## 2026-10-11: B1b S1: the boxed REPL on amd64

- Agent, branch `box-s1` (6 commits on `41f31be`), merged into main after the tidy-up (one
  conflict, B1-PLAN's status line). Details and the amendments BX1-BX10 (proposed): BOX-NOTES.md,
  S1; A2G-SPEC §9.4, §10.6, §16.
- `bin/arbace-go --build --box` (after `--build`) builds the guest (`GOOS=tamago`, reusing a2g's
  output and the image of prepared namespaces from the Linux build; `image.key`) and the
  launcher `arbace-box` into `target/arbace-go/box-amd64/`. The REPL, `-e`, `-m`, `-`, scripts in
  TamaGo's in-memory file system, futures, exit codes, the host's wall clock, rlwrap and Ctrl-C
  work in the box on four vCPUs and on one.
- The seam (BX7): `vmm` has a `Host` of its own (console, exit), an optional `TermHost`, and
  `vmm.Exit`; it no longer imports go-whim's editor, so S0's byte-identity proof no longer holds,
  by design. The ABI (BX8): the environment after argv; the host's wall clock at entry (R11 on
  amd64, X6 on arm64; D10); an idle guest on one vCPU parks in the monitor; `board.NilPanics()`
  (amd64) turns a fault below the first page into a run-time panic, so nil dereferences are
  NullPointerExceptions. The launcher (BX9): a Linux Go program with the guest appended, four
  vCPUs by default, `ARBACE_BOX_*` settings, an allow-list for the guest's environment. jrt's box
  host (BX1-BX5): the console polled without blocking the guest, entropy, exit, files in memory,
  args and env from the boot data; `jrt.ProgramHost` picks the host. The evaluator yields with
  `runtime.Gosched` every 65,536 ticks on TamaGo (R2), an empty inlined call on Linux (within
  noise). `bin/g2a build --goos tamago --root DIR` (BX6; TamaGo's `goos` v1.27.1, go.sum checked
  against pinned hashes); its BSD-3 licence in LICENSE.md. `box/` changed by editing Go in a clone
  and converting back (`bin/box-vendor --from CLONE convert`).
- Numbers (nested KVM): start 367 ms in the box against 207 ms native; resident 448 MB against
  163 MB; guest 76.8 MB, launcher 3.7 MB. Output during a wait reaches the host in 3-6 ms (the
  survey measured 2.6 s late).
- Checks: `bin/arbace-go --smoke --box` 64 checks on four and one vCPUs; the box oracle (all but
  `forms/files`, `forms/net`, `forms/processes`; the runner's new `--exclude`) 21,695 of 21,742,
  the 46 known native mismatches plus the reducers case, which runs the 3 GiB guest out of memory
  (recorded with a new `:incomplete` key, `test/oracle/known-go-box-amd64.edn`); `bin/box-suite`
  19,625 of 19,627 (`test/arbace-box-results.edn`; the two errors need the network or a host
  file; 10 namespaces excluded with reasons); `bin/box-vendor roundtrip` 16 of 16 and
  `kvm-tests` pass, both now the `box` step of `bin/gate --full`. Linux unchanged: the
  executable's smoke test, the oracle (46 known), `bin/jrt test`, the suite on the test build
  (20,506 of 20,520). Not run: arm64.
- Open for the user: accepting BX1-BX10; the box's smoke test, oracle and suite in `--full`
  (about 10 minutes more); the launcher's memory (the image read and copied whole; mapping it
  would save most of 2 × 77 MB) for S2; `NilPanics` on arm64 for S4. Noted: natively, the
  reducers tests fail when their namespace alone is loaded first.
- The user's decisions the same day: BX1-BX10 accepted; the box's checks in `--full` deferred;
  the launcher's memory to be done now (S2's first item); `doc/go/java-surface.edn` to be
  regenerated (the tidy-up's open point).
- The merged tree's essential `bin/gate` failed twice on the pty test's new check "Ctrl-C without
  rlwrap", which passed when run alone: `bin/gate` starts the smoke test as a background job of a
  script, which starts with SIGINT and SIGQUIT ignored, and an ignored signal stays ignored across
  exec (rlwrap resets it, so the check under rlwrap passed). `test/arbace-go-rlwrap.py` resets
  both in the pty's child, as a login shell would; the old test fails the same way under
  `trap '' INT QUIT`, the new one passes. The gate then passed (4m05s); the box's build and
  `--smoke --box` passed on the merged tree.

## 2026-10-11: `doc/go/java-surface.edn` regenerated

- Agent, branch `jsurface` (`da5cd2b`), merged into main; the essential `bin/gate` passed on its
  tree (7m49s). Generated once before, at `67e56cd` (2026-10-08). Its per-source j2a statuses
  need a `bin/j2a-check --jdk java.base` run first (3,003 of 3,025 shape-identical), or every
  source reads `:unchecked` (most of the 1,250 lines the tidy-up saw); the real diff is 187
  lines, deterministic.
- What changed: the REPL group 2,779 → 2,811 classes (the 39 `core$bean*` classes of hand change
  15, with reflection instead of `java.beans`: its 4 classes gone, 319 → 315 JDK classes),
  class forms 1,449 → 1,454 (the NaN fix, `shape.ae`); the version's `readAllBytes`; `Optional`
  in the shipped closure (hand change 17's module checks). jrt's closure (runtime + REPL) is
  unchanged: 422 classes, 184 files, the same boundary and natives; `bin/jrt-convert` the same
  835 files (830 identical, 5 known); java.base coverage 55.9%, Arbace's use 837 of 901 provided
  (`Module.isExported` absent: the Go build's `bean` has no modules). JAVA-SURFACE.md,
  JAVA-BASE.md, JRT-NOTES.md and `java-base-coverage.edn` follow.
- Checks: the executable unchanged (66,523,296 bytes), `--smoke` 41 of 41, the oracle 22,232 of
  22,278 (the 46 known), `bin/jrt test` (amd64); the box's build unchanged, `--smoke --box` 64 of
  64. The suite on the test build skipped: the closure did not change.
- Noted, not caused by this: a2g's member tables order same-name overloads differently from
  main's previous build (4 classes in jrt, about 17 in `lang`; that build was from a dirty tree,
  so not conclusive); 21 files of jrt-convert's output differ only in a header naming a numbered
  temporary directory (`.tmp/j2a-patch/java.base-N`).

## 2026-10-11: B1b S2, R1: the box launcher maps the guest image

- Agent, branch `box-mmap` (`01eca80`), merged into main; details in BOX-NOTES.md, S2, amendment
  BX11 (proposed). `vmm.ImageFile` (a file, offset, size; `vmm.OpenImage`, `vmm.AppendedFile`):
  the monitor reads the ELF from the file and maps each loadable segment privately into the slot
  (`mapfixed_linux.ae`), or reads it straight into the slot when it cannot (unaligned, macOS,
  `ARBACE_BOX_MAP=0`); never a copy in the launcher's heap. `vmm.Append` and `bin/arbace-go`'s
  `box_append` pad the launcher to 64 KiB before the image. The symbol table is read once for the
  three symbols the monitor needs (it was parsed three times, about 65 MB of heap). New tests in
  `imagefile_test.ae`. `box/` edited as Go in a printed copy and converted back by g2a.
- Numbers (`-e nil`, best of 9, nested KVM): resident 445-448 MB → 257-265 MB mapped, 291-295 MB
  copied (native 161-164); start 0.36 s → 0.55 s mapped, 0.32 s copied (native 0.20). Mapped
  starts slower here: about 8,400 small faults through two levels of nested paging against the
  copy's 2 MiB pages; not measured without nesting. Evaluation speed unchanged.
- Checks: the executable and guest unchanged in size; `--smoke --box` 64 of 64, mapped and copied
  (and again on merged main); the box oracle 21,695 of 21,742 (the 47 recorded); `bin/box-vendor
  roundtrip` 16 of 16 and `kvm-tests` pass. A guest image must not be rewritten in place while a
  box runs from it (the build replaces the launcher by rename).
- Open for the user: BX11, and the default (mapped, as R1 asked, or copied).
- The user's decisions: BX11 accepted; the default stays mapped. And the two nondeterminisms
  noted with the surface's regeneration (a2g's overload order, jrt-convert's headers) are to be
  fixed now, beside S2, rather than at S6.

## 2026-10-11: B1b's order: amd64 first, S4 the next milestone

- The user: S4 (arm64: KVM/arm64, Hypervisor.framework on the Mac; with it the purego decision,
  `board.NilPanics` for arm64 and S6's arm64 identity) is deferred to the next milestone. This
  milestone is the box on amd64: S2, S3 (the store), S5 (the network, D12: a stream proxy or
  packets with netstack in the guest, decided at S5) and S6 on amd64; its freeze runs
  `bin/gate --full`. AGENDA.md and BOX-SURVEY.md §6 follow.
- The user, the same day: S6 before S3 and S5. S6 needs only S1's guest build, reproducible
  builds (the determinism work in progress) and g2a's round trip of TamaGo's std, not the
  store or the network; converting all of TamaGo's runtime and std, not only the image's closure,
  covers S5's netstack if packets are chosen. With TamaGo's Go as forms first, S3's and S5's
  runtime changes are made on forms, as `box/` is. The order: S2, S6 (amd64), S3, S5, each later
  stage re-running S6's proof.

## 2026-10-11: Reproducible builds: a2g's overload order, jrt-convert's headers

- Agent, branch `determinism` (`fe7e878`, `b4af020`), merged into main; the essential `bin/gate`
  passed on its tree (6m33s).
- a2g's member tables: two `bin/a2g --program` runs in separate JVMs from one clean tree gave
  different `a2g_classes.ae` (jrt and `lang`). HotSpot orders a class's methods by the addresses
  of their names, which for classes outside the JDK's CDS archive change from process to
  process, and with them the order of same-name overloads in `getDeclaredMethods`; a2g copied
  that order (M4, so that the Reflector picks the JVM's overload). Now (`arbace/a2g/out.ae`,
  amendment RP1, A2G-SPEC §5.11, proposed): the JVM's order only for classes in the archive's
  list (`$java.home/lib/classlist`) when the JVM runs with sharing, every other class's overloads
  by descriptor; the cut classes' tables also by return type. One oracle form changed its pick
  (`concurrency.ae:286`, `.schedule` on an unhinted `ScheduledThreadPoolExecutor` with a fn:
  Callable before, Runnable now): hinted in the form, its expected output re-recorded.
- `bin/jrt-convert`'s headers named the scratch copy (`.tmp/j2a-patch/java.base-N`): j2a's `jdk`
  command takes `--origins FILE` and names each file's source (`jdk26u src/java.base/...`,
  `overlay/jdk/...`, the generated ones); `--twice` reports the conversion deterministic.
- Two builds of a clean commit (`fe7e878`, the second with an empty `GOCACHE`) gave identical
  `amd64/arbace`, `box-amd64/arbace.elf`, `arbace-box` and `image.bin` (hashes in BUILD.md); a
  dirty tree differs by its version string. Checks: `--smoke`, the oracle (the 46 known),
  `bin/a2g-check -- --program` 9,022 of 9,022 on amd64 and arm64.
- Open: RP1 for the user; `test/jrt/tables.ae` (jrt's tracked `reflect_tables.ae`) still takes
  the JVM's order (given to S6's agent); Clojure's suite on the Go test build after the merge: 20,506 of 20,520, no regressions.
- S6 started (agent, branch `box-s6`): TamaGo's runtime, std and `goos` module as Go forms, the
  guest image built from them byte-identical to one from the originals.

## 2026-10-11: B1b S2 done: the box measured and hardened

- Agent, branch `box-s2` (`3410525`, `ef5bc28`), merged into main (BOX-NOTES' two S2 sections
  kept, R1's and the rest's). Details: BOX-NOTES.md, S2; amendments BX12-BX17.
- Reducers' 4.9 GB, natively too: the evaluator kept the reducer's `reify` in the method's frame
  through the tail call into `coll-reduce`, and with it the head of the `Range`; a call in tail
  position now clears the method's object, as the JVM's `emitClearThis` (CLJ-1250) (BX14): 4.9 →
  0.30 GB natively, and it runs in the box. The reducers tests failing when their namespace is
  loaded alone: the Go build's `RT.load` re-evaluated `arbace.core` when
  `clojure.data.generators` required it, and `defprotocol` made a new `CollReduce`; an embedded
  namespace now loads once (BX15), sources on `ARBACE_PATH` still reload.
- `fold` on 4 vCPUs: the evaluator's yield counted calls in one global written by every vCPU;
  per thread now (BX16): 3.2x (allocating) and 3.4x (computing) on 4 vCPUs, native 3.5x and
  3.2x.
- The collector: `GOMEMLIMIT` was read by TamaGo's runtime before jrt set the environment; now
  applied, default the guest's RAM less 1 GiB (BX13): live data before running out 1,152 →
  2,432 MiB; GOGC stays 200 (100 was 5% slower, 400 400 MB more). The watchdog stays off: an
  evaluation makes no hypercall while it computes. The suite's file tests: `bin/box-suite`
  writes the suite directory's files into the guest's working directory (BX17); `ARBACE_PATH`
  checked by the smoke test. The guest's symbol table cut to the monitor's three symbols by
  `tools/elfsyms` (BX12): guest 76.8 → 65.9 MB.
- Numbers (nested KVM): start 330 ms on 4 vCPUs (copied; native 200-210), evaluation within noise
  of native; the box's suite in 222 s at 12 at a time (native test build 214 s).
- Checks on the branch: `--smoke` 42, `--smoke --box` 70, the native oracle (the 46 known), the
  box oracle 21,696 of 21,742 (the reducers case matches: its `:incomplete` removed),
  `bin/box-suite` 19,645 of 19,646 (the socket test), the suite on the native test build without
  regressions, `bin/jrt test` on both architectures, `bin/box-vendor roundtrip` and `kvm-tests`.
- The user's decisions: BX12-BX17 accepted (`tools/elfsyms` kept); the watchdog off by default;
  the mapped start measured on a host without nested KVM before changing the monitor; RP1
  accepted, with the hint on the oracle's ambiguous `.schedule` form.
- On merged main (with RP1): `--build`, `--build --box`, `--smoke`, `--smoke --box` pass; the
  native oracle and the box oracle as recorded (46 each, 0 new); `bin/box-suite` 19,645 of
  19,646.

## 2026-10-11: B1b S6 done for amd64: the guest's own Go as forms

- Agent, branch `box-s6` (`aa3dfda`, `cd4cfe8`, `fc808c8`, `0a95951`, `b148601`, with main merged
  in as `0853f3e` so that the proof covered S2), merged into main. Details: BOX-NOTES.md, S6;
  amendments BX20-BX24.
- `tamago/`, a source root of its own (SPEC §4.1): `tamago/goroot/go/`, TamaGo's std for
  tamago/amd64 (371 packages with their assembly, 2,105 files, 579,502 lines, 31.4 MB) and
  `tamago/goroot/src/`'s module files; `tamago/goos/`, 69 of the goos module's 94 packages (those
  that build for tamago/amd64); 32.2 MB, 6.4 MB gzipped. Left out: std's 6 test-only packages,
  `internal/trace/traceviewer` (Catapult's viewer) and `encoding/json/internal/jsontest` (JSON
  corpora): third-party data no guest can import. Origin pinned in `bin/tamago-vendor`: TamaGo's
  go1.27.1 at /root/tamago-go (a digest of `src/` without `cmd/`), goos v1.27.1 by its go.sum
  hash; recorded in `tamago/README.md` and LICENSE.md (Go's BSD-3).
- `bin/tamago-vendor`: `convert`, `verify` (a fresh conversion byte for byte), `print` (a printed
  GOROOT, the module, jrt's overlay for it), `check` (the proof). `bin/g2a build --print-only
  --module std`, `--goos-dir` (BX21). `bin/arbace-go --build --box` builds the guest from the
  printed forms by default, `ARBACE_BOX_GO=orig` from the originals (BX22).
- The proof: std printed against GOROOT/src by `bin/g2a-check`, 371 of 371; the goos module's
  packages by gocmp, 69 of 69; the guest image built with `-buildid=` from the forms and from the
  originals identical as linked (76,766,368 bytes) and after `tools/elfsyms`' cut (65,909,034
  bytes). The shipped images differ only in the build ID, which hashes the source text (the
  printer keeps no comments); two builds from the forms are identical. Its first attempt failed:
  a literal beginning a Go line (a final `return true` after a comment) was printed on the wrong
  line, changing gc's line tables; the round trip's code level compares code, not line tables.
  The converter records `[index line]` in `:go/breaks` for such literals (BX20);
  `overlay/go/runtime/proc.ae` follows.
- Also: `test/jrt/tables.ae` follows RP1, `reflect_tables.ae` regenerated (five tables reorder
  overloads that differ in arity: the Reflector's choice unchanged). The Linux executable's bytes
  change through it and `proc.go`'s line numbers.
- On merged main: the essential `bin/gate` passed; `--build`, `--build --box` (from the forms),
  `--smoke --box`; `tamago-vendor verify` and `check` (identical, as linked and cut); the native
  oracle as recorded (46).
- The user's decisions: BX20-BX24 accepted; `tamago/` kept as committed; `tamago-vendor verify`
  joins `bin/gate --full`'s `box` step and the box's build from the forms with `check` its go
  chain (`box-forms`; run at the milestone's freeze, not yet run in `--full`).

## 2026-10-11: B1b S3 started: the store

- Agent, branch `box-s3`. The user's decisions before it, all as recommended: no store unless
  `ARBACE_BOX_STORE` (or a launcher flag) names a directory (else `arbace.box` refuses), rather
  than a default under `~/.arbace`; `arbace.box` on the native build too, over a host directory
  in the same format, rather than box-only; loading by a `store:` entry kind in `ARBACE_PATH` and
  scripts by `ref:NAME`, rather than an explicit call only; the in-memory fs saved to and seeded
  from a ref as a tar, explicitly (`save-fs!`, `ARBACE_BOX_FS`), rather than EDN or not in S3;
  a host tool in the launcher (`arbace-box --store DIR put|get|ref|ls`).

## 2026-10-11: B1b S5 started: the network; D12 decided

- Agent, branch `box-s5`, beside S3 (new hypercalls numbered from 40). The user's decisions, all
  as recommended: D12, a stream proxy (TCP streams through hypercalls, the launcher dialing and
  listening on the host under a policy), rather than packets with gVisor's netstack in the guest
  (not in std: a large new third-party vendoring; the host would see only packets); the policy:
  no network unless the launcher allows it, listening on loopback by default, dialing by an
  allow-list of host:port patterns, rather than one unrestricted switch or loopback always on;
  the scope: TCP only, names resolved by the host under the same policy, TLS in the guest only if
  jrt's Linux build already has it, rather than UDP too or TLS required; the socket REPL by the
  same `arbace.server.repl` mechanism as natively, its port on the host's loopback, rather than a
  launcher flag.

## 2026-10-11: The box on amd64 is a milestone, not a freeze

- The user: when S3 and S5 are done, the milestone is marked by `bin/gate --full` and a tag on
  main; no branch, no freeze, and so the journal does not start over (unlike the freezes of
  `arbace-for-java-26` and `arbace-for-golang`).
- The tag's name, accepted by the user: `arbace-box-amd64-v1`.

## 2026-10-11: B1b S3 done: the store; S5 done on its branch

- S3: agent, branch `box-s3` (`57d360d`), merged into main (main had moved by docs only). Details:
  BOX-NOTES.md, S3; amendments BX30-BX36. go-whim's directory format (`blobs/HASH`, `refs/NAME`,
  names of 1-64 `[A-Za-z0-9._-]`, compare-and-set); no store unless `--store DIR` or
  `ARBACE_BOX_STORE`; the box and the native executable share a directory, refs written under an
  `fcntl` lock on `refs/.lock` (the monitor's filter allows `fcntl`, not `rename`, `flock` or
  `mkdir`). The store's failures have codes of their own in `abi` (-3 no store, -4 too large, -5
  bad name; go-whim's -1 and -2 kept). jrt: `jrt.StoreHost`, the box's over the hypercalls (its
  buffers on the heap: goroutine stacks move), `OSHost`'s in `store_linux.ae`; the Java class
  `jdk.internal.jrt.HostStore`. `arbace.box` (`arbace/lang/go/ns/box.ae`): `put`, `get`,
  `get-bytes`, `size`, `ref`, `ref!`, `put!`, `load`, `load-ref`, `save-fs!`, `load-fs!`,
  `store?`, refusals as `ex-info` with `:arbace.box/error`. `store:PREFIX` entries in
  `ARBACE_PATH`, `ref:NAME` scripts; snapshots as deterministic tars; `ARBACE_BOX_FS` seeds the
  in-memory fs. The launcher's host tool `--store DIR put|get|ref|ls`.
- Checks on the branch: `--smoke` 53, `--smoke --box` 86, both oracles as recorded (46),
  `bin/box-suite` as its reference, `bin/jrt test`, `box-vendor roundtrip` and `kvm-tests`,
  `tamago-vendor verify` and `check` (S6's proof holds).
- The user's decisions: BX30-BX36 accepted; the blob limit 256 MiB, the monitor's blob cache to
  be bounded; `arbace.box` on the JVM Arbace too; epoch times in snapshots, no transactions, the
  `store:` path rule confirmed.
- S5: agent, branch `box-s5` (`b6addbc`), done; its merge with S3 conflicts in `box/`'s forms
  (abi, the launcher, vmm), jrt's box host and the docs, so it is merged by an agent that prints
  both sides, merges the Go and converts back, together with S3's two follow-ups.
- S5's decisions by the user: BX40-BX44 accepted; its network semantics kept as built (host name
  `localhost`, wildcard listens bound to loopback unless a pattern allows `*`, a dial to a closed
  own listener refused by policy under `listen` alone, reverse lookups only with `dial=*`); the
  guest's polling (console and network, an idle listening server ~99 polls a second) replaced by
  a wake-on-event in the monitor before the milestone, rather than later; the box's own checks in
  `bin/gate --full` stay deferred.
- Two agents: `box-net-merge` (S5 merged with S3, the monitor's blob cache bounded, the
  wake-on-event) and `box-jvm` (`arbace.box` on the JVM Arbace).

## 2026-10-12: `arbace.box` on the JVM

- Agent, branch `box-jvm` (`af262d9`), merged into main; the essential `bin/gate` passed on its
  tree and again on merged main. Details: BOX-NOTES.md, S3, "The JVM's `arbace.box`"; amendment
  BX37; VENDOR-NOTES hand change 22.
- One shared source, `arbace/box.ae` (the Go-only namespace moved), over a host layer per build
  with the same answer codes: `arbace/box/host.ae` on the JVM (java.nio: temp file and rename,
  hash checked on read, refs under `FileChannel.lock` of `refs/.lock`, which is `fcntl(F_SETLKW)`
  over the whole file in jdk26u's `UnixFileDispatcherImpl.c`, the same POSIX record lock as the
  Go side's, as strace showed; snapshots written byte for byte as Go's `archive/tar` writes
  them), and `arbace/lang/go/ns/box/host.ae` over `HostStore` on Go. `ref:NAME` scripts moved
  into `arbace/main.ae`; the JVM's `RT.load` looks in `ARBACE_PATH`'s `store:` entries as a last
  resort.
- Tests: `test/native/box_test.ae` (6 tests, 73 assertions, run by `bin/native-tests`, so by
  the build and the gate); in `bin/arbace-go --smoke`, the store checks again on the JVM, one
  directory used by both, equal snapshot hashes, CAS counters from two JVMs and two native
  processes ending at exactly 200, the JVM's lock holding a native `ref!`.
- On merged main the first `bin/arbace-go --build` failed in Go's compile of `arbace/lang`
  (undefined `RT_IntCast_O__I` and others): stale build state in the checkout; after the gate
  (which refreshed jrt's conversion) it built (66,814,112 bytes), and the box built and passed
  `--smoke --box`, 86 of 86, with the shared namespace.
- The user: BX37 accepted, and on the JVM `ARBACE_PATH`'s directories are read too, not only its
  `store:` entries, so that it works the same on all three (the same agent, branch
  `box-jvm-path`).
- `ARBACE_PATH` on the JVM: agent, branch `box-jvm-path` (`2dbc212`), merged into main; the
  essential `bin/gate` passed on its tree. `RT.load`, when the class path has no namespace, looks
  through `ARBACE_PATH` as the Go build does (`RT.loadFromPath` → `arbace.box.host/load-from-path`):
  entries in order, directories and `store:` alike, `.ae` in all, then `.clj`, then `.cljc`;
  `bin/arbace-j` appends its directories to the class path (resources, `data_readers.ae`), and a
  source the class path finds in one of them goes through `RT.loadFromPath` so that an earlier
  `store:` entry still wins (`RT.inArbacePath`, in Java: a first version in Clojure recursed while
  `arbace.core` loaded). Started by plain `java -cp`, resources in its directories are not found.
  Tests: `box_test.ae` 75 assertions; the smoke test checks the same order on the JVM and natively.

## 2026-10-12: S5 merged with S3; the blob cache bounded; wake on events

- Agent, branch `box-net-merge` (`9c4b777` the merge of `box-s5`, `582e805`, `28f69fb`, main
  merged in twice, `b076b51`), merged into main. Details: BOX-NOTES.md, S3 (BX38), S5, "Wake on
  events" (BX50-BX54).
- S5 (`b6addbc`, from the S5 agent): a stream proxy, hypercalls 40-52 (`NetInfo` ... `NetLookup`),
  never waiting, the monitor's host goroutines dialing, accepting, resolving and moving buffers of
  up to 256 KiB; the policy `ARBACE_BOX_NET` (`listen`, `loopback`, `listen=` and `dial=`
  patterns of host and port; unset, no network), the socket system calls in the seccomp filter
  only when a network is given; jrt's `netbox_tamago.ae`; `JAVA_TOOL_OPTIONS`' `-D` options had
  never become properties in the box (built before the box's environment): rebuilt in `init`
  (BX43). TLS out of S5: jrt has no JSSE. Packets with netstack rejected (a new vendoring, a TAP
  device or a second TCP/IP stack on the host, the guest's timers under R2).
- The merge: `box/`'s forms conflicted by shifted line numbers only; resolved by printing `box/`
  at the merge base and both sides, `git merge-file` on the Go, converting back and carrying both
  sides' comments; printing `box/` after the merge gives the merged Go byte for byte. The merge's
  oracle run found a bug of S5's: `NetClose` closed the host connection at once and dropped bytes
  still queued (`forms/net.ae:91`, failing 4 of 4 on S5's own launcher); a closed connection now
  lingers until they are written (`TestNetCloseLingers`).
- BX38: the monitor's blob cache bounded, least recently used, `DirStore.CacheMax` 256 MiB by
  default (`ARBACE_BOX_STORE_CACHE`); a larger blob is served from its file, checked each use.
- BX50-BX54, wake on events: a 16-byte event area (posted, taken; call 53, `NCalls` 54) the
  monitor posts to on console input for a waiting reader and on every network change, waking
  parked vCPUs; a park ends or never starts while an event is untaken; a lone idle vCPU parks
  until its next timer or an event; in Go's runtime overlay one line in `findRunnable` readies
  jrt's parked event goroutine (as wasm's runtime does its event handler; a nil check on Linux);
  jrt's console reader and the network's retries wait on a channel instead of polling. Rejected:
  a blocking call (it stops the vCPU and the collector's stop-the-world), TamaGo's netpoll (its
  scheduler never reaches it), readying a goroutine from the board by linkname. Measured: idle
  host CPU per 20 s at the prompt 0.25 → 0.00 s (1 vCPU), 1.10 → 0.00 s (4); a listening server
  1.26 → 0.01 s (4); the socket REPL's round trip 47 → 42 ms (native 41); output latency
  unchanged.
- Checks on the final branch: `bin/build-arbace`, `jrt-convert`, `jrt overlay --check`,
  `--smoke` 69, `--smoke --box` 103, `net-check` and `net-check --box` 5 each, the box oracle with
  `forms/net` 21,779 of 21,825 and the native 22,232 of 22,278 (the 46 known each), `box-suite`
  19,647 of 19,647, `box-vendor roundtrip` 16 and `kvm-tests`, `tamago-vendor verify` and
  `check` (identical), the essential `bin/gate`.
- The user: BX38, BX50-BX54 and the `NetClose` fix accepted. Next, `bin/gate --full` and the
  tag `arbace-box-amd64-v1`.

## 2026-10-12: The milestone `arbace-box-amd64-v1`

- `bin/gate --full` passed on `ac9e54d` in 19m47s: stage 2 = stage 3 (5,777 classes); Clojure's
  suite on stages 1 and 2 20,750 of 20,750; j2a's suite without regressions; g2a's round trip
  for tamago/amd64 and linux/amd64; `box/`'s round trip, go-whim's KVM tests and
  `tamago-vendor verify`; the Go build: smoke, Clojure's suite on its test build 20,506 of
  20,520 without regressions, the oracle with its 46 known mismatches; `box-forms` (its first run
  in `--full`): the guest from `tamago/`'s forms identical to the originals', as linked and cut.
- The annotated tag `arbace-box-amd64-v1` on `ac9e54d`, pushed; its message names the gate's
  result and the toolchains (JDK 26.0.2.1, jdk26u `baf63fb`, TamaGo go1.27.1, goos v1.27.1). No
  branch, no freeze: main's journal continues (the user's decision).
- README: a section "Arbace in the box" (its commands tried: the store's host tool, a script
  run by ref); CLAUDE.md's direction names the milestone and S4 as now; the agenda follows.
- Next milestone: S4, arm64.

## 2026-10-12: B1b S4 started: arm64, towards `arbace-box-arm64-v1`

- The user: S4 leads to the milestone `arbace-box-arm64-v1` (a tag on main after `bin/gate
  --full`, as `arbace-box-amd64-v1`). Agent, branch `box-s4`: the arm64 guest with all the amd64
  box does; KVM/arm64 in an emulated aarch64 VM here (go-whim's `guest/vm/aarch64.sh` the model);
  S6's arm64 half; the Mac.
- The user's decisions: purego vendored as Go forms like `box/` (rather than a pinned module or a
  binding of our own); `tamago/` one tree for both architectures, shared files once (rather than
  a second tree); the Mac's binaries as a tarball built here with a checklist script (rather than
  building on the Mac); the milestone's gate `bin/gate --full` plus the box's checks in the arm64
  VM, the user's Mac run following the tag rather than blocking it (not as recommended).
- S4's phase 1 (the agent, `6724fa1` on `box-s4`; BOX-NOTES.md, "S4"): no blocker. go-whim's
  `aarch64.sh` (QEMU 11.1.2, `virtualization=on`, Alpine netboot, Linux 6.18.42) boots in 32 s
  with /dev/kvm; go-whim's arm64 `hv` tests pass in it; the arm64 box as built since S1 runs on
  KVM/arm64 there (`-e`, the REPL, a future, files, the store tool, a loopback socket; start 13-14
  s against 0.3 s on amd64: emulation about 40x), its one defect nil dereferences (a data abort
  ends the guest). purego for darwin/arm64 is 3 packages, 7,462 lines; std for tamago/arm64
  shares 1,683 file forms with amd64's (41 amd64-only, 52 arm64-only, 11 differing only in
  annotations the printer does not use).
- The user: BX60-BX66 accepted (arm64 nil panics; the darwin launcher, the guest beside it for
  codesigning; purego as `arbace/box/purego` forms; `tamago/`'s arm64 in one tree, the 11 files
  per architecture; `bin/box-arm64-vm`; `bin/box-smoke`; `bin/box-mac-bundle`); the VM's Alpine
  pinned to a release by SHA-256 rather than edge; before the tag only the box's smoke test in
  the arm64 VM must pass (not the oracle subset, about 1-2 h, nor `box-suite`, about 3 h).

## 2026-10-12: B1b S4 merged; the milestone `arbace-box-arm64-v1`

- The agent's branch `box-s4` merged into main (`c6deb8e`; its tree identical to `be8521e`, where the
  arm64 VM's check ran). What S4 made (BX60-BX66, doc/go/BOX-NOTES.md, "S4"): `board.NilPanics`
  on arm64 (BX60: `boot_arm64.s`'s vector turns a data abort below the first page into a call to
  `sigpanic`, as Go's runtime on linux/arm64; NullPointerException where the guest ended before);
  `bin/box-smoke` out of `bin/arbace-go` (`--slow N` for emulation, `--native`);
  `bin/box-arm64-vm` (Alpine 3.24.2 netboot, SHA-256 pinned; QEMU with `virtualization=on`, 16
  vCPUs, 12 GB; ssh by dropbear); `tamago/` one tree for tamago/amd64 and tamago/arm64 (1,759
  files once, 11 per architecture under `tamago/goroot/arch/ARCH/go`); purego v0.11.1
  (github.com/ebitengine/purego, darwin/arm64 files) vendored as Go forms under
  `box/go/arbace/box/purego` (`bin/box-vendor purego`, `purego-check`: the darwin launcher
  byte-identical, without DWARF, to one built from the originals); the launcher on darwin/arm64
  (`bin/g2a build --goos darwin`); `bin/box-mac-bundle` and `box/mac/` (`mac.sh`, entitlements,
  README.txt) for the user's run. The gate's `box` step runs `purego-check`; `box-forms` builds
  and checks both guests.
- The agent's checks: `bin/box-arm64-vm check` passed (38 min): the box smoke test 103 of 103 on
  KVM/arm64 in the VM (4 and 1 vCPUs, the store, shared with the native arm64 executable, rlwrap on
  a pty, nil dereferences), go-whim's hv (4) and vmm (15) tests. amd64 unchanged: `--smoke --box`
  103 of 103, the box oracle subset 21,779 of 21,825 and the native oracle 22,232 of 22,278 (the 46
  recorded). Not run, by the user's decision: the arm64 oracle subset (stopped after about 1 h 40
  min at -j 12: 30-60 min a corpus file emulated) and `box-suite` in the VM. Left: g2a's printer
  places a multi-line parameter list's names on its first line (DWARF's `decl_line` only).
- `bin/gate --full` passed on `c6deb8e` in 19m37s: stage 2 = stage 3 (5,777 classes); Clojure's suite
  on stages 1 and 2 20,750 of 20,750; j2a's suite without regressions; g2a's round trip for
  tamago/amd64 and linux/amd64; `box` (round trip, go-whim's KVM tests, `tamago-vendor verify`,
  `purego-check`); the Go build: smoke, the suite on its test build 20,506 of 20,520 without
  regressions, the oracle with its 46 known mismatches; `box-forms`: both guests from
  `tamago/`'s forms identical to the originals', as linked and cut.
- The annotated tag `arbace-box-arm64-v1` (a milestone, not a freeze: no branch). README's box
  section, CLAUDE.md's direction and layout, the agenda follow. Next: the user's Mac run from the
  bundle (Hypervisor.framework), which follows the tag by the user's decision.

## 2026-10-12: The box on the Mac: the user's run of the bundle

- The user ran `arbace-box-mac-arm64.tar.gz` (`bin/box-mac-bundle`, built from `120da5f`; between
  it and the tag `arbace-box-arm64-v1` only `box/mac/` and docs changed, so the results hold for
  the tag) with `./mac.sh all` on macOS 14.8.7 (23J520), Apple M2 Max, and sent back
  `mac-results.tar.gz`: check, sign, hv and the checklist all status 0.
- go-whim's hv tests 4 of 4 on Hypervisor.framework; the checklist 42 of 42 on 4 and 1 vCPUs,
  among them nil dereferences as NullPointerException (BX60 on HVF), the network on loopback with
  a socket REPL from the Mac, and the store; idle 0.01 s of CPU per 20 s; the start about 0.17 s,
  `(fib 25)` 29 ms. What was untested before (the launcher on macOS, purego's `fakecgo` under the
  renamed path, HVF with this guest) works. Details in doc/go/BOX-NOTES.md, "The Mac run".
- No new tag: the milestone's tag already names this code; the run completes S4. Next: to be set.

## 2026-10-12: The user's decisions after S4

- Next main line: a survey of the standalone Arbace (as BOX-SURVEY before the box): its own
  runtime instead of jrt and Java binary compatibility, the runtime's design (scheduler, GC,
  tail calls), what the box must hold for Arbace to rebuild itself inside it; stages for the user
  to decide before any code (rather than self-hosting in the box at once, TLS/JSSE first, or the
  small items first).
- The box's checks in `bin/gate --full`: the amd64 smoke test only (`box-smoke`, after
  `box-forms`: `ARBACE_BOX_ARCHES=amd64 bin/arbace-go --smoke --box`, 103 checks, about 2
  min); the box's oracle and `box-suite` stay manual (rather than the oracle too, or deferred
  again). Tried alone on main: 103 of 103.
- g2a's printer: fix now the placement of a multi-line parameter list's names (DWARF's
  `decl_line`), so `purego-check` compares with DWARF (rather than leaving it for later).
- The Mac: each box milestone ships a bundle and its checklist; the user's run follows the tag
  (rather than before the tag, or only on request).

## 2026-10-12: TLS in the box through Go's crypto/tls (the user: "do option 2 now")

- The user asked whether TLS (then "once jrt has JSSE") is needed to reach the Claude API from the
  box: yes, the API is HTTPS only. Three ways were put: (1) JSSE converted into jrt, TLS in the
  guest with the Java API (large); (2) Go's crypto/tls, already in TamaGo's std and the guest,
  bound for Arbace, TLS in the guest (small; the key and the encryption stay in the box, the
  launcher carries ciphertext); (3) TLS by the launcher on the host (smallest; the key in clear
  on the host). Recommended and chosen: (2), now, before the standalone survey's decisions.
- Agent, branch `box-tls`: the Arbace surface (a namespace, an HTTPS helper enough for the Claude
  API's messages endpoint), crypto/tls over the guest's stream path with wake on events, root
  certificates embedded (Mozilla's, via golang.org/x/crypto/x509roots/fallback as Go forms,
  likely) with an override for private CAs; natively and on the JVM if cheap; amendments BX70..
  for the user.

## 2026-10-12: The standalone survey (doc/STANDALONE-SURVEY.md)

- Agent, branch `standalone-survey` (`86406a5`), merged. Measured: every Arbace program is still
  built by the JVM (j2a with javac's front end over 836 jdk26u files, a2g over 3,040 classes,
  g2a's printing, gc as a host process); the runtime and the REPL's namespaces use 274 JDK
  classes (closure 422 from 184 files) of jrt's 2,134; the blockers to a Go-hosted bootstrap are
  j2a's javac, a2g's JVM reflection and ASM, and g2a's ProcessBuilder build; g2a's printer runs on
  the Go executable byte-identically (about 7x slower than the JVM); cmd/compile, link and asm
  build for GOOS=tamago unchanged (not run in a guest); gc needs 7-8 GB for `arbace/jrt`, more
  than a guest's 3 GiB. Stages S1-S9 (about 40-70 days; the Go-hosted bootstrap S1-S4 11-21) and
  decisions D1-D13 for the user, recommendations in the document.

## 2026-10-12: The user's decisions on the standalone survey (D1-D13)

All as recommended in doc/STANDALONE-SURVEY.md:
- D1 the Go-hosted bootstrap first (S1-S4), then the box (S5-S8), rather than the box or the
  runtime first. D2 j2a's output (the JDK's forms, 909 files, 11 MB) committed as sources,
  `bin/jrt-convert` a verify check (reversing "regenerated each build, untracked"). D3 the seed
  becomes the fixpoint's printed Go source pinned by hash (rather than executables per
  architecture or the jar). D4 after S4 the JVM stays on main only as the oracle's reference (the
  frozen jar) and j2a's front end.
- D5 R1: Go's runtime kept, `arbace.lang` by a2g, jrt slimmed, Arbace compiled to Go forms
  (rather than a new Go-shaped runtime, or Arbace's own collector and scheduler now). D6 tail calls
  staged: T1 now, T3 measured in S6, T4 with an own back end. D7 jrt as a core and a library, in
  Go packages split by the dependency graph. D8 smaller packages first; a larger guest slot only
  if needed.
- D9 Go's tools in the box as tool guests through a spawn call. D10 source trees in the store as
  a blob per file plus EDN tree maps, to and from git by the host tool. D11 `String` stays UTF-16.
  D12 j2a a host tool now, javac translated after S6. D13 divergence from Clojure recorded, each as
  an amendment and a known oracle mismatch, Clojure's suite through c2a the yardstick.
- S1 started now (the user), as one agent beside the TLS and g2a agents: branch `standalone-s1`.

## 2026-10-12: g2a's printer: names of lists that span lines; purego-check with DWARF

- Agent, branch `g2a-params` (`5e0889e`, `1e61959`), merged. Cause: the forms already record a
  name's `:line` where it begins a Go line, but `arbace/g2a/print_code.ae` printed a group's names
  on its first line (gc declares a parameter at its name's line, so DWARF's `decl_line` moved).
  The printer now puts each name of parameter, result, type parameter and struct field groups and
  const/var specs on its recorded line; names of equal type, doc and tag group (a doc no longer
  printed twice); a signature keeps `)` on the results' line; a spec's or return's values follow
  their `:go/breaks`. The forms are unchanged (SPEC.md, `box/`, `tamago/` too).
- `bin/box-vendor purego-check` links with DWARF (no `-w`): the darwin launcher identical to the
  originals'. `test/g2a/golines` compares every declared name's line; new printer case
  `test/g2a/cases/s10_names`. Checks: g2a's printer tests, the round trip for tamago/amd64 and
  linux/amd64 without regressions, `box-vendor roundtrip` 20/20, `tamago-vendor verify` and
  `check` identical, the essential `bin/gate` passed (6m46s). Left: golines over std 1,622 of
  1,742 files (from 1,574); the 120 others are local `var ( )` groups and labels one line off and
  members of types held in a declaration's `:tag`, none affecting `decl_line`.

## 2026-10-12: The next milestone: `arbace-seed-is-go-v1`

- The user: the next milestone is the completion of the standalone survey's S4, tagged
  `arbace-seed-is-go-v1`: the seed of the bootstrap is the fixpoint's printed Go source pinned by
  hash (D3), the build reaches the fixpoint on the Go executable (S3), and the JVM stays on main
  only as the oracle's reference and j2a's front end (D4). As the box milestones: a tag on main
  after `bin/gate --full`, not a freeze (no branch; the journal continues).

## 2026-10-12: Standalone S1: the JDK's forms committed in `jdk/`

- Agent, branch `standalone-s1` (`78d2ee0`, `1a28937`), merged (`ff53e17`). j2a's output is
  committed (D2) in a new top-level `jdk/` (beside `tamago/`, rather than under `overlay/`, which
  holds hand-written input): `jdk/forms/` 909 class-forms files (202,034 lines, 10.4 MB),
  `jdk/data/` the 10 resource data files, `jdk/sources.txt`, `jdk/provenance.edn` (jdk26u
  `baf63fbe42b8758448fee570a9d9bb914272d259`, clean; JDK 26.0.2.1 for j2a, 25.0.4 for the
  generators; j2a's version as a SHA-256 over `arbace/j2a` and `bin/j2a`), `jdk/README.md`;
  `.gitattributes` marks them generated and binary. Packed: 2.2 MB.
- a2g and `bin/a2g` read `jdk/forms` by default (`JRT_CONV` overrides); `bin/jrt-convert` gains
  `verify` (afresh into `.tmp/jrt/jdk`, `diff -r`) and `update`; `bin/gate --full`'s step is now
  `jrt-convert verify`; the gate's cache key hashes `jdk/` and drops what only the conversion
  read; LICENSE.md covers `jdk/`.
- Proof: the builds (`--build`, `--build-tests`, `--build --box` for amd64 and arm64) in a mount
  namespace with jdk26u's sources and build hidden and javac a failing stub, all 539 execs traced:
  no javac; the 8 executables byte-identical to those from a fresh conversion; against main's
  unchanged pipeline a2g's program tree identical. `--smoke` 96 of 96, `--smoke --box` 103 of 103,
  the essential gate passed (5m23s), `jrt-convert verify` passed (175 s) and fails on a tampered
  file. Left for S2: a2g on the JVM (reflection, ASM); the test build's fixtures still through j2a.
- The essential `bin/gate` on main after the merge (S1 with the g2a printer fix): passed (5m36s).

## 2026-10-12: TLS in the box merged (BX70-BX75); the printer's expression lists

- `g2a-cases` merged (`6a6d1ab`, `8665f81`): the printer keeps recorded breaks in case lists,
  returns, `:=` targets and range variables (`comma-list`); a plain `:go/breaks` entry means the
  line after the last line a form began on, as the converter means it; `})` stays on the
  literal's line. Found by the TLS branch: its guest links `net/url` (through `crypto/x509`), whose
  `validUserinfo` case list spans two lines, so the guest from forms differed by one
  `.gopclntab` line. Checks: printer tests, g2a's round trips, `box-vendor roundtrip`,
  `tamago-vendor verify`/`check` (also on the TLS branch merged with it), the essential gate.
- `box-tls` merged (`b080c18`, `7c0b994`, `6b4c44f`; BOX-NOTES "TLS (option 2)"). The user's
  decisions: BX70-BX73 accepted as built (TLS in jrt's socket table over the host's connection,
  `jdk.internal.jrt.TlsSocket` upgraded in place, no new hypercalls, the launcher carries
  ciphertext; namespaces `arbace.tls` and `arbace.http` on the JVM, natively and in the box,
  verification always on; Mozilla's roots embedded, replaced by `ARBACE_TLS_ROOTS` or `:roots`;
  `x509roots/`, golang.org/x/crypto/x509roots/fallback's `bundle` at `3b414e3e8284` as Go forms,
  `bin/x509roots-vendor`); BX74 no `javax.net.ssl` in the Go build for now; BX75 the checks
  (`tools/tlsserver`, `bin/lib/tls-checks.bash`). LICENSE.md: the certificates also under NSS's
  MPL-2.0 (the user, rather than BSD-3 only); merged after the printer fix (the user).
- The branch's checks: a request to https://api.anthropic.com/v1/messages without a key answered
  401 (`authentication_error`) from the box, natively and on arm64 under qemu, TLSv1.3 verified by
  the embedded roots; `--smoke` 83 of 83, `--smoke --box` 111 of 111, the native and box oracles
  their 46 known mismatches; idle CPU unchanged; the executables +2.4 MB (crypto/tls and x509
  linked for the first time, 128 KB of roots).
- On main after both merges (`2035f7b`): the essential gate passed (5m59s), `jrt-convert verify`
  and `x509roots-vendor verify` byte for byte; `tamago-vendor check` first reported both guests
  DIFFERENT: the printed tree's cache key (`bin/tamago-vendor`'s `tree_key`) hashed the forms and
  TamaGo's version but not the printer, so a tree printed before the printer's fixes was reused.
  Fixed: the key also hashes `arbace/g2a` and `bin/g2a`. Then both guests identical, as linked and
  cut; `--smoke --box` 111 of 111.

## 2026-10-12: Work beside S2; S4's preparation and the user's decisions SA1-SA9

- Beside S2, by the user's question ("any steps doable concurrently?"), three agents: g2a on the
  Go executable, S3's half that does not wait for S2 (branch `s3-g2a`); the test build's Java
  fixtures committed as forms (branch `test-fixtures-forms`); S4's preparation (branch `s4-seed`).
  The TLS agent's leftover wait (an arm64 VM smoke in its removed worktree) was stopped; the TLS
  checks run in `bin/box-arm64-vm smoke` at the next box check.
- S4's preparation (`7498921`, `6aac846`; doc/STANDALONE-NOTES.md "S4: the Go-source seed"): the
  seed `arbace-seed-go/` (1,148 files, 29.2 MB: `SEED.edn`, the printed module, the overlaid
  runtime files, `namespaces.txt`; one seed for amd64 and arm64), `bin/seed --go-check`,
  `--go-make`, `--go-build`, `--go-verify`; from today's main, two generations byte-identical
  (3,063,796 bytes as tar.xz), stage 0 from it identical to `--build` on amd64 and arm64.
  `bin/oracle` takes `ARBACE_ORACLE_REF=frozen`: the seed jar (v1) is unsuitable (the NaN
  comparison fixed in v2, 8 cases), so the reference is `arbace-for-java-26-v4` built from its
  tag, pinned (`test/oracle/frozen-jar.sha256`); against it the native executable shows two real
  divergences of main (loaders' `.ae` message; MultiFn's ordering since `ac8b971`), recorded in
  `test/oracle/known-go-amd64-frozen.edn`. An inventory of what leaves main at S4.
- The user: SA1-SA3, SA5-SA7 accepted (one deterministic `seed/arbace-seed-go.tar.xz` with its
  hash, the image not carried, the version map recorded, the modes renamed at S4's end, the seed
  committed once at S4's end, printed by the Go-hosted fixpoint); SA4 the toolchain pinned by
  version and a hash of GOROOT's sources (rather than the version only); SA8 v4 built from the tag
  (rather than its jar committed); SA9 the frozen reference the gate's at S4, the expected files
  re-recorded then, and the stale `test/oracle/expected/forms/files.edn` fixed now.

## 2026-10-12: Standalone S2: a2g without the JVM's reflection and ASM; the test fixtures as forms

- Agent, branch `standalone-s2` (`adc9216`, `17f3a81`, `f80c5c2`, `c9857ac`), merged (`2b0c249`).
  The edge `jdk/edge.edn` (1.40 MB, 403 classes, about 10,300 members, in `arbace.classes.env`'s
  shape, with the analyzer's other facts and a2g's own questions): the JVM's answers a2g needs,
  recorded from the builds' two a2g runs; a2g reads it by default (`--edge`, `--record-edge`,
  `--jvm-reflection`); a class in neither the forms nor the edge does not exist. Accessors as
  data in the class state, read by `arbace/a2g/access.ae` without ASM. The id floor: a2g sets
  `RT/nextID` to 100000 before it starts. `bin/a2g-edge update|verify|trace` (`trace`: JFR's
  method tracing; 25,763 calls inside a2g's run, 0 by a2g, against 13.3 million with
  `--jvm-reflection`). a2g's run 79-89 s against 93-96 s.
- Proofs: against main's a2g with the floor alone, a2g's output identical apart from the two
  embedded analyzer sources, all eight executables identical (native, test, box launcher and
  guest, amd64 and arm64); two builds reproducible; stage 2's `arbace/lang`, `asm` and
  `java/api` unchanged; `--smoke` 117 ok, `--smoke --box` 111, the oracle its 46 known
  mismatches, the essential gate passed; edge drift from the TLS merge caught and updated.
- The user: SA10-SA15 accepted as recommended (the notes in doc/STANDALONE-NOTES.md; the edge
  recorded from a2g's real runs, in `jdk/`; whole classes with the JVM's order where stable; the
  id floor 100000; the exit criterion met as proven, rather than literal identity; `bin/a2g-edge
  verify` in `bin/gate --full`). The branch numbered them SA1-SA6; renumbered at the merge, since
  S4's preparation's amendments are SA1-SA9.
- Agent, branch `test-fixtures-forms` (`f56e3d3`), merged: Clojure's suite's Java fixtures as
  class forms in `test/java-fixtures/` (15 files, 32 KB; from clojure/clojure `98d735f`
  `test/java`, renamed by c2a; `provenance.edn`, README; EPL-1.0 in LICENSE.md);
  `bin/clojure-tests --java-fixtures verify|update`; `--build-tests` reads them; `bin/gate
  --full` verifies them before the test build. With javac stubbed and execs traced: no javac or
  j2a; the test executables identical to those from today's conversion; the suite on the test
  build 20,506 of 20,520, no regressions; the essential gate passed (6m32s). The test build
  needs no javac.
- On main after both merges (`a3f3913`): the essential gate passed (5m50s; a first run failed when
  the kernel killed a2g's JVM for memory, beside the suite's JVMs and two agents' builds, 36 GB),
  `bin/a2g-edge verify` and `bin/clojure-tests --java-fixtures verify` passed.

## 2026-10-12: S4's preparation merged

- Branch `s4-seed` (`7498921`, `6aac846`, `fb4ef2d`, `da504d5`, `bd0934c`, with main merged),
  merged. SA4 as the user chose: GOROOT's hash, the SHA-256 over sha256sum's lines for `VERSION`
  and every file under `src/` sorted by path (C locale), non-regular files refused; TamaGo
  go1.27.1: 12,175 files, `5745170fd3c83cb77ec88bc50aee8606f0436e718c222b48376b5943ac0cebe5`,
  pinned in `seed/goroot.sha256` and `SEED.edn`'s `:goroot-sha256`; `--go-make`, `--go-build`,
  `--go-verify` refuse a mismatch (checked with one changed runtime file); 0.65 s warm, 3.6 s
  cold. `test/oracle/expected/forms/files.edn` re-recorded from main's JVM reference: exactly the
  six cases of the `.ae` rename. On the branch after merging `329af1a`: the essential gate passed
  (6m06s); a candidate seed (3,063,800 bytes) as pinned, stage 0 from it byte-identical to
  `--build` on amd64 and arm64 (19 s); the oracle its 46 known mismatches. The candidate is not
  committed (SA7).

## 2026-10-12: S3's g2a half merged

- Agent, branch `s3-g2a` (`b07b644`, main merged at `9770f9a`, `b152043`, `43073ad`,
  `1a5f1d5`), merged as `d9aaf0a` (doc/STANDALONE-NOTES.md: both sides' sections kept). g2a's
  printer, `bin/g2a build`'s driver and the converter run on the native Go executable:
  `G2A_HOST=jvm|go` (`bin/lib/g2a-host.bash`; the JVM the default), the executable `G2A_GO_EXE`
  or `target/arbace-go/<arch>/arbace`, run from a copy in `.tmp/g2a-host/`, g2a loaded from
  source (`arbace -m`). Proofs: `bin/arbace-go --build` and `--build --box` for amd64 and arm64
  under both hosts print equal modules and give 8 identical executables; `tamago/` prints equal
  on Go, on the JVM and by main's printer; `bin/tamago-vendor check` both guests identical;
  `bin/g2a-print-tests` 268/268; the round trips no regressions; the essential gate passed on
  the branch (5m36s).
- Speed: the program's printing 378 s / 6.2 GB → 32.3 s / 4.6 GB on Go, 23.8 → 12.1 s on the
  JVM; `tamago/` std 235 → 21-24 s on Go. Causes fixed in `arbace/g2a`: forms read twice and
  serially (now once, in parallel); files of a package and chunks of a long file printed in
  parallel, a chunk joined only when the printer's state matches what it assumed, else printed
  again in place (so equal to the sequential text); a cubic rebuild of operator chains
  (`butlast` per prefix: x/text's tables 178 s → 8.9 s on Go); smaller costs in
  `go-printable?`, escaping, `sp`/`vtab`, alignment. The converter on Go passes the round trip
  but takes 207 s (36.7 s on the JVM), peaking at 13 GB: not tuned.
- Runtime fix (`arbace/lang/go/Compiler.ae`): the evaluator recompiled a function's method on
  every variadic call or `apply` instead of using its cache (9% of std's printing); the native
  oracle unchanged (22,232 of 22,278, the 46 known).
- Amendments SA30 (`G2A_HOST`), SA31 (parallel printing), SA32 (the cache fix) proposed to the
  user. Left: a2g on Go and the G1 → G2 → G3 chain (branch `s3-fixpoint`); the remaining 2-3×
  over the JVM is the evaluator itself (AOT-compiling the tools would need a2g to embed
  `arbace/g2a`); the reader on Go is the slowest part of reading.
- The user: SA30-SA32 accepted as recommended (`G2A_HOST`, the JVM the default until the
  fixpoint; parallel printing of files and chunks; the cache fix).
- On main after the merge (`d9aaf0a`): the essential gate passed (5m43s; stage 2 = stage 3,
  5,787 classes).

## 2026-10-12: no use of the host's Go

- The user: make Arbace independent of the system's Go (Alpine's `golang` package, today
  go1.27.1; an upgrade to 1.27.2 should change nothing). Survey: every build, the seed's tooling,
  g2a (`$G2A_GOROOT/bin/go`, `gofmt`), godump and gocmp (`-go`), `bin/arbace-go`, `bin/jrt`,
  `bin/tamago-vendor`, the TLS checks and `bin/box-mac-bundle` name TamaGo's go1.27.1
  (`/root/tamago-go`, pinned by `seed/goroot.sha256`) explicitly with `GOTOOLCHAIN=local`. The
  one exception was `bin/box-vendor`: go-whim's monitor builds (`bins`), its KVM tests and the
  arm64 test binaries ran the host's `go` from PATH. Now it exports `GOROOT=$tg` and puts
  `$tg/bin` first on PATH (TamaGo's linux and darwin builds are upstream Go's). Considered:
  keeping the host's Go there to test go-whim with upstream Go; rejected, since a host upgrade
  would silently change what the gate tests.
- Checked with a failing `go` first on PATH and `GOROOT=/usr/lib/go`: `bin/box-vendor
  roundtrip` and `kvm-tests` passed, the stub never called. Found on the way, not caused by
  this: `bin/box-vendor convert` (S0's vendoring, outside the gate) fails on
  `cmd/arbace-box`, the launcher added in S1, which go-whim's clone lacks; the same at `f8e95e4`.

## 2026-10-12: the Alpine environment written down

- The user: a minimal `apk add` line mirroring this host's 560 packages, and a doc of the
  environment. `doc/ALPINE.md` and `bin/alpine-env`.
  - The line is `/etc/apk/world` (84 entries) less the 17 that other entries' dependencies
    install: 67 names. Their closure is 485 packages; the other 75 come by `install_if`
    (`-openrc`, `-pyc`, `git-perl`, `gcc-gdb`...).
  - `bin/alpine-env check` proves it offline from `/lib/apk/db/installed`.
  - `simulate` (`apk add --simulate` into an empty root against edge) gave the same 560 names;
    18 had newer versions, edge having moved since the host's upgrade of 2026-10-10. Edge has no
    snapshots, so the line mirrors names, not versions.
  - Considered: world as is (84 names, 17 redundant), and `apk info -v` with versions (not
    installable from edge once it moves).
- The doc also gives the subset Arbace's scripts use, as one line, derived from `bin/` but not
  tried on a fresh host. It also lists the inputs apk does not provide:
  - jdk26u at `baf63fbe42b8758448fee570a9d9bb914272d259`, built headless with openjdk25 as the
    boot JDK;
  - TamaGo's go1.27.1, pinned by `seed/goroot.sha256`;
  - go-whim at `80c49bf`;
  - Go's module cache under `GOPROXY=off`: tamago v1.27.1, purego v0.11.1, x509roots'
    fallback and go-whim's requirements;
  - the jars and netboot fetched into `.tmp/`;
  - /dev/kvm.

## 2026-10-12: the host upgraded

- The user ran `apk update && apk upgrade`: 18 packages (util-linux's, libcap2, fakeroot,
  abuild, libfdt, linux-virt 6.18.55 → 6.18.56), none of Arbace's inputs (the JDKs, TamaGo's
  Go, gcc, qemu-aarch64, python3, git unchanged). Still 560 packages; `bin/alpine-env check`
  holds, the line unchanged (67). The kernel runs 6.18.52 until the reboot after the milestone.

## 2026-10-12: overnight authorization (the user)

- The user, before a night away, as recommended:
  - Amendments from SA20 are merged with each agent's recommended choice applied and recorded
    as provisional, for the user to confirm or revise.
  - S4's final step may run unattended once S3 is merged and the essential gate passes: the
    seed printed by the fixpoint committed, the JVM's products removed per the accepted
    inventory, the gate's oracle on the frozen reference.
  - Then `bin/gate --full`, the annotated tag `arbace-seed-is-go-v1` pushed, and a ping. A
    failing gate stops before the tag.

## 2026-10-12: S3, the Go-hosted fixpoint, merged

- Agent, branch `s3-fixpoint` (head `91aa0b0`, main `aea0b98` merged), merged as `5bf08af`.
  - `bin/arbace-go --fixpoint` on amd64 (48m46s, at `f293ba7`): G1 is built by the JVM chain,
    G2 by G1 with a2g and g2a on the Go executable, G3 by G2. a2g's trees and the printed
    modules are equal for all three (`diff -r`, `report.edn`'s times aside).
  - G1 = G2 = G3 byte for byte: 69,202,080 bytes,
    `2f19b3d1c64525db0b11e71c58a7aca750d95f35da8fb6a0ad3f9b6388540f8d`.
  - A seed packed from G3 by `ARBACE_SEED_GO_BUILD=... bin/seed --go-make` (dry run, not
    committed): 3,069,608 bytes, 1,148 files. G3's build is kept in `.tmp/s3-fixpoint/g3` for
    S4.
- What kept a2g off Go:
  - javac's constructor helpers lived in the bytecode back end; they move to
    `arbace.classes.analyze` (SA22: stage 2's other classes unchanged, stage 2 = stage 3).
  - The JVM's module-layer query moves to `arbace.a2g.jvm`, loaded on demand (SA23).
  - `java.util.zip.Deflater`, whose zlib bytes Go cannot reproduce, is replaced by a DEFLATE
    compressor in Arbace, `arbace.a2g.zlib` (SA21): 0.5-0.7% larger output, the executable
    +8 KB.
- Speed: a2g on Go is 1,365-1,377 s against 79 s on the JVM, peaking at 9.6 GB (`GOGC=400`,
  `GOMEMLIMIT=12GiB`). Two evaluator faults were found:
  - calls through a seq recompiling, the same as SA32 (the agent's identical fix dropped at the
    merge);
  - static fields looked up by name on every read, 4%; `CodeStaticField` fixes it (SA24).
  - The rest is the evaluator, spread thin; S6 is the remedy.
- Checks: `--smoke` 83 ok; the oracle on G3 22,232 of 22,278 (the 46 known); the essential gate
  passed on the branch (5m19s).
- Amendments SA20 (`A2G_HOST`, `ARBACE_GO_HOST`), SA21 (the compressor), SA22 (the helpers in
  analyze), SA23 (`arbace.a2g.jvm`), SA24 (static field reads) and SA25 (`--fixpoint`, the seed
  from a named build): the recommended choice applied, provisional per the user's overnight
  decision.
- Left for S4:
  - the Go-hosted build as the default, the chain starting from the seed's stage 0;
  - `--build-tests` and `--build --box` on Go;
  - A2G-SPEC §10.3 and JRT-NOTES SZ2 to name the new compressor.
- On main after the merge (`5bf08af`): the essential gate passed (5m21s).

## 2026-10-13: S4 done, the seed is Go

- Agent, branch `s4-final` (from main `6cb552a`). It worked as five branches, merged into
  `s4-final` and then removed (doc/STANDALONE-NOTES.md, "S4: the JVM removed", holds the full
  account and SA40-SA88):
  - the coordinator: the seed's modes, `bin/lib/host.bash`, the Go-hosted `bin/arbace-go`, the
    gate, the JVM's products and the JVM-only namespaces;
  - `s4-a2gpar`: a2g's classes translated in parallel, deterministically. Go-hosted a2g went
    from 1,365 s to 242-258 s and a build from about 23.5 to 4.8 minutes, with byte-identical
    output.
  - `s4-frozen`: what stays on a JVM runs on the frozen v4 jar (`bin/lib/frozen.bash`): j2a,
    j2a-check (forms-level, and the suite on the jar's own compiler), jrt-convert, the edge's
    recording, the fixtures, the oracle's reference, the data generators. The oracle's expected
    files were re-recorded from v4 (SA9). The native tests moved into the oracle
    (`forms/native.ae`, 269 cases).
  - `s4-tools`: the tools on the Go executable (`arbace_tool`): c2a, the suite's runner, g2a,
    a2g's checks, the vendoring tools.
  - `s4-backend`: ASM (45 files, 14,747 lines) and the bytecode back ends removed
    (`Compiler.ae` 8,717 → 5,525 lines; `classes/{emit,compiler,shape,verify,build,boot}`,
    `Compile`, `DynamicClassLoader`, `Intrinsics`). The access flags moved to
    `arbace.classes.opcodes` (O3). The Go variants became the sources. `gen-class`, `defmodule`
    and `defpackage` are no-ops (D13).
  - In all, 132 files and 25,474 lines deleted.
- The agent's runs were cut short by hand-backs three times, so I finished the last steps on
  `s4-final`:
  - The fixpoint at `ed892a1` (hosted by S3's G3, `2f19b3d1…`, whose older analyzer makes host
    ≠ G1): G1 = G2 = G3, `f114b6bd943a508b8cb6e3b0bb8d7b0728d35b531fd612326ee4fb40073fce8a`
    (68,972,704 bytes), about 275 s a build.
  - The seed packed from G3 and committed alone (`eaea7dd`): `seed/arbace-seed-go.tar.xz`,
    3,058,352 bytes, 1,145 files, SHA-256
    `8ad2fb89b0ef7bf7923b59f9f6ec951d66791363f6c30c3c23ae8167a000e072`, `:commit ed892a1`,
    `:dirty false`.
  - `bin/seed --verify` (14m48s): printed again from the seed's own stage 0, the same tree byte
    for byte; stage 0 = G1 = G2 = G3.
  - The essential gate (13m29s): the suite on Go 20,506 of 20,520, no regressions; the oracle
    against v4 22,490 of 22,547, the 57 known, 0 new.
  - The box: `--build --box` for amd64 and arm64; `tamago-vendor check` both guests identical;
    `--smoke --box` 111 ok.
  - Proofs in STANDALONE-NOTES (`966a804`). Merged into main as `c41f00f`.
- Housekeeping: seven stale wait loops of the helper agents were stopped. They matched their
  own command lines in `pgrep -f`, so they would never end.
- Known issues, in the agenda:
  - two Go-build bugs exposed by the moved native tests (a top-level `do` of mutually
    referring `defclass`es; a class forms `lambda` in a handed-over fn);
  - `reflect_tables.ae` stale against `bin/jrt manifest`;
  - the box's known-mismatch file not run on the box.
- The milestone (the user's, 2026-10-12): `bin/gate --full` on `c41f00f` passed (41m41s). It
  ran with a failing `go` first on PATH and `GOROOT` unset of the host's, and the stub was never
  called.
  - Its steps: the seed, stage 0, the Go-hosted build, the smoke test, the suite on Go, the
    oracle against v4, `j2a-check --suite` (20,718 of 20,750, no regressions), `jrt-convert
    verify`, `a2g-edge verify`, `--java-fixtures verify`, the fresh build, the box's guests
    identical to TamaGo's, the box's smoke test, g2a's round trips, the box's vendoring checks
    and go-whim's KVM tests, `tamago-vendor verify`.
  - The fixpoint from stage 0: G1 = G2 = G3 (`1ac98441…`; host ≠ G1 only by the linked
    version, `c41f00f` against the seed's `ed892a1`).
- The annotated tag `arbace-seed-is-go-v1` on `c41f00f`, pushed: a milestone on main, not a
  freeze (no branch; the journal continues). The user's provisional amendments (SA20-SA25,
  SA40-SA88) and the host's reboot follow.
- Correction to the entry above: the full gate ran with the failing `go` first on PATH only;
  `GOROOT` was not changed (the scripts set it to TamaGo's themselves).

## 2026-10-13: the provisional amendments accepted

- The user accepted, as applied, SA20-SA25 (S3's fixpoint) and SA40-SA88 (S4), all with the
  recommended choices. Asked about separately:
  - SA85 `gen-class`: a no-op, so namespaces with `(:gen-class)` load (`-main` through `-m`);
    the alternatives were throwing, or a no-op in `ns` that throws when called.
  - SA45 the JVM-only namespaces (`arbace.xml`, `inspector`, `java.browse`, `javadoc`,
    `repl.deps`, `tools.deps`, `java.basis`): removed, each ported when a use appears; porting
    `arbace.xml` now (over Go's `encoding/xml`) was the alternative.
  - Marked "Accepted." in doc/STANDALONE-NOTES.md.
- The user asked for the two Go-build bugs to be fixed; an agent is on it (branch `go-bugs`).

## 2026-10-13: two Go-build bugs fixed

- Agent, branch `go-bugs` (`3de0fde`, main `88498b1` merged `bb0eed2`, `f8e8437`), merged as
  `284313a`. Account in doc/STANDALONE-NOTES.md, "After S4: two Go-build bugs".
  - A top-level `do` of mutually referring `defclass`es (`native.ae:29`, `:32`, `:50`).
    `arbace.classes.interp/define!` built every method's interpreter tree at definition, and a
    call needs its target, so a sibling defined by a later form of the do was missing; the JVM
    resolves names at first run. Now a body that refers to a sibling not yet resolvable is built
    at its first call (`*eager*`, the pending signal, `Meth.ensure`). A static initializer that
    refers to one becomes a lazily built `<clinit>`. A sibling's type in a member's signature is
    `Object` to the host (SA90).
  - A class forms `lambda` called from a handed-over fn (`native.ae:184`):
    `CF$CallV` (`arbace/lang/go/ClassForms.ae`) stored the receiver at the callee's `self` slot
    (-1 for a lambda) and never copied the captured values. Now `CallV.enter` builds the frame
    as `Meth.invoke` does.
  - 11 new oracle cases (`native.siblings`, `native.ae:727-772`), expected values recorded from
    the frozen jar. The 4 fixed cases left both known-mismatch files; one was added
    (`native.ae:745`, reflection reports `Object`, SA90).
  - Checks: the Go build (7m29s), `--smoke` 60 ok, the oracle 22,504 of 22,558 (the 54 known,
    0 new), and the essential gate on `bb0eed2` (13m38s; the suite 20,506 of 20,520, no
    regressions). main had not moved, so the merge is the gated tree plus a notes commit.
- The user accepted SA90 (a sibling's signature type as `Object` now; republishing members
  later if reflection needs it), SA91 (only the bodies that refer to an undefined sibling
  are built on first call) and SA92 (an `ExceptionInfo` when a sibling is never defined).
- The runtime changed, so the committed seed is no longer the program's fixpoint print; by the
  user's practice the seed is remade at the next milestone.
- Correction to the line above: remaking the seed only at a milestone was my instruction to the
  agent, not a decision of the user's. Whether and when to remake it is open.

## 2026-10-13: the host rebooted into 6.18.56

- The user rebooted the host after the milestone, as planned. It now runs kernel 6.18.56-0-virt;
  `/dev/kvm` is present and `kvm_amd`'s nested is 1.
- `ARBACE_BOX_ARCHES=amd64 bin/arbace-go --smoke --box` gave 111 ok, no failures (218 s), as
  before the reboot.
- Earlier that morning, outside the repository, at the user's request:
  - the host's leftovers were cleaned: Arbace's retired JVM products and stale work in
    `target/` and `.tmp/`, Go's build cache trimmed to six hours, Arbace's error reports in
    `/tmp`;
  - go-whim's, slim-vim's, ghc-lisp's, go-lisp's and rust-lisp's scratch and build output were
    removed, their hand-written configuration kept;
  - `spinnerTipsEnabled: false` was moved from ten repositories' `.claude/settings.local.json`
    into the global `~/.claude/settings.json`. For Arbace's `.claude/` this was an exception the
    user approved.
  - Disk went from 53% to 38%.
