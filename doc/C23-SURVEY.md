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

## As built: CS150, CS152, CS153 (2026-10-31)

On the branch `c23-c7`, in the light tier, after "Decided (CS150-CS156)": three commits (`1f88907b`, `a01025df`, `0ced324b`). Outside the shipped tree
(CS100 (b)): `arbace/c/{check,dg}.ae`, `arbace/repr/c.ae`, `arbace/repr/c/{parse,convert}.ae`, `bin/lib/ci/dg`, `test/c/`; nothing under `go/`, `overlay/`, `jdk/`.

### C7H.1 What was built

| part | where | what |
|---|---|---|
| CS153 (a) | `bin/lib/ci/dg` | `REPR_C_READ_BUDGET` defaults to 300 s in `bin/ci dg` (header too); the reads of `arbace.repr.c.rtcheck` and `annot` keep 120 s |
| CS150 (a), the three files: "attributes before C23" (`c23-attr-syntax-6`), `vector_size` validation (`bitint-21`) and the file that shares them (`c23-attr-syntax-7`, the "'vector_size' attribute ignored" lines) | `arbace/c/check.ae` | `std-attr-misc!`: under `-pedantic-errors` every `[[...]]` attribute before C23 is a pedwarn (not on a `__extension__` line) and an unprefixed attribute that is no standard one is "ignored" (an error, `__extension__` or not: gcc errs there; `ignored!` no longer skips those lines); `(...)` without a named parameter before C23; `vector_size` of a `_BitInt` ("invalid vector type"); the second `__extension__` of `[[__extension__ __extension__]]` (the preprocessor drops both) as an "'extension' attribute ignored" note (`:always`, read-c) |
| CS150 (a), gcc's recovery in `[[...]]` | `arbace/repr/c/parse.ae`, `arbace/repr/c.ae`, `arbace/c/dg.ae` | `parse/*recover*` (off everywhere but `bin/ci dg`, so cc's comparisons, the round trips and `parse-check` read as before): a stray token in a standard attribute is an error, the rest of the attribute is skipped to its `]]` and the parse goes on; the read then returns its tree (`:recovered`) and the checker's errors count beside the parser's (`:parse-errors`); two colons are the scope only when nothing separates them (`gnu : : x`, a macro's `:/**/:`: the parser sees a preprocessor space as a separator), a pasted C23 `::` is one token |
| CS152 (a), tags and enumerations | `arbace/c/check.ae` | a redefinition with a variably modified member is never "the same" (`vm-type?`); enumerations are compatible only with the same constants and values and, when fixed, a compatible underlying type; pointers between two incompatible enumerations are an error (the integer-class rule no longer joins them); packed and aligned members count in struct compatibility |
| CS152 (a), objects | `arbace/c/check.ae` | an initializer defines an object, and so does a file-scope thread-local one without `extern` (C23): a second definition is "redefinition"; a thread-local array of unknown size has no storage ("storage size", "array size missing"); a struct with a variable-length array member given a non-empty initializer (not through `typeof`: a composite type may not be variable); the initializer's line for pointer conversions (`= &x` on the second line of a declaration) |
| CS152 (a), expressions | `arbace/c/check.ae`, `arbace/repr/c/convert.ae` | `&`, `sizeof`, `alignof`, `typeof`, `typeof_unqual` of a bit-field; `nullptr` (the form `nil`) as an enumerator's value, a designator and the end of a case range, an unnamed bit-field's width at its own line; complex `++`/`--` before C2y; a type name controlling `_Generic` before C2y; tags declared in the initializer of an `auto` object |
| the classes | `test/c/divergences-dg.edn` | "used but never defined" joins `decl-diag` (`c23-typeof-2` now fails there instead of at its bit-field lines) |

### C7H.2 The numbers

`bin/ci dg --record`, 627 files (before: the record of C7G.2, at `f3de897e`; after: the record at `0ced324b`):

| class | before | after |
|---|---:|---:|
| pass | 572 | **587** |
| decl-diag | 30 | 19 |
| attr-diag | 6 | 3 |
| pedantic-diag | 3 | 2 |
| pp-diag | 3 | 3 |
| parse-gap | 3 | 3 |
| read-refused | 10 | 10 |
| not in a class | 0 | 0 |

Per group, pass: gcc-c23 251 of 270 (242), gcc-cpp-c23 16 of 16 (16), gcc-c23-sub 23 of 30 (23), gcc-gnu23 47 of 51 (42), gcc-embed 39 of 47 (39), gcc-va-opt 11 of 12 (11), gcc-bitint 200 of 201 (199).
New passes (15, none lost): CS150 `c23-attr-syntax-6`, `c23-attr-syntax-7`, `bitint-21`; CS152 `gnu23-tag-3`, `gnu23-tag-5`, `gnu23-tag-composite-1`, `gnu23-empty-init-2`, `gnu23-auto-1`,
`c23-thread-local-2`, `c23-complex-1` (was pedantic-diag), `c23-tag-enum-1`, `c23-tag-enum-4`, `c23-nullptr-6`, `c23-tag-composite-3`, `c23-generic-1`.
`bin/ci dg --essential`: 93 of 93. A first full run of the CS152 changes lost nothing but gave `gnu23-tag-composite-5` a false error (its composite type is not of variable size); the rule skips `typeof` declarations, the file is "missed" again as before.

The run level: `bin/ci check --record` over all seven corpora: unchanged but `compcert/perlin.c`, which timed out under the load of this run (`differ status timeout`), so the record was restored with `git checkout` (the committed one, perlin passes, stands). `bin/repr-c check --c23`: 62 files, 59 pass, 3 expected failures, 0 failing. The round trip (`bin/repr-c roundtrip --corpus c23conf,c23,tcc-tests2 --no-run`): c23conf 49 of 49, c23 62 of 62, tcc-tests2 85 pass and 3 known, as before.

### C7H.3 Checks not run

The compiled level (`bin/ci check --aot`): the changes do not reach the builder's node programs (the interpreted run is unchanged). `bin/repr-c parse-check` and `check --tokens` (they need tools/ccdump with go-whim's module cache; CS145/CS156): `parse.ae` changed, but only behind `*recover*` and for the bit-field member form's `:line` metadata in `convert.ae`; the round trips are the evidence. `bin/ci annotations`, `bin/gate`, `bin/ci repl-session`.

### C7H.4 What is left (CS152's stop rule: one file a family)

`decl-diag` 19 (some in the embed group): `c23-enum-7` (the empty-declaration and underlying-type rules, 30 lines of a dozen messages), `c23-complit-5` and `c23-complit-8` (storage classes of compound literals; an inline function's `static` literal),
`c23-constexpr-9` and `c23-constexpr-dfp-2` (floating `constexpr` representability), `c23-tag-6` (completeness inside `_Generic`), `c23-tag-composite-11` (a jump into a variably modified scope), `c23-tag-enum-5` (nested enum redefinitions),
`c23-typeof-2` ("used but never defined" of a static function used in an evaluated `typeof`), `c23-bool-errors-3` (`true`/`false` as enumerators: the parser refuses them), `gnu23-tag-2` (`transaction_safe`), `gnu23-tag-composite-5` (the composite of two variable-size structs).
The attribute files of CS150's list that stay failing (`c23-attr-syntax-3`, `-5`, `c23-has-c-attribute-4`, `bitint-75`, `gnu23-attr-syntax-2/3`) are spread over `attr-diag` (3), `decl-diag` and `parse-gap` by their messages; the rest as in C7G.4.

### C7H.5 Proposals (CS160-CS165)

Each with options, the recommended one first; none decided.

- **CS160 The attribute and parse diagnostics beyond the three files** (`c23-attr-syntax-3/5`, `gnu23-attr-syntax-2/3`, `c23-has-c-attribute-4`, `bitint-75`).
  - Options: (a) by pattern, now that `*recover*` exists (`gnu23-attr-syntax-2/3` are `expected ...` errors the recovery may reach, `attr-syntax-5` is the "ignored" family of other positions); (b) leave.
  - *Recommended: (a)*, beginning with `gnu23-attr-syntax-2/3`.
- **CS161 Recovery for the parser's hard errors in the dg check** (`c23-bool-errors-3`, `c23-stdarg-11`, `c23-complit-5`'s extras, embed files): gcc reports every error of a file, the parser stops at the first it cannot continue from.
  - Options: (a) widen `*recover*` to a resynchronization at the next `;` or `}` of the declaration, errors counted soft only when the tree stays usable; (b) leave, the files stay in `decl-diag`/`parse-gap`.
  - *Recommended: (b)* for now: each file needs its own message set as well, and a false error in a passing file is worse than a missing one; revisit with CS160.
- **CS162 `c23-enum-7`** (empty declarations and underlying types of enumerations).
  - Options: (a) implement the dozen rules (storage class, qualifier, `alignas`, `inline`, `auto`/`register`, "may not be specified here", wrong kind of tag, incompatible underlying types, `restrict`); (b) leave.
  - *Recommended: (b)*: one file; (a) only with CS161.
- **CS163 Static functions used but never defined, and `static` literals in inline functions** (`c23-typeof-2`, `c23-complit-8`): both need a per-function record at the end of the unit (used in an evaluated context, defined or not; `extern` declared later).
  - Options: (a) a unit-end pass with the evaluated/unevaluated distinction (`sizeof`, `typeof` of non-variable types, `_Generic` operands); (b) leave.
  - *Recommended: (b)*: two files, and an unevaluated-context tracking the checker lacks.
- **CS164 `parse-check` and `check --tokens` with `*recover*`** (CS156's heavy window).
  - Options: (a) run them in the window to show the default (off) parser unchanged, with `convert.ae`'s one `:line` change; (b) accept the argument that the code is behind a dynamic var.
  - *Recommended: (a)*, together with CS156.
- **CS165 The essential list.**
  - Options: (a) add `c23-attr-syntax-6` (recovery, "before C23", `(...)`) and `c23-thread-local-2` (definitions) to `test/c/essential-dg.txt` (95 files); (b) leave at 93.
  - *Recommended: (a)*: the recovery is the one mechanism of the dg check the essential subset does not exercise.

### C7H.6 Shipped files, scratch and rule slips

Shipped files changed: none (nothing under `go/`, `overlay/`, `jdk/`).
Scratch in the worktree's `.tmp/`: `c7/` (logs), `c7s/` (gcc probes, `dg1`: a copy of `bin/lib/ci/dg` that keeps a given `CI_DG_ONLY`, `one`: its driver for a list of files), `ci-dg/`, `ci-dg-one/`, `ci-dg-essential/`, `ci-check/`.

Rule slips: one foreground `sleep 240` beyond the tool's limit moved a command to the background (no effect on results; the dg run it waited on was started in the background already).

### Decided (CS160-CS165; the user, 2026-10-31)

All as recommended: CS160, starting with gnu23-attr-syntax-2/3; CS161, not now; CS162, not now; CS163, not now;
CS164, with CS156 in the heavy window; CS165.

## As built: CS160, CS165 (2026-10-31)

On the branch `c23-c7`, in the light tier, after "Decided (CS160-CS165)": two commits (`1962d884`, `c91b1e65`; the second holds the rest, the families sharing `parse.ae` and `check.ae`
too closely to split by hand). Outside the shipped tree (CS100 (b)): `arbace/c/{check,dg}.ae`, `arbace/repr/c/{parse,convert,pp}.ae`, `test/c/`; nothing under `go/`, `overlay/`, `jdk/`.

### C7I.1 What was built

| family | where | what |
|---|---|---|
| `gnu23-attr-syntax-2/3` | `parse.ae` (`*recover*`) | an attribute after the identifier list of a function declarator inside parentheses (`(*f(a, b) [[]])()`) and one in a K&R declaration list (`int x; [[]] int y;`) are errors the parse goes on after; `__auto_type [[...]] x`: empty attributes read, a nonempty list a soft error ("'__auto_type' followed by") |
| `c23-attr-syntax-3` | `parse.ae`, `dg.ae` | attributes before `_Static_assert` (file scope, block), after a label where a statement is expected (`x: [[]];` as the body of an `if`), before the type of `sizeof (...)`, after `struct`/`union`/`enum` where the tag is not defined and is not followed by `;`; with the file's `-pedantic-errors` (`*recover*` is `:pedantic` then, `dg.ae` binds it) a declaration with specifiers and no declarators that begins with attributes is an "empty declaration", and a qualified tag with attributes an "invalid use of attributes in empty declaration" |
| `c23-attr-syntax-5` | `parse.ae`, `check.ae`, `convert.ae` | an unprefixed attribute that is no standard one takes balanced tokens as its arguments (`[[unknown_attribute(!)]]`); "ignored" after `*` (`int *[[x]] p`), on an unnamed parameter's specifiers, and a function definition's attributes at the declarator's line (`:line` of the name, not of `void` on its own line) |
| `c23-has-c-attribute-4` | `pp.ae` | `__has_c_attribute`'s operand: not an identifier, no identifier after the scope, no closing parenthesis (on `__has_cpp_attribute` too; `__has_attribute` as before) |
| `bitint-75` | `check.ae` | an `asm` operand with a register-only constraint (`"r"`, `"+r"`, no `m`, `o`, `g`, digit...) naming an object of a `_BitInt` wider than 128 bits: "impossible constraint in 'asm'" (the statement is kept as text; a regular expression finds the operands); `bitint-81` (`"rm"`) was a false error of the first version and stays passing |
| CS165 | `test/c/essential-dg.txt` | `c23-attr-syntax-6`, `c23-thread-local-2` and, for the families CS160 made pass, `gnu23-attr-syntax-2`, `c23-attr-syntax-3`, `-5`, `c23-has-c-attribute-4`, `bitint-75`: 100 files, `bin/ci dg --essential` 100 of 100 |

### C7I.2 The numbers

`bin/ci dg --record`, 627 files (before: the record at `e98e3fc6`; after: the record at `c91b1e65`):

| class | before | after |
|---|---:|---:|
| pass | 587 | **593** |
| decl-diag | 19 | 16 |
| attr-diag | 3 | 0 |
| pedantic-diag | 2 | 2 |
| pp-diag | 3 | 3 |
| parse-gap | 3 | 3 |
| read-refused | 10 | 10 |
| not in a class | 0 | 0 |

Per group, pass: gcc-c23 254 of 270 (251), gcc-cpp-c23 16 (16), gcc-c23-sub 23 (23), gcc-gnu23 49 of 51 (47), gcc-embed 39 (39), gcc-va-opt 11 (11), gcc-bitint 201 of 201 (200).
New passes (6, none lost): `gnu23-attr-syntax-2`, `gnu23-attr-syntax-3`, `c23-attr-syntax-3`, `c23-attr-syntax-5`, `c23-has-c-attribute-4`, `bitint-75`. The classes move by message: the three attr-diag files and `c23-attr-syntax-3`
(parse-gap by its "both") passed, and a file or two changed class between decl-diag and parse-gap as their first missed message changed.
`c23-attr-syntax-5` and `-3` were stopped by hard parse errors (`_Static_assert` after attributes at line 19, `(!)` as arguments at line 47), which hid every later line.
`c23-attr-syntax-5` was the "ignored" family of other positions the entry named: the checker had them all but three.

The run level, after the last change: `bin/ci check --record` over all corpora: unchanged but `compcert/perlin.c`, which timed out again (`differ status timeout`) under the run's load, restored with `git checkout` (the committed record, perlin passes, stands).
`bin/repr-c check --c23`: 62 files, 59 pass, 3 expected failures, 0 failing. The round trip (`bin/repr-c roundtrip --corpus c23conf,c23,tcc-tests2 --no-run`): c23conf 49 of 49, c23 62 of 62, tcc-tests2 85 pass and 3 known.
tcc-tests2's level-1 "differ" (1) is `64_macro_nesting.c`, status `known`, class `:macro-context` in `test/repr/c/roundtrip-known.edn` (checked in `.tmp/repr-c-roundtrip/tcc-tests2/files.tsv`: the form's `:c/macro "CAT(A,B)"` against `AB`); the two "-" are the known `18_include.c` (split-declaration) and `95_bitfields.c` (self-include) whose reads are refused. Nothing new there.

### C7I.3 Checks not run

As in C7H.3: the compiled level (`bin/ci check --aot`), `bin/repr-c parse-check` and `check --tokens` (CS164, with CS156: `parse.ae` and `pp.ae` changed, `parse.ae` only behind `*recover*`, `pp.ae`'s new errors only for `__has_c_attribute`'s malformed operands, which no corpus but the dg one has; the round trips and cc's comparisons are the evidence), `bin/ci annotations`, `bin/gate`, `bin/ci repl-session`. The Java check was not touched.

### C7I.4 What is left

CS160's list is done: attr-diag is empty. parse-gap (3): `c23-complit-5` (both, needs CS161), `embed-11` (the include path of the test's own directory), `embed-2` (an extra `#embed` pedwarn). decl-diag (16) as in C7H.4 less `gnu23-attr-syntax-*`, `c23-attr-syntax-3`; pedantic-diag (2): `c23-digit-separators-2`, `embed-1x`.
The recovery's sites are now ten (`attribute` skipping, `__auto_type`, K&R list, identifier-list attribute, `_Static_assert`, label, `sizeof`, struct/enum tag, empty declaration, balanced arguments), each local, none a resynchronization.

### C7I.5 Proposals (CS170-CS174)

Each with options, the recommended one first; none decided.

- **CS170 The next recovery sites.** The remaining files stopped by hard parse errors (`c23-bool-errors-3`, `c23-stdarg-11`, `c23-complit-5`, `embed-18/27/4`) are CS161's.
  - Options: (a) keep adding local sites as a family needs them (what CS160 did: one soft error, one skipped construct); (b) CS161's generic resynchronization at `;` or `}`; (c) leave.
  - *Recommended: (a)* only for `c23-bool-errors-3` (`true`/`false` as enumerators: one site); (b) stays not now, since CS160 showed the local sites reach files without false errors in passing ones.
- **CS171 The pedantic flag in `*recover*`.** It is now `true` or `:pedantic`, so that the parser's soft errors that gcc gives only under `-pedantic-errors` ("empty declaration") are not false errors elsewhere.
  - Options: (a) keep the value as it is; (b) a map (`{:pedantic ... :std ...}`) for the next such rules; (c) move those rules into the checker by carrying the prefix attributes in the forms (changes the forms' text, the round trips' references).
  - *Recommended: (a)* until a second option is needed; (c) only with a reason beyond the dg check.
