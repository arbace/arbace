# The crops' scores: completeness, maturity, independence

`bin/crops-score` (and `make crops-score`) gives each of the eleven crops three percentages, measured from the recorded
results alone, as `bin/reap` judges the REAP matrix: no check is run and nothing is estimated. The user asked for it on
2026-11-06. The goal is every crop above 90% on all three. The logic is the namespace `crops.score`
(`test/crops/score.ae`, on the tools' host). Its data are `test/crops/crops.edn` (the crops: their gate steps, their held
branches, the rules that find their negative controls) and `test/crops/independence.edn` (the reviewed facts, stage by
stage).

```
bin/crops-score                       the checkout's records, every crop: a table, then each crop's evidence
bin/crops-score --ref REF             REF's records, read by git show (no checkout)
bin/crops-score --newest              each crop on its newest held branch (crops.edn's :branches over refs/remotes/mac,
                                      the newest by committer date whose test/reap/cells.edn has the crop's row)
bin/crops-score --crop C              one crop (clojure java go c cpp ocaml scheme haskell rust llvm python)
bin/crops-score --tsv                 one line a crop: the three scores, each cell's counts, each item, each stage
bin/crops-score --input FILE          REAP cell records over the tree's (test/reap/matrix.edn's format, a vector or
                                      {:cells [...]}): the numbers of a crop reported apart, Python's from the Mac VM
bin/crops-score --judge               the checkout's matrix judged afresh by reap.main's own readers and judging; a
                                      difference from test/reap/matrix.edn is a problem (exit 1)
bin/crops-score --strict              a fact of independence.edn the tree's gate steps refute is a problem (exit 1)
bin/crops-score --record              the checkout's scores of every crop written into test/crops/scores.edn (CSP1 (b));
                                      without it, scoring the checkout (no --ref, --newest, --input) compares each crop
                                      with its line there, and any difference is a problem (exit 1)
```

Each cell and crop also prints its completeness without the by-design cases, pass / (pass + deferred + pending),
beside the strict number (CSP4). The gate's essential step `crops-score` runs `bin/crops-score --judge --strict` on stage
0 after `reap` (CSP1 (b)).

It takes about 1 second for the checkout, 5 with `--judge` or `--newest` (eleven refs read by `git show`), under 0.5 GB.

## Definitions

### Completeness

Per cell (read, eval, AOT, print): pass / (pass + by design + deferred + pending), as the tree's REAP matrix record
`test/reap/matrix.edn` counts them over the cell's recorded corpora. The crop's completeness is the mean of its four
cells. A cell with no check yet, a crop with no REAP row and a cell with no case count 0 and are flagged. The cells' raw
counts are printed beside each percentage.

- *Refinement (the data's).* The matrix record is bin/reap's judged output, not a second judging. bin/reap fails when the
  record differs from the checks' records (report 021's finding 1), so the record is as current as the gate's `reap` step
  allows. Re-judging a held ref would need that ref's own `reap.main` and readers: several branches add readers
  (`:scheme`, `:hsi-check`, `:corpus-known`, `:ocaml`, `:cpp-roundtrip`, `:rust-lower`). `--judge` re-judges the checkout
  with `reap.main` itself. On main at 8d0bb009d, 16 of 16 cells equal their record.
- *Refinement.* A cell whose sources all record only their non-passes (`:pass nil`) scores pass / cases with pass 0 and is
  flagged "passes not recorded". Python's cells on `mac/mac/201-python-py7-py11` are such: its records live on the Mac VM
  (queue task 221 reports them; `--input` takes them).
- *Refinement (CSP4, decided).* By design counts in the strict denominator, as the brief says. The number "without by
  design", pass / (pass + deferred + pending), is printed beside it, in the table, the details, the TSV and the record. A class of kind `:na` (goi-not-applicable's: Go's
  AOT invalid programs) is outside the matrix's counts already, and so outside both denominators.

### Maturity

Six items, each an equal share (a sixth). Each is measured on the tree scored:

1. **The four cells recorded**: checked cells / 4 in `test/reap/matrix.edn` (a fraction).
2. **Every non-pass in a class**: every checked source's class counts sum to its non-passes (1 or 0; 0 with no checked
   cell). Pending classes are classed: they are printed with their count, not deducted.
