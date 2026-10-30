# C++ forms: C++ programs written in Clojure

Status: proposed (2026-10-30), milestone CX1 of the C++ crop ([CPP-SURVEY.md](CPP-SURVEY.md) §11, "As built: CX1"), for
the user's acceptance, which is CX1's exit criterion. The user's decisions of 2026-10-30 bind it (the survey's "Decided
(2026-10-30): CP1-CP14"): CP1 (a) C++23 as g++ 15 and clang 23 accept it, read and print for the whole language but
named modules' compiled interfaces; CP2 (a) clang's `-ast-dump=json` through a filtering helper, the libTooling dumper
later if the numbers call for it; CP3 (a) the reader is oracle-fed; CP4 (a) eval is clang's LLVM IR lowered to C forms;
CP5 (a) templates are instantiated by the oracle; CP10 (a) **C++ forms extend C forms** ([../c/SPEC.md](../c/SPEC.md));
CP11 (a) g++ the judge; CP13 (a) modules read and printed as single-TU forms. Nothing implements it yet: the shared
lexer and preprocessor are CX2, the helper `tools/cxxdump` has a first version (§13), the converter and the printer
are CX4-CX5. Where this text departs from the survey's sketch of §3, §17.1 lists the change and why; the questions it
raises for the user are the survey's CP20 and on ("As built: CX1").

This spec defines the **C++ forms**: how a C++ translation unit, as C++23 (ISO/IEC 14882:2024; the clause numbers are
its) and the extensions g++ 15 and clang 23 accept define it, is written as Clojure data in ordinary `.ae` sources. It
says what each form means, what the converter writes and what the oracle gives instead, how the preprocessor's work is
kept, and what is read and printed but by design not evaluated. It is the counterpart of the C forms
([../c/SPEC.md](../c/SPEC.md)), the Go forms ([../go/SPEC.md](../go/SPEC.md)) and the Java forms
([../classes/SPEC.md](../classes/SPEC.md)), and follows the C forms' structure and principles; **everything the C forms
say holds here unless this text says otherwise**, and §1 to §11 only add.

Words used throughout:

- **the reader**: repr/cpp's `read` (`arbace.repr.cpp`, `bin/repr-cpp read`): the shared lexer and preprocessor (CX2),
  the oracle's filtered AST (`tools/cxxdump`, CX3) and **the converter** (`arbace.repr.cpp.convert`, CX4). Every decision
  about forms is the converter's.
- **the printer**: `arbace.repr.cpp.print`, C++ forms → C++ text, in mode `:lines` (one canonical spelling) or `:full`
  (the original text, byte for byte).
- **the oracle**: clang 23.1.4 (`Alpine clang version 23.1.4`), its AST after Sema and its LLVM IR; **the judge**: g++
  15.2.0 (survey §2). **A fact** is what the oracle says about a node and the converter attaches (§12).
- **a consumer**: anything that reads C++ forms for their meaning: the printer, the LLVM-forms lowering (CX6) and the
  checkers of C23's C4 (which does not read C++ forms in v1, survey §9).
- **the target**: linux/amd64, the Itanium C++ ABI, LP64, musl 1.2.6 and libstdc++ 15.2.0 (the host's: `-std=c++23`
  accepted by both compilers on one set of headers).
- **C++ context**: the inside of a C++ declaration form, where the forms mean C++ (the C forms' "C context" with C++'s
  rules: overloading, conversions through constructors, references).
- **the layer**: what the preprocessor consumed and the forms keep beside the program (§10), as the C forms'.

Contents: 1 Principles · 2 A first example · 3 Equivalence: the round trip · 4 Units, files, namespaces, names ·
5 Types · 6 Declarations · 7 Classes · 8 Templates, concepts, constraints · 9 Code · 10 The preprocessing layer ·
11 Positions, comments and mode `:full` · 12 The oracle's facts · 13 `tools/cxxdump`: design and first version ·
14 The host data and the layouts · 15 Read and printed, out of eval and AOT · 16 Coverage · 17 Worked examples ·
18 Changes to the survey's sketch, open questions · 19 Sources.

## 1. Principles

The C forms' nine principles hold. What C++ adds or changes:

1. **C++ forms extend C forms** (CP10 (a)). A declaration or a statement that is valid C++ *and means what it means
   in C* is the C form (`c/var`, `c/defn`, `let`, `if` as `when`/`if`/`cond`, `for`, `while`, `(set! x + e)`,
   `bit-and`, `(conv T e)`, `(* T)`, `(array N T)`, `(fn [...] R)`, `(addr x)`, `@p`): one reader of those heads in
   the printer, one lowering target, `extern "C"` natural. Heads in the `cpp/` family carry everything else. No `cpp/`
   head means what a C head means with other rules; where C++ changes a C construct's meaning (a `struct` is a class,
   `char` literals have type `char`), the C form is kept and the change is the unit's language (the file's `cpp/file`).
2. **The forms are the source as written, not as instantiated.** A template is its definition, its dependent names as
   written (clang keeps them as `UnresolvedLookupExpr`, `CXXDependentScopeMemberExpr`, `DependentScopeDeclRefExpr`);
   its instances are facts beside it (§8.5). Implicit members (`isImplicit`: the special members the compiler
   declares, deduction guides, lambda closure types) are not forms; `= default` and `= delete` are (§7.4).
3. **Resolution is the oracle's** (CP5 (a)): overload resolution, ADL, deduction, SFINAE, conversions, lifetimes of
   temporaries and the order of destructors are not in the forms. The source has no destructor call; nothing is
   written for it. A consumer that needs them takes them from the facts (§12) or from the oracle's IR (CP4).
4. **Qualified names are single symbols with `/`.** `std::chrono::seconds` is `std/chrono/seconds` (the Clojure
   reader refuses `::` inside a symbol); the first `/` of the reader's symbol is its namespace part and the rest its
   name part, which the printer joins back; a consumer splits on `/`. A leading `::` is `(cpp/global x)`. A member of
   a template-id is `(cpp/scope (<> W int) get)` (§5.2).
5. **A template-id is `(<> name arg*)`** in type, expression and declarator position alike, as C++ writes `name<args>`
   (§8.1). The head `<>` is a symbol the reader accepts and no C head uses.
6. **Operators, special member names and literals keep C++'s spellings through heads, not through the lexer.** The
   operator functions are `(operator ==)`, `(operator call)`, `(operator subscript)`, `(operator "new[]")`, `(operator
   conv T)`, `(operator lit "_km")` (§6.3). The destructor has its own head (`cpp/dtor`) because `~` is the reader's
   unquote. Alternative tokens (`and`, `or`, `not`, `bitand`, ... and the digraphs) are `:full`'s: the forms have the
   canonical symbolic operator (§9.1).
7. **Values, not spellings, in `:lines`; every byte in `:full`** (the C forms' principle 7): digit separators, raw
   strings, literal suffix case, `u8`/`u`/`U`/`L` prefixes where the value and type do not need them, specifier order
   and `class` versus `typename` are `:full`'s. The source's parentheses are kept (`^:c/paren`).
8. **One target, one library.** The forms of a unit are for linux/amd64 with g++'s and clang's common predefined
   macros and headers (the host data, §14); a `#if` on the target is decided at read.
9. **The converter writes what reading needs, never what checking derives** (the C forms' principle 9), and what
   reading needs is larger here: which `<` brackets a template, whether `T(x)` is a declaration or a call (survey P6)
   and which names are types are decided by the oracle's AST and are *structure* in the forms; every type, every
   conversion, every constant value, every layout is a fact.

## 2. A first example

`a.cpp` is the survey's probe P1 (`g++ -std=c++23` and `clang++ -std=c++23` accept it; built and run by g++ 15.2 `-O0` it
exits with status 7, clang's build too):

```cpp
#include <vector>
template<class T> struct W { T v; T get() const { return v; } };
template<class T> concept Num = requires(T t){ t+t; };
template<Num T> T twice(T x){ return x+x; }
struct S { S(){} ~S(){} };
int f(){ S s; auto l=[&](int a){ return twice(a)+W<int>{1}.get(); }; try { throw 1; } catch(int){ } return l(2); }
int main(){ std::vector<int> v{1,2}; return f()+v.size(); }
```

As C++ forms (`:lines`, each item on its line; the facts are not shown, §12):

```clojure
(in-ns 'cpp.a) (cpp/file "a.cpp" :std :c++23)
(c/include "<vector>" :unit cpp.h.3a9c0e41d2b7f685)

(cpp/template [(typename T)]
  (cpp/struct W []
    (c/var ^T v)
    (c/defn ^:const get ^T [] (return v))))

(cpp/concept Num [(typename T)]
  (cpp/requires [^T t] (cpp/simple (+ t t))))

(cpp/template [(typename T :constraint Num)]
  (c/defn twice ^T [^T x] (return (+ x x))))

(cpp/struct S []
  (cpp/ctor S [] [])
  (cpp/dtor S))

(c/defn f ^int []
  (let [(c/var ^S s)
        ^auto l (cpp/lambda [&] [^int a]
                  (return (+ (twice a) ((.-get (cpp/brace (<> W int) 1))))))]
    (cpp/try (do (throw 1))
             (cpp/catch [int]))
    (return (l 2))))

(c/defn main ^int []
  (let [^{:tag (<> std/vector int)} v (cpp/brace (<> std/vector int) 1 2)]
    (return (+ (f) (.-size v)))))
```

- A translation unit is a namespace (`cpp.a`); `cpp/file` is `c/file`'s counterpart (§4.2). `#include <vector>` is a
  form where it stands, naming the **header unit** its declarations are (§4.3), read once and shared.
- `template<class T> struct W {...}` is a `cpp/template` form holding the declaration it templates; the parameter
  vector is `[(typename T)]` (`class` and `typename` are one: `:full` keeps the word, §8.1).
- `cpp/concept` is a template of its own (its parameters are its first operand); `requires(T t){ t+t; }` is
  `cpp/requires` with the parameter vector and its requirements (§8.3). `template<Num T>` is the type-constraint
  `:constraint Num` of the parameter.
- A member function is `c/defn` inside the class body, with `^:const` (the cv-qualifier after the parameters) as
  metadata on the name; the return type is the parameter vector's tag, as in C.
- `S(){}` is `(cpp/ctor S [] [])`: the class name, the parameters, the member initializers (none), then the body (empty).
  `~S(){}` is `(cpp/dtor S)`.
- `S s;` is `(c/var ^S s)` among the `let`'s bindings; the constructor call and the destructor call at the end of
  the block are the oracle's, not the forms' (principle 3).
- The lambda is `cpp/lambda`: the capture vector `[&]`, the parameters, the body; no return type was written, so the
  vector has no tag. Its closure type is a fact (`:cpp/closure`).
- `W<int>{1}.get()` is a call of a member access whose object is the braced construction of the template-id
  `(<> W int)`; `v{1,2}` is `(cpp/brace T e*)` as an initializer (§9.4).
- `try {...} catch(int){}` is `cpp/try` with its handlers; `(catch [int])` has an unnamed parameter, as C's
  unnamed parameters (`[int]`).
- `v.size()` is a call of `(.-size v)`: C's member access, a call as C's call of a member function pointer; whether it
  is a data member or a method is the declaration's (a fact).

## 3. Equivalence: the round trip

The C forms' §3.1 gives the levels. For C++ (survey §4.3) they are:

1. **Forms.** The printed text read again gives the same forms (C++ → forms → C++ → forms), metadata included but
   `:line`.
2. **Text.** In mode `:full`, the printed text is the original, byte for byte (no compiler needed).
3. **AST.** The `:lines` print through `tools/cxxdump` (clang's AST of the main file) equals the original's AST
   modulo positions and the dump's per-run ids: the declarations' kinds, names and types, and the expressions' kinds,
   types and referenced declarations (by mangled name or path), compared as data. A unit clang rejects is compared on the
   error's line and kind instead (§3.3).
4. **Code.** The `:lines` print compiled by the judge (`g++ -std=STD -O0 -fno-stack-protector -c`) and by the oracle
   (`clang++ ... -c`), the same base name in two directories, gives each compiler's object file of the original
   byte for byte. Survey probe P7 shows the objects of one compiler are deterministic for the same program.
5. **Behaviour.** A program built from the printed units runs as the original (status and stdout).

Levels 1 and 2 localize a difference; levels 3 and 4 are the read level's judges (the survey's §1). Level 3 is new to
C++ and exists because the forms' meaning is the oracle's: a print that changes how clang reads the program is
visible there even when the code is the same.

### 3.2 Normalizations

The C forms' N1-N6 hold (literal spelling, specifier order and words, grouping of declarators, braces, comments and
white space, macro invocations that are not whole nodes). C++ adds:

- **N7 alternative spellings**: `and`/`&&`, `not`/`!`, `bitand`/`&`, `compl`/`~`, `not_eq`/`!=`, the digraphs
  `<: :> <% %> %: %:%:` (`:full` keeps them, §9.1).
- **N8 `class`/`typename` in a template parameter**, `struct` versus `class` is **kept** (it sets the default access),
  `typedef` versus `using` is **kept** (two heads).
- **N9 elaborated type specifiers**: `struct S s;` and `S s;` are one form when both name the same type; the elaborated
  `enum E` and `class C` in a declaration are kept as written by the oracle's `ElaboratedType` (a fact says it was).
- **N10 trailing and leading return types**: `auto f() -> int` and `int f()` are different forms (`:cpp/trailing`),
  kept (clang keeps the written type location).
- **N11 inline definitions**: a member function defined in the class and one defined outside are different forms; an
  implicit `inline` (a class-body definition, `constexpr`, a template) is derived.

### 3.3 What equivalence does not cover

As the C forms' §3.3: a unit the oracle rejects has no forms (the diagnostics are the oracle's, no Arbace
diagnostics for Sema, survey §4.2); error tests are judged on rejection and the first diagnostic's position against
g++'s. Differences the oracle and the judge legitimately have (survey §5.6: argument evaluation order, undefined
behaviour, `long double`) are classes of the run levels, not of the read.

