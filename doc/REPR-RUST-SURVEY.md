# repr/rust: Rust's bridge — survey

Status: survey, 2026-10-17, at the user's request ("we have spare token capacity: spawn a survey
about repr/rust, you may find resources in ../rust-lisp/"). Nothing is adopted here; the
decisions (§6, RS1-RS17) are the user's. Measured on main at `ca4d3eed` with the stage 0 of the
main checkout (`target/seed/amd64/arbace`, amd64), Alpine's `rust` 1.99.1 (`rust-1.99.1-r0`,
`rustc 1.99.1 (58cf8f9c3 2026-10-13)`, LLVM 23.1.3) and the rust-lisp tree at `f28e0de8c`
(branch `rust-lisp`, 2026-10-05), read only. The experiments' scratch is in
`.tmp/repr-rust-survey/` (not tracked); the one program written for them is in Appendix A.

The family ([NAMING-SURVEY.md](NAMING-SURVEY.md), "Decided", "The rename as built"): a
**bridge** is one tool between a language's source and its forms in Arbace, with the verbs
`read` and `print` and a round trip that proves the pair lossless; `repr/java`, `repr/go`,
`repr/clojure` and `repr/haskell` are the bridges so far, and translators (j2g, c2j) run between
forms dialects. `repr/rust` would be the fifth bridge: Rust source ↔ **Rust forms**.

The proposals are numbered **RS1-RS17** (RS for repr/rust). U, the prefix suggested, is taken:
REPR-HASKELL-SURVEY's uses are U1-U4, and `doc/go/EVAL-PLAN.md` and `doc/go/B1-PLAN.md` number
items U1-U5; `RS` followed by a number occurs in the repository only in Go code held as forms
(RISC-V register names in `tamago/cmd/`, ELF's `EM_RS08`), never as a decision. The uses of §1.2 are RU1-RU4.

## Executive summary

- **What it would be.** `arbace.repr.rust`, `bin/repr-rust read|print|roundtrip`: Rust source
  files as `.ae` files that Arbace's reader reads, and back, with a round trip judged by rustc
  (§1). It would serve holding and studying Rust code as data, as `tamago/` holds Go and
  repr/haskell reads GHC's boot libraries; a translator from Rust forms is a separate, much later question
  (§5.2).
- **rust-lisp is the right prior art and the wrong surface, again.** The user's fork of rustc
  (`/root/rust-lisp`, rust master `602727f26` of 2026-10-05, version 1.101.0, plus 11 commits
  and 7,441 lines in `compiler/rustc_parse/src/lisp/`) translates `.rsl` forms into the Rust
  tokens they stand for and lets rustc's parser build the tree. By its record it round-trips the
  parsed tree exactly for all 37,743 parseable `.rs` files of rustc's own tree, 2,684 UI
  programs behave the same from `.rsl`, and **the standard library builds from 1,574 `.rsl`
  module files**, with the same programs behaving identically against it (§2.1). Its per-form
  grammar (DESIGN D1-D20, SPEC I1-I6, S1-S8, §10's implicit parentheses) is a finished
  checklist. But **Arbace's reader reads 1 of rust-lisp's 5 `.rsl` files** (a deliberately
  broken test), and fails on the others at the first path: `std::collections::HashMap` is an
  invalid token. Of 128 lexemes and small forms from its SPEC and README, 41 fail; several read
  silently as something else (`(^ a b)` as `(b)`, `(@ x p)` as `((deref x) p)`, `"\01"` as
  U+0001, `r"a\n"` as two forms with a newline) (§2.3). 1,023 of the 1,074 module files of
  `core`, `alloc`, `std`, `proc_macro` and `test` contain `::`, so practically no file printed
  by rust-lisp would read. The remedy is repr/go's rule, as for go-lisp (G2C-SURVEY §7) and
  ghc-lisp (REPR-HASKELL-SURVEY H3): Rust's spelling where the reader reads it, another
  spelling where it does not.
- **A rustc helper works on the host, from Alpine's own packages (E2-E4).** `RUSTC_BOOTSTRAP=1
  rustc -Zunpretty=ast-tree` prints rustc's parsed tree on the host's stable rustc with no
  build at all (E2). A 183-line rustc_private driver (Appendix A) built in 3 s against Alpine's
  `rustc-dev-1.99.1-r0` (not installed: the apk fetched and extracted under `.tmp/`), prints the
  same tree generically through rustc's Debug impls, transcoded into Clojure forms (E3). It
  dumped **all 1,074 files of std's five crates** (14.3 MB, from Alpine's `rust-src-1.99.1-r0`,
  the sources the host's std was built from) in 5.8 s (16 jobs), 131 MB of forms, **all 1,074
  read by Arbace's reader** (26.7 million nodes, 93 s on one thread), and 12,141 of the 12,144
  `.rs` files of the 518 crates in the host's Cargo cache, editions 2015 to 2024 (E9; 2 rejected
  by rustc's parser, 1 a Debug quirk of raw lifetimes the transcoder missed).
- **The alternatives measured.** syn, the crates.io parser (cached offline, not vendored):
  2.0.118 fails on 106 of std's 1,074 files, 3.0.3 on 3 (`final fn`, `super let`: std's
  unstable syntax), and its tree is not rustc's (E4). rust-analyzer's parser (`rust-analyzer
  parse`, Alpine's 2026.10.12, plus mimalloc): a lossless syntax tree with every token and
  comment, 1,071 of 1,074 without error nodes, but a CST of its own, its CLI unstable (E5).
  rust-lisp's own printer ported to the host's rustc: 16 compile errors over its 7,441 lines
  (AST drift between 1.101-nightly and 1.99.1), a small port, for a printer whose output Arbace
  would respell anyway (E8).
- **rustc's own pretty printer passes the tree level.** `rustc -Zunpretty=normal` printed every
  file of std's five crates; re-parsed, 1,069 of 1,074 give the same tree under rust-lisp's
  normalization (spans, node and attribute ids, token spacing, collected tokens); the 5 others
  differ by the unstable `final` it drops (2) and by a dummy span (3) (E6). So a printer in
  Arbace modelled on `rustc_ast_pretty` (4,718 lines) is a proven design, and rustc's printer a
  ready candidate maker for testing the checker (as `gocmp reprint`).
- **The round trip's levels for Rust (§4).** Tokens; tree (rustc's AST under rust-lisp's D20
  normalization, plus lines); lowered (`-Zunpretty=hir`); interface (rustdoc's JSON without
  spans; `.rmeta` is deterministic but carries spans and the source's hash, so it compares only
  at the text level); code; behaviour (rust-lisp's run-corpus method); and the text level,
  `:full` byte for byte. Rust, like Haskell, has columns that reach behaviour: every panic
  location (`unwrap`, indexing, overflow checks, `#[track_caller]`) prints `file:line:col`
  (§1.3). No configuration is needed beyond the edition: before expansion every `#[cfg]` item is
  in the tree, so one conversion covers every platform (§3.5), unlike Go's build constraints and
  Haskell's CPP.
- **Recommendation (§6).** A representation first, no translator (RS1, RS15); rust-lisp's
  grammar respelled by repr/go's rule (RS2, RS3), paths as `std.collections/HashMap` in the way
  of Go forms' `pkg/Name` (RS4), `rs/` heads at the top level (RS5), macro arguments as forms
  where they reproduce the tokens and as Rust text otherwise (RS6); a generic helper
  `tools/rsdump` on the host's rustc internals with the converter and printer in Arbace (RS8,
  RS10); the host's rustc with its `rustc-dev` and `rust-src` apks pinned by hash (RS9); modes
  `:lines` and `:full` (RS11); the tree, interface and text levels first (RS12); std's five
  crates of the pinned rustc first (RS14); after repr/haskell's R2-R6 and goi's GR1-GR6,
  beginning with a spec (RS16); rust-lisp a reference (RS17). Effort, by repr/haskell's R1 and
  rust-lisp's record: about 7-10 working days to the tree, interface and text levels over std
  (§5.3).

## 1. What repr/rust would be

### 1.1 The bridge

| | repr/go | repr/haskell | repr/rust (proposed) |
|---|---|---|---|
| source | Go (go/ast + go/types) | Haskell (GHC's parsed tree, `HsModule GhcPs`) | Rust (rustc's parsed tree, `rustc_ast::Crate`, before expansion) |
| forms | Go forms (`doc/go/SPEC.md`) | Haskell forms (`doc/haskell/SPEC.md`) | Rust forms (a `doc/rust/SPEC.md` to write) |
| read | `bin/repr-go read` (helper `tools/godump`) | `bin/repr-haskell read` (`tools/hsdump`, GHC's API) | `bin/repr-rust read` (`tools/rsdump`, rustc's internals) |
| print | `bin/repr-go print`, `:lines`, `:full` | `bin/repr-haskell print` (braces layout) | `bin/repr-rust print` |
| judge | gc, `tools/gocmp` (tree, export, code; text) | GHC (tree, lines; interface, text to come) | rustc (tree, interface; text; later code, behaviour) |
| held as forms | `tamago/`, `tamago/cmd/`, `box/` | regenerated, not tracked | nothing yet |

The Rust forms would be `.ae` files read by `arbace.core/read` (Clojure 1.12 syntax, no data
readers: Go forms principle 5; CLAUDE.md, "Working conventions"). One Rust source file is one
namespace and one forms file: Rust's modules are files (`mod foo;` names `foo.rs` or
`foo/mod.rs`), and a crate is a tree of them under its root.

### 1.2 What it would serve