3. **Negative controls for its check families**: of the crop's cells' check sources (`:role :check`, not `:until`), the share
   that a rule of `crops.edn`'s `:controls` covers. A rule names the sources by a regexp over the source's name, a control
   file and a regexp its text must hold in the tree. The families are those of doc/GATE-SURVEY.md GTS81 (a) and its crops'
   table ("Every check family", P and M).
4. **Gate steps**: 0.5 for an essential step and 0.5 for a `--full` step in `test/gate/steps.edn`. A step only in a GT9
   fragment (`test/gate/crops/CROP.edn`, a merge's draft) counts half of that. The oracle tier (`:phase :oracle`) is listed
   but does not count. A crop's steps are the table's `:crops` entry and the step names `crops.edn`'s `:steps` matches.
5. **A REAP row**: the crop in the tree's `test/reap/cells.edn` `:languages` (1 or 0).
6. **Records reproducible**: the share of the crop's record files (the cells' sources' `:file`, `:results`, `:known`,
   `:ref-runs`, `:ref`, `:same-cases`, `:upstream`) that hold no absolute path of the host (`/root/`, `/home/`, `/tmp/`,
   `/Users/` not inside a word). The literals of the tests themselves are taken out first (`crops.edn`'s `:abs-path-allow`:
   jtreg's TestUnlimited's argument `"/tmp/unlimited"`).

- *Refinement.* Items 1, 3, 4 and 6 are fractions, not 0 or 1. With binary items, one record holding a path would take a
  sixth off Java, Go and C alike, whatever their share of clean records.
