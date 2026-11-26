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

### RF3b: the codegen on rustc's data, second round (2026-11-05, branch `rust-rf3b`)

RFP1 (a) decided: finish the codegen on rustc's data before replacing the oracle's answers. One day's second pass over the refusals
and the differing items of the first coverage file: **10,433 of 11,453 mono items' functions equal rustc's IR (91.1 %)**, from 8,342
(72.8 %), over the same 81 programs (`test/rust/front/coverage-2026-11-05b.edn`, the record; the first file stays), no program lost an
item (checked per program on each of the 20 runs of the day: the coverage over the 81 programs is 6 minutes at about 1.5 GiB), **40
programs with every item equal** (13 before) and the six `no_std` samples still 46 of 46 equal with their runs' exit statuses
(`bin/rust-front codegen-check`). **Runs**: 34 synthetic programs (the 28 new ones are the `overflow-*` programs, the `panic-*`
programs, `space`, `panic-explicit`, `panic-unwrap-none`) built with their own unit made of our IR and linked with the host's native std
(`bin/rust-front run-native`) give rustc's status, stdout and stderr (thread ids normalized): 34 of 34. Three changes of the run
path were needed for them: the unit declares the functions a constant or a reified pointer names that the unit does not define,
and carries the named struct types the GEPs use.

What was built (in `arbace/rust/back/codegen.ae`, `check.ae`, `coverage.ae`):

- **LLVM's types of layouts** (`backend-type`, after `rustc_codegen_llvm/src/type_of.rs`): `Offset`, indexing and aggregates of
  arrays now carry rustc's element type: scalars as their memory type, scalar pairs as `{ a, b }`, arrays as `[N x T]`, tuples as
  anonymous structs with `[N x iK]` fillers for padding (K the filler's alignment), ADTs and closures (also scalar pairs of ADTs) as
  named structs whose name is the type's text with every ADT's path its definition's (from the DefId in the forms, the local crate's
  name and the hashes dropped, lifetimes kept as `'_`). The bodies of the named structs are not rustc's (the dump has no field types of
  ADTs): the module defines each as a `[size x i8]` filler, enough to lower and run, and the judge's normalizer compares the *names*
  in `getelementptr` types, not the bodies. Closes `Offset` of aggregates (465 items), arrays of non-scalars (91), constant indexes.
- **Analysis**: the SSA analysis of locals runs over every block, not only the mono-reachable ones (rustc keeps a local assigned in
  both arms of a constant `if` in memory); a pair local with a single variant stays a value through `downcast` projections while any
  other downcast makes the local live in memory, a zero-sized one included.
- **Intrinsics and rvalues**: `arith_offset`, `size_of_val` and `align_of_val` (slices, `str`, `dyn` through the vtable's slots 1 and 2,
  sized pointees), `compare_bytes`, `copy`, `select_unpredictable`, `is_val_statically_known` (`llvm.is.constant`), `[x; N]` by
  `memset` for zero or byte elements (the loop for the rest is not taken), `Cmp` as `llvm.ucmp`/`llvm.scmp`, the misaligned,
  null-dereference and null-reference asserts; **`rotate_left`/`rotate_right` are calls of the fallback instance**, not expanded
  (rustc's LLVM backend leaves them to the Rust body); unsizing of Box and other wrapped pointers of arrays; `BoxDerefTransmute`;
  transmutes of a scalar or pair into a memory place; unions as aggregates (all fields at offset 0); scalar pairs larger than two
  registers (an `i128` with a bool) returned through a pointer, with the temporary rustc makes for an SSA destination.
- **Folding and flags LLVM's builder and rustc's choices need**: `trunc` and `udiv`/`sdiv`/`urem`/`srem` of constants fold; a shift
  amount is masked in its own type and then cast (not the other way); `mul` by a size carries `nuw nsw` for `copy`, `size_of_val`
  and `copy_nonoverlapping` and none for `write_bytes`; a constant that is all uninitialized copies nothing; a read of a zero-sized
  field emits nothing; an enum constant held as a pair is *loaded* from its allocation while a pointer with its metadata is a value;
  the scratch of a cast integer (`i24`) is aligned as LLVM aligns the integer (4), not by its size; the align attribute of a reference
  is found through tuples, closure captures, spread tuples' elements and a Box inside a wrapper; `undef`, not `poison`, for the
  missing component of a pair.

What blocks the rest (the refusals of `coverage-2026-11-05b.edn`, by items): 565 differ, **234 `abi other`: the vector ABI** (hashbrown's
SSE2 group and `simd_*`: the helper dumps `(other)` with no element type or lane count, so the layout of `__m128i` and `Simd<T, N>`
is unknown), **31 virtual calls** (the callee carries the trait method's path but not the vtable slot), 34 transmutes between
other layouts, 26 other unsizings, 25 `subslice` places, 23 `typed_swap_nonoverlapping` (a call of the fallback instance whose symbol
needs v0 mangling of the type's arguments), 3 errors, 13 `catch_unwind`, a tail of constants, `tlsref`
and float intrinsics. The 565 differing items fall in classes seen by their first differing line: block order (54: rustc creates the
`unreachable` block of an `invoke` before the cleanup's resume block; ours creates it last), the order and sizes of allocas where
rustc's analysis keeps a different set of locals in memory (about 70), drop glue and `Drop` of `dyn` through the vtable (48, one
program), vtable shims' parameter attributes (17: rustc gives `align 8` to some `*mut Closure` self parameters and not to others, a
rule not found), loads of a field versus a GEP computed first (the order operands' places are made), and a long tail of one or two.

Findings worth keeping (the second day's surprises): the intrinsics `rotate_*` reach the IR as calls, `typed_swap_nonoverlapping` too
for aggregates; at `-O0` `Ord::cmp` is `llvm.ucmp.i8.<T>`, not the select chain; the allocas of a function appear in the reverse order
of the locals even when the sizes interleave; a local assigned once in each arm of a branch is memory, and rustc's analysis does not
look at which arm is reachable; `ZeroSized` fields are never read; the fully-initialized enum constants are loaded; a debugging
aid worth having: `CG_SYM=SYMBOL bin/rust-front coverage FILE` prints the two normal forms of one function.

### Estimates, revisited

RF1 (6-9 days planned): the modes the table names for RF3's judging are in (`mir`, `iface`, `layout` as the layouts table, the
mono-items run, the census); `expanded`, `resolve`, `typeck` and `consts` (RF4-RF10's oracles) are not built. RF2 (3-5): done
in under a day, the spec, reader, printer and check. RF3 (16-26 days): the first day took the code generation from nothing to 72.8 %
of the mono items of 81 programs, the collector's traversal to all 81 item sets, and seven programs to running; what is left of the 16 to 26 is the remaining codegen
(LLVM's types of aggregates for `Offset` and arrays, vectors, virtual calls, `size_of_val`, the intrinsics' tail, the classes of the
1,560 items that differ: about 3 to 5 days by the refusal counts), then the pieces the helper still answers: layout (`rustc_abi`, about 2,500 lines), the collector and instance
resolution with specialization, v0 symbols (the crates' disambiguator hashes), vtable and drop glue and shim MIR from data
(about 8 to 12 days). The estimate stands; the exactness of the judge is what the day showed. After the second day (RF3b) the codegen's
refusals that need only rustc's data are done but for the transmutes, unsizings and subslices; the two big ones need the helper to
dump more (the vector layouts, the vtable slots), RFP13 below; the 565 differing items need about 2 more days by the classes above.

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

### Proposed (RF3b)

- **RFP13 The helper's missing data.** Two refusal classes, 265 items (2.3 % of the corpus), wait on facts the helper does not dump:
  the vector layouts (`(other)` with no element type or lane count: 234 items, hashbrown's SSE2 group and `simd_*`) and the vtable slot
  of a virtual callee (31 items, more in programs that call trait objects: the drop and `Drop` of `dyn` are another 48); the fields'
  types of ADTs would also give the named struct types rustc's bodies instead of fillers. Options: (a) **add them to `--mono-mir`
  (`InstanceKind::Virtual(_, idx)`, `layout.backend_repr` of vectors with the element and count, field types of every ADT layout), rebuild
  the helper once (rustc-dev from the pinned apk, a heavy build: one night's lane), regenerate the 81 dumps (`COVERAGE_REDUMP=1`, about
  15 minutes), then port the vector ABI and the virtual calls: about 1 day**; (b) port layout (`rustc_abi`, 8 to 12 days of RF3) first,
  which answers all three natively; (c) leave them as recorded classes and go on to the 565 differing items (about 2 days) and RF3's
  replacement of the oracle's answers. *Recommended: (a) in the night's heavy lane, (c) meanwhile.*

## Decided (2026-11-05, the user: RFP1-RFP4)

All as recommended: the codegen finished on rustc's data first, then layout, the collector with resolution and v0 symbols ported, each judged against the helper; the native-std leg now, the ci leg once the lowered std exists; `bin/rsi run --front mir` as built; no gate step until the row fits GT8's budget. The branch's early commits without trailers are squashed into commits that carry them at its merge (no rewrite of the branch).

## Decided (2026-11-05, the user: RFP13)

(a) with (c) meanwhile: the helper's --mono-mir extended (vector layouts, vtable slots, ADT field types), rebuilt once in a night's heavy lane, the 81 dumps regenerated, then the vector ABI and virtual calls ported; the items stay classed until then.
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

## Decided (2026-11-06, the user: RFP14-RFP19)

RFP14 (a): the UI suite's run-pass programs through every judge in a night run, then the registry crates with dependencies. RFP15 (b):
expansion and resolution load out-of-line modules (`mod m;`, `#[path]`) so the pipeline runs on crates, judged by an extended
`--resolve`. RFP16 and RFP19 (a): RF9's next slice on its classes (closure kinds and captures, the parameter environment's
normalization, CoerceMany's order), then RF10. RFP17 (a): grow the old solver's accepting paths as RF9's classes ask. RFP18 (a): std's
interface regenerated by the helper, only its digest committed.

## As built: RF9b, type checking's second slice (2026-11-06)

RFP19 (a) taken on `rust-rf9b` (from `rust-rf14`'s `fccc3d812`): RF9's classes of non-equal facts, ranked by facts per fix, each piece
ported rule for rule from rustc 1.99.1 (`58cf8f9c3`) and proved on the samples and the registry crates against the helper's facts. Code:
`arbace/rust/front/typeck.ae` (now 2,004 lines), small changes to `collect.ae` (std's structural types, the printer's binders, lifetime
elision) and `lower.ae` (the `?` desugaring's residual path marked), and the helper (`tools/rsdump/front.rs`). Light: the samples' check
about 30 s under 1.2 GB (it was 85 s before the evaluation cache), the crates' about 5 minutes under 1.9 GB (20 minutes before; above the night's 1.5 GiB cap).

### The solver (RFP17 (a): the old solver's paths, grown)

- **Ambiguity** (`select.rs`'s candidate assembly and winnowing): a goal with inference variables that two impls match selects none and
  binds nothing (`:ambiguous`, the obligation stays pending); before, the first impl that unified was taken, which typed `x % 2` as
  `usize` when `x: &{integer}`. The answers for ambiguous and failed goals are cached per body by their resolved form (the evaluation
  cache's role), and holds?'s nested evaluation, whose bindings are rolled back, takes the first impl.
- **Where-clause candidates before impls** (`assemble_candidates_from_caller_bounds`): the parameter environment (the clauses of the
  definitions in scope, elaborated with their traits' supertraits, `elaborate.rs`) matches any self type with a parameter in it, regions
  aside (`for<'b> &'b I: IntoIterator`); one match is selected, several are ambiguous; a method on a parameter takes its clause's
  arguments (a WhereClausePick).
- **Normalization** (`project.rs`): a projection on a rigid self type (a parameter, an alias, an opaque type) by the parameter
  environment's projection clauses, then the alias's item bounds (the crate's associated types' `explicit_item_bounds`; std's from the
  helper), then the impls (a blanket impl applies to a rigid type too); on another type with a parameter in it, a where clause's projection
  before the impls. An unresolved projection in an operator's output is a fresh variable with its projection obligation. Projections
  without variables are cached per body.
- **The fulfillment context** drops what it processed (selected, normalized, holding by assumption, failed); integer and float variables'
  obligations select their one integral impl (`{integer}: Add<u8>`); the builtin Fn impls unify a callable's inputs with the arguments'
  tuple, so an impl's `F: FnMut(char) -> bool` gives a closure its parameters (`str::split(|c| ..)`); a trait method's `Self: Trait`
  predicate is registered with the method (`Into::into` as a value); aliases implement their item bounds' traits; a selected impl's
  nested obligations with variables stay pending (confirmation: the blanket Into impl's `U: From<T>` then binds `T`).

### Type checking

- **CoerceMany** as rustc runs it (`coercion.rs`): the first arm (array element, block tail) coerced to the target (the expectation, else
  a fresh variable), each later one coerced in LUB mode to the previous arms' type unless equal (`try_find_coercion_lub`), the LUB's
  autoref region the GLB of the two (so a `vec!`'s or a `for` array's string literals after the first are reborrowed); generalization
  when coercing into a variable (its free regions fresh variables); region variables for instantiated lifetimes, so a value of one type
  coerced to the same type is a no-op only when the regions are the same; a block's type is its coercion target; trait objects unify by
  principal, projections and auto traits, and upcast to their own principal (`Box<dyn Fn>` to `Box<dyn Fn>` is a Pointer(Unsize)).
- **Arguments** (`check_argument_types`): closures checked last after `select_obligations_where_possible`, each argument coerced to its
  expected input type (the formal one then its supertype: `Some(&e)` expecting `Option<&dyn Error>`), the expected inputs fudged
  (`fudge_inference_if_ok`: their region variables fresh); a signature's late-bound regions instantiated as fresh region variables
  (`instantiate_binder_with_fresh_vars`), so `parse(bb(s))` reborrows both `s` and `bb(s)`.
- **Operators** (`op.rs`'s `check_overloaded_binop`): the right operand's expectation the `?R` that `L: Op<?R>`'s selection binds
  before it is checked, the operand coerced to it (`"abc" < "abd"` reborrows the right literal; `String += &&str` derefs it); a
  compound assignment's left side a mutated place; overloaded unary operators (`Neg`, `Not`); calls of a Box of a Fn trait object
  through the Box's own Fn impl.
- **Return-position `impl Trait`** inside its defining body: a hidden type variable with its bounds' obligations
  (`replace_opaque_types_with_inference_vars`): `-> impl Iterator<Item = u8> { once(1) }` types the literal `u8`.
- **Locals** (`check_decl_initializer`): the initializer coerced to the local's type, a fresh variable without an annotation, which a
  block initializer takes as its target (a `&mut` block value is reborrowed).
- **Divergence**: a block whose statements have the never type is `!` without a tail; a diverging tail is coerced to a diverging
  variable, whose fallback is `()` before edition 2024 and `!` since (`fallback.rs`); a variable a `!` is coerced to diverges.
- **Writeback**: `fix_scalar_builtin_expr` (an overloaded comparison whose operands end scalar is builtin: its autorefs dropped);
  `check_casts` after fallback (a cast that is a coercion takes its adjustments: `&x as *const T`, a fn item to a fn pointer, an
  unsizing `Box::new(c) as Box<dyn Fn()>`).
- **Closure kinds** (`upvar.rs`'s `closure_analyze` over `expr_use_visitor.rs`'s uses): a closure whose expectation names a Fn trait has
  its kind; else a move of a captured non-Copy place makes it FnOnce, a mutation or mutable borrow FnMut, else Fn; a call of a closure
  value is autoref'd by its kind (`&`, `&mut`, none). The captures themselves (by-ref places, by-value moves) are not recorded yet.
- Also: `convert_place_derefs_to_mutable` (a receiver's `&mut` autoref makes its place's Index IndexMut); operators on a variable or a
  parameter left operand; type-relative paths with generic arguments and on parameters (`T::default()`, `Self::read_uint`), qualified
  paths (`<T as Trait>::item`), traits' associated constants; anonymous constants numbered in the def collector's order, const arguments
  among them, expression constants checked as typeck roots of their own; labeled breaks and blocks; unions, `Self { .. }`, `Self(..)`
  (Res::SelfCtor) and `Self::Variant`; raw string literals; ExpectCastableToType for literal operands; a Box's builtin deref; lifetime
  elision of signatures' outputs.

### The helper and std's interface (RFP18 (a): regenerated, the digest committed)

`--std-iface` grows: trait objects' inherent impls (`impl dyn Any + Send`), associated types' item bounds, constants and statics with their
types, array lengths evaluated (`[u8; size_of::<Self>()]` in `impl u16` is `[u8; 2]`), anonymous bound regions by their index in their
binder (`'^#N`: a signature's elided output is its input's), and regions bound inside a trait object or fn pointer marked `'^^` (their
binders print their own `for<..>`). `test/rust/front/std-iface.sha256` re-pinned.

### Results

`bin/rust-front typeck-check` (the 90 programs): **11,139 of 11,173 facts (99.7 %), 81 programs whole** (RF9a: 10,768, 54 whole); the
pipeline (`--ours`) 11,137. The registry crates (`--crates`, 49 crates): **23,636 of 23,722 facts (99.6 %), 27 crates whole** (RF9a: 21,922, 6 whole). The other judges unchanged:
`expand-check` 92 of 92, `resolve-check` 5,594 of 5,594, `hir-check` 91 of 91, `collect-check` 1,189 of 1,191; the negative controls differ.

Not taken yet (the classes of the facts left): closures' captures (the places and modes, which RF10 needs) and the full
ExprUseVisitor (match patterns' moves, by-ref bindings); `#[derive(Default)]`'s `#[default]` variant path; traits in scope beyond the prelude and the crate's `use`
items (`self.0.fmt(f)` inside `impl Display`); `simd_swizzle!`'s def-site names (RFP8); `format_args!`'s width and precision arguments'
`usize`; specialization; async.

### Estimates, revisited

The slice took a day as RF9a did (about 800 lines more of `typeck.ae`, 60 of helper); the solver's growth (ambiguity, where clauses,
nested obligations, the evaluation cache) was the larger half, and made the checks faster, not slower. What the samples and crates still
miss is now small and scattered; the judge's corpus (RFP14's UI suite and the crates with dependencies) is again the limit.

### Proposed (RF9b)

- **RFP20 Closures' captures in the judge.** RF10 (MIR building) needs each closure's captured places and capture modes
  (`closure_min_captures`) and kind; the slice infers kinds, and the helper's `--typeck` dumps neither. Options: (a) **extend `--typeck`
  with each closure's kind and captures (place, mode, by-value or by-ref) and port `upvar.rs`'s capture analysis whole, judged fact by
  fact before RF10 reads it**; (b) leave captures to RF10, judged through the MIR text only. *Recommended: (a)*: a wrong capture shows
  as a fact rather than as a MIR diff far from its cause.
- **RFP21 The order of RFP15 and RF10.** Options: (a) **RFP15 (b) first (expansion and resolution load `mod m;` files, the helper's
  `--resolve` extended to every local file), so the pipeline (`--ours`) runs on the registry crates and every RF4-RF9 judge sees real
  multi-file crates, then RF10**; (b) RF10 first on the samples, RFP15 after. *Recommended: (a)*: a few days at most, and RF10's MIR
  judge is better fed by crates than by the 90 samples alone.

## Decided (2026-11-06, the user: RFP20, RFP21)

- **RFP20 (a):** the helper's `--typeck` gains each closure's kind and captures, and rustc's capture analysis is ported in full, judged
  fact by fact before RF10 reads it.
- **RFP21 (a):** RFP15 (loading `mod m;` files through expansion, resolution, lowering and collection) comes before RF10.

## As built: rust-90, the read stage native and what eval still takes from rustc (2026-11-07)

Branch `rust-90` (doc/rust/RUST-EVAL-SURVEY.md, "As built: rust-90"). The native reader (the survey's calibration, doc/rust/NATIVE-READER.md)
is now the read stage's default: `bin/repr-rust read`, `dump` and `roundtrip` run no rustc, and the round trip's tree level compares
the native reader's trees and lines (`arbace.repr.rust.native tree`, byte for byte rsdump's on `native-check`'s 68 files). The
round trip's sample by day with rustc, rustdoc, cargo, cc, gcc, clang and the LLVM tools first on `PATH` exiting 99: std's eight files
and 82 UI files (tree, text and mode `:full`'s tree levels) equal, the seven controls differing, the stubs' log empty.

**How far rsi can run from the native front end instead of rustc's IR.** The front end ends today at type checking (RF9b: 99.7% of
the samples' facts, 99.6% of the crates'); rsi runs rustc's `-O0` LLVM IR. The path between, by milestone (§7), and what each
consumes from rustc after it is built:

| piece | state | what it would still take from rustc |
|---|---|---|
| RF10: THIR, MIR building, promotion, the semantic passes, CTFE | not started; needs RF9's captures (RFP20 (a)) and RFP15 (a) first (RFP21 (a)) | nothing for the program's own bodies |
| RF3's back half (monomorphization, layout, ABI, codegen to LLVM forms) | a first slice: `bin/rsi run --front mir`, from rustc's MIR, `no_std` programs with an `extern "C"` main | std's generic MIR (Tier U: std's MIR library dumped by the helper at the pin, a record) |
| RF11: Tier U's first light, `check --front arbace` | not started | std's interface and MIR library (Tier U's data), std's lowered IR (`test/rust/runtime/ir.sha256`) |
| RF12: Tier S, std from source | not started | nothing |

So until RF12 every eval and AOT case consumes a record a toolchain made (std's MIR or IR at the least): **eval and AOT stay at 0.5
whatever the front end does before Tier S**, as RF10-RF11 move only the program's own nine-in-266 functions (§2.4) off rustc's IR.
What rust-90 pinned instead: the programs' IR as packs (`test/rust/ir/`, `bin/rsi ir-pack`, the oracle tier), std's IR at its pin,
and the ci leg's library keyed without the C toolchains (EVAL.md §13). The estimates of §7 stand: RF10 14-22 days, RF11 5-8, RF12
35-60.

## As built: RF10 (first slice), MIR from the native front end (2026-11-08)

Branch `rust-rf10` (from next `bd33e27cc`), the user's order of 2026-11-07 (RES54 (a): RFP20 → RFP15 → RF10 → RF11 → RF12).

### RFP20 and RFP15 first?

Both were decided before RF10 (RFP20 (a), RFP21 (a)); neither is small, and neither is needed by RF10's first slice, so the slice was
taken first on the single-file programs where neither applies, and both stay next in the decided order:

- **RFP20** (closures' captures in the judge): the helper's `--typeck` dumps no captures, so the helper's `front.rs` grows (a
  rebuild, ~15 s) and `upvar.rs`'s capture analysis (with `expr_use_visitor.rs`'s place projections, about 2,500 lines of rustc)
  is ported and judged fact by fact: 2-3 days. RF10's slice refuses closures (`closure`, 21 bodies of the corpus), so it reads no
  capture.
- **RFP15** (`mod m;` files through expansion, resolution, lowering and collection, `--resolve` extended to every local file): "a
  few days" by RFP21's own estimate; only the registry crates need it, and RF10's judge here runs on the synthetic corpus, the
  samples and `test/rust/front/`, all single files.

### What was built

- `arbace/rust/front/build.ae` (`arbace.rust.front.build`, about 1,400 lines), ports of rustc 1.99.1 (`58cf8f9c3`) under MIT:
  - **the region scope tree** (`rustc_hir_analysis/src/check/region.rs`: `resolve_block`, `resolve_stmt`, `resolve_expr`, `resolve_arm`,
    `resolve_cond`, the terminating scopes, edition 2024's tail expressions and `if` rescopes) and `temporary_scope`
    (`rustc_middle/src/middle/region.rs`);
  - **THIR** (`thir/cx/expr.rs`'s `mirror_expr_inner`, `apply_adjustment` for NeverToAny, builtin derefs, borrows and pointer
    coercions, `make_mirror_unadjusted` for literals (negated literals), paths, calls (constructors as aggregates), method calls,
    operators, assignments, casts, fields, indexing, tuples, arrays, structs, blocks, `if`, loops, `break`/`continue` by their
    target's scope, `return`, `match`, `let`; `thir/pattern`'s bindings, wildcards, constants, tuples, structs and variants (the
    crate's and std's), references and the implicit dereferences of match ergonomics);
  - **the builder** (`builder/`): `construct_fn` and `args_and_body`, `Scopes` with the drop trees (`DropTree`'s `assign_blocks` and
    `link_blocks`, `build_exit_tree`, `diverge_cleanup`, `build_unwind_tree`), `in_scope`, `in_breakable_scope`,
    `in_if_then_scope`, `break_scope`, `break_for_else`, `ast_block_stmts` with `let`'s remainder scopes and visibility scopes,
    `expr_into_dest`, `stmt_expr`, `as_operand`, `as_temp`, `as_rvalue` with `build_binary_op`'s overflow, shift and division checks,
    `as_place` with the bounds check, `then_else_break`, and **the match lowering** (`lower_match_tree`, `match_candidates`,
    `test_candidates`, the buckets of `buckets.rs`, `perform_test`'s switches on discriminants, integers and booleans, the false
    edges, `bind_matched_candidate_for_arm_body`, `place_into_pattern` for patterns in `let` and in parameters); `construct_const`
    for explicit discriminants; user type annotations of `let`s;
  - the body as MIR forms (doc/rust/MIR.md) at the `built` phase, printed by `arbace.rust.front.mir`.
- typeck (`typeck.ae`) keeps its results by HIR id for the builder (`body-results`: each expression's type and adjustments, a method
  call's callee, a pattern's type, binding mode and implicit dereferences), with RF10's questions (`with-solver`, `copy-type?`,
  `implements-drop?`, the variants and their discriminants). Two fixes of the earlier stages it found: a `while`'s `break` is an
  expression statement (rustc's `stmt_expr`), so typeck coerces it with NeverToAny; path patterns peel references (match ergonomics).
  `typeck-check` (11,140 of 11,173) and `hir-check` (91 of 91) unchanged.
- **The judge** `build_check.ae`, `bin/rust-front build-oracles` (rustc's `-Zdump-mir=built`, the helper as rustc, the oracle tier) and
  `bin/rust-front build-check`: each body our pipeline builds (the native reader, our expansion, resolution, lowering, collection and
  typeck; std's names and interface the only rustc data) printed, against rustc's built text byte for byte after the dump's header
  line; a body equal but for the allocations rustc prints after it (a string constant's bytes, their ids the session's interning order)
  is counted apart; a negative control differs.
- **The run** `build_run.ae` and `bin/rust-front run-built FILE...`: the record (the oracle tier: the helper's `--mono-mir` dump and
  rustc's executable's status, made once), then with `rustc`, `rustdoc` and `cargo` on `PATH` as stubs exiting 99: our built bodies with
  CleanupPostBorrowck's changes (fake reads, place mentions and ascriptions removed; false edges and unwinds made gotos) and the abort
  strategy's (unwind actions unreachable) take the place of the record's bodies of the same name, the facts that are not the body's own
  carried over from the record (symbols, std's resolved callees, rustc's DefIds in types, the layouts; the layouts of unit, scalars and
  scalar pairs the record lacks computed after `rustc_abi`), then `arbace.rust.back.codegen`, `bin/rsi lower`, gcc `-O0`, the run.
  `RF10_CONTROL=1` (every Add a Sub) must differ.

### Results

- `bin/rust-front build-check` over `test/rust/front/a1`-`a7`: **47 of 54 bodies equal to rustc's built MIR byte for byte, 1 more but for
  rustc's allocations text** (a1, a2, a4, a5 whole: integers with overflow, shift and division checks, `while` loops, `if` chains, `&&`, casts, structs, tuples, arrays with
  bounds checks, references, enums with explicit discriminants and their `match`es, `Option<&i32>`, derived `Clone`); the rest: a
  derive's `AssertParamIsClone` annotation whose span our expansion does not keep (4, RF4's derive spans), a static (`SINK`: refused,
  and its own body not built), a string constant's allocation text (counted apart).
- the corpus (the synthetic programs, `test/rust/sample`, `test/rust/eval`, `test/rust/front`: 88 programs, 510 bodies):
  **92 of 510 bodies equal byte for byte, 4 more but for the allocations' text, 5 programs whole**; 54 differ, 136 refused (closures 35,
  a path 75: 73 without a resolution, the desugarings' lang item functions (`for`'s `into_iter` and `next`, `?`'s `branch`) and
  `format_args!`'s internals, 2 std's constants and statics, overloaded derefs and indexing 13, patterns 5, the rest one or
  two), 224 not built (closures', constants', statics' and promoted bodies: the slice builds functions and discriminants). Light: about
  20 minutes for the 88 programs in one process under 1.5 GB (`GOMEMLIMIT=1500MiB`), a sample in seconds.
- **runs: the six no_std samples a1-a5 and a7 from Arbace's MIR give rustc's exit statuses (5, 212, 41, 17, 45, 21), every body of
  each program ours, the stubs' log empty**; the control differs (a1: 156 for 5).

What the slice refuses (the corpus's classes): closures (RFP20), overloaded operators, derefs and indexing (the trait method calls
THIR makes of them), statics and constants' bodies, `let`-else, guards (the fake borrows), or-patterns, slices, float and char
literals, drops elaborated (ElaborateDrops: the slice keeps built MIR's drops, right for the samples' unconditional ones), the
`println!` family (`format_args!`'s lowering through `Arguments::new` with promoted constants: PromoteTemps). std programs are judged
at the MIR level only: their runs would link the host's native std through rustc as the linker driver (`run-native`), which the stubs
forbid; the ci leg's lowered std is the night's.

### What eval and AOT can move under the decided scoring

Nothing yet, and little before RF12: RES54 (a) scores a stage whose cases consume std's records 0.5, and every Rust program's run
consumes std's MIR or IR (std's generic code is most of a program's mono items, RF1's census) until Tier S builds std from source.
RF10's slices take the program's own bodies off rustc (the nine-in-266 functions of §2.4); the gate's `bin/rsi check` still runs rustc's
IR packs. Even moved onto Arbace's MIR, the eval cell stays 0.5 while the symbols, instances and layouts come from the mono record (RF3's
remaining ports) and std from its records (RF12). `bin/crops-score`: 75.0 / 95.8 / 80.0 before and after.

### Remaining RF10 slices (estimates)

1. Closures and their captures with RFP20 (upvar.rs, `insert_upvar_arg`, closure bodies): 3-4 days.
2. Overloaded operators, derefs and indexing as THIR's method calls, statics and constants (construct_const, allocations), guards with
   fake borrows, or-patterns, slices and ranges in patterns, floats and chars: 3-4 days.
3. The semantic passes to runtime MIR (ElaborateDrops with drop flags, PromoteTemps, Derefer, AddCallGuards, AbortUnwindingCalls,
   RemoveNoopLandingPads, the shims), judged against `-Zdump-mir` after each pass and rustc's runtime MIR: 4-6 days.
4. `format_args!` and the `println!` family (PromoteTemps, `Arguments`' constructors) with drops' unwinding, so the synthetic corpus runs
   through the ci leg: 2-3 days.
5. CTFE (the clean interpreter over MIR forms, §3.8): 6-10 days, unchanged.
Total about 18-27 days with CTFE, within §7's 14-22 for RF10 plus CTFE's share.

### Proposed (RF10)

- **RFP22 The order after the first slice.** Options: (a) **RFP20 (captures, judged) then RF10's slices 1-2 on the corpus, then
  RFP15 with the crates' bodies as the next corpus, then slices 3-5**; (b) RFP15 next as decided (RFP21 (a)), then RFP20; (c) the passes
  (slice 3) next so more programs run. *Recommended: (a)*: closures are the corpus's largest class and RFP20 is decided; RFP15 feeds
  only the crates, whose bodies RF10 cannot yet build (their trait-heavy code needs slice 2).
- **RFP23 The run's record.** The run takes symbols, instances and layouts from the helper's `--mono-mir` record (the oracle tier).
  Options: (a) **keep it until RF3's layout, instance resolution and v0 symbols are ported (RFP1 (a)'s order), the record made by hand
  like the IR packs**; (b) pin the records of the no_std samples as packs now (`test/rust/front/records/`, a few KB each) so
  `run-built` needs no helper at check time; (c) port the v0 symbols and the layouts of the samples' types now. *Recommended: (b) then
  (a)*: a light step could then run the six samples' MIR runs with no toolchain.
- **RFP24 A gate step.** Options: (a) **none until RF11 (RFP4 (a)'s rule)**; (b) `build-check` over `test/rust/front/a*.rs` as an
  essential step (about a minute, under 1.5 GB) with its oracle texts pinned. *Recommended: (a)*, (b) at RFP23 (b).

## Decided (2026-11-08, the user: RFP22-RFP24)

- **RFP22 (a):** RFP20's captures (judged fact by fact) first, then RF10's slices 1-2 on the corpus, then slice 3 (the passes to
  runtime MIR); RFP15 with the crates' bodies after.
- **RFP23 (b) then (c):** the run's symbols, instances and layouts recorded as packs first (`run-built` needing no helper at check
  time), then ported (v0 symbols, instance resolution, layouts).
- **RFP24 (a):** no Rust front-end gate step until RF11; (b), `build-check` over `test/rust/front/a*.rs` as an essential step, at
  RFP23 (b).

## As built: RF10 (second slice), closures, operators and the format machinery (2026-11-08)

The slice follows RFP22 (a): RFP20's captures first (judged), then slices 1 and 2 on the corpus; slice 3 is next. format_args!'s
lowering at the `built` phase belongs with slice 2's paths and literals here (its runtime half, PromoteTemps, stays in slice 4).

Branch `rust-rf10b` (from next `5ec43f7b7`). The brief: RF10's next items in their stated order (slice 1, closures with RFP20, then
slice 2), measured as the first slice measured (`bin/rust-front build-check`: each body our pipeline builds against rustc's
`-Zdump-mir=built` text, byte for byte), the biggest group of failing bodies first. RFP22-RFP24 await the user and were not built.

### The groups, biggest first

The first slice's corpus (88 programs, 510 bodies) left 418 bodies unequal. Measured on it:
1. **closures** (slice 1): 177 closure bodies not built and 35 parents refused on their closures, 212 bodies;
2. **format_args!'s lowering** (slice 4's built-MIR half, taken out of order: it was the second group, 75 bodies refused as "a path",
   and at the `built` phase it needs no PromoteTemps; the runtime half, promotion, stays in slice 4): `Arguments::new`, `from_str`,
   `Argument::new_display` and friends, and with them **user type annotations**, which nearly every body that prints carries;
3. slice 2's operator group: overloaded operators, derefs and indexing (31 refused, more differing), builtin indexing of slices;
4. slice 2's literals and constants: floats, chars, byte strings, named constants (`i32::MAX`, `T::MIN`), array repeats;
5. the long tail (lint scopes of `?`'s arms, `super let` and lifetime extension, drop-and-replace assignments, allocations).

### What was built

- **RFP20 (a), closures' captures judged fact by fact.** The helper's `--captures FILE` (`tools/rsdump/front.rs`'s `captures`): each
  closure of the root file with its kind, `closure_min_captures` in the upvars' order (variable, place string, projections, capture
  kind, mutability) and fake reads. `arbace.rust.front.build`'s port of rustc 1.99.1's `rustc_hir_typeck/src/upvar.rs`
  (`closure_analyze`, `analyze_closure`, `process_collected_capture_information`, `compute_min_captures` with its field sort,
  `restrict_capture_precision`, `restrict_precision_for_unsafe`, `restrict_repr_packed_field_ref_capture`,
  `restrict_precision_for_drop_types`, `adjust_for_move_closure`, `adjust_for_non_move_closure`, `truncate_capture_for_optimization`,
  `truncate_place_to_len_and_update_capture_kind`, `determine_capture_info`, `determine_place_ancestry_relation`,
  `determine_capture_mutability`, `init_capture_kind_for_place` for editions before 2021), over `expr_use_visitor.rs` (`consume_body`,
  `walk_expr`, `walk_stmt`, `walk_local`, `walk_block`, `walk_struct_expr`, `walk_adjustment`, `walk_autoref`, `walk_arm`, `walk_pat`,
  `walk_captures`, `cat_expr` and its kinds, `cat_res`, `cat_projection`, `cat_deref`, `cat_overloaded_place`, `cat_pattern` with
  `enumerate_and_adjust`) with `InferBorrowKind` as the delegate, and `rustc_passes/src/upvars.rs` (`upvars_mentioned`), nested closures
  first. typeck notes the kind an expectation gives (`deduced-kind`); the analysis gives the rest. `bin/rust-front capture-oracles`
  and `capture-check`: **173 of 174 closures equal** (kind, every capture, fake reads) over the 33 programs with closures; the one
  left is a closure inside a user `macro_rules!` expansion whose span our expansion does not keep. A negative control differs.
- **Closure bodies and closure expressions** (slice 1): THIR's `capture_upvar` and `convert_captured_hir_place` (the upvar places as
  THIR, by value or borrowed `&`, `&mut`, `&uniq`), `UpvarRef` and its resolution through the captures
  (`to_upvars_resolved_place_builder`, `find_capture_matching_projections`, `strip_prefix`), `insert_upvar_arg` (the captures'
  debug info, `CapturedPlace::to_symbol`), the closure's environment argument by its kind, `as_rvalue`'s `Closure` with
  `limit_capture_mutability`, the aggregate named by `upvars_mentioned` as rustc's printer names it, and the closure types' `Copy` and
  `needs_drop` from their upvars. **Overloaded calls** (`f(x)` for `F: FnOnce`, `Box<dyn Fn>`, closures): THIR's
  `<F as FnOnce<(A,)>>::call_once(f, (x,))`, the arguments' tuple, typeck noting the call's trait (`:fn-call`).
- **Slice 2's operators:** `is_builtin_binop` and `overloaded_operator` (`<&i32 as Mul>::mul`, `<String as Add<&str>>::add`,
  `PartialEq::eq` on autoref'd operands, `Neg`, `Not`), `overloaded_place` for explicit derefs and for overloaded deref adjustments
  (`*Deref::deref(&x)`, `DerefMut::deref_mut` under a mutable autoref), overloaded indexing (`*Index::index(&v, i)`, `IndexMut`),
  builtin slice indexing (`len_of_slice_or_array`'s `PtrMetadata`, the fake `&raw const (fake)` pointer, `add_fake_borrows_of_base`
  and `read_fake_borrows` for nested indexes), `build_drop_and_replace` for assignments to places that need drop, `Repeat`.
- **Constants:** float literals (rustc_apfloat's 17 or 9 significant digits), chars, byte strings (`escape_ascii`), named constants
  (`NamedConst`: the crate's, std's, an associated one by its type, `const core::num::<impl i32>::MAX`), and **allocations**: each
  string's or byte string's bytes interned per program (`allocate_bytes_dedup`), printed after the body as `write_allocation` does.
- **User type annotations** (`write_user_type_annotation_from_args`, `canonicalize_user_type_annotation`, `is_identity`,
  `instantiate_method_args`, the `TypeOf(PATH at <impl ...>)` printer): value paths (fresh arguments, a type-relative path's written
  self type before its unification with the impl's), method turbofishes, `let` types as `Ty(..)` with their inference variables and
  elided regions canonical, pushed as `declare_bindings`, `expr_into_pattern` and `ascribe_types` push them (the locals'
  `UserTypeProjection`s with their field projections, `AscribeUserType` with `o` or `+`). Their spans: std's macro tokens now keep
  their place in std's sources through expansion (`$crate::format_args_nl!(..)` at `macros.rs:148:28: 148:61`), a derive's
  expression the derive path's span, a derive's assertion its field's.
- **format_args!:** the lang items' associated functions in typeck (`Arguments::from_str`, `core::fmt::rt::Argument`, which std's
  interface lacked: the helper's `--std-iface` now adds the lang item ADTs no public module reaches, with their inherent impls, and
  enums' `#[repr]` and discriminants; `test/rust/front/std-iface.sha256` re-pinned, the old lines unchanged), the placeholders' spans.
- **Scopes:** `maybe_new_source_scope` (a lint attribute's node, `?`'s `#[allow(unreachable_code)]` arms, opens a source scope),
  `resolve_local`'s `super let` (not extended: its bindings in the enclosing expression's temporary scope) and lifetime extension
  (`record_rvalue_scope_if_borrow_expr`, `record_subexpr_extended_temp_scopes`, `extended_temp_scopes` before `temporary_scope`).
- Printing: MIR's types with every free region erased (`fn longest(_1: &str, ..)`), bound ones kept (`for<'a> fn(&'a char)`), the
  object lifetime default of `&dyn Trait` in std's signatures.
- The judge: a body equal but for its allocations' ids (the session's interning order, which follows rustc's order of building the
  bodies, not the source's) is counted apart, after renumbering the ids by first appearance.

### Results

- `bin/rust-front build-check`, the 88 programs, 510 bodies: **92 → 361 bodies equal byte for byte, 25 more but for
  the allocations' ids, 5 → 51 programs whole**; 42 differ, 26 refused (was 54 and 136), the rest not built
  (constants' and statics' bodies, promoted ones). About 7 minutes in one process under 1.5 GB.
- `test/rust/front/a1`-`a7`: 51 of 54 bodies equal and 1 more but for the allocations' ids (were 47 and 1), a1-a5 and a7 whole, a6's static `SINK` refused.
- Checks of the earlier stages: `typeck-check` 11,140 → 11,144 of 11,173 (the helper's expansion), `--ours` 11,138 → 11,142;
  `hir-check --ours` 90 of 91 and one but for whitespace (unchanged); `expand-check` 92 of 92 crates, 857 of 857 items (unchanged);
  `run-built` the six no_std samples a1-a5, a7: 6 of 6 give rustc's status from Arbace's MIR, the stubs' log empty (unchanged).
- `bin/crops-score`: Rust 75.0 / 95.8 / 80.0 before and after (RES54 (a): eval and AOT 0.5 while std's records are consumed).

### What remains to RF12

- RF10's slice 2 rest: or-patterns, guards with their fake borrows, slice and range patterns, `let`-else, `if let` chains in
  `then_else_break`, statics and constants' own bodies (`construct_const`), functional update, qualified paths' annotations,
  trait objects' casts, `?Sized` std ADTs' `needs_drop`; the user annotations of a few std paths (`Rc::strong_count`'s self type,
  `<dyn Any>::downcast`).
- By the decided order (RFP22 (a), RFP23): slice 3 (the passes to runtime MIR) next, then RFP23 (b)'s packs for `run-built` and
  RFP24 (b)'s step, RFP23 (c)'s ports; slice 4's runtime half (PromoteTemps of the format machinery), slice 5 (CTFE); RFP15 for the
  crates; RF11 (Tier U's first light); RF12 (std from source).

### Proposed (RF10's second slice)

- **RFP25 The allocations' ids.** Our ids follow our build order (items in definition order, a body before its closures); rustc's
  follow the order the driver demands `mir_built` (a derived impl's before the user's impls, observed). Options: (a) **keep the class
  "equal but for the allocations' ids" apart, judged with the ids renumbered** (as built); (b) port the driver's demand order
  (`par_hir_body_owners`' order of borrowck and the queries that build MIR before it) so the ids are rustc's. *Recommended: (a)*: the
  ids are an artifact of a session's query order, not of the body; (b) when slice 3 needs the session's order anyway.
- **RFP26 The helper's std interface.** `--std-iface` gained lang item ADTs and enums' repr and discriminants (re-pinned twice today).
  Options: (a) **keep it the helper's (the oracle tier's data, pinned), regenerated by the night's `front-oracles`**; (b) derive the
  three facts natively from std's sources (the native reader over rust-src, RF4's expansion over core) and drop them from the dump.
  *Recommended: (a)* until RF12 builds std from source, (b) with it.
- **RFP27 The capture judge with RFP24 (b).** RFP24 (b) puts `build-check` over `test/rust/front/a*.rs` in the essential gate at
  RFP23 (b). Options: (a) **add `capture-check` over the closures program and `test/rust/front/` to that step then, its oracle pinned
  with the packs (seconds)**; (b) keep the capture judge by hand until RF11. *Recommended: (a)*.

## Decided (2026-11-08, the user: RFP25-RFP27)

- **RFP25 (a):** a body equal but for its allocations' ids stays a class of its own, judged with the ids renumbered.
- **RFP26 (a):** std's interface facts (`--std-iface`: lang item ADTs, enums' repr and discriminants) stay the helper's pinned data,
  regenerated by the night's `front-oracles`, until RF12 builds std from source.
- **RFP27 (a):** `capture-check` (the closures program and `test/rust/front/`, its oracle pinned with the packs) joins RFP24 (b)'s
  essential step when that step lands.

## As built: RF10 (third slice), the rest of slice 2 and slice 3's first passes (2026-11-08)

Branch `rust-rf10b` (continued from `d9a0245e2`), in the user's order (RFP22 (a)): slice 2's rest first, then slice 3, the passes to
runtime MIR, judged against rustc's MIR after each pass (the helper as rustc, `-Zdump-mir`). RFP25-RFP27 decided (above);
RFP23 (b)'s packs and RFP24 (b)'s step not built.

### Slice 2's rest

- **The match lowering, rule for rule** (`builder/matches/{mod,match_pair,buckets,test,util}.rs` of rustc 1.99.1): `FlatPat` and
  `squash_inter_pat` (a binding pushed after its subpattern's), candidates with subcandidates, `match_candidates` with
  `expand_and_match_or_candidates`, `create_or_subcandidates`, `merge_trivial_subcandidates`, `test_remaining_match_pairs_after_or`,
  `sort_match_pairs`, `partition_candidates_into_buckets` with `choose_bucket_for_candidate`'s whole matrix (switch, switchInt with
  the covering ranges of a prior failure bucket, if, slice lengths `==`/`>=`, ranges with `PatRange::contains` and `overlaps`,
  string and scalar equality), `perform_test` for every test (`Le`/`Lt` range comparisons through an intermediate block, slice
  lengths by `len_of_slice_or_array`, `<str as PartialEq>::eq` for string patterns), `lower_match_tree`'s false edges (a guard's
  own on its otherwise block), `MatchTreeBranch::from_candidate` with `sub_branch_bindings`, `bind_pattern` (the first sub-branch's
  drops scheduled), `bind_and_guard_matched_candidate` with `collect_fake_borrows` (shallow fake borrows of the tested places and
  of bound places' dereferenced prefixes, `&fake shallow`), `bind_matched_candidate_for_guard` (each binding's reference for the
  guard, `RefForGuard` locals declared beside the binding's with their debug info), `FakeRead(ForMatchGuard)`,
  `FakeRead(ForGuardBinding)`, the guard's scope (`MatchGuard`) and `clear_match_arm_and_guard_scopes`, a guard's variables through
  their references (`guard_context`), `fixed_temps` for the or-pattern sub-branches of one guard.
- **Patterns** (`thir/pattern/{mod,const_to_pat}.rs`): ranges (`lower_pattern_range`, `a..=a` a constant, full ranges no test,
  `i64::MIN` and `MAX` endpoints), slices and arrays (`prefix_slice_suffix`'s `ConstantIndex` and `Subslice`, `rest @ ..`), `..`
  in tuple and tuple-struct patterns (`enumerate_and_adjust`), box patterns, `&str` constants as a dereference of a `str` constant,
  or-patterns' later alternatives' bindings named by the first's; typeck peels references for range and slice patterns and types
  range endpoints.
- **let-else and `if let`** (`block.rs`' let-else: the else block built first, its end unreachable, `lower_let_expr` in an if-then
  scope of the last remainder scope; `then_else_break`'s `Let`, its bindings in the `if`'s variable source scope); `while let` with it.
- **Constants' and statics' own bodies** (`construct_const`: the region tree's `var_parent` none, temporaries unscheduled), static
  references (`StaticRef`: `const {allocN: &T}`, `*mut T` for a mutable static, the static's allocation `alloc1 (static: S, size: 1,
  align: 1)` with its bytes for literal, array and `&str` initializers, a pointer printed as `write_allocation_bytes` prints
  provenance), **anonymous constants** of array lengths and repeat counts (`NAME::{constant#N}`, numbered per owner, a closure's its
  own; the ones inside a macro call after the rest, as def collection meets them during expansion; a const parameter's path none).
- **Expressions:** functional update (`AdtExprBase::Base`: the missing fields read from the base place), union aggregates,
  `write_box_via_move` (`vec![..]`'s `(*b).1.0.0 = [..]` with no array temporary), unit variants cast to integers
  (`discriminant_def_for_variant`: `Add(const E::A::{constant#0}, const 0_u8)`), `u8 as char`, pointer and fn-pointer casts,
  closure and fn-item pointer coercions' kinds (`ClosureFnPointer(Safe)`, `ReifyFnPointer(Safe)`), byte literals, string literals
  unescaped and printed as `<str as Debug>` prints, `u8::MAX`, empty allocations.
- **Elsewhere:** the builtin `DiscriminantKind` projection (an enum's repr type, else u8) in typeck's normalization; a derive's
  unresolved variant path by its constructor's kind; needs_drop of std ADTs by their Drop impls (a Drop impl is as generic as its
  type) and of projections by their normal form; an inherent impl's own regions printed unerased in a value path
  (`<(dyn Any + Send + 'static)>::downcast_ref`); a path's written arguments' elided regions canonical region variables.

### Slice 3: the passes after MIR building

`arbace/rust/front/passes.ae` (`arbace.rust.front.passes`) ports mir_built's passes that change a body after the `built` dump
(`rustc_mir_transform/src/lib.rs`): `LintAndRemoveUninhabited` (a call into an uninhabited destination loses its return) and
`SimplifyCfg-initial` (`simplify.rs`: `CfgSimplifier`'s predecessor counts over the preorder, `collapse_goto_chain`,
`merge_successor`, `simplify_branch` off before the runtime phases, then `remove_dead_blocks` with the deduplication of empty
unreachable blocks), over successors in `Terminator::successors`' order. `bin/rust-front build-oracles` now dumps
`-Zdump-mir=built|SimplifyCfg-initial` (`RF_DUMP_MIR`), and `BUILD_PASS=SimplifyCfg-initial bin/rust-front build-check` judges our
bodies through the passes against rustc's after the pass, byte for byte (a trivial constant has no pass dump: rustc skips its
passes).

### Results

- `bin/rust-front build-check`, the 88 programs, 510 bodies: **361 → 415 bodies equal byte for byte, 25 → 29 more but for the
  allocations' ids, 51 → 58 programs whole**; 42 → 31 differ, 26 → 13 refused (a cast 1, an expression 4, a closure's fake reads 1,
  a path 4, a method call without its callee 1, a variable 1, an aggregate 1), 22 not built (thread-local statics' and macro-made
  items' anonymous constants and closures, promoted bodies: was 81). About 7.5 minutes in one process under 1.5 GB.
- `test/rust/front/a1`-`a7`: **54 of 54 bodies equal, all seven programs whole** (were 51 and 1, a6's static refused).
- **Slice 3, `BUILD_PASS=SimplifyCfg-initial`: 379 of 475 bodies equal to rustc's MIR after SimplifyCfg-initial, 29 more but for
  the allocations' ids, 3 but for their text, 56 programs whole** (475: the bodies rustc runs the passes on; trivial constants have
  no pass dump); every body equal after `built` and not a trivial constant is equal after the pass but for the allocations' text.
- Checks of the earlier stages, unchanged: `typeck-check` 11,144 of 11,173 (`--ours` 11,142), `hir-check --ours` 90 of 91 and one but
  for whitespace, `expand-check` 92 of 92 crates and 857 of 857 items, `capture-check` 173 of 174 closures, `run-built` 6 of 6 no_std
  samples from Arbace's MIR (the stubs' log empty). The negative controls differ.
- `bin/crops-score`: Rust unchanged (no record of the cells changed; RES54 (a) scores eval and AOT 0.5 until RF12).

### What remains to RF12

- Slice 2's tail: the closures' fake reads (`FakeRead(ForLet(Some(DefId(0:19 ~ drops[c62a]::..))))`: a crate's `StableCrateId`
  hash in the text, RFP29), `if let` guards, deref patterns, constant patterns of named constants, the derive's `Some(Ord::cmp(..))`
  typeck gap, floats printed in exponent form (`1.0E+30f64`) and `f64::NAN`'s path, the private std ADTs' drops (RFP28),
  statics' values beyond literals, arrays and `&str` (CTFE, slice 5), a `<static(DefId(..))>` constant (RFP29's hash again).
- Slice 3's rest, in rustc's order: `PromoteTemps` with `SimplifyCfg-promote-consts` (mir_promoted), then the analysis-to-runtime
  passes (`CleanupPostBorrowck`, `RemoveNoopLandingPads`, `SimplifyCfg-post-analysis`, `Derefer`, `CriticalCallEdges`,
  `ElaborateDrops` with drop flags, `AbortUnwindingCalls`, `ElaborateBoxDerefs`, `LowerIntrinsics`, `RemovePlaceMention`,
  `SimplifyCfg-pre-optimizations`), each judged against its dump as SimplifyCfg-initial is; then RFP23 (b)'s packs and RFP24 (b)'s
  step with RFP27 (a); slice 4 (format_args!'s promotion), slice 5 (CTFE); RFP15; RF11; RF12.

### Proposed (RF10's third slice)

- **RFP28 The private std ADTs' fields.** `needs_drop` of a std ADT reached only as a private field (`FlattenCompat`,
  `FloatErrorKind`, `JoinInner`) has no fields in `--std-iface`, so it is assumed to need drop (right for `JoinInner`, wrong for the
  other two: 2 bodies). Options: (a) **the helper's `--std-iface` adds every ADT a public ADT's fields reach, transitively (a
  rebuild of the helper, the interface re-pinned, only additions)**; (b) the helper records rustc's `needs_drop` answer per type the
  corpus meets (a fact table, no fields); (c) keep the assumption until RF12. *Recommended: (a)*: the same kind of data as RFP26 (a)'s,
  and typeck's field types of private ADTs need it too.
- **RFP29 The crate hash in MIR text.** Two texts print a `DefId` with the crate's `StableCrateId` (`drops[c62a]`): a closure's fake
  read's cause and a static's pointer constant. Options: (a) **compute `StableCrateId` as rustc does (the crate name and `-C metadata`
  hashed with `StableHasher`, SipHasher13 over rustc's encoding) and the `DefIndex` in def-collection order**; (b) a class "equal but
  for the crate hash", the hash and index renumbered as RFP25 (a) renumbers allocations; (c) refuse those bodies. *Recommended: (b)
  now, (a) with RF11's v0 symbols (RFP23 (c)), which hash the same id*.

## As built: RF10 (fourth slice), slice 3's passes to runtime MIR (2026-11-08)

Branch `rust-rf10b` (continued from `8052a77c0`). The brief: the passes after mir_built in rustc's order, each judged against rustc's
own dump after it, as SimplifyCfg-initial was; RFP28 and RFP29 await the user and were not built (the bodies they hold back are
counted below); then slice 2's tail where no decision is needed.

### The passes (`arbace/rust/front/passes.ae`)

rustc 1.99.1's order (`rustc_mir_transform/src/lib.rs`: `mir_promoted`, `run_analysis_cleanup_passes`, `run_runtime_lowering_passes`,
`run_runtime_cleanup_passes`), each a function over MIR forms, `passes/pipeline`:

- **PromoteTemps** (`promote_consts.rs`, rule for rule): `collect_temps_and_candidates` (TempState over the reverse postorder, the MIR
  visitor's place contexts as `Visitor::super_*` gives them, a projected place's context Projection), `Validator` (`validate_local`,
  `validate_place` with the static and constant-index cases, `validate_operand`, `validate_ref`, `validate_rvalue` with the division
  rules, `validate_call` with `#[rustc_promotable]` and const fns in initializers' safe blocks) over the qualifs of
  `rustc_const_eval/src/check_consts/qualifs.rs` (`HasMutInterior` by Freeze, `NeedsDrop`, `in_rvalue`, `in_place`, `in_operand`;
  a named constant's `mir_const_qualif` approximated by its type), and `Promoter` (`promote_candidate`, `promote_temp` for
  statements and calls, keep_original for temporaries used twice, the promoted reference's local in the candidate's source scope,
  the extra statements in reverse location order, the promoted-out temporaries' assignments, storage and drops removed). The
  promoted bodies (`NAME::promoted[N]`) join the pipeline at ImpossibleClauses, as rustc's `promoted_mir` runs them, with their
  allocations. The statements now carry their source scope as metadata (`build.ae`'s `push!`), the type forms their typeck type
  (`ty-form`'s `:tc`), the locals whether they are user variables.
- **SimplifyCfg** (all four: initial, promote-consts, post-analysis, pre-optimizations) with `strip_nops` first and `simplify_branch`
  from the runtime phases on (`preserve_switch_reads` off); `remove_dead_blocks` on its own for AbortUnwindingCalls.
- **CleanupPostBorrowck** (`cleanup_post_borrowck.rs`): AscribeUserType, FakeRead, fake borrows and drop hints `nop`, the two
  pointer coercions `PtrToPtr`, false edges gotos, the user type annotations cleared.
- **RemoveNoopLandingPads** (`remove_noop_landing_pads.rs`, when the session unwinds): the no-op landing pads in postorder, MirPatch's
  resume block (the last statement-less resume block, else a new one), `unwind continue` for cleanup edges into them.
- **Derefer** (`deref_separator.rs`): every place but debuginfo's with a deref past its first projection split through `deref_copy`
  temporaries (MirPatch's `add_assign` and `apply`, stable by location), the temporaries marked as deref temporaries.
- **AddCallGuards** (CriticalCallEdges): a call that may unwind into a cleanup returning to a block of several predecessors returns
  through a new block.
- **PostAnalysisNormalize** (`post_analysis_normalize.rs`): opaque types revealed, each by its defining function's hidden type (the
  type of that function's body block in typeck's results); our typeck's opaque types carry no `OpaqueTy` id, so an opaque type is
  matched by the signature outputs equal to it and left as it is when two functions define it alike (RFP33).
- **ElaborateDrops** (`elaborate_drops.rs`, `elaborate_drop.rs`), with the dataflow it stands on: `MoveData::gather_moves`
  (`move_paths/{mod,builder}.rs`: move paths of the places whose types need drop, first child newest, `move_path_for`'s stops at
  references, destructors and slices, unions, `MovePathLookup::find`, the `UnDerefer` chains of deref temporaries; moves out,
  StorageDead a move out, inits Deep or NonPanicPathOnly), `MaybeInitializedPlaces` (exclude_inactive_in_otherwise,
  skipping_unreachable_unwind, precise_enum_drop_elaboration's mutable borrows and switch edges) and `MaybeUninitializedPlaces`
  (mark_inactive_variants_as_uninit, the dead unwinds) to their fixpoints over `framework/direction.rs`' Forward edges
  (`get_terminator_edges` before the terminator's effect, a call's return effect on its return edge, a switch's per-edge effects),
  `drop_flag_effects.rs`; then `ElaborateDropsCtxt` (`collect_drop_flags`, `elaborate_drops`, the flags on init, for calls' returns,
  arguments and locations) and `DropCtxt`'s drop styles (dead, static, conditional, open), `drop_flag_test_block`,
  `drop_flag_reset_block`, `drop_ladder` with its unwind half-ladder, `open_drop_for_tuple`, `open_drop_for_adt` with
  `open_drop_for_multivariant` and `adt_switch_block`, a destructor's `<T as Drop>::drop` call, Box's `open_drop_for_box_contents`;
  arrays' and slices' drop loops and async drops are refused (none in the corpus).
- **AbortUnwindingCalls** (`abort_unwinding_calls.rs` over `layout.rs`' `fn_can_unwind`): the intrinsics never unwind, a session
  that aborts on panic unwinds nowhere but through foreign items, extern "C" bodies and calls by their ABI; then remove_dead_blocks.
- **EraseDerefTemps** (`no_retag copy`), **ElaborateBoxDerefs** (`elaborate_box_derefs.rs`: `copy (b.0: Unique<T>) as *const T
  (BoxDerefTransmute)`), **LowerIntrinsics** (`discriminant_value`, `unreachable`, `forget`, `assume`, the wrapping and unchecked
  operators), **RemovePlaceMention**.
- Identities where the corpus has nothing to change: ImpossibleClauses, Subtyper, AddMovesForPackedDrops, StateTransform.

`#[rustc_promotable]` is a table of the nine functions rust-src 1.99.1's library marks (`ptr::null`, `ptr::null_mut`, `mem::size_of`,
`mem::align_of`, the integers' `min_value`/`max_value`, `RangeInclusive::new`, `RawWaker::new`, `RawWakerVTable::new`), read from the
pinned sources (RFP32).

### The judge

`bin/rust-front build-oracles` dumps every pass of the pipeline (`RF_DUMP_MIR`'s default, 21 passes; the 88 programs' dumps 193 MB,
`.before.mir` with `.after.mir`), and `BUILD_PASS=NAME bin/rust-front build-check` runs the pipeline to NAME on every built body (in
the builder's bindings, `build/*after-finish*`) and judges every pass to NAME against its own dump: a pass that rustc runs again in a
later phase (RemoveNoopLandingPads, AddCallGuards) or in a shim is judged at its first phase's dump (`passes/pass-phase`).
`BUILD_FIRST=1` prints each body's first pass where it is not equal; `PASS_TRACE=FILE` appends each pass as it starts. Built MIR is judged as before (`BUILD_PASS` unset).

### Slice 2's tail

- Floats printed as `rustc_apfloat`'s `Display` prints them (`ieee.rs`: the exact digits with the low ones past the precision's
  bits discarded, half-up rounding, the scientific form past the width of 3: `1.0E+10f32`, `1.0E+30f64`, `1.2E-4f64`).
- Coercion casts (`cast.rs`' `try_coercion_cast`): a cast to the type its operand already has is a Use (`7.5 as F`).
- A derive's unresolved tuple-struct path is its constructor (`Id(Clone::clone(&self.0), ..)` an aggregate, not a call).
- `if let` guards, deref patterns and named-constant patterns: none of the corpus's failing bodies needs them now (the patterns
  program is whole but for its allocations' ids); not built.

### Results

- `BUILD_PASS=SimplifyCfg-pre-optimizations bin/rust-front build-check`, the 88 programs (about 8 minutes, one process under 1.8 GB
  with `GOMEMLIMIT`), equal / but for the allocations' ids / of the bodies rustc dumps after the pass:

  | pass | equal | ids | of |
  |---|---|---|---|
  | SimplifyCfg-initial | 382 | 29 | 475 |
  | PromoteTemps, SimplifyCfg-promote-consts | 383 | 29 | 475 |
  | ImpossibleClauses (the promoted bodies join) | 496 | 43 | 616 |
  | CleanupPostBorrowck, SimplifyCfg-post-analysis, Derefer, AddCallGuards | 497 | 45 | 616 |
  | RemoveNoopLandingPads (unwinding programs only) | 453 | 45 | 572 |
  | PostAnalysisNormalize to RemovePlaceMention (each) | 497 | 44 | 616 |
  | SimplifyCfg-pre-optimizations | 499 | 46 | 616 |

  60 programs whole through every pass (was 56 after SimplifyCfg-initial). **No body is lost by a pass**: every body not equal after
  a pass is already unequal at the built level (55 bodies first differ at SimplifyCfg-initial, as built MIR), but for two promoted
  bodies whose `Option<_>` is typeck's gap (an unresolved `None` in `fmt` and `std`) and promoted bodies of parents not built.
- Built MIR (`bin/rust-front build-check`): 415 → 418 of 510 equal, 29 but for the allocations' ids, 0 but for their text (was 3),
  refused 13 → 12, 58 → 60 programs whole.
- `test/rust/front/a1`-`a7`: 54 of 54 bodies equal at built, 52 of 52 after every pass (a5's two trivial constants have no pass
  dumps), all seven whole (`make rust-front-pass-check`).
- The bodies RFP28 holds back: `float-f32`'s and `float-f64`'s `main` (`Result<f32, ParseFloatError>` needs drop by our assumption:
  `FloatErrorKind` has no fields in `--std-iface`), 2 bodies at every phase. RFP29's: `drops`' `main` (a closure's fake read names
  the crate hash), refused, 1 body.
- Checks of the earlier stages: `expand-check` 92 of 92 crates and 857 of 857 items, `hir-check --ours` 90 of 91 and one but for
  whitespace (unchanged); `typeck-check --ours` 11,142 of 11,173; `capture-check` 173 of 174 closures; `run-built` 6 of 6 no_std samples from Arbace's MIR (the stubs' log empty). The negative controls differ.
- `bin/crops-score`: Rust unchanged (no record of the cells changed).

### What remains to RF12

- Built MIR's 92 bodies (slice 2's tail, unchanged in kind): the unwind tree's block numbering in long bodies (`algorithms`,
  `collections`, `errors`, `structs`), user type annotations of a few std paths (`Rc::strong_count`, `Result<..>` ascriptions in
  `std` and `errors`), typeck's `Option<_>` and `{?}` gaps, qualified paths in expressions (`:qpath`), statics of thread locals,
  const generics' paths; RFP28's two and RFP29's one.
- The passes: arrays' and slices' open drops (drop loops), async drops, Subtyper's projections, AddMovesForPackedDrops,
  StateTransform (coroutines), named constants' own qualifs (`mir_const_qualif` by dataflow over the constant's body); none is met
  by the corpus yet.
- Then, by the decided order: RFP23 (b)'s packs (now with every pass's dumps) and RFP24 (b)'s step with RFP27 (a); the optimization
  passes rustc runs at `-C opt-level=0` (CheckAlignment to SimplifyCfg-final, the codegen's input); slice 4 (format_args!'s runtime
  half), slice 5 (CTFE); RFP15; RF11; RF12.

### Proposed (RF10's fourth slice)

- **RFP30 The pass judge in the essential step.** RFP24 (b)'s step judges `test/rust/front/a*.rs`' built MIR. Options: (a) **the step
  runs `BUILD_PASS=SimplifyCfg-pre-optimizations` over a1-a7 too (52 bodies through all 21 passes, about 40 s more), the packs
  carrying their pass dumps' `.after.mir` files**; (b) keep the pass judge by hand (`make rust-front-pass-check`) until RF11.
  *Recommended: (a)*: the passes are as deterministic as the builder, and they are what RF11's codegen will read.
- **RFP31 The oracles' size.** The 88 programs' dumps are 193 MB (every pass's `.before.mir` beside its `.after.mir`; the
  optimization passes' with them when `RF_DUMP_MIR=all`). Options: (a) **`build-oracles` keeps only the `.after.mir` files of the
  listed passes (about half), the packs likewise**; (b) only the dumps that differ from the previous pass's (smaller, but the judge
  then needs the chain); (c) as is. *Recommended: (a)*.
- **RFP32 std's function attributes.** PromoteTemps needs `#[rustc_promotable]`, AbortUnwindingCalls `#[rustc_nounwind]`
  (`NEVER_UNWIND`), const-fn promotion in initializers std's constness. As built: a table of the nine promotable functions read from
  the pinned rust-src by hand, and the intrinsics' path as the only never-unwinding functions. Options: (a) **the helper's
  `--std-iface` records these three attributes for every std function (pinned data as RFP26 (a)'s, regenerated by the night's
  `front-oracles`)**; (b) read them natively from rust-src's library with the native reader (no rustc, but ahead of RF12's std
  from source); (c) keep the table. *Recommended: (a) now, (b) with RF12*: the corpus meets only the table's functions, but std has
  more `rustc_nounwind` functions than the intrinsics (`panic_nounwind`, `abort`), which a program calling them would show.
- **RFP33 Opaque types' identity.** typeck's `[:opaque clauses]` carry no `OpaqueTy` id, so PostAnalysisNormalize reveals an opaque type
  by the signatures equal to it (two functions returning `impl Fn(i32) -> i32` would be ambiguous and left unrevealed; generic
  definers' hidden types are not substituted). Options: (a) **typeck's opaque type carries its OpaqueTy def id (collect already
  makes the def), its hidden type recorded by the defining body's typeck (`TypeckResults::concrete_opaque_types`)**; (b) keep the
  structural match. *Recommended: (a)*: it is rustc's representation and typeck-check judges it.

## As built: RFP23 (b), RFP24 (b) (2026-11-08)

Branch `rust-rf10b` (continued from `7c7fe2d63`), the user's decision of 2026-11-08: RFP23 (b) first (the run's symbols, instances and
layouts recorded as packs, `run-built` needing no helper at check time), then RFP24 (b) with RFP27 (a) (`build-check` over
`test/rust/front/a*.rs` an essential step, its oracle texts pinned, `capture-check` in it). RFP23 (c) (the ports) is next; RFP28-RFP33
await the user and were not built.

### The packs (`bin/lib/repr-rust/front-records.bash`)

Four packs in the oracle-records format (doc/ORACLE-RECORDS-SURVEY.md §3.2, `test/oracle-records/oracle.bash`: a deterministic tar.xz
in the store `.tmp/oracle-packs/`, its pin `test/rust/records/NAME.sha256` and manifest `NAME.tsv` committed), one kind each, made by hand
on the host with rustc and the helper (`bin/rust-front records make [KIND...]`, ORS5 (a)), `:rustc b789fd432132e37d` (rustc 1.99.1,
Alpine r0), the helper at `6378914b5a9b...` (`tools/rsdump`'s source digest):

| Pack | What | Members | Raw | xz | SHA-256 |
|---|---|---|---|---|---|
| `rust-front-std` | the helper's `std-names.edn` and `std-iface.edn` (RFS4, RFP26 (a)), their digests `test/rust/front/std-*.sha256`'s (checked at the make) | 2 | 18.2 MB | 514,512 | `a0375288...` |
| `rust-front-built` | rustc's MIR after mir_built and after each of the 21 passes to runtime MIR (`-Zdump-mir`, the `.after.mir` files: the judge reads no `.before.mir`, RFP31 (a)'s half) of a1-a7 | 1,108 | 0.97 MB | 24,580 | `9e3e5586...` |
| `rust-front-captures` | the helper's `--captures` of `test/rust/synthetic/closures.rs` and the seven `test/rust/front/` programs with a closure (RFP27 (a)) | 8 | 2 KB | 840 | `bc8cd109...` |
| `rust-front-mono` | `run-built`'s record of a1-a7 (RFP23 (b)): the helper's `--mono-mir` dump (each mono item's optimized body, its symbol and instance, std's and core's resolved callees, the layouts) and rustc's executable's exit status and output | 21 | 145 KB | 11,108 | `b44d85ab...` |

The packs total 551 KB in the store; the committed side is the four pins and manifests, 208 KB (the built pack's manifest, one line a
dump, 202 KB of it). An entry's key: the kind, the program's path and the SHA-256 of its bytes, the helper's source digest
(`bin/repr-rust`'s `helper_hash`), the flags (`build-oracles`', `run-built`'s and `capture-oracles`' as they were) and, for `built`, the
passes dumped; the toolchain's identity is the manifest's header's, not the key's, so the check computes keys with no rustc. A
program the check needs whose key the manifest lacks is UNRECORDED, one whose key moved (the program, the helper's source or the
flags changed) STALE: either fails (`rfr_stage`, checked: a2.rs with a comment appended is STALE, a missing program UNRECORDED). The
mono record's one path, the checkout's in the locations' file names, is written `@ROOT@/` and put back at the read.
`bin/rust-front records verify [KIND...] [--sample N]` makes every entry afresh into `.tmp/rust-front/records/verify/` and compares it
member by member (ORS6 (a)'s verdict: under the identity the record names a difference fails, under another rustc it is reported),
then each kind's crossed pair (ORS7 (a): the first program afresh against the second's member; std: the names against the
interface), which must differ. Run here: 1, 7, 8 and 7 entries equal, the four controls differing. Kinds `rust front-std`,
`front-built`, `front-captures`, `front-mono` in `test/oracle-records/kinds.edn` (`bin/oracle-records list`, `pins`, `verify rust`).

### The readers

- `bin/rust-front build-check --records [NAME...]` and `capture-check --records [NAME...]`: std's data and the captures linked from the
  staged packs into `.tmp/rust-front/records/front/`, rustc's MIR the unpacked `rust-front-built`; no helper.
- `bin/rust-front run-built [--records] [FILE...]` (default a1-a7): the record from `rust-front-mono` when the program is recorded at
  its key; `--records` only from it (else the program fails); `RF_LIVE=1`, or a program not recorded, made live by the helper (by
  hand, the oracle's way). The lowered C now runs on ci (`bin/ci run --std gnu23`: repr/c, ci's checker and builder, musl's forms,
  cirun) instead of gcc -O0 (`RF10_CC=gcc` by hand), so the run needs no C compiler either; rustc, rustdoc, cargo, gcc, cc and clang
  are stubs exiting 99 first on PATH (`RF10_STUBS=0`: not).
- a6 runs now (it was left out: codegen refused it): `arbace.rust.back.codegen` takes `volatile_store` and `volatile_load` (and the
  unaligned ones) as rustc_codegen_llvm's `intrinsic.rs` lowers them (`store volatile`/`load volatile` of the immediate, the
  pointee's alignment, 1 unaligned), and a static the allocation's data defines (rustc's `codegen_static`: an internal global of its
  bytes); `arbace.rust.front.build-run` turns our bodies' `(static-ptr ID)` into rustc's `(ptr allocS<ID> 0 8)` with the record's
  static (its symbol, by the static's name) and our own evaluation of its initializer (the bytes of our allocation's text; writable,
  the record not saying `mut`), and our `(raw-str ID)` and `(raw-bytes ID)` into rustc's `(slice allocB<ID> LEN)` and pointer with
  an immutable memory allocation of their bytes (the record's own bodies keep theirs).

### The essential step (RFP24 (b), RFP27 (a))

`bin/rust-front essential` (`make rust-front-essential`): with rustc, rustdoc, cargo, gcc, cc and clang stubbed first on PATH,
`build-check --records` (a1-a7), `capture-check --records` (closures and the seven `test/rust/front/` programs), `bin/ci musl ensure` (ci's musl forms
current), `run-built --records` (a1-a7) and the run's negative control (`RF10_CONTROL=1` on a1: every Add a Sub, its status must differ from the
record's); the three summary lines held to `test/rust/front/essential-known.txt` line for line (`--record` rewrites it, by hand), the
crossed pairs of build-check (a body's text against another body's) and capture-check (a closure's line against another program's)
and the run control differing (`test/rust/front/controls.edn`), the stubs' log empty. The gate step `rust-front-essential`
(`test/gate/steps.edn`, chain `rust-essential` after `rsi-judge-test`; `:mem-gib 2`, `:hosts [:stage0 :go]`, the four pins and musl's
`test/c/corpora.edn`, inputs `:rust :rust-helper :c :c-run` with `bin/rust-front` and `test/oracle-records/`; its control in
`:controls`), and the oracle tier's `rust-front-records-verify` (`bin/repr-rust helper && bin/rust-front records verify`, `:phase
:oracle`, `:hosts [:stage0 :rustc]`, its crossed pairs waived as the verify's own). `gate.crops check`: 0 problems; `bin/gate --plan`:
`run rust-front-essential ... never passed`; `bin/gate --oracle --plan`: `run rust-front-records-verify`.
RFP30 (the pass judge in the step) awaits the user: the step judges built MIR; the pack already holds every pass's dumps, so
RFP30 (a) is the command's `BUILD_PASS=SimplifyCfg-pre-optimizations` and a line of the known file.

### Results

- `bin/rust-front essential` (and `--record`, which wrote `test/rust/front/essential-known.txt`), main's stage 0, with the six tools
  stubbed: **build-check 54 of 54 bodies equal to rustc's built MIR byte for byte, 7 programs whole; capture-check 30 of 31 closures
  equal; run-built 7 of 7 programs from Arbace's MIR give rustc's status and output** (a1-a7: 5, 212, 41, 17, 45, 6, 21; a6 newly), the
  three controls differing (the run control a1: 156 for 5), the stubs' log empty; 6 min 21 s (`--record`) and 8 min 36 s (the check against the known file, with `bin/ci musl ensure`), peak
  1.44 GB (`GOMEMLIMIT=1500MiB`):
  build-check 54 s and 1.36 GB, capture-check 65 s and 1.38 GB, each run about 45 s (most of it ci reading the lowered C with
  musl's forms). The one closure not equal is `expand1`'s `(|| $b)()` inside a `macro_rules!` body: rustc's span is the definition
  site's (41:21-41:28), ours none (the closure's tokens joined across the macro's definition and its call site), so the closure has no
  line; RFP34 below.
- `RF10_CC=gcc bin/rust-front run-built --records`: the same 7 of 7 with gcc -O0 (3 min 18 s), so ci and gcc agree on the seven.
- `bin/rust-front records verify`: every entry equal, the four crossed pairs differing; `bin/oracle-records pins`: the four packs in the
  store with their members, the tampered control refused.
- `gate.crops check`: 0 problems. `bin/crops-score --judge --strict`: Rust 75.0 / 95.8 / 80.0 unchanged (maturity's check families are
  the REAP row's, 3 of 3 with controls, and no REAP cell reads the front end's checks; and the eval stage stays 0.5: every run still consumes the mono record's
  symbols, instances, layouts and core's bodies, `test/crops/independence.edn`'s Rust eval and judge facts now name the step).

### What remains

- RFP23 (c), decided: the ports that take the mono record off the run: the v0 symbols (`rustc_symbol_mangling/src/v0.rs`, with
  the crate hash RFP29 names), instance resolution (`Instance::resolve` and the collector's mono items, RF3's
  `arbace.rust.back.collect` grown to the program's own items), and the layouts (`rustc_abi`'s `layout_of` for the samples' types,
  started for scalars and pairs in `build_run.ae`). Then the run takes from the pack only core's bodies, the part RF12 removes.
- RFP28-RFP33 await the user; the expansion's macro-site spans (RFP34).

### Proposed (RFP23 (b), RFP24 (b))

- **RFP34 Closures in `macro_rules!` bodies.** `expand1`'s closure `(|| $b)()` has rustc's span at the macro's definition (Span::to of
  the closure's first and last tokens, both the definition's once `$b` is wrapped); our expansion joins a definition-site token with a
  call-site one and the closure gets no root-file span, so capture-check cannot name it (1 of 31 in the step, 1 of 174 overall).
  Options: (a) **port `Span::to`'s rule for tokens of different contexts (the outer expansion's span, `rustc_span/src/lib.rs`
  `Span::to` with `find_ancestor_in_same_ctxt`) in RF4's expansion, so the closure's span is the definition site's**; (b) keep it a
  known line of `essential-known.txt` until RF4's derive spans are taken up. *Recommended: (a)*, with RF4's derive spans (the same
  machinery): about half a day, judged by capture-check and expand-check.
- **RFP35 The packs' key and the helper.** The key holds the helper's whole source digest (as Haskell's and Rust's other records do), so
  any change to `tools/rsdump` (a new mode for another check) makes all four packs STALE and fails `rust-front-essential` until they
  are made again by hand on the host with rustc (30 s). Options: (a) **as built: the whole helper's digest**; (b) the digest of the
  modes' sources only (`front.rs`, `mirdata.rs`, the driver), a change elsewhere keeping the packs; (c) no helper digest, freshness left
  to the oracle tier's verify. *Recommended: (a)*: the records are the helper's outputs, a re-make costs 30 s, and (b) would trust a
  hand-kept list of which files a mode reads.
- **RFP36 The essential step's runs.** The seven runs are 5 to 7 of the step's 6.5 to 8.5 minutes (ci reads each program's lowered C with musl's
  forms). Options: (a) **as built: all seven in the essential step**; (b) a1 and a6 (the scalar and the unwinding programs) in the
  essential step, the seven in `--full`; (c) the runs batched into one ci session (`arbace.c.run nodes` over seven programs: one read of
  musl's forms), about 2 minutes. *Recommended: (c) then (a)*: the same coverage at a third of the time; (b) if (c) waits.

## As built: RFP23 (c) (2026-11-09)

Branch `rust-rf10b` (continued from `88ad9637d`), the user's decision of 2026-11-08: after RFP23 (b)'s packs, port the run's facts so
the runs stop reading rustc's mono record. RFP28-RFP36 await the user and were not built.

### What was ported (`arbace/rust/front/mono.ae`)

From rustc 1.99.1's sources (rust-src's `compiler/`, the same revision as every other port; LICENSE.md, doc/ARBACE.md):

- **The crate's identity.** `StableCrateId::new` (`rustc_span/src/def_id.rs`) over rustc-stable-hash 0.1.2's SipHasher128 (SipHash-1-3,
  128-bit; the stream of `str::hash`'s bytes and 0xFF, `"metadata"`, no `-C metadata`, `"exe"`, the compiler's `cfg_version`). For a1
  it gives `0x799ecbb0b009f827`: `a1[799e]` in the DefIds and `Csarn Gkp3JIMJ_` in the symbols, as rustc's. `__rustc`'s disambiguator
  (`mangle_internal_symbol`) is the same hash of `cfg_version` alone. Std's crates (core, alloc, std) are built with Cargo's
  `-C metadata`, so their ids are read, not computed: `read-rmeta-root` decodes an `.rmeta`'s header (`METADATA_HEADER` v10, the
  root's position, the version string) and `CrateRoot`'s first fields (the triple, the SVH, the name, `extra_filename`,
  `stable_crate_id`) natively; `bin/rust-front std-crates` writes `test/rust/front/std-crates.edn` from the sysroot (by hand, where
  rustc's sysroot is; `--check` compares: equal here), and the runs read that file, std's build identity as `rust-front-std` is its
  interface.
- **v0 symbols** (`rustc_symbol_mangling/src/{lib,v0}.rs`): `symbol_name_from_attrs` (a weak lang item's link name, `rust_begin_unwind`
  through `mangle_internal_symbol`, `rust_eh_personality` kept, `no_mangle`), `compute_symbol_name`'s instantiating crate (generic
  arguments, or `instantiation_mode` GloballyShared with conflicts: `rustc_middle/src/mono.rs` with `cross_crate_inlinable`'s attribute
  rules and `requires_inline`), and the mangler itself: `default_print_def_path` (own generic arguments, a trait's without Self), impl
  paths (`X`/`M`, the impl's disambiguator, self type and trait), types with their backrefs (`B`), array lengths, the base-62
  integers, identifiers with punycode (RFC 3492's encoder, as the punycode crate). ReifyShims (`::{shim:reify#0}`) for a
  `#[track_caller]` function as a constant.
- **Mono items and callees** (`rustc_monomorphize/src/collector.rs`'s roots and uses, `Instance::try_resolve` for the calls the samples
  make): the roots (non-generic local functions with an extern indicator, the entry), the calls of Arbace's own bodies (local items;
  core's generic or inlinable functions as items whose bodies are core's), drops (`drop_glue::<T>` and the local `Drop::drop` it
  calls); each callee's kind (item, intrinsic: `#[rustc_intrinsic]`), symbol, path, can-unwind (`#[rustc_nounwind]`, the ABI),
  track-caller and `:external` (`should_encode_mir`: generic or cross-crate-inlinable), each call's caller location from the call
  expression's span (the builder now keeps it on the callee operand's metadata). Core's attributes come from core's own sources
  (rust-src's `library/core`, pinned by `tools/rsdump/apks.sha256`): the item found through the module tree (`mod` items, `#[path]`,
  inline modules, an impl by its index) and read by repr/rust's native parser, `cfg_attr` evaluated for core's build; an item a macro
  generates is not found and takes the default rules (judged).
- **Layouts** (`rustc_abi/src/layout.rs`, `layout/simple.rs`, `lib.rs`'s Niche, `wrapping_range.rs`; `rustc_ty_utils/src/layout.rs`):
  scalars with their valid ranges, thin and fat pointers (the struct tail's metadata), arrays, slices, `str`, fn items, tuples,
  pattern types (`!null`), `univariant` with repr(Rust)'s field order (the alignment groups, niche sizes, the end-biased second try),
  the newtype and scalar-pair ABIs, `layout_of_enum`'s niche-filling and tagged layouts (the discriminant's range, the tag's width by
  the variants' first field's alignment, the common primitive's pair) and the choice between them, every type of the bodies with
  collect_ty's closure (pointees, elements, struct fields), the helper's `mir/layouts` form.
- **Def paths**: the instances' `DefId(0:N ~ a1[799e]::path)` and `Item(...)`, rustc's DefIndex by the def collector's order (the
  injected extern crate and prelude import, a `#[derive]` item and its derived impls collected after the crate's other items, an
  explicit discriminant's anonymous constant).

### The judge (`bin/rust-front mono-check`, `arbace/rust/front/mono_check.ae`)

Each native fact against the pack's record of the program, entry by entry; `--control`: one program's facts against another's record,
which must differ. Over a1-a7 (main's stage 0, no helper, rustc or C compiler):

| Fact | Equal | Note |
|---|---|---|
| local items' symbols | 47 of 47 | |
| local items' def, DefId, instance, track-caller | 188 of 188 | |
| callees (kind, symbol, def, can-unwind, track-caller, external, intrinsic) | 34 of 34 | |
| caller locations | 33 of 34 | `panic!()`'s call inside its expansion: our span is the expansion's, rustc's the call site |
| function constants' symbols (ReifyShims among them) | 33 of 33 | |
| statics' symbols and DefIds | 1 of 1 | |
| lang items' symbols | 113 of 113 | |
| layouts of the record's tables | 221 of 223 | `core::fmt::rt::ArgumentType` and `Argument` (std's interface lacks the private enum) |
| core's items the collector reaches from our bodies | 1 of 1 | a6's `write_volatile::<i32>`; 3 more (`precondition_check`, `is_aligned_to`, `drop_glue::<D>` through `write_volatile`'s body) are reached through core's bodies: the pack's, RF12's |

The control (a1's facts against a2's record and back): 66 of 160 entries differ (symbols, callees, locations, DefIds; the lang items and core's layouts, the same in both, equal).

### What the run takes now (`bin/rust-front run-built`, `arbace.rust.front.build-run`'s native path)

The module is made from the collector's items: Arbace's bodies in its order with their native headers, callees, locations, function
constants' and statics' symbols; the lang items' symbols, the layout table (native, a refused type's layout the record's when it has
it); our own type forms (the record's DefIds no longer substituted). From the record: core's bodies (a6's four: RF12's part) and,
per fact, what mono refuses, counted on the run's line (`build-run: a6: facts native callees 5/5, fn-symbols 5/5, langsyms 17/17,
layouts 45/47, locs 4/5, statics 1/1, symbols 7/7; core's bodies from the pack 4`). a1-a5 and a7 take **no fact from the record**:
it is read for rustc's status and output alone, the judge's reference. `RF_MONO=pack` runs RFP23 (b)'s path (every fact the record's).
Results: `run-built --records` 7 of 7 programs give rustc's status and output with the native facts; the control (RF10_CONTROL=1) a1
156 for 5.

### The essential step

`bin/rust-front essential` runs `mono-check` over a1-a7 after the runs (its summary line held to `essential-known.txt` with the other
three: a regression in any native fact moves it) and its control (a1 and a2 crossed, `test/rust/front/controls.edn`'s
`_control/mono/crossed`); `test/gate/steps.edn`'s step takes `tools/rsdump/apks.sha256` among its pins (rust-src's `library/` is read)
and the `mono-check` line in its summary. The step's timing and peak are in the journal's entry.

### Scores

`test/crops/independence.edn`'s Rust eval fact names the native facts, its score unchanged at 0.5: the seven runs are not the eval
cell's cases (rsi check's, on the IR packs), and the rule is the case-weighted mean over the REAP cells' cases (CROPS-SCORE.md, the
user's decision of 2026-11-07). `bin/crops-score --judge --strict`: Rust unchanged.

### What remains

- RF12: core's bodies from source (a6's four), then a run takes nothing of the record.
- The three facts mono refuses (RFP37, RFP38 below); core's attributes of macro-made items and core's MIR cost rule (RFP39).
- RF11: rsi's cases from the front end's MIR, which moves the eval stage.

### Proposed (RFP23 (c))

- **RFP37 std's private ADTs.** `core::fmt::rt::ArgumentType` is not in std's interface (the helper's `--std-iface` writes the public
  items and what they name by path, not a private enum a field names), so `Argument`'s and `ArgumentType`'s layouts are the record's
  (2 of 223). Options: (a) **the helper writes every ADT a written field's type names, transitively, the pack `rust-front-std` made
  again (30 s by hand, the oracle tier's verify)**; (b) collect core's ADTs from rust-src with the native front end (RF12's
  collection of core, days); (c) keep the record's two. *Recommended: (a)*, (b) with RF12.
- **RFP38 Caller locations inside macro expansions.** `panic!()`'s call to `core::panicking::panic` carries rustc's span of the macro's
  call site (17:44); our expansion gives the inner call a foreign span (1 of 34). Options: (a) **port `Span::source_callsite`/
  `ExpnData::call_site` in RF4's expansion: each expansion records its call site and a foreign span's caller location walks to it**
  (with RFP34, the same machinery; half a day); (b) take the record's location for an expansion's call. *Recommended: (a)*.
- **RFP39 core's codegen attributes.** As built they come from core's sources (inline, track_caller, rustc_nounwind, rustc_intrinsic),
  a macro-made item (the `panic_const_*` lang items, `precondition_check`) taking the default rules and `cross_crate_inlinable`'s MIR
  cost rule for an item without `#[inline]` not ported (no item of a1-a7 needs it). Options: (a) **as built, judged by mono-check**;
  (b) decode core's rmeta tables (`codegen_fn_attrs`, `cross_crate_inlinable`, `optimized_mir`'s presence: rustc_metadata's
  LazyTables) natively, the platform's facts as Java's crop reads class files; (c) core's attributes from our own collection of core
  at RF12. *Recommended: (a) then (c)*; (b) only if RF12 slips, as it reads rustc's product.
- **RFP40 mono-check's place.** As built it is in the essential step (about 1.5 minutes more). Options: (a) **as built**; (b) in
  `--full` only, the essential step running the native runs alone; (c) folded into run-built (one pipeline run a program, the
  check and the run sharing it: the step back to its earlier time). *Recommended: (c)* then (a): the same judge at no cost.
- **RFP41 The front end's runs in the eval cell.** The native runs score nothing while they are not the cell's cases. Options:
  (a) **as built: eval stays 0.5 until rsi's cases run from the front end's MIR (RF11)**; (b) count `run-built`'s programs as eval
  cases now (6 at 1, 1 at 0.5 beside rsi's thousands: the mean moves by under 0.001). *Recommended: (a)*.

## Decided (2026-11-09, the user: RFP28-RFP41)

All as recommended: **RFP28 (a)** with **RFP37 (a)** (the helper's `--std-iface` writes every ADT a written field's type names,
transitively; `rust-front-std` made again; additions only), **RFP29 (b)** now ((a) with RF11's v0 symbols), **RFP30 (a)**, **RFP31 (a)**,
**RFP32 (a)** now ((b) with RF12), **RFP33 (a)**, **RFP34 (a)** with **RFP38 (a)** (the expansion's spans: `Span::to`, the call site),
**RFP35 (a)**, **RFP36 (c)** then (a), **RFP39 (a)** then (c), **RFP40 (c)** then (a), **RFP41 (a)**. Tonight's (the night of 2026-11-09)
first: RFP30, RFP31, RFP36, RFP40; then the rest in the day, one commit a proposal (branch `rust-props`, from `rust-rf10b` at `c0f226f3b`).

## As built: RFP30 (a), RFP31 (a), RFP36 (c), RFP40 (c) (2026-11-09)

### RFP31 (a): the dumps the judge reads

`bin/rust-front build-oracles` deletes every `.before.mir` after the helper's run (a pass's `.before.mir` is the previous pass's
`.after.mir`; the judge never read one), the 88 programs' dumps about half their 193 MB. The pack `rust-front-built` already held the
`.after.mir` files alone (RFP23 (b)); it now holds only those the judge reads (`bin/lib/repr-rust/front-records.bash`'s `rfr_judged`):
each listed pass at the phase `-Zdump-mir` numbers its first run in (`passes/pass-phase`: 1-1 to SimplifyCfg-promote-consts, 2-1 to
Derefer, 2-2 to StateTransform, 3-1 after), not a pass rustc runs again in a later phase (AddCallGuards and RemoveNoopLandingPads among
the optimizations, `3-2-*`) nor a shim's own pipeline (`D.1-1-*`, drop glue): 1,050 members of 1,108, every kept member's digest the
old pack's (the manifests compared), 22,396 bytes xz (`36526aa2...`). The filter is in the entry's key, so the old pack was STALE until
remade (`bin/rust-front records make built`, 11 s); `records verify built`: 7 entries equal, the crossed pair differing.

### RFP30 (a): the pass judge in the essential step

`bin/rust-front essential` runs `BUILD_PASS=SimplifyCfg-pre-optimizations build-check --records` after the built judge: our bodies
through the 21 passes to runtime MIR, each pass against rustc's `.after.mir` in the pack, its control (a1's add against fib after the
last pass, `_control/pass/crossed` in `test/rust/front/controls.edn`) differing. Its 21 summary lines are held in
`test/rust/front/essential-known.txt` (52 of 52 bodies equal after every pass but RemoveNoopLandingPads' 8 of 8, and the analysis
lint's 0 of 0: no dump is listed for it). About 40 s.

### RFP36 (c), RFP40 (c): the runs as one session, mono-check in it

`bin/rust-front run-built` (the native facts on ci, the default; `RF_RUN_BATCH=0` the earlier process a stage a program) is three
sessions instead of four processes a program and mono-check's two:

- `arbace.rust.front.build-run`'s `batch`: every program's pipeline once in one process: its module (`native-run`, what `native-main`
  did, now a function), RFP40 (c)'s mono-check of the same bodies and facts against the record (`mono-check/judge`; `mono.log` as
  `bin/rust-front mono-check` prints it), codegen (`arbace.rust.back.check/emit-mono`) and rsi's lowering to C (as `bin/rsi lower`:
  `arbace.cpp.lower/lower-file` over the read IR); std's data read once for all. `RF_BATCH_MONO_CONTROL="a1 a2"`: mono-check's crossed
  control judged in the same runs (a1's facts against a2's record and back, `mono-control.log`); `RF_BATCH_CONTROL=a1`: the run's
  control (every Add a Sub) built beside, as `_control/a1`.
- `arbace.rust.front.ci-batch` (new): every lowered program with the driver read, checked, linked with musl's forms and built into
  ci's node program as `bin/ci run --std gnu23 FILE...` does (`arbace.c.run`'s `read-unit` and `nodes`, musl's library prebuilt once),
  under `bin/ci`'s own setup (the sysroot, ci evaluated from the checkout when the host's embedded copy differs, `musl prepare`,
  `cirun-build --ensure`). Nothing of `arbace/c` or `bin/lib/ci` changed: no key of C's moves.
- cirun runs each node program; the status and output against the record's, as before.

`bin/rust-front essential` runs the batch once with both controls; mono-check's separate runs are gone. Results (main's stage 0,
`GOMEMLIMIT=1500MiB`, the six tools stubbed, 0 stub calls): build-check 54 of 54, the passes 52 of 52 (21 lines), capture-check 30 of
31, run-built 7 of 7, mono-check's line unchanged (callees 34 of 34 ... layouts 221 of 223, locs 33 of 34), the five controls
differing (the run control a1 156 for 5; mono's 6 facts); **3 min 22 s** (`--record`) and 3 min 38 s (the check), from 8 min 36 s:
build-check 56 s, the passes 41 s, capture-check 52 s, the runs 85 s (the batch 48 s, ci's session 37 s, the eight cirun runs a
second). `gate.crops check`: 0 problems; `bin/gate --plan`: run rust-front-essential (its key moved). RFP36 (a) and RFP40 (a) (as built
is now the batch, every program, mono-check in the step) hold.

## As built: the rest of RFP28-RFP41 (2026-11-09)

Branch `rust-props`, after the night's four, one commit a proposal.

### RFP29 (b): "equal but for the crate hash"

- The builder prints the two texts that name a local DefId as rustc's Debug does, `DefId(0:INDEX ~ CRATE[HASH]::PATH)`, with the index
  and the crate's hash unknown (`DefId(0:? ~ structs[????]::TABLE)`; `build/static-def-id-text`, `closure-def-id-text`):
  a reference to a byte array whose pointer is a static's (`pretty_print_const_scalar_ptr`'s `<static(DefId)>`, rustc_middle's
  `ty/print/pretty.rs`; other statics' pointers stay `{allocN: &T}`), and a closure's fake reads (`ExprKind::Closure`'s `fake_reads`,
  rustc_mir_build's `builder/expr/as_rvalue.rs`: each captured place's fake read pushed before the operands, its cause
  `ForLet(Some(CLOSURE))` or `ForMatchedPlace(Some(CLOSURE))`; the THIR places converted as the captures are). The closure's fake reads
  were refused before ("a closure's fake reads").
- `build-check`'s class `:equal-crate-hash` ("N more but for the crate hash" on the lines): the texts equal once every local DefId's
  index and crate hash are made one and the allocations renumbered canonically (by first appearance in the body, then in the listed
  allocations they reach, then the rest by header and bytes) and listed in that order, a pointer's drawing to its width: rustc's ids
  are the session's interning order (RFP25 (a)), and its static's allocations are interned before the body's.
- The 88 programs (`bin/rust-front build-check`, 6 min 32 s): 420 of 510 equal (418 before), 27 but for the allocations' ids, 1 but for
  the crate hash (`structs`' `main`), refused 11 (12: `drops`' `main` is built, and differs now only by `Rc`'s user type annotations,
  the defaulted allocator parameter), 60 programs whole. `test/rust/front/essential-known.txt` re-recorded (the lines' new count, 0
  everywhere: a1-a7 name no such DefId).
- (a), the index from the def collector's order and the crate's StableCrateId, is `arbace.rust.front.mono`'s `def-id-text` (RFP23 (c)), for
  the builder when the runs' crate identity (`std-crates.edn`'s cfg-version) is bound there: with RF11, as decided.

### RFP28 (a), RFP37 (a): std's ADTs reached by fields

- The helper's `--std-iface` (`tools/rsdump/front.rs`'s `std_iface`) adds, after the public items and the lang items, every std ADT a
  written ADT's field's type names (the type's walk), transitively, with its variants and fields (not its inherent impls: no path
  names it): 182 new `si/adt` lines (35,823 → 36,005), none changed or removed; among them `core::num::float_parse::FloatErrorKind`,
  `core::iter::adapters::flatten::FlattenCompat`, `core::fmt::rt::ArgumentType`, `alloc::rc::RcInner`, `alloc::raw_vec::RawVec`.
  `test/rust/front/std-iface.sha256` re-pinned (`f8d35318...`); the helper rebuilt by hand (`bin/repr-rust helper`, 12 s).
- Its digest is in the packs' keys (RFP35 (a)), so the four packs were made again (`bin/rust-front records make`, 29 s): `rust-front-std`
  `fe79f73b...` (519,636 bytes xz), the other three byte for byte the same packs (their manifests' keys moved); `records verify std`
  equal, the control differing.
- Results: the 88 programs' `float-f32` and `float-f64` (`Result<f32, ParseFloatError>`'s needs_drop: `FloatErrorKind` has fields now)
  equal at built and after every pass (10 of 10 after ElaborateDrops and after SimplifyCfg-pre-optimizations, 2 bodies more);
  mono-check's layouts **223 of 223** (221: `ArgumentType`, `Argument`), a6's run with every layout native (47 of 47).
  `essential-known.txt` re-recorded (the mono line).

### RFP32 (a): std's function attributes as the helper's data

- `--std-iface`'s `si/fn` entries carry `:const` (`is_const_fn`), `:promotable` (`is_promotable_const_fn`: `#[rustc_promotable]` and a const
  fn) and `:nounwind` (`codegen_fn_attrs`' `NEVER_UNWIND`: `#[rustc_nounwind]` and the intrinsics) for every std function it writes
  (21,816: 11,265 const, 31 promotable, 302 never unwinding); `std-iface.sha256` re-pinned (the pack `rust-front-std` `5116eb09...`), the other
  packs made again with their keys (the same bytes).
- `arbace.rust.front.passes` reads them (`std-fn-attr`): PromoteTemps' `#[rustc_promotable]` (the hand table of nine functions, whose
  `<impl i8>::min_value` spelling never met std's `{impl#N}` paths, is gone: the 31 include the 24 `min_value`/`max_value`), a std
  const fn promoted in a const's or static's initializer (`is_const_fn`; only local const fns were), and `fn_can_unwind`'s
  NEVER_UNWIND for std's functions (`panic_nounwind`, `abort`, ...; the intrinsics by their path only when the interface does not list
  them).
- The 88 programs through every pass (`BUILD_PASS=SimplifyCfg-pre-optimizations bin/rust-front build-check`, 7 min 17 s, with RFP28 and
  RFP29): after SimplifyCfg-pre-optimizations **506 of 616 equal** (499 at RF10's fourth slice), 45 but for the allocations' ids, 2 but
  for their text, 1 but for the crate hash, 19 differ, 11 refused, 32 not built; **63 programs whole** (60). The essential step's summary
  unchanged (a1-a7 call none of them).
- (b), the attributes read from rust-src's library natively, comes with RF12's std from source, as decided.

### RFP33 (a): opaque types' identity

- collect's lowering of `impl Trait` (`lower-ty` `:impl`, and `item-bounds`' self type) gives the opaque type its OpaqueTy definition
  (collect-opaques!' def, found by its node's span) and its args, the definer's generics at their identity: `[:opaque CLAUSES ID ARGS]`,
  rustc's `Alias(Opaque, AliasTy { def_id, args })`. The use's substitution (`collect/subst`) gives the args the use's arguments, as
  rustc's opaque type captures its parent's generics; printing is unchanged (`impl Fn(i32) -> i32`).
- PostAnalysisNormalize (`passes/hidden-type`) reveals an opaque type by its definition: its parent is its defining function, whose
  body's type is the hidden type (`concrete_opaque_types`' entry for a return-position opaque type), the definer's parameters replaced by
  the use's args (`param-args`, `subst-map`). It now reveals the function constants' types too (`reveal-consts`: `twice::<impl Fn(i32)
  -> i32>` printed again from the revealed type, `build/fn-const-text`), as rustc's pass normalizes every type of the body. An opaque
  type without a definition (none here) takes the old structural match.
- The sample `test/rust/front/opaque1.rs` (two functions returning `impl Fn(i32) -> i32`, alike but for their DefIds, and a generic
  definer `twice<F>`), in the pack `rust-front-built` (1,210 members, `c7005a5a...`) and `rust-front-captures` (9, `19e9947c...`), not
  `rust-front-mono` (its run needs the closures behind opaque types collected: RF11's): through every pass 8 of 8 bodies equal (4 of 8
  before: the alike pair and the generic definer left unrevealed). `closures`' `compose` (a generic definer): `differ` → `but for the
  allocations' ids`. `typeck-check --ours closures collect1`: 493 of 497 facts, unchanged. The essential step's lines move to 8
  programs (built 62 of 62, the passes 60 of 60, capture-check 33 of 34), recorded.

### RFP34 (a), RFP38 (a): the expansion's spans

- RFP34 (a): `transcribe.rs`' `mk_delimited` gives a fragment substituted for `$var` (not `tt`) an invisible group whose delimiters'
  span is the definition's `$var`, marked with the expansion's mark. Our transcription now keeps it (`expand/metavar-frag`: the
  transcriber's `$` and name items, `:mv-orig` over the root file's `$var`, `:mv-fspan` for a std macro's, `:mv-mark`), and the
  fragment's placeholder is placed with that mark and place (it had mark 0 and no place). A span the parser joins from a definition's
  token to the fragment is then the definition's, as `Span::to` (`rustc_span/src/lib.rs`' `prepare_to_combine`: the same context) makes
  it: `expand1`'s `(|| $b)()` closure has rustc's 41:21-41:28 (it had none). Not ported: `try_metavars`' metavariable spans of `tt`
  substitutions (`maybe_use_metavar_location`) and the different-context fallback (`a_is_callsite`); no sample meets them.
- RFP38 (a): `span_as_caller_location` (`rustc_middle/src/mir/consts.rs`) reads a call's location at `expansion_cause`
  (`rustc_span/src/hygiene.rs`: the call sites walked to the root's, `include!` stopping it). Lowering keeps an expansion's
  expression's mark (`:expn-mark`), the builder carries it (`mk`), and the call's caller span is the mark's root call site
  (`build/caller-span` over expand's `*sites*`, which `build-check/with-program` now binds and hands over as `build/*expn-sites*`).
  `a6`'s `panic!()`: 17:44, rustc's.
- Results: capture-check **174 of 174** closures over every program (173), the essential step's 34 of 34; mono-check's locations **34 of
  34** (33); `hir-check --ours` 90 of 91 and one but for whitespace, `typeck-check --ours` 11,142 of 11,173: unchanged.
  `essential-known.txt` re-recorded.

### RFP35 (a): the packs' key holds the helper's whole digest (as built)

Kept as built: an entry's key holds `tools/rsdump`'s whole source digest (`rfr_helper_hash`, as `bin/repr-rust`'s `helper_hash`). Today's
two helper changes (RFP28/RFP37's, RFP32's) each made the four packs STALE, and each re-make took about 30 s by hand with rustc
(`bin/rust-front records make`); the three kinds the helper's change did not touch came out byte for byte the same packs (their
manifests' keys moved), which `records make` shows by the unchanged pack digests. A digest of the modes' sources alone (b) would trust a
hand-kept list of which files a mode reads, as the proposal says.

### RFP41 (a): the front end's runs out of the eval cell (as built)

Kept as built: `test/crops/independence.edn`'s Rust eval fact stays 0.5 until rsi's cases run from the front end's MIR (RF11); the
seven `run-built` programs are the essential step's, not the eval cell's cases. `bin/crops-score --judge --strict` after today's
commits: Rust 75.0 / 75.0 / 95.8 / 80.0, unchanged, exit 0.

### RFP39 (a) then (c): core's codegen attributes (as built, then RF12)

Kept as built: core's attributes for the runs' callees come from core's own sources (rust-src's `library/core`, read by repr/rust's
native parser; `inline`, `track_caller`, `rustc_nounwind`, `rustc_intrinsic`), a macro-made item taking the default rules, the
`cross_crate_inlinable` MIR cost rule not ported, all judged by mono-check (callees 34 of 34, symbols 47 of 47 after today's commits).
Then (c): the attributes from our own collection of core at RF12, when core's bodies come from source too; (b), decoding core's rmeta
tables, only if RF12 slips. Note: since RFP32 (a), MIR's passes read std's `#[rustc_nounwind]` and `#[rustc_promotable]` from the
helper's interface (pinned data), the runs' callees from core's sources: two routes to one fact until RF12 collects core.

## As built: RF11 (first slice), rsi's cases from the front end's own MIR (2026-11-09)

Branch `rust-rf11` (from next `e6214cd13`), the user's order (RES54 (a): RF11 after RF10's slices; RFP41 (a): the eval cell's
independence moves only when rsi's own cases run from the front end's MIR). The slice: which of rsi's cases the front end builds end
to end now, how such a case is judged, the switch per case, and the gaps that block whole programs.

### Design

- **The cases.** rsi's eval cell is `bin/rsi check` over the samples (`test/rust/eval/`, 6 programs), the synthetic corpus
  (`test/rust/synthetic/`, 67) and the UI corpus's run programs (3,718, the night's). A case runs today from rustc's `-O0` IR of the
  program's codegen unit (`test/rust/ir/CORPUS.tar.xz`'s `x.ll`), rustc's allocator shim (`shim.ll`) and std's lowered library
  (`bin/rsi runtime-ci`, std's IR made by Cargo and rustc: `bin/rsi std-build`), lowered to C, read and run by ci. The slice replaces
  the first of the three, the program's codegen unit, with Arbace's: the program's own bodies built by the front end and run through
  the passes to runtime MIR (no rustc MIR), arbace.rust.front.mono's native facts, codegen, the same lowering and run. The shim and
  std stay rustc's (RF12's part), and so do the bodies of std's generic and inline instances the program instantiates: rsi's `x.ll`
  is a share-generics build, ours a `-Zshare-generics=no` one, so the module holds every instance it uses, their bodies (core's,
  alloc's, std's: RF12) from a record.
- **The record, `rust-front-rsi`** (new kind of `bin/lib/repr-rust/front-records.bash`, `test/oracle-records/kinds.edn`'s
  `front-rsi`): per program the helper's `--mono-mir` dump at rsi's plan's flags (bin/lib/rsi/plan.bash's `plan_rustc`: the edition,
  rustc's debug defaults, the path relative to the checkout, `--remap-path-prefix`) with `-Zshare-generics=no`, against **rsi's std
  build's sysroot** (`.tmp/rsi/std/sysroot`, whose crates' StableCrateIds are not the host sysroot's: `bin/rust-front std-crates
  --rsi` writes `test/rust/front/std-crates-rsi.edn` from its rlibs, `read-rmeta-root` now reading an rlib's `lib.rmeta` member).
  The key holds `std-crates-rsi.edn` (a rebuilt std moves every entry). 73 members, 183 MB raw, 2.5 MB xz (`da1b73e6...`), made by
  hand in 2 min 47 s (`bin/rust-front records make rsi`, the oracle tier). What a run takes from it: std's instances' bodies (RF12)
  and, per fact, what mono refuses (counted on each program's `build-run:` line, as RFP23 (c)'s runs).
- **The judge** is rsi's, unchanged: the run's status and outputs' digests against rustc's recorded run
  (`test/rust/ref-rustc-1.99.1.edn`, a case's own verdict where it has one, EVAL.md §13), every non-pass in a class, against the
  committed record (a recorded pass that fails is a regression). Nothing of the front end's oracles is read at check time.
- **The switch, per case.** `bin/rust-front rsi-build [NAME...]` builds the cases (one process each `RF11_BATCH` programs, default
  6, `GOMEMLIMIT` 1800MiB, `GOMAXPROCS` 4; rustc, rustdoc, cargo, gcc, cc and clang stubs exiting 99 first on PATH, their log empty)
  and writes `front.tsv`: `own` when every one of the program's own bodies is ours and the module goes through codegen and rsi's
  lowering, else `pack` and why. `bin/rsi check --front own` (`make rsi-check-front-own`) runs it for the samples and the synthetic
  corpus and, in `ir1`, puts an own case's `NAME.ours.ll` in place of the pack's `x.ll` (`WORK/CORPUS/front.tsv`, a line
  `rsi check: front own: CORPUS: N of M` per corpus); the UI corpus stays on its pack (no records yet, RFP45). `--record` is refused
  with `--front own` until the user decides the default (RFP42).
- **The day's run** (no lowered std on this host: `.tmp/rsi/runtime` has never been built, about four hours and 6 GiB by hand,
  RES60 (a)): `bin/rust-front rsi-native` (by hand, the oracle tier's tools) links each own program's codegen unit through
  bin/lib/rsi/link's `RSI_FRONT_IR` (rsi's lowering with `--native-std`, gcc) with rsi's std build's rlibs by rustc as the linker
  driver, runs it as `bin/rsi check` runs a program and judges it against the same recorded run. It is evidence for the module,
  not a check: the ci leg is the night's `bin/rsi check --front own`.

### What was built

- `arbace.rust.front.build-run`'s batch (`--std`, `NAME=REL` specs): the bodies after the ported passes to runtime MIR
  (`BUILD_PASS=SimplifyCfg-pre-optimizations`: ElaborateDrops' drop flags, ElaborateBoxDerefs, PromoteTemps, ... in place of the first
  slice's built MIR with CleanupPostBorrowck's changes), the program's crate name rustc's (`exit-code` → `exit_code`), its file name as
  rustc was given it (`build-check/*file-name*`: closures' type texts and caller locations), std's functions declared rather than
  stubbed (`*std-linked*`), core's integer constants (`<impl iN>::MIN`, `MAX`, `BITS`) evaluated from their definitions
  (`std-const-value`, int_macros.rs and uint_macros.rs), promoted constants of scalars and arrays of them evaluated into their
  allocations (`eval-promoted`, RF10's slice 5's first part), Assert terminators' caller locations from their expression's span
  (`build/*assert-x*`, kept by the terminator's metadata through the builder and looked up by condition and message after the passes),
  a `#[track_caller]` method call's location at the method's name and an overloaded index's at its brackets (rustc's `fn_span`).
- `arbace.rust.front.mono`: `layout_of_union` (rustc_abi's: the common non-ZST field's ABI with its valid range discarded,
  `Scalar::Union`, `FieldsShape::Union`, no niche), a std ADT std's interface lacks (hashbrown's) no longer stopping the drop glue's walk,
  and, for rsi's runs (`*unknown-refused*`), an external item whose attributes core's sources do not show (the integer types' macro-made
  methods) a refused callee fact, the record's: by the default rules its `#[inline]` symbol named no instantiating crate.
- `arbace.rust.back.codegen`: Subslice places (place.rs' `project_subslice`, the subslice's length in the reference's metadata), the
  PointerExposeProvenance and PointerWithExposedProvenance casts, ClosureFnPointer casts by their shim's symbol, the float math intrinsics
  by the argument's type (`fabs::<f64>`) and `minimum_number_nsz`/`maximum_number_nsz`, `pow`, `copysign`; an `i128` constant's folding (a
  BigInteger, not a long); a widened constant extended by its source's signedness (`255_u8 as u16` had folded to `-1`); a scalar return never assigned (`MaybeUninit::uninit`) as `undef`; the front end's own ADT forms named by
  their path in LLVM's struct names.
- The pack `rust-front-built`'s dumps hold the checkout's path as `@ROOT@`, put back at the stage (as `rust-front-mono`'s): its members
  named the worktree that made them (`rust-rf10b`), so `build-check --records` over `opaque1` (closures' spans) and `a7` (derived impls'
  `<impl at FILE>` names) failed in any other checkout, the essential step's 62 bodies at built 51. Made again (`ab3d8d34...`); the essential
  step's lines back to 62 of 62 and 60 of 60.

### Results

- `bin/rust-front rsi-build` over the synthetic corpus (67 programs, main's stage 0, the six tools stubbed, 0 stub calls; 25 min 48 s
  at `RF11_BATCH=6`, `GOMAXPROCS=4`, peak 2.37 GB): **47 of 67 programs own** (every body of the program's own from Arbace's MIR, the
  module through codegen and rsi's lowering), 20 on the pack. The samples (6 larger programs: `intrinsics`, `panics`, `std`, `threads`,
  `unwind`, `vectors`) were measured once in the first whole run (one process, 37 min, 2.0 GB): 0 of 6 (an unevaluated named constant,
  virtual calls twice, hashbrown's layout, since fixed, a transmute, the vector ABI); their larger records put a process above 2 GiB, so
  the day's lists left them to the night (RFP46).
- The own programs on the native leg (`bin/rust-front rsi-native`, by hand: rustc as the linker driver with rsi's std build's rlibs, gcc
  for the lowered C, clang for the shim's IR): **43 of 47 give rustc's recorded run** (status, standard output's and standard
  error's digests, `test/rust/ref-rustc-1.99.1.edn`): `abort` (134), `trap` (132), `exit-code` (37), the twelve `int-ops-*` (every
  integer width's arithmetic, checked, wrapping, saturating and overflowing operations, bit counts, rotations, parsing, `isqrt`,
  formatting), the fourteen `overflow-*` but two (the panic messages with their caller locations), `i128`, the `panic-*` programs
  (unwrap, expect, index, slice range, str boundary, RefCell, char conversion, todo, unreachable). The day's fixes that the runs found:
  Assert terminators' locations (`?:0:0` before), a method call's location at its name (`o.unwrap()`: rustc's column 37, ours 35
  before), an overloaded index's at its brackets, the defaulted allocator in v0 paths (`Box<T, Global>` mangled `Box<T>`:
  `Box::new_uninit` unresolved at the link before), widened unsigned constants (`255_u8 as u16` folded to `-1`: `isqrt` gave 15 for
  every width), the macro-made integer methods' symbols (the record's).
- What the four left show: `overflow-abs-i64`, `overflow-pow-i32` and `panic-alloc` panic inside std's bodies, whose locations name
  std's sources as rsi's std build compiled them (`library/core/src/num/mod.rs` in the helper's record, `core/src/num/mod.rs` in the IR
  pack's `x.ll`) where the judge's recorded run (the host's rustc with Alpine's std) names `/rustc/58cf8f9c.../library/...`: the pack
  leg differs from the recorded run the same way, a property of rsi's std build, not of the front end (RFP47); `panic-vec-remove`
  faults (status 139) before its panic message: to triage (std's `Vec::remove`'s `assert_failed` path, a body of the record's).
- The 20 not own, by cause (the module's first refusal): virtual calls 5 (`collections`, `errors`, `generics`, `rc-refcell`, `traits`:
  the record's callee names no vtable slot, RFP13/RFP44), unsizing to `dyn` 3 (`closures`, `drops`, `patterns`: our bodies' casts
  carry no vtable, RFP44), the `[x; N]` repeat loop 2 (`algorithms`, `structs`), `powf32`/`powf64` 2 (fixed after the run: `pow`,
  `copysign`, `minnum`, `maxnum`), an unevaluated named constant (`casts`: CTFE, RF10's slice 5), a transmute between layouts
  (`iterators`), a constant allocation of another kind (`main-err`), `tlsref` (`panic-hashmap-index`: no symbol for std's
  thread-local), `typed_swap_nonoverlapping` (`strings`: its fallback instance), and three programs whose `main` is not ours: typeck's
  `Option<_>` (`fmt`), a path (`panic-assert-eq`), a promoted constant of a `&str` (`panic-explicit`: `eval-promoted` takes scalars
  and arrays of them).
- The earlier stages unchanged, the pack fixed: `bin/rust-front essential` passes, its summary equal to
  `test/rust/front/essential-known.txt` (build-check 62 of 62, the passes 60 of 60, capture-check 34 of 34, run-built 7 of 7, mono-check's
  line, the five controls differing; the stubs' log empty), in this checkout too (51 of 62 before the `@ROOT@` fix). `bin/rust-front
  records make rsi` 73 entries; `records make built` remade (`ab3d8d34...`). `bin/gate --plan`: `run rust-front-essential` (its key
  moved). `bin/crops-score --judge --strict`: Rust 75.0 / 75.0 / 95.8 / 80.0, unchanged, exit 0.

### What eval and AOT can move under the decided scoring

Nothing in the number, by the decided rule: an own case's run still consumes rustc's records (std's instances' bodies and the refused
facts from `rust-front-rsi`, the shim, std's lowered IR), so it scores 0.5 as any case does (RES54 (a), RFP43 (a) proposed to confirm
it); the eval stage stays 0.5, `test/crops/independence.edn`'s Rust eval fact names the switch and counts the own cases
(`:own-cases`: 47 of the 73 samples and synthetic programs; the UI corpus's 3,718 none). What moved is what RF12 needs: the
program's own share of the module is Arbace's for those 47, so when RF12 builds std's bodies from source the record's remaining part
is the shim and std's lowered library.

### What remains

- The night: `bin/rsi runtime-ci` (std's lowered library, once, by hand: RES60 (a)), then `bin/rsi check --front own --corpus
  samples,synthetic`, the ci leg's verdicts of the 47 against the record, every non-pass classed (RFP42).
- RFP44's vtables (8 programs), the repeat loop, CTFE's named constants and `&str` promoted constants (RF10's slice 5), the samples'
  memory (RFP46), the UI corpus's records (RFP45), `panic-vec-remove`'s fault, std's source paths (RFP47).
- Then RF12: std's bodies from source, which the rule rewards.

### Proposed (RF11's first slice)

- **RFP42 The switch's default.** `bin/rsi check --front own` judges the own cases against the committed record of the packs' runs;
  `--record` is refused with it. Options: (a) **the night runs `bin/rsi check --front own --corpus samples,synthetic` (after `bin/rsi
  runtime-ci` exists, RES60 (a)); when every own case passes or is classed, `--front own` becomes the default of `rsi-check` and
  `rsi-essential` for those corpora (the pack the fallback per case, as built), and the record is re-recorded under it**; (b) keep the
  packs the default and add a `--full` step `rsi-check-front` beside `rsi-check`; (c) own the default now, before a ci run.
  *Recommended: (a)*: the switch is per case already, and (b) doubles the night's cost of the same programs.
- **RFP43 The own cases' score.** An own case still consumes rustc's records: std's instances' bodies and the facts mono refuses
  (`rust-front-rsi`), the allocator shim (the IR pack's `shim.ll`) and std's lowered library (std's rustc-made IR). Options: (a) **0.5,
  as RES54 (a) scores any case that consumes std's records: eval stays 0.5 until RF12, the own count recorded beside
  (`test/crops/independence.edn`'s `:own-cases`)**; (b) a case's score the share of its mono items that are Arbace's (the program's own
  bodies a few of hundreds: under 0.51 either way); (c) 1 for an own case (against RES54 (a)). *Recommended: (a)*.
- **RFP44 Virtual calls, vtables and thread locals.** The biggest refusal class after RF11's fixes: `callee of kind virtual` (the
  helper's dump names the trait method, not its vtable slot: RFP13), `PointerCoercion(Unsize)` to `dyn` in our bodies (the vtable
  allocation, which mono does not make), `tlsref` (no symbol for a thread-local static). Options: (a) **port `vtable_entries`
  (rustc_trait_selection's `vtable.rs`: the segments of the trait and its supertraits, the vtable-safe methods, with
  `rustc_codegen_ssa/src/meth.rs`'s `get_vtable`) into mono, the slot and the vtable's allocation native, and the virtual call in
  codegen (`VirtualIndex::get_fn`), 1-2 days; the thread local's symbol by v0 of the static**; (b) the helper dumps the slot and the
  vtable (RFP13 (a), a rebuild and the packs again); (c) leave them classed. *Recommended: (a)*: the slot is a function of the
  trait's items, which collect and std's interface hold, and it removes a fact the record would otherwise carry.
- **RFP45 The UI corpus.** The records cover the samples and the synthetic corpus (73 programs, 2.5 MB xz). Options: (a) **the UI
  corpus's every 40th (about 93 run programs, the night's sample; about 3 MB xz more) recorded into `rust-front-rsi`, rsi-build run
  over it at night**; (b) the whole UI run set (3,718 programs, about 120 MB xz: in the main checkout's store only, as the UI IR pack);
  (c) not until RF12. *Recommended: (a)*, (b) with RF12.
- **RFP46 rsi-build's cost.** About 30 minutes for the 67 synthetic programs at four threads, each process near 2 GB (2.39 GB peak:
  std's interface and the pipeline's caches; `GOMEMLIMIT` 1800MiB), the samples' larger programs above it. Options: (a) **the night's
  step at `RF11_BATCH=6`; by day only lists; the interface read once into a compact form (RF14's lazy std data) next**; (b) one
  program a process (more starts, a lower peak by about 0.3 GB); (c) as built. *Recommended: (a)*.
- **RFP47 std's source paths in panics.** rsi's std build (`bin/rsi std-build`) names its sources `library/...` (the helper's view) and
  `core/src/...` (its codegen's), Alpine's std, which the judge's recorded runs used, `/rustc/58cf8f9c35b355199727627d07ae34a4e2846d67/
  library/...`: a panic inside std's bodies prints another location on both legs (3 of the 47 here). Options: (a) **std-build with
  Alpine's remapping (`--remap-path-prefix` of the library to `/rustc/<commit>/library`, as the distribution's build passes it), std
  rebuilt and lowered again once (the night's: about four hours with runtime-ci) and the packs made again**; (b) a class `std-paths` in
  `test/rust/divergences.edn` for those programs; (c) normalize std's paths in the judge. *Recommended: (a)*, (b) until it is done.
## As built: RF12 (first slice), core's modules through the front end (2026-11-09)

Branch `rust-rf12` (from `mac/next` `3ef7b3f78`), beside RF11 (`rust-rf11`, rsi's cases from the front end's MIR), whose files it
leaves to it but for small edits in `build.ae` (named below). The brief: run the front end over core's own sources module by
module, judge the bodies against rustc's own built MIR of core, group the failures, fix the groups that block whole modules.

### The oracle (`bin/rust-front core-oracle`, the pack `rust-core-built`)

rustc's built MIR of core, made by hand with the helper as rustc (its driver leaves `trimmed_def_paths` off, so the dumps print
full crate-relative paths as the programs' oracles do; rustc's own driver turns trimming on): `--crate-type lib --crate-name core
--edition 2024 -C opt-level=0 -C debuginfo=0 -C overflow-checks=off --emit=metadata -Zdump-mir=built`, the paths remapped to
`library/`, over a copy of rust-src's `library/core` (stdarch and portable-simd linked) in `.tmp/rust-front/core/lib`. The copy patches
`task/wake.rs`' two `from_fn_ptr` statics, whose closures name the static itself: rustc's MIR dump evaluates the static while
printing the closure's body and stops on a cycle (E0391; the build without `-Zdump-mir` passes); `|this| RawWaker::new(this,
&VTABLE)` becomes `loop {}` in the closure, those four bodies out of the judged set. 31,334 bodies (68 MB, 35 s, 1.2 GB), indexed by
their dump file's name (`hint-{impl#0}-to_llvm`: the def path with its disambiguators, unique where 860 printed names are not).
`bin/rust-front core-records make` packs the first slice's modules' bodies (every file whose key starts with a listed module's
path, one member a module: the dumps one after another, 7,547 bodies, 304 KB xz, `418ebb7b...`) and the index as `rust-core-built`
(test/rust/records/, the oracle-records format, kind `rust core-built` in `test/oracle-records/kinds.edn`; an entry's key: the copy's
sources, the helper's digest and the flags); `core-check --records` reads it, no helper and no rustc.

### The driver (`arbace.rust.front.core-check`, `bin/rust-front core-check`)

- core's module tree after `rustc_expand/src/module.rs` (`#[path]`, mod.rs directories, inline modules), cfg evaluated for core's
  build (rustc 1.99.1's `--print cfg` at these flags: target_has_atomic's values, target_feature sse2, ub_checks, debug_assertions,
  target_vendor alpine...; the expansion's cfg now takes a name's set of values, `*cfg-names*`): 378 module files, 105 of stdarch and
  portable-simd.
- core's index from its sources, cached (`.tmp/rust-front/core/index.edn`): every file read by the native reader once; its
  `macro_rules!` and macros 2.0 (231), the nearest file's definition taken for a name (textual scope approximated crate-wide);
  each module's definitions (628 modules: functions, ADTs, variants and constructors, traits and their items, constants, statics,
  modules), added to std's names where they lack them (core's private items: `ub_checks::check_language_ub`).
- One process over the listed files, one module file at a time as the crate's root (GOMEMLIMIT=1800MiB): expansion, resolution,
  lowering, collection, type checking and MIR building, each body judged and dropped as it is built; the rest of core from std's
  interface and names (the pack `rust-front-std`), plus core's own interface so far (below).

### What was built (the groups that blocked whole modules, biggest first)

- **Spans of macro output (the virtual file).** The front end keys resolution, definitions and locals by span; core's macros expand
  one definition's tokens hundreds of times (`uint_impl!`, `forward_ref_binop!`), so their nodes shared spans and resolved to one
  another. In core mode (`expand/*virtual*`) every expansion's tokens are laid after the root file's text as a virtual file, each
  expansion on lines of its own (root tokens inside an expansion too: rustc's NodeIds make `$arg` used twice two nodes), and every
  span is a place there; `expand/virtual-origin` maps a place back to where its tokens came from (a macro's definition in another
  file, a fragment's metavariable, the root file's own tokens, or a builtin's call site), which MIR's printed spans use
  (`build/*span-origin*`: impls' `<impl at FILE:L:C>`, user type annotations).
- **core's macros.** Macros 2.0 (`pub(crate) macro const_eval_select { .. }`, one-arm `macro m($x) { .. }`) with def-site hygiene for
  their identifiers (`resolve/*def-sites*`: the definition's module); metavariable expressions (`${ignore($x)}`, `${index()}`,
  `${len()}`, `${count($x)}`: mbe/metavar_expr.rs, transcribe.rs); the `meta` fragment; a fragment passed on whole to a matcher of its
  kind (`$compiletime:block` again); an `item` fragment after the transcriber's attributes; `cfg_select!`; `concat!` of a literal
  fragment.
- **Resolution in core.** `crate::`, `$crate::`, `::core::` and a root `super::` name core (std's names), a def under the root file's
  module is the file's own definition (`localize`), an out-of-line `mod` is std's module, a name the file does not define is std's
  (a macro's item not expanded); `Self` in a trait's generics (`Add<Rhs = Self>`, late.rs' self rib); `Self::Variant` in an enum's
  impl's patterns.
- **Identity.** The interface's paths of the file's own definitions are the local definitions (`collect/loc`, `*local-paths*`); impls'
  disambiguators in rustc's DefIndex order (a fragment's own items first, its macro calls' and derives' items after, one invocation
  at a time: `expand/*def-order*`, `:rf-order`); bodies' names relative to the crate (`hint::<impl at ..>::fmt`).
- **Bodies.** `#[rustc_inherit_overflow_checks]` builds with overflow checks (Builder::new); the signature normalized
  (`-> <usize as Add>::Output` is `usize`, typeck's liberated signature); a qualified path to a trait's function as a function item
  with its user type annotation; an impl's clauses on a parameter proven by the parameter environment for Copy
  (`impl<T: Copy> Copy for MaybeUninit<T>` is not `MaybeUninit<T>`'s for a T without Copy: `move`, not `copy`).
- **core's interface from its sources** (`core-check/native-iface`): after each module's collection, its functions and ADTs that
  std's interface lacks are written in the helper's `--std-iface` forms from our own generics, signatures, clauses and fields, read
  by the modules after it and by the next run (`.tmp/rust-front/core/native-iface.edn`): the first of std's interface replaced by
  core's own.
- **Speed.** The crate's impl headers indexed once and prefiltered by trait and self type's head (selection instantiated every impl
  of the crate for every goal): arith's 500 bodies in about a minute.

### Results

`bin/rust-front core-check` over the 35 module files of `test/rust/front/core-modules.txt` (main's stage 0, one process, GOMAXPROCS=4,
GOMEMLIMIT=2500MiB: 17 min, peak 2.50 GB RSS; the second pass of a run, reading the first's native interface: 33 functions and 7 ADTs):

| | first run (the slice's start) | as built |
|---|---|---|
| modules through every stage | 20 of 35 (15 stopped at expansion) | 33 of 35 (`marker`, `ptr::non_null`: `pattern_type!`) |
| bodies built (not refused) | 588 of 652 | 5,393 of 5,601 |
| equal to rustc's built MIR | 247 | 4,578 |
| more but for the allocations' ids / text | 16 / 0 | 37 / 4 |
| differ | 207 | 716 |
| no oracle body by its key | 118 | 58 |

Whole modules equal: `ops::arith` 500 of 500, `ops::bit` 1,989 of 1,989, `default` 20 of 20, `bool`, `ops::drop`, `ub_checks` (11 of 11
with one but for the ids); `num::wrapping` 1,158 of 1,201, `clone` 27 of 29, `cmp` 276 of 374, `cell` 75 of 109, `time` 71 of 96;
after the run, the where clauses' span and qualified paths' fixes: `ops::function` 5 of 5, `convert` 14 of 21 (`--records`, from
the pack). The biggest remaining groups (`.tmp/rf12`'s grouping by the first differing line): impl spans through nested macro calls
(tuple 168, `iter::range` 88: the metavariable span of an `ident` substitution, `maybe_use_metavar_location`, not ported), the
interface's missing private fields (55 refusals, "a std field") and paths (42, "a path"), qualified paths to associated constants
(41), user type annotations' texts of std paths (about 100 over five forms), method calls without a callee (28), `!` locals of empty
matches (9), patterns' types left inferred (`let _N: _`, 23).

`bin/rust-front essential` after the slice: run-built 7 of 7, mono-check unchanged (`core-closure 0 of 3`), capture-check 34 of 34,
the five controls differing; build-check's lines differ from `essential-known.txt` in this checkout (51 of 62) as they do before the
slice: RF12-P4. Rust's scores: no REAP cell reads the core check, so `bin/crops-score` is unchanged; the eval and AOT stages stay at 0.5
(every run still reads the mono record's core bodies and std's interface), RES54's ceiling holds.

### What remains

- **core's interface natively, whole.** The rest of core still comes from std's interface (the helper's `--std-iface`, rustc's data):
  its private items' fields, signatures and impls are what most refusals and differences of the measured modules lack
  (`slice::index`'s `IndexRange` fields, `ptr`'s `ub_checks::maybe_is_aligned`). The native interface covers the processed modules'
  functions and ADTs; impls, traits, constants and the vendored modules are not written yet, and a module reads only those processed
  before it (two passes a run, or the whole crate's collection first: RF12-P2).
- **Groups seen, not built:** pattern types (`pattern_type!`, which `marker` and `ptr::non_null` need: TyKind::Pat through lowering,
  collection, typeck and the printer); const generic arguments evaluated (`{ Locality::L3.to_llvm() }`: CTFE, slice 5); the reborrow
  of a coercion to a reference in a tail (`fn deref(&self) -> &T { *self }`: `&(*(*_1))`, ours `copy _1`); an empty match's `!`
  local; a trait object's default lifetime in printed paths (`<(dyn any::Any + 'static)>::is`); impl spans across nested macro calls
  (tuple's 168: `maybe_tuple_doc!`'s `$item` inside `tuple_impls!`, whose tokens lose their origin when the inner call's token
  stream is re-read; the bodies themselves match by key); `#[derive]` on core's own items in the 860 ambiguous names (keyed now).
- **The modules not yet run** (243 files of 273 non-vendored, and stdarch's 105): a night command below; the expansion of the
  largest (`num/uint_macros.rs` through `num/mod.rs`) is the memory risk, measured by `CORE_TIME`.
- **a6's core bodies and mono-check's core-closure** from the native core: not built here (RF11 is changing `build_run.ae`, `mono.ae`
  and the packs on its branch): `ptr::write_volatile` and its `precondition_check` build natively; their differences from rustc's
  built MIR are the user type annotation of `align_of::<T>()` and `maybe_is_aligned`'s signature (the native interface), and the run
  needs them through the 21 passes and the optimized MIR the pack holds (RF12-P3).
- alloc and std (RF12's next slices), after core's whole interface.

### Proposed (RF12's first slice)

- **RF12-P1 The core check in the gate.** Options: (a) **an essential step `rust-core-essential`: `bin/rust-front core-check --records`
  over a short list (`ub_checks`, `bool`, `default`, `ops/drop`, `clone`, `ops/arith`: about 6 minutes, 2.5 GB), its summary held to a
  known file with a crossed control (a body's text against another's)**; (b) only the oracle tier and by hand until the slice's
  equal share is higher. *Recommended: (a)*: it holds what is built (arith's 500, bit's 1,989) against regressions in the shared files.
- **RF12-P2 core's interface.** Options: (a) **as built, module by module with two passes (each module reads the native interface of
  those before it and of the last run)**; (b) a collection pass over all of core first (every module expanded, resolved, lowered and
  collected, no bodies; the interface written whole), then the bodies: one more pass of about the check's cost without the bodies,
  the order-dependence gone; (c) keep std's interface for core and build only bodies natively. *Recommended: (b)* for the next slice,
  (a) meanwhile: (c) keeps rustc's data in every run, which RF12 is to remove.
- **RF12-P3 a6's core bodies from the native core.** Options: (a) **after RF11 merges: `build-run` takes a core body from
  `core-check`'s native build when its built MIR equals rustc's (the four of a6), through the 21 passes, the pack's optimized body the
  fallback; mono-check's core-closure from the native callees**; (b) wait for core's whole interface (RF12-P2 (b)). *Recommended:
  (a)*, one slice after RF11's merge, on its files.
- **RF12-P4 The rust-front-built pack's paths.** The pin on next is the pack made in `rust-rf10b`, whose texts name that worktree's
  absolute paths, so `bin/rust-front essential`'s build-check lines fail in any other checkout (62 → 51 here, before and after this
  slice); the store also holds a pack with `@ROOT@` in their place. Options: (a) **re-pin the `@ROOT@` pack (or remake it with
  `@ROOT@`, as the mono pack does) and have build-check put the checkout's root back**; (b) leave it to RF11's packs.
  *Recommended: (a)*, by whoever merges RF11 (its pack tooling).
- **RF12-P5 The scope of the first slice's record.** Options: (a) **the 35 modules of `test/rust/front/core-modules.txt` recorded in the
  pack `rust-core-built`, the whole of core's 31,334 bodies judged by a night run against the unpacked oracle by hand**; (b) pack all
  31,334 (68 MB raw, a few MB xz). *Recommended: (a)* until more of core builds.

## Decided (2026-11-09, the user: RF12-P1 to P5)

RF12-P1 (a), RF12-P2 (b) for this slice, RF12-P3 (a) (RF11 merged into next), RF12-P5 (a); RF12-P4 moot: RF11's pack `rust-front-built`
names no checkout's path (`@ROOT@`).

## As built: RF12 (first slice, second part): RF12-P1, P2, the merge of RF11, more groups (2026-11-09)

Branch `rust-rf12`, `mac/next` `6c3c87c02` (RF11) merged in (conflicts only in the records: both sides kept).

### The merge and the essential step

`bin/rust-front essential` after the merge, with this slice's changes: the summary equals `test/rust/front/essential-known.txt`
(build-check 62 of 62, the passes, capture-check 34 of 34, run-built 7 of 7, mono-check, the five controls differing): RF12-P4's
diagnosis holds (RF11's pack made the 11 differences of the first part, every one a closure or impl named by `rust-rf10b`'s path).
After every change below, `bin/rust-front essential` again: the summary equals the known file (3 min 50 s), and
`bin/rust-front core-essential` equals its own.

### RF12-P1 (a): the essential step `rust-core-essential`

`bin/rust-front core-essential` (`--record` rewrites the known file, by hand): `core-check --records` over
`test/rust/front/core-essential.txt` (`ub_checks`, `bool`, `default`, `ops/drop`, `clone`, `ops/arith`) with caches of its own made
afresh (core's index from the sources, the interface pass), rustc, rustdoc, cargo, gcc, cc and clang stubbed first on PATH (their log
empty); its lines held to `test/rust/front/core-essential-known.txt` (564 of 567 bodies equal, one but for the allocations' ids, two
refused), the crossed control (the first equal body's text against another body's rustc text, `_control/core/crossed` in
`test/rust/front/controls.edn`) differing. 3 min 29 s, 1.97 GB peak. The step in `test/gate/steps.edn` beside `rust-front-essential`
(chain `rust-essential`, `:mem-gib 3`, its pins the packs `rust-front-std` and `rust-core-built` and `tools/rsdump/apks.sha256`; its
control in `:controls`): `gate.crops check` 0 problems, `bin/gate --plan` `run rust-core-essential ... never passed`, no table error.

### RF12-P2 (b): core's interface in one pass before the bodies

`core-check` first runs every listed module through expansion, resolution, lowering and collection without building a body and
writes their functions, ADTs and now traits (with their associated functions' signatures: `clone::uninit::CopySpec`) that std's
interface lacks; then the bodies, every module reading the whole pass's interface (no order between modules). `CORE_IFACE_FILES=LIST`
takes the pass over other files (all of core at night), `CORE_IFACE=cache` the last run's `native-iface.edn` instead. The pass over
the 35 modules: 11 min, 33 functions and 7 ADTs (before the traits); the run 27 min, 2.40 GB peak.

### More groups

- **Integer constant expressions in array lengths** evaluated (`[T; $n - 1]` is `[T; 31]`: collect's lower-const-arg).
- **Impl spans through nested macro calls**: a macro call inside a macro's output reads its tokens back from the output's token stream;
  each token now keeps the place it came from (`with-vorigin`: the definition's place or the root token), and a substituted
  identifier carries its metavariable's span (transcribe.rs' `maybe_use_metavar_location`, its place in the definition), which an
  impl's def_span ends at (`Span::to` of different contexts): tuple 0 → 168 of 180, `iter::range` 89 → 202 of 257 (the metavariable span at a span's start too).
- **Pattern types**: `pattern_type!(T is PAT)` (rustc_builtin_macros/src/pattern_type.rs) as TyKind::Pat through lowering and
  collection, printed `(T) is PAT`; `include_str!`: `marker` and `ptr::non_null` through every stage (10 and 58 bodies equal).
- **`*mut T` to `*const T`** (coerce_raw_ptr's MutToConstPointer), **a turbofish's bound variables in user type annotations**
  (`TypeOf(mem::align_of<T>)`), **the where clause in an impl's span**, **qualified paths' written trait arguments and `<T>::f`**,
  **f16 and f128 constants**, **closures named by their owner**.

### Results

`bin/rust-front core-check` over the 35 module files (one process, GOMAXPROCS=4, GOMEMLIMIT=2400MiB: 33 min with the interface pass,
2.41 GB peak; the interface pass over the 35 files 13 min: 45 functions, 7 ADTs, 10 traits):

| | first part's end | the interface pass (RF12-P2) | as built |
|---|---|---|---|
| modules through every stage | 33 of 35 | 33 of 35 | **35 of 35** |
| bodies built (not refused) | 5,393 of 5,601 | 5,393 of 5,601 | 5,388 of 5,601 |
| equal to rustc's built MIR | 4,578 | 4,624 | **5,030** |
| more but for the allocations' ids / text | 37 / 4 | 37 / 4 | 44 / 4 |
| differ | 716 | 670 | 337 |

Whole modules: `ops::arith` 500, `ops::bit` 1,989, `default` 20, `ops::function` 5, `bool`, `ops::drop`, `ub_checks`; `num::wrapping`
1,158 of 1,201, `cmp` 302 of 374, `iter::range` 204 of 257 (with the two but for the ids), `tuple` 168 of 180, `ptr::non_null` 58 of 70,
`ptr` 38 of 57. The remaining groups (the run's first differing lines and refusals): the interface's missing private fields (55, "a std
field": `ops::IndexRange`'s, its module outside the 35: RF12-P7), paths through a parameter or `Self` to an associated constant (45,
"a path": `T::IS_ZST`, `Self::SIZE`; std's trait constants have no type in std's interface), qualified paths to private traits (41),
user type annotations of `format_args!`'s calls (`fmt::rt::Argument`, `fmt::Arguments`: 59), method calls without a callee (29),
`num::nonzero` paths' annotations (24), `!` locals of empty matches (9). No REAP cell reads the check: Rust's scores are unchanged,
eval and AOT at 0.5.

### RF12-P3 (a): a6's core bodies

Not built in this part. Of a6's four core bodies, `ptr::write_volatile` and its `precondition_check` now build equal to rustc's built
MIR (the latter but for the allocations' ids); `const_ptr::<impl *const T>::is_aligned_to` is in a module outside the slice's list
and `drop_glue` is a shim (no source body: rustc builds it from the type). Taking them into the run needs what the run does not have
yet: a core body built in its own crate's context (core's module as the root, its definitions local) carried into the program's (each
local definition by its interface path), monomorphized (`write_volatile::<i32>`: the run takes rustc's optimized instances, our bodies
are generic built MIR through the 21 passes), and the callees it reaches collected natively (`check_language_ub`, `align_of`: the
record's optimized bodies have them inlined). Proposed as RF12-P6.

### Proposed (RF12's first slice, second part)

- **RF12-P6 A core body in the program's run.** Options: (a) **the run builds the reached core function's module through core-check's
  pipeline once per session (its body, its callees' bodies), each body translated into the program's crate (every local definition its
  interface path, `externalize`), instantiated by the mono item's arguments (`subst` over the body's types), through the 21 passes and
  codegen as ours; taken only when its built MIR equals rustc's (the pack `rust-core-built` extended to the reached modules), the record's
  body the fallback; mono-check's core-closure from the native callees**; (b) wait for RF12's alloc and std slices and do it for every
  library body at once. *Recommended: (a)*, a slice of its own (two to three days): the translation and the instantiation are the
  parts std will need too.
- **RF12-P7 The interface pass at night over all of core.** Options: (a) **the night's core-check over the 35 modules with
  `CORE_IFACE_FILES` naming every non-vendored module (about an hour more), the essential step keeping its six**; (b) only the listed
  modules. *Recommended: (a)*: the bodies then see every private item of core, not the slice's.

## As built: RF12-P6's preparation and RF12-P7's slot (2026-11-09, evening)

The user's decisions (2026-11-09): RF12-P6 (a), RF12-P7 (a) in night 11.

- **RF12-P7**: `core-check` takes `CORE_IFACE_FILES=all` (every non-vendored module file of core) and `CORE_IFACE_ONLY=1` (the interface
  pass alone, its `native-iface.edn` for a later `CORE_IFACE=cache`). Night 11's slot `.tmp/night11/slots/rust-core.sh` (lane LB, after
  rust-rf10b; outside the repository): rust-src extracted in the lane's worktree from the main checkout's pinned apk, the pass over all of
  core, then the slice's modules from the pack with that interface, GOMAXPROCS=2, GOMEMLIMIT=1800MiB, the toolchains stubbed; records
  nothing. Estimated 150 minutes; the whole-core pass's peak is the night's measure.
- **RF12-P6's first condition met for a6**: of a6's four core bodies, the three with source bodies now build equal to rustc's built
  MIR: `ptr::write_volatile`, its `precondition_check` and `<impl *const T>::is_aligned_to` (the latter two but for the allocations'
  ids). `ptr::const_ptr` and `ptr::mut_ptr` join the slice's list (37 modules; the pack `rust-core-built` made again, `b2ac8230...`).
  The fourth, `drop_glue`, is a shim without a source body. What made them equal: the native interface reading `ub_checks`' private
  functions (the pass before the bodies), `format_args!`'s place in a core macro's definition read through the virtual file (`panic!`'s
  `Arguments::from_str` annotation, `library/core/src/panic.rs:62:38: 62:72`), and type-relative and qualified paths keeping their place.
- **RF12-P6 itself, not built yet**: putting those bodies into a6's run (RF11's `build_run.ae`). Three parts remain:
  - the body's forms printed for the program's crate: today they are printed in core's crate context, with crate-relative names, local
    definitions and core's caller locations;
  - instantiation by the mono item: our bodies are generic built MIR, and the run takes rustc's optimized instances (`::<i32>`);
  - the callees those bodies reach, collected and built natively: `check_language_ub`, `align_of`, `const_eval_select`'s
    `runtime`/`compiletime` — rustc's optimized record inlines them.
  mono-check's `core-closure 0 of 3` moves with the third. Next slice.
- Results: the 37 modules (`bin/rust-front core-check --records`, 28 min with the interface pass, 2.40 GB peak): **5,108 of 5,767 bodies
  equal** to rustc's built MIR, 71 more but for the allocations, 381 differ, 217 refused, 74 without a body of rustc's by their key;
  `ptr::const_ptr` 39 of 78, `ptr::mut_ptr` 46 of 87. `bin/rust-front core-essential` and `bin/rust-front essential` still equal their
  known files.

## As built: RF12-P6, core's bodies in the program's run (2026-11-09, night)

- **`bin/rust-front core-export`**: core-check over `test/rust/front/core-native.txt` (a regex of core's names, then the module files:
  the closure the run-built samples reach, a6's), each body run through the 21 passes and written with its equality to rustc's built
  MIR into `.tmp/rust-front/core/native-core.edn`: its forms externalized (local ids as core's def paths, each type form's typeck type
  as `:tc`, named constants as `:nc`, caller locations as `:call-loc`), its promoted bodies, generics and codegen attributes. 48
  bodies, 43 equal; about 40 minutes (`num/mod.rs`), 2.27 GB at GOMEMLIMIT=2000MiB.
- **`bin/rust-front run-built --native-core`** (`RF_CORE_NATIVE`; without it the run is as before): for each core item the program's
  mono items reach, the export's body is printed again in the program's crate at the instance's arguments (types from `:tc`
  substituted, `<T as Pointee>::Metadata` normalized at the instance), LowerIntrinsics run again (since the export: `transmute`,
  `ptr_metadata`, `ub_checks` and its siblings as `RuntimeChecks`), `const_eval_select` remapped to its runtime function, its callees
  collected through mono's `core-facts` and built likewise. A root is taken only when every body of its closure is in the export and
  equal to rustc's; else the record's body stays (`record-kept ... (REASON)` in the log). Drop glue's shim is built natively when the
  glue is the type's own `Drop::drop` alone (shim.rs' elaborated shim; `mono/drop-shim-plan`), else the record's.
- **Results**: a6's core closure, 18 bodies (`write_volatile::<i32>`, `precondition_check`, `is_aligned_to`, `check_language_ub`,
  `maybe_is_aligned`, `align_of`, `str::as_ptr`, `slice::len`, `ptr::metadata` and the rest) and its drop glue's shim, all native;
  **mono-check's core-closure 3 of 3**; the 7 samples' status as rustc's executables (`7 of 7`), no core body from the pack
  (`core's bodies from the pack 0, built natively 18`). Fixes on the way: coercion casts of references to raw pointers
  (`self as *const str`), casts' user types (`ValueTypeAscription`), qualified paths to trait constants, `SizedTypeProperties`'
  constants at the instance, doc comments in macro input, `macro_rules!` in statement position, nested core functions' attributes.
- Left: `mut_ptr::cast_const` (a `*mut T` local for rustc's `*const T`), `slice::as_ptr`/`as_mut_ptr` (the user type's annotation),
  `ptr::metadata::from_raw_parts` (`move` for `copy`) and `<DynMetadata>::size_of` (`VTable` unresolved); none on a6's path. The
  export is not committed (it is the run's input, made by `core-export`); the night or the gate would run `core-export` first.

## As built: RF13 (first slice): RFP42-RFP47 as decided, what runs light (2026-11-10)

Branch `rust-rf13` (from `mac/rust-rf12` `0c22b1f1b`). The user accepted RFP42-RFP47 as recommended (2026-11-09). This slice builds
what of them runs light (one process at a time, at most about 2 GiB: `RF11_PROCS=2`, `RF11_BATCH=3`, `GOMEMLIMIT` 1800MiB) and
prepares the heavy parts for the night.

### RFP44 (a): virtual calls and vtables

- **The slot, natively** (`arbace.rust.front.mono`'s `vtable-entries`, after rustc_trait_selection's `vtable.rs`
  `prepare_vtable_segments`): the metadata entries (drop, size, align), then a depth-first walk of the supertraits (the local
  trait's bounds, std's `names/trait` `:supertraits`), each trait's own vtable-safe methods after its supertraits' (no `Self:
  Sized` clause, no own type or const parameter: std's from the interface's `:predicates` and `:generics`, a local one's from
  `explicit-clauses`), a vptr where a later supertrait with entries follows one with entries. A call on a trait object is
  Instance's `Virtual`: `callee-fact` gives `{:kind "virtual" ... :slot N}` (the object's principal from the `dyn`'s type),
  natively; the record's virtual callees (the facts mono still refuses, std's bodies) get their slot from the call's method
  path and its receiver's printed `dyn` (the principal resolved through std's modules or the crate's traits: `dyn Error`'s
  `Display::fmt` is slot 4 after Debug's and the vptr), counted as `vslots`.
- **The call** (`arbace.rust.back.codegen`, meth.rs' `VirtualIndex::get_fn`): the receiver's pair split, the function loaded from
  the vtable at slot × 8, the data pointer the first argument.
- **The vtable of an unsizing cast in our bodies**: `vtable-alloc` lays it out natively (drop glue's symbol or null, the size and
  alignment from mono's layout, each entry a local impl's method by its v0 symbol) when every entry is a local impl's own
  non-generic method with a reference receiver; else (a default method's instance, std's impls, a closure's `FnOnce` shim: trait
  resolution, which mono does not port) the record's cast of the same target type in the same body, taken in order (counted
  `vtables`). The collector roots a vtable's local methods. A cast between two trait objects of the same principal (a lifetime's
  coercion) keeps the pair (rvalue.rs' `unsize_ptr`).
- Thread locals (`tlsref`, `panic-hashmap-index`): not built (the static's v0 symbol needs std's def path of a `thread_local!`
  closure's static, which std's interface does not hold).

### Codegen's gaps found on the way (std's bodies from the record and ours)

The repeat loop (`[x; N]`: builder.rs' `write_operand_repeatedly`, header/body/next blocks, the MIR block's later code continued
in `repeat_loop_next`; an integer constant whose bytes are one byte a memset, as rvalue.rs), `powif32`/`powif64`, `fmaf32`/`fmaf64`,
`exact_div`, `typed_swap_nonoverlapping` (at -O0 the place swap, not the fallback body), `catch_unwind` (codegen_gnu_try's internal
`__rust_try`, defined once in the module), `type_id_eq` by its fallback body, TypeId constants (a relocation to a `GlobalAlloc::TypeId`
keeps its bytes), `&str` valtree constants (`tyconst`), unsizing a struct's tail (`PolymorphicIter<[T; N]>` to `PolymorphicIter<[T]>`),
transmutes between memory layouts and from memory to a scalar or pair, the discriminant of an SSA local's field, function-pointer
calls in our bodies (no callee to resolve), reified local functions (their symbol and the collector's root), another crate's static
declared external (std's `WHITESPACE_MAP`), promoted `[x; N]`, unit and newtype struct constants. A printer's failure is now the
program's refusal, not the batch's end.

A module that declares an instance of the program's own crate it does not define (a generic local function, a closure or a
derived method reached only through std's bodies, which the collector does not walk) is no longer `own`: `rsi-build` says `a local
instance not collected` (found by `rsi-native`: three programs that passed codegen and the lowering did not link).

### `panic-vec-remove`'s fault

`Vec::remove` is `#[track_caller]`, but mono's attributes (`ext-attrs`) did not find it: the file's impls in order are not rustc's
disambiguators in alloc's `vec/mod.rs` (a `const impl` the parser counts where rustc's `{impl#1}` is the next impl), so the call
passed no location and std's body read a garbage pointer (status 139). A function the file's impls name once is now found by its
name; `bin/rust-front rsi-native panic-vec-remove`: status 101, its outputs as rustc's recorded run.

### RFP45 (a), RFP46 (a), RFP47 (a), RFP42 (a): the night's parts

- **RFP45**: `RF_RSI_UI=40` makes `rfr_rsi_list` add every 40th of `bin/rsi list --set run` (those without compile flags; the
  others listed in `rsi-plan.ui-skipped`), named `ui-` and their id with `/` and `#` as `%` (`rfr_name`), read in the UI corpus's
  source root (`rfr_rsi_dir`: this checkout's or the main checkout's `.tmp/repr-rust/src/rustc-1.99.1-src`), for `records make rsi`
  and `rsi-build` (`RF_SRC_ROOT`, a batch an edition and a root). The UI corpus's sources are not on this host: the night fetches
  them first.
- **RFP46**: as decided, the day runs lists (this slice: 14 programs at a time, about 16 minutes at two threads, under 2 GiB a
  process); the night runs the whole set at `RF11_BATCH=6`.
- **RFP47**: `bin/rsi std-build` takes `RSI_STD_REMAP=/rustc/58cf8f9c35b355199727627d07ae34a4e2846d67/library` (Cargo's trim-paths
  off, the library's absolute and workspace-relative paths remapped as the distribution's build remaps them), into its own
  `RSI_STD_DIR`; `test/rust/runtime/std.edn` takes it with the night's re-pin (the runtime's key holds std.edn: changing it by day
  would make every runtime stale).
- **RFP42**: not buildable by day (no lowered std on this host, RES60 (a)); its night command below.

### The night's commands (lane LB or the oracle tier; none run here)

In the order they depend on each other, each one process at a time:

1. RFP47 (a), the oracle tier (rustc, Cargo, clang): `RSI_STD_DIR=.tmp/rsi/std-remap
   RSI_STD_REMAP=/rustc/58cf8f9c35b355199727627d07ae34a4e2846d67/library bin/rsi std-build --update` (the last build here: cargo 38 s,
   IR 2 s, 103 units), then `grep -c '/rustc/58cf8f9c' .tmp/rsi/std-remap/ir/core/*.ll` (the panics' paths named
   as the recorded runs name them), then std.edn's `:rustflags` takes the remap (a commit: the runtime's key moves) and
   `bin/rsi runtime-ci` makes std's lowered library (about four hours, 6 GiB: RES60 (a)), `bin/rust-front std-crates --rsi` and
   `bin/rust-front records make rsi` (the pack again, 3 minutes).
2. RFP42 (a): `bin/rust-front rsi-build` (RFP46 (a): the whole plan at `RF11_BATCH=6`, `RF11_PROCS=4`, about 30 minutes, 2.4 GB a
   process; the samples' processes above that), then `bin/rsi check --front own --corpus samples,synthetic`; when every own case
   passes or is classed, `--front own` the default of `rsi-check` and `rsi-essential` for those corpora and the record re-recorded.
3. RFP45 (a): `bin/repr-rust corpus fetch rustc-1.99.1-ui` (the UI sources; network), `RF_RSI_UI=40 bin/rust-front records make
   rsi` (about 93 programs, the helper as rustc: about 5 minutes, 3 MB xz more in the pack), `RF_RSI_UI=40 bin/rust-front rsi-build
   ui-...` by edition (the names of `rsi-plan.ui40.tsv`), then `bin/rsi check --front own --corpus run` over them.

### Results

- `bin/rust-front rsi-build` over the synthetic corpus (67 programs, main's stage 0, `RF11_PROCS=2`, `RF11_BATCH=4`, under 2 GiB a
  process, the toolchains stubbed, 0 stub calls; 40 minutes): **47 of 67 own**, the same 47 as RF11's run; the 20 others now stop
  later. Their first refusals before → after: virtual calls 5, unsizing to `dyn` 3, the repeat loop 2, `powi` 2, `typed_swap` 1
  (13, all past) → a local instance not collected 11 (RFP49: `algorithms`, `drops`, `errors`, `float-f32`, `float-f64`, `fmt`,
  `main-err`, `patterns`, `strings`, `structs`, `traits`), the vector ABI 4 (`collections`, `generics`, `iterators`, `rc-refcell`:
  hashbrown's SSE2 groups), `tlsref` 1, an unevaluated named constant 1 (`casts`), a closure's function pointer 1 (`closures`,
  RFP50), and two `main`s not ours (`panic-assert-eq`'s path, `panic-explicit`'s `&str` promoted constant). In the 14 programs
  this slice worked on, the vtables' and virtual calls' facts: the record's virtual callees' slots all computed (`vslots` 2 of 2 to
  4 of 4 a program), the casts' vtables the record's (`vtables` 0 of 2 to 0 of 3:
  every one needed a default method, a closure's shim or std's impl: RFP48, RFP50).
- The native leg (`bin/rust-front rsi-native`, by hand: rustc as the linker driver): **44 of 47 give rustc's recorded run** (43
  before: `panic-vec-remove`); the three left are RFP47's std paths (`overflow-abs-i64`, `overflow-pow-i32`, `panic-alloc`).
- `bin/rust-front essential` equals its known file (5 min 50 s), `bin/rust-front core-essential` equals its own (564 of 567, 10
  min), `bin/reap` 41 of 44 cells checked, 0 problems, `bin/crops-score --judge --strict` exit 0; Rust 75.0 / 75.0 / 95.8 / 80.0,
  unchanged (RFP43 (a): an own case still scores 0.5). `bin/rsi check --essential` (a 4 GiB `--full` step) was not run: it needs
  std's lowered library, which this host does not have, and this slice does not touch its path (`bin/rsi std-build` only under
  `RSI_STD_REMAP`).
- Not reached (item 2 of the brief): the core bodies still not equal to rustc's built MIR (`mut_ptr::cast_const`,
  `slice::as_ptr`/`as_mut_ptr`, `ptr::metadata::from_raw_parts`/`_mut`, `<DynMetadata>::size_of`/`align_of`; the last needs
  foreign types, `extern { type VTable; }`, which lowering marks unsupported).

### Proposed (RF13's first slice)

- **RFP48 Trait resolution in mono.** Every call of a trait method and every vtable entry that is not a local impl's own method is
  still the record's fact (`callee-fact` refuses "a trait method's resolution"; `vtable-alloc` refuses default methods, std's impls
  and shims). Options: (a) **port Instance::resolve's `resolve_associated_item` for impl candidates (the local impls, std's
  `si/impl`s of the interface, the trait's default body instantiated with the impl's arguments) and the builtin candidates mono
  meets (closures' Fn traits, `Clone` of Copy types, drop glue), the facts refused only for what it leaves**, 2-3 days; (b) keep the
  record's facts until RF12's std slices. *Recommended: (a)*: it is the largest share of the refused callee facts (synthetic:
  about a third of each program's callees), and RF12's std bodies need it too.
- **RFP49 The collector through std's bodies.** Three programs pass codegen and the lowering but miss a local instance the module
  declares (generic local functions instantiated inside std's bodies, closures passed to std's adapters, derived methods reached
  through a vtable in std's bodies), now refused as `a local instance not collected`. Options: (a) **mono's collector walks the
  record's std instances' bodies for the local definitions they name (by their DefId and arguments) and builds those instances
  from our bodies (generic ones instantiated as RF12-P6 instantiates core's)**; (b) roots from the record's mono items (the
  record's instance list: a fact of rustc's). *Recommended: (a)*.
- **RFP50 Closures' function-pointer and vtable shims.** `ClosureFnPointer` (`closures`) and a closure's `FnOnce::call_once` in a
  `dyn Fn` vtable need shim.rs' ClosureOnceShim and VTableShim bodies, which only the record has (local, so not taken). Options:
  (a) **build the two shims natively (shim.rs: a call of the closure's body with its environment by reference or by value)**, after
  RFP48; (b) take the record's shim bodies for local closures (a record body in our module). *Recommended: (a)*.

## As built: RF13b, core's bodies equal to rustc's built MIR (2026-11-10)

Branch `rust-rf13b` (from `mac/rust-rf13` `bd6a6ca6f`), light lane (one process at a time, `GOMEMLIMIT=2000MiB`, `GOMAXPROCS=2`).
Every fix below follows rustc 1.99.1's own code (the pinned rust-src and rustc's sources as RF12 read them); none needs a choice.

### RF12-P6's six bodies left

- **`mut_ptr::cast_const`** (`self as _`): check_cast's `try_coercion_cast` now takes `*mut T` to `*const T` (coerce_raw_ptr, the
  operand's `Pointer(MutToConstPointer)`), the cast's `_` unified with the return type; a raw pointer's coercion stays a PtrToPtr
  cast (not a CoercionCast, as check_cast keeps it), and the operand's pointer coercion prints `AsCast` (THIR's
  `apply_adjustment`: `is_from_as_cast` when the expression's parent is a cast).
- **`slice::as_ptr` / `as_mut_ptr`** (`self as *const [T] as *const T`): the cast's user type (`ValueTypeAscription`) is put over a
  coercion cast's `Use` too, as `make_mirror_unadjusted` does for every branch.
- **`ptr::metadata::from_raw_parts` / `_mut`**: a rigid projection is Copy by its where clauses and its item bounds
  (`<T as Pointee>::Metadata: Copy`), so the operand is `copy`.
- **`<DynMetadata<Dyn>>::size_of` / `align_of`** (and `vtable_ptr`): foreign types (`unsafe extern "C" { type VTable; }`): the
  resolver defines a foreign block's types (`ForeignTy`, build_reduced_graph_for_foreign_item), lowering gives each its own item
  and owner (`:foreign-ty`), the collector a def (`ForeignTy`, its path `ptr::metadata::VTable`), the type an ADT without
  arguments. The block's functions and statics are still not defined (none of the 37 modules calls one).

All six now equal to rustc's built MIR (`bin/rust-front core-check` over `ptr/mut_ptr.rs`, `slice/mod.rs`, `ptr/metadata.rs`
with the full oracle).

### The largest classes of RF12's non-equal bodies, by cause

- **A prelude's variant of the root file's own enum** (`Ok`, `Err`, `Some`, `None` in result.rs and option.rs): resolved through
  core's prelude to the interface path, it was std's ADT, so the pattern's bindings and the constructor's arguments stayed
  inference variables (`let _3: _`, `Option::<_>::None`). `res-def` now maps an interface path the root file defines (`*local-paths*`)
  to the local definition, a constructor by its variant or struct with the constructor's kind (rustc's one DefId). The largest
  class: result 36 → 57, option 53 → 92 equal, and method probes through those bindings (`<A as Iterator>::next` on `&mut A`).
- **A trait object's inherent impl** (`impl dyn Any`): its self type's object lifetime is type_of's, not erased
  (`<(dyn any::Any + 'static)>::is::<T>`).
- **Items nested in a function** (`assert_unsafe_precondition!`'s `precondition_check`, an impl inside a method): their MIR name is
  the owner's with the forced impl filename through the whole path (`slice::index::<impl at ..>::get_unchecked::precondition_check`).
- **A match without arms** (`match *self {}`) is `!` (check_expr_match), coerced where used: NeverToAny's temporary.
- **The no-op reference coercion only for the same type**: `self` (`&&T`) returned as `&T` with the elided region its own is
  deref-coerced (`&(*(*_1))`), no longer unified.
- **An overloaded deref under `&mut`** (`&mut *self.borrow_mut()`) is DerefMut (convert_place_derefs_to_mutable; also `&mut v[i]`'s
  IndexMut).
- **Another module's struct fields** (core's `Range` in slice/index.rs): read from the interface's fields (RF12's 55 refusals "a
  std field").
- **An inherent associated constant through a qualified self type** (`<$t>::MIN` in Wrapping's impls): NamedConst; `Self(..)` in an
  impl of a tuple struct the constructor's aggregate (Res::SelfCtor).
- **SwitchInt's values as SwitchTargets holds them** (u128 bits: an i8 discriminant's -1 is 255; codegen sign-extends as before).
- **Two items of one name under one parent** (const_panic!'s `do_panic`s): their key carries rustc's disambiguator (`do_panic#1`),
  as -Zdump-mir names their files; each is now judged against its own dump.
- `[const]` and `const` bounds kept in lowering and printed in an impl Trait parameter's name (`impl [const] FnOnce(T) -> bool`);
  a format placeholder's span from a user macro's literal kept at the root's place; `assert!`'s message from a run of root tokens a
  macro passed on is their text.

### Results

- `bin/rust-front core-check --records` over the 37 modules (about 50 minutes under the host's load, the interface pass 20 of them,
  under 2.5 GB): **5,398 equal** to rustc's built MIR (5,108 at RF12), 114 more but for the allocations (71), 148 differ (381), 117
  refused (217), 74 without a body of rustc's by their key. No module lost an equal body.
- What is left, by cause (`[:differ ...]` rows): the format placeholder span through core's `panic!` macros (`span: ?` in
  `Argument::new_display`/`new_debug`'s user types, 26), user types' canonical variables for an impl's parameters
  (`NonZero<^c_0>` for `NonZero<u8>`, 13), Copy of local ADTs whose Copy impl is in the root file (`PhantomData`, `MaybeUninit`:
  `move` for `copy`, 4), `assert!`'s message through `debug_assert!` (6), array lengths not evaluated (`16 / size_of::<*const ()>()`,
  2), `IntoIterator::into_iter` not inserted for `from_iter`'s argument (2); refusals: paths to private associated constants
  (`T::IS_ZST`, `Self::SIZE`, 48 "a path"), method calls without their callee (28), and desugarings lowering marks `:err` (25).
- `bin/rust-front core-export` (48 bodies, about 50 minutes under the host's load, 2.3 GB): **32 of 32 equal** where the pack
  `rust-core-built` holds rustc's body; the 16 others (`str`, `slice`, `fmt` and `num`'s, outside the pack's 37 modules) have no
  oracle in the pack, so the export marks them not equal and `run-built --native-core` keeps the record's body for them. Against
  the full core oracle (core-oracle's dump, by hand) `str`'s, `slice`'s (`as_ptr`, `as_mut_ptr` among them) and `fmt`'s 10 are
  equal too; `num`'s six (`is_power_of_two`, equal at RF12) were not run again. RF12-P6's "43 equal" was measured against that full
  oracle, not the pack.

### Proposed (RF13b)

- **RFP51 The pack for the export's modules.** core-export judges its bodies against `rust-core-built`, which holds only
  core-check's 37 modules, so `str`, `slice`, `fmt` and `num`'s exported bodies are never taken by `run-built --native-core`.
  Options: (a) **a list of its own, `test/rust/front/core-export-modules.txt` (`str/mod.rs`, `slice/mod.rs`, `fmt/mod.rs`,
  `num/mod.rs`), whose bodies `bin/rust-front core-records make` adds to the pack (the oracle tier, rustc as the helper: one dump of
  core as today, a re-make of the pack), core-check's default list unchanged**; (b) add the four modules to core-modules.txt (the
  pack grows the same, but every core-check run then takes num/mod.rs's 40 minutes); (c) leave it (the run takes 32 native bodies,
  the record's for the rest). *Recommended: (a)*.

## As built: RF13c, more of core's bodies equal to rustc's built MIR (2026-11-10)

Branch `rust-rf13c` (from `mac/rust-rf13b` `0d44a0343`), light lane (one process at a time, `GOMEMLIMIT=2000MiB`, `GOMAXPROCS=2`).
RF13b's remaining causes taken cheapest per body first; each fix follows rustc 1.99.1's own code, none needs a choice.

### Causes fixed

- **A trait's associated constant through a type-relative path** (`T::IS_ZST`, `Self::SIZE`, `Dst::SIZE`: RF13b's 48 "a path"
  refusals' largest group): the trait's item found through a trait in scope (std's `SizedTypeProperties` too, its constant's type
  from core's interface), a parameter's bounds or a trait's `Self`, is THIR's NamedConst at the trait's item with Self the type,
  and its user type `TypeOf(<T as mem::SizedTypeProperties>::IS_ZST)` (instantiate_value_path).
- **A parameter's where clauses as method candidates** (assemble_inherent_candidates_from_param): `self.0.fmt(f)` under
  `T: fmt::Debug` with only the module `fmt` imported, `range.start_bound()` under `R: ops::RangeBounds<usize>`; a projection's
  item bounds likewise (`self.0.hash(state)` on `<T as DiscriminantKind>::Discriminant: Hash`): RF13b's "a method call without its
  callee" in Wrapping's, Range's, Discriminant's impls and `slice::index::range`.
- **An impl item's user type** (write_user_type_annotation_from_args): the impl's parameters fresh variables, apart from the
  written self type, which is the impl's own self type for `Self` (`NonZero<^c_0>::new_unchecked at <impl .. for NonZero<u8>>`;
  `MaybeUninit<^c_0>::uninit at <impl .. for MaybeUninit<^c_1>>`).
- **One substituted token keeps its own place** (transcribe.rs' maybe_use_metavar_location returns the token unchanged unless its
  span collides): a token passed on by `$($arg)*` or `$t:tt` was given its metavariable's place when it had more than one
  character. This fixes the format placeholders' spans through `const_panic!` (`span: ?`), `debug_assert!`'s message
  (`is_reading ( borrow )`) and `const_assert!`'s values' spans.
- **A const item's initializer has a destruction scope** (region.rs' visit_body for a body that is not a function's): a match's
  scrutinee temporary dies at the initializer's end (`SizedTypeProperties::MAX_SLICE_LEN`).
- **Copy of the root file's own trait** (marker.rs: `Copy`, `Sized`, `PointeeSized` are the crate's): `PhantomData<T>: Copy` by the
  local impl; **`#[derive(Copy)]` on a union** (MaybeUninit's) expanded as a struct's.
- **Array lengths of `size_of::<T>()`** for a thin pointer, unit or primitive (TypeId's `16 / size_of::<*const ()>()` is 2).
- **A std enum's unit variant named by its variant's path** (a derive's `$crate::cmp::Ordering::Equal`): its value; a tuple
  variant's constructor function (`$crate::option::Option::Some`).
- **Try blocks** without a `?` of their own (lower_expr_try_block: the tail wrapped in `Try::from_output`).
- **Inline consts** (`const { intrinsics::type_name::<T>() }`): lowered with their `{constant#N}` in the fn's DefCollector order
  (array lengths, repeat counts, const blocks and braced arguments under the item, closures' and nested items' apart), typed in
  the enclosing body (an InlineConst shares its parent's typeck root), and in THIR the constant `const any::type_name::<T>::
  {constant#0}`. The inline const's own body is not built yet.

### Results

- `bin/rust-front core-check --records` over the 37 modules (about 50 minutes, under 2.5 GB): **5,474 equal** to rustc's built MIR
  (5,398 at RF13b), 155 more but for the allocations (114: most of the bodies whose format placeholders' spans were wrong now
  differ only by allocation ids), 85 differ (148), 63 refused (117), 74 without a body of rustc's by their key. No module lost an
  equal body; the largest gains iter::range +26 (NonZero's user types), mem +9, ptr +8, any +7, num::wrapping +6.
- Refusals left: 36 "a path" (const parameters as values in array methods, `unicode::*` tables of an unread module, a few
  private paths), 14 method calls without their callee (crate-private methods of other files: `IndexRange`'s, RFP52), 13 others.

### Left

- Crate-private items of files outside the 37 (RFP52); a const parameter used as a value (`N` in `as_array`, `spec_next_chunk`);
  the inline const's own body (`{constant#N}`: the oracle has them, core-check does not build them yet); an unnormalized alias in
  a signature (`ChangeOutputType` in `try_from_fn`); user types of derive's `AssertParamIsClone` spans; the allocation ids.

### Proposed (RF13c)

- **RFP52 core's crate-private items in the interface pass.** Bodies of the 37 modules name `pub(crate)` items of other files,
  which std's interface (rustdoc's JSON) does not hold: `ops::IndexRange`'s `start`/`end`/`len` (6 bodies of slice/index.rs),
  `iter::adapters::try_process` (result's and option's `from_iter`), `clone::uninit::CopySpec` (2 in the essential subset).
  RF12-P2's interface pass collects only the checked files. Options: (a) **a list `test/rust/front/core-iface-extra.txt`
  (`ops/index_range.rs`, `iter/adapters/mod.rs`, `clone/uninit.rs` to start) whose files the interface pass collects too, before
  any body, in every core-check run (a few minutes more)**; (b) `CORE_IFACE_FILES=all` by default (every module file, RF12-P7's
  night pass, far longer); (c) leave it. *Recommended: (a)*.

## As built: RF13d, core's bodies equal to rustc's built MIR, further (2026-11-10)

Branch `rust-rf13d` (from `mac/rust-rf13c` `7f153a722`, `mac/rust-rf13b`'s review fix `ab753d5d0` merged), light lane (one
process at a time, `GOMEMLIMIT=2000MiB`, `GOMAXPROCS=2`). RF13c's decision-free causes and the port gaps among its differing
bodies, cheapest per body first; each fix follows rustc 1.99.1's own code, none needs a choice. RFP52's crate-private items
(`NeverShortCircuit`, `IndexRange`, `try_process`, `ChangeOutputType`, `unicode::*`, `drain::Drain`) stay out, as decided.

### Causes fixed

- **A const parameter used as a value** (`repeat_n(val, N)`, `as_array::<N>`, `spec_next_chunk`; THIR's ConstParam): the
  constant `const N` (Const::Ty of the parameter); an array of length `N`'s bounds check compares with `const N` likewise.
- **Inline consts' own bodies** (RF13c lowered them only in the parent): each `const { .. }` of a fn is a body
  `f::{constant#N}` built as construct_const builds a constant's (its typeck results the parent's, its region tree a non-fn
  body's), judged against the oracle's `{constant#N}` dump.
- **Constants check overflow** (Builder::new: BodyOwnerKind::Const): `Duration::MAX`'s `NANOS_PER_SEC - 1` is a checked
  subtraction; inline consts too.
- **Derive's assertions' spans** (assert_ty_bounds: `let _: AssertParamIsClone<T>;` at the field's span): Clone's and Eq's
  user types now name the field (`panic/location.rs:43:5: 43:27`), not the derive.
- **An item nested in a method** of a const or static kind (`from_nanos::NANOS_PER_SEC`, `to_digit::TO_UPPERCASE_MASK`): its
  MIR name through the method's `<impl at ..>`.
- **A const closure's declaration span** (`const move |a| ..`): from `const` through its bars.
- **The crate's own ManuallyDrop never needs drop** (`#[lang = "manually_drop"]`, AdtDef::is_manually_drop).
- **A trait item through a parameter's where clause** (`R::from_residual(v)` under `R: Try`): its user type's trait
  arguments are fresh (`TypeOf(<R as FromResidual<^c_0>>::from_residual)`).
- **A trait's written arguments on its segment** (`AsRef::<[MaybeUninit<T>; N]>::as_ref(self)`): the trait's parameters after
  Self.
- **An impl Trait argument takes no turbofish argument** (`from_raw_parts::<U>`: `U` is `T`'s, the synthetic `impl Thin`
  fresh).
- **`Self` in an impl**: a struct pattern or expression of a braced struct (`let Self { start, end, exhausted } = self`,
  `Self { value }`), with the struct expression's user type `TypeOf(RangeInclusive<Idx>)`; a unit struct's `Self` a
  ZstLiteral (`const ZeroSized: PhantomData<T>`) with its user type.
- **Unsizing coercion casts**: `self as &[T]` from `&[T; N]` (check_casts' try_coercion_cast, `PointerCoercion(Unsize,
  AsCast)`), and `*const [T; N]` to `*const [T]` (coerce_unsized of raw pointers).
- **`write_via_move`** (into.rs's intrinsic lowering): `*ptr = val` without a call or a drop (`ptr::write`, `mem::replace`).
- **Char constants as char's Debug writes them** (`'\u{10ffff}'`, `'\u{7f}'`; a non-printable or grapheme-extending
  character escaped), and an escaped char literal's value its code point, not its first UTF-16 unit.
- The impl header's def_span to its self type when that is on a later line (`impl .. FromResidual<..>\n for Result<T, F>`).
- Ported without a gain yet: expected_inputs_for_expected_output's generalization (a variable the probe binds takes the
  generalized value, its free regions fresh); `Some(slice)` against an expected `Option<&[T]>` still copies where rustc
  reborrows, so its cause is elsewhere.

### Results

- `bin/rust-front core-check --records` over the 37 modules (`CORE_IFACE=cache`, about 50 minutes, under 2.5 GB): **5,538
  equal** to rustc's built MIR (5,474 at RF13c), 157 more but for the allocations (155), 49 differ (85), 52 refused (63), 74
  without a body of rustc's by their key. No module lost an equal body; the largest gains array +14, time +12, char::methods
  +6, any +5.
- `bin/rust-front core-essential` and `bin/rust-front essential`: equal to their known files.
- Left, by cause: RFP52's crate-private items (the most of the 52 refused: `unicode::*` and the case tables 18, `NeverShortCircuit` 4,
  `IndexRange` 6, `try_process`, `ChangeOutputType`, `drain::Drain`, `ascii::Char`); `macro_rules!` metavariable spans that
  collide across expansions (tuple.rs' `Default`, 12: transcribe.rs' global metavar_spans table); braced const arguments
  evaluated (`{ Locality::L3.to_llvm() }`, 3); `slice_get_unchecked`'s `T` through ChangePointee's associated type equality
  (4); `offset_of!` in inline consts (4); the reborrow of `Some(slice)` against an expected `Option<&[T]>` (1); a few drop
  orders and temporaries (`try_from_fn_erased`, `encode_utf8_raw`'s never temporaries).

## As built: RF13e, core's bodies equal to rustc's built MIR, further; RFP52 (a) (2026-11-10)

Branch `rust-rf13e` (from `mac/rust-rf13d` `011d4cc3b`), light lane (one process at a time, `GOMEMLIMIT=2000MiB`,
`GOMAXPROCS=2`). RF13d's decision-free causes, cheapest per body first, each after rustc 1.99.1's own code (the pinned
rust-src's `compiler/`); then RFP52 (a), decided by the user on 2026-11-10, measured apart.

### Causes fixed

- **`offset_of!`** (`const { builtin # offset_of(C, f) }`): hir::ExprKind::OffsetOf lowered, check_expr_offset_of's
  (container, variant, field) indices (an enum's variant then its field, a struct's or tuple's field), thir cx's lowering
  to a call of the `offset_of` lang item intrinsic per field (`offset_of::<option::Option<T>>(const 1_u32, const 0_u32)`,
  not from HIR, the expression's span), their sum for several; the fields of an expression fragment (`$($fields:expr)+`)
  taken from its field chain as parse_floating_field_access takes them from the nonterminal.
- **An inline const's own inline consts**: the DefCollector's anonymous constants nested under the one they are in
  (`ptr::read_unaligned::{constant#0}::{constant#0}`), built as bodies and named so in their parent's MIR.
- **Generalization freshens a body's late-bound regions** (generalize.rs: every region but one bound inside the type):
  `Some(slice)` against an expected `Option<&[T]>` now reborrows `&(*_2)` (RF13d's open cause: the expected input's region
  stayed the parameter's, so coercion's no-op case applied).
- **The return coercion is a CoerceMany** (ret_coercion): the first `return` (or the body) coerced, a later one by
  try_find_coercion_lub, whose fast path leaves a type equal to the merged one alone (a body tail after `return`s: no
  `&mut *` reborrow).
- **`Self { .. }` patterns of another file's struct** (`let Self { start, end, exhausted } = self` on `ops::RangeInclusive`).
- **The builtin Copy and Clone of tuples and arrays** (`Result<(), fmt::Error>: Copy` through `(): Copy`): `copy _3`.
- **`Destruct` holds for every type** (its builtin candidate): `Guard<'_, T>`'s Drop impl applies, the guard is dropped.
- **A crate constant in a pattern** (`UNUSED =>`): DefKind::Const as 1.99 prints it (`Const { is_type_const: false }`)
  recognised, the pattern of the constant's type, its value from its expression of literals (const_to_pat's constant).
- **An operator on a rigid projection by its item bound** (`<T as DiscriminantKind>::Discriminant: PartialEq`: the alias
  bound candidate): `PartialEq::eq(&a, &b)`.
- **convert_place_derefs_to_mutable in the builder's table** (`bytes[a..b].copy_from_slice(..)`: IndexMut).
- **partial_ord.rs' exceptions** to the simple body (`Some(Ord::cmp(..))`): a unit struct, an enum without variants or
  with one unit variant keep the default body (`PhantomPinned`).
- **A let type's elided regions are fresh region variables** (re_infer): `let s: &[U] = unsafe { transmute(x) }` is no
  reborrow.
- **RFP52 (a)**: `test/rust/front/core-iface-extra.txt` (index_range, iter::adapters, array::drain, clone::uninit,
  intrinsics::bounds, char::decode, char, ascii::ascii_char, unicode, unicode_data) collected by the interface pass in
  every run before any body (`CORE_IFACE_EXTRA`: another list, empty for none; about 75 s); the native interface also
  writes the impls the interface lacks (inherent and trait, at their `{impl#N}` paths, with their functions), indexed as
  load-std-iface indexes its own; core's index takes a uniform path's re-export (`use unicode_data::alphabetic::lookup as
  Alphabetic`) as the module's own.

### Results

- `bin/rust-front core-check --records` over the 37 modules (`CORE_IFACE=cache`, under 2.5 GB): before (RF13d's run at
  `011d4cc3b`) 5,538 equal; the causes above without RFP52 (`CORE_IFACE_EXTRA=`, RF13d's index) **5,551** (+13: slice::index
  +3, option +2, ptr +2, any, array, cell, marker, mem, mem::maybe_uninit +1 each), 157 but for the allocations, 42 differ,
  44 refused; with RFP52 (a) and core's index made afresh **5,576** (+25: char::methods +15, the unicode tables and
  `decode_utf16`; slice::index +10, `slice_get_unchecked`'s `T` through the bounds impls and `IndexRange`'s methods), 157,
  37 differ, 24 refused, 74 without rustc's body. No module lost an equal body at either step.
- `bin/rust-front core-essential` (with the extra list) and `bin/rust-front essential`: equal to their known files;
  `bin/reap` 0 problems; `bin/crops-score --judge --strict` passes.

### Left

- tuple.rs' `Default` (12): rustc's metavariable spans (transcribe.rs' maybe_use_metavar_location, the global
  MetavarSpansMap keyed by span and syntax context, read back by Span::to's try_metavars) decide which `$T` gets its
  definition's span; a faithful port needs hygiene contexts, rustc's expansion order and the parser's span combinations,
  not ported (no guess).
- Braced const arguments evaluated (`{ Locality::L3.to_llvm() }`, 3, and their `{constant#N}` bodies): needs const
  evaluation of a call (a CTFE of our MIR or an evaluator over HIR); the user type would print the braced snippet.
- RFP52's rest: `ChangeOutputType` (a crate-private type alias: the interface has no aliases), `clone::uninit`'s
  `CopySpec` qualified path (2), `drain::Drain`, `ascii::Char` (2), `try_process`'s turbofish (result's and option's
  `from_iter`), `NeverShortCircuit`'s bodies now differ rather than refused (from_fn, map, fold, rfold), `MAX_ONE_B` (a
  crate-private constant's value in a pattern).
- Others: `array::clone`'s `DefId(0:5755 ..)` (a DefIndex), `encode_utf8_raw`'s never temporaries, `encode_utf8_raw_unchecked`,
  `transmute_prefix`'s nested struct, `downcast_unchecked_*`'s cast user types, `cell::clone`, `assert_coerce_unsized`,
  `swap_nonoverlapping_bytes`, `get_or_try_insert_with`, `flatten_ref`, cmp's `smallest`/`largest` (macro impl spans).

## As built: RF14 (RFP48): trait resolution in mono; RFP49 (a) begun (2026-11-10)

Branch `rust-rf14` (from `mac/rust-rf13e` `db5fe592e`), light lane (one process at a time, `GOMEMLIMIT=2000MiB`, `GOMAXPROCS=2`,
`RF11_BATCH` 2 or 3). The user decided RFP48-RFP61 (a) on 2026-11-10; this slice builds RFP48 (a) and the first part of RFP49 (a).

### RFP48 (a): Instance::resolve for trait methods

- **Impl candidates** (`arbace.rust.front.typeck/resolve-assoc-fn`, after rustc 1.99.1's rustc_ty_utils/src/instance.rs
  resolve_associated_item and rustc_trait_selection's specialize/mod.rs and specialization_graph): the impls of the trait (the
  crate's and std's interface's, by the self type's key) whose header unifies with the concrete trait reference and whose
  clauses hold; with several (specialization), the chain ordered leaf first by `specializes` (an impl's header at its own
  parameters an instance of the other's), the leaf required to specialize every other (else refused: "overlapping impls"); the
  item from the first impl of the chain that defines it, at the impl's arguments and the method's own (rebase_onto), else the
  trait's default body at the call's arguments; an impl argument its header does not determine refused.
- **Builtin candidates** (`arbace.rust.front.mono/resolve-instance`): the Fn traits on a closure (its body when the closure's
  kind is the trait's or Fn called as FnMut, else `ClosureOnceShim`, the `closure-once-shim` callee) and on a function item or
  pointer (`FnPtrShim`, `fnptr-shim`); the closure's kind read from its built body's self (`&C`, `&mut C`, `C`). Clone's shims
  and the other builtin impls stay refused ("a builtin candidate").
- **The facts that use it**: each call's callee and function constant's symbol (`callee-fact`, `call-fsym`), the collector's
  local items (a trait call resolved to a local function at no type argument), the external items (`:ext-items`: the resolved
  instance, so RF12-P6's native core plan matches more of the record's std bodies), and the vtables (`vtable-alloc`: an entry
  that is not a local impl's own method resolved, its instance's symbol: std's impls, generic impls, default bodies; a by-value
  receiver's VTableShim and a supertrait with parameters still refused).
- **Found on the way**: v0's trait-qualified paths (default_print_def_path's `trait_qualify_parent`: `<Self as Trait>::item`,
  `Y`), trait objects (`D`, the principal at a dummy Self, `p` projections, auto traits, `L_`), local closures (by their
  `{closure@..}` text, typeck's fn_decl_span) and fn pointers (`F`); an impl's disambiguator in rustc's order of definitions (a
  derive's impls after the crate's other items, as rustc-def-index orders them; the source's order gave `<V2 as Add>::add`
  `s9_` for rustc's `s2_`); `#[track_caller]` inherited by a trait impl's method from its trait's method (codegen_fn_attrs:
  `<Grid as Index>::index`); core's attributes of a trait's provided method (`Iterator::map`) and of an impl whose file order is
  not rustc's disambiguator (the impl found by its header's names, std's interface's against the source's: `impl From<&str> for
  String` among string.rs' 82 impls); a call's function constant whose callee's attributes core's sources do not show refused
  as its callee is (`checked_add`'s macro-made impl).

### RFP49 (a), the first part: the collector through std's bodies

`build-run`'s native run hands mono the function constants of the record's std instances' bodies (`(fndef PATH [ARGS])`, rustc's
std bodies: RF12's part); mono resolves each trait method's (PATH as def_path_str prints it, std's trait through its modules, the
arguments by `form->ty`, a local closure by its text) and adds the local functions and closures it reaches at no type argument
to the collector's items (closures' bodies now items too). A generic local function or a closure of a generic one reached only
through std (an instantiation of our bodies, as RF12-P6 instantiates core's) is not ported yet: RFP49's rest.

### Results

- **The mono facts against the record** (`rsi-build`'s mono-check of the same run, each batch's lines now kept in
  `mono-all.log`), the 8 programs RF13 left with the most refused callees (traits, closures, drops, errors, structs, strings,
  algorithms, patterns; `RF11_BATCH=2`), before (`db5fe592e` in a worktree of its own, the same logging) and after RFP48 (a):
  callees equal **406 -> 592** of 710, function constants' symbols 368 -> 557 of 564/567, core's items reached 314 -> 483 (all
  equal), mono items' symbols 40 -> 50 of 128; the native callee facts 731 -> 964 of 987/993 (the refusals 263 -> 29: 21 core
  items whose attributes core's sources do not show, macro-made, 6 calls with no fact, 2 `Box<dyn Fn>`'s `Fn` impl); no
  callee fact mono gives differs from the record's (MONO_VERBOSE over traits, errors, structs, patterns: the differences left are
  facts mono does not give).
- **The whole synthetic corpus** (`bin/rust-front rsi-build` over its 67 programs, `RF11_BATCH=3`, two threads, under 2 GiB a
  process, the toolchains stubbed, 0 stub calls; about 75 minutes): **48 of 67 own** (47 at RF13: `patterns` now, through RFP49's
  first part: `<Shape as Debug>::fmt`, reached through std's formatting bodies). The native callees 4,079 of 4,524 (RF13: 3,503 of
  4,288), the vtables 25 of 32 (0 of 7); the function constants' symbols 1,739 of 2,135 (1,911 of 1,982: those whose callee's
  attributes core's sources do not show are now refused, as their callee is: the record's, where the default rules gave
  `checked_add`'s and `<&i32 as Rem>::rem`'s wrong symbol). The programs RF13 stopped at "a local instance not collected" now stop
  later: errors, float-f32, float-f64, strings and panic-explicit at a promoted constant in `main` (`promoted: a constant's
  value`, `an rvalue binop`), fmt at a callee of `Argument::new_debug::<Option<_>>` (an uninferred type in our body), and the
  rest at a generic local instance reached through std (algorithms' `quicksort::<f64>`, traits' `<Dog as Animal>::describe`,
  structs' `<[Token]>::len`, main-err's `<Failure as Debug>::fmt` through a generic, panic-assert-eq's `assert_failed::<i32, i32>`,
  drops' drop glue): RFP49's rest; the vector ABI 4 (RFP58), `casts`' unevaluated constant, `closures`' reified closure
  (RFP50), `panic-hashmap-index`'s `tlsref`.
- `bin/rust-front essential` equals its known file (6 min 27 s; the staged records copied from another worktree name its paths:
  re-staged); `bin/rust-front core-essential` equals its own (564 of 567, 8 min 43 s; core's caches copied from another worktree name its
  paths: rewritten); `bin/reap` 41 of 44 cells checked, 0 problems; `bin/crops-score --judge --strict` exit 0, Rust 75.0 / 75.0 /
  95.8 / 80.0, unchanged (RFP43 (a): an own case still scores 0.5).

### Left (RFP49's rest and the rest of RFP48)

- Generic local instances reached only through std's bodies (`quicksort::<f64>`, a trait's default body at a local Self,
  `<[Token]>::len`): mono's collector would instantiate our generic bodies at the arguments std's bodies give (RF12-P6's way for
  core's); the next part of RFP49 (a).
- RFP48: Clone's shims and the other builtin impls (a `CloneShim`, `Box<dyn Fn>`'s `Fn` impl through the object's own
  candidate), a vtable's by-value receiver (VTableShim) and a supertrait with parameters; core items whose attributes a macro
  makes (the integers' and slices' iterator impls, `default_impl!`, `forward_ref_binop!`) are the record's, as before.

### The night's commands (none run here)

- RFP46 (a)'s whole plan again, now with RFP48 and RFP49's first part: `RF11_BATCH=6 RF11_PROCS=4 bin/rust-front rsi-build`
  (samples and synthetic, about 30-60 minutes, 2.4 GB a process; the samples' processes above that), then `bin/rust-front
  rsi-native` by hand over the own programs (rustc as the linker driver) to confirm the new own program (`patterns`) runs as
  rustc's recorded run, then `bin/rsi check --front own --corpus samples,synthetic` (RFP42 (a)).

## As built: RF15 (RFP49 (a)'s rest): generic local instances, promoted constants, the own programs' next causes (2026-11-10)

Branch `rust-rf15` (from `mac/rust-rf14` `e1bdba384`), light lane (one process at a time, `GOMEMLIMIT=2000MiB`, `GOMAXPROCS=2`,
`RF11_BATCH` 3). RF14's caches were copied into the worktree and their absolute paths rewritten (`rust-rf14/` to `rust-rf15/`)
before any run.

### RFP49 (a)'s rest: generic local instances

- **The collector's items** (`arbace.rust.front.mono/program-facts`): an item is a local definition at no type argument, as
  before, or `[DEF ARGS]`, a local generic function's, a generic impl's method's, a trait's default body's (at its Self) or a
  generic function's closure's instance at concrete arguments (`local-generic-instance?`: no parameter, inference variable or
  projection left). They are reached from our bodies' calls (a local generic function at concrete arguments, a trait call
  resolved by RFP48 to a local generic instance), from std's bodies' function constants (RFP49's first part, now with the
  arguments) and from the vtables' entries; each instance's body is ours instantiated
  (Instance::instantiate_mir_and_normalize_erasing_regions: every type form from its `:tc` substituted, normalized and erased,
  a function constant's text from its type, a named constant's arguments, a const parameter's value as an integer scalar, a
  repeat's count from the builder's kept constant), so the instance's own calls are collected in turn
  (`quicksort::<f64>` calling itself). Its name is the definition's MIR name with its type arguments (`quicksort::<f64>`), its
  header's symbol v0 at the arguments; build-run instantiates our runtime body (`mono/instantiate-runtime`, the promoted
  bodies and the Assert spans with it).
- **std's vtables** (`std-fn-syms`): the function allocations of the record's std bodies (a vtable's entries:
  `<Box<dyn Error> as From<Failure>>::from`'s holds `<Failure as Debug>::fmt`), matched against the local functions' symbols,
  join the roots.
- **v0**: `core::ptr::drop_glue` is not in std's interface (`pub(crate)`): its one parameter, so the glue's symbols carry their
  type (`drop_glue::<Noisy>`, the vtable's drop entry); a trait object's auto traits in rustc's interned order
  (ExistentialPredicate::stable_cmp, by DefPathHash: `Sync` before `Send`, as rustc 1.99.1's symbols show; others after, in
  their written order): errors' `<Box<dyn Error + Send + Sync> as ToString>::to_string` was declared under the other order. A record's trait object of a local trait without parameters (`(dyn)`, its text `dyn Animal`) is read
  as that trait's object (form->ty), so a std call's arguments resolve.

### Promoted constants (RF10's slice 5, further)

`build-run/eval-promoted`, as rustc_const_eval interprets a promoted body: binary and unary operators on scalars (integers
wrapped; floats as IEEE, `%` as fmod; comparisons), numeric casts (IntToInt, IntToFloat, FloatToFloat, FloatToInt saturating),
a string's or byte string's constant (a pointer with its relocation to the bytes' allocation), a reborrow `&*p` (the same
pointer), a borrow of another local (a nested allocation), an enum's variant with or without fields and a tuple or struct of
several fields (each field at its offset in the type's layout, the tag by its encoding: `mono/type-layout-form`, else the
record's layout of that type), a variant's explicit discriminant (`Color::Green::{constant#0}`), f32's and f64's associated
constants (`std-const-value`: MAX, MIN, MIN_POSITIVE, EPSILON, NAN, INFINITY, NEG_INFINITY and the integer ones).

### Others found on the way

- LowerSliceLenCalls (`build-run/lower-slice-len-calls`, rustc_mir_transform's lower_slice_len.rs, on at mir-opt-level 1, so
  at -O0): `<[T]>::len` becomes `PtrMetadata`, as rustc's optimized MIR has it (`structs`' `<[Token]>::len` was declared and
  never defined).
- An overloaded compound assignment (`String += &str`): THIR's overloaded_operator over OpAssign's method (build.ae), where the
  builder made a builtin Add of two references.
- A unit variant's turbofish (`None::<u8>`, typeck's apply-turbofish: the variant's segment's arguments are its enum's).
- `repr(u8)` enums' layout: adt-repr read `#[repr(u8)]` as a regex match's vector (`re-matches` with a group), so every
  explicitly sized enum's layout failed (mono's layout fact the record's).

### Results

- **The mono facts against the record** (`rsi-build`'s mono-check, the lines of the 8 programs RF14 measured: traits,
  closures, drops, errors, structs, strings, algorithms, patterns; this time from the 67 programs' run, `RF11_BATCH=3`):
  callees equal **592 -> 660** of 710, function constants' symbols 557 -> 605 (of 567 -> 649: the instances' own constants
  join), core's items reached 483 -> 630 (all equal), mono items' symbols 50 -> 105 of 128, items 100 -> 210 (of 512 -> 529),
  layouts 10,225 -> 10,974 of 12,588; the native callee facts 964 -> 1,070 of 1,122 (52 refused).
- **The whole synthetic corpus** (`bin/rust-front rsi-build` over its 67 programs, `RF11_BATCH=3`, two threads, under 2 GiB a
  process, the toolchains stubbed, 0 stub calls; 47 minutes): **56 of 67 own** (48 at RF14): algorithms, drops (generic
  local instances, the glue's symbol), errors, panic-explicit, float-f32, float-f64, strings (promoted constants; strings also
  the overloaded `+=`), main-err (std's vtables). The native callees 4,275 of 4,739 (RF14: 4,079 of 4,524), the vtables 29 of
  36 (25 of 32), the function constants' symbols 1,798 of 2,215. The 11 left: the vector ABI 4 (collections, generics,
  iterators, rc-refcell: RFP58, below), promoted constants that need more of CTFE 3 (fmt: a call, `std::ptr::null::<u8>()`, in
  a promoted body; structs: a checked `AddWithOverflow` and its Assert, an implicit discriminant after an explicit one;
  traits: `&[&Dog, &Bird(1)]`, an unsizing cast to `&dyn Animal` with its vtable inside the promoted), panic-assert-eq
  (`assert_failed::<i32, i32>` is std's: the record's callee names std's upstream copy, rustc's share-generics, which mono
  does not know: the instantiating crate is ours), casts (a layout's variants codegen does not take), closures (RFP50's
  reified closure), panic-hashmap-index (`tlsref`).
- `bin/rust-front essential`: one line of its summary moved, the mono-check's total: a6's `drop_glue::<D>` is now a core item
  whose symbol mono gives equal to the record's (core-items 1 of 1 -> 2 of 2, core-closure 0 of 3 -> 0 of 2), every other
  line equal; `test/rust/front/essential-known.txt` takes the line (every fact still equal). `bin/rust-front core-essential`
  equals its own (564 of 567, 5 min 28 s), `bin/rust-front essential` 4 min 17 s. `bin/reap` 41 of 44 cells checked, 0
  problems; `bin/crops-score --judge --strict` exit 0, Rust 75.0 / 75.0 / 95.8 / 80.0, unchanged (RFP43 (a): an own case still
  scores 0.5 until S6's switch).

### RFP58 (a), the vector ABI: the design note (not built: not small)

The four synthetic programs codegen refuses with `unsupported abi other` (collections, generics, iterators, rc-refcell) all
reach hashbrown's SSE2 group through std's HashMap: the record's bodies of `hashbrown::control::group::sse2::Group`'s
`load`, `load_aligned`, `store_aligned`, `match_tag`, `match_empty`, `match_empty_or_deleted`, `match_full` and
`convert_special_to_empty_and_full_to_deleted`, over three 16-byte, 16-aligned types whose layout the helper prints `(other)`
(rustc's BackendRepr::SimdVector): `__m128i`, `core::core_arch::simd::Simd<i8, 16>` and `Group`, and six SIMD intrinsics,
`simd_splat`, `simd_eq`, `simd_lt`, `simd_gt`, `simd_or` and `simd_bitmask` (`_mm_set1_epi8`, `_mm_cmpeq_epi8`,
`_mm_movemask_epi8`: stdarch's sse2.rs writes them over the generic intrinsics, no `llvm.x86.*` call).

- **Layout** (mono): `#[repr(simd)]` (stdarch's `Simd<T, N>([T; N])`) laid out as rustc_abi's vector: size N times the
  element's, rounded to its alignment (16 for 16 bytes), the ABI `(vector ELEMENT N)` in the layout form; a newtype over it
  (`__m128i`, `Group`) takes its field's ABI (univariant's single non-ZST field). The helper's `(other)` gives neither the
  element nor the count, so this must be mono's (the record's layout cannot be the fallback; the helper could print it, the
  oracle tier's).
- **Codegen** (arbace.rust.back.codegen): a layout kind `:vector` as LLVM's `<N x iK>` immediate (loads, stores, phis and
  calls by value as rustc_codegen_llvm passes it: `<16 x i8>` in a register at -O0); the six intrinsics as
  rustc_codegen_llvm's generic_simd_intrinsic emits them: `simd_splat` an insertelement and a zero shufflevector, the
  comparisons `icmp` to `<16 x i1>` then `sext` to the element type, `simd_or` an `or`, `simd_bitmask` an `icmp slt` against
  zero to `<16 x i1>` bitcast to `i16`; transmutes between the three types a vector-to-vector `bitcast`.
- **rsi's lowering** (arbace.cpp.lower) already lowers vector types to a C struct of one array and the operations
  (`icmp`/`fcmp` over vectors, `extractelement`, `insertelement`, constant-mask `shufflevector`, bit-packed `<N x i1>`) to
  loops, from the C++ and LLVM crops; ci is not touched, so `gate_key` does not move (RFP58 (a)'s premise holds).
- **Cost**: about 1.5 to 2 days (the layout and the codegen kind about one, the six intrinsics and the transmutes half a day,
  a check over the four programs' records' `Group` bodies against rustc's IR half a day). It unblocks the four programs only
  as far as their next cause (generics then has its const generic `Matrix<R, C>` instances, now instantiated).

### Left

- Promoted constants that need more of CTFE: a call inside a promoted body (`std::ptr::null::<u8>()`), a checked operator
  with its Assert (an implicit discriminant after an explicit one), an unsizing cast with its vtable inside a promoted
  (`&[&Dog, &Bird(1)]`).
- Upstream monomorphizations (rustc's share-generics at -O0: an instance std already exports is std's symbol, not
  instantiated in the program's crate): `assert_failed::<i32, i32>` (panic-assert-eq). Natively this needs std's exported generic instances (its crates' metadata, `exported_symbols`), which
  RFP57's native interfaces would carry.
- RFP58 (above), RFP50 (`closures`' reified closure), `casts`' layout variants, `tlsref`.

### The night's commands (none run here)

- RFP46 (a)'s whole plan again, now with RF15: `RF11_BATCH=6 RF11_PROCS=4 bin/rust-front rsi-build` (samples and synthetic,
  about 30-60 minutes, 2.4 GB a process; the samples' processes above that), then `bin/rust-front rsi-native` by hand over the
  own programs (rustc as the linker driver) to confirm the 8 new own programs (algorithms, drops, errors, float-f32, float-f64,
  main-err, panic-explicit, strings) run as rustc's recorded runs, then `bin/rsi check --front own --corpus samples,synthetic`
  (RFP42 (a)).

## As built: RF16 (RFP58 (a)): the vector ABI; the HashMap programs' next causes; promoted constants further (2026-11-10)

Branch `rust-rf16` (from `mac/rust-rf15` `f9cabe9be`), light lane (one process at a time, `GOMEMLIMIT=2000MiB`, `GOMAXPROCS=2`,
`RF11_BATCH=3`). RF15's caches were copied into the worktree and their absolute paths rewritten (`rust-rf15/` to `rust-rf16/`)
before any run. Only `arbace/rust/` changes (in `arbace_crop_paths`): ci, `arbace/cpp/lower.ae` and `gate_key` do not move.

### RFP58 (a): the vector ABI

- **The definitions std's interface lacks** (`arbace.rust.front.mono/src-adt`): a private std ADT a public one reaches
  (`core::core_arch::simd::Simd`, `core::core_arch::x86::__m128i`, hashbrown's `Group`) read from the library's own sources
  (`RF_MONO_LIB`, rust-src 1.99.1's `library/`) as `ext-attrs` finds an item: the module tree from the crate's root (core,
  alloc, std, or a dependency in `library/vendor`), a module a macro declares (hashbrown's `cfg_if!`) by rustc's default file;
  a struct item with its `#[repr(simd)]`, its generic parameters and its fields' types (a parameter, a primitive, an array
  `[T; N]`, a path through the module's `use` items, their cfg evaluated against core's build, resolved by std's names), else
  stdarch's `types!` (core_arch/src/macros.rs: `pub struct NAME(LEN x ELEM);` expands to `#[repr(simd)] pub struct
  NAME([ELEM; LEN])`). Such an ADT's layout is now mono's where it was the record's: the 4 programs' layouts equal to the
  record's 16,055 -> 16,336 (collections 6,916 -> 7,043 of 7,539).
- **Layout** (`mono/simd-layout`, after rustc_ty_utils' layout_of_uncached `def.repr().simd()` and rustc_abi's
  `LayoutCalculator::simd_type`, 1.99.1): the element and count from the one array field, the element a scalar, the size N times
  the element's aligned as LLVM aligns a vector of that size (x86_64's data layout: 8 and 16 bytes; else the next power of two),
  `BackendRepr::SimdVector`, one field at 0, the element's niche; univariant's newtype rule takes a vector field's ABI as a
  scalar's (`Group`). The layout form writes it `(vector ELEMENT N)` (the helper prints `(other)`): `__m128i` and `Group`
  `(vector (int 64 true ..) 2)`, `Simd<i8, 16>` `(vector (int 8 true ..) 16)`. A record's const argument (`(cst "16")`) is read
  as `[:cval "16"]` (form->ty), so `Simd<i8, 16>`'s layout and its instances resolve.
- **Codegen** (`arbace.rust.back.codegen`): a vector layout is `:memory` with its `:vector` (element, count): rustc_target's
  `adjust_for_rust_abi` makes a SimdVector argument and return indirect (`make_indirect`), which is codegen's rule for a memory
  aggregate larger than a pointer: the same signatures as rustc's (`ptr align 16 %a`, `ptr sret([16 x i8]) align 16`, checked
  against the IR pack's `_mm_cmpeq_epi8`, `_mm_movemask_epi8`, `Group::match_full`). The generic SIMD intrinsics as
  rustc_codegen_llvm's `generic_simd_intrinsic` emits them, each vector operand loaded from its memory and the result stored:
  `simd_splat` (insertelement into poison, a zero-mask shufflevector), `simd_eq/ne/lt/le/gt/ge` (icmp, signed or not by the
  element, or fcmp's ordered predicates, then sext of `<N x i1>`), `simd_or/and/xor/add/sub`, `simd_bitmask` (lshr by the
  element's width less one, trunc to `<N x i1>`, bitcast to `iN`, then zext or trunc to the result). A transmute between two
  memory layouts of one size (`__m128i` to `Simd<i8, 16>`) is the same bytes. rsi's lowering (`arbace.cpp.lower`, LL5's
  scalarized vectors) takes them unchanged.

### The HashMap programs' next causes (found by the vector ABI, built)

- **A call through a constant function pointer** (core's quicksort passes `partition_lomuto_branchless_cyclic` as
  `(ptr allocN 0 8)` of a fn allocation, no callee fact): called through the operand's value.
- **A spread argument's memory field** (a `"rust-call"` closure shim's tuple of one non-zero-sized memory field): by pointer or
  as the integer of its size, as the caller passes the tuple (a tuple of several memory fields stays refused).
- **Thread-local statics** (`(tlsref MUT PATH)`: std's `RandomState::new`'s `thread_local!` KEYS, in every HashMap program):
  codegen_rvalue_operand's ThreadLocalRef, `llvm.threadlocal.address.p0` of the static declared `external thread_local` as
  rustc's codegen unit declares another crate's; its symbol mono's v0 of the static's def path (the body's DefId, then the
  name; `std-static-symbol`), equal in shape to rustc's (`_RNvNCNKNvNvMNtNtCs.._3std4hash6randomNtBa_11RandomState3new4KEYS0s_0
  23___RUST_STD_INTERNAL_VAL`); an item nested in a function body that std's names do not list, named in upper case, is in the
  value namespace (rustc's non_upper_case_globals: thread_local!'s `const KEYS`). build-run counts it as the fact `tlsrefs`.

### Promoted constants further (RF15's list)

- fmt: a call in a promoted body, `std::ptr::null::<u8>()` (`without_provenance(0)`: 8 zero bytes); `mem::size_of::<T>()` and
  `align_of` by T's layout (mono's).
- Checked operators (`AddWithOverflow`, `SubWithOverflow`, `MulWithOverflow`: `(T, bool)` with its fields kept for `_2.0` and
  `_2.1`) and their `assert` terminator (structs' `Color::Blue as i32 + 1`, an implicit discriminant after an explicit one).
- traits' `&[&Dog, &Bird(1)]` as `&[&dyn Animal]`: an unsizing cast in a promoted body, the pointer then its vtable's allocation;
  PromoteTemps moves the cast out of the body mono saw, so mono's vtables now carry each cast's source type and the promoted
  cast takes the body's vtable of its target and source (the body's own casts too, by source first). A nested borrow
  (`&_5` in a promoted body) is now kept in its local (RF15 built the value and dropped it: "a reborrow of a local not assigned").

### Results

- **The whole synthetic corpus** (`bin/rust-front rsi-build` over its 67 programs, `RF11_BATCH=3`, two threads, under 2 GiB a
  process, the toolchains stubbed, 0 stub calls; 48 minutes): **61 of 67 own** (RF15 56): collections and rc-refcell (the
  vector ABI, the constant function pointer, the spread argument, the thread-local static), panic-hashmap-index (the
  thread-local static: RF15's `tlsref`), fmt (the promoted `ptr::null` call) and traits (the promoted unsizing cast and its
  vtable). The native facts (build-run's, summed): callees 4,275 of 4,739 (unchanged), vtables 29 of 36 -> 35 of 46 (the
  promoted casts' and traits' now-built `main`'s), layouts 42,388 -> 42,788 of 45,856, constants 179 -> 191 of 197, thread-local
  statics 5 of 5; mono-check's against the record (`mono-all.log`, summed): layouts 41,946 -> 42,334 of 45,483, every other fact
  unchanged (callees 1,865 of 2,414, fn-symbols 1,789 of 2,204, core items 1,870 of 1,870). The SIMD types' layouts count as
  differing from the record's `(other)`.
- The 6 left: generics (`pair_up::<&str, i32>::{closure#0}`, a generic function's closure reached through std's `Map`, its v0
  type refused: "v0 of the type [:closure ...]" with parameters, and `<i32 as Zero>::ZERO`, a local trait's associated constant
  in a promoted body), iterators (`main` is the record's: "a method call without its callee", `v.iter().unzip_check()` through a
  blanket impl of a local trait for std's `Iter`), structs (local constants: `const LIMIT`, unevaluated in our MIR, in a promoted
  body and in `main`), panic-assert-eq (upstream monomorphizations, RF15's), casts (a layout's variants codegen does not take),
  closures (RFP50's reified closure).
- `bin/rust-front essential` equals its known file (4 min 22 s; no line moved: the essential programs reach no SIMD type, no
  thread-local static and none of the new promoted forms); `bin/rust-front core-essential` equals its own (564 of 567, 4 min 55 s);
  `bin/reap` 41 of 44 cells checked, 0 problems; `bin/crops-score --judge --strict` exit 0, Rust 75.0 / 75.0 / 95.8 / 80.0,
  unchanged (RFP43 (a): an own case still scores 0.5 until S6's switch).

### Left (the next causes, cheapest first)

- structs: local constants evaluated (a `const` item's body built and evaluated as a promoted body is, `eval-promoted`'s
  interpreter; its uses in `main` then constants), the cheapest: the interpreter exists.
- generics: a generic closure's type in v0 at its instance's arguments (the closure's parent's arguments substituted), and a
  local trait's associated constant (`<i32 as Zero>::ZERO`, the impl's constant's body evaluated, with structs' part).
- iterators: a method call through a blanket impl of a local trait (`impl<I: Iterator<Item = &i32>> UnzipCheck for I`) in the
  builder (typeck's method callee), then its instance at `Iter<i32>`.
- casts: codegen of a layout's variants; closures: RFP50; panic-assert-eq: upstream monomorphizations (RFP57's interfaces).

### The night's commands (none run here)

- RFP46 (a)'s whole plan again, now with RF16: `RF11_BATCH=6 RF11_PROCS=4 bin/rust-front rsi-build` (samples and synthetic,
  about 30-60 minutes, 2.4 GB a process; the samples' processes above that), then `bin/rust-front rsi-native` by hand over the
  own programs (rustc as the linker driver) to confirm the new own programs (collections, fmt, panic-hashmap-index, rc-refcell, traits) run as rustc's recorded runs:
  the vector code through rsi's lowering and the thread-local static against std's lowered library, then `bin/rsi check
  --front own --corpus samples,synthetic` (RFP42 (a)).

## As built: RF17: RF16's causes, four programs own and closures' first part (2026-11-10)

Branch `rust-rf17` (from `mac/rust-rf16` `ee2eca54d`), light lane (one process at a time, `GOMEMLIMIT=2000MiB`, `GOMAXPROCS=2`,
`RF11_BATCH=3`, the toolchains stubbed). RF16's caches were copied into the worktree and their absolute paths rewritten
(`rust-rf16/` to `rust-rf17/`) before any run. Only `arbace/rust/` changes (in `arbace_crop_paths`): ci, `arbace/cpp/lower.ae` and
`gate_key` do not move. RF16's causes were taken cheapest first, as its "Left" lists them; every rule follows rustc 1.99.1's
own code (the pinned rust-src's `compiler/`).

### structs: local constants

- **A local constant evaluated** (`build-run`'s `local-const`, codegen's eval_mir_constant of an unevaluated `const` item): the
  item's body as construct_const builds it (build-check's bodies already hold it) interpreted by `eval-promoted` in a value mode
  (`_0` holds the value, not a borrow; its fake reads and user type ascriptions skipped, its `false-edge`s followed), a scalar's
  bytes the operand's `(scalar BITS SIZE)`; in a runtime body and inside a promoted body (`LIMIT` in both); each constant once.
- **A variant's explicit discriminant in a runtime body** (`Color::Green as u8`: THIR's cast of a unit variant, its anonymous
  constant `Color::Green::{constant#0}`), mono's `variant-discriminant`, as RF15 evaluated it in promoted bodies.
- **A static holding a pointer** (`static GREETING: &str = "hi"`): its initializer's relocations read back from our allocation's
  text (`╾allocN<imm>╼`, `alloc-bytes+relocs`), each pointee its own allocation; codegen defines such a static as
  codegen_static's const_alloc_to_llvm does (a packed struct, the bytes between the relocations and each pointer the allocation
  it points at).

### generics: an associated constant, a generic function's closure

- **`<i32 as Zero>::ZERO`** (a local trait's associated constant, in a promoted body): the builder keeps the constant's
  definition and arguments (`:nc`); `mono/resolve-const` resolves it as resolve_associated_item does (typeck's
  `resolve-assoc-fn`, which now takes a trait's associated constant to the impl's constant of its name), and the impl's
  constant's body is evaluated as above.
- **A generic function's closure at its instance's arguments**: mono's instances substitute a closure type's parent arguments
  (`closed-closures`: `[:closure TEXT SIG ARGS]`, ClosureArgs' parent_args; the signature substituted too), and a record's
  closure form keeps them (its closure arguments less the three synthetic ones); v0 prints such a closure's path at them
  (`pair_up::<&str, i32>::{closure#0}`), and Instance::resolve's Fn-trait arm resolves to the closure's body at them, so the
  collector reaches `pair_up::<&str, i32>::{closure#0}` through std's `Map`.

### iterators: a closure's type in unification and in v0, constructors' shims

- **Two closure types of one closure unify** (typeck): a closure type is its definition at its parent's arguments; its
  signature's variables are equated, not compared by `=` (which saw two ids of one variable and failed FlatMap's
  `F::Output == U`, so `.flat_map(..).collect()` had no method: RF16's "a method call without its callee", which was this call,
  not the blanket impl's `unzip_check`).
- **v0 of a closure type is not a backref to its own closures' parent path**: a closure type's key is its definition at its whole
  closure arguments, the parent path of its own closures is printed at their parent arguments (generics_of takes a closure's
  parent from the typeck root), so `FlatMap<.., Map<.., {closure#0}>, {closure}>` prints the outer closure in full, as rustc's
  symbol does.
- **Constructors' shims** (`mono/ctor-shim-body`): a std ADT's tuple constructor used as a function value and instantiated in the
  program's crate (`ControlFlow::<i32>::Break`, `Result::<(), NonZero<usize>>::Err`: the record's `mir/external`, rustc's
  build_ctor shim whose MIR the helper does not dump) is built as shim.rs' build_ctor builds it (`_0 = ADT::V(move _1, ..);
  return`), its path the item's def_path_str less its generic arguments, its arguments the function constant's; the fact
  `ctor-shims`.

### casts: an uninhabited type's variants

- Codegen's `Variants::Empty` (`ControlFlow<NeverShortCircuitResidual, Infallible>`, array `map`'s `try_map`): an aggregate of it
  is a zero-sized operand, nothing stored; its discriminant codegen_get_discr's `poison`.

### closures: RFP50 (a)'s first part, then RFP62

- A closure reified as a function pointer (`fnptr(|x| x - 1, 4)`, `[("add", |a, b| a + b), ..]`: ClosureFnPointer) carries its
  ClosureOnceShim's symbol (`mono/closure-once-symbol`: Instance::resolve_closure at FnOnce, `<{closure} as FnOnce<(ARGS,)>>::
  call_once` at the program's crate), the shim's body the record's (core's `FnOnce::call_once`); the fact `closure-shims`.
  codegen then takes `closures`; its next causes are four closures the collector does not reach: three returned as
  `impl Fn` (`make_adder`'s, `make_counter`'s, `compose`'s: mono has no opaque types) and one in a `Box<dyn Fn>` vtable
  (`main::{closure#12}`: the vtable is the record's). RFP62 below.

### Results

- **The whole synthetic corpus** (`bin/rust-front rsi-build` over its 67 programs, `RF11_BATCH=3`, two threads, under 2 GiB a
  process, the toolchains stubbed, 0 stub calls; 44 minutes): **65 of 67 own** (RF16 61): structs, generics, iterators, casts.
  The native facts (build-run's, summed): callees 4,275 of 4,739 -> 4,516 of 4,978, constants 191 of 197 -> 205 of 205,
  function constants' symbols 1,798 of 2,215 -> 1,925 of 2,340, layouts 42,788 -> 42,792 of 45,860, symbols 258 -> 266 (all
  native), constructors' shims 2 of 2, closure shims 3 of 3, vtables 35 of 46 (unchanged); mono-check's against the record
  (`mono-all.log`, summed): callees 1,865 -> 1,992 of 2,414, function constants' symbols 1,789 of 2,204 -> 1,916 of 2,329, core
  items 1,870 -> 2,022 (all equal), items 464 of 1,119 -> 468 of 1,124, symbols 228 -> 230 of 272, layouts unchanged
  (42,334 of 45,483).
- The 2 left: closures (four closures the collector does not reach: RFP62) and panic-assert-eq (`assert_failed::<i32, i32>`,
  std's upstream instance: RFP63).
- `bin/rust-front essential` equals its known file (3 min 51 s; no line moved); `bin/rust-front core-essential` equals its own
  (564 of 567, 4 min 51 s); `bin/reap` 41 of 44 cells checked, 0 problems; `bin/crops-score --judge --strict` exit 0, Rust 75.0 /
  75.0 / 95.8 / 80.0, unchanged (RFP43 (a): an own case still scores 0.5 until S6's switch).

### RFP62 Closures behind opaque types and in vtables (`closures`)

`closures`' last four closures are reached only through an `impl Fn` return (`make_adder`, `make_counter`, `compose`: the call
`add5(1)` is `<opaque as Fn<(i32,)>>::call`) and through a `Box<dyn Fn() -> usize>`'s vtable (a closure's `call`, `call_mut` and
`call_once` entries: the last a VTableShim of its ClosureOnceShim). Mono has no opaque types: their identity (RFP33 (a)) is
typeck's, but neither their hidden types (the defining body's typeck results) nor `RevealAll`'s normalization in mono's
instances, layouts and symbols; and `vtable-alloc` refuses shims. Options: (a) **reveal opaque types in mono (type_of of an
opaque: its defining body's hidden type, substituted at the opaque's arguments; every type mono instantiates normalized under
RevealAll before layouts, resolution and v0) and build closures' vtable entries natively (the body's own `call`/`call_mut`,
`call_once` as `VTableShim(ClosureOnceShim)`'s symbol with the record's shim body, as RF17 took the reified pointer's)**, about two
days (opaque types reach layouts, symbols and resolution at once; a check over the samples' `impl Trait` programs too); (b) take
the record's items for closures reached only through opaque types and vtables (a record fact per such closure). *Recommended:
(a)*: `impl Trait` returns are common in the samples, and the vtable part is small once the shim symbols are mono's.

### RFP63 Upstream monomorphizations (`panic-assert-eq`)

rustc's share-generics (on at -O0): an instance an upstream crate already exports is that crate's symbol, not instantiated in the
program (`core::panicking::assert_failed::<i32, i32>`, which std exports: the record's callee names std's copy). Mono instantiates
it in the program's crate, so the module declares a local instance it never defines. Natively this needs
`upstream_monomorphizations`: each upstream crate's exported generic instances (its metadata's `exported_symbols`). Options: (a)
**the helper's std interface dump also lists std's exported generic instances (def path and arguments, from
`tcx.exported_generic_symbols`/`upstream_monomorphizations`), recorded with the interface (the oracle tier's, as
`std-iface.edn` is), and mono resolves an instance to the upstream crate's symbol when listed**, about a day plus a re-record of
the interface pack at night; (b) read the exported symbols from the rlibs' metadata natively (RFP57's native interfaces), larger;
(c) keep the record's callee for such instances (a counted record fact). *Recommended: (a)*, (b) once RFP57 is built.

### The night's commands (none run here)

- RFP46 (a)'s whole plan again, now with RF17: `RF11_BATCH=6 RF11_PROCS=4 bin/rust-front rsi-build` (samples and synthetic), then
  `bin/rust-front rsi-native` by hand over the own programs (rustc as the linker driver) to confirm the new own programs (structs,
  generics, iterators, casts; with RF15's and RF16's) run as rustc's recorded runs (the native constructors' shims, the static
  with a pointer, the uninhabited `poison` discriminant), then `bin/rsi check --front own --corpus samples,synthetic` (RFP42 (a)).

## As built: RF18 (RFP62 (a)): opaque types revealed in mono, closures' layouts and vtable entries; RFP63 (a)'s reader (2026-11-11)

Branch `rust-rf18` (from `mac/rust-rf17` `50a17174b`), light lane (one process at a time, `GOMEMLIMIT=2000MiB`, `GOMAXPROCS=2`,
`RF11_BATCH=3`, the toolchains stubbed, each process watched under 2.5 GiB). RF17's caches were copied into the worktree and their
absolute paths rewritten (`rust-rf17/` to `rust-rf18/`) before any run. Only `arbace/rust/` changes (in `arbace_crop_paths`): ci,
`arbace/cpp/lower.ae` and `gate_key` do not move. Every rule follows rustc 1.99.1's own code (the pinned rust-src's `compiler/`).

### RFP62 (a): opaque types in mono

- **Mono walks runtime MIR's types** (`program-facts`): each body passes PostAnalysisNormalize first (`passes/post-analysis-normalize`,
  RFP33 (a)'s reveal: an opaque type's hidden type, the defining body's typeck type at the use's arguments), as rustc's collector
  reads the bodies after the analysis passes. Mono read the built bodies, so `add5(1)` was `<impl Fn(i32) -> i32 as
  Fn<(i32,)>>::call`: no callee fact (its text was not the runtime body's), and the closure behind it never reached. Now the call
  is `<{closure@..} as Fn<(i32,)>>::call`, resolved by RF14's Fn arm to `make_adder::{closure#0}`, collected (likewise
  `make_counter`'s FnMut closure).
- **A generic definer's closure in its hidden type** (`passes/close-definer-closures`): `compose`'s hidden type is its own closure,
  whose ClosureArgs carry the definer's arguments; the hidden type's closures defined inside the definer get its identity
  arguments (`[:closure TEXT SIG ARGS]`, RF17's closed form) before the use's substitution, so `f(bb(41))` resolves to
  `compose::<i32, i32, String, {closure}, {closure}>::{closure#0}` and the collector instantiates it (RF15's generic instances).

### RFP62 (a): closures' layouts and vtable entries

- **A closure's layout** (`mono/layout-of*`'s `:closure`, layout_of_uncached's Closure arm): univariant over its upvars' types,
  `StructKind::AlwaysSized`; the upvars from the closure's built body (`build-body` now leaves `:upvar-tys`, closure_analyze's
  captures, the closure type's fields), a generic function's closure's substituted at its parent's arguments. The same map binds
  `build/*closure-tys*` in mono, so a closure's drop glue is its upvars' (it was none in mono).
- **The Fn traits' vtable entries** (`vtable-instance`, `vtable-alloc`): `dyn Fn(..)`'s supertraits `FnMut` and `FnOnce` take the
  object's arguments (`Fn<Args>: FnMut<Args>: FnOnce<Args>`); `call` and `call_mut` resolve to the closure's own body (its receiver
  the environment by reference, so no shim), `call_once` to the ClosureOnceShim, whose vtable entry is its VTableShim
  (Instance::resolve_for_vtable): `vtable-shim-mangle`, `::{shim:vtable#0}` over `<{closure} as FnOnce<(ARGS,)>>::call_once` then
  the instantiating crate, equal to the record's symbol; its body core's shim (the record's, as RF17's reified pointer's).
  `boxed`'s three vtables and `main::{closure#4}::{closure#0}`'s are now mono's (vtables 0 -> 4 of 4 in `closures`).
- **The record's `dyn Fn(..) -> R`** (`form->ty`'s `dyn`, `dyn-fn-sugar`): a std body's argument type `(ty "dyn std::ops::Fn() ->
  usize" (dyn))` is read from its text (the trait at the arguments' tuple, `FnOnce::Output`'s projection), so the std call
  `<{closure} as FnMut<(&Box<dyn Fn() -> usize>,)>>::call_mut` resolves and `main::{closure#12}` (`|f| f()`, reached only through
  std's `Map`) is collected.

### RFP63 (a): the reader side (std's exported generic instances)

- `mono/*upstream*` (`load-upstream`, read from `RF_STD_EXPORTS` by build-run's `native-run`): `{[DEF ARGS] SYMBOL}` of the
  helper's future `--std-exports` dump; an instance it lists is the upstream crate's (Instance::upstream_monomorphization): its
  symbol the listed one (`instance-symbol`), its callee fact `:external`, and it is no `ext-item` (never instantiated in the
  program's crate, linked from std's lowered library). Without the file nothing changes (the default until the night records it).
- A reader check with a hand-made one-entry list taken from `panic-assert-eq`'s record (`test/rust/front/std-exports-sample.edn`:
  `core::panicking::assert_failed::<i32, i32>`, std's symbol as the record's callee names it): the program's module then declares
  std's instance (`assert_failed::<i32, i32>`'s std symbol, external) and the program is own (`RF_STD_EXPORTS=test/rust/front/std-exports-sample.edn .tmp/rb.sh t-pae panic-assert-eq`, 1.7 GB). It proves the reader, not the fact: the fact is the night's dump, and `panic-assert-eq` counts own
  only once the recorded list carries it.

### Results

- **The whole synthetic corpus** (`bin/rust-front rsi-build` over its 67 programs, `RF11_BATCH=3`, two threads, the toolchains
  stubbed, 0 stub calls; 41 minutes): **66 of 67 own** (RF17 65): closures. The batch of closures, collections and drops was
  stopped by the day's process watcher (2.66 GB, over 2.5 GiB) after closures; collections and drops were then built alone,
  both own (collections 2.62 GB alone, drops 1.8 GB). RF17's code peaks the same way on collections alone (killed at 2.71 GB):
  not RF18's; RF17's "under 2 GiB a process" did not hold for collections, the light lane's line needs collections in a batch of
  its own or `RF11_GOMEMLIMIT` lower. The native facts (build-run's, summed): callees 4,516 of 4,978 -> 4,528 of 4,985, function
  constants' symbols 1,925 of 2,340 -> 1,933 of 2,346, layouts 42,792 of 45,860 -> 43,727 of 45,881 (closures' layouts now
  mono's), vtables 35 of 46 -> 42 of 46, symbols 266 -> 272 (all native), constants 205 of 205, closure shims 3 of 3;
  mono-check's against the record (summed): callees 1,992 -> 2,000 of 2,414, function constants' symbols 1,916 of 2,329 -> 1,924
  of 2,338, items 468 of 1,124 -> 478 of 1,125, symbols 230 -> 235 of 272, layouts 42,334 -> 43,265 of 45,483, core items
  2,022 -> 2,027 (all equal).
- The 1 left: panic-assert-eq (RFP63: std's upstream instance `assert_failed::<i32, i32>`; its reader built, the list the night's).
  closures' other refusals, counted record facts: `<Box<dyn Fn(i32) -> i32> as Fn<(i32,)>>::call` (Box's Fn impl needs the trait
  object's own Fn candidate in the impl's where clause: typeck's, refused as "a builtin candidate") and `<i32 as Ord>::cmp`
  (a macro-made item's attributes).
- `bin/rust-front essential` equals its known file (3 min 49 s; no line moved); `bin/rust-front core-essential` equals its own
  (564 of 567, 4 min 27 s; 2.0 GB); `bin/reap` 41 of 44 cells checked, 0 problems; `bin/crops-score --judge --strict` exit 0, Rust
  75.0 / 75.0 / 95.8 / 80.0, unchanged (RFP43 (a): an own case still scores 0.5 until S6's switch).
- build-run's `RF_CALLEE_TRACE=1` writes each callee mono refused, with its text, into the program's build-run.log (`debug
  no-callee`), as found useful here.

### Left

- RFP63 (a)'s night (below), then panic-assert-eq: 67 of 67.
- The object candidate in typeck's selection (`dyn Fn: Fn` in `Box<F>: Fn`'s where clause), so `Box<dyn Fn>` calls resolve to
  alloc's impl.
- The collections program's memory (2.6 GB in a process of its own): what the batch keeps of its layouts and core bodies (RES53's
  kind of question for the front end's builder).

### The night's commands (none run here)

- **RFP63 (a)'s dump and the packs again** (the oracle tier, the host with rustc; about an hour and a half, a helper run under its
  4 GB `ulimit -v`, a re-record night: the helper's sources are in every rust-front pack's key, `rfr_helper_hash`, so changing it
  makes each pack STALE until recorded again; outside `arbace/rust/`, so not on this branch):
  1. tools/rsdump: `--std-exports FILE`, after rustc_middle's `upstream_monomorphizations` (the instances the crate being
     compiled would link from an upstream crate: `tcx.upstream_monomorphizations(())`, each DefId's args map to the exporting
     crate), filtered to std, core and alloc's definitions, each written `{:def DEF-PATH :args [TYPE-FORM ...] :symbol
     tcx.symbol_name(instance) :crate CRATE-NAME}` sorted, the arguments as the records write types (`sty`'s `(ty TEXT STRUCT)`).
  2. bin/lib/repr-rust/front-records.bash: the kind rsi gains the member `std-exports.edn`, made on an empty program with rsi's
     plan's flags and sysroot, `"$helper" rustc --std-exports std-exports.edn -- $(rfr_rsi_flags test/rust/synthetic/panic-assert-eq.rs)
     --sysroot "$(rf11_rsi_sysroot)" -o empty.bin empty.rs` (rsi's std build: `bin/rsi std-build` at test/rust/runtime/ir.sha256 first,
     in the checkout that records; the main checkout's `.tmp/rsi/std` is an older 103-unit build); bin/rust-front's rsi-build exports
     `RF_STD_EXPORTS=$rs/std-exports.edn`.
  3. `bin/repr-rust helper`, then `bin/rust-front records make std built captures mono rsi` and `bin/rust-front core-records make`,
     each pack's verify (`bin/oracle-records`), then `bin/rust-front rsi-build` over the 67 (expected 67 of 67 own).
- RFP46 (a)'s whole plan again with RF18: `RF11_BATCH=6 RF11_PROCS=4 bin/rust-front rsi-build` (samples and synthetic), then
  `bin/rust-front rsi-native` by hand over the own programs (rustc as the linker driver) to confirm closures runs as rustc's
  recorded run (the closures' vtables and the opaque calls), then `bin/rsi check --front own --corpus samples,synthetic` (RFP42 (a)).

## As built: RF19: the trait object's Fn candidate, collections under 2 GiB, core's macro-made functions' attributes (2026-11-11)

Branch `rust-rf19` (from `mac/next` `c572f6753`, which carries rust-rf18), light lane (`GOMEMLIMIT=2000MiB`, `GOMAXPROCS=2`,
`RF11_BATCH=3`, the toolchains stubbed, every process watched and killed by PID above 2.5 GiB). RF18's caches were copied into
the worktree and their absolute paths rewritten (`rust-rf18/` to `rust-rf19/`) before any run. Only `arbace/rust/`,
`bin/rust-front`, `bin/lib/repr-rust/` and `test/rust/front/` change (crop paths): ci, `arbace/cpp/lower.ae` and `gate_key` do not
move. Every rule follows rustc 1.99.1's own code (the pinned rust-src's `compiler/`).

### `<Box<dyn Fn(..)> as Fn<..>>::call`: the builtin Tuple candidate

RF18 named the object candidate as missing; it was not: `dyn-implements?` already answers `dyn Fn(i32) -> i32: Fn<(i32,)>`. What
failed was alloc's impl's other clause, `impl<Args: Tuple, F: Fn<Args> + ?Sized, A: Allocator> Fn<Args> for Box<F, A>`: `Args:
Tuple` had no candidate (typeck's `holds?` looked for an impl of the lang item `tuple_trait`, which has none: it is builtin).
Now a tuple type implements `Tuple` (select's assemble_candidate_for_tuple; a variable or a parameter as before, any other type
none), so the call resolves to alloc's `{impl#33}::call` (closures: callees 106 -> 108 of 109, function constants' symbols 65 -> 67
of 68; mono-check's callees 68 -> 70 of 71 and core items 57 -> 59, all equal to the record's).

### collections' memory: 2.62 GB -> 2.09 GB alone, the whole corpus 2.24 GB

Measured with `RF_MEM_TRACE=1` (new: build-run's live heap after a collection at each phase, `debug mem PHASE N MiB at SECONDS` on
the standard error), collections alone (RF18: 2.62 GB, killed in its batch at 2.66 GB):

| phase | RF18's code | RF19 |
|---|---|---|
| the batch read the record (for mono-check's judge) | 894 MiB | 939 |
| native-run read it again | 1,371 | 939 (the same forms) |
| bodies built (std's interface and names, the program) | 1,649 | 1,457 |
| mono's facts | 2,096 | 1,724 |
| judged, the module's forms | 2,210 | 1,786 |
| native-run done | 2,207 | 1,877 |
| peak RSS | 2.59 GB | **2.09 GB** (1.99 GiB) |

Three costs, each fixable: (1) **the record read twice** (35 MB of text, 4,546 bodies: 477 MiB a copy): `native-run` now takes
the forms the batch read for the judge (RECORD the file or its forms); (2) **mono's cache of core's parsed files** (51 files, 355
MiB): `parse-rs` keeps a file's items without what ext-attrs and src-adt never read, the functions' bodies, the macro
definitions' token trees and the doc comments (8 large files: 322 -> 91 MiB); (3) **the module printed into one string** before
it was written (`with-out-str`, the run's last peak): `write-forms` prints it form by form into the file. Not taken: interning the
record's strings (585,415 strings, 24,828 distinct) made the read larger (1,233 MiB: the reader's strings were already cheap and the
copy was not). The whole synthetic corpus in batches of 3 now peaks at 2.24 GB (closures, collections and drops in one process),
none killed; collections alone stays under 2 GiB by a small margin (RF11_BATCH=3 batches stay under 2.5 GiB).

### core's macro-made functions' attributes: `bin/rust-front core-attrs`

The callee facts mono refused most were core's items whose attributes core's sources do not show (`mono: X's attributes are not
in core's sources (a macro-made item)`: 449 of the refusals in RF18's run, 392 of them `core::num`'s `int_impl!`/`uint_impl!` methods,
the rest the operator impls' macros, cmp's, slice and str iterators'): the record's callee was taken for each. Now:

- **`bin/rust-front core-attrs`** (`bin/lib/repr-rust/core-build.bash`'s `core_attrs`; core-check's new `--attrs` mode): core-check's
  interface pass (expansion, resolution, lowering, collection; no body) over `test/rust/front/core-attrs.txt`'s module files (num,
  ops::arith, cmp, slice::iter, str::iter, slice::index, default, convert::num, any, fmt::num), every function's codegen attributes
  after expansion (`mono/local-attrs` of its HIR, as core-export's `:attrs`) by its def path with rustc's disambiguators
  (`def-path-string`) into `.tmp/rust-front/core/core-attrs.edn`, kept by a key (the list, the files, the front end's expansion,
  resolution, lowering, collection and core-check, std's interface) in `core-attrs.edn.key`; 4,569 functions, about 23 minutes
  (num/mod.rs 18), 2.3 GB, one process.
- **mono** (`*macro-attrs*`, `RF_CORE_ATTRS`): an item `ext-attrs` does not find in core's sources takes the file's attributes
  (a found item keeps its sources', so the pass cannot change any fact that was native); `rsi-build` makes the file when it is not
  current (`RF_CORE_ATTRS=none`: not) and exports it.
- **core-check's macro table** (`macros-for`): of the definitions in files equally near the module's, the shallowest
  (`num/mod.rs`'s `#[macro_use] mod uint_macros;` exports its `uint_impl!`; `num/imp/int_bits.rs`'s private `uint_impl!`, one
  directory deeper, was taken by the tie and no arm matched: core::num did not expand).
- **`${concat(..)}`** (mbe's metavariable expression, transcribe.rs: the elements' identifiers, literals and metavariables'
  tokens joined into one identifier, marked with the expansion): fmt/num.rs' `${concat($fmt_fn, _small)}`.

### Results

- **The whole synthetic corpus** (`rsi-build` over its 67 programs, `RF11_BATCH=3`, two threads, the toolchains stubbed, 0 stub
  calls; 63 minutes with the attributes' pass current): **66 of 67 own** (unchanged; panic-assert-eq waits for RFP63's night). The
  native facts (build-run's, summed; RF18 -> RF19): **callees 4,528 -> 4,984 of 4,985**, **function constants' symbols 1,933 ->
  2,342 of 2,346**, layouts 43,727 of 45,881, vtables 42 of 46, constants 205 of 205, symbols 272 (all native); mono-check's against
  the record (summed): **callees 2,000 -> 2,408 of 2,414**, function constants' symbols 1,924 -> 2,333 of 2,338, core items 2,027 ->
  2,414 (all equal), items 478 of 1,125, symbols 235 of 272. The one callee left was fmt::num's `{impl#32}::fmt` (`${concat}`, built after the run;
  the attributes' pass again, 30 minutes beside the essential runs, 4,695 functions, fmt::num's 126 among them): fmt alone then has
  callees 160 of 160 and mono-check's 76 of 76, so the corpus' native callees are **4,985 of 4,985**, function constants' symbols
  2,343 of 2,346, mono-check's equal callees 2,409 of 2,414.
- `bin/rust-front essential` equals its known file (49 minutes this once: ci's musl forms read afresh in the new worktree;
  2.2 GB); `bin/rust-front core-essential` equals its own (5 min 33 s: the macro table's tie rule and `${concat}` move none of its
  modules); `bin/reap` 49 of 52 cells checked, 0 problems; `bin/crops-score --record` leaves `test/crops/scores.edn` as it was,
  `--judge --strict` exit 0, Rust 75.0 / 75.0 / 95.8 / 80.0, unchanged (RFP43 (a)).

### Left

- RFP63 (a)'s night (RF18's commands), then panic-assert-eq: 67 of 67.
- The six callees mono-check finds unequal to the record's (errors, fmt, iterators 2, panic-assert-eq, patterns) and the four
  vtables mono leaves to the record (errors 1, traits 2, fmt 1).
- RFP52's rest (core-check: `ChangeOutputType`, `CopySpec`, `drain::Drain`, `ascii::Char`, ...): not taken here.

### The night's commands (none run here)

- RF18's (RFP63 (a)'s dump and the packs again; RFP46 (a)'s whole plan with `rsi-native` and `rsi check --front own`), now with
  RF19: the plan's rsi-build makes `core-attrs.edn` first when not current (23 minutes, 2.3 GB, one process; the main checkout's
  `.tmp/rust-front/core/` keeps it by key).

## As built: review 1111's fixes to RF19, findings 3 and 4 (2026-11-11, branch `review-fix-1`, from mac/next a7916bb2a)

The independent review of 2026-11-11 (`.tmp/reviews/1111/REVIEW-hs-rust-ml-scm-llvm.md`). **Finding 3**: `core_attrs_key`
(`bin/lib/repr-rust/core-build.bash`) left out `arbace/rust/front/mono.ae`, whose `local-attrs` makes every entry's names, and repr/rust's
reader (`arbace/repr/rust{.ae,/}`); both are in the key now. Over rust-rf19's work directory the old function gives the key its
`core-attrs.edn.key` holds (092bd9ba5613...: taken as current), the new one 520d3557f5c0...: the next rsi-build makes the file again (23
minutes, 2.3 GB, one process). Left, as the review notes: the native-iface/index caches the pass reads from `$work/core` are not keyed by
their sources. **Finding 4**: `ext-attrs`' last fallback (RF13: the one function of that name the file's impls hold) could answer a
macro-made method (int_impl!'s inside `impl i32`) with a hand-written method of another impl, and the right `core-attrs.edn` entry was
never read. `attrs-of*` now asks, for a path the map knows, `ext-attrs` without that fallback (`*once-fallback*` false, cached apart),
then the map, then nothing; a path the map does not know is answered as before.
A comparison of both rules over every path of rust-rf19's `core-attrs.edn` (its core copy and std interface; stage 0, 18 minutes, 0.8
GB): 4,695 paths, 13 answered differently, all `core::cmp::impls::{impl#57}` to `{impl#81}::clamp`, ord_impl!'s methods (cmp.rs:2242-2244,
`#[inline] #[track_caller]`), which the fallback had answered with bool's hand-written `clamp` (cmp.rs:2301, `#[inline]`): the integers'
`clamp` had lost `#[track_caller]`.
`bin/rust-front essential` equals its known file (38 minutes, 1.0 GB), `bin/rust-front core-essential` its own (13 minutes, 2.0 GB).
Night command (not run): RF19's plan, whose rsi-build now makes `core-attrs.edn` again (23 minutes, 2.3 GB), then the synthetic corpus.

## As built: RF20: RFP52's rest, core's crate-private items in the bodies (2026-11-11)

Branch `rust-rf20` (from `mac/next` `4b8fc77e3`, which carries rust-rf19), light lane (`GOMEMLIMIT=2000MiB`, `GOMAXPROCS=2`, each
process watched and killed by PID above 2.5 GiB; none was). RF19's caches were copied into the worktree and their absolute paths
rewritten (`rust-rf19/` to `rust-rf20/`) before any run; the baseline core-check ran on an export of `mac/next` (`git archive` into
the worktree's `.tmp/base`, its own caches), so the sources under test were never edited under a run. Only `arbace/rust/front/`
and `test/rust/front/core-essential-known.txt` change (crop paths): ci, `arbace/cpp/lower.ae` and `gate_key` do not move
(`bin/arbace-go --gate-key` equal before and after, `test/gate/gate-key-crops-test.bash` passes). Every rule follows rustc 1.99.1's
own code (the pinned rust-src's `compiler/`).

### RFP52's rest

- **A qualified path to a crate-private trait in an out-of-line module** (`<T as self::uninit::CopySpec>::clone_one`, clone.rs):
  the written prefix `self::uninit::CopySpec` does not walk (uninit.rs is another file); the trait is taken from the item's own
  resolution, its parent (instantiate_value_path: tcx.parent of the resolved item), when the interface holds the trait.
- **The native interface's late-bound regions** (`ty->sty` over a function's signature, `sig-sty`): an anonymous late-bound region
  is written `'^#N` (its bound variable's index after the named ones) and a named one `'^'a`, as the helper's `--std-iface` writes
  them; before, both were `'_` (erased), so a crate-private function's type printed `unsafe fn(&T, ..)`, not `for<'a> unsafe
  fn(&'a T, ..)`. A trait's own functions are written `:has-self true`, as the helper writes them.
- **core's own kinds in collect's names** (`with-core-kinds`): collect's `ext-kind` read std's names alone, which lack crate-private
  items, so `clone::uninit::CopySpec::clone_one` was not known as a trait's function and its path printed without `<T as ..>`.
  Then **core's own modules and re-exports in collect's names** (`:mods`, the resolver's union of std's names and core's index):
  `ascii::Char` (ascii.rs' `pub use ascii_char::AsciiChar as Char`) now walks in collect as in the resolver.
- **Type aliases in the native interface** (`:aliases` with their generics, merged into collect's names per module,
  `with-native-std`): `pub(crate) type ChangeOutputType<T, V> = <T::Residual as Residual<V>>::TryType` expands at its arguments as
  a std alias does (`array::try_from_fn`'s signature).
- **An out-of-line module's item through a written path** (`walk-written`): `drain::Drain::new` from array/mod.rs (drain.rs another
  file): the module's interface path's child when core's names know its kind (`array::map`'s `try_map`).
- **The enclosing impl's trait is in scope for method calls** (rustc_resolve's traits_in_scope: `current_trait` first, when it may
  have the item): `"could not convert slice to array".fmt(f)` inside `impl fmt::Display for TryFromSliceError` names Display,
  which array/mod.rs does not import.
- **The constructors of core's structs the names lack** (`with-adt-names`): `casemappingiter_impls!` makes `ToLowercase(..)` and
  its siblings in char/mod.rs; core's index (the sources unexpanded) does not see them and std's names omit their private
  constructors, so `ToLowercase(CaseMappingIter::new(..))` did not resolve; from std's interface's ADTs (and the native ones), each
  core struct with a constructor gets its type and constructor in its module where missing.
- **A crate-private constant in a range pattern** (`..MAX_ONE_B => 1`, char/methods.rs' `len_utf8`): the native interface writes a
  constant's type and, for an integer one, its value from its expression of literals (`const MAX_ONE_B: u32 = 0x80`), and
  `pat-expr-const` takes it (a local constant's likewise); `eval-int-expr` now reads hexadecimal, octal and binary literals.
- **derive(Debug) of a struct whose fields are all cfg'd out** (`#[derive(Debug)] pub struct BorrowError { #[cfg(feature =
  "debug_refcell")] location: .. }`, cell.rs): `Formatter::write_str(f, "BorrowError")`, debug.rs' special case for no fields;
  the expansion called a `debug_struct_field0_finish` that does not exist.
- **Projection clauses in the native interface** (`clause->sty`: `(proj ITEM [ARGS] TERM)` as the helper writes them), written; no
  body gains by them yet (below).

### Results

- `bin/rust-front core-check --records` over the 37 modules (the interface pass afresh, 23 minutes of it; 48 to 56 minutes,
  2.16 GB peak), against the baseline run on `mac/next`'s export the same day (62 minutes, 2.22 GB): **5,576 -> 5,594 equal** to
  rustc's built MIR, 157 -> 161 but for the allocations, 37 -> 33 differ, **24 -> 6 refused**, 74 without rustc's body (the
  first run, before the derive change, 5,594 / 159 / 33 / 8; the derive change made cell's two `fmt` equal but for the
  allocations). No
  module lost an equal body; gains: char::methods +8 (59 -> 67: `ascii::Char` 2, the case mappings' constructors 4,
  `MAX_ONE_B`, `encode_utf8_raw_unchecked`), clone +2 (CopySpec), array +2 (`try_from_fn`, `try_map`; `fmt` but for the
  allocations), mem +2, any +1, non_null +1, result +1 and option +1 (`from_iter`'s closure, its signature through `try_process`'s alias), time one refusal
  fewer (now but for the allocations).
- `bin/rust-front core-essential`: clone 27 -> 29 of 29 equal, total 564 -> 566 equal and no refusal; its known file recorded
  again (`test/rust/front/core-essential-known.txt`, the run's summary) and the step equals it (6 min). `bin/rust-front
  essential` equals its known file unchanged (4 min 22 s; the derive change moves none of its lines). Both peaks 2.1 GB.
- `bin/reap` 49 of 52 cells checked, 0 problems; `bin/crops-score --record` leaves `test/crops/scores.edn` as it was,
  `--judge --strict` exit 0, Rust 75.0 / 75.0 / 95.8 / 80.0, unchanged (RFP43 (a), RFP53 (a): no case is own end to end yet).

### Left

- The 6 refusals left are not RFP52's: time's `Debug for Duration` (`fmt_decimal`'s
  `prefix` closure capture and its method call, 3), any's `trait_info_of_trait_type_id` (`type_id_vtable`'s callee), mem's
  maybe_uninit `write_default` (a call's type), ptr's `swap_nonoverlapping_short` (`swap_prefix!`'s `$n:literal` turbofish).
- `try_process`'s turbofish (result's and option's `from_iter`): its second argument stays an inference variable; the native
  interface now writes the function's projection clauses, but `I: Iterator<Item: Try<Output = T, Residual = R>>`'s associated
  item bounds (a bound on `<I as Iterator>::Item`, then its projections) are not among collect's clauses yet.
- `NeverShortCircuit::wrap_mut_1` (array's `from_fn` and `map`): its return type is an opaque type (`impl FnMut(..) +
  Destruct`), which neither interface writes with its bounds (std's `(opaque ..)` forms are read but never resolved): the next
  piece of RFP57's native interface.
- RF13e's others (tuple.rs' metavariable spans 12, braced const arguments 3, `array::clone`'s DefIndex, drop orders).

### The night's commands (none run here)

- RF18's and RF19's (RFP63 (a)'s dump and the packs again; RFP46 (a)'s whole plan with `rsi-native` and `rsi check --front own`).
  RF20 changes `core_check.ae`, `collect.ae` and the front end's expansion-side files that `core-attrs.edn.key` hashes
  (`collect.ae`, `core_check.ae`), so the plan's rsi-build makes `core-attrs.edn` again first (about 23 minutes, 2.3 GB, one
  process), then the 67 programs.

## RIND: eval and AOT on Arbace's own lowering, the design (2026-11-17)

Branch `h-rust-indep` (from `next` `e4a7a30f3`), light lane. The question: Rust's independence is 80 (`test/crops/independence.edn`:
read 1, eval 0.5, AOT 0.5, print 1, judge 1) because every case of the eval cell consumes rustc's records (RES54 (a), RFP43 (a)); a
case scores 1 only when its seven inputs C1-C7 (`test/crops/crops.edn`'s `:own-inputs :rust`, RFP53 (a)) are Arbace's own. The
Mac's task 322 takes the AOT cell's record and eval's width (the cell's cases beyond the five samples); this section takes the
inputs. It refreshes `.tmp/plans/RUST-90-PLAN.md` (RFP53-RFP61, decided (a) by the user 2026-11-10) at RF20's state.

### Where each input stands

How far the front end goes is no longer the question: the pipeline is complete for a program (the native reader, expansion,
resolution, HIR lowering, collection, typeck, THIR and MIR building, the ported passes, mono's facts, codegen to LLVM forms, rsi's
lowering): 66 of the 67 synthetic programs build every body of their own natively (RF19, `bin/rust-front rsi-build`), core-check
5,594 of the 37 modules' bodies equal to rustc's built MIR (RF20). What holds the stage at 0.5 is the library and its facts:

| input | today (the cell's five cases) | the work | cost |
|---|---|---|---|
| C1 the program's bodies | rustc's -O0 IR pack (the samples: 0 of 6 own at RF11's measure; synthetic 66 of 67) | the samples' causes; `--front own` the record's default (RFP42 (a) extended) | 3-5 days, one night |
| C2 std's instances' bodies | the pack `rust-front-rsi` (rustc's) | RFP55 (a): the library closure built natively, RFP54 (a) taken unconditionally | with C6 |
| C3 no mono fact from the record | refused facts from `rust-front-rsi` | RFP48/RFP49's rest, `try_process`'s bounds, opaque returns (RF20's left) | with C4 |
| C4 std's interface, StableCrateIds | the helper's `std-iface`, `std-crates-rsi.edn` (rustc's rlibs) | RFP57 (a) native interfaces; RIND6 for the ids | 5-7 days, one night |
| C5 the allocator shim | rustc's `shim.ll` | **built (RIND2 (a), below)** | done |
| C6 the library's non-generic items | std's IR from Cargo, rustc and clang | RFP55 (a) + the own lowered library (RUST-90 S5) | 12-16 days, 2-3 nights |
| C7 ci's C target | `test/c/host` (gcc's recorded macros) | RFP59 (a): runtime-ci and the program's read on `c/target` | 1 day, one night |

The score moves only when a case has all seven (RFP53 (a)): no stage before C2 and C6 changes the 80. The order below takes the
cheap inputs first so that each later stage is the only thing left between a case and 1.

### Proposed (RIND1-RIND7)

- **RIND1 The order.** (a) **By input, cheapest first, over every case at once: C5 (built), C7, C1 on the cell's cases, C3 with C4,
  then C2 with C6, then the switch and the records (RUST-90's S6)**; (b) one case end to end first (a program needs std, so its
  library closure is the bulk of the work either way, and the cheap inputs wait); (c) wait for Tier S (std whole from source,
  RES54's 35-60 days). *Recommended: (a).* Total about 25-34 agent days and 5-6 nights; the earliest 90 about four weeks out.
- **RIND2 The allocator shim (C5).** (a) **Arbace's own shim (`arbace.rust.back.shim`) in place of the pack's on every case of
  the default allocator's case, the packs left rustc's (`RSI_SHIM=pack` the old path); a program whose source names a global
  allocator, no_std, an allocator attribute, a crate name or an extern crate keeps the pack's (conservative, by its words)**; (b)
  only on `--front own`'s cases (C5 own only where C1 is, nothing gained); (c) keep `shim.ll`. *Recommended: (a); built below.*
- **RIND3 The shim's judge in the gate.** (a) **`bin/rsi shim-check` (seconds, under 1 GiB, no rustc: the forms and the lowered C
  of all 73 programs of the samples and synthetic packs against rustc's `shim.ll`, a crossed control that must differ) as a light
  step of Rust's crops tier beside `rust-front-essential`**; (b) by hand only. *Recommended: (a)*; the step's declaration is the
  main host's to add (this branch adds no step: no gate run here).
- **RIND4 ci's C target (C7).** (a) **RFP59 (a) as decided: runtime-ci's library read and each program's read on `c/target`
  (Arbace's own C target, as llvmi since batch 2), `test/c/host` the oracle tier's; the ci library's key takes `c/target` in place
  of `test/c/host` (one runtime-ci rebuild, about 4 h and 6 GiB, a night)**; (b) the program's units alone on `c/target`, std's
  library still read on gcc's macros (C7 not own). *Recommended: (a).* About 1 day, then the night; the next stage.
- **RIND5 C1 on the cell's cases.** (a) **The samples through `bin/rust-front rsi-build` (RFP46's night measure first: which of the
  six it refuses and why), their causes fixed, and `--front own` the record's default for samples and synthetic (a case the front
  end does not build runs on the pack and says so in `front.tsv`)**; (b) widen the cell to the synthetic corpus alone (task 322)
  and leave the samples on the pack. *Recommended: (a)*, with task 322's width: the own count is then the synthetic's 66 plus the
  samples fixed.
- **RIND6 The compiler's identity.** (a) **The emulated rustc's identity (CFG_VERSION, the data layout, the triple, target-cpu,
  llvm.ident's text) is a constant of Arbace's target, counted own, as `c/target` is C's; held equal to the rmeta's header only in a
  check (`shim-check` does so for CFG_VERSION). Once Arbace builds the library itself (C2, C6) the library's `-C metadata` values are
  constants of that build's description, and core's, alloc's and std's StableCrateIds are computed natively (`mono/stable-crate-id`),
  not read from rlibs (C4's ids)**; (b) read the identity from the rmeta (C4 stays rustc's until a byte-identical std, never).
  *Recommended: (a).*
- **RIND7 The library (C2, C6).** (a) **RFP55 (a) and RFP54 (a) as decided: the closure of library items the cell's programs and
  `lang_start` reach, built from source by the front end (generic instances and non-generic items), lowered by codegen, rsi's
  lowering and ci into an own lowered library kept by key (`.tmp/rsi/runtime-own`), grown with the cell**; (b) the whole std now.
  *Recommended: (a).* RFP56's census (half a day, light) first sizes it; 12-16 days with its nights.

### Stages and estimates

| stage | inputs | work | cost | independence |
|---|---|---|---|---|
| 1 | C5 | RIND2, RIND3 | 0.5 day (built) | 80 |
| 2 | C7 | RIND4 | 1 day, one night | 80 |
| 3 | C1 | RIND5 | 3-5 days, one night | 80 |
| 4 | C3, C4 | RFP57 (a), RF20's left, RIND6 | 5-7 days, one night | 80 |
| 5 | C2, C6 | RIND7 (RUST-90 S4-S5) | 12-16 days, 2-3 nights | 80 |
| 6 | the records | RUST-90 S6, RFP61 (a) | 3-4 days, two nights | 85 (eval), then 90-96 (AOT), by the own share of the cell |

## As built: RIND (stage 1): the allocator shim Arbace's own (2026-11-17)

- `arbace/rust/back/shim.ae` (`arbace.rust.back.shim`): the default allocator's shim as LLVM forms, `__rust_alloc`,
  `__rust_dealloc`, `__rust_realloc`, `__rust_alloc_zeroed` tail-calling std's hidden `__rdl_` functions, then
  `__rust_no_alloc_shim_is_unstable_v2`; the names by mono's `mangle-internal`; the attribute groups (allockind, allocsize,
  alloc-family, probe-stack, target-cpu), the module flags of a PIE executable with unwind tables, llvm.ident, each definition's
  `!guid` (MD5 of its name). The module's source filename is the allocator CGU's mangled name: the human-readable
  `CRATE.ID-crate.allocator`, ID the program crate's StableCrateId in hex without leading zeros (`casts.d3164f7a67cfd47`: the
  first try's 16 digits missed three of the 73), hashed by StableHasher (the bytes, then 0xFF) into a Hash128 in base 36, 25 digits
  (none of the 73 shorter, so the padding's side is unseen). The identity constant CFG_VERSION is the shim's own (RIND6 (a)),
  checked equal to `test/rust/front/std-crates-rsi.edn`'s.
- `bin/rsi shim-check [CORPUS...]` (`make rsi-shim-check`): the samples' and synthetic packs extracted at their pins, each
  program's shim read by repr/llvm and compared as forms with ours, both lowered to C and compared, and a crossed control (the
  first program's shim against the second's record) that must differ. **73 of 73 equal (forms and lowered C), the control
  differing; 13 s, 0.9 GB.** A mutated record (`allocsize(3)` made `allocsize(2)`) fails it (exit 1).
- `bin/lib/rsi/check`: on the pack path (`RSI_IR=pack`, the default) and `RSI_SHIM=own` (the default), after the IR step each
  program of the default allocator's case gets our `shim.ll` (printed by repr/llvm: no comments, the same forms) in place of the
  pack's; `WORK/CORPUS/shims.tsv` and `files/KEY/shim.src` say which; the packs and `ir-pack` stay rustc's. The block was run
  alone over the synthetic list (67 of 67 own); the whole `rsi check` was not run here (`rsi-essential` declares 8 GiB): its runs
  are unchanged by construction, the lowering reading the same forms.
- `test/rust/eval-inputs-linux-amd64.edn`: C5 `:own` on the five cases. Independence unchanged at 80 (the other six inputs are
  rustc's; RFP53 (a)); `bin/crops-score --judge --strict` exit 0.
- Keys: `arbace/rust/` and `bin/lib/rsi/` are crop paths (`arbace_crop_paths`): no gate key moves; `bin/rsi` and the Makefile are
  in Rust's steps' inputs. The main host: the night's `rsi-check` (weekly) and `rsi-essential` run on the own shim; RIND3's step to
  add; stage 2's runtime-ci rebuild (RIND4) a night.

## As built: RIND3 (a), the shim's step (2026-11-17)

The user decided RIND1-RIND7 (a) on 2026-11-17 (round 51). `rsi-shim-essential` (`bin/rsi shim-check`, `:mem-gib 1`, the crops tier's
`rust-essential` chain after `rust-read-essential`, `:essential-when-changed :rust`; inputs `:lib-tools :tools :rust :c`, the lowered
C printed by repr/c) in `test/gate/steps.edn`, among the crop's steps, its control the check's own crossed pair (a waiver naming it:
a control that passes fails the check). `gate.crops check` 0 problems, `test/gate/crops-test.bash` ok, `test/gate/crop-tier-test.bash`
20 of 20, `bin/gate --plan`: the step runs (never passed), simulated at 0:00 (unmeasured). Its load closure for
`test/gate/loads.edn` is the main host's (`bin/gate --loads-update`) with its first audited run.

## As built: RIND4 (a), ci on Arbace's own C target (2026-11-17)

- `bin/rsi` sources `bin/lib/repr-c/sysroot.bash` and calls `c_target_host`: `ARBACE_C_HOST` is `c/target` by default (gcc's recorded
  `test/c/host` under `ARBACE_C_TARGET=gcc`, the oracle tier's, as for every C reader); `arbace.rust.run`'s `-main` loads that host
  (its own default `c/target`), so `read-lib`, `nodes-list`, `compile-list` and `nodes` read and check the lowered C on it, and
  runtime-ci's two keys (`ci/key`, `ci/ports/key`, also `bin/rsi runtime-keys`) hash it in place of `test/c/host`. musl's forms were
  already c/target's (`bin/lib/ci/musl`'s `c_target_host`); only the program's and the library's read moved. `bin/rust-front`'s ci
  session (`rust-front-essential`'s runs) passes `$ARBACE_C_HOST` (c_sysroot's) in place of `test/c/host` likewise.
- Proven light: ten lowered units (five synthetic programs' codegen units and shims: casts, int-ops-u8, panic-todo, closures,
  algorithms) read and checked by `arbace.rust.run/read-program` on both hosts: all `:ok`, the read forms, the errors and the standard
  flags equal (the checker's symbol table an identity map, not compared). `bin/rust-front essential` on c/target: run-built 7 of 7
  programs from Arbace's MIR give rustc's status and output through ci, the summary equal to its known file, every control differing
  (5 min 48 s, 1.77 GB). `bin/rsi runtime-keys`: lowered `3f1a0098...` unchanged (std's lowering is not redone), ci `f0730105...`
  (gcc's `b43f9a75...`), ports `dfa2da64...` (gcc's `c84a13ea...`).
- `test/rust/eval-inputs-linux-amd64.edn`: C7 `:own` on the five cases (C5 and C7 own, five inputs rustc's); independence 80.
- The main host's night: `bin/heavy-lock bin/rsi runtime-ci` (the lowered std at its key is kept: the ci library and the ports read
  again on c/target, compiler_builtins' units lowered and read; minutes to about an hour, `RSI_CI_MEMLIMIT` 6 GiB), then `bin/rsi
  aot-lib` (its units' keys moved), then the cell's runs as before (`bin/rsi check --corpus samples,synthetic --record`, `check --aot`).

## As built: RIND5 (a), the samples from Arbace's MIR (2026-11-17)

`bin/rust-front rsi-build` over rsi's six samples, one program a process (`RF11_BATCH=1 RF11_PROCS=2 RF11_GOMEMLIMIT=1700MiB`, the
peak 2.16-2.31 GB, every process watched), the core attributes made once (`core-attrs.edn`, 35 minutes, then again after `mono.ae`
moved: its key hashes it). Before: **0 of the 6** (RF11's measure: none built). The causes, in the order the runs met them, each
after rustc 1.99.1's own code:

- **intrinsics**: own at the first run (RF12-RF20's work).
- **panics**: `&dyn Any + Send` to `&dyn Any` (codegen's Unsize between trait objects of one principal, the auto traits dropped:
  rustc's unsize_ptr keeps the metadata; `dyn-text` drops `+ Send`, `+ Sync`, `+ Unpin`, `+ UnwindSafe`, `+ RefUnwindSafe`); the
  `caller_location` intrinsic (codegen_intrinsic_call's get_caller_location, the location stored to the destination); then the
  closures below.
- **unwind** and **panics**: closures passed to std's generic functions over trait objects (`r.err().and_then(|e|
  e.downcast_ref::<u32>().copied())`, `Option<Box<dyn Any + Send>>`): the collector's RFP49 roots resolve each std body's
  function constant through `std-call`, which cannot read `dyn Any + Send` in the arguments, so the closure was never collected
  ("a local instance not collected"). Now a Fn trait's call (`FnOnce::call_once`, `FnMut::call_mut`, `Fn::call`) whose self is a
  local closure (the record's `(closure "main::{closure#6}" ...)`) collects that closure's body when the resolution fails (a closure
  has no arguments of its own at the program's definitions).
- **std** (the sample is named `std.rs`: the crate `std`): two crate roots of one name. mono printed every std path's root with the
  local crate's disambiguator when the names matched (`HashMap::get`'s instance under `Cs69PB9vw0pp4_3std`, the program's), and
  build-run's check of instances not collected took the first `Cs..._3std` of the module, the real std's. The local root is now
  `[:crate NAME :local]` (def-key, the instantiating crate, `def-local?`, `print-crate-name!`), and both checks take the local
  crate's own token (its StableCrateId's disambiguator), the name's first match only when that token is absent.
- **vectors**: `std::simd::Simd<T, N>` (`#[repr(simd, packed)]`) had a memory layout of alignment 1: the interface writes no
  repr(simd) and `adt-repr` read the sources only for ADTs the interface lacks; a known ADT of a SIMD module is now read too
  (`Simd<i8, 16>`: size 16, align 16, a vector; a packed one of a non-power-of-two length is not handled yet). Then codegen's
  reductions (`simd_reduce_all`, `_any`, `_and`, `_or`, `_xor`, `_max`, `_min`, `_add_ordered` on integers: llvm.vector.reduce.OP,
  all and any over the lanes made `<N x i1>`) and casts (`simd_cast`, `simd_as`). Left: `simd_shuffle`, whose index is an inline
  constant of `Swizzle::concat_swizzle` (CTFE of a trait's associated constant at the impl's arguments), then `simd_mul`, `_div`,
  `_rem`, `_neg`, `_saturating_add` and `simd_select` (the sample's other intrinsics; not written: nothing reaches them yet).
- **threads** (not a case of the cell: threads are a class): `unsupported callee of kind other`, not looked at.

**After: 4 of the cell's 5 cases own** (intrinsics, panics, std, unwind: every body of the program's own from Arbace's MIR,
codegen and rsi's lowering accept the module; `front.tsv`'s `own`), vectors on the pack. Their runs on ci are not proven here: the
ci leg needs runtime-ci's library (a night's build), and the native leg (`rsi-native`) runs rustc as the linker (the oracle tier).
Regressions: `bin/rust-front essential` equals its known summary (run-built 7 of 7, mono-check unchanged, every control differing;
1.77 GB); `bin/rust-front core-essential`: 566 equal, the known summary, the control differing (2.18 GB). Three synthetic programs spot-checked
after the changes (closures, iterators, traits: own as before, 2.15 GB); the other 64 are the night's rsi-build.

`bin/lib/rsi/check`: `--record` may take `--front own`, and a `--record`'s default is `--front own` (`RSI_FRONT` another default;
a run that does not record keeps the packs, so the gate's `rsi-check` and `rsi-essential` are as before); a passing `--record` writes
the cell's case inputs (`test/rust/eval-inputs-linux-amd64.edn`, AOT `aot-inputs-linux-amd64.edn`) from the run: C1 from
`front.tsv`, C5 from `shims.tsv`, C7 from the C target read, the rest the products still consumed (checked on a made-up run of
the synthetic list: 67 rows the Clojure reader reads).

The main host's night, after RIND4's `bin/heavy-lock bin/rsi runtime-ci`: `RF11_BATCH=1 bin/heavy-lock bin/rsi check --corpus samples,synthetic
--record` (now `--front own`: rsi-build inside it, about 35 minutes for core's attributes then a few minutes a program at 2.3 GB a
process; `RF11_BATCH=1` keeps one process at that), which writes the record and the case inputs; `bin/reap`, `bin/crops-score
--record`. Independence stays 80 until C2, C3, C4 and C6 are own (RFP53 (a)).

## As built: RIND5's vectors, the shuffle and the arithmetic (2026-11-17, evening)

The user decided (round 51): `simd_shuffle` (a), then `simd_mul`, `_div`, `_rem`, `_neg`, `_saturating_add` and `simd_select`, so
that vectors builds on the own front end. Built in `arbace/rust/back/codegen.ae`, each after rustc_codegen_llvm's
generic_simd_intrinsic (1.99.1):

- `simd_shuffle`: the index is a constant of M `u32` lanes (`SimdShuffleIdx<M>`); its value is the evaluated allocation the body
  carries (`(const .. (indirect ALLOC OFF))`, the bytes little-endian), the shufflevector's mask `<M x i32>`. The body that holds
  it is std's `Swizzle::concat_swizzle::<T, N>` at `main::Impl`, whose MIR (with rustc's CTFE of `<Impl as Swizzle<4>>::INDEX`
  in it) is the record's today (C2): the constant evaluation of the impl's associated `INDEX` at its arguments is needed when that
  body is Arbace's own (RFP55 (a), RUST-90's S4), not before; it is not written here.
- `simd_mul`, `simd_div`, `simd_rem` (fmul, fdiv, frem; sdiv, srem for signed lanes, udiv, urem else), `simd_neg` (fneg, or sub from
  zero), `simd_saturating_add` and `_sub` (llvm.[su](add|sub).sat.vNiK), `simd_select` (the mask's lanes made `<N x i1>`, select).
- The float reductions: `simd_reduce_add_ordered` (llvm.vector.reduce.fadd from the accumulator, ordered), `simd_reduce_max`, `_min`
  (fmax, fmin); `simd_as`'s saturating float-to-integer names its lanes' type as LLVM does (`.v4f32`, which rsi's lowering
  scalarizes).
- build-run's refusal lines carry the refusal's data (`a path [..]`, cut to 300 characters).

`bin/rust-front rsi-build vectors` (1.97 GB): the module is **ready** (codegen and rsi's lowering accept every body, std's SIMD
instances through the new intrinsics), but the sample stays on the pack: its own `main` is not built, `record main (a path
"[nil ["Impl" "concat_swizzle"] false :unknown]")`. The cause is `simd_swizzle!(a, b, [7, 0, 5, 2])`, a macro 2.0 of core
(swizzle.rs) that expands to a block with `use $crate::simd::Swizzle; struct Impl; impl Swizzle<{$index.len()}> for Impl { const
INDEX: .. = $index; } Impl::concat_swizzle($first, $second)`: the type-relative path `Impl::concat_swizzle` (a block-local struct
of the expansion's def-site context, the trait's provided method through the block's `use`, the trait's argument the anonymous
constant `{[7, 0, 5, 2].len()}`) has no resolution. Next: resolve's lexical lookup of a def-site item in the expansion's block
and the const argument's evaluation (a `len()` of an array literal, CTFE's first step), then `main` built. `bin/rust-front essential`
equals its known summary after the changes (run-built 7 of 7, 1.73 GB). The cell: 4 of 5 own, unchanged.

## As built: RIND5's swizzle, simd_swizzle!'s block resolved (2026-11-17, evening)

The user's next slice (round 51): `Impl::concat_swizzle` of `simd_swizzle!(a, b, [7, 0, 5, 2])` resolved so that vectors' own
`main` builds. Probed light: the pipeline (`arbace.rust.front.build-check/build-program`) over a seven-line program with the
pack rust-front-std's names and interface, seconds a run. Three causes, each after rustc 1.99.1's behaviour:

- **The block's items in scope of the body** (rustc_resolve: a block is an anonymous module whose items the body's paths see).
  An expansion's path has no resolution in the record (its span is core's file), so collect's `res-def` looks it up by its written
  names in `*module*`, the enclosing module (the crate's root for main), which does not hold main's block items (`struct Impl`'s
  parent is main). `collect/*body*` (the function whose body is checked: `body-fn`, a closure's or a block item's nearest function)
  is bound by typeck's check-body and with-solver, and `res-def` looks a name up there first (lookup-name already falls back from a
  function body to its module), but for `self::`, `super::`, `crate::`, `$crate::` and std's crates.
- **A macro's `use $crate::..`** (`use $crate::simd::Swizzle` in the block): lookup-name and typeck's traits in scope resolved the
  use's written path with `$crate` as a name; now `$crate` is the macro's crate (the use's `:krate`, else core), as res-def took it
  for paths. The impl's trait (`impl Swizzle<..> for Impl`, its header nil before) is core's `Swizzle`, and the trait is in scope for
  `Impl::concat_swizzle` (probe-assoc-fn's trait candidate).
- **Constant evaluation's first step**: the trait's const argument `{$index.len()}` was the unevaluated `{const :method}`, so no impl
  matched `Swizzle<_>`. `lower-const-arg` evaluates `[..].len()` of an array literal (its elements, or a repeat's literal count,
  through parentheses): `<[T; N]>::len` is N, here 4 (`Swizzle<4>`). The associated constant is now named as rustc names it,
  `<main::Impl as std::simd::Swizzle<4>>::INDEX`.

Unit cases: `bin/rust-front rind-test` (`test/rust/front/rind_test.ae`, the program `test/rust/front/swizzle1.rs`): `[a, b, c,
d].len()` is 4, `[x; 3].len()` is 3, the negative case `[..].first()` stays unevaluated, the impl's trait `Swizzle<4>` through the
block's use, main built, INDEX's name; all ok.

Then vectors' next cause, found by the sample's build: `f.cast::<i32>()`'s type `Simd<T, 4>` (no layout): normalize* substituted
an impl's associated type with the impl's arguments alone, so a generic associated type's own parameter (`type Cast<T> = Simd<T,
N>` of SimdFloat's impl) was left. The projection's own arguments (those after the trait's) now follow the impl's (a unit case in
rind-test: `f.cast::<i32>()` a `Simd<i32, 4>`, no local of a parameter's type). MIR's forms write a constant generic argument as
`(cst V)` (build's `arg-form`; before, `(other ":cval")`, which mono's form->ty does not read).

**`bin/rust-front rsi-build vectors`: own** (2.04 GB; mono's facts: callees 92/112 and fn-symbols 62/82 native, the rest the
record's, as for the other own cases). **The cell: 5 of its 5 cases own at C1** (intrinsics, panics, std, unwind, vectors), C5 and
C7 own; C2, C3, C4, C6 still rustc's (independence 80). `bin/rust-front essential` equal; `core-essential` 566 equal, its known summary, the control differing (2.19 GB).

## RIND stage 4 (C3, C4): the measures and the plan (2026-11-17, 19:00)

The user chose C3 and C4 next (RIND1's order). What the five cases consume, measured on their rsi-builds (mono-check against the
records, `RF11_BATCH=1`, this afternoon):

| case | callees | fn-symbols | items | langsyms | layouts | locs | symbols |
|---|---|---|---|---|---|---|---|
| intrinsics | 88/88 | 88/88 | 2/4 | 16/17 | 1204/1232 | 56/88 | 1/1 |
| panics | 63/64 | 56/56 | 34/68 | 16/17 | 808/903 | 50/64 | 17/17 |
| std | 124/125 | 119/122 | 20/45 | 16/17 | 4400/4655 | 74/125 | 10/11 |
| unwind | 66/66 | 55/55 | 28/56 | 16/17 | 733/806 | 47/66 | 14/14 |
| vectors | 62/82 | 62/82 | 6/12 | 16/17 | 2870/3082 | 61/82 | 3/3 |

(`core-closure` 0/N everywhere: std's bodies are the record's, C2.) The refusals C3 counts are the facts a run takes from the record:
vectors' 20 callees are one cause, `core::core_simd::..::{impl#N}::reduce_sum's attributes are not in core's sources` (portable
SIMD's macro-made methods: `bin/rust-front core-attrs` reads `test/rust/front/core-attrs.txt`'s files, none of them under
`portable-simd/`); the other cases' are one callee each and the locations and layouts mono writes differently (the mono-check
lines name them).

C4's two inputs:
- **The StableCrateIds** (`std-crates-rsi.edn`): `StableCrateId::new(name, lib, -C metadata, CFG_VERSION)` with the rlibs' extra
  filenames as the metadata does not give the recorded ids (core `1336fe62..` against `bdd42e15..`): Cargo passes a `-C metadata`
  of its own hash, which the rlib does not keep. Computing them natively means porting Cargo's metadata hash (Cargo's sources:
  not on this host, a fetch) or building std with Arbace's own `-C metadata` (the library stage, C6).
- **std's interface** (`std-iface.edn`, the helper's): the native whole-crate collection pass (RF12-P2 (b), RFP57 (a)) exists for
  core's modules (core-check's interface pass, about 23 minutes and 2.3 GB for the 37 modules); alloc and std (the linux `sys`) are
  not passed yet.

### Proposed (RIND8-RIND10)

- **RIND8 The library's crate identities.** (a) **Under RIND6 (a), the std build's crate identities (each crate's name and
  StableCrateId) are constants of the library build Arbace links (as CFG_VERSION is of the emulated rustc), kept as data
  (`std-crates-rsi.edn`, made once), checked against the rlibs' rmeta only in the oracle tier; computed natively when Arbace builds
  std with its own `-C metadata` (C6)**; (b) port Cargo's metadata hash now (Cargo's sources fetched: the user's go-ahead); (c) C4
  stays rustc's until C6. *Recommended: (a).*
- **RIND9 std's interface natively.** (a) **The interface pass crate by crate, core first (its 37 modules, the pass that exists),
  then alloc, then std's linux `sys` and its dependencies, each written as the helper's `std-iface` shape and compared fact by fact
  with it in a check of its own (night runs: about 25 minutes and 2.3 GB a crate's pass); a run reads the native interface once
  all three crates compare equal on the five cases' uses**; (b) the whole interface at once (one long pass, no staged proof).
  *Recommended: (a).* 3-5 days.
- **RIND10 C3's first slice.** (a) **The portable SIMD files in core-attrs (`../../portable-simd/crates/core_simd/src/simd/
  {num/int,num/uint,num/float,cmp/ord,cmp/eq}.rs` in `test/rust/front/core-attrs.txt`, if core-check's module tree reaches core's
  `#[path]` module), then each case's remaining refused callee and the locations' and layouts' differences, one by one, against
  mono-check**; (b) count C3 by the five cases' facts alone (a case whose run took no record fact). *Recommended: (a)*, judged by (b).

Not built tonight: from 19:00 UTC only code and unit tests under 1 GiB, and each step above needs an rsi-build or a core-attrs
pass (2.3 GB). The main host's commands for RIND10 (a) once decided: add the five lines, then `RF11_BATCH=1 bin/rust-front
rsi-build vectors` (core-attrs is rebuilt first: about 35 minutes, 2.3 GB), its `mono-check` line's callees against 62/82.

## As built: Rust's scores read, the facts stated, the host's rustc 1.100.0 (2026-11-17, night, round 52)

Branch `n-rust-mat` (from `next` `42bf8614b`), memory-free lane. Rust's record in `test/crops/scores.edn`: completeness 75,
maturity 96, independence 80. What holds each, from `bin/crops-score --judge --strict`:

- **Maturity**: one item short, `:cells` 0.75 (3 of 4 cells checked: the AOT cell has no record, `test/rust/check-aot-linux-amd64.edn`
  absent, the cell's `:until`). Classed, controls (3 of 3 families), gate (4 essential, 4 full, 5 oracle), REAP row and pins (5
  records, no absolute path) are 1. The same record (task 322's) moves completeness and maturity; nothing light moves either.
- **Independence**: eval 0.5 and AOT 0.5, read, print and judge 1. Eval is RFP53 (a)'s case count from
  `test/rust/eval-inputs-linux-amd64.edn`: 0 of 5 cases own in all seven inputs (C5 and C7 own; C1 buildable own for all five since
  RIND5's swizzle but not in the record until the night's `--record`; C2, C3, C4, C6 rustc's). AOT: the stated 0.5 (no record). No
  stage moves before C2, C3, C4 and C6 are own (RIND stage 5).
- **Stated tonight** (`test/crops/independence.edn`, the checkout's facts; no score moves): C5's judge `rsi-shim-essential` among the
  eval and AOT stages' `:steps` (the cross-check finds it on no toolchain host), C5's, C7's and C1's code in eval's `:evidence` (the
  shim, `RSI_SHIM=own`, `c_target_host`, a `--record`'s `--front own` default at `bin/lib/rsi/check:63`), and C1's state in `:why`.
  The AOT leg takes the own shim too: the shim block runs on the pack path before the level splits. Note: `--aot --record` keeps
  `--front pack` (line 63), so the AOT inputs record C1 from the packs unless `--front own` is passed.
- **Taken overnight (round 52): two steps not taken**: RIND10 (a)'s five core-attrs lines (core-attrs' key hashes its list: the next
  `rust-front-essential` would rebuild core's attributes, 35 minutes at 2.3 GB, unproven tonight); RIND8 (a)'s reading of the crate
  identities as own (a change of what counts as own: the user's, and it moves no score: C4 also needs the interface, RIND9).

### The host's rustc 1.100.0 (Alpine's apk upgrade, 2026-11-15)

`tools/rsdump/RUSTC_VERSION` pins 1.99.1 (`58cf8f9c3`, driver `61690d3e4c729118`); the host has rustc and rustdoc 1.100.0
(`0ef1bc386`, `librustc_driver-16ebf4feec204511.so`). What it breaks, measured (`bin/repr-rust pins check`, 56 MB):

- **`bin/gate --pins` fails** (its `:rust` crop pin, `bin/repr-rust pins check`: `rustc_check` and the rustdoc pin refuse; `rust pins:
  NOT held`), and with it **the clean run's first step** (`pre_step pins`, exit 1 before the plan): no `--clean` record, so no tag,
  until it is resolved. The ordinary gates (essential, `--nightly`, `--weekly`, `--full`) do not run `--pins`.
- **The oracle tier's five `:rustc` steps** (`rust-iface`, `rust-behaviour`, `rust-front-records-verify`, `rust-native-essential`,
  `rust-native`): the `:rustc` identity (`rustc --version -v`, the executable's and the driver library's digests) is a new one, so
  their keys move and they run, and each refuses at `rustc_check` (the helper cannot be built against 1.100's driver): failures in
  `bin/gate --oracle`, not absences.
- **By hand**: every helper-based tool (`bin/repr-rust --reader helper`, `native-check`, `--iface`, `--run`, `--code`; `bin/rust-front`'s
  oracles and `records make|verify`; `bin/rsi ir-pack`, `std-build`, `record-ref`): no new pack or reference can be made.
- **Not broken**: every essential and `--full` Rust step (no rustc at check time: the native reader, the packs, the pinned rust-src
  1.99.1 apk in `.tmp/repr-rust/apks`, ci, rustc stubs exiting 99), the scores (the `:rustc` host is on oracle steps alone).

### Proposed (RIND11)

- **RIND11 The pinned rustc and the host's.** (a) **Now: the pinned rustc is the oracle's host; another rustc on the host is the
  `:rustc` host absent, not a failure: the `:rustc` recipe `:require`s the pinned build (its version and driver library), so the five
  oracle steps are `absent` (no key, no run), and `bin/repr-rust pins check` says "the host's rustc is not the pinned 1.99.1: the
  oracle tier's Rust steps are absent" while still holding the file pins (as `--pins` treats an absent gcc); then, when the oracle
  tier is next needed for Rust (a tag), the pinned toolchain outside apk: Alpine's `rust-1.99.1-r0`, `rustc-dev-1.99.1-r0`,
  `rustdoc` and the LLVM library they link, fetched by SHA-256 (the user's go-ahead: no 1.99.1 apk but rust-src is cached here) and
  extracted into `.tmp/rust-1.99.1/` (nothing installed), named by `REPR_RUST_RUSTC` for the helper and the `:rustc` recipe**;
  (b) re-pin to 1.100.0: the helper rebuilt against 1.100's `rustc_private`, rust-src, rustc-dev and the UI tarball re-pinned, the
  five corpora's round trips and known files re-recorded, the native reader's lexer and parser and the front end's ports (MIR
  building, the passes, mono, codegen: written after 1.99.1's code) revised for 1.100's changes, and every pack (IR, std's IR,
  rust-front-*, rust-core-built, the reference runs) remade: weeks, and RIND's measures start over; (c) pin the toolchain outside
  apk at once, the gate failing at `--pins` until fetched; (d) hold Alpine's `rust` at 1.99.1 (`apk add rust=1.99.1-r0`: edge
  keeps no old versions, so a local repository from the archive's apks). *Recommended: (a)*: the gate's pins and clean run pass
  again at once, honestly (no gate step needs rustc), and the records stay valid; (b) only when the front end moves to a new
  rustc on purpose.

## As built: RIND9 (a), alloc's pass (2026-11-17, night, branch `n-rust-alloc`)

The user approved RIND9 (a). alloc's interface pass is written on core-check's (RF12-P2 (b)) and proven light on two modules; the
whole crate's pass is the day's run.

- `arbace/rust/front/alloc_check.ae` (`arbace.rust.front.alloc-check`): alloc's module files (core-check's `module-files` with the
  crate `alloc`, cfg as core's: 84 files of library/alloc), alloc's index (its 17 macro_rules! and its modules' names from the
  sources, cached in `.tmp/rust-front/alloc/index.edn`, about 5 minutes once), each module run by core-check's `run-module` with
  `*collect-only*` (expand, resolve, lower, collect; no body), its facts written by `native-iface` and read by the modules after
  it. The pass reads std's interface **with every alloc fact taken out** (`without-crate`: alloc's fns, ADTs, traits, impls and
  consts, and its impls out of the selection indexes), so all it writes of alloc is its own; std-names stays (names, not
  types). `ALLOC_IFACE_ROUNDS=2`: a second round over every module, each reading the first round's facts of the others (the
  modules' order). The output is the helper's shape, `(si/fn "PATH" {..})` with the helper's key order
  (`.tmp/rust-front/alloc/alloc-iface.edn`), and the compare holds every alloc fact of the helper's `std-iface.edn` to the
  native one: equal, differing (the keys, and the first fact's two values, by cause), missing; the native's extras (crate-private
  items the helper does not write) counted apart, not failures; the negative control judges an equal fact against another
  path's helper fact, which must differ.
- Generalized from core to the crate being built (core's build unchanged by construction): `cc/*crate*` (the tree, `crate::`
  in the names, `body-key`, the remapped `library/alloc/` paths), `resolve/crate-root` (`crate::`, `super::` at the root, the
  local paths' printing in collect), and `$crate`: a dependency's macro's `$crate` names that dependency
  (`resolve/crate-deps`, alloc `#{"core"}`; `resolve/*mark-crates*` bound by `run-module` from the expansion's marks, and a
  `use $crate::..`'s `:krate` in collect), core's build having none. lower keeps an impl's constness (`const unsafe impl
  Allocator for Global`), and native-iface writes its functions `:const true`, as fn_sig does.
- `bin/rust-front alloc-iface [FILE...]` (the pass and the compare), `--list`, `--units [--record]`
  (`bin/lib/repr-rust/core-build.bash`): the unit cases `test/rust/front/alloc_iface_test.ae`, 22 pure cases (the crate root,
  the tree with its negative case, `without-crate`, `owner`, the compare's four outcomes and its control, the shape written and
  read back) at GOMEMLIMIT 500MiB (0.75 GB), then each module of `test/rust/front/alloc-iface-units.txt` passed alone and
  compared at 600MiB, its summary held to `test/rust/front/alloc-iface-units-known.txt`.

### Results (light, 0.92 GB peak)

| module | facts | equal | differ | missing | native extras |
|---|---|---|---|---|---|
| `alloc::borrow` (borrow.rs) | 47 | 43 | 4 | 0 | 0 |
| `alloc::alloc` (alloc.rs) | 36 | 31 | 0 | 5 | 2 |

The causes the compare names (each a fix of its own, RIND9's next steps):
- a trait's associated type's bounds (`ToOwned::Owned: Borrow<Self>`): native-iface writes `[]`;
- an associated-type bound in a where clause (`B: ToOwned<Owned: Debug>`, Cow's Debug, Display and Default impls): the
  projection's clause is not among `explicit-clauses`;
- `#[derive_const(Clone, Default)]` and `#[derive(Copy, Debug)]` on `Global`: the derived impls (`{impl#6..8}`) and their
  functions not written (missing);
- (fixed here) a const impl's functions' `:const`: alloc.rs' 6 differences (18 equal of 36 before, 31 after) before the impl's constness was kept;
- the extras `dealloc_nonnull`, `realloc_nonnull`: crate-private, the helper's interface lacks them, as expected.

### Decisions taken overnight (round 52)

- RIND9a-1 (a) the pass reads std's interface less every alloc fact (no answer read from the helper), std-names kept: taken.
- RIND9a-2 (a) every helper fact judged (equal, differing, missing), the native extras counted apart: taken.
- RIND9a-3 (a) one round by default, `ALLOC_IFACE_ROUNDS=2` the second: taken (the day's run tells whether the order matters).
- RIND9a-4 (a) core's cfg for alloc (none of alloc's item cfgs reads debug_assertions or ub_checks; its features off): taken.
- RIND9a-5 (a) a dependency's macro's `$crate` names the dependency (`crate-deps`), core's own build unchanged: taken.

### The day's commands (not run here)

- The whole pass and its compare, about 25 minutes and 2.3 GB, one process (`GOMEMLIMIT=1800MiB`):
  `bin/rust-front alloc-iface` (its summary lines; `.tmp/rust-front/alloc/alloc-iface.edn.report` the causes), then
  `ALLOC_IFACE_ROUNDS=2 bin/rust-front alloc-iface` once to see whether a second round changes the counts.
- core unchanged by the generalization: `bin/rust-front core-essential` (its known summary) and `bin/rust-front rind-test`.

## As built: RIND9 (a), alloc's pass: its three causes fixed (2026-11-17, night, branch `n-rust-alloc2`)

The three causes the compare named on borrow.rs and alloc.rs are fixed, each in the code core's build shares:

- A trait's associated type's bounds: native-iface wrote `[]`; it now writes `collect/item-bounds` (explicit_item_bounds: the
  implicit `Sized`, or `?Sized`'s `MetaSized`, the written bounds, the trait's where clauses on the item), self the item's
  projection over the trait's identity arguments (`ToOwned::Owned`: Sized, then `Borrow<Self>`; `Join::Output`: Sized over
  `[Self Separator]`).
- An associated-type bound in a bound (`B: ToOwned<Owned: Debug>`): lower dropped the constraint's bounds (`{:bounds []}`); it
  now lowers them, and collect's `lower-trait-ref` gives the trait ref the metadata `:clauses`, lower_assoc_item_constraint's
  clauses in the constraints' order (an equality's projection, a bound's clauses on the projection, lower_bounds without
  implicit sizedness), which `lower-bounds` puts after the trait clause (`:constraints` stays for its other readers: dyn's
  projections).
- `#[derive_const(...)]`: the expansion read `#[derive]` alone, so `derive_const`'s impls were never made (and every later
  impl of the module numbered one or two short). expand_derive now reads both attributes in their order and writes a
  `derive_const` trait's impls `const impl` (TraitDef's is_const; Clone on a Copy type both TrivialClone's and Clone's, as core
  writes its own `const unsafe impl TrivialClone`); the attribute is dropped as `derive` is (expand, lower's attributes).

### Results (light, one process a module, 0.91 GB peak)

| module | facts | before: equal / differ / missing | after: equal / differ / missing | native extras |
|---|---|---|---|---|
| `alloc::borrow` (borrow.rs) | 47 | 43 / 4 / 0 | 47 / 0 / 0 | 0 |
| `alloc::alloc` (alloc.rs) | 36 | 31 / 0 / 5 | 36 / 0 / 0 | 2 |
| `alloc::slice` (slice.rs) | 37 | 30 / 5 / 2 | 32 / 3 / 2 | 7 |
| `alloc::collections` (collections/mod.rs) | 29 | 29 / 0 / 0 | 29 / 0 / 0 | 2 |
| `alloc::collections::btree::set_val` | 18 | 18 / 0 / 0 | 18 / 0 / 0 | 6 |

(vec/in_place_collect.rs was tried and left out: the helper writes none of its facts, all crate-private, so its control has
no pair.) slice.rs' remaining causes, not this round's: the late-bound regions of a where clause's Fn sugar written `'_`
where the helper writes `'^#0` (`sort_by`, `sort_by_key`, `sort_by_cached_key`'s `F: FnMut(&T, &T) -> Ordering`), and an
impl inside a function body inside an impl's function (`{impl#0}::to_vec_in::{impl#0}::to_vec::{impl#0}`) not written. The
unit cases: 25 pure (3 new: derive_const's text, its negative), and per module the causes' facts written by value
(test/rust/front/alloc_iface_test.ae's `module-cases`, with negatives: Cow's Hash impl has no projection clause, derive's
`fmt` not const), the summary held to `alloc-iface-units-known.txt` (five modules).

core's pass shares the code, so its facts may move: derive_const is written in 12 of core's files (cmp, any, option, result,
ops/control_flow, ops/range among `test/rust/front/core-modules.txt`: their impls now made, later `{impl#N}` renumbered as
rustc numbers them), associated-type bounds in option, convert/mod and ops/try_trait (their clauses now in the param envs);
none in core-essential's six modules.

### Decisions taken overnight (round 52)

- RIND9a-6 (a) a `derive_const` trait's every impl is const, TrivialClone's too (rustc passes is_const to each TraitDef of the
  derive; core's hand-written `const unsafe impl TrivialClone`): taken. Alternative: Clone's alone, set aside (no source for it).
- RIND9a-7 (a) the constraints' clauses as one ordered metadata `:clauses` beside `:constraints` (left for dyn's readers):
  taken. Alternative: `:constraints` extended with bound entries, set aside (its readers destructure `[ad args term]`).

### The day's commands (not run here)

- `bin/rust-front core-essential` (its known summary: expected unchanged) and `bin/rust-front rind-test`.
- core's whole check over `test/rust/front/core-modules.txt` (`bin/rust-front core-check --records`, as the day runs it) to see
  the derive_const and associated-type-bound modules move; re-record its known lines from a passing run.
- The whole alloc pass, `bin/rust-front alloc-iface` (about 25 minutes, 2.3 GB), for the crate's counts after the fixes.

## As built: RIND9 (a), alloc's pass: the compare module by module (2026-11-17/18, night, branch `n-rust-alloc3`)

slice.rs' two open causes fixed, then nine more modules passed alone (one process each, 0.90 GB peak, GOMEMLIMIT 500 MiB) and
each cause the compare showed fixed in the code core's build shares:

- Fn sugar's elided regions in a where clause (`F: FnMut(&T, &T) -> Ordering`): native-iface wrote `'_`; a clause is now
  written in its binder (`*clause-binder*`): an elided region `'^#N` (its index after the binder's named ones), a `for<'a>` one
  `'^'a`; inside a type's own binder (a fn pointer's, a trait object's) one more `^` (`'^^#0`).
- An impl in a body inside an impl's function (`{impl#0}::to_vec_in::{impl#0}::to_vec::{impl#0}`, Drop for to_vec's
  `DropGuard`) and every impl in an impl function's body (`{impl#17}::from::{impl#0}`): written at its def path (`body-path`:
  a body's impl `{impl#N}` under its function's path), a body's ADT named by its def path in the impl's types; a body's trait,
  functions and ADT facts not written (the helper writes none: the ConvertVec impl stays out).
- Trait objects (`Box<dyn Any, A>`, `dyn Error + Send + Sync + 'a`): ty->sty had no `:dyn` (the fact dropped); it writes the
  helper's `(dyn (trait P ARGS) [(proj ..)] [AUTOS] REGION)`, the auto traits Sync before Send (rustc's order in every one of
  std-iface's 83), an anonymous object region `'_`.
- Clauses the writer dropped: `E: 'a` `(type-outlives TY R)`, `'b: 'a` and a const parameter's ConstArgHasType as the helper's
  `(other TEXT)` (686 of std-iface's `the constant `N` has type `usize``).
- `#[cfg_attr(PRED, derive(Clone))]` (string.rs' FromUtf8Error, IntoChars): the attribute made from cfg_attr's items had empty
  arguments, so derive-names failed and the whole of string.rs did not expand; it now carries the list as delimited arguments.
- A generic associated type's own arguments (`<&'b str as Pattern>::Searcher<'a>`): a std trait's GAT had no own parameters
  (ext-generics); they are read off its bounds in std's interface (the projection over the trait's identity arguments, then its
  own).
- Lifetime elision: a by-value `self` is no elision candidate (resolve_fn_params' find_lifetime_for_self empty), though Self may
  be a reference (`impl Pattern for &'b String`'s `into_searcher(self, haystack: &str) -> Searcher<'_>`: the haystack's).
- A type parameter's default in generics (`Box<T, A = Global>`: `(type "A" (adt "alloc::alloc::Global" []))`, ADTs and traits),
  an impl's or trait's function's ABI (`extern "rust-call" fn call_once`), a negative impl's `:negative true` (`impl !Send for
  Rc`, so with-native-iface's selection no longer takes it as a candidate).
- An item with a tool attribute (`#[rustfmt::skip] const impl ...`) is an attribute invocation in rustc's InvocationCollector, so
  its DefId (and its impl's disambiguator) comes after its fragment's: the expansion numbers it when expanded (`:rf-order`), as
  a derive's. vec/mod.rs' `const impl<T, A> Vec<T, A>` and raw_vec's two: every later `{impl#N}` of those modules was one or two
  off; core's option.rs `FromResidual<Yeet<()>>` is the helper's `{impl#66}`, the last of 67, by the same rule.

### Results (light, one process a pass)

| module | facts | before | after | native extras |
|---|---|---|---|---|
| `alloc::slice` (slice.rs) | 37 | 32 / 3 / 2 | 37 / 0 / 0 | 7 |
| `alloc::boxed::convert` (boxed/convert.rs) | 54 | 26 / 3 / 25 | 54 / 0 / 0 | 7 |
| `alloc::string` (string.rs) | 284 | 0 / 0 / 284 (expand failed) | 284 / 0 / 0 | 74 |
| `alloc::boxed` (boxed.rs) | 166 | 159 / 7 / 0 (with the fixes above it) | 166 / 0 / 0 | 1 |
| `alloc::rc` (rc.rs) | 274 | 268 / 6 / 0 (likewise) | 274 / 0 / 0 | 49 |
| `alloc::sync` (sync.rs) | 287 | not measured | 287 / 0 / 0 | 35 |
| `alloc::fmt` (fmt.rs) | 1 | not measured | 1 / 0 / 0 | 0 |
| `alloc::collections::vec_deque` (alone) | 185 | not measured | 184 / 1 / 0 | 19 |
| `alloc::raw_vec` with `alloc::collections::vec_deque` | 189 | 187 / 1 / 1 | not rerun | 83 |
| `alloc::raw_vec` with `alloc::vec` (vec/mod.rs) | 168 | vec alone 12 / 52 / 100 | 168 / 0 / 0 | 71 |

(equal / differ / missing. A module passed alone lacks its crate siblings' facts: vec_deque's and vec's `buf: RawVec<T, A>`
written `RawVec<>` alone, equal with raw_vec passed before it, as the whole pass orders them.) The unit cases: 8 new pure ones
(the clause and inner binders, trait objects, outlives, an unknown clause's negative), the module cases of boxed/convert,
string, boxed, rc and raw_vec with negatives (test/rust/front/alloc_iface_test.ae), `alloc-iface-units.txt` ten modules, its
known file re-recorded from a passing `--record`.

core's pass shares every one of these, so its native facts may move toward the helper's: const generics' ConstArgHasType and
outlives clauses (every core impl and function with a const parameter or `T: 'a`), trait objects in signatures (formerly
dropped), Fn-sugar bounds of crate-private functions, impls in function bodies, GAT projections (`Pattern::Searcher<'a>`),
by-value self elision, defaults, negative impls (selection), `rust-call`, and the impl numbering of core's files with a tool
attribute on an impl (option.rs, result.rs: their later `{impl#N}` renumbered as rustc numbers them).

### Decisions taken overnight (round 52)

- RIND9a-8 (a) an item with a tool attribute numbered when its invocation is expanded, in source order with the derives'
  (rustc's InvocationCollector; option.rs' Yeet impl and vec's const impl confirm it): taken. Alternative: at the very end of
  the module, set aside (no case separates the two yet).
- RIND9a-9 (a) the auto traits of a trait object written Sync before Send, others after by path (std-iface's 83 objects; rustc
  sorts by DefPathHash, which Arbace does not compute): taken. Alternative: compute the stable hash, set aside (no case needs it).
- RIND9a-10 (a) a std GAT's own parameters read off its bounds in std's interface: taken. Alternative: a std-names field, set
  aside (the pack's format is the helper's).

### Open (not this round)

- vec_deque, vec and raw_vec are equal only passed together: the whole pass's order covers it.
- Arguments of type `Self` in an impl for a reference still count Self's regions as elision candidates (only by-value `self`
  is excused); no compared fact shows it yet.

### The day's commands (not run here)

- `bin/rust-front core-essential` and `bin/rust-front rind-test` (core's essential summary may move: ConstArgHasType and
  outlives clauses in its native facts); core's whole check over `test/rust/front/core-modules.txt`, its known lines re-recorded
  from a passing run.
- The whole alloc pass, `bin/rust-front alloc-iface` (about 25 minutes, 2.3 GB), for the crate's counts after the fixes (remove
  `.tmp/rust-front/alloc/index.edn` first: the expansion changed).

## As built: RIND9 (a), alloc's pass: the compare across the crate's modules (2026-11-18, night, branch `n-rust-alloc4`)

alloc's index rebuilt with the new expansion (84 files, 17 macros, 160 modules), then 32 more of alloc's modules passed alone or
after the siblings whose ADTs they name (one process each, at most 0.92 GB, GOMEMLIMIT 500 MiB, up to four at once), each cause
the compare showed fixed in the code core's build shares, and n-rust-alloc3's open cause closed:

- `Self` in an impl's header (`impl PartialEq<Cow<'_, Self>> for CStr`, `impl<R> SpecReadByte for BufReader<R> where Self:
  Read`): resolve walked the generics, the trait ref and the self type outside the SelfTyAlias rib, so `Self` there was an error
  (the impl not written, the clause dropped); they are now walked under it, as late.rs' resolve_implementation does (with_self_rib
  around walk_trait_ref, visit_ty(self_type) and visit_generics).
- A const parameter as an ADT's argument (`BoxedArrayIntoIter<T, N, A>`): core-check's writer had `cparam` only as an array's
  length, so every impl of the type (eight in boxed/iter.rs) was dropped; it writes `(cparam I N)` among the arguments.
- An array length naming a constant of the crate (btree's `[MaybeUninit<K>; CAPACITY]`, `CAPACITY = 2 * B - 1`, `[..; 2 * B]`):
  collect's integer evaluation follows a path to a local constant to its expression (depth 16), so the length is the value
  rustc's type_of writes, 11 and 12 (LeafNode and InternalNode were not written); parentheses and groups evaluated through.
- derive's projections (`#[derive(Debug)] struct Splice<'a, I: Iterator + 'a, A ..> { drain: Drain<'a, I::Item, A>, .. }`): the
  derived impl lacked `I::Item: Debug`; expand_derive now writes generic/mod.rs' find_type_parameters bounds, each field type's
  path whose first segment is a type parameter (not the parameter alone), bounded by the trait, in the impl's where clause after
  the item's predicates (impl-bounds keeps the impl's own predicates after the item's).
- A constant's `:value` (std's interface writes none of its 192 constants; the pass keeps it for its own readers): compared only
  when the helper's fact has one (io/read.rs' `DEFAULT_BUF_SIZE`, now evaluated through its `cfg_select!` group).
- (n-rust-alloc3's open cause) an argument of type `Self` in an impl for a reference: lower-ty gives a `Self` the very object
  `*self-ty*` is, and input-regions skips it, so Self's regions are no elision candidates (resolve's `Self` path has none of its
  own). No compared fact of alloc shows it; the pure cases do.
- Tooling: `CORE_TRACE=1` names each impl and ADT native-iface leaves out and why; a line of `alloc-iface-units.txt` may name
  several files, the earlier passed first as context and the last compared (`--units`, the test's module mode).

### Results (light, one process a pass; equal / differ / missing; "ctx" the siblings passed before it)

| module | facts | before | after | native extras |
|---|---|---|---|---|
| `alloc::task` | 22 | 22 / 0 / 0 | 22 / 0 / 0 | 2 |
| `alloc::str` | 25 | 25 / 0 / 0 | 25 / 0 / 0 | 4 |
| `alloc::ffi::c_str` | 177 | 174 / 0 / 3 | 177 / 0 / 0 | 3 |
| `alloc::collections::linked_list` | 181 | 181 / 0 / 0 | 181 / 0 / 0 | 8 |
| `alloc::collections::binary_heap` | 163 | 163 / 0 / 0 | 163 / 0 / 0 | 10 |
| `alloc::boxed::iter` | 71 | 46 / 1 / 24 | 71 / 0 / 0 | 6 |
| `alloc::boxed::thin` | 29 | 29 / 0 / 0 | 29 / 0 / 0 | 14 |
| `alloc::bstr` | 222 | 222 / 0 / 0 | 222 / 0 / 0 | 2 |
| `alloc::wtf8` | 59 | 59 / 0 / 0 | 59 / 0 / 0 | 14 |
| `alloc::io::error` | 4 | 4 / 0 / 0 | 4 / 0 / 0 | 6 |
| `alloc::io::impls` | 108 | 108 / 0 / 0 | 108 / 0 / 0 | 0 |
| `alloc::io::read` | 22 | 21 / 1 / 0 | 22 / 0 / 0 | 48 |
| `alloc::io::util` | 61 | 60 / 1 / 0 | 61 / 0 / 0 | 5 |
| `alloc::io::cursor` | 33 | 33 / 0 / 0 | 33 / 0 / 0 | 4 |
| `alloc::io::buf_read` | 9 | 9 / 0 / 0 | 9 / 0 / 0 | 2 |
| `alloc::io::buffered::bufreader` | 41 | 40 / 1 / 0 | 41 / 0 / 0 | 0 |
| `alloc::io::buffered::bufwriter` | 41 | 41 / 0 / 0 | 41 / 0 / 0 | 5 |
| `alloc::io::buffered::linewriter` | 18 | 18 / 0 / 0 | 18 / 0 / 0 | 0 |
| `alloc::collections::btree::node` | 29 | 27 / 0 / 2 | 29 / 0 / 0 | 195 |
| `alloc::collections::btree::navigate` (ctx borrow, node) | 13 | alone 5 / 8 / 0 | 13 / 0 / 0 | 74 |
| `alloc::collections::btree::map` (ctx borrow, node, navigate, search, set_val) | 320 | alone 309 / 9 / 2 | 320 / 0 / 0 | 5 |
| `alloc::collections::btree::set` (ctx the map's and map) | 222 | alone 219 / 3 / 0 | 220 / 2 / 0 (merge_iter not in ctx) | 1 |
| `alloc::vec::into_iter` (ctx raw_vec, vec) | 49 | not measured | 49 / 0 / 0 | 4 |
| `alloc::vec::drain` (ctx) | 24 | not measured | 24 / 0 / 0 | 0 |
| `alloc::vec::splice` (ctx) | 14 | 13 / 1 / 0 | 14 / 0 / 0 | 0 |
| `alloc::vec::extract_if` (ctx) | 11 | not measured | 11 / 0 / 0 | 0 |
| `alloc::vec::partial_eq` (ctx) | 36 | not measured | 36 / 0 / 0 | 0 |
| `alloc::collections::vec_deque::iter` (ctx raw_vec, vec_deque) | 30 | not measured | 30 / 0 / 0 | 0 |
| `alloc::collections::vec_deque::drain` (ctx) | 19 | not measured | 19 / 0 / 0 | 0 |
| `alloc::collections::vec_deque::into_iter` (ctx) | 30 | not measured | 30 / 0 / 0 | 0 |
| `alloc::ffi` (ffi/mod.rs) | 0 | no fact (reexports): left out | | |

(rc.rs and ffi/c_str.rs rerun after the resolve change: 274 and 177 equal, as before.) Every difference left is a sibling's ADT
written `Adt<>` when the sibling was not passed first: the whole pass's order (alloc's modules in the index's order: btree's map
before node, navigate, search) leaves such fields empty in one round; a DECISION below. The unit cases: 7 pure ones more (Self's
regions with a negative, a const parameter argument with a negative, a constant's :value with a negative) and the module cases of
ffi::c_str, boxed::iter, btree::node, io::buffered::bufreader, io::util and vec::splice with negatives;
`alloc-iface-units.txt` 30 lines (20 new, five with context), its known file re-recorded from a passing `--units --record` (40 pure cases, 30 module lines, about 75 minutes, peak 0.97 GB).

core's pass shares every one of these, so its native facts may move toward the helper's: `Self` in impl headers and where
clauses (core's `impl<T> PartialEq<..Self..>`, `where Self: Sized` on impls; formerly unresolved), impls of ADTs with const
parameters (core_simd's `Simd<T, N>`, `MaybeEscapedCharacter<N>`, map_windows: formerly every such impl dropped), array lengths
naming constants (core's `[u8; N_CONST]` fields), derives over projections (`#[derive(Clone, Debug)]` on structs with `I::Item`
fields: iter adapters' Peekable, Flatten's, MapWindows), consts' `:value` through parentheses, and Self-typed arguments' elision.

### Decisions taken overnight (round 52)

- RIND9a-11 (a) resolve walks an impl's generics, trait ref and self type under the SelfTyAlias rib (late.rs' order inside it
  kept as Arbace's: generics first): taken. Alternative: rustc's order (trait ref, self type, generics), set aside (no fact
  separates them; the resolutions are by span).
- RIND9a-12 (a) an array length naming a crate constant evaluated, a const generic argument naming one left unevaluated (rustc
  prints an unevaluated anonymous constant there): taken. Alternative: evaluate both, set aside (no helper fact shows a const
  argument naming a constant).
- RIND9a-13 (a) a constant's `:value` compared only when the helper writes one: taken. Alternative: drop the native `:value`, set
  aside (the pass's later modules read it).
- RIND9a-14 (a) a units line's earlier files are context, only the last compared: taken. Alternative: compare every file of the
  line, set aside (each context module is compared on a line of its own).

### DECISION (for the user)

- RIND9a-15 the whole pass's order: (a) the default `ALLOC_IFACE_ROUNDS=2` for the whole pass (the second round reads every
  module's first-round facts; about 50 minutes, 2.3 GB; recommended: simplest, it closes every context difference seen);
  (b) a first round of ADT and trait headers alone (generics, no bodies), then one full round (cheaper, a new mode);
  (c) the modules ordered by the ADTs they name (a dependency order computed from the index).

### The day's commands (not run here)

- `rm .tmp/rust-front/alloc/index.edn` is not needed again (this branch's expansion is the index's), but the whole pass is:
  `bin/rust-front alloc-iface` (about 25 minutes, 2.3 GB, one process) and `ALLOC_IFACE_ROUNDS=2 bin/rust-front alloc-iface`
  for RIND9a-15.
- core: `bin/rust-front core-essential` (its known summary may move: Self in impl headers, const-parameter ADT arguments, array
  lengths) and `bin/rust-front rind-test`; core's whole check over `test/rust/front/core-modules.txt`, its known lines
  re-recorded from a passing run.

## As built: RIND9 (a), alloc's pass: the rest of the crate's modules (2026-11-18, night, branch `n-rust-alloc5`)

alloc's index rebuilt (84 files, 17 macros, 160 modules), then every module file not yet in `alloc-iface-units.txt` passed alone or
after the siblings whose ADTs it names (one process at a time, GOMEMLIMIT 500 MiB, each under 1 GiB) and compared with the helper's
facts of that module. Three causes fixed in the code core's build shares, and the units' control generalized:

- derive's projection bounds once: expand_derive wrote `I::Item: Clone` for every field that names it (`enum Peeked<I: Iterator> {
  A(I::Item), B(I::Item) }`), the helper once: the bounds find_type_parameters adds all carry the derive's span, so predicates_of's
  set of (clause, span) keeps the first. A field type already bounded (compared without its spans and identifiers' positions) adds
  none; a written predicate equal to a derived one has another span and stays (unchanged).
- `super` after a leading `super` (`use super::super::borrow::DormantMutRef` in btree/map/entry.rs): resolve read `super` only as
  the first segment, so the second was a name looked up in `map` and every field naming the import was an error (VacantEntry and
  OccupiedEntry not written, and set/entry.rs' entries, which wrap them). `super-res` gives a module's parent, a local module's or
  std's (the root file's module built alone has std's module of `*crate-path*`'s parent), in paths and imports alike.
- (tooling) the negative control needs a second fact of a kind: a module of one fact (fmt.rs' `format`, intrinsics.rs) takes it
  from the crate's helper facts (`control`'s POOL); a module with no helper fact at all has nothing to judge and stays out of the list.

### Results (light, one process a pass; equal / differ / missing; "ctx" the siblings passed before it)

| module | facts | before | after | native extras |
|---|---|---|---|---|
| `alloc::fmt` | 1 | 1 / 0 / 0 (no control pair) | 1 / 0 / 0 | 0 |
| `alloc::sync` | 287 | 287 / 0 / 0 (not in the list) | 287 / 0 / 0 | 35 |
| `alloc::intrinsics` | 1 | | 1 / 0 / 0 | 0 |
| `alloc::collections::btree::mem` | 2 | | 2 / 0 / 0 | 2 |
| `alloc::collections::btree::merge_iter` | 10 | 8 / 2 / 0 | 10 / 0 / 0 | 4 |
| `alloc::collections::btree::dedup_sorted_iter` | 2 | | 2 / 0 / 0 | 3 |
| `alloc::collections::btree::map::entry` (ctx the map's) | 38 | 36 / 0 / 2 | 38 / 0 / 0 | 0 |
| `alloc::collections::btree::set` (ctx the map's, merge_iter) | 222 | 220 / 2 / 0 | 222 / 0 / 0 | 1 |
| `alloc::collections::btree::set::entry` (ctx the map's, map::entry, merge_iter, set) | 21 | 19 / 0 / 2 (with map::entry too) | 21 / 0 / 0 (round 54) | 0 |
| `alloc::vec::cow` (ctx raw_vec, vec) | 10 | | 10 / 0 / 0 | 0 |
| `alloc::vec::drain` (ctx) | 24 | | 24 / 0 / 0 | 0 |
| `alloc::vec::extract_if` (ctx) | 11 | | 11 / 0 / 0 | 0 |
| `alloc::vec::in_place_drop` (ctx) | 4 | | 4 / 0 / 0 | 4 |
| `alloc::vec::partial_eq` (ctx) | 36 | | 36 / 0 / 0 | 0 |
| `alloc::vec::peek_mut` (ctx) | 10 | | 10 / 0 / 0 | 0 |
| `alloc::vec::set_len_on_drop` (ctx) | 2 | | 2 / 0 / 0 | 5 |
| `alloc::collections::vec_deque` (ctx raw_vec) | 185 | alone 184 / 1 / 0 | 185 / 0 / 0 | 19 |
| `alloc::collections::vec_deque::drain` (ctx raw_vec, vec_deque) | 19 | | 19 / 0 / 0 | 0 |
| `alloc::collections::vec_deque::extract_if` (ctx) | 11 | | 11 / 0 / 0 | 0 |
| `alloc::collections::vec_deque::into_iter` (ctx) | 30 | | 30 / 0 / 0 | 0 |
| `alloc::collections::vec_deque::iter_mut` (ctx) | 30 | | 30 / 0 / 0 | 0 |
| `alloc::collections::vec_deque::splice` (ctx) | 14 | | 14 / 0 / 0 | 0 |
| `alloc::io::cursor` | 33 | | 33 / 0 / 0 | 4 |
| `alloc::io::buf_read` | 9 | | 9 / 0 / 0 | 2 |
| `alloc::io::copy` | 4 | | 4 / 0 / 0 | 0 |
| `alloc::io::copy::specialization` | 8 | | 8 / 0 / 0 | 6 |
| `alloc::io::buffered` | 15 | | 15 / 0 / 0 | 0 |
| `alloc::io::buffered::bufwriter` | 41 | | 41 / 0 / 0 | 5 |
| `alloc::io::buffered::linewriter` | 18 | | 18 / 0 / 0 | 0 |
| `alloc::io::buffered::linewritershim` | 8 | | 8 / 0 / 0 | 8 |
| `alloc::io::buffered::bufreader::buffer` | 1 | | 1 / 0 / 0 | 15 |

Left out, no fact of theirs in the helper's interface (all crate-private, or reexports and macros alone): lib.rs, macros.rs,
ffi/mod.rs, io/mod.rs, io/prelude.rs, io/copy/generic.rs, btree's mod.rs, fix.rs, remove.rs, split.rs and append.rs, vec's is_zero.rs,
spec_extend.rs, spec_from_elem.rs, spec_from_iter.rs, spec_from_iter_nested.rs and in_place_collect.rs, vec_deque's macros.rs,
spec_extend.rs and spec_from_iter.rs (20 files). With them every one of alloc's 84 module files has been passed: 64 in
`alloc-iface-units.txt` (61 lines), every helper fact of theirs equal. The unit cases: 2 pure more (the control's pool, with its
negative) and the module cases of btree::merge_iter (the derive's bound once) and btree::map::entry (the `super::super` import's
field, OccupiedEntry written, no error left). (The known file was re-recorded in round 54, below.)

core's pass shares the derive and resolve changes, so its native facts may move: derives on types naming the same projection in
two fields (core's iter adapters' `#[derive(Clone, Debug)]` structs and enums over `I::Item`, `U::IntoIter`: a duplicate clause
gone), and a `super::super::` path (core's iter/adapters/chain.rs and flatten.rs, iter/traits/iterator.rs, if any is a path and not
a doc link).

### Decisions taken overnight (round 52)

- RIND9a-16 (a) a derive's projection bound written once per type, compared without spans and identifier positions (all of them
  carry the derive's span; predicates_of dedups by clause and span): taken. Alternative: dedup every clause of predicates_of, set
  aside (a written duplicate has its own span and rustc keeps it).
- RIND9a-17 (a) `super` read after leading `super`s only, std's module parents through `*crate-path*` (rustc's resolve_path:
  `super` only in the path's prefix): taken.
- RIND9a-18 (a) a module of one fact takes its control's other fact from the crate's helper facts; a module of none stays out of
  the units list: taken. Alternative: a synthetic other fact, set aside (the control must judge a real helper fact).

### The day's commands (not run here)

- core: `bin/rust-front core-essential` and `bin/rust-front rind-test` (derives over a repeated projection may drop a duplicate
  clause from core's native facts); core's whole check over `test/rust/front/core-modules.txt`, its known lines re-recorded from a
  passing run.
- The whole alloc pass, `bin/rust-front alloc-iface` (about 25 minutes, 2.3 GB, one process), and `ALLOC_IFACE_ROUNDS=2` for
  RIND9a-15 (still the user's).

## As built: RIND9 (a), alloc's pass: set::entry and the whole crate (2026-11-18, day, round 54, branch `n-rust-alloc5`)

set::entry stayed at 19 of 21 with map::entry passed before it: OccupiedEntry and VacantEntry were not written (`CORE_TRACE=1`:
`inner`'s type `unresolved ["map" "OccupiedEntry"]`). set/entry.rs names `use super::{SetValZST, map}`, two of set's own imports
(`use super::map::{self, BTreeMap, Keys}`, `use super::set_val::SetValZST`), and a module built alone sees set through the crate's
index, which recorded a module's definitions and its single imports but skipped a `self` import. Two fixes in the index
(`core_check/file-names`, so core's build shares them):

- a `self` import binds the module it ends (`use super::map::{self, ..}`: `map`, its target the module itself), with-core-names
  copying it under its name as any re-export; a chain of imports (set/entry's `map` through set's) is its two rounds;
- each leading `super` one module up (`super::super::borrow` from btree::map::entry), where the index read the first alone.

alloc's index rebuilt (84 files, 17 macros, 160 modules, unchanged counts). set::entry 21 of 21 with its siblings; its module
cases (OccupiedEntry's `inner` through set's imports, VacantEntry written, the negative: no field unresolved) beside map::entry's.

### Results

- `bin/rust-front alloc-iface --units --record`: `bin/rust-front alloc-iface --units --record` from a passing run with all three fixes: 42 pure cases, 61 module lines (64 files), every line's helper facts equal (3,696 facts summed over the lines, 0 differ, 0 missing), every control differing; `alloc-iface-units-known.txt` re-recorded (609 lines), its earlier 283 lines unchanged. About 95 minutes on a loaded host, each process under 1 GiB.
- The whole pass, `bin/rust-front alloc-iface`, now two rounds by default (RIND9a-15 (a), below): **3,863 of 3,863 helper facts
  equal**, 0 differ, 0 missing (adts 128, consts 1, fns 2,373, impls 1,351, traits 10; native extras 1,015), the control
  differing as it must; 15 min 20 s wall (two rounds of 398 s and 462 s on a loaded host), 2.07 GB peak. One round
  (`ALLOC_IFACE_ROUNDS=1`): 3,841 equal, 18 differ, 4 missing, native extras 1,010: the order's differences, which the second round
  closes, every one.
- core: `bin/rust-front core-essential` equals its known file (566 equal, the control differing, no stub called);
  `bin/rust-front rind-test` passes; both again after the third fix below. core's whole check, `bin/rust-front core-check
  --records` over the 37 modules (the interface pass afresh; 35 min on a loaded host, 2.17 GB peak): first **5,693 equal**, 166
  but for the allocations' ids, 6 but for the allocations, 30 differ, **11 refused**, five of them array's StackOverflowError
  (from_fn, try_from_fn and its `{constant#2}`, map, try_map). The overflow was not this round's: since n-rust-alloc2 (RIND9 (a)'s
  first causes) lower-trait-ref lowers an associated-type bound's bounds into the trait ref's `:clauses`, and
  trait-bounds-defining, resolving `T::Residual` in `ChangeOutputType`'s `T: Try<Residual: Residual<V>>`, lowered the same bound
  again without end. rustc's probe there (PredicateFilter::SelfTraitThatDefines) lowers the trait ref alone: collect's
  `*trait-ref-only*`, bound by trait-bounds-defining, leaves `:clauses` out. After it: **5,696 equal**, 166, 6, 32 differ,
  **6 refused** (array 88 -> 91 equal, 5 -> 0 refused, 1 -> 3 differ: from_fn and map now built, differing by a user type
  annotation's `wrap_mut_1` path; try_from_fn and try_map equal); every other module's line unchanged between the two runs.
  Against the last recorded run (RF20's, 2026-11-11: 5,594 / 161 / 33 differ / 6 refused) no module was compared line by line (that run's
  per-module lines are not in the tree); the totals moved up. The whole alloc pass after the third fix: 3,863 of 3,863 again (14 min
  16 s, 2.06 GB).

### Decisions taken (round 54)

- RIND9a-15 (a) two rounds the whole pass's default (`ALLOC_IFACE_ROUNDS=2`; `=1` one): taken. The one-round run's 22 differences
  are all closed by the second; alternatives (b) a headers round and (c) a dependency order set aside (a new mode for what the
  second round already gives, at 15 minutes).
- RIND9a-19 (a) the index records a `self` import as its module's name and reads leading `super`s one module each: taken.
  Alternative: resolve set/entry's imports by passing set's file as context, set aside (context gives ADTs' generics, not a
  module's imports; the index is where a module built alone reads its parent's names).
- RIND9a-20 (a) trait-bounds-defining lowers the bound's trait ref without its constraints' bounds (rustc's
  SelfTraitThatDefines): taken. Alternative: a depth guard on type-relative, set aside (it hides the cause and refuses where rustc
  resolves).

### Left

- alloc's interface is equal fact for fact; RIND9 (a)'s next crate (std over the linux `sys`) is the next step. array's from_fn
  and map differ from rustc's MIR by a user type annotation's path (`NeverShortCircuit::wrap_mut_1`), now that they build.

## As built: RIND9 (a), std's pass: libc's and hashbrown's names natively (2026-11-19, queue 400, branch `400-rust-vendor`)

std's whole pass (round 54, `6aa150550`) left 76 helper facts missing and 2 differing, all of them through std's two vendored
dependencies, libc and hashbrown, whose names and facts the helper's std-names and std-iface do not hold. Decided at
`d12096365`: read their names from library/vendor natively, not from the helper. Built here, on mac/n-all4 `88b8e7f1a`.

The two crates are read from rust-src 1.99.1's `library/vendor/libc-0.2.189` and `hashbrown-0.17.1`. These are the versions
std's Cargo.lock pins, and the pinned crate set's `.crate` files hold the same versions (`test/rust/crates-2026-10-18.sha256`).
Before std's modules, each crate goes through core-check's machinery as alloc does: its module tree, its index (macros and
names from the sources), and its interface pass (`arbace.rust.front.alloc-check/deps-setup`). The pass runs two rounds
(`DEPS_IFACE_ROUNDS`), because libc's aliases and structs name each other across its files: in round 1, new::musl's `cmsghdr`
waits for unix's `socklen_t`. std's pass then reads three things from it:
- the crates' index names, with their macro-made ADTs (libc's `s! { .. }` structs);
- their native aliases with the aliases' generics (`socklen_t` -> u32);
- their native facts (ADTs, functions, impls).

The pieces are cached in `.tmp/rust-front/std/deps/`, as `index.edn` is, and `bin/rust-front std-iface --deps` makes them.
`STD_DEPS=0` leaves them out.

What the crates needed, each change gated to them (core's and alloc's readings as before):
- **The crate root under library/vendor** (`core_check/crate-lib-file`, as mono's `lib-crate-root` finds it).
- **Their cfg** (`crate-cfg`):
  - std's `features = ['rustc-dep-of-std']` with each crate's own feature closure from its Cargo.toml: libc gets `align` and
    `rustc-std-workspace-core`; hashbrown gets `nightly`, `core`, `alloc` and `rustc-internal-api`, so no `raw-entry`, no
    `default-hasher` and no `equivalent`.
  - libc's build script's cfgs for the target: `freebsd12` by default, and `musl_v1_2_3` because the target's cfg has
    `libc_unstable_musl_v1_2_3`. Nothing 32-bit.
- **libc's `prelude!()`**, a macro without parameters whose `mod types;` and `mod prelude { .. }` the tree and the index read
  where the call stands (`*item-macros*`, `crate-item-macros`: the root's `#[macro_use] mod` files' one-arm, no-parameter
  macros). rustc's expansion does the same: a macro-expanded outline module is resolved relative to the invocation.
- **Glob re-exports** (`pub use unix::*`, hashbrown's `pub mod hash_map { pub use crate::map::*; }`) and `#[path]` relative to
  the declaring file, as std's (`std-like?`).
- **with-core-names' rounds of re-exports and globs to a fixpoint, at most 8.** libc's chain is libc -> unix -> linux_like ->
  linux -> musl -> b64 -> x86_64. A module's globs from each of its files are merged (they were dropped when a module's entry
  already existed).
- **The index's `use` paths** (`file-names`): a dependency of the crate is absolute (resolve's `crate-deps`: std's libc and
  hashbrown, hashbrown's alloc), and so is a root alias (`root-extern-aliases`: hashbrown's `extern crate alloc as stdalloc`).
  The same tables give the resolver's extern prelude, so std's modules see `libc` and `hashbrown`.
- **libc's f! and safe_f!** put `$body:block` where a function's body stands. The expander's placeholder ident is no block to
  the native parser, which failed `unix`, `linux_like`, `linux` and `linux_l4re_shared` ("expand: the parser:"). For the
  dependencies alone (`ex/*raw-block-frags*`), a block fragment goes into the output as its own tokens, as rustc's parser takes
  the NtBlock's block. core's `const_eval_select!` keeps its placeholders.
- **A re-export through a module's renamed import**, std's own: sys/fs/mod.rs' `use unix as imp; pub use imp::{File, ..}`
  (sys::process's and sys::time's likewise). The first whole pass with the dependencies left fs's 13 ADTs, and process's and
  time's, missing: `unresolved ["fs_imp" "File"]`, because std::sys::fs had no File. with-core-names now reads a re-export's
  module path segment by segment through the modules' children (`module-of`: std::sys::fs::imp is std::sys::fs::unix), for
  std and its dependencies alone. The 76 missing facts were never all libc's and hashbrown's (RIND9 (a)'s entry had not checked
  them one by one): this cause was std's own.

### Results

- libc: 48 module files, every stage ok in both rounds: 182 ADTs, 112 aliases, 113 functions, 22 impls. Each libc type the
  helper's std facts name is among them: `sockaddr_un`, `sockaddr`, `ucred`, x86_64's `stat`, `DIR`, `cmsghdr`. 2 rounds of 484
  and 447 s.
- hashbrown: 17 module files, every stage ok: 67 ADTs, 644 functions, 333 impls. 2 rounds of 134 s. After the module-alias
  fix: 69 ADTs and 646 functions, because control::group's `use sse2 as imp; pub(crate) use self::imp::Group` now resolves.
- os/unix/net/addr.rs and collections/hash/map.rs passed together (one round): **258 of 258 equal**, against 247 equal, 1
  differing and 10 missing before. SocketAddr, HashMap and its 7 iterators and entries are now written, `from_parts` too, and
  `new`'s FnOnce clause is equal.
- `std-iface --units`: every pure case passes (69, 17 new). The 7 recorded lines are unchanged. The 2 new lines, addr.rs and
  map.rs, are 15/15 and 243/243, and their module cases pass. Control: with `STD_DEPS=0` and the base's index, addr.rs is 12
  equal, 1 differing and 2 missing, and both of its libc cases FAIL.
- std's whole pass with the dependencies, before the module-alias fix (two rounds, 54 min, 1.19 GB): **4,605 of 4,644 equal**,
  1 differs, 38 missing (adts 239 of 272, consts 17 of 17, fns 2,688 of 2,693, impls 1,631 of 1,632, traits 30 of 30; native
  extras 1,468), the control differing. Before (`6aa150550`): 4,566 equal, 2 differ, 76 missing.
  - Every libc and hashbrown cause is gone: SocketAddr, ucred, ancillary's messages, HashMap/HashSet and their iterators and
    entries.
  - What was left: fs's, process's and time's ADTs, through the module alias above.
- The module-alias fix, proven narrow (the whole pass again is the main host's). The 15 module files that held the 38 are
  passed together over two rounds: sys::fs::unix, fs, sys::process::{env, unix::common, unix::unix, unix}, process,
  os::linux::process, sys::pal::unix::time, sys::time::unix, time, os::unix::net::ancillary, env, backtrace and io::stdio. The
  result is **1,082 of 1,090 equal**, 1 differing, 7 missing (1 min 42 s for the two rounds).
  - std::fs::File, DirEntry, ReadDir, OpenOptions and the rest of fs, Command, Child, ExitStatus and process's others, and
    Instant and SystemTime are written and equal.
  - What is left there: std::fs::Metadata (`fs_imp::FileAttr`), process's ChildStdin/out/err (the alias ChildPipe, from a file
    not passed), CommandArgs' CStringIter lifetime (cstring_array, not passed), env's SplitPaths (sys::paths, not passed),
    backtrace's Inner (its `LazyResolve` is an `impl FnOnce` alias, a TAIT) and stdio's `as_mut_buf` (returns `impl Read`).
  - Several of these are context a narrow run lacks. Each is to be judged on the main host's whole pass.
- alloc's whole pass: **3,863 of 3,863 equal**, 0 differ, 0 missing, native extras 1,015. This is unchanged, and the control
  differs as it must (21 min 32 s).
- core: `core-essential` equals its known file (566 equal, the control differing). `rind-test` passes.
- `std-iface --units` after the module-alias fix: every case passes (71 pure). env.rs moves from 94/96 to 95/96. The other 8
  lines are as recorded, and the known file is re-recorded (5 min 22 s with the dependencies cached).

### Decisions

- The dependencies' names and facts come natively from library/vendor, not from an extension of the helper's names
  (`d12096365`'s decision, applied).
- Each new reading is gated to std and its dependencies (`std-like?`, `dep-crates`, `*raw-block-frags*`), as round 54's globs
  and `#[path]` were, so core's and alloc's checks stand as recorded. alloc's whole pass was rerun at 3,863 of 3,863, and
  core-essential and rind-test pass.

### Left

- The main host's whole std pass on this branch, which gives the new total.
- The few causes above that are not context: `fs::Metadata`, TAIT and RPIT signatures (backtrace's Inner, stdio's
  `as_mut_buf`).

## As built: RIND9 (a), std's pass: the remaining facts (2026-11-19, queue 417, branch `417-rust-std`)

After queue 400, a narrow run with the context the remaining facts need (24 module files: sys::fs and its unix, fs, sys::pipe
and its unix, sys::fd::unix, sys::process with env, unix, unix::common, cstring_array and unix::unix, process, sys::paths and
its unix, env, backtrace, io::stdio, sys::pal::unix::time, sys::time and its unix, time, os::unix::net::ancillary,
os::linux::process; two rounds) gives **1,128 of 1,132 equal**, 0 differing, 4 missing. ChildStdin/out/err and CommandArgs'
CStringIter lifetime were context alone (400's narrow run lacked sys::pipe and cstring_array). The 4 are std's own:
- **std::fs::Metadata** (`Metadata(fs_imp::FileAttr)`). sys/fs/unix.rs declares FileAttr in a macro call (`cfg_has_statx!
  {{ .. } else { pub struct FileAttr { stat: stat64 } }}`, its else arm on musl), so the index, read unexpanded, lacks it,
  and sys::fs's `pub use imp::{.., FileAttr, ..}` re-exports nothing. The pass's own facts gave unix its FileAttr
  (with-native-names) but the re-exports were taken once, at the setup. Now a later round of std's pass takes the index's
  re-exports and globs again over the names with the round before's macro-made structs (`alloc-check/pass`:
  `with-core-names (with-native-names names prev) index`, once a round, std alone).
- **backtrace's Inner** (`Captured(LazyLock<Capture, LazyResolve>)`, `type LazyResolve = impl FnOnce() -> Capture + ..`) and
  **env's SplitPaths** (`paths_imp::SplitPaths<'a>`, sys/paths/unix.rs' `type SplitPaths<'a> = Map<Split<'a, u8, impl
  FnMut(&u8) -> bool>, impl FnMut(&[u8]) -> PathBuf>`). rustc keeps an alias of an impl Trait as the alias (a free alias),
  which the helper's interface writes as its text (`(text "std::backtrace::helper::LazyResolve")`, `(text
  "std::sys::paths::unix::SplitPaths<'a>")`: tools/rsdump/front.rs' `sty`, its fallback). collect expanded it, the impl
  Trait inside was no fact, and the ADT was not written. Now, under `collect/*free-aliases*` (bound by run-module for std's
  interface pass alone), a path to a local alias holding an impl Trait (`collect/tait-alias?`) lowers to `[:free-alias ALIAS
  ARGS]`; native-iface writes such an alias as `(free-alias)` among its aliases, so a later module's path to it does the
  same; ty->sty writes it as the helper does, `(text "PATH<LIFETIMES>")` (a type argument: no fact).
- **stdio's `as_mut_buf`** (`fn as_mut_buf(&mut self) -> &mut BufReader<impl Read>` in `impl StdinLock<'_>`). ty->sty now
  writes a return-position impl Trait as the helper does, `(opaque "FN::{opaque#N}" ARGS)`: the function's generics at
  their identity, then the lifetimes the opaque type captures (edition 2024: every one in scope), the early ones in order
  and then the signature's late-bound ones (`'^#N`): `[(lt "'_") (lt "'_") (lt "'^#0")]`. native-iface's path-of names the
  OpaqueTy by its def path. std alone; a named late-bound lifetime is not written.

Each change is gated to std (`(= "std" crate)`, `*crate*`, `*free-aliases*` bound only for std's collect-only pass), so
core's, alloc's and the dependencies' readings are unchanged: without the binding no `:free-alias` is made, an `:opaque`
throws as before (no fact), and the round's re-exports are std's pass's alone.

### Results

- The narrow run above, with the fixes: **1,132 of 1,132 equal**, the control differing (3 min, 1.05 GB, two rounds).
- `std-iface --units`: every case passes (77 pure, 6 new: the round's re-export of a macro-made struct and its negative,
  the free alias at a lifetime and its negative at a type argument, the opaque type and its negative for core and outside a
  signature). io/stdio.rs moves from 166 to 167 of 167 (as_mut_buf, its case); env.rs, now after sys/paths/unix.rs, from 95
  to 96 of 96 (SplitPaths, its case); the new line backtrace.rs is 36 of 36 (Inner, its case and lazy_resolve's negative).
  The known file is re-recorded (12 min 53 s, 0.9 GB).
- std's whole pass with the fixes (two rounds, 56 min 40 s, 1.2 GB): **4,644 of 4,644 equal**, 0 differ, 0 missing (adts
  272, consts 17, fns 2,693, impls 1,632, traits 30; native extras 1,537), the control differing. 400's whole pass (before its
  module-alias fix) was 4,605 equal, 1 differing, 38 missing.
- core: `core-essential` equals its known file (566 equal, 1 equal up to alloc ids, the control differing), with the final
  code (8 min 41 s, 1.5 GB).
- alloc's whole pass not rerun: its code path is unchanged (every change gated to std); 400's 3,863 of 3,863 stands.

### Decisions

- An alias of an impl Trait is kept as the alias and written as the helper's text, for std's pass alone; the helper and its
  pinned records are unchanged (the alternative, a structural form in both writers, would re-record the rust-front-std pack).
- RPIT captures are written by edition 2024's rule in the interface writer (early lifetimes, then anonymous late-bound ones);
  `collect/opaque-captures`, which omits an impl's elided lifetimes, is left as core's checks read it.

### Left

- std's interface is complete against the helper's (4,644 of 4,644). The main host's whole pass and `--units` confirm it on
  amd64.

## As built: S4's alloc and std bodies (2026-11-19, queue 443, branch `443-rust-s4b`)

RUST-90 S4 (C2), after queue 433's first slice (core's bodies for rsi's five samples, `core-export --rsi`, `rsi-build
--native-core`, the per-item map `test/rust/front/s4-items.tsv`). This round takes the next groups of the map toward std: alloc's
and std's generic bodies that the five samples instantiate.

- **`bin/rust-front lib-export --rsi alloc|std [NAME...]`** (`bin/lib/repr-rust/core-build.bash`, `arbace.rust.front.alloc-check
  --export`). The keys are each record's Item instance of the crate, `boxed-{impl#0}-new`. The sample std.rs's own items (crate
  0, named std) are left out. The crate's interface pass runs over every module file (two rounds), cached in
  `.tmp/rust-front/CRATE/native-iface.edn`. Then the owning module files of the keys are built through the 21 passes
  (core-check's `run-module` under `*only-key?*`, closures with their parent). The bodies are written as core-export's entries
  with `:judge :instance` into `.tmp/rust-front/CRATE/native-CRATE-rsi.edn`, with a `.tsv` holding a line per key.
  - core's traits' associated constants come from core's own sources (`core-consts`, `LIB_CORE_CONSTS`, default
    `mem/mod.rs`). std's interface writes none of them, and the bodies name them (`T::IS_ZST`, `<T as
    SizedTypeProperties>::LAYOUT`).
  - Each entry carries the native facts of the crate-private functions its body names (`:fns`, `box_new_uninit`). The run adds
    them to its interface, so their item types print.
- **Judged at the instance, not against built MIR.** There is no built-MIR oracle of alloc or std (433-4 (a), a pack for the
  main host). So an instance-judged body is taken only when its instance equals the record's body, up to -O0's passes (433-2
  (a)). Otherwise the record's body stays, and the difference is printed.
  - A root is judged against its own record body.
  - A callee is judged against the record's bodies of its def (`rec-by-path`): the one whose forms agree. Such a callee is taken
    in place of the record's body of that symbol.
  - `pack-compare` (build-run) drops the record's facts beside the forms: symbols, callees, glue, locations, and a function
    item's def text.
  - It then classifies: `:equal`, `:equal-but-storage`, or `:equal-o0 KINDS`. The last means equal once both bodies go through
    `o0-normalize`, a port of the parts of rustc 1.99.1's -O0 passes that our runtime MIR stops before: RemoveStorageMarkers,
    SimplifyCfg-final (passes' `simplify-cfg`), InstSimplify's `&(*_b)` of _b's type, CopyProp's copies of locals (a move of the
    copy becomes a copy of the source), RemoveZsts' zero-sized assignments, RenameReturnPlace, and SimplifyLocals (debug info's
    places count as uses).
  - A promoted constant's value is not compared, because ours is evaluated at the run. Its text and type are compared.
  - Crossed controls: `control` over every root, and `control-instance` over the instance-judged roots alone. Each must differ.
  - `bin/rust-front pack-compare-test` holds 15 pure cases (`test/rust/front/pack_compare_test.ae`), seven of them negative.
- `rsi-build --native-core` takes alloc's and std's files beside core's (`RF_CORE_NATIVE`, `:`-separated). It prints a summary
  line per crate (roots native or from the pack, the kinds, callees judged) and the instance control.

### Results (run)

The export:

| crate | group | wanted | built | refused: closure types | refused: other |
|---|---|---|---|---|---|
| alloc | collections::btree | 112 | 99 | 12 | 1 (node `{impl#1}::new`, a field) |
| alloc | vec | 49 | 44 | 2 | 3 (method callee x2, `<usize>::MAX` type-relative) |
| alloc | collections::vec_deque | 39 | 39 | 0 | 0 |
| alloc | raw_vec | 33 | 29 | 4 | 0 |
| alloc | string | 22 | 22 | 0 | 0 |
| alloc | rc | 17 | 17 | 0 | 0 |
| alloc | boxed | 16 | 15 | 0 | 1 (`into_vec` on `Box<[T]>`: method callee) |
| alloc | alloc | 15 | 11 | 0 | 4 (extern "Rust" functions: foreign items unresolved) |
| alloc | slice, str, fmt, collections | 23 | 14 | 8 | 1 (str join's pattern) |
| alloc | total | 326 | 290 | 26 | 10 |
| std | collections::hash | 27 | 23 | 2 | 2 (`base::HashMap::with_hasher`: hashbrown's path) |
| std | panicking, panic, hash, rt | 21 | 15 | 6 | 0 |
| std | thread, sys, process, env, f32, f64 | 16 | 15 | 0 | 1 (ElaborateDrops: open drop of :err) |
| std | total | 64 | 53 | 8 | 3 |

hashbrown's 146 defs are not exported yet.

`rsi-build --native-core intrinsics panics std unwind vectors` (one run, RF11_BATCH=1, 0 stub calls) gives the same 4 of 5 own as
433's baseline. std.rs is on the pack: codegen refused, "unsupported const unevaluated". core's RefCell extras carry `(unevaluated
"85" ..)` (core::cell::is_reading, `{impl#53}::new`; read, not bisected).

Library instances taken natively:

| sample | alloc roots native / pack | std roots native / pack | judged callees taken |
|---|---|---|---|
| intrinsics | 2 / 2 | 2 / 0 | 0 of 3 |
| panics | 0 / 6 | 0 / 8 | 0 of 1 |
| std | 7 / 26 | 2 / 4 | 4 of 25 |
| unwind | 0 / 7 | 0 / 6 | 0 of 3 |
| vectors | 7 / 3 | 0 / 7 | 0 of 7 |

That is 24 alloc and std instances native: Vec's Deref and DerefMut, `[T]::sort`, String's `len` and Deref, Rc's Deref,
`f32::powi`, `f64::powi`, `f64::sqrt`, HashMap's `into_iter`, and the callees `as_str`, Rc's `inner` and VecDeque's `len`. Each
instance control differs.

The judge on core's roots, against 433's baseline (the main host's run, every root but 4 differing): intrinsics 12 equal-o0,
3 equal-but-storage, 2 differ; panics 7 and 2; std 1 equal, 2 equal-o0, 5 differ; unwind 2 and 3; vectors 4 equal-o0. That is 31
of 36 explained by -O0's passes.

Why the rest are not taken (run, from the build logs):
- **Trait objects, `(other ":dyn")`, 18.** catch_unwind, downcast and payload.
- **A named constant unevaluated, 9.** `SizedTypeProperties::LAYOUT` and `MAX_SLICE_LEN`. The record carries the evaluated value
  (mono-time CTFE). This is not a -O0 pass.
- **A trait method's function constant not resolved at the instance, 5.** `SpecToString::spec_to_string` where the record has
  `<&str as SpecToString>::spec_to_string`.
- **Unnormalized projections, `(other ":proj")`, 4.** Vec's Index.
- **A crate-private ADT's impl printed `<impl {}>`, 2.** `RawVec::new`, so Vec::new differs.
- **Our PromoteTemps adds a local where rustc reuses the temporary.** `box_new_uninit`, so Box::new_uninit's callee differs.
- **std's HashMap bodies name hashbrown's types**, which the run's interface lacks.
- **Closures**: the export does not carry closure types.

### Left

- hashbrown's export.
- alloc's foreign items (`__rust_alloc`, which needs resolve, lower and collect of foreign functions, and their
  rustc_std_internal_symbol names).
- Closure types in the export.
- Named constants evaluated at the instance.
- Trait method constants resolved at reprint.
- Crate-private ADTs and impls added to the run's interface, as `:fns` are.
- std.rs's codegen refusal (bisect with RF11_KEEP=1).

### 443-4 (b): std.rs back on Arbace's MIR (2026-11-19, the main host's answer)

- **Plan-time refusal.** `native-core-plan` now refuses, at plan time, a native body whose forms (or promoted bodies) hold a
  constant left unevaluated by a local def id, `(unevaluated "85" ..)`. The refusal is a `record`/`record-kept` line naming
  the constant, and the record's body is kept. `rsi-build` prints `refused at plan time (an unevaluated constant of a def id): N:
  NAMES`.
- **`read_via_copy` and `write_via_move`.** After that refusal, std.rs's next codegen refusal was `read_via_copy`, in
  core::mem::replace, an extra of RefCell::borrow. `passes/lower-intrinsics` now lowers both intrinsics as rustc 1.99.1's
  lower_intrinsics.rs does: `_0 = copy (*_1)` and `(*_1) = move _2`, the call becoming a goto, or `unreachable` without a
  target.
- **Run.** `RF11_BATCH=1 bin/rust-front rsi-build --native-core std`: std.rs is own, with 11 of 2,866 bodies ours and 39 bodies
  built natively. Native roots: 7 core, 7 alloc and 2 std, plus 4 judged callees. Three bodies are refused for `(unevaluated
  "85")`: core::cell::is_reading, `{impl#31}::try_borrow_mut` and RefCell::new. Both controls differ.
- **The two runs before.** With the refusal alone, codegen refused `read_via_copy`. Without either change (the five-program
  run), codegen refused `unevaluated`.

## As built: S4's roots through the record's bodies (2026-11-19, queue 448, branch `448-rust-mono`)

433-1 (a), on 443's tip (ca3229eca): every library instance std's program reaches can be a native root, not only those the
collector reaches from the program's own bodies.

- **mono collects through the record's library bodies.** build-run takes each call of the record's library bodies whose callee
  is a library body of the record (an Item instance): its function constant's `[PATH ARG-FORMS]`, that body's def (its DefId's
  path less the crate's hash, `core::fmt::{impl#4}::new`) and symbol (`rec-targets`). `mono/program-facts` resolves each as the
  collector resolves ours: a trait method's call by `resolve-instance` to its impl's method, an inherent function's at its
  constant's arguments. It keeps one only when the instance is that def and mono's own symbol of it is the record's
  (`:rec-items`, `:via :record`). The roots are the collector's items, then these, each record body once. A run prints
  `rec-items: N instances collected through the record's library bodies, of M calls' constants`.
- **core's defs outside rust-core-built's modules.** `bin/rust-front core-export --rsi-outside FILE-REGEX [NAME...]` builds the
  programs' core defs in the module files FILE-REGEX matches (core-check's `CORE_EXPORT_KEYS`: the bodies by key, no oracle).
  Each entry is `:judge :instance` and is judged against the record's instance at the run, as alloc's and std's are (443-1
  (a)). The entries go into `.tmp/rust-front/core/native-core-rsi-outside.edn`, which `rsi-build --native-core` takes beside
  core's file. The record's core bodies are in `rec-by-path` too, so a core callee is judged like alloc's.
- **Constants evaluated at the instance** (`sized-const-eval`). core::mem::SizedTypeProperties' SIZE, ALIGN, ALIGNMENT, IS_ZST,
  MAX_SLICE_LEN and LAYOUT are evaluated from mono's layout of the instance's type, as CTFE evaluates them for the record (mem/mod.rs).
  LAYOUT becomes `(indirect-bytes HEX ALIGN)`: the run makes it an immutable allocation, and pack-compare compares the record's
  `(indirect allocN 0)` by its bytes. core's private `cell::UNUSED` (0) and `swap_nonoverlapping_bytes::CHUNK_SIZE` (8),
  which the export names by a local def id, come from a two-entry table. A constant whose type mono cannot lay out (a closure
  whose body was not built) stays unevaluated, and 443-4 (b) refuses it.
- **LowerIntrinsics.** `offset` becomes BinOp::Offset, `copy_nonoverlapping` the CopyNonOverlapping statement, and
  `aggregate_raw_ptr` a RawPtr aggregate of the destination's pointer type (lower_intrinsics.rs).
- **A body calling drop glue stays the record's.** core::ptr::drop_in_place's `drop_glue::<T>()` is a shim rustc leaves out for a type
  that needs no glue, so it is no item the module defines.
- **codegen's fallback** (check's `emit-mono`). A native body codegen refuses is replaced by the record's body of its symbol. So is
  one that calls an instance of the program's crate no body defines (specialization's `spec_next`, which mono leaves at the trait
  method). A native callee the record has no body of is left out, and its native callers are replaced in turn. Each case is a
  line of `NAME.codegen-fallback.txt`. The layout table includes the record's bodies of the native ones. Before this, one native
  body that codegen refused sent the whole program to the pack.
- `rsi-build` prints per program: core's instance-judged kinds; the library bodies built natively and codegen's fallbacks;
  and the `rec-items` line.
- `bin/rust-front pack-compare-test`: 24 cases, 9 new (MAX_SLICE_LEN, ALIGNMENT, LAYOUT by its bytes, UNUSED; negatives: left
  unevaluated, SIZE for MAX_SLICE_LEN, another type's LAYOUT, an unknown private constant).

### Results (run)

The core export: core-check over 42 module files. 273 keys, 231 bodies exported, all judged at the instance. Two modules stop at
expansion: hash::sip ("no arm of compress matches") and intrinsics ("the derive ConstParamTy is not taken"). The rest of the keys
are refused for closure or opaque types (Iterator::collect, map_fold, for_each).

`RF11_BATCH=1 bin/rust-front rsi-build --native-core intrinsics panics std unwind vectors` (one run at 603b387b6, 0 stub calls):
**5 of 5 own**. Before: 4 of 5 at 443's added8d8e, std alone own at ca3229eca.

| sample | library bodies built natively, before | after | codegen's fallbacks | rec-items (collected / calls' constants) |
|---|---|---|---|---|
| intrinsics | 20 | 303 | 9 | 404 / 514 |
| panics | 10 | 175 | 0 | 280 / 345 |
| std | 39 | 976 | 22 | 1,563 / 2,353 |
| unwind | 7 | 154 | 0 | 247 / 306 |
| vectors | 11 | 639 | 22 | 1,026 / 1,569 |

Every `control` and `control-instance` differs, in all five programs. core's roots against the record in std.rs: 48 equal, 34
equal-but-storage, 430 equal-o0, 320 differ.

The six named natives:
- **Box::new_uninit**: native, except two `dyn` instances in std.rs.
- **f64::sqrt** and **RefCell::new**: native in std.rs.
- **Vec::len**: native 4 of 6 in std.rs and 3 of 5 in vectors. The other instances keep MAX_SLICE_LEN unevaluated.
- **fmt::Arguments::new**: built, but differs from the record at its aggregate's text, `fmt::Arguments::<'_>` against
  `std::fmt::Arguments::<'_>` (the export prints core's paths relative to core).
- **Iterator::collect**: refused at the export (a closure type).

## As built: aggregates' paths at the instance, drop glue no root, num and core_simd exported (2026-11-19, queue 448b, branch `448b-rust-fmt`)

448-4 (b), then 448-4's item 1, on 448's tip (ce1b754ed).

- **An aggregate's ADT text and path at the instance** (build-run's `reprint-agg-adt`). The export prints an aggregate's
  `(adt TEXT KIND FIELDS PATH VARIANT ARGS)` in its own crate: core's own ADTs relative to core (`fmt::Arguments::<'_>`,
  `option::Option::<usize>::Some`), another crate's by its visible path there (alloc's `core::option::Option`). The record
  prints them in the program's crate (`std::fmt::Arguments::<'_>`, `core::fmt::rt::Argument::<'_>`). At the instance the path is
  resolved to its def (the export crate's own first, then as written, by std's names or the interface's ADTs, then through std's
  modules; `*export-crate*` from the entry's def), and TEXT is printed again as the variant's value path at the instance's
  arguments (regions erased) and PATH as the ADT's. A path the interface does not know is left as written.
- **Drop glue is no native-core-plan root.** The collector's drop glue shims and those mono collects through the record's bodies
  (448's `:rec-items` whose def is drop_glue) were roots with no native body (33 in std.rs). They go to `glue-shims` instead (the
  record's shim by its symbol), built natively when the glue is the type's Drop::drop alone; a shim that fails at run time keeps
  the record's.
- **`core-export --rsi-outside` takes a vendored module's file** (portable-simd's core_simd, outside core/src) at its absolute
  path. num/mod.rs: 99 keys, 97 exported (about 6 minutes). core_simd: 43 keys, 40 exported. core_arch (sse2) stops at expansion
  ("no macro static_assert_uimm_bits"). The three exports (448's 42 files, num/mod.rs, core_simd and core_arch) were put in one
  `native-core-rsi-outside.edn`, 368 bodies.

### Results (run)

`RF11_BATCH=1 bin/rust-front rsi-build --native-core intrinsics panics std unwind vectors` (one run at e6f717317, 0 stub calls):
5 of 5 own. Before is 448's run (603b387b6).

| sample | library bodies built natively | core roots native / from the pack | roots with no native body | core judged at the instance: equal-o0 / differs |
|---|---|---|---|---|
| intrinsics | 303 → 359 | 287 / 516 → 342 / 403 | 105 → 44 | 14 / 35 → 69 / 37 |
| panics | 175 → 194 | 158 / 399 → 172 / 353 | 49 → 23 | 8 / 16 → 22 / 14 |
| std | 976 → 1,046 | 914 / 2,187 → 975 / 2,026 | 332 → 242 | 57 / 71 → 114 / 66 |
| unwind | 154 → 175 | 134 / 357 → 152 / 308 | 47 → 23 | 7 / 21 → 25 / 16 |
| vectors | 639 → 724 | 603 / 1,511 → 684 / 1,336 | 282 → 162 | 42 / 38 → 113 / 48 |

- fmt::Arguments::new: its 38 roots in the five programs differed at the aggregate's text; all 38 are now equal-o0 and native.
- core::num's methods built natively: 47, 9, 39, 9 and 27. core_simd's: 128 in vectors (11 roots still lack a body:
  SimdPartialEq::simd_eq, Mask::resize).
- Drop glue: no root lacks a body any more. Only unwind's shim is simple enough to build natively; the rest stay the record's
  shims.
- Every `control` and `control-instance` differs, in all five programs. A scratch probe of `reprint` in swizzle1's interface
  printed core's, alloc's and the hidden `fmt::rt` paths as the record does, and its negative control (an unknown path) was left
  as written.

## As built: S4's trait objects and projections at the instance (2026-11-19, branch `n-rust-dyn`)

448-4 (a)'s items 2 and 4, on 448's tip (ce1b754ed), split from the Mac VM's items 1 and (b): the instance's forms write a trait
object, a library projection is normalized at the instance, and a trait method's call in a native body is followed to the impl's
method.

- **Trait objects** (`build/ty-form`): `[:dyn ...]` is written `(ty "dyn std::fmt::Debug" (dyn))`, the helper's form (its
  principal, projections and auto traits in the text alone), no longer `(other ":dyn")`, which native-core-plan refused. mono's
  `form->ty` reads a native `(dyn)` by its `:tc`. An unsizing cast in a native body that mono's vtable facts do not name takes
  the record's cast of the same target type in the record's body of its symbol, with its vtable's allocations
  (`runtime-body`'s `fix-cast`; never from a trait object: a coercion between trait objects keeps its metadata, codegen's).
  pack-compare does not compare a cast's `:vtable` (`rec-extras`).
- **Projections** (`reprint`'s `instance-normalize`): after the builtin ones (`Pointee::Metadata`), a projection left in an
  instance's type is normalized by the run's solver (`mono/normalize-ty`: typeck's deep-normalize over std's interface, as
  `instantiate_mir_and_normalize_erasing_regions` normalizes), bound in the run as `*normalize-ty*`; one it leaves stays
  `(other ":proj")` and is refused.
- **Trait-method callees** (native-core-plan's closure): a native body's call of a trait method, concrete at the instance
  (`<[T] as SliceIndex<..>>::get_unchecked`), is followed to the impl's method (`mono/resolve-call`, the collector's
  `resolve-instance`; `*resolve-call*`), so the closure reaches its native body; before, "no native body of
  core::slice::index::SliceIndex::get_unchecked" stopped the root once its projection was normalized.
- `bin/rust-front pack-compare-test`: 28 cases, 4 new (a cast's record `:vtable` not compared; a library projection normalized by
  the run's solver; negatives: a cast to another trait object, the projection left without the solver).

### Results (run)

Both runs on the main host, the same exports (core's `native-core-rsi.edn` of 433's run, `--rsi-outside` over 448's 42 files: 231
bodies, `lib-export --rsi alloc`: 290, `std`: 53) and the same core-attrs file: before at ce1b754ed (448's numbers reproduced
exactly), after at this branch. `RF11_BATCH=1 RF11_KEEP=1 bin/rust-front rsi-build --native-core intrinsics panics std unwind
vectors`: 5 of 5 own in both, 0 stub calls, every `control` and `control-instance` differing.

| sample | library bodies built natively, before | after | codegen's fallbacks, before / after | `native` lines, before / after |
|---|---|---|---|---|
| intrinsics | 303 | 314 | 9 / 12 | 607 / 626 |
| panics | 175 | 202 | 0 / 4 | 347 / 398 |
| std | 976 | 1,113 | 22 / 23 | 1,960 / 2,227 |
| unwind | 154 | 179 | 0 / 1 | 303 / 349 |
| vectors | 639 | 698 | 22 / 22 | 1,283 / 1,395 |
| **total** | **2,247** | **2,506** | 53 / 62 | |

core's core-check-equal roots by pack-compare kind, before -> after (equal-o0; equal; equal-but-storage): intrinsics 156 -> 156,
13, 10; panics 78 -> 83, 8, 8; std 430 -> 468, 48 -> 49, 34 -> 38; unwind 68 -> 74, 7, 5; vectors 286 -> 288, 38, 20. The roots
that differ grow too (std 320 -> 377): bodies that stopped at the type before now reach the compare.

Refusals left (`a type its forms do not write`, lines of the build-run logs, before -> after): `(other ":dyn")` 0/38/74/26/3 ->
0 in every sample; `(other ":proj")` 12/2/73/2/53 -> 0/0/1/0/0, the one `<&mut F as FnOnce<A>>::call_once` in std.rs (a
closure's FnOnce::Output: item 7's closure types). Now in view: `(other ":closure")` (panics 51, unwind 41, std 18, vectors 6),
whose bodies stopped at the trait object before. codegen's new fallbacks: three unsizing casts with no vtable (a core-check-equal
root taken while its instance differs from the record, so the record's body has no cast of that target: Option::fmt, Result::
unwrap at Box<dyn Any + Send>) and `<dyn Any as Any>::type_id`'s call of `TypeId::of::<dyn Any>`, which no body defines; each
falls back to the record's body.

## As built: crate-private paths, constants and the niche types at the instance (2026-11-19, queue 462, branch `462-rust-items`)

448-4 (a)'s items 3 and 5, on 448b's tip merged with `n-rust-dyn` (4f02371da). Item 7 (closure types in the export) is not done:
see the report's DECISION 462-3.

- **alloc's crate-private impls and traits** (`alloc-check --export`, build-run's `with-extra-iface`). An exported alloc body
  names functions of impls and traits std's interface lacks (`alloc::raw_vec::{impl#2}::ptr`, RawVec's inherent impl;
  `alloc::string::SpecToString::spec_to_string`). The run printed their constants `alloc::raw_vec::<impl {}>::ptr`, where the
  record has `alloc::raw_vec::RawVec::<u8>::ptr`. Each entry now carries the native interface's `:impls` and `:traits` of the
  parents of the functions its body and promoted bodies name, beside 443's `:fns`. The run adds them to its interface where it
  has none, and gives the traits the kind `Trait` and their functions `AssocFn` in std's names, so print-def-path takes the
  qualified path (`<i32 as std::string::SpecToString>::spec_to_string`).
- **No debug assertions where the record has none.** rsi's std build is `test/rust/runtime/std.edn`'s profile,
  `DEBUG_ASSERTIONS false`: the record's alloc and core instances have no `debug_assert!` blocks (VecDeque's `wrap_index`: 4
  blocks against our 12). core-check keeps core's cfg (rust-core-built's flags) by default; `*debug-assertions*` false drops
  `debug_assertions` and `ub_checks` from the cfg for `alloc-check --export` (alloc's and std's rsi exports) and for
  `core-export --rsi-outside` (CORE_EXPORT_KEYS: no oracle, judged at the instance). Their interface caches are kept apart
  (`native-iface-nodebug.edn`).
- **Local constants evaluated at the instance** (DECISION 448-3 (a)). core-check's `export-entry` adds `:consts`: the bodies of
  the crate's constants a body names by its local def id (three levels), built by construct_const and exported. build-run's
  `eval-local-const` reprints such a body at no arguments and evaluates it with `eval-promoted` (a constant's own body; size_of
  and align_of by mono's layout; the constants it names in turn), and `sized-const-eval` writes the value. CONT_MASK,
  USIZE_SIZE, USIZE_BYTES, an integer's MAX, NonZero's BITS and the sort thresholds are now evaluated. A constant whose body
  calls a function (alloc's `ZERO_CAP = unsafe { Cap::new_unchecked(0) }`) stays unevaluated and its body refused, as before.
- **The niche types' range pattern types** (mono's `pattern-field`, `:pat-range`). std's interface writes
  `UsizeNoHighBit(usize is 0..=core::::num::niche_types::UsizeNoHighBit::0::{constant#1})`. mono had no layout for it, so the
  layout of `String`, `Vec` and `RawVec` failed, and with it `size_of::<String>()`, SizedTypeProperties' constants at String
  and their bodies (`core::mem::size_of`, `align_of`). An integer's range pattern type is now laid out as the integer's scalar
  with the pattern's valid range and its niche there. A literal end is read as written. The three anonymous ends std writes come
  from a table read from `library/core/src/num/niche_types.rs` (DECISION 462-2).

### Results (run)

The exports were made afresh at this branch: `lib-export --rsi alloc` (290 bodies, about 25 minutes) and one `core-export
--rsi-outside` over 448b-1's combined regex (415 keys in 56 files, 368 bodies, about 37 minutes). core's `native-core-rsi.edn`
(443's run 2) and std's export are unchanged.

`RF11_BATCH=1 bin/rust-front rsi-build --native-core vectors` (one program, by the coordinator's direction): own, 0 stub calls,
`control` and `control-instance` differing. Before is the five-program run at the merge, 4f02371da, with 448b's exports.

| vectors | before (4f02371da) | after |
|---|---|---|
| library bodies built natively | 787 | 843 |
| alloc roots: differs / equal-o0 / equal-but-storage | 50 / 49 / 2 | 38 / 65 / 33 |
| alloc differs at a function constant's text | 24 | 2 |
| pack-compare lines with `<impl {}>` | 39 | 4 |
| refused at plan time for an unevaluated constant | 32 | 3 |
| core roots native / from the pack | 727 / 1,235 | 735 / 1,172 |
| alloc roots native / from the pack | 51 / 58 | 98 / 51 |

- The before run (all five programs, 5 of 5 own, every control differing) gives the merge's numbers for the main host's
  comparison: library bodies built natively 370, 221, 1,190, 200 and 787; alloc roots differing 12, 25, 141, 21 and 50; refused
  for an unevaluated constant 5, 1, 44, 2 and 32.
- vectors' refusals left: `RawVecInner::new_in` and `{impl#4}::new_in` (ZERO_CAP, a call) and std's `<() as
  Termination>::report` (std's export, not re-made).
- alloc's differences left in vectors are structural (blocks or locals: Vec::set_len 6 blocks against 4, with_capacity_in,
  RawVec::new); the next item.

## C6: the library lowered by Arbace, the plan (write-ahead) (2026-11-20, branch `rust-c6-1120`)

RFP53 (a)'s C6, "the library's non-generic items lowered by Arbace (not std's IR from Cargo, rustc and clang)", RUST-90's S5. Today
every case links std's lowered C, which `bin/rsi runtime-build --ci` makes from std's IR (`bin/rsi std-build`: Cargo, rustc 1.99.1,
clang; 110 units pinned by `test/rust/runtime/ir.sha256`, the oracle pack `rust-std-ir`). C5 (the shim) and C7 (ci on `c/target`)
are own; C1-C3 are the Mac's line (queue 448b-479). This section measures what C6 takes and plans it; the next section builds the
first slice.

### What the five cases take from std's IR (measured)

`bin/rust-front lib-unit census` (`arbace.rust.front.lib-unit`, new: std's IR read as text, 76 s, 1.2 GB): from each sample's IR (the
pack `test/rust/ir/samples.tar.xz`, rustc's `x.ll` and `shim.ll`: its declares and the globals it names, 68 to 262 symbols) the
closure through std's 110 units (a private or internal symbol its unit's own; calls, vtables and constants alike: what can run).
Each reached define is classed by its v0 symbol (v0.rs' grammar decoded natively): an **item** (no instantiating crate: a
non-generic item its crate emits once), a **copy** (an instantiating crate, no generic argument in its path, impl or trait: a
non-generic `#[inline]` item copied into the crate that calls it; approximate, an impl over `[T]` at `u8` reads as one), an
**instance** (generic arguments: a generic definition instantiated in that crate), no_mangle.

| case | defines | items | copies | instances | globals | symbols outside std's IR |
|---|---|---|---|---|---|---|
| intrinsics | 8,356 | 624 | 1,513 | 6,217 | 2,949 | 135 |
| panics | 8,138 | 564 | 1,463 | 6,109 | 2,735 | 98 |
| std | 8,753 | 690 | 1,615 | 6,446 | 3,180 | 109 |
| unwind | 8,151 | 565 | 1,467 | 6,117 | 2,740 | 97 |
| vectors | 8,410 | 630 | 1,531 | 6,247 | 2,990 | 114 |
| **union** | **8,834** | **709** | **1,627** | **6,496** | **3,200** | |

The union by the unit's crate (defines, items, IR lines): std 5,835 / 325 / 164,094; core 1,262 / 275 / 40,135; rustc_demangle 443 /
45 / 19,741; gimli 413 / 11 / 8,579; alloc 383 / 25 / 7,153; miniz_oxide 213 / 8 / 10,564; object 147 / 2; addr2line 73 / 8; memchr
24 / 0; hashbrown 19 / 5; adler2 16 / 2; std_detect 3; panic_abort 2 / 2; libc 1 / 1: **255,692 IR lines**, 32% of std's 27,554
defines. compiler_builtins is not reached (ci gives what the lowered C calls). The symbols outside std's IR are musl's (ci's), the
unwinder's `_Unwind_*`, LLVM's intrinsics and the shim's `__rust_*` (C5).

By the definition's own crate (what the front end builds: an instance's body is its definition's at arguments): core 275 items and
1,176 other definitions, alloc 25 and 379, std 317 and 327, gimli 11 and 368, object 2 and 98, rustc_demangle 45 and 26, addr2line 8
and 49, miniz_oxide 8 and 24, adler2 2 and 6, hashbrown 5 and 1, memchr 0 and 3, libc 1 and 1, and 10 `__rustc` items (std's
`rustc_std_internal_symbol` functions, `__rdl_alloc` and the rest): **3,167 definitions, 709 items**. C2's exports (the five programs'
instances, `core-export --rsi`, `--rsi-outside`, `lib-export --rsi alloc|std` at 448b) already hold 594 of them (core 383, alloc 203,
std 8), none of the items.

**The backtrace part.** 4,647 of the 8,834 defines (152,194 IR lines, 60%) are std's backtrace symbolization: `std::backtrace_rs`'s
instances in std's units (3,306) and the crates gimli, object, addr2line, miniz_oxide, adler2, memchr and rustc_demangle. The default
panic hook reaches them (`std::sys::backtrace::_print_fmt`'s `resolve_frame_unsynchronized`) though no case prints a backtrace
(run-program's `env -i`: no `RUST_BACKTRACE`). Those seven crates have no native interface (RIND9 passed core, alloc and std).
std's own feature `backtrace-trace-only` (library/std/Cargo.toml: "Disable symbolization in backtraces") keeps `feature =
"backtrace"` (the panic note "run with `RUST_BACKTRACE=1`" unchanged) and prints a backtrace's frames as addresses, leaving the
symbolization unreached: DECISION C6-1.

### What the native path has, and what C6 adds

The native path (C2's) builds a library definition's body from source (core-check's module run, `lib-export`'s `alloc-check
--export` at keys), prints it at an instance's arguments in the program's crate (build-run's `core-instance`), takes mono's facts
for it (`mono/core-facts`: symbol, callees, function constants), makes it the runtime body codegen takes (`runtime-body`), and
codegens it into the program's module; it is taken when equal to the record's instance (rustc's MIR of that instance, the pack
`rust-front-rsi`) up to -O0's passes (`pack-compare`), else the record's body (per body). A non-generic item is the same
machinery with another selection: the definition at its identity (no arguments), as its own crate's unit compiles it (the item's
symbol and the symbols of the copies and instances it calls that crate's: mono's local crate the library crate, `mono/*lib-crate*`),
and judged against another oracle: no MIR of std's items is recorded (the records hold the programs' mono items), so the judge is
std's IR itself, the define of the same symbol, after RF3's normalizer (`arbace.rust.back.check`: local names by first
occurrence, symbols by path, constant globals by content, attributes and metadata dropped), at the level the case consumes (LLVM
forms, which rsi's lowering and ci take). The symbols must name the crates as std's IR does: the crate identities are the library
build's (RIND8), read here from the IR's codegen unit names (`alloc.58bfcc358ad62d5f-cgu.0`), the compare's own oracle.

Finding (sent to the coordinator at 14:58): `test/rust/front/std-crates-rsi.edn` (2026-11-09) holds the identities of an older
std build (core `bdd42e1572d1d028`, alloc `86a167d6b6d76ea1`, std `d0152738b0f1000c`: the main checkout's 103-unit `.tmp/rsi/std`
of 2026-11-03, before `129c91ccc`), and the pack `rust-front-rsi` was recorded against it (`CsgisaHMJzJ8a_4core`, 10,094 times in
std's record). The pinned std IR is another build (core `f4c9f03271ecd18b`, alloc `58bfcc358ad62d5f`, std `9a0bbe83fa8f68b1`;
`bin/rust-front std-crates --rsi --check` on a std build at the pin: FAIL). A `--front own` program's `ours.ll` names
`_RNvNtCsgisaHMJzJ8a_4core3fmt5write` where the pinned runtime defines `_RNvNtCsl10jNU3mE6R_4core3fmt5write`: linked against
runtime-ci's library its std references stay undefined (rsi-native, rustc linking the stale sysroot, hides it).

### C4 as it stands in `next` (939037429)

std's interface is built natively and compared complete (core's essential pass 566 equal; alloc 3,863 of 3,863, RIND9 alloc5; std
4,644 of 4,644, queue 417), but nothing that runs a case reads it: build-run's batch (rsi-build) loads the helper's `std-iface.edn`
and `std-names.edn` from the pack `rust-front-std` (`records_front`), the native interface is layered over it only inside the
exports (`cc/with-native-iface`), and std's crates' StableCrateIds come from `std-crates-rsi.edn` (the rmeta's; `mono/stable-crate-id`
is ported but Cargo's `-C metadata` is not, RIND8 undecided). `bin/rsi check --record` writes C4 as "the helper's std-iface and
std-crates-rsi.edn (rustc's rlibs)" for an own case (`arbace/rust/check.ae`'s `write-case-inputs`). C4 is not own; what is left:
the run's switch to the native interface (RIND9 (a)'s last step) and RIND8 (the identities), the latter decided with C6-2.

### The plan (C6-L1 to C6-L5)

| stage | what | size | memory, time | days |
|---|---|---|---|---|
| L0 | the census and the judge; alloc's items (this branch) | 25 items | 1.2 GB census, 2 GB export | done below |
| L1 | every item of the closure at its identity, by crate: core (275: core-export over the items' module files), std (317 + the 10 `__rustc`), alloc (25), hashbrown (5), libc (1), panic_abort (2); each compared with std's IR's define; the item's closure collected natively (mono's collector over the item's body with `*lib-crate*`: its callees' instances, the library collector) | 556 items with C6-1 (b) (core 273, std 250, alloc 25, hashbrown 5, panic_abort 2, libc 1; 709 with (a)) | std's interface pass 57 min / 1.2 GB (cached); core's modules 6-40 min each / 2.3 GB; one process a crate under 2.5 GiB | 4-6 |
| L2 | the copies and instances the items reach, made in the library's units (C2's instance machinery at `*lib-crate*`), judged by the same compare | 3,629 defines over 1,574 definitions with C6-1 (b), 499 of them already exported (8,123 over 2,458, 594 exported, with (a)) | as L1, by crate | 5-7 |
| L3 | (a) only: the seven backtrace crates' interfaces (RIND9's pass) and bodies | 4,647 defines, 7 crates | | 6-9 |
| L4 | the own lowered library: each crate's unit as LLVM forms (codegen), a define not built or not equal taken from std's IR by its symbol and counted (C2's per-body fallback), lowered by rsi's lowering and read by ci as runtime-ci's library, kept by its key (`.tmp/rsi/runtime-own`); `front.tsv`'s column own-std, a case's C6 own when its closure took no define from std's IR | the closure's 4,187 defines and 103,498 IR lines (b), or 8,834 and 255,692 (a); 3,200 globals | runtime-ci's profile scaled to the closure: about 1.5 h / 4-6 GiB, a night under the heavy lock | 3-4 |
| L5 | `bin/rsi check --front own --std own` and the case inputs (RUST-90 S6 with RFP61 (a)) | 5 cases, then the synthetic 67 | an eval record 1-2 h / 3-4 GiB | 2 |

Total with C6-1 (b): **14-19 agent days and two nights**, one more night for the re-pin; with (a): 20-28 days. The score moves only
at L5, and only for a case whose C1-C4 are own too (RFP53 (a)): C4 needs RIND9's switch and the identities (C6-2), C2 and C3 the
Mac's line.

### DECISIONs (C6-1, C6-2)

- **C6-1 The backtrace part.** (a) Keep std's configuration: L3 ports the seven crates' interfaces and builds their 4,647 defines (60%
  of the closure's IR) though no case reaches them at run time. (b) **std built with its own feature `backtrace-trace-only`
  (`test/rust/runtime/std.edn`'s features `panic-unwind backtrace backtrace-trace-only`), the IR re-pinned by `bin/rsi std-build
  --update` (the oracle tier's, the pinned rustc: one night), the samples' IR pack and `rust-front-rsi` recorded again against it, the
  runtime rebuilt (runtime-ci, 4 h / 6 GiB); the closure then 4,187 defines (103,498 IR lines; measured by leaving out the symbols of
  the seven crates and of `backtrace_rs`) in core, alloc, std and their small dependencies, the
  panic note unchanged, a `RUST_BACKTRACE=1` run printing addresses (no case sets it)**; (c) stubs for the unreached symbolization (a
  library that is neither rustc's nor a configuration of it). *Recommended: (b)*: it halves C6 and drops seven crates' interfaces,
  for one re-pin night that RFP47's remap night (`RSI_STD_REMAP`) can share.
- **C6-2 The library's crate identities.** (a) **The own library and the own programs name std's crates by the library build's
  identities as constants of Arbace's target (RIND6 (a), RIND8 (a)), read once from the pinned build's codegen unit names (or its
  rlibs' rmeta) into `std-crates-rsi.edn`, which is made again now from the build at the pin (the stale file is the finding above);
  counted own, as `c/target` is C's**; (b) port Cargo's metadata hash and compute them (Cargo's sources, a fetch: the user's go-ahead);
  (c) leave C4's identities rustc's until a byte-identical std. *Recommended: (a)*; the slice below takes (a)'s reading already, for
  the compare.

## As built: C6's first slice, alloc's items lowered and compared with std's IR (2026-11-20, branch `rust-c6-1120`)

L0 of the plan above, on `next` (939037429). alloc is the slice: 25 items in the five cases' closure (core 275, std 327), all in
modules whose interface pass is proven (RIND9: 3,863 of 3,863).

### What was built

- **`arbace/rust/front/lib_unit.ae`** (`arbace.rust.front.lib-unit`): v0's grammar decoded (a symbol's def path with `{impl#N}`,
  `{closure#N}`, its generic arguments, its instantiating crate; types and constants skipped, back-references followed), std's IR
  scanned as text, the programs' closure through it (`census`), CRATE's items in it as export keys (`keys`), and the judge
  (`compare`): each of our defines against std's IR's define of the same symbol, both after RF3's normalizer
  (`arbace.rust.back.check/normalize`), the entry block's label taken away on ours (std's IR is printed without value names, its
  entry block implicit), a crossed control (one item's forms against another item's IR) that must differ. The census was checked
  against an independent scanner (a scratch script over the same files: the same 8,834 defines, 709 items).
- **`build-run`'s `lib-unit-run`** (`bin/rust-front lib-unit build`): each exported item at its identity (`core-instance` at no
  arguments), mono's facts with the library crate as the local one (**`mono/*lib-crate*`**: `local-crate` gives the library's
  identity, so the item's symbol and those of the copies and instances it calls are the library's; **`instantiating-root`**: the
  instantiating crate is then printed as a back-reference to the path's root, `..4kindB4_`, as v0 writes it when the crate already
  appears), the function constants' symbols of operands too (**`mono/const-fsyms`**: a function reified as a pointer), the
  runtime body (`runtime-body`), then **-O0's passes** our runtime MIR stops before (`o0-normalize`, pack-compare's port:
  SimplifyCfg-final, CopyProp, RemoveZsts, RenameReturnPlace, SimplifyLocals; after runtime-body, which writes the calls' callees and
  locations into the forms; `LIB_UNIT_O0=0` not), the layouts from the typed instance forms (runtime-body's walk drops `:tc`, and
  `form->ty` reads a type from the record's DefId texts alone), caller locations' files as the library build writes them (Cargo's
  trimmed `alloc/src/..`, `LIB_UNIT_SRC_PREFIX` for RFP47's remap). The crate identities are read from the IR's codegen unit names
  (`.tmp/rust-front/lib-unit/crates.edn`), C6-2 (a)'s reading. The environment is a synthetic program's pipeline (`abort`: std's
  interface and names), nothing of it compiled into the unit.
- **`bin/rust-front lib-unit census|keys CRATE|export CRATE|build CRATE|compare CRATE`**, and `lib-export --keys KEYS CRATE OUT`
  (`bin/lib/repr-rust/core-build.bash`: the export at a key list of one's own, beside `--rsi`'s from the records).
- Two fixes the slice found, outside it:
  - **`cfg_attr(PRED, inline(never))`** (expand_derive's `attr-from-items`): the attribute's words were dropped (`Empty` arguments),
    so lower read `#[inline]`: alloc's `capacity_overflow` (and core's `cfg_attr(not(panic = "immediate-abort"), inline(never), ..)`
    panic functions) counted as cross-crate inlinable, its symbol given an instantiating crate std's IR does not have. Now the
    group's words are kept for `inline` as for `derive`. This reaches C2's exports too (their entries' `:attrs`).
  - **A nested function's attributes** (`#[inline] fn rt_error` inside `handle_alloc_error`): core-check's `export-entry` writes
    `:nested-attrs`, the attributes of the functions defined inside the entry's definition; lib-unit-run reads them into
    `mono/*native-attrs*` (C2's run does not read them yet).

### Results

`bin/rust-front lib-unit keys alloc` (76 s, 1.2 GB), `export alloc` (the interface pass, two rounds over alloc's 160 modules, then 12
module files: about 20 minutes first, about 2 minutes with the pass cached; under 2 GB), `build alloc` (39 s, 1.99 GB), `compare
alloc` (65 s, 1.04 GB; alloc's 4 units read by repr/llvm):

| | items |
|---|---|
| exported (built from source through the 21 passes) | **25 of 25** |
| made runtime bodies (symbol, callees, locations) | 20 of 25 |
| **equal to std's IR, define for define** | **7 of 25**: `handle_alloc_error`, `raw_vec::handle_error`, `raw_vec::min_non_zero_cap`, `String::try_reserve`, `FromUtf8Error::into_bytes`, `<String as Clone>::clone`, `<TryReserveErrorKind as Clone>::clone` |
| differ | 6 |
| refused | 12 |
| our symbols equal to std's IR's | 19 of the 20 byte for byte; the 20th, `btree::mem::replace`'s `PanicGuard`'s Drop, prints its parent function `replace` in the type namespace (`Nt`, rustc `Nv`): the fn-local cause below |
| the crossed control (`handle_alloc_error`'s forms against `<TryReserveErrorKind as Clone>::clone`'s IR) | differs, as it must |

Without -O0's passes (`LIB_UNIT_O0=0`) 6 are equal (`min_non_zero_cap` differs at a block SimplifyCfg-final merges). Before the
back-reference, the attributes and the locations' fixes the same run gave 2 equal.

What is left, by cause (each the next work of L1, as it holds for core's and std's items too):
- **A type defined inside a function, or crate-private, unknown to mono** (10): the symbol of an inherent impl of a fn-local
  struct (`BufWriter::flush_buf`'s `BufGuard`, 4; `CString::new`'s fn-local trait `SpecNewImpl`, 1: "v0 of the type [:err]") and
  the layouts of fn-local or crate-private ADTs std's interface lacks (`StringError`, 2; `btree::mem::replace`'s `PanicGuard`, whose
  symbol also names `replace` as a type;
  `btree::node::LeftOrRight`; `BufGuard`'s Drop). The export should carry such ADTs' definitions as it carries crate-private
  functions, impls and traits (443's `:fns`, 462's `:impls`, `:traits`), for mono's layouts and v0.
- **A projection left in a layout** (2): `Cow<'_, str>`'s `<str as ToOwned>::Owned` (`String::from_utf8_lossy`, `From<Cow<str>> for
  String`); the layout table needs the run's normalization (`mono/normalize-ty`) as the instance's forms have it.
- **Caller locations inside a macro expansion** (2): `capacity_overflow`'s `panic!` and `io::Error`'s `From<TryReserveError>`; the
  export's `:call-loc` is the macro definition's place (`core/src/panic.rs:62`, `core/src/macros/mod.rs:52`), rustc's the
  expansion's cause (`alloc/src/raw_vec/mod.rs`); `core-check/externalize` should take the outermost expansion's call site. The
  export also writes the remap prefix twice (`/rustc/COMMIT//rustc/COMMIT/library/..`; `rustc-src-prefix` before a path that has
  it), which C2's native library bodies' panic locations carry.
- **Locals** (4): `_from_vec_unchecked`, `format_inner`, `custom_owner_from_box`, `to_uppercase` differ at their first `alloca`
  (a local's size or order): the locals rustc's -O0 MIR keeps against ours after o0-normalize (pack-compare's `:differs` class
  "blocks or locals", 448's and 462's left items).

### Proposed (C6's next slices)

- **C6-3 The slice's judge in the gate.** (a) **`bin/rust-front lib-unit compare alloc` against a known file of its equal items
  (`test/rust/front/lib-unit-alloc-known.txt`), with the crossed control, as a light step of Rust's crops tier once L1 has grown it
  (about 3 minutes, 2 GiB; its export the night's, kept by key)**; (b) by hand until L4. *Recommended: (a)* at L1's end; not added
  here (no gate run).
- **C6-4 The next slice.** (a) **The four causes above, cheapest first (projections in layouts, caller locations, then fn-local
  and crate-private ADTs in the export), measured on alloc's 25, then core's 275 items (core-export's module run at the items'
  keys)**; (b) core's items first, the causes as they come. *Recommended: (a)*: each cause recurs in core and std.

## Decided (2026-11-20, the user, relayed by the coordinator): C6-1 (a), C6-2 (b); C6-3 (a), C6-4 (a) the main session's

C6-1 (a): std stays as pinned; the seven backtrace crates are ported (L1/L3). C6-2 (b): Cargo's metadata hash is ported, so that
Arbace computes std's StableCrateIds itself; Cargo's source taken from the pinned `rustc-1.99.1-src.tar.xz` (SHA-256 `6451598a..`,
`test/rust/corpus-rustc-1.99.1-ui.edn`'s `:tarball`), its `src/tools/cargo` (cargo 1.99.1, git `5f94df478`), no new pin.

## As built: C6-2, Cargo's metadata hash and std's StableCrateIds computed by Arbace (2026-11-20, branch `rust-c6-1120`)

- **`arbace/rust/front/cargo_meta.ae`** (`arbace.rust.front.cargo-meta`): Cargo's `compute_metadata` ported (cargo 1.99.1,
  `src/compiler/build_runner/compilation_files.rs`): METADATA_VERSION; the package id's stable hash (name, semver 1.0.28's Version,
  the source's kind and its path relative to the workspace or its URL); the features; the profile's comparable tuple (opt-level,
  LTO, codegen backend and units, debuginfo, split-debuginfo, debug assertions, overflow checks, rpath, incremental, panic, strip,
  profile rustflags, trim-paths); the mode; compiler::lto's LTO; the kind's fingerprint; the target's name and kind (crate types);
  `rustc -vV`'s lines (a target unit's without `host:`); `is_std`; then the dependencies' `-C metadata` sorted. The bytes are Rust's
  Hash into rustc-stable-hash 0.1.2's StableSipHasher128 (`write_isize`'s one byte under 0xFF, usize as u64, a str and 0xFF), over
  mono's SipHasher128; `finish` its halves `a * 3 + b`. A crate's StableCrateId is then `mono/stable-crate-id` (StableCrateId::new
  over its `-C metadata`), already ported (RFP23 (c)).
- **`test/rust/front/std-build-units.edn`**: the library build's 36 units (the packages, features, profiles, modes, kinds,
  targets, dependencies, `rustc -vV`), read once from Cargo's `--unit-graph` of `bin/rsi std-build`'s own build (std.edn) by
  `cargo-meta units`; what the unit graph does not write is given there: every unit's trim-paths (`-Ztrim-paths` with the
  profile's `TRIM_PATHS=all`, build scripts' too) and panic_abort's profile rustflags (`-Cpanic=abort`, library/Cargo.toml's
  `[profile.release.package.panic_abort]`). No host path in it.
- **`bin/rsi std-build --units`** (the oracle tier, by hand): the same build with Cargo's unit graph and `-v` (each unit's
  `-C metadata`, `metadata.txt`). **`bin/rust-front std-crates --units`**: that build in `.tmp/rust-front/std-units`, the
  description written, each computed `-C metadata` held to cargo's. **`bin/rust-front std-crates --native`** (no cargo, no
  rustc): the identities computed into `.tmp/rust-front/std-crates-native.edn` and held to `std-crates-rsi.edn`'s.

### Results

- `bin/rust-front std-crates --units` (1 min 19 s, 1.2 GB; cargo 53 s): **31 of 31 units' `-C metadata` equal to cargo -v's** (the
  26 target libraries and the 5 build scripts' compiles; the 5 build-script runs invoke no rustc), and the StableCrateIds of the
  pinned std build: **core `f4c9f03271ecd18b`, alloc `58bfcc358ad62d5f`, std `9a0bbe83fa8f68b1`** (`-C metadata` `ffe947da80e854e5`,
  `94a1e9115eaca776`, `a2511a1c84299cbe`). On the way: trim-paths left out of build scripts' profiles gave 5 build scripts and
  every unit depending on one wrong; panic_abort's profile rustflags left out gave panic_abort, std and their dependents wrong.
- `bin/rust-front std-crates --native` (seconds): against `next`'s stale `std-crates-rsi.edn` FAIL (the finding of 14:58); against
  the file as rust-rec-1120 rewrote it from the pinned build's rmeta (taken here byte for byte, with its re-recorded
  `rust-front-rsi` pack `4159566a..`, `test/rust/records/rust-front-rsi.{sha256,tsv}`): equal.
- Coordination: `std-crates-rsi.edn` and the pack's two record files are rust-rec-1120's content; this branch carries them
  byte for byte (sha256 of the edn `64b88365cff1a3b3..`), so either merge order is clean; rust-rec-1120's commit wins if they
  ever differ (the pack is recorded at that file's key).

### DECISION C6-5 (for the user): where the library build's units come from

The hash is Arbace's; its input, the 36 units, is today read from Cargo's `--unit-graph` (Cargo's resolver over library's
manifests, Cargo.lock and the vendored crates, features unified, profiles merged, build scripts' units added).
- (a) **The units are the library build's description, data like `std.edn` (Arbace's library build is described by it; made
  once by `std-crates --units` and held to Cargo's in the oracle tier): C4's identities counted own on (a)**. Cost: none more.
- (b) Port Cargo's resolution of the library workspace too: a TOML reader, the manifests (dependencies, target-specific ones
  under `cfg` against the target's cfgs, `[patch]`, source replacement, workspace inheritance), Cargo.lock, the feature resolver
  (v2), the profiles' merge (manifest, `--config`, environment, build-override), the build scripts' units, compiler::lto. About
  5-8 agent days.
- (c) (a) now, (b) when a second library build (another target or profile) needs another description.
*Recommended: (a)*, then (c)'s (b) only if a second description is needed: the description is a build's configuration, the hash
was the toolchain's part. Not decided here: C4's own count waits on it.

## As built: C6-4 (a), alloc's causes (2026-11-20, branch `rust-c6-1120`)

C6-4 (a) as decided: the slice's four causes on alloc's 25 items, cheapest first. `bin/rust-front lib-unit export|build|compare alloc`
after each (the export about 2 minutes with alloc's interface cached; build and compare under 2 GB):

| | equal | differ | refused |
|---|---|---|---|
| the first slice (above) | 7 | 6 | 12 |
| + projections in layouts | 7 | 7 | 11 |
| + caller locations at the expansion's cause | 8 | 6 | 11 |
| + a root-file call's own place; crate-private ADTs in the export | 9 | 6 | 10 |
| + items defined inside a function (their ADTs, impls and enclosing functions) | 16 | 7 | 2 |
| + a drop's glue (drop_glue::<T> of the dropped local's type) | **18** | 5 | 2 |

- **Projections in layouts** (`mono/layout-of*`'s `:proj`): a projection an ADT's field leaves at the instance is normalized by the
  solver, as layout_of normalizes its type: `Cow<'_, str>`'s `<str as ToOwned>::Owned` is `String`. `From<Cow<str>> for String`
  reaches the compare (it differs at a block, below); `String::from_utf8_lossy` reaches codegen and stops at a constant left
  unevaluated (`REPLACEMENT`'s text).
- **Caller locations** (`ex/virtual-cause`, `ex/root-span?`, core-check's `externalize`): a call made by an expansion takes the
  expansion's root call site in the body's own file (ExpnId::expansion_cause, span_as_caller_location's topmost: the token's mark,
  its recorded site), a call written in the body's own file its own place; a macro's file already remapped is no longer prefixed
  twice (`/rustc/COMMIT//rustc/COMMIT/library/..` before). `capacity_overflow` (`alloc/src/raw_vec/mod.rs:28:5`) and `From<
  TryReserveError> for io::Error` (`Into::into`'s location) are equal. This reaches C2's exports too (their native bodies' panic
  locations, the record's before where none was written).
- **Crate-private ADTs** (`alloc-check --export`'s `:adts`, build-run's `with-extra-iface`): an entry carries the native facts of the
  crate-private ADTs its types name, which the run's interface takes beside 443's functions and 462's impls and traits:
  `btree::node::LeftOrRight` lays out, `splitpoint` reaches the compare.

- **Items defined inside a function** (core-check's `native-iface`: such an ADT named by its def path, `body-path`, as it already
  named their impls; `alloc-check --export`: an entry's own impl among the parents whose facts it carries, and the native facts of
  the functions enclosing its definition and the ADTs it names, so that v0 prints `replace` and `flush_buf` in the value namespace):
  `BufWriter::flush_buf`'s `BufGuard` (its four methods and its Drop), `btree::mem::replace`'s `PanicGuard`, `Box<dyn Error>::from`'s
  `StringError` (Display and Debug) build; 9 -> **16 of 25 equal**. alloc's interface pass made again for it (about 20 minutes).

- **A drop's glue** (runtime-body's `:drop-glue` fact, `mono/drop-glue-symbol`): runtime-body wrote every drop terminator without
  its glue (`(drop P T U r d nil)`), and codegen takes a drop without glue for a plain branch: Arbace's bodies dropped nothing (no
  drop_glue call, no landing pad), the library's as well as the programs' own (found here; sent to the coordinator). The unit gives
  each drop drop_glue::<T> of the dropped local's type when it needs drop: `CString::_from_vec_unchecked` and `format_inner` (their
  invokes and landing pads) are equal, 16 -> **18 of 25**. The programs' run (build-run's native-run) takes it only with
  `RF_DROP_GLUE=1`: with it unwind.rs's `deep` drops its guard (invoke, landing pad), still own in rsi-build, but its native-std
  run (`bin/rust-front rsi-native`, rustc linking the pinned std's rlibs) faults (status 139) at the second catch_unwind, where
  without it the run ends as rustc's but for the missing drop lines; the ci leg (runtime-ci) is the judge before it is the default.

Left on alloc after the drop glue (7 of 25): 5 differ structurally (`custom_owner_from_box`'s first `alloca`, `to_uppercase` at its
31st line, three where rustc's -O0 CFG returns a block earlier), 2 refused (a fn-local trait's impl symbol, a fn-local `&str`
constant).

Left on alloc after the fn-local items (9 of 25): 7 differ structurally (four first `alloca`s: a local's size or order; three where
rustc's -O0 CFG returns a block earlier: `splitpoint`, `From<Cow<str>>`, `BufGuard`'s Drop), 1 refused for a fn-local trait's impl
symbol (`CString::new`'s `SpecNewImpl` for `&[u8]`: v0 of the type), 1 for a fn-local `&str` constant left unevaluated
(`from_utf8_lossy`'s `REPLACEMENT`).

Before the fn-local items, left on alloc (16): 6 differ structurally (four first `alloca`s: a local's size or order; two blocks: `splitpoint`, `From<Cow<str>>`,
rustc's -O0 CFG a block shorter), 5 refused for a fn-local type's symbol (`BufWriter::flush_buf`'s `BufGuard`, `CString::new`'s
`SpecNewImpl`), 4 for a fn-local ADT's layout (`StringError` twice, `PanicGuard`, `BufGuard`'s Drop), 1 a constant unevaluated. The
fn-local items (an item inside a function body) need the interface pass to collect them and v0 to print their parent function in
the value namespace (`Nv`, the `PanicGuard` symbol's `Nt` above): the next cause, then core's 275 items (C6-4 (a)).

### core's 275 items, first measure (C6-4 (a)'s next part)

`bin/rust-front lib-unit keys core` (275 keys), `lib-unit export core` (new: core-check's module run at those keys, `CORE_EXPORT_KEYS`,
over the 49 module files that own them, without debug assertions; 28 min, 2.8 GB: 272 bodies built, 245 exported), `build core`
(2 min 22 s, 2.2 GB: 202 runtime bodies), `compare core` (1 min 41 s, 1.2 GB; core's 16 units, 5 MB of IR):

| | items |
|---|---|
| **equal to std's IR, define for define** | **95 of 275** |
| differ | 79 |
| refused | 100 (30 not exported, 14 at the instance, 13 a crate-private impl's or ADT's v0 path, 13 a callee, 13 a layout, 10 a constant unevaluated, 2 a static, 5 others) |
| our symbol absent from std's IR | 1 |
| the crossed control | differs, as it must |

With core-check's export carrying the crate-private facts too (`cc/with-private-facts`, shared with alloc-check's export; the export
again 28 min): **109 of 275 equal**, 85 differ, 78 refused (30 not exported, 14 at the instance, 12 a layout, 10 a constant
unevaluated, 5 a callee, 2 the static `POW10`, 5 others), 3 symbols absent; the v0 refusals of crate-private impls and ADTs are gone
(223 runtime bodies, 202 before), the control differing.

The 85 that differ lead with one cause: **at -O0 `#[inline(always)]` callees are inlined** (corrected 2026-11-20: by LLVM's
AlwaysInliner, not in MIR. rustc 1.99's rustc_mir_transform `Inline` is off at mir-opt-level 1, `ForceInline` takes
`#[rustc_force_inline]` alone, while rustc_codegen_llvm's `inline_attr` writes `alwaysinline` and LLVM's O0 pipeline,
`buildO0DefaultPipeline`, runs `AlwaysInlinerPass(/*InsertLifetimeIntrinsics=*/false)`, as RF3's oracle found: its `-C
no-prepopulate-passes` keeps it away. std's IR holds no `alwaysinline` function and no `llvm.experimental.noalias.scope.decl`) (std's IR has no call of
`cast_unsigned`, `MaybeUninit::assume_init_ref` or `CStr::from_bytes_with_nul_unchecked`, each `#[inline(always)]`: their bodies
are in the caller; ours calls them), which also moves the callers' locals (the first `alloca`s that differ); then
`const_eval_select`'s runtime function (`cell::panic_already_borrowed::do_panic`'s `runtime`, where ours calls the wrapper). The next
port is LLVM's AlwaysInliner over our LLVM forms (llvm/lib/Transforms/IPO/AlwaysInliner.cpp and Utils/InlineFunction.cpp of the
llvm-project in rustc-1.99.1-src.tar.xz: the callee's static allocas to the caller's entry, the arguments substituted, the
returns joined, an invoke's unwinding given to the inlined calls, the inlined functions removed). (A float constant written as its double's
bits against LLVM's decimal is now compared by value, `canon-fp`: none of the 85 moves by it alone.)

Before it, the largest refusals were alloc's causes not yet carried to core's export: core-check's `export-entry` carries `:nested-attrs` but not
alloc-check's crate-private `:fns`, `:impls`, `:traits` and `:adts` (a `<impl {}>` path, v0 of the type `[:err]`, a missing layout);
then the named constants core's export leaves unevaluated (`U128_MAX_DEC_N`, `char::MAX_LEN_UTF8`) and the static `POW10`.

### std's 325 items, first measure

`lib-unit keys std` (325), `export std` (std's interface pass, two rounds, then the bodies: 1 h 6 min, 2.7 GB; 266 bodies exported),
`build std` (2 min 32 s, 2.3 GB: 172 runtime bodies), `compare std` (1 min 17 s, 1.1 GB, std's units read as fragments): **61 of 325
equal**, 51 differ, 213 refused (120 not exported: the export's build refusals, "a method call without its callee" 33, "a static's
value" 19, inference left open 18, and the 10 `__rustc` items, whose keys the census names by their `__rustc` symbol path; 45 a layout;
14 a callee; 17 constants; 7 an unsizing cast's vtable), the control differing.

| crate | items | equal | differ | refused |
|---|---|---|---|---|
| alloc | 25 | 18 | 5 | 2 |
| core | 275 | 109 | 85 | 78 |
| std | 325 | 61 | 51 | 213 |
| **total** | **625** | **188** | 141 | 293 |

## As built: C4's switch, std's interface from the crates' native passes (2026-11-20, branch `rust-c6-1120`)

- **`alloc-check --assemble OUT CRATE=FILE...`**: std's interface made of the crates' native passes alone, each crate's facts from
  its pass (the helper's shape, or a pass's native map), written in the helper's std-iface shape. **`bin/rust-front
  std-iface-native`**: core's, alloc's and std's passes assembled into `.tmp/rust-front/std-iface-native.edn`, then each crate's
  facts compared with the helper's, fact by fact. **rsi-build `RF_NATIVE_IFACE=1`**: a front directory whose `std-iface.edn` is the
  assembled one and the crates' identities computed (`std-crates --native`); `inputs.tsv` beside `front.tsv` says what the run read,
  and `write-case-inputs` reads it (C4 `:own` only when it begins `own`: none yet, std's names being the helper's).
- **The passes** (each with the crate's own facts taken out of the helper's interface, RIND9 (a)'s pass): std (two rounds, 50 min,
  2.4 GB): **4,644 of 4,644 equal** once the compare ignores `:self-param`, which the native pass writes since 472 and the helper does
  not (331 functions differed by it alone); alloc (two rounds, 20 min, 2 GB; 3,863 of 3,863 at RIND9); core
  (`alloc-check --crate core --pass` over core's 378 module files with core's facts taken out, never run before for the whole
  crate): round 1 alone takes 65 min and 2.4 GB (17,036 functions, 430 ADTs, 258 traits, 10,194 impls, 264 consts collected), its
  second round ran four times slower (about 4 h) and was stopped; the one-round pass is running (`.tmp/c6/core-pass.log`), and
  after it `std-iface-native` and the five samples' rsi-build on the native interface (`.tmp/c6/chains2.sh`): **not measured
  yet**. RF12-P7's core-check pass (`CORE_IFACE_FILES=all CORE_IFACE_ONLY=1`, 53 min) writes only what the helper's interface
  lacks (1,083 functions, 46 ADTs, 69 traits): a supplement, not core's interface, so core's facts had no native replacement
  before this pass.
- **The baseline** (the helper's interface, std-crates-rsi.edn at the pin, `RF_CORE_ATTRS=none`): `RF11_BATCH=1 bin/rust-front
  rsi-build intrinsics panics std unwind vectors` gives 5 of 5 own, 0 stub calls (17 min, 2.25 GB; `.tmp/rust-front/rsi-helper`),
  the run the native interface's is to equal.

### C4's measure (2026-11-20, evening)

- **core's interface pass, the whole crate, first time** (`alloc-check --crate core --pass`, 378 module files, one round: 1 h 25 min,
  3.8 GB under the 8 GiB cap): **25,812 of 27,498 facts equal**, 474 differ, 1,212 missing, the control differing. Missing: 912
  functions (core_simd's `to_bytes` 366, `vendor` 60, `ops` 40; `intrinsics::simd` 78; `num` 72; `hash::sip` 33, whose module stops at
  expansion: "no arm of compress matches"; `pat` 14), 234 impls, 40 ADTs, 25 constants. Differing: functions' `:const` 155,
  `:predicates` 65, `:output` 45, `:promotable` 29, `:nounwind` 9, `:inputs` 2 (`fmt::Arguments::new`'s among them); impls'
  `:predicates` 109, `:items` 17; ADTs' `:variants` 10, `:repr`/`:discrs` 5; traits' `:predicates` 12. alloc 3,863 of 3,863 and std
  4,644 of 4,644 in the same assembly.
- **The switch's run** (`RF_NATIVE_IFACE=1 RF11_BATCH=1 bin/rust-front rsi-build intrinsics panics std unwind vectors`, 16 min, 2.2
  GB): **0 of 5 own**, against the helper's 5 of 5: each program's `main` takes the pack ("no callee for
  `std::fmt::Arguments::<'_>::new::<13, _>`": `format_args!`'s constructor's inputs, one of core's differing facts, leave a type
  variable), then codegen refuses a Transmute between layouts the native interface gives differently. `inputs.tsv` writes C4 as
  "std's interface from the crates' native passes, the StableCrateIds by Cargo's metadata hash ported (std-names.edn the helper's)".
- So C4 needs core's interface as RIND9 gave alloc's and std's: the 1,686 facts above, cheapest first (the `:const` flag of trait
  methods, the functions' `:promotable`, the expansion of `hash::sip` and core_simd's macros), each a module run with its known
  file; then the switch's run equal to the helper's. C6-5 (the 36 units) is the user's in the morning: C4 counted own only after it.

### DECISION C4-1 (for the user): std's names

A run reads two files of std's from the helper: `std-iface.edn` (the interface, now assembled natively) and `std-names.edn` (std's
names and macros for expansion and resolution: 532 modules, 97 macros' definitions, 215 lang items, 1,078 generics lists, 215
aliases, 228 traits' supertraits, 77 hidden items). RFP53 (a) counts a case own when it consumes no product of a toolchain.
- (a) **std's names assembled natively too (the crates' indexes give the modules and the macros, the native passes the generics,
  aliases and traits, the lowered `#[lang]` attributes the lang items), compared fact by fact with the helper's, then the run's
  switch: C4 own only then**. About 1-2 days.
- (b) Count C4 own on the interface and the identities alone (C4's words name std-iface and std-crates-rsi.edn), std's names left
  the helper's.
*Recommended: (a)*: the run's resolution and expansion read std-names as much as typeck reads std-iface.

## As built: C4-1 (a), std's names assembled from the crates' sources (2026-11-20, branch `rust-c6-1120`)

The user's C4-1 (a): std's names (the helper's `--std-names` file: rustc's `module_children` of every public module reached from
core's, alloc's and std's roots, the bang macros' definitions, the reached types' generics, the aliases' types, the traits'
supertraits, the lang items, the `#[doc(hidden)]` modules and re-exports; tools/rsdump/front.rs' `std_names`) made by Arbace.

- **`arbace.rust.front.std-names`** (`bin/rust-front std-names-native`): `scan` expands each module file of a crate with the native
  expansion (the crate's own macros, its dependencies' from the names assembled before it, a vendored module's own:
  `vendored-macros`, stdarch's core_arch) and reads the expanded tree as rustc_resolve's build_reduced_graph does: each item with its
  namespace, def path, kind and visibility (a struct's constructor narrowed by a private field or `#[non_exhaustive]`), the imports
  with their visibility, a `#[macro_export]` macro_rules! the crate root's child and every macro_rules! its module's textual
  binding, the macros' definitions (their tokens with the remapped spans), the lang items (an impl item's `{impl#N}` by the
  expansion's definition order, `:rf-order`; `#[panic_handler]` the lang item panic_impl), the traits' `#[rustc_paren_sugar]` and
  `auto`, the aliases' generic parameters; a module whose expansion fails is read unexpanded with its `#[cfg]`s evaluated.
  `assemble` resolves the imports of every crate to a fixpoint (imports.rs: single imports per namespace, globs below explicit
  names, the binding's visibility the narrower of the import's and the item's), walks the reached modules in the helper's order
  (roots alloc, core, std; depth first; each module once) and writes the forms, the generics and supertraits from the native
  interface (`std-iface-native.edn`), the aliases from the passes' native maps. `compare` sets each kind's facts against the
  helper's. A crate is scanned in parts (its module files split in order), at most two at a time on this host.
- **rsi-build `RF_NATIVE_NAMES=FILE`**: with or without `RF_NATIVE_IFACE=1`, the run's front directory takes FILE as its
  `std-names.edn`; `inputs.tsv`'s C4 words say what the run read, without "own" until the user rules on C6-5.

### The measure (2026-11-20, night)

The four crates scanned (core in five parts: 23 to 25 min each, 2.2 to 2.8 GB; alloc 1 min, std_detect 22 s, std 2 parts of 1.5
min), assembled (1 min) and compared with the helper's (the pack rust-front-std's `std-names.edn`): **14,874 of 15,162 facts
equal**, none extra:

| kind | facts | equal | differ | missing |
|---|---|---|---|---|
| modules | 532 | 532 | 0 | 0 |
| modules' children | 12,623 | 12,623 | 0 | 0 |
| generics | 1,078 | 1,028 | 1 | 49 |
| aliases | 215 | 0 | 0 | 215 |
| traits' supertraits | 228 | 215 | 13 | 0 |
| lang items | 215 | 215 | 0 | 0 |
| hidden, hidden re-exports | 77 | 77 | 0 | 0 |
| macros' kinds and editions | 97 | 97 | 0 | 0 |
| macros' tokens (spans aside) | 97 | 87 | 10 | 0 |

- The 215 aliases: the native interface passes compute an alias's type only when the names they read lack it (core-check's
  `native-iface`), and the passes so far read the helper's names, which have every public alias. The passes are running again on
  the native names (core one round, then alloc and std two rounds, one at a time; `.tmp/c6/np-chain.sh`), whose native maps then
  give the aliases, and core's, alloc's and std's interfaces made with no helper names at all.
- **The passes on the native names** (no helper names in any pass: each crate's pass on the names the scans give, its
  dependencies' aliases from their passes' native maps assembled in before it; `.tmp/c6/np-chain2.sh`): core (one round, 64 min)
  **25,396 of 27,498** facts equal to the helper's interface (25,812 on the helper's names), alloc (two rounds, 10 min) **3,745 of
  3,863** (all on the helper's names), std (two rounds with libc and hashbrown, 25 min) **4,570 of 4,644**; their aliases then
  in the names: **14,953 of 15,162 facts equal**, aliases 81 of 215 (the 134 missing core_simd's `alias!` ones and core_arch's,
  in modules the expansion does not finish: `asm!`, a non-literal `format_args!`, `the derive ConstParamTy is not taken`).
  The first alloc pass on names with no alias at all lost 386 functions (`fmt::Result` and the like unresolved): hence the order.
- The 49 generics: core_arch's `__m128`..`__m512*` (made by its `types!` macro in modules the native expansion does not expand yet:
  `asm!`, a `format_args!` of a non-literal format; read unexpanded, their macro-made types are absent) and the niche types core's
  interface lacks; `TransmuteFrom`'s const default is the interface's.
- The 13 traits: the auto traits' and the sizedness traits' implied `MetaSized` supertrait, from the interface's predicates (core's
  pass writes it where rustc's `explicit_super_clauses_of` does not; RangePattern the other way): core's interface's to fix.
- The 10 macros: std_detect's macros for other architectures, made by its `features!` macro, whose tokens' contexts differ.
- **The run on the native names alone** (the helper's interface; `RF_NATIVE_NAMES=... RF11_BATCH=1 bin/rust-front rsi-build intrinsics
  panics std unwind vectors`, 15 min, 2.1 GB): **3 of 5 own** (intrinsics, panics, unwind), each program's facts the same as the
  helper-names run's; std's facts the same too but codegen refuses "no layout for `{alias core::fmt::Result}`", and vectors' main
  names `i32x4`: the two missing aliases (std's batch was first lost to the host's out-of-memory kill of 23:00 and rerun alone).
- The first assembly, before the fixes of the narrower import visibility (a re-exported constructor of a private field), the
  textual macro_rules! re-exports (`pub use assert_unsafe_precondition;`), the builtin derives `CoerceShared` and `Reborrow`,
  `#[panic_handler]` and the vendored macros: 14,852.

## C6-4's next slice: LLVM's AlwaysInliner over our forms (write-ahead, 2026-11-20, branch `rust-c6-1120`)

What std's IR is: rustc 1.99's `-O0` codegen, then LLVM's O0 pipeline (`buildO0DefaultPipeline`), whose one transformation of the
code is `AlwaysInlinerPass(/*InsertLifetimeIntrinsics=*/false)`: every call of an `alwaysinline` function with a body in the
module is inlined (rustc_codegen_llvm's `inline_attr`: `#[inline(always)]` and `#[rustc_force_inline]`), and the inlined
function removed when nothing else uses it. Our codegen writes rustc's `-O0` IR before that pipeline (RF3's oracle took `-C
no-prepopulate-passes` for this reason), so every caller of an `#[inline(always)]` callee differs (core's 85 items' lead cause).

The port (the llvm-project in rustc-1.99.1-src.tar.xz, LLVM 23, Apache-2.0 WITH LLVM-exception; LICENSE.md, "LLVM"),
**`arbace.rust.back.inline`**:
- `IPO/AlwaysInliner.cpp`'s `AlwaysInlineImpl`: the module's functions in order, each `alwaysinline` one's call sites inlined
  (the latest first: a use list's order), the function removed when nothing calls it.
- `Utils/InlineFunction.cpp`'s `InlineFunctionImpl`: the callee cloned by `Utils/CloneFunction.cpp`'s `CloneAndPruneIntoFromInst`
  (the arguments mapped to the call's operands, every other name fresh; an instruction of constant operands folded away,
  `ConstantFoldInstruction`'s integer cases; a branch or a switch on a constant cloned as a branch to its one successor; the
  blocks reached alone, in the callee's order; the phis pruned to their predecessors, none: poison; `simplifyInstruction`'s phi
  rule; `ConstantFoldTerminator`'s one-target branch; dead blocks deleted; each unconditional branch to a block of one predecessor
  merged), the callee's used static allocas moved to the caller's entry block's start (unused ones dropped), one block ending in a
  return spliced into the calling block, else the calling block split at the call (the rest to `<callee>.exit`), the returns
  joined (one: its block's code at the exit's head; more: a phi there, of one value that value), the cloned entry block's code
  into the calling block; at an invoke's call site `HandleInlinedLandingPad` (each inlined landingpad given the outer one's clauses
  and cleanup, each inlined call that may unwind made an invoke to the outer landing pad with the rest of its block split off,
  `changeToInvokeAndSplitBasicBlock`; each `resume` a branch to the outer landing pad's body, split once after its landingpad with
  phis for its values and the exception, `LandingPadInliningInfo`; the invoke's edge removed from the landing pad's phis) and a
  branch to the invoke's normal destination after the inlined code; whether a call may unwind from the callees' facts
  (`:can-unwind`; `*nounwind?*`, LLVM's intrinsics never). Not yet: InstSimplify's other rules, the attributes' propagation
  (`AddReturnAttributes`, `AddParamAndFnBasicAttributes`).
- The callees: `mono/inline-always-callees` (each call's resolved instance whose codegen attributes hold `inline(always)`);
  lib-unit-run builds them beside the items, each at its arguments, from any crate's export (`RF_CORE_NATIVE` takes the other
  crates' exports too: core's `#[inline(always)]` items inline into alloc's), and names the ones the export lacks (`lib-unit
  inline wanted DEF`; `bin/rust-front lib-unit inline-keys CRATE` adds their keys for the next export); lib-unit's compare
  codegens them and inlines them into the items before the judge (`LIB_UNIT_INLINE=0`: not), an item calling one not built
  refused by that cause.
- **The pure cases** (`bin/rust-front inline-test`, `test/rust/front/inline_test.ae`): a callee of one block with its alloca, of
  several blocks with one return, a constant argument pruning a branch, two returns joined, two invoke call sites (the callee's
  calls made invokes; the callee's own landing pad resuming into the caller's), the inlined functions dropped, and a crossed
  control: 8 of 8, each against what the host's `opt -passes='default<O0>'` (LLVM 23.1.5, by hand) writes for the same functions
  as text.
- **First measure**: alloc 18 of 25 equal as before (custom_owner_from_box refused for its one wanted callee, `Box::<Custom>::new`);
  core 109 of 275 equal as before, 85 differing become 66 and 20 of them are refused for an `#[inline(always)]` callee core's export
  lacks (32 call sites, 25 keys: `Option::unwrap`, `Result::unwrap`, `NonNull::as_ref`, `MaybeUninit`'s constructors and
  `assume_init*`, `max_size_for_alignment`, the `const_eval_select` runtimes `do_panic`...).
- **core with them** (`lib-unit inline-keys core`, core's export again over 300 keys in 55 module files, 56 min; build; compare):
  **114 of 275 equal** (109 before), 78 differ, 80 refused, 3 symbols absent, the control differing; 34 callees built and inlined,
  3 items refused for a callee still missing (the callees' own `#[inline(always)]` callees: `cast_const`, `ManuallyDrop::new`,
  `MaybeUninit::write`, `intrinsics::unlikely`, wanted for the next export; `dec2flt` and `run_utf8_validation` refused at their
  runtime bodies). The rest of the refusals as before (30 not exported, 21 `[MaybeUninit<u8>; N]` layouts of a named constant's
  length, 10 constants unevaluated).

## C1's drop glue (2026-11-21, branch `rust-c6-1120`; the coordinator's order after rust-rec-1120)

rust-rec-1120's runs with `RF_DROP_GLUE=1` (the native bodies' drops calling their glue, C6-4 (a)) left three faults:

1. **`drop_glue::<dyn Any + Send>` called itself** (unwind's second `catch_unwind`, panics: status 139, a stack overflow).
   Codegen called the glue named for a trait object's place, whose shim drops `*_1`, so itself. **Fixed**: the drop of a trait
   object is virtual as rustc_codegen_ssa's `codegen_drop_terminator` writes it for `ty::Dynamic` (VirtualIndex 0's
   `get_optional_fn`: the vtable's slot 0 loaded, called when not null, an invoke with the drop's unwind). pack-compare-test: the
   glue's forms against std's IR's `drop_glue::<dyn Error + Sync + Send>` (alloc's unit 3), equal; the control, a sized pointee's
   drop calling its glue, differs.
2. **Units name glues they never emit** (`drop_glue::<str::iter::SplitWhitespace>` in std, `<hash_map::Iter<String, u64>>` in
   vectors): rustc's `needs_drop` is false for both (references and zero-sized predicates), ours true by what it could not see: a
   projection left unnormalized (`<IsWhitespace as Pattern>::Searcher`, whose `FnMut` impl for the private `IsWhitespace` the
   helper's interface lacks) and hashbrown's ADTs (no interface holds them). `RF_DROP_GLUE_TRACE=1` prints each glue's unknowns;
   `RF_EXTRA_IFACE` gives the run the crates' native facts where its interface has none (core's whole interface pass: functions,
   ADTs, impls, traits; std's dependencies' hashbrown and libc passes).
3. **A native body's `#[inline]` callee never put in the unit** (`core::char::convert::from_u32_unchecked`): the native
   `char::from_u32_unchecked` named it by the upstream symbol (no unit defines one: an `#[inline]` function is a local copy)
   because its signature was missing (`{no sig ..}`: crate-private, and char/convert.rs outside rust-rec's outside export).
   `RF_EXTRA_IFACE` gives the signature; the outside export now takes every core file and portable-simd (stdarch aside).

The runs at the merged head (503-rust-causes4 merged, `d4268b54b`): the exports re-made (core's own with 479-2: 511 bodies, 452
equal to rustc's built MIR, 18 min; the outside one over every core file and portable-simd: 396 bodies, 57 min; alloc's 317, 4
min; std's 62, 2 min), then `RF_EXTRA_IFACE=core-pass/core-iface.edn:hashbrown:libc RF_DROP_GLUE=1 RF11_BATCH=1 bin/rust-front
rsi-build --native-core std vectors unwind panics` (1 h 42 min): **all four build own** (front.tsv's `own`; std's 1,942 library
bodies built natively, vectors' unit naming 140 drops' glues, each defined). std's unit names no `drop_glue::<SplitWhitespace>`
and calls `core::char::convert::from_u32_unchecked` by its local copy's symbol, which it defines. Their run on ci
(`bin/rsi check --front own --corpus samples`, `RSI_FRONT_DIR`, std's pinned IR from rust-rec-1120's build): the lowered std's
read into ci at four jobs exceeded the 8 GiB cap (6.6 GB in use: out of memory), rerun at one job (`RSI_JOBS=1
RSI_CI_MEMLIMIT=5GiB`; the runtime lowered again at the checkout's key, 94 units, 198 s). **The four run on ci** (no status 139:
the first bug gone; the outputs `.tmp/c6/runs/NAME/{out,err,st}`, each program's node program run on cirun as run-program runs
it), judged against rustc's recorded run:
  - **vectors passes** (status 0, its 498 bytes of stdout rustc's).
  - **unwind**: status 101 and stderr rustc's ("thread 'main' panicked at test/rust/eval/unwind.rs:45:50: overflow of 200"), every
    drop line printed in rustc's order (`drop even`/`drop odd`, `drop a`, `drop v1`, `drop v2`, `drop b`, `drop c`, `drop last`:
    the glue runs), but the hook's lines print `hook: ` empty (187 bytes against 329).
  - **panics**: status 3 (rustc's), the same empty `hook: ` lines and `overflow: Err(None)`, `custom: Err(None)` where rustc
    prints the messages; `any: Err(Some(7))` (a u64 payload) and the rest rustc's (115 bytes against 254).
  - **std**: built, lowered and read, then a stack overflow in the interpreted program (the 60 s budget's `timeout` in the
    check; alone: `fatal error: stack overflow` after the goroutine stack's 1 GB): the unit's call cycles are core's stable sort's
    own (drift sort and quicksort over `String`, over `char`), so a native sort body recursing without end.
- **What the empty hooks and the `None`s are**: `<dyn Any>::downcast_ref::<&str>` and `::<String>` are instantiated in the
  program's unit (rustc's program links std's shared instances instead), and their `TypeId::of::<T>` instances are core's native
  body with its constant `any::TypeId::of::<T>::{constant#0}` left unevaluated (pack-compare: against the record's `(indirect
  allocN 0)`), evaluated at codegen to bytes that are not rustc's type id hash for `&str` or `String` (u64's, the record's, is),
  so the payload's type never matches. Not one of C1's three faults (rust-rec-1120 saw both without the glue, at `7dd5032c0`);
  the next items: TypeId::of's constant evaluated as rustc's `type_id` (the hash of the erased type) and the sort's recursion.

### The passes again after the two fixes (2026-11-21, `.tmp/c6/np-chain3.sh`)

core (one round, 61 min) **25,879 of 27,498** facts equal (traits 182 of 188: the default MetaSized supertrait; fmt::Arguments::new's
input now `rt::Argument<'a>`), better than on the helper's names (25,812). But alloc fell to 3,532 of 3,863 (327 functions
missing), std to 3,789 of 4,644 and the names' aliases to 18 of 215 (14,893 of 15,162 facts): this chain fed core's pass the
names of the chain before, which already held core's aliases, and a pass writes an alias only when the names it reads lack it
(core-check's `native-iface`), so core's map came out without them and alloc's and std's passes ran without `fmt::Result` and the
rest. The order that works is the first chain's (names without the crate's own aliases into its pass); the lasting fix is a pass
writing every alias it computes. The switch run on this chain's interface and names (`RF_NATIVE_IFACE=1 RF_NATIVE_NAMES=...
rsi-build intrinsics panics std unwind vectors`, 12 min): **0 of 5 own** as before, four refused at codegen ("unsupported Transmute
between these layouts": a layout the native interface gives otherwise) and std for `{alias core::fmt::Result}` (the lost alias).

### Next (2026-11-21, consolidating)

1. **TypeId::of's constant as rustc's `type_id`** (rustc_middle's `type_id_hash`: the stable hash of the erased type, with the
   ADTs' DefPathHashes over the computed StableCrateIds): unwind's and panics' hooks and downcasts on ci.
2. **core's stable sort recursing without end** in std.rs's unit (drift sort and quicksort over `String` and `char`): find the
   native body that differs (a pivot, a length, a comparison) against the record's instance.
3. **C6-4's next export**: core's 8 wanted `#[inline(always)]` callees (`cast_const`, `ManuallyDrop::new`, `MaybeUninit::write`,
   `intrinsics::unlikely`...), then alloc's and std's items with the inliner.
4. **C4: core's interface differences** (25,879 of 27,498 on the native names: core_simd's and core_arch's modules unexpanded, the
   functions' `:const`, `:predicates`), then the switch run on the interface and names with no helper input. 503-rust-causes4's open findings, taken only if they block C1: 503-1 (the `[const] BytewiseEq<B>` where clause
exported with no arguments, the collector picking `{impl#4}` for u8), 503-2 (a `btree::mem::replace` instance not collected).

## As built: C6-4's next export, core's lowering as std's IR writes it (2026-11-21/22, branch `rust-c6b-1122`)

From `mac/rust-c6-1120` (`fceea7b4a`), with `mac/512-rust-causes5` (`a5cfe536f`: RemoveNoopLandingPads moved into o0-normalize) and
R3's `rust-c4-1122` (`e3c56d260`: the interface pass's facts, TypeId constants as `inttoptr`) merged. The back end's lowering alone:
what codegen writes for core's items against std's IR (`test/rust/runtime/ir.sha256`), item for item, with the crossed control.

### What was built

- **The build takes core's macro-made attributes** (`lib-unit build`: `RF_CORE_ATTRS`, `bin/rust-front core-attrs`' file, as
  rsi-build gives them): `int_impl!`'s and `uint_impl!`'s methods were attribute-less, so `cast_unsigned`, `wrapping_add`,
  `leading_zeros` were called where std's IR inlines them (`#[inline(always)]`), `checked_ilog10`, `carrying_mul` lacked the
  instantiating crate of an `#[inline]` copy, `ilog2` its `#[track_caller]` location.
- **v0**: a function the export built, or one nested in an exported body, is in the value namespace (`const_panic!`'s
  `do_panic`, the parent of `const_eval_select`'s `runtime`: `NvNvNv`, not `NvNtNv`).
- **codegen as LLVM's IRBuilder and AsmWriter write the IR** (`arbace.rust.back.codegen`): every folded integer constant printed
  signed (binops, shifts, casts, a niche's end: `i32 -2097152`, not `4292870144`); `not` and integer `neg` of a constant folded;
  a float constant in LLVM 23's text (`apfloat-text`: APFloat::toString at precision 6, its first AdjustToPrecision truncating,
  then rounding, the decimal kept only when it reads back in the constant's own semantics, else `f0x` and the bits in the
  constant's own width: `float 1e-4` is `f0x38D1B717`, its six digits `9.99999e-05` reading back as another float); `+qnan`;
  a zero-sized allocation an aligned pointer (`inttoptr (i64 1 to ptr)`, scalar_to_backend); rustc 1.99's
  `try_codegen_const_aggregate_as_immediate` (a constant tuple or ADT of 1, 2, 4, 8 or 16 bytes into memory, one integer
  store: `store i64 4294967296000000009`); a returned pair through the ConstantFolder (`{i64 1, i64 poison}`, a whole constant
  pair a constant); a pair's tag its component at the tag's offset (`Option<(usize, &u32)>`'s niche, the reference at 8);
  an edge into a no-op landing pad no edge for MergingSucc; with `*fewer-names*` (lib-unit's compare: std's build names no
  struct type) an ADT's or a closure's type the literal struct of its fields (mono's `llvm-type-of`, type_of.rs'
  struct_llfields with rustc's padding fillers), and an ADT's pointer components' alignment from `pointee-info-at`
  (rustc_middle's pointee_info_at: `PanicMessage`'s `&Arguments`, a Box made safe, a trait object's vtable at 8).
- **The export** (core-check's `export-entry`): an Assert's span put back on the passes' terminator, so its panic location is
  the source's (bounds checks: `core/src/num/imp/bignum.rs`, not `?`); core-check's native interface writes `:nounwind` from
  `#[rustc_nounwind]` (the same fix as R3's `own-attr?`, R3's kept at the merge): AbortUnwindingCalls makes panic_nounwind's
  calls invoke the terminate block and a call of panic_nounwind_fmt unwind to unreachable.
- **lib-unit's o0-normalize** with pack-compare's facts (UnreachableEnumBranching's inhabited variants, CopyProp's Freeze,
  SingleUseConsts, RemoveZsts' locals), which the items' bodies went without (`if let Some(w) = w`'s `trunc nuw`).
- **ext-attrs** finds a module a macro declares (std's `cfg_select!`'d `sys::alloc::unix`).
- **`bin/rust-front lib-unit export core --add`**: only the keys the export lacks, over their module files, appended (a later
  build's wanted callees without the hour-long whole export).
- **pack-compare-test**'s new cases: the float text (with its control, double 0.1 in decimal), the constant aggregate's store
  (control: a field not constant), the constant pair (control: two insertvalues).

### Results

`bin/rust-front lib-unit export|build|compare core` (and alloc) at each round, against std's IR at the pin, the crossed control
differing every time; the host at load 60 to 117:

| core's 275 items | equal | differ | refused | no symbol |
|---|---|---|---|---|
| before (`rust-c6-1120`, the AlwaysInliner's first slice) | 114 | 78 | 80 | 3 |
| r1: core's macro-made attributes; codegen's constants, float text, zero-sized allocations; v0's value namespace | 134 | 37 | 101 | 3 |
| r2: the export again with the 42 wanted callees' keys (317 keys in 58 module files, 286 bodies; 1 h 8 min, 2.3 GB): :nounwind, the asserts' locations, o0-normalize's facts, the literal types, the pair's tag, the constant aggregates and pairs (61 callees inlined) | 177 | 18 | 77 | 3 |
| r3: `export core --add` of 69 keys (the 35 named-constant items again on R3's collect, the new callees, the refused ones retried: 39 bodies, 7 min), InstSimplify's aggregate rules in the inliner | 179 | 16 | 77 | 3 |
| r3's unit compared again with the callees met bottom-up | 181 | 14 | 77 | 3 |
| r5: `export core --add` of std's wanted core callees (48 keys in 24 module files, 18 bodies; 50 min), built and compared again (73 callees inlined) | 181 | 14 | 77 | 3 |
| r8: pointee_info_at through NonNull's pattern type (io::error's drop through `error_drop`, `from_raw`'s vtable parameter), codegen's no-op pads | **183** | 12 | 77 | 3 |

alloc's 25 items (its export again with `Box::new`'s key, 26 bodies, 1 min 48 s): **21 equal** (18 before; the same at r8), 2 differ
(custom_owner_from_box past its parameters now, to_uppercase's 31st line), 2 refused (a fn-local trait impl's v0, from_utf8_lossy's unevaluated `REPLACEMENT`).
Build 13 min, 3.0 GB (10 min at r1, under GC thrash at GOMEMLIMIT=2000MiB: 3000MiB now); compare 1.5 min, 1.0 GB.

std's 325 items (its interface pass from the cache of 2026-11-20; build 2 to 3 min, 3.0 GB; compare 1 min, 1.1 GB):

| std | equal | differ | refused | no symbol |
|---|---|---|---|---|
| before (`rust-c6-1120`) | 61 | 51 | 213 | 0 |
| rust-c6-1120's unit through this branch's codegen alone | 65 | 47 | 213 | 0 |
| r4: std's export again (271 bodies, 153 refused; 6 min), built on core's r3 export | 80 | 21 | 223 | 1 |
| r5: with the 18 core callees std wanted | 81 | 29 | 214 | 1 |
| r6: std's export again with its entries' callee attributes (`:callee-attrs`: Path::has_root's `#[inline]`) | 82 | 28 | 214 | 1 |
| r8: the same export, the head's build (pointee through pattern types) | **83** | 27 | 214 | 1 |

std's differences are mostly symbols now: 13 of the 28 call a generic instance of core's under std's instantiating crate where
rustc takes core's own (`<[u8]>::is_empty`'s `..B4_`, share-generics' upstream_monomorphizations: core exports the instances it
instantiates; Arbace would need core's whole instance set, L2, to know them without reading std's IR), others call an
`#[inline(always)]` core callee one level deeper than the last `--add` exported (`ptr::without_provenance_mut`), and the trailing
`unreachable` blocks of the rust-c6-1120 unit are gone with 512's o0-normalize. std's refusals: 116 not exported (the export's
build), 30 layouts of std types its interface lacks (`std::fs::File`, the futex `Mutex` and `RwLock`, `FileAttr`), 11 constants
unevaluated, 10 promoted constants, 7 unsizing casts' vtables.

The targeted checks of the shared code at the branch's head: `bin/rust-front essential` passed (its summary
test/rust/front/essential-known.txt's, every control differing, 0 stub calls; 8 min, 1.8 GB), `bin/rust-front core-essential` its known summary (566 equal, the control differing); `bin/rust-front
inline-test` 11 of 11, `pack-compare-test` passed; reap 56 of 56 cells, crops-score 0 problems, records-check 9 pairs 0 problems.

### What is left

- **Not exported** (30 of core's items): the export's module run refuses them (the front end's: R3's line).
- **Array lengths of named constants** (25: fmt::num's `[MaybeUninit<u8>; U64_MAX_DEC_N]`, decimal_seq's `Self::MAX_DIGITS`) and 10
  constants left unevaluated: the export's types were made before R3's collect evaluates `u64::MAX.ilog10() as usize + 1`; r3
  exports them again (`export core --add`).
- **A fn-local item's disambiguator** (slice_index_fail: two `do_panic` in one body, rustc's second `s_8do_panic`; our def paths
  have no disambiguator for fn-local items of one name): the front end's naming.
- **An assert!'s message** (bignum's mul_pow2: `stringify!` of the whole macro text, 4,093 bytes): the expansion's.
- **MIR-level differences after -O0's passes** (the locals rustc keeps or makes SSA: digits_to_dec_str's 16-byte temporaries, a
  debug variable's dead load kept by rustc in from_bytes_with_nul, write_str's extra 1-byte locals): each needs rustc's MIR of the
  item, which no record holds (the judge is std's IR).
- **Share-generics' upstream instances** (13 of std's 28 differing items): std calls core's generic instances under std's
  instantiating crate where rustc takes core's exported one (`<[u8]>::is_empty`'s `..B4_`); Arbace needs core's own instance set
  (L2) to know which core exports, without reading std's IR.
- **The order of two `#[inline(always)]` callees inlined into one item** (their allocas' order at the entry): rustc's module order
  is items_in_deterministic_order's definition spans, which the export does not carry (the bottom-up order stands in).
- (Fixed at r8: an indirect call's and a parameter's pointee alignment through NonNull's pattern type, io::error's drop and
  `from_raw` equal.)

### DECISION 512-1 (a): std's rsi export made again on this host (2026-11-22)

The coordinator's order (512-1 (a), from the Mac's 512: catch_unwind's record lines blamed on the fn-local `Data` missing from
474's export): `mac/512-rust-causes5` (`f8f7ff401`, 512-2) merged first (`842ce3c6a`), then panics, unwind and vectors built
(`RF_EXTRA_IFACE=core-pass/core-iface.edn:hashbrown:libc RF_DROP_GLUE=1 RF11_BATCH=1 bin/rust-front rsi-build --native-core
panics unwind vectors`, 1 h 25 min, 3.2 GB) before and after std's export was made again with its interface cache removed
(`lib-export --rsi std`, both interface rounds, 62 bodies, 2 refused; 40 min, 2.5 GB), `pack-compare-test` passing between (138
checks). Lines of each program's build-run.log (catch_unwind's: the record and record-kept lines and the pack-compare differs
lines naming it):

| program | native | record + record-kept | differs | codegen fallbacks | catch_unwind's record lines | catch_unwind's differs lines |
|---|---|---|---|---|---|---|
| panics, before | 682 | 126 + 19 | 71 | 8 | 30 | 24 |
| unwind, before | 616 | 104 + 17 | 58 | 5 | 25 | 20 |
| vectors, before | 2377 | 632 + 60 | 194 | 21 | 0 | 0 |
| panics, after | 682 | 126 + 19 | 71 | 8 | 30 | 24 |
| unwind, after | 616 | 104 + 17 | 58 | 5 | 25 | 20 |
| vectors, after | 2377 | 632 + 60 | 194 | 21 | 0 | 0 |
| panics, after2 (codegen's no-op pads, `7f59c9ff7`) | 682 | 126 + 19 | 71 | 8 | 30 | 24 |
| unwind, after2 | 616 | 104 + 17 | 58 | 5 | 25 | 20 |
| vectors, after2 | 2377 | 632 + 60 | 194 | 21 | 0 | 0 |

The IR (ours.ll) moved where the lines did not: after the export, 7 of std's functions in vectors gained a personality and a dead
`unreachable` block (panics 35 IR lines, unwind 25, vectors 35); with codegen taking a no-op landing pad for no block (after2, 38
min) the dead `unreachable` blocks fall from 85 to 40 (panics), 68 to 31 (unwind) and 548 to 343 (vectors) against the run before,
and the functions with a personality from 53 to 33, 46 to 32 and 141 to 70 (rustc's x.ll of each program: no dead block).

On this host the export before already carried `Data`'s ADT facts (rust-c6-1120's exports were made again at its merged head with
with-private-facts, `d4268b54b`; each of catch_unwind's three entries holds `:adts {"std::panicking::catch_unwind::Data" ..}`),
and `Data` is printed with its arguments. catch_unwind's lines have another root: the function pointers `do_call` and `do_catch`
are coerced to `fn(*mut Data<..>)` where rustc's are `unsafe fn(*mut Data<..>)` (`local 5: (ty "fn(*mut
std::panicking::catch_unwind::Data<{closure@..}, u8>)" (fnptr)) against (ty "unsafe fn(*mut ..)" ..)`): the intrinsic
`core::intrinsics::catch_unwind<Data>(_try_fn: unsafe fn(*mut Data), ..)` reaches the run through the helper's std-iface.edn
(the pack rust-front-std), whose function pointer types are written `(fnptr INPUTS OUTPUT)` with no safety and no ABI, so the
coercion's target is a safe `fn`. The fix belongs to C4's native interface (a fnptr type's safety and ABI written and read,
then printed `unsafe fn`), R3's line; the re-made export changes no fact of catch_unwind's entries (the 62 entries differ from
the old ones only by `:callee-attrs` and anonymous regions' numbers).

## C1 and C4: the burst's round (2026-11-22, branch `rust-c4-1122`)

From `rust-c6-1120` (`fceea7b4a`) with 512-rust-causes5 merged (`a5cfe536f`, rust-front-essential's RemoveNoopLandingPads).

### C1: why unwind's and panics' hooks were empty, and std.rs's "recursion"

- **TypeId**: the bytes of our `TypeId::of::<&str>` and `::<String>` constants were rustc's hash, byte for byte (the record's
  `(indirect alloc 0)`). The difference was the form: std's IR writes each 8 bytes as a pointer, `<{ ptr inttoptr (i64 A to ptr),
  ptr inttoptr (i64 B to ptr) }>` (rustc_codegen_llvm's `scalar_to_backend` for `GlobalAlloc::TypeId`: the offset as an integer
  cast to a pointer), our codegen wrote `[16 x i8]` bytes. ci folds the lowered C's `(void *)(unsigned long)N` to 32 bits
  sign-extended (arbace/c/build.ae `int-const` wraps a constant of pointer type by `chk/wrap`, whose `int-bits` gives a pointer
  32 signed bits; a run-time cast keeps 64): std's TypeId constants lose their high halves on ci, ours did not, and no payload's
  type matched. u32's and u64's matched because both sides were the program's. Fixed on our side by writing the TypeId
  relocation as rustc does (codegen.ae); **finding for C's crop**: `int-const` should wrap a pointer constant as `t-ulong` (a
  probe `static void *g = (void *)0x123456789abcdef0ul;` reads back `ffffffff9abcdef0` on ci).
- **std.rs**: not core's sort. A traced run (CI_CALLTRACE, the last 20,000 calls) shows the interpreted depth about 30 and
  `MultiCharEqSearcher::next` called again and again past the haystack's end (the BTreeMap's `split_whitespace`): our codegen read
  a scalar pair's niche tag from the pair's first value. `Option<(usize, char)>`'s tag is the char at offset 8; the discriminant
  compared the index with -1, so `None` read as `Some` and the searcher never returned `Done`; ci's goroutine stack overflowed in
  the endless loop. Fixed (`get-discr!`: the component at the tag field's offset, codegen_get_discr's extract_field); two
  functions of std.rs's unit change (`MultiCharEqSearcher::next`, btree's `find_key_index`).

### C4: core's interface pass, the fixes (module runs against the helper's facts)

- **Aliases**: core_check's native-iface writes every alias it computes, not only those the names it reads lack (the chain's
  second core pass read names holding core's aliases and wrote none; alloc's and std's passes then lost `fmt::Result`).
- **Enums**: `:repr` from `#[repr(int)]` and `:discrs` as rustc's AdtDef::discriminants (explicit values evaluated, `-`, `<<`,
  `|`; wrapped to the discriminant type, Display signed); before every native enum was `:repr nil :discrs [0 1 ..]`
  (`Ordering`'s -1 0 1, `AlignmentEnum`'s `#[repr(usize)]` 1 2 4 ..: Alignment a byte).
- **Functions**: a `const trait`'s methods const (the trait's constness lowered); `#[rustc_comptime]` const; `:nounwind` from
  `#[rustc_nounwind]`, `:promotable` from `#[rustc_promotable]` on a const fn.
- **Expansion**: `#[derive(ConstParamTy)]` (rustc_builtin_macros' bounds.rs; core::intrinsics and intrinsics::simd were
  unexpanded: 395 intrinsics missing); a union's `#[derive(Clone)]` (clone.rs' union case, TrivialClone first: escape's impl
  numbers); an expression fragment the next tokens continue (`compress!($state.v0, ..)`, hash::sip); a `#[rustc_builtin_macro]`
  macro_rules! left to the builtin (pat.rs' own `pattern_type`); portable-simd's own macro_rules! in core's table
  (`from_transmute!`, vendor::x86).
- **Types**: pattern types as the helper's text (rustc_ast_lowering's anonymous constants of the field's written ends and of the
  struct's missing ones: `(u8) is 1..=core::::num::niche_types::NonZeroU8Inner::{constant#0}`; NonNull's `(*const T) is !null`;
  all of niche_types' 20 texts equal); a not-yet-known trait's Self kept (`P: bounds::BuiltinDeref`, `Self:
  traits::TruncateTarget<Target>`); `size_of::<Self>()` of a primitive impl, `iN::MAX.ilog10() as usize + 2` lengths; const
  generic values `(const "10")`; fn pointer binder regions `'^^#N`; extern types by their std path; `#[unstable_feature_bound]`'s
  "feature(f128) is enabled"; core's types in const-has-type text through std; a trait's items in definition order.

### C1: the three programs on ci (2026-11-22, 00:41)

rust-c6-1120's C1 units (its RF_DROP_GLUE=1, RF_EXTRA_IFACE, --native-core build, `.tmp/rust-front/c1/NAME/NAME.mono.edn`) made
again through codegen and rsi's lowering with the two fixes (build-run's emit-and-lower alone, `.tmp/c4/c1q`), then `RSI_JOBS=1
RSI_CI_MEMLIMIT=5GiB RSI_STD_DIR=<the main checkout's std at the pin> RSI_FRONT_DIR=.tmp/c4/c1q bin/rsi check --front own --corpus
samples --only {std,unwind,panics}` (2 h, the runtime read into ci at the checkout's key on a loaded host; 6.4 GB): **3 of 3 pass**,
each by rustc's recorded run (status, stdout's and stderr's digests, ref-rustc-1.99.1.edn): std.rs 0 and 1,392 bytes, unwind.rs
101 and 329 bytes, panics.rs 3 and 254 bytes (the hooks' messages and the payloads' downcasts now rustc's). vectors.rs passed before
(rust-c6-1120). The control (std against panics' run)
differs. The six programs built again at this branch's code and recorded follow (below).

### C4: the passes again and the switch run (np5, 2026-11-22)

core's pass (one round, 6,762 s on a host at load 70-140, 3.8 GB) on chain 2's native names, then alloc's and std's (two rounds
each) on the names carrying core's (and alloc's) aliases, the interface assembled and compared with the helper's, std's names
assembled and compared (`.tmp/c4/np5-chain.sh`, 3 h 05 min):

| facts equal to the helper's | before (np3, 2026-11-21) | after (np5) |
|---|---|---|
| core's interface | 25,879 of 27,498 | **27,062 of 27,498** (229 differ, 207 missing) |
| alloc's interface | 3,532 of 3,863 | **3,863 of 3,863** |
| std's interface | 3,789 of 4,644 | **4,644 of 4,644** |
| std's names | 14,893 of 15,162 (aliases 18 of 215) | **15,120 of 15,162** (aliases 207 of 215, generics 1,054 of 1,078) |

The switch run on that interface and those names (`RF_NATIVE_IFACE=1 RF_NATIVE_NAMES=np5/std-names.edn RF_CORE_ATTRS=none
RF11_BATCH=1 bin/rust-front rsi-build intrinsics panics std unwind vectors`, 17 min, 2.2 GB): **5 of 5 own** (0 of 5 before: the
Transmutes from `usize` to `Alignment`, a byte without AlignmentEnum's `#[repr(usize)]`, and std's `fmt::Result` and vectors'
`i32x4` aliases are gone), each program's facts as on the helper's interface. `inputs.tsv` writes C4 as "std's interface from the
crates' native passes, the StableCrateIds by Cargo's metadata hash ported, std's names from the crates' sources (C6-5 pending)":
C4 is not counted own until the user rules on C6-5 (the 36 units as the build's description), so no case input changes. Left in core's 436 facts: the const trait methods marked `#[rustc_non_const_trait_method]` and `#[rustc_comptime]` impl methods
(59, fixed after np5 in the module runs' code, not yet in a chain), core_simd shift_scalar's impl-header `'_` printed `'^#0` (42),
to_bytes' anonymous constant args (66), the Thin trait alias bound (9 + 51), and missing (207): core_simd ops' 40 + 40, num's
`from_*_bytes` 72 (`[u8; size_of::<Self>()]` inputs), io's 9 ADTs and their impls, legacy_int_modules' 24 consts. Also (the module
runs): io::error's and io_slice's
`cfg_select!` `#[path]` modules (module-files reads no macro's arms), core_simd ops' `use super::*` of a private import, to_bytes'
`{ $size * $elems }` written by rustc as its anonymous constant's path, the `Thin` trait alias bound, the `[const]`/`const` bound
clauses (512's ranking, not taken here).

### After np5 (module runs, no chain)

The fixes made after np5 proved on the modules whose facts they move (core's pass over those files alone against the helper's
facts, ALLOC_IFACE_ROUNDS=1): iter::traits::double_ended, any, fmt, fmt::rt, io::error: 456 of 465 equal, 0 differ (the 59
`#[rustc_non_const_trait_method]`/`#[rustc_comptime]` constness facts and create_formatter's object region among them; 9 missing,
io::error's `cfg_select!` modules); num: 1,917 of 1,917 (its 72 `from_*_bytes`/`to_*_bytes` with `[u8; size_of::<Self>()]`, the call
now evaluated in collect's lower-const-arg as rustc's type_of writes it). No chain was run again for them (the user's rule of
2026-11-22): core's count after np5 plus these is at most 27,194 of 27,498 (27,062 + the 59 constness facts + create_formatter + num's 72), not measured whole.

## As built: C6-4, round three: the back end's next causes (2026-11-22, branch `rust-c6c-1122`)

From `mac/next` (`5f59be57d`: rust-c6b-1122 and R3's `e3c56d260`), R3's `rust-c4-1122` merged twice (`152a26cb7`, then its last
head `7bd7b001d` at the coordinator's word: collect's unknown trait Self the same fix on both sides, R3's kept; codegen's pair tag
the same fix, ours kept; pack-compare-test's cases of both, the 512 cases once; R3's stray `core_check.ae.orig` removed). The back
end's lowering against std's IR at the pin, item for item with the crossed control, as rust-c6b's; the user's rule of the day
("avoid redundancy and unnecessary tests"): std's export made again only for the keys a change moves (the 69 backtrace_rs keys,
`lib-export --keys`, merged into the export by key), no whole export.

### What was built

- **The inliner's InstSimplify takes `getelementptr P, 0 -> P`** (`arbace.rust.back.inline`, simplifyGEPInst over the clone): an
  inlined `ptr.add(start)` at a constant 0 is the pointer, so the pair the next callee builds is `insertvalue {ptr, i64} poison,
  ptr %p, 0` as std's IR writes it (trim_right, memrchr_aligned, possibly_round, TwoWaySearcher::new, parse_next_component).
  Checked against the host's `opt -passes='default<O0>'` (LLVM 23.1.5): the clone's zero GEP folded, whether the zero is the
  call's argument or the callee's own constant; the caller's own zero GEP kept (inline-test's c9 and its control c10: 13 of 13).
- **CopyProp's return rule only for an SSA local** (build-run's o0-normalize): rustc 1.99 has no RenameReturnPlace (nrvo.rs is gone,
  DestinationPropagation is mir-opt-level 2); `_0` takes a local's name only through copy_prop.rs' rule, whose local is SSA.
  read_uleb128's `result |= ..` keeps its own alloca, as std's IR (pack-compare-test's case and control).
- **A reference to an unsized struct tail keeps the base's metadata** (codegen's `ref`): a struct whose layout is
  `(memory false)` (BackendRepr::Memory { sized: false }: Path, OsStr, Slice, newtypes over `[u8]`) is unsized though the table
  writes its sized prefix 0; `as_encoded_bytes(&self.inner)` passed nothing where std's IR passes the pair.
- **A closure passed as a scalar pair** takes each half's pointee alignment by its upvars (codegen's pair-align; DebugMap::finish's
  `and_then(.., |_| ..)`: `ptr align 8` on the `&mut Formatter` half).
- **A builtin derive's method is `#[inline]`** (mono's ext-attrs: rustc_builtin_macros' deriving writes `#[inline]` on clone, eq,
  partial_cmp, cmp, hash, fmt, default and Eq's assertion; the impl the sources do not hold, its trait derived on its self type's
  definition): `<Components as Clone>::clone` and `<State as PartialEq>::eq` are `#[inline]` copies with std's instantiating crate,
  as std's IR names them. core-attrs takes `num/niche_types.rs` and `num/imp/fmt.rs` (Nanoseconds' macro-made methods, Formatted's).
- **A repr(C) enum's tag is c_int** (mono's adt-repr: the sources' `#[repr(C)]` of an enum whose interface :repr, the helper's
  `repr().int`, is nil): backtrace's `uw::_Unwind_Reason_Code`, trace_fn's `ret i32`.
- **The module lists carry rustc's file names** (core-check's module-files, `--list`'s fourth column, read-modules): module.rs
  joins a `#[path]` to the declaring file's directory and never resolves `..`, so std's backtrace_rs files are
  `std/src/../../backtrace/src/..` in its spans (Bomb::drop's and stash's panic locations), core's stdarch and portable-simd files
  `core/src/../../stdarch/..`; run-module's root file is that name.
- **A module path is in the type namespace** beside a function of its name (mono's ext-value-ns?, lib-unit's RF_LIB_MODULES: the
  crates' module lists): backtrace's `gimli::mmap` is both a function and a module, `Mmap` std's IR's `..5gimli4mmap4Mmap` under `Nt`.
- **lib-unit's vtables** (build-run's lib-unit-run): each body's unsizing casts to a trait object with mono's vtable-alloc, as
  program-facts gives a program's, which runtime-body's casts take in turn.
- An export entry carries the facts of the functions nested in its body when the crate's pass holds them (custom_owner_from_box's
  generic `drop_box_raw`: alloc's pass cache of 2026-11-20 does not, so the item stays).
- **The always-inliner's order as rustc 1.99.1 and LLVM 23 give it.** rustc 1.99.1's `items_in_deterministic_order`
  (compiler/rustc_middle/src/mono.rs:569-590, read in the pinned `rustc-1.99.1-src.tar.xz`) sorts a codegen unit's items by symbol
  name unless `-Z codegen-source-order` (rust-lang/rust#145358), not by definition span: the callees come to LLVM's AlwaysInliner in
  their symbols' order (rust-c6b's stand-in, callees before callers, retired; `LIB_UNIT_SYMBOL_ORDER=0` keeps it). The inliner takes
  a callee's call sites in its use list's order, newest first (`F.users()`: codegen creates a body's calls in rustc's reverse
  postorder, codegen's `*call-seq*` numbers them `:cseq`; a clone's calls are newer than the caller's own, an invoke made of a cloned
  call newer still): the host's `opt -passes='default<O0>'` over the same modules, with the use lists rustc's codegen makes (newest
  first; a forward reference in a text module reverses them, `uselistorder` puts them back), gives inline-test's c11 (two calls:
  the first site's alloca first) and c12 (through a second callee: the second site's first), both ours (14 of 14). With them std's
  trim_right and core's memrchr_aligned, possibly_round, dragon's format_exact and TwoWaySearcher::new are equal.
- **mono's source reader parses the library as edition 2024** (core's, alloc's and std's Cargo.toml; the files their `#[path]`
  modules take): a 2021 parse refused std's path.rs (`if HAS_PREFIXES && let Some(p) = ..`), so every attribute of path.rs was
  unknown (PathBuf::deref's and From<OsString>'s `#[inline]`, the derives): `EXT_ATTRS_TRACE=1` names such an item.
- **The export's assert spans** (core-check's export-entry): a key two built asserts share, or one the passes renumbered, takes the
  next unused built assert of its message's kind in block order (digits_to_dec_str's first bounds check had no location, `?`).
- **The inliner folds an inlined `inttoptr` of a constant** (ConstantFoldCastInstruction over the clone: 0 `ptr null`, another the
  constant expression; opt's own output for the same module): `without_provenance_mut(0)` inlined is `ptr null`, as std's IR
  (stack_overflow's `null`; inline-test's c13, 15 of 15).
- `export core --add` of the wanted callees (without_provenance, without_provenance_mut, wrapping_offset, ptr::read, align_of) and of
  digits_to_dec_str and digits_to_exp_str again (their old entries taken out): 37 keys asked (the refused ones retried), 7 bodies, 18
  module files, 5 min 51 s, 2.8 GB; std's 69 backtrace_rs keys and `sys::args::unix::imp::init` by `lib-export --keys` (53 bodies,
  merged by key); alloc's `CString::new`'s nested `spec_new_impl_bytes` none (not a key the export can build).

### Results

`bin/rust-front lib-unit build|compare` at each step against std's IR at the pin (`test/rust/runtime/ir.sha256`), the crossed control
differing every time; a compare alone where only the compare's code changed (the inliner, codegen), the units built again where the
build's did:

| core's 275 items | equal | differ | refused | no symbol |
|---|---|---|---|---|
| before (`rust-c6b-1122` r8) | 183 | 12 | 77 | 3 |
| r1 at the merged head: derive attributes, the zero GEP, CopyProp's return rule, closure pairs, repr(C), rustc's file names (core's build 13 min, 3.0 GB) | 185 | 10 | 77 | 3 |
| the callees in their symbols' order (compare) | 187 | 8 | 77 | 3 |
| the call sites newest first, codegen's creation order (compare) | 189 | 6 | 77 | 3 |
| the --add round and vtables in the program's bindings (built again) | **190** | 6 | 76 | 3 |

| std's 325 items | equal | differ | refused | no symbol |
|---|---|---|---|---|
| before (`rust-c6b-1122` r8) | 83 | 27 | 214 | 1 |
| r1 at the merged head | 91 | 20 | 214 | 0 |
| the callees in their symbols' order (compare) | 92 | 19 | 214 | 0 |
| the library parsed as edition 2024 (built again), the call sites newest first | 94 | 17 | 214 | 0 |
| backtrace_rs and init exported again, vtables, built again | 97 | 23 | 205 | 0 |
| an inlined constant inttoptr folded (compare) | **98** | 22 | 205 | 0 |

alloc's 25 items: **21 equal** throughout (2 differ, 2 refused), as before.

std's 22 differing items by cause: **18 share-generics' upstream instances** (format_symbol_name, gimli::mmap's deref, parse_start,
stash's allocate, panic_handler's `get` and two `take_box`, resume_unwind's `take_box`, include_cur_dir, parse_next_component and its
`_back`, trim_left, `_set_extension`, `_print_fmt`, BacktraceLock's `print`, checked_add_duration, find_eh_action,
read_encoded_pointer: each now differs by an instantiating crate alone), StdoutLock's and StderrLock's `write_fmt` (the returned
guard's pointee alignment: the futex `ReentrantLock` std's interface lacks), trace_fn (rustc calls `<&mut dyn FnMut(..) as
FnMut>::call_mut`, our typeck calls through the object; with its repr(C) return now `i32`), hashmap_random_keys (a 9-byte local where
rustc's MIR has 8, MIR-level). alloc's 2: to_uppercase (share-generics: core's `<Chars as IntoIterator>::into_iter`),
custom_owner_from_box (the nested generic `drop_box_raw`'s facts: alloc's pass cache of 2026-11-20 predates queue 498's nested
functions). core's 6: digits_to_dec_str and digits_to_exp_str (the first bounds check's span: the built assert has none, the index
expression's THIR span, the front end's), mul_pow2 (`assert!`'s 4,093-byte `stringify!`), slice_index_fail (a fn-local item's
disambiguator), from_bytes_with_nul and lookup (MIR-level). std's refusals (205): 116 not exported (the export's, R3's line), 48
layouts std's interface lacks (`std::fs::File`, the futex `Mutex`, `RwLock`, `Option<SymbolName>`), 12 promoted and 11 unevaluated
constants (libc's `S_IFDIR` and `UIO_MAXIOV`, std's `HAS_PREFIXES` and anonymous constants), 13 callees mono refuses (the external
opaque `panic_output::{opaque#0}` whose hidden type std's interface does not write, addr2line's and rustc_demangle's unresolved
paths, a builtin Try candidate), 3 `#[inline(always)]` callees not built, 1 symbol, 1 scalar operation; no vtable refused (8 before
the round: lib-unit's casts now take mono's vtables).

**Share-generics, what Arbace would need** (rustc 1.99.1's compiler/rustc_codegen_ssa/src/back/symbol_export.rs, read in the
pinned tarball): a crate's exported generic instances are the generic items of its own whole collection
(`collect_and_partition_mono_items`, external linkage), and an instance several upstream crates export is taken from the one of the
lowest StableCrateId (lines 544-552). core's id (`f4c9..`) is the highest of std's 25 crates (rustc_demangle `1841..`, gimli,
compiler_builtins, addr2line, alloc `58bf..`, hashbrown, libc, std_detect, adler2, memchr, std, miniz_oxide, object, unwind, cfg_if
below it): find_eh_action's `<Result<usize, ()> as Try>::branch` is rustc_demangle's, its `<*const u8>::is_null` alloc's, the rest
core's. Knowing them without reading std's IR is every upstream crate's whole collection (core's roots: 8,359 non-generic functions in
its interface, its 2,486 external defines; alloc's; the deps'), C6's L2 with L3 for the seven backtrace crates unless C6-1 (b) drops
them; not this round.

The targeted checks at the head: `bin/rust-front essential` passed (its known summary, every control differing, 0 stub calls; it
first failed on records staged from rust-c6b's worktree, `.tmp/rust-front/records/stage` copied with its paths, removed and staged
again), `core-essential` its known summary (566 equal, the control differing), inline-test 15 of 15, pack-compare-test passed
(R3's cases and this branch's); reap 56 of 56 cells, crops-score 0 problems, records-check 9 pairs 0 problems.

### At the merged head: the Mac's 524 merged, C6-5 (a)'s record tried

The Mac's 524 (`mac/524-rust-c3` at `26c7532a2`: every mono fact native that needs no new export, the entry made by mono, a source
ADT's repr, a ReifyShim's instantiating crate) merged at the coordinator's word (collect's unknown trait Self R3's again; core-attrs.txt
both sides' files; the docs' and pack-compare-test's blocks each once), so one Rust line holds R3, 524 and this branch. Two more back
end fixes there: lib-unit's core exports collect `ub_checks.rs` beside core-iface-extra.txt's (test/rust/front/lib-unit-iface-extra.txt:
check_language_ub's signature, which copy_nonoverlapping's and offset's precondition tests name; their two entries exported again by
`export core --add`, 6 min), and codegen takes a function item's type zero-sized when the table lacks its text; an inlined call made
an invoke takes its destinations right after its arguments, as codegen writes an invoke. At the merged head (the three builds again):
**core 191 of 275** (core::fmt's `pad` too; bstr's Display now compared, an alloca of MIR's), **alloc 21 of 25**, **std 101 of 325**
(rt::init, interpret_cs_action, ThreadId::as_u64; 25 differ: 21 share-generics' upstream instances, realloc_fallback's and args' alloc
instances among them, two write_fmt, trace_fn, hashmap_random_keys; 199 refused), every crossed control differing;
`bin/rust-front essential` and `core-essential` their known summaries, inline-test 15 of 15, pack-compare-test passed (524's cases
with R3's and this branch's).

R3's own path before the 524 merge (`69683177b`: `RF_EXTRA_IFACE=core-pass:hashbrown:libc RF_DROP_GLUE=1 RF11_BATCH=1 bin/rust-front
rsi-build --native-core panics std unwind`, 1 h 40 min; then `bin/rsi check --front own --corpus samples --only` the three, std's
runtime lowered at this checkout's lowering key first, 12 min, its ci read 55 min): **3 of 3 pass**, the std-vs-panics control
differing (1 h 41 min, 6.2 GB).

C6-5 (a)'s record: R3's switch run (`rsi-native5`, built before R3's C1 fixes: RF_CORE_ATTRS=none, no RF_DROP_GLUE) recorded by `bin/rsi
check --record --front own --corpus samples` with its inputs.tsv's C4 words own: **4 of 6 pass** (intrinsics, panics, vectors; threads
from the pack), std.rs (`cirun: undefined function <alloc::string::String as Ord>::cmp`) and unwind.rs (stdout) not, so rsi refused the
record (nothing written). C4 is recorded own by the first native-interface build with C1's configuration that passes
(`RF_NATIVE_IFACE=1 RF_NATIVE_NAMES=.. RF_EXTRA_IFACE=.. RF_DROP_GLUE=1 bin/rust-front rsi-build --native-core` over the six, then the
record): the next host stream's rsi-build at the merged head can be that one.

## rust-c3x: TypeId::of's constants, C3's exports again, C6-5 (a)'s record, share-generics (2026-11-22, branch `rust-c3x-1122`)

From `mac/next` (`169119776`: R2b's round three, R3's C1/C4 and the Mac's 524 as one line).

### 524-1 (a): TypeId::of's constants (built)

rustc 1.99.1's `TyCtxt::type_id_hash` ported into `arbace.rust.front.mono` (`type-id-hash`; the sources read in the pinned tarball are
named in the journal's entry): the type with its regions erased hashed by StableHash into rustc-stable-hash's SipHasher128, its two
halves the TypeId's 16 bytes, each 8-byte half a pointer into the type's GlobalAlloc::TypeId at the half as offset (write_type_id).
In 1.99 a `Ty` hashes its TyKind directly (no nested fingerprint); a `&List` (generic arguments, a tuple's types) and an AdtDef each
enter as the Fingerprint of their own hash; a DefId as its DefPathHash, computed from the StableCrateIds (C6-2's) down the def path
(DefPathData's discriminant, the name, the disambiguator); an AdtDef's variants, fields, visibilities, flags and ReprOptions (its
field_shuffle_seed from the DefPathHash) read from the ADT's library sources. `build_run`'s inline-const evaluates TypeId::of's
`const { type_id::<T>() }` at the instance with it (runtime-body's fix-static makes the allocation and its two relocations).

| check | result |
|---|---|
| `bin/rust-front type-id-check`: every TypeId::of constant of the pack rust-front-rsi | **6 of 10 equal** (String, &str, u64, u32, i32, core's ParseIntError), 0 differ, 4 refused |
| the crossed control (a computed constant against the next case's recorded bytes) | 6 of 6 differ |
| the eval cases' constants (panics 4, unwind 4, intrinsics' u32) | String, &str, u64, u32: all among the equal |

Refused: a program-local ADT (the synthetic programs' Noisy, AppError, Failure: their def keys are mono's `def-key`, their items the
program's own nodes; not read yet) and an ADT local to an impl's method (alloc's `{impl#17}::from::StringError`: the impl's items are
not walked); arrays (a constant's hash), trait objects, function pointers and closures.

### Share-generics: the question (not built)

R2b's round three: 21 of std's 25 differing items differ only by the crate that instantiates a generic callee. rustc 1.99.1
(compiler/rustc_codegen_ssa/src/back/symbol_export.rs, `upstream_monomorphizations_provider`, lines 500-560 in the pinned tarball)
takes each upstream generic instance from the crates that export it (under share-generics a crate exports the generic instances of
its own whole mono collection), the one of the lowest StableCrateId when several do. Knowing it natively needs each upstream crate's
whole collection, not only the closure std's items reach.

```
DECISION C3X-1: std's units' upstream generic instances (share-generics), 21 of std's 25 differing items and alloc's to_uppercase
- (a) collect each upstream crate's whole mono set natively (mono's collector at *lib-crate* over every root of core, alloc, the small
      dependencies, and since C6-1 (a) the seven backtrace crates: L2 over whole crates, plus L3's interfaces), then rustc's
      lowest-StableCrateId choice: exact and own; core's roots alone are its 8,359 non-generic functions and statics (one process over
      8 GiB likely: under the heavy lock), about 8-12 agent days with L3
- (b) build std's reference without shared generics (test/rust/runtime/std.edn's RUSTFLAGS `-Zshare-generics=no`, the IR re-pinned by
      `bin/rsi std-build --update` in the oracle tier, the pack rust-front-rsi and the samples' IR pack recorded again, the runtime
      rebuilt: one night as C6-1 (b)'s re-pin): every crate then instantiates its own copies, which the units already do, and a
      program's -O0 build finds no upstream instance to share (mono's upstream choice for programs goes too); small code, a
      configuration of rustc's build like C6-1 (b) (recommended)
- (c) read each crate's exported generic symbols from the pinned rlibs' rmeta (exported_symbols), as std-crates-rsi.edn was read before
      C6-2 (b): about a day, exact, but a record of rustc's build (C6 stays rustc's unless counted as the build's description)
```

## As built: C3X-1 (b), std without shared generics (2026-11-23, branch `rust-indep-1123`; WIP)

The user's decision of 2026-11-23: (b). `test/rust/runtime/std.edn`'s RUSTFLAGS take `-Zshare-generics=no`; std's IR re-pinned (120
units, 56 MB; the clang pin the host's 23.1.6, which gives the old flags' 110 units byte for byte); the StableCrateIds unchanged (the
native Cargo metadata hash still equal, 31 of 31 units), the SVHs and rlib names moved (`std-crates-rsi.edn`); the samples' and the
synthetic corpus's IR packs re-made (x.ll moved, shim.ll not); rust-front-rsi's members byte for byte the old (its keys moved). C6's
lib-unit compare against the new IR, the units of rust-c3x: **std 116 of 325 equal** (101), 10 differ (25: the share-generics' 21 gone
but for alloc's to_uppercase), 199 refused; alloc 21 of 25; core not compared again. Left: the lowered std at the new pin, the six cases
built again on the own path (C1 with C4's native interface and names), the eval, synthetic and AOT records; the UI pack where its
sources are (JOURNAL 2026-11-23, rust-indep-1123).

## As built: C6's L1, round 11 (2026-11-24, branch `rust-l1-1124`, WIP)

The constants left by round 10 evaluated by CTFE with their pointers (`eval-const-whole` in build_run: a constant an allocation
read in place, its nested allocations the body's data); a non-generic item's inline consts exported as bodies of their own
(`ITEM::{constant#N}`); statics' padding undef (an init mask through variant-bytes to codegen's runs); v0 of `extern "C"` fn
pointers; fn pointer to integer casts; `extern "C"` pair returns as `{ i64, i64 }` (PassMode::Cast); libc's pointer constants from
source; export opaque types with their hidden types (`:opaques`, untested). std 180 / 39 / 106 -> 180 / 44 / 101 at the first half
(b1), then env_read_lock and Path::components equal in targeted runs; no full run at the head. Left of L1: the doc/JOURNAL.md entry of
2026-11-24 and the worktree's `.tmp/NEXT.md`.

## As built: C6's L4.4 measured, L5's tooling (2026-11-26, queue 596, VM)

On next `3725c6dc8` (590's and 591's own tree: 688 own of 31,684 defines in 23 of 120 units; the host's L4 rerun on `227dce44c`,
73 of 73, its baseline, not measured again).

**L4.4, the closure analysis.** Each case's closure through the own tree (its x.ll's and shim.ll's undefined symbols, as `census`
takes them; `lib-unit own-std`, checked against an independent scanner: the same counts for all 73 cases, and the 5 samples' union
9,718 defines as `census` gives it on the tree today; the 8,834 above were the packs and std of 2026-11-20):

| | defines in the closure | rustc's |
|---|---|---|
| the smallest, `synthetic/abort.rs` (18 more synthetic cases equal: exit-code, the panics, trap, overflow-rem-zero-u64) | 8,935 | **8,299** |
| the largest, `eval/std.rs` | 9,651 | 8,975 |
| shared by all 73 cases | | **8,299** (abort's whole rustc set) |

abort's 8,299 by crate: std 5,697, core 687, rustc_demangle 556, gimli 473, alloc 296, miniz_oxide 270, addr2line 233, adler2 44,
object 40, panic_abort 2, libc 1. Of them lib-unit has judged 109 (std 67 differ and 35 refused, rustc_demangle 5 and gimli 2
differ); **8,190 have never been built** (L1's items and L2's callees of the samples cover 865 defines: 688 equal, 82 differ, 92
refused, 3 without a symbol). So no case gets to C6 own before all of them almost do: the 8,299 are the runtime every case
links (lang_start, the panic and backtrace machinery, std's I/O and thread setup), and the first cases to get there are abort's
19, at 8,935 own defines. Working through the 109 differing and refused defines moves no case; the next work is L2 grown to
the whole closure (each reached instance and copy built by lib-unit, not only the items' direct callees), crate by crate, core
and alloc first (983 of abort's, none built), then std's 5,697, then the backtrace crates (C6-1 (a)).

**L5's tooling.**
- `arbace.rust.front.lib-unit/own-std STD LIST OUT`: a case's C6 (own when no define of its closure is rustc's; STD/own.tsv's own
  rows, none for std's pinned IR); a control with every define marked own gives every case own.
- `bin/rsi check --std own` (RSI_STD, default pinned): RSI_STD_DIR the own tree (RSI_STD_OWN_DIR, else the checkout's or the
  main checkout's `.tmp/rust-front/lib-unit/own`, checked at the pin), RSI_RUNTIME_DIR `.tmp/rsi/runtime-own`, WORK
  `.tmp/rsi-check-own-std`; each corpus's `std.tsv` (ID, own|rustc, why) and `front.tsv`'s fourth column, the own-std column.
  Refused with `--record` until DECISION 596-1, and with RSI_IR=rustc or `--make-pack`.
- `arbace.rust.check/write-case-inputs`: C6 from the run's `std.tsv` (`:own`, else the closure's count), the fixed text without it.

**DECISION 596-1 (how a run on another std is recorded).** (a) **A record of its own, `test/rust/check-own-std-linux-amd64.edn`
with its inputs `eval-inputs-own-std-linux-amd64.edn` (and `-aot-` beside), which crops-score reads for C6 (a case's C6 own
from that record's inputs, the other inputs from the main record's), the main record staying the pinned std's**; (b) the one
record, each row naming its std (`:std :own`), the inputs' C6 from it. *Recommended: (a)*: the two runs are different
configurations of the same cases, a regression of one never hides behind the other, and the pinned record (the gate's, the
matrix's) is untouched by an L4 night.
