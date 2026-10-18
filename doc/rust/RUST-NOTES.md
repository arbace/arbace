# repr/rust: notes as built

The build of repr/rust's milestones ([SPEC.md](SPEC.md) §12.1), what each part does and where it
departs from or adds to the spec, with the reasons. RR1 was built on 2026-10-17 (branch
`repr-rust-rr1`, from main at `80530da7`), on the host's rustc 1.99.1 (Alpine's `rust-1.99.1-r0`,
`rustc 1.99.1 (58cf8f9c3 2026-10-13)`, `librustc_driver-61690d3e4c729118.so`), `rustc-dev-1.99.1-r0`
and `rust-src-1.99.1-r0` fetched as apks, and stage 0 of that commit; RR2 on the same day (branch
`repr-rust-rr2`, from main at `10c7b293`), on the same rustc, its `rustdoc` and the host's
sysroot; RR3 on 2026-10-18 (branch `repr-rust-rr3`, from main at `80294e9a`), on the same rustc
and the stage 0 of that commit.

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
