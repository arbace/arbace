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
| java (after javac) | `repr-java`, `repr-java-roundtrip`, `jrt-convert`, `java-fixtures`, `repr-java-parse`, `repr-java-front`, `repr-java-attr`, `java-check-full` |
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
