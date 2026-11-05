# C++ without the host's clang: Sema and a code generator (cpp-codegen)

Status: survey, plan, the first slice (G1, branch `cpp-codegen`) and the second (G2, branch `cpp-codegen2`, §4a,
2026-11-05), for the user's acceptance. Words: **the native parser** is CX11's (`arbace.repr.cpp.parse`, [NATIVE.md](NATIVE.md)), whose tree is
clang's AST as data; **clang's route** is cppi as built (CX6-CX7, [LLVM.md](LLVM.md)): `clang++ -O0 -S -emit-llvm`, the IR
read as LLVM forms by repr/llvm, lowered to C forms by `arbace.cpp.lower`, compiled by gcc `-O0` with the lowered runtime
and run; **our route** replaces its first step: the native parser, a semantic analysis of our own (`arbace.cpp.sema`)
and a code generator (`arbace.cpp.codegen`) write the LLVM forms, and the rest of cppi is unchanged. The judge is clang's
route on the same program: the same status and standard output (§5).

Contents: 1 What clang does that we need · 2 The decisions the design rests on · 3 Sema as built · 4 CodeGen as built ·
4a G2 as built · 5 The check and the numbers · 6 The staged plan · 7 Proposed · 8 Sources.

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
6. **The calling convention is clang's**: scalars and references as clang passes them; since G2 (P3 (b)) a class by
   the SysV psABI's classification (§4a), so that our functions and clang's call each other.

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

Not in G1 (refused with a class, `{:codegen/refused CLASS}`, written to `gen.refused`): templates, exceptions
(`throw`, `try`), lambdas, `<iostream>` and every C++ library header, bit-fields, unions, multiple and virtual
inheritance, conversion functions, member pointers, `long double` constants, a class through `...`, `goto` across
destructors, a `while` condition's declaration. G2 (§4a) took on all of these but templates, the C++ library headers, a
class through `...`, `goto` across destructors and a `while` condition's declaration.

## 4a. G2 as built (branch `cpp-codegen2`)

Everything below is checked by `bin/cppi codegen-check` (§5) against clang's route; nothing of clang or libstdc++ is
ported (§8): the rules are the standard's, the Itanium ABI's and the SysV psABI's, clang's functions the models whose
names are kept, clang's IR the oracle.

