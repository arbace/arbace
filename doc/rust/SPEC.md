# Rust forms: Rust source files written in Clojure

Status: milestone RS0 of repr/rust ([REPR-RUST-SURVEY.md](../REPR-RUST-SURVEY.md) §5.3), written
2026-10-17 at the user's decision RS16 (b) ("the spec now, the build after"). It is bound by the
survey's decisions RS1-RS17 (its section "Decided (2026-10-17, the user)"); the choices made while
writing it are S1-S34 (§13.1), the proposals RSS1-RSS10 (§13.2), decided as recommended. RR1 and RR2
are built (2026-10-17), RR3 (2026-10-18): §12.4-§12.6 say what the builds change in this text, and
[RUST-NOTES.md](RUST-NOTES.md) records them. The experiments that validate its choices (Appendix A) were run on the host's
rustc 1.99.1 (Alpine's `rust-1.99.1-r0`), the survey's helper (`rsdump`, built against
`rustc-dev-1.99.1-r0`) and the stage 0 of main at `73e35434` (`target/seed/amd64/arbace`, amd64);
their scripts and inputs are `test/rust/`.

This spec defines the **Rust forms**: how every construct of a Rust source file, as rustc's parser
sees it before macro expansion (`rustc_ast::Crate`, one file at a time), is written as Clojure data
in ordinary `.ae` files that Arbace's reader reads, what the forms keep of the source (lines, or
everything), and how the round trip Rust → forms → Rust is judged. It is the fifth bridge of the
`repr` family ([NAMING-SURVEY.md](../NAMING-SURVEY.md)), after repr/java
([classes/SPEC.md](../classes/SPEC.md)), repr/go ([go/SPEC.md](../go/SPEC.md)), repr/clojure and
repr/haskell ([haskell/SPEC.md](../haskell/SPEC.md)), and follows repr/haskell's spec in structure
and repr/go's in conventions. Its grammar is rust-lisp's (`/root/rust-lisp`, `rustlisp/SPEC.md`
and `DESIGN.md`: D1-D20, I1-I6, S1-S8, its §10's implicit parentheses: RS2), respelled where
Arbace's reader cannot read rust-lisp's lexemes (RS3).

Words used throughout:

- **the helper**: the Rust program `tools/rsdump`, a `rustc_private` driver on the host's rustc
  (RS8, §10). It parses one file under one configuration (rustc version, edition), stops before
  expansion, and emits rustc's parsed tree generically through rustc's own `Debug` impls, with the
  few facts the converter cannot get from the tree alone (§10.3). It decides nothing about forms.
- **the converter**: `arbace.repr.rust.convert`, in Arbace. It reads the helper's output and the
  Rust file itself and writes Rust forms. Every decision about forms is made here; rust-lisp's
  `compiler/rustc_parse/src/lisp/printer.rs` and `printer_expr.rs` are its mapping checklist
  (RS17).
- **the printer**: `arbace.repr.rust.print`, Rust forms → Rust source, after `rustc_ast_pretty`
  (RS10); rust-lisp's `shape.rs` is the checklist of its implicit parentheses. With the converter it
  makes the round trip.
- **a consumer**: anything that reads Rust forms for their meaning: the printer, and later tools
  that study Rust as data (the survey's RU1-RU3).
- **Rust forms**: the forms of this spec. **Rust context**: the inside of a top-level form (§4.1),
  where the forms mean Rust.
- **rustc**: the host's rustc, `/usr/bin/rustc` 1.99.1 (`rustc 1.99.1 (58cf8f9c3 2026-10-13)`),
  with `rustc-dev` and `rust-src` of the same build fetched as apks and pinned by SHA-256 (RS9),
  at conversion and check time only.
- **rust-lisp**: the user's fork of rustc at `f28e0de8c` (branch `rust-lisp`, rust master of
  2026-10-05, 1.101.0-nightly), a reference, never a dependency (RS17). Its forms (`.rsl`) are cited
  as "rust-lisp's", its decisions as "rust-lisp D10", "rust-lisp S4", its invariants "rust-lisp I5".

Contents: 1 Scope and principles · 2 The file form · 3 Lexical syntax · 4 Items ·
5 Expressions, statements and patterns · 6 Types · 7 Macros and token trees · 8 Attributes and
documentation · 9 Layout and positions · 10 The helper · 11 The round trip and its corpus ·
12 Coverage plan · 13 Decisions and open proposals · 14 Sources · Appendix A: the experiments ·
Appendix B: the worked example.

## 1. Scope and principles

### 1.1 The bridge

repr/rust is a bridge (RS1): `bin/repr-rust read` turns Rust source files into Rust forms,
`bin/repr-rust print` turns Rust forms into Rust source files, and the round trip proves the pair
lossless (§11). It serves holding Rust code as forms (a vendored crate, a corpus, Rust's own
standard library), studying rustc's parsed tree in Arbace's own data, and the Arbace language's
breadth (the survey's RU1-RU3). No translator is part of it (RS15): nothing here gives Rust a
meaning in Arbace, and nothing runs Rust. A translator, if ever wanted, would start from MIR (a
`repr/mir` through `rustc_public`, survey §5.2), a separate survey.

The bridge's parts, as repr/go's and repr/haskell's:

| | repr/go | repr/haskell | repr/rust |
|---|---|---|---|
| source | Go (`go/ast` + `go/types`) | Haskell (GHC's parsed tree, after CPP) | Rust (rustc's parsed tree, `rustc_ast::Crate` of one file, before expansion) |
| helper | `tools/godump` (Go) | `tools/hsdump` (Haskell, on the `ghc` library) | `tools/rsdump` (Rust, on `rustc_driver` and `rustc_ast`) |
| converter, printer | `arbace.repr.go.convert`, `.print` | `arbace.repr.haskell.convert`, `.print` | `arbace.repr.rust.convert`, `.print` |
| forms | Go forms ([go/SPEC.md](../go/SPEC.md)) | Haskell forms ([haskell/SPEC.md](../haskell/SPEC.md)) | Rust forms (this spec) |
| judge | gc: trees, export data, code; the text | GHC: trees and lines, interfaces; the text | rustc: trees and lines, interfaces (rustdoc's JSON); the text; later code and behaviour |
| positions | `:lines`, `:full` | `:lines`, `:full` | `:lines`, `:full` (RS11) |
| command | `bin/repr-go` | `bin/repr-haskell` | `bin/repr-rust` (§11.6, a sketch) |

### 1.2 Principles

1. **The forms are rustc's parsed tree.** One form per node of `rustc_ast` as the parser builds it
   for one file, before macro expansion, with its out-of-line modules unloaded, by rust-lisp's
   grammar (RS2): its shapes, invariants I1-I6, implicit parentheses (its §10) and choices S1-S8,
   with the respellings of §3 applied. Classification is by context, head, shape and the lexical
   class of atoms, never by name resolution or types (rust-lisp I2). Nothing expansion, name
   resolution or type checking decides is written: macro calls stay calls, `#[cfg]` stays an
   attribute, paths stay unresolved (S1).
2. **Clojure keeps its meaning outside Rust context; inside, the forms mean Rust.** A file's forms
   are top-level forms whose heads are qualified by the alias `rs` (`rs/fn`, `rs/pub`, `rs/use`,
   `rs/attr`: RS5, §4.1), so loading the file as Clojure could never take `(fn ...)` or `(let ...)`
   for Clojure's. Inside a top-level form, rust-lisp's heads are used unqualified (`fn`, `let`,
   `match`, `=>`, `+=`, `.push`) with Rust's meaning, and every qualified symbol is a Rust path
   (§3.3), whatever the file's aliases.
3. **Plain Clojure syntax.** Everything reads with Arbace's reader (`arbace.core/read`), unchanged,
   without data readers (Go forms principle 5; CLAUDE.md, "Working conventions"). A Rust token is
   written as Rust spells it when the reader reads that text back as the same token, and otherwise
   by the respelling of §3 (RS3). Every bare name, path and literal the converter writes is checked
   by reading it back (§3.2, §3.3, §3.6).
4. **Rust's order and spelling.** Every form lists its parts in Rust's source order, only heads
   moving to the front (rust-lisp I1); visibility and qualifiers stay before an item's keyword.
   Names are verbatim (rust-lisp D3). Where two spellings mean one tree, the canonical one is the
   first this spec lists (rust-lisp I6's one canonical form).
5. **Lossless for rustc.** In mode `:lines` the forms keep everything of the parsed tree except
   what §11.2 normalizes: positions other than lines, node and attribute ids, token spacing, the
   token streams rustc collects for procedural macros (rust-lisp D20). Unlike Go's and Haskell's,
   the tree holds each literal's spelling (`ast::Lit`'s symbol and suffix), so the forms keep every
   literal's spelling in both modes (§3.6). In mode `:full` they keep everything, and the printed
   file is the original text byte for byte (RS11, §9.2).
6. **One conversion per file under the rustc version and the edition** (RS13). Before expansion
   every `#[cfg]` item and every `cfg_attr` is in the tree, so no target, cfg flag or feature is
   chosen: one conversion covers every platform. The edition (2015, 2018, 2021, 2024) changes the
   lexer and the parser and is recorded in the forms (§2.3).
7. **rustc is needed to read and to judge, not to hold or print.** The helper runs on the host's
   rustc, whose version the forms record and the helper checks (RS9, §10.5). Reading forms and
   printing Rust need only Arbace.

### 1.3 What changes from rust-lisp

rust-lisp's `.rsl` files and Rust forms differ only in these respects; a `.rsl` file and the Rust
forms of the same tree map onto each other mechanically, for the AST both rustc versions share (S2):

| | rust-lisp (`.rsl`) | Rust forms |
|---|---|---|
| reader | its own: EDN structure, Rust lexemes by `rustc_lexer` (D2) | Arbace's, unchanged (§3) |
| paths | one symbol with `::`: `std::io::Write`, `::core::mem` (D3) | `std.io/Write`, `(:global core/mem)` (§3.3) |
| `:` and `::` heads | `(: T Ord)`, `(: x u8)`, `(:: size_of T)` | `(:has T Ord)`, `(:has x u8)`, `(:turbofish size_of T)` (§3.5) |
| brace groups | `{:x 1 :y}`, not maps (D13) | `(:braces :x 1 :y)` (§3.5) |
| attributes | reader prefixes `#[derive Debug]`, `#![allow x]` (D11) | forms `(:attr derive Debug)`, `(:attr! allow x)`; `rs/attr`, `rs/attr!` at top level (§8) |
| doc comments | `;;;`, `;;!`, `#[:doc-block "..."]` (D11, S2) | data: `(:doc "...")`, `(:doc! "...")`, `(:doc-block "...")` (§8.3) |
| raw token trees | `#tt(...)`, `#tt[...]`, `#tt{...}` (I6) | `(:tt "(...)")`, the source text of the tree (§7.2) |
| literals | Rust's lexemes (`1u8`, `'a'`, `r#"x"#`, `b"x"`) | Clojure literals where they print back to the same token, else value forms or `(:lit "text")` (§3.6) |
| `^`, `^=`, `/=`, `@` | `(^ a b)`, `(^= a b)`, `(/= a b)`, `(@ x p)` | `(bit-xor a b)`, `(bit-xor= a b)`, `(div= a b)`, `(:at x p)` (§3.5) |
| top level | items with unqualified heads | `ns`, `rs/file`, the items with their first element qualified (§2, §4.1) |
| positions | the `.rsl` file's own spans (D17) | metadata: lines (`:lines`) or every span and token (`:full`) (§9) |
| comments | `;` comments kept approximately by its printer | `;` comments in `:lines` (lost on reading), data in `:full` (§9.3) |
| rustc | 1.101.0-nightly (its fork) | the host's 1.99.1 |

rustc 1.99.1's AST lacks some nodes rust-lisp's printer maps (`ExprKind::GcaMacro`,
`TyKind::GcaMacro`, `ItemKind::TestBinderConstraints`, `token::IdentKind`; survey §2.6, E8) and
has one it does not print (`TyKind::FieldOf`, `builtin # field_of`); §12.3 lists
rustc 1.99.1's variants and where this spec writes each.

### 1.4 A first example

From the worked example (Appendix B, `test/rust/sample/shapes.rs`):

```rust
impl Shape {
    pub fn area(&self) -> f64 {
        match self {
            Shape::Circle(r) => std::f64::consts::PI * r * r,
            Shape::Rect { w, h: height } => w * height,
            Shape::Empty => 0.0,
        }
    }
}

impl<'a> Tally<'a> {
    fn add(&mut self, name: &'a str) {
        *self.counts.entry(name).or_insert(0) += 1;
        self.total /= 1;
    }
}
```

As Rust forms, in mode `:lines` (each form on its Rust line):

```clojure
(rs/impl Shape
  (pub fn area [&self] -> f64
    (match self
      (=> (Shape/Circle r) (* std.f64.consts/PI r r))
      (=> (Shape/Rect (:braces :w :h height)) (* w height))
      (=> Shape/Empty 0.0))))

(rs/impl (:generics 'a) (Tally 'a)
  (fn add [(&mut self) name (& 'a str)]
    (+= (* (.or_insert (.entry self.counts name) 0)) 1)
    (:semi (div= self.total 1))))
```

- Top-level heads are `rs/...` (§4.1); inside, rust-lisp's heads (`pub`, `fn`, `match`, `=>`, `*`,
  `+=`, `.entry`).
- Paths are qualified symbols: `Shape::Circle` is `Shape/Circle`, `std::f64::consts::PI` is
  `std.f64.consts/PI`: the segments but the last are the namespace (§3.3). `self.counts` is a field
  chain, a symbol with dots and no `/`.
- `Shape::Rect { w, h: height }` is a struct pattern whose brace group is `(:braces ...)`, `:w` a
  shorthand field (§5.7). `'a` reads as `(quote a)`, a lifetime (§3.4).
- `/=` cannot be read: it is `div=` (§3.5). The body's last statement has its `;` in the source, so
  it is `(:semi ...)`; the other statements get theirs from the printer (rust-lisp D8, §5.6).
- `0.0` is a Clojure double that prints back as `0.0`, the token's spelling (§3.6).
- Comments are not data in `:lines` (§9.3); doc comments are (§8.3).

## 2. The file form

### 2.1 Namespaces and files

A Rust source file is an Arbace namespace and one forms file (RS5): Rust's modules are files, and a
file is what the helper parses. The namespace is `rs.` followed by the crate's name and the file's
module path, derived from the file's path, not from the `mod` items that reach it (S3):

| file (relative to the crate's directory) | namespace | forms file |
|---|---|---|
| `src/lib.rs`, `src/main.rs` (the crate root) | `rs.core` | `rs/core.ae` |
| `src/option.rs` | `rs.core.option` | `rs/core/option.ae` |
| `src/num/mod.rs` | `rs.core.num` | `rs/core/num.ae` |
| `src/num/dec2flt/parse.rs` | `rs.core.num.dec2flt.parse` | `rs/core/num/dec2flt/parse.ae` |
| `src/lib.miri.rs` (a file named by `#[path]`) | `rs.core.lib_miri` | `rs/core/lib_miri.ae` |
| `tests/num.rs`, `benches/x.rs`, `examples/y.rs`, `build.rs` | `rs.core.tests.num`, `rs.core.benches.x`, ... | `rs/core/tests/num.ae` |
| a single-file program of a test corpus, `tests/ui/borrowck/x-y.rs` | `rs.main.tests.ui.borrowck.x_y` | `rs/main/tests/ui/borrowck/x_y.ae` |

The rule: under `src/`, drop `src/` and `.rs`, then a final `mod`, `lib` or `main`; elsewhere in
the crate's directory, drop `.rs`; join the segments with `.`, and inside a segment replace `.` and
`-` by `_`. The crate's name is Cargo's (`[lib] name`, else the package name with `-` replaced by
`_`). A corpus program outside any crate is `rs.main.` followed by its path, as Haskell forms'
`hs.main.` (HS10). The forms file is the namespace's path with Clojure's munging, under the forms
root. Two files of one conversion that map to one namespace are a converter error (none in std's
five crates, Appendix A, E2).

Converted forms are regenerated, not tracked, under `.tmp/repr-rust/forms/CORPUS`
(`CORPUS-full` in mode `:full`), as Go forms' and Haskell forms' (HS11), until a crate is held as
source by a recorded decision (RSS10).

### 2.2 The file

A forms file holds, in this order (Rust's, I1):

1. `(ns rs.core.option (:require [arbace.rust :as rs]))`: `arbace.rust` is a nominal namespace that
   no consumer loads, as Go forms' `arbace.go` and Haskell forms' `arbace.haskell`; it gives the
   alias `rs` its meaning.
2. `(rs/file ...)` (§2.3), always present: the file's record.
3. The file's inner attributes and inner doc comments (the crate's `attrs` in the tree:
   `#![...]`, `//!`, `/*! */`), each a top-level form (§8): `(rs/attr! ...)`, `(rs/doc! ...)`,
   `(rs/doc-block! ...)`, in source order.
4. The items, in source order, each preceded by its outer attributes and doc comments as top-level
   forms (`(rs/attr ...)`, `(rs/doc ...)`, `(rs/doc-block ...)`), in source order (§4.1).

```clojure
(ns rs.sample.shapes (:require [arbace.rust :as rs])) (rs/file :path "shapes.rs" :crate shapes :edition 2024 :positions :lines :config {:rustc "1.99.1" :crate-type bin}) (rs/doc! " Shapes: the spec's worked example (doc/rust/SPEC.md, Appendix B).")
(rs/attr! allow dead_code)

(rs/use std.collections/HashMap)
```

In mode `:lines` the `ns` and `rs/file` forms share the first form's line (§9.1): `rs/file` has no
Rust position and is written on one line.

### 2.3 `rs/file`

```
(rs/file option*)
```

| option | value |
|---|---|
| `:path` | the file's path as rustc was given it, relative to the directory rustc ran in (`"core/src/option.rs"` for std's corpus, run in `library/`): it is the file name of every span, which reaches panic messages, `file!()` and debuginfo (§9) |
| `:crate` | the crate's name, a symbol |
| `:edition` | `2015`, `2018`, `2021` or `2024`: the crate's edition, which the helper parsed with (RS13) |
| `:kind` | how the crate reaches the file: `:root` (the crate root), `:module` (a `mod m;` file found by Rust's rules), `:path` (named by a `#[path]` attribute), `:include` (an `include!`d file of items), `:include-expr` (an `include!`d file that is one expression: RR6, §2.5); omitted for `:module` |
| `:positions` | `:lines` (the default) or `:full` (§9) |
| `:config` | the configuration (§2.4) |
| `:shebang`, `:frontmatter` | the text of a first `#!` line, of a `---` front matter: rustc's lexer strips both before parsing, so they are in no tree; kept in both modes (`:frontmatter-line`, the front matter's line when blank lines precede it: RR5) |
| `:rs/comments`, `:rs/space`, `:rs/tail`, `:rs/crlf`, `:rs/bom` | in mode `:full` only: the text the tree does not hold (§9.2) |

### 2.4 The configuration record

`:config` is a map, the same for every file of one conversion except where noted:

| key | example | meaning |
|---|---|---|
| `:rustc` | `"1.99.1"` | the version of the rustc the helper was built against and ran on (`rustc --version`); the helper refuses another (§10.5) |
| `:rustc-commit` | `"58cf8f9c3"` | the commit `rustc --version` prints |
| `:rustc-driver` | `"61690d3e4c729118"` | the hash in the name of the `librustc_driver-*.so` the helper links: it changes when the same version is rebuilt (an Alpine `-rN` revision) |
| `:crate-type` | `lib` | the crate's type, for the interface level (§11.3) |
| `:corpus` | `"rust-src-1.99.1-r0"` | the corpus the file comes from, with its pin (§11.4) |

Keys a conversion does not use are omitted. No target, cfg flags or features: the parsed tree does
not depend on them (principle 6). The printer ignores `:config`; the round trip's levels use it to
parse and build the printed file as the original (§11.3).

### 2.5 Crate roots, `#[path]` and `include!`

Each file is converted alone, as the helper parses it: as a crate root whose `mod m;` items are not
loaded (the survey's `after_crate_root_parsing`; rust-lisp's check runs per file the same way). So
a module file and a crate root have the same form, and a file reached through `#[path = "..."]`
(std's `sys/` tree uses it, in `cfg_select!` arms too) or `include!` is converted as a file of its
own with the edition of the crate that includes it (RS13), its `:kind` recording how it is reached.
An `include!`d file that is an expression, not items (`include!` in an expression's place, which
parses the file as one expression with nothing stripped: a `#!` line and `---` fences are tokens
there), is `(rs/file ... :kind :include-expr)` and one form `(rs/expr e)`, the expression (RSS6,
built at RR6: §12.9).

## 3. Lexical syntax

### 3.1 Names

Rust's identifiers are written verbatim (rust-lisp D3), as symbols, when the reader reads them back
(§3.2):

| Rust | form | notes |
|---|---|---|
| identifier `x`, `snake_case`, `CamelCase`, `_x`, `_`, `δx`, `naïve`, `x1` | the same symbols | rustc keeps identifiers NFC-normalized; the source's spelling is `:full`'s (§9.2) |
| raw identifier of a keyword `r#type`, `r#match`, `r#try` (2018+), `r#gen` (2024) | `r#type`, `r#match`, ... | `r#` is written exactly where rustc's `Ident` prints it (the name is a keyword of the file's edition); a raw identifier that is no keyword (`r#foo`) is the same tree as `foo`, and its spelling is `:full`'s |
| `self`, `Self`, `super`, `crate` (path segments, the self value) | the same symbols | |
| a tuple field or tuple index `0`, `1` | `x.0` in a field chain, `0` in `(. e 0)`, `:0` in a brace group (§5.3) | |
| a field chain on a name: `self.x.0`, `a.b.c` | one symbol with dots and no `/` (rust-lisp I4) | a path has a `/` or no dot (§3.3), so the two never collide |
| a method name in a call `.push`, `.await`, `.r#type` | the symbol with a leading `.` (rust-lisp I3) | |
| a macro name `println!`, `vec!`, `std::println!` | `println!`, `vec!`, `std/println!` | §7.1 |
| a lifetime or label `'a`, `'static`, `'_`, `'outer`, `'r#mod` | `'a`, read as `(quote a)` (§3.4) | |
| a name the reader does not read back | `(:name "text")` | §3.2 |

Rust's strict keywords cannot be identifiers (but as raw identifiers); the weak and contextual words
(`union`, `auto`, `default`, `safe`, `raw`, `macro_rules`, `dyn` in 2015, `gen` before 2024) are
special exactly where Rust makes them so, by shape (rust-lisp S3).

### 3.2 The name rule

A Rust identifier with text `t` (as rustc's `Ident` prints it: `x`, `r#type`) is written **bare**
exactly when the reader, reading `t` alone between delimiters, reads one symbol without a namespace
whose name is `t`. Otherwise it is `(:name "t")` wherever a name stands (S5). The converter applies
the rule to every name it writes, and the converter's writer checks it again by reading what it
wrote; a misread is a converter error, not a silent change.

Rust's lexical grammar for identifiers (XID_Start or `_`, then XID_Continue) excludes every
character the reader treats specially, so the rule refuses only identifiers that read as literals:

| text | the reader | form |
|---|---|---|
| `nil` (a legal Rust identifier) | `nil` | `(:name "nil")` |
| `true`, `false` | booleans | never identifiers in Rust (keywords) |
| `r#nil` | the symbol `r#nil` | bare (but rustc prints it `nil`: it is no keyword) |

Measured over std's five crates (Appendix A, E2): 394,759 identifiers outside token trees (1,719
more are tuple indices), every one bare; no `nil`. The rule costs nothing on real code, but it is
the converter's guard against the reader (survey §2.3's silent misreads).

### 3.3 Paths

A path without generic arguments (`ast::Path` whose segments have no `args`) is one symbol
(RS4, S4):

- **One segment** `x`, `Vec`, `self`: the name, by §3.2.
- **Two or more segments** `a::b::c::D`: a qualified symbol whose namespace is the segments but the
  last joined by `.` and whose name is the last segment: `a.b.c/D`. `std::io::Write` is
  `std.io/Write`, `Self::Item` is `Self/Item`, `u8::MAX` is `u8/MAX`, `crate::a::b` is
  `crate.a/b`, `super::super::x` is `super.super/x`, `Shape::Circle` is `Shape/Circle`, a macro path
  `core::ptr::addr_of!` is `core.ptr/addr_of!`, a glob `e::*` in a use tree `e/*`.
- **A global path** `::core::mem::swap`, `::std` (a leading `::`, rustc's `{{root}}` segment):
  `(:global core.mem/swap)`, `(:global std)`; in a use tree `use ::{a, b}`, `(:global [a b])`.
- **Generic arguments on the last segment**: in a type `(Vec T)`, `(std.vec/Vec T)` (rust-lisp's
  application, §6); in an expression or a pattern `(:turbofish size_of T)` for `size_of::<T>`
  (rust-lisp's `(:: ...)`, §5.3).
- **Generic arguments on an earlier segment** `Vec::<u8>::with_capacity`,
  `std::vec::Vec::<u8>::new`: `(:path (Vec u8) with_capacity)`, `(:path (std.vec/Vec u8) new)`:
  the segments up to the first with arguments as one path, then each later segment, a symbol or
  `(name args...)` (rust-lisp's `:path`); for a global path the first element's path is
  `(:global ...)`: `::std::vec::Vec::<u8>::new` is `(:path ((:global std.vec/Vec) u8) new)`.
- **Qualified paths** `<T>::f`, `<T as Tr>::Assoc`: `(:qself T f)`, `(:qself T Tr Assoc)`
  (rust-lisp's).
- **A path that does not read back** (a segment `nil`, alone): `(:path seg...)` with each segment
  a symbol or `(:name "t")`.

Rust names contain no `.` or `/`, so the split is unambiguous both ways, and a qualified symbol
is always a path, a dotted symbol without `/` always a field chain. Measured (E2): of 228,564 paths
outside token trees, 180,593 have one segment, 22,375 two and 5,570 more; all 27,945 multi-segment
paths read back as their `ns/name` symbols but one (`use ::{...}`'s empty path, `(:global [...])`);
2 are global; 19,860 have generic arguments on the last segment only and 166 on another segment.
The reader reads `std.r#type/x`, `x/r#type`, `foo/nil`, `std/println!`, `e/*` and `std.io/*` as
intended (E1).

### 3.4 Lifetimes and labels

`'a`, `'static`, `'_`, `'outer`, `'r#mod` are written as Rust spells them; the reader reads `'a`
as `(quote a)`, which in Rust context is the lifetime or label `'a` wherever a lifetime or a label
can stand: `(& 'a T)`, `(Counter 'a)`, `(:has 'a 'b)`, `(break 'outer v)`, `(loop 'outer ...)`
(S6). Characters are `\c` (§3.6), so `'a'` is never written. A lifetime whose name the reader does
not read back as a symbol (`'nil`) is `(:lifetime "nil")`.

Since `'x` reads as a list headed by `quote`, `quote` joins rust-lisp's reserved heads (I3): a call
of a path named `quote` is `((:path quote) x)` (rust-lisp's escape for a call of a special head), a
generic type named `quote` with arguments `(:path (quote A))`, a tuple-struct pattern `((:path quote)
p)`. Elsewhere `quote` is a plain symbol (a value, a binding, a module). std's five crates name
`quote` 15 times, never in one of these positions outside token trees (E2).

### 3.5 The respelling table

Every Rust token class, or rust-lisp spelling, that Arbace's reader cannot read or reads as something
else, and its spelling in Rust forms. The column "E1" names the samples of Appendix A that check it
(`test/rust/reader-samples.txt`); every form in the second column reads as one form of the expected
class.

| Rust / rust-lisp | Rust forms | why | E1 |
|---|---|---|---|
| paths `a::b::C`, `Self::Item`, `e::*` (rust-lisp one symbol) | `a.b/C`, `Self/Item`, `e/*` (§3.3) | `::` is no token inside a symbol | `std::io::Write`, `std.io/Write` |
| `::core::mem` | `(:global core/mem)` | `::` and a leading `/` do not read | `::core::mem`, `(:global core.mem/swap)` |
| `(: T Ord)`, `(: 'a 'b)`, `(: x u8)`, `(: self T)`, `(: Item Clone)`, `(trait (: Tr A B))` (bounds, typed patterns and parameters, supertraits, associated bounds, named function-pointer parameters) | `(:has T Ord)`, `(:has 'a 'b)`, `(:has x u8)`, ... : one head for Rust's `:` (S7, RSS1) | `:` is no token | `(: T Ord)`, `(:has T Ord)` |
| `(:: size_of T)`, `((:: .collect (Vec _)) it)` (turbofish) | `(:turbofish size_of T)`, `((:turbofish .collect (Vec _)) it)` | `::` is no token | `(:: size_of T)`, `(:turbofish ...)` |
| `#[derive Debug Clone]`, `#[cfg (all unix (= feature "x"))]`, `#[doc = "x"]`, `#[path #tt(...)]` | `(:attr derive Debug Clone)`, `(:attr cfg (all unix (= feature "x")))`, `(:attr doc = "x")`, `(:attr path (:tt "= \"x\""))`, before the form it attaches to; `rs/attr` at top level (§8) | `#[` is a reader tag | `#[derive Debug Clone]`, `(:attr ...)` |
| `#![allow unused]` | `(:attr! allow unused)`; `rs/attr!` at top level | `#!` is a line comment | `(:attr! allow unused)` |
| `;;; doc`, `;;! doc`, `#[:doc-block "x"]`, `#![:doc-block "x"]` | `(:doc " doc")`, `(:doc! " doc")`, `(:doc-block "x")`, `(:doc-block! "x")` (§8.3) | `;` comments are dropped by the reader | `(:doc " text")` |
| `#tt(...)`, `#tt[...]`, `#tt{...}` | `(:tt "(...)")`: the Rust text of the token tree, delimiters included (§7.2) | `#tt` is a reader tag; Rust's tokens are not Arbace's | `#tt(a, b)`, `(:tt ...)` |
| `(:rust #tt{item})` | `(:rust "item")`; `(rs/rust "item")` at top level | idem | `(:rust "reuse a::b;")` |
| brace groups `{:x 1 :y}`, `{pub :x f64}`, `{}` | `(:braces :x 1 :y)`, `(:braces pub :x f64)`, `(:braces)` (S8) | maps need pairs, refuse repeated keys and lose the order | `{:x :y 2}`, `(:braces ...)` |
| `'a'`, `'\n'`, `'\''`, `'é'`, `'\u{1F600}'` (characters) | `\a`, `\newline`, `\'`, `\é`, `(:char 128512)` (§3.6) | `'a'` reads as `(quote a')`, `'\n'` fails | `'a'`, `\a`, `\😀` |
| byte, byte string, C string, raw string literals `b'a'`, `b"x"`, `br"x"`, `c"x"`, `cr"x"`, `r"x"`, `r#"x"#` | `(:byte \a)`, `(:bytes "x")`, `(:raw-bytes "x" 0)`, `(:cstr "x")`, `(:raw-cstr "x" 0)`, `(:raw "x" 0)`, `(:raw "x" 1)` (§3.6) | two forms with the escapes decoded, or a regex literal | `r"a\n"`, `r#"x"#`, `(:raw ...)` |
| numbers with suffixes or Rust's notation `1u8`, `0x_ff`, `0o17`, `0b1010`, `1_000`, `1e3`, `1.`, `2.5f32` | `(:lit 1 u8)`, `(:lit "0x_ff")`, `(:lit "0o17")`, `(:lit "0b1010")`, `(:lit "1_000")`, `(:lit "1e3")`, `(:lit "1.")`, `(:lit 2.5 f32)` (§3.6) | invalid numbers; the tree keeps the spelling | `1u8`, `(:lit ...)` |
| strings with Rust's escapes `"\x41"`, `"\u{1F600}"`, a line continuation, `"\01"` | `(:lit "\"\\x41\"")` ... where the canonical spelling differs (§3.6) | invalid escapes; `"\01"` reads as U+0001 | `"\x41"`, `"\01"` |
| `(^ a b)` | `(bit-xor a b)` (Go forms §7.5's word) | `^` is metadata: `(^ a b)` reads as `(b)` | `(^ a b)`, `bit-xor` |
| `(^= a b)` | `(bit-xor= a b)` | `(^= a b)` reads as `(a b)` | `(^= a b)`, `bit-xor=` |
| `(/= a b)` | `(div= a b)` (S9, RSS2) | `/=` is an invalid token | `/=`, `div=` |
| `(@ x p)`, `(@ (ref x) p)` | `(:at x p)`, `(:at (ref x) p)` | `@` is deref: `(@ x p)` reads as `((deref x) p)` | `(@ x p)`, `(:at ...)` |
| the identifier `nil` | `(:name "nil")` (§3.2) | reads as `nil` | `nil` |
| a call of `quote` | `((:path quote) x)` (§3.4) | `'x` reads as `(quote x)` | `quote` |

Everything else of rust-lisp reads as written (E1): names, raw identifiers, field chains, method
heads, macro calls `(println! ...)`, `(vec! [1 2 3])`, every other operator (`+ - * / % & | ! && ||
<< >> == != < <= > >= = += -= *= %= &= |= <<= >>= .. ..= ... -> => ?`), `&mut`, `&raw`, `&pin`,
`*const`, `*mut`, `&self`, keywords (`:tuple`, `:index`, `:semi`, `:paren`, `:_`, `:generics`,
`:qself`, `:path`, `:repeat`, `:builtin`), vectors, `()`, plain strings, `true`, `false`, decimal
integers and the floats that print back as written.

### 3.6 Literals

rustc's tree keeps each literal as its token: kind, symbol (the text between the quotes, or the
digits) and suffix (`ast::Lit`, `token::Lit`; E2 of the survey: `symbol: "0x_ff", suffix: "u8"`), and
the tree level compares them (§11.2). A literal is therefore written so that its token is exact in
both modes (S10): as a Clojure literal or a short value form when the printer's **canonical
spelling** of the value is the token, and as `(:lit ...)`, the token's text, otherwise. The first
row that applies is the form (one canonical form, I6):

| token | form | canonical spelling (when the form is not `:lit`) |
|---|---|---|
| `true`, `false` | `true`, `false` | |
| integer, no suffix | the integer, read as a long or a BigInt (the writer may print `N`) | the decimal digits without leading zeros or `_`: `0`, `42`, `340282366920938463463374607431768211455` |
| float, no suffix | the double | the text the reader's double prints as (`str`): `1.0`, `0.5`, `3.141592653589793`, `1.0E10` |
| integer or float with a suffix, digits as above | `(:lit N suffix)`: `(:lit 1 u8)`, `(:lit 0 usize)`, `(:lit 2.5 f32)` | as above, then the suffix |
| cooked string `"..."` | the Clojure string of its characters | §3.6.1 |
| character `'c'` | `\c` for a character of the BMP, `(:char n)` beyond it | §3.6.1, with `'` escaped and `"` not |
| byte `b'c'` | `(:byte \c)`, the character U+0000-U+00FF of the byte | §3.6.1's byte rule |
| byte string `b"..."` | `(:bytes "s")`, one character U+0000-U+00FF per byte | §3.6.1's byte rule |
| C string `c"..."` | `(:cstr "s")` | §3.6.1 (its `\xNN` escapes above 7F make bytes that are no characters, so they are `:lit`) |
| raw string `r"..."`, `r#"..."#` | `(:raw "s" n)`, `n` the number of `#` | always: a raw literal has no escapes, so its symbol is its value |
| raw byte string, raw C string | `(:raw-bytes "s" n)`, `(:raw-cstr "s" n)` | always |
| any other: another radix, `_` separators, leading zeros, an exponent, `1.`, digits a double does not print back, escapes other than the canonical ones, line continuations, a suffix on a string | `(:lit "text")`, `(:lit "text" suffix)`: `text` the token's source text without its suffix (prefix, quotes and `#`s included), `suffix` a symbol | the text itself |

`(:lit "0x_ff" u8)` is `0x_ffu8`, `(:lit "1e3")` is `1e3`, `(:lit "\"a\\x41\"")` is `"a\x41"`,
`(:lit "'\\u{A9}'")` is `'\u{A9}'`, `(:lit "b'\\x00'")` is `b'\x00'`. The kind of a `:lit` token
is the one Rust's lexer gives its text. A **negative literal** is a negation in Rust's tree
(`Unary(Neg, Lit)`), and is written as the reader's negative number when the number is bare and
the reader reads it back: `-5`, `-1.5`, `-9223372036854775808` (rust-lisp's `-5` atom); otherwise
`(- x)`: `(- 0)` (`-0` reads as 0), `(- (:lit 1 i8))`, `(- (:lit "0x10"))`.

#### 3.6.1 The canonical escapes

The printer spells a cooked string's or a character's value with `\"` (strings) or `\'`
(characters), `\\`, `\n`, `\r`, `\t`, `\0`, the other controls (U+0001-U+001F) and DEL as
`\u{x}` in lower-case hexadecimal without leading zeros (rustc's `escape_debug`, which its pretty
printer uses), and every other character as itself; a byte or byte string's value with the same
escapes for `"`/`'`, `\`, `\n`, `\r`, `\t`, `\0`, printable ASCII (U+0020-U+007E) as itself, and
every other byte as `\xNN` in lower case. The converter writes the bare or value form exactly when
this spelling equals the token's symbol; it never writes an octal escape in a Clojure string (the
reader's `"\01"` is U+0001, survey §2.3), and writes controls and characters that do not print as
`\uXXXX`.

Measured over std's five crates (E2): of the 33,174 literals outside token trees, 25,972 (78.3%) are
plain Clojure literals (19,229 integers, 4,616 strings, 1,571 booleans, 406 characters, 150 floats),
2,972 (9.0%) value forms (2,495 `(:lit N suffix)`, 224 bytes, 123 raw strings, 107 byte strings,
19 C strings, 4 characters beyond the BMP) and 4,230 (12.8%) `(:lit "text")`: 3,390 integers
(hexadecimal masks and code points, `1_000_000`), 606 floats (the 60-digit constants of
`f64::consts`, `1e-4`), 161 strings with line continuations, 43 characters such as `'\u{A9}'`, 30
byte literals and byte strings (27 with upper-case hexadecimal). Inside token trees (macro
arguments) strings dominate: 41,577 canonical, 447 not. The alternative `\xNN` for controls in
strings and characters would make 2 more characters canonical and 1 string not (E2), so rustc's
`escape_debug` is kept.

## 4. Items

### 4.1 Top-level heads

A top-level form is rust-lisp's form with its **first element qualified by `rs`** (RS5, S11): the
item keyword, or the visibility or qualifier that Rust writes before it (rust-lisp I1 keeps them in
front). Inside a top-level form, nothing is qualified.

| rust-lisp's top level | Rust forms | Rust |
|---|---|---|
| `(fn ...)`, `(pub fn ...)`, `(pub (crate) const unsafe fn ...)`, `(async fn ...)`, `(unsafe extern "C" fn ...)` | `(rs/fn ...)`, `(rs/pub fn ...)`, `(rs/pub (crate) const unsafe fn ...)`, `(rs/async fn ...)`, `(rs/unsafe extern "C" fn ...)` | functions |
| `(struct ...)`, `(enum ...)`, `(union ...)`, `(trait ...)`, `(auto trait ...)`, `(unsafe trait ...)`, `(impl ...)`, `(unsafe impl ...)`, `(const unsafe impl ...)` | `rs/struct`, `rs/enum`, `rs/union`, `rs/trait`, `rs/auto`, `rs/unsafe`, `rs/impl`, `rs/const` | type definitions, traits, impls |
| `(type ...)`, `(const ...)`, `(static ...)`, `(const (do ...))` | `rs/type`, `rs/const`, `rs/static`, `(rs/const (do ...))` | aliases, constants, statics, const blocks |
| `(use ...)`, `(mod ...)`, `(extern crate ...)`, `(extern "C" ...)` | `rs/use`, `rs/mod`, `(rs/extern crate ...)`, `(rs/extern "C" ...)`, `(rs/unsafe extern "C" ...)` | imports, modules, extern crates and blocks |
| `(macro_rules! m #tt{...})`, `(macro m #tt{...})` | `(rs/macro_rules! m (:tt "{...}"))`, `(rs/macro m ...)` (§7.3) | macro definitions |
| `(m! ...)` (an item macro call) | `(rs/macro-call m! ...)` (§7.1; RSS3) | `thread_local! { ... }`, `cfg_select! { ... }`, `impl_int!(...)`; a path `a::m!` is `(rs/macro-call a/m! ...)` |
| `(:rust #tt{...})` | `(rs/rust "...")` (§4.10) | items written in Rust |
| `#![...]`, `;;!`, `#![:doc-block ...]` | `(rs/attr! ...)`, `(rs/doc! "...")`, `(rs/doc-block! "...")` (§8) | the file's inner attributes and docs |
| `#[...]`, `;;;`, `#[:doc-block ...]` | `(rs/attr ...)`, `(rs/doc "...")`, `(rs/doc-block "...")` (§8) | an item's outer attributes and docs, before it |

Every top-level form of a forms file is `ns`, `rs/file` or one of these heads; anything else is an
error. A top-level item never begins with a path (item macro calls have `rs/macro-call`), so the
alias `rs` in a head never meets a Rust path `rs::x`, which is `rs/x` inside forms. Contextual words
(`union`, `auto`, `default`, `safe`, `macro_rules`) qualify as heads only in an item's shape, as
rust-lisp S3 says. The canonical spelling of each item, with the respellings of §3 applied, follows.

### 4.2 Visibility and qualifiers

Before the item keyword, in Rust's order (rust-lisp §2.1): `pub`, `pub (crate)`, `pub (self)`,
`pub (super)`, `pub (in a/b)`; `default`, `final`; `const`; `async`, `gen`; `unsafe`, `safe`;
`extern`, `extern "abi"`; `auto`; and the unstable restriction `impl (crate)` before `trait`. A
contextual word is a qualifier only on the way to a keyword it can qualify (`async fn`,
`safe static`, `auto trait`, `default type`), so it stays usable as a name (rust-lisp S3).

### 4.3 Functions

| Rust | form |
|---|---|
| `fn f(a: A, b: B) -> R where ... { body }` | `(fn f [a A b B] -> R (where ...) body...)` |
| `fn f();` (no body: a trait method, a foreign function) | `(fn f [])` |
| `fn f() {}` (empty body) | `(fn f [] (:braces))` |
| `fn f<T: Ord>(...)` | `(fn (f (:has T Ord)) [...] ...)` |
| `const async unsafe extern "C" fn f(...)` | `(const async unsafe extern "C" fn f [...])` |

Parameters are flat pattern/type pairs (rust-lisp D7). A self parameter comes first and stands
alone: `self`, `(mut self)`, `&self` (rust-lisp S6; `(& self)` is the same tree), `(& 'a self)`,
`&mut self` as `(&mut self)`, `(&mut 'a self)`, `(&pin mut self)`, or `(:has self T)`,
`(:has (mut self) T)` with a type. An anonymous parameter (2015 trait methods) is `:_ T`, a C
variadic `...` alone or `args ...` named. A parameter's attributes stand before its pattern (§8.2).
`-> T` and `(where ...)` follow the parameters, then the body (§5.6), nothing, or `(:braces)`.

### 4.4 Generics, bounds, where clauses

- **Names with generics** (rust-lisp D12): a generic item is named the way it is used,
  `(Name params...)`: `(fn (largest T) ...)`, `(struct (Pair 'a T) ...)`, `(trait (From T) ...)`.
  Parameters: `T`, `'a`, `(:has T B...)` (bounds), `(:has 'a 'b...)`, `(= T Default)`,
  `(= (:has T B...) Default)`, `(const N T)`, `(const N T default)`. `impl` has no name, so its
  parameters are `(:generics params...)`: `(impl (:generics 'a T) ...)`.
- **Where clauses**: `(where pred...)`, each predicate `(:has T B...)`, `(:has 'a 'b...)`,
  `(for [params] (:has T B...))`; a predicate's attributes stand before it (§8.2).
- **Bounds**: a trait path or generic application (§6), a lifetime, or `(? B)`, `(! B)`,
  `(const B)`, `(:maybe-const B)` (`[const] B`), `(async B)`, `(for [params] B)`, `(use args...)`
  (precise capturing), `(:paren B)`, nested in Rust's order: `for<'a> [const] async ?Trait` is
  `(for ['a] (:maybe-const (async (? Trait))))`.

### 4.5 Structs, unions, enums, fields

| Rust | form |
|---|---|
| `struct S;`, `struct S();`, `struct S {}` | `(struct S)`, `(struct S [])`, `(struct S (:braces))` |
| `struct P(pub T, U);` | `(struct P [pub T U])` |
| `struct P<T> where ... { pub x: T, y: T }` | `(struct (P T) (where ...) (:braces pub :x T :y T))` |
| `union U { a: u8 }` | `(union U (:braces :a u8))` |
| `enum E { A, B(T), C { x: T }, D = 1 }` | `(enum E A (B T) (C (:braces :x T)) (= D 1))` |

Named fields are a brace group of `mods* :name T (= default)?`, tuple fields a vector of
`mods* T (= default)?` (rust-lisp D13), the modifiers a visibility, `mut (crate)` (a mut
restriction) and `unsafe`/`safe`; a field's attributes stand before its first element. A field
name is a keyword: `:x`, `:r#type`, `:0` all read (E1). Variants are `A`, `(A T...)`,
`(A (:braces ...))`, `(pub A ...)`, `(= variant e)` for a discriminant.

### 4.6 Traits and impls

| Rust | form |
|---|---|
| `trait Tr: A + B where ... { items }` | `(trait (:has Tr A B) (where ...) items...)` |
| `trait Tr = A + B;` | `(trait Tr = A B)` |
| `impl<T> Tr for S<T> where ... { items }` | `(impl (:generics T) Tr for (S T) (where ...) items...)` |
| `impl !Send for S {}` | `(impl (! Send) for S)` |
| `impl S { items }` | `(impl S items...)` |
| associated items | §4.3's functions, `(type ...)`, `(const ...)`, macro calls `(m! ...)`, with `default` and `final` qualifiers |

### 4.7 Type aliases, constants, statics

| Rust | form |
|---|---|
| `type A<T> = B<T>;` | `(type (A T) (B T))` |
| `type Item: Clone;` (trait) | `(type (:has Item Clone))` |
| `type A where W1 = T where W2;` | `(type A (where ...) T (where ...))` |
| `const N: usize = 5;`, without value | `(const N usize 5)`, `(const N usize)` |
| `static mut X: T = e;` | `(static mut X T e)` |
| `const { body }` (item) | `(const (do body...))` |

### 4.8 `use`, `mod`, extern crates and blocks

| Rust | form |
|---|---|
| `use a::b::{self, C as D, e::*};` | `(use (a/b self (as C D) e/*))` |
| `use a::b;`, `use a::b as c;`, `use a::*;` | `(use a/b)`, `(use (as a/b c))`, `(use a/*)` |
| `use {a, b};`, `use ::{a, b};`, `use ::std::io;` | `(use [a b])`, `(use (:global [a b]))`, `(use (:global std/io))` |
| `mod m;`, `mod m {}`, `mod m { items }`, `unsafe mod m { ... }` | `(mod m)`, `(mod m (:braces))`, `(mod m items...)`, `(unsafe mod m ...)` |
| `extern crate a as b;`, `extern crate self as s;` | `(extern crate a as b)`, `(extern crate self as s)` |
| `extern "C" { items }`, `unsafe extern "C" { items }` | `(extern "C" items...)`, `(unsafe extern "C" items...)` |

A use group is a list whose head is its prefix (one path symbol), a vector without prefix
(rust-lisp's). Foreign items are functions without bodies, statics (`safe static`, `unsafe
static`), types and macro calls.

### 4.9 Macro definitions and item macro calls

§7.1 and §7.3.

### 4.10 Escapes

`(:rust "text")`, `(rs/rust "text")` at top level, is an item, an associated item or a foreign
item written in Rust, for unstable syntax that rust-lisp's forms do not cover: delegation (`reuse`,
`ItemKind::Delegation`, `DelegationMac`) and function contracts (rust-lisp S5, D19). `(:noparen e)`
emits `e` without the parentheses §5.2's rules would add (rust-lisp S1; never needed by its corpus).

## 5. Expressions, statements and patterns

### 5.1 Atoms and simple forms

Literals (§3.6), `true`, `false`, paths (§3.3), `_`, `..` (the full range), field chains on a name
(`self.x.0`), negative literals (`-5`) are atoms. `[a b c]` is an array, `(:repeat e n)` is
`[e; n]`, `()` and `(:tuple ...)` are tuples (`(:tuple a)` is `(a,)`), `(:paren e)` keeps
parentheses the grammar does not need (§5.2).

### 5.2 Operators and implicit parentheses

rust-lisp D4 with the respellings of §3.5:

| Rust | form |
|---|---|
| `a + b + c` (a left-associated chain) | `(+ a b c)`; `(+ (+ a b) c)` is the same tree (the first is canonical) |
| `a ^ b`, `a ^= b`, `a /= b` | `(bit-xor a b)`, `(bit-xor= a b)`, `(div= a b)` |
| the other binary operators `- * / % && \|\| & \| << >> == != < <= > >=` | `(op a b ...)`; comparisons strictly binary |
| `-x`, `!x`, `*x` | `(- x)`, `(! x)`, `(* x)` |
| `&x`, `&mut x`, `&raw const x`, `&raw mut x`, `&pin mut x` | `(& x)`, `(&mut x)`, `(&raw const x)`, `(&raw mut x)`, `(&pin mut x)` |
| `a = b`, `a += b`, and `-= *= %= &= \|= <<= >>=` | `(= a b)`, `(+= a b)`, ... |
| `e as T` | `(as e T)` |
| `e?`, `e.await`, `e.use`, `e.yield` | `(? e)`, `(.await e)`, `(.use e)`, `(.yield e)` |
| `a..b`, `a..=b`, `a..`, `..b`, `..` | `(.. a b)`, `(..= a b)`, `(.. a :_)`, `(.. :_ b)`, `..` |
| `a[i]` | `(:index a i)` |
| `e.f.g` on an `e` that is not a name | `(. e f g)` |

With two or more operands a head is binary; `-`, `*`, `!`, `&`, `&mut` with one operand are prefix
operators. Measured (E2, outside token trees): `^` 17, `^=` 17, `/=` 13 occurrences in std.

**Implicit parentheses** (rust-lisp I5, its §10): Rust's parser keeps every pair of parentheses as
a node (`ExprKind::Paren`, `PatKind::Paren`, `TyKind::Paren`). Lisp nesting shows the structure, so
a `Paren` node where Rust's grammar needs parentheses for the tree the forms describe is implicit:
the printer adds them by the pretty printer's rules (`rustc_ast_pretty`'s `FixupContext`,
`util::classify`: an operand of lower precedence, a cast before `<` or `<<`, a block-like
expression at the start of a statement followed by an operator, a struct literal in a condition, a
jump or closure before an operator, a `let` scrutinee with `&&` or `||`, a binary expression with
outer attributes), and the converter leaves exactly those `Paren` nodes out. Every other `Paren`
node is `(:paren e)`. Patterns get parentheses for an or-pattern in a parameter, after `@` or as an
alternative of another or-pattern, and for an or-pattern, a range or a `mut` binding after `&`,
`&mut` or `box`; types for a trait object or `impl Trait` with several bounds after `&`, `*const`,
`*mut`, in a return type or after `as`. The tree level compares `Paren` nodes exactly (§11.2), so
the converter and the printer must agree to the node; rust-lisp's `shape.rs` is the shared table
(S12).

### 5.3 Calls, methods, fields, struct literals

- `(f a b)` is `f(a, b)`, `(f)` is `f()`; the callee may be any expression; a callee that is a
  reserved head (rust-lisp I3, and `quote`, §3.4) is `((:path name) args...)`.
- `(.m recv a b)` is `recv.m(a, b)`; with generic arguments `((:turbofish .m T) recv a b)` (rust-lisp
  S8 respelled).
- `(Path (:braces fields))` is a struct literal: `(Point (:braces :x 1 :y 2))`; a keyword followed
  by another keyword, a modifier, `..` or the end is shorthand (`(:braces :x :y 2)` is
  `{ x, y: 2 }`); `.. base` and `..` close it: `(S (:braces :x 1 .. (Default/default)))`.
- Field access: a field chain symbol on a name (`self.x.0`), `(. e f g)` otherwise, `(. a/b c)`
  on a path of several segments (S13).

### 5.4 Blocks and control flow

rust-lisp D8 and D18, unchanged:

| Rust | form |
|---|---|
| `{ body }`, `'a: { body }` | `(do body...)`, `(do 'a body...)` |
| `unsafe { body }`, `const { body }` | `(unsafe body...)`, `(const (do body...))` |
| `async { }`, `async move { }`, `gen { }`, `async gen { }` | `(async body...)`, `(async move body...)`, `(gen ...)`, `(async gen ...)` |
| `try { }` | `(try body...)` |
| `if c { a } else if d { b } else { e }` | `(if c a... (else if d b...) (else e...))`, the else-if chain flat |
| `if let p = e && c { }` | `(if (&& (let p e) c) ...)` |
| `while c { }`, `'a: while c { }` | `(while c body...)`, `(while 'a c body...)` |
| `for p in it { }`, `for await p in it { }` | `(for p it body...)`, `(for await p it body...)`, `(for 'a p it ...)` |
| `loop { }` | `(loop body...)`, `(loop 'a body...)` |
| `match e { p => a, p if g => b }` | `(match e (=> p a) (=> p (if g) b))`; `e.match { }` is `(.match e arms...)` |
| a never arm `p,`; with a guard | `(=> p)`, `(=> p (if g) :_)` |
| `break`, `break 'a v`, `continue 'a` | `(break)`, `(break 'a v)`, `(continue 'a)` |
| `return v`, `yield v`, `become f()`, `do yeet v` | `(return v)`, `(yield v)`, `(become (f))`, `(do yeet v)` |

An arm's attributes stand before it (§8.2).

### 5.5 Closures

`(quals... fn [params] -> T? body...)`, the qualifiers in Rust's order `for [params]`, `const`,
`static`, `async`/`gen`, `move`/`use` (rust-lisp D9): `(fn [x y] (+ x y))`, `(move fn [] (run))`,
`(async move fn [x] ...)`. Parameters are patterns, `(:has p T)` with a type. Without `-> T`, one
body form is the closure's expression and several are a block; with `-> T` the body forms are a
block's statements. `|| {}` is `(fn [] (do))`.

### 5.6 Statements and the tail

A body's statements are the elements of the form that owns it (rust-lisp D8): inner attributes
first, then items (§4, unqualified), `(let p e?)`, `(let (:has p T) e?)`, `(let p e (else
body...))`, `(let mut x e)` for `(let (mut x) e)`, `(super let ...)`, expressions and macro calls.
The last expression is the tail, the block's value. Every other expression gets the `;` Rust
requires after it, none after a block-like expression (`if`, `match`, `loop`, `while`, `for`,
blocks) or a macro call with braces; `(:semi e)` writes `e;` where Rust does not require the `;` (the
last statement, as in `self.total /= 1;` before `}`, or a block-like expression), `(:semi)` an empty
statement `;` (`StmtKind::Empty`). A macro call statement keeps its style (`MacStmtStyle`):
`(m! ...)` before the end, `(:semi (m! ...))` as the last statement with `;`, `(m! (:braces ...))`
or `(m! (:tt "{...}"))` with braces.

### 5.7 Patterns

| Rust | form |
|---|---|
| `_`, `..`, `!`, literals, `-1`, paths | the atom |
| `x`, `mut x`, `ref x`, `ref mut x`, `mut ref pin const x` | `x`, `(mut x)`, `(ref x)`, `(ref mut x)`, `(mut ref pin const x)` |
| `x @ p`, `ref x @ p` | `(:at x p)`, `(:at (ref x) p)` |
| `S(a, b)`, `S()` | `(S a b)`, `(S)` |
| `S { a, b: p, ref mut c, box d, .. }` | `(S (:braces :a :b p ref mut :c box :d ..))` |
| `(a, b)`, `(a,)`, `(..)`, `()` | `(:tuple a b)`, `(:tuple a)`, `(:tuple ..)`, `()` |
| `[a, b @ .., c]` | `[a (:at b ..) c]` |
| `a \| b`, `\| a` | `(\| a b)`, `(\| a)` |
| `&p`, `&mut p`, `&pin mut p` | `(& p)`, `(&mut p)`, `(&pin mut p)` |
| `box p`, `deref!(p)` (`PatKind::Deref`) | `(box p)`, `(:deref p)` |
| `1..=5`, `a..b`, `..=5`, `1..`, `0...9` | `(..= 1 5)`, `(.. a b)`, `(..= :_ 5)`, `(.. 1 :_)`, `(... 0 9)` |
| `(p)` | implicit (§5.2) or `(:paren p)` |
| `p if c` (guard pattern) | `(:guard p c)` |
| `const { .. }`, `m!(...)` | `(const (do ...))`, `(m! ...)` |

`@` binding patterns occur 39 times in std (E2). A struct pattern field's attributes stand before
its keyword (§8.2).

### 5.8 Other expressions

`(let p e)` is a `let` expression (in conditions). `(:builtin name (:tt "(...)"))` is
`builtin # name(...)` (`offset_of`, `type_ascribe`, `field_of`, `wrap_binder`, `unwrap_binder`;
rust-lisp writes `ExprKind::Type`, `OffsetOf`, `UnsafeBinderCast` this way, S14). `(:move e)` is
`move(e)` (`ExprKind::Move`). An expression's attributes stand before it (§8.2).

## 6. Types

rust-lisp §4 with the respellings applied.

| Rust | form |
|---|---|
| a path, `_`, `!`, `Self` | the symbol (`std.io/Result`, `Self/Item`) |
| `Vec<T>`, `std::vec::Vec<T>`, `fmt::Formatter<'_>` | `(Vec T)`, `(std.vec/Vec T)`, `(fmt/Formatter '_)` |
| `&T`, `&'a T`, `&mut T`, `&'a mut T` | `(& T)`, `(& 'a T)`, `(&mut T)`, `(&mut 'a T)` |
| `&pin mut T`, `&'a pin const T` | `(&pin mut T)`, `(&pin 'a const T)` |
| `*const T`, `*mut T` | `(*const T)`, `(*mut T)` |
| `[T]`, `[T; N]` | `[T]`, `[T N]` |
| `()`, `(A,)`, `(A, B)` | `()`, `(:tuple A)`, `(:tuple A B)` |
| `fn(A, B) -> C`, with a named parameter | `(fn [A B] -> C)`, `(fn [(:has a A)])` |
| `for<'a> unsafe extern "C" fn(&'a u8)` | `(for ['a] unsafe extern "C" fn [(& 'a u8)])` |
| `dyn A + B`, `impl A + B`, `A + B` (2015 bare trait object) | `(dyn A B)`, `(impl A B)`, `(:bare A B)` |
| `(T)` | implicit (§5.2) or `(:paren T)` |
| `<T as Tr>::Assoc`, `<T>::Assoc` | `(:qself T Tr Assoc)`, `(:qself T Assoc)` |
| `unsafe<'a> T` | `(unsafe ['a] T)` |
| `m!(...)`, `...` (C variadic) | `(m! ...)`, `...` |
| `builtin # field_of(T, f)` (`TyKind::FieldOf`) | `(:builtin field_of (:tt "(T, f)"))` (S15) |

Generic arguments: types, lifetimes, const arguments (literals, `-1`, `true`, `(do body...)` for
`{ ... }`), associated item constraints `(= Name T)`, `(= (Name args...) T)`, `(:has Name B...)`.
Parenthesized arguments `Fn(A, B) -> C` are `(Fn [A B] -> C)`; since a single vector argument means
parenthesized arguments, a slice or array as the only angle-bracketed argument takes `:of`:
`(Box :of [T])` is `Box<[T]>` (rust-lisp S4). A type named like a special head (`dyn` in 2015
code) with arguments is `(:path (dyn args...))`.

## 7. Macros and token trees

### 7.1 Calls

Before expansion a macro call (`MacCall`: a path, a delimiter and a token stream) is tokens, in
expressions, statements, patterns, types, items and associated items: 15,142 calls in std's five
crates (E2). rust-lisp D10, respelled (RS6):

- `(path! args...)`: `path!(a, b, c)` with each argument an expression form, when the tokens are a
  comma-separated list of expressions: `(println! "{} {}" a (+ b 1))`, `(assert_eq! (f x) 3)`,
  `(std/println! ...)`, `(core.ptr/addr_of! x)`.
- A single vector argument is the bracket delimiter: `(vec! [1 2 3])` is `vec![1, 2, 3]`, and a
  single `(:repeat e n)` in a vector `vec![e; n]`; a single `(:braces ...)` the brace delimiter.
- Anything else is the call's token tree as text: `(m! (:tt "(...)"))`, `(:tt "[...]")`,
  `(:tt "{...}")`, the delimiter being the call's (§7.2).
- At top level, an item macro call is `(rs/macro-call path! args...)` (§4.1).

The converter writes the arguments as forms exactly when the printer's tokens for them are the
call's tokens: the same kinds and texts, the call's delimiter, spacing ignored (rust-lisp's own
check, `mac_args_as_exprs`), and, in mode `:lines`, when every token the forms have no element for
(the commas, the delimiters) stands where the printer puts it, but the closing delimiter, whose line
the form carries (`:rs/close`, §9.1) (S16). A trailing comma, a `;`-separated argument, a pattern
or a type therefore make the call `:tt`; named format arguments (`name = value`) are an assignment
expression whose printed tokens are the call's, so they stay forms (RR-P3). The printer's tokens
are compared as rustc's lexer glues them (`Token::glue`: `>` `>` printed joint is `>>`), and a
nested call kept as `(:tt ...)` matches the source's group by its text. Brace-delimited calls are
always `(:tt "{...}")`; an empty call is `(m!)` or `(m! [])`. The helper gives the parse (§10.3),
the printer the tokens: no Rust lexer is needed in Arbace.

### 7.2 Token trees as text

`(:tt "text")` holds a token tree as Rust text: the source text from its open to its close
delimiter, verbatim, comments and white space included (S17). The converter takes it from the
Rust file by the tree's span (as repr/go's converter reads Go files for `:full`, `:text-dir`). The
printer writes the text as it is, so the tokens inside keep their relative lines, and the tree level
compares their spans' lines as it compares every span's (§11.2). Hand-written or generated forms may
hold any text that lexes to the tokens they mean. `(:tt ...)` occurs where rust-lisp writes `#tt`:
macro arguments, attribute arguments (§8.1), macro definitions (§7.3), `builtin #` arguments
(§5.8).

### 7.3 `macro_rules!` and declarative macros 2.0

- `macro_rules! m { rules }`, `macro_rules! m ( rules );`: `(macro_rules! m (:tt "{ rules }"))`,
  `(macro_rules! m (:tt "( rules )"))`: the body always text (rust-lisp D10); at top level
  `rs/macro_rules!`. 884 macro definitions in std (E2).
- `macro m { rules }`: `(macro m (:tt "{ rules }"))`; the single-rule form `macro m(args) { body }`
  is `(macro m (:tt "(args)") (:tt "{ body }"))`, its two groups as the source writes them (S18):
  the parser joins them into one token stream, which the printer reproduces from the two.

### 7.4 What the forms hold of macro-heavy code

Macro-heavy files (`core::num`'s `int_impl!` bodies, `stdarch`, declarative DSLs) are largely
`(:tt ...)` text: the bridge stays lossless, but the forms of those parts are Rust text, not data
(survey §5.4). Breaking macro bodies into token forms (the survey's RS6 (b)) is left to a consumer
that needs it.

## 8. Attributes and documentation

### 8.1 Forms

rust-lisp D11 as forms (RS7): an outer attribute is a form `(:attr ...)` before the element it
attaches to, an inner attribute `(:attr! ...)` at the start of a body; at top level they are
`(rs/attr ...)` and `(rs/attr! ...)` (§4.1).

| Rust | form |
|---|---|
| `#[inline]`, `#[rustfmt::skip]` | `(:attr inline)`, `(:attr rustfmt/skip)` |
| `#[doc = "x"]`, `#[path = "x.rs"]`, `#[doc = include_str!("x")]` | `(:attr doc = "x")`, `(:attr path = "x.rs")`, `(:attr doc = (include_str! "x"))` (an expression) |
| `#[derive(Debug, Clone)]`, `#[cfg(all(unix, feature = "x"))]`, `#[cfg_attr(test, derive(Debug))]` | `(:attr derive Debug Clone)`, `(:attr cfg (all unix (= feature "x")))`, `(:attr cfg_attr test (derive Debug))`: meta items, each a path, a literal, `(= path lit)` or `(path meta...)` |
| `#[unsafe(no_mangle)]`, `#[unsafe(export_name = "x")]` | `(:attr unsafe no_mangle)`, `(:attr unsafe export_name = "x")` |
| `#[name(tokens)]`, `#[name[tokens]]`, `#[name{tokens}]` that are no meta items | `(:attr name (:tt "(tokens)"))`, ... |
| `#![no_std]`, `#![feature(a, b)]` | `(:attr! no_std)`, `(:attr! feature a b)` |

The meta forms are used only when the printer's tokens for them are the attribute's tokens (as
§7.1's check; the helper gives the meta item, §10.3); otherwise the arguments are `(:tt ...)`
(rust-lisp D11, its §8).

### 8.2 Where they stand

rustc 1.99.1's AST carries attributes on exactly these nodes (`attrs` fields, E2's list): `Crate`,
`Item` (items, associated items, foreign items), `Variant`, `FieldDef`, `GenericParam`,
`WherePredicate`, `Param`, `Arm`, `Local`, `MacCallStmt`, `Expr`, `ExprField`, `PatField`. An
attribute form stands before the element of the node it attaches to, in the enclosing sequence:
before an item in a body or at top level, before a variant, before a field's first element (its
visibility or keyword), before a generic parameter or a where predicate, before a parameter's
pattern in a parameter vector, before an arm in `match`, before a statement, before an expression
in any operand or element position, before a field of a struct literal or a struct pattern. An
attribute form is **transparent** (S19): it does not count among its form's elements for the
grammar (an attribute in a parameter vector does not break the pattern/type pairs), and it attaches
to the next element. `#[attr] a + b` is `(+ (:attr attr) a b)` (on `a`), `#[attr] (a + b)` is
`(:attr attr) (+ a b)` in the enclosing sequence, its parentheses implicit (§5.2). Measured (E2):
158,250 outer attributes in std (146,250 of them line doc comments), 12,044 inner.

### 8.3 Documentation comments

Doc comments are attributes in rustc's tree (`AttrKind::DocComment`, with the comment's kind and
text), so they are data (survey §3.1):

| Rust | form | at top level |
|---|---|---|
| `/// text` | `(:doc " text")` | `(rs/doc " text")` |
| `//! text` | `(:doc! " text")` | `(rs/doc! " text")` |
| `/** text */` | `(:doc-block " text ")` | `(rs/doc-block " text ")` |
| `/*! text */` | `(:doc-block! " text ")` | `(rs/doc-block! " text ")` |

The text is the comment's symbol as rustc keeps it: after `///` or `//!`, leading space included;
between `/**` and `*/`, line breaks included. Each `///` line is one attribute and one form, on its
line. Plain comments are not in the tree (§9.3).

## 9. Layout and positions

rustc's positions reach a program's behaviour with their columns: every panic location
(`Option::unwrap`, `Result::expect`, slice indexing, arithmetic overflow in debug builds, everything
`#[track_caller]`) prints `file:line:col`, as do `file!()`, `line!()`, `column!()` and
`Location::caller()` (survey §1.3); spans also reach debuginfo and, through `#[track_caller]`
constants, the code. The forms keep positions in one of two modes (RS11), recorded as `rs/file`'s
`:positions`. Unlike Go and Haskell, Rust has no line directive (`//line`, `{-# LINE #-}`): the
printed file must have the lines by construction.

### 9.1 Mode `:lines` (the default)

Positions are lines, carried by the layout of the forms text, as Go forms' and Haskell forms'
(Go SPEC §10.1, Haskell SPEC §7.1):

- **The forms file has the Rust file's lines.** Every form that stands for a node with a span is
  written on the line where the span starts, so the reader's `:line` of every list is its node's; a
  list's head is not moved to its line (an operator's head stands where Rust writes the first
  operand, a method call's where it writes the receiver: the node's span starts there). `ns` and
  `rs/file` share the first form's line. Any Rust line's tokens can be printed on one line, so a
  Rust file never has fewer lines than its forms need, and the printer can always put every token on
  its line (S20).
- **Elements that change line** (S21): the reader attaches `:line` to lists only. A symbol, vector
  or `()` whose node's span starts on another line than the element printed before it gets the line
  written: `^{:line 12} Empty`, `(^{:line 3} .baz (^{:line 2} .bar foo))` for a method chain laid
  out one call per line (the method name's segment has its own span). A literal or keyword in that
  case is recorded in its parent's `:rs/breaks`, a vector of entries `[index line]` (by `nth`, the
  head 0), also for a head literal or keyword (Go's `:go/breaks`, Haskell's `:hs/breaks`).
- **Token lines**: spans of tokens the forms have no element for get their line as metadata on the
  form that implies the token, when it is not the line the printer would put it on: the line of the
  latest span that starts before the token in the file (the printer reproduces every span's start
  and adds no line of its own). The keys (RR-P2, as built at RR1):
  - `:rs/brace` on a form with a block body (a function, a loop, an `if` or `else`, a closure with
    a block body, `let`-`else`'s `else`), the line of the block's `{` (`Block`'s span: rustfmt puts
    it on a line of its own after a `where` clause); the `{` of a `match`, an impl's, a trait's or
    an enum's body has no span;
  - `:rs/close`, the line of a closing delimiter: of a macro call or an attribute (or a nested meta
    list) whose arguments are forms (`DelimSpan`); and of the delimiter that an empty span follows
    on its line: the `)` of a function's or a function pointer's parameters before a missing return
    type's span (`FnRetTy::Default`), the last delimiter before a missing `where` clause's span (`>`
    of a generic name's parameters or a type's arguments, `)` of parameters, of a tuple struct's
    fields or of a tuple type, `]` of a slice type);
  - `:rs/paren` on a form whose implicit parentheses' `(` begins a line;
  - `:rs/open` is defined for a call's open delimiter on another line; std needs none.
- **Heads**: a list's head stands where Rust writes the token it stands for. A binary operator's
  default line is its right operand's when that begins a later line, else the current one
  (rustfmt's `a\n + b` needs no metadata); a binary head on another line gets `^{:line N}`, which
  the writer writes whenever the head has one, and a chain `(+ a b c)` is flat only when its inner
  operators need none. Any other head that stands for a span (a method's name `.m`, `=` and the
  compound assignments, `:index`'s `[`, `:has`'s `:`, a range pattern's operator) gets its line
  when it is not the current one (RSS9 (a)). A head that is a list (`(:turbofish .collect T)`) is
  written on the current line with every line inside it as metadata, since Rust prints it after
  elements the forms write after it. The table of keys per node is the converter's, kept with the
  rustc version (S22).
- **Lists without a span of their own** (a brace group of fields) are written where the text is,
  their first element with its line.
- **Unspanned tokens** (commas, `->`, `=>`, `;`, closing delimiters outside token streams, keywords
  the tree has no span for) go where the printer puts them: their lines are in no tree.
- **Lists placed after their line** carry an explicit `:line`, which the reader keeps over its own
  (Go SPEC §10.1): the case of a form that must follow another the Rust line puts before it.
- **Literal spellings** are kept in `:lines` (§3.6): the tree holds them.

What `:lines` does not keep: columns; the lines of unspanned tokens; white space; plain comments
(§9.3); the spelling of raw identifiers that are not keywords, of NFC-equivalent identifiers, of
`(& self)` against `&self`; the source's choice among the spellings of one tree (trailing commas
outside token trees). The tree level compares what `:lines` keeps (§11.2).

Forms written by hand (no converter) have the lines of their own text, which is what source lines
mean: a Rust file printed from them reports the `.ae` file's lines (Rust has no line directive to
name the forms file instead).

### 9.2 Mode `:full`

Every position of the parsed tree is kept, and with the text the tree does not hold, the printer
gives the original file back byte for byte (the text level, §11.1). The metadata, written only in
this mode, follows Haskell forms' §7.2 and repr/go's as built (PRINTER-NOTES.md, "Mode :full as
built"):

- **`:rs/pos`** on every list and symbol: a map from position names to positions. `:span` is the
  node's span `[line col end-line end-col]`; the other keys are the tokens the form implies, each
  `[line col]`, a vector of them for a repeated token: the keywords by their name (`:fn`, `:let`,
  `:else`, `:in`, `:where`, `:as`, `:mut`, `:pub`...), `:eq`, `:arrow` (`->`), `:fat-arrow` (`=>`),
  `:colon`, `:path-sep` (`::`), `:comma`, `:semi`, `:lparen`/`:rparen`, `:lbrack`/`:rbrack`,
  `:lbrace`/`:rbrace`, `:langle`/`:rangle`, `:pound` (`#`), `:bang` (`!`), `:pipe` (a closure's
  `|`), `:dot`, `:question`. A form that stands for several nodes carries their spans under keys
  named after the node (a function's `:sig-span`, `:decl-span`, `:body-span`): the table of keys per
  node is the converter's, kept with the rustc version (S22).
- **`:rs/apos`** on the parent of a literal or keyword child: a map from child index to its span.
- **`:rs/paren`**: the parentheses of implicit `Paren` nodes the form absorbs, `[[lparen rparen]
  ...]`, outermost first.
- **`:rs/text`** on a form whose source spelling differs from the one the printer derives: a raw
  identifier that is no keyword, an identifier not in NFC, `(& self)` written `&self`, a trailing
  comma (as the `:comma` token's presence).
- On `rs/file`: **`:rs/comments`**, every plain comment as the source spells it, `[[line col end-line
  end-col "text"] ...]` (doc comments are attributes, in the forms); **`:rs/space`**, `[[line col
  "ws"] ...]`, the white space runs other than single spaces between tokens and newlines;
  **`:rs/tail`**, the text after the last token when it is not one newline; **`:rs/crlf`**, true when
  the file's lines end in `\r\n` (rustc normalizes them before lexing); **`:rs/bom`**, true for a
  UTF-8 byte order mark (stripped likewise).

Columns are rustc's: counted in characters (code points) from 1, a tab one column, the end column
exclusive, as rustc's spans print (`FILE:l1:c1: l2:c2`, E3 of the survey) (S23). The converter
reads the Rust file for the text (comments, white space, the source of `:tt`), and the helper's
lexer (`rustc_lexer`, over the same text) gives every token and comment with its range (§10.3).
This mode is for tools, not meant to be read (`bin/repr-rust read --full`).

### 9.3 Comments

- Doc comments are data (§8.3).
- Plain comments (`//`, `/* */`) are not in rustc's tree. In `:lines` the converter writes them as
  `;` comments in the forms text, at their Rust line where the layout allows (`; text` at the end of
  a line, `;; text` on a line of their own), and they are lost when the forms are read, as Go
  forms' (§10.4 there) and Haskell forms'; rust-lisp keeps them at item, field, statement and arm
  boundaries the same way. In `:full` every comment is data (`:rs/comments`). They do not count for
  the tree level; comments inside a token tree stay in its `(:tt ...)` text.

### 9.4 The printer's layout

The printer writes Rust from forms with no knowledge but the forms (it resolves no names, rust-lisp
I2), after `rustc_ast_pretty` (RS10: `pprust/state.rs`, `state/expr.rs`, `state/item.rs`,
`state/fixup.rs`, 4,718 lines in 1.99.1; its output gives back the same tree for 1,069 of std's
1,074 files, survey §4.3):

1. **Lines** (the first milestone): every token on its line (§9.1), the tokens of a line separated
   by single spaces where Rust's lexer needs or the pretty printer puts them, indentation by
   nesting (4 spaces, the pretty printer's), blank lines to reach a form's line. A form whose line
   was already passed is a converter error (or, for hand-written forms, a printer error naming the
   form): with no line directive, the printer does not paper over it.
2. **Pretty** (forms without positions, written by a program): the pretty printer's own layout,
   Oppen-style breaking at 100 columns (`pp.rs`), as `-Zunpretty=normal` prints.
3. **Full**: every token at its recorded line and column (§9.2), comments and white space from
   `rs/file`'s records; the output is the original text (the text level).

The printer can return its output as a token list (kinds and texts), which §7.1's and §8.1's
checks compare with the helper's tokens (S16).

## 10. The helper

### 10.1 What it is

`tools/rsdump/rsdump.rs`, a `rustc_private` driver (`rustc_driver::Callbacks`) seeded from the
survey's 183-line experiment (survey, Appendix A). It is Arbace's own code, written anew; rustc's
`-Zunpretty=ast-tree` (the same `Debug` text, pretty-printed) and rust-lisp's `check.rs` (its
normalization) are its models, not vendored (S24, RSS8). It stops after parsing the crate root
(`after_crate_root_parsing`), so every file is parsed alone with its `mod m;` items unloaded, and it
decides nothing about forms: it prints the tree through rustc's own `Debug` impls and transcodes the
text generically, so a new rustc release changes the converter's table (new or changed variants show
up as unknown ones), not the helper (survey §3.3 (a)).

### 10.2 What it dumps

`rsdump dump [--full] -o OUT --edition E FILE...`: for each file, one file of Clojure-reader forms:

- a first form `(rsdump/config {...})`: the configuration of §2.4, the edition, the helper's version
  and source hash, the source file and its SHA-256;
- then the tree, transcoded from `{:?}` as the survey's E3 did: `Name { a: x }` → `(Name :a x)`,
  `Name(x)` → `(Name x)`, `[..]` → vectors, an anonymous tuple → `(tuple ...)`, strings re-escaped
  for the reader (controls and DEL as `\uXXXX`), identifiers `x#0` → `(id "x" 0)`, lifetimes `'a#0`
  → `(id "'a" 0)`, raw lifetimes `'r#mod#0` → `(id "'r#mod" 0)`, `lifetime(ID: 'a)` → `(lifetime
  "'a")`, spans `FILE:l1:c1: l2:c2 (#n)` → `[l1 c1 l2 c2]` (`[... n]` for a context other than 0),
  `no-location` → `nil`, node ids and items' collected token streams dropped;
- errors as `(rsdump/error "message")`, the exit status 1 (a file rustc's parser rejects, or that
  it parses with recovered errors: the round trip skips it, rust-lisp D20).

The transcoder's quirks are part of what the pinned rustc prints, not a contract, and are fixed
against the corpus: the survey's transcoder read an identifier `r` with context 0 (`r#0`) as a raw
identifier, writing the symbol `r#0` (239 times in std's dumps, E2); `r#` begins a raw identifier
only before an identifier character, not a digit (S25).

`rsdump tree FILE`: the tree level's normal form (§11.2), the `Debug` text without spans, node ids,
attribute ids, token spacing and collected token streams. `rsdump lines FILE`: the start line of
every span of the tree, in the tree's order, with its node's name, for `:lines`' check (Haskell
forms' HR3). The comparison is a text comparison of two such outputs: no comparator program
(Haskell forms' S15).

### 10.3 Facts beyond the tree

The converter needs a few facts the tree does not give directly; the helper computes them with
rustc's own code and dumps them next to the node, as data:

- **Macro arguments as expressions**: for each `MacCall` whose delimiter is `()` or `[]`, the parse
  of its tokens as `expr (, expr)*` by rustc's parser (`rustc_parse::parser::Parser`, as
  rust-lisp's `parse_mac_exprs`), `(rsdump/mac-exprs [Expr...])`, or nothing when they do not parse;
  with the call's tokens (kind and text) for §7.1's check.
- **Meta items**: for each attribute, `AttrItem::meta` (the `MetaItem` rustc's built-in attributes
  read), `(rsdump/meta ...)`, or nothing.
- **Tokens and comments** (`--full`): `rustc_lexer` over the file, every token and comment with its
  byte range and its `[line col]`.

The converter reads the Rust file itself for `(:tt ...)` texts (by spans) and, in `:full`, for
the white space.

### 10.4 How it is built

By `bin/repr-rust helper`, with the host's rustc and nothing else (RS9):

1. `apk fetch rustc-dev rust-src` (Alpine edge, the host's repositories) into
   `.tmp/repr-rust/apks/`, checked against `tools/rsdump/apks.sha256`: `rustc-dev-1.99.1-r0.apk`
   `8837fc729e228a54a18fe576974fa1d48f947664133fbd2137b4dab61a356b21`, `rust-src-1.99.1-r0.apk`
   `0849c88c92333bdbe4f1867872db3499f33100851989a0760eb79685a4936cfb`; each extracted into its own
   directory (an apk is a gzip'd tar), nothing installed on the host.
2. A sysroot of links (`.tmp/repr-rust/sysroot/lib/rustlib/x86_64-alpine-linux-musl/lib/`): the
   host's `/usr/lib/rustlib/x86_64-alpine-linux-musl/lib/*` and `rustc-dev`'s 252 `.rmeta` and
   `.rlib` files, and its `librustc_driver-61690d3e4c729118.so`, the very library `/usr/bin/rustc`
   links.
3. `RUSTC_BOOTSTRAP=1 rustc --edition 2024 -O --sysroot SYSROOT tools/rsdump/rsdump.rs -o
   .tmp/repr-rust/rsdump` (2.9 s for the survey's helper; 282 KB, linked dynamically to the host's
   `librustc_driver`).

The executable is rebuilt when its source, the apks' hashes or `rustc --version` change (a key in
`.tmp/repr-rust/key`). `RUSTC_BOOTSTRAP=1` is confined to the helper's build and to the checks'
`-Z` runs (`rustdoc -Zunstable-options`); the forms and the printed Rust need nothing unstable
(std's own unstable features are what rustc accepts when it builds std, and the round trip builds
both sides the same way). The apks stay in `.tmp/` when Alpine's mirror drops them.

### 10.5 Versions

`tools/rsdump/RUSTC_VERSION` pins the rustc the bridge was written against: `1.99.1`, commit
`58cf8f9c3`, driver hash `61690d3e4c729118`. The helper checks at start that the rustc it was built
against (`rustc_interface::util::rustc_version_str()`, the driver's hash) is the pinned one, and refuses
otherwise; the converter refuses a dump whose `:rustc` differs from the pin; a corpus's forms record
`:rustc`, `:rustc-commit` and `:rustc-driver` (§2.4). When Alpine's `rust` moves (Alpine edge),
moving the pin is a recorded decision: the converter's table is revised for the new tree's variants,
the corpus re-pinned (the next `rust-src`), and the forms regenerated (RS9; an upstream toolchain
pinned by hash, `rust-1.99.1-x86_64-unknown-linux-musl` with its `rustc-dev` component, only if
Alpine's packages cannot serve). rustc is added to doc/ALPINE.md's subset when the bridge is built
(it is in the host's world already).

## 11. The round trip and its corpus

### 11.1 Levels

The round trip is *Rust → helper → converter → forms → printer → Rust*. For a file converted under
a configuration (rustc version, edition), the original and the printed file are parsed, and for the
levels after parsing their crates built, with the same rustc, edition and flags (RS12). They are
equivalent when:

1. **Tree.** rustc parses the printed file to the same tree as the original, compared as §11.2 says
   for the positions mode (rust-lisp's `-Zunpretty=lisp-check`, D20). This is the first gate: a tree
   rustc's parser cannot tell apart is the same program for expansion, resolution and everything
   after.
2. **Interface.** rustdoc's JSON of the two crates (`rustdoc -Zunstable-options --output-format
   json`), without its `span` fields, equal (§11.3): the public items, their signatures, generics,
   trait impls, attributes, docs.
3. **Lowered** (RR6, §12.9). The HIR of the two crates, after expansion, resolution and lowering,
   as `-Zunpretty=hir` prints it, without the root file's comments, positions normalized as the
   layout keeps them: it catches nothing the tree level misses for a pure printer, but checks a
   writer other than the converter, and what expansion makes of the printed tokens.
4. **Code** (RR6, §12.9). The MIR (`-Zunpretty=mir`'s text, positions normalized as for the HIR),
   then the objects byte for byte (an rlib's members but its metadata, which holds every span);
   columns reach code through panic locations, so the code of mode `:lines` is built with
   `-Zlocation-detail=file,line` (`Location` constants without columns), of the pretty layout
   with `-Zlocation-detail=file`.
5. **Behaviour** (RR5, §12.8). rust-lisp's `run-corpus.sh` method over rustc's UI run-pass programs, and
   its `std-from-lisp.sh` method: std built from the printed files (`-Zbuild-std`), the same
   programs run against it.

Beside them, the **text level**: in mode `:full`, the printed file is byte-identical to the
original. It needs no compilation, and implies every level above (`.rmeta`, which embeds spans and
the source's hash, compares only here: survey §4.1).

The tree, interface and text levels come first (RS12); the other levels later. A tokens level
(the two files' `rustc_lexer` tokens equal) is a debugging aid of the printer, not a level.

### 11.2 What the tree level compares

Both trees are printed by `rsdump tree` (§10.2) and compared as text. In mode `:full` the text
level subsumes the tree level. In mode `:lines` (rust-lisp D20, and the lines):

| N | difference | how it is normalized |
|---|---|---|
| N1 | spans | removed; instead, `rsdump lines` compares the start lines of every span, position by position (Haskell forms' HR3) |
| N2 | node ids, attribute ids (`AttrId`), syntax contexts | removed (they carry nothing before expansion) |
| N3 | token spacing (`Joint`, `Alone`, `JointHidden`) in token streams | removed (only procedural macros see it; the printer's spacing is its own) |
| N4 | the token streams rustc collects for items, expressions and attributes (`tokens: LazyAttrTokenStream`) | removed (rust-lisp D20: they are the source's tokens again, split differently: `> >` for `>>`) |

Everything else is compared exactly: every `Paren` node, literal kinds, symbols and suffixes, the
tokens of every token stream (kinds and texts), doc comments' kinds and texts, statement kinds and
macro statement styles, the order of everything. A dummy span (`no-location`) on one side against a
real one on the other is N1's (the survey's E6 met three).

### 11.3 How each level is checked

| level | how | normalization |
|---|---|---|
| tree | `rsdump tree` of the original and of the printed file, compared as text; `rsdump lines` of both in `:lines` | §11.2 |
| interface | each crate documented as its build compiles it, once from the originals and once with all the crate's printed files in their places (Haskell forms' HR8), under the same relative paths: `RUSTC_BOOTSTRAP=1 rustdoc --edition E --crate-name C --crate-type lib -Zunstable-options --output-format json`, the crate's `--cfg` and feature flags, its dependencies the host sysroot's (the same build as `rust-src`); the two JSON files compared after removing every `span` | every `span` field removed (E4: a file reprinted with other lines and layout gives equal JSON without them); in `:full` everything must be equal |
| text | the printed file against the original, byte for byte | none |
| lowered, code (RR6) | each crate built from the originals and from the printed files by the helper as rustc (`rsdump rustc`, §12.9): its HIR and MIR written after analysis, then its rlib or object | the positions rustc writes in the texts (`FILE:L1:C1: L2:C2`) kept to their start lines in `:lines`, removed in the pretty layout; the objects built with `-Zlocation-detail` as the layout keeps positions; in `:full` nothing |

rustc's own pretty printer (`-Zunpretty=normal`) is a ready candidate maker for proving the
checker, as `gocmp reprint` proved repr/go's oracle: it gives the same tree for 1,069 of std's 1,074
files under N1-N4 (survey §4.3, E6), the 5 others being `final fn` (which it drops) and dummy spans.

### 11.4 The corpus

RS14: std's five crates of the pinned rustc first, from Alpine's `rust-src-1.99.1-r0` (the very
sources of the host's std; SHA-256 in §10.4), extracted into `.tmp/repr-rust/corpus/`; then the rest
of `library/` and the compiler from the same apk; then a pinned crate set; then rustc's test suite
from rustc's source tarball for the behaviour level.

| corpus | files | first |
|---|---|---|
| `core`, `alloc`, `std`, `proc_macro`, `test` (`usr/src/rust/library/*/src`, edition 2024) | 288, 93, 646, 17, 30: 1,074 `.rs`, 14.3 MB | RR1 |
| the rest of `library/` (`stdarch` 209, `vendor` 926, `compiler-builtins` 280, `portable-simd`, `backtrace`, the `*tests` crates) | 1,902 | RR4 |
| the compiler (`usr/src/rustc/compiler`) | 2,167 | RR4 |
| a pinned crate set (the host's Cargo cache served the survey's E9: 518 crates, 12,144 files, editions 2015-2024) | | RR4 |
| rustc's test suite (`rustc-1.99.1-src.tar.xz`, pinned by SHA-256) | `tests/ui`: 19,942 files (21,108 but the 1,162 the parser rejects and 4 whose namespaces clash), 3,718 run-pass programs | RR5 |

Each crate is converted under its own configuration, written once in a corpus file
(`test/rust/corpus-rustc-1.99.1.edn`): its directory, name, edition, crate root and type, the files
reached by `#[path]` and `include!`, and for the interface level its `--cfg` flags, features and
dependencies. The survey's helper dumped all 1,074 files of the first corpus in 5.8 s (16 jobs), and
Arbace's reader read all their dumps (E2 here, E3 there).

### 11.5 Known differences

As repr/go's and repr/haskell's: one file per corpus and level,
`test/rust/roundtrip-rustc-1.99.1-known.edn` (`-iface-known.edn` for the interface level), a map
from file to the reason it differs or is skipped, read with the Clojure reader; the round trip fails
on a difference not listed and on a listed one that no longer occurs. Files rustc's parser rejects
alone are skipped there (rust-lisp D20).

### 11.6 The command (a sketch for the build)

```
bin/repr-rust helper                       build tools/rsdump (§10.4)
bin/repr-rust corpus fetch|list            the apks, checked by their hashes; the corpus's files
bin/repr-rust dump [--full] FILE...        run the helper
bin/repr-rust read [--full] FILE...|--corpus C   convert into .tmp/repr-rust/forms/
bin/repr-rust print [--layout lines|pretty|full] FILE.ae...
bin/repr-rust roundtrip [--iface] [--text] CORPUS
bin/repr-rust roundtrip --code CORPUS      the lowered and code levels (RR6)
bin/repr-rust test                         the spec's cases (test/rust/)
```

`arbace.repr.rust` is evaluated from the checkout, not embedded in the executable (as
repr/haskell's HR1).

## 12. Coverage plan

### 12.1 Milestones

| | what | judge |
|---|---|---|
| RR1 | the helper (`dump`, `tree`, `lines`, §10.3's facts); the converter and the lines printer in `:lines`; std's five crates (1,074 files) | tree level and lines |
| RR2 | the interface level over RR1's corpus: rustdoc's JSON of `core`, `alloc`, `std`, `proc_macro`, `test` | interface level |
| RR3 | `:full`: the helper's tokens and comments, the converter's metadata, the full printer | text level |
| RR4 | the rest of `library/`, the compiler, a pinned crate set: editions 2015-2021, the pretty layout for forms without positions | tree, interface and text levels |
| RR5 | behaviour: rustc's UI run-pass programs printed and run, std built from its printed files | behaviour level |
| RR6 | lowered and code levels (HIR, MIR, objects); expression files of `include!` (RSS6) | those levels |

By the survey's estimate (§5.3), RR1-RR3 over std's five crates are about 7-10 working days.

### 12.2 Editions and unstable syntax

Every edition's and every unstable feature's syntax that rustc 1.99.1 parses is in the grammar
(rust-lisp D14: all of Rust's syntax); the corpus decides what is tested first. std is edition 2024
and uses unstable syntax no stable crate may (`const` traits and impls, `[const]` bounds, `final`
methods, `super let`, `impl(crate)` restrictions, `try` blocks, `builtin #` forms, `deref!`
patterns, never patterns, `gen` blocks), all of which the helper parses since rustc parses it on
every channel. **First (RR1-RR3):** edition 2024 and std's unstable syntax. **Then (RR4):** editions
2015 (anonymous parameters, bare trait objects `(:bare ...)`, `dyn` and `async` as names), 2018,
2021 (`gen` as a name); `stdarch`'s macro-heavy files. **Later (RR5):** what the UI suite adds
(delegation, contracts, unsafe binders, guard patterns, `&pin`).

### 12.3 rustc 1.99.1's AST

The variants of the parsed tree (`rustc_ast/src/ast.rs`, 4,492 lines in `rustc-dev`'s sources) and
where this spec writes each; the converter reports any variant it meets that is not listed.

| type | variants | spec |
|---|---|---|
| `ItemKind` | `ExternCrate Use Static Const ConstBlock Fn Mod ForeignMod TyAlias Enum Struct Union Trait TraitAlias Impl MacCall MacroDef Delegation DelegationMac`; `GlobalAsm` (made by expansion only) | §4 |
| `AssocItemKind`, `ForeignItemKind` | `Const Fn Type MacCall Delegation DelegationMac`; `Static Fn TyAlias MacCall` | §4.6, §4.8 |
| `ExprKind` | `Array ConstBlock Call MethodCall Tup Binary Unary Move Lit Cast Type Let If While ForLoop Loop Match Closure Block Gen Await Use TryBlock Assign AssignOp Field Index Range Underscore Path AddrOf Break Continue Ret OffsetOf MacCall Struct Repeat Paren Try Yield Yeet Become UnsafeBinderCast`; `InlineAsm FormatArgs IncludedBytes DirectConstArg` (made by built-in macros), `Err Dummy` (error recovery's) | §5 |
| `StmtKind` | `Let Item Expr Semi Empty MacCall` | §5.6 |
| `PatKind` | `Missing Wild Ident Struct TupleStruct Or Path Tuple Box Deref Ref Expr Range Slice Rest Never Guard Paren MacCall`; `Err` | §5.7 (`Missing` is a parameter without pattern, §4.3) |
| `TyKind` | `Slice Array Ptr Ref PinnedRef FnPtr UnsafeBinder Never Tup Path TraitObject ImplTrait Paren Infer ImplicitSelf MacCall CVarArgs FieldOf`; `Pat View DirectConstArg` (made by the built-in macros `pattern_type!`, `view_type!`, `direct_const_arg!`: before expansion they are those calls), `Dummy Err` | §6 |
| `LitKind` (token) | `Bool Byte Char Integer Float Str StrRaw ByteStr ByteStrRaw CStr CStrRaw`; `Err` | §3.6 |
| `GenericParamKind`, `GenericArg`, `GenericArgs`, `WherePredicateKind`, `GenericBound` | `Lifetime Type Const`; `Lifetime Type Const`; `AngleBracketed Parenthesized ParenthesizedElided`; `BoundPredicate RegionPredicate`; `Trait Outlives Use` | §4.4, §6 |
| `UseTreeKind`, `VariantData` | `Simple Nested Glob`; `Struct Tuple Unit` | §4.8, §4.5 |
| `AttrKind`, `AttrArgs`, `MacStmtStyle` | `Normal DocComment`, `Synthetic` (inserted by the compiler, never parsed); `Empty Delimited Eq`; `Semicolon Braces NoBraces` | §8, §5.6 |
| `BinOpKind`, `AssignOpKind`, `UnOp` | `Add Sub Mul Div Rem And Or BitXor BitAnd BitOr Shl Shr Eq Lt Le Ne Ge Gt`; the ten assignments; `Deref Not Neg` | §5.2 |
| `CaptureBy`, `ClosureBinder`, `CoroutineKind`, `SelfKind`, `RangeLimits`, `BoundPolarity`, `BoundConstness`, `BoundAsyncness`, `Safety`, `Defaultness`, `ImplPolarity` | the keywords and markers of §4-§6 | §4-§6 |

Not in 1.99.1 (rust-lisp's 1.101 has them): `ExprKind::GcaMacro`, `TyKind::GcaMacro`,
`ItemKind::TestBinderConstraints` (survey §2.6).

### 12.4 As built: RR1 (2026-10-17)

[RUST-NOTES.md](RUST-NOTES.md), "RR1", records the build: std's five crates, 1,074 files, equal at
the tree level (trees and the start lines of all 1,660,638 spans) with no known difference, in
about 45 s. What it changes in this text (RR-P1-RR-P3 there, decided as recommended: §9.1 and
§7.1 now say what RR-P2 and RR-P3 adopted):

- **The helper (§10.2, §10.3, §10.5)**: `dump -o DIR` writes `DIR/FILE.edn` per file; `tree -o DIR`
  writes `FILE.tree` and `FILE.lines` from one parse; `lines FILE` prints. The macro arguments'
  parse is keyed by the span of the call's open delimiter, made for the calls of the tree, of
  attributes' `= expr` and, recursively, of the expressions parsed from another call's tokens; the
  call's tokens for §7.1's check are the tree's own. A dump records the file's SHA-256 (computed by
  the helper) and absolute path. The version check reads `rustc_version_str()` (version and
  commit) and the loaded `librustc_driver-*.so`'s hash; `RUSTC_VERSION` holds the three as
  `version`, `commit`, `driver` lines, `apks.sha256` the two apks' hashes.
- **Meta items (§8.1)** are built from the attribute's tokens, as rust-lisp's `meta_inner`, and
  checked against the printer's tokens; the helper's `rsdump/meta` is not needed.
- **Macro calls (§7.1)**: named format arguments `name = value` are an assignment expression with
  the same tokens, so they stay forms (RR-P3); brace-delimited calls are always `(:tt "{...}")`;
  `(m!)` and `(m! [])` for empty calls; a nested call kept as `(:tt ...)` matches the source's
  group by its text in the check.
- **Lines (§9.1, S21, S22)**: every form that stands for a node with a span carries its start line;
  a token without an element gets its line when the printer, which puts it on the line of the
  latest span before it, would not: `:rs/brace` (a block body's `{`), `:rs/close` (a call's or an
  attribute's closing delimiter, and the `)`, `>` or `]` that an empty span of a missing return
  type or `where` clause follows on its line), `:rs/paren`; a binary operator's head has its line
  unless it is on its right operand's (when that begins a later line) or the current one, and
  stays flat in a chain only when its inner operators need no line; other heads have theirs when
  it is not the current one; a head that is a list is written flat with its lines as metadata
  (RR-P2). `:rs/open` is not needed by the corpus.
- **Implicit parentheses (§5.2, S12)**: the converter applies the printer's own decisions (the
  file printed once with them logged); a function item's return type takes a bound list without
  parentheses; no `(:noparen ...)` in the corpus.
- **Items (§4.5)**: a tuple struct's `where` clause after its fields, `(struct (P T) [T] (where
  ...))`, Rust's order.
- **Comments (§9.3)**: plain comments are not written in mode `:lines` yet; they come with the
  lexer's comments at RR3 (RR-P1).
- **Files (§2.1, §2.3)**: outside a corpus a file's crate is `main` and its namespace `rs.main.` and
  its path (`bin/repr-rust read --crate --crate-type --ns-root` set them, as the worked example's
  `rs.sample.shapes`); `src/lib.miri.rs` is a root (miri's); the writer puts a list without a span
  of its own (a brace group of fields) where the text is, its first element with its line, where
  Appendix B's hand-written file begins the group on the next line (the same data).
- **Commands (§11.6)**: `bin/repr-rust helper`, `corpus fetch|list`, `dump`, `read`, `print`,
  `roundtrip`, `test` as sketched, without `--iface` and `--text` (RR2, RR3) and the pretty layout
  (RR4); `bin/repr-rust roundtrip && bin/repr-rust test` is the step `repr-rust` of `bin/gate
  --full`.

### 12.5 As built: RR2 (2026-10-17)

[RUST-NOTES.md](RUST-NOTES.md), "RR2", records the build: the interface level over RR1's corpus,
rustdoc's JSON (format version 61) of `core`, `alloc`, `std`, `proc_macro` and `test` from the
originals and from the printed files equal for all five crates, 79,371 items, without spans, with
no known difference; with the tree level, about 80 s. What it changes in this text (RR-P4 and RR-P5
there, open, built as recommended):

- **The normalization (§11.3)**: every `span` member removed (`"span":null` and
  `"span":{"filename":..,"begin":[l,c],"end":[l,c]}`, as keys), and also the spans rustc's `Debug`
  writes inside the texts of attributes (`"attrs":[{"other":"#[attr = CfgTrace([... span:
  core/src/os/mod.rs:18:5: 18:8 (#0) ...])]"}]`, from every `#[cfg]` that stays), each `PATH:L:C:
  L:C (#N)` written `_` (RR-P4): 50,757 of them over the five crates, whose columns the printer's
  layout changes. Nothing else is normalized: ids need none (rustdoc's JSON of the same sources is
  byte-identical run to run, and the printed files give the same items in the same order, so the
  same ids), and the external crates' paths are the same sysroot's.
- **The configuration (§11.3, §11.4)**: each crate's `:iface` in the corpus file: its root
  (`src/lib.rs`), the crates it takes from the host's sysroot as `--extern NAME=` their `.rmeta`
  (the direct dependencies of its `Cargo.toml` for the target; the sysroot's `.rlib`s are metadata
  stubs), its `--cfg` flags (std's `backtrace_in_libstd` of its `build.rs`; the features of rust's
  bootstrap for this target: std's `backtrace` and `panic-unwind`, proc_macro's default
  `rustc-dep-of-std`; test's `enable_unstable_features` is set by its `build.rs` only on a nightly
  or dev rustc) and environment (std's `STD_ENV_ARCH`); and `-Zforce-unstable-if-unmarked`, as rust's bootstrap
  documents the library (without it std's calls to its dependencies' `const fn`s are rejected).
- **The printed side (§11.3)**: the library tree mirrored as symbolic links, each corpus file a
  link to its printed file (a file not printed is missing, and its crate fails), so that the files
  `#[path]` reaches outside the corpus (`stdarch`, `portable-simd`, `backtrace`: RR4) and the
  Markdown that `include_str!` reads are the originals'.
- **The public items (§11.1)**: rustdoc's default, as §11.1 says; with `--document-private-items`
  all five are equal too (104,991 items; its ten runs 18 s on their own): RR-P5.
- **Commands (§11.6)**: `bin/repr-rust roundtrip --iface` (the tree level, then the interface
  level; not with `--only`), `make repr-rust-iface`; known differences
  `test/rust/roundtrip-rustc-1.99.1-iface-known.edn`, keyed by crate; `bin/repr-rust roundtrip
  --iface && bin/repr-rust test` is the step `repr-rust` of `bin/gate --full`, and `bin/repr-rust
  test` checks the normalization on the worked example and `test/rust/sample/attrs.rs`, and that a
  changed doc comment is found.

### 12.6 As built: RR3 (2026-10-18)

[RUST-NOTES.md](RUST-NOTES.md), "RR3", records the build: mode `:full` over RR1's corpus, each of
the 1,074 files printed back byte for byte (14,276,761 bytes), with no known difference, and the
printed files' trees and lines equal; about 55 s on its own, the gate's step `repr-rust` 145 s.
What it changes in this text (RR-P6-RR-P8 there, open, built as recommended):

- **The helper (§10.2, §10.3)**: `rsdump dump --full` adds `(rsdump/tokens [[KIND L C L2 C2]
  ...])`, `rustc_lexer`'s tokens of the file (after rustc's source map's normalization: a byte
  order mark removed, `\r\n` made `\n`) without white space, columns as spans count them, KIND
  `:c` (a plain comment), `:doc`, `:id`, `:lt`, `:lit`, `:p` (one punctuation character),
  `:shebang`, `:fm`, `:x`; positions, not byte ranges. Every dump (without `--full`) has
  `(rsdump/comments [[L C L2 C2] ...])`, the plain comments, for mode `:lines`' `;` comments.
- **The metadata (§9.2; RR-P6)**: not §9.2's named keys but, as Haskell forms' HR9/HR10,
  `:rs/pos {:span [l1 c1 l2 c2] :toks [[l c] ...]}` on every list, vector and symbol that stands
  for a node with a span: its span and the start of every printed token inside it and not inside
  an element's span, in order (a literal's and a keyword's are their parent's; the tokens of no
  span's on `rs/file`'s metadata); `:rs/text {[l c] "text"}`, the source's spelling of a printed
  token; `:rs/punct [[l c ","] ...]`, source punctuation the printer does not print;
  `:rs/drop [[l c] ...]`, a printed punctuation token the source does not have (RR-P7). No
  `:rs/apos` and no `:rs/paren` (parentheses are tokens like any other). On `rs/file`,
  `:positions :full`, `:rs/comments`, `:rs/space`, `:rs/tail`, `:rs/crlf`, `:rs/bom` as §9.2
  says; `:rs/space` holds the white space after the token ending at `[l c]` (or before the
  first, at `[1 1]`) only when it is not the gap rule's: spaces to the next token on its line,
  else newlines and spaces to its column.
- **The converter (§9.2)** pairs the printer's tokens (the same printing code, its tokens
  collected) with the source's in order and fails a file where they do not pair (as HR11); the
  forms are mode `:lines`' forms with that metadata: as data, all 1,074 files' forms equal
  mode `:lines`' but for `rs/file`.
- **The full layout (§9.4, 3)**: `bin/repr-rust print --layout full`, the printer's tokens at the
  forms' positions, a recorded spelling in place of the printer's text, the punctuation and the
  comments added, the white space from `:rs/space` or the gap rule, then the tail.
- **Comments in `:lines` (§9.3, RR-P1)**: every plain comment outside the text of `(:tt ...)`,
  `builtin #` and `(:rust ...)` forms is written in the forms text at its Rust line, `;text` at
  the end of a line with forms (a line comment's text after its `//`, each line of a block
  comment trimmed after a space), else `;;text` indented by half its column (RR-P8). The forms
  as data and their lines are RR1's, and the printed files RR1's byte for byte.
- **Commands (§11.6)**: `dump --full`, `read --full`, `print --layout lines|full`, `roundtrip
  --text` (after the tree level and, with `--iface`, the interface level) and `--text-only`,
  `make repr-rust-text`; known differences `test/rust/roundtrip-rustc-1.99.1-text-known.edn`;
  `bin/repr-rust roundtrip --iface --text && bin/repr-rust test` is the step `repr-rust` of
  `bin/gate --full`.

### 12.7 As built: RR4 (2026-10-18)

[RUST-NOTES.md](RUST-NOTES.md), "RR4", records the build: three new corpora, the rest of
`library/` (1,902 files), the compiler (2,167) and a pinned crate set (402 crates, 8,254 files,
editions 2015-2024), each at the tree, text and interface levels, and the pretty layout, judged at
the tree level over all four corpora; RR1-RR3's levels over std unchanged. What it changes in this
text (RR-P9-RR-P12 there, open, built as recommended):

- **The corpora (§11.4)**: `test/rust/corpus-rustc-1.99.1-library.edn` (every Cargo package of
  `rust-src`'s `library/` but the first corpus's files, and `rtstartup`'s two files, which no
  package holds), `corpus-rustc-1.99.1-compiler.edn` (every package of `usr/src/rustc/compiler`),
  both from the pinned `rust-src` apk; `corpus-crates-2026-10-18.edn`, the newest version of each
  of the 402 crates in the host's Cargo cache on 2026-10-18 (E9's 518 less the older versions:
  one namespace per crate), each `.crate` pinned by its SHA-256 in
  `test/rust/crates-2026-10-18.sha256` (crates.io's own checksums, checked against the index),
  copied from the host's cache (`REPR_RUST_CRATES`, else crates.io) into `.tmp/repr-rust/crates/`
  and extracted there. A crate of a corpus is a Cargo package: its name Cargo's, its edition its
  manifest's (2015 when none), its files those under its directory in no nested package; all
  made by `test/rust/corpus_make.ae` (`std`, `library`, `compiler`, `crates`).
- **Namespaces (§2.1; RR-P9)**: under `src/`, `lib` and `main` are dropped only as the crate's
  roots (`src/lib.rs`, `src/main.rs`) and a final `mod` only below them (`src/x/mod.rs` is `x`,
  `src/mod.rs` `mod`); `src/main.rs` beside a `src/lib.rs` keeps `main` (the binary is another
  crate of the package); `build.rs` is `build_script_build`, Cargo's name of the build script; a
  package nested in another package's directory, outside its workspace, has its namespaces under
  the enclosing crate's (`rs.rustc_codegen_cranelift.y`: the corpus's `:ns-crate`). Std's 1,074
  namespaces are unchanged; the rule as written made 11 clashes (`core_arch`'s and `core_simd`'s
  `src/mod.rs` against their `src/lib.rs`, `object`'s `build.rs` against `src/build/mod.rs`, the
  two codegen backends' build systems both named `y`...).
- **Editions (§12.2)**: 2015-2021 need nothing new in the forms: anonymous parameters, bare trait
  objects and `dyn`, `async`, `try` or `gen` as names print from the forms as they are; a struct
  literal whose path is a special head (`gen { .. }`, `async { .. }` before 2024 and 2018) is
  `((:path gen) (:braces ...))` as calls are; a lifetime whose name only a raw lifetime can write
  in the file's edition (`'r#mod`: the tree's `Lifetime` holds no rawness) is `(:lifetime
  "r#mod")`.
- **The helper (§10.3)**: a macro call whose arguments' parse leaves a delayed bug or raises a
  fatal error has no `rsdump/mac-exprs` entry (two files crashed the helper).
- **Lines (§9.1)**: `:rs/close` also on a closure's parameters (its closing `|`), a parenthesized
  argument list (`FnMut(A,\n)`), an empty module's body (its `}`) and `rs/file` (a file of no item:
  the line of its end), each before an empty span or the end; a missing `where` clause of a
  `const` item marks the type, which its empty span follows; the lifetime that ends a bound list
  gets the line of the empty span after it; `.await`'s head has its line; a parameter's pattern
  with attributes has its own span's line; an implicit parenthesis whose `(` is on an earlier line
  than its expression keeps that line (a binary operator before it stands there).
- **Macro arguments (§7.1)**: the printer's tokens of a call are compared after the lexer's
  floats too: `x.0 .0` prints `x.0.0`, which lexes `0.0`, so such a call is `(:tt ...)`.
- **The pretty layout (§9.4, 2; RR-P11)**: `bin/repr-rust print --layout pretty`, of any forms,
  whose positions it ignores: every item, statement, arm, field, variant, attribute and `where`
  predicate on a line of its own, indented by nesting, a `}` that closes them on a line of its own,
  then each line over 100 columns broken at its first delimited group that reaches past the limit
  (else its widest), the group's elements one per line, one level deeper (consistent breaking,
  rustfmt's vertical style). It is not `-Zunpretty=normal` byte for byte: §9.4's "as
  -Zunpretty=normal prints" is its model; its judge is the tree level, the printed files' trees
  equal to the originals' (their lines are the layout's own).
- **The interface level (§11.3; RR-P12)**: for the library, `core` and `std` documented again
  with the files `#[path]` reaches outside them printed (`core_arch`, `core_simd`, `std_float`,
  `backtrace`), and every other library crate against the host's sysroot (its dependencies but
  the optional ones, no features); for the compiler, each crate against `rustc-dev`'s builds of its
  dependencies (`:libdir "rustc-dev"`, each extern by its file, `-L dependency=` that directory),
  its default features, and the `env!` values rust's build sets; for the crate set, Cargo:
  `cargo doc` in a workspace of its own, every crate without its default features (two with one
  feature each), its dependencies locked by `test/rust/crates-2026-10-18.lock` and read from a
  directory source of their `.crate` files checked by the lock's SHA-256 (no index, none of the
  host's Cargo state); each crate's rustdoc command as `cargo -vv` prints it, run on the originals
  and on the mirror. The crates no level can document are named, with the reason, in the corpus
  files (`;; not documented: ...`). Known differences: one file of the crate set rustc's parser
  rejects alone (all levels) and `nom`'s interface (an attribute's token spacing, which rustdoc
  writes for the impls a derive copies it to: N3).
- **Commands (§11.6)**: `bin/repr-rust roundtrip [...] [--pretty | --pretty-only] [CORPUS... |
  all]`, `corpus fetch CORPUS` (the crate set's `.crate` files too), `print --layout pretty`;
  `make repr-rust-pretty`, `make repr-rust-all`; known differences per corpus and level
  (`roundtrip-CORPUS-known.edn`, `-iface-known`, `-text-known`, `-pretty-known`). The gate's step
  `repr-rust` is `bin/repr-rust roundtrip --iface --text --pretty && bin/repr-rust test` (std);
  `REPR_RUST_CORPORA=all` adds RR4's corpora (RR-P10).

### 12.8 As built: RR5 (2026-10-18)

[RUST-NOTES.md](RUST-NOTES.md), "RR5", records the build: the behaviour level, rustc's UI suite as
a fifth corpus (19,942 files of `tests/ui`) at the tree, text and pretty levels, its 3,718 run-pass
programs built and run from their originals and from their printed files in the three layouts,
and std (with core, alloc and the rest of `library/`, 2,976 files) built from its printed files,
the same programs run against it; RR1-RR4's levels unchanged. Every file of the suite but three is equal at the tree, pretty and
text levels (19,939; the three nest deeper than the tools' host evaluates); the programs behave the
same in mode `:full` (3,690, 27 nondeterministic, 1 not judged), in mode `:lines` (3,685, 10 of them
once columns are normalized; 5 known differences) and in the pretty layout (3,666; 25 known), and
against std built from its printed files (3,683, 0 differ). What it changes in this
text (RR-P13-RR-P17 there, open, built as recommended):

- **The corpus (§11.4)**: `test/rust/corpus-rustc-1.99.1-ui.edn`, made by `bin/repr-rust corpus
  make-ui` from `rustc-1.99.1-src.tar.xz` (`:tarball`: its URL, SHA-256, the directories
  extracted): every file of `tests/ui` but those rustc's parser rejects alone (`:rejected`) and
  those whose namespaces clash (`:clash`), one crate per edition (a file's crate is the one whose
  `:files` hold it, else by directory), every file a root (`:crate-type bin`); `:programs`, each
  revision of a run-pass, run-fail or run-crash test with its directives (compiletest's `//@`
  lines: edition, compile and run flags, `exec-env`, `rustc-env`), those left out counted by
  reason (RR-P15).
- **The behaviour level (§11.1, 5; §11.3)**: `bin/repr-rust roundtrip --run`: each program built
  (`RUSTC_BOOTSTRAP=1`, RR-P16; compiletest's lint flags; its source root remapped) and run from
  the original and, in a mirror of the source tree, from every layout the run printed; stdout,
  stderr and exit status compared, after compiletest's normalizations (thread ids, a test
  harness's time) and, when they differ, the columns (mode `:lines`) or lines and columns (the
  pretty layout) of the source locations they print (RR-P13); a program whose baseline varies
  between runs is nondeterministic, counted and not judged. `--std`: std built by Cargo's
  `-Zbuild-std` from `rust-src`'s library and from its printed files of mode `:lines`, the programs
  built from their originals against each. Known differences
  `test/rust/roundtrip-CORPUS-run-LAYOUT-known.edn`, `roundtrip-rustc-1.99.1-std-std-known.edn`
  (none).
- **Namespaces (§2.1; RR-P17)**: every character of a path segment a name cannot hold is `_`, as
  `.` and `-` were (std's and RR4's namespaces unchanged).
- **Attributes (§8.2)**: an attribute form before an element at a fixed place (a callee, a
  receiver, an operator's operand, an index, a guard's condition, a constant's value, an array
  type's length, a discriminant) attaches to that element, as in a sequence; a statement's
  attributes before its leftmost operand stand on their own lines before the statement's.
- **Lines (§9.1)**: `:rs/open` (the `<` of generic arguments on a later line), `:rs/close` also on
  generic arguments, a qualified path's `>` and a module of inner attributes and no item (its
  `}`), `:rs/brace` on `gen`, `async`, `try`, `unsafe` and labeled blocks; a name on a later line
  than its item's keyword has its line; in the lines' comparison a dummy span against a real one
  is N1's (§11.2).
- **The forms**: a macro path with generic arguments is `(:turbofish m! T...)`; `union` is a
  special head in expressions (`((:path union) (:braces))`); `reuse impl Trait for T {...}` and
  items of a type the parser's recovery made are `(:rust ...)` (§4.10); `for<>`, an empty binder,
  is `(for [] ...)`; a closure parameter's `_` type written out is `(:has x _)`; `rs/file`'s
  `:frontmatter-line` (§2.3); `:rs/crlf` the vector of the lines that end in `\r\n` in a file of
  both line ends (§9.2).
- **The helper (§10.3)**: `rsdump/tokens` and `rsdump/comments` lex a guarded string's prefix
  `#"` before edition 2024 as `#` and what follows, as rustc's lexer does.
- **Commands (§11.6)**: `bin/repr-rust corpus make-ui`, `corpus fetch rustc-1.99.1-ui`,
  `roundtrip [...] [--run | --run-only] [--std]`, `all` the five corpora; `make
  repr-rust-behaviour`, `make repr-rust-all`. The gate's step `repr-rust` is unchanged;
  `REPR_RUST_CORPORA=all` adds RR4's corpora and the UI suite, `REPR_RUST_CORPORA='all --run
  --std'` the behaviour level too (RR-P14).

## 13. Decisions and open proposals

### 13.1 Decisions taken while writing (S1-S34)

Each on the terms of RS1-RS17; the user may revise any (§13.2 asks about the ones with real
alternatives).

| S | decision | reason |
|---|---|---|
| S1 | The forms are the parsed tree before expansion only: macro calls, `#[cfg]` and paths unresolved (§1.2, 1) | every level is judged by rustc from the parsed tree; a bridge needs no meaning (RS1, rust-lisp I2) |
| S2 | A mechanical map between `.rsl` and Rust forms for the AST both rustc versions share (§1.3) | RS2: rust-lisp's grammar respelled, not a new one |
| S3 | Namespaces `rs.` + crate + the file's path (`src/` dropped, `mod`, `lib`, `main` dropped), `rs.main.` + path for corpus programs; a clash is a converter error (§2.1) | one namespace per file (RS5) needs no crate graph; std's 1,074 files map without a clash (E2) |
| S4 | Paths as qualified symbols, `(:global ...)`, `(:path ...)`, `(:qself ...)` (§3.3) | RS4; all of std's multi-segment paths read back (E2) |
| S5 | The name rule (§3.2), `(:name "t")` for the one identifier it refuses in practice, `nil` | RS3: every bare name checked by reading it back |
| S6 | Lifetimes and labels as `'a`, read as `(quote a)`; `(:lifetime "t")` when that does not read back (§3.4) | unambiguous once characters are `\c` (survey §3.1) |
| S7 | One head `:has` for every `:` of rust-lisp (bounds, typed patterns and parameters, supertraits) (§3.5) | the mechanical map (S2); RSS1 |
| S8 | Brace groups as `(:braces ...)`, never maps, `(:braces)` for empty braces (§3.5) | rust-lisp D13's odd lengths, shorthand and modifiers; one canonical spelling (I6) |
| S9 | `^`, `^=`, `/=` as `bit-xor`, `bit-xor=`, `div=`; `@` as `:at` (§3.5) | the reader's misreads and refusals (E1); hyphens and `=` make words no Rust name can be; RSS2 |
| S10 | Literals keep their tokens in both modes: Clojure literals and value forms where the canonical spelling is the token, `(:lit "text" suffix?)` otherwise, `(:lit N suffix)` for suffixed decimals (§3.6) | the tree holds spellings; 87.2% of std's literals need no text (E2); RSS4 |
| S11 | Top-level forms are rust-lisp's forms with the first element qualified, `rs/pub` and `rs/unsafe` included; `rs/macro-call` for item macro calls, `rs/rust`, `rs/attr`, `rs/attr!`, `rs/doc`... (§4.1) | RS5 with I1's order kept: visibility before the keyword; RSS3 |
| S12 | Implicit `Paren` nodes by rust-lisp's §10 (`FixupContext`), every other as `(:paren e)` (§5.2) | the tree holds every parenthesis |
| S13 | Field chains as symbols only on a one-segment name, `(. path f)` on a longer path (§5.3) | a qualified symbol is a path (§3.3) |
| S14 | `builtin #` forms as `(:builtin name (:tt "..."))` (§5.8) | rust-lisp's printer; rare outside macro bodies |
| S15 | `TyKind::FieldOf` as `(:builtin field_of (:tt ...))` (§6); `TyKind::View`, `Pat` and `DirectConstArg` need no form | 1.99.1 parses `builtin # field_of`, which rust-lisp's printer refuses; the others are made by expansion (`rustc_builtin_macros`) |
| S16 | Macro arguments and meta items as forms only when the printer's tokens equal the helper's, and in `:lines` the lines are reproducible (`:rs/close` aside) (§7.1, §8.1) | RS6; rust-lisp D10's check, made without a Rust lexer in Arbace |
| S17 | `(:tt "text")` is the source text of the token tree, comments and white space included (§7.2) | the tokens' lines come free; no token printer for raw trees |
| S18 | `macro m(args) { body }` as two `:tt` groups (§7.3) | the source's shape; the parser joins them |
| S19 | Attribute and doc forms are transparent elements attached to the next element (§8.2) | RS7 (a); the 13 node types that carry attributes |
| S20 | `:lines` by construction: every Rust line's tokens on one printed line; no line directive (§9.1, §9.4) | Rust has none; nothing in Rust's lexer needs more lines than the source's |
| S21 | Line metadata only where an element's node starts on another line than the element printed before it; `:rs/breaks` for literals and keywords (§9.1) | Haskell forms' HR3, generalized to method chains |
| S22 | Token-line keys (`:rs/brace`, `:rs/open`, `:rs/close`, `:rs/paren`) and `:full`'s position keys as a per-node table kept with the rustc version (§9.1, §9.2) | rustc's spans change across releases (Haskell forms' S11) |
| S23 | Columns as rustc's spans count them: characters from 1, a tab one column, end exclusive (§9.2) | rustc's `Debug` of spans (survey E3) |
| S24 | The helper written anew from the survey's experiment, `-Zunpretty=ast-tree` and rust-lisp's `check.rs` its models (§10.1) | nothing to vendor; RSS8 |
| S25 | The transcoder's quirks fixed against the corpus: `r#` only before an identifier character (§10.2) | 239 identifiers `r` misread by the survey's helper (E2) |
| S26 | The helper adds three facts: macro arguments parsed as expressions, meta items, tokens and comments (§10.3) | the converter decides with rustc's own parse, as rust-lisp did inside rustc |
| S27 | The converter reads the Rust file for `:tt` texts and `:full`'s text (§10.3) | as repr/go's `:text-dir`; the dump stays generic |
| S28 | One helper with verbs `dump`, `tree`, `lines`; no comparator program (§10.2) | the levels compare text (Haskell forms' S15) |
| S29 | The tree level in `:lines` normalizes exactly D20's four (N1-N4), lines compared apart (§11.2) | the fewest normalizations; literal spellings stay compared |
| S30 | The interface level by rustdoc's JSON without spans, all of a crate's printed files in place at once (§11.3); RSS7 | `.rmeta` is a text-level check (survey E7) |
| S31 | `:shebang` and `:frontmatter` on `rs/file` in both modes (§2.3) | stripped before parsing, so in no tree, but part of the file |
| S32 | Converted forms not tracked (§2.1) | as Go forms' and Haskell forms'; RSS10 |
| S33 | `(:braces)` for every empty brace pair rust-lisp writes `{}` (a body, a struct, a module) (§4) | I6: one spelling for a brace group, empty or not |
| S34 | One-element lists stay rust-lisp's (`(f)` a call, `(S)` a tuple-struct pattern); `()` the unit | rust-lisp's grammar unchanged |

### 13.2 Open proposals for the user (RSS1-RSS10)

Each with the recommendation this text follows.

- **RSS1 The word for `:`.** (a) One head `:has` for bounds, typed patterns and parameters,
  supertraits and associated bounds, a mechanical respelling of rust-lisp's `(: ...)` (the survey's
  sketch); (b) two heads, `:bound` (bounds, supertraits, outlives) and `:typed` (patterns and
  parameters), clearer to read but no longer one token for one; (c) Haskell forms' `:sig`.
  *Recommended: (a)*, which keeps S2's mechanical map.
- **RSS2 `/=`.** (a) `div=`, after `bit-xor=` (S9); (b) `(:op "/=" a b)` as Haskell forms' escape for
  any operator; (c) Go forms' assignment shape `(set! a / b)` for every compound assignment.
  *Recommended: (a)*: 13 uses in std, and every other compound operator reads as Rust spells it.
- **RSS3 The head of an item macro call.** (a) `(rs/macro-call path! args...)`; (b) `(rs/! path
  args...)`; (c) a qualified macro name `(rs/thread_local! ...)`, which cannot hold a path.
  *Recommended: (a).*
- **RSS4 Literals.** (a) S10: Clojure literals and value forms where the canonical spelling is the
  token, `(:lit "text")` otherwise (87.2% of std's literals without text); (b) every literal but
  booleans and plain decimals as `(:lit "text")`, no canonical spelling to define; (c) values only,
  with the spelling in metadata on the parent (`:rs/atext`, as Haskell's `:full`), so `:lines` would
  need a normalization of literal symbols at the tree level. *Recommended: (a).*
- **RSS5 Namespaces of files outside `src/` and of `#[path]` files.** (a) S3: by the file's path
  in the crate's directory; (b) by the module path the crate's `mod` items give, which needs the
  crate graph and gives one file several names (`#[path]` in `cfg_select!` arms). *Recommended:
  (a).*
- **RSS6 `include!`d expression files.** (a) Later (RR6): `(rs/file ... :kind :include-expr)` and one
  `(rs/expr e)` form; (b) never: kept as Rust beside the forms. *Recommended: (a)*; std's five crates
  need none (all 1,074 files parse as items, E2).
- **RSS7 The interface level.** (a) rustdoc's JSON without spans per crate, dependencies from the host
  sysroot (§11.3; E4); (b) `.rmeta` compared with spans normalized, which needs a decoder of rustc's
  metadata; (c) none until the behaviour level. *Recommended: (a).*
- **RSS8 Licensing of the helper.** (a) Written anew, linking `rustc_private` crates dynamically
  (nothing vendored), the survey's experiment its seed; (b) vendor parts of rust-lisp's
  `printer.rs` (MIT OR Apache-2.0, LLM-generated in a fork) into the helper, with the origin in
  doc/ARBACE.md and the licenses in LICENSE.md. *Recommended: (a)*: the converter in Arbace does
  rust-lisp's printer's job.
- **RSS9 `:lines` metadata for method chains.** (a) S21: each chain link's method name carries its
  line when it starts one (rustfmt's one-call-per-line chains); (b) normalize the lines of path
  segments' spans at the tree level, so chains need no metadata, at the cost of the lines of those
  spans (they reach no panic message: the call's span starts at the receiver). *Recommended: (a)*,
  the strict check, unless RR1 shows the metadata to dominate the forms.
- **RSS10 Held forms.** (a) Converted forms not tracked until a crate is held by a recorded
  decision (as Go and Haskell forms); (b) commit std's forms now as a corpus. *Recommended: (a).*

## 14. Sources

- Arbace: [REPR-RUST-SURVEY.md](../REPR-RUST-SURVEY.md) (the decisions RS1-RS17, E1-E9),
  [haskell/SPEC.md](../haskell/SPEC.md) (structure, §3, §7, §8, §9, HR3, HR8),
  [go/SPEC.md](../go/SPEC.md) (principles, §4.4, §7.5, §10), [go/PRINTER-NOTES.md](../go/PRINTER-NOTES.md)
  ("Mode :full as built"), [NAMING-SURVEY.md](../NAMING-SURVEY.md), [ALPINE.md](../ALPINE.md); main at
  `73e35434`; `test/haskell/reader_samples.ae` (the model of `test/rust/reader_samples.ae`).
- rust-lisp, `/root/rust-lisp` (github.com/arbace/rust-lisp), branch `rust-lisp`, commit
  `f28e0de8c2a8a8883fa8a6d4a2c9697e6ea0d280` (2026-10-05), forked at
  `602727f26878dcf9eb2999e2f58e5df1269ff47e` (rust-lang/rust master, 1.101.0), read only:
  `rustlisp/SPEC.md` (I1-I6, §1-§13, S1-S8), `rustlisp/DESIGN.md` (D1-D20),
  `compiler/rustc_parse/src/lisp/printer.rs`, `printer_expr.rs` (`mac_call`, `mac_args_as_exprs`,
  `field`, the unsupported variants), `shape.rs`.
- Rust 1.99.1, Alpine edge: `rust-1.99.1-r0` (installed; `rustc 1.99.1 (58cf8f9c3 2026-10-13)`,
  `/usr/lib/librustc_driver-61690d3e4c729118.so`), `rustc-dev-1.99.1-r0`, `rust-src-1.99.1-r0`
  (fetched by the survey into `.tmp/repr-rust-survey/dl/`, not installed; SHA-256 in §10.4): in
  `rust-src`, `usr/src/rust/library/{core,alloc,std,proc_macro,test}/`,
  `usr/src/rustc/compiler/rustc_ast/src/ast.rs`, `token.rs`, `rustc_ast_pretty/src/pprust/`,
  `rustc_lexer/src/lib.rs`.
- The Rust reference (lexical structure: identifiers, tokens, literal escapes, raw identifiers;
  attributes): https://doc.rust-lang.org/reference/; rustdoc's JSON output:
  https://doc.rust-lang.org/nightly/rustdoc/unstable-features.html; the rustc dev guide on
  `rustc_driver` and `RUSTC_BOOTSTRAP`: https://rustc-dev-guide.rust-lang.org/.

## Appendix A: the experiments

Scratch under `.tmp/repr-rust-spec/` (not tracked); the scripts and inputs that matter are
committed in `test/rust/`. Stage 0 is main's `target/seed/amd64/arbace` (scripts run as `arbace
FILE.ae ARGS`), rustc the host's 1.99.1, the helper the survey's `rsdump` (survey, Appendix A,
built in `.tmp/repr-rust-survey/e3/` against `rustc-dev-1.99.1-r0`), std's sources extracted from
the survey's `rust-src-1.99.1-r0.apk` (its SHA-256 checked, §10.4).

**E1: Arbace's reader on Rust's lexemes and the spec's spellings.** `test/rust/reader_samples.ae`
(a copy of `test/haskell/reader_samples.ae`) reads each line of `test/rust/reader-samples.txt` (291
samples in nine groups) as the text of a vector and prints the forms read, with their classes, or
the reader's error; the output is `test/rust/reader-samples.out`. The results the spec rests on:

| sample | read as | used in |
|---|---|---|
| `std::io::Write`, `::core::mem`, `Self::Item`, `(: T Ord)`, `(:: size_of T)`, `/=`, `(/= a b)`, `:/=` | `Invalid token` | §3.3, §3.5 |
| `#[derive Debug Clone]`, `#tt(a, b)` | `Reader tag must be a symbol`, `No reader function for tag tt` | §3.5 |
| `{:x :y 2}`, `{pub :x f64}` | `Map literal must contain an even number of forms` | §3.5 |
| `1u8`, `0x_ff`, `0o17`, `0b1010`, `1_000_000`, `2.5f32`, `08` | `Invalid number` | §3.6 |
| `"\x41"`, `"\u{1F600}"`; `'\n'`; `\😀` | `Unsupported escape character`, `Invalid unicode escape`; `Unsupported character` | §3.6 |
| `r#"x"#`, `^`, `^=` | `Unmatched delimiter` | §3.5 |
| `(^ a b)`, `(^= a b)`, `(@ x p)`, `x@y` | `(b)`, `(a b)`, `((arbace.core/deref x) p)`, two forms | §3.5 |
| `'a'`, `b'a'`; `r"a\n"`, `b"bytes"`, `c"x"`; `"\01"` | `(quote a')`, the symbol `b'a'`; two forms each; U+0001 | §3.5, §3.6 |
| `1e3`, `1.`, `007`, `0xFF`, `100000000.0`, `3.14159265358979323846264338327950288` | `1000.0`, `1.0`, `7`, `255`, `1.0E8`, `3.141592653589793`: the spelling lost | §3.6 |
| `340282366920938463463374607431768211455`, `9223372036854775808`, `-9223372036854775808` | BigInts (printed with `N`), a long | §3.6 |
| `nil`, `true`, `false` | literals | §3.2 |
| `foo`, `_`, `Self`, `self`, `super`, `crate`, `r#type`, `r#match`, `r#nil`, `é`, `naïve`, `δx`, `quote` | symbols, as written | §3.1 |
| `std/io`, `std.io/Write`, `a.b.c.D/e`, `Self/Item`, `u8/MAX`, `crate.a/b`, `super.super/x`, `e/*`, `std.io/*`, `std.r#type/x`, `r#type.x/y`, `x/r#type`, `foo/nil`, `foo/true`, `std/println!`, `core.ptr/addr_of!` | qualified symbols: the namespace and the name as §3.3 splits them | §3.3 |
| `self.x.0`, `a.0.1`, `self.r#type`, `.push`, `.await`, `.match`, `.use`, `.r#type`, `println!`, `vec!` | symbols, as written | §3.1, §5.3 |
| every other operator of §3.5's last paragraph, `&mut`, `&raw`, `&pin`, `*const`, `*mut`, `&self`, `bit-xor`, `bit-xor=`, `div=` | symbols, as written | §5.2 |
| `'a`, `'static`, `'_`, `'outer`, `'r#mod` | `(quote a)` ... | §3.4 |
| `\a`, `\'`, `\\`, `\"`, `\newline`, `\tab`, `\u0000`, `\u007f`, `é`, `\é`, `\あ` | characters | §3.6 |
| `"a\nb"`, `"\u0000"`, `"\u001b[0m"`, `"😀"` | strings of the characters meant (the last two UTF-16 units, one code point) | §3.6 |
| every form of §3-§8's tables (`(:global ...)`, `(:path ...)`, `(:qself ...)`, `((:path quote) a)`, `(:turbofish ...)`, `(:has ...)`, `(:attr ...)`, `(:attr! ...)`, `(:doc ...)`, `(:doc-block! ...)`, `(:tt ...)`, `(:braces ...)` with `:0`, `:r#type` and modifiers, `(:at ...)`, `(:lit ...)` with numbers and texts, `(:char 128512)`, `(:raw ...)`, `(:bytes "ÿ\u0000")`, `(:byte ÿ)`, `(:cstr ...)`, `(:rust ...)`), the top-level forms (`ns`, `rs/file`, `rs/pub`, `rs/fn`, `rs/macro_rules!`, `rs/macro-call`, `rs/attr!`...), metadata maps | one form each, as written | §3-§8 |

**E2: the spec's lexical rules over std.** The survey's helper dumped std's five crates (1,074
files, edition 2024) into `.tmp/repr-rust-spec/dumps/` in 5.8 s (16 jobs), 127 MB.
`test/rust/census.ae` read every dump with Arbace's reader (all 1,074 read) and counted, for every
identifier, path and literal of the trees, the spec's spelling, reading back every bare spelling;
the token streams' literals (macro arguments) are counted apart. Results (§3.2, §3.3, §3.6,
§5.2, §5.7, §8.2 cite them):

- identifiers outside token trees: 394,759, all bare by §3.2 (no `nil`); 1,719 tuple indices;
  `quote` 15 times, as a value or a binding;
- paths: 228,564; 180,593 with one segment, 22,375 with two, 5,570 with more; the 27,945
  multi-segment spellings all read back as their `ns/name` symbol but `use ::{...}`'s; 2 global;
  19,860 with generic arguments on the last segment only, 166 on another;
- literals outside token trees: 33,174 (integers 25,107, strings 4,777, booleans 1,571, floats
  763, characters 453, bytes 225, byte strings 136, raw strings and raw byte strings 123, C strings 19); bare 25,972,
  value forms 2,972, `(:lit "text")` 4,230, as §3.6 details; inside token trees 47,394 more
  (41,577 canonical strings, 4,537 bare integers);
- operators: `^` 17, `^=` 17, `/=` 13; `@` patterns 39;
- attributes: 158,250 outer (146,250 `///` lines, 1 block doc comment), 12,044 inner; 15,142 macro
  calls, 884 macro definitions;
- the namespaces of §2.1 for the 1,074 files: no clash (`lib.miri.rs` gives `rs.core.lib_miri`);
- the survey helper's quirk: 239 identifiers `r` dumped as the symbol `r#0` (§10.2, S25).

The attribute-carrying nodes of §8.2 are those of `rustc_ast/src/ast.rs` with an `attrs` field;
§12.3's variants are its enums, listed from the same file, and the expansion-only ones found in
`rustc_builtin_macros` (`pattern_type.rs`, `view_type.rs`, `direct_const_arg.rs`).

**E3: the worked example.** `test/rust/sample/shapes.rs` (78 lines) compiles with `rustc
--edition 2024` and runs. Its forms, `test/rust/sample/shapes.ae`, were written by hand by this
spec, each form on its Rust line. `test/rust/read_forms.ae test/rust/sample/shapes.ae DUMP`
reads them with a line-numbering reader (16 forms), finds no symbol the name rule would not write
bare, and checks that the top-level forms after `ns` and `rs/file` stand on the start lines of the
file's inner attributes and of each item's outer attributes and the item, as the helper's dump of
`shapes.rs` gives them: `[1 2 4 5 7 8 9 15 20 30 36 43 47 59]`, equal.

**E4: rustdoc's JSON of `core`** (§11.3, RSS7). `RUSTC_BOOTSTRAP=1 rustdoc --edition 2024
--crate-name core --crate-type lib -Zunstable-options --output-format json core/src/lib.rs`, run
in the extracted `library/` (core needs `stdarch` and `portable-simd` beside it, from the same apk,
and no dependency): 28.5 s, 1.47 GB resident, a 68.8 MB JSON file of format version 61, 61,777
items. Then the same in a copy of the tree whose `core/src/option.rs` (3,054 lines) was replaced by
rustc's own printing of it (`-Zunpretty=normal`, 2,823 lines: other lines, other layout, plain
comments gone, the same tree, survey §4.3): the two JSON files differ (19 bytes in size), and are
equal once every `span` field is removed (`.tmp/repr-rust-spec/e4/cmp.py`). The interface level's
normalization is therefore exactly the spans, for a printed file whose tree is the original's.

## Appendix B: the worked example

`test/rust/sample/shapes.rs` (78 lines; rustc 1.99.1 compiles it with `--edition 2024`, and it
runs) and its Rust forms in mode `:lines`, `test/rust/sample/shapes.ae`, written by hand by this
spec, each form on its Rust line (blank lines omitted here; the file keeps them, Appendix A, E3).

```rust
//! Shapes: the spec's worked example (doc/rust/SPEC.md, Appendix B).
#![allow(dead_code)]

use std::collections::HashMap;
use std::fmt::{self, Display};

/// A shape.
#[derive(Debug, Clone, PartialEq)]
pub enum Shape {
    Circle(f64),
    Rect { w: f64, h: f64 },
    Empty,
}

pub struct Tally<'a> {
    pub counts: HashMap<&'a str, usize>,
    total: u32,
}

impl Shape {
    pub fn area(&self) -> f64 {
        match self {
            Shape::Circle(r) => std::f64::consts::PI * r * r,
            Shape::Rect { w, h: height } => w * height,
            Shape::Empty => 0.0,
        }
    }
}

impl Display for Shape {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{:?} of area {:.1}", self, self.area())
    }
}

impl<'a> Tally<'a> {
    fn add(&mut self, name: &'a str) {
        *self.counts.entry(name).or_insert(0) += 1;
        self.total /= 1;
    }
}

macro_rules! twice {
    ($e:expr) => { $e * 2.0 };
}

fn largest<T>(xs: &[T]) -> Option<T>
where
    T: PartialOrd + Copy,
{
    let mut it = xs.iter();
    let mut best = *it.next()?;
    for &x in it {
        if x > best { best = x; }
    }
    Some(best)
}

fn main() {
    let shapes = vec![Shape::Circle(1.5), Shape::Rect { w: 2.0, h: 3.0 }, Shape::Empty];
    let mut tally = Tally { counts: HashMap::new(), total: 0 };
    for s in &shapes {
        tally.add(match s { Shape::Circle(_) => "circle", _ => "other" });
    }
    let areas: Vec<f64> = shapes.iter().map(|s| s.area()).collect();
    let total = areas.iter().copied().sum::<f64>() + twice!(1.0);
    let mask = 0x_ffu8 ^ 0b1010;
    let top = u8::MAX as u32 / 2;
    let n @ 1..=9 = 3 else { return };
    let c = 'é';
    let _ = ::core::mem::size_of::<Shape>();
    println!("{} {:?} {} {} {} {}\n", shapes[0], largest(&areas), mask, top, n, c);
    println!(
        "{} {}",
        r#"raw "q""#.len() + b"ab".len() + tally.counts.len(),
        total
    );
}
```

```clojure
(ns rs.sample.shapes (:require [arbace.rust :as rs])) (rs/file :path "shapes.rs" :crate shapes :edition 2024 :positions :lines :config {:rustc "1.99.1" :crate-type bin}) (rs/doc! " Shapes: the spec's worked example (doc/rust/SPEC.md, Appendix B).")
(rs/attr! allow dead_code)
(rs/use std.collections/HashMap)
(rs/use (std/fmt self Display))
(rs/doc " A shape.")
(rs/attr derive Debug Clone PartialEq)
(rs/pub enum Shape
  (Circle f64)
  (Rect (:braces :w f64 :h f64))
  ^{:line 12} Empty)
(rs/pub struct (Tally 'a)
  ^{:rs/breaks [[4 17]]} (:braces pub :counts (HashMap (& 'a str) usize)
   :total u32))
(rs/impl Shape
  (pub fn area [&self] -> f64
    (match self
      (=> (Shape/Circle r) (* std.f64.consts/PI r r))
      (=> (Shape/Rect (:braces :w :h height)) (* w height))
      (=> Shape/Empty 0.0))))
(rs/impl Display for Shape
  (fn fmt [&self f (&mut (fmt/Formatter '_))] -> fmt/Result
    (write! f "{:?} of area {:.1}" self (.area self))))
(rs/impl (:generics 'a) (Tally 'a)
  (fn add [(&mut self) name (& 'a str)]
    (+= (* (.or_insert (.entry self.counts name) 0)) 1)
    (:semi (div= self.total 1))))
(rs/macro_rules! twice (:tt "{\n    ($e:expr) => { $e * 2.0 };\n}"))
^{:rs/brace 50} (rs/fn (largest T) [xs (& [T])] -> (Option T)
  (where
    (:has T PartialOrd Copy))
  (let mut it (.iter xs))
  (let mut best (* (? (.next it))))
  (for (& x) it
    (if (> x best) (:semi (= best x))))
  (Some best))
(rs/fn main []
  (let shapes (vec! [(Shape/Circle 1.5) (Shape/Rect (:braces :w 2.0 :h 3.0)) Shape/Empty]))
  (let mut tally (Tally (:braces :counts (HashMap/new) :total 0)))
  (for s (& shapes)
    (:semi (.add tally (match s (=> (Shape/Circle _) "circle") (=> _ "other")))))
  (let (:has areas (Vec f64)) (.collect (.map (.iter shapes) (fn [s] (.area s)))))
  (let total (+ ((:turbofish .sum f64) (.copied (.iter areas))) (twice! 1.0)))
  (let mask (bit-xor (:lit "0x_ff" u8) (:lit "0b1010")))
  (let top (/ (as u8/MAX u32) 2))
  (let (:at n (..= 1 9)) 3 (else (return)))
  (let c \é)
  (let _ ((:turbofish (:global core.mem/size_of) Shape)))
  (println! "{} {:?} {} {} {} {}\n" (:index shapes 0) (largest (& areas)) mask top n c)
  (:semi ^{:rs/breaks [[1 74]] :rs/close 77} (println!
    "{} {}"
    (+ (.len (:raw "raw \"q\"" 1)) (.len (:bytes "ab")) (.len tally.counts))
    ^{:line 76} total)))
```

Points of interest:

- **The file** (§2): `ns`, `rs/file` (the crate `shapes`, a binary: no `src/`, so the namespace is
  the corpus's `rs.sample.shapes`) and the inner doc comment share line 1; the inner attribute
  `#![allow(dead_code)]` is `(rs/attr! allow dead_code)`, a meta item (§8.1).
- **Items** (§4): every top-level head is the rust-lisp form's first element qualified: `rs/use`,
  `rs/pub` (an enum and a struct), `rs/impl`, `rs/macro_rules!`, `rs/fn`. The doc comment and
  `#[derive(...)]` before the enum are top-level forms of their own, `rs/doc` and `rs/attr`.
- **Paths** (§3.3): `std::collections::HashMap` is `std.collections/HashMap`, `std::fmt::{self,
  Display}` the use group `(std/fmt self Display)`, `std::f64::consts::PI` `std.f64.consts/PI`,
  `u8::MAX` `u8/MAX`, `::core::mem::size_of::<Shape>()` the call
  `((:turbofish (:global core.mem/size_of) Shape))`, `fmt::Formatter<'_>` the type
  `(fmt/Formatter '_)`.
- **Brace groups** (§3.5): the variant `Rect { w: f64, h: f64 }`, the struct's fields (with `pub` as
  a modifier), the struct pattern with a shorthand field `:w` and the struct literals are
  `(:braces ...)`.
- **Respellings**: `0x_ffu8 ^ 0b1010` is `(bit-xor (:lit "0x_ff" u8) (:lit "0b1010"))` (§3.5, §3.6),
  `self.total /= 1` is `(div= self.total 1)`, `n @ 1..=9` is `(:at n (..= 1 9))`; `T: PartialOrd +
  Copy` is `(:has T PartialOrd Copy)`, `areas: Vec<f64>` in a `let` `(:has areas (Vec f64))`.
- **Literals** (§3.6): `1.5`, `2.0`, `0.0`, `1.0`, `0`, `1`, `2`, `9`, `3` are plain Clojure
  numbers, the format strings plain strings (`"...\n"` is canonical: `\n` is the escape the printer
  writes), `'é'` the character `\é`, `r#"raw "q""#` `(:raw "raw \"q\"" 1)`, `b"ab"` `(:bytes "ab")`.
- **Macros** (§7): `write!(f, ...)`, `println!(...)`, `vec![...]` and `twice!(1.0)` take forms (their
  tokens are comma-separated expressions the printer reproduces, `vec!`'s bracket delimiter from
  the single vector); the `macro_rules!` body is its source text, `(:tt "{\n ... \n}")`.
- **Statements** (§5.6): the last statements of `add`, of the `for` body in `main` and of `main`
  end in `;` in the source, so they are `(:semi ...)`; the `if` in `largest`'s loop is that body's
  tail, and its own block's statement `best = x;` is `(:semi (= best x))`; `Some(best)` is
  `largest`'s tail.
- **Lines** (§9.1): `^{:line 12} Empty` (a symbol beginning a line), `^{:rs/breaks [[4 17]]}` on
  the struct's brace group (the keyword `:total` begins line 17), `^{:rs/brace 50}` on `largest`
  (its body's `{` stands alone on line 50, after the `where` clause), and on the last `println!`
  `:rs/breaks [[1 74]]` (the format string begins line 74), `^{:line 76} total` and `:rs/close 77`
  (the `)` stands alone on line 77, and it is a span of the call's token stream).

## Decided (2026-10-17, the user)

RSS1-RSS10 as recommended: one head `:has` for `:` (RSS1); `div=` for `/=` (RSS2);
`rs/macro-call` for item macro calls (RSS3); literals by S10's canonical rule, `(:lit "text")`
otherwise (RSS4); namespaces by the file's path (RSS5); `include!`d expression files at RR6
(RSS6); rustdoc's JSON without spans as the interface level (RSS7); the helper written anew,
nothing vendored (RSS8); line metadata on method chains (RSS9); converted forms not tracked
(RSS10).

## Decided (2026-10-17, the user): RR-P1-RR-P3

RR-P1-RR-P3 ([RUST-NOTES.md](RUST-NOTES.md)) as recommended: plain comments in mode `:lines` come
at RR3 from the helper's lexer (RR-P1); mode `:lines`' metadata keys and defaults as built at RR1
adopted into §9.1 proper (RR-P2); §7.1 amended, named format arguments stay forms when the printed
tokens match (RR-P3).
