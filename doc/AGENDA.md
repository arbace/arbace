# Agenda

The current state of the work (as of 2026-11-17 evening; the sections below "The consolidation" are as of 2026-11-10 unless dated). This file is rewritten as things change. For the history, see
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

- **The seed** is `3c48859d…` (2026-11-17, the consolidation's re-seed W: W2's embedding of the crops, pyi, oi and goi's
  listing of `arbace/{py,libm,crt,ci}`; packed from the fixpoint's G3 at `53da41d93`, verified: stage 0 = G1 = G2 = G3). R
  (`w-retire-4`) retired the stopgaps the old seed `d5548e72` needed (`annot-lenient`, `classes_stale`,
  `bin/lib/ci/fresh.bash`). Unchanged by the day's burst.
- **The branches.** main = next = `42bf8614b` (origin and mac) since 19:17 UTC 2026-11-17: the consolidation (`f1969748f`,
  gated by the essential gate on main at 12:33, 126 min, 0 failures) plus the day's burst (45 first-parent commits, below "The
  burst (2026-11-17)"). main reached `42bf8614b` before its full gate ran, by an advance watcher's mistake (it waited on the
  gate's launcher, not the gate); the user kept it (round 52). Tonight's full gate (`bin/gate --full --concurrent` on
  `42bf8614b`, from 19:15, log `.tmp/goal/g-full2.log`) decides the fixes: its failures go to `REVISIT-full2.md`
  (`.tmp/goal/revisit-after-full2.sh`, which moves nothing) and to fix agents in the morning. Held until after it: the branches
  listed in the burst's section (unbuilt compiled or embedded code, or records first).
- **The crops' scores** (`test/crops/scores.edn` on next `42bf8614b`; completeness, without by design, maturity,
  independence): Clojure 99 99 100 100, Java 89 92 100 60, Go 98 100 100 100, C 98 100 100 100, C++ 91 93 100 72, OCaml 86
  96 100 100, Scheme 100 100 100 100, Haskell 97 99 100 76, Rust 75 75 96 80, LLVM IR 98 99 100 98, Python 0 0 83 60 (eval
  and AOT wait for its records night; 98 independence on the held Python line), x86-64 assembly 99 100 100 100, arm64
  assembly 100 100 100 100, WebAssembly 98 98 100 100 (100 with the held 326 and 327). C++'s 93 without by design (from 96)
  is CP398 (a)+(c) and CP399 (a): rows moved from by design to deferred, on purpose.
- **The REAP matrix** (`bin/reap`, `test/reap/matrix.edn` on next): fourteen rows (Clojure, Java, Go, C, C++, OCaml,
  Scheme, Haskell, Rust, LLVM IR, Python, x86-64 and arm64 assembly, WebAssembly), 56 cells, 53 checked, 0 problems; "no
  check yet": Rust's AOT (322 merged, its records after the gate) and Python's eval and AOT (its records night).
  `bin/crops-score --judge --strict` and `bin/records-check` pass.