- **CS172 Definitions' diagnostics at the declarator's line.** `convert.ae` now gives a definition with attributes after its declarator the name's line; gcc reports all of a definition's declaration errors at the name (`void` and `f (void)` on two lines).
  - Options: (a) the same for every definition; (b) as built, only with attributes; (c) the attribute token's own line.
  - *Recommended: (a)* with a dg run (a passing file's other diagnostics may move), after CS163's two files if they are taken.
- **CS173 `asm` operands.** The check reads the statement's text with a regular expression.
  - Options: (a) leave (one file, one rule); (b) the converter reads the operands into forms (`c/asm` with outputs, inputs, clobbers), the checker then types them (constraints against types, `"m"` of a register...); (c) drop the rule.
  - *Recommended: (a)* until a second asm rule appears.
- **CS174 The next family.** Remaining by size: `constexpr` representability (`c23-constexpr-9`, `c23-constexpr-dfp-2`, 60 lines), `c23-enum-7` (CS162), `c23-tag-6` and `c23-tag-enum-5` (one site each), `c23-complit-5/8`.
  - Options: (a) `c23-tag-6` and `c23-tag-enum-5` (completeness in `_Generic`, nested enum redefinition); (b) `constexpr` representability; (c) stop the dg check here.
  - *Recommended: (a)*, two small files; (b) next, as one rule covers 60 lines.

### C7I.6 Shipped files, scratch and rule slips

Shipped files changed: none (nothing under `go/`, `overlay/`, `jdk/`).
Scratch in the worktree's `.tmp/`: `c7/` (logs: `dg160*.log`, `ess160.log`, `final.log`, `final.sh`), `c7s/` (`l4.txt`-`l9.txt`: file lists for `one`), `t/` (probe files), `ci-dg-one/`, `dg-before160.edn` (the record before), `ci-check/`, `repr-c-roundtrip/`.
Rule slips: none known.

### Decided (CS170-CS174; the user, 2026-10-31)

All as recommended: CS170 (a), for c23-bool-errors-3 only, (b) not now; CS171 (a); CS172 (a), with a dg run; CS173 (a);
CS174 (a), c23-tag-6 and c23-tag-enum-5, then (b), `constexpr` representability.

## As built: CS170, CS172, CS174 (2026-10-31)

On the branch `c23-c7`, in the light tier, after "Decided (CS170-CS174)": four commits, one change each (`c95acb83` CS170 (a), `d9bc924e` CS174 (a), `9dcebbdb` CS174 (b), `04e87f97` CS172 (a)).
Outside the shipped tree (CS100 (b)): `arbace/c/check.ae`, `arbace/repr/c/{parse,convert}.ae`, `test/c/`; nothing under `go/`, `overlay/`, `jdk/`.

### C7J.1 What was built

| entry | where | what |
|---|---|---|
| CS170 (a) | `parse.ae` (`*recover*`) | two sites for `c23-bool-errors-3`: `true`/`false` as enumerators (the prelude's macros, so the lookahead is the integer constant `1`/`0` made of them; the macro's name is read from the token) are reported as gcc does ("cannot use keyword ... as enumeration constant") and read as identifiers, in the first position and after a comma; `typedef enum { ... } bool;` (a definition of an enumeration followed by C23's `bool` and no declarator) reports the keyword where gcc expects `;`. Nothing general: CS161 stays not now |
| CS174 (a) | `check.ae` | `c23-tag-6`: a `_Generic` association whose type is incomplete (a struct under its own definition, a tag never defined) is an error; `c23-tag-enum-5`: an enumeration whose definition is nested in its own (in an enumerator's expression, or in the text of its fixed underlying type, which is checked before the tag is entered) marks the outer definition, whose enumerators are then new declarations, so the repeated ones are redeclared as gcc says (the exemption for a repeated definition of C23 6.7.3.3 does not apply) |
| CS174 (b) | `check.ae` | `constexpr` representability of floating, complex and decimal objects (`constexpr-float-bad`): a signaling NaN of another type (`__builtin_nans`, `-nansf`, `-nansl`, the decimal `-nansd32/64/128` are typed, the signaling kind in `:snan`); a double float cannot hold (underflow, overflow, a significand beyond 24 bits); an integer beyond the significand (float, double; long double 64 bits); `__builtin_complex`'s two parts one by one (it was typed int before); a real initializer of a complex object; a binary object given a decimal value ("initializer for a binary floating-point type is of decimal type"); a decimal object given a value that does not fit the format's digits and quantum range (`dec-limits`: 7/16/34 digits, quanta -101..90, -398..369, -6176..6111; decimal constants keep their `BigDecimal` in `:dec`, so `0e-200DD` and 20-digit constants are caught, unary minus carries it; integers and binary values go through their exact expansion with trailing zeros stripped); a compound literal's single plain element reaches the check with its result |
| CS172 (a) | `convert.ae` | every function definition of the main file has the name's line (`:line` of the declarator), not only those with attributes (CS160) or an old-style list; a full dg read before and after: the rows of the 627 files equal line for line (result, class and message), the record unchanged |

The decimal case answers CS174 (b)'s question: the decimal types allowed it. They are types of the checker (`:dec32`, `:dec64`, `:dec128`, `:dec64x`) and the converter keeps a decimal constant's digits and exponent as a `BigDecimal`; only the arithmetic of decimals is not modelled, and the rule needs none.

### C7J.2 The numbers

`bin/ci dg --record` (a full run with gcc), 627 files; before: the record at `3ccd3b8e` (CS160's), after: the record at `04e87f97`:

| class | before | after |
|---|---:|---:|
| pass | 593 | **598** |
| decl-diag | 16 | 11 |
| pedantic-diag | 2 | 2 |
| pp-diag | 3 | 3 |
| parse-gap | 3 | 3 |
| read-refused | 10 | 10 |
| attr-diag | 0 | 0 |
| not in a class | 0 | 0 |

Per group, pass: gcc-c23 258 of 270 (254), gcc-cpp-c23 16 (16), gcc-c23-sub 24 of 30 (23), gcc-gnu23 49 of 51 (49), gcc-embed 39 (39), gcc-va-opt 11 (11), gcc-bitint 201 of 201 (201).
New passes (5, none lost), all from decl-diag: `c23-bool-errors-3`, `c23-tag-6`, `c23-tag-enum-5`, `c23-constexpr-9`, `c23-constexpr-dfp-2`. The record moves by commit: 594, 596, 598, 598; the numbers after the first three come
from the dg read and judge of the six groups (`arbace.c.dg read`, `judge --record`, no gcc rerun, the gcc outputs being unchanged), the last from the full `bin/ci dg --record`, which left the file as committed.
`c23-binary-constants-1` timed out (`the evaluated read's budget spent`) in two of the intermediate reads, while the host was loaded by a clean gate run beside it (load average above 15), and passes alone and in the full run; its line was kept in the commits.
`bin/ci dg --essential`: 105 of 105 (before: 100; added `c23-bool-errors-3`, `c23-tag-6`, `c23-tag-enum-5`, `c23-constexpr-9`, `c23-constexpr-dfp-2`).

The run level, after the last change: `bin/ci check --record` over all corpora: unchanged but `compcert/perlin.c`, which timed out again (`differ status timeout`) under the run's load, restored with `git checkout` (the committed record, perlin passes, stands).
`bin/repr-c check --c23`: 62 files, 59 pass, 3 expected failures, 0 failing. The round trip (`bin/repr-c roundtrip --corpus c23conf,c23,tcc-tests2 --no-run`): c23conf 49 of 49, c23 62 of 62, tcc-tests2 85 pass and 3 known (as in C7I.2: `64_macro_nesting.c`, `18_include.c`, `95_bitfields.c`).

### C7J.3 Checks not run

As in C7I.3: the compiled level (`bin/ci check --aot`), `bin/repr-c parse-check` and `check --tokens` (CS164, with CS156; `parse.ae` changed only behind `*recover*`), `bin/ci annotations` (`check.ae` changed: `__builtin_complex`, `__builtin_nans*` and the decimal builtins now have types and the constexpr rule is new; the run level and the round trips are the evidence, since `annotations` compares with cc's checker over corpora that hold no constexpr), `bin/gate`, `bin/ci repl-session`. The Java check was not touched.

### C7J.4 What is left

decl-diag (11): `c23-complit-5` (needs CS161), `c23-complit-8`, `c23-typeof-2` (CS163), `c23-enum-7` (CS162), `c23-stdarg-11`, `c23-tag-composite-11` (jump into the scope of a VLA-typed tag), `gnu23-tag-2` (conflicting types), `gnu23-tag-composite-5` (VLA initialization), `embed-18`, `embed-27`, `embed-4` (hard parse errors, CS161).
read-refused (10): `c23-auto-3`, `c23-constexpr-3` ("a function definition whose declarator is not a function's"), `c23-binary-constants-3`, `c23-utf8char-3`, and the decimal-suffix files (`c23-constants-3/4`, `c23-decimal64x-1` to `-4`: by design, CS3 (a)).
pedantic-diag (2): `c23-digit-separators-2`, `embed-4` (gcc.dg/cpp, traditional C). parse-gap (3): `c23-stdint-1`, `embed-11`, `embed-2`. pp-diag (3): `embed-6`, `embed-7`, `va-opt-6`.
The recovery's sites are now twelve (ten of C7I.4, the enumerator keywords and `bool` after a tag).
Note for `c23-constexpr-3` (refused by the converter): its integer and float lines (`v102`-`v105`, `__LONG_LONG_MAX__` into float and double) are covered by the new rule once the file reads.

### C7J.5 Proposals (CS180-CS184)

Each with options, the recommended one first; none decided.

- **CS180 The converter's refusal of definitions whose declarator is not a function's** (`c23-auto-3`, `c23-constexpr-3`; the message of both is "convert: a function definition whose declarator is not a function's").
  - Options: (a) read what the files hold (probably a function returning a pointer to function or a typeof-declarator definition) and lift the refusal for it; (b) leave it refused by design (the C forms have no node for it); (c) in the dg check only, recover by skipping the definition's body.
  - *Recommended: (a)*, after looking at the two files' declarators: if the case is one declarator shape, it is one site, and c23-constexpr-3 then exercises CS174 (b)'s integer lines and `v98`-`v100` (complex given to a real object), which the checker has yet to give.
- **CS181 The next decl-diag group: tags and VLAs** (`gnu23-tag-2`, `gnu23-tag-composite-5`, `c23-tag-composite-11`: three files, one or two lines each, C23's composite tag types with variably modified members).
  - Options: (a) all three in one change, in the checker's record types (compatibility of a tag redefined with a VLA member, the initializer of a variable-sized object, the jump into a VLA's scope for the new tag); (b) only `gnu23-tag-composite-5` (VLA initialization, the smallest); (c) leave.
  - *Recommended: (b)* first, then the others by what the first shows; they share a model (a tag with a member of variably modified type) that does not exist in the checker yet.
- **CS182 CS163 and CS162** (static functions used and never defined, `static` literals in inline functions: `c23-typeof-2`, `c23-complit-8`; empty declarations of enumerations: `c23-enum-7`), carried over from C7I.5 and still "not now".
  - Options: (a) CS163's two files with one per-function record at the end of the unit (used in an evaluated context, defined or not, declared `extern` later); (b) CS162 with the enum rules for empty declarations (thirty lines in one file, with CS161's recovery); (c) leave both.
  - *Recommended: (a)*: two files, one mechanism; (c) for CS162 until a second file needs its rules.
- **CS183 CS161 and the embed files** (`embed-18/27/4`, `c23-stdarg-11`, `c23-complit-5`: files stopped by hard parse errors, 5 files with 100+ lines).
  - Options: (a) the generic resynchronization at `;` or `}` in `*recover*` (CS161), judged by the whole dg run and the false errors in passing files; (b) one local site per file as in CS170 (embed's `#embed` parameter lists: `expected ')'` after malformed parameters), (c) leave.
  - *Recommended: (b)* for `embed-18/27/4` if their malformed parameters are one construct (three files, 90 lines), (a) still not now: the sites so far reached their files without a false error in a passing one.
- **CS184 Where to stop the dg check.** 598 of 627, the rest 29 files in classes recorded as deferred or gaps, most of the remaining being a file each.
  - Options: (a) take CS180 and CS182 (a) and then stop, the checker's other lines being the long tail of gcc's diagnostics; (b) go on by size until the classes are empty; (c) stop now and move the light tier to the other agenda items (CS164's `parse-check`/`--tokens` with CS156, in the heavy window, then `bin/ci annotations`).
  - *Recommended: (a)*, with (c)'s heavy checks run before the branch is merged, since `check.ae` and `parse.ae` changed in the last three rounds and the checks of CS164 and `annotations` have not run on them.

### C7J.6 Shipped files, scratch and rule slips

Shipped files changed: none (nothing under `go/`, `overlay/`, `jdk/`).
Scratch in the worktree's `.tmp/`: `c7/` (logs `r1`-`r4*.log`, `ess1`-`ess3.log`, `final2.log`, `dgrec.log`; `check-tag.patch`, `check-full.ae`, `fn.ae`, `rows-before172.edn`), `c7s/` (`m1.txt`-`m5.txt`: file lists for `one`), `t/` (probe files), `dg-orig.edn`, `dg-rec.edn`.
Rule slips: none that ran. A heredoc of a Python helper once held the text `open('/dev/null')` in a branch that never executed (`if False`), which the rule forbids even in text; nothing was opened.

## As built: review 006's fixes (2026-10-31)

On the branch `c23-c7`, in the light tier, after review 006 (a helper VM's, at `1048d639`, before CS170-CS174; its report in the worktree's
`.tmp/review-006.md`): each finding checked against the code at `aee075e4` first; all eleven hold (finding 10 confirmed on this amd64 host).
Commits `e09c6b9e` (2), `e8d75815` (1), `20790ae8` (4), `4d3c9bbc` (10), `54d13edf` (3, 5, part of 11), `daa096c0` (6-9, the rest of 11),
`5a989374` (the run level's record), then this section. The judges' unit tests are new: `test/c/judge_test.ae` (`bin/ci judge-test`, `make
ci-judge-test`), on made-up reads and on the small files of `test/c/judge/`, 17 tests, 38 assertions; and `test/c/run/`, a corpus of Arbace's
own run programs (`test/c/corpora.edn`'s `:ci-run`, judged as the run corpora are, gcc's runs in `test/c/ref-gcc15.edn`).

### R6.1 Per finding

| # | holds | the fix | the test (fails before, passes after) |
|---|---|---|---|
| 1 (high) | yes: `verdict` read neither `:unlocated` nor `:other` and judged a `:read-error` as an `:ok` read; a preprocessor panic was `{:pp-only true}`; a missing row passed | `verdict`: `:stopped` for errors of another file or without a line (unless a line-0 directive or `dg-excess-errors` takes them), for a read that failed with no message, and for a read error before the file's later expected lines; `:crash` for a missing row; `read-file` makes a panic of the preprocessor (pp/preprocess's `:panic`) a crash | `no-read-row`, `stray-errors-stop`, `stray-errors-line-zero`, `read-error-accounted`, `read-error-stops-early`, `included-header-error` (`test/c/judge/inc-error.c`), `preprocessor-panic` (`pp-panic.c`): 11 of 13 assertions fail on the verdict before |
| 2 (high) | yes: `binary` negated `k` at its own type (`build.ae`'s `p - k`) | `(ex-neg (conv k long))`, as `assign`; both back ends take the builder's node | `test/c/run/ptr-sub-unsigned.c` (the review's program: gcc 3, ci faulted, "no slab") and `ptr-sub-narrow.c` (`unsigned char`, `unsigned short`, `_Bool`, `unsigned`, `signed char` -128, `short`, `unsigned long`, `char *` minus `unsigned char`, `-=`): both faulted before, pass interpreted and compiled |
| 3 (medium) | yes: the judges exited non-zero only for a row in no class; `parse-gap`'s `fail extra: .*` and `fail both: .*`, `read-refused`'s timeouts and crashes, `decl-diag`'s `expected`, the run level's `read-error` and `long-double` (any failure of a long-double program) were catch-alls; `--aot`'s comparison never reached the status | `arbace.c.run/compare-record` in the three judges: a row the committed record passes that does not pass now fails whatever its class, a known row whose reason changed fails, the record's rows the run does not hold are reported (and fail unless the run is a subset, `CI_SUBSET`); `--aot` fails on an "interpreted only" row and on a run that differs from a passing interpreted one, and `speed` is no class of the compiled level; the classes narrowed (below) | `record-regressions`; a dg judge against a record with a refused file made a pass exits 1 with `REGRESSION`; the full runs below listed exactly the rows that moved |
| 4 (medium) | yes: cirun, `AotMain` and the shard's main exited 2 for their own failures, as Go does on a panic, and stdout was lost | their own failures (usage, read, parse, no entry, no package, a panic recovered in main) print "cirun: internal: ..." ("ciaot: ...") after flushing stdout and exit 70; `run1` keeps the line; `result` fails it whatever gcc's status, a known id does not hide it | `runner-internal-not-a-pass` (2 of 3 assertions fail before); cirun on a malformed node program: 70 and the line (before: 2) |
| 5 (medium) | yes: `--record` with `--only`, `--sample` or `--resume` rewrote a corpus from the subset | `--record` with `--only` (and `--aot`'s `--sample`) refuses (status 2); `bin/ci check --resume` keeps `list.tsv` whole, runs `todo.tsv`, appends `run.tsv` (as `--aot` did) | both refusals; a `--resume` of ci-run with one of three runs kept judges 3 of 3 (it ran the two once each: a first version ran them twice from `nodes.tsv`'s repeated rows, fixed) |
| 6 (low-medium) | yes | `dg-excess-errors` keeps the missed lines; a line-0 directive takes as many errors as it counts (the extra lines and the errors outside the main file), no fewer and no more | `excess-keeps-missed`, `line-zero-counts`; no file of the corpus moved |
| 7 (low-medium) | yes, both halves | in a file not only preprocessed, a read error stands for an expected line only when every directive of that line expects a syntax, lexical or preprocessor diagnostic (`syntax-expectation?`); the checker's errors in an included unit are `FILE:LINE:` (`check.ae`'s `*unit*`, each of the unit's forms at its own line) | `read-error-not-semantic`, `included-unit-checker-error` (`test/c/judge/inc-check.c` and `.h`) |
| 8 (low) | yes: `{ target { c \|\| c++11 } }` was dropped, `c++14`/`c++17_down`/`c++20` held; `host/dg-options` cut a selector at its first brace; `gcc-agrees` had a dead line | every `c++*` name false (`sel-atom`), the special case gone; `host/dg-options` reads a selector with its braces matched and skips a line whose selector does not hold for C (`host/for-c?`; `host/eval-selector` shared with `arbace.c.dg`); the passes gcc does not agree with are counted and named (12: the embed files' `${srcdir}` directories and `c23-ucn-2`) | `cxx-selectors` (2 of 4 assertions fail before) |
| 9 (low) | yes: 8 passes were unsupported; `c23-enum-3` is `{ target { ! int128 } }`, unsupported on this host as DejaGnu says | `:unsupported` is its own result, not a pass and not a failure; a later `dg-do` that holds wins | `later-dg-do-wins` |
| 10 (low) | yes, on this amd64 host: gcc 15.2 -O0 compiles `(unsigned long)d` as `comisd 2^63; jnb; cvttsd2siq` and prints `8000000000000000` for a NaN; ci printed 0 | `f2i`'s `u64`/`p` branch is `!(x >= 2^63)`; cf2g's `F2IConv` shares it | `test/c/run/nan-to-unsigned.c` (NaN as double and float, 1e30, -1, 9.3e18 to `unsigned long` and `unsigned`, NaN to `long`): fails with the cirun of before (differ stdout), passes |
| 11 (low) | yes, each item | `--essential` fails on a listed file not found and prints "N of M listed files pass"; `check --aot` without `CI_AOT_NODES` compares with `.tmp/ci-check` and makes its own lists; the testsuite's root dropped from the dg messages (the record is the same in any checkout); "got:" is the extra line's own error; `verdict`'s duplicate branches and `check-aot`'s repeated line gone, and its `2>/dev/null` | `extra-line-message`, `messages-without-root` |

### R6.2 The numbers

The dg check, 627 files (`bin/ci dg --record`, the last a full run with gcc; between, the seven groups' read and judge with gcc's outputs unchanged):

| | pass | unsupported | in a class | not in a class |
|---|---:|---:|---:|---:|
| before (`aee075e4`) | 598 (8 of them unsupported) | - | 29 | 0 |
| after finding 1 | 596 | - | 31 | 0 |
| after findings 6-9, 11 | **586** | **8** | 33 | 0 |

Finding 1 costs 2 passes, neither a real one: `c23-has-c-attribute-4` (the five expected errors and a sixth without a position, "-: operator
"__has_c_attribute" requires a parenthesized identifier": now pp-diag) and `c23-if-decls-2` (six parse errors in the included
`c23-if-decls-1.c`, C2Y's if declarations, which gcc accepts there: now parse-gap). No read row was missing and no `dg-do preprocess` file
panicked. Finding 9 moves the 8 unsupported files out of pass (`c23-enum-3`, `c23-float-11`, `c23-no-dfp-1`, `c23-float-no-dfp-1` to `-4`,
`gnu23-builtins-no-dfp-1`). Finding 7 costs 2 more, again no real passes: `gnu23-varmod-1` (the read's "label undefined: err" stood for
"jump into scope": decl-diag) and c-c++-common `embed-10` (a parse error stood for "makes pointer from integer without a cast": parse-gap,
the `${srcdir}` embed directory); `c23-complit-5` misses three more lines (39-41, "storage class specifier in", which its read errors had
answered). (`daa096c0`'s message says those three came from the C++ selectors: wrong, they are finding 7's.) `c23-ucn-1` would have been a
third, its lexical "is outside the UCS codespace" being a read error by right; `syntax-expectation?` takes it. Per group, pass: gcc-c23 249 of
270 (+7 unsupported), gcc-cpp-c23 16, gcc-c23-sub 24 of 30, gcc-gnu23 47 of 51 (+1 unsupported), gcc-embed 38 of 47, gcc-va-opt 11 of 12,
gcc-bitint 201. `bin/ci dg --essential`: 102 of 102; the list lost three files that were never real passes (`c23-has-c-attribute-4`, `embed-10`,
`gnu23-varmod-1`).

The classes narrowed (finding 3): dg's `parse-gap` keeps its parser-message alternatives and gains three exact ones (`c23-if-decls-1.c`'s
`unexpected 'int'`, `-: unexpected EOF`, embed-10's line 5), `read-refused` its four refusals only, `decl-diag`'s `expected` became gcc's
`expected expression before` and a single quoted token, `pp-diag` gains the port's errors without a position (`-: operator
"__has_c_attribute"`, `-: expected '('`), `pedantic-diag` the checker's `#embed` pedwarn inside `__extension__` (`embed-2`, which `fail
extra: .*` held); the run level's `long-double` is an abort or a differing stdout of a long-double program and `_Generic`'s refusal
(`pr39228` and `va-arg-pack-1` now in `gcc-builtin`, where their undefined builtins put them), its `parse-gap` (`read-error`, matching nothing)
removed, `wide` gains ci's "no integer node" of the wide nodes, and a class `signals` is new.

The run level, `bin/ci check --record` over the fifteen corpora (the seven, the seven gcc-*-run groups and ci-run): 3,761 programs, **3,457 pass**
(3,455 of 3,758 before). Gained: ci-run's 3 and `gcc-c23-run/c23-float-6` (CS174 (b)'s typed `__builtin_nans*`, not recorded then: its nodes
are refused at `d9bc924e` and built at `9dcebbdb`). Lost: `torture-execute/20101011-1.c` and its mirror's: their `dg-additional-options
"-DSIGNAL_SUPPRESS" { target { ! signal } }` no longer applies (finding 8's whole selector; before it was cut at its first brace and held),
so the program's SIGFPE handler is tested, which ci does not call (the known class `signals`; gcc's reference, recorded with the option,
exits 0 without it too). The nine gcc-bitint-run programs whose wide nodes ci lacks are "internal: cirun: internal: ci: no integer node ..."
now (finding 4), in `wide`. The record comparison listed exactly these 11 rows as regressions before they were recorded. (`5a989374`'s
message says 3,459: it is 3,457.)

The compiled level: not rerun to the end. The builder change reaches the programs that call `ex-neg` on a type narrower than 64 bits
(a scan of the fifteen corpora with the old builder, `ex-neg` wrapped: 129 programs, a superset, unary minus among them; `.tmp/r6/all.hits`);
their shard with ci-run's three (`bin/ci check --aot --only`, `CI_AOT_NODES=1`, 132 programs) was stopped after its first shard (benchmarksgame, 4
built) when the host's disk reached 80% (a clean gate run in the main checkout was writing; this worktree's `.tmp/` was 0.6 GB), as the brief's
limit says. `test/c/check-aot-linux-amd64.edn` is unchanged; its rows for `20101011-1` (pass, with the old option) and the nine wide
gcc-bitint-run programs are stale until the next `--aot --record` (CS190).

### R6.3 Checks not run

The compiled shard above (CS190); `bin/repr-c check --c23` and the round trip (`check.ae`'s `*unit*` and `host/dg-options` changed; not run, the
disk at 80%); `bin/ci annotations`, `bin/repr-c parse-check` and `check --tokens` (as in C7J.3); `bin/gate`; `bin/ci repl-session`.

### R6.4 Proposals (CS190-CS195)

Each with options, the recommended one first; none decided.

- **CS190 The compiled level after review 006** (the builder's `p - k`, finding 4's internal line, finding 8's options for `20101011-1`).
  - Options: (a) the shard of the scan's 129 programs, ci-run's 3, the two `20101011-1` and the nine wide programs (`--only`, light, about 15
    minutes) as soon as the disk allows, then `--aot --record` over the fifteen corpora in the heavy window; (b) only the full `--aot --record`
    in the heavy window; (c) ci-run's three alone.
  - *Recommended: (a)*: the scan bounds what the change reaches, and the full record needs the window's time anyway.
- **CS191 Signals delivered to a handler** (`20101011-1`: `sigaction`'s SIGFPE handler; the class `signals`).
  - Options: (a) keep it known until C8; (b) crt delivers a synchronous fault (SIGFPE, SIGSEGV, SIGBUS) to a handler `signal` or `sigaction`
    installed, on the faulting thread's C stack, the handler's return resuming after the instruction as the kernel does for SIGFPE (it does not
    return in this test: it exits); (c) also asynchronous signals (`raise`, `kill` to itself).
  - *Recommended: (a)* now, (b) with C8: one program, and (b) touches both back ends' fault path.
- **CS192 `${srcdir}` in dg-options** (`host/dg-options` writes "." for it, so `--embed-directory=${srcdir}/c-c++-common/cpp/embed-dir` is
  relative to the file's own directory, where neither gcc nor the read finds it: embed-10 and embed-11 fail for both, and 12 passes are files
  whose directives gcc does not agree with).
  - Options: (a) `${srcdir}` as the testsuite's root (the group's base), then gcc and the dg record again; (b) leave it, the 12 named by the
    judge.
  - *Recommended: (a)*: one substitution, and the judge's count of gcc's disagreements shows what it fixes.
- **CS193 The dg verdict by message as well as by line** (finding 7's remaining half: a checker error on the right line for the wrong reason
  passes).
  - Options: (a) count, per pass, the expected lines whose dg-error regex does not match the checker's message on that line (the messages
    differ in wording, so a count to read first, not a failure); (b) fail on a mismatch with a table of the checker's wordings against gcc's;
    (c) leave it by line.
  - *Recommended: (a)*: it measures the problem before a table is written.
- **CS194 C2Y's if declarations in the parser** (`c23-if-decls-1/2`: the parser rejects `if (int i = 42)`, so one file stands on a read error and
  the other is stopped by its include).
  - Options: (a) read them (C2Y's selection statements with a declaration), with gcc's pedwarn before C2Y under `-pedantic-errors`; (b) leave.
  - *Recommended: (a)*: two files, one grammar rule, and the pedwarn is the checker's existing `pedwarn!`.
- **CS195 `:unsupported` in the numbers** (8 dg files; the REAP matrix and the As built tables counted them as passes).
  - Options: (a) report pass of supported (586 of 619) with the unsupported apart; (b) count them as non-passes (586 of 627).
  - *Recommended: (a)*, as DejaGnu reports UNSUPPORTED apart.

### R6.5 Shipped files, scratch and rule slips

Shipped files changed: `go/arbace/ci/aot.ae` (finding 4) and `go/arbace/ci/expr.ae` (finding 10), under `go/`, so in the gate's key; nothing under
`overlay/` or `jdk/`. Not shipped: `arbace/c/{build,check,dg,run,cf2g}.ae`, `arbace/repr/c/host.ae`, `tools/cirun/main.go`, `bin/ci`,
`bin/lib/ci/{check,check-aot}`, `test/c/` (`judge_test.ae`, `judge/`, `run/`, `corpora.edn`, `ref-gcc15.edn`, the classes, `check-known.edn`,
`essential-dg.txt`, the dg and run records), the Makefile (`ci-judge-test`), `LICENSE.md` (`test/c/run/`, `test/c/judge/`).
Scratch in the worktree's `.tmp/`: `r6/` (the scan `scan.ae` and `all.hits`, `aot-only.txt`, the dg logs `dg1.log`, `dgA.log`, `dgD.log`,
`dgE.log`, `dgE2.log`, the run level's `checkF.log`, `checkF2.log`, the stopped `aotG.log`, `before.sh` which runs the tests on a file's older
version); `ci-dg/`, `ci-check/` (the last runs); the Go cache deleted.
Rule slips: an exploration `grep` once redirected its errors to `/dev/null`, and a `diff` of two lists once used `<(...)`; both ran, neither
wrote anything. For a minute two waiting loops ran at once (a `Monitor` beside a backgrounded `until` loop; the monitor was stopped). The
compiled shard was stopped at the disk limit rather than finished.

### Decided (CS180-CS184; the user, 2026-10-31)

All as recommended: CS180 (a); CS181 (b), starting with gnu23-tag-composite-5; CS182 (a), CS162 not now; CS183 (b),
CS161 still not now; CS184 (a): the dg check stops here, CS164's heavy checks and `bin/ci annotations` run before the
branch is merged.

### Decided (CS190-CS195; the user, 2026-10-31)

All as recommended: CS190 (a), the 145-program shard when the disk allows, the full `--aot --record` in the heavy
window; CS191 (a), (b) with C8; CS192 (a); CS193 (a); CS194 (a); CS195 (a).

## As built: CS192-CS195 (2026-10-31)

On the branch `c23-c7`, in the light tier, as decided (CS190-CS195): `a21e3f28` (CS192), `a849f362` and `f012488a` (CS193), `163b7c36`
(CS194 and CS195 together) and `68c47007` (roundtrip-known), then this section. Each with a judge-test (`bin/ci judge-test`: 21 tests, 50 assertions,
from 17 and 38). CS190 (the compiled level's shard) was not run, as asked.

### CS192 (a): `${srcdir}` in dg-options

`host/dg-options` takes the testsuite's root as a second argument (the group's base, `gcc/testsuite`; "." when not given); `arbace.c.dg`'s
`directives-cmd` and `ppcheck/file-entry` pass it, so gcc and the read see `--embed-directory=<root>/c-c++-common/cpp/embed-dir`. Test:
`srcdir-in-dg-options`. The dg record moved in two rows: `embed-11` fail (stopped: 4 errors, "-: unexpected EOF") to **pass**, and `embed-10`
(still a read error at line 5, but no longer marked as gcc disagreeing). The passes whose directives gcc does not agree with: 12 to **3**
(`embed-7`, `embed-8`, `c23-ucn-2`: not looked into). Pass 586 to 587.

### CS193 (a): the verdict by message, counted

`arbace.c.dg/msg-mismatch` (test `message-mismatch-report`) lists, for a pass, the expected error lines whose dg-error regexes match none of the
checker's messages on that line (all of a line's errors, `:all-texts` in `read.edn`; lines a read error stood for are not counted);
`bin/ci dg` prints the count and writes the files to `.tmp/ci-dg/msg-mismatch.edn`; no verdict changes. Counting found a bug of its own:
`tcl-words` kept the backslash before a non-alphanumeric in a quoted word (`\\\(` came out `\\(`, not Tcl's `\(`), so regexes with escaped
parentheses never matched; fixed (test `tcl-quoted-backslashes`; text-only changes in three recorded reasons, `c23-stdarg-11`, `embed-18`,
`embed-27`). The count with the fix: **30 of the 126 passes that expect errors (146 lines)** have lines whose regex matches no message of
the checker's. By group: gcc-c23 17 files, gcc-gnu23 5, gcc-embed 5, gcc-bitint 2, gcc-cpp-c23 1. Families (lines): "assignment discards
'const'" 22 (the checker says "discards qualifier from pointer target type"), "redefinition of struct or union" 19 (the checker names the tag:
"redefinition of 'struct q'"), "passing argument N ... incompatible pointer type" 20 (the checker prints the types), array initialized "from a
string literal" 12, "'#embed' expects" 5, "can only be applied to functions" 5, "extra tokens" 4, "both 'long' and '_BitInt'" 4 (the checker:
"two or more data types"), "division by zero in #embed" 4, "duplicate (or overlapping) case value" 2 (the checker: "duplicate case value"),
"enumerator value outside the range" 2, "- began here" 4, and a few single ones. Reading the 30 by eye, 25 are the same diagnostic in other
words; about 5 may be a different reason for the same line: `c23-bool-errors-1/2` (the checker: "two or more data types in declaration
specifiers", gcc: "'bool' cannot be defined via 'typedef'", "cannot be used here"), `c23-tag-enum-6` (redeclaration of an enumerator for
"outside the range"), `embed-9` (No such file for "no include path in which to search"), perhaps `c23-nullptr-6`. So the line verdict has let
through a few right-line, wrong-reason passes, and mostly wording.

### CS194 (a): C2y's if and switch declarations

The parser reads `if (int i = 42)`, `if (int i, j; i = 42)`, `if (D; E)`, `switch (int i = 42; i)` (`selection-head!`: an optional init
declaration ending in `;`, then an expression or a declaration without its `;`; the tree's SelectionStatement gains `:Declaration`,
`:Declaration2`, `:Token5`). The converter writes the statement as the block that declares them (`(let [^int i 42] (when i))`, marked
`:c/sel-decl`; the condition declaration's variable is the condition), the checker gives `pedwarn-c2y!` "ISO C does not support if declarations
before C2Y" on a marked block (off under `-Wno-c23-c2y-compat`, as gcc). ci runs them: `if (int i = 42) ...; if (int i = 0, j = 5; i + j > 3)
...; switch (int s = 3; s) ...` gave 64 as computed by hand. Test: `if-declarations` on the new `test/c/judge/if-decl.c` (the three pedwarns and
an undeclared `i` after the statement; with `-Wno-c23-c2y-compat` only the last). Files: `c23-if-decls-1` passed before for the wrong reason
(the read's error stood for the expected lines; now the checker gives all six pedwarns), `c23-if-decls-2` fail to **pass**. Pass 587 to 588.
Two limits: gcc 15.2 rejects `if (int k = 7; int m = k - 7)` (an init declaration and then a condition declaration), which the parser reads;
the printer writes the block, not the statement, so `c23-if-decls-1` is `converter-gap` in `roundtrip-known.edn` (it was `cc-grammar`), and
`-2` passes.

### CS195 (a): passes of supported files

`bin/ci dg` prints per group "pass N of M supported, unsupported U" and a total line. The dg check, 627 files (the last full run, with gcc):

| | files | unsupported | pass | in a class | not in a class |
|---|---:|---:|---:|---:|---:|
| after review 006's fixes | 627 | 8 | 586 of 619 | 33 | 0 |
| after CS192 | 627 | 8 | 587 of 619 | 32 | 0 |
| after CS194 (final) | 627 | 8 | **588 of 619** | 31 | 0 |

Per group, pass of supported: gcc-c23 250 of 263 (+7 unsupported), gcc-cpp-c23 16 of 16, gcc-c23-sub 24 of 30, gcc-gnu23 47 of 50 (+1),
gcc-embed 39 of 47, gcc-va-opt 11 of 12, gcc-bitint 201 of 201. `bin/ci dg --essential`: 102 of 102. Classes: `parse-gap` lost its
if-declaration alternatives (and `embed-11`'s EOF), its text says so.

### Checks run at the end

`bin/repr-c check --c23`: 62 files, 59 pass, 3 expected failures, 0 failing (the controls refuse). The round trip, `--no-run`: c23conf 49 of
49 pass (levels 1-3 equal), c23 62 of 62, tcc-tests2 88 files, 85 pass and 3 known, none failing. `gcc-c23` (270 files; the if-decls files are
there; the other corpora of the list were not run): 253 pass, 15 known, **2 failing**: `c23-bool-errors-1` and `-2` (level 1: the reread's forms
lose `:c/bad-specs`, the specifier conflict CS133 marks; they come from CS133 and this group's round trip had not been run since, so not
caused by this branch's changes, but not checked against an older commit either), and "31 known entries stale" (entries of other corpora in the
file, not listed in this run). Not run: `bin/ci annotations`, `bin/repr-c parse-check`, `check --tokens`, `bin/gate`, the compiled level.

### Proposals (CS200-CS203)

Each with options, the recommended one first; none decided.

- **CS200 The message count as a verdict** (30 files, 146 lines, above).
  - Options: (a) a table of equivalences per family (the checker's wording against gcc's regex, about twelve families) in `test/c/`, a pass
    failing when a line matches neither a regex nor the table, and the five different-reason files in a known list with a class
    (`wrong-reason`) so they are visible; (b) change the checker's wording to gcc's for these families (the oracle and annotations checks
    compare messages with cc's, so each change needs them rerun); (c) leave the count as a report.
  - *Recommended: (a)*: it turns the count into a guard without touching the wording the other checks fix, and it names the wrong-reason files.
- **CS201 The directives gcc does not agree with** (`embed-7`, `embed-8`, `c23-ucn-2`; `embed-10`'s read error).
  - Options: (a) look at each (gcc's output against the directives; a selector or a message), one line each in the survey; (b) leave them named
    by the judge.
  - *Recommended: (a)*: four files, and the judge already lists them.
- **CS202 If declarations in the printer and against gcc** (the block print; the init-then-condition form gcc 15.2 rejects).
  - Options: (a) the printer writes a `:c/sel-decl` block back as `if (D; E)` and the parser refuses (or marks) the init-then-condition form
    gcc rejects, so `c23-if-decls-1` round trips and leaves `converter-gap`; (b) leave both.
  - *Recommended: (b) for now, (a) with the next printer work*: two files and one unusual form.
- **CS203 `c23-bool-errors-1/2` in the gcc-c23 round trip** (level 1 loses `:c/bad-specs`).
  - Options: (a) record both as `converter-gap` in `roundtrip-known.edn` (their print is a different program than gcc rejects); (b) have the
    converter keep `:c/bad-specs` through the print; (c) first check them against the commit before CS133's follow-ups.
  - *Recommended: (c) then (a)*: the failure is old and the files are error tests whose print gcc rejects anyway.

## As built: re-review 027's fixes (2026-10-31)

On the branch `c23-c7`, in the light tier, after re-review 027 (the helper VM's, at `a21e3f28`, before CS193-CS195; its report in the
worktree's `.tmp/review-027.md`, findings 1-9 and residues R1-R8). Each finding was checked against the code at `0f9964c8` (CS193-CS195
landed after the review and touch none of the nine): all nine hold, the checker's four (1, 2, 7, 8) confirmed by `bin/ci diagnose` on the
review's probes and by gcc 15.2 on this amd64 host. Commits `ca5b705f` (1), `54d66ab6` (2), `f932c695` (7), `06192093` (8), `b20134a4` (3),
`543cc4a7` (4), `fdd714b8` (5), `888d5d5f` (6), `6191eb47` (9), then this section. `bin/ci judge-test`: 30 tests, 90 assertions (from 21 and
50); every new test was run against the code before its fix and failed there, except finding 9's, which pins a layout kept.

### R27.1 Per finding

| # | holds | the fix | the test |
|---|---|---|---|
| 1 (medium-high) | yes: `0.1f + 0.2f`, `1.0f / 3.0f`, `0.1f * 3`, `(float)1 / 3`, `__builtin_complex (1.0f / 3, 0.0f)`, `0.1f - 0.3f` all refused ("not representable"); gcc 15 accepts them | `binary` rounds a float-typed result of `float-op` to float (`Float/valueOf`, as `conv-val` does a constant): C23 evaluates float op float in float (FLT_EVAL_METHOD 0) | `constexpr-float-in-float` on `test/c/judge/constexpr-float.c` (the six accepted, `0.1f + 0.2` and `1.0f / 3.0` refused as gcc does: before, eight lines refused) |
| 2 (medium) | yes: a hard error; gcc 15 pedwarns before C2y ("ISO C does not support '_Generic' association with incomplete type before C2Y", also for `void` and, "with function type", for a function type) | `pedwarn-c2y!` with gcc's two wordings (an error only under `-pedantic-errors`, none in C2y or with `-Wno-c23-c2y-compat`); `void` counted as incomplete, a function type's pedwarn added | `generic-incomplete-association` on `generic-incomplete.c` (`-std=c23 -pedantic-errors`: lines 6-8, the messages gcc's; `-std=c23` and `-std=c2y -pedantic-errors`: none; before, line 6 only, an error in all three). `c23-tag-6` and `c23-generic-1` still pass |
| 3 (medium) | yes, in the three judges | a run with `--only` (`--aot`'s `--sample` too) or `CI_DG_ONLY` leaves `CORPUS/subset` (removed by a whole run); `--judge` reads it, sets `CI_SUBSET` and refuses `--record` (status 2), in `bin/lib/ci/check`, `check-aot` and in the judges themselves (`arbace.c.run/subset-record-refusal`, cf2g's judge, dg's `judge-cmd`); dg's absent files do not fail a subset's judge either | `judge-record-after-subset`: the three `--judge --record` on a made-up work directory with the marker exit 2 and leave the three records as they were (before: 1, 1 and 0, and each record rewritten; the test restores them) |
| 4 (medium) | yes | dg's record comparison: a recorded pass now unsupported, or a recorded unsupported file now supported, is a regression; `--essential` fails on any listed file that does not pass | `pass-to-unsupported`: c23-tag-6 (a recorded pass) made unsupported fails the judge and the essential judge (before: both exit 0); as a pass both exit 0 |
| 5 (medium-low) | yes: `{ target { ! ia32 } }` lines dropped, `{ target ia32 }`, `newlib_nano_io` and `arm*-*-*` lines applied | `host/target-atom` (moved from `arbace.c.dg`'s `sel-atom`): the C++ names false, triples by glob (`?` too), an effective target the table's value, an unlisted name `:unknown`; `host/eval-selector` is three-valued (Kleene's `!`, `&&`, `||`); `dg-options` skips a false line, keeps an unknown one and lists it (`dg-options-info`'s `:uncertain`, `:opts-uncertain` in `files.edn` and `dg.edn`, a line in `ppcheck lists` and `dg directives`); the dg-do, dg-error and xfail selectors keep their rule (an unlisted name holds). A scan of the 4,678 files of the dg groups and the run corpora found ten names in option selectors; `test/c/dg-targets.edn` gains the four effective targets among them, probed: `ia32` false (no `__i386__`), `fpic` true (`-fpic` compiles), `signal` true (Linux), `newlib_nano_io` false (musl) | `option-selectors-on-the-host` (before: `! ia32` gave `[]`, `ia32 && ...` and `arm*-*-*` applied; an unknown name made a line `!` dropped). No file's options moved: the four files whose lines change (`20010129-1`'s `-mtune=i686`, `920501-8`'s and `930513-1`'s `-Wl,-u,_printf_float`, `pr71626-2`'s `-fpic`) carry options `file-config` does not model; no line is left uncertain |
| 6 (medium-low) | yes | the quiet retry: a program the level's record passes that timed out is run again alone, once, and that run is judged (`bin/ci check` per corpus after its runs; `--aot` per shard, the compiled run on the shard's executable and the interpreted one on its node program, before the executable is deleted; the dg read, `dg/retry-timeouts`, for a recorded pass whose read spent its budget, marked `:retried`); each retry prints a line | `quiet-retry-of-a-timeout`: the retry function on made-up reads, and `bin/ci check --corpus ci-run --only` (`ptr-sub-unsigned.c`) on a stand-in cirun (`test/c/judge/cirun-slow-once`) whose first run sleeps past the 10 s: exit 0 (before: 1, "REGRESSION ... now fail differ status timeout") |
| 7 (low-medium) | yes: `sizeof (s.bf = 1)`, `sizeof (s.bf++)`, `++s.bf`, `s.bf += 2`, `p->u--`, `_Alignof (s.bf = 1)`, `typeof (s.bf = 1)`, `typeof (s.bf++)`, `typeof_unqual (--s.bf)` refused | `bit-field?` (`:bits` and an lvalue) in `sizeof`, `_Alignof`, `typeof`, `typeof_unqual` | `bit-field-results` on `bitfield-ops.c` (the nine accepted, `sizeof (s.bf)`, `sizeof ((p->u))`, `typeof (s.bf)`, `_Alignof (p->bf)` refused, as gcc; before, all thirteen refused) |
| 8 (low) | yes, both: `(unsigned __int128)1 << 100`, `((__int128)3) << 80` and its negation refused; `-__builtin_nansf ("")` and `+__builtin_nansf ("")` for a double accepted (gcc refuses both: the review named `-`, `+` is the same) | long double's test is the span of the set bits (`bitLength - getLowestSetBit` > 64); unary `+` and `-` keep `:snan` | `constexpr-long-double-and-snan` on `constexpr-ldouble.c` (x86_64 and int128 only): lines 8-10 refused, as gcc (before: 5-8) |
| 9 (low) | yes: since CS172 a GNU-style definition's `.ae` has `(c/defn` on the first C line and the name on the declarator's line | kept, documented here: the round trip needs neither layout (level 1 compares forms by `form-str`, which drops `:line`; a reread `.ae`'s name symbol carries no `:line` in either layout, so the declarator line lives only in the forms read from C), and the new layout follows the `.ae`'s rule, every nested form on its C line. The musl forms cache (`.tmp/ci-musl/forms`, no key) may hold the old layout and older forms in general: CS211 | `definition-name-layout` on `gnu-defn.c` pins it (it passes before too: nothing changed) |

The residues R1-R8 were not fixed (CS212-CS214). The review's "not covered" decimal case was probed here: gcc 15.2 accepts `constexpr _Decimal32 d =
1.000000000DF;` and the checker refuses it (the written digits judged `strict` though the constant already has the object's type): CS215.

### R27.2 The numbers

The dg check (`bin/ci dg --record`, a full run with gcc): **588 of 619 supported pass, 8 unsupported, 31 in a class, 0 unclassed**, as before;
the record unchanged byte for byte. The message count unchanged (30 of 126 passes, 146 lines). The checker's four fixes move no corpus file:
the dg groups have no float constexpr arithmetic needing it, no incomplete association outside the error tests (which still err under
`-pedantic-errors`), no bit-field assignment under `sizeof`, no wide long double constexpr; the judges' fixes change no verdict of a whole
run (no timeout, no subset, no unsupported change, no option line moved). `bin/ci dg --essential`: 102 of 102.

The run level (`bin/ci check --record` over the fifteen corpora): 3,761 programs, **3,457 pass**, as before, the record unchanged but one row:
`compcert/perlin.c` timed out, and timed out again in the quiet retry alone (it runs 10.2 s alone on this host at a load of about 8, at the
10 s limit, C7J.2's "timed out again"); recorded as a failure it was the run's only regression, and its row was restored with `git checkout`
(load, not the code: the node program and its run are unchanged by these commits). The judge listed it as the one REGRESSION, as finding 3's
comparison should. CS216 below. Per corpus as in R6.2. The compiled level was not run (CS190's window).

### R27.3 Checks run at the end

`bin/ci judge-test`: 30 tests, 90 assertions, all pass. `bin/ci dg --essential`: 102 of 102. `bin/repr-c check --c23`: 62 files, 59 pass, 3
expected failures, 0 failing (the controls refuse). The round trip, `--no-run`: c23conf 49 of 49, c23 62 of 62, tcc-tests2 85 pass and 3 known,
gcc-c23 253 pass, 15 known, **2 failing** (`c23-bool-errors-1` and `-2`, level 1: the reread's forms lose `:c/bad-specs`), "30 known entries
stale" (the other corpora's). CS203 (c), done here: the same group's round trip on the tree of `e575f69b^`, the commit before CS133's
follow-ups (extracted by `git archive` into `.tmp/r27/pre133/`, run with stage 0), passes both files (level 1 equal); `:c/bad-specs` comes
from `62f48f16` (CS133's second commit), so the failure is CS133's, not this round's: CS203 (a) or (b) remains to decide. Not run:
`bin/ci check --aot`, `bin/ci annotations`, `bin/repr-c parse-check`, `check --tokens`, `bin/gate`, `bin/ci repl-session`.

### R27.4 Proposals (CS210-CS216)

Each with options, the recommended one first; none decided.

- **CS210 The dg-do and dg-error selectors' unknown names** (finding 5 made option lines three-valued; the dg-do, dg-error and xfail
  selectors still take an unlisted name to hold, CS142's rule).
  - Options: (a) the same three values there: an unknown dg-do selector makes the file `:unsupported` with the selector named (reported, not
    judged), an unknown dg-error selector keeps the line and reports it; then list the names the corpora use in `test/c/dg-targets.edn`, each
    probed; (b) only report them (a count in `bin/ci dg`'s summary), the rule kept; (c) leave.
  - *Recommended: (b) then (a)*: the count shows how many names are unlisted before the verdicts depend on them.
- **CS211 The musl forms cache's key** (finding 9's residue: `.tmp/ci-musl/forms` is never invalidated by a change to repr/c's reader or
  converter).
  - Options: (a) a key file beside the cache, the digest of `arbace/repr/c{.ae,/}` and the host's files, a mismatch rereading the cache
    (`bin/ci musl read`, a light run); (b) a mismatch only warns; (c) leave it, noted here.
  - *Recommended: (a)*: the cache feeds every run of ci, and a stale form is a silent wrong answer.
- **CS212 The judges' residues R2, R4, R7** (a known program that starts passing is a REGRESSION; the main file decided by a suffix; a
  parser panic counted as a line-0 error).
  - Options: (a) fix the three in the judges, each with a judge-test (R2: "improved: re-record", not a failure; R4: the whole relative path;
    R7: a `panic:` error never expected, the file `:crash`); (b) R7 alone (the only one that can pass a crash); (c) leave.
  - *Recommended: (a)*: three small changes in code the judge-test covers.
- **CS213 Go's fatal errors in cirun and the shards (R1)** (stack exhaustion, out of memory, a concurrent map write exit 2 with no line).
  - Options: (a) `run1` also keeps a `^fatal error:` or `^runtime:` line of stderr, and `result` fails it as internal; (b) leave.
  - *Recommended: (a)*: one grep alternative in both scripts, and a crash then cannot pass as an exit 2.
- **CS214 The other residues R3, R5, R6, R8** (`--aot`'s interpreted side read from unkeyed scratch; `ext-lines` by line alone and no
  system-header exemption; `syntax-expectation?`'s broad regex; `${srcdir}` for the run corpora).
  - Options: (a) R3 (use the diff only with `CI_AOT_NODES`, else say it is not compared) and R5 (key `ext-lines` by unit and line, skip
    pedwarns in system units) now, R6 with CS200's table, R8 noted (right for the dg groups, the only users of `${srcdir}` today); (b) all
    four now; (c) leave.
  - *Recommended: (a)*: R3 and R5 can give wrong answers, R6 and R8 do not today.
- **CS215 constexpr decimal constants of the object's type** (the review's unprobed case, probed: gcc accepts `constexpr _Decimal32 d =
  1.000000000DF;`, the checker refuses it).
  - Options: (a) a decimal constant of the object's own type is judged by its value rounded to the format (`dec-fits?` not `strict` for it),
    another type's strictly as now, with a judge-test and gcc's probes of the neighbouring cases (`_Decimal64` to `_Decimal32`, quantum
    limits); (b) leave until the dg corpus's dfp files need it.
  - *Recommended: (a)*: a false error on a valid program, found by a probe of minutes.
- **CS216 A program at the run level's 10 s limit** (`compcert/perlin.c`: 10.2 s alone, a timeout with or without the retry).
  - Options: (a) a per-program budget in `test/c/check-known.edn`'s style (`perlin.c` 30 s, as goi's per-test limits), the record a pass;
    (b) record it in `speed`; (c) raise the level's budget to 20 s for all.
  - *Recommended: (a)*: one program, and the retry then decides load from slowness.

### R27.5 Shipped files, scratch and rule slips

Shipped files changed: none (nothing under `go/`, `overlay/` or `jdk/`; the `arbace/` files are evaluated from the checkout, though `gate_key`'s `arbace/` covers them). Not shipped: `arbace/c/{check,dg,run,cf2g}.ae`,
`arbace/repr/c/{host,ppcheck}.ae`, `bin/ci`, `bin/lib/ci/{check,check-aot}`, `test/c/` (`judge_test.ae`, `judge/`'s seven new files,
`dg-targets.edn`). Scratch in the worktree's `.tmp/`: `r27/` (the probes, the logs `dg-rec.log`, `check-rec.log`, the round trips',
`pre133/`, the tree of `e575f69b^` the c23-bool-errors check ran on), `ci-dg/`, `ci-dg-essential/`, `ci-check/`.
Rule slips: for about three minutes two waiting loops ran at once (a `Monitor` on the dg run's log and a backgrounded `until` loop; the
second was stopped). Nothing else known.

### Decided (CS200-CS203, CS210-CS216; the user, 2026-11-01)

All as recommended: CS200 (a); CS201 (a); CS202 (b), (a) with the next printer work; CS203 (c), then (a); CS210 (b),
then (a); CS211 (a); CS212 (a); CS213 (a); CS214 (a); CS215 (a); CS216 (a).

## As built: CS200-CS216 (2026-11-01)

On the branch `c23-c7`, in the light tier, from the user's decision of 2026-11-01 (all as recommended; CS202 waits). One change a commit,
each with a `bin/ci judge-test` test run against the code before it (it failed there) and after (it passes); the tests are 43, 139
assertions (from 30, 90). Commits, in order: `1f49fb5a` (CS215), `cf190c73` and `a45d7703` (CS213), `5e31b1e4` (CS211), `9919bf82` (CS212),
`e070b104` (CS214 R3), `f3e372fd` (CS214 R5), `c76b64aa` (CS216), `8ff1f8d8` (CS201), `983cf190` (CS203 (c)), `5b0ab70a` (CS210 (b)),
`ae50a8e5` (CS203 (a)), `afbec044` and `da6440d8` (CS200), `2a800d7c` (the dg record).

### The changes, one by one

- **CS215 (a), constexpr decimal constants of the object's own type.** `constexpr _Decimal32 d = 1.000000000DF;` was refused ("not
  representable"): the written digits were judged `strict`. gcc 15.2, probed: a constant of the object's own type is accepted whatever its
  digits (`1.000000000DF`, `1.0000001DF`, `1.2345678DF`, `1e-102DF` and `1e97DF` for `_Decimal32`, the last two with an `-Woverflow`
  warning); another decimal type's is judged by coefficient and quantum (`1.0000001DD` and `1.000000000DL` for `_Decimal32` refused, `1.0DD`
  accepted since it fits). `constexpr-float-bad` takes a decimal part whose type is the object's own as representable, the others as
  before. Test `constexpr-decimal-of-the-objects-type` on `test/c/judge/constexpr-decimal.c` (before: lines 4, 5, 6 and 11 refused, the
  verdict a failure; after: lines 8 and 10 only, as gcc).
- **CS213 (a), Go's fatal errors in cirun and the shards (R1).** The run's fault line of stderr now also keeps `^runtime: ` and `^fatal
  error: ` (`bin/lib/ci/check`, `check-aot`: the compiled run, the interpreted run, the retries), and `arbace.c.run/result` judges it
  `internal: ...` whatever the exit status. Test `go-fatal-error-is-internal`: `result` on a run row with a fatal line and gcc's status (before: a pass),
  and `bin/ci check --corpus ci-run --only` on a stand-in cirun (`test/c/judge/cirun-fatal`: the two lines of a stack overflow, exit 3, the
  status of `ptr-sub-unsigned.c`): exit 1 (before: 0).
- **CS211 (a), the musl forms cache's key.** `.tmp/ci-musl/forms/key` holds the SHA-256 of the sorted file digests of `arbace/repr/c.ae`,
  `arbace/repr/c/`, `arbace/c/musl.ae` and `test/c/host/`; `bin/ci musl key` prints the checkout's, `fresh` exits 0 when they agree,
  `read` rereads the whole cache when they do not (and writes the key), `ensure` is prepare, then read when stale; `bin/ci check`, `check
  --aot`, `nodes` and `repl-session` call `ensure`. A cache from before the key reads as stale: the first run after this commit rereads the
  1,968 sources (about 6 minutes with 4 threads), and so does every later edit to repr/c (the key covers the whole directory, CS220).
  Test `musl-forms-cache-key` (a made-up `CI_MUSL_FORMS`: no key and a wrong key are not fresh, the checkout's is; before: no `key`).
- **CS212 (a), three judge residues.** R2: `compare-record` no longer flags a recorded `known ...` row that now passes (an `:improved`
  list, printed as "recorded failures now pass (an improvement, not a failure; re-record)"). R4: `read-file` takes the main file as the unit's
  whole name (or the path it was read as), gcc's lines by the exact name, no longer any name ending with the base name; test file
  `suffix.c` includes `xsuffix.c`, whose error was the main file's (`:lines {1 1}`, now `:unlocated`/stray, the verdict `:stopped`). R7: a read whose error
  is the parser's `panic: ...` is `:crash`, never the one error of a line-0 directive (`parser-panic.c`: 30,000 nested parentheses, a stack
  exhaustion with `dg-error` at line 0: a pass before, `:crash` now). Tests `known-row-now-passing-is-no-regression`,
  `main-file-is-the-whole-path`, `parser-panic-is-a-crash` (seven assertions fail before).
- **CS214 (a), R3 and R5; R6 and R8 noted.** R3: `bin/ci check --aot` compares the compiled runs with the interpreter's only with
  `CI_AOT_NODES` (this run's) or a named `CI_INTERP`; otherwise `cf2g`'s judge prints "compiled against interpreted (runs): not
  compared" and reads no scratch (test `aot-interpreted-side-needs-its-run`, a made-up work directory: before, the line was "0 the same,
  0 differ, 1 with no interpreted run" from `.tmp/ci-check`). R5: `pedwarn!` consults the `__extension__` lines of the file the line belongs to:
  the main file's `ext-lines`, or, for a unit whose forms carry their lines (a reread unit's), the unit's own, read from its file against the read's base
  (`read-c` keeps `:base`); and none in a system header (a unit under a directory of the host's search list, `check-unit`'s `:sys-dirs`: gcc
  gives no pedwarn there). The forms read from C carry no `:line` in a unit (the pedwarn then stands on the `#include`'s line in the main
  file, where the main's lines are right), so the unit-keyed part is exercised on forms given lines, as a reread's: test
  `extension-lines-by-unit` (three assertions fail before: the header's own `__extension__` did not silence it, the main file's silenced the header's, a
  system header was pedwarned). R6 (`syntax-expectation?`'s regex) is taken up by CS200's table (the messages are now compared, so a read
  error cannot stand for a semantic message unnoticed); R8 (`${srcdir}` is the corpus base for the run corpora) is right for the dg groups,
  the only users today, and stays noted.
- **CS216 (a), a per-program budget.** `test/c/check-budget.edn` (`{"compcert/perlin.c" 30}`), read by `bin/lib/ci/budget.bash`
  (`ci_budget ID`, 10 unless listed; `CI_BUDGETS` names another file) in `bin/ci check` and `check --aot` for the run, the interpreted run and the
  quiet retry. Test `run-budget-per-program`: the lookups, and a stand-in cirun (`cirun-slow`, 12 s a run) on `ptr-sub-unsigned.c`: exit 1 at
  the default (before and without the file), 0 with a budget of 30.
- **CS201 (a), the four files gcc does not agree with.** `embed-7` and `embed-8` (c-c++-common): the directive is a fatal error (`#embed
  "non-existent-file"`) on the line its `dg-error` expects; `gcc-agrees` called any fatal error a disagreement, now the lines decide (a
  fatal error on another line is still one). `c23-ucn-2`: gcc's output was cut at 20,000 bytes (`head -c`) and the file's 10 errors come after
  186 warnings; the capture is now a filter to the diagnostic lines (`bin/lib/ci/gcc-diag.bash`, up to 1 MB), so gcc's errors at 972..996 are
  there and the directives agree. `embed-10` (c-c++-common) stays a `parse-gap` (the parser stops at the `#embed`'s integers after
  `const char *p =`, where gcc recovers: "makes pointer from integer" then "expected identifier"). The passes gcc disagrees with: 3 to
  **0**. Tests `fatal-error-at-an-expected-line`, `gcc-output-kept-whole` (8,000 warnings then an error; before: the error was lost).
- **CS203 (c) then (a), `c23-bool-errors-1/2`.** (c): the two files recorded as `converter-gap` in `test/repr/c/roundtrip-known.edn` (commit
  `983cf190`). (a), small, so done: the converter keeps `:c/bad-specs` as the specifier words written when all are plain words
  (`["int" "bool"]`; `true` otherwise) and the printer writes a `declare-specs` form's specifiers from them, so `typedef int bool;` and
  `int bool;` print as they were and read again with the same flag; the two known entries are removed again. Only the `declare-specs` form
  (all that the two files have) is printed from the words; a `c/var` with bad specs (`long _Bool z;`) still prints one type (CS221). The test is the round trip's
  level 1 over `gcc-c23` (below).
- **CS210 (b), the unlisted selector names.** `bin/ci dg` prints per group the names of the dg-do, dg-error and xfail selectors that
  `test/c/dg-targets.edn` does not list (taken to hold, CS142's rule kept) and the number of files: **one name in one file**, `int32` in the
  gcc-bitint group; the other six groups none. So CS210 (a) (three values there) would change at most one file. Test
  `unlisted-selector-names-counted`.
- **CS200 (a), the message count as a verdict.** `test/c/dg-wording.edn`: 24 families (a pattern over the `dg-error` regex's text and one
  over the checker's message: "discards .const. qualifier" / "discards qualifier", "redefinition of struct or union" / "redefinition of
  'struct T'", "both 'long' and '_BitInt'" / "two or more data types", the `#embed` and `#elifdef` families, ...) and `:wrong-reason`, five files.
  A pass with an expected-error line whose regexes match no message of the checker's passes only when a family explains the line (the 139
  lines of 25 files are, all of them), else it fails `message: ...` (a new mismatch is a regression, unclassed); the five whose message
  is another reason than gcc's are the new class `wrong-reason` of `test/c/divergences-dg.edn`: `c23-bool-errors-1/2` ('bool' cannot be defined via
  'typedef' / cannot be used here, against "two or more data types"), `c23-tag-enum-6` ("outside the range", against "redeclaration of
  enumerator"), `embed-9` ("no include path", against "No such file or directory") and `c23-nullptr-6` ("not of integer type", against "not an
  integer constant expression"). The four of them in `test/c/essential-dg.txt` leave it (102 files to 98). Test `message-as-verdict` (the verdicts on made-up lines; the committed table has the keys and five wrong-reason files).

### The numbers, before and after

| | before | after |
|---|---|---|
| dg (`bin/ci dg --record`, a full run with gcc 15.2) | 588 of 619 supported pass, 8 unsupported, 31 in a class, 0 unclassed | **583** of 619, 8 unsupported, **36** in a class (the new `wrong-reason`: 5), 0 unclassed; the record moved in those five rows only |
| passes whose directives gcc disagrees with | 3 (`embed-7`, `embed-8`, `c23-ucn-2`) | **0** |
| passes with `dg-error` lines the checker's messages do not match | 30 files, 146 lines, a report | 25 files, 139 lines, all explained by a family; 5 files `wrong-reason`; any other is a failure |
| unlisted selector names (dg-do, dg-error, xfail) | not counted | 1 name (`int32`), 1 file (gcc-bitint) |
| `bin/ci dg --essential` | 102 of 102 | 98 of 98 (the four wrong-reason files left the list) |
| run level (`bin/ci check --record`, fifteen corpora) | 3,761 programs, 3,457 pass; `perlin.c` timed out at 10.2 s | 3,761, **3,457 pass**, the record unchanged byte for byte, `compcert/perlin.c` passes (30 s budget); exit 0 |
| `bin/ci judge-test` | 30 tests, 90 assertions | **43 tests, 139 assertions**, all pass |
| `bin/repr-c check --c23` | 62 files, 59 pass, 3 expected failures, 0 failing | the same |
| round trip `--no-run` (c23conf, c23, tcc-tests2, gcc-c23) | c23conf 49 of 49, c23 62 of 62, tcc-tests2 85 pass, 3 known; gcc-c23 253 pass, 15 known, **2 failing** | the same but gcc-c23 **255 pass**, 15 known, **0 failing** (`c23-bool-errors-1/2` equal at level 1); "30 known entries stale", the other corpora's, as before |

The compiled level (`bin/ci check --aot`) was not run, nor `bin/ci annotations`, `bin/repr-c parse-check`, `check --tokens`, `bin/gate`, `bin/ci
repl-session`. The run level's record was rewritten and came out identical, so `check --record` changed no row; the `perlin.c` row is the pass it
was. A load timeout did not occur in this run.

### Proposals (CS220-)

Each with options, the recommended one first; none decided.

- **CS220 The musl forms cache's key is the whole of repr/c.** Every edit to `arbace/repr/c/` (the printer's, the pedantic notes') rereads
  the 1,968 sources, about 6 minutes, in the next `bin/ci check` or `judge-test`, though only the reader (`c.ae`, `parse`, `pp`, `lex`, `convert`,
  `host`) shapes the forms.
  - Options: (a) key the reader's files alone (`c.ae`, `lex`, `pp`, `parse`, `convert`, `tree`, `host`, `musl.ae`, `test/c/host`), the printer and
    the checkers out; (b) keep the whole directory (safe, slow after a printer edit); (c) key by the forms' own digest (read one file, compare
    with the cache's: seconds, catches what the key list misses).
  - *Recommended: (a)*, with (c) as the check `bin/ci musl fresh` could add later.
  **Decided (the user, 2026-11-01): (a); not built yet.**
- **CS221 `:c/bad-specs` on the other forms.** Only `declare-specs` prints the words; `long _Bool z;` and `typedef long _Bool T;` still print one type.
  - Options: (a) the same words for `c/var`, `c/declare` and `c/typedef` (the printer's `var-str` takes the type from the words); (b) leave:
    no corpus file has them (the gcc-bitint group's `both 'long' and '_BitInt'` lines are checker errors, not round trip files).
  - *Recommended: (b)* until a corpus file needs it.
  **Decided (the user, 2026-11-01): (b).**
- **CS222 Lines on the forms of a unit read from C.** The forms of `read-c`'s units carry no `:line`, so a pedwarn in a header stands on the
  `#include`'s line (the file shown in the message is right, the line is not) and R5's keying only works for reread units.
  - Options: (a) the reader gives a unit's forms their own lines (`convert` already knows the token positions; the `.ae` text of a unit would
    carry them as the main file's does); (b) leave: the message names the header and the include line is where the main file's author looks.
  - *Recommended: (a)* with the next converter work: a header's diagnostics and `__extension__` then are exact.
  **Decided (the user, 2026-11-01): (a); later work, with the next converter work.**
- **CS223 The dg selector name `int32`** (CS210 (b)'s count: one name, one file).
  - Options: (a) probe it and list it in `test/c/dg-targets.edn` (the table then covers every name the corpora use); (b) leave.
  - *Recommended: (a)*, then CS210 (a) is a three-valued rule with no unknown name left to report.
  **Decided (the user, 2026-11-01): (a); not built yet.**
- **CS224 The wording table versus the checker's wording** (CS200 (b), not taken: the oracle and annotations checks compare messages with cc's).
  - Options: (a) keep the table and the two-sided check; (b) change the checker's messages to gcc's for the families where cc's wording does not
    matter (`discards 'const' qualifier`, `redefinition of struct or union`, `both 'long' and '_BitInt'`), table entries removed as they go;
    (c) leave.
  - *Recommended: (a)* until the oracle's message comparison is itself decided.
  **Decided (the user, 2026-11-01): (a).**
- **CS225 The judge-test's duration.** 43 tests take about 4 minutes (the run-level stand-in cirun tests alone about a minute; a musl reread
    after a repr/c edit, CS220, adds six).
  - Options: (a) a `--quick` selection (the pure tests, seconds) beside the full run, the full run kept for commits; (b) leave.
  - *Recommended: (a)*: the light tier runs it after every change.
  **Decided (the user, 2026-11-01): (a); not built yet.**
- **CS226 The embed-10 parse gap** (`const char *p = #embed <f> limit (42);`: the parser stops at the integers after the first, gcc recovers).
  - Options: (a) recovery in the declarator list so the checker runs and gives "makes pointer from integer" (a parser change: its own review); (b)
    leave it `parse-gap`.
  - *Recommended: (b)*: one file and the parser is a port.
  **Decided (the user, 2026-11-01): (b), nothing to build (embed-10 stays `parse-gap`).**
- **CS227 `bin/ci check --aot` without CI_AOT_NODES** now compares nothing with the interpreter.
  - Options: (a) as built (the interpreted side is named or run beside); (b) make `CI_AOT_NODES=1` the default of `--record`, so a recorded compiled level always has been
    compared with a run of its own interpreter; (c) leave.
  - *Recommended: (a)*, (b) with the next compiled-level run, whose cost it doubles.
  **Decided (the user, 2026-11-01): (a).**

### Shipped files, scratch and rule slips

Shipped files changed: none (nothing under `go/`, `overlay/` or `jdk/`). Not shipped: `arbace/c/{check,dg,run,cf2g}.ae`,
`arbace/repr/c.ae`, `arbace/repr/c/{annot,convert,print}.ae`, `bin/ci`, `bin/lib/ci/{check,check-aot,dg,musl,budget.bash,gcc-diag.bash}`,
`test/c/` (`judge_test.ae`, `judge/`'s new files, `dg-wording.edn`, `check-budget.edn`, `divergences-dg.edn`, the records), `test/repr/c/roundtrip-known.edn`.
`gate_key` covers `arbace/`, so a gate run keys the checker's edits. Scratch in the worktree's `.tmp/`: `cs215/`, `pan/`, `bs/`, `ci-dg/`, `ci-check/`,
`ci-musl/` (the reread cache), `repr-c-roundtrip/`, the logs `dg-rec2.log`, `check-rec2.log`, `rt.log`, `c23chk.log`, `jt.log`.
Rule slips: the CS210 (b) commit lacks the `Co-Authored-By` and `Claude-Session` lines (not amended: no rewriting of history); a `bin/repr-c
roundtrip` with no arguments was started by mistake (it would have read every corpus) and ended at its first write into a closed pipe; a
`judge-test` run was moved to the background by the tool's time limit while a command after it swapped files for a before-run, which
briefly left old files in place (restored and re-verified). Nothing else known.

## As built: re-review 031's fixes (2026-11-01)

On the branch `c23-c7` (after `dd1402ec`), in the light tier. Report 031 (the VM's re-review of CS200-CS216 and 027's fixes, reasoned from
the code, nothing run there) gave findings A to G. Each fix is a commit with a `bin/ci judge-test` test run against the code before it (it
failed there) and after (it passes). The probes ran on this host: gcc 15.2.0 (Alpine, x86-64, **with** decimal floating point) and clang
(x86-64), scratch in `.tmp/p/`.

### F31.1 Per finding

- **A, `_Float16` and `__bf16` rounded to their formats** (`f32079b3`). One helper, `round-to-format` (with `round-sig`), used by `conv-val` (a cast),
  `lit-form` (a suffixed constant) and `binary`: `:float`/`:float32` to float as before, `:float16` to binary16 (11 bits, exponent -14..15,
  subnormals, overflow to infinity), `:bf16` to bfloat16 (8 bits, float's exponent range). `constexpr-float-bad` judges a `_Float16` or
  `__bf16` object like a float one (a value, or an integer beyond the significand, or beyond the range, is refused: probe, gcc and clang agree
  on `2049`, `65520`, `257` for a bf16, `1e-8`, `0.1f` for `_Float16`). The probe `constexpr float a = 0.1f16;` (and the cast, `(__bf16)0.1`, `0.1f16 * 3`)
  is accepted by gcc and clang, and now by the checker. **The missed error of 027 #8** (`constexpr double d = 0.1L;`, refused by gcc and clang) is
  closed in the same commit: `lit-form` keeps a long double constant's exact value (`:ld`, its decimal rounded to x87's 64-bit significand,
  `round-ldouble`) and a `double` object refuses it when it differs. Only a literal carries it (`-0.1L` and `0.1L + 0.2L` do not; they stay
  unjudged). Test `constexpr-float16-and-long-double-fraction` (`test/c/judge/constexpr-float16.c`, 7 expected errors); before: the checker
  refused lines 5, 6, 8, 9 (valid) and missed 21. A gcc quirk is *not* mirrored: gcc 15.2 refuses `constexpr _Float16 f = 0.1f16;` and
  `constexpr __bf16 i = 0.1bf16;` ("not representable") though the value is the type's own; clang accepts the first. The checker accepts them.
- **B, a read-error file's message verdict** (`ca64a6c6`, and `88349a27` for CS193's old assertion). `msg-mismatch` no longer skips the lines of
  a `:read-error` file: the lines its read errors stand for are compared with the read error's text, a family of the table allowed. On the
  recorded corpus nothing moves (no read-error pass has an unexplained line; the 139 lines are the same), so the survey's CS214 sentence about
  R6 is now true. Test `read-error-cannot-stand-for-a-semantic-message` (the report's made-up case); before: nil, a pass.
- **C, `gcc-agrees` and fatal errors** (`cef604ef`). `gcc-lines` counts the fatal errors that are not at a line of the main file (another
  file's, or `cc1: fatal error:` with no line) in `:fatal-other`; `gcc-agrees` answers `[:differ ...]` for them unless a line-0 directive or
  `dg-excess-errors` takes errors anywhere. Test: `fatal-error-at-an-expected-line` extended (a header's fatal, an unlocated one, and the
  line-0 case); before: `:yes`. The corpus's disagreements are the same five files as before (the `no-dfp` tests, none a pass).
- **D, a decimal constant of another type** (`de1db5a6`). The constant is rounded to its own type's digits (`round-dec`, half to even)
  before `dec-fits?` judges it against the object's format: `constexpr _Decimal64 a = 1.00000000000000000DF;` (and a `_Decimal128` one, and
  `1.2345678901234567890DF`) is accepted; `1.00000000000000000000DL` for a `_Decimal64` and `1.000000000DD` for a `_Decimal32` are still
  refused. **Probed on gcc 15.2 x86-64**, which has DFP: the ten lines of `.tmp/p/d2.c` agree with the checker. Test: `constexpr-decimal-of-the-objects-type`
  extended (`constexpr-decimal.c`, lines 16 and 17 added); before: the two valid lines were refused.
- **E, the wording table** (`40992f3c`). A family may name `:files` and then applies to those files alone; `tag-conflict` (two files),
  `incompatible-pointer` (five) and `static-assert-message` (one) are anchored (`^redefinition$`, `^message$`, the checker's pattern from its
  line number to the end) and pinned. A `:wrong-reason` entry is `{:lines [...] :why "..."}` and an unexplained line outside its lines fails
  as in any file (`message-verdict`). Test `wording-families-pinned`, and `message-as-verdict` checks the five entries have lines; before: the
  families explained any file. The recorded corpus does not move (583 passes, 0 unexplained).
- **F, `#pragma GCC system_header`** (`7ccbcf6c`). A unit's file is read when a pedwarn is judged (a `delay`, `*unit-info*`, no longer on
  every check) and a header with that pragma is a system header. Test: `extension-lines-by-unit` extended (`ext-main-d.c`: no error with the
  pragma; `ext-main-e.c`: the same header without it, one error; gcc agrees: 0 and 1). The text is scanned, so a pragma in an inactive `#if` counts
  too (CS230). `-isystem` is not modelled (older, CS231).
- **G, `declare-specs` qualifiers** (`dcf9caed`). The printer writes the qualifiers of `(second f)` before the words of `:c/bad-specs`. Test
  `bad-specs-keep-qualifiers` (`const int bool;`, `volatile const char bool;` read and printed back); before: `int bool;`. A `_BitInt(8)` among the
  words makes the words `true` (not plain tokens), so that form still prints one normalized type (CS232).
- **027 #5, the reference options** (not taken). `ref-gcc15.edn` records no row's options; recording them needs `bin/ci record-ref` over
  thousands of files with gcc (hours, and the lists' options are the lists', not the record's), so it is a proposal (CS233).

### F31.2 The numbers, before and after

| | before | after |
|---|---|---|
| `bin/ci judge-test` | 43 tests, 139 assertions | **47 tests, 160 assertions**, all pass (2.5 min with the musl cache fresh; 14 min on the first run after the printer edit) |
| `bin/ci dg --record` (full, gcc 15.2) | 583 of 619 supported, 8 unsupported, 36 in a class, 0 unclassed | the same; the record unchanged byte for byte; gcc disagrees with 5 files (the `no-dfp` tests), none of them a pass |
| `dg-error` lines the messages do not match | 25 files, 139 lines, all explained | the same, 0 unexplained, 5 wrong-reason files with lines |
| `bin/ci check` (run level, against its record) | 3,761 programs, 3,457 pass | 3,761 programs (the default seven corpora 3,582, then the eight `-run` ones 179, two runs): 3,458 pass, the record unchanged; one recorded `speed` row (`compcert/aes.c`) passed this time (load, not a change of ours), so no re-record |
| `bin/repr-c check --c23` | 62 files, 59 pass, 3 expected failures | 62 files, 62 pass (59 agree with gcc, 3 gcc-only-rejects), the same |

Not run: `bin/ci check --aot`, `bin/ci annotations`, `bin/repr-c parse-check`, the round trip corpora, `bin/gate`, `bin/ci dg --essential`
(its list was not touched).

### F31.3 Proposals (CS230-)

Each with options, the recommended one first; none decided.

- **CS230 `#pragma GCC system_header` by text.** F's scan sees the pragma in an inactive branch and after the header's first declarations (gcc
  honours it from where it is *processed*).
  - Options: (a) the preprocessor marks a unit when it processes the pragma and `*system-unit*` takes the mark (exact; a `pp.ae` change and a
    field in the unit's read); (b) keep the scan; (c) scan only before the first non-directive line.
  - *Recommended: (a)* with CS222 (the unit's lines), since both need the preprocessor to say more about a unit.
  **Decided (the user, 2026-11-01): (a); later work, with CS222 (the preprocessor says more about a unit).**
- **CS231 `-isystem` in `host/file-config`.** It is ignored, so a header found through `-isystem` gets pedwarns gcc does not give (gcc and clang
  treat the directory as a system one).
  - Options: (a) model it as an include directory that is also a system directory (`:sys-dirs`); (b) leave: no corpus file uses it.
  - *Recommended: (a)*, a few lines beside F.
  **Decided (the user, 2026-11-01): (a); built (`-isystem DIR` and `-isystem=DIR`, a system directory searched after `-I`; judge-test `extension-lines-by-unit`).**
- **CS232 `:c/bad-specs` with `_BitInt` and the other non-token words** (G's residue; with CS221).
  - Options: (a) keep the specifier source text, not the tokens, so any declare-specs prints as written; (b) leave.
  - *Recommended: (b)* until a corpus file needs it; (a) if CS221 (a) is taken, the one change serving both.
  **Decided (the user, 2026-11-01): (b).**
- **CS233 The reference's options** (027 #5, second half).
  - Options: (a) `record-ref` writes each row's `:std` and `:args` and a check compares them with the lists' on the next `check` (it needs one full re-record
    with gcc, a run of hours, to be explained row by row); (b) a cheap check: hash of the lists' options file kept beside the record
    (no gcc run; catches a drift of how options are read, not a change of gcc's runs); (c) leave.
  - *Recommended: (b)*, then (a) at the next full re-record.
  **Decided (the user, 2026-11-01): (b); later work (the hash of the lists' options file beside the record).**
- **CS234 (corrected by report 053) gcc's refusal of `constexpr _Float16 f = 0.1f16;` is not a bug.** gcc 15.2 on x86-64 defines
  `__FLT_EVAL_METHOD_TS_18661_3__ 0`: `_Float16` and `__bf16` are evaluated in float (C23 Annex H), so `0.1f16` is `(float)0.1` and only a
  conversion rounds to binary16; the first version of this section called it a gcc quirk and left the checker on the type's own format.
  Built in "F53" below. Left open: the evaluation method as a target fact (a `+fp16` or AVX512-FP16 target evaluates in the format,
  `-fexcess-precision=16`): options (a) a `:flt-eval-method` beside `:ldouble-size` in the host's options (recommended, with the next target work);
  (b) leave x86-64's model only.
  **Decided (the user, 2026-11-01): (a), as corrected by report 053: gcc's refusal of `constexpr _Float16 f = 0.1f16;` is FLT_EVAL_METHOD 0's float evaluation, built in F53; the `:flt-eval-method` target fact is later work.**
- **CS235 `-0.1L` and long double arithmetic keep no exact value.** A's `:ld` is carried by a literal only, so `constexpr double d = -0.1L;`
  (also refused by gcc) stays accepted.
  - Options: (a) carry `:ld` through unary minus and plus, and through a cast to long double; (b) represent long double values as BigDecimal
    pairs throughout (the full fix, CS35's "long double as double" undone); (c) leave.
  - *Recommended: (a)*, cheap; (b) only with the compiled level's long double work.
  **Decided (the user, 2026-11-01): (a); built (`:ld` through unary + and - and a cast to long double; judge-test `constexpr-long-double-through-operators`).**

### F31.4 Shipped files, scratch and rule slips

Shipped files changed: none. Not shipped: `arbace/c/{check,dg}.ae`, `arbace/repr/c/print.ae`, `test/c/` (`judge_test.ae`, `judge/`'s new files
`constexpr-float16.c`, `ext-hdr-p.h`, `ext-hdr-q.h`, `ext-main-d.c`, `ext-main-e.c`, `bad-specs-quals.c`, `dg-wording.edn`), this section. Scratch in the
worktree's `.tmp/`: `p/` (probes), `ci-dg.before-031/` (the dg work directory before), the logs `jt1.log`, `dg-rec3.log`, `check3.log`, `one.bash`
(runs single judge-test tests). Rule slips: a `git show ... > /dev/null` was written once (B's before-run; no harm); the baseline `judge-test` was
moved to the background by the tool's time limit while the first edit was made (it had loaded its sources). Nothing else known.

### F53 Re-review 053's fixes (2026-11-01)

Report 053 (the VM, reasoned from the code, nothing of Arbace run) found the conversion `round-to-format` bit-exact (49 edge cases, 14,995
near-ties) but the *evaluation* wrong for `_Float16` and `__bf16`. Probed on this host first (gcc 15.2.0 and clang, x86-64):
`__FLT_EVAL_METHOD__ 0`; `.tmp/p/e1.c` gives gcc errors on `constexpr _Float16 f = 0.1f16;`, `1.0f16/3`, `1.0f16 + 0.0001f16`,
`2049.0f16` for a `_Float16`, none on `constexpr __bf16 j = 2049.0f16 - 1;`, `_Static_assert((int)2049.0f16 == 2049);` and the others, as the report predicted.

- **Findings 1, 2 and 4, one commit** (they share the helper: the commit after `bbb7db91` touching `check.ae`: `constexpr-float16-evaluated-in-float`,
  `constexpr-long-double-range`). A `_Float16`/`__bf16` literal holds the float nearest its exact decimal (`float-of-exact`, via `round-exact`, once,
  not through a double); `binary` and the comparisons convert both operands to float (`eval-type`) and round the result to float; the type
  stays `_Float16`/`__bf16`; `conv-val` (a cast, an initialization) rounds to the format. `round-exact` finds the binary exponent exactly (no
  double estimate), so a long double constant past double's range (`1e400L`, `1e-400L`, `4.94e-324L`) is judged, and every binary object kind
  (`float`, `double`, `_Float16`, `__bf16`, ...) takes `:ld` through `fits-binary?`. Tests failed before (4 assertions) and pass; gcc x86-64 agrees on every
  line of the two files; the old `constexpr-float16.c` still agrees.
- **Finding 3** (`read-error-prefix-is-not-the-message`): a regex is matched without the read error's `FILE:LINE:COL: ` prefix. The report's
  second half (every directive of a line needs a message) was tried and **not kept**: it moves 8 passing files to unexplained (575 passes), so it
  is CS236.
- **Finding 6** (`fatal-error-at-an-expected-line`, extended): a non-fatal error in a header or with no line differs in `gcc-agrees`; gcc disagrees
  with 7 files now (was 5), none a pass.
- **Finding 7**: probed (`.tmp/p/d4.c`, gcc x86-64 DFP): gcc refuses `1.5e-399DD` and `1.5e-399DL` for a `_Decimal32`, accepts it for a
  `_Decimal64` and `1.5e-102DF` for a `_Decimal64`; the checker gives the same five verdicts, so no change and no test (the report's example is
  right in outcome).
- **Finding 5** (`_Atomic int bool;` loses `_Atomic`) and **8** (`:why` unchecked, unpinned families) not taken: CS237, CS238.

Proposals:
- **CS236 Every directive of a line needs its own message.** gcc's dg matches each directive; `msg-mismatch` accepts a line when one regex hits.
  Tried: 8 files become unexplained. Options: (a) classify the 8 (most are probably the checker saying one thing where gcc says two) and add
  families or `:wrong-reason` lines, then make the rule strict (recommended); (b) leave.
- **CS237 `_Atomic` among the qualifiers of `:c/bad-specs`.** The qualifier and the `_Atomic(T)` specifier have the same form `(atomic T)`.
  Options: (a) a meta mark from the converter (`:c/atomic-qual`) that `leading-quals` reads (recommended); (b) with CS232 (a), keep the source text.
- **CS238 Wrong-reason entries and unpinned families.** Options: (a) an optional `:checker` regex per wrong-reason entry and `:files` on every
  family (recommended; one pass over the 24 families with the msg-mismatch report); (b) leave.

### F57 Re-review 057's fixes (2026-11-01)

Report 057 (the VM, no stage 0; gcc 15.2 and clang on aarch64, clang x86-64) confirmed 053's fixes and raised the points below. Each is a commit
with a judge-test test that failed on `81006344` and passes; probes on this host (gcc 15.2 x86-64, clang) agree with the test files line by line.

- **1, `?:` and `_Float16`/`__bf16`** (false errors): the three-operand `?:` converts in float like the other operators (`constexpr-float16-evaluated-in-float`,
  four more lines).
- **2, past long double's range** (false errors): `ld-of` drops the exact value when it rounds to 2^16384 or more; the constant is an infinity, accepted like
  gcc and clang (`constexpr-long-double-range`: `1e5000L`, `-1e5000L`, `1.2e4932L`, `1e7000L`, `1e-5000L`).
- **3, CS231**: an angled `#include` searches the `-I` directories before the system ones (it searched only the system list: the pre-existing divergence;
  forms of a file with `-I` and an angled include can change); `-isystemDIR` is read; `-isystem=DIR` is not (a sysroot's, gcc ignores it here); the
  checker cleans the system directories before the prefix test (`./sys`, `sys/`). The `-I` assertion now asserts the header's pedwarn (`[:ok 1]`).
- **4, pre-existing false errors**: an integer converted to float/`_Float16`/`__bf16` is rounded once (`integer-to-float-rounds-once`); a comparison with a
  long double constant compares exact values (`long-double-compared-exactly`, `0.1L != 0.1`); `_Float16` with `__bf16` is `_Float16` in either order
  (`float16-with-bf16-is-float16`); the `bf16` suffix is gcc's `-pedantic` "non-standard suffix on floating constant" (`bf16-suffix-pedwarn`).
- **5, judges**: `bin/ci dg --judge` reads the kept gcc outputs afresh; a differ from errors outside the main file carries `{:other :fatal-other}`
  (`fatal-error-at-an-expected-line`).

A slip of mine, caught by the full `bin/ci dg` and not by `judge-test`: the `_Float16`/`__bf16` rule first built a set `#{(:k fa) (:k fb)}`, which throws
"Duplicate key" when the kinds are equal, so every `float + float` crashed (26 dg files). Fixed in its own commit with the missing cases in the test.
The unit tests alone were not enough: they only mixed `_Float16` with `__bf16`.

Left open: report 057's CS236-CS238 as before; the `:flt-eval-method` target fact (aarch64 `+fp16`, `-fexcess-precision=16` evaluate `_Float16` in the format,
and `__bf16` follows it there); whether the gcc testsuite has `-I` with an angled include, `-isystem` or `-mavx512fp16` (the corpus rerun shows no
change of the dg record).

## As built: F79, re-reviews 079's fixes (2026-11-01)

Two rounds on `c23-c7` after report 079 (the VM's re-review of F57: gcc 15.2 aarch64 and clang x86-64, no stage 0). Each fix is a commit with a
`bin/ci judge-test` test that failed on the head before it and passes; the expectations are gcc 15.2 (x86-64, this host) line by line, by `gcc -M` for
the include cases.

### F79.1 First round (findings 1 and 2)

- **1, the exact long double comparison (false errors).** `b12c7031` compared exactly as soon as one operand had an exact value (`:ld`), so `0.1L == 0.1L * 1`
  compared 0.1L with the double 0.1. Now the exact path runs only when every long double operand is exact; arithmetic, `?:` and identifiers fall back to the
  doubles (test `long-double-compared-exactly`, extended). `round-exact` gives 0 below a decimal exponent of -6000 (`1e-7000L == 0`). Carrying `:ld` through arithmetic,
  `?:` and constexpr entries is CS235 (b), not done.
- **2, the include search.** `host/config` builds gcc's chains: `-iquote` (quoted includes, after the including file's directory), the `-I` directories less
  those that name a system directory, the system ones (`-isystem`, the host's list, `-idirafter` last), duplicates keeping their first place; `-I-` is not a directory
  (its quote/bracket split is not modelled); `#include_next` continues through `-I` then the system directories. Test `include-search-is-gccs`
  (eleven cases; seven failed before). The run corpora (fifteen, 3,761 programs) were rerun after it: the record is unchanged.

### F79.2 Second round (findings 3 to 6)

- **3, a system header is the directory it was found through.** pp records `:system` on the `enter` record (the directory is one of the system list; a quoted
  include found in the including file's directory takes the *including* file's status, so it is non-system when the includer is the main file, even in `.`, and
  system beside a system header: corrected by F86, finding 1), the unit keeps it, and the checker reads it instead of a path prefix. Fixes `-isystem .`
  (a false error) and nested `-I sy -isystem sy/sub` / `-isystem sy -I sy/sub` (missed errors). Test `system-header-by-the-directory-found-through` (six cases; four failed before).
- **4, the `bf16` pedwarn** moved from the raw tokens (`pedantic.ae`) to `lit-form`: it is judged on the constants the preprocessor delivers, so none in `#if 0` or in an
  unused macro, the use's line for a macro's, `.5bf16` and `BF16` included. Test `bf16-suffix-pedwarn` extended (5 7 8 9 15, as gcc).
- **5, the `--judge` residues.** A kept gcc output without its status is a difference, not an exception (`gcc-cmd`; test `gcc-reread-without-status`); `--judge` stops when
  reading the outputs fails and skips it for `--essential`; the usage says the directives and the read are the last run's (a change to `analyze` reaches a judge-only run
  only after a full run, as before).
- **6, equal-rank floating types.** `_Float32 + float` is `_Float32`, `double + _Float32x` is double, `long double + _Float64x` is long double, `_Float128 + _Float64x`
  is `_Float128`: an interchange `_FloatN` over the standard type over `_FloatNx` (test `equal-rank-float-types`, thirteen `_Generic` assertions, all gcc's).

### F79.3 Numbers

| | before F79 (`fd1ff91a`) | after |
|---|---|---|
| `bin/ci judge-test` | 51 tests, 174 assertions (at `81006344`: 55, 188) | **59 tests, 210 assertions**, all pass (3 min with the musl cache fresh, 13 min after a `pp.ae` edit) |
| `bin/ci dg --record` (full, gcc 15.2) | 583 of 619 supported, 8 unsupported, 0 unexplained | the same, the record unchanged byte for byte |
| `bin/repr-c check --c23` | 62 files, 62 pass (59 agree, 3 gcc-only-rejects) | the same |
| `bin/ci check`, eight `-run` corpora (179) and the seven big ones (3,582), `CI_JOBS` 4 | the record | the record: 3,457 pass counted by the record (the seven big ones 3,353 plus the small 104), no row moved; 31 minutes for the big ones |

A slip of mine, caught by the full `judge-test` and not by the single tests: the finding 5 commit made `--judge` reread gcc's outputs for every group, which threw for the
made-up work directories of `judge-record-after-subset` and `pass-to-unsupported` (3 failures); `gcc-cmd` now reads nothing for a group with no `gcc/` directory.

### F79.4 Open and rule slips

Open: CS235 (b) (`:ld` through arithmetic, `?:` and constexpr entries; `0.1L * 1 != 0.1`, `x != 0.1` for a constexpr long double `x`, `1e400L < 1e5000L` stay false errors,
and the kinds of F86's finding 2 belong with them); `-I-` (silently ignored, see F86); `bf16` in `#if` conditions is not special (gcc says "floating constant in preprocessor
expression" anyway); the `-std=c17` pedwarns, see F86; the gcc corpus's use of the include flags (not checkable here: the dg record is unchanged). Rule slips: none in these
two rounds.

### F86 Re-review 086's fixes (2026-11-02)

Report 086 (the VM) found F79 mergeable with one correction. Each fix is a commit with a judge-test test that failed before and passes; probes with gcc 15.2 here (x86-64).

- **1, the including file's own directory.** In libcpp the current-directory entry takes the *includer's* system status, so `-isystem sy`, `sy/a.h` with `#include "b.h"`
  makes `sy/b.h` a system header (gcc 0 errors; `-I sy`: 1). pp keeps the status on the `Included` slot and gives the `""` entry the including unit's. For the checker a nested
  header is one unit with its includer, so the effect shows on the preprocessor's `enter` records, which the test (`system-header-by-the-directory-found-through`, a seventh case)
  asserts beside the read's errors; the error count alone was 0 before and after, so the checker-level false error the report predicted did not occur (the unit takes `a.h`'s status).
- **2, the exact comparison and the kinds wider than double.** `_Float64x`, `_Float128` and `_Float128x` operands are skipped like long double ones (`0.1L == (_Float128)0.1L`);
  test `long-double-compared-exactly`, four lines.
- **3, `-iquote` of a system directory** is dropped from the quote chain (gcc's `remove_duplicates`): `-isystem s -iquote s -I i` finds `i/zzh.h`. Three cases added to
  `include-search-is-gccs`. (F79's "dialects of `-iquote` beyond the first pick" was too weak: the first pick differed.)
- **4, `_Decimal128` over `_Decimal64x`** in either order (`equal-rank-float-types`, two lines).
- **5, the `-std=c17` pedwarns, stated right:** gcc 15 gives, for every `_FloatN`/`_FloatNx` suffix (`f16 f32 f64 f128 f32x f64x`), "non-standard suffix on floating constant before
  C23" and, for every such type, "ISO C does not support the '_FloatN' type before C23" (with `-pedantic-errors` in `c11`, `c17`, `gnu17`). The checker has neither: missed errors only.
- **6, `-I-`** is ignored, not refused: a file using it is judged with a wrong search order (079's table). Refusing it (a class of its own) is the cheap option and is proposal CS239;
  not done.

Proposal **CS239 `-I-`.** Options: (a) refuse it as unsupported so such files land in a class instead of a wrong verdict (recommended); (b) model the quote/bracket split;
(c) leave. None decided.

### As built: review 110's finding 1, a nested header's own status (2026-11-02)

Report 110 (the VM) found that the checker judged every form of a header unit by the status of the header the main file included: `m.c` with `#include <a.h>`,
`i/a.h` with `#include <b.h>`, `sy/b.h` holding a pedwarn, `-I i -isystem sy -pedantic-errors`: gcc 15.2 gives 0 errors (`gcc -E` marks `sy/b.h` with flag 3), the checker
1. Probes with gcc 15.2 here (x86-64), the cases in `test/c/judge/sysx/mix/`.

- **The records (finding 1b).** libcpp makes a file system when found through a system directory *or* entered from a system header (`_cpp_stack_file`: the
  larger of the two). pp's `enter` records and its `Included` slot now take `(or found-through-system includer-system)`, which subsumes F86's `""`/`@` case:
  `sy2/c.h` (`-isystem`) including `<d.h>` found in `-I i2` records `i2/d.h :system true`. The main file's includes are unchanged (the includer's status is nil there).
- **The checker.** convert records, for a unit whose forms come from more than one file, each form's file (`:form-files`, beside `:forms`) and each of those files'
  first `enter` record with its includers up to the top header (`:files {path {:system s :from includer}}`); a unit of one file gets neither key, so its map, its
  namespace (the forms' hash) and every unit the corpora's reads make of one file are as before. The checker binds `*system-unit*` and `*unit-info*` per form: a
  nested file's forms are system when its record is, or when it or an includer within the unit has `#pragma GCC system_header` (gcc: a file entered after the pragma
  is entered from a system buffer); the top header's forms as before. `*unit*` (the errors' `FILE:` prefix) stays the unit's path. A unit reread from its `.ae`
  (`arbace.c.musl`'s cache) has no `:form-files` and is judged as before.
- **Test** `nested-header-by-its-own-status` (judge-test), nine assertions, each against gcc 15.2: `<b.h>` through `-isystem` (0) and `-I` (1); the includer's own
  pedwarn after the nested system header (`i/a2.h`: 1, with `-I sy` 2); a nested header with the pragma (0); a `-I` header a system header includes (0, `-I sy2` 1); the
  records of both directions. Before the fix (c23-c9 with the test alone) it fails on the first, third and fifth error counts and on the `i2/d.h` record; after, all 68 tests pass.
- **Records.** None changed by reasoning, none rerun: the essential subsets' files with `-pedantic-errors` include only system headers (`<stdbool.h>`, `<float.h>`, ...),
  whose nested headers were system before and after, or `.c` files with no includes; the dg record could lose false errors only on a non-system header including a
  system header that pedwarns, or a nested pragma header, which the next `bin/ci dg` run shows. Findings 2 (the `_Float128` comparison) and 3 (4e5aefaf's example) are not
  part of this: 2 stays with CS235 (b).

### As built: review 130's header gaps, queue 136 (2026-11-02)

The VM's drafts (branch `mac/136-c-headers`, four commits, unrun there) landed on `c23-hdr` off `c23-nf1` (88b357c9) and run here: `bin/ci judge-test` 68 tests, 261
assertions, 0 failures before (17 min), and with the four commits and their new deftests 72 tests, 280 assertions, 0 failures. gcc 15.2.0 on this host gives the
fixtures' counts the tests assert (m-st 1, m-stq 1, m-p7 1, m-p8 0, m-p1 1, m-pn 1, m-op 1, m-p0 2, m-lp 2, m-lm 2; m-lm is asserted 1: the linemarker's own pedwarn is not given).
23f602c9 fixes a regression of 88b357c9: the main file's `Included` slot was nil, `push!` pushed nothing for it, so a header after a system header took that header's status;
`c23-nf1` alone is correct for what it tests but carries this bug, so these commits belong with it. Left: a header entered twice (finding 4), musl's cache path (finding 5).

## Decided (2026-11-01, the user): CS236-CS239

From main's doc/JOURNAL.md, "2026-11-01: the user's decisions after the tag" (VM report 103's sheet 2 for CS236-CS238;
CS239 asked after it). None built yet.
- **CS236 (a):** the 8 files the strict rule makes unexplained classified (families or `:wrong-reason` lines), then
  every directive of a line needs its own message.
- **CS237 (a):** a meta mark from the converter (`:c/atomic-qual`) that `leading-quals` reads, so `_Atomic` is among
  `:c/bad-specs`' qualifiers.
- **CS238 (a):** an optional `:checker` regex per wrong-reason entry and `:files` on every family.
- **CS239 (a):** `-I-` refused as unsupported, a class of its own, instead of a wrong verdict.

## As built: C8, the fourth row (2026-11-01)

On `c23-c8` from main `b0eef5d3` (c23-c7 merged), in the light tier, with the user's decisions of 2026-11-01 for CS236-CS239 (journal "the user's
decisions after the tag"). C8's exit: `bin/reap` **16 of 16 cells checked, 0 problems, no class pending**.

### C8.1 The row

| cell | sources | classes |
|---|---|---|
| `[:c :read]` | `bin/ci dg` (`check-dg-linux-amd64.edn`, 627 files: 575 pass, 8 unsupported, 44 classed); `bin/repr-c check --tokens` (`pp-check-known.edn`, 129 files); `parse-check` (`parse-check-known.edn`, 28) | `divergences-dg.edn` for the dg rows, `divergences-read.edn` (23 classes) for the two known files |
| `[:c :eval]` | `bin/ci check` (`check-linux-amd64.edn`, 3,761 programs: 3,457 pass, 304 classed); `bin/ci annotations` beside (`annot-known.edn`, 56 files) | `divergences.edn`; `divergences-annot.edn` (13) |
| `[:c :aot]` | `bin/ci check --aot` (`check-aot-linux-amd64.edn`, the same 3,761 programs: 3,467 pass, 294 classed or refused) | `divergences-aot.edn`, then `divergences.edn` |
| `[:c :print]` | `bin/repr-c roundtrip` (`roundtrip-known.edn`, 155 files) | `divergences-print.edn` (11) |

The matrix's C row: read 575+ / 156 / 53, eval 3,457 / 219 / 85, AOT 3,467 / 211 / 83, print 0+ / 65 / 90 (pass / by design / deferred; `+`: the check
records only its non-passes). Built: the reader `:c-known` (`test/reap/main.ae`: a known file's section, corpus by corpus, each file a case `corpus file`,
result `known`, reason `CLASS: text`); a cell's `:classes` may be a list of files (the AOT cell's two); `:kind-map {:gap :deferred}` (C's `:gap` kind);
`:repr-clojure-known` no longer reads the `:counts` section of repr/clojure's known file as a corpus (it made 8 false non-passes in Clojure's print cell:
a bug of main's merge, not of C). The three known-file classes files are generated from the known files' `:classes` (their texts; the kinds chosen here: what
cc has and gcc settles, or the evaluated port's limits that the compiled reader removes, is by design; what the port or the converter should close is
deferred); dg's classes gain `:dg-unsupported` (the 8 DejaGnu UNSUPPORTED files, by design) and the AOT record gains `ci-run`'s three programs (it lacked them:
the cell's `:same-cases` check found it). A recorded `unclassified` class exists in two known files and is used by no entry.

### C8.2 The gate

Essential steps (light, stage 0 and TamaGo's go, cached by key, one chain `c-essential`): `c-dg-essential` (`bin/ci dg --essential`, 92 files), `c-check-essential`
(`bin/ci check --only test/c/essential-check.txt`, 45 programs: two passes of each corpus, one program of each class but speed, the negative control's pair),
`c-aot-essential` (`bin/ci check --aot --only test/c/essential-aot.txt`, 10 programs, one shard), `c-c23` (`bin/repr-c check --c23` and `bin/ci repl-session`);
`c-judge-test` in `--full`; the `reap` step reads `test/c/*.edn` and prints the C row. Each command was run here: dg essential 92 of 92 (53 s), check essential
exit 0 (2 min once musl's forms are read), AOT essential exit 0 (1.7 min), c23 62 of 62, the REPL session passes. The planner accepts the table (essential 25 steps,
full 88, run directly on a fabricated hosts file: `bin/gate` was not run). `make check-makefile` passes; targets `ci-check-essential`, `ci-aot-essential`.
The essential-dg list lost six files CS236 made wrong-reason.

### C8.3 CS236-CS239 (decided (a), 2026-11-01)

- **CS236**: `msg-mismatch` is strict (every regex of a line must match a message); 8 passes became wrong-reason files with their lines
  (`c23-attr-reproducible-3`, `-attr-syntax-6`, `-7`, `-attr-unsequenced-3`, `-bool-errors-3`, `-switch-1`, `gnu23-attr-syntax-3`, `embed-3`): dg 583 to **575** passes, 13
  wrong-reason files, the record moved in those 8 rows only. Test `every-directive-needs-a-message`.
- **CS237**: the converter's `:c/atomic-qual` mark; `_Atomic int bool;` and `const _Atomic int bool;` print back whole. Test `bad-specs-keep-qualifiers`, extended.
- **CS238**: every family names its `:files` (none: it explains nothing; the three families nothing used were removed) and every wrong-reason entry its `:checker` regex
  (a line whose message changed to a third wording fails). Test `families-name-their-files-and-wrong-reason-its-wording`.
- **CS239**: `-I-` is refused by the read (`{:refused :option ...}`), classes `refused-option` in the dg and run classes (deferred: CS239 (b) models the split). Test `dash-I-is-refused`.

### C8.4 Numbers

| | before (main) | after |
|---|---|---|
| `bin/reap` | 12 cells (the C row absent) | **16 of 16 checked, 0 problems**, no class pending |
| `bin/ci judge-test` | 59 tests, 217 assertions | **64 tests, 243 assertions**, all pass (3 min) |
| `bin/ci dg` against its record | 583 of 619 supported | **575** (the 8 CS236 files), the record rewritten and explained |
| `bin/repr-c check --c23` | 62 of 62 | the same |

### C8.5 What is left, and what needs the user

C5's, C6's and C7's exit criteria are met on the light path at main (status, `bin/ci check` 3,761 programs none failing and every non-pass in a class, the REPL session, the compiled
level 3,761 translated or refused with a reason and compared with the interpreter's, the dg corpus judged by line and kind); what remains of them is the heavy list (C5.7, C6.7,
C7.4): the embedded tree (`embed.ae` drops the exclusion of `arbace/c` and `arbace/repr/c`, one re-seed carrying ci and cf2g compiled), ci on the executable
(`CI_EXE`, CInterp), `arbace.build.go`'s shards and a `:c-main` program, musl compiled once, the program corpora (SQLite, Lua, zlib, QuickJS), `bin/ci annotations` again, and the
essential gate run (not run here). C9 is the milestone: `GATE_FULL_SERIAL=1 bin/gate --full`, the fixpoint, the re-seed, the tag.

- **CS240 The print, read and annotation cells record no passes.** The round trip, the token and tree checks and the annotations check write their known files only, so the C
  cells print `575+` and `0+`, not a count; a `--record`ed results file per check (rows `[ID RESULT REASON]`, as `bin/ci dg`) would give the counts.
  Options: (a) each check writes a results file (the full runs, hours, once); (b) leave: the known files' non-passes are the judged part; (c) count the passes from the corpora's
  lists (cheap, but the lists are not committed). *Recommended: (a)* at the C9 full run, (b) until then.
  **Decided (the user, 2026-11-02): (a) at the C9 full run, (b) until then.**
- **CS241 The essential subsets' size.** Options: (a) as built (45 programs, 10 AOT programs, 92 dg files); (b) smaller for the AOT. *Recommended: (a)*.
  **Decided (the user, 2026-11-02): (a), as built (45 programs, 10 AOT programs, 92 dg files).**

## As built: C9 (preparation), the re-seed window for C5 and C6 (2026-11-02)

On `c23-c9` from main `619f7335`, in the light tier (the host runs `bin/gate --full`: no executable built, no musl read or compile beyond a copied cache, no
corpus run, no heavy-lock user). The code of the window that a light check can reach is written and checked; what needs the executable is a list of heavy runs
below, in order, with what each costs and writes.

### C9.1 What was built (the commits)

1. **The embedded tree** (C5.7 item 1, CS100): `arbace/j2g/embed.ae` no longer excludes `arbace/repr/c` and `arbace/c`; the keys follow
   (`bin/lib/version.bash`'s `arbace_embedded_sources`, `test/gate/steps.edn`'s `:tools`, `test/gate/audit.ae`'s `:gate`, `bin/arbace-go`'s `gate_key`, which
   also hashes `go/arbace/{ci,cinterp,crt}` again: j2g copies them into the program; the oi and scm forms and the check scripts stay out).
   `test/gate/gate-key-crops-test.bash` follows (12 failures against the old key files, passes). **Effect: any build from this branch has a new `gate_key`
   and `arbace_aot_key`; nothing is built until the window.**
2. **CInterp** (C5.7 item 2, CS103 (c)): `arbace/lang/CInterp.ae` (the static native `runNodes`), j2g's rule (`arbace/j2g/decls.ae`: its natives are
   `cinterp`'s, `out.ae`'s import path `arbace/cinterp`) and the copy of `go/arbace/{crt,ci,cinterp}` into the program (`arbace/j2g/main.ae`, beside
   goi's); `go/arbace/cinterp/natives.ae` reports as tools/cirun does (`cirun: fault:`, `cirun: internal:` with status 70) so the run level's judge reads it
   unchanged. **`bin/ci cinterp-check`** builds that Go side with TamaGo's go and a stand-in `jrt` (the two types it takes) and runs three fixtures
   (`test/c/cinterp/`: a program with arguments, stdout and stderr; a fault; `exit(3)`, their node programs) on it and on cirun against their `.expect`:
   ok on both (it failed with the old fault line). This is the first compile of `cinterp` and of `ci`'s `aot.go` together with `crt` outside tools/cirun.
3. **The Arbace side**: `arbace.c.cinterp` (needs the class: loads only on an executable built with ci) binds `arbace.c.repl`'s `*run-nodes*`; `arbace.c.repl` gets `backend`
   and a `-main` (`run-nodes FILE [ARG...]`, `backend`); `bin/lib/ci/check` runs each node program by `CI_EXE -m arbace.c.repl run-nodes FILE` when `CI_EXE` is set (no
   cirun build); `*run-nodes*`'s contract is one (argv[0] the program's name). Judge-tests `check-runs-nodes-on-ci-exe` (a stand-in executable; it failed without the branch),
   `repl-backend-default`, `cinterp-check-on-an-executable`. On stage 0 the back end stays `cirun`: **a namespace that names `arbace.lang.CInterp` is not prepared there
   ("image: not prepared"), so the build's first pass leaves it out and the second pass (the host's runtime is then the checkout's) compiles it** (`bin/lib/program.bash`'s
   `host_compiles`).
4. **c2j's first compile of ci's and repr/c's namespaces, done light** (`bin/ci c2j-check`, 25 s, 2 GB, the essential step `c-c2j`): the 24 sources compile (2,537 classes). It
   found one failure that would have stopped the re-seed's build after hours: `arbace/c/check.ae`, **"unboxed emit of invoke"** (c2j has no back end for a call of a fn with a
   primitive-hinted return or parameter in a primitive context); fixed by dropping the `^long` hints of ten of the checker's fns. `bin/repr-c check --c23` 62 of 62, the judge tests
   of the float rules pass. (The check reproduces the `bin/arbace-go --build-program` flow of a program's namespaces without j2g and Go.)

### C9.2 The heavy runs, in order (after `bin/gate --full`; the host free)

Estimates from the standalone notes (G1 in two runs 6:18, G2 and G3 4:13 and 4:19, `--verify` 13:42) and the gate's measured peaks (go-build 19 GiB); C's additions: the
embedded namespaces (c2j 25 s), a larger program (about +10% Go), and the `ci`/`crt` packages (std only).

| # | command | time | memory | writes |
|---|---|---|---|---|
| 0 | merge `c23-c9` (and the other crops' window branches, batched) | | | the commit the seed will name |
| 1 | `bin/seed --build` (stage 0 from the current seed) | 10 min | 8 GB | `target/seed/amd64/arbace`, `aot/` |
| 1a | add `-buildvcs=false` to `tools/gateaudit`'s build in `bin/lib/gate-run.bash` (decided by the user, 2026-11-02, for the window, when every step reruns anyway; not changed before); **committed at once** (step 9 refuses a tree that is not clean) | seconds | | `bin/lib/gate-run.bash` |
| 2 | `bin/ci c2j-check`, `bin/ci cinterp-check`, `bin/ci judge-test` on it (light, repeat); then **`bin/j2g-edge verify`** on the merged tree (the frozen JVM, minutes; 118 finding 4: the 24 namespaces embedded name JDK classes, all inside `jdk/sources.txt`'s world, so the edge probably holds; if it differs, `bin/j2g-edge update` and its commit before step 3, else step 3's j2g lacks an answer or the gate's `j2g-edge` step fails after the seed) | 1 + 1 + 3 min, + the edge | 2 GB, 5 GB | `jdk/edge.edn` only if it differs |
| 3 | `bin/arbace-go --build 2>&1 \| tee .tmp/c9-build.log` (heavy lock): the runtime changed (CInterp, a new runtime class: `arbace_runtime_key` differs from stage 0's), so stage 0 compiles nothing (`host_compiles` false) and the build takes the two-run path: the **first program** (with CInterp) prepares and compiles every embedded namespace, `arbace.c.cinterp` included (`prepare` with `aot_out`), then j2g translates again with them. Check: the log has no `image: not prepared: arbace.c.cinterp` line (the first program's compiling prepare and the final image's both print theirs) and its `aot:` line reports 0 failed | ~9 min | 19 GB | `target/arbace-go/amd64/arbace`, `aot/`, `aot.key` |
| 4 | `bin/arbace-go --smoke`; `bin/ci cinterp-check --exe target/arbace-go/amd64/arbace`; `ARBACE_HOST_EXE=that bin/ci repl-session` | 3 min | 2 GB | |
| 5 | `CI_EXE=target/arbace-go/amd64/arbace CI_JOBS=4 bin/ci check` (every run corpus, one process a program; 3,761 programs, +0.15 s a start) | ~45 min | 4 x 0.3 GB | none unless it differs (it must not: one loader); `--record` only for an explained difference |
| 6 | (moved after step 11, as 11a: as ordered here stage 0 is still the old one, and nothing of the compiled level's record enters the seed) | | | |
| 7 | `bin/ci annotations --record` (REPR_C_THREADS 4; rewrites `test/repr/c/annot-known.edn`, `checker-gap` entries removed as they pass, CS93 (a); **writes the results file `test/repr/c/annot-results.edn`, CS240 (a)**) | 31-43 min | 6 GB | `annot-known.edn`, `annot-results.edn` |
| 7a | CS240 (a)'s other full runs, each `--record` over every corpus: `bin/repr-c check --tokens --record` (both levels), `bin/repr-c parse-check --record` (the mutants too), `bin/repr-c roundtrip --record` (not `--no-run`); each rewrites its known file and **writes its results file** (`test/repr/c/{pp-check,parse-check,roundtrip}-results.edn`: every file's row `[ID RESULT REASON]`, `arbace.repr.c.results`); a row `:fail` makes the step fail and `bin/reap` refuse the file | hours (the full runs) | 6 GB | the three known files and results files |
| 8 | **the records of steps 2, 7 and 7a and the notes committed first, `git status --porcelain` empty** (queue 132: `g3/fixpoint.edn` names the version at the fixpoint, HEAD's commit, so records committed after it make step 9 refuse, "of another version", and the fixpoint vouches only for a checkout clean at its start and end), then `rm -rf .tmp/jrt-go/overlay` (the patched runtime's print is cached by mtime alone, 037 finding 3: an older host's print would be packed and `--verify`'s fresh print differ), then `bin/arbace-go --fixpoint` (G1 by stage 0, G2 by G1, G3 by G2); it must end `fixpoint: holds (G2 = G3)`, and only then does it write `g3/fixpoint.edn` (CS256) | ~17 min | 19 GB | `target/arbace-go/fixpoint/g{1,2,3}` |
| 9 | **`git status --porcelain` still empty, HEAD still step 8's commit** (the notes, step 1a, the records of steps 2, 7 and 7a committed before step 8: a dirty `--make` records `:dirty true` and a `-dirty` describe, and `--verify` then builds G1 at another `main.arbaceVersion`, stage 0 != G1, a second `--make`; `--make` now refuses it, CS257), **step 8's `G2 = G3`** (`--make` refuses a G3 without the fixpoint's `fixpoint.edn` for this version, CS256), then `ARBACE_SEED_GO_BUILD=target/arbace-go/fixpoint/g3 bin/seed --make`, and the seed committed | 2 min | 1 GB | `seed/arbace-seed-go.tar.xz`, `.sha256` |
| 10 | `bin/seed --verify` (the seed printed again; stage 0 = G1 = G2 = G3); **no tag before it passes** (a tag on the seed's commit changes `git describe` in the verify's worktree: before CS255's `ARBACE_VERSION` the version map, stage 0 != G1; the window's scripts give the builds the seed's own version, but the order stays) | ~14 min | 19 GB | none (it must pass) |
| 11 | `bin/seed --build` again (stage 0 from the new seed, now with ci compiled in), `bin/ci judge-test`, `bin/reap --record` (the C cells' passes now counted from steps 7 and 7a's results files, CS240 (a): read 575+ and print 0+ become counts), `bin/ci dg --essential` | 15 min | 8 GB | `test/reap/matrix.edn` |
| 11a | (step 6) `bin/ci check --aot` (CI_AOT_NODES=1 for the interpreter beside) on the new stage 0, ci compiled | ~60 min | 4 GB | `test/c/check-aot-linux-amd64.edn` only if it differs (committed before step 12) |
| 12 | `bin/gate` (essential) then `GATE_FULL_SERIAL=1 bin/gate --full`; `bin/gate --clean` at the tag | hours | per the table | `test/gate/clean-TAG.edn` |

Re-seed hazards found while preparing: (a) the first `bin/gate` after the merge rebuilds the gate's executables (the key changed); (b) `bin/lib/ci/check`'s CI_EXE needs the
executable to resolve `-m arbace.c.repl` from its compiled copy (it does once embedded; before, stage 0 evaluates it from the checkout); (c) `arbace.c.cinterp` must be in the
second pass's `namespaces.txt` (it is: the list is the embedded tree's); (d) the first pass leaves `arbace.c.cinterp` out of the compiled tree, so the first executable has
`arbace.lang.CInterp` (a runtime class) but `arbace.c.repl` loads `arbace.c.cinterp` from the second pass's compiled tree, not before: step 3's two runs are not optional (118 finding 5: with stage 0's runtime not the checkout's, the host compiles nothing and the first program compiles every
namespace, cinterp included; step 3's check is what proves it); (e) the box (parked) can no longer build the program (118 finding 6): j2g copies `go/arbace/{crt,ci,cinterp}` into every
program (`arbace/j2g/main.ae`), and `crt/syscall.ae` calls `syscall.Access` and reads `syscall.Stat_t`'s fields, which TamaGo's syscall (`tamago/goroot/go/syscall/`) lacks, so
`bin/arbace-go --build --box` would fail to compile `arbace/crt` for GOOS=tamago; not a window hazard (the box is out of `--full`); for the box's revival, a `tamago` build tag on crt's
file calls or a stub file for them (linux/arm64, which the gate builds, has both through `conv`).

### C9.3 What is not built, with its plan (C6.7 items 3-5; needs the executable)

- **Shards by `arbace.build.go`** (item 3). Today `bin/lib/ci/check-aot` prints cf2g's Go forms and builds a shard with `go build` in a module of its own (the light path). The C6 row says
  `arbace.build.go` with ci and crt as the program's packages (REAP7b's way). Plan: `bin/arbace-go --build-program` takes `:c-main "FILE.c"` (with `c/` sources, `:std`, `:args` in
  `program.edn`; `program_edn` and its key list gain it): the C unit is read, checked, linked with musl and translated by `bin/ci aot` into a `main` package; ci and crt (the
  copies j2g already puts in the shipped program) are listed in the program's goi table (`bin/goi table --program-list`) and the program is a Go program for `bin/build-go --root`.
  Test after the window: `test/programs/c-hello/` in `bin/arbace-go --build-program --check`. Needs: the new stage 0 (ci compiled), 8 GB, 5 min a program.
- **musl compiled once** (item 4, CS122 (a)): the library node program of 1,335 units translated by cf2g once into packages (split by directory, CS122: a 3 GB compile for one package),
  listed in ci's table, and the interpreter and the compiled programs call them. Needs the heavy lock, a Go build of about 6 GB, 20-30 minutes, then the full check again (CS124's time).
- **The program corpora** (item 5): SQLite (3.50.4's `sqlite3.c` and `shell.c`), Lua 5.4.4, zlib 1.2.11, QuickJS. Each is a program of several units; the check is the program's own test
  against gcc's recorded run. Proposed definitions (`test/c/programs.edn`, each `{:id :files :args :stdin}`, references recorded by gcc -O0 into `test/c/ref-gcc15-programs.edn` by an
  extension of `bin/ci record-ref`): zlib = its 13 library units + `example.c` (its self-test, status 0); Lua = `lua.c` and its library, `-e "print(1+1)"` and a script of
  its test suite; SQLite = `shell.c` + `sqlite3.c`, `:memory:` with a script on stdin; QuickJS = `qjs.c` and its libs, `-e "console.log(1+1)"`. Lua and SQLite and QuickJS are the long
  programs (each unit alone took 72-255 s to read, C3's numbers).

### C9.4 Proposals (CS250-CS254), decided (the user, 2026-11-02)

- **CS250 The window's order.** Options: (a) as C9.2: the first executable and the light checks first, CI_EXE run second, the fixpoint and `--verify` last (the re-seed once, batched
  with the other crops' windows); (b) a re-seed for C alone first. *Recommended: (a)*: a seed is rare and every crop's embedded tree is the same build.
  **Decided (the user, 2026-11-02): (a), as C9.2.**
  **Decided (the user, 2026-11-02), on the seed's content: the re-seed for `arbace-reaps-4-crops-v1` embeds C only (`arbace/c`, `arbace/repr/c`, CInterp); C++, OCaml and Scheme embed at a later re-seed, when their rows come.**
- **CS251 Shards by `arbace.build.go`** (C9.3, item 3). Options: (a) as planned (`:c-main` programs; the check keeps `go build` for its 3,761 programs, `arbace.build.go` for the programs and
  the essential subset's shard); (b) the check's shards by `bin/build-go` too (the C6 row to the letter; slower, the lock). *Recommended: (a)*, (b) if the record of the compiled level is
  to be remade anyway.
  **Decided (the user, 2026-11-02): (a), as planned.**
- **CS252 Program corpora's tests** (C9.3, item 5). Options: (a) the proposed definitions; (b) the upstream test suites (`make test`, zlib's `test/`, Lua's `all.lua`): large and needing the
  corpora's own driver; (c) the programs built and run on one input each. *Recommended: (a)* first, (b) per program as it is wanted.
  **Decided (the user, 2026-11-02): (a), the proposed definitions.**
- **CS253 Musl compiled** (C9.3, item 4). Options: (a) by directory packages as CS122; (b) not now: the interpreter's musl is enough for the milestone and `check --aot` translates the used part
  (as today, 3,461 pass). *Recommended: (b) for v1 of the milestone, (a) after*: it is a speed, not a conformance, item.
  **Decided (the user, 2026-11-02): (b) for v1 of the milestone, (a) after.**
- **CS254 The executable's check of the run corpora** (step 5). Options: (a) the 3,761 programs once at the window, none recorded; (b) in the gate as a heavy step. *Recommended: (a)*.
  **Decided (the user, 2026-11-02): (a), once at the window, none recorded.**

**The re-seed's fixes (queue 037's audit, VM report 037; built on `c23-w`, 2026-11-02), decided (the user, 2026-11-02): CS255-CS259 all (a).** Each option (a) is the one 037
recommends and is what is built; test `test/gate/seed-make-test.bash` (19 checks, a fabricated git checkout; 14 fail on the scripts before):
- **CS255 The seed's version map and the clone's tags** (037 finding 1). `:describe` (`git describe --tags --always --dirty`) is linked into stage 0 and G1-G3 and
  written into SEED.edn, so a clone with other tags or another automatic abbreviation prints another seed and `--verify` fails falsely. Options: (a) `--verify` gives the
  builds the seed's own `:version` (`ARBACE_VERSION`, which `arbace_version` honours only when its `:commit` is HEAD, the tree clean and its `:runtime` the checkout's;
  a `--verify REV` of another commit computes its own) and the describe is `--abbrev=12`; (b) only `--abbrev=12`, the tags a precondition written in doc/STANDALONE-NOTES.md;
  (c) `:describe` out of the linked version. *Recommended: (a)*. (Applies from the next seed on: the committed seed's `--verify` runs its own commit's scripts.)
  **Decided (the user, 2026-11-02): (a), as built.**
- **CS256 `--make` packs only a G3 the fixpoint vouched for** (037 finding 2). `fixpoint` wrote `g3/printed-by` before comparing G2 with G3, and `--make` packed any
  build-shaped directory. Options: (a) `fixpoint` writes `printed-by` and `g3/fixpoint.edn` (`{:holds true :g3-sha256 ... :version ...}`) only when G2 = G3, and `--make`
  into the committed seed's path requires it, of this checkout's version and naming the packed executable (another archive: packed with a warning); (b) required for every
  archive; (c) left to `--verify`. *Recommended: (a)*.
  **Decided (the user, 2026-11-02): (a), as built.**
- **CS257 `--make` on a checkout that is not clean** (037 findings 2 and 7; 118 finding 3). It warned on `:dirty true` and saw no untracked file. Options: (a) refused when
  `git status --porcelain --untracked-files=all` lists anything, or outside git (`:commit nil`); (b) refused on tracked changes only, untracked ones warned; (c) as before.
  *Recommended: (a)*: a dirty seed's `--verify` builds G1 at another `main.arbaceVersion` and fails, a second `--make`.
  **Decided (the user, 2026-11-02): (a), as built.**
- **CS258 Go's environment closed** (037 finding 4). Options: (a) `GOENV=off` in every build's environment (`bin/lib/seed-go.bash`'s `seed_go_env`, `bin/arbace-go`'s
  `go_env`, `bin/jrt`'s `goenv`, `arbace.build.go/go-env`), so no user go env file reaches a build, and `$GOROOT/go.env` and the toolchain's binaries stated trusted in
  doc/STANDALONE-NOTES.md SA4 (`go.env` here sets only GOPROXY, GOSUMDB and GOTOOLCHAIN, each overridden or unused; no `~/.config/go/env` on the main host); (b) also
  `GOFLAGS=-mod=readonly` (not empty) and `go.env` and `pkg/tool/*/{compile,link,asm}`, `bin/go` hashed into `seed/goroot.sha256` (a re-pin); (c) as before.
  *Recommended: (a)* (built: `GOENV=off`; the SA4 note not written, doc/STANDALONE-NOTES.md's to the user), (b) at a later re-pin.
  **Decided (the user, 2026-11-02): (a), as built; the SA4 note written in doc/STANDALONE-NOTES.md (`$GOROOT/go.env` and the toolchain's binaries trusted); (b) at a later re-pin.**
- **CS259 A setgid `.tmp`** (037 finding 5): `chmod 0755` keeps a directory's setgid, so a setgid `.tmp` makes every directory member `2755`. Options: (a) `seed_go_tar`
  clears the special bits (`chmod u-s,g-s,o-t` after `0755`); (b) `tar --mode=u=rwX,go=rX`; (c) as before, the host's `.tmp` checked (it is `drwxr-xr-x`).
  *Recommended: (a)*: the tar is unchanged where `.tmp` is not setgid.
  **Decided (the user, 2026-11-02): (a), as built.**
- Not built (037's lower findings, for later): the overlay cached by mtime (finding 3; for this window C9.2 step 8 removes `.tmp/jrt-go/overlay` first), links in the seed
  tree (6), the fixpoint's printed modules compared (8), `--verify --cold` (9), xz's environment (10), arm64 verified (11), `ARBACE_SEED` vs `ARBACE_SEED_GO` (12).

**C's REAP classes after queue 122 (VM report 122's audit; the user's decisions of 2026-11-02, built on `c23-w`).** F1, F3: the VM's fixes
(`:unclassified` out of the read and print classes; a `:c-known` section the known file lacks is a problem). F2: `:missing-builtin` (deferred) before
`:gcc-builtin`, the 24 programs whose runs fault on a builtin ci lacks. F4: the token check's file once (`:per-file`, its worst kind). F5: print's
split-declaration, self-include and macro-context deferred, objects split by CS81 (the 36 macro-made bodies the deferred `:objects-macro-body`, the two
asm labels `:objects` by design). F6: C's eval record bound to gcc's built programs (`:ref-runs`); the dg record's binding to the corpora's dg lists not
built (the lists are not committed). F7: the CS decisions cited in `:decided`. F8-F10: dg's first matches, the classes and patterns that match nothing,
vla's pattern, the titles. The matrix: C read 575+ / 104 / 53, eval 3,457 / 195 / 109, AOT 3,467 / 187 / 107, print 0+ / 7 / 148.

Proposals for the classes no CS decides (F7), decided (the user, 2026-11-02): CS260-CS264 all (a), the classes' `:decided` citing them:
- **CS260 The token and tree checks' by-design classes** (`test/c/divergences-read.edn`: assembler, c23-colons, c23-if-true, cc-panic, digit-separators,
  elifdef, embed, error-test, gnu-builtin-macro, has-c-attribute, has-gnu, invalid-paste, pp-errors, pp-known, pragma-once, u-prefix, va-opt; `:decided "C8"`).
  Each is a place where cc (the token check's oracle) lacks or differs from C23 and gcc settles it, the port following gcc. Options: (a) by design, by CS16 (a)
  and CS40 (a)'s rule (the reference is gcc; cc's gaps are the oracle's), cited in `:decided`; (b) deferred until the token check's oracle is gcc's own tokens
  for those files. *Recommended: (a)*. (`undiagnosed`, deferred, stays: the port's preprocessor is to diagnose it.)
  **Decided (the user, 2026-11-02): (a): by design by CS16 (a) and CS40 (a)'s rule, cited in the classes' `:decided`; `undiagnosed` stays deferred.**
- **CS261 A main file that includes itself** (print's and eval's `self-include`, 11 files, deferred since F5). Options: (a) deferred, the converter to read
  the re-entered main file's tokens as an include of its own (the layer's `c/include` of the main file), with CS104 (a)'s split declarations in C6;
  (b) by design: preprocessor tests whose program is the expansion, refused. *Recommended: (a)*.
  **Decided (the user, 2026-11-02): (a): deferred, cited in `self-include`'s `:decided`.**
- **CS262 Print's reexpansion and run-address** (by design, `:decided "C8"`). Options: (a) by design: reexpansion is N6's limit with the objects equal,
  run-address a program printing addresses (a run's own nondeterminism); (b) reexpansion deferred (the inner invocation recorded so the reread does
  not expand it again). *Recommended: (a)*.
  **Decided (the user, 2026-11-02): (a): by design, cited in `:decided`.**
- **CS263 The annotations' cc classes** (`test/c/divergences-annot.edn`: cc-attr-mode, cc-c23-constants, cc-empty-init, cc-layout, cc-preprocessor,
  cc-redeclared, enum-range, item-placement by design; layout-unjudged deferred; `:decided "C8"`). Options: (a) by design where gcc agrees with the
  checker or the item is cc's placement (CS93 (a)'s rule: the checker follows gcc), layout-unjudged deferred until the judge asks gcc about it;
  (b) all deferred until a gcc-based annotation oracle. *Recommended: (a)*.
  **Decided (the user, 2026-11-02): (a): by design, layout-unjudged deferred, cited in `:decided`.**
- **CS264 The classes the spec or a phase decided** (eval's gcc-builtin SPEC §7.9 and abi SPEC §12.3 by design, threads §6.5 deferred; dg's
  read-refused C7, a gap). Options: (a) the spec's sections and C7 stand as their decision, as cited; (b) each restated as a CS. *Recommended: (a)*.
  **Decided (the user, 2026-11-02): (a): the spec's sections and C7 stand as their decision, as cited.**

**C's read cell counts each corpus file once (VM review 132, F3), decided (the user, 2026-11-02).** Once step 7a's results files exist the read
cell would sum dg's passes, the token check's and the tree check's, so a corpus file counted up to twice, and the tree check's 4,000 mutants
(synthetic files for error recovery, not conformance tests) as files. Decided: the read cell leaves the mutants' rows and entries out and counts
each corpus file once over the two checks, a file passing only when both pass it (one that only one check runs: that check's verdict), a
non-pass of its worst kind. Built in `test/reap/main.ae` (`count-once`, the `:c-known` reader's `:exclude-corpora`; the results still held to
the known files over every corpus, mutants included) and `test/reap/cells.edn` (the two sources `:count-once :c-corpus-files :exclude-corpora
["mutants"]`); judge-test `read-cell-counts-each-file-once`. The matrix recorded again: C read 575+ / 96 / 53 (104 by design before: the two
mutants' `invalid-paste` and six files known to both checks counted once).
