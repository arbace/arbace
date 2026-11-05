# C++ print's by-design cases, reviewed (2026-11-05)

Brief: `[:cpp :print]` in `test/reap/matrix.edn` is 1,203 pass / 914 by design / 68 deferred. The 914 are two classes of
`test/cpp/divergences-print.edn` (`:kind :design`): `oracle-rejects` 833 (clang, the oracle, rejects the file: no AST, nothing to
round-trip) and `gxx-objects-location` 81 (49 recorded in the results, 32 more in the known file's lists not recorded yet). As
`doc/go/GOI-DESIGN-REVIEW.md` did for Go (branch `go-design-review`), each class is read here for what is reachable, so that "by design"
shrinks to what cannot be done. Days are one person's, rough. The measurements are sample checks, light tier (no gate, no corpus run).

How the numbers were made: the recorded rejected names (`test/repr/cpp/roundtrip-oracle.edn`: 745 of g++.dg's every-10th, 88 of g++.old-deja's,
79 of c-c++-common's; 912 with their records' duplicates) were found in the pinned gcc 15.2.0 tarball (extracted into the worktree's `.tmp/`), and
classified by their own directives and by clang's first error (`clang++ -fsyntax-only` at the file's `-std=` or gnu++17: an approximation of the
list's modelled options, so the by-message counts are indicative, the by-directive ones exact). The native reader (`cpp-native2`, 94d34b82d, in a
detached worktree, read-only) read 48 of them (seeded sample): read natively, printed, read again natively, the two forms compared.

## Findings in one table

| class | n | verdict | reachable by | days | cases/day |
|---|---:|---|---|---:|---:|
| `oracle-rejects`: the file has `dg-error`/`dg-bogus` (gcc expects diagnostics too) | 546 | **not by design of the print**: the file is invalid by Sema or syntax, so clang has no AST, but a parser needs none. The print can be judged without the oracle: native read, print, native read equal; the print's diagnostics the original's | native reader (`cpp-native2`), a judge level "rejected": forms equal natively, then the diagnostics equal (the error messages of clang and g++ on the original and on the print, line-insensitive) | 2-3 (with the next) | ~180-270 |
| `oracle-rejects`: g++'s modules (`-fmodules-ts`, `_a/_b` units, `import`) | 102 | reachable: the native reader reads modules (NATIVE.md §3.11); `bin/repr-cpp dg --reader native` already runs the modules part's round trip natively (417 of 422 passes the same) | native reader, the same judge | in the above, 0.5 to put the list in the print cell | - |
| g++-only flags and features (analyzer, plugin, pch, tm, strub, harden, simulate-thread, lto) | 43 | print is judgeable like the first class by forms; the *code* levels are not (clang does not take `-fanalyzer`, `-fgnu-tm`, plugins) | native reader; level 4 only where clang takes the flags | 0.5 | ~85 |
| target-specific, decimal floating point, machine modes | 21 | **by design** at level 3 and 4 on this host (aarch64 or arm intrinsics, `_Decimal32`, `__attribute__((mode))`: clang rejects); print judgeable by forms | native reader (forms level only) | 0.3 | ~70 |
| include not found (headers outside the extracted tree: `../gcc.dg/...`, pch `.H`) | 4 | a harness fact, not design: the corpus's extraction lacks the directory or a pch is not built | a list that carries `gcc.dg/` | 0.2 | 20 |
| other: clang lacks a g++ extension or differs (`__builtin_shuffle`, `__builtin_add_overflow_p`, `contracts`, `-fcoroutines`, `concepts-ts`, `-Wbidi-chars` ...) | 196 | split: 29 are `coroutines/`, whose rejection is `<coroutine> requires -fcoroutines`; at `-std=c++20` clang accepts 18 of them. The rest are g++ extensions or g++'s own checks | 18: the list's modelled options (map `-fcoroutines` to c++20 for clang); the rest: native reader, forms level | 0.5 (18) + the native judge | 36 (18) |
| `gxx-objects-location` | 81 | **by design while the print is canonical**: ubsan 26, dwarf2 14, gcov 4, asan 7 embed the source line and column (`__ubsan_data`, line tables); `:lines` keeps lines in macros only (`#line N`, CP91 (b)), no columns | a column-keeping print is `:full` mode (byte for byte, trivially the same text); a weaker level-4 judge for g++ (see CPR3) | 2-3, uncertain | ~15-25 |

Totals: **~700 of the 833 `oracle-rejects` are reachable** by one feature, the native reader in the print cell; ~60 stay at the code
levels by design (flags and targets clang does not take) while their forms level is reachable; 81 `gxx-objects-location` stay by design at
level 4's g++ half.

## The sample check

48 rejected files, seed 7 (34 g++.dg, 8 g++.old-deja, 6 c-c++-common), `bin/repr-cpp read --reader native FILE --std S`, then `print`, then read
again. Result at the time of writing (the run continues in the background, last numbers below): of the files that finished, the native reader read
every one (including ones clang rejects, `DRs/dr2262.C` with `[[]] asm(...)`), printed every one, and read the print to the same forms for 22 of 26 (the run was stopped there, about half of the 48); the 4
that differ are the converter's/native parser's open cases, not the rejection: `__integer_pack` in a pack expansion (text form on the second
read), a coroutine `co_return` kept as text once and not the other time, `std::coroutine_handle<promise<int>>` read with a different macro
spelling. Those would be `forms-differ` or `print-rejected`, the existing deferred classes. No file was refused by the parser.

That is the review's main finding: the 833 are not "no AST", they are "no clang AST"; Arbace has a second reader since CX11.

## The classes, read

### oracle-rejects with dg-error (546)

All g++.dg/c-c++-common tests whose directives say gcc itself errors (`dg-error 546`). The read cell's CX9 dg check already judges them on
rejection and error position. The print cell cannot compare ASTs or objects (neither compiler produces them). What can be compared:
(1) forms equal after print (native reader both times); (2) the print is rejected too, with the same diagnostics as the original's, compared as the
multiset of (clang error, g++ error) texts without line numbers. (2) is cheap with the dg machinery (`test/cpp/ref-dg-gxx15.edn` records g++'s
errors by line; the lines move in `:lines`, so compare by message). Many errors are Sema's (no matching function, access, constexpr): the
parser reads them without Sema, so no wait on `cpp-sema`.

Not reachable by this: a syntax error the native parser rejects (the print would have no forms): by design, as the Go cell's invalid programs
that are not syntactically valid (there: raw forms). The sample had none; NATIVE.md §5 lists "mutants for the error recovery" as left.

### modules (102)

`g++.dg/modules/*_a.C`, `_b.C`: separate units needing each other's BMI; clang's `-fmodules-ts` is not g++'s. The native reader read all five sampled
(print and read again the same, one pending). Reachable by the same judge.

### g++-only flags, target, dfp (64)

The print is judgeable by forms; objects are not (clang rejects the flags). Remain by design at levels 3 and 4: 64 cases whose level is "forms only".

### coroutines and flags the harness maps (29, of which 18 reachable by config)

`<coroutine>` of libstdc++ needs `__cpp_impl_coroutine`, which clang defines at c++20; the list's std for these files is g++'s default gnu++17 with
`-fcoroutines`. Mapping `-fcoroutines` to `-std=c++20` for the oracle makes clang accept 18 of the 29 (11 are tests of errors: `co_yield` outside a
function, missing `initial_suspend`, VLA in a coroutine ...). Small and certain, but it adds 18 files to the full round trip whose results are unknown (some
may land in `print-rejected`: coroutines are 7 of the `gxx-objects-location` already). Not built: it changes the list generator and the recorded
`roundtrip-oracle.edn`, results and known files, i.e. a re-record of the three lists (not light).

### gxx-objects-location (81)

ubsan (`c-c++-common/ubsan` 17, `g++.dg/ubsan` 9), dwarf2 (14), asan (7), gcov (4), opt/ipa/tm/gomp/guality/debug (the rest ~20). The recorded
reason is right: g++'s object holds the line *and column* of each check or each line-table row. Level 3 (the AST) and level 4's clang half pass for
them; only g++'s half differs. A `#line` for each statement would give lines but not columns, and the canonical layout puts columns by its own
rule, so no printer feature closes it short of keeping the text (`:full`). The remaining ~20 (opt, ipa, tm, gomp) were not each read: the
survey's CP48 says the object depends on positions; they may include some whose cause is different (a candidate for a per-file look, 1 day, not
done).

