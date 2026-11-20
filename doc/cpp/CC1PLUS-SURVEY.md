# GCC's C++ front end built by Arbace: a survey for the dg check (cc1plus-survey, 2026-11-20)

The brief (the main session, toward the CoMaIn goal "complete the independence of the crops"): C++'s independence read stage is
0.767, held there by the dg check's dg-error part. Arbace's own front end (arbace.repr.cpp.diag) passes 1,536 of the 4,564
supported g++.dg files on their own dg-error lines (test/cpp/check-dg-own-linux-amd64.edn at 412ec79dc), and those count 1. The
other 3,028 stand on clang's recorded Sema dumps (tools/cxxdump, the pack cxxdump-dg-error) and count 0.5. The Mac's checker rules close a few
rows at a time. The candidate lever follows pp-ref's precedent (doc/cpp/CPP-SURVEY.md, "As built: pp-ref"; CP413 (a)): there
Arbace's own C++ builds GCC 15.2.0's libcpp, unmodified, from the pinned gcc tarball, and it judges the token check. Here the
lever would be GCC 15.2's C++ front end, cc1plus `-fsyntax-only` (or the least closure of its objects that runs it), built by
Arbace's C++ the same way. Its diagnostics are the ones g++.dg's dg-error lines were written against.

This is a survey: nothing of the check, the records, the facts or the gate changed. Branch `cc1plus-survey-1120` from
mac/n-merge-1120b: 3c908a0cb (pp-ref-1120's increment merged) for the first probe, then fast-forwarded to 412ec79dc
(cpp-rt-own-1120's increment and queues 500 to 504 merged). friend.cc and parser.cc gave the same refusals on both code
generators, and the reductions ran on 412ec79dc's. The probe ran under the host's rule for jobs below 8 GiB: each
process under `ulimit -v 8388608` and one at a time. The first try at ptree.cc was killed by the host's OOM killer at 3.2 GB (a
global OOM: the host was at 59 of 62 GB under a 47 GB heavy-lock job). After that, a unit started only when MemAvailable was at
least 14 GiB. The coordinator
paused the probe with SIGSTOP while a 43 GiB heavy-lock job peaked, so the probe's wall times include waits. Its CPU times
(user + system) are the measure. Nothing of GCC enters git. The scratch is in the worktree's `.tmp/cc1plus/` (listed at the end).

## 1. What the lever can reach: g++ itself on the dg check

The dg check's own verdict (arbace.repr.cpp.dg's `verdict`, the file's dg-error lines) can take any reader's errors. Here it was
given g++ 15.2's errors twice:

- **as the dg check records them**: test/cpp/ref-dg-gxx15.edn, made by `bin/repr-cpp dg --record-ref`. That run is `g++ -S`, a whole
  compile, on the host's Alpine g++ 15.2.0;
- **under `-fsyntax-only`**: the same options and files run afresh (`.tmp/cc1plus/scripts/gxx-fso.sh`, 4,564 files in 59 s at 4
  jobs). The outputs were read by `arbace.repr.cpp.dg ref` into the reference's format and judged by `verdict`
  (`.tmp/cc1plus/scripts/gxx-verdict.clj`). This is the oracle tier's kind of measure: the host's g++, by hand.

| reader of the 4,564 supported dg-error files | pass on their own dg-error lines |
|---|---|
| Arbace's own front end (the record at 412ec79dc) | 1,536 |
| clang 23.1's Sema (the pack's record) | 3,369 |
| either, as the read cell takes them today (CP344 (a)) | 3,709 |
| g++ 15.2 `-S` (ref-dg-gxx15.edn, judged) | **4,558** |
| g++ 15.2 `-fsyntax-only` (afresh, judged) | **4,505** |
| the own front end or g++ `-fsyntax-only` | 4,510 |
| the own front end or g++ `-S` | 4,560 |

The 6 files g++ `-S` fails are the known ones: gxx-disagrees, musl-libc and cpp1z/fold1.C. `-fsyntax-only` loses 53 more, files
whose expected errors come after the front end (58 files' error lines differ from `-S`'s). By directory:

- gomp/ 17 and tm/ 8: OpenMP's and GNU TM's lowering errors. These are already `:gcc-dialect`, `:design` (CP399 (a)).
- 28 others:
  - musttail 7 (the tail-call pass);
  - `-Wimplicit-fallthrough` 4 (the gimplifier's warning under `-Werror`/`-pedantic-errors`: DRs/dr2406.C, cpp0x/attr-fallthrough1/2.C,
    fallthrough1.C);
  - asm operand constraints 3 (expand: ext/asm10, asm15, asm16);
  - `__builtin_va_arg_pack` 2 (the inliner);
  - 12 single files: ext/builtin-clear-padding-1, is_empty2, decay1, underlying_type7, pr84683, pr84828, pr106877,
    visibility/anon7, abi/mangle41, opt/pr69213, template/cond2, ubsan/pr85029.

The 6 that `-S` fails are coroutines/co-await-void_type.C and cpp1z/fold1.C (gxx-disagrees) and cpp26/literals2.C,
template/nontype10.C, warn/Wconversion-null-4.C and warn/Wnull-conversion-1.C (musl-libc).

So **a cc1plus at g++'s fidelity would pass about 4,505 files with `-fsyntax-only` and 4,558 with a whole compile**, against the
3,709 the read cell credits today and the 1,536 its own front end passes. Counted as the read cell counts (the own front end or the
reference), that is 4,510 or 4,560. The read cell's completeness would go from 85.9% (5,578 of 6,493) to about 99.2%
(`-fsyntax-only`) or 99.9% (`-S`), and the crop's from 95.8% to about 99.1% or 99.3%. The independence effect depends on how the
reader counts (§6).

## 2. The closure

