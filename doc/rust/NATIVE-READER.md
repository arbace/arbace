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

Since rust-90 (2026-11-07) the native reader is the default of `dump`, `read` and `roundtrip`
(`--reader helper` or `REPR_RUST_READER=helper`: the helper, the oracle); none of them needs rustc
then (a corpus's files come from `bin/repr-rust corpus fetch`), and the round trip's tree level
compares the native reader's trees: `arbace.repr.rust.native tree --pin PIN [--expr] -o DIR --edition
E FILE...` writes `rsdump tree`'s `.tree` and `.lines` (tools/rsdump's transcoder's `Mode::Tree` and
`Mode::Lines`: spans as `_`, identifiers without their syntax contexts, token spacing as `_`, one
compound a line indented by its depth; each span's start line with its node and field) from the
native tree, its spans marked while it is written (`*mark-spans*`) so that a byte string's vector of
integers is not taken for one. `bin/repr-rust native-check --trees` compares them with the helper's
byte for byte: 68 of 68 files equal (2026-11-07). A native dump's `:helper` is
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

## Unicode

The lexer's character classes are rustc 1.99.1's own: `rustc_lexer` calls `unicode_ident::is_xid_start`
and `is_xid_continue` (with `_` added to the start) and, for the recovery of identifiers with emoji,
`unicode_properties::UnicodeEmoji::is_emoji_char`. The crates and versions are those of rustc's
`Cargo.lock` (`unicode-ident` 1.0.24, `unicode-properties` 0.1.4, both Unicode 17.0.0; the manifest says
1.0.22, the lock decides), not the host's Unicode tables. `arbace/repr/rust/unicode.ae` is generated:

    bin/rust-unicode generate     arbace/repr/rust/unicode.ae from the crates' `src/tables.rs`
    bin/rust-unicode check        generated again into .tmp/ and compared
    bin/rust-unicode probe [all]  the probe below

`bin/rust-unicode` takes the two `.crate` files, pinned by SHA-256 in `tools/rsdump/unicode-crates.sha256`
(and compared with rustc's `Cargo.lock` when the rust-src apk is extracted), from `REPR_RUST_CRATES`, the
host's Cargo cache or static.crates.io into `.tmp/repr-rust/unicode/`; `test/rust/unicode_gen.ae` decodes
unicode-ident's trie as its `is_xid_start` reads it, and `EMOJI_STATUS` as `is_emoji_char` tests it, into
maximal ranges of the non-ASCII code points (about 700 for XID_Start, 800 for XID_Continue, 100 for
emoji). The ASCII classes stay in `lex.ae`. U+200C and U+200D are XID_Continue in Unicode 17; U+FEFF and
the other invisible characters are not identifiers.

What the classes change in a token or a tree, found by the probe:

- Identifier boundaries (where an identifier stops and an unknown character starts): the host's tables
  that the lexer used (Java's `isUnicodeIdentifierStart`/`Part` less the ignorables, which are ID_Start and
  ID_Continue, not the XID properties, which drop the characters whose NFKC form is no identifier) differed
  from unicode-ident's in 24 code points of XID_Start and 20 of XID_Continue (U+037A, U+0E33, U+0EB3,
  U+2E2F, U+309B, U+309C, the Arabic presentation forms from U+FC5E, ...).
- Emoji in identifiers. rustc's lexer recovers an identifier with emoji (`InvalidIdent`) as an `Ident`
  token (its symbol NFC-normalized) and reports "identifiers cannot contain emoji" only in
  `rustc_interface`'s passes after parsing, which reading never runs: the helper accepts `fn 😀() {}` and
  gives the identifier. The native lexer used to reject it; it now gives the `Ident`, except for a lone
  character that is a confusable of `rustc_parse/src/lexer/unicode_chars.rs`'s `UNICODE_ARRAY` and an
  emoji (U+2795, U+2796), which is an unknown start of token, an error.
- NFC: `nfc_normalize` (`unicode-normalization` 0.1.25, Unicode 17.0.0) changes only the symbol of an
  identifier or lifetime that is not NFC. The host's `java.text.Normalizer` (jdk26u, Unicode 17.0.0)
  agrees with it on the probe (NFD forms of every identifier character, Hangul jamo sequences, singletons
  such as U+212B, composition exclusions), so it stays. Confusables are lints (`uncommon_codepoints`),
  which reading does not run.

The probe (`bin/rust-unicode probe`, `test/rust/unicode_probe.ae`): for every boundary of the three
tables (the characters either side of each change of class, 3,774 of them) or, with `all`, every
non-ASCII code point of planes 0-3 and 14 and the edges of the others: `start.rs` has a line `c` each and `cont.rs` `ac`; the lines
`rustc -Z parse-crate-root-only` rejects as an unknown start of token (`RUSTC_BOOTSTRAP=1`, the pinned
rustc; its lexer runs before its parser, so every lexer error is listed) against the lines on which the
native lexer throws; and `ident.rs`, each identifier character bound by a `let` (its NFD form too), read by
both readers through `bin/repr-rust native-check` (modes `:lines` and `:full`).
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

- The second round (branch `rust-errors2`, 2026-11-04), the recoveries the first left out, each
  ported from `rustc_parse` 1.99.1 rule for rule:
  `parse_assoc_op_cast`'s snapshot (`x as usize < y`: "`<` is interpreted as a start of generic
  arguments for `usize`, not a comparison", `<<` likewise, the `'a: loop` typo, the postfix operators
  after a cast); `parse_generic_arg`'s `recover_unbraced_const_arg_that_can_begin_ty` and
  `recover_const_arg` (a type that fails to parse is tried as a const expression, whose own emitted
  errors, a label's for `S<'a, ...>`, come first); `parse_angle_args_with_leading_angle_bracket_recovery`
  ("unmatched angle bracket(s)", the state before the `<`s restored) and `check_trailing_angle_brackets`
  (its count and the struct fields' call); `parse_if_after_cond` and `parse_expr_else`
  (`recover_block_from_condition`, `error_on_extra_if`, `ExpectedElseBlock`), `error_block_no_opening_brace_msg`
  (rustc parses a statement from the token before it reports "expected `{`", and what that emits
  comes first), `maybe_recover_unexpected_block_label` (emitted), `recover_loop_else`, `parse_for_head`
  (`error_missing_in_for_loop`'s spans, `in in`, the parentheses of a head) and
  `LoopElseNotSupported`'s text; `parse_expr_labeled` whole (`recover_unclosed_char` with the lexer's
  stashed `LifetimeIsChar` error stolen, `UnexpectedTokenAfterLabel`, `RequireColonAfterLabeledExpression`
  at the expression's span) and the same for patterns and literals; the attribute policies of
  `parse_outer_attributes`/`error_on_forbidden_inner_attr` (after a doc comment, after an outer
  attribute, in a code block; the last attribute's span) and `E0753`; `parse_param_general` with
  `parameter_without_type` (a C-style `Type name`, a name or a type alone) and `parse_fn_params`
  emitting the parameter's error as rustc does; `parse_self_param`'s noexpect colon and
  `IncorrectTypeOnSelf`; `maybe_recover_bad_struct_literal_path` (a `{ a, b }` or `_ { a }` body parsed
  as the struct it is, the error at its span), `maybe_suggest_struct_literal` (a block of `field: value`,
  both parses run, rustc's four outcomes), `maybe_parse_struct_expr`'s span, `parse_expr_fn_call`
  with `maybe_recover_struct_lit_bad_delims` ("invalid `struct` delimiters or `fn` call arguments") and
  `recover_seq_parse_error` for the arguments and for tuples; `try_recover_let_missing_semi` and the
  `const` item's (`missing_semi_from_binop`), `attr_on_non_tail_expr` (an `ExpectedSemi`, not the feature
  gate's text), `check_mistyped_turbofish_with_multiple_type_params`, the `:` of a typed declaration
  followed by `.`, `recover_stmt_local_after_let` (the declaration parsed before the error) and
  `mut let`'s span; the ternary operator inside `expected_one_of_not_found` (a `?` before the missing
  `;`: rustc parses the two branches first and the first error may be theirs); the await recovery
  (`await?`, `await!(..)`, `await::x`), the closure-body recoveries (`recover_closure_body`'s two
  emitted cases and `recover_missing_braces_around_closure_body` through `current_closure`, a slot of the
  parser's state, cleared where rustc takes it), the fn-path bound (`F: fn() -> A`, `ExpectedFnPathFoundFnKeyword`),
  an `impl Trait` where a bound is (a wrong index let `B<e: impl I>` through unrejected), the
  visibility, `impl` and `mut` restrictions' `E0704` family, `expected_ident_found` for fields and
  variants, noexpect comparisons (`token == Bang`, `Eq`, `Colon` of fields and variants, a tuple field's
  default), `expect_lifetime` and `eat_label` emitting their keyword errors, and the front matter's
  errors of `validate_frontmatter` (opening whitespace, infostring, bare CR, unclosed, closing
  whitespace, length mismatch, characters after the close; `FrontmatterTooManyDashes` has no span and is
  not compared). `bin/repr-rust native-errors` and `arbace.repr.rust.native errors` now report a crash of
  the reader ("native reader failed") as an error line of its own: before, it printed as an acceptance and
  agreed with rustc on a file rustc accepts.

Not ported: suggestions, labels and notes (only the message, code and primary span are compared);
the recoveries after the first error; `recover_fn_ptr_with_generics`'s finer cases;
`check_no_chained_comparison`'s token-consuming branches and `attempt_chained_comparison_suggestion`'s probes
(the message is the same in every branch, they decide only whether the error is returned or emitted, which
matters inside a speculative parse); `recover_raw_ref_call_args`; the text-direction lints (warnings).

## Checks (2026-11-04)

- Unicode (`bin/rust-unicode probe`, `probe all`): boundaries, 3,774 characters: start 2,312 and continue
  1,589 lines rejected by rustc, the same lines by the native lexer; `all` (planes 0-3 and 14 and the
  edges, 264,073 characters, rustc about 10 minutes): 116,804 and 113,485, the same lines; the 3,351
  identifiers of the boundaries' `ident.rs` equal in both modes, and the 319,230 of `all` (every
  identifier character, its NFD form too) equal in mode `:lines` (17 minutes on a loaded host);
  `bin/rust-unicode check`: the generated file is what the pinned crates give, the pins those of rustc's
  `Cargo.lock`; native-check 68 of 68, and 68 of 68 with `--full`, after the change.

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
- The first errors, second round (branch `rust-errors2`, 2026-11-04; same scratch layout, `.tmp/re/`):
  the same three sets (the edge cases and 1,100 mutants, the 400 new, the 300 held out of the first
  round) all equal now: of 2,256 files, 571 accepted by both and 1,685 rejected by both with 1,685 first
  errors equal (message, code and primary span), where the first round had 1,650 (35 left: 1,432 of 1,464
  on the sets worked on, 218 of 221 held out). Two development sets of 500 mutants each (seeds 4242 at
  2021 and 31337 at 2024, 50 files each of the library and the compiler not used before), used to find
  what the round had still missed (405 of 406 and 360 of 364 at their first runs), end at 406 of 406
  and 364 of 364; two held-out sets of 300 mutants of 40 other files each, generated for the checks and
  not worked on: seed 777001 at 2024 (run once before the last fixes: 223 of 226 rejected by both equal,
  and one file rustc rejects that the reader accepted, `B<e: impl I>`; 227 of 227 and none accepted
  afterwards) and seed 9100913 at 2021 (run once, after every fix: 224 of 224 rejected by both equal, 76 accepted by both, none by one
  reader alone). The accepted files still dump equal: `bin/repr-rust native-check` 68 of 68 (and 68 of
  68 with `--full`), and in every set above the files both readers accept, their dumps equal to the
  helper's (`bin/repr-rust native-check --edition E FILES` from the set's directory: 571 of 571 in the
  first three, 94 + 136 + 73 + 76 in the others, none differing).

