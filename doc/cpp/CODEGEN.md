# C++ without the host's clang: Sema and a code generator (cpp-codegen)

Status: survey, plan and a first slice, on the branch `cpp-codegen` from `cpp-sema` (2026-11-05), for the user's
acceptance. Words: **the native parser** is CX11's (`arbace.repr.cpp.parse`, [NATIVE.md](NATIVE.md)), whose tree is
clang's AST as data; **clang's route** is cppi as built (CX6-CX7, [LLVM.md](LLVM.md)): `clang++ -O0 -S -emit-llvm`, the IR
read as LLVM forms by repr/llvm, lowered to C forms by `arbace.cpp.lower`, compiled by gcc `-O0` with the lowered runtime
and run; **our route** replaces its first step: the native parser, a semantic analysis of our own (`arbace.cpp.sema`)
and a code generator (`arbace.cpp.codegen`) write the LLVM forms, and the rest of cppi is unchanged. The judge is clang's
route on the same program: the same status and standard output (§5).

Contents: 1 What clang does that we need · 2 The decisions the design rests on · 3 Sema as built · 4 CodeGen as built ·
5 The check and the numbers · 6 The staged plan · 7 Proposed · 8 Sources.

## 1. What clang's Sema and CodeGen do that we need

Clang's pipeline for a C++ unit is Parse → Sema (the AST with every implicit node) → CodeGen (LLVM IR). The native
parser stands for Parse and the syntactic AST; NATIVE.md §8 ported the few Sema facts the AST's *shape* needs
(InitListChecker's semantic form, CTAD, defaulted members' exception specifications). Running a program needs all of
Sema's meaning and all of CodeGen. By clang's source files (llvm-project at `llvmorg-23.1.4`, the oracle's version,
pinned in `test/cpp/corpora.edn`), what a code generator of our own has to do:

