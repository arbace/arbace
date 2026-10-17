# repr/haskell: Haskell's bridge — survey

Status: survey, 2026-10-17, at the user's request ("spawn a survey about repr/haskell, you may
find resources in ../ghc-lisp/"). Nothing is adopted here; the decisions (§6, H1-H14) are the
user's. Measured on main at `8c3a289b` with the stage 0 of the main checkout
(`target/seed/amd64/arbace`, amd64), Alpine's `ghc` 9.14.1 (`ghc-9.14.1-r2`) and the ghc-lisp
tree at `db7adf93` (branch `ghc-lisp`, 2026-10-03), read only. The experiments' scratch is in
`.tmp/repr-haskell-survey/` (not tracked); the one program written for them is in Appendix A.

The family, as the user named it on 2026-10-17 ([NAMING-SURVEY.md](NAMING-SURVEY.md),
"Decided", "The rename as built"): `repr/java` and `repr/go` are **bridges** between a
language's source and its forms in Arbace, one tool each with the verbs `read` and `print`;
**translators** run between forms dialects (j2g: Java forms → Go forms; c2a: Clojure → Arbace).
`repr/haskell` would be the third bridge: Haskell source ↔ **Haskell forms**.

## Executive summary

- **What it would be.** `arbace.repr.haskell`, `bin/repr-haskell read|print`: Haskell modules
  as `.ae` files that Arbace's reader reads, and back, with a round trip that proves the bridge
  lossless (§1). It would serve holding and studying Haskell code as data, as `tamago/` and
  `tamago/cmd/` hold Go; a translator from Haskell forms is a separate, later question (§5).
- **ghc-lisp is the right prior art and the wrong surface.** The user's fork of GHC
  (`/root/ghc-lisp`, ghc 10.1 at `c35096f5` plus 6,351 lines) maps every `HsModule GhcPs`
  constructor to s-expressions and back, exactly for the 4,863 parseable files of GHC's own tree
  and 99.6% of its testsuite, and 1,069 testsuite programs run the same from `.hsl` (§2.1). Its
  per-form grammar (SPEC.md, D1-D16, S1-S19) is a finished checklist. But its reader is its own:
  **Arbace's reader reads 1 of ghc-lisp's 11 `.hsl` files** (the one that tests CPP's
  rejection), and fails on 10 at the first type signature: `::` is not a token for Clojure's
  reader. Of 123 lexemes and small forms from its SPEC, 29 fail alone, more fail in context
  (`3#`, `"x"#`, `#foo` start reader tags), and others read silently as something else (`'a'` as
  `(quote a')`, `"\1234"` as `"S4"`, `<@>` as two forms) (§2.3).
  The same was found of go-lisp in 2026-10 (G2C-SURVEY §1.2, §7: "Don't adopt go-lisp's surface
  syntax"); the remedy then was repr/go's rule, the language's spelling where the reader reads it
  and another spelling where it does not.
- **A GHC helper works today on the host's GHC (experiment E2).** A generic dumper of GHC's
  parsed tree over `Data` (86 lines of Haskell on the `ghc` package Alpine ships; Appendix A),
  `tools/godump`'s counterpart, dumped 659 of the 799 `.hs` files of `base` and `ghc-internal`
  in 5 s (16 jobs); 135 of the 140 failures are CPP files the experiment does not preprocess.
  2.45 MB of Haskell became 20.0 MB of forms, **all 659 read by Arbace's reader** in 12 s.
  ghc-lisp's own printer does not compile against 9.14.1 (four constructors missing and changed
  shapes, E3): GHC's syntax tree changes with every release, which favours a generic helper and
  the mapping in Arbace.