- **The gate** (next `42bf8614b`, `bin/gate --plan`): the essential tier 52 steps since the crops tier (`gate-ess-diet`, 71 ->
  51, then `rsi-shim-essential`): the core about 27 steps, 29.5 minutes of serial work measured after a re-seed; a crop's
  essential subsets run by day only when its own paths (`:crop-paths`), declaration or environment changed, else `night`
  (deferred to the night's gate); `--full --concurrent` 339 steps, simulated wall 8:02; the oracle tier (`--oracle`) as before.
  The crops' fragments spliced: C++, Scheme, OCaml, LLVM IR, Rust, Haskell, the assembly crops, WebAssembly; Python's after
  its records night (F1 (a): the records first, then the splice).
  Branch `gf-gate` (2026-11-12, night 12's 45 failures): the gate's own bugs fixed (lanes-test and cpp-selftest environment, the
  jdk corpus steps' memory declarations, haskell-lib's chain, repr-clojure's count); haskell-roundtrip and rust-roundtrip out of
  `--full` (their parts `:clean-only`, `GATE_ALL_PARTS=1`) until their records and printers are fixed.
- **The gate's diet** (2026-11-13, [GATE-DIET-SURVEY.md](GATE-DIET-SURVEY.md), branch `gate-diet`): night 13 took 18.6 h for a
  6 h budget, rerunning 311 of 331 steps; GDS1-GDS20 decided as recommended (the user, 2026-11-13). GD1 built on branch
  `gate-gd1` (to merge after main advances): `bin/night` (nightly every night, `bin/gate --weekly` on Saturday's), the weekly
  `:clean-only` parts, gate-tests in the full tier, `:tools` and C's records narrowed, the declarations from `--mem-check`; the
  plans' serial sums nightly 29:04 -> 22:28, essential 3:04 -> 2:42. GD2 and GD3 built on branch `gate-gd23` (from `gate-gd1`;
  "As built: GD2", "As built: GD3"):
  - every rotated subset by one rule (`bin/gate-subsets`). Java's jdk-run fell from 1,189 to 304 of 1,498, and Java's other
    levels, C's read checks, repr-go-native, cpp-dg and cpp-roundtrip rotate;
  - the logic epoch `test/gate/epoch`;
  - the load closures `test/gate/loads.edn` (`bin/gate --loads-update`);
  - `:phase :weekly` with `:also-changed` (the fixpoint and programs-full);
  - `bin/night`'s weekly fast-forward back, the parked parts out of the weekly tier.

  The nightly plan's serial sum fell from 17:30 to 14:10, and its `--concurrent` lanes' wall from 7:39 to 3:56 (rsi-essential's
  outlier pass left out). `gate_key` is unmoved, and every step's key moves once.
  GD4 built on branch `gate-gd4` (from `gate-gd23`; "As built: GD4"):
  - the gate owns the disk: a 25 GiB floor before each step, `deferred` (retried at the end, a run left with some INCOMPLETE,
    exit 3), ENOSPC failing a step, and the tools' guards yielding under `ARBACE_DISK_POLICY=gate`;
  - the gate owns the memory: admission by the larger of `:mem-gib` and 1.1 x the recorded peak, UNDER printed by the plan,
    `GATE_STEP_MEM_GIB` capping the tools' `GOMEMLIMIT` defaults;
  - `--nightly --concurrent` runs (`bin/night --concurrent`);
  - the caches' table `test/gate/caches.edn` and `bin/gate --prune [--cheap] [--worktrees]`, Go's cache capped at 8 GiB by LRU;
  - musl's forms shared by key;
  - `bin/records-check` as an essential step (41 drifted C rows accepted, to clear at C's next `--record`), with the `make hooks`
    pre-commit hook;
  - the plan view's lower median.

  The epoch goes to 2 (with GD3's 1, one rerun if the two land together). `gate_key` is unmoved. Left for window W: j2g's and
  repr/go's hosts shared by key, `gocache.bash`'s wipe outside the gate, `bin/j2g`'s 20 GiB default.

  Next:
  - GD1's and GD23's first nights;
  - audit records for the crops' 116 steps (`bin/gate --audit` at night, then `--loads-update`);
  - GD4 (the runner's one policy, `--nightly --concurrent` as a run);
  - GD5 (split repr-java-nojavac.jdk, 59 GiB).
  Branch `gate-lanes-fix` (2026-11-13, one commit on next ca3c7aff8): gate-tests' nested lanes-test failure was admission
  timing under strace, now robust (conditions, not sleeps); 36 of 36. Branch `gate-gts` (2026-11-13, for after main advances):
  GTS-A (a needed step of another chain waited for, no deadlock; needs-wait-test), GTS-B (C's `:time-dependent` round-trip
  entries, the five restored; gate_key moves: arbace/repr/c), lenc.ae's shadowed `label` (repr-clojure back to 152 outside),
  review 1113's findings 2-4 (finding 2 flags 298 C results rows recorded known but unlisted: reap fails on them until the
  night's C runs re-record), the si bisect (no regression).
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
| Scheme | 98.4 (99.95); 97.4 before `scheme-aot1` | 100 | 100 | |
| Haskell | 96.3 (98.4) | 100 | 78.9 | independence |
| Rust | 75.0 (75.0) | 95.8 | 80.0 | all three |
| LLVM IR | 97.6 (98.5) | 100 | 97.1 | llvm-ind2 (LLS47 (a), LLS50): judge 0.8541 (52 split-file sections by their parent's RUN lines), independence 97.08; left as decided: the probes (LLS48), llvmi's 177 (LLS49); llvm-eh (2026-11-13): llvmi runs modules that unwind with C++'s lowered runtime (16 of cpp-s12's 24 unwinding programs pass, 3 in cppi's classes, 5 links above 2.5 GiB: f8, library.cpp; the 13 `:personality` modules' check and records the night's), LLS51 (b), LLS52 (a), LLS53 (a) decided; the light proofs pass |
| Python | 0 | 83.3 | 20.0 | all three |

Six crops are above 90 on all three. What the others still need:

- **Java** (completeness): eval and AOT carry about 1,100 deferred cases each, print 616 by design. The slices on
  `java-night` (JC9's JCA, logging and natives, F9, F10b, F14, F26 java.lang.classfile, F27 java.rmi and JMX, java.net.http,
  serialization JCS167, the test library JCS330-JCS339) land with tonight's conversion; their gains count only once night
  11 records the whole corpus. Next: F15 (java.lang.invoke, `java-invoke`, for the next Java night), JCV11 (b) (the print
  cell judged by the round trip's own checks, class files a sub-level), the rest of
  [classes/JAVA-DEFERRED-TRIAGE.md](classes/JAVA-DEFERRED-TRIAGE.md)'s ranking. Independence 59.7: read, eval, AOT and print
  0.5 while the platform is the JDK build's (the user's ruling, 2026-11-09); `java-indep` (JCS341-JCS343 built, for the
  11-11 merge) measures the own platform (`bin/javac-vendor classes modules`). The user decided JCS359 (a) staged, JCS360 (a),
  JCS362-JCS378 as recommended and JCV10 (b) (2026-11-10). Stage 1's tooling is on `jcs359-s1` (from `java-indep`): the build's
  configuration as data, `jdk/javac/platform-build.edn` (JCS362 (a)); `classes modules --jobs N`; the known file
  `jdk/javac/platform-own-known.edn` (JCS360 (a), 12 classes); the controls. Stages 2-3's day parts are on `jcs359-s2` (2026-11-11): `bin/javac-vendor gensrc`, the 16 modules' generated
  sources by the build's own tools on javac's tool, 2,305 of 2,307 the build's byte for byte (2 known), the two `gcc -E` ones by
  repr/c (JCS369 (a)); `descriptors`, the 68 module descriptors as the image holds them, 67 equal and java.base known
  (ModuleHashes, JCS364 (a)); JCS389-JCS392 proposed. Stage 1's measure is done by day (h-java-platform, 2026-11-17: all 68 modules, 27,530 classes, 50 modules
  byte for byte, 48 classes `:param-names`, every process at most 2.07 GB under `GOMEMLIMIT=2GiB`); the open parts of stages 4-5
  are JOP1-JOP8 for the user ([classes/JAVA-FRONTEND-SURVEY.md](classes/JAVA-FRONTEND-SURVEY.md), "Own platform"). The user approved JOP1-JOP8 (a)
  (2026-11-17); the switch's tooling is ready on `h-java-platform` (the known file's 48 entries, `platform-own`, the cache built on a miss,
  the weekly rebuild, the oracle step, the audit rule, JOP5's moves, the facts staged); next the switch night (round 51,
  `.tmp/goal/java-platform-night.sh`, about 2 h 10 min), then the clean run that moves Java's independence to 100 (JOP7 (a)).
  Decided too: JOP9 (a) the pack, JOP10 (c) the two recorded inputs until a Java night, JOP11 (a) the pin out of the javac key later. The
  score moves at stages 4-5: Java to 99.7.
  P2 by day on `java-p2` (2026-11-11; doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: P2 (by day)"): RecordComponent
  (slice 5), the classes made at run time deserializable (slice 8: Clojure's `serialization`), the platform logging bean
  (JCS281 (b)); its night is night 17 (commands in the section), its proposals JCS384-JCS388.
- **C++** (completeness, independence): read 1,182 and print 1,665 by design are most of the gap (96.8 without them); the
  print cell is on the own round trip on `cpp-print-own` (CP243 (a): the records made by day, print 48.3 → 99.5, the judge
  0.735 → 0.907; CP352 fixes the five reader gaps of the 15 forms-differ on `cpp-cp352`: 10 left, the dg-error files). Independence: the
  read stage 0.66 and eval and AOT 0.5 (the programs run from clang's IR packs); CX12's own front end (`dg --front
  native`, tonight's record) and CP240 (b), the programs by the own route without clang's IR (`cpp-route`), lift them
  ([cpp/CPP-SURVEY.md](cpp/CPP-SURVEY.md), [cpp/CODEGEN.md](cpp/CODEGEN.md)). `cpp-s6` (2026-11-11): CP365, CP368-CP371 (a) built
  (the own dg record's five crashes gone, provisional instances for <format>); `cpp-s7` (2026-11-12): its proofs run (the 82's IR
  only CP369's linkage, codegen-gen re-recorded, the five samples), CP372 (a) and CP373 (a) built, CP369's follow-ups (a header's
  out-of-line static member not linkonce, inline static members defined) and an eval-text crash fixed; library.cpp alone left to a night.
  `cpp-s8` (2026-11-12, CP366 (a)'s slice): ranges' iterator concepts on std::string and std::span right (16 of 16 on `char*` and
  string's iterator, from 6 and 5), ranges::copy, provisional instances for a dropped member, <format>'s floating builtins; f8 (<format>'s
  vformat) moved from _M_reserve to CP374's literal-encoding check, its drops 111 → 98; the 80 programs' IR byte for byte; library.cpp
  alone and the other proofs left as commands (`df /` at 87% from 07:16); CP374, CP375 proposed. Night 12's `cpp-check-essential` failures fixed on
  `cpp-gatefix` (our route's va_list as clang's; `[[no_unique_address]]` and empty classes as Itanium's: the <thread> programs' tuple
  layout), its memory limit 2GiB; hello.cpp's `cpp-check` regression is CP377. `cpp-s9` (2026-11-12): CP377 (a) (a directive inside a declarator, a member template's specialization of an extern
  template class: hello.cpp's resize_and_overwrite defined), CP376 (a) (clang's size floor; nua-layout.cpp), CP374 (a) (pointers in the
  evaluator: f8 moved to `string_view __s`, CP378 proposed), CP375 (a); the 81's IR: 69 generated, none lost, 13 differing as intended;
  codegen-gen recorded (155); library.cpp alone and the ci runs of cpp-check-essential and hello.cpp wait for night 12's gate's end. `cpp-s10`
  (2026-11-12): CP378 (a), class objects in sema's constant evaluator (constexpr constructors with `this`, copies by value, field reads and
  stores, member calls, references as locations) and `__builtin_is_constant_evaluated()` / `if consteval` true when manifestly
  constant-evaluated: f8 (<format>'s vformat) past the literal-encoding check (`constexpr-if` → the encoding branch of `__write_padded`,
  basic_string from _Utf_iterator, an overload refusal: CP379 proposed; library.cpp alone the same, 39 causes of drops from 47); the
  81's IR unchanged (none lost, byte for byte or by the inliner's numbering); codegen-gen 155 as recorded, print essential, the dg
  sample (own pass 127), reap and crops-score unchanged; cpp-check-essential left (the disk at 86%, its lower step stops above 85%);
  CP380 proposed (a plain global's constant initialization manifestly constant-evaluated). `cpp-s11` (2026-11-13, "As built: cpp-s11"):
  CP379 (a) built (a requirement's decltype operands kept by the parser: iterator_traits of an iterator with only iterator_concept),
  then f8 through seven more refusals, each general (ADL of enumerations, abbreviated function templates, [over.ics.list]/7 and `T x({...})`,
  sizeof(T) as a template argument, a CRTP base's requires-clauses at overload resolution, an unknown-bound global array, a pack named in a
  constant's text, which had made `__or_<false_type, false_type>` true); f8 now stops at "a wide string" (CP382 proposed); the 81's IR
  unchanged (69 generated, none lost); CP380 not needed by any run; library.cpp alone waits for the main session. `cpp-s12` (2026-11-13, "As built:
  cpp-s12"): CP382 (b) (`_GLIBCXX_WIDEN` names the wide literal in every char instantiation) then (a) (wide and Unicode literals: code
  units, i16/i32 globals, the evaluator's element reads), CP383 (b) kept; library.cpp alone first refused at make_format_args (`_Float16`
  and `__float128` unknown); then `using enum`, the extended floating types, constexpr static members inline since C++17: **f8 and
  library.cpp both generate and lower**; their runs need gcc (not run); the 81's IR byte for byte (69); CP384, CP385 proposed. `cpp-s13` (2026-11-14, "As built: cpp-s13"): C++'s link on ci's prebuilt runtime (CS273 (a): a fragment per unit, the link's
  prune; CP386 (a)): llvm-eh's five links above 2.5 GiB now 0.47-2.04 GB and **all five pass** (f8 by its "3", library.cpp as g++'s run,
  pr77550, atomics, hello), after the code generator's fixes (constructor templates defined outside a class template, GNU abi_tag on a
  class, sizeof... in signatures, unions' destructors, non-negative enumerations' bit-fields unsigned, 128-bit constants inside
  evaluations, CP388 (a)); the full runtime with tzdb.cc (its table a string literal, CP387 (a), read in 2.2 GB); drops f8 35/86 → 12/20,
  library.cpp 26/67 → 14/33; the 81's IR byte for byte (69), codegen-gen 155, the essentials, reap and crops-score pass; no gate key
  moves; CP389 (conversion function templates) proposed. `cpp-s14` (2026-11-14, "As built: cpp-s14"): CP389 (a), conversion function
  templates deduced from the target type ([temp.deduct.conv]), references bound directly to conversions' results; partial
  specializations' preprocessed arguments, noexcept(B) deduced false (CP390 (b)), explicit specializations' defaults, qualified variable
  template partials, enumerators' attributes; local statics constant-initialized, `decltype(e)::name`: drops f8 12/20 → 4/4, library.cpp
  14/33 → 5/6, both pass on llvmi; the 81's IR 66 byte for byte, 3 changed by constant initializations (as clang); the essentials, reap and
  crops-score pass; no gate key moves; CP391 (decltype mangling), CP392 (parser reads) proposed. `cpp-s15` (2026-11-15, "As built:
  cpp-s15"): CP391 (a), dependent decltypes mangled `DT <expression> E` as clang (hello.cpp's operator<=> clang's name byte for byte),
  CP392 (a), `decltype(e)::type x` declarations and a using-declaration's preprocessed tokens (native-check over its lists passes);
  `__extension__ using`, `X<T>::template Y` template template arguments, unfinished instances' out-of-line members, noexcept-expressions
  in template arguments, unsigned template argument values: drops f8 4 → 1, library.cpp 6 → 1 (both `__make_unsigned_selector`, right),
  both pass on llvmi; the 81's IR 67 byte for byte, 2 changed as explained and passing; CP393-CP395 decided; no gate key moves.
  `cpp-threads` (2026-11-15, "As built: cpp-threads"): runtime/threads.cpp's crash was bin/cppi building node programs with stage 0's
  embedded ci (seed d5548e72, before ci's threads), so the lowered runtime's `_Thread_local` pending flag was one for all threads; its
  `arbace.cpp.run` steps now run under `c_ci_fresh` (`bin/lib/cppi/ci-fresh.bash`), probe `selftest tls`; threads.cpp passes and is
  re-recorded `:pass`; CP396 (the empty `:thread-exceptions`), CP397 (cpp-check's memory while ci runs evaluated) proposed.
- **OCaml** (completeness, independence): eval's 207 and AOT's 202 by-design cases and AOT's 64 deferred; OC202 (a)
  (about 29 AOT passes, after OC185's switch) and the native records (tonight) move the AOT stage from 0.5 to 1
  ([ocaml/OCAML-SURVEY.md](ocaml/OCAML-SURVEY.md), [ocaml/DEFERRED-TRIAGE.md](ocaml/DEFERRED-TRIAGE.md)). OC219 (a)'s first part
  on `ocaml-s5` (2026-11-11): Runtime_events at oi and the AOT runtime (the asked collections' events, user events, cursors), 4 of
  `:runtime-events`' 7 pass at oi (native record) and the light level; counted in the matrix after OC185's switch and the compiled
  night run (`oc219.txt`); OC223, OC224 proposed.
- **Haskell** (independence, 76.0): eval and AOT 0.5, the programs run over the STG GHC made. The front end compiles
  ghc-internal bottom-up on its own interfaces (258 of 265; the `--full` step `haskell-lib-check-own`, HF10-P11), the default
  methods' unfoldings after its Simplifier (HF10-P12: lib-stg 5,398 of 7,627 bindings equal to GHC's -O0, ahead of GHC's
  interfaces), and base on that pool (HF10-P14: 235 of 241, `hs-p11`); HF10-P15 to P27 decided (a) (the user, 2026-11-10; P25 yes):
  `hs-p18` built P18 (GHC.Internal.Prim's interface from primops.txt.pp, 3,085 of 3,085 facts and every line but the header equal to
  GHC's: no GHC interface left on the own path), P15 (`haskell-lib-own-base`, on haskell-lib-check-own's pool) and started P20;
  `hs-p20` carried P20 on (sixteen run-breaking classes, the whole own set 40 of run-check's 63); `hs-p21` closed P20: the
  whole own set runs 50 of 63, every pass of the `equal` set kept, 0 lost (eight more classes: implicit-parameter synonyms,
  qualified binders, guard implications, -O0 constructor layouts, ...; lib-stg 5,533 of 7,639, 0 worse), and the elaborator
  keeps representation kinds (the `kinded-results` hard-code gone); P21 not built: the 13 programs neither set runs are the
  other packages' -O2 ABI and -O0 exception reports, not the programs' (HF10-P28 proposed: P21's proof restated, those to
  P17); P28 decided (a) (the user, 2026-11-11); `hs-p22` built P21 (run-check compiled on every package's own interfaces: the
  whole own set, ghc-internal and base both own, 51 of 63, 0 lost; classed rows `{:runs :all}`, `{:runs :all-base}`: o2-names 7,
  o0-report 2, hsi-memory 2, hsi 1) and P19 with P22-P23: `bin/hsi check --libs own`, the first own cases (programs on the own
  interfaces run over the own STG alone; samples, synthetic and testsuite-core: 420 of 764 run own, 412 pass in a merge of three light runs, interim; `check-hsi-own.edn`, `own-fallback.txt`);
  four elaborator classes (a pattern's IP givens, a field's qualified result, record fields checked, the innermost IP given).
  `hs-p22b` (review 1111b): the second pattern guard's givens fixed (GuardGivens in run-check, judged by its stdout), own-only
  failures classed by what fails (`divergences-own.edn`), own-fallback.txt honest and tried (`HSI_OWN_FALLBACK=try`), lib-stg's
  run classes with outcomes; `check-hsi-own.edn` is INTERIM (a merge of light runs) until the night's whole run at 16 GiB
  (its header's commands, night 13+).
  P31 (a) decided and built on `hs-p31`: the eval and AOT cells read `check-hsi-own.edn` / `-aot-own.edn` once whole (not INTERIM; `bin/reap --self-test`), the pack's record until then; the switch is the commit of night command 4 (5), with `bin/reap --record` and `crops-score --record`.
  P29 and P30 decided (a) (the user, 2026-11-11); `hs-p29` built P29's wiring: the step `haskell-lib-stg-own` (ghc-internal's own
  STG judged and `--run`, its output `ghc-internal-own.tar`), `haskell-lib-own-base` keeps `base-own.tar`, `hsi-check` and
  `hsi-check-aot` run `--libs own` on them (`:uses`), crops-score counts `:libs :own` from `check-hsi-own.edn` (not while it is
  INTERIM: 76.0 stays; projected 80.6, about 85 with the AOT record); P30's plan (P17's packages in dependency order, one GHC
  window) written; HF10-P31 proposed (the cells read the own records). Next: the night's runs (the survey's night commands).
  `hs-p17` built P17's first slice (HF10-P30 (a), no GHC): `lib-check --package P --ifaces own` beyond base (dependencies from the
  `.cabal` files, a list of own pools, every dependency's reexports, Prelude, the package's source directory), DefaultSignatures'
  `$dm`; deepseq 1/1, array 15/15, stm 10/10 modules elaborate, every binder type GHC's pack holds equal, interface facts 202/211,
  388/388, 222/222 (`lib-check-own-{deepseq,array,stm}.edn`); lib-stg 26/26 `:ok`, unjudged (no -O0 reference); steps
  `haskell-lib-own-{deepseq,array,stm}` `:clean-only` until the oracle window (its commands in the steps' comment). P32 and P33
  decided (a) (the user, 2026-11-12); `hs-p32` built them: the slice gated in `--full` and in `hsi-check`/`hsi-check-aot`'s own
  libraries, the six own-only failures fixed, none classed (local binders' types for tostg: thawSTArray's copy loop; derived Show's
  isSym: `()`; unique dictionary-function names: Data.Array.Base's strict and lazy ST instances; `OPTIONS -cpp` ineligible), the 31
  newly eligible programs 30 pass; ghc-internal's lib-stg 5,545 (0 worse), base's 1,046 bindings compared; default signatures in the
  own interfaces (an importing module's instance applies `$dm` with their dictionaries). HF10-P34 decided (a) (the user,
  2026-11-12); `hs-p34` built it: Generic and Generic1 derived as GHC.Tc.Deriv.Generics does (from/to, the `Rep`/`Rep1` family
  instance with its metadata), DeriveAnyClass's context from the default signatures; the generics programs found five library
  faults, fixed (pattern synonyms of the module matched as their pattern: symbolVal; SingKind's kind recovered by family inversion;
  KnownNat/KnownSymbol evidence; data instances by kind named apart; standalone newtype deriving of Monoid (K1 ...)): the five
  single-module `generics/` run tests pass on the own libraries (none before), 12 of 14 compile tests elaborate; ghc-internal's
  lib-stg 5,705 of 7,757 (0 worse), GHC.Internal.Generics errors 9 → 0. HF10-P35 (generics/ into the run corpora), P34 (b), P36
  (pattern-synonym matchers) proposed. P35 and P36 decided (a) (the user, 2026-11-12); `hs-p36` built them: matchers `$mP` and
  builders `$bP` as GHC.Tc.TyCl.PatSyn makes them, every `psyn` pattern desugared through its matcher (matchPatSyn), inferred
  synonyms' contexts, record synonyms' construction, update and selectors, across modules through the own interfaces; patsyn/'s 14
  single-module run tests 13 pass on the own libraries (3 before; T14228: unboxed sums as arguments, HF10-P37); ghc-internal's
  lib-stg 5,734 of 7,789 (0 worse); generics/ in `:testsuite-core` (708 run tests), judged by their shipped stdout
  (`own-verdicts-pending.tsv`, `:oracle-pending` where GHC's STG is needed) until the window records GHC's runs. P37-P39 decided (a) (the user, 2026-11-13; `:oracle-pending` accepted); `hs-p37` built them:
  unboxed sums unarised as GHC's Unarise does (binders, arguments, slot casts, case and let binders, constructor fields; unboxedsums/'s
  run tests 18 of 19 on the own path, 5 before; T14228 passes, patsyn/ 13 of 13), patsyn/ in `:testsuite-core` (722 run tests, pending
  by their own verdicts), Typeable evidence of promoted constructors (`$tc'Many`) and functions (`mkTrFun`, the arrow's kind arguments):
  Type.Reflection's App, Fun and a function's `show` run; ghc-internal's lib-stg 5,734 of 7,789 (0 worse). P40-P42 proposed. P40-P42 decided
  (a) (the user, 2026-11-13); `hs-p40` built them: unboxedsums/ in `:testsuite-core` (743 programs); Typeable's kind arguments at the
  use site and the own data types' kinds inferred (kcTyClGroup lite, written into the own interfaces and `$tc` bindings; every module's
  `$tc'` bindings, classes' TyCons; an explicit forall's binder order), `*`, promoted tuples, literal Typeable: TypeableEq and T14341 pass
  on the own path, TypeOf and TypeRep but IO's lines (fixed after, not rerun); P43 in part (monad comprehensions, `main`'s type);
  ghc-internal lib-check 0 worse (3 better), lib-stg 5,734 of 7,789, base as before. The proofs after round 8 stopped at `df /` 89%
  (the night's gate): night command 1. P43-P45 proposed. P43-P45 decided (a) (the user, 2026-11-14); `hs-p43` built them with P46 and
  P49 (taken on the way): TransformListComp, ParallelListComp, monad comprehensions' transforms and parallel blocks, CallStack
  implicit parameters of any name, constraint synonyms in contexts, static pointers (GHC's table keys), withDict's T16646, the own
  classes' kinds, GADT binders' kind annotations and standalone kind signatures; hs-p40's proofs run: the libraries 0 worse (counts
  unchanged), the front end 437 of 483 testsuite-core programs (391 of 465 before; 447 with P46, P49), 29 programs more pass on the
  own path. P47 (Code's signature, (b)), P48 (a variable superclass), P50 (the next gaps) proposed.
  P48 and P50 decided (a) (the user, 2026-11-14, round 34); `hs-p48` built them: a variable superclass (T23761b's IP shadowing),
  the constraint tuples' GHC superclass fields, selectors and constructor names, recursive dictionaries, superclass loops
  prohibited, the builtin families' interactions, or-patterns, n+k, rec, mdo, `@a` lambdas, rank-n applications' evidence; the
  libraries 0 worse (ghc-internal's lib-stg 7,961 of 9,931, was 5,734 of 7,789), the front end 483 of 501 testsuite-core programs
  (469 before), 47 of 47 changed programs pass on the own path. P51-P53 taken (b); P54 (the next 18 gaps) proposed.
  `hs-p54` built P54 (a): fundeps and overlapping instances in the solver, constraint families, givens rewritten,
  OverloadedStrings and OverloadedLists, sized primitive literals and Show, multi-parameter GND, postfix and tuple sections; the
  libraries 0 worse (counts unchanged), the front end 500 of 501 testsuite-core programs (483 before), 65 of 65 changed programs
  pass. P55-P57 taken (b); P58 (T15598, RebindableSyntax's rank-n bind) proposed.
  Next: the window (P16's and P17's references, generics/'s runs), then containers.
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

## The burst (2026-11-17)

After the consolidation (the goal cleared by the user at 10:59), a burst of high-thinking agents on this host (about 25, light
targeted checks, no gates: `.tmp/goal/host-burst-rules.md`) and the Mac's batches 317-327 and 328-338, to use the week's
allowance. Every merge onto next kept `bin/reap` at 0 problems and `bin/crops-score --judge --strict` passing; next was reviewed
twice before the night (`h-review-diet`, `h-review-next2`: nothing breaking). The rule of the day: a branch that changes the
build (compiled or embedded code, j2g, goi's table, a key) without having been built waits for after the full gate, since a
build failure ends the whole gate (the morning's javac-tool). Each held branch's commands for the main host are in
`.tmp/goal/after-gate-todo.md`. The decisions are round 51's (`.tmp/decidables-1110.md`); round 52 (19:15) set the night.

- **The gate.** Landed: the crops tier (`gate-ess-diet`, essential 71 -> 51 steps; journal "the crops tier"), `memdecl-fix` (the
  declared `:mem-gib` at the measured peaks), `tmpdates-fix` (old-dated archives extracted with `tar -m`), `fix-javac-tool`
  (the Path variant retired; javac's key moved, gate_key not), javac-tool 51 GiB, diffcheck 50, programs-full 51. Held: the
  cover (`h-keys-design` `e55d2456a`, GDS21-GDS26 (a); first prune units.tsv by `bin/gate-unit-keys measure` on tonight's log;
  GDS27 for the user on its data).
- **Java** (89 92 100 60, from 87 90). Landed: F26 and GH5 (iii)'s first slice in one conversion (`f26-ghidra`, merged as
  `e30418178`: the class-file API, java.beans' PropertyChangeEvent and Listener, h-java-deferred's derivations, A8c; `bin/jrt-convert
  verify` byte for byte, `bin/java-check --essential` 428 of 428); `h-java-deferred` (168 stale deferred interp rows recorded, eval
  deferred 611 -> 443); RP1 (a), RP2 (a) (print deferred 107 -> 40); the own platform's design JOP1-JOP8 with its proof
  (`h-java-platform`: 68 modules, 27,530 classes, only 48 `:param-names` classes differ). Ghidra: analyze's 20 failures -> 0
  (`h-ghidra-20`), G2's A14, A16, A18 (`h-g2-arbace`). Held: RP3 (javac's key) and the platform's transition night
  (`.tmp/goal/java-platform-night.sh`, about 2 h 10 min, 45 GiB peaks); `h-java-members` (`2aa918480`: 28 of 85 tests' causes
  fixed in j2g, unbuilt); `h-java-aot-triage` (`1c4357d87`: the 536 compiled deferred grouped, AT1-AT10; its merge needs `bin/j2g-edge
  update`). Round 51: System.Logger (a) per-class derivations; JOP1-JOP8 (a), JOP9 (a), JOP10 (c), JOP11 (a); RP1-RP3 (a);
  finalize in Object's table (a); AT1-AT10 approved; Ghidra fix 1 (a), GH13-GH22 (a).
- **Clojure** (99 99 100 100). Landed: `h-clojure-go` (CDR2's and CDR4 (b)'s rows recorded, eval deferred 50 -> 45; gen-class's
  `:exposes-methods` takes the world's protected methods) and its fix `h-prepare-fail` (no fn defined after genclass.ae in
  arbace.core). Held: genclass's record after a build at the new gate_key, then the AOT suite with test-dot, test-bean, genclass.
- **Go** (98 100 100 100, from 97). Landed: the by-design classes reclassified (round 51 A (a), B (a): weak/unique, types2's two
  and the abridged type literal deferred, the rows with no reference `:na`). Held: `h-go-weak` (`d22d37048`, weak and unique
  importable: j2g and goi's table) and `h-go-noforms` (`aa47291df`, types2's range and slice checks in the gcsyntax driver),
  each Go eval deferred 9 -> 7; then a whole `bin/goi check` (corpus_key moved).
- **C** (98 100 100 100, from 96). Landed: `h-judges` (67 andrewchambers_c rows judged by their own verdict, the judge 98 -> 99),
  `h-c-deferred` (read and print rows), `h-c-run` (17 by-design programs pass; CS320 (a) GNU vector types and CS321 (a) nested
  functions, first deferred then built: eval and AOT with no deferred row). Held: `h-c-decl-diag` (`a9079b556`, read deferred 27 ->
  17, embedded C), then C's whole records. Round 51: judges D2 (a) gcc's record kept for MIR's new/; eval-depth (a) deferred.
- **C++** (91 93 100 72). Landed: `h-cpp-read` (read deferred 136 -> 103: 33 syntax files and a fatal Sema recursion, compiled
  sema's unbounded depth since W2; CP398 (a)+(c), CP399 (a)). Held, to merge together after the gate (round 51): `h-cpp-consteval`
  (`d36d4e385`: class objects in the constant evaluator, `:own-diag` 863 -> 815; the evaluator is the code generator's too), the
  Mac's 336 (both rounds, `-fconcepts`, D336-1 (a), D336-2 (a)), 324 with its review (`h-review-mac-crops-cpp` `b22363909`: the
  own IR on the auto route, D1 (a), D2 (a)) and 338 (the VM-only Sema crash, 324's header bug; 338-1 (a), 338-2 (a)); then
  cpp-runtime, the essential checks and the whole cpp-dg. Judges D3: the C++ judge's bound (about 91) accepted.
- **OCaml** (86 96 100 100). Held: the Mac's 323 (`65109ab43`: OT30 (a) split, OT31 (a) weaklifetime fixed in ExtGc, OT32 (a)
  `:config-skipped`; meant 96 -> 91 honestly once recorded; jdk/ocaml's runtime key moved: the whole records). 331 (OT30's
  classes) runs on the Mac.
- **Scheme** (100 100 100 100, from 98). Landed: `h-scheme` (the AOT cell widened to eval's corpora, 51 -> 2,513 cases; SCS27 (a),
  SCS28 (a), SCS31 (a) `:na`, SCS32 (b) si-ecraven-aot weekly; SCS33-SCS36 (a)). Held: the Mac's 334 (si's speed), then
  `SI_TIMEOUT=300 bin/si ecraven --aot --record` (about 1.5 h) and the source's `:until` dropped.
- **Haskell** (97 99 100 76). Landed: `h-haskell-deferred` (the deferred rows and design classes audited), `h-hes-lls` (HES92 (a),
  HES93 (a): print 1,345 / 0 / 0), `h-haskell-threaded` (HES84 (a), hsi's threaded way: eval deferred 35 -> 30, AOT 1 -> 0).
  Held: HES91 (a) (`h-hes-lls-hes91` `d129fc245`: callback002's record at 16 GiB, with rts_clearMemory); the Mac's 325 (own STG,
  D1-D6 (a), independence 76 -> 78 once recorded). Round 51: T24809 (a) kept skipped; the I/O manager (a) as designed, parallel
  capabilities (a) after 333.
- **Rust** (75 75 96 80). Landed: `h-rust-indep` (RIND1-RIND7 (a): the own allocator shim with `rsi-shim-essential`, rsi's ci on
  Arbace's own C target, the samples from Arbace's MIR, 5 of the eval cell's 5 cases own through the shuffle, SIMD arithmetic and
  swizzle; stage 4's measures and the StableCrateId finding), the Mac's 322 (the AOT cell, D1 (a), D2 (a)). Independence stays
  80 until C2-C6. Held: 322's amd64 records (the cell's `:until` dropped with them); RIND8 (a), RIND9 (a), RIND10 (a) (the
  portable-SIMD lines in core-attrs, proved by `bin/rust-front rsi-build vectors` before the night's `--front own` record).
  Round 51: simd const eval (a), `--record` defaults to `--front own` (a).
- **LLVM IR** (98 99 100 98). Landed: `h-haskell-deferred`'s LLVM part (the verifier's GEP and blockaddress checks: read deferred
  17 -> 15, eval 15 -> 3), LLS54 (a) (the tx pack with dg-options), `h-judges`' split-file section, the Mac's 335 (Verifier rules:
  read deferred 15 -> 1; D335-1 (a), D335-2 (a)). Held: 335's `ir-make rt --mutants` and `verify-rt-heldout --record`; the 13 eh
  modules' AOT record. Judges D1 (a): lli's record kept.
- **The assembly crops** (99 100 100 100, 100 100 100 100). Landed: `h-asm-deferred` (AXS95 (a) `:object-unjudged` deferred,
  AXS96 (a) a real judge for the error tests, AXS97 (a) `:debug-trace` by design: amd64 read deferred 32 -> 1), `h-asm-directive`
  (MCAsmStreamer's directive lines: print deferred 581 -> 531), the Mac's 337 (the object check, AXS99 (a), AXS100 (a), AXS101 (b)).
  Held: 337's whole amd64 object-check record.
- **Python** (0 0 83 60). Landed: `h-pyi-runtime` (pyi loads the checkout's runtime when it differs from stage 0's). Held: the
  line `mac/python-1117-on-next` (`593c97d03`: 317-321, 330, the review's fixes; independence 98 on it): its records night first
  (`GOGC=50 PYI_LIB_TEST_JOBS=4 bin/pyi records`, the packs, 318's results), then the splice (`317-python-splice`, F1 (a)).
  Round 51: 304 PYTHONHASHSEED (a), 305 (b) as 316, 316 D1 (a), 320 D1-D3 (a), 321 D1 (a), D2 (a), 317 D1-D3, F5 (a), 330 D1 (a).
- **WebAssembly** (98 98 100 100). Held: the Mac's 326 (both rounds, D326-1 to D326-4 (a)) and 327 (custom descriptors, D327-1 (a),
  D327-2 (a); wasm 100), with their amd64 commands.

The night (round 52): the full gate alone on the host; the burst goes on with memory-free agents (each process under 1 GiB:
reviews of the held stacks, designs, code with unit tests, this record), decisions taken as recommended and logged "taken
overnight (round 52)". After the gate, nothing heavy until the user is up: no Ghidra night jobs (deferred by the user), no
held branch's records, no Java platform night. The Mac stays settled (no new tasks).

## The consolidation (2026-11-15/17)

One stack, essential gates after the merges, the re-seed last, then one full gate (the user, 2026-11-16: the intermediate full
gates moved with every key the re-seed moves anyway; rounds 45-50 in the journal).

- **B1** (`9da791cd4`, the key-window batch): its full gate's 16 failures fixed by `revisit-b1` (`fa87475c8`); main advanced
  to B1 anyway (round 44).
- **B2** (`aac02c52c`): java-night merged without F26 (round 45 (b)), 206 build errors fixed. F26 (the classfile API) is fixed
  apart on `mac/f26-classfile` (`acaae3905`) and waits for after the goal.
- **The crops' records**: rec-ocaml (OCaml's independence 52 -> 100), rec-llvm-cpp (LLVM 97 -> 98, C++ 71 -> 72), the asm
  arm64 controls fix (`b1f27d8b3`: the thread kind needs the arm64 CPU).
- **W1** (`stack-w1`): essential gate passed but cpp-c2j and scheme-c2j (the old stage 0 lacked JCS167's serial plan); the
  fixpoint holds.
- **W2** (`53da41d93`): the embedding; `bin/pyi table-check --configs tamago/amd64,tamago/arm64` passes; the fixpoint holds.
- **The re-seed** (`03fec28a4`): seed `3c48859d`, verified. Its essential gate: cpp-c2j and scheme-c2j pass; two new failures
  fixed: `j2g-edge` (repr/scheme's reader, now embedded, had fns with primitive hints and over 4 parameters, refused by the
  frozen v4 compiler; the edge recorded afresh, `4192d6a45`, moves gate_key) and `llvm-controls-test` (the gate's cheap prune
  of TMPDIR's files older than 2 days removed llvmi's IR modules, extracted with their packs' 1970 dates; `tar -m`,
  `2455fa2be`).
- **R** (`w-retire-4`, merged as `f1969748f`).
- **The full gate** on `f1969748f` from 09:20 UTC (plan: simulated wall 7:27); the essential gate before it was dropped as
  redundant (the full tier runs every essential step; the user, 2026-11-17). It ended after 59 minutes: the `javac-tool` build
  failed (jrt's `java_nio_file_Path`: a duplicate `GetFileSystem`, the tool world's Path variant beside jdk26u's own since F9),
  every check unrun, and its advance script moved main and next to `f1969748f` without checking round 50 (3)'s condition. The
  user kept main (no force push); the essential gate on main passed at 12:33 (126 min, 0 failures). `fix-javac-tool`
  (`2229a2093`) retired the variant; the full gate moved to the night, on the burst's next.
- **Beside it** (light, branches to land after the advance): `tmpdates-fix` (every other extraction of an old-dated archive
  where the gate's age prune reaches), `memdecl-fix` (the steps whose peaks exceed their `:mem-gib`: cpp-runtime 8.1 GiB
  for 3, go-build 25 for 20, ...), `goal-records` (these records).
- **After the goal** (round 50 (4)): F26 (its classfile sources again, the conversion, the edge, its 4 commits), the Mac's
  304 and 305, Ghidra's PropertyChangeEvent slice (a quiet host, about 34 GiB), then analyze and G1.md §8 on a gate build,
  Python's gate fragment and amd64 records night, oi-divergences judging the native records, t03832 recorded again, and the
  branches pruned (cpp-cp338, night-py, oi-table2-scratch; wasm-wa8's content checked).

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
- **java-jdk (2026-11-13, light)**: the two parked jdk parts. `repr-java-roundtrip.jdk`: the corpus grew 906 -> 2,277 files
  (11-07) without its references recorded again, and the printed tree's six javac rounds no longer converged: the loop fixed
  (until accepted, at most 20); left: `roundtrip --corpus jdk --text --record` and a pass. `repr-java-attr.jdk`: GC slack at
  GOGC 200, not retention; the jdk reads under GOMEMLIMIT 26GiB (estimated 27-28 GiB); left: the whole part, then `:clean-only`
  lifted. Both whole runs wait for the rerun's end and the main session (journal, 2026-11-13).
- **gf-java (2026-11-12, by day under the big lock, the user's fix-today)**: night 12's stale Java family fixed on `next`'s
  world: `jdk/edge.edn` (java.lang.Thread, named by arbace.c.musl), javac's provenance and `:jdk-edge`, `jdk/provenance.edn`,
  the fixtures' provenance, `bin/jrt-convert verify` no longer reporting the hand-written `jdk/repl-exports.txt`, and
  javac-check's six analysis failures (five variants: four hunks of java-night's, DatagramSocket's cast here). Every verify
  passes; moves gate_key (`4c78a93d1fe3...`) and the javac key (`5bb2040681c2...`). When `next` merges into `java-night`,
  java-night's `DatagramSocket.ae` (F10b) wins.
- **batch-1115** (2026-11-15, the user's round-31 no-key batch): asm-arm64 AR14-AR24, C++ cpp-s15 (llvm-eh, cpp-s13-s15),
  Haskell hs-p48 (HF10-P40-P50), Rust RF20 and OCaml ocaml-s5 (OC219 (a)) merged, keys unmoved, the essential gate passed. Left:
  `wasm-wa8` (adding its paths to `arbace_crop_paths` changes `bin/lib/version.bash`, a `gate_key` input: it moves gate_key and
  the javac keys, so it waits for a key window), Python (`mac/mac/201-python-py7-py11`, a merge of its own: next's queue 261 and
  the branch's 262-274 changed the same pyctypes Go forms), hs-p54 (unproven); cpp-cp338 is on next already as cpp-cp338b.
- **Waiting**: `mac/201-python-py7-py11` (Python, a separate merge after batch-1115); `java-testlib` (onto `java-night`
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
   Its goi, C, OCaml and si failures rerun alone by day on `gf-misc` (2026-11-12; journal): real and fixed: goi's escape lines on
   `:full` forms and `go:norace` (escape2, escape2n, issue24651a), ci's compiled calls allocating since CS277 (c-aot's three; plus
   30 s budgets), repr/ocaml's surrogate pairs (strings.ml); stale: C's three `:item-placement` annotation entries (a5705351b's
   patch, applied); speed: oi's darkening_work and churn (oi-slow); si's ecraven/ack: no regression after all (gate-gts,
   2026-11-13: the 10-31 tree and next both 165 s on the same quiet host, 239/167 s under the rerun's load; the 407 s was the
   host), SI_TIMEOUT stays 600 pending the user's (a) keep 600, (b) back to 300, (c) a per-program limit. gate_key moves (arbace/go,
   go/arbace/ci): wait for W2's end to merge.
   Night 13's two left (`n14-fix`, 2026-11-13; journal): the Java levels' "No space left" was ext4's directory index on the
   flat forms cache (208,010 entries), not the disk: the read caches sharded (`CACHE/KK/KEY`), the shared one migrated; the
   Java level steps that passed on night 13 with ENOSPC in their saves must run again (`--rerun` of them), then `java-judge`.
   hsi-check's fasta, exact-reals and T16846 pass alone near the CPU limit: classed `:speed`.
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
  pass, the night-10 list closed; night 12 records the native records after the merge; `oi-fix`, 2026-11-13: check-oi-native.edn recorded whole, oi-check.run 379 pass, 0 failing, its two slow entries restored); `ocaml-s1` on it (2026-11-10: OC213 (a),
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
  `review-fix-1`: review 1111's eight findings (oi's :init-order and OC216's annotations, Haskell's guard givens and --o0-layout,
  hsi's Float FMA, RF19's key and attribute precedence, LLVM's section rule and renames) fixed and proven; its night: RF19's
  rsi-build makes core-attrs.edn again (23 min, 2.3 GB).
- repr/java: `java-nread-g1` (the native provider's two fixes from Ghidra's G1 read proof: a void method's reference against
  Supplier, an inherited static field through two static on-demand imports; samples `MrefVoid`, `Star*`). Its night: the four
  provenance writers (the reader hash moved), nojavac-check and attr-check over jdk, javac and jdk26u, `bin/repr-java check --jdk`.

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
  `asm-ax11` (from `mac/next`; doc/asm/AMD64-SURVEY.md, "As built: AX11"): the AOT cell's six object classes fixed (conditionals,
  macros, `.rept`/`.irp`/`.irpc`, x86-64's `.reloc`, the DWARF 4 line table's header, `$0x80` as a name): 94.2% -> 100.0% (105 / 0 / 0, the
  run check 19 of 19 with `run/macros`). Proposed AXS92 (the line program), AXS93 (arm64's `.reloc` order, for the arm64 line), AXS94
  (`--defsym` tests into the object corpus).
  `asm-ax11b` (from `mac/next` `f6ac54829`): AX11 merged with AR7, `arbace/asm/obj.ae`'s error collection inside `run-items!` (an expansion's
  errors at its invocation); both crops' checks rerun, every record unchanged; x86-64 AOT 105 / 0 / 0, arm64 AOT 3,005 / 0 / 55.
  `asm-ax12` (from `mac/next` `4ec952309`; doc/asm/AMD64-SURVEY.md, "As built: AX12"): AXS92 (a) the DWARF 4 line program (`.loc`'s
  entries, emitOne's opcodes, MCDwarfLineAddr's advances, end_sequence; `synthetic/debug-line`) and AXS94 (a) MC/X86's `--defsym` cases
  (`intel-syntax-quote-reserved`, `pltoff@ERR=1` held to llvm-mc's recorded refusal), objects recorded by hand with llvm-mc 23.1.5; x86-64
  AOT 105 -> 108 / 0 / 0. Proposed AXS95 (the five left-out refusals), AXS96 (MC/AsmParser's DWARF tests).
  `review-fix-3` (from `mac/next` `a7916bb2a` with `mac/asm-ar9`; doc/asm/AMD64-SURVEY.md, "As built: review 1111's fixes"): review 1111's
  findings, one commit each: an expansion's errors at LLVM's `<instantiation>` places with their notes (a `.rept` with no backslash
  counted and placed), `--defsym` by keyword and one radix-sensing parser, the refusal's diagnostics controlled, `.reloc`'s places,
  `.file`'s path split; hand-derived diagnostics in `test/asm/obj-err*/`; every object unchanged. Proposed AXS97, AXS98.
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
  variables): the AOT cell 78.5% → 95.0%. Proposed ARS43-ARS47. Then `asm-ar7` (from `mac/next` `15278e9c6`; doc/asm/ARM64-SURVEY.md,
  "As built: AR7"; ARS43 (a), ARS45 (b), ARS46 (b), ARS47 (a), the user's): `a64i-essential` sliced (`--essential`, judged on what it
  ran, slice-foreign control; 1 min 36 s → 36 s; the whole set the `--full` step `a64i-check`, rotated), every error of a file collected
  and judged as llvm-mc's diagnostics, `.org` and `-implicit-mapsyms`, the memory families' synthetic programs (801 cases; coverage 653
  of 653): the AOT cell 95.0% → 98.2%, completeness 98.7 → 99.5. Proposed ARS48-ARS52. Then `asm-ar8` (from `mac/next` `4b8fc77e3`;
  "As built: AR8"; ARS49 (a), AXS93, ARS48 (a), the user's): `@AUTH`/`%pltpcrel` read and assembled (`(asm/unread ...)` for refused
  statements inside conditionals, `--defsym`, readelf's GNU view), `.reloc` merged with PATCHINST last (sortRelocs), NEON's integer lanes
  in a64i (`simd.ae`, `synth-neon` 1,769 cases, coverage 1,358 of 1,358): mc-aarch64-obj 110 of 110, the AOT cell 98.2% → 99.6%,
  completeness 99.5 → 99.8. Proposed ARS53-ARS55. Next: the VM's CPU rows and lockstep of the thirteen synthetic programs (ARS44),
  NEON's structure loads and stores (ARS53), the literal pool with the C packs (ARS50). Then `asm-ar9` (from `mac/next` `4ec952309`;
  "As built: AR9"; ARS53 (a), ARS54 (a), ARS55 (b), the user's): `(asm/unread ...)` for x86-64's AT&T and Intel (amd64's round trip
  re-recorded, unchanged), NEON's structure loads and stores (`simdmem.ae`, `synth-neon-ldst` 1,020 cases) and FP lanes' vector classes
  (`simdfp.ae`, `synth-neon-fp` 1,176 cases) in a64i: coverage 2,015 of 2,015, eval 303, AOT 3,049 / 0 / 13. Proposed ARS56-ARS58.
  Next: the by-element and scalar forms (ARS56), a structure load in the essential subset (ARS57), the VM's rows (ARS55, ARS58).
  Then `review-fix-3` ("As built: review 1111's fixes"): a64i's FPCR paths held (`synth-fp-[ds]-rm`, RMode 1-3 and DN, the model's
  FPRound at a mode, qemu at `--record`; an FMA precision bug fixed in a64i; FIZ, AH, NEP refused), `synth-neon-e` in the essential
  subset, arm64's `:insn-text`: a64i 90 of 90. Proposed ARS59, ARS60.
  Then `asm-ar10` (from `mac/review-fix-3` `6bf064ab4`; "As built: AR10"; ARS56 (a), ARS57 (a), ARS59 (b), ARS60 (a), the user's):
  `simd-struct.s` (structure loads and stores and FP lanes by hand) in the essential subset (+2 s), `synth-neon-fp-rm` (the FP lanes
  under FPCR, 1,152 cases), and in a64i the scalar FP classes and the by-element multiplies (`synth-neon-el`, 568 cases): a64i 93 of 93,
  coverage 2,129 of 2,129, eval 309, AOT 3,055 / 0 / 13. Proposed ARS61, ARS62. Next: ARS61, ARS62, the VM's rows (ARS55, ARS58).
  Then `asm-ar11` (from `mac/asm-ar10` `a4f851948`; "As built: AR11"; ARS61 (b), ARS62 (a), the user's): `synth-neon-el-e` (a quarter
  of synth-neon-el, 142 cases) in the essential subset (+5 to +9 s at night 12's load), and in a64i fcvtn, fcvtl, fcvtxn (half as the
  narrow type, round to odd) and the fixed-point scvtf, ucvtf, fcvtzs, fcvtzu with #fbits, vector, scalar and general register
  (`synth-neon-cvt`, 1,000 cases): a64i 95 of 95, coverage 2,176 of 2,176, eval 311, AOT 3,057 / 0 / 13. Proposed ARS63-ARS65. Next:
  ARS63, ARS64 (the essential time on a quiet host), ARS65, the VM's rows (ARS55, ARS58).
  Then `asm-ar12` (from `mac/asm-ar11` `5740229b4`; "As built: AR12"; ARS63 (a), ARS64 (c), ARS65 (b), the user's): in a64i the
  saturating doubling multiplies (sqdmulh, sqrdmulh, sqdmull, sqdmlal, sqdmlsl: vector, scalar, by element) and the saturating shifts
  and narrows (sqshl, uqshl, sqrshl, uqrshl, sqshlu, sqshrn, sqrshrn, uqshrn, uqrshrn, sqshrun, sqrshrun, sqxtn, uqxtn, sqxtun),
  FPSR.QC exact (`simdsat.ae`, `synth-neon-sat` 1,092 cases): a64i 96 of 96, coverage 2,394 of 2,394, eval 312, AOT 3,058 / 0 / 13;
  the essential step +0.7 to +1.0 s with synth-neon-el-e at load 15-19 (ARS64; the quiet run still to do, night 12's gate ran past 12:30). Proposed ARS66, ARS67. Next: ARS66, ARS67, the VM's rows (ARS55, ARS58).
  Then `asm-ar13` (from `mac/asm-ar12` `ac5722aec`; "As built: AR13"; ARS66 (c), ARS67 (b), ARS64 (c), the user's): a64i's package
  `:init-order` set to goi's checker's (simdfp.go's variables, missing since AR9; 0 errors in linux/arm64); in a64i NEON's remaining
  integer lanes (srshl, urshl, pmul, raddhn, rsubhn, pmull, saddlp, uaddlp, sadalp, uadalp, suqadd, usqadd, sqabs, sqneg, shll, rshrn,
  addp's scalar: `simdint.ae`, `synth-neon-int` 560 cases): a64i 97 of 97, coverage 2,507 of 2,507, eval 313, AOT 3,059 / 0 / 13; of
  ARMv8.0's integer SIMD only urecpe and ursqrte left; the essential step +1.8 s with synth-neon-el-e after night 12 (load 6-14).
  Proposed ARS68-ARS70. Next: ARS68 (the estimates and ARMv8.0's FP rest), ARS69, ARS70, the VM's rows (ARS55, ARS58).
  Then AR14-AR24 (queue 277-299, the Mac VM; doc/asm/ARM64-SURVEY.md, "As built: AR14" to "As built: AR24"; ARS68-ARS95 as their
  commits record, the user's): a64i's goi table check (`a64i-table`), the rest of ARMv8.0's FP and the later extensions' lanes, Linux's
  arm64 user ABI (signals, stat, mmap), real Go programs on a64i (`a64i-go`), gofmt and vet (`a64i-go-tools`), cmd/compile, cmd/asm,
  cmd/pack and cmd/link (`a64i-go-compile`), goroot-check (AR24); a64i check 128 programs, eval 512, AOT 3,090; on next with batch-1115
  (2026-11-15).
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
- Ghidra G1's run half (2026-11-10, branch `ghidra-run` from `mac/java-p1`; GH4 (ii) approved): A4 (a closed-world
  `java.util.ServiceLoader`, the registry j2g generates from META-INF/services and `jdk/provides.txt`), A5 (resources of
  several jars, class files' URLs, ClassLoader's resource methods in translated code), A6 (native-image metadata and constant
  reflective sites as roots), A7 (refusals stubbed or dropped, `report.edn`'s `:refused`); proved light by day, built and run
  at night (the commands in J2G-SPEC's sections and the journal's entry): the conversion, the build, the four samples
  `services`, `resources`, `reflect-meta`, `refusals`. A1-A3, A11 on `ghidra-read`.
- Ghidra G1b (2026-11-11, branch `ghidra-g1b` on `java-night` with `java-p1`, `ghidra-run`, `ghidra-read`, `java-nread-g1` merged):
  A9 (`bin/jrt deflate`: 171 of 171 equal to the JDK's, the essential step `jrt-deflate`), A10's fixture (`test/programs/junit4`: JUnit
  4.13.2 and hamcrest-core 1.3 from pinned sources with two variants, `bin/java-project fetch` and `:patches`), A13 (`program.edn
  :properties`, j2g `--property`), `:java`'s resources a directory a module; A12 is the demo's plan; A8 after the night's analysis. Night
  18: `.tmp/night18/slots/` (the samples, SoftwareModeling's nread, SleighCompile built and run over the 150 specs, the tests, the ANTLR
  tool); by hand, the oracle slot (the JDK's javac on hamcrest-core's two calls, the junit4 fixture on the JDK).
- repr/java: `java-nread-g1` (the native provider's two fixes from Ghidra's G1 read proof: a void method's reference against
  Supplier, an inherited static field through two static on-demand imports; samples `MrefVoid`, `Star*`). Its night: the four
  provenance writers (the reader hash moved), nojavac-check and attr-check over jdk, javac and jdk26u, `bin/repr-java check --jdk`.

- Ghidra G1's run half (2026-11-10, branch `ghidra-run` from `mac/java-p1`; GH4 (ii) approved): A4 (a closed-world
  `java.util.ServiceLoader`, the registry j2g generates from META-INF/services and `jdk/provides.txt`), A5 (resources of
  several jars, class files' URLs, ClassLoader's resource methods in translated code), A6 (native-image metadata and constant
  reflective sites as roots), A7 (refusals stubbed or dropped, `report.edn`'s `:refused`); proved light by day, built and run
  at night (the commands in J2G-SPEC's sections and the journal's entry): the conversion, the build, the four samples
  `services`, `resources`, `reflect-meta`, `refusals`. A1-A3, A11 on `ghidra-read`.
- Ghidra (G1, the demo repository `arbace-ghidra`): `ghidra-read` (A1, A2, A3, A11: `bin/java-project`, `program.edn`'s
  `:java`, doc/go/BUILD.md "Java sources by module"; the gate's `java-project` step), `ghidra-run` (A4-A7). Night: the
  demo's `bin/read-g1 --check nread --modules SoftwareModeling` (about 9 GiB); `test/programs/java-project`'s first build
  (`--build-program --check java-project`, a whole-program j2g). Open: the two native-provider findings of the read half
  (the journal, 2026-11-10).

## Decided, not built yet

The user's decisions not yet in code, by crop; the texts are in the docs named. Every open proposal of 2026-11-08/09 was
answered (2026-11-09: all of Java's, C++'s, Rust's, Haskell's, LLVM IR's, Scheme's, C's and OCaml's as recommended); the
items below are the ones whose work is still ahead or tied to a night.

- **Java** ([classes/JAVA-DEFERRED-TRIAGE.md](classes/JAVA-DEFERRED-TRIAGE.md),
  [classes/JAVA-CONFORMANCE-SURVEY.md](classes/JAVA-CONFORMANCE-SURVEY.md)): JCS169 after serialization's slices 2-4;
  F10b's asynchronous channels after the night's records; JCS190's flip after the night's comparison; JCS231 after F9;
  JCS261 waits; JCS274 (`:nio`'s rest reclassified after the record); JCV11 (b) (reopens RPS9 (a)); JCV12 (b); JCS180
  later; F15 (JCS350-JCS356) for the next Java night. JCS370-JCS378 as recommended and JCV10 (b) (the user, 2026-11-10):
  the record first (S1), then the port batches P1 (night 15), P2 (night 17), P3 (night 19). P1 built by day on `java-p1`
  (PhantomReference and the thread "Reference Handler", System.runFinalization, thread groups, the Cleaner, jar verification
  JCS231, DGCClient jdk26u's; "As built: P1 (by day)", its night commands there; JCS379-JCS383 proposed); P2 takes
  serialization's slices 5 and 8 (JCS383).
- **C++** ([cpp/CPP-SURVEY.md](cpp/CPP-SURVEY.md)): `gf-cpp` (2026-11-12, "As built: gf-cpp"): cpp-s9 merged into next's tree, explicit6.C's
  explicit((T) 1.0) evaluated (floating literals in eval-text), the runtime part of cpp-check-essential as recorded; its samples part,
  cpp-check.samples-runtime and dg-error-own whole are night commands; CP381 (the essential step's memory; CP379 on its branch) proposed. CP243 (a) (`cpp-print-own`) and CP352 (`cpp-cp352`, the five valid-code reader
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
  unroll1.C's names now clang's), CP362 (<format>'s consteval check) to CP364 proposed, the user's (a) each; `cpp-s5` (2026-11-11,
  "As built: cpp-s5"): CP364 (a)'s drops.tsv and its largest causes, CP362 (a), <format>'s first slices (library.cpp now at
  <format>'s `_M_reserve` (span from std::string)), is_signed_v corrected; CP363 (a)'s two samples wait in `test/cpp/runtime/pending/` for the oracle tier (its commands in the
  survey); CP365-CP366 proposed; `cpp-s5b` (2026-11-11, "As built: cpp-s5b"): C++'s line as one branch, cpp-s5 with review-fix-2's
  C++ commits (review 1111's #1, #4-#7; its goi commits wait for W2) and the night review's #1-#5 (a consteval constructor's storing body
  analyzed, 128-bit constants exact or not constants, one representation of unsigned 64-bit values in the template text, TLS kinds as
  clang's), the 82 run programs' IR byte for byte cpp-s5's; CP367-CP371 proposed.
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
  RF12-P8), its rest built at RF20 (below). RF14 (`rust-rf14` on rust-rf13e, "As built: RF14 (RFP48)"):
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
  66 of 67 own, independence unchanged. RF20 (`rust-rf20` on next, "As built: RF20"): RFP52's rest (crate-private traits, aliases,
  constants and macro-made constructors in the native interface and collect's names, out-of-line modules' items, the impl's trait
  in scope, derive(Debug) of a cfg'd-out struct): core-check 5,576 -> 5,594 equal, refused 24 -> 6 (core-essential 564 -> 566);
  `gate_key` unmoved. Next: `try_process`'s associated item bounds, opaque return types in the native interface (RFP57), RFP56's
  census. RIND (`h-rust-indep`, 2026-11-17; FRONTEND-SURVEY, "RIND", "As built: RIND (stage 1)"): RIND1-RIND7 proposed (the
  inputs C1-C7 cheapest first: C5, C7, C1, C3+C4, C2+C6, then the records; 25-34 days to 90); stage 1 built: the allocator shim
  Arbace's own (`arbace.rust.back.shim`, `bin/rsi shim-check` 73 of 73), C5 `:own` on the cell's cases, independence 80 unchanged.
  Next: RIND3's step, RIND4 (ci on `c/target`, one runtime-ci night).
- **LLVM IR** ([llvm/LLVM-CROP-SURVEY.md](llvm/LLVM-CROP-SURVEY.md)): LLS28 (a), the 282 modules packed once with clang at
  night; LLS40-LLS42 (above); `llvm-compl2` (2026-11-10): llvmi on the checkout's ci and RES34's cilib, eight Verifier rules
  ported, completeness 97.6.
- **Scheme** ([scheme/SCHEME-SURVEY.md](scheme/SCHEME-SURVEY.md)): SCS2 (a), the Chez dumps pack with the oracle-records
  merge; SCS22-SCS24 (above); `scheme-aot1` (2026-11-11): SC90 (b) built, the AOT cell 90.0 -> 94.0 (100 without by design),
  proposals SCS29 (one graph per program), SCS30 (SC94 (b)'s half); `scheme-scs30` (2026-11-11): SCS29 (a) kept, SCS30 (a) built: compiled
  libraries installed in psyntax, proved by `si/eval-library` (AOT cell 48 / 3 / 0).
- **C** ([C23-SURVEY.md](C23-SURVEY.md)): CS281 (a) at W ((c) until); CS275 and CS280, the gmp and pcre2 packs at the next
  pack recording. CS299 (`c-py1`, 2026-11-12: the Python line's requests, an included file's `#pragma pack`, the generic
  `__atomic` forms on float/double, ci-run's self-checking programs) moves gate_key: it merges after W2 (nights 13-14), then
  `bin/pyi-clib build ctypestest` on next and the pyconfig.h atomics workaround dropped (Python's line).
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
- WebAssembly, the new crop ([wasm/WASM-SURVEY.md](wasm/WASM-SURVEY.md); WAS1-WAS26 decided as recommended, the user 2026-11-13;
  branch `wasm-wa1`): WA1 built (repr/wasm's binary format, every Wasm 3.0 instruction, the 20 Go-built modules byte for byte, the
  pins fetched: the testsuite, the spec at wg-3.0, wazero, webgpu.idl; wabt, wasmtime and node installed for the oracle tier) and WA2
  (the text format and the testsuite's scripts at the read and print levels: "As built: WA2", "As built: WA2 (rest)": the
  proposals/, legacy/ and custom/ scripts classed by proposal, @custom and @name, the messages' second level); branch `wasm-wa3`: WA3
  built (`arbace.wasm.valid`, Wasm 3.0's validation: the root scripts 7,182 of 7,182 read cases, 4,668 of 4,668 messages; "As built:
  WA3"); Go's SIMD encoding verified (a `nop` after each SIMD opcode at or above 0x80); WAS27-WAS29 decided as recommended (the
  user 2026-11-13). Branch `wasm-wa4`: WA4 built (wai, the engine in Go forms as a light driver, with SIMD128 and relaxed SIMD and
  GC; `bin/wai check`: the root scripts 60,498 of 60,498 commands, 5,192 of 5,192 messages, the 35 beyond classed by proposal;
  doc/wasm/WAI.md, "As built: WA4"); WAS30-WAS32 decided (a) (the user 2026-11-13). Branch `wasm-wa5`: WA5 built (wai's own
  validator, 5,237 of 5,237 root modules agreeing with arbace.wasm.valid; WASI preview 1; `bin/wai go`: `$GOROOT/test`'s 1,051 run
  programs and 6 own programs built for wasip1 and run on wai, 1,001 pass, 53 n/a, 3 over budget; "As built: WA5"). The REAP row
  read 7,840 / 0 / 454, eval 61,890 / 0 / 1,046, AOT "no check yet". WAS33-WAS37 decided (a) (the user 2026-11-13). Branch
  `wasm-wa6`: WAS33 (the data count section written by the encoder and the text reader, required by the decoder, wai's validator
  strict), WA5b (the 131 directory tests built for wasip1 as Go's test driver builds them: 123 pass; the Go corpus 1,124 of 1,188) and
  WA7's first part (w2g: wai's internal code translated to Go packages over the interpreter's own cases, generated from exec.go;
  `bin/wai check --aot`: the testsuite at AOT equal to eval, 60,498 of 60,498 root commands, every function translated; `bin/wai go
  --aot`: 18 of 18, the 3 over eval's budget among them; "As built: WAS33, WA5b, WA7 (first part)"). Branch `wasm-wa7` (taking
  WAS38-WAS42 as recommended): WA11 (gojs, the headless world in wai: Go's js/wasm imports after the pinned wasm_exec.js, the JS value
  model, a small eval, fs, a virtual clock, the page's recording fakes; syscall/js's tests 37 of 37 on wai, 6 own js programs) and WA12
  begun (the shim `wasm/web/index.html` and `arbace.js` with its import-list check, the WebGPU bridge's command buffer, the demo scene's
  90 frames against its stated values; `bin/wai js` 47 of 47; `bin/wasm build|serve|check`; doc/wasm/WEB.md; "As built: WA11, WA12
  (begun)"); WAS40 (a)'s rotation declared, unspliced. Branch `wasm-wa8` (WAS43-WAS49 taken as recommended): WAS40 (c), w2g by
  shape (a function's immediates read from its code: the 1,176 Go modules' 1.6 million functions are 8,869 shapes; one AOT program a
  slice of 400 programs; functions over 2,000 instructions in segments): the whole Go corpus at AOT in 37 minutes, 1,127 of 1,188 pass,
  61 n/a, equal to eval but the 3 over its budget (WAS51 (a): the rotation removed); WA8's rest, the legacy exceptions (try, catch,
  delegate, rethrow) at read, eval and AOT, legacy/ all passing; REAP read 7,854 / 0 / 440, eval 62,088 / 0 / 971, AOT 62,091 / 0 /
  971, print 2,785 / 0 / 0 ("As built: WAS40 (c), WA8"). To decide: WAS50-WAS54 (taken as recommended on the branch). Next: WA9
  (Arbace's own js build, at the key window), then the scene from Arbace with the REPL in the page; a persistent cache of shape
  packages; the proposals beyond 3.0 (threads WA13).
  The branches move `gate_key` once (WA1's crop paths' lists): they merge in a key window.
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