## 4. Units, files, namespaces, names

### 4.1 Namespaces and layout

Each source file is a namespace `cpp.NAME` of an `.ae` file `NAME.ae`, the C forms' §4.1: the base name's image, one
file of forms per translation unit, a header read as a **header unit** `cpp.h.DIGEST` (§4.3). The alias `cpp` is
`arbace.cpp`, as `c` is `arbace.c`.

### 4.2 `cpp/file`

```clojure
(cpp/file "a.cpp" :std :c++23)
(cpp/file "pr12345.C" :std :gnu++20 :args ["-fpermissive" "-DX=1"] :host "3f2a...")
```

The keys are `c/file`'s: the first argument the base name, `:std`, `:args` (the options that change the read: `-D`,
`-U`, `-I`, `-include`, `-fpermissive`, `-fno-exceptions`, `-fno-rtti`, `-fchar8_t`, `-fno-char8_t`, `-fno-operator-names`,
`-fconcepts`, ...), `:name`, `:host` (the digest of the host data the read used, §14), `:positions`, and `:full`'s
layer. `:std` is one of `:c++98 :c++03 :c++11 :c++14 :c++17 :c++20 :c++23 :c++26` or GNU's `:gnu++98` ... `:gnu++26` (the
`-std=` names; the host data has the predefined macros of all sixteen, §14); the default of a file whose corpus names no
standard is its action's (`:gnu++17` for gcc's `g++.dg`, the compilers' default). `:std` changes meaning, not only
acceptance (`auto`, `char8_t`, `constexpr`, `noexcept`, `export`, `concept`, `requires`, `co_await` are keywords or
identifiers by standard; template argument deduction for class templates, `static_assert` without a message,
lambda syntax, `if constexpr`, structured bindings, and the resolution of `>>` and `<:` by standard).

### 4.3 Header units

As the C forms' §4.3 (CS30 (a)): the declarations of a header are read once per context (standard, relevant
macros, `-D` options that reach it) into a unit `cpp.h.DIGEST`, which every unit that includes the header in the
same context shares. C++'s headers are far larger than C's: `<vector>` preprocesses to 21,443 lines (745 KB),
`<iostream>` to 48,213 lines, `<print>` to 1.56 MB (survey P1), and clang's JSON AST of a four-line program that
includes three of them is 608 MB (survey P3). Two consequences:

- **The header unit's forms are not read in v1's read path**: the oracle's AST of an included header's declarations is
  dropped by the helper (§13) and the unit keeps the include **as a name and a digest**. Reading libstdc++'s headers
  as forms is a corpus of its own (survey §7: 862 files of the tarball, 834 installed), done by translation units
  that include the header with `-main` set to it, or by `cpp/file` of the header itself.
- **The token streams** of the headers are cached by digest (survey P1: 16 GB for 10,000 files including
  `<iostream>`).

A header unit has the forms of its declarations *when asked for* (a corpus entry for the header, `:until :CX5`); the
units of a program's own headers (wpp's `editor.hpp`) are read as headers always.

### 4.4 Names

Identifiers are written verbatim (case sensitive, no munging); the few that the reader would read as something else are
`(c/id "...")` (the C forms' §4.4): an identifier with a universal character name or one that is a symbol the reader
cannot read. **Qualified names** are one symbol with `/` between the parts (`std/vector`, `a/b/c`, `Base/f`); the
leading `::` is `(cpp/global x)`; a name through a template-id is `(cpp/scope (<> W int) get)` (principle 4). The
`operator` names (§6.3) and the destructor (§7.2) are forms of their own. The special names `this`, `true`, `false` are
symbols and `nullptr` is `nil`, as C23's `nullptr` (C's §8.5): the type is a fact (`std::nullptr_t`).

A C++ identifier that is also one of the reserved heads of §16.1 or of C's §15.4 (a variable called `init`, `at`,
`label`, `case`, `access`) is reached through `(c/id "...")`, as in C. The `cpp/` heads cannot clash with a C++
name: `cpp/x` is a qualified symbol and a program's own namespace called `cpp` is written `(cpp/global ...)` or never
(the unit's namespace alias `cpp` is `arbace.cpp`, §4.1; a program that uses a namespace `cpp` has its names read as
`c/id`; proposal CP28).

### 4.5 `namespace`, `using`, aliases, `extern`

```
(cpp/namespace name decl*)        ; namespace name { ... }; name may be a/b/c (C++17 nested definition); nil: unnamed
^:inline (cpp/namespace name decl*)   ; inline namespace
(cpp/namespace-alias n a/b)       ; namespace n = a::b;
(cpp/using a/b)                   ; using a::b;      (a using-declaration; several names: several forms)
(cpp/using-namespace a/b)         ; using namespace a::b;
(cpp/using-enum E)                ; using enum E;
(cpp/alias name T)                ; using name = T;
(cpp/linkage "C" decl*)           ; extern "C" { ... }; (cpp/linkage "C" decl) for extern "C" decl
```

- `typedef` is `c/typedef`, as in C; `using name = T` is `cpp/alias`. An alias template is a `cpp/template` around a
  `cpp/alias`.
- `extern "C"` (and `"C++"`) holds declarations that are C forms or C++ forms; its functions have C linkage (derived).
- A `using` of a base class member inside a class is the same form in the class body (§7.5); `using typename` is
  `^:typename`.
- `namespace` names its members as items; a re-opened namespace is another form.
- Attributes on a namespace, `[[deprecated]] namespace` and `namespace [[...]] n`, are metadata (C's §5.6).

## 5. Types

### 5.1 Type forms

C's type forms (§5.1 there) hold; `bool` is a C++ keyword type: the symbol `bool` (C's `_Bool` is not used in a C++
unit). The additions:

