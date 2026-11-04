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
    bin/repr-rust native-errors [--edition E] [--list FILE] [FILE...]   (make repr-rust-native-errors)

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
- Recovery past the first error. Where rustc's parser reports an error, even one it recovers
  from, the native reader rejects the file with rustc's first error (see "Errors" below); the
  helper rejects the same files, since it refuses a file on which rustc reported errors. rustc's
  suggestions, labels, notes, recovered trees and further errors are not ported.
- `include!` expression files (`--expr`) are read with nothing stripped, as rustc does; an empty
  span at the file's very start would be written as the dummy span (`nil`) where the helper,
  whose expression file follows an empty crate root in the source map, writes `[1 1 1 1]`.
- Unicode: `XID_Start`/`XID_Continue` come from the host's Unicode identifier tables (the Go
  runtime's: `Character/isUnicodeIdentifierStart`, `Part`, less the ignorables; ZWJ and ZWNJ
  continue), not from the tables rustc_lexer calls, those of the crate `unicode-ident` 1.0.22
  (`compiler/rustc_lexer/Cargo.toml`; rustc_lexer requires `unicode-properties`, its emoji
  tables, to be of the same Unicode version). The two may differ in Unicode version and in the
  derivation (unicode-ident's XID properties, NFKC-closed, against Java's
  identifier-start/part classes): files whose identifiers lie where they differ may disagree.
  Emoji (`is_emoji_char`, from `unicode-properties`) are approximated by the symbol category
  and the block U+1F000-1FAFF. An identifier with an emoji is accepted, as rustc's parser
  accepts it (rustc records it and reports "identifiers cannot contain emoji" after expansion,
  so `-Z parse-crate-root-only` and the helper accept the file).
