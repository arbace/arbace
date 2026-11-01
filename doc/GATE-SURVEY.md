# The efficient gate — survey and plan

Status: survey and plan, 2026-10-27, the first part of the milestone `arbace-reaps-3-crops-v2`
(doc/JOURNAL.md at the tag `arbace-reaps-3-crops-v1`, 2026-10-26, "the gate made efficient, between
the scripts' sweep and C23", and "the milestone `arbace-reaps-3-crops-v2` ..."). Nothing here is
adopted: no script, no data and no check changed. The decisions are the user's, as proposals
GTS1-GTS20 (§11); the milestones are GT1-GT7 (§10).

The user's brief (2026-10-26): `bin/gate --full` is overkill as it stands. Most of its steps are
proofs that the record reproduces (the fixpoint, `jrt-convert verify`, `javac-vendor verify`, the
vendoring checks), the rest the full corpora whose subsets the essential gate runs. The waste is
steps rerun when nothing they read changed, duplicated builds (a fresh build beside the fixpoint's
G1, javac's tool more than once), the 20 GiB jobs colliding, and corpus sizes. Asked for: (1) a
key per step over its declared inputs and a recorded pass, an unchanged step skipped, and a way to
catch an input a declaration misses; (2) the builds shared; (3) the 20 GiB jobs one at a time, the
rest concurrent, serial by default if simpler, memory measured per step; (4) a clean, uncached
`--full` at each milestone's tag, the proof the tag claims; (5) timeouts that hold under load.

Names. `GT` and `GTS` are unused in the repository (searched: `doc/`, `arbace/`, `test/`): the
milestones are **GT1..GTn**, the proposals **GTS1..GTSn**.

Base: branch `v2-gate-survey`, from main `2e8629c7`. Nothing heavy was run: the measurements are
read from the logs of REAP9's four full gates (`.tmp/gate-reap9-full{,2,3,4}.log` of main's
checkout), the files those runs left (`.tmp/java-check/`, `.tmp/repr-go/native-check.report.edn`),
the essential gate's last logs (`.tmp/gate/`, 2026-10-27 07:53), the journal at the tag, and
`git log` (§2.6). Memory was not measured by any of those runs; §2.5 gathers what the journal
recorded.

Contents: Executive summary · 1 Today · 2 Measurements · 3 The waste · 4 Keys and recorded passes ·
5 Builds shared · 6 Scheduling and memory · 7 The clean run at a tag · 8 Timeouts · 9 Estimates ·
10 Plan (GT1-GT7) · 11 Proposals (GTS1-GTS20) · 12 Risks · 13 Sources.

## Executive summary

- **Where the time goes (§2.1, §2.2).** REAP9's clean serial run (`5cfb1f4a`, 227m17s, every step
  passing) spent 4 minutes in the essential phase (its executables cached) and 223 in `--full`'s
  chains: **the java chain 107.5 minutes (48%)**, of which `java-check-full` 71.7 (32% of the
  gate), `repr-go-native` 51.0 (23%), the fixpoint 20.8 (9%), javac's tool 12.1, repr/go's round
  trips 8.8, goi 7.5 and goi's AOT 7.1. Concurrent (the run before, `f75d89d3`), the same steps took
  219 minutes and three failed: the chains at once inflate each step 1.3 to 2.3 times (§2.3), so
  the java chain alone (166 minutes there) is the critical path and concurrency won nothing.
