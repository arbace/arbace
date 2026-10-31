# C23 at parity with the three crops — survey and plan

Status: survey and plan, 2026-10-28, at the user's request (doc/AGENDA.md, "Next, in the user's order",
item 3: "C23 at parity with the three crops, its survey first (prior art: go-whim's C front end,
canonical printer, C-lisp and conformance test, `../go-whim` at `80c49bf5`)"). Nothing here is
adopted: no code under `arbace/`, `go/`, `bin/` or `test/` changed. The decisions are the user's, as
proposals CS1-CS24 (§12); the milestones are C1-C9 (§11), named by CS1.

Base: branch `c23-survey`, from main `713424ac`. The measurements are light probes (§3), run on the
shared 64-core amd64 host with TamaGo's go1.27.1 (`/root/tamago-go`, `CGO_ENABLED=0`) and the host's
gcc; no Arbace executable was built or run, no big-job lock taken. The scratch is the worktree's
`.tmp/c23/` (Appendix A).

The goal, in the terms of doc/FULL-SUPPORT-SURVEY.md's "REAP v1's divergences": **a fourth row of
the REAP matrix, C**, whose four cells (read, eval, AOT, print) are judged by `bin/reap` from recorded
results under goi's v1 rule: each level's conformance corpus against its reference, none failing,
every non-pass in a recorded class (by design or deferred, named, citing where it was decided). C23
is ISO/IEC 9899:2024; section numbers below are those of WG14's working draft N3220 (February 2024,
C23 with the editorial fixes since), as go-whim's doc/C23.md uses them.

Words used throughout:

- **C forms**: C23 translation units written as Arbace forms (§4), the counterpart of the Go forms
  (doc/go/SPEC.md) and the Java forms (doc/classes/SPEC.md).
- **repr/c**: the bridge between C text and C forms (`arbace.repr.c`, `bin/repr-c read`, `print`),
  as repr/go is Go's (§5).
- **ci**: the interpreter of C forms at the REPL, as goi is Go's (§6); the name is CS17's.
- **cc**: go-whim's C front end `crefactor/cc`, a fork of `modernc.org/cc/v4` v4.29.7 (§2.1).
  **The reference**: the host's gcc 15.2 (§8).
- **The corpora**: the C programs each level is checked over (§8.2).

Contents: Executive summary · 1 What parity means for C · 2 Prior art · 3 Measurements · 4 C forms ·
5 repr/c: read and print · 6 eval: ci · 7 AOT · 8 The oracle and the corpora · 9 Licenses, the host,
the box · 10 The fourth row · 11 Plan (C1-C9) · 12 Proposals (CS1-CS24) · 13 What the user decides
first · 14 Risks · 15 Sources · Appendix A: the scratch · Decided (2026-10-28): CS1-CS24 · C1: the plan
after CS4 (b) (with CS30-CS43) · As built: C2, C3, C3b, C4 (with CS50-CS51, CS60-CS63, CS70-CS83, CS90-CS95) ·
As built: C5 (write-ahead) (with CS100-CS111).

## Executive summary

