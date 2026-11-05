# Haskell's eval and AOT: survey and plan

Status: initial survey and plan, 2026-11-03, at the user's request, after Haskell was unparked (doc/JOURNAL.md,
"2026-11-03: Haskell and Rust unparked; Python's survey"). Nothing here is adopted: no code under `arbace/`, `go/`,
`bin/`, `tools/` or `test/` changed, and only this file was written. The decisions are the user's, as proposals
HES1-HES22 (§12); the milestones are HE1-HE11 (§11), named by HES2.

Base: branch `haskell-eval-survey` from main `a25d12d1`. The work is reading (repr/haskell's survey, spec and notes, the
crop surveys of OCaml, Scheme and Python, GATE-SURVEY.md's GT8, C23-SURVEY's memory model) and probes of a few seconds
each with the host's GHC 9.14.1 (`ghc -ddump-stg-final`, `-ddump-simpl`, `-ddump-prep`, `-fwrite-if-simplified-core`,
`--show-iface`, `:browse GHC.Prim`, `nm` of the installed libraries, runs of four small programs; scratch in the
worktree's `.tmp/haskell-eval/`, Appendix A), and the existing `bin/repr-haskell test` (passing, on the main checkout's
stage 0). No Arbace executable was built, no lock taken, nothing installed, nothing downloaded. What the host does not
hold (GHC's testsuite tarball, nofib, Csaba Hruska's external STG tools) is named from memory of the projects, every
such size, URL or license marked **to verify**, verified at the fetch the user decides (HES1).

Words used throughout:

- **Haskell forms**: Haskell modules written as Arbace forms (doc/haskell/SPEC.md), what repr/haskell reads and prints.
  Surface syntax: operator chains unresolved, names unrenamed, no types inferred, no dictionaries.
- **STG forms**: GHC's final STG of a module (after CorePrep, CoreToStg, unarisation; `-ddump-stg-final`'s program)
  written as Arbace forms (§4), the input of eval and AOT. A second representation beside Haskell forms, produced by
  the helper, never printed back to Haskell.
- **hsi**: the evaluator of STG forms for programs and at the REPL (the name is HES2's), as goi is Go's, oi OCaml's,
  si Scheme's, pyi Python's. Its runtime **`go/arbace/hs`**: hand-written Go forms, as `go/arbace/oi` and
  `go/arbace/scm`.
- **The helper**: `tools/hsdump`, repr/haskell's GHC API program, extended with STG (and Core) modes (§5).
- **The front end**: GHC 9.14.1's renamer, type checker, desugarer, simplifier, CorePrep and CoreToStg, run by the
  helper. **The judge** of runs: the same GHC's builds of the originals, their runs recorded as data (§8.1).
- **The boot libraries**: the 41 packages of the pinned `ghc-9.14.1-src.tar.xz` that repr/haskell's corpus holds
  (`test/haskell/corpus-ghc-9.14.1.edn`, 1,226 modules, R1 and R5), `ghc-internal` and `base` first.

Contents: Summary · 1 What parity means · 2 The host and prior art · 3 Measurements · 4 STG forms · 5 The front end:
the helper's STG · 6 eval: hsi · 7 AOT · 8 The references and the corpora · 9 Licenses · 10 The row and GT8's budget ·
11 Plan (HE1-HE11) · 12 Proposals (HES1-HES22) · 13 What to decide first · 14 Risks · 15 Sources · Appendix A: the
scratch.

## Summary

- **Haskell's meaning is not in its syntax, and the gap is the largest of the crops.** Fixity, renaming, type
  inference with type classes (dictionary passing), type families, GADTs, defaulting, deriving, the desugaring of `do`,
  list comprehensions, records and pattern matching, rewrite rules, Template Haskell's splices: all lie between Haskell
  forms and anything that runs. GHC's front end is several hundred thousand lines (to verify); an Arbace type checker
  for GHC Haskell with the boot libraries' extensions is out of reach of one crop. So, as OCaml's crop took Lambda from
  `ocamlc` (OC4 (a), OC5 (a)), **eval takes GHC's final STG from the host's GHC** (HES3 (a)): the language GHC's own
  back ends share (the native code generator through Cmm, the JavaScript back end `GHC.StgToJS`, GHCi's
  `GHC.StgToByteCode`), A-normal, untyped but with each variable's representation, laziness explicit (`\u` updatable
  thunks, `\r` functions), constructors and primops saturated, evaluation order fixed by `case`.
- **Interpreting GHC's STG of the same compilation as the reference gives the same evaluation order**: which thunk is
  forced first, which of two imprecise exceptions is raised, the order `Debug.Trace` prints (E6: `sum (reverse [trace
  "a" 1, trace "b" 2])` prints `b` then `a`). A front end of Arbace's own could not promise that.
- **Haskell's library is almost all Haskell** (E4, E5): `Show Double`, `Read`, `Data.Char`'s Unicode tables, the
  UTF-8 codec, `Handle`s and their buffers, `printf`, `Data.Map`, the top-level exception handler and its message, the
  `HasCallStack` backtraces: all STG from `ghc-internal`, `base` and the other boot libraries, interpreted as the
  program is. What is not Haskell is small and enumerable: **1,498 primops, 742 of them without SIMD** (E4; 299 of those
  array, `Addr#` and `MutVar#` accessors), the out-of-line primops GHC writes in Cmm (`stg_*zh`), and the C the
  libraries call: **375 external symbols from `ghc-internal`'s archive, 77 from `base`'s** (E5): libc (`read`, `write`,
  `open`, `poll`, `isatty`, `getenv`, ...), libm, 31 GMP functions (avoidable: GHC's own pure-Haskell bignum backend,
  HES7), and the RTS's API (about 200, most of them for the threaded RTS, profiling and the event log, never reached by a
  non-threaded run).
- **The machine is GHC's own design, on Go.** An eval/apply STG machine (Marlow and Peyton Jones, "Making a fast curry")
  with an explicit, heap-allocated stack per Haskell thread (HES8 (a)): update frames, case continuations, catch and mask
  frames, STM and prompt frames on it, so deep thunk chains (E7: `foldl (+) 0 [1..3000000]` at `-O0` needs 350 MB of
  stack natively and runs), green threads, asynchronous exceptions, `<<loop>>` detection (E7) and the delimited
  continuation primops are all frames, as si's heap frames are Scheme's (SCHEME-SURVEY §5.2 (a)). Haskell threads are
  green threads on hsi's scheduler with the non-threaded RTS's semantics (HES9 (a)); memory for `Addr#` is ci's flat
  address space (`go/arbace/crt`, C23-SURVEY §6.3), so `malloc`, `peek`, `poke`, the C ports and musl's libm are
  shared with the C crop (HES10 (a)).
- **AOT is STG → Go forms over the same runtime** (HES15 (a)), GHC's JavaScript back end's shape: each closure's code a
  Go function, tail calls through a trampoline, the stack the same explicit stack; realistic speed 3 to 10 times hsi,
  well under GHC's `-O0` native code (§7). The boot libraries compiled once this way and linked into hsi is GHCi's own
  model (libraries native, the program interpreted), HES14 (b).
- **Print stays as it is** (§1, HES20): repr/haskell's read and print, their tree, interface, text and layout levels
  over the boot libraries, are the row's read and print cells; the print cell gains a behaviour level (the printed
  modules compiled and run, as ghc-lisp's run corpus did) and the read corpus gains the testsuite's sources.
- **Plan (§11): HE1-HE11, about 54 to 88 working days**; HE4 (the machine and its primops, 15-25) and HE5 (the
  libraries' C and the I/O, 8-14) are the long poles, HE8 (AOT, 10-16) the third. A standalone front end (GHC itself
  compiled by hsi's AOT, as javac runs on Arbace; HES21) is a later survey.
- **GT8 from day one (§10):** the GHC testsuite's run tests and nofib through coverage subsets and the seven-night
  rotation; synthetic programs per primop family and per Prelude class; the dumps and the references cached by key so a
  night pays only for runs. The read and print levels already cost about 10 minutes (HASKELL-NOTES R5: 585 s), so
  Haskell's 35 minutes leave about 20 for eval and AOT: they must be subsets.
- **To decide first (§13):** what eval interprets (HES3), how the helper captures STG (HES4), the libraries'
  compilation (HES6) and bignum backend (HES7), the machine (HES8), the memory model (HES10), when (HES22).

## 1. What parity means

Under the crops' rule (doc/FULL-SUPPORT-SURVEY.md, "The binding decisions"; C23-SURVEY §1; OCAML-SURVEY §1;
PYTHON-SURVEY §1): on the host (linux/amd64), forms only, each level's results recorded as data, `bin/reap` judging
from the records with no runs, none failing, every non-pass in a recorded class.

| cell | in Arbace (proposed) | corpus | reference | judge |
|---|---|---|---|---|
| **Haskell read** | repr/haskell `read` (as built, R1-R6) | the boot libraries (1,345 modules with R6), the testsuite's sources, nofib, the samples, synthetic (§8.2) | GHC 9.14.1's parse (`HsModule GhcPs`), its acceptance and its parse errors | trees equal (tree level), interfaces equal (interface level), as today; a rejected file rejected with GHC's message and span |
| **Haskell eval** | the Haskell forms printed by repr/haskell (`:full`, byte for byte), the front end's STG (GHC, HES3), hsi | the run programs: synthetic, the testsuite's run tests, nofib, samples | GHC 9.14.1's `-O0` builds of the originals run, recorded (status, stdout, stderr; §8.1) | byte for byte, no masks by default (HES16); per test for the testsuite |
| **Haskell AOT** | the same STG → Go forms → `arbace.build.go` (HES15) | as eval | as eval | as eval; hsi and compiled compared (diffcheck's kind) |
| **Haskell print** | repr/haskell `print` (`:lines`, `:full`, the layout layout) as built | every read corpus | the original text; GHC's trees and interfaces of the print; the run programs' behaviour | the four levels as built; plus behaviour: the printed program compiled by GHC runs as the original (HE9) |

What differs from the other crops:

- **The front end is the oracle's, for good in v1.** Java's eval was Arbace's from the start (the Java forms'
  interpreter), Go's checker is "after go/types", Python needs no checker (PYTHON-SURVEY §1). For Haskell, as for
  OCaml's v1 (OC4 (a)), types and everything up to STG come from the pinned compiler; eval begins at STG. Eval of
  Haskell forms is therefore: print the forms (`:full`, byte for byte, so columns in `HasCallStack` text are the
  original's), compile with the front end, run the STG in hsi. The record of the dumps makes a check need no GHC only
  if the dumps are committed (HES13).
- **The front end's tests are not eval's.** Most of GHC's testsuite tests the type checker (`compile_fail`, `compile`,
  `ghci_script` with `:t`): with GHC as the front end they test GHC, not Arbace; they are read and print corpus, and a
  class at eval ("front end is GHC's").
- **Columns reach behaviour** (REPR-HASKELL-SURVEY §1.3; E6: `error, called at
  libraries/containers/containers/src/Data/Map/Internal.hs:629:17 in containers-0.8-b42e:Data.Map.Internal`): the
  source paths, the columns and the unit ids of the boot libraries are part of the stderr the judge compares, so the
  libraries' dumps must be compiled from the same relative paths with the same unit ids as the installed ones (HES6).

## 2. The host and prior art

### 2.1 The host

- `ghc-9.14.1-r2` (Alpine edge), the `ghc` library `ghc-9.14.1-f453` (hidden), `base-4.22.0.0-3875`,
  `ghc-internal-9.1401.0-6d86`: `Tables next to code` YES, `Have interpreter` YES, `Support SMP` YES, `GHC Dynamic`
  YES, LLVM `llc` 15 configured (unused by default). The bignum backend is **GMP** (31 `__gmpn_*` symbols in
  `ghc-internal`'s archive, E5).
- The installed interfaces carry unfoldings only, not whole programs (E3: `--show-iface` of
  `GHC/Internal/List.hi` has `Unfolding: Core:` entries and no `extra decls`): the libraries' STG must come from
  compiling their sources, which repr/haskell's interface level already does package by package with each package's
  configuration (HASKELL-NOTES R2, R5: 184 s for all, four packages at a time).
- `-fwrite-if-simplified-core` writes the whole simplified Core of a module into its `.hi` (`extra decls`, E3), the
  input GHCi's `-fprefer-byte-code` reads; `-ddump-stg-final`, `-ddump-prep`, `-ddump-simpl`, `-ddump-bcos`,
  `-ddump-cmm` are all there (E2).
- Not on the host: GHC's testsuite (the release's `ghc-9.14.1-testsuite.tar.xz`, to verify), nofib, Hackage packages
  beyond the boot libraries. `/root/forks/ghc-lisp` keeps ghc-lisp as patches only; its numbers (11,423 parseable
  testsuite files, 1,069 single-module run programs) are from GHC 10.1's tree (REPR-HASKELL-SURVEY §2.1).

### 2.2 How GHC runs Haskell (the facts eval must reproduce; from GHC's source and papers, to verify against 9.14.1)

- **The pipeline**: parse → rename → typecheck → desugar (Core) → simplify → CorePrep (A-normal form, saturated
  applications) → CoreToStg → STG passes (unarise: unboxed tuples and sums become several values, `State#` and other
  void arguments vanish; lambda lifting at `-O1`; tag inference) → Cmm → code. GHCi's bytecode, the JS back end and the
  Cmm code generator all start from the final STG.
- **The STG machine**: closures (functions `\r`, updatable thunks `\u`, single-entry `\s`, constructors), partial
  applications (PAPs), eval/apply calls with arity checks (`stg_ap_*` in E5's list), update frames that overwrite a
  thunk with an indirection to its value, blackholing (a thunk under evaluation is marked so that re-entering it is
  `<<loop>>`, E7), pointer tagging (an optimization, invisible to programs).
- **The RTS**: the scheduler (green threads on capabilities; the non-threaded RTS has one), MVars with FIFO wake-up,
  `threadDelay` and I/O waits, asynchronous exceptions (`throwTo`, `killThread`, `timeout`) with masking states, the
  interrupted thunks frozen (`raiseAsync` turns the stack into `AP_STACK` closures), STM (transaction records,
  validation, `retry` blocking on the read set), weak pointers and finalizers, stable pointers and names, the
  deadlock detection that raises `BlockedIndefinitelyOnMVar`/`OnSTM` to unreachable blocked threads, stack overflow
  at `-K` (default 80% of physical memory), heap overflow, compact regions, the `prompt#`/`control0#` delimited
  continuations (9.6+).
- **The top level**: `:Main.main = runMainIO main`, Haskell code in `GHC.Internal.TopHandler`: it flushes `stdout` and
  `stderr`, prints an uncaught exception as `PROG: Uncaught exception TYPE:` with its `displayException` and the
  backtraces (E6), and sets the exit status (1; `ExitFailure n`; signals). The program name is `getProgName`, the
  basename of `argv[0]`.

### 2.3 Prior art (from memory, to verify)

- **GHC's back ends** (gitlab.haskell.org/ghc/ghc, BSD-3-Clause): `compiler/GHC/StgToJS/` (STG → JavaScript, 9.6+,
  from GHCJS; a trampolined machine with its own heap objects, the model of §7), `compiler/GHC/StgToByteCode.hs` and
  `rts/Interpreter.c` (GHCi: bytecode from STG, interpreted by a C loop inside the RTS, interoperating with compiled
  code), `compiler/GHC/StgToCmm/` (the native path), the WebAssembly back end (9.6+, through Cmm and the NCG,
  the RTS compiled to wasm: not a model, it keeps GHC's RTS). `rts/` (the C and Cmm runtime), `utils/genprimopcode`
  and `compiler/GHC/Builtin/primops.txt.pp` (every primop's type and attributes: the table hsi's primops are checked
  against).
- **The external STG interpreter** (Csaba Hruska, github.com/grin-compiler/ghc-whole-program-compiler-project, BSD-3
  to verify): a GHC fork exports every module's STG in a serialized "external STG" format (`.o_stgbin`), and an STG
  interpreter written in Haskell runs whole programs from it, with the primops, the FFI through libffi, threads,
  MVars, STM, exceptions, weak pointers; reported to run GHC itself and pandoc (to verify). The closest prior art: it
  shows that GHC's STG plus a primop table plus the C calls is enough, and its primop implementations are a checklist
  (a reference, not vendored: HES5).
- **MicroHs** (Lennart Augustsson, github.com/augustss/MicroHs, Apache-2.0, 0.16.0.0 of 2026-06-02 per
  REPR-HASKELL-SURVEY §7): a self-hosting Haskell compiler (Haskell 2010 and many GHC extensions) in Haskell, compiling
  to combinators run by a small C runtime. Its own `base`, not GHC's: it cannot run GHC's boot libraries. A candidate
  standalone front end (HES21 (b)).
- **Hugs** (BSD-style, unmaintained since 2006): an interpreter with its own front end, Haskell 98 and some extensions;
  shows the size of a full front end of its time. **GHCJS** (MIT), **Eta** (BSD-3, GHC 7.10's STG to JVM bytecode,
  abandoned: the Java-forms route of HES15 (b)), **Frege** (BSD-3, a Haskell-like language on the JVM), **stgi** (David
  Luposchainsky, BSD-3, an educational STG interpreter of the 1992 paper's machine), **ministg** (Bernie Pope).
- **Papers**: Peyton Jones, "Implementing lazy functional languages on stock hardware: the Spineless Tagless
  G-machine" (JFP 1992); Marlow and Peyton Jones, "Making a fast curry: push/enter vs. eval/apply for higher-order
  languages" (ICFP 2004, JFP 2006); Marlow, Peyton Jones, Moran, Reppy, "Asynchronous exceptions in Haskell" (PLDI
  2001); Harris, Marlow, Peyton Jones, Herlihy, "Composable memory transactions" (PPoPP 2005); Marlow, Peyton Jones,
  Singh, "Runtime support for multicore Haskell" (ICFP 2009); Peyton Jones, Reid, Hoare, Marlow, Henderson, "A semantics
  for imprecise exceptions" (PLDI 1999).

### 2.4 What Arbace already has

- repr/haskell (R1-R6, HR1-HR28): the helper `tools/hsdump` on GHC's API with the package configurations of all 41
  boot libraries (`test/haskell/corpus-ghc-9.14.1.edn`: unit ids, CPP flags, hsc2hs, the generated modules), the
  interface level's per-package `ghc --make` (the same compilations HE2 needs, with a dump added), `bin/repr-haskell
  test` (passing on 2026-11-03).
- oi (`go/arbace/oi`): an interpreter of a compiler's IR dumped by its oracle (Lambda), with a primitives table, proper
  tail calls, a depth limit, effects by goroutine hand-off: the closest pattern. si's heap frames (`go/arbace/scm`):
  the explicit-stack machine of HES8 (a). goi's check machinery (`--record-ref`, `divergences`, controls, CPU limits,
  essential subsets).
- ci's runtime `go/arbace/crt`: one flat 64-bit address space of Go slabs with a block table (C23-SURVEY §6.3), libc's
  system calls, and musl 1.2.6 as C forms with cf2g: the base for `Addr#`, `malloc`, the libc and libm calls of E5
  (HES10, HES11).

## 3. Measurements

Probes (seconds each; Appendix A):

- **E1, the host.** `ghc --version`, `ghc --info`: as §2.1.
- **E2, the dumps.** On `T.hs` (31 lines: a class and instance, a lazy sieve, `Data.Map.Strict`, `IORef`, `Integer`,
  `try`/`evaluate`), `ghc -O0 -ddump-simpl -ddump-prep -ddump-stg-final -ddump-to-file` took 1.2 s with linking. Final
  STG is 506 lines (55 KB with uniques suppressed, 20 KB of Core at `-O0`; at `-O1` 49 KB of STG, 46 KB of Core). The
  text dump is not an interface: with `-dsuppress-uniques` two different binders print as the same name
  (`$carea_sat` shadows `$carea_sat` in one `let`), and the dump omits the representations and the data constructors'
  tags. So the helper must serialize GHC's STG data structures itself (HES4), not scrape the text.
- **E3, whole Core in interfaces.** `-fwrite-if-simplified-core` adds `extra decls` (the module's whole Core, fully
  qualified, with types) to `T.hi` (8 KB); the installed libraries' `.hi` files have only unfoldings.
- **E4, primops.** `:browse GHC.Prim` lists **1,498 primops** (`GHC.Internal.Prim` in 9.14); **756** are SIMD vector
  operations (`X2` to `X64`), **742** are not, 299 of those array, `ByteArray#`, `Addr#`, `MutVar#` and `SmallArray#`
  operations; among the rest `fork#`, `forkOn#`, `atomically#`, `keepAlive#`, `prompt#`, `control0#`,
  `newPromptTag#`, `compactNew#`, `newBCO#` and the exception, masking, MVar, TVar, weak and stable families.
- **E5, the C the libraries call.** Undefined symbols of the installed archives not defined in them, without GHC's own
  `_info`/`_closure` symbols: **`ghc-internal` 375** (31 GMP `__gmpn_*`, 139 `stg_*` RTS entry points and out-of-line
  primops, about 200 RTS API and libc and libm functions: `read`, `write`, `open`, `close`, `lseek`, `fstat`, `poll`,
  `epoll_*`, `eventfd`, `pipe`, `isatty`, `tcgetattr`, `getenv`, `setenv`, `environ`, `iconv*`, `nl_langinfo`,
  `strerror_r`, `malloc`, `free`, `memcpy`, `sin` ... `tanhf`, `getMonotonicNSec`, `rts_*`, `hs_popcnt*`, `hs_pdep*`,
  `libdw*`, `lookupIPE`, `getOrSet*Store`, `performGC`, ...), **`base` 77** (mostly `stg_*` again and libm),
  `ghc-bignum` and `ghc-prim` none (re-export shims). The list for a non-threaded run reached by the corpora is smaller
  and is measured at HE5.
- **E6, observable order and the top handler.** `sum (reverse [trace "a" 1, trace "b" 2])` prints `b`, `a`, `3`;
  `M.fromList [(1,'x')] M.! 2` ends the program with status 1 and on stderr `U: Uncaught exception
  ghc-internal:GHC.Internal.Exception.ErrorCall:`, the message, and `HasCallStack backtrace:` with the library's source
  path, line, column and unit id (`containers-0.8-b42e`). All of it is Haskell (the top handler), so hsi prints it if
  the libraries' STG is compiled from the same paths with the same unit ids.
- **E7, depth and loops.** `foldl (+) 0 [1..3000000::Integer]` at `-O0` runs natively in 0.78 s with a peak RSS of
  352,856 KB (a 3,000,000-deep chain of thunks forced on GHC's growable stack); `let x = x + 1 :: Int in print x` ends
  with `L: Uncaught exception ghc-internal:GHC.Internal.Control.Exception.Base.NonTermination:` and `<<loop>>`, status
  1. A Go-recursive evaluator would not survive the first (or would need a gigabyte Go stack) and could not detect the
  second the same way.
- **E8, speed.** `nfib 32` (7,049,155 calls, `Int` with `Num` dictionaries at `-O0`): `-O0` native 0.32 s, `-O1`
  0.02 s, GHCi's bytecode (`runghc`) 3.62 s, of which about 0.23 s is `runghc`'s start (`main = print 1`). GHCi's
  interpreter is about 10 times `-O0` native: the order hsi can hope for (§6.8).

## 4. STG forms

### 4.1 Principles

The other crops' (doc/go/SPEC.md, doc/ocaml/SPEC.md §11.2's Lambda forms): data readable by the Clojure reader, one
form per node, `(head field...)` with an optional trailing metadata map, keywords for flags, vectors for lists. They
are a compiler's IR, not a user's source: written by the helper, read by hsi and the AOT, never printed as Haskell
(HES19). Top form per module `(stg/module UNIT MODULE {:imports [...] :foreign-stubs ...} bindings...)`; the heads
follow GHC's `GHC.Stg.Syntax` constructors (proposed, to fix at HE1):

```
(stg/rec [...])  (stg/bind ID RHS)                         ; StgTopLifted (rec or non-rec); StgTopStringLit
(closure :u [] BODY)  (closure :r [ARGS] BODY)             ; StgRhsClosure, update flag \u \r \s
(con DATACON [ATOMS])                                      ; StgRhsCon, StgConApp
(app ID [ATOMS])  (op PRIMOP [ATOMS])  (ffi "sym" CONV [ATOMS] {:safe ...}) ; StgApp, StgOpApp
(case SCRUT BNDR ALT-TYPE [(alt :default [] E) (alt DATACON [B...] E) (alt (lit ...) [] E)])
(let BIND E)  (let-no-escape BIND E)  (tick T E)  (lit ...)
```

- **Ids** as symbols with their unique and their module (`base:GHC.Internal.Base.map`, `x_s1Ab`), each binder's
  `PrimRep` list in metadata (`:rep [:lifted]`, `[:int :word]`, `[]` for void) and arity; no types otherwise.
- **Data constructors** by name, with the tag, arity, field representations and the strictness marks of their
  wrapper-free worker in a per-module table (`stg/data`), so hsi builds them without GHC.
- **Literals** with their representation (`Int#`, `Word8#`, `Double#` as the IEEE bits, `Addr#` strings as bytes, label
  literals for `foreign import ... &sym`).
- **Positions**: the source notes (`tick`s at `-g`) only where the dump asks for them; `HasCallStack` text is already in
  the literals (E6), so eval needs none.

### 4.2 Size and storage

Text forms are large (E2: 55 KB for 31 lines) and Arbace's reader reads forms at a few MB/s (REPR-HASKELL-SURVEY E2:
20 MB in 12 s on 16 jobs): the boot libraries' STG as text is of the order of 100 to 300 MB (to measure at HE2). So the
forms are the specification and the check's medium (a dump read back by Arbace's reader, every head known, as
`arbace.repr.ocaml.lambda` checks Lambda dumps), and hsi loads a **binary image** compiled from them (HES14).