| use | what it needs | when |
|---|---|---|
| RU1 Holding Rust code as forms (a vendored crate, a corpus, Rust's own standard library), printed back for rustc, as `tamago/` is printed for Go's toolchain | read, print, the round trip | first |
| RU2 Studying rustc's pipeline in Arbace's own data (parsed tree; later HIR, MIR) | the helper's dumps; forms of the parsed tree | with RU1 |
| RU3 The language's breadth: Arbace as Clojure plus idiomatic Java and Go (CLAUDE.md, "Direction"), Rust as one more dialect held as data | a Rust-forms vocabulary that reads naturally beside Clojure | with RU1 |
| RU4 A translator from Rust to Go forms or Java forms, running Rust on Arbace's runtime or in the box | typed, monomorphized MIR; a runtime for Rust's memory model | much later, if ever |

RU1-RU3 are a bridge's job and need no runtime. RU4 starts from another representation (§5.2).

### 1.3 What Rust adds over Go and Haskell

- **Macros are token streams in the tree.** Before expansion, a macro call (`println!(...)`,
  `vec![...]`, a crate's own macros) and a `macro_rules!` body are token trees, not syntax:
  15,142 macro calls and 884 macro definitions in std's 1,074 files (E3's dumps). Attributes
  (`#[derive(...)]`, `#[cfg(...)]`, attribute macros) carry token streams too. The forms must
  represent tokens; rust-lisp writes macro arguments as forms when that gives the same tokens,
  and as raw tokens otherwise (D10).
- **No configuration to resolve.** `#[cfg(...)]` is an attribute; before expansion every
  configured item is in the tree, whatever the target. One conversion covers every platform,
  where Go's forms are per GOOS/GOARCH (Go forms principle 7) and Haskell's per CPP
  configuration (H9). What remains configuration is the **edition** (2015, 2018, 2021, 2024),
  a crate's property that changes the lexer and the parser (`async`, `dyn`, `try` keywords
  from 2018; reserved prefixes and `c"..."` from 2021; `gen`, `unsafe extern` blocks from 2024),
  and the files a crate selects by `#[path]`, `include!` or a `cfg_select!`.
- **The parser accepts unstable syntax.** std is written with features no stable crate may use
  (`const` traits, `final` methods, `super let`, `impl(crate)` restrictions, `try` blocks,
  `builtin #` forms); rustc parses them on every release channel, and the AST has nodes for
  them. A reader other than rustc's lags behind (E4: syn).
- **Positions reach run-time behaviour, with columns.** A panic's location is `file:line:col`:
  `Option::unwrap`, `Result::expect`, slice indexing, arithmetic overflow in debug builds,
  everything `#[track_caller]`, `file!()`, `line!()`, `column!()`, `Location::caller()`. As
  for Haskell (REPR-HASKELL-SURVEY §1.3), a printed file that keeps only lines compiles to code
  that differs in its panic messages; rust-lisp's run corpus excluded the programs that print
  their own locations (its `run-corpus.sh` filter).
- **Literal spelling is in the tree.** rustc's `ast::Lit` keeps the token's text and suffix
  (`symbol: "0x_ff", suffix: Some("u8")`, E2), so the tree level compares spellings, which Go's
  and Haskell's tree levels normalize away. Rust forms must keep every literal's spelling even in
  `:lines`.
- **Comments are not in the tree.** Doc comments (`///`, `//!`, `/** */`) are attributes; plain
  comments are only in the text. rustc's pretty printer re-inserts them from the source
  (`gather_comments`), approximately (E7: a trailing comment moved to its own line).

## 2. rust-lisp as prior art and as a basis

### 2.1 What it is

