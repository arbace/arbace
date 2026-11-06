# Rust without rustc: a survey of the front end

Survey of 2026-11-05 (branch `rust-frontend-survey`, from `rust-native`), design only, with small probes. Question: today
rsi (`doc/rust/RUST-EVAL-SURVEY.md`, `doc/rust/EVAL.md`, branch `rust-re5`) runs rustc's `-O0` LLVM IR through C++'s
lowering on ci, so a Rust program needs the host's rustc at build time. What is the smallest route from repr/rust's native
parse trees (`doc/rust/NATIVE-READER.md`: rustc 1.99.1's AST, no rustc at read time) to an IR rsi can run, and what does it
cost? RUST-EVAL-SURVEY §4.3 called a Rust front end in Arbace "hundreds of days, not planned"; this survey measures it piece by
piece, as `doc/haskell/FRONTEND-SURVEY.md` (branch `haskell-frontend-survey`, decided 2026-11-05: a trusting elaborator in
tiers) did for Haskell, and finds a smaller thing than rustc if the front end trusts rustc's acceptance and takes std as data
first.

Sources: the compiler and library of Alpine's `rust-src-1.99.1-r0` (pinned by `tools/rsdump/apks.sha256`, read in place in
`rust-native`'s `.tmp/repr-rust/apks/rust-src/`: `usr/src/rustc/compiler/`, the 2,167 files of
`test/rust/corpus-rustc-1.99.1-compiler.edn`, and `usr/src/rust/library/`); the host's rustc 1.99.1 (`58cf8f9c3`, the pin
`tools/rsdump/RUSTC_VERSION`) for the probes of §2.4; rsi's synthetic corpus and samples (`rust-re5`'s `test/rust/synthetic/`,
`test/rust/eval/`); the host's Cargo registry cache (518 crates) as a sample of ordinary crates. The UI suite's text
(`rustc-1.99.1-src.tar.xz`, `test/rust/corpus-rustc-1.99.1-ui.edn`) is not on this host today (no network for this survey):
its feature census is RF1's first job. Measured with `wc`, `grep` and `find`; scratch in this worktree's `.tmp/src/` and
`.tmp/probe/` (not committed). What is from memory (prior art, sizes of other projects) is marked "(memory, verify)".

Contents: Summary · 1 What the front end must produce · 2 Measurements · 3 The pieces · 4 Judging · 5 Prior art ·
6 Routes and tiers · 7 Milestones RF1-RF14 · 8 Risks · Proposed (RFS1-RFS13).

## Summary

- **The target is MIR, then rsi's LLVM forms.** rsi runs `-O0` IR; that IR is a mechanical function of rustc's
  monomorphized runtime MIR, layouts and ABI. So the front end's pivot is **MIR as forms** (judged against rustc's MIR, text
  for text), and a separate **back half** (monomorphization, layout, ABI, codegen to LLVM forms) feeds rsi unchanged (std's
  IR pinned, RES34's per-key translation, the lowering, ci, cf2g).
- **A program's IR is mostly std's generic code.** A 28-line program (a derived enum, a trait, `HashMap`, `Vec`, an iterator
  chain, `println!`) compiles at `-O0` to 266 functions, of which **9 are the program's own** and the rest instances of
  core's, alloc's, std's and hashbrown's generics (§2.4). So a front end for user programs must monomorphize **std's generic
  MIR**, and the back half carries most of the weight; given rustc's MIR it can be judged **exactly** against rustc's IR
  (`-Zshare-generics=no`), with no front end at all. Hence the order: the back half first.
- **rustc's analysis is 866k lines; the path source to MIR that a trusting front end needs is about 170k of them.**
  Diagnostics and suggestions (about 97k lines in their own files, a third of `rustc_hir_typeck`), lints (25.6k), the borrow
  checker (35.8k), well-formedness, coherence and variance checks, the MIR optimizations (`gvn`, `inline`, ...), incremental
  compilation, the query system and the three code generators are not needed to run programs rustc accepts. **Regions can be
  erased from the start** (codegen erases them; selection never depends on them in an accepted program), which removes
  `rustc_infer`'s region half, `outlives`, `variance` and borrowck at once.
- **The front end is a port of rustc's accepting paths, not a clean design** (RFS1 (b)): the oracle is rustc's own output at
  every stage, and inference, method probing, closure capture, match lowering and drop order are observable choices a clean
  reimplementation would make differently. The native parser is the calibration: rustc_lexer and rustc_parse (34,000 lines
  with their recovery and messages) became 4,862 `.ae` lines in one session day, equal to rustc's trees on every sample.
- **std in two tiers.** Tier U, user programs: std's interface (items, generics, predicates, impls, ADTs, lang items, the
  specialization graph, symbols) and its MIR library (the generic and `#[inline]` items' MIR, as rustc's metadata holds it)
  **dumped once by the helper at the pin as forms**, no rustc at program build time. Tier S: core, alloc and std from source
  by the same front end, which must then take the unstable surface std uses: 253 distinct `#![feature]`s across the three
  crates, 158 lang items, 316 intrinsics, 138 `default fn`s (specialization), 82 const traits and impls, 62 `rustc_*`
  attributes, 67 `macro` 2.0 definitions, 132 `cfg_select!`s, coroutines, SIMD.
- **Costs** (§7): Tier U (RF1-RF11) **about 95 to 150 working days**; Tier S (RF12) 35 to 60 more; proc macros and build
  scripts (RF13) 12 to 20; in all about 145 to 235. The long poles are type checking (RF9), the trait solver (RF8) and MIR
  building with constant evaluation (RF10). The back half alone (RF1-RF3, 25 to 40 days) is useful by itself: Rust's codegen
  on Arbace from rustc's MIR, the MIR forms RE10 wanted, and an exact judge.
- **Judging is cheap because rustc dumps every stage**: the expanded AST (as the native reader's dumps: `native-check`'s
  equality), resolutions, `-Zunpretty=hir`, the type-check results per body (node types, method resolutions, adjustments,
  captures) through the helper, `-Zunpretty=thir-tree`, `-Zdump-mir` after every pass (50 passes at `-O0` for one body),
  the mono items, the IR, and rsi's runs against the judge's as the final word.
- **Recommended** (RFS1-RFS13): the trusting port in tiers (RFS1 (b)); MIR forms as the pivot and LLVM forms as the target
  (RFS2 (a)); the back half first, expansion and resolution beside it (RFS3 (a)); std's interface and MIR as helper data in
  Tier U (RFS4 (a)); regions erased, no borrowck, no exhaustiveness, no lints (RFS5 (a)); the next trait solver ported with
  rust-analyzer as the guide, after RF1 confirms it agrees on the corpus (RFS6 (a)); proc macros as programs on rsi, mrustc's
  shape, in Tier P (RFS8); rustc's own front end on rsi measured as an oracle, not adopted (RFS11 (a)).

## 1. What the front end must produce

