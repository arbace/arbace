# The gate's diet — survey and plan

The user (2026-11-13): "Our testing harness is still a behemoth, that we need to address asap." This survey measures where
`bin/gate` spends its time, memory and disk, and why one edit reruns so much. It proposes decisions (GDS1-GDS20) and a
staged plan (GD1-GD6); the user decided GDS1-GDS20 as recommended on 2026-11-13, and GD1 is built ("As built: GD1", at the end).
As first written nothing here was implemented, and nothing was run beyond `bin/gate --full --plan`, `bin/gate --nightly
--plan`, `bin/gate --full --costs` and `bin/gate --full --mem-check`, which read and plan only, plus `du` and `find` over
`.tmp`. It continues doc/GATE-SURVEY.md (GT1-GT9, GTS1-GTS255). It changes no step's key: it adds this file and its data
file alone.

Sources (2026-11-13):
- night 13's serial `bin/gate --full` on `next` `265cab86a`, 2026-11-12 17:12 to 11-13 11:49 UTC: `.tmp/night13/g-full-1.log`
  and `progress-1.log`;
- the 12:15 rerun on `ca3c7aff8`: `.tmp/night13/g-full.log`. It stopped at 13:17 with no summary;
- the triages `.tmp/night12/triage/TRIAGE.md` and `.tmp/night13/triage/TRIAGE.md`;
- the store `.tmp/gate-pass/`: 379 step directories, each pass's `.edn` with `:secs`, `:cpu-s`, `:maxrss-kib` and
  `:tree-peak-kib`, and its `.inputs`, the material of its key;
- the plans of `mac/next` `3b25e0d43` (`.tmp/gate-plan/plan.tsv`, `plan/STEP.inputs`, `rotation/`);
- next's `.tmp/gate/*.time` (GNU time, "File system outputs", 77 steps of the rerun).

The per-step data is `doc/gate-diet/costs-2026-11-13.tsv`: every step and part of the table, with its night-13 wall, CPU,
peak, declaration, disk written where measured, the nightly tier's fraction and why it ran.

## Headline numbers

