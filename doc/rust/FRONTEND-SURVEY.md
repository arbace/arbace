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
  definer `twice<F>`), in the pack `rust-front-built` (1,210 members, `3f84e0a6...`) and `rust-front-captures` (9, `e41dfc4e...`), not
  `rust-front-mono` (its run needs the closures behind opaque types collected: RF11's): through every pass 8 of 8 bodies equal (4 of 8
  before: the alike pair and the generic definer left unrevealed). `closures`' `compose` (a generic definer): `differ` → `but for the
  allocations' ids`. `typeck-check --ours closures collect1`: 493 of 497 facts, unchanged. The essential step's lines move to 8
  programs (built 62 of 62, the passes 60 of 60, capture-check 33 of 34), recorded.