## Ranked, cases per day

1. **The native reader as the print cell's reader for rejected files** (the judge "rejected": forms equal, diagnostics by message): ~700 cases, 3 days: **~230/day**.
2. -fcoroutines mapped to c++20: 18 cases, 0.5 day: 36/day.
3. A weaker g++ half of level 4 for location-dependent objects (compare g++'s objects with `-g0` and with the sanitizer's location data stripped, or the
   GIMPLE dumps without locations): ~50 cases, 2-3 days: ~20/day, uncertain.
4. Include/harness fixes (gcc.dg in the list): 4, 0.2 day.

## Design of the top item (CPR1)

A new verdict for the print cell, `:rejected-roundtrip`, in `bin/lib/repr-cpp/roundtrip` (and `rtcheck`):
- for a file the oracle rejects (recorded), instead of the verdict `oracle-rejects`: read natively (`--reader native`), print, read the print natively
  (level 1 as now), then **level R**: the print compiled by clang and by g++ with the original's options fails like the original: the same count of
  errors and the same error texts without positions (`error: ...` lines, sorted).
- A file whose native read declines or throws stays `oracle-rejects` (still by design: invalid beyond the parser). Passing files move to `pass`; a
  forms difference to `forms-differ`/`print-rejected` (the existing deferred classes, honest).
