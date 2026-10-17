# repr/haskell: notes as built

The build of repr/haskell's milestones ([SPEC.md](SPEC.md) §10.1), what each part does and where it
departs from or adds to the spec, with the reasons. R1 was built on 2026-10-17 (branch
`repr-haskell-r1`, from main at `82345a08`), on the host's GHC 9.14.1 (Alpine's `ghc-9.14.1-r2`,
its `ghc` library `ghc-9.14.1-f453`) and stage 0 of that commit.

## Decided (2026-10-17, the user)

HR1-HR5 as recommended: HR1 repr/haskell stays out of the executable (excluded by
`arbace/j2g/embed.ae`, evaluated from the checkout, as built); HR2 the spec amended for the
layout of the module's and the classes' bodies in the braces printer (S14, §7.4); HR3 the
stricter lines check (every located node) and the `:lines` metadata keys (`:hs/eq`, `:hs/brace`,
`:hs/paren`, `^:hs/braces`, `:hs/breaks` as `[index line]`) adopted into the spec proper; HR4
`bin/repr-haskell roundtrip` and `test` a step of `bin/gate --full` (`repr-haskell`, beside
repr/go's); HR5 the constructor-declaration forms for tuple and sum syntax and the `(:doc-type
...)` fallback adopted into the spec.

## R1: the helper, the converter, the braces printer, the tree level

### What was built

| part | file | what |
|---|---|---|
| helper | `tools/hsdump/HsDump.hs`, `tools/hsdump/GHC_VERSION` | GHC's parsed tree as Clojure-reader forms (`dump`), the tree level's normal form (`tree`) and lines (`lines`); SPEC §8 |
| shared rules | `arbace/repr/haskell/syntax.ae` | the name rule (§3.2), the lexical classes of names, the kinds of forms, the implicit parentheses (I5) |
| converter | `arbace/repr/haskell/convert.ae` | a dump to Haskell forms, mode `:lines` |
| writer | `arbace/repr/haskell/write.ae` | Haskell forms as `.ae` text at the Haskell lines, read back and compared |
| printer | `arbace/repr/haskell/print.ae` | Haskell forms to Haskell, the braces layout (§7.4, as built below) |
| command | `arbace/repr/haskell.ae`, `bin/repr-haskell`, `bin/lib/repr-haskell/test` | `helper`, `corpus fetch|list`, `dump`, `read`, `print`, `roundtrip`, `test` (§9.6) |
| corpus | `test/haskell/corpus-ghc-9.14.1.edn` | the four R1 packages, their configurations and modules (§9.4) |
| known differences | `test/haskell/roundtrip-ghc-9.14.1-known.edn` | none (`{}`) |

repr/haskell is not one of the tools the executable embeds: `arbace/j2g/embed.ae` excludes
`arbace/repr/haskell.ae` and `arbace/repr/haskell/`, as it excludes repr/java, so the program the
gate builds embeds none of it, and `bin/repr-haskell` runs its namespaces evaluated from the
checkout (`bin/lib/tools.bash`'s `arbace_tool`). The one line added to `embed.ae` changes the
embedded copy of that file, not what the program does.

### Results

`bin/repr-haskell roundtrip` (also `make repr-haskell-roundtrip`) over the corpus of HS4
(`ghc-9.14.1-src.tar.xz`, SHA-256 `2a83779c...1e77`, its `libraries/base`, `ghc-internal`,
`ghc-prim`, `ghc-bignum`):

| package | modules | with CPP | equal at the tree level (trees and lines) |
|---|---|---|---|
| `ghc-internal` | 244 | 74 | 244 |
| `base` | 237 | 12 | 237 |
| `ghc-prim`, `ghc-bignum` | 1, 1 (`Dummy`) | 0 | 2 |
| total | 483 | 86 | 483 |

All 483 convert (21,707 top-level forms), read back as written, print, parse with GHC, and give
GHC's tree of the original and its start lines: 3.5 million lines of `showAstData` text and
639,097 located nodes compared. No known differences. On this host (64 cores), about 40 s: the
dumps 2 s, the conversion 9 s, the printing 4 s, GHC's parses of both sides 19 s.

Not in R1 (SPEC §10.1, R6), listed in the corpus file: 26 `.hsc` modules (22 of `ghc-internal`,
4 of `base`), 24 boot files of `ghc-internal`, and the 2 modules without a source in the tarball
(`GHC.Internal.Prim`, `GHC.Internal.PrimopWrappers`, genprimopcode's output). The module lists are
those the host's GHC installed for each package (`ghc-pkg field PKG
exposed-modules,hidden-modules`), which are the `.cabal.in`'s for this OS, flags and bignum
backend (GMP), without the re-exports of `ghc-prim` and `ghc-bignum`.

`bin/repr-haskell test` checks the spec's own cases: the reader's 328 samples of Appendix A (E1)
read as recorded; the worked example `test/haskell/sample/Shape.hs` converts to exactly the forms
the spec wrote by hand in `Shape.ae` (as data, and every form at the same line; its `:config`
compared on the keys the spec writes), and its printed module parses to the same tree and lines.

### The helper (SPEC §8)

- Built by `bin/repr-haskell helper`: `ghc -O1 -package ghc -optP-DGHC_PIN="9.14.1"`, the pin read
  from `GHC_VERSION`, into `.tmp/hsdump/hsdump` (79 MB), rebuilt when the sources, the pin or the
  `ghc` unit change (`.tmp/hsdump/key`). It refuses to run when `cProjectVersion` is not the pin,
  or when the libdir it is given has no `ghc-VERSION-*` unit; the converter refuses a dump whose
  `:ghc` is not 9.14.1. `bin/repr-haskell` refuses a host GHC of another version.
- Verbs: `dump [--full] -o DIR`, `tree -o DIR` (FILE.tree and FILE.lines), `lines`, each taking
  `[--ghc FLAG... --files] FILE...`: the GHC flags (language, extensions, `-D`, `-I`,
  `-this-unit-id`, `-package`) then the modules, all in one GHC session. A dump is three forms:
  `(hsdump/config {...})`, `(hsdump/pragmas [...])` (the header pragmas with their lines, read by
  GHC's own header lexer, `initPragState`, as `getOptions` reads them; OPTIONS and OPTIONS_GHC told
  apart by the source's spelling), then the tree.
- The tree: `(Con field...)` positional; `(L [l1 c1 l2 c2] x)`; exact-print annotation types (`Ep*`,
  `Ann*` but `AnnDecl`, `AnnProvenance`, and the like) as `nil` without `--full`; names
  `(unqual ns "occ")`, `(qual ns "M" "occ")`, `(exact ns "occ")`; `(mod "M")`; strings with
  controls, DEL and surrogates as `\uXXXX`; `(src "text")` or `nil`; `(char n)`; `(:% n d)`;
  `(bytes "...")` for a `ByteString`; `(chunk "...")` for a doc chunk (decoded UTF-8); `(tuple ...)`
  for Haskell tuples; a `NonEmpty` as a vector.
- CPP (H9, §2.8): a module whose pragmas (or flags) turn CPP on is preprocessed through
  `GHC.Driver.Pipeline.preprocess`, as GHC's driver does, at verbosity 3 with a log hook that
  catches the command line. `:cpp` records the command (`cc -E -undef -traditional`), the arguments
  with the temporary paths replaced by their roles (`<macros>`, `<output>`) and `$libdir`, the
  defines and include directories, every file the output's line markers name with its SHA-256,
  the `MIN_VERSION_` header's SHA-256, and the output's SHA-256 with the temporary header's path
  replaced by `<macros>` (it is named in the line markers), so that two dumps of one module are
  identical. The pragmas are read from the preprocessed text (GHC reads them again after CPP).
- SHA-256 is computed by the helper itself (FIPS 180-4, 60 lines): GHC ships no package with it.
- `tree` (§9.2): `showAstData BlankSrcSpan BlankEpAnnotations` after `eraseEpLayout` (written
  anew, HS8) and after making every `SourceText` `NoSourceText`: `showAstData` blanks most of them,
  but its `numericLit` (for `HsIntPrim`, `HsWordPrim`) prints the source text, so `0b111111##` and
  `63##` would differ (N3).