| area | clang | what it decides | needed for |
|---|---|---|---|
| name lookup | `SemaLookup.cpp` (LookupName, LookupQualifiedName, ADL's `ArgumentDependentLookup`) | which declaration a name means: scopes outward, using-directives, class members through the bases, hiding, the namespaces associated with the arguments | every name |
| types of expressions | `SemaExpr.cpp`, `SemaExprCXX.cpp`, `SemaExprMember.cpp` | each expression's type and value category, the implicit casts (`ImplicitCastExpr`: lvalue-to-rvalue, decay, integral promotion and conversion, floating, to bool, null to pointer, derived-to-base) | every expression |
| overload resolution | `SemaOverload.cpp` (AddOverloadCandidate, CompareImplicitConversionSequences, isBetterOverloadCandidate) | the best viable function by the implicit conversion sequences' ranks and their tie-breaks; the operators' candidates (members, non-members, built-ins) | calls, operators, constructors |
| initialization | `SemaInit.cpp` (InitializationSequence) | default, value, zero, copy, direct, list and aggregate initialization, reference binding and the temporaries it materializes, copy elision | every declaration, argument, return |
| implicit members | `SemaDeclCXX.cpp` (DeclareImplicitDefaultConstructor, ...Copy..., ...Destructor; the triviality of each) | which special members exist, whether trivial (no call: bytes copied, nothing run) or defined by the compiler | every class |
| virtual functions | `SemaDeclCXX.cpp` (overriders), `VTableBuilder.cpp` | which functions override which, the vtable's slots and their order | dynamic classes |
| templates | `SemaTemplateDeduction.cpp`, `SemaTemplateInstantiate.cpp`, `SemaTemplateInstantiateDecl.cpp`, `SemaConcept.cpp` | deduction from calls, substitution (SFINAE), the instantiation of class and function templates, partial ordering, constraints | the library (almost every testsuite file) |
| constant evaluation | `ExprConstant.cpp` | constant expressions: array bounds, enumerators, case labels, `constexpr`, `if constexpr`, static initialization | templates' arguments, `static_assert`, globals |
| class layout | `RecordLayoutBuilder.cpp` (Itanium) | offsets of bases and fields, the vptr, the primary base, empty bases, tail padding, bit-fields | every object |
| mangling | `ItaniumMangle.cpp` | the linker's names, the substitutions | every external name; linking with the lowered runtime |
| calls and the ABI | `CGCall.cpp`, `Targets/X86.cpp` (X86_64ABIInfo::classify) | how arguments and results pass: SysV's classes, `sret`, `byval`, indirect non-trivial classes | calls to code not ours |
| classes | `CGClass.cpp`, `CGCXX.cpp`, `ItaniumCXXABI.cpp` | constructors (C1/C2), destructors (D0/D1/D2), vptr stores, base and member initialization and destruction order, array cookies | classes |
| vtables, RTTI | `CGVTables.cpp`, `ItaniumCXXABI.cpp` (BuildTypeInfo) | vtables, type_info objects (`__class_type_info`, `__si_`, `__vmi_`), key functions and linkage | dynamic classes, exceptions, `dynamic_cast` |
| cleanups | `CGCleanup.cpp`, `CGDecl.cpp` | destructors at scope exits and on every jump out, temporaries at the full-expression's end, conditional cleanups, lifetime extension | classes with destructors |
| exceptions | `CGException.cpp` | `invoke`, landing pads, catch clauses by type_info, `__cxa_*` calls | `throw`, `try`; the lowering's pending flag (LLVM.md §8) runs them unchanged |
| statics | `CGDeclCXX.cpp` | dynamic initialization in `_GLOBAL__sub_I_`, `__cxa_atexit`, static locals' guards | globals |

What the libstdc++ headers demand on top: templates everywhere (class and function templates, partial
specializations, default template arguments, variadic packs, SFINAE through `enable_if` and `__and_`, concepts and
`requires` in C++20 mode), `constexpr` evaluation (type traits' `value`s, `if constexpr`), aliases and `decltype`,
inline namespaces (`std::__cxx11`) with their mangling (`St`, `NSt7__cxx1112basic_string...`, the abi tags `B5cxx11`),
attributes that change code (`always_inline`, `noexcept` on calls), the builtins the headers call
(`__builtin_memcpy`, `__builtin_is_constant_evaluated`, `__is_trivially_copyable` and the other type-trait
intrinsics), allocators, `operator new` overloads with alignment. The testsuite's run tests (`bin/cppi list
libstdcxx`, the recorded every-15th list, 234 files): 229 include a C++ library header (`<sstream>` 37, `<algorithm>`
33, `<string>` 27, `<locale>` 19, `<memory>` 17, `<vector>` 16, `<random>` 16, ...), 34 declare templates of their own,
202 name `std::`. None runs without templates.

## 2. The decisions the design rests on

1. **The output is LLVM forms**, the forms repr/llvm reads clang's IR into (LLVM.md §1-§5), not the lowering's C forms
   directly. The forms are already the contract between the oracle and the lowering: our forms go through the same
   lowering, the same runtime, the same exceptions' scheme, so a difference between the routes is the code generator's.
   They also print as IR (`arbace.repr.llvm.print`) that clang compiles: LLVM's verifier checks every function our route
   writes (a malformed phi or a branch to nowhere is a build error, §5's native level), and the IR can be diffed against
   clang's. Proposed as P1 (§7).
2. **The input is the native parser's tree**, clang's AST as data, not the C++ forms: the tree is uniform (`:kind`,
   `:inner`) and carries what the converter turned into text (operators, value categories of literals), and the native
   parser's scopes hold the headers' declarations. The C++ forms are the alternative input (P2).
3. **Sema is ours, in clang's terms.** The analysis follows the standard's rules and clang's structure (the functions
   named in §1 are the models; their names are kept where the parts correspond), written from the standard's text and
   the Itanium ABI's (itanium-cxx-abi.github.io/cxx-abi/abi.html), clang's output the oracle. No code of clang or
   libstdc++ is ported in this slice (clang and LLVM are Apache-2.0 with the LLVM exception, porting allowed and to be
   recorded when it happens, as the later stages may; libstdc++ is GPL with the runtime exception: its headers are read
   as input, never ported).
4. **Memory by byte offsets.** Every object is an `[N x i8]` alloca or global of its size and alignment, its fields and
   bases at the layout's offsets (`getelementptr i8`): no LLVM struct types, so the IR needs no type names and the layout
   is the one place offsets come from (`arbace.cpp.abi/layout`, Itanium's rules). A constant initializer is a packed
   struct of its leaves with byte padding. The lowering takes these as it takes clang's.
5. **Names are clang's.** Every function and variable the code generator defines has the Itanium mangled name clang
   gives it (the check requires every defined name to be one clang's IR defines, §5), so that our objects link with the
   lowered runtime and later with clang's objects. What differs from clang's IR is free: value names, block order,
   attributes (none are written: the lowering then tests the pending flag after every call, correct and slower).
6. **The calling convention is clang's for scalars and references; a class passes by address** (the caller's copy for
   an argument, `sret` for a result) whatever its size. Inside one program that is consistent; against code compiled by
   clang a trivially copyable class of up to 16 bytes passes in registers (SysV's classes). The slice calls nothing of
   that kind (the C library takes scalars and pointers). P3.

## 3. Sema as built (`arbace.cpp.sema`, `arbace.cpp.abi`)

- **Types** are maps: `{:k :b :n :int}` for the builtins, `:ptr`, `:ref`, `:rref`, `:arr`, `:fn`, `:class` and `:enum` by
  id, with `:c`/`:v` qualifiers. A declaration's type is clang's printed `qualType` read by `arbace.repr.cpp.types`'
  type parser and resolved in the declaration's scope. Classes and enumerations live in a registry by id
  (`arbace.cpp.abi`), with their scope, fields, bases, constructors, destructor, virtual functions, vtable and layout.
- **Declarations**: one pass over the tree enters namespaces (reopened, nested, unnamed: internal linkage and the
  `_GLOBAL__N_1` path, aliases, using-declarations and -directives), classes (fields with their access and default
  member initializers, bases, methods, static members, nested types, the injected-class-name), enumerations (scoped,
  fixed underlying types, enumerators by constant evaluation), functions (out-of-line definitions joined to their
  declarations by name, qualifier and parameter types; `extern "C"`; `static`), variables. The tree drops a
  definition's qualifier (`int ns::P::get()`) and a namespace alias's target; they are read from the main file's text at
  the node's range, as the converter reads them. A namespace-scope name is visible only after its declaration (the
  token position), a member's body sees the whole class.
- **The headers' declarations**: the native parser skims the headers into its scopes; the code generator takes a C
  function (its entity now keeps its printed type and its `extern "C"`, a small change to the parser, §8) and a typedef of
  a builtin or pointer type from there. `#include <cstdio>` and `printf` work; a C++ library declaration is refused.
- **Classes completed** (`finish-class!`): the overriders (a method of a base's virtual function's name and parameters
  is virtual), the implicit destructor, default constructor, copy constructor and copy assignment, each trivial or not
  ([class.dtor]/8, [class.default.ctor]/3, [class.copy.ctor]/11), the vtable (the primary base's slots overridden, then the
  new virtual functions, a destructor two slots: D1, D0), the layout.
- **Expressions** are typed nodes `{:op ... :ty T :cat :lv|:xv|:pr}` with the conversions explicit (`:load` for
  lvalue-to-rvalue, `:decay`, `:cast` with its kind, `:base-lv` for derived-to-base), the usual arithmetic conversions and
  the integral promotions, pointer arithmetic, the built-in and the overloaded operators (members of the first operand's
  class, non-members by unqualified lookup and the class arguments' namespaces), member access through the bases,
  qualified calls without virtual dispatch, `new`/`delete`, `sizeof`, casts (C-style, `static_cast`, `reinterpret_cast`,
  `const_cast`, functional), the address of an overloaded function by the target type ([over.over]), constant variables
  folded where they are not odr-used.
- **Overload resolution**: viable functions by arity (default arguments, ellipsis), each argument's implicit conversion
  sequence ranked (exact, promotion, conversion, user-defined by a converting constructor, ellipsis), the reference
  bindings' qualification as a sub-rank, the implicit object parameter's constness; the best function is the one better
  than every other.
- **Initialization** is written for the code generator as an initializer: none, zero, a value, a constructor call, a
  trivial copy, an element list (arrays and aggregates, the rest value-initialized or by default member initializers),
  a string into a char array, a reference bound to an object or to a materialized temporary. Copy-initialization from a
  prvalue of the class constructs in place.
- **Statements**, with range `for` over arrays desugared, condition variables, `switch` labels by constant evaluation,
  and the named return value of a function every return statement of which returns the same local ([class.copy.elision]
  as clang's computeNRVO decides it for one variable): a returned local is an xvalue first.

## 4. CodeGen as built (`arbace.cpp.codegen`)

- Every local and parameter in an alloca of the entry block (a class parameter is the caller's copy, by address), loads
  and stores (bool `i8` in memory, `i1` as a value), a return slot and one `return` block.
- **Constructors**: C2 (bases, the vptr, members by their mem-initializers, default member initializers or default
  initialization, then the body), delegation; C1 an alias of C2 for a strong definition and C2 called everywhere (no
  virtual bases: clang's `-mconstructor-aliases` behaviour). **Destructors**: D2 (the vptr, the body, then members and
  bases in reverse as cleanups, so a `return` in the body runs them), D0 calling D2 and `operator delete(void*, size_t)`,
  D1 an alias.
- **Cleanups**: a stack of the destructors to run; a scope's exit runs its own, `break`/`continue` those of the loop's
  scopes, `return` all; a full-expression's temporaries are destroyed at its end in reverse, a temporary made in a
  conditional branch (`?:`, `&&`, `||`) behind a flag set where it is made; a temporary bound to a local reference lives
  as long as the reference; `goto` across a destructor is refused.
- **Virtual calls** load the vptr and the slot; vtables `[N x ptr]` (offset to top, type_info, the slots), the address
  point at 2; type_info objects of the three kinds (`__class_type_info`, `__si_class_type_info`,
  `__vmi_class_type_info` with the bases' offset flags); pure virtual slots `__cxa_pure_virtual`.
- **new/delete**: `_Znwm`/`_Znam`, the array cookie (the count before the elements) when the element has a non-trivial
  destructor, elements initialized in a loop; `delete` through a virtual destructor calls D0; `_ZdlPvm`, `_ZdaPv`, `_ZdaPvm`.
- **Globals**: a constant initializer as a packed constant (internal ones written only when used, as clang does), else
  zero and a dynamic initialization in `_GLOBAL__sub_I_FILE` (`llvm.global_ctors`), a non-trivial destructor registered
  with `__cxa_atexit`; static locals constant or behind their guard (`__cxa_guard_acquire`/`release`, the acquire load of
  the guard byte), named `_ZZ...E` with their discriminators.

Not in the slice (refused with a class, `{:codegen/refused CLASS}`, written to `gen.refused`): templates, exceptions
(`throw`, `try`), lambdas, `<iostream>` and every C++ library header, bit-fields, unions, multiple and virtual
inheritance, conversion functions, member pointers, `long double` constants, a class through `...`, `goto` across
destructors, a `while` condition's declaration.

## 5. The check and the numbers

`bin/cppi codegen FILE OUT` compiles one file by our route and runs it; `bin/cppi codegen-check [LIST] WORK [--runtime
DIR]` takes each sample of `test/cpp/codegen/` by both routes and judges (arbace.cpp.codegen's `judge`, against
`test/cpp/codegen/known.edn`, empty):

1. **run**: our forms lowered, compiled by gcc `-O0` with the lowered runtime and run: the status and the standard output
   equal to clang's IR's, lowered the same way and linked with the same runtime (`differs`);
2. **native**: our IR printed, built by `clang++` (LLVM's verifier) with the native libraries and run: the same
   (`native-differs`; a build failure counts);
3. **names**: every global name our IR defines is one clang's defines (`names-differ`), the string literals and the
   global constructor's function aside.

The runtime is the lowered one `bin/cppi runtime-build` makes (CX7); the check does not rebuild it and does not require
it at the checkout's key, since both routes link the same archives.

The samples (38; `test/cpp/codegen/*.cpp`, each with a header comment saying what it exercises): arithmetic, control
flow, overloads, pointers, references, aggregates, methods, constructors, destructors, inheritance, virtual functions,
new/delete, namespaces, operators, enumerations, globals, strings, a linked list, a stack (rule of three), shapes,
sorting, bits, a matrix class, a bank, recursion, conversions, temporaries, static members, the exit status, a ring
buffer, shadowing, characters, virtual calls during construction and destruction, the Itanium layout, arrays of
objects, const overloads with a nested class, default arguments, static locals.

| check (2026-11-05, light, 4 jobs) | result |
|---|---|
| `bin/cppi codegen-check` over `test/cpp/codegen` | 38 of 38 pass all three levels; generate and lower about 33 s, gcc, clang and runs 4 s |
| the existing samples (`test/cpp/samples`, `test/cpp/runtime/samples`; 16 files, our route alone) | 0 of 16: each refused in its first class beyond the slice, below (4 min 53 s: the headers' skim dominates) |
| the libstdc++ testsuite's recorded list (234 files) | not run: 229 need C++ library headers and templates (§1) |
| `bin/repr-cpp native-check samples` after the parser's change | 9 of 9 the same, 68 of 68 items (as on `cpp-sema`) |

The existing samples' first refusals, the work G2-G4 take on: templates (`auto.cpp` a function template, `classes.cpp`
and `p1-first.cpp` class templates, `atomics.cpp` and `io.cpp` `std::atomic<int>` and `std::vector<int>`), the C++ library
(`coroutine.cpp`, `ctor-throw.cpp`, `hello.cpp`, `thread-throw.cpp`, `threads.cpp`: `std::` names from `<iostream>` and the
others), bit-fields (`bitfields.cpp`, `decls.cpp`, `layouts.cpp`), exceptions (`uncaught.cpp`'s `throw`), a block-scope
function declaration (`vexing.cpp`), a construct the native parser declines (`library.cpp`).

## 6. The staged plan

Each stage ends with its samples passing codegen-check and its coverage measured on the recorded lists of cppi
(`test/cpp/lower-results.edn`: `samples`, `libstdcxx-every-15th`), the measure being the files whose run by our route
equals clang's route. Estimates in sessions of this size (a slice like this one: about one day's agent).

| stage | what | samples / coverage target | estimate |
|---|---|---|---|
| **G1** (this branch) | C-like C++ and classes: §3, §4 | 38 samples; 0 of the recorded lists | done |
| **G2** | the rest of the core language without templates: exceptions (`throw`, `try`/`catch` by type_info, `invoke` and landing pads as clang writes them, cleanups on the unwind path, `noexcept`), lambdas (closure classes), bit-fields, unions, multiple and virtual inheritance (thunks, VTTs, construction vtables), conversion functions, member pointers, `dynamic_cast` and `typeid`, `long double`, SysV's argument classes (P3), `constexpr` functions evaluated | 60 samples; the recorded `samples` list's files that use no library header beyond C's | 2-3 |
| **G3** | templates: class and function templates instantiated on use (deduction from calls, explicit arguments, defaults, partial specializations and their partial ordering, packs, SFINAE, `if constexpr`, concepts), the substitution of the native trees' dependent nodes (`UnresolvedLookupExpr`, `CXXDependentScopeMemberExpr`), template mangling (`I...E`, `T_`, the substitutions of template prefixes) | 80 samples with templates of their own; testsuite files with no library header | 4-6 |
| **G4** | the headers: the native parser parses the headers' bodies (not skims) when a definition is instantiated or inline; libstdc++'s demands of §1 (type traits' builtins, inline namespaces and abi tags, `__builtin_*`); `<cstdio>`, `<cstring>`, `<utility>`, `<memory>`, `<vector>`, `<string>`, `<algorithm>` first (P5) | the recorded `samples` list (16) by our route; 30 % of `libstdcxx-every-15th` | 6-10 |
| **G5** | `<iostream>`, `<sstream>`, `<locale>`, `<map>`, `<unordered_map>`, `<functional>`, `<random>`, `<regex>`, `<filesystem>`; the runtime's own units by our route (libsupc++ and libstdc++-v3/src compiled without clang: the lowered runtime clang-free) | 80 % of `libstdcxx-every-15th`; the runtime's units | 8-12 |
| **G6** | the oracle retired from the eval: cppi's default route ours, clang's the reference only (as javac on Arbace became repr/java's) | the CX9 corpora's run tests by our route | 2 |

## 7. Proposed

**P1. The code generator's output.** (a) LLVM forms, lowered by the existing lowering (as built): one contract with
clang's route, the IR printable and checked by LLVM's verifier and diffable against clang's, the lowering's exceptions'
scheme reused; costs an IR's indirection (the lowering's C is a little more verbose than direct C would be). (b) C forms
directly: one step less, but a second lowering of C++ semantics to keep in step with the first, no verifier, no
comparison with clang at the IR. (c) Both: (a) now, (b) later for speed. **Recommended: (a).**

**P2. The input.** (a) The native parser's tree (as built): uniform, the headers' scopes at hand; the tree's shape is
clang's and checked against it (native-check). (b) The C++ forms (SPEC.md): Arbace's representation, so that C++ forms
written by hand would run; costs a second reading of every construct the converter wrote as text (`cpp/text`) and of the
headers. (c) (a) now, (b) in G6 when the forms are complete enough. **Recommended: (c).**

**P3. ABI fidelity of calls.** (a) Clang's for scalars and references, classes by address (as built): consistent inside
a program, wrong against clang-compiled code for small trivially copyable classes. (b) SysV's classification as clang's
`X86_64ABIInfo::classify` (registers for up to two eightbytes, `byval`, `sret`, non-trivial classes indirect): calls to the
lowered runtime and to clang's objects right (needed by G4: `std::pair` returned, `std::string_view` passed); about a
day; the lowering already takes the coerced forms clang writes (`<2 x float>` aside, its `vector` class). **Recommended:
(b) in G2.**

**P4. Attributes.** (a) None (as built): the lowering tests the pending flag after every call. (b) `nounwind` on what
cannot throw (C functions, `noexcept`, destructors), as clang writes it: fewer tests, the lowered C closer to clang's
route's. **Recommended: (b) with G2's exceptions**, when the difference starts to matter.

**P5. The order of the headers.** (a) By the recorded lists' demand (§1's counts: `<sstream>`, `<algorithm>`,
`<string>`, `<locale>`, `<memory>`, `<vector>`), (b) by dependency (`<cstdio>`, `<cstring>`, `<utility>`, `<new>`,
`<type_traits>`, `<memory>`, `<vector>`, `<string>`, `<algorithm>`, then the streams), each a sample that uses it.
**Recommended: (b)**: each header brings the next's machinery, and the streams need nearly all of it.

**P6. Clang's code ported or not.** (a) Written from the standard and the Itanium ABI, clang the oracle (as built). (b)
Ported parts of clang's Sema and CodeGen where the rules are long and precise (SemaOverload's ranking,
ItaniumMangle, RecordLayoutBuilder, X86_64ABIInfo::classify, the template instantiator), Apache-2.0 with the LLVM
exception, recorded in LICENSE.md and doc/ARBACE.md; costs reading C++ of clang's size, gains its corner cases. **Recommended:
(b) for G3's templates and the ABI's classification, (a) elsewhere.**

**P7. The check in the gate.** (a) Out of the gate (as built: repr/cpp's crop is out of `gate_key` and no gate step runs
it). (b) `bin/cppi codegen-check` as an essential step once C++'s crop has a row in `bin/reap` (light: about 40 s).
**Recommended: (b) at the crop's REAP row.**

## 8. Sources

- The standard: ISO/IEC 14882:2024 (C++23), clauses as cited ([basic.lookup], [over.match], [over.ics.rank],
  [dcl.init], [class.copy.elision], [class.dtor], [class.default.ctor], [class.copy.ctor]).
- The Itanium C++ ABI, itanium-cxx-abi.github.io/cxx-abi/abi.html: §2.4 (layout), §2.5 (vtables), §2.9.5 (type_info),
  §3.3.2 (guards), §5.1 (mangling).
- clang at `llvmorg-23.1.4` (https://github.com/llvm/llvm-project, the oracle's version): the files of §1 named as the
  models; nothing ported in this slice. Its output (`clang++ -O0 -S -emit-llvm` on the samples) is the oracle.
- The native parser's change: `arbace/repr/cpp/parse.ae` keeps a function entity's printed type (`:qt`) and its C linkage
  (`:clink`, the dynamic `*linkage*` bound in `parse-linkage`); the tree and the forms are unchanged.

## Decided (2026-11-05, the user)

P1-P7, each as recommended: LLVM forms as the output; the native tree as input now, C++ forms at G6; SysV's classification in G2; nounwind with G2; the headers in dependency order; clang's code (Apache-2.0 with the LLVM exception) ported for templates and the ABI classification only, recorded in LICENSE.md and doc/ARBACE.md; codegen-check essential once C++ has its row.
