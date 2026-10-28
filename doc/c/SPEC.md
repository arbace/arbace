# C forms: C programs written in Clojure

Status: proposed (2026-10-28), milestone C1 of C23 at parity with the three crops
([C23-SURVEY.md](../C23-SURVEY.md) §11 and "C1: the plan after CS4 (b)"), for the user's acceptance,
which is C1's exit criterion. The user's decisions of 2026-10-28 bind it (the survey's "Decided
(2026-10-28): CS1-CS24"): CS3 (a) the language's scope, CS5 (a) the preprocessor inside read, CS6 (a)
Go forms' vocabulary, CS7 (a) literals as values, CS8 (a) the converter writes what reading needs,
CS9 (a) one flat address space, CS10 (a) the machine's behaviour at gcc `-O0`, and CS4 (b): the front
end is a port of cc into Arbace, cc kept as the oracle. Nothing implements it yet: the reader is C2-C3b,
the checker C4, ci C5, AOT C6. Where this text departs from the survey's sketch of §4 (spellings the
Clojure reader refuses, ambiguities found while writing it), §17 lists the change and why; the questions
it raises for the user are the survey's CS30-CS43.

This spec defines the **C forms**: how every construct of a C translation unit, as C23 (ISO/IEC
9899:2024; subclause numbers are WG14's N3220's) and the earlier standards and GNU extensions CS3 admits
define it, is written as Clojure data in ordinary `.ae` sources. It says what the forms mean, what the
converter writes and what a consumer derives, how the preprocessor's work is kept, which behaviour of
the memory and of undefined operations a program can observe, and what is read and printed but by
design not evaluated or compiled. It is the counterpart of the Go forms ([go/SPEC.md](../go/SPEC.md))
and the Java forms ([classes/SPEC.md](../classes/SPEC.md)), and follows the Go forms' structure and
principles.

Words used throughout:

- **the reader**: repr/c's `read` (`arbace.repr.c`, `bin/repr-c read`): the port of cc's lexer,
  preprocessor and parser (`arbace.repr.c.lex`, `.pp`, `.parse`; CS4 (b)) and **the converter**
  (`arbace.repr.c.convert`), which turns the parse into C forms. Every decision about forms is the
  converter's.
- **the printer**: `arbace.repr.c.print`, C forms → C text, in mode `:lines` (one canonical spelling,
  cemit's) or `:full` (the original text, byte for byte).
- **a consumer**: anything that reads C forms for their meaning: the checker `arbace.c.check`, ci's
  builder, the AOT translator (cf2g, CS17), and the printer.
- **cc**: go-whim's `crefactor/cc` (modernc.org/cc/v4 v4.29.7 with go-whim's C23 delta), the oracle of
  the port's trees (the survey's "C1: the plan after CS4 (b)"); never part of Arbace.
- **the reference**: the host's gcc 15.2.0 (`x86_64-alpine-linux-musl`, musl 1.2.6), at each file's
  standard, `-O0` the judge and `-O2` the witness (CS14 (a)); clang 23.1.4 a second opinion on acceptance.
- **the target**: linux/amd64, the SysV x86-64 psABI, LP64. It is the only one: the forms of a unit are
  for it (§4.2), as Go forms are for one configuration.
- **C context**: the inside of a C declaration form (§7.1), where the forms mean C.
- **the layer**: what the preprocessor consumed and the forms keep beside the program: the main file's
  directives, the macro invocations, and in `:full` the text itself (§9, §10).

Contents: 1 Principles · 2 A first example · 3 Equivalence: the round trip · 4 Units, files, namespaces,
names · 5 Types · 6 Declarations · 7 Code · 8 Constants and literals · 9 The preprocessing layer ·
10 Positions, comments and mode `:full` · 11 What the converter writes, what is derived · 12 The memory
model · 13 Undefined, unspecified and implementation-defined behaviour · 14 Read and printed, out of eval
and AOT · 15 Coverage · 16 Worked examples · 17 Changes to the survey's sketch, open questions · 18 Sources.

## 1. Principles

1. **Arbace keeps its meaning outside C context; inside, the forms mean C** (CS6 (a), as Go forms'
   principle 1). Inside a C declaration form `+`, `<<`, `==` are C's operators at the operands' types
   after C's conversions (6.3), `if`, `while`, `for`, `set!` are C's statements and expressions, `@p` is
   C's indirection. No Arbace code runs inside C context: no vars, no seqs, no Arbace functions.
2. **Declarations mirror C.** One form per external declaration, in source order; one declarator a form
   unless the declarators share a type the specifiers define (§6.7); one file of forms per translation
   unit. C's identifiers are written verbatim (case sensitive, no munging); the few that the reader
   would read as something else are `(c/id "...")` (§4.4).
3. **The forms are the program after preprocessing; the preprocessing is kept beside it** (CS5 (a)):
   the main file's directives as forms where they stand between items, every macro invocation that
   expanded to one whole node recorded on that node, the rest in mode `:full`'s text layer (§9). Eval and
   AOT never preprocess.
4. **Go forms' vocabulary wherever C means the same** (CS6 (a)): `(* T)`, `(array N T)`, `@p`,
   `(addr x)`, `(set! x e)`, `(set! x + e)`, `(aget a i)`, `(.-f s)`, `bit-and`, `bit-or`, `bit-xor`,
   `bit-not`, `and`, `or`, `not`, `(conv T e)`, `(goto :L)`, `(break)`, `(return e)`, `let` for a
   block's declarations, `byte-string`; C's own heads where Go has none (`?`, `comma`, `pre-inc!`,
   `sizeof`, `->`, `compound-lit`, `init`, `do-while`, `generic`).
5. **Plain Clojure syntax.** Everything reads with Arbace's reader (`arbace.core/read`, Clojure 1.12
   syntax) without data readers: metadata, qualified symbols, keywords, characters, BigInts, BigDecimals,
   ratios. Not EDN (CLAUDE.md, "Working conventions"); where in doubt the Clojure reader wins, and every
   example of this spec was read by it (§16). C's tokens the reader cannot read as C means them (`^`,
   `~`, `|`, `?:`, `'a'`, `1'000`, `0x1p-2`, `.5`) have other spellings (§7.5, §8).
6. **Types are tags and type forms, read from the name outward** (C-lisp's order, never C's inside-out
   declarator): `int (*(*x)[10])(int)` is `^{:tag (* (array 10 (* (fn [int] int))))} x`.
7. **Values, not spellings, in `:lines`; every byte in `:full`** (CS7 (a)). A literal is its value and,
   when not the default, its type; the radix, suffix case, digit separators, escapes, specifier order,
   `_Bool` against `bool`, white space and comments are `:full`'s (§10). The source's parentheses are
   kept in both modes (§7.5).
8. **One target.** The forms of a unit are for linux/amd64 with the reference's predefined macros and
   headers (§4.2); a `#if` on the target is decided at read, as Go forms decide build constraints.
9. **The converter writes what reading needs, never what checking derives** (CS8 (a)): structure (which
   the C grammar decides by name lookup: a typedef name is structure here), the layer, positions,
   literal types. Every expression's type, every conversion, every constant's value, layout and linkage
   are the checker's (§11).

## 2. A first example

`letters.c` (built and run by gcc 15.2 `-std=c23 -pedantic-errors -Wall`: prints `10 3`):

```c
#include <stdio.h>

static int count[26];

/* Counts the letters of s. */
static int tally(const char *s) {
    int n = 0;
    for (; *s; s++) {
        if (*s >= 'a' && *s <= 'z') {
            count[*s - 'a']++;
            n += 1;
        }
    }
    return n;
}

int main(void) {
    int n = tally("hello, world");
    printf("%d %d\n", n, count['l' - 'a']);
    return 0;
}
```

As C forms (`:lines`, each form on its C line):

```clojure
(in-ns 'c.letters) (c/file "letters.c" :std :c23)
(c/include "<stdio.h>" :unit c.h.6f1d0c2a9e43b871)

(c/var ^:static ^{:tag (array 26 int)} count)

;; /* Counts the letters of s. */
(c/defn ^:static tally ^int [^{:tag (* (const char))} s]
  (let [^int n 0]
    (for [] @s (inc! s)
      (when (and (>= @s \a) (<= @s \z))
        (inc! (aget count (- @s \a)))
        (set! n + 1)))
    (return n)))

(c/defn main ^int [void]
  (let [^int n (tally "hello, world")]
    (printf "%d %d\n" n (aget count (- \l \a)))
    (return 0)))
```

- A translation unit is a namespace (`c.letters`); `c/file` holds what is the file's (its name, its
  standard; in `:full` its text's layer). The alias `c` is `arbace.c`, as `go` is `arbace.go`.
- `#include <stdio.h>` is a form where it stands; its declarations are the **header unit** it names,
  `c.h.6f1d...` (the digest is illustrative), read once and shared by every unit that includes the
  header in the same context (§4.3, CS30).
- `c/var`, `c/defn` are external declarations; `static` is metadata on the name. Types are tags; the
  return type is the parameter vector's tag, as in `defn`; `(void)` is `[void]`.
- A block's declarations are a `let` whose body is the rest of the block (§7.2).
- `*s` is `@s`, `s++` is `(inc! s)` (postfix: its value is the old one), `count[i]++` is
  `(inc! (aget count i))`, `n += 1` is `(set! n + 1)`.
- `'a'` is `\a`: an integer constant of type `int` whose value is the character's code (§8.2).
- `return 0` stays `(return 0)`: C has no implicit result, and a function body's last form is a
  statement, never a value.
- The comment is a `;;` comment of the forms text, lost on reading (`:full` keeps it as data, §10.4).

## 3. Equivalence: the round trip

