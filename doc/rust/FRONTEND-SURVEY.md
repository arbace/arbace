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
run beyond the 80 programs of the synthetic corpus and the samples, each process under 2 GiB.

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

`bin/rust-front oracles|corpus` runs them: the synthetic corpus (67 programs), the samples (5 of `test/rust/sample/`, 3 of
`test/rust/eval/`, `p.rs`-like programs not counted) are 80 files, **77 built** (the three left out: `constructs.rs` needs
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
`black_box`, `cold_path` and the inhabitedness assertions. **Not taken yet**: floats, SIMD, `i128` in pairs, `Offset` of
non-scalars, unsized places, virtual calls, most intrinsics (`ctpop`, `ctlz`, `size_of_val`, the saturating ones...), coroutines,
inline assembly.

`arbace.rust.back.collect` (the collector slice, about 100 lines): the mono items of a program from its roots (the entry's
`main` and `lang_start`; for `no_std` programs the functions with plain symbols and the panic handler) by what the bodies use:
callees (the helper's resolved instances), drop glue, function items and pointers (a closure coerced to a pointer too), the
functions and statics of the allocations the constants reach (vtables' methods among them), the panic functions the asserts call, the
personality and terminate functions unwinding needs; after `rustc_monomorphize/src/collector.rs`. **80 of 81 programs' item sets equal
rustc's** (`bin/rust-front collect`; the one miss, `iterators`, lacks one function item, `min_by_key::compare`). The instances
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

- the six `no_std` samples `a1`-`a7` (integers, structs, tuples, enums, arrays, overflow checks, panics, large aggregates;
  `bin/rust-front codegen-check`): **every function equal**, and the programs built from our IR (`bin/rsi run --front mir`: the LLVM forms
  lowered by rsi's lowering to C, gcc `-O0`) give the exit statuses rustc's executables give (5, 212, 41, 17, 45); `a6` (drops,
  unwinding) 8 of 11 (its `core::ptr::write_volatile` precondition checks need intrinsics);
- the corpus: **7,117 of 11,453 mono items' functions equal rustc's IR (62.1 %)** over the 81 programs, **12 programs with every
  item equal** (the six samples, `abort`, `exit-code`, `panic-assert`, `panic-todo`, `panic-unwrap-err`, `trap`). The progress of the
  day: the scalar subset 391 of 4,526 (the first run; nine programs were not readable then), then aggregates, enums, unwinding,
  `track_caller` and the shims, then folding, constants in memory, indirect aggregates and nop landing pads;
- **runs**: six of those synthetic programs (`abort` 134, `exit-code` 37, `panic-assert`, `panic-todo`, `panic-unwrap-err` 101,
  `trap` 132) built with their own unit made of our IR, linked with the host's native std as rsi's native-std leg links the oracle's
  (`bin/rust-front run-native`, `RSI_FRONT_IR` in `bin/lib/rsi/link`), give rustc's status, stdout and stderr (thread ids
  normalized). These print through `std::io::_print`, exit, trap and panic with a message: the first synthetic programs run from
  Arbace's code generation. rsi's ci leg and its lowered std (`runtime-ci`, minutes and gigabytes) were not run: the day's rule.
- what blocks the rest, by items (the last run's refusals): `Offset` of non-scalar pointees 396, floats 300, an ABI the slice
  does not take (`abi other`: vectors) 233, pointer scalars (constants) 224, `ctpop` 132, `ptr_offset_from_unsigned` 107,
  `size_of_val` 75, arrays of non-scalars 67, `ctlz` 62, the saturating intrinsics 61, then a tail; 1,383 items differ (classes seen:
  an alloca or a store the other side does not make in functions with drop flags of other shapes, an unreachable block the traversal
  keeps, parameter attributes of newtype pointers) and 627 are whole programs' reads that failed (counted by item).

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
in under a day, the spec, reader, printer and check. RF3 (16-26 days): the first day took the code generation from nothing to 43.6 %
of the mono items of 80 programs and to six programs running; what is left of the 16 to 26 is the remaining codegen
(memory aggregates and `memcpy`, floats, the intrinsics' list, virtual calls, `indirect` constants, about 5 to 8 days by the
refusal counts), then the pieces the helper still answers: layout (`rustc_abi`, about 2,500 lines), the collector and instance
resolution with specialization, v0 symbols (the crates' disambiguator hashes), vtable and drop glue and shim MIR from data
(about 8 to 12 days). The estimate stands; the exactness of the judge is what the day showed.

### Proposed (RF-trial)

- **RFP1 What next in RF3.** Options: (a) **finish the codegen on rustc's data first (memory aggregates, floats, the intrinsics,
  virtual calls, constants in memory) to reach most of the corpus's mono items, then replace the oracle's answers one at a time (layout,
  then the collector with resolution, then symbols), each judged against the helper's**; (b) port layout and the collector first, with
  the codegen at today's 43 %; (c) stop at the trial. *Recommended: (a)*: every step stays judged exactly and the program runs grow
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
