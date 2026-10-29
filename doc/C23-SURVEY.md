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
after CS4 (b) (with CS30-CS43).

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
