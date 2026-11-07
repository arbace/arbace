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

## What is left

- Rejected files' messages: a parse error throws with the offending token's span (`parse error on
  input`), not GHC's `PsMessage` text, and PostProcess's errors are reported with their spans in
  short words; GHC's messages (and the lexer's) are a later step. The warnings (`-Winvalid-haddock`,
  `-Woperator-whitespace`...) are not kept, as the dump has none.
- hsc2hs natively: it runs a C program (above); an `.hsc` module still needs the helper.
- Speed: on stage 0's evaluator about 0.1 to 0.3 s a module; not yet compiled (c2j) or tuned.
