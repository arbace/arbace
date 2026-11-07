# repr/haskell's native reader

Haskell text read into GHC 9.14.1's parsed tree (`HsModule GhcPs`) by Arbace code alone, no GHC at
read time, written as the helper's dump of mode `:lines` (SPEC §8.2), so that the converter
(`arbace.repr.haskell.convert`) makes the same Haskell forms from either reader. The helper
(`tools/hsdump`, GHC's own parser through its API) stays the oracle: `bin/repr-haskell
native-check` compares the two readers. Built on 2026-11-04 (branch `haskell-native`, from main at
`46d04247`), from the corpus's pinned source tarball, `ghc-9.14.1-src.tar.xz` (SHA-256
`2a83779c...101e77`), read in place; the tarball ships the Alex and Happy sources as
`Lexer.x.source`, `Lexer/String.x.source`, `HaddockLex.x.source` and `Parser.y.source` (GHC's
repository's `Lexer.x` ... `Parser.y`) beside their generated `.hs`.

## The parts

| part | file | ports |
|---|---|---|
| lexer | `arbace/repr/haskell/lex.ae` | `compiler/GHC/Parser/Lexer.x` (and `Lexer/String.x`, `Lexer/Interface.hs`, `String.hs`, `CharClass.hs`) |
| header and options | `header.ae` | `GHC/Parser/Header.hs`'s `getOptions` (as the helper records the pragmas), the `-X`, `LANGUAGE`, `OPTIONS_GHC` flags with `impliedXFlags` and the languages' sets (`GHC/Driver/{DynFlags,Flags,Session}.hs`), `mkParserOpts`' bits |
| LALR(1) tables | `happy.ae` (generator), `tables.ae` (generated) | Happy's construction over `GHC/Parser.y` (no Happy code: Happy's own output, the tarball's `Parser.hs`, is the check) |
| LR driver | `parse.ae` | the template of Happy's generated parser (`happyDoAction`, `happyFail`, the error token) |
| actions | `rules_module.ae`, `rules_decl.ae`, `rules_expr.ae` | `GHC/Parser.y`'s actions, one `defrule` per alternative, keyed by the rule as written |
| post-processing | `post.ae` (expressions, patterns, commands, bindings), `post_decl.ae` (declarations, types) | `GHC/Parser/PostProcess.hs`, the builders of `GHC/Hs/{Utils,Type}.hs` |
| Haddock | `haddock.ae`, `haddocklex.ae` | `GHC/Parser/PostProcess/Haddock.hs`, `GHC/Parser/HaddockLex.x` |
| data | `ast.ae` | the dump's representation: spans with their buffer positions, `(L span x)`, nodes, names; the action registry |
| unlit | `unlit.ae` | `utils/unlit/unlit.c` |
| C preprocessor | `cpp.ae` | none: a clean-room implementation of GCC's traditional mode, judged against gcc 15.2.0's `cpp -traditional` as a black box (below) |
| driver | `driver.ae` | `GHC/SysTools/Cpp.hs`'s doCpp (its arguments and macros), `GHC/Unit/State.hs`'s units in scope and preload closure, `GHC/Driver/Pipeline`'s order (unlit, then CPP), the installation's `settings` and package database read as data |
| entry | `native.ae` | `read-text`, the preprocessing, the dump's forms, the check |

## The decision: a generated LALR parser, not a hand-written one

The task named two ways to port `Parser.y`: a hand-written parser following the grammar, or a port
of Happy's LALR construction generating tables from it. Built: the second, with the generated
parser's driver written after Happy's template and the actions ported by hand.

- **Layout is half in the grammar.** The Haskell report's parse-error(t) rule, which closes an
  implicit block when the next token cannot continue it (`let x = 1 in x`, `(case x of y -> y)`, a
  `where` after a `case`'s alternatives), is in GHC Happy's error token: `close : vccurly | error
  {% popContext }`. Where it fires is decided by the LR automaton: the state in which a token has no
  action, after the default reductions Happy chose for that state, and whether that state shifts
  `error`. A hand-written parser would re-derive that rule per construct and differ from GHC wherever
  the derivation is off; the generated automaton is GHC's by construction.
- **The grammar's conflicts are resolved in the grammar.** 50 `%shift` annotations resolve 264
  state/terminal conflicts (the `if`'s body grabbing an operator chain, `exp10 -> fexp`, the empty
  activations of pragmas, `(,,)`...), and one grammar parses expressions, patterns and arrow
  commands alike (the `ECP` productions). Generating from `Parser.y` keeps all of that as GHC has it.
- **It is checkable.** `bin/repr-haskell tables --check` proves the generated tables equal to
  Happy 1.20.1.1's in the tarball's `Parser.hs`, state for state with the same numbering (907
  productions, 1,537 states, every action and goto, the default actions included), and the committed
  `tables.ae` equal to a fresh generation. A port read the other way (decoding `Parser.hs`'s tables)
  would carry generated data with no generator; it serves as the check instead.
- **It moves with GHC.** A new GHC's grammar regenerates the tables; its new or changed alternatives
  show up as rules without an action (`parse/missing-actions`), every action being keyed by its
  rule as `Parser.y` writes it (`"exp10 : '-' fexp"`), parameterized rules (`exp_gen(IEXP)`,
  `alts(PATS)`) once for all their instances.

The cost: a generator of 1,600 lines and a 209 KB table file (base-36 rows, decoded in about 0.1 s).
The lexer is the other way round: hand-ported from `Lexer.x`'s rules with Alex's semantics (at each
position the longest match among the current start code's rules whose predicates hold, the earlier
rule on a tie, empty rules included, also at the end of the input), not generated by a port of
Alex's DFA construction: its regular expressions are small and its actions are code, and a
hand-written scanner dispatches on the first character where a 256-byte DFA would need Alex's
Unicode trick (`adjustChar`'s classes) reproduced in its tables.

## How it reads

1. The header pragmas (`getOptions` with the lexer's `option_prags` start code) give the module's
   flags over the configuration's (the corpus's language, extensions and `-haddock`); the extensions
   on (implied ones included) give the lexer's bitmap (`ExtBits`, `mkParserOpts`).
2. The lexer (one mutable state, `PState`'s fields) gives tokens `[kind value span]`, spans
   `[file l1 c1 l2 c2 b1 b2]` with GHC's buffer positions (code points; columns code points with
   tab stops of 8); comments are skipped (mode `:lines` needs none), documentation comments are kept
   for the Haddock pass.
3. The driver runs `parseModuleNoHaddock` (`parseSignatureNoHaddock` for an `.hsig`); each reduction
   calls its rule's action with the lexer state (for `getBit`, `popContext`, `pushModuleContext`).
4. An `ECP` (`forall b. DisambECP b => PV (LocatedA b)`) is a fn of the mode, `:expr`, `:pat` (a
   `PatBuilder`) or `:cmd`; `DisambECP`'s methods are fns of the mode; `checkPattern` turns a
   `PatBuilder` into a `Pat`. A `PV` computation runs where GHC runs it: a monadic action's at the
   reduction, a pure action's when its value is used.
5. With `-haddock`, the Haddock pass inserts the documentation comments (`addHaddockToModule`).
6. The tree is written with the dump's spans (`[l1 c1 l2 c2]`, the file first when not the
   module's) and the dump's forms (`(hsdump/config ...)`, `(hsdump/pragmas ...)`).

Exact-print annotations are not built (mode `:lines`' dump has them as `nil`); where GHC computes
a node's location from a token (`comb2` with a token, `stmtsLoc`'s leading semicolon, an `@`'s
`InvisPat`), the token's span is used.

## CPP, unlit, hsc2hs

Before 2026-11-04 (branch `haskell-pp`) left to GHC's driver, the helper's `cpp` verb writing the
text GHC parses (`FILE.hspp`, with its configuration `FILE.hspp.edn`); now native but for hsc2hs.
GHC's driver runs, before it parses (`GHC.Driver.Pipeline.preprocess`), unlit on a literate module,
then the Haskell C preprocessor when `CPP` is on (the configuration's flags, `-cpp`, or the header
pragmas of the module's text, unlit's for a literate one), and the native reader does the same:

- **unlit** (`unlit.ae`, a port of GHC's `utils/unlit/unlit.c` as `runUnlitPhase` runs it, `unlit -h
  FILE FILE OUT`): `#line 1 "FILE"` first, a bird track's line with a space for its `>`, a
  `\begin{code}` block's lines as they are, every other line blank; tabs outside the blocks expanded to
  stops of 8 (`egetc`), `#` lines kept, a `#!` line dropped; a program line next to a comment line, a
  missing `\end{code}` and a script without code are errors, as unlit's exit status makes them GHC's.
  Checked against the installation's `unlit` on the crop's two literate modules and five of the
  corpus's (the `Setup.lhs` scripts, `UseCases.lhs`, `Memo1.lhs`): byte for byte.
- **CPP** (`cpp.ae`): GHC's "Haskell CPP command" is `cc -E -undef -traditional` with `-x
  assembler-with-cpp`, GCC's traditional mode. `cpp.ae` is a clean-room implementation (2026-11-04,
  branch `haskell-cpp`, replacing the port of GCC's libcpp the branch `haskell-pp` made, GPL-3.0, not
  merged): written from the documented behaviour of traditional mode, no GCC code read, and judged
  against Alpine's gcc 15.2.0 `cpp -traditional` run as a black box, its output and line markers
  observed on probe inputs. The behaviour it implements, as observed:
  - the text is not tokenized: copied as it is, identifiers (`[A-Za-z_][A-Za-z0-9_]*`; no `$` in
    assembler mode, digits before a letter are not a number: `0x` with a macro `x` expands it) looked up
    as macros outside quotes; a quote (`'` or `"`) runs to its match or to the line's end (so in Haskell
    `foldl'` quotes the rest of its line: no macro after it expands, no `/*` after it opens a
    comment), a backslash outside quotes keeps the quote or backslash after it from counting; C block
    comments removed without a space in text and in a `#define`'s body (after its parameters are found:
    `a/**/b` substitutes both), as a space elsewhere in a directive; backslash-newlines (blanks between
    allowed) spliced before anything, a string gap's first backslash with them; CR LF and a lone CR are
    newlines;
  - directives only with the `#` in the line's first column (blanks and comments between it and the
    name); in assembler mode a `#` line whose name is no directive (`#!`, `#-}`, `# 12`) is text, its
    macros expanded; `#pragma`, `#ident`, `#warning` print nothing, but the pragmas libcpp runs
    itself (`once`, `push_macro`, `pop_macro`, `GCC poison`, `system_header`, `dependency`, `warning`,
    `error`) bring the output to the line after them, a `pop_macro` that restores a definition with one
    newline more, and a few others make gcc fail (`GCC diagnostic`, `GCC target`, `message`...); a
    skipped group still tracks comments and quotes, and its conditionals' structure is checked; a line
    is numbered by the line it begins on, a splice at its start included;
  - a function-like macro's `(` is looked for past blanks, comments and newlines, out of an expansion
    into the text after it; not found, the name and the blanks stay, newlines included (the output's
    line count then off, as gcc's), but at the end of the file; its arguments are taken as written,
    across lines (a newline a space, a quote running past the line's end, a `#` line inside them
    text), substituted unexpanded, in the body's quotes too (an argument's `"` backslashed in a `"`
    quote of the body), then the whole rescanned with the text after it (`f(1)(2)` with `f` giving a
    function-like name expands both; a quote an expansion opens runs on into the text); `F( )` of a
    macro without parameters is an error, `F()` of one with one parameter an empty argument;
  - a macro is disabled in the text of its own body, not in its arguments' text: each char of an
    expansion carries the set of the macros disabled where it was read (a function-like macro's at its
    `(`), and meeting a disabled macro is gcc's error "detected recursion" (traditional mode does not
    leave it unexpanded: `#define X X+1` then `X` fails), while `F(F(1))` and `#define F(a) a F` then
    `F(1)(3)` expand;
  - `#if` is expanded by the same scan (`defined X` and `defined(X)` kept, from a macro too;
    `__has_include` answered) and evaluated with C's conversions over 64 bits, a constant unsigned only
    with a `u` (`0xFFFFFFFFFFFFFFFF` is -1), `L'a'` but not `u'a'`, `true` 0, a division by zero an
    error only when evaluated; `__LINE__` is the line the scan has reached (the end of an invocation
    over lines), `__COUNTER__`, `__INCLUDE_LEVEL__`, `__BASE_FILE__`, `__FILE_NAME__` as gcc's;
  - the output: `# 0 "file"`, `<built-in>`, `<command-line>`, the `-D`/`-U` as that pseudo-file's lines
    1 to n, `stdc-predef.h` preincluded then the `-include` files at its line n, `# 1 "file"`; each
    logical line printed at its first line, its embedded newlines not counted; a gap before a line,
    from directives and skipped lines, of under 8 lines given as blank lines, else a marker `# N
    "file"` (` 3 4` in a system header); an `#include` brings the output to the line after it, then
    `# 1 "header" 1` (` 3 4` for one found in a system directory), and back `# N "parent" 2`; `#line`
    prints its marker at once;
  - the include search as gcc's: the including file's directory first for `"..."`, the `-I`
    directories (each kept once, a nonexistent or system one dropped) then the system ones; found files
    cached by name and directory, the search's first directory and the one the file was found in, so
    that a later search meeting either finds the same file: the multiple-include optimization (a file
    wholly inside `#ifndef X` or `#if !defined X`, `X` then defined, is not entered again, no marker)
    applies to it, while `MachDeps.h`'s `"ghcautoconf.h"` (found in its own directory, a directory
    object of its own) does not keep `<ghcautoconf.h>` out, its guard then skipping it.
  The host's answers it reads are recorded, never asked at read time (`cpp/host`: the system include
  directories, the macros gcc predefines under `-undef` in assembler mode, gcc 15.2.0's, Alpine's).
  Not covered (an error when met): `__DATE__`, `__TIME__`, `__TIMESTAMP__` (their value is the run's
  time), `#assert`, `#unassert`, `#embed`, `__has_feature` and the like (gcc too rejects variadic macros
  and `__has_embed` in traditional mode); `#pragma GCC system_header` and
  `GCC poison` only bring the output's lines up (their effect not kept); NUL bytes and gcc's diagnostics' wording are not reproduced (an error is
  an exception, a warning nothing).
