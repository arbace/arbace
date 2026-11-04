# repr/cpp's native parser (CX11)

Status: CX11's first part, on the branch `cpp-native` from `cpp-cx9` (2026-11-04); its second part on `cpp-native2` from
`cpp-native` (2026-11-04, §7); Sema's minimum for the recorded classes on `cpp-sema` from `cpp-native2` (2026-11-04, §8); the tree level on
`cpp-tree` from `cpp-sema` (2026-11-04, §9). The survey's §4.4 and its CX11 row
([CPP-SURVEY.md](CPP-SURVEY.md) §11) planned a C++ parser of Arbace's own, "name-kind tracking, a port of the grammar", so that
repr/cpp reads C++23 without the oracle's AST dump at read time; clang stays the oracle. This note is its design: what C++'s
parsing needs beyond C's (§1), the reference model and why (§2), the design as built (§3), the check (§4), what is left (§5).

Words: **the parser** is `arbace.repr.cpp.parse`; **the converter** `arbace.repr.cpp.convert` (CX4-CX9's; the second part fixed four of its bugs, §7); **the
oracle's tree** is clang 23.1.4's `-ast-dump=json` as `tools/cxxdump` keeps it; **the native tree** the parser's.

## 1. What C++'s parsing needs beyond C's

C's parser (`arbace.repr.c.parse`, a port of cc's) tracks one kind of name, the typedef names, in the scopes it builds as it goes;
its grammar is otherwise context-free. C++'s is not, in ten ways:

1. **Names' kinds.** A name is a type (a class, an enumeration, a typedef or alias, a template type parameter, the injected class
   name), a template (a class, alias, function or variable template, a concept, a template template parameter), a namespace, or a
   value (a variable, a function: the difference matters to C++20's assumed template names). The kinds come from C++'s scopes:
   namespaces (reopened, inline, unnamed, aliased, nominated by using directives), classes (their members, their bases', the
   injected class name), template parameter lists, function parameters, blocks, and qualified lookup through `::`.
2. **Template argument lists.** `<` opens one only after a template's name ([temp.names]/3: found as a template, after `template`,
   or, from C++20, an unqualified name lookup finds nothing or only functions for); `>` closes it (no operator at its top level,
   one again inside parentheses), and `>>` closes two (C++11). Template-ids stand in every name position: nested-name-specifiers,
   declarator-ids, base-specifiers, mem-initializers, elaborated type specifiers.
3. **Dependent names.** `T::x` is a value unless `typename` says a type (or the context admits only a type, P0634), `T::template
   f<int>` a template; a dependent base's members are not found.
4. **The declaration-or-expression ambiguities**, which the standard resolves by syntax given the names' kinds: [stmt.ambig] (a
   statement that can be a declaration is one: `T(x);`, `T(x) = 1;`, `T *x;`), [dcl.ambig.res] (a parameter list or a direct
   initializer: the most vexing parse `U u(T());`; a type-id or an expression in parentheses: `(T())` cast or construction,
   `sizeof(T())`), a condition's or a for's declaration, a template argument's type-id or expression.
5. **Declarators**: references, pointers to members `C::*`, function declarators with cv- and ref-qualifiers, exception
   specifications and trailing return types, `override`/`final`, `= 0`, `= default`, `= delete`; parameter packs; structured
   bindings; the declarations without a type specifier (constructors, destructors, conversion functions, deduction guides).
6. **Attributes** in their places: `[[...]]` (with `using ns:`) before the specifiers, after the declarator-id, between a
   declarator's parts, on statements; GNU's `__attribute__((...))`; `alignas`.
7. **Expressions** beyond C's: qualified names, `this`, `new` and `delete` (placement, arrays, `::`), the named casts, functional
   casts and constructions `T(x)`, `T{x}`, `typeid`, `noexcept`, `sizeof...`, `<=>`, `.*`, `->*`, lambdas, braced lists, fold
   expressions, requires-expressions, `co_await`, `co_yield`, `throw`.
8. **Statements**: range-for (with an init-statement), `if`/`switch` with init-statements, `if constexpr`, `if consteval`,
   `try`/`catch`, declarations as conditions.
9. **Classes**: member declarations and access specifiers, base clauses, member templates, friends, nested classes, constructors'
   mem-initializers, and the complete-class context: a member function's body sees the members declared after it.
10. **The headers.** The kinds of a program's names come from what it includes (`std::vector` is a template only if `<vector>`'s
    declarations say so): libstdc++'s headers are the parser's input too.

## 2. The reference model

Three were open:

- **(a) A port of clang's `lib/Parse/`** (Parser, ParseDecl, ParseDeclCXX, ParseExpr, ParseExprCXX, ParseStmt, ParseTemplate,
  ParseTentative; tens of thousands of lines) with Sema's minimum behind its Actions.
- **(b) The standard's grammar** (ISO/IEC 14882:2024, Annex A) with its disambiguation rules, Arbace's own code.
- **(c) A port of gcc's `cp/parser.cc`** (on the host in the pinned tarball).

**Chosen: (b), decided as (a) decides.** The productions are Annex A's; every place where the standard leaves the parser a choice or
a heuristic is decided as clang's Parse decides it, and its disambiguation is followed function for function: ParseTentative's
`isCXXDeclarationSpecifier`, `TryConsumeDeclarationSpecifier`, `TryParsePtrOperatorSeq`, `TryParseDeclarator`,
`TryParseParameterDeclarationClause`, `TryParseFunctionDeclarator`, `isCXXFunctionDeclarator`, `isCXXSimpleDeclaration`,
`isCXXTypeId`, `isCXXConditionDeclarationOrInitStatement` (the TPResult values True, False, Ambiguous, Error), ParseExpr's
`ParseCXXAmbiguousParenExpression` (an ambiguous `(T())` is a cast only if a cast-expression follows its `)`), ParseTemplate's
`ParseGreaterThanInTemplateList` (`>>`, `>=`, `>>=` split), Sema's `isTemplateName` with C++20's assumed templates.