| C++ | type form |
|---|---|
| `T &`, `T &&` | `(& T)`, `(&& T)`; a forwarding reference is a `(&& T)` with `T` a template parameter (derived) |
| `auto`, `decltype(auto)` | `auto`, `decltype-auto` (as types: `^auto x`; also `(& auto)`, `(const auto)`) |
| `decltype(e)` | `(decltype e)` |
| `char8_t`, `char16_t`, `char32_t`, `wchar_t` | the same symbols |
| `std::nullptr_t` | `std/nullptr_t` (a typedef name); the type of `nullptr` is `(decltype nullptr)` |
| `X::*` pointer to member | `(member-ptr C T)`: `int C::*` is `(member-ptr C int)`, a pointer to member function `void (C::*)(int)` is `(member-ptr C (fn [int] void))` |
| a class, union, enum name | `X` (a symbol; qualified `a/X`); `struct X`, `class X` elaborated is `^:cpp/elaborated X` |
| template-id `std::vector<int>` | `(<> std/vector int)` |
| a dependent name `typename T::type` | `(typename T/type)`; `typename W<T>::type` is `(typename (cpp/scope (<> W T) type))` |
| `typename T::template X<int>` | `(typename (cpp/scope T (template (<> X int))))` |
| `T...` in a parameter pack expansion | `(pack-expand T)`; a parameter declaration `Ts... ts` is `^{:tag (pack-expand Ts)} ts` |
| `decltype(auto)` in a trailing return | `decltype-auto` |
| `T[N]`, `T[]`, `T *`, `const T` | C's `(array N T)`, `(array T)`, `(* T)`, `(const T)`, `(volatile T)` |
| the type of a constrained placeholder `Num auto x` | `(constrained Num auto)` |
| `noexcept` function types | `^{:noexcept true} (fn [...] R)`, `^{:noexcept expr} (fn ...)` |
| a function type with ref-qualifier and cv (member functions' types) | `^{:const true :cpp/ref :&} (fn ...)` |
| `T(x)` as a type (functional cast, P6) | never a type form (see §9) |

- A type symbol is never an expression and the converter has decided it (the C forms' §5.2), from the oracle: in
  `U u(T());` the AST gives a function declaration `u`, and the forms are a `c/declare` (survey probe P6).
- **Cv-qualifiers** are wrappers outermost first, C's rule; on a member function they are metadata (`^:const`).
- A template-id's arguments are types, expressions or template names (§8.1).

### 5.2 Where types are written

As C's §5.2: the tag of a declared name, the tag of a parameter vector (the function's return type, or none when
deduced), the first argument of heads that take a type (`conv`, `cpp/new`, `cpp/static-cast`, `sizeof-type`, ...),
inside type forms. The **trailing return type** `auto f(int) -> int` is `(c/defn ^{:cpp/trailing true} f ^int [^int x] ...)`:
the tag is `int`, the metadata says it was written after the parameters; `auto f() -> decltype(x)` has the tag
`(decltype x)`. A deduced return type has no tag (`^auto` in the tag is `auto f()`; no tag is `auto f()` in a lambda
or a `constexpr auto`: the tag is `auto` in a function, absent in a lambda, §9.5).

### 5.3 Function types and parameters

C's §5.3 holds, with these additions:

- **Default arguments** are metadata on the parameter's name: `^{:tag int :cpp/default 4} n`.
- **A parameter pack** is `^{:tag (pack-expand Ts)} ts`; **an explicit object parameter** (C++23's deducing this) is
  `^{:tag (& Self) :cpp/this true} self`.
- **No parameters** is `[]` in a C++ unit (`f()` and `f(void)` are the same C++ declaration; the converter writes `[]`
  and `f(void)`'s spelling is `:full`'s `:c/text`). The C-style variadic `...` is `&` last, as C; a parameter pack is not.
- **Attributes** of a parameter are `:c/attrs` metadata on its name (`[[maybe_unused]]`).

## 6. Declarations

### 6.1 Specifiers

C's metadata (`^:static ^:extern ^:thread_local ^:inline ^:constexpr ^:noreturn`) holds; `^:constexpr`, `^:consteval`
and `^:constinit` are C++'s three, `^:inline` on a variable is C++17's, `^:mutable` on a member, `^:explicit` and
`{:explicit expr}` for `explicit(e)`, `^:virtual`, `^:friend` (in §7), `^:register` is gone in C++17 and read in
earlier standards. A declaration's attributes are C's §5.6 (`:c/attrs`, `:c/gnu-attrs`), with the C++ namespaces
(`[[gnu::always_inline]]`, `[[clang::...]]`, `[[msvc::...]]` as symbols with `/`: `gnu/always_inline`).

### 6.2 Objects and functions

`c/var`, `c/declare`, `c/defn`, `c/typedef`: C's, with initializers of C++'s three kinds:

| C++ | form |
|---|---|
| `T x = e;` | `(c/var ^T x e)` |
| `T x(e1, e2);` | `(c/var ^T x (cpp/paren e1 e2))` |
| `T x{e1, e2};`, `T x = {e1, e2};` | `(c/var ^T x (cpp/brace e1 e2))`; `T x = {...}` is `^:cpp/copy (cpp/brace ...)` |
| `T x;` | `(c/var ^T x)` |
| `T x{};` | `(c/var ^T x (cpp/brace))` |
| designated `T x{.a = 1, .b = 2}` | `(cpp/brace :a 1 :b 2)`, C's `(init :a 1 :b 2)` spelling with keyword designators, as C |
| `auto x = e;`, `auto &&x = e;` | `(c/var ^auto x e)`, `(c/var ^{:tag (&& auto)} x e)` |
| `auto [a, b] = e;` | `(cpp/structured ^auto [a b] e)`; `auto &[a, b](e)` is `(cpp/structured ^{:tag (& auto)} [a b] (cpp/paren e))` |
| `constexpr int N = 4;`, `constinit int g = 1;` | `(c/var ^:constexpr ^int N 4)`, `(c/var ^:constinit ^int g 1)` |
| `static const int k = 3;` in a class | `(c/var ^:static ^{:tag (const int)} k 3)` |

A function is `(c/defn name ^R [params] stmt*)` and its prototype `(c/declare name ^R [params])` as C. Specifiers and
what follows the declarator are **metadata on the name**: `^:const` `^:volatile` (cv of a member), `^{:cpp/ref :&}` `^{:cpp/ref :&&}`,
`^:noexcept` and `^{:noexcept expr}`, `^:override`, `^:final`, `^:virtual`, `^:pure` (`= 0`), `^:static`, `^:explicit`,
`^:constexpr`, `^:consteval`, `^:inline`, `^{:cpp/trailing true}`, `^{:cpp/trailing-requires expr}`. A body that is
`= default;` or `= delete;` is `(cpp/defaulted)` or `(cpp/deleted)` or `(cpp/deleted "reason")` (C++26's message), the sole body form.
A **function-try-block** `f() try { } catch (...) { }` is a body `(cpp/function-try (do stmt*) (cpp/catch ...)*)`. A **coroutine** is a
function whose body holds `co-await`, `co-yield` or `co-return` (§9.7); no marker is written.

### 6.3 Operator functions, conversions, literals

The name of an operator function is a list headed by `operator`, in the name position of `c/defn`/`c/declare` or inside a class:

| C++ | name |
|---|---|
| `operator==`, `operator<=>`, `operator+`, `operator!`, `operator->`, `operator->*`, `operator,`, `operator&&` | `(operator ==)`, `(operator <=>)`, `(operator +)`, `(operator not)`, `(operator ->)`, `(operator ->*)`, `(operator comma)`, `(operator and)` |
| unary and binary overloads of `+ - * &` | the same name; the parameter count (with the implicit object) tells |
| `operator()`, `operator[]` | `(operator call)`, `(operator subscript)` (C++23: several parameters allowed) |
| `operator new`, `operator delete`, `operator new[]`, `operator delete[]` | `(operator new)`, `(operator delete)`, `(operator "new[]")`, `(operator "delete[]")` |
| `operator T()` conversion function | `(operator conv T)` (a type form: `(operator conv (& int))`) |
| `operator co_await` | `(operator co-await)` |
| `operator""_km(long double)` literal operator | `(operator lit "_km")` (the suffix, with `operator "" _km`'s spelling in `:full`) |
| a call `a + b` of an overloaded `operator+` | `(+ a b)`: the same as a built-in operator; the oracle's fact says it is a call to a function (`:cpp/call`) |
| an explicit call `a.operator+(b)` | `(cpp/member-call a (operator +) b)` (the member access and the call in one form, since `operator+` is not a symbol) |

The operator forms follow C's names for the operators C writes with words or other symbols: `(operator not)` is `operator!`,
`(operator and)` is `operator&&`, `(operator or)` `operator||`, `(operator bit-and)` `operator&`, `(operator bit-or)`,
`(operator bit-xor)`, `(operator bit-not)` `operator~`, `(operator comma)` `operator,`, `(operator inc)` and `(operator dec)` for
`operator++`/`operator--` (the postfix overload has an `int` parameter, as the standard says); the others are the symbols
`+ - * / % << >> == != < <= > >= <=> -> ->* = += -= ...`. A compound assignment operator is the symbol with `=`:
`(operator +=)`, `(operator <<=)`.
An operator whose spelling is no symbol is a string, as `new[]` is: `(operator "/=")` (`/=` would read as a symbol and a number
in one; the reader's tokens end at `/`), the same for any spelling the reader cannot read back as one symbol.

### 6.4 Enumerations

```
(cpp/enum E [A B (C 3)])                       ; enum E { A, B, C = 3 };
^{:cpp/underlying unsigned-char} (cpp/enum E [...])   ; enum E : unsigned char { ... }
(cpp/enum-class E [A B])                        ; enum class E { A, B };
(cpp/enum-struct E [A B])                       ; enum struct E { A, B }
(cpp/enum E)                                    ; opaque: enum E : int;  (with :cpp/underlying)
```

C's enum forms (`c/enum`) are used for an unscoped enumeration whose constants do not need C++ (the converter writes
`c/enum` when valid C and `cpp/enum` otherwise: the underlying type of an unscoped enum, an enumerator initialised by a
constexpr call). **The simple rule: a C++ unit uses `cpp/enum` always**, so a printer has one head. `c/enum` is C-only.

### 6.5 Static assertions, attribute declarations, `asm`

`c/static-assert` (with the message optional, C++17) and `c/attr-decl`, `c/asm` as C. An empty declaration `;` at namespace
scope is dropped (`:full`'s). `[[...]]` attribute declarations as C (`(c/attr-decl fallthrough)`).

## 7. Classes

### 7.1 Class heads

```
(cpp/struct Name [base*] member*)       ; struct Name : bases { members };  default access public
(cpp/class Name [base*] member*)        ; class Name ...;                   default access private
(cpp/union Name [] member*)
(cpp/struct Name)  (cpp/class Name)     ; a forward declaration (no bases vector)
^:final (cpp/struct Name [...] ...)     ; class Name final : ...
(cpp/struct [] member*)                 ; an unnamed class (a variable's or a typedef's type)
base = Name | ^:public Name | ^:private Name | ^:protected Name | ^:virtual Name | ^:virtual ^:public Name
```

A class in a type position is a type form `(cpp/struct Name [...] ...)`, as C's `(struct s [...])`; a class declared
and a variable of its type in one declaration is a `c/group` (C's §6.7). A class template or a specialization is a
`cpp/template` around the class head (§8).

**Declaration groups (CP75 (b)).** C++'s counterpart of C's `c/group` is `(cpp/group head declarator*)`: a class or enum definition (or an
elaborated forward declaration, `struct A *p;`) and the variables and typedefs declared with it in one declaration. `head` is the class
form, unnamed or named; each declarator is a `c/var` or `c/typedef` whose type names the head as `c/base` (`c/base`, `(* c/base)`,
`(const c/base)`). The printer writes the head in place of `c/base` in the declarators' common specifiers and the declarators after it,
separated by commas; a local group is a binding of a `let`, a member group a member. The unnamed class thereby has one definition, as in the
source, and the AST of the print is the original's:

```clojure
(cpp/group (cpp/struct [] (c/var ^int a)) (c/typedef T1 c/base))                         ; typedef struct { int a; } T1;
(cpp/group (cpp/struct S2 [] (c/var ^int a)) (c/typedef S2 c/base) (c/typedef PS2 (* c/base)))
(cpp/group (cpp/enum E3 [e1]) (c/var ^c/base ev))                                         ; enum E3 { e1 } ev;
; struct A {...} x(65535);
(cpp/group (cpp/struct A [] (c/var ^long-long a)) (c/var ^c/base x (cpp/paren 65535)))
```

The reader makes a group when the oracle's declaration of the class overlaps the extent of a variable, field or typedef (clang lists them
one after the other); where a declarator's type cannot be rewritten the union of the extents is `(cpp/text ...)` as before. CP65 (c)'s
normal form (template parameters' names) is not built: the print names an unnamed template parameter by what the converter has. A member class is a member (nested). A local class is a binding in a `let`.

### 7.2 Members

A member is an item of the class body:

| member | form |
|---|---|
| data member | `(c/var ^T x)`, `(c/var ^T x init)` (a default member initializer; `{}` and `=` forms as §6.2); `^:static`, `^:mutable`, `^:inline`, `^:constexpr`; a bit-field `^{:tag unsigned :c/bits 3} x` |
| member function | `(c/defn name ^R [params] stmt*)` (inline) or `(c/declare name ^R [params])`, with the metadata of §6.2 |
| constructor | `(cpp/ctor Name [params] [meminit*] stmt*)`; `(cpp/ctor Name [params] (cpp/defaulted))`; a delegating one has a single `meminit` naming the class |
| destructor | `(cpp/dtor Name stmt*)`, `^:virtual`, `^:noexcept`, `(cpp/dtor Name (cpp/defaulted))` |
| conversion function | `(c/defn (operator conv T) [] stmt*)` (no tag: the type is the name's) |
| operator | `(c/defn (operator ==) ^bool [^{:tag (& (const S))} o] ...)` |
| access specifier | `(access :public)`, `(access :private)`, `(access :protected)`: an item, like a label in C (`(label :L)`) |
| nested class, enum, typedef, alias | the declaration forms |
| `using Base::f;`, `using Base::Base;` (inheriting constructors) | `(cpp/using Base/f)`, `(cpp/using Base/Base)` |
| friend | `(cpp/friend decl)`, `(cpp/friend T)` (a type), `(cpp/friend (c/defn ...))` (a defined friend) |
| static assertion | `(c/static-assert e msg)` |
| member template | `(cpp/template [params] member)` |

`meminit` is `(member e*)` for `member(e, ...)`, `^:brace (member e*)` for `member{...}`, `(Base e*)` for a base, `(Base ... )`
with `^:pack` for `Bases(args)...`; the delegating form is `(Name e*)`. Out of class a member definition has a
qualified name: `(c/defn S/f ^int [] ...)`, `(cpp/ctor S/S [] [] ...)`, `(cpp/dtor S/S)`.

### 7.3 Implicit members and special member functions

None is a form (principle 2). Which ones the compiler declared, whether they are trivial or deleted, the class's
data (`polymorphic`, `aggregate`, `standard_layout`, `trivially_copyable`, ...: clang's `definitionData`) are
**facts on the class** (`:cpp/def-data`). A user-declared special member is a form. A `= default` out of class is
`(cpp/ctor S/S [] (cpp/defaulted))`.

### 7.4 Virtual functions, abstract classes, RTTI

`^:virtual`, `^:override`, `^:final`, `^:pure` on member functions; the class's vtable layout, `typeid`, `dynamic_cast`
are the IR's (the Itanium ABI); `typeid(e)`/`typeid(T)` are `(typeid e)`/`(typeid-type T)`; `dynamic_cast<T>(e)` is
`(cpp/dynamic-cast T e)`.

### 7.5 Access, friends, nested names

The default access of `cpp/struct`/`cpp/union` is public, of `cpp/class` private; an `(access ...)` item switches it for
the items after it. `private`/`public` before the first member that equals the default is *kept* (an item), so the
print repeats it (`:lines` writes every access item that was written).

## 8. Templates, concepts, constraints

### 8.1 Template parameters

```
(cpp/template [param*] decl)
param = (typename T opt*)                  ; template<typename T>  or  template<class T>
      | (value ^T N opt*)                  ; template<T N>          a non-type parameter (a type tag), also  auto N
      | (template [param*] T opt*)         ; template<template<class> class T>
opt   = :default type-or-expr | :pack true | :constraint C | :constraint (C arg*)
```

```clojure
(cpp/template [(typename T) (typename Alloc :default (<> std/allocator T))] (cpp/class Vec [] ...))
(cpp/template [(value ^size_t N)] (cpp/struct A [] (c/var ^{:tag (array N int)} a)))
(cpp/template [(typename ^:pack Ts)] (c/defn count ^size_t [] (return (sizeof... Ts))))
(cpp/template [(typename T :constraint std/integral)] (c/defn half ^T [^T x] (return (/ x 2))))
```

`template<class T>` versus `typename` is `:full`'s (N8). `template<>` (an explicit specialization) is
`(cpp/template [] decl)`; a partial specialization is a template whose declaration's name is a template-id
(`(cpp/struct (<> V T *) [] ...)`). An explicit instantiation `template class W<int>;` is `(cpp/instantiate (cpp/class (<> W int)))`
and `extern template ...` is `^:extern (cpp/instantiate ...)`. A deduction guide `template<class T> A(T) -> A<T>;` is
`(cpp/template [(typename T)] (cpp/deduction-guide A [T] (<> A T)))`.

**Template arguments** in `(<> name arg*)`: a type is a type form; a template name is a symbol; a non-type argument is
a literal (`3`, `true`, a string, a character) or `(expr e)` for anything else, because a symbol in the position could be a
type or a value and the oracle's `TemplateArgument` kind (`type`, `integral`, `expression`, `template`, `pack`) is the
converter's decision. A pack expansion is `(pack-expand arg)`.

### 8.2 Function, variable, alias, member templates

A `cpp/template` holds any declaration: `c/defn`, `c/declare`, `c/var` (a variable template), `cpp/alias` (alias
template), `cpp/struct`/`cpp/class`/`cpp/union`, a member (member template), a `cpp/concept` is itself a template.
**Explicit template arguments** in a call: `((<> twice int) 2)`; a template-id as a callee is the head of the call.
`typename`/`template` disambiguators are in `typename` types (§5.1) and `(template (<> X int))` inside a scope.

### 8.3 Concepts and requires

```
(cpp/concept Name [param*] e)                        ; template<class T> concept Name = e;
(cpp/requires [^T t ...] req*)                        ; requires(T t) { ... }   an expression
req = (cpp/simple e)                                   ; e;
    | (cpp/type-req T)                                 ; typename T::type;
    | (cpp/compound e opts*)                           ; { e } noexcept -> C;     opts: :noexcept true  :returns C
    | (cpp/nested e)                                   ; requires e;
```

A requires-clause on a template is `^{:cpp/requires e} (cpp/template ...)` (metadata of the template form), on a
function `^{:cpp/trailing-requires e}` (§6.2); a `requires requires(...) {...}` is the clause `(cpp/requires ...)`.
Whether a constraint is satisfied is compile-time only (the oracle); an unsatisfied constraint is a rejection.

### 8.4 Dependent expressions

An expression that depends on a template parameter is written as any expression (a call `(f x)`, `(.-m x)`, `(-> p m)`,
`(cpp/scope T type)`); the oracle gives it as `UnresolvedLookupExpr`, `CXXDependentScopeMemberExpr`,
`DependentScopeDeclRefExpr`, `CXXUnresolvedConstructExpr`; the converter writes the forms the same as non-dependent ones:
the dependent/non-dependent difference is a fact (`:cpp/dependent`).

### 8.5 Instances as facts

Under every `ClassTemplateDecl`, `FunctionTemplateDecl`, `VarTemplateDecl` the AST lists its implicit instantiations
(survey P2). They are not forms; each is a map in the template form's metadata `:cpp/instances`:

```clojure
^{:cpp/instances [{:args [int] :kind :function :mangled "_Z5twiceITk3NumiET_S0_" :used true}]}
(cpp/template [(typename T :constraint Num)] (c/defn twice ^T [^T x] (return (+ x x))))
```

`:args` are the template arguments as type forms; `:mangled` is the Itanium name; the instantiation's bodies (the
`TemplateInstantiationPattern` below) are not kept. An explicit specialization written by the user is a form (a
`(cpp/template [] ...)`), not an instance.

## 9. Code

### 9.1 Operators and their spellings

C's operators (§7.5 there) with the same forms; the C++ ones: `<=>` as `(<=> a b)`; `a->*p` as `(->* a p)`, `a.*p` as `(.* a p)`;
`co_await` etc. in §9.7. The tokens `and`, `or`, `not`, `xor`, `bitand`, `bitor`, `compl`, `and_eq`, `or_eq`, `xor_eq`, `not_eq` and
the digraphs are N7: the forms have the symbolic operator, `:full` keeps the spelling (`:c/ops` on the form). The AST has
no trace of the alternative spelling: the tokens are the only source, so the converter reads them from the shared
lexer (survey §3.3). A call of an overloaded operator is the operator's form (the fact says it calls a function).

### 9.2 Names and scopes in expressions

A name is a symbol (`x`, `std/cout`); `this` is the symbol `this`; `*this` is `@this`; a qualified call
`Base::f(x)` is `(Base/f x)`; a member of a template-id `W<int>::size` is `(cpp/scope (<> W int) size)`;
`::x` is `(cpp/global x)`; a pointer to member `&C::m` is `(addr C/m)` (the oracle tells a member pointer from an address).

### 9.3 Calls, members, subscripts

As C (§7.6 there): `(f a b)`, `((.-m s) x)`, `((-> p m) x)`, `(aget a i)` (a built-in subscript; an overloaded one is
the same form, the fact says the call), `(@fp x)`. Template arguments on the callee: `((<> f int) x)`; on a member
`x.f<int>(1)`: `((<> (.-f x) int) 1)`. Pseudo-destructor calls (`p->~T()`) are
`(cpp/dtor-call p T)`. A call with a default argument leaves the default out (it is a fact on the call).

### 9.4 Construction, initialization, new and delete

| C++ | form |
|---|---|
| `T(e1, e2)` (functional cast / construction) | `(cpp/construct T e1 e2)`; a single-argument one is a conversion (`T(e)` is `(cpp/construct T e)`, the oracle tells `CXXFunctionalCastExpr` from `CXXTemporaryObjectExpr` and the fact says which) |
| `T{e1, e2}`, `T{}` | `(cpp/brace T e1 e2)`, `(cpp/brace T)`; `{e1, e2}` alone (a braced list in a return, an argument) `(cpp/brace e1 e2)` with the type a fact |
| `new T`, `new T(a)`, `new T{a}` | `(cpp/new T)`, `(cpp/new T (cpp/paren a))`, `(cpp/new T (cpp/brace a))` |
| `new (p) T(a)` placement | `(cpp/new T (cpp/paren a) :place [p])` |
| `new T[n]` | `(cpp/new-array T n)`, with an initializer `(cpp/new-array T n (cpp/brace ...))` |
| `delete p`, `delete[] p`, `::delete p` | `(cpp/delete p)`, `(cpp/delete-array p)`, `^:cpp/global (cpp/delete p)` |
| `static_cast<T>(e)` `const_cast` `reinterpret_cast` `dynamic_cast` | `(cpp/static-cast T e)`, `(cpp/const-cast T e)`, `(cpp/reinterpret-cast T e)`, `(cpp/dynamic-cast T e)` |
| `(T)e`, `T(e)` with a C-style meaning | `(conv T e)` (the C form; the AST's `CStyleCastExpr`) |
| `std::bit_cast<T>(e)`, `std::move(e)`, `std::forward<T>(e)` | calls: `((<> std/bit_cast T) e)`, `(std/move e)`, `((<> std/forward T) e)` (library, not language) |
| `sizeof...(Ts)` | `(sizeof... Ts)` |
| `alignof(T)`, `sizeof(T)`, `sizeof e` | C's |
| `noexcept(e)` | `(noexcept e)` |
| `typeid(e)`, `typeid(T)` | `(typeid e)`, `(typeid-type T)` |
| `throw e;`, `throw;` | `(throw e)`, `(throw)` |
| `nullptr`, `true`, `false`, `this` | `nil`, `true`, `false`, `this` |
| a fold expression `(... + args)`, `(args + ...)`, `(init + ... + args)` | `(cpp/fold :left + args)` (unary left), `(cpp/fold :right + args)`, `(cpp/fold :binary + init args)` |
| a pack expansion `f(args...)` | `(f (pack-expand args))` |
| a user-defined literal `5_km`, `"x"s` | `(cpp/udl 5 "_km")`, `(cpp/udl "x" "s")`: the value then the suffix (the call to the literal operator is a fact) |

### 9.5 Lambdas

```
(cpp/lambda [capture*] [param*] stmt*)
(cpp/lambda [capture*] ^R [param*] stmt*)             ; with a trailing return type
(cpp/lambda [capture*] ^{:cpp/specs [mutable constexpr noexcept]} [param*] stmt*)
(cpp/lambda [capture*] ^{:cpp/template [(typename T)]} [param*] stmt*)     ; [] <class T> (T x) { }
capture = & | = | x | (& x) | (= y e) | (& y e) | this | @this | ^:pack x
```

A lambda with `static` or `consteval` or `noexcept(e)`, `requires`: specs in `:cpp/specs` and `:cpp/requires`. The lambda's
parameter vector has no tag if no return type was written. The closure type and its call operator are facts and, in the IR,
a class with `operator()` (the survey's §5: the IR is the oracle's).

### 9.6 Statements

C's statements (§7.3 there) hold. Additions:

| C++ | form |
|---|---|
| `try { ... } catch (T e) { ... } catch (...) { ... }` | `(cpp/try (do stmt*) (cpp/catch [^T e] stmt*) (cpp/catch [&] stmt*))` (the `...` catch-all is the vector `[&]`: C's variadic marker) |
| `for (auto x : xs) s` | `(cpp/for-each [^auto x xs] stmt*)`; with an init-statement `(cpp/for-each [(c/var ^int i 0) ^auto x xs] ...)` |
| `if (init; c) s`, `switch (init; e) s` | the C forms with `^{:cpp/init [binding*]}` on the form |
| `if constexpr (c) a else b` | `^:constexpr (if c a b)`; `if consteval { } else { }` `(cpp/if-consteval a b)` / `^:not (cpp/if-consteval a)` |
| a declaration in a condition `if (auto p = f())` | the condition is `(cpp/decl ^auto p (f))`, as `while`'s |
| `co_return e;` | `(co-return e)`, `(co-return)` |
| a statement-expression is GNU's (C forms) | same |
| a local `static_assert`, `using`, class, alias | bindings of the block's `let` (the declaration forms) |
| `[[likely]] s` | C's `^{:c/attrs [likely]}` |
| a label, `goto` | C's |

The `catch` parameter is `[^T e]` or `[T]` (unnamed). `try` and function-try-block share `cpp/catch`.

### 9.7 Coroutines

`co_await e` is `(co-await e)`, `co_yield e` is `(co-yield e)`, `co_return` is `(co-return e)`. A function with any of them is
a coroutine: its `promise_type`, the frame, `initial_suspend`/`final_suspend` and the lowering are the oracle's (clang's
CoroutineBodyStmt; the IR is the split ramp, resume, destroy, cleanup functions, §14.3). No marker is written in the
forms; the printer needs none (the C++ text has none).

### 9.8 Constant evaluation

`constexpr`, `consteval`, `constinit`, `if constexpr`, `static_assert`: specifiers and statements as written; **no
constexpr interpreter in v1**: the value of a constant expression is a fact (`:cpp/value` on the node, from clang's
`ConstantExpr`). A `consteval` function has no run-time code (the IR lacks it).

## 10. The preprocessing layer

The C forms' §9 (directives as forms, macro invocations, `:full`'s text) with these changes:

- **Shared with C.** `c/include`, `c/define`, `c/undef`, `c/directive`, `c/pragma`, the macro-invocation recovery: the
  same forms and the same encoding; repr/cpp's preprocessor is repr/c's, extended (CX2).
- **Additions.** `__cplusplus`, `__has_cpp_attribute`, `__has_include`, `#include_next`, `__VA_OPT__`, raw string literals
  `R"d(...)d"` (a raw string never has directives inside), `import` and `module` and `export` as pp-tokens at the start of a
  line (C++20's module directives: `(cpp/import <h>)` forms, §15), `#pragma once` and `#pragma GCC ...` (C's), `_Pragma`.
- **Search list.** The C++ list has the compiler's own include directory **last** (g++ and clang agree on the first five
  entries: libstdc++'s three directories, `/usr/include/fortify`, `/usr/include`; then `/usr/lib/gcc/.../15.2.0/include` for g++
  and `/usr/lib/llvm23/lib/clang/23/include` for clang); in C, CS32 (a) puts the compiler's directory first. The host data
  records both lists (`test/cpp/host/search-gxx.txt`, `search-clang.txt`). Which compiler's list the preprocessor uses is
  the oracle's: clang's, since the AST is clang's (proposal CP23).
- **Predefined macros.** `test/cpp/host/predefined/{gxx,clang}/STD.h`: g++'s 495 (c++23) and clang's 502; the two sets differ
  (compiler-specific `__GNUC__`/`__clang__`, `__VERSION__`, `__cpp_*` feature macros of different dates, `__STDC_UTF_16__`
  and so on). The preprocessor uses **clang's** (the oracle's AST is made with them); the judge's set is data for the
  `#if`s that differ (a unit that `#if`s on `__GNUC__` differs between the oracle and the judge and is a class,
  `gxx-clang`). The date macros are the fixed epoch's (2026-10-28 00:00:00 UTC, C's CS43 (a)).
- **`__has_*` answers** (`test/cpp/host/has.tsv`): `__has_cpp_attribute(n)`, `__has_attribute(n)`, `__has_include(<h>)`
  for both compilers (e.g. `likely` 201803L in clang; `carries_dependency` 0 in g++ and 200809L in clang).

## 11. Positions, comments and mode `:full`

The C forms' §10 holds: `:lines` (positions off; comments as `;` comments, lost), `:full` (positions, spellings, the
layer, **item-granular text** as built by C3b: each top-level item's text from its first token to the next item's,
CS70 (a)). Additions: the comment forms of C++ (`//` and `/* */` and doc comments `///`, `/** */` kept as data in `:full`);
raw strings' text; `export`/`module`/`import` lines as items; the items of a class body are items of their own (a member
function's text is its item's); alternative tokens (N7) as `:c/ops`. Sub-item layers (per token) are not v1. A macro invocation that stands for declarations, statements or an expression is the form of what it expands to,
with the invocation as written in the metadata, `^{:cpp/macro "ASSERT (x);" :cpp/line 12}` (`:cpp/line`: the line it stood at,
for `__LINE__`): the printer writes the text; the nodes of one invocation after the first carry `:cpp/macro ""`. A value that cannot carry metadata (a number, `nullptr`
as `nil`, a boolean: `INT_MAX`, `NULL`, `SIGSEGV`) is wrapped, `(cpp/macro "INT_MAX" 2147483647)`: the printer writes the text (an operand of
precedence 0), the forms keep the value (CP73 (c)); the wrapper is only for expressions. Where the expansion
cannot be converted the invocation is `(cpp/text "...")`. The
oracle's `range`s (begin and end offsets with macro-expansion pairs) place each node among the lexer's tokens.

## 12. The oracle's facts

### 12.1 What a fact is

The converter attaches to the forms what the oracle gave and the forms do not carry (survey §3.2). They are data **beside**
the forms, not in `:lines`: a unit read with facts has a file `NAME.facts.ae` of one map
`{path fact-map ...}` where a path is a vector of child indices from the unit's items (`[3 1 0]`: item 3's second child's first
child) and a fact map holds:

| key | value | from |
|---|---|---|
| `:type` | the expression's type as a type form | `type.qualType`, `desugaredQualType` (parsed by the converter) |
| `:value-category` | `:lvalue :xvalue :prvalue` | `valueCategory` |
| `:ref` | the referenced declaration (an id: unit-local path or a mangled name) | `referencedDecl` |
| `:call` | the function a call or an operator expression calls, with its mangled name | `CXXOperatorCallExpr`, `CXXMemberCallExpr`, `CallExpr` + `referencedDecl` |
| `:conv` | the implicit conversion chain (castKind: `LValueToRValue`, `IntegralCast`, `UserDefinedConversion`, `FunctionToPointerDecay`, `DerivedToBase`, ...) | `ImplicitCastExpr` |
| `:temp` | a temporary is materialized/bound; `:cleanups` an `ExprWithCleanups` | `MaterializeTemporaryExpr`, `CXXBindTemporaryExpr`, `ExprWithCleanups` |
| `:ctor` | the constructor a construction calls | `CXXConstructExpr.ctorType`, `hadMultipleCandidates`, `constructionKind` |
| `:value` | the constant value of a constant expression | `ConstantExpr.value` |
| `:instances` | a template's instances (§8.5) | the `ClassTemplateSpecializationDecl` etc. under the template |
| `:def-data` | a class's properties: `polymorphic`, `aggregate`, `standard_layout`, `trivial`, ..., and the implicit special members | `definitionData` and implicit children |
| `:layout` | a record's size, alignment, field offsets, bases, vptr | §14.2 |
| `:mangled` | the Itanium mangled name | `mangledName` |
| `:closure` | a lambda's closure class (its name and call operator's mangled name) | `LambdaExpr` |
| `:dependent` | the expression depends on a template parameter | `isDependent`, `isInstantiationDependent` |