- *Refinement.* "Records reproducible from pins" is measured as "holds no host path". That a record's corpus is pinned is
  not measurable from the record. The paths found are real ones: messages that name `/root/jdk26u/...` (Java's
  `ref-jdk26.edn`), `/root/tamago-go/test/...` (goi's AOT record), `/home/buildozer/aports/...` (musl's build paths in
  `ref-gcc15.edn`, `ref-gxx15.edn`) and `/root/arbace/.tmp/repr-c/...` (C's annotation and round-trip results).

### Independence

Five stages: read, eval, AOT and print are the stage's pipeline (Arbace's path from its input to its output, and the inputs
it consumes: OCaml's Lambda, GHC's STG, rustc's and clang's IR, the helpers' dumps, installed headers). The judge is what
the crop's checks compare with at check time. Each stage scores:

- **1**: no host toolchain at check time, and no record of one consumed;
- **0.5**: none at check time, but the stage consumes or compares with a record that a host toolchain made and that is
  re-recorded by hand, or the toolchain runs only in the oracle tier (by hand, at a tag);
- **0**: a host toolchain runs at check time in the stage (a live dump, a live reference run, a toolchain package's
  installed tree read in place), or the stage does not exist on the ref.

The crop's independence is the mean of the five. The host toolchains are GT9's hosts `:gcc :cxx :llvm :ocaml :chez
:chibi :ghc :rustc :python`, and the JDK beyond the frozen jar. Not counted: Arbace's executables, TamaGo's go (the seed's
toolchain, pinned by `seed/goroot.sha256`, its sources as Go forms in `tamago/cmd/`), and the frozen v4 jar (pinned by
`test/oracle/frozen-jar.sha256`).

The facts are reviewed data in `test/crops/independence.edn`. Each stage has its tool, the host toolchains it needs, the
reason, its evidence (file:line, REF:file:line on a held ref), the gate steps that run it, and where it was run with stubs.
`:at` holds the facts that differ at a held ref. The tool **cross-checks** every claim with the scored tree's gate steps. A
stage claimed above 0 whose named step runs on a toolchain host outside the oracle tier scores 0 and is printed "CLAIM
REFUTED". A claim of 0 whose steps are on no toolchain host is printed too. With `--strict` either is a failure. When a
fragment and `steps.edn` both have a step, `steps.edn`'s wins: the fragment is a merge's draft.

- *Refinement.* The judge is a stage of its own. Otherwise every round-trip check would make the stage "print" 0 even
  where the printer is Arbace's alone. Every crop's printer is Arbace's (print 1). What GHC, rustc, clang or ocamlc does
  with the print is the judge.
- *Refinement.* Installed headers read in place (C's gcc include directory and musl-dev, C++'s libstdc++) count as the
  toolchain (0). doc/ORACLE-RECORDS-SURVEY.md's class E: "the goal excludes the package all the same".
- *Refinement.* Go's judge is gc, run live, but gc is TamaGo's pinned go. It scores 1 (Proposed CSP2).
- *What the cross-check found.* The first run refuted six claims at `mac/oracle-records-cpp2`. Four were the survey's: OCaml's
  read, eval, AOT and judge at 0.5. That branch's scripts read OCaml's packs, but its `steps.edn` still plans every OCaml step
  on `:ocaml`. Only the fragment `test/gate/crops/ocaml.edn` drops it, and that "on merge". OCaml scores 0 there, and the data
  say why. The other two were the data's own mistakes. Haskell's `haskell-native-essential` (`:ghc`) is native-check, a
  judge. The rsi steps' `:cxx :gcc` are the runtime's build, a pipeline use. Both were listed under the wrong stage and have
  moved.

### The stub runs (2026-11-06, the checkout at 8d0bb009d, main's stage 0)

`.tmp/stubs/bin` held `clang clang++ clang-23 gcc g++ cc llvm-as opt lli ocamlc ocamlopt ocamlfind ghc ghc-pkg rustc
rustdoc cargo chez scheme chibi-scheme java javac`. Each was a script that logged its call and exited 99, first on `PATH`.
After each run the log was empty.

| stage | command | result |
|---|---|---|
| LLVM read, print | `bin/repr-llvm read test/cpp/selftest/atomic-libcalls.ll`, `print`, `read` again | exit 0; the forms equal but the `in-ns` (the file's name) |
| Scheme read, print | `bin/repr-scheme read --full`, `print --full` of `test/scheme/chibi/driver.scm` | byte for byte |
| Scheme eval | `bin/si check --only si/amb --only si/data` | 2 of 2 pass, the control amb-vs-data differs; 554 MB peak |
| Clojure print | `bin/repr-clojure roundtrip .tmp/frozen/clojure/walk.clj` | 1 of 1 equal |

The 0 claims need no stub run: each names the live call (the evidence lines). Most come from the agents' reading of the
scripts at each ref and from doc/ORACLE-RECORDS-SURVEY.md §1.2. Where a gate step exists, its `:hosts` confirm them.

## Each crop's sources

The cells' sources are each tree's `test/reap/cells.edn`. The baseline's refs:

| crop | REAP row at | cells' checks (sources) | controls (`crops.edn` rules) | gate steps |
|---|---|---|---|---|
| Clojure | main | the suite (`test/arbace-go-results.edn`, `-aot-`), the oracle, repr/clojure's rules and round trip | `test/clojure/controls.edn`, `test/oracle/controls.edn` | main's `:clojure` |
| Java | main | `bin/java-check` read, classes, diagnostics, interp, compiled, print (`test/java/check-*.edn`) | `test/java/controls.edn` (c00001, control-diag, control-print) | main's `:java` |
| Go | main | `bin/repr-go native-check`, `bin/goi check`, `--aot`, `bin/repr-go roundtrip` (4) | `test/goi/controls.txt` (interp, aot) | main's `:go` |
| C | main | `bin/ci dg`, `bin/repr-c check --tokens`, `parse-check`, `bin/ci check`, `--aot`, `bin/repr-c roundtrip` | `test/c/controls.edn` (:dg :pp :tree :run :roundtrip) | main's `:c` |
| C++ | `mac/cppi-deferred` | `bin/repr-cpp pp-check`, `dg`, `bin/cppi check`, `--sample 25`, `--aot`, `bin/repr-cpp roundtrip` | `test/cpp/controls.edn` | steps.edn's `cpp-*` |
| OCaml | `mac/ocaml-libs4` | `bin/repr-ocaml roundtrip` read and print, `bin/oi check`, `suite check`, both `--aot` | `test/ocaml/controls.txt` (judge lines) | steps.edn's `oi-*`, `ocaml-*` |
| Scheme | `mac/scheme-smr7` | `bin/repr-scheme check`, `roundtrip`, `bin/si check`, `r7rs`, `srfi`, `chibi-tests`, `ecraven`, `--aot` | `test/scheme/si/controls.edn` | steps.edn's `si-*`, `scheme-*` |
| Haskell | `mac/haskell-hf7` | `bin/repr-haskell roundtrip` (tree, iface, text), `bin/hsi check`, `--aot` | `test/haskell/hsi-controls.txt` | none (fragment on `mac/oracle-records-cpp2`) |
| Rust | `mac/rust-row` | `bin/repr-rust roundtrip` (tree, iface, text), `bin/rsi check` (`--aot` not recorded) | `test/rust/controls.edn` (:run) | none (fragment on `mac/oracle-records-cpp2`) |
| LLVM IR | `mac/llvm-ll6` | `bin/repr-llvm roundtrip`, `verify-check` (3), `--text`, `bin/llvmi check`, `--aot` | `test/llvm/controls.edn` | none (`llvm-*` in `:cpp` on `mac/oracle-records-cpp2`) |
| Python | `mac/mac/201-python-py7-py11` | `bin/repr-python roundtrip`, `lex-check`, `parse-check`, `bin/pyi check`, `--aot` | `test/python/controls.edn`, `controls-read.edn` | none |

The independence facts' sources are each stage's `:evidence` in `test/crops/independence.edn`. They come from
doc/ORACLE-RECORDS-SURVEY.md (on `mac/oracle-records-cpp2`: §1.2's inventory, its crops' "As built") and the scripts at
each ref.

## Baseline (2026-11-06)

Measured by `bin/crops-score`, `--newest` and `--ref mac/oracle-records-cpp2` on branch `crops-score` (main 8d0bb009d), after
CSP1-CSP5. Each entry reads completeness (without by design) / maturity / independence, in percent. The record of main's line
is `test/crops/scores.edn`. CSP5 changed two numbers from the first baseline. Main's Java maturity went from 98.3 to 100
(`ref-jdk26.edn` normalized), and C's from 95.8 to 100 (`ref-gcc15.edn`, `annot-results.edn` and `roundtrip-results.edn`
normalized). The held branches keep their own records, unnormalized until they merge.

| crop | main | newest held branch | that branch | mac/oracle-records-cpp2 |
|---|---|---|---|---|
| Clojure | 99.0 (100.0) / 96.3 / 100.0 | `mac/clojure-cdr` | 99.0 (100.0) / 96.3 / 100.0 | 99.0 (100.0) / 96.3 / 100.0 |
| Java | 72.6 (78.9) / 100.0 / 90.0 | `mac/java-jc9c` | 72.6 (78.9) / 98.3 / 90.0 | 72.6 (78.9) / 98.3 / 90.0 |
| Go | 93.9 (100.0) / 86.0 / 100.0 | `mac/goi-escape` | 93.9 (100.0) / 86.0 / 100.0 | 93.9 (100.0) / 86.0 / 100.0 |
| C | 94.9 (98.6) / 100.0 / 20.0 | `mac/c-deferred` | 94.9 (98.6) / 95.8 / 20.0 | 94.9 (98.6) / 95.8 / 60.0 |
| C++ | 0.0 (0.0) / 0.0 / 20.0 | `mac/cppi-deferred` | 75.4 (96.5) / 90.2 / 20.0 | 75.4 (96.5) / 90.2 / 60.0 |
| OCaml | 0.0 (0.0) / 0.0 / 20.0 | `mac/ocaml-libs4` | 75.6 (81.5) / 94.4 / 20.0 | 75.6 (81.5) / 94.4 / 20.0 |
| Scheme | 0.0 (0.0) / 0.0 / 80.0 | `mac/scheme-smr7` | 98.3 (98.8) / 87.5 / 80.0 | 98.3 (98.8) / 79.2 / 90.0 |
| Haskell | 0.0 (0.0) / 0.0 / 20.0 | `mac/haskell-hf7` | 96.3 (98.7) / 73.3 / 20.0 | 94.9 (96.8) / 81.7 / 50.0 |
| Rust | 0.0 (0.0) / 0.0 / 20.0 | `mac/rust-row` | 75.0 (75.0) / 66.7 / 20.0 | 75.0 (75.0) / 75.0 / 40.0 |
| LLVM IR | 0.0 (0.0) / 0.0 / 40.0 | `mac/llvm-ll6` | 91.7 (92.9) / 71.4 / 40.0 | 49.5 (49.5) / 66.7 / 50.0 |
| Python | 0.0 (0.0) / 0.0 / 20.0 | `mac/mac/201-python-py7-py11` | 0.0 (0.0) / 75.0 / 80.0 | 0.0 (0.0) / 0.0 / 20.0 |

On main the seven crops outside the matrix have no REAP row: completeness and maturity 0, each cell flagged.

The newest held branches in detail. A cell's counts are pass / design / deferred / pending. The maturity items are cells,
classed, controls, gate, REAP and records. Independence lists read, eval, AOT, print and judge.

| crop | read | eval | AOT | print | maturity items | independence |
|---|---|---|---|---|---|---|
| Clojure | 908/2/0/0 | 21060/73/1/0 | 809/29/1/0 | 1031/0/0/0 | 1, 1, 0.78, 1, 1, 1 | 1, 1, 1, 1, 1 |
| Java | 7742/0/96/0 | 1848/254/1220/0 | 1921/160/1241/0 | 2598/615/109/0 | 1, 1, 1, 1, 1, 0.9 | 1, 1, 1, 1, 0.5 |
| Go | 12158/204/0/0 | 1664/173/0/0 | 1690/147/0/0 | 2584/141/0/0 | 1, 1, 0.25, 1, 1, 0.91 | 1, 1, 1, 1, 1 |
| C | 5201/231/51/0 | 2111/124/33/0 | 2120/115/33/0 | 4768/5/78/0 | 1, 1, 1, 1, 1, 0.75 | 0, 0, 0, 1, 0 |
| C++ | 5262/1192/77/0 | 70/8/3/0 | 70/8/3/0 | 1625/1665/76/0 | 1, 1, 0.5, 1, 1, 0.91 | 0, 0, 0, 1, 0 |
| OCaml | 3082/80/0/0 | 360/114/190/0 | 337/108/219/0 | 3080/0/2/0 | 1, 1, 0.67, 1, 1, 1 | 0, 0, 0, 1, 0 |
| Scheme | 1371/0/0/0 | 2815/2/13/0 | 45/1/2/0 | 1364/0/0/0 | 1, 1, 0.25, 1, 1, 1 | 1, 1, 1, 1, 0 |
| Haskell | 2690/0/0/0 | 790/76/42/0 | 138/1/0/0 | 1331/14/0/0 | 1, 1, 0.4, 0, 1, 1 | 0, 0, 0, 1, 0 |
| Rust | 33853/2/3/0 | 5/0/0/0 | no check yet | 33342/1/3/0 | 0.75, 1, 0.25, 0, 1, 1 | 0, 0, 0, 1, 0 |
| LLVM IR | 2695/1/58/0 | 218/7/33/0 | 218/7/33/0 | 282/0/0/0 | 1, 1, 0.29, 0, 1, 1 | 1, 0, 0, 1, 0 |
| Python | 0/37/0/0 (passes not recorded) | no check yet | no check yet | 0/5/0/0 (passes not recorded) | 0.5, 1, 1, 0, 1, 1 | 1, 1, 1, 1, 0 |

What keeps each crop under 90%:
- **Completeness.**
  - Java eval and AOT: 1,220 and 1,241 deferred, the world (net, security, serialization, nio, management).
  - Java print: 615 by design, its class files.
  - C++ print: 1,665 by design, the lists not recorded yet.
  - C++ read: 1,192 by design.
  - OCaml eval and AOT: 190 and 219 deferred.
  - Rust: AOT not recorded, and eval has 5 programs.
  - Python: no passes in the main checkout's records (the Mac's).