- `lines`: the start line of *every* located node, in the tree's order, with its constructor (a
  span in another file prefixed by the file's name; none occur). This is stricter than §8.2's
  "every located node that begins a line": the two lists are compared position by position, which
  needs no definition of beginning a line and catches a node that moved within a line's reach.

### The converter and the writer (SPEC §2-§7)

As the spec says, with these choices where it leaves one, recorded as built:

- **Lines.** Every form that stands for a located node carries that node's start line, not only the
  forms that begin a Haskell line; the writer puts each form on its line of the `.ae` file, so the
  reader's `:line` of every list is its node's line (§7.1). A list's head is never moved to its
  line: an operator's head stands where Haskell writes the first operand, so a head symbol gets
  `^{:line N}` and a head literal or keyword an `:hs/breaks` entry instead. `:hs/breaks` entries
  are always `[index line]` (the spec allows a bare index for the next line). Empty lists `()` get
  `^{:line N}`, which the reader gives them no line of their own.
- **Token lines** (metadata in `:lines`, for the located nodes that begin with a token the forms
  have no element for): `:hs/eq` on an equation, an alternative or a lambda, the line of the `=` or
  `->` of an unguarded right-hand side when it is not the form's; `:hs/brace` on a record
  constructor (its `{`), a `case`, `\case`, `do` or a pattern synonym's `where` (the line its
  located list of items begins on, the `{` of explicit braces); `:hs/paren` on a form whose implicit
  parentheses (an `HsPar`, `ParPat`, `HsParTy` left implicit) begin on another line than it; and
  `^:hs/braces` (S12) on a `case` or `\case` without alternatives whose source wrote `{}`.