rsi's input is the IR of one codegen unit, as rustc writes it with `-C opt-level=0 -C codegen-units=1` (EVAL.md §1), plus the
allocator shim, linked with std's lowered units (EVAL.md §3, §8; RES34: std's fragments translated once per key). That IR is
rustc's codegen of the program's **mono items**: every function instance reachable from `main` (the program's functions, its
closures, its derived and written impls, and every generic of std instantiated at the program's types), drop glue, vtables,
shims (`FnOnce` call-once shims, `Clone` shims of closures and tuples, vtable shims), statics and promoted constants as global
allocations. Each instance's body is rustc's **runtime-optimized MIR** for it, substituted, with layouts (field order, niches,
enum tags) and the Rust ABI (`PassMode`: direct, pair, cast, indirect) deciding the LLVM types.

So there are two halves, joined by MIR:

- **The front half** turns a crate's source into MIR bodies (polymorphic, per item): expansion, resolution, HIR, type
  collection, type checking with the trait solver, THIR, MIR building, the analysis passes that change semantics (drop
  elaboration, promotion of constants, the box and deref passes, `StateTransform` for coroutines), and constant evaluation
  (array lengths, `const` items, statics' initial bytes, enum discriminants, const generic arguments).
- **The back half** turns MIR into LLVM forms: the mono item collector (instance resolution at monomorphic types, with
  specialization), layout, ABI, symbol names (v0 mangling, `-C symbol-mangling-version` default), codegen of statements,
  terminators, intrinsics and unwinding (`invoke` and landing pads, which rsi's pending flag already takes), and globals.

What the front half owes the back half and std (Tier U): the same names and shapes as rustc's metadata (each std item by its
`DefPath` and crate, each lang item, each intrinsic by name), so std's MIR library and the program's MIR mix in one collector;
the layout of every std type exactly as rustc computes it (`fmt::Arguments`, `Formatter`, `Vec`, `String`, `Option<&T>`'s
niche), because std's lowered non-generic code reads them; the Rust ABI exactly, for every function whose address reaches std's
compiled code (vtable methods, function pointers, `lang_start`'s `main`); and std's symbol names for non-generic calls.

## 2. Measurements

### 2.1 rustc's compiler by crate (1.99.1, `compiler/`, lines of `.rs`, `wc -l`, tests included where in `src/`)

866k lines in 2,167 files over 80 crates. The crates on the path source to MIR to IR, with what a trusting front end needs:

| crate | lines | what is needed |
|---|---:|---|
| `rustc_expand` | 13,699 | `expand.rs` 2,635, `mbe/` (macro_rules: `macro_rules.rs` 1,873, `transcribe` 1,050, `macro_parser` 750, `quoted` 472, `metavar_expr` 341), `base` 1,342, `config` (cfg) 444, `placeholders` 421, `module` 307; not `proc_macro_server` 853 (Tier P), `mbe/macro_check` 646, diagnostics 1,178 |
| `rustc_builtin_macros` | 15,304 | `deriving/` (generic 1,920, debug, clone, cmp, default, hash: about 4,000), `format` 1,170, `assert` 628, `source_util` 375, `cfg` 314, `concat`/`env`/`edition_panic`/`standard_library_imports` 500; not `autodiff` 912, `asm` 672, `eii` 639, `test` + harness 992 (later), diagnostics 1,220 |
| `rustc_span` (hygiene) | 12,043 | `hygiene.rs` 1,572 (syntax contexts, expansions, transparency), `symbol.rs` 3,135 (the pre-interned names: data) |
| `rustc_attr_parsing` | 16,077 | a few thousand: `cfg`, `repr`, `inline`, `lang`, `derive`, stability ignored; diagnostics 2,037 not |
| `rustc_resolve` | 28,912 | `late.rs` 5,695, `lib.rs` 3,082, `ident.rs` 2,188, `imports.rs` 1,950, `build_reduced_graph` 1,570, `macros.rs` 1,281, `def_collector` 610; not `late/diagnostics` 4,708, `diagnostics/` 5,984, `rustdoc` 800, `check_unused` 631: about 17,000 accepting |
| `rustc_ast_lowering` | 13,822 | `lib.rs` 3,367, `expr.rs` 2,321 (for, `?`, `while let`, `if let` chains, ranges, `async`), `item.rs` 2,096, `path` 608, `pat` 563, `format.rs` 556 (format_args to `fmt::Arguments`), closures 379; not delegation 1,742, contracts 358, asm 514 |
| `rustc_hir` | 8,162 | the HIR's data types: `hir.rs` 5,187, `def.rs` 968, lang items |
| `rustc_hir_analysis` | 39,634 | `collect/` (type_of, generics_of, predicates, item bounds, resolve_bound_vars: about 7,600), `hir_ty_lowering/` (3,900 + bounds, generics, dyn: 2,800), inherent impls 231, specialization graph; not `check/` (wfcheck 2,490, compare_impl_item 3,264, check 2,339: a checker's), coherence's orphan and overlap, `variance/`, `outlives/`, diagnostics 3,612 |
| `rustc_hir_typeck` | 49,535 | `expr.rs` 3,974, `fn_ctxt/checks.rs` 3,672 and `_impl.rs` 1,664, `pat.rs` 3,399 (binding modes), `method/probe.rs` 2,749 + `confirm` 817 + `mod` 580, `upvar.rs` 2,698 + `expr_use_visitor` 1,892 (closure captures), `coercion.rs` 2,226, `op.rs` 1,350, `callee` 1,268, `cast` 1,235, `closure.rs` 1,117 (signature deduction), `writeback` 1,095, `fallback` 734, `place_op` 435, `_match` 564, `autoderef` 73; not `method/suggest` 5,050, `fn_ctxt/suggestions` 3,904, `adjust_fulfillment_errors` 1,205, `diagnostics` 1,350, `arg_matrix` 411, edition lints 514, `inline_asm` 624: about 28,000 accepting once the inline diagnostics of `expr.rs` and `checks.rs` are dropped |
| `rustc_infer` | 13,233 | type and const variables, unification, snapshots, canonicalization, generalization: about 5,000; not the region half (`lexical_region_resolve`, `region_constraints`, `outlives`: 3,800) |
| `rustc_trait_selection` | 50,298 | old solver: `traits/select/` 6,121, `project` 2,167, `fulfill` 1,074, `specialize/` 1,039, `vtable` 448, `normalize` 524; new solver's glue `solve/` 2,300; not `error_reporting/` (about 24,000), `dyn_compatibility`, `auto_trait` (rustdoc), `coherence` (overlap checks) |
| `rustc_next_trait_solver` | 12,069 | the new solver, generic over an `Interner`: `eval_ctxt` 2,466, `trait_goals` 1,676, `assembly/` 2,576, `normalizes_to` 1,129, canonicalization 1,232, `search_graph` 159 (+ `rustc_type_ir/search_graph` 1,719) |
| `rustc_type_ir` | 18,623 | the solver's types and folding, generic: `ty_kind` 2,068, `binder` 1,293, `relate` 1,308, `fold`/`visit` 1,328, `predicate` 914, `elaborate` 439, `fast_reject` 541, `canonical` 414: about 10,000 |
| `rustc_mir_build` | 22,837 | `builder/` (matches 4,742, scope 2,190, expr 3,431, block 355, cfg 148, mod 1,171), `thir/cx` 2,083, `thir/pattern` 1,651, `thir/constant` 116; not `check_match` 1,572 (exhaustiveness, a checker's), `check_unsafety` 1,118, `check_tail_calls` 485, `custom/` (MIR as Rust) 895, diagnostics 1,440: about 16,000 accepting |
| `rustc_mir_transform` | 35,195 | the semantic passes: `elaborate_drops` + `elaborate_drop` 2,217, `shim.rs` 1,230 + async 435 (drop glue, clone shims, fn pointer and vtable shims), `promote_consts` 1,105, `coroutine/` 2,815 (Tier S), `lower_intrinsics` 346, Derefer, CleanupPostBorrowck, AbortUnwindingCalls, ElaborateBoxDerefs, SimplifyCfg, AddCallGuards: about 7,500; not the optimizations (`gvn` 2,214, `inline` 1,640, `jump_threading`, `dataflow_const_prop`, `dest_prop`, ...), `validate` 1,697, coverage, lints |
| `rustc_const_eval` | 22,739 | `interpret/` 12,200 (memory 1,734, validity 1,726: not, intrinsics 1,427 + simd 901, place 1,202, call 1,127, operand 919, step 691, cast 589, operator 543), `const_eval/` 3,500; not `check_consts` 2,700 (a checker's) |
| `rustc_abi` + `rustc_ty_utils` + `rustc_middle/ty/layout.rs` | 6,122 + 5,353 + 1,495 | layout (`layout.rs` 1,525 + `layout/ty`, `simple`, `coroutine`; `ty_utils/layout.rs` 1,090), `ty_utils/abi.rs` 737, `instance.rs` 439, `needs_drop.rs` 562; `rustc_target`'s x86-64 calling convention |
| `rustc_monomorphize` | 4,816 | `collector.rs` 1,962; not `partitioning` 1,396 (one codegen unit), offload, mono checks |
| `rustc_symbol_mangling` | 2,311 | `v0.rs` 1,087 |
| `rustc_codegen_ssa` | 30,320 | `mir/` 9,541 (block 2,547, operand 1,248, rvalue 1,199, intrinsic 637, place 534, mod 713, analyze 407); not `back/` (linking, archives: about 13,000), debuginfo, naked asm, retag |
| `rustc_codegen_llvm` | 30,648 | the LLVM side of a builder (`builder.rs`, `intrinsic.rs`, `abi.rs`, `type_of.rs`, `consts.rs`): about 8,000 relevant; the rest is LLVM's API, passes, debuginfo |
| `rustc_middle` | 65,511 | the data types: `ty/` (sty, generic_args, adt, instance, predicate, typeck_results), `mir/` (syntax 1,751, terminator, statement, consts), `thir.rs` 1,130, `mir/pretty.rs` 2,082 and `ty/print/pretty.rs` 3,623 (the judges' printers); not the query plumbing, dep graph, on-disk cache (about 6,000) |

Not needed at all: `rustc_borrowck` 35,808, `rustc_lint` 25,613, `rustc_pattern_analysis` 6,001 (exhaustiveness; match
lowering does not need it), `rustc_passes` 9,118 (checks; `lang_items` and `entry` excepted: a few hundred lines),
`rustc_privacy` 2,028, `rustc_codegen_gcc` 34,062, `rustc_codegen_cranelift` 23,514, `rustc_incremental`, `rustc_query_impl`,
`rustc_metadata` 11,647 (std's metadata comes as the helper's dump, RFS4), `rustc_public` 11,200 (the helper's API, not ours),
`rustc_errors`, `rustc_lint_defs`, `rustc_thread_pool`, `rustc_sanitizers`, `rustc_transmute` (a checker's). Files of
diagnostics alone (`error_reporting/`, `diagnostics*.rs`, `suggest*.rs`, `errors.rs`, `lints/`): 96,544 lines across the
compiler, before the diagnostics inside other files.

**The accepting path, summed**: expansion and builtin macros about 23,000; resolution 17,000; AST lowering 10,500; collection
and type lowering 16,000; trait solving with inference's core 29,000; type checking 28,000; THIR and MIR building 16,000; the
semantic MIR passes 7,500 (10,300 with coroutines); constant evaluation (the interpreter less validity) about 13,000 as a
reference; layout and ABI 7,000; monomorphization, instances, mangling and vtables 4,500; codegen 15,000. **About 170,000 rustc
lines, 20% of the compiler.** At the parser's ratio (4,862 `.ae` lines for 34,000 rustc lines with recovery: about 0.15) to
twice it for analysis code with fewer messages to drop (0.25 to 0.35), **45,000 to 60,000 `.ae` lines**.

### 2.2 The library: what std, core and alloc use

The sysroot's sources (`library/`, tests and benches left out): core 173,561 lines in 288 files, alloc 67,490 (93), std
148,741 (647, all platforms' `sys`), stdarch 516,991 (206: `core::arch`, included into core by path; x86-64's share is what
matters), compiler-builtins 44,515, portable-simd 8,764, std_detect 4,673, backtrace 7,911, test 7,435, proc_macro 5,046,
panic_unwind 642, unwind 575, and the vendored crates (hashbrown, libc, memchr, addr2line, gimli, object, miniz_oxide, ...).
rsi's sysroot is 110 codegen units of 25 crates (EVAL.md §3).

The unstable surface (counts over `core/src`, `alloc/src`, `std/src`):

| what | count | what the front end must do |
|---|---:|---|
| `#![feature(...)]` lines in the crate roots | core 92, alloc 129, std 149; 253 distinct | each a language or library feature; most library features are names only, the language ones below |
| `#[lang = "..."]` items | 158 distinct | the front end's wired-in names: operators, `Sized`, `Copy`, `Drop`, `Fn*`, `Deref`, `Try`, `IntoIterator`, `Range*`, `panic_*`, `format_*`, `Box`, `drop_in_place`, `start`, ... (Tier U reads them from std's interface) |
| `#[rustc_intrinsic]` functions | 316 (`intrinsics/mod.rs` 234, `simd/` 80, `gpu.rs` 2) | roughly half carry a fallback body in core (a codegen back end may use it); the rest must be lowered (`LowerIntrinsics`) or emitted by codegen, or evaluated by const evaluation |
| `default fn` (specialization) | 138 | `min_specialization` and `specialization`: selection at monomorphic types picks the most specific impl (the specialization graph) |
| `rustc_specialization_trait`, `rustc_unsafe_specialization_marker` | 11 files | the markers specialization may test (`TrustedLen`, `FusedIterator`, ...) |
| `const trait`, `impl const` | 82 | const traits (`[const] Trait` bounds), checked by const evaluation's callers |
| `const fn` | 2,374 | callable by constant evaluation: their MIR for CTFE (`mir_for_ctfe`) |
| `macro_rules!` definitions | 324 | the integers' API is macro-generated (`int_macros.rs`, `uint_macros.rs`, `nonzero.rs`: 11,174 lines of macro bodies); item-level macro calls in the three crates: 6,221 |
| `macro` 2.0 definitions | 67 | def-site hygiene (`pub macro`); std's builtin macros are declared so, with `#[rustc_builtin_macro]` (42 in `core/src/macros/mod.rs`) |
| `cfg_select!` | 132 | the configuration macro (`cfg_if!`'s successor) |
| distinct `#[rustc_*]` attributes | 62 | layout scalar ranges (`rustc_layout_scalar_valid_range_*`, the niches of `NonNull`, `NonZero`), `rustc_nonnull_optimization_guaranteed`, `rustc_box`, `rustc_inherit_overflow_checks`, `rustc_const_*` (ignored), `rustc_diagnostic_item` (ignored), ... |
| `impl` blocks | 5,893 written | plus the macro-generated ones (every integer and float type's operators and conversions) |
| `#[derive(...)]` | 915 | the builtin derives |
| `asm!` | 30 | inline assembly: refused, as rsi refuses it (`:inline-asm`) |

Tier U sees all of this only as data (std's interface and MIR, RFS4); Tier S must implement it.

### 2.3 The programs: what user code uses

The UI suite's run programs (3,718 revisions, `test/rust/eval-programs.edn`) are rsi's main corpus; their text is not on this
host today, so their census (per program: `#![feature]`s, `macro_rules!`, derives, `async`, `dyn`, generics with associated
types, closures, `impl Trait`, unions, `asm!`, editions) is RF1's first measurement, and Tier U's subset is drawn from it.
From memory, a sizeable share of UI run tests opt into `#![feature]` (they test the compiler), an estimate to verify at RF1.

rsi's own programs (`rust-re5`: the 67 synthetic programs and the 5 eval samples, 1,593 lines) are ordinary stable Rust: 2
files with `#![feature]`, 8 with derives, 1 with `macro_rules!`, 72 with the formatting macros (696 calls), 19 with `vec!`, 18
with closures (88), 10 with trait impls, 6 with `dyn`, 8 with `HashMap`/`BTreeMap`, 4 with `Rc`/`RefCell`, 3 with
`catch_unwind`, no `where` clause.

The registry cache (518 crates, 9,437 source files outside tests, benches and examples; a sample of the ecosystem, not of
programs): `#[derive]` in 457 crates (88%), `macro_rules!` 292 (56%), `unsafe` blocks 301 (58%), `dyn` 237, const generics
206, generic associated types or generic aliases 170, return-position `impl Trait` 128 (25%), `extern "C"` 56, `asm!` 23,
`async fn` 16; no `#![feature]` (205 crates gate nightly paths behind `cfg_attr`). **Proc macros: 32 of the 518 are
proc-macro crates** (`serde_derive`, `thiserror-impl`, `clap_derive`, `async-trait`, `tracing-attributes`, ...) and 43 depend
on one directly; the UI suite's run programs use proc macros only through auxiliary crates (411 revisions RR5 leaves out).
**Build scripts: 65 crates.**

### 2.4 Probes (the host's rustc 1.99.1)

`p.rs`, 28 lines: an enum deriving `Debug`, `Clone`, `PartialEq`; a trait and its impl with a `match`; a `Display` impl with
`write!`; a `Vec` of the enum, a `HashMap<&str, Vec<f64>>` with `entry().or_default().push()`, an `iter().map().sum()`, a
sort, `println!`. Its stages (`RUSTC_BOOTSTRAP=1 rustc --edition 2021 -Zunpretty=M`):

| stage | lines |
|---|---:|
| `expanded` (the source after expansion: derives' impls, the prelude, `format_args!`) | 98 |
| `ast-tree,expanded` | 5,415 |
| `hir` / `hir,typed` | 136 / 228 |
| `hir-tree` | 8,876 |
| `thir-tree` / `thir-flat` | 6,301 / 8,369 |
| `mir` (the crate's own bodies, runtime-optimized) | 720 |
| `-Zdump-mir=main`: files | 196 (for `main` alone: 50 passes' "after" dumps, from `built` through `runtime-optimized`) |
| IR at `-O0`, one unit: lines, functions defined, declared | 13,746, 266, 61 |
| IR with `-Zshare-generics=no`: functions defined, declared | 280, 58 |

Of the 266 functions, **9 are the program's own** (`main` twice, the closure, `area`, the derived `clone`, `eq`, `fmt`, the
`Display` `fmt`, `lang_start`'s closure); the rest are std's generics instantiated for it (by v0 path: 210 name core, 79
alloc, 61 std, 42 hashbrown; a name can name several). The 61 declarations are std's non-generic functions (`fmt::write`,
`panicking::panic_fmt`, `io::stdio::_print`, `rt::lang_start_internal`, `__rust_alloc`, ...) and LLVM intrinsics.

The new trait solver: the 67 synthetic programs, the 5 samples and `p.rs` (73 files) compiled with `-Znext-solver=globally`
give **the same MIR** as the default (old) solver, byte for byte (`-Zunpretty=mir`), and none fails. In 1.99.1 the new solver
is the default in coherence only (`NextSolverConfig { coherence: true, globally: false }`, `rustc_session/src/config.rs`).

## 3. The pieces

For each piece: rustc's text and size, what a port costs, what a clean reimplementation costs, how it is judged. Days are
working days of one agent as the surveys give them; the port rate is calibrated on the native parser (34,000 rustc lines with
recovery to 4,862 `.ae` lines in one session day, tree-equal) and slowed for analysis code, whose judges are less direct:
about 2,000 to 4,000 rustc accepting lines a day with its judge.

### 3.1 Input: the native reader's AST

The front end reads the native reader's dumps (`arbace.repr.rust.native`: rustc's `ast::Crate` as the helper's Debug writes it,
spans, token streams of macro calls and attributes), not repr/rust's Rust forms (for tools and print; they drop node ids and
the token trees' spacing expansion needs). The module tree (`mod m;` files, `#[path]`) is loaded as rustc's `module.rs` does.
RFS (none needed): the reader's `native-check` already proves the input equal to rustc's parse.

### 3.2 Expansion: cfg, `macro_rules!`, builtin macros, derives, hygiene

rustc: `rustc_expand` (expansion loop, invocation collection, placeholders, `cfg` stripping and `cfg_attr`, `macro_rules!`'s
matcher and transcriber with metavariable expressions), `rustc_builtin_macros` (`format_args!`, `assert!`, `panic!` by
edition, `concat!`, `stringify!`, `include*!`, `env!`, `line!`/`file!`/`column!`/`module_path!`, `cfg!`, `cfg_select!`,
`compile_error!`, the builtin derives `Clone`, `Copy`, `Debug`, `Default`, `Hash`, `PartialEq`, `Eq`, `PartialOrd`, `Ord`,
the std prelude's injection), `rustc_span::hygiene` (syntax contexts, transparency: `macro_rules!`' mixed-site, `macro`'s
opaque def-site). Expansion interleaves with resolution (a macro's path is resolved before it is expanded; `#[macro_use]`,
`$crate`, textual scoping of `macro_rules!`), so it shares resolution's early half (3.3).

Port: about 23,000 accepting lines, 6,000 to 8,000 `.ae`, **8 to 12 days** (with the early resolver's part). Clean: no saving,
since the output is checkable token for token and the derives' and `format_args!`'s shapes must be rustc's exactly (the MIR
judge sees them). Judge: the helper's new `expanded` mode writes the expanded crate in the native reader's dump format, and
the native front end's expanded crate is compared as data, as `native-check` compares parse trees; `-Zunpretty=expanded`
text as a second view. Tier U needs std's exported macros (`println!`, `vec!`, `assert_eq!`, `write!`, `matches!`) and their
hygiene: std's macro definitions come in std's interface as token trees with their transparency.

### 3.3 Name resolution

rustc: `rustc_resolve`'s def collector, reduced graph (modules, imports, globs, re-exports), import resolution to a fixpoint,
the early resolution of macro paths, and late resolution (ribs for locals, generics, labels; hygiene for locals; associated
items left to type checking; `traits_in_scope` per method call for the probe), with edition rules (2015's paths, the 2018
extern prelude, the 2021 prelude's additions). Port: about 17,000 accepting lines, 5,000 to 6,000 `.ae`, **8 to 12 days**.
Clean: 4,000 lines but glob shadowing, macro-expanded imports and the preludes' order are rustc's rules either way. Judge: the
helper's `resolve` mode writes each path's resolution (`Res`: def path, local, primitive, self type) by span, and the traits in
scope per method call; compared per node.

### 3.4 AST to HIR

rustc: `rustc_ast_lowering`: the desugarings (`for` to `IntoIterator::into_iter` and a `loop` over `next`, `?` to
`Try::branch` and a `match`, `while let`, `if let` chains, ranges to lang items, `format_args!` to `fmt::Arguments` (`format.rs`),
closures, `async` blocks to coroutines (Tier S), `..` in struct expressions), HIR ids, owners, the lowering of paths with their
resolutions. Port: 10,500 accepting lines, about 3,000 `.ae`, **4 to 6 days**. Judge: `-Zunpretty=hir` text by a port of
`rustc_hir_pretty` (2,745 lines, about 900 `.ae`), and `hir-tree` where the text is ambiguous.

### 3.5 Type collection and type lowering

rustc: `rustc_hir_analysis`'s `collect` (generics, `type_of`, predicates and item bounds, explicit and implied), the HIR type
lowering (`hir_ty_lowering`: paths to types, `T::Assoc` lookups through bounds, `dyn` principal and projections,
argument-position `impl Trait`, elided lifetimes: erased), the inherent impls index, the trait impls index, lang items, and the
specialization graph's ancestors. With regions erased (RFS5), `resolve_bound_vars` keeps only late-bound vs early-bound for
generics' count, `variance` and `outlives` go. Port: about 16,000 accepting lines, 4,500 to 5,500 `.ae`, **6 to 9 days**.
Judge: the helper's `iface` mode, the same dump that carries std's interface in Tier U, written for the program's own crate:
generics, predicates, types of items, impls, ADT definitions, compared item by item. One format serves as std's data and as the
judge of the program's collection.

### 3.6 The trait solver

What it must answer: trait obligations with inference variables during type checking (method calls, operators, closures'
`Fn*` bounds, `?`'s `Try`, `IntoIterator`, `Deref`), projections (`<I as Iterator>::Item` everywhere), auto traits (`Send`,
`Sync`, `Unpin`), the builtin impls (`Sized`, `Copy`/`Clone` of tuples, arrays and closures, `Fn*` of function items, pointers
and closures, `Unsize`/`CoerceUnsized` for coercions, `Pointee`, `DiscriminantKind`, `Destruct`, `Tuple`), and at
monomorphic types the codegen-time selection (instance resolution with specialization, vtable entries). Regions erased; no
coherence, no overlap checks, no error reporting.

Options: port the **new solver** (`rustc_next_trait_solver` 12,069 and the generic parts of `rustc_type_ir` about 10,000, with
an `Interner`/`SolverDelegate` facade over Arbace's types: what rust-analyzer does (memory, verify), and the solver rustc is
moving to); or port the **old solver** (`traits/select` 6,121, `project` 2,167, `fulfill` 1,074, `specialize` 1,039, plus
`rustc_infer`'s core): what 1.99.1 uses for type checking. The probe (§2.4) found the two agree on all 73 programs tried; RF1
measures the UI run programs under `-Znext-solver=globally` (MIR equal or not) before the choice (RFS6). Port: about 22,000
(new) or 16,000 (old) accepting lines plus inference's core 5,000, 7,000 to 9,000 `.ae`, **10 to 15 days** (Opus). Clean (an
SLD resolver with projection normalization for Tier U's subset): 4,000 to 6,000 lines, 8 to 12 days, and divergences on
ambiguity and on projection caching that the judges would chase for long: not recommended. Judge: indirect, through type
checking's facts (method resolutions, normalized types) and the mono collector's instances (each `Instance` rustc's).

### 3.7 Type checking: inference, methods, autoderef, coercions, closures

rustc: `rustc_hir_typeck`: expression checking with expectations, statements, the argument checks (two passes: closures last),
patterns with default binding modes (match ergonomics, edition 2024's rules), method probing (`probe.rs`: the autoderef steps,
by value, `&`, `&mut`, array-to-slice unsizing, inherent before trait candidates, `traits_in_scope`, the edition's prelude
collisions) and confirmation, autoderef and overloaded deref and index (`place_op`), coercions (reborrows, `&mut` to `&`,
unsizing, function items to pointers, non-capturing closures to pointers, never), casts' kinds, operators (builtin for scalars,
else the traits), calls (`callee`: function, closure, `Fn*` overloaded), closures (signature deduction from the expected type
and pending obligations; capture analysis, `upvar.rs` with `expr_use_visitor`: the edition 2021 precise captures against 2018's
whole variables; closure kind inference), fallback (integer `i32`, float `f64`, never-type fallback by edition), writeback.
Port: about 28,000 accepting lines, 8,000 to 10,000 `.ae`, **14 to 22 days** (Opus); **the long pole**. Clean: the observable
choices (which method, which capture, which coercion, the fallback) would be re-derived from the Reference and the FLS and
would differ at the edges; rejected. Judge: the helper's `typeck` mode writes each body's `TypeckResults` as data: the node
types, the method resolutions (`type_dependent_defs` with their generic arguments), the adjustments (autoderef, autoref,
pointer coercions), binding modes, closure captures and kinds, coercion casts, the fallback's choices, after region erasure;
compared fact by fact, as repr/java's `attr-check` compares javac's attribution (JN9). `-Zunpretty=hir,typed` as a view.

### 3.8 Constant evaluation

Needed for array lengths, `const` items and `const {}` blocks, statics' initial bytes (with relocations: pointers into other
allocations, which become LLVM globals), enum discriminants, const generic arguments, promoted constants, and std's const fns
called from them (`Vec::new`, `String::new`, `AtomicUsize::new`, `Mutex::new`, `u32::MAX.count_ones()`, `size_of`). rustc's is
a MIR interpreter (`rustc_const_eval/interpret`, the engine Miri extends) with validity checks and a checker (`check_consts`).

Options: a **clean interpreter over MIR forms** with a flat memory of allocations with provenance (pointers as allocation and
offset, so statics' relocations come out), the const intrinsics ported from `interpret/intrinsics.rs`: 3,000 to 5,000 `.ae`,
**6 to 10 days**; or a port of `interpret/` less validity (about 10,000 lines): 3,000 `.ae` more and a closer match on the
corner cases (float semantics are LLVM's and IEEE's either way; pointer comparisons in const). Judge: the allocations of every
`const` and `static` against rustc's (the helper's `consts` mode writes each evaluated constant's bytes and relocations; the
IR's globals as a second view). The same interpreter is RE10's MIR interpreter's seed (RUST-EVAL-SURVEY §4.2).

### 3.9 THIR and MIR building

rustc: THIR construction (`thir/cx`: adjustments applied, overloaded operators made calls, method calls resolved, closures'
upvars, patterns lowered with constants to patterns), and the MIR builder: places and rvalues (`as_place`, `as_rvalue`,
`as_operand`, `as_temp`), `into` destinations, blocks, `let`-`else`, the match lowering (`matches/`: candidates, tests,
buckets, bindings, guards, fake borrows: dropped with borrowck), the drop scopes and unwind paths (`scope.rs`: the order of
drops on every exit, `StorageDead`, edition 2024's `if let` and tail-expression scopes), loops and breaks, `return`, `?`'s
paths. Port: about 16,000 accepting lines, 5,000 to 6,000 `.ae`, **8 to 12 days**. Clean: drop order and temporaries' scopes
are observable (a `Drop` that prints) and rustc's rules are the language's; rejected. Judge: `-Zdump-mir=all` at `built`
(after `SimplifyCfg-initial` to drop the unreachable blocks the builder leaves), by a port of `mir/pretty.rs` (2,082 lines,
with the type printer's subset, about 1,200 `.ae`) printing Arbace's MIR as rustc's text, compared after renumbering locals
and blocks by first use; `-Zunpretty=thir-tree` for THIR.

### 3.10 The semantic MIR passes

At `-O0`, `main`'s body goes through 50 passes (§2.4); the ones that change meaning: `PromoteTemps` (a `&` of a constant
expression becomes `'static`: without it a returned `&[1, 2, 3]` dangles), `ElaborateDrops` (drop flags, conditional drops
of moved-out values, `drop_in_place` calls), `Derefer`, `ElaborateBoxDerefs`, `CleanupPostBorrowck`, `AbortUnwindingCalls`,
`AddMovesForPackedDrops`, `LowerIntrinsics`, `AddCallGuards`, `StateTransform` (coroutines: `async`, Tier S), and the shims
(`shim.rs`: drop glue, `Clone` of closures and tuples, `FnPtrShim`, `ClosureOnceShim`, vtable shims). The rest at `-O0`
(`InstSimplify`, `SimplifyLocals`, `RemoveZsts`, `SingleUseConsts`, `CopyProp`, `RemoveUnneededDrops`,
`UnreachableEnumBranching`, the checks `CheckAlignment`/`CheckNull`/`CheckEnums` under debug assertions) are optimizations or
checks. Port of the semantic ones: about 7,500 lines (10,300 with coroutines), 2,500 `.ae`, **3 to 5 days**; the `-O0`
optimizations, if the IR is to be rustc's byte for byte end to end (RFS2's variant), about 4,000 lines more, 3 days.
Judge: `-Zdump-mir` after each ported pass, and `runtime` phase MIR.

### 3.11 Monomorphization, layout, ABI, symbols

rustc: the collector (`collector.rs`: roots from `main` and statics, uses in bodies, vtables of unsizings, drop glue, shims,
`#[track_caller]` instances), `Instance::try_resolve` (codegen-time selection with specialization, at monomorphic types:
simpler than type checking's), layouts (`rustc_abi::layout`: field reordering of `repr(Rust)` structs by alignment, niches,
enum tag encodings, `rustc_layout_scalar_valid_range_*`; `ty_utils/layout.rs`), the Rust and C ABIs (`ty_utils/abi.rs`,
`rustc_target`'s x86-64 System V), v0 symbol names with the crates' disambiguators (std's from its interface). Port: about
11,500 lines, 3,500 to 4,500 `.ae`, **6 to 9 days**. Judge: the mono items list against rustc's (`-Zprint-mono-items`),
layouts against the helper's `layout` mode (size, alignment, field offsets, variants, niche, ABI per type), symbols against
the IR's.

### 3.12 Codegen to LLVM forms

rustc: `rustc_codegen_ssa/mir` (operands as immediates, pairs or references; places; rvalues; terminators with `invoke` and
cleanup pads; intrinsics; drops; asserts and their panics; switch on discriminants; casts) over a builder, and
`rustc_codegen_llvm`'s choices (LLVM types, intrinsic mapping, constant globals, attributes). Port, emitting `arbace.repr.llvm`
forms: about 15,000 lines, 4,500 to 6,000 `.ae`, **8 to 12 days**. Debug info out. Judge: given rustc's own runtime MIR (the
helper's `mir` dump of the program and std's MIR library), the IR must equal rustc's `-O0 -Zshare-generics=no` IR after a
normalizer (attribute groups, metadata, value names, unnamed globals' order): the exact judge of RF3; then rsi's run.

### 3.13 std as data (Tier U)

The helper (`tools/rsdump`, already a `rustc_private` driver with an `after_analysis` hook, RR6) gains modes that dump, for
the pinned sysroot (`.tmp/rsi/std`, EVAL.md §3): **std's interface** (every item's def path, kind, generics, predicates, type,
ADT definition with variants, fields and `repr`, impls with their items, associated items, lang items, diagnostic items needed
by expansion, the specialization graph, macros as token trees with hygiene transparency, exported symbols) and **std's MIR
library** (`optimized_mir` of every generic and `#[inline]` item, and `mir_for_ctfe` of const fns: what rustc's own
monomorphization and CTFE read from metadata). Forms, one file per item keyed by def path, pinned by digest beside
`test/rust/runtime/std.edn`, loaded lazily. About 1,500 to 2,500 lines of Rust in the helper, **5 to 8 days** with the
normalizer and the MIR forms' spec. A native `.rmeta` reader instead: rustc's metadata encoding changes every release and is
about 11,600 lines of `rustc_metadata`: no (RFS4).

### 3.14 Proc macros and build scripts (Tier P)

A proc macro is a Rust crate compiled to a host dylib that rustc loads and calls through `proc_macro`'s bridge (RPC over
buffers with handles; `rustc_expand/proc_macro_server.rs` serves it). Two shapes on Arbace: **mrustc's** (memory, verify):
the proc-macro crate is linked with a replacement `proc_macro` library into an executable that reads token streams on stdin
and writes them on stdout; the compiler runs it per invocation. On Arbace: the proc-macro crate compiled by the front end, with
Arbace's own `proc_macro` crate (written anew in Rust, about 2,000 lines: `TokenStream`, `TokenTree`, `Span`, `Literal`, the
text protocol), run on rsi; `syn`, `quote`, `proc-macro2` then compile as ordinary crates. Or rustc's bridge served in
process by Arbace (ci calling back into Arbace per token operation): tighter, but needs ci-to-host callbacks ci does not have.
Build scripts are ordinary programs run on rsi before the crate, their `cargo:` lines parsed (RUST-EVAL-SURVEY §9). **12 to 20
days** with multi-crate programs (the UI suite's 411 auxiliary revisions).

### 3.15 Tier S: std from source

What Tier S adds to Tier U's front end: lang items defined (not read), intrinsics declared and their bodies' fallbacks,
specialization in selection (`default fn`, the graph, `min_specialization`'s markers), const traits, `rustc_*` attributes
(layout ranges, `rustc_box`, `rustc_inherit_overflow_checks`, `rustc_nonnull_optimization_guaranteed`), `macro` 2.0 hygiene,
`cfg_select!`, auto traits and negative impls, GATs, return-position `impl Trait` in traits and opaque types (inference of
their hidden types), coroutines (`StateTransform`, `coroutine/layout`), `repr(simd)` and the `simd_*` intrinsics, `#[track_caller]`,
`extern` blocks, thread locals, `#[global_allocator]`'s shim, `panic_unwind`'s personality; the vendored crates (hashbrown, libc,
memchr, ...); stdarch's x86-64 subset std reaches. Judge: std's MIR against its metadata MIR (the Tier U data: the front end's
output and its own input agree), std's IR units against the pinned `test/rust/runtime/ir.sha256` after the normalizer, and
the corpus run with Arbace's std. **35 to 60 days.**

## 4. Judging

Every stage has an oracle from the host's rustc at the pin (never at program build time), cached by key; each comparison has
its negative control (GTS81 (a)): a crate judged against another crate's reference must fail.

| stage | oracle | compare | exact? |
|---|---|---|---|
| reader | the helper's `dump` | trees, as today (`native-check`) | yes, done |
| expansion | helper `expanded`: the expanded crate in the reader's dump format; `-Zunpretty=expanded` | trees as data | yes |
| resolution | helper `resolve`: `Res` per path by span, traits in scope per method call | per node | yes |
| HIR | `-Zunpretty=hir` (the port of `rustc_hir_pretty` prints ours); `hir-tree` | text | yes |
| collection | helper `iface` of the program's crate (std's format) | per item, after alpha-renaming generics | yes |
| type checking | helper `typeck`: `TypeckResults` per body, regions erased | fact by fact | yes |
| THIR | `-Zunpretty=thir-tree` | text, ids renumbered | yes up to ids |
| MIR | `-Zdump-mir` at `built`, after each ported pass, and `runtime`; the port of `mir/pretty.rs` prints ours | text, locals and blocks renumbered by first use | up to the renumbering |
| constants | helper `consts`: bytes and relocations of every evaluated constant and static | per constant | yes |
| mono items, layouts | `-Zprint-mono-items`; helper `layout` | sets; per type | yes |
| IR (back half) | rustc `-O0 -Zshare-generics=no --emit=llvm-ir`, from rustc's MIR (RF3) | normalized IR | yes after the normalizer |
| run | the judge's recorded runs (`test/rust/ref-rustc-1.99.1.edn`, RES5) | status, stdout, stderr per program, RR5's normalizations | yes |

The **run** is the final judge, as in the eval survey: a program passes when rsi's run of Arbace's IR equals the judge's.
The stage oracles localize a failure and make the early milestones judgeable without rsi (expansion, resolution, HIR,
collection and type checking are judged on data alone, no night budget). Where Arbace's pipeline stops reproducing rustc's
passes (the `-O0` optimizations, §3.10), the IR is no longer compared end to end, only the MIR at `runtime` and the run; the
back half's own exactness was established on rustc's MIR (RF3). Corpora: rsi's synthetic corpus and samples (all in Tier U's
subset), the UI run programs in Tier U's subset (RF1's census), std's `coretests`/`alloctests` (Tier S), the crate set's
binaries (Tier P); the compiler corpus's 2,167 files for expansion, resolution, collection and type checking (no run needed),
which also measures the front end's speed on large crates.

## 5. Prior art

From memory, to verify (a fetch of each repository would confirm sizes and licenses; none was made):

- **mrustc** (John Hodge, `github.com/thepowersgang/mrustc`, MIT): a Rust compiler in C++ that emits C, written to bootstrap
  rustc (1.19, 1.29, 1.39, 1.54, 1.74 by its releases, newer in progress). It trusts rustc's acceptance: no borrow checker, few
  diagnostics. Passes: parse, expand (cfg, `macro_rules!`, derives, builtins; proc macros by building them into executables over
  a token protocol on stdin/stdout, with its own `libproc_macro`), resolve, lower to HIR, HIR conversion (paths to absolute,
  UFCS, bindings), HIR type checking (an outer pass over signatures, then per-body constraint solving, `expr_cs.cpp`, several
  thousand lines), HIR expansion (closures to structs, value usage, reborrows, vtables, erased types), MIR from HIR, MIR cleanup
  and optimizations, monomorphization, codegen to C (`codegen_c.cpp`). Roughly 130,000 to 150,000 C++ lines in all. Its shape
  is this survey's (a trusting front end to a MIR, then a C or IR back end); its MIR is its own, its std support follows its
  target rustc versions, and it is C++: prior art and a design reading, not a part (RFS1 (e)). Its proc-macro shape is RFS8 (a).
- **rust-analyzer** (`github.com/rust-lang/rust-analyzer`, MIT or Apache-2.0): `hir-expand` (`macro_rules!` through its `mbe`
  crate, builtin macros and derives, proc macros through `proc-macro-srv` loading the compiled dylibs), `hir-def` (item trees,
  name resolution, body lowering), `hir-ty` (inference, method resolution, autoderef, coercions, closure captures, and its own
  MIR lowering with a MIR interpreter used for constant evaluation), on `rustc_next_trait_solver` and `rustc_type_ir` through the
  `ra_ap_*` crates since 2025, after chalk. The closest prior art to "rustc's solver over a foreign IR" (RFS6 (a)) and to a CTFE
  interpreter outside rustc; error-tolerant and lazy (salsa), so it diverges from rustc at the edges, which is why rustc's text,
  not rust-analyzer's, is the port's source.
- **gccrs** (the GCC front end, GPL-3): not read, clean-room rule (the day's rules: GPL sources are not ported).
- **Chalk** (MIT or Apache-2.0, retired), **a-mir-formality** (MIT or Apache-2.0, a model of the type system), **Polonius**
  (borrowck, not needed): background.
- **The Rust Reference** and **the FLS** (the Ferrocene Language Specification, now the Rust project's, MIT or Apache-2.0):
  the specifications to read beside rustc's text, never in place of the oracle.
- **rustc_codegen_clr** (MIT or Apache-2.0: MIR to .NET's CIL and to C) and **rustc_codegen_cranelift** (in the corpus,
  23,514 lines): back ends over MIR, the back half's design references; cranelift's `base.rs` and `abi/` are a smaller reading
  of `codegen_ssa` than LLVM's.
- **Miri** (`rustc_const_eval/interpret` plus its machine): the reference interpreter for RFS9 (b).
- Inside Arbace: repr/java's native attribution (JN9a-JN9d: javac's Enter, Resolve, Infer ported in tiers, each fact checked
  against javac's) is the model of RF7-RF9's judging; goi's checker (`arbace.go.check`, after go/types) and the Haskell survey's
  plan (HF1-HF12) are the sizes beside it.

## 6. Routes and tiers

Five routes to "rsi's input from native parse trees":

- **R1, a faithful port of rustc's analysis**, borrowck, lints, checks and messages included: about 450,000 lines; 300 to 400
  days; the result rejects what rustc rejects. Not the smallest; the checks can be added later as a check level.
- **R2, a trusting port of rustc's accepting paths** (the recommendation): the 170,000 lines of §2.1, messages, checks and
  regions out, ported in rustc's order and names as the parser was, judged at every stage. Tiers:
  - **Tier B** (RF1-RF3), the back half: rustc's MIR as data (the program's and std's), Arbace's collector, layout, ABI and
    codegen to LLVM forms, rsi unchanged. Useful alone (Rust's codegen without rustc's LLVM back end; RE10's MIR forms) and the
    exact base the front half is built on.
  - **Tier U** (RF4-RF11), user programs: the front half, with std's interface and MIR library as pinned data from the helper.
    No rustc at program build time; rustc at the pin.
  - **Tier S** (RF12): core, alloc, std and the vendored crates from source; the unstable surface.
  - **Tier P** (RF13): proc macros, build scripts, multi-crate programs.
- **R3, a clean front end** (mrustc's or rust-analyzer's shape, written anew from the Reference): similar size to R2's Tier U
  (mrustc's front end is not smaller than the accepting path), without agreement by construction; its divergences on method
  choice, captures and drop order would be found by runs one at a time. Rejected.
- **R4, rustc's own front end on rsi** (the analogue of HFS8): the helper (a `rustc_driver` with no LLVM back end, emitting MIR
  forms) compiled by the host's rustc once to IR, lowered and run on ci (or cf2g's AOT). No front-end days; but it is about
  860,000 lines of compiler plus some 150 crates, single-threaded on ci at ci's speed (RE4 measured about 13 s for a small UI
  program's build on ci; rustc's own run would be orders slower), needing the AOT path at whole-program scale (RES34's per-key
  translation of a sysroot of rustc's size) and every `sys` call rustc makes (files, `mmap`, threads with `-Zthreads=1`, the
  jobserver). A seed, not derived by hand, as CLAUDE.md's "derivation done once by hand" does not want for the front end; 20 to 40
  days after RE6 to measure. Kept as an oracle and a fallback for Tier S (RFS11).
- **R5, mrustc on Arbace's C++ path** (cppi): mrustc compiled by the host's clang to IR and run on ci: its own MIR (not rustc's,
  so no stage judges), its supported std is not 1.99.1's. Prior art only.

Tier U's reasoning (smallest that removes rustc from a program's build): a user's program needs expansion, resolution, HIR,
collection, the solver, type checking, THIR and MIR building, the semantic passes, CTFE and the back half; std enters as data,
so none of std's unstable surface is implemented, and the program's own unstable features are classed (`feature`) until Tier S.
What remains host-dependent in Tier U: std's interface and MIR (pinned data made by the host's rustc, as std's IR already is,
EVAL.md §3). Tier S removes that.

## 7. Milestones (RF1-RF14)

Working days; each milestone ends with its check green and its records. RF1-RF3 need only rsi as it is (RE5); RF4, RF5 and RF7
are judged on data and can run beside RF2-RF3 (RFS3).

| milestone | content | exit criteria | days |
|---|---|---|---:|
| **RF1** oracles and census | the helper's modes `expanded`, `resolve`, `iface` (std's and a crate's), `typeck`, `consts`, `layout`, `mir` (a crate's bodies at a phase; std's MIR library); `-Zdump-mir` and `-Zprint-mono-items` runs; the normalizers (MIR renumbering, IR); the UI run programs' census and Tier U's subset; `-Znext-solver=globally` over the subset (RFS6); corpora and controls | every stage's reference for the synthetic corpus, the samples and Tier U's UI subset; std's data pinned | 6-9 |
| **RF2** MIR forms | the spec (`doc/rust/MIR.md`), reader of the helper's MIR, the printer as rustc's `mir/pretty.rs` | rustc's MIR of every corpus body read and printed back equal to `-Zunpretty=mir`/`-Zdump-mir` text | 3-5 |
| **RF3** the back half (Tier B) | collector, instance resolution with specialization, vtables, drop glue and shims from data, layout, ABI, v0 symbols, codegen to LLVM forms, globals; `bin/rsi run --front mir` | from rustc's MIR: IR equal to `-O0 -Zshare-generics=no` after normalization on the synthetic corpus and Tier U's subset; runs on rsi equal to the judge's, every non-pass classed | 16-26 |
| **RF4** expansion | module loading, cfg, `macro_rules!` with hygiene, the builtin macros and derives, std's macros from its interface, the prelude | the expanded crate equal to the helper's `expanded` dump on the corpora and on the compiler corpus's crates | 8-12 |
| **RF5** resolution | the def collector, modules and imports from std's interface, late resolution, traits in scope, the editions' rules | resolutions equal per path on the corpora and the compiler corpus | 8-12 |
| **RF6** HIR | AST lowering's desugarings, the HIR printer | `-Zunpretty=hir` equal on the corpora | 4-6 |
| **RF7** collection | generics, predicates, `type_of`, HIR type lowering, impl indexes, lang items read, regions erased | the program crate's `iface` equal to the helper's | 6-9 |
| **RF8** the solver | the new (or old, RFS6) solver over Arbace's types, inference variables, projections, builtin and auto traits, codegen-time selection (RF3's moved onto it) | type checking's trait facts equal (with RF9); RF3's instances from Arbace's solver equal rustc's | 10-15 |
| **RF9** type checking | expressions, patterns, methods, autoderef, coercions, casts, closures and captures, fallback, writeback | `typeck` facts equal per body on Tier U's subset and the compiler corpus's bodies in it | 14-22 |
| **RF10** MIR from source | THIR, the builder, promotion, the semantic passes, the CTFE interpreter | MIR equal at `built` and `runtime` on Tier U's subset; constants equal | 14-22 |
| **RF11** Tier U first light and check | `bin/rsi run --front arbace FILE.rs`: no rustc; `check --front arbace` over Tier U's corpora; the classes (`feature`, `proc-macro`, `inline-asm`, ...); the cells of the row | every program passes or is in a recorded class; judged by `bin/reap` | 5-8 |
| **RF12** Tier S | the unstable surface (§3.15); core, alloc, std and the vendored crates from source; std's MIR and IR against the pinned data | std's MIR equal to its metadata MIR after normalization; the corpora run with Arbace's std | 35-60 |
| **RF13** Tier P | Arbace's `proc_macro` crate and protocol, proc macros and build scripts run on rsi, multi-crate programs | the UI auxiliary revisions and a crate set sample (serde with derive) build and pass | 12-20 |
| **RF14** speed and the milestone | the front end compiled (`ARBACE_AOT=1`), lazy std data, the nightly's share; `bin/gate --full --clean` | within GT8's budget; the tag | 4-7 |

Totals: **Tier B (RF1-RF3) 25 to 40 days; Tier U (RF1-RF11) 94 to 146; with Tier S, Tier P and RF14 145 to 233.** RF9, RF10
and RF12 are the long poles and the least certain (RF9 by a factor of 1.5 either way, as the Haskell survey's HF3). Sonnet
and Opus (the user's split): Opus for RF3's codegen and ABI, RF8, RF9, RF10's match lowering, scopes and CTFE, RF12's
specialization and coroutines; Sonnet for RF1's helper modes and census, RF2, RF4's builtin macros and derives, RF6, RF11's
classes and records, RF14's records.

**Re-seed needs.** The front end's namespaces are evaluated from the checkout like rsi's (`arbace.rust.*`), out of
`gate_key` (RFS13); no re-seed until the row is embedded.

## 8. Risks

- **Type checking's edges.** Inference order, closure signature deduction from pending obligations, method probe's
  collisions, never-type fallback and the 2024 edition's changes (fallback, `if let` scopes, tail expression temporaries,
  binding modes, RPIT captures) are where a port drifts from rustc. Mitigation: the `typeck` facts judge per body (RF9), the
  compiler corpus as a large accepted input, the controls.
- **The solver choice.** 1.99.1 type-checks with the old solver. If the new solver disagrees on the corpus (RF1 measures it),
  porting it gives other answers than the oracle's; then the old one is ported (RFS6 (b)), which is less reusable and on its
  way out of rustc.
- **std's data size and loading.** std's interface and MIR library are large (tens of thousands of items with the
  macro-generated integer impls, the MIR of every generic); loading them whole per program on the evaluated host would cost
  more than the program. Mitigation: per-item files by key, loaded lazily, cached in the compiled build (RF14).
- **ABI and layout exactness.** A wrong field order, niche or `PassMode` breaks the link with std's lowered code silently.
  Mitigation: RF3 is judged on rustc's MIR against rustc's IR before any front end exists; the helper's `layout` dump per type.
- **Drop order and temporaries.** Observable; rustc's scope rules change by edition. Mitigation: a port of `scope.rs`, the
  synthetic corpus's drop programs, MIR at `built` compared.
- **Constant evaluation's reach.** Std's const fns use intrinsics, raw pointers and `const_eval_select`; an interpreter without
  validity checks must still give rustc's bytes. Mitigation: the `consts` judge; RFS9 (b) if the clean interpreter stalls.
- **Hygiene.** `macro_rules!` mixed-site and std's `macro` 2.0 def-site hygiene interact with resolution; errors show as
  wrong resolutions, caught by RF5's judge.
- **Tier S's drift.** std's unstable surface is tied to 1.99.1; a move of the pin moves Tier S with it (as it moves the
  parser, whose port follows rustc by diff). Tier U depends only on the helper's dumps, regenerated at a new pin.
- **Speed.** The native reader runs at about 3,300 lines a second evaluated; type checking is heavier. Tier U's programs are
  small but std's data is not; Tier S on the evaluated host would take hours. Only the compiled build is realistic for Tier S
  (RF14).
- **Proc macros.** The ecosystem leans on them (§2.3); Tier P's protocol needs Arbace's own `proc_macro` to behave as rustc's
  (spans, hygiene of generated tokens: `Span::call_site`, `mixed_site`, `def_site`). Until RF13 a program using one is classed.
- **The judge's normalizers hide bugs** if too liberal: they are checked on rustc's own output (two rustc runs with different
  crate names equal after normalization), the controls fail by design, the run is the last word.

## Proposed (RFS1-RFS13)

For the user's decision; recommendations only.

- **RFS1 The route.**
  - Options: (a) R1, rustc's analysis ported whole with its checks: 300 to 400 days; (b) **R2, rustc's accepting paths ported,
    trusting, in tiers B, U, S, P**: Tier U 94 to 146 days, 145 to 233 in all; (c) R3, a clean front end from the Reference:
    similar days, no agreement by construction; (d) R4, rustc's front end on rsi: 20 to 40 days after RE6, a seed; (e) R5,
    mrustc on cppi: not rustc's MIR, not 1.99.1's std.
  - *Recommended: (b)*, Tier B and Tier U first, Tier S decided at RF11; (d) measured as an oracle (RFS11).
- **RFS2 The pivot and the target IR.**
  - Options: (a) **MIR forms as the front end's output, codegen MIR to LLVM forms for rsi**: rsi, std's IR, the lowering, ci and
    cf2g unchanged, the back half judged exactly on rustc's MIR; (b) MIR to C forms directly (mrustc's `codegen_c` shape): one
    layer fewer, no exact judge, its own unwinding lowering; (c) MIR forms interpreted (RE10's interpreter) for eval, LLVM forms
    for AOT: two back ends. Variant of (a): port the `-O0` MIR optimizations too (about 4,000 lines, 3 days) so the whole
    pipeline's IR equals rustc's byte for byte after normalization.
  - *Recommended: (a)*, without the variant at first (the MIR at `runtime` and the run judge the full pipeline); the variant if
    IR equality proves worth it for the nightly.
- **RFS3 The order.**
  - Options: (a) **RF1-RF3 (the back half on rustc's MIR) first, with RF4, RF5 and RF7 beside them** (judged on data, no rsi);
    RF8-RF10 after; (b) the front half first, the back half last; (c) Tier S's surface first.
  - *Recommended: (a)*: the back half's judge is exact and carries 96% of a program's IR (§2.4); expansion and resolution are
    judged by data.
- **RFS4 std in Tier U.**
  - Options: (a) **std's interface and MIR library dumped by the helper at the pin, as forms per item, pinned by digest**; (b) a
    native `.rmeta` reader (a format of every release, about 11,600 lines of `rustc_metadata`); (c) std's signatures from source
    (expansion, resolution, collection, no bodies), its MIR from the helper: a step toward Tier S.
  - *Recommended: (a)*; (c) as Tier S's first step.
- **RFS5 What the trusting front end leaves out.**
  - Options: (a) **regions erased at type lowering; no borrowck, no exhaustiveness, no lints, no well-formedness, coherence or
    privacy checks; the read level's rejections stay rustc's (the helper's)**; a program rustc rejects is outside the front end's
    contract; (b) the same, with the checks added later as a level of their own (porting borrowck and `pattern_analysis`:
    about 42,000 lines); (c) checks from the start.
  - *Recommended: (a)*, (b) a later decision.
- **RFS6 The trait solver.**
  - Options: (a) **port `rustc_next_trait_solver` with `rustc_type_ir`'s generic parts behind an `Interner` over Arbace's types,
    rust-analyzer as the guide, if RF1 finds `-Znext-solver=globally` gives the same MIR on Tier U's subset** (73 of 73 in this
    survey's probe); (b) port the old solver (what 1.99.1 type-checks with); (c) a clean SLD solver for Tier U, a port later.
  - *Recommended: (a)*, (b) if RF1 finds divergences that matter.
- **RFS7 Generic sharing.**
  - Options: (a) **every generic instantiated in the program's unit (`-Zshare-generics=no`'s behaviour), the judge's reference IR
    made with that flag**; (b) call std's exported instances as `-O0`'s default does, from a list in std's interface.
  - *Recommended: (a)*: simpler, exact with the flag; the program's unit grows by a few percent (§2.4: 280 functions against 266).
- **RFS8 Proc macros.**
  - Options: (a) **in Tier P, mrustc's shape: the proc-macro crate compiled by the front end with Arbace's own `proc_macro` crate,
    run on rsi over a token-stream text protocol**; (b) rustc's bridge served in process (needs ci-to-host callbacks);
    (c) the oracle's expansion recorded as data (not standalone); (d) never, a class.
  - *Recommended: (d) until RF11, then (a)*.
- **RFS9 Constant evaluation.**
  - Options: (a) **a clean interpreter over MIR forms with a flat memory of allocations and provenance, the const intrinsics
    ported**, also RE10's interpreter's seed; (b) a port of `rustc_const_eval/interpret` less validity; (c) constants evaluated at
    run time (wrong for array lengths and const generics).
  - *Recommended: (a)*, (b) where the `consts` judge shows it.
- **RFS10 Tier S's decision point.**
  - Options: (a) **decide at RF11 between Tier S (35 to 60 days) and keeping std as data, with RFS11's measurement in hand**;
    (b) commit to Tier S now; (c) Tier U only.
  - *Recommended: (a)*.
- **RFS11 rustc's front end on rsi (R4) as an oracle or a fallback.**
  - Options: (a) **measure after RE6 (AOT) and RES34: the helper without LLVM compiled to IR and run on ci for one small program,
    its stage dumps equal to the native helper's**; (b) adopt it instead of Tier S; (c) never.
  - *Recommended: (a)*; the choice between (b) and Tier S is the user's after Tier U exists.
- **RFS12 Reuse and licenses.**
  - Options: (a) **rustc's text ported under MIT as the native reader's is (`LICENSE.md`'s "Rust (rustc 1.99.1)" section;
    origins per module in `doc/ARBACE.md`); rust-analyzer and mrustc read as design, not copied; gccrs never read (GPL-3)**;
    (b) also port rust-analyzer's pieces (MIT or Apache-2.0) where they are smaller than rustc's (its MIR interpreter for CTFE);
    (c) rustc only, nothing else read.
  - *Recommended: (a)*; (b) revisited if RF10's CTFE stalls.
- **RFS13 Where the front end lives.**
  - Options: (a) **`arbace/rust/front/` (`arbace.rust.front.expand`, `.resolve`, `.hir`, `.collect`, `.solve`, `.typeck`, `.mir`,
    `.ctfe`) and `arbace/rust/back/` (`.mono`, `.layout`, `.abi`, `.codegen`) beside rsi's `arbace.rust.*`, evaluated from the
    checkout, out of `gate_key`; `bin/rsi run|check --front mir|arbace|rustc` (rustc the default until RF11)**; (b) under
    `arbace.repr.rust` (repr is read and print, not analysis); (c) a crop of its own command.
  - *Recommended: (a)*.

What to decide first: RFS1 (the route) and RFS3 (the order); RFS2, RFS4 and RFS7 follow. RF1-RF3 are a cheap trial with no
commitment to the front half (25 to 40 days): the back half is useful alone and its exact judge re-estimates the rest.

## Decided (2026-11-05, the user)

RFS1-RFS13 as recommended: RFS1 (b), rustc's code ported for the paths programs it accepts take, in tiers; MIR forms out, LLVM forms the target; Tier B first with expansion and resolution beside it; std's interface and MIR as helper data in Tier U; no lifetimes, borrowck, exhaustiveness or lints; the new trait solver if RF1's census confirms it; every generic instantiated locally; proc macros a class until RF11, then mrustc's token protocol on rsi; a clean MIR interpreter for constants; Tier S decided at RF11; rustc's front end on rsi measured as an oracle only; rustc's text ported under MIT, rust-analyzer and mrustc read as design, gccrs never; the code in arbace/rust/front and arbace/rust/back, out of gate_key. RF1-RF3 start as a trial.


## As built: RF1-RF3 (trial, 2026-11-05)

The trial the user asked for (RFS1-RFS13 decided as recommended): RF1's oracles and census, RF2's MIR forms, and a first slice of RF3
(the code generation of the back half), one day of work on the branch `rust-rf13`. Code in `arbace/rust/front/` and
`arbace/rust/back/` (evaluated from the checkout, out of `gate_key`, RFS13), the helper's new modes in `tools/rsdump/mirdata.rs`,
the command `bin/rust-front` (`make rust-front-*`), samples in `test/rust/front/`, the spec `doc/rust/MIR.md`. Ports of rustc 1.99.1
(`58cf8f9c3`, the apks `tools/rsdump/apks.sha256` pins) are recorded in `LICENSE.md` and `doc/ARBACE.md`. No gate step, no corpus
run beyond the 81 programs of the synthetic corpus, two samples, rsi's five and the seven of `test/rust/front/`, each process
under 2 GiB.

### RF1: the oracles and the census

`rsdump rustc` (the helper on the host's rustc 1.99.1, `bin/repr-rust helper`) gained, beside `--hir` and `--mir` (RR6):

- `--mir-data FILE`: the crate's MIR bodies as forms, in the order and with the choices of `write_mir_pretty`
  (runtime-optimized MIR, promoteds, the CTFE body of a `const fn`), with the locals' layouts (`layout_of`: size, alignment, backend
  representation, field offsets, variants with their tags and niches, discriminants), each body's allocations (`:alloc-data`:
  bytes, initialization, relocations, functions, statics, vtables) and its `track_caller` and spread-argument facts. Spec:
  `doc/rust/MIR.md`.
- `--mono-mir FILE` (the oracle of RF3's collector, resolution, symbols and layouts): the mono items rustc collects
  (`collect_and_partition_mono_items`), each function instance's monomorphic MIR (`instance_mir` instantiated at its arguments)
  under its symbol, every call's callee resolved (`Instance::try_resolve`: kind, v0 symbol, `can_unwind`, `track_caller`, intrinsic
  name), drop glue symbols, unevaluated constants evaluated, function items' symbols, unsizing casts' vtables, the lang items'
  symbols (the panic functions, the personality) and the entry wrapper's facts.
- `--census FILE` (the enabled features, the lang items and intrinsics called, the rvalue and terminator kinds) and `--iface FILE`
  (the crate's collected items: kind, generics, signatures and types, ADTs' variants, impls' trait references; std's interface
  stays RR's rustdoc JSON).

`bin/rust-front oracles|corpus` runs them over the synthetic corpus (67 programs), `test/rust/sample/` (8) and `test/rust/eval/`
(5), 80 files, **77 built** (the three left out: `constructs.rs` needs
`#![feature(stmt_expr_attributes)]`, `edition2015.rs` is not dyn-compatible at its edition, `lowered_table.rs` is not a crate).
Census: no program of the 77 enables a feature; the crates' own bodies (not std's generics) call 15 lang items (the iterator's
`next`/`into_iter`, `PanicFmt`, `CloneFn`, `TryTrait*`, `RangeInclusiveNew`, `FromFn`, `SliceLen`, `Panic`), 1 intrinsic and use
613 drops, 99 pointer coercions and 20 closures. **The new solver agrees**: `-Znext-solver=globally` gives the same MIR text on all
77 (RFS6's condition holds for this corpus, as the survey's probe did for 73). The mono items (`-Zprint-mono-items`, 74 programs
that build with the default flags): 11,406 in all, 9 (`trap`) to 1,664 (`collections`), median 44, 21,402 of the path components
name `std`: **a program of this corpus is its handful of own functions plus std's generics instantiated for it**, so Tier B has
to take std's MIR as it takes the program's. The intrinsics the monomorphic bodies still call (after MIR lowering), by calls:
`cold_path` 583, `black_box` 233, `rotate_left` 208, `ctpop` 147, `ptr_offset_from_unsigned` 144, `assert_inhabited` 123, `ctlz` 115,
`abort` 113, `saturating_add` 103, `arith_offset` 102, `size_of_val` 82, `copy` 77, `simd_*` (about 190), `catch_unwind` 13.
The two judges' texts: the helper's `--mir` writes paths in full, rustc's `-Zunpretty=mir` trims them; 73 of 74 programs'
texts agree once module prefixes are removed (the other differs in a closure's name).

### RF2: MIR forms

`doc/rust/MIR.md` (the spec), `arbace.rust.front.mir` (the reader of the dump and `print-dump`/`print-body`, after
`rustc_middle/src/mir/pretty.rs`) and `bin/rust-front mir-check`: **79 of 79 programs' bodies, read from the helper's forms and
printed back, equal the helper's `--mir` text**, byte for byte (the three header lines, every separator, `// MIR FOR CTFE`,
promoteds, the allocations' text), the negative control (one program's bodies against another's text) differing. What the
printer does not yet own: the text of types and of constants (rustc's renderings travel in the forms next to their structure,
`(ty "TEXT" STRUCT)`, `(const "TEXT" T VALUE)`) and the allocations' bytes display. The phases before `runtime-optimized` (`built`,
`promoted`, `drops-elaborated`) are not dumped as data (their queries are stolen by later passes in a `rustc_driver` run): RF10
compares those through `-Zdump-mir`'s text.

### RF3: a first slice of the back half

`arbace.rust.back.codegen` (about 1,500 lines): MIR forms to LLVM forms (`arbace.repr.llvm`), after `rustc_codegen_ssa/src/mir`:
the SSA analysis of locals (`analyze.rs`: which locals need an alloca, dominators, the order of allocation), the order and merging
of blocks (reverse postorder, last successor first, mono reachability of constant switches; a block with one successor that has
one predecessor merges; blocks created when first referenced; unreached blocks as `unreachable`), operands (immediates, scalar
pairs, memory aggregates by reference), places (fields at the layout's offsets, downcasts, derefs, indexes), the rvalues
(arithmetic, checked arithmetic with rustc's lowering to `llvm.*.with.overflow` or `add` and `icmp ult`, unchecked arithmetic with
`nuw`/`nsw`, shifts masked and folded, comparisons, casts, borrows, aggregates of structs, tuples, arrays and enums,
discriminants with direct and niche tags, vtables of unsizings, function pointers, constants held in allocations), the
terminators (switch in all its forms, calls and `invoke`s with landing pads, the personality slot, `resume`, terminate blocks,
nop landing pads as `find_noop_landing_pads` finds them, drops through the glue's symbol, asserts with the panic functions and
`Location` constants, `#[track_caller]` parameters and arguments through the inlined scopes, spread `rust-call` arguments,
indirect calls), the Rust ABI of scalars, pairs, small memory aggregates (cast to an integer, through a scratch alloca when
under-aligned) and large ones (by pointer, `sret` returns, `memcpy`), return and parameter attributes, constants interned by
content (strings, byte slices, vtables, locations), the constant folding LLVM's builder does, and the intrinsics `abort`,
`black_box`, `cold_path`, the inhabitedness assertions, the bit intrinsics (`ctpop`, `ctlz`, `cttz`, `bswap`, rotates, funnel
shifts, saturating arithmetic), `ptr_offset_from`, `write_bytes` and the float math ones; floats (constants in LLVM's text,
arithmetic, comparisons, conversions with the saturating intrinsics). **Not taken yet**: SIMD and the vector ABI, `i128` in
pairs, `Offset` of aggregates (LLVM's named struct types), arrays of aggregates, unsized places, virtual calls, `size_of_val`,
the rest of the intrinsics, coroutines, inline assembly.

`arbace.rust.back.collect` (the collector slice, about 100 lines): the mono items of a program from its roots (the entry's
`main` and `lang_start`; for `no_std` programs the functions with plain symbols and the panic handler) by what the bodies use:
callees (the helper's resolved instances), drop glue, function items and pointers (a closure coerced to a pointer too), the
functions and statics of the allocations the constants reach (vtables' methods among them), the panic functions the asserts call, the
personality and terminate functions unwinding needs; after `rustc_monomorphize/src/collector.rs`. **all 81 programs' item sets equal
rustc's** (`bin/rust-front collect`; the first run missed 6 programs by one to six items each: closures coerced to function
pointers, a function item held in a local). The instances
themselves are the helper's (`Instance::try_resolve`): of the 23,287 calls of function items in the corpus's monomorphic bodies
(`:callee` facts), 15,813 resolve to the very item named (nothing to resolve), 4,413 to another definition (an impl's method or a
trait's default method: impl selection, RF8's work), 2,809 are intrinsics, 228 drop glue (167 empty), 24 virtual calls.

What it does **not** do: the layouts, the instance resolution, the symbols and the vtables' contents are rustc's (the helper's data);
porting those is the rest of RF3 (`layout`, `abi`, resolution, v0 names). The judge needs none of them to be Arbace's to be exact
about the code generation and the collection.

The judge (`arbace.rust.back.check`, `arbace.rust.back.coverage`, `bin/rust-front codegen-check|coverage`): rustc's `-O0
-Zshare-generics=no -C no-prepopulate-passes` IR of the same program, read by repr/llvm, each function's forms against ours after a
normalizer (values and blocks renamed by first occurrence, symbols by their v0 path, constants by their content, attribute groups,
metadata and linkage dropped), and the global constants as a set; a negative control (one function's forms against another
function's IR) must differ. `-C no-prepopulate-passes` is part of the oracle: at `-O0` LLVM still runs the always-inliner, so the
final IR of an `#[inline(always)]` callee is inlined into its callers (the first coverage run found it).

Results (`test/rust/front/coverage-2026-11-05.edn`, `.tmp/rust-front/mono/*.coverage.edn` per program):

- the six `no_std` samples `a1`-`a5`, `a7` (integers, structs, tuples, enums, arrays, overflow checks, panics, large aggregates;
  `bin/rust-front codegen-check`): **every function equal**, and the programs built from our IR (`bin/rsi run --front mir`: the LLVM forms
  lowered by rsi's lowering to C, gcc `-O0`) give the exit statuses rustc's executables give (5, 212, 41, 17, 45); `a6` (drops,
  unwinding, as a monomorphic dump) 9 of 11;
- the corpus: **8,342 of 11,453 mono items' functions equal rustc's IR (72.8 %)** over the 81 programs, **13 programs with every
  item equal** (the six samples, `abort`, `exit-code`, `panic-assert`, `panic-assert-eq`, `panic-todo`, `panic-unwrap-err`, `trap`). The progress of the
  day: the scalar subset 391 of 4,526 (the first run; nine programs were not readable then), then aggregates, enums, unwinding,
  `track_caller` and the shims, then folding, constants in memory, indirect aggregates and nop landing pads (62.1 %), then the bit intrinsics, floats, pointer
  constants, partly uninitialized allocations, constant GEPs into globals and the pointer attributes of boxes (72.8 %);
- **runs**: seven of those synthetic programs (`abort` 134, `exit-code` 37, `panic-assert`, `panic-assert-eq`, `panic-todo`,
  `panic-unwrap-err` 101, `trap` 132) built with their own unit made of our IR, linked with the host's native std as rsi's native-std leg links the oracle's
  (`bin/rust-front run-native`, `RSI_FRONT_IR` in `bin/lib/rsi/link`), give rustc's status, stdout and stderr (thread ids
  normalized). These print through `std::io::_print`, exit, trap and panic with a message: the first synthetic programs run from
  Arbace's code generation. rsi's ci leg and its lowered std (`runtime-ci`, minutes and gigabytes) were not run: the day's rule.
- what blocks the rest, by items (the last run's refusals, `test/rust/front/coverage-2026-11-05.edn`): `Offset` of non-scalar
  pointees 465 (rustc types them as named structs or byte arrays by rules not ported), an ABI the slice does not take (`abi other`:
  vectors) 234, arrays of non-scalars 91, `size_of_val` 75, `BoxDerefTransmute` 55, unsizing of other pointers 53, `arith_offset`
  42, `repeat` 40, a tail; 1,560 items differ (classes seen: an extra alloca or block where rustc's analysis differs, parameter
  attributes of newtype pointers, `nuw` flags on some arithmetic) and 209 raise errors (bugs of the slice, counted by item).

Findings worth keeping (the surprises of the first runs): the oracle IR needs `-C no-prepopulate-passes`; unsigned `AddWithOverflow`
is `add` and `icmp ult res, lhs`, unsigned `SubWithOverflow` `sub` and `icmp ult lhs, rhs`, the other four use the intrinsics;
allocas appear in the reverse of their allocation (arguments, then `_0`, then the rest) and LLVM blocks in the order they are first
referenced; an invoke continues in its normal destination, which is where the result is stored; `#[track_caller]` is an extra
last parameter and `get_caller_location` walks the inlined scopes (the dump carries `inlined_parent_scope`); a `rust-call`
function's spread tuple argument is a place of its own even when zero-sized; a cleanup block is a landing pad only when
reached from a non-cleanup block, and a cleanup block that only resumes (through trivial statements, switches on drop flags and
empty drops) is no pad at all; aggregates over a pointer's size travel by pointer, returns by `sret`; constants LLVM's builder
can fold (shifts, comparisons, int-to-pointer) never reach the IR as instructions.

### Estimates, revisited

RF1 (6-9 days planned): the modes the table names for RF3's judging are in (`mir`, `iface`, `layout` as the layouts table, the
mono-items run, the census); `expanded`, `resolve`, `typeck` and `consts` (RF4-RF10's oracles) are not built. RF2 (3-5): done
in under a day, the spec, reader, printer and check. RF3 (16-26 days): the first day took the code generation from nothing to 72.8 %
of the mono items of 81 programs, the collector's traversal to all 81 item sets, and seven programs to running; what is left of the 16 to 26 is the remaining codegen
(LLVM's types of aggregates for `Offset` and arrays, vectors, virtual calls, `size_of_val`, the intrinsics' tail, the classes of the
1,560 items that differ: about 3 to 5 days by the refusal counts), then the pieces the helper still answers: layout (`rustc_abi`, about 2,500 lines), the collector and instance
resolution with specialization, v0 symbols (the crates' disambiguator hashes), vtable and drop glue and shim MIR from data
(about 8 to 12 days). The estimate stands; the exactness of the judge is what the day showed.

### Proposed (RF-trial)

- **RFP1 What next in RF3.** Options: (a) **finish the codegen on rustc's data first (memory aggregates, floats, the intrinsics,
  virtual calls, constants in memory) to reach most of the corpus's mono items, then replace the oracle's answers one at a time (layout,
  then the collector with resolution, then symbols), each judged against the helper's**; (b) port layout and the collector first, with
  the codegen at today's 73 %; (c) stop at the trial. *Recommended: (a)*: every step stays judged exactly and the program runs grow
  with it.
- **RFP2 Running std.** Options: (a) **keep the native-std leg for RF3's runs (the unit replaced, std native) and the ci leg after the
  lowered std exists at the checkout's key (RE3/RE4, heavy)**; (b) build the lowered std in a worktree for the trial's runs. *Recommended:
  (a)* until the night's budget has a place for it.
- **RFP3 Where `--front mir` ends.** Options: (a) **`bin/rsi run --front mir FILE` as built (no_std programs through the lowering and gcc;
  `bin/rust-front run-native` for std programs through the native leg), the ci leg when RFP2's runtime exists**; (b) one command with
  both legs now. *Recommended: (a)*.
- **RFP4 The gate.** Options: (a) **none until the row fits GT8's budget (as rsi, RES14 (a)); `make rust-front-mir-check` and
  `rust-front-codegen-check` are seconds and light, the corpus coverage about ten minutes**; (b) the two light checks as essential
  steps now. *Recommended: (a)*.

## Decided (2026-11-05, the user: RFP1-RFP4)

All as recommended: the codegen finished on rustc's data first, then layout, the collector with resolution and v0 symbols ported, each judged against the helper; the native-std leg now, the ci leg once the lowered std exists; `bin/rsi run --front mir` as built; no gate step until the row fits GT8's budget. The branch's early commits without trailers are squashed into commits that carry them at its merge (no rewrite of the branch).

## As built: RF4-RF5 start (2026-11-05)

The front half's first stages beside Tier B (RFS3 (a)), one afternoon on the branch `rust-rf4` (from `rust-rf13`; `rust-native`, the
native reader, was already its ancestor: nothing to merge). Code in `arbace/rust/front/` (RFS13: evaluated from the checkout, out of
`gate_key`), the helper's new modes in `tools/rsdump/front.rs`, the verbs `bin/rust-front front-oracles|expand-check|resolve-check`
(`make rust-front-oracles`, `rust-front-expand-check`, `rust-front-resolve-check`), three samples `test/rust/front/expand1.rs`,
`expand2.rs` and `resolve1.rs`. Ports of rustc 1.99.1 (`58cf8f9c3`) under MIT (RFS12), recorded in `LICENSE.md` and `doc/ARBACE.md`; rust-analyzer and
mrustc not read for this slice. Light: no gate step, no corpus beyond the 90 files of the synthetic corpus, `test/rust/sample`,
`test/rust/eval` and `test/rust/front`; each check about 20 s and 0.5 GB.

### The oracles (`tools/rsdump/front.rs`)

`rsdump rustc` gained three modes:

- `--expanded FILE`: the crate after expansion (`tcx.resolver_for_lowering()`'s crate, in `after_expansion`), written by the same
  transcoding of rustc's `Debug` as the native reader's dumps; spans of other files (std's macros, `include!`d files) as
  `(span "FILE" l1 c1 l2 c2 ctxt)`, and `FormatArgs`' name maps (string keys) taken. The judge of RF4.
- `--resolve FILE`: every HIR path written in the root file and not from an expansion (`!span.from_expansion()`, so the paths of a
  user's tokens, also inside macro calls), with its `Res`: another crate's item by its def path (`(def "Fn" "core::hint::black_box")`),
  the crate's own by its identifier's span (`(local-def "Struct" [l c l c])`, no def-path disambiguators needed), locals by their
  binding's span, primitives, `Self` (by the impl's or trait's start), attributes, `Err`; a `use` path's resolution in each namespace.
  The judge of RF5. Traits in scope per method call are not dumped yet.
- `--std-names FILE` (RFS4: std's interface as helper data): std's, core's and alloc's module trees as metadata gives them
  (`module_children`, public children, each module once: 532 modules), and their 97 public bang macros' definitions as token trees
  (`CStore::load_macro_untracked`), builtin or not. Deterministic, 4 MB, pinned by `test/rust/front/std-names.sha256`; regenerated by
  `bin/rust-front front-oracles` into `.tmp/rust-front/front/`, not committed.

### RF4: expansion (`arbace.rust.front.expand`)

Over the native reader's trees (`arbace.repr.rust.parse` and its dump writer): the root file's tokens are kept as `Tok`s with their
positions, so a user's tokens keep their spans through any expansion; a macro definition's tokens are copies marked with the
expansion's mark (the syntax context their identifiers carry), remembering the root position they came from. An expansion's output is
assembled into one token array over the root file's text with the foreign tokens' text appended, and parsed by the native parser as the
fragment its position asks (expression, statements, items, pattern, type); a parsed fragment (`$e:expr` ...) stands in that array as a
placeholder identifier and is put back after the parse, as rustc's invisible delimiters keep it whole. What is taken:

- `macro_rules!` (and macros 2.0 arms, `,`-separated), after `rustc_expand/src/mbe/`: matchers with nested repetitions, separators,
  `*`/`+`/`?`, every fragment kind (`tt`, `ident`, `lifetime`, `literal`, `vis`, `expr`, `ty`, `pat`, `pat_param`, `path`, `block`,
  `stmt`, `item`; `pat` without a top-level guard), the first arm that matches all of the input, transcription with `$crate`, the
  definition's tokens `Alone` and `]` closing `Alone` as `transcribe.rs` does, a fragment left in a token stream (a macro a macro
  defines) as rustc's invisible group; user macros by name, those a macro's expansion defines from there on (textual scoping otherwise
  simplified to the crate's definitions, the last of a name), std's from `--std-names`;
- the builtins the corpus uses: `format_args!`, `format_args_nl!`, `const_format_args!` (`rustc_parse_format`'s grammar and
  `make_format_args`: implicit, numbered, named and captured arguments, width, precision and `*`, fill, alignment, sign, `#`, `0`,
  `x?`/`X?`, the traits, literals merged, the spans inside the literal), `assert!` (with and without a message), `panic!` and
  `unreachable!` by edition (`panic_2021`/`unreachable_2021`), `stringify!` and `concat!` (escaped as `expr_str` writes them),
  `line!` and `column!` (at the outermost call site, through the marks), `file!`, `cfg!`, `include!` (an expression file); std's
  `dbg!`, `matches!`, `write!`, `vec!`, `assert_eq!` ... as macro_rules!; the prelude's injection (`extern crate std`/`core`, `#[prelude_import] use ...::rust_20xx::*`,
  edition 2015's `::std`);
- the derives `Debug`, `Clone` (with `Copy`'s `TrivialClone` shortcut), `Copy`, `PartialEq` (`StructuralPartialEq`, discriminants),
  `Eq`, `PartialOrd` (with `Ord`'s shortcut), `Ord`, `Hash`, `Default` (an enum's `#[default]`), on structs (named, tuple, unit) and
  enums, `PartialEq`'s scalar fields first (`Ty::maybe_scalar`), generic types bounded per parameter after their own bounds, the
  item's where predicates, written as Rust text read by the native parser with the fields' types as fragments, then the builder's
  differences applied (expression statements in `Hash`, no colon spans, compiler-generated `unsafe`, the item's generics' and the
  derive path's spans, the type parameters the item's own tokens);
- `#[cfg]` and `#[cfg_attr]` on items, fields, variants, statements, arms and every attributed element of a vector (StripUnconfigured,
  also over a macro's output and a derive's input), evaluated for the host's target and left as their `CfgTrace`/`CfgAttrTrace`;
- a statement macro's semicolon as `expand.rs` and the parser apply it (an expression statement takes it; after a statement that already
  has one, the call's `;` stays as an empty statement).

**Results** (`bin/rust-front expand-check`): **89 of 89 crates equal to rustc's expanded crate, 759 of 759 items**, at the tree level
(spans and syntax contexts set aside, as `native-check` compares parse trees); the negative control (one crate's expansion against
another's oracle) differs. The second view, `EXPAND_SPANS=1`, keeps the root file's spans at context 0: 86 of 89 crates, 722 of 759
items (the classes left: an empty generics span after a fragment identifier in a macro's output is the root's here and has the
expansion's context in rustc; a derived impl's `Id<T>` has the parameter's context in rustc's tree where ours keeps the root's, though
rustc's HIR reports that path at the root's span, as ours does). The first run of the day's code: 65 of 87; then the derives, `cfg`,
`include!`, fragments through nested macros (a fragment's placeholder carried through a nested call's token stream) and the statement
macros' semicolon.

What is not taken yet: hygiene beyond marks (one mark per expansion, a local visible to identifiers of its own mark: `macro_rules!`'
mixed-site behaviour for locals; no transparency levels, no `$crate` by context, no opaque `macro` def-site resolution), macro paths
resolved by resolution (`#[macro_use]`, `$crate::m!` by its crate, a name's textual shadowing before its last definition), packed
structs and unions in derives, const generics in derived impls, the rest of the builtins (`env!`, `option_env!`, `include_str!`,
`include_bytes!`, `module_path!`, `compile_error!`, `concat_idents!`, `cfg_select!`, `global_asm!`/`asm!`), attribute macros and
`#[test]`, proc macros (RFS8's class until RF11), `-Zunpretty=expanded`'s text (the survey's second view; it needs
`rustc_ast_pretty`, RFP5).

### RF5: resolution (`arbace.rust.front.resolve`)

After `rustc_resolve`'s `build_reduced_graph.rs`, `imports.rs`, `ident.rs` and `late.rs`, over an expanded crate in the dump format (the
helper's or ours): the def collector (modules, inline `mod`, block items as anonymous modules, enums' variants and constructors,
traits' items, generics), imports (simple, renamed, nested trees, `self`, globs) to a fixpoint, the preludes in rustc's order (the
`#[prelude_import]` glob from std's names, the extern prelude, builtin types, tool and builtin attributes), std's tree from
`--std-names` (re-exports followed by the canonical path), late resolution with ribs for locals (patterns, closures, `for`, `if let`,
`while let`, let-else), generic parameters, `Self`, identifier patterns that name unit structs, variants or constants, partial
resolutions reported at their resolved prefix as HIR's type-relative paths are, and locals' hygiene by the identifier's context.
`resolve-crate` takes the crate and std's names (and the root file's text, which places definitions whose identifiers the dump writes
without spans; identifiers of a macro's output carry their span, `(id NAME CTXT [l c l c])`).

**Results** (`bin/rust-front resolve-check`): over the helper's expanded crates **4,948 of 4,948 paths of 87 programs equal**, nothing
missing or extra; and **the pipeline** (`resolve-check --ours`: the native reader, our expansion, our resolution) **4,948 of 4,948 paths,
87 of 87 programs**; the negative control differs. (`constructs`, `edition2015` and `lowered_table` have no resolution oracle: rustc stops
on them after expansion or they are no crate.) What rustc's HIR reports, found on the way: `&self`'s implicit `Self` at the `self` token;
a parameter's bound as a path at its identifier; `impl Trait` arguments as synthetic type parameters; `e?`'s desugared local; the leaves of
nested use trees, not their stem; a derived enum `Default`'s `Self::Variant` reported at the variant's identifier, its `Self` the impl's
(the derive path's) start. Not taken yet: privacy, `pub(in path)`, out-of-line `mod m;` files, `<T as Trait>::f`'s trait part,
the crate root's items seen from nested modules without a path (an approximation the corpus does not exercise), traits in scope.

### Estimates, revisited

RF4 (8-12 days) and RF5 (8-12 days) were planned against the corpora and the compiler corpus's crates. This slice took both to all of
the synthetic corpus and samples in one afternoon (about 1,300 lines of `.ae` for expansion, 750 for resolution, 240 of helper), by
ports of the paths these programs take: what the corpus does not exercise (listed above) is most of the rest, and the compiler corpus,
written with every macro feature, is where hygiene, macro paths' resolution and `cfg` everywhere will be needed. The estimates stand for
that corpus; the trial says the judges are exact and cheap.

### Proposed (RF4-RF5)

- **RFP5 The expansion's second view.** Options: (a) port `rustc_ast_pretty` (`pp.rs` and `pprust/state*`, about 3,500 lines of rustc,
  1,200 of `.ae`, 2-3 days) and compare `-Zunpretty=expanded`'s text byte for byte; (b) **keep the tree level and the root spans' view
  (`EXPAND_SPANS=1`) now, and port the pretty printer with RF6's `rustc_hir_pretty` (§3.4), which shares `pp.rs`**. *Recommended: (b)*.
- **RFP6 Where std's macros come from.** Options: (a) **the helper's metadata (`load_macro_untracked`, as built), pinned by
  `test/rust/front/std-names.sha256`**; (b) rust-src's library sources read by the native reader. *Recommended: (a)* (RFS4: what
  rustc itself expands, with no source paths to follow).
- **RFP7 The next corpus.** Options: (a) **rustc's UI suite's run-pass programs through `front-oracles`, `expand-check` and
  `resolve-check` once in a night shift (light per file, long in all), the differences recorded as classes**; (b) the compiler corpus's
  crates (Cargo, `--extern`s, every feature; RF4's and RF5's planned judges); (c) stay on the synthetic corpus until RF6. *Recommended:
  (a)*, then (b) when hygiene and macro paths are taken.
- **RFP8 Hygiene.** Options: (a) **port `rustc_span::hygiene`'s syntax contexts and transparencies when a program of RFP7's corpus needs
  them (marks as built until then)**; (b) port them now. *Recommended: (a)*: the corpus is equal with marks.
- **RFP9 What next.** Options: (a) **RF6 (AST to HIR) on these crates, RF4's and RF5's remaining pieces as RFP7's corpus asks**; (b)
  finish RF4 and RF5 against the compiler corpus first. *Recommended: (a)*: every stage stays judged exactly and the pipeline grows
  toward Tier U's MIR.

## Decided (2026-11-05, the user: RFP5-RFP9)

All as recommended: the -Zunpretty=expanded text compared later with RF6's pretty printers; std's macros from the helper's metadata; the UI run-pass programs through the checks in a night, then the compiler corpus; full hygiene when that corpus needs it; RF6 (AST to HIR) next.

## As built: RF6 (2026-11-05)

AST to HIR and rustc's HIR printer, one afternoon on the branch `rust-rf6` (from `rust-rf4`), front half only (`rust-rf3b` carries RF3's
back half). Code in `arbace/rust/front/` (RFS13: evaluated from the checkout, out of `gate_key`): `pp.ae` (rustc_ast_pretty's `pp.rs`
and `pp/ring.rs`: Oppen's printer as rustc keeps it, margin 78, and `pprust/state.rs`'s `gather_comments`), `lower.ae` (rustc_ast_lowering),
`hir_pretty.ae` (rustc_hir_pretty with the PrintState it inherits), `hir_check.ae` (the judge); the verb `bin/rust-front hir-check
[--ours]` (`make rust-front-hir-check`); the sample `test/rust/front/lower1.rs`. Ports of rustc 1.99.1 (`58cf8f9c3`) under MIT (RFS12),
recorded in `LICENSE.md` and `doc/ARBACE.md`. Light: no gate step, the 91 files of the synthetic corpus, `test/rust/sample`,
`test/rust/eval` and `test/rust/front`; the whole check about 5 s over the helper's crates, 30 s through the pipeline, under 1 GB.

### The oracle

`bin/rust-front front-oracles` also writes `NAME.hir.txt`: rustc's `-Zunpretty=hir` text, by the helper run as rustc (`rsdump rustc --
... -Zunpretty=hir`), the same driver, flags and pin as the `expanded` and `resolve` modes (no new helper code, no rebuild). 89 files
have one (`constructs` and `lowered_table` stop before HIR, as before). Paths in the text (`file!()`, the cfg traces' spans) are the
absolute paths `front-oracles` passes; the judge gives the lowering the same path.

### RF6: lowering (`arbace.rust.front.lower`)

`(lower-crate CRATE RES opts)`: over an expanded crate in the dump format (the helper's or `arbace.rust.front.expand`'s) and the
resolutions by span (the helper's `--resolve` or `arbace.rust.front.resolve`'s), the HIR as data: maps with `:node`, `:kind`, the AST's
span and `:hir-id [OWNER LOCAL]` (each item and associated item an owner, its nodes numbered in lowering order; not judged: the text does
not show them, `-Zunpretty=hir-tree` would), paths with their `:res`. After `lib.rs`, `item.rs`, `expr.rs`, `pat.rs`, `block.rs` and
`format.rs`:

- the desugarings: `while` and `while let` to `loop { if .. { .. } else { break; } }`, `for` to `match into_iter(e) { mut iter => loop
  { match next(&mut iter) { None {} => break, Some { 0: pat } => body } } }` in `DropTemps`, `?` to `match branch(e) { Break { 0: residual }
  => return from_residual(residual), Continue { 0: val } => val }` with its `#[allow(unreachable_code)]`, ranges to `Range { start, end }`,
  `RangeFrom`, `RangeTo`, `RangeToInclusive`, `RangeFull {}` and `range_inclusive_new(a, b)`, an `if`'s condition in `DropTemps` unless it
  holds a `let` (let chains stay), parentheses dropped (the printer puts back what precedence needs), a trailing expression statement as the
  block's expression;
- `format_args!` after `format.rs`: `flatten_format_args` (nested `format_args!` spliced), `inline_literals` (string and integer
  literals within their type's range folded into the template), `expand_format_args`' bytecode (literal chunks, placeholders' flag words,
  width and precision, explicit argument positions, the terminator), the `super let args` pair, `format_argument::new_*` per (argument,
  trait), `from_str`/`from_str_nonconst` for a literal alone, the compiler-generated `unsafe` block;
- items: `use` trees split into one item per leaf with a `ListStem` for each nested tree, functions with implicit `self`, generics whose
  parameters' bounds move into the where clause, `impl Trait` arguments as synthetic type parameters named by their text, elided
  lifetimes printed `'_` in signatures, statics and consts (references and the types std and the crate declare with a lifetime parameter:
  std's by their generics from the helper's data, RFP10), structs, enums, unions, impls, traits with associated types and consts, modules, `macro_rules!`;
- patterns: an identifier that resolves to a unit struct or variant, a constant or a static is a path pattern (the resolutions decide);
  expression paths whose resolution is partial (no resolution at the whole path) and that carry generic arguments before their last segment
  are type-relative (`Pair<u8, 4>::len`);
- attributes as rustc_attr_parsing leaves them: doc comments and unparsed attributes in order, the parsed ones after them (`Inline`,
  `NoMangle`, `Lang(..)`, `NoStd`, `NoMain`, `Feature([..])`, `PreludeImport`, `AutomaticallyDerived`, `Doc(DocAttribute {..})`,
  `Coverage(..)`, `Repr {..}`, `MacroExport {..}`, `MustUse`, `Cold`), then the cfg traces as one `CfgTrace([..])` and `CfgAttrTrace([..])`
  with their entries' Debug text;
- `$crate`: the crate of the macro whose tokens the path came from: the helper's crates by the tokens' file (`library/std/` ...), ours
  by the expansion's mark (`arbace.rust.front.expand`'s new `*mark-crates*`: std's macros by their path's crate, a user's `crate`, a
  builtin derive's `:derive`, whose spans then place no comment, as rustc's derive spans do not).

### RF6: the printer (`arbace.rust.front.hir-pretty`, `arbace.rust.front.pp`)

A port of `rustc_hir_pretty`'s `State` (items, generics, where clauses, types, expressions with `ExprPrecedence` and the
parenthesization rules, patterns, arms, blocks, statements, `DropTemps` as `{ let _t = ..; _t }`), with what it inherits from
`pprust/state.rs`: the comments copied forward by position (`maybe_print_comment`, trailing comments, blank lines with their `twice`
rule, `break_offset_if_not_bol`'s tucked offset), `head`/`bopen`/`bclose`, `commasep_cmnt`, token streams for macro definitions
(`print_tt`, `print_tts`, `space_between`, `print_mac_common`, `print_mac_def`), literals as `LitKind`'s Display (integers in decimal
with their suffix, strings and chars by `escape_string_symbol`/`escape_default`, byte strings by `escape_ascii`), parsed attributes as
`#[derive(PrintAttribute)]` prints them (words and `word_space`, no boxes: so they break at the enclosing box's indentation). A node
whose span lies in another file (std's macros) passes every comment of the root file, as the source map's order makes rustc's do.

**Results** (`bin/rust-front hir-check`): over the helper's expanded crates and resolutions **89 of 89 programs' HIR text equal to
`-Zunpretty=hir` byte for byte**; **the pipeline** (`hir-check --ours`: the native reader, our expansion, our resolution, our lowering)
**88 of 89 byte for byte**, the last (`expand1`) equal but for whitespace: four blank lines placed early, because our expansion gives a
`macro_rules!`-made struct's fields the span of the macro's tokens where rustc's start at the call site's identifier (an RF4 span
class). The negative control (one program's text against another's oracle) differs. `expand-check` (90 of 90) and `resolve-check`
(5,138 of 5,138 paths, both inputs) unchanged. The first run of the day's code: 2 of 3, then 38 of 88 with `format_args!`, 68 with the
comments of other files' spans, 78 with the parsed attributes.

Not taken yet: `async` and coroutines (Tier S), `asm!`, `yield`, `become`, contracts, delegation, foreign items' printing, inline
`const` blocks, `offset_of!`, unsafe binders, trait aliases, `impl` restrictions, closures' `for<>` binders, `box` patterns' lang items,
the `hir-tree` view and the HIR ids' judge, the parsed attributes beyond the corpus's (each is a line of the table), std's types' lifetime
parameters from data (done, RFP10: below).

### Estimates, revisited

RF6 was planned at 4 to 6 days; this slice took the corpus to byte equality in an afternoon: 305 lines of `.ae` for `pp.rs` and the
comments, 831 for the lowering, 917 for the printer, 84 for the judge. As with RF4-RF5, what the corpus does not exercise is most of
the rest; the judge (text for text, comments included) is stricter than planned, and was cheap.

### Proposed (RF6)

- **RFP10 Lifetime parameters of std's types.** Elided lifetimes in signatures print `'_` per lifetime parameter of the named type
  (`Formatter<'_>`), which needs each type's generics. Options: (a) **the helper's `--std-names` extended with each public type's
  generic parameters (kinds and names; pinned with the rest, RFS4), the name table replaced**, about half a day; (b) keep the table
  of 20 names until RF7 reads std's interface (`iface`), which carries generics anyway. *Recommended: (a)*: a wrong name in the table
  is a silent difference in RF7's input, and the data is a few lines of the helper.
- **RFP11 RF6 on the next corpus.** Options: (a) **`hir-check` joins RFP7's night run of the UI suite's run-pass programs (the
  oracle is one more helper run per file), differences recorded as classes**; (b) a corpus of its own. *Recommended: (a)*.
- **RFP12 What next.** Options: (a) **RF7 (collection: generics, predicates, `type_of`, HIR type lowering) over this HIR, judged
  against the helper's `iface`**; (b) the HIR ids' judge first (`-Zunpretty=hir-tree`, owners and local ids per node). *Recommended:
  (a)*: no later stage reads the ids' numbering, only their identity; (b) when a stage needs it.

## Decided (2026-11-05, the user: RFP10-RFP12)

All as recommended: std's generic parameters from --std-names now, the 20-name table dropped; hir-check in the UI run-pass night run, differences as classes; RF7 (collection) next, judged against the helper's iface, before a judge of the HIR ids.

## As built: RFP10 (2026-11-05)

std's types' generic parameters come from the helper's data; the 20-name table of `lower.ae` is gone.

- `tools/rsdump/front.rs`'s `--std-names` writes, after the modules and before the macros, one `(names/generics "DEFPATH" [(KIND NAME) ...])`
  per struct, enum, union, type alias and trait of std, core and alloc reachable through public module children (the definition's
  path once, sorted; `generics_of(d).own_params`: KIND `lifetime`, `type` or `const`; 1,078 lines). The data file's digest
  (`test/rust/front/std-names.sha256`) is re-pinned; the helper rebuilt by `bin/repr-rust helper`. The other readers of the file
  (`load-std-names`, `load-std-macros`) skip the new lines.
- `arbace.rust.front.lower/load-std-generics` reads them, `lower-crate`'s `:std-generics` option carries them, `hir-check` loads them from
  the file beside the oracles. A signature's elided path (`Formatter`, `Ref<T>`, `Cow<str>`) prints one `'_` per lifetime parameter of
  the type its last segment resolves to (the resolution's `(def KIND DEFPATH)`; the table printed exactly one, and `PhantomData`, `Iter`
  and `Ref` had to be taken out of it by hand: `Iter` and `Ref` of the programs are the crate's own or lifetime-less names). The crate's
  own types keep their by-name set (`crate-lt-types`; a local definition's resolution is `local-def`).
- A path that expansion generated has no resolution (a builtin derive's `$crate::fmt::Formatter`): its written text with `$crate` as
  the macro's crate (core for a derive) is the definition's path.
- Checked (helper rebuilt, std-names regenerated, light runs): `bin/rust-front hir-check` 89 of 89 byte for byte, `--ours` 88 of 89 and 1
  more equal but for whitespace (as before); `expand-check` 90 of 90 crates, 786 of 786 items; `resolve-check` and `--ours` 5,138 of
  5,138 paths, 88 of 88 programs.

## As built: RF7 (2026-11-06)

Collection, one day on the branch `rust-rf14` (from `rust-rfp10`), front half only. Code: `arbace/rust/front/collect.ae` (1,543 lines,
`arbace.rust.front.collect`) and its judge `collect_check.ae`; the verbs `bin/rust-front collect-check [--crates] [--ours]` and `bin/rust-front
crates DIR...` (`make rust-front-collect-check`); the samples `test/rust/front/collect1.rs` and `collect2.rs`. Ports of rustc 1.99.1
(`58cf8f9c3`) under MIT (RFS12), recorded in `LICENSE.md` and `doc/ARBACE.md`; evaluated from the checkout, out of `gate_key` (RFS13).
Light: no gate step, each check one process under 1 GB.

### The oracle

The helper's `--iface` (RF1's mode, `tools/rsdump/mirdata.rs`) writes one `(iface/item {...})` per local definition rustc collects: its
def path (`def_path_str` with no trimmed paths), `DefKind`, own generic parameters, their defaults, `fn_sig` (identity, binder skipped),
`type_of`, ADT variants and fields, impls' trait refs and self types, `explicit_clauses_of`, and since RF7 the associated and opaque
types' `explicit_item_bounds` and the opaque types themselves. `front-oracles` writes `NAME.iface.edn` beside the expansion and
resolution oracles; `bin/rust-front crates` writes them for ordinary crates (a Cargo registry's sources, each `src/lib.rs` at its
`Cargo.toml` edition, no features) into `.tmp/rust-front/crates/`. std's interface comes from `--std-names` (RFS4), which RF7 extends:
each type and trait parameter's default as a structural type, std's type aliases' types (`names/alias`), traits' supertraits, Fn sugar and
auto flags (`names/trait`), the lang items (`names/lang`) and the `#[doc(hidden)]` modules and re-exports visible_parent_map
deprioritizes (`names/hidden`, `names/hidden-reexport`); `test/rust/front/std-names.sha256` re-pinned (the old readers skip the new
forms).

### RF7: collection (`arbace.rust.front.collect`)

`(collect-crate HIR RES STD {:source TEXT :edition E})` over `arbace.rust.front.lower`'s HIR and the resolutions it was lowered with:

- the definitions as the def collector makes them (`def_collector.rs`): items, fields, variants and constructors, associated items, impls,
  closures (in the body's own fragment first, then those of each macro expansion as it is collected: a node of another file's span or a
  lowered `format_args!`), nested items, opaque types of return-position `impl Trait`; the disambiguators per parent and data;
- `generics_of`: Self, early-bound lifetimes (`is_late_bound_map`: named by a where clause, or by the output and constrained by no
  input), types and consts, synthetic `impl Trait` parameters named by the AST's text (nested ones first), an impl header's elided
  lifetimes as fresh `'_` parameters, closures' three, opaque types' captured lifetimes (edition 2024: all in scope);
- HIR type lowering: paths by their resolutions (std's ADTs with defaults filled, std's and the crate's free aliases expanded, a bare trait
  object in 2015/2018), type-relative paths (`Self::Item`, `T::Item`, through the bounds that define the name, the
  `SelfTraitThatDefines` filter), qualified paths, GATs' own arguments, references with the object lifetime default, `dyn` with its
  principal, projections and auto traits, `impl Trait` as opaque types, fn pointers and Fn sugar with their binders' anonymous regions,
  array lengths (a literal's value, a const parameter, else the anonymous constant's source text, any file's);
- the clauses (`gather_explicit_clauses_of`): a trait's MetaSized and supertraits, each type parameter's implicit Sized (MetaSized for
  `?Sized`), const parameters' types, the inline bounds, the where clause, then `impl Trait` arguments' bounds, Sized first within its
  predicate, Fn sugar's `Output` projection on the supertrait that defines it, `for<...>` binders; impls' order after
  `setup_constraining_clauses`; a trait's clauses on its own associated types moved to their `explicit_item_bounds`; opaque types'
  bounds and bidirectional outlives;
- rustc's type printer (`FmtPrinter`, no trimmed paths): visible paths through `visible_parent_map` computed from std's module children
  (a BFS from the crates an extern crate item names: std, or core for `no_std`), impl paths (`<T as Trait>`, `Type`, `module::<impl ..>`
  by `characteristic_def_id_of_type`), the value namespace's `::<..>`, `own_args_no_defaults`, regions (`should_print_optional_region`),
  binders named as `name_all_regions` names them, `dyn` and opaque types, clauses with `TraitRefPrintSugared`.

The names the oracle's resolutions do not cover (an expansion's paths: derives, local `macro_rules!`, other files of the crate) are looked
up in their module: its items, its imports and glob imports, the function scopes' items, then the edition's prelude; a span several
expansions share (a macro's `$t`) is resolved by the name it carries.

**Results** (`bin/rust-front collect-check`): the synthetic corpus, samples, eval programs and `test/rust/front` (90 programs with an
oracle), **1,189 of 1,191 items equal to rustc's, 89 of 90 programs whole**, from the helper's crates and from the pipeline (`--ours`:
the native reader, our expansion, resolution and lowering) alike; the two left are `vectors`' `simd_swizzle!` impl (a def-site name,
`Swizzle`, which needs hygiene, RFP8, and an anonymous constant rustc prints by its def path). **The registry's crates without
dependencies** (`bin/rust-front crates`: 75 crates under 6,000 lines with no `[dependencies]` and no build script; 62 build alone,
the others need target dependencies, `rustc_private` or a build script's output; 55 of them define items):
**8,157 of 8,157 items, 55 of 55 crates whole** (termcolor, glob, pkg-config, time-core, unicode-segmentation, fallible-iterator, ...).
The day's first runs: samples 991 of 1,017, crates 5,240 of 8,147 (out-of-line modules and macro-made items), then 7,930, 8,090, 8,147.
Along the way, gaps of the earlier stages the new samples showed, fixed: RF4's derives over lifetime and const parameters
(`cx.const_ident`), RF5's const initializers, RF6's `for<..>` and Fn sugar's fresh lifetimes, fn pointers' ABI, impl Trait arguments
(nested; their predicates after the where clause), an impl with only elided header lifetimes; `expand-check` 92 of 92, `resolve-check`
5,594 of 5,594, `hir-check` 91 of 91 (`--ours` 90 and one equal but for whitespace).

Not taken yet: hygiene for def-site names (RFP8), the resolutions of out-of-line modules' own paths (looked up by name, RFP15), lazy
type aliases, inherent associated types, trait aliases, `impl Trait` in let bindings and type aliases (TAIT), return-type notation,
const generics beyond literals and parameters (`generic_const_exprs`), the specialization graph and the impls indexes (RF8 builds them
on these facts), the variances and inferred outlives (regions are erased, RFS5).

### Estimates, revisited

RF7 was planned at 6 to 9 days; this slice took the corpora to equality in a day: 1,543 lines for collection and the printer, 108 for
the judge, 60 of helper. As before the judge's corpus is the limit: the registry crates were a better test than planned (real code's
macros, modules and generics), and the UI suite (RFP7) is the next.

### Proposed (RF7)

- **RFP14 The next corpus for collection.** Options: (a) **the UI suite's run-pass programs in RFP7's night run (one helper run per file
  more), then the registry crates with dependencies through the helper with `--extern`s built from the registry (no network: the
  sources are on the host)**; (b) the compiler corpus's crates (a Cargo build of rustc's own crates, heavy); (c) stay on the samples and
  the 55 crates. *Recommended: (a)*: both are light per file and exercise what real code writes.
- **RFP15 Paths outside the root file.** The helper's `--resolve` reports the root file's paths only; collection looks the others up by
  name (the module's items, imports, globs, prelude): a small resolver of its own. Options: (a) extend `--resolve` to every local file
  (spans with their file) and key lowering and collection by file; (b) **teach RF4-RF5 to load out-of-line modules (`mod m;`,
  `#[path]`) so the pipeline (`--ours`) runs on the crates, its resolutions judged by an extended `--resolve` as (a), and the name
  lookup kept only for the helper's input**; (c) keep the lookup. *Recommended: (b)*: the pipeline is the goal (no rustc) and RF4 must
  load module files anyway; (a)'s oracle extension is its judge.
- **RFP16 What next.** Options: (a) **RF9's first slice with RF8's part it needs: the helper's `typeck` mode (each body's node types,
  method resolutions, adjustments, by span), then type checking of bodies without method calls on generic receivers (literals and
  their fallback, locals, paths, calls of fn items, operators on scalars, struct and tuple expressions, fields, blocks, `if`, `match`
  on simple patterns), judged fact by fact**; (b) RF10 first, MIR built from the helper's typeck data (judged against MIR text); (c) RF3's
  layouts from RF7's ADT data (the back half's next piece). *Recommended: (a)*: the survey's order, and every later stage reads its facts.

## As built: RF9a, type checking's first slice with RF8's part (2026-11-06)

RFP16 proposed RF9's first slice next (the survey's order, §7: RF8 and RF9 after RF7); taken the same day on `rust-rf14` while the
proposal waits, since every later stage reads its facts. Code: `arbace/rust/front/typeck.ae` (`arbace.rust.front.typeck`, over RF7's
collection in its `with-crate` context) and its judge `typeck_check.ae`; the verb `bin/rust-front typeck-check [--crates] [--ours]`
(`make rust-front-typeck-check`). Ports of rustc 1.99.1 (`58cf8f9c3`) under MIT (RFS12), recorded in `LICENSE.md` and `doc/ARBACE.md`;
evaluated from the checkout, out of `gate_key` (RFS13). Light: one process under 1.2 GB, the samples' check about a minute.

### The oracles

- **`--typeck`** (`tools/rsdump/front.rs`): for each typeck root (`hir_body_owners`, closures inside their roots), over the root
  file's HIR (no expansion's spans): `(tc/expr OWNER [span] KIND "TYPE" {:adj [[KIND "TARGET"]...] :method "PATH" :args "..."})` per
  expression (`node_type`, the adjustments, the type-dependent definition) and `(tc/pat OWNER [span] "TYPE" "MODE")` per binding (its
  type and `BindingMode`), types printed with no trimmed paths after writeback (regions erased). `front-oracles` writes `NAME.tc.edn`.
- **`--std-iface`**: std's interface as data (RFS4, Tier U's data, pinned by `test/rust/front/std-iface.sha256`, 13.8 MB, one second to
  write): every reachable ADT with its variants and fields `(si/adt ..)`, trait with its items and clauses `(si/trait ..)`, impl (the
  ADTs' and the primitives' inherent impls, the traits' impls in std, core and alloc, with polarity) with its self type, trait ref,
  generics, clauses and items, associated types' values and constants' types `(si/impl ..)`, and function or method with its
  generics, clauses and signature `(si/fn ..)`, all as structural types (parameters by index, late-bound regions marked, projections,
  opaque types, trait objects, fn pointers, const parameters in array lengths); `core::arch` alone left out.

### RF9a: type checking (`arbace.rust.front.typeck`), with RF8's part

- inference variables (types, integers, floats, consts; ids never reused across snapshots), unification without regions,
  snapshots and rollback; `'static` kept through the body for coercion's no-op case, every region erased at writeback;
- `check_expr_with_expectation` for literals (suffixes, expectations, fallback i32/f64), paths (locals, fn items with fresh arguments,
  constructors, consts and statics, const parameters, std's values, a desugaring's lang item paths and bindings), calls (fn items and
  pointers, closures, Fn-bounded parameters, trait objects and opaque types with the callee's autoref,
  `expected_inputs_for_expected_output`), method calls (the probe below, turbofish), builtin and overloaded operators (by their traits;
  comparisons by reference; compound assignments), unary operators, references, casts, fields (autoderef), indexing (builtin, then
  `Index` impls with the base's autoref, `IndexMut` on assigned places), tuples, arrays, repeats, blocks, `if`, `loop` and `while`,
  `match` with guards, `let` chains, `return`, `break` with values, struct expressions (the crate's and std's, lang items' ranges),
  closures (signature deduction from pending Fn obligations, trait objects and return-position `impl Trait`; their types printed
  `{closure@FILE:L:C: L:C}` by their `fn_decl_span`), anonymous constants (array lengths, repeat counts, discriminants by the enum's
  repr) as typeck roots of their own;
- coercions (`coercion.rs`): never to any, reborrows (`coerce_borrowed_pointer` with its autoderef through Deref impls), references to
  raw pointers, unsizing (arrays to slices, to `dyn`, `Box`/`Rc`/`Arc` of them), fn items and non-capturing closures to fn pointers,
  CoerceMany's LUB for `if`, `match` and blocks;
- patterns (`pat.rs`): bindings with explicit and default binding modes (match ergonomics), tuples, references, literals, the crate's
  and std's tuple-struct, struct and path patterns, or-patterns;
- the method probe (`method/probe.rs`): the receiver's autoderef steps (references, then `Deref` impls), by value (a reference
  reborrowed), then `&`, then `&mut`; at each, inherent candidates (the crate's impls, std's by the self type's key) before trait
  candidates (Self a fresh variable, the method's self type unified with the adjusted receiver, then `Self: Trait` proven; the trait's
  other arguments left to inference); type-relative paths (`Vec::new`, `u32::try_from`, `i32::MAX`) the same way;
- **RF8's part, the solver's slice** (`rustc_trait_selection`'s accepting paths): selection of an impl whose header unifies and whose
  clauses hold (trait clauses recursively, projection clauses binding the impl's parameters), negative impls excluded, Sized by the type,
  the builtin impls of the Fn traits for closures, fn items and pointers, integer variables selecting their one integral impl, a
  cycle or a chain over four deep unproven, a per-body budget; normalization of projections through the selected impl's associated
  type (FnOnce::Output of a callable by its signature), as their self types resolve; pending obligations (each instantiated item's
  clauses) selected where possible (`select_obligations_where_possible`) before fallback and before a method call on a variable.

Also: the object candidate (a trait object implements its principal, the principal's supertraits and its auto traits), traits in
scope (the edition's prelude's and those the crate's use items name, not all of std's), slice patterns, overloaded derefs (Deref,
DerefMut on assigned places), arrays' unsized method step (`arr.iter()`: Borrow, then Pointer(Unsize)), operators over integer
variables left pending until fallback, Fn obligations on callables unifying their signatures, the `?` desugaring's `residual` binding
(a desugaring's, no fact), named regions kept through the body for coercion's no-op case, each anonymous late-bound region of a
signature its own (`for<'a, 'b>`).

**Results** (`bin/rust-front typeck-check`, the 90 programs with an oracle; facts: each expression's node type and adjustments, each
binding's type and mode): **10,768 of 11,173 facts equal to rustc's (96.4 %), 92 more with the type equal and the adjustments not,
54 programs whole**; from the pipeline (`--ours`: native reader, our expansion, resolution, lowering, collection, std's interface the
only rustc data) **10,760**. The day's run: 4,217, then 5,890 with std's interface and the probe, 7,544 with operators, 8,142 with
closures, 9,843 with associated constants, 10,084 with the Fn builtins, 10,566 with pending integer obligations, 10,768. The registry
crates (`typeck-check --crates`, 49 of the 55 with bodies, every function of a library): **21,922 of 23,722 facts (92.4 %), 266 more with the type equal, 6 crates whole** (the midday code: 20,731). The negative control
differs. Light: about a minute for the samples (under 1.2 GB); the crates about twenty minutes (a per-body selection budget bounds a
runaway search).

Not taken yet (the classes of the non-equal facts): closures' kinds (FnMut/FnOnce by their captures: a call's autoref) and captures;
normalization through the parameter environment (`<I as IntoIterator>::Item` by a where clause's projection, `I::IntoIter`'s item
bound); a literal's reborrow when a `vec!`'s or a `for` loop's array elements are coerced (rustc's CoerceMany order); `Box<dyn Fn>`
returns' Pointer at the arm or the block; specialization; async; SIMD's intrinsics-heavy programs (`vectors`); formatting macros'
internal facts (none: rustc's are expansions); the facts of anonymous constants inside closures and of const generic arguments.

### Estimates, revisited

RF8 (10-15 days) and RF9 (14-22) were planned as the long poles; this first slice of both, judged on every expression of 90 programs
and 49 crates, took one day: 1,215 lines of `.ae` for type checking with the solver's slice (1,697 for collection with the printer),
100 for the judge, about 330 of helper. As with RF4-RF7, what the corpora do not exercise is most of the rest (and the solver here is
the old solver's accepting paths for the common cases, not RFS6's next solver); the judge (every node's type, every adjustment) is
stricter than the plan's and was cheap; the estimates for the remainder stand.

### Proposed (RF9a)

- **RFP17 The solver's shape.** RF9a's selection is the old solver's accepting paths for the cases the corpora reach (unification of
  impl headers, recursive clauses, projection clauses, a depth bound, the builtin Fn and object candidates). Options: (a) **keep it and
  grow it as RF9's classes ask, with RF8 proper (the next solver after rust-analyzer's shape, RFS6) when ambiguity, coinduction (auto
  traits) or specialization first need it**; (b) port `rustc_next_trait_solver` now and move RF9a onto it; (c) port the old solver's
  `select.rs` and `project.rs` whole. *Recommended: (a)*: every fact is judged, so a divergence shows where the slice is too simple.
- **RFP18 std's interface as pinned data.** `--std-iface` (13.8 MB) is regenerated by the helper and pinned by its digest, like
  `--std-names` (RFS4, RFP6). Options: (a) **as built: the helper writes it at the pin, the digest is committed**; (b) commit the file
  (compressed) so no rustc is needed even for the data; (c) derive it from the std sources by RF4-RF7 themselves (Tier S, RF12).
  *Recommended: (a) now, (c) at RF12*: (b) adds 2-3 MB of compressed data to the repository for a dependency that the pin already makes
  reproducible.
- **RFP19 What next.** Options: (a) **RF9's next slice on its classes (closure kinds and captures, the parameter environment's
  normalization, the CoerceMany order), then RF10 (THIR and MIR building) over RF9's facts, judged against the MIR text**; (b) RF10 now
  on the helper's typeck data, RF9 later; (c) the UI suite's run-pass programs through every judge first (RFP7, RFP14). *Recommended:
  (a)*, with (c) in the night run when the suite's tarball is on the host.
