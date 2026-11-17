# W: the re-seed window (plan)

Status: a plan, 2026-11-05, branch `window-w` (from `next` 88b71ffa4), W-P1 to W-P10 decided by the user (2026-11-05); refreshed
2026-11-09 by `.tmp/window-w/PLAN.md` (W-D1 to W-D9, decided by the user 2026-11-09, "Decided (2026-11-09)" below). W2's candidate
is `window-w2b` (day step D-W2a, "As built: D-W2a" below), on `next` c2536d182. Nothing here is merged or built.

W is the batched re-seed that follows the tag `arbace-reaps-4-crops-v1` and the merge plan's batches
(`.tmp/tag-v2/merge-plan.md`, batch W; its rehearsal `.tmp/tag-v2/merge-plan-rehearsal-1105.md`, §3 and §4). It carries:

- eval-speed (doc/go/EVAL-SPEED.md): the evaluator's speed, a runtime change held for a re-seed;
- the embedding of C++'s repr/cpp, repr/llvm and lowering and of Scheme's repr/scheme and si (CP180 (a), SC230 (a)), with
  embed-gaps' si shipped path (SC240 (a)), oi listed in goi's table (OC165 (a), OC170 (a)) and ci in `RT.toolSource`
  (CP191 (a));
- pyi's embedding (PYS18 (a), PYS28 (a): `arbace/py` and `arbace/libm` in goi's table, pyi's front end embedded);
- repr/c's nesting guard (808d08d60 on `next`, 2026-11-05), compiled into stage 0 for the first time by W2's seed.

The rehearsal recommends two nights, and this plan follows it:

- **W1** proves eval-speed alone: its build, the gate, the fixpoint and the keyed `--full`. The same night runs W2's light
  checks on the candidate tree.
- **W2** adds the embedding, then makes the fixpoint and the seed.

A failure in W1 then costs a night of checks, not the seed night. A failure in W2 cannot be confused with eval-speed's.

## 1. Where W sits

The order follows rehearsal §4. First the tag, then batch 1 (the rows, the keys commit `gt9-build` 100f8e0be, Python, the
192 redo), P's records night, batches 2+3, batch 4, batch 5, then Python's tag (PYS18 (a): W waits for it). W1 and W2
come after that, on consecutive nights. Then comes the keyed `--full` that GTS230 asks after a re-seed (W-P8).

W's inputs only exist after the batches. Today's work therefore prepares the parts that can be prepared on today's `next`,
and states the rule for the rest.

## 2. What was prepared today (branches, all local and pushed to `mac`)

| branch | head | what |
|---|---|---|
| `window-w-eval-speed` | d996a695f | `eval-speed` (840eb78cb) rebased onto `next` 88b71ffa4; `eval-speed` itself is untouched |
| `window-w-embed` | e52e368fd | `embed-gaps` (012dc86a1) rebased onto `window-w-eval-speed`; the trial of W2's tree without the batches |
| `window-w-keys-trial` | e923f4731 | a rehearsal merge of `window-w-embed` into `gt9-build` (the keys commit), resolved by §4's rule; **not to merge** |
| `window-w` | (this doc) | the plan; the scripts are in the worktree's `.tmp/window-w/` |

**eval-speed's rebase.** It was clean, with no file touched by both sides: eval-speed changes 15 files and `next` 52 since
their base 01d3ce9f6. Two checks confirm it:

- every eval-speed file is byte-identical to `eval-speed`'s tip;
- every file `next` changed is byte-identical to `next`.

The rebase therefore invalidates none of eval-speed's own checks. `CompilerNodes.ae` stays valid: its generator's input,
`arbace/c2j/emit.ae`, changed on eval-speed alone. The same holds for `jdk/edge.edn`, which `next` did not touch. Not run
today (each needs a build of the checkout): `bin/generated-check`, `bin/diffcheck`, the suite, the oracle and
`bin/j2g-edge verify`. They are W1's step 00, the essential gate.

