# The crops' scores: completeness, maturity, independence

`bin/crops-score` (and `make crops-score`) gives each of the eleven crops three percentages, measured from the recorded
results alone, as `bin/reap` judges the REAP matrix: no check is run and nothing is estimated. The user asked for it on
2026-11-06. The goal is every crop above 90% on all three. The logic is the namespace `crops.score`
(`test/crops/score.ae`, on the tools' host). Its data are `test/crops/crops.edn` (the crops: their gate steps, their held
branches, the rules that find their negative controls) and `test/crops/independence.edn` (the reviewed facts, stage by
stage).

```
bin/crops-score                       the checkout's records, every crop: a table, then each crop's evidence
bin/crops-score --ref REF             REF's records, read by git show (no checkout): every input, crops.edn and
                                      independence.edn too (queue 507; the checkout's only when REF has none), the
                                      checkout's :at for REF merged over them
bin/crops-score --newest              each crop on its newest held branch (crops.edn's :branches over refs/remotes/mac,
                                      the newest by committer date whose test/reap/cells.edn has the crop's row)
bin/crops-score --crop C              one crop (clojure java go c cpp ocaml scheme haskell rust llvm python)
bin/crops-score --tsv                 one line a crop: the three scores, each cell's counts, each item, each stage
bin/crops-score --input FILE          REAP cell records over the tree's (test/reap/matrix.edn's format, a vector or
                                      {:cells [...]}): the numbers of a crop reported apart, Python's from the Mac VM
bin/crops-score --judge               the checkout's matrix judged afresh by reap.main's own readers and judging; a
                                      difference from test/reap/matrix.edn is a problem (exit 1)
bin/crops-score --strict              a fact of independence.edn the tree's gate steps refute is a problem (exit 1)
bin/crops-score --self-test           the rulings' own tests (fabricated cases, a negative control each; also run by --judge)
bin/crops-score --self-test-ref       queue 507: on a clean tree --ref HEAD equals the checkout's scores row by row; its
                                      control (independence.edn from a working tree behind HEAD) must differ
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
  AOT invalid programs; since OC222 (a), 2026-11-10, OCaml's `:config-skipped`) is outside the matrix's counts already, and so outside
  both denominators; the details print a cell's n/a count beside its counts.

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
`test/oracle/frozen-jar.sha256`) with the JVM that runs it (Java 26 by `bin/lib/java26.bash`, the user's ruling of 2026-11-09).

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
- *Refinement (the user, 2026-11-20).* A case whose expected output is undefined, unspecified or host-dependent, or that is
  refused by design, leaves the judge's cases, as the n/a cases and Haskell's D435-3 (a) do; its run still counts in completeness.
  The fact's `:out-of-cases {:file F :cases N}` names the crop judge's list of them, `{ID {:kind K :levels [L ...] :why W}}`
  (C's `test/c/selfref-out.edn`, C++'s `test/cpp/selfref-out.edn`), and N the cases its stated `:cases` leave out. An entry
  without a kind (`:undefined :unspecified :constraint :implementation-defined :host :by-design`), its levels or a written reason,
  or levels that do not sum to N, refute the claim and hold the stage at 0.5: nothing leaves without a reason. The crop's own
  judge refuses an entry with a verdict of its own (a defined output cannot be counted out), with a control on every run.
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
| Clojure | main | the suite (`test/arbace-go-results.edn`, `-aot-`), the oracle, repr/clojure's rules and round trip | `test/clojure/controls.edn`, `test/oracle/controls.edn`; since `controls-cg` (2026-11-08) `test/repr/clojure/rules-controls.edn`, `roundtrip-controls.edn` | main's `:clojure` |
| Java | main | `bin/java-check` read, classes, diagnostics, interp, compiled, print (`test/java/check-*.edn`) | `test/java/controls.edn` (c00001, control-diag, control-print) | main's `:java` |
| Go | main | `bin/repr-go native-check`, `bin/goi check`, `--aot`, `bin/repr-go roundtrip` (4) | `test/goi/controls.txt` (interp, aot); since GTS88 (b) `test/repr/go/native-controls.edn`, `roundtrip-controls.txt`, since `controls-cg` (2026-11-08) with the level `:known` and the kind `dirtest` | main's `:go` |
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

## Decided (the user, 2026-11-09): the independence audit's corrections

The independence audit of 2026-11-09 (`.tmp/indep-audit/` in the main checkout: `independence.edn` corrected with
`indep-audit` comments, its diff, crops-score before and after) found facts that overstate. The user's rulings:

- **Java's platform is a toolchain's product.** javac on Arbace attributes against the platform, the class files of jdk26u's
  build tree pinned by `jdk/javac/platform.sha256`; the interpreter, j2g and the printer take the forms made against it. Java's
  read, eval, AOT and print score **0.5** (the header's rule, as Haskell's front end over GHC's interfaces) until the platform is
  Arbace's own (its modules compiled from jdk26u's sources by javac on Arbace).
- **The JVM running the frozen jar is exempt, like the jar** (`independence.edn`'s header). The JDK beyond that use counts.
- **The audit's other corrections, applied on next.** OCaml's read, eval, print and judge: every record the cells read was made on
  ocdump's path (`test/ocaml/read-results.edn` and `print-results.edn` of 1000db42c, "the oracle's parser, tools/ocdump",
  `check-oi.edn`, `check-suite-oi.edn`), so read, eval and print 0.5 and the judge the case-weighted 0.563 until the night's
  native `--record-results` and OC185's switch of the cells (then 1); OCaml 81.4 to 51.3. Haskell's 87 eval and 2 AOT programs with
  C read the host's installed headers (`bin/hsi cnodes`, `test/haskell/eval/cnodes.ae` over `test/c/host`, no sysroot): 0, and
  its judge counts the cells' interface level (recorded, 0.5) and layout level (own): Haskell 78.9 to 74.9. C++'s recorded
  samples and runtime are 15, not 14. Python's facts were older than its scripts: read 1 (the native reader the default),
  eval 0 (`bin/pyi check`'s driver is python3), AOT 1 (`check --aot` runs no python3; not recorded yet), print 1, judge 0:
  Python 20.0 to 60.0. Java's judge: t08351, t08362 and t08376, added to `ref-jdk26.edn` after JCV1, had no line in
  `test/java/verdicts.edn`; `bin/java-check verdicts` wrote it again (the three `:status`): 0.985.
- **Haskell's C headers** (the user's item (4)). `bin/hsi` sources `bin/lib/repr-c/sysroot.bash` as `bin/llvmi` and `bin/cppi` do:
  `bin/hsi cnodes` assembles C's sysroot under its musl lock and exports `ARBACE_C_HOST` (Arbace's `c/target`) and
  `ARBACE_C_SYSROOT`, and `test/haskell/eval/cnodes.ae` falls back on `c/target`, never `test/c/host`. The programs' C reads
  Arbace's own C headers; 6 programs with C checked by `--only` (T2594, T4038, T5402, ffi001, ffi003, ffi022: 6 of 6) and
  hsi-essential's command (123 programs, the controls failing as they must). The 89 programs' C counts 0.5 again, as the STG pack it
  runs over: Haskell 74.9 to 76.0 (its judge stays the audit's, with the interface and layout levels).
- **The cross-check's blind spots closed** (`crops.score`, `test/crops/crops.edn`). Beside a step on a toolchain host, on the
  checkout: (a) a claim above 0 is refuted when a step of the stage runs a script that reads a toolchain's files by path
  (`:toolchain-reads`: a script or namespace of bin/ or test/ the step's command reaches by the root, to depth 4, naming
  `test/c/host` with no C sysroot in it or the script that named it; libstdc++'s or gcc's installed include directory with no
  C++ sysroot); (b) a claim above 0.5 is refuted when a sentence of a cell's record's header names a helper's path (`tools/ocdump`,
  `tools/hsdump`, ...: `:helper-records`) other than as a comparison or a pin; (c) a claim above 0 that no step of the tree runs
  (no `:steps`, or none outside the oracle tier) is printed "unverified" (its score kept, not a failure: Python's read, AOT and
  print until its fragment's steps are planned). The hosts `:jdk` and `:frozen` are toolchain hosts now, with the exempt uses as
  data (`:host-uses`): `:frozen` (the jar), `:jdk` where it is `bin/lib/java26.bash`'s `java -version` alone (suite-go,
  suite-aot-essential, suite-aot, gate-tests) or the JVM of a step that runs the frozen jar (but javac's, repr/java's and Java's
  checks). Each refutes what it should: with `bin/hsi` and `cnodes.ae` as they were, (a) refutes Haskell's five stages (the
  closure follows a script's every verb, so the read and print stages too: an over-approximation, harmless once the file is
  fixed); with OCaml's read claimed 1, (b) refutes it on `read-results.edn`'s "the oracle's parser, tools/ocdump"; a Java stage
  naming javac-classes is refuted on `:jdk`. On the corrected facts none is refuted. Facts that had no `:steps` and whose steps
  exist were given them (Clojure's read and print, Go's read and print, C's and C++'s print, Scheme's read, Rust's AOT).

## Decided (the user, 2026-11-10, about 10:30 UTC): RFP53 (a) for Rust, OC222 (a) for OCaml

Two scoring rulings of the user, relayed by the main session, built on branch `crops-rulings` (from `mac/next` dc6a55df2).

- **RFP53 (a), Rust's own cases** (`.tmp/plans/RUST-90-PLAN.md` §3). A case of Rust's eval or AOT cell scores **1** when all
  seven of its inputs are Arbace's own, else **0.5** (RFP43 (a) stays for a partly own case): C1 the program's bodies from our
  front end, C2 std's instances' bodies from source, C3 no mono fact refused or taken from the record, C4 std's interface and
  the StableCrateIds native, C5 a native allocator shim, C6 the library lowered by Arbace, C7 ci on `c/target` (not
  `test/c/host`). Eval and AOT are the case-weighted mean over the cell's cases, the mixed-stage rule of 2026-11-07, counted from
  the records. Built: `test/crops/crops.edn`'s `:own-inputs :rust` names C1-C7; a stage fact's `:case-inputs {:file F}` names the
  record of the cell's cases' inputs, `[ID {:c1 VALUE ... :c7 VALUE}]` a case, `:own` an own input (`test/rust/eval-inputs-linux-amd64.edn`,
  written by hand from the run as bin/rsi makes it until RUST-90's S6 makes `bin/rsi check --record` write it from `front.tsv`;
  `test/rust/aot-inputs-linux-amd64.edn` once the AOT cell is recorded). `crops.score`'s `rfp53-counts` counts the cell's cases
  (the ids of its sources' records) whose every input is `:own` (N); every other case of the cell, a row with one input not own or
  no row at all, counts 0.5 (M = the matrix's cases less N), and the stage is (N + 0.5 M) / (N + M). With the cell not recorded
  or F absent the stated score stands, said in the check. Today 0 of 5 eval cases are own (each runs on the IR pack, std's IR,
  rustc's shim and `test/c/host`): eval 0.5, AOT 0.5 (no record), Rust's independence **80.0, unchanged**. It moves as cases
  become own: with one case's seven inputs marked own on the checkout (a trial, not committed) eval was 0.6 and independence 82.0,
  and the second case, own but for C7, stayed 0.5. The self-test's fabricated cell: 37 of 73 cases own in eval and AOT, the judge
  (66,692 + 0.5 x 146) / 66,838, gives **90.1**, as the plan computes; its control, the same 37 with C7 `test/c/host`, counts 0
  own and gives 80.0.
- **OC222 (a), OCaml's configuration skips** (`.tmp/plans/OCAML-90-PLAN.md` §1, §5). The 33 tests ocamltest itself reports skipped
  on the pinned configuration (OCaml 5.5.0, linux/amd64, `ocamlc -config`: flambda false, tsan false, with_frame_pointers false)
  are **not applicable**, `:na`, outside completeness's denominators, as Go's AOT invalid programs; the other tools' tests
  (the debugger 5, gdb and lldb on amd64 2, func_sections, execvpe's skip) stay by design in `:tools-and-targets`. Built: the
  class `:config-skipped` (`:kind :na`), first in `test/ocaml/divergences.edn` and `divergences-aot.edn`, its `:skips` naming each
  program's skipping action (tsan 10, frame_pointers 8, flambda 3, target-windows 6 (win-env and win-stat moved from
  `:c-runtime-internals`), macos 2, arch_power 1, arch_arm64 2, arch_riscv 1), each checked in the 5.5.0 testsuite's headers;
  `test/ocaml/ocamltest-config.edn` records the configuration (`ocamlc -config` of the host's 5.5.0, 2026-11-10) and ocamltest's
  actions (`builtin_actions.ml`, `ocaml_actions.ml`: each passes when a configuration key holds a value). `crops.edn`'s `:na-rules
  :ocaml` makes `crops.score` check every `:na` class of OCaml's cells' classes (`na-class-problems`): `:skips` covering `:programs`,
  each action known and failing on the recorded configuration, `:match` taking only `class` rows (a program a run judged is never
  `:na`); any breach is a problem (exit 1). OCaml's eval cell 477 / 162 / 4 (33 n/a), AOT 419 / 158 / 66 (33 n/a): completeness
  **82.5 → 84.2** (+1.7, the plan's figure), without by design 96.4 unchanged; `bin/oi divergences` (both levels, the suite's too)
  class every non-pass.
- **Negative controls.** `bin/crops-score --self-test` (also run by every `--judge`, so by the gate's `crops-score` step) holds both
  rulings' numbers and controls that must fail: Rust's C7 control above; OCaml's an amd64 program classed `:na` by `arch_amd64` on
  amd64, an action not recorded, a program with no action, a `:match` that takes a run's failure, a class without `:skips`, no
  configuration. Each was seen to fail; with the rules broken (an input counted own whatever its value, an action never passing)
  the self-test fails. On the records: `native-debugger/linux-gdb-arm64` given `arch_amd64` makes `bin/crops-score` exit 1
  ("passes on the pinned configuration (architecture amd64): ocamltest runs it").
## Decided (the user, 2026-11-10): AX10 forward, CSP7 reversed

The roadmap's CSP7 (`.tmp/plans/CROPS-90-ROADMAP.md` in the main checkout) had kept the assembly crops out of `crops.edn` until
AX10, so that no crop under 90 would join with read and print alone. The user reversed it on 2026-11-10: AX10 now, with the gate
steps AXS21 (a), AXS42 (a), AXS65 (a), AXS73 (a) and AXS87 (a) had left to it. Two crops join (doc/asm/AMD64-SURVEY.md, "As built:
AX10"): `:asm-amd64` (x86-64 assembly) with all four cells, and `:asm-arm64` (arm64 assembly) with read and print checked and eval
and AOT "no check yet" until a64i (AR5) and arm64's ELF and runs (AR4), as Python's cells waited for their records. Their REAP rows
read bin/repr-asm's records through three readers of `test/reap/main.ae` (`:asm-rows`, `:asm-counts`, `:asm-arm64`), each non-pass in
the class its own judge found. Independence: no stage runs a host assembler, linker or TableGen at check time; the judge is the
tests' own CHECK lines, the programs' verdicts and the CPU's runs, but for the object check's 86 files against llvm-mc 23.1.5's
recorded objects (0.5; x86-64's judge 0.9998); llvm-mc, llvm-tblgen and ld.lld run only in the oracle tier (host `:llvm-mc`, a
toolchain host). arm64's eval and AOT stages do not exist yet and score 0.

- `crops.score`'s record reader took a crop id of letters alone (`:([a-z]+)`), so a hyphenated id (`:asm-amd64`) read as "not
  recorded" after `--record`; it takes `[a-z][a-z0-9-]*` now.
- The scores at the branch `asm-ax10` (2026-11-10): x86-64 assembly completeness 97.9% (98.5% without by design), maturity 100.0%,
  independence 100.0%; arm64 assembly 49.9% (49.9%), 91.7%, 60.0%.

## Done (2026-11-20, branch `judge-rest-1120`): the judges' last recorded cases of Java, OCaml and arm64 assembly

The user's goal of 2026-11-20: complete the crops' independence. Three crops sat just under 100 for a few recorded cases in their
judge stage (0.5 each); each was read again and made own wherever an accepted precedent applies (reviewed golden files, JCS361 (a)
and its extension to C and C++; the test's own stated expectations), the rest left on the record with its reason.

| crop | recorded before | made own | left, why | judge | independence |
|---|---|---|---|---|---|
| Java | 24 `:stdout` tests (48 cases) | 3 by reviewed golden files: t03912 (1 MB, one range row), t05050 and t05278 (test.src masked as `@TESTSRC`) | 21: output not fixed by the source (threads, identity hashes, DSA's DER lengths, heap size, method order), the host's environment, HotSpot's listings, fuzz-generated programs (`test/java/golden/left.tsv`) | 0.9983 -> 0.9986 | 0.9996 -> 0.9998 |
| OCaml | 27 programs-levels (16 at oi's level, 11 at the AOT level, at today's corpora) | the opam set's 11 demonstrations by reviewed golden files (`test/ocaml/opam/golden.edn`), at both levels | 5 at oi's level, by design: the native back end's allocations (3), ocamlopt's debug frames (1), a program ocamlc does not compile (1) | 0.9982 -> 0.9997 | 0.9996 -> 1.0000 |
| arm64 asm | the object check's 126 files | 95 MC/AArch64 object tests by their own words (object runs' CHECK lines, two-step runs, `--sd`, and 1,626 stated encodings of 43 files) | 31: musl's 13 sources and the 3 synthetic tests state nothing; 15 tests state only the decoder's text, unbuilt views, numeric variables, rtdyld or nothing FileCheck reads (`test/asm/object-recorded-arm64.tsv`) | 0.9973 -> 0.9993 | 0.9994 -> 0.9998 |

The recorded scores (`test/crops/scores.edn`, the judge rounded to 3 decimals by `crops.score`): Java 0.9996 -> 0.9998, OCaml
0.9996 -> 1.0000, arm64 assembly 0.9994 -> 0.9998 (floors: 99.9 -> 99.9, 99.9 -> 100.0, 99.9 -> 99.9). arm64's completeness 99.87 ->
99.66: the two-step runs show 28 stated lines whose views are not built (deferred), beside 33 now met. The byte-for-byte comparison of
every arm64 object with llvm-mc's record is the oracle tier's `asm-arm64-object-oracle`; the JDK's and ocamlc's records stay the
oracle tier's as before.

### The second round (2026-11-20, later the same day; the user's decision: a case whose expected output nothing defines leaves the judge's cases)

The user decided the same day that a judge case whose expected output is undefined, unspecified or host-dependent leaves the
judge's cases (as the n/a cases and D435-3 (a)), its run still counted in completeness, its reason written per case (`:out-of-cases
{:file F :cases N}`, `crops.score`'s check from c-cpp-golden-1120, taken here unchanged). Java: 16 of its 21 `:stdout` tests out
(`test/java/golden/out.edn`, 32 cases); the 5 fuzz-generated stay on the JDK's record (DECISION: their output is fixed by their source,
which states none of it, and no evaluator independent of the JDK and of Arbace's Java exists). arm64: the disassembly's texts
encoded by Arbace, the views -D, -Ss and the call frames, FileCheck's numeric variables, the large EH encoding, and the object round
trip (a64i's decoder against the source's forms) for the files that state nothing: 125 of the object check's 126 files own,
elf-objdump (an empty source) out (`test/asm/object-out-arm64.edn`).

| crop | judge | independence (recorded) | floor |
|---|---|---|---|
| Java | 0.9986 -> 0.99965 ((14,452 + 0.5 x 10) / 14,462; 32 out) | 0.9998 -> 1.0000 | 99.9 -> 100.0 |
| arm64 asm | 0.9993 -> 1.0 (23,059 own, 1 out) | 0.9998 -> 1.0000 | 99.9 -> 100.0 |

arm64's completeness 99.66 -> 99.97 (the 28 deferred lines of the first round met, 1 left: error-location-ldr-pseudo's literal pool).

## Decided (the user, 2026-11-22): the burst's counting questions

The user's decisions of 2026-11-22 (doc/JOURNAL.md, "the user's decisions on the burst's by-design and counting questions") count
some non-passes n/a (outside both denominators, reap's `:kind :na`), some as passes by a verdict another record holds, and some out
of a judge's cases (the score's `:out-of-cases`). Each is a rule over the records, keyed on a verdict a record holds, never a copied
list of cases; each row keeps its reason. Built (branch `counting-1122`):

- **Python, n/a.** `test/python/divergences.edn`: `:cpython-only` (617) `:kind :na`; four classes before `:skipped-by-test`, each
  taking its rows from it: `:skipped-by-both` (1,917; `:ref-outcomes [:skipped]`: the method skips itself on pyi and CPython 3.14.10's
  recorded run skipped it too; bin/reap's source option `:py-ref-outcomes` puts the reference's outcome beside each eval non-pass and
  `classify` keys a class's `:ref-outcomes` on it), `:udp-lite` (113, the host's kernel), `:cpython-implementation-details`
  (test_opcache's 73 and test_itertools' tuple reuse 6), `:remote-debugging-disabled` (test_remote_pdb's 45; its 46th method CPython
  skipped too, `:skipped-by-both`'s). `:skipped-by-test` keeps 376 (CPython ran 235 of them, 96 have no reference, 45 an excluded one).
  bin/pyi's own judge (test/python/libtest_judge.ae) reads no outcome, so it leaves a `:ref-outcomes` class to bin/reap.
- **Python's four `:rejected` files.** Read: bin/reap's `:own-verdict {:file F :class :rejected}` (test/reap/main.ae's
  `own-verdicts`): each file's recorded error (test/python/roundtrip-known.edn's `:error`, `:message`) held to every expectation its
  own tests state (test/python/rejected-own.edn: the test, its lines in the pinned tarball, the class and the message as the test
  compares it), as parse-check judges its error cases; the position a test states (badsyntax_3131's line 2, offset 1) is the read
  cell's count-once group's, whose parse check passes the file only with the native error's position the reference parser's. Print:
  `test/python/divergences-print.edn`'s `:rejected` `:kind :na`.
- **C.** CR1 (a): `test/c/divergences-read.edn`'s fourteen classes where cc, the reference, is wrong are marked `:reference-wrong`;
  the token and tree checks' sources' `:own-elsewhere {:records [...] :sep " "}` (`own-elsewhere-pick`): a file known only in such
  classes passes when the dg check's record (its DejaGnu directives) or the eval check's (its run) passes it, each test by its own
  verdict; `count-once` counts such a file a pass when no check of the group leaves it a non-pass (the tree check's "pp-known" rows
  follow the token check's verdict). 132 pass (131 by the dg check, 1 by its run); the 5 without a verdict stay by design
  (c23-attr-syntax-6, c23-complit-5 and embed-10, whose dg verdicts fail; sqlite3.c and tcc-pp's 13.S, with none). The five speed
  benchmarks at eval: `test/c/divergences.edn`'s `:speed-benchmarks` (compcert's aes, almabench, binarytrees, chomp, fannkuch),
  `:kind :na`, before `:speed` (4 stay); the judge's stated cases 14,952 own to 14,947.
- **Go.** `test/goi/divergences.edn`'s `:foreign-code` (8) and `:memory` (6) `:kind :na`. (The read cell's 137 are another stream's.)
- **x86-64.** `test/asm/divergences-cross.edn`'s six classes of llvm-mc's own Intel text (`:ambiguous` 537, `:pcrel-imm` 118,
  `:mask-operand` 33, `:other-instruction` 21, `:disassemble-only` 12, `:dollar-name` 2: 723) `:kind :na`, out of the cases (AXS34's
  question (b)); the judge's stated cases less the same 723: (221,193 + 0.5 x 86) / 221,279.
- **Haskell.** T13825-unit and unboxedsums_unit_tests (`:by-design`: they import GHC's compiler as a library) and tcrun022
  (`:implementation-defined`: an -O test whose RULES must fire) join test/haskell/eval-out.edn: 34 out of eval's judged cases, 910
  of 916 own.
- **Negative controls.** `bin/crops-score --self-test` (`counting-1122-tests`, run by every `--judge`) calls reap.main's own
  `classify`, `py-ref-annotate`, `own-verdict-pass?`, `own-elsewhere-pick`, `kind-of` and `sum` on fabricated cases and holds each
  crop's numbers: a method CPython ran (and one with no reference) not taken by `:ref-outcomes`; another message, another class, a
  word the message lacks, one of two expectations unmet, no expectation: not a pass; a dg verdict that fails, a class not marked, a
  file with one deferred entry: not picked; the 723 by design and the 34 listed against 31 stated: refused. With each rule broken by
  hand (classify ignoring `:ref-outcomes`, the own verdict ignoring `:exact`, CR1 ignoring `:reference-wrong` or taking any verdict)
  the self-test failed.

| crop | cells before | cells after | completeness (floored) | independence (floored) |
|---|---|---|---|---|
| Python | read 3,702 / 34 / 0, eval 39,204 / 5,490 / 3,627, print 2,796 / 4 / 0 | read 3,706 / 30 / 0, eval 39,204 / 2,719 / 3,627 (2,771 n/a), print 2,796 / 0 / 0 (4 n/a) | 0.9485 -> 0.9615 | 1.0 |
| C | read 5,328 / 137 / 10, eval 2,324 / 35 / 0 | read 5,460 / 5 / 10, eval 2,324 / 30 / 0 (5 n/a) | 0.9861 -> 0.9926 | 0.99997 |
| Go | eval 1,781 / 52 / 1 (4 n/a) | eval 1,781 / 38 / 1 (18 n/a) | 0.9889 -> 0.9908 | 1.0 |
| x86-64 | print 195,651 / 861 / 2 | print 195,651 / 138 / 2 (723 n/a) | 0.9989 -> 0.9998 | 0.99996 |
| Haskell | eval stage (910 + 0.5 x 9) / 919 | (910 + 0.5 x 6) / 916 | 0.9832 | 0.9990 -> 0.9993 |

## Decided (the user, 2026-11-22): the second questions

The user's decisions of 2026-11-22 on the burst's second questions (doc/JOURNAL.md, "the user's decisions on the burst's second
questions") count eight more classes n/a, as the section above counts its own: the class's `:kind :na`, its rows and their reasons
kept, keyed on the verdict the record holds (a row a class names counts n/a only when the record does not pass it), never a copied
list. Built (branch `counting2-1122`):

- **C++, read.** `test/cpp/divergences-dg.edn`'s `:gcc-dialect` (173: OpenMP, OpenACC, GNU TM, GCC's contracts, -fpermissive,
  -fms-extensions; CP399 (a)), `:gxx-only-rules` (2, CP328 (a)) and `:musl-libc` (1, Wconversion-null-4: musl's NULL is nullptr)
  `:kind :na`. `:musl-libc` names a second file, Wnull-conversion-1.C, which the own front end passes: a pass in reap (its
  `classify` sees only the non-passes) and in the read stage's count (below). Left by design: `:gcc-own-rules` (135), which the
  user's decision builds (the GNU extension rules ported into the own front end).
- **Python, eval.** `test/python/divergences.edn`'s `:fork` (557), `:testinternalcapi` (132) and `:venv-and-build` (49) `:kind :na`.
  bin/pyi's own judge (test/python/libtest_judge.ae) classes by `:tests` and `:match` alone (the kind is only printed): `bin/pyi
  divergences` over the record gives the same classes and counts before and after, the kind's word aside. Left by design:
  `:extension-modules` (75), which the decision builds (C extension modules through ci's C world).
- **Haskell, eval.** `test/haskell/divergences.edn`'s `:process` (T24672, forkProcess) and `:skipped` (T24809, T4262, all.T's own
  skips) `:kind :na`. Left by design: `:no-reference` (31), which the decision builds (the packages vendored, GHC's runs recorded by
  hand in the oracle tier).
- **The stages' cases follow the cells'.** An n/a case is none of the cell's cases, so none of a stage's counted cases:
  - `:count-from`'s `:out` (497-3 (a), C++'s read) takes `na-cases`: an `:na` class's id leaves the counted rows unless the record
    passes it (Wnull-conversion-1.C stays, an own pass): 1,699 of 4,382 rows own (182 n/a), the stage (1934 + 1699 + 0.5 x 2683) /
    6316 = 0.788 (0.780 before). One row differs from the cell still, as before this round: undefined-expectation's
    pr105475-2.C, which the cell passes by clang's record (outside the syntax-only list) and the own record fails.
  - `:case-inputs` (RFP53 (a), HF10-P29 (a)) leaves out of the cell's ids the ids its `:na` classes name in `:tests` or `:ids` (hsi's
    classes) whose row does not pass (`cell-rows`, `cell-na-ids`, `na-cases`), as the matrix's total leaves them out; before, an
    `:out-of-cases` id that is n/a was taken out twice (Haskell's eval counted 913 of 913). T24809 and T4262 left
    test/haskell/eval-out.edn (34 -> 32 out, the fact's `:cases` too): the eval stage 915 of 915 own. No other crop's stage moves
    (Java, C++ and Rust name no `:na` class's id among their cases).
  - C++'s judge: its stated `:dg` cases the cell's, 4,383 (3,731 pass and 866 not, less 214 n/a; the 4,564 stated before had not
    taken 497-3 (a)'s and 502-2 (a)'s 5 either): (8430 + 1929 + 0.5 x 5) / 10364, 1.0 at three decimals as before.
- **Negative controls.** `bin/crops-score --self-test`'s `counting-1122b-tests` (run by every `--judge`): `na-ids` takes an `:na`
  class's `:tests` and `:ids`, not a `:design` class's; `na-cases` leaves a passing row and an unnamed one; C++'s count keeps the
  passing row (the control, every named row out, differs); Haskell's eval counts 915 with or without the two ids on the out list
  (the control, the n/a ids kept with 34 out, gives 913); the three crops' completeness before and after, and C++'s with the 176 by
  design (must differ). With `na-cases` taking every named row, or `na-ids` reading `:tests` alone, the self-test failed.

| crop | cell before | cell after | completeness exact (floored) | independence |
|---|---|---|---|---|
| C++ | read 5,665 / 311 / 517 (38 n/a) | read 5,665 / 135 / 517 (214 n/a) | 0.961380 -> 0.967457 (96.1 -> 96.7) | 0.953940 -> 0.955500 (95.3 -> 95.5; the read stage 0.7798 -> 0.7876) |
| Python | eval 39,204 / 2,719 / 3,627 (2,771 n/a) | eval 39,204 / 1,981 / 3,627 (3,509 n/a) | 0.961507 -> 0.965050 (96.1 -> 96.5) | 1.0 |
| Haskell | eval 894 / 34 / 22 | eval 894 / 31 / 22 (3 n/a) | 0.983464 -> 0.984209 (98.3 -> 98.4) | 1.0 (eval 916 of 916 -> 915 of 915) |

## Decided (the user, 2026-11-22): the third questions

The user's decisions of 2026-11-22 on py-capi's and c-tails' questions (doc/JOURNAL.md, "the user's decisions on py-capi's and
c-tails' questions"), built as the two sections above (branch `counting3-1122`), each keyed on a verdict the record holds:

- **Python, eval** (`test/python/divergences.edn`, the record `test/python/check-lib-test-linux-amd64.edn`):
  - `:testinternalcapi-absent` (new, `:kind :na`, before `:testcapi`; 762): a `skipped` or `import` row whose message names
    `_testinternalcapi` ("requires _testinternalcapi", "need the _testinternalcapi module", "_testinternalcapi.hamt() not available",
    "No module named '_testinternalcapi'", also as a SkipTest). 757 of them were `:testcapi`'s. A row `:capi-port` takes first (50
    whose message names it) and `:testinternalcapi` (132) stay there.
  - `:cpython-only` (`:kind :na`) gains the match `^skipped the module 'xxsubtype' is internal$`: test_descr's 4 xxsubtype rows
    (`support.impl_detail`, CPython-only; they were `:extension-modules`').
  - `:capi-refcounts` (3) `:kind :na`; `:capi-singletons` (new, 2: test_bytes' `b'' is not b''`) and `:capi-gc-tracked` (new, 2:
    test_tuple's `gc.is_tracked`) `:kind :na`, matched on the failure messages, their ids removed from `:capi-port`'s and
    `:gc-collector`'s `:tests`. The other C API rows stay deferred: the "about 40" are not all identifiable from the record's messages
    (listed in the journal).
  - bin/pyi's own judge classes by `:tests` and `:match` alone (`bin/pyi divergences` before and after: the same 31 "takes it first"
    problems as at HEAD, now naming the new class for testcapi-behaviour's 21 `:tests`; no other row changes class).
- **C, read (CT1)**: `test/reap/cells.edn`'s token and tree sources' `:own-elsewhere` gains a record `{:file
  "test/repr/c/pp-check-results.edn" :reason-re "pp ref"}` (`own-elsewhere-record`): the token check's own `:known` row whose reason
  says `pp ref` (judged by the own reference, `REPR_C_PP_JUDGE=own`, tools/ppref's libcpp stream equal to the port's) is the check's
  own verdict, so a file known only in `:reference-wrong` classes passes (sqlite3.c, tcc-pp's 13.S), the tree check's pp-known rows
  following (`count-once`). **The record holds no `pp ref` row yet** (its rows say `pp gcc`; recording needs `bin/repr-c check --tokens
  --only` on those two files under `REPR_C_PP_JUDGE=own`, a run), so the C read cell is unchanged: 5,460 / 5 / 10, to 5,462 / 3 / 10
  once recorded.
- **Negative controls**: `crops-score --self-test`'s `counting-1122c-tests`: a skip or import naming `_testinternalcapi` is n/a; a
  failure naming it, a skip naming only `_testcapi`, another xxsubtype skip, another bytes comparison and another tuple are not; the
  record's `pp ref` rows pass; a `pp gcc` row, a failing row, a class where the reference is not wrong and a record read without
  `:reason-re` do not.

| crop | cell before | cell after | completeness exact (floored) |
|---|---|---|---|
| Python | eval 39,204 / 1,981 / 3,627 (3,509 n/a) | eval 39,204 / 1,972 / 2,863 (4,282 n/a) | 0.967058 -> 0.970897 (96.7 -> 97.0) |
| C | read 5,460 / 5 / 10 | unchanged until the ppref rows are recorded | unchanged |
