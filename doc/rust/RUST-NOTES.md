# repr/rust: notes as built

The build of repr/rust's milestones ([SPEC.md](SPEC.md) §12.1), what each part does and where it
departs from or adds to the spec, with the reasons. RR1 was built on 2026-10-17 (branch
`repr-rust-rr1`, from main at `80530da7`), on the host's rustc 1.99.1 (Alpine's `rust-1.99.1-r0`,
`rustc 1.99.1 (58cf8f9c3 2026-10-13)`, `librustc_driver-61690d3e4c729118.so`), `rustc-dev-1.99.1-r0`
and `rust-src-1.99.1-r0` fetched as apks, and stage 0 of that commit; RR2 on the same day (branch
`repr-rust-rr2`, from main at `10c7b293`), on the same rustc, its `rustdoc` and the host's
sysroot; RR3 on 2026-10-18 (branch `repr-rust-rr3`, from main at `80294e9a`), on the same rustc
and the stage 0 of that commit; RR4 on 2026-10-18 (branch `repr-rust-rr4`, from main at
`96d7bafe`), on the same rustc, the host's `cargo` and the stage 0 of that commit; RR5 on
2026-10-18 (branch `repr-rust-rr5`, from main at `f6ca94b2`), on the same rustc and `cargo`,
`rustc-1.99.1-src.tar.xz` and the stage 0 of that commit; RR6 on 2026-10-19 (branch
`repr-rust-rr6`, from main at `a1a50c4c`), on the same rustc, `rustc-dev`, `rust-src` and tarball,
the host's `objcopy` (binutils) and the stage 0 of that commit.

## Decided (2026-10-17, the user): RR-P1-RR-P3

RR-P1-RR-P3 as recommended: RR-P1 plain comments in mode `:lines` at RR3, from the helper's lexer;
RR-P2 the `:lines` metadata keys and defaults as built adopted into SPEC §9.1 proper; RR-P3 SPEC
§7.1 amended: named format arguments stay forms when the printed tokens match.

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

- Built by `bin/repr-rust helper` (and by every verb that needs it, so that a fresh checkout's
  `bin/repr-rust roundtrip` sets up `.tmp/repr-rust/` itself): the apks of
  `tools/rsdump/apks.sha256` fetched once into `.tmp/repr-rust/apks/` (taken from
  `REPR_RUST_APKS`, a local cache directory, when it holds them with the pinned hashes; else `apk
  fetch NAME`, by name, since apk cannot select `NAME=VERSION` alone: the repositories' build must
  be the pinned one, else the script refuses; nothing installed), checked by their SHA-256 and
  each extracted into its own directory (checked 2026-10-17 in a fresh worktree with an empty
  `.tmp/`: fetched in 8 s, the round trip and the test passed); a sysroot of links (the host's
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

### Choices for the user (RR-P1-RR-P3; decided as recommended, below)

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

## RR2: the interface level

### What was built

| part | file | what |
|---|---|---|
| comparison | `arbace/repr/rust.ae` (`compare-iface`, `strip-spans`, `strip-debug-spans`) | rustdoc's JSON of the originals against the printed files', spans removed (SPEC §11.3) |
| command | `bin/repr-rust` (`roundtrip --iface`: `iface_plan`, `rustdoc_json`, `iface_level`), `make repr-rust-iface` | the interface level after the tree level |
| corpus | `test/rust/corpus-rustc-1.99.1.edn` (each crate's `:iface`; `test/rust/corpus_make.ae` writes it) | how each crate is documented |
| known differences | `test/rust/roundtrip-rustc-1.99.1-iface-known.edn` | none (`{}`), keyed by crate |
| cases | `bin/lib/repr-rust/test` case 5, `test/rust/sample/attrs.rs` | the normalization on two small crates, and a changed doc comment found |

### Results

`bin/repr-rust roundtrip --iface` (also `make repr-rust-iface`), after the tree level's 1,074 of
1,074:

| crate | items | span fields removed | spans in attributes' texts removed | JSON without spans | equal |
|---|---|---|---|---|---|
| `core` | 61,777 | 61,777 | 48,857 | 58.9 MB | yes |
| `alloc` | 5,119 | 5,119 | 682 | 4.9 MB | yes |
| `std` | 10,934 | 10,934 | 1,216 | 10.2 MB | yes |
| `proc_macro` | 691 | 691 | 1 | 0.6 MB | yes |
| `test` | 850 | 850 | 1 | 0.6 MB | yes |
| total | 79,371 | 79,371 | 50,757 | 75.1 MB | 5 of 5 |

No known differences. On this host (64 cores) the ten rustdoc runs (five crates, two sides, all at
once; `core` the longest, about 20 s) take about 25 s, the comparison about 20 s (the tools' start
and 150 MB of JSON read twice); `bin/repr-rust roundtrip --iface` about 80 s in all, `bin/repr-rust
test` 10 s. The JSON of the files found equal is removed after the comparison (the scratch keeps
the mirror of links and the reports, 4.5 MB).

### How each crate is documented (SPEC §11.3)

From `rust-src`'s `library/` directory (the originals) or its mirror (the printed side), with the
paths relative to it as rustc is given them:

```sh
RUSTC_BOOTSTRAP=1 [STD_ENV_ARCH=x86_64] rustdoc --edition 2024 --crate-name C --crate-type lib \
  -Zforce-unstable-if-unmarked -Zunstable-options --output-format json -o OUT \
  --extern D=$(rustc --print sysroot)/lib/rustlib/x86_64-alpine-linux-musl/lib/libD-HASH.rmeta... \
  --cfg ... C/src/lib.rs
```

- **Dependencies**: the direct dependencies of the crate's `Cargo.toml` for this target, each the
  host sysroot's `.rmeta` (exactly one per name, else the run fails): rustdoc does not find them
  alone (only `std` and `core` are in the extern prelude, and the library's crates use `alloc`,
  `cfg_if`, `hashbrown`... by name), and the sysroot's `.rlib`s are metadata stubs (rustdoc
  refuses them: "only metadata stub found for `rlib` dependency"). `core` takes none, `alloc`
  `core` and `compiler_builtins`, `std` 13, `proc_macro` `std`, `core` and
  `rustc_literal_escaper`, `test` `getopts`, `std`, `core` and `libc`. The sysroot is the build of
  the very sources of `rust-src` (the same apk version), so the documented crate and its
  dependencies agree.
- **Flags**: `-Zforce-unstable-if-unmarked`, as rust's bootstrap builds and documents the library:
  without it, std is rejected for calling `libc`'s `const fn`s in constants ("`libc::CMSG_SPACE`
  is not yet stable as a const fn"). The `--cfg`s: std's `backtrace_in_libstd` (its `build.rs`
  sets it on every target), `feature="backtrace"` and `feature="panic-unwind"` (bootstrap's std
  features); proc_macro's `feature="rustc-dep-of-std"` (its default). `test`'s `build.rs` sets
  `enable_unstable_features` only when `rustc -vV` says nightly or dev: not for 1.99.1. std's
  `build.rs` also sets `STD_ENV_ARCH` (`env!` in std), here `x86_64`. All in the corpus file's
  `:iface`, written by `test/rust/corpus_make.ae` from a table that cites these sources.
- **The printed side**: `cp -rs` mirrors `library/` as symbolic links into
  `.tmp/repr-rust/iface/CORPUS/src/`, and each corpus file becomes a link to its printed file in
  `.tmp/repr-rust/cand/CORPUS/` (RR1's printing; a file that did not print has no link, so its
  crate fails to document instead of passing on the original). rustc resolves `#[path]`,
  `include!` and `include_str!` against the path it was given, not the link's target, so the
  files outside the corpus (`stdarch`'s `core_arch`, `portable-simd`, `backtrace`, the Markdown
  of `#![doc = include_str!(..)]`) are the originals', as in rustc's own build.
- The JSON is deterministic: two runs over the same sources gave byte-identical files for all five
  crates (measured before the build), so a difference after normalization is the sources'.

### The normalization (SPEC §11.3, RR-P4)