## 5. The front end: the helper's STG

### 5.1 The pipeline

`bin/hsi build FILE.hs` → `tools/hsdump stg` (a GHC API driver: the module compiled as `ghc -O0 -c` would compile it,
with the package's configuration from the corpus file) → STG forms (`.stg.ae`) → hsi's loader. For Haskell forms the
first step is repr/haskell's `print` in mode `:full` (the original text byte for byte); for `:lines` forms, the layout
printer's output (columns differ: a class, E6).

### 5.2 Capturing STG (HES4)

GHC's driver offers a hook between STG and Cmm (`GHC.Driver.Hooks`' `stgToCmmHook`, to verify in 9.14.1): the helper
installs it, serializes the `CgStgTopBinding`s, the module's data constructors (`TyCon`s and `DataCon`s with tags and
representations) and foreign stubs, and lets code generation go on, so one compilation gives both the object the
reference links and the STG hsi runs. A Core-to-Core plugin sees Core only (no STG); scraping `-ddump-stg-final` is
lossy (E2). Core is also dumped (`core` mode, from the same compilation or from `-fwrite-if-simplified-core`'s
interfaces) for the REPL's types and for debugging, not for eval.

### 5.3 The boot libraries (HES6, HES7)

Every module of the 41 packages compiled from the pinned tarball **as hadrian compiled the installed ones**: the same
unit ids (`-this-unit-id`, already in the corpus file), the same relative source paths (E6's
`libraries/containers/containers/src/...`), the same optimization level and flags (hadrian's release flavour, `-O1`
or `-O2` per package: to verify from the installed `.hi` files' flags, `--show-iface`'s "flags" hash), so that the STG is
what the installed libraries were compiled from, up to the bignum backend (HES7). The dumps are cached by key (GHC's
pin, the tarball's sha256, the configuration), rebuilt only when one changes (minutes, as the interface level's 184
s).

### 5.4 What GHC's front end decides and hsi inherits

Typing, defaulting, instance selection, rewrite rules, inlining across modules (unfoldings of the libraries' `.hi`
into the program's STG), worker/wrapper and strictness at `-O1`, Template Haskell (splices run by GHC at compile time:
a TH program's STG is ordinary), deriving, `HasCallStack`'s implicit parameters (call sites as string literals), the
order of evaluation. A program GHC rejects never reaches hsi: its rejection is the read or the front end's business.

## 6. eval: hsi

### 6.1 What eval means for Haskell

Program runs (`bin/hsi run FILE.hs ARGS`, or `run --stg DIR` on dumps), with `getArgs`, `getProgName` (the reference's
name), stdin, stdout, stderr and the exit status as GHC's top handler sets them; and a REPL (`bin/hsi`, GHCi's
behaviour for expressions and `let`, `it`, `:t` and `:k` answered by GHC; HES18). Not in v1: GHCi's other commands,
`-threaded`, profiling, `+RTS` options beyond `-K` and `-M` (classes).

### 6.2 The parts

1. **The front end** (§5): the helper's `stg` mode, the libraries' dumps, the image.
2. **The machine** `go/arbace/hs` (§6.3): heap objects, eval/apply, the stack, updates, blackholes, the scheduler.
3. **The primops** (§6.4): the 742 non-SIMD ones in Go, the SIMD ones a class (HES12).
4. **The C ports** (§6.6): the libc, libm and RTS functions the libraries call, on `crt`.
5. **The tools**: `bin/hsi run|check|record-ref|divergences|test`, `arbace.hs.check` on the tools' host, as goi's.

### 6.3 The machine (HES8)

- **Heap objects** as Go structs: `*Con` (data constructor, tag and fields), `*Fun` (code and free variables, arity),
  `*Thunk` (code, free variables; on update its value and the indirection), `*PAP`, `*AP` (a frozen application), the
  unlifted kinds (`Int#` as `int64`, `Word#` `uint64`, `Double#` `float64`, `Addr#` a `crt` address, `ByteArray#`,
  `MutableArray#`, `MutVar#`, `MVar#`, `TVar#`, `ThreadId#`, `Weak#`, `StablePtr#`, `StableName#`, `BCO` refused) and
  unboxed tuples as multiple return values (unarisation has flattened them already).
- **Eval/apply** with an explicit stack per thread (a Go slice of frames, grown on demand to the `-K` limit; GHC's
  default is 80% of memory, hsi's a recorded figure): return frames for `case` (the continuation's code and saved
  variables), update frames, apply frames for over-saturated calls, catch frames, mask and unmask frames, `atomically`
  and `catchRetry`/`catchSTM` frames, prompt frames. A `case` pushes its frame and evaluates the scrutinee; a value
  returns to the top frame; a thunk entered pushes an update frame and is marked a blackhole (eager blackholing), so
  re-entering it from the same thread raises `NonTermination` (E7) and from another thread blocks on it, as GHC's
  `BLACKHOLE` does.
- **The loop** is a Go `for` over the current code, never Go recursion: depth is the stack slice's, tail calls are
  free, and a thread switch is a change of stack.
- **Code**: each `closure`'s body compiled once at load into a tree of Go closures over frame slots (goi's and oi's
  technique), with the frequent primops and the known-arity calls as nodes of their own.
- **Alternatives** (HES8): (b) direct style on Go's stack, one goroutine per Haskell thread (calls fast; E7's chain
  and `<<loop>>` lost, asynchronous exceptions and `control0#` impossible without the same bookkeeping); (c) push/enter
  (GHC's pre-2004 machine; no gain over eval/apply here).

### 6.4 Primops (HES12)

The 742 non-SIMD primops (E4), checked against `primops.txt.pp`'s attributes (`can_fail`, `has_side_effects`,
`out_of_line`): integer and word arithmetic at every width with GHC's overflow and division semantics (`quotRemInt#`,
`timesInt2#`, `addIntC#`), `Double#`/`Float#` with IEEE results (`decodeDouble_2Int#`, the conversions), `Char#`,
the array families (bounds unchecked as in GHC: an out-of-bounds index is undefined behaviour there, a Go panic
turned into a class here), `Addr#` loads and stores at every width on `crt`, `MutVar#` (`atomicModifyMutVar2#`), the
exceptions (`raise#`, `raiseIO#`, `catch#`, the masks), MVars, TVars, `fork#`, `yield#`, `myThreadId#`, `killThread#`,
`delay#`, `waitRead#`, `mkWeak#`, `finalizeWeak#`, `makeStablePtr#`, `makeStableName#`, `seq#`, `keepAlive#`,
`dataToTag#`, `tagToEnum#`, `reallyUnsafePtrEquality#`, `prompt#`, `control0#`, `getSpark#` and `par#` (no-ops:
one capability), `traceEvent#` (no-op). `compactNew#` and friends, `newBCO#`, `unpackClosure#`, `getApStackVal#`,
`getCCSOf#` (profiling) and the SIMD vectors are classes in v1. The Cmm out-of-line primops (`stg_*zh`, E5) are among
these, written in Go.

### 6.5 Threads, exceptions, STM (HES9)

- **Green threads** on hsi's own scheduler, the non-threaded RTS's semantics: one Haskell thread runs at a time,
  switched at allocation counts (GHC switches on a 20 ms timer at heap checks; the switch points are a design class),
  MVars with FIFO wake-up, `threadDelay` on a timer queue, `threadWaitRead`/`Write` by `poll` when every thread is
  blocked, `getNumCapabilities` 1, `forkOn` as `fork`.
- **Deadlock**: when no thread can run and no timer or I/O is pending, the blocked threads not reachable from a root
  get `BlockedIndefinitelyOnMVar` or `OnSTM` (GHC finds them by its GC; hsi by a reachability walk of its own heap from
  the runnable threads, the stable pointers and the main thread's waiters, at that moment only).
- **Asynchronous exceptions**: `throwTo` delivered at the target's next safe point when unmasked, the target's stack
  unwound to the nearest catch frame; the thunks under evaluation on the unwound part are reverted to unevaluated
  (GHC freezes them as `AP_STACK` to resume the work later: the same results, different cost; observable only through
  `unsafePerformIO` side effects, a class).
- **STM**: transactions on a log; with one thread running at a time, a transaction is not preempted (it runs to its
  commit or its `retry`), which is serializable and so a valid execution; `retry` blocks the thread on its read set;
  `orElse` nests logs.
- **Delimited continuations**: `control0#` captures the stack segment up to its `prompt#` frame as an object;
  resuming it pushes the segment back.

### 6.6 The C and the RTS (HES11)

- **A table by C symbol**, `ffi` forms resolved at load: each entry a Go function over the arguments' representations.
  Families: the POSIX calls the I/O library makes (`read`, `write`, `open`, `close`, `lseek`, `fstat`, `ftruncate`,
  `poll`, `isatty`, `tcgetattr`/`tcsetattr`, `dup`, `pipe`, `unlink`, `getenv`, `environ`, ...) on `crt`'s system
  calls; libm through musl's own C forms compiled by cf2g (bit-exact with the host's GHC, which links musl's libm) or
  Go's `math` with a class for the last bit; the RTS API the libraries call (`getMonotonicNSec`, `getProgArgv`,
  `rtsSupportsBoundThreads` = 0, `performGC`, `getRTSStatsEnabled` = 0, `lockFile`, `hs_popcnt*`, `hs_pdep*`,
  `hs_bitrev*`, `hs_word2float*`, `__int_encodeDouble`, `getOrSet...Store`, `cmp_thread`, `rts_isThreaded` = 0, ...);
  the C files of the packages (`cbits/`) ported or read as C forms and run by ci (HES11 (b)).
- **Unreached is a class**: the event manager's `epoll`/`eventfd` (threaded RTS only), `libdw*` and `lookupIPE`
  (backtraces from DWARF and info tables, off by default), profiling, `iconv` beyond UTF-8 (the references run under
  `LANG=C.UTF-8`, where the Haskell UTF-8 codec is used).
- **`foreign import ccall "wrapper"`** (`createAdjustor`, a C function pointer calling Haskell) and `foreign export`:
  classes in v1; a user program's own C (`c-sources` in the testsuite) through ci later (HES11 (b)).

### 6.7 Memory, GC, finalizers (HES10)

- **Lifted heap objects** are Go objects under Go's GC; a thunk's update leaves an indirection the code short-cuts on
  every read (GHC's GC removes indirections; hsi's loads do).
- **Selector thunks** (`fst p` where `p` is evaluated) are evaluated at update time where GHC's GC would, so the
  classic space leaks GHC avoids are avoided (a memory difference only).
- **`Addr#` and pinned memory** in `crt`'s flat address space (C23-SURVEY §6.3): `malloc`/`free` (musl's, as ci's),
  string literals (`"..."#`) in a static segment, pinned `ByteArray#`s (`newPinnedByteArray#`, every `ByteString`) in
  slabs, `byteArrayContents#` an address. Unpinned byte arrays are Go `[]byte`.
- **Weak pointers and finalizers** on Go's `weak` package and `runtime.AddCleanup`, run by a finalizer thread at
  `performGC` (which calls `runtime.GC`) and at exit as GHC's `hs_exit` does for C finalizers; their timing is a
  design class (as Python's PYS9 (a), OCaml's OC11).
- **Stable pointers** a table; **stable names** a weak-keyed map from Go pointers (Go's GC does not move objects), the
  hash values a class.

### 6.8 Speed

GHCi's bytecode runs `nfib` about 10 times slower than `-O0` native code (E8); goi-style closure trees on Go with
boxing and an explicit stack are likely 1 to 5 times GHCi (to measure at HE4), so 10 to 50 times `-O0` native. The
`-O0` programs' STG has dictionaries passed for every `+` (E2's `* $fNumDouble`), which `-O1` removes: hsi runs `-O0`
STG for the judge's sake (HES6), and runs a library compiled at `-O1`/`-O2`, which carries most of a typical program's
time.

## 7. AOT

STG → Go forms over `go/arbace/hs` (HES15 (a)), printed by repr/go and built by `arbace.build.go`, as Scheme's AOT
(SC9 (a)) and Python's (PYS13 (a)):

- each closure's code a Go function of the frame, returning the next code to run (a trampoline: Go has no tail calls),
  the explicit stack and heap objects the interpreter's, so an AOT program and hsi share the scheduler, the primops,
  the C ports and every class; GHC's JS back end has this shape (`GHC.StgToJS`: a trampoline over its own heap
  objects);
- known calls of known arity direct (a Go call where the callee returns to its caller without a thread switch or a
  stack growth check failing), primops inlined, `case` on a tag a Go `switch`;
- the libraries compiled once per pin into the program's packages (with hsi's image they are the same STG), so a
  compiled program and hsi with compiled libraries (HES14 (b)) differ only in the user's modules.

