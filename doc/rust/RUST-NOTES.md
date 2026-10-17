# repr/rust: notes as built

The build of repr/rust's milestones ([SPEC.md](SPEC.md) §12.1), what each part does and where it
departs from or adds to the spec, with the reasons. RR1 was built on 2026-10-17 (branch
`repr-rust-rr1`, from main at `80530da7`), on the host's rustc 1.99.1 (Alpine's `rust-1.99.1-r0`,
`rustc 1.99.1 (58cf8f9c3 2026-10-13)`, `librustc_driver-61690d3e4c729118.so`), `rustc-dev-1.99.1-r0`
and `rust-src-1.99.1-r0` fetched as apks, and stage 0 of that commit.

## RR1: the helper, the converter, the lines printer, the tree level

### What was built

| part | file | what |
|---|---|---|
| helper | `tools/rsdump/rsdump.rs`, `tools/rsdump/RUSTC_VERSION`, `tools/rsdump/apks.sha256` | rustc's parsed tree as Clojure-reader forms (`dump`), with §10.3's facts; the tree level's normal form and lines (`tree`, `lines`); SPEC §10 |
| shared rules | `arbace/repr/rust/syntax.ae` | the name rule (§3.2), paths as symbols (§3.3), literals and their canonical spellings (§3.6, S10), rustc's token gluing (`Token::glue`) for the token checks |
| converter | `arbace/repr/rust/convert.ae` | a dump and the Rust file to Rust forms, mode `:lines` |
| writer | `arbace/repr/rust/write.ae` | Rust forms as `.ae` text at the Rust lines, read back and compared |
| printer | `arbace/repr/rust/print.ae` | Rust forms to Rust, mode `:lines`' layout (§9.4, 1); the paren rules (§5.2) the converter applies too |
| command | `arbace/repr/rust.ae`, `bin/repr-rust`, `bin/lib/repr-rust/test` | `helper`, `corpus fetch\|list`, `dump`, `read`, `print`, `roundtrip`, `test` (§11.6) |
| corpus | `test/rust/corpus-rustc-1.99.1.edn` (made by `test/rust/corpus_make.ae`) | std's five crates: their directories, edition, type, roots, `#[path]` and `include!` files, and their 1,074 files (§11.4) |
| known differences | `test/rust/roundtrip-rustc-1.99.1-known.edn` | none (`{}`) |
| cases | `test/rust/sample/constructs.rs` | constructs std uses rarely or not at all, round-tripped by `bin/repr-rust test` |

repr/rust is not one of the tools the executable embeds: `arbace/j2g/embed.ae` excludes
`arbace/repr/rust.ae` and `arbace/repr/rust/`, as it excludes repr/haskell, and `bin/repr-rust`
runs its namespaces evaluated from the checkout (`bin/lib/tools.bash`'s `arbace_tool`).

### Results

`bin/repr-rust roundtrip` (also `make repr-rust-roundtrip`) over the corpus of RS14 (std's five
crates in `rust-src-1.99.1-r0`, SHA-256 `0849c88c...6cfb`):

| crate | files | equal at the tree level (trees and lines) |
|---|---|---|
| `core` | 288 | 288 |
| `alloc` | 93 | 93 |
| `std` | 646 | 646 |
| `proc_macro` | 17 | 17 |
| `test` | 30 | 30 |
| total | 1,074 | 1,074 |

All 1,074 convert (92,154 top-level forms, 390,471 lines of forms, 15 MB), read back as written,
print, parse with rustc without errors, and give rustc's tree of the original under N1-N4 and the
start lines of every one of its spans: 1,660,638 spans compared. No known differences. On this
host (64 cores, 32 jobs), about 45 s: the dumps 3 s, the conversion 20 s, the printing 5 s, the
trees of both sides 7 s, the rest the tools' start. `bin/repr-rust roundtrip && bin/repr-rust test`,
the step `repr-rust` of `bin/gate --full`, took 48 s on the gate's stage 0 (54 s on main's).

