# goi's by-design classes, reviewed (2026-11-05)

Brief: Go eval in the REAP matrix was 1,664 pass / 173 by design / 0 deferred (goi interpreting
`$GOROOT/test` against gc; `test/goi/divergences.edn`, record `test/goi/check-linux-amd64.edn`; AOT 1,690 / 147).
Each by-design class is read here for what is reachable, so that "by design" shrinks to what truly cannot be done.
Programs are read at TamaGo's go1.27.1 `test/`. Costs are days of one person's work, rough.

## Findings in one table

Built on this branch (`go-design-review`, sample checks only, see "Built"): 5 programs, 1,664 -> 1,669 pass,
173 -> 168 not passing, 16 -> 14 classes.

| class (goi, at the start) | n | verdict | reachable by | programs | days |
|---|---|---|---|---|---|
| gc's optimizer diagnostics and flags | 98 | by design for an interpreter; **passed at the AOT level already** | gc itself (AOT: yes; goi: "gc in the executable", Proposed 1) | 98 (+7 below) | 5-7 for goi |
| -race | 5 | **reachable, built**: 4 expect no error; 1 (`issue24651a`) is `-m -m` | goi feature (flag accepted) | 4 | 0.2 (done) |
| gc compiling std or the runtime | 7 | 3 reachable by a flag in the front-end port (`-std` x2, `-complete`); `-+` x3 and `issue79258`'s are gc's back end | goi feature (3), gc (4) | 3 | 1-1.5 |
| errors of gc's phases after type checking | 7 | "stack frame too large": a back-end fact | gc only; no honest heuristic | 0 by port | gc: see 1 |
| the toolchain's root | 1 | **reachable, built**: not by design, the run lacked `GOROOT` in its environment | goi harness | 1 | 0.1 (done) |
| speed beyond the check's 10 s | 6 | a check budget, not a limit of goi: per-program limits as `test/goi/timeouts.txt` already gives | harness | 6 | 0.5 (measured: see below) |
| S23's call depth | 1 | a default (100,000), not a design limit | `ARBACE_GOI_DEPTH` + `debug.SetMaxStack` in program mode | 1 | 0.5-1 |
| finalizer timing, escape analysis, memory statistics, heap profiles | 17 | split: 8 liveness (finalizers, stack objects), 2 `uintptrescapes`, 7 allocation counts/profiles | dead-slot clearing (liveness) 10; allocation: by design 7 | 10 | 4-6 |
| runtime.Caller etc. at positions within a line | 3 | reachable if the forms keep columns | `:full` forms in the interpreter (GC49 (b)) | 3 | 3-5 |
| line directives | 1 | the same: a `/*line` sets a column | `:full` forms in the checker | 1 | in GC49 (b) |
| message spellings, columns, lines the forms do not keep | 9 | 7 reachable by errorcheck over `:full` forms or types2 on gc's tree; 2 need types2 | GC49 (b) | 7 | 3-4 |
| syntax errors types2 reports on syntax's tree | 2 | reachable by types2 on gc's syntax tree (the "rest of types2") | port / gc front end | 2 | 3-5, with 1 |
| standard packages out of scope (weak, unique) | 2 | `weak.go` is an errorcheck: the checker only needs the declarations; `issue68415` needs `unique` and checkptr | checker import for `weak` | 1 (2) | 0.5 (+2) |
| foreign code (assembly, cgo, linkname) | 9 | **by design** at goi; passed at AOT except `issue9608` | none for goi | 0 | - |
| unsafe layout and memory order | 2 | `issue8606b`: reachable (gc's comparison order); `nilptr`: by design | goi feature | 1 | 0.5-1 |
| gc's own errors/run no longer pass the test | 3 (+0 run) | **truly by design**: no reference | - | 0 | - |
| a program of another architecture, the box's console | 0 / 1 (box rows) | by design | - | 0 | - |
| gc flags, run (`converthash`, `maymorestack`) | 2 (in the 98 count? no, in "known") | reachable: two goi features | goi | 2 | 2 |

(The table's 98 is the class `gc-flags` as counted by `bin/goi divergences`: it holds the errorcheck programs with
`-m`, `-l`, `-live`, `-N`, `-wb`, `-d=...` and the two run programs with gc flags known.)

## The fact that changes the question: AOT already passes 156 of the 168

Of the 168 programs not passing at the goi level, **156 pass at the AOT level**, where gc is the back end (the forms
printed, `go build`, gc's own diagnostics: escape2, prove, live2, nowritebarrier, stackobj, peano, fixedbugs/issue27518b,
issue8606b, maymorestack, convert5, closure all pass there). Only 12 fail at both:

- `devirtualization_nil_panics`, `fixedbugs/issue22662`, `issue29504` (positions within a line: the forms keep
  lines, not columns);
- `fixedbugs/issue18331`, `issue20233`, `issue20245`, `shift1` (gc's own result is not the test's: stale tests);
- `return.go`, `fixedbugs/issue50372`, `slice3err.go`, `weak.go` (the forms of an invalid program);
- `fixedbugs/issue9608` (`go build -complete`; see AOT below).

Conversely the AOT level's 147 are 137 invalid programs with no forms to print, and 10 others, of which 135 of the
147 pass in goi. So "Go eval" as a pair (goi, AOT) is, but for those 12, complete already. What the by-design
classes of the goi cell say is what an *interpreter* cannot do: gc's phases (98 + 7 + 7), the collector's and
allocator's observable behaviour, assembly. They are tests of the compiler, and the compiler is what AOT uses.

## Class by class

### gc's optimizer diagnostics and flags (98 at the start, 99 now)

All errorcheck: `-m` 55, `-m=2` 4, `-l` 9, `-live` 5, `-N` 3 (`escape2n`, `issue17318`, `nilcheck`), `-d=ssa/...` 11
(prove, loopbce, checkbce, phiopt, fuse, known_bits, intrinsic_atomic, opt_branchlikely, devirt, ...), `-d=nil` 2,
`-d=wb` 4, `-d=checkptr`, `-d=typeassert`, `-d=tailcall`, `-d=append,slice`, `-d=escapedebug`, `-smallframes`,
`-live -wb=0`, plus `issue24651a` (`-race -m -m`). The expected text is what gc's escape analysis, inliner
(`-m=2` prints each cost), SSA passes and liveness say.

Verdicts:
- **Port goi's own** (an escape analysis on goi's checked forms): not reachable honestly. `cmd/compile/internal/escape`
  works on `ir.Node` after typecheck, unified IR's reader, the inliner (`-m` prints inlining decisions beside
  escapes), devirtualization and, for `-d=ssa/*`, the whole SSA pipeline. A re-implementation is months and
  would be judged only by whether its text equals gc's. Not recommended.
