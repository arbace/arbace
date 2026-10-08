# The round trip of Go through forms: the oracle

g2c's milestone G2 ([G2C-SURVEY.md](../G2C-SURVEY.md) §6.1, §7) is the round trip
**original Go package → forms (converter) → Go (printer)**. This document describes the oracle
that judges it, independently of forms: given the original source of a package and a candidate
re-printed source, it decides whether they are the same program, with three checks of
increasing strength.

| level | compares | catches |
|---|---|---|
| `tree` | the `go/ast` trees, after normalizations that cannot change the program | any change of the program's text beyond spelling |
| `export` | gc's export data: types, method sets, constants, linknames, pragmas, inlinable and generic bodies | what the package shows its importers |
| `code` | gc's generated code, symbol by symbol: instructions, machine code, relocations, data, type descriptors, DWARF | what the package compiles to |

The tools are Arbace's own code (EPL, standard library only):

- `tools/gocmp/`, a Go module, the comparator (`gocmp`), plus two candidate makers used to prove
  it (`gocmp reprint`, `gocmp mutate`);
- `bin/g2c-check`, the corpus driver.

## How to run

```sh
bin/g2c-check CANDIDATE                     # all of the corpus, three levels, tamago/amd64
bin/g2c-check --arch arm64 CANDIDATE        # tamago/arm64
bin/g2c-check --levels tree --only '^fmt$' CANDIDATE
bin/g2c-check --reference OLD.edn CANDIDATE # fail only on regressions against OLD.edn
bin/g2c-check --record REF.edn CANDIDATE    # also write the results as a reference
bin/g2c-check --known KNOWN.edn CANDIDATE   # failures listed there (with reasons) expected
bin/g2c-check --with-tests --no-tests CANDIDATE   # std with its _test.go files
bin/g2c-check --list-tests                  # the test programs of the corpus
bin/g2c-check --make identity|reprint|canon DIR   # candidates made without forms
bin/g2c-check --mutations [--seed N]        # the oracle's self-test (below)
```

`CANDIDATE` is laid out like `GOROOT`: `CANDIDATE/src/<package dir>/*.go` and
`CANDIDATE/test/...`. The driver builds `gocmp` for the host into `.tmp/g2c-check/bin/`, runs
one comparison per corpus entry in parallel (`-j`, default all cores), and writes under
`.tmp/g2c-check/GOOS-GOARCH/`:

- `results.edn`, read by the Clojure reader: `{:config ... :candidate ... :levels [...]
  :results [...]}` with one entry per line, `[:package "fmt" {:tree :pass :export :pass :code
  :pass :ms 412} ""]`, the string being the first line of the first difference;
- `logs/`, gocmp's full output per entry;
- a summary table on stdout (pass/fail/error/skip per level and kind).

With `--reference`, an entry and level passing in the reference and not now (or missing from
a run that includes it) is a regression and fails the run; without, any failure does, or,
with `--known`, any failure not listed in the known-differences file (`[KIND "PATH" [LEVEL
...] "reason"]` per line; the run also reports listed levels that now pass). `--record FILE`
writes the results as a reference: `results.edn` without times, date or candidate path. g2c's
round trip keeps its references under `test/g2c/` (`bin/g2c roundtrip`,
[CONVERTER-NOTES.md](CONVERTER-NOTES.md)). The environment overrides the defaults:
`G2C_GOROOT` (`/root/tamago-go`), `G2C_GOOS` (`tamago`), `G2C_GOARCH` (`amd64`), and
`G2C_GOCMP_FLAGS` passes flags to gocmp. The driver runs every go command with
`GOTOOLCHAIN=local`, `GOFLAGS=` and `GOWORK=off`.

`gocmp` alone:

```sh
GOROOT=/root/tamago-go GOOS=tamago GOARCH=amd64 \
  gocmp [-levels tree,export,code] [-keep-positions] [-tests] A B
GOOS=tamago GOARCH=amd64 gocmp tests [-v] $GOROOT/test
```