- Speed: stage 0 evaluates the reader from the checkout: about 3,300 lines a second (core's
  and std's 60 files, 53,158 lines, in 16 s, one process, 0.55 GB at its peak); a corpus is read
  by `REPR_RUST_NATIVE_JOBS` processes (default 4).

## Errors

Built on 2026-11-04 (branch `rust-errors`, from `rust-native`). A rejected file's dump is
`(rsdump/error "error[CODE]: MESSAGE")` and `(rsdump/diagnostic {:message M :code C :span [l1
c1 l2 c2]})`: rustc 1.99.1's first error of the parse, its message, code and primary span (the
first primary span of `rustc --error-format=json`, lines from 1, columns characters from 1),
as `rustc -Z parse-crate-root-only` reports it. `arbace.repr.rust.native errors` prints a file's
first error as a line (FILE, ok|err, l1:c1-l2:c2, code, message) and compares the lines with
rustc's (`--ref`); `bin/repr-rust native-errors` makes rustc's lines
(`bin/lib/repr-rust/rustc-errors`, the host's rustc at the pin, `RUSTC_BOOTSTRAP=1` for `-Z`)
and compares. What decides the first error, ported from rustc_parse 1.99.1
(`compiler/rustc_parse/src/`: the diagnostics' messages are inline in this version,
`diagnostics.rs`'s `#[diag("...")]` structs, not `errors.rs` and `messages.ftl`):

- The expected token types (`parser/token_type.rs`): `check`, `eat`, `check_keyword`,
  `eat_keyword`, `check_ident`, `check_path`, `check_type`, `check_lifetime`, `check_const_arg`,
  `check_plus`, `break_and_eat` add the TokenType they looked for to a 128-bit set in the
  parser's state (two longs, so a snapshot keeps it), `bump` clears it; `check-ne`, `eat-ne`,
  `eat-kw-ne` and `is?` are rustc's `_noexpect` checks and `self.token == ...` comparisons, which
  add nothing. Each check of the port follows rustc's choice; the explicit inserts rustc makes
  (`TokenType::Operator` before the operators' loop, `Lt` and `OpenParen` in a type path's
  segment, `OpenBrace` after a closure's `async`, `Colon` after a shorthand field) are made too.
- `expected_one_of_not_found` (`parser/diagnostics.rs`): the set's strings sorted and deduplicated,
  "expected X, found Y", "expected one of X, Y, or Z, found W", "unexpected token: W"; the
  missing-`;` cases rustc recovers from (ExpectedSemi at the previous token's end or at a `,` or
  `:`), `==` for `=`, `async move` in 2015, too many `#` after a raw string; `unexpected_err`
  ("expected `X`, found Y" when nothing else was expected); `token_descr` and
  `pprust::token_to_string` (keyword, reserved keyword, reserved identifier, doc comment);
  `expected_ident_found` (with its recovered cases emitted).
- Returned and emitted errors. An error rustc returns (`Err(diag)`) is `fail`: thrown with the
  diagnostic in the parser's state, which a snapshot's restore cancels (`attempt`, rustc's
  `create_snapshot_for_diagnostic` then `cancel` and `restore_snapshot`); one rustc emits
  (`dcx().emit_err`, and an `Err` a caller emits) is `emit`: recorded at once in the parse's
  list, then thrown (this parser stops where rustc recovers). So an error emitted inside a
  speculative parse rustc then abandons still rejects the file, as in rustc (the
  over-acceptance the first version allowed). The first error is the first emitted; the
  top-level returned error comes after those emitted before it. Stashed errors keep rustc's
  order: an expression in a pattern (`StashKey::ExprInPat`, the expression parsed in its place
  as rustc's snapshot does) is emitted at the end of its statement, a missing type of a `const`
  or `static` (`ItemNoType`) and a lifetime starting with a digit (`LifetimeIsChar`) after every
  other error of the parse.
- Recovery before the first error, where it changes that error's message or span: the
  separator recovery of `parse_seq_to_before_tokens` (the element after a missing `,` parsed
  first, its own errors coming first), ternary operators, `:` for `;`, malformed loop labels,
  `match` arms (the missing comma, a body without braces, `=>` typos), struct expression and
  pattern fields, `..` in struct patterns, the generic arguments' unbraced const expressions
  (`recover_const_arg`), `<T>::` without angle brackets (with `to_ty`), path single and triple
  colons, `fn` parameters (the pattern route's expected set, `self` not first, anonymous
  parameters in 2015, patterns in bodiless methods), `fn`'s front matter, missing keywords before
  an item, trait bounds' modifiers, types (`+`, `?`, `typeof`, C-style pointers, `dyn mut`),
  `impl` headers, struct fields, item lists, `use` trees, `let` chains and forbidden `let`s at
  the `let`'s span.
- The lexer: each error of `lexer/mod.rs` with rustc's span and code (unterminated literals and
  comments, raw strings' starters, terminators and hashes, number literals, unknown prefixes,
  guarded strings in 2024, raw identifiers and lifetimes, doc comments' bare CR, unknown tokens
  with their repeats and the confusables of `unicode_chars.rs`), the escape errors of
  `rustc-literal-escaper` with `unescape_error_reporting.rs`'s messages and spans (the escaper's
  scan order: a `\u{...}`'s characters, digits and braces checked as it reads them); and the
  token trees' errors (`lexer/tokentrees.rs`, `lexer/diagnostics.rs`): on the first unmatched
  delimiter the file is lexed again with rustc's recovery (a mismatched closer closes the
  innermost open delimiter and is kept for an outer one it matches; `report_missing_open_delim`
  drops the mismatches at `)` and `]` an unexpected closer reports), a lexer error on the way
  first (rustc emits those as it lexes), then the mismatched closing delimiters, then the
  stream's error (an unexpected closer at the top, the file's end in an open delimiter).

Not ported: suggestions, labels and notes (only the message, code and primary span are compared);
the recoveries after the first error; `parameter_without_type`'s and `recover_fn_ptr_with_generics`'s
finer cases, `check_mistyped_turbofish_with_multiple_type_params` ("`<` is interpreted as a start
of generic arguments ..."), `maybe_recover_struct_lit_bad_delims`, the closure-body recovery
(`recover_missing_braces_around_closure_body`), the block-label and `if`-without-block
recoveries, frontmatter's errors, the text-direction lints (warnings).

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
- The first errors (branch `rust-errors`, light runs, no corpus; scratch in the worktree's
  `.tmp/re/`): the first agent's edge cases and mutants (39 files at 2015, 2018, 2021, 2024;
  1,100 mutants, 1,400 runs at their editions), 400 new mutants of 40 other core and std files at 2021
  (seed 77) and 300 held out of 40 more at 2024 (seed 1234, run once the work was done): of
  2,256 files, 571 accepted by both, none by one reader alone, and of the 1,685 rejected by
  both 1,650 first errors equal (message, code and primary span; 97.9 %), 1,432 of 1,464 on the
  sets worked on and 218 of 221 on the held-out set. The 35 left: rustc's
  `check_mistyped_turbofish_with_multiple_type_params` (6), the recoveries listed above as not
  ported, inner attributes after doc comments, block labels, a few expected sets. The
  accepted files' dumps are still the helper's: the 60 and 50 library files and the 42
  compiler files of the first checks (and the 60 with `--full`), the mutants' and edge cases'
  equal dumps.