- **gc itself**, vendored already as forms (`tamago/cmd/`, S9: 105 packages, built byte-identical to the originals by
  `bin/toolchain-vendor check`): list `cmd/compile`'s packages in goi's table (compiled into the executable as
  go/parser and gc's syntax are since GP7), a driver (`go/arbace/gcrun`) calling `gc.Main` for amd64 in a child
  process of the executable (gc ends with `os.Exit`), `bin/goi`'s `check_err` running it with the program's flags in
  place of goi's checker, the imports' export data from the reference's `importcfg` (`$gcwork/importcfg`: the std
  `.a` files of the toolchain already used for gc's reference runs; or gc compiling the few imports from forms,
  slower). Everything under this class and `-+`/`-std`/`-complete`/stack frame errors then passes, but **by gc, not
  goi's checker**, which is what the AOT level already says. It unlocks about 98 + 7 + 7 + `issue79258` + 2 run programs
  = ~115, cost 5-7 days (table inclusion and build issues 2, the driver and the child process 1-2, the flags and
  `errorcheckwithauto` 1, the record, the class and the gate 1). The question for the user is whether this counts
  for the goi cell: see Proposed 1.
- **By design for goi** (the interpreter), as now, and reached at AOT: recommended unless Proposed 1.

### -race (5, now 1 inside gc-flags)

