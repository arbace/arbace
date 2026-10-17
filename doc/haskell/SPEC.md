# Haskell forms: Haskell modules written in Clojure

Status: draft for the user's review, 2026-10-17: milestone H0 of repr/haskell
([REPR-HASKELL-SURVEY.md](../REPR-HASKELL-SURVEY.md) §5.3), written at the user's decision H13 ("the
spec now, in parallel with F2; the build after it"). It is bound by the survey's decisions H1-H14
(its section "Decided (2026-10-17, the user)"); the choices made while writing it are S1-S36
(§11.1), the questions for the user HS1-HS12 (§11.2), each with the recommendation this text
follows. Milestone R1 (§10.1) was built on 2026-10-17: the helper, the converter, the braces
printer and the tree level over `base`, `ghc-internal`, `ghc-prim` and `ghc-bignum`, all 483
modules equal; what it adds to this text and where it departs is §10.4 and
[HASKELL-NOTES.md](HASKELL-NOTES.md). Milestone R2, the interface level over the same corpus, was
built the same day: all 483 modules' interfaces equal at `-O1` (§10.5). Milestone R3, mode `:full`
and the text level, was built the same day too: all 483 modules printed back byte for byte, 480
of them with GHC's trees of mode `:full` equal (§10.6). Milestone R4, the layout printer, was built
the same day too: all 483 modules printed by Haskell's layout rule parse to the original's trees
and lines and compile to equal interfaces (§10.7). Milestone R5, the other boot libraries, was
built the same day too: 41 packages, 1,226 modules equal at the tree and interface levels in both
layouts, 1,214 printed back byte for byte (§10.8). The experiments that
validate the spec's choices (Appendix A) were run on the host's GHC 9.14.1 (Alpine's
`ghc-9.14.1-r2`) and the stage 0 of main at `f0d7fc2f` (`target/seed/amd64/arbace`, amd64);
their scripts and inputs are `test/haskell/`.

This spec defines the **Haskell forms**: how every construct of a Haskell module, as GHC's parser
sees it (`HsModule GhcPs`), is written as Clojure data in ordinary `.ae` files that Arbace's reader
reads, what the forms keep of the source (lines, or everything), and how the round trip Haskell →
forms → Haskell is judged. It is the third bridge of the `repr` family
([NAMING-SURVEY.md](../NAMING-SURVEY.md)), after repr/java ([classes/SPEC.md](../classes/SPEC.md))
and repr/go ([go/SPEC.md](../go/SPEC.md)), and follows repr/go's spec in structure and conventions.
Its grammar is ghc-lisp's (`/root/ghc-lisp`, `ghclisp/SPEC.md`, D1-D16 and S1-S19: H2), respelled
where Arbace's reader cannot read ghc-lisp's lexemes (H3).

Words used throughout:

- **the helper**: the Haskell program `tools/hsdump` on GHC's API (H5, §8). It parses one module
  under one configuration, preprocessing it as GHC's driver does (CPP), and emits GHC's parsed tree
  generically: every constructor with its fields, spans and exact-print annotations. It decides
  nothing about forms.
- **the converter**: `arbace.repr.haskell.convert`, in Arbace. It reads the helper's output and
  writes Haskell forms. Every decision about forms is made here; ghc-lisp's
  `compiler/GHC/Parser/Lisp/Printer.hs` is its mapping checklist (H14).
- **the printer**: `arbace.repr.haskell.print`, Haskell forms → Haskell source (H6). With the
  converter it makes the round trip.
- **a consumer**: anything that reads Haskell forms for their meaning: the printer, and later
  tools that study or transform Haskell as data (the survey's U1-U3).
- **Haskell forms**: the forms of this spec. **Haskell context**: the inside of a top-level form
  (§4.1), where the forms mean Haskell.
- **GHC**: the host's GHC, `/usr/bin/ghc` 9.14.1, and its `ghc` library (the hidden package
  `ghc-9.14.1`), at conversion and check time only (H11).
- **ghc-lisp**: the user's fork of GHC at `db7adf93` (branch `ghc-lisp`, GHC 10.1), a reference,
  never a dependency (H14). Its forms (`.hsl`) are cited as "ghc-lisp's".

Contents: 1 Scope and principles · 2 The module form · 3 Lexical syntax · 4 Declarations ·
5 Expressions and patterns · 6 Types and kinds · 7 Layout and positions · 8 The helper ·
9 The round trip and its corpus · 10 Coverage plan · 11 Decisions and open questions ·
12 Sources · Appendix A: the experiments · Appendix B: the worked example.

## 1. Scope and principles

### 1.1 The bridge