- **Operator chains.** `(op a b c)` only when every operator after the first stands on the last
  line of its left operand, where the printer puts it; otherwise `(:infix a op b op c)`, whose slots
  carry their lines. The symbols that are special in forms (`!`, `~`, `@`, `->`, `=>`, `|`, `=`, `<-`,
  `..`) are never chain heads: `arr ! i` is `(:infix arr ! i)`.
- **Prefix applications of an operator**: `((:name "op") a b)` (§3.2) for two or more arguments,
  and for one when the operator is `-` (`(- e)` is a negation) or not a chain head (`(!) x` is
  `((:name "!") x)`, since `(! x)` is a bang pattern); `(op e)` for one otherwise (§5.2). The same
  in left-hand sides and constructor patterns.
- **Built-in syntax in constructor declarations**: `data Tuple2 a b = (a, b)`, `data Unit = ()`,
  `data Sum2# a b = (# a | #) | (# | b #)` (in `GHC.Internal.Tuple`, `Types`) are `(:tuple a b)`,
  `()`, `(:usum a :_)`, `(:usum :_ b)` in constructor position; the spec has no entry for them.
- **An infix left-hand side whose operands are constructor chains** (`f :+ g <*> a :+ b = ...`,
  `Data.Complex`) is one `(:infix f :+ g <*> a :+ b)`, ghc-lisp's S14, which the spec does not
  restate.
- **Defaults GHC's tree does not tell apart from the source's word**: a `foreign import`'s `safe`
  is written when the source wrote it (its node is then located), and omitted otherwise; an
  activation equal to its pragma's default (`AlwaysActive`, `NeverActive` for `NOINLINE` and
  `OPAQUE`) is omitted.
- **Documentation on a type** is an element after (or before) the type in arrows, tuples,
  deriving lists, constructor fields and synonyms' right-hand sides (§4.12, §6's `HsDocTy`).
  Elsewhere the converter writes `(:doc-type T (:doc ...))` and counts it; the corpus has none.
- **Header pragmas** come first in the forms file (§2.2); the printer prints them and the module's
  documentation in their source order.

### The printer (SPEC §7.4)

The braces layout as built puts every block in explicit braces and semicolons **except two**,
which are in layout (virtual braces), as the original's are:

- **the module's body**: its items at column 1, each on a line of its own, the lines inside an
  item at column 2 (a `;` only between two items on one line);
- **a class's body**: its items at column 3, the lines inside them at column 5.

The reason is GHC's Haddock pass (`GHC.Parser.PostProcess.Haddock`, `addHaddockInterleaveItems`
and `mkDocDecl`): only under virtual braces does it restrict the documentation comments a block
takes by their column. Under explicit braces, a `-- |` comment after a class's `}` goes into the
class body (`tcdDocs`), and a top-level `-- ^` after a signature goes into the signature's type
(`HsDocTy`), where the original, in layout, has them as top-level `DocD`s. The module's and the
classes' bodies are the only blocks the pass interleaves (`hsmodDecls`, `tcdDocs`); every other
block (`where`, `let`, `do`, `of`, instance bodies, GADT bodies) keeps its braces, and §7.4's tree
level after `eraseEpLayout` cannot tell the two apart. Further:

- Lines: each form that carries a line goes to it (newlines, indented inside a layout block); a
  `{-# LINE N "file" #-}` pragma when a form's line was passed (none in the corpus once the header
  is printed in source order). A block's `{` goes to the line of its first item, or to its
  `:hs/brace`. A Haddock line comment ends its line.
- Prefix tokens (`!`, `~`, `@`, `-` of a negation, `$` of a splice, `'`) are printed on their
  operand's line, glued to it.
- A context of one constraint is printed without parentheses (GHC keeps them as an `HsParTy`); of
  none or several, as a tuple.
- A deriving clause's `via` type is printed as it stands (GHC keeps no `HsParTy` for its
  parentheses).
- `case x of` without alternatives prints no braces (an implicit block closes at once) unless the
  form has `^:hs/braces`; a block's items are separated by `;`, never ended by one, since `case x
  of ;` does not parse.
- A character outside ASCII that is not a letter or a digit is printed as a numeric escape in
  literals (GHC's lexer refuses some, U+10FFFF among them).

### Checks and how to reproduce

```sh
bin/repr-haskell corpus fetch        # the tarball (once, checked by its SHA-256), its four packages
bin/repr-haskell roundtrip           # dump, convert, print, the trees and lines of both sides
bin/repr-haskell test                # the reader's samples, the worked example and its round trip
bin/repr-haskell read --corpus       # the forms only: .tmp/repr-haskell/forms/ghc-9.14.1/PKG/hs/...
```

The round trip fails on a module that differs and that the known differences do not list, and on
a listed one that no longer differs; its report is `.tmp/repr-haskell/cand/ghc-9.14.1.report.edn`.
The converter reads every forms file it writes back with Arbace's reader and compares it with the
forms in memory (H3: every bare name, literal and form), and counts the places where a node the
printer would parenthesize has no parentheses in the source (warnings in its log; none in the
corpus).