`strip-spans` removes, as text (the JSON is one line per crate, 69 MB for `core`), every member
`"span":null` or `"span":{"filename":"..","begin":[l,c],"end":[l,c]}` that is a key (after `{` or
`,`; inside a JSON string the quotes would be escaped) with the comma after it. The first run with
only that gave `alloc`, `proc_macro` and `test` equal and `core` and `std` different, each at an
attribute's text: rustdoc writes the attributes it has no structure for as `{"other":"#[attr =
CfgTrace([Not(All([NameValue { name: \"doc\", value: None, span: core/src/os/mod.rs:18:5: 18:8
(#0) }, ...` (rustc's `Debug` of the parsed `#[cfg]` predicates, kept on every item that a `cfg`
leaves), whose columns move when the printer lays the file out again (here `18:5` against
`18:1`). `strip-debug-spans` writes each such `PATH:L:C: L:C (#N)` as `_` (50,757 over the
corpus); with it all five crates are equal. Nothing else needed normalizing:

- **Ids**: the JSON's ids are assigned in the order rustdoc meets the items; the printed files
  give the same items in the same order, so the same ids (an id normalization would be needed only
  for a printer that reorders items, which the tree level already forbids).
- `external_crates` names the sysroot's `.rmeta` files by absolute path, the same on both sides;
  `target`, `format_version` (61) and `crate_version` (none) likewise.

The comparison is of the normalized texts; on a difference the report names the crate, the
position and the item (`{"id":N` before it) with its text on both sides. `bin/repr-rust test`'s
case 5 documents the worked example (a binary, 66 items) and `test/rust/sample/attrs.rs` (cfg'd
items indented by two spaces, so the printed file's columns differ: 7 spans in attributes' texts)
and their printed files, equal; and a copy of `shapes.rs` with one doc comment changed, found
different (item 6).

### Checks and how to reproduce

```sh
bin/repr-rust roundtrip --iface      # the tree level, then rustdoc's JSON of both sides compared
bin/repr-rust test                   # case 5: the interface level's normalization and its check
```

The report is `.tmp/repr-rust/iface/rustc-1.99.1.report.edn`; rustdoc's logs are
`.tmp/repr-rust/iface/rustc-1.99.1/{orig,cand}/C/C.log`, its JSON stays there for a crate that
differs. `bin/repr-rust roundtrip --iface && bin/repr-rust test`, the step `repr-rust` of `bin/gate
--full`, took 151 s on the gate's stage 0 with the host busy with other builds (the rustdoc runs
39 s), about 90 s on a quieter host.

### Choices for the user (RR-P4, RR-P5; open, built as recommended)

- **RR-P4 The spans in attributes' texts** (SPEC §11.3). (a) Amend §11.3: the normalization
  removes every `span` member and every span rustc's `Debug` writes in the attributes' texts
  (`PATH:L:C: L:C (#N)` as `_`), as built; (b) keep their start lines (`PATH:L` only), a stronger
  check in mode `:lines`, which keeps lines, but one the `span` members do not get (their lines
  are removed too) and that mode `:full` makes moot; (c) drop the `"other"` attributes altogether,
  which loses the `cfg`s they record. *Recommended: (a)*, the spec's own rule ("every span
  removed") applied to the spans rustdoc prints as text.
- **RR-P5 Private items** (SPEC §11.1). (a) The public items only, rustdoc's default and §11.1's
  "the public items"; (b) also `--document-private-items`, which covers the private modules
  (std's `sys`, most of `core`'s internals): measured equal for all five crates too (104,991
  items, 53,176 spans in attributes' texts removed; its ten runs took 18 s on their own).
  *Recommended: (a)*: the tree level already compares every item of every file, and the
  interface level checks what other crates see; (b) is a flag away if wanted.

## Decided (2026-10-18, the user): RR-P4, RR-P5

As built: spans written inside attribute texts blanked by the interface level's normalization
(RR-P4, SPEC §11.3 amended); public items only (RR-P5).

## RR3: mode `:full`, the text level

SPEC §9.2 (mode `:full`), §9.3 (comments), §9.4 (3, the full layout) and §11.1 (the text level)
define it; SPEC §12.6 records what the build changes in the spec.

### What was built

| part | file | what |
|---|---|---|
| helper | `tools/rsdump/rsdump.rs` | `dump --full`: `(rsdump/tokens [[KIND L C L2 C2]...])`, `rustc_lexer`'s tokens and comments of the file but white space; every dump: `(rsdump/comments [[L C L2 C2]...])`, the plain comments |
| converter | `arbace/repr/rust/full.ae`; `convert.ae`'s `*full*`, `*texts*`, `line-comments` | mode `:full`'s forms: mode `:lines`' forms with every form's span and the positions of the printer's tokens paired with the source's; the plain comments of mode `:lines` |
| writer | `arbace/repr/rust/write.ae` (`with-comments`) | the plain comments as `;` comments at their lines (RR-P1) |
| printer | `arbace/repr/rust/print.ae` (`print-pieces`, `print-full`) | the full layout: the printer's tokens at the forms' positions |
| command | `bin/repr-rust roundtrip --text`, `--text-only`, `dump --full`, `read --full`, `print --layout full`; `arbace.repr.rust compare-text`; `make repr-rust-text` | the text level, then the tree level of the printed files |
| known differences | `test/rust/roundtrip-rustc-1.99.1-text-known.edn` | none (`{}`) |
| cases | `bin/lib/repr-rust/test` case 6, `test/rust/sample/space.rs` (`-text` in `.gitattributes`) | the samples byte for byte, a byte order mark and CRLF, a name changed in the forms, `;` comments in mode `:lines` |
| gate | `bin/gate --full`'s `repr-rust` step: `roundtrip --iface --text && test` | |

### The helper

`rustc_lexer::tokenize` (with `FrontmatterAllowed::Yes`, after `strip_shebang`) over the file's
text as rustc's source map holds it (a UTF-8 byte order mark removed, `\r\n` made `\n`), white
space left out, each token `[KIND L C L2 C2]` with rustc's columns (characters from 1, the end
exclusive): `:c` a plain comment (line or block), `:doc` a doc comment, `:id` an identifier
(raw or not), `:lt` a lifetime, `:lit` a literal, `:p` one punctuation character (the lexer does
not glue: `::` is two), `:shebang`, `:fm` a front matter, `:x` anything else. Positions rather
than SPEC §10.3's byte ranges: the converter works on the text's characters. Without `--full`
the dump carries the plain comments alone, `[L C L2 C2]` each (the full dumps are 181 MB, the
others 146 MB).

### The converter (SPEC §9.2 as built)

A dump of `dump --full` is converted as in mode `:lines` with `*full*` bound: `at` puts on each
list, vector and symbol it gives a line the node's span too. Then (`arbace.repr.rust.full`):

1. The printer prints the forms with its tokens collected (`print-pieces`: the texts it emits, the
   same code as mode `:lines`' layout), and they are paired in order with the file's text
   (`align`): each printed token is the text at the next lexer token's start (a printed token may
   cover several of the lexer's, `::`, `..=`, a `(:tt ...)` text, or part of one, `x.0.1`'s `0`
   in the lexer's float `0.1`); plain comments before it are recorded. Where it is not, the
   alternatives are weighed by how many printed tokens then match in a row (up to 4): the
   source's punctuation before it (1 to 3 tokens, no delimiter) recorded as `:rs/punct`, or a
   printed punctuation token (no delimiter) the source lacks recorded as `:rs/drop`; else the
   source's tokens without their white space (`> >` for a printed `>>`), or one identifier,
   lifetime or literal spelled otherwise, as `:rs/text`; else the file fails to convert, with the
   position, the token and the printed tokens before it (as Haskell forms' HR11). The first corpus
   run failed 3 files, where a `}` was taken for skipped punctuation: delimiters are never
   skipped or dropped since.
2. The positions are given to the forms by span (`distribute`: the innermost span that holds the
   position, by a sweep over the spans sorted by start, the longer first, and the positions):
   `:rs/pos {:span :toks}`, `:rs/text`, `:rs/punct`, `:rs/drop` on the owner, the spans of every
   other spanned form as `:rs/pos {:span}`; positions no span holds go to `rs/file`'s metadata
   (none in std).
3. On `rs/file`: `:positions :full`, `:rs/comments [[l1 c1 l2 c2 "text"]...]` (the plain comments
   outside the text forms hold), `:rs/space [[l c "ws"]...]` (a gap between two tokens, or before
   the first, that is not `\n*` then spaces), `:rs/tail` (the text after the last token when it
   is not one newline), `:rs/crlf`, `:rs/bom`.

The forms of mode `:lines` are unchanged as data and lines (all 1,074 files compared with RR1's,
read back; the printed files byte-identical to RR1's), with the plain comments now in their text;
the forms of mode `:full` are, as data, mode `:lines`' but for `rs/file`.

### The printer's full layout (SPEC §9.4, 3)

`print-full`: the printer's tokens (`print-pieces` of the read forms), the positions of every
form's `:toks` and `:rs/drop` sorted (a dropped token before a token at its position), each
`:toks` position taking the next printed token (its `:rs/text` in its place), each `:rs/drop` one
written nowhere, the punctuation and the comments added at their positions; between two written
texts the recorded white space, else the gap rule; then `:rs/tail` (else a newline), `\r\n` for
`:rs/crlf`, the mark for `:rs/bom`. A count of positions other than the printer's tokens, or a
token before the end of the one before it, is an error. The printer resolves nothing: case 6 of
`bin/repr-rust test` prints the worked example's forms with `largest` renamed `highest` and gets
the original with the name changed.

### Results

`bin/repr-rust roundtrip --text` (also `make repr-rust-text`, `--text-only` for the level alone)
over RR1's corpus:

| crate | files | byte-identical | trees and lines of the printed files equal |
|---|---|---|---|
| `core` | 288 | 288 | 288 |
| `alloc` | 93 | 93 | 93 |
| `std` | 646 | 646 | 646 |
| `proc_macro` | 17 | 17 | 17 |
| `test` | 30 | 30 | 30 |
| total | 1,074 | 1,074 (14,276,761 bytes) | 1,074 (1,660,638 spans) |

No known differences. What the forms hold for it: 1,651,504 token positions, 20,248 plain comments
(of the lexer's 22,182; the other 1,934 are inside `(:tt ...)` texts), 3,495 punctuation tokens
(all commas: trailing commas the printer does not print), 975 dropped tokens (the commas the
printer prints after a block-bodied arm and a last field where the source has none), no spelling
(std writes no raw identifier that is no keyword, nor `> >`), no white space other than the gap
rule's (std has no tab, no trailing blank outside comments and literals, no CR, no byte order
mark) and no tail (every file ends in one newline). The forms of mode `:full` are 59 MB (mode
`:lines`' 16 MB): 3.8 times. `test/rust/sample/space.rs` adds what the corpus lacks: tabs,
trailing blanks, a form feed, raw identifiers, an identifier not in NFC (spellings), a leading
`|`, trailing commas, nested and non-BMP comments, no final newline; the test also prints a copy
with a byte order mark and CRLF line ends.

Mode `:lines` writes 20,390 lines of `;` comments over the corpus. Times on this host (64 cores,
shared with other builds): the text level about 55 s (the dumps 2 s, the conversion 28 s, the
printing 7 s, the trees of the printed files 7 s, the rest the tools' start); the gate's step
(`roundtrip --iface --text && test`) 145 s (RR2's about 90-150 s), `bin/repr-rust test` 24 s.

### Checks and how to reproduce

```sh
bin/repr-rust roundtrip --text-only   # dump --full, read --full, print --layout full, cmp, trees
bin/repr-rust roundtrip --iface --text   # the gate's: all three levels
bin/repr-rust test                    # case 6: mode :full's cases
bin/repr-rust read --full --corpus    # the forms only: .tmp/repr-rust/forms/rustc-1.99.1-full/
```

Reports: `.tmp/repr-rust/cand/rustc-1.99.1-full.text.edn` (the text level) and `.report.edn`
(the tree level of the printed files), the conversion's `.tmp/repr-rust/forms/rustc-1.99.1-full.report.edn`
with its counts in the log (`warning :full-tokens` ...).

### Choices for the user (RR-P6-RR-P8; open, built as recommended)

- **RR-P6 Mode `:full`'s positions.** (a) As built, Haskell forms' HR9/HR10: the lexer's tokens
  paired in order with the printer's, `:rs/pos {:span :toks}` unnamed on the innermost spanned
  form, `:rs/text`, `:rs/punct`, `:rs/drop`, the white space by the gap rule; (b) SPEC §9.2's
  named keys (`:fn`, `:comma`, `:lbrace`...) per node, `:rs/apos`, `:rs/paren`, a table per node
  kept with the rustc version (S22). *Recommended: (a)*: every token by construction, no table to
  keep, one mechanism for literals, keywords and parentheses; (b) names what (a) pairs by order.
- **RR-P7 Commas the printer decides otherwise.** (a) As built: the printer keeps mode `:lines`'
  commas, and the source's differences are data, `:rs/punct` (3,495 trailing commas) and
  `:rs/drop` (975 printed commas the source lacks); (b) commas always data in mode `:full` (as
  Haskell forms' punctuation), the printer printing none in the full layout: 1.6% more data, and
  a form edited by hand then has no commas. *Recommended: (a).*
- **RR-P8 The text of `;` comments in mode `:lines`.** (a) As built, Go forms': a line comment's
  text after its `//` (`// x` is `; x`), a block comment's lines trimmed after a space; (b) the
  Rust text verbatim (`; // x`), unambiguous about the comment's kind. *Recommended: (a)*, as
  repr/go.

## Decided (2026-10-18, the user): RR-P6-RR-P8

RR-P6-RR-P8 as built.

## RR4: the rest of `library/`, the compiler, a pinned crate set; the pretty layout

SPEC §11.4 (the corpus), §12.1 (RR4), §12.2 (editions) and §9.4 (2, the pretty layout) define it;
SPEC §12.7 records what the build changes in the spec. Built on 2026-10-18 (branch
`repr-rust-rr4`, from main at `96d7bafe`), on the same rustc, `rustc-dev` and `rust-src` as
RR1-RR3, the host's `cargo` 1.99.1 (the crate set's interface level) and the stage 0 of that
commit.

### What was built

| part | file | what |
|---|---|---|
| corpora | `test/rust/corpus-rustc-1.99.1-library.edn`, `corpus-rustc-1.99.1-compiler.edn`, `corpus-crates-2026-10-18.edn` (made by `test/rust/corpus_make.ae library`, `compiler`, `crates`) | the rest of `library/` (74 crates, 1,902 files), the compiler (82 crates, 2,167 files), the crate set (402 crates, 8,254 files); each crate's edition, roots, `#[path]`, `include!` and text files, namespace root, interface configuration |
| pins | `test/rust/crates-2026-10-18.sha256`, `test/rust/crates-2026-10-18.lock` | the crate set's 402 `.crate` files; the interface level's Cargo workspace locked (459 packages, their checksums) |
| helper | `tools/rsdump/rsdump.rs` | macro arguments whose parse leaves a delayed bug or raises a fatal error: none |
| converter | `arbace/repr/rust/convert.ae`, `full.ae`, `syntax.ae` | namespaces, raw lifetimes, special struct heads, seven line cases, the lexer's floats in the macro check, a dropped comma before skipped punctuation in mode `:full` |
| printer | `arbace/repr/rust/print.ae` (`print-pretty`, `break-lines`, `brk!`) | the pretty layout; an arm whose body is an `if`; a `const` parameter's default `false` |
| command | `bin/repr-rust` (`--pretty`, `--pretty-only`, several corpora and `all`, `crates_fetch`, `crates_docs`, `edition_plan`, `prune_dumps`), `arbace.repr.rust` (`print --layout pretty`, `compare --trees-only`), `make repr-rust-pretty`, `make repr-rust-all` | |
| known differences | `test/rust/roundtrip-crates-2026-10-18-known.edn` (and `-pretty-`, `-text-`, `-iface-known.edn`); none for the library and the compiler | one file the parser rejects alone; `nom`'s interface (attribute spacing) |
| cases | `bin/lib/repr-rust/test` cases 7 and 8, `test/rust/sample/edition2015.rs` | edition 2015, the pretty layout |
| gate | `bin/gate --full`'s `repr-rust` step: `roundtrip --iface --text --pretty ${REPR_RUST_CORPORA:-} && test` | |

### Results

`bin/repr-rust roundtrip --iface --text --pretty all` (also `make repr-rust-all`), every level
over the four corpora, in one run of 52 minutes on this host (64 cores, shared with other builds):

| corpus | files | tree level (trees and lines) | pretty layout (trees) | text level (byte for byte) | interface level (crates) |
|---|---|---|---|---|---|
| std's five crates (RR1-RR3) | 1,074 | 1,074 (1,660,638 spans) | 1,074 | 1,074 (14.3 MB) | 5 of 5 (79,371 items) |
| the rest of `library/` | 1,902 | 1,902 (6,448,553) | 1,902 | 1,902 (41.0 MB) | 46 of 46 (142,204 items) |
| the compiler | 2,167 | 2,167 (4,013,953) | 2,167 | 2,167 (33.9 MB) | 66 of 66 (129,433 items) |
| the crate set | 8,254 | 8,253 (17,320,391) | 8,253 | 8,253 (117.9 MB) | 394 of 395 (286,161 items) |
| total | 13,397 | 13,396 | 13,396 | 13,396 | 511 of 512 |

Two known differences, each with its cause in its known file: `erased-serde`'s
`src/features_check/error.rs`, a module whose text is one string literal, which rustc's parser
rejects alone (rust-lisp D20; at the tree, pretty and text levels), and `nom`'s interface (token
spacing, N3: rustdoc writes the attribute `#[allow(deprecated,missing_docs)]`, which the derived
impls copy, with its tokens' spacing; mode `:lines` prints its own). The mode `:full` printed
files' trees and lines are equal too (13,396). The crates no level documents are listed with the
reason in the corpus files: of the library's 74 crates 46 have a library rustdoc documents against
the sysroot, of the compiler's 82 66 against rustc-dev, of the crate set's 402 395 through Cargo.

Times by corpus (seconds; dumps, conversion, printing, rustc's trees): the library's tree level
12, 113, 15, 38, its text level's conversion 278 (stdarch's generated files); the compiler's 3, 30,
7, 6 and 78; the crate set's 88, 122, 26, 182 and 309, its interface level's `cargo doc` and
rustdoc runs 74 (the dependencies built once into `.tmp/repr-rust/cargo/target`, kept for the next run). The helper
runs once per edition over all its files (`edition_plan`), not once per crate. The dumps are
removed after their conversion but the failing files' (`prune_dumps`: the crate set's are 1.5 GB,
1.9 GB with `--full`). Std's levels alone, the gate's step: about 3 minutes (RR3's 145 s and the
pretty layout's half minute).

`bin/repr-rust test` adds case 7 (edition 2015's sample, trees and lines, and mode `:full` byte for
byte; edition 2018 rejects it) and case 8 (the five samples' forms in the pretty layout: their
trees).

The forms of the new corpora (mode `:lines`): 105,591 and 105,232 attributes with meta forms in
the library and the crate set, 38,664 and 83,072 macro calls with their arguments as forms,
6,668 and 78,447 as `(:tt ...)`.

### The corpora

- **The rest of `library/`** (`rustc-1.99.1-library`): `rust-src`'s `usr/src/rust/library/` but the
  first corpus's 1,074 files: every Cargo package (a `Cargo.toml` with a `[package]`) a crate,
  `stdarch`'s, `portable-simd`'s, `compiler-builtins`'s, `vendor/`'s 30 crates, `backtrace`,
  `alloctests` and `coretests`, std's tests and benches...; `rtstartup`'s two files, in no package
  (rust's bootstrap compiles them with rustc itself), a crate of library/'s edition 2024. Editions
  2015 (3 crates), 2018 (7), 2021 (19), 2024 (the rest).
- **The compiler** (`rustc-1.99.1-compiler`): every package of `usr/src/rustc/compiler`, paths
  relative to `usr/src/rustc` as rust's build gives them; `usr/src/rustc/library/proc_macro`
  (`rustc_proc_macro`'s `[lib] path`) is the first corpus's `proc_macro` (identical) and left out.
  The codegen backends' build systems are packages of their own, both named `y`.
- **The crate set** (`crates-2026-10-18`): the host's Cargo cache served the survey's E9 (518
  crates); the corpus pins the newest version of each name, 402 crates (E9's other 116 are older
  versions of the same crates: one namespace per crate, §2.1), 31 of edition 2015, 102 of 2018,
  164 of 2021, 105 of 2024. `test/rust/crates-2026-10-18.sha256` holds each `.crate`'s SHA-256,
  all 402 checked equal to crates.io's index checksums (the cache's index); `bin/repr-rust corpus
  fetch crates-2026-10-18` copies them from `REPR_RUST_CRATES`, else the host's Cargo cache, else
  crates.io, checks them and extracts each into `.tmp/repr-rust/crates/src/NAME-VERSION/`
  (checked in this worktree: 15 s, 53 MB of `.crate` files, 260 MB extracted). Nothing of the
  cache's state is read after that.
- `test/rust/corpus_make.ae` makes all four (`std`, `library`, `compiler`, `crates`; `std`'s output
  is RR1's corpus file byte for byte): a minimal reader of Cargo manifests (sections, keys, strings,
  inline tables, arrays over several lines, `edition.workspace`), the target's `cfg(...)` for
  target-specific dependencies (x86_64-alpine-linux-musl), each crate's roots (`src/lib.rs`,
  `src/main.rs`, `src/bin/`, `build.rs`, `tests/`, `benches/`, `examples/`, every target's `path`),
  `#[path]`, `include!` and text files (`include_str!`/`include_bytes!` of a `.rs` by the
  library's own files, its tests left out), `:ns-crate` for a package nested in another's
  directory, and each crate's `:iface` or the reason it has none (`;; not documented: ...`).

### Namespaces (SPEC §2.1; RR-P9)

The rule as written mapped 11 pairs of files to one namespace over the new corpora (none in std):
`stdarch`'s `core_arch` and `portable-simd`'s `core_simd` have a `src/mod.rs` (the module `core`
includes by `#[path]`) beside their `src/lib.rs`; `object`'s `build.rs` and `src/build/mod.rs` (in
`vendor/` and in the crate set); `pulldown-cmark` and `minifier` have `src/lib.rs` and
`src/main.rs`; `gix-credentials` has `src/program/main.rs` beside `src/program/mod.rs`; the two
codegen backends' build systems are both crates `y`. As built: `lib` and `main` are dropped only as
`src/`'s own files, a final `mod` only below `src/`; `src/main.rs` keeps `main` when the package has
a `src/lib.rs`; `build.rs` is `build_script_build`; a package nested in another package's directory
has its namespaces under the enclosing crate's (`rs.rustc_codegen_cranelift.y.prepare`). Std's
namespaces are unchanged (checked: no std file is affected).

### What the new corpora needed

- **The helper**: `vendor/memchr`'s `x86_64/memchr.rs` crashed it (a delayed bug left in the
  silent context of a macro call's argument parse panics when the context drops), and
  `serde_derive`'s `de.rs` and `de/struct_.rs` (a fatal error raised by such a parse): both now
  leave the call without parsed arguments, `(:tt ...)`.
- **Editions 2015-2021**: nothing in the forms; a struct literal named `gen` or `async` (names
  before 2024 and 2018) is `((:path gen) (:braces ...))`, as a call is; a raw lifetime `'r#mod`
  (`yoke-derive`'s example) is `(:lifetime "r#mod")`: rustc's `Lifetime` keeps the name without
  its rawness, and a name rustc reserves in the file's edition can be written only raw
  (`Ident::is_reserved_lifetime`). `test/rust/sample/edition2015.rs` (anonymous parameters, bare
  trait objects, `dyn`, `async`, `await`, `try`, `gen` as names, `...` ranges, 2015 paths), which
  edition 2018 rejects, round-trips in both modes (`bin/repr-rust test` case 7).
- **Lines** (mode `:lines`, all first found at RR4): a closure's `|` and a parenthesized
  argument list's `)` before the empty span of a missing return type (`:rs/close`), an empty
  module's `}` (`:rs/close` on `(:braces)`: its inner span begins there), a file of no item
  (`linux-raw-sys`'s 38 generated files of one comment: `:rs/close` on `rs/file`, the line of its
  end), a `const` item's missing `where` (its empty span follows the type, so the type carries the
  mark), a lifetime ending a bound list before such a span (its own line), `.await` (the head's
  line, its `.` with it), a parameter with attributes (its pattern at its own span, not the
  parameter's, which begins at the attributes), QSelf's trait path (its `path_span`), an implicit
  parenthesis whose `(` is on an earlier line than its expression (the line kept, and a binary
  operator before it stands there).
- **Macro arguments**: `syn`'s tests and `zerovec` write `x.0 .0` in macro arguments; the forms
  print `x.0.0`, which rustc's lexer makes `x . 0.0`: the check now merges an integer, `.` and an
  integer printed together into the lexer's float, so such calls are `(:tt ...)`.
- **The printer**: an arm whose body is an `if` (`'"' => if let ... {}`, `shlex`) was printed as a
  guard (a guard is always followed by the body or `:_`); a `const` parameter's default `false`
  was dropped (`object`); `-9223372036854775808` (`stdarch`'s tests) overflowed the negation, and
  its form is now the long it reads back as.
- **Mode `:full`**: two compiler files close a block arm with no comma before a next arm with a
  leading `|`: the aligner now weighs a printed comma dropped together with source punctuation
  skipped.

### The pretty layout (SPEC §9.4, 2; RR-P11)

`print-pretty` strips every form's metadata, then prints with `*pretty*` bound: `brk!` begins a
line before every item, statement, arm, field, variant, attribute and `where` predicate (indented
one level) and before a `}` that closes any of them; the rest is mode `:lines`' printer with no
lines. `break-lines` then breaks every line over 100 columns but those a token of several lines
touches (a `(:tt ...)` text, a block doc comment): its first delimited group (`(` `[` `{`, matched
on the line, or open to its end) that reaches past column 100, else its widest, is broken
consistently, the open delimiter ending the line, each element (split at the group's commas) on a
line of its own one level deeper, the close delimiter beginning the next line, each new line
broken again. Comments are not printed (forms without positions have none but doc comments, which
are attributes). It is no copy of rustc's `pp.rs`: §9.4 names `-Zunpretty=normal` as its model;
the judge is the tree level (`roundtrip --pretty`: the forms of mode `:lines`, positions stripped,
printed into `.tmp/repr-rust/cand/CORPUS-pretty/`, rustc's trees compared without the lines).

### The interface level of the new corpora (SPEC §11.3; RR-P12)

- **Library**: `core` and `std` documented again exactly as RR2 does, but in this corpus's mirror,
  where the files `#[path]` reaches outside them are printed: `stdarch`'s `core_arch` (core's
  `arch`), `portable-simd`'s `core_simd` (core's `simd`) and `std_float`, `backtrace` (std's
  `backtrace_rs`); then every other library crate against the host's sysroot: its dependencies but
  the optional ones (a renamed one, `core = rustc-std-workspace-core`, by its file), no features.
  Not documented (in the corpus file): crates with no library, proc macros (their dependencies
  are host builds the sysroot lacks), crates with a dependency the sysroot lacks (rand_core,
  wit-bindgen, ...), `windows-sys` (a `compile_error!` placeholder).
- **Compiler**: each crate against `rustc-dev`'s builds of its dependencies, the build of these
  very sources (`:libdir "rustc-dev"`: each extern by its file, `-L dependency=` rustc-dev's
  directory for the rest; of two builds of one crate, `rustc_hash`, `object`, `libloading`,
  `twox_hash`, the one the crate's own metadata names first), its default features (`nightly`),
  `-Zforce-unstable-if-unmarked`, and the environment rust's build sets (`CFG_RELEASE`,
  `CFG_VERSION`, ... from `rustc -vV`; macros of other crates expand them in every crate). rustdoc's
  JSON of a compiler crate takes about a second. Not documented: the crates rustc-dev lacks
  (`rustc_main`, `rustc_driver`'s own, the codegen backends, `rustc_public`, `rustc_windows_rc`, the
  build systems), the proc macros (`proc-macro2`, `quote`, `syn` are host builds rustc-dev
  lacks), `rustc_driver_impl` (needs `rustc_public`), `rustc_expand` (rustdoc's synthesis of
  auto-trait impls overflows, E0275).
- **Crate set**: rustdoc needs each crate's dependencies built, which only Cargo resolves.
  `crates_docs` writes a workspace (`.tmp/repr-rust/cargo/ws`): one package depending on each
  documented crate `=VERSION`, without default features (with them, offline resolution needs
  crates the cache lacks), `gix-hash` with `sha1` and `http` with `std` (which build with none);
  its `Cargo.lock` is `test/rust/crates-2026-10-18.lock` (made once from the host cache's index,
  459 packages with their checksums); the registry replaced by a directory source of the lock's
  `.crate` files (the corpus's extractions as links, the others fetched like the corpus's and
  checked by the lock's SHA-256; a `.cargo-checksum.json` each), so no index and none of the host's
  Cargo state. `cargo doc --no-deps` runs rustdoc through a wrapper (`RUSTDOC`), which runs each
  corpus crate's command twice more, its paths in the directory source made the corpus's
  extraction and the mirror (flags, features, build-script cfgs, `OUT_DIR`, dependencies' metadata:
  cargo's). A library's text files (examples its docs `include_str!`: `clap`, `winnow`,
  `annotate-snippets`, `wit-bindgen`) stay the originals in the mirror, as RR2's Markdown. Not
  documented: the two yanked crates (Cargo resolves no `=VERSION` of `chacha20` 0.10.1 and
  `libssh2-sys` 0.3.2), `openssl-sys` (its build script needs an OpenSSL installation) and the
  four that depend on it.

### Choices for the user (RR-P9-RR-P12; open, built as recommended)

- **RR-P9 Namespaces of the new corpora** (SPEC §2.1). (a) As built: `lib`/`main` dropped only as
  `src/`'s roots, `mod` only below them, `src/main.rs` beside a library keeps `main`, `build.rs` is
  `build_script_build`, a nested package under its enclosing crate; (b) a clash resolved by the
  corpus file naming one of the two files' namespace; (c) the crate set's older versions kept, the
  crate's version in its namespace (`rs.syn_2.`). *Recommended: (a)*: unconditional rules, std
  unchanged, 11 clashes gone.
- **RR-P10 The gate** (`bin/gate --full`'s `repr-rust`). (a) As built: std's five crates at every
  level (`--iface --text --pretty`, about 3 minutes), RR4's corpora opt-in, `REPR_RUST_CORPORA=all`
  (about 40 minutes more), and `make repr-rust-all`, at a milestone or after a change to
  repr/rust; (b) all four corpora always; (c) the new corpora at the tree level only (about 12
  minutes). *Recommended: (a).*
- **RR-P11 The pretty layout** (SPEC §9.4, 2). (a) As built: rustc's printer's line structure
  (one item, statement, arm, field per line), lines broken at 100 columns by consistent breaking
  of a delimited group, judged at the tree level; (b) a port of `rustc_ast_pretty`'s `pp.rs`
  (Oppen's algorithm with its boxes), judged against `-Zunpretty=normal` byte for byte.
  *Recommended: (a)* until a consumer needs rustc's exact text: (b) is a larger printer to keep in
  step with rustc.
- **RR-P12 The crate set's interface configuration** (SPEC §11.3). (a) As built: each crate without
  its default features, through Cargo with a pinned lock and a directory source; (b) default
  features, which needs crates the host cache lacks (fetched from crates.io, then pinned in the
  lock): more of each crate documented. *Recommended: (a)* for a corpus reproducible from the
  pinned files alone; (b) when more coverage is wanted.

## Decided (2026-10-18, the user): RR-P9-RR-P12

RR-P9-RR-P12 as built.

## RR5: the behaviour level; rustc's UI suite

SPEC §11.1 (5, the behaviour level), §11.4 (the corpus) and §12.2 ("Later (RR5): what the UI suite
adds") define it; SPEC §12.8 records what the build changes in the spec. Built on 2026-10-18
(branch `repr-rust-rr5`, from main at `f6ca94b2`), on the same rustc, `rustc-dev` and `rust-src`
as RR1-RR4, the host's `cargo` 1.99.1 (`-Zbuild-std`) and `cc` (the programs' linker), rustc's
source tarball and the stage 0 of that commit.

### What was built

| part | file | what |
|---|---|---|
| corpus | `test/rust/corpus-rustc-1.99.1-ui.edn` (made by `bin/repr-rust corpus make-ui`: `test/rust/corpus_make.ae ui-editions`, the helper's dumps, `ui`) | `tests/ui` of `rustc-1.99.1-src.tar.xz` (SHA-256 `6451598a99e77f07571960f6b21d577399d3a30f33e80cbd2e3a4aae6535d265`, rust-lang's own `.sha256`): 19,942 files at the syntax levels, one crate per edition; `:rejected` (1,162 files rustc's parser rejects alone), `:clash` (4); `:programs`, 3,718 run-pass programs (each revision of a test) with their directives; the 574 revisions left out, by reason, in its header |
| command | `bin/repr-rust` (`tarball_fetch`, `corpus make-ui`, `--run`, `--run-only`, `--std`, `run_level`, `mirror`, `native_helpers`, `build_std`, `std_level`), `bin/lib/repr-rust/run-program` (one program: build, run, compare), `arbace.repr.rust` (`run-plan`, `compare-run`), `make repr-rust-behaviour`, `make repr-rust-all` | |
| converter | `arbace/repr/rust/convert.ae`, `full.ae` | what the UI suite adds (below) |
| printer | `arbace/repr/rust/print.ae` | attributes at fixed places (`attrs-moved`, `leading-attrs`), the rest below |
| helper | `tools/rsdump/rsdump.rs` | a guarded string's prefix before edition 2024 (`#"`: `#`, then lexed again, as rustc's lexer does) |
| known differences | `test/rust/roundtrip-rustc-1.99.1-ui-known.edn` (and `-pretty-`, `-text-`), `-ui-run-lines-known.edn`, `-ui-run-pretty-known.edn` | three files nested deeper than the host evaluates; programs that compare their own columns, lines, token spacing or text |
| cases | `bin/lib/repr-rust/test` case 9, `test/rust/sample/behaviour.rs` | the behaviour level on the sample |

### Results

`bin/repr-rust roundtrip --iface --text --pretty --run --std all` (also `make repr-rust-all`), every
level over the five corpora, then the behaviour level and std built from its printed files: in
two runs on this host (64 cores, shared), 21 and 33 minutes (the first lost the crate set and the
UI suite to a stage 0 rebuilt under it, run again).

| corpus | files | tree level (trees and lines) | pretty layout (trees) | text level (byte for byte) | interface level (crates) |
|---|---|---|---|---|---|
| std's five crates, the rest of `library/`, the compiler, the crate set (RR1-RR4) | 13,397 | 13,396 | 13,396 | 13,396 | 511 of 512 |
| rustc's UI suite (`tests/ui`) | 19,942 | 19,939 (2,438,508 spans) | 19,939 | 19,939 (14.9 MB) | none (no library) |

RR1-RR4's numbers are RR4's, span for span and byte for byte. The behaviour level (3,718
programs; a program not judged when its baseline does not build or times out):

| printed side | the same | of them, once the locations are normalized | nondeterministic | differ (all known) | not judged |
|---|---|---|---|---|---|
| mode `:lines` | 3,685 | 10 (columns) | 27 | 5 | 1 |
| mode `:full` | 3,690 | 0 | 27 | 0 | 1 |
| the pretty layout | 3,666 | 274 (lines and columns) | 26 | 25 | 1 |
| std built from its printed files (mode `:lines`), against std built from the originals | 3,683 | 8 (columns) | 25 | 0 | 10 |

The UI suite's levels take: the tree level 70 s (dumps 11, conversion 33, printing 8, rustc's
trees 18), the pretty layout 25 s, the text level 95 s (its conversion 53), the behaviour level
of the three layouts 127 s, std's two builds 48 s each and the programs against them 67 s (and
the tree levels of std's five crates and the rest of `library/`, about 3 minutes, which `--std`
needs printed). The one program no side judges is `process/nofile-limit.rs` (its static link,
`-Ctarget-feature=+crt-static -Crelocation-model=static`, which the host's linker refuses); std's nine more are built with `-C panic=abort`. The scratch's peak: about
8 GB (`.tmp/repr-rust`; the UI suite's dumps 0.2 GB, its mirrors 35,479 links each, a program's
build removed after its run).

`bin/repr-rust test` adds case 9 (`test/rust/sample/behaviour.rs`: its round trip, mode `:full`
byte for byte, then `run-program` on the original and its printed file: the same once its panic's
column is normalized, and not the same raw).

### The corpus (SPEC §11.4)

`tests/ui` holds 21,108 `.rs` files; the corpus is every one the helper dumps without error with
its edition (compiletest's `//@ edition:` for all revisions, the first of a range `2015..2021`,
else its first revision's, else 2015, compiletest's default): 1,162 are left out as `:rejected`
(they test parse errors) and 4 as `:clash` (`trivial-casts.rs` and `trivial_casts.rs`, two
`stability-cfg2` auxiliaries: one namespace each pair). Each file is a crate root of its own,
`:name ui` and `:dir "tests/ui"` (one crate per edition), its namespace `rs.ui.` and its path
(`crate-of` finds a file's crate by its `:files` first: the editions' crates share a directory).
The tarball is fetched from `static.rust-lang.org` (or `REPR_RUST_SRC`) into
`.tmp/repr-rust/src/`, checked by the SHA-256 the corpus file pins, and its `tests/ui` and
`tests/auxiliary` (compiletest's C helper) extracted (14 s, 67 MB).

The programs (`:programs`): each revision of a test outside `auxiliary/` and outside
compiletest's ignored directories (`compiletest-ignore-dir`: a test's modules) whose mode is
`run-pass`, `run-fail` or `run-crash`, with its edition, compile flags (`--cfg REV` first, `-` made
`_`), run flags, `exec-env` and `rustc-env`. Left out, by the directives (574 revisions): an
auxiliary crate (`aux-build`, `aux-crate`, `aux-bin`, `proc-macro`: 411), another target
(`only-*`, 62; `ignore-*` naming the host, 4), what the host lacks (`needs-sanitizer-*` and the
like, 45), `ignore-test` (10), compile flags no single build gives (`--extern`, `-l`, `-Zsanitizer`,
`-Clink-arg`: 6), incremental and compiletest's own tests, channels (`only-nightly`).

### How a program is run (`bin/lib/repr-rust/run-program`)

Each side builds the program from its source root under the same relative path (`file!()` and
the paths panics print are the same): `RUSTC_BOOTSTRAP=1 rustc --edition E FILE -A unused -A
internal_features -Cdebuginfo=0 --remap-path-prefix ROOT=/src -L native=...` and the program's
flags (compiletest's lint flags; the UI suite's programs use unstable features, which
compiletest's stage compiler allows: the confinement of `RUSTC_BOOTSTRAP` of SPEC §10.4 extended
to these builds, RR-P16); compiletest's `rust_test_helpers.c` is built by `cc` into
`librust_test_helpers.a` once. The executables run as `./t` in one directory (argv[0], the current
directory and the executable's path the same), with an environment of `PATH`, `HOME` and the
program's `exec-env`, stdin empty, at most 60 s; a `--test` harness with `--test-threads=1`.
Compared: stdout, stderr, the exit status, after the normalizations compiletest makes too (the
thread ids panics print, `thread 'main' (1234)`, as `($TID)`; a harness's `finished in 0.01s`).
When they differ, the baseline runs twice more: if its runs differ among themselves, or the side's
second run is the baseline's, the program is nondeterministic (counted, not judged); a program
whose baseline does not build or times out is not judged either.

Mode :lines keeps a file's lines, not its columns, and the pretty layout neither: a side of mode
`:lines` whose outputs differ is compared again with the column of every source location in them
(`FILE.rs:L:C`, as panics print them) written `_`, the pretty layout's with the line and column
(`same-cols`, `same-pos`, counted apart: RR-P13). Mode `:full`'s printed files are the originals
byte for byte, so its behaviour is the original's by construction; it is run all the same.

The printed side is a mirror of the source tree (`.tmp/repr-rust/run/CORPUS/LAYOUT/`, 35,479
links), every corpus file a link to its printed file, so a test's out-of-line modules are printed
too, and the files it `include!`s or reads are the originals.

### std built from its printed files (`--std`)

rust-lisp's `std-from-lisp.sh` method: `rust-src`'s `library/` mirrored as links
(`.tmp/repr-rust/std/library`), std built by Cargo from a scratch crate
(`__CARGO_TESTS_ONLY_SRC_ROOT`, `-Zbuild-std=std,panic_unwind,panic_abort,proc_macro,test`,
`--release`, `-Cembed-bitcode=yes` for the programs built with `-C lto`, offline: the library's
vendored crates through a source replacement, their `.cargo-checksum.json` without file sums on
both sides) into a sysroot (`.tmp/repr-rust/std/orig`: the built crates, the host sysroot's other
files linked), then every file of std's five crates and of the rest of `library/` (2,976 files,
the vendored crates' too: none left original) made a link to its printed file of mode `:lines` and
std built again in the same directory (`.tmp/repr-rust/std/printed`: the paths std's panics
print are the same). The UI suite's programs are built from their originals against each sysroot
and compared as above. Not judged: 9 programs built with `-C panic=abort` (Cargo's build of
`panic_abort` keeps the unwind strategy, which rust's own build changes) and the tests that are
modules.

### What the UI suite needed (SPEC §12.2, "Later (RR5)")

The forms needed nothing new for delegation, contracts, unsafe binders, guard patterns or `&pin`
(the syntax RR1-RR4 already wrote: delegation as `(:rust ...)`, SPEC §4.10). What failed at
first (150 of the 19,942 files at the tree level, then 14 more at the text level) and how:

- **Attributes in fixed places** (SPEC §8.2): an attribute before a call's callee, an index, a
  field, a cast or a binary operator's first operand (`#[a] x * 2`), a paren's content
  (`(#[a] || x)()`), a guard, a constant's or static's value, an array type's length, a variant's
  discriminant was dropped by the converter (forms built with `list`, not `lst`, which splices
  attributes) or not printed. The converter splices them now, and the printer prints an attribute
  form before the element after it wherever an operator's elements stand at fixed places
  (`attrs-moved`), skips them in its shape questions (`strip-attrs`), and prints the attributes of
  a statement's leftmost operand before the statement's line, which rustc's statement span begins
  after (`leading-attrs`).
- **Paths**: generic arguments on a pattern's non-final segment need `::` (`Enum::<N>::Foo`, and
  after a qualified self type); the `<` and `>` of generic arguments on later lines than the path
  (`:rs/open`, `:rs/close` on the application); the `>` of a qualified path (`:rs/close`); `::` of
  a global path in a use group; a macro path with generic arguments (`foo::<T>!()`, which rustc
  parses to reject later: `(:turbofish foo! T)`).
- **Lines**: an item's name on a later line than its keyword (the generics' span); a module of
  inner attributes and no item (its `}` at the `inject_use_span`); the `{` of `gen`, `async`,
  `try`, `unsafe` and labeled blocks on a later line (`:rs/brace`); an empty span at the file's
  first byte, rustc's dummy span (N1: the comparison takes `-` for any line).
- **Items and names**: `reuse impl Trait for T { ... }` (the parser's impl of one glob delegation
  at the impl's span) as `(:rust ...)`; a type the parser's recovery made (`impl Trait for .. {}`)
  likewise; `extern crate r#trait as t` (the tree holds the crate's name without its rawness);
  `const async: u32` and `static async: u32` in edition 2015 (constants named `async`, not
  closures); a struct literal named `union` (`((:path union) (:braces))`, as `gen`); a pattern
  named `await` in `for await in` (edition 2015); `async use { }` (no `use` item); `static ||`
  closures (their movability was read from a symbol, not a list); `fn(self)`, `fn(mut self)` in a
  function pointer type; attributes on a binder's parameters (`for<#[cfg(a)] 'a>`); `.. ..`, two
  range tokens the printer glued into `....`.
- **Text level**: `for<>`, an empty binder, which no tree holds (the span's text); `|x: _|`, an
  inferred type written out (its span after the pattern's); a front matter after blank lines or
  with an infostring with hyphens, an indented `---` inside it; files mixing `\r\n` and `\n` line
  ends (`:rs/crlf` the lines that end in `\r\n`); a bare `\r` in a plain comment, written `\r` in
  mode `:lines`' `;` comment; a guarded string prefix before edition 2024 (the helper).
- **Namespaces** (SPEC §2.1): a path segment's characters a name cannot hold made `_` (
  `impl[t]-foreign[local]-for-t.rs`, 32 files; RR-P17).

### Known differences

At the syntax levels three files, each nested deeper than the converter, evaluated by the tools'
host, can recurse (10,000 nested evaluated calls, Compiler's `MAX_DEPTH`: Go's stack overflow is
fatal): `associated-consts/issue-93775.rs` (964 nested generic applications),
`expr/if/expr-stack-overflow.rs` (5,204 `else if`s), `parser/survive-peano-lesson-queue.rs`
(8 MB of extracted code). At the behaviour level, mode `:lines` (5): the columns
`Location::caller()` gives, compared by `call-chain.rs` (two revisions); `stringify!`'s output,
which shows the tokens' spacing (N3), compared by `stringify.rs` and
`syntax-extension-source-utils.rs`; `lexer-crlf-line-endings-string-literal-doc-comment.rs`, which
reads its own text for its `\r\n` line ends. The pretty layout (25): those and the programs that
compare the lines `Location::caller()`, `line!()` and backtraces give, and two that print their
panics' positions without a file name (`-Zlocation-detail=line,column`). Mode `:full` and std
built from its printed files: none.

### Choices for the user (RR-P13-RR-P17; open, built as recommended)

- **RR-P13 The behaviour level's normalizations.** (a) As built: compiletest's (thread ids, a
  harness's time), and the columns (mode `:lines`), the lines and columns (the pretty layout) of
  the source locations in the outputs, applied only when the raw outputs differ and counted apart;
  (b) none but compiletest's: every program whose panics print a location different in the
  layout a known difference (about 10 for `:lines`, 275 for the pretty layout); (c) the behaviour
  level of mode `:full` only. *Recommended: (a).*
- **RR-P14 The gate** (`bin/gate --full`'s `repr-rust`). (a) As built: unchanged by default (std's
  five crates, about 3 minutes); `REPR_RUST_CORPORA=all` adds the UI suite's syntax levels with
  RR4's corpora, and `REPR_RUST_CORPORA='all --run --std'` the behaviour level, as `make
  repr-rust-all`; (b) the behaviour level always (the UI suite's syntax levels and both runs, about
  15 minutes more than std alone). *Recommended: (a)*, the behaviour level at a milestone or after a change to the
  printer.
- **RR-P15 The UI corpus** (SPEC §11.4). (a) As built: every file of `tests/ui` the parser takes
  at the syntax levels, the run-pass programs without auxiliary crates at the behaviour level;
  (b) the programs with auxiliary crates too (411 revisions), their crates built first from their
  originals and printed files (compiletest's `aux-build` rules: crate types, `no-prefer-dynamic`);
  (c) only the run-pass programs at every level. *Recommended: (a)* now, (b) when the behaviour
  level grows (RR6).
- **RR-P16 `RUSTC_BOOTSTRAP=1` for the programs' builds** (SPEC §10.4). (a) As built: the UI
  suite's programs and the std builds with it, as compiletest builds them with its stage
  compiler; (b) only the programs that use no unstable feature (stable rustc), fewer of them.
  *Recommended: (a).*
- **RR-P17 Namespaces of paths with other characters** (SPEC §2.1). (a) As built: every
  character a name cannot hold made `_` (as `.` and `-` were), a clash left out of the corpus
  (`:clash`); (b) the characters escaped (`munge`'s spelling). *Recommended: (a).*

## Decided (2026-10-19, the user): RR-P13-RR-P17

RR-P13-RR-P17 as built.

## RR6: the lowered and code levels; `include!`'s expression files

SPEC §11.1 (3, the lowered level; 4, the code level), §2.5 and RSS6 (the expression files) define
it; SPEC §12.9 records what the build changes in the spec. Built on 2026-10-19 (branch
`repr-rust-rr6`, from main at `a1a50c4c`), on the same rustc, `rustc-dev`, `rust-src` and
`rustc-1.99.1-src.tar.xz` as RR1-RR5, the host's `objcopy` and the stage 0 of that commit (the
worktree's own).

### What was built

| part | file | what |
|---|---|---|
| helper | `tools/rsdump/rsdump.rs` | `rsdump rustc [--hir FILE] [--mir FILE] -- ARGS...`: rustc as a driver (`rustc_driver::run_compiler`), after analysis the crate's HIR and MIR written, then its code generated as ARGS say; `--expr` for `dump`, `tree`, `lines`: the file parsed as one expression |
| command | `bin/repr-rust` (`--code`, `--code-only`, `code_plan`, `code_level`; `edition_plan`'s `E+expr` lines, `run_helper --expr`; `corpus make-ui`'s expression files), `bin/lib/repr-rust/code-unit` (one unit: build, compare), `arbace.repr.rust` (`compare-code`), `make repr-rust-code`, `make repr-rust-all` | |
| converter, printer | `arbace/repr/rust/convert.ae`, `print.ae` | `(rs/file ... :kind :include-expr)` and `(rs/expr e)`; a negation and its literal on two lines stay `(- lit)` |
| corpora | `test/rust/corpus_make.ae` (`ui-expr-candidates`, `ui ROOT DUMPS EXPRS`, the crates' `include-expr-pat`), `corpus-rustc-1.99.1-ui.edn`, `corpus-crates-2026-10-18.edn` | `:include-expr-files`: the UI suite's 6, the crate set's 1; RR1-RR4's library and compiler corpus files made again byte for byte |
| known differences | `test/rust/roundtrip-rustc-1.99.1-ui-code-lines-known.edn`, `-ui-code-pretty-known.edn`, `-compiler-code-pretty-known.edn` | 5, 70 and 42 units, each level with its cause |
| cases | `bin/lib/repr-rust/test` case 10, `test/rust/sample/lowered.rs`, `lowered_table.rs` | |

### Results

`bin/repr-rust roundtrip --text --pretty --code` over std's five crates, the rest of `library/`,
the compiler and the UI suite (the crate set has no unit: below); each unit built from the
originals and from the printed files of the three layouts. A unit is a crate of the corpus with an
interface configuration (`:iface`: its root, edition, `--cfg` flags, environment and externs, an
rlib) or a program of the UI suite (`:programs`, as the behaviour level builds it, one object).

| corpus | units | judged | layout | HIR the same | MIR the same | objects the same (of them: but the bitcode; once the locations' lines are left out) | known |
|---|---|---|---|---|---|---|---|
| std's five crates | 5 | 5 | `:lines`, pretty | 5 | 5 | 5 (2; 0) | 0 |
| | | | `:full` | 5 | 5 | 5 (0; 0) | 0 |
| the rest of `library/` | 46 | 46 | `:lines`, pretty | 46 | 46 | 46 (2; 0) | 0 |
| | | | `:full` | 46 | 46 | 46 | 0 |
| the compiler | 66 | 65 | `:lines` | 65 | 65 | 65 (19; 0) | 0 |
| | | | `:full` | 65 | 65 | 65 | 0 |
| | | | pretty | 23 | 23 | 23 (1; 0) | 42 (32 differ, 10 do not build) |
| the UI suite's programs | 3,718 | 3,715 | `:lines` | 3,711 | 3,710 | 3,711 (0; 14) | 5 |
| | | | `:full` | 3,715 | 3,715 | 3,715 | 0 |
| | | | pretty | 3,657 | 3,645 | 3,654 | 70 |

Not judged: `rustc_codegen_llvm` (its build needs the native `llvm-wrapper` library, which
rust's bootstrap builds with LLVM's headers), and three programs (`simd/shuffle.rs` both
revisions, `simd/intrinsic/inlining-issue67557-ice.rs`) whose MIR, written for every body,
fails rustc's MIR validation (`-Zunpretty=mir` fails the same way; they build without it). The
times on this host (64 cores, shared with another agent's work): std's five crates 140 s (core
and std the longest, about 25 s per build, four builds each), the rest of `library/` 144 s, the
compiler 374 s (eight crates at a time), the UI suite's programs 100 s; with the syntax levels
the corpora need printed first. The gate's step is unchanged (RR-P21). The syntax levels over
RR1-RR5's corpora are unchanged: std's 1,074 files, the rest of `library/` (1,902) and the
compiler (2,167) equal at the tree, pretty and text levels and std's interface level; the UI suite
19,945 of 19,948 (the six expression files added, all equal, the three known as before), and its
behaviour level as RR5's (one program, `threads-sendsync/yield.rs`, differed once in mode
`:lines` while two runs shared the host and was the same in three runs alone: its threads'
interleaving); the crate set below.

### How a unit is built and compared (`bin/lib/repr-rust/code-unit`)

Each side is built by the helper from its source root under the same relative path, with
`RUSTC_BOOTSTRAP=1`, `-Cdebuginfo=0`, the root remapped to `/src`, the unit's arguments, and
`--emit=link` (a library's rlib) or `--emit=obj` (a program's object). The printed side is a
mirror of the source tree, every corpus file but the text files a link to its printed file, as the
behaviour level's (`.tmp/repr-rust/code/CORPUS/LAYOUT/`). Three judges:

- **HIR** (the lowered level): `rustc_hir_pretty::print_crate`, which `-Zunpretty=hir` calls, given
  no source text: `-Zunpretty=hir` interleaves the root file's comments by their positions, and
  mode `:lines` prints no plain comment (core's HIR differs only there: RR-P18). The text compared
  after the positions rustc writes in it (`#[attr = CfgTrace([... span: FILE:L:C: L:C (#0)])]`,
  the RR2 kind) are normalized as the layout keeps them: mode `:full` none; mode `:lines` each
  kept to its start line (`FILE:L:_: _:_`: the lines where spans end are not kept, a coroutine's
  closing brace in core); the pretty layout to its file.
- **MIR** (the code level): `write_mir_pretty`'s text, which `-Zunpretty=mir` writes, normalized
  as the HIR (closures and impls are named by their positions, `{closure@FILE:L:C: L:C}`,
  `<impl at FILE:L:C: L:C>`). Written by the helper's copy of `write_mir_pretty` for two reasons:
  1.99.1's asks `optimized_mir` of core's `#[rustc_comptime]` functions (`intrinsics::needs_drop`),
  which panics, so the helper takes their `mir_for_ctfe`; and the paths are printed in full, not
  trimmed as rustc's own callbacks do, which rustc's check refuses when no diagnostic follows
  (`proc_macro`, `test`).
- **Objects** (the code level): every member of the rlib (its objects, `lib.rmeta-link`) or the
  object, byte for byte; the metadata (`lib.rmeta`, which holds every span and each file's hash)
  only in mode `:full`. The code of mode `:lines` is built with `-Zlocation-detail=file,line` on
  both sides (`Location` constants without their columns), the pretty layout's with
  `-Zlocation-detail=file`. Two differences that are positions are set aside, counted apart
  (RR-P20): an object that differs only in the LLVM bitcode an rlib embeds (`.llvmbc`, compared
  again with `objcopy -R .llvmbc`), where inline assembly's `!srcloc` holds its span's byte
  positions in the session's source map (std and test; `std_detect` and the library corpus's
  `std`; 19 crates of the compiler: of std's LLVM IR one line differs, an `asm!`'s `!srcloc`); and in mode `:lines` objects
  that differ and are the same once both sides are built again with `-Zlocation-detail=file`: a
  location at a block's closing brace (the debug assertions' pointer checks of boxes freed at the
  end of a scope), whose line mode `:lines` does not keep (the printer puts a `}` on its last
  token's line), 14 programs.

The HIR and MIR are written in `after_analysis`, and code generation goes on in the same run: one
build per side and location detail (the baseline twice when the pretty layout is judged, mode
`:lines`' objects a third time when they differ). rustc is deterministic here: the same sources
built twice give the same bytes, the metadata included (mode `:full`'s rlibs are the originals'
byte for byte).

### The known differences

All are positions that reach values, or text the layout respells:

- mode `:lines` (5 programs of the UI suite): `stringify!` (`stringify.rs`,
  `syntax-extension-source-utils.rs`) and `assert!`'s message (`half-open-range-pats-semantics.rs`)
  show the tokens' spacing (N3); `lexer-crlf-line-endings-string-literal-doc-comment.rs` reads its
  own text; `nested_uneval_unification-2.rs`'s MIR prints an unevaluated constant by its source
  snippet (`{{ N }}`, printed `{ { N } }`). The first four are the behaviour level's known
  differences too.
- the pretty layout (70 programs, 42 crates of the compiler): the test harness's descriptors
  (each `#[test]`'s `start_line`, `start_col`: 38 programs), `line!()` and `column!()` values (15),
  `dbg!`'s `[file:line:col]` (4), unevaluated constants' snippets (9, the printer's line breaks
  inside them), the four of mode `:lines`; in the compiler, `tracing`'s events, which record their
  `line!()` and `file:line` (32 crates), and `rustc_macros`' `derive(Diagnostic)`, which reads
  the columns of a message's string literal over lines and refuses the pretty layout's
  indentation (10 crates do not build).

### The crate set

The crate set's crates are built by Cargo (their build scripts, features and dependencies: RR4's
interface level runs Cargo's rustdoc commands through a wrapper); `code_plan` builds a corpus's
units from its `:iface` roots and externs, so the crate set has none (RR-P21). Its syntax levels
with the expression file (`askama`'s `benches/strings.inc`), in this build: 8,255 files, 8,254
equal at the tree, pretty and text levels (the one known since RR4), 394 of 395 crates at the
interface level (`nom`, known), 26 minutes with std's five crates' levels on the shared host.

### The expression files of `include!` (SPEC RSS6)

`include!` in an expression's place (`ExpandInclude::make_expr`, also a statement's place) parses
the file with `StripTokens::Nothing` and `parse_expr`, and lints a file not ended by the
expression. The helper's `--expr` gives rustc an empty crate root (`Config::input` an empty
string), then parses the file so with the session's parser and dumps the `Expr`; its lexer's
tokens (`--full`) keep a `#!` line and `---` fences as tokens. The converter writes `(rs/file
... :kind :include-expr)`, no `:shebang` or `:frontmatter`, and `(rs/expr e)`; the printer prints
the expression at the form's line. Which files are expression files is the corpus's
(`:include-expr-files`, in `:files`): for the UI suite, the files the helper rejects as items, or
not `.rs`, that an `include!` call of the suite names (its literal, over lines too, or its file
name on the call's line: `include!(concat!(..))`) and the helper parses as one expression, with
the edition of the first test that names it: `frontmatter/auxiliary/expr.rs` (`---\n-\n---\n1`,
seven negations), `macros/auxiliary/macro-comma-support.rs`, `macro-include-items-expr.rs`,
`include-single-expr-helper-1.rs`, `parser/issues/auxiliary/issue-21146-inc.rs` and
`macros/syntax-extension-source-utils-files/includeme.fragment` (1,157 files rejected, 1,162
before); for the crate sets, an `include!("...")` after `=`, `(` or `,`, the named file in the
package (`askama`'s `benches/strings.inc`). `expr.rs` needed one converter change: a negation
whose literal is on a later line is no negative literal (`(- 1)` with the literal's line).

### Checks and how to reproduce

`bin/repr-rust roundtrip --text --pretty --code [CORPUS... | all]` (`--code-only` with the
printed files already there; `make repr-rust-code` for std's five crates), `bin/repr-rust test`
(case 10), `bin/repr-rust corpus make-ui` (the UI corpus with its expression files, byte for byte
as committed). The scratch's peak: about 7 GB (`.tmp/repr-rust`; the UI suite's dumps and
mirrors, the crate set's extraction; a unit's builds removed after their comparison).

### Choices for the user (RR-P18-RR-P22; open, built as recommended)

- **RR-P18 The lowered level's text.** (a) As built: the helper's HIR, `rustc_hir_pretty`'s text
  without the root file's comments, in the run that builds the code; (b) rustc's `-Zunpretty=hir`
  as it is, a separate run per crate and side, its comments removed by a normalization (they are
  interleaved by position, trailing ones inside lines); (c) `-Zunpretty=hir-tree` (HIR's `Debug`,
  every span in it, normalized). *Recommended: (a)*: rustc's own printer, no text normalization
  but positions, one build for three judges.
- **RR-P19 The MIR.** (a) As built: `write_mir_pretty`'s text by the helper (a comptime
  function's body from `mir_for_ctfe`, paths in full); (b) `-Zdump-mir=PreCodegen`, one file per
  function as codegen asks them (no ICE on the three programs; positions in comments, many files);
  (c) objects only. *Recommended: (a)*, the three programs not judged.
- **RR-P20 Objects and positions.** (a) As built: every member but the metadata byte for byte,
  `-Zlocation-detail` as the layout keeps positions, the bitcode's `!srcloc` and mode `:lines`'
  closing braces set aside only when they alone differ, counted apart; (b) both counted as known
  differences, unit by unit (21 crates, 14 programs); (c) mode `:lines` extended to keep a block's
  closing brace's line (`:rs/close` on a body whose `}` stands on a later line than its last
  token: most blocks), its objects then equal at `file,line`. *Recommended: (a)*: (c) changes
  SPEC §9.1's forms for most blocks of every file, for a check the rebuild makes.
- **RR-P21 The corpora and the gate.** (a) As built: `--code` opt-in, over the library corpora's
  crates with an interface configuration and the UI suite's programs; `REPR_RUST_CORPORA='all
  --code'` (about 13 minutes more), `make repr-rust-code` (std's five, about 2.5 minutes); the
  crate set not built; (b) the crate set too, through a `RUSTC_WRAPPER` as its interface level's
  rustdoc wrapper (Cargo builds every crate and its dependencies, the wrapper builds each corpus
  crate again on both sides); (c) std's five crates' code levels in the gate's default step
  (about 2.5 minutes more). *Recommended: (a)*, (b) when a consumer of the crate set's code needs
  it.
- **RR-P22 The expression files.** (a) As built: found by `corpus_make` (the UI suite's rejected
  or non-`.rs` files an `include!` names and the helper parses as an expression; the crate set's
  `include!` after `=`, `(`, `,`), non-`.rs` files included, `(rs/expr e)`; (b) `.rs` files only;
  (c) found by expansion (the helper records the files `include!` loads in an expression's place,
  which needs each crate expanded). *Recommended: (a).*