| | measured |
|---|---|
| night 13, `--full` serial | **18.6 h** (1,116 min); 339 steps run, 1 skipped, 21 FAILED; budget 6 h (GTS200), 8 h planned (GTS224) |
| the steps' wall, summed | 30 h 24 min. CPU 145 h, an average of 7.8 of the host's 64 cores (12%). The host is bound by memory, not CPU |
| memory | the gate's processes at most 58.6 GiB on a 54.7 GiB host. MemAvailable fell to 1.9 GiB. Three kernel OOM kills over nights 12-13 |
| keyed, yet everything ran | 311 of 331 steps passed again inside the night, against a pass from the night before. The causes: `arbace/go/escape.ae` (176 steps), four C record files (138 steps, 7.6 h of wall; for 79 of them, 1.7 h, nothing else changed), and in night 12 the gate's own logic (163 steps) |
| the nightly tier | `bin/gate --nightly`, GT8.4's subsets and rotation, decided 2026-11-03 and built. **It never ran at night**: `_rotation/` holds no slice's runs. Every night ran `--full` over the whole corpora |
| the essential gate | 48 min of wall after 51 min of builds. Its 54 steps sum to 4 h 48 min of wall and 17 h of CPU, against a 15 min budget. `gate-tests` alone took 53 min |
| fan-out (static, today's plan) | 3,130 of 16,286 tracked files each rerun ≥ 100 steps. A file of `:tools` reruns 222 steps (median), 24 h of wall |
| memory declarations | 4 steps over 40 GiB, all `repr-java-*.jdk` at 47-61 GiB. 19 steps declared below their peaks. 82 parts declared far above (1,058 GiB declared against 207 GiB recorded). `java-interp.*` declares 53 GiB with peaks of 0.5-13.6 GiB, so it runs alone |
| disk | `/` at 85% (207 GB). `.tmp` 121 GiB over 840 entries: 48 worktrees 74 GiB (next 32.6 GiB; 15 merged into next, about 18 GiB), `keep/` 15.5 GiB, 711 unreferenced scratch entries 22.9 GiB. 32 GiB of `.tmp` files written in the last 24 h. Go's cache 7.9 GiB three hours after a trim to 1 MB |
| main | `2a72c94a` (2026-11-08), 917 commits behind `next` |

## 1. The cost map

### 1.1 Where night 13's 18.6 hours went

| phase | the steps' wall | elapsed | notes |
|---|---|---|---|
| pre, plan | 2 min | 2 min | |
| builds (go-build, javac-tool, cpp-runtime) | 51 min | 51 min | go-build 24 min at 28.9 GiB of tree; javac-tool 23 min |
| essential (concurrent by chain) | 4 h 48 min (54 steps) | about 53 min | gate-tests 53, diffcheck 32, cpp-check-essential 23, oi-essential 20, c-check-essential 18, java-compiled 14 |
| full, serial | 16 h 48 min (215 steps) | about 16 h 50 min | the night's length |
| full, the crops' light chains beside (GTS216) | 8 h 04 min (56 steps) | inside the serial run | rust-full 2 h 40, scheme-full 1 h 25, llvm-full 1 h 11, ocaml-full 56, haskell-lib 53, haskell-target-probe 35 |
| waits ("waited for room in the memory lane", the lock) | 4 h 26 min summed | | in a serial run; the beside chains held the room |

### 1.2 By crop (night 13, every phase; share at a 6 h night, GT9.1: 29.5 min a share)

| crop | steps | wall | of the night | CPU | its share |
|---|---|---|---|---|---|
| Java | 66 | 7 h 04 | 23% | 51 h 21 | 29.5 min (14×) |
| C | 119 | 4 h 46 | 16% | 16 h 14 | 29.5 (9.7×) |
| C++ | 21 | 3 h 33 | 12% | 11 h 24 | 22 (9.7×) |
| Rust | 8 | 2 h 58 | 10% | 9 h 30 | 22 (8.1×) |
| shared (builds, fixpoint, gate-tests, programs) | 14 | 2 h 27 | 8% | 18 h 44 | 36 (GT9.1's 10%) |
| Go | 22 | 1 h 48 | 6% | 15 h 11 | 29.5 (3.7×) |
| Haskell | 8 | 1 h 46 | 6% | 3 h 03 | 29.5 (3.6×) |
| Scheme | 23 | 1 h 33 | 5% | 2 h 12 | 29.5 (3.2×) |
| OCaml | 11 | 1 h 20 | 4% | 2 h 36 | 29.5 (2.7×) |
| Clojure | 6 | 1 h 17 | 4% | 11 h 36 | 29.5 (2.6×) |
| LLVM IR | 15 | 1 h 16 | 4% | 2 h 41 | 15 (5.1×) |
| asm (amd64, arm64) | 19 | 30 min | 2% | 45 min | 15 + 15 |

### 1.3 By family, the serial part (16 h 48 min)

| family | crop | parts | wall | CPU | peak GiB | FAILED |
|---|---|---|---|---|---|---|
| java-compiled-full | Java | 6 | 3 h 00 | 28 h 44 | 28.2 | 0 |
| java-interp | Java | 6 | 1 h 30 | 7 h 29 | 13.6 | 0 |
| c-aot | C | 15 | 1 h 21 | 3 h 29 | 2.2 | 0 |
| repr-go-native | Go | 16 | 1 h 02 | 9 h 24 | 29.5 | 0 |
| cpp-roundtrip | C++ | 4 | 58 min | 3 h 43 | 2.1 | 0 |
| c-annotations | C | 21 | 49 | 3 h 18 | 5.8 | 1 |
| cpp-dg | C++ | 3 | 48 | 3 h 09 | 5.1 | 1 |
| cpp-check | C++ | 1 | 45 | 1 h 59 | 5.1 | 1 |
| c-roundtrip | C | 20 | 40 | 3 h 48 | 14.0 | 0 |
| c-check | C | 15 | 33 | 1 h 44 | 2.2 | 0 |
| fixpoint | shared | 1 | 24 | 3 h 00 | 26.6 | 0 |
| repr-java-* jdk parts (roundtrip, attr; now `:clean-only`) | Java | 2 | 44 | 2 h 33 | 58.5 | 2 |
| java-print | Java | 6 | 20 | 2 h 43 | 3.6 | 0 |
| suite-aot | Clojure | 1 | 20 | 4 h 55 | 32.3 | 0 |
| c-parse | C | 21 | 19 | 1 h 52 | 11.2 | 0 |
| repr-go | Go | 1 | 16 | 3 h 43 | 37.3 | 0 |
| the other 27 families | | 76 | 3 h 03 | | | |

C's four read checks run over the same 20 corpora: tokens, parse, annotations and round trip. Each reads and preprocesses
every file again, 1 h 55 min in all. repr/java's four tree checks (parse, front, attr, nojavac) do the same over the jdk
corpora.

### 1.4 The top 20 by wall (night 13; the essential tier marked)

| step | crop | wall | CPU | peak GiB | declared | cause of the run |
|---|---|---|---|---|---|---|
| rsi-check.synthetic | Rust | 1 h 32 | 5 h 34 | 3.9 | 4 | first pass |
| java-compiled-full.jdk-run | Java | 1 h 16 | 9 h 22 | 28.2 | 50 | `:gate` host (goi's escape.ae, bin/goi) |
| cpp-roundtrip.libstdcxx-testsuite-every-40 | C++ | 54 | 3 h 32 | 2.1 | 3 | escape.ae, C's records (`:tools`, `:c`) |
| gate-tests (essential) | shared | 53 | 53 | 0.6 | 1 | `arbace/` is its input |
| cpp-dg.dg-error-own | C++ | 48 | 3 h 08 | 5.1 | 6 | failed |
| cpp-check.samples-runtime | C++ | 46 | 1 h 59 | 5.1 | 8 | failed |
| si-ecraven.ecraven | Scheme | 44 | 1 h 02 | 2.4 | 2 | first pass (SI_TIMEOUT 600) |
| c-aot.torture-execute | C | 40 | 1 h 46 | 1.4 | 4 | ci's aot.ae, C's records |
| java-compiled-full.jdk-other | Java | 40 | 5 h 00 | 20.5 | 50 | `:gate` host |
| rsi-check.samples | Rust | 36 | 1 h 55 | 3.0 | 4 | first pass |
| haskell-target-probe | Haskell | 36 | 44 | 0.9 | 2 | ci's aot.ae, prog.ae |
| java-interp.hotspot-compiler | Java | 35 | 4 h 57 | 5.5 | 53 | `:prog`, `:javac-prog` (escape.ae) |
| diffcheck (essential) | Clojure | 32 | 4 h 38 | 29.3 | 31 | `:gate` (its runs 42 s, the rest preparation) |
| llvmi-aot | LLVM | 32 | 1 h 09 | 1.4 | 2 | escape.ae, ci, C's records |
| rsi-essential | Rust | 32 | 1 h 33 | 3.6 (6.7 recorded) | 4 | escape.ae, ci, C's records |
| java-interp.jdk-run | Java | 30 | 1 h 54 | 13.6 | 53 | `:prog` |
| oi-check.run | OCaml | 28 | 1 h 19 | 2.4 | 2 | failed |
| haskell-lib-stg | Haskell | 25 | 42 | 1.5 | 2 | first pass |
| fixpoint | shared | 24 | 3 h 00 | 26.6 | 20 | `:gate` |
| go-build | shared | 24 | 4 h 25 | 28.9 | 20 | the gate key |

### 1.5 The top 20 by memory (the larger of the largest process and the tree)

| step | crop | wall | peak GiB | declared | heavy |
|---|---|---|---|---|---|
| repr-java-attr.jdk (now `:clean-only`) | Java | 21 min | 58.5 (61.4 at the OOM kill, 11-12 20:38) | 55 | yes |
| repr-java-nojavac.jdk | Java | 7 | **53.4** | 34 | inner |
| repr-java-front.jdk | Java | 6 | 48.3 | 48 | yes |
| repr-java-roundtrip.jdk (now `:clean-only`) | Java | 24 | 47.0 | 53 | yes |
| repr-go | Go | 17 | 37.3 | 30 | yes |
| suite-aot | Clojure | 21 | 32.3 | 32 | inner |
| jrt-convert | Java | 8 | 29.7 | 34 | yes |
| repr-go-native.cmd-linux-amd64 | Go | 19 | 29.5 | 28 | inner |
| javac-tool | shared | 23 | 29.3 | 27 | yes |
| diffcheck | Clojure | 32 | 29.3 | 31 | inner |
| go-build | shared | 24 | 28.9 | 20 | yes |
| java-compiled-full.jdk-run | Java | 76 | 28.2 | 50 | inner |
| programs-full | shared | 12 | 27.0 | 20 | inner |
| fixpoint | shared | 24 | 26.6 | 20 | yes |
| java-compiled | Java | 14 | 24.7 | 22 | inner |
| java-compiled-full.hotspot-runtime | Java | 12 | 23.2 | 50 | inner |
| java-compiled-full.jls | Java | 6 | 22.0 | 50 | inner |
| repr-go-native.tests | Go | 10 | 22.0 | 24 | inner |
| java-compiled-full.jdk-other | Java | 40 | 20.5 | 50 | inner |
| java-compiled-full.langtools-run | Java | 23 | 20.1 | 50 | inner |

`bin/gate --full --mem-check` (2026-11-13) reports 19 UNDER: suite-go (14 against 16.7), c-parse.benchmarksgame (6 against
9.1), java-compiled, programs, fixpoint (20 against 26.6), rsi-essential (4 against 6.7), repr-go (30 against 37.3),
suite-aot-essential (20 against 28.8), programs-full, repr-java-nojavac.jdk (34 against 53.4), cpp-builtins and others.
It also reports 82 "far above" (1,058 GiB declared against 207 GiB recorded). The lanes admit by the declarations, so
the UNDER ones cause the OOM kills and the far-above ones serialise the night: java-interp's 53 GiB runs alone.

### 1.6 Disk written

The gate does not record disk writes. GNU time's "File system outputs" is in each step's `.time` file and is dropped from
the `.mem` record. For the 77 steps the 12:15 rerun ran it sums to 26 GB: java-compiled 8.4 GB, suite-aot-essential
6.8, suite-aot 4.4, llvmi-aot 2.2, gate-tests 1.1, hsi-check 0.9. The programs j2g writes and their Go builds dominate.
Over 24 h, 32 GiB of `.tmp` files were written that are still there, plus Go's cache. In next's `target/arbace-go`
alone, 8 GiB was rewritten.

### 1.7 The essential gate's cost

54 steps. The night's elapsed time was about 53 minutes after the builds, bounded by gate-tests. The wall sums to 4 h 48 min
and the CPU to 17 h. GTS215 limits a new crop's chain to 2 min of warm wall; `bin/gate --costs` lists 11 of 12 chains over
it: rust-essential 17 min, oi-essential 14, cpp-essential 10.6, asm-arm64 6.7, haskell 5.4, llvm 5.0 (limit 1), scheme 4.2.
Over their own budgets, and not new crops: gate-tests 53 min, and diffcheck 32 min against "about 5 minutes, 40 s cached"
in CLAUDE.md. diffcheck's runs take 42 s and the other 31 minutes are preparation. Then cpp-check-essential 23,
c-check-essential 18, java-compiled 14, suite-go 12, c-aot-essential 11 and suite-aot-essential 10.

(A slip in `--costs`: its essential row counts the oracle tier's steps, asm-*-oracle among them, as unmeasured
essential ones.)

## 2. Fan-out: what one change reruns

### 2.1 Measured: why the keyed night 13 reran everything

For each step that passed inside night 13 (311), the `.inputs` of that pass compared with the pass before it:

| changed input | steps it reran | what kind of change |
|---|---|---|
| `arbace/go/escape.ae` | 176 | goi's escape analysis. It reached Java's, C's, C++'s and the crops' steps through `:tools` and the `:gate`/`:prog` hosts |
| `test/c/check-budget.edn`, `test/repr/c/annot-known.edn`, `annot-results.edn`, `roundtrip-known.edn` | 138 (7.6 h) | C's records. Every C step lists `test/c/` and `test/repr/c/` whole (`:c`), and so do C++, LLVM IR, Rust and OCaml's C API (`:ocaml-capi`) |
| ... of which only C records | 79 (1.7 h) | c-parse 21, c-tokens 20, c-roundtrip 18, c-annotations 17: their reruns found nothing that could change |
| `go/arbace/ci/aot.ae`, `prog.ae` | 51 | ci's back end (`:c-run`, `:hsi`, `:ocaml-run`) |
| `jdk/edge.edn`, five overlay variants, `jdk/README.md` | 47 | Java's world, through the `:javac-prog` host |
| `bin/goi` | 35 | |
| the gate's logic (`bin/lib/gate-run.bash`, `test/gate/plan.ae`) | 14 (night 12: **163**) | in every key; one runner edit rekeys the table |

Classes (night 13): changed files alone 204, a host and files 81, the declaration, host, logic and files 8, a first pass 8.

How much the crop boundary alone would save: if a step reran only for files of its own crop, of the crops it depends on
(OCaml's on C, Rust's on C++, LLVM IR and C, ...) or of the shared runtime, night 13 would have run 255 steps instead of
311 (23 h 07 min of wall instead of 25 h 09 min). That is 2 h, not more. The day merged real changes into every crop
(1,196 commits on `next` since 11-06). A narrower key saves what goes across crops. It does not save what a crop's own
churn costs, and that is the work of subsets, rotation and the weekly tier (§4).

### 2.2 Static: each input group of `test/gate/steps.edn`

From today's plan (`plan/STEP.inputs`, 331 steps), each tracked file's steps: those that list it, plus those whose host
key hashes it. `:gate` and `:prog` hash gate_key's 5,520 files (`:prog` less `arbace/j2g/` for a step that never loads
j2g). `:javac` and `:javac-prog` hash arbace_javac_key's 6,490 files. The logic files are in every step. The wall is
night 13's.

| group | files | steps a change to one file reruns (median / max) | their wall (median / max) | the worst file |
|---|---|---|---|---|
| `:lib-base`, `:lib-tools` | 2, 4 | 331 / 331 | 28 h 56 | `bin/lib/tmpdir.bash` |
| `:c2j-runtime` | 23 | 268 / 268 | 27 h 50 | `arbace/core.ae` |
| `:tools` | 233 | 222 / 328 | 24 h 00 / 28 h 55 | `arbace/repr/c.ae` (C's bridge: embedded, so in gate_key, and in `:tools`) |
| `:rsi` | 53 | 201 / 201 | 22 h 52 | `arbace/rust/**` (in `:tools`: the group's exclusions miss `arbace/rust/`, `arbace/llvm/`, `arbace/asm/`, `arbace/repr/asm*`) |
| `:c-run` | 38 | 154 / 245 | 19 h 44 | `arbace/repr/go/print.ae` |
| `:ocaml-capi` | 110 | 150 / 328 | 13 h 01 | `arbace/repr/c.ae` |
| `:c` | 464 | 148 / 331 | 11 h 53 | (each record file: 149 steps, 11 h 54) |
| `:build` | 22 | 95 / 152 | 11 h 05 | `bin/lib/repr-go-host.bash` |
| `:goi` | 3,022 | 108 / 237 | 10 h 27 | `bin/lib/cover.bash` |
| `:repr-java`, `:repr-java-checks` | 3,798 | 94 / 112 | 9 h 26 | `bin/lib/frozen.bash` |
| `:corpus-jdk` | 126 | 94 | 9 h 26 | `jdk/sources.txt` |
| `:java-judge` | 18 | 39 / 127 | 6 h 12 | `bin/lib/limit.bash` (127 steps) |
| `:java-run` | 28 | 36 / 237 | 5 h 19 | |
| `:cpp` | 481 | 22 / 205 | 3 h 30 | `bin/lib/repr-c/sysroot.bash` |
| `:corpus-javac` | 17 | 55 | 3 h 18 | |
| `:scheme-run` | 45 | 38 / 114 | 3 h 16 | |
| `:rust` | 310 | 8 / 331 | 2 h 59 | `bin/lib/gate-cache.bash` |
| `:hsi`, `:ocaml-run`, `:x86i`, `:a64i` | 39-74 | 27-29 / 154 | 2 h | ci's `crt` |
| `:repr-go` | 152 | 24 / 152 | 1 h 49 | |
| `:scheme`, `:ocaml`, `:llvm`, `:haskell` | 299-803 | 9-23 | 1 h 11 to 1 h 33 | |
| `:asm` | 244 | 20 / 201 | 31 min / 22 h 52 | `arbace/asm/amd64/asm.ae` (201: the `:tools` leak) |
| `:java` | 925 | 4 / 40 | 16 min | |

The cases named in the brief:
- `test/repr/c/annot-known.edn` reruns 149 steps (11 h 54 min). Its readers are `bin/ci annotations` and the C judge, so
  about 22 steps (c-annotations' 21 parts and c-judge-test) need it.
- `arbace/cpp/abi.ae` reruns 50 steps (9 h 34 min): C++, LLVM IR and Rust (`:cpp`, `:llvm`, `:rsi`, `:rust` list
  `arbace/cpp/` whole). Rust's and LLVM IR's steps read the lowering and not the ABI tables for every check. That a step
  does not read the file can be proved by the load log (§2.3).
- `arbace/go/escape.ae` reruns 222 steps. goi is embedded (gate_key) and is under `:tools`. Only Go's steps, the programs,
  the fixpoint and the suite's builds load it.
- `gate-tests` lists `arbace/` and `CLAUDE.md`, so it reruns, 53 min of the essential tier, for any edit of `arbace/`.

### 2.3 How to narrow

Cheapest first:
1. **Fix `:tools`'s exclusions.** Add `arbace/rust.ae`, `arbace/rust/`, `arbace/llvm/`, `arbace/asm/`, `arbace/repr/asm.ae`
   and `arbace/repr/asm/`. These are the crops `arbace_crop_paths` already keeps out of gate_key. An asm or Rust edit
   then reruns about 20 steps, not 201.
2. **Records per reader.** Take `test/c/` and `test/repr/c/` out of `:c` and give each level's steps their own records,
   known files and classes. A record's edit then reruns its level: 21 parts, not 149. Do the same for the crops that
   list another crop's tree whole (`:ocaml-capi` lists all of `test/c/corpora.edn`, `test/c/host/`).
3. **`:uses` early cutoff where a step reads another's results.** The judge pattern (java-judge, oi-divergences) already
   does it. C's and C++'s judges read records directly and could take them through `:uses`.
4. **Per-step load closures** (GDS9). `bin/gate --audit` already records the namespaces a step loads
   (`ARBACE_LOADED_LOG`, GT5). The record becomes data (`test/gate/loads/STEP.edn`), and the planner swaps `:tools` for
   the files of those namespaces. The audit already fails a step that loads one outside its declaration, so the closure
   stays honest. The same for the program hosts: `:never-loads` grows from j2g to goi (`arbace.go.*`) and to C
   (`arbace.c`, `arbace.repr.c`) for the steps that never load them. On night 13 that keeps Java's compiled and interpreted
   levels (6 h of wall) out of goi's edits.
5. **The gate's logic out of the keys** (GDS11). Keyed today: `bin/lib/gate-run.bash`, `test/gate/plan.ae` and
   `bin/lib/gate-cache.bash`. Night 12 reran 163 steps for them. The fix of `:needs` across chains was turned down on
   2026-11-12 because it "moves every step's key" (journal, gf-gate). An epoch (a file the keys hash in their place,
   bumped when a change alters what a step sees: its command line, environment or inputs) lets the gate be fixed without
   rerunning the world.

## 3. Caches and scratch

### 3.1 What is there (2026-11-13, `du -x`)

| area | size | written in 24 h | who reads it | eviction today | proposed rule |
|---|---|---|---|---|---|
| `.tmp/worktrees/` (48) | 74 GiB | 23 GiB (next's 18) | each agent's branch; next is the gate's checkout | none | removed by the main session once merged into `next` and the report read (15 merged, about 18 GiB); listed by `--prune` (GDS18) |
| next's `.tmp` (552 entries) + `target/` | 24 + 8.3 GiB | most | the gate's runs | the tools' own | the per-tool rules below, applied in next as in main |
| `.tmp/keep/` (15) | 15.5 GiB (`keep/wt` 14.3) | | reports naming scratch | none | 14 days after the report's merge, at most 10 GiB, the oldest out first |
| 711 unreferenced scratch entries (no `bin/`, `test/`, `tools/` script names them) | 22.9 GiB | 0.9 GiB | agents, once | none | listed after 7 days unused (621 entries, 5.1 GiB, are > 7 days old; 68 entries, 16.8 GiB with keep, are 3-7 days old); removed by the main session |
| `~/.cache/go-build` | 7.9 GiB (10.6 GB over night 13; "27 GB a night" at its worst) | | every Go build | cut to the last 90 min at each gate start, so each night starts cold | an LRU cap by size (8 GiB, by atime) at each step's start, not a 90-minute wipe |
| `.tmp/gate-pass/` | 401 MiB | | the planner | `--prune`: per step the last 5, 30 days | as now |
| `.tmp/gate-exe/` (4 dirs) | 3.2 GiB | 1.6 GiB | the gate's builds | `--prune`: the newest 3 | as now |
| `.tmp/gate-clean/` | 2.4 GiB | | the clean records (`--only` reads them) | never (memory rule) | kept while a committed clean record names them |
| `.tmp/clojure-tests/` | 3.3 GiB (next 2.2) | 2.2 | `bin/clojure-tests --aot` (keyed builds) | none | the current key's and one older generation |
| `.tmp/oracle-packs/` | 2.5 GiB | | `bin/oracle-records` | none | the packs the committed pins name |
| `.tmp/llvm`, `repr-python`, `goi-table`, `goi-check`, `javac-vendor`, `rsi`, `repr-go`, `repr-c*`, `hsi` (next 4 GiB), `repr-rust` (next 2.5), `godump` (next 2.0), `diffcheck` (1.7) | 0.3-4 GiB each | 0.3-2 each | their tool | the tool's own, mostly none | by key generation: the current key's and one older; else 7 days unused |
| `.tmp/repr-go-print-cache`, `gocache` | 0.9, 0.8 GiB | 0.6 each | `bin/build-go`, ci/cppi/rsi AOT | 90 min | as Go's cache: an LRU size cap |
| `repr-go-host`, `j2g-host` per worktree | 197 MiB × N worktrees | yes, in each | `bin/lib/repr-go-host.bash`, `bin/j2g` | none | one copy by key in the main checkout's `.tmp` (as `gate-exe`), linked into worktrees |
| `.tmp/java-check/forms*` | 0.75 GiB (next 0.6) | | `bin/java-check` | none (the 208,000-entry directory index of night 12, sharded since `7df3e67e5`) | entries unused for 3 days removed; no directory above 10,000 entries (a `--prune` check) |
| `.tmp/tmp` (TMPDIR) | 0.6 GiB | | everyone | none | files older than 2 days |

### 3.2 One prune command

`bin/gate --prune` grows from the store and the executables to every area of a table, `test/gate/caches.edn`, which is
in no key. An entry is `{:path ".tmp/clojure-tests/aot" :owner "bin/clojure-tests" :rule [:key-gen 2]}`, and the rules are
`[:key-gen N]` (directories named by key: the current and N-1 older, by mtime), `[:age DAYS]`, `[:count N]`,
`[:size GIB :lru]`, or `:keep` (gate-clean's logs, the store). It runs:
- at the start of every gate, the cheap rules only (the Go cache's cap, TMPDIR's age);
- at the end of every `--full` and `--nightly` (as GTS34 (a) does for the store);
- when a step finds the disk below the floor (§5.1).

`--prune --dry-run` prints what it would free. Worktrees and unlisted scratch are listed, never removed: the agents'
rule "keep worktree scratch" stands, and the main session removes them. The prune's run counts as a check:
`gate-tests` gains a test that the table names every `.tmp` directory a `bin/` script writes.

### 3.3 The worktrees' lifecycle

1. The main session creates one per brief.
2. The agent works and pushes its branch to mac.
3. The report is read, and the scratch the report names is copied to `keep/`.
4. Once the branch is merged into `next`, the worktree is removed, its `.tmp` with it. `bin/gate --prune --worktrees`
   lists those whose branch `git branch --merged mac/next` names and that are idle for a day. Today that is 15, about
   18 GiB.
5. `keep/` follows §3.1's rule.

A worktree's host builds are hard links to the main checkout's by key, as stage 0 already is (`bin/seed --link`).

## 4. The budget

### 4.1 The target

| option | what it means on this host | feasible by |
|---|---|---|
| **(a) 6 h of wall, concurrent in the lanes; no step's peak above 40 GiB; the charged serial sum ≤ 14 h** | 64 cores and 54.7 GiB: the big steps (≥ 16 GiB) one at a time in lane 1, the small ones (most under 6 GiB) beside them | GD1 + GD2 (below) |
| (b) 6 h serial (GTS200's letter) | the serial sum from 30 h to 6 h: most corpora weekly, the nightly only the subsets | GD1-GD5 and a weekly tier holding most of Java's and C's corpora |
| (c) 8 h of wall (GTS224's planned hours) | (a) with 2 h more | GD1 alone, nearly |

The model behind (a), from night 13's walls after GD1's moves (§6): the ≥ 16 GiB steps sum to 5.9 h, the 6-16 GiB
steps to 1.7 h and the < 6 GiB steps to 13.8 h. These used 107 CPU-h in night 13. In lane 2, eight small steps at a time
fit into what the big steps leave (16-24 GiB), so the night is about the big steps' 5.9 h plus the mid ones', about 7 h.
GD2's compressed Java subsets bring the big steps under 5 h. This is a model, not a run; GD1's night measures it. Two
conditions: correct declarations, without which the lanes either OOM (night 12) or serialise, and no step above 40 GiB
(night 12's three OOM kills and night 13's one were all 43-61 GiB javac steps).

### 4.2 What moves where

**To the nightly tier's rotation** (`bin/gate --nightly`, GT8.4, built and never run):
- Already rotated: Java's interp and compiled-full, goi-check-full, C's run checks, si-ecraven, si-mats, ocaml-roundtrip,
  oi-check, oi-aot, x86i, a64i. Their parts take 8 h 33 min in full and about 4 h 40 min as subset plus slice.
- To rotate (GD2): C's read checks (c-tokens, c-parse, c-annotations, c-roundtrip: 1 h 55 min, rotated today only for
  c-parse.benchmarksgame); cpp-dg and cpp-roundtrip (1 h 46 min); repr-go-native's corpora (1 h 02 min); the rust and
  haskell lib checks.
- Subsets that do not compress (GTS205's rule gave large sets): java-compiled-full.jdk-run's subset is 1,191 of about 1,555
  tests, 80% of the corpus; jdk-other's is 520 of 548 (96%); java-interp.jdk-run's 82%; jdk-other's 89%; si-ecraven's 40 of
  47 (87%). C's are 25-45%. A coverage subset for Java's jdk corpora near 20% saves about 1 h 40 min a night.

**To a weekly tier** (a Saturday `--full`, `GATE_ALL_PARTS=1` without the clean clone; `:clean-only` parts today):
- repr-java-{nojavac,front,attr,roundtrip}.jdk: 47-61 GiB, alone, about 1 h. The memory ceiling cannot hold them; two of them
  are `:clean-only` already, by the user's decision of 2026-11-12.
- rsi-check.synthetic and rsi-check.samples: 2 h 08 min. Rust's share is 22 min a night.
- cpp-roundtrip.libstdcxx-testsuite-every-40: 54 min, already a sample of a sample.
- haskell-target-probe: 36 min. It probes the target, and the target moves rarely.
- llvmi-aot: 32 min.
- fixpoint (24 min, 26.6 GiB) and programs-full (12 min, 27 GiB): weekly, and on a night whose commits touch j2g, repr/go
  or the build's scripts. Keyed as now, so they also run when their inputs change.

Together that is 6.2 h of the serial and beside wall out of the night.

**To the oracle tier** (`--oracle`, by hand, ORS1): no new step. The live-toolchain checks are there already.

**To retire or merge** (GD5): C's four read checks become one pass per file that judges the four levels (tokens, tree,
annotations, text), saving half of 1 h 55 min. repr/java's four tree checks become one read per corpus. The two
`*-every-N` samplings of the same g++ dg tree (`cpp-roundtrip-dg.gxx-dg-every-10`, cpp-dg's) become one sample.

### 4.3 The per-crop share

Under (a), the charged nightly is the serial sum within 14 h: 1.4 h shared and 11 shares of about 69 min (C++, Rust 52;
LLVM IR, each asm 34). After GD1 (§6) and against it:

| crop | night 13 | after GD1 (estimate) | share | over |
|---|---|---|---|---|
| Java | 7 h 04 | 4 h 31 | 69 min | 3.9× |
| C | 4 h 46 | 3 h 41 | 69 | 3.2× |
| C++ | 3 h 33 | 2 h 28 | 52 | 2.8× |
| Rust | 2 h 58 | 50 | 52 | 1.0× |
| shared | 2 h 27 | 1 h 34 (gate-tests out of the nightly path) | 84 | 1.1× |
| Go | 1 h 48 | 1 h 45 | 69 | 1.5× |
| Haskell | 1 h 46 | 1 h 10 | 69 | 1.0× |
| Scheme | 1 h 33 | 1 h 20 | 69 | 1.2× |
| OCaml | 1 h 20 | 48 | 69 | 0.7× |
| Clojure | 1 h 17 | 1 h 17 | 69 | 1.1× |
| LLVM IR | 1 h 16 | 44 | 34 | 1.3× |
| asm × 2 | 30 | 18 | 34 + 34 | 0.3× |

Java, C and C++ remain the work of GD2 and GD5. The shares stay GT9.1's weights, and GTS222's "measuring" stays until a
step has two passes.

### 4.4 The rule a new step must meet (GDS16)

A step enters `--full` (or `--nightly`) only when it meets all of these:
1. It has passed twice by hand in the gate's environment (`bin/gate --full --only STEP`, the gate unsetting `ARBACE_HOST_EXE`
   and the heavy variables). Night 12's cpp-selftest and lanes-test failed on exactly this. It enters at its two passes'
   measured peak +10%.
2. It declares `:mem-gib` ≤ 6 for the nightly lane (≤ 3 essential, GTS215), else it is `:heavy` with a reason. Nothing
   above 40.
3. It declares `:disk-gib` (new, the measured "File system outputs" +25%) and the scratch it writes, by its line in
   `test/gate/caches.edn`.
4. Its crop's share holds with it (`bin/gate --costs`), or it is rotated (`:rotate` with a subset and a universe record) or
   weekly.
5. Its inputs name its own records, not a crop's tree. The planner's check: no step lists another crop's `test/` tree
   whole.
6. A negative control (GTS81 (a)), and its judge treats time-dependent classes as may-pass (§5.3).
7. Its packs and pins are present where it runs: `bin/gate --pins` checks that each pinned pack exists. rsi-check-ui's
   missing `ui.tar.xz` failed nights 12 and 13.

## 5. Robustness

### 5.1 One disk policy, one memory policy, both the gate's

Today the tools guard themselves. cppi stopped at 80% (`CPPI_DISK_MAX`, raised to 90 on 2026-11-13), so night 13's
cpp-lower.samples failed in 0 s. rsi still stops at 80% (`bin/rsi:372`, `RSI_DISK_MAX`): the same trap, waiting. The
night's own guard, `.tmp/night13/disk-guard.sh`, polled every 300 s at 88% and missed the 01:58 spike: ENOSPC, and 22
Java level steps "passed" with corrupted saves. The memory guards are per tool too: about 40 `*_GOMEMLIMIT` / `*_CI_MEMLIMIT`
defaults between 900 MiB and 6 GiB, none derived from the step's declaration.

Proposed (GDS12, GDS13):
- **Disk.** The runner checks, before each step, that the free disk is at least the step's `:disk-gib` plus a floor
  (`GATE_DISK_FLOOR`, 25 GiB). Below it, the runner runs the prune's cheap tiers, then waits up to 30 min, and then
  marks the step `deferred`, which is not `FAILED` and is retried in the night's slack. The gate exports
  `ARBACE_DISK_POLICY=gate`, under which every tool's own guard (cppi, rsi, ...) yields. A step whose log or saves show
  "No space left on device" fails, and its pass and `:outputs` are not recorded. A sampler (the memory one, 5 s) watches
  `df` too, and pauses admission at the floor.
- **Memory.** The lanes admit by `max(declared, recorded max × 1.1)` over the store's passes. A step UNDER by more than 10%
  in two passes is printed by the plan with its new figure, and the morning's table edit takes it in. The runner exports
  `GATE_STEP_MEM_GIB`, from which the tools derive their `GOMEMLIMIT` defaults. `GATE_JOBS` is the only parallelism knob.

### 5.2 Records that drift from their known files

The pattern this week: a record or known file goes stale and the night finds it, at the cost of a step or a chain. Examples:
c-annotations.torture-compile's two stale entries, c-roundtrip's `:eval-limits` entries, oi-slow.edn's STALE ids, repr-clojure's
`:counts`, haskell's 14 FIXED entries, the Java provenance family after the javac tool's key moved.

The check (GDS15): `bin/records-check`, judge-only and seconds. Each crop registers its pairs:
- a known file against its recorded results file: an entry for a row that now passes is stale; a non-pass row with no
  entry is unclassed;
- counts derived from the results, not written by hand;
- provenance files against the current keys: the tool's key, the reader's hash.

It runs as an essential step on stage 0 (beside `reap`). It also runs as an opt-in pre-commit hook (`make hooks`, which
writes `.git/hooks/pre-commit`, the user's choice) for a commit touching `test/**/{*known*,*results*,divergences*}.edn`.
It cannot see what only a run reveals, such as a fix that makes a known failure pass. It does catch the most common slip:
a `--record` without the known file, or the other way round.

### 5.3 Time-dependent classes

`:eval-limits` (C), `oi-slow.edn` (OCaml), `:speed` (Java, hsi) and the timeouts pass or fail with the load. A judge that
fails on "stale" or "regression" for them turns load into failures. Night 12 did it with c-roundtrip; night 13 with
oi-check.run's STALE parallel/churn. Proposed (GDS14): a time class is may-pass. A pass is reported, never fails, and is
listed for removal after it has passed in four rotations. Limits are CPU seconds (GTS102's rule) rather than wall.

### 5.4 The week's self-inflicted failures, each with the rule that prevents it

| # | failure | night | rule |
|---|---|---|---|
| 1 | three `arbace-javac` OOM kills (47.5, 43.1, 48.2 GiB) admitted by the free lane with declarations of 30, 31 and 43 | 12 | admission by the recorded max (§5.1); no step above 40 GiB at night (GDS3) |
| 2 | repr-java-attr.jdk at 61.4 GiB, OOM-killed on a 54.7 GiB host | 13 | the same; weekly and alone until split |
| 3 | ENOSPC 02:53-10:07 (night 12) and at 01:58 (night 13); 22 Java level steps "passed" with corrupt saves, so java-judge and java-held failed | 12, 13 | the disk floor before each step; ENOSPC in a log fails the step and records no pass (§5.1) |
| 4 | ext4's directory index full at 208,000 entries in `.tmp/java-check/forms` | 12 | sharded (`7df3e67e5`); the prune fails a directory above 10,000 entries (§3.2) |
| 5 | timeouts under `--concurrent` (oi-check.run, si-ecraven, hsi-check, c-aot.compcert, c-aot.benchmarksgame) | 12, 13 | CPU-second limits; time classes may-pass (§5.3) |
| 6 | load-dependent known entries flipping to STALE (c-roundtrip's `:eval-limits`; oi-slow's churn, weak_array_par) | 12, 13 | may-pass (§5.3) |
| 7 | cppi's own 80% disk guard stopped cpp-lower.samples in 0 s | 13 | one disk policy, the tools yield (§5.1); rsi's 80% guard is the next |
| 8 | lanes-test saw `ARBACE_HEAVY_HELD=1` from the `--concurrent` gate's environment | 12 | a step's environment is the runner's list, and nested tests scrub it; entry rule 1 (§4.4) |
| 9 | cpp-selftest needed `ARBACE_HOST_EXE`, which the gate unsets | 12 | entry rule 1 |
| 10 | rsi-check-ui: its pinned IR pack is in no checkout | 13 | entry rule 7: `--pins` checks that packs exist |
| 11 | haskell-roundtrip and rust-roundtrip entered `--full` never having passed | 12 | entry rule 1 (two passes) |
| 12 | the provenance family stale after the javac tool's key moved (j2g-edge, javac-verify, jrt-convert, java-fixtures) | 12 | `bin/records-check`'s provenance pairs at commit (§5.2) |
| 13 | repr-clojure's hand-written `:counts` stale after an unrelated file was added | 12 | counts derived (§5.2) |
| 14 | haskell-lib-check-own "not run" although its need passed: a `:needs` across chains is not waited on, and the fix was refused because it moves every key | 12 | the logic epoch (GDS11), then the fix |
| 15 | `frozen-env-test`'s `\b` matched `bin/oracle-records`, failing gate-tests and two nested tests | 13 | gate-tests in the essential gate of the branch that changes them (it was), and the 53 min test out of the essential path (GDS7) |
| 16 | declared `:mem-gib` far above the peaks (java-interp 53 against 0.5-13.6), so the night ran serially | 12, 13 | declarations from the store (§5.1) |
| 17 | the 12:15 rerun stopped at 13:17 with no summary or record | 13 | the runner traps its end (signal or exit) and writes the summary and partial record |
| 18 | the disk at 84-85% for days with 18 GiB of merged worktrees and 22.9 GiB of unreferenced scratch | all week | §3's prune and lifecycle |
| 19 | the keyed night reran 311 of 331 steps | 12, 13 | §2.3's narrowing, the logic epoch |
| 20 | the decided nightly tier (GTS202) never ran; `--full` ran instead | all week | GDS1 |

## 6. The plan

**GD1 (one day; table edits, small script fixes; one night's rerun cost).** It cuts the most for the least:
1. The night runs `bin/gate --nightly`, and a weekly `--full` runs Saturday night (GDS1). This removes about 3.9 h.
2. The weekly list of §4.2 as `:clean-only` parts, run weekly with `GATE_ALL_PARTS=1` (GDS4). This removes about 6.2 h from
   the night's serial and beside wall, and with it every step above 40 GiB.
3. gate-tests to `:phase :full`, its inputs narrowed to the files its tests read (no `arbace/`, no `CLAUDE.md`). This takes
   53 min off the essential path (GDS7).
4. `:tools` gains its five missing exclusions (GDS8).
5. `:c` loses `test/c/` and `test/repr/c/`, and each C level names its own records (GDS10, the C part).
6. The declarations from `--mem-check`: 19 raised, 82 lowered. java-interp's 53 GiB goes down to its 15 (GDS13, the table part).
7. rsi's 80% guard goes to 90%, the same stopgap as cppi's (one line).
8. The main session removes the 15 merged worktrees (about 18 GiB) and caps Go's cache by size (`gocache_trim`'s rule).

Estimate on night 13's walls: the serial part from 16 h 48 to about 12 h, the beside chains from 8 h to 3 h 50 min. Under
`--concurrent` with correct declarations, about 7 h of wall at peaks ≤ 40 GiB (§4.1's model). Keys: the table edits
rekey the edited steps (their declarations), so GD1's first night runs them all once. No gate_key move; it fits window
W's rule.

**GD2 (days 2-3): subsets that compress.** Rederive Java's jdk-run and jdk-other subsets to about 20% (GTS205's rule with
a size target), and si-ecraven's to under 10. Rotate C's read checks, cpp-dg and cpp-roundtrip, and repr-go-native's
corpora (GDS5, GDS6). About 3 h more off the night. The concurrent night then fits within 6 h.

**GD3: narrower keys.** The logic epoch (GDS11), the per-step load closures from the audit's load log, and `:never-loads`
for goi and C on the program hosts (GDS9). One night's full rerun, then the cross-crop fan-out falls by about 2 h on a
night like 13, and a gate fix no longer rekeys everything.

**GD4: one policy.** In the runner: the disk floor, `deferred`, ENOSPC as failure, `GATE_STEP_MEM_GIB`, admission by
recorded max (GDS12, GDS13); the caches table and the extended `--prune` (GDS17); `bin/records-check` (GDS15); may-pass
time classes (GDS14). The runner is in every key, so GD4 lands with GD3's epoch to pay the rekey once.

**GD5: merges.** One read pass for C's four read levels, one for repr/java's four tree checks, and the jdk parts split by
module under 40 GiB, back into the nightly rotation.

**GD6: the essential tier to 15 min.** Each crop's chain within GTS215's limit on warm builds. A chain over it in two passes
is listed by the plan and demoted to the nightly tier at the morning's table edit (GDS20). diffcheck's 31 min of
preparation looked into.

## 7. Decisions for the user (GDS1-GDS20)

- **GDS1 The night's command.** (a) `bin/gate --nightly` every night, `--full` weekly, the clean run at a tag; (b) `--full`
  every night. *Recommended: (a)*. GTS202 decided it on 2026-11-03, and it was never switched on.
- **GDS2 The budget.** (a) 6 h of wall, concurrent in the lanes, every step ≤ 40 GiB, the charged serial sum ≤ 14 h; (b) 6 h
  serial; (c) 8 h of wall. *Recommended: (a)*: the host has 64 cores and used 7.8 of them; (b) moves most corpora to weekly.
- **GDS3 The memory ceiling at night.** (a) 40 GiB, the steps above weekly and alone until split; (b) 55 as now.
  *Recommended: (a)*: every OOM kill this week was above 43.
- **GDS4 The weekly tier.** (a) `:clean-only` parts run weekly by `GATE_ALL_PARTS=1`, §4.2's list; (b) a new `:phase
  :weekly` (a planner change, rekeys everything until GDS11). *Recommended: (a) now, (b) with GD3*.
- **GDS5 Java's subsets.** (a) rederived to about 20% of the jdk corpora (the coverage rule plus a size target, the rest
  rotating); (b) as now (80-96%). *Recommended: (a)*.
- **GDS6 More rotation.** (a) C's four read checks, cpp-dg, cpp-roundtrip and repr-go-native rotated with subsets; (b) not.
  *Recommended: (a)*.
- **GDS7 gate-tests.** (a) to the full tier, inputs narrowed; (b) essential as now. *Recommended: (a)*.
- **GDS8 `:tools`.** (a) add the five missing crop exclusions; (b) wait for GDS9. *Recommended: (a)*: a one-line fix.
- **GDS9 Per-step load closures.** (a) the audit's load log as data, and the planner keys each step by the namespaces it
  loads; `:never-loads` for goi and C on the program hosts; (b) groups narrowed by hand. *Recommended: (a)*.
- **GDS10 Records per reader.** (a) a level's records are inputs of that level's steps alone, starting with C, then each
  crop listing another crop's tree; (b) as now. *Recommended: (a)*.
- **GDS11 The gate's logic and the keys.** (a) an epoch file in place of the runner's and planner's text, bumped by hand
  when a change alters what a step sees, with gate-tests proving the runner; the clean run at a tag runs everything
  anyway; (b) as now. *Recommended: (a)*. The risk is that a runner bug keeps a pass it should drop; the clean run and the
  weekly `--full` bound it.
- **GDS12 Disk.** (a) the gate's floor, `deferred`, ENOSPC fails, tools yield under `ARBACE_DISK_POLICY=gate`; (b) per-tool
  guards. *Recommended: (a)*.
- **GDS13 Memory.** (a) admission by the recorded max, `GATE_STEP_MEM_GIB` for the tools' limits, the plan printing UNDER
  ones; (b) declarations by hand. *Recommended: (a)*.
- **GDS14 Time classes.** (a) may-pass, CPU-second limits; (b) as now. *Recommended: (a)*.
- **GDS15 Records drift.** (a) `bin/records-check` as an essential step and an opt-in pre-commit hook; (b) the step alone.
  *Recommended: (a)*. The hook is the user's to install.
- **GDS16 The entry rule.** (a) §4.4's seven conditions, checked by the planner where it can (2, 3, 5, 7); (b) GTS222's two
  passes alone. *Recommended: (a)*.
- **GDS17 Caches.** (a) `test/gate/caches.edn` and `bin/gate --prune` over it, Go's cache capped by size; (b) per tool.
  *Recommended: (a)*.
- **GDS18 Worktrees.** (a) removed by the main session once merged and reported, listed by `--prune`, the host builds
  shared by key; (b) kept until the end of a round. *Recommended: (a)*.
- **GDS19 Shares.** (a) GT9.1's weights over the charged 14 h (about 69 min a share); (b) the 6 h serial shares (29.5).
  *Recommended: (a)* with GDS2 (a).
- **GDS20 The essential tier.** (a) chains over GTS215's limit in two passes demoted to nightly at the morning's edit; (b)
  as now. *Recommended: (a)*.

## 8. Not measured

- The disk each `--full` step writes. Only the 77 steps of the 12:15 rerun have it (§1.6). GD4 records it from the
  `.time` files the runner already writes.
- The concurrent night's wall after GD1 is a model (§4.1), not a run.
- The fan-out of §2.2 counts every step that would rerun, including those whose `:uses` cutoff would stop them early
  (judges).
- Go's cache growth is from two trim lines (7.1 GB and 10.6 GB at the gates' starts), not a series.

## As built: GD1 (branch `gate-gd1`, 2026-11-13)

The user decided GDS1-GDS20 as recommended (2026-11-13) and asked for GD1 at once, on a branch merged after main advances.
GD1 changes the table, `bin/gate`'s option parsing and two scripts. It changes neither the planner (`test/gate/plan.ae`) nor
the runner (`bin/lib/gate-run.bash`), so no step's logic key moves. `gate_key` is the same before and after
(`126c321af14f…`, `bin/arbace-go --gate-key` on `3b25e0d43` and on the branch; `test/gate/gate-key-crops-test.bash` passes).
The steps whose inputs changed (C's, gate-tests, the `:tools` steps) are rekeyed and run once on GD1's first night. Changes
to `:mem-gib` and `:clean-only` are not in a key.

**GDS1 (a): the night's command.** `bin/night` is the night's runner. It replaces the main session's scratch scripts
(`.tmp/nightNN/run.sh`), and its header states the contract:
- The tier is `auto` by default. The night that starts on Saturday (UTC, the start time less 12 h, as `GATE_NIGHT` counts
  it; `NIGHT_WEEKLY_DAY`) runs `bin/gate --weekly`. Every other night runs `bin/gate --nightly`.
- The runner starts only on a clean checkout. The disk must be at most 85% full (`NIGHT_DISK_MAX`). With
  `--upstream REMOTE/BRANCH`, HEAD must be that branch's head after a fetch. Otherwise it writes the reason to the progress
  log and exits 1 without running anything.
- It drops the host and lock variables, then runs one gate in the foreground. It writes `LOG/gate.log`, `progress.log`,
  `status`, `tier` and `commit`; LOG defaults to `.tmp/night/NIGHT`.
- With `--ff DIR [--push REMOTE]...`, a pass fast-forwards DIR's branch to the commit the gate ran on. The fast-forward is
  `--ff-only` and never forced, and each push follows it. A failure fast-forwards nothing.
- `--detach` starts the run with setsid and writes its PID to `LOG/run.pid`. `--plan` prints the tier's plan.
- `make night`, `make night-plan`.

The main session's slot becomes `bin/night --upstream mac/next --ff /root/arbace --push origin --push mac --detach`, run in
`.tmp/worktrees/next`.

**GDS4 (a): the weekly tier.** `bin/gate --weekly` is `--full` with every part (`GATE_ALL_PARTS=1`); `make
gate-weekly-plan` prints its plan. §4.2's weekly list is now `:clean-only` parts:
- repr-java-nojavac.jdk (declared 59 GiB, `:heavy true`) and repr-java-front.jdk. Beside the two the user had already made
  `:clean-only`, repr-java-attr.jdk and repr-java-roundtrip.jdk, they make four.
- rsi-check.samples and rsi-check.synthetic.
- cpp-roundtrip.libstdcxx-testsuite-every-40.
- Four whole steps as one part `weekly` each: haskell-target-probe (its chain named, so `:night-concurrent` keeps it),
  llvmi-aot, fixpoint and programs-full.

A `:clean-only` part now means weekly and the clean run.
- **Every peak in the nightly tier is now 40 GiB or less** (GDS3 (a)). The largest is repr-go's 37.3 GiB, declared 42.
- **Not built: GDS4's "and on a night whose commits touch j2g, repr/go or the build's scripts".** Without a planner change
  (GDS4 (b), with GD3's epoch), the fixpoint and programs-full wait for the weekly night.

**GDS7 (a): gate-tests.** gate-tests is now `:phase :full`, which takes 53 minutes off the essential path. Its inputs no
longer include `arbace/` or `CLAUDE.md`:
- Of `arbace/` it declares only the tool namespaces that stage 0 loads from the checkout when they differ from its embedded
  copies. These come from the clean run's `gate-tests.loaded` and the `.paths` of 2026-11-06, checked again on the branch by
  gate-reads-test: box, classes, go's checker (not `run.ae`, `escape.ae` or `table.ae`), j2g's start, jrt, model, names and
  embed, repr/go's reader and printer, and the lexers of repr/c and repr/java.
- `test/gate/claude-md-test.bash` moved to `test/docs/claude-md-test.bash`. It runs as the new essential step `claude-md`,
  which takes seconds and whose inputs are `CLAUDE.md`, `doc/GATE-SURVEY.md` and `arbace/j2g/embed.ae`.

**GDS8 (a): `:tools`.** It gains `!arbace/rust/`, `!arbace/llvm/`, `!arbace/repr/asm.ae`, `!arbace/repr/asm/` and
`!arbace/asm/`. These are the rest of `arbace_crop_paths` under `arbace/`; `arbace/rust.ae` does not exist. No namespace
outside these trees requires them, and every step that runs their scripts declares its own group (`:rust`, `:rsi`, `:llvm`
or `:asm`).

**GDS10 (a), the C part.** `:c` keeps the tools and test/c/'s shared data: the manifest, `host/`, `records/`, the controls,
dg-targets, synth/, run/, judge/, c23/ and cinterp/. It loses test/c/'s level files and all of test/repr/c/. Each level
names its own group:
- `:c-records-run` (c-check, c-check-essential with `essential-check.txt`);
- `:c-records-aot` (c-aot, c-aot-essential);
- `:c-records-dg` (c-dg, c-dg-essential);
- `:c-records-tokens`;
- `:c-records-parse` (with the token level's known file, which parsecheck reads);
- `:c-records-annot` (with `parse-gcc.tsv` and the token level's known file);
- `:c-records-roundtrip` (with `parse-check-results.edn`, which rtcheck reads, `parse-gcc.tsv` and `records/`);
- `:c-records-c23`;
- `:c-records`, every one of them, for c-judge-test and c-records-verify.

The groups are drawn from the C scripts' and namespaces' paths and the clean runs' audits (2026-11-05/06). The crops that
list `:c` read no C record: cppi, rsi and llvmi read their own. The next clean run's audit is the proof.

**The declarations (GDS13 (a), the table part).** From `bin/gate --full --mem-check` and the store's and clean records'
peaks: each UNDER step was raised to ⌈1.1 × its peak⌉, and each step declared far above (more than 2 × its peak + 2 GiB)
was lowered to it.
- **13 raised**: suite-go 19, java-compiled 28, suite-aot-essential 32, programs 28, haskell-lib-own-base 4,
  repr-java-nojavac.jdk 59, fixpoint 30, programs-full 30, repr-go 42, c-parse.benchmarksgame 11, cpp-builtins 3,
  rsi-essential 8, and oi-essential 3 (its 2.8 GiB within the margin of 3, GTS215's essential limit).
- **81 lowered**:
  - java-interp's parts from 53 to 3-16 (jdk-run 16; the step's own 53 kept for the whole level);
  - java-compiled-full's five parts from 50 to 23-26;
  - 66 of C's parts (tokens, parse, annotations, roundtrip, aot) to 1-4;
  - cpp-selftest to 1, cpp-dg's preprocess and dg-error to 1;
  - rust-roundtrip's two measured parts to 3.
- After the edit, `--mem-check` passes: none UNDER, and one far above, goi-aot. That is a whole-step entry of an older record
  (1.9 GiB), set against its parts' 8. java-interp keeps its step-level 53 GiB for the whole level, the reaps-3-crops-v2 clean run's 47.7 GiB, and its parts
declare their own. `gate.lanes mem-check` now compares a parted step's whole-step entries of older records with the larger
of its parts' and its own `:mem-gib`. lanes.ae is in no key, and lanes-test holds the committed records to it.
`gate-tests-test` plans gate-tests in the full mode. A consequence: rsi-essential at 8 GiB takes rust-full
  out of GTS216's beside-the-serial chains (`:max-step-gib` 4). `crops-test` now expects the chains without it.

**rsi's disk guard** stops past 90% (`RSI_DISK_MAX`), as cppi's does since `e7debd721`.

**Not in GD1, as the survey said.** The main session removes the merged worktrees and caps Go's cache (§3.3, GD1 item 8).
GDS9, GDS11, GDS12, GDS14-GDS18 and GDS20 are GD3-GD6's.

### Before and after (the plans of 2026-11-13 on `3b25e0d43` and on the branch; durations from the store, `--rerun`: every check run)

| plan | steps | serial sum | simulated wall (the essential memory lane) |
|---|---|---|---|
| essential (`bin/gate --plan --rerun`) | 59 → 59 (gate-tests out, claude-md in) | 3:04 → 2:42 | 0:55 → 0:56 |
| `--full --plan --rerun` | 337 → 329 | 32:00 → 25:23 | 22:46 → 18:40 |
| `--nightly --plan --rerun` (the nights from now) | 385 → 377 | 29:04 → 22:28 | 21:45 → 17:37 |
| `--weekly --plan --rerun` (Saturday) | – → 358 | – → 32:01 | – → 26:30 |
| `--full --plan` (keyed, today's store) | 159 run → 295 run | 20:54 → 24:47 | 17:50 → 18:14 |

The keyed row runs more steps after GD1, because GD1's input edits rekey them once. The wall comes from the plan's
simulation, which uses the essential memory lane's free rule, not `--concurrent`'s lanes. The 23 parts with no pass yet
count as 0: the `.weekly` parts and the new `claude-md`.

### Fan-out (steps a change to one file reruns; their wall is night 13's, from `costs-2026-11-13.tsv`)

| file | before | after |
|---|---|---|
| `test/repr/c/annot-known.edn` | 149 steps, 11 h 54 | 24, 0 h 59 |
| `test/repr/c/roundtrip-known.edn` | 149, 11 h 54 | 24, 0 h 51 |
| `test/c/check-budget.edn` | 149, 11 h 54 | 35, 2 h 33 |
| `test/c/check-linux-amd64.edn` | 150, 12 h 46 | 36, 3 h 26 |
| `arbace/cpp/abi.ae` | 50, 9 h 34 | 45, 5 h 06 |
| `arbace/go/escape.ae` | 222, 23 h 59 | 213, 18 h 43 |
| `arbace/asm/amd64/asm.ae`, `arbace/repr/asm.ae` | 201, 22 h 52 | 24, 1 h 19 |
| `arbace/rust/check.ae` | 201, 22 h 52 | 10, 1 h 39 |
| `arbace/llvm/check.ae` | 201, 22 h 52 | 19, 1 h 34 |
| `CLAUDE.md` | 1 (gate-tests), 52 min | 1 (claude-md), seconds |

For `arbace/go/escape.ae` the count still includes the program hosts. The `:never-loads` for goi is GD3's (GDS9).

### Checks run on the branch

All of these pass:
- `bin/gate-tests`: 35 of 35, gate-reads-test among them. It holds gate-tests' narrowed inputs to what the step reads
  under strace.
- `gate.crops check`: 0 problems.
- `test/gate/crops-test.bash`, `test/gate/gate-key-crops-test.bash` and `test/gate/lanes-test.bash`.
- `bin/gate --full --mem-check`.
- `bin/gate --full|--nightly|--weekly --plan [--rerun]` and `bin/night --plan --tier weekly`.
- `make check-makefile`.
- `test/docs/claude-md-test.bash`.
- `bin/reap`: 0 problems.
- `bin/crops-score --judge --strict`.

No gate was run.

## As built: GD2 (branch `gate-gd23`, 2026-11-13)

GD2 makes the nightly tier's subsets with one stated rule (GDS5 (a), GDS6 (a)). `bin/gate-subsets` (`test/gate/subsets.ae`,
`gate.subsets`; `make gate-subsets`, `make gate-subsets-check`) regenerates the committed lists byte for byte from the records;
`--check` fails on a difference.

The rule, per rotated part:
- the universe is the planner's own reading of the `:rotate`'s record (`gate.plan`'s `record-ids`, so the two cannot drift);
- kept, never dropped: the controls' ids (`:always`); the ids a class of the level names (Java's `:tests` and `:examples`; for C
  and C++'s round trip every non-pass row of the level's record, its `:known` entries); the ids that ever failed in the rotation
  (`test/gate/failed-ever.tsv`, a snapshot of the store's `_rotation/*.failed` that `bin/gate-subsets snapshot` only extends: 18
  lines); one representative per class held by `:match` alone (its member with the lowest SHA-256);
- then each area is filled to a fifth, ceil(n/5), counting the kept ones, in ascending SHA-256 of the id. The area: for Java the
  corpus's `:areas` prefix plus `:dir`'s next component (`jdk/java/util`, `hotspot/jtreg/compiler/c2`); for C the file's directory;
  for repr/go the key's first directory; for C++ the corpus and its first directory.

The rest rotates through the seven slices as before, so a night runs the subset and about a seventh of the rest: some 30% of a corpus.

| part | universe | before | after |
|---|---|---|---|
| java-interp.jdk-run (`:subset-interp`) | 1,498 | 1,189 | 304 (20%) |
| java-compiled-full.jdk-run (`:subset-compiled`) | 1,498 | 1,190 | 304 (20%) |
| java-interp / -compiled-full .jdk-other | 545 | 490 / 520 | 137 / 157 |
| java-{interp,compiled-full}.langtools-run | 515 | 136 / 89 | 158 / 159 |
| java-{interp,compiled-full}.hotspot-compiler | 697 | 100 / 75 | 162 / 161 |
| java-{read,classes,print}.* (`:subset`, new) | as above | whole | 304, 121, 144, 161, 29, 4 |
| java-diagnostics.langtools-diagnostics | 1,194 | whole | 239 |
| c-{tokens,parse,annotations,roundtrip}.torture-execute | 1,672 | whole | 336 |
| c-{tokens,parse,annotations,roundtrip}.torture-compile | 1,791 | whole | 359 |
| repr-go-native.test (and the three other test parts) | 1,707 | whole | 346 |
| repr-go-native.amd64 (and arm64, linux-*) | 378-382 | whole | 93-94 |
| cpp-dg.dg-error-own | 4,564 | whole | 931 |
| cpp-roundtrip.gxx-old-deja-every-10, c-c++-common-every-10 | 323, 326 | whole | 72, 71 |

Where the kept ids cover a corpus it still runs whole (gcc-embed's tokens and parse levels: all 47 files `:known`). Small corpora keep
more than a fifth, as ceil rounds up in small areas (hotspot-runtime 29 of 55). The Java subsets replace GTS205's coverage subsets
(1,954 and 1,905 tests). java-compiled-full.jdk-run's part declares 31 GiB (night 13's 28.2, ⌈1.1 ×⌉) in place of the step's 50,
so that no nightly check declares more than 40 GiB but repr-go's 42 (the user's (3)).

`--nightly --concurrent --plan` is now allowed (a plan, never a run), to measure the nightly tier in the memory lanes.

### Before and after (the plans of 2026-11-13, GATE_NIGHT=20405, `--rerun`: every check run)

The base is the branch's first commit (`gate-gd1` plus `bin/night`'s weekly guard). The durations come from the store. One pass is
left out (the `fixed` columns): rsi-essential's pass of 2026-11-12 03:33, 19,767 s against its other pass's 1,903 s. With two
passes the view's median is the larger one, so that outlier put 5 h 29 min into every plan. The view now estimates a rotated check as
its part's duration times its list's share of the universe (`est. N/U of PART`), the same rule before a subset changes and after;
the base's view used the subsets' own passes.

| plan | checks | serial sum | wall, the essential memory lane | wall, `--concurrent` lanes |
|---|---|---|---|---|
| `--nightly`, fixed | 377 → 493 | 17:30 → 14:10 | 12:39 → 8:24 | 7:39 → **3:56** |
| `--nightly`, the store as it is | 377 → 493 | 22:28 → 19:08 | 17:37 → 13:21 | – → 7:56 |
| `--full`, fixed | 329 → 331 | 20:26 → 20:53 | 13:42 → 14:09 | 7:01 → 7:09 |
| `--weekly`, fixed | 358 → 338 | 27:03 → 27:02 | 21:02 → 19:17 | – |
| essential | 59 → 59 | 2:42 → 2:42 | 0:56 → 0:56 | – |

- The nightly tier is now within GDS2's 6 h of wall in the lanes (3:56), but only with the outlier left out. In the serial model it
  is still 8:24.
- `bin/gate --nightly` runs without `--concurrent`. To run in the lanes it needs the runner to accept `--nightly --concurrent`;
  GD4's runner work decides that.
- In the nightly plan, Java's share of the steps' minutes fell from 5:56 to 1:57.
- `--full` grew by two checks, the fixpoint and programs-full: their `:also-changed` files differ from their latest passes. On
  their first pass after GD3 that is once.
- `--weekly` lost the parked parts (GD3).

## As built: GD3 (branch `gate-gd23`, 2026-11-13)

The user (2026-11-13) accepted GD1's open points and asked for GD2 and GD3 at once, on one branch from `gate-gd1`. GD3 changes the
planner (`test/gate/plan.ae`), the audit (`test/gate/audit.ae`), `bin/gate`'s option parsing, `bin/night` and the table. It moves
every step's key once (the epoch replaces the logic's three files, the closures narrow the file lines) and `gate_key` not at all
(`126c321af14f…`, `bin/arbace-go --gate-key` on the base and on the branch; `test/gate/gate-key-crops-test.bash` passes).

**GDS11 (a): the epoch.** Every key held the text of `bin/lib/gate-run.bash`, `bin/lib/gate-cache.bash` and `test/gate/plan.ae`.
It now holds one file instead, `test/gate/epoch`: a number and a dated line why, bumped by hand when a change to the runner, the
planner or the cache library alters what a step sees (its command line, environment, inputs, or how its verdict is judged). A
change that alters none of it (a message, the schedule, the plan's printing) drops no pass. `test/gate/plan-logic-test.bash` holds
the planner to the one line. The bound on a missed bump: gate-tests proves the runner, and the weekly `--full` and the clean run at
a tag run every check anyway.

**GDS9 (a): the load closures.** `test/gate/loads.edn` holds, per audited step, the files under `arbace/` it read (`:reads`) and the
namespace families it never loaded (`:never`: `:go` for `arbace.go`, `:c` for `arbace.c` and `arbace.repr.c`). `bin/gate
--loads-update [LOGDIR...]` (`gate.loads`, `test/gate/loads.ae`) makes it from the audited runs' `NAME.paths` and `NAME.loaded`; by
default from every clean run's logs of the main checkout, oldest first, then the latest `--audit`. The committed file comes from the
eleven clean runs of 2026-10-29 to 11-06: 235 steps and parts.

Why the reads and not `:tools`: stage 0 and the executables read a tool namespace from the checkout only to compare it with the
embedded copy (`RT.load`'s tool source rule), and they read it whenever they load it. A file under `arbace/` that a step never
reads cannot change what it does. A file whose namespace is not a tool source is never read by stage 0, and it reaches a step only
through a host's identity (`:gate`, `:prog`), which keys it as before.

For a step with an entry (the step; a rotated check's part or `.sub`; else the union of its step's parts; else its step):
- its file lines under `arbace/` are the closure: its reads, and every source they name, transitively (each `arbace.NAME` in a
  source's text whose file exists, and each `(load "PATH")`). The text scan catches a namespace required after the audit, also a
  `(require 'NS)` in code: `arbace.go.run` loads `arbace.go.escape`, added on 11-06 after the last clean run, that way;
- on `:prog` or `:javac-prog`, the families it never loaded join its `:never-loads`, so their units leave the host's identity and
  their sources its files. goi's units (`arbace.go.*`, 14 in each program) leave the key of every program-host step that never ran
  goi.

A step with no entry keeps its whole declaration. 116 checks of the full plan have none (the crops' steps that joined the table after the
last clean run: C++, Scheme, OCaml, LLVM IR, Rust, Haskell, asm). The next clean run, or `bin/gate --audit STEP...`, writes their
logs, and `bin/gate --loads-update` adds them.

The audit holds the closure honest. It declares a step's arbace/ files by the same closure, so a read outside it is undeclared and
fails the audit, and it holds the step to the augmented `:never-loads`, so a load of a dropped family fails too. The remedy is
`bin/gate --loads-update` with that run's logs.

**GDS4 (b): `:phase :weekly`.** The weekly tier is now a phase, not `GATE_ALL_PARTS`:
- a step or a part declares `:phase :weekly`. The planner plans it with `--weekly` (`bin/gate --weekly`, the clean run and its plan,
  `--audit`) as a `:full` step in the table's order and chain;
- a `:weekly` step with `:also-changed RULES` also runs on any other night of `--full` or `--nightly` when the files those rules
  select changed since its latest pass, or it never passed. The rules' files join its key, so the pass's material names them. The
  fixpoint and programs-full declare `["arbace/j2g/" "arbace/repr/go.ae" "arbace/repr/go/" :build]` (the user's (2)). The plan's
  why says which files changed;
- a `:needs`, `:uses` or `:start-after` naming a weekly step a plan leaves out is dropped. The declared `:needs` stay in the key
  (`:keyed`), so the key is the same on the weekly night and the others;
- `bin/gate --weekly` no longer sets `GATE_ALL_PARTS=1`. The parked `:clean-only` parts (the user's of 2026-11-12/13: haskell-roundtrip,
  rust-roundtrip, rsi-check-ui, repr-java-{attr,roundtrip}.jdk, ...) are the clean run's alone again.

GD1's weekly list moved from `:clean-only` to `:phase :weekly`: repr-java-{front,nojavac}.jdk (parts), rsi-check (the step, both
parts), cpp-roundtrip.libstdcxx-testsuite-every-40 (a part), haskell-target-probe, llvmi-aot, fixpoint and programs-full (whole steps
again, their one-part `weekly` form gone). `gate.costs` counts a weekly row as the full tier's and leaves it out of the nightly sums
(marked `(weekly)`). `gate.crops concurrent` takes weekly steps as full ones.

**bin/night's fast-forward on a weekly night.** The user's (1): no fast-forward on a weekly night until `:phase :weekly`. The
branch's first commit (`e51ba2698`) makes `bin/night` honour that on its own, so it can go ahead of the rest. With `:phase :weekly`
built and the parked parts out of the weekly tier, a weekly pass fast-forwards again; `NIGHT_WEEKLY_FF=0` keeps it from doing so.

### Fan-out (checks a change to one file reruns; the full plans' material, `--full --plan`; wall night 13's)

| file | before | after |
|---|---|---|
| `arbace/go/escape.ae` | 213 checks, 18 h 43 | 149, 15 h 55 |
| `arbace/go/check.ae` | 214, 19 h 35 | 150, 16 h 48 |
| `arbace/repr/c.ae`, `arbace/repr/c/annot.ae` | 318, 23 h 03 | 264, 20 h 21 |
| `arbace/c/run.ae` | 317, 22 h 56 | 263, 20 h 14 |
| `bin/lib/gate-run.bash` | every check (331) | the 22 that list it |
| `test/gate/plan.ae` | every check (331) | the 2 that list it (gate-tests, crops-score) |
| `bin/lib/gate-cache.bash` | every check (331) | the 176 that list it (it is also in `gate_key`) |
| `test/gate/epoch` | – | every check |

For `arbace/go/escape.ae`, 143 of the 149 list it as a file and 5 see it through `:gate` or `:javac`. Of the 143, 116 have no load
record: the crops' steps, which joined the table after the last clean run (C++, Scheme, OCaml, LLVM IR, Rust, Haskell, asm). Their
first audited run (the next clean run, or `bin/gate --audit STEP...` at night) followed by `bin/gate --loads-update` takes them to
the 27 that do read goi's sources. 66 checks left the fan-out: 51 whose closure holds no goi source, and 15 on `:prog` or
`:javac-prog` that never loaded goi (its units out of their identity). Two joined it: the fixpoint and programs-full, in the full
plan this time by their `:also-changed`.
GD3 also showed what the closures cannot do: `arbace/core.ae` (258) and `arbace/repr/go/print.ae` (236) reach their checks through
`:gate` and the program hosts' units, which load closures do not touch.

### Checks run on the branch

All of these pass:
- `bin/gate-tests` (36 tests: 34 in one run, then gate-reads-test and gate-tests-test again after the planner tests of the real table took `GATE_LOADS` none, the closures' text scan reading `arbace/` that gate-tests does not declare), with the new `test/gate/weekly-test.bash`: 19 checks for the weekly phase (a `:weekly` step and part
  only with `--weekly`; `:also-changed` in and out; a dropped `:needs` kept in the key), the epoch (the planner's text moves no key,
  the epoch's bump moves every one) and the closures (reads plus the sources they name, a require in code too; a source outside
  them moves no key; `:never [:go]` takes goi's unit out of a `:prog` identity).
- `gate.crops check`: 0 problems.
- `test/gate/crops-test.bash`, `gate-key-crops-test.bash`, `lanes-test.bash`, `rotation-test.bash` (65 of 65),
  `rotation-controls-test.bash`, `costs-test.bash` and `plan-logic-test.bash`.
- `bin/gate --full --mem-check`: every declaration holds.
- `bin/gate-subsets --check`.
- `make check-makefile`.
- Every plan above.

No gate was run.

## As built: GD4 (branch `gate-gd4`, from `gate-gd23` `dc685b3df`, 2026-11-13/14)

GD4 is §6's "one policy": the disk and the memory are the gate's, in its runner, and the tools' own guards yield to it (GDS12 (a),
GDS13 (a)); the caches have one table that the prune reads (GDS17 (a), GDS18 (a)); the records are judged against their known files
by an essential step (GDS15 (a)). It changes the runner (`bin/lib/gate-run.bash`), the planner (`test/gate/plan.ae`), `bin/gate`'s
option parsing and summary, `bin/night`, the table, and the tools' guards. `gate_key` is the same before and after (`126c321af14f…`,
`bin/arbace-go --gate-key` without `ARBACE_HOST_EXE` on `dc685b3df` and on the branch; `test/gate/gate-key-crops-test.bash`
passes): nothing in `gate_key` moved (no `bin/lib` file but `gate-run.bash`, `ci/` and `repr-c/`, which it leaves out, and no build
script). The epoch goes from 1 to 2, since every step's environment changes (`GATE_STEP_MEM_GIB`, `ARBACE_DISK_POLICY`,
`ARBACE_GOCACHE_KEEP_MIN`): every check runs once, the same once as GD3's epoch 1 if the two land together, which neither has yet.

**The disk (GDS12 (a)).**
- Before each step of the plan (`run_one`'s `disk_admit`) the free disk of the checkout's file system must hold the floor
  `GATE_DISK_FLOOR` (25 GiB) plus the step's `:disk-gib` (new, optional, in no key: the planner's `DISK-GIB` column).
- Below it: the prune's cheap rules once a low spell (`gate_prune --cheap`, logged in `.tmp/gate/disk.log`), then a wait for the
  disk to come back, `GATE_DISK_WAIT` (1800 s) polled every `GATE_DISK_POLL` (30 s). The wait is the run's, counted from the
  spell's start (`.tmp/gate/disk.low-since`), so a night of low disk defers its later steps at once instead of 30 minutes each.
- Then the step is `deferred`: no run, no pass, not `FAILED`; a step whose need was deferred is deferred with it. Once the chains are
  done, `disk_retry` runs the deferred steps again in the plan's order, each checked once without a wait. A run left with deferred
  steps and nothing failed ends `== gate INCOMPLETE: N step(s) deferred for the disk`, exit 3, so `bin/night` fast-forwards nothing.
- After a step, "No space left on device" in its log, its failure lines or its outputs (`GATE_OUT`, where Java's levels save) fails
  it (status 28 when it exited 0), its pass and outputs not recorded, its report saying where. The store held 37 such passes from
  nights 12 and 13 (Java's level steps, the full disk and the 208,000-entry directory index), each of which this rule turns into a
  failure.
- The sampler writes `DiskAvail` beside `MemAvailable` every 5 s in `.tmp/gate/mem.tsv`.
- The gate exports `ARBACE_DISK_POLICY=gate`. `bin/gate-policy disk-ok PCT` (new, in no key) is cppi's and rsi's light-tier guard
  now: it yields under the gate and stops past PCT% outside it, as before. A grep of `bin/`, `tools/` and `test/` for `df` and
  `DISK_MAX` found no third guard; `bin/night`'s 85% start rule stays, and over it `bin/night` now runs `bin/gate --prune --cheap`
  once before deciding.
- Each step's `.mem` and so its record keep busybox time's "File system outputs" as `:fs-out-kib`, the material of a measured
  `:disk-gib` (GDS16's rule 3). No step declares `:disk-gib` yet: the writes are not the net growth (most are temporaries), and a
  declaration from them alone would defer most of the night at today's 30-37 GiB free.

**The memory (GDS13 (a)).**
- The planner admits each check by the larger of its `:mem-gib` and 1.1 times the largest peak of its passes (the larger of
  `:maxrss-kib` and `:tree-peak-kib`), rounded up to half a GiB (`gate.plan/admission`). That figure is plan.tsv's `MEM` column, by
  which `bin/gate-lane` admits the step in both lane modes and the report flags a peak over it. Two columns join before the command,
  `DECLARED` (the table's `:mem-gib`) and `DISK-GIB`. An undeclared step with passes is admitted by its record alone.
- A check whose passes are over its declaration by the margin (10% and 0.5 GiB, `--mem-check`'s) twice or more is printed by the plan
  as `UNDER  NAME: declared D GiB, N passes above it (the largest P GiB): admitted at A GiB; declare :mem-gib ⌈1.1 × P⌉`, for the
  morning's table edit.
- The runner exports the admission into the step as `GATE_STEP_MEM_GIB` (not for an undeclared, unrecorded step).
  `bin/gate-policy mem DEFAULT` caps a tool's `GOMEMLIMIT` default at it, and prints DEFAULT unchanged outside the gate. It replaces
  the 29 defaults of 3 GiB and more in rsi, cppi, pyi, ci's and the crops' c2j checks and repr/c's annotations (`${X:-6GiB}` becomes
  `${X:-$(bin/gate-policy mem 6GiB)}`; a tool's own variable still wins). The 1,500 MiB-and-under defaults (oi, si, repr-ocaml,
  repr-python, the C++ readers) are left: at or below the admission of any step that runs them. `bin/j2g`'s 20 GiB is left: the file is in
  `gate_key` (window W). The runner never exports `GOMEMLIMIT` itself: a step's processes run in parallel, and a step-wide soft limit
  per process would raise the limit of every tool that defaults lower (limit-test holds `bin/lib/limit.bash`'s default too).

**`--nightly --concurrent` as a run.** `bin/gate --nightly --concurrent` runs (GD2 allowed its plan alone), and `bin/night
--concurrent` (`make night CONCURRENT=1`) runs it, so the night can take the lanes' planned wall. The main session's slot chooses.

**The caches' table (GDS17 (a), GDS18 (a)).** `test/gate/caches.edn` is in no key. It has 97 rows, one per line, and the runner's
`caches_rows` reads it with awk, so the prune works before stage 0 and with a full disk. caches-test also reads it with the Clojure
reader. A row names a path (`.tmp/NAME` with a glob, or `{gocache}`), where it is (`:checkout`, `:main` or `:home`), its owner and its
rule:
- `[:size G :lru]`: Go's build cache to 8 GiB, `.tmp/gocache` and `.tmp/repr-go-print-cache` to 2 GiB each;
- `[:age D]`: TMPDIR's files after 2 days, `.tmp/shared` after 30;
- `[:key-gen N]`: `.tmp/goi-table`'s generations, the newest 2;
- `[:age-self D]`: the gate's tests' scratch directories;
- `:builtin`: the store, the executables and ci's runtime, by `gate_prune_store`, GTS27's code unchanged;
- `:keep`: the clean runs' logs, the packs, the locks and every tool directory whose layout mixes keyed builds with corpora fetched
  by pin (there is no network at night, so a removed corpus fails the step);
- `:list`: the worktrees and `keep/`.
- `:cheap true` marks the rows the cheap tier runs; `:max-entries N` warns of a directory over N (`.tmp/java-check` at 10,000);
  `:shared true` marks a content-keyed cache kept once by key.

`bin/gate --prune [--dry-run] [--cheap] [--worktrees]` runs the store's prune, then every row, then lists this checkout's `.tmp`
entries no row names (never removed). `--worktrees` lists the main checkout's worktrees whose branch is merged into `mac/next`
(`GATE_PRUNE_MERGED`) and whose index is a day old (GDS18 (a): the main session removes them). The cheap tier runs:
- at every gate's start, in place of `bin/lib/gocache.bash`'s 90-minute wipe;
- before a step, at most every 5 minutes and one step at a time (`gate_cache_cap`);
- in `disk_admit`.

The gate exports `ARBACE_GOCACHE_KEEP_MIN=7200` (Go's own five days), so `bin/arbace-go`'s build-time trim, which is in `gate_key`,
no longer empties the cache under the gate. Outside the gate it still does (window W). The essential step `caches-table`
(`test/gate/caches-test.bash --coverage`, seconds) holds every `.tmp/NAME` a `bin/` script names (172) to a row. Its inputs are `bin/`
and the table, so gate-tests' key does not take `bin/` whole.

**Shared by key: musl's forms.** `bin/lib/ci/musl read` takes the main checkout's `.tmp/shared/ci-musl-forms/KEY` when it is there
and whole (its key, its index listing its units), copying 11 MiB in place of the read that cost a worktree 5 h on 2026-11-13, and
publishes its own after a read, through a temporary name. The forms hold no checkout path. The clean run's clone neither takes nor
publishes (`CI_MUSL_NO_SHARED`). `test/c/musl-lock-test.bash` gains the case and keeps its first three cases on their own copy.
j2g's and repr/go's hosts (197 MiB a worktree, §3.1) were left: their scripts are in `gate_key`.

**records-check (GDS15 (a)).** `bin/records-check` (`records.check`, `test/records/check.ae`; essential step `records-check` on stage
0, beside reap, seconds; `make records-check`) judges every recorded results file (rows `[ID RESULT REASON]`) against the known file
that accounts for its non-passes. The pairs are the `:c-known` sources of `test/reap/cells.edn` that have a `:results` (9: C's four,
C++'s token check, Python's four not recorded yet), and `test/records/pairs.edn` adds what the cells do not say. For C's tree check
and annotations that is the token level's known file (`:also-known`). The tree check's mutants are excluded, since their `:known`
goes through a generated list. The problems:
- known-unlisted: a `:known` row that no section lists. reap does not see this one;
- stale: a known entry whose row passes;
- unclassed: a row neither `:pass` nor `:known`;
- lacking: a known entry missing from results that hold its corpus.

The negative controls always run: on mutated copies, the judge must find each of the four. On the tree as it is, it found 41
known-unlisted rows:
- annot's two torture-compile rows, pr33855.c and pr54559.c (§5.2's example);
- 39 rows of the round trip: 25 objects-macro-body, 6 self-include, 5 eval-limits, 1 run-address, 1 split-declaration.

They are `test/records/accepted.edn`'s: printed and not failed, C's to clear at its next `--record`. An accepted entry that stops
being a problem fails the check until it is removed, so the list only drains. `bin/records-check --install-hook` (`make hooks`, the
user's to run) writes an opt-in pre-commit hook that runs the check when a commit touches a known, results or divergences file.

**The plan view's median (gate-gd23's note 6).** An even count of passes takes the lower middle one (`gate.lanes`). With two passes,
the upper one had put rsi-essential's 19,767 s outlier into every plan.

**Not built (GD4's part of §6 left).** GDS14's may-pass time classes (not in this brief); GDS13's "the morning's table edit takes
it in" is the main session's, from the plan's UNDER lines; the shared host builds (j2g, repr/go) and `gocache.bash`'s rule outside
the gate wait for window W (`gate_key`); records-check's count and provenance pairs (repr-clojure's `:counts` need a run to derive;
the provenance pairs are `bin/provenance-check`'s, essential already); the runner's end trap (§5.4 #17).