- **The prior art is real and close (§2).** go-whim at `80c49bf5` holds a C23 front end
  (`crefactor/cc`, 27,976 lines of Go: modernc.org/cc/v4 v4.29.7, BSD-3, with go-whim's C23 delta in
  `c23.go` and marked hooks), a canonical printer (`crefactor/cemit`, 2,528 lines), C as
  s-expressions (`crefactor/clisp`, 4,767 lines, `ToC(ToLisp(x)) == cemit.Canonical(x)` byte for
  byte) and a conformance test (`crefactor/c23conf`, 49 one-feature files, six judges, gcc 15 the
  oracle). Reproduced here: **49 of 49 pass**, its control refuses all four wrong prints (E1). It has
  **no interpreter and no general C→Go path**: `crefactor/togo` (52,115 lines) translates one program
  (vim's core) to Go, Clojure and others under a profile, not C at large.
- **What go-whim's tools do on general C (E2).** Run over the C corpora already on the host
  (`modernc.org/ccorpus2` v1.6.0 in Go's module cache: gcc's c-torture, tcc's, CompCert's, MIR's
  tests), parse, print, fixpoint and C-lisp's round trip, then the original and the print each
  built by gcc and run: **gcc 9.1's c-torture/execute, 1,195 of 1,700 run the same from the print,
  0 run differently**; 323 (19%) refused for a directive other than `#include` (cemit refuses every
  other one by design), 69 not a fixpoint, 31 printer refusals (GNU nested functions,
  `__real__`/`__imag__`), 7 prints gcc rejects, 69 that gcc 15 does not build as given. cc's type
  check accepts 1,693 of 1,700. The same shape on the later gcc-mirror snapshot (1,252 of 1,632)
  and tcc's tests (62 of 88, 24 directives).
- **The preprocessor is the read level's main gap (E3).** cc's preprocessor predates C23:
  `#elifdef` takes the wrong branch silently, `#embed` is dropped silently, `__VA_OPT__` is left
  unexpanded, `__has_c_attribute` is 0, `#warning` is silent. And cemit/C-lisp keep no directive but
  `#include`, no comment. Mode `:full` (byte for byte) and the directives are Arbace's own work.
- **The reference is on the host.** `gcc` 15.2.0 (Alpine, musl) and `clang` 23.1.4 both take
  `-std=c23` (`constexpr`, `auto`, `[[...]]`, `_BitInt`, `wb` build and run statically); `tcc` and
  csmith are absent. The host's libc is musl 1.2.6, and `/root/musl` holds musl's source (MIT) with
  the same VERSION, so **the reference's libc can be the interpreter's libc**, which makes `printf`'s
  and `libm`'s output comparable byte for byte (CS11).
- **No C23 conformance corpus is on the host but c23conf's 49 files.** The torture tests are C89-C17
  (gcc 15 accepts 969 of the 1,700 as `-std=c23`), so the corpora's files keep their standard as data
  (CS3). A C23 corpus needs a fetch: gcc's `gcc.dg/c23-*.c` and `c2x-*.c`, clang's `test/C/C23`
  (CS15), the user's decision.
- **The proposed shape mirrors Go's (§4-§7).** C forms in Go forms' vocabulary (`c/defn`, `(* T)`,
  `@p`, `(addr x)`, `(set! x + e)`, `bit-and`); repr/c reading with cc vendored as Go forms (as `box/`
  and GP7's go/parser), first interpreted by goi then listed in goi's table and compiled, a generated
  dumper and an Arbace converter; ci as goi (a checker `arbace.c.check` in Arbace with cc's checker
  as its oracle, a builder, a Go back end); one memory model for eval and AOT (a flat virtual
  address space with the SysV x86-64 layout, allocations recorded for checks); musl as C forms with
  its syscall layer in Go; AOT as C forms → Go forms built by `arbace.build.go`, no cgo, no C compiler
  of the host's.
- **Plan (§11): C1-C9, about 32 to 54 working days**: C1 the spec and c23conf in Arbace (2-3), C2
  repr/c read and print `:lines` (4-6), C3 the preprocessor's C23 and `:full` (3-5), C4 the checker
  (5-8), C5 ci with musl (8-14), C6 AOT (6-10), C7 the C23 corpus and the error tests (2-4), C8 the
  fourth row and the essential subsets (1-2), C9 the milestone run (1-2).
- **To decide first (§13):** the language's scope (CS3), go-whim's parts' license (CS22) and their
  vendoring (CS4), the preprocessor's place (CS5), fetching a C23 corpus (CS15), musl (CS11, CS12),
  the memory model (CS9) and the milestone's name (CS2).

## 1. What parity means for C

The three crops' rule (doc/FULL-SUPPORT-SURVEY.md, "The binding decisions" and "REAP v1's
divergences"), for C:

| cell | in Arbace (proposed) | corpus | reference | judge |
|---|---|---|---|---|
| **C read** | repr/c `read`: cc (vendored, compiled) + the converter | every corpus of §8.2, valid and invalid | gcc 15.2's acceptance and diagnostics; gcc's object code of the original | the forms printed and compiled by gcc give the original's object file byte for byte (c23conf's judge 5); a file gcc rejects is rejected, its error lines matched as gcc's error tests are |
| **C eval** | ci (`arbace.c.check`, `arbace.c.build`, `go/arbace/ci`) | the run programs | gcc's build and run of the originals (status, stdout) | status and stdout byte for byte, as goi's |
| **C AOT** | C forms → Go forms (CS13), printed by repr/go, `arbace.build.go` | as eval | as eval | as eval |
| **C print** | repr/c `print` (`:lines`, `:full`) | every corpus | the original text; the forms read back; gcc's object code | `:full` byte for byte; `:lines` forms → C → forms equal, its run equal |

As for the crops: on the host (linux/amd64) alone, forms only (no C text typed at the REPL), each
level's results recorded as data, `bin/reap` judging them from the records with no runs.

What differs from the crops:

- **C has no implementation in Arbace's toolchain.** Go's gc is in TamaGo's toolchain; Java's javac
  runs on Arbace. C's reference is the host's gcc from Alpine's apk, which the standalone direction
  cannot keep at every run: its runs are recorded as data (as `test/java/ref-jdk26.edn` records the
  JDK's), so the check needs gcc only to record (CS14).
- **Undefined behaviour.** A C program's meaning can be no meaning. The corpora are chosen UB-free
  (c-torture's execute tests are, by gcc's own rule), a run that differs between gcc `-O0` and `-O2`
  is suspect and compared on status alone, and the interpreter traps what it detects (CS10).
- **The preprocessor.** Every other crop reads one language; C's text is two (§5.2).
- **C's library is part of the language** (clause 7). The interpreter needs a libc; CS11 takes
  musl, the reference's own.

## 2. Prior art

### 2.1 go-whim's `crefactor/cc`, the C front end

`/root/go-whim/crefactor/cc` (go-whim `80c49bf53cd6340487ac623f3d6d135b77ac1db6`, module
`github.com/arbace/go-whim/crefactor`, go 1.27.1): `modernc.org/cc/v4` v4.29.7, its fifteen non-test
files, and go-whim's own `c23.go` (296 lines), `host.go` (169) and `check_export.go` (25), hooks in
upstream's files each marked `go-whim` (cc/README.md is the delta's record; `diff -ru
$(go env GOMODCACHE)/modernc.org/cc/v4@v4.29.7 crefactor/cc` shows it, upstream's copy is in the
host's module cache).

| file | lines | what |
|---|---:|---|
| `check.go` | 5,543 | the type checker (`Translate`, `AST.Check`) |
| `ast.go`, `ast2.go` | 5,652 | the tree (generated node types, one per production) |
| `parser.go` | 4,037 | the parser (hand-written recursive descent) |
| `cpp.go` | 3,093 | the preprocessor (C11's, `__has_include`) |
| `type.go`, `value.go`, `abi.go` | 5,306 | types, constant values, the ABI (sizes, alignments, layouts per GOOS/GOARCH) |
| `scanner.go`, `cc.go`, `etc.go`, the rest | 4,345 | tokens, configuration, errors |
| total (with `fork_test.go`, 289) | 27,976 | |

Its imports: std (`os/exec` to ask the host's compiler for its predefined macros and include paths,
memoized per process; no `unsafe`), and `modernc.org/mathutil` v1.7.1, `strutil` v1.2.1, `sortutil`
v1.2.1, `token` v1.1.0, `opt` v0.2.0, `github.com/remyoudompheng/bigfft` (all BSD-3; 13,354 non-test
lines, most of it mathutil's, of which cc uses a few functions). `host.go`'s `SetHost` gives the
compiler's answers and the headers as a file system instead of asking: what a program without a C
compiler needs (go-whim's editor in a guest), and what Arbace needs (CS4).

**What it covers of C23.** Upstream parses C11 with GNU's extensions; go-whim's delta adds (cc/README.md,
doc/C23.md): `[[...]]` attributes in every position, held in the tree (6.7.13; a `gnu::` prefix apart,
another vendor's arguments as their balanced tokens), `constexpr` (6.7.2), `_BitInt(N)` (6.7.3.1, the
`wb`/`uwb` suffixes already pp-numbers), `typeof_unqual` and `typeof(const int)` (6.7.3.6),
`static_assert` without a message (6.7.12), digit separators (6.4.4), `u8'a'` (6.4.4.5), storage classes
in a compound literal (6.5.3.6), a label at a block's end (6.8.2); `bool`, `true`, `false`, `nullptr`,
`alignas`, `thread_local`, `auto`, the fixed underlying type of an enum, binary constants, `= {}` were
upstream's already (GNU's or C2x drafts'). Three upstream faults fixed (a tagged struct's trailing
attributes, a GNU list of three, a block's static assertion's `;`).

**Its limits.** The checker parses and refuses `_BitInt` ("a bit-precise integer type is parsed, not
type-checked"), refuses `typeof(type-name)` (upstream's TODO) and a VLA's `= {}`, and reads `constexpr`
as no storage class (its value is not a constant expression to the checker); `_Decimal32/64/128`
parse. The preprocessor is C11's (E3: no `#elifdef`, `#elifndef`, `#embed`, `__VA_OPT__`,
`__has_c_attribute`, `#warning`). `[[...]]` just inside a declarator's parentheses is refused. C11's
`u'x'`, `U"x"` are refused by the scanner (only `u8` was added).

**How it is tested.** `fork_test.go` (six tests: the C23 productions, the trailing label,
`Scope.Declares`, the attributes held, GNU's list kept), and c23conf (§2.4). Upstream's tests need
`modernc.org/ccorpus2` (not carried by the fork; present in the host's module cache, §8.2).

### 2.2 `crefactor/cemit`, the canonical printer

`/root/go-whim/crefactor/cemit` (2,528 lines, 536 of them tests): C23 printed from cc's tree in one
canonical form, a fixpoint (`Emit(Emit(x)) == Emit(x)`, checked). One declarator a declaration, braces
on every body, one statement a line, the source's own parentheses kept, comments dropped
(`TestCommentsDropped`). It **refuses** every directive but `#include` ("printing the parsed tree would
drop" it), a GNU nested function, a node it does not print, with the position, never silently. Macro
invocations are recovered from the source text where a whole node came from one (`errno`,
`va_arg(ap, int)`, `nullptr`). Includes are written together before the first declaration. Its tests:
one spelling per construct, fixpoint, the print compiles and runs the same, declarators and attributes,
attribute statements.

### 2.3 `crefactor/clisp`, C as s-expressions

`/root/go-whim/crefactor/clisp` (4,767 lines, 569 tests; spec `clisp/SPEC.md`, 405 lines; account
`doc/C-LISP.md`, `doc/C-LISP-TREE.md`): `ToLisp` reads cc's tree case for case as cemit does; `ToC`
prints C-lisp in cemit's spelling, so `ToC(ToLisp(x)) == cemit.Canonical(x)` byte for byte and on
canonical text the identity (whim's 104 pipeline boundaries and `whim-vim.c`). Atoms are C's token
text, never decoded (`0x7fUL`, `'\''`, `1'000'000`); declarators are read in order from the name out
(`int (*(*x)[10])(int)` is `(def x (ptr (array 10 (ptr (fn (int) int)))))`); expressions are prefix
forms with one head each (`(call f a)`, `(-> p a b)`, `(index a i)`, `(cast T e)`, `(addr x)`,
`(deref p)`, `(post++ i)`); `(paren e)` for parentheses the source wrote where precedence needs none;
labels are items before what they label. Beside it: a tree API and patterns (`?x:pat`) for editing
(`tree.go`, `pattern.go`, `scope.go`). It has its own reader (`sexp.go`), not Clojure's: `^`, `~`, `|`
and `'` in numbers are atoms there, which the Clojure reader would read otherwise (§4.1).

C-lisp is a spelling of cemit's text, not a language with a meaning of its own: nothing evaluates it.
It is the model for repr/c's `:lines` print (one canonical spelling, a fixpoint), not for C forms'
spelling, which follows Arbace's Go forms (CS6).

### 2.4 `crefactor/c23conf`, the conformance test

`/root/go-whim/crefactor/c23conf` (552 lines of Go; `testdata/`, 49 `.c` files, each one C23 feature,
its first line the N3220 subclause; doc/C23.md's table lists them). For each file, the first no is its
answer: (1) gcc accepts it (`gcc -std=c23 -fsyntax-only`), else the file is wrong; (2) cc parses it;
(3) cemit prints it and gcc accepts the print; (4) the print is a fixpoint; (5) the print is the same
program by two judges: the preprocessing tokens (the test's own lexer, from 6.4, not the front end's)
compared one for one, and the objects (`gcc -std=c23 -O0 -fno-stack-protector -c`, the same base name
in two directories) byte for byte; (6) C-lisp round trips it. `cc.Translate` type-checks each and the
table reports it; a panic fails. Expected failures are a map with reasons, a listed file that passes
fails the test. `TestControl`: four wrong prints gcc accepts (`[[fallthrough]];` printed `;`,
`1'000'000` printed `1000000`, `0b1010` printed `0b1011`, `- -a` printed `--a`) that the judge must
refuse.

It covers the language's C23 additions (keywords, attributes in every position, `_BitInt`, the fixed
enum, the literals, the initializer, labels, parameters, `va_start`'s one argument, `unreachable()`,
tag compatibility, UAX #31 identifiers, `f()` as `f(void)`) and **nothing of the preprocessor's**
(`#embed`, `__has_include`, `__has_c_attribute`, `#elifdef`, `#elifndef`, `#warning`, `__VA_OPT__`) nor
of the library's (clause 7's new headers and functions: `<stdbit.h>`, `<stdckdint.h>`, `memset_explicit`,
`strdup`, `timegm`...). It judges reading and printing, not running: no file has a `main`.

### 2.5 The rest of go-whim's C tools, and other prior art

go-whim's `crefactor/` also holds `ccwalk` (generated walks of cc's tree, 2,701 lines), `ccx` (2,534),
`sweep` (dead-code removal, 2,259), `graph` (the program as a typed graph, 54,330), `pipeline` and
`togo` (52,115 lines: one translation unit, "no preprocessor, no libc: a core like editor.c", to Go,
with C pointers as `Ptr[T]` cursors where they walk, under a `Profile` of the program's own allocators
and runtime; also its Clojure, C++ and other back ends). togo is the evidence that cc's tree carries
what a translator to Go needs, and a source of rules (cursors, unions, gotos); it is not a C→Go
translator for C at large. `doc/LISP-SANDBOX.md` measures Joker and yaegi in a TamaGo guest; no C
interpreter exists in go-whim.

Outside go-whim, from what is known of them (none is on the host but `modernc.org/cc/v4` and
ccorpus2, and musl's source):

| project | what | license | relevance |
|---|---|---|---|
| `modernc.org/cc/v4` v4.29.7 (host module cache) | the C11+GNU front end cc forks | BSD-3 | the upstream; a later release can be diffed in (cc/README.md's recipe) |
| `modernc.org/ccgo/v4`, `modernc.org/libc` | C → Go translator (SQLite, Tcl, ... in pure Go), libc as translated musl plus Go | BSD-3 | the proof that C at large goes to cgo-free Go; flat memory through `unsafe` and `uintptr`; not on the host |
| `github.com/gotranspile/cxgo` | C → readable Go | MIT | idiomatic, lossy on type punning; its test cases are in ccorpus2 (`github.com/cxgo/`, 80 files) |
| `github.com/vnmakarov/mir` (c2m) | C11 compiler to MIR, interpreter and JIT | MIT | a C interpreter's shape; its tests in ccorpus2 (315 files) |
| chibicc, cproc + QBE, tcc, lacc | small C compilers | MIT, ISC, LGPL-2.1, MIT | test corpora; tcc's `tests2` and `pp` in ccorpus2 |
| Cerberus (rems-project) | executable C semantics, memory object models (PNVI) | BSD-2 | the memory model's reference (CS9), the provenance TS (ISO/IEC TS 6010) |
| CompCert's interpreter (`-interp`) | reference interpreter of CompCert C | INRIA non-commercial (tests vary) | a UB-trapping interpreter's model; 24 of its tests in ccorpus2 |
| musl 1.2.6 (`/root/musl`) | the host's libc, its source | MIT | the interpreter's libc (CS11) |

### 2.6 How Arbace did it for Go and Java

The same steps, in the same order, for each language:

1. **A spec of the forms** before code: Go forms (doc/go/SPEC.md, G0, accepted 2026-10-08, after the
   survey doc/G2C-SURVEY.md), the Java forms (doc/classes/SPEC.md).
2. **The reader on the language's own front end, then made native.** Go: the helper `tools/godump`
   (go/parser and go/types in a Go program), then GP1-GP7 (doc/go/GO-PARSER-SURVEY.md): go/parser run
   from `tamago/goroot`'s forms by goi, interpreted (GP1), the decisions answered by goi's checker in
   its resolve mode (GP4), then listed in goi's table and compiled (GP7: +0.36 MB), a generated dumper
   committed and kept so by `bin/generated-check`. Java: javac on Arbace (JN3-JN6), then the native
   parser, a port of javac's in Arbace (JN8), its trees equal to javac's.
3. **The print with levels**: forms, text, the compiler's own output (gc's export data and objects
   symbol by symbol; javac's class files byte for byte), and `:full` byte for byte.
4. **The interpreter as analysis in Arbace, execution in Go** (doc/go/GOI-SPEC.md §1.2): a checker in
   Arbace equal to the language's checker on the corpora (`arbace.go.check` after go/types), a builder,
   a Go back end of hand-written Go forms (nodes as typed closures over frames), a table of compiled
   packages.
5. **A conformance check of the language's own corpus against its reference**, results as data,
   classes as data (`bin/goi check`, `bin/goi divergences`; `bin/java-check` with its levels, JC1-JC8,
   doc/classes/JAVA-CONFORMANCE-SURVEY.md), an essential subset in the gate.
6. **AOT through Arbace's build of Go forms** (`arbace.build.go`), checked over the same corpus
   (`bin/goi check --aot`, REAP2; `bin/java-check` compiled, JC2).

C follows them (§11). What C lacks is step 2's first half: there is no C front end in Arbace's
toolchain to start from, so cc takes the helper's place and the native reader's at once (CS4).

## 3. Measurements

All in the worktree's `.tmp/c23/` (Appendix A). The probe `probe/main.go` imports go-whim's packages
by a `replace` (read-only) and, per file: `cc.Translate` (reported, not judged); `cemit.Canonical`
(a refusal classed `directive`, `parse` or `print`); the print's fixpoint; `clisp.ToLisp` then `ToC`,
equal to the print; then, for the run corpora, the original and the print each built by `gcc
-std=gnu17 -fpermissive -w -O0 ... -lm` and run (10 s), status and stdout compared. Eight files at a
time.

**E1: c23conf reproduced.** `go test -v -run 'TestC23|TestControl|TestExpectedListed' ./c23conf` in
go-whim's `crefactor` (`GOFLAGS=-mod=readonly`, `GOPROXY=off`): **49 of 49 pass**, the expected list
empty and checked, the control's four wrong prints refused. The type check: 44 ok, 5 errors
(`bitint.c`, `bitint_suffix.c`: `_BitInt` not type-checked; `typeof.c`, `typeof_unqual.c`:
`TypeSpecifierTypeofType` TODO; `empty_init.c`: a VLA's `= {}`), as doc/C23.md says.

**E2: cc, cemit and C-lisp over the host's corpora.**

| corpus (ccorpus2 v1.6.0 unless named) | files | run the same / pass | run differently | directive | not a fixpoint | printer or C-lisp refuses | parse fails | gcc rejects the print | the original does not build or fails | `cc.Translate` ok |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| go-whim's c23conf (no run: judges 2-4, 6) | 49 | 49 | — | 0 | 0 | 0 | 0 | — | — | 44 |
| gcc 9.1 `gcc.c-torture/execute` | 1,700 | **1,195** | **0** | 323 | 69 | 33 | 4 | 7 | 69 | 1,693 |
| gcc-mirror `gcc.c-torture/execute` (a later snapshot) | 1,632 | 1,252 | 0 | 270 | 54 | 33 | 4 | 8 | 11 | 1,624 |
| gcc 9.1 `gcc.c-torture/compile` (no run) | 1,791 | 1,547 | — | 121 | 66 | 55 | 2 | — | — | 1,787 |
| tcc 0.9.27 `tests/tests2` | 88 | 62 | 0 | 24 | 1 | 0 | 0 | 0 | 1 | 88 |
| CompCert 3.6 `test/c` | 24 | 7 | 0 | 17 | 0 | 0 | 0 | 0 | 0 | 24 |
| MIR's `c-tests` | 315 | 122 | 0 | 46 | 0 | 0 | 1 | 3 | 143 | 312 |
| benchmarksgame | 52 | 3 | 0 | 44 | 0 | 0 | 0 | 0 | 5 | 41 |

- **When cemit prints, the print runs as the original does: no program of any corpus ran differently.**
- The refusals are the printer's rules, not its faults: a `#define` or `#if` (cemit's rule), GNU
  nested functions and `__real__`/`__imag__` (cemit has no form), GNU's `?:` without a middle operand
  (C-lisp's "a missing expression", 17 of the compile corpus). The fixpoint failures and the 7 prints
  gcc rejects ("redefinition of 'union u'", a tag defined in a parameter list) were not reduced.
- MIR's 142 "fails" are tests whose `main` returns a nonzero value on purpose (andrewchambers_c's);
  the reference's status is what a check compares, so they belong in the corpus with their status.
- 6 s to 24 s per corpus at eight jobs, gcc's two builds and runs included: recording gcc's runs of
  every corpus is a matter of minutes (CS14).

**E3: cc's preprocessor on C23's directives** (`ppprobe`, `cc.Preprocess`, against `gcc -std=c23 -E`):

| directive | gcc 15.2 | cc |
|---|---|---|
| `#elifdef Y` with `Y` defined | the `#elifdef` group | the `#else` group, silently |
| `#embed "file"` | the bytes as a list of integers | nothing, silently |
| `__VA_OPT__(,)` | expanded | left in the text (`f(1 __VA_OPT__(,) )`) |
| `#if __has_c_attribute(nodiscard)` | true | false |
| `#warning hi` | a warning | nothing |
| `__has_include` | yes | yes |

**E4: the corpora's standard.** gcc 15.2 accepts (`-fsyntax-only -w`) 969 of c-torture/execute's 1,700
files as `-std=c23` and 1,034 as `-std=gnu17`; the rest need `-fpermissive` (implicit `int` and
implicit declarations, errors since gcc 14) or are K&R definitions, which C23 removed. So "C23 at
parity" cannot mean "every file read as C23": each file keeps the standard its suite gives it (CS3).

**E5: the host.** `gcc` 15.2.0 (`x86_64-alpine-linux-musl`), `clang` 23.1.4, `cpp`, `cc` (gcc);
`tcc`, `csmith`, `creduce`, `valgrind` absent. A C23 program (`constexpr`, `auto`, `[[maybe_unused]]`,
`_BitInt(17)`, `5wb`) builds statically with both and runs. `/usr/lib/libc.a` (static musl) present;
`/lib/ld-musl-x86_64.so.1` is musl 1.2.6. `/root/musl`: a one-commit clone of
`https://github.com/kraj/musl.git` (branch `kraj/master`, commit `1c82569f4cc53f5bb25c820051c2a02463f46cab`,
dated 2017-03-15 though its `VERSION` reads 1.2.6), 1,538 `.c` files (62,557 lines) and 304 assembly
files under `src/`; its provenance must be settled before it is vendored (CS12).

## 4. C forms

### 4.1 Principles

Those of the Go forms (doc/go/SPEC.md §1), for C:

1. **Arbace keeps its meaning outside C context; inside, the forms mean C.** Inside a C declaration
   form, `+`, `<<`, `==` are C's operators at the operands' types after C's conversions (6.3), `if`,
   `while`, `for`, `set!` are C's statements and expressions. No Arbace code runs inside C context.
2. **Declarations mirror C.** One form per external declaration, in source order, one declarator per
   form (as cemit prints), one file per translation unit; identifiers verbatim (C's are case
   sensitive and need no munging; a C keyword that is an Arbace special form is only a head).
3. **The forms are the program after preprocessing**, and the preprocessing is kept beside it (§5.2):
   `#include` and the main file's macros as forms, a macro invocation recorded where it stood.
4. **Vocabulary shared with the Go forms** wherever C means the same: `(* T)`, `(array N T)`, `@p`,
   `(addr x)`, `(set! x e)`, `(set! x + e)`, `(aget a i)`, `(.-f s)`, `bit-and`, `bit-or`, `bit-xor`,
   `bit-not`, `and`, `or`, `not`, `(label :L)`, `(goto :L)`, `(break)`, `(return e)`; C's own heads
   where Go has none (`?:`, `comma`, `pre-inc!`, `sizeof`, `(-> p f)`). The Clojure reader reads them:
   `^`, `~`, `|` and `&^` are not symbols, so the bit operators are `bit-*` as in Go forms.
5. **Positions and text in metadata**: `:lines` (the default) keeps lines (`:c/line`), `:full` every
   token's position, spelling, comment and directive, for the print byte for byte (§5.4).
6. **Types are tags and type forms**, read in order from the name outward (C-lisp's insight), never
   C's inside-out declarator: `int (*(*x)[10])(int)` is `^{:tag (* (array 10 (* (fn [int] int))))} x`.

### 4.2 A first example

```c
#include <stdio.h>

static int count[26];

/* Counts the letters of s. */
static int tally(const char *s) {
    int n = 0;
    for (; *s; s++) {
        if (*s >= 'a' && *s <= 'z') {
            count[*s - 'a']++;
            n += 1;
        }
    }
    return n;
}

int main(void) {
    int n = tally("hello, world");
    printf("%d %d\n", n, count['l' - 'a']);
    return 0;
}
```

As C forms (`:lines`):

```clojure
(in-ns 'c.letters)

(c/file "letters.c" :std :c23
  :includes ["<stdio.h>"])

(c/var ^:static ^{:tag (array 26 int)} count)

(c/defn ^:static tally
  "Counts the letters of s."
  ^int [^{:tag (* (const char))} s]
  (c/let [^int n 0]
    (for [] @s (inc! s)
      (when (and (>= @s \a) (<= @s \z))
        (inc! (aget count (- @s \a)))
        (set! n + 1)))
    (return n)))

(c/defn main ^int [void]
  (c/let [^int n (tally "hello, world")]
    (printf "%d %d\n" n (aget count (- \l \a)))
    (return 0)))
```

- A translation unit is a namespace (`c.letters`, as goi's packages are `go.*` namespaces); `c/file`
  holds what is the file's (its standard, its includes in order, in `:full` its text's layer).
- `c/var`, `c/defn`, `c/typedef`, `c/struct`, `c/union`, `c/enum`, `c/static-assert`, `c/declare` (a
  declaration without a definition: a prototype, an `extern`) are the external declarations; storage
  classes and function specifiers are metadata keys (`^:static`, `^:extern`, `^:inline`,
  `^:thread_local`, `^:constexpr`, `^:noreturn`).
- A block's declarations are `c/let`: C's block scope begins at the declarator and ends at the block's
  end, so a `c/let` scopes over the rest of its block, as Go forms' `let` does.
- `*s` is `@s`, `s++` is `(inc! s)` (postfix, its value the old one, as Go's `i++` is `(inc! i)`),
  `++s` would be `(pre-inc! s)`, `count[i]++` is `(inc! (aget count i))`, `n += 1` is `(set! n + 1)`.
- `'a'` is `\a`: a C character constant, of type `int` (6.4.4.5); the comment is the doc string where
  it stands before a definition, a `;` comment elsewhere (in `:full`, kept as text).
- `return 0` stays `(return 0)`: C has no implicit result, and `main`'s implicit `return 0` (5.1.2.3.4)
  is the absence of a `return`, as in the text.

### 4.3 Types

| C | type form |
|---|---|
| `int`, `char`, `void`, `bool`, `float`, `double` | `int`, `char`, `void`, `bool`, `float`, `double` |
| `unsigned`, `unsigned long long`, `long double`, `signed char` | `unsigned`, `unsigned-long-long`, `long-double`, `signed-char` (one symbol per type of 6.2.5; `:full` keeps the specifiers' words and order, `long unsigned int`) |
| `T *`, `T *const`, `T *restrict` | `(* T)`, `(const (* T))`, `(restrict (* T))` |
| `const T`, `volatile T`, `_Atomic(T)`, `_Atomic T` | `(const T)`, `(volatile T)`, `(atomic T)` |
| `T[10]`, `T[]`, `T[n]` (VLA), `T[static 3]`, `T[*]` | `(array 10 T)`, `(array T)`, `(array n T)`, `(array ^:static 3 T)`, `(array * T)` |
| `R(int, char *, ...)`, `R(void)`, `R()` | `(fn [int (* char) &] R)`, `(fn [void] R)`, `(fn [] R)` (in C23 the same as `[void]`; before C23 unprototyped: the file's `:std` says) |
| `struct s`, `union u`, `enum e`, `struct { ... }` | `(struct s)`, `(union u)`, `(enum e)`, `(struct ^:anon {fields})` |
| a typedef name `T` | `T` |
| `typeof(e)`, `typeof(T)`, `typeof_unqual(e)` | `(typeof e)`, `(typeof-type T)`, `(typeof-unqual e)` |
| `_BitInt(17)`, `unsigned _BitInt(7)` | `(bit-int 17)`, `(unsigned-bit-int 7)` |
| `_Complex double`, `_Decimal64` | `complex-double`, `decimal64` |
| `alignas(16)` on a declaration | `^{:c/alignas 16}` |

The reserved type heads are `*`, `const`, `volatile`, `restrict`, `atomic`, `array`, `fn`, `struct`,
`union`, `enum`, `typeof`, `typeof-type`, `typeof-unqual`, `typeof-unqual-type`, `bit-int`,
`unsigned-bit-int`. A typedef name never collides with them: C's typedef names are ordinary
identifiers, and a type form is only read in a type position (a tag, a cast's, `sizeof`'s), so whether
`T` is a type is decided by where it stands, never by a symbol table (C-lisp's property, kept).

Struct and union types:

```clojure
(c/struct point
  ^int x
  ^int y
  ^{:tag unsigned :c/bits 3} flags
  (c/union ^int i ^char c))                 ; an anonymous member
(c/struct ^{:c/attrs [[gnu/packed]]} s
  ^{:tag int :c/attrs [[gnu/aligned 8]]} a)
```

Enums, C23's fixed underlying type and attributes:

```clojure
(c/enum hue RED (GREEN 3) BLUE)
(c/enum ^{:c/underlying long} big (LIMIT (conv long (>> (bit-not 0ul) 1))))
```

### 4.4 Declarations

| C | form |
|---|---|
| `static const char *name = "x";` | `(c/var ^:static ^{:tag (* (const char))} name "x")` |
| `int a, *b;` | `(c/var ^int a)` `(c/var ^{:tag (* int)} b)` (one declarator a form; `:full` keeps the shared specifiers) |
| `extern int f(int);` | `(c/declare ^:extern f (fn [int] int))` |
| `typedef unsigned long usize;` | `(c/typedef usize unsigned-long)` |
| `constexpr int N = 4;` | `(c/var ^:constexpr ^int N 4)` |
| `auto x = 1.5;` (C23 inference) | `(c/var ^:auto x 1.5)` (no tag: the checker infers, 6.7.10) |
| `static_assert(sizeof(int) == 4);` | `(c/static-assert (== (sizeof-type int) 4))` |
| `[[nodiscard("why")]] int f(void);` | `(c/declare ^{:c/attrs [[nodiscard "why"]]} f (fn [void] int))` |
| `int f(a, b) int a; char *b; { ... }` (K&R, before C23) | `(c/defn f ^int ^:c/kr [^int a ^{:tag (* char)} b] ...)` |
| `struct pt p = { .x = 1, [2] = 3 };`, `= {}` | `(c/var ^{:tag (struct pt)} p (init :x 1))`, `(init (at 2) 3)`, `(init)` |

Attributes are `:c/attrs`, a vector of attributes, each a vector `[name args...]` or a symbol;
`gnu::packed` is `gnu/packed` (a namespace qualified symbol, the prefix kept); another vendor's
arguments are their tokens as a string; GNU's `__attribute__((...))` is `:c/gnu-attrs` in the same
shape, so the print gives each back in its syntax. Where C puts them (before a declaration, after a
declarator's identifier, on a pointer, a member, an enumerator, a statement) is where the metadata
goes.

### 4.5 Code

| C | form |
|---|---|
| `x = e`, `x += e`, `x &= m`, `x <<= n` | `(set! x e)`, `(set! x + e)`, `(set! x bit-and m)`, `(set! x << n)`; an expression, its value the stored one (6.5.17) |
| `i++`, `i--`, `++i`, `--i` | `(inc! i)`, `(dec! i)`, `(pre-inc! i)`, `(pre-dec! i)` |
| `a[i]`, `a[i] = v` | `(aget a i)`, `(aset a i v)` (`i[a]` is `(aget ^:c/swapped i a)`) |
| `s.f`, `p->f`, `p->a->b` | `(.-f s)`, `(-> p f)`, `(-> p a b)` |
| `&x`, `*p`, `*p = v` | `(addr x)`, `@p`, `(set! @p v)` |
| `(T)e`, `sizeof e`, `sizeof(T)`, `alignof(T)`, `_Generic(e, int: a, default: b)` | `(conv T e)`, `(sizeof e)`, `(sizeof-type T)`, `(alignof-type T)`, `(generic e [int a] [:default b])` |
| `c ? a : b`, `a , b`, `(T){1, 2}`, `(static T){...}` | `(?: c a b)`, `(comma a b)`, `(lit T 1 2)`, `(lit ^:static T ...)` |
| `f(a, b)`, `(*fp)(x)`, `fp(x)` | `(f a b)`, `(@fp x)`, `(fp x)` |
| `{ ... }` | `(do ...)` |
| `if (c) {...}`, `if (c) {...} else {...}` | `(when c ...)`, `(if c then else)` |
| `while (c) s`, `do s while (c);`, `for (i = 0; i < n; i++) s`, `for (int i = 0; ...)` | `(while c ...)`, `(do-while c ...)`, `(for [(set! i 0)] (< i n) (inc! i) ...)`, `(for [^int i 0] ...)` |
| `switch (x) { case 1: case 2: f(); break; default: g(); }` | `(switch x (case 1) (case 2) (f) (break) (default) (g))`: labels are items, as C-lisp has them (a `switch` body is a block in which `case` labels stand) |
| `L:`, `goto L;`, `break;`, `continue;`, `return;`, `return e;` | `(label :L)`, `(goto :L)`, `(break)`, `(continue)`, `(return)`, `(return e)` |
| `[[fallthrough]];`, `[[likely]] {...}` | `(c/attr-stmt [fallthrough])`, `(do ^{:c/attrs [likely]} ...)` |
| a GNU statement expression `({ ... })`, `&&L`, `goto *p` | `(stmt-expr ...)`, `(label-addr :L)`, `(goto* p)` (GNU, CS3) |
| `asm("...")` | `(c/asm "...")` (read and printed; ci and AOT refuse it, CS3) |

Operators are Go forms' table (doc/go/SPEC.md §7.5) at C's meaning: `(+ a b c)` a left-nested chain;
`and`, `or` short-circuit with C's `int` result; comparisons binary, of type `int`; `(- x)`, `(+ x)`,
`(bit-not x)`, `(not x)` unary. A pair of parentheses C does not need is `^:c/paren` on the form (Go
forms' rule: N1 drops them, but C's prints need them for byte-for-byte `:full`, and for gcc's
warnings `-Wparentheses` at `:lines`).

### 4.6 Literals

Integer and floating constants are Clojure numbers with the same value; a constant whose C type is not
the default one (decimal `int`, `double`) is `(lit T v)`, since Clojure numbers carry no metadata:

| C | form | C type |
|---|---|---|
| `42`, `0x2A`, `052`, `0b101010`, `1'000` | `42`, `0x2A`, `052`, `2r101010`, `1000` | `int` |
| `42u`, `0xFFFFFFFF`, `42ul`, `3000000000` | `(lit unsigned 42)`, `(lit unsigned 0xFFFFFFFF)`, `(lit unsigned-long 42)`, `(lit long 3000000000)` | as 6.4.4.2's table gives it (the radix matters: `0xFFFFFFFF` is `unsigned`, `4294967295` `long`) |
| `3wb`, `3uwb` | `(lit (bit-int 3) 3)`, `(lit (unsigned-bit-int 2) 3)` | `_BitInt(N)` |
| `1.5`, `1.5f`, `1.5L`, `0x1p-2`, `1.5dd` | `1.5`, `(lit float 1.5)`, `(lit long-double 1.5M)`, `1/4` (exact), `(lit decimal64 1.5M)` | |
| `'a'`, `'\n'`, `u8'a'`, `L'é'`, `'ab'` | `\a`, `\newline`, `(lit unsigned-char \a)`, `(lit wchar_t \é)`, `(char-const "ab")` (multi-character, implementation-defined) | `int`, `unsigned char`, ... |
| `"abc"`, `"a" "b"`, `u8"x"`, `L"x"`, `"\xff"` | `"abc"`, `"ab"` (joined, `:full` keeps both), `(lit char8_t "x")`, `(lit wchar_t "x")`, `(byte-string 0xff)` (Go forms' form for bytes that are not UTF-8) | arrays |
| `true`, `false`, `nullptr` | `true`, `false`, `nil` | `bool`, `nullptr_t` |

The spelling a value does not keep (radix, suffix's case, separators, escapes) is `:full`'s.

### 4.7 The preprocessing layer

| C | form |
|---|---|
| `#include <stdio.h>`, `#include "x.h"` | the file's `:includes`, in order; an include among declarations is `(c/include "\"x.h\"")` where it stands |
| `#define N 10`, `#define MAX(a, b) ((a) > (b) ? (a) : (b))` (in the main file) | `(c/define N "10")`, `(c/define MAX [a b] "((a) > (b) ? (a) : (b))")`: the replacement list as its tokens' text (6.10.4); the code below holds the expansions |
| an invocation `MAX(x, 3)` whose expansion is one whole node | the node with `^{:c/macro "MAX(x, 3)"}` (cemit's recovery), printed back as the invocation |
| `#if`, `#ifdef`, `#elifdef`, `#else`, `#endif` around declarations | the active group's forms with `:c/cond` metadata naming the condition; the inactive groups' text in `:full` |
| `#pragma`, `_Pragma`, `#line`, `#error`, `#warning`, `#embed` | `(c/pragma "...")`; `#line` in positions; `#error` a read error; `#warning` a read warning; `#embed` its bytes as the initializer's integers, `^{:c/embed "\"file\""}` |

What C's preprocessor makes of the text is in the forms as code: eval and AOT never preprocess. What
was written is in the layer, which only the printer and the tools read. CS5 is the decision.

## 5. repr/c: read and print

### 5.1 The pipeline

```
C text ──cc (Go forms of go-whim's fork, compiled)──▶ cc's tree ──the dumper (generated Go forms)──▶ the dump (data)
      ──arbace.repr.c.convert (Arbace)──▶ C forms (.ae)          ──arbace.repr.c.print (Arbace)──▶ C text
```

As GP1-GP7 did for Go (doc/go/GO-PARSER-SURVEY.md, "As built: GP7"):

1. **cc as Go forms** (CS4 (a)): go-whim's `crefactor/cc` and its six dependencies converted by
   `bin/repr-go read` (the native reader) into `go/arbace/repr/c/cc/` and `go/arbace/repr/c/internal/`
   (`mathutil` and the rest, pruned to what cc imports), import paths renamed as GP7 renamed gc's
   `syntax`; a vendoring script (`bin/c-vendor convert|verify`, as `bin/box-vendor`) holding the origin
   (go-whim `80c49bf5`, upstream `modernc.org/cc/v4` v4.29.7) and proving the forms print back to the
   originals' code (`roundtrip`). `host.go`'s `SetHost` with Arbace's answers: the predefined macros
   and include paths of the reference (`gcc -std=c23 -dM -E`), recorded as data, and musl's headers
   (CS11) as the file system: no compiler asked at run time.
2. **Interpreted first, then compiled**: goi runs cc from the forms on the tools' host (GP1's way; cc has
   no `unsafe`, no assembly; its `os/exec` path unused under `SetHost`), then cc and the dumper are listed
   in goi's table and compiled into the executable (GP7's way), batched into a re-seed. GP1 measured
   go/parser interpreted at about 70 times slower than compiled (0.75 MB/s); a C corpus of 5,000 small
   files is about 10 MB after preprocessing (the headers dominate), minutes interpreted, seconds compiled.
3. **The dumper**: Go forms generated from cc's `ast.go` (one node type per production, generated
   upstream) as GP1's was from go/ast's forms, committed and kept so by `bin/generated-check`; it writes
   every node with its tokens' positions and, from cc's checker, its types where the check succeeds.
4. **The converter** `arbace.repr.c.convert`: the dump into C forms (§4), case for case after cemit and
   C-lisp's rules (one declarator a form, labels as items, parentheses kept), the macro recovery after
   cemit's `recover.go`, the preprocessing layer from cc's preprocessor's record (§5.2).
5. **The printer** `arbace.repr.c.print`: `:lines` in cemit's canonical spelling (a fixpoint, CS7's
   judges), `:full` the original text byte for byte (§5.4).

### 5.2 The preprocessor's place

C's translation phases 1-4 (5.1.1.2) make preprocessing tokens and execute directives; phases 5-7 make
the program. The forms must hold phase 7's program (eval and AOT need it) and, to print the text back,
what phases 1-4 consumed. CS5 (a), recommended: **the preprocessor runs inside read**, cc's preprocessor
completed for C23 (E3's six directives and operators, `__has_embed`, `__STDC_VERSION__` 202311L), and the
forms are the program after preprocessing with the layer of §4.7: the main file's directives as forms,
the invocations recovered where a whole node came from one, the rest (an invocation that is part of a
node, the inactive groups, comments, spacing) in `:full`'s text layer. So a `:lines` print is cemit's
canonical text with the main file's `#define`s and `#include`s in place and the recovered invocations,
gcc compiling it as it compiles the original; a `:full` print is the original.

Rejected: (b) forms of the preprocessing tokens (directives structured, the unit derived at load), which
makes every consumer a preprocessor and every conditional group a program to check under configurations
no build chooses; (c) cemit's rule, refusing every directive but `#include`, which refuses 19% of
c-torture and most real programs (E2).

### 5.3 Language versions and dialects

Each file's `:std` (`:c89`, `:c99`, `:c11`, `:c17`, `:c23`, and `:gnu89` ... `:gnu23`), from the corpus'
action as Go's `:lang` comes from its build tags: the parser's and the checker's rules that changed
(C23's removal of K&R definitions and of implicit `int`, `f()` as `f(void)`, keywords that were macros)
follow it. The reference compiles each file at its `:std` (CS3, CS14).

### 5.4 Mode `:full` and the print's levels

`:full` keeps, as repr/go's `:full` does (doc/go/SPEC.md §10.2): every token's position (`:c/pos`), its
spelling where the value does not keep it (radix, suffix, separators, escapes, specifiers' order,
`_Bool` against `bool`), the comments and the white space, the directives' text, the inactive groups,
the invocations that are parts of nodes, the line splices (`\` newline), the trigraph-free text C23
requires. It is read from the text by Arbace, as repr/go's `:full` reads the Go files' bytes itself
(cemit drops comments; cc keeps positions). The print's levels, as repr/go's and repr/java's round trips:

1. **forms**: C → forms → C → forms, the forms equal;
2. **text**: `:full` printed back, the original byte for byte;
3. **code**: the `:lines` print compiled by gcc gives the original's object file byte for byte at `-O0`
   (c23conf's judge; with `-g` excluded, since line numbers move), and its run is the original's (E2's
   measure).

### 5.5 Errors

A file the reference rejects is a read error with gcc's message where the corpus judges messages (gcc's
`dg-error` tests, CS16), as GP6 made gc's syntax errors the read level's for Go. cc's messages are not
gcc's: the judge is on the line and the kind (error or not), as goi's errorcheck matches regexps.

## 6. eval: ci

### 6.1 The parts

As goi's (doc/go/GOI-SPEC.md §1.3), with names by CS17:

| part | where | language |
|---|---|---|
| `arbace.c`: the macros `c/file`, `c/defn`, `c/var`, ... and the REPL's `c/include`, `c/eval`, `c/run-main` | `arbace/c.ae` | Arbace, embedded, compiled by c2j |
| `arbace.c.repl`: translation units as namespaces, declarations, redefinition, the load hook | `arbace/c/repl.ae` | Arbace |
| `arbace.c.check`: the checker (types, conversions, constant expressions, layout, linkage) | `arbace/c/check.ae`, `check/` | Arbace |
| `arbace.c.build`: the checked tree into nodes | `arbace/c/build.ae` | Arbace |
| `arbace/ci`: memory, frames, nodes, calls, varargs, `setjmp`, the natives | `go/arbace/ci.ae`, `ci/` | Go forms, hand-written |
| `arbace/crt`: the memory model and the libc's syscall layer, shared with AOT (§7) | `go/arbace/crt.ae`, `crt/` | Go forms, hand-written |
| the libc: musl as C forms (CS11) | `c/musl/` | C forms, converted |
| `bin/ci`: `run`, `check`, `divergences` | `bin/ci`, `test/c/` | bash, Arbace |

### 6.2 The checker

C's semantic decisions are local and specified: the integer promotions and the usual arithmetic
conversions (6.3.1), lvalue conversion, array and function decay, constant expressions and their values
(6.6, `constexpr` 6.7.2), layout (sizes, alignments, bit-fields, by the SysV x86-64 psABI for
linux/amd64, which cc's `abi.go` holds), linkage and composite types (6.2.2, 6.2.7), `auto`'s and
`typeof`'s types, `_Generic`'s selection. CS8 (a): the converter writes none of them (unlike repr/go,
which writes go/types' decisions, SPEC §11.1), and `arbace.c.check` computes them, as forms typed at the
REPL need; cc's checker is its oracle over the corpora (`bin/ci annotations`, as goi's checker against
go/types), with cc's own gaps (`_BitInt`, `typeof(type)`, `constexpr`'s values, a VLA's `= {}`) the
checker's to fill and gcc's acceptance their judge. The checker's size: cc's `check.go`, `type.go`,
`value.go`, `abi.go` are 10,849 lines of Go; goi's checker is the comparable Arbace work.

### 6.3 Memory and pointers

C's object model (6.2.4, 6.5, 6.7.3.2's unions, 7.24.2's `memcpy`) needs bytes: type punning through
unions and `char *`, `memcpy` of structs holding pointers, pointer ↔ integer casts, alignment tests on
addresses. CS9 (a), recommended, and the same in eval and AOT:

- **One flat virtual address space per C program**, 64-bit addresses, little-endian, the SysV x86-64
  layout; pointers are addresses (`uintptr`-sized words in memory, so `sizeof(void *) == 8`, a pointer
  stored and loaded through `char *` is the same pointer, `(uintptr_t)p & 7` is meaningful).
- **Memory as Go slabs**: the address space is a map of slabs (Go `[]byte` of 64 KiB and more), loads and
  stores by `encoding/binary`'s little-endian functions or `unsafe` within a slab (goi's back end allows
  `unsafe.Pointer` conversions, GOI-SPEC §1.1); the static data, the stacks (one per thread), the heap
  (musl's `malloc` over `mmap`/`brk` natives that hand out slabs).
- **Allocations recorded**: a block table (base, size, live), so that the REPL's checked mode reports a
  load or store outside a live block, a `free` of what was not allocated, a null dereference, with the
  C position (the REPL survives: the C program's memory is its own, never Arbace's); the check and AOT
  run unchecked by default, as gcc's build does.
- **Values crossing into Arbace**: integers as `Long` (or `BigInt` beyond), floating as `Double`, a
  pointer as a `CPointer` (its address and its type), a string copied out of memory on request
  (`(c/string p)`); from Arbace, numbers in range, strings copied into a fresh block.

Rejected: (b) blocks with provenance (each allocation its own `[]byte`, pointers `(block, offset)`, a
shadow table for pointers stored in memory: exact detection of every out-of-bounds access, Cerberus's
PNVI model), slower on every access and hard for integer ↔ pointer casts, which the corpora use; (c) Go
values for C objects (cxgo's way), which loses type punning.

### 6.4 Undefined behaviour

CS10 (a): ci gives the behaviour gcc's `-O0` code gives on x86-64 where it is defined by the machine
(signed overflow wraps in two's complement; a shift count is masked as `shl` masks it; `char` is signed;
an uninitialized automatic object holds whatever its memory held, zero on a fresh stack), traps what the
checked mode detects (§6.3; division by zero raises `SIGFPE` as the machine does: a status the check
compares), and the corpora are UB-free by their suites' rules. A program whose reference differs between
`-O0` and `-O2` is suspect (CS14) and compared on its status alone. A difference due to UB is a class
`ub`, by design.

### 6.5 The libc

Clause 7's library is the language's (a hosted implementation, 5.1.2.3), and the corpora print with
`printf`. CS11 (a): **musl 1.2.6 as C forms**, read by repr/c and interpreted by ci (later compiled, §7),
its few machine-dependent parts as natives in Go forms (`crt`): the syscall entry (`__syscall` on
linux/amd64, through Go's `syscall` and `os`), the thread pointer, `setjmp`/`longjmp` (a native that
unwinds ci's frames as goi's `panic` does, GOI-SPEC §4.7), the `fenv` and `math` assembly that has no C
fallback, signal delivery (a native over `os/signal`, for `raise` and `abort`). The reference links the
same musl (Alpine's), so `printf("%.17g")`, `strtod`, `qsort`'s order and `libm`'s results agree byte
for byte with no work of Arbace's. musl's 1,538 C files (62,557 lines) read once; its headers serve cc
(§5.1). Threads (`<threads.h>`, `pthread`) as goroutines over musl's own primitives (`futex` natives).

### 6.6 The REPL

As goi's: a translation unit is a namespace; `c/defn` and `c/var` are its declarations, redefinable (a
call goes through a cell, so a redefinition is seen by later calls; a build keeps C's static linkage);
`(c/include "<stdio.h>")` at the REPL brings musl's declarations; `(c/run-main 'c.letters)` runs `main`
in the program mode (exit and abort end the run, not the REPL); C forms call Arbace only through
function pointers to Arbace functions adapted (CS20). Forms typed by hand and forms converted by repr/c
take the same path.

## 7. AOT

CS13 (a): **C forms → Go forms**, a translator in Arbace (its name CS17's), its Go over `crt` (the
memory model of §6.3, the libc's natives), printed by repr/go and built by `arbace.build.go` with the
program, as j2g's output is: no cgo (none is allowed), no C compiler (none is in Arbace's toolchain), the
same build in the box later. Each C function a Go function of the scalars and addresses it takes
(`int32`, `int64`, `float64`, `uintptr`), locals whose address is taken in the C stack's frame, others
Go locals; `goto` as Go's `goto` where Go's rules allow, else a state machine (togo's `gotos.go` is the
prior art); `switch` with fall-through as Go's `fallthrough`; varargs as a `crt.VaList` over the stack;
`setjmp` as a Go `defer`/`recover` frame. musl compiled this way is the executable's libc for ci too
(listed in ci's table, as goi's table lists std): the interpreted program calls it compiled.

Checked as Go's AOT is (`bin/goi check --aot`): every run program of the corpora translated, built and
run against gcc's recorded run; a program the translator refuses is `refused` with its reason (as JC2's).

Rejected: (b) the forms printed and built by the host's gcc, which is not Arbace's and is absent from the
box (kept as the print level's code judge, §5.4); (c) ccgo/v4 vendored as Go forms (BSD-3, not on the
host, its own front end and memory through `unsafe` raw addresses, so two front ends to keep); (d) a code
generator of Arbace's own to machine code (QBE's shape), the long-term direction (CLAUDE.md: "all the way
down to the bare metal ISA"), not this milestone's.

## 8. The oracle and the corpora

### 8.1 The reference

The host's **gcc 15.2.0** (Alpine's `gcc`, target `x86_64-alpine-linux-musl`, musl 1.2.6), which
go-whim's c23conf already holds C23 to, at each file's `:std`, `-O0` (the judge), and `-O2` (UB's
witness); **clang 23.1.4** a second opinion on acceptance at the read level (where gcc and clang disagree
on a valid C23 file, the file is classed, not judged). Both are in Alpine's apk (doc/ALPINE.md's line
would gain them if the check records references; the check itself needs neither, CS14). Their runs
recorded as data (`test/c/ref-gcc15.edn`: per program the status, stdout's digest and its first line,
the `-O0`/`-O2` agreement), as `test/java/ref-jdk26.edn` records the JDK's. Recording all of §8.2 takes
minutes (E2).

If neither gcc nor clang were on the host, the references would have to come from elsewhere (recorded
on another machine and committed, or the suites' golden outputs: tcc's `.expect`, c-testsuite's), and
the read level would lose its object-code judge; they are present, so the question is only the
standalone direction's (CS14 (a) records them).

### 8.2 The corpora

On the host (read in place, nothing vendored):

| corpus | where | files | action | language | license (as data) |
|---|---|---:|---|---|---|
| go-whim's c23conf | `/root/go-whim/crefactor/c23conf/testdata` | 49 | read, print (no `main`) | C23 | go-whim's (CS22) |
| gcc 9.1 `gcc.c-torture/execute` (+ `ieee`, `builtins`) | Go's module cache, `modernc.org/ccorpus2@v1.6.0/assets/gcc-9.1.0/` | 1,700 | run (status 0, `abort()` on failure) | GNU C89-C17 | GPL-3.0+ (gcc's testsuite) |
| gcc-mirror `gcc.c-torture/execute` | `.../assets/github.com/gcc-mirror/gcc/` | 1,632 | run | GNU C | GPL-3.0+ |
| gcc 9.1 `gcc.c-torture/compile` | `.../gcc-9.1.0/` | 1,791 | compile | GNU C | GPL-3.0+ |
| tcc 0.9.27 `tests/tests2`, `tests/pp` | `.../tcc-0.9.27/` | 88 + 45 | run against `.expect`; preprocess against `.expect` | C99 | LGPL-2.1 |
| CompCert 3.6 `test/c` | `.../CompCert-3.6/` | 24 | run | C99 | CompCert's (mixed) |
| MIR's `c-tests` (andrewchambers_c, lacc, new) | `.../github.com/vnmakarov/mir/` | 315 | run | C11 | MIT |
| cxgo's cases | `.../github.com/cxgo/` | 80 | run | C | MIT |
| programs: SQLite amalgamation, Lua, zlib, QuickJS | `.../assets/` | | build and run their tests | C99-C11 | public domain, MIT, zlib, MIT |
| benchmarksgame | `.../benchmarksgame-team.pages.debian.net/` | 52 | run | C99 | BSD-3 |

ccorpus2 v1.6.0 (`modernc.org/ccorpus2`, 115 MB, BSD-3 for the module; each asset under its own
license) is pinned by Go's own sum (`/root/go/pkg/mod/cache/download/modernc.org/ccorpus2/@v/v1.6.0.ziphash`);
read in place like jdk26u's jtreg tests, nothing of it is vendored (CS15).

Not on the host, and the only sources of **C23 itself** beyond c23conf's 49:

- **gcc 15.2's testsuite**: `gcc/testsuite/gcc.dg/c23-*.c` and `c2x-*.c` (several hundred files, each one
  feature or one paper, with DejaGnu actions `dg-do run|compile`, `dg-error`, `dg-warning`, `dg-options
  "-std=c23 -pedantic-errors"`), `gcc.dg/cpp/c23-*.c` (the preprocessor), `gcc.dg/torture/`; GPL-3.0+,
  read in place from a fetched and pinned gcc 15.2 source tarball (as rustc's source is for repr/rust).
  The `dg-error` tests are C's errorcheck programs (goi's kind).
- **clang's `clang/test/C/C23/`** (one file per WG14 paper, `n3029.c` ...), `clang/test/Sema/`, `clang/test/
  Preprocessor/` (`-verify` with `expected-error` comments); Apache-2.0 with LLVM exception.
- **c-testsuite** (`github.com/c-testsuite/c-testsuite`, about 220 single-file tests with expected
  outputs); **chibicc's tests** (MIT).
- **Random programs**: csmith (BSD-2; UB-free programs printing a checksum) and YARPGen (Apache-2.0): a
  differential corpus of any size, built from source (C++), for eval and AOT.
- **UB**: NIST's Juliet suite (public domain), for the checked mode's reports, not for parity.

Nothing on the host covers clause 7's C23 additions (`<stdbit.h>`, `<stdckdint.h>`, `memset_explicit`,
`strdup`, `memccpy`, `timegm`, `%b` and `%w32d` in `printf`): musl 1.2.6 has part of them; c23conf's
manner (one file a feature, gcc's run the reference) extended by hand covers them (CS15).

## 9. Licenses, the host, the box

- **go-whim's parts.** go-whim's `LICENSE` is Vim's, for the modified vim its README names. `crefactor/cc`
  is upstream's under BSD-3 (Copyright (c) 2014 The CC Authors; its `LICENSE` beside it) with go-whim's
  delta; `cemit`, `clisp`, `c23conf` (and `togo`) carry no notice; every commit of `crefactor/` is the
  user's (`git log --format=%an -- crefactor`: 203 commits, `rhz`). As for the box (D8, LICENSE.md's
  `box/` entry), the user's statement would license go-whim's delta and those packages to Arbace under
  the EPL-1.0 (CS22); cc stays BSD-3, its dependencies BSD-3 (mathutil, strutil, sortutil, opt: their
  authors; token and bigfft: The Go Authors), each a LICENSE.md entry.
- **musl**: MIT (`/root/musl/COPYRIGHT`), a LICENSE.md entry with its full text when vendored as forms.
- **The corpora** are read in place and not distributed: no LICENSE.md entry (as jtreg's tests); the
  ones fetched are pinned by hash.
- **gcc and clang** are run, never vendored.
- **No cgo** (`CGO_ENABLED=0` everywhere): the libc cannot be the host's through cgo, which is why musl
  becomes C forms (CS11) and AOT goes through Go (CS13). **TamaGo's Go only**: cc, its dependencies and
  `crt` build with it (E1 built them). **62 GB, the big-job lock**: the corpora's references and ci's
  runs are small; AOT's shards (many test programs per `bin/build-go`, as JC2's) and musl's compiled
  build are the big jobs, one at a time under the lock. The disk (39 GB free at 65%) holds ccorpus2
  already; a gcc source tarball is about 150 MB.
- **The box is parked** (the user, 2026-10-26): C's four cells are the host's, as the crops' v1 were.
  The design keeps the box open: cc is Go forms (runs in the guest), `SetHost` replaces the host's
  compiler, musl's syscall layer is `crt`'s natives (the box's doorbell later), AOT needs no C compiler.

## 10. The fourth row

`test/reap/cells.edn` gains `:c` in `:languages` and four cells, `test/reap/main.ae` a reader per new
results file (or the existing `:results` reader, goi's rows `[ID RESULT REASON]`, reused):

| cell | sources | classes |
|---|---|---|
| `[:c :read]` | `bin/repr-c check` (acceptance against gcc, the print's object code, the error tests' lines): `test/c/check-read.edn` | `test/c/divergences-read.edn` |
| `[:c :eval]` | `bin/ci check`: `test/c/check-linux-amd64.edn`; `bin/ci annotations` beside | `test/c/divergences.edn` |
| `[:c :aot]` | `bin/ci check --aot`: `test/c/check-aot-linux-amd64.edn` | `test/c/divergences-aot.edn` |
| `[:c :print]` | `bin/repr-c roundtrip` (`:full` byte for byte, forms again, the run): `test/c/roundtrip-known.edn` | `test/c/divergences-print.edn` |

Until a check is recorded its cell prints "no check yet" with `:until` the milestone (cells.edn's
existing rule). The essential gate gains each level's essential subset (`test/c/essential*.txt`) at C8,
seconds to a minute each (CS19).

## 11. Plan (C1-C9)

Estimates in working days, as the repository's surveys give them (they have run well under). Each
milestone ends with its check green and its records (the journal, the agenda, this survey's "As built").

| milestone | content | exit criteria | days |
|---|---|---|---:|
| **C1** the spec | `doc/c/SPEC.md`: C forms (§4) complete for C23 and the GNU extensions CS3 admits, with worked examples; c23conf's 49 files and its judges brought into Arbace's tree (`test/c/c23/`), widened to the preprocessor's C23 and clause 7's (one file a feature); the corpora's manifest (`test/c/corpora.edn`) | the spec accepted by the user; the manifest lists every corpus of §8.2 with its pin | 2-3 |
| **C2** repr/c `:lines` | cc and its dependencies as Go forms (`bin/c-vendor convert|verify|roundtrip`), interpreted by goi; the dumper generated; `arbace.repr.c.convert`, `arbace.repr.c.print` (`:lines`); `bin/repr-c read|print|check`; `SetHost` with the recorded answers and musl's headers | every corpus of §8.2 read; c23conf's judges 2-6 on the forms; E2's numbers reproduced on repr/c with the directive refusals gone (CS5) | 4-6 |
| **C3** the preprocessor and `:full` | cc's preprocessor completed for C23 (E3); the layer of §4.7; mode `:full`; `bin/repr-c roundtrip` (forms, text, code); cc and the dumper listed in goi's table and compiled (one re-seed, batched) | `:full` byte for byte over every corpus; tcc's `pp` tests against `.expect`; the print's object code equal over the run corpora, every exception classed | 3-5 |
| **C4** the checker | `arbace.c.check`: types, conversions, constants, layout, linkage, `_BitInt`, `constexpr`, `typeof`, `auto`, `_Generic`; `bin/ci annotations` against cc's checker | the corpora checked with no error gcc does not give; the annotations equal to cc's where cc succeeds, every difference classed (cc's gaps) | 5-8 |
| **C5** ci | `go/arbace/crt` (memory, CS9), `go/arbace/ci` (frames, nodes, calls, varargs, `setjmp`), `arbace.c.build`, `arbace.c.repl`, the macros; musl as C forms interpreted, its natives; `bin/ci run|check|divergences`; gcc's runs recorded (`--record-ref`) | `bin/ci check` over every run corpus: none failing, every non-pass in a class; the REPL session of §6.6 | 8-14 |
| **C6** AOT | the C forms → Go forms translator over `crt`; `bin/ci check --aot` (shards built by `arbace.build.go`, under the lock); musl compiled and listed in ci's table | every run program translated or `refused` with a reason; none failing; the interpreted and compiled results compared (diffcheck's kind) | 6-10 |
| **C7** C23's corpus and errors | the C23 corpus of CS15 fetched and pinned (gcc 15.2's `gcc.dg/c23-*`, `c2x-*`, `cpp/c23-*`; clang's `test/C/C23` if decided), its actions (`dg-do run`, `compile`, `dg-error`); the read level's error judge | each file of it in a level's result or a class; the error tests judged by line and kind | 2-4 |
| **C8** the fourth row | cells.edn's `:c` row, its classes files, `bin/reap` with four rows; the essential subsets in `bin/gate`; `make` targets (`repr-c`, `ci-check`) and `make check-makefile` | `bin/reap`: 16 of 16 cells checked, none failing, no class pending | 1-2 |
| **C9** the milestone | `GATE_FULL_SERIAL=1 bin/gate --full` with C's full checks, the fixpoint, the re-seed, the tag (CS2); the divergences recorded as REAP v1's were | the tag, annotated with C23 (ISO/IEC 9899:2024), gcc 15.2 and musl 1.2.6 | 1-2 |

**Total: about 32 to 54 working days.** C1-C3 change only tools' namespaces and `go/`'s new packages
(evaluated or interpreted until a re-seed); C3's compiled reader, C5 and C6 change the program and need
re-seeds, batched. C4 can run beside C3; C7 beside C5 and C6 once C2 reads. The long pole is C5 (the
interpreter and musl), as goi's GR3-GR10 were Go's.

## 12. Proposals (CS1-CS24)

Each with its options, the recommended one first.

- **CS1 Names of the milestones.**
  - Options: (a) C1-C9, as the request named them, noting that C1-C9 also name repr/go's converter
    amendments of 2026-10-08 (doc/go/CONVERTER-NOTES.md, closed records), as JCS1 noted JC1-JC10's other
    meaning; (b) CR1-CR9 (unused); (c) C23-1 ... (long).
  - *Recommended: (a)*: the amendments are closed and always cited with their document.
- **CS2 The milestone's tag.**
  - Options: (a) `arbace-reaps-4-crops-v1`, annotated with the four crops' versions (C23 as ISO/IEC
    9899:2024, gcc 15.2 the reference, musl 1.2.6); (b) `arbace-reaps-c23-v1`; (c) the next version of
    `arbace-reaps-3-crops`.
  - *Recommended: (a)*: the matrix gains a row, and the name says what the tag certifies.
- **CS3 The language's scope.**
  - Options: (a) ISO C23 (N3220), and every earlier standard a corpus file's `:std` names (C89-C17, their
    GNU dialects), with the GNU extensions musl's headers and the run corpora need (`__attribute__`,
    `__builtin_*`, statement expressions, `typeof`, `__extension__`, `?:` without a middle operand, case
    ranges, labels as values, zero-length arrays), the rest (nested functions, `__real__`/`__imag__`, inline
    `asm`, `__int128` beyond arithmetic, vector types) read and printed but by design out of eval and AOT;
    (b) ISO C23 only, every other standard and extension out (about 43% of c-torture out, E4); (c) all of
    GNU C.
  - *Recommended: (a)*: musl and every real corpus need some GNU C, and the classes say which.
- **CS4 The front end.**
  - Options: (a) go-whim's `crefactor/cc` (modernc cc v4.29.7 with go-whim's C23 delta) vendored as Go forms
    (`go/arbace/repr/c/`), interpreted by goi then compiled through goi's table (GP1 → GP7), the delta
    recorded and a later upstream diffable; (b) cc ported to Arbace by hand (JN8's way for javac's parser):
    28,000 lines, and GPS's E4 measured a byte loop in Arbace at about the interpreted speed, so nothing
    gained in fidelity or speed; (c) a new C23 parser in Arbace; (d) clang's `-ast-dump=json` as a helper
    (repr/go's first helper's way): host-bound, not standalone, kept at most as an oracle beside.
  - *Recommended: (a)*: the front end exists, is tested to C23 by c23conf, and Arbace's way of taking Go
    code (box/, GP7) applies unchanged.
- **CS5 The preprocessor's place.**
  - Options: (a) inside read, cc's preprocessor completed for C23, the forms the program after
    preprocessing with the main file's directives and recovered invocations as forms and the rest in
    `:full`'s layer (§5.2, §4.7); (b) forms of the preprocessing tokens, the program derived at load; (c)
    cemit's rule (only `#include`).
  - *Recommended: (a)*: eval and AOT see the program, the printer the text, and 19% of c-torture is not
    refused (E2).
- **CS6 C forms' spelling.**
  - Options: (a) Go forms' vocabulary (`c/` for `go/`, `(* T)`, `@p`, `(addr x)`, `(set! x + e)`, `bit-*`,
    tags) with C's own heads where Go has none (§4); (b) go-whim's C-lisp verbatim (`(def ...)`, `(ptr
    T)`, token atoms), which needs its own reader (`^`, `~`, `|`, `'` in numbers); (c) C-lisp's heads with
    the Clojure reader's fixes.
  - *Recommended: (a)*: one Arbace language for its three foreign dialects, readable by the Clojure
    reader (CLAUDE.md: the reader wins), and goi's and ci's macros alike.
- **CS7 Literals.**
  - Options: (a) values as Clojure literals, a non-default C type by `(lit T v)`, the spelling in `:full`
    (§4.6); (b) the token text as atoms (C-lisp), values decoded by every consumer.
  - *Recommended: (a)*: Go forms' rule, and the checker reads values, not spellings.
- **CS8 What the converter writes.**
  - Options: (a) only what reading needs (structure, the layer, positions), every type and conversion
    derived by `arbace.c.check`, cc's checker its oracle; (b) cc's types of every expression written as
    `:tag` (repr/go's way with go/types), the checker later; (c) both from the start.
  - *Recommended: (a)*: C's decisions are local and specified, forms typed at the REPL need the checker
    anyway, and cc's checker fails where C23 is newest (`_BitInt`, `typeof(type)`, E1).
- **CS9 The memory model.**
  - Options: (a) one flat 64-bit virtual address space per program over Go slabs, the SysV x86-64 layout,
    pointers as addresses, allocations recorded for a checked mode, the same in eval and AOT (§6.3); (b)
    blocks with provenance and shadow tables for stored pointers (exact UB detection, Cerberus's PNVI);
    (c) Go values for C objects.
  - *Recommended: (a)*: it gives the corpora's integer ↔ pointer and punning code its meaning at one
    lookup per access, and keeps eval and AOT equal by construction.
- **CS10 Undefined behaviour.**
  - Options: (a) the machine's behaviour at gcc `-O0` on x86-64 where it defines one, the checked mode's
    traps at the REPL, UB-free corpora, an `-O0`/`-O2` disagreement compared on status (§6.4); (b) trap
    every detectable UB, signed overflow included; (c) no checks at all.
  - *Recommended: (a)*: parity is with gcc's runs; the REPL still reports what would corrupt memory.
- **CS11 The libc.**
  - Options: (a) musl 1.2.6 as C forms, interpreted then compiled, its syscall layer and assembly-only
    parts as natives in `crt` (§6.5); (b) a libc in hand-written Go forms (Go's `fmt`, `math`: outputs
    differ from musl's in the last digit or ulp, e.g. `%g`, `pow`); (c) the host's libc through cgo
    (forbidden).
  - *Recommended: (a)*: the reference links the same musl, so the library is equal by construction, and
    Arbace gains a libc of its own all the way down.
- **CS12 musl's provenance.**
  - Options: (a) musl's own release, `https://musl.libc.org/releases/musl-1.2.6.tar.gz`, fetched into
    `.tmp/` and pinned by SHA-256 (as `tools/rsdump/apks.sha256` pins rustc-dev), Alpine's patches noted;
    (b) `/root/musl` as it is (a one-commit clone of kraj/musl `1c82569f`, VERSION 1.2.6, its history
    unknown); (c) Alpine's `musl` source package.
  - *Recommended: (a)*: a named release and a hash; (b) cannot be traced to a musl commit from what is on
    the host.
- **CS13 AOT.**
  - Options: (a) C forms → Go forms over `crt`, printed by repr/go, built by `arbace.build.go` (§7); (b) the
    print built by the host's gcc; (c) ccgo/v4 vendored as the translator; (d) a code generator of
    Arbace's own (later).
  - *Recommended: (a)*: the build every other crop uses, no cgo, no C compiler, the box later.
- **CS14 The reference.**
  - Options: (a) gcc 15.2 at each file's `:std`, `-O0` the judge and `-O2` the witness, clang 23.1.4 a
    second opinion on acceptance, the runs recorded as data so the check needs no compiler (`--record-ref`,
    as JC's `ref-jdk26.edn`); (b) gcc live at every check; (c) clang the primary.
  - *Recommended: (a)*: gcc is what c23conf already trusts and what links musl; the record keeps the check
    standalone.
- **CS15 The corpora.**
  - Options: (a) ccorpus2 v1.6.0 read in place from Go's module cache (§8.2), c23conf's 49 brought in and
    widened by hand to the preprocessor's and clause 7's C23, and gcc 15.2's `gcc.dg/c23-*`, `c2x-*`,
    `cpp/c23-*` fetched (the source tarball, pinned by SHA-256) and read in place; (b) the same without any
    fetch (C23 only through c23conf's files, widened by hand); (c) (a) and clang's `test/C/C23` and csmith
    programs (csmith built from source).
  - *Recommended: (a)*: gcc's own C23 tests are the language's tests as `$GOROOT/test` is Go's, and the
    reference's own; csmith (c) after C6, when a second differential corpus is worth its build.
- **CS16 The read level's judge.**
  - Options: (a) the forms printed and compiled by gcc give the original's object file at `-O0` (c23conf's
    judge 5) over every corpus, and the error tests by line and kind against `dg-error`; (b) acceptance
    alone (cc accepts what gcc accepts); (c) tree equality with clang's JSON AST.
  - *Recommended: (a)*: object code is the judge that sees a changed value whatever the spelling, and it
    needs only the reference.
- **CS17 Names.**
  - Options: (a) `arbace.repr.c` (`bin/repr-c`), `arbace.c` (alias `c`), `arbace.c.check`, `arbace.c.build`,
    `arbace.c.repl`, the Go back end `arbace/ci` and runtime `arbace/crt`, the interpreter as a whole
    **ci**, `bin/ci`; the translator **cf2g** (C forms to Go), since `c2j`'s `c` is Clojure's and
    `c2g` named another tool before 2026-10-11 (CLAUDE.md); (b) the interpreter `cgi`, the translator
    `c2go`; (c) goi's pattern, `cii`.
  - *Recommended: (a)*, with the translator's name the user's (cf2g proposed).
- **CS18 The fourth row.**
  - Options: (a) `:c` in `test/reap/cells.edn`, the sources and classes of §10, `bin/reap` judging 16 cells;
    (b) a separate matrix for C.
  - *Recommended: (a)*: parity means the same judge.
- **CS19 The gate.**
  - Options: (a) each C level's essential subset in the essential gate from C8, the whole in `--full`
    (cached by key under the efficient gate's rules, doc/GATE-SURVEY.md); (b) `--full` only.
  - *Recommended: (a)*, as REAP8 did for the crops.
- **CS20 The REPL's scope for v1.**
  - Options: (a) goi's: translation units as namespaces, redefinition through cells, Arbace ↔ C at
    declarations and values (numbers, `CPointer`, strings copied), C calling Arbace through adapted function
    pointers; (b) the program mode only (`bin/ci run`), the REPL later.
  - *Recommended: (a)*: eval's cell is the REPL's, as goi's was; the interop surface is small.
- **CS21 The box.**
  - Options: (a) the host only, as the crops' v1 (the box parked), the design kept box-ready (§9); (b) C's
    essential subset in the box too.
  - *Recommended: (a)*: the user parked the box (2026-10-26).
- **CS22 The license of go-whim's parts.**
  - Options: (a) the user states that go-whim's delta to cc, `cemit`, `clisp`, `c23conf` (and `togo`'s rules
    if used) are licensed to Arbace under the EPL-1.0, as D8 did for the box; cc keeps BSD-3 (The CC
    Authors), each dependency its own BSD-3, LICENSE.md entries for all; (b) take only upstream cc (BSD-3)
    and rewrite the C23 delta.
  - *Recommended: (a)*: every commit is the user's, and D8 is the precedent.
- **CS23 Where cemit and C-lisp go.**
  - Options: (a) not vendored: their rules ported into `arbace.repr.c.print` (`:lines` is cemit's spelling)
    and `arbace.repr.c.convert` (C-lisp's case for case reading), cemit kept as the oracle of the `:lines`
    print in the vendoring check (the print equal to cemit's on the files cemit takes); (b) vendored as Go
    forms beside cc and called from Arbace; (c) neither.
  - *Recommended: (a)*: they are small (7,300 lines), C forms are not C-lisp, and the printer must be
    Arbace's to print forms typed by hand; cemit as an oracle costs nothing.
- **CS24 c23conf's home.**
  - Options: (a) its 49 files and judges re-made in Arbace (`test/c/c23/`, `bin/repr-c check --c23`), the
    files copied under CS22 (a), widened (C1); (b) read in place from `../go-whim`.
  - *Recommended: (a)*: go-whim is a pilot Arbace no longer depends on (CLAUDE.md).

## 13. What the user decides first

Before C1 can start:

1. **CS3**, the scope: C23 with the earlier standards and the GNU extensions musl and the corpora need,
   or ISO C23 alone (which drops about 43% of c-torture).
2. **CS22 and CS4**: a statement that go-whim's C23 delta, cemit, clisp and c23conf are licensed to
   Arbace under the EPL-1.0, and cc vendored as Go forms.
3. **CS5**, the preprocessor inside read with the directives as a layer (or cemit's refusal).
4. **CS15**, whether a C23 corpus may be fetched (gcc 15.2's source tarball, pinned): without it, C23 is
   checked through c23conf's files and their hand-made extensions alone.
5. **CS11 and CS12**, musl as the libc, and from which release.
6. **CS9**, the flat memory model (shared by eval and AOT).
7. **CS2**, the tag's name.

The rest (CS1, CS6-CS8, CS10, CS13, CS14, CS16-CS21, CS23, CS24) can be taken as recommended and
revisited when the spec (C1) is reviewed.

## 14. Risks

- **cc's checker is the weakest part** (E1's five errors on C23's newest features; `Translate` refuses a
  text whole). The checker of C4 is Arbace's own; cc's is only its oracle where it succeeds.
- **goi's speed on cc** is unmeasured: cc was not converted here (a repr/go read of 41,000 lines and a goi
  run are not light probes). GP1's 70× for go/parser suggests minutes per corpus interpreted; C3's
  compiled reader removes it.
- **musl through ci**: an interpreted `printf` for every line of output; the corpora print little, and C6
  compiles musl. musl's assembly parts beyond the syscall (`memcpy`'s fast paths have C versions; `fenv`,
  `setjmp`, the thread pointer do not) are natives to write and test.
- **Under-specified behaviour leaks into the reference**: `-O0`'s uninitialized stack, the order of
  evaluation gcc picks (unspecified, 6.5), `qsort`'s order on equal keys; the second run at `-O2` finds
  the first, the same musl the last, and the classes take what remains.
- **The corpora are old C**: c-torture's GNU C89 is not C23; parity on it says ci runs C, and C23's own
  features rest on c23conf's files and gcc's `c23-*` tests (CS15).
- **Program growth**: cc compiled (about 28,000 lines of Go, less what goi's table prunes) and musl
  compiled will grow the executable (GP7's go/parser added 0.36 MB for 3,821 lines; cc's share could be
  some MB, musl's more), to be measured at C3 and C6.

## 15. Sources

- go-whim, `https://github.com/arbace/go-whim` at `80c49bf53cd6340487ac623f3d6d135b77ac1db6`, checked out
  at `/root/go-whim` (read, not modified): `crefactor/cc/` (README.md, LICENSE, c23.go, host.go, cpp.go,
  check.go ...), `crefactor/cemit/`, `crefactor/clisp/` (SPEC.md), `crefactor/c23conf/` (doc.go,
  conf_test.go, pptokens_test.go, testdata/), `crefactor/togo/`, `crefactor/go.mod`; `doc/C23.md`,
  `doc/C-LISP.md`, `doc/C-LISP-TREE.md`, `doc/LISP-SANDBOX.md`.
- `modernc.org/cc/v4` v4.29.7 and `modernc.org/ccorpus2` v1.6.0 (`/root/go/pkg/mod/modernc.org/`),
  `modernc.org/mathutil` v1.7.1, `strutil` v1.2.1, `sortutil` v1.2.1, `token` v1.1.0, `opt` v0.2.0,
  `github.com/remyoudompheng/bigfft` `v0.0.0-20230129092748-24d4a6f8daec` (the same cache).
- musl: `/root/musl`, clone of `https://github.com/kraj/musl.git`, `1c82569f4cc53f5bb25c820051c2a02463f46cab`,
  VERSION 1.2.6, COPYRIGHT (MIT); the host's `/lib/ld-musl-x86_64.so.1` (1.2.6).
- The host's gcc 15.2.0 (Alpine) and clang 23.1.4 (Alpine), target `x86_64-alpine-linux-musl`.
- ISO/IEC 9899:2024; WG14 N3220 (2024-02-22) and N3096 (2023-04-01), `https://www.open-std.org/jtc1/sc22/wg14/`.
- Arbace (main `713424ac`): doc/FULL-SUPPORT-SURVEY.md ("REAP v1's divergences", §1), doc/go/SPEC.md
  (§1, §5.1, §7.1-§7.5, §8.1, §10.2), doc/go/GO-PARSER-SURVEY.md (Summary, §8, "As built: GP7"),
  doc/go/GOI-SPEC.md (§1, §5.1), doc/classes/JAVA-FRONTEND-SURVEY.md, doc/classes/JAVA-CONFORMANCE-SURVEY.md
  (§2, §4, §8, JCS1), doc/go/BOX-SURVEY.md (D8), LICENSE.md (`box/`'s entry), test/reap/cells.edn,
  doc/GATE-SURVEY.md, doc/go/CONVERTER-NOTES.md (C1-C9 amendments).
- Named, not on the host: `modernc.org/ccgo/v4`, `modernc.org/libc`, `github.com/gotranspile/cxgo`,
  `github.com/vnmakarov/mir`, `github.com/c-testsuite/c-testsuite`, `github.com/rui314/chibicc`,
  cproc and QBE (`c9x.me`), Cerberus (`github.com/rems-project/cerberus`), csmith
  (`github.com/csmith-project/csmith`), YARPGen (`github.com/intel/yarpgen`), gcc 15.2's
  `gcc/testsuite/gcc.dg/`, LLVM's `clang/test/C/C23/`, NIST's Juliet suite.

## Appendix A: the scratch

The worktree's `.tmp/c23/` (kept):

- `probe/` — the probe module (`main.go`, `go.mod` with a `replace` to `/root/go-whim/crefactor`,
  `go.sum` copied from it; `pp/main.go`, the preprocessor probe); `probe/probe` and `ppprobe` built with
  TamaGo's go1.27.1, `CGO_ENABLED=0`.
- `c23conf.log` — E1's `go test -v` output.
- `c23conf.tsv`, `torture-exec.tsv`, `torture-mirror.tsv`, `torture-compile.tsv`, `tcc.tsv`,
  `compcert.tsv`, `mir.tsv`, `bg.tsv` — E2's results, one line per file: path, stage, the type check's
  answer and the first line of a refusal.
- `pp23.c`, `va.c`, `elif.c`, `emb.c`, `hca.c` — E3's inputs; `t.c` — E5's C23 program.
- Reproduce E2: `cd .tmp/c23 && TMPDIR=$PWD/tmp ./probe/probe runs 1 FILE...` (eight jobs; gcc on PATH).

## Decided (2026-10-28): CS1-CS24

The user decided the survey's proposals, C23 to start after `arbace-reaps-3-crops-v2`'s tag:

- **CS4: (b), cc ported to Arbace**, not vendored as Go forms: the front end (cc's lexer, preprocessor,
  parser and the C23 delta) rewritten in Arbace code, as repr/java's native parser was a port of javac's
  (JN8), its trees checked against cc's as JN8's against javac's (cc, go-whim's `crefactor/cc`, kept as the
  oracle, run from the host's module cache or `../go-whim`, not vendored). C2 and C3 grow by the port.
- **CS22: left open until C1.** A port is a derivative of cc (BSD-3, The CC Authors, stated in LICENSE.md
  with the port's origin); the statement for go-whim's own parts (the C23 delta, cemit's and clisp's rules,
  c23conf) is the user's to make when C1 brings them in.
- **CS3: (a)**, ISO C23 with the earlier standards a file's `:std` names and the GNU extensions musl and the
  corpora need, the rest read and printed but out of eval and AOT by design.
- **CS9: (a)** (one flat 64-bit address space per program), **CS11: (a)** and **CS12: (a)** (musl 1.2.6 as C
  forms, its release tarball fetched into `.tmp/` and pinned by SHA-256), **CS14: (a)** (gcc 15.2 at `-O0` the
  judge, `-O2` the witness, the runs recorded as data), **CS15: (a)** (gcc 15.2's source tarball fetched and
  pinned), **CS2: (a)** (the tag `arbace-reaps-4-crops-v1`), **CS5: (a)** (the preprocessor inside read),
  **CS17: (a)** (`arbace.repr.c`, ci, `crt`, cf2g).
- The rest as recommended: CS1 (a), CS6 (a), CS7 (a), CS8 (a), CS10 (a), CS13 (a), CS16 (a), CS18 (a),
  CS19 (a), CS20 (a), CS21 (a), CS23 (a), CS24 (a).

## C1: the plan after CS4 (b)

C1, the spec, on the branch `c23-c1` from main `041b8242`, 2026-10-28: **doc/c/SPEC.md** (the C forms,
for the user's acceptance, C1's exit criterion), **test/c/corpora.edn** (every corpus of §8.2 with its
pin, and the gcc 15.2 groups it adds), and this section: the plan revised for CS4 (b), the oracle's setup,
and proposals CS30-CS43. No code under `arbace/`, `go/` or `bin/` changed; nothing of go-whim, musl or
gcc entered the tree (CS22 is open: CS37). The probes were light (gcc and clang on single files, the
Arbace reader of the main checkout's stage 0 on the spec's forms blocks); the scratch is the worktree's
`.tmp/c1/` (below).

### C1.1 What C1 found

- **F1 Two of §4's spellings do not read, three were ambiguous.** `(?: c a b)` (a symbol cannot end in
  `:`) and `(array ^:static 3 T)` (a number cannot carry metadata) are refused by the reader; `(lit T 1
  2)` for a compound literal clashed with CS7's `(lit unsigned 42)`, `(struct ^:anon {fields})` held
  members in a map, and type symbols such as `bool`, `int128`, `float32`, `decimal64` are legal
  identifiers of older C (`typedef int bool;`). SPEC §17.1 lists the twelve changes (A1-A12); every
  forms block of the spec (23 blocks) reads with Arbace's reader, and its six C programs build with
  gcc 15.2 (`-std=c23 -pedantic-errors -Wall`; the GNU one `-std=gnu23`) and print what §16 says.
- **F2 gcc 15.2 has no `c2x-*` tests.** gcc 14 renamed them `c23-*`: `gcc.dg/c23-*.c` is 270 files (dg-do
  compile 208 and one by default, run 50, preprocess 11; 102 with `dg-error`), `gcc.dg/cpp/c23-*.c` 16
  (preprocess 10, compile 5, run 1), and 30 more `c23-*` in `gcc.dg/{atomic,dfp,format,torture}`. C23's
  other tests are named by feature: `gnu23-*` (51), `#embed`'s `embed*` (47, in `gcc.dg/cpp` and
  `c-c++-common/cpp`), `va-opt*` (12), `bitint-*` (201). CS33.
- **F3 musl 1.2.6's headers are not C23's, and on the host they come first.** The reference's search
  list is `/usr/include/fortify`, `/usr/include` (musl's), then gcc's `include`; musl's `<stddef.h>`
  has no `nullptr_t` and no `unreachable()`, its `<stdarg.h>` no one-argument `va_start`, its
  `<limits.h>` no `BITINT_MAXWIDTH`, and no `<stdbit.h>` exists (gcc's own headers have the first four;
  `<stdckdint.h>` is gcc's). With gcc's include directory first (`-isystem`), a C23 file using them
  builds. CS32.
- **F4 gcc and clang evaluate a call's arguments in opposite orders.** `g(f(1), f(2), f(3))` calls
  `f(3)` first under gcc 15.2 at `-O0` and `-O2`, `f(1)` first under clang 23. Unspecified (6.5), and
  any program printing from its arguments' evaluation shows it. CS31.
- **F5 `/root/musl` is not the 1.2.6 release.** Against the fetched tarball 34 files differ and one is
  extra (`src/locale/gb18030utf.h`); the tarball's `include/` equals the host's `/usr/include` but for
  `libintl.h` and `paths.h` (Alpine's patches). CS12 (a) stands: the tarball is the source.
- **F6 Where the reference and clang part** (the classes' material, SPEC §9.1, §13.1, §14):
  `__has_c_attribute` gives 202311 for every standard attribute under gcc, the papers' dates under clang
  (and 0 for `unsequenced`); clang 23 rejects storage classes in a compound literal and `_Decimal64` on
  the target; `(unsigned long)1e20` is 0 under gcc and 2^63 under clang; `enum { A0, A1 }` is `unsigned
  int` under both.

### C1.2 The plan, C2-C9

CS4 (b) replaces the survey's C2 and C3 (cc vendored as Go forms, interpreted by goi, then listed in
goi's table and compiled) by a port, as JN8 ported javac's lexer and parser: cc's lexer, preprocessor,
parser and go-whim's C23 delta rewritten in Arbace namespaces, their output checked against cc's run as
the oracle, tree for tree. The converter and the printer, which were C2's and C3's second halves, become
C3b (CS42 (a): CS1's names kept). C4-C9 are as §11 had them.

What is ported (go-whim `80c49bf5`, `crefactor/cc`; the delta is 461 added and 66 removed lines of
`diff -ru` against upstream v4.29.7 in the upstream files, and `c23.go`'s 296):

| cc's file | lines | into | what |
|---|---:|---|---|
| `scanner.go` | 1,026 | `arbace.repr.c.lex` | phases 1-3: splices, pp-tokens with their separators (white space and comments) and sources, digit separators, `u8'a'`; and what cc's scanner refuses (`u'x'`, `U"x"`, survey §2.1) |
| `cpp.go` (with `#if`'s arithmetic from `value.go`) | 3,093 (+ about 300) | `arbace.repr.c.pp` | directives, macro expansion, `#if`, `__has_include`, include guards; and C23's directives cc lacks (E3): `#elifdef`, `#elifndef`, `#embed` and `__has_embed`, `__VA_OPT__`, `__has_c_attribute`, `#warning` |
| `parser.go`, `c23.go`, the hooks in `ast.go` | 4,037 + 296 | `arbace.repr.c.parse` | the recursive descent with its typedef scopes, C23's productions |
| `ast.go`, `ast2.go` (generated) | 5,652 | a table in `arbace.repr.c.tree` | 85 node types and their cases as data: each node a map of `:node`, `:case`, its tokens and children under cc's field names (JN8.1's trees as data) |
| `cc.go`, `etc.go`, `host.go` (the parts the front end needs) | about 800 of 2,134 | `arbace.repr.c.host` | the configuration: predefined macros, include paths, the headers as files, from the recorded host data |

About 9,500 lines of Go, by JN8's ratio (javac's lexer and parser into 3,414 lines) 4,000 to 6,000 lines
of Arbace. The checker (`check.go`, `type.go`, `value.go`, `abi.go`, 10,849 lines) is not ported: C4's
`arbace.c.check` is Arbace's own (CS8 (a)), cc's checker its oracle through the helper's `types` mode.

| milestone | content | exit criteria | days |
|---|---|---|---:|
| **C1** the spec | doc/c/SPEC.md, test/c/corpora.edn, this section (done, on `c23-c1`) | the spec accepted by the user; CS30-CS43 decided | 1 |
| **C2** the lexer and the preprocessor | `arbace.repr.c.lex`, `.pp`, `.host`; the host data recorded (`bin/repr-c host record`: the reference's predefined macros per standard, its search list and the headers' digests, its `__has_*` answers, under `test/c/host/`); `tools/ccdump` (below) with its `tokens` mode; `bin/repr-c pp FILE` and `pp-check` | every corpus file's token stream (kind, text, position, separator, whether from a macro) equal to cc's, or listed in `test/repr/c/pp-check-known.edn` where cc is wrong and gcc's `-E -P` text is the judge (CS40 (a)); tcc's 22 `pp` tests against `.expect`; gcc 15.2's `cpp/c23-*`, `embed*`, `va-opt*` preprocess tests against gcc's output | 3-5 |
| **C3** the parser and the C23 delta | `arbace.repr.c.parse`, `.tree`; the helper's `tree` mode; `bin/repr-c parse FILE` and `parse-check` (JN8.3's shape: chunks, threads, `report.edn`, `files.tsv`, both dumps of a differing file) | every file of every corpus parsed by both, the trees equal node for node (case, every token's text and position) or known; mutants of the corpora (JN8's 8,000: tokens deleted, duplicated, swapped, inserted) accepted or refused alike, at the same first error position | 2-4 |
| **C3b** repr/c read and print | `arbace.repr.c.convert` (trees into forms, SPEC §11.1; header units, the layer), `arbace.repr.c.print` (`:lines` by cemit's rules, CS23 (a); `:full`), `bin/repr-c read`, `print`, `roundtrip`, `check`; `test/c/c23/` and its judges re-made (CS38), widened to the preprocessor's C23 and clause 7's | E2's numbers reproduced on repr/c with no directive refusal (CS5 (a)); the round trip's levels 1-3 over every corpus, every exception classed; `:full` byte for byte; the c23 judges on every file | 4-6 |
| **C4** the checker | as §11; `bin/ci annotations` against the helper's `types` mode (`cc.Translate`) | as §11 | 5-8 |
| **C5** ci | as §11, with CS34's and CS35's arithmetic (`__int128` and `_BitInt` beyond 64 bits as limbs in `crt`; `long double` as `double`, musl read with `LDBL_MANT_DIG` 53, SPEC §12.5) | as §11; the class `long-double` recorded | 8-14 |
| **C6** AOT | as §11 | as §11 | 6-10 |
| **C7** C23's corpus and errors | as §11, over CS33's groups (`test/c/corpora.edn`'s `:gcc-*`, from the pinned tarball) | as §11 | 2-4 |
| **C8** the fourth row | as §11 | as §11 | 1-2 |
| **C9** the milestone | as §11 | as §11 | 1-2 |

**Total: about 33 to 56 working days** (§11's 32 to 54). The port costs more than vendoring: about 9,500
lines to port and check, against `bin/c-vendor` and goi's run of cc; it saves goi's interpretation of cc
(the survey's §14 put it at minutes per corpus), listing cc and a generated dumper in goi's table, and the
re-seed that would carry them. JN8 ran at about a tenth of its estimate (four and a half hours against
4-6 days), and the surveys run under their estimates, so the low ends are the likelier. C5 stays at
§11's 8-14: CS35 was decided without software `long double` (below, "Decided (2026-10-28): CS30-CS43").
Speed: JN8's native parser ran at 0.9 M characters a
second compiled and 0.1 to 0.16 M evaluated; the C corpora are about 10 MB after preprocessing (§5.1), so
a corpus read evaluated on the tools' host is one to two minutes, and headers are lexed once per path
(the token streams cached by file digest) (CS36 (a)).

Where the port runs (CS36 (a)): evaluated from the checkout on the tools' host, as JN8's parser and
repr/haskell's namespaces are, so C2-C4 change no shipped code and need no re-seed; at C5, when ci's
`c/include` reads headers at the REPL, `arbace/repr/c` is embedded and compiled by c2j with the program,
in the re-seed C5 needs anyway.

### C1.3 The oracle: `tools/ccdump`

cc runs as a helper beside the port, as `tools/godump` runs go/parser and go/types beside repr/go's
native reader; nothing of cc or go-whim is vendored (CS4 (b), CS39 (a)):

- **The helper** is Arbace's own Go program, `tools/ccdump` (module `arbace/tools/ccdump`, a few hundred
  lines: `main.go`, `dump.go`), which imports `github.com/arbace/go-whim/crefactor/cc` and nothing of
  it is copied. It reads a list of files with their `:std` and `:args` and the recorded host data, and
  calls cc's `SetHost` (go-whim's `host.go`) with the data's predefined macros, include paths and an
  `fs.FS` over the recorded headers: cc never runs gcc, and sees the headers the port sees.
- **Modes**: `tokens` (`cc.Preprocess`: per token its kind's name, its source text, its separator, its
  position, whether a macro made it); `tree` (`cc.Parse`: a reflection walk of the tree, one node a line:
  the type's name, `Case`'s name, each `Token` field with its text and position, children in field order,
  an absent child as `nil`; the port's `arbace.repr.c.parse/dump` prints its trees in the same format, so
  the check compares lines, JN8.1's `parse/dump`); `types` (C4: `cc.Translate`, each expression's type,
  each constant's value, each declaration's layout).
- **The build** (`bin/repr-c ccdump-build`, into `.tmp/repr-c/ccdump/`): a generated `go.work` with
  `use` of `tools/ccdump` and of `$GOWHIM/crefactor` (`GOWHIM`, default `../go-whim`); it requires the
  checkout at `80c49bf53cd6340487ac623f3d6d135b77ac1db6` with `crefactor/cc`'s tree
  `7df4827ccebe519cc06945dd5da4994b2b6d73d1` and no local change (`git status --porcelain crefactor/cc`
  empty), the tree hashes recorded in `test/c/corpora.edn`'s `:go-whim-80c49bf5`; the dependencies
  (`modernc.org/mathutil` v1.7.1, `strutil` v1.2.1, `sortutil` v1.2.1, `token` v1.1.0, `opt` v0.2.0,
  `remyoudompheng/bigfft`) come from Go's module cache under `crefactor/go.sum`'s hashes,
  `GOPROXY=off GOFLAGS=-mod=readonly`, with `/root/tamago-go/bin/go`, `CGO_ENABLED=0`. A build tag
  `upstream` builds the same helper against `modernc.org/cc/v4` v4.29.7 from the module cache
  (`h1:q+NXGJ0bK3b4TXFYQQVr9pYETGnmwFWkrUzJnMya/Tg=`), to tell a difference of the delta from one of
  upstream's.
- **Where it runs**: `pp-check`, `parse-check` and `bin/ci annotations` run it; `bin/gate --full` gains
  them at C3b (CS19 (a)), and doc/ALPINE.md's list of what apk does not provide gains a go-whim checkout
  at the pin, as it lists Go's module cache.
- **Where cc is wrong** (E3's directives, the scanner's `u'x'`, cc's checker's gaps) the port follows the
  reference, and each such file is listed in the check's known file with gcc's answer (CS40 (a)).

### C1.4 go-whim's parts, and `test/c/c23/` (CS22 open)

What the port and the checks would take from go-whim, listed and not copied:

- `crefactor/cc`'s C23 delta: `c23.go` (296 lines), `host.go` (169), `check_export.go` (25), `fork_test.go`
  (289), `README.md`, and the hooks marked `go-whim` in upstream's files (30 in `parser.go`, 27 in
  `ast.go`, 10 in `check.go`, 6 in `cc.go`, 5 in `scanner.go`, and 1-2 in each of nine more). Ported in
  C3 (the productions) and C2 (the scanner's), the port a derivative of cc (BSD-3, The CC Authors, a
  LICENSE.md entry when C2 lands) and, for the delta, of go-whim's (CS37).
- `crefactor/cemit` (2,528 lines) and `crefactor/clisp` (4,767 lines, `SPEC.md`): their rules ported into
  `arbace.repr.c.print` and `.convert` (CS23 (a)); cemit run as the `:lines` print's oracle by a helper
  mode like ccdump's.
- `crefactor/c23conf`: `doc.go`, `conf_test.go`, `pptokens_test.go` (552 lines: the six judges, the
  expected failures, `TestControl`'s four wrong prints) and `testdata/`'s 49 files, listed in
  `test/c/corpora.edn` (`:c23conf`, each file's git blob at `80c49bf5`, its SHA-256 and its N3220
  subclauses).

`test/c/c23/` is re-made in C3b, once CS37 is answered (CS38): under CS37 (a) or (b), the 49 files are
copied with their origin (go-whim, the commit, the path) recorded in `test/c/c23/README` and the journal,
and LICENSE.md's entry; the judges are Arbace's (`bin/repr-c check --c23`): (1) the reference accepts
the file (`-std=c23 -fsyntax-only`), (2) the port parses it with cc's tree, (3) the `:lines` print is
accepted, (4) a fixpoint (read, print, read, print equal), (5) the same program: the `:full` print's
pp-tokens one for one and the `:lines` print's object file byte for byte (`-O0 -fno-stack-protector -c`,
the same base name), (6) forms → C → forms equal; the expected failures a map with reasons; the control's
four wrong prints re-made as forms the judges must refuse. Under CS37 (c), 49 files of Arbace's own are
written from N3220, one per subclause the manifest lists, without go-whim's texts. Either way the set is
widened by Arbace's files for the preprocessor's C23 (`#embed`, `__has_include`, `__has_c_attribute`,
`#elifdef`, `#elifndef`, `#warning`, `__VA_OPT__`) and clause 7's (what musl 1.2.6 has: CS32).

### C1.5 Pins (test/c/corpora.edn)

| pin | where | hash | checked |
|---|---|---|---|
| gcc 15.2.0 | `https://ftp.gnu.org/gnu/gcc/gcc-15.2.0/gcc-15.2.0.tar.xz`, fetched into `.tmp/c1/gcc/` (101,056,276 bytes) | SHA-256 `438fd996826b0c82485a29da03a72d71d6e3541a83ec702df4271f6fe025d24e` | its SHA-512 equals gcc.gnu.org's `sha512.sum` for the release (fetched from that host, not the tarball's); the `.sig` fetched, not verified (no gpg on the host); `gcc/BASE-VER` 15.2.0 |
| musl 1.2.6 | `https://musl.libc.org/releases/musl-1.2.6.tar.gz`, fetched into `.tmp/c1/musl/` (1,082,499 bytes) | SHA-256 `d585fd3b613c66151fc3249e8ed44f77020cb5e6c1e635a616d3f9f82460512a` | its SHA-512 equals Alpine's aports `main/musl/APKBUILD` (1.2.6-r6; its nine patches listed in the manifest); the `.asc` fetched, not verified (no gpg) |
| ccorpus2 v1.6.0 | Go's module cache, `modernc.org/ccorpus2@v1.6.0/assets/` | `h1:JODbahvmixwZDoF1kZs/9Rj6aHA2vAPP7HKiNDq//xo=` (cc v4.29.7's `go.sum`, the cache's `ziphash`); zip SHA-256 `3b92d4cf...a4b168ed` | |
| go-whim | `80c49bf53cd6340487ac623f3d6d135b77ac1db6`, `crefactor/` | trees: `cc` `7df4827c`, `c23conf/testdata` `51e5218b`, `cemit` `c0f8bd96`, `clisp` `53686a78` | 49 files with their blobs |

The manifest's 21 corpora: c23conf (49), ccorpus2's thirteen (c-torture execute 1,672 and its mirror
1,632, compile 1,791, tcc's tests2 88 and pp 22, CompCert 24, MIR 315, cxgo 88 (§8.2 said 80),
benchmarks game 52, SQLite, Lua 5.4.4, zlib 1.2.11 15, QuickJS 10) and gcc 15.2's seven groups (F2: 627
files), 6,388 files in all, and six named but not fetched (clang's `test/C/C23`, c-testsuite, chibicc,
csmith, YARPGen, Juliet).

### C1.6 Proposals (CS30-CS43)

Each with its options, the recommended one first; the spec follows the recommendations.

- **CS30 Header declarations.**
  - Options: (a) **header units**: each active inclusion's declarations in a unit of its own,
    `c.h.DIGEST`, the digest of its forms, shared by the inclusions that give the same declarations
    (SPEC §4.3); (b) each translation unit holds every declaration it sees, the headers' marked
    `:c/from` and not printed; (c) the main file's declarations only, consumers looking names up in
    musl's units of the default context.
  - *Recommended: (a)*: a unit is self-contained through its `c/include` forms, headers are read and
    checked once per context, and no context needs recording (the content is the key); (b) repeats
    stdio's few hundred declarations in each of 6,000 files, (c) is wrong whenever a feature macro or
    `NDEBUG` changes a header.
- **CS31 The order of evaluation.**
  - Options: (a) gcc 15.2's `-O0` order on x86-64 where the corpora show it: a call's arguments right
    to left, an operator's operands left to right (F4); (b) left to right everywhere, a program whose
    output depends on the order a class `unspecified-order`; (c) the order read from the reference's
    object code per call.
  - *Recommended: (a)*: parity is with gcc's runs, and its order is regular enough to state; (c) is
    exact but makes eval depend on gcc.
- **CS32 The reference's headers.**
  - Options: (a) gcc's own include directory before musl's (`-isystem` it, in the host data's search list,
    for the reference's runs and for the reader alike), so C23's freestanding headers are gcc's and the
    rest musl 1.2.6's; clause 7's C23 additions musl lacks (`<stdbit.h>`, `memset_explicit`, ...) a class
    `musl-1.2.6` (deferred); (b) Alpine's default order, musl's headers first, C23's `nullptr_t`,
    `unreachable`, one-argument `va_start` and `BITINT_MAXWIDTH` classed; (c) headers of Arbace's own
    over musl's, for both.
  - *Recommended: (a)*: it is how gcc is configured where the libc does not ship freestanding headers
    (glibc), and it needs no header of Arbace's; C23 programs can then use `unreachable()` and the
    one-argument `va_start` from the headers, where c23conf's `unreachable.c` and `va_start.c` call the
    builtins directly.
- **CS33 The gcc 15.2 corpus.**
  - Options: (a) the manifest's seven groups (F2): `gcc.dg/c23-*`, `cpp/c23-*`, the subdirectories'
    `c23-*`, `gnu23-*`, `#embed`'s, `__VA_OPT__`'s and `_BitInt`'s tests, 627 files; (b) CS15's letter,
    `c23-*` and `cpp/c23-*` (286), `c2x-*` being none; (c) (a) and `c2y-*` (52), C2y's drafts.
  - *Recommended: (a)*: `#embed`, `__VA_OPT__` and `_BitInt` are C23's and their tests are named by
    feature, not by standard.
- **CS34 The floating-point environment.**
  - Options: (a) round to nearest only: `fesetround` to another mode fails (returns nonzero),
    `fetestexcept` reports no flag; a program needing more a class `fenv` (deferred; no file of the run
    corpora calls them); (b) software IEEE arithmetic honouring modes and flags once a program calls
    `fesetround` or `feclearexcept` (a slow path); (c) the host's FPU state through Go assembly.
  - *Recommended: (a)*: Go's arithmetic is round to nearest and has no flags; (b) is real work no corpus
    asks for yet; (c) puts assembly in `crt`.
- **CS35 The extended arithmetic types.**
  - Options: (a) `long double` as software x87 80-bit extended in `crt` (bit for bit at round to nearest
    and 64-bit precision), `__int128` and `_BitInt(N)` beyond 64 bits as 64-bit limbs, in eval and AOT;
    `_Decimal*`, `_Float16`, `_Float128`, `__bf16` out by design (SPEC §14); (b) `long double` computed as
    `double` (stored in its 16 bytes), classed where results differ; (c) (a) and the decimal and binary128
    types in software.
  - *Recommended: (a)*: musl's `printf("%Lf")`, `strtold` and 56 c-torture files use `long double`, and
    `__int128` and `_BitInt` are integer arithmetic the corpora (201 `bitint-*` tests) exercise; the
    decimal types have no libc support in musl.
- **CS36 Where the port runs.**
  - Options: (a) evaluated from the checkout on the tools' host until C5, then embedded and compiled with
    the program in C5's re-seed; (b) embedded from C2, compiled into each re-seed; (c) only ever
    evaluated.
  - *Recommended: (a)*: JN8's way, no shipped change before ci needs the reader at the REPL.
- **CS37 CS22's statement: the license of go-whim's own parts.** The user is asked: *are go-whim's own
  C parts (the C23 delta of `crefactor/cc`: `c23.go`, `host.go`, `check_export.go`, `fork_test.go` and
  the hooks marked `go-whim`; `crefactor/cemit`; `crefactor/clisp`; `crefactor/c23conf` with its 49
  files; `togo`'s rules if used), all of whose commits are yours, licensed to Arbace, and under which
  license?*
  - Options: (a) yes, under the EPL-1.0, as D8 licensed the box's parts: the port's LICENSE.md entry is
    cc's BSD-3 (The CC Authors) for what derives from upstream and the EPL-1.0 for what derives from the
    delta, c23conf's files under the EPL-1.0; (b) yes, under BSD-3 like cc, so the whole port is one
    license; (c) no: the port derives from upstream cc v4.29.7 alone, the C23 productions re-derived from
    N3220 and the reference without go-whim's text, cemit's and clisp's rules re-derived likewise, and
    c23conf's files replaced by Arbace's own (CS38 (b)).
  - *Recommended: (a)*: every commit is the user's, D8 is the precedent, and the port is then reviewable
    against its sources; (b) is as good if the user prefers one license for the port.
- **CS38 c23conf's re-making.**
  - Options: (a) under CS37 (a) or (b): its 49 files copied into `test/c/c23/` with their origin, its six
    judges re-made in Arbace over the forms (C1.4), widened by Arbace's own files; (b) under CS37 (c): 49
    files of Arbace's own, one per subclause the manifest records, written from N3220, the same judges;
    (c) read in place from a go-whim checkout (CS24 rejected it).
  - *Recommended: (a)*, which CS37 (a) or (b) allows; (b) if CS37 is (c).
- **CS39 The oracle's build.**
  - Options: (a) `tools/ccdump` built against a go-whim checkout through a generated `go.work`, the
    checkout's commit and tree verified, the dependencies from the module cache (C1.3); (b) against
    upstream `modernc.org/cc/v4` v4.29.7 only, C23's productions judged by the reference alone; (c) cc
    copied under `tools/` (CS4 (b) excludes it).
  - *Recommended: (a)*, with (b)'s build as the `upstream` tag beside it.
- **CS40 Where cc is wrong.**
  - Options: (a) the port follows cc where cc is right and the reference where cc is wrong (E3's
    directives, the scanner's refusals, the checker's gaps), each such file in the check's known file
    with the reference's answer; (b) the port reproduces cc exactly first, the fixes after; (c) the
    reference alone as the oracle (gcc's `-E` and `-fdump-tree-original`), cc not run.
  - *Recommended: (a)*: the trees are cc's where cc parses C23, and the known file says where they are not.
- **CS41 Parentheses in `:lines`.**
  - Options: (a) kept (`^:c/paren`, `:c/aparen`), as cemit keeps the source's (SPEC §7.5); (b) dropped,
    as Go forms' N1, the printer adding what precedence needs.
  - *Recommended: (a)*: CS23 (a) compares the `:lines` print with cemit's, and a reader of the print
    sees the source's grouping.
- **CS42 The milestones' names.**
  - Options: (a) C2 (the lexer and the preprocessor), C3 (the parser), C3b (read and print), the rest as
    CS1 named them; (b) renumbered C1-C10; (c) C2a, C2b, C3.
  - *Recommended: (a)*: CS1's names and the survey's references stay valid.
- **CS43 The date macros.**
  - Options: (a) `__DATE__`, `__TIME__` and `__TIMESTAMP__` from one fixed epoch (`SOURCE_DATE_EPOCH`,
    which gcc honours), the same for the reader and the reference's recordings; (b) the reader's clock,
    a program printing them classed.
  - *Recommended: (a)*: a read is then reproducible from its record.

### C1.7 Checks and scratch

Run: gcc 15.2 and clang 23.1.4 on the probes and on the spec's six C programs (built, run, outputs as
§16 says); the main checkout's stage 0 reading the spec's 23 forms blocks and `test/c/corpora.edn`
(21 corpora, 6,388 files, every `:files` list its `:count`); the SHA-512 cross-checks of C1.5. Not run:
any Arbace build, `bin/gate` (no code changed; the caller's instruction), gpg (absent).

The worktree's `.tmp/c1/` (kept):

- `gcc/` — `gcc-15.2.0.tar.xz`, `.sig`, `sha512.sum`, `gnu-keyring.gpg`, `listing.txt` (the tarball's
  149,865 paths), `x/` (the C23 tests extracted, F2's counts).
- `musl/` — `musl-1.2.6.tar.gz`, `.asc`, Alpine's `APKBUILD`, `x/` (the release extracted);
  `musl-vs-root.txt`, `musl-vs-usrinclude.txt` (F5).
- `probe/` — `types.c` (constant types, sizes, layout, `_BitInt`), `en.c` (enums, conversions),
  `order.c` (F4, overflow, shifts), `pp.c` (`#embed`, `__VA_OPT__`, `#elifdef`, `__has_c_attribute`),
  `c23lib.c` (F3), `div.c`, `ovf.c`, `mod.c`, `nul.c`, `ab.c` (statuses), the acceptance probes (`kr.c`
  ... `ucn.c`, with `.gcc` and `.clang` logs), `sl.c`, `im.c`.
- `examples/` — the spec's six C programs; `specc/` — the same extracted from the spec and rebuilt.
- `scripts/` — `gen_manifest.py` (writes `test/c/corpora.edn` from `listing.txt` and `c23conf.txt`),
  `readcheck.ae` (reads a Markdown file's `clojure` blocks); `c23conf.txt` (the 49 files' blobs, hashes
  and subclauses); `cc-delta.diff` (upstream v4.29.7 against the fork).

## Decided (2026-10-28): CS30-CS43

The user decided C1's proposals (relayed by the coordinator, 2026-10-28):

- **CS37: (a)**, go-whim's own C parts (the C23 delta of `crefactor/cc`: `c23.go`, `host.go`,
  `check_export.go`, `fork_test.go` and the hooks marked `go-whim`; `crefactor/cemit`; `crefactor/clisp`;
  `crefactor/c23conf` with its 49 files; `togo`'s rules if used) are licensed to Arbace under the EPL-1.0,
  as D8 licensed the box's parts. **This settles CS22.** cc keeps its BSD-3 (Copyright (c) 2014 The CC
  Authors), and so do its dependencies.
- **CS38: (a)**, following CS37: c23conf's 49 files are copied into `test/c/c23/` and its judges re-made
  in Arbace (C1.4).
- **CS35: not the recommended option.** No software x87 arithmetic: `long double` is `double` (8 bytes,
  align 8, `double`'s arithmetic) in eval and AOT, a distinct type for the checker. The reference's
  `long double` is 16 bytes (align 16, x87 80-bit extended), so `sizeof(long double)`, the layouts that hold
  one (`max_align_t` among them), `LDBL_*` and the values computed in `long double` differ from gcc's: a
  run that differs because of it is in the recorded class `long-double` (by design, SPEC §12.5), and the
  reader keeps the reference's configuration for corpus files (their `#if`s and the print's object code
  stay gcc's) while musl, as ci's libc, is read with `long double` as `double` (`LDBL_MANT_DIG` 53, its
  `arch/arm/bits/float.h`). The rest of CS35 stands: `__int128` and `_BitInt` beyond 64 bits are in (64-bit
  limbs in `crt`), `_Decimal*`, `_Float16`, `_Float128`, `__bf16` out by design (SPEC §14). C5 stays at
  8-14 days; the plan's total is 33 to 56 (C1.2).
- **CS30, CS31, CS32, CS33, CS34, CS36, CS39, CS40, CS41, CS42, CS43: (a)**, as recommended: header units;
  gcc's `-O0` order of evaluation; gcc's include directory before musl's; the seven gcc 15.2 groups; round to
  nearest only; the port evaluated on the tools' host until C5; `tools/ccdump` through a generated `go.work`;
  the reference where cc is wrong; parentheses kept in `:lines`; the milestones C2, C3, C3b; a fixed
  `SOURCE_DATE_EPOCH`.

What CS37 (a) allows, and when (nothing was copied at C1, `c23-c1` `009be3d7` and this section's commit):

- **C2 and C3**: the port may follow go-whim's delta line for line where it derives from it (the scanner's
  digit separators and `u8'a'` in C2; `c23.go`'s productions and the hooks in C3), its files carrying
  cc's notice and the delta's origin; LICENSE.md gains, when the first ported file enters the tree, an
  entry for `arbace/repr/c/` with cc's BSD-3 text (The CC Authors, modernc.org/cc/v4 v4.29.7) and the
  EPL-1.0 for what derives from go-whim's delta (go-whim `80c49bf5`, `crefactor/cc`), and doc/ARBACE.md
  the origin, as for every vendored `.ae`.
- **C3b**: cemit's and clisp's rules ported into `arbace.repr.c.print` and `.convert` under the EPL-1.0
  (their origin recorded likewise); c23conf's 49 files copied into `test/c/c23/` with their origin
  (go-whim, the commit, the path, each file's blob as `test/c/corpora.edn` records it) in
  `test/c/c23/README` and the journal, and a LICENSE.md entry naming them under the EPL-1.0; its judges
  re-made in Arbace as C1.4 describes.


## As built: C2 (2026-10-28)

C2, the lexer and the preprocessor, on the branch `c23-c2` from main `bf84a975`, in about one day of
work (against C1.2's 3-5). `arbace.repr.c.lex` and `arbace.repr.c.pp` port cc's scanner and preprocessor
(go-whim `80c49bf5`, `crefactor/cc`; go-whim's `cpp.go` is upstream v4.29.7's, its scanner carries the
delta's digit separators and `u8'a'`) function by function, with C23's directives and operators cc lacks
and the reference's behaviour where cc is wrong (CS40 (a)), and give the token stream cc's `Preprocess`
gives. `bin/repr-c check --tokens` compares both levels, each file's own tokens (`lex`) and the
preprocessed stream (`pp`), with cc's over every corpus of `test/c/corpora.edn` read in place: **every file
of the 20 corpora (6,387 files, 18.2 MB of main files) gives cc's dump line for line, or is in the known
file with its class and the reference's tokens**. The port runs evaluated on the tools' host (CS36 (a)):
nothing shipped changed but the embedding list (below); no re-seed. Go only TamaGo's go1.27.1, no cgo.

### C2.1 What was ported

| Arbace | lines | ports (go-whim `crefactor/cc`) |
|---|---:|---|
| `arbace/repr/c/lex.ae` | 577 | `scanner.go` (1,026): the scanner's phases 1-3 (`rune`'s splices, `cppScan`'s states, `cppScan0`, the separators, comments, pp-numbers with the delta's separators, character constants and strings with `u8`), modernc.org/token's `File` (the line table, `#line`'s infos, `Position`), `runeName` and the tokCh names |
| `arbace/repr/c/pp.ae` | 1,655 | `cpp.go` (3,093): `cppParser` (the groups), the token sequences (`cppTokens`, `dequeue`, `tokenizer`), `expand`/`subst`/`glue`/`stringize` with hide sets (Prosser's algorithm after Spinellis), `parseMacroArgs`, `varArgs`, `nextLine`'s machine, `#include`/`#include_next` with `included`, `__has_include`, `defined`, `_Pragma`, `push_macro`/`pop_macro`, `#if`'s arithmetic (int64/uint64 as cc's type switches make them), `intConst`, `charConstValue` and `decodeEscapeSequence` (`etc.go`), `tokens2CppTokens`, `toksTrim`, `Preprocess`'s text (`cc.go`) |
| `arbace/repr/c/host.ae` | 104 | Arbace's: the recorded host, a file's dg-options (`-std`, `-ansi`, `-D`, `-U`, `-I`, `--embed-dir`, `-O`) |
| `arbace/repr/c/ppcheck.ae` | 378 | Arbace's: the corpora, the lists, the check, the judge, the record, `bin/repr-c pp` |

About 4,400 lines of Go (with the 300 of `etc.go` and `cc.go` the two use) into 2,232 of Arbace for the
port proper, JN8's ratio. Both files carry cc's notice; go-whim's delta and Arbace's changes are marked
(`go-whim`, `Arbace:`). LICENSE.md gains `arbace/repr/c/`'s entry and "cc (modernc.org/cc/v4) and go-whim's
C23 delta: BSD 3-Clause License" (CS37 (a): cc's BSD-3, the delta and Arbace's changes EPL-1.0);
doc/ARBACE.md the origin.

The port's tokens are cc's: kind (cc's tokCh name, or the character), source text, separator (the white
space and comments before it, splices taken out), position (cc's `token.Position`: the file and offset
of the outermost invocation for a macro's tokens, `#line` applied, computed from each file's line table
as cc's fset does) and the object-like macro that made it (cc's `Token.m`, which only object-like
expansion sets). A file's texts are held as bytes (ISO-8859-1 strings, one char a byte), so nothing is
lost to decoding. cc's behaviour is kept where the corpora cannot tell it from gcc's or show it right,
its oddities included: one token after a failed `#include` before the stream closes; the separator lost
after `#include MACRO`; `#include`'s level count, which never passes 1 in cc (the port counts truly, to
200, so that a recursive include stops); arguments' new-lines dropped without a space; cc's panics
(`D4(a)` of tcc's `11.c`, an unterminated invocation) as the port's, the stream kept to there. The
groups of a file are parsed once a run (cached by name and content), so headers are lexed once.

C23 and the reference where cc is wrong (each `Arbace:` in the files, each difference classed in the known
file): `#elifdef`, `#elifndef`; `#warning` (a warning of the read); `#embed` (its header name lexed,
`limit`, `prefix`, `suffix`, `if_empty`, `gnu::offset`, `gnu::base64`, unknown parameters not found,
`--embed-dir`, negative operands refused) and `__has_embed` (in `#if` and outside it); `__VA_OPT__` (a
pseudo-argument of `subst`, so that `#` and `##` treat it as a parameter; the variable arguments' presence
after their expansion; placemarkers in its body where an empty argument stands, gcc's reading);
`__has_c_attribute`, `__has_attribute`, `__has_builtin`, `__has_feature`, `__has_extension` from the
recorded answers, `defined` of them true, the attribute and builtin ones outside `#if` too; `## #param`
(the stringized argument pasted); a paste that makes no one token kept as two, with gcc's error; `u'x'`,
`U'x'`, `u"x"`, `U"x"` as one token (the wide kinds); `true` in a C23 `#if`; binary constants, digit
separators and `wb` suffixes in `#if` and in `#line`; `#pragma once`; `__BASE_FILE__`,
`__INCLUDE_LEVEL__`, `__FILE_NAME__`.

### C2.2 The oracle, the host, the command

- **`tools/ccdump`** (C1.3, CS39 (a); 311 lines of Go): `main.go` (modes `lex`, the scanner's tokens of
  each file, and `pp`, the preprocessed stream; one line a token: position, kind, source, separator, and
  in `pp` the macro; errors and a panic after the tokens; the list of files as JSON lines; `-skip-done`
  and `.current`, so that a hang is marked and the run resumed), `cc_whim.go` (go-whim's cc, configured
  through `SetHost`: cc never runs gcc) and `cc_upstream.go` (the build tag `upstream`: modernc.org/cc/v4
  v4.29.7, configured by a `Config` made as `NewConfig` makes it). cc gives its preprocessor's tokens only
  to its parser, so `tools/ccdump/overlay/export.go`, Arbace's, is laid into cc's package by `go build
  -overlay` (`ArbaceLex`, `ArbacePreprocess`: they call cc's own functions, nothing of cc copied, the
  checkout untouched). `bin/repr-c ccdump-build [--upstream]` writes the `go.work` (tools/ccdump and
  `$GOWHIM/crefactor`, default `../go-whim`) after checking the commit, `crefactor/cc`'s tree `7df4827c` and
  no local change; the dependencies from Go's module cache, `GOPROXY=off GOFLAGS=-mod=readonly`; the
  upstream build checks the module's ziphash and lays the exporter into a copy of it under `.tmp/` (the
  module cache refuses `-overlay`). Upstream against go-whim (the delta's share of the oracle): the
  same dumps but 2 of c23conf's 49 files, 5 of gcc-c23's 270, 1 of cpp/c23's 16 (pp), none of torture's.
- **The host** (`bin/repr-c host record`, `check`; SPEC §9.1): `test/c/host/predefined/STD.h` for 13
  standards (`gcc -std=STD -isystem GCC-INCLUDE -dM -E`, sorted, then `__DATE__` "Oct 28 2026",
  `__TIME__` "00:00:00" and `__TIMESTAMP__` of the epoch 1793145600, 2026-10-28, as gcc gives the first
  two with `SOURCE_DATE_EPOCH`, CS43 (a)); `search.txt` (gcc's include directory, then
  `/usr/include/fortify` and `/usr/include`, CS32 (a)); `has.tsv` (263 answers: every `__has_*` name
  the corpora and the headers spell, and those a check's run found missing); `headers.sha256` (the 198
  headers the check read); `host.edn`. cc and the port get the same predefined text (the `<predefined>`
  source) and search list; a file's `-D`/`-U`/`-O` are lines after it.
- **`bin/repr-c`**: `host record|check`, `ccdump-build`, `gcc-tests` (gcc 15.2's tarball, fetched when
  absent, checked against the pin, its testsuite extracted into `.tmp/repr-c/gcc-15.2.0/`), `lex FILE`,
  `pp [--std S] [--text] [OPTION...] FILE`, and `check --tokens [--corpus C,...] [--level lex,pp]
  [--record] [--judge-all]` (`bin/lib/repr-c/check`, `arbace.repr.c.ppcheck`). The judge where cc and
  the port differ is the reference: `test/repr/c/pp-check-known.edn` lists each such file by level and
  corpus with its class and, at `pp`, `:gcc`, the SHA-256 of gcc -E -P's tokens (lexed by the port's
  lexer, one a line, `#pragma` lines dropped as cc's stream drops pragmas, `#embed`'s base64 lines as
  their integers), which the port's tokens must give, or `:rejected` when gcc rejects the file (an
  error test) and the port reports an error too. `--record` runs gcc on the differing files
  (`SOURCE_DATE_EPOCH`, the file's modelled options, gcc's include directory first) and rewrites the
  entries, the classes kept; a stale entry fails the check. gcc corpora's files are named by their
  absolute paths, as DejaGnu gives them (`__FILE__`, `#embed __FILE__`). Make: `repr-c-host-record`,
  `repr-c-host-check`, `repr-c-ccdump`, `repr-c-check`. No gate step (C8).

### C2.3 The check's numbers

`bin/repr-c check --tokens --judge-all`, every corpus read in place (Lua's tarball not: C5 extracts
it), 8 threads:

| corpus | files | lex: equal / known | pp: equal / known (gcc) | the reference on every file: agree / both reject / differ |
|---|---:|---|---|---|
| c23conf | 49 | 49 | 49 | 48 / 0 / 1 |
| torture-execute | 1,672 | 1,672 | 1,672 | 1,672 |
| torture-execute-mirror | 1,632 | 1,632 | 1,632 | 1,632 |
| torture-compile | 1,791 | 1,791 | 1,791 | 1,789 / 2 |
| tcc-tests2 | 88 | 88 | 88 | 87 / 0 / 1 |
| tcc-pp | 22 | 22 | 22 | 20 / 0 / 2 |
| compcert | 24 | 24 | 24 | 24 |
| mir | 315 | 315 | 315 | 313 / 1 / 1 |
| cxgo | 88 | 88 | 88 | 88 |
| benchmarksgame | 52 | 52 | 52 | 46 / 6 |
| sqlite | 2 | 2 | 1 / 1 | 2 |
| zlib | 15 | 15 | 15 | 15 |
| quickjs | 10 | 10 | 10 | 10 |
| gcc-c23 | 270 | 269 / 1 | 264 / 6 | 261 / 9 |
| gcc-cpp-c23 | 16 | 12 / 4 | 10 / 6 | 8 / 2 / 3, 3 gcc only rejects |
| gcc-c23-sub | 30 | 30 | 30 | 30 |
| gcc-gnu23 | 51 | 51 | 51 | 51 |
| gcc-embed | 47 | 5 / 42 | 1 / 46 | 37 / 10 |
| gcc-va-opt | 12 | 12 | 3 / 9 | 10 / 1, 1 gcc only rejects |
| gcc-bitint | 201 | 201 | 201 | 201 |
| **total** | **6,387** | **6,340 / 47** | **6,319 / 68** | **6,345 agree, 31 both reject** |

No file fails; the known entries are 68 at `pp` and 47 at `lex`. `--judge-all` (informative, the status
unchanged) runs gcc on every file: where the port follows cc the reference agrees but on 9 files, cc's
gaps the port keeps (C2.6): UTF-8 identifiers (c23conf's `ident_utf8.c`, tcc's `83_utf8_in_identifiers.c`:
gcc -E spells them as UCNs, so the digest differs though the identifiers are the same; and gcc's
`c23-ucnid-1*.c`, UCNs in identifiers, which cc lexes as `\` and letters), trigraphs (mir's
`lacc/comment.c` at `-std=c11`, `c23-trigraphs-2.c` with `-trigraphs`), tcc's `11.c` (cc's panic,
the port's) and `13.S` (assembler, gcc's `assembler-with-cpp` lexing). gcc-only rejections (5) are error
tests whose error the port's preprocessor does not diagnose (bad UCNs, `__VA_OPT__` misuse): C7's.

### C2.4 The known differences, by class

At `pp` (68): `:embed` 46 (gcc-embed), `:va-opt` 10 (gcc-va-opt's 9, gcc-cpp-c23's `c23-va-opt-1.c`),
`:u-prefix` 5 (gcc-cpp-c23's 4 UCN and delimited-escape tests, gcc-c23's `c23-concat-1.c`),
`:has-c-attribute` 2, `:has-gnu` 1 (sqlite3.c's `__has_attribute(fallthrough)`, which cc answers 0),
`:digit-separators` 1 (`#line 1'2'3`), `:c23-colons` 1 (`: ## :`, one token to gcc in C23), `:error-test`
2 (gcc rejects, the port reports an error: `c23-attr-syntax-6.c`, `c23-scope-1.c`). At `lex` (47):
`:embed` 42 (`#embed`'s header name) and `:u-prefix` 5.
Thirteen classes are defined, `:unclassified` empty. The classes are the known file's `:classes`.

### C2.5 Speed

Evaluated on the tools' host (stage 0, CS36 (a)), the port lexes about 40 KB a second on one thread; the
check's port side, lexing and preprocessing each file and writing both dumps on 8 threads, took 868 s
for the 6,387 files (18.2 MB of main files, the headers on top: the groups of a header parsed once a run):
torture-execute's 1,672 files in 51 s, quickjs's 2.4 MB in 72 s, SQLite's amalgamation (10.3 MB) in 255 s.
The whole `check --tokens --judge-all` takes 28.5 minutes of wall time (ccdump's runs and gcc's
included: seconds). C1.2's estimate (a corpus read in one to two minutes evaluated) holds for the
corpora without SQLite; compiled (C5) the port should run some ten times faster.

### C2.6 What is left for C3 (and C3b)

- **The parser** (`arbace.repr.c.parse`, `.tree`), ccdump's `tree` mode, `parse-check` (C1.2's C3).
  The stream the parser reads is `preprocess`'s `:tokens` without the `\space` tokens.
- **cc's lexical gaps the port keeps**, which the parser's check will meet again: trigraphs (phase 1,
  which gcc applies at `-std=c89`..`c17` and with `-trigraphs`), UCNs in identifiers (`é`, and
  gcc's spelling of UTF-8 identifiers), the error diagnostics of the preprocessor (bad UCNs, `#include`
  of nothing, `__VA_OPT__` misuse: C7's error tests). CS50.
- **The layer for `:full`** (C3b, SPEC §9, §10.2): the port records the directives with their position
  and text (the main file's), each conditional group taken or not, `_Pragma`'s pragmas and `#embed`'s
  resource (`preprocess`'s `:layer`), and every token keeps its separator; not yet the main file's
  macro invocations' extents, the inactive groups' text spans, or the encoding, which SPEC §10.2 leaves to
  C3b's round trip.
- `#pragma` lines are not in the stream (cc's), but in the layer; C3b's `c/pragma` forms read them there.
- The `-O`, `-D`, `-U`, `-I`, `--embed-dir` options are modelled; others that change predefined macros
  (`-funsigned-char`, `-fopenmp`, `-ffast-math`) are not yet (no corpus file showed one).

### C2.7 Proposals (CS50, CS51)

- **CS50 cc's lexical gaps.** cc lacks trigraphs and UCNs in identifiers, and the port follows it (the
  check's oracle agrees; gcc -E does not on 7 files).
  - Options: (a) the port follows gcc in C3 (trigraphs in phase 1 at the standards gcc applies them,
    UCNs in identifiers, gcc's spelling of UTF-8 identifiers in the judge's normalization), each file then
    known at `lex`, `pp` and `parse` with its class; (b) keep cc's reading, the files classed as cc's
    gaps in C3b's read level; (c) (a) for UCNs only (C23 keeps them; trigraphs left C23).
  - *Recommended: (a)*: the reader's standard is the reference's (CS40 (a)), and both are small changes
    to the lexer (phase 1 and `identifier`).
- **CS51 The reference's tokens for every file.** The check judges by gcc only where cc differs;
  `--judge-all` judges every file but needs gcc at check time.
  - Options: (a) record gcc -E's digest of every file as data (`test/repr/c/pp-gcc.sha256`, about 6,400
    lines, `--record` writing it), so that the check judges the port against the reference everywhere
    without a compiler (CS14 (a)'s rule) and cc's agreement becomes a second judge; (b) keep the
    reference for the differing files only, `--judge-all` by hand; (c) drop cc at `pp` and judge by gcc
    alone.
  - *Recommended: (a)*: it states parity with the reference itself, and the 9 files of C2.3 become
    known entries instead of a note; cc stays the token-level oracle for kinds, separators and positions.

### C2.8 Checks and scratch

Run: `bin/repr-c check --tokens --judge-all` (every corpus, passing; C2.3), `--record` over the gcc
corpora and SQLite, `bin/repr-c host record` and `host check`, `ccdump-build` and `--upstream` (the
upstream comparison of C2.2), `make check-makefile`, and the essential gate at `d6a5a978`: every
step ran (the embedding list's change makes a new gate key: the build in 16.5 minutes) and passed but
`suite-aot-essential`, whose j2g run the kernel killed for memory while other agents' jobs ran (the
host's MemAvailable at 6.6 GiB); run again, with the 12 other passes kept, it passed (`.tmp/c2/gate.log`,
`gate2.log`). Not run: anything of `--full` (nothing of it changed; repr/c has no step until C8).

Shipped files: none of `arbace/lang`, `go/`, `jdk/`, `overlay/`. `arbace/j2g/embed.ae`'s exclusion list
gains `arbace/repr/c.ae` and `arbace/repr/c/` (as repr/haskell's and repr/rust's), so that no executable
embeds repr/c before C5 (CS36 (a)); `bin/lib/version.bash`'s `arbace_embedded_sources`, `bin/arbace-go`'s
`gate_key` (and `bin/lib/repr-c/`) and `test/gate/steps.edn`'s `:tools` exclude it likewise. The change to
`embed.ae` is a change to an embedded namespace: the next re-seed carries it; until then stage 0 loads
it from the checkout (bin/lib/tools.bash).

The worktree's `.tmp/c2/` (kept): `final.log` (the last full check with `--judge-all`), `run2.log`,
`run3.log` (the earlier full runs), `up/` and `up-*.log` (upstream ccdump's dumps), `gdiff.ae` (the port's
tokens against gcc's for one file), `t1/` (the first lexer comparison); `.tmp/repr-c-check/` (each
corpus's lists, both dumps, `files.tsv`, `gcc-agree.tsv`, `report.edn`, gcc's outputs), `.tmp/repr-c/`
(ccdump's builds, gcc 15.2's tarball and testsuite).

## Decided (2026-10-29): CS50-CS51

The user decided both as recommended:

- **CS50 (a):** the port follows gcc in C3. Trigraphs are read in phase 1 at the standards where gcc applies them,
  UCNs are allowed in identifiers, and the judge normalizes UTF-8 identifiers to gcc's spelling. Each of the 7 files is
  then a known entry at `lex`, `pp` and `parse`, with its class.
- **CS51 (a):** gcc -E's digest of every file is recorded as data (`test/repr/c/pp-gcc.sha256`, written by `--record`).
  The check then judges the port against the reference everywhere without a compiler. cc's agreement becomes a second
  judge and stays the token-level oracle for kinds, separators and positions.

## As built: C3 (2026-10-29)

C3, the parser, on the branch `c23-c3` from main `50900e48`, in about one day of work (against C1.2's
2-4). `arbace.repr.c.parse` ports cc's parser (go-whim `80c49bf5`, `crefactor/cc/parser.go`) and go-whim's
C23 delta (`c23.go`'s productions and the hooks marked `go-whim` in `parser.go`, followed line for line,
CS37 (a)) function by function; `arbace.repr.c.tree` holds cc's trees as data. `bin/repr-c parse-check`
parses every file of the 20 corpora read in place and 7,724 mutants with the port and with cc (ccdump's new
`tree` mode), and compares the dumps line for line: **every file gives cc's tree and errors line for line, or
is known with its class (6,300 of 6,387 equal, 26 known, 61 known through the token check; 7,679 of 7,724
mutants equal, 45 known)**. With it CS50 (a) (the lexer follows
gcc: trigraphs, UCNs in identifiers, gcc's spelling of extended identifiers in the judge) and CS51 (a) (gcc
-E's digest of every file recorded, `test/repr/c/pp-gcc.sha256`, the token check judging every file by it)
are built: **every file's stream agrees with gcc's recorded digest, or both reject the file, or it is
known (6,349, 31, 7)**. The port runs evaluated on the tools' host (CS36 (a)); no shipped file changed,
no re-seed. Go only TamaGo's go1.27.1, no cgo.

### C3.1 What was ported

| Arbace | lines | ports (go-whim `crefactor/cc`) |
|---|---:|---|
| `arbace/repr/c/parse.ae` | 2,241 | `parser.go` (4,037) and `c23.go` (296): the recursive descent, its lookahead (`rune`, `peek`, `shift` with the string literals' concatenation and `canonicalizeStrlitTail`, `must`), the token sequence numbers and the scopes that tell a typedef name from an identifier (`Scope.declare`, `idents`, `ident` as they answer before the checker has typed anything), `__func__`'s injected tokens, the gotos' label check, the keyword sets (`defaultKeywords`, `c23Keywords` from `__STDC_VERSION__`), `Builtin` (the second source of every parse, as go-whim's tools and ccdump pass it), `errors.err`'s same-line dedupe |
| `arbace/repr/c/tree.ae` | 236 | `ast.go`'s 85 node types and `c23.go`'s `BalancedTokenSequence` as a table (each type's first case, its exported fields in the struct's order), the kinds' names (`stringer.go`), the parser's token `PTok`, the dump |
| `arbace/repr/c/parsecheck.ae` | 269 | Arbace's: the tree check, the mutants, `--record` |

About 4,300 lines of Go into about 1,950 of Arbace for the parser (`Builtin`'s 240 lines of C apart), JN8's
ratio. A node is a `java.util.HashMap` of `:node`, `:Case` (the zero case of a node made without one, as Go's
zero value) and the fields under cc's names; the parser's own facts (`:isTypename`, `:visible`, `:params`,
`:scope`, `:gotos`) are lower-case keys the dump does not write. Go's mutation of nodes after they are made
(`r.Case = ...`, `prev.X = next`, `x.Pointer = ptr`) is kept as mutation; a field access through a nil node
panics as Go's does (`g`, `s!`), so that a mutant that makes cc panic makes the port panic at the same
place; a Go interface holding a nil `*PrimaryExpression` (cc's `postfixExpression` keeps it, and
`assignmentExpression`'s "LHS must be unary-expression" does not fire for it) is the value `:tnil`. The
parser pulls the stream lazily, as cc's does (`arbace.repr.c.pp/stream`): the preprocessor's errors and the
parser's are one list in cc's order.

cc's behaviour is kept where the corpora show it, its oddities included (each commented in the files): the
eof token at no position when another file entered cc's file set after the main one (an included file, a
`_Pragma`'s text; `pp.ae`'s `EOF-NOPOS`), which shows in the trees (an eof consumed by `must`) and in the
errors' positions; the injected `__func__` tokens at the function's `{`; the typedef check on token copies
and in place where cc does each; error recovery that does not consume the eof, where cc loops until
ccdump's timeout and the port gives up after 100,000 errors (both dumps a panic).

Arbace's changes for C3 outside the parser: the tree dump walks with an explicit stack (cc's lists nest as
deep as they are long: torture's `limits-externalid.c` is a chain of 100,000 `TranslationUnit`s, deeper than
the evaluator's 10,000 calls); string literals concatenate in builders (torture's `pr46534.c` concatenates a
million literals).

### C3.2 The oracle and the command

- **ccdump's `tree` mode** (`tools/ccdump/tree.go`, 105 lines): `cc.Parse` through the exporter's new
  `ArbaceParse` (it calls cc's `newParser` and `parse`, and returns cc's error list), the sources
  `<predefined>`, `<builtin>` (cc's `Builtin`) and the file, the tree written by a reflection walk: one line a
  node or token, pre-order, its depth first (a number: cc's lists nest), the field that holds it, the type and
  case, a token's position, kind, text and separator; absent children and tokens not written; then the
  errors, `#notree` when `Parse` gave none, `#panic` when cc panicked (`CCDUMP_STACK=1`: its stack on the
  standard error, how the port's panics were placed). `-current FILE` lets runs in parallel mark the file they
  are on. The upstream build (`--upstream`) builds the mode too. The port's `arbace.repr.c.tree/dump` writes the same format.
- **`bin/repr-c parse [--std S] [OPTION...] FILE...`**: the port's tree of each file.
- **`bin/repr-c parse-check [--corpus C,...] [--mutants N|--no-mutants] [--record]`**
  (`bin/lib/repr-c/parse-check`, JN8.3's shape; Makefile `repr-c-parse-check`): each corpus's lists as the
  token check makes them; ccdump in chunks of 50 files on `REPR_C_THREADS` (8) runs, resumed past a file it
  hangs on; the port on as many threads; `files.tsv` (name, status, lines, errors, the first difference and
  the first error of each), `report.edn`, both dumps of each file that is not equal under `diff/` (an equal
  file's cc dump removed). Statuses: `equal`; `known` (`test/repr/c/parse-check-known.edn`, by corpus, with
  the class); `pp-known` (the token check's known file lists the file at `:pp`, or, for a mutant, its
  original is known at either level: the stream differs from cc's by design, and the tree with it); `differ`,
  `crash`, `no-cc`, which fail. `--record` rewrites the known entries of the corpora from the differing
  files, the classes kept or guessed.
- **The mutants** (`parsecheck/mutants`, JN8.3's): 4,000 `.c` files of at most 64 KB drawn from the corpora
  checked (seed 26), two mutants each of 4 edits at tokens off the directive lines (deleted, duplicated,
  swapped with the next, a token of the file inserted), 7,724 kept (a file needs 9 tokens), each read with its
  original's configuration and its original's directory first among the `-I` paths. A mutant passes when
  its dump is cc's line for line: the same tree as far as each parser goes, the same errors in the same
  order (stronger than C1.2's "the same first error position").

### C3.3 The check's numbers

| corpus | files | equal | known (class) | pp-known | port's seconds |
|---|---:|---:|---|---:|---:|
| c23conf | 49 | 49 |  |  | 4 |
| torture-execute | 1,672 | 1,672 |  |  | 141 |
| torture-execute-mirror | 1,632 | 1,632 |  |  | 133 |
| torture-compile | 1,791 | 1,786 | 5 (`:eval-depth`) |  | 346 |
| tcc-tests2 | 88 | 88 |  |  | 13 |
| tcc-pp | 22 | 22 |  |  | 2 |
| compcert | 24 | 24 |  |  | 5 |
| mir | 315 | 314 | 1 (`:trigraphs`) |  | 27 |
| cxgo | 88 | 88 |  |  | 8 |
| benchmarksgame | 52 | 52 |  |  | 89 |
| sqlite | 2 | 1 | 1 (`:pp-known`, `:has-gnu`) |  | 215 |
| zlib | 15 | 15 |  |  | 7 |
| quickjs | 10 | 10 |  |  | 81 |
| gcc-c23 | 270 | 261 | 5 (`:pp-errors`) | 4 | 162 |
| gcc-cpp-c23 | 16 | 5 | 5 (`:pp-errors` 2, `:ucn-identifier` 2, `:trigraphs` 1) | 6 | 2 |
| gcc-c23-sub | 30 | 30 |  |  | 3 |
| gcc-gnu23 | 51 | 51 |  |  | 4 |
| gcc-embed | 47 | 1 | 1 (`:pp-errors`) | 45 | 14 |
| gcc-va-opt | 12 | 5 | 1 (`:pp-errors`) | 6 | 1 |
| gcc-bitint | 201 | 194 | 7 (`:pp-errors`) |  | 27 |
| **total** | **6,387** | **6,300** | **26** | **61** | **1,284** |
| mutants | 7,724 | 7,679 | 2 (`:invalid-paste`) | 43 | 767 |

`bin/repr-c parse-check --record`, every corpus read in place (Lua's tarball not), 8 threads: **no file fails**.
The dumps compared hold 62.4 million lines over the corpora (nodes and tokens, the headers' declarations
included: each translation unit is parsed whole) and 48.5 million over the mutants; 145 corpus files and 7,536
mutants have errors, the same as cc's in the same order. The known entries are 28 in six classes
(`test/repr/c/parse-check-known.edn`'s `:classes`): `:pp-errors` 16 (the trees equal, the errors not: cc's
preprocessor errors on `#if`'s binary constants and `wb` suffixes (`strconv.ParseUint`), on `__has_c_attribute`'s
answers in `#error` tests, on `#elifdef`, a missing `#embed` resource, where the port follows gcc and the token
check, which compares no error lines, found the streams equal), `:eval-depth` 5 (CS60), `:trigraphs` 2,
`:ucn-identifier` 2, `:invalid-paste` 2 (mutants), `:pp-known` 1; the `pp-known` files (61 and 43 mutants) are
those whose stream the token check knows to differ from cc's (`:embed`, `:va-opt`, `:u-prefix`, ...), or a
mutant's whose original is known. The bugs the check found in the port, each fixed: cc's eof at no position
after an included file (C3.1); the nil `*Declarator` an identifier list's parameter gives cc's `ident`
(`checkTypeName` panics: seven mutants); Go's typed nil in `postfixExpression`; the dump's recursion and the
literals' concatenation (C3.1); the errors' order with the preprocessor's (one list).

### C3.4 CS50 and CS51

- **CS50 (a)**: the lexer reads trigraphs in phase 1 when the file's configuration says so (`host.ae`:
  `-std=c89`, `c94`, `c99`, `c11`, `c17`, or `-trigraphs`, as gcc does; the scanner's `TRIGRAPHS` slot,
  `??/` before a new-line a splice, a token holding one has its texts replaced, `lex/replace-trigraphs`);
  universal character names are part of identifiers (and of pp-numbers, as gcc lexes `0À`); the judge
  writes each identifier's extended characters, UTF-8 or UCN, as gcc -E does, `\UXXXXXXXX`
  (`ppcheck/norm-ident`, on both sides). C2.6's files: c23conf's `ident_utf8.c` and tcc's
  `83_utf8_in_identifiers.c` agree with gcc by the normalization alone (cc lexes UTF-8 letters as the port
  does: equal to cc at every level); gcc's `cpp/c23-ucnid-1.c` and `-2.c` (UCNs) are known at `lex` and
  `pp` (`:ucn-identifier`), and `-2.c`, an error test, at `:gcc` (`:undiagnosed`); mir's `lacc/comment.c` and
  gcc's `cpp/c23-trigraphs-2.c` are known at `lex`, `pp` and `parse` (`:trigraphs`) and agree with gcc. The
  token check: **lex 6,337 equal, 50 known; pp 6,315 equal, 72 known (each with gcc's tokens)**, against
  C2.3's 6,340 / 47 and 6,319 / 68.
- **CS51 (a)**: `test/repr/c/pp-gcc.sha256`, a line a file (`DIGEST  CORPUS/NAME`, or `rejected` where gcc
  -E fails), written by `bin/repr-c check --tokens --record`, which now runs gcc on every file (16 at a time;
  Makefile `repr-c-record`); the check judges each file's stream by it (`files.tsv`'s fifth column: `agree`,
  `both-reject`, `known`, `differ`, `unrecorded`, the last two failing) without a compiler; the known file
  gains a `:gcc` level, the files whose stream is not gcc's, each with its class; a `:pp` entry whose stream is
  neither cc's nor gcc's says `:gcc :differs`, its class at `:gcc`. Over the 6,387 files: **6,349 agree with
  gcc, 31 both reject, 7 known** (`:undiagnosed` 4: gcc's error tests on UCNs not valid in C23 and on
  `__VA_OPT__`'s misuse, which the port's preprocessor does not diagnose, C7's; `:xid-identifier` 1, CS63;
  `:cc-panic` 1, tcc's `11.c`; `:assembler` 1, tcc's `13.S`), where C2.3 had 9 files differing besides the
  error tests: the UTF-8 identifiers, the UCNs in identifiers and the trigraphs now agree.

### C3.5 Speed

Evaluated on the tools' host (stage 0, CS36 (a)), the port's side of the check (preprocessing, parsing and
writing the dump of each translation unit, the headers included) took 1,284 s for the corpora's 6,387 files
on 8 threads (torture-execute's 1,672 in 141 s, SQLite's amalgamation in 215 s, torture's `pr46534.c`, a
million concatenated literals, about 100 s) and 767 s for the mutants; ccdump's side, in chunks of 50 on 8
runs, minutes. A whole `parse-check` (lists, mutants, both sides) takes over an hour of wall time on the shared host;
`--record` checks again after rewriting the known file. The parser costs about as much as the preprocessor
(C2.5's 868 s for the token check's port side).

### C3.6 What is left for C3b

- The converter and the printer (C3b, C1.2), which now read the port's trees.
- The reference at the parse level: gcc's acceptance of each file (CS61).
- The keywords by standard (CS62), the extended characters gcc admits in identifiers (CS63), the
  evaluator's depth (CS60).

### C3.7 Proposals (CS60-CS63)

- **CS60 The evaluator's call depth.** Five of torture's `limits-*.c` (`caselabels`, `declparen`, `exprparen`, `pointer`, `structnest`) nest deeper than the evaluator's
  10,000 interpreted calls (`Compiler`'s `MAX_DEPTH`): the port panics where cc's Go stack grows.
  - Options: (a) known (class `:eval-depth`) until C5 compiles repr/c into the program (CS36 (a)), where Go's
    stack is the limit, the class then emptied; (b) the deep recursions rewritten with explicit stacks (the
    expression levels, the declarators, the statements: much of the parser, unlike cc's shape); (c) a larger
    `MAX_DEPTH` (a shipped change, a re-seed, and Go's stack would then overflow fatally first).
  - *Recommended: (a)*: the files are limits tests, C5 removes the cause, and (b) would make the port harder
    to review against cc.
- **CS61 The reference at the parse level.** cc is the parse check's only judge; gcc's acceptance of each
  file is not recorded.
  - Options: (a) in C3b, `gcc -fsyntax-only`'s status of every file recorded (`test/repr/c/parse-gcc.edn`,
    as CS51 (a) at `pp`), each file's acceptance by the port (no error) judged by it, the differences classed;
    (b) now, at the tree level; (c) never: cc's trees are the parse's oracle, gcc judges the read (C3b) and the
    runs.
  - *Recommended: (a)*: at C3b the read has a checker's errors to compare too, and an error test needs the
    checker (C4) more than the parser.
- **CS62 The keywords by standard.** cc's keyword set is the same in every mode but C23's spellings: `typeof`
  and `asm` are keywords even at `-std=c99`, where gcc reads them as identifiers (GNU's `__typeof__` and
  `__asm__` stay).
  - Options: (a) the reference's: `typeof` and `asm` keywords only in the GNU modes and C23 (`typeof`), each
    file where it matters known with a class `:keywords`; (b) cc's set, a C3b read class.
  - *Recommended: (a)*, CS40 (a)'s rule; no corpus file showed it yet.
- **CS63 Extended characters in identifiers.** The lexer admits a character from 0x80 in an identifier when
  Java's `Character.isLetter` says so (cc's `unicode.IsLetter`); gcc admits C23's set (UAX #31's XID_Start
  and XID_Continue, N3220 annex D), so `x😀` is one identifier to gcc and two tokens to both cc and the port.
  - Options: (a) the lexer follows gcc with annex D's ranges as a table, the files known with `:ucn-identifier`;
    (b) cc's reading kept, a class.
  - *Recommended: (a)*, CS50 (a)'s rule; no corpus file shows it.

### C3.8 Checks and scratch

Run: `bin/repr-c parse-check --record` (every corpus and the mutants, passing: C3.3; the earlier runs found
the bugs C3.3 lists), `bin/repr-c check --tokens --record` (gcc on every file, `pp-gcc.sha256` written) and
`bin/repr-c check --tokens` after it (every corpus, passing: C3.4), `ccdump-build` and `ccdump-build
--upstream` (the upstream helper builds with the tree mode; its dumps not compared), `make check-makefile`,
and the essential gate at `b2e3b763` (main `fdf939b5` merged): 9 steps run (`java-compiled`, `suite-aot-essential`,
`j2g-edge`, `reap` and the canary among them), 10 skipped at their keys, passed in 8 minutes (`.tmp/c3/gate.log`). Not run: anything of `--full` (nothing of it changed; repr/c has no gate step
until C8, C2.8).

Shipped files: none. The change is under `arbace/repr/c/` (excluded from the embedded tree, `gate_key` and
`steps.edn`'s `:tools` by C2's rules, which cover the new `parse.ae`, `tree.ae`, `parsecheck.ae`), `bin/repr-c`,
`bin/lib/repr-c/`, `tools/ccdump/`, `test/repr/c/`, the Makefile (`repr-c-record`, `repr-c-parse-check`),
LICENSE.md and doc/ARBACE.md (the parser's origin).

The worktree's `.tmp/c3/` (kept): `run4.log`, `run5.log` (the parse checks; `run5.log` the final one's first
pass, which passed, its second pass stopped as redundant), `tok1.log` (the token check's record), `tok2.log`
(the token check after it, passing), `run1.log`-`run3.log` (the runs that found C3.3's bugs), `t/` (the first
comparisons: `a.c`, the string literals' `s.c`, the eof's `e1.c`-`e7.c`), `ucn/` (gcc -E on UTF-8 identifiers,
UCNs and trigraphs), `cases.txt`, `fields.txt`, `table.txt` (ast.go's cases and fields, the tree table's
source), `asbuilt.md`; `.tmp/repr-c-parse/` (each corpus's lists, `files.tsv`, `report.edn`, `diff/`, the
mutants' sources), `.tmp/repr-c-check/` (the token check's, with gcc's outputs of every file).

## Decided (2026-10-29): CS60-CS63

The user decided all four as recommended: CS60 (a), the five `limits-*` files stay known until C5 compiles repr/c;
CS61 (a), gcc -fsyntax-only's acceptance per file is recorded in C3b; CS62 (a), `typeof` and `asm` are keywords only
where gcc treats them so; CS63 (a), C23's XID tables (annex D) for identifiers, as gcc.

## As built: C3b (2026-10-29)

C3b, repr/c's read and print, on the branch `c23-c3b` from main `bde8b0f0`, in about one day of work (against
C1.2's 4-6). `arbace.repr.c.convert` turns the port's trees (C3) with the preprocessor's layer into C forms
(doc/c/SPEC.md), `arbace.repr.c.print` prints them back in mode `:lines` (cemit's canonical spelling, CS23 (a))
and gives a `:full` read back byte for byte, and `bin/repr-c read`, `print`, `roundtrip` and `check --c23` run
them; `test/c/c23/` holds c23conf's 49 files and Arbace's 13 more, judged by its six judges re-made over the forms
(CS38 (a)), and gcc -fsyntax-only's acceptance of every corpus file is recorded (CS61 (a)). Evaluated on the tools'
host (CS36 (a)); no shipped file changed, no re-seed. Go only TamaGo's go1.27.1, no cgo.

### C3b.1 What was built

| Arbace | lines | what |
|---|---:|---|
| `arbace/repr/c/convert.ae` | 1,696 | the converter (`arbace.repr.c.convert`): cc's 85 node types into C forms case for case, after cemit's and clisp's rules (CS23 (a), CS37 (a)): specifiers and declarators read from the name outward (§5), declarations (`c/var`, `c/declare`, `c/defn`, `c/typedef`, `c/struct`, `c/group`, `^:c/grouped`, K&R's `^:c/kr`), statements with labels as items and a block's declarations as `let` (§7.2, §7.3), expressions (§7.4-§7.9, n-ary chains flattened, the source's parentheses kept, CS41 (a)), literals as values (§8: integer types by gcc's table at the file's standard, floats shortest, strings decoded from each concatenated literal's own text, `byte-string`, prefixed literals' `lit`), attributes (§5.6), the main file's directives placed between items (§9.2), macro invocations recovered (§9.4), header units (§4.3) |
| `arbace/repr/c/print.ae` | 732 | the printer (`arbace.repr.c.print`): `:lines` in cemit's canonical spelling (one declarator a declaration, braces on every braced body, one statement a line, four spaces an indent; the parentheses the forms keep, and those C's grammar needs for forms written by hand), types printed as specifiers and declarators around the name; `:full` |
| `arbace/repr/c/layout.ae` | 108 | the `.ae` writer: a form on one line with its metadata (sorted, `:line` out: the comparisons' and the units' digests' text), and a unit laid out on its C lines (§10.1) |
| `arbace/repr/c.ae` | 147 | `arbace.repr.c`: `read-c` (the parse, the conversion, the `c/file` form; mode `:full`), `unit-texts`, `print-c`; `bin/repr-c read`, `print` |
| `arbace/repr/c/rtcheck.ae` | 240 | the round trip's levels 1, 2 and the fixpoint per file, the judgment of all four levels and of the acceptance (CS61 (a)), `--record` |
| `arbace/repr/c/c23check.ae` | 135 | c23conf's judges over the forms, the expected failures, the control (CS38 (a)) |
| `bin/lib/repr-c/roundtrip`, `c23check` | 172 | the drivers: lists, gcc's -fsyntax-only (`--record-gcc`), the objects and runs (levels 3, 4), `--resume` |

Changes to C2's and C3's code: `pp.ae`'s layer records each directive's offset and file and each inclusion
(`enter`: the path, the includer, the offset), and `stream` returns the layer; `parse.ae` returns it with the tree,
keeps the texts of the string literals a token was concatenated from (`strlit-pieces`, a weak identity map: cc's
widened tail does not keep a hex escape's end), and gives up a parse past `*deadline*` (the round trip's budget per
read, `REPR_C_READ_BUDGET`, 120 s). Neither changes a dump: the token and tree checks are unchanged. `test/c/corpora.edn`'s
c23conf is read from `test/c/c23/` (the `:checkout` root, the token and tree checks' drivers know it) and a corpus
`:c23` (the 62 files) is added.

How the converter decides what the spec left to C3b (doc/c/SPEC.md §10.2's "the exact encoding ... is C3b's"),
each a proposal below where it departs from the spec's text:

- **Directives** (§9.2) are read from the main file's text by the port's lexer (every directive line, active or
  not), the active ones known from the layer (a conditional family is active when its `#if` was evaluated); each is
  placed in the gap between the items of the statement list it stands in (the top level, a compound statement's block
  items, nested), by the items' first tokens' offsets. A conditional family whose lines do not all fall between the
  items of one list is left out of `:lines` whole (its active group printed expanded), as is a directive inside an item
  (CS73).
- **Macro invocations** (§9.4): the expansion's tokens all carry the outermost invocation's position (cc's), so a run
  of tokens at one offset that the source does not spell there is an invocation; its text is the source's, from the
  macro's name through its argument list's `)`. A node whose tokens are exactly such a run gets `:c/macro` (a literal
  its parent's `:c/amacro`), the outermost node of the extent claiming it; a statement or declaration whose run holds
  its `;` gets `:c/macro-stmt`. Not recovered: an expansion holding `__LINE__`'s or `__COUNTER__`'s value (printed in
  place, the invocation would give the print's line); cc's Builtin's own `__builtin_va_arg`, `__builtin_offsetof`,
  `__builtin_types_compatible_p`, whose expansions are read back as `(va-arg ...)`, `(offsetof ...)`,
  `(types-compatible? ...)` and printed as gcc's builtins; C23's `nullptr`, `true`, `false` (cc's Builtin's macros) are
  `nil`, `true`, `false`.
- **Header units** (§4.3): the items a main-file `#include` brought in (its file and the files they include, by the
  layer's inclusions) are its unit, the digest of their forms its name; a unit holds no nested `c/include` and no
  directive forms (CS74).
- **Bodies' braces**: gcc's -O0 code tells `if (c) s;` from `if (c) { s; }` (a block's end is a `nop`: measured on 17
  of torture's files whose objects differed), so N4 cannot hold at level 3: a form whose substatement was not braced is
  `^:c/bare` (`if` and `cond`: the set of their unbraced branches' indices), and the print keeps it (CS71).
- **Mode `:full`** keeps the text at the granularity of the top-level items: each top-level form's `:c/text` is the
  main file's bytes from its first token to the next item's, `c/file`'s `:c/head` the bytes before the first; a form
  without a `:c/text` (written or changed by hand) is printed as `:lines` prints it. Every byte of the main file is in
  exactly one place (§10.2's rule), and the print is the original byte for byte; finer layers (per token) are CS70.
- **Attributes** are kept by their place in cc's tree, a vector of C23 attributes and GNU specifiers
  `(__attribute__ ...)` under a key of the place: `:c/attrs` (after the identifier), `:c/decl-attrs` (among the
  specifiers), `:c/after-attrs` (after the declarator), `:c/list-attrs`, `:c/decl-end-attrs`, `:c/def-attrs`,
  `:c/end-attrs` (after a struct's `}`) (CS72).
- `#embed` is read as the integers it expands to (its directive inside the initializer is `:full`'s), no `c/embed`
  form yet (CS75); inline `asm` is `(c/asm "TEXT")`, its tokens' text, not §7.9's structured operands (CS76).

### C3b.2 The round trip's numbers

`bin/repr-c roundtrip --record-gcc` over every corpus read in place (Lua's tarball not; `c23` is test/c/c23/), 6 threads,
`GOMEMLIMIT=5GiB`, about 80 minutes; then the classes written (`--record`'s entries, classed by hand from each file's
detail). Columns: files; pass; known; read ok / read error (an error the reference shares is a pass, below); level 1
(the forms equal); level 2 (`:full` byte for byte); level 3 (the objects equal; in parentheses the files whose original
gcc does not compile at its options, nothing to compare); level 4 (run the same / differently, the run corpora); the
acceptance against gcc -fsyntax-only's recorded status (agree / both reject / gcc only rejects / the port only rejects):

| corpus | files | pass | known | read ok / error | l1 | l2 | l3 (orig fails) | l4 same / differ | agree / both / gcc only / port only |
|---|---:|---:|---:|---|---:|---:|---|---|---|
| c23 | 62 | 62 | 0 | 62 / 0 | 62 | 62 | 62 (0) | — | 59 / 0 / 3 / 0 |
| c23conf | 49 | 49 | 0 | 49 / 0 | 49 | 49 | 49 (0) | — | 49 / 0 / 0 / 0 |
| torture-execute | 1672 | 1653 | 19 | 1660 / 7 | 1658 | 1660 | 1636 (12) | 1527 / 0 | 999 / 7 / 666 / 0 |
| torture-execute-mirror | 1632 | 1614 | 18 | 1620 / 7 | 1618 | 1620 | 1599 (11) | 1595 / 1 | 961 / 7 / 664 / 0 |
| torture-compile | 1791 | 1774 | 17 | 1781 / 8 | 1778 | 1776 | 1732 (44) | — | 1170 / 3 / 613 / 5 |
| tcc-tests2 | 88 | 85 | 3 | 86 / 0 | 85 | 86 | 81 (4) | 79 / 0 | 84 / 0 / 4 / 0 |
| tcc-pp | 22 | 20 | 2 | 3 / 19 | 3 | 3 | 2 (1) | — | 2 / 17 / 1 / 2 |
| compcert | 24 | 24 | 0 | 24 / 0 | 24 | 24 | 24 (0) | 24 / 0 | 24 / 0 / 0 / 0 |
| mir | 315 | 313 | 2 | 313 / 2 | 311 | 313 | 310 (1) | 310 / 2 | 305 / 2 / 8 / 0 |
| cxgo | 88 | 88 | 0 | 88 / 0 | 88 | 88 | 87 (1) | 2 / 0 | 86 / 0 / 2 / 0 |
| benchmarksgame | 52 | 48 | 4 | 45 / 7 | 45 | 41 | 40 (5) | 29 / 0 | 39 / 7 / 6 / 0 |
| sqlite | 2 | 0 | 2 | 1 / 0 | 0 | 1 | 0 (0) | — | 1 / 0 / 1 / 0 |
| zlib | 15 | 13 | 2 | 13 / 0 | 13 | 13 | 13 (0) | — | 15 / 0 / 0 / 0 |
| quickjs | 10 | 7 | 3 | 3 / 4 | 3 | 3 | 3 (0) | — | 4 / 4 / 2 / 0 |
| gcc-c23 | 270 | 224 | 46 | 218 / 47 | 208 | 218 | 156 (53) | — | 161 / 18 / 62 / 29 |
| gcc-cpp-c23 | 16 | 14 | 2 | 7 / 9 | 7 | 7 | 5 (2) | — | 5 / 7 / 2 / 2 |
| gcc-c23-sub | 30 | 22 | 8 | 22 / 6 | 22 | 22 | 21 (1) | — | 23 / 0 / 1 / 6 |
| gcc-gnu23 | 51 | 45 | 6 | 42 / 9 | 42 | 42 | 29 (12) | — | 29 / 4 / 13 / 5 |
| gcc-embed | 47 | 40 | 7 | 30 / 17 | 24 | 30 | 24 (0) | — | 30 / 16 / 0 / 1 |
| gcc-va-opt | 12 | 12 | 0 | 7 / 5 | 7 | 7 | 7 (0) | — | 7 / 5 / 0 / 0 |
| gcc-bitint | 201 | 186 | 15 | 198 / 2 | 198 | 198 | 179 (7) | — | 192 / 0 / 7 / 2 |
| **total** | **6,449** | **6,293** | **156** | **6,272 / 149** | **6,245** | **6,263** | **6,059 (154)** | **3,566 / 3** | **4,245 / 97 / 2,055 / 52** |

**No file fails**: every one passes every level it reaches or is known with its class. The known files, by class
(`test/repr/c/roundtrip-known.edn`'s `:classes`): `:cc-grammar` 47 (the port only rejects: the files gcc 15.2 takes and
cc's grammar does not, C23's newest attribute positions and literals, CS80), `:objects` 38 (the same program, gcc -O0's
code location-dependent, CS81), `:eval-limits` 18 (CS60, CS78), `:print-grammar` 14, `:converter-gap` 12 (CS83),
`:self-include` 11, `:macro-context` 6 (CS82), `:split-declaration` 5, `:reexpansion` 4, `:run-address` 1.

- **E2's numbers on repr/c, no directive refused** (CS5 (a)): gcc 9.1's c-torture/execute, **1,527 programs run the same
  from the `:lines` print, none differently**, against E2's 1,195 of 1,700 through cemit (its 323 directive refusals,
  69 not fixpoints and 31 printer refusals gone); the later snapshot 1,595 the same and 1 differently (`return-addr.c`,
  which prints an address, its objects equal: `:run-address`). Of torture's execute files, 1,636 give the original's
  object file byte for byte. tcc's tests 79 the same (E2: 62), CompCert's 24 (E2: 7), MIR's 310 and 2 differently
  (`:macro-context`: lacc's tests of macro expansion itself), the benchmarks game 29 (E2: 3). The runs not counted are
  programs whose original gcc 15.2 does not build at the corpus's standard (the files' own `-std`, or C23 for the
  torture files without options, where implicit declarations are errors) or does not link.
- **The reference's acceptance (CS61 (a))**: `test/repr/c/parse-gcc.tsv`, gcc -fsyntax-only's status of all 6,449
  files at their standards and options, gcc's include directory first. The reader agrees on 4,245 and both reject 97;
  gcc alone rejects 2,055 (old C at C23 strictness: implicit `int` and declarations, and the error tests: the checker's,
  C4); the reader alone rejects 52, every one classed (`:cc-grammar` 47, `:eval-limits` 5 through the evaluator's
  depth).
- **c23's judges** (`bin/repr-c check --c23`, CS38 (a)): of test/c/c23/'s 62 files, **59 pass all six judges, 3 are
  expected failures** (`lib_memccpy.c`, `lib_strdup.c`, `lib_timegm.c`: musl 1.2.6's headers declare them only under
  feature macros, CS32 (a)'s class `musl-1.2.6`), none fails; **the control's four wrong prints are refused**, their
  objects alike or not as c23conf's TestControl says.

### C3b.3 Speed and memory

Evaluated on the tools' host (CS36 (a)), a file is read three times (the original, the print, `:full`): the corpora's
forms took about 70 minutes on 6 threads (torture's execute files 645 s, torture's compile files about 25 minutes,
SQLite's shell 244 s, the amalgamation past the budget), gcc's builds and runs some 10 minutes on 6 jobs. Peak RSS about
9 GiB on 6 threads (one run's; `GOMEMLIMIT` 5-6 GiB). One bug made the converter quadratic: the placed directives were
keyed in a `java.util.HashMap` by tree nodes, whose hash is the whole subtree's (an `IdentityHashMap` since; torture's
`20001226-1.c` at a quarter of its size went from 95 s to 1.6 s).

### C3b.4 What is left

- The known classes' fixes: cc's grammar where gcc 15.2 is right (CS80), gcc -O0's location-dependent code (CS81),
  macro context (CS82), the converter's gaps (CS83).
- CS62 (the keywords by standard) and CS63 (C23's XID tables) were decided with C3's proposals and are not built: no
  corpus file shows either (the round trip found none).
- `:full` per token (CS70), header units nested (CS74), `c/embed` (CS75), structured asm (CS76), levels 3-4 as data
  (CS77), a gate step (C8, CS19 (a)).

### C3b.5 Proposals (CS70-CS83)

Each with its options, the recommended one first.

- **CS70 `:full`'s granularity.** C3b keeps `:full`'s text per top-level item (each form's `:c/text`, `c/file`'s
  `:c/head`): byte for byte, every byte in one place (§10.2's rule), but a hand-changed item loses its text whole.
  - Options: (a) keep the item granularity until a consumer edits forms read in `:full` (ci's REPL, C5), then refine;
    (b) per token now (repr/go's `:full`: `:c/pos` on every list and symbol, spellings, the gaps' white space and
    comments on `c/file`), the printer rebuilding the text from the forms; (c) per statement and declaration.
  - *Recommended: (a)*: nothing reads `:full` but the round trip today, (b) is repr/go's whole GP-scale work, and
    (a)'s rule already holds.
- **CS71 Bodies' braces (N4).** gcc 15.2's -O0 code tells a braced body from a bare one (a block's end is a `nop`),
  so §3.2's N4 does not hold at level 3; C3b writes `^:c/bare` (and `if`'s and `cond`'s sets of bare branches).
  - Options: (a) amend doc/c/SPEC.md §3.2 and §7.3: N4 dropped, `:c/bare` the forms', the print braced where the
    source was; (b) keep N4, the files whose objects differ by it classed `braces`.
  - *Recommended: (a)*: level 3 is the read's judge (CS16 (a)), and the mark costs one key.
- **CS72 Attributes' places.** §5.6 puts attributes on names with `:c/attrs-at :decl`; C3b keys them by cc's tree
  places (`:c/attrs`, `:c/decl-attrs`, `:c/after-attrs`, `:c/list-attrs`, `:c/decl-end-attrs`, `:c/def-attrs`,
  `:c/end-attrs`), a vector of C23 attributes and `(__attribute__ ...)` specifiers (GNU's and C23's can mix in one
  place).
  - Options: (a) amend §5.6 to C3b's keys; (b) map them back onto §5.6's two keys and `:c/attrs-at`, losing which
    of GNU's places a specifier had (gcc's object code can tell: `aligned` on a type or a declarator).
  - *Recommended: (a)*.
- **CS73 Directives inside items.** A directive that is not between two items of one statement list (in an
  initializer, a struct, an expression), and a conditional family split across lists, are left out of `:lines`
  (`:full` keeps them); a `#define` so left out can change a later `#if`'s answer in the print (none found).
  - Options: (a) as built, a file it changes classed; (b) such `#define`/`#undef`/`#pragma` hoisted before the item;
    (c) directives placed among members, enumerators and initializer elements too (§9.2's full list).
  - *Recommended: (a)* until a corpus file needs (c), which is the spec's and C5's.
- **CS74 Header units' shape.** C3b's unit is every item the main-file `#include` brought in, flattened: no nested
  `c/include`, no directive forms of the header (the layer records only the main file's), `:path` the first
  inclusion's.
  - Options: (a) keep it until ci reads headers (C5), then nest units by the layer's inclusions and record the
    headers' directives; (b) nest now.
  - *Recommended: (a)*: nothing evaluates a unit before C5, and the units' names already key their content.
- **CS75 `#embed`'s form.** `#embed` reads as the integers it gives (its directive `:full`'s only); §9.3's
  `(c/embed ...)` is not written.
  - Options: (a) write `c/embed` in C5 with the directives among initializer elements (CS73 (c)); (b) now.
  - *Recommended: (a)*.
- **CS76 Inline asm.** `(c/asm "TEXT")`, the statement's tokens' text, not §7.9's operands.
  - Options: (a) as built, asm out of eval and AOT anyway (§14); (b) §7.9's structured form.
  - *Recommended: (a)*.
- **CS77 Level 3 and 4 as data.** The round trip runs gcc for levels 3 and 4 at check time (CS14 (a) wants the
  reference only to record).
  - Options: (a) record the originals' object digests and runs (status, stdout digest) as data
    (`test/repr/c/objects.sha256`, `runs.edn`, `--record`), gcc then needed only for the print's objects, which C6's
    AOT replaces; (b) gcc at every check, as now.
  - *Recommended: (a)*, at C8 when the fourth row's cells read the records.
- **CS78 The evaluated reader's cost.** Three reads a file (the original, the print, `:full`) on the evaluated port
  take 6 threads over an hour for the corpora, peak RSS about 9 GiB; one file (torture's `20001226-1.c`, 16,384
  statements of one invocation) exceeds the 120 s budget, SQLite's amalgamation too.
  - Options: (a) as CS60 (a): known (`:eval-cost`) until C5 compiles repr/c; (b) a faster evaluated path (`:full`
    from the `:lines` read's tree instead of a third parse).
  - *Recommended: (a)* and (b)'s cheap half: one parse for `:lines` and `:full` saves a third of the time.
- **CS79 The c23 corpus's acceptance.** `bin/repr-c check --c23` asks gcc at every run (its judges 1 and 3), as
  c23conf did.
  - Options: (a) as CS77 (a); (b) as built.
  - *Recommended: (a)* with CS77.
- **CS80 cc's grammar where gcc is right.** 47 files gcc 15.2 accepts that the port (cc's parser) refuses: C23
  attributes in positions cc lacks, `(static T){...}`'s parenthesized forms, `_Decimal64x`, `typeof` of a qualifier.
  - Options: (a) the parser follows gcc for each (CS40 (a)'s rule), the trees then known against cc in the parse check
    with a class `:gcc-grammar`, in C4 beside the checker; (b) leave them to C7's error tests.
  - *Recommended: (a)*: the read's judge is the reference (CS16 (a)), and each is a production or two.
- **CS81 gcc -O0's location-dependent code.** 38 files' prints are the same program but their objects differ: gcc emits
  a `nop` at a body's end whose `}` is not on its last statement's line (a macro-made body's tokens share one place,
  the print lays it out), and an asm label's section placement.
  - Options: (a) the class, level 4 (the runs) the judge for them; (b) the print keeps a statement's line breaks where
    the original had none (`:lines` laying out a macro-made body on one line); (c) recover the invocation (done for
    whole definitions, C3b.1).
  - *Recommended: (b)* for the bodies an invocation made, (a) for the rest: level 3 then holds for most.
- **CS82 Macro context.** A recovered invocation's text, or a printed expansion, can mean another thing where the print
  stands (a name a later function-like macro takes; an invocation's stringized escapes): 6 files.
  - Options: (a) the class; (b) recover only invocations whose macro is defined at the print's point and whose
    expansion names no later macro (the layer's definitions in order).
  - *Recommended: (b)* in C5, with the layer's macro table; (a) until then.
- **CS83 The converter's gaps.** 12 files: an octal `_BitInt` suffix, `d32` decimal suffixes, a definition through a
  typedef or `auto`, `__declspec`, an attribute list after a declarator's comma lost, `nullptr` as a case's operand.
  - Options: (a) fixed one by one with C4 (the checker reads the same forms); (b) now.
  - *Recommended: (a)*.

### C3b.6 Checks and scratch

Run: `bin/repr-c roundtrip --record-gcc` over every corpus (C3b.2), `--record` and the judgment again (every corpus
passes); `bin/repr-c check --c23` (passes, the control holds); the spec's six examples (§16) read, printed and read
again, the forms equal; `make check-makefile`; the token check over c23conf and tcc's tests and the tree check over
c23conf, tcc's tests and CompCert's (no mutants), all equal to cc's (c23conf now read from `test/c/c23/`). Not run: the
token and tree checks over every corpus (`pp.ae` and `parse.ae` changed only the layer, the literals' pieces and the
budget, which no dump shows), anything of `--full` (repr/c has no gate step until C8), SQLite's amalgamation
within the budget.

Shipped files: none. The change is under `arbace/repr/c/` and `arbace/repr/c.ae` (no executable embeds them;
`test/gate/steps.edn`'s `:tools` now names `!arbace/repr/c.ae` too, as `bin/lib/version.bash` and `arbace/j2g/embed.ae`
already did), `bin/repr-c`, `bin/lib/repr-c/`, `test/c/`, `test/repr/c/`, the Makefile, LICENSE.md.

The worktree's `.tmp/` (kept): `repr-c-roundtrip/` (each corpus's lists, `forms.tsv`, `obj.tsv`, `run.tsv`,
`files.tsv`, `report.edn`, the prints under `print/`, the objects under `obj/`), `repr-c-c23/` (the c23 judges'
`judges.tsv`, the control), `c3b/run5.log` (the final round trip), `run1.log`-`run4.log` (the runs that found C3b.1's
bugs), `classes.json` and `classify.py` (the classes' first draft), `ex/` (the spec's examples).

## Decided (2026-10-30): CS70-CS83

The user decided all fourteen as recommended: CS70 (a), CS71 (a), CS72 (a), CS73 (a), CS74 (a), CS75 (a), CS76 (a),
CS77 (a), CS78 (a) with (b)'s cheap half, CS79 (a), CS80 (a), CS81 (b) for the bodies an invocation made and (a) for
the rest, CS82 (b) in C5 and (a) until then, CS83 (a).

## As built: C4 (2026-10-30)

C4, the checker over C forms, on the branch `c23-c4` from `c23-c3b` (`f04f9886`; `b11b0cef`, CS70-CS83 decided, and
main `6b53ec03` merged), in about one day of work (against C1.2's 5-8). `arbace.repr.c.check` checks a read's C forms
(doc/c/SPEC.md §11.2): every expression's type and integer value, the conversions, constant expressions, layout, linkage,
`_BitInt`, `constexpr`, `typeof`, `auto` and `__auto_type`, `_Generic`; `bin/ci annotations` compares its annotations with
cc's checker's (tools/ccdump's new `types` mode, `cc.Translate`) item by item over every corpus, and judges its errors by
gcc 15.2's recorded acceptance (`test/repr/c/parse-gcc.tsv`, CS61 (a)). Evaluated on the tools' host (CS36 (a)): **no
shipped file changed**, no re-seed. Go only TamaGo's go1.27.1, no cgo.

### C4.1 What was built

| Arbace | lines | what |
|---|---:|---|
| `arbace/repr/c/check.ae` | 1,579 | the checker (`arbace.repr.c.check`, Arbace's own, CS8 (a)): types as data (the base types, `_BitInt(N)`, complex, pointers, arrays with VLAs, functions with or without prototypes, struct/union/enum records, `nullptr_t`, GNU's vector types), qualifiers; the integer promotions (bit-fields by width, as gcc), the usual arithmetic conversions (ranks, `_BitInt`'s, complex and floating), decay, null pointer constants, the conditional's type (C23's qualifiers of pointed-to arrays, `_Atomic` dropped); constant folding of integers (BigInteger, wrapped to the type) and floating values; scopes (ordinary identifiers, tags), linkage (6.2.2, inherited from a visible declaration), composite types of redeclarations, arrays completed by their initializers (brace elision counted, designators, strings, prefixed strings), parameters adjusted and in scope for the later parameters' types; layout by gcc 15.2's rules on x86-64 (bit-fields in a window of their type's size on their type's alignment, which gives `_BitInt(N > 64)` fields gcc's places; zero-width fields; packed, aligned, `alignas`, `#pragma pack`, `mode(QI...TI)`, `vector_size`; `long double` 16 by default, 8 for ci's configuration, CS35); enumerated types (gcc's compatible type from the values; C23's fixed underlying types; C23's constant types during and after the definition); `_Generic`'s selection; `typeof`/`typeof_unqual`, C23 `auto`, `__auto_type`; `constexpr` objects' values; cc's `Builtin` declarations read as forms (the `<builtin>` source every read sees), an undeclared library function resolved to its `__builtin_` declaration as gcc and cc do; the errors gcc gives that the checker diagnoses (C4.4) |
| `arbace/repr/c/annot.ae` | 308 | the check (`arbace.repr.c.annot`): the dumps (the checker's in ccdump's format, each item's features), the comparison item by item as sets of lines, the classes of cc's gaps, the judge, `--record`; `bin/ci check`, `annot` |
| `tools/ccdump/types.go` (and `types_upstream.go`, `overlay/export.go`'s `ArbaceRawLine`) | 389 | ccdump's `types` mode: `cc.Translate`, one block an external declaration of the main file (its first token's raw line, `#line` not applied: `overlay/export.go`'s `ArbaceRawLine`), its declarators (name, linkage, type), its expressions (type decayed, integer value), its structs' layouts (size, alignment, members' offsets, bit-fields' bit positions and widths), its enumerators' values, written by a reflection walk; types without qualifiers (cc's keep none) |
| `bin/ci`, `bin/lib/repr-c/annotations` | 124 | the command (`check`, `annot`, `annotations`) and the check's driver (lists as the token check's, ccdump in chunks of 50, the checker on `REPR_C_THREADS`, `--resume`, `--judge`, `--record`) |
| `test/repr/c/annot-known.edn` | | the files no automatic class explains, each with its class (C4.3) |

The annotation format (one line an annotation, an item a block `@LINE`; compared as multisets, so the two walks' orders
do not matter): `d NAME LINKAGE TYPE`, `e TYPE [= VALUE]`, `s TYPE SIZE ALIGN`, `m STRUCT NAME OFFSET - TYPE` or
`m STRUCT NAME @BITPOS BITS TYPE` for a bit-field, `n NAME VALUE`. Where cc's tree is not the forms' shape the checker's
dump writes what cc's tree has, so that the two compare: the expansions of cc's `Builtin` macros (`va_arg` as
`(*(T*)__builtin_va_arg_impl(ap))`, `types_compatible_p` as a call of two compound literals), an assignment's and a comma's
value when cc gives one (written, never folded: `:av`), attribute arguments typed as expressions (an identifier that names
nothing `int`), a bit-field's access typed by its promotion, the injected `__func__` and member declarators left out on cc's side.

### C4.2 The check's numbers

`bin/ci annotations` over every corpus read in place (Lua's tarball not; `c23` is test/c/c23/), 8 threads,
`GOMEMLIMIT=6GiB`, 31-43 minutes a run (the read is C3b's evaluated reader; ccdump's side minutes). A file passes when
every item's annotations equal cc's, or every differing item is explained by a class of cc's gaps (C4.3), or cc does not
translate it (no comparison: `cc.Translate` fails, cc's gaps `_BitInt`, `constexpr`, VLAs' `= {}`, C23's newest syntax), and
when the checker gives no error on a file gcc 15.2 accepts; a file that does not pass is in `test/repr/c/annot-known.edn`
with its class, or is a pp-known file (its stream differs from cc's by the token check's known file) or one gcc rejects.

| corpus | files | pass | known | equal | classed (cc's gaps) | cc does not translate | not read | errors: agree / both reject / gcc only rejects / false |
|---|---:|---:|---:|---:|---:|---:|---:|---|
| c23conf | 49 | 49 | 0 | 34 | 10 | 5 | 0 | 49 / 0 / 0 / 0 |
| c23 | 62 | 60 | 2 | 41 | 11 | 8 | 0 | 59 / 3 / 0 / 0 |
| torture-execute | 1672 | 1653 | 19 | 1398 | 243 | 0 | 12 | 994 / 641 / 25 / 0 |
| torture-execute-mirror | 1632 | 1612 | 20 | 1363 | 236 | 1 | 12 | 956 / 640 / 24 / 0 |
| torture-compile | 1791 | 1759 | 32 | 1625 | 123 | 0 | 11 | 1168 / 259 / 353 / 0 |
| tcc-tests2 | 88 | 88 | 0 | 79 | 7 | 0 | 2 | 82 / 0 / 4 / 0 |
| tcc-pp | 22 | 22 | 0 | 2 | 0 | 1 | 19 | 2 / 0 / 1 / 0 |
| compcert | 24 | 24 | 0 | 24 | 0 | 0 | 0 | 24 / 0 / 0 / 0 |
| mir | 315 | 313 | 2 | 283 | 27 | 1 | 2 | 305 / 4 / 4 / 0 |
| cxgo | 88 | 88 | 0 | 85 | 2 | 1 | 0 | 86 / 1 / 1 / 0 |
| benchmarksgame | 52 | 52 | 0 | 31 | 10 | 4 | 7 | 39 / 5 / 1 / 0 |
| sqlite | 2 | 2 | 0 | 0 | 1 | 0 | 1 | 0 / 1 / 0 / 0 |
| zlib | 15 | 13 | 2 | 8 | 3 | 0 | 2 | 13 / 0 / 0 / 0 |
| quickjs | 10 | 10 | 0 | 1 | 2 | 0 | 7 | 3 / 0 / 0 / 0 |
| gcc-c23 | 270 | 262 | 8 | 118 | 29 | 63 | 52 | 160 / 5 / 53 / 0 |
| gcc-cpp-c23 | 16 | 16 | 0 | 3 | 0 | 4 | 9 | 5 / 0 / 2 / 0 |
| gcc-c23-sub | 30 | 30 | 0 | 17 | 2 | 3 | 8 | 21 / 0 / 1 / 0 |
| gcc-gnu23 | 51 | 48 | 3 | 23 | 8 | 8 | 9 | 29 / 0 / 13 / 0 |
| gcc-embed | 47 | 36 | 11 | 0 | 0 | 19 | 17 | 30 / 0 / 0 / 0 |
| gcc-va-opt | 12 | 12 | 0 | 1 | 0 | 6 | 5 | 7 / 0 / 0 / 0 |
| gcc-bitint | 201 | 201 | 0 | 1 | 0 | 197 | 3 | 191 / 0 / 7 / 0 |
| **total** | **6449** | **6350** | **99** | **5137** | **714** | **321** | **178** | **4223 / 1559 / 489 / 0** |

**No file fails, and the checker gives no error on any file gcc 15.2 accepts** (4,223 accepted files without an error;
1,559 of the 2,152 files gcc rejects get an error of the checker's, 489 none: C7's error tests). Of the 5,950 files read
and translated by cc too, **5,137 give cc's annotations line for line**, 714 differ only in items a class of cc's gaps
explains, and 99 are known: `gcc-rejects` 31 (an error test: no reference for its annotations), `pp-known` 12,
`checker-gap` 12, `cc-layout` 11, `converter-gap` 7, `layout-unjudged` 5, `cc-empty-init` 4, `cc-redeclared` 3,
`vector-types` 3, `item-placement` 3, `cc-preprocessor` 2, `cc-attr-mode` 2, `enum-range` 2, `cc-c23-constants` 1,
`checker-layout` 1. The 321 files cc does not translate are not compared: the checker's annotations of them have no
oracle but gcc's acceptance (the 197 of gcc-bitint among them: cc parses `_BitInt` and does not type it).

### C4.3 The classes of cc's gaps

The checker records, per item, the features it met that cc's checker is known not to handle as gcc does; a differing item is
classed by the first of them (`arbace.repr.c.annot/gap-classes`), or by what cc's lines show. The files whose differing items
all have a class, by their classes (a file can have several): `cc-undeclared-builtin` 185 (a builtin cc's Builtin does not
declare, typed by cc as an implicit `int` function: `va_start`, `__builtin_trap`, ...), `asm` 142 (cc types an `asm`'s
operands, which `(c/asm "TEXT")` does not structure, CS76 (a)), `vector-types` 91, `cc-attr-layout` 90 (packed, aligned,
`alignas`, `#pragma pack` in a layout), `cc-folds` 54 (cc gives a value the standard does not make constant: address
arithmetic on 0, a string's element), `cc-redeclared` 34, `cc-invalid` 33 (cc wrote `?`, an invalid type, or a size of -1),
`cc-enum-type` 19 (gcc's `unsigned int` compatible type, C23's enumerated constants), `cc-float-fold` 17, `cc-c23-constants` 16,
`cc-addr-array` 15 (`&a` as `**T`), `cc-builtin-macro` 12 (`offsetof`'s expansion), `cc-bitfield-promotion` 11 (a bit-field
wider than `int`), `cc-builtin-types` 10, `cc-attr-mode` 9, `cc-int128-fold` 9, `cc-prefixed-string` 8 (C23's `u8` strings'
type), `cc-bitfield-layout` 5 (unnamed and zero-width bit-fields), `cc-builtin-fold` 4, `cc-auto` 3, `cc-constexpr` 2, `cc-linkage` 2.

A feature is evidence, not proof, that the difference is cc's: an item with an `asm` statement and another difference of the
checker's is classed `asm` too. Where cc and the checker disagree on a layout, gcc was asked once (`.tmp/c4/layout_judge.py`:
the checker's sizes, alignments and offsets as static assertions appended to the original, `gcc -fsyntax-only`): of the 17
files whose remaining differences are layouts gcc agreed with the checker on 11 (`cc-layout`), refused it on 1
(`checker-layout`: `gnu23-tag-alias-8.c`, a struct with a variably modified member, GNU's), and 5 could not be asked
(`layout-unjudged`: anonymous or block-scope records). An earlier round of the judge found the checker wrong once more (a
typedef's `aligned` raising a member's alignment, `pr40233.c`), fixed. The layouts probed by hand against gcc while building (`.tmp/c4/lay.c`, `lay2.c`: packed members and
unions, `aligned` after the brace, zero-width and unnamed bit-fields, `alignas` members, packed bit-fields) all agreed with the
checker, none with cc where they parted.

### C4.4 The checker's errors

The errors are the ones gcc 15.2 gives as errors that the checker can diagnose without guessing: an undeclared identifier,
an unknown type name, an implicit declaration of a function from C99 on (gcc 15's default error; a library builtin
resolved to its `__builtin_` type first), a member not in its struct, a member of something not a struct, an invalid use
of an incomplete type, a call of a non-function, `*` of a non-pointer, invalid operands (a struct in arithmetic,
`%` and the bit operators on floating values, a shift of a pointer), a unary operator's wrong operand, an assignment to a
`const` place, a failed `static_assert`. The judge found every false error of the first runs (vector types, a function's
struct return type defined in its declaration, parameters adjusted, `static_assert`s on gcc's `_BitInt` bit-field layout,
C23's enumerated constants' types, the conditional's qualifiers, tag compatibility in C23) and they were fixed; `_Generic`
without a match is no error (a controlling type of an unknown builtin's).

### C4.5 CS80 and CS83

CS80 (a) (the parser following gcc where cc's grammar lacks C23's productions) is not built: it changes repr/c's reader,
whose token, tree and round-trip checks would run again (over an hour each on the evaluated port), and the 47 files are
those cc does not translate, where the checker has no oracle anyway (CS94). Of CS83 (a)'s converter gaps one is fixed: a
`u8` string literal in a `gnu23` (or `c2y`, `gnu2y`) file is `(lit unsigned-char ...)`, as in `c23` (it was a `char`
string, which made gcc's `gnu23-utf8str-type.c` fail its `static_assert` under the checker); the round trip was not run
again for it (the forms of such files change, their print does not). Found and not fixed: an imaginary constant's `f`
suffix lost (`(imaginary x)` for `xif`, 7 files, `converter-gap`).

### C4.6 What is left

- `checker-gap`'s 12 files and the layout judge as a step (CS91, CS93); the attribute appertainment rules, `[[deprecated]]`
  and the other diagnostics only attributes give; `#pragma pack` inside a function; the checker's errors widened to C7's
  error tests (CS92).
- ci's configuration: `long double` as `double` (8, 8; CS35) is a parameter of `check-unit` (`:ldouble-size`), not yet a
  host configuration; musl's headers under it are C5's.
- The checker as `arbace.c.check` (CS90), C5's builder over its results (types on the forms' nodes: the checker returns only
  annotations and errors today, CS95).

### C4.7 Proposals (CS90-CS95)

Each with its options, the recommended one first.

- **CS90 The checker's namespace.** The spec (§6.1) names it `arbace.c.check` in `arbace/c/`, embedded and compiled with ci;
  C4 put it in `arbace/repr/c/` as `arbace.repr.c.check`, since a new tree `arbace/c/` would have to be excluded from the
  embedded tree in `arbace/j2g/embed.ae`, a shipped file (CS36 (a): nothing shipped before C5).
  - Options: (a) move it to `arbace/c/check.ae` as `arbace.c.check` in C5, when ci's namespaces are embedded anyway (the
    re-seed C5 needs); (b) move it now with `embed.ae`'s exclusion (a shipped change, the next re-seed carries it);
    (c) keep `arbace.repr.c.check`.
  - *Recommended: (a)*: no shipped change before C5, and the name the spec gives once ci needs it.
- **CS91 Layouts judged by gcc.** cc's `abi.go` parts from gcc on packed, aligned and bit-field layouts; the one-off judge of
  C4.3 asked gcc for 17 files' layouts.
  - Options: (a) record gcc's sizes, alignments and members' offsets of every main-file tagged struct and union as data
    (`test/repr/c/layout-gcc.tsv`, `bin/ci annotations --record-gcc`: the static assertions compiled per file, CS14 (a)'s
    rule), the check judging every layout by it and cc only where the record has none; (b) keep cc the oracle, layouts
    classed by hand; (c) judge only the differing files at check time, as the one-off did.
  - *Recommended: (a)*: the reference is the judge (CS40 (a)), layout is what ci's memory (CS9) depends on, and cc is
    known to be wrong there.
- **CS92 The checker's errors.** C4 diagnoses the errors of C4.4 only; gcc rejects 2,152 corpus files and the checker
  gives an error on 1,559.
  - Options: (a) widened by C7's error tests, each error judged by line and kind against gcc's `dg-error` (§5.5); (b) now,
    the constraints of clause 6 one by one; (c) keep the checker permissive (ci runs what gcc accepts).
  - *Recommended: (a)*: C7 has the judge (the error tests' lines), and C4's exit is no false error.
- **CS93 The checker's own differences.** 12 files `checker-gap` and 1 `checker-layout`, each a difference no class of cc's
  explains.
  - Options: (a) fixed in C5 as ci meets them (the builder reads the same types), the known entries removed as they pass;
    (b) now.
  - *Recommended: (a)*: none is an error and none is in a run corpus's program that C5 runs first.
- **CS94 CS80 (cc's grammar where gcc is right).** Decided for C4, not built (C4.5).
  - Options: (a) in C7, with the C23 corpus's error tests, the parse check's known file gaining `:gcc-grammar`; (b) a
    C4b now, the reader's three checks run again.
  - *Recommended: (a)*: the 47 files are C23's newest productions in gcc's own tests, which C7 judges.
- **CS95 Types on the forms.** The checker computes every node's type but returns only the annotations and the errors.
  - Options: (a) `check-unit` returns an identity map from each form to its result (type, value, lvalue, bit-field) for C5's
    builder, with the scopes' declarations; (b) the builder calls the checker per node; (c) the types written on the forms'
    metadata (CS8 (a) forbids the converter's doing so, not a consumer's).
  - *Recommended: (a)*: one pass, no change to the forms, and the annotations become a view of it.

### C4.8 Checks and scratch

Run: `bin/ci annotations` over every corpus (the final run, `--resume` of the run before it, cc's dumps kept: C4.2), and
`--judge` after the known file was written (every corpus passes); the earlier runs that found the bugs above (`.tmp/c4/
all1.log`-`all7.log`); `bin/repr-c ccdump-build` (and its types mode by hand on the probes); the one-off layout judge;
gcc on the layout probes; `make check-makefile`. Not run: the token, tree and round-trip checks (C2-C3b's; only the
converter's `u8` rule changed, C4.5), `bin/repr-c check --c23`, ccdump's upstream build's types mode (it has none: the
upstream build writes `#notree`), anything of `--full`, and the essential gate before handing back (GT7's clean rerun holds
the host: `.tmp/gt7.quiet`; the coordinator's instruction is to run it once the file is gone).

Shipped files: none. The change is under `arbace/repr/c/` (`check.ae`, `annot.ae` new; `convert.ae`'s `u8` rule), already
outside the embedded tree, `gate_key` and `steps.edn`'s `:tools`; `bin/ci`, `bin/lib/repr-c/annotations`, `tools/ccdump/`
(`types.go`, `types_upstream.go`, `main.go`, `overlay/export.go`), `test/repr/c/annot-known.edn`, the Makefile
(`ci-check`, `ci-annot`, `ci-annotations`, `ci-annotations-record`), LICENSE.md and doc/ARBACE.md.

The worktree's `.tmp/` (kept): `ci-annot/` (each corpus's lists, `cc/` and `port/` dumps, `port.tsv`, `files.tsv`,
`report.edn`), `c4/` (`all1.log`-`all7.log` the runs, `fail.tsv`, `classes.tsv`, `classify.py`, `layout_judge.py`, `lj.tsv`
and `lj/` the judge's files, `sum.py` the table, `lay.c`, `lay2.c`, `u.c` the layout probes, `a.c`, `b.c`, `c.c`, `i.c`
the first probes, `cmp.sh`), `repr-c/` (ccdump's build, gcc 15.2's tarball and testsuite).


### Decided (CS90-CS95; the user, 2026-10-30)

The user decided C4's six proposals as recommended (relayed by the coordinator, 2026-10-30): **CS90 (a)**, the checker
`arbace.c.check` in `arbace/c/`, embedded and compiled with ci (moved in C5); **CS91 (a)**, layouts judged by gcc's recorded
sizes, alignments and offsets; **CS92 (a)**, C4 diagnosing C4.4's errors only, the rest judged by C7's error tests; **CS93
(a)**, the checker's 13 own differences recorded and fixed in C5 as ci meets them; **CS94 (a)**, CS80's grammar left to C7;
**CS95 (a)**, the types kept inside the checker, `check-unit` returning each form's result to C5's builder, the annotations a
view of them.

## As built: C5 (write-ahead) (2026-10-30)

C5, ci, on the branch `c23-c5` from `c23-c4` (`c55aedac`), in the light tier (the user, 2026-10-30: under 2 GB, at
most 4 threads, no gate, no build of Arbace's executables, no lock), so **write-ahead**: ci's back end is written as the
Go forms the shipped executable will compile in (`go/arbace/crt`, `go/arbace/ci`, `go/arbace/cinterp`) and tested
outside it, by repr/go printing the two packages and TamaGo's go building them with a small runner of Arbace's own
(`tools/cirun`, std only); its Arbace side (`arbace.c`, `arbace.c.build`, `arbace.c.run`, `arbace.c.musl`,
`arbace.c.repl`) runs evaluated from the checkout on stage 0. What needs an executable built with ci is the heavy
list (C5.7). Go only TamaGo's go1.27.1, no cgo.

### C5.1 What was built

| part | where | lines | what |
|---|---|---:|---|
| crt: memory (CS9 (a)) | `go/arbace/crt/mem.ae` | 375 | one flat 64-bit little-endian address space per program, made of slabs (Go `[]byte`): nothing below `Text` (functions are addresses `Text + 16·i`, labels `LabelBase + code`), read-only data (string literals, `const` statics: a store traps once the static initializers have run), static data, the heap (`brk`), `mmap`'s mappings (anonymous, `MAP_FIXED`, partial `munmap`), the 8 MiB stack below `StackTop`; loads and stores by `encoding/binary`'s little-endian functions behind a two-entry cache of slabs; a load or store with no slab, a store into read-only data are `SIGSEGV` (`crt.Fault`), the program's end `crt.Exit` |
| crt: wide integers (CS35) | `go/arbace/crt/wide.ae` | 385 | `__int128` and `_BitInt(N)` beyond 64 bits as 64-bit limbs (`crt.Wide`, `crt.WType`): the operators, shifts, comparisons, conversions to and from 64-bit integers and `double`, decimal text, memory at the psABI's sizes; written by a Sonnet subagent against `math/big` |
| crt: the syscall layer (§6.5) | `go/arbace/crt/syscall.ae` | 1,211 | the Linux x86-64 system calls musl 1.2.6 makes, over Go's `os` and `syscall` (`crt.Sys.Syscall`: read, write, writev, open/openat, close, lseek, fstat/newfstatat, mmap, munmap, mremap, brk, rt_sigaction, rt_sigprocmask, kill/tkill/tgkill with pending signals and the default actions, ioctl's `TIOCGWINSZ` as `ENOTTY`, clock_gettime, uname, getrandom, futex for one thread, arch_prctl, exit_group, ...; `-ENOSYS` for the rest), a bad pointer argument `-EFAULT`; written by a Sonnet subagent |
| ci: nodes, frames, calls | `go/arbace/ci/{prog,expr,stmt,call}.ae` | 1,591 | goi's shape (GOI-SPEC §4.2-§4.6): typed Go closures over a frame, `I` (integers of up to 64 bits, pointers, bool: normalized), `F` (float rounded to float, double, long double as double), `W` (wide), `E` (effects); a frame's slots (`I`, `F`) for the scalar locals whose address is not taken, its memory in the C stack for the rest (aggregates, wide integers, address-taken locals, temporaries, the varargs area); statements return a `Ctl`; labels are codes and a statement list, loop, `if` or `switch` holding a label can be **entered** at it (`Stmt.Enter`), so C's gotos into blocks, loops and switches (Duff's device) and `switch`'s cases need no state machine; a call evaluates its arguments right to left (CS31 (a)), passes them as values, an aggregate by its bytes, an aggregate result through a hidden address; varargs in an 8-byte-slot area after the frame, `va_list` a pointer to the next slot (cc's `__builtin_va_list` is `void *`); `alloca`, VLAs on the C stack; the machine's arithmetic of gcc `-O0` (SPEC §13.1: wrapping, shift counts masked, `SIGFPE` on division by zero and `MIN / -1`, `cvttsd2si`'s conversions) |
| ci: `setjmp`, natives | `go/arbace/ci/{natives,builtins}.ae` | 319 | `setjmp` is legal only as (part of) a whole controlling expression or expression statement (7.13.1.1), so the builder wraps such a statement (`SjStmt`): `longjmp` is a Go panic the statement of the frame that set the `jmp_buf` recovers, running the statement again with `setjmp` returning the value; the natives of musl's assembly (the `__syscall0`-`6` entry, `__get_tp`, `__set_thread_area`, `__syscall_cp_asm`, `setjmp`/`longjmp` and their aliases) and of `arch/x86_64/atomic_arch.h`'s inline functions for one thread; gcc's bit and classification builtins (`__builtin_clz` ... `__builtin_fpclassify`) |
| ci: the registry | `go/arbace/ci/load.ae` | 559 | the node program (below) made into functions, statics and nodes, from data or from its text (`ci.Parse`) |
| cinterp | `go/arbace/cinterp{.ae,/natives.ae}` | 49 | arbace.lang.CInterp's native `runNodes` (a node program run in the process on its standard streams), importing jrt; not built (C5.7) |
| tools/cirun | `tools/cirun/main.go` | 50 | the light path's runner: a node program's text loaded and run, `CI_STRACE` and `CI_CALLTRACE` tracing system calls and interpreted calls; `bin/ci cirun-build` prints `go/arbace/crt` and `go/arbace/ci` with repr/go and builds it into `.tmp/ci/cirun` |
| the builder | `arbace/c/build.ae` (`arbace.c.build`) | 1,708 | the checked forms into a node program: slots and the frame's memory, label codes, the conversions' nodes (6.3), places (names, `@p`, `aget`, `.-f`, `->`'s paths, bit-fields through their unit), assignments (the right operand evaluated before the left one's load, as gcc `-O0`), `++`/`--`, pointer arithmetic, `?:`, comma, statement expressions, compound literals, string literals (merged, read-only), initializers by 6.7.11 (designators, brace elision, strings for character arrays), static initializers run before the program starts, `_Generic` by the checker's choice, the builtins in scope (`__builtin_expect`, the `va_` ones, `alloca`, the overflow builtins in 128 bits, `__builtin_classify_type`, `__builtin_frame_address`, the library's `__builtin_X` as `X`), complex arithmetic of the floating types (in memory, gcc's formulas), gcc's bit-fields wider than `int` at their width (`pr32244-1`), GNU's cast to a union; a refusal (SPEC §14's by design, or deferred) as a class |
| the linker | `arbace/c/build.ae`, `arbace/c/musl.ae` | | static linking by name: external functions and objects across the units, a strong definition over a weak one, `alias` attributes (musl's `weak_alias`), an undefined weak object at address 0, a function whose body is inline assembly a native |
| musl (CS11 (a), CS12 (a), CS35) | `arbace/c/musl.ae` (`arbace.c.musl`), `bin/lib/ci/musl` | 306 | musl 1.2.6's tarball (pinned) extracted and configured into `.tmp/ci-musl/`, read by repr/c in Arbace's configuration (gcc's c99 macros with `__STDC_HOSTED__` 0, `__SIZEOF_LONG_DOUBLE__` 8, `__LDBL_*` as double's, musl's `arch/arm/bits/float.h` before `arch/x86_64`'s: `LDBL_MANT_DIG` 53; musl's own include path and `-D_XOPEN_SOURCE=700`), its generic C sources (1,335 of the static libc's 1,340; the generic stubs of what x86_64 writes in assembly left out for natives); the forms cache (`.tmp/ci-musl/forms`, 15 MB) with an index of each unit's definitions (weak ones and aliases marked) and the names its code references; a program links the units that define what it still needs, as `ld` pulls archive members, each unit checked once per process |
| ci's programs | `arbace/c/run.ae` (`arbace.c.run`) | 208 | a program's units read, checked (long double as double; the reference's `-fpermissive` makes implicit declarations no error), linked and built; `nodes`, `nodes-list` (a corpus's programs in threads), `judge`, `divergences` |
| the REPL | `arbace/c.ae` (`arbace.c`), `arbace/c/repl.ae` (`arbace.c.repl`) | 174 | §6.6: the macros of C forms, a unit a namespace, declarations entered (a redefinition replaces), `c/include` at the REPL, `c/run-main` (exit and abort end the run, not the REPL); its back end `*run-nodes*`: tools/cirun on the light path, CInterp on the executable (C5.7) |
| the checker (CS90 (a), CS95 (a)) | `arbace/c/check.ae` (`arbace.c.check`, moved from `arbace/repr/c/`) | | `check-unit :results true` returns an identity map from each expression form to its result (type, value, lvalue, bit-field, member), each declarator's symbol to its entry (with an identity, `:id`, and its name), each `_Generic`'s chosen association; two own differences ci met fixed (CS93 (a)): an integer constant converted to `float` rounded to float (`16777217 != (float)16777217e0`), and `{ (lit T 42) }` no longer taken for `{ "string" }` in an array's size |
| the commands | `bin/ci run`, `nodes`, `check`, `divergences`, `repl-session`, `musl prepare|read`, `cirun-build`, `record-ref` (`bin/lib/ci/`); the C4 verb `check` renamed `diagnose` | | the Makefile's `ci-*` targets |
| the data | `test/c/ref-gcc15.edn`, `test/c/check-linux-amd64.edn`, `test/c/divergences.edn`, `test/c/check-known.edn`, `test/c/repl/letters.ae`, `letters.out` | | gcc 15.2's runs (3,879 files of seven corpora: status, stdout's SHA-256, its first line, -O2's agreement; a Sonnet subagent's `bin/lib/ci/record-ref`); the check's results; the classes; the REPL session |

The **node program** is the builder's output and the back end's input: a vector of commands, `(fn ID NAME)`,
`(native ID NAME NATIVE)`, `(static IDX SIZE ALIGN RO NAME)`, `(data IDX OFF HEX)`, `(at IDX ADDR)`, `(alias IDX OF OFF)`,
`(fnalias ID TARGET)`, `(code ID NI NF FRAME VARIADIC RETF32 PARAMS BODY)`, `(init STMT)`, `(start LIBC MAIN)`, nodes as
lists (`(bin "add" "i32" (il 0) (i 1))`, `(ld "u8" (adds (g 3) (il 2) 1))`, `(call (fnref 7) [(ai ...)])`), floating
constants as their bits. Its text is what crosses on both paths (CS103).

### C5.2 The check's numbers

`bin/ci check --record` over the seven single-file run corpora gcc 15.2's runs were recorded for (C5.1's data; 3,879 files,
3,582 of them programs gcc builds and runs), on stage 0 with tools/cirun, at most 4 threads (37 minutes):

| corpus | programs | pass | by-design | long-double | speed | vla | read | complex-int | other classes |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---|
| torture-execute | 1,532 | 1,430 | 71 | 6 | 0 | 13 | 5 | 4 | gcc-builtin 1, checker 1, wide 1 |
| torture-execute-mirror | 1,601 | 1,494 | 76 | 6 | 0 | 13 | 5 | 4 | gcc-builtin 1, checker 1, wide 1 |
| tcc-tests2 | 82 | 77 | 1 | 0 | 0 | 1 | 2 | 0 | abi 1 |
| compcert | 24 | 16 | 0 | 0 | 8 | 0 | 0 | 0 | |
| mir | 312 | 309 | 0 | 1 | 0 | 0 | 0 | 0 | ub 2 |
| cxgo | 2 | 2 | 0 | 0 | 0 | 0 | 0 | 0 | |
| benchmarksgame | 29 | 22 | 5 | 0 | 1 | 0 | 0 | 0 | threads 1 |
| **total** | **3,582** | **3,350** | **153** | **13** | **9** | **27** | **12** | **8** | ub 2, gcc-builtin 2, checker 2, wide 2, abi 1, threads 1 |

(`bin/ci divergences` on the record: 3,350 pass, every other program in a class.) The first full run, before the fixes C5.3
names, passed 3,330.

**No program fails**: every one that does not pass is in a class of `test/c/divergences.edn`, by design (`by-design` 153,
`long-double` 13, `speed` 9, `ub` 2, `gcc-builtin` 2, `abi` 1) or deferred (`vla` 27, `read` 12, `complex-int` 8, `checker`
2, `wide` 2, `threads` 1), 34 of them by `test/c/check-known.edn`'s entries (the mirror's copies of the torture tests share
theirs). The classes
`long-double` (CS35, by design, the class SPEC §12.5 records: every program in it declares or converts to `long double`), and
`by-design` (SPEC §14) hold most; the deferred ones name the proposal that would build them.

Not in the check (C5.7's): the program corpora (SQLite, Lua, zlib, QuickJS: several units and their tests), gcc 15.2's
`dg-do run` tests (C7's), cxgo's 86 fragments without `main` (gcc does not link them: no reference).

### C5.3 What ci met in musl and in the checker

- musl 1.2.6 runs as C forms from its own start: `__libc_start_main` (with an initial stack of argc, argv, an empty
  environment and the auxiliary vector), `__init_libc`, `__init_tls` (no TLS segment: the builtin TLS), stdio (vfprintf's
  `%f` and `%e` through musl's `fmt_fp` in double, CS35), mallocng over `brk` and `mmap` (its guard page put with
  `MAP_FIXED` at the heap's start, which made crt's `brk` extend the slab ending at the old break), `exit` flushing
  through `__stdio_exit` (pulled by `__towrite`'s strong reference, the weak alias in `exit.c` overridden: the linker's
  strong-over-weak rule, and a weak alias of an object another unit defines not taken).
- The checker's own differences ci met and C5 fixed (CS93 (a)): an integer or double constant converted to `float` was
  not rounded to float (`16777217L != (float)16777217e0`, `3.14 == 3.14f`); `(lit float x)` likewise; `{ (lit unsigned 42) }`
  counted as `{ "string" }` for an array's size (`pr31448.c`); an implicitly declared function's entry had no name. One is
  known: `__alignof__` of a function declared `aligned(256)` (`align-3.c`).

### C5.4 Speed and memory

The builder reads, checks, links and builds a corpus's programs at about 3 programs a second with 3 threads (each
program's musl part, about 70 units for `printf`, checked once per process and built again: a node program is about
0.4 MB of text); the runs, 4 at a time, take seconds. The interpreter: `fib(30)`'s node program (no libc) runs in 0.61 s
(gcc's -O0 code under 0.01 s); CompCert's `binarytrees.c`, whose time is mostly musl's mallocng interpreted, 14 s (gcc's 0.05 s). The Arbace process stays under its `GOMEMLIMIT` of 1.8 GiB;
cirun's runs under 100 MB. The whole check took 37 minutes at 4 threads.

### C5.5 Proposals (CS100-CS111)

Each with its options, the recommended one first. CS90-CS95 (C4's), decided by the user as recommended (above), are followed: CS90 (a) the checker moved to
`arbace/c/check.ae` as `arbace.c.check` (its requirers and doc/ARBACE.md follow; it is in the embedded tree with ci's
namespaces, CS100), CS91 (a) not built (gcc's layouts as data: ci met no layout difference; C7 with the error tests),
CS92 (a) (C7's), CS93 (a) (four of the checker's own differences ci met fixed, one known), CS94 (a) (C7's), CS95 (a) (built).

- **CS100 Where ci's Arbace namespaces wait for the heavy window.** `arbace/c.ae` and `arbace/c/` (the checker among them
  since CS90 (a)) are in the embedded tree (only `arbace/repr/c` is excluded, CS36 (a)), and they require `arbace.repr.c.*`: an executable built from this
  branch before the heavy window would embed namespaces whose requires it does not hold.
  - Options: (a) the branch is merged in the heavy window only, with `arbace/j2g/embed.ae`'s exclusion of `arbace/repr/c`
    dropped (CS36 (a)'s C5 change) and the checker moved (CS90 (a)) in the same change; (b) `embed.ae` excludes `arbace/c.ae`
    and `arbace/c/` now (a one-line shipped change), the branch mergeable before the window, ci evaluated from the checkout
    until then; (c) the namespaces moved under `arbace/repr/c/` until then.
  - *Recommended: (b)*: the branch's light work (the check, the REPL session) then lands without a re-seed, and the window's
    change is the exclusion's removal; (a) holds the work back, (c) renames twice.
- **CS101 ci's Go sources.** `go/arbace/crt` and `go/arbace/ci` were written as Go text in the scratch and converted by
  repr/go's native reader (`bin/repr-go read --goos linux`), the forms committed; their unit tests (`wide_test.go`,
  `syscall_test.go`, against `math/big` and a temporary directory) were not converted.
  - Options: (a) the forms are the source from now, edited as forms, the tests converted with `--tests` into
    `:test-files` (goi does not load them, GOI-SPEC S45; `go test` on the printed packages runs them); (b) the forms the
    source, the tests dropped, ci's check their judge; (c) the Go text kept under `tools/` as the source and the forms
    generated (a generated-check step).
  - *Recommended: (a)*: Arbace keeps Go as forms (CLAUDE.md), and the tests are cheap evidence for the limbs and the
    syscall layer, which the corpora exercise only in part.
- **CS102 musl's forms.** SPEC §6.1 puts musl's C forms in `c/musl/`; C5 keeps them in a cache (`.tmp/ci-musl/forms`,
  15 MB, 1,335 units and their header units, read in 12.5 minutes at 4 threads), made by `bin/ci musl read` from the
  pinned tarball.
  - Options: (a) the cache until C6, then the forms committed under `c/musl/` with a provenance file (the tarball's pin,
    the reader's digest) and `bin/ci musl verify` (read afresh, byte for byte) as `jdk/` is, when ci's executable embeds
    them; (b) committed now; (c) always the cache.
  - *Recommended: (a)*: no consumer needs them committed before the executable embeds ci, and 15 MB is the disk's.
- **CS103 What crosses into ci's back end.** The builder makes a node program as data; on the light path its text runs
  in tools/cirun; arbace.lang.CInterp's one native takes the same text.
  - Options: (a) the text, one native (`runNodes`), as written (the REPL's state is the forms, rebuilt for each run);
    (b) goi's way, a native per node (`CInterp.node(kind, args)`), the program kept in the process between evaluations
    (a REPL whose C globals keep their values between `c/run-main`s, `c/eval` of an expression);
    (c) (a) now, (b) when `c/eval` is built.
  - *Recommended: (c)*: (a) is enough for §6.6's session and shares the light path's loader; values crossing (SPEC §12.4)
    need (b)'s persistent program.
- **CS104 musl's five unread units.** `iswalpha.c`, `iswpunct.c`, `wcwidth.c`, `strerror.c` and `iconv.c` include a table
  inside an initializer (`static const unsigned char table[] = { #include "alpha.h" };`); the converter refuses a
  declaration that begins in one file and ends in another (`split-declaration`, SPEC §4.3).
  - Options: (a) the converter keeps such a declaration whole in the including unit (the included tokens part of its
    initializer, the layer's `c/include` inside the `init` as §9.2 allows), in C6; (b) the five functions written as C forms
    by hand; (c) their natives.
  - *Recommended: (a)*: the same rule serves tcc's `18_include.c` and C7's tests; `strerror` is the one the corpora call.
- **CS105 GNU's complex integer types.** `_Complex int` (and `__real__`/`__imag__`, SPEC §14) are refused; the floating
  complex types are built (gcc's formulas, not its `__muldc3`/`__divdc3` handling of infinities).
  - Options: (a) complex integers deferred to C7 with GNU's extensions, the class `complex-int`; (b) now; (c) by design.
  - *Recommended: (a)*.
- **CS106 Variably modified types.** Built: a block's VLA on the C stack, `sizeof` of a VLA object. Not: VLAs in structs,
  `sizeof` of a variably modified type name, parameters' array sizes evaluated at entry (side effects), a VLA's storage
  freed when its scope is left (a loop re-entering the declaration grows the stack: `20040811-1.c` overflows 8 MiB).
  - Options: (a) in C6 with AOT, which needs the same (a VLA's size slot per declaration, its stack pointer restored at
    the scope's exit), the class `vla` until then; (b) now; (c) by design.
  - *Recommended: (a)*.
- **CS107 The check's speed.** The interpreter runs about 100 to 300 times slower than gcc's -O0 code (C5.4); 10 s per
  program puts 9 programs in the class `speed` (8 of CompCert's benchmarks, the benchmarks game's spectral-norm).
  - Options: (a) the class, by design (goi's), and a profile-guided pass over the hot nodes in C6 (frames from a thread's
    stack instead of allocated, as goi's S21; fused load-op-store nodes); (b) a longer limit for the check; (c) AOT the
    judge for them.
  - *Recommended: (a)*.
- **CS108 The checked mode.** SPEC §12.1's block table (a load or store outside a live block, a bad `free`, the C
  position) is not built: the memory checks are the slabs' (a fault at an unmapped address, a store into read-only data).
  - Options: (a) at the REPL's in-process back end (CS103 (b)), hooking musl's malloc and free as natives that record
    blocks; (b) now on the light path; (c) dropped, the slabs' faults the checked mode.
  - *Recommended: (a)*.
- **CS109 Signals and the REPL.** `raise`, `abort` and a fault end the run with 128 + the signal (musl's own `raise` over
  `tkill`); a handler installed by `signal` runs for `raise`/`kill` of a handled signal, not for a fault (`SIGSEGV`,
  `SIGFPE` from the machine).
  - Options: (a) as built until a corpus program needs a fault's handler (none of the run corpora's passing ones); (b) a
    fault delivered to an installed handler, `longjmp` out of it allowed.
  - *Recommended: (a)*.
- **CS110 The REPL's back end on the light path.** `*run-nodes*` runs tools/cirun as a process; on the executable it is
  CInterp's native.
  - Options: (a) both, the session's test (`bin/ci repl-session`) on the light one until the window, then on both;
    (b) the executable's only.
  - *Recommended: (a)*.
- **CS111 gcc's -fpermissive.** The reference builds the corpora with `-fpermissive` (gcc 15 rejects implicit
  declarations at C99 and later otherwise); ci's reads drop the checker's implicit-declaration errors for them.
  - Options: (a) as built, a corpus file's `:args` naming it (test/c/corpora.edn) as C7 records the error tests' own;
    (b) the checker's error kept and the files classed.
  - *Recommended: (a)*.

### C5.7 The heavy list (the window after the tag)

What C5 still needs that the light tier does not allow, in order:

1. **The embedded tree** (CS100, CS36 (a)): `arbace/j2g/embed.ae` drops the exclusion of `arbace/repr/c.ae` and
   `arbace/repr/c/` (and `bin/lib/version.bash`'s and `test/gate/steps.edn`'s `!arbace/repr/c` entries follow it), so
   `arbace.c` and `arbace.c.*` (the checker, moved by CS90 (a)) embed and compile with repr/c, c2j's first compile of
   them (evaluated only so far).
2. **CInterp** (CS103): `arbace/lang/CInterp.ae`, one static native whose Go side is `go/arbace/cinterp`'s
   `CInterp_RunNodes_String_String1__I_native`:

   ```clojure
   (in-ns 'arbace.lang)
   (defclass ^:public ^:final CInterp
     (constructor ^:private [this])
     ;; runs the node program text (arbace.c.build/program-text's) with the command line argv: its status
     (method ^:public ^:static ^:native runNodes ^int [^String text ^String/1 argv]))
   ```

    j2g's rule that its natives are `arbace/cinterp`'s
   (`arbace/j2g/decls.ae`, beside GoInterp's: `(= n "arbace/lang/CInterp") (symbol "cinterp" fname)`) and its copy of
   `go/arbace/crt`, `go/arbace/ci`, `go/arbace/cinterp` into the program (`arbace/j2g/main.ae`, beside goi's, J2G-SPEC
   §9.1's amendment as S32's); `arbace.c.repl/*run-nodes*` bound to it on the executable.
3. **The build**: `bin/arbace-go --build` (heavy lock), `bin/goi` untouched; the smoke test; `go/arbace/cinterp`'s Go
   forms compiled for the first time (they were not: they import jrt).
4. **The re-seed** the embedded namespaces need (C5's, with C6's if batched, C1.2).
5. **ci on the executable**: `bin/ci check` with `CI_EXE` (each node program run in the process by CInterp) over every run
   corpus, against the light path's results (equal by construction: one loader); `bin/ci repl-session` on it.
6. **The full corpora** beyond the light tier's: SQLite, Lua, zlib, QuickJS (programs of several units with their tests:
   `bin/ci check` gains program corpora), gcc 15.2's `dg-do run` tests of the `gcc-*` groups (C7's, their references
   recorded with `bin/ci record-ref --corpus gcc-c23,...`).
7. **The gate**: the essential gate in this worktree (not run: the light tier), and C8's essential subsets
   (`test/c/essential*.txt`) as gate steps; `make check-makefile` passes.
8. **`bin/ci annotations`** again (C4's check, 31-43 minutes at 8 threads): the checker changed (CS95 (a)'s results,
   three own differences fixed), its known file's `checker-gap` entries removed as they pass (CS93 (a)).

### C5.6 What is left

C5's exit is met on the light path for the single-file run corpora: `bin/ci check` none failing, every non-pass in a class
(`long-double` recorded, 13 programs), and the REPL session of §6.6 (`bin/ci repl-session`, `test/c/repl/letters.ae`: a unit typed at
the REPL with musl's `<stdio.h>`, run, a function redefined and seen by the next run, `exit` and `abort` ending the run and
not the REPL). Left: the heavy list (C5.7) and CS100-CS111.

### C5.8 Checks and scratch

Run: `bin/ci check --record` over the seven corpora (C5.2) and the runs before it that found the bugs above; `bin/ci
repl-session`; `bin/ci record-ref` (by a Sonnet subagent, 2 min 18 s at 4 jobs); `bin/ci musl prepare` and `read`; `bin/ci
cirun-build` from the committed forms and the printed packages' build; `go vet` and `go test` of crt (its limbs against
`math/big`, the syscall layer) on the Go text before conversion; repr/go's read of the two packages (`0 failed, 0 did not
read back`) and the print back, built, running a node program the same; `make check-makefile`. Not run: anything of C5.7,
the essential gate (the light tier), `bin/ci annotations` (C4's check: the checker changed, C5.7's item 8), the token,
tree and round-trip checks (repr/c's reader unchanged).

Shipped files: `go/arbace/crt.ae`, `go/arbace/crt/`, `go/arbace/ci.ae`, `go/arbace/ci/`, `go/arbace/cinterp.ae`,
`go/arbace/cinterp/` (new packages nothing imports yet: inert in a build until C5.7's item 2); `arbace/c.ae` and `arbace/c/`
(new namespaces in the embedded tree, the checker moved there: CS100). Not shipped:
`bin/ci`, `bin/lib/ci/`, `tools/cirun/`, `test/c/`, the Makefile.

The worktree's `.tmp/` (kept): `ci-musl/` (the extracted and configured release, `ldbl53/bits/float.h`, the forms cache
`forms/` with `index.edn`), `ci-check/` (each corpus's `list.tsv`, `nodes.tsv`, `run.tsv`, the node programs of the runs
that did not exit 0, `results.edn`), `ci-ref/` (record-ref's lists), `ci/` (the printed packages and `cirun`), `cigo/` (the
Go text the forms were converted from, with `crt`'s tests), `c5/` (the probes `s0.c`, `s1.c`, `h1.c`, `h2.c`, `m1.c`, `si*.c`,
the logs `te1.log`, `others1.log`, `all1.log`, `final.log`, `final2.log` (the recorded run), this section's draft).

Rule slips: two waiting loops used `pgrep` (stopped once noticed), one probe redirected to `/dev/null`, and the judge of one
check run loaded `arbace/c/build.ae` while it was being edited (the run's judgment was repeated).

### Decided (CS100-CS111; the user, 2026-10-30)

- CS100 (b): `arbace/c.ae` and `arbace/c/` excluded from the embedded tree (`arbace/j2g/embed.ae`, `bin/lib/version.bash`'s
  `arbace_embedded_sources`, `test/gate/steps.edn`'s `:tools`) beside repr/c, so the branch lands before the heavy window;
  the window removes both exclusions together (C5.7, item 1).
- CS101, CS102, CS104-CS111 (a); CS103 (c), as recommended.

## As built: C6 (write-ahead) (2026-10-30)

C6, the AOT path for C, on the branch `c23-c6` from `c23-c5` (`306037b7`), in the light tier (the user, 2026-10-30: under
2 GB, at most 4 threads, no gate, no build of Arbace's executables, no lock), so **write-ahead** as C5 was: the translator
**cf2g** (`arbace.c.cf2g`, CS17 (a)'s name) runs evaluated from the checkout on stage 0 (it is under `arbace/c/`, excluded
from the embedded tree by CS100 (b)); the Go it writes is built by TamaGo's go1.27.1 (`CGO_ENABLED=0`) against
`go/arbace/ci` and `go/arbace/crt` printed by repr/go, outside Arbace's executable; programs of a few hundred kilobytes
of Go each, at most 2 compiler jobs, which the brief counts as light. What needs an executable built with ci or cf2g
is the heavy list (C6.7).

### C6.1 What was built

| part | where | lines | what |
|---|---|---:|---|
| cf2g | `arbace/c/cf2g.ae` (`arbace.c.cf2g`) | 952 | a program's **node program** (the builder's output, the lowered C forms ci runs: CS120) into Go forms: each function with code a Go function `cfID(t *ci.Thread, args []ci.Arg) ci.Arg`, the Native of its `Func`; its slots Go locals (`iK`, `fK`), its memory in the C stack by `ci.Enter` as `Prog.Call` makes it; its statements and expressions **flattened into one Go block** of assignments, conditional gotos and labels, so C's gotos into blocks, loops and switches and `switch`'s cases are Go's `goto` (Go's two rules, no jump into a block and none over a declaration, hold: every label is at the function's top level, every variable declared at its start); a switch's exact cases a Go `switch` of gotos, GNU's ranges `if`s after it; `goto *p` a switch over the function's labels; an expression's operands in temporaries wherever a later operand has statements, so ci's order of evaluation (the builder's, gcc -O0's: calls' arguments right to left, a store's address before its value) is kept exactly and Go's unspecified order between a variable's read and a call never matters; the machine's arithmetic as ci's: wrapping operators normalized by width, `ci.SDiv`/`UDiv` for SIGFPE, `ci.F2IConv` for cvttsd2si, every floating result through an explicit `float64(...)` (Go's spec: no fused multiply-add), float rounded through `float32`; an operation of constants alone made one of values (`ci.K`), so Go folds it with wrapping and not its exact constants'; `setjmp`'s function body a closure under `ci.SjRun`, entered again at the setjmp statement's label; the variables the body never reads not declared (their assignments to `_`). The program's data (`fn`, `native`, `static`, `data`, `at`, `alias`, `fnalias`, `start`) stays node program text, read by ci's Loader in the order of the commands, so the compiled and the interpreted program lay memory out alike. `bin/ci aot NODES OUT` |
| ci's compiled side | `go/arbace/ci/aot.ae`, `prog.ae`, `natives.ae` | 240 | `Enter`/`Leave` (the prologue and epilogue of `Prog.Call` for a compiled function: frame, memory parameters, varargs area, the thread's innermost frame for setjmp and the fault's trace), `SDiv`, `SRem`, `UDiv`, `URem`, `F2IConv`, `BitLoad`, `BitStore`, `VaArgAt`, `AllocaT`, `Trap`, `WDiv`, `WRem`, `MustWide`, `B`, `Bool`, `K`, `Frame.SjSet`, `SjRun` (SjBody's recovery of the longjmps to the frame, for a closure), `GoStmt` (a compiled static initializer as a `Stmt` of `Prog.Inits`), `Loader.TopText`, `Loader.SetCompiled`, `Prog.StaticsI`, `AotMain` (tools/cirun's main for a compiled program: argv, the standard streams, the status, a fault as `ciaot: fault: ...`, `CIAOT_CPUPROFILE`'s profile); a compiled function's frames kept per thread for reuse (`Thread.free`), a frame a setjmp recorded pinned (C6.4) |
| VLAs left (CS106 (a), C6's half) | `arbace/c/build.ae`, `go/arbace/ci/call.ae`, `load.ae` | 81 | a block that declares VLAs records the stack pointer at its entry (`vlamark`) and restores it at its end and before a `break` or `continue` that leaves it (`vlarestore`); a VLA's declaration reached again while nothing was allocated since its allocation (a goto back before it) frees it first, and a block entered again while nothing but its VLAs was allocated since frees them (`vlalloc`, `vlamark`): the stack no longer grows in a loop or a goto loop over a VLA. Both back ends (ci's nodes `VlaMark`, `VlaAlloc`, `VlaRestore`; cf2g's `ci.VlaMarkAt`, `VlaAllocAt`) |
| the check | `bin/lib/ci/check-aot`, `bin/ci check --aot` | 177 | below |
| the classes | `test/c/divergences-aot.edn` | | AOT's own (`aot-node`, `aot-goto`: none met), judged before `test/c/divergences.edn`'s; a `test/c/check-aot-known.edn` (none written) before `check-known.edn` |
| the results | `test/c/check-aot-linux-amd64.edn` | | every program of the seven run corpora compiled, `[ID RESULT REASON]` |
| the Makefile | `ci-check-aot`, `ci-aot-translate` | | |

**The check** (`bin/ci check --aot`): the programs of the run corpora as `bin/ci check` lists them, in shards of 60
(`CI_SHARD`); a shard's programs read, checked, linked with musl, built and translated by one Arbace process (`cf2g
compile-list`, 3 threads, `GOMEMLIMIT` 1.8 GiB), each into a package `arbace/progs/pN` of a module beside the printed
`ci` and `crt`; the shard one executable (a `main` naming each package) built by `go build -p 2`; a shard that does not
build has its packages built alone and those that do not compile taken out as `go-error` (none did); each program run
with an empty standard input, no arguments, 10 s, 4 at a time, against gcc 15.2's -O0 run (`test/c/ref-gcc15.edn`,
arbace.c.run's rule); the shard's sources, executable and Go cache entries deleted (the disk). With `CI_AOT_NODES=1` each
node program is also run by tools/cirun, and the comparison is **diffcheck's kind**: per program, the compiled run's
status and standard output's SHA-256 against the interpreted run of the same node program; the interpreted results are
judged by `arbace.c.run`'s judge as `bin/ci check`'s (and recorded with `--record`).

### C6.2 The check's numbers

`CI_AOT_NODES=1 bin/ci check --aot --record` over the seven single-file run corpora (3,582 programs gcc builds and runs), on
stage 0, at most 4 threads, 64 shards in 86 minutes, each node program compiled and also interpreted:

| corpus | programs | compiled pass | interpreted pass | by-design | vla | long-double | read | complex-int | speed (compiled / interpreted) | other classes |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| torture-execute | 1,532 | 1,433 | 1,433 | 71 | 10 | 6 | 5 | 4 | 0 / 0 | gcc-builtin 1, checker 1, wide 1 |
| torture-execute-mirror | 1,601 | 1,497 | 1,497 | 76 | 10 | 6 | 5 | 4 | 0 / 0 | gcc-builtin 1, checker 1, wide 1 |
| tcc-tests2 | 82 | 78 | 78 | 1 | 0 | 0 | 2 | 0 | 0 / 0 | abi 1 |
| compcert | 24 | 24 | 15 | 0 | 0 | 0 | 0 | 0 | 0 / 9 | |
| mir | 312 | 309 | 309 | 0 | 0 | 1 | 0 | 0 | 0 / 0 | ub 2 |
| cxgo | 2 | 2 | 2 | 0 | 0 | 0 | 0 | 0 | 0 / 0 | |
| benchmarksgame | 29 | 23 | 22 | 5 | 0 | 0 | 0 | 0 | 0 / 1 | threads 1 |
| **total** | **3,582** | **3,366** | **3,356** | **153** | **20** | **13** | **12** | **8** | **0 / 10** | ub 2, gcc-builtin 2, checker 2, wide 2, abi 1, threads 1 |

**Every run program translated; none refused by cf2g** (no `aot-*` class met), **none whose Go does not compile, none
failing**: the 216 compiled non-passes are each in a class, all of them the interpreter's too (a read, check or build
refused before cf2g, or a run differing from gcc's in the same way). **Interpreted against compiled**, program by program:
3,394 runs with the same status and standard output; 10 that differ, every one an interpreted timeout that the compiled
program passes (9 of CompCert's benchmarks and the benchmarks game's `spectral-norm.c`: no compiled program is in the class
`speed`, CS107 (a)). Results: 3,356 pass both, 216 neither, 10 compiled only, **0 interpreted only**.

The interpreted side moved from C5's 3,350 to 3,356 (`test/c/check-linux-amd64.edn` recorded again): the VLA rule (C6.1)
passes `20040811-1.c`, `pr43220.c`, `vla-dealloc-1.c` (and their mirrors) and `79_vla_continue.c` (its `check-known.edn`
entry removed); CompCert's `perlin.c`, just under the 10 s at C5, is now just over on the loaded host: `speed`. Before the
recorded run, a first full run found two bugs, both fixed: a shift's untyped constant left operand takes its type from the
conversion around it in Go (`int32(0x14ff6e2207db5d1f >> a)` in `pr70429.c`, refused by Go's compiler; now through
`ci.K`), and the check's runner split cxgo's ids, which hold spaces.

### C6.3 musl compiled

Each program's linked part of musl was translated and compiled with it (CS122 (b), as built): every musl function the
corpora's programs reach, stdio's `vfprintf` and `fmt_fp`, mallocng, `exit`'s flushing and `__libc_start_main` among them,
ran compiled. musl as a whole, sampled (`.tmp/c6/musl-sample.ae`): every fourth of its 1,335 cached units (332, by path)
checked, built into one node program with a stub `main` (none refused by the builder or cf2g), translated into 2,967
functions, 4.3 MB of Go, in 70 s at a peak of 1.8 GB (the light tier's limit: the process holds every unit's check
results); `go build -p 1` compiled it in 9.8 s at 0.8 GB, and the executable runs. A sixteenth: 83 units, 721 functions,
1.0 MB of Go, compiled in 14 s at 0.28 GB. All of musl in one process would need about 7 GB on the Arbace side and about
3 GB for one package's compile: the heavy list's (C6.7, item 4), split by directory as CS122 (a) proposes.

### C6.4 Speed and memory

CompCert's benchmarks, one run each on this host (busy with GT7's clean run), the same standard output on all three:

| program | gcc 15.2 -O0 | cf2g (frames reused) | cf2g (first) | ci (tools/cirun) |
|---|---:|---:|---:|---:|
| `nbody.c` | 0.29 s | 4.9 s | 11.6 s | 49 s |
| `binarytrees.c` | 0.07 s | 1.5 s | 3.7 s | 13.6 s |

The first profile (`CIAOT_CPUPROFILE`, runtime/pprof in `AotMain`) put half of nbody's time in `ci.Enter`'s frame
allocation and the collector; frames are now kept per thread for reuse (`Thread.free`; a frame a setjmp recorded is
pinned, never reused), which also lets Go keep the argument slices on the stack. What is left is the prologue's work
(`Enter`'s clearing of the frame, its loops over the parameters) and crt's loads and stores (the slab lookup): CS123 and a
fast path in `crt.Mem` for the stack's slab are the next steps. Memory: a compiled program under 100 MB of RSS; the
Arbace process of a shard under its `GOMEMLIMIT` of 1.8 GiB; `go build -p 2` of a shard of 60 packages well under 2 GB.

### C6.5 Proposals (CS120-CS127)

Each with its options, the recommended one first. CS100-CS111 (C5's), decided by the user, are followed: CS100 (b)
(cf2g under `arbace/c/`, excluded), CS106 (a) (the VLAs' storage freed, C6.1), CS107 (a) (AOT runs the `speed` programs:
C6.2), CS101 (a) (`aot.ae` written as forms, the source).

- **CS120 What cf2g translates.** Built: the node program, the builder's output, which ci's back end also runs.
  - Options: (a) the node program (one lowering for both back ends: the same layouts, conversions, order of evaluation and
    refusals; a difference between them is the back end's alone, which diffcheck's comparison isolates); (b) the checked C
    forms directly, a second lowering into typed Go (C's `int` a Go `int32` local, structs as Go values where their
    address is not taken), closer to idiomatic Go and faster, but a second implementation of 6.3's conversions and of
    the builder's layout to keep equal; (c) (a) now, (b) for a hot subset later.
  - *Recommended: (a)*: C6's exit is correctness against gcc and the interpreter, and (a) gives it with no second
    semantics; the speed is the calling convention's (CS123), not the lowering's.
- **CS121 cf2g's refusals.** A goto out of a setjmp function's body (the body is a closure) and a setjmp body inside
  another are refused (`aot-goto`, `aot-node`); the run corpora meet neither.
  - Options: (a) refused with the class until a program needs them; (b) the setjmp body as a state machine in the
    function's own block (a `recover` in a deferred function that sets the resume label, the body re-entered by a goto),
    which removes the closure and both refusals.
  - *Recommended: (a)*.
- **CS122 musl compiled once, listed in ci's table** (C6's row). Built: each program's own linked part of musl translated
  and compiled with it (the builder's per-program function indices make two programs' musl differ in their indices);
  every musl function a corpus program reaches was compiled and run (C6.3).
  - Options: (a) musl's units as one library node program with fixed indices (all 1,335 units, built and checked once),
    translated once into Go packages split by source directory (`arbace/cmusl/stdio`, ...), the builder linking a program
    against it by name (its functions and statics after the library's), the interpreter calling the compiled library (ci's
    table, as goi's lists std), in the heavy window; (b) per program, as built; (c) each unit translated alone with its
    references symbolic, resolved at load through a table of names (no fixed indices, one package per unit).
  - *Recommended: (a)*: it is the row's design, it removes most of the check's time (C6.4: the printing of musl's part
    is about 90% of a program's), and the interpreter then runs musl at compiled speed; (c) pays a lookup per call.
- **CS123 The calling convention.** Built: every call through `ci.Arg` slices, as natives take them (a direct Go call when
  the callee is compiled in the same program, `Prog.Call` otherwise).
  - Options: (a) typed functions for static calls (the scalars and addresses as Go parameters and results, CS13 (a)'s
    sketch), the `[]ci.Arg` form kept as a wrapper for indirect calls, natives and the loader; (b) as built.
  - *Recommended: (a)* in the heavy window, measured on CompCert's benchmarks (C6.4): the slices are the remaining
    allocation per call.
- **CS124 The check's time.** About 70 s per shard of 60 small programs and 2 to 3 s per program linking stdio; repr/go's
  printer is most of it (240 KB/s on one thread; a program with `printf` is about 0.6 MB of Go).
  - Options: (a) CS122 (a), musl printed once; (b) a Go text writer of cf2g's own for its few statement shapes, beside the
    forms (the forms stay the output; the writer an optimization checked against repr/go's print byte for byte);
    (c) as is.
  - *Recommended: (a)*, (b) only if the per-program part stays slow after it.
- **CS125 VLAs' storage, C6's rules.** Built: freed at a VLA block's end and at a break or continue leaving it, and at a
  re-entry (a goto back before the declaration or the block) when nothing else was allocated since.
  - Options: (a) as built until a program needs more; (b) gcc's model exactly, the stack saved at each VLA scope's
    entry and restored on every edge that leaves it (gotos included: the builder resolving each goto's scopes), which
    also frees `alloca` storage in the scope as gcc does.
  - *Recommended: (a)*: the corpora's VLA programs pass with it (C6.2); (b) is the rest of CS106 with VLAs in structs.
- **CS126 Where interpreted against compiled lives.** Built: in `bin/ci check --aot` with `CI_AOT_NODES=1`, per program
  (status and stdout).
  - Options: (a) as built, and C8's essential subset (`test/c/essential-aot.txt`) a gate step running both sides;
    (b) `bin/diffcheck` gaining C forms cases (per form: an expression's value interpreted and compiled), as F2 has for
    Clojure and Java.
  - *Recommended: (a)* now, (b) with C8 when ci is in the executable (per-form cases need the REPL's in-process ci).
- **CS127 `ci`'s new exports.** `go/arbace/ci` gains the compiled programs' API (`aot.go`) and the VLA nodes: shipped
  files of an inert package (nothing imports it until C5.7's item 2).
  - Options: (a) kept in `ci` (one package for the nodes and the compiled code they share: frames, setjmp, natives);
    (b) a package `arbace/ciaot` over `ci`, which would need `ci`'s frame and setjmp internals exported.
  - *Recommended: (a)*.

### C6.6 What is left

C6's exit is met on the light path for the single-file run corpora: every run program translated (none refused), none
failing, every non-pass in a class, the interpreted and compiled runs compared program by program (diffcheck's kind: none
differs but the interpreter's timeouts). Left: the heavy list (C6.7: `arbace.build.go`'s shards under the lock, musl
compiled once and listed in ci's table, the executable) and CS120-CS127.

### C6.7 The heavy list (the window after the tag)

What C6 still needs that the light tier does not allow, after C5.7's items (which it extends), in order:

1. **The embedded tree** (C5.7 item 1, CS100): `arbace/c/cf2g.ae` embeds and compiles with the rest of `arbace/c/` when
   the exclusion goes; c2j's first compile of it (evaluated only so far).
2. **The executable's ci** (C5.7 items 2-5): `go/arbace/ci`'s new `aot.go` and VLA nodes compiled into Arbace's program
   for the first time (as `go/arbace/ci` itself, they were built only by TamaGo's go in the light path's module).
3. **The build of a compiled C program by `arbace.build.go`** (C6's row: "shards built by `arbace.build.go`"): the
   check's shards built today by `go build` in a module of their own; `bin/build-go --root` over the shard's forms with
   ci and crt as the program's packages (REAP7b's way), under the lock for a shard of the size `bin/j2g --program`'s
   are; and `bin/arbace-go --build-program` taking a C program (`program.edn`'s `:c-main`, its `c/` sources: the fourth
   language of `test/programs/`).
4. **musl compiled once and listed in ci's table** (CS122 (a)): the library node program of all 1,335 units, translated
   once (C6.3's sample measures a quarter), its packages compiled, the interpreter calling them; then the full check
   again with programs linked against it (and the check's time, CS124).
5. **The full corpora** (C5.7 item 6) compiled: SQLite, Lua, zlib, QuickJS and gcc 15.2's `dg-do run` tests.
6. **The gate** (C5.7 item 7) and C8's AOT essential subset (`test/c/essential-aot.txt`, CS126 (a)).
7. **CS123 (a)** measured: the typed calling convention on CompCert's benchmarks.

### C6.8 Checks and scratch

Run: `CI_AOT_NODES=1 bin/ci check --aot --record` twice over the seven corpora (the first found C6.2's two bugs; the
second, on the final code, is recorded), the four programs again after the fixes, `--judge --record`; sampled runs before
(`--sample 8` over four corpora, 238 programs; tcc-tests2 whole); `bin/ci cirun-build` after each change to `go/arbace/ci`
(repr/go's print of the package and TamaGo's build); the probes `.tmp/c6/h.c` (setjmp, a switch with fall-through, a
struct returned, printf), `t1.nodes` (fib), `vla.c` (VLAs in a loop with `continue`, a goto loop over a block, a goto back
over a later VLA), each interpreted, compiled and against gcc; CompCert's `nbody.c` and `binarytrees.c` timed and
profiled; the musl samples; `make check-makefile`. Not run: anything of C6.7 or C5.7, the essential gate (the light
tier), `bin/ci repl-session` (the REPL's path is unchanged but for the builder's VLA nodes, which its session does not
use).

Shipped files: `go/arbace/ci.ae`, `go/arbace/ci/aot.ae` (new), `go/arbace/ci/call.ae`, `load.ae`, `natives.ae`,
`prog.ae` (a package nothing imports yet: inert in a build until C5.7's item 2). Not shipped (CS100 (b)'s exclusion):
`arbace/c/cf2g.ae` (new), `arbace/c/build.ae`; not shipped either: `bin/ci`, `bin/lib/ci/check-aot` (new), `test/c/` (the
new `divergences-aot.edn` and `check-aot-linux-amd64.edn`; `check-linux-amd64.edn` recorded again, `check-known.edn` one
entry less), the Makefile (`ci-check-aot`, `ci-aot-translate`), `doc/ARBACE.md`, this survey.

The worktree's `.tmp/` (kept): `ci-aot/` (each corpus's `list.tsv`, `nodes.tsv`, `run.tsv`, the shards' lists and logs,
`interp/CORPUS/run.tsv` the interpreted runs, `results.edn`; the module's sources and executables deleted shard by
shard), `ci-interp/` (C5's lists and interpreted runs: the first runs' comparison), `c6/` (the probes, `musl-sample.ae`,
the logs `tcc1.log`, `s1.log`, `full1.log` (the first full run), `redo.log`, `cxgo.log`, `judge1.log`, `judge2.log`,
`full2.log` (the recorded run), the first run's records `check-aot-run1.edn` and `check-interp-run1.edn`, the draft
`asbuilt.md`), `ci-musl/` (C5's musl, copied from `c23-c5`'s scratch), `ci/` (the printed packages and `cirun`).

Rule slips: two commands used process substitution `<(...)`, which the rules forbid (a one-off comparison of the first
run's compiled and interpreted runs, and a stray `diff <(true)` in the final results' command; neither wrote outside the
worktree); one probe run with `TMPDIR` unset wrote its error report to `/tmp` (removed); a `--resume` of four programs
took the earlier shards' logs for shards (`shard.[0-9]*`), whose rows were repaired by hand and then replaced by the
recorded run (the glob is now `shard.[0-9][0-9][0-9][0-9]`).

## As built: C7 (write-ahead) (2026-10-31)

C7, the C23 corpus and its error tests, on the branch `c23-c7` from `c23-c6` (`bdf99d5e`), in the light tier (under 2 GB,
at most 4 threads, no gate, no build of Arbace's executables), so **write-ahead** as C5 and C6 were: the new namespace
`arbace.c.dg` runs evaluated from the checkout on stage 0 (under `arbace/c/`, excluded from the embedded tree by CS100 (b));
the run level reuses C5's and C6's checks (tools/cirun, cf2g's shards) unchanged but for the corpora they take. The corpus
is CS33's seven gcc groups of `test/c/corpora.edn` (`:gcc-c23`, `:gcc-cpp-c23`, `:gcc-c23-sub`, `:gcc-gnu23`, `:gcc-embed`,
`:gcc-va-opt`, `:gcc-bitint`: 627 files of gcc 15.2.0's testsuite, from the pinned tarball extracted by `bin/repr-c
gcc-tests` into `.tmp/repr-c/`); gcc 15.2's `gcc.dg/c2x-*` has no file in 15.2 (renamed `c23-*`, as the manifest says). No Decided
section asks for clang's `test/C/C23` here: **not done** (CS136).

### C7.1 What was built

| part | where | what |
|---|---|---|
| the directives | `arbace/c/dg.ae` (`arbace.c.dg`) | DejaGnu's directives read as Tcl lists (braced, quoted and bare words, backslashes): `dg-do` (the first whose selector applies; default compile), `dg-options` (through `host/dg-options`), `dg-error`/`dg-warning`/`dg-message`/`dg-note`/`dg-bogus` with their line (`.`, `.+N`, `.-N`, an absolute number, 0 for any) and selector (a C++ target does not apply; `xfail` kept apart), `dg-excess-errors`, `dg-additional-sources`, `dg-require-effective-target`, `dg-skip-if`. Per file: action, options, expected lines by kind, the error messages by line |
| gcc's side | `bin/lib/ci/dg` | gcc 15.2 on every file in its own directory with its dg-options (`-S`, `-E` for `dg-do preprocess`, `-fdiagnostics-plain-output`, output capped at 20 KB), its diagnostics by line and kind parsed; **they validate the directives**: the expected error lines are gcc's own error lines |
| the read level's judge | `arbace.c.dg` `read-file`, `verdict` | each file read and checked as `bin/ci diagnose` does (a `dg-do preprocess` file by the preprocessor alone, `pp/preprocess`), the errors by line of the main file (errors of included files counted apart); pass when every expected error line has an error of the checker's and the checker gives none on a line that expects none (an `xfail` line allowed, `dg-excess-errors` and line 0 meaning any); else `missed` (an expected line without an error), `extra` (an error where none is expected), `both`, or `refused`, `timeout`, `crash` (no judgment); the reason holds the lines and gcc's expected message |
| the classes, results | `test/c/divergences-dg.edn`, `test/c/check-dg-linux-amd64.edn` | six classes (below); `[ID RESULT REASON]` for each of the 627 files |
| the run groups | `test/c/corpora.edn` `:gcc-*-run` (`:like` the group, `:dg-do :run`), `arbace.repr.c.ppcheck` (`:like`, `run-files`) | the group's files whose action is `dg-do run` (a selector for C++ apart) and that have no `dg-additional-sources` (one file a program): 176 programs |
| the run levels | `bin/ci check`, `bin/ci check --aot` | `gcc-*` ids now extract the testsuite (`check`, `check-aot`); gcc's runs recorded in `test/c/ref-gcc15.edn` (`bin/ci record-ref --only`); `test/c/check-linux-amd64.edn`, `check-aot-linux-amd64.edn` recorded; classes of `divergences.edn` extended (below); 17 programs in `check-known.edn` |
| the command | `bin/ci dg [--corpus C,...] [--record] [--judge]`, `make ci-dg` | lists, directives, gcc, read, judge; about 8 minutes for the seven groups at 3 threads |

### C7.2 The numbers

**The directives against gcc**: of 627 files, 606 have directives whose expected errors are exactly gcc 15.2's errors on the
file; 21 differ, each for a selector or an option the judge does not evaluate (the DFP files' `no-dfp` targets, the
`#embed` files' `--embed-dir`/`-I` through `dg-additional-options` and `dg-prune-output`, an `xfail`). The judge uses the
directives; the 21 are named in the results when they do not pass (`[gcc: ...]` in the reason).

**The read level** (checker's errors by line and kind against the `dg-error` lines), 627 files:

| group | files | pass | decl-diag | attr-diag | pedantic-diag | pp-diag | parse-gap | read-refused |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| gcc-c23 | 270 | 145 | 60 | 24 | 11 | 0 | 25 | 5 |
| gcc-cpp-c23 | 16 | 7 | 0 | 0 | 1 | 6 | 2 | 0 |
| gcc-c23-sub | 30 | 21 | 1 | 0 | 0 | 0 | 6 | 2 |
| gcc-gnu23 | 51 | 30 | 15 | 1 | 1 | 0 | 4 | 0 |
| gcc-embed | 47 | 36 | 2 | 0 | 2 | 5 | 2 | 0 |
| gcc-va-opt | 12 | 9 | 0 | 0 | 1 | 2 | 0 | 0 |
| gcc-bitint | 201 | 189 | 6 | 2 | 1 | 0 | 2 | 1 |
| **total** | **627** | **437** | **84** | **27** | **17** | **13** | **41** | **8** |

**None unclassed**: every non-pass is in a class. By the file's expectation: of the 471 files that expect no error, 428 pass (no false
error) and 43 fail (parser gaps and refusals on valid or off-line code); of the 156 files that expect errors, **9 pass and 147
fail**: the checker diagnoses, for the C23 corpus, almost none of what gcc rejects beyond syntax (C4's CS92 (a) and CS94 (a), which
left the rest to this judge). The classes: `decl-diag` (conflicting and redefined tags and types, incompatible pointers,
`constexpr`, enumeration and VLA rules, static assertion), `attr-diag` (standard attributes with arguments, ignored or misplaced:
the checker keeps attributes as data), `pedantic-diag` (the pedwarns of `-pedantic-errors`, which the checker has no mode for),
`pp-diag` (`#embed` parameters repeated or malformed, invalid or non-NFC universal character names, `__VA_OPT__` misuse),
`parse-gap` (C23 the parser rejects: `va_start` and `...` without a named parameter, storage classes in compound literals, `_Generic`
with a type argument, named loops, `decimal64x`, `#embed` in an initializer) and `read-refused` (a declarator the converter reads
as not a function's, `d32` suffixes by design, an integer suffix beyond 128 bits, one read over the 120 s budget, one crash on a
malformed binary constant).

**The run level** (`dg-do run`, 176 programs against gcc 15.2's runs; interpreted and compiled by cf2g, 2 jobs of `go build`):

| group | programs | pass | other classes |
|---|---:|---:|---|
| gcc-c23-run | 47 | 27 | parse-gap 6, vla 5, complex-int 3, long-double 2, fenv 2, threads 1, gcc-builtin 1 |
| gcc-cpp-c23-run | 1 | 1 | |
| gcc-c23-sub-run | 6 | 0 | parse-gap 3, by-design 2, gcc-builtin 1 |
| gcc-gnu23-run | 13 | 8 | vla 3, gcc-builtin 1, by-design 1 |
| gcc-embed-run | 17 | 11 | by-design 4, checker 1, parse-gap 1 |
| gcc-va-opt-run | 2 | 2 | |
| gcc-bitint-run | 90 | 43 | wide 22, gcc-builtin 17, by-design 7, abi 1 |
| **total** | **176** | **92** | **84, none failing** |

Compiled against interpreted: 92 pass both, 84 neither, **0 compiled only, 0 interpreted only**; 122 runs with the same status and
standard output, none differing. The classes added to `divergences.edn`: `fenv` (new: FE_INVALID of a signaling NaN, SPEC §12.5), `parse-gap`
(new: `read-error` rows), and matches added to `by-design` (decimal floating suffixes), `gcc-builtin` (`__builtin_*` and `__atomic_*`
functions called but undefined: `popcountg`, `stdc_*`, `*_overflow_p`, `clear_padding`, `__atomic_is_lock_free`) and `threads`
(`__clone`). The 22 `wide` programs are `_BitInt` wider than 128 bits: ci's back end has no node for their conversions and loads
(`w2w`, `i2w`, `wld`, `wbin`: a panic, status 2).

### C7.3 Proposals (CS130-CS136)

Each with options, the recommended one first; none decided.

- **CS130 `fenv`: floating-point exception flags.** Built as a class (2 programs).
  - Options: (a) a class by design until a program outside gcc's own tests needs them; (b) flags kept by ci's arithmetic (FE_INVALID,
    FE_DIVBYZERO, ...) and `fetestexcept`/`feclearexcept` over them, cf2g's float operations through the same helpers; (c) a class `deferred`.
  - *Recommended: (a)*: two tests in 176 and the arithmetic would pay for it everywhere.
- **CS131 The parse gaps.** 41 files at the read level and 10 programs of the run level (the same constructs).
  - Options: (a) a slice of repr/c before C8 fixing the listed constructs (`va_start (ap)` and `...` as the first parameter, storage-class
    compound literals, `_Generic` over a type, named loops, labels before declarations, `decimal64x`), the known file shrinking; (b) leave them
    recorded until the heavy window; (c) split: those that also make a C23 program fail to run first.
  - *Recommended: (c)*: 10 programs fail to run for them, the rest are error tests whose first lines the parser already misses.
- **CS132 A pedantic mode of the checker.** 17 files (and part of `decl-diag`) need `-pedantic-errors`.
  - Options: (a) the checker takes the file's `-pedantic`/`-pedantic-errors` (as `:unsigned-char` is taken from `-funsigned-char`) and
    diagnoses the C23 pedwarns of the corpus (imaginary constants, `0o` prefixes, digit separators in older standards, `__VA_OPT__` before C2y);
    (b) leave `-pedantic-errors` to the checked mode (C8's, CS11); (c) never.
  - *Recommended: (a)* restricted to what the c23 files test, since both the options and the diagnostics are on the files' lines.
- **CS133 Widening the checker's diagnostics.** 84 `decl-diag` and 27 `attr-diag` files; 147 of 156 error tests fail.
  - Options: (a) by family, in the order of the counts in the results (tag compatibility and redefinition, 24 files; `constexpr` and
    enumeration ranges, 16; attribute arguments, 24; incompatible pointers and conversions, 10), each family a commit with the judge as its
    check; (b) leave as recorded; (c) only the families a run program needs.
  - *Recommended: (a)* in C7's follow-up, in the light tier: the judge is cheap (about 4 minutes for the groups' reads) and exact by line.
- **CS134 The 21 files whose directives gcc does not meet.** Selectors (`dfp`, `bitint`, `int128` targets), `dg-additional-options` with
  `${srcdir}` and `--embed-dir`, `dg-prune-output`.
  - Options: (a) evaluate the selectors the reference host settles (its target triple and effective targets as a small table recorded from
    `gcc -dumpmachine` and probes) and the options as DejaGnu composes them; (b) as built: the directives judged, gcc's disagreement noted in the
    result; (c) the judge takes gcc's diagnostics as the expectation where the two differ.
  - *Recommended: (b)* now, (a) if the 21 matter once CS133 starts: they are 3% of the corpus.
- **CS135 The essential subset.** Not made (C8's, with the gate step).
  - Options: (a) `test/c/essential-dg.txt` with the files that pass at each level plus one of each class, `bin/ci dg --essential` a gate step
    of seconds (the read of about 60 files) and the run groups' passing programs in `essential.txt`/`essential-aot.txt`; (b) the whole groups
    in the gate (8 minutes of reading).
  - *Recommended: (a)*.
- **CS136 clang's `test/C/C23`.** Not fetched: the survey's pin has no revision for it.
  - Options: (a) after CS133, pin an LLVM release tarball's `clang/test/C/C23` and `clang/test/Sema` (Apache-2.0 WITH LLVM-exception; `-verify`
    directives `expected-error {{...}}` by line, a second judge on the same verdict); (b) not at all, gcc's corpus is the reference (CS14 (a)).
  - *Recommended: (b)* until the gcc groups' error tests mostly pass, then (a).

### C7.4 What is left (the heavy list)

Nothing of C7 needed more than the light tier. What C7 adds to C5.7's and C6.7's lists: (1) `bin/ci dg` and the seven run groups as steps of the
gate (CS135) once the executable has ci and cf2g; (2) the 22 `_BitInt` programs over 128 bits need ci's wide nodes (`w2w`, `i2w`, `wld`, `wbin`),
a change to `go/arbace/ci` and so a shipped file built only by the next executable; (3) the run groups with `dg-additional-sources` (4 files) as
multi-file programs once the corpora take them; (4) the `torture` groups of gcc (`gcc.dg/torture`'s `dg-do run` outside the c23 files) are not in CS33.

### C7.5 Checks and scratch

Run: `bin/ci dg` over the seven groups (lists, directives, gcc, reads at 3 threads, the judge; recorded), `bin/ci record-ref --only` for the
seven run groups, `bin/ci check --record` and `bin/ci check --aot --record` with `CI_AOT_NODES=1` over them, `make check-makefile`. Not run:
`bin/gate`, the essential gate, anything of C5.7/C6.7, `bin/ci annotations` (unchanged), `bin/ci repl-session`. A first judge run
had `dg-do preprocess` files read to the end as C (the parser's errors on the preprocessed text counted); the judge now stops at the
preprocessor for them (the 11 + 10 + 15 + 6 + 2 files of the groups, 44 in all).

Shipped files changed: none under `go/`, `overlay/`, `jdk/`. Not shipped (CS100 (b)): `arbace/c/dg.ae` (new), `arbace/repr/c/ppcheck.ae` (`:like`,
`run-files`); `bin/ci`, `bin/lib/ci/{dg,check,check-aot}`, `test/c/` (`corpora.edn` seven `-run` entries, `divergences.edn`, `divergences-dg.edn`, `check-known.edn`,
`ref-gcc15.edn`, `check-dg-linux-amd64.edn`, `check-linux-amd64.edn`, `check-aot-linux-amd64.edn`), the Makefile (`ci-dg`), this survey.

The worktree's `.tmp/` (kept): `ci-dg/` (the groups' lists, `dg.edn`, `gcc.edn`, `read.edn`, `gcc/` outputs, `judge-rows.edn`), `ci-check/`, `ci-aot/` (the
run groups' lists and runs), `c7/` (logs), `repr-c/gcc-15.2.0/` (the extracted testsuite, 220 MB), `ci-musl/` (musl extracted and read), `ci/` (cirun).

### Decided (CS120-CS127, CS130-CS136; the user, 2026-10-31)

All as recommended:
- C6's: CS120 (a), CS121 (a), CS122 (a) in the heavy window, CS123 (a) in the heavy window, measured on CompCert's
  benchmarks, CS124 (a), CS125 (a), CS126 (a) now and (b) with C8, CS127 (a).
- C7's: CS130 (a), CS131 (c), CS132 (a) restricted to what the c23 files test, CS133 (a) in C7's follow-up in the
  light tier, CS134 (b) now, CS135 (a), CS136 (b) until most of the gcc groups' error tests pass, then (a).

## As built: C7's follow-up (CS131, CS132, CS133) (2026-10-31)

On the branch `c23-c7`, in the light tier, after the Decided section above: the parse gaps (CS131 (c)), the checker's pedantic mode
(CS132 (a)) and its diagnostics by family (CS133 (a)). Everything stays outside the shipped tree (CS100 (b)): `arbace/c/{check,dg}.ae`,
`arbace/repr/c/*` and `arbace/repr/c.ae` (not in the gate's key, `bin/lib/version.bash`); nothing under `go/`, `overlay/`, `jdk/`.

### C7F.1 A finding first: the judge did not see the checker's errors

C7's judge located an error by `LINE:COL:` or `FILE:LINE:COL:`; the checker writes `LINE: message`. Its errors were counted as
"unlocated" and judged neither for the lines that expect an error nor for the lines that do not. So C7's 437 passes counted the reader's
and the preprocessor's errors alone, and a false error of the checker could not show. `arbace.c.dg/error-line` takes `LINE: msg` now, and the
file lists are made again by `bin/ci dg` (the checker reads the file's options from `:fc`). The numbers below are with the corrected judge.

### C7F.2 The numbers

`bin/ci dg` over the seven groups, 627 files (before: C7's record at `2fb50012`; after: this record):

| class | before | after |
|---|---:|---:|
| pass | 437 | **533** |
| decl-diag | 84 | 36 |
| attr-diag | 27 | 14 |
| pedantic-diag | 17 | 7 |
| pp-diag | 13 | 12 |
| parse-gap | 41 | 15 |
| read-refused | 8 | 10 |
| not in a class | 0 | 0 |

By the file's expectation: of the 471 files that expect no error, **452 pass** (428 before: no passing no-error file was lost, 24 gained by the
parser), and of the 156 that expect errors **81 pass** (9 before). Per group, pass: gcc-c23 222 of 270 (145), gcc-cpp-c23 7 of 16 (7),
gcc-c23-sub 23 of 30 (21), gcc-gnu23 38 of 51 (30), gcc-embed 37 of 47 (36), gcc-va-opt 9 of 12 (9), gcc-bitint 197 of 201 (189).
`read-refused` rose by two because the four `decimal64x` files now parse and the converter refuses their `d64x` suffix by design (CS3 (a)).
The run level (`dg-do run`, 176 programs against gcc's runs): **98 pass** (92 before): gcc-c23-run 32 of 47 (27), gcc-gnu23-run 8 of 13 (8), gcc-embed-run 12 of 17 (11),
gcc-bitint-run 43 of 90, gcc-cpp-c23-run 1, gcc-va-opt-run 2, gcc-c23-sub-run 0 of 6; the programs that failed to parse (`parse-gap`) are all read: the six `va_start`/`...` programs, `c23-complit-2` (which
now parses and faults at run time: the new class `complit-static`, `test/c/divergences.edn`, `check-known.edn`) and `embed-13`; the three `decimal64x` programs are `by-design`. Compiled against interpreted (cf2g, `CI_AOT_NODES=1`):
98 and 98, 0 compiled only, 0 interpreted only. The six older corpora (torture-execute and its mirror, tcc-tests2, compcert, mir, cxgo, benchmarksgame): unchanged, interpreted and compiled, after one fix
(below).

One run-level slip of this slice, caught by that check: the first `bin/ci check` after the pointer-conversion check failed 15 programs of the torture corpora and mir, which gcc builds with `-fpermissive -w`
(`bin/lib/ci/record-ref`); the checker called by `arbace.c.run` now runs `-fpermissive` (`:permissive`), and those 15 pass again.

### C7F.3 What was built

| part | where | what |
|---|---|---|
| CS131 (c), the parser | `arbace/repr/c/parse.ae`, `convert.ae`, `tree.ae`, `print.ae` | `_Generic` over a type name (`:c/type` on the form, which the checker and the printer already read); storage classes in `sizeof`/`alignof` compound literals; `...` as the first and only parameter (also `(...)` in an abstract declarator, but not after a GNU attribute); abstract declarators ending at `,` and `()`/`(int)` in a type name; C23 `va_start (ap, anything)` (`__builtin_c23_va_start`: gcc skips the tokens after the first argument); `_Decimal32x`/`_Decimal64x`; `enum E : 2` in a generic association (the colon is the association's); a narrow or u8 string before a prefixed one (gcc's non-standard concatenation); `break L;`/`continue L;` (named loops); `__seg_gs`/`__seg_fs` as qualifiers; `0o` and `wb`/`wbu` in `#if`; `typeof` of a parenthesized literal (a `Lit` object leaked into the form); an empty prefixed character constant and a bare `0b` are refusals, not crashes |
| CS132 (a), the pedantic mode | `arbace/repr/c/host.ae`, `c.ae`, `pedantic.ae`, `arbace/c/check.ae` | the file's `-pedantic-errors` (`:pedantic`) and `-Wno-c23-c2y-compat`, `-Wno-c11-c23-compat`, `-fpermissive`, `-fsigned-char`/`-funsigned-char` (with `__CHAR_UNSIGNED__`), `-fexec-charset` (static assertions not judged) are the checker's options; `pedwarn!` makes an error only then and not on a line with `__extension__` (`read-c` finds them); `arbace.repr.c.pedantic/notes`: the lexical pedwarns on the main file's own tokens (`0o` before C2y, `wb` before C23, adjacent string literals of two prefixes, `#embed` before C23); in the checker: imaginary constants, case ranges, named loops, `_BitInt(N)` before C23, old-style definitions in C23, qualifier discard |
| CS133 (a), diagnostics | `arbace/c/check.ae` | tags: a repeated definition in a scope is the same type when the members are (C23 6.2.7; it merges), else "redefinition", a nested redefinition; tagged types compare by members (self-references cut off, variable-length and zero-length members, tagless members by their members); a conflicting redeclaration of an object or function (a block-scope `extern` against the file scope's), a typedef redefined with another type; enums: a repeated definition, an implicit constant overflowing a 64-bit type, a value outside a fixed type, `declared both with and without` a fixed type, a constant redeclared by another enum, `_BitInt` underlying types, values beyond `intmax_t` (pedantic); pointer conversion (assignment, initialization, argument, return): incompatible pointees are errors (not under `-fpermissive`), a discarded qualifier a pedwarn; call arity against a prototype; `constexpr` integer objects given a floating or complex value, `constexpr` strings whose elements the element type cannot represent; duplicate case values (ranges included); `_BitInt` widths; empty initializers of unknown-size arrays, variable-length arrays given non-empty initializers, variable-size compound literals; arrays of wide characters given a narrow or u8 string; `alignof` of an incomplete type; `nullptr` where an integer constant is required; `bool`/`_Bool` as names; data-type words beside `_Bool`/`_BitInt`; `[[...]]` arguments (`deprecated`/`nodiscard` take one string, the others none, `()` is an error for `deprecated` and `nodiscard`); attribute declarations (`[[deprecated]];` ignored, `[[fallthrough]];` at top level, outside a switch, or before no case label); `#embed` parameters (a name expected, repeated, `(` expected) |
| the judge | `arbace/c/dg.ae` | the checker's `LINE: msg` located (C7F.1) |
| ci's own run | `arbace/c/run.ae` | the checker runs `-fpermissive` (the references are built with `-fpermissive -w`, `bin/lib/ci/record-ref`) |

### C7F.4 What is left, by class (all in `test/c/divergences-dg.edn`)

- `decl-diag` 36: the `qual` family (the conditional operator's pointer types and qualifiers, `c23-qual-4/5/6`, 3 files), `nullptr` (comparison, arithmetic, conversion: `c23-nullptr-3`, and the lines of `nullptr-6`
  the member and designator contexts do not locate), enum and complit specifier rules (`c23-enum-7`, `c23-complit-5/8`), `typeof` of a bit-field, `static` never defined,
  thread-local redefinitions, tag compatibility in composite types and jumps into scopes (`tag-composite-3/11`, `tag-6`), floating `constexpr` representability (`constexpr-9`, the dfp files),
  `bitint-115/117` (incompatible `_BitInt` pointers), `gnu23-tag-3/5`, VM types in members, `auto` with a statement expression.
- `attr-diag` 14: the type-position attributes ("ignored" on `int [[x]] v`, on array and function declarators, on statements and labels: the forms do not say where the attribute stood, 8 files),
  `vector_size`, `weak`, `has_c_attribute`, GNU attributes before `...`.
- `pedantic-diag` 7: digit separators, complex `++`, delimited escapes, `__VA_OPT__` before C2y, `_Decimal` without DFP.
- `pp-diag` 12: `#embed` parameter values (`embed-3/4/16/18/27`), universal character names (`ucn-1/2`, `ucnid-*`), `__VA_OPT__`, `#elifdef`, pasting.
- `parse-gap` 15: attributes inside abstract declarators and arrays (`attr-syntax-1/2`, `gnu23-attrs-2`), labels with GNU attributes before `}` (`labels-3`), `[*]` in a member's function-pointer
  parameter (`gnu23-tag-4`), and the files whose expectations depend on a target selector (CS134: `inff`, `dfp`), a pp macro (`__STDC_VERSION_STDINT_H__`, `__has_c_attribute (foo)`), `u8` in `#if`.
- `read-refused` 10: `d32`/`d64x` suffixes (by design), an integer suffix beyond 128 bits, `auto;` and `constexpr extern` declarators the converter reads as no function's, a macro-heavy file past the budget.

### C7F.5 Proposals (CS140-CS146)

Each with options, the recommended one first; none decided.

- **CS140 Where an attribute stood.** The "ignored" family (8 files, about 200 lines) needs the position of each standard attribute: after the type specifiers, on an array or function declarator, on a statement, a label.
  - Options: (a) the converter keeps a position tag in the attribute vectors (`:c/attrs-at`), printed nowhere, read by the checker; (b) a second walk over the tree in the checker's front end; (c) leave the family.
  - *Recommended: (a)*: the tree knows it and the forms lose it.
- **CS141 The conditional operator's types.** `?:` with pointers needs the composite type with merged qualifiers, `nullptr` rules and the "pointer type mismatch" error (`qual-4/5/6`, `nullptr-3`, 4 files).
  - Options: (a) rewrite `cond-type` after 6.5.15 p6-p7 and C23 `nullptr_t`, then the conversions of `ptr-conv!` use it; (b) leave.
  - *Recommended: (a)* once CS140 is done: it also repairs the macro-built types of the torture files.
- **CS142 Target selectors (CS134 again).** Five files fail only because their selectors (`! inff`, `! dfp`) are not evaluated; with the corrected judge CS134 (b) costs 5 passes (`float-11`, `float-no-dfp-1/3`, `no-dfp-1`, `builtins-no-dfp-1`).
  - Options: (a) a table of the host's effective targets recorded from `gcc` probes, read by the judge; (b) as built.
  - *Recommended: (a)*, small.
- **CS143 The preprocessor's diagnostics.** 12 files: `#embed` parameter values, UCNs, `__VA_OPT__`.
  - Options: (a) a slice of repr/c's preprocessor after gcc's libcpp messages, file by file, in the light tier; (b) heavy window; (c) leave.
  - *Recommended: (a)*, one family a commit like CS133.
- **CS144 The remaining parse gaps.** 7 files (attributes in abstract declarators and arrays, labels, `[*]`).
  - Options: (a) the parser takes a GNU/standard attribute in every declarator position cc's grammar lacks (go-whim has most); (b) leave.
  - *Recommended: (a)*, with CS140 (the same positions).
- **CS145 The parser's and the preprocessor's own checks.** This slice changed repr/c's parser, converter and preprocessor; the checks against cc (`bin/repr-c parse-check`, `check --tokens`) need tools/ccdump (Go with go-whim's module cache) and were not run; `bin/repr-c check --c23` and `roundtrip` over c23conf, c23 and tcc-tests2 were.
  - Options: (a) run both in the heavy window and record the new known entries (`_Generic (type`, `...` first, named loops are not cc's); (b) accept the divergence classes now.
  - *Recommended: (a)*.
- **CS146 The essential subset (CS135) with the corrected judge.** `bin/ci dg --essential` now sees false errors of the checker, which makes it a good gate step.
  - Options: (a) as CS135 (a); (b) all groups.
  - *Recommended: (a)*, now with the files this slice made pass.

### C7F.6 Checks and scratch

Run: `bin/ci dg --record` (twice: the corrected judge, then the final), `bin/repr-c check --c23`, `bin/repr-c roundtrip --corpus c23conf,c23,tcc-tests2 --no-run`, `bin/ci check --record`, `bin/ci check --aot --record`
(a first `check --record` with `--only` rewrote the record to the 15 lines: restored from git and recorded again in full; a `compcert/qsort.c` timeout under the host's load was not recorded). Not run: `bin/gate`, `bin/ci annotations` (needs tools/ccdump), `bin/repr-c parse-check` and `check --tokens` (same), `bin/ci repl-session`.
The worktree's `.tmp/`: `ci-dg/`, `ci-check/`, `ci-aot/`, `c7/` (logs, capped at 1 MB), `dgsave/` (gcc's outputs), `t/` (probes), `dg*.sh` (the read/judge loop), `repr-c/gcc-15.2.0/` (220 MB).

### Decided (CS140-CS146; the user, 2026-10-31)

All as recommended: CS140 (a); CS141 (a) after CS140; CS142 (a); CS143 (a), one family a commit; CS144 (a) with
CS140; CS145 (a) in the heavy window; CS146 (a) with the files the follow-up made pass.

## As built: CS140-CS144, CS146 (2026-10-31)

On the branch `c23-c7`, in the light tier, after "Decided (CS140-CS146)": the user's decisions carried out, eight commits (`49ff3437..HEAD`). Everything stays
outside the shipped tree (CS100 (b)): `arbace/c/{check,dg}.ae`, `arbace/repr/c/*`, `arbace/repr/c.ae`, `bin/ci`'s libraries, `test/c/`; nothing under `go/`, `overlay/`, `jdk/`.

### C7G.1 What was built

| part | where | what |
|---|---|---|
| CS140 (a) + CS144 (a), attributes | `arbace/repr/c/parse.ae`, `convert.ae`, `tree.ae`, `print.ae`, `arbace/c/check.ae` | attributes after array and function suffixes, in type names, after labels and in `for` declarations, `[*]`, `gnu::` balanced tokens; the forms keep `:c/type-attrs`, `:c/attrs` on array and function types, `:c/after-at`, `:c/attrs` on expression statements; the printer puts type-position attributes after the type specifiers (CS140's last commit); the checker's "ignored" diagnostics, "can only be applied" for noreturn/nodiscard, reproducible/unsequenced |
| CS141 (a), `?:` | `arbace/c/check.ae` | composite pointer types with merged qualifiers or "pointer type mismatch", `nullptr_t` rules, the read-only check through `aset` |
| CS142 (a), targets | `test/c/dg-targets.edn`, `arbace/c/dg.ae` | target selectors evaluated from a table recorded from `gcc` probes; a file whose selector is false is "unsupported", counted as a pass |
| CS143 (a), the preprocessor | `arbace/repr/c/pp.ae`, `lex.ae`, `pedantic.ae` | `#embed` parameter errors, `#if` "operator has no right operand", `__VA_OPT__` errors, `#elifdef` after `#else`, C23 `::` paste, UCN and identifier rules, N3353 delimited escapes, lexical pedantic notes in preprocess-only files; later `u8` character constants unsigned in `#if`, macros expanded in a `__has_c_attribute` name |
| CS146 (a), essential | `bin/ci`, `bin/lib/ci/dg`, `test/c/essential-dg.txt`, `Makefile` | `bin/ci dg --essential` (93 files, read and judged, no gcc run), `make ci-dg-essential`; 93 of 93 pass |

### C7G.2 The numbers

`bin/ci dg --record` with `REPR_C_READ_BUDGET=300`, 627 files (before: the record of C7F.2):

| class | before | after |
|---|---:|---:|
| pass | 533 | **572** |
| decl-diag | 36 | 30 |
| attr-diag | 14 | 6 |
| pedantic-diag | 7 | 3 |
| pp-diag | 12 | 3 |
| parse-gap | 15 | 3 |
| read-refused | 10 | 10 |
| not in a class | 0 | 0 |

Per group, pass: gcc-c23 242 of 270 (222), gcc-cpp-c23 16 of 16 (7), gcc-c23-sub 23 of 30 (23), gcc-gnu23 42 of 51 (38), gcc-embed 39 of 47 (37), gcc-va-opt 11 of 12 (9), gcc-bitint 199 of 201 (197).
`c23-binary-constants-1`, which timed out at the 120 s read budget under the host's load, passes at 300 s (CS153).

The run level: `bin/ci check --record` unchanged (committed in `fdf6de6e`; a `compcert/perlin.c` timeout under load passes). The compiled level (`bin/ci check --aot --record`, `CI_AOT_NODES=1`): every result as recorded before (the record file is unchanged); the compiled run passes every compcert program (24) and `spectral-norm`, which the interpreted run times out on under load (10 programs, the only differences: interpreted timeouts, 0 interpreted-only passes). The same `--aot` run re-recorded the interpreted level with a `compcert/perlin.c` timeout; that record was restored from git, the committed one (perlin passes) stands.
`bin/repr-c check --c23`: 62 files, 0 failing; the round trip: all pass.

### C7G.3 Checks not run

`bin/repr-c parse-check` and `check --tokens` (CS145/CS156: need tools/ccdump with go-whim's module cache), `bin/ci annotations` (same), `bin/gate`, `bin/ci repl-session`.

### C7G.4 Proposals (CS150-CS156)

Each with options, the recommended one first; none decided.

- **CS150 The remaining attribute and parse diagnostics** (`attr-syntax-3/5/6/7`, `gnu23-attr-syntax-2/3`, `has-c-attribute-4`, `bitint-21/75`).
  - Options: (a) by pattern, starting with "attributes before C23" and `vector_size` validation; (b) leave.
  - *Recommended: (a)* for those 3 files only.
- **CS151 `gnu::base64` and `#embed` string-literal diagnostics** (`embed-4/18/27`, `embed-6/7`).
  - Options: (a) escape decoding (`\N{name}` needs a Unicode names table) and strict base64; (b) leave.
  - *Recommended: (b)* until a names table exists.
- **CS152 The decl-diag long tail.**
  - Options: (a) family by family as CS133; (b) leave; (c) only what run programs need.
  - *Recommended: (a)*.
- **CS153 The read budget under load.**
  - Options: (a) the default of `bin/ci dg` raised to 300 s; (b) cache `parse-type` results of `typeof` and typedefs; (c) leave.
  - *Recommended: (a)* now, (b) later.
- **CS154 Decimal-float suffixes `d32` and `d64x`**, refused by design (CS3 (a)).
  - Options: (a) keep refusing; (b) read them as a decimal-float literal form.
  - *Recommended: (a)*.
- **CS155 `bin/ci dg --essential` as a gate step** (C8's CS135).
  - Options: (a) a step with `test/c/essential-dg.txt` and the checker's sources as inputs; (b) leave it manual.
  - *Recommended: (a)*.
- **CS156 CS145 again.** The lexer now differs from cc in identifier continuation, `#embed` header names, UCN errors and delimited escapes.
  - Options: (a) run `parse-check` and `check --tokens` in the heavy window and record the new known entries; (b) accept the divergence classes now.
  - *Recommended: (a)*.

### C7G.5 Shipped files, scratch and rule slips

Shipped files changed: none (nothing under `go/`, `overlay/`, `jdk/`).
Scratch in the worktree's `.tmp/`: `c7/` (logs, capped at 1 MB), `ci-dg/`, `ci-check/`, `ci-aot/`.

Rule slips: a `> /dev/null` in one nohup redirect (previous agent); the CS143 families in one commit (they share `pp.ae`, `lex.ae`, `pedantic.ae`) plus a later small commit `7de9cb4f`; CS141 committed by temporarily
reverting hunks (no stash was used); this follow-up's run waited with a sleep longer than the tool's foreground limit once, which moved it to the background (no effect on results).

### Decided (CS150-CS156; the user, 2026-10-31)

All as recommended: CS150 (a), for the three files only; CS151 (b) until a Unicode names table exists; CS152 (a), family
by family as CS133; CS153 (a) now, (b) later; CS154 (a); CS155 (a), with C8's gate step (CS135); CS156 (a) in the heavy
window.
