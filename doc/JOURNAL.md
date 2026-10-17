# Journal

An append-only log of important decisions and actions, oldest first. Never edit past entries; add
a new entry to correct or supersede one. The goal is that the progress could be recreated from this
log.

This journal starts over after the milestone `arbace-inside-itself-v1`, by the user's decision
(2026-10-15), as it did at the two freezes. The history before it is on the frozen branches, at
the tag and in git history:

- 2026-10-06 to 2026-10-08 (the class forms, j2c, the vendored and self-hosting Clojure, the work
  for Java 26, the g2c survey): on
  [`arbace-for-java-26`](https://github.com/arbace/arbace/blob/arbace-for-java-26/doc/JOURNAL.md),
  tag `arbace-for-java-26-v1`;
- 2026-10-08 to 2026-10-11 (milestone B1a: g2c, c2g, jrt, the evaluator, the Go executable and
  its feature completion, the freeze of the Go build): on
  [`arbace-for-golang`](https://github.com/arbace/arbace/blob/arbace-for-golang/doc/JOURNAL.md),
  tag `arbace-for-go1.27.1-v1`;
- 2026-10-11 to 2026-10-15 (the `.ae` sources, c2a, B1b the box and its milestones
  `arbace-box-amd64-v1`, `arbace-box-arm64-v1`; the standalone Arbace S1-S8 and the milestones
  `arbace-seed-is-go-v1`, `arbace-inside-itself-v1`): at the tag
  [`arbace-inside-itself-v1`](https://github.com/arbace/arbace/blob/arbace-inside-itself-v1/doc/JOURNAL.md)
  (main's `e0ad476`).

## 2026-10-15: the journal starts over

- The user's decision, after the milestone `arbace-inside-itself-v1` (tag on main, not a freeze:
  no branch): this journal starts over; the earlier one, 1,535 lines from 2026-10-11 to the
  milestone's entry, is at the tag (linked above). CLAUDE.md's Records paragraph names the three
  places of the history.
- Still running at the reset: the whole chain in the emulated arm64 VM, nothing given (BX117 (a),
  `.tmp/s8c/run.sh` at `434021a`, started 2026-10-14 22:49): its namespaces compiled in 8,839 s
  (127 times the amd64 box's); its result is recorded here when it ends.
- Next, the user's order: the docs searched for sloppiness, then the build's speed (the agenda's
  "Later": the builds keep 5 to 9 of the host's 64 cores busy).

## 2026-10-15: the docs searched for sloppiness

The user's order: four read-only audits of the repository's Markdown (35,940 lines; README,
CLAUDE.md, `doc/`, `box/README.md`, `tamago/README.md`, `LICENSE.md`'s coverage), then four
fixes on branches `docs-1` to `docs-4`, one set of files each, merged (`8d8e87c`, `36e1356`,
`5ccdf7e`, `f0dd320`); the state documents (the agenda rewritten, CLAUDE.md's direction, the
README) on main (`0df2cdf`). About 120 findings, of these kinds:
- present state left stale: statuses and "Left"/"Next" lists of work since done (S4-S8, the
  Mac's run, HV1-HV9's decisions, BX12-BX17, AOT "not scheduled" though the default since S6),
  sizes and counts given as current (the seed 3 MB, now 5.4; 75 MB, now 101.3; jdk's 835 files,
  now 838);
- names that no longer exist: `bin/arbace-j`, `bin/arbace-box`, `bin/java-surface`,
  `arbace/lang/go/ns/` and the hand-written variants merged by SA150, `test/arbace-results.edn`,
  `seed/arbace-seed.jar`, `bin/seed --go-*`, `known-box-amd64.edn`;
- the tools described as loaded from the checkout, though compiled into the host since S6
  (SA133), also in the headers of `bin/a2g`, `bin/g2a-print`, `bin/lib/g2a-host.bash`,
  `bin/lib/tools.bash`;
- references to journal entries now at the tag `arbace-inside-itself-v1` or on the frozen
  branches (one quoted title renamed by the tools' rename: "g2c's open questions");
- `LICENSE.md`: the Go-source seed was covered by no entry (added: its parts and the licenses
  they carry, each one already recorded); `box/`'s `tool` and `tree` added as Arbace's own;
- typos ("a a2g", 17 times), a garbled sentence, missing blank lines before headings.
Kept as decided: the user's git identity where the docs name the commits' author; public
endpoints; HOST-VM-SURVEY's list of absent devices (no provider named). Dated history was
annotated, not rewritten. Left: `arbace/c2a/rules.ae`'s `:why` for "Optional modules" (code)
still names the runtime image.

## 2026-10-15: the build's speed

The user's order, after the docs: a build (about 4.5 minutes) kept few of the 64 cores busy; aim
2 to 3 minutes. Branch `speed`, merged (`9b62cd2`); recorded in `doc/STANDALONE-NOTES.md`, "After
the milestone: the build's speed". Measured on the shared host while the emulated arm64 VM ran
(single timings vary by up to a fifth): each phase's cores sampled each second, a2g's CPU
profiled (`ARBACE_CPUPROFILE`, `go tool pprof` of TamaGo's toolchain).

Found and changed:
- a2g's analysis spent 13% of its CPU (and much of its garbage) in one reflective call: the
  class forms' `parse-method-desc` called `indexOf(String, int)` with a long; the analysis'
  class-info cache (`arbace.classes.env/reflect-cache`) was untyped (reflective) and behind one
  lock taken by every lookup from 16 threads; the back end's `registered?` lost a hint. Fixed
  (40 reflection warnings in the tools' namespaces, the rest cold).
- a2g loads in two rounds (the runtime's classes, then the compiled namespaces'), and with
  `--await` starts while the host compiles the namespaces, analyzing the runtime meanwhile; its
  output does not depend on the wait (checked: byte for byte with `--input`) (SA160).
- a2g read every file of a tree whole, one after another, to find its package files (16 s for
  the compiled namespaces' 28 MB): now their first form.
- report.edn printed directly, not by `arbace.pprint` (18 s at the end) (SA161); g2a's
  amd64 and arm64 builds at once (SA162); `jdk/edge.edn` out of the runtime key, as a2g's data
  (SA163).
Tried and left: `GOGC` higher within the same memory limit (slower), 32 jobs (no gain: bound by
Go's collector).

Results: a2g alone about 100 s (was 160 to 210); the fixpoint's one-run builds 2:57 and 2:44
(were 4:13, 4:19); the essential gate passed on the branch (10:58); the fixpoint G1 = G2 = G3
byte for byte. The seed remade from the fixpoint at `d23bc5e`, `79d4f552...` (was
`c7f5e3d5...`), and verified (stage 0 = G1 = G2 = G3, `b6f2e89e...`). Left: the host's
compiling of the namespaces (about 60 s; its emit sequential by its shared constant pools),
a2g's reachability (19 s) and sequential translation steps (18 s), and `jrt`, one Go package,
the long pole of g2a's builds. SA160-SA163 proposed for the user, recommended (a).

## 2026-10-15: SA160-SA163 decided; c2a's stale `:why` fixed

The user decided the build speed's amendments (`doc/STANDALONE-NOTES.md`, "After the milestone:
the build's speed", Amendments): SA160-SA163 (a) as recommended, all as built: a2g's two rounds
with `--await`, overlapping the host's compiling of the namespaces (SA160); report.edn printed
directly (SA161); g2a's architectures built concurrently (SA162); `jdk/edge.edn` out of the
runtime key (SA163). The alternatives, (b) each, were the previous behaviour.

The leftover of the docs' sweep closed (the user: "fix that one stale string"):
`arbace/c2a/rules.ae`'s `:why` for the divergence "Optional modules" named a runtime image that
main has not had since S4; it now says, as `doc/C2A.md` does, that it was the JVM's runtime image
(on `arbace-for-java-26`) and that main removed the three namespaces at S4. Only
`bin/c2a --rules` prints it (checked). The essential gate passed on it (`9942951`).

## 2026-10-15: a do evaluated again names the new sibling class in reflection

SA90 (b) (S5, `3f31072`, decided as SA104 (a)) was already done; its uncovered case fixed at
the user's request: a top-level do of class forms evaluated again published a member whose type
names a later sibling with the class its previous evaluation defined (`desc->class` found it in
the registry, as `*eager*` is false while publishing), so reflection reported the old class.
`arbace.classes.interp/sig-class` now resolves under `*eager*`: the sibling is pending, the member
published with `Object` and again when the new sibling is defined, as on the first evaluation.
Alternative considered: dropping the old class from the registry at the do's start (it would
change what the do's earlier forms see of the name, which the JVM's lazy resolution keeps).
A case added to `test/oracle/forms/native.ae` (the SA/SB do evaluated a second time; the return
type of `SA.mk` and the type of `SA.b` against the new `SB`), recorded on the frozen jar
(`[11 11 false true true]`); main's build gave `[11 11 false false false]` before the fix. The
essential gate passed on `6e9af3c` (9m16s): the oracle 22,508 of 22,561, the 53 known, 0 new.

## 2026-10-15: the box's oracle and `bin/box-suite` in `bin/gate --full`

At the user's request (the agenda's Later: "the box's oracle and `box-suite` in `bin/gate
--full`, manual today"). `bin/gate --full`'s Go chain runs, after the box's smoke test and only
when it passed, `box-oracle` (`ARBACE_BOX_NET=loopback bin/oracle check
'target/arbace-go/box-amd64/arbace-box -' -j 8 --timeout 900 --exclude forms/files. --exclude
forms/processes. --expected test/oracle/known-go-box-amd64.edn`, as run by hand since S1) and
`box-suite` (`bin/box-suite`, on the renamed suite the essential gate's `suite-go` leaves in
`.tmp/clojure-tests/go`), one after the other and before the fixpoint (the box on /dev/kvm takes
4 vCPUs a run; the fixpoint's builds beside them would contend for memory). Measured by hand at
`1b24757` (the box built afresh, 3m10s): the box's oracle 22,055 of 22,108 cases, the 53 of
`known-go-box-amd64.edn`, 0 new, in 54 s; `bin/box-suite` 67 namespaces, 685 tests, 19,647 of
19,647 assertions, in 2m41s. About 3.5 minutes added to the Go chain. Alternatives: in the
essential gate (it would need the box built on every change, 3 minutes more, and /dev/kvm for
every gate run); concurrently with the fixpoint (memory). Arm64's box oracle and suite stay
out (an emulated VM takes hours).

## 2026-10-15: S9, Go's toolchain as forms

At the user's request, started beside the build's second speed round: the survey's last stage
(doc/STANDALONE-SURVEY.md §7 step b1', §8.1's S9), branch `s9`; recorded in
`doc/STANDALONE-NOTES.md`, "S9: Go's toolchain as forms", and `tamago/cmd/README.md`.

Source: TamaGo's Go distribution, https://github.com/usbarmory/tamago-go, `VERSION` "go1.27.1"
(time 2026-08-28T16:20:06Z), the local tree `/root/tamago-go` (Arbace's toolchain,
`G2A_GOROOT`; it carries no git metadata), the whole tree pinned by `seed/goroot.sha256`
(`5745170f...`) and its `src/cmd/` by `bin/toolchain-vendor` (`c0db2e8f...`, the sorted SHA-256
list of every file). `cmd/vendor/golang.org/x/telemetry` is Go's vendored copy of
https://go.googlesource.com/telemetry at the version `src/cmd/go.mod` names.

What: `go list -deps cmd/compile cmd/link cmd/asm` gives the same 105 packages under `cmd/` for
linux/amd64 (the host's tools) and tamago/amd64, tamago/arm64 (the box's tool guests): 653 Go
files, 766,624 lines (469,834 generated). `bin/toolchain-vendor convert` (new, modelled on
`bin/tamago-vendor`) converts them with `bin/g2a convert` for each configuration and merges the
three into `tamago/cmd/` (758 forms files, 30.3 MB; no file's forms differ by configuration),
with `src/cmd/`'s `go.mod`, `go.sum`, `vendor/modules.txt` and `compile/default.pgo` (cmd/compile's
PGO profile). `check` builds `compile`, `link` and `asm` from the forms printed and from the
originals, the same way (linux/amd64: `go build -trimpath`; tamago: as `bin/box-tools` builds the
tool guests), both with empty build IDs (`-buildid=` and `-X=cmd/internal/objabi.buildID=`, the
build ID the go command stores in a toolchain binary): all nine byte-identical, linux/amd64's
DWARF included. `verify`: a fresh conversion is the tree byte for byte.

Fixed on the way, in g2a's printing (lines layout): a labeled declaration whose label is not on
the line before the declaration (a comment between: `cmd/asm`'s `parse.go:182`; gc records such
a label's line) now carries `:go/label-line` in the forms, printed there (SPEC §7.3; in std only
`regexp/backtrack.ae` changes, as a fresh conversion writes it); a parameter list after type
parameters that span lines is printed from its vector's line, before its `(` (the parameter's
DWARF line in `compile`); `--module std` prints a tree with several main packages.
`bin/tamago-vendor`'s verify and key take its two roots only; `bin/box-rebuild` puts those two of
`tamago/` into the box's store.

Decided by the user (2026-10-15): SA200 (a), the printing fixes as built (alternative: the
label's line always recorded, every labeled declaration's forms changing); SA201 (a),
`tamago/cmd/` as a third root of `tamago/` (alternatives: inside `tamago/goroot/go/cmd/` with std,
which every box build would print, 30 MB more; a top-level tree); SA202 (a), the three
configurations (alternative: the tool guests' two); SA203 (b), the box's tool guests built from
`tamago/cmd/` printed, `BOX_TOOLS_GO=orig` the way back (alternative (a): the forms only proven
equivalent, the tools built from the toolchain's tree); SA204 (a), `bin/toolchain-vendor verify`
and `check` in `bin/gate --full`, a chain of their own beside the JVM's, the Go build's and g2a's
(alternative: by hand at milestones); `bin/g2a convert` takes its dumps' directory from
`G2A_DUMPS` so that the two vendoring scripts convert concurrently.

Checks: `bin/toolchain-vendor check` and `verify`; `bin/tamago-vendor check amd64` (std's round
trip without regressions, the goos module 69 of 69, the box's guest from the printed forms
identical); `bin/g2a test` (150 assertions); the tool guests built from the forms by
`bin/box-tools` (amd64) and the box's smoke test with them (compile spawned with trees in and
out, a failing compile, an image booted from the store); the essential gate on `0db720f`
(passed, 12m23s) and on the branch's last commit (its result in STANDALONE-NOTES.md, S9).

## 2026-10-15: the build's speed, second round; the seed remade (9b2a4687...)

The user's order ("do a second build-speed round"), after the first round's SA160-SA163. Four
branches, one per part, each measured on the shared host (the emulated arm64 VM beside),
output-neutral unless stated; recorded in `doc/STANDALONE-NOTES.md`, "the build's speed, second
round", parts A-D.
- A, the host's compiling of the namespaces (`speed2-host`): about four fifths of its CPU was
  the evaluator, since the host loads the namespaces from source to record their units and so
  ran the back end (`arbace.aot`, core, what they call) evaluated. `arbace.aot.main/run` now runs
  the host's compiled loaders of the back end's closure (23 sources) first, when each is byte for
  byte the host's embedded copy (SA164): the back end 22 to 29 s, was 55 to 58; the same tree
  (G1 = G2). Alternatives: only `arbace.aot` compiled (a third of the gain), `GOGC` (no gain).
- B, a2g after the compiled namespaces arrive (`speed2-a2g`): the writer (no dynamic binding per
  tag, newlines by `indexOf`), `simple?`/`effects?` cached per node, pure name functions cached,
  dyn's default methods by an index, registrations and collisions in parallel, reachability in
  batches flattened in parallel and scanned in order: 66 to 75 s, was 123; a2g's output for the
  same input byte for byte the same. Tried and dropped: the core loader's analysis first.
- C, `jrt` split (`speed2-jrt`): measured, not built (SA180 (a)): Go's cache holds `jrt` unless
  the runtime changes, so a one-run build compiles none of it; a split would gain about 45 s at
  best once per runtime change for S5's 5 to 10 days. SA181 (a): the method's scripts stay in
  `.tmp/speed2/`.
- D, g2a's own work (`speed2-g2a`): it read all 77 MB of forms every build (62% of its CPU) even
  with every package cached; the print cache's collect index (SA190) skips the packages whose
  files are unchanged (by SHA-256), cached Go files copied as is: its step about 8 s, was 18 to
  21; the printed module the same. SA191 (a): `aot_arbace_core_protocols` left its own package
  (about 2 s, only when the runtime changes).
The user decided SA164, SA180, SA181, SA190, SA191 (a). Merged (`d45e718`): the essential gate
passed, the fixpoint G1 = G2 = G3. S9 merged after (`57a4428`, its journal entry above), then
the seed remade once for both from the fixpoint at `57a4428`: `9b2a4687...` (was `79d4f552...`),
verified by `bin/seed --verify` (the same tree and archive; stage 0 = G1 = G2 = G3 byte for byte,
`2ee89825...`); the essential gate passed on it (`dd61501`, 8:07, its two builds 5:06). Left:
the analysis of `arbace.core`'s loader on one thread (9 to 14 s), a2g's sequential load after
the tree arrives (about 10 s), reachability's scan (12 to 16 s).

## 2026-10-16: a Makefile for newcomers

At the user's request ("its purpose only is to provide a familiar interface for newcomers, but it
should be maintained in sync with bin/ scripts"), after the targets were listed and accepted: a
top-level `Makefile` whose every target runs a script of `bin/` (the logic stays in the scripts,
which cache by content; all targets phony, no dependency tracking; the environment passes
through). Groups: the bootstrap (`seed-check`, `stage0`, `seed-verify`, `reseed`), the Go build
(`build`, `build-tests`, `smoke`, `repl`, `fixpoint`), the box (`box`, `box-smoke`, `box-oracle`,
`box-suite`, `box-tools`, `box-rebuild`, `mac-bundle`, `arm64-vm-check`), the checks (`gate`,
`gate-full`, `suite`, `oracle`), the vendored trees' and recorded data's verifications (`verify`
and one target each), `alpine-check`, `clean` (`target/` only). `make help` (the default) lists
them from their `##` comments. Kept in step: `make check-makefile` checks that every `bin/`
script a recipe runs exists and is executable (tested failing on a misnamed script), run as the
essential gate's first step (`makefile`); CLAUDE.md states the rule. The heavy lock of this host
(`.tmp/heavy`) is not in it (`.tmp/heavy make gate`). Alternatives: a `justfile` (a tool apk
would add), no check (the Makefile would drift), the Makefile carrying logic (two places to
maintain). The README's build section names the main targets.

## 2026-10-16: the whole chain in the emulated arm64 VM (BX117 (a))

`bin/box-rebuild --arch arm64` at `434021a`, the whole chain in the box inside the emulated
aarch64 VM with KVM/arm64 (`bin/box-arm64-vm`, 16 vCPUs, 28 GiB), nothing given, kept running by
the user's choice: started 2026-10-14 22:49, ended 2026-10-16 15:24. The image built in the box
equals the arm64 host step's byte for byte: `5e4506dee1c54183640df087a18e2508490741ac1da6a424618ce69237708000`
(98,464,042 bytes), booted by its ref ("arbace-seed-is-go-v1-154-g434021a aarch64"), in 145,762 s
against the host's 591 s (about 247 times). Every step's tree equals the host's but a2g's whole
output, whose `report.edn` holds its timings (its program equal). a2g took 103,558 s of it,
`arbace/jrt`'s compile 14,851 s. `compare` exited 1 only on its last check (that
`target/arbace-go` is built from these sources: main had moved on since); the steps were compared
by hand from `box-steps.edn` and `host-steps.edn`. The chain thus holds on arm64 from nothing in
the VM, as with the amd64 box's results given (BX117 (b)) and on the Mac. Details in
doc/go/BOX-NOTES.md, BX117. Open: the waiting guest's 3.5 vCPUs under emulation.

## 2026-10-16: the scratch cleared, the merged branches deleted

At the user's request: `.tmp/box-rebuild/`'s stores removed (each run's `*-steps.edn`, logs and
`tools.txt` kept; the results are in doc/go/BOX-NOTES.md), the merged worktrees and their
branches (`s9`, `speed2-a2g`, `speed2-g2a`, `speed2-host`, `speed2-jrt`) and the speed rounds'
scratch removed; about 36 GB freed. `s6-t3`, S6's T3 measurement build (three commits never
merged: the benchmarks as a namespace, 9,000 deep, tail calls trampolined, ending at `694660d`),
deleted without a tag, by the user's decision: its numbers and design are in
doc/STANDALONE-NOTES.md ("Benchmarks and T3 (SA139)"), whose two references to the branch now say
the build was not kept.

## 2026-10-16: Arbace to depend on neither go-whim nor cgo (the user)

The user: "you should not depend on go-whim, that is a vim derivative, so it's at application
level, not system level, the virtualization there was a pilot project, will be pruned, so vendor
and adapt everything needed from go-whim, what's more: consider dropping cgo dependency
altogether". What still needed go-whim (https://github.com/arbace/go-whim at `80c49bf`, in
`/root/go-whim`): `box/`'s code was already vendored as forms (S0, S1); its checks were not:
`bin/box-vendor kvm-tests` (in `--full`) and the arm64 VM's `hv` ran go-whim's own tests from a
clone, `convert` and `check` replayed S0's derivation, and go-whim's module requirements sat in
Go's module cache for that clone. cgo: no Arbace package imports "C" (the Mac's launcher binds
its frameworks through purego); `CGO_ENABLED=1` only for the race detector (`bin/jrt test
--race`, `bin/a2g-race`: hand-run, in no gate; Go's `-race` requires cgo), unconditionally in
`bin/jrt test`, and the go-whim guest tests' C guests needed gcc and ld scripts. Decided, as
recommended: GW2 (a) the C guests rewritten in Go's assembly, built by TamaGo's own asm and link
(alternatives: drop them, or keep C with gcc); GW3 (a) the replay from go-whim retired, `box/`
Arbace's own source, its origin recorded (alternative: keep it optional); CG1 (a) cgo dropped
entirely with the race detector (alternative: keep it optional for `-race`), a2g's parallelism
still guarded by its byte-for-byte checks and the fixpoint. Also decided for the speed's third
round: SA210 (a), SA211 (a) (the loader in member parts of 100 units; their frames named by the
part), SA220 (b) (the compiled tree in several files), SA230 (a) (an id slot only for sources
loaded by a resource path, so the compiled program does not depend on the checkout's path:
found when a worktree's path made G2 differ from G3).

## 2026-10-16: the build's speed, third round; the seed remade

At the user's request ("do what's left of the speed work"), four agents in worktrees, merged on
an integration branch `speed3`, then into main (`2831811`); details in doc/STANDALONE-NOTES.md,
"The build's speed, third round", parts A, B, C, SA220 (b), SA230 (a):
- A (`speed3-loader`, `arbace/aot/emit.ae`): a loader of more than 100 units in member classes
  `SCRIPTBASE__init$partP` of 100 units, each with its own constants (copied from an earlier
  part's field when shared), all initialized before the first unit runs; a2g analyzes them in
  parallel. The analysis after the tree arrives 25 → 15 s; the executable +168 KB. SA210 (a),
  SA211 (a) (a loading error's frame names the part).
- B (`speed3-load`, `arbace/a2g/world.ae`, `arbace/classes/analyze.ae`): the headers, members
  and bridges in parallel one level of the hierarchy at a time, the class forms parsed in
  parallel, declared in order. The sequential load 10.7 → about 5 s, a2g's output the same.
- C (`speed3-reach`, `arbace/a2g/reach.ae`): each batch's code planned in parallel, the scan
  sequential in the queue's order (`report.edn`'s texts depend on it), its state explicit, its
  lookups cached per run. Reachability 19-21 → about 6 s, a2g's output the same (a trace of its
  109,323 state changes the same line for line).
- SA220 (b), SA240 (a) (`arbace/aot/main.ae`, `world.ae`): a class form past 256 KB written in
  parts, `(aot/continue NAME k ...)` in `aot_NAME.k.ae`, joined by a2g after its parallel read.
  The read about 2 → 0.8 s.
- SA230 (a) (`arbace/lang/Compiler.ae`): an id slot only for sources loaded by a resource path:
  the host's compile script had one from its absolute path, so the compiled program depended on
  the checkout's location (part C's fixpoint broke in a worktree whose path collided with
  `arbace/box/print.ae`'s slot; main's path collided with `arbace/spec/gen/alpha.ae`'s). Proven by
  a colliding worktree building the same executable.
The essential gate passed on `speed3` (8:53; the suite 20,506 of 20,520, the oracle's 53 known,
0 new); the fixpoint G1 = G2 = G3 there (`848c3b02...`) and at the merge (`21519fbd...`,
102,391,968 bytes). The seed remade from the merge's G3: `5858e83c...` (was `9b2a4687...`,
`30f1656`), verified by `bin/seed --verify` (the same tree and archive printed again at
`2831811`; stage 0 = G1 = G2 = G3 byte for byte, `21519fbd...`, 354 s). Left: the scan's 6 to
8 s (mutable sets), the analysis' member-class group (about 15 s on 16 jobs).

## 2026-10-16: Arbace independent of go-whim and of cgo

Following the user's direction (entry above), on the branch `no-gowhim`, merged into main
(`ff90747`); details in doc/go/BOX-NOTES.md, "go-whim vendored whole: Arbace independent of it;
cgo dropped". From go-whim (https://github.com/arbace/go-whim at `80c49bf`) into
`box/go/arbace/box/guest`: the guest tests (`guest/guest_test.go`, `smp_test.go`,
`readahead_test.go`), the guest build (`guest/gotamago.go`, as `guest.Build`), `faulty`, and a
demo guest adapted from `guest/tamago/main.go` in place of the editor; the C guests
(`guest/rt`) rewritten in Go's assembly (`guest/cmd/bare`, amd64 and arm64, linked by TamaGo's
own linker, GW2 (a), GW6 (a)). `bin/box-vendor`: `convert`, `check`, the go-whim clone retired
(GW3 (a), GW7 (a): S0's replay stays runnable at the tag `arbace-inside-itself-v1`);
`kvm-tests` and the arm64 VM's `hv` run box/'s own tests (GW8 (a)); `roundtrip` covers the
guest packages (39 of 39). g2a's build takes several main packages in one printing, `--main`
picking one. cgo: `bin/jrt test --race`, `bin/a2g-race` and `test/a2g/race/` removed,
`CGO_ENABLED=0` in every script; `bin/g2a-build-tests` checks static linking with
`tools/elfstatic` (Go's debug/elf) instead of `file` and `readelf`. LICENSE.md: the new parts
under go-whim's EPL 1.0 entry (GW5 (a)). doc/ALPINE.md: cgo's row and go-whim gone; `alpine-sdk`
stays for jdk26u (GW9 (a)); Go's module cache needed only by the vendoring checks (stage 0
built with an empty cache). CLAUDE.md updated (GW4 (a)). Checked with /root/go-whim, go-whim's
module-cache entries and every C tool masked (bwrap): the KVM tests (hv 4, vmm 15, tree 3,
guest 8), `roundtrip`, `purego-check`, the box's build and smoke test; in the arm64 VM hv 4,
vmm 15, guest 8 (the bare guest's faults `0x96000047`). The essential gate passed on the
branch at its second run: the first had one oracle mismatch, `test/oracle/forms/concurrency.ae:201`,
whose result depends on which thread reaches a `CyclicBarrier` last (to make deterministic).

## 2026-10-16: the oracle made independent of thread interleavings

At the user's request ("make the oracle deterministic"), after the essential gate on the branch
`no-gowhim` failed once on `test/oracle/forms/concurrency.ae:201` (`[1 (0 2)]` for `[2 (0 1)]`):
that case returned the main thread's `CyclicBarrier` arrival index, which depends on which
thread arrives first; it now compares the three indices as a set (`(0 1 2)`). A review of every
case that starts threads (`concurrency.ae`, `native.ae`, `defclass.ae`) found one more:
`ThreadPoolExecutor.shutdownNow` after a 20 ms sleep, assuming the pool's thread had taken the
first task by then (on a loaded host `[3 ... 0]` instead of `[2 ... 1]`); the task now counts
down a latch the main thread awaits. Both recorded again on the frozen JVM (the same values;
`bin/oracle record forms/concurrency`), all 247 cases matching on Go; doc/go/ORACLE.md states
the rule (no case depends on which thread arrives first, nor on a sleep for ordering). Checked:
the concurrency file 40 times on a loaded host and 10 times each at `GOMAXPROCS` 1, 2 and 4, and
the whole oracle five times (`GOMAXPROCS` 1, 2, 64, 1, 2): every run the 53 known mismatches,
none new. Alternatives: rerunning a failed case (hides real races), or recording such cases as
noisy (they test real semantics that hold whatever the order).

## 2026-10-16: a note on the translators' names (the user)

The user: "take note: \"g2a\" name is unfortunate and misleading, maybe \"g2g\" would be better,
or how to name these converters/compilers?". Noted in the agenda (Later) as a survey: g2a is a
two-way syntax bridge between Go and Go forms, not a translation into Arbace as j2a and c2a are;
a2g compiles class forms into Go forms. No renaming yet; the 2026-10-11 renaming (j2c, g2c, c2g
to j2a, g2a, a2g) is the precedent for how a rename is carried out.

## 2026-10-16: a tools-only seed measured, and not pursued (the user)

The user asked whether the seed is minimal (it holds JDK data such as `tzdb.dat`), then for a
measurement. The seed is the whole program by design (stage 0 = G1). Measured on a branch
`tools-only` from `ad5e2a5a` (hacks behind environment variables: `A2G_EMBED_ONLY`,
`A2G_JDK_REACH`), packed as `bin/seed --make` packs (today's seed reproduced so, byte for byte):
the whole program 5,451,704 bytes of xz (85 namespaces, executable 102.4 MB); the bootstrap's
tools (46 namespaces: a2g, the back end, the class forms' front end, g2a's build and print)
4,692,972 (-13.9%, executable 90.7 MB); all the host's tools (63, adding c2a, the suite's
runner, g2a's convert and read, `arbace.box`) 4,965,496 (-8.9%); the bootstrap's tools with the
time, text, locale, net, security, sql and stream packages of the JDK reached rather than rooted
4,535,488 (-16.8%); with no JDK roots 4,249,356 (-22.1%), not runnable (`RT.doInit` reaches
`Properties.setProperty` by reflection); evaluated (`ARBACE_AOT=0`) 3,160,420 (-42%). `jrt`
(20 MB) does not follow the namespaces: `--program` roots every public member of the JDK classes
the REPL sees. The JDK's data (about 300 KB, 5% of the seed) stays in every variant that runs:
`arbace.core` loads `arbace.instant`, which reaches `TimeZone` and the tzdb provider. The
bootstrap-tools seed's stage 0 built the same G1 as today's stage 0, byte for byte, once the
classes the host's compile cuts were taken from all of `arbace/` (else `arbace.repl`, the only
user of `sun.misc.Signal`, compiled differently on a host that does not embed it). A minimal seed
would need a named tools-only build and a longer check (G3 printing the seed again, its
executable = stage 0). The user: "it isn't worth it for a 14-17% smaller seed"; the branch was
dropped, its numbers kept here.

## 2026-10-16: the build's speed, fourth round; the seed remade

At the user's request ("do the rest of the build's speed"), two agents in worktrees, merged on
`speed4`, then into main (`51300c0`); details in doc/STANDALONE-NOTES.md, "The build's speed,
fourth round", parts A and B.
- A (`arbace/a2g/reach.ae`): the run's state in Java's mutable collections (`LinkedHashSet`s,
  an `ArrayList` queue), rebuilt as persistent values in insertion order at the end; no dynamic
  vars on the hot path; the plans of nodes that cannot record anything shared by key and skipped
  once a run of them had no effect (the state only grows: 43,357 distinct plans for 632,000
  nodes); `java-impl-of`'s superclass and implementations cached, the lambdas' `subclass?` tests
  lookups in each interface's supertypes. `:reach` 7-10 → 3-4.5 s; a2g's output the same byte
  for byte, and a trace of its 110,056 state changes the same at each of five steps. SA250 (a):
  the second left outside the scan stays (alternative: a fifth round on `main.ae`'s stand-in
  rounds and the parallel walk).
- B (`arbace/classes/analyze.ae`, `env.ae`, `types.ae`, `arbace/a2g/world.ae`): the time was the
  analysis' own work (16 threads busy to the end, no lock waits, the collector a third), mostly
  `find-own-field`'s linear scans through the holders' constant fields and the supertypes'
  methods walked on every call. An index of each class's fields, `all-supertypes` and
  `member-methods` memoized (each result reused only while the infos it came from are the same
  objects), `parse-method-desc` memoized, the top-level classes without members analyzed in the
  first group. The analysis of the compiled classes 16 → 5 s; a2g's output the same.
  `A2G_ANALYSIS_TIMES=1` prints each group's longest tasks. SA260 (a): the memos live for the
  process, as `env/reflect-cache` (alternatives: per a2g run; weak or bounded maps).
The essential gate passed on `speed4` (7:15; the suite's 20,506 of 20,520, the oracle's 53
known, 0 new) and the fixpoint held there (`9c22a730...`) and at the merge. The seed remade from
the merge's G3: `b566d53a...` (was `5858e83c...`), 5,477,008 bytes, verified by `bin/seed
--verify` (the same tree and archive printed again at `51300c0`; stage 0 = G1 = G2 = G3 byte for
byte, `7b394f03...`, 336 s).

## 2026-10-16: the naming note refined (the user)

The user, on the translators' names: "translation is always between forms, so j2a or j2c might
be better jaf read / jaf print instead. the earlier renaming stemmed from the confusion around
the letter 'c' that starts \"clojure\" and also \"classes\"". Added to the agenda's survey: one
bridge per language between its source and its forms (two verbs, read and print), translators
between forms dialects only, no bare `c`.

## 2026-10-17: g2a's printer mode `:full`; the naming survey

At the user's request ("build the printer's :full mode"), on the branch `g2a-full`, merged into
main (`6b6c0de`); details in doc/go/PRINTER-NOTES.md, "Mode :full as built", and doc/go/SPEC.md
§10.2. A package converted with `--full` now prints back to its original Go files byte for byte:
a layout `full` places each token at its recorded line and column; the converter records, in
that mode only, every comment as spelled, the source's semicolons, trailing commas, white space
other than gofmt's and the file's tail, literal spellings that differ from the printer's,
parentheses, and the positions the forms had dropped (implicit returns, braces, labels...),
reading the original files itself (G2F3). The text level (`bin/g2a roundtrip --text`): std
1,742 of 1,742 files (tamago/amd64), 1,738 of 1,738 (arm64), 1,834 of 1,834 (linux/amd64),
std with its tests 2,935 of 2,935; `$GOROOT/test`'s programs 1,705 of 1,708, the three known
(an empty declaration group, `import ( )` and a local `var ()`, has nothing in the forms: G2F6).
`:lines` unchanged: the round trips against their references, `tamago/`, `tamago/cmd/`, `box/`
and a2g's program printed the same (`diff -r`). `:full` forms are about 3.4 times the size of
`:lines`. Decided by the user: G2F1-G2F6, G2F8 as built; G2F7 the text round trip in
`bin/gate --full`. The essential gate passed on main after the merge (7:27), and the text round
trip there (1,705 of 1,708, the 3 known).

The naming survey (doc/NAMING-SURVEY.md, the user's "survey first, decide later"), merged
(`d2e220e`): the tools as they are (c2a right but for its bare `c`; j2a read only; g2a a two-way
bridge; a2g between two Arbace dialects), schemes A (the user's bridges `jaf`/`gaf` with read and
print, translators `cf2gf`, `clj2ae`), A', B (long names), C (x2y), the rename's impact (6,959
occurrences in 1,299 files, 225 paths; a re-seed), the 2026-10-11 precedent; proposals N1-N13,
open.

## 2026-10-17: the survey of Futamura's projections

The user's choice (2026-10-16): doc/FUTAMURA-SURVEY.md, written on the branch `futamura-survey`
(`5f4c3a6`); the document taken into main, its experiment (`test/futamura/`: it patches the
seed's printed program) kept on that branch. Findings: Arbace has the projections' pairs made by
hand: the evaluator's closure compiler is the first projection done by hand at run time,
`arbace.aot.emit` the same ahead of time, a2g a compiler beside the class forms' interpreter;
each pair shares its analysis and writes its back half twice (about 3,200 lines against 1,470;
3,935 against 2,227), and no check compares the two halves on the same form. The experiment
rebuilt the back end's Go for `fib` and `loop-long` in the seed's program (6.40 and 11.87 ms, as
SA139's 6.36 and 12.21): on `fib` the call path is 13% and `Numbers`' boxed generic operations
78%; a `Long` fast path 6.40 → 1.44 ms, with var speculation 0.78, with an unboxed worker behind
type guards 0.168 (the JVM 0.16); on `loop-long` the class-initialization guard pushes even
`x < y` past Go's inlining budget (81 against 80): inlined with the guards 2.81 ms, without them
1.09 (the JVM 0.41); fns as Go closures (SA156 (c)) change no speed (6.46). The prepare run is a
snapshot of the loaded program (3,161 vars, 2,601 holding fns, 108 dynamic), as Wizer and weval
use one; Clojure's direct linking is the precedent for static var roots. The second projection
by self-application is judged unrealistic for Java's semantics; one definition of the semantics
is reachable by staging (LMS, "Collapsing towers of interpreters", Truffle's DSL). Proposals
F1-F11, open: staging as the approach (F1), per-form differential checks (F2), arithmetic fast
paths (F3), no initialization guards for classes initialized at start (F4), guarded direct
linking (F5), primitive fns then unboxed workers (F6), a `defnode` prototype (F7), the class
forms' pair kept with F2 (F8), closures later (F9), inline caches in the REPL (F10), the records
(F11).

## 2026-10-17: the bridges' names settled: repr/java, repr/go (the user)

The user: "i prefer something, like \"repr/java\" and \"repr/go\", where \"repr\" stands fo
read/print and prefixes \"representation\"". Decided with it (doc/NAMING-SURVEY.md, "Decided"):
namespaces `arbace.repr.java` (j2a) and `arbace.repr.go` (g2a), commands `bin/repr-java` and
`bin/repr-go` with `read` and `print` (alternative: one command `bin/repr java|go`); a2g and c2a
keep their names (alternatives: a parallel family `arbace.trans.*`, or the survey's `cf2gf`,
`clj2ae`); g2a's build as `arbace.build.go`; `G2A_GOROOT` → `ARBACE_GOROOT`, the bridges'
variables `REPR_GO_*`, `REPR_JAVA_*`; no shims, a guard for the old `G2A_GOROOT`; all at once,
then one re-seed, then `repr/java print` (the user, earlier: "Create jaf print after the naming
debate settled").

## 2026-10-17: a2g to j2g, "class forms" to "Java forms" (the user)

The user: "the term \"class forms\" might be better as \"java forms\", i propose renaming a2g to
j2g for now, then we may revisit both j2g and c2a later, much later". Decided, as recommended:
into the running rename (one re-seed; alternative: a second rename after); the printed
program's names follow (`a2g_*.go`, `A2g_*`, the `a2g/*` directives → `j2g`; alternative: kept);
the term changed in docs and prose, `arbace.classes.*` kept (alternative: the namespaces too);
`A2G_*` → `J2G_*` with a guard (alternative: kept).

## 2026-10-17: Futamura's proposals decided as suggested (the user)

The user: "start the Futamura proposals F1–F11 when you see fit as your suggestion". Decided
(doc/FUTAMURA-SURVEY.md, "Decided"): F1 (a), F8 (b), F9 (a), F10 (a), F11 (a); the order F2,
then F3 with F4 (one re-seed), then F5 on the numbers, F6 and F7 after; started after the
running rename, which changes the same files (j2g, the back end's callers).

## 2026-10-17: the rename carried out; the seed remade

As decided (entries above; doc/NAMING-SURVEY.md, "The rename as built"), on the branch
`repr-rename`, merged into main (`8c3a289`), done as the 2026-10-11 rename: `git mv` commits,
then the callers, then the regenerated records. j2a → `repr/java` (`arbace.repr.java`,
`bin/repr-java read|read-jdk|coverage|check`), g2a → `repr/go` (`arbace.repr.go`, `bin/repr-go
read|print|dump|corpus|roundtrip|check|test`), g2a's build → `arbace.build.go` (`bin/build-go`),
a2g → j2g (`arbace.j2g.*`, `bin/j2g`, `j2g-check`, `j2g-edge`, `j2g-evalproof`, J2G-SPEC.md,
J2G-NOTES.md; the printed program's `j2g_*.go`, `J2g_*`, `j2g_header`, `j2g/*` directives);
`G2A_GOROOT` → `ARBACE_GOROOT`, `G2A_*`, `J2A_*`, `A2G_*` → `REPR_GO_*`, `REPR_JAVA_*`, `J2G_*`,
an old one set stopping a script with the new name (`bin/lib/renamed-env.bash`, R2); `RT.toolSource`
lists `arbace/repr/`, `arbace/build/`, `arbace/j2g/`; "class form(s)" → "Java form(s)" in prose.
Regenerated: jdk/'s headers and provenance (`bin/jrt-convert update`), the fixtures' forms,
`bin/repr-java check --update` (headers only), `jdk/edge.edn` (`bin/j2g-edge update`; it had been
stale on main since `2352fb2` of 2026-10-16: 7 `java.util` classes the new reachability code
names, which only `--full`'s edge verify would have caught). Checks on the branch: the
essential gate (10:18), the fixpoint G1 = G2 = G3, repr/go's round trips (code; text 1,705 of
1,708, 1,742 of 1,742), its tests, repr/java's checks, the vendoring verifies. Found on main
too: `bin/tamago-vendor verify` fails since the `:full` mode (`53c65f6`): a fresh conversion
adds `:go/label-line` to four labelled declarations (R4: reconverted separately). Decided by the
user: R1 (repr/java's `.ae` writer renamed `layout` with `repr/java print`), R2, R3 (`:a2g` kept
in java-surface.edn, a record of removed stages), R4, R5 (the checks as verbs of one command).
The seed remade from the merge's G3: `56fabe20...` (was `b566d53a...`), 5,530,756 bytes,
verified (the same archive printed again at `8c3a289`; stage 0 = G1 = G2 = G3, `79eb9ef9...`).

## 2026-10-17: the survey of repr/haskell

At the user's request ("spawn a survey about repr/haskell, you may find resources in
../ghc-lisp/"): doc/REPR-HASKELL-SURVEY.md (from the branch `repr-haskell-survey`, `fa3d318`).
Prior art: ghc-lisp (/root/ghc-lisp at `db7adf93`, read only), GHC with a second front end for
Haskell as s-expressions (`.hsl`), its round trip exact over 4,863 of GHC's files and 99.6% of
its testsuite. Measured: Arbace's reader reads 1 of ghc-lisp's 11 `.hsl` files (the others stop
at their first signature, `Invalid token: ::`), and misreads some silently (`'a'`, `"\1234"`,
`x@y`), so its forms need respelling; an 86-line generic helper on the host's GHC 9.14.1 dumped
659 of `base`'s 799 files (the rest mostly CPP) in 4.9 s, 20 MB of forms Arbace reads in 12 s;
ghc-lisp's printer does not port to 9.14.1 (four constructors missing). Recommended: a bridge
only, ghc-lisp's grammar respelled by repr/go's rule, a generic helper with the converter and
printer in Arbace, the tree, interface and text levels first, after `repr/java print` and
F2-F7; about 6 to 9 days to the tree and text levels over `base`. Proposals H1-H14, open.

## 2026-10-17: c2a to become repr/clojure (the user)

The user, asked whether c2a should be `repr/clojure`: "yes, rename c2a at the next re-seed,
print later". It fits the bridges' principle once it has a `print` verb (Arbace → Clojure, which
would let Arbace's libraries be used from Clojure, with a text round trip since c2a keeps layout
and comments); without one it would stay a translator named c2a. To do: the rename (about 307
occurrences in 44 files) at the next re-seed, F3 and F4's; `repr/clojure print` later.

## 2026-10-17: repr/haskell decided; its spec started

The survey's proposals (doc/REPR-HASKELL-SURVEY.md, "Decided"): H1-H12 and H14 as recommended
(a bridge only, ghc-lisp's grammar respelled, a generic helper on the host's GHC with the
converter and printer in Arbace, `hs.` namespaces and `hs/` heads, layout with `:lines` and
`:full`, the tree, interface and text levels over `base` first, one configuration per
conversion with CPP by the helper); H13 against the recommendation (after `repr/java print` and
F2-F7): the spec now, beside F2, the build after it. Also: R4 done (`tamago/` converted again,
the guests identical on amd64 and arm64, `819dccc`).

## 2026-10-17: repr/go's round trip over `$GOROOT/test`'s directory tests

At the user's request ("do the directory tests beside F2"), on the branch `repr-go-dirtests`
(from `d652647`). Source studied: Go's test driver, `/root/tamago-go/src/cmd/internal/testdir/testdir_test.go`
(TamaGo's go1.27.1): `goDirPackages` (a `.dir`'s files grouped by package clause in file order,
`-s` one per file), `compileInDir` (`go tool compile -e -D test -importcfg CFG -p test/STEM`, or
`-p main`), the importcfg, `linkFile`, and the cases `builddir`/`buildrundir` (one package main
with the `.s` files: `asm -gensymabis`, `-symabis`/`-asmhdr`, `pack`, `link`) and `runindir`
(a copy of the `.dir` in a module named by its base name, `go 1.14` or `-gomodversion`, `go run .`).

Built:
- `godump -dirs FILE.go...` (tools/godump/dirs.go): reads the action line as the driver does,
  compiles the packages other than main in the driver's order into a temporary directory, and
  type-checks each package with an importer that resolves its local paths (`./a` → `test/a`)
  to the earlier packages' archives; runindir tests are listed by `go list -export -deps ./...`
  in such a module copy and dumped as packages. Dumps of a directory test's package add
  `:dir-test` and `:local-imports`; other dumps are unchanged.
- `gocmp tests -dirs` (the corpus: compiledir, rundir, builddir, buildrundir, runindir; no
  other GOEXPERIMENT; go/build's match) and `gocmp -dirtest F.go B` (tools/gocmp/dirtest.go):
  both sides compiled per package as the driver compiles them (flattened, `-trimpath` to the
  original's directory, each side against its own earlier packages), export and code compared
  per package; runindir through module copies and `go list -export -trimpath`; a fourth level
  `run` (both sides linked from their own positions, the output compared with `F.out`; for the
  configurations this machine runs, linux/arm64 through qemu-aarch64, `GOCMP_EXEC`).
- `bin/repr-go corpus [linux-]dirtest[-arm64]`, `bin/repr-go check --dirtests`
  (`--list-dirtests`), `bin/repr-go roundtrip --dirs [--text] [[GOOS/]ARCH]`, references
  `test/repr/go/roundtrip-dirtest-{amd64,arm64,linux-amd64,linux-arm64}.edn`, known differences
  `test/repr/go/roundtrip-dirtest-known.edn` (none).
- The converter: a dump with `:dir-test` gets its namespace from its dump's path
  (`go.test.fixedbugs.issue10066_dir.a`), the test's other packages mapped to theirs; local import
  paths resolved through `:local-imports` (written into `go/package`); in mode `:full`, the
  position of `chan` in `<- chan T` (go/ast keeps the arrow's only), which the printer uses.
  Proposed as amendments D1 and D2 (SPEC §4.1-§4.3, §10.2, §15). Alternatives considered for D1:
  the paths as written (`./a`) in `:path`, which names no package gc compiles; resolving the
  import specs to `test/a` in the forms, which would print another source. For D2: a known
  text difference (2 files of `import2.dir`); a new helper field (the converter already reads
  the source in mode `:full`).
- D3 (proposed, built): `bin/gate --full`'s repr/go step and the Makefile's round-trip targets
  also run `roundtrip --dirs amd64`, `--dirs linux/amd64`, `--text --dirs amd64` (about 20 s).

Results (every level, no known difference): tamago/amd64 253 tests (561 packages, 586 files),
tamago/arm64 and linux/arm64 251, linux/amd64 254: tree, export and code pass for all; run
127 pass on linux/amd64, 124 on linux/arm64, the rest skip (tamago, and the tests that only
compile); text 586, 584, 587, 584 files byte-identical. Left out: errorcheckdir (26) and
errorcheckandrundir (5), the two fieldtrack runindir tests, and the tests the configuration's
build constraints exclude. Checked unchanged: the round trips `amd64`, `linux/amd64` (no
regressions) and `--text amd64`; repr/go's tests (`test read`, `test print`); the Go printed
from `tamago/`, `tamago/cmd/` (linux-amd64) and `box/`'s forms, byte for byte with the base's
printer and with this one.

## 2026-10-17: the directory tests' decisions (the user)

D1 (a directory test's packages: gc's `:path`, the namespace `go.test.<dir>_dir.<key>`, local
imports verbatim with `go/package`'s `:local-imports`), D2 (the `:chan` position of `<- chan T`
in mode `:full`), D3 (the directory tests' round trips in `bin/gate --full`'s repr/go step and
the Makefile's targets): accepted as built. Merged into main.

## 2026-10-17: repr/haskell's spec

As decided (the survey's H13: the spec beside F2), doc/haskell/SPEC.md, merged from the branch
`repr-haskell-spec` (`522d4a6`), with test/haskell/ (328 reader samples and their output, the
name rule and a forms reader as scripts, a worked example `Shape.hs`/`Shape.ae`). The forms are
GHC's parsed tree only (no fixity, scopes or types). The name rule: a name is bare only if
Arbace's reader reads its text back as the same one symbol (or a keyword, for constructor
operators), else `(:name "t")`, a respelled operator's chain `(:op "t" a b ...)`; over GHC
9.14.1's `base` and `ghc-internal` (445 of 535 modules dumped) it keeps 6,900 of 6,995 names
bare, 28 respelled (0.35% of occurrences). Respellings: `::` as `:sig` (`hs/sig` at top level),
`\` as `:lambda`, `:` as `:cons`, `~` as `:lazy`, `#foo` as `(:label "foo")`; characters `\c`;
strings with their escapes resolved; primitive literals `(:int# n)`...; fractional literals
exact (a double only when its shortest text gives GHC the same significand and exponent, else
a BigDecimal). Found in GHC: re-laying out a module changes only its interface's `src hash` and
`interface hash`, while moving an inlined equation by two columns changes its unfolding and the
ABI hash, so the interface level normalizes span columns in `:lines`. The corpus: GHC 9.14.1's
source tarball, pinned by SHA-256. Decisions S1-S36; proposals HS1-HS12, open; the build after
them (milestones R1-R6).

## 2026-10-17: repr/haskell's HS1-HS12 decided (the user)

All as recommended (doc/haskell/SPEC.md, "Decided"). The build (milestones R1-R6) follows F2,
as the user's H13 has it ("its spec in parallel with F2, the build after").

## 2026-10-17: arbace.aot to become c2j (the user)

The user: "confirm that arbace.aot is effectively a clojure forms to java forms compiler, while
the term aot ought to mean the whole pipeline through j2g -> repr/go print -> gc compile, then
the \"back end\" derived from the clojure compiler might be named \"c2j\"", then "yes, rename aot
to c2j with repr/clojure". Confirmed: `arbace.aot` builds on Clojure's `Compiler`'s `Expr` trees
and writes Java forms where Clojure's back end writes bytecode; `ARBACE_AOT=1` already denotes
the whole compiled pipeline. To do with c2a → `repr/clojure`, after F2 and before F3 and F4, one
re-seed; `ARBACE_AOT` and `aot_arbace_*` kept (alternative: renamed too, churn in the seed only).

## 2026-10-17: Go forms to be first-class at the REPL (the user)

The user: "go forms should be first class citizens like arbace and java forms: interpreted in
the repl. bring arbace/clojure, java and go forms to the same level in repl when it is
appropriate. haskell forms are good as data for now". Today Arbace forms are evaluated (the
closure compiler's `Code` nodes, over the compiled program), Java forms are analyzed by
`arbace.classes` and interpreted (`arbace.classes.interp`, classes made at run time), and Go
forms are data only (read, transformed, printed by repr/go, built by Go's toolchain). Next: a
survey of Go forms at the REPL (an interpreter of Go forms beside the compiled path through
`repr/go print` and Go's compiler; their analysis; calls between the three dialects and the
compiled program; prior art), then the build when the running work allows; Haskell forms stay
data.

## 2026-10-17: F2 built, the per-form differential checks

As decided (doc/FUTAMURA-SURVEY.md, "Decided", F2 (a)), on the branch `f2-diff` from `d652647`:
`bin/diffcheck` (`make diffcheck`; `test/diffcheck/`: `gen.ae`, `driver.ae`, `main.ae`, the
exclusions and `known.edn`), a step of `bin/gate --full`. Design (survey, "F2 as built"): a
program built for the check from the gate's executable, carrying both halves: the oracle's forms
(53 corpus files, 11,758 cases) each a namespace of one `(diffcheck.driver/case ...)` macro per
case, compiled by the back end with the runtime's namespaces into one tree and also evaluated,
the two runs compared case by case; and the Java forms of defclass.ae, defclass_corpus.ae,
native.ae and j2g's fixtures (713 cases; 134 classes given to j2g) interpreted against
translated by j2g, the same evaluated cases calling them. Alternatives considered: the Java
forms the back end emits run by the interpreter (cheaper, no Go build, but not the compiled
build's path); the corpora compiled into the test build (Clojure's suite would run on a program
twice its size); a compiling run per corpus file (no cross-file state at analysis, but 50 runs
of the runtime's compilation). Found and fixed in `arbace/aot/emit.ae`: compiled `import`'s
value (nil, where the evaluator and the JVM give the class; 21 cases) and a deftype's `this` in a
fn's code (every compiled defrecord failed j2g's analysis); the shipped program changes in the
back end and its import units only, no re-seed. Found and recorded: j2g's bugs with anonymous
classes taking constructor arguments, a Go name collision for a reify in a nested anonymous fn,
the stub of an unimplemented method throwing UnsupportedOperationException, an abstract class's
constructors missing from reflection, Clojure constants in class bodies; the interpreter's
lambdas without default methods; the back end's limits (code forms in fns, deftypes and reifys;
namespaces of one segment; a proxy made at run time first). 12,449 of 12,471 cases agree, the
22 others recorded with reasons; about 5 minutes from the gate's executable, 40 s cached.
Proposals F2.1-F2.6 (survey) are the user's.

## 2026-10-17: F2 merged; its proposals decided (the user)

F2 (`bin/diffcheck`, merged from `f2-diff` `107b40a`; details in its own entry above and
doc/FUTAMURA-SURVEY.md, "F2 as built"): 12,449 of 12,471 cases agree (11,758 of Clojure's pair,
713 of the Java forms' pair), 22 known with reasons; two back-end fixes found by it (compiled
`import` returned nil; a deftype inside a fn emitted its methods' `this` as the fn's), which
change the shipped program until the next re-seed. Decided: F2.1 (a), F2.2, F2.3, F2.5, F2.6 as
recommended, and F2.4 with the rest of F2.6 fixed too (the user, against "leave recorded"). The
next batch, under one re-seed: the rename (c2a → repr/clojure, arbace.aot → c2j), F3 and F4,
and these fixes; then `bin/diffcheck` in the essential gate.

## 2026-10-17: the survey of Go forms at the REPL

Following the user's direction (entry above): doc/GO-FORMS-REPL-SURVEY.md (from the branch
`go-forms-repl-survey`, `cf1052e`). Findings: Go forms do not run at the REPL today (the
namespace `arbace.go` their heads name does not exist); values cross cheaply since the compiled
program is Go (an Arbace value is a Go `any`); a Go type checker in Arbace is needed either way
(the converter's annotations cover go/types' non-local answers only; estimated 5,000-8,000
lines, against `arbace.classes.analyze`'s 4,375); stock `reflect` cannot make named types,
methods or interfaces, and a static Arbace cannot `dlopen` (Go's `plugin` needs cgo), so the
program's compiled packages are reached through a build-time table, as every Go interpreter
does; `$GOROOT/test`'s programs, already converted by repr/go, are a ready conformance corpus
(interpreted against printed and compiled: F2 extended to Go forms). Measured (ms, fib 22):
gc 0.09-0.15, typed Go closures (a sketch) 5.5, the Java forms interpreter 6.8-7.0, Arbace's
evaluator 8.4-8.6, yaegi v0.16.1 38.8, go/ssa/interp 75. Options: (a) Arbace's own interpreter
(checker and node builder in Arbace, typed closures in hand-written Go forms as jrt is, a table
of compiled packages), (b) an SSA-like IR, (c) yaegi embedded (stale upstream, its own checker,
5.7 times slower than the Java forms interpreter on fib), (d) the compiled path only (a child
process or tool guest; rebuild and restart; a loader of gc's objects). Recommended (a), with a
`go/run-compiled` command (d1); milestones GR0-GR6, 22 to 33 working days; proposals G1-G17
(cited as GR where the fixpoint's G1-G3 could be confused), open.

## 2026-10-17: the second rename carried out

As decided (entries above; doc/NAMING-SURVEY.md, "The second rename as built"), on the branch
`rename-c2j`, merged into main: c2a → `repr/clojure` (`arbace.repr.clojure`, `bin/repr-clojure
read|check`, doc/REPR-CLOJURE.md, test/repr/clojure; `C2A_*` → `REPR_CLOJURE_*`, the guard
extended), `arbace.aot` → `arbace.c2j` (`arbace/c2j/`, test/c2j; its marks on Java forms
`:c2j/*`); kept as the pipeline's name: `ARBACE_AOT`, `ARBACE_NO_AOT`, the packages
`aot_arbace_*`, the tree `target/arbace-go/aot/`, `aot.key`, `Compiler$AOT`, `AOTFn`. Regenerated:
the fixtures' headers and provenance, jdk/'s provenance (911 forms equal), repr/clojure's
`libs.out`. Checks on the branch: the essential gate, the fixpoint G1 = G2 = G3 (`e122ade3...`),
`bin/repr-clojure check` (unit, `--suite`, `--libs`), `bin/diffcheck` (22 known, 0 new), repr/go's
round trip, `bin/j2g-edge verify`. R6-R8 accepted (the user). The seed still embeds the old
names until the next re-seed.

## 2026-10-17: repr/haskell's milestone R1

At the user's request (R1 beside F2), on the branch `repr-haskell-r1`, merged into main; details in
doc/haskell/HASKELL-NOTES.md and doc/haskell/SPEC.md §10.4 ("As built: R1"). Built: the helper
`tools/hsdump/HsDump.hs` on the host's GHC 9.14.1 (verbs dump, tree, lines; the pinned version
checked; `-haddock` on; CPP through GHC's own driver, each dump recording the command, the
included files' hashes and the output), the converter, writer and printer in Arbace
(`arbace/repr/haskell{.ae,/}`), the command `bin/repr-haskell` (helper, corpus, dump, read,
print, roundtrip, test), the corpus file `test/haskell/corpus-ghc-9.14.1.edn` (the source
tarball pinned by SHA-256). Over `base`, `ghc-internal`, `ghc-prim`, `ghc-bignum`: 483 of 483
modules give GHC's tree and every located node's start line equal to the original's (86 with
CPP; 21,707 top-level forms, 639,097 located nodes), no known differences, about 40 s; 26 `.hsc`
modules, 24 boot files and 2 generated ones left for R6. Departures from the spec, decided by the
user as recommended (HR1-HR5): kept out of the executable (j2g's embed exclusions, as
repr/java); the module body and class bodies printed in layout, since GHC's Haddock pass places
doc comments by column only under layout; the stricter lines check and new `:lines` keys; tuple
and unboxed-sum forms in constructor declarations; the round trip and tests in `bin/gate
--full` (a step of their own). `ghc` added to doc/ALPINE.md as a check-only dependency.

## 2026-10-17: Go forms at the REPL decided; GR0 started

The survey's G1-G17 decided as recommended (doc/GO-FORMS-REPL-SURVEY.md, "Decided"): Arbace's
own interpreter of Go forms, a type checker in Arbace, typed Go-closure nodes in hand-written Go
forms, a table of the program's compiled packages, Go packages as namespaces of late-bound vars,
the check over `$GOROOT/test`. GR0 (the spec) started on the branch `go-forms-spec`; GR1-GR6
after the coming re-seed. The essential gate passed on main after repr/haskell R1's merge (7:38).

## 2026-10-17: Go's build cache kept three hours (the user)

The disk reached 87% (Go's build cache 32 GB, `.tmp/` 29 GB, with up to five agents building at
once); the finished work's scratch removed and the cache trimmed of entries unused for two hours
(to 19 GB; 58% used). At the user's request, `bin/lib/gocache.bash`'s default lowered from six
hours to three (`ARBACE_GOCACHE_KEEP_MIN`, 180): Go refreshes an entry's time at most hourly, so
three hours still keeps every build in progress.

## 2026-10-17: GR0, goi's spec

doc/go/GOI-SPEC.md (from the branch `go-forms-spec`, `85f3928`): the spec of `goi`, the
interpreter of Go forms (Go package `arbace/goi`, driven by `arbace.go.*`), named as J2G-SPEC is
after its component. Design: a package's declarations queued while it loads and checked whole
at the end (one after-load hook in `arbace.core/load`); the checker written anew after go/types,
verifying every annotation; nodes as Go closures typed by result, frames as windows of a
per-goroutine stack, captured or address-taken variables in real `reflect.New` cells; calls
through function cells (redefinition seen), bodies built on first call; named struct types
distinct through a `goi:` tag on the first field; proxies with a fmt-aware `Format`, wrappers
in compiled interfaces; no dynamic `MethodByName` (it would keep every exported method linked);
a typed invoker per signature. Measured (Appendix A): fib 22 4.95 ms with frames per call, 2.87
ms on a per-goroutine stack (gc 0.23); `strings.ToUpper` 331 ns through `reflect.Value.Call`, 39
ns through a typed invoker (38 direct); `sort.Slice` of 1e5 550 ms with a `MakeFunc` less, 13.9
ms with a typed adapter (11.1 native); a table of 35 listed packages' 1,081 functions and
variables at most 3.9 MB. Milestones GR1-GR6, 24 to 34 days; decisions S1-S55; proposals
GS1-GS15, open.

## 2026-10-17: goi's GS1-GS15 decided (the user)

All as recommended (doc/go/GOI-SPEC.md, "Decided"). GR1-GR6 after the coming re-seed.

## 2026-10-17: the j2g batch merged (F4, F2's j2g and interpreter fixes)

On the branch `j2g-batch` (`951c8eb`), merged into main; details in doc/FUTAMURA-SURVEY.md, "F4
and F2's j2g fixes as built". F4 (a): `arbace/j2g/start.ae` lists the 82 runtime classes the
program initializes before any user code (derived by `test/j2g/start_classes.ae` from
`ARBACE_TRACE_INIT`'s trace of three runs); each static method of a listed class becomes a
guard-free `C_m_NoInit` called only by compiled code (1,724 methods, 43,830 call sites), after
`jrt.StartCheck` (status 70 when a listed class is not started). Benchmarks (eval-bench as a
namespace): geometric mean 0.87; loop-long 12.20 → 4.37 ms, loop-double 1.55 → 0.53; fib 6.30 →
6.68 (gc now inlines `Numbers`' boxed generic operations; F3 replaces them). F2.2 (anonymous
classes with constructor arguments: `analyze-anon` built the constructor before the superclass's
was chosen), F2.3 (`AbstractMethodError`), F2.4 (Clojure constants made at class
initialization), F2.5 (the interpreter's lambdas get default methods), F2.6's collision (c2j's
classes outside `arbace/` named `A_..._A`), abstract classes' constructors in the member tables
(`InstantiationError`, reflection as the JVM's). diffcheck: 12,463 of 12,471 agree (known 22 →
8, excluded 5 → 1, Java forms classes excluded 19 → 16); the essential gate passed; the fixpoint
G1 = G2 = G3 on the branch. Decided by the user: F4.1 (a) the 82, F4.2 (a) re-measured with F3,
F4.3 (a) a deviation recorded in J2G-SPEC §6.2. The agent once used `pgrep -f` by pattern,
read-only, while waiting on a build (against the session's rule; nothing was signalled).

## 2026-10-17: the survey of a kernel and a user part in the box

At the user's choice: doc/go/KERNEL-USER-SURVEY.md (from the branch `kernel-user-survey`,
`91598b3`). Findings: today the guest cannot run below ring 0/EL1 (amd64: DPL 0 segments only,
no U/S bit, no SYSCALL setup, every IDT gate ends the guest; arm64: every page EL1-only and UXN,
TTBR1 off; TamaGo has no signals or asynchronous preemption). An experiment (a scratch TamaGo
guest on the box's board, the monitor unchanged) dropped to ring 3 on nested KVM: SYSCALL and
SYSRETQ 24.5 ns, INT 0x80 and IRETQ 102 ns, a CR3 switch 96-136 ns, against the doorbell's
13-18 µs and a spawned tool guest's 84-170 ms; Go ran unchanged at ring 3 with every page
user-accessible, until a GC assist ran the collector at ring 3 and failed to unwind at the
crossing. One shared heap cannot give isolation (every allocation a system call, or a collector
trusting user pointers, which is only safe for memory-safe user code, and then the hardware adds
nothing). Prior art: GoTEE (`813dc65a`, read only) runs a whole second TamaGo runtime in ARM user
mode, values crossing as JSON-RPC; TamaGo's `user/linux` runs a `GOOS=tamago` program as a Linux
process with about five system calls. Recommended: a TamaGo runtime per process at EL0 (the same
image, a page table per process, the text shared: about 3.5 MB plus heap and stacks), language
isolation inside processes, S7's guests where virtualization exists, behind one spawn request;
the system calls the doorbell's call block by SYSCALL/SVC; the compilers and the REPL outside
the kernel. Plan KU0-KU6, 17 to 27 days, no patch to Go's runtime. Proposals K1-K12, open.

## 2026-10-17: repr/haskell R2 merged; the box's kernel/user split decided

R2 (branch `repr-haskell-r2`, `530969d`): the interface level, each package built at `-O1` from
the originals and from the printed modules in place, `ghc --show-iface` compared token by token:
483 of 483 modules equal (1,072,106 lines), after the normalizations HR6 adopted into SPEC §9.3
(fingerprints blanked, SrcLoc columns through `$WSrcLoc`, CPP's `addDependentFile` dropped, tokens
not lines); HR7 (`-O1`) and HR8 (one build per side) as recommended; its `--full` step about 3.5
minutes. The kernel/user survey's K1-K12 decided as recommended (doc/go/KERNEL-USER-SURVEY.md,
"Decided"); KU0, its spec, started now (the user, against "after goi").

## 2026-10-17: KU0, the processes' spec; the Mac bundle rebuilt

doc/go/PROCESS-SPEC.md (from the branch `ku0-spec`, `39d75da`): a process is a second TamaGo
runtime at EL0 (one image, a page table per process, text and read-only data shared and
user-executable, data, bss, heap and stacks private, the kernel EL1-only in every table); the
system calls the doorbell's 64-byte block by SYSCALL/SVC, its address in RAX/X0; bytes and
handles across; a process run by a kernel goroutine as one long `Exec` call, so the collector
scans it in an ordinary frame; no patch to Go's runtime through KU5; one spawn request with
`:process`, `:guest` and `:auto` back ends. Experiments: the ring-3 crossings measured again
(SYSCALL and SYSRET 26-31 ns, a CR3 switch 145 ns, the doorbell 18 µs); a two-mode board
prototype builds one image with a guest entry and a process entry, three LOAD segments (R E, R,
RW). KU1-KU6 14 to 23 days; decisions S1-S40; proposals KS1-KS12, open.

For the box's checks on the Mac under macOS 26 (the user): `bin/box-mac-bundle` refused since S9,
comparing the rebuild's sources over all of `tamago/` (which holds `tamago/cmd`) where
`bin/box-rebuild` archives `tamago/goroot` and `tamago/goos`; fixed (`7019817`). The bundle
built at `7019817` (55 MB; the rebuild's expected image from `bin/box-rebuild --arch arm64
prepare host` at that commit). Found on the way: `arbace.j2g.start` (the j2g batch's) is in the
program's `namespaces.txt` but neither compiled nor embedded, so a prepare without ARBACE_PATH
(the box's) reports it not prepared; given to the c2j integration.
