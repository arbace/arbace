# C++ without the host's clang: Sema and a code generator (cpp-codegen)

Status: survey, plan, the first slice (G1, branch `cpp-codegen`) and the second (G2, branch `cpp-codegen2`, §4a,
2026-11-05), templates (G3), the first slice of the headers (G4, branch `cpp-g4`, "G4. The headers"), P15, P16 and the
streams (G5, branch `cpp-g5`, "G5. P15, P16 and the streams"), and the runtime's libsupc++ by our route (G5c, branch
`cpp-runtime-own`, "G5c. The runtime by our route" at the end), for the user's acceptance. Words: **the native parser** is CX11's (`arbace.repr.cpp.parse`, [NATIVE.md](NATIVE.md)), whose tree is
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

After the merge the wrong results were 26: 14 the same on G2's branch alone (its constant-evaluation and lifetime cases;
`temp-extend1`, a temporary bound through a subobject, `B().i[0]`, destroyed early, is one G1 passed), 8 as before the
merge (the table's last row), 4 reached through G2's features: `mi1` (an empty base's offset among virtual bases, 16
where clang has 24), `vbase1` (fixed since: a construction vtable's name now shares its substitutions,
`_ZTC12ECPrivateKeyI4EC2NE0_11ECPublicKeyIS0_E`), `anon-union1`, `constexpr-initlist`.

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

## Decided (2026-11-05, the user: P11-P14)

P11 (b): gcc's template run corpus list committed with a recorded-results file as a regression check; P12 (a): instantiation by re-analysis; P13 (b): the native parser records explicit template arguments, before G4; P14 (b): the native parser keeps constraints and requires-expressions.

## G2 fixes (branch `cpp-g2fix`, 2026-11-05): G3's three bugs and P8 (a)

Each fix has a sample in `test/cpp/codegen/` that failed before and passes at all three levels of `bin/cppi codegen-check`
(run, native, names).

| Bug | Cause | Fix | Sample |
|---|---|---|---|
| a temporary bound to a reference through a subobject (`const int &r = B().i[0]`) destroyed at the full-expression's end | only a reference bound directly to the temporary extended it | codegen finds the complete temporary under a member, base, array element or decay (`extend-root`) and puts its destructor in the extended cleanups ([class.temporary]) | `temp-extend1` |
| an empty base's offset among virtual bases (16 where clang has 24) | empty bases and empty-class members always at 0 | `arbace.cpp.abi/layout` tracks the empty subobjects placed ({offset #{class}}), as clang's `EmptySubobjectMap` (`CanPlaceBaseAtOffset`, `CanPlaceFieldSubobjectAtOffset`, `UpdateEmpty...`): a base or member whose empty subobjects clash with one of the same class at the same offset moves to the next aligned offset, virtual bases likewise | `mi1` |
| an `extern` global declared before its definition broke the lowered C (`extern void *bp` after `struct lt$1 bp`) | the use through the extern declaration declared the global again | a defined variable's name is entered as declared when the definitions are written | `virtual4` |

`virtual4` showed two more faults behind its native level's failure (`_ZN1BC1Ev`): an out-of-line constructor or
destructor did not join its in-class declaration (`find-redecl` looked the name up where only functions are entered; the
call went to the body-less declaration, declared next to the definition, which clang's IR parser refuses), and a member
declared in the class and defined outside was marked inline (so `linkonce_odr`, where clang's is external). Also, the
complete-object alias `C1`/`D1` of a strong definition is now written where the complete constructor is called, as clang
emits it (a class never constructed whole has none). Sample `ctor-outofline`.

P8 (a), the three items asked for (done):

- Covariant returns that need an adjustment ([class.virtual]/8; the base's offset is not 0, or it is a virtual base): as
  clang's `AddMethods`, the overrider gets a slot of its own and the slot of the overridden function keeps its function,
  whose entry is a thunk `_ZTc<this call-offset><return call-offset><encoding of the overrider>` (this adjustment
  `h0_` or `v..._`; return adjustment `h<n>_` or, through a virtual base, `v<n>_<offset of the vbase offset>_`),
  null-checked for pointers, the virtual offset applied before the non-virtual one (`performReturnAdjustment`). Samples
  `covariant-adjust` (non-virtual: `_ZTch0_h16_N1Q3getEv`), `covariant-virtual` (virtual base, reference, a further override).
- A namespace-scope lambda's `$_N`: the numbering was right (the TU's closures in order); what differed was the
  mangled scope, the closure class took no namespace path (`_ZNK3$_1clEi` for `_ZNK1N3$_1clEi`). Sample `lambda-namespace`.
- Array bounds that call a constexpr function (`int a[sq(3)]`): `arbace.cpp.template/eval-text` evaluates a call of a
  named function on constant arguments through `arbace.cpp.sema/ce-call-fn`. Sample `constexpr-bound`. A call with a
  dependent argument is still refused (G3's substitution would carry it).

Not done of P8: generic lambdas (they need templates' machinery for the closure's `operator()` template), `goto` across
destructors, a `while` condition's declaration, a class through `...`.

Provenance: the layout and the vtable code were read in `clang/lib/AST/RecordLayoutBuilder.cpp` (EmptySubobjectMap,
`LayoutBase`, `LayoutField`) and `clang/lib/AST/VTableBuilder.cpp` (`AddMethods`, `ComputeReturnAdjustmentBaseOffset`,
`ComputeReturnAdjustment`) and `ItaniumMangle.cpp` (`mangleThunk`) of llvm-project 23.1.4 (the pinned tarball); the code
is a reimplementation of their rules in Arbace's structures, not a port of their text (P6, P9 as built).

## G4. The headers (branch `cpp-g4`, 2026-11-05)

Status: a first slice of G4, built on `cpp-g2fix` (G1-G3 with G2's fixes; P1-P14 decided), for the user's acceptance. P13
(b) first, then the headers in P5's order: `<cstdio>`, `<utility>` (`std::move`, `std::forward`, `std::swap`, `std::pair`,
`std::make_pair`) and `<type_traits>`' common traits by our route at all three levels; `<vector>` (`std::vector<int>`:
`push_back`, `size`, `operator[]`, `back`, iteration) and `<string>` (`std::string` from a literal, `+=`, `c_str`) at the
run and native levels, their names differing by the always-inline functions clang inlines at `-O0` (G4.5, P16);
`<memory>`'s `std::unique_ptr` not yet (inheriting constructors). libstdc++ stays unported: its headers (15.2.0,
`/usr/include/c++/15.2.0`) are parsed and analyzed as the program's own text is.

Contents: G4.1 P13: the written name in the tree · G4.2 The headers parsed in full · G4.3 What the headers asked of the
analysis · G4.4 The samples and the numbers · G4.5 What is not done · G4.6 The staged plan · G4.7 Proposed.

### G4.1 P13 (b): the written name in the tree

The native parser's `DeclRefExpr` and `MemberExpr` keep the name as written (`arbace.repr.cpp.parse/written-name`):
`:explicitTemplateArgs` (each explicit template argument's text, as clang's `DeclRefExpr::template_arguments` holds
them), `:qualifier` (the nested-name-specifier's parts, each with its arguments: `["ns" "Box<int>"]`) and
`:globalQualifier` (a leading `::`), with `:_wn` marking a node that has them. `arbace.cpp.sema` reads them instead of
the main file's text: a name's explicit arguments (`written-targs`, for `f<int>(x)`, `v<int>`, `o.as<double>()`), a
reference's qualifier (`ref-qualifier`), a member access's (`o.A<int>::f`) and a call's qualification. The converter
does not read the new fields, so the forms are unchanged: `bin/repr-cpp native-check samples` is 9 of 9 the same, 68 of
68 items, after the change and at the branch's head.

The other readings of a node's text (a declaration's qualifier before its name, `using` declarations, a lambda's
captures, a partial specialization's arguments) are now of the node's own file (`sema/text-of`: the main file's text,
or a header's read once, its comments blanked so that the offsets stay), so they work in headers too; a macro token's
location is its expansion's.

### G4.2 The headers parsed in full

`arbace.repr.cpp.parse/*full-headers*` (the code generator's mode; `arbace.cpp.codegen/gen-file` binds it,
`CPP_FULL_HEADERS=0` turns it off): the headers' tokens are parsed as the main file's are (bodies, initializers,
default arguments), no skim and so no skim cache, and their nodes are given to the analysis with the main file's. A
header construct the parser cannot read is dropped (a declaration) or kept as a `NativeDecline` statement (in a body)
without counting as the main file's declines (`CPP_HDR_TRACE=1` prints each with its file and offset). The converter's
mode is unchanged (the headers skimmed for their names).

The analysis takes the header nodes as the program's (`sema/header-node?`: a node whose file is a header's path):

- a header declaration the analysis refuses is dropped, not the program (`declare-node!`; a use of it fails later as
  an unknown name; `CPP_HDR_TRACE=1` prints each drop and why); on `<utility>` 66 of its declarations are dropped
  today (variable template partial specializations, `<compare>`'s corners, conversion function templates), 188 with
  `<vector>`, 463 with `<memory>`;
- a header's function definition, a header class's implicit and defaulted members, a header's explicit
  specialization of a member and a header's variable definition are defined only when used (`:lazy`, `:pending-def`:
  clang emits an inline function or an inline variable only when odr-used); an implicit member of a header class
  whose subobjects' members are not viable is deleted ([class.copy.ctor]/10), not refused;
- `<typeinfo>`'s own `std::type_info` replaces the prelude of G2 when the headers are parsed;
- the C library's functions are the headers' declarations (`extern "C"` in `<stdio.h>`), not the skim's entities.

The traces for the work on headers: `CPP_HDR_TRACE=1` (the parser's declines and the analysis's drops in headers, the
function whose body failed, an ambiguous call's candidates), `CPP_TMPL_TRACE=1` (templates and constructor templates
that drop out, constraints not satisfied, decltype operands left dependent), `CPP_FAIL_STACK=TEXT` and
`CPP_NC_STACK=1` (the stack of a refusal whose message contains TEXT, of a constant evaluation's failure).

On the 128 samples of `test/cpp/codegen` the full parse costs little: generate and lower 119 s against G3's 111 s (the
headers are `<cstdio>` and `<typeinfo>`); `<utility>` takes about 15 s to parse and analyze, `<memory>` about 50 s.

### G4.3 What the headers asked of the analysis

Each in the general form the standard gives, found by the samples on libstdc++ 15.2.0 at `-std=c++23`:

| what | where | as built |
|---|---|---|
| `decltype(e)` ([dcl.type.decltype]) | `__and_`/`__or_`'s bases `decltype(__detail::__and_fn<_Bn...>(0))` | the parser keeps each operand it parses (`:decltypes` of the unit, by its text); sema analyzes it where the type is resolved (unevaluated) and takes the declared type of an id-expression or member access, else `T&`/`T&&`/`T` by the value category |
| the type-trait builtins (47 of G3.6 and the ones `<type_traits>` uses under `__has_builtin`) | `__bool_constant<__is_same(_Tp, _Up)>`, `using type = __remove_cv(_Tp);` | `arbace.cpp.template`'s table: 61 value traits (`__is_same`, `__is_class`, `__is_constructible`, `__is_convertible`, `__is_trivially_copyable`, `__is_nothrow_constructible`, ...) and 13 transform traits (`__remove_cv`, `__remove_reference`, `__remove_cvref`, `__decay`, `__add_pointer`, `__add_lvalue_reference`, `__underlying_type`, ...), written from [meta]'s definitions with clang's `SemaTypeTraits.cpp` as the model; constructibility and convertibility asked of the analysis (an initialization of `std::declval`'s expressions, the implicit conversion sequence), the nothrow ones by the called functions' exception specifications; dependent in a signature |
| a functional cast in a constant (`bool(_Bn::value)`) and a pack in a member's qualifier (`__enable_if_t<bool(_Bn::value)>...`) | `__and_fn` | `const-form`, `expand-type-pack` |
| a dependent template-id's defaults | `enable_if<C>::type` in a signature | completed in a scope binding the dependent arguments |
| functionally equivalent redeclarations ([temp.over.link]/5) | `<type_traits>`' `_Require<...> swap(_Tp&, _Tp&)` and `<bits/move.h>`'s `typename enable_if<...>::type swap(...)` | one template when the signatures with the templates' own parameters are equal |
| requires-clauses (P14 (b), the minimum) | `std::pair`'s constructors | the parser keeps a template head's requires-clause (`:requiresClause` on the template node) and a trailing one (`:trailingRequiresClause` on the function) as expression nodes in full-header mode (a requires-expression inside one not yet: the clause is then skipped as before); a function template's specialization and a class specialization's member whose constraint evaluates false (a constant expression after substitution, G2's constant evaluation, a failure not satisfied) are not candidates |
| `noexcept(e)` ([except.spec]/2) | `swap`'s `_GLIBCXX_NOEXCEPT_IF(...)` | evaluated where the function is declared; not noexcept when it cannot be |
| `if consteval` ([stmt.if]/4) | `std::__is_constant_evaluated` | the branch of a context not constant-evaluated |
| the library functions clang builds in (Builtins.td's `LIBBUILTIN`: `std::move`, `forward`, `move_if_noexcept`, `as_const`, `forward_like`, `addressof`, `__addressof`) | everywhere | the call folded to its argument (a cast, an address), no function emitted, as clang's CodeGen does at `-O0`; `std::__is_constant_evaluated()` (always_inline, which clang inlines at `-O0`) folded to false |
| a template-id after a qualifier in a template's body (`ns::X<T>::v`) | `std::is_integral<T>::value` | its arguments resolved where the name is written (a G3 bug, also outside headers) |
| `__builtin_va_list`, GNU attributes in a type's spelling | `<stdio.h>`'s `vprintf`, `__attribute__((noreturn))` | x86-64's `__va_list_tag[1]`; the attributes left out of the type |
| mangling | `swap`'s `enable_if<...>` | `::std::X<...>` as `St X I...E` in a dependent template-id; an unresolved `sr` name as written (no qualifier, a pack's elements one by one) |
| concepts ([temp.concept]) and requires-expressions ([expr.prim.req]) | `iterator_traits<_Tp*>`'s `requires is_object_v<_Tp>`, `construct_at`'s `requires { ::new((void*)0) _Tp(std::declval<_Args>()...); }`, `__iterator_traits`' concept-ids | the parser keeps a concept's constraint expression (`:constraintExpr`) and a requires-expression's parameters and requirements (simple, type, compound, nested) in full-header mode; a concept-id is its constraint's satisfaction for its arguments, a requires-expression true when each requirement analyzes (a compound one's return-type requirement not checked) |
| constrained partial specializations, their redeclarations, SFINAE in their arguments ([temp.spec.partial.match]/2) | `iterator_traits<_Tp*>` declared then defined; `__is_allocator<_Alloc, __void_t<..., decltype(...)>>` | one partial specialization per argument list (its definition kept); after deduction its constraint checked and its arguments substituted again (a failure no match); a cv-qualified parameter (`allocator<const _Tp>`) matched by type |
| a decltype operand that names a template's own parameters or function parameters | `-> decltype(make_move_iterator(__niter_base(__it.base())))`, `using _Dest = decltype(std::__niter_base(__result))` | a trailing return type resolved with the parameters in scope; an operand that does not analyze in a signature a dependent type, analyzed again at the specialization; a signature naming its own template refused inside (no recursion) |
| deduced return types ([dcl.spec.auto.general]/11) | `auto __to_address(const _Ptr&)` | the callee's body analyzed at the call; `auto` mangled `Da` |
| placement new ([new.delete.placement]) | `construct_at`'s `::new((void*)p) T(...)` | the reserved form `new (void*) T`: the object constructed at the argument, no operator new called, as clang's CodeGen does |
| range-based for over a class ([stmt.ranged]/1) | `for (int x : v)` | `auto&& __range`, `begin()`/`end()` members, `!=`, `++`, `*` by overload resolution |
| operator function templates (non-members by lookup and ADL, members) | `__normal_iterator`'s `operator==`/`operator-`, `ranges::iter_move`'s `operator()` | their specializations among the candidates; C++20's rewritten `!=` from `==` |
| `operator->` of a class ([over.ref]) | `unique_ptr`'s | applied until a pointer |
| inline namespaces ([namespace.def]/7) | `std::__cxx11::basic_string` | their members found in the enclosing namespace |
| a class template's parameters' defaults on an earlier declaration ([temp.param]/12) | `basic_string` (defaults in `<stringfwd.h>`) | kept when the definition comes; a header declaration that named a specialization before it was defined (`typedef basic_string<char> string`, `char_traits<char>` defined later) declared again after the headers |
| overloaded out-of-line member templates | `basic_string::_M_construct` for input and forward iterators | matched by their spellings with the parameters' names as positions |
| using-declarations of dependent bases' members | `using std::__is_integer<_Tp>::__value;` | declared in the specialization; the parser's unresolved member expressions keep their names |
| a defaulted default constructor's triviality ([class.default.ctor]/3) | `vector() = default` over `_Vector_impl`'s | non-trivial when a base's or member's is |
| `noexcept(e)`, pseudo-destructor calls, `base()` mem-initializers, static_assert in blocks, a parameter's attributes, `__func__`/`__PRETTY_FUNCTION__` | `destroy_at<int>`, `_Vector_impl() : _Tp_alloc_type()` | as the standard says (`__PRETTY_FUNCTION__` approximated by the name: no run reads the headers' assertion strings) |
| builtins | `__builtin_expect`, `__builtin_constant_p`, `__builtin_is_constant_evaluated`, `__builtin_unreachable`, `__builtin_addressof`, `__builtin_operator_new`/`delete`, `__builtin_{add,sub,mul}_overflow` | their values; the overflow ones' flag false (a divergence on overflow) |
| the key function ([basic.def.odr], Itanium §5.2.3) | `std::exception`, `std::logic_error` | a header's dynamic class whose key function is not defined here: its vtable and type_info external, the runtime's |
| the standard abbreviations (Itanium §5.1.8) | `std::allocator` | `Sa`, `Sb` |
| mangling of constrained templates (clang 23) | `std::pair`'s constructor templates | `Q <expression> E` after the template arguments: operators, calls, unresolved names with their arguments, the template's own parameters by depth and index (`TL0__`); a requires-expression in it left out (the names then differ) |

None of this is a port: the traits and the rest are written from the standard, clang's functions read as the model
(P6 (a) outside templates and the ABI); libstdc++ is read as input, never copied.

### G4.4 The samples and the numbers

The eight samples `test/cpp/codegen/hdr-*.cpp` (each with a header comment saying what it exercises): `hdr-cstdio`
(`printf`, `puts`, `snprintf` through `<cstdio>`'s `std::` and global names), `hdr-move` (`std::move`, `std::forward`),
`hdr-swap` (`std::swap` of ints, an aggregate and a class with its own moves), `hdr-traits` (31 values of
`<type_traits>`: `true_type`, `integral_constant`, `is_same`, `is_integral`, `is_pointer`, `is_class`, `remove_cv`,
`remove_reference`, `decay`, `is_enum`, `is_union`, `is_base_of`, `is_polymorphic`, `is_convertible`,
`is_constructible`, `add_pointer`, `conditional`, `is_reference`, `is_trivially_copyable`), `hdr-pair`
(`std::pair`'s constrained constructors, `std::make_pair`), `hdr-vector` (`std::vector<int>`) and `hdr-string`
(`std::string`).

| check (2026-11-05, light) | result |
|---|---|
| `bin/repr-cpp native-check samples` after P13 and at the branch's head (the parser's new fields, its full-header mode off for the converter) | 9 of 9 the same, 68 of 68 items |
| `bin/cppi codegen-check` over `test/cpp/codegen` (G1-G3's 123 samples, now through the headers parsed in full, and the eight `hdr-*`) | 128 of 130 pass all three levels; `hdr-vector` and `hdr-string` pass the run and native levels and are in `known.edn` (class `always-inline`); generate and lower 237-280 s (G3's 123 alone took about 2 minutes skimming); `known.edn` judged: 0 failing, gcc, clang and runs 12 s |
| the samples that were on clang's route (`test/cpp/samples`, `test/cpp/runtime/samples`: 16 files, `bin/cppi codegen-check` with clang++ -std=c++23) | **5 of 16 pass all three levels** (G2's 2: `bitfields`, `vexing`; now also `auto`, `classes`, `uncaught`); generate and lower 760 s (`<iostream>` parsed in full in most) |

The 11 others by their first refusal: `decls` and `layouts` have no `main` (the reference does not run); `p1-first`
`std::vector<int> v{1, 2}` (its initializer-list constructor: `std::initializer_list` not built; refused since the last
run, which chose another constructor and gave the wrong status); `io` and `hello` `<iostream>` (`std::cout` and the
streams' classes, G5); `atomics` `std::atomic<int>::fetch_add` (`<atomic>`'s builtins); `threads` and
`thread-throw` `<thread>` (`std::to_string`, `std::thread`'s invocation); `coroutine` `co_yield` (coroutines);
`ctor-throw` an attribute in an expression (`NativeAttr`); `library` a construct the parser declines in the main
file.

### G4.5 What is not done

- **Always-inline functions** (`__attribute__((__always_inline__))`, libstdc++'s `_GLIBCXX_ALWAYS_INLINE` on
  `__normal_iterator`'s operators, `allocator`'s members, `char_traits`' and the like): clang's AlwaysInliner inlines
  them even at `-O0` and emits none, and the functions only they called are then unused and not emitted either; we
  emit them, so `hdr-vector` and `hdr-string` pass the run and native levels and differ in their names (their class
  `always-inline` in `known.edn`; P16). Only `std::__is_constant_evaluated` is folded.
- **`<memory>`'s `std::unique_ptr`**: `__uniq_ptr_data` inherits `__uniq_ptr_impl`'s constructors (`using
  __uniq_ptr_impl<_Tp, _Dp>::__uniq_ptr_impl;`); inheriting constructors ([class.inhctor.init], clang's `CI2` mangling)
  are not built yet.
- A requires-expression in a constrained template's mangling (`rq ... E`: `construct_at`'s), the constrained
  parameters' mangling (`Tk`), a compound requirement's return-type requirement, `explicit(bool)` (read as not
  explicit), the subsumption ordering of constrained overloads (G3b).
- The overflow builtins' flag (false: no wider arithmetic yet), `__PRETTY_FUNCTION__`'s text.
- The parser's declines in the headers (`CPP_HDR_TRACE=1`: 12 on `<utility>`, 20 on `<vector>`, 28 on `<memory>`),
  among them `__extension__ template<>`, `_Tp(-1) < _Tp(0)` in a template argument list, `__make_integer_seq` and
  `__type_pack_element` (builtin templates), out-of-line nested classes' scope (`class locale::_Impl` naming
  `facet`), constrained template parameters (`template<__tuple_like _Tp>`); and the analysis's drops (G4.2).
- `resolve-uses` (the defaulted members' exception specifications the converter writes) is off in full-header mode:
  the headers' classes make its graph cyclic.

### G4.6 The staged plan

| stage | what | target | estimate |
|---|---|---|---|
| **G4a** (this branch) | G4.1-G4.3 | `<cstdio>`, `<utility>`, `<type_traits>` at three levels; `<vector>`, `<string>` at two | done |
| **G4b** | always-inline functions inlined as clang does at `-O0` (P16); inheriting constructors; the `rq` mangling; the header declines of G4.5; a cache of the full parse (P15) | `hdr-vector`, `hdr-string` at three levels; `<memory>`'s `unique_ptr`; `<algorithm>`, `<cstring>` | 2-3 |
| **G4c** | `std::initializer_list`, `<atomic>`'s builtins, `<iostream>`'s classes (with G5), coroutines, threads; the recorded `samples` list | the 9 samples with a `main` still on clang's route (G4.4) | 2-3 |

### G4.7 Proposed

**P15. The headers' cost.** The full parse has no cache: `<memory>`'s headers take about 50 s a file before the
analysis. (a) As built: parse them each time (the samples' light runs). (b) A cache of the full parse's nodes and
state keyed as the skim cache is (the tokens' digest, the parser's sources), the nodes written with the state's table;
costs the nodes' size on disk (several MB a header set) and the skim cache's code extended to nodes. (c) Parse a
header's body only when the analysis needs it (the plan's first wording: the skim's ranges kept, a definition
re-parsed on use); costs a second parser entry point and the parser's state at each range, but parses only what is
used. **Recommended: (b)**, in G4b; (c) not: the analysis needs most of `<type_traits>` anyway.

**P16. Always-inline functions.** (a) Inline them in our code generator as clang's AlwaysInliner does at `-O0`
(their bodies at the call, their names not emitted); costs an inliner over our LLVM forms (renaming, the return as a
branch). (b) Emit them `internal` and let the names level ignore internal always-inline definitions; costs a judge
that is no longer "every name ours is clang's". (c) Fold the few the headers use on hot paths (as `__is_constant_evaluated`
now). **Recommended: (a)**, in G4b: libstdc++ marks its iterators' and allocators' accessors always-inline under
`_GLIBCXX_ALWAYS_INLINE`; `hdr-vector` and `hdr-string` need it for their names.

## Decided (2026-11-05, the user: P15, P16)

P15 (b): the full parse's nodes cached as the skim cache does, in G4b; P16 (a): always-inline functions inlined in our code generator as clang's AlwaysInliner does at -O0, in G4b.

## G5. P15, P16 and the streams (branch `cpp-g5`, 2026-11-06)

Status: built on `cpp-g4` (P15 (b) and P16 (a) decided), for the user's acceptance. The aim of the stage (§6's G5, G4b's
and G4c's items first): the g++ run tests that include C++ library headers compiled by our Sema and code generator,
run with clang's route's output. Every item below has a sample of `test/cpp/codegen/` or a corpus file that failed
before and passes at all three levels of `bin/cppi codegen-check` (run, native, names).

Contents: G5.1 P16: LLVM's AlwaysInliner · G5.2 P15: the full parse's cache · G5.3 What the corpus asked ·
G5.4 The check and the numbers · G5.5 Sources · G5.6 What is not done · G5.7 Proposed.

### G5.1 P16 (a): always-inline functions inlined as LLVM's AlwaysInliner does

`arbace.cpp.inline` (`arbace/cpp/inline.ae`) is a port of llvm-project 23.1.4's `AlwaysInliner.cpp` (`AlwaysInlineImpl`)
and of the parts of `InlineFunction.cpp` -O0 IR without metadata exercises, over our LLVM forms, run on the module
after the code generator (`gen-module`): sema marks a function `:always-inline` from GNU's `always_inline` (an
attribute of any of its declarations), the code generator writes `alwaysinline` in its define's attributes, and the
pass inlines every call of a viable one (`isInlineViable`: not variadic, not calling itself) -- the callee's own
always-inline calls first --, then erases each such definition that is discardable (`linkonce_odr`, internal) and no
longer referenced, until none is (a comdat's functions erased together, as `filterDeadComdatFunctions` leaves them).
`InlineFunctionImpl`'s parts: the callee's locals renamed (`%x.iN`), its arguments the call's operands (a `byval` one
copied into an alloca, `HandleByValArgument`), the callee entry block's allocas moved to the caller's entry block
(the lowering wants them there), each `ret` a branch to the call's continuation whose result is a phi, the split
block's later phis' incoming edges renamed to its last piece. Through an `invoke` (`HandleInlinedLandingPad`): the
inlined landing pads take the outer pad's clauses and its cleanup flag, every call that may throw becomes an invoke to
the outer pad (`HandleCallsInBlockInlinedThroughInvoke`, the block split at it), and a `resume` a branch to the outer
pad's body, the outer pad split after its `landingpad` whose value is now a phi of it and of the resumed values
(`forwardResume`, `getInnerResumeDest`). With it `hdr-string` passes all three levels; `hdr-vector`'s always-inline
names are gone too.

Two of clang's -O0 rules came with it, as the names asked: **constant-folded branches**
(`CodeGenFunction::ConstantFoldsToSimpleInteger`, `EmitIfStmt`, `VisitBinLAnd`/`VisitBinLOr`,
`VisitAbstractConditionalOperator`): sema marks a condition that constant-folds (`fold-mark`: G2's evaluator, the
context not constant-evaluated, `std::is_constant_evaluated()` false) and the code generator writes only the live
branch when the dead one holds no label -- `char_traits`' `if (std::__is_constant_evaluated())` and the like no longer
reach `__gnu_cxx::char_traits`; and **`noexcept(e)` per specialization**: the native parser keeps a declaration's
`noexcept(e)` operand (full-header mode, as decltype's) and sema evaluates it with the parameters in scope, so
`vector::_S_use_relocate()` is true and `_M_realloc_append` takes clang's relocation path.

### G5.2 P15 (b): the full parse's cache

`arbace.repr.cpp.parse/full-headers!`: in full-header mode the leading run of header tokens is parsed in full, its
nodes and the parser's state (the scopes, the member templates' names, decltype's and noexcept's operands, the
unnamed counter, the cursor) written with the skim cache's table (`arbace.repr.cpp.skim`, its key and directory, a
`.full` file) and read back on a hit. Measured on `hdr-vector` (145k header tokens, light): preprocessing 20 s, the
full parse 9.6 s without the cache, 25 s when storing it (the table's writing), 8 s on a hit (6.6 s of it reading the
2.3 MB table back, which on the tools' host costs about what parsing does). The cache is built as decided but
**opt-in** (`CPP_FULL_CACHE=1`): on by default it would slow every first run by 15 s for 1.6 s on later ones. P17
proposes what would pay.

### G5.3 What the corpus asked

The corpus of this stage: gcc 15.2.0's `dg-do run` tests of `g++.dg` and `g++.old-deja` that include a C++ library
header (and no header beyond the standard ones), without options our route does not take (`-fno-exceptions`,
`-fno-rtti`, modules, coroutines' flags, `dg-additional-*`, `dg-require-*`, xfails, standards before C++20 only), that
clang accepts at `-std=c++23`: 206 files (`.tmp/cg/hdr-ok.txt` by the rule; not committed, P18). Measured on every
fifth (42 files: the measure is light, a whole run's 206 files would take about an hour of four jobs). Each item in
the general form the standard (or clang, where named) gives:

| what | where | as built |
|---|---|---|
| allocation and deallocation functions ([expr.new]/9-28, [expr.delete]/7-12; clang's `FindAllocationFunctions`, `resolveAllocationOverload`, `resolveDeallocationOverload`, `FindDeallocationFunction`, `FindUsualDeallocationFunction`, `FindDeallocationFunctionForDestructor`, `doesUsualArrayDeleteWantSize`) | `new7`, `delete7`, `aligned-new1/3`, `anew3`, `new5`, `eh/new2` | a class's own `operator new`/`delete` (static members, [class.free]/1), looked up in the class then globally, overload-resolved with the size, the alignment for an over-aligned type (`align_val_t`, retried without), the placement arguments; the implicit global declarations always there; the reserved placement forms called not (also `new (p) T[n]`, no cookie); the matching deallocation on a throwing initialization (the placement one by its parameters); a nothrow allocation's null check ([expr.new]/17); the array cookie when the destructor is not trivial or `operator delete[]` takes the size, its size `max(8, alignof(T))`; a deleting destructor's operator delete the class's |
| `alignas` on a class ([dcl.align]) | `aligned-new1` | the class's alignment at least the strictest one written (the parser keeps `alignas`'s argument) |
| `throw()` | `eh/spec10` | noexcept(true) (C++17's) |
| `std::initializer_list` ([dcl.init.list]/2-6, [over.match.list], [over.ics.list], [over.best.ics]/4) | `initlist1`, `initlist6`, `initlist-opt6`, `initlist106`, `p1-first` | the backing array (an alloca, its elements copy-initialized, destroyed at the full-expression's end or with a declared `initializer_list`), libstdc++'s object `{_M_array, _M_len}`; initializer-list constructors first, then all constructors with the elements (no copy constructor through a user-defined conversion when the one element is itself a list); a list argument's conversion its worst element's; a braced range of a range for |
| type-constraints ([temp.param]/4) | `p1-first` (`template<Num T>`) | checked as the concept's satisfaction for the argument; mangled `Tk <name>` (clang 17's) |
| a local name's substitutions (Itanium §5.1.6) | `hdr-string`'s `_Guard` | one table for the enclosing function's encoding and the entity |
| constraints' mangling (clang 23's `Q`) | `construct_at` | requires-expressions (`rq`, simple, type and nested requirements: `X`, `T`, `Q`), `nw`/`gs nw` with `pi`, `cv` casts, `sp` pack expansions, qualified unresolved names (`sr <levels> E`); a template parameter one substitution candidate in the constraint and the signature |
| unresolved names as written | `__relocate_a_1`'s `sr3std24__is_bitwise_relocatableIT_EE5value` | a dependent template-id keeps the count of its written arguments and its qualifier's namespaces (no part of its identity) |
| `<iostream>` | `hdr-iostream`, `io1`, the corpus's `cout` programs | one candidate set for an operator's members and non-members ([over.match.oper]/3, `resolve-overload*`); the address of a function template deduced from the target type ([temp.deduct.funcaddr]: `std::endl`); member classes defined outside their class template (`basic_ostream<_CharT,_Traits>::sentry`) and outside their class (`class locale::facet`: the parser's lookup scope and sema's); header declarations that failed on a type declared later retried until a round declares nothing more; `static_cast` in constants; type traits' text from their tokens (a macro's expansion: `_GLIBCXX_STD_FACET`); `__c_locale ()` a function type, not a trait; layouts kept only for finished classes |
| explicit instantiation declarations ([temp.explicit]/10; `CodeGenModule::shouldEmitFunction`) | `<ostream>`'s `extern template class basic_ostream<char>`, `extern template ostream& endl(ostream&)` | the class's (and its member classes') members and static data members, and the function specializations deduced from the declared type, are the library's: declared, not defined, but the always-inline ones |
| the standard abbreviations (Itanium §5.1.8) | `std::ostream::operator<<(int)`: `_ZNSolsEi` | `So`, `Si`, `Sd`, `Ss`, no substitution candidates |
| a static member's explicit specialization without an initializer ([temp.expl.spec]/14) | `__timepunct_cache<char>::_S_timezones` | a declaration |
| variadic functions | `pr45843`, `embed-11` | `va_start`, `va_end`, `va_copy` (LLVM's intrinsics), `__builtin_va_arg` (the parser keeps its operand and type) as clang's `X86_64ABIInfo::EmitVAArg` writes it for one INTEGER or SSE eightbyte; va_list's tag a complete trivial class |
| builtins | the streams | `__builtin_alloca`, `__builtin_strncpy` and other C library builtins, `__builtin_vsnprintf` |
| explicit destructor calls ([class.dtor]/14) | `alias-1`, `virtual8` | `p->~T()` the class's destructor, virtually through D1 |
| user-defined literals ([lex.ext]) | `udlit-embed-quote`, sample `udl` | the literal operator called with the integer's value (`unsigned long long`), the floating literal's (`long double`), the character, the string and its length; mangled `li <source-name>` |
| brace elision ([dcl.init.aggr]/16) | `pr95493-1`'s `array{x, y, z}`, sample `brace-elision` | a flat list's items distributed over subaggregates; a lone braced item of the class's own type copied (the native parser's semantic form braces it as if elided) |
| three-way comparison ([expr.spaceship], [class.compare.default], [over.match.oper]/3.4) | `spaceship-synth2b`, sample `spaceship` | the built-in `<=>` (`strong_ordering` for integers and pointers, `partial_ordering` for floating types: libstdc++'s one-byte value); defaulted member and friend `operator<=>` (the common category deduced for `auto`) and the `==` it declares; relational operators rewritten as `(x <=> y) @ 0`; a consteval constructor of an empty class evaluated (`__cmp_cat::__unspec`) |
| ref-qualified member functions ([dcl.fct]/4, [over.match.funcs]/5) | `pr67600`'s `basic_ostringstream::str() const &` | mangled `R`/`O`, told apart in redeclarations and by the object's value category |
| conversion sequences | `is_eq(a <=> b)`, `cout << ... << endl` with `<complex>` | a class converted to another by its conversion function (rank user-defined); an overload set holding a function template a non-deduced context ([temp.deduct.call]/6) |
| layouts | `basic_ostringstream` (`pr67600`) | a layout kept only when the class and every class it holds (bases, members, virtual bases) are finished: one computed while a virtual base was being defined was 48 bytes short |
| dynamic initialization's order ([basic.start.dynamic]; clang's `EmitCXXGlobalInitFunc`) | `ctor-throw.cpp`, sample `init-priority` | `init_priority(N)` initializations in a function `_GLOBAL__I_00000N` of their own, the `constructor(N)` functions first in `llvm.global_ctors` (clang's order at equal priorities); a variable's attributes no longer taken for its initializer |
| template corners | `pr95493-1`, `<sstream>`, `move_iterator` | a qualified member type found through the class's bases, dependent ones in the pattern too (`move_iterator<It>::iterator_category` from `__move_iter_cat`); a class template's later declaration's default arguments merged (`<iosfwd>`'s `istreambuf_iterator`, declared again by the headers' retry); a delegating constructor naming the injected-class-name; a constraint's satisfaction that needs itself not satisfied (a depth bound, as clang's "depends on itself") and a constructibility trait's cycle false |
| smaller | the corpus | `typeid` of arrays and functions (`__array_type_info`, `__function_type_info`), a `bool` global's zero (`i8 0`), GNU's `__alignof__`, a braced range of a range for (`std::initializer_list<E>`) |

### G5.4 The check and the numbers

| check (2026-11-06, light) | result |
|---|---|
| `bin/cppi codegen-check` over `test/cpp/codegen` (130 of G1-G4 and nine new: `hdr-iostream`, `va-args`, `tmpl-member-class`, `initlist`, `newdelete-class`, `udl`, `brace-elision`, `spaceship`, `init-priority`) | **138 of 139 pass all three levels** (`hdr-string` now among them); `hdr-vector` passes run and native, known (`trailing-requires`, G5.6); generate and lower 308 s, gcc, clang and runs 12 s |
| `bin/repr-cpp native-check samples` and `native` after the parser's changes (its new fields underscored, a qualified class definition's lookup scope) | 9 of 9 the same, 68 of 68 items; 1 of 1, 30 of 30 |
| the corpus's every fifth (42 files, 2 jobs) at the branch's start (G4's head) | **7 of 42** pass all three levels |
| the same at the branch's head | **32 of 42** (76 %); generate and lower 661 s for the 42 (two jobs for the runs), gcc, clang and runs 6 s; 25, 29 and 30 at intermediate measures of the branch |
| the samples that were on clang's route (`test/cpp/samples`, `test/cpp/runtime/samples`: 16 files) | **7 of 16** (G4: 5): `auto`, `bitfields`, `classes`, `p1-first`, `vexing`, `uncaught`, and `ctor-throw` (init_priority, constructor attributes); `decls` and `layouts` have no `main`; `hello` an overload set's conversion among `<string>`'s and `<map>`'s `operator<<` candidates, `io` `iterator_traits` of a class iterator in `std::sort`'s internals, `atomics` `__atomic_fetch_sub`, `threads` and `thread-throw` `std::thread`'s invocation, `coroutine` `co_yield`, `library` a parser decline |

The corpus sample's files that do not pass at the branch's head, by their first refusal: coroutines (`func-params-08`,
`exceptions-test-01-n4849-a`, `co-yield-04-complex-local-state`); `_Destroy`'s local typedef of `iterator_traits<It>::value_type`
for a class iterator (`asan/pr90570`, `jason/template31`: `std::vector` of a class, G5.6); a class through `...`
(`pr45843`); C++23's multi-argument `operator[]` (`subscript6`, a parser decline); GNU's `__complex__` (`unchanging1`'s
`std::complex<double>`); `embed-11` defines a constant global clang folds away (names); `jason/thunk3`'s reference run
fails (clang's route).

The traces added for the work on headers: `CPP_OP_TRACE=1` (an operator's candidates when none is viable),
`CPP_LOOKUP_TRACE=NAME` (each scope an unqualified lookup of NAME tries, a variable NAME's declaration, the
using-directives), `CPP_EXTERN_TRACE=1` (the explicit instantiation declarations of functions and the
specializations each names), `CPP_LAYOUT_TRACE=NAME,...` (the layouts of the classes so named), `CPP_EQ_TRACE=1` (the
functional-equivalence check of function template redeclarations).

### G5.5 Sources

- llvm-project at `llvmorg-23.1.4` (commit `7a941dfb2877a0b8cf30323fefa85ad019e0b0f5`, the pinned tarball, extracted into
  the worktree's `.tmp/llvm/`): `llvm/lib/Transforms/IPO/AlwaysInliner.cpp`, `llvm/lib/Transforms/Utils/InlineFunction.cpp`,
  `llvm/lib/Analysis/InlineCost.cpp` (ported, G5.1; Apache-2.0 with the LLVM exception, LICENSE.md, doc/ARBACE.md, the
  file's notice); `clang/lib/CodeGen/CodeGenFunction.cpp` (`ConstantFoldsToSimpleInteger`, `ContainsLabel`),
  `CGStmt.cpp` (`EmitIfStmt`), `CGExprScalar.cpp` (`VisitBinLAnd`, `VisitBinLOr`, `VisitAbstractConditionalOperator`),
  `clang/lib/Sema/SemaExprCXX.cpp` (`FindAllocationFunctions`, `resolveAllocationOverload`, `UsualDeallocFnInfo`,
  `resolveDeallocationOverload`, `FindDeallocationFunction`, `FindUsualDeallocationFunction`,
  `FindDeallocationFunctionForDestructor`, `doesUsualArrayDeleteWantSize`, `ActOnCXXDelete`), `clang/lib/AST/ItaniumMangle.cpp`
  (`mangleRequirement`, the `RequiresExprClass` and `CXXNewExprClass` cases) read as the models of the rest, which is
  written anew in Arbace's structures (P6 (a) outside templates and the ABI).
- The standard (ISO/IEC 14882:2024): [expr.new], [expr.delete], [basic.stc.dynamic], [dcl.init.list], [over.match.list],
  [over.ics.list], [over.best.ics], [temp.param], [temp.deduct.funcaddr], [temp.deduct.call], [temp.explicit],
  [temp.expl.spec], [class.nest], [class.free], [class.dtor], [lex.ext], [dcl.init.aggr], [expr.spaceship],
  [class.compare.default], [over.match.oper], [dcl.fct], [dcl.align], [except.spec].
- The Itanium C++ ABI §5.1.3 (literal operators), §5.1.5 (ref-qualifiers), §5.1.6 (local names), §5.1.8 (the standard
  abbreviations), §2.7 (array cookies), §2.9.5 (type_info classes); the System V x86-64 psABI §3.5.7 (va_list, va_arg).
- gcc 15.2.0's testsuite (the tarball pinned for repr/c, its extraction in the main checkout's `.tmp/repr-c/`): the corpus
  of G5.3, read as input; libstdc++ 15.2.0's headers on the host, read as input, never copied.

### G5.6 What is not done

- **A non-template member's trailing requires-clause in a class template** (`_Vector_impl() requires
  is_default_constructible_v<_Tp_alloc_type>`, clang 23's `Q` after the parameters): the constraint names the class's
  member typedef, which only the pattern's symbolic instantiation (the current instantiation, a dependent class) can
  mangle as clang does (`N9__gnu_cxx14__alloc_traitsIT0_NS5_10value_typeEE6rebindIT_E5otherE`); `hdr-vector` stays
  known for it (P19).
- Function explicit instantiation declarations with explicit template arguments (`extern template bool
  has_facet<collate<char>>(const locale&)`: the arguments are not deduced from the declared type; their specializations
  are emitted when used).
- The corpus's refusals of G5.4's last list; among them coroutines (frames and the coroutine intrinsics: the lowering
  refuses `llvm.coro.*` too), GNU's `__complex__` types (`std::complex`'s), a class through `...`, C++23's
  multi-argument `operator[]` (the parser declines it), a local typedef of a dependent iterator's `value_type` in a header
  function template (`_Destroy`'s `_Value_type`: the partial specialization `iterator_traits<_Tp*>` not chosen there,
  not understood yet), `istreambuf_iterator` named without arguments in its own class.
- The cost: G5 made the analysis slower (codegen-check's 135 samples: generate and lower 321 s at G4's head with the
  new samples, 441 s after the layouts were no longer kept for unfinished classes, 308 s once concept satisfactions
  were memoized).

### G5.7 Proposed

**P17. The headers' cost, again.** The full parse's cache (P15 (b), G5.2) does not pay on the tools' host; the
preprocessor is the larger part (20 s of `hdr-vector`'s 44). (a) As built: the cache opt-in, every run preprocesses and
parses. (b) A cache of the preprocessor's tokens of the leading `#include` run, keyed by the include closure's files'
digests and the configuration (the run's tokens are then the parse cache's key at no cost, and both caches pay); costs
recording the closure (the preprocessor knows every file it opened) and a flat token table (cheaper to read than the
parse's graph). (c) A compiled executable for the C++ crop's namespaces (out of `gate_key` today: evaluated from the
checkout), which would speed the parser and the analysis several times; costs the crop's namespaces in the program's
build. **Recommended: (b)**, then (c) when the crop joins the gate (P7).

**P18. The headers' corpus.** (a) Measured on every fifth file of the rule's 206, the list not committed (as built).
(b) The list and its recorded results committed (`test/cpp/codegen/gxx-headers.tsv`, `gxx-headers-results.edn`, a known
file), `bin/cppi codegen-check --corpus gxx-headers` as P11's template corpus; costs about an hour of four jobs for a
whole run (the `<iostream>` files take a minute each), so a night run. **Recommended: (b)** with P11's corpus, both run
at night.

**P19. The current instantiation.** (a) As built: a class template's members are analyzed only in specializations;
what clang mangles from the pattern (a constrained non-template member's `Q`) is left out. (b) Analyze a class template's
pattern once as the current instantiation (a dependent class whose members' types stay dependent: what
`arbace.cpp.template`'s symbolic scope does for function templates' signatures, extended to member typedefs and bases);
costs a dependent class type in sema's registry and its member lookups. **Recommended: (b)** in G6, with the remaining
template corners; `hdr-vector` needs it for its last name.

### G5.8 The staged plan

| stage | what | target | estimate |
|---|---|---|---|
| **G5a** (this branch) | P15 (b), P16 (a), G5.3 | `<iostream>`, `<sstream>`, `<initializer_list>`, `<compare>` programs at three levels; the corpus sample of G5.4 | done |
| **G5b** | P17, P18, P19; coroutines (frames, `co_await`/`co_yield`, the lowering's `llvm.coro.*` or our own frame lowering); `__complex__`; classes through `...`; the remaining refusals | the headers' corpus at 80 %; the existing 16 samples | 3-4 |
| **G5c** | the runtime's units by our route (libsupc++ and libstdc++-v3/src compiled without clang, §6's G5) | `bin/cppi runtime-build --route ours` | 3-5 |

## Decided (2026-11-06, the user: P17-P19)

- **P17 (b), then (c):** a preprocessor token cache keyed by the include closure now; compiling the C++ crop's namespaces into the
  executable later.
- **P18 (b):** the 206-test headers corpus list committed with recorded results, as a night check beside P11's template corpus.
- **P19 (b):** a class template's pattern analyzed as a dependent class (the current instantiation), in G6.

## G5c. The runtime by our route (branch `cpp-runtime-own`, 2026-11-08)

Status: built on `next` bd33e27cc by the user's decision CP240 (c) (2026-11-07): libsupc++, the part of C++'s runtime the
language itself calls (the Itanium ABI's exceptions, RTTI, guards, `new`/`delete`, the vector helpers, the terminate and
unexpected handlers), compiled by Arbace's route -- the native parser, `arbace.cpp.sema` and `arbace.cpp.codegen` --
from the pinned gcc 15.2.0 tarball's sources, so that clang's IR leaves the runtime's libsupc++ part. libstdc++-v3/src
(134 units with libstdc++exp's: strings, streams, locales, `<chrono>`, `<filesystem>` ...) stays clang's IR, from the pack (G5c's next step).

Contents: G5c.1 What the units asked · G5c.2 The proof · G5c.3 The build · G5c.4 The numbers · G5c.5 What is not done ·
G5c.6 Sources · G5c.7 Proposed · G5c.8 The night's commands · G5c.9 As built: P20 (b), P21 (b), P22 (b) (cpp-props).

### G5c.1 What the units asked

cpp-90 measured 41 of 62 units generating (its 62 are units.edn's C++ libsupc++ units but `atomicity.cc`'s, which is
63 with it; `eh_personality.cc` is excluded by design, CP86 (c): the pending-flag layer replaces the personality
routine). "Generated" was not "correct": a names level (each unit's external names against the oracle's IR, both ways)
found most of the generated units wrong -- whole units empty, vtables missing. Each item below in the rule's general
form, with the unit that asked and a sample of `test/cpp/codegen` where one was written:

| what | unit | as built |
|---|---|---|
| using-directives' nominated sets each namespace once ([namespace.udir]/2-3) | the type_info units (tinfo.h: an unnamed namespace inside `__cxxabiv1` nominating `abi` = `__cxxabiv1`): a stack overflow | `own-names` keeps the namespaces seen; sample `using-cycle` |
| GNU's restrict-qualified references | `class_type_info.cc`'s `__upcast_result &__restrict` | the parser's ptr-operators and the type reader take `__restrict` after `&`/`&&` |
| GNU's `__atomic` builtins (clang's AtomicExpr) | `guard.cc`, `new_handler.cc`, `atomicity.h` | the parser keeps the operands (`:_args`, its dump's children untouched); sema types each form (`_n` and generic, exchange, compare-exchange, fetch-op and op-fetch, test-and-set, clear); the code generator writes LLVM's atomic load, store, `atomicrmw`, `cmpxchg` at the order's LLVM ordering for a constant order (an order the operation does not take emits nothing, as clang's), a switch over the C ABI's values for a run-time one, the compare-exchange's failure set; also as a call in a template's pattern (std::atomic's members); sample `atomic-builtins` |
| the bit builtins | `new_opa.cc` (`<bit>`'s `__has_single_bit` over `__builtin_popcountg`) | popcount, clz, ctz (the generic `g` forms and their fallback operand), bswap: llvm.ctpop/ctlz/cttz (zero undefined)/bswap; sample `bit-builtins` |
| `__builtin_offsetof` | `dyncast.cc`, `eh_ptr.cc` | the type and the designator kept by the parser, folded from the layouts (a base member at its base's offset, an index times its element's size); sample `offsetof-builtin` |
| constant casts and comparisons | `<bit>`'s `__bit_ceil` (`__int_traits<unsigned>::__digits` was 31: `(_Value)(-1) < 0` true) | `const-int` wraps a cast to its type and compares unsigned operands unsigned |
| a preprocessor error is the compile's | every `eh_*.cc`: unwind-cxx.h's quoted `"unwind.h"` was not found and the rest of the unit silently parsed to nothing | `gen-file` fails on the preprocessor's first error (class `pp`); the runtime's units read gcc's unwind.h, the target tree's compiler include directory, by `-iquote` (P20) |
| typedef names for linkage ([dcl.typedef]/9) | unwind.h's `typedef enum {...} _Unwind_Reason_Code` (every unwind-dependent declaration dropped) | the unnamed enumeration or class just declared takes the typedef's name |
| GNU's `mode` attribute | cxxabi.h's `typedef int __guard __attribute__((mode (__DI__)))` (mangled `Pi`, clang's `Pl`) | the integer type of the mode's width and the base's signedness, the first of char, short, int, long (clang's getIntTypeForBitwidth) |
| `thread_local`, `__thread` ([basic.stc.thread]) | `eh_globals.cc`'s per-thread globals | `:tls` on the variable, block scope implying static; the globals `thread_local`; a dynamic initialization or destructor refused (class `tls`) |
| qualified definitions of extern variables ([basic.def]/2) | `new_handler.cc`'s `std::nothrow`, `eh_term_handler.cc`'s `__terminate_handler` | the definition no longer extern |
| a qualified namespace member's body ([basic.lookup.unqual]/4) | `eh_terminate.cc`'s `std::terminate() { ... get_terminate() ... }` | looked up in its namespace, then the enclosing ones (a definition at namespace scope; a template's keeps its bindings) |
| a qualified conversion function's type | `eh_ptr.cc`'s `exception_ptr::operator __safe_bool()` | the conversion-type-id looked up in the class first |
| key functions (Itanium §5.2.3) | every class whose destructor or first virtual function the unit defines: `bad_alloc.cc`, the type_info units ... | `vtable-home`: the vtable, VTT and type_info strong where the key function is defined, emitted whether used or not; external where it is defined elsewhere; linkonce_odr without one (a class template's specialization too) |
| the constructors' and destructors' variants | the same | a defined constructor's or destructor's C1/D1 (an alias of C2/D2 without virtual bases), a virtual destructor's D0, strong; no C1 for an abstract class; an out-of-line `= default` (`nested_exception::~nested_exception() = default`) a definition, its members' destructors used |
| the fundamental types' type_info (Itanium §2.9.4) | `fundamental_type_info.cc` (`__fundamental_type_info`'s key function) | `_ZTI`/`_ZTS` of the 25 fundamental types, their pointers and pointers to const |
| GNU's `used` | `eh_ptr.cc` (exception_ptr.h's `_GLIBCXX_EH_PTR_USED`) | a used inline function defined and emitted |
| unnamed namespaces' classes ([basic.link]/4) | `vec.cc`'s `uncatch_exception`, `atexit_thread.cc`'s local `key_s` | internal, with their members |
| clang's -O0 control flow | `guard.cc` (code after the futex path's `return` called `__set_and_release`, which clang never emits) | no statement after a terminator unless it holds a label (an if whose branches both end leaves no continuation); `unreachable` after a call of a `noreturn` function (`[[noreturn]]`, `__attribute__((noreturn))`, the C library's abort, exit, _Exit, quick_exit, which are nothrow too) |
| `__cxa_end_catch` may throw | `new_op*nt.cc` (noexcept with `catch (...)`), `vec.cc` (`catch (...) { ...; throw; }`) | for a catch-all or a class type an invoke: on the normal path to the enclosing pad (a noexcept function's terminate), while unwinding to a terminate pad (`__clang_call_terminate`) |
| a static member function template's call in a template argument | `eh_alloc.cc`'s `std::pair<string_view, int>` (C++17's pair: `enable_if<_PCCP::template _ConstructiblePair<_U1, _U2>() && ...>`) | evaluated (the specialization instantiated, constexpr-called), dependent in a pattern's signature and mangled `cl sr <levels> E <name> <args> E` (sample in `.tmp`, the mangling equal to clang's); **not enough for std::pair yet** (G5c.5) |
| a throwing static local's initialization (Itanium §3.3.3) | (found by `rt-guard`) | `__cxa_guard_abort` on the EH path, so a later pass tries again |
| `typeid(*p)` of a null pointer ([expr.typeid]/3) | (found by `rt-eh`) | `__cxa_bad_typeid` |

### G5c.2 The proof

"Generated" is the first of four levels, each checked on every unit:

1. **names**: the external names the unit's IR defines are those the oracle's IR defines (clang's, from the pack), none
   missing, none more (`arbace.cpp.check/defined-names`; `runtime-judge`'s level for a unit by our route, `names.txt`).
2. **the runtime build's levels** (runtime-build): our IR read and printed by repr/llvm, lowered by `arbace.cpp.lower`,
   the lowered C compiled by gcc (the oracle tier's judge of C, CPPI_GCC=1).
3. **behaviour, unit by unit** (`bin/cppi runtime-swap`, new): the programs of a codegen-check's work directory (each
   sample's lowered C by clang's IR and by ours) linked with the runtime built from clang's IR, the baseline; then, for each
   unit by our route, with that unit alone replaced by ours (the exceptions', RTTI's, guards', allocations' and headers'
   samples); then with all of them over every program: every status and stdout the baseline's.
4. **the samples on the runtime by our route**: codegen-check with `--runtime` the runtime by our route, its three
   levels (run against clang's route, native, names).

Three samples were written for the runtime: `rt-eh` (throw and catch by type, base, pointer, catch-all, rethrow, unwinding
through destructors with `uncaught_exceptions`, the current exception's type, `what()`, the bad_* the runtime throws),
`rt-rtti` (dynamic_cast down, across, to void, through virtual bases; typeid's names and equality; catching through
pointer conversions, qualification, to `void*`, enums, function pointers) and `rt-guard` (a static local whose initializer
throws, then succeeds; every `new`/`delete` form, over-aligned, nothrow; the new-handler; exception_ptr; nested
exceptions; `set_terminate`'s handler).

### G5c.3 The build

`bin/cppi runtime-build` takes `CPPI_RUNTIME_ROUTE` (`bin/cppi`'s `runtime_ours`): `clang` (the default until CP403 (a)), `ours`
or `auto` (the default since CP403 (a), queue 406, 2026-11-19: doc/cpp/CPP-SURVEY.md, "As built: queue 406"). With `ours` or `auto` the C++ units whose name begins with `CPPI_RUNTIME_OURS` (default `libsupc++/`) are
generated by our route in one process (`arbace.cpp.codegen gen-batch`) from the tarball's sources (extracted by
`runtime_ours` when missing; no clang), with rt_ir's preprocessor options (`-D`, `-I`; the `-f` flags are the compiler's)
and `-iquote` of the tree's gcc include directory; each unit's IR is its `x.ll`, the oracle's kept beside as `clang.ll`
(the names level's reference), `route` naming which made it. A unit our route refuses fails the build under `ours` and
keeps the oracle's IR under `auto`. The rest of the build is unchanged (the round trip through LLVM forms, the lowering,
gcc in the oracle tier or ci's checker from the records); the IR pin `test/cpp/runtime/ir.sha256` is checked for the
oracle's IR alone (the IR of ours listed in `OUT/ir-ours.sha256`); the runtime's key holds the route, the prefix and the
route's namespaces (sema, abi, codegen, template, inline, repr/cpp, the target tree's description), so the runtime-ci
and lower-check that read it rebuild after a change of the code generator. `runtime-judge` adds the names level (§G5c.2).

### G5c.4 The numbers (2026-11-08, light: one process, CPPI_THREADS=1, under 1.5 GiB)

| check | result |
|---|---|
| libsupc++'s C++ units generated by our route (`gen-batch` over units.edn's 63) | cpp-90: 41 of 62; **62 of 63** (`eh_alloc.cc` refused: G5c.5) |
| the names level (each unit's external names against the oracle's IR, both ways) | at the branch's start about 10 of the 46 generating units equal; **62 of 62** |
| `CPPI_GCC=1 CPPI_RUNTIME_ROUTE=auto bin/cppi runtime-build` (the pack's IR for the 136 others: libstdc++-v3/src's 134, eh_alloc and libiberty's C demangler) | **198 of 198 pass, 62 by our route**; IR 152 s, read, print and lower 522 s, gcc 56 s (12 min) |
| the same from the records (no gcc, ORS27), clang, clang++, clang-23, g++, gcc, cc, cpp, c++, cc1, cc1plus, ld, as, opt, llvm-as, lli, llc stubs exiting 99 first on PATH | **198 of 198, 62 by our route, the stubs' log empty** (11 min) |
| `bin/cppi runtime-swap` over codegen-check's 149 samples' programs by both routes (298 programs, 280 exiting 0) | **each of the 62 units alone: no run of the 102 exceptions', RTTI's, guards', allocations' and headers' programs changes; all 62 at once: no run of the 298 changes** (9 min). Its first run found eh_ptr.cc alone crashing rt-guard (GNU's `aligned`, fixed) |
| `bin/cppi codegen-check --runtime` the runtime by our route (run against clang's route, native, names) | **148 of 149 pass, 1 known** (hdr-vector, P19); at the branch's base 11 of the 145 samples failed on today's tree (G5c.1's last three sema items) |
| `bin/repr-cpp native-check samples` and `native` after the parser's changes | 9 of 9, 68 of 68 items; 1 of 1, 30 of 30 (both controls fail as they must) |

Independence (`bin/crops-score --crop cpp`): unchanged in the checkout's facts (54.3: read 0.148, eval 0.5, AOT 0.5, print 1,
judge 0.568), since the gate's `cpp-runtime` still builds the runtime from the pack alone. With P21 (b) (`auto` the
runtime's default), the runtime's cases are 62 units by our route (1) and 136 from the pack (0.5), 0.657; a program's eval
and AOT the mean of its IR's (the pack, 0.5, until cpp-indep's CP240 (b) makes our route the programs' default) and the
runtime's: **0.578 each, independence 57.5**; with CP240 (b) too, the programs our route generates at 1.

### G5c.5 What is not done

- **eh_alloc.cc** (the emergency pool, `__cxa_allocate_exception`'s): its tunables' `std::pair<std::string_view, int>
  tunables[]{{"obj_size", 0}, ...}` finds no viable constructor at gnu++17. Every templated constructor of std::pair drops
  out (its C++17 `enable_if<_PCCP::template _ConstructiblePair<_U1, _U2>() && ...>` conditions); the same constructors
  copied into a class template of the main file or of a header of one's own are viable, and the condition alone
  evaluates true. Not found by the day's end; under `auto` the unit keeps the oracle's IR.
- **The terminate pad of a cleanup while unwinding** is written for `__cxa_end_catch` alone; a destructor that may
  throw called while unwinding is still a call, not an invoke to a terminate pad (clang's EH cleanups' terminate scope).
- **thread_local with dynamic initialization or a destructor** is refused (class `tls`): clang's TLS wrapper and init
  functions (`_ZTW`, `_ZTH`, `__cxa_thread_atexit`) are not written. No libsupc++ unit has one.
- **libstdc++-v3/src** (134 units with libstdc++exp's): P22.
- **A pointer to member's type_info** (`__pointer_to_member_type_info` objects for `typeid(int S::*)` and a thrown member
  pointer): refused in programs (class `rtti`); `pmem_type_info.cc` itself, the class's methods, is by our route.

### G5c.6 Sources

- The Itanium C++ ABI (§2.5 vtables, §2.9.4 the fundamental types' type_info, §2.9.5 the type_info classes, §3.3.2-§3.3.3
  the guards, §5.1 mangling: `cl`, `sr`, template-param-decls, §5.2.3 key functions), the System V x86-64 psABI, ISO/IEC
  14882:2024 ([namespace.udir], [basic.lookup.unqual], [basic.def], [basic.link], [dcl.typedef], [dcl.stc], [basic.stc.thread],
  [expr.typeid], [except.terminate], [stmt.dcl], [class.abstract], [support.types.layout], [conv.integral], [expr.rel]),
  GCC 15's manual (Common Type and Variable Attributes: `mode`, `aligned`, `used`, `noreturn`; Built-in Functions for
  Memory Model Aware Atomic Operations; Other Built-in Functions: the bit builtins).
- gcc 15.2.0's `libstdc++-v3/libsupc++` and `config/` (the pinned tarball): the program compiled, read as input (GPL with
  the GCC Runtime Library Exception; nothing of it copied into Arbace's sources).
- clang 23.1.4's IR of the units (the pack `cpp-runtime-ir`) as the oracle: its names and its -O0 shapes (the atomics'
  orderings and switches, the end-catch invokes, the terminate pads, dead code after a return). No clang source ported.

### G5c.7 Proposed

**P20. Clang's resource headers in the target tree.** (Decided (b), the user 2026-11-08; built, G5c.9.) The tree (CP241 (b)) lists clang's resource directory
(`/usr/lib/llvm23/lib/clang/23/include`) among the native parser's roots, but makes nothing there: a header only the
compiler provides (`<unwind.h>`, also `<stddef.h>`'s and `<stdarg.h>`'s own when musl's are not reached first) is not
found. The runtime's units read gcc's unwind.h through `-iquote` of the tree's gcc include directory (as g++ builds
libsupc++), which is ABI-equal to clang's. (a) As built. (b) The tree makes clang's resource headers from the pinned
llvm-project tarball (Apache-2.0 with the LLVM exception; the description's `:layout` as for gcc's, its oracle the
installed directory byte for byte), so the native parser reads what clang reads; costs a day, on cpp-target's line (its
files). **Recommended: (b)**, on cpp-target.

**P21. The runtime's proof in the gate.** (Decided (b), the user 2026-11-08; built write-ahead, G5c.9.) (a) As built: `bin/cppi runtime-swap` and the names level by hand (both need
gcc, the oracle tier's, CPPI_GCC=1). (b) The names level in `cpp-runtime` (records mode, no gcc: it reads the pack's IR),
`CPPI_RUNTIME_ROUTE=auto` the default of runtime-build, and `runtime-swap` an oracle-tier step (`test/gate/crops/cpp-oracle.edn`);
costs the key's new lines (the route's namespaces: a change of sema or the code generator rebuilds the runtime, about 12
minutes light) and one oracle-tier step of about 20 minutes. **Recommended: (b)** after the night's runs below hold.

**P22. libstdc++-v3/src by our route.** (Decided (b), the user 2026-11-08; the first measure in G5c.9.) The other 134 units (the strings', streams', locales' explicit instantiations,
`<chrono>`, `<filesystem>`, `<format>`): each instantiates the library's templates at scale, which is G5b's (P19's current
instantiation, the template corners of G5.6) and C++17's `std::pair` (G5c.5). (a) Unit by unit after G5b, measured by
`CPPI_RUNTIME_OURS=c++11/` (then `c++98/`, `c++17/` ...) `bin/cppi runtime-build` (the same route, the names level and runtime-swap the proof). (b) As (a),
first the c++98/ and c++11/ units that instantiate nothing (`functexcept.cc`, `functional.cc`, `hash_c++0x.cc`,
`system_error.cc`, `random.cc` ...), measured tonight. **Recommended: (b)** then (a).

### G5c.8 The night's commands (one process at a time, about 1.5 GiB each)

```
export ARBACE_HOST_EXE=/root/arbace/target/seed/amd64/arbace GOMEMLIMIT=1400MiB CPPI_THREADS=1 CPPI_JOBS=1
CPPI_GCC=1 bin/cppi runtime-build .tmp/cppi/rt-clang                                   # the baseline from the pack (12 min)
CPPI_GCC=1 CPPI_RUNTIME_ROUTE=auto bin/cppi runtime-build .tmp/cppi/rt-ours            # expect 198 of 198, 62 by our route (12 min)
bin/cppi codegen-check .tmp/cppi/cg --runtime .tmp/cppi/rt-ours --jobs 2                # expect 148 of 149, hdr-vector known (6 min)
bin/cppi runtime-swap .tmp/cppi/rt-clang .tmp/cppi/rt-ours .tmp/cppi/cg .tmp/cppi/swap   # expect every unit same (9 min)
CPPI_RUNTIME_ROUTE=auto bin/cppi runtime-build .tmp/cppi/runtime                       # records mode, no gcc: the gate's runtime
CPPI_RUNTIME_ROUTE=auto bin/cppi runtime-ci .tmp/cppi/runtime                          # ci's library from it (3 GB at 2 threads: 1 job, CP246)
CPPI_RUNTIME_ROUTE=auto bin/cppi check --only test/cpp/essential-check.txt             # the essential run check on ci with it
CPPI_RUNTIME_ROUTE=auto bin/cppi check && CPPI_RUNTIME_ROUTE=auto bin/cppi check --aot   # as test/cpp/check{,-aot}-linux-amd64.edn
CPPI_RUNTIME_OURS=c++11/ CPPI_RUNTIME_ROUTE=auto CPPI_GCC=1 bin/cppi runtime-build .tmp/cppi/rt-src11   # P22 (b)'s measure
```

### G5c.9 As built: P20 (b), P21 (b), P22 (b)'s first measure (branch `cpp-props`, 2026-11-08)

The user's decisions of 2026-11-08 on G5c.7; doc/cpp/CPP-SURVEY.md, "As built: cpp-props", has the rest of the branch.

**P20 (b): clang's resource headers in the target tree.** `test/cpp/target.ae`'s `resource-plan` reads
`clang/lib/Headers/CMakeLists.txt` of the pinned llvm-project-23.1.5 tarball (test/cpp/corpora.edn's `llvmorg-23.1.5`) as data: its
`set(NAME ...)` lists at the file's scope (a function's body, `copy_header_to_output_dir`'s and `clang_generate_header`'s, sets its
products only when the build calls it), `${VAR}` expanded, and its `install(FILES ... DESTINATION ${header_install_dir}[/SUB] ...
COMPONENT clang-resource-headers)` commands, each file at its destination under its base name; `cpp/target/clang.edn`'s
`:resource-include` names the directory, the source, the CMake file, the component and `:not-made`, the 11 TableGen products the build
makes (arm_neon.h, arm_fp16.h, arm_bf16.h, arm_vector_types.h, arm_sve.h, arm_sme.h, arm_mve.h, arm_cde.h, riscv_vector.h and HLSL's two
`.inc`: ARM's, AArch64's, RISC-V's and HLSL's, which no read of an x86-64 program reaches). `bin/lib/repr-cpp/sysroot.bash`'s
`cpp_target_tree` extracts `clang/lib/Headers`, lays it out under `usr/lib/llvm23/lib/clang/23/include` (resource-tree.sha256 beside),
the tree's key holding `cpp/target/clang.edn` and the llvm pin; the full sysroot (every search directory a root) then holds what clang
reads. `bin/repr-cpp target oracle` compares it too (`oracle-resource`, the :not-made exempt and held to the installed directory):
**285 of the 296 installed files made, all 285 equal byte for byte, the 11 TableGen products not made, as described** (libstdc++'s 834
and gcc's 151 equal as before). The runtime's units by our route no longer take `-iquote` of gcc's include directory: unwind-cxx.h's
`"unwind.h"` is clang's, as the pack's IR read it (`cpp/target/clang.edn` in the runtime's key). Checked on the new tree (stubs first on
PATH, their log empty, but native-check's oracle): `CPPI_RUNTIME_OURS=libsupc++/eh_ CPPI_RUNTIME_ROUTE=ours bin/cppi runtime-build`
(records mode, no gcc): 198 units, 197 pass, **13 eh_* units by our route with clang's unwind.h, the names level equal for all 13**,
eh_alloc.cc refused as before (G5c.5: std::pair's constructors; IR 103 s, read, print and lower 488 s); `pp-check --essential`,
`roundtrip-corpus gxx-dg-essential --judge own` (33 of 33) and `samples --judge own` (9 of 9) as before; `native-check samples` 9 same, 68
of 68 items, `native` 1, 30 of 30, the controls failing as they must.

**P21 (b), write-ahead.** `bin/cppi`'s `cppi_runtime_route_default` (`rt_route`, read wherever the route was `${CPPI_RUNTIME_ROUTE:-clang}`:
runtime-build, runtime_key, the IR pin's check) stays `clang` until the night's runs (G5c.8) hold; then that one word becomes `auto`, and
the gate's `cpp-runtime` (`bin/cppi runtime-build && bin/cppi runtime-ci`, records mode) builds libsupc++'s units by our route with the
names level (runtime-judge). `runtime-swap` is the oracle tier's new step `cpp-runtime-swap-oracle` (test/gate/crops/cpp-oracle.edn and
test/gate/steps.edn: both runtimes with gcc, CPPI_GCC=1, codegen-check on the runtime by our route, then runtime-swap; one job, about 45
minutes; its control waived: each unit's runs against the baseline's). `gate.crops check`: 0 problems; `bin/gate --oracle --plan`
plans it.

**P22 (b), the first measure.** `arbace.cpp.codegen gen-batch` over the 103 units of `c++98/` and `c++11/` (runtime-build's own lists,
`-D_GNU_SOURCE`, the build's include options, no `-f` flags), each generated unit's external names against the pack's IR of the unit
(`arbace.cpp.check/defined-names`): **52 of the 103 generate, 13 of them with the oracle's names exactly** (cxx11-stdexcept.cc 29 names, functexcept.cc 21, globals_io.cc
20, functional.cc 10, regex.cc 9, mutex.cc 3, snprintf_lite.cc 3, parallel_settings.cc 3, and five that define nothing external:
concept-inst.cc, list-aux-2.cc, list_associated-2.cc, math_stubs_float.cc, math_stubs_long_double.cc); four more have every oracle name
and one or two extra: list.cc, list-aux.cc and tree.cc a `__clang_call_terminate` clang does not emit there (a terminate pad our route
writes where clang's cleanup needs none), futex.cc two `std::chrono::duration` C2 constructors clang inlines away; the other 35 that
generate lack the explicit instantiations their unit exists for (limits.cc 506 names, misc-inst.cc 102, cow-istream-string.cc 92: the
template work of G5b). The 51 refused by class: parse 11, name 10, type 7, string 6, template 3, init 3, expr 3, overload 2, crash 2, virtual,
stmt, pp, conversion 1 each. 27 min in one process at GOMEMLIMIT 1400 MiB. Next (P22 (a)): the 13 under `CPPI_RUNTIME_OURS` by name
(runtime_ours takes a prefix; a list of units would let `auto` take exactly these), then the terminate pad and the chrono constructors.

## CP311 (a): G3c's class template argument deduction (2026-11-09, branch `cpp-props3`)

G3.8's "class template argument deduction" and G3c's CTAD are built in `arbace.cpp.template/ctad` (the `:ctad` hook): the implicit
guides from the constructors, `C()`, the copy deduction candidate, the user's deduction guides and C++20's aggregate candidate,
deduced and chosen as clang's DeduceTemplateSpecializationFromInitializer does, for variables, `T(...)` and `T{...}`; where the
slice is unsure the parser's own deduction stays the type. doc/cpp/CPP-SURVEY.md, "As built: CP311 (a) and the rest of CP313 (a)".

## The vtables' order (2026-11-09, branch `cpp-codegen-fix`)

The code generator kept the vtable groups, VTTs and deleting destructors a unit refers to in hash sets (`:groups`, `:vtts`,
`:d0s` of `*top*`) and wrote them in the sets' order, which followed the class and function ids' allocation: cpp-props3, handing
out more ids, moved rt-eh's four vtable globals, and every forms digest with them. They are now kept in the order first referred
to, and each vtable group and VTT takes its place among the module's forms at its first reference (`want!` reserves the slot,
`place!` fills it when the group is written), as clang creates a global where it is first referred to
(`CodeGenVTables::GetAddrOfVTable`, `GetAddrOfVTT`, clang/lib/CodeGen/CGVTables.cpp, CGVTT.cpp at llvmorg-23.1.5) and defines it
in place; a class's VTT follows its vtable (`GenerateClassData`: the vtable's definition, then `emitVirtualInheritanceTables`),
its vtable wanted first. Against clang 23.1.5's `-O0` IR of the 15 samples with vtables (by hand, not recorded), the order of the
`_ZTV`/`_ZTT` definitions is clang's in 4 (ctorvirtual, diamondless, virtual-construct, virtual-deep; none before); the string
constants and `_ZTI` objects agree with clang in the same samples as before (6 and 7 of 17). The rest differs by the order the
deferred functions are written: ours a round at a time, clang's depth first (`CodeGenModule::EmitDeferred` recursing after each
definition). Tried, depth first gives 8 of 15 (rt-eh, rtti-cross, tmpl-virtual, virtual too) but moves dtors' strings off clang's
order: P23 below. `bin/cppi codegen-gen --record` recorded again (149 samples, 149 generated: rtti, recorded refused in cpp-props2's
record, is generated since G5c).

### Proposed

- P23: the deferred functions written depth first as clang's `EmitDeferred`, with `EmitDeferredVTables` at the start of each
  round. (a) port it whole (the vtables' definitions deferring their functions as clang's do, then the order checked against
  clang's IR of the samples: that IR recorded in a pack, a corpus `codegen` for `bin/cppi record-ir`, would make it a light check;
  about a day); (b) leave the order as it is (deterministic, digests stable). Recommended: (a) when the records pack is made,
  as the IR level's comparison with clang needs the same order.

## G5d. The programs by our route (branch `cpp-route`, 2026-11-09)

The user's CP240 (b) (2026-11-07: the run checks' programs by Arbace's own route, `CPPI_ROUTE=auto`, our code generator where it
builds and clang's pack otherwise), CP263 (a) and P21 (b): the refusals of the run corpora's programs grouped by cause and the
biggest groups closed, each rule as the standard states it and as clang's -O0 IR shows it (no clang source ported). The measure,
the switch and the proposals are in doc/cpp/CPP-SURVEY.md, "As built: cpp-route".

| what | asked by | as built |
|---|---|---|
| structured bindings ([dcl.struct.bind]) | cpp1z/decomp1.C; hello.cpp's `for (auto& [k, val] : m)` | `sema/decomposition!`: the unnamed variable e of the declared type deduced from the initializer (an array copied element by element without a ref-qualifier, /1); an array's elements (/2) and a class's data members, all of one class (/5), as names of e's subobjects (`:binding` entries: the subobject's expression, clang's BindingDecl holding expressions, no variable); a tuple-like type (std::tuple_size<E>::value a constant, /4) by variables of type Ti& or Ti&& initialized from get<i>(e) (argument-dependent lookup, e an lvalue when its type is an lvalue reference, else an xvalue; a member get<i>() when E has a member template get), Ti the call's type; in a declaration statement and in a range-based for over an array or a class. The checker keeps cpp-props3's [dcl.struct.bind]/4 rules and refuses the declaration as before (CP331) |
| GNU's `x ?: y` (clang's BinaryConditionalOperator) | ext/cond3.C, opt/pr83252.C | x analyzed once and bound as an opaque value (`:bind-opaque`, `:opaque`: a glvalue by its address, a class prvalue materialized, a scalar by its value; clang's OpaqueValueMapping), the condition and the true operand both it |
| the conditional operator's operands ([expr.cond]/4, /7.1) | ext/cond3.C (`X() ? X() : false`), recursion's `b ? true : f()` | one operand of class type, the other not: the class operand converted by its conversion function to the other's type, else the other by the class's converting constructor; operands of one type keep it (bool ? bool : bool a bool: clang's `phi i1`, before an int) |
| a default member initializer in an aggregate's list ([dcl.init.aggr]/5.1) | cpp1y/pr79937-3.C, cpp0x/initlist-throw2.C | the native parser's semantic list holds CXXDefaultInitExpr in the member's place (`:default-init`): the member's own initializer, its `this` the aggregate being initialized ([class.mem.general]/11; `:agg-this`, the code generator's `*this-val*` for that function, clang's CXXDefaultInitExprScope) |
| a semantic list's value-initialized fill ([dcl.init.aggr]/5.2) | opt/unroll1.C | ImplicitValueInitExpr as `:value-init`, an element value-initialized |
| a list bound to a reference ([dcl.init.list]/3.10) | libstdc++'s `__helper<const _Tp&>({})` (std::pair's C++17 default constructor's condition) | a list that is not a single reference-related element: a prvalue of the referenced type copy-list-initialized, the reference (const or rvalue) bound to it; [over.ics.list]: a non-const lvalue reference binds no list's temporary |
| deduction from a reference-typed expression ([expr.type]/1) | the same condition: `__test(declval<_Tp>())` | adjust-for-deduction takes A without its reference (std::declval<int>()'s call carried int&& and _Tp was deduced int&&) |
| redeclarations of function templates ([temp.over.link]/6) | std::pair's two C++17 default constructors | the template parameter lists equal also by the non-type parameters' types, spelled with the parameters by position (enable_if<A, bool> and enable_if<B, bool> made the explicit one a redeclaration of the implicit one) |
| a template parameter's kind (clang's isStartOfTemplateTypeParameter, [temp.param]/2) | std::pair's C++17 constructors (`typename enable_if<...>::type = true`) | `class`/`typename` then `...`, `,`, `>`, `>>`, `=`, or an identifier and then one of `,`, `>`, `>>`, `=`: a type parameter; otherwise a non-type parameter's type (the parser declined these headers and dropped the constructors) |
| default construction by a constructor template ([class.default.ctor], [over.match.ctor]) | std::pair<int, long> p; at C++17 | no constructor that is no template taking no argument: the constructor templates' specializations for no argument, by overload resolution |
| a converting constructor template in an implicit conversion sequence ([over.match.copy]/1.1) | std::string's `template<typename = _RequireAllocator<_Alloc>> basic_string(const _CharT*, ...)` for a const string& parameter | tried when no constructor that is no template converts; the answer cached per program by class and argument (type, category, a null pointer constant's value) |
| explicit template arguments for argument-dependent lookup ([temp.names]/3, P0846) | `get<i>(e)` of a tuple-like binding | an unqualified name that finds nothing keeps its written template arguments for the templates the lookup of the call's arguments finds |
| classes through an ellipsis ([expr.call]/12) and va_arg of them | runtime/deferred.cpp's `mix(2, ..., P{1, 2}, Big{{0, 0, 9}}, ...)` | a trivially copyable and destructible class passed as a parameter of its type is (the psABI's classes, the six INTEGER and eight SSE registers counted on from the declared parameters; a MEMORY one byval), the call's type the prototype's (clang's X86_64ABIInfo::computeInfo); va_arg of a class (`va-arg-class!`, the psABI's §3.5.7 as clang's EmitVAArg writes it): from the register save area when gp_offset <= 48 - 8n and fp_offset <= 176 - 16m, the eightbytes copied into a temporary when mixed or two SSE (16 bytes apart in the save area), else from the overflow area advanced by the size rounded to eight; a non-trivial class refused (clang's error) |
| the implicit global allocation functions ([basic.stc.dynamic.general]/2) | cpp0x/defaulted19a.C's `::operator new (t)` without <new> | declared in the global scope the first time a name expression names one no declaration has: new(size_t), new[](size_t), delete(void*), delete[](void*) and the sized deletes |
| GNU's statement expressions, `({ s...; e; })` | cpp2a/range-for14.C | `:stmt-expr`: the statements in a scope of their own, the last expression's value (a scalar's, or void) kept in a temporary while the scope's cleanups run; a class's refused |
| a range-based for's init-statement ([stmt.ranged]/1, C++20) | the same | the native node is clang's (init, range, begin, end, cond, inc, loop variable, body): the init-statement analyzed first in the loop's scope (it was taken for the range) |
| universal character names in identifiers ([lex.charset]/2; CP337 (b)) | other/ucnid-1.C (`\u00C1` declared, `\u00c1` called) | `sema/ucn-name` on the scopes' keys: one identifier however its UCNs are spelled; the symbols keep the declaration's spelling |
| GNU's `__decltype` at a declaration's start | std::sort (libstdc++'s heap and algorithm bodies: `__decltype(...) __cmp(...);`) | the parser's declaration-specifier test takes it as decltype (the statement was read as an expression and declined, so every std::sort specialization was refused) |

Two checks of the parser came with them: a non-type template parameter's type naming an enclosing template's parameter pack with no
`...` is diagnosed as clang does (`non-type template parameter type contains unexpanded parameter pack 'T'`, DiagnoseUnexpandedParameterPack,
[temp.variadic]/7; g++.dg's cpp0x/variadic170.C had passed its own dg-error line by the decline the type-parameter test removed), and a
parse refusal names the parser's reasons (`declined 2 constructs: declaration: a parameter's type x2`).

Samples of test/cpp/codegen written for them: `struct-bind`, `gnu-cond`, `va-class`, `agg-dmi`, `stmt-expr` (each `codegen-check`'s three levels at the
oracle tier; the light half's digests recorded).

## G5e. CP330 (a), CP332 (a), CP336 (a) and P23 (a) (branch `cpp-props5`, 2026-11-09)

The user's acceptance of 2026-11-09 (CP325-CP337 and P23, each as recommended). The code generator's half; the checker's half
(CP325, CP327-CP329, CP331) and the measures are in doc/cpp/CPP-SURVEY.md, "As built: cpp-props5".

| what | asked by | as built |
|---|---|---|
| inheriting constructors ([namespace.udecl]/3, [class.inhctor.init]; CP330 (a)) | std::unique_ptr's `__uniq_ptr_data` (`using __uniq_ptr_impl<_Tp, _Dp>::__uniq_ptr_impl;`), runtime/hello.cpp | `ctor-init` takes the named bases' constructors as candidates for the code generator too (but copy, move and parameterless ones, [over.match.funcs]/8-9, and one with the parameter types of the class's own, [namespace.udecl]/15); the chosen one gives the class's inheriting constructor (`inheriting-ctor!`, made once per base constructor, no member of `:ctors`): the base constructor's parameters and default arguments, its definition (`analyze-implicit!`'s `:inh-from`) the base subobject initialized by the base constructor with the parameters forwarded (an lvalue reference's an lvalue, else an xvalue: static_cast<T&&>(p)) and the other subobjects as an implicit default constructor initializes them (`ctor-inits`' `:pre-inits`); mangled `CI1`/`CI2` and the base's name (Itanium §5.1.4.3's <ctor-dtor-name>, `abi/unqualified-name`). A virtual base's constructors are not inherited by this slice (refused as before) |
| `__type_pack_element<N, T...>`, `__make_integer_seq<Seq, T, N>` (clang's BuiltinTemplateDecl; CP332 (a)) | <bits/utility.h>'s `_Nth_type` and `_Build_index_tuple` when `__has_builtin` answers 1 (cpp/target's clang half): every `std::get<I>(tuple)`, `tuple_element` | the native parser declares both names as templates in the global scope (the alias `using type = __type_pack_element<_Np, _Types...>;` had been declined, so `_Nth_type` had no member `type`); `template-id` resolves them: the Nth type of the pack, Seq<T, 0, ..., N-1>, dependent in a signature |
| a pack parameter's own type as a template argument | `tuple<typename __decay_and_strip<_Elements>::__type...>` (make_tuple's return type): an alias template's argument in a pack expansion's pattern | `{:k :tparam :pack true}` is a type, not a pack of arguments (`arg-entity`, the deduction's `match-args` case, the mangling's `template-arg` read `:pack` as an argument pack) |
| a mem-initializer and a functional cast whose one argument is a pack expansion ([temp.variadic]/5, [expr.type.conv]/2) | tuple's `_Inherited(std::forward<_UTypes>(__u)...)`, make_tuple's `__result_type(std::forward<_Elements>(__args)...)` | the expansion's arguments, as many as the pack has |
| a parenthesized expression that is no fold | `requires (__constructible<_Elements...>())`: `...>` was taken for a fold over `>`, the constraint kept as text and every tuple constructor dropped | in full-header mode, a candidate fold that does not parse as one and parses as an expression is a ParenExpr (the skim mode's tree unchanged) |
| if constexpr over a class ([stmt.if]/2: contextually converted to bool) | std::map's `_Rb_tree::_S_key`: `if constexpr (__is_invocable<...>{})` | the condition converted as a condition is (`to-bool`: the conversion function) and evaluated; the constant evaluation calls a constexpr member function of an empty class (nothing of its object can be read) |
| the full parse cache in gen-batch (CP336 (a)) | the run checks' iostream programs, 60 to 450 s each | `arbace.repr.cpp.parse/*full-cache*`, which gen-batch binds by `CPP_GEN_FULL_CACHE=1` (CPP_FULL_CACHE=1 on everywhere, 0 off, as before): G5.2's cache, keyed by the header run's tokens and the parser's sources. Measured: a hit is slower than the parse it saves (samples/io.cpp 557 s against 378 s, the forms byte for byte the same), so it is off by default; CP341 |

**P23 (a): EmitDeferred's order.** The module is written in clang's order (CodeGenModule::EmitGlobal, EmitDeferred and
EmitDeferredVTables, clang/lib/CodeGen/CodeGenModule.cpp at llvmorg-23.1.5, Apache-2.0 WITH LLVM-exception; the order ported, no
code): the definitions that must be emitted written first in order without recursion (the eager ones), then EmitDeferred: the
deferred vtables (groups and VTTs) first, then the deferred functions taken as a list, each written and, when it deferred more,
EmitDeferred again before the next (depth first); a virtual destructor's deleting variant (D0) is a deferred function like the
others, in the order first referred to (it was written in rounds of its own).

## As built: coroutines (queue 393, W4; branch `393-cpp-coro`, 2026-11-19)

Report 345's W4: the coroutine programs of the run checks (samples/coroutine.cpp and the six of gxx-dg-run's sampling:
coroutines/pr106188.C, torture/class-02-templ-parm.C, co-await-20-do-while-cond.C, co-yield-02-loop.C, lambda-04-templ-parm.C,
pr95519-01-initial-suspend.C) by our route, no clang. clang's IR of a coroutine at -O0 is already split (SPEC.md §14.3: LLVM's
CoroSplit ran, no `llvm.coro.*` call left), so the code generator writes that split itself; the lowering's refusal of
`llvm.coro.*` stays (nothing of ours emits one).

| part | where | as built |
|---|---|---|
| the analysis | `arbace.cpp.sema`'s `coroutine-body`, `coro-await` | a function whose body holds co_await, co_yield or co_return outside a lambda's body or a local class ([dcl.fct.def.coroutine]/1): the promise type `std::coroutine_traits<R, [cv C& / C&&], P...>::promise_type` (/4, the pack grouped as the templates bind it); every parameter copied (/13: a class by value move-constructed, a reference bound again; the body's names the copies); the promise initialized from the copies' lvalues (with *this first for a member that is no lambda's call operator: clang's buildPromiseInitialization, the IR of lambda-04 calls `promise_type(int)`) when that initialization is valid, else default-initialized (/5); get_return_object's value the returned object at once when of the return type, else kept and converted when the ramp returns (clang's, class-02's `coro1(handle_type)` after the first suspension); `{ co_await p.initial_suspend(); try { body; [p.return_void();] } catch (...) { p.unhandled_exception(); } final: co_await p.final_suspend(); }`; co_return `p.return_value(e)` (a class local named alone moved) or `p.return_void()`, then the final suspension; an await-expression ([expr.await]/3): await_transform for co_await's operand when the promise declares one, operator co_await when viable, the awaiter materialized, `await_ready()` as a bool, `await_suspend(coroutine_handle<P>::from_address(frame))` (void or bool; a handle's symmetric transfer refused), `await_resume()` its value. Refused: a promise's own operator new or delete, `get_return_object_on_allocation_failure`, a parameter pack, an unnamed parameter, `std::noop_coroutine`. The checker (CX12, `*on-refusal*`) keeps its analysis as before |
| the builtins | `:coro-builtin` | <coroutine>'s `__builtin_coro_done` (a null resume pointer), `_resume` and `_destroy` (the call through the frame's first and second word), `_promise(p, align, from)` (p ± alignTo(16, align)), as LLVM's CoroCleanup lowers them for the switch ABI |
| the ramp | `arbace.cpp.codegen`'s `coro-ramp!` in `emit-fn!` | the frame by `::operator new` (freed if what follows throws), its header (the resume and destroy functions' pointers), the index 0 and the destroy flag, the flags the resume function's entry would clear, `this` and the copies, the promise at alignTo(16, align(P)), the returned object, then a call of NAME.resume; the return |
| NAME.resume(frame) | `coro-resume!` | internal; the body with every alloca a frame slot (laid out in order after the header, the promise first; an alignment above 16 refused), entered by a switch on the suspension index (0 the start); the copies' and the promise's destructors the bottom of the cleanups ([dcl.fct.def.coroutine]/12); falling off the final suspension runs them and frees the frame |
| a suspension point | `co-await!` | the awaiter's address kept in a frame slot (read again after a resumption), `await_ready`, then the index stored (at the final point the resume pointer nulled: `done()`), `await_suspend` (a bool's false resumes at once) and `ret void`; its resumption the switch's case: with the destroy flag set (NAME.destroy(frame) sets it and calls NAME.resume) the cleanups in force there run, the full-expression's temporaries first, and the frame is freed, else `await_resume` |
| the check | `coro-check-ssa!` | a resume function whose value would cross a suspension point without a slot (`g + co_await a`: `g` loaded before) is refused (`coroutine`, "a value live across a suspension point"), by dominance over the blocks the dispatch's switch enters |

Beside it, an `auto` static data member's type is deduced at its first use by the code generator's analysis
(`var-node`; [dcl.spec.auto.general]/11): pr95519's `static constexpr auto initial_suspend = []() noexcept {...};`.

The proofs (the VM, light): the seven programs by our route (`arbace.cpp.codegen gen-batch`) generated, lowered and, as a quick
probe, built natively by gcc with the runtime of exceptions: the status and stdout of each the pack's recorded clang run
(coroutine.cpp 3, the six 0); then `CPPI_ROUTE=ours bin/cppi check --only` on them (all seven's IR by `ours`), on ci with the lowered
runtime: 7 of 7 pass, by their own verdicts and with `CPPI_JUDGE=gxx` by g++'s recorded runs; `check --aot --only` the same: 7 of 7
(the sampling's six compiled-only against the interpreted record, which predates them); the control `_control/run/coroutine-vs-pr106188`
(test/cpp/controls.edn) differs, as it must; `bin/cppi codegen-gen`: 155 of 155 samples' forms unchanged. Probes beside, natively against clang++'s own build: a coroutine destroyed while suspended (the
awaiter, the locals, the promise and the copies destroyed in clang's order), a bool await_suspend that declines and a final
suspension run to completion: the same output; `g + co_await A{}` refused.