- The results record gets the new verdict; `test/cpp/divergences-print.edn`'s `oracle-rejects` text says "or the native reader cannot read it".
- Uses `cpp-native2`; no dependence on `cpp-sema` or `cpp-codegen`: the sample's differences are the converter's, the native parser's and the printer's
  open cases. (`cpp-sema`'s Sema facts, NATIVE.md §4's classes, matter to the round trip only where a native tree differs from clang's, as in the
  read cell's `native-check`; the print's level 1 compares native with native.)
- Cost: the judge level R (1 day), the lists run natively (the cell's lists are 2,000 files at ~10 s: a heavy night run, not built), the records (0.5-1), the
  known file's new entries for the ~15% that differ (1). Total 2-3 days.

## Built

Nothing in the tree but this note: the one small and certain item (coroutines) changes recorded lists and needs the heavy re-record, and the top item
needs `cpp-native2` merged first. The sample check above is the evidence; its scripts are scratch files in `.tmp/`, not committed. The run was left to finish in the background and is stopped by 18:30 at the latest.

## Proposed

- **CPR1: the print cell reads the oracle's rejections natively** (judge level R above). (a) as designed, after `cpp-native2` is merged, 2-3 days, moves up to
  ~700 cases from by design to pass or deferred; (b) leave it, the read cell's dg check judges the rejections; (c) only the 102 modules and the 43 g++-only
  files (the cases where the native reader is the only reader), 1 day. *Recommended: (a)*; the sample reads 22 of 26 equal and none refused.
- **CPR2: reclassify** `oracle-rejects` in `test/cpp/divergences-print.edn` to two classes after CPR1: `oracle-rejects` (design: the parser cannot read it, or the
  compilers' flags and targets are not clang's) and the moved cases judged at level R. (a) as recommended; (b) keep one class.
- **CPR3: the g++ half of level 4 for location objects** (`gxx-objects-location`): (a) leave by design, as decided (CP48), the `:full` print covers byte-for-byte;
  (b) a locations-stripped comparison, 2-3 days, ~50 cases at most; (c) split the class by cause after a per-file read (1 day). *Recommended: (a) now, (c)
  with the next re-record.*
- **CPR4: `-fcoroutines` to c++20 for clang in the list's modelled options** (18 files accepted, 11 more stay rejections of error tests): (a) with CPR1's
  re-record, (b) now with its own re-record. *Recommended: (a).*

## Decided (2026-11-05, the user)

CPR1-CPR4 as recommended: CPR1 (a), the print cell reads the oracle's rejected files natively with the new judge level, after cpp-native2's merge; CPR2 (a), oracle-rejects split after it; CPR3, gxx-objects-location by design now, split by cause at the next re-record; CPR4, -fcoroutines mapped to c++20 for clang with CPR1's re-record.

## As built: CPR1, CPR2, CPR4 (2026-11-05)

On the branch `cpp-cpr1` (from `cpp-print-review`), light tier (stage 0 by `ARBACE_HOST_EXE`, 4 jobs, no gate, no corpus run).

- **The merge.** `cpp-native2` (94d34b82d) merged (d71d82395). Two conflicts, both additive: `bin/repr-cpp` (CX10's `roundtrip-corpus` verb and
  CX9/CX11's `dg` and `native-check` verbs all kept, `read|print` with CX11's `REPR_CPP_HOST`; the header's `--reader native` line kept under
  `roundtrip`) and `doc/cpp/CPP-SURVEY.md` (CX9's and CX11's As built sections and CX10's, all kept). `cpp-tree` not merged: the print's level 1
  compares native with native, the tree check (native against clang's tree) is not needed. One fix the merge called for:
  `test/cpp/selftest/known-names.sh` knows CX9's corpus `gxx-modules/` (the known file's five modules names were "not qualified").
- **CPR1, the judge level R** (`bin/lib/repr-cpp/roundtrip`, `arbace/repr/cpp/rtcheck.ae`). A file whose original clang rejects is read natively
  whatever `--reader` says (rtcheck `run`'s new ORIG argument, the original's dumps: its status and diagnostics are clang's rejection), printed,
  and its print read natively again (level 1, the two `.ae` texts equal). Then level R (`rtcheck/level-r`): the print fails like the original for
  both compilers: clang's diagnostics are the oracle's dumps' (`WORK/orig/NAME.err`, `WORK/dump2/NAME.err`), g++'s a `-fsyntax-only
  -fdiagnostics-plain-output` run with the file's g++ options on the original and on the print (`WORK/diag/{orig,print}/NAME.gxx.{log,status}`);
  each compiler's status (accepting or not) the same and its errors the same multiset by message (`diag-errors`: the `error:`/`fatal error:` lines
  from `error:` on, sorted, a `FILE:LINE[:COL]` inside a message dropped). No AST nor objects are made for them (level 4 skipped). The verdicts:
  level 1 and R hold (and level 5, mode `:full`, when run): **pass**; the native read refused or threw: **oracle-rejects** (as before, the design
  class); else a failure unless the known file classes the file **:rejected-differs**. results.tsv's read column says `rejected-native` (with
  `:clang` or `:gxx` when level R fails), the summary counts `:rejected-native` and `:rejected-native-pass`.
- **CPR2, the split.** `test/cpp/divergences-print.edn`: `:rejected-differs` (deferred: the native parser's, the converter's and the printer's open
  cases, level 1 or R) beside `:oracle-rejects` (design, now "the native reader cannot read it"); the known file's `:classes` has its text and the
  judge's `fits` maps it to the levels `:rejected-l1`, `:rejected-r`; `test/reap/main.ae`'s oracle-rejects text says so.
- **The re-record's tool.** `bin/repr-cpp roundtrip ... --record-known-rejected`: the judge's rows first, then `rtcheck known-rejected` writes the
  failing rejected-native files (levels 1 or R, no known entry yet) into the known file's `:files` as `:rejected-differs` (a block after `:files {`
  under a comment naming the key), then the judge again; `bin/repr-cpp dg --corpus modules --record` passes it too (the modules part's round trip
  reads its rejected importers natively as well).
