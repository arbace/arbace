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