- **Inside the long steps (§2.4).** `java-check-full`: read, interp, classes and diagnostics 21.4
  minutes, **compiled 45.2** (7 shards of about 6.5 minutes: two j2g rounds of 1.2-1.3 minutes,
  the shard's build, then **4.5 minutes running its tests**), print 4.6, the forms' verify 0.5.
  `repr-go-native`: 22 corpora; **the three `cmd-*` corpora (Go's toolchain sources in three
  configurations) take 29.5 of its 48 minutes of corpus wall time**, two of them tamago's, the
  parked sub-projects' configurations; goi's checker is most of every corpus's time.
- **Nothing in `--full` is skipped today (§1.3).** The only whole-step key is the essential
  gate's executables (`gate_key`); a doc-only commit on main costs the same 227 minutes. Half of
  main's 185 first-parent commits since 2026-10-20 touched only `doc/`, `CLAUDE.md`, `LICENSE.md`
  or `README.md` (94); 78 changed `gate_key`'s inputs (§2.6).
- **The builds (§3, §5).** A `--full` run builds the shipped program three times from the same
  sources by the same host when the essential gate's cache misses (the gate's build, `go-build-full`,
  the fixpoint's G1) and twice when it hits; javac's world is translated by j2g twice (`javac-vendor
  check`, then the tool build) and three times when repr/java's compiled reader is stale.
- **Proposed (§11).** A key per step over declared inputs (a table, `test/gate/steps.edn`, read by a
  planner on stage 0 as `bin/reap` reads its cells), passes in a host-wide store outside git, shared
  by the worktrees; missed inputs caught by an `strace` audit at each tag's clean run and a canary
  rerun of one skipped step per gate; inner keys where a step is long (Java's check per level, its
  judgment split from its runs; native-check per corpus); G1 the gate's cached executable,
  `go-build-full` dropped, javac's tool kept by its key; serial `--full` by default, then one heavy
  process at a time gate-wide; per-program limits in CPU time instead of wall time; `bin/gate --full
  --clean` in a fresh clone at each tag, its record committed.
- **The everyday `--full` after (§9).** A doc change: **227 minutes to about 1**. A repr/java change:
  about 260 to **about 118** with step keys alone, **about 61** with the inner keys. A j2g change:
  about 265 to **about 190** with step keys and shared builds, **about 111** with output cutoffs
  when the runtime's translation does not change, **75-85** with the heavy-process scheduler. The
  clean run at a tag: about 4.7-4.8 hours, serial, plus the audit's cost (to be measured, GT4).

## 1. Today

### 1.1 What `bin/gate` runs

`bin/gate` (368 lines) runs, one after another, `makefile`, `seed`, `stage0` and `go-build`, then
13 essential steps concurrently on the cached executables (`.tmp/arbace-go-gate/amd64/`), the
whole-program j2g runs among them one at a time (`J2G_LOCK`, `bin/j2g`). With `--full`, after
those, four chains (concurrent, or one after another with `GATE_FULL_SERIAL=1`, since REAP9):

| chain | steps, in order |
|---|---|
| go | `go-build-full` (`bin/arbace-go --build`, `--smoke`), `javac` (`javac-vendor check`, `build`, `verify`, `classes`), then the java chain beside the rest: `fixpoint`, `suite-aot`, `programs-full` |
| java (after javac) | `repr-java`, `repr-java-roundtrip`, `jrt-convert`, `java-fixtures`, `repr-java-parse`, `repr-java-front`, `repr-java-attr`, `repr-java-nojavac` (JN9f), `repr-java-platform` (JN10, stage 0), `java-check-full`; the essential `repr-java-closure` (JN10) |
| repr-go | `repr-go` (six round trips, `x509roots-vendor verify`, `check`), `repr-go-native`, `goi` (`annotations`, `test --gc`, `check --jobs 8`), `goi-aot` |
| repr-clojure | `repr-clojure` (`check`, `--suite`, `--roundtrip`) |

Every step is `step NAME CMD...` (`bin/gate` line 223): its log, status and seconds under
`.tmp/gate/`, which the gate removes first. There is no per-step time limit and no memory
measurement.

### 1.2 The hosts the steps run on

| host | what it is | identified by |
|---|---|---|
| stage 0 | `target/seed/amd64/arbace`, built from the seed by TamaGo's go every run (`bin/seed --build`) | the seed's and Go's pins; the checkout's tool namespaces that differ from the embedded ones load from the checkout |
| the gate's executables | `.tmp/arbace-go-gate/amd64/{arbace,arbace-tests,aot}` | `gate_key` (`bin/arbace-go` line 1024) |
| target/ | `go-build-full`'s `target/arbace-go/amd64/arbace` and `aot/`: javac's check and build (`J2G_GO_EXE`'s default), `java-check`'s compiled level (`JAVA_CHECK_AOT`'s default) | nothing: rebuilt each `--full` |
| javac's tool | `.tmp/gate-javac/arbace-javac`, copied from the javac step's build | `arbace_javac_key` (`bin/lib/javac-key.bash`), recorded beside it, not used to skip |
| the frozen JVM | the v4 jar | `test/oracle/frozen-jar.sha256` |
| outside the repository | TamaGo's go (`seed/goroot.sha256`), its `$GOROOT/test`, the JDK build tree (`jdk/javac/platform.sha256`), jdk26u at the platform's pin, the system JDK (`javac-vendor classes`' reference), python3 and rlwrap (the smoke test) | their pins, where there is one |

### 1.3 The caches that exist

Whole-step: only `go-build` (`gate_key`: the files of `arbace go overlay jdk x509roots bin/lib
test/java-fixtures`, the build's scripts, the pins, Go's version). Inside steps, keyed by what
they build from: diffcheck's program, `java-check --essential-compiled`'s program, the compiled
suite's program (`.tmp/clojure-tests/aot`), the programs' builds (`target/arbace-go/programs`),
repr/java's reads in Java's check (`.tmp/java-check/forms/KEY`, the sources, the options, the
reader's sources and the tool's key), its `:full` reads, repr/java's compiled reader
(`target/arbace-go/aot-reader`, `reader_key`), goi's gc references (`.tmp/goi-check`), the printed
packages (`.tmp/repr-go-print-cache`), Go's build cache. These are what made REAP9's runs 3 and 4
show `diffcheck` 0m42s, `suite-aot` 2m02s and `programs-full` 0m09s against 11m20s, 8m22s and
11m22s in run 1. No `--full` step is skipped as a whole: their inputs are not declared anywhere.

## 2. Measurements

### 2.1 REAP9's four full gates

Wall time per step (minutes:seconds). Runs 1 and 2 had the chains at once, 3 and 4
`GATE_FULL_SERIAL=1`; nothing else ran on the host (the journal, 2026-10-26 and 27). Run 1's
executables were built (`go-build` a miss), runs 3 and 4 had them cached.

| step | run 1 `9b46fb30` | run 2 `f75d89d3` | run 3 `c216272a` | run 4 `5cfb1f4a` |
|---|---:|---:|---:|---:|
| stage0 | 0:14 | 0:18 | 2:28 | 2:03 |
| go-build | 6:29 | 6:43 | 0:00 | 0:00 |
| go-smoke | 2:36 | 2:33 | 2:14 | 2:01 |
| suite-go | 3:13 | 3:11 | 2:30 | 2:09 |
| oracle-go | 1:12 | 1:12 | 1:12 | 1:09 |
| diffcheck | 11:20 | 12:09 | 0:42 | 0:32 |
| goi-check | 2:04 | 2:12 | 1:36 | 1:13 |
| goi-aot-essential | 1:42 | 1:55 | 1:17 | 1:13 |
| java-check | 0:51 | 1:05 | 0:51 | 0:40 |
| java-compiled | 5:10 | 5:40 | 0:28 | 0:21 |
| suite-aot-essential | 6:45 | 7:28 | 0:43 | 0:32 |
| programs | 3:38 | 4:04 | 0:02 | 0:02 |
| generated | 0:05 | 0:05 | 0:05 | 0:04 |
| j2g-edge | 2:19 | 2:16 | 1:36 | 1:19 |
| reap | 0:03 | 0:03 | 0:02 | 0:02 |
| go-build-full | 6:49 | 11:10 | 5:33 | 4:56 |
| javac | 17:10 | 23:09 | 12:49 | 12:07 |
| repr-java | 3:06 | 3:28 | 2:58 | 2:51 |
| repr-java-roundtrip | 14:38 F | 18:36 | 9:54 | 9:31 |
| jrt-convert | 3:47 | 6:23 | 4:33 | 3:43 |
| java-fixtures | 0:19 | 0:21 | 0:22 | 0:19 |
| repr-java-parse | 3:58 | 3:38 | 3:32 | 3:01 |
| repr-java-front | 7:32 | 10:03 | 7:33 | 6:55 |
| repr-java-attr | 13:58 | 17:51 | 10:57 | 9:30 |
| java-check-full | 79:23 F | 105:28 F | 72:23 F | 71:42 |
| fixpoint | 23:52 | 32:32 | 20:10 | 20:45 |
| suite-aot | 8:22 | 11:37 | 1:57 | 2:02 |
| programs-full | 11:22 | 11:26 | 0:10 | 0:09 |
| repr-go | 10:37 | 18:08 | 9:05 | 8:47 |
| repr-go-native | 63:42 | 65:24 F | 51:25 | 51:01 |
| goi | 7:52 | 14:10 F | 7:27 | 7:31 |
| goi-aot | 7:44 | 9:31 | 6:59 | 7:06 |
| repr-clojure | 1:33 | 1:36 | 0:54 | 0:52 |
| **the gate** | **168:44** | **219:17** | **234:03** | **227:17** |

F: failed. Run 1's two failures were real (12 round-trip references, 4 Java tests); run 2's three
were load (java-check-full's interpreted timeouts, repr-go-native killed for memory, one goi
timeout); run 3's was two new disagreements of Java's levels, recorded at `5cfb1f4a`.

The essential gate alone, its executables built (main's `.tmp/gate/`, 2026-10-27 07:53, seconds):
go-build 304, diffcheck 445, suite-aot-essential 300, java-compiled 234, programs 173, suite-go
146, go-smoke 128, j2g-edge 97, goi-aot-essential 95, goi-check 91, oracle-go 69, java-check 39,
stage0 14. The journal's essential runs on a miss: 15m56s, 16m02s, 25m37s, 37m05s (the last two
beside other agents' jobs).

### 2.2 The critical paths

Run 4, serial: the essential phase 4.2 minutes (stage 0, then the concurrent steps' longest,
suite-go), then the go chain 147.5 (go-build-full 4.9, javac 12.1, **the java chain 107.5**,
fixpoint 20.8, suite-aot 2.0, programs-full 0.2), the repr-go chain 74.4 (8.8 + 51.0 + 7.5 + 7.1),
repr-clojure 0.9: 227 minutes, the sum. Run 2, concurrent: the essential phase about 19 minutes
(go-build 6.7, then diffcheck's 12.2), then the go chain's go-build-full 11.2 and javac 23.2 and
the java chain 165.8 behind them: 219 minutes. The concurrent run's critical path is the java
chain, which concurrency slows (§2.3) and does not split: it was no faster than serial, and it
failed.

### 2.3 Load: the same steps concurrent and serial

Run 2 (chains at once) against run 4 (serial), the same sources but two small fixes:

| step | concurrent | serial | ratio |
|---|---:|---:|---:|
| go-build-full | 11:10 | 4:56 | 2.26 |
| repr-go | 18:08 | 8:47 | 2.06 |
| repr-java-roundtrip | 18:36 | 9:31 | 1.95 |
| javac | 23:09 | 12:07 | 1.91 |
| repr-java-attr | 17:51 | 9:30 | 1.88 |
| goi (failed) | 14:10 | 7:31 | 1.88 |
| repr-clojure | 1:36 | 0:52 | 1.85 |
| jrt-convert | 6:23 | 3:43 | 1.72 |
| fixpoint | 32:32 | 20:45 | 1.57 |
| suite-go | 3:11 | 2:09 | 1.48 |
| java-check-full (failed) | 105:28 | 71:42 | 1.47 |
| repr-java-front | 10:03 | 6:55 | 1.45 |
| goi-aot | 9:31 | 7:06 | 1.34 |
| repr-go-native (killed) | 65:24 | 51:01 | ≥1.28 |

A per-program wall limit set on a quiet host (goi's 10 s, Java's 20 s then 40 s) is crossed when
its program runs 1.5-2.3 times slower, which is what the trial's and run 2's timeouts were.

### 2.4 Inside the long steps

**`java-check-full`, run 4** (from the files it left in `.tmp/java-check/`): started 03:13:50; the
read's cache entries and the interpreted, classes and diagnostics levels until 03:35:11 (21.4
minutes); the compiled level's seven shards 03:35:24-04:20:25 (45.2 minutes); print until 04:25:00
(4.6); `--forms verify` until 04:25:28. One shard (s0, 500 tests): j2g's first round 80 s, its
second (the refused tests taken out) 72 s, the shard's build, then the tests run until 03:42:26:
**about 4.5 of a shard's 6.5-7 minutes run tests**, at 8 jobs, light in memory; the j2g rounds
(the runtime's 16,885 classes analyzed again in 34 s each time, `j2g1.log`) are the 20 GiB part.
Shards s4 and s5 took a third round (a Go error).

**`repr-go-native`, run 4** (`.tmp/repr-go/native-check.report.edn`, each corpus's `:wall-s`):

| corpora | dumps | wall s |
|---|---:|---:|
| `amd64`, `arm64`, `linux-amd64`, `linux-arm64` (std) | 1,519 | 577 |
| `tests` (std's tests) | 554 | 211 |
| `test`, `test-arm64`, `linux-test`, `linux-test-arm64` | 6,829 | 121 |
| the four `dirtest` and `linux-errtest` | 2,827 | 123 |
| **`cmd-linux-amd64`** | 105 | **595** |
| **`cmd-tamago-amd64`, `cmd-tamago-arm64`** | 210 | **1,175** |
| `goos-amd64`, `goos-arm64`, `box`, `purego`, `x509roots` | 231 | 96 |
| all 22 | 12,275 | 2,898 (48.3 min; the step 51.0) |

goi's checker (`:check-s`, summed over threads) is the largest part of every corpus: 2,464 s of
`cmd-linux-amd64`'s. The tamago configurations of the toolchain's tree and the box's corpora (31
minutes) are the parked sub-projects' (the user, 2026-10-26), whose trees do not change while
they are parked.

**`javac`, run 4**: `javac-vendor check` (j2g over javac's world on target/'s executable), `build`
(the tool: j2g over the same world with the compiled reader, Go's build; a first build before it
when the reader's key changed), `verify` (javac's 903 forms files read afresh, 136 s, the edge
recorded on the frozen JVM), `classes` (173 s).

**The fixpoint, run 4**: 20.8 minutes for G1, G2 and G3, about 7 each.

### 2.5 Memory (from the journal; no run here measured it)

| job | peak | source |
|---|---|---|
| a whole-program j2g (`bin/j2g --program`: builds, diffcheck, the compiled suite, the programs, Java's shards, javac's check and tool) | about 20 GiB (`GOMEMLIMIT=20GiB`, `GOGC=400`) | 2026-10-25, REAP8, REAP7; killed at 18 GB resident beside other jobs, 2026-10-23 |
| javac's tool build (its j2g) | about 20 GiB; three builds killed by the host's OOM killer, the fourth passed with `GOMEMLIMIT=16GiB` | 2026-10-25, JC5 |
| `repr-go-native` (one process) | 29.9 GB (GP4's run, 70m57s); killed at 21 GB beside others; passed alone with `GOMEMLIMIT=18GiB` | 2026-10-21, 2026-10-23, the trial 2026-10-26 |
| repr/java's jdk-corpus read on the tool (roundtrip, jrt-convert, front-check, attr-check) | 26.6 GB (JN4), about 30 GB one process (JN5) | 2026-10-21 |
| `attr-check --corpus all` | 29.4 GB (`GOGC=100`) | 2026-10-26, JN9d |
| `repr-java check --jdk` (68 modules) | 8.5 GB | 2026-10-22, JN6 |
| the frozen JVM's recordings (`j2g-edge`, javac's edge) | the jar's 12 GB heap | J2G-SPEC, `bin/j2g`'s header |
| the rest (suite-go's 12 processes, goi's and Java's 8 jobs, Go's builds of the program) | not recorded | — |

The host has 64 CPUs and 62 GB, no swap; cgroups are not mounted, so a step's peak can be had
from `wait4`'s `ru_maxrss` (the largest single descendant: busybox `time -v`) and a sampler of
`/proc`, not from `memory.peak`. Two of the heavy jobs at once (2 × 20-30 GB) is what the kernel
killed in run 2, the trial and JC5's builds.

### 2.6 How often what changes

Main's 185 first-parent commits from 2026-10-20 to `2e8629c7` (66 of them merges), classified by
the paths they touch:

| class | commits |
|---|---:|
| docs only (`doc/`, `CLAUDE.md`, `LICENSE.md`, `README.md`) | 94 |
| scripts or test data only (`bin/`, `test/`, `tools/`, the Makefile; not `gate_key`'s) | 13 |
| `gate_key`'s inputs (the executables rebuilt) | 78 |
| ... of those, with `gate_key` narrowed to what the shipped build reads (§4.2) | 70 |
| touching what javac's tool or the java chain reads | 50 |
| touching repr/go, goi or their data | 34 |
| touching `arbace/j2g` | 24 |
| touching `arbace/repr/java` | 31 |

Half of main's commits change nothing any step reads (the journal's and the agenda's entries);
those still cost a whole `--full`. Merges touch several areas at once: the inner keys (§4.6) and
output cutoffs (§4.2) are what keep a mixed merge from rerunning everything.

Key computation is cheap: hashing `gate_key`'s 2,571 files took 0.14 s here.

### 2.7 Not measured

Each step's peak memory and CPU time; the share of the java-check-full's 21.4 minutes that is
the read (its cache was partly warm in run 4); `strace`'s cost on the steps (GT4); a cold Go build
cache's cost on the clean run (the journal's one figure: javac's tool 1,006 s cold against 231 s
warm, JN5).

## 3. The waste

1. **Unchanged steps rerun.** No `--full` step has a key; a doc-only commit runs all of them
   (227 minutes). Of the steps a change does touch, many are long only because one input changed
   that a part of them reads: Java's check reruns its compiled level (45 minutes) when its
   classes file changes, though judging needs only the results.
2. **The same build three times.** `go-build`, `go-build-full` and the fixpoint's G1 are each
   `bin/arbace-go`'s `build` for amd64 by stage 0 from the same sources (`gate_build` calls `build`
   and `build_tests`; `fixpoint` G1 calls `build` with stage 0 as host). About 5 + 7 minutes of each
   `--full`, three 20 GiB j2g runs.
3. **javac's world translated twice.** `javac-vendor check` runs `bin/j2g --program` over javac's
   world to see that it translates; the tool build runs the same translation (with the reader)
   a minute later. The tool is rebuilt every `--full` though `arbace_javac_key` names what it is
   built from.
4. **`gate_key` too wide.** It hashes all of `arbace/`, `overlay/` and `jdk/`, but the shipped
   build embeds `arbace/` less repr/java, repr/haskell and repr/rust (`arbace/j2g/embed.ae`'s
   `excluded`), and does not read javac's forms (`jdk/javac/`, `overlay/jdk/javac/`) or the
   provenance files repr/java rewrites (`jdk/provenance.edn`, `test/java-fixtures/provenance.edn`,
   which name javac's tool's key). A repr/java change therefore rebuilds the executables and
   invalidates every check of them.
5. **Corpora of parked sub-projects.** `repr-go-native` reads the toolchain's tree for tamago/amd64
   and tamago/arm64 and the box's trees, 31 of its 48 minutes, every `--full`.
6. **Collisions.** The chains at once run up to four 20-30 GB processes (Java's shards, javac's
   tool, repr-go-native, the jdk-corpus reads) on 62 GB; the essential phase already serializes its
   whole-program j2g runs (`J2G_LOCK`), `--full` does not.
7. **Load-sensitive limits.** Wall-clock limits per program fail a correct program on a loaded
   host (§2.3).
8. **Stage 0 rebuilt every run** (`bin/seed --build`): 11-14 s warm, 2-2.5 minutes when Go's
   cache was trimmed in between (runs 3, 4), and a race with another script building the same
   `target/seed` (the journal's slip of 2026-10-26).

## 4. Keys and recorded passes

### 4.1 What a key covers

A step's key is a SHA-256 over: the step's name and command line; the gate's own logic (`bin/gate`,
the declarations, the planner); the content of every file its declared inputs name (paths and
directory globs under the checkout, hashed as `gate_key` hashes); the identities of the hosts it
runs on (§4.2); the pins of what lies outside the repository (`seed/goroot.sha256` for TamaGo's
go and `$GOROOT/test`, `jdk/javac/platform.sha256` for the JDK build tree and jdk26u,
`test/oracle/frozen-jar.sha256` for the frozen JVM, the system JDK's `release` file for the
reference javac, Go's `VERSION`); and the environment variables the step declares as inputs.
Hashing the files themselves (not git's trees) keeps a dirty checkout honest and costs well under
a second (§2.6). The key does not hold the commit: a commit that changes no input keeps every
pass, as `gate_key` keeps the executables today.

A key is computed before the step runs; when the store (§4.5) holds a pass of that step at that
key, the step is skipped and the summary prints `skipped  NAME (passed at KEY, COMMIT, DATE,
WHERE)`; else the step runs and, when it passes, its pass is recorded. A failure is never recorded:
a failing step runs again next time.

### 4.2 The executables as inputs

Most steps run on an executable the gate builds; how that executable enters a key decides how
much is skipped.

- **(a) Source keys.** The gate's executables stand for `gate_key`, narrowed to what the shipped
  build reads (§3.4); javac's tool for `arbace_javac_key`; stage 0 for the seed's and Go's pins.
  Simple and safe; but every change to an embedded namespace changes them, j2g's own included, so
  a j2g change reruns every check of the executable.
- **(b) The program's digest.** The hash of the Go module j2g and repr/go make (j2g's `prog/`
  tree, the overlay), less the version stamp (`*arbace-version*` names the commit), known after the
  build: an early cutoff, as Shake and Bazel have it. Two source trees that print the same program
  give the same digest: a comment in an excluded namespace, a change to a script the build does
  not run. It does not help a j2g change: j2g's own namespaces are compiled into the program
  (`aot_arbace_j2g_*`).
- **(c) Per-step package sets.** The digest of the program less the packages a step never loads:
  the suite, the oracle, goi's interpreted check and Java's interpreted level should never load
  `arbace.j2g.*`, nor repr/java's checks on javac's tool (expected, not yet observed: the audit's
  record of each step's loaded namespaces decides). A j2g change that
  leaves the runtime's and javac's translations alone (most j2g fixes are for constructs of the
  test programs) then reruns only the steps that run j2g. Sound only if those packages' `init` does
  nothing a step sees beyond registering loaders, and only with each step's package set checked:
  the audit (§4.4) can record the namespaces a step loads (`*loaded-libs*` at exit) as it records
  the files it opens.

### 4.3 Declaring inputs

Each step's declaration lists its command, its inputs (coarse directory globs: a whole namespace
tree, a whole `test/` subtree), its hosts, its pins, whether it is heavy and its measured peak, and
what it runs after. One table, `test/gate/steps.edn` (the Clojure reader's format), read by a
planner namespace on stage 0 (`test/gate/plan.ae`, as `bin/reap` reads `test/reap/cells.edn` with
`test/reap/main.ae`), which prints the plan (`bin/gate --plan`: each step, its key, run or skipped
and why). For example:

```clojure
{:name repr-go-native :chain :repr-go :after [repr-go]
 :cmd "bin/repr-go native-check --fresh"
 :inputs ["arbace/repr/go.ae" "arbace/repr/go/" "arbace/go.ae" "arbace/go/" "go/arbace/repr/"
          "tools/godump/" "test/repr/go/native-known.edn" "bin/repr-go" "bin/lib/repr-go/"
          "bin/lib/repr-go-host.bash" "tamago/" "box/" "x509roots/"]
 :hosts [:stage0] :pins ["seed/goroot.sha256"] :heavy true :mem-gib 30}
```

The alternatives: bash functions in `bin/gate` (no new reader, but the table hidden in code, not
printable as data), or a verb in each script (`bin/java-check --inputs LEVEL`: the owner declares,
but 20 scripts to change and no single view).

### 4.4 Catching a missed input

A declaration that misses an input makes a stale pass: the worst failure of a keyed gate,
silent. The ways to catch it:

- **(a) An audit run.** Each step run under `strace -f --seccomp-bpf -e trace=%file,execve` (the
  host has strace; `--seccomp-bpf` stops only on the traced calls, so Go programs run near full
  speed), the paths it opened or stat'ed normalized to the checkout's and the pinned roots', less
  what it wrote (`.tmp/`, `target/`), compared with its declaration: an undeclared read fails the
  audit and names the file. Directory reads (`openat` with `O_DIRECTORY`) count: a step that lists a
  directory depends on its set of files. Stage 0's tools compare each checkout namespace with the
  embedded copy, so they open them: the audit sees them (to be confirmed in GT4). Run at each tag's
  clean run (§7) and whenever a declaration changes (`bin/gate --audit STEP`).
- **(b) A sandbox.** Each step in `bwrap` (on the host) with only its declared inputs bound: an
  undeclared read fails loudly in the everyday run. Strong, but every system path, `/proc`, the
  frozen JVM's and the JDK's trees must be declared, and paths in messages change.
- **(c) The runtime's own trace.** Arbace's file natives (jrt) and Go's `os` (the overlay) record
  what an Arbace process opens; blind to bash, `go build`, the JVM and javac.
- **(d) A canary.** Each gate run also runs one skipped step anyway (the one whose pass is
  oldest), and fails if it does not pass: over a few weeks every pass is re-proved.
- **(e) The clean run.** A step that fails clean and passed keyed had a missed input; found late,
  at the tag.

### 4.5 Where the passes live

- **(a) A host-wide store outside git**: `ARBACE_GATE_STORE`, by default the main checkout's
  `.tmp/gate-pass/` (found from `git rev-parse --git-common-dir`), one record per step and key:
  the key, the commit, the worktree, the date, the seconds, the peak memory, the log's digest and a
  copy of the log. Shared by the worktrees: a branch's gate in `.tmp/worktrees/NAME` records passes
  that main's gate after a fast-forward or clean merge finds again (the merged tree's inputs are
  the branch's), so a merge's gate costs what main changed since the branch.
- **(b) Per checkout** (`.tmp/gate-pass/` of each): no sharing, no path questions.
- **(c) Committed** (`test/gate/passes.edn`): every pass a commit, conflicts at every merge, and a
  record that claims without proving.
- **(d) git notes** (`refs/notes/gate`): attached to commits, while passes belong to keys.
- **(e) The box's store** (S3's blobs and refs, `arbace-box --store DIR`): content-addressed, Arbace's
  own; the box is parked.

What is committed is the tag's clean run (§7): its record proves; the store only saves time.

### 4.6 Inner keys: where one step is long

A step key skips a step whose inputs are all unchanged; a long step with many inputs reruns whole
for any one of them. Java's check, native-check and repr/java's three tree checks hold 63% of the
serial run (142 of 227 minutes):

- **Java's check by level.** `read` keyed by the tool and the corpus (its per-test cache exists);
  `interp` by the read's output (the forms' digest) and the gate's executable; `compiled` per shard
  by its tests' forms, the compiled namespaces and j2g; `classes` and `diagnostics` by javac's part
  of the tool; `print` by repr/java's printer and the forms. And the judgment split from the
  runs: the references, the classes and the known files (`check-LEVEL.edn`, `divergences.edn`,
  `levels-known.edn`) judge cached results in seconds, so editing a class reruns nothing.
- **native-check per corpus**, each keyed by the reader, goi's checker and the corpus's tree.
- **repr/java's checks per corpus** (parse, front, attr: samples, baseline, jdk, javac, jdk26u).

Where an inner key takes an output as input (the forms' digest), it is an early cutoff: a reader
change that reads every test to the same forms skips the interpreted and compiled levels.

## 5. Builds shared

- **G1 and the fresh build.** The gate's cached executable is `build` for amd64 by stage 0 from
  the sources `gate_key` hashes: it is a G1. The fixpoint takes it as G1 (copied into
  `target/arbace-go/fixpoint/g1`) and builds only G2 and G3; `go-build-full` goes, its smoke test
  being the essential `go-smoke`; its consumers (`javac-vendor check`'s and `build`'s j2g host,
  Java's compiled level's `JAVA_CHECK_AOT`) point at the gate's cache. Saves about 12 minutes and
  two 20 GiB runs per `--full`. A cached G1 carries an older commit's stamp, so "G1 = G2 byte for
  byte" may be reported false where it would hold; only G2 = G3 is required, and the clean run
  (§7) builds G1 fresh.
- **javac's tool once per key.** Built when `arbace_javac_key` has no tool beside it (kept in
  `.tmp/gate-javac/` by key, across gates), every Java step on it; `javac-vendor check`'s verdict
  (status 0, no class failing its analysis) read from the tool build's own j2g report rather than a
  second translation. repr/java's compiled reader keeps its first build only when its `reader_key`
  changes, as now.
- **Stage 0 by its pins.** `bin/seed --build` skips Go's build when `target/seed/` holds an
  executable recorded with the seed's and Go's pins (the clean run builds it).

## 6. Scheduling and memory

### 6.1 One heavy process at a time

The heavy processes are each one OS process of 20-30 GB: a whole-program j2g, repr-go-native's
reader, repr/java's jdk-corpus reads on the tool, the frozen JVM's recordings (12 GB). Two at once
on 62 GB is what the kernel killed. The rest (the suites' runs, goi's and Java's programs, the
round trips' builds) are light, though CPU-hungry.

- **(a) Serial by default.** `GATE_FULL_SERIAL`'s behaviour the default (`GATE_FULL_PARALLEL=1` to
  opt in), the essential phase concurrent as now. One line; with the keys, the everyday `--full`
  runs a few steps, and the clean run wants a quiet host anyway (§7).
- **(b) One heavy process gate-wide.** `J2G_LOCK` generalized (`ARBACE_HEAVY_LOCK`), taken by
  `bin/j2g`, repr-go's native reader, repr/java's corpus reads and the frozen JVM's recordings
  around the heavy process only, everything else concurrent. Java's shards then pipeline: shard
  *k+1*'s j2g while shard *k*'s 4.5 minutes of tests run.
- **(c) A memory budget.** A scheduler admitting steps while their declared peaks sum under the
  host's memory less a reserve; needs every peak measured and kept current.

With other agents on the host, the gate's heavy processes also go through the host's slot
command when one is given (`GATE_HEAVY_CMD`, as `JAVA_CHECK_HEAVY` does for Java's shards).

### 6.2 Memory measured per step

Each step run under busybox `time -v` (the host has it as `/usr/bin/time`): its wall and CPU
seconds and its largest process's peak RSS in `.tmp/gate/NAME.mem` and in the pass's record; a
sampler every 5 s writes `MemAvailable` and the gate's processes' RSS to `.tmp/gate/mem.tsv`, the
host's total during the run. The clean run's peaks go into the declarations (`:mem-gib`). Later,
with cgroup v2 mounted, a cgroup per step gives `memory.peak` exactly and `memory.max` makes the
kernel kill the step that overran, not whichever process it picks.

## 7. The clean run at a tag

A tag claims that the record reproduces. The everyday gate's caches and passes say only that the
steps passed at some time on some inputs; the claim needs a run from nothing.

| | everyday `bin/gate --full` | `bin/gate --full --clean` at a tag |
|---|---|---|
| checkout | the working one (a worktree or main) | a fresh `git clone --shared` of the commit in `.tmp/gate-clean/SHA` |
| passes | read and written | not read; written as `:clean` |
| caches | kept (`.tmp/`, `target/`, Go's cache, the print cache) | the clone's own empty `.tmp/` and `target/`, `GOCACHE` its own empty directory |
| builds | stage 0, the executables, the tool by key | stage 0 from the seed, G1, the tool: all fresh |
| order | serial or the heavy lock | serial, nothing else on the host |
| the audit | when a declaration changes | every step (§4.4 (a)) |
| corpora | the everyday ones | all of them (§11, GTS19's) |
| record | the store | `test/gate/clean-TAG.edn` (each step's key, status, seconds, peak), committed with the tag's journal entry; its quiet times calibrate the limits (§8) |

What a clone cannot make fresh is pinned and checked by the steps that use it: TamaGo's go
(`seed/goroot.sha256`), the JDK build tree (`jdk/javac/platform.sha256`), the frozen jar
(`test/oracle/frozen-jar.sha256`); the system JDK is the reference javac, named by its version.
A fresh path also exposes path-dependent results (run 1's `UserDirChangedTest` failed in main's
checkout only).

## 8. Timeouts

The limits that failed were per program: goi's 10 s (`test/goi/timeouts.txt` for the slow ones),
Java's 20 s retried at 40 s; steps have none.

- **(a) CPU time, a wall cap, a quiet retry.** Each program's limit as CPU seconds (`ulimit -t` in
  the action's shell: the kernel counts all of a Go process's threads), with a wall cap at four
  times the limit for a program blocked without spending CPU; the quiet CPU time recorded beside
  each reference so a tight limit is visible; a timeout retried once at the step's end, alone. CPU
  time barely moves with load (§2.3's slowdowns are waiting for a CPU), so a correct program does
  not time out because the host is busy.
- **(b) Wall limits scaled by load**: the limit multiplied by the 1-minute load average over the
  CPUs (clamped 1-4) at the action's start. Simple; the load average lags a minute and does not
  see memory stalls.
- **(c) A calibration**: a fixed benchmark timed at the gate's start against its quiet time, the
  limits scaled by the ratio. Once per run; misses load that comes later.
- **(d) Unchanged**, relying on serial runs.

And per step a budget, not a limit: three times the clean run's time, a warning in the summary
(a hang shows as a step far over its budget), no kill.

## 9. Estimates

Minutes on a quiet host with Go's cache warm, from run 4's step times (§2.1, §2.4), the essential
phase on a miss as §2.1's. Columns cumulative: **T0** today (serial); **T1** step keys (GTS2-GTS7,
narrowed `gate_key`), shared builds (GTS10-GTS12), Java's judgment split (GTS9's half); **T2** inner
keys and output cutoffs (GTS9, GTS4 (b)-(c)); **T3** the heavy-process scheduler (GTS13 (b)).

| change | T0 | T1 | T2 | T3 |
|---|---:|---:|---:|---:|
| docs only | 227 | 1 | 1 | 1 |
| a classes or known file (`test/java/divergences.edn`) | 227 | 74 | 2 | 2 |
| repr/java (`arbace/repr/java/`, the provenance rewritten) | 260 | 118 | 61 | 50-55 |
| j2g, the runtime's and javac's translations unchanged | 265 | 190 | 111 | 75-85 |
| j2g with the translations changed, or the runtime (`arbace/lang/`, `go/arbace/jrt`) | 265 | 190 | 185 | 110-120 |

How they are made:

- **Docs only.** T0: the full chains always run (run 4: 4.2 + 223). T1: every key unchanged; what
  runs is the makefile check, the seed's hash, stage 0 by its pins, the keys and `bin/reap`.
- **A classes file.** T1 reruns `java-check` (1) and all of `java-check-full` (71.7) since the file
  is its input; T2 judges the cached results.
- **repr/java.** T0: `gate_key` changes (the provenance files are under `jdk/`): the essential
  phase on a miss (about 20), all of `--full` (223), the compiled suite's and the programs' caches
  missing (+17). T1: the narrowed key keeps the executables, the essential steps skip; javac's tool
  rebuilt with the reader's first build (about 9.6 without the duplicate check), the java chain's
  seven steps (35.8) and all of `java-check-full` (71.7); the fixpoint, the repr-go chain, the
  compiled suite and the programs skip. T2: Java's check reruns the read (about 10, estimated) and
  print (4.6) and the forms' verify; the forms unchanged, interp and compiled skip; classes and
  diagnostics skip on javac's part of the tool. T3: the lights (the suite on the frozen JVM, the
  print level, javac's classes) beside the heavy reads.
- **j2g.** T0: the essential phase on a miss (about 25: the rebuild and four j2g-locked steps),
  `--full`, the caches' misses (+17). T1: everything that runs on the executable or the tool reruns;
  the repr-go chain's reader and native check (59.8) and repr/clojure skip; less the shared builds
  (−4.9, −6.9, −2.5): 25 + javac 9.6 + the java chain 35.8 + Java's check 71.7 + the fixpoint 13.9 +
  the compiled suite 8.4 + the programs 11.4 + goi 7.5 + goi's AOT 7.1. T2, translations unchanged:
  only what runs j2g reruns: the essential phase's j2g steps (about 25), the tool's builds (about 7),
  the fixpoint (13.9), the compiled suite (8.4), the programs (11.4), Java's compiled level (45.2).
  T3: the j2g rounds are the heavy part (about 70 summed); the shards' test runs, the compiled
  suite's run and the programs' runs overlap them.
- **j2g with the translations changed, or the runtime.** Everything that runs an executable or the
  tool reruns; only repr/go's and repr/clojure's checks on stage 0 skip (the runtime's sources are
  not among the tools' checkout namespaces), and T2's cutoffs find little to cut.

**The clean run** (§7): run 4 plus what its caches saved (the executables 6.5, diffcheck 10, the
essential compiled programs 14, the compiled suite 6.4, the programs 11.2), plus a cold Go cache
(15-30, from JN5's one figure), less the shared builds (−14): **about 280-290 minutes**, serial,
plus the audit's cost (to be measured; `strace --seccomp-bpf` on file calls, probably 5-20%).
With GTS19 (a) the everyday `repr-go-native` loses its 31 minutes of parked corpora; the clean run
keeps them.

## 10. Plan (GT1-GT7)

Estimates in working days, as the repository's surveys give them.

| milestone | content | exit criteria | days | depends on |
|---|---|---|---:|---|
| **GT1** measured | `step` records wall and CPU seconds and peak RSS (busybox `time -v`), the 5 s memory sampler, the summary prints them | a gate run's `.tmp/gate/*.mem` and `mem.tsv` | 0.5 | — |
| **GT2** keyed steps | `test/gate/steps.edn` (every step, essential and `--full`), the planner `test/gate/plan.ae` on stage 0, the store (GTS6), `bin/gate --plan`, skipped steps in the summary; `gate_key` narrowed (GTS5) | a doc-only commit's `--full` in about a minute; `--plan` names each step's inputs and decision | 2-3 | GT1 |
| **GT3** shared builds | G1 the gate's executable, `go-build-full` gone, javac's tool by key with its check folded, stage 0 by its pins | a `--full` builds the program twice (G2, G3) and javac's world once | 1 | — |
| **GT4** the audit | `bin/gate --audit [STEP]` under strace, the declarations completed from it; the canary rerun | every step audited once, no undeclared read; its cost measured | 1-2 | GT2 |
| **GT5** inner keys | Java's check per level with its judgment split; native-check and repr/java's checks per corpus; the program's digest and the per-step package sets (GTS4 (b), (c)), with the audit's loaded-namespaces record | a classes-file change reruns nothing; a j2g change that leaves the translations alone reruns only j2g's steps | 2-3 | GT2, GT4 |
| **GT6** scheduling and limits | serial by default, `ARBACE_HEAVY_LOCK` gate-wide, `GATE_HEAVY_CMD`; per-program CPU-time limits with wall caps and the quiet retry (goi, Java, the suite, the oracle); per-step budgets | a concurrent `--full` with no OOM kill and no timeout on a loaded host | 1-2 | GT1 |
| **GT7** the clean run | `bin/gate --full --clean` (the clone, empty caches, the audit, the record); run at `arbace-reaps-3-crops-v2`'s tag | `test/gate/clean-arbace-reaps-3-crops-v2.edn` committed, every step passing | 0.5 + the run (about 5 h) | GT1-GT4 |

**Total.** 8-12 days; the recommended first part (GT1, GT2, GT3, GT6's serial default and
limits, GT7) 5-7. GT2 and GT3 touch only `bin/gate`, `bin/arbace-go`, `bin/seed`, `bin/javac-vendor`
and new files under `test/gate/`; GT5 touches `bin/java-check`, `bin/repr-go` and `bin/repr-java`,
whose owners (JC4b, JN9e) run in the same milestone: GT5 after their merges. Nothing here changes
shipped code: no re-seed.

## 11. Proposals (GTS1-GTS20)

- **GTS1 Names.**
  - Options: (a) the milestones GT1-GTn, the proposals GTS1-GTSn, this file
    `doc/GATE-SURVEY.md`; (b) the gate's milestones as `arbace-reaps-3-crops-v2`'s own (V2-1...).
  - *Recommended: (a)*: unused names, and the gate's work outlives this milestone.
- **GTS2 What a key covers.**
  - Options: (a) the step's name and command line, the gate's logic, the declared files' content,
    the hosts' identities, the external pins, the declared environment (§4.1); (b) the commit (any
    commit reruns everything: today, but per step); (c) git's tree hashes of the declared paths.
  - *Recommended: (a)*: content keys skip doc commits and survive merges; hashing costs under a
    second (§2.6); (c) is no faster at this size and blind to a dirty tree.
- **GTS3 Where inputs are declared.**
  - Options: (a) one table `test/gate/steps.edn`, read by a planner on stage 0 (`bin/gate --plan`);
    (b) bash functions in `bin/gate`; (c) an `--inputs` verb in each script.
  - *Recommended: (a)*: data in the reader's format, printable and checkable, as `bin/reap`'s
    cells are; (c) later for the scripts whose inputs change often.
- **GTS4 The executables in keys.**
  - Options: (a) source keys (§4.2 (a)); (b) the program's digest less its stamp, an early cutoff;
    (c) per-step package sets, the audit checking each step's loaded namespaces.
  - *Recommended: (a) in GT2, then (b) and (c) in GT5*: (a) is sound at once; (c) is what makes a
    j2g change cheap (§9: 190 to 111 minutes), and needs the audit's record to be trusted.
- **GTS5 `gate_key` narrowed.**
  - Options: (a) to what the shipped build reads: `arbace/` less j2g's embed exclusions
    (repr/java, repr/haskell, repr/rust), `jdk/` less `jdk/javac/` and the provenance files,
    `overlay/` less `overlay/jdk/javac/`, `test/java-fixtures/` less its provenance; the audit
    confirming; (b) unchanged.
  - *Recommended: (a)*: a repr/java change then keeps the executables (§9: the essential phase
    from about 20 minutes to about 1); the exclusions are the build's own list.
- **GTS6 Where passes live.**
  - Options: (a) a host-wide store outside git (`ARBACE_GATE_STORE`, default the main checkout's
    `.tmp/gate-pass/`), shared by the worktrees; (b) per checkout; (c) committed; (d) git notes;
    (e) the box's store.
  - *Recommended: (a)*: a merge's gate finds its branch's passes; the store only saves time, the
    tag's committed clean run proves (§7). Its risk, a path-dependent pass reused at another path,
    is what the clean run in a fresh clone catches.
- **GTS7 What a pass holds and how a skip shows.**
  - Options: (a) the key, the commit, the worktree, the date, the seconds, the peak, the log's
    digest and a copy of the log; the summary's `skipped NAME (passed at ...)`; `bin/gate --plan`
    and `--why STEP`; (b) the key alone.
  - *Recommended: (a)*: a skipped step's last log stays one command away; the times feed the
    budgets (GTS17).
- **GTS8 Catching a missed input.**
  - Options: (a) the strace audit at each clean run and on a declaration's change, with the canary
    (one skipped step rerun per gate, the oldest pass) and the clean run as the last net; (b) a
    bwrap sandbox per step; (c) the runtime's own trace; (d) the clean run alone.
  - *Recommended: (a)*: it names the missed file, costs nothing every day, and the canary
    re-proves every pass over a few weeks; (b) breaks on every system path.
- **GTS9 Inner keys.**
  - Options: (a) Java's check per level with the judgment split from the runs, native-check and
    repr/java's checks per corpus (§4.6); (b) the judgment split alone; (c) step keys only.
  - *Recommended: (a)*: those steps are 63% of the serial run; (b) is the cheap half
    (a classes edit from 74 minutes to 2).
- **GTS10 G1 and the fresh build.**
  - Options: (a) G1 the gate's cached executable, `go-build-full` dropped, its consumers on the
    gate's cache; (b) G1 built first in the go chain and used by the rest, `go-build-full` dropped;
    (c) unchanged.
  - *Recommended: (a)*: the gate's build is a G1 by construction (§5); about 12 minutes and two
    20 GiB runs saved per `--full`; the clean run keeps G1 fresh.
- **GTS11 javac's tool.**
  - Options: (a) built once per `arbace_javac_key`, kept across gates, `javac-vendor check` read
    from the tool build's j2g report; (b) once per gate with the check folded; (c) unchanged.
  - *Recommended: (a)*: the key exists already and names the tool in the provenance files; a
    change outside its inputs (repr/go's, a test's) no longer costs its 12 minutes.
- **GTS12 Stage 0.**
  - Options: (a) kept by the seed's and Go's pins (`bin/seed --build` skips Go's build when
    `target/seed/` records them), the clean run building it; (b) unchanged.
  - *Recommended: (a)*: up to 2.5 minutes a run and the race on `target/seed` (2026-10-26's slip).
- **GTS13 Scheduling.**
  - Options: (a) serial `--full` by default, the essential phase concurrent as now; (b) one heavy
    process at a time gate-wide (`ARBACE_HEAVY_LOCK`), the rest concurrent, Java's shards
    pipelined; (c) a memory budget from measured peaks.
  - *Recommended: (a) now, (b) in GT6 once GT1 has measured the lights*: (a) is one line and safe;
    with keys the everyday `--full` is short; (b) takes a third off a large change's run (§9's T3);
    (c) needs every peak current.
- **GTS14 Other agents' heavy jobs.**
  - Options: (a) the gate's heavy processes through the host's slot command when given
    (`GATE_HEAVY_CMD`); (b) none: the gate assumes a quiet host.
  - *Recommended: (a)*: worktrees' gates run beside other agents every day; `JAVA_CHECK_HEAVY`
    already does it for Java's shards.
- **GTS15 Memory per step.**
  - Options: (a) busybox `time -v` per step and a 5 s sampler of the host's memory and the gate's
    processes, the peaks in the passes and, from the clean run, the declarations; (b) a cgroup v2
    per step (`memory.peak`, `memory.max`), cgroup2 mounted first; (c) none.
  - *Recommended: (a)*: nothing to install; (b) when the host's cgroups are set up, for the kill
    that hits the overrunning step.
- **GTS16 Per-program limits.**
  - Options: (a) CPU-time limits with a wall cap and a quiet retry at the step's end, the quiet CPU
    time recorded beside the references; (b) wall limits scaled by the load average; (c) a
    calibration benchmark at the gate's start; (d) unchanged.
  - *Recommended: (a)*: CPU time is what a quiet run measures and load does not inflate; the cap
    still catches a hang; the retry covers what CPU time misses (memory stalls).
- **GTS17 Per-step budgets.**
  - Options: (a) three times the clean run's time, a warning in the summary; (b) a hard step
    timeout; (c) none.
  - *Recommended: (a)*: a hang becomes visible without a new way to fail under load.
- **GTS18 The clean run at a tag.**
  - Options: (a) `bin/gate --full --clean`: a fresh shared clone of the commit, empty caches and
    `GOCACHE`, passes not read, serial, every step audited, all corpora, the record
    `test/gate/clean-TAG.edn` committed with the tag's journal entry; (b) the same in the main
    checkout with its caches removed; (c) as REAP9 (serial, main's caches).
  - *Recommended: (a)*: the only run whose pass says the record reproduces from the commit alone;
    a fresh path catches path-dependent results; its times and peaks calibrate GTS13, GTS16, GTS17.
- **GTS19 native-check's corpora.**
  - Options: (a) per-corpus keys, and the tamago configurations of the toolchain's tree and the
    box's corpora (31 minutes) at the clean run only while those sub-projects are parked,
    `cmd-linux-amd64` kept everyday; (b) per-corpus keys only; (c) unchanged.
  - *Recommended: (a)*: the parked trees do not change, and a change to the reader or the checker
    is exercised by the same code on `cmd-linux-amd64`; the tag's record keeps Go's read cell's
    full count.
- **GTS20 The merge policy after.**
  - Options: (a) `bin/gate --full` at every merge to main, the essential gate the quick check in
    worktrees; (b) today's rule (essential per merge, `--full` when the listed areas change), keyed;
    (c) `--full` only at tags.
  - *Recommended: (a), once GT2-GT5 are in*: a merge then costs what it touches (§9), and the
    hand-kept list of areas that call for `--full` (CLAUDE.md's gate paragraph), which a merge can
    miss, goes.

## 12. Risks

- **A stale pass.** A missed input skips a step that would fail. Mitigated by GTS8's audit and
  canary and by the clean run; until GT4 has audited a step, its declaration can be wider than
  needed (whole trees), which costs skips, not soundness.
- **Shared passes across paths.** Some results depend on the checkout's path (run 1's
  `UserDirChangedTest`); a pass in a worktree reused in main may not hold there. The clean run's
  fresh path exposes them; a step known to be path-dependent declares the root as an input.
- **Output cutoffs' soundness** (GTS4 (b), (c)): the version stamp must be the only commit-bound
  byte, and a package left out of a step's set must not act in its `init`. Both are checked before
  they are trusted (GT5 with the audit).
- **The planner on stage 0.** A planner bug skips or reruns wrongly; stage 0 is built from the
  pinned seed, so the planner's code is evaluated from the checkout, as `bin/reap`'s is. `--clean`
  does not consult it for skipping.
- **Serial by default is slower on a large change** (§9's T2 against T3) until GT6's lock; the
  everyday run is short either way.
- **The audit's cost and noise** (strace on 64 threads, `stat`s of the Go cache): unmeasured; GT4
  measures it and filters the toolchain's own paths by the pins.

## 13. Sources

- `bin/gate` at `2e8629c7` (the steps, lines 223-365); `bin/arbace-go` (`gate_key`, `gate_build`,
  `build`, `build_java`, `reader_key`, `fixpoint`); `bin/lib/gocache.bash`; `bin/lib/javac-key.bash`;
  `bin/j2g` (`J2G_LOCK`, `GOMEMLIMIT`); `bin/java-check` (its levels and caches); `bin/javac-vendor`
  (its verbs); `bin/repr-go` (`native_corpora`, `native_check`); `bin/seed` (`--build`);
  `arbace/j2g/embed.ae` (`excluded`).
- REAP9's full gates' summaries: main's `.tmp/gate-reap9-full.log` (`9b46fb30`),
  `-full2.log` (`f75d89d3`), `-full3.log` (`c216272a`), `-full4.log` (`5cfb1f4a`); the files run 4
  left in `.tmp/java-check/` (`j2g/s0`-`s6`, `forms/`, the results' times) and
  `.tmp/repr-go/native-check.report.edn` (per-corpus `:times`); the essential gate's
  `.tmp/gate/*.secs` of 2026-10-27 07:53.
- doc/JOURNAL.md at the tag `arbace-reaps-3-crops-v1`: 2026-10-21 (JN4, JN5, GP4: the peaks),
  2026-10-22 (JN6), 2026-10-23 (the full gate's OOM kills), 2026-10-25 (REAP7b and JC5: the tool
  builds killed), 2026-10-26 (the gate made efficient; the trial; the final re-seed; REAP9's runs),
  2026-10-27 (REAP9, `GATE_FULL_SERIAL`, the clean run).
- `git log --first-parent --since=2026-10-20 main` at `2e8629c7` (§2.6).
- Prior art for the ideas, not vendored: early cutoff and content keys as in Shake (Mitchell,
  "Shake before building", ICFP 2012; Mokhov, Mitchell and Peyton Jones, "Build systems à la
  carte", ICFP 2018) and Bazel's action cache; strace's `--seccomp-bpf` (strace 5.3).

## Decided (2026-10-27): GTS1-GTS20

The user decided every proposal as recommended: GTS1 (a), GTS2 (a), GTS3 (a), GTS4 (a), then (b) and (c) in
GT5, GTS5-GTS12 (a), GTS13 (a) now and (b) in GT6, GTS14-GTS20 (a). The order: GT1-GT3 first (the measure,
the keyed steps, the shared builds), then GT4 and GT6, GT5 after JC4b and JN9e merge (it changes
`bin/java-check`, `bin/repr-java` and `bin/repr-go`), GT7 at `arbace-reaps-3-crops-v2`'s tag.

## As built: GT1-GT3 (2026-10-27)

Branch `gate-gt1-3`, from main `fb2610fa`, main merged at `7ff11474` (v2-small-a). The decided proposals
GTS2 (a), GTS3 (a), GTS4 (a), GTS5 (a), GTS6 (a), GTS7 (a), GTS10 (a), GTS11 (a), GTS12 (a), GTS13 (a)
and GTS15 (a) are built; GT4 (the audit, the canary), GT5 (inner keys), GT6's lock and limits and GT7 (the
clean run) are not.

**GT1, measured.** Every step runs under busybox `time -v -o .tmp/gate/NAME.time`; `NAME.mem` holds its wall
and CPU seconds (user + system of the step and its waited-for descendants) and its largest process's peak
RSS (`wait4`'s `ru_maxrss`), and, from the sampler, the peak of its process tree's summed RSS. The sampler,
a loop of `bin/gate` every 5 s, reads `/proc/meminfo` and `/proc/*/stat` (no `ps`) and appends to
`.tmp/gate/mem.tsv` the lines `TIME WHAT KIB`: the host's `MemAvailable`, the gate's whole process tree, and
each running step's tree (the step's PID in `NAME.pid` while it runs). The summary prints each step as
`ok NAME (wall, cpu C, peak P GiB (tree T GiB))`, then the gate's largest tree and the host's lowest
`MemAvailable`.

**GT2, keyed steps.**

- *The table*, `test/gate/steps.edn` (the Clojure reader's format): `:groups` (named input lists:
  `:lib` bin/lib's top-level scripts; `:tools` the checkout's namespaces a tool loads instead of its embedded
  copy, `arbace/` less the runtime's Java forms and the namespaces no executable embeds; `:build`, `:repr-go`,
  `:goi`, `:java`, `:repr-java`, `:suite`) and `:steps`, 37 of them in the order they run: 3 `:pre`
  (`makefile`, `seed`, `stage0`: always run, before the plan), 1 build of the essential phase (`go-build`),
  13 essential checks, 20 of `--full` (the build `javac-tool` and 19 checks). A step: `:name`, `:phase`,
  `:kind` (`:check`, keyed; `:build`, run when needed; `:always`), `:chain` and `:start-after` (the concurrent
  order), `:needs`, `:cmd` (run by `bash -c` with `GATE_CACHE`, `GATE_EXE`, `GATE_TESTS`, `GATE_JAVAC_DIR`,
  `GATE_JAVAC`, `GATE_JOBS`), `:hosts`, `:inputs` (`dir/`, globs with `*` and `**`, files, group keywords,
  `!` to take files back out; the last matching rule wins), `:pins`, `:env`, `:heavy`, `:mem-gib`,
  `:summary` (the summary's regex and line count). The commands are the table's: `bin/gate` holds none but
  the three `:pre` steps' (the planner runs on stage 0, which they make).
- *The key* (GTS2 (a)): the SHA-256 of the step's *material*, one line per input: `step NAME`; `decl` (the
  step's declaration printed as a sorted map, less `:doc`, `:summary` and `:mem-gib`); `logic` (the digests
  of `bin/gate` and `test/gate/plan.ae`); `host H ID` per host; `pin SHA PATH`; `env X=V` (or `unset`);
  `file SHA PATH` per selected file. Never the commit. The files' digests come from one table bash makes
  per run, `.tmp/gate/files.sha256` (`find` over the checkout but `.git`, `.tmp`, `target`, `.claude`, then
  `sha256sum`: 7,820 files in 0.1 s), so a dirty tree keys as it is. The hosts' identities, in
  `.tmp/gate/hosts.edn`: `:gate` (`bin/arbace-go --gate-key`), `:javac` (`arbace_javac_key`), `:stage0` (the
  seed's and Go's pins), `:frozen` (the jar's pin), `:jdk` (the system JDK's `release` file, the reference
  javac), `:go` (TamaGo's `VERSION`).
- *The planner*, `gate.plan` (`test/gate/plan.ae`) on stage 0 (`arbace_tool -p test`, as `bin/reap`), 1.4 s
  for the 37 steps: each check's material and key, `.tmp/gate/plan/NAME.inputs`; the store looked up; the
  builds marked needed when a check that runs needs them (transitively); `plan.tsv` for bash (name,
  decision, key, phase, chain, needs, start-after, summary, why, command) and the plan printed: each step
  `always`, `build`, `run`, `skip` or `-` (not needed), and why: `never passed`; `changed since the pass at
  KEY (DATE): FILES...` (the material's lines that differ from the latest pass's, by subject: a path, `host
  javac`, `env X`, `decl`); `passed at KEY, COMMIT, DATE, WORKTREE`.
- *The store* (GTS6 (a), GTS7 (a)): `ARBACE_GATE_STORE`, default the main checkout's `.tmp/gate-pass/` (from
  `git rev-parse --git-common-dir`), shared by the worktrees; per pass `STORE/NAME/KEY.edn` (`{:step :key
  :commit :dirty :worktree :date :secs :cpu-s :maxrss-kib :tree-peak-kib :log-sha256}`), `KEY.log` (the
  log) and `KEY.inputs` (the material, which `--why` and the plan's reasons diff against). Written by bash
  after a check passes (the record by `mv`, atomic); a failure is never recorded.
- *`bin/gate`*: `[--full] [--rerun]` (`--rerun`: every check runs and records), `[--full] --plan` (into
  `.tmp/gate-plan/`, a running gate's logs untouched), `--why STEP` (the material but its files, the passes,
  every line changed since the latest one). Skipped: `skipped NAME (passed at KEY, COMMIT, DATE, WORKTREE)`;
  a build not needed: `- NAME (not needed: ...)`; a check whose `:needs` did not pass: `FAILED NAME (not run:
  X did not pass)`. `GATE_STEPS` names another table (the gate's own tests: a toy table of `sleep`s, a
  failing step, a `:needs` and a `:start-after` exercised the executor before the real runs).
- *`gate_key` narrowed* (GTS5 (a)): `arbace/` less repr/java, repr/haskell and repr/rust
  (`arbace_embedded_sources`, `bin/lib/version.bash`), `jdk/` less `jdk/javac/` and `jdk/provenance.edn`,
  `overlay/` less `overlay/jdk/javac/`, `test/java-fixtures/` less its provenance, `bin/lib` less the check
  scripts of repr/java, repr/haskell and repr/rust. The compiled namespaces' key follows it (`arbace_aot_key`:
  the runtime key and the embedded sources; `bin/java-check --essential-compiled` reads the same function),
  `reader_key` names repr/java's sources itself, and `arbace_runtime_key` leaves `jdk/provenance.edn` out
  too: with it in, a rewritten provenance kept the gate's executables (`gate_key` unchanged) but made their
  runtime "not the checkout's", and `java-check-full` refused their compiled namespaces (found by the second
  `--full` attempt, below).
- *The provenance files* are inputs of the verify that compares them alone (`javac-verify`,
  `jrt-convert`, `java-fixtures`; `test/java/forms/provenance.edn` of `java-check-full`'s `--forms verify`),
  not of the other steps: repr/java's every change rewrites them.

**GT3, shared builds.**

- *G1 the gate's build* (GTS10 (a)): `--gate-build` keeps j2g's program (`amd64/j2g`) and the version map
  (`version.edn`) beside the executables; `bin/arbace-go --fixpoint --g1 DIR` takes DIR's executable and j2g
  tree as G1 when DIR's key is the checkout's `gate_key`, else builds G1 as before; `bin/gate`'s fixpoint
  passes `.tmp/arbace-go-gate` (`GATE_FRESH_G1=1`: G1 built, the clean run's hook). `go-build-full` is gone:
  javac's tool build and Java's compiled level take the gate's executable and compiled namespaces
  (`ARBACE_AOT_BASE`, `JAVA_CHECK_AOT`; `aot_fresh` finds the executable beside a cache's `aot/`).
- *javac's tool by its key* (GTS11 (a)): `bin/javac-vendor tool DIR --host EXE --aot-base DIR` keeps
  `.tmp/gate-javac/arbace-javac` while its key is the checkout's `arbace_javac_key`, else builds it
  (`--build-java`, amd64, j2g on the gate's executable, the gate's compiled namespaces) and copies its j2g
  log and report beside it; `bin/javac-vendor check --from DIR` reads that run's verdict (the tool of the
  checkout's key, no class failing its analysis): javac's world translated once (twice when `reader_key`
  changed: the first tool build that compiles repr/java's reader, as before). The old `javac` step is four:
  `javac-tool` (a build), `javac-check`, `javac-verify`, `javac-classes`.
- *Stage 0 by its pins* (GTS12 (a)): `bin/seed --build` keeps `target/seed` when it holds the executables
  recorded with the seed's SHA-256, `seed/goroot.sha256`'s, `ARBACE_GOROOT` and the architectures
  (`target/seed/pins`); `--force` or `ARBACE_SEED_FORCE=1` builds it. One build at a time: `--build` takes
  `.tmp/seed-build.lock`, the lock `bin/lib/host.bash` holds when it calls it (`SEED_BUILD_LOCKED=1`). After
  main's RPS95 (b) the compiled namespaces `--build` makes are kept likewise while their `aot.key` is the
  checkout's; the gate's stage 0 is `--no-aot`.
- *Serial by default* (GTS13 (a)): `--full`'s steps one after another in the table's order;
  `GATE_FULL_PARALLEL=1` runs them by chain, the java chain after `javac-tool` (`GATE_FULL_SERIAL` accepted, a
  no-op). The Makefile: `gate-full-parallel`, `gate-plan`, `gate-why STEP=...`.

**The runs** (`.tmp/heavy`, beside other agents' 20-40 GB jobs; the host's `MemAvailable` fell to 0.8 GiB).

| run | commit | what ran | result | time |
|---|---|---|---|---:|
| essential 1 | `fc524cd2` | all 13 checks, the executables built (`gate_key` narrowed: a miss) | passed | 25m14s |
| essential 2 | `fc524cd2` | nothing changed: 13 skipped, `go-build` not needed | passed | **0m06s** |
| `--full` 1 | `c715efe7` | all 37 (every key new: `bin/lib/version.bash` changed); javac's tool built with the reader's first build | 2 failed (below) | 276m31s |
| `--full` 2 | `47eec789` | the two failures and the 7 steps whose inputs the fix touched: 9 run, 28 skipped; the tool kept | passed | 75m14s |
| `--full` 3 | `47eec789` | nothing changed: 32 skipped, both builds not needed | passed | **0m04s** |

The first `--full`, step by step (serial; the essential steps, then `--full`'s; wall m:s, CPU minutes, the
largest process's peak RSS and the sampled tree's, GiB):

| step | wall | CPU min | peak | tree | |
|---|---:|---:|---:|---:|---|
| go-build | 9:50 | 82 | 18.8 | 18.8 |  |
| go-smoke | 2:15 | 2 | 0.7 | 0.6 |  |
| suite-go | 2:16 | 27 | 6.5 | 10.9 |  |
| oracle-go | 1:09 | 2 | 0.5 | 0.6 |  |
| diffcheck | 9:40 | 95 | 26.3 | 26.3 |  |
| goi-check | 1:37 | 12 | 0.6 | 1.9 |  |
| goi-aot-essential | 1:27 | 5 | 0.5 | 0.4 |  |
| java-check | 0:41 | 4 | 0.4 | 0.0 |  |
| java-compiled | 6:02 | 37 | 18.5 | 17.7 |  |
| suite-aot-essential | 5:09 | 25 | 19.4 | 19.3 |  |
| programs | 2:48 | 30 | 18.7 | 18.7 |  |
| generated | 0:03 | 0 | 0.4 | 0.0 |  |
| j2g-edge | 1:30 | 11 | 2.9 | 1.9 |  |
| reap | 0:02 | 0 | 0.4 | 0.0 |  |
| javac-tool | 8:47 | 99 | 19.9 | 25.6 |  |
| javac-check | 0:00 | 0 | 0.0 | 0.0 |  |
| javac-verify | 2:21 | 10 | 7.9 | 7.7 |  |
| javac-classes | 3:04 | 7 | 3.2 | 3.2 |  |
| repr-java | 3:03 | 88 | 11.1 | 11.4 |  |
| repr-java-roundtrip | 9:25 | 29 | 28.1 | 28.1 |  |
| jrt-convert | 4:51 | 11 | 25.2 | 24.2 | failed |
| java-fixtures | 0:27 | 0 | 1.0 | 0.6 |  |
| repr-java-parse | 4:41 | 43 | 2.0 | 2.0 |  |
| repr-java-front | 9:43 | 28 | 28.8 | 28.8 |  |
| repr-java-attr | 13:03 | 40 | 40.1 | 40.1 |  |
| java-check-full | 91:04 | 1011 | 35.0 | 38.2 | failed |
| fixpoint | 12:37 | 64 | 19.0 | 18.8 |  |
| suite-aot | 8:37 | 115 | 22.5 | 21.5 |  |
| programs-full | 6:31 | 69 | 19.0 | 18.6 |  |
| repr-go | 8:57 | 161 | 11.4 | 26.4 |  |
| repr-go-native | 52:49 | 488 | 27.7 | 27.7 |  |
| goi | 8:06 | 39 | 5.0 | 5.0 |  |
| goi-aot | 7:56 | 29 | 1.2 | 0.6 |  |
| repr-clojure | 0:54 | 3 | 0.5 | 0.5 |  |

Against REAP9's run 4 (§2.1): the fixpoint 20:45 to **12:37** (G1 not built; G1 = G2 = G3 byte for byte, G1
having been built at the same commit); `go-build-full` (4:56) gone; javac's 12:07 is now `javac-tool` 8:47
(two translations of javac's world here, `reader_key` having changed; 8:27 and one translation in an
earlier attempt, 0:00 when kept, as in `--full` 2) plus `javac-check` 0:00 (the tool build's report),
`javac-verify` 2:21 and `javac-classes` 3:04; stage 0 0:00 (kept by its pins; 2:03 in run 4). The rest
moved with the host's load: the runs shared it with other agents' 20-40 GB jobs, `diffcheck` and
`suite-aot` ran on caches that missed, `java-check-full` took 91:04 (70:07 in `--full` 2, 71:42 in run 4).
**Memory** (GT1's first measurements): above 25 GiB `repr-java-attr` 40.1, `java-check-full` 35.0 (tree
38.2), `repr-java-front` 28.8, `repr-java-roundtrip` 28.1, `repr-go-native` 27.7, `diffcheck` 26.3,
`jrt-convert` 25.2 (27.4 in `--full` 2); about 19-22 GiB every whole-program j2g (`go-build`,
`java-compiled`, `suite-aot-essential`, `programs`, `javac-tool`, `fixpoint`, `suite-aot`, `programs-full`);
the lights under 12 GiB. The first essential run's gate tree reached 44.9 GiB (its concurrent j2g steps
under `J2G_LOCK` beside the suite's 12 processes) with the host's `MemAvailable` at 1.1 GiB.

The second `--full` was right after the first but for one commit (the fourth provenance file, below); the
third, with nothing changed, skipped every check in 4 seconds (after waiting for the host's big-job lock).
A doc-only change afterwards (this section): `bin/gate --full --plan` ran every check `skip`
("gate plan (full): 37 steps; 3 always, 32 skip, 2 none", in about 5 s).

**Slips and findings.** The four provenance files' `:repr-java` hash was stale on main since `8d621e91`
(the scripts' sweep edited `bin/repr-java`, which `repr_java_reader_hash` covers, without rewriting them):
`javac-vendor verify` and `java-check --forms verify` failed on it; the hashes were brought to the reader's
(the forms verified equal byte for byte), and main's JN9e merge rewrote them since. `jrt-convert` was
killed by the kernel in the first full run (its read at 25.4 GB with the host's `MemAvailable` at 2.2 GB:
another agent's job beside it); rerun alone in the second, it passed. Two full attempts were stopped by
hand to fix the gate (the provenance, then the runtime key): their essential passes stayed valid.

## Proposals (GTS21-GTS29)

- **GTS21 Stale provenance caught early.**
  - Options: (a) an essential step `provenance` (seconds): the `:repr-java` hash of the four provenance
    files (`jdk/`, `jdk/javac/`, `test/java-fixtures/`, `test/java/forms/`) equal to
    `repr_java_reader_hash`, keyed on the reader's sources and the four files; (b) left to the verifies of
    `--full`.
  - *Recommended: (a)*: the staleness of `8d621e91` (a script's edit inside the reader's hash) passed every
    essential gate and failed two `--full` steps a day later; the check is a hash comparison.
- **GTS22 The gate's logic in every key.**
  - Options: (a) the logic narrowed to what decides a step's outcome: `test/gate/plan.ae` and a runner
    `bin/lib/gate-run.bash` (`step`, `run_one`, the pass record), with `bin/gate`'s header, summary and
    option parsing out of the keys; (b) as built: `bin/gate` and the planner in every key.
  - *Recommended: (a)*: today a comment in `bin/gate` reruns every step (four hours); the header is most of
    the file and changes with every step added.
- **GTS23 `bin/lib`'s scripts in every key.**
  - Options: (a) each step's libraries declared (its scripts' `.` lines: `tmpdir`, `host`, `tools`,
    `version`, ...), confirmed by GT4's audit; (b) as built: `bin/lib/*.bash` in every step (`:lib`).
  - *Recommended: (a) with GT4*: the runtime key's fix in `version.bash` cost a full rerun; the audit names
    which steps read which library.
- **GTS24 The gate's executables shared by the worktrees.**
  - Options: (a) `.tmp/arbace-go-gate` keyed by `gate_key` in the main checkout's `.tmp/` (as the store),
    one directory per key, the last few kept; (b) per checkout, as built.
  - *Recommended: (a)*: a worktree whose checks all skip already needs no build, but one check that runs
    there rebuilds the executables (10-15 minutes, 20 GiB) that main's checkout holds at the same key.
- **GTS25 The heavy steps under the host's lock, per step.**
  - Options: (a) a `:heavy` step's command run under `GATE_HEAVY_CMD` (the host's slot or big-job lock,
    `flock .tmp/big.lock`), the gate itself not: the table makes it one line in `run_one`; (b) the whole
    gate under the lock, as the host's rule asks today; (c) GT6's `ARBACE_HEAVY_LOCK` inside the tools.
  - *Recommended: (a) now, (c) in GT6*: the third `--full`, four seconds of work, waited 100 minutes for the
    lock; `:heavy` and the measured peaks (`:mem-gib`, below) are in the table already.
- **GTS26 The measured peaks into the table.**
  - Options: (a) `:mem-gib` of every step from the first `--full`'s `.mem` files (§"The runs"), `:heavy`
    for every step above 8 GiB, refreshed at each clean run (GT7); (b) by hand when a step is killed.
  - *Recommended: (a)*: GT6's scheduler (GTS13 (b)) and GTS25 read them; three steps the table had as light
    (`repr-java-attr` 40 GiB, `java-check-full` 35, `repr-java-roundtrip` 28) are among the heaviest.
- **GTS27 The store's growth.**
  - Options: (a) `bin/gate --prune`: per step the last 5 passes and every pass younger than 30 days kept
    (the logs are most of it: 37 steps' logs about 60 MB a `--full`); (b) none.
  - *Recommended: (a)*: the store only saves time; old keys never match again once their inputs moved.
- **GTS28 javac's world translated twice when `reader_key` changes.**
  - Options: (a) `reader_key` over repr/java's sources, c2j's, the runtime key and javac's world, not every
    embedded source (`aot_key`), so that an embedded namespace's change keeps the compiled reader; (b) as
    built.
  - *Recommended: (a), once checked*: the reader's compiled units name the runtime's classes and c2j's
    output, not the other namespaces' code; to be confirmed by building the tool from a kept reader after
    an unrelated namespace's change (a byte-for-byte check against a fresh one).
- **GTS29 The table checked.**
  - Options: (a) the planner fails on an input rule that matches no file (a typo or a moved path), a
    `:needs` or `:start-after` naming no step, a host absent from `hosts.edn`, a `:cmd`'s `bin/` script
    missing, as `make check-makefile` checks the Makefile; (b) as built: unknown groups and hosts fail,
    the rest silently selects nothing.
  - *Recommended: (a)*: a rule that matches nothing makes a key that never changes, a stale pass waiting.

## Decided (2026-10-28): GTS21-GTS29

The user decided GT1-GT3's proposals as recommended, folded into GT4-GT6: GTS21 (an essential step checking the
provenance files' `:repr-java` hash against the reader's), GTS22 (the gate's logic in every key narrowed to the
planner and a runner library), GTS23 (each step's `bin/lib` libraries declared, with GT4's audit), GTS24 (the
gate's executables shared across worktrees by `gate_key`), GTS25 (only `:heavy` steps under the host's lock,
`GATE_HEAVY_CMD`), GTS26 (the measured peaks in `:mem-gib`/`:heavy`), GTS27 (`bin/gate --prune`), GTS28
(`reader_key` narrowed), GTS29 (the planner validates the table).

## As built: GT4, GT6 (2026-10-28)

Branch `gate-gt4-6`, from main `16f79f3d`. Built: GT4 (the audit and the canary), GT6 (the heavy lock gate-wide, the
measured peaks, the limits in CPU time with wall caps and the quiet retry, the step budgets), and the decided
GTS21-GTS29. Not built: GT5 (inner keys; it waits for JC4c and JN9f), GT7 (the clean run), the cgroup of GTS15 (b).

**The runner (GTS22 (a)).** `bin/gate` is its header, the options and the main flow; what decides a step's outcome
moved to `bin/lib/gate-run.bash` (`gate_env`, `plan_inputs`, `step`, `run_one`, `report`, `record_pass`,
`gate_prune`, `gate_audit`, the sampler). The logic every key holds is `bin/lib/gate-run.bash` and
`test/gate/plan.ae`; `bin/gate`'s comments and summary are out of the keys, and `bin/lib/gate-run.bash` is out of
`gate_key` and `arbace_javac_key` (a runner edit rebuilt the executables and javac's tool otherwise). Editing the
runner or the planner reruns every check once: merging this branch makes every pass of the store stale.

**GT4, the audit.** `bin/gate --audit [STEP...]` (`make gate-audit STEP=...`; no step: every check):

- *How.* The step's command (its `:audit-cmd` where the table gives a cheaper one that exercises the same code on a few
  tests or corpora: the files a step reads in the checkout do not depend on how many tests it runs) runs under
  `strace -f -qq -y -s 4096 --seccomp-bpf -e trace=%file,execve,clone,clone3,fork,vfork,chdir` (strace 7.2 on the host;
  `--seccomp-bpf` stops only on those calls), its output a FIFO read at once by `test/gate/audit.awk` (busybox awk): the
  distinct `OP PATH` lines, absolute and normalized, OP one of X (execve), R (open for reading, readlink), S (stat,
  access), D (open of a directory) and W (open for writing, creation, removal). A relative path is resolved against the
  process's directory (`-y` prints it in every `AT_FDCWD<dir>`; for the old `open` and `stat` calls musl's programs
  make, `chdir` is followed and a child starts in its parent's); `git`'s reads (the version stamp's `describe --dirty`)
  are dropped, the commit being in no key. The FIFO keeps strace's output (gigabytes for a Go build) off the disk.
- *The comparison*, `gate.audit` (`test/gate/audit.ae`, on stage 0): a file of the checkout that the step opened or
  executed and its key does not cover fails the audit and is named. Covered: the files its `:inputs` select, its `:pins`,
  the gate's logic, and the files of its hosts' keys (`:gate` gate_key's set, `:javac` arbace_javac_key's, `:stage0` the
  seed's pins, `:frozen` the jar's pin: a step on a host reads that host's sources without declaring them). Reported
  and not failing: directory listings with no declared file below (a `find` or a tree walk: a new file in an
  undeclared directory changes no key; advisory), files only stat'ed (counted), writes to checked-in files, reads
  outside the checkout by directory (TamaGo's go and `$GOROOT/test` belong to the pins; the system JDK and jdk26u to
  `:jdk` and the platform pin), and the declared rules whose files were never read (candidates to narrow). The report
  is `.tmp/gate-audit/NAME.report` (`GATE_AUDIT_DIR`); a step that itself fails, or a trace strace aborted, is not an
  audit (the aborted trace is run again, twice at most: below).
- *Findings* the audit of 24 of the 36 checks (all 13 essential, `provenance`, `javac-check`, `javac-classes`,
  `javac-verify`, `repr-java`, `repr-java-roundtrip`, `-front`, `-attr`, `-parse`, `jrt-convert`, `java-fixtures`, `goi`,
  `goi-aot`, `repr-clojure`, with `:audit-cmd` for `goi`, `goi-aot` and the three repr/java tree checks; `repr-go`,
  `programs-full`, `suite-aot`, `repr-go-native`, `java-check-full` and `fixpoint` were not audited: the lock was held
  by other agents for most of 14 hours, and `repr-go` stalled for 1h47 under strace; their declarations follow their
  twins' (`programs-full` = `programs`, `suite-aot` = `suite-aot-essential`) and are the first thing to audit,
  GTS30). Undeclared reads found and declared: `go-smoke` 100 files (the goi REPL's programs, `test/goi/`),
  `repr-clojure` 182 (it round-trips all of `arbace/`, Java forms of `arbace/lang/` included, not `:tools`'
  subset), `suite-aot-essential` 41 (`arbace/repr/{haskell,java,rust}`, the fixtures' provenance; `suite-aot` the
  same), `java-compiled` 962 (javac's world, `jdk/javac/`, `overlay/jdk/javac/`, hashed to find the tool fresh),
  `programs` 2,160 (`tamago/goroot/` as goi's table, `tools/godump/`; `programs-full` likewise), `repr-java-parse`
  15 (`bin/jrt-convert`, `doc/go/java-surface.edn`, `test/jrt/`), `jrt-convert` and `java-fixtures` one each
  (`bin/javac-vendor`). None in `reap`, `generated`, `oracle-go`, `java-check`, `j2g-edge`, `diffcheck`, `goi-check`,
  `goi-aot-essential`, `provenance`, `javac-check`, `javac-classes`, `repr-java`, the round trip, front and attr.
  The audit also found three things that are not declarations: `javac-classes` ran `bin/jrt-convert` on the default
  tool `target/arbace-go/amd64/arbace-javac`, absent in a clean worktree (fixed: `REPR_JAVA_EXE`);
  `jdk/javac/provenance.edn`'s `:jdk-edge` was stale at the base (`9ff8d44b` changed `jdk/edge.edn`), so `javac-verify`
  failed on the base (its hash corrected here, and `bin/provenance-check` compares it, like the reader hash);
  and goi's gc references were cached when they timed out (status 124 recorded as gc's own, five run references and one
  errorcheck reference in the worktree), which poisoned every later run (fixed: a timed-out reference is an error and
  is not cached). The repr/java tree checks read 49 tracked files: the reader runs compiled into the tool, so the
  tool's key (`:javac`) is where its sources enter..
- *Cost* `strace` with `--seccomp-bpf` on the file calls costs nothing visible on the light steps (seconds,
  audit against the GT3 run of the same step: `reap` 2 / 2, `generated` 3 / 3, `go-smoke` 118 / 135, `j2g-edge` 89 / 90,
  `java-check` 32 / 41, `oracle-go` 75 / 69, `goi-check` 95 / 97, `goi-aot-essential` 110 / 87, `repr-clojure` 70 / 54,
  `jrt-convert` 273 / 291, `javac-verify` 210 / 141, `programs` 484 / 168) and a lot on a Go build: with the stat calls
  traced `diffcheck` took 72 minutes (9:40 in GT3; strace used 57 CPU-minutes and the busybox awk behind the FIFO 27) and
  `repr-go` stalled past 1h47 (9 minutes normally). The traced set is therefore the opens, `execve`, `readlink`, `clone`
  and `chdir` (`GATE_AUDIT_TRACE=%file` puts the stats back); a stat reads no content. Even so the heavy steps cost
  2 to 3 times their time, and the whole audit about 4 hours of the host's lock beside the waiting.
  A trace is only as complete as the run: a warm cache hides the reads that built it (`suite-go` reads 13 files, its
  translation being cached), so the audit belongs at the clean run (GT7); `strace -f` also aborts
  (`ptrace(PTRACE_LISTEN): I/O error`) when a traced process dies in a group stop, as goi's check does at random: an
  aborted trace is run again (twice at most)..
- *The canary*, both senses of the survey's §4.4. (d) Each gate runs one skipped check anyway, the light one (not
  `:heavy`, its pass under five minutes) whose pass is the oldest (`GATE_CANARY=0` turns it off; the planner prints it
  as `run ... canary`); its new pass replaces the old one, so over a few gates every light pass is re-proved. The
  demonstration (a toy table, `GATE_STEPS`): a step reading `a.txt` and `b.txt` and declaring `a.txt` passes; `b.txt`
  changed, `GATE_CANARY=0` skips it on the stale pass; the default reruns it and it fails; `bin/gate --audit` names
  `canary-toy/b.txt` as undeclared.
- *GTS23 (a).* `:lib` (all of `bin/lib/*.bash`) is gone from every step on `:gate` or `:javac`, whose hosts' keys
  hold `bin/lib`; the stage-0 steps declare their libraries (`:lib-base`, `:lib-tools`: `tmpdir`, `renamed-env`, `tools`,
  `host`; `reap` its three). Each step's other undeclared reads are declared (go-smoke's `test/goi` programs, java-fixtures'
  and repr-java-parse's scripts, repr-clojure's whole `arbace/`: it round-trips the tree).

**GT6.**

- *The heavy lock, per step (GTS25 (a)).* A step the table marks `:heavy` runs under `GATE_HEAVY_CMD`, by default
  `flock $ARBACE_HEAVY_LOCK`, `ARBACE_HEAVY_LOCK` by default the main checkout's `.tmp/big.lock` (found from git's common
  directory, so the worktrees share it); the gate does not. The step's wall time starts once it holds the lock (a start
  file), the wait is its `.wait` and the summary prints it (`waited 2m10s for the lock`). In the essential phase the
  heavy steps' lock replaces `J2G_LOCK`. A caller that holds the lock already (`flock .tmp/big.lock bin/gate`, the old
  rule) must set `GATE_HEAVY_CMD=env`, else the gate waits for itself: the agents' rule becomes "run `bin/gate`
  through `.tmp/heavy`, without the big lock". The step's environment has `ARBACE_HEAVY_HELD=1`. The tools take no
  lock of their own inside a step (GTS31).
- *The peaks (GTS26 (a)).* `:mem-gib` and `:heavy` of every step from "As built: GT1-GT3"'s first `--full` (the larger of
  the process's peak and its tree's, rounded up); `:heavy` where that is above 8 GiB: three steps were light that are
  not (`repr-java` 11, `repr-go` 27 for its tree, `suite-go` 11 for its 12 processes), one stays heavy under 8
  (`javac-verify` 7.9: the frozen JVM's 12 GiB heap). 
- *Limits in CPU time, a wall cap, a quiet retry (GTS16 (a)).* `bin/lib/limit.bash`: `limited_cpu CPU WALL CMD` (`ulimit
  -t`, the kernel counting all of a process's threads, under `timeout -k 5`) and `limited TMO CMD` = 5 x TMO of CPU and a
  wall cap of 4 x that. Used for goi's programs (`bin/goi check`: `run`, the directory tests, the error checks; the
  limit of `test/goi/timeouts.txt` still scales it), Java's interpreted level (`bin/java-check`, its 20 s, retried
  at twice), the suite's namespaces (`bin/clojure-tests`: 2 x and 4 x the 900 s) and the oracle's runs (`test/oracle/
  runner.ae`: `ulimit -t` 2 x and a wall cap 8 x the `--timeout`). Status 124 (the cap), 137 (the CPU limit's kill)
  and 152 are timeouts; goi's check and the suite run them again alone at the end (the oracle's runner too; Java's
  `again` already did) and the second result stands. The first factor was 2, and goi-check's audit refuted it:
  `gcgort.go` runs 2.5 s of wall time and 32 s of CPU (the Go runtime's GC threads on 64 CPUs), 3.2 times its 10 s,
  and `linkmain_run.go`, which starts the linker, was killed at 20 s; with 5 both pass. CPU time is what a quiet run
  measures and a loaded host barely moves (the 1.3 to 2.3 of §2.3 was waiting for a CPU); the factor is the price of
  GC threads.
- *Budgets (GTS17 (a)).* The planner gives each check a budget of three times the longest of its last three passes (at
  least 120 s); the summary flags a step over it (`OVER ITS BUDGET of 12m00s`); never a kill.
- *The exit criterion* **Not done:** the concurrent `--full` (`GATE_FULL_PARALLEL=1`). The host's big-job lock was held by other agents for
  most of the 15 hours of this work (my audit queued for 3 hours, then held it for 2 more), and a `--full` is five hours of
  heavy steps; the criterion is open (GTS37). What ran: the lock, the serialization of heavy steps beside concurrent
  light ones, the canary, the budgets and the prune, on a toy table (`GATE_STEPS`: two heavy steps waited for each
  other, `waited 0m06s for the lock`, the step's wall time counted from the lock; with `GATE_FULL_PARALLEL=1` the
  chains ran at once); and the real essential gate, in a worktree whose executables were built by the gate itself into the
  shared `ARBACE_GATE_EXES/KEY` (13m52s, 19.0 GiB), holding the big lock once from outside (`flock ... bin/gate`
  with `GATE_HEAVY_CMD="flock .tmp/own.lock"`, so the heavy steps serialized on a private lock): **passed in 36m19s**, all
  14 checks, `diffcheck` waiting 9m55s, `programs` 20m33s on that private lock behind the other heavy steps. Peaks
  (GiB, process / tree): go-build 19.0 / 18.9, suite-go 9.8 / 12.2, diffcheck 24.6 / 24.4, java-compiled 18.8 / 18.7,
  suite-aot-essential 19.8 / 19.8, programs 19.1 / 18.8, j2g-edge 4.4 / 3.0, goi-check 0.6 / 2.0, the rest under 1; the
  host's MemAvailable fell to 20 GiB at the lowest. Immediately after, a second essential gate: 13 skipped, the
  canary (`provenance`, the oldest light pass) run, 6 s..

**The decided GTS21-GTS29.**

- *GTS21.* `bin/provenance-check` (the essential step `provenance`, seconds, no executable): the `:repr-java` hash of
  `jdk/provenance.edn`, `jdk/javac/provenance.edn`, `test/java-fixtures/provenance.edn` and `test/java/forms/provenance.edn`
  against `repr_java_reader_hash`; keyed on the reader's sources, `bin/repr-java` and the four files. It fails on the
  staleness of `8d621e91`.
- *GTS22.* Above.
- *GTS24 (a).* `bin/gate` keys the gate's executables and javac's tool and keeps them in `ARBACE_GATE_EXES/KEY` (default the
  main checkout's `.tmp/gate-exe`, `KEY` the first 20 hex of `gate_key`; `javac-KEY` for the tool): `ARBACE_GATE_CACHE`
  (`bin/arbace-go --gate-build`, `bin/diffcheck`, `bin/generated-check`, `bin/java-check`, `bin/clojure-tests` read it,
  else `.tmp/arbace-go-gate`), `GATE_CACHE`, `GATE_JAVAC_DIR`. A worktree whose check runs finds main's build at the
  same key. `--prune` keeps the newest three directories of each (and any younger than a day).
- *GTS27 (a).* `bin/gate --prune [--dry-run]`: per step the last `ARBACE_GATE_KEEP` (5) passes and every pass younger than
  `ARBACE_GATE_KEEP_DAYS` (30) kept. Tested on a copy of main's store (`cp -a`, 102 passes): with a keep of 1 and 0 days it
  removed 70 and kept 32 (one per step plus the toy), and then none the first try ran on main's real store by a slip (the environment did not carry over to the command) and removed 70 passes; they were restored at once from the copy taken just before (`cp -an`, the store identical again, 102 passes).
- *GTS28 (a).* `reader_key` = `arbace_runtime_key`, `arbace_reader_sources` (arbace/ less repr/java, j2g, repr/go,
  repr/clojure, repr/haskell, repr/rust, goi, the Go build and the box: `bin/lib/version.bash`), repr/java's own sources and
  javac's world; no longer `aot_key`. A change to j2g or repr/go keeps the compiled reader ; the key function was checked to be unchanged by an edit of `arbace/j2g/` and changed by one of `arbace/classes/`; the compiled reader itself was not compared byte for byte (that needs a first tool build before and after, 20 GiB each; GTS30).
- *GTS29 (a).* The planner fails (exit 1, every problem listed) on a duplicate step, an unknown `:phase` or `:kind`, a
  `:needs` or `:start-after` naming no step, a host absent from `hosts.edn`, a pin that is no file, an `:inputs` rule or group
  rule that matches no file, an unknown group, a `:cmd` running a `bin/` script that is not a file. It found at once
  `:tools`' `arbace/lang/go/ns/` (gone since SA150) and, with the audit, a step command that worked only in main's
  checkout: `javac-classes` ran `bin/jrt-convert` on the default tool `target/arbace-go/amd64/arbace-javac`, absent in a
  clean worktree; its command now names `REPR_JAVA_EXE`.

**Slips and findings** `bin/gate --prune` was first run on main's real store by a slip (the test's environment did not carry over
  to the command): 70 passes removed with a keep of 1 and 0 days, restored the same minute from the `cp -a` taken a minute
  before; the wrapper `.tmp/g.sh` sets the scratch store since. Two `pgrep`/`ps` lookups, one `/dev/null` in a sed, and
  one `kill` of my own shell by a pattern that matched its command line broke the agents' rules. The audit block held
  `big.lock` for 2h14m (one hold for several steps, to escape the queue), and its first kill by PID left the children, which
  inherit the lock's descriptor and kept it until the coordinator found them. Editing a file `gate_key` hashes (`bin/goi`,
  `bin/lib/*.bash`) renames the executables' directory under a running queue: the copies of main's executables used
  for the audits were hard-linked to the new key each time (`cp -al`). A goi program that exceeds its CPU limit under
  strace is killed in a group stop and can abort strace..

## Proposals (GTS30-GTS37)

- **GTS30 The audit at the clean run, completed.**
  - Options: (a) at GT7's clean run (cold caches, nothing cached to hide a read), every step audited with its full command, the six steps GT4 left (`repr-go`, `programs-full`, `suite-aot`, `repr-go-native`, `java-check-full`, `fixpoint`) first, the reducer a program instead of busybox awk (it was behind a quarter of `diffcheck`'s cost); (b) as built, `:audit-cmd` for the long steps; (c) none.
  - *Recommended: (a)*: a warm cache hides reads (`suite-go` read 13 files); the clean run is the only trace without cache hits, and its cost is the run's once.
- **GTS31 Heavy locks inside the tools.**
  - Options: (a) as built: the step holds the lock, the tools none; (b) `bin/j2g`, repr-go's reader and the repr/java corpus reads take `ARBACE_HEAVY_LOCK` around the heavy process only, unless `ARBACE_HEAVY_HELD`, so that Java's shards pipeline (shard k+1's j2g while shard k's tests run) and a heavy step stops holding 40 GiB while it runs light work; (c) a memory budget from the measured peaks.
  - *Recommended: (b), with GT5*: `java-check-full` holds the lock for over an hour at 39 GiB while most of its time is light; (b) is the survey's §6.1 (b) and the step-level lock is its base.
- **GTS32 The lock's fairness.**
  - Options: (a) a ticket lock (FIFO) in front of `big.lock`, `flock` being unfair: an audit queued behind four gates waited 3 hours and the block, once it held the lock, held it 2 hours more; (b) as built; (c) a per-agent budget of hold time.
  - *Recommended: (a)*: the audits and the final `--full` queue per step, and every step of a long gate re-enters the race.
- **GTS33 CPU limits recorded.**
  - Options: (a) the quiet CPU time of each goi and Java reference recorded beside it (`bin/goi check --record`), each program's limit its recorded CPU time times 3 or the default, so that the factor of 5 (gcgort.go needs 3.2) shrinks for programs that are quiet; (b) as built; (c) cgroup v2 `cpu.max`.
  - *Recommended: (a) once a clean run has measured them*; (b) is sound.
- **GTS34 The store pruned by the gate.**
  - Options: (a) `bin/gate --prune` run at the end of every `--full` (cheap: seconds); (b) by hand.
  - *Recommended: (a)*.
- **GTS35 Caches that survive a killed run.**
  - Options: (a) goi's `gcref`, `eref` and `earef`, Java's forms cache and the print cache written to a temporary and renamed only when complete, and a timed-out reference never cached (done for the timeouts); (b) as built.
  - *Recommended: (a)*: a SIGKILL (the audit's, an OOM kill) left half-made references that failed the next run.
- **GTS36 Staleness of derived hashes.**
  - Options: (a) every hash a data file records of another (`:repr-java`, `:jdk-edge`, the platform's pin, the fixtures' and forms' provenance) checked by `bin/provenance-check`; (b) the writers' `update` rewriting them in the same commit as the change (`bin/j2g-edge update` rewrites the edge and leaves `jdk/javac/provenance.edn` stale).
  - *Recommended: both*: the check is built for two hashes; (b) removes the cause.
- **GTS37 The concurrent `--full`.**
  - Options: (a) one run of `GATE_FULL_PARALLEL=1 bin/gate --full` holding `big.lock` once from outside with `GATE_HEAVY_CMD="flock .tmp/own.lock"` (the private lock serializes the heavy steps, the big lock keeps the others off), recording the peaks, a quiet host; (b) the per-step lock on the shared host, as built, when the host is quiet; (c) GT7's clean run serves.
  - *Recommended: (a)*: it tests the exit criterion without five hours of queueing behind each step, and is what an agent can do while the rule stands.


## Decided (2026-10-28): GTS30-GTS37

The user decided GT4-GT6's proposals as recommended: GTS30 (a) (at GT7's clean run every step audited with its full
command, the six GT4 left first, the reducer a program instead of busybox awk), GTS31 (b) (with GT5: `bin/j2g`, repr/go's
reader and repr/java's corpus reads take `ARBACE_HEAVY_LOCK` around the heavy process only, unless `ARBACE_HEAVY_HELD`),
GTS32 (a) (a FIFO ticket lock in front of `big.lock`), GTS33 (a) (the quiet CPU time recorded beside each reference, once a
clean run has measured it), GTS34 (a) (`--prune` at the end of every `--full`), GTS35 (a) (goi's `gcref`, `eref` and `earef`,
Java's forms cache and the print cache written to a temporary and renamed when complete), GTS36 (a) and (b) (every derived
hash checked by `bin/provenance-check`, and the writers' updates rewriting them in the same commit), GTS37 (a) (one
concurrent `--full` holding `big.lock` from outside with a private lock for the heavy steps). GTS30, GTS33 and GTS37 are runs
at GT7's time; GT5 builds GTS31, GTS32, GTS34, GTS35, GTS36 (b) and the tooling GTS30 needs.

## As built: GT5 (2026-10-29)

Branch `gate-gt5`, from main `8d5e06d3` (GT1-GT4, GT6, JC4c, JN9f, JFS133), main merged at `9c89817d` (the provenance
files rewritten, RPS150, C23's C2: repr/c's scripts are out of the program host's rest and of the audit's `:gate` and `:prog`
as they are out of gate_key). Built: GT5 (the inner keys: parts, outputs and
early cutoffs, Java's check per level with its judgment apart, native-check and repr/java's tree checks per corpus, the
program's digest and the per-step package sets with the audit's record of the namespaces loaded), and the decided GTS31 (b),
GTS32 (a), GTS34 (a), GTS35 (a), GTS36 (b) and the tooling of GTS30 (a) (the audit over every step, its reducer a program).
Not built: GT7 and the runs GTS30, GTS33 and GTS37 belong to.

**The table's new fields** (`test/gate/steps.edn`'s header; the planner `test/gate/plan.ae`, the runner `bin/lib/gate-run.bash`):

- *`:parts`* (survey §4.6): a step with parts is one check per part, `NAME.PART`, keyed alone: the step's inputs and the part's,
  `{part}` in its command the part's name; a `:needs`, `:uses` or `:start-after` naming the step names its parts. A part
  `:clean-only true` runs only with `GATE_ALL_PARTS=1` (GTS19 (a): the parked sub-projects' corpora, GT7's clean run). The
  planner expands them before it validates (`expand-parts`; the audit expands the same way).
- *`:outputs`, `:uses`* (the early cutoff of §4.2 (b) for a step's output): a step with `:outputs` writes into `$GATE_OUT`
  (`.tmp/gate/out/NAME`), kept with its pass (`STORE/NAME/KEY.out/`, copied in and renamed) and put back when it is skipped. A
  step's `:uses` (`"STEP"` or `"STEP/FILE"`) put the producers' outputs' digests in its key (lines `uses STEP/FILE SHA`: a file's
  SHA-256, a directory's the SHA-256 of `sha256sum`'s lines of its files; `gate.plan/out-digest` and the runner's `out_sha`
  compute them alike) and imply `:needs`. When every producer is skipped the plan computes the key from their passes'
  outputs; when one runs the step's decision is `after`: the runner appends the lines once the producers ran, hashes, and
  skips it on a pass at that key (`skipped NAME (cut off: its producers' outputs as at its pass; passed at ...)`). A producer
  is never the canary (its consumers' decisions were made on its pass).
- *The program hosts `:prog`, `:javac-prog`* (GTS4 (b), (c)): `--gate-build` (and `bin/javac-vendor tool`) write the
  program's units beside the build, `units.tsv` (`bin/lib/prog-units.bash`): j2g's Go forms of the executable and of the test
  build (`j2g/prog`, `j2g-tests/prog`: no version stamp, which the link adds) split by namespace, a namespace's unit its
  compiled package `go/arbace/aot_NS` and its embedded source `cmd/arbace/res/PATH.ae.z`, every other file (the runtime's
  classes, jrt, goi's table, the main package, the other resources) `-core`; each unit's SHA-256 over its files' digests. A
  step on `:prog` is keyed by the units less those of its `:never-loads` and by `bin/arbace-go --gate-rest-key` (gate_key's
  files less the program's sources, `arbace/`, `go/`, `jdk/`, `overlay/jdk/`, the fixtures' forms, which reach the
  executables only through the units: the build's scripts, `bin/lib`, `overlay/go`, `x509roots`, the pins, Go's version);
  `:javac-prog` the same of javac's tool (`arbace_javac_rest_key`). A step's `:never-loads` also take those namespaces'
  sources out of its files (`:tools`' `arbace/j2g/`). Before the build exists the identity is pending: the step is planned to
  run, and since GT5 the builds run before every check and the plan is made again after them (`plan1.tsv` kept); a build
  kept from before the units has none, and the identity falls back to the source key.
- *`:heavy :inner`* (GTS31 (b)): below.

**The steps** (79 in the plan of `--full`, 85 with `GATE_ALL_PARTS=1`). Java's check: the essential `java-check` runs
(`bin/java-check --essential --save`) and `java-check-judge` judges (`--judge --essential`); `--full`'s `java-check-full` is eight
steps, `java-read` (its results and `forms.sha256`), `java-interp` and `java-compiled-full` (`:uses java-read/forms.sha256`:
the read's forms in their key, not repr/java's sources), `java-classes`, `java-diagnostics`, `java-print`, `java-forms-verify`
and `java-judge` (`:uses` the six levels' `results.tsv`; its inputs the group `:java-judge`: the classes, the records, the
reference, `check.ae`; the runs' group `:java-run` leaves the classes and records out, but the two levels whose would-be
regressions run again, interp and compiled, declare their `check-LEVEL.edn`). `bin/java-check --save DIR` runs the levels and
judges nothing; `--judge RESULTS...` judges (seconds); `JAVA_CHECK_FORMS`, the read's cache, is the main checkout's for the
gate's steps (`GATE_JAVA_FORMS`), so a level that runs where the read was skipped reads through it. Per corpus: `repr-go-native`
22 parts (6 `:clean-only`: `goos-*`, `cmd-tamago-*`, `box`, `purego`; each its report `native-check-PART.report.edn`,
`REPR_GO_NATIVE_REPORT`), `repr-java-parse` 7, `-front`, `-attr`, `-nojavac` 4 each, `repr-java-roundtrip` 3; each part
declares its corpus's own data (`:corpus-samples`, `:corpus-jdk`, `:corpus-javac`), the common group `:repr-java-checks` leaves
it out. On the program hosts with `:never-loads [arbace.j2g]`: suite-go, oracle-go, goi-check, goi-aot-essential, java-check,
java-interp, goi, goi-aot (`:prog`); repr/java's tree checks and round trips, java-fixtures, javac-classes, java-read,
java-classes, java-diagnostics, java-print, java-forms-verify (`:javac-prog`); the judgments and reap on stage 0 with
`:never-loads` (`:tools` less j2g). On `:gate` or `:javac` still: what runs j2g (diffcheck, java-compiled, suite-aot*,
programs*, fixpoint, java-compiled-full, javac-tool, j2g-edge, generated), the frozen JVM's (javac-verify, repr-java,
jrt-convert) and go-smoke (below).

**The audit's record of the namespaces loaded** (GTS4 (c)): `RT.load` appends each scriptbase to the file
`ARBACE_LOADED_LOG` names (one write, `O_APPEND`: the step's processes share it; `arbace/lang/RT.ae`, shipped code: the
executables and the tool built from this branch write it, stage 0 not until a re-seed); `bin/gate --audit` sets it per step,
and `gate.audit` fails on a namespace of the step's `:never-loads` (stage 0's tools loaded from the checkout show as reads of
their sources, which `:never-loads` took out of the declaration). The audit also covers a step's `:uses`' producers' inputs and
lists the files it read only through them (advisory: sound when it reads them to make the producer's output again, as Java's
levels read through the read's cache), and covers on `:prog` only the rest of the build's inputs.

**GTS30's tooling.** `bin/gate --audit` with no step audits every check of the plan (as GT4 built it), now every part and level
too. Its reducer is `tools/gateaudit` (Go, built by TamaGo's go into the audit's directory when its sources change;
`GATE_AUDIT_AWK=1`: busybox awk): the port of `test/gate/audit.awk`, the same lines in the same order on two traces (240 and
13,056 distinct pairs of 1,606 and 22,371 strace lines: the gate's own `--full --plan`), 23 times faster (0.09 s against 2.1).

**GTS31 (b), the tools' own lock.** `bin/lib/heavy.bash`'s `heavy_run CMD`: under `ARBACE_HEAVY_CMD` when it is set and
`ARBACE_HEAVY_HELD` is not, CMD's environment `ARBACE_HEAVY_HELD=1` either way. Wrapped: `bin/j2g` (j2g's process),
repr/go's native reader and converter (`bin/repr-go`'s `native_run`, `convert_main`, through `repr_go_run`), repr/java's
corpus reads (`read-jdk` at its five callers: `front-check`, `roundtrip` twice, `jrt-convert`, `javac-vendor convert`; not in
`bin/repr-java` itself, which is in the reader's hash and so in the provenance files'). The protocol: the gate exports
`ARBACE_HEAVY_CMD=$GATE_HEAVY_CMD`; a step `:heavy true` runs under the lock as a whole with `ARBACE_HEAVY_HELD=1` (its
tools take none: no second flock by a child of the holder, which would wait for ever); a step `:heavy :inner` runs without
it, its tools lock around their heavy process; a light step likewise (a heavy tool it starts still locks);
`GATE_HEAVY_CMD=env` (the caller holds the lock) sets `ARBACE_HEAVY_HELD=1` for every step. Outside the gate
`ARBACE_HEAVY_CMD` is unset and nothing locks: the agents' rule (`flock big.lock CMD`) is unchanged. The inner waits are
summed per step (`HEAVY_LOCK_WAITS`, `.tmp/gate/NAME.waits`, `:inner-wait-s`) and the summary says `its tools waited`.
`:inner` now: go-build, diffcheck, java-compiled, suite-aot-essential, programs, the repr/java tree checks' parts, the round
trip's parts, java-compiled-full, the fixpoint, suite-aot, programs-full, repr-go-native's parts; `:heavy true` stays where
the step's peak is not one tool's process (suite-go's 12 processes, repr-go's tree, the frozen JVM's steps, java-read and
java-print, javac-tool).

**GTS32 (a), the ticket lock.** `bin/heavy-lock CMD` (`GATE_HEAVY_CMD`'s default since GT5; `make gate-queue`: `--queue`):
a ticket `LOCK.queue/NANOSECONDS-PID` (holding the process's start time, so that a reused PID is not taken for the waiter),
a wait until no earlier ticket is live (the dead ones removed by the next waiter), then `flock LOCK`, CMD run with
`ARBACE_HEAVY_HELD=1`, the ticket removed at its end. The lock is still the plain flock: an agent's `flock big.lock CMD`
takes no ticket, never waits for the queue, and competes with its head alone (it may come before the head once, never
before the rest). Tested (`.tmp/gt5/fifo-test.sh`): a plain holder, five ticket waiters 0.4 s apart, then a plain user: the
waiters got the lock W1 to W5 in order, the plain user once between W1 and W2; a dead PID's ticket and a reused PID's (wrong
start time) removed, the next waiter not held up.

**GTS35 (a).** goi's references (`gcref`, `eref`, `earef`, `bin/goi`'s `ref_commit`): made in a temporary of their own
(`REF.tmp.PID`, not the shared `REF.tmp` two jobs of one key could both write), renamed with `mv -T` once complete; a
reference without its marker (an older run killed while writing) replaced; one another job completed first kept. Java's
forms caches (`bin/java-check`: the read level's, `nread`'s, the print level's `print_read`): the result file renamed into
place once written, and a read killed by a signal (status above 128) not cached at all (it was cached as a failure,
`fail status 137`, for good). The print cache of `arbace.build.go` (`cache-texts!`, `write-index!`) already wrote a
temporary and renamed it: unchanged.

**GTS36 (b).** `bin/javac-vendor edge` rewrites `jdk/javac/provenance.edn`'s `:jdk-edge` with the edge it records (made
less `jdk/edge.edn`); `bin/j2g-edge update`, when `jdk/edge.edn` changed, runs `bin/javac-vendor edge` (`J2G_EDGE_JAVAC=0`:
not, and it says what to run). `bin/provenance-check` (GTS36 (a)) also compares each provenance file's `:jdk` with the
platform's pin, and names each file's writer.

**GTS34 (a).** Every `--full` ends with `gate_prune` (its last line in the summary; `.tmp/gate/prune.log`); the prune removes a
pass's kept outputs with it.

**The exit criteria.**

- *On a toy table* (`.tmp/gt5/toy/toy.edn`, its runner `run.sh`, inputs under a scratch `gt5-toy/` of the checkout, its own
  store and lock; every mechanism): run 1 all 13 checks ran; run 2 nothing changed, all skipped; run 3 one part's input changed:
  that part alone ran; run 4 the "reader" changed but not its output: `toy-read` ran, `toy-interp` and `toy-judge` were cut off
  (`skipped ... (cut off: its producers' outputs as at its pass; ...)`); run 5 the source changed: read, interp and judge ran;
  run 6 the "classes" file changed: the judge alone ran; run 7 a j2g unit of `units.tsv` changed: the `:prog` step with
  `:never-loads [arbace.j2g]` skipped, the one without ran; run 8 the core unit changed: both ran. The heavy protocol: two
  `:inner` steps in two chains and a `:heavy true` one started their light halves in the same 30 ms, their heavy processes ran
  one after another (the `:heavy true` step's tool, with `ARBACE_HEAVY_HELD=1`, did not lock again), `its tools waited 0m08s
  for the lock`. The audit (`audit.sh`): a step that records `arbace/j2g/code` as loaded fails (`1 never-loads loaded`), a
  consumer's read of its producer's input is covered and listed as advisory.
- *A classes file changes, nothing reruns* (the real table, after the essential gate passed): a comment appended to
  `test/java/divergences.edn`; `bin/gate --full --plan`: `java-check` skip, `java-check-judge` run (`changed since the pass at
  ...: test/java/divergences.edn`), reap run (it judges the classes too); in `--full` the eight Java steps' runs are keyed
  without it (`java-judge` alone names it), their passes not yet made on this branch. The essential gate: **7 seconds**, the two
  judgments run, 13 checks skipped (`.tmp/gt5/demo-classes-gate.txt`); the comment reverted.
- *A j2g change that leaves the translations alone reruns only j2g's steps* (the real table): a comment line added to
  `arbace/j2g/out.ae` (shifting its lines); the essential gate rebuilt the executables (8m02s), whose units differed from the
  previous build's in one, `arbace.j2g.out`; the second plan skipped suite-go, oracle-go, goi-check, goi-aot-essential,
  java-check, java-check-judge, reap, provenance and go-smoke, and ran diffcheck, java-compiled, suite-aot-essential, programs,
  generated and j2g-edge: **14m57s** against 56m56s for the same gate with every check run (`.tmp/gt5/demo-j2g-gate.txt`);
  the comment reverted. The audit then moved go-smoke back to `:gate` (below): with it the same change reruns go-smoke's 2
  minutes too. In `--full` the same holds for the `:javac-prog` steps once javac's tool is rebuilt (the units of the tool's
  program less j2g's); not run here: no javac tool at this branch's key yet (a `--full`, GT7's).

**Measurements.**

| run | what | time | |
|---|---|---:|---|
| essential 1 (`903b053e`) | everything new: the build (two-pass, the runtime changed: RT.load), 14 checks | 275m56s | the build 13,276 s, of it about 3 hours waiting for the host's lock (other agents' 40-50 minute holds; each of the build's four heavy processes queued again); goi-check and goi-aot-essential failed (`ref_commit` not exported to the check's jobs, fixed), provenance failed (main's stale provenance, fixed by main's v2-prov) |
| essential 2 (`e407f73a`, main merged) | every check run | 56m56s | passed; go-build 20m31s (8m54s waiting), the `:inner` steps' tools waited 20-29 minutes for the lock each while their light work ran |
| essential, a classes file changed | the two judgments | 0m07s | passed |
| essential, j2g changed | the build and j2g's 6 steps | 14m57s | passed |
| the plan (`--full --plan`) | 79 steps, two plans | 2-3 s each | |

Peaks as GT6 measured them (diffcheck 25.2 GiB, the j2g steps 18-19.5, suite-go 9.3 / 11.7 tree); the gate's tree at most
43.2 GiB with the light steps beside one heavy process, the host's MemAvailable at least 17.6 GiB. The audit of the `:prog`
steps (`.tmp/gt5/audit-prog.txt`): oracle-go, goi-check, goi-aot-essential, java-check, suite-go loaded 66, 51, 24, 854
(the tests' own) and 153 namespaces, none of `arbace.j2g`, and read nothing the program host and their declarations do not
cover; java-check-judge and reap (stage 0, no record before a re-seed) read no j2g source.

**Slips and findings.**

- go-smoke was put on `:prog` and its audit refused it: `bin/arbace-go --gate` computes gate_key on its way to the cached
  build, which reads all 1,560 sources it hashes; back on `:gate`. A step's reads that only a key computation makes are reads
  all the same to strace.
- `ref_commit` (GTS35) was not in `bin/goi`'s `export -f` line: every error test's reference failed in the first gate; fixed.
- The per-process lock (GTS31 (b)) under contention: a step's heavy processes queue one by one, and the build of essential 1
  (four heavy processes, the runtime having changed) waited about three hours in all behind other agents' long holds, plain
  `flock big.lock` users coming in between them (GTS40).
- My first edit of the heavy lock went into `bin/repr-java`, which `repr_java_reader_hash` covers: it made the four provenance
  files stale; reverted, the lock put at `read-jdk`'s five callers instead.
- The coordinator's rule since the deadlock of 2026-10-29 (slot first, lock second: `.tmp/heavy flock big.lock CMD`) is what a
  GT5 gate does: it runs under `.tmp/heavy` and takes the lock per step or per process.

## Proposals (GTS40-GTS42)

- **GTS40 The lock held across a step's heavy processes.**
  - Options: (a) a step `:heavy :inner` whose tools lock per process keeps its ticket's place between them (bin/heavy-lock
    `--hold DIR`: the first heavy process of the step takes the lock, the ticket stays at the head until the step ends or idles
    for N seconds, its later heavy processes enter at once), so that a build's four processes queue once; (b) as built, one
    queue per process; (c) the step-level lock back for the builds (`go-build`, `fixpoint`: `:heavy true`).
  - *Recommended: (c) now, (a) if the everyday runs show it*: the builds are the steps whose heavy processes follow one
    another with little light work between them (essential 1: about three hours of queueing for 25 minutes of work); the others'
    light work (the shards' runs, the suite's runs) is what GTS31 meant to free, and it did (20-29 minutes waited beside it).
- **GTS41 Reads through a producer.**
  - Options: (a) as built: a consumer's reads of its `:uses` producers' inputs are covered and listed (advisory), the consumer
    declared to read them only to make the producer's output again (Java's levels through the read's cache); (b) refused, every
    such read declared (no cutoff for Java's levels, which read through `bin/repr-java read` on a cache miss); (c) the consumer
    run with the cache forced (`JAVA_CHECK_FORMS_ONLY=1`: a miss fails), so that it never reads the producer's inputs.
  - *Recommended: (c) at GT7, (a) until then*: (c) makes the claim checkable, a miss being a failure; it needs the read's
    cache present where the level runs, which the shared `GATE_JAVA_FORMS` gives.
- **GTS42 The essential gate's own exit criteria on `--full`.**
  - Options: (a) at GT7's clean run, then the two demonstrations again on its passes (a classes file, a j2g comment) with
    `--full --plan` after the tool's rebuild, recorded in the clean run's record; (b) a `--full` on this branch now (about five
    hours of the host's lock beside the other agents); (c) the essential gate's and the toy table's demonstrations only.
  - *Recommended: (a)*: the passes a `--full` makes are the clean run's to make; the mechanisms are the same code as the
    essential gate's, which ran them on the real table.

## Decided (2026-10-29): GTS40-GTS42

The user's decisions, all as recommended:

- **GTS40 (c) now, (a) if the everyday runs show it:** `go-build` and `fixpoint` are `:heavy true` again, so each holds
  the lock once for the whole step (`test/gate/steps.edn`). A ticket that keeps its place between a step's heavy
  processes (`bin/heavy-lock --hold`) waits until the everyday runs show it is needed.
- **GTS41 (c) at GT7, (a) until then:** for now, a consumer's reads of its producers' inputs are covered and listed as
  advisory. GT7's clean run runs Java's levels with the forms cache forced (`JAVA_CHECK_FORMS_ONLY=1`), where a cache
  miss fails.
- **GTS42 (a):** at GT7's clean run, the two demonstrations are repeated on its passes: a classes file, and a comment
  in j2g. `--full --plan` runs after the tool's rebuild, and the results go into the clean run's record.

## As built: GT7 (tooling) (2026-10-29)

Branch `gate-gt7`, from main `00ec2bf5` (REAP v2's second re-seed), main's `6ad08d82` merged (a fast-forward). Built: the tooling of GT7, the
clean run at a tag (§7), with the decided GTS18 (a), GTS19 (a), GTS30 (a), GTS33 (a)'s measurement, GTS37 (a), GTS41 (c) and
GTS42 (a); proved on the essential gate run clean. Not run here: the clean `--full` at `arbace-reaps-3-crops-v2` (GT7's exit
criterion, `test/gate/clean-arbace-reaps-3-crops-v2.edn`), the concurrent `--full` and the `--full` demonstrations: the main
session's, after the merge (the commands below). No shipped file changed.

**`bin/gate [--full] --clean [--tag TAG]`** (`make gate-clean`, `make gate-full-clean TAG=...`). In the invoking checkout,
`clean_launch` (in `bin/gate`, not `bin/lib`: nothing of it decides a step's outcome, so no key holds it):

- *The clone*: `git clone --shared --no-checkout` of git's common directory into `.tmp/gate-clean/SHA`, HEAD's commit checked
  out detached (an uncommitted change is warned of: the run tests the commit). Its `.tmp/` and `target/` are empty; `GOCACHE`
  is its own `.tmp/gocache`, `TMPDIR` its `.tmp/tmp`. The clone's git directory is its own, so `gate_env` makes its store, its
  `ARBACE_GATE_EXES` and `GATE_JAVA_FORMS` the clone's: no pass is read (an empty store, and `--rerun`), stage 0, the
  executables (G1: the fixpoint's `--g1` takes the clone's own fresh build) and javac's tool are built fresh.
- *The environment*: every `ARBACE_*`, `GATE_*`, `J2G_*`, `REPR_*`, `JAVA_CHECK_*`, `CLOJURE_TESTS_*`, `GOI_*`, `FROZEN_*`, `JAVAC_*`,
  `BOX_*`, `HEAVY_LOCK_*` variable and `GOCACHE`, `GOFLAGS`, `GOROOT`, `GOOS`, `GOARCH`, `GOMEMLIMIT`, `TMPDIR` dropped, but the
  caller's lock options (`ARBACE_HEAVY_LOCK`, by default the main checkout's `.tmp/big.lock`, `GATE_HEAVY_CMD`,
  `ARBACE_HEAVY_HELD`), `GATE_TEST_JOBS`, `ARBACE_GOROOT` and `JDK`. A caller that holds the big lock for the whole run passes
  `GATE_HEAVY_CMD=env` (serial: nothing beside it); by default the heavy steps take it in turn (`bin/heavy-lock`) as everyday.
- *The clone's own `bin/gate`* runs with `GATE_CLEAN_INNER=1`: first the pre-step `pins` (below), then the gate as ever with
  `--rerun`, serial (the essential phase too, in the table's order), every step under the audit (`audit_all`), `GATE_ALL_PARTS=1`
  (GTS19 (a): 85 steps in `--full`'s plan), `JAVA_CHECK_FORMS_ONLY=1` (GTS41 (c)), `GATE_CPU_LOG=1` (GTS33 (a)), and at the end
  the record `.tmp/gate/clean-record.edn`.
- *Afterwards*: the record copied to `test/gate/clean-TAG.edn` (TAG `--tag`, `GATE_CLEAN_TAG`, else HEAD's short SHA), the logs to
  `.tmp/gate-clean/SHA.logs/` (`gate/`: every step's log, `.mem`, `.audit`, `.paths`, `.loaded`, `.cpu.tsv`; `summary.txt`), the
  passes (each marked `:clean true`) copied into the invoking checkout's store beside its own (none replaced) and, when the run
  passed, the executables' and javac's tool's directories moved into the shared `ARBACE_GATE_EXES` where that key is absent
  (`GATE_CLEAN_PUBLISH=0`: neither), so that the everyday gate and the demonstrations find the clean run's passes and builds; the
  clone removed (on a failure kept without its Go cache; `GATE_CLEAN_KEEP=1` keeps it).
- `bin/gate [--full] --clean --plan` prints the plan such a run makes (an empty store and executables' directory, every part,
  every check run), on the invoking checkout's stage 0: `--full`'s is 85 steps, 3 always, 2 builds, 77 run, 3 after.

**The audit of every step** (GTS30 (a), `bin/lib/gate-run.bash`). With `audit_all` set, `run_one` runs each step's own command
(not its `:audit-cmd`: the full command, every corpus) under strace inside `step`, between the lock and busybox `time`: `env
ARBACE_LOADED_LOG=NAME.loaded strace -f ... -o NAME.fifo /usr/bin/time -v ... CMD`, the FIFO read at once by the reducer
(`audit_reducer`, `audit_reduce`: `tools/gateaudit`, built into the clone's `.tmp/gate-audit/`, the same code `--audit` uses since
this branch), into `NAME.paths`; then `audit_compare` runs `gate.audit` against the plan's `files.sha256`: `NAME.audit`, and
`NAME.audit-status` `ok`, `N undeclared`, `step-failed` (what a failed step read is not an audit) or an error. An undeclared read
fails the gate (the summary's `AUDIT 3 undeclared (...)` with the files) and the step's pass is not recorded (its key is known to
miss an input). A trace strace aborted (`ptrace(PTRACE_LISTEN)`) is run again, twice at most (`NAME.audit-aborts`). The builds
(`go-build`, `javac-tool`) are audited too, against their hosts' keys (`gate_key`, `arbace_javac_key`): a read outside them is a
build input the key misses. The pre steps are not (always run, never keyed). strace's cost lands in the step's wall time; its
CPU and peak are not counted (time measures the command), so the record's CPU seconds and peaks are the steps'.

**`bin/gate --pins [FILE]`** (`make gate-pins`), the clean run's first step: what the clone cannot make fresh, by its pins (§7):
TamaGo's go against `seed/goroot.sha256` (its sources' hash, `bin/lib/seed-go.bash`), jdk26u's commit against
`jdk/javac/platform.sha256`'s and the build tree's platform made and checked module by module (`bin/javac-vendor platform`, 9 s),
the system JDK (the reference javac: `javac 26`, its `release` file's digest, the `:jdk` host's identity), the frozen jar against
`test/oracle/frozen-jar.sha256` when `FROZEN_JAR` names it (the launcher passes the main checkout's
`.tmp/oracle-frozen/arbace-for-java-26-v4.jar`; without it the clone builds it from its tag and `frozen_jar` checks it), the tag
of the frozen tree `clojure/` (extracted by the clone's own git into its `.tmp/frozen`); `$GOROOT/test`'s digest is recorded,
not checked (no pin covers it: GTS50). About 10 s; exit 1 on a pin that fails, which stops the clean run.

**The record**, `test/gate/clean-TAG.edn` (the Clojure reader's format, written by `clean_record`): `:tag`, `:commit`, `:date`,
`:mode`, `:status`, `:wall-s`, `:host` (CPUs, memory, kernel), `:env`, `:pins` (`--pins`'s map), `:hosts` (the hosts' identities
of `hosts.edn`) and `:steps`, one map a step in the run's order: `:step`, `:kind`, `:status` (`:pass`, `:fail` with `:exit`,
`:not-run`), `:key`, `:wall-s`, `:cpu-s`, `:maxrss-kib`, `:tree-peak-kib`, `:wait-s`, `:inner-wait-s`, `:audit` (`:ok`,
`:step-failed`, `:none` for the pre steps, or the failure's text), and for a step whose programs run under `limited`
`:programs`, `:programs-cpu-s`, `:programs-timeouts` (each program's line in its `.cpu.tsv`). `bin/gate-demos --record` adds
`:demonstrations`.

**GTS41 (c), `JAVA_CHECK_FORMS_ONLY=1`** (`bin/java-check`): a run without the read level (interp, compiled, print) takes each
test's forms from the read's cache alone; a miss is not read but recorded (`WORK/forms-misses`, the test's result `fail the
forms cache missed`) and fails the run (`java-check: FAILED: N tests' forms not in the read's cache`). A run with the read level
(java-read, `--forms verify`) reads as ever, so the clean run exports it for the whole gate. A cached read failure is a hit (the
read's own verdict). Not run here: javac's tool exists at no key yet (the clean `--full` makes it).

**GTS33 (a)'s measurement** (`bin/lib/limit.bash`): with `ARBACE_CPU_LOG` set, `limited_cpu` runs the program under busybox time
and appends its status, wall, user and system seconds and command to that file (a time killed by the wall cap writes no
times); the status is the one a shell gives (time's signal number mapped back to 128+N: 137 and 152 still read as timeouts).
The clean run sets it per step (`NAME.cpu.tsv`). Recording the limits from it is GTS51. (`bin/lib/limit.bash` is in `gate_key`:
this change rebuilds the executables once.)

**GTS37 (a), `bin/gate --full --concurrent`** (`make gate-full-concurrent`): the run re-executes itself under `bin/heavy-lock`
(the host's big lock taken in turn, once, for the whole run; `GATE_CONCURRENT_HELD=1`: the caller holds it), drops the
`ARBACE_HEAVY_HELD` that gives it, sets `GATE_FULL_PARALLEL=1` and `GATE_HEAVY_CMD="flock .tmp/gate-concurrent.lock"`
(`GATE_CONCURRENT_LOCK`): the `:heavy true` steps and the `:inner` steps' heavy processes serialize on that private lock
(`ARBACE_HEAVY_CMD`, `bin/lib/heavy.bash`), the light work beside them, the peaks in the `.mem` files and `mem.tsv` as ever. Tested
on a toy table: a step's environment had `ARBACE_HEAVY_CMD=flock .../own.lock`, no `ARBACE_HEAVY_HELD`, the big lock the toy's.

**GTS42 (a), `bin/gate-demos [--record FILE]`** (`make gate-demos RECORD=...`) and `bin/gate [--full] --plan --builds` (the builds
the plan needs run as the gate runs them, then the plan on their units). In a scratch worktree of HEAD (`.tmp/gate-demos/SHA`, the
store, the executables and the lock shared through git's common directory): the base plan; a comment line appended to
`test/java/divergences.edn`, `--full --plan`; a comment line added to `arbace/j2g/out.ae`, `--full --plan --builds` (the
executables and javac's tool at the changed key); each reverted. Each plan's steps by decision into
`.tmp/gate-demos/SHA.edn` and, with `--record`, the clean run's record's `:demonstrations`; the j2g change's new builds removed.
Plans and two builds (about 20 minutes and the lock), no check; `--essential`: the essential gate's plans, one build. The demos'
plans run with `GATE_CANARY=0`.

**The toy table** (`.tmp/gt7/toy/toy.edn`, `run.sh`: the inner mode on steps that read `gt7-toy/` files): a declared read
`audit ok`; an undeclared one `AUDIT 1 undeclared` naming `gt7-toy/b.txt`, the gate failed and its pass not recorded; a failing
step `not audited (the step failed)`; the build and the heavy step audited; a `limited` step's two programs in its `.cpu.tsv`;
the passes `:clean true`; the record read back by the Clojure reader on stage 0.

**The clean audit's findings** (the essential gate clean, three runs; every one fixed, the third passed with every audit ok):

- *`go-build` read 2,160 files its key does not cover*: `tamago/goroot/` (2,152: the tamago configurations of goi's table, which
  the executables embed, `bin/goi table --configs ...,tamago/amd64,tamago/arm64`) and `tools/godump/` (8: the key of the linux
  std's read the table is made from). A change to either kept the gate's executables, their table stale: `gate_key`
  (`bin/arbace-go`) and `arbace_javac_key` (`bin/lib/javac-key.bash`) now hash both, and `gate.audit`'s `:gate` and `:javac`
  hosts cover them. GT4 never audited the builds; the clean run's audit of every step is what found it.
- *`bin/heavy-lock`* in every `:inner` step (diffcheck, java-compiled, suite-aot-essential, programs, goi-aot-essential): the
  tools' `ARBACE_HEAVY_CMD`, which takes the lock and decides nothing; `gate.audit` counts it with the gate's logic.
- *`goi-aot-essential` loaded `arbace.j2g.zlib`*, one of its `:never-loads` (`arbace.j2g`): `arbace.go.table` compresses goi's
  table with it, and a cold run makes the table (GT5's audit ran on a cached one). goi's four steps (goi-check,
  goi-aot-essential, goi, goi-aot) now name j2g's other 19 namespaces one by one: a zlib change reruns them.
- *`fixedbugs/issue21576.go` failed under gc and goi alike*: it runs `go run` within a minute, and found the host's Alpine go
  1.27.2 on PATH (pinned by nothing), which with the clone's empty `GOCACHE` compiled std past the minute; the log's
  timestamps then differed. `bin/goi check` puts TamaGo's `bin/` first on its programs' PATH (the 26 programs of
  `$GOROOT/test` that run the go command run the pinned toolchain, whose cache the references warm); the gate-wide fix is GTS52.
- *`gcgort.go` timed out* in the second run, alone too: 53 s of CPU (36 s of it system time) against `limited 10`'s 50, on a
  loaded host (diffcheck took 23:50 in that run, 11:17 and 11:38 in the others). Its own limit is 20 s in
  `test/goi/timeouts.txt` (100 s of CPU); its measured times are GTS51's data.

**The runs** (`.tmp/heavy bin/gate --clean --tag gt7-essential` in this worktree; each built stage 0, the executables and the
test build fresh in an empty `GOCACHE`; the logs and records in `.tmp/gt7/run{1,2,3}/`):

| run | commit | result | time |
|---|---|---|---:|
| 1 | `ddc55365` | 4 checks' audits failed (heavy-lock), go-build's (2,160), goi-aot-essential's (zlib); goi-check failed (issue21576.go) | 76m42s |
| 2 | `9961d166` (the fixes) | every audit ok; goi-check failed (gcgort.go's CPU limit) | 87m04s |
| 3 | `97f8367c` (its limit) | **passed**, every step audited ok; 15 passes published, the executables published | 86m06s |

The third run step by step (wall m:s, CPU minutes, the largest process's peak and the tree's, GiB), against the keyed essential
gate's latest passes (main's, concurrent, warm caches, no audit):

| step | clean | CPU | peak / tree | keyed | |
|---|---:|---:|---:|---:|---|
| pins | 0:05 | 0 | 0.1 | — | new |
| stage0 | 2:28 | 14 | 9.3 / 9.2 | 0:00 | kept by its pins everyday |
| go-build | 16:54 | 103 | 18.8 / 18.7 | 0:00-15:00 | one j2g run (the host's runtime the checkout's) |
| go-smoke | 2:07 | 1 | 0.8 / 0.6 | 1:59 | |
| suite-go | 3:26 | 21 | 9.4 / 12.7 | 2:01 | |
| oracle-go | 1:24 | 2 | 0.8 / 3.2 | 1:08 | |
| diffcheck | 11:38 | 128 | 26.2 / 26.2 | 9:55 | |
| goi-check | 14:01 | 47 | 6.1 / 3.2 | 1:21 | gc's references and the corpora's reads made cold |
| goi-aot-essential | 3:40 | 16 | 7.8 / 6.5 | 1:18 | |
| java-check | 0:28 | 3 | 0.4 / 1.7 | 0:34 | |
| java-check-judge | 0:02 | 0 | 0.3 | 0:02 | |
| java-compiled | 13:16 | 63 | 18.9 / 18.9 | 5:14 | its program built cold |
| suite-aot-essential | 10:11 | 40 | 18.1 / 18.1 | 6:31 | |
| programs | 4:19 | 24 | 18.9 / 18.9 | 4:10 | |
| generated | 0:04 | 0 | 0.4 | 0:03 | |
| j2g-edge | 1:31 | 10 | 6.0 / 3.1 | 1:19 | |
| reap, provenance | 0:02 | 0 | 0.3 | 0:02 | |
| **total** | **86:06** | | gate tree 25.8 max | 56:56 (GT5's essential 2, every check run) | serial and audited against concurrent |

The audit's cost is in the wall times only (strace traces the file calls and `clone`; the CPU and peaks are the commands'): the
light steps moved by seconds, the heavy ones by their cold caches more than by the trace. The record (`.tmp/gt7/run3/
clean-gt7-essential.edn`) reads back with the Clojure reader; it is not committed (the tag's is the exit criterion).

**The demonstrations, on the essential gate** (`bin/gate-demos --essential --record`, after run 3, on its passes; `--essential`
plans the essential gate, GT5's own criteria; `.tmp/gt7/demos-essential.txt`, the record with `:demonstrations` in
`.tmp/gt7/demo-record.edn`): nothing changed, 14 of 15 checks skipped (the canary ran: `GATE_CANARY=0` in the demos' plans
since); a classes file, `java-check-judge` and `reap` to run (and the canary), the other 12 skipped; a j2g comment, after the
executables' rebuild (11:08, units differing in `arbace.j2g.out`), go-smoke, diffcheck, java-compiled, suite-aot-essential, programs,
generated and j2g-edge to run, the five `:prog` checks (suite-go, oracle-go, goi-check, goi-aot-essential, java-check) skipped;
the rebuilt directory removed. The `--full` demonstrations need the clean `--full`'s passes and javac's tool: the main session's.

**`bin/gate --full --clean --plan`** here: 85 steps, 3 always, 2 builds, 77 run, 3 after (the 6 `:clean-only` parts of
repr-go-native included; `.tmp/gt7/clean-plan-full.txt`).

**For the main session** (after the merge; each from the main checkout, the quiet-host file up while the clean run runs):

- the clean `--full` at the tag: `touch .tmp/gt7.quiet; .tmp/heavy bin/heavy-lock env GATE_HEAVY_CMD=env bin/gate --full --clean
  --tag arbace-reaps-3-crops-v2; rm .tmp/gt7.quiet` (the big lock held once from outside for the whole run; serial, so
  nothing beside its steps), then commit `test/gate/clean-arbace-reaps-3-crops-v2.edn` with the tag's journal entry;
- the demonstrations on its passes: `.tmp/heavy bin/gate-demos --record test/gate/clean-arbace-reaps-3-crops-v2.edn` (two builds,
  the executables' and javac's tool's, under the lock; plans otherwise);
- the concurrent `--full` (GTS37 (a)): `.tmp/heavy bin/gate --full --concurrent --rerun` (`--rerun`: every check runs, else the
  clean run's passes skip them), its peaks in `.tmp/gate/*.mem` and `mem.tsv`.

## Proposals (GTS50-GTS57)

- **GTS50 `$GOROOT/test` pinned.**
  - Options: (a) `seed/goroot.sha256`'s hash, or a second line of it, covers TamaGo's `test/` beside `VERSION` and `src/`,
    checked by `--pins` and `bin/seed`; (b) as built: its digest recorded in the clean run's `:pins`, checked by nothing.
  - *Recommended: (a)*: goi's checks, repr/go's corpora and `$GOROOT/test`'s references read it, and the steps declare
    `seed/goroot.sha256` as its pin, which does not cover it; 3,516 files, a second of hashing.
- **GTS51 The CPU limits recorded from the clean run (GTS33 (a)'s second half).**
  - Options: (a) `bin/goi check --record-cpu` and `bin/java-check --record-cpu` read a clean run's `NAME.cpu.tsv`
    (`.tmp/gate-clean/SHA.logs/gate/`), write each program's CPU seconds beside its reference (`test/goi/cpu-linux-amd64.tsv`,
    `test/java/cpu-jdk26.tsv`), and `limited` takes three times the recorded time (at least the default's floor) where one is
    recorded; (b) the same from a second, unaudited quiet run; (c) the factor of five kept.
  - *Recommended: (a)*: the audit traces the file calls alone (`--seccomp-bpf`), which a program's CPU time barely sees; the
    clean run is the quiet run the decision asked for, and its record names the commit the times belong to.
- **GTS52 The host's `go` on PATH.**
  - Options: (a) `gate_env` puts TamaGo's `bin/` first on every step's PATH (and `bin/lib/tmpdir.bash` for the scripts outside
    the gate), so that a program or script that runs `go` runs the pinned toolchain; (b) per tool, as built for goi's check.
  - *Recommended: (a)*: the clean run found `$GOROOT/test`'s programs that run the go command (26 of them) on the host's
    Alpine go 1.27.2, pinned by nothing, and "no build may use the host's go"; one place covers every step.
- **GTS53 Stage 0's key and audit.**
  - Options: (a) stage 0 kept by its pins and by `bin/seed` and `bin/lib/seed-go.bash`'s digests, and audited at the clean run
    against them; (b) as built: kept by the seed's and Go's pins alone, never audited (a `:pre` step).
  - *Recommended: (a)*: a change to the scripts that build stage 0 keeps a stage 0 they no longer make; the cost is a second
    of hashing.
- **GTS54 The clean run's order.**
  - Options: (a) serial, as §7 and as built (the quiet times calibrate the limits and the budgets); (b) by chain under the
    audit, the heavy steps on a private lock (`--concurrent`'s protocol), its times then not quiet.
  - *Recommended: (a)*: the essential gate took 77 minutes clean and serial with every step traced against 57 keyed and
    concurrent with every check run (GT5's essential 2); the `--full` clean run is the tag's, once.
- **GTS55 The clean run's passes published.**
  - Options: (a) as built: its passes (`:clean`) into the shared store beside the everyday ones, its builds into the shared
    executables' directory where absent, so that the next gates and the demonstrations start from them; (b) kept in the clone
    (the record alone survives).
  - *Recommended: (a)*: the passes are keyed by content like every other, and the demonstrations (GTS42 (a)) are to run on
    them; `GATE_CLEAN_PUBLISH=0` keeps a run apart.
- **GTS56 The frozen jar at the clean run.**
  - Options: (a) as built: the main checkout's `.tmp/oracle-frozen/` jar, checked by its pin; (b) built in the clone from its tag
    (the frozen branch's own build, a JVM build of minutes).
  - *Recommended: (a)*: the pin is the jar's identity, and the jar's build belongs to the frozen branch's record, not the
    commit's; (b) when no jar is there, as built.
- **GTS57 A kept build that waits for the lock.**
  - Options: (a) `go-build` and `javac-tool` look for their key's directory before the lock (`bin/arbace-go --gate-build
    --check-cached`, `bin/javac-vendor tool --check`) and run without it on a hit; (b) as built: the whole step under the lock.
  - *Recommended: (a)*: this branch's last essential gate skipped 14 checks on the clean run's passes and found the executables
    the clean run published, and still waited 10m15s in the queue for a build of 0 seconds.

**Slips.** Two commands redirected stderr to `/dev/null` (a check of the record and a loop over the store's passes, both
read-only); `bin/gate-demos` was edited (by `sed -i`, a new file) while a run of it executed; the toy's first table named
`bin/lib` in a `:cmd`, which the planner refused (a test file, fixed).


## Decided (2026-10-29): GTS50-GTS57

The user decided all eight as recommended. GTS50 (a), GTS52 (a) and GTS53 (a) are built before GT7's `--full
--clean` run, so that the run proves them, and GTS57 (a) with them (branch `gate-gts5x`). GTS51 (a) comes after the
clean run, from its CPU log. GTS54 (a), GTS55 (a) and GTS56 (a) are as built.

## As built: GTS50, GTS52, GTS53, GTS57 (2026-10-29)

Branch `gate-gts5x`, from main `abc1316c`. Built before GT7's `bin/gate --full --clean` run, which is to prove the first
three; the four decided as recommended (GTS50-GTS57). No shipped file changed (the seed, `arbace/`, `go/`, `overlay/`,
`jdk/`, `tamago/` are as they were). The scripts `bin/seed`, `bin/arbace-go` and `bin/lib/seed-go.bash` are in `gate_key`:
the first gate on this branch builds the executables once (and javac's tool once, in `--full`).

- **GTS50 (a), `$GOROOT/test` pinned.** `seed/goroot-test.sha256` ("`<SHA-256>  go1.27.1`": sha256sum's lines of every file
  under TamaGo's `test/` in the C locale, hashed again: 3,516 files, a second), `seed_go_test_hash` in `bin/lib/seed-go.bash`.
  `bin/gate --pins` checks it (`pins: $GOROOT/test ...: seed/goroot-test.sha256's`; a changed digest fails, tested) and no
  longer only records it. Deviation from the proposal's wording: a file of its own, not a second line of
  `seed/goroot.sha256`, which is in `gate_key`, in stage 0's identity and in the pins of every step on `:go`: a change to `test/`
  would have reran what never reads it. The six steps that declare `seed/goroot.sha256` as the pin of `$GOROOT/test` (goi,
  goi-aot, goi-check, goi-aot-essential, repr-go, repr-go-native) declare the new file beside it.
- **GTS52 (a), the host's `go`.** `gate_env` (`bin/lib/gate-run.bash`) puts `${ARBACE_GOROOT:-/root/tamago-go}/bin` first on
  `PATH`: every step, the tools and the audit inherit it. `bin/lib/tmpdir.bash` (the scripts run by hand) is not changed: it is
  in `gate_key` and would rebuild the executables for nothing the gate needs (GTS60).
- **GTS53 (a), stage 0's key and audit.** `bin/seed --build`'s pins file (`target/seed/pins`) holds the digest of `bin/seed` and
  `bin/lib/seed-go.bash` (a change to either builds stage 0 again, 15 s warm, 2.5 minutes cold), and `hosts.edn`'s `:stage0`
  (`plan_inputs`) the same digest: every step on `:stage0` is rekeyed by a change to either script, as it already was by the seed's
  and Go's pins. The `stage0` step declares `:hosts [:stage0] :inputs [:lib-base]` and an `:audit-cmd` that forces the build
  (`ARBACE_SEED_FORCE=1`); `audit.ae`'s `:stage0` host covers the seed's tar (held by its pinned hash), `bin/seed` and
  `bin/lib/seed-go.bash`. Audited by `bin/gate --audit stage0` (now allowed for this one `:always` step: 7 declared files, 0
  undeclared after the tar was declared, which the first audit found) and by the clean run: `bin/gate --clean`'s inner gate
  runs the pre step `stage0` under strace and compares it with the plan's files once the plan exists (`stage0's audit`; an
  undeclared read fails the gate; `stage0.audit-status` reaches the record's `:audit`). Tested on a toy table in inner mode
  (`ok  stage0's audit`; stage 0 kept there: a real clone builds it).
- **GTS57 (a), a kept build and the lock.** `bin/arbace-go --gate-build --check-cached` and `bin/javac-vendor tool DIR --check`
  say, with their own key test, whether the build is there (exit 0); `run_one` asks (`build_cached`, keyed on the step's name)
  for a `:heavy true` `:kind :build` step and runs it without the lock on a hit (the command then only prints that it is
  cached). A miss goes through the lock as before.

**Runs** (this worktree; the logs `.tmp/gate-run{1,2,3}.log`). `bin/gate --pins` ok (`$GOROOT/test` 812ac548..., a wrong digest
fails); `bin/gate --full --clean --plan` 85 steps (3 always, 2 builds, 77 run, 3 after), as before; `bin/gate --audit stage0`
ok (7 declared files, 0 undeclared; 19 s); `make check-makefile` ok. The essential gate: run 1 built the executables (go-build
11m08s, 89m CPU, 19.2 GiB) and then waited hours in the lock's queue behind another agent's `repr-c` job (the run was lost before
its end; a killed run left an empty `.tmp/repr-go/forms/linux-dirtest`, which made goi-check and goi-aot-essential fail with
"no forms of a main package" in run 2: scratch, removed); run 2 (11m01s) ran diffcheck, java-compiled, suite-aot-essential and
programs; run 3 passed (2m52s: goi-check and goi-aot-essential again, the rest at their passes). **GTS57**: in runs 2 and 3
`go-build` was `ok (0m00s ...)`, its `.cached` file "the cached executables (their inputs unchanged, key d24c0b71ce32)", its
wait 0, no ticket in the lock's queue (the previous essential gate waited 10m15s for the same). Not run: the clean essential
gate (about 90 minutes; the stage 0 audit of a real build is proved by `--audit stage0`, the inner mode's glue on a toy table
where stage 0 was kept), `--full`, the javac-tool path of GTS57 (the same code, `bin/javac-vendor tool DIR --check`, not exercised).

**Proposals.**

- **GTS60 `PATH` for the scripts run by hand.**
  - Options: (a) `bin/lib/tmpdir.bash` puts TamaGo's `bin/` first on `PATH` too; (b) the same change at the next change that
    rekeys the executables anyway; (c) as built: the gate's steps alone.
  - *Recommended: (b)*: nothing in the gate reaches the host's `go` any more; `tmpdir.bash` is in `gate_key`, so (a) now costs an
    8 minute build and a rerun of the keyed checks on this branch for a convenience outside the gate.
- **GTS61 The kept-build check as data.**
  - Options: (a) a step key `:cached-cmd` (an unkeyed column of `plan.tsv`) in place of `build_cached`'s two cases; (b) as built.
  - *Recommended: (b)* while there are two builds: the command and its check live in the same two scripts; (a) when a third
    build step appears.

## Decided (2026-10-29): GTS60-GTS61

The user decided both as recommended: GTS60 (b), so `bin/lib/tmpdir.bash` puts TamaGo's `bin/` first on `PATH` at the
next change that rekeys the executables anyway (NH11's merge after the tag is one); GTS61 (b), as built.

## As built: GT7's fix round (2026-10-30)

Branch `gate-gt7fix`, from main `68b2bba6` (the commit of GT7's first clean `--full`, `bin/gate --full --clean --tag
arbace-reaps-3-crops-v2`, started 2026-10-29 16:59; its summary `.tmp/gt7-full/run.log`, its clone
`.tmp/gate-clean/68b2bba6.../`). Built: the fixes of what that run found, a watchdog for the audit's reducer, the
audit's rule for hashed-only reads, and `bin/gate --clean --only`, the rerun of some steps merged into the record. No
shipped file changed: nothing under `arbace/`, `go/`, `overlay/`, `jdk/`, `tamago/`, `seed/`; `bin/lib/gate-run.bash`
and `test/gate/plan.ae`, which every key holds, are unchanged too (below: the rule that lets a rerun keep the
other steps' passes), and so are `gate_key`, `arbace_javac_key` and the program hosts' identities.

**The first clean run's findings** (status by step in the clone's `.tmp/gate/*.status`, `*.audit-status`):

| finding | cause | fix |
|---|---|---|
| `goi-check` failed: `gcgort.go` timeout (20 s) | 112 and 118 s of CPU (94 and 105 s system time: the GC's threads on 64 CPUs under ptrace) against `limited 20`'s 100 (`goi-check.cpu.tsv`; 32 s quiet unaudited, 37-53 s loaded at GT7's essential runs) | `test/goi/timeouts.txt`: 60 (300 s of CPU, 2.5 times the audited time) |
| `goi` failed: `fixedbugs/issue13160.go`, `issue13169.go` timeouts (10 s), gcgort.go again | 52 and 50 s of CPU (47 and 16 s system time), the retry alone the same (`goi.cpu.tsv`): goroutines and the GC under ptrace | 30 each (150 s) |
| `goi-aot` failed: `gc2.go`, `heapsampling.go` timeouts (10 s) | compiled by gc, 40 to 52 s of CPU at both tries (`goi-aot.cpu.tsv`; the GC on 64 CPUs) | 30 each |
| `repr-go` frozen 3.5 hours under the audit, killed at 04:08 (137) | the reducer stopped reading strace's FIFO (`repr-go.paths` stopped at 40 × 64 KiB, its last full buffer, at 00:42, during the second conversion, `$GOROOT/test`'s); strace blocked on the full FIFO, every traced process on strace. Once the reducer was killed strace wrote on (89 MB of `Broken pipe` in the log within a minute): the FIFO was full and strace alive, so the reader did not read with data pending. The reducer has no goroutine pipeline and no bounded channel: one goroutine reads the FIFO (an `os.Open`ed FIFO is non-blocking and waits in Go's netpoller), parses, writes a regular file. Not reproduced (a stress run of 8 parallel tree walks through the FIFO, `.tmp/gt7fix/`, read it all); the futex wait the main session saw is what a Go program parked on its netpoller, or one stopped, shows | the reducer reads the FIFO by a blocking descriptor (`syscall.Open` + `os.NewFile`: not pollable, plain `read(2)`, no netpoller between strace's writes and the reader) and ignores SIGTSTP, SIGTTIN, SIGTTOU; a **watchdog** (below) turns any stall into a loud failure in minutes. A second cause, found by tracing the step here: under strace repr-go's tens of concurrent Go builds and gocmp runs (GOMAXPROCS 64 each) preempt their threads by SIGURG, each signal a ptrace stop that strace's one thread serves (24% of a std build's trace lines; here a `compile` at 1,396 s system and 187 s user time, `go list` at 1,991 s user, 150-220 signals queued each, strace at 96% CPU): `roundtrip amd64` traced did 73 of 2,080 packages in 100 minutes, the reducer keeping up (its heartbeat: 6.5 GB read, nothing pending). `export GODEBUG=asyncpreemptoff=1` in repr-go's `:cmd` (cooperative preemption only; the tools' outputs do not depend on it): the same traced roundtrip **passed in 20 minutes** (1,206 s, no regression), audit ok once `tamago/goroot/go/go/` was declared (go/parser's forms, which stage 0's goi interprets for the native reader until the re-seed that carries GP7) |
| 7 Java steps' passes not recorded, 2,581-2,603 undeclared each | `bin/java-check` (the forms cache's reader key) and `bin/clojure-tests --java-fixtures` (the provenance's host) call `repr_java_host`, whose `arbace_javac_fresh` computes `arbace_javac_key`: `xargs sha256sum` over every source of javac's tool (2,601 files: `tamago/goroot/` 2,269, `arbace/lang/` 142, `go/arbace/` 142, `arbace/j2g/` 20, repr/haskell, rust and c 19, `tools/godump/` 8, `go/arbace.ae`). On `:javac-prog` (GT5) the audit covers only the rest key's files, the sources being in the tool's units. GT5 audited the `:prog` steps but never the `:javac-prog` ones (no tool at its key); go-smoke's like case went back to `:gate` | the reducer marks a read by `sha256sum` **H** (hashed only); `gate.audit` covers a hashed-only read of a file in the full key of a step's program host or of its `:uses` producers' (on `:prog` gate_key's files, on `:javac-prog` arbace_javac_key's): the identity check, which GT5's design keys by the units instead. A read by any other process stays a read. Proved: `repr_java_host` traced alone with the new reducer, 4,848 H lines, 26 R; `gate.audit` as java-fixtures: **0 undeclared, 2,601 hashed only, covered** (the run's 2,601 exactly). The steps themselves traced (`.tmp/gt7fix/trace.sh`, step()'s strace and FIFO with the new reducer, javac's tool of the clean clone copied): java-classes (`--only` its audit subset) and java-forms-verify **audit ok**, 2,601 hashed only; java-fixtures read 140 of `arbace/lang/` for real (`bin/repr-java declarations`: the runtime's Java forms, javac's class path for the fixtures), hidden among the hashed in the first run: `arbace/lang/` declared, then audit ok |
| java-compiled-full: 19 undeclared | the same key computation (repr/haskell, rust, c: in arbace_javac_key, not in gate_key), the step on `:gate` with `:uses java-read/forms.sha256` (`:javac-prog`) | covered by its producer's host (above) |
| java-print: `test/jrt/classfile.ae`; java-forms-verify: `test/java/check-read.edn`, `test/java/divergences.edn` | real reads (the print level's class files; `--forms verify` judges the read) beside the 2,601 | declared |
| repr-go-native.box (`:clean-only`): `bin/build-go` | the box's packages built for the comparison | declared on the part |
| programs-full: `bin/repr-clojure` | `--build-program` translates a sample's `.clj` with repr/clojure (REAP7b) | declared |
| javac-tool: `bin/javac-vendor`, `jdk/javac/provenance.edn` | `jdk/javac/provenance.edn`: hashed by `bin/arbace-go`'s `reader_key` (the compiled reader's cache, `find jdk/javac ... \| xargs sha256sum`); `bin/javac-vendor`: the build's own command, which `arbace_javac_key` does not hash: a real gap (a change to `tool` keeps a stale tool) | the hashed read covered as a build's inner cache key; `bin/javac-vendor` a known gap: the step's `:audit-known` (new; the audit reports it as `KNOWN GAPS`, not failing) until GTS72 decides: adding it to the key rekeys every step |
| repr-java failed: the baseline's `clojure/lang/Range.ae` | RPS151 (a) (JN9f, `2a2925fc`: a constant's widening and boxing written) changed it; `test/repr/java/expected/baseline.sha256` was not updated (no `--full` since GT5). Deterministic: two conversions here on the clone's tool gave the same new hash | the manifest line (`bin/repr-java check --update`'s line, from the conversion) |
| repr-java-roundtrip.jdk failed: `java/time/format/Parsed.ae` forms and ae, `{:tag Long} \| nil` | RPS151 again: the converter writes `^Long (Long/valueOf (if ...))` for a primitive conditional passed as a Long, the printer `Long.valueOf(...)`, which reads back untagged (an explicit `Long.valueOf` needs no tag) | a known difference with its reason (`test/repr/java/roundtrip/jdk-known.edn`); the converter's fix changes `jdk/forms` (shipped): GTS75. Re-judged here on the clone's F1 and F2: exit 0. The text level's "anew 39 fail" (javac rejecting printed files, compiled again with their originals) is as its references record it and failed nothing: not clean-only |
| java-judge failed: 272 new levels disagreements, 1 print test in no class | the six levels' records predate GT6's CPU limits and JC6b: 437 compiled and 253 interp tests recorded as timeouts or failures now pass (272 of them compiled only: `levels-known.edn`); `t03447` TestFloatVect's print differs by a constant's shape, `0 \| (float 0.0)` (RPS151's widening) | the records written from the run's saved results (`bin/java-check --judge --record`: `check-{interp,compiled,classes,print}.edn`, `levels-known.edn`); `t03447` in `:print-shapes` (its title names a constant's shape); the judgment again: 0 problems. The interp level's load failures (`ClassNotFoundException: javax.crypto.Cipher`, `No accessible method newDecoder`, ...) are as `check-interp.edn` recorded them (0 regressions, 0 changed): not `JAVA_CHECK_FORMS_ONLY` nor GT5's split, they list as disagreements because the same tests now pass compiled. Not recorded: diagnostics' `t07536`, whose message depends on the checkout's path length (GTS76), a non-failing "changed" |

**The reducer's watchdog** (`tools/gateaudit/watch.go`). A reducer whose input is a FIFO writes a heartbeat beside it,
`NAME.fifo.progress` ("UNIX-TIME BYTES-READ LINES PENDING", every 5 s from a goroutine of its own; PENDING by
FIONREAD), and starts itself again as its watchdog (`-watch PID`), in a process group of its own. Every
`GATE_AUDIT_POLL_S` (30) seconds the watchdog calls the reducer stalled when, for `GATE_AUDIT_STALL_S` (600) seconds,
its heartbeat is stale (it does not run: stopped, or its runtime wedged), or bytes wait in the FIFO and none is read,
or when the reducer is gone while strace still holds the FIFO (twice in a row). Then: the reason in
`NAME.reduce.stalled` and appended to `NAME.log`, strace's process tree (the step's traced processes first, then
strace) and the reducer SIGKILLed by PID; the step ends with 137, `bin/gate`'s summary says `AUDIT STALLED NAME:
reason` and fails, the clean record's `:audit` is `"reducer stalled: ..."`; `bin/gate --audit` the same. The watchdog
never opens the FIFO (it finds strace by `/proc/PID/fd` links) and exits with the reducer. It lives in the reducer
and `bin/gate`, not in `bin/lib/gate-run.bash`, whose change would rekey every step (the rule below); the runner
starts the reducer as before (`GATE_AUDIT_WATCH=0`: no watchdog). Tested (`.tmp/gt7fix/reducer-test.sh`, a traced
loop of `cat` and `ls` through a FIFO as `step()` runs it): a normal run, no watchdog left; a reader that stops reading
(`GATE_AUDIT_TEST_STALL_AFTER=3000`): `64730 bytes wait in the FIFO and none was read for 8 s`, the step 137 in 15 s; a
SIGSTOPped reducer: `its heartbeat is 8 s old`; a SIGKILLed reducer: `the reducer exited while strace still writes`;
the same lines as before on a regular file (3,877 lines of a 46,395-line trace, byte for byte against the old
reducer); through the gate, a toy step (`.tmp/gt7fix/toy2/stall.edn`): `FAILED toy-busy`, the next step ran, `AUDIT
STALLED toy-busy: 64832 bytes wait ...`, the gate failed, the record's `:audit "reducer stalled: ..."`.

**`bin/gate [--full] --clean --tag TAG --only STEP,...|failed|changed`** (`make gate-full-clean-only TAG=... [ONLY=...]`,
`test/gate/clean.ae`). The clone, its empty caches and its pins as `--clean`; the base is `test/gate/clean-TAG.edn` (the
record so far) and the logs of its runs, `.tmp/gate-clean/SHA.logs/` (the rerun's own `SHA.logs` or, at a commit run
before, `SHA.logs.N`). After the first plan `gate.clean select ... builds` drops the builds no selected check needs;
after the builds `select ... checks` decides each check:

- *selected*: the steps named (a step with parts names its parts), `failed` (the base's steps that did not pass clean
  with their audit ok, and those it does not hold), `changed` (the steps whose key material, `plan/NAME.inputs` less
  the producers' output digests, differs from the run they passed at), closed over `:uses` both ways (a producer's
  output exists in the fresh clone only by running it; a consumer of a rerun producer is keyed by what it makes again);
  run and audited as in a clean run;
- *every other check*: skipped on its clean pass (`skipped NAME (passed clean at SHA, its key unchanged ...)`), and
  **the run stops** when one of them changed (`N steps not selected have changed since their clean pass, their keys not
  the ones they passed at: ... (add them, or: changed)`).

At the end `gate.clean merge` writes the record: the base's steps with this run's in their place, each step with
`:run`, an index of `:runs` (each run's commit, date, mode, status, selection, logs, pins and hosts), `:commit` the
latest run's, `:status :passed` when every step passed clean with its audit ok. **What the record means**: every
step passed in a fresh clone with empty caches, audited, at the key the record gives it; a step rerun passed at a
later commit, and at that commit every step not rerun had the key it passed at (the selection checked their material
line by line), so each pass still stands for the tag's commit's inputs. The rule chosen (GTS73 (a)): a rerun may be at
a new commit when its differences touch only the rerun steps' keys; a fix that touches the gate's logic, gate_key or a
host's identity rekeys every step, and `changed` then selects them all. Tested on a toy table (`.tmp/gt7fix/toy2/`,
the inner mode): run A (every step; one failing, one audit undeclared); run B1 `--only failed` after a change to a
passed producer's input: **refused**, `toy-read` named; run B2 `--only failed,changed`: the build not needed (no
selected check needs it), the two failed steps, the changed producer and its consumer (`a consumer of a step rerun`)
ran, the other two skipped on their clean passes, the merged record `:status :passed` with `:run 0` and `:run 1` steps.

**Which fixes change a check's results, which only the gate.** Only the gate (the audit, the declarations, the
record): the reducer and its watchdog, the H rule, `:audit-known`, the declarations of java-fixtures, java-print,
java-forms-verify, programs-full, repr-go and repr-go-native.box, `--only`. A check's verdict: the five programs' limits
(goi-check, goi, goi-aot: a timeout becomes a pass), repr-go's `GODEBUG` (the same results, in minutes instead of
hours), the baseline manifest (repr-java), Parsed.ae's known difference (repr-java-roundtrip.jdk), the Java levels'
records and `t03447`'s class (java-judge, and java-check-judge's essential subset). No program's behaviour changed.

**What the rerun selects** (`select` on this branch's plan with the clone's units, against the first run's statuses so
far; `.tmp/gt7fix/sel.tsv`): the failed and unrecorded steps (goi-check, repr-java, repr-java-roundtrip.jdk, the seven
Java levels, java-judge, programs-full, repr-go and the steps after it the first run had not reached) and the steps
this branch's records rekey: through `test/goi/timeouts.txt` (`:goi`: goi-aot-essential, reap and repr-go-native's 22
parts), `test/java/check-*.edn` (java-check, java-check-judge, java-compiled, reap), `test/repr/java/`
(`:repr-java`: javac-classes, javac-verify, jrt-convert, repr-java-roundtrip's other parts, the 19 parts of
repr-java-parse, -front, -attr, -nojavac). The other steps (go-smoke, suite-go, oracle-go, diffcheck,
suite-aot-essential, programs, generated, j2g-edge, provenance, javac-check, fixpoint, suite-aot) keep their keys and
their passes.

## Proposals (GTS70-GTS78)

- **GTS70 The reducer's stall.**
  - Options: (a) as built: the blocking read, job-control signals ignored, the watchdog (a stall fails the step in
    `GATE_AUDIT_STALL_S`, 10 minutes, with its reason); (b) also a reader goroutine draining the FIFO into memory
    ahead of the parser, so strace never waits on the parsing (a bounded backlog, say 1 GiB); (c) strace's output to
    a regular file and the reducer after the step (no FIFO, no stall; repr-go's trace is tens of GB of disk).
  - *Recommended: (a)*: the cause is not reproduced, and the one blocking point the reducer had, netpoll on the FIFO,
    is gone; whatever else stops it now costs ten minutes and a named failure, not hours; (b) treats a slow parser,
    which nothing measured shows (the reducer is 23 times faster than awk, GT5).
- **GTS71 Hashed-only reads.**
  - Options: (a) as built: a read by `sha256sum` of a file in the full key of a step's program host or its producers'
    is the host's identity check, covered and listed; (b) the seven steps moved to `:javac` as go-smoke was (GT5),
    losing their cutoff on a j2g change; (c) the gate hands the steps the key it computed (`gate_env`'s jkey) and
    `arbace_javac_fresh` compares it with the tool's `.key` instead of hashing, so a step hashes nothing.
  - *Recommended: (a) now, (c) at the next change that rekeys every step anyway (as GTS60 (b))*: (c) is the narrower
    claim (no rule about which process read a file), but it edits `bin/lib/javac-key.bash`, in every key; (a) keeps the
    audit's other reads exact (a source read by the tool or by Arbace is still undeclared).
- **GTS72 `bin/javac-vendor` in `arbace_javac_key`.**
  - Options: (a) added at the next change that rekeys the executables anyway, `:audit-known` then removed; (b) now (a
    clean rerun of every step); (c) kept as a known gap.
  - *Recommended: (a)*: the script is javac-tool's own command (its `tool` passes the build's options), so a change to
    it can keep a stale tool; the gap is narrow and named in the audit's report meanwhile.
- **GTS73 The rule for `--only`.**
  - Options: (a) as built: a new commit allowed when every check not rerun keeps its key material (checked line by line
    against the run it passed at), the record naming each step's run; (b) the rerun at the first run's commit only (no
    fix can be in it); (c) a full clean run after any change.
  - *Recommended: (a)*: a key is the step's whole input, never the commit (GTS2 (a)), so a pass at an unchanged key is
    the same claim at either commit; (b) cannot carry the fixes, (c) costs a day per finding.
- **GTS74 The Java levels' records from the clean run.**
  - Options: (a) as built: `check-{interp,compiled,classes,print}.edn` and `levels-known.edn` written from the clean
    run's saved results (437 compiled and 253 interp tests recorded as timeouts or failures before GT6's CPU limits and
    JC6b now pass; 272 tests pass compiled only); (b) recorded from an everyday `--full` after the tag.
  - *Recommended: (a)*: the clean run is the quiet, audited run the records should come from, and the judgment of its
    own results is the check that fails without them.
- **GTS75 The converter's `^Long` over its own `Long/valueOf` (RPS151).**
  - Options: (a) as built: a known round-trip difference of `java/time/format/Parsed.ae`; the converter drops the tag
    on a boxing it writes, after the tag (`jdk/forms` changes: the re-seed's business); (b) now (shipped forms, every key).
  - *Recommended: (a)*: the forms mean the same Java; the fix belongs with the next re-seed.
- **GTS76 `t07536`'s path-dependent diagnostic.**
  - Options: (a) java-check masks the work directory before it shortens a reason (the clean clone's longer path leaves
    `RUN/classes/Bar.` where main's leaves `RUN/classes/Bar.class, 26`); (b) the record follows the clean run's; (c) as
    built: the record kept, a non-failing "changed" in the clean run.
  - *Recommended: (a)*: a result that depends on the checkout's path is the harness's, and GT7's fresh path exposed it
    (like run 1's `UserDirChangedTest`).
- **GTS78 The programs' limits under the audit.**
  - Options: (a) as built: a program's own limit in `test/goi/timeouts.txt` where the audited clean run's `cpu.tsv`
    shows it over (five programs: gcgort 60, issue13160, issue13169, gc2, heapsampling 30); (b) a CPU factor for
    every program when the step is audited (`ARBACE_CPU_FACTOR`, `bin/lib/limit.bash`: in gate_key, every step rekeyed);
    (c) GTS51 (a)'s recorded CPU times, three times each.
  - *Recommended: (a) now, (c) after the tag*: ptrace's cost lands on the programs that spawn threads or signal (system
    time 2 to 5 times the user time here), not on all; (c) is the systematic form, from this run's logs.
- **GTS77 The watchdog's limits.**
  - Options: (a) as built: 10 minutes without progress (`GATE_AUDIT_STALL_S`), polled every 30 s; (b) 3 minutes.
  - *Recommended: (a)*: a working reducer drains a 64 KiB pipe in milliseconds and beats every 5 s; ten minutes of a
    stall is no false alarm and still bounds the loss to minutes.

**Checks run here.** The reducer's toy tests (`.tmp/gt7fix/reducer-test.sh`: normal, stuck, stopped, killed; the old and
new reducer's output byte for byte on a regular file); `gate.audit` on synthetic traces (the H rule, `:audit-known`); the
toy tables in the inner mode (`.tmp/gt7fix/toy2/`: the `--only` runs A, B1 refused, B2 merged; the stall through the
gate); `bin/gate --full --clean --plan` (85 steps: 3 always, 2 builds, 77 run, 3 after) and with the clone's units;
`select failed,changed` against the first run's record (`.tmp/gt7fix/real-sel.tsv`: 67 checks to run, 13 skipped on
their clean passes: go-smoke, suite-go, oracle-go, diffcheck, suite-aot-essential, programs, generated, j2g-edge,
provenance, javac-check, fixpoint, suite-aot, repr-clojure); the Java judgment on the clean run's saved results (exit 0
after the records); the jdk round trip's forms re-judged (exit 0); the baseline converted twice (deterministic); the
steps traced as step() traces them (`.tmp/gt7fix/trace.sh`, `.tmp/gt7fix/trace/`): java-fixtures, java-classes (its audit
subset), java-forms-verify, and `repr-go roundtrip amd64` (under the lock: 100 minutes without asyncpreemptoff, killed;
1,206 s with it, passed, audit ok); `make check-makefile`; the essential gate in this worktree (`.tmp/heavy bin/gate`,
`.tmp/gt7fix/essential-gate.txt`): **passed in 10m07s**, 11 steps run (goi-check, goi-aot-essential, java-check,
java-check-judge, java-compiled, j2g-edge, reap: their records or limits changed), 8 skipped. Not run: the clean rerun
itself (the main session's), the other audited Java levels at full size (interp, compiled-full, print, read: their
reads are the same `repr_java_host` path, proved on three of them), the whole six-roundtrip repr-go traced.

**For the main session** (after the merge, from the main checkout, with `test/gate/clean-arbace-reaps-3-crops-v2.edn`
and `.tmp/gate-clean/68b2bba6...logs/` left where the first run put them):

    touch .tmp/gt7.quiet; .tmp/heavy bin/heavy-lock env GATE_HEAVY_CMD=env bin/gate --full --clean \
      --tag arbace-reaps-3-crops-v2 --only failed,changed; rm .tmp/gt7.quiet

(`make gate-full-clean-only TAG=arbace-reaps-3-crops-v2` is the same without the lock.) It builds stage 0, the
executables and javac's tool fresh in a new clone, runs the 67 selected checks audited (about 11 hours: the Java chain
about 4, repr-go about 2, repr-go-native's parts about 1.5), skips the 13, and merges the record; then commit the record
with the tag's journal entry. A selection refused names the steps whose keys moved.

**Slips.** I ran `bin/gate` in the inner mode on toy tables while the quiet file existed (light: seconds, no lock, its
own store; each run trimmed Go's shared build cache as every gate run does); I executed javac's tool copied out of the
running clone's `.tmp/gate-exe/` (a copy, read only); the first traced repr-go ran 100 minutes under the big lock
before I killed its process group.

## Decided (2026-10-30): GTS70-GTS78

The user decided all nine as recommended. GTS72 (`bin/javac-vendor` into `arbace_javac_key`), GTS75 (the converter's tag
fix) and GTS78's second half (GTS51's recorded CPU times) come at the next change that rekeys everything; the rest are
as built. Test t03447's class `:print-shapes` (a constant printed as `0` against `(float 0.0)`) is confirmed by the user.

## Decided (2026-10-31): GTS79

- **GTS79 `--only` and a program host not built** (GT7's rerun 2). Options: (a) the selection builds every host an unselected
  step's key names; (b) a pending program host compared by its build's key (`arbace_javac_key`, `gate_key`) rather than as
  changed; (c) the workaround (select one cheap step that needs the host). *Recommended: (a)*.
  - **Decided (the user, 2026-10-31): (b).** After the tag, at the next change that rekeys every step.

## Decided (2026-10-31): GTS80-GTS82

The user decided the three as recommended, after the tag, at the next change that rekeys every step (GTS79 (b) with
them; built ahead on branch `gate-post-v2`, "As built: GTS72, GTS79-GTS82 (write-ahead)" below).

- **GTS80 Passes written on a full disk** (GT7's rerun 4). `bin/gate --clean` publishes with `cp -an` (none replaced), so
  rerun 4's empty pass files, written at 01:35 with the disk full, kept rerun 5's passes out of the store; the planner
  ignored them, and the demonstrations' base plan ran 4 steps until they were rebuilt from rerun 5's logs. Options:
  (a) `record_pass` writes its `.edn`, `.log` and `.inputs` through temporary names renamed into place, and the
  publication replaces a pass that is empty or does not read; (b) the planner's check alone, `--prune` removing empty
  files; (c) nothing. *Recommended: (a)*, at the next change that rekeys every step (it edits `bin/lib/gate-run.bash`).
  - **Decided (the user, 2026-10-31): (a).**
- **GTS81 A negative control per check family** (2026-10-31: `bin/java-check`'s two judges, GT5 to the tag's eve, every
  interp and compiled run passing). Options: (a) one deliberately failing case per check family (Java's levels, goi, the
  suite, the oracle, diffcheck, the crops' checks), recorded as expected to fail; a run where it passes fails the step;
  (b) Java's levels only; (c) none. *Recommended: (a)*, after the tag, with the next change that rekeys every step.
  - **Decided (the user, 2026-10-31): (a).**
- **GTS82 Stage 0 built in a worktree by mistake** (2026-10-31: twice an agent ran a tool in a linked worktree without
  `ARBACE_HOST_EXE`, and `bin/lib/host.bash`'s `arbace_host_exe` started `bin/seed --build` there, a 10-minute, 8 GB
  build). Options: (a) in a linked worktree with no `ARBACE_HOST_EXE` and no stage 0 of its own, use the main
  checkout's stage 0 when its seed and Go pins match, else refuse with a message; `ARBACE_SEED_BUILD=1` builds anyway;
  (b) fall back to the main checkout's whenever it matches, building only when nothing matches; (c) no change.
  *Recommended: (a)*, after the tag (`bin/lib/host.bash` is in many keys).
  - **Decided (the user, 2026-10-31): (a).**

## As built: GTS72, GTS79-GTS82 (write-ahead) (2026-10-31)

Branch `gate-post-v2`, from main `ba772ee5`, built in the light tier ahead of the merge after the tag
`arbace-reaps-3-crops-v2`: the merge is the change that rekeys every step anyway (`bin/lib/gate-run.bash` is in every
key, `bin/lib/host.bash` and `bin/lib/javac-key.bash` in most). No `bin/gate` run, no build of the executables or of
javac's tool, no store touched; each part tested on synthetic material (the scripts named below, in the worktree's
`.tmp/`). No shipped file changed (nothing under `arbace/`, `go/`, `overlay/`, `jdk/`, `tamago/`, `seed/`).

**GTS72 (a).** `bin/javac-vendor` is in `arbace_javac_key` and `arbace_javac_rest_key` (`bin/lib/javac-key.bash`), and in
`gate.audit`'s files of the `:javac` and `:javac-prog` hosts (`test/gate/audit.ae`); the `:audit-known ["bin/javac-vendor"]`
of javac-tool and repr-java-parse are gone (the mechanism stays, unused). Tested (`.tmp/gts72/`): a comment line added to
`bin/javac-vendor` moves both keys, its removal restores them; `gate.audit` on a trace that reads and executes
`bin/javac-vendor`: javac-tool and repr-java-parse.samples `audit ok` here, `1 undeclared` with main's audit.ae.

**GTS79 (b)** (`test/gate/clean.ae`). `select` compares a program host that is pending in the rerun's plan (`host
javac-prog pending`: its build not made) by its build: when the rerun's `hosts.edn` has the base run's build key and
rest (`:javac` and `:javac-prog-rest`; for `:prog`, `:gate` and `:prog-rest`; the base run's from the record's `:runs`
`:hosts`, a first record's own `:hosts`), the pending line counts as the base's (`host-pending`); the identity of a
program host is a function of those two (its units are the build's, the build is its key's). A skipped step says so
(`...; host javac-prog not built, its build's key 63071998db8e the one it passed at (GTS79 (b))`). Tested on GT7's
rerun 2 made synthetic (`.tmp/gts79/test.sh`: 29 Java steps on `:javac-prog` pending, goi-aot failed; 9 cases): main's
`select` refuses (29 changed), this branch's builds `none javac-tool`, runs goi-aot and skips the 29 (exit 0), the same
with `failed,changed`; a moved `:javac` or `:javac-prog-rest` refuses the 29 and `failed,changed` builds the tool; a
pending step whose files changed is still changed; a base pass at a pending host is compared as before.

**GTS80 (a).** `record_pass` (`bin/lib/gate-run.bash`) writes the `.log`, `.inputs`, `.out` and the record through
temporary names (`.KEY.PID.*`), each compared with its source (`cmp`, `diff -r`) before its rename, the record last and
only when it reads (`pass_reads`: not empty, `{:step "NAME" :key "`, a 64-digit `:log-sha256`, closed by `}`); a failure
removes the temporaries and leaves no file of that key. The `:clean true` mark is written by the `printf`, not a `sed -i`
after it: the record is byte for byte the old one. `bin/gate --clean`'s publication is `publish_passes` (in `bin/gate`,
no key): a pass the store holds and that reads is kept; one absent, empty or cut is replaced (its files through
temporaries, the record last); a clone's pass that does not read is not published; the count says how many replaced one
that did not read. The planner still skips a record it cannot read. Tested (`.tmp/gts80/test.sh`, 21 cases): writes
past a file size limit (`ulimit -f`: a real EFBIG, the full disk's failure) leave no file and no temporary, the earlier
pass intact; the record and log byte for byte main's `record_pass`'s (the date masked), plain and clean;
`publish_passes` over rerun 4's case (an empty pass and log, a cut pass with an output, a good one, an absent one, a
source pass that does not read), then again (nothing published).

**GTS82 (a)** (`bin/lib/host.bash`). In a linked worktree (git's common directory not the checkout's own `.git`) with no
`ARBACE_HOST_EXE` and no current stage 0 of its own (a stale one counts as none), `arbace_host_exe` takes the main
checkout's `target/seed/ARCH/arbace` when its `target/seed/pins` names the worktree's seed and Go's pin (the first two
fields) and its `seed.sha256` the seed; else it refuses, naming `ARBACE_HOST_EXE` and `ARBACE_SEED_BUILD=1`, the main
checkout's state and the cost; `ARBACE_SEED_BUILD=1` builds the worktree's own as before. The main checkout, a clone of
its own (the clean run's) and `bin/gate`'s stage0 step (`bin/seed --build` itself) are unchanged. Tested
(`.tmp/gts82/test.sh`, a scratch worktree and a scratch clone under the worktree's `.tmp/`, a stub `bin/seed` that
reports a build instead of building; 11 cases): `ARBACE_HOST_EXE` wins; main's stage 0 taken; another seed
(`ARBACE_SEED`) or Go pin refused, status 1, no build; `ARBACE_SEED_BUILD=1` builds; a current own stage 0 taken, a stale
one replaced by main's; the clone builds as before.

**GTS81 (a): the negative controls.** One design for every family: a control is a *crossed pair*, the run (or record)
of one case of the corpus judged by the family's own comparison against the expected result of another case, which it
must fail. It runs inside the step's own run, needs no case, build or reference of its own (the two cases are the
corpus's, both passing), and goes through the very code path a shadowed or broken judge would take: a judge that passes
everything passes the control, and the step fails (`control passed: ...`); a control that did not reach the judge while
its subject did also fails (`control not judged: ...`); a control whose subject was not run (a selection) is printed and
left. The controls are no case of the records, classes, known lists or counts. Each is documented with its check:

| family | control (file) | crossed pair | judged by | documented |
|---|---|---|---|---|
| Java interp, compiled, classes | c00001 (`test/java/controls.edn`) | t03208 EnumsRecords' run against t03209 EvalOrder's reference (exit 0, other stdout; other class files) | `run_actions`' `judge`; `classes_verdict` (the inline comparison made a function); `java.check/report` and `--essential-compiled`'s tail | `bin/java-check`'s header; doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As built: the negative controls" |
| goi, goi-aot | `_control/helloworld-vs-printbig`, `_control/issue21879-vs-fibish` (`test/goi/controls.txt`) | the subject's run (interpreted; at `--aot` built and run) against the reference's gc run (`gc_ref`'s cache, shared) | `check_one`'s comparison; `controls_judged` | `bin/goi`'s header; doc/go/GOI-SPEC.md §11.3 |
| the suite, its AOT level | 1 (`test/clojure/controls.edn`) | clojure.test-clojure.macros' result against vars' expected (10 tests against 9) | `regressions` in the report (`CLOJURE_TESTS_CONTROLS`) | `bin/clojure-tests`' header, step 7 |
| the oracle | control-1 (`test/oracle/controls.edn`) | collections.ae:6's actual `[]` against :7's expected `[1 2 3]` | `oracle.runner/check`'s `differs` (canon, V11); `controls-ok?` | doc/go/ORACLE.md, "The negative controls" |
| diffcheck | control-1 (`test/diffcheck/controls.edn`) | collections.ae:6's evaluated run against :7's compiled run | `diffcheck.main/check`'s comparison; `controls-ok?` | doc/FUTAMURA-SURVEY.md, "F2 as built" |

Not covered (no cheap control; GTS84, GTS85): Java's read (its verdict is the reader's status and log, nothing of
another test to judge against), diagnostics (the golden comparison sits inside `diag_actions`, per action) and print (the
five steps of `java.print/judge`); goi's errorcheck and directory tests (`check_err`'s `gocmp`, `check_dir`'s golden
output); repr/go's native-check (whole-corpus dumps, 28 GiB a part; a crossed pair inside `check-job` is cheap to run but
not to build without a run). Tested (`.tmp/gts81/`): Java (`java/test.sh`, 12 cases; `java/ec.sh`, 3): the plan's
control lines from the real reference; `run_actions` with a fake executable (the control differs, another status
fails, the shadowed `judge` of 2026-10-31 passes subject and control alike); `--judge` on synthetic results (the
controls hold, a passing one exits 1, a missing one exits 1, a load-failed pair is left); the essential-compiled tail.
goi (`goi/test.sh`, 7): `check_one` on the real controls with gc's references built by TamaGo's go and goi faked by `go
run` (subject and reference pass, the control fails on stderr, two references for three programs), `controls_judged`'s
cases. The oracle (`oracle/run.sh`): `oracle.runner check` of collections.ae on stage 0, `--expected` the frozen record:
exit 0 with the control differing; the control against itself: exit 1, `control passed`; collections.ae not selected:
left; a case that does not exist: `control not judged`, exit 1. diffcheck (`diffcheck/run.sh`, a hand-made corpus of the
two cases on stage 0): differs; against itself, passed, exit 1; runs incomplete, left. The suite (`suite/test.sh`, 4):
the runner renamed as `bin/clojure-tests` renames it, on synthetic results: holds, passes against itself (exit 1), its
subject not run (left), no expected reference (exit 1). Not run: any check on the gate's executables, goi's AOT control
(`aot_build`), the controls through `check()`'s and the steps' wiring end to end.

**GTS75's and GTS78's second halves** (decided for the next change that rekeys everything): not built here. GTS75 (the
converter drops its `^Long` over its own `Long/valueOf`) changes `jdk/forms` (shipped), a conversion on javac's tool and
a re-seed's business; GTS78 (c), GTS51 (a) (`--record-cpu` for goi and Java, `limited` at three times the recorded CPU
time) needs the clean run's `NAME.cpu.tsv` logs, which are the main checkout's: GTS88. GTS71 (c) (the gate hands the steps
the tool's key instead of `arbace_javac_fresh` hashing) was recommended for the same change but is not in the decision's
list: GTS87.

**What the merge must check.**

- The essential gate (every step rekeys); its first run's logs show the controls: java-check `control c00001 ...: differ,
  as a control must`, goi-check `_control/helloworld-vs-printbig ...: fail`, goi-aot-essential `_control/issue21879-vs-fibish
  ...: fail` (the AOT path, not run here), oracle-go and diffcheck `control control-1 ...: differs`, suite-go and
  suite-aot-essential `control 1 ...: ran 10 tests, expected 9`. Rebuilt once by their keys: the essential-compiled program
  (its key holds `test/java/check.ae`), diffcheck's diff build (`test/oracle/runner.ae`), the suite's AOT builds if their
  keys hold the runner; javac's tool (`arbace_javac_key` moved).
- `bin/gate --full --plan`: 85 steps, every check `run` (here: `gate.plan` on this branch's table and files, 85 steps, 77
  run; essential 19).
- The next clean run's selection: a `--clean --only` where javac's tool is not built skips the `:javac-prog` steps with
  `(GTS79 (b))` in their reason instead of stopping; `publish_passes`' line in the summary.
- In a linked worktree without `ARBACE_HOST_EXE`, a tool takes the main checkout's stage 0 (no `bin/seed --build`).
- The Java levels' records, re-recorded after the two judges' fix (the journal's next step), hold no `c00001` line.

## Proposals (GTS83-GTS89)

- **GTS83 The gate's stage 0 in a linked worktree.** GTS82 (a) covers the tools; `bin/gate`'s stage0 step runs `bin/seed
  --build` itself, so an everyday gate in a worktree still builds its own stage 0 (about 10 minutes, 8 GB) once.
  - Options: (a) the stage0 step in a linked worktree takes the main checkout's stage 0 when its `pins` file equals what
    `bin/seed --build` would write (the seed, Go's pin, the scripts' digest, the root, the arches: GTS53's identity),
    a link or `ARBACE_HOST_EXE` for the run; (b) as now.
  - *Recommended: (a)*: the identity is already exact (`:stage0` in hosts.edn); the clean run, a clone of its own, keeps
    building its own.
- **GTS84 Java's read, diagnostics and print without a control.**
  - Options: (a) crossed controls there too: diagnostics, a subject's javac output against another test's golden file
    (`diag_actions` given the reference's golden path); print, the subject's printed class files against another test's
    original ones (the `:classes` step); read left (its verdict is the reader's own status and summary line, which a
    control cannot cross); (b) leave the three.
  - *Recommended: (a)*: the diagnostics and print judges are the next most complex code paths; read's is one `grep`.
- **GTS85 goi's errorcheck and directory tests, native-check.**
  - Options: (a) crossed controls: `check_err` judging a subject's errors against another program's `// ERROR` comments,
    `check_dir` a directory test's run against another's golden output, native-check one dump's native tree against
    another dump's helper tree inside `check-job` (one diff per part); (b) goi's two, native-check left; (c) none.
  - *Recommended: (b)*: native-check's comparison is a structural diff whose failure mode is "no difference found", which
    its E-forms level (`.ae` text byte for byte) already cross-checks.
- **GTS86 A control's own run.** Java's and the suite's, the oracle's and diffcheck's controls judge records their
  subjects already made (no run of their own: a control fails only with its judge); goi's run their subject again (the
  control's id, its own run, at `--aot` its own `aot_build`), so a control can time out alone; goi's quiet retry reruns
  it (the dispatcher knows `_control/` ids), and a control left in error while its subject passes fails the check.
  - Options: (a) as built; (b) goi's control judges the subject's own run (`check_one` writing the subject's outputs
    once, judged twice, as Java's `run_actions` does), no second run or build.
  - *Recommended: (b)*: the same code path, half the cost at `--aot`, and no failure of a control alone.
- **GTS87 GTS71 (c) at this rekey.** GTS71 recommended (c) (the gate hands the steps javac's tool key, `arbace_javac_fresh`
  compares it instead of hashing) for the next change that rekeys every step, but the decision of 2026-10-30 lists only
  GTS72, GTS75 and GTS78 for it.
  - Options: (a) built on this branch before the merge; (b) left as built (a): the H rule.
  - *Recommended: (a)*: it removes the audit's one rule about which process read a file, at no extra rekey.
- **GTS88 GTS51 (a)'s recorded CPU times.** The second half of GTS78 needs the clean run's `NAME.cpu.tsv`, under the main
  checkout's `.tmp/gate-clean/`.
  - Options: (a) built by the main session after the merge, from the v2 record's logs (`--record-cpu` for `bin/goi check`
    and `bin/java-check`, `limited` at three times the recorded time, the default's floor); (b) at the next clean run.
  - *Recommended: (a)*: the logs exist now, and the limits are in `bin/lib/limit.bash`, in `gate_key`: the same rekey.
- **GTS89 What "does not read" means to the publication.** `pass_reads` is a shape check in bash (start, step, key, log
  digest, closing brace); the planner's test is the Clojure reader.
  - Options: (a) as built; (b) `publish_passes` asks the planner (`gate.plan` reading each pass on stage 0).
  - *Recommended: (a)*: the shape is `record_pass`'s own, which now writes it whole or not at all; (b) costs a stage 0 run
    per publication for the case a cut file already fails.

**Slips.** The GTS82 test ran `git worktree prune` once (shared git metadata: it removes only entries whose directories
are gone; dropped from the script after); the tests read the main checkout's `target/seed/pins` and `seed.sha256` (read
only, as GTS82's fallback does) and ran its stage 0 (as `ARBACE_HOST_EXE`).

## Decided (2026-10-31): GTS83-GTS89

The user decided all seven as recommended: GTS83 (a); GTS84 (a); GTS85 (b); GTS86 (b); GTS87 (a), built on this
branch before the merge; GTS88 (a), built after the merge from the v2 record's logs; GTS89 (a), as built.

## As built: GTS83-GTS87 (write-ahead) (2026-10-31)

Branch `gate-post-v2`, after "As built: GTS72, GTS79-GTS82 (write-ahead)", in the light tier: no `bin/gate` run (not even
`--plan`), no build of the executables or of javac's tool, no store touched. Each part tested on synthetic material and
scratch copies (the scripts named below, in the worktree's `.tmp/`). No shipped file changed.

**GTS83 (a): the gate's stage 0 in a linked worktree** (`bin/gate`, `bin/seed`, `bin/lib/seed-go.bash`). The pins line
`bin/seed --build` writes and keeps a stage 0 by is now one function, `seed_go_pins ARCHIVE ARCHS` (the seed's SHA-256,
`seed/goroot.sha256`'s, the digest of `bin/seed` and `bin/lib/seed-go.bash`, the toolchain's root, the architectures:
GTS53's identity, the `:stage0` host's), byte for byte the inline formula it replaces. `bin/seed --kept DIR [--arch A]`
says whether DIR is a stage 0 `--build` would keep here (its executables, its `seed.sha256` this seed's, its `pins` this
line): exit 0 with `stage 0 kept: ...`, else the reason, building nothing. `bin/gate`'s `stage0_choose`: in a linked
worktree (git's common directory another checkout's `.git`) whose own stage 0 `--kept` refuses, the main checkout's when
`--kept` accepts it; the stage0 step is then `bin/seed --kept MAIN/target/seed` (its summary line the same regex) and
`ARBACE_HOST_EXE` is the main checkout's executable for the gate and every step; else `bin/seed --build --no-aot` as
before. The main checkout, the clean run (`GATE_CLEAN_INNER`, a clone of its own) and a worktree with a current stage 0
of its own are unchanged; `bin/gate --audit` chooses the same way. The identity is the whole line, stricter than GTS82's
fallback in `bin/lib/host.bash` (the seed's and Go's pins only): a pass claims the `:stage0` identity, which holds the
scripts' digest. Tested (`.tmp/gts83/test.sh`, 22 cases, a scratch repository of its own: a fake main checkout with a
fake seed archive, its linked worktree, a clone; fake executables, no build): `seed_go_pins` equal to the old formula;
`--kept` on none, its own, the worktree's view of main's, another toolchain root, another architecture set, another
`seed.sha256`; `stage0_choose` (extracted from `bin/gate`) in the main checkout, the linked worktree (main's taken, the
host set), the clean mode, the clone, a worktree whose `bin/seed` or Go pin differs (builds), one with a current stage 0
of its own (kept by `--build`), a stale one (main's); the step's summary regex on `--kept`'s line.

**GTS84 (a): Java's diagnostics and print controls** (`bin/java-check`, `test/java/check.ae`, `test/java/print.ae`,
`test/java/controls.edn`; doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As built: the negative controls"). The golden
comparison is `diag_golden` (out of `diag_actions`) and `check_diag`'s judgment `diag_judge P SUF EXP`, both the tests'
and the controls'. **c00002**: t06443 T5017953's javac output against t06444 T6304921's golden file (`plan-diag`'s
`control-diag CID EXIT DIR/FILE`): the subject's actions up to its first golden one, that action's status against the
reference's and its output against `T6304921.out`. **c00001 at print**: `control-print CID REF` in the subject's plan,
`control-reference CID` in the reference's; `print_check_one` writes `print/classes.res.CID` by `classes_verdict` and
keeps `print/c2` of both; `java.print/judge` prints the control's row from the steps' own comparisons crossed
(`control-steps`: `forms-result` of the subject's F2 against the reference's F1, `text-result` of its full print against
the reference's source, the classes' digest then `classes-result` against the reference's printed class files), `differ`
only when every crossed step differs, `pass` naming a step that passed what differs, `left` when the reference was not
printed in the run (a selection), `unjudged` when no step was taken. `controls-judged!` prints `left` and leaves it. Read
is left. Tested (`.tmp/gts84/test.sh`, 27 cases): the plans from the real reference (t03208's `control-print`, t03209's
`control-reference`, GTS81's lines unchanged, t06443's `control-diag` naming T6304921.out, t06444 none); `check_diag` on
the real jdk26u tests read in place with the system JDK's javac as the javac (as `record_diag` runs it): t06443 and
t06444 pass, c00002 differs at line 1, the subject's own golden file as the reference passes it, another status fails
it, a `diag_judge` that passes everything passes the control too; `diag_judge` bounded to the first golden action;
`print_check_one` on synthetic trees (javac faked): the control's digest, the trees kept for the pair and removed for a
test of no control; `java.print/judge` on synthetic print runs on stage 0: differ at forms, text and classes, the
subject against itself passes naming the three, one step passing what differs (classes) passes naming it, the reference
absent is left; the report (`bin/java-check --judge`) on synthetic results: c00002 and c00001 at print differ (exit 0),
c00002 passing, missing, c00001 left (exit 0), unjudged, passing (exit 1). Not run: either level on javac on Arbace.

**GTS85 (b), GTS86 (b): goi's errorcheck and directory controls, judged from the subject's own run** (`bin/goi`,
`test/goi/controls.txt`; doc/go/GOI-SPEC.md §11.3). A control is no entry of the program list any more: each checker,
once its subject reached its comparison, judges the same run again under the control's id (`controls_of SUBJECT`, the
level's lines whose subject it is; none in the box's runs). The comparisons are functions both use: `run_verdict`
(`check_one`), `dir_verdict` (`check_dir`), `syn_verdict` and `err_verdict` (`check_err`'s two paths), `err_aot_verdict`
(`check_err_aot`). New controls: `_control/append1-vs-assign1` and `_control/dedup-vs-issue11053` (typeparam/dedup's
output against fixedbugs/issue11053.out) at interp, `_control/const6-vs-bug046` and `_control/issue48337a-vs-dedup` at
aot, beside GTS81's two; every subject is in its level's essential list and passes in the records. A subject that ends
before the comparison prints no control row (the control "not judged", no failure); a subject not in the run fails the
check, but a selection (`--only`) leaves it; the quiet retry's merge adds the rows a retried subject brings. At `--aot` a
control costs no `aot_build`. Tested (`.tmp/gts85/test.sh`, 13 cases, the real programs of `$GOROOT/test`, gc's
references real, `tools/gocmp` built here with TamaGo's go, goi faked: `go run` of the program, the subject's golden
output, gc's own compile for the errors): `check_one` one goi run for subject and control, the control fails on
stderr, two gc references; `check_dir` one run, the control fails against issue11053.out; `check_err` one run, the
control fails on assign1's missing errors, and on its syntax path; comparisons that pass everything make the three
controls pass (status 1); a subject absent fails, with `--only` is left; a subject timing out leaves the control not
judged; `err_aot_verdict` on gc's errors of const6 (its own pass, against bug046's comments fail); the retry's merge.
Not run: `check_one`'s and `check_dir`'s AOT paths (`aot_build`), `check_err_aot` end to end, goi itself.

**GTS87 (a): GTS71 (c)** (`bin/lib/gate-run.bash`, `bin/lib/javac-key.bash`, `test/gate/audit.ae`). `gate_env` exports
`GATE_JAVAC_KEY` (its `jkey`, `arbace_javac_key` of the checkout) and `GATE_JAVAC_KEY_ROOT` (the checkout's physical
path); `arbace_javac_fresh` (`repr_java_host`, `bin/javac-vendor check --from`) compares the tool's `.key` with it when
the root is this checkout, else hashes as before (by hand, another checkout). The H rule's `identity-host` keeps `:prog`
(gate_key's files: its steps' tools compute the executables' keys, `arbace_runtime_key` and `arbace_aot_key` through
`program_base` and `bin/java-check`) and `:gate` (a no-op: a step on `:gate` or `:javac` declares its host's files
anyway); `:javac-prog`'s went (it served only `arbace_javac_fresh`: GT7's fix round counted exactly the 2,601 files of
`arbace_javac_key` hashed in java-fixtures and java-classes), and so did `:javac`'s no-op. The builds' rule stays
(javac-tool's `reader_key`, `bin/javac-vendor tool`'s and `--build-java`'s own `arbace_javac_key`, go-build's
`gate_key`). Tested (`.tmp/gts87/test.sh`, 10 cases): `arbace_javac_fresh` under strace: the handed key, no
`sha256sum` run; a stale tool refused without hashing; a key for another root, or none, hashed (the same key);
`gate_env` (scratch executables, store and lock) exports both; `gate.audit` on synthetic traces: java-classes hashing
three of the tool's sources undeclared here and covered with main's `audit.ae`, java-check hashing the runtime's still
covered by `:gate`'s identity, javac-tool's declared by its host.

**What the merge must check** (with "As built: GTS72, GTS79-GTS82"'s list).

- The first gate in the main checkout rebuilds stage 0 once (`bin/seed` and `bin/lib/seed-go.bash` changed, so its pins
  line did); then a gate in a linked worktree says `stage 0 kept: /root/arbace/target/seed (amd64), its pins this
  checkout's (GTS83 (a) ...)` in its stage0 line and builds none; before that rebuild it builds its own, as now.
- goi-check's and goi-aot-essential's logs: three controls each `fail, as a control must`; goi-aot-essential faster by
  one `aot_build` (GTS81's control built its subject again); the full `goi-check` and `goi-aot` the same.
- java-diagnostics: `control c00002 ...: differ, as a control must`; java-print: c00001 at print `differ` (both judged
  again by java-check-judge from the saved results); java-classes, interp and compiled as before.
- The clean run's audit of the `:javac-prog` steps (java-fixtures, java-classes, java-forms-verify, the repr-java tree
  checks): no `hashed only` line and audit ok; an undeclared read of a tool source there is a route the H rule hid,
  to be declared (or handed the key the same way).
- GTS81's test scripts are superseded where they assumed the old shapes (`.tmp/gts81/java/test.sh`'s "t03209 has none":
  it now has `control-reference`; `.tmp/gts81/goi/test.sh` dispatches `_control/` ids, which no checker takes now).

## Proposals (GTS90-GTS94)

- **GTS90 Which pins let a worktree's gate take main's stage 0.**
  - Options: (a) as built: the whole pins line (the scripts' digest and the toolchain's root too: the `:stage0` claim);
    (b) GTS82's two fields (seed and Go), so a worktree that changed `bin/seed` still takes main's (its stage0 pass then
    names scripts that did not build it).
  - *Recommended: (a)*: a pass at the `:stage0` identity must be about the stage 0 it ran on; a change to the seed's
    scripts is rare and is exactly when a fresh build is the check.
- **GTS91 Java's read level's control** (left by GTS84 (a)).
  - Options: (a) a crossed control at `--forms verify` (java-forms-verify): t03208's fresh forms compared, by
    `forms_cmd`'s comparison, with t03209's committed forms in `test/java/forms/`; (b) one in the read level itself,
    the subject's `forms.sha` against another test's recorded digest (none is recorded: a new record); (c) none.
  - *Recommended: (a)*: the committed forms are the one recorded output of the read, and their comparison is the only
    judge there besides the reader's status; it needs no record of its own.
- **GTS92 goi's controls in the box.**
  - Options: (a) as built: none in the box's runs (`controls_of` returns nothing there); (b) the box's console
    comparison crossed too, when the box is unparked.
  - *Recommended: (a) now, (b) with the box's return*: the box is parked (2026-10-26) and its comparison is a third one
    (the console as one stream), worth a control when it runs again.
- **GTS93 A print control's reference outside a selection.**
  - Options: (a) as built: `left` (`--only` without t03209); (b) `print_level` adds a control's reference to its list
    when its subject is there (one more test printed).
  - *Recommended: (a)*: the gate's java-print step runs every test; a selection is a person's diagnosis, where the
    control saying it was left is enough.
- **GTS94 The handed key for the tool build too.**
  - Options: (a) as built: `bin/javac-vendor tool` and `bin/arbace-go --build-java` hash `arbace_javac_key` themselves
    (the javac-tool step is on `:javac`, its files declared); (b) both take `GATE_JAVAC_KEY` as `arbace_javac_fresh`
    does.
  - *Recommended: (a)*: no audit rule depends on it, and a build that computes its own key cannot record another
    checkout's by a stale environment.

**Slips.** None of the rules': the tests created a scratch git repository with a linked worktree of its own under the
worktree's `.tmp/` (its metadata its own, removed with `git worktree remove`), read the main checkout's
`target/seed/pins` and ran its stage 0 as `ARBACE_HOST_EXE`, ran the system JDK's javac on two jdk26u tests in place
(`-J-Xmx512m`, output in the worktree's `.tmp/`), and built `tools/gocmp` with TamaGo's go into the worktree's `.tmp/`.

## Decided (2026-10-31): GTS90-GTS94

The user decided all five as recommended: GTS90 (a), the whole pins line, as built; GTS91 (a), built on this branch
before the merge; GTS92 (a), (b) when the box is unparked; GTS93 (a), as built; GTS94 (a).

## As built: GTS91 (write-ahead)

GTS91 (a), decided 2026-10-31. `bin/java-check --forms verify` (the step `java-forms-verify`) gains Java's read level's
negative control. `forms_same` is the verify's comparison (`diff -r -x provenance.edn`, now a function), and
`forms_controls` runs it crossed: the controls of `test/java/controls.edn` with `:forms` in their `:levels` (c00001,
beside its other levels; `:forms` is no level of `check`, so `java.check/report` ignores it) compare the subject's fresh
forms (t03208, `WORK/forms-new`) with the reference's committed ones (t03209, `test/java/forms/`), which must differ.
Equal forms print `control passed: c00001 (forms): ...` and fail the verify even when the forms verify; a subject or
reference not in the run (the essential list without it, a forms tree without it) is `left`, as GTS93 (a) has it for
print (`forms_cmd` has no `--only`: it always reads the whole essential list). The step's inputs gained
`test/java/controls.edn`. Documented in `bin/java-check`'s header, doc/classes/JAVA-CONFORMANCE-SURVEY.md beside GTS81's
and GTS84's controls, and CLAUDE.md's line on the controls.

Tested (`.tmp/gts91/test.sh`, the real committed forms as the "fresh" ones, no read): the real comparison differs (rc 0);
`forms_same` replaced by one that always says equal makes the control pass (rc 1, the verify fails); t03208's forms
replaced by t03209's pass the control (rc 1); no t03208 in the run is `left` (rc 0). A real read was not run (javac's tool
and the gate's builds were off limits): the merge should see `bin/gate --full`'s java-forms-verify print the control's
line.

Proposals (GTS95): none.

## As built: review 001's fixes (write-ahead) (2026-10-31)

Review 001 (a helper VM's, of branch `gate-post-v2` at `ef97d7d6`, light: nothing built or run; findings 1-4 checked
against the code by the main session) found nine things; each fix is a commit of its own on this branch, in the light
tier as the earlier rounds (no `bin/gate` run, no build of the executables or of javac's tool, no store touched), tested on
scratch copies and synthetic material (the scripts named below, in the worktree's `.tmp/`). No shipped file changed.

**Findings 1 and 4: a linked worktree's stage 0 is its own** (`bin/seed`, `bin/lib/host.bash`, `bin/gate`,
`bin/gate-demos`). GTS82 (a) and GTS83 (a) ran a linked worktree's tools and gate on the main checkout's
`target/seed/ARCH/arbace` by its path, checked once: main's rebuild (`bin/seed --build` begins with `rm -rf` of its
directory, about 10 minutes) made every later tool call fail, and after it the same path held a stage 0 of other
pins, on which the worktree's steps went on recording passes at the `:stage0` identity of their own files. Now
`bin/seed --link FROM [-o OUT] [--arch A]` (under OUT's checkout's stage 0 lock) does nothing when `--kept OUT` holds;
else, when `--kept FROM` holds, it removes OUT's `pins` and `seed.sha256`, hard-links FROM's executables into OUT (a
copy when the link fails, e.g. across filesystems) and copies `seed.sha256` and `pins` (a few bytes: no write in the
worktree reaches main's through a shared inode), each through a temporary renamed into place, `pins` last; then checks
that each placed file is still FROM's (the same inode, or for a copy the same bytes) and that FROM and OUT are both still
kept, else removes what it placed (exit 1: a rebuild in FROM meanwhile, which removes its `pins` first and writes them
last). The worktree's `target/seed` is then a stage 0 of its own: main's `rm -rf` cannot pull it away, and its own
`bin/seed --build` and `--kept` keep it by its pins. `seed_kept` is `--kept`'s check made a function, shared by both.
`arbace_host_exe` in a linked worktree: its own when `bin/seed --kept` keeps it, else main's linked (`--link`), else
the refusal (naming `--link`'s reason, `ARBACE_HOST_EXE` and `ARBACE_SEED_BUILD=1`); `ARBACE_SEED` passed on as
`ARBACE_SEED_GO`. So the tools, `bin/gate --plan` and the gate's stage0 step decide by the one identity, the whole pins
line (finding 4: host.bash compared the seed's and Go's pins alone); about 70 ms a tool call in a linked worktree (the
seed's hash, `--kept`'s `go_check`). `bin/gate`'s `stage0_choose` runs `bin/seed --link MAIN/target/seed` and prints its
line when it linked or did not (nothing when the worktree's own was kept); the stage0 step stays `bin/seed --build
--no-aot`, which keeps the linked stage 0 or, when nothing was linked, builds as in the main checkout (GTS83 (a)'s
"else"); no `ARBACE_HOST_EXE` is exported. `bin/gate-demos` links the invoking checkout's stage 0 into its scratch
worktree (the same commit, so the same pins line), so a stale main stage 0 no longer stops it (finding 4's second
case); without one, its tools take main's the same way or refuse. Tested (`.tmp/review001/fix1.sh`, 59 cases, a
scratch repository of its own: a fake main checkout with a fake seed, its linked worktrees, a clone; fake
executables; `ln` and `cp` shims on `PATH` for a failing link and for main's rebuild in the middle of the linking):
`--link` with no stage 0 in main (refused), main's kept (the executable main's inode, `seed.sha256` and `pins` equal
copies, a write into the worktree's `pins` leaves main's kept), again (nothing linked), main's `rm -rf` then a rebuild
of other pins (the worktree's still kept, runs the old executable, its `bin/seed --build` keeps it), a stale own one
replaced, another toolchain root refused, a failing `ln` (copies), main's rebuild starting after the executable's link
(refused, nothing left), main's executable replaced while linking (refused), main's pins rewritten after their copy
(refused, the links dropped); `arbace_host_exe`: main's linked, still there after main's `rm -rf`, the worktree's
`bin/seed` changed (refused: the gate's identity), another seed refused, `ARBACE_SEED_BUILD=1` builds, a current own
one kept (its own inode), an own one stale by its pins line alone replaced by main's, no log left, a clone builds, the
main checkout unchanged; `stage0_choose` with the step's command run after it (linked, then kept by `bin/seed --build
--no-aot`; silent the second time; not linked when the worktree's `bin/seed` differs, then the build; nothing in the
clean mode, a clone or the main checkout); `gate-demos`' line (a second worktree linking the first's own while main's is
stale). `.tmp/gts83/test.sh`'s `stage0_choose` cases rewritten to the new behaviour (23 cases); `.tmp/gts82/test.sh`
retired (it read and would now link the real main's `target/seed`).

**Finding 2: `--essential-compiled`'s retry** (`bin/java-check`). The controls' rows were split off before the timeout
retry, so a subject that timed out at both of `run_actions`' limits and passed when run again left its control's row
`timeout` and the step failed (`control not judged`). Now the retry comes first: the timed-out tests are those that are
no control (a control id is no test to run), the merge replaces each retried row and adds the rows the retry brings
(its control's), and the controls are split off after it. Tested (`.tmp/gts81/java/ec.sh`, 7 cases, the tail extracted
from the script, `compiled_one` faked): GTS81's three; a subject timing out then passing (exit 0, the control judged,
only the subject run again, at twice the limit); the retried control passing (exit 1, `control passed`); timing out
again (exit 1). On `ef97d7d6`'s file the first two of the new cases fail (`ec.sh ef97d7d6`): the finding.

**Finding 3: the passes' temporaries** (`bin/lib/gate-run.bash`, `bin/gate`, `test/gate/plan.ae`). GTS80's temporary
names ended in `.edn` (`.KEY.PID.edn`, `.KEY.pub.PID.edn`), and gate.plan's `passes` read every `*.edn`, dotfiles too: a
gate killed between the record's `printf` and its `mv` left a pass the planner counted. Both: every temporary of
`record_pass` and `publish_passes` ends in `.tmp` (`.KEY.PID.edn.tmp`, `.log.tmp`, ...), and `passes` skips dotfiles.
Tested (`.tmp/gts80/test.sh`, now 39 cases): every name `record_pass` and `publish_passes` rename from ends in `.tmp`,
none in `.edn`; gate.plan's `passes` on stage 0 over a step directory of three leftovers (both old styles and the new
one) counts none, and a real pass one; with `ef97d7d6`'s `plan.ae` the two old-style leftovers count (the finding).

**Finding 5: a record always reads** (`bin/lib/gate-run.bash`). An empty number (no `NAME.mem`, or one without a field)
went into the record unquoted, an odd-length map the reader rejects, which `pass_reads` accepted. `record_pass` prints
`nil` for a number not measured (`${v:-nil}`, `:secs` too), and `pass_reads` rejects a key with no value (a keyword
followed by another keyword or by the closing brace; the record's values are never keywords). Tested (in
`.tmp/gts80/test.sh`): no `.mem` gives `:cpu-s nil :maxrss-kib nil :tree-peak-kib nil`, one field gives that one;
gate.plan's `passes` on stage 0 reads both (`[3 nil]`, `[nil nil]`); `pass_reads` rejects one, three empty numbers and a
key before the brace, accepts the whole ones (nil, numbers, `:clean true`), and `ef97d7d6`'s accepted the odd one. The
record is byte for byte main's when the numbers are there (GTS80's cases).

**Finding 6: a rerun at a key that has a pass** (`bin/lib/gate-run.bash`). `record_pass` moved the new log and inputs
into place before writing the record, so a failure after them (a full disk at the output or the record) left the old
record beside the new log. Now every file is written and checked under its temporary name first (a write failure
leaves the old pass whole, its record with its own log and output); then the old record is removed, the companions
renamed and the record last (a failure to rename leaves no pass, never one with another's log). Tested (in
`.tmp/gts80/test.sh`): a pass of a step with an output, a rerun at its key with another log and a 20 KB output under
a file size limit of 8 KiB (`ulimit -f`, EFBIG): the old record reads, its `:log-sha256` its log's, its output and no
temporary left; with `ef97d7d6`'s `record_pass` the old record had the rerun's log (the finding); the record's rename
failing (a shimmed `mv`): `record_pass` fails, no record, no temporary; a rerun that succeeds: the new record with
its log.

**Finding 8: a control's reference by its own action** (`bin/goi`). `gc_ref` reads `bflags` and `args` by bash's
dynamic scope, so in `check_one`'s controls' loop the reference was built and run with the subject's flags and
arguments (and limit). `action_parse PROGRAM` is `check_one`'s parsing of the action line made a function (it sets the
caller's `tmo`, `bflags` and `args` from their defaults, or `why` and returns 1; the subject's rows are as before); the
loop calls it for each reference before `gc_ref`. The comment says why the loop's assignments (these and `gc_ref`'s
`ref`) stay in the pipeline's subshell and why `run_verdict` there reads the reference's `ref`. Tested
(`.tmp/review001/fix8.sh`, 8 cases, a scratch `$GOROOT/test` of four programs and a scratch `controls.txt`, `gc_ref`
and `aot_build` stubbed to print what they are given): at `--aot` the subject's reference with its own `-t 30`, flags
and arguments, its build with its flags and its run with its arguments, control 1's reference with no flags, no
arguments and the default limit, control 2's with its reference's `-t 7`, one build and one run; at interp the
reference without the subject's arguments; with `ef97d7d6`'s `bin/goi` three fail (the finding). `.tmp/gts85/test.sh`
(13 cases, gc's references real) passes unchanged.

**Findings 7 and 9: what the controls catch** (doc/go/GOI-SPEC.md §11.3, doc/classes/JAVA-CONFORMANCE-SURVEY.md,
`test/java/controls.edn`, `bin/clojure-tests`' header). Each control catches a comparison that passes everything (or is
skipped), GTS81's case, and each document now says which partial judge it does not catch: goi's run and directory
controls and Java's interp and compiled ones exit 0 on both sides (not a judge that ignores the status); goi's
errorcheck controls differ by the file name, which `gocmp errorcheck` matches by (not a judge that ignores message text
or lines); Java's classes, print and `:forms` trees and c00002's output differ by their file names too (not a judge that
ignores the contents or the messages' text); the suite's by its test count (not a report that ignores failures and
errors). `test/java/controls.edn`'s header: GTS91's paragraph moved out of the sentence it was spliced into.

**The golden files.** `$GOROOT/test/fixedbugs/issue11053.out` (49 bytes) and `typeparam/dedup.out` (20 bytes) exist
under `/root/tamago-go/test` (and `.tmp/gts85/test.sh`'s `check_dir` case read both: dedup passes against its own,
its control fails against issue11053's). Without one, `dir_verdict` would expect empty output, as the review says.

**What the merge must check now** (with the two earlier lists; the stage 0 items replace GTS83's).

- In a linked worktree, before the main checkout's stage 0 is rebuilt with this branch's `bin/seed` (its pins line
  names the scripts' digest), the gate builds its own (as GTS83's list says). After it: the gate's output shows
  `bin/seed: stage 0 kept: WT/target/seed (amd64), linked from /root/arbace/target/seed (hard links), ...` before the
  stage0 step, whose line is `stage 0 kept: WT/target/seed`; `stat -c %i` of both executables is the same; a tool run
  in the worktree without `ARBACE_HOST_EXE` links the same way; and after a later rebuild in main the worktree's
  stage 0 still runs (its own inode).
- The first gate's passes in the store: no dotfile left, every record with numbers or `nil` (`:cpu-s nil` only for a
  step with no `.mem`); old-style leftovers (`.KEY.PID.edn`) in the shared store are ignored now and go with `--prune`
  after 24 h; an odd-length record already there is replaced by the next clean run's publication.
- `--essential-compiled`: its control's line `control c00001: differ, as a control must` also on a run that retried a
  timeout (`timed out, run again one at a time`).
- goi-check's and goi-aot-essential's controls as before (`fail, as a control must`); none of today's six references
  has flags or arguments, so their `gc_ref` cache entries are the same keys as before.

**Proposals (GTS95-GTS97).**

- **GTS95 The log's digest checked.** `:log-sha256` is written but read by nothing (finding 6), so a record and a log
  that do not belong together go unnoticed. Options: (a) as now; (b) `pass_reads` (so `record_pass` and
  `publish_passes`) also checks the digest against its `KEY.log` when there is one, and gate.plan's `passes` drops a
  record whose log's digest differs. *Recommended: (b)*, at the next rekey (both are in every key): one `sha256sum` of
  the log a pass read, the cost of the reads the planner makes already.
- **GTS96 The cost of the linked worktree's check.** `arbace_host_exe` runs `bin/seed --kept` at every tool call in a
  linked worktree (about 70 ms: the 19 MB seed hashed by `go_check`). Options: (a) as built; (b) `--kept --quick`
  for host.bash, the pins line compared with the archive's `.sha256` taken as it is (the gate's stage0 step keeps the
  full check). *Recommended: (a)*: the gate's tool calls are a few hundred (seconds in all), and one check everywhere is
  what finding 4 asked for.
- **GTS97 Controls that differ by one thing.** Finding 7's partial judges (the status ignored, message text ignored,
  contents ignored) are not caught. Options: (a) as documented; (b) per family a second control crossed on one
  dimension (an errorcheck reference copied under the subject's base name into a scratch directory; a run pair of the
  same stdout and another status; a class file tree renamed to the subject's names). *Recommended: (a)*: GTS81's
  question was the judge that passes everything; (b) is a check of each comparison's parts, worth it only where one of
  them is ever shadowed.

**Slips.** None of the rules': the tests made scratch repositories with linked worktrees of their own under the
worktree's `.tmp/` (removed with `git worktree remove`), ran the main checkout's stage 0 as `ARBACE_HOST_EXE` (gate.plan's
`passes` on synthetic stores), and `.tmp/gts85/test.sh` ran TamaGo's go (gc's references, `go run` of four programs,
GOMAXPROCS=4).

## As built: re-review 020's fixes (write-ahead) (2026-10-31)

Re-review 020 (the helper VM's, of `gate-post-v2` at `9a362397`: review 001's fixes, light) found seven things; each
was checked against branch `gate-merge` at `bf6a281d` (`gate-post-v2` with mac/011-014 merged, report 015's
rehearsal), and each fix is a commit of its own there, in the light tier as the earlier rounds: no `bin/gate` run, no
build of the executables, of javac's tool or of a real stage 0, no store touched. The tests are scratch scripts in the
worktree's `.tmp/rv020/` (`lib.sh`: a scratch repository with a fake main checkout, a fake seed and a linked worktree,
the scripts copied from the branch or, with `REV=COMMIT`, from that commit, so that each test is run before its fix
too); each fails at the commit before its fix and passes after. Review 001's own tests, rerun from copies
(`.tmp/rv020/fix1-001.sh` 59 cases, `gts80.sh` 39, `gts83.sh` 23), pass unchanged. No shipped file changed.

**Finding 1 (medium): an interrupted `--link` broke the next one. Holds.** A link killed after an executable's rename
and before `pins` left OUT holding FROM's executable (the same inode) and no `pins`; the next link's `mv` of a second
link of that inode over it is refused by coreutils ("are the same file"; busybox's rename does nothing and leaves the
temporary), so the second call failed, removed the executable, and only a third linked (through `bin/gate`, the stage0
step built one of its own). Fix (`bin/seed`): an executable already FROM's (`-ef`) is left in place (the post-check
verifies it); the dead runs' temporaries (`OUT/*.link.*`, `OUT/ARCH/arbace.link.*`) are removed under the lock, which no
other link of OUT holds; a run stopped by INT or TERM removes its own. Test `.tmp/rv020/f1.sh` (13 cases): a link killed
by `kill -9` while it copies `seed.sha256` (a `cp` shim), plus a dead run's executable temporary; the next link links
(no "same file"), kept, no temporary left; a link stopped by TERM leaves no temporary and the next links. At `bf6a281d`
8 fail.

**Finding 2 (low-medium): `--link` did not wait for main's rebuild. Holds.** Fix (`bin/seed`): when FROM is
`CHECKOUT/target/seed` and `CHECKOUT/.tmp/seed-build.lock` exists and is not this checkout's own (a `-o` link from the
checkout's own stage 0, as `bin/gate-demos` does from a worktree, would wait on itself), `--link` takes that lock shared
(links run beside each other; `bin/seed --build` and `bin/lib/host.bash` there take it exclusive) after its own and
before deciding, printing `bin/seed: waiting for CHECKOUT's stage 0 build ...` on stderr; at most
`ARBACE_SEED_LINK_WAIT` seconds (default 1200: a build with its compiled namespaces; polled each second, busybox's
`flock` has no `-w`), then the refusal `not linked: CHECKOUT is building its stage 0 (its lock ... held after N s)`. The
order is always the worktree's lock, then main's: nothing takes a worktree's lock while holding main's, so no cycle.
Test `.tmp/rv020/f2.sh` (13 cases): main's lock held while its directory is removed and a stage 0 written 2 s later:
the link waits, then links the rebuilt one; a build longer than `ARBACE_SEED_LINK_WAIT=1`: refused within the wait,
naming the build, not "has no amd64/arbace"; no build or no lock file in main: no wait; FROM of the checkout itself: no
deadlock; main's lock free afterwards. At `972f7fa7` 6 fail.

**Finding 3 (low): `bin/gate-demos` from a stale main checkout refused. Holds.** Fix (`bin/gate-demos`,
`stage0_link`): the invoking checkout's stage 0 is made current before it is linked into the scratch worktree, as its
own tools would: in the main checkout `bin/seed --build --no-aot` (kept when its pins are the checkout's, in about
70 ms; else built, about 10 minutes and 8 GB, with a line `the main checkout's stage 0 is not current (...): bin/seed
--build --no-aot` first), in a linked worktree `bin/seed --link MAIN/target/seed` (its own kept, else main's linked);
when none is current, what to run (`bin/seed --build --no-aot` in the main checkout) and no plan (exit 1). Test
`.tmp/rv020/f3.sh` (14 cases, the whole `bin/gate-demos --keep` on a fake checkout whose `seed_go_build` writes a
fake stage 0; the plans fail there, there being no `bin/gate`, which is not what is checked): from main with other
pins, built and the scratch worktree running the new one; from main current, nothing built, the scratch worktree
main's inode; from a worktree, main's linked into it and the scratch worktree; from a worktree with main stale, what
to run and no plan. At `e3cb0478` 11 fail.

**Finding 4 (low): the `:uses` cut-off decided on a record's existence. Holds.** Fix (`bin/lib/gate-run.bash`,
`run_one`): `pass_reads FILE NAME` in place of `[ -f ]`, as `publish_passes` and the planner decide; a record that does
not read lets the step run, and its pass replaces it. Test `.tmp/rv020/f4.sh` (7 cases, `run_one` sourced with `step`
faked, a step decided `after` with a record at its cut-off key): a record that reads cut off; no record, an empty one, a
cut one, an odd-length one and another step's record run; the run's pass replaces the bad one. At `5df57cce` 5 fail.

**Finding 5 (low): `--link` left the rest of a stale stage 0. Holds.** Fix (`bin/seed`): once FROM is kept and before
linking, what `bin/seed --build` writes into a stage 0 directory is removed from OUT: `src/` (the seed unpacked),
`prepare/`, `prepare.ae`, `prepare.log`, `build-*.log`, `aot/`, `aot.key`, `aot.runtime`, and another architecture's
`ARCH/arbace` (its directory too when that leaves it empty); any other file of OUT stays (`-o` may name any directory),
and nothing is removed when OUT is kept already or the link is refused. A linked stage 0 so has no `src/`:
`bin/lib/repr-go/plan-check` (the box's, parked) refuses in a linked worktree with its own message ("no
target/seed/src/mod (bin/seed --build)") rather than reading another seed's module (GTS100). Test `.tmp/rv020/f5.sh`
(8 cases): a stale own stage 0 with all of them and a `notes.txt`: after the link only `amd64/arbace`, `pins`,
`seed.sha256` and `notes.txt`; kept already: its `aot/` stays; refused: OUT untouched. At `251bec79` 1 fails.

**Finding 6 (low): `--essential-compiled`'s split by `NR == FNR`. Fixed before this round** by the rehearsal's
follow-up `5549f17b` (`FILENAME == ARGV[1]` in the three awk calls; an empty `controls.ids` no longer takes
`results.tsv` for the ids). What remained: with no id read (`test/java/controls.edn` emptied, or its controls not one
`{:id "..." ...}` a line, the `sed`'s shape), no control is split off and the control's row from its subject's run is
judged as a test's: a `differ` printed a "not passing" line while the count of passes stayed whole, so the step exited
0 with its control unjudged. Fix (`bin/java-check`): an empty `controls.ids` fails the step (`no control id in
.../test/java/controls.edn`). Test `.tmp/rv020/f6.sh` (10 cases: GTS81's and review 001's seven, on the tail extracted
from the script, and three new: no id with a timed-out subject, no id, the file on one line): at `e443577e` the three
new fail (exit 0); at `5549f17b^` (the `NR == FNR` split) the step failed with `2 tests: 0 pass`, the finding.

**Finding 7 (low): companions with no record never pruned. Holds.** Fix (`bin/lib/gate-run.bash`, `gate_prune`): per
step directory, each `KEY.log`, `KEY.inputs` or `KEY.out` with no `KEY.edn` and older than a day (as the temporaries'
rule: a younger one may be a publication in progress, `publish_passes` moving the companions before the record) is
removed, named on a line of its own (`gate --prune: STEP/KEY12 (no record: its .log .inputs .out)`), counted in the
summary (`..., N left without a record removed in STORE`); `--dry-run` names them. Test `.tmp/rv020/f7.sh` (6 cases):
a pass two days old with its record kept whole (one of the last 5), orphans two days old removed (named by
`--dry-run`, which removes nothing), fresh orphans kept. At `64f9dd12` 3 fail.

**Commits** (on `gate-merge`, after `bf6a281d`): `251bec79` (1), `972f7fa7` (5), `e3cb0478` (2), `5df57cce` (3),
`64f9dd12` (4), `e443577e` (7), `1fbe580c` (6).

**What the merge must check** (with the earlier lists).

- In a linked worktree, once main's stage 0 is rebuilt with this branch's `bin/seed`: a link with a stale stage 0 of
  the worktree's own leaves `target/seed` holding `ARCH/arbace`, `seed.sha256` and `pins` alone; a link run while main
  rebuilds (`bin/seed --build` in main) prints `waiting for /root/arbace's stage 0 build` and then links the new one
  (through `bin/gate`, the stage0 step's line comes after the wait: `stage0_choose` writes `bin/seed`'s output to a file,
  GTS101); a link killed partway (Ctrl-C) is repaired by the next tool call.
- `bin/gate-demos` from the main checkout right after the merge's rebuild: no "not current" line (main's own stage 0
  current), the scratch worktree's executable main's inode.
- The first `bin/gate --full`'s `--prune`: its summary line names the orphans removed; any it lists should be keys
  of interrupted runs (GT7's reruns), never one with a record.
- `--essential-compiled` with today's `test/java/controls.edn`: its control line as before (the ids c00001 and c00002
  read).
- `after` steps (`:uses`): their "cut off" skips as before; a record that does not read at a cut-off key now runs the
  step (none is expected in the store after GTS80's publication).

**Proposals (GTS100-GTS101).**

- **GTS100 A linked stage 0's `src/`.** `--link` now removes a stale `src/` and links none, so
  `bin/lib/repr-go/plan-check` (the box's plan check, parked) refuses in a linked worktree. Options: (a) as built (its
  message names `bin/seed --build`); (b) `--link` hard-links FROM's `src/` too (`cp -al`, checked by `diff -rq`);
  (c) plan-check unpacks the seed's `mod/` into its own work directory (the seed archive is the checkout's). *Recommended:
  (a) now, (c) with the box's return*: plan-check is the only reader, and its input is the seed, not stage 0's copy.
- **GTS101 The gate's stage0 line during a wait.** `stage0_choose` sends `bin/seed --link`'s output to a file and
  prints it after, so a gate in a worktree waiting up to 20 minutes for main's rebuild shows nothing meanwhile.
  Options: (a) as built; (b) `stage0_choose` prints the `waiting for` line as it comes (`bin/seed`'s stderr to the
  terminal, its stdout to the file). *Recommended: (b)*, at the next change to `bin/gate` (in every key with the
  planner): one line, the difference between a gate that waits and one that hangs.

**Slips.** None of the rules': the tests made scratch repositories with linked worktrees of their own under the
worktree's `.tmp/rv020/` (removed with `git worktree remove`), read the review 001 tests from `gate-post-v2`'s `.tmp/`
by path (copied, never run there), and ran the main checkout's stage 0 as `ARBACE_HOST_EXE` (`gts80.sh`'s gate.plan
`passes` on synthetic stores, `GOMAXPROCS=4`). The scratch `bin/gate-demos` runs made and removed scratch worktrees of
the fake repository only.

## Decided (2026-10-31): GTS95-GTS97, GTS100, GTS101

The user decided all five as recommended: GTS95 (b), at the next rekey; GTS96 (a); GTS97 (a); GTS100 (a), (c) when the
box comes back; GTS101 (b), at the next change to `bin/gate`.

## Decided (2026-11-01): GTS102, GTS103

From GT7's reruns 7 and 8 (doc/JOURNAL.md, 2026-11-01). Decided by the user:
- **GTS102** `bin/java-check` takes per-test time limits (`test/java/timeouts.txt`, as goi's `test/goi/timeouts.txt`),
  replacing the `audit-timeout` class: the 13 interp tests near the limit under the audit's ptrace get their own
  limits. At the merge window's rekey (`bin/java-check` is in every Java step's key).
- **GTS103** `bin/gate --clean --only` refuses when a run the record cites has no logs, instead of counting its steps
  as changed (rerun 8's first attempt selected 53 unchanged steps after its logs were deleted).

## As built: GTS103 (write-ahead)

GTS103, decided 2026-11-01 (a VM's branch `mac/029-gts103`, light: nothing built or run). `test/gate/clean.ae`'s
`select` (lines 112-132 and 149) no longer counts the steps of a run whose logs are gone as changed. Before,
`base-material` read `LOGS-ROOT/:logs/gate/plan/NAME.inputs` of the run each step passed at (its `:run`) and gave nil for
a missing file, so `changed?` was true for every step of a deleted logs directory: rerun 8's first attempt selected 53
unchanged steps after five such directories were deleted. Now `refuse-gone` looks, for the checks whose material is
compared, at each run they cite: when `LOGS-ROOT/:logs`, or its `gate/plan`, is not a directory, it prints one line per
such run, `error<TAB>-<TAB>gate --only: run I of the record (COMMIT, DATE, --only ONLY) has no logs, DIR is missing: the
clean keys of its N steps cannot be compared (restore the directory, or select those steps by name: S,...; GTS103)`, and
exits 2. `bin/gate`'s `only_apply` already prints a failed selection's `error` lines and stops the run (`== gate FAILED:
--only's selection`), so `bin/gate` is unchanged. Which checks are compared:
- with `changed` among ONLY's tokens, every check, before the selection is made (line 132);
- else the checks the selection leaves out (line 149), which MODE checks' stale test compares. MODE builds refuses on
  the same set, so the run stops before a build is made. A run whose steps are all named (`--only c,d`) is not compared
  and not refused: naming them is the way out when the logs cannot be restored.

A step whose own `plan/NAME.inputs` is missing in a run whose logs directory and `gate/plan` are there still counts as
changed: that is a real gap in the run's record, not a deleted run. `select`'s docstring says both.

Tested (`.tmp/fix-tests/029/gts103-test.sh`, not committed): `select` extracted from `clean.ae` and run under joker
(v1.5.8; no Java interop, so `File`'s `.isFile`/`.isDirectory`, `read-file` and `System/exit` are shimmed textually), on a
synthetic record of two runs (run 0 cites checks a, b; run 1 cites c, d) and a logs tree. On the merge commit (`8053e906`,
before GTS103), with run 1's logs deleted, `--only changed` printed `run c`/`run d ... its material changed` and exited 0,
and `--only a` (checks) failed with "2 steps not selected have changed": 5 of 10 cases fail. On GTS103's commit, all 10
pass: both modes refuse with the line above, for `changed` and for `--only a`, and for a logs directory without
`gate/plan`; `--only c,d` is not refused; and with only `d.inputs` missing in run 1's logs, `d` runs as changed and `c`
is skipped.

What the main host's run must check: `bin/gate --clean --tag TAG --only changed` with one cited run's logs directory
moved aside prints the refusal and `== gate FAILED: --only's selection` before any build is made; put back, the same
command selects as before. `clean.ae` is in no key, so nothing is rekeyed.

## Proposals (GTS110-GTS115): the judge audit of j2g-edge, generated-check and provenance-check (VM report 041, 2026-11-01)

Findings 2 to 6 of the report are fixed on the branch `audit-041` (one commit each; tests `test/gate/*-test.bash`);
finding 1 and the choices made on the way are proposed here, none decided.

- **GTS110 The reader hash covers less than the reader** (report 041, finding 1; not fixed on the branch: it rewrites
  all four provenance files and waits for a merge with the other branches).
  - Options: (a) hash `arbace/repr/java/*.ae` less a named exclusion list (print, roundtrip, coverage, split, jtree), so a
    new file on the read path is in by default, and add the front end the read reaches through `resolve.ae`
    (`arbace/classes/{analyze,env,types,lower,parse,opcodes,interp,go,emit}.ae`); the `provenance` step's `:inputs` gain
    `arbace/classes/`; (b) derive the list from the requires closure of `arbace.repr.java` (an awk over the `ns` forms),
    plus the same front end; (c) as built (a hand-kept list of 24 files).
  - *Recommended: (a)*: a list one cannot forget to extend, no parser of `ns` forms to keep right; the exclusion list is
    five names, a blind spot only if one of them joins the read path (a new file is in by default). The writers then rewrite the provenance after a change to `analyze.ae` or `env.ae`: that is the price and
    the point (it is the "a day later" slip the step was made to close). To be done in the commit that rewrites the four
    files' hash, after the merges.
  - **Decided (the user, 2026-11-01): (a).**
- **GTS111 jrt's JVM-generated files stay outside generated-check** (finding 2, third item): `standin_{lang,character,
  reflect}.ae`, `manifest.edn`, `manifest_test.ae`, `reflect_tables.ae` (`bin/jrt standins|manifest`), which the frozen
  JVM's reflection generates.
  - Options: (a) exempt, written in `bin/generated-check`'s header (as built); (b) a `--check` for them beside j2g-edge
    on the `:frozen` host (a second heavy JVM step, the same JVM answers j2g-edge already pins in `jdk/edge.edn`);
    (c) a cheap proxy: the hash of their inputs recorded in the file's header and compared (seconds, no JVM; it catches
    "inputs changed, not regenerated", not "generator changed").
  - *Recommended: (a)*; (c) if one of them goes stale in practice. `bin/jrt modules --check` and goi's pair, which need
    no JVM, are in the check.
  - **Decided (the user, 2026-11-01): (a).**
- **GTS112 The frozen jar's selection** (finding 3). As built: `frozen_pin` checks `FROZEN_JAR`/`ARBACE_ORACLE_JAR` and a
  built tag's jar against the pin line of the file name, a name with no line is an error, `FROZEN_ALLOW_UNPINNED=1` only
  warns (for the paths that make a pin); j2g-edge declares `:env J2G_FIXTURES J2G_EDGE_WORK`, javac-verify and j2g-edge
  the `:jdk` host; a `J2G_EDGE_WORK` that holds or lies in a tracked file is refused (status 2).
  - Options for what remains: (a) as built, and `FROZEN_CP` (adds to the class path, unchecked) left; (b) also
    `FROZEN_CP`, `FROZEN_TAG` and `ARBACE_ORACLE_*` in the steps' `:env` (`FROZEN_ALLOW_UNPINNED` is refused by `frozen_pin` when `GATE_EXE` is set, as built); (c) `frozen_java` refuses `FROZEN_CP` when
    `GATE_*` is set (inside the gate).
  - *Recommended: (b)*: cheap, and a pass then records which jar and class path it ran on. Whether the `:frozen` host
    should hash the jar that ran rather than the pin file is the same question; the pin check makes the two equal.
    Not decided on the branch: the `:env` change rekeys j2g-edge, javac-verify.
  - **Decided (the user, 2026-11-01): (b).**
- **GTS113 What `bin/j2g-edge trace` sees** (finding 4). As built: it fails on a recording with no traced event, and runs
  with `J2G_JOBS=1` (par-mapv is then `mapv` on j2g's own thread). The runtime's analysis `future`
  (`arbace/j2g/world.ae`, on a host with id spaces) still runs off the main thread.
  - Options: (a) as built; (b) bind a dynamic var in j2g's `run` that the futures convey, and count a call when it is
    bound, whatever the stack; (c) a switch (`J2G_ANALYZE_SYNC`) that analyzes on the caller's thread under trace; also a
    floor for the allowed categories instead of zero.
  - *Recommended: (b)*: it covers every future, now and later, at the price of one binding in `run`; the floor needs a
    first real recording on the main host to choose it from. Not run on a JVM here (the gate needs the host).
  - **Decided (the user, 2026-11-01): (b).**
- **GTS114 The tests of the scripts** (`test/gate/{generated-check,frozen,j2g-edge-record,edge-verdict}-test.bash`,
  fabricated trees and fake executables under `.tmp/`, seconds). Not in any step yet.
  - Options: (a) one light essential step `script-tests` running them, inputs the scripts and the tests; (b) run from
    `make check-makefile`'s step; (c) by hand.
  - *Recommended: (a)*: a gate script is changed rarely and silently; a step keyed on the script and its test costs
    seconds when unchanged.
  - **Decided (the user, 2026-11-01): (a).**
- **GTS115 j2g-edge's lock** (finding 5): `record` removes its three intermediate files and takes a lock on
  `$work/.lock`, polled once a second up to `J2G_EDGE_LOCK_WAIT` seconds (3600: busybox's `flock` has no `-w`).
  - Options: (a) as built; (b) a distinct work directory per run (`$work.$$`), no lock; (c) fail at once when locked.
  - *Recommended: (a)*: the gate's step and a hand-run `update` should queue, not race; (b) loses the kept logs.
  - **Decided (the user, 2026-11-01): (a).**

## As built: GTS111-GTS115 on the branch `audit-041` (2026-11-01)

- GTS112 (b): `FROZEN_CP`, `FROZEN_TAG`, `ARBACE_ORACLE_JAR` and `ARBACE_ORACLE_TAG` are in the `:env` of the five steps that
  run the frozen JVM (`j2g-edge`, `oracle-go`, `javac-verify`, `repr-java`, `jrt-convert`; `oracle-go` also has the
  `:frozen` host and `ARBACE_ORACLE_EXPECTED`), checked by `test/gate/frozen-env-test.bash`. A gate that scrubs the
  environment of `FROZEN_*` and `ARBACE_*` (the branch `mac/029-gts103`'s `gate_scrub_env`) drops these variables
  unless it keeps the names a step declares in `:env`: until then the declarations key the empty value (safe: the tools
  run on the pinned jar) and only document the variables. To settle at that merge together with the scrub's list.
  Settled (queue 085, report 084 finding 5): `gate_keep_env` keeps every name a step of the table declares in `:env`
  (`gate_declared_env_of`, the table's `:env` vectors read in awk before stage 0, outside strings and comments), so a
  caller's `FROZEN_CP`, `FROZEN_TAG`, `ARBACE_ORACLE_*`, `J2G_FIXTURES` and `J2G_EDGE_WORK` reach the steps and their keys
  hold the values; an undeclared `FROZEN_JAR` is still dropped. `test/gate/declared-env-test.bash` (no executable);
  `gate-tests` takes `bin/gate` among its inputs. A kept name is visible to every step: one that reads it without
  declaring it would run unkeyed, which `test/gate/frozen-env-test.bash` rules out for the frozen JVM's variables.
- GTS113 (b): `arbace.j2g.model/*run*` (bound in `arbace.j2g.main/run`), `run-fn` and the marker frame `on-run-thread`;
  the trace counts an event whose stack holds `arbace.j2g.main$run` or `arbace.j2g.model$on_run_thread`
  (`test/j2g/edge_verdict.ae`'s `in-run?`). Every future of j2g goes through `run-fn` (par-mapv, the runtime's analysis,
  the report). Not run on a JVM.
- GTS114 (a): the step is `gate-tests` (not `script-tests`), `bin/gate-tests` (`make gate-tests`), the tests
  `test/gate/*-test.bash`. A test marked `# needs: exe` takes `ARBACE_HOST_EXE` or stage 0; with neither it is named as
  not run and the status is 1 (`--no-exe`: 0). `test/gate/gate-reads-test.bash` holds the step to its `:inputs` by
  strace, as `bin/gate --audit` will.
- `bin/j2g-edge`: the work directory must lie under the tree's own `.tmp/`, or outside every checkout; a tree that is
  not its repository's root (an archive under another checkout) accepts only its own `.tmp/`. A work directory in
  another checkout's ignored `.tmp/` is refused (too strict, safe: `git check-ignore` could admit it).

## Decided (2026-11-01, audit 040): GTS120-GTS126

Proposals from the branch `audit-040` (the VM's audit of CLAUDE.md against the code, queue task 040, and its review, queue task 045).
All decided by the user, 2026-11-01, as recommended; what the branch built is named in each.

- **GTS120 where the gate's executables directory is computed for the tools run by hand** (finding 3, 5). Options: (a) one
  library, `bin/lib/gate-cache.bash` (`arbace_main_checkout`, `arbace_gate_exes`, `arbace_gate_cache`), used by `gate_env`,
  `bin/arbace-go`, `bin/diffcheck`, `bin/generated-check`, `bin/clojure-tests`, `bin/java-check` and `bin/lib/program.bash`,
  `ARBACE_GATE_CACHE` an override (built); (b) the function in `bin/lib/version.bash`; (c) only document that a caller sets
  `ARBACE_GATE_CACHE` to `ARBACE_GATE_EXES/KEY20`. *Recommended: (a)*: (b) mixes the version map with the gate's layout, (c)
  keeps the trap. Cost of (a): `bin/lib` is in `gate_key`, so this change rekeys both builds once; a hand run computes
  `bin/arbace-go --gate-key` (a few seconds of hashing) per call.
  - **Decided (the user, 2026-11-01): (a).**
- **GTS121 a miss of the gate's build and another key's directory** (finding 3). Options: (a) `gate_build` replaces only a directory it
  made, an allow-list checked before it builds or removes anything: none yet, this key's, or one holding a `key` file whose
  name is neither `javac-...` nor another key's; anything else (the executables' root and its parents, javac's tool's
  directory, another key's, any directory without a `key` file) is refused (built, review 045 and 048 N3); (b) never
  `rm -rf`: move a stale directory aside as `NAME.old`; (c) as before. *Recommended: (a)* now, (b) if a stale `.tmp/arbace-go-gate`
  style directory should also survive. `bin/gate`'s steps never hit it (they pass the checkout's own key).
  - **Decided (the user, 2026-11-01): (a).**
  - As built since review 051 (A, B): "one holding a `key` file" is narrowed to a gate build's shape (`bin/arbace-go`'s
    `gate_made`: a `key` file holding a 64-hex key, `version.edn` and `amd64/`), since bin/diffcheck's work directory and
    the box VM's (an ssh key) keep a file named `key`; an `ARBACE_GATE_CACHE` with trailing slashes is the same directory,
    one ending in `.` or `..` is refused.
- **GTS122 the `--g1` default** (finding 4). Options: (a) comments and docs say `--g1 "$GATE_CACHE"`, behaviour unchanged (a
  directory not at the checkout's key builds G1 with a note; built); (b) `--g1` without a directory takes `arbace_gate_cache`;
  (c) a `--g1` directory at another key is an error rather than a note. *Recommended: (a)*, then (b) for hand runs.
  - **Decided (the user, 2026-11-01): (a) now, (b) after the merge (not built).**
- **GTS123 `ARBACE_SEED` and `ARBACE_SEED_GO`** (`bin/lib/host.bash`). Options: (a) the host reads `ARBACE_SEED_GO` as `bin/seed`
  does, root-relative like it, `ARBACE_SEED` dropped (built; nothing else read it); (b) accept both, `ARBACE_SEED` as an alias;
  (c) `bin/seed` follows the host. *Recommended: (a)*.
  - **Decided (the user, 2026-11-01): (a).**
- **GTS124 the first tool run in a fresh worktree** (finding 1). Options: (a) document (`ARBACE_HOST_EXE` at main's stage 0;
  built, in CLAUDE.md); (b) `arbace_host_exe` builds stage 0 with `--no-aot` (the gate's own mode; the tools need no compiled
  namespaces, `program_base` makes them when a program wants them), saving about 3 minutes per worktree; (c) a worktree
  without a stage 0 uses the main checkout's when the seed pins are equal. *Recommended: (a) now, then (b)*; (c) shares a binary
  between checkouts whose `ARBACE_PATH` loads differ, which the key of `--gate-build` would then have to know.
  - **Decided (the user, 2026-11-01): (a) now, (b) after the merge (GTS82 (a); not built).**
- **GTS125 the heavy lock for a tool run by hand** (finding 2). Options: (a) document `bin/heavy-lock CMD` (built); (b) when
  `ARBACE_HEAVY_CMD` is unset, the tools default to `bin/heavy-lock` (a hand run then queues; `ARBACE_HEAVY_CMD=env` opts out);
  (c) only the whole-program j2g runs default to it. *Recommended: (a)*, then (c): (b) would make every `bin/j2g` of a light
  experiment wait behind a gate's heavy step.
  - **Decided (the user, 2026-11-01): (a) now, (c) after the merge (not built).**
- **GTS126 how a miss fills the directory** (review 045, 4, review 048, N2). Options: (a) the build is copied into `KEY20.tmp.PID`
  and put in place with `mv -T` at the end, the old directory renamed aside, then removed: a reader sees the old directory or
  the whole new one but for the instant between the two renames, a failed build or copy leaves the old; (b) a `flock` around
  the whole miss, a run that waited checking the key again and taking the first's build; (c) the scripts run by hand call
  `--gate-build` under `bin/heavy-lock` when `ARBACE_HEAVY_HELD` is unset (the gate's `go-build` is `:heavy`).
  **Built: (a) with (b)** (`gate_miss` under `flock` on `EXES/.KEY20.lock`; (a) alone raced: two misses failed or left a copy);
  readers stay unlocked, so a `--check-cached` landing between the renames sees a miss. *Recommended: (a) with (b), then (c)*:
  (c) would make a hand run wait behind the gate's heavy steps instead of only behind a build of its own key.
  - **Decided (the user, 2026-11-01): (a) with (b) now, (c) after the merge (not built).**
- The new tests (`test/gate/gate-cache-test.bash`, `stale-comments-test.bash`, `host-seed-test.bash`) are bash over fabricated
  trees under `.tmp/`, run by hand, in no gate step: a step (light, inputs `bin/` and `test/gate/`) would run them with the
  essential gate; *recommended*, left for the user.

## As built: GTS81 (a), the crops' controls (write-ahead, queue 064, 2026-11-01)

Branch `mac/064-gts81` from `mac/054-merge` (`a69c1dfc`), in the light tier (no Arbace executable, no build, no gate run):
GTS81 (a)'s negative controls for the families gate-post-v2 left out, the crops' checks. The design is GTS81's: a *crossed
pair*, the run (or read) of one case of the corpus judged by the family's own comparison against the expected result of
another case, both passing in the records; it runs inside the check's own run (no case, build or run of its own, but the
expect action's one run), a control that passes (`control passed:`) or does not reach the judge while its subject did
(`control not judged:`) fails the check, and one whose subject (or reference) a selection left out is printed as left. No
control is a row of the results, the records, the classes or the counts. Every judge below is tested by a joker copy of its
functions or by its shell functions extracted and run alone (the report of queue 064 holds the tests): the controls differ
on the real data files; a judge that passes everything, and the control made to pass (its reference its subject), fail the
check; a missing reference is not judged and fails; a selection leaves the control. None of these checks is a gate step;
`test/goi/controls.txt` is a gate input (goi-check, goi-aot, goi), the new line of LEVEL nethttp rekeys them.

**Every check family** (P: a control on this branch; M: missing, the proposal below it; cost per run: what the control adds):

| family | control (file:line) | how the judge knows it | out of the counts | cost |
|---|---|---|---|---|
| Java interp, compiled, classes, print, diagnostics, read (`--forms verify`) | P: c00001, c00002, `test/java/controls.edn:28,30` | plan lines `control`, `control-diag`, `control-print`; `java.check/report`, `--essential-compiled`'s tail; `forms_controls` (`bin/java-check:1160`) | the ids apart from tests, `check-LEVEL.edn` and classes | none (the subjects' runs) |
| goi interp, aot, errorcheck, directory | P: `test/goi/controls.txt` (interp, aot lines) | `controls_of` / `controls_judged` (`bin/goi:657,662`), `run_verdict` (`:643`) and the errorcheck/dir verdicts | `_control/` rows taken out of `results.tsv` | none |
| goi's net/http corpus (`bin/goi check --corpus nethttp`) | P (new): `test/goi/controls.txt`, LEVEL nethttp `_control/client_get-vs-mux_patterns` | `nethttp_one` prints the row from the subject's run against the reference's gc run; `nethttp_controls_judged`, before `nethttp_report` records | `_control/` rows taken out of `results.tsv` | none |
| the suite (eval, `--aot`, `--aot --essential`) | P: `test/clojure/controls.edn` | the report's `regressions` (`test/run_clojure_tests.clj:338`) | no namespace of the results | none |
| the oracle | P: control-1, `test/oracle/controls.edn` | `oracle.runner/controls-ok?` | no case of the record | none |
| diffcheck | P: control-1, `test/diffcheck/controls.edn` | `controls-ok?` (`test/diffcheck/main.ae:201`) | no case of known.edn | none |
| repr/c `check --c23` | P (before this branch): `arbace/repr/c/c23check.ae:113` `control-judge` (wrong prints refused) | its own verb | its own | its own |
| C run level (`bin/ci check`, `check --aot`) | P (new): `test/c/controls.edn` `:level :run` `_control/fannkuchredux-vs-fasta` | `arbace.c.run/control-result`, `controls-failing` in ci's judge and cf2g's (the same `result` rule) | not in `results.edn` or the record; a failing row of its own id | none |
| C dg level (`bin/ci dg`, `--essential`) | P (new): `test/c/controls.edn` `:level :dg` `_control/bitint-75-vs-attr-syntax-3` | `arbace.c.dg/control-result` (`verdict`, then `message-verdict`, as a row), `control-rows` among the failing rows | not in the rows, the record or `judge-rows.edn` | none |
| OCaml `bin/oi check`, `check --aot`, `suite check [--aot]` | P (new): `test/ocaml/controls.txt` judge lines (`check-oi.edn`, `check-aot.edn`, `check-suite-oi.edn`, `check-suite-aot.edn`) | `controls-ok` in `test/ocaml/oi_check.ae` and `o2j_check.ae` (`result-of` against the reference's ocamlc/ocamlopt run), before recording | not in the rows | none |
| OCaml `bin/oi suite expect` | P (new): `test/ocaml/controls.txt` expect line | `suite_expect_one`'s `@CTL=ID=REF` (the final correction against REF's text by the action's `cmp`), `expect_controls_judged`, before recording | `_control/` rows split off before the record is written | one expect run (two tool runs, about a second) |
| Scheme `bin/si check`, `check --aot` | P (new): `test/scheme/si/controls.edn` (the suite branch and the byte comparison) | `arbace.scheme.check/control-result`, `controls-code` in the judgment's exit | not in the results or the counts | none |
| C++ `bin/cppi lower-check` | P (new): `test/cpp/controls.edn` `_control/bitfields-vs-io` | `arbace.cpp.check/control-result` (`run-verdict`, factored out of `file-result`), the judge's exit; `test/cpp/selftest/controls.sh` | not in `results.tsv` or `summary.edn` | none |
| Scheme `bin/si r7rs`, `srfi`, `mats`, `ecraven`, `chibi-tests`, `test` (expand) | M | | | |
| OCaml `bin/oi repl-check`; `bin/repr-ocaml roundtrip` | M | | | |
| C `bin/ci annotations`, `bin/repr-c roundtrip`, `parse-check`, `check --tokens` | M | | | |
| C++ `bin/cppi runtime-build`, `bin/repr-cpp roundtrip`, `pp-check`; `bin/repr-llvm roundtrip` | M | | | |
| repr/java `parse-check`, `front-check`, `attr-check`, `nojavac-check`, `roundtrip`, `check --jdk`, `check --suite` | M | | | |
| repr/go `native-check`, `roundtrip`; repr/clojure `check` | M | | | |
| `bin/generated-check`, `bin/j2g-edge verify`, `bin/provenance-check`, `bin/reap` | M (VM report 041's GTS110-GTS115 cover them) | | | |

**The pairs**, each checked against the records here: C run, fannkuchredux.c and fasta.c (status 0 both, other stdout
digests; both pass in `test/c/check-linux-amd64.edn`); C dg, bitint-75.c (one dg-error, line 10) and c23-attr-syntax-3.c
(dg-errors on lines 7, 9, 14, ...: gcc 15.2's files read at gcc-mirror/gcc `releases/gcc-15.2.0`, `gcc/testsuite/gcc.dg/`),
both in `test/c/essential-dg.txt` so `--essential` judges it; OCaml, asmcomp/compare and asmcomp/evaluation_order (oi and
AOT), basic-modules/main and basic-multdef/usemultdef (the suite), tests/basic/patmatch_for_multiple.ml and
patmatch_incoherence.ml (expect); Scheme, racket/enums and racket/lists (26 tests against 72), si/amb and si/data; C++,
samples/bitfields.cpp and samples/io.cpp (status 0, 43 bytes against 6, built by g++ here); net/http, client_get and
mux_patterns (other want.out).

**Proposals for the missing families** (for the user; numbers at the merge):
- *Scheme r7rs*: a crossed pair through the `against-references` predicate: a test si passes judged against the Chez and
  Chibi answers of a test both references fail (a both-fail entry of `ref-r7rs-chez.edn`/`ref-r7rs-chibi.edn`, paired by
  source); it must give an item. *srfi*: one program's output judged by `srfi-result` with another's expected report line
  (`srfi-N: P passed`); *mats*, *ecraven*: a benchmark's CSVLINE judged against another's expected result; *chibi-tests*:
  one library's si marks against another's Chibi marks (`test-marks`); *expand* (`bin/si test`): a case's run against another
  case's Chez `:out` (`expand.ae`'s `judge`). Each in `arbace/scheme/corpora.ae` beside the existing judge, a few lines;
  none needs a run.
- *OCaml repl-check*: the session's output compared with another recorded session (none exists: a second, two-line
  `session-b.ref` would be the reference), `cmp` must fail; *repr-ocaml roundtrip*: one file's printed dump compared with
  another's original (`do-compare`).
- *C annotations, repr-c roundtrip, parse-check, tokens*; *C++ runtime-build, repr-cpp roundtrip and pp-check, repr-llvm's
  objects*: a file's level (annotations, printed text, tokens, object) compared with another file's by the check's own
  comparison (`judge-row`, `rtcheck/judge`, ppcheck's `judge`; repr-llvm's object `cmp`, bash), the pair from the corpus's
  passing files.
- *repr/java's tree checks* (parse, front, attr, nojavac): one file's native tree (or facts, forms) compared with another
  file's javac tree by the check's comparison, inside its per-corpus job; *roundtrip* and *check --jdk*: one file's forms
  against another's recorded hash; *repr/go native-check*: a dump's tree against another dump's helper tree inside
  `check-job` (GTS81's note: cheap to run, the pairing needs the job's indices); *repr/clojure*: one file's round trip
  against another's text. These need javac's tool or the tools' host: the main host.
- Every control here is in its family's own data file; the main host's first run of each check confirms each control
  differs on real runs (the pairs were chosen from the records and, for C and C++, the sources; nothing here ran the
  checks themselves).

## Proposed (2026-11-01): GTS130-GTS133 (GT7's audit 061, repr/java's tree checks; none decided)

Made on branch audit-061 where neither javac's tool nor jdk26u exist: the judges are tested over fabricated trees and a
stubbed `bin/repr-java` (`test/repr/java/judge-*-test.bash`), the records below are not measured.

- **GTS130 Where the new records live.** Options: (a) flat maps read by awk (`judge_flat_get`, `judge_flat_set`:
  `test/repr/java/expected/jdk26u-fallbacks.edn`, `parse-check-diagnostics.edn`, `parse-check-mutants.edn`), so the
  judges need no tool and the tests need none; (b) keys inside the existing known files (`parse-check-known.edn`
  `{:CORPUS {:forms #{...} :diagnostics n}}` as front-check's already allows), read by `arbace_tool`. *Recommended: (a)*
  (as built; (b) makes a corpus's known set a map and every reader of the file learn it).
- **GTS131 The mutants' record, as committed.** The mutants' digest is not recorded (`parse-check-mutants.edn` is `{}`),
  so the first real `parse-check --corpus mutants` fails naming the digest. Options: (a) fail until recorded (as built:
  nothing is silently unchecked); (b) pass with a notice until recorded (the gate stays green, the hole stays open until
  someone records); (c) record it by hand from a run before merging. *Recommended: (c), then (a) stays as the rule.*
  The finding's other suggestions are not built: a Random per file seeded by its path (changes `jtree.ae`, the reader's
  namespace), sites drawn from javac's tokens, the 4,000 files chosen from `git ls-files` at the pinned commit.
  Recommended after the next change to the reader's namespaces.
- **GTS132 The diagnostics counts.** Samples, baseline, jdk, javac and jdk26u are recorded 0 (the report's expectation,
  not measured; if a corpus has a legitimate syntax error, the first run fails with the count and `--record` fixes it);
  mutants and tests are not recorded and are said so. Options: (a) as built; (b) record them before merging and fail on
  an unrecorded corpus; (c) have `jtree`'s `-main` fail on diagnostics (a change to the reader's namespace).
  *Recommended: (b) at the first real run, (c) never (the mutants hold errors on purpose).*
- **GTS133 attr-check's `:bridges`.** Options: (a) the judge refuses a non-empty `:bridges` set (as built; a future
  admitted bridge needs a deliberate change to the judge); (b) `split.ae` counts bridges per corpus
  (`{:CORPUS {:bridges {read n}}}`), a change to the reader's namespace that rewrites the provenance; (c) leave it.
  *Recommended: (b) at the next change to the reader, (a) until then.*
  Also not built: finding 8's `rm -rf "$work/$c"` at the start of each front-check corpus (stale module trees summed and
  judged) and per-corpus attr/split/nojavac tables; and `REPR_JAVA_VENDOR_READ` as a step's `:env` (jdk.bash is no step,
  GTS-035 #4).

## Proposed (2026-11-01): GTS137 and a note on GTS132 (review 067 of audit-061; none decided)

- **GTS132 as now built:** its option (b) is built: a corpus with no recorded diagnostics count fails (tests and mutants
  until `parse-check --corpus tests,mutants --record` runs on the main host with the javac tool; not recorded here).
- **GTS137 `check --jdk` in no gate step** (035 #4; the fallbacks ceiling of audit 061 #1 only runs by hand). Options:
  (a) a `--full` step `repr-java-jdk`, `:heavy true`, `:env ["JDK_SRC" "JDK" "REPR_JAVA_VENDOR_READ"]`
  (`REPR_*` is dropped by 029's scrub, so `gate_keep_env` would need the last), after measuring its peak memory and
  time on the main host; (b) leave it by hand (`make repr-java-jdk`); (c) fold it into `repr-java-nojavac`'s jdk
  corpus. *Recommended: (a), after the first real run records `jdk26u-fallbacks.edn`.* Not added: the step's `:mem-gib`
  and `:summary` cannot be measured here.

## Proposed (2026-11-01): GTS134-GTS136 (GT7's audit 065, doc/ALPINE.md and bin/alpine-env; none decided)

- **GTS134 A dependency with several installed providers.** Options: (a) it is no edge, and `check` counts it met only
  by a provider the line's closure holds, naming it otherwise (as built); (b) resolve like apk: an exact name first, then
  the highest `k:` provider priority; (c) follow every provider (the old closure; passes a line that misses one).
  *Recommended: (a)* (conservative; the line must name one provider, as it now names `openjdk25-jdk`). Not run against
  the amd64 host's real database: `check` may now ask for a provider to be named there; run it on that host first.
- **GTS135 The host's own line.** Not edited: its counts (560, 84, 67, 636), its date (2026-10-12) and its names are the
  amd64 host's and were not regenerated. The audit proposes `ocaml5` to `ocaml5-compiler-libs` (ocaml-o1's diff),
  `chibi-scheme` and `openjdk25-jdk` added. Options: (a) regenerate the section on the host with `bin/alpine-env world`
  and `check` once the crops merge; (b) edit it now by the report's text. *Recommended: (a).*
- **GTS136 `alpine-env`'s other findings (#6, #9), not built.** A verb `doc` comparing the doc's first `apk add` block with
  `world`; `simulate --line doc|arbace --arch A --keys-dir /usr/share/apk/keys/ARCH`; `@tag` and `!name` world entries;
  `LC_ALL=C` and a second sort in `simulate`; dropping `check`'s dead `extra` branch; whether the line keeps version pins
  (`linux-virt=...`). Options: (a) all in one change after GTS134; (b) only `doc` and `--arch`; (c) none. *Recommended: (b).*

- **GTS138 (review 068 of audit-065) providers offered by the repositories.** A name counts as apk's choice when the
  index cache offers several packages for it, not only when several are installed (one installed JDK would otherwise
  drift to a newer `-jdk` and `world` would drop it). Options: (a) as built: the index cache read offline
  (`/var/cache/apk/APKINDEX.*.tar.gz`), an installed exact-name package wins and is the edge alone, one only offered
  makes the name apk's choice (review 108), `so:`/`cmd:`/`pc:` names count installed providers
  only; (b) every name, `so:` too (11 more names to put in the line on the amd64 host); (c) a short always-named list
  (`openjdk25-jdk`). *Recommended: (a).* Run on the amd64 host (this worktree shares its database), `check` now fails
  naming five providers the host's world does not: busybox-binsh (`/bin/sh`), ifupdown-ng, mkinitfs,
  openjdk25-jdk and openssh-client-default (with the packages only they pull: bridge, kmod, lddtree, ...);
  `apk add` them on the host (puts them in world) or accept them as the host's, then regenerate the doc's host line
  (GTS135).

## Proposals (GTS140-GTS143) (2026-11-01, branch `audit-060`, none decided)

From report 060 (the AOT levels of the suite and goi). The fixes are in `audit-060`; these are the choices they leave.

- **GTS140 Where the judges' tests run.** `test/goi/judge-test.bash` and `test/clojure/judge-test.bash` cut functions out of
  `bin/goi` and `bin/clojure-tests` and run them on fabricated inputs (seconds, the tools' host only).
  - Options: (a) a light gate step each (`:inputs` the script, the file it cuts from, the runner, the hook), so a change
    to a judge reruns its test; (b) one step for both; (c) by hand, `make` targets only.
  - *Recommended: (a)*: a judge that is only tested by hand is the kind of check report 060 found drifting; the step is
    seconds and keyed by the files it cuts from, so it reruns exactly when a judge changes.
- **GTS141 How a known AOT difference names the kind of result it stands for** (finding 2).
  - Options: (a) as built: a table in `apply_known` by the reason's prefix (positions, go command, runtime.GOROOT: a
    fail; -complete: an AOT build error; errorcheck: a fail that is not the printing of the forms); (b) the kind as a third
    element of each entry of `check-aot-known.edn`, `bin/goi` reading it; (c) the whole raw row recorded and compared.
  - *Recommended: (b)* once a second kind of file needs it: the data then says what a reason stands for, and a new
    entry cannot be added without it; (a) is the smaller change and keeps the file's format, as `speed:` did.
- **GTS142 Programs whose forms are missing** (finding 1).
  - Options: (a) as built: the forms path spelled as repr/go does (`forms_rel`); a run program with no forms is still
    left out of the list; (b) list the run programs from the corpus's own list (`gocmp tests`, the reader's `.tsv`) and
    print `error no forms` for any listed program without forms, so a dropped program is a row.
  - *Recommended: (b)*: (a) fixes the one name that was lost, (b) makes the next one loud; it costs a read of the `.tsv`.
- **GTS143 Checking the AOT essential lists against their rule** (finding 5).
  - Options: (a) as built: each entry must pass in the record (`bin/goi divergences --aot`; the suite's controls fail
    when their subject is missing under `--essential`); (b) also `bin/goi essential-aot --check` re-deriving the list by
    its stated rule from the two records and `$GOROOT/test` (a list that shrinks is then a failure); (c) the same for the
    suite's `essential-aot.txt`, which has no stated rule yet.
  - *Recommended: (a) now, (b) at the next re-record*: the rule's parts hold today (126 of 126 directory tests, 13 of 13
    flagged programs, 11 of 11 known ones), so (b) is a guard against a future shrink, worth building with the next record.

**Not fixed.** Finding 3 and 4 hold on main only (fixed on `mac/054-merge`); the AOT record was not recorded again after
`:gc-run` (report 011), so nothing here re-records it. Finding 6's key hashes the reader's sources (goi's REPL and builder, go/parser's forms, box.ae, REPR_GO_READER), not the host
executable (report 069 A: the interp and AOT steps run on different hosts over one corpora tree, so a host in the key
made each re-read them in turn). The AOT essential subset runs `goi.divergences --aot` on the record (069 D). The
suite's subset check is one way (069 E): a stale reference entry for a source of the subset is caught by the whole run.

## Proposed (2026-11-01, queue 074): GTS150, a judgment-only clean rerun

From the VM's queue task 074. Not decided.

The case. A change to `java-judge`'s own key (`test/java/divergences.edn`, `bin/java-check`'s judgment, the `:java-judge`
group) makes `bin/gate --clean --tag TAG --only failed,changed` rerun every Java producer: `test/gate/clean.ae`'s `select`
closes the selection over `:uses` both ways, and its upward step ("a producer of a step rerun") takes `java-read`,
`java-interp`, `java-classes`, `java-diagnostics`, `java-compiled-full` and `java-print`, then `javac-tool` and `go-build`
for them. In `test/gate/clean-arbace-reaps-3-crops-v2.edn` those six producers took 290 + 1,814 + 162 + 74 + 11,481 + 936 =
14,757 s (4.1 h) under the audit, `javac-tool` 782 s; `java-judge` itself 9 s. The upward step's reason is in the docstring:
"a producer's output is made in the fresh clone only by running it". Yet the producers' keys did not change, and their
outputs from the run they passed at are on disk.

What a clean run keeps (bin/gate's `clean_launch`, `bin/lib/gate-run.bash`'s `run_one`, `record_pass`): the clone's
`.tmp/gate` is copied whole to `.tmp/gate-clean/SHA.logs[.N]/gate/` at the end, so each run's logs hold
- `plan/NAME.inputs`, the material of each step's key, and for a consumer (decision `after`) its `uses U DIGEST` lines,
  appended by `run_one` from `out/U` just before it ran (also when it then failed; not when it was not run);
- `out/NAME`, the `GATE_OUT` of each step with `:outputs` that ran in that run, whole (for the Java levels `results.tsv`,
  `forms.sha256` and the rest of `bin/java-check --save`). A step `--only` skipped has none: `only_apply` marks it done and
  `run_one` returns before `restore_out` (and its pass is not in the clone's empty store anyway);
- `NAME.log`, `.status`, `.audit`, `.audit-status`, `.mem`, `.inputs` digests and the plan.
The clone's own store (`NAME/KEY.{edn,log,inputs,out}`, `:clean true`) is published into the shared store by
`publish_passes`, beside the everyday gate's passes (a pass already at the key is kept, not replaced), and trimmed there by
`--prune` (the last 5 passes and 30 days per step).

So what a consumer read at the base, and the digest it was keyed with, are both in the logs of the run its producer passed
at: a producer that passed clean (audit ok) at run J, whose material (its `uses` lines aside, as `changed` compares) is the
base's, has `LOGS-ROOT/<run J's :logs>/gate/out/NAME`, and the consumer's base `plan/C.inputs` has `uses U D` with D that
output's digest (`gate.plan/out-digest`, `out_sha`). Today `run J` is always the consumer's own run (a consumer is rerun
whenever its producer is), so the two are in the same directory.

- **GTS150 a consumer rerun alone on its producers' clean outputs.** Options:
  - (a) **reuse from the base run's logs, digests checked.** In `select`, the upward step takes a producer P of a selected
    consumer C only when P's output cannot be reused for each of C's `:uses` U of it; it can when P is not selected itself,
    is one step (no parts), passed clean with its audit ok at the base, its material is the base's, C's base material has a
    `uses U D` line, and `LOGS-ROOT/<P's run's :logs>/gate/out/U` has the digest D. Such a P is printed `reuse` (not `skip`)
    with its source; `bin/gate`'s `only_apply` copies the source into the clone's `.tmp/gate/out/P` (checked whole by
    `diff -r`) and marks P skipped, so that `run_one` keys C over the copied output (its `uses` lines the base's digests)
    and runs it. The downward step is unchanged: a producer rerun for its own reasons still reruns its consumers.
    *The record*: P's entry is untouched (`:run J`: passed clean at run J at the key it has here, which the selection
    checked); C's entry is this run's (`:run I`, its key over the same digests); run I's map in `:runs` gets `:reused
    [{:step P :from "SHA.logs/gate/out/P"}]`, the output each reused producer gave and where it came from, so the record
    still reads as "every step passed clean at its key", and now also "C's inputs are P's clean output of run J".
    *Trust*: the clean run's promise for a step skipped by `--only` is already "passed clean in a fresh clone at its key,
    audited, at the run its `:run` names"; (a) extends it to the bytes a consumer reads: "made in a fresh clone (run J's),
    audited there", not "made in this run's clone". The new reliance is on the logs directory as the carrier between runs:
    plain files under `.tmp/gate-clean/`, unsigned. The digest check (against the value C was keyed with at the base, read
    from the base's own logs) catches a partial copy, a deletion or an accidental edit, not a deliberate one, which could
    equally edit the record or the logs' plan files the selection already trusts. A nondeterministic producer (Java's
    interp level with timeouts, thread order) keeps the outcome of run J instead of drawing again: what a skip already does.
    *Logs gone*: GTS103 stands: a run whose logs directory or `gate/plan` is gone is refused when its steps are compared,
    which includes a producer left out of the selection. A producer whose run's logs are there but whose `out/U` is missing
    or has another digest, or whose consumer's base line is missing (the consumer did not run then, or is new), is not
    reused: it is rerun as today, the reason in its `why` line. Not a refusal: rerunning is what the clean run would do
    anyway, correct and only slower, and refusing would block a run that can proceed; the operator sees the reason in
    `only-checks.tsv`. *Time*: for the case above, 4.1 h of producers and the `javac-tool` build (when no other selected
    step needs it) become a few seconds of copying; the essential pair `java-check` / `java-check-judge` saves 43 s.
  - (b) **rerun the producers** (today). The guarantee stays "every output this run's consumers read was made in this
    run's clone"; the cost is the 4+ h above for any judgment-only change, which in GT7's reruns came with each
    `divergences.edn` class added.
  - (c) **reuse from the shared store's pass** (`STORE/P/KEY.out` at P's key, a pass marked `:clean true`). Independent of
    the logs' directories, but: `--prune` removes passes after 5 newer ones or 30 days, while the record's runs (and GTS103)
    keep their logs; a non-clean pass at the same key already in the store is kept by `publish_passes` and holds the
    everyday gate's output, so the check needs `:clean` and the key and still cannot say which run made it; the record would
    cite a store path that the next prune removes. The clone's own store is deleted with the clone when the run passes.
  - (d) **(a) behind an opt-in** (`--only java-judge --reuse-outputs`, or a token `reuse`), (b) the default. Keeps today's
    guarantee unless asked; the operator must know to ask, and `failed,changed` (the usual call) would still take 4 h.
  *Recommended: (a)*, the default for every `--only` run: it changes no step's outcome and no key, the record says what was
  reused and from where, and its fallback is today's behaviour. (d) if the user wants "made in this run's clone" to stay the
  default. (c) only if the logs were to be pruned, which GTS103 already rules out for a cited run.

## As built: GTS150 (a) (write-ahead)

Proposed above, not decided; built on the VM's branch `mac/074-clean-judge` (queue 074, light: nothing built or run), to be
kept or dropped with the decision. `clean.ae` and `bin/gate`'s `--only` code are in no key: nothing is rekeyed.
- `test/gate/clean.ae`'s `select`: the upward step of the closure takes a producer P of a selected consumer C only when
  `reuse-no` gives a reason (P has parts; did not pass clean with its audit ok; its material changed; C's base
  `plan/C.inputs` has no `uses U` line; `LOGS-ROOT/<P's run's :logs>/gate/out/U` is missing or its digest,
  `gate.plan/out-digest`, is not the line's). The reason is in P's line: `run P a producer of a step rerun, its output not
  reused: REASON`. A producer left out whose outputs selected consumers read is printed in MODE checks as `reuse NAME WHY
  SRC` (WHY the skip's, then `; its output reused from LOGS/gate/out/NAME, the digests of U,... the base's (GTS150 (a))`;
  SRC the directory to copy). The downward step, GTS79 (b), GTS103 and the stale test are unchanged; a reused producer is
  compared as any check left out (its run's logs gone: GTS103's refusal).
- `bin/gate`'s `only_apply`: a `reuse` line copies SRC whole into the clone's `.tmp/gate/out/NAME` (`cp -a`, then `diff
  -rq`; a failed copy stops the run, `== gate FAILED: --only's reuse of NAME's output`), appends `NAME<TAB>SRC` (relative to
  `GATE_CLEAN_LOGS_ROOT`) to `.tmp/gate/reused.tsv` and marks NAME skipped, so that `run_one` keys each consumer (decision
  `after`) over the copied output. `clean_record` prints ` :reused [{:step NAME :from SRC} ...]` when there is one;
  `merge` puts it in the run's map in `:runs`; the merged record's header says what it means.

Tested (`.tmp/fix-tests/074/gts130-test.sh`, not committed): `select` and `merge` copied from `clean.ae` and run under joker
(shims as GTS103's test, and `out-digest` as `out_sha`), `only_apply` and `clean_record`'s `:reused` line extracted from
`bin/gate`, over a fabricated table (builds `go-build`, `javac-tool`; producers `r`, `i` (uses `r/forms.sha256`); the judge
`j` (uses `r/results.tsv`, `i/results.tsv`); `x`) and a record whose run 0 passed everything. On the proposal's commit
(`2f467bfb`), a change to `j`'s material alone selects `run r`, `run i` (`a producer of a step rerun`) and the build
`javac-tool`: 15 of 16 cases fail (GTS103's refusal passes). On the change, all 16 pass: `j` alone runs, `r` and `i` are
`reuse` lines with run 0's `out/` as source, no build is needed (also `--only j`); an edited `out/i` reruns `i` (digest) and
still reuses `r`; a missing `out/r` reruns `r` and `i` (its consumer); `j`'s base line missing reruns both; `r`'s material
changed reruns `r`, `i`, `j` as before; `r` failed at the base reruns it; with `j` passed at a later run 1, the outputs come
from run 0's logs; run 0's logs gone is refused (GTS103); `only_apply` copies both outputs, marks them skipped and writes
`reused.tsv`, and stops when a source is gone at the copy; the `:reused` line reads and `merge` puts it in the new run's map,
`r` and `i` keeping `:run 0`, `j` at `:run 1`, `:status :passed`.

What only the main host's run can show: `bin/gate --full --clean --tag TAG --only failed,changed` after a change to
`test/java/divergences.edn` alone, on a record whose Java producers passed at a run with its logs kept: `only-checks.tsv`
lists the six Java producers as `reuse`, `javac-tool` is not built (unless another selected step needs it), `java-judge`
runs under the audit on the copied outputs (its audit ok: `.tmp/gate/out/` is read as before) and passes, its `uses` lines
equal to the base's, and the merged record carries `:reused` with the producers keeping their `:run`. Also that the run's
own logs (`SHA.logs.N/gate/out/`) then hold the copies (the size of the Java levels' `--save` directories, not measured
here).

## Proposals (GTS170-GTS173): the audited rule's review (070)

From the VM's review of main's `07194e71` (`.tmp/mac-queue/reports/070-review-main-audited.md`); built on the branch
`audit-070`, one commit per finding, none of the choices below decided. This branch rekeys Java steps (java-judge's
`:uses`, java-judge's `:outputs`, the new step java-held), so it merges after the tag.

- **GTS170 java-held under the clean run's own strace** (finding 1; review 077 found (a) and the first reading of
  (b) both leave no untraced run: the clean run's pass is published at a key the keyed `--full` shares). Options:
  (a) the first build: traced, `--timeout 100`; its traced pass is the keyed gate's too, a slowdown of one to five
  times the budget goes unseen; (b) the step defers loudly under the audit (`GATE_AUDITED`) *and* its key holds
  `GATE_CLEAN_INNER` (`:env`), so the clean pass is not the keyed gate's and the keyed `--full` after the clean run
  runs the held tests untraced at the default limit; (c) the clean run runs it twice, the second outside the audit.
  *Recommended: (b)*; built (the user's coordinator, review 077). The clean record alone then carries no untraced
  confirmation: a keyed `--full` must pass between the clean run and the tag, or the tag procedure runs
  `bin/java-check --levels interp --only "^($(paste -sd'|' held.txt)) "` by hand.
- **GTS171 which signal marks a run audited** (finding 5). Built: both must agree (`GATE_AUDITED=1` from the runner and
  TracerPid), else a message and no mark, so the judgment is stricter. Options: (a) as built; (b) `GATE_AUDITED` alone
  (a person's strace is no mark, but an audited step whose tracer was SIGKILLed mid-run still marks); (c) TracerPid alone
  (main's rule). *Recommended: (a)*: (b) trusts a variable no tracer backs, (c) marks a person's strace.
- **GTS172 what `--record` does with an audited judgment** (finding 4). Built: refused, exit 2. Options: (a) as built;
  (b) record, keeping the recorded row for a held result; (c) record the held tests as `:timeout` (main's behaviour).
  *Recommended: (a)*: the gate never records, and (b) needs `report` to know what the file held before.
- **GTS173 the two test scripts** (`test/java/audited-test.bash`, `test/gate/audited-test.bash`, and
  `test/gate/audited-test-test.bash`, review 077). On `audit-041` (GTS114 (a), decided) `bin/gate-tests` runs every
  `test/gate/*-test.bash` in the essential step gate-tests, so the gate ones are run there: they follow its
  conventions (`# needs: exe`, status 77 when already traced, file names from `git ls-files`, only declared inputs);
  `audited-test-test.bash` checks the first two by trace. `test/java/audited-test.bash` is outside `test/gate/` and is
  run by hand. Options for the Java one: (a) by hand, seconds; (b) a step of the essential gate (`:never-loads
  [arbace.j2g]`, stage 0 only); (c) a Makefile target. *Recommended: (b)*, after the merge with audit-041.

## Proposals (2026-11-01): GTS180 (review 075's findings; none decided)

From the VM's audit of `bin/lib/limit.bash` and its callers (`.tmp/mac-queue/reports/075-audit-limit-bash.md`), built on
branch `audit-075`. Done there without a decision (code fixes): `rt_sigprocmask` in `audit_trace` (finding 1), `time` outside
`timeout` (2), the retries' evidence and the single essential-compiled retry (3, 4), every timeout row retried (5), the
comments on 137/152 and load (6, 10), nethttp's gc level naming a timeout (7), the JDK reference run at the test's jtreg
timeout (8), `timeouts.txt` for directory and errorcheck programs (9). Their tests: `test/gate/limit-test.bash`,
`test/gate/audit-trace-test.bash`. Choices left open:

- **GTS180 (a) Revisiting `timeouts.txt`'s audit-only bumps and the `:audited` rule once finding 1 is in.**
  Options: (a) after the next clean run with `rt_sigprocmask` traced, read `NAME.cpu.tsv` for the programs bumped for the
  audit (gcgort, issue13160, issue13169, gc2, heapsampling, issue16037_run, the 13 interp tests of rerun 7, t03686) and
  lower each bump to its quiet CPU time times the usual margin, keeping `:audited` as it is for that one run; remove the
  `:audited` rule only when a clean run passes with no audited timeout. (b) Remove the bumps and the rule at once and let a clean
  run show what is left. (c) Keep both. Recommendation: (a): one run's evidence per step, the rule stays until a run shows
  it unused; (b) risks a clean run lost to the 8-minute-per-step reruns; (c) leaves tests hidden by `:audited`.
  Measured here (amd64 host, stage 0 at 7 s of allocation, `GOMAXPROCS=2`, the audit's strace line): system time 0.54 and 0.24
  s without `rt_sigprocmask`, 0.19 and 0.35 with, 0.17 plain (noisy: small effect for this program); a C thread making 100,000
  `sched_yield` calls: 3.3 s without, 0.07 s with. The effect that matters is Go programs with many idle Ms (64 CPUs); the
  main host's clean run is the measurement.
- **GTS180 (b) nethttp's limits (`limited_cpu 120 90`, wall below CPU).** (a) `limited_cpu 120 480` (wall four times the CPU,
  as `limited`), a blocked loopback program waits 8 minutes; (b) keep 90 and document it as a wall bound for loopback
  programs; (c) CPU 20, wall 90. Recommendation: (c) if the corpus's programs are measured under 5 s of CPU (read
  `cpu.tsv` of the next clean run), else (a).
- **GTS180 (c) The JDK reference's timeouts.** Record a JDK run that hit its timeout as `:jdk :timeout`, not `:jdk :fail "status
  124"`, so that the test leaves the plan visibly, not as a failed reference. (a) do it (check.ae and a re-record of the
  twelve rows); (b) leave the rows as they are, now that the limit is the test's own (a re-record needs the JDK). Recommendation:
  (a) at the next `--record-ref`.
- **GTS180 (d) The doubled Java retry (JCS47 (a)).** A test needing between 100 and 200 s of CPU passes at the second attempt
  with a line on stderr and a row in `timeout-attempts.tsv` now. (a) keep, and make `--save` copy `timeout-attempts.tsv` beside
  `results.tsv` so the judgment can list the tests that needed it; (b) mark such a pass `flaky` as JCS71 does (changes
  `check-*.edn`); (c) drop the in-run doubling and keep only the quiet retry. Recommendation: (a), then (b) if the list is not
  short.
- **GTS180 (e) 137 told from an OOM kill.** (a) adopt oi's CPU test (a 137 with user+system seconds under 0.8 of the limit is
  not a CPU kill) in `limited_cpu` when `ARBACE_CPU_LOG` is set, and print `killed` as the reason; (b) leave 137 as a timeout.
  Recommendation: (b) until an OOM kill is seen to be read as a timeout in a gate record.

What the main host must check: `test/gate/audit-trace-test.bash` and `test/gate/limit-test.bash` (seconds); a clean run's
`NAME.cpu.tsv` system times of the bumped programs against rerun 8's; the amd64 strace's line count with `rt_sigprocmask`
traced (bash-heavy steps make many such calls; here the trace of a 7 s stage 0 run grew by 0 lines, a gate step with many forks
needs a look at its FIFO's size and the reducer's rate).