The sources were extracted from the pinned tarball (test/cpp/corpora.edn's `:gcc-15.2.0`, `.tmp/c1/gcc/gcc-15.2.0.tar.xz`): gcc/
without its testsuite, po and the other languages, plus libcpp, libiberty, libdecnumber, include, libbacktrace, libcody, zlib and
config. `gcc/configure` was run alone for x86_64-alpine-linux-musl with Alpine's g++ options (C and C++ only, `--disable-plugin`,
`GMPINC` pointed at the headers, no top-level configure: the host has no mpfr or mpc headers). Then the Makefile's
`generated_files` and the insn-*.cc were made. This used **native** generators (the host's g++ built the 28 gen programs) as a
scaffold for the probe. The own route is §2.4.

### 2.1 cc1plus as GCC links it

`cc1plus: $(CXX_OBJS) cc1plus-checksum.o $(BACKEND) $(CODYLIB) $(LIBS) $(BACKENDLIBS)`, where BACKEND is libbackend.a (OBJS), main.o,
libcommon-target.a, libcommon.a, libcpp and libdecnumber. The configured Makefile expands it as follows (lines and bytes of each
object's source; "generated" means a source the build makes):

| part | units | lines | MB | of which generated |
|---|---|---|---|---|
| CXX_OBJS (cp/ 40, c-family/ 22 with stub-objc, attribs, i386-c, glibc-c) | 65 | 329,304 | 10.1 | none |
| OBJS (libbackend.a: middle end, back end, i386, LTO, analyzer, debug info) | 562 | 3,782,404 | 112.7 | 54 units, 2,537,690 lines, 75.3 MB (insn-recog-1..10, insn-emit-1..10, gimple-match-1..10, generic-match-1..10, insn-attrtab, insn-output, ...) |
| OBJS-libcommon (diagnostics, pretty-print, input, json, text-art) | 37 | 40,741 | 1.2 | none |
| OBJS-libcommon-target (options, opts, i386-common) | 15 | 41,178 | 1.5 | 2 units, 28,269 lines (options.cc, options-urls.cc) |
| EXTRA_BACKEND_OBJS, main.o, cc1plus-checksum.o | 4 | about 600 | | cc1plus-checksum.cc (genchecksum) |
| **gcc/ in all** | **683** | **about 4.19 M** | **125.5** | **57 units, 2.57 M lines** |
| libcpp | 14 | 30,999 (16 files) | | already built by Arbace (pp-ref) |
| libiberty (the archive; the link takes what is called) | 129 in the tree, about 45 called | 47,023 in the tree | | |
| libdecnumber (decNumber, decContext, the BID encodings) | 9 of 18 | about 16,000 | | |
| libbacktrace (ICE backtraces: elf, dwarf, mmap, ...) | about 12 of 33 | about 9,000 | | |
| libcody (modules' mapper; C++) | 8 | 1,647 | | |
| zlib (LTO's compression) | in the tarball, 17 units | 10,312 | | or the system's; stubbed in the least closure |
| zstd (LTO's compression) | the system's | | | `--without-zstd` drops it |
| gmp, mpfr, mpc | **not in the tarball** (§2.3) | | | |
| libstdc++'s compiled part | 56 gcc units define `INCLUDE_STRING`, `INCLUDE_VECTOR`, `INCLUDE_MAP` or `INCLUDE_MEMORY` (cp/parser, cp/module, the mapper, the SARIF and JSON writers), and libcody is written over std::string and std::vector | | | the explicit instantiations libstdc++.so holds: here the lowered runtime of `bin/cppi runtime` (198 units, 177 by Arbace's own code generator) |

### 2.2 What `-fsyntax-only` runs

Read from toplev.cc and passes.cc (gcc 15.2.0). `flag_syntax_only` stops compile_file after `lang_hooks.parse_file` (toplev.cc
line 465) and stops expand_or_defer_fn_1 before cgraph's finalization (cp/semantics.cc 5512). But it does not stop the
initialization:

- general_init makes `new gcc::context ()`, whose pass manager constructor calls all 301 `make_pass_*` functions. They are defined
  in 159 units, from every corner of libbackend. It also registers their dump files from the passes' names.
- process_options runs the target's option override (ix86_option_override: i386-options.cc).
- do_compile calls init_adjust_machine_modes (insn-modes), mpfr_set_emin and mpfr_set_emax, and then **backend_init**: emit-rtl's
  init_emit_once and init_emit_regs, rtlanal, tree-inline's init_inline_once, init_varasm_once, save_register_info and reginfo's
  init_regs (the target's register classes).
- lang_dependent_init calls init_asm_output, init_eh and **init_optabs** (optabs-libfuncs over insn-opinit's generated tables), then
  c_common_nodes_and_builtins with `targetm.init_builtins` (i386-builtins.cc: thousands of `__builtin_ia32_*`, over the generated
  i386-builtin-types.inc).
- symtab->initialize and init_final run.

initialize_rtl (recog, IRA, expmed) is delayed until code generation, so it never runs. While parsing, the front end calls into the
middle end:

- fold, with genmatch's generic_simplify (generic-match-1..10, 186,000 generated lines), builtins.cc and fold-const-call.cc's
  folding of math builtins (mpfr and mpc);
- constexpr evaluation's copy_fn (tree-inline.cc) and `__builtin_object_size` (tree-object-size.cc);
- the global range query (value-query, value-range, range-op);
- **real.cc's real_from_string**, which converts every decimal floating literal with `mpfr_strtofr` (line 2155);
- cgraph, varpool and symtab nodes, and rest_of_decl_compilation (passes.cc);
- the internal functions' tables (internal-fn), the dump manager, the plugin hooks, ggc-page and gtype-desc's roots;
- the target hooks' structure: `targetm`'s TARGET_INITIALIZER in i386.cc references every hook in it, and through them all of i386.cc
  (27,000 lines) and i386-expand.cc.

**The least closure that runs `-fsyntax-only`** is therefore not the front end alone. It is the front end and libcommon, about 109
OBJS units read from that path, and a generated glue file for the other 453 OBJS units. In the glue, each function traps, each
`make_pass_*` returns a dummy pass of its own name, and each data symbol is zero (Arbace's own, as tools/ppref/shim.c is).

The 109, by reading (`.tmp/cc1plus/tierb-core.txt`):

- the tree core: tree, fold-const, fold-const-call, builtins, stor-layout, convert, attribs, stringpool, tree-iterator,
  tree-pretty-print, print-tree, tree-dump, tree-inline, tree-eh, tree-object-size, internal-fn, langhooks, targhooks;
- the init path's RTL and target: emit-rtl, rtl, rtlanal, reginfo, function-abi, varasm, insn-modes, insn-opinit, the optabs-* units;
- symbols: cgraph, cgraphunit, varpool, symtab, symtab-thunks;
- compile_file's setting: passes, context, toplev, timevar, dumpfile, plugin, opts-global;
- support: ggc-common, ggc-page, gtype-desc, real, realmpfr, dfp, double-int, wide-int, the value-range and range-op units,
  generic-match-1..10;
- the target: i386, i386-builtins, i386-options, linux, host-linux;
- the generated options-save and options-urls, and the like.

| the least closure (an estimate by reading) | units | lines |
|---|---|---|
| CXX_OBJS | 65 | 329,304 |
| OBJS-libcommon and OBJS-libcommon-target | 52 | 81,919 |
| OBJS kept | 109 | 447,121 (193,314 generated: generic-match 10, gtype-desc, insn-modes, insn-opinit, insn-enums, options-save, options-urls) |
| main, the extra backend objects, the glue (generated) | 4 | small |
| **gcc/ in the least closure** | **about 230** | **about 860,000 (about 250,000 generated)** |
| replaced by the glue | 453 OBJS units | 3,335,283 |

The rest is libcpp (built), about 45 libiberty units, 9 libdecnumber units, libcody, gmp/mpfr/mpc's units that are called, and
stubs for zlib, zstd and libbacktrace.

The 109 come from reading, not from a link. The exact list is a measurement: link the front end with the glue, run the dg corpus,
move each unit whose trap fires into the closure, and stop when none fires. That was not done here: the brief said survey, do not
build. A native build of cc1plus's objects for the measurement was started and refused by the session's permission check as
beyond the brief, and nothing of it was built. It is the build's first step (CC1P2).

### 2.3 gmp, mpfr, mpc

They are needed and cannot be stubbed for fidelity. real_from_string's `mpfr_strtofr` converts every decimal floating literal.
toplev sets mpfr's exponent range at start-up. fold-const-call folds `__builtin_sin` and its kind, and the complex builtins
through mpc, in constant expressions: real.cc uses 19 mpfr functions, realmpfr.cc 13, builtins.cc 16 and 8 of mpc, and
fold-const-call.cc 58 and 24. system.h includes gmp.h in every unit. wide-int.cc and double-int.cc use mpz, which the front end
reaches rarely.

They are not in the gcc tarball. GCC's own contrib/download_prerequisites (in the pinned tarball) names **gmp-6.2.1, mpfr-4.1.0
and mpc-1.2.1**, with their SHA-512 in contrib/prerequisites.sha512. Copies are already on this host, with no fetch:

- Go's module cache holds modernc.org/ccorpus2@v1.6.0 (pinned by its go.sum `h1:` hash). Its assets include
  `mpc-1.2.1.tar.gz`, **whose SHA-512 is GCC's pin byte for byte**, and `mpfr-4.1.0.tar.gz` and `gmp-6.2.1.tar.gz`. These two are the
  .gz packings, while GCC pins the .bz2.
- GHC's corpus holds `gmp-6.3.0-nodoc.tar.xz`, under the Haskell corpus's pin.

All three are C and fit ci's route (repr/c, cf2g): mpfr and mpc are portable C, and gmp has its generic C (`--disable-assembly`,
`mpn/generic`). Their licenses are LGPL-3.0-or-later (gmp also GPL-2.0-or-later), build-only as libcpp's. The probe read mpfr.h and
mpc.h from those tarballs and gmp.h from the host's gmp-dev (a scaffold).

### 2.4 The configuration and the generated files

| file(s) | made by | the own route |
|---|---|---|
| auto-host.h (243 defines) | configure's host-compiler tests | written by the build script for x86_64-linux-musl, as ppref's config.h is; checked against a native configure's in the oracle tier |
| config.h, bconfig.h, tm.h, tm_p.h, tm_d.h, tm_rust.h, multilib.h, specs.h, gtyp-input.list, configargs.h, bversion.h | mkconfig.sh, genmultilib and the Makefile's shell | shell (busybox): no toolchain |
| options.h, options.cc (24,527 lines), options-save.cc, options-urls.cc, optionlist | opt-gather.awk, opth-gen.awk, optc-gen.awk, optc-save-gen.awk, options-urls-cc-gen.awk over 13 .opt files | awk: the probe's were made by **busybox awk** (`/usr/bin/awk`), so no toolchain |
| i386-builtin-types.inc, pass-instances.def | awk scripts | busybox awk |
| insn-modes.h, insn-modes-inline.h, insn-modes.cc, min-insn-modes.cc | genmodes | a generator compiled by Arbace's C++ and run |
| tree-check.h, genrtl.h | gencheck, gengenrtl | the same |
| gtype-desc.h, gtype-desc.cc (13,443 lines), gt-*.h and gtype-*.h (102 files, 27,910 lines) | gengtype (gengtype-lex.cc ships in the tarball: no flex) | the same |
| insn-constants.h, insn-flags.h, insn-codes.h, insn-config.h, insn-attr*.h, tm-preds.h, tm-constrs.h, insn-target-def.h, insn-opinit.h/cc, insn-enums.cc and, for the whole link only, insn-recog-*, insn-emit-*, insn-output, insn-attrtab, insn-dfatab, insn-latencytab, insn-automata, insn-extract, insn-peep, insn-preds | genconstants, genflags, gencodes, genconfig, genattr, genattr-common, genpreds, gentarget-def, genopinit, genenums, genrecog, genemit, genoutput, genattrtab, genautomata, genextract, genpeep, over common.md and i386.md; **insn-conditions.md** by genconditions, which writes gencondmd.cc, compiled against the target's headers and run | the same; gencondmd is the one compiled per target |
| generic-match-1..10.cc, gimple-match-1..10.cc, *-auto.h | genmatch over match.pd (it links the build's libcpp) | the same (libcpp is built already) |
| target-hooks-def.h, c-family/c-target-hooks-def.h, common/common-target-hooks-def.h (and d/, rust/) | genhooks | the same |
| case-cfn-macros.h, cfn-operators.pd | gencfn-macros | the same |
| cc1plus-checksum.cc | genchecksum over the objects | a constant (the checksum is PCH's guard; no PCH here) |

There are 28 generator programs from 43 build objects (129,380 lines of C++: the rtl reader, gensupport, ggc-none and the programs).
They are the same kind of C++ as cc1plus, so the own route is to build them by Arbace's C++ (gen-batch, ci, cf2g, TamaGo's go) and
run them. Their outputs are keyed by the tarball's pin, so the generators run once a key. No generated file needs a host
toolchain. auto-host.h needs only a written answer, as libcpp's config.h did. The generated files are derived from GPL sources, so
they stay build-only as well (CP413 (a)).

## 3. The probe: gcc/cp's units through Arbace's C++

The route was ppref's: `arbace.cpp.codegen gen-batch` (the native parser, sema, the code generator and the lowering to C) on stage 0
of the main checkout, with mac/n-merge-1120b's arbace/cpp evaluated from the worktree. Each unit had a one-line list and its own
process under `ulimit -v 8388608`, `GOMEMLIMIT=4GiB` and `/usr/bin/time -v`. The options were `c++17 -U__GNUC__ -fno-exceptions
-DIN_GCC_FRONTEND -DIN_GCC -DHAVE_CONFIG_H` with the gcc build's include path (build/gcc, gcc, gcc/cp, include, libcpp/include,
libcody, libdecnumber, libbacktrace, the prerequisites' headers). `-U__GNUC__` takes GCC's portable paths, as for libcpp; GCC
supports non-GCC host compilers.

What dominates is the headers. A gcc/cp unit preprocesses to about 100,000 to 160,000 lines (g++ -E: ptree.cc's 443 lines give
98,965, friend.cc 99,102, lex.cc 102,062, decl.cc 139,618, pt.cc 150,224, parser.cc 161,248). libcpp's lex.cc gives 15,274. tree.h,
cp-tree.h, the rest of gcc's headers and libstdc++'s `<memory>`, `<type_traits>` and `<new>` come with every unit.

| unit | source lines | preprocessed | result | CPU (user + sys) | peak RSS |
|---|---|---|---|---|---|
| cp/ptree.cc | 443 | 98,965 | refused: `member`, "no member u" at `DECL_MODULE_IMPORT_P (ntnode)` (line 67) | 230 s | 4.11 GB |
| cp/friend.cc | 685 | 99,102 | refused: `member`, "no member friend_classes" at `CLASSTYPE_FRIEND_CLASSES` (line 118) | 221 s | 4.25 GB |
| cp/lex.cc | 1,178 | 102,062 | refused: `template`, "missing template arguments for hash_table" (hash-table.h's defaulted parameters) | 258 s | 4.28 GB |
| cp/pt.cc | 32,376 | 150,224 | refused: `template`, "missing template arguments for hash_table" (the same) | 602 s | 4.28 GB |
| cp/parser.cc | 54,312 | 161,248 | refused: `template`, "not a constant: enum" (at struct omp_for_parse_data's bit-fields, line 2332) | 592 s | 4.28 GB |
| cp/friend.cc again (412ec79dc, `CPP_HDR_TRACE=1`) | | | the same: "... of lang_type nil fields []" | 261 s | 4.28 GB |
| cp/parser.cc again (412ec79dc, `CPP_HDR_TRACE=1 CPP_NC_STACK=1`) | | | the same | 505 s | 4.35 GB |

Every unit generated nothing: each stops at its first refusal, and the native parser read all five (no decline). Each also
dropped the same header declarations by cause (CP364 (a)'s drops.tsv: 21 to 34 by "missing template arguments for hash_table",
among them hash_map, hash_set and, in parser.cc, cgraph_node and constant_pool_htab; 2 by "unknown type value_type"; 1 by "no
scope inline").

**The refusals have a few causes, all in gcc's core headers.** They were reduced to small files and run through the same route on
412ec79dc (`.tmp/cc1plus/red/`, 12 s and 1.1 GB for the list):

1. **A class named like an enumerator** (r8 reproduces it). tree-core.h's `enum tree_node_kind` has the enumerators `lang_decl` and
   `lang_type`. tree_decl_common's `struct lang_decl *lang_specific` and tree_type_with_lang_specific's `struct lang_type
   *lang_specific` name the classes (an elaborated type specifier ignores names that are not types, [basic.lookup.elab]), and
   cp-tree.h completes them. The code generator takes a class with no fields: friend.cc's "no member friend_classes ... of
   lang_type nil fields []". Without the enumerators (r3), or with a forward declaration first (r1, r2), the same code generates.
   Every cp unit reaches lang_type and lang_decl through TYPE_LANG_SPECIFIC and DECL_LANG_SPECIFIC.
2. **A defaulted template template parameter** (r4 reproduces it; r5, with the defaulted `bool Lazy` alone, generates).
   hash-table.h line 372 is `template <typename Descriptor, bool Lazy = false, template<typename Type> class Allocator =
   xcallocator> class hash_table`, used as `hash_table<D>`, and the code generator says "missing template arguments for
   hash_table". lex.cc and pt.cc stop on it in their own code. Every unit drops 21 to 34 header declarations by it: hash_map and
   hash_set with their iterators, function.h's types_used_by_vars_hash, hard-reg-set.h's target_hard_regs, cp-tree.h's
   language_function and lang_decl_ns, and in parser.cc cgraph_node and constant_pool_htab. lang_decl_ns takes struct lang_decl
   with it, which is ptree.cc's "no member u".
3. **`inline` followed at once by `::`** (r7 reproduces it): wide-int.h line 724, `inline::wi::storage_ref::storage_ref (...)`, read
   as a scope named inline ("no scope inline"). This drops the constructor every wide_int's storage_ref is built by.
4. **A nested class of a class template whose base is a concrete specialization, naming the base's member typedef** (r9 reproduces
   it: the members are not declared, "unknown name equal" at the call): mem-stats.h line 282's mem_location_hash::hash and
   ::equal, dropped in every unit.
5. **Not reduced**: parser.cc's "not a constant: enum" at struct omp_for_parse_data (line 2307: an `enum tree_code` member,
   `auto_vec<tree, 4>`, `releasing_vec` and bool bit-fields; the place sema records is the last bit-field's width; CPP_NC_STACK
   printed no stack). Also machmode.h line 357's `typedef typename T::measurement_type`, dropped 3 times; r10, its plain form,
   generates.

The four reductions that fail, for the C++ line (`arbace.cpp.codegen gen-batch` at c++17 with `-fno-exceptions`; the passing
variants r1, r2, r3, r5, r6 and r10 are in `.tmp/cc1plus/red/`):

```
// r8: "no member friend_classes ... of lang_type nil fields []"
enum tree_node_kind { d_kind, t_kind, lang_decl, lang_type, all_kinds };
struct tree_type_with_lang_specific { int code; struct lang_type *lang_specific; };
struct lang_type { unsigned char align; void *friend_classes; };
int f (tree_type_with_lang_specific *t) { return t->lang_specific && t->lang_specific->friend_classes != 0; }
int main () { return 0; }

// r4: "missing template arguments for hash_table" (without the template template parameter, r5, it generates)
template <typename Type> struct xcallocator {
  static Type *data (unsigned long count) { return new Type[count]; }
  static void data_free (Type *memory) { delete[] memory; }
};
template <typename Descriptor, bool Lazy = false, template<typename Type> class Allocator = xcallocator>
class hash_table {
  typedef typename Descriptor::value_type value_type;
  value_type *m_entries;
public:
  hash_table () : m_entries (Lazy ? 0 : Allocator <value_type>::data (8)) {}
  ~hash_table () { Allocator <value_type>::data_free (m_entries); }
  value_type *entries () { return m_entries; }
};
struct int_desc { typedef int value_type; };
int main () { hash_table<int_desc> h; h.entries ()[0] = 3; return h.entries ()[0] == 3 ? 0 : 1; }

// r7: "no scope inline"
namespace wi { struct storage_ref { storage_ref (const long *, unsigned int, unsigned int); const long *val; unsigned int len; }; }
inline::wi::storage_ref::storage_ref (const long *val_in, unsigned int len_in, unsigned int)
  : val (val_in), len (len_in) {}
int main () { long v = 3; wi::storage_ref r (&v, 1, 64); return r.len == 1 ? 0 : 1; }

// r9: "unknown name equal" (the nested class's members not declared)
template <typename Type> struct pointer_hash { typedef Type *value_type; };
template <typename T> struct nofree_ptr_hash : pointer_hash <T> {};
struct mem_location { int m_line; };
template <class T> class mem_alloc_description {
public:
  struct mem_location_hash : nofree_ptr_hash <mem_location> {
    static unsigned hash (value_type l) { return (unsigned) l->m_line; }
    static bool equal (value_type l1, value_type l2) { return l1->m_line == l2->m_line; }
  };
};
int main () { mem_location a = {3}; return mem_alloc_description<int>::mem_location_hash::equal (&a, &a) ? 0 : 1; }
```

**So the probe found a few causes, not many, but it sees only one a unit.** gen-one stops at a unit's first refusal, so the probe
cannot look past these until they are fixed. All five sit in headers every unit includes (tree-core.h, hash-table.h, wide-int.h,
mem-stats.h, cp-tree.h, vec.h), so each fix moves every unit at once. Below them the units' own code is unprobed: pt.cc's template
machinery and parser.cc's 54,000 lines. libcpp's precedent: 11 first gaps for its 14 units on the morning of 2026-11-20, all closed by
the C++ line that day, with two more found after them (constant arrays as structs, a macro's goto). cc1plus's own code is ten times
libcpp's and uses more of C++ (templates, gcc's vec and hash containers, GTY'd unions), so more rounds are likely.

## 4. The estimate

The probe gives the **header floor**, the cost of a unit before its own code counts. All five units reached their first refusal
after their headers were analyzed:

- 220 to 260 s of CPU for ptree.cc, friend.cc and lex.cc (about 100,000 preprocessed lines);
- 500 to 600 s for pt.cc and parser.cc (150,000 and 161,000 lines: c-family's, the OpenMP and gimplify headers too; parser.cc
  stopped at its line 2,332 of 54,312);
- 4.1 to 4.35 GB in each case.

The middle end's units preprocess to as much (tree.cc 148,173 lines, fold-const.cc 151,329, toplev.cc 143,485). So the estimate
takes **400 s a unit** on average.

ppref's libcpp shows the cost of a unit's own code: 14 units and the harness, about 35,000 lines on 15,000-line preprocessed units,
took 3 minutes in one process. That is about 5 s a thousand lines beyond the headers. With the gaps closed and one process a unit,
as the probe ran:

| step | the least closure (CC1P2 (a)) | the whole cc1plus (CC1P2 (b)) | the measure behind it |
|---|---|---|---|
| C++ units through gen-batch | about 230 gcc units: 230 x 400 s + 860 kLOC x 5 s, about **27 h of CPU**; 4.3 GB a process, so 3 at once on this host: about **9 h** | 683 units: about 82 h of CPU, about 27 h at 3 at once | the probe's floor; ppref's 5 s a kLOC |
| the generators (28 programs, 43 units, 129 kLOC, lighter headers) and their runs | about 1.5 h of CPU, then minutes | the same | rough: half the floor a unit |
| C units (libiberty, libdecnumber, gmp, mpfr, mpc) through repr/c | 300 to 400 small units: about 2 h | the same | ci's reads of musl and sqlite |
| the lowered C's read (repr/c, `read-lib-opts`) | about 200 MB of lowered C (libcpp: 7.3 MB for 31 kLOC, about 235 bytes a line): about **7 h** of CPU at libcpp's 15 minutes for 7.3 MB; by units 3 at once, about 2.5 h | about 1 GB: about 34 h | ppref's read: 32 units, 15 min, 3.8 GB peak |
| cf2g and TamaGo's go | about 290 MB of Go (libcpp: 10.5 MB, one 417,000-line prog.go): **past one package under 8 GiB**, so a package a unit group (CC1P8); about 2 h of CPU; the link of a 400 to 500 MB executable unmeasured (Go's linker may pass 8 GiB) | about 1.4 GB of Go | ppref's prog.go and 20 MB executable |
| **in all** | **about 40 h of CPU, about 14 h of wall at 3 processes**: more than a night (ppref: 11.8 minutes) | about 125 h of CPU | |

**Peaks**: gen-batch at 4.3 GB a unit fits the 8 GiB rule. repr/c's read of one lowered unit grows with the unit: charset's 10.2 MB
of lowered C passed 8 GiB before the array fix. So parser.cc (54 kLOC, perhaps 12 MB lowered), options.cc's tables, gtype-desc.cc
and generic-match-N are the risks, and insn-recog-N and gimple-match-N too in the whole link. Each would be measured alone, and a
unit past 8 GiB would be a heavy-lock job.

**Levers on the estimate** (the C++ line's, not this survey's):

- **A shared header prefix.** Every gcc unit begins with the same 95,000 lines (config.h, system.h, coretypes.h, tree.h, cp-tree.h
  and libstdc++'s headers). If the code generator analyzed that prefix once a process and reused it for many units, as a
  precompiled header does, most of the 27 h would go, and the build would fit a night (about 6 to 8 h of wall). P15's parse cache, measured slower on samples/io.cpp (CP336 (a)), is a
  parse cache, not this.
- **Several units in one process**, as ppref's single gen-batch run does.

**The key**: ppref's key holds the code generator, so every arbace/cpp commit rebuilds it (12 minutes). A 14-hour build cannot
follow arbace/cpp that way (CC1P7).

**The dg check's run time.** g++ `-fsyntax-only` took 94 s of user and 121 s of system CPU on the 4,564 files, about 0.05 s each,
mostly process start. The own build runs about 30 times the native build's time a file (ppref). That gives about 47 minutes of
CPU for the front end's work. Each process also starts a 400 to 500 MB executable and builds the target's thousands of builtins,
perhaps 0.3 to 1 s more a file. **In all about 1.3 to 2 h of CPU, 20 to 30 minutes at 4 jobs**, a few hundred MB a process. For
comparison, the own front end's whole dg run takes about 80 minutes at two threads.

## 5. The judging design

**The harness**: `tools/cc1ref/`, Arbace's own code under Arbace's license, as tools/ppref is, with no line of GCC. It holds:

- **the driver's part.** g++'s specs give cc1plus its command line: `-quiet -fsyntax-only -std=... -fdiagnostics-plain-output`, the
  test's options, and the search list (`-nostdinc -isystem` over the target tree that `bin/repr-cpp target` makes: libstdc++ from
  the pinned gcc tarball, musl from its tarball, gcc's include directory; no header of the host, as the token check, CP241 (b)).
  The harness writes that command line from the dg check's run.tsv (the standard and the g++ options it already records), as
  `g++ -###` shows it. cc1plus computes its own predefined macros from its target configuration, so they are GCC's own. The
  harness passes none.
- **the closure's glue.** It is generated from the closure's undefined symbols: traps, dummy passes and zero data (§2.2). Like
  shim.c, it supplies operator new and delete, `__cxa_pure_virtual` and `__cxa_guard_*`. GCC builds itself with `-fno-exceptions
  -fno-rtti`, and libcody too, so no exception tables are needed. It also stubs zlib, zstd and libbacktrace. libstdc++'s compiled
  part comes from the lowered C++ runtime.
- **one process per file.** That is cc1plus's model and DejaGnu's: GCC keeps global state, as ggc and the symbol table do.

**The check**: `bin/repr-cpp dg --front cc1`, or a judge mode as `REPR_CPP_PP_JUDGE=own` is for the token check:

- It runs the reference on each dg-error file and keeps its stderr where gxx1 keeps g++'s (WORK/dg-error/cc1/NAME.{txt,status}).
- It reads them with the code `ref-cmd` already uses: `errors-of`, the dg-prune-output rule, `alt-hits` and `fold-other`. Then it
  judges with `verdict`, the test's own dg-error lines. The reference speaks GCC's words, so its messages could also be held to
  the dg-error regexps, which the line model does not check today (a finer judgment: CC1P9).
- A file's row says which reader passed it: the own front end first (CX12's record, unchanged), the reference where the own front
  end does not pass.
- The record test/cpp/check-dg-linux-amd64.edn would hold the reference's rows in place of clang's. The pack cxxdump-dg-error and
  tools/cxxdump would serve only the oracle tier.

**What stays reviewed:**

- **Files whose expected errors come after the front end** (§1): under `-fsyntax-only`, 25 files of gomp/ and tm/ are already
  `:gcc-dialect` by design; the 28 others go in a class of their own, "the error is the middle end's" (`:design`, each file named,
  as `:gcc-dialect` names its files). A whole-program build (CC1P3 (b)) would leave about 6.
- **Target selectors**: `{ target i?86-*-* x86_64-*-* }`, `{ target { ! lp64 } }`, ilp32 and c++NN only. They stay the dg check's
  (dg.ae's `holds?`, test/cpp/dg-targets.edn), evaluated for x86_64-linux-musl, unchanged. The reference is configured for the same
  target, so its own target-dependent errors (`__int128`, x86 builtins, `-m` options) are the target's.
- **The existing classes**: musl-libc (musl's NULL as nullptr, iconv's `*`) becomes the reference's own behaviour on musl, as
  Alpine's g++ shows; gxx-disagrees and prune-output stay as classes. The reference's failures would be few (g++ `-S` fails 6, all
  already classed).
- **Pristine against Alpine.** The reference is GCC 15.2.0 as the tarball has it, while ref-dg-gxx15.edn is Alpine's patched g++.
  The fidelity step lists every difference: a step of the oracle tier (`:cxx`), the native stand-in's runs against ref-dg-gxx15.edn
  file by file. A difference is a `:ref` class with its reason (the patch, or `-fsyntax-only`), as pp-check's `:ref` is.

**The proofs and controls** (pp-ref's, §"As built: pp-ref"):

1. The own build's runs equal the native stand-in's byte for byte, the statuses and the diagnostics' text, over the corpus or its
   samplings. This catches the code generator's miscompilations of GCC, the build's real risk.
2. The negative controls (test/cpp/controls.edn's dg entries: a case judged against another case's expectations) fail under the
   reference's runs.
3. A planted bug: a mutation in the dg judge (dg.ae's `verdict` reading a dg-error line one off) fails rows. A second planted bug
   in the code generator (an integer comparison flipped in a scratch checkout, the reference rebuilt) fails proof 1.
4. **The stub run**: gcc, g++, cc, c++, cpp, cc1, cc1plus, clang, clang++, clang-23, ld and as, scripts exiting 99 first on PATH.
   The own-judged dg check passes with the stubs' log empty. The reference is run by its path, never by a name on PATH.

**Licensing** (CP413 (a)'s terms; CC1P6). cc1plus and its libraries are GPL-3.0-or-later; libiberty, libdecnumber and libcody are
LGPL-2.0+ or GPL with GCC's runtime exception, and gmp, mpfr and mpc LGPL-3.0-or-later. Running them is not restricted (GPLv3 §2).
The build (objects, generated files, lowered C, Go forms, the executable) stays in `.tmp` and the gate's local caches: never
committed, packed or pushed. tools/cc1ref is Arbace's own, against GCC's interfaces. The glue's generator is Arbace's own; its
output includes GCC's headers and so is build-only too. LICENSE.md says what the harness is linked with, where, and that the result
is never conveyed.

## 6. The fair counting question

C++'s independence today (`bin/crops-score --crop cpp` at 412ec79dc) is **94.3**: read 0.767, eval 0.973, AOT 0.973, print 1.0,
judge 0.9998. The judge already counts the dg check's 4,564 cases at 1, because the verdict is the test's own dg-error lines. **So
the lever can only move the read stage, and the read stage's dg part is a question of whose acceptance and errors are read.**
Today the read is the own front end's where that passes (1,536, at 1), and clang's recorded dumps for the rest (3,028, at 0.5). The
stand-in exists only for completeness: without it, those 3,028 rows would be failures of the own front end and the read stage
would score 1.0 (option (c) below).

The question, plainly: **is GCC's front end, hosted by Arbace (built from the pinned tarball by Arbace's C++, run at check time,
no host toolchain, no record), the crop's own reader, as javac on Arbace is Java's? Or is it a reference, a better stand-in than
clang's record, that the crop's own reader is measured against?**

For "own reader" (the Java model):

- CROPS-SCORE's rule as written gives 1 to a stage with no host toolchain at check time and no record consumed. A cc1plus that
  Arbace built is neither.
- Java's read stage scores 1 on javac on Arbace. That covers its diagnostics too: java-check's `diagnostics` level, langtools'
  golden diagnostics run by javac on Arbace, is one of the read stage's steps.
- Java also keeps its own native reader measured against javac on Arbace (parse-check, front-check, attr-check, nojavac-check). C++
  would keep arbace.repr.cpp.diag's record beside the reference in the same way.

For "a reference":

- The user's py-own-judge-1 (a) (2026-11-20) names exactly this kind of thing: "an independent reference implementation,
  unmodified from its pinned upstream tarball, compiled by Arbace's own C or C++ behind a small harness, becomes the check-time
  judge". libcpp (pp-ref) and CPython's parser (queue 494) were placed as judges under it.
- Java's case differs in two ways. repr/java is built on javac's trees by design, so its reader is javac. And javac's sources are in
  the tree as Java forms, translated by Arbace's own j2g. repr/cpp's reader is the native parser; GCC's front end would be a second
  front end consulted only for diagnostics, never committed.
- g++.dg's dg-error lines are GCC's own messages, so GCC's front end passing them measures Arbace's C++ compiler (it compiled GCC
  correctly), not Arbace's C++ front end. It is a large and real test of the code generator, but of the AOT path, not of the read.

The effect, stage by stage (the read cell at 412ec79dc: 5,578 pass of 6,493, the token check's 1,934 cases and the dg check's; the
reference at g++'s `-fsyntax-only` fidelity, §1):

| answer | read | eval | AOT | print | judge | independence | read cell completeness | crop completeness |
|---|---|---|---|---|---|---|---|---|
| today | 0.767 | 0.973 | 0.973 | 1.0 | 0.9998 | **94.3** | 85.9% | 95.8% |
| (a) a reference: its verdicts stand in for the rows the own front end fails, counted 0.5 as clang's record was | 0.767 (rises only as arbace.repr.cpp.diag passes more) | 0.973 | 0.973 | 1.0 | 0.9998 | **94.3** | about 99.2% | about 99.1% |
| (b) the crop's reader (Java's model): every dg row by the hosted front end, 1 | **1.000** | 0.973 | 0.973 | 1.0 | 0.9998 | **98.9** | about 99.2% | about 99.1% |
| (c) no stand-in at all: the own front end's verdicts alone (cc1plus optional, as a reference for its errors) | **1.000** | 0.973 | 0.973 | 1.0 | 0.9998 | **98.9** | about 53.4% | about 87.6% |
| (d) not built: CX12's rule-by-rule slices | 0.767, rising a few rows a slice | | | | | 94.3 | 85.9% | 95.8% |

(b) and (c) give the same independence. The difference between them is only whose passes the completeness counts. That shows what
the read stage's 0.233 is: the price of crediting 3,028 rows' completeness to a reader that is not Arbace's. Under (a) the lever
buys completeness (+13 points of the read cell), takes clang out of the check (the pack and tools/cxxdump go to the oracle tier),
and gives the own front end a reference that speaks GCC's words. It does not buy independence. The remaining independence gap,
eval and AOT at 0.973, is the C++ runtime's units still on clang's IR pack (177 of 198 own since cpp-rt-own-1120's increment) and
is not this lever's.

## 7. Proposed (CC1P1-CC1P10; none decided)

In each, the recommended option is listed first.

- **CC1P1: how the hosted front end counts** (§6).
  - **(a) A reference.** Its verdicts stand in for the dg rows the own front end does not pass, counted 0.5 as clang's record is.
    The read stage stays at 0.767 and rises only with arbace.repr.cpp.diag. The lever buys completeness (the read cell about 85.9% to
    99.2%, the crop about 95.8% to 99.1%), takes clang out of the check, and gives the own front end GCC's words to be measured
    against. It follows py-own-judge-1 (a)'s wording, the closest ruling.
  - (b) The crop's reader, as javac on Arbace is Java's: every dg row by the hosted front end, 1. The read stage goes to 1.000 and
    independence from 94.3 to **98.9**. The own front end's record is kept and reported beside, as Java's native reader is measured
    against javac on Arbace. The rule's letter and Java's read stage (its diagnostics level among its steps) support it.
  - (c) No stand-in: the own front end's verdicts alone. Read 1.000, independence 98.9, but the read cell's completeness falls to
    about 53% (the crop's to about 87.6%). It needs no cc1plus, and shows what the 0.233 is.
  - (d) Not built: CX12's slices, a few rows at a time.

  **Recommended: (a)**, plainly because (b) would score Arbace's hosting of GCC as C++'s reading: the tests are GCC's own messages,
  and the native front end passes a third of them. If the user prefers the Java precedent, (b) is consistent with it and with the
  rule as written. The build is the same under (a) and (b); only independence.edn's fact differs.
- **CC1P2: the closure.**
  - **(a) The least closure.** The front end, libcommon and the ~109 OBJS units §2.2 lists, with a generated glue for the other 453
    units. It is measured first by a native link-and-run: the host's g++ builds the same closure and glue, as ppref's `--native`
    build does (the oracle tier's and the harness's development build, never the judge). The dg corpus is run, each trap that fires
    moves its unit into the closure, and that is repeated until none fires. About 230 gcc units and 860 kLOC.
  - (b) The whole cc1plus: 683 units and 4.19 M lines, 2.57 M of them generated. No glue; `-S` possible. About three times the build.
  - (c) A harness that calls the front end without toplev, as ppref redoes cc1's preprocessing: smaller still, but it redoes
    toplev's initialization order (options, target override, backend_init, the builtins), where fidelity is easily lost.

  **Recommended: (a).** Its native measurement is a native build of part of cc1plus, which this survey did not do (the brief: no
  build). It needs the user's go-ahead as the first step.
- **CC1P3: `-fsyntax-only` or a whole compile.**
  - **(a) `-fsyntax-only`**: 4,505 of 4,564 files at g++'s fidelity. The 25 gomp/ and tm/ files are already `:gcc-dialect`, and the
    28 others whose errors are the middle end's go in a reviewed class (the 6 that `-S` fails too are classed already).
  - (b) `-S` (CC1P2 (b) only): 4,558.

  **Recommended: (a).** 53 files for three times the build is not worth it while the eval and AOT gaps are elsewhere.
- **CC1P4: gmp, mpfr and mpc** (§2.3; needed: real_from_string's `mpfr_strtofr` for every decimal floating literal).
  - **(a) GCC's own prerequisites**: gmp-6.2.1, mpfr-4.1.0 and mpc-1.2.1, pinned by the SHA-512 in the pinned tarball's
    contrib/prerequisites.sha512. mpc-1.2.1.tar.gz is already on the host with that SHA-512 (Go's module cache,
    modernc.org/ccorpus2@v1.6.0). gmp-6.2.1.tar.bz2 and mpfr-4.1.0.tar.bz2 need a fetch from gcc.gnu.org's infrastructure
    directory, so the user's go-ahead. They are built by ci as C (gmp's generic C), build-only.
  - (b) No fetch: the module cache's copies, with SHA-256 pins of our own. These are mpc-1.2.1.tar.gz (`17503d2c...`, GCC's own
    file), mpfr-4.1.0.tar.gz (`3127fe81...`) and gmp-6.2.1.tar.gz (`c9d5a19e...`), the .gz packings of the same releases, which
    GCC does not pin.
  - (c) Stubs for mpc and mpfr's math, so that fold-const-call folds no math builtin. mpfr's conversion is still needed, so this is
    partial, and constant expressions over `__builtin_sin` and its kind are lost.

  **Recommended: (a)**, or (b) if no fetch is wanted.
- **CC1P5: the generated files.**
  - **(a) The tarball's own generators, compiled by Arbace's C++ and run.** These are the 28 programs; the awk and shell scripts run
    by busybox. auto-host.h is written by the build script for x86_64-linux-musl, as ppref's config.h is, and checked against a
    native configure's in the oracle tier. The outputs are keyed by the pin.
  - (b) Generated once by native generators (the host's g++), as this probe's scaffold was: the reference's build would then rest on
    a host toolchain's output.
  - (c) Generated files committed: excluded. They derive from GPL sources (CP413 (a)).

  **Recommended: (a).**
- **CC1P6: licensing.**
  - **(a) As CP413 (a)**: the build in `.tmp` and the gate's local caches only, never committed, packed or pushed. tools/cc1ref (the
    harness and the glue's generator) is Arbace's own under EPL-1.0. LICENSE.md says what it is linked with (GCC 15.2.0's cc1plus
    objects, libcpp, libiberty, libdecnumber, libcody, gmp, mpfr, mpc), where, and that the result is never conveyed.
  - (b) Not accepted (the lever falls).

  **Recommended: (a).**
- **CC1P7: the reference's key.**
  - **(a) The units cached one by one**, each keyed by its preprocessed text, its options and the code generator's key, with
    `bin/repr-cpp cc1ref build` rebuilding the stale ones. The judge's executable is keyed by the units' keys, the harness, repr/c,
    ci and cf2g. A rebuild after an arbace/cpp change is then more than a night until the shared-prefix lever exists; the check keeps
    the last build until the night makes the new one, and says which build judged.
  - (b) ppref's key as it is: every arbace/cpp commit invalidates the reference, a 14 h rebuild before the check can run.
  - (c) A code generator revision pinned for the reference, rebuilt deliberately.

  **Recommended: (a).**
- **CC1P8: the program's size.**
  - **(a) cf2g and `compile-list` translate a program into a package a unit group**, each compile under 8 GiB, and Go's link of a
    400 to 500 MB executable is measured before the build. This is needed by (a) and (b) of CC1P2 alike: ppref's single
    417,000-line prog.go scales to about 290 MB of Go.
  - (b) One package under the heavy lock: Go's compiler on hundreds of MB in one package is far past this host's memory.

  **Recommended: (a).**
- **CC1P9: the judging.**
  - **(a) The own front end first and the reference for the rest** (CP344 (a)'s either-reader rule). The record says which reader
    passed. The middle end's 28 files are a reviewed class. Fidelity (the native stand-in against ref-dg-gxx15.edn, pristine against
    Alpine) is in the oracle tier. The proofs and controls are §5's: own build equal to the native byte for byte, the negative
    controls, a planted bug in the judge and one in the code generator, and the stub run.
  - (b) The same, and each reference error's message also held to its dg-error regexp, which the line model does not check today:
    finer, and possible only for GCC's words.

  **Recommended: (a)**, with (b) as a later refinement.
- **CC1P10: the code generator's gaps.**
  - **(a) The probe's refusals and their reductions** (§3; `.tmp/cc1plus/red/`) go to the C++ line (cpp-rt-own), which owns
    arbace/cpp/codegen, as pp-ref's 11 gaps went. The probe is rerun after each fix, to see past the first gap.
  - (b) Wait for CX12's own needs.

  **Recommended: (a).** Four reduced causes and one unreduced, all in gcc's core headers, stop every unit; until they are fixed
  the probe cannot see further. The reductions are r4 (a defaulted template template parameter), r7 (`inline::`), r8 (a class
  named like an enumerator) and r9 (a nested class naming its concrete base's typedef), with parser.cc's "not a constant: enum".

**The order, once ruled:**

1. CC1P10's gaps to the C++ line, and the probe again.
2. The native link-and-run closure (CC1P2 (a); the user's go-ahead).
3. CC1P4's prerequisites and CC1P5's generators through Arbace.
4. CC1P8's packages.
5. The build: about 14 h of wall at 3 processes, or 6 to 8 h with the shared-prefix lever.
6. The record run and the proofs (§5).
7. The fact under CC1P1.

## Scratch

`.tmp/cc1plus/` in the worktree:

- `src/`: the tarball's parts, extracted;
- `build/`: the native configure, the generated files and the build libraries, a scaffold;
- `prereq/`: mpfr.h and mpc.h from the module cache's tarballs, and the host's gmp.h;
- `pre/`: g++ -E of the units;
- `probe1/`: each unit's list, log, refusal and drops.tsv;
- `red/`: the reductions;
- `fso/`, `fso-verdict.edn`, `gxx-verdict.edn`, `fso-diff.txt`: §1's measures;
- `scripts/`: probe.sh, gxx-fso.sh, gxx-verdict.clj, fso-diff.clj, sizes.py;
- `objs.txt`, `tierb-core.txt`, `listing.txt`.
