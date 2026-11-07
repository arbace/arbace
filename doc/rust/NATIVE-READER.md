# repr/rust's native reader

Rust text read into Rust forms by Arbace code alone: no rustc runs at read time. The native reader
writes, for each Rust file, the dump the helper `tools/rsdump` writes ([SPEC.md](SPEC.md) §10:
the configuration, rustc's parsed tree as its Debug transcodes it, the facts `rsdump/mac-exprs`
and `rsdump/meta`, the lexer's `rsdump/comments` or, with `--full`, `rsdump/tokens`), so the
converter (`arbace.repr.rust.convert`) reads its dumps as it reads the helper's and gives the same
forms. The helper stays the oracle: `bin/repr-rust native-check` compares the two readers' dumps.
Built on 2026-11-04 (branch `rust-native`, from main at `46d04247`), against rustc 1.99.1 (the
pin `tools/rsdump/RUSTC_VERSION`, `58cf8f9c3`) and the sources of Alpine's `rust-src-1.99.1-r0`
(pinned by `tools/rsdump/apks.sha256`), read in place in `.tmp/repr-rust/apks/rust-src/`.

## Use

    bin/repr-rust dump --reader native [--full] [--expr] [-o DIR] [--edition E] FILE...
    bin/repr-rust read --reader native [--full] [--edition E] [-o DIR] ... FILE.rs...
    bin/repr-rust read --reader native [--full] --corpus [CORPUS]
    bin/repr-rust native-check [--full] [--edition E] [FILE...]     (make repr-rust-native-check)

`REPR_RUST_READER=native` makes it the default reader of `dump` and `read`; neither needs rustc
then (a corpus's files come from `bin/repr-rust corpus fetch`). A native dump's `:helper` is
`native:sha256:H`, H the digest of the reader's sources and the pin. `native-check` without files
runs `test/rust/native-check.txt`: the samples of `test/rust/sample/` at their editions (the
expression file of `include!` with `--expr`) and 60 files of core and std.

## Design

Three namespaces, evaluated from the checkout like the rest of repr/rust (not embedded, out of
`gate_key`):

- `arbace.repr.rust.lex` (`arbace/repr/rust/lex.ae`): `rustc_lexer` (the raw lexer, its character
  classes, shebang and frontmatter), `rustc_parse::lexer` (the cooking of raw tokens into
  `token::Token`s: keywords and raw identifiers by edition, literals and their checks, the
  reserved prefixes, guarded strings, doc comments, gluing) and `lexer/tokentrees.rs` (the token
  trees), ported function by function, with the escape checks of the crate
  `rustc-literal-escaper` 0.0.8 that rustc calls. Positions are code point indices into the text
  as rustc's source map holds it (a byte order mark removed, `\r\n` made `\n`): the dump's columns
  count characters, so code points give rustc's lines and columns, and the span (0, 0) is rustc's
  dummy span, written `nil`. The token trees are one flat array, the delimiters included, each
  token with its spacing, and the index of each delimiter's partner: a `TokenStream` is a range of
  it, so a macro call's or an attribute's tokens are never copied.
- `arbace.repr.rust.parse` (`parse.ae`, and `parse/expr.ae`, `parse/pat.ae`, `parse/item.ae`,
  which it loads): `rustc_parse::parser` ported rule for rule, function by function in rustc's
  order and with its names: `mod.rs` (the cursor, `check`/`eat`/`expect`, `break_and_eat`'s
  ungluing, delimited sequences, visibility, ABI, token trees), `attr.rs`, `path.rs` (turbofish,
  angle and parenthesized arguments, constraints), `generics.rs` (with `where` and contracts),
  `ty.rs`, `expr.rs` (precedence climbing over `AssocOp` with rustc's bounds, the restrictions,
  `can_begin_expr`, the dot and call chains with `break_up_float`, closures, `async`/`gen`/`try`
  blocks, `let` chains by edition, `match`, struct literals and their ambiguity checks, labels),
  `stmt.rs` (`let`-`else`, macro statements, trailing semicolons, `expr_requires_semi_to_be_stmt`),
  `pat.rs`, `item.rs` and `function.rs` (every item kind, `macro_rules!` and `macro` 2.0, the
  front matter of functions, `impl` and `trait` headers, `extern` blocks, delegation), with
  `rustc_ast`'s `token.rs` predicates and `util/{classify,parser}.rs`. The parser's state is two
  arrays (the token and the previous one; the cursor's index, restrictions, angle bracket counts,
  the edition), so rustc's snapshots are two array copies. Nodes are built directly in the dump's
  shapes (`(Expr :kind K :span S :attrs A)`, variants as symbols or lists, `None`/`(Some x)`,
  vectors); spans, identifiers, lifetimes and token streams are small values while parsing.
- `arbace.repr.rust.native` (`native.ae`): the dump. The tree written as the helper writes it
  (`Ident`'s Debug with rustc's raw guess, token streams as `TokenStream`/`Delimited` with their
  spacing, `Pu128` integers), the facts as `tools/rsdump`'s visitor computes them (each macro
  call's tokens parsed by a parser over their range as `expr (, expr)*` or `e; n`, each normal
  attribute's `Attribute::meta`, ported from `rustc_ast`'s `attr/mod.rs` and `util/literal.rs`,
  in `rustc_ast::visit`'s order), the lexer's tokens as the helper's `lex`; and the check, which
  compares two dumps as data (the configuration but the reader's name) and reports the first
  difference with the path of node names to it.

What rustc's parser does that reading does not need is not ported: `collect_tokens` (the
`tokens` fields, which the dump drops), `nonterminal.rs` (macro fragments: expansion's), the
`asm!`, `cfg_select!` and builtin-macro argument parsers (macro arguments stay token trees, as
in rustc's parsed tree).

## What is left out

- Macro expansion. `macro_rules!` definitions and invocations are read as rustc's parser reads
  them, token trees; expanding them is not reading (the helper's tree is before expansion too).
- Recovery and rustc's messages. Where rustc's parser reports an error, even one it recovers
  from, the native reader stops: the file is rejected with a message of its own (the first
  error, at its position), and the helper rejects the same files, since it refuses a file on
  which rustc reported errors. rustc's wording, suggestions, recovered trees and further errors
  are later work. An error rustc emits inside a speculative parse it then abandons is not
  reported here: an over-acceptance of invalid text only (none was found).
- `include!` expression files (`--expr`) are read with nothing stripped, as rustc does; an empty
  span at the file's very start would be written as the dummy span (`nil`) where the helper,
  whose expression file follows an empty crate root in the source map, writes `[1 1 1 1]`.
- Unicode: `XID_Start`/`XID_Continue` come from the host's Unicode identifier tables
  (`Character/isUnicodeIdentifierStart`, `Part`, less the ignorables; ZWJ and ZWNJ continue), not
  from `unicode-ident` 1.0.22's; emoji in identifiers (an error) are approximated. Files whose
  identifiers lie where the two tables differ may disagree.
- Speed: stage 0 evaluates the reader from the checkout: about 3,300 lines a second (core's
  and std's 60 files, 53,158 lines, in 16 s, one process, 0.55 GB at its peak); a corpus is read
  by `REPR_RUST_NATIVE_JOBS` processes (default 4).

## Checks (2026-11-04)

- `bin/repr-rust native-check`: 68 of 68 files equal (the 8 samples and the 60 library files),
  and 68 of 68 with `--full`; 45 s and 53 s.
- By hand, beyond the list (light runs, no corpus): 50 more files of alloc, core and std and 42
  of the compiler (`rustc_parse`, `rustc_ast`, `rustc_lexer`, `rustc_expand`, `rustc_hir_typeck`,
  `rustc_middle`, `rustc_resolve`, `rustc_codegen_ssa`, `rustc_errors`, `rustc_type_ir`): 92 of
  92 equal; 39 files of edge cases (raw strings and identifiers, C strings, frontmatter, let
  chains, guarded strings, edition keywords, attributes everywhere, `use` trees, `impl` and trait
  headers, const generics, closures with binders, never and guard patterns, `builtin #`, field
  defaults) at editions 2015, 2018, 2021 and 2024, and with `--full` at 2015 and 2024: each equal
  or rejected by both; 1,400 mutants of one token edit each (seeds 26-28, of the samples, the edge
  cases and library and compiler files, at 2015, 2021 and 2024): 254 equal, 1,146 rejected by
  both, none differing.
- The forms the converter makes of the native dumps are byte for byte those of the helper's
  (the samples in modes `:lines` and `:full`, the 60 library files).