- **GHC's arguments** (`driver.ae`, after doCpp): the `-D`, `-U` and `-optP` flags first (opt_P), the
  command line's `-I` directories, the include directories of the preload units, the command line's
  again (the home unit's), `-include` the rts's `ghcversion.h`, `-Dlinux_BUILD_OS
  -Dx86_64_BUILD_ARCH -Dlinux_HOST_OS -Dx86_64_HOST_ARCH` (the settings' target), `-D__GLASGOW_HASKELL_TH__`,
  `__GLASGOW_HASKELL_ASSERTS_IGNORED__` under `-O`, `__SSE__ __SSE2__` on x86, `-D__IO_MANAGER_MIO__=1`,
  and `-include` the header of `MIN_VERSION_pkg` macros of the units in scope (every exposed
  package's latest version, or under `-hide-all-packages` the `-package` ones). The installation is
  read as data from `--libdir` (`ghc --print-libdir`): its `settings` (the target, the CPP command, the
  base unit) and its package database (`package.conf.d/*.conf`: names, versions, ids, dependencies,
  include directories), so the dump's configuration has the helper's keys `:ghc-unit`, `:target`,
  `:os`, `:arch` for every module, and `:cpp` is the native reader's own record of its run.

  One choice is canonical where GHC's is not knowable: GHC writes the `MIN_VERSION` header's blocks,
  and closes the preload units from the `-package` flags, in the order of its unit map's keys, the
  uniques its process gave the units' names (a run of `ghc -E` on the same module gives the same
  order, nothing outside GHC's process can compute it). The native reader closes the units from the
  flags in their order and sorts the header's blocks; `:macros` (and `:files`' `<macros>`) is the
  SHA-256 of the header with its blocks sorted, the helper's too since its version 5. The order of
  the include directories GHC passes was the reader's in every module checked (it matters only when
  two directories hold a header of the same name). A literate module's CPP input is unlit's
  temporary file, `<unlit>` in the record (the helper's since version 5), and the MIN_VERSION
  header's `<macros>`, as its markers name them.
- **hsc2hs stays the helper's.** An `.hsc` module's text is what a C program prints: hsc2hs writes a C
  file from the `.hsc` (`#{size ...}`, `#{const ...}`, `#{peek ...}`), compiles and links it with the
  host's C compiler against the module's headers and runs it; the values are the target's ABI's,
  which only that compiler knows. The native reader reads the helper's hsc2hs output `X.hs`
  (written beside the `.hsc`, as a Cabal build's), preprocessed natively, with the helper's
  `:hsc2hs` record (its `cpp` verb's `X.hs.hspp.edn`).

## Faithful to the helper

The native dump reproduces the helper's dump, including two of its choices: an `OPTIONS_GHC`
pragma is recorded as `options`, as the helper's `pragName` reads the word three characters after
the token of the pragma's options (on the buffer's line that the token's line number names, as
`StringBuffer`'s `atLine` finds it, so in a preprocessed text the line its markers number); and, as
GHC's lexer does, a line marker inside a block comment leaves a `bol` start code pushed
(`popLinePrag1` does not pop in a comment, `parseNestedPragma` pops once), so the token after the
comment is checked against the layout.

The dump's configuration names what the reader knows: the GHC it reads for (`:ghc "9.14.1"`), the
GHC installation's keys (`:ghc-unit`, `:target`, `:os`, `:arch`, from its package database and
settings), the language, extensions, flags and unit, `:reader :native`, the preprocessing's record
(`:cpp`, `:unlit`) of its own run; `:hsc2hs` is the helper's (above). The check compares the forms
with the native configuration, not the helper's.

## Checks

```sh
bin/repr-haskell tables --check      # the generated tables against the tarball's Parser.hs
bin/repr-haskell native-check        # test/haskell/native-check.txt: the crop's samples and a sample
                                     # of the boot libraries, against the helper, as trees and as forms
bin/repr-haskell read --reader native -o DIR FILE... --ghc FLAG...   # read natively, then convert
```

`native-check` dumps each listed module with the helper (mode `:lines`, the corpus's flags and
hsc2hs arguments), preprocesses it natively from the helper's directory and compares the text with
the helper's `cpp` verb's, byte for byte (`pp` when it differs; the MIN_VERSION header's and unlit's
temporary names as `<macros>` and `<unlit>`), reads it natively with the dump's flags, compares the
trees and the header pragmas as data, then converts both dumps, the native one with the native
configuration, and compares the forms as text; report `.tmp/repr-haskell/native-check/report.edn`.
A module whose helper dump the converter rejects is counted apart (`converter`: the trees equal,
no forms).

Results on 2026-11-04 (stage 0 `/root/arbace/target/seed/amd64/arbace`):

- `tables --check`: 907 productions and 1,537 states equal to `Parser.hs`'s, 264 conflicts resolved by
  `%shift` as Happy resolves them; `tables.ae` equal to a fresh generation; 3.7 s.
- `native-check`: 54 of 54 modules equal (since: 60 of 60, with `test/haskell/rare/`'s four and
  `test/haskell/cpp/`'s two, the preprocessing native, below), trees, pragmas and forms: the crop's 17 samples
  (`test/haskell/`: the worked example, mode `:full`'s and the layout's samples, R5's and R6's cases
  with a CPP inclusion, a signature, boot files, two literate modules, an `.hsc`, alex's output with
  and without CPP, HR26's `{-# LINE #-}` module, the interface cases) and 37 boot-library modules of
  22 packages, 12 of the 54 through CPP, unlit or hsc2hs; 7.6 s natively (141 ms a module, at most
  1.2 s: `Data/Text/Lazy.hs`), 21 s with the helper's runs.
- Wider samples while building, not committed: 119 more modules without preprocessing (6 a package,
  seeded), 88 preprocessed ones (3 a package: CPP, `Setup.lhs`, `.hsc`), the 25 `.hs-boot` files, all
  equal; three modules of rare syntax (arrows, Template Haskell's quotes and splices, `TransformListComp`,
  linear and required type arguments, type abstractions, GADT records, data families, `RULES`,
  `SPECIALISE`, `ANN`, `COMPLETE`, foreign declarations, warnings naming identifiers) with and without
  `-haddock`: trees equal (the converter rejects all three, the helper's dumps as well: `HsProc`,
  `{-# ANN module #-}`'s nullary provenance, a quasi-quote in a type). The declarations' port was also
  run over the corpus's existing dumps (1,316 of its 1,345 modules: 879 without preprocessing, 347
  with CPP, 90 `.hsc`), trees equal; the Haddock pass over 178 corpus modules and 10 synthetic ones,
  from the helper's trees without `-haddock`, equal to its trees with.

Results with the native preprocessing (2026-11-04, branch `haskell-pp`, the helper at version 5):

- `native-check`: 60 of 60 modules equal, the preprocessed text, the trees, the pragmas and the
  forms (the native `:config` with its own `:cpp` record): 14 of them through CPP or unlit (the
  helper's 12, `test/haskell/cpp/Trad.hs`, GCC's traditional cases: quotes, primes, a comment,
  function-like macros over lines, an escaped newline, guarded headers included twice, `#!` and an
  indented `#` passed through; `Lit.lhs`, a literate module through CPP), 10.1 s natively.
- The first agent's 88 preprocessed modules (3 a package of the corpus: CPP, the `Setup.lhs`
  scripts, `.hsc`), as a native-check list: 88 of 88 equal, 36 s natively (one had shown the
  multiple-include optimization's keying, above).
- GCC's own output on a sample through `ghc -E -keep-tmp-files` (the command line and the
  MIN_VERSION header GHC wrote): byte for byte.

With the clean-room preprocessor (2026-11-04, branch `haskell-cpp`), the same checks: `native-check` 60
of 60 equal (`:full` 57 equal and the same 3 the converter rejects), the 88 preprocessed modules 88 of 88
(`:full` 84 and the same 4); and a differential probe set run through gcc's `cpp -traditional` with GHC's
flags shape (`-undef -traditional -I... -include ... -D... -U... -x assembler-with-cpp`, also with nine
`-D`s) and through `cpp.ae`, compared byte for byte: 1,072 small inputs (the behaviours above one by one,
`#if`'s arithmetic, includes and their markers, system headers, pragmas, and randomized mixes), 466
with an output equal to gcc's and 606 that gcc rejects rejected too.

## Mode `:full`

Since 2026-11-04 (branch `haskell-pp`) the native reader also writes the dump of mode `:full`
(`read --reader native --full`, `native-check --full`): what the converter's mode `:full`
(`arbace.repr.haskell.full`) reads of the helper's `dump --full`, made from the native lexer, so that
the forms keep the comments and the layout and print back byte for byte (the full layout, the text
level's `roundtrip --text` path). The helper's dump --full has, besides mode `:lines`' tree:

- the exact-print annotations, as discarded forms (`#_`): the converter reads none of them (its
  facts of the annotations are found in the tokens, `:hs/where`, `:hs/parens`), so the native dump
  has none, and its tree is mode `:lines`';
- `(hsdump/text ...)`, the text the positions refer to: the source, a literate module's own; a CPP
  module's module view (`module-view`: each line of the module's file, the output line the line
  markers attribute to it), with the HR26-HR28 `{-# LINE #-}` lines for a text with pragmas of its
  own (`module-view-pragmas`, `:cpp`'s `:view-markers`);
- `(hsdump/tokens ...)`, GHC's lexer's tokens of the module's file (lexTokenStream: the raw token
  stream, comments as tokens, `{-# LINE #-}` pragmas not applied; the native lexer's `tokens`), the
  layout's zero-width ones left out, a comment's marked `:c`; a literate module's text that unlit
  leaves out as comments (`literate-tokens`, the helper's literateTokens);
- for a module with `{-# LINE #-}` pragmas of its own (an `.hsc` module's hsc2hs output, alex's
  and happy's output), physOf's places (R6, HR25: `phys-of`): GHC's positions, which the pragmas put
  in the `.hsc` (or the `.x`) where an expanded construct makes more lines than it had, remapped to
  the places in the text of the tokens they name (the nearest pair of a start and an end), the
  tree's spans with them, the pragmas as comments.

`native-check --full` compares the text, the tokens, the tree, the forms of mode `:full` (the native
with its own configuration) and the native forms printed in the full layout with the text, byte for
byte. Results (2026-11-04): over `test/haskell/native-check.txt`, 57 of 60 equal and 3 that the
converter's mode `:full` rejects for the helper's dump as well, their texts, tokens and trees
equal (`Incl.hs` and `Trad.hs`, whose texts include Haskell from another file, `Lit.lhs`, a literate
module through CPP: R5's and R6's known limits of mode `:full`); over the first agent's 88
preprocessed modules, 84 equal and 4 the converter rejects for the helper as well (`filepath`'s and
`os-string`'s Windows and Posix modules, whose text is an `#include` of another file); the `.hsc`
module, alex's outputs with and without CPP and HR26's module included, equal.
`test/haskell/full/Space.hs` (tabs, a CRLF, trailing blanks, a form feed, Unicode syntax...) read
natively and printed: byte for byte.

### The converter's limits of mode `:full`, lifted (2026-11-04, branch `haskell-full2`)

The 3 modules of the list and the 4 of the 88 that the converter rejected in mode `:full`, for the
helper's dump as for the native one, had two causes, both in the pairing of the full layout's tokens
with the dump's (`arbace.repr.haskell.full`), neither in the tree:

- **Haskell from another file** (`Incl.hs`, `Trad.hs`, `filepath`'s and `os-string`'s Posix and Windows
  modules: a CPP `#include` of a file whose text is Haskell). The dump's text is the module view (SPEC
  §2.8: the other files' lines dropped) and its tokens are the module's file's, so the tokens of the
  included nodes (the converter marks the first form of each file with `:hs/file`, R5) are in no text. The
  full layout (`print.ae`) now follows the forms' files: a node of a file other than the module's
  (`:file`, or `:hs/module-file` for an `.hsc`'s output) prints no token (`foreign?`; `at!` takes the
  file of a form, a documentation form's included, a pragma's not: its file only names a `{-# LINE #-}`),
  so the printed module is the module view byte for byte, as §2.8 defines the text level's reference.
  The forms keep the included nodes (the layouts of mode `:lines` print them with `{-# LINE #-}`). What
  it does not do is HR18 (c): the other files' text and tokens in the forms, which the module view would
  then hold whole; the helper's dump has neither.
- **A macro expansion in a literate module** (`Lit.lhs`: unlit, then CPP). The dump's text is the
  literate source, its tokens the lexer's on unlit's and CPP's output, so `x = __GLASGOW_HASKELL__` has
  the token `914` at the columns of three of the source's characters. `full/expand-macros` finds, where
  the printer's token is not the source's at a position, the identifier there (the macro's name) and
  replaces it in the text by the token, when the dump's span has the token's width, so that the text is
  the source with its object-like macros expanded and the later tokens of the line at their columns: the
  text the forms print back (as a CPP module's, which is the preprocessed text, never the source with its
  macros). A function-like macro or an expansion of several tokens is still an error. `native-check`
  expects that text (`want`).

Results (same day, stage 0 as above): `native-check --full` 60 of 60 equal (`:lines` 60 of 60), the 88
preprocessed modules 88 of 88 in `:full` and in `:lines`, and `bin/repr-haskell test`'s ten steps ok
(the samples' round trips, mode `:full` byte for byte, unchanged).

## The converter's fixes (2026-11-04, branch `haskell-pp`)

The three rare-syntax modules the native reader was checked on showed bugs of the converter
(`arbace.repr.haskell.convert`) that the corpus never reached, the helper's dumps rejected as the
native ones. Fixed, with their forms in SPEC §5.5 and the printer's support:

- arrows: `HsProc` and every command (`HsCmdArrApp`, `HsCmdArrForm` prefix and infix,
  `HsCmdApp`, `HsCmdLam`, `HsCmdPar`, `HsCmdCase`, `HsCmdIf`, `HsCmdLet`, `HsCmdDo`): a command's
  form is an expression's (the dump's constructors tell them apart; `paren-of` takes `HsCmdPar` as
  `HsPar`); the arrow applications at the positions of `exp : infixexp '-<' exp`, under `Arrows`
  only (syntax's `exp-needs-parens?`; without it `proc` is a variable, `-<` an operator);
- `{-# ANN module #-}`: `ModuleAnnProvenance` is a bare constructor (`con`, not `first`);
- quasi-quotes: in a type (`HsSpliceTy`'s `HsQuasiQuote`), and their text in every place (the dump
  holds it as `(L span "text")`, read as `second` of the string before);
- met on the way: declaration quotes `[d| |]` (`(:quote-decls d*)`), `TransformListComp`'s
  `TransStmt` (`(then ...)`, the qualifiers it holds flattened before it), `OverloadedRecordDot`'s
  `FieldLabelString`, `rec` printed under `Arrows`, and the layout layout's `'[ 'A` (glued it read
  as the character literal `'['`).

The modules are `test/haskell/rare/` (the three and `S4.hs`, more commands and quasi-quotes), in
`native-check`'s list and in `bin/repr-haskell test`'s step 10 (braces and layout layouts' trees
and lines, mode `:full` byte for byte). The forms of the corpus's modules that use `proc` as a
variable (Cabal, process, unix) are unchanged.

## The native reads' inputs (2026-11-07, branch `haskell-90`)

Since the crop's independence work (doc/haskell/FRONTEND-SURVEY.md, "As built: the crop's independence") the native reader is
the default of `bin/repr-haskell read --corpus` and of the round trip (`roundtrip`, its tree, layout and text levels), both
sides of it: the originals and the printed modules, read again. The helper is the oracle's (`REPR_HASKELL_READER=helper`, the
oracle tier's `haskell-roundtrip-oracle`; `native-check` compares the two readers). No GHC runs at check time, but three things
the host's toolchains made are still read, and they come from one pinned pack, `haskell-native-inputs-ghc-9.14.1`
(`test/haskell/records/haskell-native-inputs-ghc-9.14.1.{sha256,tsv}`, the store of `test/oracle-records/oracle.bash`), recorded
by hand on a host with GHC (`bin/repr-haskell record-native-inputs`; `--verify` compares a fresh recording with it member for
member):

| part | what | made by | read by |
|---|---|---|---|
| `sysroot/` | the files the native preprocessing reads outside the corpus's tree, under their absolute paths: GHC's `settings` and package database (`package.conf.d/*.conf`), its units' include directories (`ghcversion.h`, `MachDeps.h`, `ghcautoconf.h`, `HsBaseConfig.h`, ... and the RTS's headers), the C library's `stdc-predef.h` (the preprocessor's preinclude) | GHC's build and installation (Alpine's ghc package), musl-dev | `arbace.repr.haskell.cpp/*sysroot*` (`--sysroot DIR`): an absolute path P is read as DIR/P while it is still named P, so the dumps, the line markers and the `:cpp` records are the same as on the host |
| `configured/` | the headers the packages' configure scripts make (Cabal's build-type Configure: `unix`, `directory`, `process`, `time`) | `sh ./configure`, which runs the C compiler | copied into the extracted tree, where Cabal's build puts them (`corpus_fetch` runs no configure when the pack is pinned; `REPR_HASKELL_CONFIGURE=1` does) |
| `hsc/PKG/` | the corpus's 90 `.hsc` modules through hsc2hs: `X.hs` and the helper's record `X.hs.hspp.edn` | hsc2hs, which compiles and runs a C program | `X.hs` copied beside `X.hsc`, the record read by `read --hspp` |
| `LIBDIR` | the installation's path the reads name | | `--libdir` |

The member list is what a live native read of the whole corpus in both modes read (every `:cpp` record's `:files` outside the
tree; 2026-11-07: 1,345 modules each mode, none failing, 528 of them preprocessed by CPP or hsc2hs), with the installation's data
and include directories. The pack is keyed by the source tarball's SHA-256, the helper's identity (its `cpp` verb ran hsc2hs)
and the corpus's plan: a change to any is STALE until it is recorded again.

What would make these 1 (no toolchain's product read): the configure products and `MachDeps.h`, `ghcautoconf.h`, `ghcplatform.h`
derived from the target's facts (they are a few dozen `#define`s of sizes and features), the installation's package database
from the corpus's own `.cabal` files, and hsc2hs's run on ci (the C program hsc2hs writes, run by Arbace's C interpreter, as
`bin/hsi`'s programs' own C already is): FRONTEND-SURVEY's proposal HFS-I2.

## Messages

A module GHC rejects gets, from the native reader, the text `ghc -fno-code` prints for it on standard
error (built on 2026-11-04, branch `haskell-errors` from `haskell-native` at `61d0f191`): the parser's
messages in GHC's format, `FILE:LINE:COL: error: [GHC-NNNNN]` (or `warning: [GHC-NNNNN] [-Wflag]`)
hung over the message, its hints (`Suggested fix:`), the caret lines and a blank line.

- **The reference.** The helper's verb `errors` (`hsdump errors -o DIR ... FILE...`) writes
  `DIR/FILE.errors`, empty for a module GHC accepts, and `DIR/FILE.errors.hspp`, the text parsed (so a
  CPP module's messages are checked on the same text). It emulates the driver's two parses: the
  header first (`GHC.Parser.Header.getImports`' `parseHeader`, a `%partial` parser), its errors alone
  when it fails or records errors (the downsweep's, no `Compiling` line); else the module's, a failed
  parse's warnings and then its errors (`handleWarningsThrowErrors`), a parse that succeeds with errors
  its errors alone (the Hsc monad's warnings are dropped with the exception). It prints through GHC's
  own `printMessages` and `defaultLogActionWithHandles` at the module's flags, so the text is GHC's:
  checked equal to `ghc -fno-code`'s standard error on hand cases. The `dump` verb now rejects the
  modules the driver rejects as well (a header error, a parse with errors); the dumps of the modules
  it accepts are unchanged (the helper's version stays 4).
- **The messages as data** (`arbace.repr.haskell.errors`): `{:con :span :args}`, the constructor of
  `GHC.Parser.Errors.Types` (`:PsErrParse`, `:PsErrLexer`, `:PsErrInPat`...) with its fields as the
  native reader has them. A fatal one (`addFatalError`) is thrown (`lex/fatal!`, the message in the
  exception's `:ps-message`); a non-fatal one (`addError`) is kept in the lexer state's errors, a
  warning in its warnings (the tab warning counted as `getPsMessages` adds it, at the first tab;
  `-Wunrecognised-pragmas` with its suggestions, `-Woperator-whitespace-ext-conflict`,
  `-Wstar-is-type`, `-Wdeprecated-pragmas`' SPECIALISE), filtered by the module's `-W` flags
  (`header/warning-on?`: `-w`, `-W`, `-Wall`, `-Weverything`, `-Wcompat`, `-W<flag>`,
  `-Wno-<flag>`; `standardWarnings` by default). `native/read-text` gives `{:messages [warnings
  errors]}` in the driver's order, the header's parse run when the module's fails.
- **Where they are made.** The lexer: `reportLexError` with the span to where Alex's automaton
  stops (`lex/dfa-stop`: within a string or a character literal), a `\0` as a UTF-8 decoding error
  at its position, unterminated comments (a documentation comment's from after its decoration),
  OPTIONS pragmas and quasi-quotes to the end, a numeric escape out of range at its digit, smart
  quotes (`PsErrUnicodeCharLooksLike`), the suffix `@`, `Missing block`, lambda-case, numeric
  underscores, primitive strings. The parser: `srcParseErr` from the last token (its text, or none for
  a virtual token: the message's "possibly incorrect indentation"), with its details for the hints
  (the last 100 and 8 characters as `StringBuffer.decodePrevNChars` takes them, which leaves out the
  character before the position), `popContext`'s, and the `%partial` header parser's accept on the
  error token. The post-processing and the actions: every `PsErr` constructor the port raises, with
  GHC's spans and fields (patterns with the parse context: an infix function binding's operator,
  `bindpat`'s incomplete do block), and the non-fatal checks of `PostProcess.hs` and `Parser.y`
  (`BlockArguments`, an empty lambda, as-, lazy and bang patterns in expressions and `BangPatterns`,
  or-patterns, multi-way if, `forall`, linear arrows and `looksLikeMult`, qualified do, explicit
  namespaces, `ImportQualifiedPost`, `data T where`, `TraditionalRecordSyntax`, `DoAndIfThenElse`,
  `@` in a pattern).
- **The text.** `arbace.repr.haskell.doc` ports `GHC.Utils.Ppr` (Hughes and Peyton Jones's
  printer: `beside`, `above`, `sep`, `fill`, `best`, `display`, 100 columns and 1.5 ribbons) with
  Outputable's combinators (Unicode quotes); its reduction and its rendering walk long `vcat`s in loops,
  as a 1,400-line message needs (the interpreter's depth of 10,000 calls). `arbace.repr.haskell.errors`
  holds the codes (`GHC.Types.Error.Codes`' 123 of the parser), each constructor's documents, its
  reason and hints (`GHC.Types.Hint.Ppr`'s, the implied extensions from `impliedXFlags`, the pragma
  and role suggestions by `fuzzyLookup`), the header, `formatBulleted`, the caret lines
  (`getCaretDiagnostic`: tabs expanded, `\0` shown as U+FFFD, a multi-line span's `...`, the line read
  from the file the span names) and the order (`sortMsgBag`: by span, stably over the bag, whose
  newest message is first). `arbace.repr.haskell.pprhs` is the parsed tree as GHC's Outputable
  instances print it (expressions, patterns, `PatBuilder`, types, literals, matches, statements,
  bindings, signatures), with `pprDeeper`'s depth of 5 and `pprDeeperList`'s `....`.

- **The driver's header phase** (`arbace.repr.haskell.fileargs`, 2026-11-05, branch `haskell-errors2`).
  GHC's driver reads a module's header before it parses it (`getFileArgs`: `getOptions`, then the
  pragmas' flags through `parseDynamicFilePragma`, then `checkProcessArgsResult`), and what it finds there
  stops it or comes before the parse's messages. The helper's `errors` verb runs the same three
  functions (`driverHeader`); the native reader ports them. `getOptions'` over the pragma-state lexer's
  tokens: a LANGUAGE pragma that does not parse (`PsErrParseLanguagePragma`, at the token), an OPTIONS_GHC one
  (`toArgs` with GHC's locations: `PsErrParseOptionsPragma`, over the pragma), an extension GHC does not
  support (`PsErrUnsupportedExt`, with `fuzzyMatch` over `supportedLanguagePragmas`, in the order of the
  generated `arbace.repr.haskell.flags`; GHC throws it when `processArgs` reaches the argument, so after
  any pragma that does not parse); the misplaced pragmas' warnings (`-Wmisplaced-pragmas`), lazily. `processArgs`
  over the table of `flagsDynamic` (`flags.ae`, 2,300 names and `OptKind`s, generated by `hsdump flags`;
  `findArg`'s shortest rest, `processOneArg`'s kinds): a malformed argument (`-fmax-errors=x`) or a missing
  one is a usage error, printed as ghc's driver does (`ghc-9.14.1: FILE:L:C1-C2: malformed integer argument in
  -fmax-errors=x`, then the usage line); a flag no dynamic flag takes (`-fbogus`, `-XFooBar`) is
  `PsErrUnknownOptionsPragma`, one message each; an unrecognised `-W` flag a warning
  (`DriverUnrecognisedFlag`, `-Wunrecognised-warning-flags`), `-#include` a deprecation
  (`DriverDeprecatedFlag`), both printed before the parse's messages. The flag effects the messages need
  are `header/dyn-flags`'s (the extensions, the warnings, the diagnostics' flags below); other
  dynamic flags are checked for their shape and ignored.
- **`-Werror` and the warnings' flags.** `-W`, `-Wno-`, `-Werror=`, `-Wwarn=`, `-Wno-error=` and their groups
  (`-Wall`, `-Wcompat`, `-Weverything`...), `-fwarn-` and `-fno-warn-`, `-Werror`, `-Wwarn`, in the
  command line and in OPTIONS_GHC: a warning that is fatal is an error printed `error: [GHC-94817] [-Wtabs,
  Werror=tabs]`. As in GHC (`partitionMessages` does not see -Werror warnings) a failed parse prints
  every warning first (the fatal ones as errors) and then its errors; a parse that succeeds with only fatal
  warnings is rejected (`errorsOrFatalWarningsFound`), all its warnings in one batch.
- **The diagnostics' flags.** `-ferror-spans` (`FILE:L:C1-C2:`, `FILE:(L,C)-(L,C):`), `-fdiagnostics-show-caret`
  and `-fno-diagnostics-show-caret` (GHC 9.14 spells them so; `-fno-show-caret` is an unrecognised
  flag), `-dppr-cols=N` (the width of the rendering), and `-fdiagnostics-as-json` (schema 1.1: one JSON line a
  message, `GHC.Types.Error`'s `ToJson`: span, severity, code, message and hints rendered one by one, the
  warning's flag; the helper points standard error at the file while it prints, as the JSON log action
  writes there whatever the hook). In a pragma these apply to the messages after the header, as in
  GHC (checked: `-fno-diagnostics-show-caret` in OPTIONS_GHC).
- **Constructors added.** `PsErrIllegalDataTypeContext` (`mkDataDefn`), `PsErrUnpackDataCon`,
  `PsErrInvalidInfixDataCon`, `PsErrIllegalPromotionQuoteDataCon` (`mkUnpackednessPV`, `mkHsOpTyPV`,
  `tyToDataConBuilder`), `PsErrIllegalGadtRecordMultiplicity` (`mkGadtDecl`), `PsErrIllegalUnboxedStringInPat`
  and `...FloatingLitInPat` (`checkUnboxedLitPat`), `PsErrDeclSpliceNotAtTopLevel` (`cvBindsAndSigs`),
  `PsErrUnallowedPragma` (`rejectPragmaPV` at an infix operand, a type signature and a section),
  `PsErrInvalidPun` (`requireLTPuns`, seven rules), `PsErrInvalidPackageName` (`maybe_pkg`),
  `PsErrInvalidRuleActivationMarker`, `PsErrOverloadedRecordUpdateNoQualifiedFields` (fatal, at the first
  qualified field), and `-Wstar-binder`; the printing of `{-# SCC #-}`, declaration splices, `Float#` and
  `Double#` literals and strictness marks in record types.

The check: `bin/repr-haskell native-check --errors [--count N] [--seed S] [LIST]` gives each module
of the list, the rejected samples of `test/haskell/errors/` (119 small modules, one or a few errors
each, their flags in `errors.txt`) and N mutants of each listed module that GHC parses as it is
(`arbace.repr.haskell.mutate`: one edit at a token or a line from a seeded `java.util.Random`: a token
deleted, doubled, swapped, replaced or preceded by one of a pool of tokens and fragments, a line's
indentation changed, a line broken or joined) to the helper's `errors` verb and to the native reader,
and compares the texts (`accepted`: both accept; `equal`: the same messages); report
`.tmp/repr-haskell/errors-check/report.edn`. `native-check` compares a listed module the helper
rejects by its messages the same way (`test/haskell/native-check.txt`'s two rejected samples).

Results on 2026-11-04 (stage 0 `/root/arbace/target/seed/amd64/arbace`): the samples and the list
alone (`--count 0`): 135 modules, 80 equal, 55 accepted; with mutants, every run all equal or
accepted: seed 1 and 30 mutants a module 1,254 modules (782 equal, 472 accepted), seed 2 and 40
1,654, seed 3 and 40 1,654, seed 4 and 40 1,735 (1,096 equal, 639 accepted), seed 5 and 60 2,657 (1,724 equal, 933
  accepted, 2.7 minutes); about 0.1 s a
module natively, the helper's runs most of the two minutes of a run of 1,700. `native-check`: 54 of 54
trees equal, the 2 rejected modules' messages equal.

Results on 2026-11-05 (branch `haskell-errors2`; the same stage 0): the samples are 121 now (39 more: the
header phase, `-Werror`, the diagnostics' flags, the rarer constructors; `errors.txt`), and every one of
the 121 prints, from the helper and from the native reader, what `ghc -fno-code FLAGS FILE` prints on standard
error, byte for byte (checked against ghc itself, not only against the helper). Mutants of the list with
the samples: seed 6 and 40 a module 1,854 modules (1,186 equal, 668 accepted), seed 13 the same
(1,219, 635), seed 14 (after the last fixes) 1,857 (1,166, 691), all equal or accepted; and `test/haskell/errors-check.txt` (two accepted modules with rich
headers, `test/haskell/errors-base/`, the mutator's pool with header pragmas and flags added): seeds 7 to
12 and 15 and 150 mutants each 420 modules a run (about 300 equal, 120 accepted), after three differences
found and fixed (a record field's `!` in a message; Alex's last smart quote inside an unterminated
string; the stop point after a prefix of an escape's name, `'\\Foo`).
`native-check` (the trees): 54 of 54 equal, the 2 rejected modules' messages equal.

- **The rarer messages (2026-11-04, branch `haskell-errors3`).**
  `PsErrSpliceOrQuoteTwice` (`import splice M quote`: `importDeclLevelStyle`; ExplicitLevelImports).
  `PsErrExpectedHyphen` is a constructor of `GHC.Parser.Errors.Types` that nothing in GHC 9.14.1 raises
  (grep over `compiler/`: its type, its Ppr and its code only), so there is nothing to port.
  The warnings: `-Wunicode-bidirectional-format-characters` (`hscParse'`'s `checkBidirectionFormatChars`,
  which logs before the parse, so only a parse that goes through has it, in the one batch of a rejected
  `-Werror` module; the helper's `errors` verb has the same check now, `bidiWarning`, GHC's being
  unexported), `-Wview-pattern-signatures` (`addSigPatP`: both parses, `add_parens_sig` and
  `add_parens_view`, as a three-document message), the two Haddock warnings of `-Winvalid-haddock` (the
  comments left over, `PsWarnHaddockInvalidPos`, and `selectDocString`'s extra ones,
  `PsWarnHaddockIgnoreMulti`, whose state a `peekHdkM` rolls back; the pass ends GHC's parse of a module,
  so its warnings are the parse's), `-Wpattern-namespace-specifier` (the export and import items).
  `PsWarnTransitionalLayout` needs the alternative layout rule, which the native lexer does not run
  (`AlternativeLayoutRule` is a bit, not a mode), so it stays unraised.
  The deprecated flags: `flags.ae` holds `deprecated-flags` now, generated by `hsdump flags`, which runs
  each flag of `flagsDynamic` through `parseDynamicFlagsCmdLine` with the arguments 1 and 2 and records
  the `DriverDeprecatedFlag` it warns with (a message that names the argument is a template, `%s`: 189
  flags, `-fglasgow-exts`, `-fcontext-stack=N`, the `-fwarn-`/`-Wno-` names of removed warnings, the
  `-X` of deprecated extensions); `processArgs` adds `deprecate`'s warning when the flag took its
  argument. The command line's flags are processed the same way (`fileargs/command-line`): ghc's main
  prints their warnings first, `on the commandline: warning: ...`, newest first, and stops there when one
  is fatal (`-Werror`); the helper passes its flags located `on the commandline` (as ghc's main does) and
  prints that batch before the module's. Response files: `@file` in the command line and in an OPTIONS_GHC
  pragma, `parseResponseFile` over `unescapeArgs` (quotes, backslashes, nested files), the arguments
  located at the file's name (`resp.txt: warning: ...`), looked for in the module's directory; a file that
  cannot be read is the error `Could not open response file` (ghc's command line panics in that case, its
  pragma path was not checked).
  The placeholders of `pprhs`: arrow commands (`ppr_cmd`: application, lambda, case, if, let, do, the
  four arrow applications, `(| |)` forms, with `pprCmd`'s depth), quasi-quotes and splices in expressions
  and types, the quotation brackets (`[| |]`, `[e| |]`, `[p| |]`, `[t| |]`, `[d| |]` with value
  declarations, `[|| ||]`), `.field` selections and projections, `{-# SCC #-}` before an expression
  (and its unquoted name), parallel and transform comprehensions, the spacing of arithmetic sequences
  (`[x .. ]`); the location of `PsErrOverloadedRecordUpdateNotEnabled` (the record expression's). Still
  placeholders: type, class and instance declarations inside `[d| |]` (`<TyClD>`: the Outputable
  instances of `GHC.Hs.Decls`). Their samples are in `test/haskell/errors/` (`Pr*`, `Pq*`, `Pt*`
  for the placeholders, the others named by their message).

Results on 2026-11-04 (branch `haskell-errors3`; the same stage 0): the samples are 166 now, each printed
from the helper and from the native reader as `ghc -fno-code FLAGS FILE` prints it (the new ones checked
against ghc itself while written), all equal. The seeds the earlier branch ran and nobody re-checked, on
`test/haskell/native-check.txt` with 40 mutants a module: seed 6 1,899 modules, 1,209 equal and 689
accepted, and one difference (`PsErrUnexpectedTypeInDecl` of a family printed a trailing blank: the
words after the head are `empty` there, not `text ""`), fixed and run again: 1,210 equal, 689 accepted;
seed 13 1,899 (1,264 equal, 635 accepted); seed 14 1,899 (1,208 equal, 691 accepted); and the rich-header
list `test/haskell/errors-check.txt`, seed 16 and 150 mutants: 468 modules (352 equal, 116 accepted).
`native-check` (the trees): 54 of 54 equal. The mutator's pool did not change (the seeds stay
reproducible): its headers do not reach the deprecated flags or the response files, the samples do.

## Speed (2026-11-04, branch `parsers-speed`)

Profiled on stage 0 (the evaluator over the checkout's namespaces; the method and the evaluator's costs, one call 100 to 150
ns, an allocation 100 ns and more, `aset` of a boxed value into a typed array 50 to 100 times dearer, in doc/ocaml/OCAML-SURVEY.md,
"Speed (OC173)"). Where the time went, a 40 KB module of 4,520 tokens (Cabal's `PackageDescription/Parsec.hs`) before: lexing
218 ms (a token and its blank 30 to 60 µs: `scan!` allocated its accumulator, tried every rule's predicates by calls of the
character functions and walked `m-run` with a function per code point), the driver and the actions 270 ms (an `ArrayList` of
states and one of values, a sub-list copied to a vector for every reduction), the Haddock pass 10 ms, `to-dump` 55 ms and
`pr-str` 97 ms.

| | before | after |
|---|---:|---:|
| `Parsec.hs` (40 KB): lexing alone; parse with lexing; Haddock; `to-dump`; text | 218; 487; 10; 55; 97 ms | 140; 265; 10; 60; 79 ms |
| `Data/Fixed.hs` (15 KB), the same | 81; 158; 14; 20; 33 ms | 59; 97; 14; 19; 28 ms |
| `read` of 15 modules (407 KB), process start included | 11 s | 7 s |
| `read` of 128 modules (1.4 MB), process start included | 27 s | 16 s |
| `native-check` (54 modules), the native reads | 7.6 s (recorded) | 6.0 s |

What changed (`parse.ae`, `lex.ae`, `ast.ae`, `native.ae`; the dumps byte for byte the same on 15 and on 124 modules, the
failures the same): the LR driver keeps the states and the values in two arrays of a `loop`, and calls an action with its
arguments read from the array (no vector, no sub-list), the token's terminal found in a `HashMap` (the `Long/valueOf`
that was reflective is typed); the lexer has a table of the character classes (the predicates' answers for ASCII, computed
from the predicates themselves; macros `small?*`, `idchar?*`, `run*`... inline them) and `scan-fast`, which answers in the
start codes `:s0` and `:bol` when the byte alone decides (blanks, a lowercase letter's variable, a newline, a special
character, an operator that begins no comment or bracket, the empty `do_bol` rule), leaving `scan!` the rest; `scan!`
reuses its accumulator, `set-input!` and the span are made without intermediate vectors, `combine` without three; `read`
writes the dump with `write-form`/`write-dump`, a printer for the dump's data (lists, vectors, strings, symbols, keywords,
integers; any other value by `pr-str`) that also applies `to-dump`'s changes while it writes, the text the same as
`pr-str` of the converted tree (tried on all code points below 400 in a string). The start of the process (the namespaces
from source, `tables.ae` decoded) is about 2 s, unchanged. What is left: the lexer is a third of a module's time, the actions
25 µs a token (the ECP closures and `comb`s), the process start; compiled with the program (c2j) at a re-seed the next step.

On batch 3 (2026-11-07) the speed commits were re-applied onto the errors and preprocessing lines (merge plan §3 item 32):
the fused writer writes mode `:lines`' dumps (`read` without `--mode full`); mode `:full`'s dumps, whose tree `full-data`
remaps, are printed by `form-text` of the converted tree.

## What is left

- Messages: `PsWarnTransitionalLayout` (the alternative layout rule is a lexer bit, not run);
  declarations inside `[d| |]` in a message (`<TyClD>`, `<InstD>`, `<DerivD>`, `<ForD>`, `<FixSig>`,
  `<InlineSig>`, `<DefD>`, `<PatSynBind>`, `<SpliceD>`, `<RoleAnnotD>`, `<RuleD>`, `<AnnD>`: the Outputable
  instances of `GHC.Hs.Decls`); the effects of dynamic flags other than the extensions, warnings,
  diagnostics and `-cpp`/`-haddock` (shape only); `-fdiagnostics-color=always` and
  `-fprint-error-index-links=always` (escape sequences); a response file ghc cannot open (its command line
  panics); Alex's stopping point beyond strings and characters. `PsErrExpectedHyphen` is not raised by GHC
  9.14.1 itself.
- hsc2hs natively: it runs a C program (above); an `.hsc` module still needs the helper.
- Speed: on stage 0's evaluator about 0.06 to 0.2 s a module after the tuning above (0.1 to 0.3 s before); not yet
  compiled (c2j).