Why: the forms are the converter's reading of clang's tree, so the native reader gives the same forms only if it decides each
ambiguity as the oracle does: clang, not g++, whose parser differs in its recovery and its extensions (so not (c)). A port line for
line (a) is not possible without Sema: clang's Parse calls into Sema at every step (the ActOn callbacks, ClassifyName, the annotated
tokens), and what is portable of it is the disambiguation algorithm, which is taken. Sema's minimum is Arbace's own: the name-kind
table (§3.3). repr/c's and repr/java's parsers are ports because their front ends' parsers are separable from their semantics (cc
keeps its own scopes, javac's parser needs no lookup); C++'s is not.

## 3. The design as built

**3.1 The tree is clang's AST as data.** The parser gives the main file's declarations as `tools/cxxdump` writes clang's: each node a
map with its `:kind`, `:range` (begin and end locations: offset, file, token length as clang measures it; a macro's token at its
expansion), `:loc` and the fields the converter reads (names, `:type {:qualType}`, flags, `:inner`). The converter is unchanged and
reads either tree: its text forms, declaration groups, directives, macro forms and mode `:full` apply alike. This is repr/java's
pattern (JN8, JFS66 (b): javac's trees as data, one converter) and repr/c's (cc's tree); the native tree can be compared with the
oracle's node for node (a later part, §5). Where clang's tree carries what a parser cannot know, the parser writes what the converter
reads into the same forms (§3.8) or leaves it to a class (§4).

**3.2 Tokens.** The shared preprocessor (`arbace.repr.c.pp` in C++ mode) with clang's host data: its predefined macros and search
list, since the forms are the oracle's reading of the file (CP23). Its stream becomes the parser's tokens: keywords by standard
(C++11, C++20, GNU modes' `typeof`), alternative tokens as their operators, digraphs as their primaries, `>>`, `>=`, `>>=` split into
glued pieces whose locations measure as clang measures them (a split `>`'s token length is the source token's). The main file's
tokens are those of its file; a macro's tokens are at their invocation (clang's expansion location).

**3.3 Sema's minimum** (`arbace.repr.cpp.scope`): scopes of kinds namespace, class, enumeration, template parameters, function
parameters, block; an entity a map `{:k :ns|:type|:var|:fn|:concept :tmpl :scope ...}`. Unqualified lookup goes outward, a
namespace's inline namespaces and nominated namespaces with it, a class's bases (those not dependent); qualified lookup in a
namespace or a class (an alias's target's). A value hides a class of its name (the `struct stat` rule). What depends on an
instantiation is not known (a member of a dependent base, `T::x`): a value, as [temp.res] says.

**3.4 The headers** are parsed for their names alone (skim mode, as clang's `-skip-function-bodies`): the declarations, their
specifiers and declarators, template headers and class heads, with bodies, initializers, default arguments, bit-field widths,
exception specifications, requires-clauses and static assertions skipped by their brackets; a declaration the parser cannot read is
skipped to its end. `<iostream>`, `<vector>` and `<algorithm>`: 368,465 tokens, 9.1 s (the preprocessor's 4.7 s before).

**3.5 The complete-class context.** A class's body is skimmed for its members' declarations before it is parsed, so that a member
function's body, a default argument or a default member initializer sees the whole class ([class.mem]/7: clang parses them after the
class).

**3.6 Types as clang prints them** (its TypePrinter), since the converter reads a declaration's `qualType`: builtin types in
clang's spelling, cv-qualifiers first, `int (*)[3]`, `void (C::*)(int) const`, parameters decayed in a function's type, `__restrict`,
a template argument's integer as clang's StmtPrinter writes it (`16` for `0x10`), the injected class name of a class template with
its parameters (`W<T, U>` for `W`), of a specialization with its arguments, a `constexpr` variable's type `const`, an unnamed class
named by its typedef for linkage, GNU `noreturn` and `nothrow` as type attributes (also when a header's declaration had them: a
library function redeclared). A deduced type is not printed: the converter reads `auto` from the text.

**3.7 Literals as clang holds them** (`arbace.repr.cpp.lit`): an integer's value in decimal and its type by [lex.icon]'s table on LP64
(`1z` clang's `__signed_size_t`); a character's value (a narrow one sign-extended as clang's unsigned holds it, a multicharacter
one an int); a string's code units after concatenation, escaped as clang prints them; a float's and a long double's value as LLVM's
`APFloat::toString` prints it (FormatPrecision 0: 9 and 21 significant digits, its truncating and rounding steps), a double's any
spelling of its value.

**3.8 What clang's tree has that the parser writes too**: an init-capture's initializer (the converter finds it by its offset among
the initializers clang lists, one a capture: the parser writes the init-captures' alone, §7); a static assertion without a message has an empty second child; an anonymous union in a
block or a namespace has its implicit object, and a use of its member is a member access through it; a scoped enumeration's fixed
type `int`; a using-declaration through a qualifier whose template arguments depend on a template parameter is
`UnresolvedUsingValueDecl`; the builtins that are expressions of their own (`__atomic_*` outside templates, `__builtin_offsetof`, type
traits; `__builtin_is_constant_evaluated` is a call); attributes clang does not know are dropped (an unknown `[[x]]`,
`__attribute__((noclone))`, `noipa`, a `format` of gcc's own kinds), GNU attributes after a class's or an enumeration's closing brace
are the type's, a `[[...]]` among the ptr-operators appertains to the pointer type (dropped), an attribute-declaration `[[...]];` at
namespace scope is an `EmptyDecl`; calling conventions (`fastcall`, `ms_abi`...) are attributes of the function's type, and a
leading one begins the declaration's range; a deallocation function is implicitly `noexcept`; a cast's type has no top-level
cv-qualifiers unless a class; a specialization's template arguments are converted to the primary template's parameters (a `bool`'s
`true` is -1, a default argument filled in, a pack one pack argument); a non-type parameter whose type expands an enclosing pack
keeps the expansion; the mem-initializers in their initialization order (virtual bases, other bases, members as declared) when the
class's members are known; a non-static member named in a `noexcept` is `this->` it; `1.0i` is a literal operator's when one is
declared (`<complex>`'s), else GNU's imaginary literal; `X(T::type)` in a class is a constructor (P0634, an extension before C++20);
an unqualified name followed by `<...>(` a template's in every mode (clang's extension before C++20); the first half of a split `>>`
is clang's SplitToken location (in the scratch space); `__FILE__` is the name the oracle was given; an explicit instantiation's text
runs to its `;`, its function's parameters' names in a `FunctionProtoTypeLoc`; a class defined in an alias declaration is a node of
its own before the alias.

**3.5b The complete-class scope is apart.** The skim of a class's body declares its members in a scope of its own, which a member
function's body, a mem-initializer, a default member initializer and a `noexcept` look up first; the members' declarations are parsed
in the class's scope as it fills (so `Kind Kind;` names the enumeration `Kind` declared before it).

**3.9 Declines.** A main-file statement or declaration the parser cannot read is a node of kind `NativeDecline` over its tokens: the
converter writes it as its text (`(cpp/text "...")`) and counts it under that kind; the parse goes on after it.

**3.10 Designated initializers** in clang's semantic form: by the declaration's or the functional cast's type, an aggregate's
designated elements at their members' places in declaration order (the skim of a class records its fields and their types), an
array's at their indices, a member skipped before a written one value-initialized (an `ImplicitValueInitExpr`: the converter writes
such a list as its text), the trailing ones left out; a nested designator (`.a.y = 2`) a list of the member's own without braces;
a nested list designated by its member's type; a designator of an anonymous union's or struct's member through that member (clang's
implicit `.anon.a`); a function's argument by its parameter's type when the function is not overloaded (§8). A list the parser
cannot place (an index not a literal, a class with bases, a union, an unknown type: an overloaded function's argument) is kept as
written (`init-list-form`).

**3.11 Modules** ([module.unit], [module.import]): `module;`, `export? module NAME[:PARTITION] attrs;`, `module :private;` and
`export? import NAME|:PARTITION|<h>|"h" attrs;` parsed; clang's AST has no node for a module declaration (the converter reads its
line from the text, as before) and an `ImportDecl` for an import. The names and partitions are not macro-expanded (P3034R1, as
clang reads them: `module :PRIVATE_SEMI` with `#define PRIVATE_SEMI private ;` names the partition `PRIVATE_SEMI`, and clang's
recovery then skips to the next `;` of the file, taking the declaration after it); a missing `;` is recovered to the next one or
the end.

**3.12 C++26's constructs** g++.dg/cpp26 exercises and clang 23 reads: pack indexing (`Ts...[I]` as a type, clang's
`PackIndexingType` printed `T...[I]`; `pack...[I]` as an expression, a `PackIndexingExpr`; a base-specifier's), structured bindings
as conditions (P0963), variadic friends and friend-type-specifier lists (`friend Ts::Y..., Us...;`, P2893), a `for`'s condition
declaration, name-independent declarations (`_`).

**3.13 The headers' cache** (`arbace.repr.cpp.skim`): the parser's state after a unit's leading run of header tokens (20,000 or
more: the declarations its first `#include`s bring, skimmed) is kept in `.tmp/repr-cpp/skim-cache/KEY.skim` (`REPR_CPP_SKIM_CACHE`
names another directory, `off` none), KEY the SHA-256 of the run's tokens as the preprocessor gave them (kind, spelling, offset,
length, file, macro expansion, glue, scratch), the standard, the main file's name and the parser's own sources (`parse.ae`,
`scope.ae`, `lit.ae`, `skim.ae`: a change to the parser misses every entry). The state is a graph (the global namespace's scope and
everything reachable from it: scopes, entities, the classes' fields; the member templates' names; the cursor): written as a table of
its objects, each once and referred to by its index, gzipped, and read back with the same sharing (a scope built after its parent
and entity, its containers filled after). io.cpp (`<iostream>`, `<vector>`, `<algorithm>`, 368,465 tokens): the parse 14.4 s cold,
4.5 s warm (the skim's 9 s now the key's 1 s and the read's 0.5 s; the tokens' conversion 1.8 s and the preprocessor's 4.7 s
remain); the light lists' forms are the same cold and warm. `REPR_CPP_TIME=1` prints a read's times on stderr.

## 4. The check

`bin/repr-cpp read --reader native FILE [--main NAME] [--std S] [-- ARGS]` reads with no oracle. `bin/repr-cpp native-check
LIST|samples|native|gxx-a|gxx-b|gxx-26|gxx-mod WORK` (`bin/lib/repr-cpp/native-check`, `arbace.repr.cpp.ncheck`) reads each file twice, from the
oracle's dump and natively, and compares the two units' forms item by item, each written by repr/c's layout on one line; the result
is judged against `test/repr/cpp/native-known.edn`, where each file that differs is listed with its class:

| class | the Sema fact the native reader does not compute |
|---|---|
| `defaulted-noexcept` | a defaulted special member's or comparison's exception specification, which clang evaluates when the member is used (`M m;` uses `M() = default`, which then prints `noexcept`) |
| `init-list-form` | an initializer list's semantic form in clang's tree: an aggregate's elided braces restored, the members a designator skips value-initialized |
| `ctad` | a class template's arguments deduced from an initializer (`A a(b);` declares an `A<int>`) |
| `builtin-signature` | a library function's redeclaration with the attributes of clang's builtin or of the header's declaration (`_Nonnull`), a builtin's call rewritten (`__sync_fetch_and_add` to its sized `_8`) |
| `dependent-member` | a member named in a class template's member function, which clang leaves unresolved (an `UnresolvedMemberExpr`: the converter writes its text) |
| `implicit-argument` | a call's argument clang adds (a default argument of the `operator new` a new-expression calls) |
| `clang-recovery` | an ill-formed construct clang reads by its error recovery |
| `deduced-type` | a function's deduced return type, held in its type (`auto f() { return [=](int b) {...}; }`: a closure type, which the converter writes as the function's text) |
| `native-decline` | a construct the parser declines (its text) and clang reads: the grammar's gap, not a Sema fact (since §8) |

Since §8 the parser computes each class's fact as clang does where that is bounded; a class stays for the cases it leaves to clang.

The lists: `samples` (test/cpp/samples, 9 files), `native` (test/repr/cpp/native/samples: `subset.cpp`, one construct of each kind
of §1's subset), `gxx-a` and `gxx-b` (test/repr/cpp/native/: of the recorded round-trip passes of g++.dg's every-10th sample, every
16th, 62 files, and every 6th of the rest from the 4th, 163; their `{gcc-15.2.0}` an extraction of the pinned tarball), `gxx-26`
(g++.dg/cpp26's files the oracle reads, at c++26, 38) and `gxx-mod` (every 4th of g++.dg/modules' recorded round-trip passes and the
four that differ, 109).

Numbers (2026-11-04, the light tier, 4 jobs): samples 9 of 9 files the same (68 of 68 items); native 1 of 1 (30 of 30); gxx-a 60 of
62 (291 of 293 items), 2 known; gxx-b 156 of 163 (737 of 744), 7 known; no failure. A wider measurement, not a committed list: all 977
recorded round-trip passes of g++.dg's every-10th sample, 942 the same (5,064 of 5,135 items) in 101 s; of the 35 that differ, 32 in
the classes above (defaulted-noexcept 10, init-list-form 8, ctad 5, dependent-member 5, builtin-signature 2, implicit-argument 1,
clang-recovery 1) and 3 not classed (§5); 7 constructs declined in all.

Numbers of the second part (2026-11-04, the light tier, 4 jobs, §7): samples 9 of 9 (68 of 68 items); native 1 of 1 (30 of 30);
gxx-a 60 of 62 (291 of 293), 2 known; gxx-b 156 of 163 (737 of 744), 7 known; gxx-26 37 of 38 (237 of 244), 1 known; gxx-mod 105 of
109 (382 of 389), 4 known; no failure. The wide measurement again: 945 of 977 the same (5,080 of 5,135 items) in 105 s, the 32 that
differ all in the classes (the three unclassed files now the same), 2 constructs declined (7 before). Over g++.dg/modules' 422
recorded round-trip passes: 417 the same, 5 classed.

**The native reader in the round trip and the dg check**: `bin/repr-cpp roundtrip LIST WORK --reader native` reads each file and its
print with the native parser (rtcheck's `REPR_CPP_READER=native`; the options the modelled ones and `-iquote` the original's
directory, as the dumps'), the oracle's dumps still made for its rejections and for level 3; `bin/repr-cpp dg --reader native` reads
the dg-error part's accepted units natively (the oracle still judging acceptance and errors) and runs the modules part's round trip
natively (`--record` and `--record-ref` refused: the records are the clang reader's). The clang reader stays the default. The
samples' round trip natively: 8 pass, 1 known (`gxx-objects-location`), the forms and prints byte for byte the clang reader's; gxx-a
natively, without objects: 62 of 62 pass; dg over cpp26 and five modules files: the clang reader's results.

## 5. What is left

- **Sema's facts** beyond §8's minimum (clang's builtin table, overload resolution, CTAD with packs or user guides), and the instantiation-dependent kinds (`S<int>::x` a type or a value by the specialization a
  program selects; a name declared only in a template's instance): a checker's work (CX12's), or classes.
- **The tree level** (since §9: `native-check --tree`): what is left is §9's classes, the Sema facts the converter does not need and the
  parser does not compute (overload resolution's operator and conversion calls, constructions of header classes, range-for's hidden
  declarations, template arguments of partial specializations and instances); mutants for the error recovery; the whole g++.dg
  compile corpus, libstdc++'s headers as a read corpus (the survey's stress test), the other corpora; CX11's exit: trees equal over
  the corpora or known.
- **The grammar's remainder**: C++26's
  contracts and reflection (clang 23 rejects them), header units (`import <h>;`: clang without `-fmodule-header` rejects them, so the
  parser does not read the header's names), Objective-C++, `__declspec`, nested functions, `asm` operands, `if consteval`'s forms
  (the converter's gap), generic lambdas' forms (the converter's gap).
- **Speed**: the cache covers a unit's leading run of headers only (an `#include` after the first declaration is skimmed each
  time); the tokens' conversion (1.8 s for io.cpp) and the key's digest (1 s) are what a warm read of three headers costs beyond the
  preprocessor.
- **Uses**: the native round trip and dg over whole corpora (recorded results of their own); syntax errors of Arbace's own for the
  dg check (CP8's classes).

## 6. Sources

ISO/IEC 14882:2024 (C++23), Annex A and [stmt.ambig], [dcl.ambig.res], [temp.names], [temp.res], [class.mem]; clang 23.1.4's
behaviour as `tools/cxxdump`'s dumps show it (the host's `Alpine clang version 23.1.4`), its lib/Parse's disambiguation (llvm-project at
`llvmorg-23.1.4`, `test/cpp/corpora.edn`'s pin; not on the host: followed from the functions' documented algorithm and checked on
probes, `.tmp/cx11/probe/` of the worktree); LLVM's `APFloat::toString` (llvm/lib/Support/APFloat.cpp) for the floating literals,
checked on 13 probes against clang's dumps; gcc 15.2.0's g++.dg (the pinned tarball) for the corpus.

## 7. The second part (2026-11-04)

On `cpp-native2`, each step a commit, checked in the light tier (`native-check` samples, native, gxx-a, gxx-b, gxx-26, gxx-mod and
the samples' round trip; no whole corpus, no gate):

1. **The three unclassed files**, fixed in the parser: a qualified declarator-id's rest (its parameters, its `noexcept`) is in its
   class's scope, as clang's DeclScopeObj makes it (noexcept50: `this->fn()`); a qualified constructor `X::X` (and an operator, a
   destructor, a conversion function: not an identifier, Declarator::mayBeFollowedByCXXDirectInit) takes its `(` as a parameter
   list, a qualified declarator-id naming a member function too (Sema::isDeclaratorFunctionLike), and a dependent qualified name is
   an abstract parameter's type (clang's implicit typename, an extension before C++20; typename15); a dependent type's
   construction is a `CXXUnresolvedConstructExpr` (Sema::BuildCXXTypeConstructExpr; sfinae9: the converter writes it in a macro as
   the invocation's text).
2. **The converter's init-capture bug**: `lambda-form` took an init-capture's initializer as the next of clang's per-capture list,
   so `[&n, m = n * 2]` was `(= m n)`; it takes the initializer that begins within the capture's text, and the parser lists the
   init-captures' initializers alone. No known file changed (no recorded round-trip file has an init-capture after a simple
   capture; the dg records do not depend on the forms).
3. **Grammar**: designated initializers (§3.10; the converter writes a list with a skipped member before a written one as its text,
   a default member initializer's too, where it wrote `}`, and one in a nested designator's list, where it wrote `.y = 2`), C++26
   (§3.12; the converter keeps a friend's pack expansion, `(cpp/friend (pack-expand Ts))`, where it wrote `friend Ts;`), modules
   (§3.11) with the modules part's differences: implicit typename in an alias's and a trailing return type (P0634), a friend
   template's class named as written (not the injected `T<H>`), a friend specialization's template arguments
   (`friend void ns::f<int>();`), GNU vector types in an alias or a typedef (`__attribute__((__vector_size__(N))) T`, which the
   converter writes as the declaration's text); the lists gxx-26 and gxx-mod; the class `deduced-type`.
4. **The headers' cache** (§3.13).
5. **The native reader as an option** of `roundtrip` and `dg` (§4); the native host configuration takes `-iquote`.


## 8. Sema's minimum (cpp-sema, 2026-11-04)

The recorded classes of §4 were facts of clang's Sema; for each the question was whether a port on the parser's side is bounded.
The ports are `arbace.repr.cpp.sema` (`arbace/repr/cpp/sema.ae`, in the skim cache's key) with the parser's records, following
clang's functions by name; type information only as far as they need it. A class's facts are kept in a map of its own under the
entity's `:info`, filled as the parser skims the class's body (main file and headers): the fields (an anonymous union or struct a
field of its own), their types and default member initializers, the bases with their access and virtuality, the current access,
the constructors (user-declared, user-provided, explicit; their parameters' types and own template parameters), virtual functions,
the special members by kind with their written exception specifications, a class template's parameters with their defaults; the
id of the class (its class-key's token, shared by the skimmed and the parsed entity). Variables, parameters, value template
parameters, typedefs and aliases record their types; a skimmed array bound keeps its text (`:sn`).

- **`init-list-form`**: SemaInit.cpp's InitListChecker, CheckListElementTypes / CheckSubElementType / CheckImplicitInitList /
  CheckStructUnionTypes / CheckArrayType / FillInEmptyInitializations: an aggregate's ([dcl.init.aggr]/1 by the standard's year:
  user-declared or -provided constructors, virtual functions, non-public fields, bases) or an array's list in its semantic form; an
  element that is an expression initializes the subobject when it can (a scalar, its own class, a string for a char array) and
  otherwise the braces are elided ([dcl.init.aggr]/16): an implicit list from that element for the subobject, an array's as many as
  its bound, a flexible array's the rest; the members a short list leaves out filled as clang's tree has them (a default member
  initializer's `CXXDefaultInitExpr`, a class member's empty list, a scalar's value-initialization not appended), a base of a class
  whose default constructor is not user-provided none. Also the mem-initializers' braced lists by the member's or the base's type,
  a direct-initialization's `T x({...})` and `m({...})` of a class as its copy constructor's `CXXConstructExpr` over the list, a
  designator naming an anonymous member's member, a non-overloaded function's braced argument. A list whose decision needs a type
  not known (dependent, a call's, a header class template's) is left as written: in a template when an element's type is not known.
- **`dependent-member`**: Sema::BuildMemberReferenceExpr's UnresolvedMemberExpr: in a class template (or a partial
  specialization, a member of one defined outside it; templates with unnamed parameters too), a member function named through the
  implicit or an explicit dependent object (`f()`, `this->f()`, `e->f()` with `e` of the current instantiation) or with a
  type-dependent argument (`S2(0).s(*this)`, `f(u)` in a member template), when the name is an overload set or a template (the
  scope marks a function declared twice `:ovl`) and not all static; a value named through a dependent using-declaration (`using
  Base<T>::i;`). Probed against clang: one non-overloaded member function, a static overload set, are resolved.
- **`defaulted-noexcept`**: SemaExceptionSpec's ResolveExceptionSpec when Sema::MarkFunctionReferenced uses the member, and
  ComputeDefaultedSpecialMemberExceptionSpec: the uses the parser records (a variable's default construction, from `{}`, an
  array's elements a list leaves out, `T()`, `T{}` of a class that is not an aggregate; a copy from an lvalue, a move from an xvalue
  (`static_cast<T&&>`, `move(x)`), an implicit move of a local on return; a copy capture; an assignment; `==` and `!=`;
  `__is_nothrow_constructible`; a user-provided constructor's definition default-constructing the members it does not initialize)
  and the uses a defaulted or implicit member's definition makes of its bases' and members' members, a move falling back to a copy
  when the class has no move ([class.copy.ctor]/8); the specification noexcept when every function it invokes is, noexcept(false)
  else, unknown (left unevaluated) when a subobject's class or a constructor is not known. The parsed tree's defaulted members are
  given the evaluated specification in their type after the unit (`patch-defaulted`).
- **`ctad`**: SemaTemplateDeductionGuide's implicit guides and SemaTemplateDeduction's deduction from a call: the template's
  constructors (with their own template parameters), the copy deduction candidate, a braced initializer's initializer-list
  constructors first ([over.match.list]), an alias template's target's guides deducing the alias's parameters
  ([over.match.class.deduct]/3), a member template of a specialization with the enclosing arguments; [temp.deduct.call]'s matching
  (references and top-level cv dropped, arrays decayed, a template-id against the argument's type or its bases'), defaults for the
  parameters not deduced; the type printed as clang prints it (a class's trailing default arguments omitted, an alias's in full).
  Kept only when certain: one viable guide (or all deducing the same), every argument's type known, no user-declared guide, no pack.
  A deduction from a closure (`Merged(l1, l2)` with lambdas, through its user guide) is a type with a closure in it: the converter's
  text, as clang's.
- **`deduced-type`**: a function's plain `auto` return deduced from a returned lambda-expression (outside templates): a closure type.
- **`implicit-argument`**: a new-expression calling its class's `operator new` with a default argument: the argument clang adds.
- **`builtin-signature`**: the `__sync` builtins overloaded on the pointee (Sema's BuiltinAtomicOverloaded: `__sync_fetch_and_add`
  on an 8-byte pointee calls `__sync_fetch_and_add_8`; the size by LP64's table, a typedef's GNU `mode`). **Stays a class** for a
  library function's redeclaration with the attributes of clang's builtin (`fprintf`'s parameters `_Nonnull`): that is clang's
  table of library builtins (Builtins.td, with each one's header, signature and attributes), not on the host as source; a table of
  hundreds of entries derived by probing clang would be data of clang's, not a port.
- **`clang-recovery`**: `inline template <...>` read as clang's recovery reads it, the stray specifier dropped.

Two parser gaps found on the way were fixed too: the injected-class-name as the argument of a type template parameter is the class
([temp.local]/1: `__constrain_pkgdtask<packaged_task, _Fn>` in `packaged_task<_Res(_ArgTypes...)>`), and `...` after a parameter
whose type names a parameter pack is a pack expansion, not C's ellipsis (`F(typename decay<B>::type...)`).

The check, light (`native-check`, 4 jobs; every class's files before and after; "files" are the known files of the lists and the
977-file sample together, 37 before):

| class | before | after |
|---|---|---|
| `defaulted-noexcept` | 11 | 0 |
| `init-list-form` | 10 | 0 |
| `ctad` | 5 | 0 |
| `dependent-member` | 5 | 0 (dump1.C's three differences: its unresolved call fixed; its other two parser gaps fixed; one decline left, `native-decline`) |
| `builtin-signature` | 2 | 1 (pr45679-1_1.C: `_Nonnull`) |
| `deduced-type` | 2 | 0 |
| `implicit-argument` | 1 | 0 |
| `clang-recovery` | 1 | 0 |
| `native-decline` | - | 1 (dump1.C) |

Numbers (2026-11-04): samples 9 of 9 (68 of 68 items); native 1 of 1 (30 of 30); gxx-a 62 of 62 (293 of 293); gxx-b 162 of 163
(743 of 744), 1 known; gxx-26 38 of 38 (244 of 244); gxx-mod 109 of 109 (389 of 389); the 977-file sample 975 the same (5,133 of
5,135 items) in 122 s, 2 known, 1 construct declined; no failure. `REPR_CPP_SEMA_DEBUG=1` says why a CTAD was not decided.


## 9. The tree level (cpp-tree, 2026-11-04)

The check of §4 compares the forms the converter makes from the native tree with those from clang's dump: a native tree that differs
where the converter does not look is not seen. `bin/repr-cpp native-check LIST|samples|native|gxx-a|gxx-b|gxx-26|gxx-mod WORK --tree`
(`arbace.repr.cpp.tcheck`, `arbace/repr/cpp/tcheck.ae`; the same dumps and lists as §4, JN8's parse-check in spirit) compares the trees
themselves, node for node, and says which differences the converter reads.

**The comparison.** The nodes of the two trees (clang's dump of the main file's declarations, the parser's `:nodes`) are aligned list
by list: the longest common subsequence over kind, name and first offset (the common prefix and suffix first; a greedy alignment looking
40 nodes ahead beyond 1.5 million cells), then within each gap the nodes paired through clang's wrappers the native tree omits
(`ImplicitCastExpr`, `ConstantExpr`, `ExprWithCleanups`, `MaterializeTemporaryExpr`, `CXXBindTemporaryExpr`) over a core of the same
offsets, then by kind and name, then by the same first and last offset under another kind (the parser's `CallExpr` for clang's
`CXXMemberCallExpr`). A pair's fields are compared recursively (clang's `:id`, `:line` and `:col` and the parser's `_` keys left out;
types as clang prints them, positions as offset, token length and file, macro locations by their spelling and expansion). The
differences: `missing` (a node clang has and the native tree lacks, with the size of its subtree), `wrapper` (a missing node whose only
child is paired), `extra`, `value` (a field that differs), `absent-n` and `absent-c` (a field the native tree or clang's lacks). A file's
first difference is reported by its path (`FunctionDecl main@410 > CompoundStmt@421 > CXXForRangeStmt@524 > ...`, kind, name, offset).

**Read or ignored.** A difference is *read* when the converter's forms change with it, *ignored* otherwise, tried on the converter itself:
the difference applied to the tree the forms come from (a field of the native tree put into clang's, a missing node taken out of it, a
node only the native tree has taken out of that) and the forms compared with the unchanged tree's (an error or a refusal counts as a
change). A unit of differences (a node kind missing, a field of a kind absent) is tried first on every node of its kind, all units of a
type together and then each; if the forms stay, the whole unit is ignored (the converter does not look at that kind or that field), else its
differences are tried by halves, each applied alone (a wrapper's removal changes what the converter finds around it, which is why the
unit is tried on the whole kind first: all `ImplicitCastExpr` out, as the native tree has it, leaves the forms; one of them out does not).
At most 600 conversions and 120 s a file; the rest are `UNKNOWN` (none in the lists). A file whose dump exceeds 4 MB is `skipped` (memory
of the light tier: `warn/Wsequence-point-4.C`, 15 MB). A file is `same` when no difference is read, `differ` otherwise; the forms level's own
result is beside it (`forms differ in N`), and `tree reads differ with the forms the same in N` is what the forms level hides.

**Outputs.** `WORK/tree-results.tsv` (name, tree result, forms result, the nodes of each tree, the pairs, those with a differing field, missing, extra,
read, ignored, seconds, the first read difference), `WORK/tree/NAME.txt` (the read differences with their paths and classes, then the ignored
ones by unit with a count and a first path), `WORK/tree-units.tsv` (every unit, read or ignored, its differences and files), the summary on
stdout with each class's differences and files. **The classes** are `test/repr/cpp/native-tree-classes.edn` (a regex over a difference's
detail line gives its class, the first that matches) and the known file `test/repr/cpp/native-tree-known.edn` (a file that differs, with the
classes of its read differences: it fails when a difference is of a class not listed for it, or of none, and is said when it turns `same`).

**What it found, fixed in the parser** (the forms equal throughout: the lists, the 977-file sample at the forms level 976 the same and
1 known, as before, `builtin-signature`'s pr45679-1_1.C):

- a non-static member named unqualified in a function's body (a default member initializer's too) is clang's `MemberExpr` through an
  implicit `this` (`CXXThisExpr`, `:implicit`), not a `DeclRefExpr` (121 `CXXThisExpr` missing in the lists before, 2 after; the sample's
  member-access class 46 to 29: members of overloaded or dependent members in odd places); marked on the `DeclRefExpr` while parsing, so the parser's own uses of it are
  unchanged, and rewritten after the unit (`this-members`);
- a member of a type-dependent object that no class of the current instantiation has is a `CXXDependentScopeMemberExpr` (`:member`),
  not a `MemberExpr` (36 of the kind differences, 18 left: conversion-function names `b.operator typename A<T>::type()` and a
  lambda's `template operator()`);
- a `DecompositionDecl`'s initializer is its first child, before the bindings;
- a cast to a reference type has the referent's type and the value category lvalue or xvalue (`static_cast<T&&>(x)` is a `T` xvalue),
  not the reference with prvalue; `char...` without the comma: the parameter ends at `char`;
- a constructor's mem-initializer naming its own class (not in a template, no arguments) is `delegatingInit`, not `baseInit`;
- **dump1.C's declined typedef** (§5's last grammar gap): `struct __future_base::_Task_state_base<_Res(_Args...)> { typedef
  __future_base::_Ptr<_Result<_Res>> _Ptr_type; }`. The tree check showed `NativeDecline` where clang has a `TypedefDecl`, and a reduction
  to ten lines (`.tmp/x/t1.C` of the worktree: a member class template `Res` of `F`, an alias template `Ptr`, `struct F::TSB<R(A...)>`
  defined outside `F`) showed the cause: a class defined outside its enclosing class took only the namespace as its parent scope, so
  `Res`, a member template of `F`, was not a template in its body ([basic.lookup.unqual]/7: the names of the enclosing class follow the
  class's own). The class's scope now has a lookup scope of the qualifier's class (`F`) as its parent. dump1.C is read in full: the forms
  equal, no decline (its `native-decline` entry left `test/repr/cpp/native-known.edn`; the 977-file sample has no declined construct now).

**The classes** (what the parser does not compute and the converter does not need: each is a Sema fact, the same kind as §4's). The six
light lists together (382 files, 295 `same`, 86 `differ`, 1 `skipped`; 410 differences read, 37,000 ignored) and the 977-file sample
(744 `same`, 232 `differ`, 1 `skipped`; 770 read, 132,000 ignored; 231 of the 232 differ at the tree level with the forms the same):

| class | the fact | the lists | the sample |
|---|---|---|---|
| `implicit-construction` | a `CXXConstructExpr`, `CXXTemporaryObjectExpr` or default argument clang adds (copy and move constructions, a class's temporary, `ParenListExpr` for a mem-initializer), a cast clang reads as a construction | 41 / 15 files | 175 / 68 |
| `operator-call` | `CXXOperatorCallExpr` for an overloaded operator on class operands (overload resolution; the native tree has the operator's own node) | 39 / 23 | 130 / 81 |
| `template-instances` | the `TemplateArgument` nodes of a partial specialization (`type-parameter-0-0`'s canonical spelling) and a function template's instances | 51 / 27 | 91 / 43 |
| `conversion-function` | an implicit call of a conversion function (`while (S{})` through `operator bool`: `CXXMemberCallExpr` over a `MemberExpr` of the conversion) | 95 / 16 | 73 / 27 |
| `init-list-form` | an initializer list's semantic form beyond §8's (`std::vector<int> v = {1, 2}`'s `CXXStdInitializerListExpr`, an array's `array_filler`, an elided nested aggregate) | 88 / 17 | 64 / 31 |
| `dependent-member` | the unresolved forms of dependent names beyond the member access (a conversion-function name, a template `operator()`) | 30 / 5 | 59 / 12 |
| `range-for` | the hidden declarations and expressions of a range-based `for` (`__range1`, `__begin1`, `__end1`, the condition and the increment) | 39 / 7 | 55 / 11 |
| `member-access` | implicit-this accesses of overloaded or dependent members, a destructor's pseudo-destructor call | 8 / 6 | 29 / 18 |
| `abbreviated-template` | the invented template parameters of `void f(auto p)` (`FunctionTemplateDecl`, `TemplateTypeParmDecl`) | 16 / 8 | 13 / 5 |
| `unresolved-lookup`, `attributes`, `gnu-vector`, `friend-template`, `coroutine`, `builtin-signature`, `pragma-range` | `UnresolvedLookupExpr`'s name; clang's attribute nodes (`UnusedAttr`...) for the parser's `NativeAttr`; `__vector_size__` in a type; a friend member template; `CoroutineBodyStmt`; §8's `_Nonnull`; a `#pragma omp` before a declaration | 2 files (coroutine, builtin-signature) | 29, 21, 6, 3, 1 (unclassed: 21 in 12 files) |

**The ignored differences** (132,248 in the sample; the same shapes in the lists), what the native tree deliberately does not carry because
the converter does not read it: clang's expression types and value categories (over a third of them together: every `DeclRefExpr`, `MemberExpr`,
`BinaryOperator`, call and literal's `:type` and `:valueCategory`), `isUsed`, `isReferenced`, `mangledName`, `isHidden`,
`previousDecl`, `parentDeclContextId`, declaration links (`referencedDecl`'s kind and type, `referencedMemberDecl`,
`foundReferencedDecl`), `definitionData`, `depth` and `index` of template parameters, `castKind`, `nonOdrUseReason`, `canOverflow`;
the implicit nodes (`ImplicitCastExpr`, `ConstantExpr`, `ExprWithCleanups`, `MaterializeTemporaryExpr`), the implicit members and
injected class names of every class, the instantiations under every template, the type nodes (`BuiltinType`, `TemplateTypeParmType`,
`TypedefType`) under declarations; the macro locations' spelling (the parser's `<macro>` file where clang has the definition's
position, and `\n` as `\012` in a string from a macro: the converter reads the expansion).

**The numbers** (2026-11-04, the light tier, 4 jobs; per list: files `same` / `differ`, nodes of clang's tree and the native tree, pairs
(with all fields equal), missing, extra, differences read / ignored, files whose tree reads differ with the forms the same):

| list | files | clang / native nodes | pairs (equal) | missing / extra | read / ignored | forms hide |
|---|---|---|---|---|---|---|
| samples | 9: 4 / 5 | 1,158 / 622 | 611 (225) | 430 / 11 | 14 / 1,197 | 5 |
| native | 1: 0 / 1 | 929 / 547 | 524 (162) | 303 / 23 | 14 / 1,184 | 1 |
| gxx-a | 62: 45 / 17 | 3,323 / 2,013 | 1,974 (722) | 1,047 / 39 | 64 / 3,807 | 17 |
| gxx-26 | 38: 18 / 20 | 8,909 / 6,037 | 5,838 (2,135) | 1,927 / 199 | 165 / 11,132 | 20 |
| gxx-b | 163: 127 / 35, 1 skipped | 10,975 / 7,059 | 6,988 (2,157) | 2,660 / 71 | 137 / 16,236 | 34 |
| gxx-mod | 109: 101 / 8 | 2,161 / 1,555 | 1,551 (330) | 467 / 4 | 16 / 3,488 | 8 |
| the 977-file sample | 977: 744 / 232, 1 skipped | 75,930 / 46,676 | 46,220 (12,259) | 20,737 / 456 | 770 / 132,248 | 231 |

Every list passes the judgment (the files' classes are in the known file). Forms level, unchanged by the fixes: samples 9 of 9, native 1 of
1, gxx-a 62 of 62, gxx-26 38 of 38, gxx-mod 109 of 109, gxx-b 162 of 163 (1 known), the 977-file sample 976 of 977 (1 known), 0 declines.
