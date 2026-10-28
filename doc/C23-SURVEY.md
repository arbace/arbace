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
first · 14 Risks · 15 Sources · Appendix A: the scratch.

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