A is the original, a package directory or a program (a `.go` file, or `F.go,G.go,...`: files of
one directory, the others named relative to F's) that the go command can build for the
configuration in the environment; B is the candidate, a directory with a file of the same
name for each of A's files for the configuration (other files in B are ignored), or the files
of a program, spelled as A. With `-tests`, A is a package directory with its `_test.go` files
(see "Test files" below). `gocmp tests` prints the corpus's test programs (below). The go command is `$GOCMP_GO`, else `$GOROOT/bin/go`, else `go`. The output gives, per
level, `pass`, `fail` with the first difference (path to the node, the symbol or the export data
element, with both sides' source or listing), `error` or `skip`, then a line
`RESULT tree=... export=... code=...`. Exit status: 0 equal, 1 different, 2 error, 3 the
original does not build (export and code skipped). Its unit tests: `cd tools/gocmp && go test`.

## The corpus and the configuration

The configuration is `GOOS=tamago`, `GOARCH=amd64` or `arm64`, go1.27.1, TamaGo's toolchain at
`/root/tamago-go` (`VERSION` go1.27.1, 2026-08-28), the decisions of the survey's §9.

- **Packages:** every package of `go list std` (which leaves out `cmd`) with Go files for the
  configuration: 373 of 379 on amd64, 372 on arm64. The six without Go files are test-only
  (`crypto/internal/fips140test`, `embed/internal/embedtest`, `internal/copyright`,
  `internal/coverage/test`, `internal/runtime/wasitest`, `net/internal/cgotest`). The files
  compared are the package's `GoFiles` for the configuration (not its `_test.go` files).
  `unsafe` has no export data or code: those two levels report `skip` for it.
- **Test programs** (`gocmp tests`, the one rule for the oracle's corpus and the helper's,
  `bin/g2c corpus test`): the `.go` files of the directories of Go's test driver
  (`cmd/internal/testdir`, `dirs`) whose action (the first line after build constraints, read
  as the driver reads it, `//run` too) is `run`, `runoutput`, `build`, `buildrun`, `compile`
  or `asmcheck`, that need no other `GOEXPERIMENT` (`-goexperiment` on the action line), and
  that go/build matches for the configuration (build constraints and file name, cgo off):
  1,707 on amd64, 1,702 on arm64. The extra files an action line names join the program
  (`cmplxdivide.go,cmplxdivide1.go`). Programs are compiled as the test driver compiles them,
  without `-complete` (`-gcflags=-complete=false` after the go command's own `-complete`), so
  that the 18 `compile` programs declaring functions without bodies build; the flag only
  allows body-less functions (`noder/writer.go`) and changes no code. (Until 2026-10-08 the
  oracle took every directory and the programs that `go list -export F.go` compiles, which
  applies no build constraints to named files: 19 programs differed from the helper's list
  either way.) The list is written to `corpus-tests.txt`.

## Level 1: the trees

Both files are parsed with `go/parser` (`ParseComments`) and compared by reflection over the
`go/ast` nodes, field by field, after the normalizations below. This is go-lisp's method
(`/root/go-lisp`, commit `2483a496`: `src/cmd/compile/internal/syntax/lisp_roundtrip_test.go`,
`lispCompare`, and `golisp/SPEC.md` §8), on `go/ast` instead of the compiler's `syntax` trees,
with more normalizations, since a printer from forms has more freedom than one from go-lisp.

Not compared, because they carry no meaning for the program:

- **positions**, except where the presence of a position is syntax: `TypeSpec.Assign` (an alias
  `type A = B` against a definition `type A B`), `CallExpr.Ellipsis` (`f(x...)`) and
  `GenDecl.Lparen` (a group, see below);
- **comments**, except directives (below), and parser bookkeeping (`Incomplete`, `Implicit`,
  `Obj`, `Scope`, and the derived `File.Imports`, `File.Unresolved`).

Normalizations, each identifying two spellings that the Go specification defines to mean the
same program:

1. **Parentheses** are skipped. The tree's shape already encodes precedence, so a redundant
   pair adds nothing, and a needed one is visible as the shape it produces.
2. **Literals** are compared by kind and exact value (`go/constant`): `0x10` and `16`, `1_000`
   and `1000`, `"a"` and `` `a` `` (carriage returns are dropped from raw strings, as the spec
   says), `'\x61'` and `'a'`. The kind stays significant: `'a'` is an untyped rune and `97` an
   untyped int, `1e3` a float: their default types differ.
3. **Imports** are compared per file as a multiset of (name, path), regardless of order and
   grouping. Within a file the import order has no meaning: the linker orders initialization by
   the dependency graph and then by import path (`cmd/link/internal/ld/inittask.go`).
4. **Declaration groups:** `var` and `type` groups are split into one declaration per spec, and
   empty groups (`var ()`) are dropped, at the top level and in function bodies. A group only
   shares the keyword. A `const` group of one spec counts as ungrouped; a `const` group of
   several specs is kept as it is, since `iota` and implicit repetition depend on it.
5. **Field groups** `(a, b T)` become one field per name `(a T, b T)` in parameters, results,
   struct fields (with the tag repeated) and method lists: the language gives each name the
   type `T` either way. **Except** in type parameter lists and anywhere inside a generic
   declaration (a function with type parameters or a generic receiver, a type with type
   parameters): there the two spellings are the same program for the language, but not for
   gc. Each written `[]T` is a type object of its own and the generic dictionary lists derived
   types by identity, so the expanded spelling gets a larger dictionary and different code
   (measured below, "also generic").
6. **Empty statements** are dropped from statement lists (not as the statement of a label):
   `a(); ; b()` is `a(); b()`. `for ;; {}` and `for {}` already parse the same.
7. **Elided composite literal types:** in a literal whose type is written as an array, slice or
   map type, an element (or key) `T{...}` whose type equals the element type is the elided
   `{...}`, and `&T{...}` with element type `*T` is too (the spec's rule for elision). Only
   types known from the syntax are used; a named slice type's element type is not looked up.
   Elision through a named pointer type or a type parameter is not valid Go: the tree level
   identifies `[]PS{PS{...}}` with `[]PS{{...}}` all the same, and the compile of the export
   and code levels rejects it.

**Directives** are compared separately, per file, as a multiset of (anchor, text): `//go:`
comments (`go:build`, `go:linkname`, `go:noinline`, `go:nosplit`, `go:embed`, `go:generate`,
...) and `// +build` lines. The anchor is what the directive precedes: `header` (before the
package clause), the next declaration or spec, named by kind and name and an ordinal
(`func (*T).M#0`, `var x#0`; independent of grouping), `imports` for any import spec (they are a
set), `inside` a declaration (a directive in a function body), or `eof`. A `//go:build` line is
compared as its parsed constraint (`a&&b` is `a && b`). `//line` directives are positions and
are not compared (see "Positions").

## Level 2: the export data

Both sides are compiled by the go command of the configuration, B **in A's place**: `go list
-export` with an `-overlay` that maps each of A's files to B's (to their canonical layouts, see
level 3). So both sides have the same import path, file names, flags, and dependencies: the
original's, from the build cache. The export data of the package archive's `__.PKGDEF` member is
then compared.

gc writes it in the Unified IR format (`internal/pkgbits`). With `-d=syncframes=0` (for every
package: an export file copies parts of its dependencies' export data as they are,
`noder/linker.go`, `relocCommon`) the format is self-describing: every value is preceded by a
sync marker giving its kind (`Bool`, `Int64`, `Uint64`, a reference) and every step of the
grammar by a marker naming it. gocmp decodes this generic structure, without the compiler's
grammar, and compares the two element graphs from the roots of the `Meta` section, following
references. Pairs under comparison count as equal when met again, so cyclic graphs compare as
the largest bisimulation; element numbering and sharing do not matter. An object's companions
(`Name`, `ObjExt` with its pragmas, linkname, inlining cost and body, and `ObjDict`, its
dictionary), which share its index instead of being referenced, are compared with it. The flag
changes only the encoding of the export data: the `-S` listings of `fmt`, `strings`, `runtime`,
`encoding/json`, `sync` and `net/http` with and without it are byte-identical.

Normalizations:

1. **Positions** (a `Pos` marker, its known flag, base, line and column) are left out.
2. **The imports of a package element** are compared in the order of their paths: gc lists
   them in the order the source first mentions them (`noder/writer.go`, `pkgIdx`).
3. **Constants** are compared by exact value: gc writes an `int64`, a big integer, a rational or
   a float depending on how `go/constant` happened to represent the value (`1000` against
   `1e3`, a small value computed through big ones).
4. **A constant of a basic floating-point or complex type** is compared rounded to that type:
   types2 records some constants in a typed context exactly and others rounded, depending on
   parentheses around them; the compiler rounds them where it uses them.
5. **Derived types** (in generic code, a type is an index into the object's dictionary,
   numbered in the order gc met the types) are compared by the type the index stands for.

## Level 3: the generated code

The same compiles as level 2 print their code with `-S`: for every symbol gc emits (functions,
closures, wrappers, their funcdata, read-only data, static data, type descriptors, DWARF), its
header (kind, size, flags, frame), instructions with offsets, machine code bytes and
relocations. The listing is split per symbol and compared symbol by symbol, by name; the first
difference is shown with context and both sides' positions. Normalizations:

1. The source position printed on every instruction is removed.
2. The `R_INITORDER` relocations of a package's `..inittask` are compared in sorted order: they
   list the imported packages in source order, which the linker does not use for ordering
   (as level 1, imports).

### The canonical layout

gc's generated code depends on source positions beyond the position tables and DWARF:

- the instruction scheduler orders otherwise unordered values by position ("favor in-order
  line stepping", `ssa/schedule.go`);
- an inline mark becomes a `NOP` (`XCHGL AX, AX` on amd64) when no instruction of the same line
  can carry it;
- frame slot merging orders same-named variables by their position's text
  (`liveness/mergelocals.go`, `nameLess`);
- physical lines matter as well as the lines that `//line` directives give: layouts that
  agreed on the latter but not on the former still gave closures inlined elsewhere other
  names.

So a printer that does not reproduce the original's layout changes the code of some functions
without changing the program: compiled from their own layouts, the `reprint` candidate below
differs in code from the original in 52 packages and 26 test programs (with DWARF left out),
all by instruction order, `NOP`s and stack slot order. To compare the code of the trees rather
than of the layouts, **both sides are compiled from a canonical layout of their tokens**
(`tools/gocmp/flatten.go`), the same for every two spellings that level 1 identifies:

- every top-level function and every spec starts with `//line FILE:1` (FILE the original file's
  path), so lines count from the start of their declaration;
- every line starts with 256 spaces, past which gc's position encoding saturates the column
  (`cmd/internal/src`, `colMax` 255): a position is a line;
- a new line starts after every `;` and `{`, except inside the braces of a struct or interface
  type, unless the next token is `;`, `)` or `}` or a top-level declaration keyword;
- directives stand on their own lines; automatic semicolons become explicit; other comments are
  dropped; raw strings become interpreted ones (their newlines would count).

Parentheses, trailing commas, literal spellings, elided literal types, import order and
grouping, the grouping of declarations, fields and parameters then change neither a position
nor a physical line (`tools/gocmp/tree_test.go` checks an example). Positions must not all be equal: with every token at one position, frame
slot merging was found to depend on map order (`internal/trace/internal/tracev1`), so the layout
keeps statements apart. Under the canonical layout DWARF is compared too (variable names, scopes,
inlining); with `gocmp -keep-positions` both sides compile from their own layouts and DWARF is
left out.

## Positions: what is compared

No level compares line numbers. A candidate printed without the original's positions compiles
to a program whose position tables (pclntab, DWARF line programs) name other lines, so whatever
observes them differs: `runtime.Caller` and `runtime.Callers` frames, stack traces of panics and
`runtime/debug.Stack`, `testing`'s file:line in messages, and programs that check their own
lines (go-lisp excluded 15 such run programs, its F12). The code level compares the program
apart from those tables. A printer that writes the original positions back (`//line`
directives from the forms' `:line` and `:column` metadata) would make them equal too; this
oracle does not require it.

## Proving the oracle

Candidates made without forms, on tamago/amd64 and tamago/arm64 (2026-10-08, 64 cores):

| candidate | tree | export | code |
|---|---|---|---|
| identity (`--make identity`, a copy) | 373 + 1,705 | 372 + 1,705 | 372 + 1,705 |
| reprint (`--make reprint`) | 373 + 1,705 | 372 + 1,705 | 372 + 1,705 |
| canon (`--make canon`) | 373 + 1,705 | 372 + 1,705 | 372 + 1,705 |

(packages + test programs passing, amd64; arm64 the same with 372 and 371 packages; the one
package fewer at export and code is `unsafe`, skipped. With the corpus of `gocmp tests`, the
same three candidates on amd64 pass 373 + 1,707, 372 + 1,707, 372 + 1,707, re-run
2026-10-08.)

- **reprint** is `gocmp reprint`: each file parsed, its comments dropped except the directives,
  every position reset, printed again by `go/printer`, the directives put back before what they
  preceded, then `go/format`ted (which also sorts imports). It is what a printer from a tree
  without positions or comments produces, which is what the forms printer will be. The files
  that do not parse (45 under `testdata/`, 80 error tests of `GOROOT/test`) are copied as they
  are and listed; none is in the corpus.
- **canon** is `gocmp reprint -canon`: reprint after respelling the trees in every way level 1
  normalizes: imports one per declaration in reverse order, `var` and `type` groups split, field
  groups expanded (outside generic declarations), parentheses around every operand of a binary
  expression (except of `&&` and `||`), integers in decimal, raw strings interpreted, elided
  composite literal types written out (where the element type is an array, slice, map, struct
  or pointer type). It passes all three levels: the normalizations do not change the program,
  for gc either.
- Two respellings that are the same program for the language but not for gc, measured with
  `GOCMP_CANON_ALSO`:
  - **logical:** parentheses around the operands of `&&` and `||`. gc's front end removes dead
    branches of `if false && x` by syntax (`noder/writer.go`, `staticBool`) and does not look
    through parentheses; the dead code it keeps changes inlining and register allocation.
    Level 1 passes all (parentheses are normalized); export differs in 1 package
    (`crypto/rsa`), code in 11 packages and 1 test program.
  - **generic:** field groups of generic declarations expanded. Level 1 reports it (31 packages,
    54 tests); export differs in 6 packages and 3 tests, code in 1 package and 10 tests (the
    dictionaries).

  The forms printer should therefore not add parentheses around `&&` and `||` operands, and must
  keep the field groups of generic declarations.
- **Mutations** (`--mutations`): for each corpus entry and operation, one site of its files,
  chosen from the seed, is changed, and the result compared with the original. Every mutant is
  caught by the tree level (swaps whose operands are equal trees are not generated: that
  mutant is the same program). Mutants that do not compile count as caught by export and code.

  | operation (amd64) | mutated | tree | export | code | do not compile |
  |---|---:|---:|---:|---:|---:|
  | swap `x op y` → `(y) op (x)`, seed 1 | 1,384 | 1,384 | 182 | 617 | 42 |
  | const `n` → `n+1`, seed 1 | 1,611 | 1,611 | 292 | 1,426 | 103 |
  | drop a statement, seed 1 | 1,763 | 1,763 | 562 | 1,671 | 387 |
  | swap, seed 2 | 1,384 | 1,384 | 172 | 563 | 28 |
  | const, seed 2 | 1,611 | 1,611 | 279 | 1,441 | 93 |
  | drop, seed 2 | 1,763 | 1,763 | 551 | 1,666 | 369 |

  On arm64 (seed 1): swap 1,384/182/627, const 1,611/293/1,425, drop 1,763/563/1,670. The
  rest (entries without a site) have no binary expression, integer literal or droppable
  statement. Export catches what changes the package's interface or an inlinable body; code
  misses what compiles the same: swapped operands of commutative operators, constants and
  statements in code that is not compiled (unused constants, uninstantiated generic code,
  branches on constants).

Timings (64 cores): a run over the whole corpus at three levels takes about 10 s with a warm
build cache, 56 s from an empty one (the cache grows by about 1.5 GB: the standard library
compiled with `-d=syncframes=0`, and each package's two canonical compiles). The first run for
a new configuration takes about a minute (it also finds the test programs that compile). `--make reprint` takes
2.4 s; `--mutations` 30 to 40 s for its three operations.

## Known gaps

- **Line numbers** are not compared (see "Positions").
- **gc is not the language.** Level 1 identifies spellings by the language's semantics; gc's
  output can still differ for two of them (parentheses around `&&`/`||` operands, generic field
  groups, measured above). The first is reported only by the export and code levels.
- **Elision** is normalized only where the element type is written in the literal's type, and
  identifies invalid spellings through named pointer types (caught by the compile).
- **Test files** (`_test.go`) of the packages are compared only with `--with-tests` (`gocmp
  -tests`): the trees of the package's `GoFiles`, `TestGoFiles` and `XTestGoFiles`, and the
  export data and code of every package `go list -test -export` compiles (the package, its
  test variant, the external test package, the generated test main), paired by import path,
  the code split by the go command's `# pkg` headers; test-only packages are included (379
  packages for tamago/amd64). Packages are compared per configuration: files of other
  GOOS/GOARCH or build tags are not. A test that embeds the package's own Go sources (TamaGo's
  `testdata_tamago_test.go`, `//go:embed *.go`) embeds the candidate's text, so its code
  differs whatever the candidate (9 std packages).
- **Export data** must be Unified IR version 4 with sync markers (go1.27); dictionaries are
  read only for their derived types. Other dictionary lists (runtime types, itabs,
  subdictionaries) are compared by index, which would report a renumbering as a difference.
- **The code level** compares gc's `-S` listing, not the object file: the object's own
  metadata (header, build ID, symbol indexes) and what the linker makes of it (dead code
  elimination, final addresses) are not compared, and DWARF only under the canonical layout.
- **gc's own nondeterminism** under equal positions (above) is avoided by the layout, not
  excluded in general: a difference that comes and goes between two runs of the same comparison
  would be gc's.
