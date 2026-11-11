# Agenda

The current state of the work (as of 2026-11-09). This file is rewritten as things change. For the history, see
[JOURNAL.md](JOURNAL.md) (since 2026-11-07), the journals at the tags
[`arbace-reaps-4-crops-v1`](https://github.com/arbace/arbace/blob/arbace-reaps-4-crops-v1/doc/JOURNAL.md)
(2026-11-01 to 2026-11-07),
[`arbace-reaps-3-crops-v2`](https://github.com/arbace/arbace/blob/arbace-reaps-3-crops-v2/doc/JOURNAL.md)
(2026-10-27 to 2026-11-01),
[`arbace-reaps-3-crops-v1`](https://github.com/arbace/arbace/blob/arbace-reaps-3-crops-v1/doc/JOURNAL.md)
(2026-10-20 to 2026-10-27),
[`arbace-interprets-go1.27.1-v1`](https://github.com/arbace/arbace/blob/arbace-interprets-go1.27.1-v1/doc/JOURNAL.md)
(2026-10-15 to 2026-10-20) and
[`arbace-inside-itself-v1`](https://github.com/arbace/arbace/blob/arbace-inside-itself-v1/doc/JOURNAL.md)
(2026-10-11 to 2026-10-15), and the frozen branches' (before).

## State

- **The seed** is unchanged since the tag: `seed/arbace-seed-go.tar.xz` `d5548e72…` (2026-11-05, verified: stage 0 = G1 =
  G2 = G3). Stage 0 runs its own compiled copy of every embedded namespace but the tools' (`RT.toolSource`: `arbace/{j2g,
  repr,build,c2j,go}/`), so two stopgaps stand until the re-seed W: `bin/lib/ci/fresh.bash` (CS281 (c): ci evaluated from
  the checkout) and java-night's stage-0 patches (`annot-lenient`, `build-classes-host2`: F14's analyzer on the old stage 0).
- **The branches.** `main` is at `2a72c94a` (the last gated `next`, 2026-11-08). `next` is about 240 commits ahead,
  ungated (night 9 was deferred by the user, 2026-11-08). `java-night` is `next` plus the Java slices that need a
  conversion night (below); it takes every `next` as it moves.
- **The REAP matrix** (`bin/reap`): eleven rows (Clojure, Java, Go, C, C++, OCaml, Scheme, Haskell, Rust, LLVM IR,
  Python), 44 cells, 41 checked, 0 problems; "no check yet": Rust's AOT (tonight's record) and Python's eval and AOT (its
  records night).
- **The gate** (at the morning's merge, `bin/gate --plan`): the essential tier 49 steps, about 44 minutes; `--full` 316
  steps, about 9:52 simulated serial; the oracle tier (`--oracle`, the host toolchains) 64 steps. The crops' fragments
  spliced: C++, Scheme, OCaml, LLVM IR, Rust, Haskell; Python's after its records night.
- **The host** (Alpine edge, amd64, /dev/kvm; [ALPINE.md](ALPINE.md)): 62 GB of memory; the disk at 79% (the night's steps
  stop at 90%; agents' rule 80%). The Mac peer, an arm64 VM, develops the Python crop through the queue.
- The JVM runs only the frozen jar of `arbace-for-java-26-v4` (the oracle's reference, the edges' recordings, the data
  generators); repr/java and javac run on javac's tool executable. `LICENSE.md` holds all licenses.

## The goal: every crop above 90% (the user, 2026-11-07)

"Raise the crops' completeness, maturity and independence above 90%" ([CROPS-SCORE.md](CROPS-SCORE.md): completeness the
REAP cells' pass / (pass + by design + deferred + pending), maturity a checklist measured on the tree, independence the
reviewed facts of `test/crops/independence.edn`, a test's own verdict 1, a recorded toolchain reference 0.5, a live one 0).
The scores of `next` (`test/crops/scores.edn`, `bin/crops-score`; in brackets completeness without by design, CSP4):

| crop | completeness | maturity | independence | below 90 |
|---|---:|---:|---:|---|
| Clojure | 99.0 (99.4) | 100 | 100 | |
| Java | 72.9 (80.4); 77.2 on `java-night` | 100 | 99.7 | completeness |
| Go | 97.0 (100.0) | 100 | 100 | |
| C | 96.0 (99.6) | 100 | 99.7 | |
| OCaml | 84.2 (96.4); 82.5 before OC222 (a) | 100 | 51.3 | completeness, independence |
| C++ | 76.3 (96.8); 89.1 (97.8) / 71.3 on `cpp-print-own` | 100 | 67.8 | completeness, independence |
| Scheme | 97.4 (98.9) | 100 | 100 | |
| Haskell | 96.3 (98.4) | 100 | 78.9 | independence |
| Rust | 75.0 (75.0) | 95.8 | 80.0 | all three |
| LLVM IR | 97.6 (98.5) | 100 | 97.1 | llvm-ind2 (LLS47 (a), LLS50): judge 0.8541 (52 split-file sections by their parent's RUN lines), independence 97.08; left as decided: the probes (LLS48), llvmi's 177 (LLS49) |
| Python | 0 | 83.3 | 20.0 | all three |

Six crops are above 90 on all three. What the others still need:

- **Java** (completeness): eval and AOT carry about 1,100 deferred cases each, print 616 by design. The slices on
  `java-night` (JC9's JCA, logging and natives, F9, F10b, F14, F26 java.lang.classfile, F27 java.rmi and JMX, java.net.http,
  serialization JCS167, the test library JCS330-JCS339) land with tonight's conversion; their gains count only once night
  11 records the whole corpus. Next: F15 (java.lang.invoke, `java-invoke`, for the next Java night), JCV11 (b) (the print
  cell judged by the round trip's own checks, class files a sub-level), the rest of
  [classes/JAVA-DEFERRED-TRIAGE.md](classes/JAVA-DEFERRED-TRIAGE.md)'s ranking.
- **C++** (completeness, independence): read 1,182 and print 1,665 by design are most of the gap (96.8 without them); the
  print cell is on the own round trip on `cpp-print-own` (CP243 (a): the records made by day, print 48.3 → 99.5, the judge
  0.735 → 0.907; CP352 fixes the five reader gaps of the 15 forms-differ on `cpp-cp352`: 10 left, the dg-error files). Independence: the
  read stage 0.66 and eval and AOT 0.5 (the programs run from clang's IR packs); CX12's own front end (`dg --front
  native`, tonight's record) and CP240 (b), the programs by the own route without clang's IR (`cpp-route`), lift them
  ([cpp/CPP-SURVEY.md](cpp/CPP-SURVEY.md), [cpp/CODEGEN.md](cpp/CODEGEN.md)).
- **OCaml** (completeness, independence): eval's 207 and AOT's 202 by-design cases and AOT's 64 deferred; OC202 (a)
  (about 29 AOT passes, after OC185's switch) and the native records (tonight) move the AOT stage from 0.5 to 1
  ([ocaml/OCAML-SURVEY.md](ocaml/OCAML-SURVEY.md), [ocaml/DEFERRED-TRIAGE.md](ocaml/DEFERRED-TRIAGE.md)).
- **Haskell** (independence, 76.0): eval and AOT 0.5, the programs run over the STG GHC made. The front end compiles
  ghc-internal bottom-up on its own interfaces (258 of 265; the `--full` step `haskell-lib-check-own`, HF10-P11), the default
  methods' unfoldings after its Simplifier (HF10-P12: lib-stg 5,398 of 7,627 bindings equal to GHC's -O0, ahead of GHC's
  interfaces), and base on that pool (HF10-P14: 235 of 241, `hs-p11`); HF10-P15 to P27 decided (a) (the user, 2026-11-10; P25 yes):
  `hs-p18` built P18 (GHC.Internal.Prim's interface from primops.txt.pp, 3,085 of 3,085 facts and every line but the header equal to
  GHC's: no GHC interface left on the own path), P15 (`haskell-lib-own-base`, on haskell-lib-check-own's pool) and started P20;
  `hs-p20` carried P20 on (sixteen run-breaking classes fixed in the front end, lib-stg 5,473 of 7,637, 0 worse): the whole own set
  runs 40 of run-check's 63 (0 of 10 before), the `equal` set 50 (83 modules), both recorded as run rows; P20's proof (all ≥
  equal, 0 lost) not reached: 10 lost in three classes (a case on the void token, output differences, NonTermination); next
  P20's tail, P21, P19 with P22-P23 (the first own cases: what moves eval and AOT to 1)
  Eval: HES90 (2026-11-11, `hs-hes90`): a test with `ignore_stdout` is judged without its stdout, as GHC's driver does (conc065 and
  conc066 pass: 811 programs pass, 0 unclassed).
  ([haskell/HASKELL-EVAL-SURVEY.md](haskell/HASKELL-EVAL-SURVEY.md), [haskell/FRONTEND-SURVEY.md](haskell/FRONTEND-SURVEY.md)).
  hsi's eval cell (`hsi-e1`, HES88): 797 to 809 of 908 (HE9's six recorded, the static pointer table, forkOn#'s locked threads,
  the SIMD primops); 99 left in classes (60 no reference, 12 speed, 10 Cmm); HES89 (`simd/` in the corpora) and HES90 (all.T's
  `ignore_stdout`) proposed; rts_clearMemory's record for a night.
- **Rust** (all three): the AOT cell has no record yet (RE4's `bin/rsi check --aot --record`, tonight: completeness and
  maturity rise with it); eval 0.5 until rsi runs the native front end's MIR (RF10b's passes: 506 of 616 bodies equal;
  RF11, `rust-rf11`). Since RFP53 (a) (the user, 2026-11-10; branch `crops-rulings`) eval and AOT are case-weighted: a case
  scores 1 when all seven inputs C1-C7 are own (`test/rust/eval-inputs-linux-amd64.edn`, 0 of 5 today), so 37 of 73 own in
  both cells give 90.1 (`.tmp/plans/RUST-90-PLAN.md`, S6) ([rust/FRONTEND-SURVEY.md](rust/FRONTEND-SURVEY.md),
  [rust/RUST-EVAL-SURVEY.md](rust/RUST-EVAL-SURVEY.md)).
- **OCaml, OC222 (a)** (the user, 2026-11-10; branch `crops-rulings`): the 33 tests ocamltest itself skips on the pinned
  configuration are `:na` (`:config-skipped`, `test/ocaml/ocamltest-config.edn`): completeness 82.5 → 84.2.
- py-aot-classid (2026-11-10): the AOT judge records a classed row's `:class` by its id (`aot.ae` `(:id c)`; test `test/python/aot_judge_test.ae`); aot.ae's digest moves, the AOT packages and runner rerun; no record touched.
- **Python** (all three): the branch `mac/201-python-py7-py11` is not on `next` yet; its read records (240r) are made, the
  rest (stdlib, site-packages, Lib/test, the mutants, the runtime and wheels' outcomes, the AOT runtime and `check --aot`)
  is night 11's. After it: the branch merged into `next` (`merge-py`, the morning of 11-11), the independence facts moved to
  the checkout's (the user, 2026-11-09: 20 → about 79), Python's gate fragment spliced
  ([python/PYTHON-SURVEY.md](python/PYTHON-SURVEY.md)).

## The branch consolidation (2026-11-08/09)

- **On `next`** since the tag: batches 1-4 (the crops' rows and gate fragments, GT9, the oracle-records line, the native
  readers), then the 2026-11-08 burst and the 2026-11-09 day merges: the C triage (c-compl, c-props, c-props2), C++
  (cpp-compl, cpp-cx12, cpp-props to cpp-props3: CX12's own Sema, CTAD, the codegen), Haskell (hs-hes84, hs-hf9, hs-hf10,
  hs-props, hs-props2, hs-hfsi9), OCaml (ocaml-compl, ocaml-props: OC199-OC205), Scheme (scheme-props to scheme-props3:
  the pinned Unicode 16.0 database), LLVM IR (llvm-lls33, llvm-props, llvm-props2: own verdicts), Rust (rust-rf10b,
  rust-props: RF10b, RFP23-RFP41), the gate (go-tests, gate-lane2: GTS255), controls-cg (Clojure's and Go's maturity 100),
  table-ready (W-D13: oi and crt type-check in every configuration of goi's table), night10-fixes; oi-table2 (2026-11-10: ext_final.ae's seven table errors fixed, tamago amd64 and arm64 none).
- **On `java-night` only**: java-rmi (with java-more, java-props, java-j2g, java-refl, jcv7, security, net, serial4),
  java-older, java-classfile, java-http, java-props2, the stage-0 patches (`33d04049c`), and before them F9, F10b, F14,
  JCV2, java-unix, java-serial and java-serial2. They need tonight's conversion (`bin/jrt-convert update`, `bin/j2g-edge
  update`) and reach `next` and `main` when tonight's Java night passes.
- **Waiting**: `mac/201-python-py7-py11` (Python, night 11 then the morning's merge); `java-testlib` (onto `java-night`
  after tonight); `java-invoke` (F15, the next Java night); `window-w2b`, `w-retire`, `eval-speed-w1` (the re-seed W);
  java-props2's commits after the conversion stay on `java-night` for the next Java night.
- Merged branches were deleted (2026-11-09: local 149 → 65, the mirror's 461 → 250); unmerged work stays on the mirror.
  The day's second merge pass (`merge2`, `--dry` first) takes what finishes before 18:00 UTC: java-* branches onto
  `java-night`, the rest into `next`, then `next` into `java-night`; records only with `bin/reap` at 0 problems.

## The nights ahead

The night shift is 19:00-07:00 UTC (overflow allowed to 12:00 for a `--full`). One heavy lane (processes above 8 GiB,
serial, under `bin/heavy-lock`) and light lanes beside it (one process each, at most 1.5-2 GiB). A step runs only when its
estimate fits before its lane's deadline; a crop's records are committed only when `bin/reap` shows 0 problems.

1. **Night 10 (tonight, 11-09/10): the gate, the Java night, the crops' records.** Heavy lane: the essential gate on
   `next` and `main` fast-forwarded; the Java night on `java-night` (the joint javac, the conversion, the edges, the
   build, the slices' smokes, Java's levels over the slices' areas, a failure attributed to a slice and reverted, at most
   four), then the gate on `java-night` and `next` and `main` fast-forwarded to it; Java's area records; C's records
   (musl, tokens, parse, annotations, round trip, check, AOT, dg); Haskell's and the closing gate only if they fit.
   Light lanes: OCaml's records (and OC185's switch), Scheme's, LLVM IR's rebuilt rt pack and families (LLS31 (a), LLS33's
   mutants), C++'s (CX12's own dg record, the own round trips for CP243 (a), cp247's pack), Rust's AOT cell. *Must prove:*
   `next` passes the essential gate (the first since 2026-11-08); the Java slices convert, build and pass their gate.
2. **Night 11 (11-10/11): Java's whole corpus and Python's records.** Java's levels read, interp and compiled over every
   corpus with `--record` (about 9 h, each level one hold of the big lock), then classes, diagnostics and print if they
   fit; Python's records night beside it (its AOT runtime, 13.5 GB, before Java's levels; its check at two jobs), not
   merged by the night; C's leftovers, OC202, Haskell's records, HF10's measures, what night 10 dropped. If night 10's
   Java night did not land: the Java night again and the whole corpus on night 13. *Must prove:* current Java records at
   every level (for `--full`'s `java-judge`), Python's records with 0 reap problems.
3. **Night 12 (11-11/12): `bin/gate --full`**, keyed, with Python's chain for the first time. *Must prove:* the whole
   tree green after the consolidation, so that W's failures can be attributed.
4. **Night 13: W1**, the evaluator's speed alone (`eval-speed-w1` merged by day): the gate, the fixpoint, the bench, W2's
   light checks on the candidate, the keyed `--full`. *Must prove:* G2 = G3 and the gains of
   `doc/go/EVAL-SPEED.md` (on `eval-speed-w1`) with nothing else failing.
5. **Night 14: W2, the re-seed**: C++, Scheme, oi, pyi and ci's back end embedded (`window-w2b`, refreshed by day),
   `toolSource` with `arbace/{c,cpp,scheme,python,classes}/`, the fixpoint, `bin/seed --make` and `--verify`, the new stage
   0, R (`w-retire`: the stopgaps retired), the post-seed gate, `main` advanced. *Must prove:* stage 0 = G1 = G2 = G3 on the
   new seed, and the gate green on it.
6. **Night 15: W2+1**, the keyed `bin/gate --full` after the seed (every stage-0 step moved).

After W: crop merges batched weekly (W-P9 (b)), the Mac VM builds stage 0 from the new seed (W-D8 (a)), and the clean run
with the next tag (W-D7 (a)). The window's plan is `doc/W-WINDOW.md` (branch `window-w`), decisions W-P1 to W-P10 and
W-D1 to W-D13 taken; W-D9 (pruning the disk to 70% before W1) was not accepted, so W1's disk condition is open.

## Being built today (2026-11-09; light, sample checks; for the second merge pass or later)

- C: `c-props3` (CS294, CS295), `c-props4` (CS296 (b), CS297 (a), the Mac's ms_struct request), `c-musl-lock` (stacked on it: musl's index rebuilt when stale, `read` under a lock; night 10's false c-check fails). C++: `cpp-props4` (CP320-CP324), `cpp-route` (CP240 (b)), `cpp-codegen-fix` (the vtables
  in a deterministic order, the codegen record 149 of 149: `cpp-codegen-gen` fails on `next` without it).
- Haskell: `hs-p3` (HF10-P3, the own interfaces). Rust: `rust-rf11` (RF11's first slice); `rust-rf13` (RFP42-RFP47's light
  parts: virtual calls and vtables, codegen's gaps, `panic-vec-remove`; the UI sampling and std's remapping for the night);
  `rust-rf13b` (core's bodies: RF12-P6's six left all equal, core-check 5,398 equal of the 37 modules' bodies, 5,108 before).
  `rust-rf13c` (core's bodies: core-check 5,474 equal, the refusals 117 to 63; RFP52 proposed, the interface pass's extra files).
- OCaml: `ocaml-indep2` (2026-11-10, on `ocaml-indep`: night 10's native-path gaps, TYPER.md §27: the C stubs classed, nondep,
  -safer-matching, Tmc's [@tailcall], id_pos_list, the -I units' shadowing) and `ocaml-indep3` on it (TYPER.md §28: Ctype's
  pattern mode, is_instantiable, mcomp, reify, record_equation; the or-pattern's equations scoped: immediate64 and sexplib0
  pass, the night-10 list closed; night 12 records the native records after the merge); `ocaml-s1` on it (2026-11-10: OC213 (a),
  the read cell's 61 testsuite rejections judged by their own tests, `bin/repr-ocaml own-read`, all pass; OC214 (a), the quoted
  strings' bytes, the print rows at the night's `--record-results testsuite opam-re`; OC209 (a), OC210 (a), the own verdicts' rest;
  completeness 82.5 to 83.0 on its base); `ocaml-s2` on it (2026-11-10: regression_value_kinds fixed, TYPER.md §29, Typecore's
  Ppat_constraint and the alias's type; OC215 (a) with OC199 (a), the C API at the compiled level: CApi over a file the main serves,
  oicapi's hooks and CallRaw, the C API's shards; built and checked light, its compiled runs the night's: `bin/oi check --aot --level
  compiled --only-list test/ocaml/capi-compiled.txt` under the big lock, then the failures' classes); `ocaml-s3` on it (2026-11-10:
  OC216 (a), the small runtime ports: Gc.Tweak, the counters, reachable_words, OCAMLRUNPARAM l, weak keys as weak pointers, Marshal's
  Closures flag; 12 programs at oi's level and 6 at the light AOT level out of their by-design classes, weak_array_par to `:speed`;
  AF_UNIX and signals at the compiled level not built; the compiled level's rows the night's: `bin/oi check --aot --level compiled
  --only-list` of the moved programs, then aot-compiled-known.edn's stale entries out); `ocaml-s4` on it (2026-11-10: OC216 (a)'s
  rest, AF_UNIX sockets and the OS's signals at the compiled level through the main's host file `/proc/self/ocaml-host`, HostSys;
  the light level unchanged, the recvfrom programs `:unix-java` there; the three OC216 timeouts are CPU, not load: classed as their
  gc class's timeouts and recorded; the night's: `bin/oi check --aot --level compiled --only-list test/ocaml/oc216-compiled.txt` and
  `bin/oi suite check --aot --level compiled --only-list test/ocaml/oc216-suite-compiled.txt` under the big lock, then recvfrom_unix's
  and recvfrom_linux's entries out of aot-compiled-known.edn if they pass). `ocaml-props2` (OC191-OC197, OC206, OC207). Scheme: `scheme-props4` (SCS22-SCS24), `scheme-props5` (SCS25 (a), SCS26 (b)). LLVM IR: `llvm-props3`
  (LLS40-LLS42), `llvm-props4` (LLS43 (a), LLS44 (b), LLS45 (a): the step `llvm-heldout`).
- Java: `java-invoke2` (JCS350-JCS356 on F15), `java-testlib2` (JCS334-JCS339).
- goi: `goi-fields` (P7, the Mac VM's: loads and stores by their words, GOI-SPEC.md "Since v1: loads and stores by their
  words"; 4-15x on a field's load or store in plain Go), proven light; its night slot `.tmp/p7-goi-fields/goi-fields.sh`
  (the gate's build, `bin/goi test`, `check`, the interpreted benchmark) on night 12 or 13, merged after it.
- W: `eval-speed-w1` (D-W1, eval-speed rebased onto `next` with `java-night`), `w-retire` (R on `window-w2b`).
- Reviews: today's merged code on `next` (patches for the owners) and an audit of the independence facts.

- Assembly: `asm-ax1` (AX1's first slice, doc/asm/AMD64-SURVEY.md, "As built: AX1"): repr/asm reads GNU as's AT&T text into
  assembly forms and prints it back byte for byte; musl 32/32, `MC/AsmParser` 139/157 and `MC/X86` 464/621 pass, the rest classed
  (Intel for AX3, the error tests for AX2). Proposed AXS17-AXS22. Next: AX2 (LLVM's `.td` table, the machine-level forms, the
  stated encodings and CHECK texts), then arm64's AR1 on the same frame.
- Assembly, amd64's AX2: `asm-ax2` (on `asm-ar1`; AX2's first slice, doc/asm/AMD64-SURVEY.md, "As built: AX2 (first slice)",
  doc/asm/AMD64.md): amd64's instruction table generated from LLVM's X86 `.td` by Arbace's own TableGen (`asm/amd64/table.edn`,
  checked by regeneration; llvm-tblgen's `MatchTable0` its oracle by hand, 23,014 of 23,014 rows), the AT&T matcher and the
  encoder of the legacy, VEX and XOP encodings, `bin/repr-asm table gen|check|oracle` and `encode-check`: `MC/X86`'s stated
  encodings judged by their own CHECK lines, every non-pass classed (EVEX and APX the largest, Intel for AX3). Proposed
  AXS23-AXS27. `asm-ax2b` (on `asm-ax2`; "As built: AX2 (second slice)"): EVEX encoded (opmasks, `{z}`, broadcast, rounding and
  `{sae}`, disp8*N, maps 5-7, `{evex}`): 81,966 of 106,075 stated encodings met (EVEX 49,206), none failing, the rest Intel (AX3)
  and APX. Proposed AXS28. `asm-ax2c` (on `asm-ax2b`; "As built: AX2 (third slice)"): the CHECK text, each matched instruction
  printed in AT&T syntax as llvm-mc prints it from the table's AsmStrings, print methods and printAliasInstr's patterns (the table
  regenerated with them; llvm-tblgen's MatchTable0 and printAliasInstr its oracles by hand): the print level of `encode-check`,
  82,838 of 108,601 CHECK texts met, none failing, the rest classed (Intel 21,318, APX 2,221, directives 1,202, other tools' output
  772, ...); by hand against llvm-mc 23.1.5, 83,157 printed lines of 83,158 llvm-mc's own (the one left an `.ifdef`'d statement the
  check assembles). Proposed AXS30-AXS32. `asm-ax3` (on `asm-ax2c`; AX3, "As built: AX3"): Intel syntax read (`.intel_syntax noprefix`,
  `-x86-asm-syntax=intel`, `ptr` sizes, `[base + index*scale + disp]`, MASM's symbol rule as llvm-mc applies it), matched by the table's
  Intel variant (MatchTable1, llvm-tblgen's 22,609 rows by hand) and printed as X86IntelInstPrinter does: round trip 609 of `MC/X86`'s
  621 (464 before), stated encodings 99,509 met (81,966), CHECK texts 100,884 met (82,838), the new cross-syntax judge 82,765 of 83,505
  AT&T instructions read back from their Intel print, the rest llvm-mc's own asymmetries; none failing at any level, `:intel` empty.
  Proposed AXS34-AXS36. `asm-ax2d` (on `asm-ax3`; "As built: AX2 (fourth slice, APX)"): APX matched, encoded and printed (REX2,
  `%r16`-`%r31`, EVEX map 4's NDD, NF, CCMP/CTEST with `{dfv=}`, `{rex2}`, `{nf}`, `{evex} cmp`, PUSH2/POP2, PPX, JMPABS) and VEX's maps
  5 and 7 (AXS33): stated encodings 106,074 of 106,075 met (99,509), CHECK texts 107,451 (100,884), cross-syntax 86,350 of 87,103,
  none failing, `:apx` empty; the table regenerated (TableGen's let around a class, the shifts by 1 of NDD forms), the `:directive`
  rule taught the matchers' mnemonics (it had hidden 2,322 APX texts). Proposed AXS37-AXS39. `asm-tg` (on `asm-ax2d`; "As built:
  AXS37, AXS38"): TableGen's let order for defs is TGParser's (the table unchanged); `table oracle` compares the conversions, the `:insns`
  fields and both printers too, and found 763 conversions per variant tied the other way round, now buildInstructionResultOperands'
  (encodings unchanged, 106,074 met).
  `asm-ax4` (on `asm-ax3`; AX4's first slice, "As built: AX4 (first slice)", doc/asm/AMD64.md §7): ELF64 relocatable objects written as
  llvm-mc -filetype=obj writes them (`arbace.asm.amd64.obj` over the encoder, `arbace.asm.elf`'s writer and reader; sections, symbols,
  relaxation, the relocations X86ELFObjectWriter chooses, CFI into `.eh_frame`), `bin/repr-asm object|object-check`: musl's 32 and 6
  synthetic objects equal to llvm-mc 23.1.5's recorded objects byte for byte, `MC/X86`'s object tests 30 of 47, the rest classed (APX,
  macros and `.reloc`, `.loc`), none failing. Proposed AXS40-AXS44 (renumbered at the merge, AXS37-AXS41 on `asm-ax4`).
  `asm-ax4b` (on `asm-ax4`; AX4's second slice, "As built: AX4 (second slice)", doc/asm/AMD64.md §8): Arbace's static linker
  (`arbace.asm.link`: ELF64 objects → a static ET_EXEC as lld lays one out: COMDAT, symbol resolution, lld's sections and rank, merged
  pieces, `.eh_frame`, `.got` with GOTPCRELX relaxed, TLS, program headers, `bin/repr-asm link`), and `bin/repr-asm run-check`:
  `test/asm/run/`'s 18 programs (P4's, musl's crti/crtn, string and setjmp functions among them) assembled, linked and run by the CPU,
  18 of 18 by their own verdicts; GNU ld by hand agrees on every run. Freestanding only (musl's libc.a waits for AX7). Proposed
  AXS45-AXS50.
  `asm-ax4c` (on `asm-ax4b`; AX4's third slice, "As built: AX4 (third slice)", doc/asm/AMD64.md §9): the decoder, LLVM's X86
  disassembler rebuilt from the table (not x86asm: AXS63), `bin/repr-asm decode|decode-object|decode-check`: MC/Disassembler/X86's 186
  tests 24,441 of 24,445 CHECK lines (4 `--mdis` classed), every encoding encode-check meets decoded back (99,505 of 99,509, 4 by
  design), musl's objects read back (479 of 479); eight controls; a TableGen fix (`let` around a `class`). AX4's exit met. Proposed
  AXS63-AXS67. Next: AX5's x86i over this decoder (AXS67 (a)).
- Assembly, AX10 (`asm-ax10`, on `asm-ax5c` with `asm-ar2b` merged; the user's of 2026-11-10, CSP7 reversed; doc/asm/AMD64-SURVEY.md,
  "As built: AX10"): the rows `:asm-amd64` and `:asm-arm64` in REAP (47 of 52 cells checked, 0 problems: x86-64 all four, arm64 read and
  print), both crops in crops-score (x86-64 97.9 / 100.0 / 100.0, arm64 49.9 / 91.7 / 60.0), their gate steps spliced from
  `test/gate/crops/asm.edn` and `asm-oracle.edn` (five essential, about 1 min 20 s a chain; `--full` 4 min 48 s nightly with encode,
  table, decode dis and rt and x86i-check `:clean-only`; the oracle tier on `:llvm-mc`), half a share each (`:crops` 11). Next: the
  first passes (two each bind the share, GTS222), AX5.6, AR3.
  `asm-axs89` (on `asm-ax10`; the user's of 2026-11-10, doc/asm/AMD64-SURVEY.md, "As built: AXS89, AXS90"): `bin/repr-asm x86i-check --only LIST`
  (judged on what it ran, with its slice-foreign control), `test/asm/subsets.edn` (13 quick programs nightly, the rest a seventh a night,
  GT8.4's rotation) and `x86i-check` nightly (a night 38 s and 1 min 30 s on average, 3 min 6 s the heaviest slice; whole 8 min 39 s);
  `asm-amd64-encode` out of `:clean-only` into the nightly (9 min 31 s). asm-amd64's full tier is 17 min 25 s the heaviest night (256% of
  half a share, 6 min 48 s; `--costs` says 338%, it does not know the rotation): over, by the encode check. Proposed AXS91: encode-check
  rotated in sevenths (the night at about 8 min 20 s, 122%), or a larger share.
- Assembly, arm64: `asm-ar1` (on `asm-ax1`; AR1, doc/asm/ARM64-SURVEY.md, "As built: AR1"): repr/asm reads arm64's GNU syntax (ELF
  and Apple's spelling) into assembly forms and Plan 9's text lexically, printed back byte for byte: musl 15/15, libgcc's `.S` 7/7,
  `MC/AArch64`'s base 400/409, the box 7/7, Go's testdata 5/5, the rest error tests classed; amd64's AX1 results unchanged. Proposed
  ARS17-ARS23. Next: AR2 (Go's optab and arm64asm as tables, the aliases, the stated encodings and CHECK texts).
- Assembly, arm64: `asm-ar2` (on `asm-ar1`; AR2's first slice, doc/asm/ARM64-SURVEY.md, "As built: AR2 (first slice)"): A64's table
  generated from Go's arm64asm and `sysRegEnc.go` in TamaGo's GOROOT (`bin/repr-asm --arch arm64 table check`), the encoder over
  all 312 argument kinds, `encode-check` judged by the tests' own stated bytes: `MC/AArch64`'s 12,298 stated encodings 7,685 met,
  the rest classed (later extensions, FP16, system registers and operations Go's tables do not name), none failing; CHECK texts
  printed back 12,156 of 12,227 (66 hexadecimal spellings); the 188 error tests' 5,030 `error:` lines classed (refused without
  LLVM's message, feature-gated, relocations). Proposed ARS24-ARS29 (ARS24: LLVM's AArch64 `.td` for the extensions and registers,
  amending ARS5 (a): the user's). Next: ARS24's table, the print by argument kind, the input lines.
- Assembly, arm64: `asm-ar2b` (on `asm-merged`; AR2's second slice, doc/asm/ARM64-SURVEY.md, "As built: AR2 (second slice)"; ARS24-ARS29
  (a), the user's of 2026-11-10): LLVM's AArch64.td read by Arbace's TableGen into `asm/arm64/table.edn` (7,484 instructions, 3,053
  aliases, 1,657 system registers, the features and processors, AArch64AsmParser's messages; `table check` byte for byte, `table
  oracle` against llvm-tblgen 0 differences), the matcher and encoder over it after Go's table, feature sets from the RUN lines, LLVM's
  messages, the print by kind, the input level: encode 12,290 of 12,298 (was 7,685), print 12,222 of 12,227 (was 12,156), input 6,212
  of 6,212 (new), reject 4,176 of 5,030 with LLVM's fragment (was 0), none failing, eleven controls. Proposed ARS30-ARS32. Next: the
  base cross level (ARS30), the directives' features (ARS31), the parser's messages (ARS32); AR3's decoder.
- Assembly, arm64: `asm-ar6` (from `mac/next` `adf65b4fd`; AR6's first part, doc/asm/ARM64-SURVEY.md, "As built: AR6 (first part)";
  ARS42 (b) and AR6, the user's): a64i's coverage record (`a64i claimed`, `test/asm/a64i-coverage-arm64.txt`: 474 of 653 claimed executed), eight seeded
  self-checking synthetic programs (`test/asm/a64i/gen.ae`: the integer families and the scalar FP at both precisions, 13,304 cases, the
  model held to qemu-aarch64 at `--record`), the scalar FP in a64i (`go/arbace/a64i/fp.ae`: FPSR's exceptions, FPCR's modes, DN), and the
  AOT cell's cheap classes (PAuth relocations, label arithmetic, `.reloc`, build attributes, the readobj/readelf/objdump views, FileCheck's
  variables): the AOT cell 78.5% → 95.0%. Proposed ARS43-ARS47. Next: the memory families' synthetic program (ARS45 (b)), the error
  tests' messages (ARS43), the VM's lockstep of the new programs (ARS44).
- Assembly, both arches integrated: `asm-merged` (asm-tg, then asm-ax4b and asm-ar2 merged; doc/asm/AMD64-SURVEY.md, "As built: the
  asm branches integrated"), the one branch for the 11-11 merge in place of the three: REVIEW2's findings fixed (the REX conflict
  resolved by porting llvm-mc's `_rex2`/`_relax_evex` fixup kinds and their CODE_4/CODE_6 relocations, `call *a@tlscall` ported,
  `bin/repr-asm`'s verbs by `--arch`, AX4's AXS37-AXS41 renumbered AXS40-AXS44, the vacuous controls `left`, AR2's record merged per
  corpus with "recorded, not run", busybox awk, ASM's old path), every check run again: encode 106,074 met, arm64 7,685, objects 80
  of 86 byte for byte (the 11 APX object tests now equal), runs 18 of 18, none failing. Then `asm-ax4c` (the decoder) merged on top:
  one `let`-around-`class` fix kept (asm-tg's), the table regenerated from the resolved generator, the table oracle taught `:dis`
  (20,718 of 20,718; `-gen-disassembler`'s instructions the table's 20,715 `:dis` ones), the decode check re-recorded on the merged
  table: `dis` 24,441 of 24,445, `rt` 106,070 of 106,074 (APX now encoded: 6,565 more than ax4c's), `obj` 479 of 479, none failing.
  Next: the 11-11 merge of `asm-merged`; then AX5's x86i over the decoder (AXS67 (a)).
- Assembly, amd64's AX5: `asm-ax5` (on `asm-merged`; AX5.2 and AX5.3's sys mode, doc/asm/AMD64-SURVEY.md, "As built: AX5.2",
  doc/asm/AMD64.md §10): x86i, the x86-64 integer machine, a light driver (`go/arbace/{x86i,emu}` over crt, built by TamaGo's go; crop
  paths, no key moves): the decoder from the table (`arbace.asm.amd64.dgen`), the core integer families, lazy flags, Linux's loader and
  stack, the lockstep's sys mode over ptrace; `bin/repr-asm x86i-check`: 28 of 28 programs (test/asm/run's 16 and test/asm/x86i's 12)
  equal to the CPU's runs and by their verdicts, decoded alike by both decoders, lockstep-equal; ten controls. Proposed AXS68-AXS74.
  `asm-ax5b` (on `asm-ax5`; AX5.3's rest and AX5.4, "As built: AX5.3 and AX5.4"): the lockstep's step mode (every instruction
  compared over ptrace's single step), the loader's judge (it found Linux's segment pages carry the file's bytes), the bisection of a
  sys-mode difference to its instruction; rcl rcr shld shrd, the bit family, enter, xlatb, movbe, rdtscp, cpuid's model; signal
  handlers with Linux's frames, sigaltstack, rt_sigreturn; five seeded synthetic programs (`bin/repr-asm x86i synth`). x86i-check:
  35 programs, 34 equal to the CPU (cpuid `:cpuid`), 35 of 35 step-equal and loaded as Linux loads them; 18 controls. Proposed
  AXS75-AXS81.
  `asm-ax5c` (on `asm-ax5b`; AX5.5, "As built: AX5.5"): the VEX contexts in dgen and the Go decoder; BMI1/2, ADX, cmpxchg8b/16b,
  rdrand/rdseed/rdpid behind the model's CPUID (leaf 7); SSE/SSE2 (moves, logic, shuffles, packed integers, single and double
  arithmetic, comparisons, conversions) with MXCSR's flags in software at round to nearest; XMM and MXCSR in the step mode; seven new
  synthetic families; the per-run coverage (702 of 1,291 claimed, `test/asm/x86i-coverage-amd64.txt`); `bin/repr-asm x86i-decode-check`
  (AXS70 (b): 20,531 byte strings equal to arbace.asm.amd64.decode's, none different, 62,457 EVEX/XOP/REX2 deferred). x86i-check: 43
  programs, 42 equal to the CPU (cpuid `:cpuid`), 43 of 43 step-equal; 23 controls. Proposed AXS82-AXS88. Next: AX5.6's musl
  harnesses (AXS88 (a)).

## Decided, not built yet

The user's decisions not yet in code, by crop; the texts are in the docs named. Every open proposal of 2026-11-08/09 was
answered (2026-11-09: all of Java's, C++'s, Rust's, Haskell's, LLVM IR's, Scheme's, C's and OCaml's as recommended); the
items below are the ones whose work is still ahead or tied to a night.

- **Java** ([classes/JAVA-DEFERRED-TRIAGE.md](classes/JAVA-DEFERRED-TRIAGE.md),
  [classes/JAVA-CONFORMANCE-SURVEY.md](classes/JAVA-CONFORMANCE-SURVEY.md)): JCS169 after serialization's slices 2-4;
  F10b's asynchronous channels after the night's records; JCS190's flip after the night's comparison; JCS231 after F9;
  JCS261 waits; JCS274 (`:nio`'s rest reclassified after the record); JCV11 (b) (reopens RPS9 (a)); JCV12 (b); JCS180
  later; F15 (JCS350-JCS356) for the next Java night.
- **C++** ([cpp/CPP-SURVEY.md](cpp/CPP-SURVEY.md)): CP243 (a) (`cpp-print-own`) and CP352 (`cpp-cp352`, the five valid-code reader
  gaps) merged into next; CP250 when
  needed; CP251 at the next re-record; CP264 a night task; CP240 (b) and CP320-CP324 (above). CP338-CP351 (the user,
  2026-11-10, as recommended): CP338 (a) and CP350 (a) built on `cpp-cp338` (the run corpora's refusals in the plan's order,
  the options our route models under `auto`; its runs by their own verdicts are a night's, the night commands in the survey's
  "As built: CP338 (a) and CP350 (a)"); `cpp-s2` (2026-11-11, "As built: cpp-s2"): the mangler's written qualifiers close P21 (b)'s
  blocker (libsupc++'s 63 units by our route name what clang's IR names; the runtime on auto is the night's), std::thread's tuple
  constraints and shared_ptr's `__has_esft_base` (the 82 run programs 62 → 65 by our route), two CX12 diagnostics (+5 own dg passes);
  CP356-CP358 proposed; `cpp-s3` (2026-11-11, "As built: cpp-s3"): the TLS wrapper, explicit object parameters (by reference),
  `__complex__` and the atomics of doubles, pr77550's five causes (the 82 run programs 65 → 69 by our route, the 65's IR unchanged),
  CP344 (a) (either judge passes a dg file: completeness 90.99 → 91.04), CP348 (a)'s list of the runtime's own units; library.cpp
  (variant, any, format: CP359), coroutines and vectors (CP351) ahead; CP345 and CP349 the night's and P21 (b)'s (CP361); CP359-CP361
  proposed; the night's commands in the survey; CP342/CP343's dg own record on night 13 (its slot prepared); `cpp-s4` (2026-11-11,
  "As built: cpp-s4"): CP359 (a)'s variant and any slices (their names clang's; library.cpp now stops at <format>, the 82 still 69,
  unroll1.C's names now clang's), CP362 (<format>'s consteval check) to CP364 proposed.
- **OCaml** ([ocaml/OCAML-SURVEY.md](ocaml/OCAML-SURVEY.md)): OC202 (a) after OC185's switch; OC182 waits for T12;
  OC191-OC207 (above); OC209-OC222 (a) (the user, 2026-11-10: the plan to 90%): S1 with OC209, OC210 on `ocaml-s1`, OC215 built
  on `ocaml-s2` (its compiled runs a night's), OC216 on `ocaml-s3` and its rest (AF_UNIX and signals at the compiled level) on `ocaml-s4`, then OC217, OC218 and OC219, OC220 and OC221; OC222 (the configuration's skips `:na`) with the scoring's next change.
- **Haskell** ([haskell/HASKELL-EVAL-SURVEY.md](haskell/HASKELL-EVAL-SURVEY.md)): HES89, HES90 (hsi-e1's proposals); HES83 (b) (the own verdict where a
  `.stdout` ships, GHC's references at the oracle tier); HF9-P7 with slice 7; HF10-P20 to P27 (above, the plan's stages 2-9); HF10's lib-stg
  measures on night 11; `bin/gate --full --plan` to confirm `haskell-lib-own-base` plans after `haskell-lib-check-own`.
- **Rust** ([rust/FRONTEND-SURVEY.md](rust/FRONTEND-SURVEY.md)): RF11, RF12 (RES54's ceiling until it); RFP29 (a) later;
  RFP42 (a), RFP45 (a), RFP47 (a) and RFP46 (a)'s whole run at night ("As built: RF13 (first slice)", its night commands);
  core-check 5,576 equal at RF13e ("As built: RF13e": 5,551 by its causes, then RFP52 (a)'s interface list; rust-rf13e on
  rust-rf13d, which carries rust-rf13b's fix and RF13c); RFP52 (a) decided by the user 2026-11-10 (with RFP48-RFP61,
  RF12-P8), its rest (aliases, CopySpec, Drain, ascii::Char) left. RF14 (`rust-rf14` on rust-rf13e, "As built: RF14 (RFP48)"):
  RFP48 (a) built (trait resolution in mono: impls, specialization's leaf, default bodies, the Fn traits' builtin candidates;
  the native callees of the synthetic corpus 3,503 -> 4,079 of about 4,500, vtables 0 -> 25 of 32) and RFP49 (a)'s first part
  (the local non-generic instances std's bodies reach): 48 of 67 synthetic programs own (47). RF15 (`rust-rf15` on rust-rf14,
  "As built: RF15"): RFP49 (a)'s rest (generic local instances: our bodies instantiated; std's vtables' local functions),
  promoted constants (operators, casts, strings, variants, layouts), LowerSliceLenCalls, overloaded `+=`: 56 of 67 own.
  RF16 (`rust-rf16` on rust-rf15, "As built: RF16 (RFP58 (a))"): RFP58 (a) built (repr(simd) layouts in mono, the private SIMD
  types read from the library's sources, vectors indirect in codegen, the generic SIMD intrinsics; ci and `gate_key` unmoved),
  the HashMap programs' next causes (a constant function pointer's call, a spread memory field, thread-local statics), promoted
  constants further (`ptr::null`, `size_of`, checked operators and asserts, unsizing casts with vtables): 61 of 67 own.
  RF17 (`rust-rf17` on rust-rf16, "As built: RF17"): RF16's causes (local constants and an associated constant evaluated, a
  static with a pointer, generic closures at their parent's arguments in mono, closure types' unification and v0, std's
  constructors' shims, `Variants::Empty`, a reified closure's ClosureOnceShim symbol): 65 of 67 own. RF18 (`rust-rf18` on
  rust-rf17, "As built: RF18"): RFP62 (a) built (opaque types revealed in mono, closures' layouts and `dyn Fn` vtable entries with
  call_once's VTableShim, the record's `dyn Fn(..)` read): 66 of 67 own; RFP63 (a)'s reader (`RF_STD_EXPORTS`). Next: RFP63 (a)'s
  night (the helper's `--std-exports`, every rust-front pack recorded again: panic-assert-eq); the night: the whole plan's
  rsi-build, rsi-native over the own programs RF15-RF18 added, `rsi check --front own`. RF19 (`rust-rf19` on next, "As built:
  RF19"): `Box<dyn Fn>` calls native (the builtin Tuple candidate), collections 2.62 -> 2.09 GB (the whole corpus 2.24 GB at
  RF11_BATCH=3), core's macro-made functions' attributes by `bin/rust-front core-attrs` (native callees 4,528 -> 4,985 of 4,985):
  66 of 67 own, independence unchanged.
- **LLVM IR** ([llvm/LLVM-CROP-SURVEY.md](llvm/LLVM-CROP-SURVEY.md)): LLS28 (a), the 282 modules packed once with clang at
  night; LLS40-LLS42 (above); `llvm-compl2` (2026-11-10): llvmi on the checkout's ci and RES34's cilib, eight Verifier rules
  ported, completeness 97.6.
- **Scheme** ([scheme/SCHEME-SURVEY.md](scheme/SCHEME-SURVEY.md)): SCS2 (a), the Chez dumps pack with the oracle-records
  merge; SCS22-SCS24 (above).
- **C** ([C23-SURVEY.md](C23-SURVEY.md)): CS281 (a) at W ((c) until); CS275 and CS280, the gmp and pcre2 packs at the next
  pack recording.
- **The gate** ([GATE-SURVEY.md](GATE-SURVEY.md), [ORACLE-RECORDS-SURVEY.md](ORACLE-RECORDS-SURVEY.md)): GTS252 (a) later;
  GTS249 (b) for the clean run; Python's planner change (`:slice-re`) with GTS240's runner change.
- **Python** ([python/PYTHON-SURVEY.md](python/PYTHON-SURVEY.md)): the 3.15.2 oracle deferred to the 3.15 move; pyi
  embedded at W2 (W-D1 (a), amending PYS18's "after Python's tag").

New proposals go into each crop's doc under "Proposed"; the user answers them by day.

## The Mac queue (Python)

The VM develops the Python crop on `mac/201-python-py7-py11` (at `2b5c0c07`, 2026-11-09), light sampling only; every
whole-corpus record is the main host's. Done today: 239f (3.14's fixes), 240 (the records mode of the read checks), 245
(the read classes), 246 (py type-checks for tamago, W-D13). Taken: 241 (the cycle collector), 242 (sys's attributes and
sigwaitinfo), 243 (a module's doctests), two side by side. Open, in order: 244 (ctypes over Arbace's own C), 247 (own
verdicts for the 151 programs), 248 (the compiler's view), 249 (sockets, buffers, multiprocessing), 250 (the object
model's edges), 251 (pyi on goi's speed), 252 (3.15's P6-P8).

## Open

- JCS127 (`mac/039-jcs127` `d6870d37`, volatile fields under Rt's striped locks): not merged; after a test on the host.
- GTS137 (a): the `--full` step `repr-java-jdk` (`:heavy true`), at the next reader change.
- `arbace/c/repl.ae`'s back-end default and the judge tests that stand in for the compiled parser: at W.
- The box can no longer build the program: `go/arbace/crt` calls `syscall.Access` and `syscall.Stat_t`, which TamaGo's
  syscall lacks (C9.2's hazard (e)); for the box's revival.
- repr/haskell and repr/rust's round trips stay out of `--full` until their rows fit the budget; Zed parked
  ([ZED-SURVEY.md](ZED-SURVEY.md)).

## Done (brief; the history is in the journals)

- The JVM work to the first freeze (`arbace-for-java-26`, 2026-10-08); B1a, the native Go executable
  (`arbace-for-golang`, 2026-10-11); B1b, the box (2026-10-12); the standalone bootstrap S1-S9 (2026-10-15); goi
  (2026-10-20); the front ends (repr/go's native reader, javac on Arbace, repr/java without a JVM, the Java conformance
  check, 2026-10-24).
- `arbace-reaps-3-crops-v1` (2026-10-27), `-v2` (2026-11-01, the gate made efficient and run clean), and
  `arbace-reaps-4-crops-v1` (2026-11-07, at `a4a84533`): C the fourth row, 16 of 16 cells, an audited clean run of the
  four crops over GT8's compressed corpora within its budget (`test/gate/clean-arbace-reaps-4-crops-v1.edn`).
- Since the tag (2026-11-07/09): seven more rows in the matrix (C++, OCaml, Scheme, Python, LLVM IR, Rust, Haskell), the
  crops' gate (GT9), the oracle tier (`bin/gate --oracle`, the records and packs), `bin/crops-score`, and the consolidation
  above.

## Parked (the user, 2026-10-26; out of `bin/gate --full`, scripts kept, revisited piece by piece)

- The box (with its Mac bundle, the arm64 VM and the KU series), `tamago/` and `tamago/cmd/`'s forms, the macOS user
  interface and the iOS app ([IOS-REPL-SURVEY.md](IOS-REPL-SURVEY.md), IR1-IR13 open).
- KU3 (processes on arm64) and KU4 (the process manager): stopped before they wrote anything (worktrees `ku3` at
  `80294e9a`, `ku4` at `728440fe`); KU3's finding, TamaGo's arm64 `osInit` reading ID registers that trap at EL0, is
  proposal KS15. The KU series waits for the user's direction.
- The Mac GUI app Moshi Go's tooling (the user, 2026-10-13): a pure-Go, cgo-free GUI on macOS, of the same shape as the
  box's launcher there, for when the REPL gets a user interface beyond the terminal.

## Long term

- The standalone Arbace: no Java binary compatibility, with Java at the source level through repr/java. The language
  is Clojure plus the Java forms, and later the Go forms. The goal is a self-sustaining REPL in a virtual sandbox, in
  Arbace down to the bare metal ISA, in the style of Go plus TamaGo. Targets: /dev/kvm on amd64 (Alpine edge Linux
  host) and Hypervisor.framework on arm64 (macOS host). Open: the runtime's design, Go-inspired (scheduler, GC), with
  proper tail calls possible.
- The crops toward independence of every host toolchain: each read, eval, AOT and print stage judged by Arbace's own
  tools and the tests' own verdicts, the toolchains' runs only the oracle tier's records.
- Futamura's projections ([FUTAMURA-SURVEY.md](FUTAMURA-SURVEY.md)): the second projection, j2g derived from the
  interpreter, one definition of each language's semantics; speed (F10's inline caches, specialization).
- A kernel and a user part in the box ([go/KERNEL-USER-SURVEY.md](go/KERNEL-USER-SURVEY.md)): KU0-KU2 done, KU3 and KU4
  parked.
