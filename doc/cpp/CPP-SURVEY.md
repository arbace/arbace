# C++ (C++23) as a future crop — survey and plan

Status: survey and plan, 2026-10-30, at the user's request (a further language for the REAP matrix: read, eval,
AOT, print). Nothing here is adopted: no code under `arbace/`, `go/`, `bin/` or `test/` changed, and no Arbace
executable was built or run (GT7's clean run was going: light work only). The decisions are the user's, as proposals
CP1-CP14 (§12); the milestones CX1-CX10 (§11) are named by CP-order's neighbour, CP9.

Base: branch `cpp-survey`, from main `cacdcba5`. The probes are a few seconds each (`g++`, `clang++` on files of 3 to
10 lines, §2.4); the scratch is the worktree's `.tmp/cpp/` (Appendix A). The counts of the gcc testsuites were made
from the pinned gcc 15.2.0 tarball already on the host (`.tmp/c1/gcc/gcc-15.2.0.tar.xz`, C23's CS15), read into the
scratch; nothing was downloaded. What the host does not hold (clang's and libc++'s tests, llvm-test-suite) is named
with source and license from memory of the projects, and every size or license marked "to verify" is verified at the
fetch the user decides (CP8).

Words used throughout:

- **C++ forms**: C++ programs written as Arbace forms (§3), the counterpart of the C forms (doc/c/SPEC.md), the Go
  forms (doc/go/SPEC.md) and the Java forms (doc/classes/SPEC.md).
- **repr/cpp**: the bridge between C++ text and C++ forms (`arbace.repr.cpp`, `bin/repr-cpp read`, `print`,
  `roundtrip`, `check`), as repr/c and repr/go are.
- **The oracle**: the tool that gives the semantic analysis Arbace does not have: clang 23.1.4's AST after Sema, its
  LLVM IR (§2, §5). **The judge**: the reference whose behaviour a result is compared with: g++ 15.2.0 (§2).
- **LLVM forms**: LLVM IR text as Arbace forms (`arbace.repr.llvm`, §5.2), the input of eval and AOT.
- **cppi**: the evaluator of C++ at the REPL (the name is CP14's): the lowering of §5 over ci (C23's interpreter).

Contents: Summary · 1 What "C++ as C++ forms" means · 2 The host and the reference · 3 C++ forms · 4 repr/cpp: read
and print · 5 eval · 6 AOT · 7 The standard library · 8 The corpora · 9 How it fits C23's pieces · 10 The row ·
11 Plan (CX1-CX10) · 12 Proposals (CP1-CP14) · 13 What the user decides first · 14 Risks · 15 Sources · Appendix A

## Summary

- **C++ is the largest crop by an order of magnitude, and its meaning cannot be had without a semantic analyser.**
  gcc's C++ front end, `gcc/cp/`, is 288,409 lines of C++ in the pinned tarball (reference only); clang's Sema and
  Parse are of the same order (to verify). Parsing needs lookup (`a<b>(c)`, `T(x)`, `U u(T());` is a function, probe
  P6), and meaning needs overload resolution, template argument deduction, instantiation, SFINAE, concepts,
  constexpr evaluation and ADL. Arbace's own C++ Sema is out of this plan by scale (hundreds of days) and said so:
  **every plan below takes Sema from the oracle**, as OCaml's v1 takes types (OC4 (a)) and repr/haskell and
  repr/rust take everything from their compilers.
- **The oracle is on the host and is two compilers.** `g++` 15.2.0 (Alpine, musl, `-std=c++23`) and `clang++` 23.1.4
  (same target, same libstdc++ headers: `clang++ a.cpp` compiles against `/usr/include/c++/15.2.0` as probed). No
  libclang or libTooling headers are installed (no `clang23-dev`/`llvm23-dev`, no `clang-tidy`, `clang-query`, `opt`,
  `llc`, `lli`, `llvm-dis`; libc++ is absent); what works is clang's **`-ast-dump=json`** and **`-S -emit-llvm`**.
- **The AST dump is enormous and the IR is small, and that decides the architecture (P3).** A 7-line program that
  includes `<vector>` gives **127 MB** of JSON AST; a 4-line one with `<iostream>`, `<vector>`, `<algorithm>` gives
  **608 MB** (8.5 s of clang). The same programs' `-O0` LLVM IR are **40 KB (31 functions) and 155 KB (59
  functions)**: clang has already instantiated the templates, run the constructors and destructors into the code,
  laid out the classes and lowered exceptions to `invoke`/`landingpad` and coroutines to split functions. An AST-fed
  eval would have to re-implement clang's CodeGen on 600 MB of JSON per program; an IR-fed one reads kilobytes.
- **Recommendation: read/print oracle-fed from the filtered AST (§4), eval and AOT from the oracle's LLVM IR lowered
  to C forms (§5-§6).** The reader (a helper, `tools/cxxdump`) keeps the main file's declarations only; repr/cpp
  shares C23's lexer and preprocessor (C2, C3) and `:full`'s item-granular text (C3b). Eval and AOT: LLVM IR as LLVM
  forms (`arbace.repr.llvm`), lowered to **C forms** (`arbace.cpp.lower`: one lowering, with exceptions as a pending
  flag), then **C23's ci (C5) and AOT (C6) unchanged**, with libstdc++ (the host's, GPL-3 with the Runtime Library
  Exception) compiled to IR by the oracle and lowered the same way. C++ semantic analysis is therefore the oracle's,
  recorded as a stated divergence from parity (§1), as OCaml's v1 types are.
- **Plan (§11): CX1-CX10, about 55 to 91 working days on top of C23's C1-C9** (which C++ needs: ci and AOT):
  read and print CX1-CX5 (24 to 38) need only C2-C3b, which are done, and can start now; eval and AOT CX6-CX8 (26
  to 44) need C5 and C6. An optional native parser (name-kind tracking, validated by the oracle, +25 to 45) and an
  own checker for the C-with-classes subset (+15 to 25) are listed, not in the total.
- **v1 scope (CP1):** C++23 as g++ 15 and clang 23 accept it, **read and print for the whole language but named
  modules' built interface files**; **eval and AOT for single-translation-unit hosted programs** over libstdc++ with
  no threads, no filesystem, the "C" locale (classes, templates, overloading, RAII, exceptions, RTTI, lambdas,
  concepts, constexpr, coroutines all come out of the oracle's IR). Corpora: the pinned gcc 15.2.0's `g++.dg` (18,066
  files) and libstdc++ testsuite (9,725 `.cc`), go-whim's whim++ (75,292 lines), jdk26u's HotSpot as a read corpus
  (1.41 M lines of C++), libstdc++'s own headers (905 files).
- **To decide first (§13):** the scope (CP1), the oracle and the apk (CP2), the eval path (CP4), the fetch of clang's
  tests (CP8), and the order against C23, Scheme and OCaml (CP9).

## 1. What "C++ as C++ forms" means

The crops' rule (doc/FULL-SUPPORT-SURVEY.md, "The binding decisions" and "REAP v1's divergences"; C23's §1; OCaml's
§1), for C++:

| cell | in Arbace (proposed) | corpus | reference | judge |
|---|---|---|---|---|
| **C++ read** | repr/cpp `read`: Arbace's preprocessor (shared with C) and the oracle's AST (the filtered dump) | §8's corpora, valid and invalid | g++ 15.2's acceptance; clang's AST of the original; the object code of the original | forms printed and compiled give the original's object file byte for byte (C3b's level 3); a file g++ rejects is rejected (and its diagnostics are the oracle's, not Arbace's) |
| **C++ eval** | cppi: LLVM forms of the oracle's IR, lowered to C forms, run by ci | the run programs | g++ `-O0`'s build and run of the originals (status, stdout) | status and stdout byte for byte; the classes of §14 |
| **C++ AOT** | the same C forms through C23's C forms → Go forms (C6) | as eval | as eval | as eval |
| **C++ print** | repr/cpp `print` (`:lines`, `:full`) | every corpus | the original text; the forms read back; clang's AST and object code of the print | `:full` byte for byte; `:lines` forms → C++ → forms equal, the AST of the print equal to the original's, its run equal |

Same as the crops: linux/amd64 host alone, forms only (no C++ text typed at the REPL), results recorded as data,
`bin/reap` judging from the records.

**What differs from every other crop:** Arbace has no semantic analysis of its own for C++, so the eval cell is
"the oracle compiled this, Arbace runs what it produced". That is what OCaml's v1 does with types (OC4 (a)) and what
repr/haskell and repr/rust do for the whole language, but here it also holds for **template instantiation and
overload resolution**, the heart of the language. The check needs the oracle to produce (clang for the IR), or the
IR recorded; the references (g++'s runs) are recorded as data (C23's CS14).

**What "C++ forms" means beyond the C forms (each detailed in §3):**

| feature | what the forms must carry | where its meaning comes from |
|---|---|---|
| namespaces, `using`, `inline namespace`, aliases | scopes and lookup-affecting declarations as written | the oracle's lookup |
| classes: access, members, static members, bases (virtual too), friends, nested, local, anonymous | the declaration as written, `public`/`private` sections, member initializer lists, `= default`/`= delete`, `override`/`final` | layout and vtables: the IR |
| constructors, destructors, conversion functions, operators, user-defined literals | as functions with their special names, and the **operator spellings** (`operator<=>`, `and`/`or`/`not`, digraphs) | the calls the IR already contains |
| RAII | not a form: it is semantics. The source has no destructor call; the IR has it (and the cleanup landing pads) | clang's CodeGen |
| overloading, ADL, default arguments, references, `this`, `auto`, `decltype`, structured bindings | as written; resolved callees are the oracle's | the oracle |
| **templates**: function, class, variable, alias, member, variadic, template template parameters, partial and explicit specialization, deduction guides | the definitions *uninstantiated*, dependent expressions as written; their instances as facts (`:cpp/instances`) | instantiation: the oracle |
| **concepts, `requires`** | constraint expressions as written; satisfaction is compile-time only | the oracle |
| exceptions | `try`/`catch`/`throw`, `noexcept`, function-try-blocks | `invoke`/`landingpad` in the IR (§5.4) |
| lambdas | the capture list and body as written, a closure type the oracle names | the IR's call operator |
| constexpr, consteval, constinit, `if constexpr`, `static_assert` | specifiers and statements as written; no constexpr interpreter in v1 | folded by the oracle |
| **modules** (C++20) | `export module`, `module :private`, `import`, header units, `export` | read/print only in v1 (CP13) |
| **coroutines** | `co_await`, `co_yield`, `co_return` as written | clang's coroutine lowering (§2.4, P8) |
| the preprocessor | shared with C (§9): the same directives, plus `__cplusplus`, `__has_cpp_attribute`, raw strings, `import` as a token | Arbace's own, judged by `g++ -E` |

## 2. The host and the reference

### 2.1 The compilers

- **g++ 15.2.0** (`g++ (Alpine 15.2.0) 15.2.0`, target `x86_64-alpine-linux-musl`), `-std=c++23` accepted. GCC 15 has
  no `std::execution`, no reflection, no contracts (go-whim's `doc/CPP.md` §1). Its libstdc++ is 15.2.0 (headers 14 MB,
  `/usr/include/c++/15.2.0`). Its plugin headers are installed (`/usr/lib/gcc/x86_64-alpine-linux-musl/15.2.0/plugin/include`),
  so a gcc plugin dumper is possible with no apk; `-fdump-tree-original-raw` and `-fdump-lang-class` work.
- **clang++ 23.1.4** (`Alpine clang version 23.1.4`, same target), `/usr/lib/llvm23/bin` holding only the driver tools
  (`clang`, `clang++`, `clang-cpp`, `clang-installapi`, `clang-ssaf-*`, ...). Clang 16 and 22 are also installed side
  by side (llvm15, 16, 22, 23 libraries). `libclang-cpp.so.23.1` is installed, **its headers are not**.
- **Not installed:** clang23-dev and llvm23-dev (the libTooling/libclang headers; package names to verify with
  `apk search`), `clang-tidy`, `clang-check`, `clang-query`, `opt`, `llc`, `lli`, `llvm-as`, `llvm-dis`, libc++.
  clang takes `.ll` files as input (`clang++ -c a.ll` gives an object **byte for byte equal** to the one of `a.cpp`,
  P7), so the IR has a round-trip judge with no LLVM tools.
- **Pins.** gcc: `gcc-15.2.0.tar.xz`, SHA-256 `438fd996826b0c82485a29da03a72d71d6e3541a83ec702df4271f6fe025d24e`
  (101,056,276 bytes), already pinned by `test/c/corpora.edn` for C23 and holding gcc/cp, libstdc++-v3 and the whole
  testsuite. LLVM: `llvm-project` at the release tag of clang 23.1.4 (expected `llvmorg-23.1.4`; the tarball's
  SHA-256 and size are read at the fetch, CP8), plus Alpine's `clang23` APKBUILD sha as C23 would pin gcc's.

### 2.2 The oracles

| oracle | gives | cost on the host | gaps |
|---|---|---|---|
| **clang `-Xclang -ast-dump=json`** | the AST after Sema: declarations with ranges (byte offsets, line, column, token length, macro spelling and expansion locations), types as strings (`qualType`, `desugaredQualType`), `referencedDecl` per use, implicit casts, `CXXConstructExpr`, `CXXBindTemporaryExpr`, `ExprWithCleanups`, template definitions with their implicit and explicit instantiations under them, `isImplicit`, mangled names | 8.5 s and 608 MB for `<iostream>`+`<vector>`+`<algorithm>` (P3); the helper must filter | no comments; no tokens; implicit special members have no bodies; destructor calls at scope end are not nodes; record layout is a separate flag (`-fdump-record-layouts`, P5, not exercised) |
| **clang `-S -emit-llvm -O0`** | the lowered program (§5) | 40 KB, 155 KB for P3's programs | target-specific; clang-version-specific |
| libclang / libTooling | the same AST as a C++ API, with `Lexer` tokens and `Rewriter` | **needs an apk** (clang23-dev, llvm23-dev) | not installed; the pattern of `tools/hsdump` and `tools/rsdump` |
| **g++** `-fsyntax-only`, `-E`, `-c`, `-O0` runs; `-fdump-tree-original-raw`, `-fdump-lang-class` | acceptance, preprocessing, object code, behaviour; gcc's GENERIC and class layouts as text; a plugin can hold the trees | seconds | GENERIC is lowered further than the AST and is a text format; no JSON |

So the proposal: **clang is the structural oracle** (AST for read, IR for eval), **g++ is the judge** (acceptance, the
object code, the runs), each the other's second opinion where they disagree on a valid file (C23's §8.1 rule:
"classed, not judged").

### 2.3 Prior art

- **go-whim `wpp/`** (whim++): the editor core of vim as C++23, **75,292 lines** (`editor.cpp` 61,921, `editor.hpp`
  11,090, `printf.cpp` 1,497, `term.cpp` 380, `host.cpp` 195, `host.hpp` 92, `rt.cpp` 61, `rt.hpp` 24, `main.cpp` 17, `term.hpp`
  15), built by g++ with `-std=c++23 -O2 -Wall -Wextra` and no warning, **generated** from the C by `crefactor/togo`'s
  C++ backend (4,079 lines of Go in `cpp*.go`). Its C++ is **C printed as C++**: one `class Editor` holding every
  global, member functions, named casts (5,719 of them), `enum class` for 11 of 24 enums, `static constexpr` tables,
  pointers to members, `[[nodiscard]]`, `std::unique_ptr`, `std::jthread` and a few templates. Counted over its
  `src/`: `template` 15 lines, lambdas 3, `throw`/`try`/`catch` 172 (`host_exit` is an exception), `virtual` 82, `constexpr`
  1,182, `concept`/`requires` 4, `co_await` 0, `std::` 70. It is **not** a template-heavy or library-heavy corpus: it
  is the C-with-classes subset, which makes it the right v1 acceptance corpus for the C pieces and a poor test of
  templates. `doc/CPP.md` (270 lines) and `doc/CPP-IDIOMS.md` document the backend; its own lesson is that **C++ is not
  C's superset in the details** (jumps past initialisation, narrowing in braces, `void *` conversions, assignment
  order, uninitialised locals, `char8_t`), the list §9 uses.
- **There is no C++ front end in go-whim** (as there was one for C: `crefactor/cc`); `togo` prints C++, it does not read
  it. So C23's route (a port of the existing front end, oracle by the port's original) has no counterpart.
- **Arbace's own:** repr/c's lexer, preprocessor and parser (C2, C3, ports of modernc.org/cc, trees equal to cc's on
  6,300 of 6,387 corpus files), converter and printer (C3b), C forms (doc/c/SPEC.md), the checker C4 and ci C5 and
  AOT C6 to come. repr/haskell and repr/rust show the helper pattern for a language whose front end Arbace cannot
  have: a dumper on the compiler's own API, forms and printer in Arbace, the compiler the oracle.

### 2.4 What the probes showed (`.tmp/cpp/`)

- **P1.** `g++ -std=c++23 -fsyntax-only a.cpp` accepts a 7-line file with a class template, a concept, a constrained
  function template, a class with a destructor, a lambda capturing by reference, `try`/`throw`/`catch` and a
  `std::vector` brace initialiser. Preprocessed, `<vector>` is 21,443 lines / 745 KB, `<iostream>` 48,213 lines /
  1.63 MB, `<ranges>` 1.30 MB, `<print>` 1.56 MB, `<format>` 1.56 MB. `g++ -fsyntax-only` takes 0.26 s (`<vector>`) to
  1.4 s (`<print>`). **A corpus of 10,000 files that include `<iostream>` is 16 GB of preprocessed text**: the
  headers' token streams must be cached by file digest (C23's CS36 does it already for libc headers).
- **P2.** `clang++ -std=c++23 -Xclang -ast-dump -Xclang -ast-dump-filter=twice` prints the template and its implicit
  instantiation (`twice 'int (int)' implicit_instantiation instantiated_from ...`): templates and their instances are
  both in the dump.
- **P3.** The JSON dump (`-ast-dump=json`) of P1's file: **127,230,453 bytes**, 131 top-level declarations, of which
  8 are the file's. Of `io.cpp` (`<iostream>`, `<vector>`, `<algorithm>`, a sort and a loop): **607,664,490 bytes**,
  8.5 s. The main file's declarations carry `loc` with no `file` key and no `includedFrom`, so a streaming filter (a
  Go helper, `tools/gocmp`'s kind, TamaGo's Go) can keep them; `-ast-dump-filter=NAME` filters by declaration name
  only, and the filtered dump of `twice` is 15,409 bytes.
