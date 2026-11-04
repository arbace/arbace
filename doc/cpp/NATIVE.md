# repr/cpp's native parser (CX11)

Status: CX11's first part, on the branch `cpp-native` from `cpp-cx9` (2026-11-04). The survey's §4.4 and its CX11 row
([CPP-SURVEY.md](CPP-SURVEY.md) §11) planned a C++ parser of Arbace's own, "name-kind tracking, a port of the grammar", so that
repr/cpp reads C++23 without the oracle's AST dump at read time; clang stays the oracle. This note is its design: what C++'s
parsing needs beyond C's (§1), the reference model and why (§2), the design as built (§3), the check (§4), what is left (§5).

Words: **the parser** is `arbace.repr.cpp.parse`; **the converter** `arbace.repr.cpp.convert`, unchanged (CX4-CX9's); **the
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

**3.8 What clang's tree has that the parser writes too**: a lambda's capture initializers, one an explicit capture (the converter
reads an init-capture's initializer as the next of them: with a simple capture before it, the wrong one; a converter bug the check
surfaced, kept so that the forms agree); a static assertion without a message has an empty second child; an anonymous union in a
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

## 4. The check

`bin/repr-cpp read --reader native FILE [--main NAME] [--std S] [-- ARGS]` reads with no oracle. `bin/repr-cpp native-check
LIST|samples|native|gxx-a|gxx-b WORK` (`bin/lib/repr-cpp/native-check`, `arbace.repr.cpp.ncheck`) reads each file twice, from the
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

The lists: `samples` (test/cpp/samples, 9 files), `native` (test/repr/cpp/native/samples: `subset.cpp`, one construct of each kind
of §1's subset), `gxx-a` and `gxx-b` (test/repr/cpp/native/: of the recorded round-trip passes of g++.dg's every-10th sample, every
16th, 62 files, and every 6th of the rest from the 4th, 163; their `{gcc-15.2.0}` an extraction of the pinned tarball).

Numbers (2026-11-04, the light tier, 4 jobs): samples 9 of 9 files the same (68 of 68 items); native 1 of 1 (30 of 30); gxx-a 60 of
62 (291 of 293 items), 2 known; gxx-b 156 of 163 (737 of 744), 7 known; no failure. A wider measurement, not a committed list: all 977
recorded round-trip passes of g++.dg's every-10th sample, 942 the same (5,064 of 5,135 items) in 101 s; of the 35 that differ, 32 in
the classes above (defaulted-noexcept 10, init-list-form 8, ctad 5, dependent-member 5, builtin-signature 2, implicit-argument 1,
clang-recovery 1) and 3 not classed (§5); 7 constructs declined in all.

## 5. What is left

- **The wide measurement's three unclassed files**: `cpp0x/noexcept50.C` (a `noexcept`'s member found through the complete scope
  is printed without clang's `this->` where the member is declared later: the late scope is not consulted from a declarator's
  qualifiers yet), `cpp2a/typename15.C` (`B(T::type);` in a class template, P0634's constructor: the check finds no `T` among the
  template parameters there), `template/sfinae9.C` (declarations a macro writes, whose texts clang's converter keeps).

- **Sema's facts** of §4's classes, and the instantiation-dependent kinds (`S<int>::x` a type or a value by the specialization a
  program selects; a name declared only in a template's instance): a checker's work (CX12's), or classes.
- **The tree level**: the native tree against the oracle's node for node (the fields the converter reads), as JN8's parse-check;
  mutants for the error recovery; the whole g++.dg compile corpus, libstdc++'s headers as a read corpus (the survey's stress test),
  the other corpora; CX11's exit: trees equal over the corpora or known.
- **The grammar's remainder**: designated initializers out of order (clang's semantic form needs the fields' order), C++26 (pack
  indexing, contracts, reflection), module declarations (read by the converter from the text; the parser skips their lines),
  Objective-C++, `__declspec`, nested functions, `asm` operands, `if consteval`'s forms (the converter's gap), generic lambdas' forms
  (the converter's gap).
- **Speed**: the headers' skim (9 s for three headers) cached by their digests as the preprocessor caches its groups.
- **Uses**: `bin/repr-cpp roundtrip` and `dg` with the native reader; syntax errors of Arbace's own for the dg check (CP8's classes).

## 6. Sources

ISO/IEC 14882:2024 (C++23), Annex A and [stmt.ambig], [dcl.ambig.res], [temp.names], [temp.res], [class.mem]; clang 23.1.4's
behaviour as `tools/cxxdump`'s dumps show it (the host's `Alpine clang version 23.1.4`), its lib/Parse's disambiguation (llvm-project at
`llvmorg-23.1.4`, `test/cpp/corpora.edn`'s pin; not on the host: followed from the functions' documented algorithm and checked on
probes, `.tmp/cx11/probe/` of the worktree); LLVM's `APFloat::toString` (llvm/lib/Support/APFloat.cpp) for the floating literals,
checked on 13 probes against clang's dumps; gcc 15.2.0's g++.dg (the pinned tarball) for the corpus.