- **Exceptions** (`CGException.cpp`, `CGCleanup.cpp` as models). The cleanup stack of §4 also holds `{:try handlers}`,
  `{:end-catch}`, `{:eh-only}` (a base or member a constructor has built: destroyed only on the unwind path),
  `{:delete}` (new's storage while its initializer runs) and `{:terminate}` (the bottom of a noexcept function or a
  destructor). A call that may unwind is an `invoke` when the stack holds a cleanup or a handler, else a plain `call`
  (the lowering's pending flag returns it, LLVM.md §8); the landing pad of a stack state is written once, out of line:
  `landingpad` with the enclosing handlers' type_infos innermost first up to a catch-all and `cleanup` when a cleanup
  runs, the exception and selector stored, the cleanups run innermost first, each try's handlers dispatched by
  `llvm.eh.typeid.for.p0`, `resume` past the outermost. A handler begins with `__cxa_begin_catch` and binds its variable
  (a reference to the adjusted object, a pointer's value, a copy), runs under `{:end-catch}`. `throw` allocates with
  `__cxa_allocate_exception`, constructs the object (copy-initialization), calls `__cxa_throw` with the type_info and the
  complete destructor; `throw;` calls `__cxa_rethrow`. A noexcept function's or a destructor's calls unwind into a pad
  that calls `__clang_call_terminate` (written as clang writes it). type_infos of fundamental types and pointers to them
  are the runtime's; of classes, pointers to classes and enumerations written here. P4 (b): `nounwind` on noexcept
  functions, destructors, the C library functions clang knows as nothrow builtins (`memcpy`, `strcmp`, ...) and the
  runtime's nothrow entry points, at their declarations, definitions and calls.
- **Lambdas** ([expr.prim.lambda]): a local closure class, a field per capture in clang's order (explicit, init-captures,
  then implicit at first use), by value (copy-initialized) or by reference, `this`, `[=]`, `[&]`, `mutable`, return
  types deduced from the first `return`; a captureless lambda's conversion to a function pointer and its static
  `__invoke`; the names clang gives: `$_N` numbered per enclosing function in the order the code generator first names
  them (clang numbers them so), `Ul<params>E<n>_` in an inline function's or a closure's body. Local entities are
  mangled as local names (`Z<encoding>E`, a constructor's under its C1). Generic lambdas are refused (G3).
- **Bit-fields and unions**: the SysV layout (a bit-field never crosses its type's alignment, zero-width and unnamed
  ones), loads and stores of the storage unit with masks and shifts (signed extraction by shifts), bit-fields in
  aggregates, constructors and copies; unions at offset 0, their first member aggregate-initialized; anonymous unions
  and structs in classes and blocks (their members named in the enclosing scope), mem-initializers of their members,
  unnamed classes and enumerations as members' types.
- **Conversion functions** ([class.conv.fct]): user-defined conversions to non-class types in initialization, in
  arguments (rank user-defined), in contextual conversions to bool (explicit ones too), in built-in operators,
  subscripts, switches and compound assignments, by casts; a base's inherited. C++20's rewritten `!=` (as `!(a == b)`).
- **Multiple inheritance**: vtable groups (the primary vtable, a secondary one per dynamic non-virtual base that is not
  primary), this-adjusting thunks (`_ZThn16_...`, D1 and D0 thunks), every vptr stored, conversions with offsets (null
  kept null), member lookup through several bases with dominance.
- **Virtual inheritance** (Itanium §2.4-§2.6; clang's `RecordLayoutBuilder`, `ItaniumVTableBuilder`, `VTTBuilder`,
  `CGClass.cpp` as models): the layout with the non-virtual size, virtual bases after the non-virtual part in
  inheritance graph order, nearly empty virtual primary bases, indirect primary bases shared; vtables with vcall and
  vbase offsets, virtual thunks (`_ZTv0_n24_...`), the slots of a virtual primary base a layout does not share left
  unused (clang's IsOverriderUsed), construction vtable groups (`_ZTC1D16_1C`) and VTTs (`_ZTT1D`, §2.6.2's order);
  complete-object (C1, D1) and base-object (C2, D2, taking the VTT) constructors and destructors written apart, the
  virtual bases initialized by the complete object's constructor in clang's order and destroyed by its destructor in
  reverse, the vptrs of a base-object constructor or destructor set from the VTT (a virtual base's address from the
  vtable's vbase offset); an abstract class's complete destructor traps, as clang's; the type_info flags for virtual and
  repeated bases.
- **Pointers to members** (Itanium §2.3): data members as offsets (null -1), member functions as {ptr, adj} (a virtual
  one's ptr 1 + its vtable offset), `.*` and `->*`, calls through them (the virtual branch through the vtable), null,
  comparisons, conversions from a base's to a derived class's, the `M` mangling.
- **RTTI**: `typeid` of a polymorphic glvalue (the vtable's type_info), of a type, of another expression; `std::type_info`
  declared by a prelude of our own as the ABI lays it out and compares it (§2.9.4; `name`, `==`, `before`), in place of
  `<typeinfo>`'s class whose bodies the native parser skims; `dynamic_cast` down, across and to `void*` (by the offset to
  top) through `__dynamic_cast` with clang's hints, a failing reference cast by `__cxa_bad_cast`. `St` in the mangling.
- **SysV x86-64 classification** (P3 (b); the psABI §3.2.3 in the structure of clang's `X86_64ABIInfo::classify`, its
  Lo/Hi eightbytes, merge and post-merge): a trivially copyable class of up to 16 bytes in one or two eightbytes
  (INTEGER as i8-i64 by its data's extent, SSE as float or double; clang's `<2 x float>` written as double and its i24 as
  i32, the same registers), as separate arguments and as a return value of one type or a struct of two; an empty class
  not passed; a MEMORY class `byval` (as is one for which the six integer or eight SSE registers no longer suffice); a
  non-trivial class by the address of the caller's copy, its result by `sret`. Checked by hand against clang-compiled
  callers (`sysvabi.cpp`'s functions compiled by our route, its `main` by clang, linked: the same output).
- **Constant evaluation** (`ExprConstant.cpp` as the model): constexpr functions with parameters, locals, conditions,
  loops and assignments evaluated where clang folds them -- globals' initializers (constant initialization), constexpr
  and const integral locals, enumerators, case labels; called at run time elsewhere. Globals of classes whose implicit
  default constructor only stores constants are constant objects with their vptrs (clang's), a destructor registered
  all the same; destructors with trivial bodies leave the vptrs alone (clang's CanSkipVTablePointerInitialization).
- **`long double`**: constants exact to the bit from the literal's decimal (LLVM 23's `f0x` spelling), x87 arithmetic.
  The lowering (clang's route too) now takes clang's decimal x86_fp80 constants as long double literals: before, a
  constant without a double (`1e-4000L`) became 0 or inf through a double (`arbace.cpp.lower/fp-lit`).
- Smaller: block-scope function declarations; `std::` names only the headers declare (`std::printf`); the headers' C
  variables (`stdout`) and opaque structs (`FILE`); the libc builtins (`__builtin_memcpy`, ...); attributes in classes;
  `auto` locals deduced; internal functions written only when used, as clang defers them; deleting destructors written
  when a vtable refers to them.

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

The samples (82; `test/cpp/codegen/*.cpp`, each with a header comment saying what it exercises). G1's 38: arithmetic,
control flow, overloads, pointers, references, aggregates, methods, constructors, destructors, inheritance, virtual
functions, new/delete, namespaces, operators, enumerations, globals, strings, a linked list, a stack (rule of three),
shapes, sorting, bits, a matrix class, a bank, recursion, conversions, temporaries, static members, the exit status, a
ring buffer, shadowing, characters, virtual calls during construction and destruction, the Itanium layout, arrays of
objects, const overloads with a nested class, default arguments, static locals. G2's 44: exceptions (`eh-basic`,
`eh-cleanup`, `eh-rethrow`, `eh-hierarchy`, `eh-ctor`, `eh-noexcept`, `eh-uncaught`, `eh-temps`, `eh-stack`,
`eh-arrays`, `eh-nested-try`, `eh-dtor-order`, `eh-lambda`, `eh-terminate`, `eh-rethrow-types`), lambdas (`lambdas`,
`lambda-capture`, `lambda-stateful`, `lambda-members`), bit-fields and unions (`bitfields2`, `bits-flags`, `unions`,
`union-variant`), conversion functions (`convfns`, `convfns-ops`), multiple inheritance (`multiple`, `diamondless`,
`mi-casts`, `covariant`), virtual inheritance (`virtual-bases`, `virtual-interface`, `virtual-deep`,
`virtual-construct`, `virtual-eh`), pointers to members (`memberptrs`, `memberptr-dispatch`), RTTI (`rtti`,
`rtti-cross`), the SysV classification (`sysvabi`, `sysv-mixed`), constant evaluation (`constexpr`), `long double`
(`longdouble`), and two programs over all of it (`inventory`, `mixed-features`).

| check (2026-11-05, light, 4 jobs) | result |
|---|---|
| `bin/cppi codegen-check` over `test/cpp/codegen` | 38 of 38 pass all three levels; generate and lower about 33 s, gcc, clang and runs 4 s |
| the existing samples (`test/cpp/samples`, `test/cpp/runtime/samples`; 16 files, our route alone) | 0 of 16: each refused in its first class beyond the slice, below (4 min 53 s: the headers' skim dominates) |
| the libstdc++ testsuite's recorded list (234 files) | not run: 229 need C++ library headers and templates (§1) |
| `bin/repr-cpp native-check samples` after the parser's change | 9 of 9 the same, 68 of 68 items (as on `cpp-sema`) |
| G2 (`cpp-codegen2`, 2026-11-05, light, 4 jobs): `bin/cppi codegen-check` over `test/cpp/codegen` | 82 of 82 pass all three levels (`known.edn` empty); generate and lower 70-85 s, gcc, clang and runs 6 s |
| G2: `bin/repr-cpp native-check samples` after the parser's change (variables' `:clink`) | 9 of 9 the same, 68 of 68 items, 43 s |
| G2: the existing samples again (16 files, our route) | 2 of 16 run by our route, as clang's: `samples/bitfields.cpp`, `samples/vexing.cpp`; the other 14 below (3 min 53 s, the headers' skim dominating) |

The existing samples' first refusals after G2: templates (`auto.cpp` a function template, `classes.cpp`, `layouts.cpp`
and `p1-first.cpp` class templates, `decls.cpp` an alias template; `decls.cpp` and `layouts.cpp` have no `main`, so
clang's route does not run them either), the C++ library (`atomics.cpp` and `io.cpp`'s `std::atomic<int>` and
`std::vector<int>`, `hello.cpp`'s `std::string`, `ctor-throw.cpp`, `uncaught.cpp`, `thread-throw.cpp`, `threads.cpp`
and `coroutine.cpp`'s classes of `<stdexcept>`, `<thread>`, `<coroutine>`), a construct the native parser declines
(`library.cpp`): G3 and G4's work. Before G2 (G1's record): templates (`auto.cpp` a function template, `classes.cpp`
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
| **G2** (branch `cpp-codegen2`, §4a: done but covariant returns' return adjustment, generic lambdas, constexpr array bounds) | the rest of the core language without templates: exceptions (`throw`, `try`/`catch` by type_info, `invoke` and landing pads as clang writes them, cleanups on the unwind path, `noexcept`), lambdas (closure classes), bit-fields, unions, multiple and virtual inheritance (thunks, VTTs, construction vtables), conversion functions, member pointers, `dynamic_cast` and `typeid`, `long double`, SysV's argument classes (P3), `constexpr` functions evaluated | 60 samples; the recorded `samples` list's files that use no library header beyond C's | 2-3 |
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

**P8. G2's remainder.** Left out of G2: (1) covariant return types whose return needs an adjustment (a thunk with a
return adjustment, `_ZTc...`); (2) generic lambdas (they are templates); (3) array bounds given by a constexpr call
(the native parser keeps such a bound as text in the declaration's type, `int[sq(3)]`); (4) a namespace-scope lambda's
`$_N` (clang numbers it in an order not yet understood: the TU's lambdas before it, less one); (5) `goto` across
destructors, a `while` condition's declaration, a class through `...` (G1's). Options: (a) each with G3, when templates
bring their own thunks and the native parser's bounds are expressions (about half a day all told); (b) now, before G3
(the same cost, nothing waits for them). **Recommended: (a)**: none of the recorded lists' files needs them before the
headers do.

**P9. The code generator's provenance.** G2 follows clang's functions closely in structure (ItaniumVTableBuilder's
vcall and vbase offsets and IsOverriderUsed, VTTBuilder's order, X86_64ABIInfo::classify's eightbytes) but is written
from the Itanium ABI, the psABI and clang's IR on the samples: clang's source is not on the host (no llvm-project tree;
the brief allowed no fetch), so nothing could be ported and nothing is recorded in LICENSE.md. (a) Keep it so; the
names in the comments say which of clang's functions each part models (as built). (b) Fetch llvm-project at
`llvmorg-23.1.4` (pinned by its tarball's SHA-256 in `test/cpp/corpora.edn`'s style) and compare each modelled part
with clang's code line by line, porting where ours differs, recorded in LICENSE.md (Apache-2.0 with the LLVM
exception) and doc/ARBACE.md as P6 allows; about half a day, worth it before G3's instantiator, which P6 ports anyway.
**Recommended: (b) at the start of G3**, together with the template instantiator's port.

**P10. codegen-check's cost.** 82 samples take about 70-85 s light (4 jobs) to generate and lower, 6 s to compile and
run. (a) All in the essential step when P7 (b) applies. (b) An essential subset (one sample per area, about 20, 25 s) in
the essential gate, all in `--full`. **Recommended: (b)**, with P7.

## 8. Sources

- The standard: ISO/IEC 14882:2024 (C++23), clauses as cited ([basic.lookup], [over.match], [over.ics.rank],
  [dcl.init], [class.copy.elision], [class.dtor], [class.default.ctor], [class.copy.ctor]).
- The Itanium C++ ABI, itanium-cxx-abi.github.io/cxx-abi/abi.html: §2.4 (layout), §2.5 (vtables), §2.9.5 (type_info),
  §3.3.2 (guards), §5.1 (mangling).
- clang at `llvmorg-23.1.4` (https://github.com/llvm/llvm-project, the oracle's version): the files of §1 named as the
  models, G2's also `RecordLayoutBuilder.cpp`, `VTableBuilder.cpp` (ItaniumVTableBuilder, VCallAndVBaseOffsetBuilder),
  `CGVTT.cpp`/`VTTBuilder.cpp`, `CGClass.cpp` (getVTablePointers, CanSkipVTablePointerInitialization),
  `Targets/X86.cpp` (X86_64ABIInfo::classify), `ExprConstant.cpp`; nothing ported (P9). Its output (`clang++ -O0 -S
  -emit-llvm` on the samples) is the oracle.
- The System V x86-64 psABI (gitlab.com/x86-psABIs/x86-64-ABI), §3.2.3 (parameter passing).
- The lowering's change: `arbace/cpp/lower.ae`'s `fp-lit` takes a decimal x86_fp80 constant as a long double literal.
- The native parser's change on `cpp-codegen2`: a variable's entity keeps its C linkage (`:clink`), as functions' did.
- The native parser's change: `arbace/repr/cpp/parse.ae` keeps a function entity's printed type (`:qt`) and its C linkage
  (`:clink`, the dynamic `*linkage*` bound in `parse-linkage`); the tree and the forms are unchanged.

## Decided (2026-11-05, the user)

P1-P7, each as recommended: LLVM forms as the output; the native tree as input now, C++ forms at G6; SysV's classification in G2; nounwind with G2; the headers in dependency order; clang's code (Apache-2.0 with the LLVM exception) ported for templates and the ABI classification only, recorded in LICENSE.md and doc/ARBACE.md; codegen-check essential once C++ has its row.

## Decided (2026-11-05, the user: P8-P10)

P8 (a): G2's remainder with G3; P9 (b): at G3's start, the ABI code compared with llvm-project 23.1.4 (in the main checkout's .tmp/llvm/) and the modelled parts ported, recorded in LICENSE.md, with the template instantiator's port; P10 (b): about 20 samples in the essential gate, all in --full, once C++'s REAP row applies.

## G3. Templates (branch `cpp-templates`, 2026-11-05)

Status: the design and a first slice, built on `cpp-codegen` (P1-P7 decided), for the user's acceptance. It ran beside
G2 (`cpp-codegen2`: exceptions, lambdas, inheritance, SysV) and kept out of G2's areas; its code is a namespace of its
own, `arbace.cpp.template` (`arbace/cpp/template.ae`), with hooks in `arbace.cpp.sema`, the template parts of
`arbace.cpp.abi`'s mangling and a linkonce_odr static with its guard in `arbace.cpp.codegen`. G2's branch (at
`0fff28150`, P8-P10 decided) is merged into this one: the conflicts were in sema (class lookup through several bases
with the dependent-base rule, the bases' list, constant evaluation's fallback to G2's evaluator, the entities of
captures, the analysis's worklist with G2's typeid prelude), abi (std's `St` with template prefixes; G2's local names,
which this branch's local classes now use too: `[:local f]` paths) and codegen (the global initializations' kinds); G2's
P8 (a) (its remainder with G3) and P9 (b) (the ABI code compared with clang's and the modelled parts ported, at G3's
start) are not done here: this branch's brief predates them.

Contents: G3.1 What clang does · G3.2 What the native tree gives · G3.3 The design · G3.4 Ported and written ·
G3.5 Mangling · G3.6 What the libstdc++ headers demand first · G3.7 The check and the numbers · G3.8 What is not
done · G3.9 The staged plan · G3.10 Proposed · G3.11 Sources.

### G3.1 What clang does

By clang's sources at `llvmorg-23.1.4` (the tarball pinned in `test/cpp/corpora.edn`, read from
`.tmp/llvm/llvm-project-23.1.4.src.tar.xz`, its SHA-256 checked against the pin):

| part | clang | what it does |
|---|---|---|
| the parser's hooks | `SemaTemplate.cpp`: `ActOnTemplateParameterList`, `ActOnTypeParameter`, `ActOnNonTypeTemplateParameter`, `ActOnClassTemplateSpecialization`, `CheckTemplateIdType`, `CheckTemplateArgumentList` | a template's parameter list and pattern entered; a template-id's arguments checked against the parameters (defaults added, packs gathered) and its type made (a specialization, dependent or not) |
| deduction | `SemaTemplateDeduction.cpp`: `DeduceTemplateArguments` (a call's, a declaration's, a partial specialization's), `AdjustFunctionParmAndArgTypesForDeduction`, `DeduceTemplateArgumentsByTypeMatch`, `DeduceTemplateSpecArguments`/`DeduceTemplateBases`, `FinishTemplateArgumentDeduction` | P and A adjusted ([temp.deduct.call]/2-4), matched structurally under flags (`TDF_*`), the results checked for consistency, defaults substituted, the signature substituted (a failure there is SFINAE) |
| partial ordering | `SemaTemplateDeduction.cpp`: `isAtLeastAsSpecializedAs`, `getMoreSpecializedTemplate`, `getMoreSpecializedPartialSpecialization` | deduction of each template's parameters from the other's types in a "partial ordering" mode ([temp.deduct.partial]: references and cv removed, the lvalue-reference and cv rules of /9; a trailing pack less specialized) |
| instantiation | `SemaTemplateInstantiate.cpp` (`InstantiateClass`, `InstantiateClassMembers`, `SubstType`, `getPatternForClassTemplateSpecialization`), `SemaTemplateInstantiateDecl.cpp` (`InstantiateFunctionDefinition`, `InstantiateVariableDefinition`, `PerformPendingInstantiations`), `TreeTransform.h` | a class specialization's members declared from the pattern (TreeTransform substituting the arguments); a function's or member's definition instantiated when odr-used, queued as a pending instantiation and done at the end of the unit; the pattern chosen among the primary, the partial specializations (the most specialized matching) and an explicit specialization |
| specializations | `SemaTemplate.cpp`: `CheckFunctionTemplateSpecialization`, `CheckMemberSpecialization` | `template <>` declarations matched to their template (deduction from the declared type for a function's) |
| mangling | `ItaniumMangle.cpp`: `mangleTemplatePrefix`, `mangleTemplateArgs`, `mangleTemplateParameter`, `mangleIntegerLiteral`, `mangleFunctionEncodingBareType`, `mangleUnresolvedPrefix`, `mangleExpression` | `<template-args>` `I ... E`, `T_`, the template-prefix as a substitution candidate, a function template's specialization mangled with its template's signature and return type, dependent names (`N T_ 4type E`) and expressions (`X ... E`, `sr`) |

### G3.2 What the native tree gives

The native parser keeps a template as clang's dump shows it before instantiation: `ClassTemplateDecl`,
`FunctionTemplateDecl`, `TypeAliasTemplateDecl`, `VarTemplateDecl` with their parameter declarations
(`TemplateTypeParmDecl`, `NonTypeTemplateParmDecl` with the default as a `TemplateArgument` kid, `TemplateTemplateParmDecl`,
`isParameterPack`) and the pattern; `ClassTemplatePartialSpecializationDecl` and `ClassTemplateSpecializationDecl`
(the latter with its `TemplateArgument`s); out-of-line members as plain declarations whose text starts with `template
<...>`; inside a pattern, every type as its spelling (`T (&)[N]`, `typename T::type`, `const Box<T> &`) and every name
as its identifier, the dependent nodes clang's dump has (`CXXUnresolvedConstructExpr`, `UnresolvedMemberExpr`,
`PackExpansionExpr`, `SizeOfPackExpr`, `DeclRefExpr`s without a resolved declaration). Not in the tree: a template-id's
explicit arguments in an expression (`f<int>(x)`, `Box<int>::count`, `o.as<double>()`) and a partial
specialization's arguments, which are read from the main file's text at the node's range (as G1 reads qualifiers).

### G3.3 The design

1. **Instantiation is re-analysis.** A pattern is syntax with spellings, and `arbace.cpp.sema` analyzes syntax: so a
   specialization is the pattern declared again (`declare-node!`, `declare-fn!`, `declare-var!`) in a scope that binds the
   template's parameter names to the arguments (a type to a type entity, a value to an enumerator's entity, a template
   to the template, a pack to a pack entity). That is what clang's TreeTransform does by substitution; here no tree is
   transformed and no dependent node needs a case of its own: `CXXUnresolvedConstructExpr` is analyzed as a construction,
   a dependent call as a call, `T::type` as a qualified name, all at the instantiation. Two-phase lookup's first phase is
   approximated: a function template's body sees the names declared before its definition (G1's cutoff), ADL at the
   call adds the arguments' namespaces' functions and function templates, and an unqualified name in a class
   specialization does not look into a base that was dependent in the pattern ([temp.dep]/4: `this->f()` and
   qualified names do).
2. **A template's signature has its parameters as types.** For deduction, ordering and mangling, a function template's
   declared type is resolved in a scope where each parameter stands for itself: `{:k :tparam :i i :tid tid}` (a type),
   `{:k :tparam-v ...}` (a value: an array bound, an argument), `{:k :ttp ...}` (a template), and the dependent forms
   built from them: `{:k :tspec}` (a template-id of dependent arguments, `Box<T>`), `{:k :tdep}` (`typename T::type`,
   `remove_reference<T>::type`), `{:k :texpr}` (a dependent expression: `X<T>::value`, `!B`, `N + 1`, `sizeof(T)`),
   `{:k :pack-exp}` (`Ts...`). Arguments are types, `{:val n :ty T}`, `{:tmpl tid}` and `{:pack [...]}`.
3. **On use only.** A class specialization's members are declared with the class but their definitions are analyzed
   only when a round of analysis references them (`:lazy`, made eager by the `:pending` hook: the functions a body
   calls, the classes it names with their destructors and, when dynamic, every virtual function of their vtable, the
   static members it reads), clang's pending instantiations; a body that does not compile for the arguments
   (`never_used()` in `tmpl-stack.cpp`) is never analyzed. A function template's specialization is declared when a call
   deduces it and defined when used. All are emitted `linkonce_odr` (codegen's inline linkage), static data members
   included; explicit specializations are ordinary definitions.
4. **Memoized by arguments.** Each template keeps `{args id}`; a class specialization is registered before its members
   are declared, so a member naming the class (`Num<T>& twice()`, the injected-class-name) finds it.
5. **SFINAE is a refusal caught.** A specialization whose declaration fails to analyze (`typename T::type` for `int`,
   `enable_if<false>::type`) is not a candidate; deduction failures are nil (`CPP_TMPL_TRACE=1` prints each template
   that drops out and why).
6. **Overload resolution** takes the specializations each function template deduces with the plain candidates; the
   tie-breaks of [over.match.best]/2.4-2.5 are a sema hook: a non-template over a specialization, then the more
   specialized template by partial ordering.
7. **Pattern selection** for a class: an explicit specialization, else the matching partial specializations (each one's
   parameters deduced from the arguments, the non-deduced expressions checked against what was deduced) ordered by
   [temp.class.order], else the primary.
8. **Packs** are entities: a template parameter pack's name stands for its elements; a function parameter pack's name
   for its parameters (`:param-packs`); a pack expansion in a parameter list, an argument list or an expression is
   resolved or analyzed once per element in a scope binding each pack named in its pattern to that element.
9. **The rest by the same means**: alias templates resolve their pattern's type in the binding scope (transparent, as
   [temp.alias]/2 says); variable templates declare a variable per specialization (folded where used as a constant,
   defined when odr-used); `if constexpr` evaluates its condition and analyzes the taken branch only; constructor
   templates are candidates of a class's initialization; member templates and member class templates live in their
   class's scope; out-of-line members of a class template are attached to each specialization of the primary (or of
   the partial specialization their qualifier's arguments name), their own header's names bound, out-of-line member
   templates likewise; explicit specializations of members and of member templates of a class specialization replace
   the member's definition; a friend function (or function template) defined in a class is a member of the enclosing
   namespace whose body sees the class's names, each class specialization's hidden friends their own; classes and
   enumerations local to a function (a function template's included) mangle as local names; a pointer or reference
   non-type parameter is bound to the function or variable its argument names; a template template parameter to the
   template; a class template's static member initialized dynamically runs behind its guard variable.

The hooks sema calls (`arbace.cpp.sema/hooks`, installed by `arbace.cpp.template`): `:declare` (a template's nodes),
`:template-id` (`(<> name args...)`), `:dependent-name` (`T::x` in a signature), `:const-bound` (an array bound),
`:call-candidates`, `:ctor-candidates`, `:tiebreak`, `:pack-expand` (types), `:expand-pack` (expressions),
`:var-template`, `:pending`, `:wrap` (the templates' state around an analysis). Sema itself changed where G3 needed the
general rule (the slice's samples and the corpus found these; they hold for non-templates too): reference collapsing, a parameter's array or function type adjusted to a pointer ([dcl.fct]/5), the
qualification adjustment's rank ([over.ics.rank]/3.2.6), an array's cv at its elements in reference binding, `if
constexpr`, `sizeof...`, comparisons and logical operators in constant evaluation, a constant local's initializer
kept, value-initialization by `T()`, a qualified name whose parts are template-ids split at the top level only (`o.A<int>::f`,
`using ::f`), `*p` of a function pointer called, `o.k` of a constant static member folded (not an odr-use), an implicit
destructor trivial whatever the vptr ([class.dtor]/8), an unknown name left to argument-dependent lookup,
`__builtin_abort` (clang calls `abort`), `decltype(nullptr)` and `decltype(sizeof ...)` (the headers' `nullptr_t` and
`size_t`), a function's local classes' path, the analysis of a program as a worklist (declarations, then each round's
pending definitions); codegen: a linkonce_odr static member and its guard. repr/cpp's type reader
(`arbace.repr.cpp.types`) now ends a template argument at a `,` or `>` outside parentheses only (`X<(a > b)>`).

### G3.4 Ported and written (P6 (b))

| part | origin |
|---|---|
| `deduce-by-type-match` | ported: `DeduceTemplateArgumentsByTypeMatch` (the flags `TDF_SkipNonDependent`, `TDF_ParamWithReferenceType`, `TDF_IgnoreQualifiers`, `TDF_DerivedClass`; the partial-ordering kind `Call` and its reference and cv rules; template type parameters with their qualifiers; pointers, references, arrays with a deduced bound, function types, template specializations, non-deduced contexts) for the types the slice has |
| `adjust-for-deduction`, `forwarding-reference?` | ported: `AdjustFunctionParmAndArgTypesForDeduction`, `isForwardingReference` |
| `deduce-template-spec` | ported: `DeduceTemplateSpecArguments` with `DeduceTemplateBases` (the unique base) |
| `bind-arg!`, `inconsistent-or-superset?` | ported: `checkDeducedTemplateArguments`, `hasInconsistentOrSupersetQualifiersOf` |
| `at-least-as-specialized?`, `more-specialized-fn?` | ported: `isAtLeastAsSpecializedAs` (a call's context) and `getMoreSpecializedTemplate`'s variadic rule |
| the template parts of `arbace.cpp.abi`'s mangling | reimplemented after `ItaniumMangle.cpp`'s functions named in G3.1 and the ABI's §5.1 |
| everything else (`declare-template`, instantiation, pattern selection, packs, the constant expressions of text, the pending definitions) | written, from the standard ([temp]) with clang's functions as the model; TreeTransform is not ported (G3.3, 1) |

The ported functions keep their names' sense and structure, not their C++; the files carry clang's notice and
LICENSE.md and doc/ARBACE.md record the origin (llvm-project at `llvmorg-23.1.4`, commit
`7a941dfb2877a0b8cf30323fefa85ad019e0b0f5`; Apache-2.0 with the LLVM exceptions).

### G3.5 Mangling

Equal to clang's on every sample (the check's names level) and on the corpus files that pass: a class specialization's
`<name> I <args> E` with the template-prefix and the specialization as substitution candidates (`3BoxIiE`,
`N2ns3BoxIiEE`, `NS0_IcEE`); a value argument `L <type> <n> E` (`Li3E`, `Lin3E`, `Lb1E`, `Lm4E`); a function template's
specialization with its template's signature and return type (`_Z4max2IiET_S0_S0_`, `_Z3lenIiLm4EEmRAT0__T_`); member
templates (`_ZNK2ns3BoxIiE2asIdEET_v`), constructor templates (`_ZN4PairIliEC2IicEERKS_IT_T0_E`); dependent member
types (`N9enable_ifIX...EiE4typeE`) and expressions (`Xsr6is_intIT_EE5valueE`, `XntsrT_5valueE`, `XgtplT_Li1ELi2EE`);
packs (`IJidcEE`, `DpT_`); template template arguments and parameters (`I1DE`, `T_IiE`); variable template
specializations (`_Z5widthIlE`); a function template's argument for a non-type parameter of dependent type, its
parameter written first as `ItaniumMangle.cpp`'s `needToMangleTemplateParam` says (`IiTnT_Li42EE`, a pack's
`TpTnT_`), a non-type parameter as an argument (`XT0_E`); a function's or variable's address and a reference
(`XadL_Z1FiEE`, `L_Z2gvE`, a null pointer `LPv0E`); local names (`_ZZ1fILi3EEivEN1S1gEv`); guard variables of template
statics (`_ZGVN1XIiE4flagE`).

### G3.6 What the libstdc++ headers demand first

From the host's libstdc++ 15.2.0 headers (`/usr/include/c++/15.2.0`, the ones the native parser reads, read here as
input, never ported: GPL with the runtime exception): `<utility>` (`bits/move.h`, `bits/stl_pair.h`, `bits/utility.h`)
and `<type_traits>` (4,197 lines) need, beyond this slice,

- **the headers' bodies parsed and their text at hand**: the native parser skims headers into declarations; templates
  need their patterns (G4's first task), and this slice reads explicit template arguments and qualifiers from the main
  file's text (`node-text`), which must become the text of the node's own file;
- **the type-trait builtins** clang provides and `<type_traits>` uses when `__has_builtin` says so: 47 distinct
  (`__is_same`, `__is_class`, `__is_enum`, `__is_union`, `__is_trivially_copyable`, `__is_constructible`,
  `__is_convertible`, `__is_base_of`, `__remove_reference`, `__remove_cv`, `__remove_cvref`, `__decay`,
  `__add_pointer`, `__add_lvalue_reference`, `__add_rvalue_reference`, `__underlying_type`, `__type_pack_element`,
  `__make_integer_seq`, ...), each a type or a constant of the arguments: a table in sema, each checked by a sample
  against clang;
- **inline namespaces and abi tags** (`std::__cxx11`, `B5cxx11`), `__attribute__((__always_inline__))`, `noexcept(expr)`,
  `decltype` (the corpus's `decltype` refusals), `constexpr` functions evaluated (`std::forward` is constexpr, used in
  constant contexts), defaulted template arguments that are SFINAE (`typename = _Require<...>`: already the slice's
  shape, `tmpl-enable-if.cpp`), `__and_`/`__or_` (class templates over packs: the slice's packs);
- `std::pair` itself needs its constructors' constraints (C++20 `requires` and `explicit(bool)` in libstdc++ 15 at
  `-std=c++23`): concepts and `explicit(...)` are G3's next stage.

The slice's `tmpl-utility.cpp`, `tmpl-traits.cpp` and `tmpl-enable-if.cpp` are those headers' shapes written out
(`remove_reference`, `move`, `forward`, `pair`, `make_pair`, `swap`, `integral_constant`, `is_same`, `enable_if`).

### G3.7 The check and the numbers

The 33 samples `test/cpp/codegen/tmpl-*.cpp` (each with a header comment saying what it exercises) are judged by
`bin/cppi codegen-check` with G1's 38, all three levels (run, native, names), against the empty known file.

| check (2026-11-05, light) | result |
|---|---|
| `bin/cppi codegen-check` over `test/cpp/codegen` (38 + 33, before the merge with G2) | 71 of 71 pass; generate and lower about 60 s, gcc, clang and runs 7 s |
| the same after the merge (G2's 82 with G1's, G3's 33) | 115 of 115 pass; generate and lower about 2 minutes, gcc, clang and runs 11 s |
| gcc 15.2.0's template run tests (`g++.dg/template`, `g++.old-deja/g++.pt`, `g++.dg/cpp0x`: the `dg-do run` files including no header beyond C's (`stdio`, `stdlib`, `string`, `stddef`, `assert` and their `c` forms) that clang accepts at `-std=c++23`, 288 files; read in place from the pinned tarball's extraction, not committed) | 147 of 288 pass all three levels (51 %): `g++.old-deja/g++.pt` 104 of 143, `g++.dg/template` 17 of 35, `g++.dg/cpp0x` 26 of 110; 63 at the first measure of this branch, 99 after the first round of fixes; generate and lower about 2.5 minutes, gcc, clang and runs 17 s. After the merge with G2: **169 of 288 (58.7 %)**: `g++.old-deja/g++.pt` 112, `g++.dg/template` 20, `g++.dg/cpp0x` 37 (G2's branch alone: 21 of 288; no file passing on either branch fails merged) |
| `bin/repr-cpp native-check samples` after repr/cpp's type reader change | 9 of 9 the same, 68 of 68 items |
| the existing samples refused on templates at G1 (`auto.cpp`, `classes.cpp`, `p1-first.cpp`) | past their templates; refused next on `auto` return types, `try` (G2) and a concept |

The corpus's 141 others before the merge, by what stops them (each file's first refusal or its verdict; after the merge
119 remain, G2's classes the most reduced):

| class | files | whose |
|---|---|---|
| inheritance (virtual and multiple bases), unions, exceptions, lambdas, conversion functions and their templates, member pointers, `auto` | 54 | G2's areas |
| the core analysis: initialization corners (`ImplicitValueInitExpr`, `CXXDefaultInitExpr`, aggregates, references to temporaries), constant evaluation (`constexpr` functions), range `for` over a class, `this` in a default member initializer | 27 | G1/G2 (sema) |
| builtins (`__builtin_memset`, `__builtin_operator_new`, `__builtin_addressof`), `__PRETTY_FUNCTION__`, attributes in types, user-defined literals, statement expressions, the parser's declines | 23 | G4 and repr/cpp |
| templates' remaining corners: G3.8's (pointer arguments of subobjects, explicit packs extended, a template template argument `T::template B`, a member template-id of three levels) and overload-resolution failures inside templates (initializer lists, deduction from `const T` arrays) | 20 | G3b-G3c |
| wrong results: names clang folds away (`constexpr` objects and functions clang evaluates, a static local's constant initialization: 7), a user `operator new` or `C1` called natively (`new1`, `new11`, `virtual4`, `rv-life`: the native level), addresses printed (`t39`), `constexpr` odr-use (2), a function-try-block (`fntry1`, G2) | 16 | mostly G2's constant evaluation; none in the template machinery's names |
| other (the reference failed, a declaration kind) | 2 | |

After the merge the wrong results are 26, all but three on G2's branch alone too or newly reachable through G2's
inheritance: `temp-extend1` (a temporary bound through a subobject, `B().i[0]`, destroyed early: G2's branch regressed
G1 there), `constexpr-static4`, `mi1` (an empty base's offset among virtual bases: 16 where clang has 24), `virtual4`
(an extern global declared before its definition, lowered twice); a construction vtable's name now shares its
substitutions (`_ZTC12ECPrivateKeyI4EC2NE0_11ECPublicKeyIS0_E`, found by `mi1`).

### G3.8 What is not done (refused with a class)

Concepts and `requires` (the native tree keeps a `ConceptDecl`'s parameters but not its constraint, and no
requires-clause or requirement: P14), class template argument deduction, `explicit(bool)`, conversion function templates
(with G2's conversion functions), defaults of template template parameters, `decltype` beyond `nullptr` and `sizeof`
(with G2's `auto`), the address of an array or of a subobject as a template argument (`XadsoiL_Z3arrEEE`), a dependent
expression in a signature beyond G3.5's kinds (calls, casts), explicit pack arguments extended by deduction
(`foo<int>(2, 3, 4)` for `template <class... T>`), the trailing requires-clause's and constrained parameters' mangling
(`Q...E`, `Tk`), packs of different lengths in one expansion, two-phase lookup's exact first phase (G3.3, 1).

### G3.9 The staged plan

| stage | what | target | estimate |
|---|---|---|---|
| **G3a** (this branch) | G3.3-G3.5 as built | 32 samples (all pass); the gcc corpus as measured (G3.7), its template-specific refusals nearly gone | done |
| **G3b** | concepts after P14: the parser's constraint nodes, `SemaConcept.cpp`'s satisfaction (atomic constraints evaluated as constant expressions, `requires` expressions' requirements checked by analysis as SFINAE), constrained overloads ordered by subsumption (normal forms, `IsAtLeastAsConstrained`), `Q`/`Tk` mangling; `explicit(bool)`, `noexcept(expr)` | `p1-first.cpp`; the libstdc++ 15 constructs of G3.6 at `-std=c++23` | 2-3 |
| **G3c** | CTAD (`SemaTemplateDeductionGuide.cpp`: implicit guides from constructors, user guides), conversion function templates with G2's conversions, `decltype` and `auto` with G2 | the corpus files refused on these | 1-2 |
| **G3d** | with G4: header bodies parsed, each node's text from its own file (P13), the type-trait builtins of G3.6 | `<utility>` and `<type_traits>` samples by our route | with G4 |

### G3.10 Proposed

**P11. The template corpus.** (a) gcc's run tests as measured here, read in place from the gcc tarball's extraction, the
list made by a rule (no header beyond C's, accepted by clang at `-std=c++23`), not committed. (b) The same with the list
and its recorded results committed (`test/cpp/codegen/gxx-templates.tsv`, `gxx-templates-results.edn`, a judge as
`lower-check`'s with a known file), `bin/cppi codegen-check --corpus gxx-templates`; costs a known file to keep. (c)
(b) plus llvm-test-suite's `SingleSource` C++ programs (not fetched). **Recommended: (b)**: the measure becomes a
regression check at light cost (the 288 files run in about 3 minutes).

**P12. Instantiation by re-analysis or by substitution.** (a) Re-analysis of the pattern in a binding scope (as built):
small, every construct sema knows works in templates at once; costs the first phase of two-phase lookup's exactness
(a name visible at the definition but declared after it is approximated by the cutoff) and repeats the analysis per
specialization. (b) A port of TreeTransform's substitution over analyzed patterns: clang's exact behaviour; costs
analyzing dependent patterns (a dependent type system in sema) and a port of TreeTransform's 18,000 lines' worth of
cases. **Recommended: (a)**, revisited if the corpus finds a program whose meaning differs by lookup phase.

**P13. Where explicit template arguments come from.** (a) The main file's text at the node's range (as built). (b) The
native parser records them in its tree (a `TemplateArgument` list on `DeclRefExpr` and `MemberExpr`, the explicit arguments clang's
`DeclRefExpr::template_arguments` holds): no text reading, headers work as the main file; costs a parser change
and native-check's comparison of the new field. **Recommended: (b) before G4**, which needs it for the headers.

**P14. Concepts' input.** (a) Read constraints from the main file's text, as explicit arguments are now: needs an
expression parser of text (only constant expressions are read today) and fails in headers. (b) The native parser keeps
what clang's dump keeps: a `ConceptDecl`'s constraint expression, a template's requires-clause and a function's trailing
one (`TemplateParameterList::getRequiresClause`, `FunctionDecl::getTrailingRequiresClause`), a constrained parameter's
`TypeConstraint`, `RequiresExpr` with its parameters and requirements (simple, type, compound, nested); costs a
parser change, its native-check against cxxdump (the oracle's dumper, extended where it does not write them yet) and
the converter's forms for them.
**Recommended: (b)**, with P13 (b): both are the native tree carrying what the analysis needs instead of the text.

### G3.11 Sources

- The standard: ISO/IEC 14882:2024, [temp] (in particular [temp.param], [temp.names], [temp.arg], [temp.class.spec],
  [temp.class.order], [temp.func.order], [temp.deduct], [temp.deduct.call], [temp.deduct.partial], [temp.deduct.type],
  [temp.inst], [temp.expl.spec], [temp.variadic], [temp.alias]), [over.match.best], [over.ics.rank], [dcl.fct], [dcl.ref].
- The Itanium C++ ABI §5.1 (mangling: templates, substitutions, expressions).
- clang at `llvmorg-23.1.4`, commit `7a941dfb2877a0b8cf30323fefa85ad019e0b0f5`: `clang/lib/Sema/SemaTemplate.cpp`,
  `SemaTemplateDeduction.cpp` (ported in part, G3.4), `SemaTemplateInstantiate.cpp`, `SemaTemplateInstantiateDecl.cpp`,
  `SemaTemplateVariadic.cpp`, `TreeTransform.h` (read as the model), `clang/lib/AST/ItaniumMangle.cpp` (reimplemented in
  part), `clang/LICENSE.TXT`; extracted from the pinned tarball into the worktree's `.tmp/llvm/`.
- gcc 15.2.0's testsuite (the tarball pinned for repr/c, extracted in `.tmp/repr-c/`): the corpus of G3.7, read as input.
- libstdc++ 15.2.0's headers on the host (G3.6), read as input.