What the forms hold (counted over the corpus's forms): 6,626 macro calls with their arguments as
forms and 1,316 as `(:tt ...)` (rustc's parser did not take their tokens as `expr (, expr)*`, they
are brace-delimited, or, for 24, the printer's tokens or their lines are not the call's: closing
delimiters and `=>` on lines of their own inside the arguments, a trailing comma); 19,695
attributes with meta forms and 129 with `(:tt ...)`; 7,028 `(:lit ...)` literals; 544 explicit
`(:paren ...)`, every other `Paren` node implicit, and no `(:noparen ...)`; 936 item macro calls
(`rs/macro-call`), 1,126 turbofish forms, 235 qualified paths, 2 global paths. The line metadata:
7,355 `^{:line N}`, 1,928 `:rs/breaks`, 1,218 `:rs/brace`, 830 `:rs/close`, 522 `:rs/paren`.

`bin/repr-rust test` checks the spec's own cases: the reader's 291 samples of Appendix A (E1) read
as recorded; the worked example `test/rust/sample/shapes.rs` converts to exactly the forms the spec
wrote by hand in `shapes.ae` (as data, each top-level form at the same line; `rs/file` and its
`:config` compared on the keys the spec writes), and its printed file parses to the same tree and
lines (508 spans); `test/rust/sample/constructs.rs` (a shebang, unions, trait aliases, C
variadics, labeled blocks, let-else, try blocks, closures with return types, literals of every
kind, attributes on expressions, slice and box patterns) round-trips likewise (800 spans).

### The helper (SPEC §10)

- Built by `bin/repr-rust helper`: the apks of `tools/rsdump/apks.sha256` fetched once into
  `.tmp/repr-rust/apks/` (`apk fetch NAME=VERSION`, nothing installed), checked by their SHA-256 and
  each extracted into its own directory; a sysroot of links (the host's
  `/usr/lib/rustlib/x86_64-alpine-linux-musl/lib/*`, then `rustc-dev`'s 252 `.rmeta`/`.rlib` files
  and its `librustc_driver-61690d3e4c729118.so`, byte-identical to the host's) in
  `.tmp/repr-rust/sysroot/`; then `RUSTC_BOOTSTRAP=1 rustc --edition 2024 -O --sysroot ...
  tools/rsdump/rsdump.rs` (about 10 s, 458 KB, linked dynamically to the host's
  `librustc_driver`). Rebuilt when its source, `RUSTC_VERSION`, `apks.sha256` or `rustc --version`
  change (`.tmp/repr-rust/key`). `bin/repr-rust` refuses a host rustc whose version and commit are
  not the pin's, or without the pinned driver library.
- `RUSTC_VERSION` pins `version 1.99.1`, `commit 58cf8f9c3`, `driver 61690d3e4c729118`; the helper
  compiles it in (`include_str!`) and refuses to run on another rustc: the version and commit from
  `rustc_interface::util::rustc_version_str()`, the driver's hash from the name of the
  `librustc_driver-*.so` the process mapped (`/proc/self/maps`). The converter refuses a dump whose
  `:rustc` is not the pin's.
- Verbs: `dump -o DIR --edition E [--helper-hash H] FILE...` (DIR/FILE.edn each), `tree -o DIR
  --edition E FILE...` (DIR/FILE.tree and DIR/FILE.lines, from one parse), `lines --edition E FILE`
  (standard output). Several files per process (one `run_compiler` each; `bin/repr-rust` gives 8
  at a time to 32 parallel runs): the whole corpus dumps in 3 s.
- A dump: `(rsdump/config {...})` (`:rustc`, `:rustc-commit`, `:rustc-driver`, `:edition`, `:file`
  as rustc was given it, `:abs`, its absolute path for the converter's source reads, `:sha256` of
  the file, computed by the helper itself (rustc ships no crate it may use), `:helper`), the tree,
  then the facts: `(rsdump/mac-exprs [[OPEN :exprs [Expr...]] [OPEN :repeat [e n]]...])`, each macro
  call (in the tree, in an attribute's `= expr`, and, recursively, in the expressions parsed from
  another call's tokens) whose `()` or `[]` tokens rustc's parser takes as `expr (, expr)*` (or
  `e; n` in brackets), with a silent diagnostic context, keyed by the span of the call's open
  delimiter; no entry for a trailing comma, a recovered error, or an empty stream (the converter
  writes `(m!)` and `(m! [])` itself); and `(rsdump/meta [[SPAN MetaItem]...])`, each attribute's
  `Attribute::meta`, keyed by its span. A file the parser rejects or reports errors for is
  `(rsdump/error "...")`, exit status 1. The call's tokens for §7.1's check are those of the tree.
- The transcoder (the survey's, extended): node ids (every `NodeId(..)` value and the fields named
  `id`) and the collected streams (`tokens: Some(LazyAttrTokenStream(..))`, `None`) are dropped;
  `r#` begins a raw identifier only before an identifier character (S25: `r#0` is the identifier
  `r` in context 0); a character `'c'` is told from a lifetime `'a#0` by the quote after its first
  character; `ImplPolarity` prints as `"positive"`/`"negative"` (rustc's own `Debug`, without its
  span).
- `tree`: the same text with every span written `_`, the token spacings (`Joint`, `Alone`,
  `JointHidden`, `DelimSpacing {..}`) `_` and identifiers' syntax contexts dropped (N1-N4), one
  compound per line, so that a `diff` shows the first difference in context. `lines`: one line per
  span, in the tree's order, `LINE Node.field` (`- Node.field` for a dummy span), compared
  position by position: every span of the tree, the spans of the tokens inside token streams
  included (1.66 million over the corpus).

### The converter and the writer (SPEC §2-§9)

As the spec says, with these choices where it leaves one, recorded as built:

- **Lines.** Every form that stands for a node with a span carries the line its span starts on;
  the writer puts each form on its line, so the reader's `:line` of every list is its node's. For
  the spans of tokens the forms have no element for, the converter computes where the printer
  would put the token: on the line of the latest span that starts before it in the file (the
  printer reproduces every span's start and adds no line of its own); when that is not the
  token's line, the form that implies the token gets the line:
  - `:rs/brace`: the `{` of a block body (functions, loops, `if` and `else`, closures with a block
    body, `let`-`else`'s `else`); the `{` of a `match`, an impl's, a trait's or an enum's body has
    no span;
  - `:rs/close`: a closing delimiter followed by an empty span on its line: the `)` of a
    function's parameters before the empty span of a missing return type (`FnRetTy::Default`), and
    the last delimiter before the empty span of a missing `where` clause: `>` of a generic name's
    parameters or of a type's arguments, `)` of parameters, of a tuple struct's fields, of a tuple
    type or of a function pointer's parameters, `]` of a slice type; also a macro call's or an
    attribute's closing delimiter (§7.1, §9.1);
  - `:rs/paren`: the `(` of implicit parentheses that begins a line.
  - Heads: a list's head stands where Rust writes the token it stands for, after the first
    operand. A binary operator's head gets its line (`^{:line N}`, always written) unless it is
    the printer's default, its right operand's line when that begins a later line, else the
    current one: rustfmt's chains `a\n + b\n + c` need no metadata and stay flat, `(+ a b c)`; a
    chain is flattened only when its inner operators need none. Any other head that stands for a
    span (a method's name `.m`, `=` and the compound assignments, `:index`'s `[`, `:has`'s `:`, a
    range pattern's operator) gets its line when it is not the current one (RSS9 (a)).
  - The writer writes a head that is a list (a turbofish method `(:turbofish .collect T)`) on the
    current line with every line inside it as metadata, since Rust prints it after elements the
    forms write after it.
  - `:rs/open` is not needed by the corpus and not written.
- **Implicit parentheses** (§5.2, S12): the converter writes every `Paren` node as `(:paren e)`,
  prints the file once with the printer's paren decisions logged (`print.ae`'s `implicit-parens`:
  for each position, whether rust-lisp's rules, `FixupContext` and `classify`, would add
  parentheses), and rewrites: a `(:paren e)` the printer would add is left implicit (`e`, with
  `:rs/paren` when its `(` begins a line), a form it would parenthesize that has none in the
  source would be `(:noparen e)` (none in the corpus). Converter and printer agree by
  construction: the same code decides both. A function item's return type allows a bound list
  without parentheses (`-> impl A + B`); rust-lisp's return-type rule stays for function
  pointers, closures and parenthesized arguments.
- **Macro calls** (§7.1, S16): the arguments are forms when the helper parsed them and the
  printer's tokens for the call, glued as rustc's lexer glues them (`Token::glue`: the printer's
  `>` `>` against the source's `>>`), are the call's, each at its line but the closing delimiter's
  (`:rs/close`); a nested call kept as `(:tt ...)` matches the source's group by its text. A single
  vector argument means brackets, a single `(:repeat e n)` in it `[e; n]`. Named format arguments
  `name = value` parse as assignments and print back as the same tokens, so they stay forms
  (§7.1's "therefore" lists them among the `:tt` cases: RR-P3). Brace-delimited calls are always
  `(:tt "{...}")`: no `(:braces ...)` argument is written. Empty calls are `(m!)`, `(m! [])`.
- **Attributes** (§8.1): the meta forms are built from the attribute's tokens (rust-lisp's
  `meta_inner`: a literal, a path, `(= path lit)`, `(path meta...)`) and kept when the printer's
  tokens are the attribute's, as for macro calls; the helper's `rsdump/meta` is dumped as §10.3
  says, for consumers, and not needed by the converter.
- **Item forms**: a tuple struct's `where` clause follows its fields, Rust's order (`(struct (P
  T) [T] (where ...))`); an item macro call at top level is `(rs/macro-call m! ...)`, in a
  module's, impl's or trait's body `(m! ...)`, its `;` the printer's for `()` and `[]`.
- **Fields**: a tuple index on a non-path base is the integer, `(. (.get c) 0)`.
- **Files outside a corpus** (`bin/repr-rust read FILE.edn`): the crate `main` (or `--crate`), the
  namespace `rs.main.` and the file's path (or `--ns-root NS` and the path, as the test's
  `rs.sample.shapes`); in a corpus, `rs.CRATE.` and the path in the crate's directory (§2.1, RSS5),
  and a namespace two files map to is an error of both (none). `:kind` is `:root` for `src/lib.rs`
  and `src/lib.miri.rs` (miri's alternative root), `:path`, `:include` from the corpus file,
  omitted for modules. `:shebang` and `:frontmatter` are read from the file (S31).
- **Plain comments** are not written in mode `:lines` (§9.3 has them as `;` comments where the
  layout allows): the forms text has the blank lines where they stood. They come with the lexer's
  comments (RR3; RR-P1).
- **Placement of lists without a span of their own** (a brace group of fields, `(:braces ...)`):
  the writer writes them where the text is, and their first element gets its line, `(rs/pub struct
  (Tally 'a) ^{:rs/breaks [[4 17]]} (:braces\n    ^{:line 16} pub :counts ...`; the spec's
  hand-written `shapes.ae` begins the group on line 16. As data the two are the same, and the test
  compares data and the top-level forms' lines.
- The converter reads every forms file it writes back with Arbace's reader and compares it with
  the forms in memory (principle 3); integer classes too (a code point is a long).

### The printer (SPEC §9.4)

The layout of mode `:lines`: each token at its form's or metadata's line when that is later than
the current one, else on the current line, separated by single spaces where rustfmt puts them
(none after `(`, `[`, prefix operators, `.`, `::`, `#`, `!` of a macro; none before `)`, `]`, `,`,
`;`, `?`, `:`); indentation by nesting, four spaces; a `}` on the current line (it has no span: a
`}` moved to a line of its own could push a later `} else {` past its line). Blank lines stand
where the source had them or its comments. A line doc comment ends its line.

### Checks and how to reproduce

```sh
bin/repr-rust corpus fetch           # the apks (once, checked by their SHA-256), extracted
bin/repr-rust roundtrip              # dump, convert, print, the trees and lines of both sides
bin/repr-rust test                   # the reader's samples, the worked example, constructs.rs
bin/repr-rust read --corpus          # the forms only: .tmp/repr-rust/forms/rustc-1.99.1/rs/...
```

The round trip fails on a file that differs and that the known differences do not list, and on a
listed one that no longer differs; its report is `.tmp/repr-rust/cand/rustc-1.99.1.report.edn`,
the conversion's `.tmp/repr-rust/forms/rustc-1.99.1.report.edn`, with its counts in the log
(`warning :macro-args-as-forms` ...). The trees of the files found equal are removed after the
comparison; the dumps (146 MB) stay until the next run. `REPR_RUST_DEBUG=1` makes the converter
print each macro call whose arguments parsed but did not print back.

### Choices for the user (RR-P1-RR-P3; built as recommended)

- **RR-P1 Plain comments in mode `:lines`** (§9.3). (a) With RR3: the helper's `rustc_lexer` tokens
  and comments, which `:full` needs anyway, give the converter the comments' positions, and the
  writer puts them as `;` comments at their lines; (b) now, a comment scanner in the converter
  over the Rust file (strings, raw strings and nested block comments to skip). *Recommended: (a)*;
  built: no comments in RR1's forms.
- **RR-P2 Mode `:lines`' metadata into the spec.** (a) Adopt the keys and defaults as built
  (above, "Lines") into §9.1 proper: `:rs/brace`, `:rs/close` on the delimiters before a missing
  return type's or `where` clause's empty span, `:rs/paren`, `:rs/breaks`, a binary operator's
  default line (its right operand's), heads' lines, the flat writing of list heads; (b) keep §9.1's
  text and these notes apart. *Recommended: (a)*, as HR3 did for Haskell forms.
- **RR-P3 Named format arguments.** (a) §7.1 amended: arguments are forms whenever the printer's
  tokens are the call's, `name = value` included (an assignment expression, the same tokens); (b)
  the spec's text: `name = value` makes the call `(:tt ...)`. *Recommended: (a)*, as built.