Realistic speed: 3 to 10 times hsi on call-heavy code (the dispatch and closure-tree overheads gone, the boxing and
the trampoline left), still slower than GHC's `-O0` native code; the gain is small on code that spends its time in
the libraries once those are compiled for hsi too. The alternatives: (b) STG → Java forms → j2g (Eta's route): j2g
adds a translation of closures and statuses for nothing, the runtime being Go forms already; (c) Cmm → Go (the
unregisterised C back end's shape): Cmm assumes GHC's RTS and heap layout, so the RTS (scheduler, GC, stacks) would
have to be ported to Go too.

The check: `bin/hsi check --aot` over the run corpora against the same references, and hsi against compiled per
program (diffcheck's kind).

## 8. The references and the corpora

### 8.1 The references

- **Runs recorded as data** (`test/haskell/ref-ghc914.edn`, `bin/hsi record-ref`, as `bin/java-check --record-ref`
  and `bin/oi`'s references): each program built by the host's GHC at `-O0` (the user's brief, and the level whose STG
  hsi runs) and run under a fixed environment (`LANG=C.UTF-8`, `TZ=UTC`, `GHCRTS` unset, no terminal, a fixed program
  name), its status, stdout's digest and text, stderr's digest and text. The check needs no GHC beyond the dumps
  (HES13).
- **Masks**: none by default (GHC's outputs carry no addresses); timings and `GHC.Stats` a class. The testsuite's own
  normalisers (`normalise_errmsg_fun`, `ignore_stdout`, ...) apply to its expected files (below).
- **The testsuite's expected files** (`T.stdout`, `T.stderr`, with platform variants `-ws-64`, `-mingw32`, ...): a
  second judge, as jtreg's golden files are Java's diagnostics level; where they disagree with the host's run (they
  are made for GHC's own build configuration), the recorded run wins and the difference is listed.
- **`-O1` runs** a later second reference (programs whose output depends on optimization: imprecise exceptions,
  `trace` order, `unsafePerformIO`), against `-O1` STG.

### 8.2 The corpora

| corpus | source | size | license | use |
|---|---|---|---|---|
| the boot libraries | the pinned `ghc-9.14.1-src.tar.xz` (repr/haskell's corpus) | 41 packages, 1,345 modules with R6, 11 MB | BSD-3-Clause and each package's (BSD-3 mostly; to verify per package) | read, print (as built); compiled to STG: hsi's library; AOT |
| the testsuite | `ghc-9.14.1-testsuite.tar.xz` from downloads.haskell.org (to verify) | of the order of 10,000 tests (to verify), of which the `compile_and_run` and `run_command` ones perhaps 3,000 to 4,000 (to verify; ghc-lisp ran 1,069 single-module ones of GHC 10.1's tree) | BSD-3-Clause (GHC's) | read, print (every `.hs`); eval and AOT for run tests, judged per test against the host's runs and the expected files; the rest a class |
| the testsuite's language core | `testsuite/tests/{codeGen/should_run,deSugar/should_run,typecheck/should_run,concurrent/should_run,lib/base,primops/should_run,array/should_run,ffi/should_run}` (names to verify) | a few hundred | as above | the first eval corpus |
| nofib | gitlab.haskell.org/ghc/nofib at a pinned commit (to verify) | about 100 programs in `imaginary`, `spectral`, `real`, `shootout`, `gc`, `parallel`, with inputs and expected outputs, run in its `fast` mode | per program, BSD-like and others (to verify) | eval and AOT; speed |
| synthetic | generated (§8.3) | hundreds of small programs | Arbace's | every level; the essential subsets |
| samples | `test/haskell/sample/` (repr/haskell's) and `test/haskell/eval/samples/`, one per runtime feature | tens | Arbace's | every level |
| Hackage set (later) | a pinned set of pure packages with their test programs (`QuickCheck`, `tasty`, `random`, `splitmix`, ...) | to choose | each package's | eval (the boot libraries' own test suites need them) |

### 8.3 Synthetic corpora (GT8.6, GTS207 (a))

From the start: (1) one program per primop family (E4), printing each operation's results over boundary values
(`minBound`, `maxBound`, `-0.0`, NaN, shifts by the width); (2) one per Prelude and `base` class method at its standard
instances (`Show`, `Read`, `Enum`, `Num`, `Integral`, `RealFrac`, `Floating`, `Bits`, `Ix`), printing; (3) the
runtime's paths: laziness (`trace` order, `seq`, deep thunk chains, `<<loop>>`), exceptions (`throw` in pure code,
`catch`, `bracket`, `finally`, masks, `throwTo`, `timeout`), concurrency (MVar fairness, `threadDelay`, deadlock
detection), STM (`retry`, `orElse`), weak pointers, `IORef` and arrays, `Integer` at every size, text I/O (encodings,
buffering, `hSetBuffering`, `interact`). Each run by the same GHC and recorded (GTS207 (a)); generated by an Arbace
generator from `primops.txt.pp`'s list and the classes' method lists, committed with its seed.

## 9. Licenses

GHC (`compiler/`, `rts/`, `utils/genprimopcode`, the testsuite) is under the **Glasgow Haskell Compiler License**, a
BSD-3-Clause text; `ghc-internal`, `base`, `ghc-prim`, `ghc-bignum` the same; the other boot libraries each under their
own (BSD-3-Clause for most: `containers`, `bytestring`, `text`, `transformers`, `mtl`, `parsec`, `stm`, `time`,
`unix`, `directory`, `process`, `filepath`, `deepseq`, `array`, `binary`; `haskeline` BSD-3; to verify each `LICENSE`
in the tarball). `LICENSE.md` gains them when STG dumps or ports derived from them are committed (HES13): an STG dump
is a translation of the library's code, so it carries the library's notice, as `jdk/`'s Java forms carry the JDK's.
GMP (LGPL-3.0-or-later or GPL-2.0-or-later) is not needed with the native bignum backend (HES7 (a)). nofib's programs
have mixed licenses (to verify per program: some are BSD-style, some "free for research"); a program without a clear
license stays out of the committed corpus and is fetched by pin. MicroHs (Apache-2.0) and the external STG
interpreter (BSD-3, to verify) are references, not vendored.

## 10. The row and GT8's budget

### 10.1 The row

`test/reap/cells.edn` gains `:haskell` with four cells and classes files (`test/haskell/divergences{,-read,-aot,-print}.edn`);
the read and print cells judged from repr/haskell's existing records (the round trip's results files, written by the
full runs as C's are, CS240 (a)), extended to the testsuite's sources; the Makefile gains `hsi-*` targets (`make
check-makefile`). As the other crops since OC120 (b), its steps enter the gate only when its row fits GT8's budget
(the journal, 2026-11-03).

### 10.2 The budget (GTS201 (a): about 35 minutes nightly, about 2.4 hours clean, a share of the essential 15 minutes)

| tier | Haskell's content (proposed) | estimate (to measure at HE2, HE4, HE6) |
|---|---|---|
| essential (about 1 min) | the samples at every level; the synthetic core's coverage subset under hsi from cached dumps; one AOT program; 20 boot-library modules round-tripped at the tree level | under 60 s on stage 0 and the gate's executable, the dumps and the libraries' image cached by key |
| nightly (35 min) | read and print over the boot libraries (today 585 s, HASKELL-NOTES R5; the interface level's 4.5 minutes the largest part, so a coverage subset of its packages plus the rotation) and the testsuite's sources, deduplicated (GT8.2); eval and AOT over the coverage subset of the testsuite's run tests and nofib's `fast` mode (GTS205 (a): every block of `go/arbace/hs` reached, every primop family, every class with one case, the controls, every deferred case) plus the rotation's seventh (GTS203 (a)) | read and print about 8 min after compression; eval about 15 min (hundreds of programs at 1 to 10 s each under hsi, eight at a time); AOT about 10 min (the Go builds of shards, under the heavy lock) |
| clean (2.4 h) | every part over the compressed corpora, audited; the rotation's record complete; the dumps rebuilt from GHC in the fresh clone | the dumps of the boot libraries about 5 minutes (as the interface level's compiles); the rest within 2.4 h if the nightly is within 35 min |

**Why the testsuite whole cannot be nightly.** GHC's own validate runs the testsuite in tens of minutes on many cores
natively (to verify); each run test is a GHC compilation (about 1 s, E2) plus a run that hsi makes 10 to 50 times
slower (§6.8). The references and dumps are recorded once per pin, so a night pays only for hsi's runs; the coverage
subset carries the night, the rotation the week, and per-test CPU limits (GTS102, GTS208) catch the slow ones (nofib's
`real` programs, the `concurrent` stress tests).

## 11. Plan (HE1-HE11)

Estimates in working days, as the other surveys give them. Each milestone ends with its check green and its records.

| milestone | content | exit criteria | days |
|---|---|---|---:|
| **HE1** the spec and pins | `doc/haskell/STG-SPEC.md`: STG forms for every `GHC.Stg.Syntax` constructor of 9.14.1, Ids, data constructors, representations, literals, foreign calls (§4); pins (the testsuite tarball's sha256, nofib's commit, `test/haskell/eval-corpora.edn`); the testsuite's run tests counted; samples | the spec accepted; every corpus pinned and counted | 2-3 |
| **HE2** the STG dump | `tools/hsdump stg` and `core` (the hook, §5.2); the boot libraries compiled as hadrian did (HES6) with the bignum backend of HES7 and dumped; `bin/hsi dumps` (each dump read back by Arbace's reader, every head known, two runs byte for byte equal); sizes and times recorded; the binary image format and its loader | every module of the 41 packages and every sample dumped and checked; the image loads in a measured time | 3-5 |
| **HE3** references and synthetic corpora | `bin/hsi record-ref`, `test/haskell/ref-ghc914.edn`; the generator of §8.3; the testsuite's `all.T` read (HES17), its run tests listed with their options | references for the synthetic, the language-core tests and nofib | 3-5 |
| **HE4** hsi's machine | `go/arbace/hs`: heap objects, the explicit-stack eval/apply loop, updates, blackholes, PAPs; the 742 primops; exceptions and masks; green threads, MVars, `delay#`; the top level (`runMainIO`, argv, exit); the first C ports (the I/O library's POSIX calls on `crt`); `bin/hsi run` | the samples and the synthetic corpus run or classed; `Hello`, `T.hs`, `U.hs`, `L.hs` (Appendix A) byte for byte | 15-25 |
| **HE5** the libraries' C and the rest of the RTS | every C symbol the corpora reach, libm through musl's C forms, the `cbits` of `base`, `bytestring`, `text`, `containers`, `unix`, `directory`, `process`, `time`; STM; asynchronous exceptions, `timeout`, deadlock detection; weak pointers, finalizers, stable names; `prompt#`/`control0#` | the testsuite's language-core run tests and nofib: none failing, every non-pass classed | 8-14 |
| **HE6** the check and the classes | `bin/hsi check`, `divergences`, controls (GTS81 (a)), the coverage subsets and rotation slices (GT8), the testsuite's run tests whole | every run test in a result or a class; the nightly within its share | 4-6 |
| **HE7** the REPL | the helper as a GHC session server (statements, `let`, `it`, `:t`, `:k`), hsi's REPL holding the session's STG | a scripted session byte for byte against GHCi's output for expressions and `let` | 3-5 |
| **HE8** AOT | STG → Go forms (§7), the libraries compiled once per pin, `bin/hsi check --aot`, hsi against compiled; hsi with compiled libraries (HES14 (b)) | none failing; every refusal with a reason; speed recorded | 10-16 |
| **HE9** read and print extended | the testsuite's and nofib's sources read and printed at the four levels; the behaviour level (the printed run programs compiled by GHC and run against the originals' references) | every file in a result or a class; the print cell's behaviour level recorded | 3-4 |
| **HE10** the row | `cells.edn` `:haskell`, classes files, `bin/reap`, essential subsets, steps (`:parts` per corpus), Makefile | `bin/reap`: Haskell's four cells checked, none failing; `bin/gate --costs` within the share | 1-2 |
| **HE11** the milestone | `bin/gate --full --clean` with Haskell's steps within budget, the embedding and re-seed if decided (HES19), the tag | the tag | 1-2 |

**Total: about 54 to 88 working days** (HE1 2-3, HE2 3-5, HE3 3-5, HE4 15-25, HE5 8-14, HE6 4-6, HE7 3-5, HE8 10-16,
HE9 3-4, HE10 1-2, HE11 1-2). HE1-HE3 (8 to 13 days) are a trial with no runtime commitment: they give the dumps and
the references, from which the size of the long poles is re-estimated. HE7 and HE9 can run beside HE4-HE6. A standalone
front end (HES21) is not in the total.

**Sonnet and Opus** (the user's split): Sonnet for HE1's spec from `GHC.Stg.Syntax`, the helper's serializer (Haskell,
mechanical once the heads are fixed), HE3's references, `all.T` reading and the generator, the libc and libm ports'
tables, HE6's classing, HE9, HE10. Opus for the machine (stack, updates, blackholes, the scheduler), asynchronous
exceptions and STM, the deadlock walk, the memory model's join with `crt`, the AOT's trampolined code, HE5's harder
`cbits`.

**Re-seed needs.** HE1-HE3 change tools' namespaces and the helper only (evaluated from the checkout, out of
`gate_key`). `go/arbace/hs` is evaluated by goi until listed in goi's table and compiled; embedding hsi in the shipped
program waits for a re-seed batched with the other crops' (CS250's decided line).

## 12. Proposals (HES1-HES22)

Each with options, the recommended one first.

- **HES1 The reference and its pins.**
  - Options: (a) GHC 9.14.1 as on the host (Alpine `ghc-9.14.1-r2`, `tools/hsdump/GHC_VERSION`), the source tarball
    already pinned by repr/haskell, the release's testsuite tarball (`ghc-9.14.1-testsuite.tar.xz`, sha256 pinned, to
    verify that the release ships one) and nofib at a pinned commit, fetched at HE1; (b) a GHC bindist pinned by hash in
    `.tmp/` (independent of Alpine's moves); (c) GHC 9.12 (the previous series).
  - *Recommended: (a)*, (b) if Alpine edge moves before HE11 (repr/haskell's H11 (a) already says so).
- **HES2 Names.**
  - Options: (a) `hsi` (`bin/hsi`), the runtime `go/arbace/hs`, STG forms with heads `stg/...`, the spec
    `doc/haskell/STG-SPEC.md`, milestones HE1-HE11, proposals HES (HES30 onward after HE1); (b) `ghi` (close to GHCi);
    (c) `stgi` (an existing project's name, §2.3).
  - *Recommended: (a)*.
- **HES3 What eval interprets.**
  - Options: (a) GHC's final STG (after unarisation) from the helper: the representation GHC's three back ends share,
    evaluation order fixed, untyped with representations; (b) Core (`-fwrite-if-simplified-core`'s `extra decls`, or
    the simplifier's output) with Arbace's own CorePrep and CoreToStg (saturation, A-normal form, unarisation: some
    thousands of lines re-done, typed and more stable across releases); (c) an Arbace front end (renamer, type checker
    with classes, families, GADTs and the boot libraries' extensions: far beyond one crop); (d) GHCi's bytecode
    (`-ddump-bcos`): an internal format tied to `rts/Interpreter.c`.
  - *Recommended: (a)*: the same STG the judge's code is generated from, so evaluation order and imprecise exceptions
    agree; (b) if STG's changes between releases cost more than CorePrep's re-implementation would.
- **HES4 How the helper captures STG.**
  - Options: (a) a GHC API driver in `tools/hsdump` with the hook between STG and Cmm, serializing the STG bindings,
    the data constructors and the foreign stubs, the compilation continuing to the object; (b) a plugin (Core-to-Core
    plugins see Core only; a later GHC's STG plugin point if any, to verify); (c) `-ddump-stg-final`'s text scraped
    (lossy, E2).
  - *Recommended: (a)*, as `hsdump` already drives GHC's API.
- **HES5 Prior art's role.**
  - Options: (a) references: Hruska's external STG interpreter's primops and FFI as a checklist, GHC's `rts/` and
    `StgToJS` as the models, no code vendored; (b) vendor the external STG format and its exporter (a patched GHC, not
    the host's); (c) none.
  - *Recommended: (a)*: the host's GHC and its API serve; a patched GHC is a second compiler to pin.
- **HES6 The libraries' compilation.**
  - Options: (a) every boot library compiled from the pinned tarball as hadrian compiled the installed ones (unit ids,
    relative paths, the release flavour's optimization levels, verified against the installed `.hi` files), the user's
    programs at `-O0`; (b) everything at `-O0` (simpler; the library's STG then differs from what the reference
    linked: `trace`, imprecise exceptions and `unsafePerformIO` inside libraries may differ); (c) everything at `-O1`.
  - *Recommended: (a)*: the reference program links the installed libraries, so their STG must be theirs.
- **HES7 The bignum backend.**
  - Options: (a) `ghc-internal` compiled with GHC's pure-Haskell native backend (`GHC.Internal.Bignum.Backend.Native`,
    the flag's name to verify), so `Integer` and `Natural` need no C (results identical; speed lower; the backend's name
    is the one observable difference, a class); (b) the 31 GMP functions of E5 ported to Go (math/big's internals are
    not exported); (c) GMP read as C forms and run by ci (LGPL; its generic C only).
  - *Recommended: (a)*; (b) at HE8 if `Integer`-heavy programs are too slow.
- **HES8 The machine.**
  - Options: (a) eval/apply with an explicit heap-allocated stack per thread, eager blackholing, the loop a Go `for`,
    code as closure trees compiled at load (§6.3); (b) direct style on Go's stack with goroutines as Haskell threads
    (fast calls; E7's deep chains, `<<loop>>`, asynchronous exceptions and `control0#` lost or rebuilt); (c) push/enter.
  - *Recommended: (a)*, as si's heap frames for Scheme (SC5) and PYS7's frames for Python.
- **HES9 Haskell threads.**
  - Options: (a) green threads on hsi's scheduler with the non-threaded RTS's semantics (one capability, `forkOn` as
    `fork`, STM transactions unpreempted), `-threaded` a class; (b) goroutines (the threaded RTS's semantics: MVars,
    TVars, blackholes and the heap need locks; nondeterminism in the judge's runs); (c) no concurrency in v1.
  - *Recommended: (a)*: the references are non-threaded runs (GHC's default).
- **HES10 Memory for `Addr#` and pinned data.**
  - Options: (a) `crt`'s flat address space (C23-SURVEY §6.3) for `Addr#`, `malloc` (musl's), pinned byte arrays and
    string literals, shared with ci, so C ports and C read as forms see the same bytes; (b) `Addr#` as a Go slice and
    an offset of hsi's own (no C interop beyond hand-written ports); (c) `unsafe.Pointer` into Go memory (GC hazards).
  - *Recommended: (a)*.
- **HES11 The C the libraries call.**
  - Options: (a) a Go table by symbol, ported as the corpora reach them (measured at HE5), libm through musl's C forms
    (cf2g) for bit-exact results, the unreached a run-time class with the symbol's name; (b) (a) plus the packages'
    `cbits/*.c` read by repr/c and run by ci where a port is long (`text`'s simdutf is C++: a port or a class), and a
    user program's own C the same way, later; (c) GHC's RTS read as C forms (it assumes its own heap and stacks: no).
  - *Recommended: (a)*, (b) studied at HE5.
- **HES12 Primops.**
  - Options: (a) the 742 non-SIMD primops in Go, checked one by one against `primops.txt.pp` and the synthetic corpus,
    SIMD, compact regions, BCOs, closure inspection and profiling primops a class; (b) SIMD too (Go has no portable
    vectors: element loops); (c) only those the corpora reach.
  - *Recommended: (a)*; (b) when a corpus needs it.
- **HES13 Where the dumps live.**
  - Options: (a) cached by key under `.tmp/` (GHC's pin, the sources' digests, the flags), rebuilt by the helper in
    minutes, so eval's checks need the host's GHC as read's and print's do today; (b) committed as data per corpus
    (OC4 (a)'s way: checks need no GHC; sizes of the order of 100 MB to verify, compressed); (c) the boot libraries'
    image committed, the programs' dumps cached.
  - *Recommended: (a)* in v1, (c) when the standalone direction asks for it, the sizes measured at HE2.
- **HES14 How hsi loads the libraries.**
  - Options: (a) a binary image of the libraries' STG (compiled code trees serialized, one file per pin and
    configuration) loaded per run, its load time measured; (b) the libraries AOT-compiled into hsi (GHCi's own model:
    libraries native, the program interpreted), at HE8; (c) the forms read per run (too slow, §4.2).
  - *Recommended: (a)* until HE8, then (b).
- **HES15 AOT's path.**
  - Options: (a) STG → Go forms over `go/arbace/hs`, trampolined, sharing the stack and heap objects (StgToJS's
    shape); (b) STG → Java forms → j2g (Eta's route); (c) Cmm → Go (needs GHC's RTS in Go).
  - *Recommended: (a)*, as Scheme's SC9 (a) and Python's PYS13 (a).
- **HES16 The run judge.**
  - Options: (a) status, stdout and stderr byte for byte, the program name fixed, `LANG=C.UTF-8`, `TZ=UTC`, `GHCRTS`
    unset, no masks by default (masks per class where timings or `GHC.Stats` print); (b) stdout only; (c) stderr's
    last line only (as Python's default).
  - *Recommended: (a)*: GHC's stderr is deterministic and its backtraces are Haskell (E6), so it can be judged whole.
- **HES17 The testsuite's runner and judge.**
  - Options: (a) Arbace's own runner: `all.T`'s common forms (`test(name, opts, compile_and_run, [flags])`, `extra_files`,
    `exit_code`, `ignore_stdout`, `only_ways`, `when(...)`) read as data (by repr/python's reader once it exists, a
    small reader of that subset until then), each test a case judged against the host's recorded run and its expected
    files, tests beyond the subset a class; (b) GHC's Python driver (`testsuite/driver`) with a `TEST_HC` wrapper that
    compiles with GHC and makes the "executable" a launcher of hsi (its normalisers for free; the host's Python at
    check time); (c) only single-module tests with a `.stdout` (ghc-lisp's method).
  - *Recommended: (a)*, as goi reads `$GOROOT/test`'s directives itself; (b) as a cross-check at HE6.
- **HES18 The REPL.**
  - Options: (a) the helper as a GHC session server (InteractiveContext: each statement typed and compiled to STG by
    GHC, its value shown by `show` as GHCi's `print`), hsi holding the bindings, at HE7; (b) programs only in v1; (c)
    GHCi itself with hsi as its external interpreter (`-fexternal-interpreter`'s protocol runs bytecode, not STG: no).
  - *Recommended: (a)*.
- **HES19 Embedding.**
  - Options: (a) the helper's modes and hsi's tools evaluated from the checkout, `go/arbace/hs` evaluated by goi, out of
    `gate_key`, until the row fits the budget, then embedded at a re-seed batched with the other crops'; (b) embedded
    from HE4.
  - *Recommended: (a)*, as the crops since CS250 and PYS18.
- **HES20 Print.**
  - Options: (a) as built (R1-R6, HR1-HR28): the four levels are the print cell, plus the behaviour level of HE9; STG
    forms are never printed as Haskell (a `-ddump-stg-final`-like text printer for debugging only); (b) a Core level
    (the print compiled, its Core equal to the original's: REPR-HASKELL-SURVEY H8's "Core later"), beside the interface
    level; (c) nothing new.
  - *Recommended: (a)*; (b) if the interface level proves too weak (it already compares `-O1` interfaces with
    unfoldings).
- **HES21 A standalone front end (later).**
  - Options: (a) a later survey: GHC itself (the installed `ghc` library, about the size of javac's world many times
    over) compiled to STG by the host's GHC and through hsi's AOT, as javac runs on Arbace (JN3-JN6), making the front end
    Arbace's executable; (b) MicroHs on hsi (smaller; its own `base`, so its STG is not the judge's); (c) an Arbace type
    checker for Haskell (years).
  - *Recommended: (a)*, after HE11, its survey first.
- **HES22 The crop's place in the order.**
  - Options: (a) after the tag `arbace-reaps-4-crops-v1` and GT8's compressed corpora: HE1-HE3 (light: a spec, the
    helper's modes, references) any time beside Python, HE4 onward when the night budget has room, on the main host;
    (b) on the Mac VM after Python (PYS19's line); (c) beside Rust's eval survey, whichever row is nearer first.
  - *Recommended: (a)*; (b) if the main host stays on GT8 and the other crops.

## 13. What to decide first

HES3 (what eval interprets) and HES4 (how the STG is captured), then HES6 and HES7 (the libraries' compilation and
bignum backend), HES8 (the machine), HES10 (memory), and HES22 (when). The rest follow. A cheap trial is HE1-HE3: the
dumps of the boot libraries, their sizes, and the references, with no runtime commitment.

## 14. Risks

- **The libraries' STG must be the installed libraries'** (HES6): hadrian's flags, unit ids and source paths
  reproduced exactly, or `HasCallStack` text (E6) and library-internal evaluation order differ. Checked at HE2 by the
  interfaces: the dumped compilation's `.hi` files equal to the installed ones (repr/haskell's interface level already
  compares interfaces), up to the bignum backend's modules.
- **The RTS's semantics are many and subtle**: asynchronous exceptions and masking, `BlockedIndefinitely` detection,
  finalizer timing, scheduling points, `timeout`. The testsuite's `concurrent` directory exercises them; classes may
  be large, each with its reason.
- **Speed** (§6.8): hsi at 10 to 50 times `-O0` native narrows the nightly share; nofib's `real` programs and the
  testsuite's stress tests may need the AOT or per-test limits.
- **The C tail** (E5): `iconv`, `process`'s `fork`/`exec`, terminal handling (`haskeline`, `terminfo`), `text`'s C and
  C++ (`simdutf`, to verify), sockets (not a boot library) as classes or ports; a program importing an unported one is a
  class, not a failure.
- **STG changes between GHC releases**: the helper's serializer and the spec follow `GHC.Stg.Syntax`; a new pin means a
  new spec revision (as repr/haskell's tree did, REPR-HASKELL-SURVEY E3).
- **The front end stays GHC's** (HES3): the row is not standalone in v1; the standalone path (HES21) is a long one.
- **Dump sizes** (§4.2): of the order of 100 MB or more of text for the libraries (to measure): the binary image and
  the cache keep it off the nightly's time, not off the disk.
- **Moving pins**: Alpine edge may move GHC (to 9.14.2 or 9.16, to verify); the apk's version recorded, HES1 (b) the
  fallback.
- **Estimates**: HE4's range (15-25) is the widest; the primops are many but mechanical, the machine is the risk. The
  plan is re-estimated after HE3's numbers and HE4's first runs.

## 15. Sources

- The host: `/usr/bin/ghc` 9.14.1 (Alpine `ghc-9.14.1-r2`), `ghc --info`, `ghc --show-options`, `ghc --show-iface`,
  `/usr/lib/ghc-9.14.1/lib/x86_64-linux-ghc-9.14.1-07a2/` (`ghc-internal-9.1401.0-6d86`, `base-4.22.0.0-3875`,
  `ghc-bignum-1.4-aa8c`, `ghc-prim-0.13.1-b8e0` and their `.a` archives, E5), `runghc`.
- Upstream (not downloaded; to verify at HE1): gitlab.haskell.org/ghc/ghc at tag `ghc-9.14.1-release`:
  `compiler/GHC/Stg/Syntax.hs`, `compiler/GHC/Stg/Unarise.hs`, `compiler/GHC/CoreToStg.hs`, `compiler/GHC/CoreToStg/Prep.hs`,
  `compiler/GHC/StgToJS/`, `compiler/GHC/StgToByteCode.hs`, `compiler/GHC/Driver/Hooks.hs`,
  `compiler/GHC/Builtin/primops.txt.pp`, `rts/` (`Schedule.c`, `RaiseAsync.c`, `STM.c`, `Threads.c`, `Interpreter.c`,
  `PrimOps.cmm`, `Updates.cmm`), `libraries/ghc-internal/` (`GHC/Internal/TopHandler.hs`, `GHC/Internal/Bignum/Backend/`,
  `cbits/`), `hadrian/src/Settings/` (the release flavour), `testsuite/` (`driver/`, `tests/`, `mk/`); the release
  tarballs at downloads.haskell.org/ghc/9.14.1/ (`ghc-9.14.1-src.tar.xz` pinned by repr/haskell;
  `ghc-9.14.1-testsuite.tar.xz` to verify); gitlab.haskell.org/ghc/nofib; the GHC User's Guide (the RTS options,
  concurrency, the FFI chapter).
- Prior art (references, not vendored; licenses to verify): grin-compiler/ghc-whole-program-compiler-project (Csaba
  Hruska: external STG, the STG interpreter), augustss/MicroHs (Apache-2.0), Hugs (haskell.org/hugs), GHCJS (MIT), Eta
  (typelead/eta, BSD-3), Frege (BSD-3), stgi (quchen/stgi, BSD-3), ministg; the papers of §2.3.
- Arbace: CLAUDE.md; doc/REPR-HASKELL-SURVEY.md (H1-H14, §5.2 "Translators"), doc/haskell/SPEC.md,
  doc/haskell/HASKELL-NOTES.md (R1-R6, HR1-HR28; R5's times); doc/ocaml/OCAML-SURVEY.md (OC4, OC5, O4, O5: the
  oracle's IR interpreted); doc/scheme/SCHEME-SURVEY.md (§5.2 heap frames, §6 AOT); doc/python/PYTHON-SURVEY.md (this
  file's shape); doc/C23-SURVEY.md (§6.3, the memory model; CS250); doc/GATE-SURVEY.md, "GT8, the test budget" and its
  decisions; doc/FULL-SUPPORT-SURVEY.md; `tools/hsdump`, `test/haskell/corpus-ghc-9.14.1.edn`, `go/arbace/{oi,scm,crt}`.

## Appendix A: the scratch

In the worktree `.tmp/worktrees/haskell-eval-survey`, `.tmp/haskell-eval/` (gitignored): `T.hs` (E2, E3: a class and
instance, a lazy sieve, `Data.Map.Strict`, `IORef`, `Integer`, `try`/`evaluate`; its `T.dump-simpl`, `T.dump-prep`,
`T.dump-stg-final` at `-O0` and `-O1`, `T.hi` with `-fwrite-if-simplified-core`), `U.hs` (E6: `trace` order and an
uncaught `Map.!`), `L.hs` (E7: a 3,000,000-deep `foldl` and `<<loop>>`), `F.hs` (E8: `nfib 32` at `-O0`, `-O1` and under
`runghc`), `E.hs` (`runghc`'s start), `b.ghci` and `prim.txt`, `primnames.txt`, `nosimd.txt` (E4: `:browse GHC.Prim`),
`u-*.txt`, `d-*.txt`, `ext-*.txt` (E5: `nm` of the four archives), `test.log` (`bin/repr-haskell test`, passing, run
with `ARBACE_HOST_EXE` the main checkout's stage 0). Each probe took seconds; the longest was `bin/repr-haskell test`.

## Decided (2026-11-03, the user): HES1-HES21

All as recommended. The trial HE1-HE3 starts now, by an agent on the main host, beside GT8 (light work by day).

## As built: HE1-HE3 (write-ahead)

Built 2026-11-03 on branch `haskell-he1` from main `c545ede1`, on the host's GHC 9.14.1 and the main checkout's stage 0.
Write-ahead: the whole boot-library dump and the whole testsuite reference are night runs (below), not yet run; the
numbers here are those of the runs made by day.

### HE1: the spec and the pins

- **`doc/haskell/STG-SPEC.md`**: STG forms for every constructor of the 23 declarations GHC 9.14.1's final STG is made
  of (`GHC.Stg.Syntax`, `AltCon`, `Literal`, `LitNumType`, `PrimRep`, `PrimElemRep`, `Levity`, `CCallTarget`,
  `CCallConv`, `Safety`, `GenTickish`, `StrictnessMark`, ...: 89 constructors), names (§3: strings, external
  `"UNIT:MODULE/OCC"`, internal `"OCC_N"` numbered per module), representations (§4), literals (§5), data
  constructors (§8), Core forms (§9), choices STG-S1-STG-S13. `bin/hsi spec-check` extracts the constructors from the
  pinned source tarball and requires each in the checklist (a spec with one renamed fails: its negative control).
- **Pins** (`test/haskell/eval-corpora.edn`, `bin/hsi pins --fetch|--count|--check`): the testsuite tarball
  `ghc-9.14.1-testsuite.tar.xz` (8,154,160 bytes, SHA-256 `84aefc21...`, the line of downloads.haskell.org's
  SHA256SUMS, the same host's: no second source; SHA-512 recorded); nofib at `d4750745` (master's head, 2026-07-10;
  tree `1f5a8852` checked; no licence at its top: read in place, never committed); the source tarball repr/haskell's.
- **Counted**: the testsuite's 520 `all.T` read by `test/haskell/eval/allt.ae` (HES17 (a): Python's lexical syntax and
  the calls `test(...)`, `setTestOpts(...)` at any depth, none unread): 10,286 tests, of them **1,713 run tests**
  (`compile_and_run` 1,594, `multimod_compile_and_run` 94, `multi_compile_and_run` 22), listed with their options in
  `test/haskell/eval/testsuite-run.edn`; besides them 980 `makefile_test` and 27 `run_command` (shell scripts),
  3,742 `compile`, 2,374 `compile_fail`, 582 `ghci_script`. The survey's guess of 3,000 to 4,000 run tests was high.
  11,227 `.hs`/`.lhs` files (311,309 lines). The language core (§8.2's nine directories): 701 run tests. nofib: 144
  programs (imaginary 15, spectral 46, real 29, shootout 8, gc 15, parallel 19, smp 12), 628 files. Samples:
  `test/haskell/eval/samples/` (10: Hello, the survey's T/U/L/F as Survey, Trace, Loop, Nfib, and Conc, Exc, Stm,
  Arrays, Echo with its stdin and arguments).

### HE2: the STG dump

- **The helper** (`tools/hsdump/Stg.hs`; `hsdump stg [--core] [--one-shot]`, `core`): GHC's `stgToCmmHook` writes each
  module's `CgStgTopBinding`s and data constructors, then runs GHC's own `StgToCmm.codeGen` (the objects and
  interfaces as without it); `--core` adds a static plugin's pass at the end of the Core pipeline. Built `-threaded`
  (`bin/repr-haskell helper`; its key changed). Two of GHC's lazy fields are panics when forced, found by the
  libraries: a `StgConApp`'s `[[PrimRep]]` rebuilt by tag inference (`mkSeqs`), written only when it evaluates; and
  `typePrimRep` of a representation-polymorphic type, written `:rp` (STG-S13; 4,095 in the five packages below).
- **HES6 reproduced exactly**, checked by the interfaces' ABI hashes against the installed ones: equal for every
  module of `ghc-internal` (267 of 267, with the GMP backend), `base` (241), `containers` (35), `ghc-prim`,
  `ghc-bignum`. What it takes: the installed unit ids (every package's hashed id, `containers-0.8-b42e`; `ghc-internal`
  alone wired-in, plain); paths from the source tree's root (`libraries/...`; `.tmp/hsi/root` with `libraries` linked
  to the extracted tarball and hadrian's `_build/stage1/.../build` for the `.hsc` and generated modules); `-O2`;
  hsc2hs with hadrian's `-Dx86_64_HOST_ARCH=1 -Dlinux_HOST_OS=1` (without them `ExecutablePath` takes another branch);
  and **one-shot compilation**, a process per module in dependency order as hadrian's `ghc -c` (`--one-shot`, `-j8` of
  them): `ghc --make` rehydrates an hs-boot loop with the real interfaces, so 50 modules' unfoldings lost the
  `noinline` GHC puts around an id seen through a boot file; and one process compiling the modules in order keeps the
  interfaces it read (wrong exports). The check discriminates: `-O1`, or paths from the package's directory, give
  other hashes.
- **HES7** (native bignum): 190 of `ghc-internal`'s 267 ABI hashes differ from the installed, but its STG differs
  from the GMP build's in 5 modules only (`Bignum.Backend.Native`, `BigNat`, `Integer`, `Natural`, `Real`): the rest
  is the hashes' cascade. `base`'s and `containers`' STG name no GMP backend id; no `__gmpn_*` call remains.
- **`bin/hsi dumps`** (`arbace.repr.haskell.stg`, `test/haskell/eval/dump_plan.ae`): every dump read back by Arbace's
  reader on stage 0 and checked against the spec's grammar (every head known at its place), 0 errors; `--again`: two
  runs byte for byte equal (`ghc-internal`, `containers`, the samples, 2026-11-03).
- **Sizes and times** (five packages and the samples; `.tmp/hsi/dumps/report.edn`): `ghc-internal` 268 modules,
  **32.0 MB**, 100 s to compile at `-O2` one-shot; `base` 3.7 MB, 9 s; `containers` 9.5 MB, 24 s; `ghc-prim`,
  `ghc-bignum` 1 KB each; the samples 95 KB (10 KB each at `-O0`). In all 555 dumps, **45.3 MB**, read back and
  checked in 4 s on 8 jobs; peak 0.95 GB. The 41 packages' 1,319 modules are a night run: about 100 to 150 MB of
  forms by these ratios (`Cabal`, `Cabal-syntax`, `text`, `bytestring` the large ones), about 10 to 15 minutes.
- **The inventory** (the five packages): 735 distinct primops called (of 742 non-SIMD), 195 foreign symbols (519
  `ccall`, 83 `capi` call sites; `__hscore_*`, libc, the RTS's `arenaAlloc`, `cmp_thread`, `epoll_*`...), 21 Cmm
  `primcall`s (`stg_cloneMyStackzh`, the stack decoder's `getStackClosurezh`...); 137,838 `app`, 97,318 `case`, 72,535
  closures (36,595 `:r`, 29,026 `:u`, 4,733 `:j`, 2,181 `:s`), no ticks.
- **Not built**: the binary image and its loader (the survey's HE2 row; not in this trial's brief): HES32.

### HE3: references and synthetic corpora

- **`bin/hsi record-ref`** (`bin/lib/hsi/record-ref`, `test/haskell/eval/run_plan.ae`) writes
  `test/haskell/ref-ghc914.edn`: each program built by `ghc -O0 -rtsopts` in a fresh copy of its directory and run
  under `env -i`, `LANG=C.UTF-8`, `TZ=UTC`, 10 s of CPU, 30 s of wall time; status, stdout's and stderr's SHA-256, sizes
  and first lines, a testsuite test's or nofib program's expected stdout file compared (`:expected`), and every
  program's build and run seconds (GT8); `--only` keeps the other lines. Recorded 892 programs in 2.5 minutes, 8 at a
  time: samples 10, synthetic 37 (35 exit 0, `runtime-exceptions` and `runtime-loop` 1 as meant), the language core
  701 (623 exit 0, 56 another status, 46 of them all.T's `exit_code`, 20 not built: their C sources or objects,
  which the plan does not pass yet, 2 timed out: `T4262`, `hs_try_putmvar001`; stdout as the expected file 528,
  differs 38, none 115), nofib 144 (78 exit 0, 59 not built: packages not installed, `parallel`, `random`,
  `old-time`, and modules hidden without their `-package`; expected 53 same, 7 differ). Build seconds 828, run
  seconds 206 in all.
- **The generator** (`test/haskell/eval/gen.ae`, `bin/hsi generate [--check]`, §8.3): from the tarball's
  `primops.txt.pp` (through cpp, as hadrian makes it) one program per scalar section (15: `Char#`, the eight sized
  integers, `Int#`, `Word#`, narrowings, `Double#`, `Float#`, fused multiply-add) covering **340 of the 585 primops**
  (those with scalar arguments and results), each over every combination of boundary values (64 at most a primop;
  a division's zero divisor and minBound by -1, an unchecked shift past its width, `quotRemWord2#` with a quotient that
  does not fit left out: traps); 14 class programs (Show, Read, Eq/Ord, Enum, Bounded, Num, Integral, Real/Fractional,
  RealFrac, Floating, RealFloat, Bits, FiniteBits, Ix at their standard instances); 8 runtime programs (laziness and
  `trace` order, deep chains, `<<loop>>`, exceptions and masks, threads and deadlock, STM, Integer at every size,
  text I/O). Deterministic (its boundary values are its seed); 37 files, 11,469 lines, all built by GHC.

### Night runs (listed, not run)

1. `bin/hsi dumps --samples --again`: the 41 packages twice (their ABI hashes, sizes, times, determinism).
2. `bin/hsi record-ref --corpus samples,synthetic,testsuite,nofib`: the 1,713 run tests (about 6 minutes by the day's
   rate).
3. `bin/hsi pins --check`, `bin/hsi spec-check`, `bin/hsi generate --check` (seconds; passing 2026-11-03).

### Proposals (HES30 onward, HES2 (a)'s numbering)

- **HES30 The libraries' compilation mode.** (a) one-shot, a helper process per module (as built: the only mode whose
  interfaces are the installed ones); (b) `--make` (faster, one process; 50 modules differ in unfoldings).
  *Recommended: (a)*.
- **HES31 HES7's check.** (a) at each pin, the native build's STG compared with a GMP build's (`HSI_BIGNUM=gmp`),
  the differing modules a recorded list (today 5), and the GMP build's ABI hashes all equal to the installed;
  (b) the native build's ABI hashes alone (190 differ: says nothing). *Recommended: (a)*.
- **HES32 The binary image.** (a) at HE4 with hsi's code trees (its format follows the loader's needs); (b) now, a
  serialized form tree. *Recommended: (a)*: the forms read at 11 MB/s (45 MB in 4 s on 8 jobs), so per run cost
  matters only for hsi's start.
- **HES33 Where the dumps live** (HES13 (a) as decided, the size now measured): cached by key under `.tmp/` (GHC's
  pin, the tarball's, the helper's hash, the plan); committed never in v1 (100 to 150 MB).
- **HES34 The testsuite's tests beyond the run functions.** (a) `makefile_test` (980) and `run_command` (27) a class
  in v1 ("a shell script's test"), their Makefiles read later; (b) read now. *Recommended: (a)*.
- **HES35 The runner's C and packages.** (a) record-ref passes a test's C sources and objects (the
  `extra_files`/`multi_compile` lists, `*_c.c`) at HE6, and nofib's missing packages are a class (`parallel`, `random`,
  `old-time`, `unboxed-ref`: not boot libraries); (b) fetch those packages by pin. *Recommended: (a)*.
- **HES36 `:rp`** (STG-S13). (a) as built; (b) the kind's variable written. *Recommended: (a)*.

### Open questions for the user

- The testsuite tarball has a single source for its digest (downloads.haskell.org's SHA256SUMS, unsigned on this
  host): accept, or ask the Mac VM for a second fetch?
- May the night runs above use the main host tonight (the dumps are under the heavy lock, about 15 minutes)?

## Decided (2026-11-03, the user): HES30-HES36

All as recommended. HE4 starts (the machine and its primops), its first step below.

## As built: HE4a (write-ahead)

Built 2026-11-03 on branch `haskell-he4` from `haskell-he1` (`349a2b58`), on the host's GHC 9.14.1, TamaGo's go and the
main checkout's stage 0 (`ARBACE_HOST_EXE`), light work by day (no lock, at most 8 jobs, the longest run a minute).
Write-ahead: HE4's first coherent step, not the milestone; its exit criteria (§11) are not all met (below, "The rest of
HE4"). Nothing of Haskell is in `gate_key` or a gate step (HES19 (a)).

### What is in the tree

- **`go/arbace/hs.ae`, `go/arbace/hs/*.ae`**: hsi's runtime, the package `arbace/hs` (12 files, about 4,800 lines of Go),
  written in Go and converted by repr/go's native reader (`bin/repr-go read --goos linux --goarch amd64`, mode `:lines`,
  as oi's, OC70): the forms are the source, printed back by `bin/hsi build`. It imports std and `arbace/crt` (ci's
  address space and system calls, HES10 (a)). The parts:
  - `read`: a reader of the dumps' syntax (STG-SPEC §1) in Go: Arbace's reader reads the forms at 11 MB/s on 8 jobs
    (HE2), Go's at about 100 MB/s on one, so a run loads its modules' text directly (HES32 (a)'s image deferred, below).
  - `load`: the units indexed by their dumps' `:unit` (`hsi run --lib DIR...`: `.tmp/hsi/dumps/stg`'s packages and the
    program's directory, unit `main`); a module loaded when a name of its is first needed (lazily by module, HES14
    (a)'s first half), in three phases (binders; objects; constructors' fields, other modules loaded as they need);
    a top-level closure compiled when first entered. `Hello` loads 52 modules (13.3 MB of forms) and runs in 0.16 s;
    `Exc` 81 modules (21.1 MB) in 0.25 s.
  - `compile`, `nodes`: each closure's body a tree of nodes over the slots of one frame per closure (its free variables,
    its arguments, every local binder; scopes restored, so GHC's reused binder numbers are harmless); a join point
    (`letne`, `:j`) is no closure at all: its arguments are slots of its binder's frame and a jump assigns them (GHC's
    let-no-escape exactly); `case` pushes a frame unless its scrutinee is a pure primop, a literal or an evaluated
    variable (then the alternative is chosen at once); unboxed tuples are several return values (`MkSolo#`, `(##)`,
    `(#,#)`...; their binders positional).
  - `machine`: HES8 (a)'s eval/apply machine, the loop a Go `for`, never Go recursion: a thread's stack a slice of
    frames by value (case, update, apply, catch, mask, atomically, catchRetry, catchSTM); a thunk entered is
    blackholed by its thread and an update frame pushed (eager blackholing: re-entered by its own thread it raises
    `NonTermination`, `<<loop>>`; by another, that thread waits for its update); exact, partial (PAP) and
    over-saturated calls; `raise#` unwinds to the nearest catch frame, the thunks under evaluation updated to raise
    again; an asynchronous exception (`killThread#`, `throwTo`) unwinds them reverted to unevaluated; the handler
    runs masked interruptibly with an unmask frame when the catch was unmasked (GHC's `raisezh`); the masking states
    and their frames, pending asynchronous exceptions delivered at unmasking. Green threads (HES9 (a)): a run queue,
    slices of 20,000 nodes, MVars with FIFO single wake-up (takers, readers, putters), `delay#` on a timer list,
    `yield#`; when nothing can run and no timer waits, `BlockedIndefinitelyOnMVar` (`OnSTM`) raised in the threads
    blocked on them (GHC raises in the unreachable ones only; reachability is HE4e's). The program: `:Main.main`
    applied to the void token in the main thread; at its end, or at `shutdownHaskellAndExit`, `flushStdHandles` run
    as `hs_exit` runs it, then the status.
  - `stm`: HES9 (a)'s STM: a transaction is never preempted, so it runs alone to its commit or its `retry`; an undo log
    per transaction, `orElse` by marks, `catchSTM#`, `retry#` blocking the thread until a transaction commits a write.
  - `prim_scalar`, `prim_mem`, `prim_ctl`: the primops (below); `mem`: string literals in crt's read-only data
    (NUL-terminated, one per string), pinned byte arrays and `malloc` in arenas of crt's mapped slabs, an unpinned byte
    array a Go slice moved into crt's space when its address is asked (`byteArrayContents#`); `ffi`: 196 C functions
    by symbol (HES11 (a)), a `capi` wrapper's name decoded to its C function (`ghczuwrapperZC23ZC...ZCread` is
    `read`): the POSIX calls through crt's system calls (its fds 0-2 the process's), errno the machine's, the RTS's
    API (`getProgArgv`, `getMonotonicNSec`, the `getOrSet...Store`s, `errorBelch2`'s and `debugBelch2`'s formats,
    `cmp_thread`, ...), `localeEncoding` "UTF-8", base's MD5 (Typeable's fingerprints) on Go's `crypto/md5`, the
    floating-point predicates and `__int_encodeDouble`'s kin, libm on Go's `math` (the class `:libm`), and C's stdio
    for a program's own foreign calls (`printf`, `puts`, `putchar`: buffered to the end, as libc's stdout to a file).
- **`test/haskell/hsi/cmd.ae`, `cmd/main.ae`**: the light driver (`hsi run --lib DIR... --prog DIR --name NAME -- ARGS`,
  `hsi primops`, `hsi cfunctions`), as oi's `test/ocaml/oi/cmd.ae`.
- **`bin/hsi build`** (the forms printed by repr/go with `go/arbace/crt`, built by TamaGo's go into `.tmp/hsi/bin/hsi`,
  kept by the forms' digest; `HSI_EXE` names another), **`run FILE.hs [ARG...]`**, **`check`**
  (`bin/lib/hsi/check`), **`primops`**; the Makefile's `hsi-build`, `hsi-check`, `hsi-primops`.
- **`bin/hsi check`**: the samples and the synthetic corpus, each program dumped at `-O0` by the helper (cached by the
  source's and the helper's digests in `.tmp/hsi/progs/`) and run on hsi over the libraries' dumps as record-ref ran
  GHC's build (`env -i`, `LANG=C.UTF-8`, `TZ=UTC`, its stdin, arguments and name, CPU and wall limits), judged against
  `test/haskell/ref-ghc914.edn` by status, stdout's and stderr's SHA-256 (HES16 (a)); results in
  `test/haskell/check-hsi.edn` (`--record`); every non-pass in a class of **`test/haskell/divergences.edn`**, and
  every negative control of **`test/haskell/hsi-controls.txt`** (a run judged against another program's reference)
  failing, else the check fails (GTS81 (a)'s kind).
- The libraries' dumps the check runs on: `bin/hsi dumps` (HE2's five packages) and `bin/hsi dumps --packages
  array,stm,deepseq,transformers,mtl --out .tmp/hsi/dumps-extra` (33 s), `HSI_LIBS` naming others.
- **gate_key**: `go/arbace/hs{.ae,/}` and `bin/lib/hsi/` left out as oi's and scm's (`bin/arbace-go`'s `gate_key` and
  `gate_rest_key`, `test/gate/audit.ae`'s `:gate` and `:prog`; `test/gate/gate-key-crops-test.bash` extended and
  passing). `bin/lib/hsi/`'s HE3 scripts were in it on `haskell-he1`: no longer.

### Results

- **`bin/hsi check`: 44 of 47 equal** (status, stdout and stderr byte for byte): **all 10 samples** (Hello, Survey's
  sieve, `Data.Map` and `try`, Trace's order and uncaught `Map.!` with its `HasCallStack` backtrace, Loop's
  3,000,000-deep `foldl` and `<<loop>>`, Nfib, Conc's MVars, `threadDelay`, `Chan` and the deadlock detected, Exc's
  bracket, finally, masks, `killThread` and the uncaught exception's message, Stm's `retry` and `orElse`, Arrays'
  ST, unboxed arrays and `Integer`, Echo's stdin, arguments, program name and exit status 3) and **34 of the 37
  synthetic programs** (every class program but `class-floating`; every primop family but `Double#` and `Float#`;
  the 8 runtime programs). The 3 others are the class `:libm` (deferred, HES37): Go's `math` where GHC calls musl's
  libm, 9 lines of `primops-double`'s 1,029, 8 of `primops-float`'s, 15 of `class-floating`'s; and `decodeDouble_2Int#
  0.0`'s sign, which GHC's RTS leaves uninitialised. The controls fail as they must. 47 programs in about 30 s at 4
  jobs.
- **A probe of the testsuite's language core** (scratch, not a check yet: HE4c): its 555 single-module run tests with
  no flags and no run options (of 701; the 20 not built excluded) dumped and run the same way: **504 equal**, 27
  differ (13 of them killed at 20 s of CPU: hsi's speed; `concurrent`'s scheduling and `allocLimit`, `UnliftedWeakPtr`,
  `CarryOverflow`), 24 stopped by hsi (`createAdjustor`: `foreign import "wrapper"`, 12; packages without dumps, `unix`,
  `directory`, `bytestring`, `ghc-experimental`, `ghc-boot`, 6; a test's own C, `sleep`'s thread, a SIMD primop).
- **Primops** (`bin/hsi primops`, against `:browse GHC.Prim`'s 742 non-SIMD names, the survey's E4): **714
  implemented**; of the 28 others 7 are not primops at run time (`void#`, `realWorld#`, `nullAddr#`, `proxy#`: the
  loader's globals; `coerce`, `leftSection`, `rightSection`: gone by Core) and 21 remain: the 11 compact regions'
  (`compactNew#` ...), `prompt#`, `control0#`, `newPromptTag#` (HE4e), closure inspection (`unpackClosure#`,
  `closureSize#`, `getApStackVal#`, `annotateStack#`, `whereFrom#`'s kin), `mkApUpd0#`, `addrToAny#`, `newBCO#`
  (classes, HES12 (a)). The 340 scalar primops of the synthetic programs are checked against GHC's runs (all equal but
  the `:libm` family); the others by the samples and the probe only (HE4f checks them).
- **GHC's own behaviour, found and reproduced**: amd64's `minsd`/`maxsd` with GHC's operand order (a literal operand
  second: a NaN's or a zero's result depends on it); `Char#` compared as an unsigned word (`chr# (-1#)` shows `'\-1'`);
  the RTS's `hs_bitrev16`, which leaves bits 16-31 of its register (`bitReverse16#` of all ones is 4294967295: its
  instructions emulated from `rts/bitrev.o`); `dataToTag#` reached by a thunk (a lazy field: evaluated first); a
  class of one method's dictionary, the method itself, under an `alg` case with a default only; a thread killed before
  it ran ends silently.
- **Speed** (E8's `nfib 32`): 13 s on hsi, against 0.32 s native at `-O0` and 3.6 s in GHCi: about 3.6 times GHCi, in
  §6.8's range (1 to 5). A profile: half of it the Go collector scanning the loaded code (the live heap is mostly
  compiled code: GOGC 400 by default), the rest allocation (frames, argument slices, boxed `Int#`s) and dispatch.
  Loop (E7) 12.6 s against 0.78 s native.

### The rest of HE4 (sub-steps, re-estimated: 12 to 18 days)

1. **HE4b libm** (1-2 days): musl 1.2.6's libm functions GHC's code reaches (exp, log, pow, the trigonometric,
   hyperbolic and inverse ones, expm1, log1p, and their `float` versions) ported to Go from musl's sources (HES37),
   the class `:libm` empty but `decodeDouble_2Int#`'s uninitialised sign (a class of its own, `:design`).
2. **HE4c the testsuite's language core as a check** (2-3 days): `bin/hsi check --corpus testsuite-core` from
   record-ref's plan (flags, several modules, stdin, run options, the C sources HES35 (a) passes), every test in a
   result or a class; the packages the core imports dumped (`unix`, `directory`, `bytestring`, `process`, `text`).
3. **HE4d speed** (3-5 days): no allocation for over-saturated calls and returns to apply frames, preboxed small
   integers, a constructor's tag in a table for `case`, PAPs' arities, and HES32's image (the loaded code's load and
   scan); the target GHCi's speed or better (nfib 32 under 4 s), the 13 programs the probe killed under their limit.
4. **HE4e the RTS's semantics** (3-4 days): `throwTo`'s blocking when the target is masked (GHC's), deadlock by
   reachability (§6.5), `prompt#`/`control0#`, blackholes between threads under test, `threadStatus#`, finalizers
   (`finalizeWeak#`'s and the C finalizers at exit, `hs_exit`'s), stable names' hashes, `performGC`.
5. **HE4f the remaining primops and their check** (1-2 days): every primop of `primops.txt.pp` with its attributes in a
   table checked against hsi's (`can_fail`, `has_side_effects`, `out_of_line`); compact regions as copies (or a class),
   closure inspection, `mkApUpd0#`, `newBCO#` classes; synthetic programs for the array, `Addr#`, `MutVar#` and
   concurrency families (the generator's 245 primops not yet covered).
6. **HE4g the top level and the I/O** (2 days): files (`open`, `stat`, `lseek`, `ftruncate`), the environment,
   terminals (`hSetEcho`, `tcgetattr`), signals, `iconv` beyond UTF-8 as a class, `foreign import "wrapper"` (HES11:
   `createAdjustor`, a function pointer of crt's text calling back into the machine).
7. **HE4h the exit**: the samples and the synthetic corpus all equal or classed, `Hello`, `T.hs`, `U.hs`, `L.hs` byte
   for byte (§11's HE4 row), the core's run tests every one in a result or a class.

### Proposals (HES37 onward)

- **HES37 libm.** (a) musl's libm functions ported to Go by hand from musl 1.2.6's sources (MIT: `LICENSE.md` and
  `doc/ARBACE.md`'s origin), checked by the synthetic programs (bit for bit with GHC's runs); (b) musl's C forms run
  by ci or compiled by cf2g (HES11 (a)'s first idea: needs ci's runtime in hsi's program); (c) Go's `math` and the
  class kept. *Recommended: (a)*: about 2,000 lines of C, mechanical, no dependency on ci's machinery; (b) when ci's
  runtime is linked into hsi anyway (HES11 (b)'s `cbits`).
- **HES38 How hsi's runtime is written.** (a) Go source converted by repr/go's native reader into Go forms (mode
  `:lines`, as oi's, OC70), the forms the source from then on (a comment inside a function body is not kept: the
  notes are declarations' doc comments); (b) mode `:full` (every comment and space kept: five times the size); (c)
  hand-written forms as goi's. *Recommended: (a)*.
- **HES39 The libraries' dumps the check reads.** (a) `.tmp/hsi/dumps` (HE2's five packages) and `.tmp/hsi/dumps-extra`
  (array, stm, deepseq, transformers, mtl) until the night run dumps the 41 packages (HE2's night run 1), then the
  one directory; (b) the 41 now. *Recommended: (a)*.
- **HES40 `dataToTag#` of a thunk.** (a) evaluated first (as built: GHC 9.14's STG passes a lazy field's value, and its
  code generator reads the info table of what it gets); (b) a fatal error (the STG's invariant, which the dumps break).
  *Recommended: (a)*.
- **HES41 `throwTo` to a masked thread.** (a) GHC's: the thrower blocks until the exception is delivered (HE4e); (b) as
  built, the exception queued and the thrower going on. *Recommended: (a)*.
- **HES42 A program's own C stdio.** (a) `printf`, `puts`, `putchar` into a buffer written after the Haskell handles'
  flush at the end (libc's stdout to a file, the references' case), `fflush` writing it (as built); (b) written at
  once. *Recommended: (a)*.
- **HES43 The check's classes.** (a) by the programs' ids (`test/haskell/divergences.edn`'s `:ids`) while the classes
  are few; (b) by reasons matched as goi's (`:match`), once hsi writes a reason per run (HE6). *Recommended: (a)* now,
  (b) at HE6.

## Decided (2026-11-03, the user): HES37-HES43

All as recommended. HE4's next steps: HE4b (libm) and HE4c (the testsuite's language core as a check), below.

## As built: HE4b, HE4c (write-ahead)

Built 2026-11-03 on branch `haskell-he4b` from `haskell-he4` (`846e987c`), on the host's GHC 9.14.1, TamaGo's go, the
host's cc and musl, and the main checkout's stage 0 (`ARBACE_HOST_EXE`); light work by day (8 jobs, no lock but for the
packages' dumps, run under `.tmp/heavy`, 2.5 minutes). Write-ahead: HE4's exit criteria (§11) are still not all met
(HE4d-HE4h, "The rest of HE4" above). Nothing of Haskell is in `gate_key` or a gate step (HES19 (a)).

### HE4b: musl's libm in Go

- **`go/arbace/hs/libm.ae`, `libm_data.ae`** (HES37 (a)): musl 1.2.6's libm functions GHC's code calls, ported to Go by
  hand from the pinned tarball's `src/math` (`test/c/corpora.edn`'s `:musl-1.2.6`, SHA-256 `d585fd3b...`; the files the
  first one's header names: the Arm Limited exp, log and pow and their float versions with their tables; FreeBSD msun's
  expm1, log1p, the trigonometric kernels and argument reductions, `__rem_pio2_large`, the inverse and hyperbolic
  functions; `__math_*`), then converted to Go forms by repr/go's native reader as the rest of `go/arbace/hs` (HES38
  (a), mode `:lines`; the Go source in the worktree's `.tmp/hs/hs-src/`, the forms the source from now on). 34 functions:
  `exp expm1 log log1p sin cos tan asin acos atan atan2 sinh cosh tanh asinh acosh atanh pow` and the 16 `f` versions
  with `powf`, `atan2f`; `sqrt fabs floor ceil trunc round` stay on Go's `math` (exact operations, musl's results). As
  x86_64 compiles them: `FLT_EVAL_METHOD` 0, no FMA (the host's `ld-musl-x86_64.so.1` has no FMA or AVX instruction),
  `TOINT_INTRINSICS` 0. Two rules of the port: every named constant is a Go variable (Go folds a constant expression
  exactly, C rounds each operation: `3*pio2_1`, `0x1.c6af84b912394p-5/N/N/N`), and a C double literal in float code
  makes the operation double, as in C. The tables are the C initializers transcribed mechanically (a scratch script,
  the x86_64 preprocessor branches; `ipio2` in full, long double's). The floating-point exceptions and `errno` are not
  kept (no caller reads them). The primops (`expDouble#` ... `atanhFloat#`, `**##`, `powerFloat#`) and the C functions
  by symbol (`ffi`) call the port; the `Float#` primops call the `f` functions (GHC's code calls `expf`, not `exp`).
- **`bin/hsi libm-check [N [SEED]]`** (`make hsi-libm-check`): `test/haskell/hsi/libm-ref.c`, built by the host's cc
  (`-O0 -fno-builtin`, the functions through pointers: no constant folding by gcc), writes for each function its
  results by the host's musl over the edges (signed zeros, the smallest and largest subnormals, the normal bounds, each
  edge's neighbours, infinities, three NaNs, the functions' thresholds, multiples of pi/2 up to 2^30, the overflow and
  underflow bounds), N random bit patterns, N values in five ranges and N/5 subnormals, and for `pow`, `powf`, `atan2`,
  `atan2f` the edges' cross product with signs and random pairs (negative bases with integer exponents, bases near 1
  with large exponents); `hsi libm` (the driver) recomputes every line and compares the bits. **1,769,602 calls, 0
  mismatches** (N 20,000, 1 s); with N 300,000, 25,401,602 calls, 0 mismatches (about 15 s). The negative control,
  the same lines by Go's `math` (what hsi had at HE4a), mismatches 471,953 and must.
- **The synthetic corpus**: `class-floating` and `primops-float` now pass; `primops-double` differs in its two lines of
  `decodeDouble_2Int#` of a zero only, whose sign GHC's RTS leaves uninitialised (`rts/StgPrimFloat.c`): the class
  `:uninitialised` (`:design`). The class `:libm` is gone. Samples and synthetic: **46 of 47 equal**.

### HE4c: the testsuite's language core as a check

- **`bin/hsi check --corpus testsuite-core`** (`bin/lib/hsi/check`): record-ref's plan (`test/haskell/eval/run_plan.ae`
  over `testsuite-run.edn` and eval-corpora.edn's nine directories: 701 tests), each test dumped by the helper at `-O0`
  with its all.T flags (less its own C, Cmm, assembly and object files, HES45) from its directory, GHC following the
  imports for the multi-module ones (the dump cached by the main file's, the flags' and, for those, the directory's
  sources' digests), run on hsi in a fresh copy of its directory as record-ref ran GHC's build: its stdin, its
  `extra_run_opts` through sh (`/dev/null` as an empty file), the driver dropping `+RTS ... -RTS` as GHC's RTS does
  (`progArgs`; HES47), `env -i`, 60 s of CPU and 180 s of wall time. Every result carries `:dump` and `:run` seconds
  (GT8). `--only` takes ids or `@FILE` (one id a line, GT8's lists); `--record` keeps the lines of the programs a run
  does not cover. The reference was complete: every one of the 701 tests has its line in `ref-ghc914.edn` (HE3), so
  record-ref recorded nothing new; its 20 `:no-build` are tests whose own C, Cmm or `pre_cmd` setup the plan does not
  pass (HES35 (a)).
- **The packages the core imports** (HES44): `bin/hsi dumps --packages bytestring,os-string,filepath,time,unix,file-io,
  directory,ghc-platform,ghc-boot,ghc-experimental,ghc-heap,process,text,binary,pretty,template-haskell,ghc-boot-th,
  exceptions,ghc-compact,parsec --out .tmp/hsi/dumps-more` (2.5 minutes under the heavy wrapper; 338 dumps, 36 MB, read
  back with 0 errors), every module's ABI hash equal to the installed one's but `text`'s `Data.Text.Encoding` (1 of 54;
  not yet looked at). `time`'s `.hsc` needed the corpus's `$repo` (its `cabal_macros.h`) substituted in the dump's flags
  (fixed in `bin/hsi`). The check's and `bin/hsi run`'s default libraries: `dumps`, `dumps-extra`, `dumps-more`.
- **Results** (`test/haskell/check-hsi.edn`, samples, synthetic and the core: 748 programs): **630 equal**, of the core
  **584 of 701** (HE4a's probe: 504 of its 555 plain single-module tests); every other one in a class of
  `test/haskell/divergences.edn` (HES43 (a), by ids): `:test-c` 54 (the test's own C: HE6), `:callback` 19 (foreign
  import "wrapper" and foreign exports called back: HE4g), `:rts` 24 (deadlock by reachability, scheduling, throwTo to a
  masked thread, allocation limits, finalizers, the static pointer table: HE4e), `:speed` 9 (killed at the limit: HE4d;
  one of them, `T8726`, at 35 to 39 s against the 60 s of CPU Go's collector threads share, passes or not by a few
  seconds: it passes in the record),
  `:hsi-bug` 3 (internal errors: HE4f), `:rts-options` 2, `:process` 2, `:c-stdio` 1 (`%f`; HE4g), `:simd` 1,
  `:skipped` 1 (`T4262`, all.T's unconditional skip, HES46), `:no-reference` 2 (GHC did not build them),
  `:uninitialised` 1. The negative controls (`test/haskell/hsi-controls.txt`, now also `arr001` against `arr002`) fail
  as they must.
- **Times** (GT8): the whole check, 748 programs at 8 jobs, **3.5 minutes** of wall time with the dumps cached (18
  minutes of CPU), the core's dumps 108 s of helper time when cold; per test the median run 0.20 s, the 90th percentile
  0.38 s, 546 of the passing core tests under 0.5 s; the 8 `:speed` tests take 180 s each (most of the 977 s summed).
  Under the 20 minutes the brief allows by day: the whole core is a day run, not a night one.
- **GT8's list** (HES49): `test/haskell/hsi/essential.txt`, the samples and synthetic programs that run in under 2 s
  (all but Loop and Nfib, 12 s each), and per directory of the core the 8 fastest passing tests, with the three
  controls' subjects: 117 programs, **10 s** at 8 jobs with the dumps cached (`make hsi-check-essential`; `bin/hsi
  check --corpus samples,synthetic,testsuite-core --only @test/haskell/hsi/essential.txt`).

### Night runs (listed, not run)

1. HE2's: `bin/hsi dumps --samples --again` (the 41 packages, twice), then HES44's three directories replaced by it.
2. `bin/hsi libm-check 3000000 SEED` for a few seeds (about 2.5 minutes each).
3. `bin/hsi record-ref --corpus testsuite` and a check of the testsuite's 1,713 run tests (the core's rate: about 9
   minutes).

### Proposals (HES44 onward)

- **HES44 The packages the core imports.** (a) dumped into `.tmp/hsi/dumps-more` until HE2's night run of the 41
  packages (as built, HES39's interim extended); (b) wait for the night run. *Recommended: (a)*.
- **HES45 A test's own C in the check.** (a) left out of the helper's flags, the Haskell dumped and run, hsi stopping
  at the test's function (the class `:test-c`, as built); (b) the test `:no-dump`. *Recommended: (a)*: the reason is
  hsi's own message, and the C runs once HE6 brings ci beside hsi.
- **HES46 all.T's unconditional `skip`.** (a) the test kept in the corpus and its non-pass a class `:skipped` (as
  built); (b) left out of the corpus (eval-corpora.edn's counts change). *Recommended: (a)*.
- **HES47 The RTS's options.** (a) the driver drops `+RTS ... -RTS` and after `--RTS` keeps the rest (as built), the
  RTS's rejections a class `:rts-options` until HE4g; (b) parse GHC's RTS options and give its messages now.
  *Recommended: (a)*.
- **HES48 libm's check.** (a) `bin/hsi libm-check` by day at N 20,000 (1 s) and by night at millions, the host's musl
  the reference (its pin: the host's musl package, 1.2.6-r6, whose `src/math` is the tarball's); (b) only the synthetic
  programs. *Recommended: (a)*.
- **HES49 GT8's list for Haskell's eval.** (a) `test/haskell/hsi/essential.txt` as built, for the row's share of the
  essential gate when Haskell's row enters it; (b) a coverage-chosen subset (primops and C functions each reached once)
  at HE6. *Recommended: (a)* now, (b) at HE6.

## Decided (2026-11-03, the user): HES38 is (b), mode `:full`

Correcting "Decided: HES37-HES43" above for HES38 alone: hsi's runtime is Go forms in mode `:full` (every comment and
space kept), as Python's pyi's runtime (`bin/pyi runtime`, PY5b0), not `:lines`. The other six stand as recommended.
And Python shares hsi's port of musl's libm: it moves into a package of its own.

## As built: HES38's `:full` forms and the shared libm

Built 2026-11-03 on branch `haskell-full` from `haskell-he4b` (`f3672f68`).

- **`go/arbace/libm.ae`, `go/arbace/libm/`**: the package `arbace/libm` (`libm.go`, `libm_data.go`), HE4b's port moved
  out of `arbace/hs` and its names exported (`libm.Exp` ... `libm.Atan2f`; `libm.D` and `libm.F` by C name;
  `libm.Call` on bit patterns, `bin/hsi libm-check`'s subject), its origin in the package's doc comment and
  LICENSE.md. hsi imports it (`ffi`, `prim_scalar`); pyi may. Out of `gate_key` as `go/arbace/hs` is (`bin/arbace-go`'s
  `gate_key`, `test/gate/audit.ae`'s `:gate`, `test/gate/gate-key-crops-test.bash`, passing): no executable imports it.
- **Mode `:full`**: `go/arbace/hs`, `go/arbace/libm` and the driver (`test/haskell/hsi/cmd.ae`, `cmd/main.ae`) read again
  from their Go text, gofmt'ed first (1.7 MB of forms, against 0.4 MB in `:lines`).
- **`bin/hsi runtime print|read|check`**, as `bin/pyi runtime`: `print` writes the three packages' Go (with crt's,
  which hs imports) into the module `.tmp/hsi/runtime` to edit; `read` requires it gofmt'ed and reads it back by repr/go's
  native reader (`read --full`) into the tree; `check` prints the committed forms and reads them again, and requires
  the same forms file for file (36 s). `bin/hsi build` prints through the same function. Negative control, by hand: a
  form a read would not give (`libm.ae`'s `:init-order` with two names swapped) fails the check; an edit a read would
  give (a literal's value) passes it, by design: the check says the forms are a read's output, not what they compute.
- **Checks**: `bin/hsi runtime check` passes; `bin/hsi libm-check` 1,769,602 calls, 0 mismatches, the control 471,953;
  `bin/hsi check --corpus samples,synthetic,testsuite-core`: 630 of 748 equal as before, samples and synthetic 46 of
  47, every other one in its class (3.5 minutes; `T8726`, `:speed`'s edge case, failed at 8 jobs and passed alone, as
  recorded).

## As built: HE4d, HE4e (write-ahead)

Built 2026-11-03 on branch `haskell-he4d` from `haskell-full` (`5553edfd`), with the Mac's `mac/189-libm-fma`
(`4600a322`) merged; on the host's GHC 9.14.1, TamaGo's go and the main checkout's stage 0, light work by day (8 jobs,
no lock), the runtime edited through `bin/hsi runtime print` and `read` (HES38 (b)). Write-ahead: HE4's exit criteria
(§11) are still not all met (HE4f-HE4h below). Nothing of Haskell is in `gate_key` or a gate step (HES19 (a)). The
host was shared with other agents throughout: the times below vary by about 10 %.

### HE4d: speed and the image

- **Measured first** (CPU profiles, `hsi run --cpuprofile`, which a SIGTERM or SIGXCPU stops cleanly): on nfib half of
  the time Go's allocator and collector (every call's frame, every closure and constructor two objects, a primop's
  argument buffer escaping), then the dispatch of class methods at `-O0` (a selector entered as a function every `+`);
  on the testsuite's slow tests the same, plus linear scans of large case tables and the loaded forms read and
  compiled per run (a run's median 0.22 s, most of it loading).
- **The hot paths**: a constructor, function or thunk of up to four fields is one Go object (`alloc.ae`); an updatable
  thunk is allocated with room for its whole frame and evaluated in place (entered once, blackholed, its free
  variables never written); a pure primop's arguments go through the machine's buffer; a class method's selector
  (`\d -> case d of C ... -> field`) applied to an evaluated dictionary takes the method at once; a constructor of
  literals is one static value; a call in its function's tail position (outside every scrutinee: no frame of the
  activation is on the stack) reuses the caller's frame; the other frames of slots come from a per-thread arena
  (`arena.ae`): every stack frame records the arena's top and its pop gives back what was allocated above it (frames
  never escape: closures copy their values, a continuation's capture copies its frames' slots); case alternatives are
  indexed by constructor tag and by integer literal; frames shrank from 96 to 72 bytes; GOGC 200 (400 kept the loaded
  forms' heap; with the image it only grows the peak).
- **The image** (HES32 (a), `image.ae`; `hsi image --out DIR --lib DIR...`, made by `bin/hsi run` and `bin/hsi check`
  when stale, `HSI_IMAGE`, default `.tmp/hsi/image`, `none` for none): each library module compiled once into hsi's
  code trees and written as bytes, `IMAGE/UNIT/MODULE.img`: the dump's size and modification time (stale when either
  differs), a string table, the module's data declarations, string literals and top-level bindings, then each
  closure's compiled body (join points numbered, literals by value, string literals and labels by bytes and name,
  a closure GHC's forms leave uncompilable as its error), decoded when the closure is first entered. The bytes hold no
  pointers: the collector does not scan them. 957 modules in 0.6 s on 8 jobs, 22 MB (the forms 80 MB).
- **Results** (before: `haskell-full`'s hsi; after: this branch's): **nfib 32 13.3-14.1 s → 5.8-6.1 s** (2.3 times;
  GHCi 3.6 s, GHC's `-O0` 0.32 s: the plan's 4 s target is not met); Hello 0.20 s → 0.02-0.06 s (51 of its 52 modules
  from the image); Loop 11-13 s → 10-14 s (its 7.6 GB peak, allocation-bound: unchanged within the noise). **The
  check of samples, synthetic and the testsuite's core: 3 min 46 s → 2 min 19 s** of wall time at 8 jobs (748 programs
  before, 756 after; of the 87 s, about 60 s are T4262's 180 s no longer run, L8 below, and the rest the runs: their
  summed seconds 1,070 → 923 at the same plan, the median run 0.22 s → 0.07 s); the essential list 123 programs in 10 s.
  `T23071` and `CarryOverflow` pass now (`:speed`, listed still: near the limit); `T19149` reaches its test's C.

- **Memory** (after the coordinator's report of two hsi runs of about 20 GB each, outside the heavy lock, at 8 jobs):
  each slow program's peak RSS measured alone under the lock (`/usr/bin/time`, 30 s of CPU). Two leaks, found and
  fixed: (1) conc012's endless recursion grew the stack without bound: hsi had no `-K`; now the driver takes `+RTS
  -K<size>` (the one RTS option it keeps) and a thread's stack is bounded (by default 2 GiB of frames, 72 bytes
  each), StackOverflow raised past it as GHC's `threadStackOverflow` (11.7 GB → 96 MB; conc012 passes); (2) a list
  consumed by `length` was kept whole (`length [1..5000000]`: 1.36 GB, also on `haskell-full`'s hsi, 2.4 GB) by a
  dead slot of a case frame (a frame keeps its activation's slots, GHC's only the live ones): a call inside a
  scrutinee now clears, once its arguments are read, every slot no pending continuation of its activation reads
  (`live.ae`, `App.kill`, `Enter.kill`), and a call or entry in tail position clears the activation's locals
  (`clr`, which an updatable thunk evaluated in its own array needs): 237 MB, and allocLimit1, `length [1..]` until
  the limit, 6 GB in 25 s → 235 MB. What stays large is expected: deep stacks, a frame of hsi's being about three of
  GHC's words: Loop 7.3 GB (GHC 0.35 GB), T16846 9.7 GB (`mapM` over 10 million in IO). So hsi's checks run under
  `.tmp/heavy` at 3 jobs (`bin/hsi check`'s default since). And each program runs under main's memory cap
  (`bin/lib/limit.bash`'s `ARBACE_MEM_GIB`, default 16 GiB: `ulimit -v` at it, GOMEMLIMIT at three quarters; `bin/hsi
  check`'s runs and `bin/hsi run`; `HSI_MEM_CAPS` names a program's own, none needed: Loop and T16846 pass under 16
  GiB): a leaking program fails alone (a list kept live, at `ARBACE_MEM_GIB=2`: Go's out of memory, status 2, at 0.75 GB
  of RSS in 3 s); the check unchanged under it, 650 of 756 in 4 min 35 s. With `-K`, two update frames in a row are squeezed as
  GHC's stack squeezing does (one frame updates both thunks; cgrun060 at `-K64k`).

### HE4e: the RTS's semantics (`rts.ae`)

- **throwTo** (HES41 (a), rts/RaiseAsync.c's `throwToMsg` by the target's state): a target that does not accept the
  exception now (masked and running, uninterruptibly masked anywhere, masked on a blackhole) queues it and the thrower
  blocks, interruptibly (an exception to the thrower revokes its message); the queue's first is delivered at the
  target's unmasking (the unmask primop, an unmask frame's return), as it blocks interruptibly while masked
  interruptibly (an MVar, a delay, an STM retry: review 189's F5), and a finished target wakes its throwers.
- **The collector's decisions** (rts/sm/MarkWeak.c's stages): hsi's own marking from the roots GHC's major collection
  has (the running and runnable threads, the sleeping ones, the stable pointers), through heap objects, thread stacks
  (a case frame keeps only the slots its alternatives read, a closure's code the globals it names: GHC's SRTs), MVars'
  queues, the TVars a retried transaction read, blackholes' owners and waiters; the weak pointers whose keys are marked
  keep their values and finalizers; the blocked threads still unmarked are resurrected (oldest first:
  BlockedIndefinitelyOnMVar, OnSTM, NonTermination on a blackhole; one blocked in throwTo left), their reach marked, the
  weak pointers again, the rest dead. At `performGC` (and `performMajorGC`, `performBlockingMajorGC`) and when every
  thread is blocked and none sleeps (scheduleDetectDeadlock: when none is resurrected, the main thread, blocked, gets
  NonTermination). GHC also collects as allocation goes; hsi does not (HES52).
- **Weak pointers and finalizers** (Weak.c): every `mkWeak#` registered; a dead one marked dead, its C finalizers run
  at once (the last added first), its Haskell finalizer given with the others to one new thread running
  `runFinalizerBatch` (the newest first, as GHC's); `hs_exit`'s C finalizers of the weak pointers still alive at the
  end; `keepAlive#` a frame keeping its value reachable.
- **prompt#, control0#, newPromptTag#** (rts/Continuation.c): `control0#` captures the frames above the nearest prompt of
  its tag (their slots copied), pops them and the prompt, and applies its function to the continuation, a function of
  two arguments; a capture through an update, atomically, catchRetry or catchSTM frame, or without a prompt, raises
  `noMatchingContinuationPrompt`; a captured mask frame restores the prompt's masking state at the capture, and on a
  resume (which copies the frames again: a continuation may resume many times) the outermost mask frame restores the
  resumer's state and the captured state applies inside.
- **Review 189's findings** (the Mac's, on `haskell-he4b`): F1 (on arm64 Go fused libm's multiply-adds) by the Mac's
  413 conversions, with `tools/fmacheck` (a go/types scan: every floating-point product of go/arbace/libm a conversion
  of itself unless a product's or a quotient's operand; `-w` rewrites) and an FMA-instruction count in `bin/hsi
  libm-check`; L1 (pow's and atan2's NaN operand) by `lmNaN2`, x86_64 musl's choice whatever order Go emits the add in;
  L2 the constants written as their value; F2 (a transaction blocked on another thread's blackhole rolled back over
  another's commit): the user's decision, abort and rerun (the writes undone, its frames popped to atomically#, the
  thread waits on the blackhole and reruns from its start), and retry's reverted thunks wake their waiters; F3 a
  readMVar waiter ahead of the takers; F4 an interrupted MVar operation leaves no state; F7 `fmaddFloat#` and its kin
  rounded once (musl's `fmaf.c`); F8 nested atomically refused; F9 catchSTM# lets asynchronous exceptions pass, a
  blackhole knot gives NonTermination; L7 a runnable thread is not queued twice. F6, L3 and L8 in the check, below.
  Their known divergence (an interrupted thunk reverted, not frozen) is the class `:async-revert` (`:design`).
- **Samples judged against GHC** (`test/haskell/eval/samples/`, references by `bin/hsi record-ref --only`): ThrowMasked,
  Deadlock, Cont, Weak, StmBlock (F2, F8, F9), MVarOrder (F3, F4, F5), Fma (F7), all equal to GHC's runs and each
  differing on `haskell-full`'s hsi; AsyncRerun in `:async-revert` (GHC 1, hsi 2).
- **The check** (review 189's F6, L3, L8): a class's `:results` (and `:message`, a text of hsi's message) are the kinds
  its members may have: a classed program whose result changes kind is in no class (the negative control, by hand:
  `:simd`'s `:results [:fail]` makes T22296 unclassed); `:test-c` (hsi stops at the test's C) split from
  `:test-c-output` (the run goes on without it); `:uninitialised` gone (the generator leaves out `decodeDouble_2Int#`
  of a zero, primops-double re-recorded, it passes); a check where no control ran fails, and a fourth control judges
  the status alone (`samples/Echo` against its reference with status 0); a test all.T skips is not run (`:skipped`).
- **Results**: `bin/hsi check --corpus samples,synthetic,testsuite-core` **650 of 756**: samples 17 of 18, synthetic 37
  of 37, the core **596 of 701** (584 before: conc012, conc016, conc018, conc019, conc024, conc034, conc068, mask002,
  tryReadMVar2, UnliftedWeakPtr, T23071, CarryOverflow; compareByteArrays at 51 s, near the limit); every other one in
  its class, the four controls failing; 2 min 19 s at 8 jobs, 4 min 55 s at 3 jobs under `.tmp/heavy` (the memory rule);
  `test/haskell/check-hsi.edn` recorded. `bin/hsi libm-check` 0 mismatches, 0 FMA; `bin/hsi runtime check` the same
  forms.

### The rest of HE4

1. **HE4f** the remaining primops and their check (as planned), and the scheduling order GHC's timer gives (`:rts`'s
   conc037, conc065-072, T3279), allocation counters and limits (allocLimit1-4).
2. **HE4g** the top level and the I/O (as planned), the C data labels (HES57).
3. **HE4h** the exit; speed beyond HE4d is HE8's AOT (the interpreter's per-node cost now about 30 ns, mostly
   allocation: STG's thunks, boxed `Int#`s).

### Proposals (HES50 onward)

- **HES50 The image's freshness.** (a) the dump's size and modification time (as built: a stat a module); (b) the dump's
  SHA-256 (a read and a hash a module each run). *Recommended: (a)*: the dumps are made by `bin/hsi dumps` only.
- **HES51 Frames of slots.** (a) a per-thread arena given back at frame pops, a tail call reusing its caller's frame (as
  built); (b) Go's heap for every frame (simpler; nfib about 7 % slower). *Recommended: (a)*.
- **HES52 When hsi collects.** (a) at `performGC` and at a deadlock only (as built: deterministic; a program that waits
  for a finalizer without calling performGC waits forever, a class when one is found); (b) also after every N
  megabytes allocated, as GHC's major collections (the testsuite's runs would depend on N). *Recommended: (a)*.
- **HES53 The classes' result kinds.** (a) `:results` and `:message` per class (as built, review 189 F6), HES43 (b)'s
  reasons later; (b) by id alone. *Recommended: (a)*.
- **HES54 all.T's skipped tests.** (a) not run, their result `:skipped` (as built, review 189 L8); (b) run to the wall
  cap. *Recommended: (a)*.
- **HES55 STM blocking.** (a) a transaction that would block on a blackhole aborts and reruns (as built, the user's F2
  decision); one that blocks inside `unsafeIOToSTM` (an MVar, a delay) keeps its writes in place while it waits, a class
  when a test shows it; (b) a write-log TRec (GHC's design) at HE4f. *Recommended: (b) at HE4f*.
- **HES56 Interrupted thunks.** (a) reverted, the class `:async-revert` (`:design`, as built); (b) frozen as GHC's
  AP_STACK (the stack segment kept in the thunk, resumed by the next demand). *Recommended: (a)* until a test needs (b).
- **HES57 An unknown C data label.** (a) a stop (`unimplemented C data label NAME`) unless `dataLabels` knows it (review
  189 L5), at HE4g with the labels the libraries read listed; (b) zeroed storage (as now). *Recommended: (a)* at HE4g.
- **HES58 libm-check on arm64.** (a) its reference musl's `src/math` from the pinned tarball built as x86_64 builds it
  (review 189 F1's third fix), at the Mac's next hsi task; (b) amd64 only. *Recommended: (a)*.

## As built: HE5, HE6 (write-ahead)

Built 2026-11-05 on branch `haskell-he5` from `haskell-he4d`, on the host's GHC 9.14.1, TamaGo's go and the main checkout's
stage 0, light work by day (2 jobs, no lock, no gate, no corpus run of the whole row). Write-ahead: HE5's exit criterion
(the testsuite's language-core run tests and nofib: none failing, every non-pass classed) is met for what the two
corpora reach, with the classes below; the whole-row runs are the night commands at the end. Nothing of Haskell is in
`gate_key` or a gate step (HES19 (a)). The tarballs `bin/hsi pins --fetch` pins (the testsuite's, nofib's commit) were
fetched by that command, as every `bin/hsi record-ref` and `bin/hsi check --corpus testsuite-core` does; the source
tarball is the one the other branches' `bin/repr-haskell corpus fetch` kept, copied.

### HE5: the C by need

- **Method.** `bin/hsi dumps` (this branch, 1,320 modules of the 41 packages, 0 errors, about 12 minutes at 3 jobs) counts
  the foreign symbols the libraries' STG names: 434, 253 of them C functions hsi's table lacked once a `capi` wrapper's
  `ghczuwrapperZC...ZC` name is decoded to the C function's (`cSymbol`). `bin/hsi cfunctions` lists the table (203 symbols at the
  start of HE5, **395** now); what the table lacked went by need: the library
  samples below were written, run on hsi against GHC's recorded runs, and each stop (an unimplemented C function, a data label
  read as zeros, an internal error) fixed. 69 symbols remain, none reached by a sample, a synthetic program or a language-core
  test but those in the classes below: createAdjustor, allocateExecPage, freezeExecPage, ffi_prep_cif and
  `arenaAlloc` (wrappers, HE5's open piece), fork, forkProcess, forkOS_*, execv*, `__hsunix_execvpe` (a process image
  cannot be forked: a design class), POSIX semaphores and shared memory (`sem_*`, `shm_*`), terminfo (`setupterm`, `tigetstr`,
  ...), the loader (`loadObj`, `lookupSymbol`, ... : GHCi's linker, unreached), `haskeline_mk_wcwidth`, `__gmpn_popcount` (the
  GMP backend, not the native one HES7 chose), `fchdir`, `findSystemLibrary`, `addLibrarySearchPath`.
- **`go/arbace/hs/cbits.go`**: the cbits of two packages ported to Go by hand from their own sources (GHC 9.14.1's tarball,
  `libraries/bytestring/cbits`: `itoa.c`, `fpstring.c`, `shortbytestring.c`, `is-valid-utf8.c`; `libraries/text/cbits`:
  `is_ascii.c`, `measure_off.c`, `reverse.c`, `utils.c`; BSD licenses, LICENSE.md): the SIMD variants (`fps_count_avx2`,
  `measure_off_avx`) compute what their portable fallbacks do, the same bytes; `bytestring_is_valid_utf8` is Go's `unicode/utf8`'s
  (both are the Unicode standard's well-formedness: no surrogates, no overlong forms, nothing past U+10FFFF); `memchr`. The data
  tables of `cbits/aligned-static-hs-data.c` (the hex and digit-pair tables, Ryu's float and double tables) are `cdata.go`,
  generated from the tarball by `bin/lib/hsi/cdata-gen` (a data label's storage is the table's, not 4 KiB of zeros).
- **Unlifted arguments** (`UnliftedFFITypes`, text's imports take `ByteArray#`): an unpinned byte array given to a foreign
  call is copied into a scratch region of the C memory and back after the call (`ffiBack`, `scratchFor`); a pinned one is its
  address. T9274 (the `:hsi-bug` class) passes by it.
- **`go/arbace/hs/posix.go`**: what crt's kernel does not emulate, over Go's `os` and `syscall` on the host's files, paths
  resolved against the working directory the program set by `chdir`: `chmod`, `chown`, `link`, `symlink`, `readlink`,
  `realpath`, `utimes`/`utimensat`/`utime`, `truncate`, `mkfifo`, `mknod`, `mkdtemp`, `mkstemp(s)`, `getcwd`, `creat`,
  `openat` and friends on crt's calls; directory streams (`opendir`, `readdir` through `__hscore_readdir`'s dirent,
  `closedir`, `rewinddir`, `telldir`, `seekdir`) over crt's `getdents64`; the environment (`setenv`, `putenv`, `unsetenv`,
  `clearenv`, `environ`: the host's, as `getenv`); users and groups from `/etc/passwd` and `/etc/group` (`getpwnam_r`, ...,
  as musl's); time (`clock_gettime` with the CPU clocks, `gettimeofday`, `times`, `getrusage`, the time zone of Go's
  `time.Local`, `TZ` as the host gives it); `sysconf`, `pathconf`, `getrlimit`, `setrlimit`, the wait macros; signal sets;
  terminals as non-terminals (`tcgetattr` and the rest fail with ENOTTY: a handle on a terminal is a class if a program
  needs one); `dlopen` failing with a message. The answers of an RTS built without the event manager, backtraces from DWARF,
  the debugger and static pointers for the RTS functions no program reaches.
- **Processes and pipes.** `pipe` and `pipe2` make `os.Pipe`'s two ends, and `runInteractiveProcess`, `waitForProcess`,
  `getProcessExitCode`, `terminateProcess`, `waitpid` are the process package's C over `os/exec`: the standard handles -1 a
  new pipe, -2 closed, 0 to 2 the program's own; the parent's ends are fds of the same layer (numbers from 4096, below them
  crt's), `read`, `write`, `close`, `fstat`, `fcntl` going to the layer that owns the number. A read that would block while another thread can
  run returns EAGAIN after 300 microseconds and the library retries (GHC's `throwErrnoIfMinus1RetryMayBlock`), so the
  writer thread of `readProcess` runs; when no thread is runnable it blocks. `forkProcess` and `exec*` stay (a Go process
  image has no fork): T24672's class `:process`, now `:design`.
- **Signals** (rts/posix/Signals.c, the non-threaded RTS): `stg_sig_install` keeps what each signal does (default,
  ignored, the Haskell handler), `raise` and `kill` of the program's own pid queue a signal with a handler, and the scheduler
  starts one thread per queued signal running `GHC.Internal.Conc.Signal.runHandlersPtr` (the RTS's `startSignalHandlers`);
  a signal from outside the program is not delivered (a class when a test needs it).
- **A general `printf`** (`cformat.go`): the conversions d i u x X o c s p f F e E g G with flags, width, precision, `*` and
  the length modifiers, for the RTS's messages and a program's own `printf` (T4012, the class `:c-stdio`, passes: the class is gone).
- **Smaller fixes by the samples and the core**: `reallyUnsafePtrEquality#` on stable names and prompt tags (the `Eq`
  instance of `StableName`); the range primops with a zero length at any offset (`CheckBoundsOK` passes: `copyByteArray#`,
  `compareByteArrays#`, `cloneArray#` and kin, whose offset may be out of bounds when nothing is copied);
  `getRTSStats` an empty answer (`getRTSStatsEnabled` is 0).
- **Library samples** (`test/haskell/eval/samples/Lib*.hs`, references recorded by `bin/hsi record-ref --only`): `LibBytes`
  (bytestring: packed strings, builders, readers), `LibText` (text: UTF-8 measure, reverse, encode and decode, builders),
  `LibContainers` (containers, array, mtl, stm, deepseq), `LibSystem` (directory, filepath, process, time, `System.Posix.Files`,
  timeout), `LibPosix` (links, times, the environment, users, directory streams, handle seeks, `Fd`s), `LibMisc` (zones, pipes,
  signals, resource limits, stable names, weak pointers, `printf`), `LibIO` (encodings, binary, temporary files, buffering, end of
  file, terminals). All equal to GHC's runs, byte for byte, stdout, stderr and status; the first three differed on the
  HE4d branch's hsi at their first stop (`memchr`, the byte-array argument, `fstatat`).
- **Results.** `bin/hsi check --corpus samples,synthetic,testsuite-core` (2 jobs): **661 of 763** pass (650 of 756 before:
  samples 24 of 25 with `AsyncRerun` in `:async-revert`, synthetic 37 of 37, the core 600 of 701; 52 minutes at 2 jobs with the other runs of the day beside it), every other one in its class, the four
  controls failing as they must; `test/haskell/check-hsi.edn` recorded. nofib (`bin/hsi check --corpus nofib`, new: the Makefile's flags,
  arguments and stdin as `record-ref` plans them): 144 programs: **76 pass**, 9 `:speed` (killed at the 60 s CPU limit: imaginary/kahan, queens, x2n1, shootout/k-nucleotide, n-body, pidigits, spectral-norm, smp/threads001 and threads007: hsi is a few times slower than GHCi, HE8's AOT), 2 `:callback` (smp/callback001 and 002), 57 `:no-reference` (GHC 9.14.1 builds none of them either: their references are `:no-build`; every one of the 57 fails to dump for the same reason), none unclassed; about 35 minutes at 2 jobs. nofib's per-program results are in `test/haskell/check-hsi.edn` (907 programs recorded with the samples, synthetic and the core).
- **Classes after HE5** (`bin/hsi divergences`): `:test-c` 46 and `:test-c-output` 11 (the tests' own C, Cmm and assembly: HES35 (a),
  ci beside hsi), `:callback` 19 (below), `:rts` 14, `:speed` 3 (5 of its 8 pass), `:rts-options` 2, `:fd-wait` 1 (new: T21969,
  `threadWaitRead` and `killThread` on a pipe: waitRead# is a no-op and fdReady always ready, so a wake-up by readiness does not
  happen), `:process` 1 (`:design`: forkProcess), `:simd`, `:skipped`, `:no-reference`, `:async-revert`; `:hsi-bug` and
  `:c-stdio` gone.
- **What is left of HE5.** (1) `foreign import "wrapper"` and `foreign export`: the dump has the `createAdjustor` call
  with a stable pointer to the Haskell function and the label of the C stub (`$main$Main$Main_mk`), but the exported Haskell
  helper the stub applies and the typed marshalling (the function's argument and result types) are not in the dump
  (STG-SPEC §11: foreign stubs do not reach the hook): HES60 below. (2) A program's own C (`:test-c`, 57 tests): ci
  beside hsi (HES11 (b)), HE6 in the plan. (3) Waiting on a file descriptor: `waitRead#`/`waitWrite#` and `fdReady` by `poll`
  with the scheduler sleeping until a descriptor is ready (HES61). (4) GHC's scheduling order of yields and the timer
  (`:rts`: conc037, conc065-072, T3279) and allocation limits: HE4f's. (5) Signals from outside the program.

### HE6: the verbs and the classes

- **`bin/hsi divergences [--results FILE] [--programs]`** (`bin/lib/hsi/divergences`, `test/haskell/eval/divergences.ae` on
  stage 0): `check`'s recorded results judged against `test/haskell/divergences.edn` with no run, as `bin/goi divergences`
  and `bin/reap` judge theirs: each non-pass in a class of its `:ids`, the result's kind among the class's `:results`, for `:hsi`
  the message holding the class's `:message`; a summary by class (kind, non-passes, members that pass, members absent from the
  results; a class every member of which passes is noted, never a failure); exit 1 on a non-pass in no class. The classes file
  is read by the Clojure reader (the verb exposed an escape in `:skipped`'s `:why` that the awk of `check` had hidden: fixed).
- **`bin/hsi slice K [--results FILE] [--slices N]`**: the recorded programs of rotation slice K of N (default 7, GTS203 (a):
  the first 8 hex digits of the id's SHA-256 mod N, a function of the name), one a line: the list for `bin/hsi check --only @FILE`,
  which the planner's `:rotate` writes the same way. Over the 760 recorded programs the slices hold 101 to 113.
- **`bin/hsi check --corpus nofib`**: the third run corpus (87 of its 144 programs built by GHC in the reference; the others are
  `:no-build` in `ref-ghc914.edn`, the class of HE3's classes file when the row comes).
- **Left of HE6.** (1) Reasons per run, HES43 (b): hsi writing why it stopped (a typed reason beside the message) so classes
  match by reason, as goi's: not started. (2) The coverage subsets of GT8.3 (`test/haskell/hsi/essential.txt` is the HE4d list by
  speed, not by coverage): the files that reach every primop and every C function of the table, every class and every control, need
  hsi to count the primops and C functions a run reaches (`hsi run --stats` counts steps only): a counting mode and the greedy
  cover over the recorded runs (HES62). (3) The testsuite's run tests whole: the 701 of the core are the language core; the
  other directories' run tests (about 5,000 more) need their classes by the same method. (4) The planner's step, parts
  and `:rotate` entries: HE10 (the row).

### Night commands (listed, not run)

1. `bin/hsi dumps` (the 41 packages, 12 minutes at 3 jobs; `--again` for the determinism check), then `bin/hsi check --corpus
   samples,synthetic,testsuite-core,nofib --jobs 3 --record` under `bin/heavy-lock` (about 12 minutes for the first three at 3 jobs, nofib
   about 30 minutes at 2 jobs: its programs run 5 to 60 s, the CPU limit's `:speed` class takes the rest), then `bin/hsi
   divergences` (must exit 0) and `bin/hsi runtime check` (the committed forms printed and read again equal themselves).
2. `bin/hsi record-ref --corpus samples,synthetic,testsuite-core,nofib` afresh only when GHC's pin changes; the samples' references
   with `--only`.
3. `bin/hsi libm-check` (unchanged by HE5), `bin/lib/hsi/cdata-gen .tmp/repr-haskell/corpus/ghc-9.14.1 | diff - <(print of
   go/arbace/hs/cdata.ae)` (the generator reproduces the committed table: done by hand today).

### Proposed (HES60 onward)

- **HES60 Wrappers and foreign exports.** (a) the helper dumps the exported Haskell helper of a `wrapper` import and of each
  `foreign export` (the desugarer's `fe_bind`, which GHC passes to the code generator in the module's bindings but the
  stgToCmm hook's `binds` do not hold: to find out in `tools/hsdump/Stg.hs`) with the argument and result C types; hsi's
  `createAdjustor` makes a function pointer in crt's text that applies the helper to a stable pointer and the marshalled
  arguments and runs it in a thread of its own (19 tests of `:callback`, 3 of `:test-c`); (b) a class in v1 (as now);
  (c) hsi boxes by the type string `createAdjustor` gets: measured on two wrappers, it holds the arguments only, one character
  each by width (`"dWb"` for a `CDouble`, a `CInt` and a `Word8`; `"L"` for an `Int64`; `"W"` for a `CInt`), not the signedness (`Int32` or
  `Word32`), not the result: unsound, so not proposed. *Recommended: (a)* at the next night: what the stgToCmm hook is given decides.
- **HES61 Waiting on descriptors.** (a) `waitRead#`/`waitWrite#` and `fdReady` poll the host's descriptor and the scheduler
  sleeps until one is ready or a delay expires (Go's `poll` through `syscall.Select` or `os.File`'s deadlines), so a thread
  blocked in `threadWaitRead` does not busy-wait and a `killThread` interrupts it (T21969); (b) the EAGAIN retry of HE5 (as
  built). *Recommended: (a)* with HE4f's scheduling work: one scheduler change.
- **HES62 The coverage subsets.** (a) a counting mode in hsi (`hsi run --reach FILE`: the primops, C functions and data labels a
  run reached) and `bin/hsi coverage` choosing greedily from the recorded runs the programs that reach every one, plus one per class
  and every control, into `test/haskell/hsi/essential.txt` (GT8.3); (b) by hand, as built. *Recommended: (a)* at the row's
  HE10: the essential share is 15 minutes for every crop.
- **HES63 forkProcess.** (a) a design class (as built: Go has no fork); (b) `forkProcess` re-executes hsi on the same program
  with the continuation's state serialized: not feasible without a heap image. *Recommended: (a)*.
- **HES64 A program's own C.** (a) ci's C forms beside hsi (HES11 (b)): the test's `.c` files read and compiled by ci and run with
  hsi's memory (`ci` over `crt`'s kernel, which hsi's `Mem` already is), the Haskell side's foreign calls resolved into them; the
  57 `:test-c` and `:test-c-output` tests; (b) a class in v1. *Recommended: (a)* at HE6's second half, after the cbits (done):
  ci already runs musl and `crt`'s kernel is the one hsi uses.

### HE7: the REPL (design only)

Not started. The plan's design, checked against GHC's API: the helper as a session server (`tools/hsdump`'s GHC API session,
`ghc --interactive`'s statements: `GHC.Driver.Main.hscParsedStmt`) takes a line, returns the STG of the expression GHCi would
compile (`hscCompileCoreExprHook`, a field of `GHC.Driver.Hooks.Hooks` in 9.14.1 as `:info` on the host's ghc package shows, is the hook GHC offers for the one step between Core and the interpreter's bytecode: the
helper's hook would run `corePrep`, `coreToStg` and the serializer instead of `byteCodeGen`, and return a placeholder
value), the interactive context's bindings named as external names of the session's modules, and hsi's REPL holds the loaded
STG of each line, evaluates it and prints with the `Show` instance GHC chose (`it`, `let`, `:t` and `:k` answered by the
server alone, from the GHC API, with no evaluation). The scripted session compared byte for byte against `ghc
--interactive`'s output on stdin. Two things to establish first: what `hscCompileCoreExprHook` is given for a `let` binding
(GHC compiles it as an expression returning the new bindings) and how the session's earlier names resolve in later STG
(by name: `Ghci1.it` style external names). A spike of one `1 + 1` is the first step (about 1 to 2 days); HE7's estimate of
3 to 5 days holds if the hook is enough.

## Decided (2026-11-05, the user)

HES60-HES64, each (a) as recommended: the helper dumps foreign exports' and wrappers' helpers and hsi makes real callbacks (decided at the next night by what the stgToCmm hook sees); descriptors waited on by poll with HE4f's scheduler work; coverage subsets by a counting mode and greedy cover at HE10; forkProcess a design class; a program's own C through ci beside hsi at HE6's second half.

## As built: HE6's second half (write-ahead)

Built 2026-11-05 on branch `haskell-he6` from `haskell-he5` (main `d86cb874` merged first, for the C crop's prebuilt
runtime, `arbace.c.runtime`), light work by day (2 jobs, no gate, no lock, the main checkout's stage 0), every result below on
lists of the testsuite's run tests a few dozen at a time, never a whole corpus. Nothing of Haskell is in `gate_key` or a gate
step (HES19 (a)); `go/arbace/ci` and `go/arbace/crt`, which hsi now imports, are unchanged (the C crop's, in `gate_key`).

### HES64 (a): a program's own C

- **`bin/hsi cnodes OUT [--refs FILE] [OPTION...] FILE...`** (`test/haskell/eval/cnodes.ae`, on stage 0): the test's C files
  read by repr/c and checked by ci's checker (`arbace.c.run/read-unit`, gnu23, the RTS's headers of the installed GHC on the
  include path: `HsFFI.h`, `Rts.h` read as they are), linked with what they need of musl through ci's prebuilt runtime
  (`arbace.c.runtime/link-text`, the fragments cached in `.tmp/ci-runtime/`) and with hsi's glue (`test/haskell/hsi/c/glue.c`:
  `fflush(0)` and musl's allocator always linked; `main.c`, a `main` of nothing when the files have none, since the linker
  wants one), into one node program. `--refs`: the C functions the Haskell calls (the dump's static foreign targets) that musl
  defines and the files do not, linked through a unit of references (`qsort` for a wrapper's comparator). About 8 to 10 s a
  program, once (`bin/hsi check` keeps it by the dump's key, which now holds the C files' text).
- **`hsi run --c NODES`** (`go/arbace/hs/cprog.ae`): the node program loaded into `go/arbace/ci` over the machine's own
  `crt.Mem` and `crt.Sys` (one address space, one table of descriptors), its static initializers run and musl initialized as
  `__libc_start_main` would (`__init_libc` over an initial stack's argv, environment and auxiliary vector), no `main` called.
  Then: a static foreign call of a symbol the C defines and hsi's table lacks calls ci's function (the C's stdio and
  allocator, `printf`, `puts`, `putchar`, `fflush`, `malloc`, `calloc`, `realloc`, `free`, are musl's whenever a C program is
  loaded: one stdout buffer for both sides, a block freed on either side); a label (`&sym`) of the C's object or function is
  its address in ci's space, a capi `value` import of a C object its value read by the result's representation; a dynamic
  call of an address of ci's text calls that function; hsi's own C functions take addresses past ci's (`funIndex`, 65,536
  functions in) and are natives of ci's program, so C can call a pointer to one, and a function the C calls that neither it nor
  musl defines is hsi's when its table has it (the RTS's API below); a C finalizer (`newForeignPtr` of a C function) runs
  in ci, and `finalizeWeak#` now runs a weak pointer's C finalizers at once, as `stg_finalizzeWeakzh` does (fptr01); an
  `Array#` or `ArrayArray#` argument (UnliftedFFITypes, T16650b-d) is a word per element, an element's identity; C's fault
  is the program's end by its signal (`hsi: C fault: ...`, status 128 + the signal).
- **`bin/hsi check`** builds a test's C: all.T's `.c` files, the `.c` beside a `.o` of a `pre_cmd` (the `_ghci` tests),
  `multi_compile_and_run`'s extra sources (`test/haskell/eval/run_plan.ae`: its `(tuple FILE OPTS)` list, T20735, T21305),
  `-optc` flags' `-D`, `-I`, `-U` (`-optc-D -optcFOO` is `-DFOO`, T17471).
- **Results** (`test/haskell/divergences.edn`'s `:test-c` and `:test-c-output` of HE5, 57 tests): **33 pass** (28 by the C
  alone, the rest with the stubs and the RTS's API below); of the 24 left, 10 are Cmm (`:test-cmm`), 3 need what ci or hsi's RTS
  API lacks (`:test-c`), 10 have no reference (GHC's own build of them failed, `:no-reference`, HES66) and conc038 needs a
  blocking safe call beside running threads (`:rts`).

### HES60 (a): Haskell called from C

- **The probe** (`tools/hsdump/probe/StubProbe.hs`, `bin/hsi stubs-build`, about 3 minutes to build, light: not the helper,
  so the dumps' key is untouched): what GHC 9.14.1 holds of a module's foreign exports and wrappers, at the end of the Core
  pipeline and at the stgToCmm hook. Answered by day on ffi002 and ffi005: the `ModGuts`' `mg_foreign` holds the module's
  stubs, the `_stub.h` header and the stub C GHC's C compiler compiles (for a foreign export `foo`, a C function `foo` that
  calls `rts_lock`, `rts_inCall(&cap, rts_apply(cap, ghc_hs_iface->runIO_closure, rts_apply(cap,
  &$fstableZC0ZCmainZCFooZCfoo_closure, rts_mkInt(cap, a1))), &ret)`, `rts_getInt`; for a wrapper, a C function
  `zdmainzdMainzdMainzuwrapIO(StgStablePtr the_stableptr, ...)` that applies `deRefStablePtr(the_stableptr)`); the exported
  helper `$fstable...` is an exported binding of the Core and **is in the STG the hook sees, so in hsdump's dumps already**
  (HE5's "not in the dump" was wrong: what the dump lacked was the stub C, which is not STG). No helper extension is
  needed beyond writing the stubs; the stubs come from the probe's `--stubs` mode, run by `bin/hsi check` (about 20 to 30 s
  a program, once, kept with the dump) for a program whose sources export a function or import a wrapper.
- **The stubs** are compiled by ci with the program's C (`DIR/BASE_stub.c`, GHC's prologue: `IN_STG_CODE 0`, `Rts.h`), and
  **the RTS's C API is hsi's** (`go/arbace/hs/rtsapi.ae`): a `HaskellObj` is a word standing for a machine value (a region no
  mapping has); a stub's closure label `X_closure` (undefined in the C, linked by ci at address 0) is rewritten at load to
  the word of the global it names (`$fstableZC0ZCunitZCModZCname` decoded); `ghc_hs_iface` a structure whose closures at
  DerivedConstants.h's offsets are `runIO`, `runNonIO`, `()`, `True`, `False`, `flushStdHandles`, `runMainIO`;
  `stable_ptr_table` the stable pointers' words (`deRefStablePtr` is an inline function of Rts.h reading it, entry `sp - 1`);
  `rts_mk*` box, `rts_apply` makes an application thunk, `rts_inCall`/`rts_evalIO` run the IO action on a thread of its
  own to its end while the calling thread waits in its foreign call (blocked to exceptions meanwhile: a `throwTo` to it waits,
  conc040), `rts_get*` force and unbox, `getStablePtr`, `hs_try_putmvar`, `hs_init*`, `hs_exit` (the handles flushed, the C
  finalizers run), `hs_perform_gc`. `createAdjustor` makes a function of ci's text that calls the wrapper's stub with the stable
  pointer first, and with a word where the stub declares GHC's x86-64 adjustor's `original_return_addr` (its index read from
  the stub by `bin/hsi check` into `NODES.adjust`; T24314's eight- and twenty-four-argument callbacks). A `-no-hs-main` program
  runs its C `main` through musl's `__libc_start_main` (`hsi run --c-main`).
- **Results**: of HE5's `:callback` (21, with nofib's two) **18 pass** (conc021, conc039, conc040, T1679, T24314, T2469,
  T2594, T4038, T4221, fed001, ffi006, ffi007, ffi008, ffi010, ffi011, ffi013, ffi019, and ffi020, whose C finalizer calls
  back, GHC's error: its message and status 1 as GHC's), nofib's two run but have no reference (`:no-reference`), conc038 is
  in `:rts`; the class `:callback` is gone. Of the C tests T19149, ffi016, T493 and ffi005 pass by it.

### HES61 (a): waiting on descriptors

`waitRead#`, `waitWrite#` and `fdReady` on the posix layer's host descriptors (pipes, spawned processes' ends) by `poll(2)`
(`go/arbace/hs/fdwait.ae`): a thread not ready blocks interruptibly (a `killThread` takes it off its wait), the scheduler
polls the waiters after its slices (every millisecond at most) and, when no thread can run, sleeps in `poll` until a
descriptor is ready or the first delay expires (100 ms at most with no delay, the non-threaded RTS's `awaitEvent`); a
waiting thread is a root of the collector; crt's descriptors are always ready (its files never block). HE4f's scheduling
work does not exist on the branch: this is the minimal scheduler change. With it `fcntl(F_GETFL)` of a pipe's write end says
`O_WRONLY` (`fdToHandle`'s mode; before, a write handle of `createPipe` was a read handle). T21969 (50,000 rounds of
`threadWaitRead`, `hClose` and `killThread` on pipes) passes in 15 s; the class `:fd-wait` is gone.

### HES43 (b): reasons per run

hsi writes why it stopped on a line of its own after its message, `hsi-reason: WORD` (`c-function`, `primcall`, `primop`,
`c-pointer`, `rts-api`, `deadlock`, `fatal`, `compile`, `load`, `c-fault`, `internal`); `bin/hsi check` records it as the
result's `:reason` (`:c-build` when ci refuses the C, `:limit` for a run killed at the check's limits, status 137 or 152), and a
class may name its `:reasons`, a member's reason among them (`bin/lib/hsi/check` and `bin/hsi divergences` both judge it).
The classes keep their `:ids` (HES43 (a)); `:reasons` replaces `:message` where both say the same, as the record is rerun.

### Classes after HE6

`bin/hsi divergences` over the record (`test/haskell/check-hsi.edn`, rerecorded today for the 181 testsuite programs of
the lists above, nofib's two callbacks and the samples and synthetic corpora; the other programs' lines kept): **789 of 907
pass** (737 before: the samples 24 of 25, synthetic 37 of 37, the testsuite's core **652 of 701** (600 before), nofib 76 of
144), the other 118 in a class, none unclassed: `:test-cmm` 10, `:test-c` 3, `:rts` 15, `:speed` 12, `:rts-options` 2,
`:process` 1, `:simd` 1, `:skipped` 1, `:no-reference` 72 (57 of nofib's, T17920, T24809, and the 13 tests and nofib's two
callbacks of HES66), `:async-revert` 1. Gone: `:test-c-output`, `:callback`, `:callback-c` (an intermediate split of the
day), `:fd-wait`. The lists' runs: the 104 recorded passes of the core's `concurrent`, `lib` and `ffi` (scheduling and
descriptors touched) all pass again; the samples and synthetic corpora 61 of 62 as before (AsyncRerun in `:async-revert`), the
four negative controls failing as they must.

### Night commands (listed, not run)

1. `bin/hsi stubs-build` (3 minutes), then `bin/hsi check --corpus samples,synthetic,testsuite-core,nofib --jobs 3 --record`
   under `bin/heavy-lock` (the HE5 night's command; about 15 minutes more than HE5's for the C and stubs of about 90
   programs, once), then `bin/hsi divergences` (must exit 0).
2. HE5's night commands 2 and 3 unchanged.

### Proposed (HES65 onward)

- **HES65 Where the stubs come from.** (a) the probe's `--stubs` mode, a second GHC compile of a program that exports or
  wraps (as built: 20 to 30 s once per program, kept with its dump; the helper and the dumps' key untouched); (b) the helper
  writes the stubs beside the STG in its stg mode (`mg_foreign` at its Core pass, as the probe does): one compile, but the
  helper's hash changes and every cached dump of every corpus is made again (a night's re-dump). *Recommended: (b)* at the
  next helper change that re-keys the dumps anyway; (a) until then.
- **HES66 The references GHC did not build.** Ten tests (ffi002, T5594, T5402, ffi023, conc059, hs_try_putmvar002 and 003,
  the `_ghci` three, T20735) and nofib's two callbacks have `:no-build` references: record-ref compiled the C before GHC wrote
  the Haskell's `_stub.h`, or did not link the `.o` a `pre_cmd` makes. (a) record-ref compiles the Haskell first (`ghc -c`
  with `-stubdir`), then the C with the stub directory on its include path, then links, as the testsuite's driver does with
  its ways' setup; (b) judge them against the testsuite's `.stdout` files (ffi002 and T5594 equal them today); (c) the
  class `:no-reference` (as built). *Recommended: (a)*: an hour of record-ref and a night's `--only` of the twelve.
- **HES67 Cmm.** (a) a class until HE8 (as built: 10 tests, `:test-cmm`, `foreign import prim` and `.cmm` sources); (b) the
  helper dumps the test's Cmm (GHC's Cmm parser's `CmmGroup`) as forms and hsi interprets them: weeks for 10 tests.
  *Recommended: (a)*.
- **HES68 C threads.** (a) a class until ci has threads (as built: hs_try_putmvar001's `pthread_create`, IncallAffinity's
  RTS OS-thread calls); (b) `pthread_create` in hsi as a Haskell-side green thread running the C function on ci's
  thread state (ci has one C stack, `Thread` per call chain: a second stack and the scheduler's switch between them, the C
  crop's work). *Recommended: (a)*, (b) when ci's REPL needs threads.

## Decided (2026-11-05, the user: HES65-HES68)

HES65: the stub writer a separate step now (a), folded into the helper at the next helper change that re-keys the dumps (b); HES66 (a): record-ref compiles the Haskell before the C; HES67 (a): Cmm a class until HE8; HES68 (a): C threads a class until ci has threads.

## As built: HE8 (write-ahead)

Built 2026-11-05 on branch `haskell-he8` from `haskell-he5` (`a7fafe0b`), on the host's GHC 9.14.1, TamaGo's go and the main
checkout's stage 0, light work by day (2 jobs, no gate, no lock; every Go build of a program under `GOMEMLIMIT=1536MiB`, `-p 2`).
Write-ahead: HE8's design and its first slice, not the milestone (§11's exit criteria: below, "The rest of HE8"). Nothing of
Haskell is in `gate_key` or a gate step (HES19 (a)). The AOT's files are its own (`go/arbace/hs/aot.ae`, `aotapi.ae`,
`test/haskell/hsi/cmd/aot.ae`, `bin/lib/hsi/aot`); the machine's files carry four hooks only (below), the driver one verb and
one counter, `bin/lib/hsi/check` the `--aot` option, so HE6's branch merges.

### The design

§7's shape, decided against the machine as HE4-HE5 built it: compiled code is a second implementation of hsi's node
trees' `eval`, not a second machine.

- **The code's shape.** One Go function per STG closure body, per case continuation whose scrutinee may push a frame, and per
  join point, each `func(m *Machine, env []Value) (Expr, []Value)`: a function of the closure's frame of slots (hsi's
  frames: free variables, arguments, every local binder, join points' arguments; `compile.go`'s numbering, liveness and
  scopes) that returns the next code to run and its frame. The trampoline is hsi's own loop (`runSlice`): Go has no tail
  calls and Haskell's stack is not Go's. Go's goroutine stacks for Haskell's stack (HES8 (b)) were rejected again: E7's
  `<<loop>>`, `-K`'s StackOverflow, asynchronous exceptions' unwinding, `control0#`'s captured segments, the collector's
  reachability walk and the thread switch at any bounce all need the stack as data, and with the explicit stack an AOT
  closure and an interpreted one call each other freely (the libraries interpreted, the program compiled, or any mix).
  Within one function the code is straight-line Go: atoms are `env[k]`, a global's value (`m.gv`, loaded on first use) or a
  number literal as a Go constant; a case on a pure primop or a literal never pushes a frame, so its alternatives are inline
  Go; a case on a variable takes the value at once when it is evaluated (a thunk's indirection followed once, as
  `CaseE.eval`) and pushes the case's frame and enters the thunk otherwise; a case on a constructor is a chain of pointer
  comparisons of its `DataCon`, on integer literals a Go `switch` (a duplicate literal's alternative dropped, the first
  wins as in hsi); a `let` allocates inline (`allocClosure`, `newConN`; `rec` in two passes); a jump assigns the join's
  slots and returns its function.
- **Calls, known and unknown.** A call's atoms go into the machine's argument buffer and `aotApp` takes `App.eval`'s paths
  on the values: an evaluated function of the call's arity is entered with its arguments placed in its frame (in a tail
  position the caller's frame reused when large enough), a class method's selector applied to an evaluated dictionary takes
  the method at once (the `-O0` programs' every `+`), anything else goes through the machine's `apply` (PAPs, over-saturated
  calls through apply frames, thunks). A callee compiled or not is the same `Lambda`: its `Body` is a `goCode` or a tree.
- **Laziness and updates.** Compiled code never evaluates a thunk itself: it pushes its continuation's frame and returns
  `m.enter(t)`, so blackholing, update frames, their squeezing, `NonTermination` and the waits on another thread's blackhole
  are the machine's, unchanged. Thunks and functions are allocated as hsi allocates them (one Go object up to four fields;
  an updatable thunk with room for its frame, evaluated in place).
- **Exceptions, asynchronous exceptions, masks, STM, continuations.** All are frames (`fCatch`, `fMask`, `fAtomically`,
  `fPrompt`...), and a compiled function holds nothing on Go's stack across a return: `raise#`, `throwTo`, `killThread#`, the
  masking states, `catchSTM#`/`catchRetry#` and `control0#`'s captured frames work on compiled code as on trees. A compiled
  case's frame is the tree's `CaseE` with its continuation in Go (`CaseE.k`): a frame copied by `control0#` resumes in Go.
- **Concurrency.** Green threads on hsi's scheduler (HES9 (a)): a thread is preempted at a bounce of the trampoline (a slice
  of 20,000 bounces; a compiled function runs longer per bounce than a node, so the slices are longer in time: scheduling
  points are a design class already). A compiled function never blocks; the primops that block return to the scheduler as
  in hsi.
- **The collector.** Go's for the heap. hsi's own reachability walk (deadlock detection, weak pointers: HE4e) reads the
  trees: each `goCode` keeps the tree it was generated from (`orig`), and a compiled case keeps its `CaseE` and alternatives.
- **Primops.** 48 pure scalar primops are inline Go over the atoms (`Int#`, `Word#`, `Char#` and `Double#` arithmetic and
  comparisons, each `Double#` result an explicit `float64` conversion so Go cannot fuse it); the other pure ones call the
  primop's function on the machine's buffer as `PrimE.simple`; effectful and control primops and the foreign calls run their
  node, hsi's implementations (one primop implementation for both levels: the classes are shared).
- **Installation by shape, and what the slice still loads.** The generated code does not rebuild the tree: at run time the
  closure is compiled from its dump (or decoded from its image) as before, and the installer registered for "UNIT:MODULE
  NAME" indexes that tree's nodes and atoms in a fixed preorder, checks a 64-bit digest of its structure (kinds, slots,
  arities, liveness lists, atoms' kinds) against the one the generator saw, and puts the Go functions in its places (the
  closure's `Body`, nested closures' bodies, join points' bodies, cases' `k`). Another dump or another compiler: refused,
  the closure interpreted (counted, `HSI_AOT_TRACE`). So the slice still loads the program's dumps and the libraries'
  images (declarations, constructors, globals, string literals' addresses): §7's program without dumps needs the metadata
  emitted too (HES71). Two latent nondeterminisms found on the way: `live.go`'s kill lists were in a map's order (images'
  bytes and shapes differed run to run: sorted now), and a string literal's or label's address is a number of the
  generating process (never a constant now: read from the node).
- **What is compiled, and the libraries once per key.** Every closure of the program's modules, and the library modules a
  list names: `bin/hsi aot`'s list is a profile, the modules whose closures a first run on hsi entered (`HSI_AOT_MODULES`),
  which decides only what is compiled, never what a run computes (a module not listed is interpreted). Each Haskell module is
  a Go package of its own, `arbace/aot/ID` (ID the module's key, `ghc_internal_GHC_Internal_Base`), over the machine's
  exported API (`go/arbace/hs/aotapi.ae`: the return registers, the argument buffers, `PushCase`, `Enter`, `AotApp`, the
  allocation helpers and the nodes' accessors, small enough for Go to inline across packages). A library module's package
  depends only on the module's image and the generator: it is generated once per (runtime digest, image digest) into
  `.tmp/hsi/aot/lib/KEY/ID` and compiled once by Go's build cache, and every program links the packages it needs by blank
  imports (`hs/cmd/hsi/aot_link.go`); a program's own modules are its only packages compiled per program. Why a package per
  module, measured: the first slice put every closure into package `hs` itself, and the Go compiler on one package of 21
  whole `ghc-internal` modules (4,206 closures, 9.2 MB of Go) peaked at 5.2 GB of RSS; then, with the closures selected by
  the profile alone, a synthetic program's main module (one closure of a thousand nested closures, 2.9 MB) still took 5.6 GB,
  because each closure's functions were Go closure literals inside one installer function: now each is a method of a struct
  of the captured nodes and globals, a function of its own for the compiler (the same program 1.5 GB under
  `GOMEMLIMIT=1536MiB`). The largest library package of the samples is `GHC.Internal.Classes` (5.8 MB: the tuples' derived
  instances), about 2 GB to compile, once.
- **Forms.** The generator writes Go text (the packages `arbace/aot/ID`) that TamaGo's go builds with the runtime printed
  from its forms. The route through Go forms was tried once on Nfib's 20 packages (4.8 MB of Go): read by repr/go's native
  reader (`bin/repr-go read --goos linux --goarch amd64`, mode `:lines`) in 2 min 39 s at 5.3 GB of RSS, printed back package
  by package (`bin/repr-go print`) in 25 s, built with the runtime and run: the same output, 67 closures installed, the
  printed text differing from the generated only in what mode `:lines` drops (the header comment, the shapes' hexadecimal
  spelling, blank lines). It works, at a cost a day's light work cannot pay per program: HES72.
- **Expected speed.** §7 expected 3 to 10 times hsi on call-heavy code. Measured on `nfib 32` (`-O0`: every `<`, `-`, `+` a
  dictionary method call): 1.2 times hsi, below §7's estimate (Results). The rest of the time is the machine's: frames pushed and popped, thunks allocated and
  updated, `Int#` boxed in interfaces, the bounces; HES73's typed locals and direct calls are the next factor.

### What is in the tree

- **`go/arbace/hs/aot.ae`**: the generator (`(*Machine).GenAOT`, `aotGen`: a package per module, a struct of captures and
  its methods per closure), the runtime half (`goCode`, `aotRegister`, `aotInstall`, `aotCollect`, `aotShape`, `aotApp`);
  **`go/arbace/hs/aotapi.ae`**, the machine's exported API to the generated packages; the profile (`HSI_AOT_MODULES`, `EnteredModules`) and the bisection's
  switches (`HSI_AOT_LIMIT=N`: the first N installs only; `HSI_AOT_NO=Kind,...`: node kinds left to their nodes). The hooks
  elsewhere: `compileTop` calls `aotInstall` after compiling or decoding a closure (`load.ae`), `CaseE` has the field `k`
  and `cont` calls it (`nodes.ae`), the collector's walk reads a `goCode`'s tree (`rts.ae`), the kill lists sorted
  (`live.ae`).
- **`test/haskell/hsi/cmd/aot.ae`**: `hsi aot [--lib DIR... --image DIR --modules FILE] [--prog DIR] --out DIR`; `hsi run`'s
  `--stats` counts the installed and refused closures.
- **`bin/hsi aot build --prog STGDIR [--closures FILE] --out DIR`** (`bin/lib/hsi/aot`): the program's packages generated into
  `DIR/gen`, the library modules FILE names taken from `.tmp/hsi/aot/lib/` or generated there once (under a lock), a library
  package over `HSI_AOT_MAX_KB` (default 4,096) left interpreted for the compile's memory (listed in `DIR/libs-skipped.txt`),
  then built with the runtime module `bin/hsi build` printed, into `DIR/hsi-aot`, kept by the generated files' and the
  runtime's digests;
  **`bin/hsi aot run FILE.hs [ARG...]`**: dumped, profiled on hsi with an empty stdin, built, run with the arguments and stdin.
- **`bin/hsi check --aot`**: each program profiled on hsi in a copy of its directory with its stdin and arguments, built, and
  run on its executable as `check` runs hsi, judged against the same references and classes (`:aot-build` its build's
  seconds), and against the profile's run on hsi (`:vs-hsi :same|:differs`, `:hsi-run` its seconds); results in
  `.tmp/hsi/check-aot/`, `--record` into `test/haskell/check-hsi-aot.edn`; `make hsi-check-aot`.

### Results

- **`bin/hsi check --aot` over the samples and the synthetic corpus** (the samples but Loop, whose 7 GB stack is a night's run;
  with HE8's new sample `Classes`: user classes with superclasses, default methods, existentials, constructor classes over
  `Stack` and `Queue`, an instance context on pairs and a polymorphic recursion, equal on hsi too): **61 of 62 equal** to GHC's
  runs (status, stdout and stderr byte for byte), the 62nd `AsyncRerun` in its class `:async-revert` as on hsi; none
  unclassed, the controls failing as they must; with the four nofib programs below, 66 programs recorded in
  `test/haskell/check-hsi-aot.edn`, each also against its own run on hsi (the profile's run, the same stdin, arguments and
  limits: `:vs-hsi`, diffcheck's kind): **all 66 the same** as hsi's (status, stdout, stderr), and `:hsi-run` beside `:run`
  the two levels' seconds of the same run. Every closure the runs entered in the compiled modules was installed (none refused by shape) but those of `GHC.Internal.Classes`, whose 5.8 MB
  package is over the day's `HSI_AOT_MAX_KB`.
- **GT8's essential list** (`test/haskell/hsi/essential.txt`: 14 samples, 36 synthetic programs and 73 of the testsuite's run
  tests, chosen by HE4d for speed), `bin/hsi check --aot --corpus samples,synthetic,testsuite-core --only
  @test/haskell/hsi/essential.txt` at 2 jobs after the morning's gate: **123 of 123 equal** to GHC's runs and **all 123 the same
  as hsi's**, the four controls failing as they must; 25 minutes of builds (the testsuite's programs' library modules compiled
  for the first time; the longest build 4.8 minutes, a program entering many new modules). With the runs above,
  `test/haskell/check-hsi-aot.edn` holds 139 programs: 138 pass, AsyncRerun in its class, every one the same as on hsi.
- **The builds**: the check's first pass at 1 job, every library package compiled for the first time, 23 minutes of builds
  for 62 programs (the 211 library modules the samples and the synthetic programs enter: 85 MB of Go); the second pass, those
  packages from Go's build cache, the program's own packages alone compiled: 0.4 to 15 s a program (the synthetic programs'
  main modules, up to 3 MB of Go each, the longest), the peak of one compile about 1.5 GB (2.8 GB for `Data.Sequence.Internal`
  before the size limit). Each run's own time is a fraction of a second, as on hsi.
- **Every module of the libraries generated** (`hsi aot --modules` over the 1,320 modules of the 41 packages, no build): 88,465
  closures, **none refused**, 337 MB of Go in 2 min 22 s at 1.9 GB of RSS; 8 modules over 4 MB (60 MB together; the largest
  `Distribution.Simple.Configure` 14.6 MB, `GHC.Internal.TH.Syntax` 10.1 MB, `Data.Sequence.Internal` 9.5 MB, `GHCi.Message`
  8.7 MB, `GHC.Internal.Classes` 5.8 MB).
- **Speed** (the same host, one after the other, wall seconds): `nfib 32` (Nfib, `-O0`) hsi 5.7-6.0 s, compiled 4.7-4.8 s
  (1.2 times; 176 million bounces of the trampoline on hsi, 46 million compiled; GHCi 3.6 s, GHC's `-O0` code 0.32 s, HE4d's
  figures); four of nofib's programs that pass on hsi, each run by `bin/hsi check` then `check --aot` (their CPU limits, their arguments): `imaginary/tak` 25.1 → 21.5 s (1.17), `imaginary/rfib` 25.5 → 20.8 s (1.23), `spectral/fibheaps` 29.7 → 28.2 s (1.05), `spectral/sphere` 22.1 → 20.3 s (1.09), all four equal to GHC's runs compiled too. A profile of the compiled nfib: 42 % in the machine's returns (`ret`, `pop`, the case frames' arena),
  15 % in `aotApp`, 16 % allocating thunks, the generated functions themselves a few percent: the dispatch hsi spent is gone,
  and what is left is the machine's frames and heap, which compiled code shares by design (§7's estimate of 3 to 10 times
  assumed the frames and boxing cheaper than they are). HES73 is where the next factor is. Tried and left out: returning a
  value straight into a compiled case's continuation from the compiled code (no bounce of the trampoline, at most 32 calls
  deep): no measurable gain on nfib, the frame's pop and the arena's reset being the cost, not the bounce. (The recorded
  `:run` and `:hsi-run` of the nofib four were measured later on a host three times as loaded: 1.04 to 1.23 times there too.)

### The rest of HE8 (re-estimated: 8 to 12 days)

1. **The libraries whole** (HES70; 2-3 days): the package per module is built (above); left: the 41 packages' 1,320 modules
   generated and compiled at night once per key (measured: the 211 modules the samples and the synthetic programs enter are
   85 MB of Go and compiled in about 23 minutes at `-p 2`; all 1,320 are 337 MB, so about an hour and a half; the largest,
   `Data.Sequence.Internal` 9.5 MB, takes about 3 GB to compile: under the heavy lock, HES73's code size), a program then
   needing no profile, and hsi itself linking them (HES14 (b)).
2. **The metadata emitted** (HES71; 2 days): data declarations, globals, string literals and labels as Go, so a compiled
   program loads no dump or image; then hsi with compiled libraries (HES14 (b)) is the same packages linked into hsi.
3. **Speed** (HES73; 2-3 days): `Int#`/`Word#`/`Double#` binders as typed Go locals (no interface boxing between primops),
   a call to a known compiled closure of known arity as a direct Go call when its result returns to a pushed frame anyway,
   a case on a known constructor's tag as a jump table.
4. **The check whole** (1-2 days): `bin/hsi check --aot` over the testsuite's core and nofib at night (the programs' builds
   are the cost: a minute or less each, 1.5 GB), hsi against compiled per program (diffcheck's kind), the AOT's own classes.

### Proposed (HES70 onward; HES65-HES69 left to HE6's branch)

- **HES70 The libraries whole.** (a) every module of the 41 packages generated and compiled at night once per (runtime,
  image) key, the packages kept in `.tmp/hsi/aot/lib` and Go's build cache, `HSI_AOT_MAX_KB` raised for the night (the
  largest packages take 3 GB to compile: under the heavy lock), every program and hsi itself (HES14 (b)) linking all of them,
  no profile; (b) the profile's modules only (as built): a program's first build compiles the modules no earlier program
  needed; (c) a size budget per module first (HES73 (b)), then (a). *Recommended: (a)*: 337 MB of Go, about an hour and a
  half of compiling once per key (the 211 modules' 23 minutes scaled), fits a night; the 8 modules over 4 MB under the heavy
  lock.
- **HES71 The metadata.** (a) the generator emits each module's data declarations, globals, string literals and labels, and
  its trees' constants, so a compiled program needs no dump, no image and no loader at run time (a standalone executable, as
  Go's AOT gives); (b) keep loading them (as built). *Recommended: (a)*, after HES70.
- **HES72 Forms.** (a) the generator's Go read into Go forms by repr/go's native reader and built by `arbace.build.go` (goi's
  `check --aot`'s route), the forms committed for nothing (generated); (b) the generator writes Go forms itself (as c2j writes
  Java forms): the generator in Arbace over the dumps' forms; (c) Go text built directly (as built). *Recommended: (a)* at
  HE8's end, for the row's AOT cell to be "STG → Go forms" literally, for the libraries' packages once per key (read at night:
  5.3 GB for 4.8 MB of Go, so the 337 MB of all modules a package at a time under the heavy lock); (b) only if the generator moves to Arbace for HE7's
  REPL anyway.
- **HES73 Speed before size.** (a) typed locals for unlifted binders and direct calls between compiled closures (above),
  measured on nfib and nofib's `:speed` nine; (b) size first (a smaller expansion per node, e.g. the continuations' common
  tails shared) to make HES70's packages cheaper to compile. *Recommended: (a)*, HES70's per-module packages bounding the
  compile's memory anyway.

## Decided (2026-11-05, the user: HES70-HES73)

All (a): the 1,320 library modules compiled at night once per key under the heavy lock; then the metadata emitted for a standalone executable; the build through Go forms at HE8's end; speed before size (typed locals for unlifted binders, then direct calls between compiled closures).
