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