- **Options (§3).** Grammar: (a) ghc-lisp's shapes, respelled for Arbace's reader, top-level
  heads `hs/...` as Go forms' `go/...`; (b) Arbace's own grammar derived from GHC's tree in
  repr/go's style; (c) ghc-lisp verbatim (not readable: excluded). Reading: (a) ghc-lisp's
  `--hs2lisp` plus a respelling pass (needs a ghc-lisp build); (b) a helper on GHC's API with the
  converter in Arbace (repr/go's architecture); (c) a Haskell parser in Arbace (no GHC to read;
  GHC still the checks' judge).
- **Recommendation (§6).** A representation first, no translator: ghc-lisp's grammar as the
  basis, respelled by repr/go's rule (H2, H3), heads `hs/` at top level (H4); read through a
  generic helper on the host's GHC (`tools/hsdump`, H5) with the converter and the printer in
  Arbace (H6); modes `:lines` and `:full` (H7); the round trip's levels tree, interface and
  text first, Core and code later (H8); one configuration per conversion with CPP resolved by
  the helper (H9); corpus `base` and the boot libraries of the pinned GHC, then a Stackage subset
  (H10); GHC at conversion and check time only (H11); translators deferred (H12); after
  `repr/java print` and F2-F7, beginning with a spec (H13); ghc-lisp a reference, not a
  dependency (H14). Effort, by repr/go's and ghc-lisp's record: about 6-9 working days to the
  tree and text levels over `base` (§5.3).

## 1. What repr/haskell would be

### 1.1 The bridge

| | repr/java | repr/go | repr/haskell (proposed) |
|---|---|---|---|
| source | Java (javac's tree, attributed) | Go (go/ast + go/types) | Haskell (GHC's parsed tree, `HsModule GhcPs`) |
| forms | Java forms (`doc/classes/SPEC.md`) | Go forms (`doc/go/SPEC.md`) | Haskell forms (a `doc/haskell/SPEC.md` to write) |
| read | `bin/repr-java read` (javac in the frozen jar) | `bin/repr-go read` (helper `tools/godump`) | `bin/repr-haskell read` (helper `tools/hsdump` on GHC's API) |
| print | to come (AGENDA: next) | `bin/repr-go print`, modes `:lines`, `:full` | `bin/repr-haskell print` |
| judge | javac | gc, `tools/gocmp` (tree, export, code; text) | GHC (tree, interface, Core, code; text) |
| held as forms | `jdk/` (j2a's output, committed) | `tamago/`, `tamago/cmd/`, `box/` | nothing yet |

The Haskell forms would be `.ae` files read by `arbace.core/read` (Clojure 1.12 syntax, no data
readers: Go forms principle 5; CLAUDE.md, "Working conventions"). A Haskell module is one
namespace and one file, as a Go package is one namespace (Go forms §4.1).

### 1.2 What it would serve

| use | what it needs | when |
|---|---|---|
| U1 Holding Haskell code as forms (a vendored library, a corpus, GHC's own sources), printed back for GHC as `tamago/` is printed for Go's toolchain | read, print, the round trip | first |
| U2 Studying GHC's pipeline in Arbace's own data (parsed tree; later renamed, typed, Core) | the helper's dumps; forms of the parsed tree | with U1 |
| U3 The language's breadth: the Arbace language as Clojure plus idiomatic Java and Go, and Haskell as one more dialect (CLAUDE.md, "Direction") | a Haskell-forms vocabulary that reads naturally beside Clojure | with U1 |
| U4 A translator from Haskell forms to Go forms or to Clojure, running Haskell on Arbace's runtime or in the box | typed Core, a runtime for laziness and type classes (§5.2) | much later, if ever |

U1-U3 are a bridge's job and need no runtime. U4 is a translator (the family's other kind) and
starts from a different representation (§5.2).

### 1.3 What Haskell adds over Java and Go

- **Fixity is not syntax.** GHC's parser leaves operator chains unresolved (`OpApp` chains);
  the renamer re-associates them by fixity declarations, possibly imported. The forms of the
  parsed tree must keep chains as written (ghc-lisp D4: `(op a b c)` and `(:infix a + b * c)`).
- **Layout.** Indentation is syntax; the forms have none (ghc-lisp D8), so the printer makes
  layout (or explicit braces), and the text level needs GHC's exact-print annotations.
- **Extensions.** About 130 language extensions change the lexer and parser (`MagicHash`,
  `NegativeLiterals`, `LexicalNegation`, `OverloadedRecordDot`, `TemplateHaskell`, ...). The
  forms record the module's extensions; reading and printing depend on them.
- **CPP.** 557 of the 3,706 `.hs` files in ghc-lisp's `libraries/` declare `{-# LANGUAGE CPP
  #-}` (15%; `containers` 49 of 74, `text` 58 of 106): Go's build constraints' counterpart, but
  textual.
- **Positions reach run-time behaviour, with columns.** `HasCallStack` stacks, pattern-match
  failure messages (`Non-exhaustive patterns in function f at M.hs:12:1-20`), `assert`, HPC and
  profiling cost centres print line and column. In Go only lines reach gc's output (Go forms
  §10), so `:lines` was enough for the export and code levels; for Haskell `:lines` loses
  columns that some programs print (ghc-lisp's run corpus excluded 12 programs for that, §2.1).

## 2. ghc-lisp as prior art and as a basis

### 2.1 What it is

`/root/ghc-lisp`, branch `ghc-lisp`, commit `db7adf93` (2026-10-03), forked from GHC master at
`c35096f5` (2026-10-01, version 10.1). A second front end: `.hsl` files (Haskell as
Clojure/EDN-style s-expressions) parse into the same `HsModule GhcPs` as `GHC.Parser`, so the
renamer and everything after treat both alike (DESIGN.md §2-3). Code: `compiler/GHC/Parser/Lisp.hs`
(2,291 lines: forms → GhcPs), `Lisp/Reader.hs` (360: text → forms; every atom lexed by GHC's own
lexer with the file's extensions), `Lisp/Printer.hs` (1,321: GhcPs → forms), `Lisp/Parens.hs`
(256: the implicit parentheses of SPEC §9); hooks in five upstream files marked `ghc-lisp:`; 25
commits over two days (2026-10-02 to 03).

What it established (README.md, "How well it works"; DESIGN.md, "Status"):

- `ghc --lisp-check`: Haskell → forms → Haskell gives the same tree (compared as
  `utils/check-ppr`: `showAstData BlankSrcSpan BlankEpAnnotations` after `eraseEpLayout`, with
  normalizations N1-N4) for all 4,863 parseable files of `compiler/`, `libraries/`, `utils/`,
  `ghc/`, `hadrian/`, and 11,382 of 11,423 parseable `testsuite/tests` files (99.6%).
- `ghclisp/run-corpus.sh`: 1,069 single-module testsuite programs converted to `.hsl` run with
  the same output and exit code; 12 excluded (they print their own source locations or depend on
  timing).
- `--hs2lisp` keeps comments (`-- |` as `;;|`); with `-haddock`, 4,185 of 4,863 files round-trip
  their documentation too.
- `--lisp2hs` prints with GHC's `ppr`, not exact print (D13): no text level.
- Boot files: `.hsl-boot` (`db7adf93`), `(import :source M)`.
- CPP: not supported in `.hsl` (D11); converting a CPP file captures one configuration.
- Tooling: `ghc --make` with mixed `.hs`/`.hsl`, GHCi, `runghc`, a one-line Cabal patch, Vim.

### 2.2 Its grammar against Arbace's conventions

| | ghc-lisp | Go forms (repr/go) |
|---|---|---|
| reader | its own (EDN structure, Haskell lexemes, D2): `::`, `:`, `\`, `'x'`, `@T`, `#label`, Haskell escapes and numbers | Arbace's reader, unchanged (principle 5) |
| names | verbatim (D3) | verbatim (§4.4) |
| heads | Haskell keywords and reserved operators (`data`, `=`, `::`, `->`, `\`), EDN keywords for the rest (`:tuple`, `:infix`, D1) | `go/...` for declarations, Go's and Clojure's words inside Go context (`if`, `let`, `bit-xor`) |
| file | header forms (`(:language ...)`, `(module M [exports])`) then imports and declarations at top level (S5) | `ns` + `go/package` + `load`s; `in-ns` + `go/file` + declarations |
| order | Haskell's source order (I1) | Go's source order |
| parentheses | implicit where Haskell needs them, else `(:paren e)` (I5, S4) | dropped (N1) except `&&`/`||` operands (`^:go/paren`) |
| positions | none in the text: `.hsl` spans are its own lines and columns (D13) | metadata: `:line` by layout (`:lines`), `:go/pos` maps (`:full`) |
| literal spelling | kept verbatim (D2) | value only in `:lines`; spelling in `:full` (`:go/atext`) |
| types | none: purely syntactic, like go-lisp (I2) | go/types' decisions written out (principle 3) |
| comments | `;` dropped; Haddock as `;;|`, `;;^` | `;` comments; doc strings; every comment in `:full` |

Its shapes fit Arbace's conventions well: source order, verbatim names, vectors for brackets,
keywords for non-Haskell forms are what Go forms do. What does not fit is the lexical level, and
the lack of position metadata.

### 2.3 Does Arbace's reader read ghc-lisp's forms? (experiment E1)

No `.hsl` corpus exists beyond the testsuite's: no ghc-lisp build is on the host (no `_build/`),
so `--hs2lisp` could not be run. Read instead: (1) the 11 `.hsl`/`.hsl-boot` files of
`testsuite/tests/ghclisp/`, every form in turn; (2) 123 lexemes and small forms taken from
SPEC.md §1-§9 and README's cheat sheet, with `read-string` (`tokens.clj`), then the doubtful
ones as streams and with more operators (`stream.clj`, `ops.clj`, `cand.clj`).

**Files: 1 of 11 reads** (`lisp004.hsl`, whose purpose is to fail on `(:language CPP)`). The 10
others stop at their first signature with `Invalid token: ::`.

**Lexemes** (`.tmp/repr-haskell-survey/tokens.clj`, `stream.clj`, `cand.clj`, `ops.clj`):

| class | ghc-lisp | Arbace's reader |
|---|---|---|
| fails | `::` (every signature, field group, kind signature) | `Invalid token: ::` |
| fails | `:` (cons), `':`, constructor operators ending in `:` (`:+:`, `M.:+:`) | `Invalid token` |
| fails | `\ x e`, `\case`, `\cases` | `Unsupported character` |
| fails | `'\n'` | unsupported character |
| fails | `0o17`, `0b101`, `1_000_000`, `0x1.8p1` | `Invalid number` |
| fails | `"a\&b"`, `"\SOH"`, `"\^A"`, `"\x41"`, `"\o101"`, string gaps | `Unsupported escape character` |
| fails | `3#`, `"x"#`, `123#Int8`, `#foo` (labels), `#.` | reader tags (`No reader function for tag`) |
| fails | `(^ x 2)`, `^.`, `^^`, `.~`, `%~`, `&~`, `<~`, `=~`, `//` | metadata or unquote characters |
| reads as something else | `'a'` (char) | `(quote a')`: a promoted `a'`? ambiguous |
| reads as something else | `"\1234"` (Haskell: char 1234) | `"S4"` (octal escape `\123` then `4`) |
| reads as something else | `<@>`, `x@y`, `~~>` | `<` and `@>`; `x` and `@y`; `((unquote (unquote >)))` |
| reads as something else | `@Int`, `~p`, `[~ 2]` | `(arbace.core/deref Int)`, `(arbace.core/unquote p)` (unambiguous as data, but not the spelling) |
| reads as something else | `:|`, `:.`, `:=`, `:+` (constructor operators) | keywords (ghc-lisp's rule tells them from `:tuple` by the second character; usable) |
| reads, spelling lost | `0x1F`, `007`, `1e3`, `-5` | 31, 7, 1000.0, -5 (ghc-lisp keeps literal text, D2) |
| reads | names (`foldl'`, `x#`, `+#`, `?x`, `M.insert`, `M.!`, `Prelude..`, `M.:|`), most operators (`>>=`, `<$>`, `<|>`, `!!`, `.`, `..`, `$`, `|`, `->`, `=>`, `<-`, `=`), keywords (`:tuple`, `:_`, `:section-l`), vectors, `'Just`, `''T` (as `quote`), `#_`, `#!` lines, commas | as intended |

With `read-string`, 31 of the 123 fail, two of them only because a lone `#_ x` or `#!` line
leaves nothing to read; in a stream, the MagicHash and ExtendedLiterals suffixes and labels
fail too (`read-string` stopped before the `#`), and so do operators that begin with or contain
`~`, `^` or `@` mid-token (`.~`, `a^b`). Silent
misreads are the worse kind: a writer of Haskell forms must check every name it prints by
reading it back (as repr/go's printer is checked by its round trip).

How often the failing lexemes occur, approximately (grep over the 799 `.hs` files of `base` and
`ghc-internal` in ghc-lisp's tree): `::` with spaces in 481 files, lambdas or `\case` in 266,
cons or constructor operators in 454, `@` type applications in 323, character literals in 134,
`^` in 111, MagicHash literals in 88. So, practically, **no real module printed by `--hs2lisp`
would read**.

### 2.4 Its round trip against repr/go's levels

| repr/go level (Go forms §3.1) | ghc-lisp | Haskell counterpart |
|---|---|---|
| 1 Trees (go/ast after N1-N10) | `--lisp-check`: GhcPs equal, spans and exact-print annotations blanked (its N1-N4) | the same |
| 2 Export data | none | interface files (`.hi`; `ghc --show-iface`) |
| 3 Object code | none | Core (`-ddump-simpl`), STG, object code |
| 4 Behaviour | `run-corpus.sh`: 1,069 programs | the same method |
| text (`:full`) | none (`--lisp2hs` is GHC's `ppr`) | exact print (GHC's `utils/check-exact`, ghc-exactprint) |

ghc-lisp's tree level is the right first gate: a front end that yields the same `GhcPs` yields
the same program, since everything after the parser sees the same tree. It does not test
positions, which reach behaviour (§1.3), nor anything a printer in another language would get
wrong beyond what the tree shows. repr/go found that the tree level is not enough to compare
parentheses that change gc's output (Go forms §3.2, N1's exception); the Haskell counterpart is
small (GhcPs keeps `HsPar`, which ghc-lisp compares exactly through `(:paren ...)`).

### 2.5 Coverage

All of GHC's syntax (D14): Template Haskell (D16), arrows (§8), linear types, GADTs, type
families, pattern synonyms, `OverloadedRecordDot` (D15), unboxed tuples and sums, RULES and the
other pragmas (§6). Not covered: CPP (D11; the corpus is the post-CPP tree). Multiline strings are read
by GHC's lexer (SPEC §1). Haddock comments are kept, other comments only placed approximately.

### 2.6 As a basis (experiment E3)

- **Version.** ghc-lisp follows GHC master (10.1, 2026-10-01); the host has 9.14.1 (Alpine's
  package, [ALPINE.md](ALPINE.md): "GHC and cabal ... serve other work on this host"). Using
  ghc-lisp's code as Arbace's reader means either a build of ghc-lisp (about 30 minutes,
  CLAUDE.md there; 1.4 GB for a GHC install) or a port of its printer to the host's GHC.
- **The port.** Copies of `Printer.hs` and `Parens.hs` type-checked against 9.14.1's `ghc`
  package (`ghc -package ghc -fno-code`): `Parens.hs` fails on four constructors that 9.14 lacks
  (`HsQualLit`, `HsStar`, `QualLitPat`, `ModifiedPat`); with those cases removed, on
  `PrefixCon`'s arity (one argument in 9.14, two in 10.1); the experiment stopped there. A port
  is a version-pinning exercise, repeated at each GHC release, for a printer whose output Arbace
  would then respell anyway.
- **License.** GHC is BSD-3-Clause; vendoring from ghc-lisp would add GHC's license to
  `LICENSE.md` and the origin to `doc/ARBACE.md`/the journal (CLAUDE.md, "Records").

## 3. Design options

### 3.1 The grammar

**(a) ghc-lisp's shapes, respelled for Arbace's reader.** Keep its SPEC form by form (D1-D16,
S1-S19, the implicit parentheses of §9, the canonical forms of I6), and change only what the
reader cannot read, by repr/go's rule (Go forms principle 5 and §7.5: Go's spelling where the
reader reads it, other spellings where not). A respelling table (sketch; the spec would settle
it):

| ghc-lisp | Haskell forms (sketch) | why |
|---|---|---|
| `(:: f T)`, `(:: x y T)` (signatures, fields, kind and expression signatures) | `(:sig f T)` (or `:has-type`, `:of-type`) | `::` is no token |
| `(\ p e)`, `(\case ...)`, `(\cases ...)` | `(:lambda p e)`, `(:lambda-case ...)`, `(:lambda-cases ...)` | `\` is the char prefix |
| `(: x xs)`, `(:infix x : xs)` | `(:cons x xs)`, `(:infix x :cons xs)` | `:` is no token |
| constructor operators `:|`, `:+`, `:.`, `:=` | the same, read as keywords (a keyword whose name begins with a symbol character is a constructor operator: ghc-lisp D1's rule) | readable |
| names the reader does not read back as themselves (`:+:`, `M.:+:`, `^`, `^^`, `\\`, `<@>`, `.~`, `%~`, `//`, `#.`) | `(:name "text")` (ghc-lisp's S2 with a string) | general escape; the printer checks every bare name by reading it back |
| `'a'`, `'\n'` | `\a`, `\newline` (Clojure characters, as Go forms' runes) | `'a'` misreads |
| `'Just`, `''T`, `'[a b]` | as written, read as `(quote ...)` | unambiguous once characters are `\x` |
| `@T`, `@_` | as written, read as `(arbace.core/deref T)` (as Go forms' `@p`) | readable, recognized as data |
| `~p`, `(~ a b)` | `(:lazy p)`, `(:name "~")` for the equality | `~` is unquote |
| `#foo`, `#"x y"` | `(:label "foo")` | `#` dispatches |
| `3#`, `3##`, `"x"#`, `'x'#`, `123#Int8` | `(:prim 3)`, `(:prim-word 3)`, `(:prim "x")`, `(:prim \x)`, `(:prim 123 Int8)` | reader tags |
| numbers `0o17`, `0b101`, `1_000`, hex floats | `017`, `2r101`, `1000`, ratios (Go forms §8.1's rules; the spelling in `:full`) | invalid numbers |
| strings with Haskell escapes or gaps | a Clojure string of the same characters; the spelling in `:full` | invalid escapes |

Pros: the user's own design, proven lossless on 4,863 files; a mechanical bijection with `.hsl`
(a respelling pass both ways), so a built ghc-lisp could compile Haskell forms. Cons: a few
keyword heads (`:sig`, `:lambda`) are less Haskell-like than ghc-lisp's; the bijection holds
only while ghc-lisp's grammar and Arbace's respelling move together.

**(b) Arbace's own grammar, derived from GHC's tree, in repr/go's style.** Heads chosen anew
(`hs/data`, `(sig f T)`, `(fn [x y] e)`...), possibly closer to Clojure, with GHC's decisions the
renamer makes (fixity resolution, binding groups) optionally written out as repr/go writes
go/types' decisions. Pros: the most freedom, a more Clojure-like surface; room for semantic
annotations. Cons: a second grammar for the same tree beside ghc-lisp's, its spec to be written
from nothing (ghc-lisp's took a day with D1-D16 already decided); Haskell's names verbatim make
any unqualified Clojure-word head (`fn`, `let` used differently) a possible collision (ghc-lisp
principle 3, D9).

**(c) ghc-lisp verbatim.** Excluded by E1: Arbace's reader cannot read it, and changing the
reader would break principle 5 shared by every forms dialect.

### 3.2 Reading: who produces the forms

**(a) ghc-lisp's `--hs2lisp`, then a respelling pass in Arbace.** Needs a ghc-lisp build (or its
printer ported to the host's GHC, E3) and a respelling pass over `.hsl` text, which needs a
reader of ghc-lisp's lexemes in Arbace (the very thing the reader cannot do), so the pass would be
a small tokenizer of its own. Fast to a first result if a build exists; ties Arbace to the
fork's version and to its repository.

**(b) A helper on GHC's API, the converter in Arbace (repr/go's architecture).** `tools/hsdump`
emits GHC's parsed tree generically (every constructor, its fields, spans; experiment E2,
Appendix A); `arbace.repr.haskell.convert` makes every decision about forms, with ghc-lisp's
`Printer.hs` as its mapping checklist (as go-lisp's §4.1 table was repr/go's). The helper
decides nothing, so it survives GHC's releases with little change (it walks whatever
constructors exist; the converter is keyed by constructor names, and a release's changes show up
as unknown constructors). Measured (E2):

| | |
|---|---|
| helper | 86 lines of Haskell, `ghc -O1 -package ghc` (the hidden `ghc` package of Alpine's GHC), 28.9 MB static-linked executable, built in under a minute |
| input | ghc-lisp's `libraries/base` and `libraries/ghc-internal`: 799 `.hs` files, 2.45 MB |
| dumped | 659 files in 4.9 s wall (16 jobs; 28 s CPU), 20.0 MB (8.1 times the source) |
| not dumped | 140, of which 135 declare CPP (not preprocessed by the experiment), 5 others (test files that fail to parse in isolation) |
| read by Arbace | all 659, 0 unreadable, 12.1 s (one thread, `read-string`) |
| a small sample | 478 bytes of Haskell → 10.2 KB of forms (2,601 nodes); GHC's own `-ddump-parsed-ast`, 59.8 KB |

The dump keeps spans on located nodes (`(L (EpaSpan [l1 c1 l2 c2]) ...)`), names as `(rdr "x")`,
literal source text as `(src "...")`, and drops exact-print tokens; a real helper would keep
those too (for `:full`), and run CPP (H9).

**(c) A Haskell reader in Arbace.** A lexer with the extensions' lexemes, a layout algorithm and
a parser for GHC's syntax (GHC's `Parser.y` is several thousand lines of grammar; MicroHs's
parser is a smaller model, below). Pros: printing and reading need no GHC; the standalone
direction (a Haskell bridge inside the box). Cons: a second Haskell parser that must equal GHC's
on every file, judged by GHC anyway (the checks still need it); every extension by hand. Worth it
only if Haskell must be read where GHC cannot run.

### 3.3 The dependency on GHC

| | GHC needed for | fits the standalone direction? |
|---|---|---|
| (a) ghc-lisp's front end | reading (the fork, built on the host) | no: a 30-minute build of a fork, outside Arbace's records |
| (b) helper on GHC's API | reading and the checks; printing is Arbace's | as repr/go: the Go toolchain is needed to read Go (go/types) and to judge, the printer is Arbace's; as repr/java: javac in the frozen jar |
| (c) reader in Arbace | the checks only | yes, for reading and printing |

Arbace's recent moves away from foreign dependencies (2026-10-16: go-whim's tests in `box/` on
the box's own guests, cgo dropped with the race detector; AGENDA.md) concern what *builds and
runs* Arbace. A bridge's helper is not in that path: Arbace builds without it, as it builds
without javac (the frozen jar runs only the oracle's reference, repr/java and the generators).
GHC would be the same kind of dependency, at conversion and check time only. The host's GHC is
Alpine's package (`ghc-9.14.1-r2`, 1.4 GB under `/usr/lib/ghc-9.14.1`), on Alpine edge, so its
version can move under the helper; the forms would record the GHC version (as Go forms record
`:config`) and the helper would check it. MicroHs (Augustsson; Haskell 2010 plus many
extensions, compiles itself to combinators run by a small C runtime with minimal dependencies,
Apache-2.0, 0.16.0.0 of 2026-06-02) shows that a Haskell implementation small enough to be
carried along exists, if a reader without GHC is ever wanted.

### 3.4 Side by side

| | grammar (a) + read (b) | grammar (a) + read (a) | grammar (b) + read (b) | read (c) |
|---|---|---|---|---|
| spec work | respelling table over ghc-lisp's SPEC | the same | a new spec | as chosen |
| GHC version | the host's, generic helper | the fork's (10.1) or a port | the host's | none to read |
| fidelity | GHC's parser | GHC's parser | GHC's parser | a reimplementation |
| first result | days (§5.3) | fastest if a build exists | longer (spec) | weeks |
| bijection with `.hsl` | yes | yes | no | as grammar |
| repr/go's architecture | yes | no | yes | no |

## 4. The round trip

### 4.1 Levels

For a module converted under a configuration (extensions, CPP defines, GHC version), compile the
original and the printed module with the same GHC and flags. Equivalent when:

1. **Tree.** The two `HsModule GhcPs` are equal, compared as ghc-lisp and `utils/check-ppr`
   compare (`showAstData BlankSrcSpan BlankEpAnnotations` after `eraseEpLayout`), plus spans
   by the positions mode (H7). The helper does the comparison (`tools/hscmp`, `tools/gocmp`'s
   counterpart) or the converter compares two dumps.
2. **Renamed / typechecked** (optional). `-ddump-rn-ast`, `-ddump-tc-ast` compared: catches
   nothing the tree level misses for a pure printer, useful only for a writer other than the
   converter (a hand-written or translated module).
3. **Interface ("export").** `ghc --show-iface` of the two `.hi` files, normalized (source
   hashes, file names): exports, types, instances, fixities, unfoldings. GHC has aimed at
   deterministic interface files since 8.0 (the deterministic-builds work), which would make
   this level cheap; to be measured.
4. **Core and code.** `-ddump-simpl -dsuppress-uniques` equal; then object code symbol by
   symbol, as `gocmp`'s code level. Columns and lines reach Core through `HasCallStack` and
   pattern-match failure strings, so this level needs `:full` or a normalization of source
   locations.
5. **Behaviour.** ghc-lisp's `run-corpus.sh` method over the testsuite's `should_run` programs.

Beside them, the **text level**: the printed file byte-identical to its original in mode `:full`
(as repr/go since 2026-10-16). GHC keeps the needed facts in the parsed tree's exact-print
annotations (every keyword's and bracket's position, comments), and `utils/check-exact` proves
GHC's exact printer over its testsuite; the forms would carry them as `:hs/pos` metadata and the
file's comments as repr/go's `:go/comments`.

### 4.2 Corpora

| corpus | size | notes |
|---|---|---|
| `base` + `ghc-internal` | 799 `.hs` (ghc-lisp's tree, GHC 10.1 sources; 135 with CPP) | E2's; for the pinned 9.14.1, the sources of its own boot packages (Hackage tarballs, pinned by hash; Alpine's package ships no `.hs`) |
| GHC's boot libraries and compiler | 4,863 parseable files (ghc-lisp's tree level) | the compiler needs its build's CPP headers |
| GHC's testsuite | 11,423 parseable; 1,069 run programs | behaviour level |
| Hackage | a Stackage LTS subset (`text`, `containers`, `bytestring`, `aeson`...) | needs Cabal's macros (`MIN_VERSION_*`) for CPP |

## 5. Where it fits

### 5.1 In Arbace's plans

Today's order (AGENDA.md, Next): the rename (merged at `8c3a289b`, re-seeded at `c8678ccf`), then
`repr/java print` (Java forms → Java source, with a round trip and a text level like repr/go's
`:full`), then F2-F7 of the Futamura survey. repr/haskell is a third bridge with no consumer
yet; it would come after those, and after `repr/java print` it can reuse that work's shape (a
printer for a language with a tree-level round trip and a text level). It touches neither the
build nor the seed: a new namespace, helper and script, and its forms regenerated, not tracked
(as Go forms' converted output, decision 9 of G2C-SURVEY), until a module is held as source by
a recorded decision.

### 5.2 Translators

- **Haskell → Go forms (or Java forms).** Haskell's semantics live after the parser: type class
  dictionaries, laziness, the renamer's fixities and binding groups are explicit only in Core.
  A translator would start from typed Core (GHC's `CoreProgram`, or STG), not from the parsed
  tree, and need a runtime (thunks and their update, a lazy evaluator's stack, GHC's primops,
  the `base` library's foreign calls). Prior art: GHC's own JavaScript and WebAssembly back
  ends (from STG), Eta (GHC 7.10 to the JVM, abandoned), Frege (a Haskell-like language on the
  JVM), MicroHs (combinators). Go's collector would serve; the rest is a project of B1a's size.
- **Haskell → Arbace (Clojure) forms.** Clojure's `delay` and lazy seqs give laziness, but not
  Haskell's performance model or its type classes; the result would be a Haskell interpreter in
  Clojure's clothes.
- **Neither is needed for U1-U3**, and the user's note on the translators (j2g and c2a "to
  revisit later, much later") argues for leaving translators out of this step. If one is ever
  wanted, its representation would be Core as forms (a `repr/core` reading GHC's Core through the
  same helper), a separate survey.

### 5.3 Effort

By repr/go's record (G0 spec to G2 round trip closed: 2026-10-07 to 10-08 on a larger language
surface, with go-lisp's tables; the `:full` mode a day on 2026-10-16-17; `arbace/repr/go` 6,115
lines, `tools/godump` and `tools/gocmp` 5,604) and ghc-lisp's (printer, parser and the corpus round
trip in two days):

| step | days |
|---|---|
| H0 spec (`doc/haskell/SPEC.md`: ghc-lisp's SPEC with the respelling, namespaces, metadata, modes) | 1 |
| helper (`tools/hsdump`: dump with exact-print tokens and comments, CPP, the configuration) | 1 |
| converter (forms from the dump; `Printer.hs` as checklist) | 1.5-2 |
| printer (forms → Haskell, layout, implicit parentheses) and the tree level over `base` | 2-3 |
| `:full` and the text level | 1-2 |
| interface level; the boot libraries | 1 |
| in all, to tree + interface + text over `base` and the boot libraries | about 6-9 |

### 5.4 Risks

- **GHC's syntax tree changes every release** (E3). A generic helper limits it to the converter's
  table; the forms record the GHC version.
- **Alpine edge moves GHC.** A new GHC changes the tree and the corpus's results; pin by recording
  the version, and if needed a GHC binary distribution pinned by hash (as `seed/goroot.sha256`
  pins Go).
- **CPP** makes a module's forms one configuration's (as Go's build constraints); the
  configuration must be recorded and reproducible (Cabal's macros).
- **Silent misreads** (`<@>`, `"\1234"`): the printer and converter must check every bare name
  and literal by reading it back.
- **Layout printing** is the printer's hardest part (where to break, `where`, `do`, guards);
  explicit braces are an easy fallback that GHC accepts with the same tree after `eraseEpLayout`.
- **Columns reach behaviour** (§1.3): `:lines` printed modules differ in call-stack text.
- **No consumer yet**: a bridge without a use (U1) risks drift; tie it to a held corpus.

## 6. Recommendation and proposals

Recommended: a representation, not a translator; ghc-lisp's grammar respelled by repr/go's
rule; repr/go's architecture (a generic helper on the host's GHC, every decision in Arbace's
converter, Arbace's printer); the tree, interface and text levels first; scheduled after
`repr/java print` and F2-F7, beginning with its spec.

- **H1 Purpose.** (a) A bridge only: read, print, the round trip; Haskell code held and studied
  as forms (U1-U3); (b) a bridge aimed at a translator now (U4). *Recommended: (a).*
- **H2 The grammar's basis.** (a) ghc-lisp's SPEC (D1-D16, S1-S19, I1-I6, the implicit
  parentheses) respelled for Arbace's reader; (b) a new grammar derived from GHC's tree in
  repr/go's style; (c) ghc-lisp verbatim (unreadable, E1). *Recommended: (a).*
- **H3 The respelling.** (a) repr/go's rule: Haskell's spelling where Arbace's reader reads it
  back unchanged, the table of §3.1 otherwise (`:sig`, `:lambda`, `:cons`, `(:name "...")`,
  `\c` characters, `:lazy`, `:label`, `:prim`), every bare name checked by reading it back;
  (b) the same with other words (`:has-type`, `fn`...); (c) a reader extension for Haskell's
  lexemes. *Recommended: (a)*, the words to settle in the spec; (c) breaks principle 5.
- **H4 Namespaces and heads.** (a) One namespace per module (`hs.Data.Map.Strict`, file
  `hs/Data/Map/Strict.ae`), the header `(hs/module ...)`, top-level heads qualified (`hs/data`,
  `hs/=`, `hs/sig`, `hs/class`, `hs/instance`, `hs/inline`...), ghc-lisp's unqualified heads in
  Haskell context inside (as `go/func` and Go context); (b) ghc-lisp's unqualified top level in a
  file no one evaluates. *Recommended: (a)*: the forms stay safe to load as `.ae` (a top-level
  `(= (f x) ...)` would be Clojure's `=`).
- **H5 Reading.** (a) `tools/hsdump`, a generic dumper on GHC's API (E2), with
  `arbace.repr.haskell.convert` deciding every form; (b) ghc-lisp's `--hs2lisp` plus a respelling
  pass (a ghc-lisp build or a port, E3); (c) a Haskell reader in Arbace. *Recommended: (a)*;
  (c) only if Haskell must be read where GHC cannot run.
- **H6 Printing.** (a) A printer in Arbace with Haskell layout (in the style of GHC's `ppr`),
  explicit braces where layout is in doubt, and mode `:full` byte for byte; (b) print `.hsl`
  and let a ghc-lisp build's `--lisp2hs` print Haskell; (c) explicit braces only. *Recommended:
  (a)*, with (c) as its first milestone.
- **H7 Positions.** (a) Modes `:lines` (the forms text keeps Haskell's lines; default, readable)
  and `:full` (every span and exact-print token as `:hs/pos` metadata, comments and spellings on
  the file form), as repr/go; (b) `:full` only; (c) no positions (ghc-lisp's D13). *Recommended:
  (a)*, noting that `:lines` changes column-carrying messages (§1.3).
- **H8 The round trip's levels.** (a) Tree, interface and text first (`bin/repr-haskell check`),
  Core and code later, behaviour by ghc-lisp's run-corpus method; (b) tree only, as ghc-lisp.
  *Recommended: (a).*
- **H9 Configuration and CPP.** (a) One configuration per conversion, recorded in the forms
  (GHC version, extensions, CPP defines), CPP run by the helper through GHC's driver, as Go's
  build constraints are resolved by `godump`; (b) CPP files skipped (as ghc-lisp's corpus);
  (c) a form for conditional compilation. *Recommended: (a)*; (c) only for hand-held source.
- **H10 Corpus.** (a) `base` and the boot libraries of the pinned GHC (their release tarballs,
  pinned by hash), then GHC's testsuite programs for behaviour, then a Stackage subset; (b)
  ghc-lisp's tree (GHC 10.1 sources, which the host's 9.14.1 does not build). *Recommended:
  (a).*
- **H11 GHC as a dependency.** (a) The host's GHC (Alpine's `ghc`), its version recorded in the
  forms and checked by the helper, needed at conversion and check time only (as javac and Go's
  toolchain for the other bridges); a pinned GHC bindist if Alpine's moves; (b) a ghc-lisp
  build; (c) none (H5 (c)). *Recommended: (a).* Record GHC in doc/ALPINE.md's subset when the
  bridge is built.
- **H12 Translators.** (a) None now; a later survey if wanted, from Core (`repr/core`) to Go
  forms; (b) Haskell → Go forms now; (c) Haskell → Clojure forms now. *Recommended: (a).*
- **H13 Schedule.** (a) After `repr/java print` and F2-F7, beginning with `doc/haskell/SPEC.md`
  for the user's review (as Go forms' G0); (b) now, beside `repr/java print`; (c) not at all.
  *Recommended: (a).*
- **H14 ghc-lisp's role.** (a) A reference: its SPEC the grammar's basis, its `Printer.hs` the
  converter's checklist, its corpus method and run corpus the checks' model; no code vendored, no
  build; (b) a dependency (its front end reads Haskell, H5 (b)); (c) a back end (a respelling to
  `.hsl` compiled by a ghc-lisp build). *Recommended: (a)*, as go-lisp's role was decided
  (G2C-SURVEY §9, question 10).

## 7. Sources

- ghc-lisp, `/root/ghc-lisp` (github.com/arbace/ghc-lisp), branch `ghc-lisp`, commit
  `db7adf93a4d5076a29d1a1d45295bfc38e615923` (2026-10-03), forked at
  `c35096f55daffde5d17ca878a965345db410abf5` (GHC master, 2026-10-01, version 10.1):
  `CLAUDE.md`, `ghclisp/README.md`, `ghclisp/DESIGN.md`, `ghclisp/SPEC.md`,
  `ghclisp/run-corpus.sh`, `compiler/GHC/Parser/Lisp.hs`, `compiler/GHC/Parser/Lisp/{Reader,
  Printer,Parens}.hs`, `testsuite/tests/ghclisp/`, `libraries/base`, `libraries/ghc-internal`.
- go-lisp (github.com/arbace/go-lisp), as described in [G2C-SURVEY.md](G2C-SURVEY.md) §1.2, §7,
  §9 (question 10).
- Arbace: [go/SPEC.md](go/SPEC.md) (principles, §3, §7.5, §8.1, §10), [go/PRINTER-NOTES.md](go/PRINTER-NOTES.md),
  [go/ROUNDTRIP.md](go/ROUNDTRIP.md), [NAMING-SURVEY.md](NAMING-SURVEY.md), [AGENDA.md](AGENDA.md),
  [ALPINE.md](ALPINE.md).
- GHC 9.14.1, Alpine's `ghc-9.14.1-r2` (`/usr/lib/ghc-9.14.1`; the `ghc` library package,
  hidden by default); GHC's `utils/check-ppr` and `utils/check-exact` (in GHC's tree).
- MicroHs: https://github.com/augustss/MicroHs, https://hackage.haskell.org/package/MicroHs
  (0.16.0.0, 2026-06-02; doc/hs2024.pdf there).
- ghc-exactprint: https://github.com/alanz/ghc-exactprint; ghc-lib-parser:
  https://github.com/digital-asset/ghc-lib; Eta: https://github.com/typelead/eta; Frege:
  https://github.com/Frege/frege.

## Appendix A: the experiments

Scratch under `.tmp/repr-haskell-survey/` (gitignored). Stage 0 is the main checkout's
`target/seed/amd64/arbace`; scripts run as `arbace FILE.clj ARGS`.

- **E1** (§2.3): `hsl/` holds copies of `testsuite/tests/ghclisp/*.hsl*` and `Geo/Shape.hsl`;
  `readall.clj` reads every form of each file with `read` and reports the first failure;
  `tokens.clj`, `stream.clj`, `cand.clj`, `ops.clj` read lexemes with `read-string` and as
  streams (a `PushbackReader` loop).
- **E2** (§3.2): `hsdump/HsDump.hs` (below, SHA-256 `8bf16c34...67b4fb`) built with
  `ghc -O1 -package ghc HsDump.hs -o hsdump`; `corpus/src` holds copies of ghc-lisp's
  `libraries/base` and `libraries/ghc-internal`; `corpus/one.sh` runs `hsdump
  /usr/lib/ghc-9.14.1/lib FILE` per file (`xargs -P 16 -n 10`); `corpus/readdir.clj` reads every
  dump with `read-string`.
- **E3** (§2.6): `port/` holds copies of `Printer.hs` and `Parens.hs`, checked with
  `ghc -package ghc -fno-code -i. GHC/Parser/Lisp/Printer.hs`.

```haskell
{-# LANGUAGE ScopedTypeVariables, RankNTypes #-}
-- Read-only survey experiment: GHC's parsed AST (HsModule GhcPs) as Clojure-reader data,
-- generically through Data (as showAstData walks it). Positions as [l1 c1 l2 c2] on L nodes;
-- exact-print annotations dropped. Usage: hsdump LIBDIR FILE.hs
module Main (main) where
import GHC
import GHC.Driver.Session
import GHC.Driver.Config.Parser (initParserOpts, supportedLanguagePragmas)
import GHC.Utils.Logger (HasLogger(..))
import Numeric (showHex)
import GHC.Parser.Header (getOptions)
import GHC.Parser.Lexer (initParserState, unP, ParseResult(..))
import qualified GHC.Parser as P
import GHC.Data.StringBuffer (hGetStringBuffer)
import GHC.Data.FastString
import GHC.Types.SrcLoc
import GHC.Utils.Outputable (showSDocUnsafe, ppr)
import GHC.Types.Name.Reader (RdrName)
import GHC.Types.SourceText (SourceText(..))
import Data.Data
import System.Environment (getArgs)
import System.IO

esc :: String -> String
esc s = "\"" ++ concatMap e s ++ "\"" where
  e '"' = "\\\""; e '\\' = "\\\\"; e '\n' = "\\n"; e '\t' = "\\t"; e '\r' = "\\r"
  e c | fromEnum c < 32 = "\\u" ++ pad (showHex (fromEnum c) "") | otherwise = [c]
  pad x = replicate (4 - length x) '0' ++ x

sym :: String -> String
sym c = case c of
  "(,)" -> ":tuple"; "(,,)" -> ":tuple3"; "(,,,)" -> ":tuple4"; "[]" -> "[]"; ":" -> ":cons"; ":|" -> ":ne"
  _ -> c

gen :: forall d. Data d => d -> String
gen x
  | Just (s :: String) <- cast x = esc s
  | Just (f :: FastString) <- cast x = esc (unpackFS f)
  | Just (r :: RdrName) <- cast x = "(rdr " ++ esc (showSDocUnsafe (ppr r)) ++ ")"
  | Just (m :: ModuleName) <- cast x = "(mod " ++ esc (moduleNameString m) ++ ")"
  | Just (st :: SourceText) <- cast x = case st of SourceText t -> "(src " ++ esc (unpackFS t) ++ ")"; NoSourceText -> "nil"
  | Just (sp :: SrcSpan) <- cast x = span' sp
  | Just (b :: Bool) <- cast x = if b then "true" else "false"
  | Just (i :: Int) <- cast x = show i
  | Just (i :: Integer) <- cast x = show i
  | Just (c :: Char) <- cast x = "(char " ++ show (fromEnum c) ++ ")"
  | Just (_ :: EpAnnComments) <- cast x = "nil"
  | tn == "EpAnn" = gmapQi 0 gen x
  | isEpAnn = "nil"
  | otherwise = case dataTypeRep (dataTypeOf x) of
      AlgRep _ ->
        let c = showConstr (toConstr x)
            kids = gmapQ gen x
        in if c == "(:)" then "[" ++ unwords (listElems x) ++ "]"
           else if c == "[]" then "[]"
           else if null kids then sym' c else "(" ++ unwords (sym' c : kids) ++ ")"
      _ -> esc (show (toConstr x))
  where
    tn = tyConName (typeRepTyCon (typeOf x))
    isEpAnn = tn `elem` ["EpToken","EpUniToken","AnnListItem","AnnList","AnnParen","EpLayout","EpaComments","NoEpAnns","AnnsModule","EpLinearArrow","AnnSig","AnnPragma","EpTok","TokenLocation"]
    sym' c = if take 1 c == "(" || c == ":" || c == ":|" then sym c else c

listElems :: Data d => d -> [String]
listElems y
  | showConstr (toConstr y) == "(:)" = gmapQi 0 gen y : gmapQi 1 listElems y
  | otherwise = []

span' :: SrcSpan -> String
span' (RealSrcSpan r _) = "[" ++ unwords (map show [srcSpanStartLine r, srcSpanStartCol r, srcSpanEndLine r, srcSpanEndCol r]) ++ "]"
span' _ = "nil"

main :: IO ()
main = do
  [libdir, file] <- getArgs
  (dflags0, logger) <- runGhc (Just libdir) ((,) <$> getSessionDynFlags <*> getLogger)
  buf <- hGetStringBuffer file
  let popts = initParserOpts dflags0
      (_, opts) = getOptions popts (supportedLanguagePragmas dflags0) buf file
  (dflags, _, _) <- parseDynamicFilePragma logger dflags0 opts
  let st = initParserState (initParserOpts dflags) buf (mkRealSrcLoc (mkFastString file) 1 1)
  case unP P.parseModule st of
    POk _ m -> putStrLn (gen m)
    PFailed _ -> hPutStrLn stderr ("parse failed: " ++ file)
```

(The listing drops four unused imports of the measured file, `occNameString`, `intercalate`,
`liftIO` and their modules; it compiles and behaves the same.)