repr/haskell is a bridge (H1): `bin/repr-haskell read` turns Haskell modules into Haskell forms,
`bin/repr-haskell print` turns Haskell forms into Haskell modules, and the round trip proves the
pair lossless (§9). It serves holding Haskell code as forms, studying GHC's parsed tree in
Arbace's own data, and the Arbace language's breadth (the survey's U1-U3). No translator is part
of it (H12): nothing here gives Haskell a meaning in Arbace, and nothing runs Haskell. A
translator, if ever wanted, would start from typed Core (a `repr/core`, survey §5.2), a separate
survey.

The bridge's parts, as repr/go's:

| | repr/go | repr/haskell |
|---|---|---|
| source | Go (`go/ast` + `go/types`) | Haskell (GHC's parsed tree, `HsModule GhcPs`, after CPP) |
| helper | `tools/godump` (Go) | `tools/hsdump` (Haskell, on the `ghc` library) |
| converter, printer | `arbace.repr.go.convert`, `.print` | `arbace.repr.haskell.convert`, `.print` |
| forms | Go forms ([go/SPEC.md](../go/SPEC.md)) | Haskell forms (this spec) |
| judge | gc: trees, export data, code; the text | GHC: trees, interfaces (later Core, code, behaviour); the text |
| positions | `:lines`, `:full` | `:lines`, `:full` (H7) |
| command | `bin/repr-go` | `bin/repr-haskell` (§9.6, a sketch) |

### 1.2 Principles

1. **The forms are GHC's parsed tree.** One form per construct of `HsModule GhcPs`, by
   ghc-lisp's grammar (H2): its shapes, invariants I1-I6, implicit parentheses (§9 there) and
   choices S1-S19, with the respellings of §3 applied. Classification is by context, head,
   shape and the lexical class of names, never by scope or type (ghc-lisp I2). Nothing GHC's
   renamer or type checker decides is written: operator chains stay unresolved (§5.2), binding
   groups are not computed, names are not resolved. This is the opposite of Go forms' principle 3
   (go/types' decisions written out), on purpose: for Haskell every level of the round trip is
   judged by GHC from the same parsed tree, and the bridge needs no meaning (S1).
2. **Clojure keeps its meaning outside Haskell context; inside, the forms mean Haskell.** A
   module's forms are top-level forms with heads qualified by the alias `hs` (`hs/data`,
   `hs/=`, `hs/sig`: H4, §4.1), so loading the file as Clojure could never take `(= (f x) ...)`
   for Clojure's `=`. Inside a top-level form, ghc-lisp's heads are used unqualified (`case`,
   `do`, `=`, `->`, `|`, `where`) with Haskell's meaning, as Go context's `if` and `let`.
3. **Plain Clojure syntax.** Everything reads with Arbace's reader (`arbace.core/read`), unchanged,
   without data readers (Go forms principle 5; CLAUDE.md, "Working conventions"). A Haskell token
   is written as Haskell spells it when the reader reads that text back as the same token, and
   otherwise by the respelling of §3 (H3). Every bare name the converter or printer writes is
   checked by reading it back (§3.2).
4. **Haskell's order and spelling.** Every form lists its parts in Haskell's source order, only
   heads moving to the front (ghc-lisp I1). Names are verbatim (ghc-lisp D3). Where two
   spellings mean one tree, the canonical one is the first this spec lists (ghc-lisp I6).
5. **Lossless for GHC.** In mode `:lines` the forms keep everything of the parsed tree except what
   §9.2 normalizes: positions other than lines, exact-print annotations (token positions,
   comments, layout), and the spelling of literals and pragmas. In mode `:full` they keep all of
   it, and the printed module is the original text byte for byte (H7, §7).
6. **One configuration per conversion.** A module's forms are those of one GHC version, one
   platform, one set of build flags and extensions, and, for a module that uses CPP, one
   preprocessing, all recorded in the forms (H9, §2.7, §2.8).
7. **GHC is needed to read and to judge, not to hold or print.** The helper runs on the host's
   GHC, whose version the forms record and the helper checks (H11). Reading forms and printing
   Haskell need only Arbace.

### 1.3 What changes from ghc-lisp

ghc-lisp's `.hsl` files and Haskell forms differ only in these respects; a `.hsl` file and the
Haskell forms of the same tree map onto each other mechanically, for the constructors both GHC
versions share (S2):

| | ghc-lisp (`.hsl`) | Haskell forms |
|---|---|---|
| reader | its own (Haskell lexemes in EDN structure, D2) | Arbace's, unchanged (§3) |
| lexemes the reader cannot read | `::`, `:`, `\`, `'c'`, `~`, `#lit`, Haskell numerals and escapes | respelled (§3.4, §3.5) |
| top level | `(module M ...)`, imports and declarations, unqualified heads (S5) | `ns`, `hs/...` forms (§2, §4.1) |
| header pragmas | `(:language ...)` before `module` (D10) | `(hs/language ...)` (§2.3) |
| Haddock | `;;|`, `;;^` comments, placed by GHC's Haddock pass (D12) | data: `(hs/doc ...)`, `(:doc ...)` (§4.12) |
| positions | the `.hsl` file's own spans (D13) | metadata: lines (`:lines`) or every span and token (`:full`) (§7) |
| literal spelling | verbatim (D2) | the value; the spelling in `:full` (§3.5) |
| CPP | rejected (D11) | run by the helper, one configuration recorded (§2.8) |
| GHC | 10.1 (its fork) | the host's 9.14.1 |

GHC 9.14.1 lacks four of the constructors ghc-lisp's SPEC maps (`HsQualLit`, `HsStar` in
expressions, `QualLitPat`, `ModifiedPat`) and `IEWholeNamespace`; its forms (`:qual-lit`,
`(type ..)` exports) are kept in this spec for the GHC that has them, and the converter of 9.14.1
never writes them (§10.3).

### 1.4 A first example

From the worked example (Appendix B, `test/haskell/sample/Shape.hs`):

```haskell
area :: Shape -> Double
area (Circle r) = pi * r ^ 2
area Rect {w = x, h} = x * h

classify :: (Ord a, Num a) => a -> String
classify n
  | n < 0 = "negative"
  | n /= 0, n > 100 = big
  | otherwise = 's' : "mall\tor\1234zero"
  where
    big = "big"
```

As Haskell forms, in mode `:lines` (each form on its Haskell line):

```clojure
(hs/sig area (-> Shape Double))
(hs/= (area (Circle r)) (:infix pi * r (:name "^") 2))
(hs/= (area (:rec Rect (= w x) h)) (* x h))

(hs/sig classify (=> (Ord a) (Num a) (-> a String)))
(hs/= (classify n)
  (| (< n 0) "negative")
  (| (:op "/=" n 0) (> n 100) big)
  (| otherwise (:cons \s "mall\torӒzero"))
  (where
    (= big "big")))
```

- Top-level heads are `hs/...` (§4.1); inside, ghc-lisp's heads (`=`, `|`, `where`, `->`, `=>`).
- `::` is no token for the reader: signatures are `hs/sig` (`:sig` inside declarations, §3.4).
- `pi * r ^ 2` is a mixed operator chain, unresolved as GHC's parser leaves it (§5.2), written
  with `:infix`; `^` cannot be read, so it is `(:name "^")` (§3.2). `n /= 0` is `(:op "/=" n 0)`:
  `/=` cannot be read either.
- `'s'` is the character `\s`, `:` is `:cons`, and the string holds the characters the Haskell
  escapes denote (`\1234` is U+04D2) (§3.5).
- Comments are not data in `:lines` (§7.3); with `-haddock`, documentation comments are (§4.12).

## 2. The module form

### 2.1 Namespaces and files

A Haskell module is an Arbace namespace and one file (H4). Its name is `hs.` followed by the
module name; the file is the namespace's path, with Clojure's munging of the segments (`'` and
the other characters `munge` replaces), under the forms root:

| module | namespace | file |
|---|---|---|
| `Data.Map.Strict` | `hs.Data.Map.Strict` | `hs/Data/Map/Strict.ae` |
| `GHC.Internal.Base` | `hs.GHC.Internal.Base` | `hs/GHC/Internal/Base.ae` |
| `Foo'` (a legal module name) | `hs.Foo'` | `hs/Foo_SINGLEQUOTE_.ae` |
| a boot file `Data/Map/Strict.hs-boot` | `hs.Data.Map.Strict.boot` | `hs/Data/Map/Strict/boot.ae` |
| a Backpack signature `Str.hsig` | `hs.Str.sig` | `hs/Str/sig.ae` |
| a `Main` module or a module without header, from `T1234.hs` of a corpus | `hs.main.T1234` | `hs/main/T1234.ae` |

Module name segments begin with an upper-case letter, so a lower-case segment can never be part
of one: the bridge uses lower-case segments for its own distinctions (S3). A `Main` module (named
or implicit) is named by its file's path relative to the corpus root, without the extension,
`/` replaced by `.` and `.` and `-` inside a segment by `_` (Go forms' rule for programs, §4.1
there): `testsuite/tests/typecheck/should_run/T1234.hs` → `hs.main.testsuite.tests.typecheck.should_run.T1234`.
Two modules of one conversion that map to one namespace are a converter error.

Converted forms are regenerated, not tracked, under `.tmp/repr-haskell/forms/CORPUS`
(`CORPUS-full` in mode `:full`), as Go forms' (decision 9 of G2C-SURVEY), until a module is held
as source by a recorded decision (HS11).

### 2.2 The file

A forms file holds, in this order (Haskell's, I1):

1. `(ns hs.M (:require [arbace.haskell :as hs]))`: `arbace.haskell` is a nominal namespace that
   no consumer loads, as Go forms' `arbace.go`; it gives the alias `hs` its meaning.
2. The header pragmas (§2.3), each a top-level form.
3. The module's Haddock header, `(hs/doc ...)`, when there is one (§4.12).
4. `(hs/module ...)` (§2.4), always present.
5. One `(hs/import ...)` per import declaration (§2.6).
6. The declarations, in source order (§4), with the documentation declarations among them.

```clojure
(ns hs.Data.Shape (:require [arbace.haskell :as hs])) (hs/language LambdaCase BangPatterns TypeApplications MagicHash)
(hs/doc :next " Shapes: the spec's worked example (doc/haskell/SPEC.md, section 2).")
(hs/module Data.Shape
  [(Shape ..)
   area
   <+>
   classify]
  :file "Shape.hs" :positions :lines :config {:ghc "9.14.1" :os "linux" :arch "x86_64" :language GHC2021 :flags ["-haddock"]})
(hs/import qualified Data.Map.Strict as M)
```

In mode `:lines` the `ns` form shares the first form's line (§7.1).

### 2.3 Header pragmas

The file-header pragmas GHC reads before parsing (`GHC.Parser.Header.getOptions`) are forms before
`hs/module`, one per pragma, in source order, with ghc-lisp's shapes (D10) qualified:

| Haskell | form |
|---|---|
| `{-# LANGUAGE CPP, MagicHash #-}` | `(hs/language CPP MagicHash)` |
| `{-# OPTIONS_GHC -Wall -fno-warn-orphans #-}` | `(hs/options-ghc "-Wall" "-fno-warn-orphans")` |
| `{-# OPTIONS -fglasgow-exts #-}` | `(hs/options "-fglasgow-exts")` |
| `{-# OPTIONS_HADDOCK hide, prune #-}` | `(hs/options-haddock "hide, prune")` |

The options are the words GHC splits the pragma into (`getOptions`), each a string, except
`OPTIONS_HADDOCK`, whose text is one string, as GHC keeps it. `CPP` is an extension like any
other: a module that declares it was preprocessed by the helper, and its forms are those of the
preprocessed text (§2.8; unlike ghc-lisp's D11, S4). Pragmas GHC does not recognize are comments
(§7.3).

### 2.4 `hs/module`

```
(hs/module name warning? exports? option*)
```

- `name`: the module name as a symbol (`Data.Map.Strict`), or `nil` for a module without a
  header (`hsmodName` is `Nothing`; GHC treats it as `module Main (main) where`).
- `warning`: `(:deprecated "msg")` or `(:warning "category"? "msg")`, the module's
  `{-# DEPRECATED #-}`/`{-# WARNING #-}` (`hsmodDeprecMessage`), as §4.11's warning forms.
- `exports`: the export list as a vector of export items (§2.5); omitted when the module has none
  (`Nothing`); `[]` is `module M () where`.
- options:

| option | value |
|---|---|
| `:file` | the source path as GHC was given it, relative to the package's source directory (`"src/Data/Maybe.hs"`): it is the file name of every span, which reaches interfaces and run-time messages (§7) |
| `:kind` | `:boot` for an `.hs-boot` file, `:sig` for an `.hsig` file; omitted for a module |
| `:positions` | `:lines` (the default) or `:full` (§7) |
| `:config` | the configuration (§2.7) |
| `:cpp` | the preprocessing record, when the module was preprocessed (§2.8) |
| `:unlit` | `true` for a literate module (`.lhs`), whose code GHC's `unlit` extracted (§8.5) |
| `:hs/comments`, `:hs/space`, `:hs/tail`, `:hs/unicode` | in mode `:full` only: the text the tree does not hold (§7.2; as built, §10.6: metadata of the `hs/module` form, `:hs/unicode` folded into `:hs/text`) |

### 2.5 Export items

The export list's items (`IE`), ghc-lisp's §2 with the respellings of §3:

| Haskell | form |
|---|---|
| `f`, `(+)`, `T`, `(:+:)` (`IEVar`, `IEThingAbs`) | `f`, `+`, `T`, `(:name ":+:")`: the name, bare or respelled (§3.2); which constructor follows GHC's rule for the name's namespace |
| `T(..)` (`IEThingAll`) | `(T ..)` |
| `T(A, b)`, `T(A, .., b)` (`IEThingWith`) | `(T A b)`, `(T A .. b)` |
| `T()` | `(T :_)` (ghc-lisp S6) |
| `module M` | `(module M)` |
| `pattern P`, `type (+)`, `data X` (`IEWrappedName`) | `(pattern P)`, `(type +)`, `(data X)`; combined `(type (T ..))`, `(pattern (P ..))` |
| `-- * Section`, `-- ** Sub` (`IEGroup`) | `(:doc :group 1 " Section")`, `(:doc :group 2 " Sub")` |
| `-- | text` (`IEDoc`), `-- $name` (`IEDocNamed`) | `(:doc :next " text")`, `(:doc :named "name")` |
| `f -- ^ text` (`ExportDoc`) | `f (:doc :prev " text")` |
| `{-# DEPRECATED #-}` on an export (`ExportWarning`) | `(:deprecated "msg" f)`, `(:warning "msg" f)` |

### 2.6 Imports

```
(hs/import :source? safe? level? qualified? "pkg"? M level? qualified? (as N)? (hiding? [item*])?)
```

Haskell's words in Haskell's order (ghc-lisp S7): `(hs/import qualified Data.Map as M)` is
`QualifiedPre`, `(hs/import Data.Map qualified as M)` `QualifiedPost`; `{-# SOURCE #-}` is
`:source`; `"pkg"` a package import (`PackageImports`); `level` is `splice` or `quote`
(`ExplicitLevelImports`, GHC 9.14). `as N` is two elements, not a list. The import list is a
vector of §2.5's items, `hiding [item*]` the `EverythingBut` list.

### 2.7 The configuration record

`:config` is a map, the same for every module of one conversion except where noted:

| key | example | meaning |
|---|---|---|
| `:ghc` | `"9.14.1"` | `ghc --numeric-version` of the GHC the helper was built with and ran on; the helper refuses to run on another (H11, §8.4) |
| `:ghc-unit` | `"ghc-9.14.1-f453"` | the unit id of the `ghc` library the helper links: it changes when the same version is rebuilt (an Alpine `-rN` revision) |
| `:target` | `"x86_64-alpine-linux"` | GHC's target platform (`ghc --info`, "Target platform") |
| `:os`, `:arch` | `"linux"`, `"x86_64"` | GHC's names for the host OS and architecture, which select CPP's `linux_HOST_OS`, `x86_64_HOST_ARCH` and the like |
| `:language` | `GHC2021` | the default language edition of the build (GHC 9.14.1's own default is `GHC2021`, Appendix A, E3) |
| `:extensions` | `[NoImplicitPrelude]` | the extensions the build gives (a `.cabal` file's `default-extensions`, `-X` flags), before the module's own pragmas |
| `:flags` | `["-haddock"]` | the other flags that change what GHC parses or what the levels compare: `-haddock` (on by default, §4.12), `-cpp`, `-F` preprocessors; `-D` and `-I` go to `:cpp` |
| `:unit` | `"base-4.22.0.0"` | the unit the module is compiled into (`-this-unit-id`) for the interface level, when the corpus gives one (§9.4) |
| `:corpus` | `"ghc-9.14.1-src"` | the corpus the module comes from, with its pin (§9.4) |

Keys a conversion does not use are omitted. The printer ignores `:config`; the round trip's
levels use it to compile the printed module as the original (§9.3).

### 2.8 CPP's record

A module whose extensions include `CPP` (or a build with `-cpp`) is preprocessed by the helper
through GHC's driver, exactly as GHC preprocesses it before parsing (§8.5), and the forms are those
of the preprocessed text: one configuration (H9). `:cpp` records what the preprocessing depended
on, so that it can be reproduced and checked:

| key | example | meaning |
|---|---|---|
| `:command` | `["cc" "-E" "-undef" "-traditional"]` | GHC's "Haskell CPP command" and flags (`ghc --info`) |
| `:args` | `["-Dlinux_HOST_OS" "-Dx86_64_HOST_ARCH" "-D__GLASGOW_HASKELL_TH__" ...]` | the arguments GHC's driver passed, temporary paths replaced by their role (`<macros>`), paths under GHC's libdir written from `$libdir` |
| `:defines` | `["-DFOO=1"]` | the `-D` flags of the build (a `.cabal` file's `cpp-options`) |
| `:include-dirs` | `["include" "$libdir/../lib/x86_64-linux-ghc-9.14.1-07a2/rts-1.0.3/include"]` | the `-I` directories |
| `:files` | `{"$libdir/.../MachDeps.h" "sha256:..."}` | every file the output's line markers name, with its SHA-256: the headers that took part |
| `:macros` | `"sha256:..."` | the SHA-256 of the header of `MIN_VERSION_pkg` macros GHC's driver generates for the packages in scope |
| `:output` | `"sha256:..."` | the SHA-256 of the preprocessed text |

The preprocessed text keeps the module's lines: traditional CPP replaces removed lines by blank
ones and resynchronizes with line markers (`# 4 "C.hs" 2`), which GHC's lexer takes as line
pragmas, so every span is in the module's own file and its lines are the original's (Appendix A,
E3). Columns after an expanded macro are the output's (`z = __GLASGOW_HASKELL__` becomes
`z = 914`). The text level of a CPP module (§9.1) compares the printed module with the
**module view** of the preprocessed text: for each line of the module, the output line the
markers attribute to it, the lines of other files dropped (S5). The `#if` structure itself is
not in the forms: a module converted under two configurations has two sets of forms (H9 (c), a
form for conditional compilation, was not chosen).

## 3. Lexical syntax

### 3.1 Names

Haskell's names are written verbatim (ghc-lisp D3), as symbols, when the reader reads them back
(§3.2):

| Haskell | form | notes |
|---|---|---|
| variable `x`, `foldl'`, `x''`, `_x`, `λ`, `x#` (`MagicHash`) | `x`, `foldl'`, `x''`, `_x`, `λ`, `x#` | |
| constructor `Just`, `Int#`, `State#` | `Just`, `Int#`, `State#` | |
| variable operator `+`, `>>=`, `<$>`, `.`, `$`, `!!`, `+#`, `.|.`, `∘` | the same symbols | |
| constructor operator `:|`, `:+`, `:.`, `:%`, `:<|` | the same, read as keywords | a keyword whose name does not begin with a letter or `_` is a constructor operator (ghc-lisp D1) |
| qualified `M.insert`, `Data.Map.Map`, `M.!`, `Prelude..`, `M.:|` | `M.insert`, `Data.Map.Map`, `M.!`, `Prelude..`, `M.:|` | one symbol without a namespace: the dots are the name's (ghc-lisp D15) |
| implicit parameter `?x` | `?x` | |
| a backquoted name ``a `div` b`` | `div` in an operator slot of `:infix` or a section (§5.2) | |
| a parenthesized operator `(+)` as a value | `+` | ghc-lisp §4.1: an operator used as a value is the bare symbol |
| a name the reader does not read back | `(:name "text")` | §3.2 |

Haskell's reserved words cannot be names; the words special only under an extension or in a
context (`as`, `qualified`, `hiding`, `family`, `forall`, `pattern`, `role`, `proc`, `rec`,
`mdo`, `static`, `stock`, `anyclass`, `via`...) are special exactly where Haskell makes them so
(ghc-lisp S1). The reserved symbols and the context-special ones (`!`, `-`, `*`, `@`) follow
ghc-lisp's I3, with the respellings of §3.4.

### 3.2 The name rule

A Haskell name with text `t` (as GHC prints it: `M.insert`, `:+:`, `</>`) is written **bare**
exactly when the reader, reading `t` alone between delimiters, reads one form that is

- a symbol without a namespace whose name is `t`, or
- for a constructor operator, a keyword without a namespace that prints as `t`.

Otherwise it is respelled (S6):

- `(:name "t")` where a name stands as a plain name: a value, a type, a constructor in a pattern,
  an export or import item, a declared name, an operator in an `:infix` slot or a section. This
  is ghc-lisp's S2 (`(:name sym)`, the operator as a plain name) with the name always a string;
  in an operator slot the lexical class of `t` says whether it is an operator or a backquoted
  name, as for bare names. `((:name "/=") a b)` is the prefix application `(/=) a b`.
- `(:op "t" e1 e2 ...)` for an operator chain whose operator is respelled: the form
  `(t e1 e2 ...)` would be, if `t` could be read (§5.2). `(:op "/=" n 0)` is `n /= 0`.

`:` (cons) and the other built-in syntax have forms of their own (§3.3). The converter applies
the rule to every name it writes, and the printer of forms text (the converter's writer) checks
it again by reading what it wrote, as repr/go's printer is checked by its round trip; a misread
is a converter error, not a silent change. The reader's failures and misreads that the rule
catches (Appendix A, E1):

| text | the reader | why |
|---|---|---|
| `::`, `:+:`, `:*:`, `:~:`, `.:`, `M.:+:`, `:::` | `Invalid token` | a token ending in `:` |
| `/=`, `//`, `M./` | `Invalid token` | `/` placement |
| `</>`, `<//>`, `M.</>` | a symbol with namespace `<` (`M.<`) | `/` splits namespace and name |
| `::+`, `::>` | the keyword `:user/+` | `::` auto-resolves |
| `^`, `^^`, `.^.`, `^>>`, `~`, `~~`, `%~`, `.~`, `<~`, `#.`, `##` | `Unmatched delimiter`, or a reader macro | `^` (metadata), `~` (unquote), `#` (dispatch) |
| `<@>` | two forms: `<`, `(deref >)` | `@` is a reader macro |
| `\\` | the character `\` | `\` begins a character |
| `true`, `false`, `nil` (Haskell variables) | a boolean, `nil` | literals |
| `:@`, `:\` | `Invalid token` | |

Measured over GHC 9.14.1's `base` and `ghc-internal` (Appendix A, E2): of 6,995 distinct names
(100,327 occurrences) in the 445 modules that parse without CPP, 6,900 are bare (98,511
occurrences), 67 are built-in syntax (`:`, `[]`, `()`, tuple constructors: 1,465), and 28 are
respelled, 351 occurrences (0.35%): `:~:` 69, `#.` 44, `/=` 34, `:*:` 31, `:~~:` 23, `~` 19,
`^` 17, `:.:`, `:<>:`, `:+:` 14 each, `\\`, `//` 7 each, the arrow combinators `^>>`, `^<<`,
`>>^`, `<<^`, `.^.`, `~~` 6 each, `nil` 4, `</>` 4, `/=#` 4, `^^`, `Eq.:~:`, `:$$:` 2 each,
`^^%^^`, `^%^`, `N.^`, `Eq.:~~:` 1 each.

### 3.3 Built-in syntax

| Haskell | form | notes |
|---|---|---|
| `:` (cons), as a value, an operator, a pattern constructor | `:cons` | `(:cons x xs)` is the chain `x : xs`, `(:cons x)` is `(:) x` (ghc-lisp §4.1's one-operand rule), `(foldr :cons [] xs)` passes `(:)`; promoted `':` is `':cons` (it reads as `(quote :cons)`) |
| `[]`, `()` | `[]`, `()` | what Haskell's `[]` and `()` mean in that context (ghc-lisp §1) |
| `(,)`, `(,,)`, `(# , #)`, `(# | #)` as names | `(:tuple-con 2)`, `(:tuple-con 3)`, `(:utuple-con 2)`, `(:usum-con 2)`; a sum's data constructor `(:usum-con alt n)` | ghc-lisp S11; `(:utuple-con 0)` is `(# #)` |
| `->` as a name (`(->) r`) | `(:name "->")` | ghc-lisp S11 |
| `~` as a name (the equality) | `(:name "~")`, `(:op "~" a b)` | §3.2 |

### 3.4 The respelling table

Every Haskell token class that Arbace's reader cannot read, or reads as something else, and its
spelling in Haskell forms. The column "E1" names the samples of Appendix A that check it; every
form in the right column reads as one form of the expected class.

| Haskell / ghc-lisp | Haskell forms | why | E1 |
|---|---|---|---|
| `::` in signatures, field groups, kind signatures, binders, expression and pattern signatures, implicit-parameter types (ghc-lisp `(:: ...)`) | `(:sig ...)` inside declarations, `(hs/sig ...)` at top level; the shape is ghc-lisp's: names (or the subject) first, the type last | `::` is no token | `::`, `(:sig f T)` |
| `\ p1 p2 -> e` (ghc-lisp `(\ p+ e)`) | `(:lambda p+ e)` | `\` begins a character | `(\ x e)`, `(:lambda [x y] e)` |
| `\case`, `\cases` | `(:lambda-case alt*)`, `(:lambda-cases alt*)` | idem | `\case` |
| `:` (cons) | `:cons` (§3.3) | `:` is no token | `:`, `(: x xs)` |
| constructor operators the reader does not read back (`:+:`, `:*:`, `:~:`, `M.:+:`) | `(:name ":+:")`, `(:op ":+:" a b)` (§3.2) | a token ending in `:` | `:+:` |
| any other name the rule refuses (`/=`, `^`, `</>`, `\\`, `nil`) | `(:name "t")`, `(:op "t" ...)` | §3.2 | `/=`, `</>` |
| `'a'`, `'\n'`, `'\''` (characters) | `\a`, `\newline`, `\'`; `(:char n)` for a surrogate or a character beyond the BMP (§3.5) | `'a'` reads as `(quote a')`, `'\n'` fails | `'a'`, `\a`, `\😀` |
| `'Just`, `'[]`, `'[a b]`, `'(:tuple a b)` (promotion), `'f`, `''T` (Template Haskell name quotes) | the same, read as `(quote ...)` | unambiguous once characters are `\c` | `'Just`, `''T` |
| `~p` (lazy pattern), `~T` (lazy field) (ghc-lisp `(~ p)`) | `(:lazy p)`, `(:lazy T)` | `(~ p)` reads as `((unquote p))` | `(~ p)` |
| `a ~ b` (equality) | `(:op "~" a b)` | `~` is unquote | `~` |
| activations `[~2]`, `[~]` (ghc-lisp `[~ 2]`, `[~]`) | `[:before 2]`, `[:never]`; `[2]` unchanged | idem | `[:before 2]` |
| `@T`, `@_`, `@(Maybe a)` (type applications, binders) | as written: the reader reads `(arbace.core/deref T)`, which a consumer takes as `@T` (as Go forms' `@p`) | readable and unambiguous: no Haskell name reads as `arbace.core/deref` | `@Int`, `(f @T x)` |
| `x@p` (as-pattern) | `(:as x p)` (ghc-lisp) | `x@y` reads as two forms | `x@y` |
| `#foo`, `#"foo bar"` (overloaded labels) | `(:label "foo")`, `(:label "foo bar")` | `#` dispatches | `#foo` |
| `3#`, `3##`, `1.5#`, `1.5##`, `'x'#`, `"x"#`, `123#Int8` (`MagicHash`, `ExtendedLiterals`) | `(:int# 3)`, `(:word# 3)`, `(:float# 1.5)`, `(:double# 1.5)`, `(:char# \x)`, `(:string# "x")`, `(:int8# 123)` (§3.5) | `#` dispatches | `3#`, `(:int# 3)` |
| numerals `0o17`, `0b101`, `1_000`, `0x1.8p1`, `010` | `017`, `2r101`, `1000`, `(:hex-float 0x18 -3)`, `10` (§3.5) | invalid numbers; `010` reads as 8 | `0o17`, `010` |
| string escapes `\&`, `\SOH`, `\^A`, `\x41`, `\o101`, `\1234`, gaps | the characters they denote (§3.5) | invalid escapes; `"\1234"` reads as `"S4"` | `"\1234"` |
| `{-# INLINE #-}` and the other pragmas (ghc-lisp `(:inline ...)`) | `(:inline ...)` inside, `(hs/inline ...)` at top level | readable; qualified at top level (H4) | |
| `(:name sym)` (ghc-lisp S2) | `(:name "sym")` | one escape for all names | `(:name "~")` |
| backquotes, braces, commas, semicolons | ghc-lisp's forms (`:infix` slots, `:rec`, `:update`, `:tuple`, block forms) | no such tokens in forms | |

Unicode syntax (`∷`, `→`, `∀`, `⊸`, `★`) is written in ASCII; mode `:full` records which tokens the
source spelled in Unicode (§7.2). The reader reads all of them as symbols, but Haskell forms never
use them as heads.

### 3.5 Literals

A literal is written as a Clojure literal with the same value where one exists, and as a short
keyword form otherwise. In mode `:lines` only the value (and, for fractional literals, the
significand and exponent) is kept; in mode `:full` the source text too (`:hs/atext`, §7.2), so
`0x1F`, `0o17` and `"\SOH"` print back as written.

**Integers** (`HsIntegral`; `HsInteger` and the type-level `HsNumTy`): a Clojure integer, read as
a long or a BigInt (`N` when written by the reader). The converter keeps the source's radix where
the reader has it: decimal, hexadecimal `0x1F`/`0X1F`, octal `0o17` as `017`, binary `0b101` as
`2r101` (Go forms §8.1's table), without `_` separators. A decimal literal is written without
leading zeros: Haskell's `010` is ten, the reader's `010` is eight, and `08` does not read (E1).
A negative literal (`NegativeLiterals`, `LexicalNegation`: the literal's `il_neg`) is a negative
integer, `-5`; a negative literal whose value is zero is `(:neg-literal 0)`, since `-0` reads as 0.
Without those extensions `-5` is a negation, `(- 5)` (ghc-lisp D2). Under `NumDecimals`, `1.5e3`
is an integer literal (1500), written `1500`.

**Fractional literals** (`HsFractional`, `HsFloatPrim`, `HsDoublePrim`): GHC keeps a fractional
literal as significand, exponent and base (`FL` with `fl_signi`, `fl_exp`, `fl_exp_base`: `1e3` is
1 × 10^3, `1000.0` is 10000 × 10^-1, `1.5e-3` is 15 × 10^-4; Appendix A, E3), and the tree level
compares those fields. The forms keep them exactly (S7):

- a **double** when the reader's shortest text for it, read again by GHC's lexer, gives the same
  significand and exponent: `1.5`, `0.1`, `1.0E10`, `-1.5`;
- otherwise a **BigDecimal** whose unscaled value is the significand and whose scale is the
  negated exponent: `1e3` is `1E+3M`, `1.50` is `1.50M`, `1e400` is `1E+400M`, `2.5e-1000000`
  is `25E-1000001M`;
- a **hexadecimal float** (`HexFloatLiterals`, base 2) is `(:hex-float s e)`, the significand
  `s` (an integer, in the source's radix) and the binary exponent `e`: `0x1.8p1` is
  `(:hex-float 0x18 -3)` (24 × 2^-3 = 3);
- a negative literal is negative (`-1.5`, `-1.5M`); a negative zero is `(:neg-literal 0.0)`.

**Characters** (`HsChar`, `HsCharTy`): a Clojure character for a code point in the Basic
Multilingual Plane outside the surrogates; the reader's names where it has them (`\newline`,
`\space`, `\tab`, `\return`, `\backspace`, `\formfeed`), `\uXXXX` for the other controls, DEL and
the characters that do not print, the character itself otherwise (`\a`, `\'`, `\"`, `\\`, `\(`,
`\;`, `\é`, `\あ`). A surrogate code point (Haskell's `'\xD800'`, which the reader refuses as
`\uD800`) or one beyond U+FFFF is `(:char n)`, `n` an integer: `(:char 0x1F600)`.

**Strings** (`HsString`, `HsIsString`, `HsStrTy`): a Clojure string of the characters the
literal denotes, a code point beyond the BMP as the reader's surrogate pair. The converter writes
`"` and `\` escaped, `\n`, `\t`, `\r`, `\b`, `\f`, other controls and characters that do not print
as `\uXXXX`, the rest as themselves, never an octal escape. Haskell's escapes are values only:

| Haskell | denotes | in the form |
|---|---|---|
| `\a \b \f \n \r \t \v` | U+0007 U+0008 U+000C U+000A U+000D U+0009 U+000B | `\u0007 \b \f \n \r \t \u000b` |
| `\\ \" \'` | `\ " '` | `\\ \" '` |
| `\NUL` ... `\SP`, `\DEL` (the ASCII names) | U+0000 ... U+0020, U+007F | `\u0000` ... ` `, `\u007f` |
| `\^@` ... `\^_` (control) | U+0000 ... U+001F | `\uXXXX` |
| `\65`, `\x41`, `\o101` (decimal, hex, octal) | `A` | `A` |
| `\1234` | U+04D2 | `Ӓ` (or `Ӓ` if it did not print) |
| `\&` (the empty escape), gaps `\   \` | nothing | nothing |

A string that contains a surrogate code point is `(:string part...)`, each part a string or an
integer code point (`(:string "a" 0xD800 "b")`): the reader would join two surrogates into one code
point, which Haskell keeps apart (`"\55357\56832"` is two characters, `"😀"` one). A multiline
string (`MultilineStrings`, `HsMultilineString`) is `(:multiline "value")`, the value after GHC's
processing (indentation stripped, escapes resolved): `"""\n    hello\n      world\n    """` is
`(:multiline "hello\n  world")`. The printer can always spell it as a one-line `"""` literal with
escapes, which GHC reads to the same value (E3).

Whether a literal is overloaded (`HsOverLit`: integers, fractional literals, strings under
`OverloadedStrings`) or not (`HsLit`) follows from its context and the module's extensions, as in
GHC's parser (ghc-lisp §4: "Which one follows GHC's parser").

**Primitive literals** (`MagicHash`, `ExtendedLiterals`): keyword forms named after the literal's
type, lower case, with the value as above:

| Haskell | GHC | form |
|---|---|---|
| `3#`, `-3#` | `HsIntPrim` | `(:int# 3)`, `(:int# -3)` |
| `3##` | `HsWordPrim` | `(:word# 3)` |
| `1.5#`, `1.5##` | `HsFloatPrim`, `HsDoublePrim` | `(:float# 1.5)`, `(:double# 1.5)` |
| `'x'#` | `HsCharPrim` | `(:char# \x)` |
| `"x"#` | `HsStringPrim` (bytes) | `(:string# "x")`: each character one byte, 0-255 |
| `123#Int8` ... `123#Int64`, `123#Word8` ... `123#Word64` | `HsInt8Prim` ... `HsWord64Prim` | `(:int8# 123)` ... `(:word64# 123)` |
| `123#Int`, `123#Word` | `HsIntPrim`, `HsWordPrim` | `(:int# 123)`, `(:word# 123)` (the suffix is spelling, `:full`) |

**Type-level literals** (`HsTyLit`): the same literal forms in type context: `42`, `"sym"`, `\c`.

## 4. Declarations

### 4.1 Top-level heads

At top level, ghc-lisp's heads are qualified by `hs` (H4); inside a top-level form, they are not.

| ghc-lisp's top level | Haskell forms | Haskell |
|---|---|---|
| `(module ...)` | `(hs/module ...)` (§2.4) | `module M (...) where` |
| `(:language ...)`, `(:options-ghc ...)`, `(:options-haddock ...)` | `(hs/language ...)`, `(hs/options-ghc ...)`, `(hs/options-haddock ...)`, `(hs/options ...)` (§2.3) | header pragmas |
| `(import ...)` | `(hs/import ...)` (§2.6) | `import` |
| `(= lhs rhs)` | `(hs/= lhs rhs)` | bindings |
| `(:: f T)` | `(hs/sig f T)` | type signatures |
| `(pattern ...)` | `(hs/pattern ...)` | pattern synonyms and their signatures |
| `(infixl 6 +)`, `infixr`, `infix` | `(hs/infixl 6 +)`, `hs/infixr`, `hs/infix` | fixity |
| `(data ...)`, `(newtype ...)`, `(type ...)`, `(class ...)`, `(instance ...)`, `(deriving ...)`, `(default ...)`, `(foreign ...)` | `hs/data`, `hs/newtype`, `hs/type`, `hs/class`, `hs/instance`, `hs/deriving`, `hs/default`, `hs/foreign` | type-level, standalone deriving, defaults, foreign declarations |
| `(:inline ...)`, `:noinline`, `:inlinable`, `:opaque`, `:specialise`, `:scc`, `:complete` | `hs/inline`, `hs/noinline`, `hs/inlinable`, `hs/opaque`, `hs/specialise`, `hs/scc`, `hs/complete` | top-level pragmas |
| `(:deprecated ...)`, `(:warning ...)`, `(:ann ...)`, `(:rules ...)` | `hs/deprecated`, `hs/warning`, `hs/ann`, `hs/rules` | |
| `(:splice e)`; a bare top-level expression (a naked splice) | `(hs/splice e)`; `(hs/naked-splice e)` | `$(e)`, `$e`; `makeLenses ''T` |
| `;;|`, `;;^`, `;;*`, `;;$name` doc comments | `(hs/doc ...)` (§4.12) | `DocD` |

Every top-level form of a forms file is one of these heads (or `ns`); anything else is an error.
Inside them, the forms are ghc-lisp's with §3's respellings; the following sections give each
with those respellings applied. The canonical spelling (I6) is the first listed.

### 4.2 Signatures and bindings

| GHC | form | notes |
|---|---|---|
| `TypeSig` | `(hs/sig f g T)`, `(:sig f T)` inside | names, then the type |
| `ClassOpSig` | `(:sig f T)` in a class body; `(default (:sig f T))` the default signature | |
| `PatSynSig` | `(hs/pattern (:sig P Q T))` | |
| `FunBind`, one equation | `(hs/= (f p*) rhs)`, `(hs/= f rhs)`, `(hs/= (:infix p1 op p2) rhs)`, `(hs/= ((:infix p1 op p2) p*) rhs)` | adjacent equations of one name merge into one `FunBind` as GHC's `getMonoBind` merges them (ghc-lisp D7); `(= ...)` inside |
| strict variable binding `!x = e` | `(= (! x) rhs)` | `FunBind` with `SrcStrict` |
| `PatBind` | `(= pat rhs)`, the pattern not a variable or a variable's application: `(= (Just x) e)`, `(= (:tuple a b) e)`, `(= (:lazy p) e)`, `(= (:cons x xs) e)` | |
| multiplicity of a binding (`let %1 x = e`) | `(= (:mod 1 x) e)` | `pat_mult` |
| `IPBind` | `(= ?x e)` in `let`, `where` | |
| right-hand side | an expression, or guards `(| qual* expr)+`, then `(where binding*)?` | the last element of a guard is the body (ghc-lisp D7) |
| `PatSynBind` | `(hs/pattern lhs = pat)`, `(hs/pattern lhs <- pat)`, `(hs/pattern lhs <- pat (where (= ...)+))`; lhs `(P a b)`, `(:infix a :< b)`, `(:rec P a b)` | |

### 4.3 Data types, newtypes, GADTs

Declaration heads are `T`, `(T tv*)`, or `(:infix tv1 op tv2)`; binders `a`, `(:sig a K)`,
`@a`, `@(:sig a K)` (invisible), `_`; `(:inferred tv)` is `{a}`; a kind signature on the head
wraps it, `(:sig (T a) K)` (ghc-lisp S8).

| GHC | form |
|---|---|
| `DataDecl` | `(hs/data :ctype? head-or-ctx con* deriving*)`, `head-or-ctx` the head or `(=> ctx+ head)` |
| H98 constructor | `C`, `(C T*)`, `(:infix T1 :+ T2)`, record `(C (:sig f g T)+)`, no fields `(:rec C)` (S12); built-in syntax as the constructor (HR5): `()`, `(:tuple a b)` for `(a, b)`, `(:utuple a b)`, `(:usum a :_)` for `(# a | #)`; existentials `(forall tv+ (=> ctx+ con))`; a field type wrapped in `(! T)`, `(:lazy T)`, `(:unpack T)`, `(:nounpack T)`; a multiplicity `(:sig f (:mod m T))` |
| GADT body | `(where (:sig C+ type)*)`; a record GADT constructor `(:sig C (-> (:record (:sig f T)+) R))` (S13); `(:paren ...)` for a parenthesized GADT signature |
| `type data` | `(hs/type data head con*)` |
| `newtype` | `(hs/newtype head-or-ctx con deriving*)` |
| `DataFamInstDecl` | `(hs/data instance head con* deriving*)`, `(hs/newtype instance ...)`; head `(F T*)` or `(forall tv* head)` |
| `RoleAnnotDecl` | `(hs/type role T r*)`, `r` one of `nominal representational phantom _` |

### 4.4 Classes and instances

| GHC | form |
|---|---|
| `ClassDecl` | `(hs/class head-or-ctx fundeps? body*)`; fundeps `(| (a b -> c)+)`; the body: signatures, default signatures, bindings, fixity declarations, pragmas, `(:minimal bf)`, associated types `(type head)`, `(data head)`, `(type family ...)`, defaults `(type head T)`, `(type instance (= ...))`, docs |
| `ClsInstDecl` | `(hs/instance overlap? T body*)`, `T` the head with its context and forall (`(=> (Eq a) (Eq [a]))`); overlap `:overlapping :overlappable :overlaps :incoherent :no-overlap :noncanonical` |
| `MinimalSig` | `(:minimal bf)`, `bf` a name, `(:or bf+)`, `(:and bf+)`, `(:paren bf)` |

### 4.5 Type synonyms and families

| GHC | form |
|---|---|
| `SynDecl` | `(hs/type head T)` |
| `StandaloneKindSig` | `(hs/type (:sig T K))` |
| open `FamilyDecl` | `(hs/type family head result? inj?)`, `(hs/data family head result?)`; result `(:sig head K)` on the head, or `(= r)`, `(= (:sig r K))`; injectivity `(| r -> a b)` |
| closed family | `(hs/type family head result? inj? (where eqn*))`, `(where ..)` abstract |
| `FamEqn` | `(= (F T*) rhs)`, `(forall tv* (= (F T*) rhs))`, arguments `@k` allowed |
| `TyFamInstDecl` | `(hs/type instance eqn)` |

### 4.6 Deriving

| GHC | form |
|---|---|
| `HsDerivingClause` | `(deriving strategy? C)` (`DctSingle`), `(deriving strategy? [C*])` (`DctMulti`), `(deriving [C*] via T)`; strategy `stock`, `newtype`, `anyclass` |
| `DerivDecl` (standalone) | `(hs/deriving strategy? instance overlap? T)`, `(hs/deriving via V instance T)` |

### 4.7 Fixity

`(hs/infixl 6 + -)`, `(hs/infixr 5 type :+:)` (a namespace specifier), `(hs/infix 4 (:name "/="))`;
the precedence may be absent, as in Haskell; inside class bodies and `let`/`where`, unqualified.
Fixity declarations are data: their effect on chains is the renamer's (§5.2).

### 4.8 Pattern synonyms

§4.2's `hs/pattern` forms; in export and import lists `(pattern P)`; `COMPLETE` pragmas
`(hs/complete A B)`, `(hs/complete A B :sig T)` (ghc-lisp's `(:complete A B :: T)` respelled).

### 4.9 Foreign declarations

As data, in ghc-lisp's shape:
`(hs/foreign import cconv safety? "entity"? (:sig f T))`,
`(hs/foreign export cconv "entity"? (:sig f T))`; `cconv` one of `ccall capi stdcall prim
javascript`, `safety` one of `safe unsafe interruptible`. The entity string is kept as a value; its
spelling in `:full`.

### 4.10 Defaults and annotations

`(hs/default T*)`, named defaults `(hs/default C [T*])` (S9); `(hs/ann f e)`, `(hs/ann type T e)`,
`(hs/ann module e)`.

### 4.11 Pragmas

Pragma forms are keywords named after the pragma, lower case (ghc-lisp §6), qualified `hs/` at top
level (§4.1). Activations: none (`AlwaysActive`), `[2]` (`ActiveAfter 2`), `[:before 2]`
(`ActiveBefore 2`), `[:never]` (`NeverActive`).

| GHC | form |
|---|---|
| `InlineSig` | `(hs/inline act? :conlike? f)`; `hs/noinline`, `hs/inlinable`, `hs/opaque` likewise |
| `SpecSig` | `(hs/specialise :inline? act? f T+)` |
| `SpecSigE` | `(hs/specialise :inline? act? (forall ...)? e)` |
| `SpecInstSig` | `(:specialise instance T)` in an instance body |
| `SCCFunSig` | `(hs/scc f)`, `(hs/scc f "label")` |
| `RuleDecls` | `(hs/rules rule*)`, rule `("name" act? (forall tv*)? (forall bndr*)? (= lhs rhs))`, `bndr` `x` or `(:sig x T)` |
| `WarnDecls` | `(hs/deprecated ns? name+ msg)`, `(hs/warning in? "category"? ns? name+ msg)`; `msg` a string or a vector of strings, `ns` `type` or `data` |
| `{-# UNPACK #-}`, `{-# NOUNPACK #-}`, `{-# CTYPE #-}`, `{-# SOURCE #-}`, overlap pragmas | `:unpack`, `:nounpack`, `:ctype`, `:source`, `:overlapping` ... where §4.3, §2.6, §4.4 place them |
| `{-# SCC #-}` in an expression (`HsPragE`) | `(:scc "label" e)`, `(:scc label e)` |

The spelling of a pragma's opener (`{-# SPECIALIZE` against `{-# SPECIALISE`, `{-# INLINABLE`
against `{-# INLINEABLE`, case and spacing) is spelling: `:full` keeps it as `:hs/text` on the
pragma's form; in `:lines` the printer writes the canonical one (ghc-lisp's N2).

### 4.12 Documentation

Conversions run with `-haddock` (in `:config :flags`, S8): GHC's Haddock pass then puts the
documentation comments into the tree (`DocD`, `con_doc`, `cd_fld_doc`, `HsDocTy`, the export
list's docs, the module header), and the installed interfaces of the boot libraries carry them
(Appendix A, E3). Arbace's reader drops `;` comments, so ghc-lisp's `;;|` cannot carry them:
they are data (S9).

| GHC | form |
|---|---|
| a top-level `DocD`: `-- \|` (next), `-- ^` (previous), `-- *`, `-- **` (group), `-- $name` (named chunk) | `(hs/doc :next "text")`, `(hs/doc :prev "text")`, `(hs/doc :group 1 "text")`, `(hs/doc :group 2 "text")`, `(hs/doc :named "name" "text")` |
| the module header (`hsmodHaddockModHeader`) | `(hs/doc :next "text")` before `hs/module` |
| a doc inside a declaration (constructor, field, argument, class member, export item) | `(:doc :next "text")` before the documented element, `(:doc :prev "text")` after it |
| `{-| ... -}` (`NestedDocString`) | `:nested` after the decorator: `(hs/doc :next :nested "text")` |

`text` is the comment's text after its decorator, leading space included, the lines of a
multi-line comment (`MultiLineDocString`'s chunks) joined by newlines. A `(:doc ...)` element is
transparent for its form's grammar: it does not count among the form's elements, and attaches to
the element GHC's Haddock pass attaches the comment to (ghc-lisp D12): the printer writes the
comment where that pass will attach it again.

## 5. Expressions and patterns

### 5.1 Expressions

ghc-lisp's §4 with the respellings applied; the forms of operators are §5.2's.

| GHC | form | notes |
|---|---|---|
| `HsVar` | `x`, `M.x`, `Just`, `+` (as a value), `:cons`, `(:name "t")` | §3 |
| `HsOverLabel`, `HsIPVar` | `(:label "foo")`, `?x` | |
| `HsOverLit`, `HsLit` | §3.5's literals | |
| `HsLam` | `(:lambda p+ e)`, `(:lambda-case alt*)`, `(:lambda-cases alt*)`; an alternative of `\cases` is `(-> p+ rhs)` | |
| `HsApp` | `(f a b)`, left-nested (`((f a) b)` is the same tree) | an operator head is §5.2 |
| `HsAppType` | `(f @T x)` | §3.4 |
| `HsPar` | implicit where Haskell needs it (ghc-lisp I5, its §9 table and S18), `(:paren e)` otherwise | the tree level compares `HsPar` exactly |
| `ExplicitTuple` | `(:tuple e+)`, `:_` for a missing slot (`(a,)` is `(:tuple a :_)`); `(:utuple e*)` | |
| `ExplicitSum` | `(:usum :_* e :_*)` | the position of `e` is the tag |
| `HsCase` | `(case e alt*)`, `alt` `(-> pat rhs)`, rhs as §4.2 | |
| `HsIf`, `HsMultiIf` | `(if c t e)`; `(if (| qual* e)+)` | |
| `HsLet` | `(let binding+ e)`, the last element the body | |
| `HsDo` | `(do stmt+)`, `(mdo stmt+)`, `QualifiedDo` `(M.do ...)`, `(M.mdo ...)` | |
| list comprehensions | `[e | qual+]`, parallel `[e | qual+ | qual+]` | a vector whose second element is `|`; `MonadComprehensions` as in Haskell |
| `ExplicitList`, `ArithSeq` | `[e*]`; `[a ..]`, `[a b ..]`, `[a .. c]`, `[a b .. c]` | |
| `RecordCon` | `(:rec C field*)`, field `(= f e)`, `f` (pun), `..` (wildcard, last) | |
| `RecordUpd` | `(:update e (= f e)+)`; `OverloadedRecordUpdate` paths `(= (:get f g) e)` (S15) | |
| `HsGetField`, `HsProjection` | `(:get e f+)` (`e.a.b`), `(:proj f+)` (`(.a.b)`) | `OverloadedRecordDot` (ghc-lisp D15) |
| `ExprWithTySig` | `(:sig e T)` | |
| `HsTypedBracket`, `HsUntypedBracket` | `(:typed-quote e)`; `(:quote e)`, `(:quote-pat p)`, `(:quote-type T)`, `(:quote-decls d*)`, `'f`, `''T` | Template Haskell as data (S10) |
| `HsTypedSplice`, `HsUntypedSplice` | `(:typed-splice e)`; `(:splice e)`, `(:qq quoter "text")` | |
| `HsProc` | `(proc pat cmd)` | arrows (§5.5) |
| `HsStatic` | `(static e)` | |
| `HsPragE` | `(:scc "label" e)` | |
| `HsEmbTy` | `(type T)` | `RequiredTypeArguments` |
| `HsHole` | `_` | |
| `HsForAll`, `HsQual`, `HsFunArr` | `(forall ...)`, `(=> ...)`, `(-> a b)`, `(->. a b)` in expression context | as types (§6) |

Statements (`StmtLR`): `(<- pat e)` (`BindStmt`), `(let binding+)` in statement context
(`LetStmt`), an expression (`BodyStmt`, and `LastStmt` where GHC's parser makes one; ghc-lisp
S19), `(rec stmt+)` (`RecStmt`), the `|` groups of a parallel comprehension (`ParStmt`), and
`(then f)`, `(then f by e)`, `(then group using f)`, `(then group by e using f)` (`TransStmt`).

### 5.2 Operators and unresolved chains

GHC's parser does not know fixities: it builds every operator chain nested to the left, without
parentheses (`a + b * c` is `OpApp (OpApp a + b) * c`), and the renamer re-associates the chain by
the fixities in scope, possibly imported. The forms keep the parser's chain, as ghc-lisp's D4:

| form | Haskell | the tree |
|---|---|---|
| `(op a b c ...)`, two or more operands, `op` read bare | `a op b op c` | the parser's left-nested `OpApp` chain; `(++ a b c)` groups to the right only after renaming |
| `(:op "t" a b c ...)` | `a t b t c` with a respelled operator `t` | the same |
| `(:infix a op1 b op2 c ...)` | a mixed chain `a op1 b op2 c` | the same; an operator slot holds a bare operator, a respelled one `(:name "t")`, `:cons`, or a name (a backquoted operator: `(:infix x div y)` is ``x `div` y``) |
| `(- e)` | `-e` | `NegApp` |
| `(op e)`, `(:op "t" e)`, one operand, `op` not `-` | `(op) e` | `HsApp (HsVar op) e` |
| `((:name "t") a b)` | `(t) a b` | two `HsApp`s |
| `(:section-l e op)`, `(:section-r op e)` | `(e op)`, `(op e)` | `SectionL`, `SectionR`; `(:section-r div 2)` is ``(`div` 2)``; `(:section-r - 1)` is the section `LexicalNegation` writes `(- 1)` |

An operator form nested as an operand is parenthesized (`HsPar`, implicit by I5): `(* (+ a b) c)` is
`(a + b) * c`, whereas `(:infix a + b * c)` is the unresolved chain. `pi * r ^ 2` is therefore
`(:infix pi * r (:name "^") 2)` and not `(* pi (:op "^" r 2))`, which would be `pi * (r ^ 2)` with
parentheses the source does not have. Type operator chains are nested to the right by GHC's
parser (E3: `Int :+ Bool :* Char` is `HsOpTy Int :+ (HsOpTy Bool :* Char)`) and written the same
way (§6). Fixity declarations are kept as data (§4.7); a consumer that wants resolved chains must
resolve them itself, with the fixities of the module and its imports (HS6).

### 5.3 Patterns

| GHC | form |
|---|---|
| `WildPat`, `VarPat` | `_`, `x` |
| `LazyPat`, `BangPat` | `(:lazy p)`, `(! p)` |
| `AsPat` | `(:as x p)` |
| `ParPat` | implicit (I5), or `(:paren p)` |
| `ListPat`, `TuplePat`, `SumPat` | `[p*]`; `(:tuple p*)`, `(:utuple p*)`; `(:usum :_* p :_*)` |
| `OrPat` | `(:or p+)` |
| `ConPat` | prefix `C`, `(C @T* p*)`; infix `(op p1 p2 ...)`, `(:cons x xs)`, `(:infix p1 op p2 ...)`; record `(:rec C (= f p)* f* ..?)` |
| `ViewPat` | `(-> e p)` |
| `SplicePat` | `(:splice e)`, `(:qq q "text")` |
| `LitPat`, `NPat` | §3.5's literals; `(- 5)` a negated `NPat` |
| `NPlusKPat` | `(+ n 1)` under `NPlusKPatterns` |
| `SigPat` | `(:sig p T)` |
| `EmbTyPat`, `InvisPat` | `(type T)`; `@t` |

A pattern's parentheses follow I5's table: in `xs@(~(first : _))` the inner parentheses are
needed (`~` takes an atomic pattern) and implicit, the outer ones are not (`xs@~p` is legal), so
the pattern is `(:as xs (:paren (:lazy (:cons first _))))` (Appendix B).

### 5.4 Records, let, where, case, if

Records are §5.1's `:rec`, `:update`, `:get`, `:proj`: construction `(:rec C (= f e) g ..)`
(`g` a pun, `..` `RecordWildCards`), update `(:update r (= f e))`, dot syntax `(:get r a b)`. `let`
is `(let binding+ e)` in an expression, `(let binding+)` in a statement; `where` is the last
element of a right-hand side, `(where binding*)` (§4.2). `case`, `\case`, `\cases`, `if` and the
multi-way `if` are §5.1's.

### 5.5 Arrows and Template Haskell

Both are data, in ghc-lisp's forms (S10): the parsed tree holds them, and the bridge neither runs
splices nor desugars commands.

| GHC | form |
|---|---|
| `HsCmdArrApp` | `(-< f x)`, `(-<< f x)`, `(>- x f)`, `(>>- x f)` |
| `HsCmdArrForm` | `(:form e cmd*)` (banana brackets), `(:infix c1 op c2)` |
| `HsCmdApp`, `HsCmdLam`, `HsCmdPar` | `(cmd e)`; `(:lambda p+ cmd)`, `(:lambda-case alt*)`, `(:lambda-cases alt*)`; implicit or `(:paren cmd)` |
| `HsCmdCase`, `HsCmdIf`, `HsCmdLet`, `HsCmdDo` | `(case e alt*)`, `(if c c1 c2)`, `(let b+ cmd)`, `(do stmt+)` |

Template Haskell: §5.1's quotes and splices, §4.1's `hs/splice` and `hs/naked-splice`, name quotes
`'f` and `''T` (read as `quote`), quasi-quotes `(:qq quoter "text")` with the body as text. A
module's splices are not run by the conversion: the forms are the module as written, which is
what GHC's parser sees. The interface level of a module with splices runs them as GHC compiles
both sides (§9.3).

## 6. Types and kinds

ghc-lisp's §7 with the respellings applied. Kinds are types.

| GHC | form | notes |
|---|---|---|
| `HsForAllTy` | `(forall tv+ T)` (invisible), `(forall tv+ -> T)` (visible) | binders as §4.3 |
| `HsQualTy` | `(=> ctx* T)`, the last element the body; `(=> T)` is `() => T` | |
| `HsTyVar` | `a`, `Maybe`, `M.T`, `'Just` (promoted), `+`, `(:name "t")` | |
| `HsAppTy`, `HsAppKindTy` | `(Maybe a)`; `(T @k a)` | |
| `HsFunTy` | `(-> a b c)` (right-nested), `(->. a b)` (linear: `%1 ->`, `⊸`), `(-> (:mod m a) b)` (`%m ->`) | |
| `HsListTy` | `[T]` | one element |
| `HsTupleTy`, `HsSumTy` | `()`, `(:tuple A B ...)`, `(:utuple ...)`; `(:usum A B ...)` | |
| `HsOpTy` | `(op A B ...)` (right-nested, as GHC's parser builds type chains), `(:op "t" A B)`, `(:infix A op B ...)`, promoted `(':cons A B)` | the equality `a ~ b` is `(:op "~" a b)` |
| `HsParTy` | implicit (I5) or `(:paren T)` | |
| `HsIParamTy` | `(:sig ?x T)` | |
| `HsStarTy` | `*` (`StarIsType`) | |
| `HsKindSig` | `(:sig T K)` | |
| `HsSpliceTy` | `(:splice e)`, `(:qq q "text")` | |
| `HsDocTy` | the type followed by `(:doc :prev "text")` (§4.12), an element of the arrow, tuple, deriving list, constructor or synonym the type is in; elsewhere `(:doc-type T (:doc ...))` (HR5) | |
| `HsExplicitListTy`, `HsExplicitTupleTy` | `'[T*]`, `[T T+]` (two or more elements, unpromoted); `'(:tuple T*)` | |
| `HsTyLit` | `42`, `"sym"`, `\c` (§3.5) | |
| `HsWildCardTy` | `_` | |
| tuple, list and arrow constructors as names | `(:tuple-con n)`, `(:utuple-con n)`, `[]`, `()`, `(:name "->")` | §3.3 |

A type argument that is an operator is `@(:name "t")` or `@+`; a promoted operator applied prefix
is `('(:name ":+:") x y)`, the promoted cons `(':cons x xs)` (ghc-lisp S16).

## 7. Layout and positions

GHC's positions reach a program's behaviour and its interfaces with their columns:
pattern-match failures (`N.hs:3:1-14|function g`), `HasCallStack` call stacks, `assert`, HPC.
Inlined code carries them into interface files: two copies of a module that differ only in a
column of an equation have different interfaces, through the unfolding's error string (Appendix A,
E3). The forms keep positions in one of two modes (H7), recorded as `hs/module`'s `:positions`.

### 7.1 Mode `:lines` (the default)

Positions are lines, carried by the layout of the forms text, as Go forms' `:lines` (Go SPEC
§10.1):

- **The forms file has the Haskell file's lines.** Every form that stands for a located node
  carries the node's start line, and the converter writes it on that line of the `.ae` file (HR3),
  so that the reader's `:line` of every list is its node's; a list's head is not moved to its
  line (an operator's head stands where Haskell writes the first operand) and gets its line as a
  symbol's or a literal's below. A Haskell file never has fewer lines than its
  forms need, since any form can be written on one line. `ns` shares the line of the first form
  (the first header pragma, or `hs/module`); `hs/module` begins on the line of the `module`
  keyword, its exports on their lines, and its options with no Haskell position (`:file`,
  `:config`, `:cpp`) after the last export, on the line of `where`.
- The reader attaches `:line` to every list. **Symbols and vectors** that begin a Haskell line
  get it written: `^{:line 5} area`. A **literal** or keyword that begins a Haskell line is
  recorded in its parent's `:hs/breaks`, a vector of entries `[index line]` (by `nth`, the head 0;
  HR3), also for a head literal or keyword whose line is not its list's (Go's `:go/breaks`).
  `()`, which the reader gives no line, gets `^{:line N}` like a symbol.
- **Token lines** (HR3): the located nodes that begin with a token the forms have no element for
  get their line in metadata on the form that has the token: `:hs/eq` on an equation, a case
  alternative or a lambda, the line of the `=` or `->` of an unguarded right-hand side when it is
  not the form's; `:hs/brace` on a record constructor (its `{`), a `case`, `\case`, `do` or a
  pattern synonym's `where` (the line its located list of items begins on) when it is not the first
  item's; `:hs/paren` on a form whose implicit parentheses begin on another line than it;
  `^:hs/braces` (S12) on a `case` or `\case` without alternatives whose source wrote `{}`.
- **Lists placed after their line** carry an explicit `:line`, which the reader keeps over its own
  (Go SPEC §10.1): the case of a form that must follow another the Haskell line puts before it.
- **The lines of the end of a node** (a multi-line span's last line) reach run-time messages
  (`N.hs:(3,1)-(5,20)`); they are kept in so far as the node's last token begins a form that begins
  a line, which is the usual case; the round trip's levels say what is compared (§9.2).

What `:lines` does not keep: columns; the positions of tokens (keywords, `=`, `->`, parentheses,
commas); comments other than documentation (§7.3); layout against explicit braces; the spelling of
literals and pragmas. The tree level compares what `:lines` keeps (§9.2); the interface level
normalizes the columns inside unfoldings (§9.3).

Forms written by hand (no converter) have the lines of their own text, which is what source lines
mean: a module printed from them reports the `.ae` file's lines (the printer may emit
`{-# LINE N "file.ae" #-}` pragmas, as Go's `//line`, HS7).

### 7.2 Mode `:full`

Every position of the parsed tree is kept, and with the text the tree does not hold, the printer
gives the original module back byte for byte (the text level, §9.1). The metadata, written only in
this mode:

- **`:hs/pos`** on every list and symbol: a map from position names to positions. `:span` is the
  node's `SrcSpan` as `[line col end-line end-col]`; the other keys are the node's exact-print
  tokens (GHC's `EpToken`, `EpUniToken`, `AnnList`, `AnnParen` and the annotation records), each
  `[line col]`, a vector of them for a repeated token (commas, semicolons, `|`s): `:where`, `:let`,
  `:in`, `:do`, `:of`, `:then`, `:else`, `:module`, `:import`, `:as` and the other keywords by
  their name, `:eq` (`=`), `:dcolon` (`::`), `:rarrow` (`->`), `:larrow` (`<-`), `:darrow` (`=>`),
  `:vbar` (`|`), `:lam` (`\`), `:at` (`@`), `:tilde` (`~`), `:bang` (`!`), `:dotdot` (`..`),
  `:comma`, `:semi`, `:lparen`/`:rparen`, `:lbrack`/`:rbrack`, `:lbrace`/`:rbrace`,
  `:open`/`:close` (a pragma's `{-#` and `#-}`, `(#`/`#)`, `[|`/`|]`), `:backquote` (a pair). A form
  that stands for several located nodes (an equation `(hs/= ...)` is a `FunBind`, a `Match` and
  their `GRHSs`) carries their spans under keys named after the node (`:match-span`,
  `:grhs-span`): the table of keys per constructor is the converter's, kept with the GHC version
  (S11).
- **`:hs/apos`** on the parent of a literal or keyword child: a map from child index to its span.
- **`:hs/paren`**: the parentheses of an implicit `HsPar` (I5) the form absorbs, `[[lparen
  rparen] ...]`, outermost first.
- **`:hs/atext`** on the parent of a literal, `{index "text"}`, and `:hs/text` on a literal form
  (`(:int# 3)`, `(:multiline ...)`) or a pragma's form: the source spelling, where the printer
  would spell the value otherwise.
- **`:hs/layout`** on a block form (`do`, `let`, `where`, `case`'s alternatives, a class or
  instance body, the module's body): `{:braces true}` for explicit braces (their positions in
  `:hs/pos`), `{:column c}` for a layout block opened at column `c` (GHC's `EpVirtualBraces`).
  In mode `:lines`, the converter writes `^:hs/braces` on a block the source wrote with explicit
  braces, a hint for the layout printer (S12).
- On `hs/module`: **`:hs/comments`**, every comment of the module as the source spells it,
  `[[line col end-line end-col "text"] ...]`, header pragmas and documentation comments included
  (GHC keeps the header pragmas among the comments; the printer takes their text from here, not
  from the `hs/language` forms, as Go's printer takes doc comments from `:go/comments`);
  **`:hs/space`**, `[[line col "ws"] ...]`, the white space runs other than single spaces between
  tokens and newlines (tabs, form feeds, carriage returns, trailing spaces, more than the printer's
  minimum); **`:hs/tail`**, the text after the last token when it is not one newline;
  **`:hs/unicode`**, `[[line col] ...]`, the tokens the source spelled in Unicode syntax.

Columns are GHC's: counted in characters (code points) from 1, a tab advancing to the next tab
stop of 8 (GHC's lexer, `advance_tabstop`), so a tab is recorded in `:hs/space` (S13). Positions
are the file's raw positions; GHC's `{-# LINE #-}` pragmas and CPP's markers are themselves text
(comments, or the CPP module view, §2.8). This mode is for tools, not meant to be read
(`bin/repr-haskell read --full`).

### 7.3 Comments

- Documentation comments are data (§4.12).
- Other comments are written by the converter as `;` comments in the forms text, at their Haskell
  line where the layout allows (`; text` at the end of a line, `;; text` on a line of their own),
  and are lost when the forms are read, as Go forms' (§10.4 there); in mode `:full` every comment
  is data too (`:hs/comments`). They do not count for the tree level (GHC's tree holds no ordinary
  comments: `-dkeep-comments` puts them among the exact-print annotations, which the tree level
  blanks).

### 7.4 The printer's layout

The printer writes Haskell from forms with no knowledge but the forms (it resolves no names), in
one of three layouts (H6):

1. **Braces** (the first milestone, S14): every block in explicit braces and semicolons,
   `do {`, `let {`, `where {`, `of {`, an instance body's `where {`, with a `;` between items,
   **except the module's body and a class's body**, which are in layout (HR2): their items at
   column 1 and 3, each on a line of its own (a `;` only between two items on one line), the lines
   inside an item indented further (2 and 5). GHC's Haddock pass restricts the documentation
   comments a block takes by their column only under virtual braces (`addHaddockInterleaveItems`,
   `mkDocDecl`), and it interleaves only these two blocks: under explicit braces a `-- |` after a
   class goes into the class body and a top-level `-- ^` after a signature into its type. GHC parses it to the same tree after `eraseEpLayout`,
   which is what the tree level compares (check-ppr's method, §9.3). In mode `:lines` each form
   that carries a line is printed on that line (Haskell's tokens can always share a line, except
   after a `--` comment, which the printer does not write in this layout); a form whose line was
   already passed (the header took more lines than the original's) gets a `{-# LINE N "file" #-}`
   pragma before it, which GHC's lexer honours, so the spans stay the original's.
2. **Layout** (the second milestone): Haskell's indentation, as GHC's `ppr` lays out: a block's
   items aligned at one column, greater than the enclosing block's; an item's continuation lines
   indented further; `where` indented under its equation; a block's first item on the keyword's
   line or the next. The printer keeps a stack of layout columns, as the Haskell report's
   algorithm L, so that every line it starts opens, continues or closes the right block;
   `NondecreasingIndentation` (11 modules of `base`/`ghc-internal`) is never needed by the
   printer's own output. Blocks marked `^:hs/braces` keep their braces. In mode `:lines` the
   recorded lines are kept as in the braces layout. (As built, §10.7: the block columns, the
   blocks that keep or take braces, the style.)
3. **Full**: every token at its recorded line and column (§7.2), comments and white space from
   `hs/module`'s records; the output is the original text (the text level). Layout is not
   computed: the original's columns are valid layout.

Without positions (forms built by a program), the braces or layout printer lays the module out
from nothing, as Go's printer lays out gofmt's way.

## 8. The helper

### 8.1 What it is

`tools/hsdump/HsDump.hs` (with its modules), a Haskell program on GHC's API, seeded from the
survey's 86-line experiment (survey, Appendix A). It is Arbace's own code, written anew; GHC's
`utils/check-ppr` (its `eraseEpLayout`) and `utils/check-exact` are its models, not vendored
(HS8). It decides nothing about forms: it walks the tree generically through `Data` and prints what
it finds, so a new GHC release changes the converter's table (new or changed constructors show up
as unknown ones), not the helper (survey §3.2 (b)).

### 8.2 What it dumps

`hsdump dump [--full] [--lines] -o OUT FILE...`: for each module, one file of Clojure-reader forms
(read with `read-string`, as E2's 659 dumps of `base` were), in this shape:

- a first form `(hsdump/config {...})`: the configuration as §2.7 lists it, the preprocessing as
  §2.8, the helper's version and source hash, the source file and its SHA-256;
- then the tree: every constructor as `(Name field...)`, positional, in the constructor's field
  order (the converter is keyed by constructor name and arity for its GHC version); lists as
  vectors; `L` nodes as `(L span x)`, the span `[l1 c1 l2 c2]`, with the file name only when it is
  not the module's (`["Foo.h" l1 c1 l2 c2]`, a CPP inclusion);
- names (`RdrName`) structured: `(unqual ns "occ")`, `(qual ns "Mod" "occ")`, `(orig ns "unit" "Mod"
  "occ")`, `(exact "occ")` (built-in syntax), `ns` one of `var data tv tc` (the `OccName` namespace);
  module names `(mod "Data.Map")`;
- strings (`String`, `FastString`) as Clojure strings with every control, DEL and surrogate as
  `\uXXXX` (a lone surrogate written raw would be invalid UTF-8); `SourceText` as `(src "text")` or
  `nil`; `Integer`, `Int`, `Rational` as `(:% n d)`, `Bool`, `Char` as `(char n)`;
- exact-print annotations (`EpAnn`, `EpToken`, `EpUniToken`, `AnnList`, `EpLayout`...): with
  `--full`, every one with its positions; otherwise only what the converter needs in `:lines` (the
  layout's kind, for `^:hs/braces`); comments with `--full`, as GHC keeps them under
  `-dkeep-comments` (`Opt_KeepRawTokenStream`, E3);
- documentation (`HsDocString`) as `(doc decorator [chunk...])`, each chunk located;
- errors as `(hsdump/error "message")`, the exit status 1.

`hsdump tree [--lines|--full] FILE`: the tree level's normal form of a module (§9.2), the text of
`showAstData BlankSrcSpan BlankEpAnnotations` after `eraseEpLayout` with the normalizations of mode
`:lines`, or `showAstData NoBlankSrcSpan NoBlankEpAnnotations` (what `-ddump-parsed-ast` prints)
for `:full`, and `hsdump lines FILE`, the start line of every located node, in the tree's order,
with its constructor (HR3: the two lists compared position by position). The
comparison is a text comparison of two such outputs, so no separate comparator program is needed
(S15; repr/go has `tools/gocmp` because its levels need gc's object files read).

### 8.3 How it is built

By `bin/repr-haskell helper`, with the host's GHC and nothing else: `ghc -O1 -package ghc
-outputdir .tmp/hsdump/obj -o .tmp/hsdump/hsdump tools/hsdump/HsDump.hs`. It uses only the
packages GHC ships (`ghc`, `base`, `containers`, `bytestring`, `filepath`, `directory`,
`process`; Appendix A, E4's package list), no Cabal, no Hackage, no network. The executable is
rebuilt when its sources or `:ghc-unit` change (a key in `.tmp/hsdump/key`). E2's helper was a
28.9 MB executable built in under a minute.

### 8.4 Versions

`tools/hsdump/GHC_VERSION` pins the GHC the bridge was written against (`9.14.1`). The helper
checks at start that the GHC it was compiled with (`__GLASGOW_HASKELL__`, `cProjectVersion`) is
the pinned one and that the libdir it is given belongs to it, and refuses otherwise; the converter
refuses a dump whose `:ghc` differs from the pin; a corpus's forms record `:ghc` and `:ghc-unit`
(§2.7). When Alpine's `ghc` moves (Alpine edge), moving the pin is a recorded decision: the
converter's table is revised for the new tree's constructors, and the forms regenerated (H11; a
GHC binary distribution pinned by hash, as `seed/goroot.sha256` pins Go, only if Alpine's package
cannot serve, HS9). GHC is added to doc/ALPINE.md's subset when the bridge is built (H11).

### 8.5 Preprocessing: CPP, unlit, hsc2hs

The helper preprocesses as GHC's driver does, through its API (`GHC.Driver.Pipeline.preprocess`:
`unlit` for literate modules, CPP when `CPP` is on, the `-F` preprocessors), then parses the
result with the dynamic flags the module's pragmas give (`parseDynamicFilePragma`, as E2 did).
CPP is GHC's "Haskell CPP command" (`cc -E -undef -traditional` on this host, Alpine's GCC 15.2)
with GHC's own arguments: the platform's `-D` flags, GHC's include directories, `ghcversion.h`,
and the `MIN_VERSION_pkg` macros of the packages in scope (E3). The helper records them (§2.8),
reading the included files' names from the output's line markers.

`.hsc` modules (`hsc2hs`'s input: C constants for the FFI) are not GHC's driver's business: the
corpus step runs the host's `hsc2hs` (Alpine's, with GHC) and converts its `.hs` output, recording
the tool's version and the input's and output's SHA-256 in `:cpp` (HS5). There are 7 in `base`, 27
in `ghc-internal`, 50 in `unix`.

## 9. The round trip and its corpus

### 9.1 Levels

The round trip is *Haskell → helper → converter → forms → printer → Haskell*. For a module converted
under a configuration, the original and the printed module are compiled with the same GHC, flags,
extensions, preprocessing and package database (H8). They are equivalent when:

1. **Tree.** GHC parses the printed module to the same tree as the original, compared as §9.2
   says for the positions mode (ghc-lisp's `--lisp-check` and `utils/check-ppr`'s method). This is
   the first gate: a tree GHC's parser cannot tell apart is the same program for everything after
   it.
2. **Interface.** The two modules' interface files (`.hi`) are equal as §9.3 says: exports, types,
   instances, fixities, unfoldings, documentation.
3. **Core and code** (later). `-ddump-simpl -dsuppress-uniques` equal, then the object code
   symbol by symbol, as repr/go's code level.
4. **Behaviour** (later). ghc-lisp's `run-corpus.sh` method: the testsuite's single-module
   `should_run` programs, printed, run with the same output and exit code.

Beside them, the **text level**: in mode `:full`, the printed module is byte-identical to the
original (for a CPP module, to its module view, §2.8). It needs no compilation. When it holds, the
interface files are byte-identical too (they depend on the source's text through its hash), so in
mode `:full` the interface level only localizes a difference.

Levels 1, 2 and the text level come first (H8); Core, code and behaviour later.

### 9.2 What the tree level compares

Both trees are printed by `hsdump tree` (§8.2) and compared as text. In mode `:full` everything is
compared: spans, exact-print annotations, comments, source texts (`showAstData NoBlankSrcSpan
NoBlankEpAnnotations`). In mode `:lines`:

| N | difference | how it is normalized |
|---|---|---|
| N1 | spans | blanked (`BlankSrcSpan`); instead, `hsdump lines` compares the start lines of every located node (HR3) |
| N2 | exact-print annotations: token positions, Unicode spelling, comments, explicit braces against layout | blanked (`BlankEpAnnotations`) after `eraseEpLayout` |
| N3 | source texts: literal spellings (`IL`'s, `FL`'s `SourceText`, strings', characters'), pragma openers (ghc-lisp's N2), the spelling of `StringLiteral`s (foreign entities, rule names, warning texts) | every `SourceText` blanked to `NoSourceText` |

Everything else is compared exactly: `HsPar` and every parenthesis the tree holds, chains as
parsed, literal values with the significand, exponent and base of fractional literals (§3.5),
`NestedDocString` against `MultiLineDocString` (ghc-lisp's N3 is not needed: the forms keep the
difference), the merge of equations, the order of everything. ghc-lisp's N4 (skipped files) is a
known-differences entry here (§9.5).

### 9.3 How each level is checked

| level | how | normalization |
|---|---|---|
| tree | `hsdump tree` of the original and of the printed module, compared as text; `hsdump lines` in `:lines` | §9.2 |
| interface | each module compiled as its package compiles it (`ghc --make`, the package's `-this-unit-id`, extensions and flags, `-haddock`, `-O` as the corpus says), once from the originals and once with all the package's printed modules in their places (HR8), under the same relative paths, at `-O1` (the corpus's `:iface :opt`, HR7); `ghc --show-iface` of the two `.hi`, compared as text (in `:lines`, token by token) | drop the lines `src hash:`, `interface hash:` and `flags: fingerprint:` (self-recompilation data: the source's hash, a hash over the rest, and a hash of the flags, which include the output directory: E3, E5). In `:full` the ABI hash and everything else must be equal. In `:lines` also: the source spans inside unfoldings' strings normalized to lines (`N.hs:3:1-14` → `N.hs:3`, `N.hs:(3,1)-(5,20)` → `N.hs:3-5`), the two columns of every `SrcLoc` construction in unfoldings blanked (also through its wrapper `$WSrcLoc`, boxed arguments `(I# 7#)` and let-bound ones, `let { $dIP5 :: Int = I# 47# } in ... $WSrcLoc ... $dIP5`), every fingerprint blanked (ABI, export, orphan, usage and the declarations' own hashes, which cover them), the usages of the files CPP included (`addDependentFile`) dropped (the printed module is the preprocessed text, §2.8, which includes none), and the texts compared by tokens, not lines (`--show-iface` breaks lines by width, and a column's digits change it) (HR6) |
| text | the printed file against the original (or the CPP module view), byte for byte | none |
| Core (later) | `-ddump-simpl -dsuppress-uniques -dsuppress-ticks` of both | as the interface level for spans in `:lines` |

Measured (E3, E5): a module laid out differently (explicit braces on three lines against layout)
gives the same `--show-iface` but for `src hash` and `interface hash`, the ABI hash equal; a column
moved inside an inlined equation changes the unfolding's error string and the ABI hash; 236
modules of 9.14.1's `base` compile with the host's GHC in 9.5 s at `-O0`, and two builds into two
output directories differ in `flags: fingerprint` and `interface hash` only.

GHC's `utils/check-exact` (in 9.14.1's source tree, 8,883 lines, ghc-exactprint's printer) proves
GHC's own exact printer over its testsuite; it is the model of the `:full` printer, and may serve as
a second judge of it while it is written (built from the corpus's sources, never a dependency).

### 9.4 The corpus

H10: `base` and the boot libraries of the pinned GHC first, from GHC 9.14.1's source distribution,
`ghc-9.14.1-src.tar.xz` (https://downloads.haskell.org/ghc/9.14.1/ghc-9.14.1-src.tar.xz,
33,970,212 bytes, SHA-256
`2a83779c9af86554a3289f2787a38d6aa83d00d136aa9f920361dd693c101e77`), fetched once into
`.tmp/repr-haskell/corpus/` and checked by its hash (HS4). Alpine's package ships no `.hs` sources.
The libraries, in order (counts of `.hs`, `.hs-boot`, `.hsc`, `.lhs` outside tests and benchmarks;
modules that declare `CPP`, by a grep, approximate):

| package | modules | CPP | `.hsc` | first |
|---|---|---|---|---|
| `base` | 264 | 16 | 7 | R1 |
| `ghc-internal` | 327 | 84 | 27 | R1 |
| `ghc-prim`, `ghc-bignum` | 1, 1 | 0 | 0 | R1 |
| `containers`, `text`, `bytestring`, `array`, `deepseq` | 37, 61, 33, 16, 3 | 33, 39, 15, 2, 1 | | R5 |
| `transformers`, `mtl`, `stm`, `binary`, `pretty`, `parsec`, `exceptions` | 31, 25, 17, 12, 7, 26, 3 | 30, 0, 13, 9, 0, 1, 2 | | R5 |
| `filepath`, `directory`, `process`, `time`, `unix`, `haskeline` | 16, 10, 11, 48, 54, 32 | 11, 2, 8, 6, 3, 1 | 0, 3, 2, 1, 50, 3 | R5, R6 |
| `template-haskell`, `Cabal`, `Cabal-syntax` and the rest | 12, 150, 140, ... | 1, 18, 7, ... | | R5 |

Each package is converted under its own configuration, written once in a corpus file
(`test/haskell/corpus-ghc-9.14.1.edn`): its modules for this OS (from its `.cabal.in`, without the
Windows and JavaScript ones), `default-language`, `default-extensions` (`NoImplicitPrelude` for
`base` and `ghc-internal`), `cpp-options`, include directories, the unit id and the dependencies
the interface level compiles against (the installed packages of the host's GHC). E2 dumped 445 of
the 535 `.hs` files of `base` and `ghc-internal` without preprocessing; 85 failed for want of CPP,
5 for other reasons (modules for another backend). After the boot libraries: GHC's testsuite
programs for the behaviour level, then a Stackage subset (H10).

### 9.5 Known differences

As repr/go's (`test/repr/go/roundtrip-*-known.edn`): one file per corpus and level,
`test/haskell/roundtrip-CORPUS-known.edn`, a map from module to the reason it differs or is
skipped, read with the Clojure reader; the round trip fails on a difference not listed and on a
listed one that no longer occurs.

### 9.6 The command (a sketch for the build)

```
bin/repr-haskell helper                     build tools/hsdump (§8.3)
bin/repr-haskell corpus fetch|list          the corpus tarball, checked by its hash; its modules
bin/repr-haskell dump [--full] FILE...      run the helper
bin/repr-haskell read [--full] FILE...|--corpus C    convert into .tmp/repr-haskell/forms/
bin/repr-haskell print [--layout braces|layout|full] FILE.ae...
bin/repr-haskell roundtrip [--text] CORPUS  the tree level (and the text level)
bin/repr-haskell check [--levels tree,iface] CORPUS
bin/repr-haskell test                       the spec's cases (test/haskell/)
```

## 10. Coverage plan

### 10.1 Milestones

| | what | judge |
|---|---|---|
| R1 | the helper; the converter and the braces printer in `:lines`; `base`, `ghc-internal`, `ghc-prim`, `ghc-bignum` without CPP, then with CPP (built 2026-10-17: 483 of 483 modules, §10.4) | tree level |
| R2 | the interface level over R1's corpus (built 2026-10-17: 483 of 483 modules, §10.5) | interface level |
| R3 | `:full`: the helper's annotations and comments, the converter's metadata, the full printer (built 2026-10-17: 483 of 483 modules byte-identical, 480 with equal trees of mode `:full`, §10.6) | text level, tree level in `:full` |
| R4 | the layout printer (built 2026-10-17: 483 of 483 modules at the tree and interface levels, §10.7) | tree and interface levels |
| R5 | the other boot libraries (built 2026-10-17: 37 packages, 743 modules, all at the tree and interface levels in both layouts, 731 at the text level, §10.8) | all of the above |
| R6 | `.hsc` (hsc2hs), `.lhs` (unlit), boot files and signatures in the corpus | all of the above |
| later | Core and code levels; behaviour over GHC's testsuite; a Stackage subset | |

By the survey's estimate (§5.3), R1-R3 over `base` and the boot libraries are about 6-9 working days.

### 10.2 Extensions

Every extension's syntax is in the grammar (ghc-lisp D14: all of GHC's syntax); the corpus decides
what is tested first. In `base` and `ghc-internal` 9.14.1, by the number of modules whose
`LANGUAGE` pragmas name them (E6): `NoImplicitPrelude` 283, `Trustworthy` 204, `Safe` 172,
`MagicHash` 141, `CPP` 123, `UnboxedTuples` 90, `BangPatterns` 67, `ScopedTypeVariables` 57,
`PolyKinds` 41, `Unsafe` 37, `RankNTypes` 30, `DeriveGeneric` 29, `TypeApplications` 26,
`TypeOperators`, `FlexibleInstances`, `DataKinds` 25 each, `StandaloneDeriving`,
`GeneralizedNewtypeDeriving` 24, `UnliftedFFITypes` 21, `TypeFamilies`, `GADTs` 18,
`RecordWildCards`, `ExistentialQuantification` 16, `GHCForeignImportPrim` 14, `LambdaCase` 13,
`StandaloneKindSignatures`, `PatternSynonyms`, `KindSignatures`, `DeriveDataTypeable` 12,
`NondecreasingIndentation`, `DerivingStrategies`, `CApiFFI` 11, `ConstraintKinds` 10,
`RoleAnnotations`, `ExplicitNamespaces`, `BlockArguments` 9, `NegativeLiterals`, `ImplicitParams`
8, `MultiWayIf` 7, `ViewPatterns` 6, `InterruptibleFFI` 5, `UnboxedSums`, `TemplateHaskellQuotes`,
`QuantifiedConstraints`, `NumDecimals`, `BinaryLiterals` 4, `UnliftedNewtypes`, `RecursiveDo`,
`NoStarIsType`, `NamedFieldPuns`, `JavaScriptFFI`, `FunctionalDependencies`, `EmptyCase`,
`DerivingVia` 3, `OverloadedStrings`, `LinearTypes`, `ImpredicativeTypes` 1. (Extensions on by
default under `GHC2021` are named less.)

**First (R1-R2):** Haskell 2010 and `GHC2021`, with every extension above. **Then (R5):** what the
other boot libraries add: `OverloadedStrings` (33 modules), `PackageImports` (8), `TupleSections`
(8), `PartialTypeSignatures`, `DuplicateRecordFields`, `DeriveLift`, `TemplateHaskell`.
**Later, by the testsuite (behaviour level):** `Arrows`, `TemplateHaskell` splices and
`QuasiQuotes` at large, `OverloadedLabels`, `OverloadedRecordDot`, `OverloadedRecordUpdate`,
`RebindableSyntax`, `QualifiedDo`, `MonadComprehensions`, `ParallelListComp`,
`TransformListComp`, `StaticPointers`, `RequiredTypeArguments`, `TypeAbstractions`,
`OrPatterns`, `MultilineStrings`, `ExtendedLiterals`, `HexFloatLiterals`, `NumericUnderscores`,
`LexicalNegation`, `UnicodeSyntax`, `ExplicitLevelImports`. All of them are specified above; the
order is only the order of testing.

### 10.3 GHC 9.14.1's constructors

The constructors of the parsed tree (E4, listed by the helper's `Data` instances) and where this
spec writes each. The `X...` constructors are GHC's extension points; the converter reports one if
it meets it.

| type | constructors | spec |
|---|---|---|
| `HsModule` | `HsModule` | §2 |
| `HsDecl` | `TyClD InstD DerivD ValD SigD KindSigD DefD ForD WarningD AnnD RuleD SpliceD DocD RoleAnnotD` | §4 |
| `HsBindLR` | `FunBind PatBind VarBind PatSynBind` (`VarBind` is not made by the parser) | §4.2 |
| `Sig` | `TypeSig PatSynSig ClassOpSig FixSig InlineSig SpecSig SpecSigE SpecInstSig MinimalSig SCCFunSig CompleteMatchSig` | §4.2, §4.7, §4.11 |
| `TyClDecl`, `InstDecl`, `ConDecl` | `FamDecl SynDecl DataDecl ClassDecl`; `ClsInstD DataFamInstD TyFamInstD`; `ConDeclGADT ConDeclH98` | §4.3-§4.5 |
| `HsExpr` (39) | `HsVar HsOverLabel HsIPVar HsOverLit HsLit HsLam HsApp HsAppType OpApp NegApp HsPar SectionL SectionR ExplicitTuple ExplicitSum HsCase HsIf HsMultiIf HsLet HsDo ExplicitList RecordCon RecordUpd HsGetField HsProjection ExprWithTySig ArithSeq HsTypedBracket HsUntypedBracket HsTypedSplice HsUntypedSplice HsProc HsStatic HsPragE HsEmbTy HsHole HsForAll HsQual HsFunArr` | §5.1, §5.2 |
| `HsCmd` | `HsCmdArrApp HsCmdArrForm HsCmdApp HsCmdLam HsCmdPar HsCmdCase HsCmdIf HsCmdLet HsCmdDo` | §5.5 |
| `StmtLR` | `LastStmt BindStmt BodyStmt LetStmt ParStmt TransStmt RecStmt` | §5.1 |
| `Pat` (19) | `WildPat VarPat LazyPat AsPat ParPat BangPat ListPat TuplePat OrPat SumPat ConPat ViewPat SplicePat LitPat NPat NPlusKPat SigPat EmbTyPat InvisPat` | §5.3 |
| `HsType` (20) | `HsForAllTy HsQualTy HsTyVar HsAppTy HsAppKindTy HsFunTy HsListTy HsTupleTy HsSumTy HsOpTy HsParTy HsIParamTy HsStarTy HsKindSig HsSpliceTy HsDocTy HsExplicitListTy HsExplicitTupleTy HsTyLit HsWildCardTy` | §6 |
| `HsLit` (18), `OverLitVal` | `HsChar HsCharPrim HsString HsMultilineString HsStringPrim HsInt HsIntPrim HsWordPrim HsInt8Prim HsInt16Prim HsInt32Prim HsInt64Prim HsWord8Prim HsWord16Prim HsWord32Prim HsWord64Prim HsFloatPrim HsDoublePrim`; `HsIntegral HsFractional HsIsString` (`HsInt` is not made by the parser) | §3.5 |
| `IE` | `IEVar IEThingAbs IEThingAll IEThingWith IEModuleContents IEGroup IEDoc IEDocNamed` | §2.5 |
| `HsUntypedSplice`, `HsQuote` | `HsUntypedSpliceExpr HsQuasiQuote`; `ExpBr PatBr DecBrL DecBrG TypBr VarBr` (`DecBrG` is the renamer's) | §5.1, §5.5 |
| `HsLocalBindsLR` | `HsValBinds HsIPBinds EmptyLocalBinds` | §4.2 |
| `ForeignDecl` | `ForeignImport ForeignExport` | §4.9 |
| `HsTyVarBndr`, `HsForAllTelescope` | `HsTvb`; `HsForAllVis HsForAllInvis` | §4.3, §6 |
| `HsDocString`, `HsDocStringDecorator` | `MultiLineDocString NestedDocString GeneratedDocString`; `HsDocStringNext HsDocStringPrevious HsDocStringNamed HsDocStringGroup` | §4.12 |

Not in 9.14.1 (ghc-lisp's 10.1 has them): `HsQualLit`, the expression `HsStar`, `QualLitPat`,
`ModifiedPat`, `IEWholeNamespace`; `PrefixCon` takes one argument in 9.14.1 and two in 10.1 (the
survey's E3).

### 10.4 As built: R1 (2026-10-17)

[HASKELL-NOTES.md](HASKELL-NOTES.md) records the build; what it changes in this text:

- **The printer's braces layout (§7.4, S14)** keeps two blocks in layout, the module's body (items
  at column 1) and a class's body (column 3), with the lines inside them indented: GHC's Haddock
  pass restricts the documentation comments a block takes by column only under virtual braces, so
  with explicit braces a `-- |` after a class went into its body and a top-level `-- ^` after a
  signature into its type. Every other block is braced.
- **The lines (§7.1, §8.2, §9.2 N1)**: every form that stands for a located node carries its start
  line, and `hsdump lines` lists the start line of every located node, compared position by
  position (not only the nodes that begin a line). Metadata for the tokens the forms have no
  element for: `:hs/eq` (the `=` or `->` of an unguarded right-hand side), `:hs/brace` (a block's or
  a record's `{`, a pattern synonym's `where`), `:hs/paren` (the `(` of implicit parentheses on
  another line), `^:hs/braces` (S12, for an empty `case` written `{}`); `:hs/breaks` entries are
  always `[index line]`, also for a list's head.
- **The tree level (§9.2 N3)** makes every `SourceText` `NoSourceText` before `showAstData`, whose
  `numericLit` prints the source text of `HsIntPrim` and `HsWordPrim` literals.
- **Forms the text does not give**: tuple and sum syntax in constructor declarations
  (`(:tuple a b)`, `(:usum a :_)`); `(:doc-type T (:doc ...))` for documentation on a type no form
  has an element for (none in the corpus); an infix left-hand side with constructor-chain operands
  as one `:infix` (ghc-lisp S14); a `foreign import`'s `safe` kept when written.
- **Commands (§9.6)**: `bin/repr-haskell helper`, `corpus fetch|list`, `dump`, `read`, `print`,
  `roundtrip`, `test` as sketched; the interface level is R2's (§10.5).

### 10.5 As built: R2 (2026-10-17)

[HASKELL-NOTES.md](HASKELL-NOTES.md), "R2", records the build; what it changes in this text:

- **The command (§9.6)**: the interface level is `bin/repr-haskell roundtrip --iface` (the tree
  level, then the interface level over the modules it printed; `--iface-only` over those of an
  earlier run), not a separate `check --levels` verb; its known differences are
  `test/haskell/roundtrip-ghc-9.14.1-iface-known.edn` (§9.5: one file per corpus and level).
- **How a package is compiled (§9.3)**: `ghc --make` from inside a copy of the package's tree (the
  same relative paths on both sides), with the flags the helper parses with (language,
  extensions, `-haddock`, `-this-unit-id`, CPP's `-D` and `-I`), `-i` of the modules' source
  root, and the corpus's `:iface :opt`, `-O1` (HR7). `ghc-internal` is compiled as its own unit
  against `rts` alone; `base`, `ghc-prim` and `ghc-bignum` against the host's installed
  `ghc-internal`. The modules R1 does not convert are the same on both sides: the `.hsc` modules
  through the host's `hsc2hs`, the boot files as they are, and `GHC.Internal.Prim` and
  `GHC.Internal.PrimopWrappers` generated as hadrian generates them (genprimopcode, built from
  the tarball's `utils/genprimopcode`, over `compiler/GHC/Builtin/primops.txt.pp` through `cc -E
  -undef -traditional -P`). The printed side has every converted module of the package in its
  place at once, one build per side (HR8), not one printed module at a time.
- **Mode `:lines`'s normalizations (§9.3)**, as built and adopted into §9.3 (HR6): besides the three dropped lines and
  the spans in unfoldings' strings, (1) the comparison is of the texts' tokens, not their lines:
  `--show-iface`'s pretty printer breaks lines by width, and a column's digits change the width;
  (2) *every* fingerprint is blanked, the declarations' own hashes too (they cover their
  unfoldings); (3) a `SrcLoc` construction's two columns are blanked through its wrapper
  (`$WSrcLoc`), boxed arguments (`(I# 7#)`) and let-bound ones (a stable unfolding of an `INLINE`
  function binds the call stack's parts, `let { $dIP5 :: Int = I# 47# } in`, before
  `$WSrcLoc ... $dIP5 ...`); (4) the usages of the files CPP included (`addDependentFile`) are
  dropped: the printed module is the preprocessed text (§2.8), which includes none, and the
  forms record those files in `:cpp`'s `:files`.
- **Results**: all 483 modules' interfaces equal (1,072,106 lines of `--show-iface` text); with
  mode `:full`'s normalization alone 237 are equal and 246 differ, the differences being columns
  and the hashes over them, and CPP's inclusions. No known differences. About 2.5 minutes on this
  host beyond the tree level, the two builds of the corpus at `-O1` 116 s.

### 10.6 As built: R3 (2026-10-17)

[HASKELL-NOTES.md](HASKELL-NOTES.md), "R3", records the build; what it changes in this text
(HR9-HR13, built as recommended, for the user's decision):

- **The helper (§8.2)**: `dump --full` writes the exact-print annotations as §8.2 says, each as a
  discarded form (`#_`) after the `nil` a dump without `--full` has in its place, so that a dump of
  either mode reads as the same tree; then `(hsdump/text "...")`, the text the positions refer to
  (the source, or a CPP module's module view, §2.8), and `(hsdump/tokens [...])`, GHC's lexer's
  tokens (`lexTokenStream`, the raw token stream `-dkeep-comments` keeps: comments marked `:c`,
  the layout's virtual braces and semicolons and other files' tokens left out). The converter's
  positions come from these tokens, not from the annotations (HR9). A CPP module's module view is
  also written as `FILE.view`, the text level's reference.
- **The metadata of mode `:full` (§7.2)**, as built (HR10): `:hs/pos {:span [l1 c1 l2 c2] :toks
  [[l c]...]}` on a list, vector or symbol that stands for a located node, its span and the start of
  each token inside it and inside none of its elements' spans, in order (a literal's and a
  keyword's tokens are their parent's; parentheses are tokens like any other, so no `:hs/paren`
  and no `:hs/apos`); `:hs/punct [[l c ","]...]`, likewise, the commas, semicolons and braces,
  which the printer never prints in the full layout (so no `:hs/layout`: a layout block's
  columns are its tokens' positions, explicit braces and semicolons are punctuation); `:hs/text
  {[l c] "text"}`, likewise, the source's spelling of a token the printer spells otherwise, a
  literal's, a pragma opener's, Unicode syntax's (in place of `:hs/atext`, `:hs/text` on a literal
  form and `:hs/unicode`); on `hs/module` `:hs/comments [[l1 c1 l2 c2 "text"]...]`, `:hs/space [[l c
  "ws"]...]` (the white space after the element ending at `l c` when it is not the full layout's
  own: spaces to the next element on its line, else newlines and spaces to its column) and
  `:hs/tail`, and the tokens no form's span holds; and two facts GHC keeps only in annotations:
  `:hs/where` (a class, instance or data declaration without items written with `where`) and
  `:hs/parens N` (the parentheses around a family equation's left-hand side).
- **The full layout (§7.4, 3)**: the printing code of the braces layout, collecting its tokens,
  which are paired in order with the forms' positions; the converter checks the pairing against
  the source's tokens and fails a module whose tokens differ other than in spelling (HR11).
- **The levels (§9.1, §9.3, §9.5, §9.6)**: `bin/repr-haskell roundtrip --text` (after the tree
  level, and the interface level with `--iface`; `--text-only` alone): byte for byte against the
  original or the module view, then `hsdump tree --full` and `lines` of both sides; known
  differences `test/haskell/roundtrip-ghc-9.14.1-text-known.edn`: the three modules that include
  `CTypes.h`, whose comments are in the original's tree and outside the module view, differ in the
  tree of mode `:full` only (HR12). `bin/gate --full`'s step runs `roundtrip --iface --text`, about
  5 minutes. `print` takes `--layout braces|full` (the full layout by default for forms of mode
  `:full`), `read` and `dump` `--full --corpus`.
- **The interface level (§9.3)**: both modes sort the entries of the deprecation warnings' lists
  of `--show-iface`, whose order GHC varies from build to build (HR13).

### 10.7 As built: R4 (2026-10-17)

[HASKELL-NOTES.md](HASKELL-NOTES.md), "R4", records the build; what it changes in this text
(HR14-HR16, built as recommended, for the user's decision):

- **The layout layout (§7.4, 2)**, `print --layout layout`: the braces layout's printing code, every
  block (`do`, `mdo`, `rec`, `let`, `where`, `of`, `\case`, `\cases`, a class's or an instance's
  body, a GADT's constructors, a closed family's equations, a multi-way `if`'s guards, which GHC
  lays out without semicolons) a layout block. Its first item on the keyword's line, the block's
  column that item's; or, when the item's line is later, on a line of its own, the column 2 further
  than both the line the keyword is on and the enclosing item's continuation lines, so that those
  lines close it. Every later item at the column on its line, after a `;` when it shares the
  previous item's line; the lines inside an item 2 further than its column. The module's items are
  at column 1, their continuation lines at 3, so a top-level `where`, `do` or class body has its
  items at 5. A block ends where the next line begins left of its column, or at a token GHC's
  parse-error(t) rule closes it on (`)`, `]`, `,`, `then`, `else`, `of`, `in`, `#)`,
  `|]`, `||]`); a block that any other token on the line of its end would continue is braced
  after the fact (a `{` before its first item, `;` before its items that begin lines, `}` after the
  last), and so is one a line would continue (a safety net: the columns as built never need it).
  None of the corpus's 4,628 blocks is braced so.
- **Explicit braces (§7.2, S12)**: a block whose `{` the source wrote on another line than its first
  item (`:hs/brace`, HR3) keeps its braces there, since the located list of its items begins at the
  `{` (16 blocks in the corpus); `^:hs/braces` stays as built, on an empty `case` only: the
  converter does not mark the other braced blocks, and the forms of mode `:lines` are unchanged
  (HR15).
- **The style**: separators without a position of their own lead the line of the form after them
  when it begins a later line (`,`, `=` and `|` of constructors, `->` of types, `::`, `=>`); `then`,
  `else` and `in` go to the line of their expression, at the `if`'s column + 2 or the `let`'s
  column; a `where` of bindings on a line of its own before them when the source left one, else on
  the first binding's line; no space after `(` and `[` or before `)`, `]`, `,`, `;` (but where it
  would make another token: `(#`, `(|`, `[|`, `#)`, `|)`, `|]`), none in records' braces and after a
  lambda's `\` before a name (HR14). Ordinary comments are not in the forms of mode `:lines` (§7.3)
  and not printed; documentation comments are, items' at the block's column.
- **The levels (§9.1, §9.5, §9.6)**: `bin/repr-haskell roundtrip --layout` (after the braces
  layout's tree level, over the same forms; `--layout-only` alone), with `--iface` the interfaces
  of both layouts against the originals', each package's three sides compiled concurrently; known
  differences `test/haskell/roundtrip-ghc-9.14.1-layout-known.edn` and
  `-layout-iface-known.edn`, none. `bin/gate --full`'s step runs `roundtrip --iface --text --layout`
  (HR16). `bin/repr-haskell test`'s step 6 checks the style on the worked example and
  `test/haskell/layout/Layout.hs` against `test/haskell/layout/expected/`.

### 10.8 As built: R5 (2026-10-17)

[HASKELL-NOTES.md](HASKELL-NOTES.md), "R5", records the build; what it changes in this text
(HR17-HR20, built as recommended, for the user's decision):

- **The corpus (§9.4)**: the other 37 packages of the tarball's `libraries/` that the host's GHC
  installs, in dependency order after R1's four (HR17): `array`, `deepseq`, `pretty`,
  `ghc-boot-th`, `template-haskell`, `bytestring`, `containers`, `binary`, `transformers`, `mtl`,
  `stm`, `exceptions`, `os-string`, `filepath`, `time`, `unix`, `file-io`, `directory`,
  `ghc-platform`, `ghc-boot`, `ghc-compact`, `ghc-experimental`, `ghc-heap`, `ghci`, `process`,
  `terminfo`, `haskeline`, `hpc`, `integer-gmp`, `text`, `parsec`, `semaphore-compat`,
  `template-haskell-lift`, `template-haskell-quasiquoter`, `xhtml`, `Cabal-syntax`, `Cabal`:
  743 modules converted (261 with CPP), 64 `.hsc` modules and one boot file left for R6, three
  generated modules. Not the compiler (`compiler/`, the `ghc` library), `utils/`'s packages
  (`ghc-toolchain`, `haddock-api`, `haddock-library`), `Win32`, nor the test and benchmark
  packages. Each package's configuration is its `.cabal` library's as Cabal configures it for this
  platform with hadrian's flags (HR19): `default-language` (Haskell98 when none, as Cabal),
  `default-extensions`, `cpp-options`, `include-dirs`, `build-depends` as `-package` (compiled
  against the installed packages, as R2), the Cabal macros a module needs as `-D`, the headers a
  package's `configure` script makes made in the extracted tree, the generated modules
  (`GHC.Version`, `GHC.Platform.Host`, `Paths_Cabal`) committed as hadrian and Cabal make them.
- **Another file's nodes (§2.8, §7.1, §8.2)** (HR18): 12 modules' nodes are in another file, 11
  whose text a CPP `#include` of Haskell source brings (`filepath`'s `System.FilePath.Posix` is
  `#include "Internal.hs"`) and alex's output `Distribution.Fields.Lexer`, whose `{-# LINE #-}`
  pragmas name `Lexer.x`'s lines. In mode `:lines` a form whose node is in another file than the
  located form before it (for a top-level form: the top-level form before it) carries
  `:hs/file "file"`, the file's name as the dump's span names it, and the module's own `:file`
  when it comes back; the printer puts it there with `{-# LINE n "file" #-}`, and ends the text in
  the module's file. Mode `:full` does not convert them: the module view (S5) holds only the
  module's own lines, and the helper's tokens leave other files' out (12 known differences of the
  text level).
- **The interface level (§9.3)**: mode `:lines` writes a file name `./F` as `F`: `ghc --make`
  names a package's files under `-i.` `./F`, the helper's parse and the `{-# LINE #-}` pragmas
  `F`.
- **Forms and printing found on the way**: the variable `quote` applied is `((:name "quote") x)`
  (`(quote x)` reads as the name quote `'x`); `(:op "t" ...)` only when its first operator stands on
  its first operand's last line, else `:infix`; the line of a declaration's context (its `(`), of a
  pragma's `{-#` before its warning's names, of a GADT signature's `forall`; an operator beginning
  with `?` that is not an implicit parameter (`?~`); in a CPP module, a printed line ending in `\`
  ends in an empty comment (`infixl 9 \\ --`), since CPP would join it with the next; a later
  constructor's documentation after its `|` (after a `-- ^` comment, a `-- |` before the `|` would
  continue it); a form placed after its line (a module's documentation before its `LANGUAGE`
  pragma) leaves the next form on its line, and the lists without a line inside such a form get the
  Haskell line explicitly. R1-R4's results are unchanged; of their forms, two files' texts changed
  (the same data, `ghc-internal`'s documentation before a pragma), and three printed modules (the
  `|` before a constructor's documentation).
- **The levels (§9.6)**: the trees of both sides are parsed as one job list over the corpus, the
  packages compiled four at a time at the interface level (`REPR_HASKELL_PACKAGES`), the interfaces
  shown in one job list; `bin/gate --full`'s step over the whole corpus, about 10 minutes (HR20).

## 11. Decisions and open questions

### 11.1 Decisions taken while writing (S1-S36)

Each on the terms of H1-H14; the user may revise any (§11.2 asks about the ones with real
alternatives).

| S | decision | reason |
|---|---|---|
| S1 | The forms are `GhcPs` only: no fixity resolution, no scopes, no types (§1.2, 1) | every level is judged by GHC from the parsed tree; a bridge needs no meaning (H1, ghc-lisp I2) |
| S2 | A mechanical map between `.hsl` and Haskell forms for the constructors both GHCs share (§1.3) | H2: ghc-lisp's grammar respelled, not a new one |
| S3 | Namespaces `hs.` + module name; lower-case segments for the bridge's own (`hs.main.PATH`, `.boot`, `.sig`) (§2.1) | module segments are upper case, so no clash |
| S4 | CPP modules converted, post-CPP, one configuration recorded in `:cpp` (§2.8) | H9; 15% of library modules use CPP |
| S5 | The text level of a CPP module is against its module view (§2.8) | the `#if` structure is not in the tree |
| S6 | The name rule (§3.2), `(:name "t")` with ghc-lisp S2's meaning, `(:op "t" ...)` for chains | H3; one escape, checked by reading back; keeps `((:name "t") a b)` as the prefix application |
| S7 | Fractional literals as doubles only when the shortest text has the source's significand and exponent, else BigDecimals of exactly them; hex floats `(:hex-float s e)` (§3.5) | the tree compares `FL`'s fields; no normalization needed beyond `SourceText` |
| S8 | Conversions with `-haddock` (§4.12) | the boot libraries' interfaces carry docs (E3) |
| S9 | Documentation as data, `(hs/doc ...)`, `(:doc ...)`, transparent elements (§4.12) | the reader drops `;` comments |
| S10 | Template Haskell and arrows as data, splices never run by the conversion (§5.5) | the parsed tree holds them; ghc-lisp D16 |
| S11 | `:full`'s position keys: `:span` plus tokens by name, a per-constructor table kept with the GHC version (§7.2) | GHC's annotation types change across releases |
| S12 | `^:hs/braces` in `:lines` as a layout hint (§7.2) | the layout printer can keep the source's choice at no cost to the tree level |
| S13 | Columns as GHC counts them (code points, tab stops of 8); tabs in `:hs/space` (§7.2) | GHC's lexer |
| S14 | The braces layout as the printer's first milestone, `{-# LINE #-}` for overflow (§7.4); the module's and the classes' bodies in layout (HR2) | H6 (c) first; GHC's tree equal after `eraseEpLayout`; GHC's Haddock pass |
| S15 | One helper with verbs `dump`, `tree`, `lines`; no comparator program (§8.2) | the levels compare text |
| S16 | `::` is `:sig` inside, `hs/sig` at top level (§3.4) | one word for every use of `::`; HS1 |
| S17 | `\` is `:lambda`, `\case` `:lambda-case`, `\cases` `:lambda-cases` (§3.4) | keyword heads never collide with names (ghc-lisp D1) |
| S18 | `:` is `:cons` everywhere, `':cons` promoted (§3.3) | `:` is no token; `:cons` is a keyword head, not a constructor operator |
| S19 | `~` as `:lazy` (patterns, fields), `(:op "~" a b)` (equality), `[:before n]`, `[:never]` (activations) (§3.4) | `~` is unquote |
| S20 | `@T` kept, read as `(arbace.core/deref T)` (§3.4) | as Go forms' `@p`; unambiguous |
| S21 | Characters `\c`, `(:char n)` for surrogates and beyond the BMP (§3.5) | the reader refuses `\uD800` and characters beyond the BMP |
| S22 | Strings as code points, `(:string ...)` when a surrogate occurs, `(:multiline "v")` (§3.5) | a surrogate pair would read as one code point |
| S23 | Integers in the source's radix where the reader has it, decimals without leading zeros, `(:neg-literal 0)` (§3.5) | `010` reads as 8; `-0` as 0 |
| S24 | Primitive literals `(:int# n)` ... `(:word64# n)`, `(:string# s)` (§3.5) | `#` dispatches |
| S25 | Labels `(:label "x")` (§3.4) | idem |
| S26 | Header pragmas as top-level `hs/language`, `hs/options-ghc`, `hs/options-haddock`, `hs/options` forms (§2.3) | Haskell's order (I1); ghc-lisp D10 |
| S27 | `hs/module` always present, `nil` for a headerless module, options for the file, kind, positions, configuration and preprocessing (§2.4) | the record needs a home |
| S28 | Imports as top-level `hs/import` forms (§2.6) | ghc-lisp S5; one form per line in `:lines` |
| S29 | Top-level naked splices as `(hs/naked-splice e)` (§4.1) | with qualified heads, a bare top-level form cannot be told apart |
| S30 | `:config` keys (§2.7): `:ghc`, `:ghc-unit`, `:target`, `:os`, `:arch`, `:language`, `:extensions`, `:flags`, `:unit`, `:corpus` | H9, H11 |
| S31 | The tree level normalizes in `:lines` only spans (lines compared apart), annotations and source texts (§9.2) | the fewest normalizations that `:lines` needs |
| S32 | The interface level drops `src hash`, `interface hash`, `flags: fingerprint`; in `:lines` also spans' columns in unfoldings and the hashes over them (§9.3); as built (HR6): `SrcLoc` columns through wrappers, boxing and lets, every fingerprint, CPP's `addDependentFile` usages, tokens for lines | measured (E3, E5) |
| S33 | Converted forms not tracked (§2.1) | as Go forms' decision 9; HS11 |
| S34 | The helper written anew, GHC's `check-ppr` and `check-exact` its models (§8.1) | nothing to vendor beyond a few lines; HS8 |
| S35 | `.hsc` converted from hsc2hs's output, recorded like CPP (§8.5) | hsc2hs is not GHC's driver's; HS5 |
| S36 | One-element lists stay errors (ghc-lisp S3); `(:name "t")` and `(:lazy p)` are two-element lists, so no form of this spec is one | ghc-lisp's I4 unchanged |

### 11.2 Open questions for the user (HS1-HS12)

Each with the recommendation this text follows.

- **HS1 The words for `::` and `\`.** (a) `:sig`/`hs/sig`, `:lambda`, `:lambda-case`,
  `:lambda-cases` (the survey's sketch); (b) `:has-type`, `:fn`; (c) `:t`, `:\`-like short words
  (`:\` does not read: E1). *Recommended: (a)*: `:sig` names the construct for every use of `::`,
  and `:lambda-case` the extension.
- **HS2 Integer radix.** (a) Kept where the reader has it (`0x1F`, `017`, `2r101`), as Go forms;
  (b) always decimal in `:lines`. *Recommended: (a)*; the value is what counts either way.
- **HS3 Fractional literals.** (a) S7's exact significand and exponent (`1E+3M` for `1e3`), no
  normalization; (b) Go forms' value rule (`1000.0`) with `FL` compared by value at the tree level.
  *Recommended: (a)*: a stricter tree level for a few `M` suffixes (4 `HsFractional` literals in
  445 modules of `base`, E2).
- **HS4 Where the corpus comes from.** (a) GHC 9.14.1's source tarball, fetched once and pinned
  by hash in `test/haskell/` (all boot libraries at the pinned versions, 33,970,212 bytes); (b) one
  Hackage tarball per package; (c) the tarball vendored into the repository. *Recommended: (a).*
- **HS5 `.hsc` and `.lhs`.** (a) Both in R6: `.hsc` from hsc2hs's output recorded like CPP,
  `.lhs` through GHC's `unlit` with the literate text kept in `:full` (on `hs/module`); (b) skipped
  (known differences). *Recommended: (a).*
- **HS6 Resolved fixities.** (a) None: the renamer's (S1); (b) an optional annotation of resolved
  chains (`:hs/fixity`) from a renamer pass of the helper, for consumers. *Recommended: (a)* now;
  (b) when a consumer needs it.
- **HS7 Forms written by hand.** (a) The printer may emit `{-# LINE N "file.ae" #-}` so GHC's
  positions name the forms file, as Go's `//line`; (b) not. *Recommended: (a)*, as an option, not
  used by the round trip.
- **HS8 Licensing of the helper.** (a) Written anew, GHC's `check-ppr` (`eraseEpLayout`, about
  20 lines of SYB) as a model; (b) vendor those lines with GHC's BSD-3 in LICENSE.md.
  *Recommended: (a).*
- **HS9 GHC's pin.** (a) Alpine's `ghc` with the version file and checks of §8.4; a GHC bindist
  pinned by hash only if Alpine moves; (b) a pinned bindist now. *Recommended: (a)* (H11).
- **HS10 Main modules' namespaces.** (a) `hs.main.` + the corpus path (§2.1); (b) `hs.Main.` +
  path. *Recommended: (a)*: a lower-case segment cannot be a module's.
- **HS11 Held forms.** (a) Converted forms not tracked until a module is held by a recorded
  decision (as Go forms); (b) commit `base`'s forms now as a corpus. *Recommended: (a).*
- **HS12 Docs on.** (a) `-haddock` on by default, documentation as data (S8, S9); (b) off,
  documentation as ordinary comments. *Recommended: (a)*: the interfaces carry it.

## 12. Sources

- Arbace: [REPR-HASKELL-SURVEY.md](../REPR-HASKELL-SURVEY.md) (the decisions H1-H14, E1-E3),
  [go/SPEC.md](../go/SPEC.md) (principles, §3, §4, §8.1, §10, §12), [go/PRINTER-NOTES.md](../go/PRINTER-NOTES.md)
  ("Mode :full as built"), [go/ROUNDTRIP.md](../go/ROUNDTRIP.md), [NAMING-SURVEY.md](../NAMING-SURVEY.md),
  [ALPINE.md](../ALPINE.md); main at `f0d7fc2f`.
- ghc-lisp, `/root/ghc-lisp` (github.com/arbace/ghc-lisp), branch `ghc-lisp`, commit
  `db7adf93a4d5076a29d1a1d45295bfc38e615923` (2026-10-03), read only: `ghclisp/SPEC.md` (D1-D16,
  I1-I6, S1-S19, §1-§12), `ghclisp/DESIGN.md`, `ghclisp/README.md`,
  `compiler/GHC/Parser/Lisp.hs`, `compiler/GHC/Parser/Lisp/{Reader,Printer,Parens}.hs`.
- GHC 9.14.1: Alpine's `ghc-9.14.1-r2` (`/usr/lib/ghc-9.14.1`, the `ghc` library
  `ghc-9.14.1-f453`); its source distribution
  https://downloads.haskell.org/ghc/9.14.1/ghc-9.14.1-src.tar.xz (SHA-256 above):
  `libraries/`, `utils/check-ppr/Main.hs` (`eraseEpLayout`, `showAstData BlankSrcSpan
  BlankEpAnnotations`), `utils/check-exact/`.
- The Haskell 2010 report (layout, algorithm L; lexical syntax, escapes):
  https://www.haskell.org/onlinereport/haskell2010/; GHC's user's guide for 9.14
  (extensions, `-dkeep-comments`, `MultilineStrings`, `ExtendedLiterals`):
  https://downloads.haskell.org/ghc/9.14.1/docs/users_guide/.
- ghc-exactprint: https://github.com/alanz/ghc-exactprint.

## Appendix A: the experiments

Scratch under `.tmp/repr-haskell-spec/` (not tracked); the scripts and inputs that matter are
committed in `test/haskell/`. Stage 0 is main's `target/seed/amd64/arbace` (scripts run as
`arbace FILE.ae ARGS`), GHC the host's 9.14.1.

**E1: Arbace's reader on Haskell's lexemes and the spec's spellings.**
`test/haskell/reader_samples.ae` reads each line of `test/haskell/reader-samples.txt` (328
samples in five groups) as the text of a vector and prints the forms read, with their classes, or
the reader's error; the output is `test/haskell/reader-samples.out`. The results the spec rests on:

| sample | read as | used in |
|---|---|---|
| `::`, `(:: f T)`, `:`, `(: x xs)`, `:+:`, `M.:+:`, `:*:`, `:::`, `.:`, `':`, `hs/::` | `Invalid token` | §3.2, §3.4 |
| `:|`, `:+`, `:.`, `:=`, `:/`, `:->`, `:%`, `:<>`, `:-`, `:..` | keywords | §3.1 |
| `:@`, `:\\` | `Invalid token: :` | §3.2 |
| `::+`, `::>` | `:user/+`, `:user/>` | §3.2 |
| `(\ x e)`, `\case` | `Unsupported character` | §3.4 |
| `'a'`; `'x'#` | `(quote a')`; `(quote x'#)` | §3.4 |
| `'\n'`, `'\''` | `Unsupported character` | §3.5 |
| `"\1234"` | `"S4"` (two characters) | §3.5 |
| `"a\&b"`, `"\SOH"`, `"\^A"`, `"\x41"`, `"\o101"`, gaps, `"\DEL"` | `Unsupported escape character` | §3.5 |
| `0o17`, `0b101`, `1_000_000`, `0x1.8p1` | `Invalid number` | §3.5 |
| `1e3`, `0x1F`, `007`, `-0`, `-0.0` | `1000.0`, `31`, `7`, `0`, `-0.0` | §3.5 |
| `010`, `08` | `8`; `Invalid number: 08` (a separate `-e` run) | §3.5 |
| `3#`, `3##`, `"x"#`, `1.5#`, `123#Int8`, `#foo`, `#.`, `#`, `##`, `^`, `^^`, `x^y`, `.~`, `%~`, `&~`, `<~`, `=~`, `~`, `@`, `` ` `` | `Unmatched delimiter` (a reader macro took the closing `]`) | §3.2, §3.4 |
| `~~>`, `(~ p)`, `~p` | `(unquote (unquote >))`, `((unquote p))`, `(unquote p)` | §3.4 |
| `@Int`, `@_`, `(f @T x)` | `(arbace.core/deref Int)`, ... | §3.4 |
| `x@y`, `<@>` | two forms each | §3.2, §3.4 |
| `//`, `/=`, `M./` | `Invalid token` | §3.2 |
| `/`, `M.//`, `</>`, `<//>`, `M.</>` | `/`; symbols with namespaces `M.`, `<`, `<`, `M.<` | §3.2 |
| `\\` | the character `\` | §3.2 |
| `true`, `false`, `nil` | literals | §3.2 |
| `!`, `!!`, `$`, `$!`, `<$>`, `>>=`, `.`, `..`, `Prelude..`, `M.!`, `|`, `||`, `->`, `<-`, `=>`, `=`, `==`, `<=`, `>=`, `&&`, `&`, `+#`, `==#`, `*##`, `x#`, `Int#`, `?x`, `foldl'`, `x''`, `_`, `M.:|`, `∘`, `→`, `∷`, `∀`, `λ`, `x.y`, `A.B.c`, `-->`, `?`, `%1`, `hs/=`, `hs/sig`, `hs/naked-splice` | symbols, as written | §3.1, §4.1 |
| `'Just`, `''T`, `'[]`, `'[a b]`, `'(:tuple a b)`, `':cons` | `(quote ...)` | §3.3, §3.4 |
| `\a`, `\'`, `\"`, `\\`, `\newline`, `\space`, `\tab`, `\u0000`, `\u007f`, `\é`, `￿`, `\(`, `\;`, `\,`, `\#`, `\@`, `\~`, `\^`, `\u`, `\o`, `\n`, `\s` | characters (`\n` is the letter n) | §3.5 |
| `\uD800`, `\uDFFF`, `\😀` | `Invalid character constant`, `Unsupported character` | §3.5 |
| `"\uD800"`; `"😀"`, `"😀"` | one lone surrogate; two UTF-16 units, one code point, alike | §3.5 |
| `1E+400M`, `1E+1000000000M`, `1/4`, `3M`, `0.1000...625M`, `18446744073709551616`, `2r101`, `017` | BigDecimals, a ratio, a BigInt, `5`, `15` | §3.5 |
| every form of §3.4's right column (`(:sig f T)`, `(:lambda ...)`, `(:cons x xs)`, `(:name ":+:")`, `(:op "/=" n 0)`, `(:int# 3)` ... `(:word64# 18446744073709551615)`, `(:label "foo")`, `(:lazy p)`, `[:before 2]`, `[:never]`, `(:neg-literal 0)`, `(:hex-float 0x18 -3)`, `(:multiline ...)`, `(:char 0x1F600)`, `(:string "a" 0xD800 "b")`, `(:doc :next " text")`, metadata maps) | one form each, as written | §3, §4, §7 |

**E2: the name rule over `base`.** The survey's helper (its Appendix A, built in
`.tmp/repr-haskell-survey/hsdump/`) dumped the 535 `.hs` files of 9.14.1's `libraries/base` and
`libraries/ghc-internal` (tests excluded) in 3.0 s (16 jobs): 445 dumped, 85 failed to parse without
CPP, 5 for other reasons. The distinct `RdrName`s of the dumps (`(rdr "...")`), counted, went
through `test/haskell/name_rule.ae` (the rule of §3.2): 6,995 names, 100,327 occurrences; bare
6,900 (98,511); built-in syntax 67 (1,465); respelled 28 (351), listed in §3.2. Literal
constructors in the dumps: `HsChar` 9,355 (Unicode tables), `HsIntegral` 1,688, `HsString` 924,
`HsIntPrim` 224, `HsStringPrim` 205, `HsCharPrim` 11, `HsWordPrim` 7, `HsFractional` 5.

**E3: GHC's facts.** With `ghc -fno-code -ddump-parsed-ast` on small modules (scratch
`.tmp/repr-haskell-spec/e3/`):

- literals: `-0` under `NegativeLiterals` is `IL (SourceText -0) True 0`; `0x1.8p1` is `FL` with
  significand 24/1 (base 2); `1e3` significand 1/1, exponent 3; `-1.5` significand -15/1;
  `"a\&b\SOH\^A\x41\o101\1234\   \c"` is `HsString` with the value `"ab\SOH\SOHAA\1234c"`;
  `"\xD800"` is accepted (value `"\55296"`); `123#Int8` is `HsInt8Prim`; a `"""` block and
  `"""hello\n  world"""` have the same value `"hello\n  world"`;
- type operator chains nest to the right (`Int :+ Bool :* Char`);
- GHC 9.14.1's default language is `GHC2021` (`\case` without `LambdaCase` is refused);
- comments are in the tree only with `-dkeep-comments` (`EpaComments` with `EpaLineComment`);
- CPP (`ghc -E`): the output starts with `{-# LINE 1 "C.hs" #-}` and line markers, keeps the
  module's line numbers with blank lines and `# 4 "C.hs" 2` after an `#include`, and expands
  `__GLASGOW_HASKELL__` to `914`; GHC runs `cc -E -undef -traditional` with `-I` for
  `ghc-internal`'s and the RTS's include directories, `-include ghcversion.h`, the platform's
  `-D` flags and a generated header of `MIN_VERSION_` macros;
- interfaces: a module and the same module in explicit braces on three lines compile (`-O`) to
  interfaces whose `--show-iface` differ only in `interface hash` and `src hash` (the ABI hash
  equal); a comment added changes the same two lines; recompiling in place gives a byte-identical
  `.hi`; an `INLINE` function whose equation moves by two columns changes its unfolding's string
  (`"N.hs:3:1-14|function g"#` against `3:1-16`) and the ABI hash;
- the installed `base` interfaces carry documentation (`--show-iface` of `GHC/Maybe.hi`, "docs:").

**E4: the constructors of 9.14.1's parsed tree.** A 30-line program over `Data.Data`
(`dataTypeConstrs` of `HsDecl GhcPs`, `HsExpr GhcPs`, ...; scratch `e4/Constrs.hs`, built with
`ghc -package ghc`) printed §10.3's table. The package list of the host's GHC (`ghc-pkg list`):
`base-4.22.0.0`, `ghc-internal-9.1401.0`, `ghc-9.14.1` (hidden), `containers-0.8`,
`bytestring-0.12.2.0`, `text-2.1.3`, `filepath-1.5.4.0`, `directory-1.3.10.0`,
`process-1.6.26.1`, `Cabal-3.16.0.0` and the other boot packages.

**E5: `base` compiled by the host's GHC.** `ghc --make -O0 -haddock -this-unit-id base-4.22.0.0
-hide-all-packages -package ghc-internal -package ghc-prim -package ghc-bignum
-XNoImplicitPrelude -isrc -Iinclude` over the 242 modules of 9.14.1's `libraries/base/src` that are
not for Windows, JavaScript or WebAssembly: 236 interfaces in 9.5 s; the 6 failures are
Windows-only modules (`GHC.Conc.POSIX`, which imports Windows modules) and one Safe Haskell import
(`Foreign.C`), which the package's real module list and flags avoid. Built twice into two output
directories, all 236 `.hi` differ, and `--show-iface` shows why: `interface hash` and
`flags: fingerprint` (the output directory is among the flags) only.

**E6: extensions in the corpus.** The `LANGUAGE` pragmas of the modules of `base` and
`ghc-internal`, and of the other boot libraries, counted (§10.2).

**The worked example** (Appendix B) was checked twice: `ghc -fno-code` accepts
`test/haskell/sample/Shape.hs`, and `test/haskell/read_forms.ae test/haskell/sample/Shape.ae`
reads its 20 forms with a line-numbering reader, each at its Haskell line (1, 1, 2, 3, 10, 11, 12,
14, 16, 21, 22, 23, 25, 26, 28, 29, 36, 37, 43, 44), and finds no symbol the name rule would not
write bare.

## Appendix B: the worked example

`test/haskell/sample/Shape.hs` (50 lines; GHC 9.14.1 accepts it) and its Haskell forms in mode
`:lines`, `test/haskell/sample/Shape.ae`, written by hand by this spec, each form on its Haskell
line (blank lines omitted here).

```haskell
{-# LANGUAGE LambdaCase, BangPatterns, TypeApplications, MagicHash #-}
-- | Shapes: the spec's worked example (doc/haskell/SPEC.md, section 2).
module Data.Shape
  ( Shape (..)
  , area
  , (<+>)
  , classify
  ) where

import qualified Data.Map.Strict as M
import Data.List (sortBy, foldl')
import GHC.Exts (Int (I#), (+#))

infixl 6 <+>

data Shape
  = Circle Double
  | Rect { w, h :: !Double }
  deriving (Show, Eq)

area :: Shape -> Double
area (Circle r) = pi * r ^ 2
area Rect {w = x, h} = x * h

(<+>) :: Shape -> Shape -> Double
a <+> b = area a + area b

classify :: (Ord a, Num a) => a -> String
classify n
  | n < 0 = "negative"
  | n /= 0, n > 100 = big
  | otherwise = 's' : "mall\tor\1234zero"
  where
    big = "big"

tally :: [Shape] -> M.Map String Int
tally = foldl' (\m s -> M.insertWith (+) (name s) 1 m) M.empty
  where
    name = \case
      Circle _ -> "circle"
      Rect {} -> "rect"

main :: IO ()
main = do
  let xs@(~(first : _)) = [Circle 1.5, Rect 2 3]
      !n = length xs
  print (map area xs, n, first)
  print [area s | s <- xs, area s > 1e3]
  print (read @Int "42", I# (1# +# 2#), 0x1F, (`div` 2) 7, (subtract 1) 3)
  mapM_ print (sortBy (\a b -> compare (area a) (area b)) xs)
```

```clojure
(ns hs.Data.Shape (:require [arbace.haskell :as hs])) (hs/language LambdaCase BangPatterns TypeApplications MagicHash)
(hs/doc :next " Shapes: the spec's worked example (doc/haskell/SPEC.md, section 2).")
(hs/module Data.Shape
  [(Shape ..)
   area
   <+>
   classify]
  :file "Shape.hs" :positions :lines :config {:ghc "9.14.1" :os "linux" :arch "x86_64" :language GHC2021 :flags ["-haddock"]})
(hs/import qualified Data.Map.Strict as M)
(hs/import Data.List [sortBy foldl'])
(hs/import GHC.Exts [(Int I#) +#])
(hs/infixl 6 <+>)
(hs/data Shape
  (Circle Double)
  (Rect (:sig w h (! Double)))
  (deriving [Show Eq]))
(hs/sig area (-> Shape Double))
(hs/= (area (Circle r)) (:infix pi * r (:name "^") 2))
(hs/= (area (:rec Rect (= w x) h)) (* x h))
(hs/sig <+> (-> Shape Shape Double))
(hs/= (:infix a <+> b) (+ (area a) (area b)))
(hs/sig classify (=> (Ord a) (Num a) (-> a String)))
(hs/= (classify n)
  (| (< n 0) "negative")
  (| (:op "/=" n 0) (> n 100) big)
  (| otherwise (:cons \s "mall\torӒzero"))
  (where
    (= big "big")))
(hs/sig tally (-> [Shape] (M.Map String Int)))
(hs/= tally (foldl' (:lambda m s (M.insertWith + (name s) 1 m)) M.empty)
  (where
    (= name (:lambda-case
      (-> (Circle _) "circle")
      (-> (:rec Rect) "rect")))))
(hs/sig main (IO ()))
(hs/= main
  (do (let (= (:as xs (:paren (:lazy (:cons first _)))) [(Circle 1.5) (Rect 2 3)])
         (= (! n) (length xs)))
      (print (:tuple (map area xs) n first))
      (print [(area s) | (<- s xs) (> (area s) 1E+3M)])
      (print (:tuple (read @Int "42") (I# (+# (:int# 1) (:int# 2))) 0x1F ((:section-r div 2) 7) ((:paren (subtract 1)) 3)))
      (mapM_ print (sortBy (:lambda a b (compare (area a) (area b))) xs))))
```

Points of interest: the record constructor's field group reuses the signature form, with the
strict field `(! Double)` (§4.3); `Rect {w = x, h}` is a record pattern with a pun, unparenthesized
as an argument (records need no parentheses, I5); `pi * r ^ 2` is the unresolved mixed chain
(§5.2); `(+)` as an argument is the bare `+`; `xs@(~(first : _))` keeps its one extra pair of
parentheses as `:paren` (§5.3); `(subtract 1) 3` likewise, since a function application in function
position needs none; `1e3` is `1E+3M` (§3.5) and `0x1F` keeps its radix (HS2); `(read @Int "42")`
reads as `(read (arbace.core/deref Int) "42")`; `1# +# 2#` is a chain of `+#` over primitive
literals.

## Decided (2026-10-17, the user)

HS1-HS12 as recommended: `:sig` and `:lambda` (HS1); an integer's radix kept (HS2); fractional
literals exact (HS3); the corpus from GHC 9.14.1's source tarball, pinned by SHA-256 (HS4);
`.hsc` and `.lhs` in R6 (HS5); no resolved fixities now (HS6); an optional `{-# LINE #-}` to the
`.ae` file (HS7); the helper written anew after check-ppr (HS8); Alpine's GHC with version
checks (HS9); `hs.main.<path>` (HS10); converted forms not tracked (HS11); `-haddock` on (HS12).

On R1 as built (2026-10-17, the user): HR1-HR5 as recommended (doc/haskell/HASKELL-NOTES.md,
"Decided"): repr/haskell not embedded in the executable (HR1); the module's and the classes'
bodies in layout in the braces printer (HR2, §7.4, S14); every located node's line compared and
the `:lines` metadata keys of §7.1 (HR3); `bin/repr-haskell roundtrip` and `test` in `bin/gate
--full` (HR4); built-in syntax in constructor declarations and `(:doc-type ...)` (HR5, §4.3, §6).

On R2 as built (2026-10-17, the user): HR6-HR8 as recommended (doc/haskell/HASKELL-NOTES.md,
"Decided"): mode `:lines`'s four added normalizations of the interface level adopted into §9.3
(HR6); the interface level at `-O1` (HR7); all of a package's printed modules in their places at
once, one build per side (HR8, §9.3).