- **Maturity.**
  - Missing controls: repr/go's native-check and round trips (Go 0.25), Scheme's r7rs, srfi, chibi-tests and ecraven and
    its read (0.25), the round trips of Haskell, Rust, LLVM, OCaml and C++.
  - No gate steps on the crop's branch: Haskell, Rust, LLVM, Python.
  - Paths in records: C (3 of 12), Java, Go, C++ (1 each).
- **Independence.**
  - Every stage that consumes a toolchain's front-end product (OCaml's Lambda, GHC's STG, rustc's and clang's IR,
    cxxdump's and hsdump's dumps) scores 0 until it is packed (0.5) or native (1).
  - Every judge that reruns the toolchain on the originals or on the print scores 0 until it is recorded and the print's
    half moves to the oracle tier.
  - `mac/oracle-records-cpp2` raises C and C++ to 60, Scheme to 90, Haskell to 50 and Rust to 40. Above 0.5 per stage,
    only the native front ends help: `ocaml-typer`, `cpp-codegen`, HF's Core/STG, RF's MIR, llvmi on hand-written IR.

## Proposed (decided 2026-11-06: see Decided)

- **CSP1: a gate step.** `crops-score` as an essential step, `bin/crops-score --judge --strict` on `[:stage0]`. Its inputs
  are `test/crops/`, `test/reap/` and every record `cells.edn` names (the `reap` step's inputs plus `test/crops/` and
  `test/gate/`). It takes seconds.
  - (a) Fail only on `--judge` and `--strict` problems: a refuted fact or a stale matrix.
  - (b) As (a), and also record the scores in `test/crops/scores.edn`, failing on a difference (bin/reap's
    `matrix.edn` rule), so that every change of a score is made on purpose.
  - (c) Print only, never fail.

  Cost: (a) about an hour (the step, its declared inputs, `bin/gate --audit crops-score`); (b) about two hours more (the
  record and `--record`). Recommendation: (b), at the tag's merge, beside `reap`. A score that drops then shows in review,
  as the matrix does.