`issue13265`, `issue15091`, `issue16008`, `issue17449` have no `// ERROR` line: they are compiler-crash
regressions, and the test expects gc to accept the program with `-race`. Race instrumentation adds code and no
errors for a valid program, so `-race` is a flag goi accepts (gc's reference run passes it too). **Built**; four pass.
`issue24651a` is `-race -m -m`: inlining text, in gc-flags.

### gc compiling std or the runtime (7)

- `-std`: `issue18331` (`//go:unknown is not allowed in the standard library`, `nowritebarrierc`, `noesape`),
  `uintptrkeepalive` (`go:uintptrkeepalive requires go:nosplit`): noder's pragma handler under `base.Flag.Std`
  (noder.go:333, 348, 351), already ported in `go/arbace/repr/go/gcsyntax/errors.ae` for a non-std package. A `std`
  switch plumbed `bin/goi` -> `arbace.go.run --gc-front` -> `arbace.repr.go.syntax` -> the driver: 1 day for 2
  programs. At the AOT level `issue18331` is a stale test (gc's own errorcheck fails): it stays failing in the pair.
- `-complete`: `issue48097`: writer.go:1175 `missing function body`; the same plumbing, 0.5 day, 1 program. (`decl-errors`
  already has the `funcs` walk; only the `Complete` condition and the body-less check are new.)
- `-+` (`issue14999` heap-allocated closure; `nowritebarrier`; `inlinegcpc`) and `issue79258` (`println([1]byte{})`,
  `illegal types for operand: println`: walk's error): gc's back end; with Proposed 1 only.

### errors of gc's phases after type checking (7)

`bug385_64` (a function with 16,480 variables), `issue20529`, `issue20780`, `issue22200`, `issue22200b`, `issue25507`,
`issue52697`: "stack frame too large" (> 1 GB), decided by gc's back end after the stack allocator and the `-d=...`
move-to-heap rules (a `range` over an array copies it; results always spilled; large `make` goes to the heap). An
estimator over the forms would be tuned to these seven programs and wrong elsewhere: not recommended. Reachable by gc
(Proposed 1). The AOT level passes all seven.

### speed beyond the check's 10 s (6, plus `gc2` which also names speed)

`abi/fibish`, `abi/fibish_closure`, `abi/uglyfib`, `divmod`, `fixedbugs/issue78081` (32 s), `fixedbugs/issue79186` (41 s).
These pass in goi given time; only the check's 10 s wall limit stops them. `test/goi/timeouts.txt` exists for exactly
this (the user, 2026-10-24: "as Go's own test runner gives slow tests more time"). Built: limits of 150 s for four (`issue79186`, `issue78081`, `divmod`, `uglyfib`), which pass in the check (244 s together
at one job); `abi/fibish` and `fibish_closure` (f(40), hundreds of millions of calls) not measured, still known. This is not a limit of goi, so it leaves "by design". Cost: the nightly check's wall time (the six can
run in parallel with the rest at `--jobs 8`).

### S23's call depth (1)

`peano.go`: `fact(9)` in Peano numbers is 362,880 frames deep; goi's limit is 100,000 by default (`ARBACE_GOI_DEPTH`),
a protective default so that Go's fatal stack overflow does not end a REPL (S23). GC42 (b) already described the
fix: a higher default in program mode (1,000,000) with `debug.SetMaxStack` raised, as gc's 1 GB stack holds the
frames. Memory is the risk (the interpreted call's Go stack is larger than gc's frame; 362,880 deep might need
about 1-2 GB): measure first. 0.5-1 day.

### finalizer timing, escape analysis, memory statistics, heap profiles (17)

Read one by one:
- **Liveness (reachable, about 10 programs)**: `issue27518b`, `issue32477`, `issue46725`, `issue54343`, `issue57823`,
  `typeparam/chans`, `typeparam/chansimp` (a finalizer must run when gc's liveness says the variable is dead, which
  goi's frame slots, alive for the whole call, delay), `stackobj`, `stackobj3` (stack object liveness),
  `finprofiled` (finalizers and a profile; partly). A goi feature: per-function liveness over the checked forms
  (last use of each local, loops and gotos by a dataflow over the statement tree) and "clear this slot" nodes after the
  last use of a slot holding a pointer, as a Java interpreter clears dead frame slots. 4-6 days, a speed risk
  to measure on the essential subset. Unlocks 7 finalizer programs with confidence and 2-3 more less so.
- **`uintptrescapes` (2)**: `issue24491a`, `issue24491b`: a pointer converted to `uintptr` in a call's arguments of a
  `//go:uintptrescapes` function must stay alive until the call returns. A goi feature: the builder keeps the converted
  pointers in hidden frame slots for the call's duration. 1-2 days, partly shared with the liveness work (the
  same "keep alive" vs "clear" machinery).
- **Allocation counts and profiles (7)**: `closure`, `issue4618` (`testing.AllocsPerRun` == 0 for non-escaping closures),
  `issue15281`, `gc2` (`ReadMemStats` after allocations; `gc2` also speed), `heapsampling` (profile samples of gc's
  allocation sites), `finprofiled`: they observe gc's escape analysis and allocator. **By design** at goi, passed at AOT.

### runtime.Caller, Callers, FuncForPC and traces at positions within a line (3), line directives (1), spellings (9)

The same root: mode `:lines` keeps statements' lines and no columns (SPEC.md §10.1). The three frames programs
(`devirtualization_nil_panics`: a call split over two lines, gc's operator line; `issue22662`, `issue29504`: `/*line`)
and the checker's seven (`issue19947`, `issue56220`, `issue31747`: a literal's spelling; `issue68292`: a column;
`issue23732`: a closing brace's line; `funcdup2`: a type spanning lines; `return.go`: a final expression statement;
the line-directives program `issue24339`) are all about what mode `:full` keeps (text, spellings, layout, positions
printed back byte for byte). GC49 (b) described it: the checker and the interpreter reading `:full`'s forms. It is a
cross-cutting change (memory of the forms, the builder's positions, the checker's messages) of 3-5 days for the frames
and 3-4 for the errorcheck seven; it is also what the AOT level's three `positions` programs need (the printer
from `:full` forms with `//line` directives). Rank below the cheap items. Not by design: by an unbuilt layer.

### syntax errors types2 reports on syntax's tree (2)

`issue50372` (`range clause permits at most two iteration variables`), `slice3err` (3-index slice of string): types2's
errors on the tree gc's syntax makes and go/parser rejects. With gc's syntax already run by goi (GP6) and types2 in
`tamago/cmd`, the two programs are reachable by running types2 (only) on syntax's tree as the front end of check_err,
3-5 days if done alone; free with Proposed 1.

### standard packages out of scope (2)

`weak.go` is an *errorcheck* whose only subject is a conversion between `weak.Pointer[string]` and `weak.Pointer[*byte]`
(a type error): the checker needs `weak`'s declarations, not an implementation; goi refuses the import at load
(GR8a). Letting the checker import `weak` (and `unique`) for errorcheck only: 0.5 day, 1 program (AOT cannot print
the invalid program: no forms). `issue68415` is a run program (`-d=checkptr`, `unique`): an implementation of
`unique.Make` over goi's values (a map keyed by value) is a day, and checkptr is one more goi feature
(alignment and straddle checks on `unsafe.Pointer` conversions; 6 more programs' flag `-d=checkptr`
are skipped or pass already): not for 1 program. Weak pointers themselves (`weak.Make`) need the runtime's.

### foreign code (9), unsafe layout (2)

Assembly (`retjmp`, `linknameasm`, `asmhdr`, `issue37513`, `issue74648`, `issue15609`, `issue9608`) and linkname into gc's
symbols (`noinit`, `issue42401`): goi interprets Go forms; an amd64 assembler/emulator in the interpreter would
test the emulator, not Go. **By design**; AOT passes all but `issue9608`. `nilptr`: the data segment's layout of
huge arrays (an address in unsafe arithmetic): by design. `issue8606b`: `==` of interface values holding structs
compares fields in gc's generated order (string lengths first, so as not to fault on a protected pointer); goi's
equality follows field order. A goi feature: compare in gc's chunk order (`cmd/compile/internal/compare`): 0.5-1 day.

### gc flags that change run programs (2 "known")

`convert5` (`-d=converthash=qy`): the saturating float-to-int conversions selected by a bisect hash of positions: goi
implements gc's default; the flag needs `bisect`'s matcher over (file:line) and a second conversion semantics (an
existing helper in `runtime`/`math`?), 1-2 days; `maymorestack` (the function prologue calls the hook): a flag in the
builder that calls the named function at each non-`nosplit` function's entry: 1 day. 2 programs, 2-3 days: low rank.

### truly by design (what is left)

- gc's own errors no longer pass the test (`issue20233`, `issue20245`, `shift1`; AOT adds `issue18331`): the reference
  fails; there is nothing to judge.
- Assembly and linkname (9) at goi; the box's console (1 per box row); a program of another architecture.
- Allocation counts, memory statistics, heap profiles (7).
- With nothing built: the rest only if their layer (liveness, `:full` forms, gc in the executable) is built.

Floor if every reachable item is built: about 20 programs at goi (9 foreign, 3 gc-reference, 7 allocation observations,
`nilptr`), against 173 at the start.

## The AOT level (147)

- 137 "invalid programs without forms": the original is not a Go program (go/parser rejects 73; the converter cannot
  convert 64); AOT prints the forms, so there is nothing to print. By design in the definition of the level: its
  count could be reported as "not applicable" beside the pass count (Proposed 2). Reach: GC47 (b), forms for
  invalid code (a selection with no object, a conversion of several arguments): weeks, per construct.
- `go build's -complete` (`issue9608`): `go build` compiles with `-complete` a package without a `.s` file;
  `arbace.build.go` can place an empty `.s` in the module (the go command then omits `-complete`): 0.5 day, 1 program.
- `toolchain` (`winbatch`): the run's `GOROOT` was missing at both levels' runs; with this branch's `run_env`
  (see "Built") it should pass at AOT as well: unverified (no AOT build run today), the known line of
  `test/goi/check-aot-known.edn` to be removed after a run.
- `positions` (3) and `spellings` (1): as for goi's frames and `return.go`.

## Ranked plan

By programs per day, then by what they build for:

1. (done) `-race`: 4 programs, 0.2 d. `GOROOT`: 1, 0.1 d. (5 programs)
2. Speed budgets: 6 programs, 0.5 d (measured here).
3. `-std` and `-complete` plumbing: 3 programs, 1 d (`gcsyntax` already has the pragma handler).
4. `weak` import for errorcheck: 1 program, 0.5 d. AOT `-complete` empty `.s`: 1 program, 0.5 d.
5. Call depth for program mode: 1 program, 0.5-1 d (memory to measure).
6. `issue8606b`'s comparison order: 1 program, 0.5-1 d.
7. Liveness (dead-slot clearing) + `uintptrescapes`: ~10 programs, 5-8 d: the largest real goi feature.
8. `:full` forms in the checker and interpreter (GC49 (b)): 3 + 7 + 1 programs, 6-9 d, and AOT's `positions`.
9. gc in the executable (Proposed 1): ~115 programs, 5-7 d, which also gives types2's two (`no-forms`) and
   the 7 stack-frame programs.
10. `converthash`, `maymorestack`: 2 programs, 2-3 d.

## Built (this branch)

- `bin/goi` `check_err`: `-race` goes to the reference's flags and goi's checker judges as for no flag. The four
  programs without `// ERROR` pass (`bin/goi check --only '^fixedbugs/issue(13265|15091|16008|17449|24651a)\.go$'`: 4 pass,
  1 unsupported (`-m`), no regression; 2.5 minutes with the corpus reads of a first run, 2 s of checks). The class `race`
  of `test/goi/divergences.edn` is removed (empty), `issue24651a`'s reason is `-m`'s.
- `bin/goi` `run_env`: `GOROOT="$goroot"` beside Go's test driver's variables (the driver's `go env` sets it for its
  children): `winbatch.go` passes (`bin/goi check --only '^winbatch\.go$'`: 1 pass). Its line of `test/goi/check-known.edn` and the class
  `toolchain` of `divergences.edn` are removed; the record `test/goi/check-linux-amd64.edn` has the five lines changed.
  `bin/goi divergences`: 1,669 pass, 168 in 14 classes. Not run: the full check (the keyed gate on main); a program
  whose output prints `GOROOT` would change, none is known.
- `test/goi/timeouts.txt`: 150 s for `fixedbugs/issue79186`, `issue78081`, `divmod`, `abi/uglyfib`: `bin/goi check --only` of the four: 4 pass (244 s). Their known lines are removed and the record has them as pass. `fibish` and `fibish_closure` not tried (likely minutes).
- Totals: `bin/goi divergences` linux/amd64: 1,673 pass, 164 not passing (1,664 / 173 at the start).

## Proposed

1. **Run gc in the executable for the errorcheck programs of gc's phases (the biggest unlock).**
   (a) Not: keep the 98 + 7 + 7 by design at goi, reached at AOT (cost 0). (b) Build it: gc's `cmd/compile` packages (from
   `tamago/cmd/`, as forms) in the goi table, a `gcrun` driver in a child process, `check_err` running it for the
   programs whose flags goi does not have (5-7 days, ~115 programs, the export data from the reference's importcfg).
   Cost beyond days: the executable grows by gc's size (about 20-30 MB stripped), `gate_key` takes the packages, and the
   cell says "gc judged by gc, from forms". (c) As (b) but as a separate cell/level ("gc front/back end on forms")
   rather than the goi cell. *Recommended: (a) now, then (c)*: those programs are tests of the compiler; the AOT level already is
   gc's verdict on forms, and goi's cell then means what an interpreter can do. (b) only if the user wants the goi number
   itself to rise.
2. **Count the 137 invalid programs of the AOT level as not applicable.** (a) as now, by design; (b) "n/a" beside the
   pass count in the matrix (they are not programs); (c) GC47 (b), forms for invalid code (weeks). *Recommended: (b).*
3. **Reclassify "speed" (6) from by design to the check's budget.** (a) as now; (b) per-program limits in
   `test/goi/timeouts.txt` (built) and the class removed once the six pass in the check. *Recommended: (b).*
4. **Fund liveness (item 7)**: (a) not; (b) yes, 5-8 days for ~10 programs and a precise-GC property for goi. *Recommended: (b)*
   after the cheap items and before `:full` forms; measure the interpreter's speed on the essential subset first.
5. **`:full` forms in the checker and interpreter (GC49 (b))**: (a) not; (b) yes, 6-9 days, ~12 programs and AOT's positions.
   *Recommended: (b) after 4*, the larger change; the checker's errorcheck seven first.
6. **Reclassify the by-design list to the floor** (about 20: foreign code, gc's stale tests, allocation observations,
   `nilptr`), the rest "deferred, with the item above" until built: the matrix's "0 deferred" then rises, which is the honest
   state if Proposed 1 (a) is chosen. *Recommended*: do it as items land, class by class, not at once.

## Decided (2026-11-05, the user)

All six proposals as recommended: (1) gc's flag-diagnostic programs by design for goi, a gc-on-forms cell later; (2) the AOT level's invalid programs reported as not applicable; (3) speed as a check budget; (4) liveness funded; (5) :full forms after it; (6) the by-design list shrunk class by class.

## As built: Proposed 2 and 3 (2026-11-05)

- **Two new kinds of class** in `test/reap/main.ae` (`kind-of`, `judge-cell`, `cell-text`, the matrix record and its diff):
  `:na` (not applicable: the case is no program of the level) and `:budget` (over the check's time budget). A cell's
  numbers stay "pass / by design / deferred"; `:na` is printed beside the cell and is outside its denominator, `:budget`
  is printed beside it and inside it. `test/reap/matrix.edn` records both counts per cell (`:na`, `:budget`).
- **Proposal 2 (b)**: the class `no-forms` of `test/goi/divergences-aot.edn` is `:kind :na` (its text says it is not a
  divergence). Go's AOT cell before: 1,690 / 147 / 0. After: 1,690 / 10 / 0 (137 n/a, not in the denominator), 1,700
  programs counted of 1,837. `bin/goi divergences --aot` prints the class as `(na)`; 1,837 programs, 1,690 pass, 147 in 6
  classes, the essential subset 0 not passing.
- **Proposal 3 (b)**: the class `speed` of `test/goi/divergences.edn` is `:kind :budget`, retitled "programs over the
  check's time budget"; a program leaves it when its limit in `test/goi/timeouts.txt` lets it pass (the four fixed by the
  branch's earlier work are gone; `fibish` and `fibish_closure` remain, untried at a longer budget). Go's eval cell:
  1,673 / 162 / 0 (2 over budget, in the denominator) (it was 1,664 / 173 / 0 at the previous matrix record: the
  earlier work's `race`, `toolchain` and four speed programs, now recorded).
- Checked by records alone: `bin/reap` 0 problems after `bin/reap --record` (the matrix record had been stale since the
  earlier commits of this branch); `bin/goi divergences --aot` and `bin/goi divergences` pass.