`:ref` ids are the oracle's hex ids in the dump (`0x7f...`, per run) renamed by the converter to the stable
**mangled name** where the declaration has one, else the qualified source path with the position
(`"a.cpp:3:12"`). The facts are optional input to later consumers; the printer does not read them.

### 12.2 The node kinds and their forms (the converter's table)

The converter is a table from clang's node kinds to forms; the kinds below were observed in the dumps of P1 and of
whim++'s style of code; the table is closed in CX4 against the whole g++.dg compile corpus.

| clang kind | form |
|---|---|
| `NamespaceDecl`, `NamespaceAliasDecl`, `UsingDirectiveDecl`, `UsingDecl`, `UsingEnumDecl`, `TypeAliasDecl`, `LinkageSpecDecl` | `cpp/namespace`, `cpp/namespace-alias`, `cpp/using-namespace`, `cpp/using`, `cpp/using-enum`, `cpp/alias`, `cpp/linkage` |
| `CXXRecordDecl` (non-implicit), `EnumDecl`, `FieldDecl`, `VarDecl`, `ParmVarDecl`, `FunctionDecl`, `CXXMethodDecl` | `cpp/struct\|class\|union` (`tagUsed`), `cpp/enum*`, `c/var`, `c/var`, a parameter, `c/defn`/`c/declare` |
| `CXXConstructorDecl`, `CXXDestructorDecl`, `CXXConversionDecl`, `CXXDeductionGuideDecl` | `cpp/ctor`, `cpp/dtor`, `(c/defn (operator conv T) ...)`, `cpp/deduction-guide` |
| `ClassTemplateDecl`, `FunctionTemplateDecl`, `VarTemplateDecl`, `TypeAliasTemplateDecl`, `ClassTemplatePartialSpecializationDecl`, `ClassTemplateSpecializationDecl` (explicit), `ConceptDecl`, `TemplateTypeParmDecl`, `NonTypeTemplateParmDecl`, `TemplateTemplateParmDecl` | `cpp/template`, `cpp/template`, `cpp/template`, `cpp/template`, `cpp/template` with a template-id name, `cpp/template []`, `cpp/concept`, the parameter forms |
| `FriendDecl`, `StaticAssertDecl`, `AccessSpecDecl`, `DecompositionDecl` + `BindingDecl`, `ExportDecl`, `ImportDecl` | `cpp/friend`, `c/static-assert`, `access`, `cpp/structured`, module forms (§15) |
| `LambdaExpr`, `CXXNewExpr`, `CXXDeleteExpr`, `CXXThrowExpr`, `CXXTryStmt`, `CXXCatchStmt`, `CXXForRangeStmt` | `cpp/lambda`, `cpp/new`/`cpp/new-array`, `cpp/delete`, `throw`, `cpp/try`, `cpp/catch`, `cpp/for-each` |
| `CXXStaticCastExpr`, `CXXDynamicCastExpr`, `CXXReinterpretCastExpr`, `CXXConstCastExpr`, `CStyleCastExpr`, `CXXFunctionalCastExpr`, `CXXTemporaryObjectExpr`, `CXXUnresolvedConstructExpr` | the casts of §9.4, `conv`, `cpp/construct`, `cpp/construct` |
| `CXXConstructExpr` | the initializer's form (`cpp/paren`, `cpp/brace`) when written; nothing when implicit (default initialization, copy); a fact |
| `CXXOperatorCallExpr`, `CXXMemberCallExpr`, `CallExpr`, `MemberExpr`, `CXXDependentScopeMemberExpr`, `DeclRefExpr`, `DependentScopeDeclRefExpr`, `UnresolvedLookupExpr`, `UnresolvedMemberExpr` | the operator form or a call, a call of a member access, `.-`/`->`, a name, the name, the name, the name |
| `CXXThisExpr`, `CXXBoolLiteralExpr`, `CXXNullPtrLiteralExpr`, `IntegerLiteral`, `FloatingLiteral`, `StringLiteral`, `CharacterLiteral`, `UserDefinedLiteral` | `this`, `true`/`false`, `nullptr`, literals (C's §8), `cpp/udl` |
| `InitListExpr`, `CXXStdInitializerListExpr`, `ParenListExpr`, `DesignatedInitExpr` | `cpp/brace`, (a fact), `cpp/paren`, designators |
| `PackExpansionExpr`, `SizeOfPackExpr`, `CXXFoldExpr`, `CXXNoexceptExpr`, `CXXTypeidExpr`, `RequiresExpr`, `ConceptSpecializationExpr`, `CoawaitExpr`, `CoyieldExpr`, `CoreturnStmt` | `pack-expand`, `sizeof...`, `cpp/fold`, `noexcept`, `typeid`, `cpp/requires`, a constraint name, `co-await`, `co-yield`, `co-return` |
| `CXXRewrittenBinaryOperator` | the operator as written (`a == b` whose reversed or `<=>` rewrite clang found) |
| **dropped, facts only** | `ImplicitCastExpr`, `ImplicitValueInitExpr`, `CXXBindTemporaryExpr`, `MaterializeTemporaryExpr`, `ExprWithCleanups`, `CXXDefaultArgExpr`, `CXXDefaultInitExpr`, `ConstantExpr`, `SubstNonTypeTemplateParmExpr`, `SubstNonTypeTemplateParmPackExpr`, `FullExpr`, `ParenExpr` (kept as `^:c/paren`), implicit declarations, the instantiations under templates (→ `:cpp/instances`), `CoroutineBodyStmt`'s synthesized parts |

## 13. `tools/cxxdump`: design and first version

### 13.1 What it does

`tools/cxxdump` (CP2 (a)) is the oracle's filter: it runs `clang++ -Xclang -ast-dump=json -fsyntax-only ARGS FILE` and
**streams** the JSON, keeping the declarations whose location is in the main file. It is a Go program (TamaGo's go,
`CGO_ENABLED=0`, no cgo, no apk): `bin/repr-cpp cxxdump-build`, `cxxdump`, `cxxdump-test`.

### 13.2 The dump's facts the design rests on

- clang prints a location as a **delta**: `file` only when it changes from the last printed location, `line` likewise, `col` and
  `offset` always; `includedFrom` is printed on every location of an included file. A filter must therefore follow every
  location in document order, in the nodes it drops too.
- A macro location is `{spellingLoc, expansionLoc}`; the main file's nodes have no `includedFrom`.
- `-ast-dump-filter` filters by declaration *name* only; the stream is the only way to filter by file.
- The huge nodes are containers: the translation unit, `namespace std` (one or several nodes), `extern "C"`. Everything
  else is small.

### 13.3 Algorithm (built)

1. Stream the translation unit's `inner` array element by element with `encoding/json`'s token API (nothing else is held).
2. For each element, read its keys in document order into an **ordered** tree (a list of keys and values), tracking
   `file` and `line` at every location (`loc`, `range.begin`, `range.end`, nested macro locations), making them
   absolute.
3. A **container** (`TranslationUnitDecl`, `NamespaceDecl`, `LinkageSpecDecl`, `ExportDecl`) is not read whole: its `inner` is
   streamed recursively; the container is kept when it is in the main file or has a kept child, with only the kept children
   inside.
4. Any other node is kept when its `loc` (the expansion's, for a macro) is in the main file, and whole (with its inner
   nodes and the instances under a template, which are in the main file's template). `-implicit=false` drops
   `isImplicit` nodes.
5. Output, one form a line: a header `{:cxxdump 1 :main "a.cpp"}`, then one form per kept top-level declaration, **Clojure-reader
   data** (maps with keyword keys in clang's order, vectors, strings escaped for the Clojure reader; Arbace has no JSON reader,
   so the helper writes what the converter reads, as `tools/godump` and `tools/hsdump` do), locations absolute (`:file`, `:line`,
   `:col`, `:offset`, `:tokLen`). `-format json` writes JSON Lines instead (the test's and a debugging aid).
   The output of P1's file reads with Arbace's reader on stage 0 (6 declarations and the header); the converter works on that
   data (CX4) and the dump's `:inner` trees are the converter's input as C3b's cc trees are repr/c's.

### 13.4 First version: numbers (CX1)

`tools/cxxdump/main.go` (about 500 lines, two unit tests, `go vet` and `gofmt` clean): on `io.cpp` (the survey's P3: `<iostream>`, `<vector>`,
`<algorithm>`, a sort and a loop; clang's JSON is 607,664,490 bytes) it keeps one declaration of 28,968 bytes, drops 7,618,
in **9.9 s wall** (clang's 8.5 s; the filter keeps pace with the pipe) and **199 MB** peak (RSS) with the heap never
holding a header node beyond the current one. On P1's `a.cpp` it keeps 6 top-level declarations (46,540 bytes) of 2,736.
The filter's result equals a full-parse reference (Python) on the small dump of a header-including program. The numbers
are the survey's P3 answered: the 608 MB is no obstacle to a Go helper.

### 13.5 What a libTooling dumper would add (CP2 (b))

Real tokens (and so alternative spellings, comments and macro-invocation text), `ASTRecordLayout`s, the preprocessor's
callbacks, no JSON. Its packages on this host (`apk search -e`, nothing installed; `apk fetch` file lists read): `clang23-dev`
(the headers `usr/lib/llvm23/include/clang/...`, 1,084 of them, 32 MiB installed; `libclang-cpp.so` link), `llvm23-dev`
(`include/llvm/...`, `llvm-config`, the cmake config; 37 MiB), and their dependencies not on the host: `clang23-libclang` (56 MiB),
`clang23-extra-tools` (47 MiB; it brings `clang-tidy`, `clang-query`), `llvm23` (111 MiB) and `llvm23-test-utils` (1 MiB). Already
installed: `clang23`, `clang23-libs` (95 MiB, `libclang-cpp.so.23.1`), `clang23-headers`, `llvm23-libs` (180 MiB, `libLLVM`). A dumper
that links the monolithic `libclang-cpp.so` needs **no static libraries** (`clang23-static`, 828 MiB, and `llvm23-static`, 445 MiB, are for
linking `libclangTooling.a` and friends; not needed). The simulation `apk add -s clang23-dev llvm23-dev` installs 7 packages, **about 250 MiB** in all.
It would be C++ (a helper in the repository's one allowed exception for helpers, like `tools/hsdump` on GHC and `tools/rsdump` on
rustc), the first C++ the repository builds with a host compiler. Not done; the numbers above (CX1 §13.4) say the JSON does not force it.

## 14. The host data and the layouts

### 14.1 The host data

`test/cpp/host/` (recorded by `bin/repr-cpp host record`, checked by `host check`; the C23 `test/c/host/`'s twin):

| file | content |
|---|---|
| `host.edn` | the compilers' versions, the include directories, the fixed epoch, the list of standards, `:has-std` |
| `predefined/{gxx,clang}/STD.h` | `CXX -std=STD -dM -E` on an empty file, sorted, then the date macros; 16 standards each (`c++98 c++03 c++11 c++14 c++17 c++20 c++23 c++26`, and the `gnu++` ones); g++ 495 and clang 502 macros at `c++23` |
| `search-{gxx,clang}.txt` | the `#include <...>` list, in order |
| `has.tsv` | `__has_cpp_attribute`, `__has_attribute`, `__has_include` for named questions: operator, name, g++'s answer, clang's |
| `sizes.tsv` | `sizeof` and `alignof` of 38 types, **both compilers' runs equal**: the fundamental types (`bool` 1, `wchar_t` 4, `long double` 16/16, `__int128` 16/16, `max_align_t` 32/16), pointers and pointers to member (8), and libstdc++'s `std::string` 32/8, `std::string_view` 16/8, `std::vector<int>` 24/8, `std::unique_ptr<int>` 8/8, `std::shared_ptr<int>` 16/8, `std::map<int,int>` 48/8, `std::optional<int>` 8/4, `std::variant<int,double>` 16/8, `std::function<void()>` 32/8, `std::type_info` 16/8, `std::exception` 8/8, `std::initializer_list<int>` 16/8 |
| `headers.sha256` | libstdc++ 15.2.0's 834 installed header files (`/usr/include/c++/15.2.0`) |

The target is LP64 (`long` 8), `char` signed, `wchar_t` 4 (`int`), `long double` x87 80-bit stored in 16 bytes (the IR's
`x86_fp80`, survey §5.6, CP12), `std::size_t` `unsigned long`.

### 14.2 Record layouts (survey P5, settled)

`-Xclang -fdump-record-layouts[-simple]` prints **nothing for a record the program does not use in generated code**
(the survey's P5 finding: the layout is computed when CodeGen needs it); with `-S -emit-llvm` it prints, per record used, the
**AST layout** (bit offsets for the `-simple` form: `Size`, `DataSize`, `Alignment`, `BaseOffsets`, `VBaseOffsets`, `FieldOffsets`;
a text table with byte offsets, bit-field ranges `0:0-2`, the `vtable pointer`, primary bases, `sizeof`, `dsize`, `align`,
`nvsize`, `nvalign`) and the **IR layout** (the `CGRecordLayout`: the LLVM type, the non-virtual base type, bit-field storage).
Example, `struct B { int b; virtual void f(); };`, `struct D : B { char c; double d; void f() override; };`,
`struct F { unsigned a:3; unsigned b:5; char c; };` (used in global variables):

```
 0 | struct D            sizeof=24, dsize=24, align=8, nvsize=24, nvalign=8
 0 |   struct B (primary base)
 0 |     (B vtable pointer)
 8 |     int b
12 |   char c
16 |   double d
 0 | struct F            sizeof=4, dsize=4, align=4
0:0-2 |   unsigned int a
0:3-7 |   unsigned int b
    1 |   char c
```

and, in the IR: `%struct.D = type { %struct.B.base, i8, double }`, `%struct.B.base = type <{ ptr, i32 }>`, `%struct.F = type
{ i8, i8, [2 x i8] }` with the data layout `e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-i128:128-f80:128-n8:16:32:64-S128`.
gcc's `-fdump-lang-class` prints sizes, bases and vtables for the same classes (`Class D size=24 align=8 base size=24 base align=8`,
the vtable `D::_ZTV1D: 3 entries`), the second opinion on the layout. **Recommended (proposal CP20):** layouts are facts
(`:layout`) taken from the oracle by a **CodeGen pass of its own**: the helper runs the program through
`-S -emit-llvm -Xclang -fdump-record-layouts-simple -o file` once and parses the AST layouts, which exist only for the
records used; a record not used by the unit's code gets no layout (the converter does not need one); for eval the IR's
struct types are the layout.

### 14.3 Coroutines in the IR (survey P8, settled)

A coroutine that suspends (a generator with `co_yield`, `suspend_always` and `final_suspend`) compiled at `-O0` by clang
23.1.4 gives **no `llvm.coro.*` intrinsic call** (a count of 0 in the IR of the 9-line probe): the coroutine passes ran
and the IR holds the **split functions**: the ramp `@_Z5counti` (allocates the frame by `operator new`, constructs the promise,
runs `initial_suspend`'s `await_ready`/`await_suspend`/`await_resume` as calls, returns the handle), `@_Z5counti.resume`
(the body, with a `switch` on the frame's suspend index), `@_Z5counti.destroy` and `@_Z5counti.cleanup`, and
`internal` wrappers `@_Z5counti.__await_suspend_wrapper__init|yield|final`. The frame is a struct holding the resume and
destroy function pointers, the suspend index and the promise and locals. The probe (`count(3)` yields 0, 1, 2 and
`main` returns their sum) exits with status 3 under both g++ and clang. **The lowering to C forms (CX6) therefore
sees ordinary functions, `switch` and indirect calls**; `std::coroutine_handle<void>::resume` is the library's call through the
frame's first word. libstdc++'s coroutine types live in the inline namespace `std::__n4861` (the mangling `St7__n4861`).

## 15. Read and printed, out of eval and AOT

Read and printed (the whole language but named-module interfaces, CP1) and not evaluated or compiled (v1):

- **Modules** (CP13): `export module a.b;`, `module a.b:part;`, `module :private;`, `import a.b;`, `import <h>;` (header units),
  `export` declarations: `(cpp/module a.b)`, `(cpp/module a.b :partition part)`, `(cpp/module-private)`, `(cpp/import a.b)`,
  `(cpp/import "<h>")`, `^:export` on a declaration and `(cpp/export decl*)` for a block. `import std;` is not read by g++ 15
  here (survey P10: the standard module is not built on the host) and clang has no `std` module for libstdc++; a class
  `modules` of the read corpora (`g++.dg/modules`, 1,655 files). Compiled module interfaces (gcm, pcm) are not forms.
- **Threads** (`std::thread`, `std::jthread`, mutexes, `thread_local` in a multi-threaded program), **the filesystem**, locales
  other than "C", signals: read and printed, not run.
- **Compiler extensions beyond the corpora's needs**: `__attribute__` as C's `:c/gnu-attrs`, vector types, `_Complex`, `__int128`
  (C's), `asm` (C's), `#pragma omp`. The attributes of clang (`[[clang::...]]`) are read; they are metadata.
- **Reflection, contracts, `std::execution`** (GCC 15 has none) and C++26 features beyond what `-std=c++2c` of the
  oracle accepts: the dialect is data.

## 16. Coverage

The language, clause by clause (ISO/IEC 14882:2024 clause 5 to 15 and annex A), against the forms; "form" means §6-§9 of
this spec, "fact" §12, "layer" §10, "dropped" a derived property:

| clause | content | covered by |
|---|---|---|
| 5 | lexical conventions: tokens, alternative tokens, raw strings, literals, UDLs | §9.1, CX2's lexer; `:full` keeps spelling |
| 6 | basics: declarations and scopes, linkage, ODR, lifetime, types | §4-§6; lifetime and ODR are derived |
| 7 | expressions: every operator, new/delete, casts, lambdas, fold, requires, coroutines | §9 |
| 8 | statements: if constexpr, range-for, init-statements, try, coroutine statements | §9.6, §9.7 |
| 9 | declarations: specifiers, enums, namespaces, using, linkage, attributes, structured bindings, `asm` | §5, §6, §4.5 |
| 10 | modules | §15 (read and print) |
| 11 | classes: members, bases, virtual, special members, unions, bit-fields, nested | §7 |
| 12 | overloading: operator functions, user-defined literals, conversions | §6.3 (the resolution is the oracle's) |
| 13 | templates: all kinds, deduction guides, concepts, constraints, specializations, variadics | §8 |
| 14 | exception handling: try, throw, noexcept, function-try-block | §9.6, §6.2 |
| 15 | preprocessing directives | §10 |
| annex A | the grammar | the converter's table (§12.2) is closed against the g++.dg compile corpus (CX4) |

The syntax productions are the table of clang's AST kinds (§12.2) and not a separate list: unlike C's 15.1, no
cc-style production list exists (no native parser, CP3 (a)); the ambiguities that a native parser would need a
name-kind table for are the oracle's (survey §4.4).

### 16.1 Reserved heads

The heads of C++ forms in C++ context, a list the printer and a consumer need (C's reserved heads, C's §15.4, hold):
`cpp/file cpp/namespace cpp/namespace-alias cpp/using cpp/using-namespace cpp/using-enum cpp/alias cpp/linkage cpp/struct cpp/class
cpp/union cpp/enum cpp/enum-class cpp/enum-struct cpp/ctor cpp/dtor access cpp/friend cpp/template cpp/instantiate
cpp/deduction-guide cpp/concept cpp/requires cpp/simple cpp/type-req cpp/compound cpp/nested cpp/construct cpp/brace cpp/paren cpp/new
cpp/new-array cpp/delete cpp/delete-array cpp/static-cast cpp/const-cast cpp/reinterpret-cast cpp/dynamic-cast cpp/lambda cpp/try cpp/catch
cpp/function-try cpp/for-each cpp/if-consteval cpp/decl cpp/structured cpp/fold cpp/udl cpp/scope cpp/global cpp/member-call cpp/dtor-call
cpp/module cpp/module-private cpp/import cpp/export cpp/defaulted cpp/deleted operator <> & && member-ptr typeid typeid-type noexcept
sizeof... pack-expand co-await co-yield co-return throw decltype decltype-auto constrained typename expr template`.

## 17. Worked examples

Each C++ text below was compiled by g++ 15.2 and clang 23.1.4 where it is a whole program (§2, §17.5-§17.7) or taken from
go-whim's whim++ at `80c49bf5` (`/root/go-whim/wpp/src`, §17.1-§17.4); every forms block of this spec was read by Arbace's reader
on stage 0 (`target/seed/amd64/arbace`, `bin/repr-cpp spec-check`).

### 17.1 A class with virtual functions (whim++'s `host.hpp`)

```cpp
namespace whimpp {
struct WinSize
{
    int rows;
    int cols;
};
class Host
{
public:
    virtual ~Host() = default;
    virtual void init(std::function<void(int)> deathtrap) = 0;
    virtual std::optional<WinSize> win_size() = 0;
};
}
```

```clojure
(cpp/namespace whimpp
  (cpp/struct WinSize [] (c/var ^int rows) (c/var ^int cols))
  (cpp/class Host []
    (access :public)
    ^:virtual (cpp/dtor Host (cpp/defaulted))
    (c/declare ^:virtual ^:pure init ^void [^{:tag (<> std/function (fn [int] void))} deathtrap])
    (c/declare ^:virtual ^:pure win_size ^{:tag (<> std/optional WinSize)} [])))
```

The class has a vtable pointer (a fact `:layout`, §14.2), an implicit copy constructor that `~Host() = default` suppresses
the move of (`:def-data`); none of it is a form.

### 17.2 Exceptions and a member function (whim++'s `host.cpp`)

```cpp
void Editor::host_exit(int r)
{
    throw HostExit{r};
}

void *Editor::host_alloc(usize n)
{
    constexpr std::size_t align = alignof(std::max_align_t);
    const std::size_t want = (n + (align - 1)) & ~(align - 1);
    const std::size_t used = glue_->used.fetch_add(want, std::memory_order_relaxed);
    return glue_->arena.get() + used;
}
```

```clojure
(c/defn Editor/host_exit ^void [^int r]
  (throw (cpp/brace HostExit r)))

(c/defn Editor/host_alloc ^{:tag (* void)} [^usize n]
  (let [^:constexpr ^std/size_t align (alignof-type std/max_align_t)
        ^{:tag (const std/size_t)} want (bit-and (+ n (- align 1)) (bit-not (- align 1)))
        ^{:tag (const std/size_t)} used ((.-fetch_add (-> glue_ used)) want std/memory_order_relaxed)]
    (return (+ ((.-get (-> glue_ arena))) used))))
```

`throw HostExit{r}` is a `throw` of a braced construction; the unwinding to `run` is the oracle's landing pads (survey §5.4).
`glue_->used` is `(-> glue_ used)`; `.fetch_add(...)` is a call of a member access whose object is that expression; the
`std::atomic<std::size_t>` type of `used` is a fact. The out-of-class definition's name is the qualified symbol `Editor/host_alloc`.

### 17.3 Named casts and the C subset (whim++'s `editor.cpp`)

```cpp
bool Editor::ascii_isupper(int c)
{
    return static_cast<unsigned>(c) - 'A' < 26;
}
```

```clojure
(c/defn Editor/ascii_isupper ^bool [^int c]
  (return (< (- (cpp/static-cast unsigned c) \A) 26)))
```

whim++ has 5,719 named casts: the converter's commonest C++ form. The comparison is `<` on `unsigned`; `'A'` is `\A`, an
`int` constant in C and a `char` one in C++ (a fact on the literal: C++ differs from C here, and `sizeof('A')` is 1).

### 17.4 Scoped enumerations and pointers to members (whim++'s `editor.hpp`)

```cpp
enum class SpecialKey : int
{
    KS_NAME = 0,
    KS_CE,
    KS_AL
};
using opt_did_set_cb_T = char *(Editor::*)([[maybe_unused]] optset_T *args);
// in a struct:  char_u *(Editor::*ea_getline)(int, int, getline_opt_T);
```

```clojure
^{:cpp/underlying int} (cpp/enum-class SpecialKey [(KS_NAME 0) KS_CE KS_AL])

(cpp/alias opt_did_set_cb_T
  (member-ptr Editor (fn [^{:tag (* optset_T) :c/attrs [maybe_unused]} args] (* char))))

(c/var ^{:tag (member-ptr Editor (fn [int int getline_opt_T] (* char_u)))} ea_getline)
```

### 17.5 The most vexing parse (survey P6)

```cpp
struct T{}; struct U{ U(T){} };
int main(){ U u(T()); }
```

clang reads `u` as a function declaration of type `U (T (*)())` (warning `-Wvexing-parse`), as the standard says: the forms

```clojure
(cpp/struct T [])
(cpp/struct U [] (cpp/ctor U [T] []))
(c/defn main ^int []
  (let [(c/declare u ^U [(* (fn [] T))])]))
```

and `U u((T()));` is a variable with a parenthesised construction, `(c/var ^U u (cpp/paren (cpp/construct T)))`. The
oracle decides; a native parser would need the rule of §4.4 of the survey. Parenthesising the argument is how the program
means the other reading, and the forms differ by structure, not by a flag.

### 17.6 Record layouts as facts (survey P5)

```cpp
struct B { int b; virtual void f(); };
struct D : B { char c; double d; void f() override; };
struct F { unsigned a:3; unsigned b:5; char c; };
```

```clojure
(cpp/struct B [] (c/var ^int b) (c/declare ^:virtual f ^void []))
(cpp/struct D [B] (c/var ^char c) (c/var ^double d) (c/declare ^:override f ^void []))
(cpp/struct F [] (c/var ^{:tag unsigned :c/bits 3} a) (c/var ^{:tag unsigned :c/bits 5} b) (c/var ^char c))
```

and the facts file (`a.facts.ae`, §12.1; the paths are item indices):

```clojure
{[0] {:layout {:size 16 :dsize 12 :align 8 :nvsize 12 :nvalign 8 :vptr 0
               :fields [{:name b :offset 8}]
               :llvm "%struct.B = type <{ ptr, i32, [4 x i8] }>"}}
 [1] {:layout {:size 24 :dsize 24 :align 8 :nvsize 24 :nvalign 8
               :bases [{:type B :offset 0 :primary true}]
               :fields [{:name c :offset 12} {:name d :offset 16}]
               :llvm "%struct.D = type { %struct.B.base, i8, double }"}}
 [2] {:layout {:size 4 :dsize 4 :align 4
               :fields [{:name a :offset 0 :bits [0 2]} {:name b :offset 0 :bits [3 7]} {:name c :offset 1}]
               :llvm "%struct.F = type { i8, i8, [2 x i8] }"}}}
```

The layouts exist only when the unit's code uses the records (`D gd; F gf;` made them used in the probe, §14.2).

### 17.7 A suspending coroutine (survey P8)

```cpp
#include <coroutine>
struct Gen { struct promise_type { int v;
  Gen get_return_object(){ return Gen{std::coroutine_handle<promise_type>::from_promise(*this)}; }
  std::suspend_always initial_suspend(){return {};} std::suspend_always final_suspend() noexcept{return {};}
  std::suspend_always yield_value(int x){ v=x; return {}; } void return_void(){} void unhandled_exception(){} };
  std::coroutine_handle<promise_type> h; };
Gen count(int n){ for(int i=0;i<n;i++) co_yield i; }
int main(){ Gen g=count(3); int s=0; while(!g.h.done()){ g.h.resume(); if(!g.h.done()) s+=g.h.promise().v; } g.h.destroy(); return s; }
```

(g++ and clang both exit with status 3.)

```clojure
(cpp/struct Gen []
  (cpp/struct promise_type []
    (c/var ^int v)
    (c/defn get_return_object ^Gen []
      (return (cpp/brace Gen ((cpp/scope (<> std/coroutine_handle promise_type) from_promise) @this))))
    (c/defn initial_suspend ^std/suspend_always [] (return (cpp/brace)))
    (c/defn ^:noexcept final_suspend ^std/suspend_always [] (return (cpp/brace)))
    (c/defn yield_value ^std/suspend_always [^int x] (set! v x) (return (cpp/brace)))
    (c/defn return_void ^void [])
    (c/defn unhandled_exception ^void []))
  (c/var ^{:tag (<> std/coroutine_handle promise_type)} h))

(c/defn count ^Gen [^int n]
  (for [^int i 0] (< i n) (inc! i) (co-yield i)))

(c/defn main ^int []
  (let [^Gen g (count 3)
        ^int s 0]
    (while (not ((.-done (.-h g))))
      ((.-resume (.-h g)))
      (when (not ((.-done (.-h g))))
        (set! s + (.-v ((.-promise (.-h g)))))))
    ((.-destroy (.-h g)))
    (return s)))
```

`count` has no `co_return` and no `co_await`: `co_yield` makes it a coroutine; `return {}` is `(return (cpp/brace))`. In the IR it is the
ramp function `@_Z5counti` and the `.resume`, `.destroy`, `.cleanup` functions (§14.3).

## 18. Changes to the survey's sketch, open questions

### 18.1 Changes to the survey's §3

- **Qualified names** are single symbols with `/` (`a/b/c`), not `(cpp/scope ...)` lists: the Clojure reader refuses `::` inside a
  symbol, and one symbol keeps the forms short (principle 4).
- **Heads**: the survey named `cpp/class`, `cpp/using`, `cpp/static-cast`, `cpp/new`, `cpp/lambda`, `cpp/try`, `cpp/concept`,
  `cpp/requires`, `cpp/module`, `cpp/import`, `cpp/co-await`. This spec keeps all but `cpp/co-await`: `co_await`, `co_yield` and
  `co_return` are `co-await`, `co-yield`, `co-return` without a family prefix (they are statements and operators as `return`,
  `throw`, `sizeof` are), and `cpp/struct`, `cpp/union` and `cpp/enum-class` are added (survey §3.1 named one class head).
- **Facts**: the survey wrote them "as metadata or in a table"; the spec chooses a table beside the unit (CP21) because a literal
  cannot carry metadata.
- **Instances**: the survey's `:cpp/instances` is kept, in the template form's metadata (§8.5).
- **`nullptr`** is `nil` (C's rule), **`bool`** the symbol `bool` (C's `_Bool` is not used in C++).

### 18.2 Open questions

The questions this milestone raises are the survey's proposals CP20 and on ("As built: CX1"): layouts' source (CP20), the
facts' storage (CP21), the spelling of qualified names (CP22), the compiler whose predefined macros and search list the
preprocessor takes (CP23), the helper's output format (CP24), the libTooling packages (CP25), header units' forms (CP26),
`cpp` as a user's namespace (CP28), and the set of standards (CP27).

### 18.3 As built: CX4's encoding (2026-10-30)

The converter of CX4 (`arbace.repr.cpp.convert`, survey "As built: CX3, CX4") writes the forms above with these
departures and additions, each a proposal of the survey (CP40 and on) until the user decides:

- **A braced list without a type** in an expression (`return {1, 2};`, `f({1})`, `x = {}`) is `(cpp/init-list e*)`;
  `(cpp/brace T e*)` is always the typed `T{e*}`; in an initializer's place (`c/var`, `cpp/new`, a member initializer)
  `(cpp/brace e*)` is the declaration's own braces, `^:cpp/copy` for `= {...}` (§9.4 wrote both as `cpp/brace`, which a
  printer cannot tell apart: a type symbol and an expression symbol look the same). CP40.
- **Text forms.** A node the converter has no form for, or whose first or last token comes from a macro expansion, is
  `(cpp/text "...")`: the source text of its extent, a macro invocation with its argument list as written (§9.4 of the C
  forms recovers invocations the same way; here the unit is the outermost node whose boundary touches an expansion).
  As a statement or a declaration the text holds its `;`; an expression statement of a text is `(c/expr (cpp/text ...))`.
  The converter counts them by kind; a unit with none is fully structured. CP41.
- **Constructor and destructor declarations** (no body) are `^:cpp/declare (cpp/ctor S [params])` and
  `^:cpp/declare (cpp/dtor S)`; without the flag `(cpp/dtor S)` is `~S() {}` (§7.2). CP42.
- **Elaborated type specifiers** are `(cpp/elaborated struct S)` in a type position (clang prints `struct S` where the source
  wrote it; N9 is kept, not normalized, so that the oracle's types of the print equal the original's). CP43.
- **Attributes** are kept as their texts by syntax, in the name's metadata: `:cpp/attrs ["nodiscard" "gnu::cold"]`
  (`[[...]]`), `:c/gnu-attrs ["noinline"]` (`__attribute__((...))`), `:cpp/alignas ["alignas(16)"]`; on a statement
  `:cpp/attrs` on its form (`[[fallthrough]];`); `override` and `final` are `^:override`, `^:final`. Printed before the
  declaration (§5.6 of the C forms places them; CP44).
  **As built (CP44 (b))**, the attribute encoding: each attribute is `name`, `ns/name` (`gnu::cold` is `gnu/cold`) or `[name arg*]`
  for one with arguments (an argument: an integer, an identifier, a plain string, else `(cpp/text "...")`; `[name]` for `name()`), in
  the vectors `:cpp/attrs` (`[[...]]`) and `:c/gnu-attrs` (`__attribute__((...))`); `alignas(N)` is `:cpp/alignas N` (integers) and
  its text otherwise; what is none of these is `(cpp/text "...")`. Inherited and implicit attributes are the oracle's.
- **User-defined literals** are `(cpp/udl "LITERAL" "SUFFIX")`, the literal part's spelling and the suffix (§9.4's value
  form waits for the literal operator's fact). **Literals** otherwise follow the C forms: an `int` is a number, another
  integer type `(lit unsigned 3)`, `(lit long 3)`; a `char` a character or `(lit char 200)`, the others `(lit wchar_t 65)`,
  `(lit char16_t 65)`; a string a string, a narrow one with bytes that are not UTF-8 `(byte-string [...])`, a prefixed one
  `(lit wchar_t [code units])`; `double` a number, `(lit float 2.5M)`, `(lit long-double 2.5M)`; `nullptr` `nil`, `__null`
  the symbol.
- **A declaration statement with several declarators** (`int a, *b;`) is `(cpp/decls (c/var ...) (c/var ...))` in a `let`'s
  bindings (one `DeclStmt` in the oracle's AST: two declarations would be two). At namespace scope and in a class the
  declarators are separate forms (the AST has no statement there).
- **A null statement** is `(c/null)`; a name that is one of the printer's heads or not a readable symbol is `(c/id "...")`
  (`when`, `init`, `access`, `nil`, ...).
- **Template parameters**: `(typename T :constraint Num)` prints `Num T`; `class` and `typename` are one (N8) and the AST level
  normalizes `tagUsed` on template type parameters to match.
- **Parameters' types** are read from the parameter's text when it reads as a declaration (clang prints a parameter's type
  decayed and desugared: `va_list ap` as `__va_list_tag *`), else from clang's type.

CX4b (the fix round after CX4, survey "As built: CX4b, CX5a") adds:

- **A copy-initializer that is a typed or parenthesized construction** (`int c = int{i};`) is the form with `^:cpp/expr` in its
  metadata: `(c/var ^int c ^:cpp/expr (cpp/brace int i))`, printed ` = int{i}` (without the flag `(cpp/brace e*)` in an
  initializer's place is the declaration's own braces, CP40).
- **A member initializer that is a pack expansion** (`T(args)...`) has `^:pack` on its form.
- **Placement new** is `(cpp/new T init? :place [p*])` and `(cpp/new-array T n init? :place [p*])` (§9.4, as the table's row).
- **Explicit instantiations** are `(cpp/instantiate decl)` with `^:extern` for `extern template`; the declaration is `(c/declare
  name ^ret [params])`, `(c/var ^T name)` or `(cpp/class (<> W int))` (§8), read from the text.
- **Structured bindings** are `(cpp/structured ^{:tag T} [a b] init?)` (§9.4's table), `T` the written specifiers
  (`auto`, `(& (const auto))`, ...), `init` as a variable's (`^:cpp/expr`, `(cpp/paren e)`, `(cpp/brace e*)`).
- **Partial specializations** are `(cpp/template [params] (cpp/struct (<> V T *) [bases] members))` with the written template-id
  (read from the text); under an enclosing template's header the outer headers wrap it, as for an out-of-line member.
- **Nested namespace definitions** keep their inline parts: `namespace A::inline B::C` is `(cpp/namespace A/B/C ...)` with
  `{:cpp/inline-parts [1]}` on the name symbol.
- **Abbreviated function templates** (`void f(auto p)`) are the function alone: the invented template parameters are not forms.
- **Directives** (`c/include`, `c/pragma`, `c/directive`) stand between a compound statement's statements too, and the ones inside
  an executed `#if` group are kept (the helper's `{:executed [LINE ...]}` line, CP45 (a)); the conditional lines themselves are not.
  A `#pragma` that clang reads as a loop hint is the directive, not an attribute of the statement.
- A dependent construction `T(a)` / `T{a}` is `(cpp/construct T a)` / `(cpp/brace T a)`.

## 19. Sources

- The survey: [CPP-SURVEY.md](CPP-SURVEY.md) §1-§4, §11 (CX1), §12 (CP1-CP14, decided 2026-10-30).
- C forms: [../c/SPEC.md](../c/SPEC.md) (this file's base), [../C23-SURVEY.md](../C23-SURVEY.md) ("As built: C3b" on the branch `c23-c3b`).
- ISO/IEC 14882:2024 (C++23): the clause numbers; the compilers' documentation.
- clang 23.1.4 (`Alpine clang version 23.1.4`): `-Xclang -ast-dump=json` (the node kinds of §12.2; observed in the dumps of P1 and of
  `io.cpp`), `-Xclang -fdump-record-layouts[-simple]`, `-S -emit-llvm`; clang's AST kinds are named in `clang/include/clang/AST/` of
  `llvm-project` at `llvmorg-23.1.4` (`test/cpp/corpora.edn`'s pin).
- g++ 15.2.0: `-fdump-lang-class`, `-dM -E`, `-std=` names; libstdc++ 15.2.0 (`/usr/include/c++/15.2.0`).
- go-whim `/root/go-whim` at `80c49bf5` (`test/cpp/corpora.edn`): `wpp/src/host.hpp`, `host.cpp`, `editor.cpp`, `editor.hpp`.
- The host data: `test/cpp/host/`; the pins: `test/cpp/corpora.edn`; the helper: `tools/cxxdump`; the script: `bin/repr-cpp`.