`/root/rust-lisp` (github.com/arbace/rust-lisp, private), branch `rust-lisp`, commit
`f28e0de8c2a8a8883fa8a6d4a2c9697e6ea0d280` (2026-10-05), forked from rust-lang/rust master at
`602727f26878dcf9eb2999e2f58e5df1269ff47e` (2026-10-05, `src/version` 1.101.0): 11 commits in one
day, 53 files changed, 9,719 lines added. A second front end for rustc (DESIGN.md §1-3): a
`.rsl` file (Rust as Clojure/EDN-style s-expressions) is read into forms, translated into the
Rust `TokenStream` the forms stand for, each token with the span of the atom or form that made
it, and parsed by rustc's own `Parser`, so it becomes exactly the `ast::Crate` of the equivalent
Rust (D15). Code, all in `compiler/rustc_parse/src/lisp/`: `reader.rs` (409 lines: text → forms;
literals and lifetimes delimited by `rustc_lexer`), `translate.rs`, `expr.rs`, `pat.rs`, `ty.rs`
(3,291: forms → tokens), `printer.rs`, `printer_expr.rs`, `layout.rs` (3,141: AST → forms),
`shape.rs` (231: the implicit parentheses, after the pretty printer's `FixupContext`),
`check.rs` (228: the round trip), `mod.rs`; one-line hooks marked `rust-lisp:` in five upstream
files (`rustc_parse/src/lib.rs`, `rustc_interface/src/passes.rs`, `rustc_expand/src/module.rs`,
`rustc_session/src/config.rs`, `rustc_driver_impl/src/pretty.rs`). Its CLAUDE.md states that the
work is a private, one-time experiment under the personal-use exemption of rust-lang/rust's LLM
policy (`AGENTS.md`), never to be contributed upstream as is.

What it established (README.md, "How well it works"; DESIGN.md, "Status (2026-10-05)"):

- `rustc -Zunpretty=lisp-check FILE.rs`: Rust → forms → Rust gives the same tree for all 37,743
  `.rs` files of `compiler/`, `library/`, `src/` and `tests/` that rustc parses without errors;
  984 files with recovered parse errors are skipped, 489 do not parse (`check-corpus.sh`, about
  2 minutes).
- `rustlisp/run-corpus.sh`: the UI suite's single-file run-pass tests converted to `.rsl`, 2,684
  with the same output and exit status, 9 more whose Rust originals vary between two runs;
  tests with aux builds, revisions, flags, or that print their own source locations are left out.
- `rustlisp/std-from-lisp.sh`: 1,574 module files of `core`, `alloc`, `std` and their
  dependencies converted to `.rsl` (crate roots and files named by `#[path]` or `include!` stay
  Rust), built with `-Zbuild-std` into a sysroot; against it, the run-pass programs (2,687, the
  number DESIGN.md gives) behave identically. This is the strongest result of the three sister
  projects: a whole standard library compiled from forms.
- Comments: `-Zunpretty=lisp` keeps plain comments at item, field, statement and arm boundaries;
  `///` and `//!` become `;;;` and `;;!`. Comments are not compared (SPEC §12).
- Printing Rust: `-Zunpretty=normal` on a `.rsl` file, rustc's own pretty printer: no text level.
- Tooling: mixed `.rs`/`.rsl` crates (`mod foo;` finds `foo.rsl`), a Cargo patch, Vim support;
  diagnostics and debuginfo point into the `.rsl` file.

**No build is on the host** (no `build/` in the tree), so `-Zunpretty=lisp` could not be run;
building it means a copy of the tree, `download-ci-llvm` (network) and about 12 minutes
(its CLAUDE.md). The only `.rsl` files are its five tests (`tests/ui/rust-lisp/`,
`tests/run-make/rust-lisp/`, 86 lines).

### 2.2 Its grammar against Arbace's conventions

| | rust-lisp | Go forms (repr/go) | Haskell forms (repr/haskell) |
|---|---|---|---|
| reader | its own: EDN structure, Rust lexemes by `rustc_lexer` (D2) | Arbace's, unchanged (principle 5) | Arbace's, unchanged |
| names, paths | verbatim, a path one symbol with `::` (D3: `std::io::Write`) | `pkg/Name` | verbatim, `M.insert`; `(:name "...")` |
| heads | Rust keywords and operators (`fn`, `let`, `match`, `=>`, `+=`), EDN keywords for the rest (`:tuple`, `:index`, `:semi`: D1, D9) | `go/...` at top level; Go's and Clojure's words inside | `hs/...` at top level; ghc-lisp's inside |
| order | Rust's source order (I1) | Go's | Haskell's |
| brackets | lists `()`, vectors `[]` (arrays, parameters), brace groups `{}` that are **not maps** (D13: odd lengths, shorthand `{:x :y 2}`, visibility `{pub :x T}`) | vectors, maps only where Clojure's meaning fits | vectors; records as lists |
| blocks | no braces: a body is the form's elements, the last its tail, `;` added (D8) | `do`-like bodies | layout by the printer |
| parentheses | implicit by the pretty printer's rules, `(:paren e)` for redundant ones (I5) | dropped but `&&`/`||` operands | implicit, `(:paren e)` |
| attributes | reader-level prefixes `#[derive Debug Clone]`, `#![...]` (D11) | directives as forms | pragmas as forms |
| macros | `(name! args...)`, raw tokens `#tt(...)` where args are not expressions, `macro_rules!` bodies raw (D10) | n/a | Template Haskell as forms |
| literals | verbatim Rust lexemes (`1u8`, `r#"..."#`, `b'a'`, `'a'`) | value; spelling in `:full` | value; spelling in `:full` |
| positions | its own spans into `.rsl` (D17) | `:line` metadata (`:lines`), `:go/pos` (`:full`) | `:lines`, `:full` |
| comments | `;` dropped; `;;;`, `;;!` docs; plain comments kept approximately by the printer | data in `:full` | data in `:full` |

Its shapes fit Arbace's conventions: source order, verbatim names, vectors for brackets, keyword
heads for forms Rust has no keyword for are what Go and Haskell forms do. What does not fit is
the lexical level: paths, the `:` of bounds and typed patterns, attributes, raw token trees,
brace groups, and Rust's literals.

### 2.3 Does Arbace's reader read rust-lisp's forms? (experiment E1)

Read (1) the five `.rsl` files of rust-lisp's tests, every form in turn (`readall.ae`); (2) 128
lexemes and small forms from SPEC.md §1-§10 and README's cheat sheet, each as the text of a
vector, with the script of repr/haskell's E1 (`test/haskell/reader_samples.ae`, copied), then
36 doubtful cases and 27 candidate respellings (`samples.txt`, `samples2.txt`, `samples3.txt`).

**Files: 1 of 5 reads** (`syntax_error_mod.rsl`, a test that `(:index v)` without an index is
an error). `counter.rsl` and `shapes.rsl` stop at their first form (`(use
std::collections::HashMap)`: `Invalid token`), `main.rsl` at `counter::Counter::new`,
`type_error_mod.rsl` at `(: n u32)`.

**Lexemes and forms** (`.tmp/repr-rust-survey/e1/samples*.out`):

| class | rust-lisp | Arbace's reader |
|---|---|---|
| fails | every path with `::`: `std::io::Write`, `::core::mem`, `Self::Item`, `crate::a::b`, `super::scale`, `e::*`, `u8::MAX`, `Default::default` | `Invalid token` (the reader refuses `::` inside a symbol) |
| fails | `(: T Ord)`, `(: x u8)` (bounds, typed patterns), `(:: size_of T)` (turbofish) | `Invalid token: :`, `::` |
| fails | `#[derive Debug Clone]`, `#[cfg (all unix ...)]`, `#[doc = "x"]`, `#[:doc-block "x"]` | `Reader tag must be a symbol` |
| fails | `#![allow unused]` | `#!` is a line comment: the rest of the line vanishes, then EOF |
| fails | `#tt(a, b)`, `#tt{ ($x:expr) => ... }` | `No reader function for tag tt`, or the braces read as a map first |
| fails | brace groups with shorthand, visibility or odd length: `{:x :y 2}`, `{pub :x f64 :y f64}`, `(P {:x 1.0 :y})`; a repeated key | `Map literal must contain an even number of forms`, `Duplicate key` |
| fails | `1u8`, `42i32`, `0usize`, `1i128`, `0x_ff`, `0xFFu32`, `0o17`, `0b1010`, `1_000_000`, `1e3f64`, `2.5f32` | `Invalid number` |
| fails | `'\n'`, `'\''`, `'\u{1F600}'`, `b'\x7f'` | `Unsupported character`, `Invalid number` |
| fails | `"\x41"`, `"\u{1F600}"`, a string with a line continuation (`\` newline) | `Unsupported escape character`, `Invalid unicode escape` |
| fails | `r#"raw "quoted""#`, `r#"x"#`; `^`, `a^b` alone | unmatched delimiters (`#"` is a regex literal; `^` metadata) |
| reads as something else | `(^ a b)` (xor), `(^= a b)` | `(b)` and `(a b)`: the operand becomes metadata, silently |
| reads as something else | `(@ x (..= 1 5))` (binding pattern), `x@y` | `((arbace.core/deref x) (..= 1 5))`: three elements become two; `x` and `(deref y)` |
| reads as something else | `'a'` (char), `'static'` | `(quote a')`, `(quote static')` |
| reads as something else | `"\01"`, `"\012x"`, `"\1"` (Rust: `\0` then a digit; `\1` invalid) | U+0001, `"\nx"`, U+0001: Clojure's octal escapes |
| reads as something else | `r"raw\n"`, `b"bytes"`, `br"x"`, `c"x"`, `cr"x"` | two forms, a symbol `r`/`b`/`br`/`c`/`cr` and a string, with the raw string's `\n` decoded to a newline |
| reads as something else | `{:x 1 :y 2}` (a struct literal's fields) | a map: the right elements, but as an unordered collection with unique keys |
| reads, spelling lost | `1e3`, `0.5E-3`, `1.` | 1000.0, 5.0E-4, 1.0 (the tree keeps the spelling, §1.3) |
| reads as a symbol | `b'a'`, `&'a`, `Vec<u8>`, `$crate`, `$x` | one symbol each (usable, but not a byte literal) |
| reads | names (`foo`, `r#type`, `r#match`, `_`, `Self`, `é`, `naïve`), field chains (`self.x.0`), lifetimes and labels as `(quote a)` (`'a`, `'static`, `'outer`, `'_`), operators (`+ - * ! & &mut &raw | || && << >> == != <= % %= <<= += -= ? .. ..= ... -> =>`), `(*const T)`, method heads (`.push`, `.await`), macro calls (`(println! "{} {}" a (+ b 1))`, `(vec! [1 2 3])`), keywords (`:tuple`, `:_`, `:semi`, `:qself`, `:path`), vectors, `#_`, plain strings, `true`, `false`, decimal integers and floats | as intended |

With `read-string`, 41 of the 128 fail; of the 36 doubtful cases 14 fail. The silent misreads
are the worse kind: `(^ a b)` loses an operand without an error, `(@ x p)` changes a form's
arity, and `"\01"` changes a string's characters. As for Haskell forms, a writer of Rust forms
must check every bare name and literal by reading it back.

How often the failing lexemes occur (grep over the 1,074 `.rs` files of std's five crates,
402,154 lines): `::` in 1,023 files (51,698 lines), lifetimes in 697 files, attributes `#[` in
905 files (35,510 lines), `#![` in 2,435 lines, macro calls `name!` in 19,734 lines,
`macro_rules!` in 130 files, `&` in parameter types in 598 files, suffixed numbers in 113 files
(2,923 lines), hexadecimal literals in 2,222 lines, `^` in 85 files, `@` in 351 lines.

The candidates of §3.1 all read (`samples3.out`): `std.io/Write` (namespace `std.io`, name
`Write`), `Self/Item`, `std.io/*`, `std:io:Write` (one plain symbol), `(:path std io Write)`,
`(:has T Ord)`, `(:turbofish size_of T)`, `\a` and `\newline` (characters), `(:attr derive
Debug Clone)`, `(:tt "($x:expr) => { $x + 1 };")`, `(:lit "0x_ff" u8)`, `bit-xor`, `(:braces :x
1 :y)`. `std/io/Write` reads too, but as namespace `std` and name `io/Write`, and `/core/mem` does
not read.

### 2.4 Its round trip against repr/go's levels

| repr/go level (Go forms §3.1) | rust-lisp | Rust counterpart |
|---|---|---|
| 1 Trees | `-Zunpretty=lisp-check`: the trees' `Debug` text equal without spans, attribute ids, token spacing and items' collected token streams (D20, SPEC §11); 37,743 files | the same, plus lines |
| 2 Export data | none | rustdoc's JSON of the crate (public API), or `.rmeta` (§4.1) |
| 3 Object code | none | object code, symbol by symbol; MIR (`-Zunpretty=mir`) |
| 4 Behaviour | `run-corpus.sh`: 2,684 UI programs; and against the std built from `.rsl` | the same method |
| text (`:full`) | none (`-Zunpretty=normal` is rustc's pretty printer) | the source byte for byte |

The tree level is the right first gate, as for ghc-lisp: a front end that gives the same AST
gives the same program, since expansion, resolution and everything after see the same tree.
The normalization D20 lists is exactly what E6 had to apply to compare rustc's pretty-printed
output with its source; token spacing (`Joint`, `Alone`, `JointHidden`) matters only to
procedural macros that inspect it, which is why it may be dropped. rust-lisp's strongest
check, std built from forms and the same programs behaving the same, is the behaviour level
applied to the library itself.

### 2.5 Coverage

All of Rust's syntax, stable and unstable, as of rustc master of 2026-10-05 (D14): async, const
generics, raw pointers, inline `const`, `&pin`, `gen` blocks, `builtin #` forms, guard patterns,
unsafe binders, contracts and delegation (the last two written as `(:rust #tt{...})`, D19).
Macros: calls with expression arguments as forms, other token trees raw; `macro_rules!` bodies
always raw (D10). Not covered: comments in expressions (moved to the next boundary), and files
rustc cannot parse alone.

### 2.6 As a basis (experiment E8)

- **Version.** rust-lisp follows rust master of 2026-10-05 (1.101.0-nightly); the host has
  1.99.1 stable (released 2026-10-13, two release trains behind master). Its front end in the
  fork is unusable on the host without a build of the fork (a copy of the tree, LLVM fetched,
  12 minutes, several GB) or a port to the host's rustc.
- **The port.** A copy of `compiler/rustc_parse/src/lisp/` compiled as a rustc_private library
  against 1.99.1's `rustc-dev` (E3's sysroot), `crate::` replaced by `rustc_parse::`: **16
  errors** in 7,441 lines. The AST's drift: `token::IdentKind` (absent in 1.99.1),
  `ItemKind::TestBinderConstraints`, `ExprKind::GcaMacro`, `FnHeader::coroutine_marker`, a use
  tree's items without `inner`, `GenBlockKind::as_str`, three variants whose arity changed, and
  one private function (`lexer::lex_token_trees`). Far smaller than ghc-lisp's port (E3 there
  stopped at a changed constructor shape after four missing ones), but repeated at each rustc
  release, and the output, `.rsl`, is what Arbace's reader cannot read.
- **License.** rustc is MIT OR Apache-2.0; nothing is vendored by this survey. Vendoring from
  rust-lisp (a fork of rust-lang/rust, its additions LLM-generated) would add the origin to
  `doc/ARBACE.md` and the journal and Rust's licenses to `LICENSE.md` (CLAUDE.md, "Records").

## 3. Design options

### 3.1 The grammar

**(a) rust-lisp's shapes, respelled for Arbace's reader.** Keep its SPEC form by form (D1-D20,
I1-I6, S1-S8, §10's implicit parentheses), and change only what the reader cannot read, by
repr/go's rule (Go forms principle 5, §7.5). A respelling table (sketch; the spec would settle
it):

| rust-lisp | Rust forms (sketch) | why |
|---|---|---|
| `std::io::Write`, `Self::Item`, `a::b::C::d`, `e::*` | `std.io/Write`, `Self/Item`, `a.b.C/d`, `e/*`: the segments before the last joined by `.` as a namespace, the last as the name (Go forms' `pkg/Name`, §4.4 there) | `::` is no token inside a symbol |
| `::core::mem` (global path), `crate::m`, `super::x` | `(:global core/mem)`; `crate/m`, `super/x` | `::` and a leading `/` do not read |
| `self.x.0` (field chain on a path) | as written: a symbol with dots and without `/` | readable; Rust names have no dots, so `a.b/c` (path) and `a.b` (fields) never collide |
| `(: T Ord)`, `(: x u8)`, `(: 'a 'b)` | `(:has T Ord)`, `(:has x u8)` (or `:bound` and `:typed`, two heads) | `:` is no token |
| `(:: size_of T)`, `((:: .collect (Vec _)) it)` | `(:turbofish size_of T)`, `((:turbofish .collect (Vec _)) it)` | `::` is no token |
| `#[derive Debug Clone]`, `#[cfg (all unix (= feature "x"))]`, `#[path #tt(...)]` | `(:attr derive Debug Clone)`, `(:attr cfg (all unix (= feature "x")))` before the form it attaches to (RS7) | `#[` is a reader tag |
| `#![allow unused]` | `(:attr! allow unused)` at the start of a body or file | `#!` is a comment |
| `#[:doc-block "x"]`, `;;; doc`, `;;! doc` | `(:doc "x")`, `(:doc! "x")`, `(:doc-block "x")`: doc comments as data, as Haskell forms' `(:doc ...)` | doc comments are attributes in the tree |
| `#tt(...)`, `#tt[...]`, `#tt{...}` (raw tokens) | `(:tt "(...)")`: the Rust text of the token tree, delimiters included (RS6) | `#tt` is a reader tag; Rust's tokens are not Arbace's |
| `{:x 1 :y}`, `{pub :x f64}`, `{}` (brace groups) | `(:braces :x 1 :y)`, `(:braces pub :x f64)`, `(:braces)`; or a map where every field has a value and no visibility (two spellings: rejected by I6's one-canonical-form) | maps need pairs and lose order |
| `'a'`, `'\n'`, `'\u{1F600}'` | `\a`, `\newline`, `\u{1F600}` as `(:char 128512)` beyond the BMP (Clojure characters, as Go forms' runes) | `'a'` misreads as `(quote a')` |
| `'a`, `'static`, `'outer`, `'_` (lifetimes, labels) | as written, read as `(quote a)` | unambiguous once characters are `\x` (Haskell forms' promoted names) |
| `b'a'`, `b"x"`, `br"x"`, `c"x"`, `cr"x"`, `r#"x"#` | `(:byte \a)`, `(:bytes "x")`, `(:bytes "x" :raw 0)`, `(:cstr "x")`, `(:cstr "x" :raw 0)`, `(:raw "x" 1)` (the hash count) | two forms, decoded escapes |
| strings with Rust escapes (`\x41`, `\u{..}`, `\0`, line continuations) | a Clojure string of the same characters; the spelling as `(:lit "...")` (the tree keeps it, §1.3) | invalid or octal escapes |
| numbers with suffixes or Rust's notation (`1u8`, `0x_ff`, `0o17`, `0b1010`, `1_000`, `1e3`, `2.5f32`) | bare when Arbace reads the text back and prints the same text (`1`, `-5`, `1.0`), else `(:lit "0x_ff" u8)` (the token's symbol and suffix, as `ast::Lit`) | invalid numbers; spelling in the tree |
| `(^ a b)`, `(^= a b)` | `(bit-xor a b)`, `(bit-xor= a b)` (Go forms §7.5's word) | `^` is metadata: a silent misread |
| `(@ x p)` | `(:at x p)` | `@` is deref: the arity changes |
| `(m! a b)`, `(vec! [1 2 3])` | as written | readable |

Pros: the user's own design, proven lossless over 37,743 files and the whole standard library; a
mechanical bijection with `.rsl` (a respelling pass both ways), so a built rust-lisp could compile
Rust forms. Cons: the keyword heads (`:has`, `:turbofish`, `:braces`) are less Rust-like than
rust-lisp's; the bijection holds only while both grammars move together.

**(b) Arbace's own grammar, derived from rustc's AST, in repr/go's style.** Heads chosen anew
(`rs/fn`, `(struct P (field x f64))`...), closer to Clojure, perhaps with rustc's later decisions
(resolved paths, inferred types from HIR) written out as repr/go writes go/types' decisions.
Pros: freedom, room for semantic annotations. Cons: a second grammar for the same tree beside
rust-lisp's, its spec from nothing; Rust's type inference is global to a function and its trait
resolution to the crate graph, so "decisions written out" would need rustc's later phases (HIR,
THIR) in the helper, which the bridge does not need (Haskell forms' principle 1 made the same
choice).

**(c) rust-lisp verbatim.** Excluded by E1, and changing the reader breaks principle 5.

### 3.2 Paths

Rust's paths are the most frequent failing lexeme (§2.3) and need a decision of their own:

| | `std.io/Write` (a) | `std:io:Write` (b) | `(:path std io Write)` (c) |
|---|---|---|---|
| reads as | a qualified symbol, ns `std.io`, name `Write` | one plain symbol (single colons are allowed inside symbols) | a list |
| Clojure's sense | a name in a namespace, as `arbace.core/map`, Go forms' `fmt/Println` | none | none |
| Rust's spelling | `::` → `.` and the last `/` | `::` → `:`, closest | segments spelled out |
| global `::a::b` | `(:global a/b)` | `:a:b` reads as a keyword: needs another spelling | `(:path :global a b)` |
| generic segments `Vec::<u8>::new` | `(:path (Vec u8) new)` (rust-lisp's `:path`, kept) | the same | natural |
| cost | a rule for the split (all segments but the last are the namespace) | a reader corner (colons inside symbols are unspecified in Clojure's reader docs) | verbose: 51,698 lines in std have one |

(a) fits Arbace best: Clojure's own notion of a qualified name, and repr/go's precedent.

### 3.3 Reading: who produces the forms

**(a) A rustc_private helper, the converter in Arbace (repr/go's and repr/haskell's
architecture).** `tools/rsdump` (E3, Appendix A): a driver on `rustc_driver` that stops after
parsing the crate root (`Callbacks::after_crate_root_parsing`), so every file is parsed alone
with its out-of-line modules unloaded, as rust-lisp's check runs per file. It prints the AST with
rustc's own `Debug` impls (the text `-Zunpretty=ast-tree` prints, but compact) and transcodes it
generically: `Name { a: x }` → `(Name :a x)`, `Name(x)` → `(Name x)`, `[..]` → vectors, strings
re-escaped, idents `x#0` → `(id "x" 0)`, spans `FILE:l1:c1: l2:c2 (#n)` → `[l1 c1 l2 c2]`, node
ids and items' collected token streams dropped. The helper decides nothing about forms; the
converter, keyed by constructor names, decides everything (rust-lisp's `printer.rs` as its
checklist), and a release's AST changes show up as unknown constructors. Measured:

| | |
|---|---|
| helper | 183 lines of Rust, `RUSTC_BOOTSTRAP=1 rustc -O --sysroot SYSROOT rsdump.rs` (SYSROOT: the host's std libraries plus Alpine's `rustc-dev-1.99.1-r0`, 252 rmeta files and `librustc_driver-61690d3e4c729118.so`, the very library the host's `rustc` links), built in 2.9 s, 282 KB, dynamically linked to the host's `librustc_driver` |
| input | std's five crates (`core`, `alloc`, `std`, `proc_macro`, `test`) from Alpine's `rust-src-1.99.1-r0`: 1,074 `.rs` files, 14.3 MB, edition 2024 |
| dumped | 1,074 in 5.8 s wall (16 jobs), 131 MB (9.2 times the source) |
| read by Arbace | all 1,074, 0 unreadable, 26.7 million nodes, 93 s (one thread) |
| crates (E9) | the host's Cargo cache, 518 crates, 12,144 `.rs` files (151 in crates of edition 2015, 2,076 of 2018, 8,052 of 2021, 1,865 of 2024): 12,141 dumped; 2 rejected by rustc's parser (a file that is a `compile_error!` string, a negative inherent impl in syn's tests), 1 transcoder failure (a raw lifetime `'r#mod`) |
| a sample | `fn f() -> u32 { 0x_ffu8 as u32 + 1 }` (37 bytes) → 1.3 KB of forms; `Lit :kind Integer :symbol "0x_ff" :suffix (Some "u8")` |

The Debug text is regular except for six spellings the transcoder must know, all found by the
corpus: idents `name#ctxt` (any name: `{{root}}`, `0` for tuple fields, `r#type`), lifetimes and
labels `'a#0`, raw lifetimes `'r#mod#0`, `lifetime(ID: 'a)`, spans, and `no-location`. They are
part of what the pinned rustc prints, not a contract; the helper would check the rustc version
and the converter would refuse unknown shapes.

**(a') No helper: `-Zunpretty=ast-tree` and a transcoder in Arbace.** `RUSTC_BOOTSTRAP=1 rustc
--edition 2024 -Zunpretty=ast-tree FILE.rs` works on the host's stable rustc with nothing
installed (E2). The output is the pretty-printed Debug (`{:#?}`): 1.27 GB for std's 14.3 MB (89
times; 10 s with 16 jobs), with node ids and token streams, to be transcoded in Arbace. Pros: no
build, no `rustc-dev`, the rustc binary itself the only dependency. Cons: the size; the same
Debug quirks; `-Z` flags need `RUSTC_BOOTSTRAP` on every run, and nothing can be added (comments,
the lexer's tokens for `:full`, the tree level's normal form) without a driver.

**(b) syn, in a helper of its own.** syn (David Tolnay; MIT OR Apache-2.0), the parser
procedural macros use; with `features = ["full", "extra-traits"]` its tree has `Debug`. Built
offline from the host's Cargo cache (`~/.cargo/registry`, not vendored in Arbace) in 20 s (E4).
Over std's 1,074 files: syn 2.0.118 fails on 106 (`const unsafe impl`, `impl(self) trait`,
`const trait`: std's unstable syntax), syn 3.0.3 on 3 (`final fn` in `core/src/clone.rs` and
`core/src/io/size_hint.rs`, `super let` in `std/src/macros/tests.rs`). Pros: a stable library
API, no rustc internals, `RUSTC_BOOTSTRAP` not needed. Cons: its tree is syn's, not rustc's, so
the judge (rustc's tree) and the reader differ, and the converter would map syn's tree while the
checker compares rustc's; syn lags rustc's syntax (and keeps unknown items as `Verbatim`
tokens); a crates.io dependency to vendor (with `proc-macro2`, `quote`, `unicode-ident`).

**(c) rust-analyzer's parser.** `rust-analyzer parse` (Alpine's `rust-analyzer-2026.10.12-r0`,
which needs `mimalloc2`; both fetched under `.tmp/`) prints a lossless syntax tree: every token,
whitespace and comment with byte ranges (E5). Over std: 1,071 of 1,074 without `ERROR` nodes
(the same `final fn` files and `core/src/ops/try_trait.rs`), 23 s one file at a time. Pros: the
text level for free (a CST is the text). Cons: a third tree (rowan's), neither rustc's nor
rust-lisp's; the CLI says its subcommands have no stability guarantees; as a library
(`ra_ap_syntax`) it is not in the Cargo cache.

**(d) rust-lisp's printer ported to the host's rustc, then a respelling pass.** 16 errors to fix
(E8), then `.rsl` text to respell, which needs a reader of rust-lisp's lexemes in Arbace (the
thing Arbace's reader cannot do), so a tokenizer of its own. Ties Arbace to the fork's
printer at each release.

**(e) A Rust parser in Arbace.** A lexer for every edition and a parser for rustc's grammar,
stable and unstable (`rustc_parse` is 32,529 lines in 1.99.1). Pros: reading without rustc, inside
the box. Cons: a second Rust parser that must equal rustc's on every file, judged by rustc
anyway. Worth it only if Rust must be read where rustc cannot run.

### 3.4 The dependency on rustc

| | rustc needed for | its version |
|---|---|---|
| (a) rustc_private helper | reading (the helper links rustc's driver) and the checks | exactly the host's: `rustc-dev` must be the same build as `rustc` (the `.so` hash) |
| (a') `-Zunpretty=ast-tree` | reading and the checks | the host's; Debug text pinned by version |
| (b) syn, (c) rust-analyzer | the checks only; reading by syn's or rust-analyzer's own parser | syn's or rust-analyzer's release |
| (e) parser in Arbace | the checks only | none to read |

As for GHC and javac, rustc would be needed at conversion and check time only, never to build or
run Arbace. The host's rustc is Alpine edge's `rust` (installed: [ALPINE.md](ALPINE.md) lists it
among world's names); `rustc-dev` (343 MiB installed, 127 MB apk) and `rust-src` (102 MiB, 17.5
MB apk) are in Alpine's repository but not installed. E3 used them without a host change:
`apk fetch` into `.tmp/`, extracted, a sysroot of links. The apks are pinned by SHA-256
(`rustc-dev-1.99.1-r0.apk` `8837fc72...a356b21`, `rust-src-1.99.1-r0.apk` `0849c88c...4936cfb`).
When Alpine edge moves `rust` to the next release, its mirror drops 1.99.1's apks: the forms
record the rustc version, the helper refuses another, and either the pins move with Alpine
(the corpus re-pinned with them, as `tools/hsdump/GHC_VERSION`) or a toolchain is pinned
independently of Alpine (the official `rust-1.99.1-x86_64-unknown-linux-musl` tarball and its
`rustc-dev` component, by hash, as `seed/goroot.sha256` pins Go; whether rust-lang publishes
`rustc-dev` for the musl host is to check).

`RUSTC_BOOTSTRAP=1` turns a stable rustc into one that accepts `-Z` flags and `#![feature]`; it
is meant for building rustc itself, and the Rust project discourages its use elsewhere. Here it
would be confined to the helper's build and to the checker's `-Zunpretty` runs; the forms and the
printed Rust need nothing unstable (std's own files use unstable features, which rustc accepts
when it builds std, and the round trip compiles them the same way).

### 3.5 Configuration

What one conversion fixes: the rustc version, the edition (from the crate's `Cargo.toml`, as
rust-lisp's scripts take it, or a `//@ edition:` directive for the UI tests), and the file set.
What it does not need, unlike Go and Haskell: a target, cfg flags or features, since the tree
before expansion keeps every `#[cfg]` item and every `cfg_attr`. Files reached only through
`#[path = "..."]` (std's `sys/` tree uses it), `include!` or a macro are converted as files of
their own, with the edition of the crate that includes them (rust-lisp's `std-from-lisp.sh`
keeps them as Rust; the bridge has no reason to).

### 3.6 Side by side

| | grammar (a) + read (a) | grammar (a) + read (a') | grammar (a) + read (b) | grammar (a) + read (d) | read (e) |
|---|---|---|---|---|---|
| spec work | respelling table over rust-lisp's SPEC | the same | the same, mapped from syn's tree | the same | as chosen |
| rustc | the host's, internals (`rustc-dev`) | the host's, `-Z` only | for the checks | a port per release | for the checks |
| fidelity | rustc's parser | rustc's parser | syn's (3 of 1,074 std files fail) | rustc's parser | a reimplementation |
| `:full` | spans plus `rustc_lexer` over the text | spans, the text read by the converter | proc-macro2 spans | as (a) | own |
| first result | days (§5.3) | days, slower runs | days | fastest if the port is done | weeks |
| repr/go's architecture | yes | yes, helper-less | yes | no | no |

## 4. The round trip

### 4.1 Levels

For a file converted under a configuration (rustc version, edition), parse the original and the
printed file with the same rustc and flags (and, for the levels after parsing, build both
crates). Equivalent when:

1. **Tokens.** The two token streams (`rustc_lexer`, or rustc's `TokenStream` with spacing
   ignored) equal. Weaker than the tree level; useful only to localize a printer's error.
2. **Tree.** The two `ast::Crate` equal under rust-lisp's normalization (D20: no spans, node
   ids, attribute ids, token spacing, items' collected token streams), plus the start line of
   every node with a span in `:lines` (repr/haskell R1's lines check). The helper produces the
   normal form (`rsdump tree`, `hsdump tree`'s counterpart); E6 applied it with `sed` to rustc's
   own pretty printer: 1,069 of 1,074 std files equal (§4.3).
3. **Lowered** (optional). `-Zunpretty=hir` of the two crates: after expansion, resolution and
   lowering, printed without spans. Equal for E7's sample with a comment moved; catches nothing
   the tree level misses for a pure printer, useful for a writer other than the converter.
4. **Interface ("export").** rustdoc's JSON of each crate (`rustdoc -Zunstable-options
   --output-format json`, format version 61 on 1.99.1) without its `span` fields: the public
   items, their signatures, generics, trait impls, attributes, docs. Equal for E7's sample.
   `.rmeta`, rustc's metadata, is deterministic (E7: two builds in different directories equal
   under `--remap-path-prefix`), but it embeds spans and the source files' hashes, so it differs
   as soon as one character of the text does: a text-level check, not an interface one.
5. **Code.** Object code symbol by symbol (as `gocmp`'s code level), or MIR
   (`-Zunpretty=mir`). Columns reach code through panic locations (§1.3), so this level needs
   `:full` or a normalization of `Location` constants.
6. **Behaviour.** rust-lisp's `run-corpus.sh` method over the UI suite's run-pass programs (not in
   `rust-src`: from rustc's source tarball, pinned), and its `std-from-lisp.sh` method: std
   built from the printed files, the same programs run against it.

Beside them, the **text level**: the printed file byte-identical to its original in mode `:full`,
as repr/go's since 2026-10-16 (PRINTER-NOTES.md, "Mode :full as built") and repr/haskell's to
come. rustc's tree holds spans for nodes and for the tokens of token streams, but not for every
keyword, comma and brace, nor for plain comments; as repr/go's converter reads the Go files
themselves in `:full` (`:text-dir`, G2F3), Rust's would read the file and lex it with
`rustc_lexer` (in the helper: every token and comment with its byte range) and merge by position.
rust-analyzer's lossless tree (E5) shows the facts exist in one pass of a lexer.

### 4.2 Positions

`:lines` (the forms text at Rust's lines, as Go and Haskell forms) is enough for levels 1-4.
Levels 5-6 and the text level need columns: either `:full`, or an intermediate mode keeping each
node's line and column. Since a panic location is in nearly every function of std (indexing,
`unwrap`, overflow checks), `:full` is the mode that makes a converted crate behave as its
original, and it should come early for Rust (RS11).

### 4.3 rustc's printer as a reference (experiment E6)

`RUSTC_BOOTSTRAP=1 rustc --edition 2024 -Zunpretty=normal FILE.rs` printed each of std's 1,074
files; each original and its printed file were dumped by `rsdump` and compared after removing
spans, contexts and token spacing (D20): **1,069 equal**, 5 differ: `core/src/clone.rs` and
`core/src/io/size_hint.rs` (the printer drops `final` from `final fn`, an unstable feature newer
than its printer), `core/src/attribute_docs.rs`, `core/src/keyword_docs.rs`,
`core/src/num/traits.rs` (a span that is a real location on one side and `no-location` on the
other, an artifact of the comparison's normalization). 62 s with 16 jobs. Two conclusions: a
printer in Arbace modelled on `rustc_ast_pretty` (`state.rs`, `state/expr.rs`, `state/item.rs`,
`state/fixup.rs`: 4,718 lines) can reach the tree level, as rust-lisp's `shape.rs` follows the
same `FixupContext` for parentheses; and rustc's printer is a ready candidate maker to prove the
checker (as `gocmp reprint` proved repr/go's oracle).

### 4.4 Corpora

| corpus | size | notes |
|---|---|---|
| std's five crates (`core`, `alloc`, `std`, `proc_macro`, `test`) | 1,074 `.rs`, 14.3 MB, edition 2024 | E3's; Alpine's `rust-src-1.99.1-r0`, exactly the sources of the host's std; rust-lisp converted these (1,574 module files with the dependencies) |
| the rest of `library/` | 1,902 `.rs` more (`stdarch` 209 files and 21.2 MB, `vendor` 926, `compiler-builtins` 280, `portable-simd`, `backtrace`, the `*tests` crates) | in the same apk; `stdarch` is macro-heavy |
| the compiler | 2,167 `.rs` (`compiler/`, the `rustc-src` part of the same apk) | rustc's own sources, as rust-lisp's corpus included |
| crates | the host's Cargo cache: 518 crates, 12,144 `.rs`, editions 2015-2024 | E9; informal (what other work on the host fetched); a pinned crate set later |
| rustc's test suite | `tests/ui` and the rest (rust-lisp: 37,743 parseable files of the tree; 2,684 run programs) | rustc's source tarball for 1.99.1 (`rustc-1.99.1-src.tar.xz`), pinned by hash, as repr/haskell's `ghc-9.14.1-src.tar.xz` |

## 5. Where it fits

### 5.1 In Arbace's plans

Today's order (AGENDA.md, "Next"): goi (Go forms interpreted at the REPL: GR1-GR6 after the
coming re-seed, 24-34 days), repr/haskell's R2-R6 (the text level, interfaces, the rest of its
corpus), `repr/java print` (running), Futamura's F3-F7. repr/rust is a fifth bridge with no
consumer yet; it would come after repr/haskell's R-milestones, reusing their shape (a generic
helper on a compiler's API, a converter keyed by constructor names, a printer with implicit
parentheses, the tree and lines levels, `:full`), and is independent of goi. It touches neither
the build nor the seed: a namespace (`arbace.repr.rust`, evaluated from the checkout and not
embedded, as repr/haskell by HR1), a helper (`tools/rsdump`), a script, and its forms
regenerated, not tracked, until a crate is held as source by a recorded decision.

### 5.2 Translators and later representations

- **Rust → Go forms (or Java forms).** Rust's meaning is decided after the parser: name
  resolution, macro expansion, trait resolution, type inference, monomorphization, drop
  elaboration. A translator would start from MIR (rustc's mid-level IR, after borrow checking,
  explicit drops, monomorphic per instance), not from the AST, and need a runtime for raw
  memory, unwinding and Rust's `std` on Arbace's platform. Prior art: rustc's own back ends
  (cranelift, GCC), Miri (a MIR interpreter), mrustc (a Rust compiler to C). The host ships
  `librustc_public` (stable MIR) in its `rustlib`, which a later `repr/mir` could read. Not
  needed for RU1-RU3.
- **Rust → Arbace (Clojure) forms.** Rust's ownership has no counterpart in a collected heap
  that a translation could keep; the result would be an interpreter of MIR in Clojure's clothes.
- If one is ever wanted, a separate survey, from MIR as forms.

### 5.3 Effort

By repr/haskell's record (H0 spec and R1 in one day, 2026-10-17: the helper, the converter, the
braces printer and the tree and lines levels over 483 modules, 3,932 lines of Arbace and
Haskell) and rust-lisp's (reader, translator, printer and the corpus round trip in one day,
2026-10-05, 7,441 lines), on a language whose parsed tree (`rustc_ast/src/ast.rs`, 4,492 lines,
about 320 variants) is smaller than GHC's:

| step | days |
|---|---|
| RS0 spec (`doc/rust/SPEC.md`: rust-lisp's SPEC with the respelling, namespaces, metadata, modes) | 1 |
| helper (`tools/rsdump`: E3's driver with `tree` and `lines` verbs, the version check, `rustc_lexer` tokens and comments for `:full`) | 0.5-1 |
| converter (forms from the dump; rust-lisp's `printer.rs` as checklist; macro arguments as forms where the tokens agree) | 1.5-2 |
| printer (forms → Rust, implicit parentheses after `FixupContext`, rust-lisp's `shape.rs` as checklist) and the tree and lines levels over std's five crates | 2-3 |
| `:full` and the text level | 1-2 |
| interface level (rustdoc's JSON); the rest of `library/` | 1 |
| in all, to tree + interface + text over std | about 7-10 |

### 5.4 Risks

- **rustc's internals are unstable.** `rustc_private` crates change every six weeks (E8's 16
  errors between two release trains); a generic helper over `Debug` limits the change to the
  converter's table, but the Debug text itself is not a contract (six quirks found, E3, E9).
- **Alpine edge moves `rust`**, and with it `rustc-dev`, which must match `rustc` exactly; the
  mirror drops old apks. Pin by version and hash, keep the apks in `.tmp/` (or a pinned upstream
  toolchain).
- **`RUSTC_BOOTSTRAP`** is a supported escape hatch for rustc's own build, discouraged elsewhere;
  confined to the helper and the checker.
- **Macros** are tokens: the forms of macro-heavy code (`stdarch`, `macro_rules!` bodies,
  declarative DSLs) are largely `(:tt "...")` strings, holding Rust text, not forms. The bridge is
  still lossless, but RU2-RU3 gain little there.
- **Unstable syntax** in std moves faster than any reader but rustc's (E4, E6's `final`).
- **Columns reach behaviour** (§1.3): `:lines` printed crates differ in panic messages.
- **Silent misreads** (`(^ a b)`, `(@ x p)`, `"\01"`, `r"..."`): every bare name and literal checked
  by reading it back.
- **Size**: forms about 9 times the source (E3, before the converter's compaction); std's are
  131 MB as dumps.
- **No consumer yet**: tie it to a held corpus (std at the pinned rustc).

## 6. Recommendation and proposals

Recommended: a representation, not a translator; rust-lisp's grammar respelled by repr/go's rule,
paths as qualified symbols; repr/haskell's architecture (a generic helper on the host's rustc,
every decision in Arbace's converter, Arbace's printer); the tree, interface and text levels
first; scheduled after repr/haskell's R2-R6, beginning with its spec.

- **RS1 Purpose.** (a) A bridge only: read, print, the round trip; Rust code held and studied as
  forms (RU1-RU3); (b) a bridge aimed at a translator now (RU4). *Recommended: (a).*
- **RS2 The grammar's basis.** (a) rust-lisp's SPEC (D1-D20, I1-I6, S1-S8, §10's implicit
  parentheses) respelled for Arbace's reader; (b) a new grammar derived from rustc's AST in
  repr/go's style; (c) rust-lisp verbatim (unreadable, E1). *Recommended: (a).*
- **RS3 The respelling.** (a) repr/go's rule: Rust's spelling where Arbace's reader reads it back
  unchanged, the table of §3.1 otherwise (paths, `:has`, `:turbofish`, `:attr`, `:tt`, `:braces`,
  `\c` characters, `(:lit ...)` for literals that do not read back, `:bytes`, `:raw`, `bit-xor`,
  `:at`), every bare name and literal checked by reading it back; (b) the same with other words;
  (c) a reader extension for Rust's lexemes. *Recommended: (a)*, the words to settle in the spec;
  (c) breaks principle 5.
- **RS4 Paths.** (a) `std.io/Write`: all segments but the last as the namespace, Go forms'
  `pkg/Name` (§3.2), `(:global ...)` for a leading `::`, rust-lisp's `:path` and `:qself` for
  generic segments and qualified paths; (b) `std:io:Write`, one plain symbol; (c) `(:path std io
  Write)` everywhere. *Recommended: (a).*
- **RS5 Namespaces and heads.** (a) One namespace per source file (`rs.core.option` for
  `core/src/option.rs`, file `rs/core/option.ae`; a crate root `rs.core`), top-level heads
  qualified (`rs/fn`, `rs/struct`, `rs/impl`, `rs/use`, `rs/mod`, `rs/macro_rules!`, `rs/attr!`),
  rust-lisp's unqualified heads inside; (b) rust-lisp's unqualified top level in a file no one
  evaluates. *Recommended: (a)*, as Haskell forms' H4: a top-level `(fn ...)` or `(let ...)`
  would be Clojure's.
- **RS6 Macros and token trees.** (a) rust-lisp's D10: a call's arguments as forms when they
  translate back to the same tokens, else the call's token tree as Rust text `(:tt "...")`;
  `macro_rules!` bodies always `(:tt ...)`; attributes' arguments likewise (D11); (b) every
  token tree as a vector of token forms (each token respelled); (c) always text. *Recommended:
  (a)*; (b) only if a consumer needs macro bodies as data.
- **RS7 Attributes.** (a) `(:attr ...)` forms before the form they attach to, in Rust's order,
  `(:attr! ...)` for inner ones (rust-lisp's prefixes as forms); (b) a wrapper `(:attrs [...]
  form)`; (c) Clojure metadata `^{:rs/attrs [...]}` (it cannot attach to numbers or strings, and
  attributes go on expressions, parameters and fields too). *Recommended: (a)*, the spec to check
  every position where attributes may stand (parameters, fields, match arms, statements).
- **RS8 Reading.** (a) `tools/rsdump`, a rustc_private driver dumping rustc's parsed tree
  generically (E3), with `arbace.repr.rust.convert` deciding every form; (a') `-Zunpretty=ast-tree`
  transcoded in Arbace, no helper (E2); (b) syn (E4); (c) rust-analyzer's parser (E5); (d)
  rust-lisp's printer ported plus a respelling pass (E8); (e) a Rust parser in Arbace.
  *Recommended: (a)*; (a') as a fallback when `rustc-dev` is missing; (e) only if Rust must be
  read where rustc cannot run.
- **RS9 rustc as a dependency.** (a) The host's rustc (Alpine's `rust`), with `rustc-dev` and
  `rust-src` of the same build fetched as apks into `.tmp/` and pinned by SHA-256 (no host change),
  the version recorded in the forms and checked by the helper (`tools/rsdump/RUSTC_VERSION`), at
  conversion and check time only; (b) `apk add rustc-dev rust-src` on the host, recorded in
  doc/ALPINE.md's set; (c) an upstream toolchain pinned by hash, independent of Alpine.
  *Recommended: (a)*, moving to (c) if Alpine's moves become a burden.
- **RS10 Printing.** (a) A printer in Arbace modelled on `rustc_ast_pretty` (E6: its output gives
  the same tree for 1,069 of 1,074 std files), implicit parentheses by rust-lisp's §10, then
  `:full` byte for byte; (b) print through rustc (forms → helper → rustc's pretty printer).
  *Recommended: (a)*; rustc's printer as the checker's candidate maker.
- **RS11 Positions.** (a) Modes `:lines` (default, readable) and `:full` (every span, token and
  comment as `:rs/pos` metadata and data, the spellings; printed back byte for byte), as repr/go
  and repr/haskell, `:full` early since columns reach panic messages; (b) `:full` only; (c) no
  positions (rust-lisp's D17: positions of the `.rsl` file itself). *Recommended: (a).*
- **RS12 The round trip's levels.** (a) Tree (D20's normalization plus lines), interface
  (rustdoc's JSON without spans) and text first (`bin/repr-rust roundtrip`); code and behaviour
  later, by rust-lisp's run-corpus and std-from-forms methods; (b) tree only, as rust-lisp.
  *Recommended: (a).*
- **RS13 Configuration.** (a) One conversion per file under the rustc version and the crate's
  edition, recorded in the forms; `#[cfg]` kept as written (no target, no cfg resolution);
  `#[path]` and `include!` files converted as files; (b) per target, cfg resolved (after
  expansion). *Recommended: (a).*
- **RS14 Corpus.** (a) std's five crates of the pinned rustc (`rust-src`, 1,074 files), then the
  rest of `library/` and the compiler (`rustc-src`), then a pinned crate set, and rustc's test
  suite (source tarball, pinned) for behaviour; (b) the Cargo cache's 518 crates (informal, not
  pinned); (c) rust-lisp's tree (1.101-nightly sources, which the host's 1.99.1 does not
  build). *Recommended: (a).*
- **RS15 Translators.** (a) None now; a later survey from MIR (`repr/mir`, stable MIR through
  `rustc_public`) if ever wanted; (b) Rust → Go forms now; (c) Rust → Clojure forms now.
  *Recommended: (a).*
- **RS16 Schedule.** (a) After repr/haskell's R2-R6 (and independent of goi), beginning with
  `doc/rust/SPEC.md` for the user's review; (b) the spec now, in parallel with the running work,
  the build after it (what H13 decided for Haskell); (c) not at all. *Recommended: (a)*; (b) if
  capacity allows, as for Haskell.
- **RS17 rust-lisp's role.** (a) A reference: its SPEC the grammar's basis, its `printer.rs` the
  converter's checklist, `shape.rs` the printer's, its three corpus scripts the checks' model; no
  code vendored, no build; (b) a dependency (its front end reads Rust, RS8 (d)); (c) a back end
  (Rust forms respelled to `.rsl` and compiled by a rust-lisp build). *Recommended: (a)*, as
  go-lisp's and ghc-lisp's roles were decided (G2C-SURVEY §9, question 10; H14).

## 7. Sources

- rust-lisp, `/root/rust-lisp` (github.com/arbace/rust-lisp), branch `rust-lisp`, commit
  `f28e0de8c2a8a8883fa8a6d4a2c9697e6ea0d280` (2026-10-05), forked at
  `602727f26878dcf9eb2999e2f58e5df1269ff47e` (rust-lang/rust master, 2026-10-05, 1.101.0):
  `CLAUDE.md`, `AGENTS.md`, `rustlisp/README.md`, `rustlisp/DESIGN.md`, `rustlisp/SPEC.md`,
  `rustlisp/check-corpus.sh`, `run-corpus.sh`, `std-from-lisp.sh`, `rustlisp/upstream/README.md`,
  `compiler/rustc_parse/src/lisp/*.rs`, `tests/ui/rust-lisp/`, `tests/run-make/rust-lisp/`.
- ghc-lisp and go-lisp, as described in [REPR-HASKELL-SURVEY.md](REPR-HASKELL-SURVEY.md) and
  [G2C-SURVEY.md](G2C-SURVEY.md) §1.2, §7, §9 (question 10).
- Arbace: [go/SPEC.md](go/SPEC.md) (principles, §3, §4.4, §7.5, §10), [go/PRINTER-NOTES.md](go/PRINTER-NOTES.md)
  ("Mode :full as built"), [go/ROUNDTRIP.md](go/ROUNDTRIP.md), [haskell/SPEC.md](haskell/SPEC.md),
  [haskell/HASKELL-NOTES.md](haskell/HASKELL-NOTES.md), [NAMING-SURVEY.md](NAMING-SURVEY.md),
  [AGENDA.md](AGENDA.md), [ALPINE.md](ALPINE.md); `test/haskell/reader_samples.ae`.
- Rust 1.99.1, Alpine edge: `rust-1.99.1-r0` (installed; `rustc 1.99.1 (58cf8f9c3 2026-10-13)`,
  `/usr/lib/librustc_driver-61690d3e4c729118.so`), `rustc-dev-1.99.1-r0`, `rust-src-1.99.1-r0`
  (fetched, not installed; SHA-256 in §3.4), `rust-analyzer-2026.10.12-r0` and `mimalloc2-2.2.7-r0`
  (fetched). rustc's sources in `rust-src`: `compiler/rustc_ast/src/ast.rs`,
  `compiler/rustc_ast_pretty/src/pprust/`, `library/`.
- syn: https://github.com/dtolnay/syn, versions 2.0.118 and 3.0.3 from the host's Cargo cache
  (`~/.cargo/registry/cache/index.crates.io-1949cf8c6b5b557f/`), with `proc-macro2` 1.0.106,
  `quote` 1.0.46, `unicode-ident` 1.0.24.
- rust-analyzer: https://github.com/rust-lang/rust-analyzer (`rust-analyzer parse`); rowan:
  https://github.com/rust-analyzer/rowan.
- The Rust project on `RUSTC_BOOTSTRAP` and `rustc_private`: the rustc dev guide,
  https://rustc-dev-guide.rust-lang.org/building/bootstrapping/what-bootstrapping-does.html and
  https://rustc-dev-guide.rust-lang.org/rustc-driver/intro.html; rustdoc's JSON output:
  https://doc.rust-lang.org/nightly/rustdoc/unstable-features.html.
- Miri: https://github.com/rust-lang/miri; mrustc: https://github.com/thepowersgang/mrustc.

## Appendix A: the experiments

Scratch under `.tmp/repr-rust-survey/` (gitignored). Stage 0 is the main checkout's
`target/seed/amd64/arbace`; scripts run as `arbace FILE.ae ARGS`. `dl/` holds the fetched apks,
each in its own directory, extracted under `x/`.

- **E1** (§2.3): `e1/rsl/` holds copies of rust-lisp's five `.rsl` files; `readall.ae` reads every
  form of each with `read` and reports the first failure; `samples.txt`, `samples2.txt`,
  `samples3.txt` are read by a copy of `test/haskell/reader_samples.ae` into `samples*.out`;
  `map.ae` checks that a 20-field map keeps its order (it does, as an array map).
- **E2** (§3.3 (a')): `e2/sample.rs`, `RUSTC_BOOTSTRAP=1 rustc --edition 2024
  -Zunpretty=ast-tree sample.rs` (579 bytes → 90.9 KB); `at.sh` over std (byte counts only).
- **E3** (§3.3 (a)): `e3/sysroot/` is links to the host's `/usr/lib/rustlib/x86_64-alpine-linux-musl/lib`
  plus `rustc-dev`'s; `rsdump.rs` (below, SHA-256 `da8142e1...5647650`) built with
  `RUSTC_BOOTSTRAP=1 rustc --edition 2024 -O --sysroot sysroot rsdump.rs -o rsdump`;
  `corpus/one.sh` runs it per file with the crate's edition (`xargs -P 16 -n 10`);
  `corpus/readdir.ae` reads every dump with `read` and counts nodes.
- **E4** (§3.3 (b)): `e4/syndump/` and `e4/syndump3/`, a 15-line `syn::parse_file` program with syn
  2.0.118 and 3.0.3, `cargo build --offline --release`; run over std's 1,074 files.
- **E5** (§3.3 (c)): `rust-analyzer parse < FILE` over std's 1,074 files, `ERROR@` nodes counted.
- **E6** (§4.3): `e6/rt.sh`: `-Zunpretty=normal`, both sides dumped by `rsdump`, spans and spacing
  removed by `sed`, compared.
- **E7** (§4.1): `e7/a`, `b`, `c`: a 12-line crate, a copy, and its `-Zunpretty=normal` printing;
  `.rmeta` and `.rlib` compared with `--remap-path-prefix`, `-Zunpretty=hir` and rustdoc's JSON
  (spans removed) compared.
- **E8** (§2.6): `e8/src/lisp/` is a copy of rust-lisp's `compiler/rustc_parse/src/lisp/`,
  compiled as a library against E3's sysroot.
- **E9** (§3.3 (a)): `e9/one.sh` dumps every `.rs` of each crate in `~/.cargo/registry/src/` with
  its edition, output discarded; the 11 failures of a first transcoder (quadratic on long string
  literals, the 60 s timeout) rerun with the final one.

```rust
// Read-only survey experiment (repr/rust, E3): rustc's parsed AST of one file (before macro
// expansion and before out-of-line modules are loaded), as Clojure-reader forms. The tree is
// taken from rustc's own Debug impls (`{:?}`, as -Zunpretty=ast-tree prints it with `{:#?}`) and
// transcoded generically: `Name { a: x, b: y }` -> `(Name :a x :b y)`, `Name(x, y)` -> `(Name x y)`,
// `[x, y]` -> `[x y]`, an anonymous tuple -> `(tuple x y)`, strings re-escaped for Clojure, an
// ident `x#0` -> `(id "x" 0)`, a span `FILE:l1:c1: l2:c2 (#n)` -> `[l1 c1 l2 c2]`, node ids dropped.
// The helper decides nothing about forms. Usage: rsdump [--debug] [rustc args] FILE.rs
#![feature(rustc_private)]
extern crate rustc_ast;
extern crate rustc_driver;
extern crate rustc_interface;
use rustc_driver::{Callbacks, Compilation};
use rustc_interface::interface::Compiler;
use std::fmt::Write;

struct Dump { file: String, debug: bool }

impl Callbacks for Dump {
    fn after_crate_root_parsing(&mut self, _c: &Compiler, krate: &mut rustc_ast::Crate) -> Compilation {
        let text = format!("{:?}", krate);
        if self.debug { println!("{}", text); return Compilation::Stop; }
        let mut t = T { s: text.as_bytes(), i: 0, file: self.file.as_bytes(), out: String::new() };
        t.value();
        t.ws();
        if t.i != t.s.len() { panic!("rsdump: trailing text at {}: {}", t.i, &text[t.i..(t.i + 80).min(text.len())]); }
        println!("(rsdump/config {{:rustc {:?} :file {:?}}})", "1.99.1", self.file);
        println!("{}", t.out);
        Compilation::Stop
    }
}

struct T<'a> { s: &'a [u8], i: usize, file: &'a [u8], out: String }

impl<'a> T<'a> {
    fn peek(&self) -> u8 { if self.i < self.s.len() { self.s[self.i] } else { 0 } }
    fn ws(&mut self) { while self.i < self.s.len() && (self.s[self.i] == b' ' || self.s[self.i] == b'\n') { self.i += 1; } }
    fn eat(&mut self, c: u8) -> bool { self.ws(); if self.peek() == c { self.i += 1; true } else { false } }
    fn word(&mut self) -> String {
        let st = self.i;
        while self.i < self.s.len() && (self.s[self.i].is_ascii_alphanumeric() || self.s[self.i] == b'_' || self.s[self.i] >= 0x80) { self.i += 1; }
        String::from_utf8(self.s[st..self.i].to_vec()).unwrap()
    }
    fn seq(&mut self, close: u8) {
        loop {
            if self.eat(close) { return; }
            self.out.push(' ');
            self.value();
            self.eat(b',');
        }
    }
    fn fields(&mut self) {
        loop {
            if self.eat(b'}') { return; }
            self.ws();
            if self.s[self.i..].starts_with(b"..") { self.i += 2; self.out.push_str(" :.."); continue; }
            let k = self.word();
            if !self.eat(b':') { panic!("rsdump: field without colon at {}: {}", self.i, String::from_utf8_lossy(&self.s[self.i.saturating_sub(60)..(self.i+40).min(self.s.len())])); }
            if k == "id" || (k == "tokens" && { let r = &self.s[self.i..]; let r = &r[r.iter().position(|&b| b != b' ').unwrap_or(0)..]; r.starts_with(b"None") || r.starts_with(b"Some(LazyAttrTokenStream") }) { // node ids carry nothing before expansion
                let o = std::mem::take(&mut self.out); self.value(); self.out = o;
            } else { write!(self.out, " :{}", k).unwrap(); self.out.push(' '); self.value(); }
            self.eat(b',');
        }
    }
    fn string(&mut self, q: u8) -> String {
        // Rust Debug escapes: \" \' \\ \n \r \t \0 \u{X}
        self.i += 1;
        let mut v: Vec<char> = Vec::new();
        loop {
            let c = self.peek();
            if c == q { self.i += 1; break; }
            if c == b'\\' {
                let e = self.s[self.i + 1]; self.i += 2;
                match e {
                    b'n' => v.push('\n'), b'r' => v.push('\r'), b't' => v.push('\t'), b'0' => v.push('\0'),
                    b'\\' => v.push('\\'), b'"' => v.push('"'), b'\'' => v.push('\''),
                    b'u' => {
                        let st = self.i + 1; // after '{'
                        while self.s[self.i] != b'}' { self.i += 1; }
                        let h = std::str::from_utf8(&self.s[st..self.i]).unwrap();
                        self.i += 1;
                        v.push(char::from_u32(u32::from_str_radix(h, 16).unwrap()).unwrap());
                    }
                    _ => panic!("rsdump: escape {}", e as char),
                }
            } else {
                let b0 = self.s[self.i];
                let n = if b0 < 0x80 { 1 } else if b0 >= 0xf0 { 4 } else if b0 >= 0xe0 { 3 } else { 2 };
                let ch = std::str::from_utf8(&self.s[self.i..self.i + n]).unwrap().chars().next().unwrap();
                v.push(ch); self.i += ch.len_utf8();
            }
        }
        v.into_iter().collect()
    }
    fn clj(&mut self, s: &str) {
        self.out.push('"');
        for ch in s.chars() {
            match ch {
                '"' => self.out.push_str("\\\""), '\\' => self.out.push_str("\\\\"),
                '\n' => self.out.push_str("\\n"), '\t' => self.out.push_str("\\t"), '\r' => self.out.push_str("\\r"),
                c if (c as u32) < 0x20 || c as u32 == 0x7f => write!(self.out, "\\u{:04x}", c as u32).unwrap(),
                c => self.out.push(c),
            }
        }
        self.out.push('"');
    }
    fn num(&mut self) -> String {
        let st = self.i;
        if self.peek() == b'-' { self.i += 1; }
        while self.i < self.s.len() && (self.s[self.i].is_ascii_digit() || self.s[self.i] == b'.' || self.s[self.i] == b'e') { self.i += 1; }
        String::from_utf8(self.s[st..self.i].to_vec()).unwrap()
    }
    fn span(&mut self) {
        // FILE:l1:c1: l2:c2 (#n)
        self.i += self.file.len() + 1;
        let mut ns = Vec::new();
        for k in 0..4 {
            ns.push(self.num());
            if k == 1 { self.i += 2; } else if k < 3 { self.i += 1; }
        }
        self.ws();
        let c = if self.eat(b'(') { self.i += 1; let n = self.num(); self.eat(b')'); n } else { "0".into() };
        if c == "0" { write!(self.out, "[{} {} {} {}]", ns[0], ns[1], ns[2], ns[3]).unwrap(); }
        else { write!(self.out, "[{} {} {} {} {}]", ns[0], ns[1], ns[2], ns[3], c).unwrap(); }
    }
    fn value(&mut self) {
        self.ws();
        let c = self.peek();
        if self.s[self.i..].starts_with(self.file) && self.s.get(self.i + self.file.len()) == Some(&b':') { return self.span(); }
        if self.s[self.i..].starts_with(b"no-location") { self.i += 11; self.ws(); if self.eat(b'(') { while self.peek() != b')' { self.i += 1; } self.i += 1; } self.out.push_str("nil"); return; }
        match c {
            b'"' => { let s = self.string(b'"'); self.clj(&s); }
            b'\'' if self.s[self.i + 1] != b'\\' && !self.s[self.i + 1..].iter().skip(1).take(4).any(|&b| b == b'\'') => {
                // a lifetime or label ident: 'a#0
                self.i += 1; let w = format!("'{}", self.word());
                self.out.push_str("(id "); self.clj(&w);
                if self.peek() == b'#' { self.i += 1; let n = self.num(); write!(self.out, " {}", n).unwrap(); }
                self.out.push(')');
            }
            b'\'' => { let s = self.string(b'\''); let ch = s.chars().next().unwrap(); write!(self.out, "(char {})", ch as u32).unwrap(); }
            b'[' => { self.i += 1; self.out.push('['); self.seq(b']'); self.out.push(']'); }
            b'(' => { self.i += 1; self.out.push_str("(tuple"); self.seq(b')'); self.out.push(')'); }
            b'{' if self.s[self.i..].starts_with(b"{{root}}#") => { self.i += 9; let c = self.num(); write!(self.out, "(id \"{{{{root}}}}\" {})", c).unwrap(); }
            b'{' => { self.i += 1; self.out.push_str("(map"); self.fields(); self.out.push(')'); }
            b'#' => { self.i += 1; let n = self.num(); write!(self.out, "(ctxt {})", n).unwrap(); }
            b'-' | b'0'..=b'9' => {
                let n = self.num();
                if self.peek() == b'#' { self.i += 1; let c = self.num(); self.out.push_str("(id "); self.clj(&n); write!(self.out, " {})", c).unwrap(); }
                else { self.out.push_str(&n); }
            }
            _ => {
                let mut w = self.word();
                if w.is_empty() { panic!("rsdump: unexpected {:?} at {}", c as char, self.i); }
                if w == "r" && self.peek() == b'#' { self.i += 1; w = format!("r#{}", self.word()); }
                if self.peek() == b'#' { // an Ident: name#ctxt
                    self.i += 1; let n = self.num();
                    self.out.push_str("(id "); self.clj(&w); write!(self.out, " {})", n).unwrap();
                    return;
                }
                if w == "lifetime" && self.peek() == b'(' { // lifetime(ID: 'name)
                    while self.peek() != b'\'' { self.i += 1; }
                    let st = self.i; while self.peek() != b')' { self.i += 1; }
                    let name = String::from_utf8(self.s[st..self.i].to_vec()).unwrap(); self.i += 1;
                    self.out.push_str("(lifetime "); self.clj(&name); self.out.push(')');
                    return;
                }
                let save = self.i; self.ws();
                match self.peek() {
                    b'(' => { self.i += 1; write!(self.out, "({}", w).unwrap(); self.seq(b')'); self.out.push(')'); }
                    b'{' => { self.i += 1; write!(self.out, "({}", w).unwrap(); self.fields(); self.out.push(')'); }
                    _ => { self.i = save; self.out.push_str(&w); }
                }
            }
        }
    }
}

fn main() {
    let mut args: Vec<String> = std::env::args().collect();
    let debug = args.iter().any(|a| a == "--debug");
    args.retain(|a| a != "--debug");
    let file = args.last().unwrap().clone();
    rustc_driver::run_compiler(&args, &mut Dump { file, debug });
}
```

## Decided (2026-10-17, the user)

RS1-RS15 and RS17 as recommended: a bridge only, no translator (RS1, RS15); rust-lisp's grammar
respelled by repr/go's rule (RS2, RS3); paths as `std.io/Write` (RS4); a namespace per file with
qualified top-level heads (RS5); macro arguments as forms when they print back to the same
tokens, else `(:tt "...")` (RS6); `(:attr ...)` forms (RS7); `tools/rsdump` on the host's rustc
with `rustc-dev` and `rust-src` fetched and pinned (RS8, RS9); a printer after
`rustc_ast_pretty`, modes `:lines` and `:full` (RS10, RS11); the tree, interface and text levels
over std's five crates, one conversion per file under the rustc version and edition (RS12-RS14);
rust-lisp a reference (RS17). RS16 (b): the spec (doc/rust/SPEC.md) now, the build after.
