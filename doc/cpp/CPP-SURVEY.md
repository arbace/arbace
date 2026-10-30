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