- **P4.** The same programs as IR (`clang++ -std=c++23 -O0 -S -emit-llvm`): P1's **912 lines, 40,247 bytes, 31
  `define`s, 15 `invoke`/`landingpad`**; `io.cpp`'s **3,258 lines, 155,280 bytes, 59 `define`s**. Instances such as
  `vector<int>::vector(initializer_list)` and `std::sort`'s internals are in it as `linkonce_odr` functions, already
  instantiated. Four thousand times smaller than the JSON.
- **P5.** `-Xclang -fdump-record-layouts-simple` printed nothing for a header-free class template (not
  investigated; CX3 finds the way to get layouts, or takes them from the IR's struct types and the data layout string).
- **P6.** The most vexing parse: `struct T{}; struct U{ U(T){} }; int main(){ U u(T()); }`. clang reads `u` as a
  function declaration (`FunctionDecl u 'U (T (*)())'`) with a `-Wvexing-parse` warning, as the standard says. The oracle
  resolves it; a native parser would need the declaration-or-expression rule (§4.4).
- **P7.** `clang++ -c a.ll -o a2.o` **equals** `clang++ -std=c++23 -O0 -c a.cpp -o a1.o` byte for byte (`cmp`): the IR is
  a complete, deterministic stand-in for the source at the object level, and clang is also LLVM IR's assembler.
- **P8.** Coroutines: `co.cpp` (`std::suspend_never`, `co_return`) compiles with g++ 15 and clang 23; clang's `-O0`
  IR has 50 lines with "coro" in them but no `llvm.coro.*` intrinsic call: **the coroutine passes ran at `-O0` and the
  IR is the split ramp/resume/destroy functions** (to confirm in CX1 with a `co_await` that suspends).
- **P9.** Predefined macros: `clang++ -std=c++23 -E -dM` 502, `g++ -std=c++23 -E -dM` 495. Include search list (both):
  `/usr/include/c++/15.2.0`, `.../x86_64-alpine-linux-musl`, `.../backward`, `/usr/include/fortify`, `/usr/include`,
  clang's own `/usr/lib/llvm23/lib/clang/23/include`. The host data C2 records for C (`test/c/host/`) has a C++ twin.
- **P10.** `import std;` with `-fmodules` fails in g++ ("failed to read compiled module"): GCC 15 builds `std` from
  `bits/std.cc` into a gcm first; nothing prebuilt on the host.

## 3. C++ forms

### 3.1 Principles

The same as the other crops': data the Clojure reader reads, one form per syntactic category, positions in mode
`:full` only, names as the source spells them, forms in the C forms' vocabulary where C++ means what C means
(`c/var`, `c/defn`, `(* T)`, `(set! x + e)`, `bit-and`). **C++ forms extend C forms** (CP10): a declaration that is
valid C with C's meaning is the same form; heads in the `cpp/` family carry everything else (`cpp/namespace`,
`cpp/class`, `cpp/template`, `cpp/using`, `cpp/static-cast`, `cpp/new`, `cpp/lambda`, `cpp/try`, `cpp/concept`,
`cpp/requires`, `cpp/module`, `cpp/import`, `cpp/co-await`). The **printer** is the new large piece (§4.3), since
the declarator and template syntax of C++ is nothing C's printer (cemit's rules) knows.

### 3.2 Two kinds of facts

The reader (§4) produces, per translation unit:

1. **Source forms**: what was written, once: template definitions uninstantiated with dependent names as written
   (clang keeps them as `UnresolvedLookupExpr`, `CXXDependentScopeMemberExpr`, `DependentScopeDeclRefExpr`), default
   arguments, implicit members *not* included (`isImplicit` is dropped; `= default` is kept).
2. **Facts from the oracle**, attached as metadata or in a table, not in `:lines`: each expression's type
   (`:cpp/type`), its resolved declaration (`:cpp/ref`), the implicit conversions, each template's instances
   (`:cpp/instances`: name, arguments, mangled name), record layouts. These are what a later checker (CP11's
   extension) would compute itself, and what the eval path of §5 does not need at all.

### 3.3 Operator spellings and the other lexical alternatives

`and`, `or`, `not`, `xor`, `bitand`, `bitor`, `compl`, `and_eq`, `or_eq`, `xor_eq`, `not_eq`, the digraphs `<% %> <: :> %: %:%:`,
`<=>`, trigraphs (gone in C++17), raw string literals `R"d(...)d"`, user-defined literal suffixes, `'` digit
separators, `u8`/`u`/`U`/`L` literals, `char8_t`: the lexer (shared with C, extended, CX2) keeps each token's
spelling; the forms' `:lines` print the canonical symbolic spelling and `:full` (item-granular text, as C3b's, CS70
(a)) keeps the original bytes. The AST has no trace of `and` versus `&&`, so the **tokens** are the only source
of the alternative spelling; that is the one place where a token-level agreement of the shared lexer with the
oracle's source text is needed.

### 3.4 What is not in C++ forms in v1