- **CPR4.** In the round trip's list (not the corpus lists, so pp-check's tokens are unchanged): a file of g++.dg's `coroutines/` (coroutines.exp
  gives it `-fcoroutines`) or whose g++ options have `-fcoroutines`, below C++20, is read and compiled by clang at `c++20` (`gnu++20` for gnu++),
  g++ keeping its standard: `-std=STD -fcoroutines` before its options. Two coroutine files clang now accepts and g++ rejects are the known
  file's `:orig-fails` (`gxx-dg/coroutines/pr105475.C`, `pr115858.C`, `#{:gxx}`).
- `test/cpp/selftest/rejected-native.sh`: pass, level R by g++ and by clang (clang accepting the print), the native refusal staying
  oracle-rejects, the summary's counts, `known-rejected` and the judge then known.

Checked (commands from the worktree, `ARBACE_HOST_EXE=/root/arbace/target/seed/amd64/arbace`):
- the samples of the oracle's rejected files (from the every-10 lists, `bin/repr-cpp pp-check --corpus C --sample 10 --lists-only`; lists without
  a key, `bin/repr-cpp roundtrip LIST WORK --no-objects`): the review's seeded 48 (seed 7) and 14 more of g++.dg (6 modules), 4 of g++.old-deja
  and 4 of c-c++-common, then 100 more of g++.dg (seed 23): **162 files not under `coroutines/`, 144 pass (89%), 18 `rejected-differs` (3 at
  level 1, 15 at level R), none refused by the native reader**; of the seeded 42 not under `coroutines/`, 40 pass. The 18 are the printer's and
  the native parser's: `u8` literals at `-fchar8_t` (the print's static assertions fail), `__integer_pack` read as text, an invalid
  `template enum`, `namespace X alignas`, a top-level `asm` and the like, where the print of an invalid construct is not the original's.
  About 21 s for 100 files at 4 jobs.