**embed-gaps' rebase.** Two textual conflicts, both in the header comment of `test/gate/gate-key-crops-test.bash`
(embed-crops 3 and embed-gaps against `next`'s comment about the re-seed after the backlog). Both resolved as unions. The
rebased tree's `gate-key-crops-test.bash` passes.

That pass hides a semantic conflict. The test still says that `go/arbace/{oi,scm,scminterp}` move neither the runtime key
nor the javac key, while embed-gaps copies those three packages into every program, javac's tool's included (§4).

## 3. W1: eval-speed (one night, `.tmp/window-w/w1.sh`)

**Day.** The main session merges `window-w-eval-speed`, refreshed onto the batches' `next` with `git rebase next` on a new
branch, into `next`. It prepares W2's candidate `window-w2` (§5) on the same `next`.

**Night** (dry-run plan, from 19:00 UTC):

| step | est. | expect | on fail |
|---|---|---|---|
| 00 `bin/gate` on next | 75 min | the essential gate passes; every key moved (eval-speed changes `arbace/lang`, `arbace/c2j`, `go/arbace/jrt`), both builds take the two-run path; `generated-check`, diffcheck, suite 78/78, oracle, `j2g-edge verify` | stop |
| 01 main fast-forwarded and pushed | 1 | main = next | stop |
| 02 worktree `window-w1` at main, stage 0 linked | 4 | | stop |
| 03 `bin/arbace-go --fixpoint --g1 GATE_CACHE` | 30 | `fixpoint: holds (G2 = G3)` with the new evaluator | mark |
| 04 `test/bench/eval-micro.ae` on the old stage 0 and on the gate's build | 5 | the calls, arrays and dispatch groups faster, as EVAL-SPEED.md's tables | mark |
| 05 W2's candidate on the old stage 0: `gate-key-crops-test`, the c2j checks of repr-cpp, repr-scheme, repr-ocaml, ci and pyi, `repr-ocaml embed-check`, `si interp-check`, `oi table-check`, `repr-c parse-check --corpus torture-compile --no-mutants` | 47 | each passes; the candidate's tree recorded (W2 then skips its own light checks for that tree) | mark |
| 06 `bin/j2g-edge verify` on the candidate | 10 | the edge holds for the embedded namespaces; else `update` and commit on `window-w2` by day | mark |
| 07 keyed `bin/gate --full --concurrent` to 12:00 (W-P2) | ~600 | passes, or stopped with its passes kept | mark |

Steps 00-06 take about 2 h 50 min, and the keyed `--full` takes the rest of the night and the morning.

If 00 or 03 fails, eval-speed leaves `next` (its merge reverted) and W2 embeds alone. Since eval-speed reaches stage 0 only
through a seed, a bug found after W2 would need another re-seed. This is why W1 runs the whole `--full` (W-P2).

## 4. The keys at W2 (the rule, rehearsed)

After batch 1 the keys commit (PYS30 (a), `gt9-build` 100f8e0be) holds one list, `bin/lib/version.bash`'s
`arbace_crop_paths`. `gate_key`, `gate_rest_key`, `arbace_runtime_key`, `arbace_embedded_sources`, `arbace_javac_key`
and `arbace_javac_rest_key` all leave it out, and `test/gate/audit.ae`'s `crop-rules` mirrors it. The rule for W2:

1. **What is embedded leaves the list** and `crop-rules`. That means `arbace/repr/scheme`, `arbace/scheme`,
   `arbace/repr/cpp`, `arbace/repr/llvm` and `arbace/cpp`, plus the Go forms copied into the program,
   `go/arbace/{oi,scm,scminterp}`. With pyi (PYS28 (a)) the list also loses `arbace/python`, `arbace/repr/python`,
   `go/arbace/py` and `go/arbace/libm`, and `go/arbace/{expat,pyexpat}` if W-P6 (b) is chosen.
   - These paths then enter `gate_key` (the executable embeds them), the javac key (javac's tool is built by the same
     `j2g --program`, which copies the Go packages and lists goi's table) and the audit's `:gate`, `:prog` and `:javac`.
   - The `:javac` host's own exclusions of `oi`, `scm` and `scminterp` (from the re-seed after the backlog) go too.
2. **The embedded crops' Go forms stay out of `arbace_runtime_key` alone** (W-P3 (a)): a new array `arbace_runtime_not`.
   c2j's compilation of the namespaces analyzes the runtime's classes, never their Go natives (PYS30 (a)'s reason). A crop's
   Go commit after W2 then rebuilds the executables but takes no two-run path.
3. **The check tools that compare with an oracle's dump stay out** (W-P4 (a)): `arbace/repr/cpp/ncheck.ae`,
   `arbace/repr/cpp/tcheck.ae` and `arbace/repr/llvm/mutate.ae` go into `embed.ae`'s excluded and into the list.
4. **`arbace/rust` stays out** (W-P5 (a)).
5. embed-gaps' `arbace_embedded_resources` (si's `psyntax.pp.ae` in `gate_key`) and `bin/lib/crops` (out of the keys,
   now a list entry) are kept.

`window-w-keys-trial` applies this rule to `gate-key-crops-test.bash`, now in its W2 form (the embedded Go forms move the
javac key, not the runtime key; the audit's `:javac` and `crop-rules` no longer name them). The conflicts were
`embed.ae`, `bin/arbace-go`, `bin/lib/version.bash` and `test/gate/audit.ae`, and each resolves as "the keys commit's form,
less the embedded paths". Light checks on the trial:

- `test/gate/gate-key-crops-test.bash`: PASS;
- `test/gate/audited-test.bash`: 19 of 19;
- `test/gate/crops-test.bash`: ok.

The keys measured (first 10 hex digits; `bin/arbace-go --gate-key`, `arbace_runtime_key`, `arbace_javac_key`):

| tree | gate | runtime | javac |
|---|---|---|---|
| `next` 88b71ffa4 | 3157cb94cd | f29a387594 | 8f24f568a9 |
| `window-w-eval-speed` | c96d0a62de | 672cfc0390 | a844e4becc |
| `window-w-embed` | 61c05436a4 | 719e3714e2 | 92dd1a3916 |
| `gt9-build` 11579f826 | 9fda8cf334 | c40e01ac56 | c611505b36 |
| `window-w-keys-trial` | 40f5e8bde4 | dcca2904cd | dbf732d329 |

eval-speed moves all three keys. The embedding moves the runtime key again through `arbace/lang/SchemeInterp.ae`. W1
and W2 are therefore both rekey-all nights, as the rehearsal expected.

## 5. W2's candidate, `window-w2` (prepared by day after Python's tag)

The candidate is built on the batches' `next`, which already holds eval-speed after W1:

1. `embed-gaps` at its final head (embed-crops 1-3, CP191's commit, the gaps), rebased, with §4's key resolution. Its
   survey conflicts (CPP, SCHEME and OCAML-SURVEY) are unions after the rows' and native lines' sections.
   `gate-key-crops-test.bash` follows the trial.
2. pyi's embedding (PYS28 (a); AFTER-PY11 §4.1 on `mac/201-python-py7-py11`):
   - `go/arbace/gotable.edn`'s `:packages` gains `"arbace/libm"` and `"arbace/py"` beside embed-gaps' `"arbace/oi"`. This
     is the rehearsal's gotable conflict: a union in sorted order.
   - `arbace/go/table.ae`'s `program-path?` accepts `arbace/py` and `arbace/libm` as it does `arbace/oi`.
   - `embed.ae` drops `arbace/python{.ae,/}` and `arbace/repr/python{.ae,/}`.
   - `RT.toolSource` gains `arbace/python` and `arbace/python/` (otherwise a changed pyi front end would run its stale
     compiled copy on stage 0).
   - §4's list entries are removed.
   - `bin/pyi c2j-check` comes from queue 206 (`mac/206-python-c2j-check` 36f7a4da) if Python's merge has not brought it.
   - PYS29 (a): no stdlib embedded.
   - Not in W2: the in-process compiler and `sys.executable` (AFTER-PY11 §4.1 items 6-7; W-P10).
3. The night's verbs must exist on the merged scripts: `w2.sh --check`'s `verbs` precondition lists them.
4. `bin/repr-cpp c2j-check` re-run on the merged tree. The embedded set now holds C++'s native parser, LLVM's verifier and
   `full` and the C++ `dg`/`lit` (19 files beyond E.1's measurement), and `cpp-c2j` fails on any reflection warning.

## 6. W2: the embedding and the seed (one night, `.tmp/window-w/w2.sh`)

The shape is `.tmp/night5/reseed.sh`, with CPP-SURVEY E.4/E.6's rows and AFTER-PY11 §4.3's window:

| step | est. | expect | on fail |
|---|---|---|---|
| 00 `bin/gate` on next | 75 min | passes; `cpp-c2j`, `scheme-c2j` and the crops' essential steps on the embedded tree | stop |
| 01 main fast-forwarded | 1 | | stop |
| 02 branch `reseed-w2` at main, old stage 0 linked | 4 | | stop |
| 02c light checks on the old stage 0 | 35 (0 if W1 checked this tree) | as W1's 05 | stop |
| 03 `ARBACE_GO_ARCHES=amd64 bin/arbace-go --build` | 25 | `aot: ... 0 failed`; no `image: not prepared` for `arbace.{cpp,scheme,repr.cpp,repr.llvm,repr.scheme,python,repr.python}`; goi's table lists `arbace/oi`, `arbace/py`, `arbace/libm`; no `aot: not loaded: arbace.scheme.interp` | stop |
| 04 on the executable: `--smoke`, `si interp-check --exe` (61/64, 3 known), `si test`, `oi test`, the embedded psyntax resource (`:embedded`), `cppi lower-check` on the samples, `make python-essential` | 30 | each as on stage 0 | stop |
| 04b repr/c's guard on the executable: `repr-c parse-check --corpus torture-compile --no-mutants`, `repr-c roundtrip --corpus torture-compile`, `ci annotations --corpus torture-compile` | 15 | every file equal or known; no stack overflow | stop |
| 05 `bin/goi table --size` | 5 | sizes printed (W-P7) | mark |
| 06-09 clean tree, `--fixpoint`, `seed --make` (G3), commit, `seed --verify` | 88 | `G2 = G3`; a new pin; `stage 0 = G1 = G2 = G3` | stop |
| 10 stage 0 from the new seed | 8 | its AOT pass 0 failed | stop |
| 12 the light checks again on the new stage 0, `bin/reap` | 35 | as 02c | mark |
| 13 the guard on the new stage 0 | 15 | as 04b | mark |
| 14 `bin/gate` on `reseed-w2` | 75 | passes | stop |
| 15 main and next fast-forwarded and pushed | 1 | | stop |
| 16 main's stage 0 | 8 | | mark |

About 7 h, ending around 02:00 UTC, which is inside GTS224's 8 h. The keyed `--full` follows the next night (W-P8). The
guard's check runs twice: before the seed on the executable, where a crash stops the night before a seed is made, and after
it on the new stage 0.

## 7. Costs (nights)

| night | serial work | wall (est.) |
|---|---|---|
| W1 | gate rekey-all 75 min + fixpoint 30 + W2's light checks 57 + keyed `--full` about 10 h | 19:00 to about 08:00, the `--full` into the morning (overflow allowed, as night 5) |
| W2 | 7 h (two gates, a build, the fixpoint, the verify, the checks) | 19:00 to about 02:00 |
| W2+1 | the keyed `--full` after the seed (GTS230: every key moved with stage 0) | a night |

Peak memory: the last seed build peaked at 20.4 GiB with C alone. With C++, LLVM, Scheme and pyi added (about 70 sources
over night 5's 157) the estimate is 26-30 GiB, so `w2.sh` requires 48 GiB available.

## 8. Risks

1. **The keys after W2.** Every commit to an embedded crop moves `gate_key` and the javac key, and therefore the gate's
   builds, every executable-keyed step and javac's tool. C++ (`cpp-codegen`, `cpp-tree`) and pyi are moving fast. After
   W2 a crop commit is a rekey night unless it is held (W-P9).
2. **Types redefined by a reloaded namespace** (CPP-SURVEY E.2): when `RT.toolSource` reloads a changed
   `arbace.scheme.rt` (`definterface`/`deftype`) alone, the compiled namespaces that use it keep the old types. The PATH_FIRST
   trio was removed from C's steps today (d86cb874a); the crops' steps must not need it (W-P9).
3. **The embedded set is untested beyond 14 C++ sources** (E.1). W1's step 05 is the first c2j check of the merged set.
4. **eval-speed's behaviour change**: `(aset ^floats a i 1e300)` stores `##Inf`. The oracle's and diffcheck's known files
   meet it at W1's step 00.
5. **goi's table grows** by oi's and pyi's surface (pyi: 173 functions, 183 methods, 63 types, 20 vars and constants). The
   table's generation and the build's memory grow too. `bin/goi table --size` measures this at step 05.
6. **The candidate moves between W1 and W2**: the light checks are skipped only when the tree is the one W1 checked.
7. **Heads still moving** (embed-gaps, the Python line): refresh the candidate the day of W2, never at night.
8. **`w1.sh` and `w2.sh` hard-code the night's verbs.** Re-read them against the merged `steps.edn` on W's days. The
   `verbs` precondition catches a missing verb, not a changed meaning.

## 9. The scripts

`.tmp/window-w/{lib.sh,w1.sh,w2.sh}` in this worktree; copy them to the main checkout's `.tmp/window-w/` for the nights.

- `lib.sh` is `.tmp/night5/lib.sh` with N the scripts' directory and the seed pin d5548e72.
- `--dry-run` is the default and prints the plan.
- `--check` checks the preconditions, read-only.
- `--run` and `--detach` run the night.
- `W_BASE=REF w2.sh --check` tests the content preconditions on another tree.

**Tested today (`--check` only).**

- `w1.sh --check` on today's `next`: 4 preconditions fail, as they must before W: eval-speed not merged, its core commit,
  no keys commit, no `window-w2`.
- `w2.sh --check`: 6 fail on `next`: no candidate, no embedding, no key rule, `toolSource`, the verbs, no W1 log. The guard
  passes (808d08d60).
- `W_BASE=window-w-keys-trial PY_EMBED=0 w2.sh --check`: the embedding, keys, toolSource and verbs preconditions pass. The
  guard fails there because the trial predates 808d08d60. So each check tells the trees apart.

One fix was found while testing: `git show ... | grep -q` fails under `pipefail` when grep exits early (SIGPIPE), which
made the guard precondition fail on `next`. The scripts use `gq`, a grep that reads its whole input.

## Proposed

- **W-P1 Two nights or one.**
  - (a) W1 and W2 as above. Costs one more night and keeps eval-speed's proof apart.
  - (b) One night, as CPP-SURVEY E.4: about 9 h plus the `--full` the night after. A failure is harder to attribute.
  - *Recommended: (a).*
- **W-P2 W1's tail.**
  - (a) The keyed `--full` (about 10 h, into the morning). It is eval-speed's whole proof before the seed carries it.
  - (b) `bin/gate --nightly`. Lighter, but a bug found after W2 would need another re-seed.
  - *Recommended: (a).*
- **W-P3 The embedded crops' Go forms and the runtime key.**
  - (a) Out of `arbace_runtime_key` alone (`arbace_runtime_not`), in `gate_key` and the javac key. This is PYS30 (a)'s
    reason: c2j never depends on them.
  - (b) Back in all three keys (one list). Each crop Go commit then takes the two-run build (+3 min a build) and moves
    `aot.key`.
  - *Recommended: (a).*
- **W-P4 The oracle-comparing check tools** (`repr/cpp/ncheck`, `tcheck`, `repr/llvm/mutate`).
  - (a) Not embedded: listed in `embed.ae`'s excluded and in `arbace_crop_paths`, evaluated by their scripts.
  - (b) Embedded as C's `dg` is. Larger, and their churn moves the keys.
  - *Recommended: (a).* C++'s `dg`, `lit`, LLVM's `diag` and `full` are product code and stay embedded.
- **W-P5 `arbace/rust`.**
  - (a) Stays out until Rust's row and tag. RES14 (a)'s text, "the re-seed that embeds C++'s lowering", is amended to
    "the first re-seed after RE9".
  - (b) Embedded at W2 as RES14 (a) reads. rsi is mid-work (RE5) and every commit would rekey the gate.
  - *Recommended: (a).*
- **W-P6 pyexpat and expat.**
  - (a) Not listed: pyexpat stays a lazy module (queue 202e), interpreted from forms on first import.
  - (b) Listed too (`arbace/pyexpat`, `arbace/expat` compiled): about 2.8 s of goi's start saved where used, and a larger
    table.
  - *Recommended: (a)*, PYS28's text, measured later.
- **W-P7 Where `bin/goi table --size` is recorded.**
  - (a) In the surveys (OCAML-SURVEY, PYTHON-SURVEY), out of every key, by day after W2.
  - (b) In `gotable.edn`'s comment, as E.6 asks. Before the fixpoint it changes the tree the night has already built; after
    the seed it rekeys the next night for a comment.
  - *Recommended: (a).*
- **W-P8 The keyed `--full` after W2.**
  - (a) The next night alone.
  - (b) Appended to W2 (`W2_FULL=1`): about 17 h, past noon.
  - *Recommended: (a).*
- **W-P9 Crop commits after W2.**
  - (a) Merge them as they come; each is a rekey night (8 h serial, the lanes shorter). Steps that meet risk 2 get the
    PATH_FIRST trio one by one.
  - (b) Batch the embedded crops' merges on a weekly day, so that their rekeys share a night.
  - *Recommended: (b)* while C++ and pyi move daily.
- **W-P10 pyi's in-process compiler and `sys.executable`** (AFTER-PY11 §4.1 items 6-7).
  - (a) After W2, as their own work. W2 embeds the runtime and the front end; `bin/pyi run`'s compile server stays.
  - (b) In W2. Larger, and untested now.
  - *Recommended: (a).*

## Decided (2026-11-05, the user)

W-P1-W-P10 as recommended: two nights W1 and W2; W1 ends with a keyed --full; the embedded crops' Go forms out of the runtime key only (arbace_runtime_not); ncheck, tcheck and mutate not embedded; arbace/rust out until RE9 (RES14 amended); pyexpat and expat not in goi's table; the table's size in the surveys; a keyed --full the night after W2; the embedded crops' merges batched after W2 (b); pyi's in-process compiler and sys.executable after W2.

## Decided (2026-11-09, the user)

From `.tmp/window-w/PLAN.md` §9 (the plan of 2026-11-09, which refreshes this one for batches 1-4, the C triage, Java's night,
OCaml's C API, LLVM IR's and Rust's rows):

- **W-D1 (a)**: pyi embedded in W2 now (repr/python and arbace/python compiled with the program, `arbace/py` and `arbace/libm`
  listed in goi's table), and ci's back end `arbace/ci`, `arbace/crt` listed too (queue 208's DECISION 1 (a)); Python's tag later,
  on the embedded pyi. This amends PYS18 (a)'s and queue 208's "at the first re-seed after Python's tag".
- **W-D2 (a)**: W1 on its own night (night 13), after night 12's `bin/gate --full` on next.
- **W-D3 (a)**: `arbace/classes/` in `RT.toolSource`, so stage 0's j2g runs the checkout's analyzer; R retires
  `build-classes-host2`'s second run.
- **W-D4 (a)**: llvmi (`arbace/llvm/`) stays out until LLVM IR's own milestone.
- **W-D5 (a)**: OCaml: oi alone (`go/arbace/oi` listed); repr/ocaml stays evaluated.
- **W-D6 (a)**: R (the retirement commit) in W2's night, step 11, after the verify, before the post-seed gate.
- **W-D7 (a)**: no clean run right after W; the next tag's clean run proves the seed from a fresh clone.
- **W-D8 (a)**: an arm64 check by the Mac VM after W2 (a queue task: `bin/seed --check`, `--build`, `bin/reap`, `bin/gate-tests`,
  `bin/ci judge-test`; report only).
- **W-D9** (disk before W1: merged worktrees removed, `bin/gate --prune`, at most 70%): not accepted.

## As built: D-W2a (`window-w2b`, 2026-11-09)

The `window-w2` candidate (PLAN.md §10, item 2) redone on today's `next` (mac/next c2536d182) as `window-w2b`, by day, light: no
build, no fixpoint, no seed, no gate run but `--plan` (those are W2's night). The old embed-crops (0c711daa6, ad0165745) and
embed-gaps (16fac1462, 012dc86a1) of 2026-11-04 conflict with today's `next` in 15 files (`.tmp/window-w/mt-window-w-embed.txt`),
mostly where the keys commit (PYS30 (a)) replaced each key's own exclusions by `arbace_crop_paths`: the code was redone on today's
tree, taking each old commit as the reference, rather than merged.

### The commits

| commit | what | from |
|---|---|---|
| f6a7d67e9 | si's shipped path (`arbace.lang.SchemeInterp`, `go/arbace/scminterp`, scm and scminterp copied into the program, psyntax.pp.ss a generated resource: `arbace.scheme.resource`, `embed/generated`, `arbace_embedded_resources`), `bin/si interp-check`; oi listed in goi's table (`gotable.edn`, `program-path?`, j2g's copy), `ext_sys.ae` split into `ext_sys_linux.ae`/`ext_sys_other.ae`, `bin/oi table-check`, `bin/goi table-configs` | 16fac1462 cherry-picked; 6 files conflicted, resolved on next's side plus the commit's additions (oi has grown 8 files since: they keep next's list, the split's two added) |
| 2ef68c884 | goi's REQUEST: a loaded package's `//go:build` honoured by the REPL; `:init-order` verified in a configuration that leaves files out | 656dfcdc3, clean |
| (none) | goi's REQUEST c9047d524 (popped R and C slots cleared at restore) | already on next (8ea59b57a's liveness, with guards): skipped |
| 79bcbc869 | crt for tamago: `sys_linux.ae`, `sys_other.ae` (access(2), Stat_t's times) | 3db255c5f; `crt.ae`'s file list a union with next's `sched.go`, `spawn.go` |
| e7e9d9b95 | goi's table lists `arbace/ci`, `arbace/crt`, `arbace/libm`, `arbace/py` beside `arbace/oi`; `program-path?` takes the five; j2g copies libm and py | a420fa0d5; three conflicts, unions |
| 8803e5b2e | the embedding and the keys (below); `RT.toolSource` | 0c711daa6, 101c0d032, 0525fa896, e923f4731 (the keys trial), rewritten |
| 293936f4c | `arbace/repr/llvm/expect.ae`: the one reflection warning of the embedded C++ set (an `aset` on a long array) | new |

Not taken: embed-crops' and embed-gaps' survey texts (CPP-SURVEY E.4-E.6, SCHEME-SURVEY and OCAML-SURVEY "As built: the embedding's
preparation" and "the embedding's gaps", ad0165745's CP191 line): they are 11-04's notes of the same work, conflicting with the
surveys' later sections; this section is the as-built record, and the surveys' own notes are the crops' owners' to merge after W2.
d603d2ea5 and 87c58f158 (the crops' c2j checks and their hints) are on next already under other hashes.

### The embedded set

`arbace/j2g/embed.ae`'s excluded loses `arbace/repr/scheme{.ae,/}`, `arbace/scheme/`, `arbace/repr/python{.ae,/}`,
`arbace/python{.ae,/}`, `arbace/repr/cpp{.ae,/}`, `arbace/repr/llvm{.ae,/}` and `arbace/cpp/` and gains
`arbace/repr/cpp/ncheck.ae`, `arbace/repr/cpp/tcheck.ae`, `arbace/repr/llvm/mutate.ae` (W-P4 (a)). Out as before: repr/haskell,
repr/rust, repr/ocaml (W-D5 (a)), `arbace/rust` (W-P5 (a)), `arbace/llvm` (W-D4 (a)). The crops' namespaces now compiled with the
program: C++ 34 sources (repr/cpp 15, repr/llvm 9, arbace/cpp 8 and the two roots; `arbace/cpp/rt/*.c.ae` embedded as resources),
Scheme 23 (and `arbace.scheme.interp`, which needs the program's `arbace.lang.SchemeInterp`), Python 20. No embedded namespace requires
a namespace left out (`arbace.llvm.run` and `arbace.repr.llvm.mutate` are named in comments only).

The Go forms copied into the program: `go/arbace/{crt,ci,cinterp,scm,scminterp,oi,libm,py}` (`arbace/j2g/main.ae`); listed in goi's
table: `arbace/ci`, `arbace/crt`, `arbace/libm`, `arbace/oi`, `arbace/py`. Not listed (W-P6 (a)): oicapi, expat, pyexpat, clib,
pybz2, pylzma, pyzstd, pyssl, pysqlite3; hs.

### The keys (§4's rule on today's tree)

- `bin/lib/version.bash`'s `arbace_crop_paths` keeps repr/haskell, repr/rust, repr/ocaml, `arbace/rust`, `arbace/llvm`, the three
  oracle check tools, the Go forms oicapi, expat, pyexpat, hs and pyi's lazy modules, `jdk/ocaml`, and the crops' scripts'
  libraries; the embedded namespaces and Go forms leave it. `arbace_runtime_not` keeps oi, scm, scminterp, py and libm out of
  `arbace_runtime_key` alone (W-P3 (a)).
- `test/gate/audit.ae`'s `crop-rules` mirror the list; the `:gate` and `:javac` hosts' own copies of the embedded paths' exclusions
  are gone (`:gate` names `scheme/psyntax/psyntax.pp.ae`).
- `test/gate/steps.edn`'s `:tools` keeps the crops' namespaces out (W-D10 below; a comment, no input changed).
- `test/gate/gate-key-crops-test.bash` in its W2 form: the embedded paths hashed by `gate_key` and the javac key, their Go forms
  moving the javac key and not the runtime key, the audit's `:gate`, `:javac` and `crop-rules` naming none of them, the paths still
  out moving no key: PASS, 309 checks.

The keys measured (first 12 hex digits):

| tree | gate_key | runtime | javac | aot |
|---|---|---|---|---|
| `next` c2536d182 | 1c6836bc492e | 6baefbe7230d | f0cd740e7eaa | 23926633fa9d |
| `window-w2b` 293936f4c | fb37dc317a32 | 42b40f043fff | 8ffad8259f1a | 72280fa2a220 |

All four move: `RT.ae` (toolSource) and `arbace/lang/SchemeInterp.ae` are runtime sources, so the first build takes the two-run path.
`bin/gate --full --plan` on both trees: of 316 steps, 310 have another key (or wait for a program host that is rebuilt), the other 6
the always and build steps: W2's merge is a rekey-all, as PLAN.md §8 expects.

### `RT.toolSource`

Gains `arbace/c`, `arbace/c/` (CS281 (a), CP191 (a)), `arbace/cpp/`, `arbace/scheme/`, `arbace/python`, `arbace/python/` (PYS28 (a))
and `arbace/classes/` (W-D3 (a)). repr/cpp's, repr/llvm's, repr/scheme's and repr/python's are `arbace/repr/`'s already. Effective
only once a seed carries it (stage 0 before W2 runs the old `toolSource`).

### Checks (light, on main's stage 0, `ARBACE_HOST_EXE=/root/arbace/target/seed/amd64/arbace`)

| check | result | wall | peak RSS |
|---|---|---|---|
| `bin/repr-cpp c2j-check` | first run FAILED, 1 reflection warning (`arbace/repr/llvm/expect.ae:160`); after 293936f4c: ok, 34 of 34 sources, 5783 classes | 8 min 12 s | 2.20 GB |
| `bin/repr-scheme c2j-check` | ok, 23 of 23 sources, 1556 classes | 35 s | 1.55 GB |
| `bin/pyi c2j-check` | ok, 20 of 20 namespaces, 2345 classes, the negative control refused | 1 min 52 s | 1.81 GB |
| `bin/ci c2j-check` | ok, 27 sources, 3182 classes | 1 min 39 s | 1.76 GB |
| `bin/oi table-check` | stopped: its linux configurations convert linux's std first (`bin/goi table-configs`, repr/go's native read and annotate, 3.5 GB and rising after 17 minutes): a whole-corpus read, the night's | | |
| `test/python/table_check.ae` over `arbace/{libm,py,crt,ci,oi}`, tamago/amd64 and tamago/arm64 only (what `bin/pyi table-check --configs` runs, with oi added) | **FAILED**: libm and ci no error; py 2 errors, crt 5, oi 101 (in each tamago configuration; below) | 2 min 35 s | 1.78 GB |
| `test/gate/gate-key-crops-test.bash` | PASS (309) | | |
| `test/gate/audited-test.bash` | 19 of 19 | | |
| `gate.crops check` (steps.edn) | 0 problems | | |
| `test/gate/crops-test.bash` | 41 ok, 1 FAIL, on next too: its expected concurrent chains name haskell-target-probe and the others, the plan gives haskell-roundtrip alone (steps.edn's chains are next's; this branch changes a comment of it only) | | |
| `make check-makefile` | ok | | |
| `bin/gate --plan`, `--full --plan` | ok (above) | 16 s | 0.4 GB |

The C++ check measured 2.2 GB and 8 minutes (E.1's measurement was 14 sources): above the day's 2 GiB rule by a little; it is the
essential step `cpp-c2j` (`:mem-gib 2`): its declaration should be 3 (W-D11).

### Found: the listed packages are not table-ready (blocks W2's step 03)

goi's table is generated and checked in four configurations (linux and tamago, amd64 and arm64); a listed package with a checker
error stops the table, hence the build. On this tree, in tamago/amd64 and tamago/arm64 (the linux ones not run, above):

- **arbace/oi, 101 errors.** Since embed-gaps' split of `ext_sys.ae` (11-04, 10 errors then) oi has grown `ext_bigarray`,
  `ext_signal`, `ext_str_re`, `ext_unix`, `ext_unix_fd`, `ext_unix_sock`, `ext_thread`, `ext_capi`. Two kinds:
  - Linux-only syscall names with no `//go:build linux` split (Mmap, Pipe2, SYS_PPOLL, SYS_SETITIMER, ForkExec, WNOHANG, SIGSTOP,
    SIGWINCH, Stat_t's Atim, the socket calls...): ext_bigarray 5, ext_signal about 20, ext_unix 8, ext_unix_fd 15, ext_unix_sock 19.
  - annotations goi's checker requires and the hand-written forms lack, in any configuration: `:val` on constants (ext_str_re's
    19 opcodes, ext_signal's poll bits), `conv` on `unsafe.Pointer(&x)`, and `oi.ae`'s `:init-order`, which lacks the new files'
    variables (baEltSize, sigWaiters, reWordLetters, sysThreads, unixErrors, sockDomains, sockTypes, sockBoolOpts, sockIntOpts).
    These fail the linux configurations too.
- **arbace/crt, 5 errors**: `spawn.go` (next's, after 3db255c5f) uses syscall.Rlimit, Setrlimit and SYS_RT_SIGACTION outside a
  Linux file; `sched.go:254` an annotation `:tag` on `1 << uint32(oparg) & 31` (the checker says int32).
- **arbace/py, 2 errors**: `tbobj.go` calls `cIntArg`, defined in `builtins_resource.go` (`//go:build linux`).

The fixes are the crops' owners' (OCaml's, C's, Python's), by day before W2 (W-D13).

### Not done here (W2's night, or later by day)

- Every build: `bin/arbace-go --build` with the embedding (W2 03), the fixpoint, `--make`, `--verify`, stage 0 from the new seed.
- `bin/j2g-edge verify` on this tree (W1's step 05/06): the embedded namespaces' JDK classes may make `jdk/edge.edn` stale.
- `bin/si interp-check` (builds scminterp with TamaGo's go), `bin/repr-ocaml embed-check`, `bin/cppi lower-check`, `make
  python-essential`: W1's step 05 and W2's step 04.
- java-night (33d04049c, `arbace.classes.annot`) is not on next yet: when it merges, `window-w2b` is refreshed onto next (by day,
  W-WINDOW risk 7), and R (§5.3 of PLAN.md) is written on `w-retire` off it.

## As built: R, the retirement commit (`w-retire`, 2026-11-09, write-ahead)

**When it applies: W2's step 11 only** (W-D6 (a)), after `bin/seed --verify` (step 09) and stage 0 from the new seed (step
10), before step 12's light checks and step 14's gate, by `git cherry-pick` of R's commit (the tip of `w-retire`) onto
`reseed-w2`. Never before the new seed exists: the old stage 0 (`d5548e72`) has neither F14's analyzer nor `arbace/classes/`
and `arbace/c/` in `RT.toolSource`, so the fixpoint's G1 (built by it) needs both stopgaps. `w-retire` is pushed to `mac`
only and merged nowhere.

`w-retire` is `mac/window-w2b` (884c3c346) with java-night (a7160b35e) merged in (a scaffold, so that the stopgaps R removes
exist in its tree; not for merging: `window-w2`'s refresh onto next by day is the real one; its resolutions: the journal's
two appends kept, java-night's first; `go/arbace/crt.ae` and `go/arbace/oi.ae`, where both sides split files by build tag,
the union of `:files` and loads, java-night's `:init-order`), then R:

- `arbace/classes/annot.ae`: 33d04049c's annot half reverted (`git apply -R` of its hunks, clean): `lenient-var`, the
  `lenient` macro and the `element-default` fallback removed, `(binding [a/*lenient* true] ...)` (three places) and
  `(a/element-default mm)` back.
- `bin/arbace-go`: 33d04049c's other half reverted: `build()`'s `classes_stale` test (and its comment) removed; the older
  `RT.ID_SPACE` test (SA143) and `host2` stay, byte for byte the text before night 10.
- `bin/lib/ci/fresh.bash` removed; its callers: `bin/ci` (sourced it and ran `c_ci_fresh` for every verb but `cirun-build`,
  `c2j-check`, `cinterp-check`, `synth`, `selfref`) and `bin/rust-front`'s ci batch session (`c_ci_fresh` before `bin/ci musl
  prepare`). `git grep` finds no other caller (steps.edn, the crops' fragments, `test/gate/audit.ae`, the Makefile, `bin/`
  and `test/` scripts: none name it; `bin/lib/fresh.bash`, the box's freshness rules, is another file and stays).
- Docs: doc/C23-SURVEY.md "As built: CS281 (a)", doc/classes/JAVA-DEFERRED-TRIAGE.md's F14 note (stage 0 and the
  stopgaps). CLAUDE.md's seed pin, its toolSource sentence and crops paragraph, AGENDA and the journal: the main session's,
  at step 11.

Checks (light, no build): `bash -n` of `bin/ci`, `bin/rust-front`, `bin/arbace-go`; `git grep` for `lenient-var`,
`classes_stale`, `ci/fresh.bash`, `c_ci_fresh`, `CI_FRESH` (only history: the journal, the surveys' past sections and
`test/c/divergences.edn`'s `:decided` text). On the old stage 0 the plain annot cannot load (`a/*lenient*` unresolved, night
10's failure): `bin/gate --plan` on a copy of this tree is expected to fail to load `arbace.classes.annot` until the new
seed. Step 11's own check: `grep` finds no `lenient-var`, no `classes_stale`, and `bin/ci` sources no `fresh.bash`; step 12
proves `arbace.classes.annot` loads on the new stage 0.

Not retired by R (left as they are, a decision for the main session): `test/gate/steps.edn`'s rsi steps (`rsi-essential`,
`rsi-check` and the AOT one) run under `env ARBACE_PATH_FIRST=1 ARBACE_NO_AOT=1 ARBACE_NO_IMAGE=1` (the same trio, for ci's
lowering run on stage 0).

Replayed 2026-11-11 as `w-retire-3` onto `window-w2d-jn` (`window-w2d`, the embedding replayed onto next adf65b4fd, with
java-night adba65cea merged: W-D16 (a)). With the user's R (a) of 2026-11-11 it also drops the two later guarded callers of
`fresh.bash`: `bin/llvmi`'s (llvm-compl2 b60d9d0f3: `[ -f ]`, then `c_ci_fresh` for run, lower, check, modules) and `bin/rsi`'s
(79c4e60c6: `[ -f ]`, then `c_ci_fresh` for every verb but list, sets, runtime-keys), their comments naming RT.toolSource
(CS281 (a)) in its place, and `test/c/musl-lock-test.bash`'s comment. `bin/rust-front`'s session line went with R's own hunk
(its context, next's, applied cleanly). `git grep -n 'ci/fresh.bash' bin/` then finds only history comments (bin/ci, llvmi,
rsi), no `.` line and no `c_ci_fresh`; `bin/lib/fresh.bash`, the box's, is another file.

Replayed 2026-11-16 as `w-retire-4` onto `w2-1116` (`window-w2e`, the embedding replayed onto B2's `b2-1115` with W1, and
embed-gaps' survey texts). One more caller had come since: cpp-threads (946a9bff4, 2026-11-15) built C++'s node programs through
`bin/lib/cppi/ci-fresh.bash`, which sourced `fresh.bash` unguarded and wrapped them in `c_ci_fresh`, and
`test/cpp/selftest/tls.sh` called `c_ci_fresh` for a build control under `CI_FRESH=0`. R now makes `cppi_ci` a plain run (kept as
a function for its five callers) and retires tls.sh's build control (the host's ci is the checkout's after the seed); the judge's
control stays. `doc/C23-SURVEY.md`'s conflict: both sections (gf-misc's night 12 C failures, then CS281 (a)). The proof on
`w-retire-4`: `git grep` finds no `lenient-var` outside the docs, no `classes_stale`, no `c_ci_fresh` call and no
`lib/ci/fresh.bash` source line in `bin/`, `test/` or `tools/` (comments only); `bash -n` of every script it changes.

## Proposed (2026-11-09, D-W2a)

- **W-D10 The embedded crops and the planner's `:tools`.** `:tools` (119 uses) is "the checkout's namespaces a tool on stage 0 loads
  instead of its embedded copy"; it excluded the crops because they were not embedded. embed-crops (0c711daa6) and 214 (101c0d032)
  removed the exclusions with the embedding.
  - (a) Keep the crops out of `:tools` (as built): a crop's namespace is loaded only by its own steps, which declare its sources
    (`:cpp`, `:llvm`, `:scheme`, `:pyi`, `:python`); the audit catches a step that loads one undeclared. A crop commit after W2 then
    reruns the gate's builds and the `:gate`/`:prog` steps (as decided) but not every stage-0 step.
  - (b) Put them in `:tools`, as the old commits did. Every C++ or pyi commit reruns all 119 stage-0 steps too.
  - *Recommended: (a).*
- **W-D11 `cpp-c2j`'s memory.** Measured 2.2 GB peak, 8 minutes (34 sources).
  - (a) `:mem-gib 3` (GTS215's essential limit is 3 GiB). (b) Leave 2 and let the planner's flag catch it.
  - *Recommended: (a)*, a one-line change to `test/gate/crops/cpp.edn` and steps.edn, by C++'s owner or with W2's merge.
- **W-D13 The listed packages not table-ready** (above: oi 101 errors, crt 5, py 2 in the tamago configurations).
  - (a) Their owners fix them by day before W2 (11-12): oi's Linux-only calls into `_linux.ae`/`_other.ae` files as `ext_sys`'s,
    its forms' annotations completed (`:val`, `conv`, `:init-order`; the checker names each), crt's `spawn.go` split, py's `cIntArg`
    moved to a file of every build; then `bin/oi table-check` and `bin/pyi table-check` (all four configurations) on W1's night (step
    05). Cost: an OCaml day; W2 waits if they are not green.
  - (b) W2 lists ci, libm and py (after py's one-line move) and leaves oi (and crt, if not fixed) out of the table: oi stays in the
    program (copied, compiled) but unreachable through the table until the next re-seed. Cost: OC165 (a)'s oi listing slips a
    re-seed.
  - (c) goi's table learns to list a package in the linux configurations alone. Cost: a change to arbace.go.table and the box's
    configurations (parked) lose the package; untested.
  - *Recommended: (a)*, with (b) as the fallback decided now, so that W2's night does not stop on it.
- **W-D12 Types under `toolSource`.** PLAN.md risk 4 found only `arbace/scheme` defining types among the new directories, but the
  newly embedded namespaces under `arbace/repr/` define them too (`repr/cpp`'s convert, parse, scope, skim; `repr/llvm`'s read,
  verify; `repr/python`'s lex, peg, native): a changed one reloaded leaves its compiled users with the old types.
  - (a) As W-P9 (a)'s rule for Scheme: a crop step that meets it takes the PATH_FIRST trio for that change. (b) A test in
    `bin/lib/tools.bash`: when a changed embedded source defines a type, every namespace of its crop is loaded from the checkout.
  - *Recommended: (a)* now, (b) if it bites.