Named modules' compiled interfaces (gcm, pcm), `import std;`'s standard module (P10), C++26 (reflection, contracts,
pack indexing beyond what the oracle accepts at `-std=c++2c` — the dialect is data, CP1), GNU and clang extensions
beyond what libstdc++ and the corpora need (attributes are kept as C23's CS72), `#pragma` beyond the layer's.

## 4. repr/cpp: read and print

### 4.1 The pipeline

```
C++ text --(shared lexer + pp, Arbace's)--> tokens, layer (directives, inclusions, macro spans)
         --(oracle: clang -ast-dump=json, filtered by tools/cxxdump)--> main-file declaration tree
         --(arbace.repr.cpp.convert)--> C++ forms (+ facts)       [read]
C++ forms --(arbace.repr.cpp.print: :lines, :full)--> C++ text     [print]
```

`bin/repr-cpp read`, `print`, `roundtrip`, `check`. The `.ae` file layout is C3b's (a top-level item a line, header
units). The preprocessor is **Arbace's** (the same ported code as C's, CX2), judged by `g++ -E` digests recorded as data
(C23's CS51); the AST comes from the oracle run on the original text, so the oracle and Arbace's preprocessor must
agree on every file, and where they do not the file is known with a class (as C3's 61 token-check files).

### 4.2 Read: the oracle-fed route (recommended, CP3 (a))

- **The helper `tools/cxxdump`** (CX3) runs `clang++ -Xclang -ast-dump=json -fsyntax-only` on the file with the
  corpus entry's flags, **streams** the JSON and keeps the declarations whose location is in the files asked
  (the main file; header units' files when a corpus names them), plus the facts of §3.2 for them, and writes
  a compact S-expression in the dumpers' style (`tools/godump`, `tools/hsdump`). Streaming matters (P3: 608 MB). A
  Go helper needs no apk; it can be **replaced by a libTooling dumper** (C++ on `clang23-dev`, with real tokens and
  record layouts) if the user installs the packages (CP2).
- **Template definitions** are in the dump uninstantiated, with instances below them; the converter prints the
  definition and attaches the instances as facts. **Implicit declarations** (`isImplicit`: the compiler-generated
  special members, deduction guides, lambda closure types) are dropped from the forms.
- **Ranges** give each node's begin and end byte offsets and the macro-expansion pair, so the converter can place
  the node among the shared lexer's tokens (the positions are source offsets, as C3's token check uses) and do C3b's
  macro-invocation recovery from the spelling/expansion locations clang gives (better than cc's: clang records both).
- **Mode `:full`** is C3b's: each top-level item's text, from its first token to the next item's, plus the layer's
  directives (CS70 (a), CS73). Sub-item layers (per token) are what a finer `:full` would add, not v1.
- **What clang's AST loses and the tokens give:** comments (in the text of an item, kept by `:full`), the alternative
  spellings, and whether a brace was written (`if (c) s;` versus `if (c) { s; }`: C3b's `^:c/bare`; clang keeps a
  `CompoundStmt` only where written, so it is in the AST here and not lost).
- **Errors.** A file the oracle rejects gives its diagnostics as the oracle's words (no Arbace diagnostics for Sema).
  Syntax errors cannot be told from semantic ones without a parser of Arbace's; error tests are judged by *rejection*
  and by the first line's file position, against g++'s (CP8's classes).

### 4.3 Print

The new large piece. `:lines` prints forms as canonical C++ (cemit's rules extended: braces on every body, one
declarator a declaration; C++'s grammar needs the printer to know declarator-versus-expression positions, template
argument lists and `>>`, `typename`/`template` disambiguators, lambda syntax, `requires` clauses, trailing return
types, `noexcept(...)`, ref-qualifiers, `operator` names, attributes, `decltype(auto)`, structured bindings,
`co_await`/`co_yield` precedence, `if constexpr`, `if consteval`, range-for with an init-statement, designated
initialisers, `explicit(bool)`, `constinit`, `export`/`module`/`import`). `:full` is the stored text. Estimate: the
printer is 2 to 3 times C's (732 lines of `print.ae` in C3b) in Arbace code.

The print is judged at the levels of C3b, adapted: (1) forms → C++ → forms equal; (2) the **AST of the print**
(`tools/cxxdump` on it) equal to the original's modulo positions; (3) the **object code** of the print (clang `-O0 -c` and
g++ `-O0 -c`) equal to the original's, which P7 suggests is a faithful judge; (4) the run equal.

### 4.4 The native reader (a proved replacement, later; CP3 (b))

A C++ parser that is not a Sema is possible if it tracks the *kind* of every name (type, template, namespace,
value), as cc does for typedefs: the grammar's ambiguities (declaration versus expression statement, `<` as template
bracket, `T(x)`, `a * b;`, `typename`/`template` in dependent contexts, `>>`, `decltype`, `auto`) are decided by name
kinds, which come from the declarations in view (headers included). Where it fails is where the kind depends on
instantiation: `S<int>::x` is a type or a value depending on the specialization selected, `using` through a
dependent base, a name that exists only in a template instance. gcc's parser (`gcc/cp/parser.cc`, tens of
thousands of lines) and clang's Parse both call back into their Sema for those. A reader of Arbace's own would be
checked against the oracle's AST as JN8 and GP5 were, **with a known class for the cases it declines**, and would
cost 25 to 45 working days with libstdc++'s headers as the stress test. It is listed as optional (CX11), not
in v1.

## 5. eval

### 5.1 The candidates for what is interpreted

| route | input | what Arbace implements | size of the input | verdict |
|---|---|---|---|---|
| (a) **LLVM IR → C forms → ci** | clang's `-O0` IR | an IR reader, a lowering (instructions, phi, GEP, intrinsics, `invoke`/`landingpad`), the Itanium runtime (`__cxa_*`, RTTI) | 40 KB to 1 MB per program (P4) | **recommended**: all of C++'s semantics already done by clang |
| (b) an interpreter of LLVM IR | the same | the same, as an interpreter (`lli`) instead of a lowering | same | cheaper to start, but a second interpreter beside ci; no AOT sharing |
| (c) AST after Sema → C forms | the filtered or full JSON | clang's **CodeGen** on JSON: constructors/destructors order, temporaries' lifetimes, vtables, guard variables, RTTI, `invoke`, implicit special members' synthesised bodies, instantiation order, coroutine frames | 127 MB to 608 MB per program (P3) | CodeGen is a large part of clang; and the data is unusable |
| (d) an AST interpreter | the same | the same, plus a value model | same | the same |
| (e) Arbace's own C++ Sema | source forms | everything (§Summary) | n/a | out by scale |

### 5.2 LLVM forms (`arbace.repr.llvm`)

LLVM IR text (a documented grammar, LangRef) as forms: modules, `define`/`declare`, types (integers of any width,
pointers (opaque in recent LLVM), structs, arrays, vectors), basic blocks, the instruction set at `-O0` (about 40
kinds: arithmetic, memory, `getelementptr`, casts, `icmp`/`fcmp`, `phi`, `select`, `br`, `switch`, `call`/`invoke`,
`landingpad`/`resume`, aggregate ops, `atomicrmw`/`cmpxchg`/`fence`), attributes, metadata (mostly ignorable),
the data layout string. Judged by clang as the assembler (P7): forms → text → `clang++ -c x.ll` → the object equal
to the original's. A grammar of this size is a Sonnet task; the reader on Arbace's tools host, evaluated from the
checkout like repr/haskell's.

### 5.3 The lowering (`arbace.cpp.lower`: LLVM forms → C forms)

One function per `define`, locals for SSA values, `phi` as assignments at the predecessors' edges, blocks as
labels with `goto` (C forms have them, C23's §7.3), `alloca` as locals whose address is taken (CS9's flat address
space), `getelementptr` as address arithmetic with the type sizes the IR's data layout gives, intrinsics
(`llvm.memcpy`, `memset`, `lifetime.*` ignored, `trap`, `va_*`, overflow arithmetic, `ctlz`...) in a table,
`x86_fp80` per CS35 (long double as double: a recorded class). The output is **C forms** that ci evaluates and C6
translates, so *there is one back end for C and C++*: the checker (C4) needs to accept what the lowering emits (its
own subset: no unions of weird types, but `goto`, casts, address-of locals, function pointers), and the lowering is
written to produce C that gcc would accept too, so that gcc `-O0` compiles the lowered text as a second judge.

### 5.4 Exceptions (CP7)

The Itanium ABI throws by `__cxa_throw` and unwinds through frames by `_Unwind_RaiseException` using DWARF
tables. There are none here. Instead: **a pending-exception flag**. `__cxa_throw` stores the exception object and
sets the flag and the lowered `call` sites that can unwind (every `call`/`invoke` to a function not marked `nounwind`
and every indirect call) test it: an `invoke` jumps to its landing-pad block, where the `landingpad` instruction's
clauses (`catch @typeinfo`, `cleanup`, `filter`) are matched in C against the exception's type info by the runtime's
`__cxa_*` routines (`__cxa_begin_catch`, `__cxa_end_catch`, `__cxa_rethrow`, `__gxx_personality_v0`'s matching
reimplemented over the clauses, not the LSDA); a `call` outside any landing pad returns to its caller at once with the
flag still set. The same lowered C is what eval runs and what C6 translates, so the semantics are written once.
Costs: a test per call (acceptable), `std::terminate` on an uncaught exception and on `noexcept` violation, and
nothing like asynchronous unwinding (signal-handler throws are out).

### 5.5 The runtime

`libsupc++` (the language runtime: `new`/`delete`, `__cxa_*`, `__dynamic_cast`, `type_info` comparison, guard
variables for function-local statics, `std::terminate`, the allocator) and **libstdc++-v3/src** (194 files in the
tarball: strings, ios, locale, `ostream`, `iostream` objects, `to_chars`, `<charconv>`, random) compiled to IR by the oracle
from the pinned tarball and lowered like a program; C++'s libc is musl as C forms (CS11, C5), shared. Threads
(`std::thread`, `std::mutex` on pthreads) are out of v1 (CP1): the host process is single-threaded in ci, and the
atomics lower as plain operations.

### 5.6 What is hard, honestly

- The **lowering** must be right for a long tail of IR (bit-fields, packed structs, `byval`/`sret`/`inreg` attributes
  at call boundaries with C forms' by-value structs, `i128`, vectors, `switch` on wide integers, `unreachable`, `musttail`).
- **Start-up cost**: a program that includes `<iostream>` initialises `std::ios_base::Init`, the locale facets and
  `cout`/`cin`/`cerr`: hundreds of thousands of instructions run in ci before `main`. The first measurement of
  C5's speed (`ci` on musl's startup) predicts it. (AOT does not pay it at run time.)
- **`long double`** (`x86_fp80`) appears in libstdc++'s numeric code and `std::numeric_limits`; CS35 decided no
  software x87, so values in `long double` differ from g++'s and layouts holding one differ (16 versus 8 bytes). The
  IR's struct types carry the x87 size, so the lowering must keep the IR's layout and compute in double: the
  class `long-double` of C23 covers it.
- **Differences between g++ and clang** that are legal: the order of evaluation of function arguments (g++ right to
  left in many cases, clang left to right), unspecified layouts of non-standard-layout classes' padding contents,
  the exact text of `typeid(T).name()` (mangled in both, same Itanium), `std::string`'s and `std::vector`'s growth
  are libstdc++'s in both. The judge is g++ (CP11); the order-of-evaluation class is recorded, tests that depend
  on it are rare.
- **Modules, header units, multi-TU programs** (`dg-additional-sources`): the oracle produces IR per TU, the
  lowering links by symbol; v1 runs a program of **one TU** and the corpora's multi-file tests are a later class.

## 6. AOT

C++ AOT is C23's AOT (C6, C forms → Go forms printed by repr/go and built by `arbace.build.go`) applied to the
lowering's C forms: the pipeline is oracle (clang IR) → LLVM forms → C forms → Go forms → Go's compiler. The new
work is the lowering (§5.3), the runtime in C forms (§5.5) and the shards: a program that links libstdc++'s
runtime is a large translation (libstdc++.so's code is about 2 MB), cached by key (the runtime translated once, as the
Java build's compiled namespaces are). Estimated at +0 days beyond the lowering if C6 is complete; the checks (CX8)
cost the days. A different AOT target, **Java forms** (j2g), is possible by the same lowering to Java forms, but C is the
natural IR for C++ and gives one back end (CP4 note).

## 7. The standard library

| option | what | license | on the host | for |
|---|---|---|---|---|
| **libstdc++ 15.2.0** | headers (14 MB in `/usr/include/c++/15.2.0`, 905 files, 13.5 MB in the tarball's `libstdc++-v3/include`) and `src/` (194 files) | GPL-3.0-or-later with the GCC Runtime Library Exception 3.1 (`COPYING.RUNTIME` in the tarball) | yes (both compilers use it) | the recommended choice: g++ and clang on this host already compile against it |
| libc++ 23 | headers and `src/` | Apache-2.0 with LLVM Exceptions | no (an apk) | clang's native library, reference for `libcxx/test` |
| a hand-written mini-library | a subset in forms | Arbace's | n/a | would hide the corpus's real usage |

**As C++ forms or compiled:** both, for different jobs. The headers are the largest **read corpus** of all (the
template-heaviest real code: ranges, format, variant, chrono; 905 files) and their forms are read and printed by
repr/cpp, **not vendored** (read in place from the pinned tarball, as jdk26u's tests are). What eval and AOT run is
**the IR of the oracle's compilation** of libstdc++'s sources, an artifact built from the pinned tarball by the pinned
clang and pinned by digest in the record, not vendored source. Vendoring the runtime as forms (as the Java crop did
with jdk26u) would need the forms of C++ to be *compiled* by Arbace, i.e. the Sema of §Summary; that is the day Arbace
has a C++ checker (CP5 (b)). The licenses: reading is free; shipping the lowered runtime in a binary carries libstdc++
under the Runtime Library Exception (for combinations that include it, no GPL propagates). LICENSE.md gains the
entry when the first artifact ships.

## 8. The corpora

On the host (read in place, nothing vendored, counts made from the pinned tarball or the checkouts):

| corpus | where | files | action | language | license (as data) |
|---|---|---:|---|---|---|
| gcc 15.2.0 `gcc/testsuite/g++.dg` | the pinned tarball | 18,066 `.C`/`.cc` of 18,894, 26 MB | read, print; the `dg-do run` ones (1,811) run | C++98 to C++26 | GPL-3.0-or-later |
| the same, by directive | | `dg-do compile` 10,899; `dg-do link` 163; `dg-do preprocess` 71; files with `dg-error` 4,762; with a C++20-or-later `-std` in options 136 | compile (read, acceptance); error tests (rejection) | | |
| the same, by directory | | `cpp0x` 3,052; `cpp1y` 721; `cpp1z` 582; `cpp2a` 1,281; `cpp23` 243; `cpp26` 73; `modules` 1,655; `coroutines` 334; `concepts` 143; `template` 1,569; `eh` 155; `lookup` 344; `overload` 99; `parse` 614; `warn` 952; `ext` 1,052 | | | |
| gcc `g++.old-deja` | the tarball | 3,258 files | read; the old runs | C++98 | GPL-3.0-or-later |
| gcc `c-c++-common` | the tarball | 4,086 files | shared with C (C23's CS33 groups: to check which are in) | C and C++ | GPL-3.0-or-later |
| **libstdc++ testsuite** | the tarball `libstdc++-v3/testsuite` | 9,725 `.cc` of 13,012 files, 36 MB; 6,027 of the `.cc` carry no compile/link/preprocess/error directive (run tests: `VERIFY`, `testsuite_hooks.h`); `20_util`, `23_containers`, `25_algorithms`, `21_strings`, `27_io`, `28_regex`, `29_atomics`, `30_threads` ... | run (the run tests), read | C++98 to C++26 | GPL-3.0-or-later with the Runtime Library Exception for the library, GPL for the tests |
| libstdc++'s headers | the tarball `libstdc++-v3/include`; `/usr/include/c++/15.2.0` | 905 files, 13.5 MB | read, print (the heaviest template corpus) | C++23 | as above |
| **whim++** | `/root/go-whim/wpp/src` | 10 files, 75,292 lines | read, print, compile; run its editor suite (`whim test --cpp`: 80 and 240 cases) | C++23 (C-with-classes) | go-whim's, EPL-1.0 by CS37 (the generated `editor.cpp` derives from vim: its license too) |
| **HotSpot** | `/root/jdk26u/src/hotspot` | 3,810 `.cpp`/`.hpp`, 1,409,583 lines | read, print (the real-world C++14/17 corpus; no STL, no exceptions, no RTTI) | C++17 | GPL-2.0 with Classpath Exception |
| GCC's own front end | the tarball `gcc/cp` | 58 `.cc`/`.h` files, 288,409 lines | read corpus (GCC is C++14-ish; needs generated headers) | C++14 | GPL-3.0-or-later |

Not on the host (to be fetched under CP8; sizes and licenses from memory, **to verify** at the fetch):

- **clang's tests** in `llvm-project` at the tag: `clang/test/SemaCXX` (on the order of 2,000 files, `-verify` with
  `expected-error` comments), `clang/test/CXX` (organised by the standard's clauses), `clang/test/Parser`,
  `Lexer`, `Preprocessor`, `Modules`, `CodeGenCXX` (CHECK lines on IR: judge for the oracle's output, not Arbace's),
  `clang/test/std`; Apache-2.0 with LLVM Exceptions. The source of the standard's clauses' coverage that the gcc tests lack,
  and written to *diagnostics*, which Arbace cannot match (the oracle's).
- **libc++'s tests**, `libcxx/test/std` (over ten thousand files, `.pass.cpp`, `.fail.cpp`, `.verify.cpp`); Apache-2.0
  with LLVM Exceptions. They need libc++, which the host lacks: out of v1 unless CP6 (b).
- **llvm-test-suite** (`SingleSource` incl. `Regression/C++`, `Benchmarks`, `UnitTests`; `MultiSource`): LLVM's license
  for the harness and the programs under their own (the suite is a mix; to verify), the best source of C++ **programs with
  expected output**; and the `externals` are not included (SPEC).
- **Random C++**: YARPGen (Apache-2.0) generates C and C++; Csmith (BSD-2) has a C++ mode (to verify).
- **The standard's examples** (the draft's sources, `cplusplus/draft`, CC-BY-SA/BSD-style, to verify): a clause-by-clause check no
  test suite orders.

**Judging per class** (the crops' way: every non-pass in a recorded class): `dg-do run` tests on status and stdout with
`abort()`/`VERIFY` meaning failure; `dg-do compile` on acceptance (g++) and the read's object code; `dg-error` tests on
rejection and position only; `dg-warning`, `scan-assembler`, `scan-tree-dump` and `dg-prune-output` are not judged
(classes `diagnostics-text`, `scan-*`), as are tests that need `-fmodules-ts` multi-file steps (`modules`, class
`modules`) until CP13.

## 9. How it fits C23's pieces

| C23 piece | status (branches `c23-c3b`, `c23-c4`) | C++'s use |
|---|---|---|
| lexer and preprocessor (C2) | done, 20 corpora token-equal to cc's or known | **shared**, extended in CX2: C++ lexical items (raw strings, UDLs, digraphs and alternative tokens, `<=>`, `->*`, `::`, `char8_t`, `import`/`module`/`export` as contextual preprocessing tokens, `#include_next`, `__has_cpp_attribute`, `__cplusplus`), the host data for `-std=c++23` (P9) |
| parser (C3) | done (cc's trees, 6,300 of 6,387 equal, 26 known) | **not used**: cc is a C parser. The C subset of a C++ file is read from the oracle's AST like the rest; C's parser stays the reader of C. A native C++ parser (§4.4) would reuse its infrastructure (tokens, scopes, the `*deadline*` budget) |
| converter and printer (C3b) | done: `:lines` by cemit's rules, `:full` at item granularity, 49+13 c23 files | **shared in part**: the `.ae` writer (`layout.ae`), the layer, header units, the macro-invocation recovery, expressions/statements/declarators of the C subset; the C++ printer extends it (§4.3) |
| C forms (doc/c/SPEC.md) | spec | C++ forms **extend** them (CP10); `extern "C"` is C forms |
| checker `arbace.c.check` (C4) | branch `c23-c4` | needed by the lowering's output (C forms emitted by `arbace.cpp.lower` pass through it); **not extended to C++** in v1. An extension for the C-with-classes subset (wpp's: member functions, references, named casts, `enum class`, `constexpr`, no templates beyond a handful) is the option that would give a C++ REPL with no oracle (CP5 (b)), +15 to 25 days |
| ci (C5) | to be built (8-14) | **runs the lowering's C forms** (eval) and C++'s runtime in C forms |
| AOT (C6) | to be built (6-10) | **translates the lowering's C forms** |
| musl as C forms (CS11) | planned C5 | C++'s libc |
| `crt` (CS9: flat 64-bit address space, `__int128`, `_BitInt` limbs) | planned C5 | C++'s memory model, the same; `long double` as `double` (CS35) |
| gcc as reference (CS14: runs recorded as data) | decided | g++ is the judge, the same record format (`test/cpp/ref-gxx15.edn`) |
| `bin/reap` cells, `test/c/corpora.edn` format | decided | the cells and the corpora format reused |

So C++ shares C23 *at both ends*: the preprocessor at the front, ci and AOT at the back; what is new is the
oracle-fed reader, the C++ printer, LLVM forms and the lowering. **Where C++ is not C**, and the lowering stays C's:
the lowered code is C that has already had C++'s semantics compiled into it by clang, so the differences that
go-whim's `doc/CPP.md` lists (jumps past initialisation, narrowing in braces, implicit `void *` conversion,
assignment order, uninitialised locals) are the oracle's concern at read and none of the lowering's.

## 10. The row

`test/reap/cells.edn` gains `:cpp` with four cells; classes files
`test/cpp/divergences{,-read,-aot,-print}.edn` (goi's format); `bin/reap` judging 4 more cells (the total follows the
crops that come first: 16 with C23 is 16 of 16, 20 with one more, 24 with two, 28 with three). Makefile: `repr-cpp`,
`cppi-check`. Parked bridges (repr/haskell, repr/rust) stay parked. C++ is a crop (the rows' rule) with a REPL
(cppi), so it lands as one.

## 11. Plan (CX1-CX10)

Estimates in working days as the repository's surveys give them. Each milestone ends with its check green and its
records. C23's C1-C9 are prerequisites for CX6-CX8 and are **not** counted.

| milestone | content | exit criteria | days |
|---|---|---|---:|
| **CX1** the spec and pins | `doc/cpp/SPEC.md` (C++ forms for §1, §3: heads, the facts, worked examples from wpp and P1); `test/cpp/corpora.edn` (pins: gcc 15.2.0 tarball, llvm-project tag, clang's Alpine APKBUILD); the apk question (CP2); P5 (layouts) and P8 (a suspending coroutine) settled; the host data for `-std=c++23` | the spec accepted by the user; every corpus has a manifest | 2-3 |
| **CX2** the shared lexer and preprocessor | `arbace.repr.c.lex` and `.pp` extended: C++ tokens, alternatives, raw strings, UDLs, `import` and `module`, `#include_next`, `__has_cpp_attribute`, the C++ host data (`test/cpp/host/`: predefined macros, search list, header digests); `pp-check` against `g++ -E` digests and clang's token dump | every corpus file's stream agrees with g++'s recorded digest, or both reject it, or it is known (C3's rule, CS51), libstdc++'s 905 headers and the whole g++.dg included | 4-6 |
| **CX3** the oracle's helper | `tools/cxxdump`: streaming filter of `-ast-dump=json` to the main file's tree and facts (Go, TamaGo's, no cgo); layouts; speed and memory recorded; the cached dumps by key | the filter's output for the corpus samples; 608 MB handled in a bounded memory; the decision on libTooling recorded with its numbers (CP2) | 4-6 |
| **CX4** repr/cpp `:lines` | `arbace.repr.cpp.convert`, `.print` (the C++ printer), `bin/repr-cpp read|print|roundtrip|check` | whim++ and the samples read; forms → text → forms equal; **the AST of the print equal to the original's** on the g++.dg compile corpus (known classes for the oracle's rejects); the object code of the print equal | 10-16 |
| **CX5** `:full` and the wide corpora | item-granular text (C3b's), directives layer, header units; widened to libstdc++'s headers, HotSpot (read only), the libstdc++ testsuite, the g++.dg directories of §8 | `:full` byte for byte over every corpus file the oracle accepts; every exception classed | 4-7 |
| **CX6** LLVM forms and the lowering | `arbace.repr.llvm` (read, print, the `clang -c x.ll` object judge); `arbace.cpp.lower` (instructions, phi, GEP, intrinsics, `invoke`/`landingpad` per §5.4, `x86_fp80`); `bin/cppi lower` | the IR of the samples and of the libstdc++ testsuite's subset lowered to C forms that **gcc `-O0` compiles and runs** as the original (a second judge before ci exists) | 12-20 |
| **CX7** the runtime | libsupc++ and libstdc++-v3/src compiled to IR by the oracle (pinned, by digest) and lowered; `__cxa_*`, RTTI, guards, `new`/`delete`, the locale in "C", the iostream objects; against ci and musl (C5) | `hello world` with `<iostream>`, `std::vector`, `std::string`, `std::map`, `std::sort`, exceptions and RTTI run on ci with g++'s output | 8-14 |
| **CX8** cppi and AOT checks | `bin/cppi run|check|divergences|check --aot`; the corpora's run programs; refs recorded (`test/cpp/ref-gxx15.edn`); classes | the check over the run corpora: none failing, every non-pass in a recorded class; the same compiled by C6 | 6-10 |
| **CX9** error tests and the rest | the `dg-error` corpus judged on rejection and position; `dg-do preprocess`; modules read/print (single TU); the classes | each file in a result or a class | 3-5 |
| **CX10** the row and the milestone | `cells.edn` `:cpp`, `bin/reap` all checked, the essential subsets in the gate (read, print, a lowered program), Makefile, `bin/gate --full` serial, the re-seed, tag | `bin/reap`: all cells checked, none failing; the tag | 2-4 |
| *CX11* (optional) native parser | §4.4: name-kind tracking, a port of the grammar; `parse-check` against the oracle's AST, mutants | trees equal over the corpora or known, with the instantiation-dependent cases classed | 25-45 |
| *CX12* (optional) the subset's checker | C4 extended for the C-with-classes subset (wpp's), no oracle at the REPL | wpp's editor runs on cppi with no clang at run time | 15-25 |

**Total without the optional ones: about 55 to 91 working days** (CX1 2-3, CX2 4-6, CX3 4-6, CX4 10-16, CX5 4-7, CX6
12-20, CX7 8-14, CX8 6-10, CX9 3-5, CX10 2-4). Read and print alone (CX1-CX5): **24 to 38**, startable now; eval and
AOT (CX6-CX8): 26 to 44, after C5/C6. With both optional pieces about 95 to 161. **For scale:** C23 is 33 to 56, OCaml 37 to
63, Scheme 44 to 75. The surveys have run under their estimates (JN8 at a tenth, C3b at a quarter of its days), but
the long poles here are the lowering and the runtime, whose size is a guess, and an honest re-estimate is due
at CX3 and again at CX6.

**Sonnet versus Opus** (the user's model split): **Sonnet:** CX1's spec drafting from clang's node kinds and the
standard's grammar, CX2's lexical additions (mechanical, judged by the digest), CX3's streaming filter, CX4's
converter (a table from clang's node kinds to forms, once the heads are fixed) and the printer's mechanical
productions, CX5's widening, CX6's LLVM forms reader/printer and the intrinsics table, CX7's `__cxa_*` in C forms
once the model is fixed, CX8's classing, CX9, CX10 and the records. **Opus:** the forms' design for templates and
classes (CX1), the printer's declarator and template-argument rules where ambiguity lives (CX4), the lowering's design
(ABI attributes, phi/GEP, exceptions as a pending flag, `long double`), the Itanium runtime's matching and
unwinding semantics (CX6-CX7), the divergence classes' judgment and the oracle disagreements (g++ against clang).

**Re-seed needs.** CX1-CX5 and CX9 change only tools' namespaces (evaluated from the checkout) and a Go helper: no
re-seed. CX6 adds LLVM forms and the lowering, evaluated on the tools' host; CX7-CX8 need `arbace.repr.llvm` and the
lowering embedded and compiled (c2j) where `bin/cppi` runs at the REPL, and the runtime as C forms listed with C5's
(`crt`, musl): **one re-seed at CX10 at most**, batched with C23's (C5, C6) as C3 batched with C5.

## 12. Proposals (CP1-CP14)

Each with options, the recommended one first.

- **CP1 The standard and the subset for v1.**
  - Options: (a) C++23 as g++ 15.2 and clang 23.1.4 accept it (`-std=c++23`; each corpus file keeps its own `:std`,
    C23's CS3). **Read and print: the whole language** but named modules' compiled interfaces. **Eval and AOT:
    single-translation-unit hosted programs** over libstdc++, with no threads, no filesystem, the "C" locale, no
    asynchronous signals; (b) C++20; (c) the C-with-classes subset of whim++ only (no templates beyond a few, no
    library beyond `<memory>`, `<utility>`, `<string_view>`...) with its own checker (CX12) and no oracle at run time;
    (d) all of C++23 including modules, threads and the filesystem.
  - *Recommended: (a).* (c) is the smallest thing that is a C++ REPL standing alone, and CX12 is its option; (d)
    adds threads (ci is single-threaded) and module BMIs that the reference builds with multi-step commands.
- **CP2 The oracle: which compiler, which interface.**
  - Options: (a) **clang's `-ast-dump=json` filtered by a Go helper now** (`tools/cxxdump`), clang's IR for eval, g++
    the judge; the libTooling dumper later if the numbers (P3: 608 MB, 8.5 s) call for it, with `apk add clang23-dev
    llvm23-dev` then; (b) install the dev packages now and write the dumper in C++ on libTooling (real tokens,
    layouts, no JSON); (c) g++ only: a GCC plugin (the plugin headers are installed) dumping GENERIC, `-fdump-tree-original-raw`
    as text; (d) a native reader at once (CP3 (b)).
  - *Recommended: (a).* The JSON works today with no apk and the filter's cost can be measured in CX3 before the
    user installs hundreds of MB of dev packages; (c) is the second opinion on the structure if clang and g++ disagree
    and no replacement: GENERIC is a lowered tree.
- **CP3 The reader: oracle-fed or a native port.**
  - Options: (a) oracle-fed (§4.2); a native reader (§4.4) as a later, optional CX11 with a proved equality to the
    oracle; (b) a native parser first (name-kind tracking), the oracle only to judge it; (c) a port of clang's Parser
    and Sema (hundreds of thousands of lines): no.
  - *Recommended: (a).* This is the pattern of repr/haskell and repr/rust; (b)'s ambiguity cases need instantiation
    (`S<int>::x`) that no parser without Sema decides; GP5 and JN8 came *after* the helper.
- **CP4 What eval is fed.**
  - Options: (a) **clang's LLVM IR, as LLVM forms, lowered to C forms**, run by ci and translated by C6 (§5); (b) the
    same IR interpreted by an LLVM-IR interpreter of Arbace's (no lowering, no AOT sharing); (c) the AST after Sema,
    lowered to C forms by Arbace's reimplementation of CodeGen; (d) an AST interpreter; (e) Arbace's own Sema.
  - *Recommended: (a).* The IR is 4,000 times smaller than the AST (P3, P4), has instantiation, destructors, layouts,
    vtables and exceptions done; one back end serves C and C++; (b) is the fallback if the lowering's C does not fit
    ci's checker. The consequence for the rule of the rows: **eval and AOT of C++ are the oracle's compilation run on
    Arbace**; the REPL cannot define a C++ form without clang (or CX12's subset). Stated here so the user decides it
    knowingly.
- **CP5 Templates: instantiated by the oracle or by Arbace.**
  - Options: (a) by the oracle, always (clang's instances are in the IR and listed as facts); (b) by Arbace for the
    subset (CX12: a checker with simple function/class templates and overloading, no SFINAE, concepts, partial
    ordering) and the oracle for the rest; (c) by Arbace entirely (= CP4 (e)).
  - *Recommended: (a)* for v1; (b) only if the user wants a no-clang REPL for the wpp subset (+15 to 25 days, CX12). (c) is
    the largest single piece of work in this survey (overload resolution + deduction + instantiation + SFINAE +
    concepts + constexpr evaluation) and is not planned.
- **CP6 The standard library.**
  - Options: (a) **libstdc++ 15.2.0** (the host's, g++'s, clang's here; headers read as forms, the runtime built to IR
    by the oracle from the pinned tarball); (b) libc++ (clang's native; needs an apk, its `libcxx/test` tree is the best
    library test suite there is); (c) a minimal library by hand.
  - *Recommended: (a).* Both compilers on the host use it, the judge's runs use it, whim++ is built against it; (b)
    later as a second library if the user wants clang's tests to apply.
- **CP7 Exceptions.**
  - Options: (a) **a pending-exception flag with checks after calls that can unwind**, and the landing pads matched
    over the IR's clauses by the Itanium runtime reimplemented in C forms (§5.4); (b) `setjmp`/`longjmp` chains
    (cleanups need registration per frame, destructors during unwinding); (c) host panics in AOT, a separate
    mechanism in eval.
  - *Recommended: (a)*: written once, the same in ci and in AOT, no DWARF tables. (b) is cheaper to write but breaks on
    cleanups (the IR has them as landing pads, not as registrations).
- **CP8 The corpora and the fetch.**
  - Options: (a) the pinned gcc tarball's `g++.dg` and libstdc++ testsuite, whim++, HotSpot (read), libstdc++'s headers: all on the
    host, no fetch; (b) (a) plus the `llvm-project` tag's clang tests and llvm-test-suite (fetch, sizes to verify, Apache-2.0
    with LLVM Exceptions and the suite's mixed licenses); (c) whim++ only.
  - *Recommended: (a) for CX1-CX8, (b) at CX9* (error tests and the standard's clause coverage, which clang's `test/CXX`
    organises), the user's fetch decision.
- **CP9 The order against C23, OCaml and Scheme.**
  - Options: (a) **C23 first (it is the prerequisite); C++'s read and print (CX1-CX5, 24 to 38 days) may run beside C23's
    C4-C6 since they need only C2-C3b; OCaml and Scheme in the user's order; C++'s eval and AOT (CX6-CX10) after C6**;
    (b) C++ after OCaml and Scheme entirely; (c) C++ before OCaml and Scheme.
  - *Recommended: (a).* The read level is cheap relative to its payload (whim++, HotSpot, libstdc++'s headers become
    readable and printable) and independent of OCaml and Scheme; the eval half needs C23. OCaml-versus-Scheme stays the
    user's; C++ does not depend on either. The tag follows the number of crops finished (`arbace-reaps-N-crops-v1`).
- **CP10 The forms' vocabulary.**
  - Options: (a) **C++ forms extend C forms**: shared heads for what is C, `cpp/*` heads for what is not; (b) a
    separate `cpp/*` vocabulary throughout; (c) forms that keep clang's node kinds as heads (a near-identity converter,
    like OC8's idea for OCaml).
  - *Recommended: (a).* One printer core, one lowering target, `extern "C"` natural; (c) is quick but makes the forms
    clang-versioned.
- **CP11 The reference for the runs.**
  - Options: (a) **g++ 15.2 at `-O0` the judge** (as C23's CS14), the runs recorded as data; clang's own run a witness
    for the class `gxx-clang` (legal differences: argument evaluation order, UB, `long double`); (b) clang the judge,
    as it is the IR's source.
  - *Recommended: (a).* The corpora are gcc's (`dg-do run`) and written for g++'s ABI; the IR route differs from g++ only
    in the classes of §5.6.
- **CP12 `long double` and the rest of C23's arithmetic decisions.**
  - Options: (a) inherit CS34/CS35: `long double` is `double` in eval/AOT, a recorded class `long-double`; `__int128` and
    `_BitInt` as limbs; round to nearest; (b) software x87 (rejected for C).
  - *Recommended: (a).*
- **CP13 Modules and coroutines.**
  - Options: (a) coroutines through the oracle's lowering (already split in the IR, P8) and in eval/AOT; **modules read
    and printed as single-TU forms (`export module`, `import`, `module :private`) and out of eval** until a build model for
    BMIs exists; (b) modules in eval via a multi-TU build of the oracle's IR; (c) both out.
  - *Recommended: (a).* `g++.dg/modules` (1,655 files) is the largest class in this crop and needs a separate
    multi-step test model (`dg-module-cmi`); a known class until then.
- **CP14 Names and the tag.**
  - Options: (a) `arbace.repr.cpp` (`arbace/repr/cpp{.ae,/}`), `bin/repr-cpp`, `arbace.repr.llvm`, `arbace.cpp.lower`,
    the evaluator `cppi` (`bin/cppi`), tools `tools/cxxdump`, `test/cpp/`, `doc/cpp/`, milestones CX1-CX10, the tag
    `arbace-reaps-N-crops-v1` with N the number of crops then complete; (b) `cxx` instead of `cpp` (the files'
    extension and the compilers' `c++`/`g++`); (c) `cc` (taken by go-whim's C front end).
  - *Recommended: (a).*

## 13. What the user decides first

1. **CP1 and CP4**: the scope of v1, and whether Arbace's C++ eval is "the oracle compiles, Arbace runs" (the
   recommended and cheap route) or aims at a standalone REPL (CP5 (b), CX12, and in the limit CP4 (e)).
2. **CP2**: whether to install `clang23-dev` and `llvm23-dev` (hundreds of MB) for a libTooling dumper, or start with the JSON
   filter (recommended) and decide on CX3's numbers.
3. **CP9**: the order: C++'s read half can start beside C4-C6; the eval half cannot start before C6.
4. **CP8**: fetch clang's tests and llvm-test-suite at CX9 or not.
5. **CP13**: modules out of eval.

## 14. Risks

- **Size and speed of the oracle's output.** P3: 608 MB of JSON for a four-line program; 8.5 s of clang per file; a
  corpus of 18,066 files is hours and hundreds of GB of transient output. Mitigation: the streaming filter, dumps cached by key
  (source digest, flags, clang version), the headers' token streams cached by digest, shards, and CX3's measurement
  before CX4 starts. The libTooling dumper (CP2 (b)) is the exit.
- **Pinned to a moving compiler.** The IR and the AST are clang 23.1.4's (Alpine edge moves; clang 16, 22 and 23 are
  all installed now). The IR is committed only for the essential subset (kilobytes, compressed) and the rest is
  regenerated; the record pins Alpine's APKBUILD sha and the llvm-project tag; a clang upgrade is a re-record.
- **The oracle disagrees with the judge.** clang accepts what g++ rejects and conversely, and the test corpora are
  gcc's: expect hundreds of g++.dg files in classes `clang-rejects`, `gxx-rejects`, and different evaluation orders (§5.6).
  The classes are the crops' manner; their size is unknown until CX4.
- **Arbace's preprocessor against the oracle's.** The AST has the oracle's expansion of the macros; the shared
  preprocessor must agree on every file or the converter's macro recovery misplaces nodes. C3's rule (digest equality
  with the reference's `-E`, known entries) controls it; libstdc++'s headers are the stress (C23's did not meet
  `#include_next`, `__has_include`, `#pragma GCC`, `_Pragma` at that scale).
- **The reader's value is the oracle's.** If the user's target is a self-sustaining system (CLAUDE.md, "Long term"),
  a C++ whose Sema is clang's is a dependency on a tool the standalone direction cannot keep; the references are
  recorded (CS14) but the *front end of every run* needs the oracle or its recorded IR. Stated (§1, CP4); CX12 and
  CP5 (b) are the way out for a subset, a native Sema for the whole language is not.
- **The lowering's long tail** (§5.6): ABI attributes, aggregates by value, vectors, wide integers, bit-fields,
  `musttail`, atomics; and the start-up cost of iostream in ci. Estimated 12 to 20 days for the lowering with a wide
  range for exactly this reason.
- **`long double`** (CS35): libstdc++ uses it in `<limits>`, `<cmath>`, number formatting; the class is wide.
- **libstdc++'s runtime on Arbace's libc** (musl as C forms): locale, `std::ios_base::Init`, `__cxa_atexit`,
  `thread_local`, `std::random_device`, `std::chrono::system_clock`, `std::filesystem`: each a syscall layer piece;
  v1 excludes threads, filesystem and the non-C locales; the libstdc++ testsuite's share that does (`27_io`, `30_threads`,
  `22_locale`, `28_regex`'s locale use) is a class.
- **The printer's size.** C++'s concrete syntax is the largest of the crops'; `:lines` must produce text the oracle
  re-reads to the same AST for a large corpus; the printer's days (10-16 with the converter) are the widest estimate of the read half.
- **gcc-specific tests.** The g++.dg suite's directives (`dg-error` with exact text, `scan-assembler`, `dg-lto-*`,
  `dg-additional-sources`, `dg-module-*`) are gcc's tooling; the share Arbace can judge is at most the 1,811 run and 10,899
  compile files (60 % of the directory); the rest are classes by design.
- **Estimates.** The surveys run under their estimates, but CX6 and CX7 are the least known (no similar piece exists
  in the repository); re-estimate at CX3 and CX6. Total 55 to 91 days is wider than C23's 33 to 56 for that reason and
  is on top of C23's.
- **The licenses.** LLVM and libc++ are Apache-2.0 with LLVM Exceptions (compatible with EPL-1.0 for the use planned:
  reading, not vendoring); libstdc++ is GPL-3.0-or-later with the Runtime Library Exception; the g++.dg and libstdc++
  tests are GPL-3.0-or-later; HotSpot is GPL-2.0 with the Classpath Exception. The lowered runtime (an artifact derived from libstdc++)
  inside a distributed executable is covered by the exception for libstdc++'s runtime parts, but LICENSE.md must say
  so precisely when the first artifact ships (the rule of CLAUDE.md: extend it when vendoring from a new source).

## 15. Sources

- The host: `g++ (Alpine 15.2.0) 15.2.0`; `Alpine clang version 23.1.4` (`/usr/lib/llvm23/bin`; clang 16 and 22 also
  installed); libstdc++ 15.2.0 headers `/usr/include/c++/15.2.0`; musl 1.2.6 and `/root/musl` (C23's CS11);
  `libclang-cpp.so.23.1` without headers; GCC plugin headers `/usr/lib/gcc/x86_64-alpine-linux-musl/15.2.0/plugin/include`.
  Licenses as Alpine's packages state (GCC: GPL-3.0-or-later with the Runtime Library Exception for libgcc and
  libstdc++; clang and LLVM: Apache-2.0 with LLVM Exceptions); to verify with `apk info -L`.
- gcc 15.2.0: `https://ftp.gnu.org/gnu/gcc/gcc-15.2.0/gcc-15.2.0.tar.xz`, SHA-256
  `438fd996826b0c82485a29da03a72d71d6e3541a83ec702df4271f6fe025d24e` (`test/c/corpora.edn`, `:pins :gcc-15.2.0`; fetched
  into `.tmp/c1/gcc/` by C1). Read here (listing and `g++.dg`, `libstdc++-v3/testsuite` extracted into the scratch):
  `gcc/cp/` (58 files, 288,409 lines), `gcc/testsuite/g++.dg/`, `g++.old-deja/`, `c-c++-common/`, `libstdc++-v3/include/`,
  `libstdc++-v3/src/` (194 files), `libstdc++-v3/testsuite/`, `COPYING.RUNTIME`.
- LLVM: `https://github.com/llvm/llvm-project`, the release tag of clang 23.1.4 (expected `llvmorg-23.1.4`; not fetched: tag,
  SHA-256 and sizes at the fetch); `clang/test/SemaCXX`, `clang/test/CXX`, `clang/test/Parser`, `clang/test/Modules`,
  `clang/test/CodeGenCXX`, `libcxx/` (headers, `src/`, `test/`), `llvm/docs/LangRef.rst` (the IR's grammar). llvm-test-suite:
  `https://github.com/llvm/llvm-test-suite`. YARPGen: `https://github.com/intel/yarpgen`. Csmith:
  `https://github.com/csmith-project/csmith`.
- go-whim `/root/go-whim` (revision `80c49bf5` named by C23-SURVEY, not re-read here): `wpp/` (src/, `wpp.go`, `wpp_test.go`,
  `testdata/instances/main.cpp`), `doc/CPP.md`, `doc/CPP-IDIOMS.md`, `crefactor/togo/cpp*.go`, `cpp_test.go`.
- jdk26u `/root/jdk26u/src/hotspot` (3,810 files, 1,409,583 lines of `.cpp`/`.hpp`; GPL-2.0 with the Classpath Exception).
- Arbace: CLAUDE.md; doc/FULL-SUPPORT-SURVEY.md (the matrix); doc/C23-SURVEY.md (this file's base; "As built: C2", "C3",
  "C3b" read on `c23-c3b`, the C4 branch `c23-c4` named); doc/scheme/SCHEME-SURVEY.md and doc/ocaml/OCAML-SURVEY.md
  (the shape); doc/haskell and doc/rust (the helper pattern); `test/c/corpora.edn`.
- Probes: `.tmp/cpp/a.cpp`, `c.cpp`, `v.cpp`, `io.cpp`, `co.cpp`, `h_*.cpp`, `a.ll`, `io.ll`, `m.cpp` in the worktree
  `.tmp/worktrees/cpp-survey` (scratch, gitignored; the JSON dumps were deleted after being measured: 127,230,453 and
  607,664,490 bytes).

## Appendix A: the scratch

`.tmp/cpp/` in the worktree: `a.cpp` (P1/P2/P3/P4/P7), `c.cpp` (P5), `v.cpp` (P6), `io.cpp` (P3/P4 with the iostream),
`co.cpp` (P8), `h_vector.cpp`, `h_iostream.cpp`, `h_ranges.cpp`, `h_print.cpp`, `h_format.cpp` (P1's header sizes), `m.cpp`
(P10), `a.ll`, `io.ll`, `co.ll` (the IR), `a1.o`, `a2.o` (P7), `list.txt`, `l2.txt` (the listing of the gcc tarball), `x/` (the
testsuite's extract: `gcc-15.2.0/gcc/testsuite/g++.dg`, `libstdc++-v3/testsuite`), `c23b.md` (C23-SURVEY on `c23-c3b`). Counts of §8: by
`awk` over `l2.txt` (the tarball's listing) and `grep -l` over `x/`: `dg-do run` 1,811, `dg-do compile` 10,899, `dg-do link` 163,
`dg-do preprocess` 71, `dg-error` 4,762 files of `g++.dg`.

## Decided (2026-10-30): CP1-CP14

The user decided all fourteen as recommended. In short:
- v1 is C++23 as g++ 15 and clang 23 accept it: read and print cover the whole language but named-module BMIs; eval
  and AOT cover single-TU hosted programs over libstdc++ 15.2.0.
- clang is the oracle through a filtering helper (`tools/cxxdump`), and the reader is oracle-fed.
- Eval is clang's IR read as LLVM forms, lowered to C forms and run by C23's interpreter and AOT. "The oracle
  compiles, Arbace runs", accepted.
- Templates are instantiated by the oracle.
- Exceptions use a pending-exception flag.
- The corpora start with the pinned gcc tarball, wpp and HotSpot.
- C23 comes first. The crop order against OCaml and Scheme is left for later.

## As built: CX1 (2026-10-30)

CX1, the spec and the pins, on the branch `cpp-cx1` from main `cb01f62c`, in a few hours of work (against 2-3 days). GT7's clean run was going: the
light tier of `.tmp/agent-rules.md` held (under 2 GB and 4 threads, no gate, no Arbace build, no lock); the only Arbace run was stage 0's
`-e` on small forms, 0.25 s each. No shipped file changed (nothing under `arbace/`, `go/`, `overlay/`, `jdk/`, `seed/`; `Makefile` gained
targets, `test/gate/steps.edn` untouched).

### CX1.1 What was built

- **`doc/cpp/SPEC.md`** (1,123 lines): C++ forms for the survey's §1 and §3, extending the C forms (CP10 (a)): principles, a first example
  (P1 as forms), the round trip's levels (the AST of the print is level 3), units and names, types, declarations, classes, templates
  and concepts, code (casts, new/delete, lambdas, fold, try, range-for, coroutines), the preprocessing layer, `:full`, the
  oracle's facts (§12.1, the table of what the converter attaches) and the converter's table of clang's node kinds (§12.2, 94
  kinds), `tools/cxxdump`'s design (§13), the host data and the layouts (§14), the closed list of reserved heads (§16.1), seven worked
  examples (whim++'s `host.hpp`, `host.cpp`, `editor.cpp`, `editor.hpp`, the most vexing parse P6, layouts P5, a suspending coroutine P8).
  **All 12 forms blocks read with Arbace's reader on stage 0** (`bin/repr-cpp spec-check`; the check was seen to fail on `a::b`).
- **`test/cpp/corpora.edn`**: the pins and 11 corpora in `test/c/corpora.edn`'s format (§CX1.3).
- **`test/cpp/host/`**: the host data for `-std=c++23` and the other fifteen (§CX1.4), recorded by `bin/repr-cpp host record`, rechecked
  by `host check` (second run: "the recorded answers stand", 7 s).
- **`tools/cxxdump`** (`main.go` 498 lines, `main_test.go`, `go.mod`): the oracle's filter, built and tested with TamaGo's go
  (`CGO_ENABLED=0`; `go vet`, `gofmt`, `go test` clean) (§CX1.5).
- **`bin/repr-cpp`**: `host record|check`, `pins check`, `spec-check`, `cxxdump-build|cxxdump-test|cxxdump`; Makefile targets
  `repr-cpp-host-record`, `repr-cpp-host-check`, `repr-cpp-pins-check`, `repr-cpp-spec-check`, `repr-cpp-cxxdump-build`,
  `repr-cpp-cxxdump-test`; `make check-makefile` passes.

### CX1.2 The probes settled

- **P5, record layouts.** `-Xclang -fdump-record-layouts[-simple]` on `-fsyntax-only` prints nothing, because clang computes a layout when
  CodeGen needs one; with `-S -emit-llvm` it prints, for each record the code uses, the AST layout (`-simple`: `Size`, `DataSize`,
  `Alignment`, `BaseOffsets`, `FieldOffsets` in bits; the full form: a byte-offset table with `(B vtable pointer)`, primary bases, bit-field
  ranges `0:0-2`, `sizeof`, `dsize`, `align`, `nvsize`, `nvalign`) and the IR layout (`CGRecordLayout`: `%struct.D = type { %struct.B.base,
  i8, double }`, `%struct.B.base = type <{ ptr, i32 }>`, `%struct.F = type { i8, i8, [2 x i8] }`); the data layout string is
  `e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-i128:128-f80:128-n8:16:32:64-S128`. g++'s `-c -fdump-lang-class` gives sizes and vtables
  (`Class D size=24 align=8 base size=24`, `D::_ZTV1D: 3 entries`) as the second opinion; it writes `FILE.cpp.001l.class` beside the *output*
  file. Proposal CP20.
- **P8, a suspending coroutine.** A generator (`co_yield`, `suspend_always`, `final_suspend`) at `-O0`: **zero `llvm.coro.*` calls**; the IR
  has the ramp `@_Z5counti` and the split `.resume`, `.destroy`, `.cleanup` functions, three `__await_suspend_wrapper__init|yield|final`
  helpers and the library's calls (`coroutine_handle::resume` through the frame), the body in `.resume` a `switch` on the suspend
  index. g++ and clang builds both return 3 from the probe (0+1+2). The survey's guess ("the passes ran at -O0") is confirmed.
  libstdc++'s coroutine types are in the inline namespace `std::__n4861` (`St7__n4861` in the mangling). Spec §14.3.
- **P3 answered for the helper:** a streaming Go filter handles the 608 MB JSON of `io.cpp` in **9.9 s wall and 199 MB peak** (RSS) in all,
  clang's own 8.5 s included; it keeps 1 declaration (28,968 bytes) of 7,619; the `<vector>` program of P1 keeps 6 of 2,736 (46,540
  bytes). Clang's JSON prints locations as deltas (`file` and `line` only on change) and `includedFrom` on every header location; the
  filter follows every location in document order, in dropped nodes too, and writes them absolute. A first reading of the P3 note
  ("the main file's declarations carry no `file` key") was true only after the first of them, which does carry it.

### CX1.3 The pins

- **gcc 15.2.0**: the tarball of C23's pin, reused (`test/c/corpora.edn`): SHA-256 `438fd996...25d24e`, 101,056,276 bytes; `bin/repr-cpp pins check` confirms the
  tarball in the main checkout's `.tmp/c1/gcc/` still matches.
- **llvm-project, clang 23.1.4**: the tag `llvmorg-23.1.4` (tag object `f2c432f2...`, GitHub: signature valid; commit `7a941dfb2877a0b8cf30323fefa85ad019e0b0f5`, tagged
  2026-10-20), the release asset `llvm-project-23.1.4.src.tar.xz`, **179,272,572 bytes (under the 200 MB limit, so it was fetched)**, SHA-256
  `819ecf7351f1a66f76b2d701d8942faaf5faeb324cb90c9d5b090de142434bb2` (equal to the digest GitHub publishes for the asset), SHA-512
  `14435317...bde23` (equal to the one in Alpine's APKBUILD for the same file: a source other than the asset's host). `xz -t` passes and `tar -t`
  lists 197,228 entries. The `.sig` (466 bytes) was not verified (no gpg). The tarball stays in the worktree's `.tmp/llvm/` (scratch,
  gitignored), nothing of it extracted. Also pinned, **not fetched**: the release's `test-suite-23.1.4.src.tar.xz` (172,751,524 bytes, SHA-256
  `f3c8d4da...f562` from the assets API) for CP8 (b).
- **Alpine's `clang23` APKBUILD** at aports commit `8a39512a7631101da2e7883603b5ca5e1f55ecc6` ("main/clang23: upgrade to 23.1.4"): SHA-256
  `b299a9b7...a9b66`, 15,153 bytes, the six source patches' SHA-512s (musl triples, stack protector and fortify by default, as-needed). It is pinned by
  digest and URL, not committed (it is Alpine's file; a copy would need a LICENSE.md entry): `bin/repr-cpp pins check` reads it from `.tmp/cpp-pins/`.
- **Counts the listings gave** (the survey's §8 was from memory for LLVM and from the survey's own greps for gcc): `g++.dg` 18,066 `.C`/`.cc` (as the
  survey), **`g++.old-deja` 3,229** (the survey said 3,258: it counted more extensions), **`c-c++-common` 3,324 files, 3,251 `.c`** (the survey: 4,086), libstdc++
  testsuite **9,726** `.cc` (9,725), headers 862 files in the tarball and **834 installed**, `libstdc++-v3/src` 181 files (the survey: 194), `gcc/cp` 58. LLVM
  (the survey's "to verify"): `clang/test/SemaCXX` **1,453 files, 1,424 `.cpp`** (the survey guessed about 2,000), `clang/test/CXX` 947 (916 `.cpp`),
  `Parser` 420, `Lexer` 170, `Preprocessor` 468, `Modules` 2,251, `CodeGenCXX` 1,298; **no `clang/test/std`** in this release; `libcxx/test/std` **8,717 files**
  (7,977 `.pass.cpp`, 539 `.fail.cpp`/`.verify.cpp`; the survey said over ten thousand), `libcxx/include` 1,688, `libcxx/src` 115. The corpora file records these.

### CX1.4 The apk question (CP2) and the host data

`apk search -e` finds `clang23-dev`, `llvm23-dev`, `clang23-libs`, `llvm23-libs`, `clang23-libclang`, `clang23-headers`, `clang23-static`, `llvm23`,
`llvm23-static`, `llvm23-gtest` (all `23.1.4-r0`). Nothing was installed; `apk add -s clang23-dev llvm23-dev` simulates **7 packages**: `clang23-extra-tools` (47 MiB),
`clang23-libclang` (56 MiB), `clang23-dev` (32 MiB), `llvm23` (111 MiB), `llvm23-test-utils`, its pyc package and `llvm23-dev` (37 MiB), about **250 MiB**. The file lists
(`apk fetch --stdout PKG | tar -t`, a download without an install): `clang23-dev` holds 1,084 clang headers (`usr/lib/llvm23/include/clang/Tooling/Tooling.h`,
`ASTMatchers/ASTMatchers.h`, `AST/ASTContext.h`) and the `libclang-cpp.so` link; `llvm23-dev` holds `llvm-config`, `include/llvm/IR/Module.h` and the cmake config.
`libclang-cpp.so.23.1` (95 MiB, `clang23-libs`) and `libLLVM` (`llvm23-libs`, 180 MiB) are already installed, so a dumper linked to the monolithic
`libclang-cpp.so` needs **no static package** (`clang23-static` 828 MiB and `llvm23-static` 445 MiB are for `libclangTooling.a` and friends). Proposal CP25.

The host data (`test/cpp/host/`, 643 KB): predefined macros of g++ and clang for **sixteen standards** each (`c++23`: 495 and 502 macros; `c++98` to `c++26` and the
`gnu++` ones), the search lists (in C++ the compiler's own include directory is **last**: g++'s `/usr/lib/gcc/.../15.2.0/include`, clang's
`/usr/lib/llvm23/lib/clang/23/include`; libstdc++'s three directories, `/usr/include/fortify`, `/usr/include` first in both), 69 `__has_*` answers of both compilers
(`carries_dependency`: g++ 0, clang 200809L; clang answers with an `L` suffix), 38 `sizeof`/`alignof` pairs that **g++'s and clang's runs agree on** (checked: the
recorder fails on a difference), the date macros at the fixed epoch (as C's CS43), 834 header digests. `SOURCE_DATE_EPOCH` fixes `__DATE__` in both compilers.

### CX1.5 `tools/cxxdump`

See spec §13. The first version is not small by line count (498 lines with its EDN writer) but it is one file with no dependency: stream the TU's `inner`
array with `encoding/json` tokens, an ordered tree per element, containers streamed recursively, locations followed in document order and made absolute, the kept
nodes written as **Clojure-reader data** (Arbace has no JSON reader: `arbace.core/read` reads the output; checked on P1's and `io.cpp`'s). `-implicit=false`, `-stdin`,
`-format json`. A unit clang rejects ends with "clang rejected the unit" and clang's diagnostics on the standard error.

### CX1.6 Checks

Run (all light): `bin/repr-cpp host record` then `host check` (second run: the answers stand); `pins check` (gcc, llvm, APKBUILD match; clang 23.1.4 and g++ 15.2.0;
the six apk packages found); `spec-check` (12 blocks, 0 unreadable; and 1 of 1 unreadable on an injected `a::b`); `go vet`, `gofmt -l`, `go test` of `tools/cxxdump`; `make
check-makefile`; the cxxdump runs of §CX1.2. **Not run:** `bin/gate` in any form (GT7's clean run; the essential gate is the user's rule per merge, to run after GT7); any Arbace
build; cxxdump over a corpus.

**Heavy or later checks left for after the tag** (none is a gate step yet: `test/gate/steps.edn` was not touched):
1. `bin/gate` (essential) in the worktree, and adding the light steps `repr-cpp host check`, `pins check`, `spec-check`, `cxxdump-test` to `test/gate/steps.edn` with their
   inputs (the planner needs the declared files; the audit reads them).
2. cxxdump over the g++.dg compile corpus (10,899 files, sharded, `-fsyntax-only` with libstdc++ headers: seconds each, hours in all) for the filter's total
   time, the peak memory per file and the files clang rejects; CX3's numbers.
3. cxxdump equality with an independent whole-dump filter over a sample of 100 corpus files (this milestone compared one small dump by hand and by a Python parse, not a corpus).
4. HotSpot's 3,810 files through cxxdump (which of them read at all without HotSpot's build flags).
5. `bin/repr-cpp host check` on the other hosts the gate runs on, and after any Alpine update (the record is exact).
6. The 862 libstdc++ headers in the tarball against the 834 installed (which 28 are not, and whether the corpus needs them): the tarball extraction.
7. Verifying the llvm tarball's `.sig` where a gpg exists.

### CX1.7 Proposals (CP20-CP28)

Each with options, the recommended one first; the spec's choices stand until the user decides.

- **CP20 Where record layouts come from.**
  - Options: (a) from the oracle by a CodeGen pass with `-fdump-record-layouts-simple` (the AST layout in bits: sizes, offsets, bases), once per unit, parsed by the helper into the `:layout` facts; the IR's struct types
    for eval (CX6); (b) from the IR's struct types and the data layout string alone (no text to parse; no bit-field offsets, no vptr or primary-base knowledge); (c) a libTooling dumper (`ASTRecordLayout`, CP25 (b)).
  - *Recommended: (a).* It is the only source of source-level layouts (bit-field ranges, primary bases, `nvsize`); (b) is what eval needs and costs nothing extra; (c) waits for CP25.
- **CP21 Where the facts live.**
  - Options: (a) a sibling file `NAME.facts.ae`: one map from a path of child indices to a fact map (spec §12.1); (b) metadata on the forms, with every literal that needs a fact wrapped in a carrier form; (c) not stored: recomputed by rerunning the oracle when a consumer needs them.
  - *Recommended: (a).* A literal cannot carry metadata (the C forms' constraint); (b) changes the forms for the sake of the facts; (c) makes every consumer depend on clang being present, and the facts are what CP4's "the oracle compiles, Arbace runs" records.
- **CP22 How a qualified name is written.**
  - Options: (a) one symbol with `/` (`std/chrono/seconds`; the Clojure reader's symbol has the first `/` as its namespace separator and the printer joins it back); (b) a list `(cpp/qual std chrono seconds)`; (c) dotted `std.chrono.seconds` (collides with Java-style class names in the reader's eyes).
  - *Recommended: (a).* The reader refuses `std::chrono`; (a) keeps the forms short and a symbol still a symbol; (b) is the escape for the one case (a) cannot write, a qualified name whose parts include a template-id, which is `(cpp/scope (<> W int) get)` already.
- **CP23 Whose predefined macros and search list the preprocessor takes.**
  - Options: (a) clang's (the oracle's AST is made with them, so the preprocessor and the AST agree token for token); (b) g++'s (the judge); (c) the corpus action's choice.
  - *Recommended: (a),* with g++'s recorded (it is, `test/cpp/host/predefined/gxx/`) for the class `gxx-clang` of units whose `#if`s differ (`__GNUC__`, `__clang__`, feature-test macros that differ: 495 against 502 macros at `c++23`).
- **CP24 The helper's output format.**
  - Options: (a) Clojure-reader data, one form a line (built); (b) JSON Lines (built as `-format json`); (c) a compact S-expression per clang kind in `godump`'s style, designed with the converter at CX3.
  - *Recommended: (a) for CX3,* and (c) only if the converter's reading of the data shows its size is a cost (the filtered data of P1 is 46 KB for 7 lines; the filtered data of a 60,000-line file like `editor.cpp` is the thing to measure at CX3).
- **CP25 libTooling packages.**
  - Options: (a) none now: the JSON route costs 9.9 s and 199 MB for the worst probe; decide at CX3 on the numbers over a corpus; (b) install `clang23-dev` and `llvm23-dev` (7 packages, about 250 MiB) at CX3 and write the dumper in C++ for tokens, layouts and macro callbacks; (c) install now.
  - *Recommended: (a).* Tokens, comments and alternative spellings come from the shared lexer (CX2); layouts from CP20; what is left for libTooling is only speed.
- **CP26 Whether header units have forms.**
  - Options: (a) the helper drops the headers' declarations; `c/include` keeps the name and digest; a header's forms exist when a corpus entry names it (`libstdcxx-headers`, a program's own headers); (b) every included header read into forms (the AST of `<iostream>` is 600 MB, the forms would be hundreds of MB per header closure); (c) only the program's own headers (wpp's `editor.hpp`) and none of the system's.
  - *Recommended: (a).* It is the only one that scales to 18,066 files that include libstdc++; (c) is what v1's programs need and (a) contains it.
- **CP27 The set of standards the host data records.**
  - Options: (a) sixteen (`c++98`..`c++26`, `gnu++98`..`gnu++26`, both compilers; 643 KB); (b) `c++23`, `gnu++23` and `c++17` (3 per compiler; 120 KB); (c) `c++23` only.
  - *Recommended: (a).* The corpora's tests set their own `-std` (g++.dg has C++98 to C++26), the cost is one recorder run, and a missing standard is a failure at CX2's token check.
- **CP28 A user's namespace called `cpp`.**
  - Options: (a) escape such a name with `(c/id "cpp::x")` (a reserved prefix, like C's reserved heads); (b) rename the family to `cxx/` (a user namespace called `cxx` is as likely); (c) refuse such a program.
  - *Recommended: (a).* A program with a namespace called `cpp` is rare (none in the corpora so far); `cpp/x` for a head `x` that is not a C++ keyword (`file`, `alias`, `scope`, ...) is the only clash and the converter can test it.
- **CP29 `cpp/enum` always, or `c/enum` when valid C.**
  - Options: (a) `cpp/enum` (and `cpp/enum-class`) in every C++ unit, `c/enum` only in C; (b) `c/enum` when the enumeration is valid C, `cpp/enum` otherwise.
  - *Recommended: (a).* One head per language for a printer; the spec says it so; (b) makes the same source text read differently under C and C++ readers for no gain.

### CX1.8 Rule slips and notes

- The first stage 0 `-e` run that failed (`clojure.java.io` not available) wrote an error report to `/tmp` before `TMPDIR` was set in that shell; I removed that one file (`/tmp/arbace-10217912861722075296.edn`).
  The 34 other `arbace-*` files in `/tmp` are not from this task. All later runs had `TMPDIR` under the worktree's `.tmp/tmp`.
- `python3 -I` was used on my own files and on the release's JSON (a downloaded file, parsed with `-I` in a scratch directory), not on the downloaded tarballs.
- The tarball (179 MB) and the APKBUILD are in the worktree's `.tmp/llvm/` and `.tmp/cpp-pins/` (the scratch the pins check reads; copy it before the worktree is removed);
  the gcc tarball and stage 0 were read from the main checkout, read-only.
- The first draft of the stage 0 call used `/dev/null` in a probe of `cxxdump` (`-o /dev/null`): a slip of the rule against `/dev/null`; the output was a file elsewhere in the next runs.

## As built: CX2 (2026-10-30)

CX2, the shared lexer and preprocessor, on the branch `cpp-cx2` (from `c23-c3b`, which holds repr/c's `arbace.repr.c.lex` and `.pp`, with `cpp-cx1` merged in), in
one working day (against 4-6 days). GT7's clean run was going: the light tier of `.tmp/agent-rules.md` held for the work (the port's runs at 4 threads, g++ and
clang at 4 at a time under `nice`, no gate, no Arbace build, no lock), with one exception, a memory peak above 2 GB, in §CX2.7. The proposals CP20-CP29
are undecided; the recommendations were followed (CP23: the preprocessor reads g++'s host data when it is judged by g++, clang's when it serves the AST, a
parameter of `host/load-host`).

### CX2.1 What was built

- **The lexer, `arbace.repr.c.lex`, in C++ mode.** A scanner takes a feature mask (`F-CXX` and the bits below; a boolean for C, as before, so C's lexing is
  untouched): `::`, `->*`, `.*`, `<=>` (C++20), the digraphs `<: :> <% %> %: %:%:` (`%:` is `#` and starts a directive), `<::` as `<` and `::` unless a `:` or `>` follows
  (C++11, the digraph `<:` before), raw string literals `R"d(...)d"` with the prefixes `LR`, `uR`, `UR`, `u8R` (C++11; the characters taken as they stand in the file, lines
  counted, an unterminated one an error and one token to the end of the file as clang gives it), user-defined literals (C++11; kinds `:UDLSTRING`, `:UDLCHAR`; a suffix
  begins with an underscore or a non-ASCII character, as clang's raw lexer reads it: libcpp reads any identifier, the outputs print the same text), the literal prefixes by
  standard (`u"` `U"` `u8"` `u'` from C++11, `u8'` from C++17; before them an identifier and a literal), digit separators from C++14, an unterminated `'` or `"` one `:OTHER`
  token to the end of the line (libcpp's and clang's way; C's: an error that ends the unit), combining marks inside identifiers, and `import <h>;`, `export import "h";`
  at a line's start lexed with a header name (`F-MODULES`, only with `-fmodules-ts`: without it g++ reads them as ordinary tokens, probe).
  The feature mask of a standard is `arbace.repr.cpp.host/feature-mask` (libcpp's `lang_defaults`: trigraphs in the ISO modes up to C++14, raw strings from C++11 in all modes
  but with g++ not in `-std=gnu++98`, probe).
- **The preprocessor, `arbace.repr.c.pp`.** `:cxx` in the configuration: `true` and `false` are 1 and 0 in `#if` at every standard; the alternative tokens (`and or not xor bitand bitor compl
  not_eq and_eq or_eq xor_eq`) are the operators in `#if`, and an error as a macro name in `#define`, `#undef`, `#ifdef` and `defined`; a user-defined literal whose suffix is a macro's
  name is split into the literal and the macro (`is_macro_not_literal_suffix`, `"s"FOO`), also in a macro's body at its definition; a user-defined literal is escaped by `#` as a string
  is; a paste gives whatever the lexer reads the text as; `#include_next` finds the entry of the search list after the current file's directory with the entries cleaned (libstdc++'s list
  has `..` in it; the C list never did: a recursion to depth 200 before the fix). `__has_cpp_attribute` and `__has_include_next` were there; `__cplusplus` is the host's.
  C's results do not change: `bin/repr-c check --tokens` on c23conf (49 files: lex 49 equal, pp 49 equal, gcc 49 agree) and tcc-tests2 (88: 88, 88, 88), the numbers of C3b, before and after.
- **`arbace.repr.cpp.host`** (`arbace/repr/cpp/host.ae`): `load-host DIR "gxx"|"clang"` over `test/cpp/host/` (the predefined macros of the sixteen standards, the search list with the
  compiler's directory last, the `__has_*` answers, `has-extra.tsv`, the option deltas), the standards and their aliases (`c++2a`...), `dg-options` (the options of a g++.dg file that
  carry no selector), `file-config`, `feature-mask`, `config`.
- **`arbace.repr.cpp.ppcheck`** and `bin/repr-cpp pp-check` (`bin/lib/repr-cpp/pp-check`): the token check. Each file of the corpora is read in place, lexed (the raw tokens: position and text) and preprocessed by the
  port and judged by two recorded answers, as C's CS51 (a) judges by gcc -E: the **stream's** tokens against **g++ -E -P**'s (`test/repr/cpp/pp-gxx.sha256`: the SHA-256 of the tokens as
  C's, or `rejected`; g++'s output lexed by the same lexer with the user-defined literals off, its `#pragma` lines dropped, its `#embed` lines decoded), or both reject the file (g++'s status
  not 0 and an error of the port's), or the known file lists it; the **raw tokens** against **clang's `-Xclang -dump-raw-tokens`** (`test/repr/cpp/lex-clang.sha256`: line, column and text of each token but
  comments and white space; clang places a token at the line splices before it, the port's positions are moved to match; the escapes of clang's dump undone). Known classes:
  `test/repr/cpp/pp-check-known.edn`. Also `bin/repr-cpp pp FILE...` and `lex FILE...`, `pp-diff`-like debugging commands in the namespace (`diff`, `lexdiff`), `has-add` (the `__has_*` names a check found
  missing, asked of both compilers: `test/cpp/host/has-extra.tsv`, 65 names after the sample runs) and the Makefile's `repr-cpp-pp-check`, `repr-cpp-pp-record`.
- **Option deltas** (`test/cpp/host/opt-deltas-gxx.edn`, 324 option lists): a g++.dg file's options change the predefined macros in ways the port does not model (`-O2` adds `_FORTIFY_SOURCE 2` on Alpine,
  which musl's fortify headers act on; `-fcontracts`, `-fopenmp`, `-fconcepts`, `-fno-exceptions`...). The recorder asks g++ for the macros under each (standard, options) list and stores the lines to add
  and the `#undef`s; the port applies them (else its own `-D`, `-U`, `-O` model). Recorded, not modelled, so that no flag list goes out of date.
- **`test/cpp/lexical/`** (19 files, the corpus `:cpp-lexical` of `test/cpp/corpora.edn`): Arbace's own cases, each with its `dg-options`: punctuators and digraphs at C++98, 11 and 20, raw strings, user-defined literals (with a
  macro suffix), literal prefixes at C++14 and 23, `#if` with the alternative tokens, `true`, `__has_cpp_attribute`, `__has_include`, `#include_next` through libstdc++ and musl's fortify wrappers, module directives with and without
  `-fmodules-ts`, pragmas and `_Pragma`, `__VA_OPT__`, pastes that make the new tokens, two rejected files.

### CX2.2 The probes settled

- **Raw strings.** g++ does not read them in `-std=c++98`, `c++03` and the `gnu++` ones of those either (`R"x(FOO ")x"` is a macro-expanded `R` and an unterminated string); clang reads them in every GNU mode,
  so the raw-token check differs there, and the cases are two files of the lexical corpus with the recorded clang digest of their own standard.
- **User-defined literals and macros.** g++ at C++11 and later lexes `"s"FOO` as a literal with a suffix, unless `FOO` is a macro, and then warns (`C++11 requires a space between literal and string macro`) and expands it; it prints a
  space after a string, none after a character literal. clang's raw lexer: a suffix not beginning with `_` is a separate identifier (`"s"x` is two tokens), a UTF-8 suffix is part of the literal. The port follows clang at the lexer and
  libcpp at the preprocessor (the split by macro name), and the judged output is the same in all cases probed.
- **Trigraphs** are on for the ISO standards up to `c++14` (not `c++17`: g++ 15 turns them off there), as `-trigraphs` says.
- **`-O` and Alpine.** `-O1` and up define `__OPTIMIZE__` and `_FORTIFY_SOURCE 2` (an Alpine patch), `-Os` also `__OPTIMIZE_SIZE__`, `-Ofast` `__FAST_MATH__`, `-O0` `__NO_INLINE__`; `__GNUC_STDC_INLINE__` always. This was the cause of the
  first real difference (the fortify header chain of a `-O2` file): the deltas of §CX2.1.
- **`-fmodules-ts`.** `g++ -E -fmodules-ts` looks the imported header units and modules up and fails when there is no compiled module (`failed to read compiled module`), and rewrites `import <h>;` to `import "/abs/path";`
  where it succeeds. 20 of the 1,807 sampled files are of this kind; they are a class (`modules-ts`), not a lexical question. Without the option g++ reads `import <vector>;` as `< vector >`.
- **`#embed` in C++.** g++ supports it at every standard, as C does; its `-E -P` output keeps long resources as `#embed ... __base64__(...)` lines, which the judge decodes as C's does.
- **An unterminated `'` or `"`** is a warning in g++ and clang and one token to the end of the line (`CPP_OTHER`); `1'000` at C++11 is `1` and `'000;` there.
- **`#include_next` through libstdc++.** `<stdlib.h>`, `<math.h>`, `<string.h>` of libstdc++ reach musl's through `/usr/include/fortify/` by `#include_next`; the search list's entries must be compared cleaned.
- **A file with no known extension** (`c/cstdarg`) is a linker input to g++ and clang; the recorder passes `-x c++`.
- **`__has_builtin`, `__has_attribute`, `__has_cpp_attribute` answers differ by standard** only for the C++ attributes (`likely`, `assume`, ...); the host table is recorded at `c++23`. Over the samples no file disagreed for that reason (the
  names asked came from libstdc++'s `__has_builtin` and `__has_attribute` tests). Proposal CP33.
- **Speed.** The shared preprocessor is C's and was made for C: libstdc++'s `<vector>` takes 28 s the first time in a process (the headers' groups are parsed and kept) and about 3 s a translation unit after that; a g++.dg file
  averages 0.3 s at 4 threads (509 s for 1,807), a libstdc++ header as a main file 3.8 s (407 s for 108). Resident set at the end of the sample runs: 1.4 to 1.8 GB.

### CX2.3 Numbers

Samples (the light tier; the whole is the heavy list, §CX2.5): every 10th file of `g++.dg` (sorted), every 8th of the libstdc++ headers in the tarball's `include/`, all of the lexical corpus.

| corpus | files | lex: agree / known | pp: agree / both reject / known | time (4 threads) |
|---|---|---|---|---|
| `cpp-lexical` | 19 | 19 / 0 | 14 / 4 / 1 | 15 s |
| `gxx-dg` (every 10th of 18,066) | 1,807 | 1,806 / 1 | 1,735 / 44 / 28 | 507 s |
| `libstdcxx-headers` (every 8th of 862) | 108 | 107 / 1 | 103 / 5 / 0 | 407 s |

(Final runs, after the last edit of the lexer and preprocessor.) The known entries: `modules-ts` 20 (the lexical corpus' one included), `gxx-rejects` 7 (pedantic or character-set errors, a header unit), `target-option` 2 (`-march=...`),
lexical `named-ucn` 1 (C++23's `\N{...}` in an identifier) and `not-cxx` 1 (libstdc++'s `Makefile.am`). g++ rejects 82 of the 1,934 recorded files; on 53 of them the port reports an error too (the sample's both-reject column). The recorded data:
`pp-gxx.sha256` (1,934 lines, 178 KB), `lex-clang.sha256` (1,934 lines, 183 KB), `opt-deltas-gxx.edn` (324 lists, 26 KB), `has-extra.tsv` (65 names).

### CX2.4 Checks

Run (all light): `bin/repr-cpp pp-check --corpus cpp-lexical --record --add-missing`, then `--record-known`, then plain (the recorded answers alone, no compiler needed); `--corpus gxx-dg --sample 10 --record --add-missing`, `--redigest` after the last lexer fix, and then plain;
`--corpus libstdcxx-headers --sample 8 --record --add-missing` and then plain; `bin/repr-c check --tokens --corpus c23conf,tcc-tests2` before the first edit and after the last (identical); the lexer's directed cases by hand against clang's raw dump
(`import <vector>;` with the modules bit, the combining marks); `make check-makefile`.
**Not run:** `bin/gate` in any form; any Arbace build; the other eighteen corpora of C's token check; `bin/repr-c parse-check` and `roundtrip` (C's parser and converter use the same lexer and preprocessor: the lexer's C path is the old code
and `bin/repr-c check --tokens` is the direct check); `bin/repr-cpp host check` (nothing under the recorded files but the new `has-extra.tsv` and `opt-deltas-gxx.edn` was written there); the full `g++.dg`, `g++.old-deja`, `c-c++-common`, the libstdc++ testsuite
and all 862 headers (§CX2.5).

### CX2.5 The heavy list (for a window after the tag)

1. `bin/gate` (essential) in the worktree; the files changed under `arbace/` change the gate's key (`arbace/` less repr/java, haskell, rust), so the executable and test build rebuild. `arbace/repr/c/lex.ae` and `pp.ae` are repr/c's:
   run C's `bin/repr-c check --tokens` (all twenty corpora), `parse-check`, `roundtrip` and `bin/gate --full`'s repr/c steps; the lexer's C path has no behavioral change by construction (the new branches test `cxx?`), but only those checks say so.
2. The whole `gxx-dg` (18,066 files): about 85 minutes of the port at 4 threads (509 s for a tenth) and about 30 minutes of g++ and clang to record (167 s for a tenth); `bin/repr-cpp pp-check --corpus gxx-dg --record --add-missing`. At 8 threads about half. Memory 1.8 GB at 4
   threads for a tenth, to be measured for the whole (the header cache grows): run it under the big lock or in shards (`--sample` takes every Nth, no offset: add one if shards are wanted).
3. The other corpora: `libstdcxx-headers` (862: about 55 minutes), `gxx-old-deja` (3,229), `c-c++-common` (3,251 `.c` read as C++), `libstdcxx-testsuite` (9,726; its harness' include path is added by DejaGnu, the script adds `testsuite/util` only if the corpus' entry is extended) with `--record`;
   the lexical and header corpora's `Makefile.am`-like files may need `not-cxx` entries.
4. `test/gate/steps.edn`: the steps `repr-cpp pp-check` (recorded answers only, a sample or the lexical corpus) with their inputs (`arbace/repr/c/lex.ae`, `pp.ae`, `arbace/repr/cpp/*`, `test/repr/cpp/*`, `test/cpp/*`, `bin/lib/repr-cpp/pp-check`, the gcc tarball's pin).
5. The hotspot corpus through `pp-check` (`/root/jdk26u` and its build flags) and the wpp corpus (go-whim): not in CX2's exit.
6. `bin/repr-cpp host record` on another host, then `has-add` for the names the checks found (a host with another Alpine needs both).

### CX2.6 Proposals (CP30-CP35)

- **CP30 The user-defined literal suffix rule.**
  - Options: (a) clang's at the lexer (an underscore or a non-ASCII start), libcpp's macro split at the preprocessor (built); (b) libcpp's everywhere (any identifier is a suffix unless it is a macro: `"s"x` is one token, `"s"FOO` two);
    (c) clang's everywhere (no macro split: `"s"_FOO` stays one token when `_FOO` is a macro).
  - *Recommended: (a).* The lexer is checked against clang's raw tokens (the converter's oracle), the preprocessor against g++'s output; the two disagree only on programs the standards call ill-formed or reserved (a suffix without an underscore), and (a) agrees with each judge where it judges.
- **CP31 Option-dependent predefined macros.**
  - Options: (a) recorded deltas per (standard, option list) from g++ (built, 324 lists in the sample); (b) a model of a fixed set of flags (`-O`, `-D`, `-U`, `-std`, `-fno-exceptions`, `-fno-rtti`, `-fopenmp`, `-fconcepts`...); (c) refuse the files whose options are not modelled.
  - *Recommended: (a) for the check, (b) for the converter* (CX4): the converter's units come with clang's flags, few, and clang's own delta for them can be recorded the same way (`opt-deltas-clang.edn` is read when it exists: `load-host` already looks for it).
- **CP32 `-fmodules-ts` files.**
  - Options: (a) a known class (built: g++ -E needs compiled modules, its output is its own rewriting); (b) model g++'s rewriting (`import <h>;` to `import "abs-path";` and the failures); (c) judge them against clang's preprocessor (`clang -E -std=c++20` with its header unit rules) instead of g++'s.
  - *Recommended: (a)* for the token check; (c) at CX3 for modules in the converter, where clang is the oracle.
- **CP33 The `__has_*` table by standard.**
  - Options: (a) one table at `c++23` plus `has-add` for names (built); (b) a table per standard (16 files, the attribute names a check asks: about 30); (c) only for `__has_cpp_attribute` (the one that varies).
  - *Recommended: (c)* when the full run finds a file that depends on it; none of the 1,934 recorded files did. `has-add` would take a standard and write `has-STD.tsv`.
- **CP34 Unterminated literals as tokens.**
  - Options: (a) in C++ mode a `:OTHER` token to the end of the line, as libcpp and clang (built); (b) cc's error that ends the unit, as C's; (c) also in C mode (a change to C's results: C's check would show it, CS-class files).
  - *Recommended: (a);* (c) is for the C side to decide (gcc's C preprocessor does the same).
- **CP35 The cost of the preprocessor on libstdc++.**
  - Options: (a) as it is (28 s for a first include of `<vector>`, 3 s after, per process); (b) cache the parsed groups of the system headers on disk (keyed by digest, the host's `headers.sha256`); (c) a faster scanner (the evaluator's cost, c2j-compiled tools).
  - *Recommended: (a) now, (c) when the preprocessor is compiled into the tool executable* (S6 does this for the other tools: repr/c is evaluated from the checkout, CS36 (a); the compiled build would take the evaluator out). (b) changes what a run is a function of.

### CX2.7 Rule slips and notes

- **Stage 0 was built in the worktree twice.** The first `bin/repr-c check` (the C baseline) and one `bin/repr-cpp pp-check` run in a shell that had lost `ARBACE_HOST_EXE` started `bin/seed --build` (a Go build of Arbace's stage 0, and once a `--compile-aot` pass of `bin/arbace-go`); I stopped each within two minutes by its process group (`kill -- -PGID`) and removed `target/`. Everything after ran on `/root/arbace/target/seed/amd64/arbace`.
- **Memory above 2 GB once:** the second sample run (`gxx-dg` and 44 headers in one process) peaked at 2.23 GB resident. The script now runs one process per corpus (peaks 1.8 and 1.65 GB). The thread count was 4 throughout; g++ and clang ran 4 at a time under `nice` while the port was not running.
- **Sources edited while a run was going:** `bin/lib/repr-cpp/pp-check` once (bash read a changed script at its end: an error message after the results), `arbace/repr/cpp/ppcheck.ae`, `lex.ae` and `pp.ae` several times (the Arbace process had loaded the sources at its start; a later process of the same chain loaded
  the new ones, which were the intended ones). The final numbers are from runs started after the last edit of `lex.ae` and `pp.ae`.
- **`/dev/null`** appeared in four of my read-only probe commands (`awk ... /dev/null`, `sed ... /dev/null`, `grep -c . /dev/null`): a slip of the rule; nothing was written there.
- `sleep` inside `timeout bash -c 'until ...'` loops to wait for a background run (the tool blocks a bare foreground `sleep`).
- The gcc tarball was extracted into the worktree's `.tmp/cpp-gcc/` (g++.dg, g++.old-deja, c-c++-common, libstdc++-v3's include and testsuite), after its SHA-256 was checked against the pin; the scratch (`.tmp/cpp-gcc/`, `.tmp/repr-cpp-check/`) is the sample runs' work directory.

## As built: CX3, CX4 (2026-10-30)

CX3, the oracle's helper, and CX4, repr/cpp in mode `:lines`, on the branch `cpp-cx2` (on top of CX2's `e8702613`), in one working
day (against 4-6 and 10-16). GT7's clean run was going: the light tier of `.tmp/agent-rules.md` held (4 threads, under 2 GB,
no gate, no Arbace build, no lock); every Arbace run was evaluated on stage 0 (`/root/arbace/target/seed/amd64/arbace`,
`ARBACE_HOST_EXE` set). The proposals CP20-CP35 are undecided; the recommendations were followed and are named below where
they bind. No shipped file changed: the new code is tools' namespaces evaluated from the checkout (`arbace/repr/cpp*`), a
Go helper and scripts; `arbace/repr/c/` is untouched.

### CX3.1 What was built: `tools/cxxdump`

| file | lines | what |
|---|---:|---|
| `tools/cxxdump/main.go` | 664 | the streaming filter (CX1's, reworked): a container's (`TranslationUnitDecl`, `NamespaceDecl`, `LinkageSpecDecl`, `ExportDecl`) children streamed one at a time; **a header's node is skipped as it streams** once its `loc` (which clang prints before `inner`) says it is not the main file's, its inner nodes read token by token with only their locations followed (`skipVal`), so no header node is held; **an instantiation under a main-file template keeps only its `TemplateArgument`s without their type trees** (`-instances` keeps them whole); `-norm`; `-implicit=false` as before |
| `tools/cxxdump/run.go` | 515 | the run (clang, the filter, the numbers), the layouts pass, the cache |
| `tools/cxxdump/norm.go` | 45 | the AST level's normal form: no `id`, `loc`, `range`, `previousDecl`, `parentDeclContextId`, `referencedMemberDecl`, no value that is a node id, the positions clang writes into names (`(lambda at a.cpp:6:20)`) masked as `at *`, `tagUsed` of a template type parameter dropped (N8) |
| `bin/lib/repr-cpp/cxxdump-check` | 51 | cxxdump over a list (pp-check's `gxx.list` format), in each file's directory under its relative name, the numbers in `summary.tsv` and `totals.txt` |

- **Layouts (CP20 (a), recommended and followed).** `-layouts` runs a second clang pass, `-S -emit-llvm -Xclang -fdump-record-layouts`
  (the IR to a scratch file), and parses both dumps: each `*** Dumping AST Record Layout` block (the type as clang prints it, `sizeof`,
  `dsize`, `align`, `nvsize`, `nvalign`, the vtable pointer's offset, the direct bases with offset, primary and virtual, the fields with
  name, declaration, byte offset and bit range) is paired with the `*** Dumping IRgen Record Layout` block that follows it for the same
  record (its `Record:` line gives the declaration's kind and location, its `LLVMType:` the IR type); the main file's records are
  written as `{:layout {...}}` lines after the declarations (spec §17.6's facts, in the helper's output; the facts file of CP21 is CX5's,
  CP49). A unit whose code CodeGen refuses keeps its AST (`:layouts-error true` in its numbers).
- **Numbers (`-stats FILE`).** Each run writes one map: the JSON bytes read, the output bytes, the declarations kept and dropped, the
  layouts, the wall seconds of the AST pass and of the layouts pass, the peak resident sets of the filter and of clang (getrusage), the
  status (`ok`, `rejected`, `filter-error`) and the cache's answer.
- **The cache (`-cache DIR`).** The key is the SHA-256 of the filter's version, clang's `--version`, the options, the arguments and the
  main file's bytes; the AST pass runs with `-MD -MF` and the entry keeps the SHA-256 of every file the unit read (`KEY.deps`); an entry
  is used only when all of them still have their digests (a header changed is a miss). A rejection is kept too (clang's standard error
  with it); a unit clang stops before it writes its dependencies is not cached.

### CX3.2 Numbers

| run | JSON | kept | wall | filter peak | clang peak |
|---|---:|---:|---:|---:|---:|
| survey P3's `io.cpp` (`<iostream>`, `<vector>`, `<algorithm>`) | 607.7 MB | 1 declaration, 26 KB | 6.7 s | **88 MB** | 199 MB |
| P1's `a.cpp` (`<vector>`) | 127.2 MB | 6, 38.5 KB | 1.2 s | 20 MB | 121 MB |
| `g++.dg/template/crash124.C` (1,024 nested instantiations, rejected) | **6,807.6 MB** | 5, 1.8 MB | 17.5 s | **32 MB** (CX1's filter: 872 MB and 221 MB kept) | 103 MB |
| `g++.dg` every 10th file (1,807), 4 at a time, `-layouts` | 13,253.5 MB | 62.9 MB | 135.7 s summed (slowest 16.7 s) | 146 MiB (`warn/Wsequence-point-4.C`: 2.55 GB of JSON, 15.5 MB kept) | 123 MiB |

Of the 1,807: clang accepts 1,060, rejects 746 (the error tests, and g++'s extensions and dialect options clang refuses), and one does not
end (`cpp0x/pr104668.C`: clang loops after its errors; 300 s timeout). 601 layouts were written; three units' CodeGen failed
(`cannot compile this flexible array initializer yet`). A second run is all cache hits. **The filter's memory is bounded by the largest
main-file declaration**, not by the unit: the 6.8 GB dump of a 21-line test is filtered in 32 MB.

### CX3.3 The libTooling decision (CP2, CP25)

The numbers say the JSON route costs what clang's dump costs: the filter keeps pace with the pipe (9.9 s became 6.7 s on P3 with the
header nodes skipped), its memory is bounded, and a corpus file costs 0.075 s on average. What CX4 needed from beyond the JSON was
**spellings** (written `auto`, `explicit`, a declaration's qualifier, template arguments of a name, a parameter's type as written,
macro invocations' extents), which the converter reads from the main file's text through the dump's ranges (CX4.2); a libTooling
dumper would give the same through `TypeLoc`s and the `Lexer`. **CP25 stays (a): no packages installed**, the decision recorded with
its numbers (CP47 restates it for the user with CX4's experience).

### CX4.1 What was built: repr/cpp in mode `:lines`

| Arbace / script | what |
|---|---|
| `arbace/repr/cpp/types.ae` | a small C++ tokenizer and the reader of clang's printed types (`qualType`) and of written names: specifiers (builtin words to the C forms' symbols, `unsigned int` is `unsigned`), qualified names (`std/vector`, `(cpp/scope (<> std/vector int) size_type)`, `(cpp/global x)`), template-ids with type, literal and text arguments, `operator` names, abstract and named declarators (pointers, references, pointers to members, arrays, function types with cv, ref-qualifier, `noexcept`, `throw()`, trailing return), `parse-decl` for a parameter's or a variable's written text |
| `arbace/repr/cpp/convert.ae` | the converter: clang's node kinds to C++ forms (spec §12.2's table) for declarations (namespaces nested and inline, linkage blocks, classes with bases, access and members, constructors with member initializers, destructors, conversion and operator functions, `= default`, `= delete`, function-try-blocks, enums, typedefs, aliases, `using`, `using namespace`, `using enum`, `static_assert`, friends, function, class, variable and alias templates with type, non-type, pack and constrained parameters and defaults, explicit specializations), statements (blocks as `let` around the items after a declaration, `if` with init and condition declarations and `constexpr`, `when`, loops, range-for, `switch`, labels, `case`, `goto`, `try`/`catch`, attributed statements, coroutines' `co_return`), expressions (operators and overloaded operators back to their operator forms, calls, members, casts with their reference from the value category, constructions, braced lists, `new`/`delete`, `throw`, `typeid`, `sizeof`/`alignof`, `noexcept`, packs, lambdas with simple captures, `co_yield`/`co_await`, literals); the directives outside conditionals placed between the items of the top level, namespaces, linkage blocks and classes; text forms (CP41) for the rest, counted by kind |
| `arbace/repr/cpp/print.ae` | the printer: repr/c's canonical layout extended (one statement a line, four spaces an indent, braces exactly where the source had a compound statement, the source's parentheses and no others: every parenthesis the oracle's AST needs is a `ParenExpr` the forms keep), types as specifiers and a declarator around the name |
| `arbace/repr/cpp.ae` | `read-cpp` (a dump and the main file's text into `{:ns :file-form :forms :texts}`), `unit-text` (repr/c's `.ae` layout), `print-cpp`; `bin/repr-cpp read`, `print` |
| `arbace/repr/cpp/rtcheck.ae`, `bin/lib/repr-cpp/roundtrip` | the round trip: the originals dumped (in their directories, under their relative names, `-iquote` their directory), read and printed under `WORK/print/` at the same relative names; the prints dumped and read again; level 1 the two `.ae` texts byte for byte; level 3 the two `-norm` dumps byte for byte; level 4 the objects of g++ (`-O0 -fno-stack-protector`, the file's options) and of clang (the modelled options) byte for byte, `-fdebug-prefix-map` for the files that ask `-g`; judged against `test/repr/cpp/roundtrip-known.edn` |
| `test/cpp/samples/` | eight sample programs: P1, P6 (both readings), P8's coroutine, the layouts probe, `<iostream>` and `<algorithm>`, `auto` and lambdas, classes, declarations of every kind |

### CX4.2 How the converter reads what the AST does not keep

- **Types** are clang's printed types read back, which are the written ones with typedef sugar and qualifiers as written, but for
  three cases where the text decides: a deduced type (`auto x = 1` prints as `int`; `auto l = [...]` as `(lambda at ...)`): the
  written specifiers before the name are read instead, `^auto`, `^{:tag (& (const auto))}`; an array (clang prints the bound's value,
  `char buf[NUMBUFLEN]` as `char[65]`: the print would lose the reference to `NUMBUFLEN`, an AST difference): the declaration's text
  read as a declaration; a parameter (clang prints it decayed and desugared, `va_list ap` as `__va_list_tag *`): likewise (CP46).
- **Names** of references are their written text read as a name (`std::cout`, `n::id<int>`, `::n::g`), so template arguments and
  qualifiers are kept; a declaration's qualifier (`int D::f()`) is read backwards from its name; `explicit`, `inline`, `constexpr`,
  `consteval`, `constinit` are the written words before the name (the JSON has no `explicit`; its `inline` and `constexpr` are also
  the implicit ones).
- **Initializers** follow the declaration's `init` style (`c`, `call`, `list`; a member's from the text between its name and its
  initializer); `std::vector<int> v{1, 2}` is one pair of braces (the list's braces are the construction's), `T x;` with a class `T`
  has no initializer (clang's zero-argument construction).
- **Macros** (CP41): the outermost node whose first or last token comes from an expansion is its extent's text, the invocation as
  written with its arguments; a statement or a declaration from a macro likewise, its `;` included. This recovers the
  invocations (and so `__LINE__`, `assert`'s file and line) without the preprocessor; a file of nested directives keeps only the ones
  outside every conditional group (CP45).
- **Implicit nodes** (`isImplicit`, instantiations, the implicit special members, default arguments, default member initializers,
  the implicit `this`, conversions through conversion functions, `CXXDefaultInitExpr`s of an aggregate's omitted members) are left
  out; the oracle recreates them from the print.
- **Grouped declarations.** A declaration statement of several declarators is `cpp/decls` (one `DeclStmt`); `struct S {...} s;` at
  namespace scope is two forms (the AST has two declarations, the variable's type `struct S` elaborated); a declaration whose text
  overlaps another's and holds an unnamed class or a text form is one text (`typedef struct {...} T;`).

### CX4.3 Numbers

`bin/repr-cpp roundtrip` (4 at a time; the final runs, after the last edit of the converter and printer):

| corpus | files | oracle rejects | read | text-free | l1 | l3 | l4 g++ (orig fails) | l4 clang (orig fails) | pass | known |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| `test/cpp/samples/` | 8 | 0 | 8 | 6 | 8 | 8 | 7 (0) | 8 (0) | 7 | 1 |
| whim++ (`/root/go-whim/wpp/src` at `80c49bf5`, 10 files, 75,292 lines) | 10 | 0 | 10 | 6 | 10 | 10 | 10 (0) | 10 (0) | **10** | 0 |
| `g++.dg`, every 10th file (CX2's sample) | 1,807 | 747 | 1,060 | 703 | 1,005 | 976 | 916 (34) | 1,001 (3) | **918** | 142 |

- **No file fails**: every file passes every level it reaches, or the oracle rejects it (`oracle-rejects`: clang refuses 747 of the
  sample, the error tests and g++'s own dialect), or the known file lists it with a class (`test/repr/cpp/roundtrip-known.edn`). Of the
  sample's `dg-do compile` files: 549 pass, 86 known, 433 rejected by the oracle; `dg-do run` 107, 26, 36; `dg-do link` 18, 2, 3.
- **whim++ round-trips whole**: `editor.cpp` (61,921 lines; one text form, an initializer) and `editor.hpp` (11,090; 16) read, print,
  read again to the same forms, give clang's AST of the original, and give g++'s and clang's objects byte for byte; the printed
  `host.cpp` includes the printed `editor.hpp` (CP50). The three files with text forms hold 101 of them, every one a macro invocation or a
  member array's initializer group (`printf.cpp`'s `va_arg`s, `term.cpp`'s `sigaction` fields).
- **The known classes** (142): `gxx-objects-location` 52 (and the coroutine sample: g++ -O0's code depends on positions inside a
  function, `-fcompare-debug`, `-fsanitize`; the AST and clang's objects equal; CP48), `print-rejected` 41, `ast-differs` 34,
  `forms-differ` 14, `objects-differ` 1: the last four are the converter's and printer's open cases, classed by the first level that
  fails (CX4.5).
- **Text forms**: 2,300 in 357 of the 1,060 read files; by kind, macro invocations 1,354 (the outermost node touching an expansion:
  `static_assert`s through `SA(...)` macros 349, declarations from macros 144, expressions and statements 861), groups of overlapping
  declarations 198, partial specializations 103, placement `new` 68, explicit instantiations 42, `CXXUnresolvedConstructExpr` 39,
  structured bindings 71, lambdas with init captures or specifiers, `asm`, type traits, deduction guides (the rest).
- **Speed and memory** (light tier, 4 at a time): the sample's dumps 307 s (clang, the original and its normal form), the forms and
  prints 12 s, the prints read again 37 s, the four compilations of each file 62 s: about 7 minutes for 1,807 files. whim++: 44 s to
  read and print its 2.2 MB on stage 0 (`editor.cpp`'s dump is 90 MB of Clojure-reader data), 54 s again, 14 s of objects. One Arbace
  process stayed under the 1.5 GiB `GOMEMLIMIT`.
- **The first pass and the fix rounds**: the first full sample gave 765 passing and 295 failing; my rounds and a Sonnet subagent's
  (out-of-line template members' headers read from the text, the constructor and destructor names, clang's `type-parameter-D-I`
  spellings back to the parameters' names, inherited default arguments, attributes after declarators and on classes, record
  declarations with declarators as one text, the array filler's elements) brought it to 918 and 142, each round checked against the
  samples and whim++.

### CX4.4 Checks

Run (all light): `bin/repr-cpp roundtrip` on the samples, whim++ and the g++.dg sample (final runs above); `bin/repr-cpp cxxdump-check`
on the g++.dg sample with `-layouts`, twice (the second all hits); `go vet`, `gofmt -l`, `go test` of `tools/cxxdump` (four tests:
the filter, the Clojure-reader output, the skipped header nodes with the instantiations' summary, the normal form); `make
check-makefile`; **C's results unchanged**: `bin/repr-c roundtrip --corpus c23conf,tcc-tests2` gives c23conf 49 pass and tcc-tests2 85
pass and 3 known, C3b's numbers (no file under `arbace/repr/c/` changed). **Not run:** `bin/gate` in any form; any Arbace build; the whole
g++.dg (CX4.7); `bin/repr-cpp pp-check` (the lexer and preprocessor did not change); `bin/repr-cpp spec-check` (no forms block was added
to the spec).

### CX4.5 What is left

- The open classes: dependent names in templates with unnamed parameters (clang's `type-parameter-0-0` has no name to give back),
  abbreviated function templates and constrained `auto` parameters, unexpanded packs in lambdas, deduction guides and CTAD's
  `DeducedTemplateSpecializationType`, conversion functions to function references, `__LINE__` inside a structured node (a node whose
  boundary is not the macro's), `isReferenced` lost through canonical template arguments, `int (T::*)` printed without its parentheses
  (a `ParenType` the type reader drops), pragma placement inside functions.
- The directives inside conditional groups (CP45), the structured forms of what is text now (partial specializations, explicit
  instantiations, placement `new`, structured bindings, init captures, requires-clauses and concepts' definitions), the facts file
  (CP49): CX5's, with `:full`.


### CX4.6 Proposals (CP40-CP50)

Each with options, the recommended one marked, and why; the built choice stands until the user decides.

- **CP40 A braced list without a type.**
  - Options: (a) `(cpp/init-list e*)` in an expression, `(cpp/brace T e*)` typed, `(cpp/brace e*)` only in an initializer's place
    (built); (b) one head `cpp/brace` with `^:cpp/typed` metadata on the typed one; (c) the spec's §9.4 as written (ambiguous).
  - *Recommended: (a).* A printer cannot tell `T{x}` from `{T, x}` by the symbols (a type symbol and a value symbol look alike); a head
    keeps hand-written forms right, metadata does not.
- **CP41 Text forms and the macro unit.**
  - Options: (a) the outermost node whose boundary touches a macro expansion is its extent's text, and a node with no form yet is
    its text, counted by kind (built); (b) recover invocations from the shared preprocessor's stream (C3b's way: the run of tokens at
    one invocation's position), structured forms with `:c/macro` for whole nodes; (c) refuse such units.
  - *Recommended: (a) now, (b) at CX5* with the directive layer (CP45): (a) is exact and needs no preprocessor run; (b) keeps the
    structure of what the macros expand to as C does, at the preprocessor's cost (CX2: 28 s for a first `<vector>`).
- **CP42 Constructor and destructor declarations.**
  - Options: (a) `^:cpp/declare (cpp/ctor S [params])`, `^:cpp/declare (cpp/dtor S)` (built); (b) heads of their own
    (`cpp/ctor-declare`); (c) `(cpp/dtor S (do))` for a definition, `(cpp/dtor S)` a declaration (changing §7.2).
  - *Recommended: (c)* for the destructor and (a) for the constructor until then: §7.2's `(cpp/dtor S)` for `~S(){}` is the
    ambiguous one; a constructor's member-initializer vector already tells a definition.
- **CP43 Elaborated type specifiers.**
  - Options: (a) `(cpp/elaborated struct S)` (built); (b) `^:cpp/elaborated S` metadata as §5.1 says; (c) normalize them away
    (N9) and class the AST difference.
  - *Recommended: (a).* clang's type of the print equals the original's only if the keyword is printed, and metadata on a type symbol
    inside a tag is easy to lose.
- **CP44 Attributes.**
  - Options: (a) their texts by syntax (`:cpp/attrs`, `:c/gnu-attrs`, `:cpp/alignas`; built); (b) structured as C's `:c/attrs`
    vectors (a name and arguments as forms); (c) facts only.
  - *Recommended: (b) at CX5*, (a) until then: C's encoding exists and the printer is shared in spirit; the texts were the fast way
    to a correct print.
- **CP45 Directives inside conditional groups.**
  - Options: (a) the shared preprocessor's layer (the executed directives' offsets) for a main file whose directives nest in
    conditionals (`convert` takes the set; not wired yet); (b) the directives outside every group only (built); (c) the conditional
    lines kept with their active groups' items.
  - *Recommended: (a).* (b) drops an active `#define` under an `#if`, which a later text form or the headers may need; the layer is CX2's
    and C3b's.
- **CP46 Written types against clang's printed types.**
  - Options: (a) clang's types, the written text for deduced types, arrays and parameters (built); (b) the written text for every
    declaration through `parse-decl`; (c) libTooling's `TypeLoc`s (CP25 (b)).
  - *Recommended: (a).* clang's types are already the written ones up to the three cases, and they read without the macros a
    written declaration may hold.
- **CP47 The libTooling dumper (CP2, CP25 restated with CX3's numbers).**
  - Options: (a) no packages: the JSON filter is bounded (32 MB for a 6.8 GB dump) and keeps pace with clang; (b) install
    `clang23-dev llvm23-dev` (about 250 MiB) and write the dumper in C++ for tokens and `TypeLoc`s.
  - *Recommended: (a).* Every spelling CX4 needed was in the text the dump's ranges point at.
- **CP48 g++'s location-dependent objects.**
  - Options: (a) a class `gxx-objects-location` per file, judged by the AST level and clang's objects (built); (b) compare g++'s objects
    of the `:full` print instead (CX5), which keeps every byte's place; (c) drop g++ from level 4.
  - *Recommended: (a) and (b) at CX5*: C3b found the same of gcc 15 (`:objects`), and the `:full` print settles it.
- **CP49 The facts file.**
  - Options: (a) CX5 writes `NAME.facts.ae` (CP21 (a)) from the dump: layouts (CX3's `-layouts`), instances (the summaries), mangled
    names, types; (b) facts in the `.ae` as metadata; (c) none until eval needs them (CX6).
  - *Recommended: (a)* with CX5: the data is in the dump already and CX6's lowering will read it.
- **CP50 A corpus's own headers in the round trip.**
  - Options: (a) the prints of a corpus's headers stand beside the prints of its sources, so a printed source includes the printed
    header (built: whim++'s `host.cpp` includes the printed `editor.hpp`); (b) `-iquote` the original directory first, every print
    against the original headers.
  - *Recommended: (a).* It is the stronger check: a program printed whole compiles to the same objects.
### CX4.7 The heavy list (for a window after the tag)

1. `bin/gate` (essential) in the worktree; `arbace/repr/cpp*` is under `arbace/` and changes the gate's key (the executables rebuild,
   about 8 minutes) though nothing shipped loads it.
2. The whole `g++.dg` (18,066 files): `bin/repr-cpp cxxdump-check` (about 25 minutes of clang at 4 jobs, 135 s for a tenth) and
   `bin/repr-cpp roundtrip` (about 70 minutes at 4 jobs, 7 minutes for a tenth; the converter's one process holds a corpus's reads,
   to measure: shard by directory if it passes 2 GB), then the known file rewritten from the classes.
3. `g++.old-deja`, `c-c++-common` as C++, the libstdc++ testsuite and headers, HotSpot (its build flags), through both.
4. `test/gate/steps.edn`: the light steps `repr-cpp cxxdump-test` and `roundtrip` on the samples and whim++ (seconds and 2 minutes),
   with their inputs (`arbace/repr/cpp*`, `tools/cxxdump`, `bin/lib/repr-cpp/*`, `test/cpp/samples`, `test/repr/cpp/roundtrip-known.edn`,
   go-whim's pin).
5. CX2's list stands (its full corpora, `host check` on another host).

### CX4.8 Rule slips and notes

- **Threads**: for a few minutes a `bin/repr-c roundtrip` (3 threads) started beside a running `bin/repr-cpp roundtrip` (4); I stopped it
  by its task and ran it again later at 2 threads, while the subagent's runs used 4: about six threads in all for that run (the light tier
  says 4 an agent).
- **A stage 0 build in the worktree**: the Sonnet subagent once ran `bin/repr-cpp` without `ARBACE_HOST_EXE`, which started a stage 0
  build (`target/seed`); it removed it; the leftover `target/arbace-go/host-compile-seed.*` was moved to `.tmp/sub/slip/`.
- No source was edited while a check that loaded it ran (the subagent's rounds and mine took turns); the wrapper scripts were not edited
  during their runs but for `bin/repr-cpp` (exec'd into the round trip before the edit) and the Makefile.
- The scratch the numbers come from: `.tmp/cx/f-samples/`, `.tmp/cx/f-wpp/`, `.tmp/cx/f-dg/` (`results.tsv`, `read.tsv`, `classes.tsv`,
  `summary.edn`), `.tmp/cx/dg/` (cxxdump-check's numbers), `.tmp/sub/` (the subagent's rounds), the cxxdump cache `.tmp/repr-cpp/cxxdump-cache/`;
  copy them before the worktree is removed.

## As built: CX4b, CX5a (2026-10-30, the same day)

The fix round after CX4 and the first step of CX5, on the branch `cpp-cx2` (on top of `bff81261`), in the light tier again (4 threads, under
2 GB, stage 0 `/root/arbace/target/seed/amd64/arbace` with `ARBACE_HOST_EXE` set, no gate, no Arbace build, no lock). Proposals
CP20-CP50 are still undecided; the recommendations were followed where they bind (CP45 (a), CP48 (b) in part). New proposals CP60-CP66
below. No shipped file changed (tools' namespaces, the Go helper, the known file, the spec and survey).

### CX4b.1 What changed

| area | change |
|---|---|
| directives (CP45 (a)) | `tools/cxxdump` runs a third pass, `clang -E -dD -dI`, and writes `{:executed [LINE ...]}`: the lines of the main file's `#define`, `#undef`, `#include`, `#pragma` that were executed (a group that was taken). The converter keeps them, unconditionally, between the items of the top level and of classes and namespaces, **and between the statements of a compound statement** (`compound-forms`); the printer writes them at column 0 in a body. The conditional lines themselves are not forms: the print holds the executed branch only. A `#pragma` that clang reads as a loop hint (`LoopHintAttr`) is the directive, not `[[unroll 8]]` |
| macro invocations | `extent` of a macro node is now the invocation with its argument list (`macro-extent`); before, the overlap merge of several declarations from one `CHECK_CAST(static);` kept the macro's name only. A statement or a declaration that is a macro text carries `^{:cpp/line N}` (the line as the compiler counts it, a `#line` before it honoured) and the printer writes `#line N` before it, so `__LINE__` inside the expansion (`VERIFY`, `assert`) gives the original's number; the original's own `#line` directives are not kept as forms |
| forms for what was text | partial specializations `(cpp/template [params] (cpp/struct (<> V T *) ...))` (the template-id from the text, outer headers wrapped), explicit instantiations `(cpp/instantiate decl)` with `^:extern`, placement new `:place [p*]` (the placement arguments are the last children, as many as `operator new` takes after the size), structured bindings `(cpp/structured ^{:tag auto} [a b] init)`, dependent constructions `(cpp/construct T a)` / `(cpp/brace T a)`; spec §18.3 |
| converter fixes | a copy-initializer that is a typed brace (`int c = int{i};`, `^:cpp/expr`); pack expansions of member initializers (`T(args)...`); `decltype(auto)`; operator names (`operator/=`); a glued `>=` in a re-tokenized array bound; the closing `>>` token of `vt<T>>` in a text; nested namespaces' `inline` parts (`A::inline B::C`); abbreviated function templates (the invented parameters are not forms); members of anonymous unions and structs (`b.m_1`, not `b.<anon>.m_1`); `cv` qualifiers in one order (`const volatile`); a variadic `...` in a function type; `new T()` of a dependent type; `friend` texts; a destructor's `noexcept(false)` behind a `{` in a comment; `BindingDecl` nodes left to their `DecompositionDecl` |
| `tools/cxxdump` | version 7; the AST normal form drops `isReferenced` (CP60); a key clang writes twice (`UsingType`'s `type`) is written `type`, `type-2`; the cache is sharded in 256 subdirectories (see the slip below) |

### CX4b.2 Numbers

Same runs as CX4.3, all with objects (g++ and clang), after the last edit of the converter and printer:

| corpus | files | oracle rejects | read | text-free | l1 | l3 | l4 g++ (orig fails) | l4 clang (orig fails) | pass | known | failing |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| `test/cpp/samples/` | 8 | 0 | 8 | 6 | 8 | 8 | 7 (0) | 8 (0) | 7 | 1 | 0 |
| whim++ (`80c49bf5`) | 10 | 0 | 10 | 6 | 10 | 10 | 10 (0) | 10 (0) | **10** | 0 | 0 |
| `g++.dg`, every 10th file | 1,807 | 747 | 1,060 | 759 | 1,041 | 1,020 | 961 (34) | 1,039 (3) | **972** | 88 | **0** |
| libstdc++'s testsuite, every 40th file (CX5a) | 244 | 32 | 212 | 99 | 202 | 203 | 195 | 196 | **193** | 19 | 0 |

- **g++.dg**: 918 passing and 142 known became 972 and 88; the 88 open converter and printer cases (41 print rejected, 34 AST differs, 14
  forms differ, 1 objects differ) are now 41 (9 print rejected, 21 AST differs, 10 forms differ, 1 objects differ), and 47 are `gxx-objects-location` (CP48). No file fails.
- **Text forms** in the g++.dg sample: 2,300 in 357 files became 2,031 in 301 (the macro invocations, 1,354 before, stay text by CP41; the
  structured forms took partial specializations, placement `new`, structured bindings, dependent constructions and most explicit instantiations).
  What is text now, by kind: macro invocations about 1,350, overlapping declaration groups 189, `FunctionDecl` types 33, init-capture lambdas 30, explicit instantiations clang gives no declaration for 29, `EmptyDecl` 26.
- **The 88 cases**: 49 closed by the directive layer (`#define` under `#ifndef`, inside a function, `#pragma`), the macro extents, the typed
  braces, the pack expansions and the normal form; the rest are classed again by the first level that fails, in the known file
  (`test/repr/cpp/roundtrip-known.edn`, rewritten from the 1,807 files' results).
- **Speed**: the g++.dg sample 8.5 minutes (4 jobs); the 88 cases 9 seconds; the libstdc++ sample 3 minutes (4 jobs).
- **C's results unchanged**: `bin/repr-c roundtrip --corpus c23conf,tcc-tests2` (2 threads): c23conf 49 pass, tcc-tests2 85 pass and 3 known (no file under
  `arbace/repr/c/` changed).

### CX5a Libstdc++'s testsuite (the widening, first step)

`arbace.repr.cpp.ppcheck lists` makes the list (`libstdcxx-testsuite`, every 40th of 9,725 `.cc` files: 244); the round trip needs
`-I testsuite/util -I testsuite` added to the compilers' arguments (the list's fields 3 and 5: the DejaGnu harness' own `-I`, not
in `dg-options`; `testsuite_hooks.h` is 121 of the first 165 rejections). Of 244 files the oracle accepts 212 (32 rejected: library features
the host's libstdc++ or clang lacks, `mallinfo`, `<chrono>` time zones). The first pass gave 96 passing; **the macro texts' `#line`** (every
`VERIFY(...)` expands `__LINE__`) took it to 181, UsingType's repeated key and the instantiation names to 193. The 19 left are in the known
file: 9 print rejected (library spellings `using namespace std` hides, `ranges`, an `A` of a pstl test), 7 objects differ, 2 g++-location, 1 forms differ. The `:full` mode, the item-granular text, the header units and the other corpora (HotSpot, the old g++.dg directories,
libstdc++'s headers as sources) are CX5's: not started.

### CX4b.3 Checks

Run (all light): `bin/repr-cpp roundtrip` on the samples, whim++, the g++.dg sample and the libstdc++ sample (above); the 88 cases
in a loop of 9 seconds on stage 0; `go vet`/`gofmt`/`go test` of `tools/cxxdump` (five tests, one new: the repeated key); `bin/repr-c
roundtrip` on c23conf and tcc-tests2; `make check-makefile`. **Not run:** `bin/gate` in any form; any Arbace build; `bin/repr-cpp
pp-check` and `spec-check` (the lexer and the preprocessor did not change; §18.3's added list has no `clojure` block); the whole g++.dg
or libstdc++ testsuite (CX4.7); `host check`.

### CX4b.4 What is left

- Classes the 51 open g++.dg cases and the libstdc++ failures fall into: `__LINE__` inside an expression-level macro text (the `#line`
  works at statement and declaration level only), unnamed template parameters (`type-parameter-0-0` has no name to give back), a
  parenthesized type (`int (T::*)`), deduction guides and CTAD, conversion functions to a function reference, `nullptr_t` and other
  library spellings that `using namespace std` hides, a `friend` template header, `#embed`, GNU `__inline`/attribute order.
- Text forms left by kind (macro invocations about 1,350, overlapping declaration groups 189, `FunctionDecl` types 33, init-capture lambdas 30, explicit instantiations clang gives no declaration for 29, `EmptyDecl` 26): the macro invocations by CP41 (a); init-capture lambdas, `asm`, type traits, `EmptyDecl`.

### CX4b.5 Proposals (CP60-CP66)

- **CP60 `isReferenced` in the AST normal form.**
  - Options: (a) dropped from the normal form (built): it is clang's bookkeeping of lookups (an injected class name, a typedef used inside a
    text form), the references themselves (`DeclRefExpr`, `TemplateSpecializationType`) are compared; (b) kept, and the converter learns to
    reproduce it (write `S9` or `S9<T>` as the source wrote them, everywhere); (c) kept, the files classed.
  - *Recommended: (a).* 11 of the 34 `ast-differs` cases differed only there; (b) needs the written spelling of every injected class name.
- **CP61 `#line` before macro texts.**
  - Options: (a) `#line N` before a statement or declaration that is a macro invocation's text (built); (b) before every item and statement whose
    line differs from the print's (the printer tracks its line; exact for any `__LINE__`); (c) refuse files that use `__LINE__`; (d) `:full`
    (CX5) keeps every line where it was, so no `#line`.
  - *Recommended: (a) now, (d) at CX5:* (a) closed 7 g++.dg cases and took the libstdc++ sample from 96 to 181; `:full` makes it unnecessary.
- **CP62 Executed directives from a third clang pass.**
  - Options: (a) `clang -E -dD -dI` in the helper, `{:executed [...]}` (built; clang's answer, one process more, 0.1 s a file); (b) the shared
    preprocessor's layer (CX2/C3b's `:off`s, g++'s predefined macros: the same answer the host's g++ would give, 28 s for a first `<vector>`);
    (c) both, compared.
  - *Recommended: (a) while the oracle is clang; (b) at CX5* if a g++-judged level needs the other compiler's executed set.
- **CP63 A repeated key in clang's JSON.**
  - Options: (a) `type`, `type-2`, ... (built); (b) a vector of the values under the first key; (c) drop the later ones.
  - *Recommended: (a).* The values differ in meaning (`UsingType`: the type as written and the underlying one); both are kept, readable.
- **CP64 The cxxdump cache layout.**
  - Options: (a) 256 subdirectories by the key's first byte (built); (b) one SQLite or tar file; (c) a bound on entries with eviction.
  - *Recommended: (a) with (c)* when the cache passes 2 GB: a flat directory of 83,000 entries made ext4 refuse `rename` with ENOSPC on
    an 83% full disk with 18 GB free (see the slips).
- **CP65 Written types for injected class names (and unnamed template parameters).**
  - Options: (a) clang's printed type (built: `S9<T> &` for `S9 &`, `type-parameter-0-0` for an unnamed parameter); (b) the written text for a
    declaration's return and member-initializer types when the printed one is the injected class's template-id; (c) name unnamed parameters
    in the print (`T0`).
  - *Recommended: (b) and (c) at CX5:* (b) matters once `isReferenced` is kept (CP60 (b)); (c) changes the AST (a parameter gets a name), so it
    needs a normal form that drops the names of template parameters.
- **CP66 The libstdc++ testsuite's harness arguments.**
  - Options: (a) the list takes `-I testsuite/util -I testsuite` (built, by the caller for now); (b) `ppcheck lists` adds them for the corpus
    (the harness' `libstdc++-v3/testsuite/lib` knows more: `-D_GLIBCXX_ASSERT`, the `dg-options` variants); (c) the corpus' tests are read as
    DejaGnu runs.
  - *Recommended: (b).* The corpus entry owns its include paths, as g++.dg owns `-iquote`.

### CX4b.6 The heavy list (for a window after the tag)

CX4.7's list stands, with: the whole libstdc++ testsuite (9,725 files, about 2 hours at 4 jobs; the converter's process holds a corpus'
reads: shard by directory); `bin/repr-cpp roundtrip` of the g++.dg sample in the gate's light steps; `bin/gate` essential (the gate's key
includes `arbace/`).

### CX4b.7 Rule slips and notes

- **The disk**: the g++.dg run died at about 1,400 files with `rename ...: no space left on device` from cxxdump while `df` showed 18 GB free:
  ext4 refuses a rename in a directory with some 80,000 entries (the cache, 83,237 files). 46 files showed as failing; I deleted the cache (1.2 GB,
  regenerable), sharded it (CP64) and ran the sample again whole. The disk was 85% at the start; I removed `.tmp/sub`, the CX3 round trips'
  work directories and a 122 MB JSON dump I had left in `.tmp/cx/a.json` (the rule: no JSON dumps on disk outside the streaming filter) and kept the
  worktree's `.tmp` under 2 GB (cxxdump cache about 1 GB at the end).
- The wait for the other agent's processes: my own waiting shell held the worktree as its cwd, so the check saw itself; I read the process
  list by hand.
- No source was edited while a run that loaded it executed (the edits waited for the runs; the doc was written during runs).
- Threads: the C check ran with 2 threads beside nothing else; the libstdc++ list was made with 2 threads while it ran. No run beyond 4.
- The scratch the numbers come from: `.tmp/cx/g-dg`, `g-samples`, `g-wpp`, `g-lt` (`results.tsv`, `read.tsv`, `summary.edn`), `.tmp/cx/l88.tsv` (the 88),
  `.tmp/cx/lt.list` (the libstdc++ list); copy them before the worktree is removed.
