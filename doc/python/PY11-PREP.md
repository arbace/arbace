# PY11, the milestone: its preparation (queue 201, PY10/PY11, 2026-11-04, the Mac VM)

What Python's milestone needs from the main host, prepared on the VM and not run: the row (PY10, built), the gate's steps
as GT9 proposes them for every crop (doc/GATE-SURVEY.md on the branch `gate-crops`, "GT9: the crops' gate", GTS210-GTS223,
decided (a) by the user 2026-11-04), what the clean run takes from Python, the embedding decision PYS18 measured on a
sample, and the budget per tier. The clean run and the tag are the main host's. Every measure here is the VM's (aarch64,
8 CPUs, load 2 to 8 from the other agents of queue 201) unless it says otherwise; GTS222 (a) re-measures them on amd64.

## 1. The row (PY10, built on `mac/201-python-py10`)

`bin/reap`'s fifth row, `test/reap/cells.edn`'s `:python` (doc/python/PYTHON-SURVEY.md, "As built: PY10"):

| cell | sources (the check's record) | classes | on this branch |
|---|---|---|---|
| read | `bin/repr-python roundtrip` (`roundtrip-known.edn`, levels forms, ast, code); `lex-check` (`lex-check-known.edn`, PY9); `parse-check --full` (`parse-check-known.edn`, PY9); each file counted once (`:count-once`), the mutants not counted | `test/python/divergences-read.edn` | 0+ / 34 / 0 (PY9's two sources `:until` its merge) |
| eval | `bin/pyi check` (`check-lib-test-linux-amd64.edn`, PY7); pyi on goi over the synthetic corpus and the samples (`check-aot-linux-amd64.edn`'s pyi runs); beside: `scope-check`, `compile-check` | `test/python/divergences.edn` (PY7's); `divergences-synth.edn` | no check yet (both records the main host's) |
| AOT | `bin/pyi check --aot` (`check-aot-linux-amd64.edn`'s compiled runs; an `:as-pyi` run classed by the eval classes of the same program) | `divergences-aot.edn`, `divergences-synth.edn` | no check yet |
| print | `bin/repr-python roundtrip --text` (`full-known.edn`, levels bytes and marshal) | `divergences-print.edn` | 0+ / 5 / 0 |

With the VM's sample record of `check --aot --no-pyi` in place (a scratch overlay, never committed) the AOT cell reads
138 / 0 / 13 and the synthetic eval source 138 / 0 / 13 with the problem "recorded with --no-pyi: no run of pyi's to
judge" (as it must: the record of the eval level needs pyi's runs).

**Passes.** A known file names only the files that do not pass, so the read and print cells count no passes ("0+") until
each check writes a results file of every file's row (GTS219 (a), CS240's format `["CORPUS FILE" :pass|:known "REASON"]`):
`test/python/{roundtrip,full,lex-check,parse-check}-results.edn`, already named in `cells.edn` (`:results`), REQUEST R1
below. The c-known reader then holds the results to the known file (a row neither pass nor known, a known file that
passes, a known file absent from the results: problems).

## 2. The proposed steps (not applied to `test/gate/steps.edn`)

GT9.4's Python block, revised by what PY7, PY8, PY9 and PY10 built. Applied by the main host after GT9's planner
changes (`:host-ids`, the crops' pins in `bin/gate --pins`, `:crops :python`, `costs.ae`'s known set). Only one line is
applied on this branch: the existing `reap` step's `:inputs` gain `"test/python/*.edn"` and its `:summary` the row
(bin/reap reads the Python records; `bin/gate --plan` on the branch keys it over 741 files).

```clojure
;; ---- groups
:python ["bin/repr-python" "arbace/repr/python.ae" "arbace/repr/python/" "tools/pydump/" "test/python/"]
:pyi ["bin/pyi" "arbace/python.ae" "arbace/python/" "go/arbace/py.ae" "go/arbace/py/" "go/arbace/libm.ae" "go/arbace/libm/"
      "tools/pyi/" :goi]

;; ---- the host's identity (GTS210 (a), :host-ids)
:host-ids {:python {:version ["python3" "-I" "-c" "import sys; print(sys.version)"]
                    :files ["/usr/bin/python3" "/usr/lib/libpython3.14.so.1.0"]   ; hashed through readlink -f
                    :pin "tools/pydump/PYTHON_VERSION"}}                         ; its apk checksum per architecture

;; ---- the pins (GTS211 (a)): bin/gate --pins runs `bin/repr-python pins --check` (20 s on the VM: the oracle's apk, the
;; tarball's SHA-256 and SHA-512, pyperformance's, the 20 apks, every corpus's measures), its line in the clean record's
;; map as :python {:pins-check true :oracle "3.14.10 python3-3.14.10-r1"}; the copies in the main checkout's
;; .tmp/repr-python/src (GTS212 (a), REPR_PYTHON_SRC for worktrees and the clean clone)

;; ---- the essential tier: one chain, 40 s warm on the VM (GTS215 (a): <= 2 min, <= 4 threads, <= 3 GiB, no lock)
{:name pyi-aot-runtime :phase :build :kind :build :mem-gib 3          ; cold: 6.6 min (the runtime printed, the stdlib modules compiled)
 :cmd "bin/pyi check --aot --no-pyi --only samples/functions.py"      ; warms .tmp/repr-python/pyi/aot (REQUEST R4: a shared cache)
 :hosts [:stage0 :go] :inputs [:lib-tools :tools :pyi]}
{:name python-read-essential :mem-gib 2 :phase :essential :chain python-essential
 :cmd "bin/repr-python spec-check && bin/repr-python samples && bin/repr-python roundtrip samples && bin/repr-python roundtrip --text samples && bin/pyi gen --check && bin/pyi scope-check samples synthetic errors && bin/pyi compile-check --full samples synthetic errors"
 :hosts [:stage0 :python] :never-loads [arbace.j2g] :pins ["test/python/corpora.edn" "tools/pydump/PYTHON_VERSION"]
 :inputs [:lib-tools :tools :python :pyi] :summary ["files, 0 failing|equal|differ|FAIL|known" 8]}       ; 13 s
{:name pyi-aot-essential :mem-gib 2 :phase :essential :chain python-essential :needs [python-read-essential pyi-aot-runtime]
 :cmd "bin/pyi check --aot --no-pyi --only-list test/python/essential-aot.txt"                             ; REQUEST R2 (on this branch)
 :hosts [:stage0 :go :python] :never-loads [arbace.j2g] :pins ["test/python/corpora.edn"] :inputs [:lib-tools :tools :python :pyi]
 :summary ["^check --aot:|classed|failing|negative control" 4]}                                           ; 27 s: 19 pass, 1 classed

;; ---- the full tier (nightly: a rotated part as its subset and a seventh; clean: the subsets, the :clean-only parts)
{:name python-roundtrip :mem-gib 2 :phase :full :chain python-full
 :cmd "bin/repr-python roundtrip {only} {part} && bin/repr-python roundtrip --text {only} {part}"            ; R1: --only-list
 :hosts [:stage0 :python] :never-loads [arbace.j2g] :pins ["test/python/corpora.edn"] :inputs [:lib-tools :tools :python]
 :parts [{:part stdlib} {:part site-packages} {:part lib-test}] :summary ["equal|known|differ|FAIL|control" 5]
 :rotate {:only "--only-list {list}" :universe "test/python/roundtrip-results.edn" :id-re "^\\[\"([^ \"]+) ([^\"]+)\"" :id-as "$1/$2"
          :subset "test/python/corpora.edn" :subset-key :subset-roundtrip :failed-re "^FAIL ([^[:cntrl:]]+)[[:cntrl:]].*$"}}
{:name python-front :mem-gib 2 :phase :full :chain python-full
 :cmd "bin/pyi scope-check {part} && bin/pyi compile-check --full {part}"          ; stdlib: 28 s + 43 s (:lines) on the VM
 :hosts [:stage0 :python] :never-loads [arbace.j2g] :pins ["test/python/corpora.edn"] :inputs [:lib-tools :tools :python :pyi]
 :parts [{:part stdlib} {:part site-packages} {:part lib-test}] :summary ["equal|not passing|FAIL|control" 4]}
{:name python-native :mem-gib 2 :phase :full :chain python-full                     ; PY9's verbs
 :cmd "bin/repr-python lex-check --no-mutants {only} {part} && bin/repr-python parse-check --full --no-mutants {only} {part}"
 :hosts [:stage0 :python] :never-loads [arbace.j2g] :pins ["test/python/corpora.edn"] :inputs [:lib-tools :tools :python]
 :parts [{:part stdlib} {:part site-packages} {:part lib-test} {:part lex-errors}
         {:part mutants :clean-only true :cmd "bin/repr-python lex-check samples && bin/repr-python parse-check --full samples"}]
 :summary ["equal|differ|rejected|FAIL|control" 5]
 :rotate {:only "--only-list {list}" :universe "test/python/parse-check-results.edn" :id-re "^\\[\"([^ \"]+) ([^\"]+)\"" :id-as "$1/$2"
          :subset "test/python/corpora.edn" :subset-key :subset-native :failed-re "^FAIL ([^[:cntrl:]]+)[[:cntrl:]].*$"}}
{:name pyi-check-aot :mem-gib 3 :phase :full :chain python-full
 :cmd "bin/pyi runtime check && bin/pyi record-ref --check && bin/pyi check --aot --no-pyi"   ; 151 programs: about 5 min warm
 :hosts [:stage0 :go :python] :never-loads [arbace.j2g] :pins ["test/python/corpora.edn"] :inputs [:lib-tools :tools :python :pyi]
 :parts [{:part light}
         {:part with-pyi :clean-only true :cmd "bin/pyi check --aot"}]                       ; pyi on goi: about 35 min (DECISION 2)
 :summary ["^check --aot:|classed|failing|negative control|FAIL" 5]}
{:name pyi-check :mem-gib 4 :phase :full :chain python-full
 :cmd "bin/pyi check --corpus {part} {only}"                                          ; PY7-check's verb, its universe and subsets
 :hosts [:stage0 :go :python] :never-loads [arbace.j2g] :pins ["test/python/corpora.edn"] :inputs [:lib-tools :tools :python :pyi]
 :parts [{:part language-core} {:part stdlib-tests} {:part lib-test-asyncio} {:part lib-test-all}]
 :summary ["methods:|classed|failing|regressions|control" 5]
 :rotate {:only "--only {list}" :universe "test/python/check-lib-test-linux-amd64.edn" :id-re "^\\[\"([^\"/]+)/"   ; modules (PY7's)
          :subset "test/python/corpora.edn" :subset-key :subset-lib-test :always ["test_bool" "test_int"]
          :failed-re "^   failing: ([^/]+)/.*$"}}
{:name pyi-bench :mem-gib 3 :phase :full :chain python-full :cmd "bin/pyi bench"
 :hosts [:stage0 :go :python] :never-loads [arbace.j2g] :inputs [:lib-tools :tools :python :pyi]
 :parts [{:part pyperformance :clean-only true}] :summary ["benchmarks|classed|FAIL" 3]}

;; :crops (test/gate/costs.ae's known set gains :python)
:python [pyi-aot-runtime python-read-essential pyi-aot-essential python-roundtrip python-front python-native pyi-check-aot pyi-check pyi-bench]
```

**The negative controls before entry** (GTS218 (a)): each step's family must have one that fails. Present: `check --aot`'s
judge (two passing programs against each other's references; the essential's 20 hold it), `lex-check`'s, PY7's
`test/python/controls.edn` (`bin/pyi check`), `lib-test-all`'s. Missing: the round trips (both modes), `parse-check`,
`scope-check` and `compile-check`: REQUESTs R1 and R3. Until they exist, `python-roundtrip`, `python-front` and the
`parse-check` half of `python-native` cannot enter under GTS218 (a), and `python-read-essential` enters only with them.

**The REQUESTs** (to their owners; R2 and R5 are commits on this branch, the others text):
- **R1 (PY9, `bin/repr-python`):** `roundtrip`, `roundtrip --text`, `lex-check` and `parse-check` write their results file on
  `--record` (every file's row, CS240's format, `test/python/{roundtrip,full,lex-check,parse-check}-results.edn`); take
  `--only-list FILE` (ids `CORPUS/FILE`, a judge that leaves out what a partial run did not run: GTS223 (a)); print a
  failure as `FAIL ID<TAB>WHY`; a negative control in the round trips and `parse-check` (a file's output judged against
  another file's reference, which must fail); `--no-mutants` keeps working beside a corpus argument.
- **R2 (PY7-check, `bin/pyi`; commit 407eac20):** `check --aot --only-list FILE`.
- **R3 (PY7-check, `bin/pyi`):** a negative control in `scope-check` and `compile-check` (a file's table or verdict judged
  against another file's reference).
- **R4 (PY7-check, `bin/pyi`):** the AOT's work directory (`.tmp/repr-python/pyi/aot`: the printed runtime, keyed by its
  forms' digest, and the compiled modules, keyed by their IR's hash) shareable from the main checkout's `.tmp` (as the
  gate's executables are), so the essential tier's cold 6.6 minutes are paid once per runtime, not once per worktree.
- **R5 (PY7-runtime, `arbace/python/aot.ae`; commit 36bf4e52):** the judge keeps pyi's outcome (`:pyi-outcome`) on a
  program whose compiled run passes but pyi's does not.

## 3. What the clean run takes from Python

**Steps:** the nine above, every part (the `:clean-only` ones too), under the audit; the rotation's record complete
(`bin/gate --rotation-status`) for `python-roundtrip`, `python-native` and `pyi-check`.

**Pins** (checked first, `bin/gate --pins`): `tools/pydump/PYTHON_VERSION` (the oracle: version, `sys.version`, the apk
`python3-3.14.10-r1` and its APKINDEX checksum per architecture), `test/python/corpora.edn` (the tarball's SHA-256 and
SHA-512, pyperformance 1.14.0's SHA-256 and size, five reference files, every corpus's files, lines and digest),
`tools/pydump/apks.sha256` (the 20 site-packages apks per architecture), and TamaGo's go (`seed/goroot.sha256`, already
the gate's) for the AOT's builds.

**Externals** (outside the clone): the host's `python3` 3.14.10 (Alpine's apk; it is the oracle of every reference and the
helper of every read check); `Python-3.14.10.tar.xz`, the 20 apks and `pyperformance-1.14.0.tar.gz` in the main checkout's
`.tmp/repr-python/src` or `REPR_PYTHON_SRC` (GTS212 (a): no network in the clean run); stage 0 (goi runs pyi's runtime
`go/arbace/py`, PYS18 (a)); `/root/tamago-go`.

**Records** the clean run judges, recorded beforehand on amd64 by the main host (the records rule): `check-aot-linux-amd64.edn`
(with pyi's runs), `check-lib-test-linux-amd64.edn` (PY7), the four results files (R1), `bench-aot-linux-amd64.edn`, the
amd64 costs (GTS222 (a)); then `bin/reap --record` and the `:until` keys of `cells.edn` removed (a recorded `:until` is a
problem: bin/reap names each).

**Caches** (GTS217 (a)): the helper's dumps (`tools/pydump`'s, CPython's) by key from the shared store; pyi's printed runtime and
compiled modules (R4) fresh, as Arbace's own products.

## 4. PYS18, embedding, measured

PYS18 (a) is decided (the user, 2026-11-03): repr/python and pyi evaluated from the checkout, out of `gate_key`, until the row
fits the budget, then embedded at a re-seed batched with the other crops'. What is left for PY11 is when, and what the
checks run on until then. The sample (the VM, 5 programs of the synthetic corpus and the samples, every output byte-equal
across the three ways):

| program | pyi on goi (`bin/pyi run`) | the loop, compiled (`PYI_AOT_LOOP`) | compiled (AOT) |
|---|---:|---:|---:|
| `print(1)` (startup alone) | 9.9-10.1 s | | |
| samples/functions.py | 20.9 s | 0.75-1.29 s | 0.78-1.31 s |
| synth/datamodel/copy-protocol.py | 14.0 s | 0.94 s | 1.65 s |
| synth/datamodel/numeric-all.py | 11.4 s | 0.66 s | 1.09 s |
| synth/ops/add.py | 11.0 s | 0.003 s | 0.004 s |
| synth/paths/async-gen-aclose.py | 18.4 s | 0.87 s | 0.95 s |

- **pyi on goi** costs about 10 s a process before the program starts (goi loading and initializing `go/arbace/py`), then
  roughly 4 to 15 times the compiled loop on these small programs (PY8a measured about 100 times on the benchmarks). The 151
  programs' pyi runs are about 35 minutes serial here (PY8a reported 17 minutes for its whole check with pyi's runs).
- **The loop** is pyi's own dispatch loop compiled by Go: what pyi is once embedded. It runs today without a re-seed, through
  the AOT's build (TamaGo's go, out of `gate_key`), and every check that can run on it (`check --aot`'s loop column,
  `lib-test --aot`'s driver) already pays the compiled price.
- **Embedding** would put `go/arbace/py` in goi's table and the shipped executable (a re-seed, then `gate_key` covers it: every
  change to pyi's runtime rebuilds the gate's executables, about 8 minutes). It buys pyi at the REPL of the shipped `arbace`
  and the 10 s startup gone from `bin/pyi run`; the gate's budget does not need it, since the loop gives the checks the same
  speed now.

```
DECISION: Does Python's milestone tag wait for the batched re-seed that embeds pyi (PYS18 (a)'s second half)?
- (a) No (recommended): tag with repr/python and pyi evaluated from the checkout and the checks on the compiled loop; embed at the first re-seed after the tag, batched with C++'s and Scheme's (CP180, SC230): no re-seed on Python's critical path, and the budget holds either way (the loop is the embedded speed).
- (b) Yes: re-seed for Python before its clean run: the clean run checks the shipped form, at the cost of a re-seed (bin/seed --make, --verify) and gate_key growing by pyi's runtime before the tag.
```

```
DECISION: What does the eval level's synthetic source judge in the clean run: pyi on goi, or the compiled loop?
- (a) The loop in every tier (recommended): check --aot's loop column (the same runtime and compiler, pyi's dispatch loop compiled), about 5 min for the 151 programs; pyi on goi's runs (35 min) rotate a seventh a night (`with-pyi` as a rotated part) and stay in `bin/pyi check --aot --record`'s record when the main host records it: within GTS225's one-night clean run.
- (b) pyi on goi in every clean run (the :clean-only `with-pyi` part as written above): the evaluated form judged at the tag, 35 min more, Python's clean share (about 54 min under GTS225) exceeded.
```

## 5. The budget

The shares: GT9.1's (6 h nightly, 24 h clean; a crop 32.4 and 130 min) and the user's day and night of 2026-11-04 (GTS224: 8 h
of planned nightly runs, so a crop about 43 min; GTS225: the clean run within one night of about 10 h planned, so a crop about
54 min). Estimates scaled 1:1 from the VM (*est.*), to replace by the first two `--nightly` runs on amd64 (GTS222 (a)).

| tier | Python's content | measured or estimated | share |
|---|---|---|---|
| essential | `python-read-essential` 13 s, `pyi-aot-essential` 27 s | **40 s warm, measured**; cold +6.6 min in the `:build` step `pyi-aot-runtime` | 2 min (GTS215) |
| nightly | round trips: subset and a seventh (whole: 4.5 + 8.5 min) | 3 *est.* | |
| | front: scope and compile over the three corpora (stdlib measured 28 s + 43 s; 2,980 files) | 5 *est.* | |
| | native: lex and parse, subset and a seventh (whole 6.5 and 11.5-13 min, PY9's) | 4 *est.* | |
| | `pyi-check-aot.light` whole (151 programs compiled and on the loop) | 5 warm, measured (6.6 cold) | |
| | `pyi-check`: PY7's coverage subset and a seventh of Lib/test (the whole about 15 job-hours on the VM: 1/7 at 8 jobs about 16 min) | 26 *est.* | |
| | **sum** | **about 43 *est.*** | 43.2 (GTS224) / 32.4 (GT9) |
| clean | the subsets of the rotated parts | 30 *est.* | |
| | the `:clean-only` parts: the mutants (5), `pyi-bench` (7), `with-pyi` (35, or 0 under DECISION 2 (a)) | 12 to 47 *est.* | |
| | `pins --check`, the helper's dumps (cached, GTS217) and the AOT's cold build | 8 *est.* | |
| | **sum** | **about 50 *est.* (85 with (b))** | 54 (GTS225) / 130 (GT9) |

Python is at its nightly share and within its clean share under DECISION 2 (a), both estimates from a loaded aarch64 VM; the
nightly figure rests on PY7's coverage subset, whose cover run is the main host's.

## 6. The main host's commands (expected output)

On this branch alone (stage 0 of the seed `a59cc441`):
- `bin/reap` → `reap: 18 of 20 cells checked, 2 with no check yet; 0 problems` (Python read `0+ / 34 / 0`, print `0+ / 5 / 0`).
- `make python-essential` → about a minute warm; ends `check --aot: 20 programs: 19 pass, 1 classed, 0 failing` and
  `the negative control: ... do not pass`.

At the merges, in order:
1. PY9 (`mac/201-python-py9`): remove `:until` from the `lex-check` and `parse-check` sources; `bin/reap --record`.
2. On amd64, `bin/pyi check --aot --record` → `check --aot: 151 programs: 138 pass, 13 classed, 0 failing` as on the VM (fewer
   classed once PY7-runtime fixes `divergences-synth.edn`'s classes; a program pyi on goi runs otherwise than the compiled one
   shows as `pyi-differs` in the eval cell); remove both `:until "PY8 ..."`; `bin/reap --record` → AOT `138 / 0 / 13`.
3. PY7 (`mac/201-python-py7check`): `bin/pyi check --record` (the night's); remove `:until "PY7 ..."`; `bin/reap --record`.
4. R1 written: `bin/repr-python roundtrip --record`, `--text --record`, `lex-check --record`, `parse-check --full --record` on
   amd64; the read and print cells then count their passes (the "+" goes).
5. GT9's planner changes, then the block of §2; `bin/gate --plan` and `bin/gate --audit pyi-aot-essential python-read-essential`.