- `coroutines/` (the 32 recorded rejections of gxx-dg-every-10, with objects): clang at C++20 accepts **19**: 17 pass every level, 2 are the
  `:orig-fails` above; of the 13 it still rejects, 12 pass at level R, 1 (`ramp-return-a.C`, `-std=c++14`) differs at level 1.
- `bin/repr-cpp roundtrip-corpus gxx-dg-essential` (the gate's cpp-print-essential): its 2 recorded rejections (`DRs/dr2262.C`,
  `cpp1y/auto-fn18.C`) pass at level R, 2 improvements; recorded again (`--record-results`, `roundtrip-results/gxx-dg-essential.edn`: 33 files,
  the two `:pass`); `roundtrip-corpus samples`: 8 pass, 1 known, as before.
- selftests: known-names, known-levels, results-hold, results-record, results-nonpass, rt-objects, rejected-native: ok.

The measure, by the samples: of the 833 `oracle-rejects` (g++.dg 745, g++.old-deja 88), about **19 leave by CPR4** (clang accepts them, judged at
every level), and of the rest about **89% (~725) move to pass and ~11% (~90) to `rejected-differs`**, ~0 stay `oracle-rejects`; c-c++-common's 79
the same way. The exact numbers are the night's re-record.

The night's command (the re-record, a heavy run: the three lists' every-10 and the modules part; about 2,500 files, the rejected ones fast):

    for k in gxx-dg-every-10 gxx-old-deja-every-10 c-c++-common-every-10; do
      bin/repr-cpp roundtrip-corpus $k --record-oracle --replace-oracle --record-known-rejected --record-results --replace-results
    done
    bin/repr-cpp dg --corpus modules --record     # gxx-modules-*: its rejected importers read natively

then `c-c++-common-every-10` out of `test/repr/cpp/roundtrip-results/pending.edn` (its record now exists), the new `:rejected-differs` entries
read over (each a case for the printer or the native parser), `bin/reap` (C++'s print cell). Until it runs, a whole-list run of a corpus whose
recorded rejections now pass fails as an improvement (`-all` keys: gxx-modules-all) and a recorded `:oracle-rejects` that is now
`rejected-differs` fails as a change; the every-10 sample runs count improvements only. CPR3 is unchanged (by design now).

Left: the re-record above; the `:rejected-differs` cases themselves; the native parser's error recovery (a syntax error it rejects stays
oracle-rejects, none in the samples).