The round trip is *C → reader → forms → printer → C*. It checks the reader and the printer, and later
anything that writes C forms (hand-written forms, cf2g's inverse is not one).

### 3.1 Levels

For a unit read at its standard and configuration, the original and the printed text are
**equivalent** when (CS16 (a), the survey's §5.4):

1. **Forms.** The printed text read again gives the same forms (C → forms → C → forms), metadata
   included but `:line` (in `:lines`, lines can move where the printer breaks differently).
2. **Text.** In mode `:full`, the printed text is the original, byte for byte (no compiler needed).
3. **Code.** The `:lines` print compiled by the reference (`gcc -std=STD -O0 -fno-stack-protector -c`,
   the same base name in two directories) gives the original's object file byte for byte; `-g` is not
   given, since lines move (c23conf's judge 5). A unit the reference rejects is compared on the error's
   line and kind instead (§3.3).
4. **Behaviour.** A program built from the printed units runs as the original (status and stdout, E2's
   measure), which level 3 implies for the units it covers.

Level 3 is the read level's judge (the survey's §1, CS16 (a)); levels 1 and 2 localize a difference.
c23conf's other judge, the preprocessing tokens compared one for one, applies to the `:full` print (it
is level 2 seen through the lexer); on `:lines` it would refuse what CS7 (a) normalizes on purpose.

### 3.2 Normalizations

What `:lines` does not keep, and level 3 does not see:

- **N1 spelling of literals**: radix, suffix case and order (`10UL`, `10lu`), digit separators, escapes,
  string concatenation (`"a" "b"` is `"ab"`), the prefix of a character constant whose value and type a
  plain spelling gives (`L'a'` is `'a'`: both `int` 97), `u8` strings before C23 (§8).
- **N2 spelling of specifiers**: their order and words (`long unsigned int` is `unsigned-long`;
  `_Bool`/`bool`, `_Thread_local`/`thread_local`, `_Alignas`/`alignas`, `__typeof__`/`typeof`,
  `__inline__`/`inline`, `__const`/`const`, `[[__nodiscard__]]`/`[[nodiscard]]`).
- **N3 grouping of declarators** that share no defined type (`int a, *b;` prints as two declarations,
  cemit's rule; §6.7).
- **N4 null statements and braces**: a body is always braced in the print (cemit's rule), a `;` that is
  not a statement's own is dropped, `if (c) s;` and `if (c) { s; }` read the same (§7.3).
- **N5 comments and white space** (kept as `;` comments in the forms text, lost on reading).
- **N6 macro invocations that are not whole nodes** and inactive conditional groups: the print gives
  the active configuration's text (§9.4).

What `:lines` keeps although gcc's object code does not depend on it: the source's parentheses
(`^:c/paren`, §7.5), each macro invocation that is a whole node (§9.4), the directives that stand between
items (§9.2), attributes (§5.6). They make the `:lines` print cemit's canonical text up to N1 and N2,
which is what CS23 (a) compares with cemit (on the files cemit prints).

### 3.3 What equivalence does not cover

- **Diagnostics**: a unit the reference rejects is read as an error (§9.5, the survey's §5.5); the error
  tests (`dg-error`) are judged by line and kind, not by the forms.
- **Warnings**: `-Wparentheses` and the like may differ where N1-N4 normalize.
- **Debug information** (`-g`): lines and columns are `:full`'s; `:lines` keeps lines only (§10).
- **Other targets**: a `#if __aarch64__` group is inactive and kept as text in `:full` only.

## 4. Units, files, namespaces, names

### 4.1 Namespaces and layout

- A **translation unit** (5.1.1.1) is a namespace `c.NAME` and one forms file. NAME is the C file's
  path relative to the root its corpus or program gives (`test/c/corpora.edn`'s `:path`, a program's
  `src/`), `/` as `.`, the extension dropped, and a `.` inside a file's base name as `_`:
  `gcc.c-torture/execute/20000112-1.c` under ccorpus2's gcc root is `c.execute.20000112-1`,
  `pr12.3.c` is `c.pr12_3`. Two paths that give one name keep the first; the second gets `_2`, and
  `c/file`'s `:name` records the file. The forms file is the C file's path with `.ae` added
  (`letters.c.ae`), so `x.c` and `x.h` never collide.
- A **header unit** (§4.3) is a namespace `c.h.DIGEST` and one forms file `c/h/DIGEST.ae` beside the
  units that include it in a read's output.
- The reader writes the namespace and `c/file` as the file's first forms, on its first line (§10.1).

### 4.2 `c/file`

```clojure
(c/file "letters.c" :std :c23)
(c/file "20000112-1.c" :std :gnu17 :args ["-fpermissive"] :name "execute/20000112-1.c")
```

| key | value |
|---|---|
| the first argument | the file's base name, as written to disk by the printer |
| `:std` | `:c89` `:c99` `:c11` `:c17` `:c23`, or GNU's `:gnu89` ... `:gnu23` (CS3 (a)): the standard the file is read and checked at, from its corpus' action (§15.3) |
| `:args` | the reference's options that change the read or the program (`-D`, `-U`, `-I`, `-include`, `-fpermissive`, `-funsigned-char`, `-fwrapv`...), verbatim, in order; absent when none |
| `:name` | the path the namespace came from, when it is not NAME's plain image (§4.1) |
| `:host` | the digest of the host data the read used (§9.1: the reference's predefined macros, include paths and `__has_*` answers), so a read is reproducible from its record |
| `:positions` | `:full` when the unit was read in mode `:full` (§10.2); absent for `:lines` |
| `:c/...` | `:full`'s file-level layer (§10.2) |

`:std` changes meaning, not only acceptance: `f()` declares a function without a prototype before C23
and `f(void)` in C23 (6.7.7.4); `bool`, `true`, `false`, `nullptr`, `typeof`, `constexpr`, `static_assert`,
`alignas`, `thread_local` are keywords in C23 and identifiers or macros before (§4.4); implicit `int` and
K&R definitions exist before C23 only (§6.4). The target is not a key: there is one (§1, 8).

### 4.3 Header units (CS30 (a))

A header's text is preprocessed in the context of its inclusion (the macros defined before it), so the
declarations it contributes depend on that context (`_GNU_SOURCE`, `NDEBUG`). The reader writes, per
`#include` that is active and not suppressed by an include guard or `#pragma once`:

```clojure
(c/include "<stdio.h>" :unit c.h.6f1d0c2a9e43b871)
```

and, once per distinct content, the header unit's file:

```clojure
(in-ns 'c.h.6f1d0c2a9e43b871)
(c/header "<stdio.h>" :path "/usr/include/stdio.h" :std :c23)
(c/include "<features.h>" :unit c.h.0b4e...)
(c/typedef FILE (struct _IO_FILE))
(c/declare printf ^int [(restrict (* (const char))) &])
...
```

- **DIGEST** is the first 16 hex digits of the SHA-256 of the unit's forms text after the `c/header`
  form: two inclusions that give the same declarations share one unit, two that give different ones
  (another context) get two. No context is recorded: the content is the key.
- A header unit holds what the header contributed: declarations (and definitions: a `static inline`
  function of a header is defined in each unit that includes it, as in C), its own nested `c/include`
  forms, its `c/define` and other directive forms (the layer, §9). Its forms are laid out on the header's
  own lines (§10.1); `:full` keeps its text like a main file's.
- **Evaluating** `(c/include ... :unit U)` enters U's declarations into the including unit's scopes at
  that point, as the text's inclusion does (once per unit; a second inclusion the guard suppressed has no
  form). The printer prints the directive, never the unit.
- An include whose header is not found is a read error (the reference's message, §9.5); `__has_include`
  is answered from the host data (§9.1). A declaration that begins in one file and ends in another is
  refused with its position (none in the measured corpora; such a file is classed `split-declaration`).
- The header units of musl (CS11) are those its own sources and the corpora's units produce; ci's
  `(c/include "<stdio.h>")` at the REPL reads the header in the REPL unit's current context.

### 4.4 Names

- **Identifiers** (6.4.2) are symbols, verbatim: ASCII letters, digits, `_`, `$` (a GNU extension the
  reader accepts), UCNs as the characters they name (`été` and `été` are the same identifier
  `été`; `:full` keeps the spelling). Every C identifier is a valid Clojure symbol.
- **Identifiers the reader reads otherwise** are `(c/id "...")`: `nil`, `true`, `false` (identifiers
  before C23: `enum { false, true };` is common in old C). `(c/id "true")` means the identifier, `true`
  the C23 constant (§8.5). The converter writes `c/id` wherever such a name stands, a tag included
  (`^{:tag (c/id "nil")}`).
- **Identifiers equal to a reserved head** (§15.4: `at`, `init`, `lit`, `conv`, `when`, `generic`,
  `label`, ...) are written as themselves everywhere but as a callee, where the call is
  `(c/call name args...)` (Go forms' `go/call` rule): a function named `init` is called `(c/call init x)`.
  As a list head only a callee can collide: type forms are read in type positions only (§5.2).
- **Labels** are keywords: `(label :again)`, `(goto :again)`. **Members** are the names of `.-f`
  symbols and of member keywords in designators (`:x`). **Tags** are symbols in `(struct s)`.
- **Keywords of the standard** are never identifiers of the forms: type specifiers are type symbols
  (§5.1), storage classes metadata (§6.1), statements heads. A C23 keyword is an identifier in an older
  file (`int typeof;` in `:c89`), written as a symbol as any identifier.
- **Reserved identifiers** (`__x`, `_X`) are ordinary symbols; the builtins `__builtin_*` are callee
  symbols the checker knows (§7.9).

### 4.5 Programs and linkage

A program is a set of units, named outside the forms: a corpus' action, or a user's program's
`program.edn` (REAP7's shape, `:c` sources). Each unit's external declarations have C's linkage
(6.2.2): the checker derives it from storage classes and scope, the builder links the units by name, and
the tentative definitions and composite types of 6.9.2 and 6.2.7 are resolved per unit then across
units as a linker resolves common symbols (gcc 15's default `-fno-common`: two definitions of one
external object are a link error).

## 5. Types

### 5.1 Type forms

| C (6.2.5, 6.7.3) | type form |
|---|---|
| `void`, `char`, `int`, `float`, `double` | `void`, `char`, `int`, `float`, `double` |
| `signed char`, `unsigned char`, `short`, `unsigned short` | `signed-char`, `unsigned-char`, `short`, `unsigned-short` |
| `unsigned`, `long`, `unsigned long`, `long long`, `unsigned long long` | `unsigned`, `long`, `unsigned-long`, `long-long`, `unsigned-long-long` |
| `long double` | `long-double` |
| `bool`, `_Bool` | `_Bool` (all standards; `bool` in C23 is the same keyword type, N2) |
| `_BitInt(17)`, `unsigned _BitInt(7)`, `signed _BitInt(4)` | `(bit-int 17)`, `(unsigned-bit-int 7)`, `(bit-int 4)` |
| `float _Complex`, `double _Complex`, `long double _Complex`, `_Complex T` | `(complex float)`, `(complex double)`, `(complex long-double)`, `(complex T)` |
| `_Decimal32`, `_Decimal64`, `_Decimal128` | `_Decimal32`, `_Decimal64`, `_Decimal128` |
| `_Float16`, `_Float32`, `_Float64`, `_Float128`, `_Float32x`, `_Float64x`, `__bf16` (GNU) | the same symbols |
| `__int128`, `unsigned __int128` (GNU) | `__int128`, `unsigned-__int128` |
| `T *` | `(* T)` |
| `const T`, `volatile T`, `restrict T` (on pointers) | `(const T)`, `(volatile T)`, `(restrict T)`; nested in written order, outermost first: `T *const volatile` is `(const (volatile (* T)))` |
| `_Atomic(T)`, `_Atomic T` | `(atomic T)` (6.2.5: the same type) |
| `T[10]`, `T[]`, `T[n]`, `T[*]` | `(array 10 T)`, `(array T)`, `(array n T)` (a VLA when `n` is not a constant expression, derived), `(array * T)` |
| array parameter `T a[static 3]`, `T a[const 3]`, `T a[restrict static n]` | `^:c/static (array 3 T)`, `^{:c/quals [const]} (array 3 T)`, `^{:c/quals [restrict] :c/static true} (array n T)` |
| `R(int, char *, ...)`, `R(...)`, `R(void)`, `R()` | `(fn [int (* char) &] R)`, `(fn [&] R)` (C23), `(fn [void] R)`, `(fn [] R)` (§5.3) |
| `struct s`, `union u`, `enum e` | `(struct s)`, `(union u)`, `(enum e)` |
| `struct s { ... }`, `struct { ... }` | `(struct s [members])`, `(struct [members])` (§5.4) |
| `enum e { ... }`, `enum e : long { ... }` | `(enum e [enumerators])`, `^{:c/underlying long} (enum e [...])` (§5.5) |
| a typedef name `T` | `T` |
| `typeof(e)`, `typeof(T)`, `typeof_unqual(e)`, `typeof_unqual(T)` | `(typeof e)`, `(typeof-type T)`, `(typeof-unqual e)`, `(typeof-unqual-type T)` (GNU's `__typeof__` the same, N2) |
| the type of `nullptr` | `(typeof nil)` (`nullptr_t` is a typedef name of `<stddef.h>`, §13.4) |

- **Type symbols are never C identifiers.** Every base type symbol is a keyword of every standard
  (`int`, `char`...), joins words with `-` (which no identifier contains), or is a reserved identifier
  (`_Bool`, `__int128`, `_Float32`): so `typedef int bool;` in a `:c89` file, `typedef float float32;`,
  `typedef long int128;` (legal, and common in old C) name typedefs, and the symbols `bool`, `float32`,
  `int128` are those typedefs. `signed`, `signed int` are `int`; `short int`, `signed short` are `short`;
  plain `char` is its own type (signed on the target, §12.2).
- **Qualifiers** are wrapper forms, in the order written, outermost first; a consumer compares types
  with the qualifier set (6.2.5's qualified types), so `(const (volatile int))` and
  `(volatile (const int))` are one type, and the converter writes C's order: `const volatile int x` is
  `(const (volatile int))`. A qualifier on an array type qualifies its elements (6.7.4.1):
  `const int a[3]` is `(array 3 (const int))`.
- **`T[*]`** occurs only in a prototype's parameter. **`static`** and qualifiers inside a parameter's
  brackets are metadata on the `array` form (`^:c/static`, `:c/quals`), since a literal size cannot carry
  metadata.
- **Function types** are `(fn [params] R)` (§5.3); `R` is a type form. GNU's `__attribute__((mode(DI)))`
  and vector types are attributes (§5.6, §14).
- **Address spaces, `_Nonnull`, `__ptr32` and Microsoft's `__declspec`** are read and printed only, as
  `:c/gnu-attrs` or `:c/quals` entries (§14).

### 5.2 Where types are written

- As the **tag** of a declared name: `^int n`, `^{:tag (* (const char))} s`. Metadata `^T` is
  `{:tag T}` (the reader's rule for a symbol), so a type that is a symbol needs no map: `^size_t n`.
- As the tag of a **parameter vector**: the function's return type (§5.3).
- As the **first argument** of the heads that take a type: `conv`, `sizeof-type`, `alignof-type`,
  `compound-lit`, `typeof-type`, `va-arg`, `offsetof`, `types-compatible?`, `generic`'s associations,
  `lit` (§8), `c/typedef`.
- Inside type forms.

A symbol in a type position is a type: a base type symbol or a typedef name; the converter has decided
it (C's grammar needs the typedef names in scope to parse, 6.7.8, and the parse's decision is structure
here). A type form is never an expression: there is no ambiguity for a consumer.

### 5.3 Function types and parameters

```
(fn [param*] R)
param = T              ; an unnamed parameter of type T
      | ^T name        ; a named parameter (the name's tag is its type)
      | void           ; alone: (void), no parameters
      | &              ; last: ..., variadic
```

- `(fn [] R)` is `R()`: in a `:c23` file the same as `(fn [void] R)` (6.7.7.4), before C23 a function
  without a prototype (its calls' arguments get the default argument promotions, derived). The printer
  prints `[]` as `()` and `[void]` as `(void)` (N2 does not merge them).
- Parameter names in a prototype are kept (they are spelling, but cemit prints them, and they make the
  print readable); attributes of a parameter are metadata on its name, or on the type form of an
  unnamed one.
- An array or function parameter type is written as declared (`(array 10 int)`, `(fn [int] int)`); its
  adjustment to a pointer (6.7.7.4) is derived.
- A **function definition's** parameter vector is the same grammar (`c/defn`, §6.4); a C23 definition
  may have unnamed parameters (`[int]`).
- **K&R definitions** (before C23): `int f(a, b) int a; char *b; { ... }` is
  `(c/defn f ^int ^:c/kr [^int a ^{:tag (* char)} b] ...)`: the identifier list's order is the vector's,
  each parameter tagged with its declared type (an undeclared one `^:c/implicit-int`), `:c/kr` marks the
  form (the declarations' order and grouping are `:full`'s).

### 5.4 Struct and union types

```
(struct tag)                     ; a reference: struct tag
(struct tag [member*])           ; a definition: struct tag { ... }
(struct [member*])               ; an anonymous definition: struct { ... }
(struct tag [])                  ; an empty definition (GNU): struct tag {}
member = ^T name                       ; a member
       | ^{:tag T :c/bits n} name      ; a bit-field
       | (c/bits T n)                  ; an unnamed bit-field: T : n; (n may be 0)
       | (struct [...]) | (union [...]) ; an anonymous member (6.7.3.2), tag allowed (GNU, -fms-extensions: read and printed only)
       | ^{:tag (array T)} name        ; a flexible array member, last
       | (c/static-assert e msg?)      ; C11's static assertion among the members
```

`union` is the same. The vector keeps the members' order (a Clojure map would not, and the reader would
refuse duplicate keys). Attributes on the type are metadata on the form, attributes on a member
metadata on its name (§5.6):

```clojure
(c/struct point [^int x ^int y])                       ; struct point { int x, y; };
^{:c/gnu-attrs [packed]} (struct hdr [^char tag ^int len])
(struct [^{:tag unsigned :c/bits 3} kind
         (c/bits unsigned 0)
         ^{:tag unsigned :c/bits 5} flags
         (union [^int i ^float f])
         ^{:tag (array char)} name])
```

A definition inside a type form defines the tag in the scope where the form stands (a parameter list,
a cast, `sizeof`'s operand, a compound literal), as in C; the same tag defined twice with the same
content is C23's tag compatibility (6.7.3.4), derived.

### 5.5 Enumerated types

```
(enum tag)                           ; a reference
(enum tag [enumerator*])             ; a definition; (enum [...]) anonymous
enumerator = NAME | [NAME expr]      ; with an explicit value
```

```clojure
(enum hue [RED [GREEN 3] BLUE])                         ; enum hue { RED, GREEN = 3, BLUE }
^{:c/underlying unsigned-char} (enum hue [RED [GREEN 3] BLUE])   ; enum hue : unsigned char { ... }
^{:c/underlying long} (enum big)                        ; enum big : long;  (a C23 forward declaration)
```

An enumerator's implicit value (the previous plus one) and the enumerated type's compatible integer type
(6.7.3.3; gcc's choice, §12.3) are derived, never written. Attributes on an enumerator: metadata on its
symbol or on its vector (`^{:c/attrs [deprecated]} [OLD 1]`).

### 5.6 Attributes

C23's attributes (6.7.13) are `:c/attrs`, GNU's `__attribute__((...))` `:c/gnu-attrs`, both a vector of
attributes in source order:

```
attribute = name                        ; [[deprecated]], [[fallthrough]]
          | [name arg*]                 ; [[nodiscard("why")]], [[gnu::aligned(8)]]
name      = symbol                      ; a standard attribute: deprecated
          | ns/symbol                   ; a prefixed one: gnu::packed is gnu/packed
args      = forms (C context)           ; a standard or gnu:: attribute's arguments: expressions
          | "balanced tokens"           ; another vendor's: the tokens' text, one string
```

```clojure
(c/declare ^{:c/attrs [[nodiscard "why"]]} f ^int [void])     ; [[nodiscard("why")]] int f(void);
(c/struct ^{:c/gnu-attrs [packed [aligned 8]]} s [^char c ^int i])
                                    ; struct __attribute__((packed, aligned(8))) s { char c; int i; };
(c/declare ^{:c/gnu-attrs [[format printf 1 2]]} logf ^void [^{:tag (* (const char))} fmt &])
                                    ; format(printf, 1, 2): printf is the attribute's identifier
(c/var ^{:c/attrs [[clang/annotate "\"x\" , 1"]] :tag int} v)   ; [[clang::annotate("x", 1)]] int v;
```

The metadata goes where C puts the attribute: before a declaration (on its form's name, with
`:c/attrs-at :decl` when it precedes the specifiers rather than following the declarator; the
distinction is spelling for every standard attribute but N3220's appertainment rules, which the checker
applies), after a declarator's identifier (on the name), after a `*` (on the `*` form), on a struct,
union or enum (on the type form), a member, an enumerator, a parameter, a label (on the `label` form), a
statement (on the statement form). An attribute declaration is `(c/attr-decl attribute...)`:
`[[fallthrough]];` is `(c/attr-decl fallthrough)` (6.7.1). `[[__nodiscard__]]` is `nodiscard` (N2).

Which attributes mean something to a consumer: `[[deprecated]]`, `[[nodiscard]]`, `[[maybe_unused]]`,
`[[fallthrough]]` are diagnostics only; `[[noreturn]]`, `[[unsequenced]]`, `[[reproducible]]` are
promises (their violation is undefined, §13); GNU's `packed`, `aligned`, `mode`, `vector_size`,
`transparent_union`, `weak`, `alias`, `section`, `cleanup`, `constructor`, `destructor` change layout,
linkage or behaviour and are the checker's and the builder's (`vector_size` is out of eval, §14).

## 6. Declarations

### 6.1 Storage classes and specifiers

Metadata on the declared name: `^:static`, `^:extern`, `^:thread_local` (also `_Thread_local` and GNU's
`__thread`, N2), `^:register`, `^:auto`, `^:constexpr` (6.7.2), the function specifiers `^:inline` and
`^:noreturn` (`_Noreturn`; C23's `[[noreturn]]` is an attribute, §5.6), and `:c/alignas` (an integer
constant expression) or `:c/alignas-type` (a type): `alignas(16) char buf[64]` is
`(c/var ^{:c/alignas 16 :tag (array 64 char)} buf)`. GNU's `__extension__` before a declaration is
`^:c/extension` (it silences pedantic warnings only).

`^:auto` with a `:tag` is the storage class `auto`; `^:auto` without one is C23's type inference
(6.7.10): `auto x = 1.5;` is `(c/var ^:auto x 1.5)`, and the checker infers `double`.

### 6.2 `c/var`: objects

```
(c/var name)            ; T name;         the tag is T
(c/var name init)       ; T name = init;
```

```clojure
(c/var ^:static ^{:tag (* (const char))} name "x")       ; static const char *name = "x";
(c/var ^:extern ^int errno_copy)                         ; extern int errno_copy;
(c/var ^int counter)                                      ; int counter;  (a tentative definition, 6.9.2)
(c/var ^:constexpr ^int N 4)                              ; constexpr int N = 4;
(c/var ^{:tag (struct point)} p (init :y 2 :x 3))        ; struct point p = { .y = 2, .x = 3 };
(c/var ^{:tag (array int)} a (init (at 2) 5 1 2))        ; int a[] = { [2] = 5, 1, 2 };
(c/var ^{:tag (* (fn [int (* char)] int)) :c/asm-label "h0"} handler)   ; ... handler asm("h0");
```

Whether a `c/var` is a definition, a tentative definition or a declaration, and its linkage, are C's
rules over its storage class and scope (6.2.2, 6.9.2), derived.

### 6.3 `c/declare`: functions

```
(c/declare name ^R [param*])     ; R name(params);   a prototype or an old-style declaration
(c/declare ^T name)              ; T name;  where T names a function type (a typedef)
```

```clojure
(c/declare ^:extern printf ^int [^{:tag (restrict (* (const char)))} fmt &])  ; extern int printf(const char *restrict fmt, ...);
(c/declare ^:static ^:inline area ^int [^{:tag (* (const (struct point)))} p])
(c/declare ^handler_fn on_signal)                              ; handler_fn on_signal;
```

### 6.4 `c/defn`: function definitions

```
(c/defn name ^R [param*] stmt*)
```

The body is the function's compound statement, its statements in order (§7.3); no value is returned
implicitly. `main` that reaches its end returns 0 (5.1.2.3.4), derived. Metadata on the name: storage
class, function specifiers, attributes (both kinds), `:c/asm-label`. A definition's attributes after its
declarator (6.7.7.4) are `:c/attrs-at :declarator`. Old-style definitions are `^:c/kr` (§5.3); an
implicit `int` return type before C23 is `^:c/implicit-int` on the vector, the tag absent:

```clojure
(c/defn ^:static tally ^int [^{:tag (* (const char))} s] ...)
(c/defn f ^:c/implicit-int ^:c/kr [^int a] (return a))      ; f(a) int a; { return a; }   (:gnu89)
```

A GNU nested function is a `c/defn` among a block's declarations (§7.2): read and printed, out of eval
and AOT (§14).

### 6.5 `c/typedef`

```clojure
(c/typedef usize unsigned-long)                          ; typedef unsigned long usize;
(c/typedef handler_fn (fn [int] void))                   ; typedef void handler_fn(int);
(c/typedef node (struct node [^int v ^{:tag (* (struct node))} next]))
```

The type is the second argument (a typedef has no value, so no tag on the name is needed); attributes
and `:c/alignas` (GNU's `aligned` on a typedef) on the name.

### 6.6 Tag declarations

A declaration that declares only a tag (6.7.3.4) is `c/struct`, `c/union` or `c/enum` with the type
form's arguments:

```clojure
(c/struct point [^int x ^int y])        ; struct point { int x, y; };
(c/struct point)                        ; struct point;  (a forward declaration)
(c/enum hue [RED [GREEN 3] BLUE])
(c/union ^{:c/attrs [deprecated]} u [^int i ^float f])
```

Attributes of the tag are metadata on the tag's symbol.

### 6.7 `c/group`: declarators that share a defined type

A declaration whose specifiers define a struct, union or enum (with its member list, anonymous or
tagged) and that has several declarators gives them one type; printing them as separate declarations
would define the type again, or make two anonymous types that are not compatible. Such a declaration is
one `c/group`, its specifiers' type first and its declarators' forms after, with `c/base` standing for
the shared type:

```clojure
(c/group (struct [^{:tag unsigned :c/bits 3} kind ^{:tag (array char)} name])
  (c/typedef node c/base)
  (c/typedef node_ptr (* c/base)))      ; typedef struct { ... } node, *node_ptr;
(c/group ^:static (struct [^int x])
  (c/var ^{:tag c/base} a)
  (c/var ^{:tag (* c/base)} b (addr a)))  ; static struct { int x; } a, *b = &a;
```

Storage classes shared by the declarators are on each declarator's name; metadata on the group's type
form is the specifiers'. Other multi-declarator declarations (`int a, *b;`) are one form per declarator,
the later ones `^:c/grouped` (N3: the `:lines` print splits them, `:full` joins them again).

### 6.8 Static assertions, attribute declarations, `asm`, empty declarations

```clojure
(c/static-assert (== (sizeof-type int) 4))               ; static_assert(sizeof(int) == 4);
(c/static-assert (== N 4) "N")                           ; _Static_assert(N == 4, "N");
(c/attr-decl fallthrough)                                ; [[fallthrough]];
(c/asm "\t.globl f")                                     ; asm("\t.globl f");  at file scope
```

An empty declaration `;` at file scope (GNU and C23 accept it) is `:full`'s only. `c/static-assert` and
`c/attr-decl` stand where declarations do: at file scope, in a block (§7.2), among members.

### 6.9 Initializers

```
init-expr = expr                       ; = e
          | (init element*)            ; = { ... }; (init) is = {} (6.7.11)
element   = init-expr                  ; positional
          | :member init-expr          ; .member = ...
          | (at designator+) init-expr ; [i] = ..., .a.b[2] = ..., [lo ... hi] = ... (GNU)
designator = :member | index-expr | [lo hi]
```

```clojure
(init 1 2 3)                     ; { 1, 2, 3 }
(init :y 2 :x 3)                 ; { .y = 2, .x = 3 }
(init (at 2) 5 1 2)              ; { [2] = 5, 1, 2 }
(init (at :pos 1) 7)             ; { .pos[1] = 7 }
(init (at [0 9]) -1)             ; { [0 ... 9] = -1 }  (GNU)
(init (init 1 2) (init 3 4))     ; { {1, 2}, {3, 4} }
"abc"                            ; = "abc" for a char array; (init "abc") for = { "abc" }
```

Elements keep source order; brace elision is written as the source elides (the checker fills the
aggregate as 6.7.11 says). `(c/embed ...)` may stand among the elements (§9.3).

## 7. Code

### 7.1 C context

The inside of `c/defn` bodies, initializers, array sizes, enumerator values, `c/static-assert`'s and
attributes' arguments, and the operands of `c/var` is C context: the forms mean C. A form is a
statement where a statement list stands (a body, `do`, a `let` body, a `switch` body, the branches of
`if`, `when`, `cond`) and an expression elsewhere. An expression in a statement list is C's expression
statement. Labels are items of a statement list (§7.8).

### 7.2 Blocks and declarations: `let`

A declaration in a block scopes from its declarator to the end of the block (6.2.1). In the forms it is
a `let` whose body is **the rest of the block**, so a `let` is the last form of its statement list, and
consecutive declarations share one `let` (Go forms' rule):

```
(let [binding*] stmt*)
binding = name init                  ; T name = init;  (the tag on the name)
        | (c/var name)               ; T name;  any c/var, c/declare, c/typedef, c/struct,
        | (c/group ...)              ;          c/union, c/enum, c/group, c/static-assert,
        | ...                        ;          c/attr-decl, or a GNU nested c/defn
```

```clojure
(let [^int n 0
      (c/var ^int j)
      (c/var ^:static ^int calls 0)
      (c/typedef pair (array 2 int))]
  (inc! calls)
  ...)
```

- A binding vector's element that is a list headed by a `c/` declaration head is a declaration; any
  other element is a name followed by its initializer.
- A jump into a `let`'s body from outside it (a `goto` or a `case` label past the declaration) is C's
  jump over a declaration: allowed but into the scope of a variably modified type (6.8.6.1), and the
  object is then uninitialized, as in C.
- A `(do ...)` is an explicit compound statement `{ ... }`: a block of its own.
- GNU's local labels `__label__ a, b;` are `(c/local-labels :a :b)`, a binding of the block.

### 7.3 Statements

| C (6.8) | form |
|---|---|
| `{ ... }` | `(do stmt*)` |
| `e;` | the expression's form (`(c/expr e)` when `e` is a literal that needs metadata) |
| `;` | nothing in a statement list (N4); an empty body is an empty one: `while (c);` is `(while c)` |
| `if (c) s` | `(when c stmt*)` |
| `if (c) s1 else s2` | `(if c s1 s2)`, each branch one statement (a `do` for several) |
| `if (a) ... else if (b) ... else ...` | `(cond a s1 b s2 :else s3)`, each `s` one statement |
| `switch (e) s` | `(switch e item*)` |
| `while (c) s` | `(while c stmt*)` |
| `do s while (c);` | `(do-while c stmt*)` |
| `for (init; cond; step) s` | `(for [init] cond step stmt*)` |
| labels: `L:`, `case 3:`, `case 1 ... 3:` (GNU), `default:` | items: `(label :L)`, `(case 3)`, `(case 1 3)`, `(default)` |
| `goto L;`, `goto *p;` (GNU) | `(goto :L)`, `(goto* p)` |
| `continue;`, `break;` | `(continue)`, `(break)` |
| `return;`, `return e;` | `(return)`, `(return e)` |
| `asm(...)` statement (GNU) | `(c/asm ...)` (§7.9) |
| an attribute declaration, `[[fallthrough]];` | `(c/attr-decl fallthrough)` |
| an attribute on a statement, `[[likely]] { ... }` | `^{:c/attrs [likely]} (do ...)` |

- **Bodies.** `(when c s1 s2)` is `if (c) { s1; s2; }`; `if (c) s;` without braces reads the same (N4:
  a selection or iteration statement's substatement is a block in C, 6.8.4, 6.8.5, so the braces do not
  change scopes). `if`'s branches are single statements as in Clojure, a `do` holding several.
- **`for`'s init** is a vector: `[]` none, `[expr]` an expression, or bindings as `let`'s:
  `(for [^int i 0] (< i n) (inc! i) ...)`, `(for [(set! i 0)] (< i n) (inc! i) ...)`. An omitted
  condition or step is `_`; a C identifier `_` in an expression is `(c/id "_")` there. An infinite
  `for (;;)` is `(for [] _ _ ...)`; `while (1)` stays `(while 1 ...)`.
- **Labels are items** (C-lisp's rule, and C23's 6.8.2: a label may precede a declaration and end a
  block): `(label :again)` labels what follows it; at a block's end it labels nothing (C23; before C23
  the printer prints `L: ;`). Labels may stand in any statement list, so a `case` inside a nested
  loop of the `switch` (Duff's device) is a `(case n)` item of that loop's body (§16.3). Attributes on a
  label are metadata on the `label` form.
- **`switch`**'s items are its body's statement list: `(switch x (case 1) (case 2) (f) (break) (default)
  (g))`. A body that is not a compound statement (`switch (x) case 1: f();`) is `^:c/bare` on the form.

### 7.4 Assignment, increment

```
(set! place e)            ; place = e
(set! place op e)         ; place op= e, op one of + - * / % << >> bit-and bit-or bit-xor
(aset a i e)              ; a[i] = e, the same as (set! (aget a i) e)
(inc! place) (dec! place)            ; place++ place--   (postfix: the old value)
(pre-inc! place) (pre-dec! place)    ; ++place --place
```

All are expressions (6.5.17, 6.5.3.2): their value is the value stored (postfix: the value before), not
an lvalue. A **place** is a modifiable lvalue: a name, `@p`, `(aget a i)`, `(.-f s)`, `(-> p f)`, a
compound literal, a parenthesized place, and GNU's `(? c a b)` of lvalues is not one (rejected by
gcc 15). The converter writes `(aset a i v)` for an assignment whose place is a subscript and whose
value is not used, `(set! (aget a i) v)` otherwise. The place's operands are evaluated once.

### 7.5 Operators

Operators mean C's at the operands' types after the usual conversions (6.3.1.8), derived: `+` on `int`
wraps (§13), on a pointer and an integer is pointer arithmetic, `/` truncates, `<<` by a count beyond the
width is §13's.

| C | form | arity |
|---|---|---|
| `a + b`, `a - b`, `a * b`, `a / b` | `(+ a b)`, `(- a b)`, `(* a b)`, `(/ a b)` | n-ary, left to right |
| `a % b`, `a << n`, `a >> n` | `(% a b)`, `(<< a n)`, `(>> a n)` | binary |
| `a & b`, `a \| b`, `a ^ b` | `(bit-and a b)`, `(bit-or a b)`, `(bit-xor a b)` | n-ary, left to right |
| `a && b`, `a \|\| b` | `(and a b)`, `(or a b)` | n-ary, short-circuit, `int` result |
| `a == b`, `a != b`, `a < b`, `a <= b`, `a > b`, `a >= b` | `(== a b)`, `(!= a b)`, `(< a b)`, `(<= a b)`, `(> a b)`, `(>= a b)` | binary only, `int` result |
| `-x`, `+x`, `~x`, `!x` | `(- x)`, `(+ x)`, `(bit-not x)`, `(not x)` | unary |
| `&x`, `*p` | `(addr x)`, `@p` | unary |
| `c ? a : b`, `a ?: b` (GNU) | `(? c a b)`, `(? a b)` | ternary; the two-operand form evaluates `a` once |
| `a, b, c` | `(comma a b c)` | n-ary, left to right, sequenced |

- **Names.** `^` is the reader's metadata character and `~`, `|` are not usable, so the bit operators
  are Clojure's `bit-*` names (Go forms' rule); `?:` is not a symbol the reader accepts (a symbol cannot
  end in `:`), so the conditional is `?`.
- `@p` reads as `(arbace.core/deref p)`; in C context that list is indirection whatever its qualifier.
- **n-ary forms** are left-nested chains of one operator, as Go forms': `(+ a b c)` is `(a + b) + c`; the
  converter flattens a left-nested chain of one operator and keeps any other nesting. The order of
  evaluation of the operands is unspecified in C (6.5); §13.3 says which order ci takes (CS31).
- **Comparisons** are binary only: `(< a b c)` is refused.
- **Parentheses** the source wrote and C's precedence does not need are kept: `^:c/paren` on the
  parenthesized form (`^:c/paren (+ a b)`), and for a literal operand, which cannot carry metadata, its
  index in the parent's `:c/aparen` set (`^{:c/aparen #{1}} (* 10 x)` for `(10) * x`). A redundant
  pair the precedence needs is not marked: the printer adds what precedence needs (CS41 (a)).
- **Negative literals.** A negative number is the unary minus of the literal of its magnitude, as C
  parses `-1` (§8.1). `(- 1)` is never written; `(- x)` for a non-literal.

### 7.6 Members, subscripts, calls

| C | form |
|---|---|
| `s.f`, `p->f`, `p->a->b`, `p->a.b` | `(.-f s)`, `(-> p f)`, `(-> p a b)`, `(.-b (-> p a))` |
| `a[i]`, `i[a]` | `(aget a i)`, `^:c/swapped (aget a i)` (the operands in the source's order are `i`, `a`) |
| `f(a, b)`, `(*fp)(x)`, `fp(x)`, `s.fn(x)` | `(f a b)`, `(@fp x)`, `(fp x)`, `((.-fn s) x)` |
| a call of a function named like a reserved head | `(c/call init x)` (§4.4) |

A call's callee is any expression of function or pointer-to-function type; whether a name is a
function or an object holding a pointer is the declarations', derived. Arguments of a variadic or
unprototyped function get the default argument promotions, derived.

### 7.7 Casts, sizes, generic selection, compound literals

| C | form |
|---|---|
| `(T)e`, `(void)e` | `(conv T e)`, `(conv void e)` |
| `sizeof e`, `sizeof(T)` | `(sizeof e)`, `(sizeof-type T)` |
| `alignof(T)`, `_Alignof(T)`, GNU's `__alignof__(e)` | `(alignof-type T)`, `(alignof e)` |
| `_Generic(e, int: a, char *: b, default: c)` | `(generic e [int a] [(* char) b] [:default c])` |
| `_Generic(int, ...)` (a type operand, C2y; gcc 15 accepts it with a pedantic diagnostic) | `(generic ^:c/type int ...)`: the operand is a type form |
| `(T){1, 2}`, `(static T){...}` (C23 6.5.3.6) | `(compound-lit T 1 2)`, `^:static (compound-lit T ...)` (storage classes on the form) |
| `({ s1; s2; e; })` (GNU) | `(stmt-expr s1 s2 e)`: its value is the last expression statement's |
| `&&L` (GNU) | `(label-addr :L)` |
| `__real__ z`, `__imag__ z` (GNU) | `(real z)`, `(imag z)` (§14) |
| `__extension__ e` (GNU) | `^:c/extension` on `e`'s form |

`compound-lit`'s elements are an initializer list's (§6.9): `(compound-lit (struct point) :x 1 :y 2)`.
A compound literal is an lvalue with storage (6.5.3.6), not a constant: `lit` (§8) is never one.

### 7.8 Control flow notes

- `break` leaves the innermost `while`, `do-while`, `for` or `switch`; `continue` goes to the innermost
  loop's step or condition. `(goto :L)` jumps anywhere in the function (§7.2's rule on declarations).
- A label's scope is the function (6.2.1); GNU's `__label__` narrows it to a block.
- **`setjmp`/`longjmp`** are library functions (7.13) with the usual restrictions on where `setjmp`
  may be called; their forms are calls. ci implements them as natives (the survey's §6.5).

### 7.9 Builtins and GNU expression forms

`__builtin_*` functions are callees: `(__builtin_expect x 1)`, `(__builtin_memcpy d s n)`. The checker
knows gcc's builtins the corpora and musl's headers use (122 distinct in c-torture/execute, 13 in musl
1.2.6), their types and constant folding (`__builtin_constant_p`, `__builtin_choose_expr`,
`__builtin_inff`). Builtins that take a type or a member designator are forms of their own:

| C | form |
|---|---|
| `__builtin_va_arg(ap, T)` (`va_arg`) | `(va-arg ap T)` |
| `__builtin_offsetof(T, m.a[2])` (`offsetof`) | `(offsetof T :m :a 2)` (the designator path as `at`'s) |
| `__builtin_types_compatible_p(T1, T2)` | `(types-compatible? T1 T2)` |
| `__builtin_classify_type(T)` (with a type) | `(classify-type T)` |
| `__builtin_has_attribute(x, attr)` | `(has-attribute? x attribute)` |
| `__builtin_convertvector(v, T)`, `__builtin_shufflevector` | `(convertvector v T)`, a call (vector types, §14) |
| `__builtin_bit_cast(T, e)` | `(bit-cast T e)` |

`va_start`, `va_end`, `va_copy` and C23's one-argument `va_start` (`__builtin_c23_va_start`) are calls
of their builtins. The rest of the builtins in scope for eval and AOT is C5's list (the corpora's and
musl's); a builtin outside it is a class `builtin` (deferred).

**Inline assembly** (GNU, `asm`): `(c/asm "template")` for a basic `asm`; extended `asm` is
`(c/asm ^:volatile "template" :outputs [["=r" x]] :inputs [["r" y]] :clobbers ["memory"] :goto [:L])`,
each operand `[constraint expr]` or `[[name] constraint expr]` for a symbolic name. Read and printed;
out of eval and AOT (§14): musl's uses of it are natives in `crt` (the survey's §6.5).

## 8. Constants and literals

C's constants (6.4.4) and string literals (6.4.5) are Clojure literals with the same value; a constant
whose type is not its default is `(lit T v)` (CS7 (a)). The spelling a value does not keep is `:full`'s
(N1).

### 8.1 Integer constants

| C | form | type |
|---|---|---|
| `42`, `0x2A`, `052`, `0b101010`, `1'000` | `42`, `42`, `42`, `42`, `1000` | `int` |
| `2147483648`, `4294967295` | `2147483648`, `4294967295` | `long` (a decimal constant's list: `int`, `long`, `long long`) |
| `0x80000000`, `0xFFFFFFFF` | `(lit unsigned 2147483648)`, `(lit unsigned 4294967295)` | `unsigned` (a hexadecimal, octal or binary constant may be unsigned: 6.4.4.2's table) |
| `42u`, `42l`, `42ul`, `42ll`, `42ull` | `(lit unsigned 42)`, `(lit long 42)`, `(lit unsigned-long 42)`, `(lit long-long 42)`, `(lit unsigned-long-long 42)` | the suffix's, widened by the value as the table says |
| `0xFFFFFFFFFFFFFFFF` | `(lit unsigned-long 18446744073709551615N)` | `unsigned long` |
| `3wb`, `3uwb`, `255wb` | `(lit (bit-int 3) 3)`, `(lit (unsigned-bit-int 2) 3)`, `(lit (bit-int 9) 255)` | `_BitInt(N)`, N the smallest width that holds the value (and a sign bit for `wb`) |
| `-1`, `-2147483648` | `-1`, `-2147483648` | the unary minus of `1` (`int`) and of `2147483648` (`long`): `long` |
| `-0x80000000` | `(- (lit unsigned 2147483648))` | `unsigned` |

- **A bare integer** is a C integer constant whose type is the decimal unsuffixed rule's at its
  magnitude (`int`, `long`, `long long`); every other type is written by `lit`, the type the converter
  computed from the suffix, the radix and the value (lexical, so the reader's). A decimal constant beyond
  `long long` is gcc's `__int128` with a pedantic diagnostic: `(lit __int128 v)`.
- **Negative numbers** are the unary minus of the magnitude's constant (6.4.4.2 has no negative
  constants), so `-2147483648` is `long` as in C, and the type of `-0x80000000` needs its `lit`.
- The reader reads `0x2A`, `052` and `2r101010` as `42` (Clojure's radix syntax); the converter writes
  decimal (N1). BigInts (`N`) where the value exceeds a `long`.

### 8.2 Character constants

| C | form | type and value |
|---|---|---|
| `'a'`, `'\n'`, `'\0'`, `'\x7f'` | `\a`, `\newline`, `\u0000`, `\u007F` | `int`, the character's code |
| `L'a'`, `L'é'`, `L'\x263A'` | `\a`, `\é`, `\☺` | `int` (`wchar_t` is `int` on the target, UTF-32): the code point; N1 for `L'a'` |
| `u8'a'` (C23) | `(lit unsigned-char \a)` | `unsigned char` (`char8_t`) |
| `u'é'`, `U'é'`, `U'😀'` | `(lit unsigned-short \é)`, `(lit unsigned \é)`, `(lit unsigned 128512)` | `char16_t` (`unsigned short`), `char32_t` (`unsigned`) |
| `'ab'`, `'é'` (two UTF-8 bytes), `'\377'`, `'\200'` | `24930`, `50089`, `-1`, `-128` | `int`: gcc's value for a multi-character or high-byte constant (6.4.4.5, implementation-defined: each byte as a `char`, signed, shifted in), N1 keeps the spelling |

- **A Clojure character in C context** is an `int` constant whose value is the character's code point.
  The printer spells it `'c'` (with C's escapes: `'\n'`, `'\0'`, `'\x7f'`) when the code is below 0x80,
  and `L'c'` otherwise (on the target `L'é'` is `int` 233, as `\é` is), so a character prints back to
  a constant of the same type and value.
- Characters outside the Basic Multilingual Plane are integers (`128512`), as are surrogates; `(lit T v)`
  with a character or an integer gives the prefixed constants' types.
- The converter writes a character where the source wrote a character constant whose value is its
  character's code; else the integer.

### 8.3 Floating constants

| C | form | type |
|---|---|---|
| `1.5`, `1e10`, `.5`, `0x1p-2`, `1'0.5` | `1.5`, `1.0E10`, `0.5`, `0.25`, `10.5` | `double` |
| `1.5f`, `0.1f` | `(lit float 1.5)`, `(lit float 0.1)` | `float` |
| `1.1L`, `1.10000000000000000001L` | `(lit long-double 1.1)`, `(lit long-double 1.10000000000000000001M)` | `long double` |
| `1.5dd`, `1.50dd` | `(lit _Decimal64 1.5M)`, `(lit _Decimal64 1.50M)` | `_Decimal64` (the scale is the quantum) |
| `2.0i`, `2.0fi` (GNU) | `(imaginary 2.0)`, `(lit (complex float) (imaginary 2.0))` | `(complex double)`, `(complex float)` |

- **A number denotes its exact decimal value** (a double: `Double/toString`'s, the shortest that reads
  back to it; a BigDecimal or a ratio: itself), and the C constant is that value **rounded once** to the
  constant's type (6.4.4.3, round to nearest on the target). So `(lit float 0.1)` is `0.1f`, not the
  double 0.1 rounded again. The converter writes, of the literals that round to the constant's value at
  its type, the shortest: a double when one does (always for `float` and `double`), else a BigDecimal
  with the digits needed (`long double`'s 64-bit significand, `_Float128`), or a ratio for a hexadecimal
  constant whose decimal expansion is longer than 80 characters (Go forms' rule).
- An integral value keeps a decimal point or exponent (`1.0`, `16.0`), so its kind stays floating.
- C has no literal for an infinity or a NaN: `INFINITY` and `NAN` are calls of `__builtin_inff` and
  `__builtin_nanf("")` with their macro recorded (§9.4).
- Decimal floating constants keep their quantum as the BigDecimal's scale; their arithmetic is out of
  eval and AOT (§14).

### 8.4 String literals

| C | form | type |
|---|---|---|
| `"abc"`, `"a" "b"`, `"a\0b"` | `"abc"`, `"ab"`, `"a\u0000b"` | `char[N]`, the UTF-8 bytes and a NUL |
| `"\xff\x00a"` (not UTF-8) | `(byte-string 0xff "\u0000a")` | `char[N]` |
| `u8"x"` | `(lit unsigned-char "x")` in C23; `"x"` before (a `char` array then, N1) | `unsigned char[N]` (`char8_t`) |
| `L"x"`, `u"x"`, `U"x"` | `(lit int "x")`, `(lit unsigned-short "x")`, `(lit unsigned "x")` | arrays of `wchar_t`, `char16_t`, `char32_t`: UTF-32, UTF-16, UTF-32 code units |
| `L"\xD800"` (a lone surrogate) | `(lit int (code-units 0xD800))` | |

- A Clojure string in C context is an array of `char` holding its characters' UTF-8 encoding and a
  terminating NUL (the reference's execution character set is UTF-8). It must not contain unpaired
  surrogates.
- `(byte-string part...)` is Go forms' form: each part a string (its UTF-8 bytes) or an integer 0-255,
  concatenated. The converter splits a literal into maximal valid UTF-8 runs and single invalid bytes.
- `(lit E s)` with a string gives a prefixed literal's element type `E`; `(code-units part...)` holds
  code units that a string cannot (each part a string or an integer).
- Adjacent literals are concatenated at read (6.4.5): one form. Mixing `L` with `u8` is rejected by the
  reference, as by the reader.

### 8.5 `true`, `false`, `nullptr`, predefined identifiers

| C | form | |
|---|---|---|
| `true`, `false` (C23 keywords) | `true`, `false` | `bool` constants 1 and 0 |
| `true`, `false` before C23 (`<stdbool.h>`'s macros) | `1`, `0` with the macro recorded (§9.4) | `int` |
| `nullptr` | `nil` | `nullptr_t` |
| `NULL` | `(conv (* void) 0)` (musl's `((void*)0)`) with the macro recorded | |
| `__func__` | `__func__` | the implicit `static const char __func__[]` (6.4.2.2) |

## 9. The preprocessing layer

C's translation phases 1-4 (5.1.1.2) are the reader's: phase 4's result is the program, which the forms
hold (CS5 (a)); what phases 1-4 consumed is the layer.

### 9.1 The configuration

The reader preprocesses as the reference does for the file's `:std` and `:args`:

- **Predefined macros**: the reference's (`gcc -std=STD -dM -E` on an empty file; `__STDC_VERSION__` is
  `202311L` for `:c23`, `__STDC_HOSTED__` 1, `__x86_64__`, `__linux__`, `__GNUC__` 15...), recorded as
  data per standard by `bin/repr-c host record` (C2), never asked at read time.
- **Include paths**: the reference's search list (`gcc -v -E`), recorded with the headers' contents'
  digests: `/usr/include/fortify`, `/usr/include` (musl 1.2.6's headers, Alpine's), gcc's
  `include` directory (CS32 discusses its place).
- **`__has_include`, `__has_embed`, `__has_c_attribute`, `__has_attribute`, `__has_builtin`**: answered
  as the reference answers (gcc 15.2 answers `__has_c_attribute(nodiscard)` 202311, clang 23 answers
  202003: the reference's numbers are the recorded ones).
- **`__DATE__`, `__TIME__`, `__TIMESTAMP__`**: from one fixed epoch, the reference's runs recorded with
  the same `SOURCE_DATE_EPOCH` (CS43), so a read is reproducible.
- `__FILE__`, `__LINE__`, `__COUNTER__`, `__BASE_FILE__`, `__INCLUDE_LEVEL__`: as the text gives them.
- `#error` (active) makes the read fail with its message; `#warning` (C23) is a read warning, recorded in
  the read's report.

### 9.2 Directives as forms

A directive of the main file (or of a header, in its unit) that stands **between two items** (top-level
declarations, members, enumerators, parameters, statements of a body, initializer elements, arguments
of a call) is a form there:

| C (6.10) | form |
|---|---|
| `#include <stdio.h>`, `#include "x.h"`, `#include HDR` | `(c/include "<stdio.h>" :unit U)`, `(c/include "\"x.h\"" :unit U)`, `(c/include "<stdio.h>" :unit U :c/text "#include HDR")` |
| `#define N 10` | `(c/define N "10")` |
| `#define MAX(a, b) ((a) > (b) ? (a) : (b))` | `(c/define MAX [a b] "((a) > (b) ? (a) : (b))")` |
| `#define F(a, ...) f(a __VA_OPT__(,) __VA_ARGS__)`, GNU's `#define G(a, rest...) ...` | `(c/define F [a &] "...")`, `(c/define G [a & rest] "...")` |
| `#undef N` | `(c/undef N)` |
| `#if X > 1`, `#ifdef X`, `#elifdef Y`, `#else`, `#endif` (and the rest of the conditional family) | `(c/directive "#if X > 1")`, ...: the line's text after the `#`'s white space is normalized to one space |
| `#pragma pack(1)`, `_Pragma("pack(1)")` | `(c/pragma "pack(1)")`, `^:c/operator (c/pragma "pack(1)")` |
| `#line 10 "x.c"` | `(c/directive "#line 10 \"x.c\"")`; positions stay the file's raw ones (§10.3) |
| `#error "x"`, `#warning "x"` in an inactive group | text of the group (§9.4) |
| `#ident`, `#sccs`, `#assert`, `#unassert` (GNU), a null directive `#` | `(c/directive "...")` |

- The replacement list is its tokens' text with single spaces (6.10.4.1 compares replacement lists so);
  `:full` keeps its spacing.
- `(c/pragma ...)` is read by the checker for the pragmas that change the program: `pack` (layout),
  `STDC FP_CONTRACT`, `STDC FENV_ACCESS`, `STDC CX_LIMITED_RANGE`, GCC's `weak`, `redefine_extname`,
  `visibility`; the others (`once`, `GCC diagnostic`, `GCC optimize`) are the layer's only.
- A directive that does not fall between two items (inside an expression, between a declaration's
  specifiers) is `:full`'s only; the `:lines` print gives the active configuration (N6).
- **Eval and AOT ignore** `c/define`, `c/undef`, `c/directive`; `c/include` enters its unit (§4.3); the
  checker reads `c/pragma` as above.

### 9.3 `#embed`

`#embed` (6.10.4) expands to a comma-separated list of integer constants; in the forms it is one
element of the initializer list where it stands, holding its bytes:

```clojure
(init (c/embed "#embed \"data.bin\"" (byte-string "AB" 1)))
(init (c/embed "#embed \"data.bin\" limit(2) prefix(0,) suffix(,9)" :prefix [0] (byte-string "AB") :suffix [9]))
```

Its elements are the bytes as `int` constants (unsigned values 0-255), in order; `:prefix`, `:suffix`
and `:if-empty` hold those parameters' tokens as forms. A consumer splices them. The resource is read at
read time (the reference's search, `__has_embed` the same) and its bytes are in the forms: eval never
reads the resource.

### 9.4 Macro invocations

- An invocation whose expansion is **one whole node** of the program (its first and last tokens the
  node's, nothing else in the node) carries it on that node: `^{:c/macro "MAX(x, 3)"}` on a list or
  symbol; for a literal, which cannot carry metadata, the parent's `:c/amacro {index "text"}`
  (`^{:c/amacro {1 "LIMIT"}} (< i 10)` for `i < LIMIT`). The text is the invocation's tokens with single
  spaces (`:full` keeps the spacing). The `:lines` print writes the invocation in place of the node
  (cemit's recovery): the macro is defined where the print is compiled, by a `c/define` or a header.
- An invocation **inside** a node (`#define BEGIN {`, `#define ARGS a, b`), or nested so that its
  expansion is part of a node, is `:full`'s only: `:lines` prints the expansion (N6).
- The macros of a header (`EOF`, `errno`, `assert`, `va_arg`) are recovered the same way; `errno` is
  `^{:c/macro "errno"} @(__errno_location)`.
- `#` and `##` leave no trace beyond the invocation's text: the forms hold their result.

### 9.5 Errors

A file the reference rejects is a read error carrying the reference's message where a corpus judges
messages (gcc's `dg-error` tests, C7), as GP6 made gc's errors the Go read's. The port's own messages are
cc's (the oracle's) where cc gives one, and the error tests are judged by line and kind (error or not),
not by text, as goi's errorcheck (the survey's §5.5).

## 10. Positions, comments and mode `:full`

### 10.1 Mode `:lines` (the default)

As Go forms' §10.1, with less to keep: gcc's object code at `-O0` without `-g` depends on no position.

- **The forms file has the C file's lines**: each form that begins a C line is written on that line of
  the `.ae` file; the `in-ns` and `c/file` forms share the first line. A C line with several forms keeps
  them on one line; a form whose line would be taken is written after it with an explicit `:line`.
- The reader attaches `:line` to every list; a symbol or vector that begins a C line gets `^{:line n}`
  written. Literals that begin a line carry nothing (no consumer needs their lines: C's diagnostics and
  ci's reports cite the enclosing form's).
- A function's closing brace's line is `:c/end` on `c/defn`, for ci's reports of a function's end.
- Header units are laid out on their header's lines likewise.
- **What `:lines` does not keep**: columns, and lines of literals not at a line's start.

### 10.2 Mode `:full`

Every byte of the main file is in exactly one place of the forms or the layer, so the printer gives it
back byte for byte (level 2):

- **Node positions**: every list and symbol carries `:c/pos`, `[line column]` of its first token, and
  the positions of the tokens a form absorbs (`:lparen`, `:rparen`, `:lbrace`, `:rbrace`, `:semi`,
  `:colon`, `:op` for an operator, `:ops` for a chain's inner operators); literals' positions are in the
  parent's `:c/apos {index [line column]}`. Columns count bytes from 1.
- **Spellings**: `:c/text` on a form or `:c/atext {index "text"}` on a literal's parent where the value
  or the canonical words do not give the source's tokens: literal spellings (N1), specifier words and
  order (N2), a UCN in an identifier, a trigraph (before C23), a line splice inside a token.
- **The file-level layer** on `c/file`: `:c/comments [[line col "text"] ...]` (every comment, as the
  source spells it), `:c/space [[line col "ws"] ...]` (the white space before each token where it is not
  exactly one space: newlines with the indentation after them, tabs, several spaces; keyed by the token
  it precedes), `:c/splices [[line col] ...]` (backslash
  newlines outside tokens), `:c/tail` (what follows the last token), the semicolons and commas the forms
  do not imply (`:c/semis`, `:c/trails`).
- **The preprocessing layer**: each directive form's `:c/text` (the directive line as written, its
  white space and comments); the inactive groups' text on the conditional directive form that opens
  them (`:c/inactive "..."`); the text of every invocation (`:c/macro` with its spacing, and for an
  invocation that is not a whole node `:c/invocations [[line col "text"] ...]` on `c/file`, which the
  printer writes in place of the expansion's tokens).
- **Grouping**: `^:c/grouped` declarators (§6.7) print joined; a K&R definition's declaration list,
  an empty declaration, a null statement and the braces of a bare substatement are positions of their
  tokens.

The exact encoding of these runs is C3b's to settle with the round trip over the corpora, as repr/go's
`:full` got its "As built" (doc/go/SPEC.md §10.2); the rule it must keep is the first sentence above.

### 10.3 Line directives

`#line` is a directive form (§9.2); positions in the forms are the file's raw positions (as Go forms'
§10.3), so the printed directive adjusts them again in the reference.

### 10.4 Comments

`:lines` writes every comment as a `;` comment of the forms text at its C line (`;;` on a line of its
own, `;` at a line's end, each line of a block comment on its line); they are lost on reading. C has no
doc comments: no comment becomes a doc string. `:full` keeps them as data (§10.2).

## 11. What the converter writes, what is derived

### 11.1 Written by the converter

| what | why it is the reader's |
|---|---|
| declaration against expression, cast against parenthesized expression, `sizeof(T)` against `sizeof(e)`, a type against an expression in `typeof`, `_Generic`, `_Alignas` | C's grammar decides them by the typedef names in scope (6.7.8); the parse's decision is the forms' structure |
| which name is a typedef in a type position | the same |
| literal types (`lit`) | lexical: the suffix, the radix and the value (6.4.4) |
| parentheses, macro invocations, directives, attributes | the text |
| `:std`, `:args`, `:host` | the read's configuration |
| `^:c/kr`, `^:c/implicit-int`, `^:c/grouped`, `c/group` | the declaration's shape |
| positions, comments, spellings (`:full`) | the text |

### 11.2 Derived by a consumer (the checker, `arbace.c.check`, C4)

The type of every expression; the integer promotions, the usual arithmetic conversions, lvalue, array
and function conversions (6.3); the default argument promotions; null pointer constants; which `+` is
pointer arithmetic; constant expressions and their values (6.6): enumerators' implicit values,
`sizeof`, `alignof`, `constexpr` objects (6.7.2), case labels, array sizes completed by an initializer;
layout (§12.3); linkage, tentative definitions, composite types (6.2.2, 6.2.7, 6.9.2); `auto`'s and
`typeof`'s types; `_Generic`'s selection; the parameter adjustments; whether an array is variably
modified; `main`'s implicit `return 0`; the enumerated types' compatible types; the meaning of `(fn [] R)`
per `:std`. cc's checker is the oracle where it succeeds (`bin/ci annotations`, the survey's §6.2); its
gaps (`_BitInt`, `typeof(type-name)`, `constexpr`'s values, a VLA's `= {}`) are the checker's to fill,
the reference's acceptance their judge.

## 12. The memory model

CS9 (a): one flat 64-bit virtual address space per program, the same in eval and AOT. This section
states what a program can observe; how ci and `crt` implement it is GOI-SPEC's kind of document, C5's.

### 12.1 The address space

- Addresses are 64-bit, little-endian; a pointer is its address (8 bytes, `sizeof(void *) == 8`);
  pointers to objects and to functions are addresses in the same space, so casts between them and
  `uintptr_t` round-trip (6.3.2.3, implementation-defined: the identity).
- The **null pointer** is address 0 (all bits zero, so `memset(&p, 0, sizeof p)` gives a null pointer);
  no object or function is at 0, and a load or store at an address with no object traps (§13.1).
- **String literals and `const` objects of static storage** are read-only (the reference puts them in
  `.rodata`): a store traps as the reference's `SIGSEGV`.
- **Static storage** is zero-initialized (including padding) before the program starts; **automatic
  storage** holds what the stack's memory held (zero on a fresh stack): an uninitialized automatic
  object's value is that memory's (§13.2). **Allocated storage** is musl's `malloc` (16-byte aligned,
  musl's mallocng over `mmap`/`brk` natives), so reuse after `free` is musl's.
- **The stack**: each thread's stack is 8 MiB (the reference's default limit); a call beyond it traps as
  `SIGSEGV`. Frames are allocated downward; comparing addresses of different objects is undefined (§13).
- **Object representations** are bytes (6.2.6): a `char *` walk over any object, `memcpy` of structs
  holding pointers, unions that pun types, all see the bytes the reference lays out (§12.3), since
  layout, not the address values, is the observable part.
- **Address values** themselves differ from the reference's (the reference randomizes them per run):
  output that prints addresses is compared on status only (CS14 (a)'s `-O0`/`-O2` disagreement rule
  catches it).

### 12.2 Representations

| type | size | align | representation |
|---|---:|---:|---|
| `_Bool` | 1 | 1 | 0 or 1; another stored value read as `_Bool` is undefined (§13) |
| `char`, `signed-char`, `unsigned-char` | 1 | 1 | `char` is signed (gcc on x86-64; `-funsigned-char` in `:args` changes it), two's complement |
| `short` | 2 | 2 | two's complement |
| `int`, `unsigned` | 4 | 4 | |
| `long`, `long-long` and unsigned | 8 | 8 | |
| `__int128`, `unsigned-__int128` | 16 | 16 | |
| `(bit-int N)` | 1 for N ≤ 8, 2 ≤ 16, 4 ≤ 32, 8 ≤ 64, else 8·⌈N/64⌉ | as its size up to 8, else 8 | the psABI's: two's complement, the unused high bits of the last unit unspecified on store (gcc 15.2 measured: `_BitInt(129)` is 24 bytes, align 8; `BITINT_MAXWIDTH` 65535) |
| `float`, `double` | 4, 8 | 4, 8 | IEEE 754 binary32, binary64 |
| `long-double` | 16 | 16 | x87 80-bit extended in the low 10 bytes, 6 bytes of padding |
| `_Float128` | 16 | 16 | IEEE binary128 (out of eval, §14) |
| `(complex T)` | 2·T | T's | real then imaginary |
| `_Decimal32/64/128` | 4, 8, 16 | 4, 8, 16 | BID encoding (out of eval, §14) |
| pointers | 8 | 8 | the address |
| `wchar_t`, `char16_t`, `char32_t` | 4, 2, 4 | 4, 2, 4 | `int`, `unsigned short`, `unsigned` |
| `size_t`, `ptrdiff_t`, `intptr_t` | 8 | 8 | `unsigned long`, `long`, `long` |
| `max_align_t` | 16 | 16 | |

Floating-point: `FLT_EVAL_METHOD` 0 (each operation rounds to its type; `long double` operations to the
x87's 64-bit significand), round to nearest, IEEE 754 arithmetic (Annex F) for `float` and `double`.
The floating environment (`fesetround`, exception flags) is CS34's.

### 12.3 Layout

The SysV x86-64 psABI's, which gcc follows (cc's `abi.go` holds the same tables for linux/amd64):

- A struct's members at increasing offsets, each at the next offset that satisfies its alignment; the
  struct's alignment is its members' greatest, its size a multiple of it; a union's size is its largest
  member's rounded to its alignment.
- **Bit-fields** are allocated in units of their declared type's size, packed from the least significant
  bit, never straddling a unit's boundary for their type (a field that does not fit starts the next
  unit); `T : 0` ends the unit; an unnamed bit-field does not affect the struct's alignment; plain `int`
  bit-fields are signed. Measured: `struct { char a; int b : 3; char c; }` has size 4, `c` at 2.
- A **flexible array member** adds no size beyond the alignment it imposes.
- **Enumerated types**: with a fixed underlying type, that type; else gcc's choice from the values:
  `unsigned int` when none is negative and all fit, `int` when some is negative and all fit, else
  `unsigned long` or `long` (measured: `enum { A0, A1 }` is `unsigned`, its constants `int`;
  `enum { X = 1, Y = 0x100000000 }` is `unsigned long`, C23's constants of the enumerated type).
- `#pragma pack(n)`, `__attribute__((packed))`, `aligned(n)`, `alignas` change the layout as gcc does.
- Padding bytes' values: zero in static storage; otherwise unspecified (6.2.6.1), and a struct
  assignment copies them (as gcc's `-O0` code does with `rep movs` or moves of the whole object).

### 12.4 Values crossing into Arbace

At the REPL (CS20 (a)): integers as `Long` (`BigInt` beyond, `__int128` and `_BitInt` included),
floating values as `Double` (`long double` as a `BigDecimal` of its exact value), a pointer as a
`CPointer` (its address and type), a struct or array by its bytes on request; `(c/string p)` copies a
NUL-terminated string out of the program's memory. From Arbace: numbers in range, strings copied into a
fresh block. The C program's memory is its own: no Arbace object is ever in it.

## 13. Undefined, unspecified and implementation-defined behaviour

CS10 (a): ci gives the behaviour gcc 15.2's `-O0` code gives on x86-64 where the machine defines one,
traps what the checked mode detects, and the corpora are UB-free by their suites' rules. A program whose
reference runs differ between `-O0` and `-O2` is compared on status alone (CS14 (a)). A difference due to
undefined behaviour is a class `ub` (by design).

### 13.1 Undefined behaviour the machine defines

| behaviour (N3220 J.2) | ci, eval and AOT | measured on the reference |
|---|---|---|
| signed overflow of `+`, `-`, `*`, `++`, conversions to a signed type | wraps, two's complement | `INT_MAX + 1` is `INT_MIN` |
| `x / 0`, `x % 0` (integers) | `SIGFPE`: status 136 | status 136 |
| `INT_MIN / -1`, `INT_MIN % -1`, the same for `long` | `SIGFPE` | status 136 |
| a shift count negative or ≥ the promoted width | the count masked to the width's bits (5 for 32-bit, 6 for 64-bit), as `shl`/`sar` | `1 << 33` is 2 |
| `>>` of a negative value (implementation-defined) | arithmetic shift | `-8 >> 1` is -4 |
| a load or store through a null or unmapped pointer | `SIGSEGV`: status 139; the checked mode reports the C position | status 139 |
| a store into a string literal or a static `const` object | `SIGSEGV` | `.rodata` |
| `abort()`, an `assert` failure | `SIGABRT`: status 134 (musl's `abort`) | status 134 |
| stack exhaustion | `SIGSEGV` at 8 MiB | |
| floating division by zero, overflow, invalid | IEEE results (Annex F: not undefined) | |
| a floating value out of a signed integer type's range converted to it | the "integer indefinite" value (`INT_MIN` for `int`, `LONG_MIN` for `long`), as `cvttsd2si` | `(int)1e20` is `INT_MIN` |
| the same to an unsigned type | as gcc's `-O0` sequence gives (`(unsigned)1e20` and `(unsigned long)1e20` are 0; clang 23 gives `9223372036854775808` for the latter): the classes take what the corpora show | |

### 13.2 Undefined behaviour ci does not define

Reading an uninitialized automatic object (its memory's value: §12.1), accessing outside an object's
bounds (the neighbouring bytes, whose layout is not the reference's), using an object after its lifetime
(musl's allocator decides what is there), unsequenced modifications (`i++ + i++`), a `_Bool` holding
another value, reaching the end of a non-`void` function whose value is used, `__builtin_unreachable`
reached (the checked mode traps; unchecked, execution continues as the reference's `-O0` code does),
a violated `[[unsequenced]]` or `restrict` promise, data races, `longjmp` to a returned frame, modifying
an object through a `const`-qualified lvalue that is not in read-only memory. Their results are a class
`ub`; the checked mode (the REPL's default, CS9 (a)) reports the ones its block table sees: a load or
store outside a live block, a `free` of what was not allocated, a null dereference.

### 13.3 Unspecified behaviour

- **The order of evaluation** of operands and arguments (6.5.1). ci takes the order gcc 15.2's `-O0`
  code takes on x86-64 where the corpora show it: **a call's arguments right to left**, the operands of
  an operator left to right (measured: `g(f(1), f(2), f(3))` calls `f(3)`, `f(2)`, `f(1)` under gcc at
  `-O0` and `-O2`, `f(1)` first under clang 23). The alternative is CS31's.
- **Padding values**, the values of unnamed bit-fields, the bytes of a union beyond the member stored:
  §12.3.
- **Whether two string literals with the same content share storage**: they do within a unit when their
  contents are equal (gcc's merging); across units, not.
- **`qsort`'s order of equal elements**, `rand`'s sequence, `printf`'s digits: musl's, so equal by
  construction (CS11 (a)).

### 13.4 Implementation-defined behaviour

The reference's, all fixed by the target: `char` signed; `CHAR_BIT` 8; integers two's complement with
no padding bits; conversion to a signed type modulo 2^N; `>>` arithmetic; plain `int` bit-fields
signed; multi-character constants as §8.2; the source and execution character sets UTF-8; `wchar_t`
`int`; `__STDC_IEC_559__` 1; the headers' answers are musl 1.2.6's with gcc's freestanding headers as
CS32 settles (musl 1.2.6's `<stddef.h>` defines no `nullptr_t` and no `unreachable`, its `<stdarg.h>` no
one-argument `va_start`, its `<limits.h>` no `BITINT_MAXWIDTH`, and there is no `<stdbit.h>`: measured
with the reference's default include order).

## 14. Read and printed, out of eval and AOT

CS3 (a): what the reader reads and the printer prints, but ci and cf2g refuse by design, each a class
(`by-design`, naming this section) when a corpus file needs it:

| construct | why out |
|---|---|
| GNU nested functions (`c/defn` in a block) | trampolines on an executable stack; no corpus needs them beyond c-torture's tests of them |
| `__real__`, `__imag__` (`real`, `imag`) | the survey's CS3 (a) lists them; `creal`, `cimag` (musl's functions) serve the same need |
| inline assembly (`c/asm` in a function, extended `asm`) | the target's instructions; musl's are natives in `crt` |
| GNU vector types (`vector_size`, `__builtin_shufflevector`, `__builtin_convertvector`) | SIMD semantics of their own (24 c-torture/execute files) |
| `__int128` beyond `+ - * / % << >> & \| ^ ~`, comparisons and conversions | the survey's CS3 (a): `__int128` arithmetic is in, its library (`printf` has none) and atomics out |
| `_Decimal32/64/128` arithmetic and conversions | BID arithmetic is libgcc's; musl's `printf` cannot print them; clang 23 rejects them on the target (measured) |
| `_Float16`, `_Float128`, `_Float32x`, `_Float64x`, `__bf16` arithmetic | libgcc's soft-float routines; `_Float32` and `_Float64` are `float` and `double` and are in |
| `__declspec`, address spaces (`__seg_fs`), `_Nonnull` | other targets' and other compilers' |
| the `cleanup`, `constructor`, `destructor` attributes | run order outside `main`: deferred to C5's list (a class `deferred` if a corpus file needs one) |
| `#pragma` other than §9.2's | the layer's only |

CS35 proposes which of `long double`, `__int128` and `_BitInt` beyond 64 bits are in for v1; this table
assumes its recommendation ((a): all three in).

## 15. Coverage

These tables check the spec against cc's productions (what the port parses) and N3220's syntax summary.

### 15.1 cc's productions

cc's `ast.go` (go-whim `80c49bf5`, crefactor/cc) has 85 node types and their cases; each case maps to:

| cc node (cases) | form |
|---|---|
| `TranslationUnit`, `ExternalDeclaration` (`FuncDef`, `Decl`, `AsmStmt`, `Empty`) | the unit's forms (§4); `c/defn`; a declaration form; `(c/asm ...)`; `:full`'s |
| `FunctionDefinition`, `DeclarationList` (K&R) | `c/defn`, `^:c/kr` (§6.4) |
| `Declaration` (`Decl`, `Assert`, `Auto`), `InitDeclaratorList`, `InitDeclarator` (`Decl`, `Init`) | `c/var`, `c/declare`, `c/typedef`, `c/group`, `c/static-assert`; `Auto` is the old GNU `__auto_type` (`^:auto` with no tag) |
| `DeclarationSpecifiers` (`Storage`, `TypeSpec`, `TypeQual`, `Func`, `AlignSpec`, `Attr`), `SpecifierQualifierList` | metadata (§6.1) and type forms (§5.1) |
| `StorageClassSpecifier` (`Typedef`, `Extern`, `Static`, `Auto`, `Register`, `ThreadLocal`, `Declspec`, `Constexpr`) | `c/typedef`; `^:extern` ... `^:constexpr`; `__declspec` read and printed (§14) |
| `TypeSpecifier` (`Void` ... `BitInt`, `Int128`, `Uint128`, `Float16`, `BFloat16`, `Float32`...`Float128x`, `Decimal32/64/128`, `Complex`, `Imaginary`, `StructOrUnion`, `Enum`, `TypeName`, `TypeofExpr`, `TypeofType`, `Atomic`) | the type symbols and forms of §5.1 (`_Imaginary`: gcc does not implement Annex G's imaginary types; refused as the reference refuses) |
| `TypeQualifier` (`Const`, `Restrict`, `Volatile`, `Atomic`, `Nonnull`, `Attr`), `TypeQualifiers` | `const`, `restrict`, `volatile`, `atomic` forms; `_Nonnull` §14; attributes §5.6 |
| `FunctionSpecifier` (`Inline`, `Noreturn`) | `^:inline`, `^:noreturn` |
| `AlignmentSpecifier` (`Type`, `Expr`) | `:c/alignas-type`, `:c/alignas` |
| `AtomicTypeSpecifier` | `(atomic T)` |
| `StructOrUnionSpecifier` (`Def`, `Tag`), `StructOrUnion`, `StructDeclarationList`, `StructDeclaration` (`Decl`, `Assert`), `StructDeclaratorList`, `StructDeclarator` (`Decl`, `BitField`) | `(struct ...)`, `(union ...)`, members (§5.4) |
| `EnumSpecifier` (`Def`, `Tag`), `EnumTypeSpecifier`, `EnumeratorList`, `Enumerator` (`Ident`, `Expr`) | `(enum ...)` (§5.5) |
| `Declarator`, `DirectDeclarator` (`Ident`, `Decl`, `Arr`, `StaticArr`, `ArrStatic`, `Star`, `FuncParam`, `FuncIdent`), `Pointer` (`TypeQual`, `Ptr`, `Block`) | the name and its type form read outward (§1, 6); `FuncIdent` is the K&R identifier list; `Block` (Apple's `^`) is refused as gcc refuses it |
| `AbstractDeclarator`, `DirectAbstractDeclarator` (`Decl`, `Arr`, `StaticArr`, `ArrStatic`, `ArrStar`, `Func`), `TypeName` | type forms |
| `ParameterTypeList` (`List`, `Var`), `ParameterList`, `ParameterDeclaration` (`Decl`, `Abstract`), `IdentifierList` | parameter vectors (§5.3) |
| `Initializer` (`Expr`, `InitList`), `InitializerList`, `Designation`, `DesignatorList`, `Designator` (`Index`, `Index2`, `Field`, `Field2`) | `init`, `at`, `:member` (§6.9); `Index2` is GNU's range, `Field2` GNU's `field:` designator (`:field` the same) |
| `StaticAssertDeclaration` | `c/static-assert` |
| `AttributeSpecifierList`, `AttributeSpecifier`, `AttributeValueList`, `AttributeValue` (`Ident`, `Expr`) and the delta's `BalancedTokenSequence` | `:c/attrs`, `:c/gnu-attrs` (§5.6) |
| `Statement` (`Labeled`, `Compound`, `Expr`, `Selection`, `Iteration`, `Jump`, `Asm`), `CompoundStatement`, `BlockItemList`, `BlockItem` (`Decl`, `Label`, `Stmt`, `FuncDef`), `LabelDeclaration` | §7.2, §7.3; `FuncDef` a nested `c/defn`; `LabelDeclaration` `c/local-labels` |
| `LabeledStatement` (`Label`, `CaseLabel`, `Range`, `Default`) | `(label :L)`, `(case v)`, `(case lo hi)`, `(default)` |
| `ExpressionStatement` | the expression; an empty one: nothing (N4) |
| `SelectionStatement` (`If`, `IfElse`, `Switch`) | `when`, `if`, `cond`, `switch` |
| `IterationStatement` (`While`, `Do`, `For`, `ForDecl`) | `while`, `do-while`, `for` |
| `JumpStatement` (`Goto`, `GotoExpr`, `Continue`, `Break`, `Return`) | `goto`, `goto*`, `continue`, `break`, `return` |
| `AsmStatement`, `Asm`, `AsmArgList`, `AsmExpressionList`, `AsmIndex`, `AsmQualifierList`, `AsmQualifier` (`Volatile`, `Inline`, `Goto`) | `c/asm` (§7.9) |
| `ExpressionList` (`Assign`, `Comma`), `AssignmentExpression` (`Cond`, `Assign`, `Mul` ... `Or`), `ConditionalExpression` (`LOr`, `Cond`) | `comma`; `set!`; `?` |
| `LogicalOrExpression`, `LogicalAndExpression`, `InclusiveOrExpression`, `ExclusiveOrExpression`, `AndExpression`, `EqualityExpression`, `RelationalExpression`, `ShiftExpression`, `AdditiveExpression`, `MultiplicativeExpression` (each case one operator) | the operators of §7.5 |
| `CastExpression` (`Unary`, `Cast`) | `conv` |
| `UnaryExpression` (`Postfix`, `Inc`, `Dec`, `Addrof`, `Deref`, `Plus`, `Minus`, `Cpl`, `Not`, `SizeofExpr`, `SizeofType`, `LabelAddr`, `AlignofExpr`, `AlignofType`, `Imag`, `Real`) | `pre-inc!`, `pre-dec!`, `addr`, `@`, `+`, `-`, `bit-not`, `not`, `sizeof`, `sizeof-type`, `label-addr`, `alignof`, `alignof-type`, `imag`, `real` |
| `PostfixExpression` (`Primary`, `Index`, `Call`, `Select`, `PSelect`, `Inc`, `Dec`, `Complit`), `ArgumentExpressionList` | `aget`, calls, `.-f`, `->`, `inc!`, `dec!`, `compound-lit` |
| `PrimaryExpression` (`Ident`, `Int`, `Float`, `Char`, `LChar`, `String`, `LString`, `Expr`, `Stmt`, `Generic`), `GenericSelection`, `GenericAssociationList`, `GenericAssociation` (`Type`, `Default`) | symbols, §8's literals, `^:c/paren`, `stmt-expr`, `generic` |
| `ConstantExpression` | an expression in a constant position |

### 15.2 N3220's syntax (Annex A)

A.2.1 expressions: every production is §7.5-§7.7's (and C23's `nullptr`, `true`, `false`, §8.5;
`constexpr` constants are identifiers). A.2.2 declarations: §5, §6 (C23's attribute specifier sequences
in every position, §5.6; the fixed enum, §5.5; `typeof`, `typeof_unqual`, §5.1; `_BitInt`, §5.1;
storage classes in compound literals, §7.7; the empty initializer, §6.9; `static_assert` without a
message, §6.8; `auto` and `constexpr`, §6.1). A.2.3 statements: §7.3 (labels before declarations and
at a block's end, §7.3). A.2.4 external definitions: §6.4 (unnamed parameters, §5.3). A.3 the
preprocessor: §9 (`#elifdef`, `#elifndef`, `#embed` and its parameters, `#warning`, `__has_include`,
`__has_embed`, `__has_c_attribute`, `__VA_OPT__`). A.1 the lexical grammar: §8 (digit separators, binary
constants, `wb`/`uwb`, `u8` character constants, UAX #31 identifiers, §4.4).

### 15.3 The corpora

`test/c/corpora.edn` lists every corpus with its pin (ccorpus2 v1.6.0, go-whim's c23conf at `80c49bf5`,
gcc 15.2.0's C23 tests from its source tarball, musl 1.2.6's release); each file's `:std` comes from its
corpus or, for gcc's tests, from its `dg-options`.

### 15.4 Reserved heads in C context

- Statements: `let do when if cond switch case default while do-while for label goto goto* break
  continue return`, and the qualified `c/asm c/attr-decl c/local-labels c/expr c/include c/define c/undef
  c/directive c/pragma`.
- Expressions: `+ - * / % << >> == != < <= > >= and or not bit-and bit-or bit-xor bit-not addr set! inc!
  dec! pre-inc! pre-dec! aget aset .-NAME -> conv sizeof sizeof-type alignof alignof-type generic ? comma
  compound-lit init at stmt-expr label-addr real imag va-arg offsetof types-compatible? classify-type
  has-attribute? convertvector bit-cast`, `deref` (from `@`), and the qualified `c/call c/id c/embed`.
- Literals (data, never code): `lit byte-string code-units imaginary`.
- Types (in type positions): `* const volatile restrict atomic array fn struct union enum typeof
  typeof-type typeof-unqual typeof-unqual-type bit-int unsigned-bit-int complex c/bits c/base`.
- Declarations (qualified, at top level or in a `let`): `c/file c/header c/var c/declare c/defn c/typedef
  c/struct c/union c/enum c/group c/static-assert`.

## 16. Worked examples

Each C text below was built by gcc 15.2 (`-std=c23 -pedantic-errors -Wall`, the last `-std=gnu23`) and
run; each forms block was read by Arbace's reader (the main checkout's stage 0, `arbace.core/read` until
end of input). The forms show `:lines` without the layout's metadata; header units' digests are
illustrative. The files are in the C1 worktree's scratch, `.tmp/c1/examples/`.

### 16.1 Declarations and types (`decls.c`, prints `6 8 12 4`)

```c
#include <stddef.h>

struct point { int x, y; };

typedef struct {
    unsigned kind : 3;
    unsigned : 0;
    unsigned flags : 5;
    union { int i; float f; };
    char name[];
} node, *node_ptr;

enum hue : unsigned char { RED, GREEN = 3, BLUE };

static int (*handlers[4])(int, char *);

int (*(*grid)[10])(int);

extern int printf(const char *restrict fmt, ...);

[[nodiscard("check it")]] static inline int area(const struct point *p) {
    return p->x * p->y;
}

int sum(int n, int a[static n]) {
    int s = 0;
    for (int i = 0; i < n; i++) s += a[i];
    return s;
}

int main(void) {
    struct point p = { .y = 2, .x = 3 };
    int a[] = { [2] = 5, 1, 2 };
    node_ptr q = nullptr;
    (void)q; (void)handlers; (void)grid;
    printf("%d %d %zu %d\n", area(&p), sum(5, a), offsetof(node, name), (int)BLUE);
    return 0;
}
```

```clojure
(in-ns 'c.decls) (c/file "decls.c" :std :c23)
(c/include "<stddef.h>" :unit c.h.2b7e9d01c4a3f586)

(c/struct point [^int x ^int y])

(c/group (struct [^{:tag unsigned :c/bits 3} kind
                  (c/bits unsigned 0)
                  ^{:tag unsigned :c/bits 5} flags
                  (union [^int i ^float f])
                  ^{:tag (array char)} name])
  (c/typedef node c/base)
  (c/typedef node_ptr (* c/base)))

(c/enum ^{:c/underlying unsigned-char} hue [RED [GREEN 3] BLUE])

(c/var ^:static ^{:tag (array 4 (* (fn [int (* char)] int)))} handlers)

(c/var ^{:tag (* (array 10 (* (fn [int] int))))} grid)

(c/declare ^:extern printf ^int [^{:tag (restrict (* (const char)))} fmt &])

(c/defn ^{:c/attrs [[nodiscard "check it"]] :c/attrs-at :decl :static true :inline true} area
  ^int [^{:tag (* (const (struct point)))} p]
  (return (* (-> p x) (-> p y))))

(c/defn sum ^int [^int n ^{:tag ^:c/static (array n int)} a]
  (let [^int s 0]
    (for [^int i 0] (< i n) (inc! i) (set! s + (aget a i)))
    (return s)))

(c/defn main ^int [void]
  (let [^{:tag (struct point)} p (init :y 2 :x 3)
        ^{:tag (array int)} a (init (at 2) 5 1 2)
        ^node_ptr q nil]
    (conv void q) (conv void handlers) (conv void grid)
    (printf "%d %d %zu %d\n" (area (addr p)) (sum 5 a)
            ^{:c/macro "offsetof(node, name)"} (offsetof node :name) (conv int BLUE))
    (return 0)))
```

- `struct point { int x, y; }` declares a tag only: `c/struct`. The shared declarators of the typedef
  of an anonymous struct are a `c/group` (§6.7).
- `static int (*handlers[4])(int, char *)`: read outward, an array of 4 pointers to functions.
- `int a[static n]` is `^:c/static (array n int)`, the metadata inside the tag's form.
- `offsetof(node, name)` is `<stddef.h>`'s macro over `__builtin_offsetof`: the node with its macro.
- The anonymous member `union { int i; float f; }` is an element of the member vector.

### 16.2 Control flow (`control.c`, prints `11 -1 1 abcdefg 3`)

```c
#include <stdio.h>

static int classify(int c) {
    int r = 0;
    switch (c) {
    case 'a':
    case 'e':
        r = 1;
        [[fallthrough]];
    case 'x':
        r += 10;
        break;
    default:
        r = -1;
    }
    return r;
}

static void copy(char *to, const char *from, int count) {
    int n = (count + 3) / 4;
    switch (count % 4) {
    case 0: do { *to++ = *from++;
    case 3:      *to++ = *from++;
    case 2:      *to++ = *from++;
    case 1:      *to++ = *from++;
            } while (--n > 0);
    }
}

int main(void) {
    char buf[8] = {};
    copy(buf, "abcdefg", 7);
    int i = 0;
again:
    i++;
    if (i < 3) goto again;
    int k = i, j;
    for (j = 0; j < 2; j++, k--) continue;
    if (k < 0) goto end;
    printf("%d %d %d %s %d\n", classify('a'), classify('q'), k, buf, i > 2 ? i : -i);
end:
}
```

```clojure
(in-ns 'c.control) (c/file "control.c" :std :c23)
(c/include "<stdio.h>" :unit c.h.6f1d0c2a9e43b871)

(c/defn ^:static classify ^int [^int c]
  (let [^int r 0]
    (switch c
      (case \a)
      (case \e)
      (set! r 1)
      (c/attr-decl fallthrough)
      (case \x)
      (set! r + 10)
      (break)
      (default)
      (set! r -1))
    (return r)))

(c/defn ^:static copy ^void [^{:tag (* char)} to ^{:tag (* (const char))} from ^int count]
  (let [^int n (/ ^:c/paren (+ count 3) 4)]
    (switch (% count 4)
      (case 0) (do-while (> (pre-dec! n) 0)
                 (set! @(inc! to) @(inc! from))
                 (case 3) (set! @(inc! to) @(inc! from))
                 (case 2) (set! @(inc! to) @(inc! from))
                 (case 1) (set! @(inc! to) @(inc! from))))))

(c/defn main ^int [void]
  (let [^{:tag (array 8 char)} buf (init)]
    (copy buf "abcdefg" 7)
    (let [^int i 0]
      (label :again)
      (inc! i)
      (when (< i 3) (goto :again))
      (let [^int k i
            (c/var ^:c/grouped ^int j)]
        (for [(set! j 0)] (< j 2) (comma (inc! j) (dec! k)) (continue))
        (when (< k 0) (goto :end))
        (printf "%d %d %d %s %d\n" (classify \a) (classify \q) k buf (? (> i 2) i (- i)))
        (label :end)))))
```

- `copy` returns `void`: the vector's tag is `void`, as any return type.
- Duff's device: the `case` labels are items of the `do-while`'s body, inside the `switch`.
- `*to++ = *from++` is `(set! @(inc! to) @(inc! from))`: postfix increments, their old values
  dereferenced.
- `[[fallthrough]];` is an attribute declaration among the items; `end:` at the block's end labels
  nothing (C23).
- `int k = i, j;`: one binding with an initializer and one `c/var` marked `^:c/grouped` (N3).

### 16.3 C23 (`c23.c`, prints `500 0 z 1 4`)

```c
#include <stdio.h>

constexpr int N = 1'000;
static_assert(N == 0b11'1110'1000);

typedef typeof(N) n_type;

static unsigned _BitInt(12) wrap(unsigned _BitInt(12) x) { return x + 1uwb; }

int main(void) {
    auto half = N / 2.0;
    typeof_unqual(N) m = 0;
    unsigned char c = u8'z';
    int *p = nullptr;
    int zeros[4] = {};
    bool ok = p == nullptr && !zeros[3];
    m += (int)wrap(4095);
    printf("%g %d %c %d %d\n", half, m, c, ok, (int)sizeof(n_type));
    return 0;
}
```

```clojure
(in-ns 'c.c23) (c/file "c23.c" :std :c23)
(c/include "<stdio.h>" :unit c.h.6f1d0c2a9e43b871)

(c/var ^:constexpr ^int N 1000)
(c/static-assert (== N 1000))

(c/typedef n_type (typeof N))

(c/defn ^:static wrap ^{:tag (unsigned-bit-int 12)} [^{:tag (unsigned-bit-int 12)} x]
  (return (+ x (lit (unsigned-bit-int 1) 1))))

(c/defn main ^int [void]
  (let [^:auto half (/ N 2.0)
        ^{:tag (typeof-unqual N)} m 0
        ^unsigned-char c (lit unsigned-char \z)
        ^{:tag (* int)} p nil
        ^{:tag (array 4 int)} zeros (init)
        ^_Bool ok (and (== p nil) (not (aget zeros 3)))]
    (set! m + (conv int (wrap 4095)))
    (printf "%g %d %c %d %d\n" half m c ok (conv int (sizeof-type n_type)))
    (return 0)))
```

- `1'000` and `0b11'1110'1000` are both `1000` (N1; `:full` keeps them).
- `1uwb` is `unsigned _BitInt(1)`: `(lit (unsigned-bit-int 1) 1)`.
- `auto half` has no tag: the checker infers `double` (6.7.10).
- `bool` is `_Bool` (N2); `nullptr` is `nil`; `= {}` is `(init)`.

### 16.4 The preprocessing layer (`square.c`, prints `101`)

```c
/* square.c: macros and conditionals. */
#include <stdio.h>
#define SQUARE(x) ((x) * (x))
#define LIMIT 10

#if LIMIT > 5
static int big = 1;
#else
static int big = 0;
#endif

int main(void) {
    int s = SQUARE(LIMIT) + big; // 101
    printf("%d\n", s);
    return 0;
}
```

`:lines`:

```clojure
(in-ns 'c.square) (c/file "square.c" :std :c23)  ; /* square.c: macros and conditionals. */
(c/include "<stdio.h>" :unit c.h.6f1d0c2a9e43b871)
(c/define SQUARE [x] "((x) * (x))")
(c/define LIMIT "10")

(c/directive "#if LIMIT > 5")
(c/var ^:static ^int big 1)
(c/directive "#else")

(c/directive "#endif")

(c/defn main ^int [void]
  (let [^int s (+ ^{:c/macro "SQUARE(LIMIT)" :c/paren true :c/aparen #{1 2}} (* 10 10) big)] ; 101
    (printf "%d\n" s)
    (return 0)))
```

The `:lines` print is cemit's text with the directives in place: the inactive group empty, the
invocation recovered (`int s = SQUARE(LIMIT) + big;`). In `:full`, `c/file` carries the comments and
white space, `(c/directive "#else")` the inactive group's text (`:c/inactive "static int big = 0;\n"`),
and every form its `:c/pos`; the print is the file byte for byte:

```clojure
(in-ns 'c.square) (c/file "square.c" :std :c23 :positions :full
  :c/comments [[1 1 "/* square.c: macros and conditionals. */"] [13 34 "// 101"]]
  :c/space [[13 5 "\n    "]])
^{:c/pos [8 1] :c/inactive "static int big = 0;\n"} (c/directive "#else")
```

(Only the layer's shape is shown; §10.2's encoding is C3b's.)

### 16.5 GNU C (`gnu.c`, `-std=gnu23`, prints `2 4 5 2`)

```c
#include <stdio.h>

#define MAX(a, b) ({ __typeof__(a) _a = (a), _b = (b); _a > _b ? _a : _b; })

struct __attribute__((packed)) hdr { char tag; int len; };

static int grade(int score) {
    switch (score) {
    case 90 ... 100: return 4;
    case 80 ... 89: return 3;
    default: return 0;
    }
}

int main(void) {
    static void *jump[] = { &&one, &&two };
    int n = 0;
    goto *jump[n];
one:
    n = MAX(n + 1, 2);
two:
    printf("%d %d %zu %d\n", n, grade(95), sizeof(struct hdr), (int)__builtin_expect(n ?: 7, 1));
    return 0;
}
```

```clojure
(in-ns 'c.gnu) (c/file "gnu.c" :std :gnu23)
(c/include "<stdio.h>" :unit c.h.6f1d0c2a9e43b871)

(c/define MAX [a b] "({ __typeof__(a) _a = (a), _b = (b); _a > _b ? _a : _b; })")

(c/struct ^{:c/gnu-attrs [packed]} hdr [^char tag ^int len])

(c/defn ^:static grade ^int [^int score]
  (switch score
    (case 90 100) (return 4)
    (case 80 89) (return 3)
    (default) (return 0)))

(c/defn main ^int [void]
  (let [(c/var ^:static ^{:tag (array (* void))} jump (init (label-addr :one) (label-addr :two)))
        ^int n 0]
    (goto* (aget jump n))
    (label :one)
    (set! n ^{:c/macro "MAX(n + 1, 2)"}
            (stmt-expr
              (let [^{:tag (typeof (+ n 1))} _a ^:c/paren (+ n 1)
                    ^{:c/aparen #{2}} (c/var ^:c/grouped ^{:tag (typeof (+ n 1))} _b 2)]
                (? (> _a _b) _a _b))))
    (label :two)
    (printf "%d %d %zu %d\n" n (grade 95) (sizeof-type (struct hdr))
            (conv int (__builtin_expect (? n 7) 1)))
    (return 0)))
```

- The statement expression's `let` is the body of `stmt-expr`; its value is its last expression
  statement's, the conditional.
- `n ?: 7` is `(? n 7)`; `case 90 ... 100` is `(case 90 100)`; `&&one` is `(label-addr :one)`.
- `__typeof__(a)` is `(typeof (+ n 1))`: the parentheses are `typeof`'s own. `(b)` is a parenthesized
  literal, so its mark is the `c/var`'s `:c/aparen` (index 2, the initializer); `_b` shares `_a`'s
  specifiers and is `^:c/grouped` (N3).

## 17. Changes to the survey's sketch, open questions

### 17.1 Changes to the survey's §4

The survey's §4 was a sketch; writing this spec against the reader and the reference found these, each
folded in above:

- **A1** `(?: c a b)` is unreadable (the reader refuses a symbol ending in `:`): `(? c a b)`, and GNU's
  `a ?: b` `(? a b)` (§7.5).
- **A2** `(array ^:static 3 T)` is unreadable (a number cannot carry metadata): `^:c/static (array 3 T)`
  (§5.1).
- **A3** `(lit T 1 2)` for a compound literal clashed with `(lit unsigned 42)` for a constant (CS7 (a)):
  compound literals are `(compound-lit T ...)` (§7.7).
- **A4** `(struct ^:anon {fields})` held members in a map (no order, duplicates refused): member vectors
  (§5.4), and `c/struct`, `c/union`, `c/enum` take the type form's arguments (§6.6).
- **A5** Type symbols that are C identifiers (`bool`, `int128`, `decimal64`, `float32`, `wchar_t`,
  `char8_t`) would collide with typedefs of older code: keywords, hyphenated words or reserved
  identifiers only (§5.1); a prefixed literal's type is the base type (`(lit unsigned-char "x")`, §8.4).
- **A6** `c/let` is `let`, as Go forms' (CS6 (a)): in C context it can only mean C's block declaration;
  its bindings take declaration forms too (§7.2).
- **A7** A function's return type is always its parameter vector's tag, `void` included
  (`(c/defn copy ^void [...] ...)`), and a body never yields a value (§6.4).
- **A8** `:includes` on `c/file` is replaced by `c/include` forms where the directives stand (their
  order relative to `c/define` matters), with header units (§4.3, CS30).
- **A9** Doc strings: C has no doc comments, so no comment becomes one (§10.4).
- **A10** `(c/declare ^:extern f (fn [int] int))` is `(c/declare ^:extern f ^int [int])`, the
  signature as `c/defn`'s (§6.3).
- **A11** `(char-const "ab")` is the integer value (`24930`, §8.2), and `L'é'`, `u8'a'` are values of
  their types (CS7 (a)).
- **A12** Labels and `case` labels are items (as the survey had), and `(label :L stmt)` (Go forms') is
  not used.

### 17.2 Open questions

The survey's CS30-CS43 (doc/C23-SURVEY.md, "C1: the plan after CS4 (b)") are this spec's open
questions; the spec follows each recommendation:

- CS30 header units (§4.3); CS31 the order of evaluation (§13.3); CS32 the reference's headers (§13.4);
  CS34 the floating environment (§12.2); CS35 the extended arithmetic types (§14); CS41 parentheses in
  `:lines` (§7.5); CS43 the fixed epoch (§9.1).

## 18. Sources

- ISO/IEC 9899:2024 (C23); WG14 N3220 (2024-02-22), the working draft whose subclause numbers this spec
  uses, `https://www.open-std.org/jtc1/sc22/wg14/www/docs/n3220.pdf`.
- System V Application Binary Interface, AMD64 Architecture Processor Supplement (sizes, alignments,
  bit-fields, `_BitInt`); gcc 15.2.0's behaviour on the host (`x86_64-alpine-linux-musl`), measured in
  the C1 worktree's `.tmp/c1/probe/` (`types.c`, `en.c`, `order.c`, `pp.c`, `div.c`, `ovf.c`, `nul.c`,
  `ab.c`, `c23lib.c`), with clang 23.1.4 beside.
- go-whim at `80c49bf53cd6340487ac623f3d6d135b77ac1db6`: `crefactor/cc/ast.go` (the productions of §15.1),
  `crefactor/cc/README.md` (the C23 delta), `crefactor/cemit` and `crefactor/clisp/SPEC.md` (the
  canonical spelling and the outward reading of declarators), read, not copied.
- musl 1.2.6's release (`include/`), pinned in `test/c/corpora.edn`.
- Arbace: doc/go/SPEC.md (the model: §1, §4.4, §7.3-§7.8, §8, §10, §13.5), doc/classes/SPEC.md,
  doc/C23-SURVEY.md (§4-§8, CS1-CS43), doc/classes/JAVA-FRONTEND-SURVEY.md ("As built: JN8").