- **CSP2: TamaGo's go as a toolchain.** Go's judge (gc's runs, live) and every AOT stage go through TamaGo's go.
  - (a) As now: it is the seed's own toolchain, pinned and held as forms (S9). It is not counted.
  - (b) Count it as a host toolchain unless the box's tool guests build it (`bin/box-tools`).

  Recommendation: (a). The goal names Alpine's toolchains, and Go plus TamaGo is the model the goal adopts.
- **CSP3: Python's numbers.** Python's records are on the Mac VM.
  - (a) Queue task 221 returns the four cells in `test/reap/matrix.edn`'s format, and `--input` scores them.
  - (b) The Mac's branch commits its result files, so `--ref` scores them like the others.

  Recommendation: (b) once the branch merges, (a) until then.
- **CSP4: by design in completeness.** By design counts against completeness, as the brief says. It is a recorded decision
  that a case will not pass (C++'s print, Java's print: 1,665 and 615).
  - (a) Keep it.
  - (b) Print a second completeness without it, "of what is meant to pass".

  Recommendation: (b), printed beside the first and not replacing it. Cost: a few lines.
- **CSP5: absolute paths in records.** Six records of the newest branches hold a host path, all in messages or reasons. Options:
  - (a) Normalize them at record time (`@ROOT@`, `@JDK@`, as `mac/oracle-records-cpp2`'s manifests do), one change per
    check's `--record`.
  - (b) Exempt the `:reason` and `:error` fields from the item.

  Recommendation: (a). A path in a reason still differs between a clone and the main checkout.

## Decided (2026-11-06, the user: CSP1-CSP5)

- **CSP1 (b).** The gate step and the scores' record.
  - The essential step `crops-score` (`test/gate/steps.edn`, beside `reap`, after it) runs `bin/crops-score --judge
    --strict` on `[:stage0]`. Its inputs are `reap`'s, plus `bin/crops-score`, `test/crops/` and `test/gate/steps.edn`;
    GT9's fragments `test/gate/crops/*.edn` join them when they arrive on main. It is in `:crops`' `:shared`.
  - The scores of the checkout are recorded in `test/crops/scores.edn`, one map a crop: the three scores, CSP4's
    completeness, each cell's counts, each maturity item, each stage. Any difference fails until `bin/crops-score
    --record`, which writes only when `--judge` finds no difference.
  - Checked: `bin/gate --plan` plans it ("run crops-score, key 0d4fdbd1f463, 731 files; never passed"); `test/gate/
    costs-test.bash` passes; one line of the record edited by hand fails with both lines printed, exit 1. Not run under
    `bin/gate --audit` today (no gate runs): its first audited run is the night's.
- **CSP2.** TamaGo's pinned go is not a host toolchain. Go's judge and every AOT stage through it score as before.
- **CSP3.** Python's numbers come through `--input FILE` (`test/reap/matrix.edn`'s records) until the Mac's branch commits
  its result files. Checked with a hand-made one-cell input.
- **CSP4.** Completeness without by design is printed beside the strict number, and recorded.
- **CSP5.** Host paths are normalized when records are written. The checkout is `${root}` (the main checkout's too, when a
  worktree reads its corpora), the JDK's home `${JDK}`, Alpine's package-builder tree `${aports}`, the toolchain's GOROOT
  `${GOROOT}`. The six records:

  | record | host path | writer | state |
  |---|---|---|---|
  | `test/java/ref-jdk26.edn` | `/root/jdk26u/.../images/jdk` (one `:why`, one `:err`) | `test/java/check.ae`'s `clean`, the home from `bin/java-check --record-ref` (`JAVA_CHECK_JDK`) | fixed, off gate_key; the record's line rewritten (the fields are messages, never judged) |
  | `test/c/ref-gcc15.edn` | `/home/buildozer/aports/...` (195 `:error`s: the linker names musl's crt1.c) | `bin/lib/ci/record-ref`'s `run1` | fixed, off gate_key (`bin/lib/ci/` is out); the 195 lines rewritten |
  | `test/repr/c/annot-results.edn` | `/root/arbace/.tmp/repr-c/gcc-15.2.0/...` (47 reasons) | `bin/lib/repr-c/annotations`, after `arbace.repr.c.results` | fixed in the script, off gate_key (`arbace.repr.c.results` is embedded, so it is left alone); rewritten |
  | `test/repr/c/roundtrip-results.edn` | the same (50 reasons) | `bin/lib/repr-c/roundtrip`, after `arbace.repr.c.results` | as above |
  | `test/goi/check-aot-linux-amd64.edn` | `/root/tamago-go/test/...` (one reason) | `bin/goi check --record` | **after the tag**: `bin/goi` is in gate_key. Prepared as `test/crops/after-tag/csp5-goi-goroot.patch` (the writer through `sed` with `$goroot`, and the record's line); `git apply --check` passes |
  | `test/cpp/ref-gxx15.edn` | `/home/buildozer/...` (2 `:error`s) | `bin/lib/cppi/record-ref` | **on the C++ branch** (`mac/cppi-deferred`): the writer is not on main; its run1 takes the same `${aports}` rule as C's |

  After the rewrite, `bin/reap` still finds 16 of 16 cells and no problem, and `--judge` matches all 16. Of the records
  scored on main, only goi's AOT record still holds a host path (Go's maturity is 86.0 until the patch). The held
  branches' copies of the C and Java records change at their merge with main: take main's side, since the rules are
  the same.
- **The `:ocaml` finding (reported here, as asked).** On `mac/oracle-records-cpp2`, `test/gate/steps.edn` plans every
  OCaml step on `:ocaml`: `oi-essential`, `oi-check`, `oi-aot`, `oi-suite-aot`, `ocaml-read-essential`, `ocaml-roundtrip`
  and `ocaml-judges`. The branch's survey ("As built: OCaml") and its fragment `test/gate/crops/ocaml.edn` say these
  steps lose `:ocaml` and read the packs. But the fragment is a merge's draft ("On merge, take this file's side"), and
  the planner reads `steps.edn`. As planned on that branch, OCaml still needs ocamlc and ocdump at check time, so
  `independence.edn` scores OCaml 0 there (20% for the crop, not 60%). The fix is the merge of the fragment's hosts into
  `steps.edn`, on that branch's line.

## Decided (the user, 2026-11-07): a test's own verdict is a judge no toolchain made; a mixed stage the case-weighted mean

Two decisions of the user on 2026-11-07, relayed by the main session:

- A test's own verdict counts as a judge no toolchain made, and scores 1: DejaGnu's `dg-error`, `dg-warning`, `dg-bogus` and
  `dg-message` directives; an execute test's `exit(0)` or `abort()`; a suite's own expected output or status (tcc's `.expect`,
  mir's `.expectrc`, CompCert's `Results/`); a round trip's reread forms equal and its `:full` print byte for byte. Judge by it
  wherever a test has one; the toolchain's records move to the oracle tier, checked by hand at a tag. A test with no verdict of
  its own, whose only reference is the toolchain's recorded output, keeps 0.5.
- A mixed stage (cases partly judged by their own verdict, 1, partly by a recorded toolchain reference, 0.5) scores the
  case-weighted mean over its cases. A stage's fact carries `:cases {:self N :recorded M}` and `test/crops/score.ae` scores it

  ```
  (N + 0.5 M) / (N + M)  =  0.5 + 0.5 x N / (N + M)
  ```

  N and M count the crop's REAP cells' cases (each cell's cases as `test/reap/matrix.edn` counts them; a case whose pass rule
  asks a record counts in M). Both counts are printed beside the stage; the cross-check with the gate's steps applies as
  before (a named step on a toolchain host scores the stage 0).
- **C (branch `c-indep-b1`, from `mac/batch1`).** Read 5,483 cases, 72 by a record (the token check's known preprocessor
  differences whose evidence is gcc -E's digest); eval and AOT 2,268 each, 2,105 by their own verdict (`test/c/selfref.ae`),
  163 by gcc's runs (benchmarksgame, ci-run and cxgo carry no verdict; 25 exceptions with reasons); print 4,818, 35 by gcc's
  acceptance (files the reader rejects whose tree cc's does not give). N = 14,404, M = 433: judge 0.985. With read, eval, AOT
  and print at 1 (Arbace's own target, doc/C23-SURVEY.md, "As built: C's own target"), C's independence is 99.7% (20.0% on
  `mac/batch1`; 90.0% were the judge left on gcc's records).

- **C++ (branch `cpp-90`, from `mac/cpp-g5` with `mac/batch1` merged).** The run checks by each test's own verdict
  (`test/cpp/selfref.ae`: g++.dg's and libstdc++'s `dg-do run` tests by exit status, 205 of 225 reference cases), the round trip by
  its own levels 1 and 5 on the native parser (`bin/repr-cpp roundtrip --judge own`, no clang: the essential step on `[:stage0]`, the
  whole lists' own records the night's). The judge's cases: 1,934 at 0.5 (the token check's recorded digests), 4,597 at 1 (dg
  directives), 81 + 81 (65 + 65 at 1, 16 + 16 at 0.5), 3,366 at 0 until the night's own records (then 1): judge 0.568 today, 0.90 after.
  Two controls added (the token check's, the dg check's) and the round trip's rule pointed at GT9's control file: maturity 100. C++'s
  independence 20.0 to 31.4 today, 38.0 after the night; read, eval and AOT need doc/cpp/CPP-SURVEY.md's CP240-CP242.

## Left

- The facts at refs other than those in `independence.edn`'s `:at` are main's. `--newest` says so in the output, as
  "facts of the checkout (no :at for REF)". A new held branch that changes a pipeline needs its `:at` entry.
- The native front ends that exist but are not the cells' default (Haskell's native reader on `mac/haskell-hf7`, Rust's on
  `mac/rust-rf14`, C++'s codegen on `mac/llvm-ll6`) do not raise a score until a cell's check reads through them.

## Decided (the user, 2026-11-07): a mixed stage

A stage whose cases are judged partly by their own verdict (1: jtreg's status, a suite's own report, a golden file, the
original text) and partly by a recorded reference (0.5), or live by a host toolchain (0), scores the **case-weighted mean**
over its cases. Built in `crops.score` (`mixed-score`, test/crops/score.ae): where a fact of `test/crops/independence.edn`
gives the per-check case counts, the tool computes the mean and uses it in place of the fact's stated `:score`, printing
"the case-weighted mean of N cases" (and the stated score beside when it differs by more than 0.005):
`:cases {CHECK [N SCORE] ...}` (Scheme's judge: 5,624 cases, 0.752) or `:mixed {:own N :jdk-record M}` (Java's judge:
14,482 cases, (14,038 + 0.5 x 444) / 14,482 = 0.985). A fact without counts keeps its stated score. A claim refuted by the
gate's steps (a step of the stage on a toolchain host) scores 0 as before.

## Decided (the user, 2026-11-07): a pinned input corpus in the crop's own language (LLS27 (a))

A pinned input corpus written in the crop's own language counts **1** as a stage's input, as hand-written test data does,
when no output of a toolchain about it is a reference: the toolchain that made it does none of the crop's work (reading,
lowering, running and the verdicts are Arbace's), as gcc's torture C sources are to C. It is pinned (committed, or fetched by a
SHA-256 the checkout pins) and read as the language under test, not as a stand-in for a front end the crop lacks: OCaml's
Lambda, GHC's STG, C++'s and Rust's IR made by clang or rustc for those crops stay 0.5 (a toolchain's product consumed in
place of the crop's own front end). Where a toolchain's output about a case is its reference (lli's or opt's recorded run),
that is the judge stage's 0.5, not the input's. Inputs a toolchain made that are in no checkout and pinned by nothing (the
first LLVM records' 282 modules and their mutants, before LLS28 (a)'s pack) stay 0.5.

- **LLVM IR (batch 2, the merge of `llvm-90`).** The pinned IR packs `test/llvm/ir/{tx,c}.tar.xz` (clang 23's, made by hand by
  `bin/llvmi ir-make`) and llvm/test's files from the pinned tarball count 1; llvmi's ci reads Arbace's own C target
  (`c/target`, `bin/llvmi`'s `c_sysroot`, batch 2) in place of gcc's recorded `test/c/host`. Eval and AOT 1; read 0.706 (the
  2,276 cases of the 282-module lists and mutants, in no checkout, 0.5, until the night pack of LLS28 (a)); print 1; judge
  0.711 (opt's and lli's records, 0.5). LLVM IR's independence (0.706 + 1 + 1 + 1 + 0.711) / 5 = 88.3% (68.3% on `llvm-90`);
  94.2% once LLS28 (a)'s pack and regenerated mutants are the read stage's inputs.
